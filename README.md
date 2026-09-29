# devoxx-hol-2026 — Java is for Data Science, Too

Hands-on lab material for **Devoxx Belgium 2026**: build an end-to-end data and
machine-learning pipeline over a real satellite catalog, without leaving the
JVM.

CelesTrak element sets → **Hardwood** (Parquet) → **dflib** (DataFrames) →
**gog4j** (plots) → **DeepNetts** (ML). Notebooks run in **JTaccuino**.

```
narrative/    slides (Marp markdown) — open as prose, render as a deck
narrative/web/  pre-rendered HTML decks (open index.html, arrow keys to navigate)
cheatsheets/  one-page API reference per library
notebooks/    exercises + solutions
notebooks/fallback/  penguins worksheet, if the dataset is unavailable
notebooks/bonus/     orbits-now-3d: propagate the whole catalog to now (Orekit)
data/         the dataset (committed) and raw inputs
dataset/      jbang scripts that rebuild the dataset
tools/        notebook generator, narrative renderer, verification harness
```

## Requirements

| | |
|---|---|
| JDK | **26 or newer** — gog4j is compiled to class file 70. This material is verified on **JDK 27**. |
| JTaccuino | the notebook kernel |
| Maven | a **pre-seeded `~/.m2`** — everything resolves offline |

`dataset/*.java` run on **JDK 21**; the notebooks need 26+.

## Run order

Open the notebooks **from this directory** (they resolve `data/` relative to the
working directory):

1. `notebooks/exercises/01-jtaccuino-basics.ipynb`
2. `notebooks/exercises/02-parquet-with-hardwood.ipynb`
3. `notebooks/exercises/03-dataframes-with-dflib.ipynb`
4. `notebooks/exercises/04-plotting-with-gog4j.ipynb`

Each has a matching file in `notebooks/solutions/`. Every notebook has a final
**check** cell that prints `✓` when the TODOs are correct.

`narrative/00-overview.md` is the slide deck; `narrative/01..03` are the module
intros. They are plain Markdown with [Marp](https://marp.app) front-matter, so
`marp narrative/00-overview.md --pdf` renders a deck and the files read fine as
prose without any tooling.

## Narrative (web)

The decks are also pre-rendered to **self-contained HTML** in `narrative/web/`.
Open `narrative/web/index.html` in a browser — no build step, no network:

- arrow keys / space / click to move, `Home` / `End` to jump, `#7` in the URL
- also print to PDF from the browser

`narrative/00b-space-primer.md` is a short introduction to the space vocabulary
(TLE, inclination, LEO/MEO/GEO/HEO, propagation) for anyone new to the domain.

Re-render after editing the markdown:

```bash
python3 tools/render_narrative.py
```

## The dataset

`data/celestrak_gp_catalog.parquet` — 21,207 satellites × 52 columns, enriched
with SATCAT metadata. Full provenance, attribution, SHA-256 and per-column
distributions are in [`data/README.md`](data/README.md).

Rebuild it (offline and deterministic, from the committed raw inputs):

```bash
jbang dataset/build.java
jbang dataset/read-check.java     # read-back with hardwood
```

See [`dataset/README.md`](dataset/README.md) for a refresh from CelesTrak and
the usage policy.

## Fallback

If the Parquet cannot be read, `notebooks/fallback/penguins-worksheet.ipynb`
repeats the whole workflow on the penguins dataset bundled inside gog4j — same
TODO structure, nothing to download.

## Bonus

`notebooks/bonus/orbits-now-3d.ipynb` (with a solution) propagates **every**
object in the catalog from its element-set epoch to the current instant with
Orekit's SGP4/SDP4, then plots the resulting x/y/z point cloud in 3-D with
gog4j — plus a top-down payload-vs-debris view. 20,210 objects propagate in
well under a second.

## Known gog4j issues

Found while building this lab (gog4j `0.5-SNAPSHOT`, 2026-09-28). Both are
library-side, reproducible without this repo's code, and worth fixing upstream.

**1 · Facets fail on a table containing a local-wall-clock timestamp.**

`HardwoodTable.ofFile(...)` + `.facets(...)` throws:

```
IllegalStateException: Column 'epoch' is a local-wall-clock TIMESTAMP
(isAdjustedToUTC=false); use getLocalTimestamp instead
```

Faceting materialises every column, and `gog4j-hardwood`'s
`ParquetColumns.decode` calls `rows.getTimestamp(name)` for *any*
`TimestampType`, ignoring `isAdjustedToUTC`. Fix: use `getLocalTimestamp` when
the column is not UTC-adjusted.

*Workaround used in the lab:* project to the columns being plotted with
`HardwoodTable.ofColumns(...)` before building the plot (see module 3).

**2 · The same failure is masked by an NPE.**

In `Plot.renderToInternal`, `partitionData()` is called in a `try` whose
`finally` dereferences the (still null) `partitions`:

```
NullPointerException: Cannot invoke "java.util.Map.size()" because "partitions" is null
```

so the real exception is hidden. Guard the `finally` on `partitions != null`.

```java
finally {
    if (partitions != null) partEvt.groups = partitions.size();
    partEvt.end();
    partEvt.commit();
}
```

## Maintainer notes

Notebooks are generated, not hand-edited:

```bash
python3 tools/make_notebooks.py          # writes exercises, solutions, fallback, bonus
python3 tools/verify_notebooks.py        # replays every solution, prints PASS/FAIL
python3 tools/render_narrative.py        # regenerates narrative/web/*.html
```

`tools/verify_notebooks.py` runs the solutions in JShell with the JTaccuino
builtins stubbed; the JavaFX notebooks (04, the penguins worksheet and the 3-D
bonus) are reassembled into a jbang program that runs on the JavaFX Application
Thread, because gog4j plot construction and `SvgExporter` require it.

## Attribution

Satellite element sets and catalog data are derived from **CelesTrak**
(<https://celestrak.org>). Please keep the attribution if you redistribute the
dataset. See [`data/README.md`](data/README.md).
