# devoxx-hol-2026 — Java is for Data Science, Too

Hands-on lab material for **Devoxx Belgium 2026**: build an end-to-end data and
machine-learning pipeline over a real satellite catalog, without leaving the
JVM.

CelesTrak element sets → **Hardwood** (Parquet) → **dflib** (DataFrames) →
**gog4j** (plots) → **DeepNetts** (ML). Notebooks run in **JTaccuino**.

```
narrative/    slides (Marp markdown) — open as prose, render as a deck
narrative/web/  the single pre-rendered HTML deck (open index.html)
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
| Maven | the **offline bundle** from the releases page, unpacked into `~/.m2/repository` |

`dataset/*.java` run on **JDK 21**; the notebooks need 26+.

gog4j `0.5.0` is published to **GitHub Packages**
(`https://maven.pkg.github.com/jtaccuino/gog4j`). GitHub Packages requires
authentication even for public packages, and jbang does not read repositories
from `settings.xml` — so the labs are delivered with a **pre-seeded `~/.m2`**:
download `devoxx-hol-2026-m2.zip` from the
[releases page](https://github.com/jtaccuino/devoxx-hol-2026/releases) and
unpack it into `~/.m2/repository`. That is the supported way to run the
notebooks; resolving gog4j live needs a GitHub token and is not part of the lab.

## Run order

Open the notebooks in JTaccuino. Each one locates the dataset by walking up from
its own folder, so it works wherever the repository sits on disk:

1. `notebooks/exercises/01-jtaccuino-basics.ipynb`
2. `notebooks/exercises/02-parquet-with-hardwood.ipynb`
3. `notebooks/exercises/03-dataframes-with-dflib.ipynb`
4. `notebooks/exercises/04-plotting-with-gog4j.ipynb`

Each has a matching file in `notebooks/solutions/`. Every notebook has a final
**check** cell that prints `✓` when the TODOs are correct. Each **TODO** cell
gives the task and a Socratic hint — a nudge toward the right API, never the
finished line — so you work out the code rather than uncomment it.

`narrative/00-overview.md` opens the story; `narrative/01..03` are the module
intros. They are plain Markdown with [Marp](https://marp.app) front-matter, so
the files read fine as prose and render as a deck with any Marp tool.

## Narrative (web)

All of `narrative/*.md` is pre-rendered, in order, to **one self-contained HTML
deck**: `narrative/web/index.html`. Open it in a browser — no build step, no
network.

The running order is:

1. **Overview** — the big agenda and what you will build (one slide), then the data
2. **Space primer** — just enough vocabulary to do the exercises
3. **Getting set up** — how the notebooks work, and how to get everything
4. **Module 1 · JTaccuino**, **Module 2 · Hardwood + dflib**, **Module 3 · gog4j**
   — each opens with a "meet the tool" slide and closes with a ★ Bonus slide
5. **Over to Zoran** — a separate stream, left open

The deck has **two visible streams**: Sven's *tooling* (the four tools, modules
1–3) and Zoran's *ML pipeline*. The opening slide shows both; everything after it
belongs to the tooling stream, and the closing slide hands off to Zoran.

Navigation:

- a **running footer** shows the current stream, the section, the time slot and
  how many minutes are left; **prev / next / overview / contents** are on the right
- keyboard: arrows / space to move, `Home` / `End`, `o` for the overview,
  `c` for contents, `Esc` to close it; `#7` in the URL jumps to a slide
- click the left quarter to go back, elsewhere to go forward
- print to PDF from the browser

Stream and timing are driven by front matter — `stream:` (`intro`/`tooling`/`ml`)
and `slot:` (`10:40-10:55`) per file, overridable on a single slide with
`<!-- _stream: ml -->` / `<!-- _slot: 11:25-12:15 -->`.

Re-render after editing the markdown:

```bash
jbang tools/render_narrative.java
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

`notebooks/bonus/orbits-live-3d.ipynb` takes it further: it precomputes a few
hundred LEO positions per frame and **redraws the gog4j plot about twice a
second**, swapping a freshly built plot into a container that was displayed
once — so the fleet appears to orbit the Earth.

## Known gog4j issues

Found while building this lab (gog4j `0.5.0`, 2026-09-28). Both are
library-side, reproducible without this repo's code, and worth fixing upstream.

**1 · Facets fail on a table containing a local-wall-clock timestamp.**

`HardwoodDataFrame.of(...)` + `.facets(...)` throws:

```
IllegalStateException: Column 'epoch' is a local-wall-clock TIMESTAMP
(isAdjustedToUTC=false); use getLocalTimestamp instead
```

Faceting materialises every column, and `gog4j-hardwood`'s
`ParquetColumns.decode` calls `rows.getTimestamp(name)` for *any*
`TimestampType`, ignoring `isAdjustedToUTC`. Fix: use `getLocalTimestamp` when
the column is not UTC-adjusted.

*Workaround used in the lab:* project to the columns being plotted with
`HardwoodDataFrame.ofColumns(...)` before building the plot (see module 3).

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

**3 · `gog4j-dflib-data`'s POM is invalid for Maven consumers.**

The published POM declares `org.dflib:dflib-csv` with **no version** and no
`dependencyManagement` (the module is published from Gradle with module
metadata, `do_not_remove: published-with-gradle-metadata`). A Maven/Aether
consumer sees:

```
'dependencies.dependency.version' for org.dflib:dflib-csv:jar is missing
... transitive dependencies (if any) will not be available
```

so resolving `gog4j-dflib-data` on its own pulls no transitives. The penguins
worksheet works only because it also adds `dflib`, `dflib-csv` and
`dflib-parquet` explicitly. Fix: add a `dflib-bom` `import` to
`gog4j-dflib-data`'s `dependencyManagement`, or pin the version.

**4 · The local engine cannot resolve `addToClasspath` classes during stack-map generation.**

Not strictly gog4j, but it bit this lab. JShell's **local** execution engine
instruments each snippet with the Class-File API (JDK 24+) to inject stop
checks. Regenerating stack maps resolves referenced classes through the
**system** class loader, which cannot see a class added with `addToClasspath`:

```
java.lang.IllegalArgumentException: Could not resolve class TLE
    at ...StackMapGenerator.mergeReferenceFrom...
    at jdk.jshell.execution.LocalExecutionControl.instrument(...)
```

It only triggers when such a type sits on a **stack-map merge point** (a branch,
loop, ternary or try/catch merging two reference types) — which is why it looked
intermittent. Workaround used in the lab: put the external type in a single
straight-line helper and let the branchy caller carry only primitives.
`tools/verify_jshell.java` replays a notebook against the real local engine to
catch this before the IDE does.

## Maintainer notes

Notebooks are generated, not hand-edited:

```bash
jbang tools/make_notebooks.java          # writes exercises, solutions, fallback, bonus
jbang tools/verify_notebooks.java        # replays every solution, prints PASS/FAIL
jbang tools/verify_jshell.java <nb> <cp> <cwd>   # replay on the real JShell local engine
jbang tools/render_narrative.java        # regenerates narrative/web/index.html
jbang tools/package_m2.java              # builds devoxx-hol-2026-m2.zip (offline Maven repo)
```

`tools/verify_notebooks.java` builds the solution classpaths from
`tools/cp/*.java`, which carry a `//REPOS` line pointing at Maven Central and
GitHub Packages; that is how jbang reaches gog4j `0.5.0`. Keep `~/.m2/settings.xml`
(server `github`) on a developer machine, or the `//REPOS` resolution will 401.

`tools/verify_notebooks.java` runs the solutions in JShell with the JTaccuino
builtins stubbed. The JavaFX notebooks (04, the penguins worksheet and the two
3-D bonus notebooks) are reassembled into a jbang program that starts the JavaFX
toolkit, runs the notebook on a **worker** thread and has `display(...)` attach
nodes to a live scene — exactly like JTaccuino — so FX-thread mistakes fail the
build rather than surfacing only in the IDE.

> **Threading rule for notebook code.** A cell runs on a worker thread, so any
> JavaFX work must be marshalled: the Python-free notebooks define `onFx(...)`
> and route `save(...)` through it, and call `onFx(plot::markDirty)` after
> mutating a plot that is already displayed.
>
> **Dependency note.** A cell is not a snippet: `ReactiveJShell.eval` loops over
> `analyzeCompletion`, so **each statement becomes its own JShell snippet** and
> `addDependency(...)` is already separate from the imports below it. The
> generator keeps the `addDependency(...)` calls first and follows them with a
> one-line `Class.forName(...)` probe that names any jar which did not land —
> that is what turns a mystery "cannot find symbol: TLE" into a clear warning.
>
> **Output rule.** Notebook code uses the notebook builtins, never
> `System.out.println` (that writes to the JVM console and the notebook shows
> nothing): `println(...)` is captured into the cell and `display(...)` draws a
> node. `use("dflib")` activates the dflib extension, whose
> `println(DataFrame)` renders a real table.

`tools/package_m2.java` assembles the offline Maven bundle. It resolves the
dependency closure — gog4j `0.5.0` from GitHub Packages when
`GOG4J_REPO_PASSWORD` is set (which is what CI does), otherwise from a developer's
local `~/.m2` — verifies the closure resolves again with `-o` (no network), and
zips the whole repository. It pulls **every** JavaFX platform classifier so the
bundle is not tied to one OS.

The bundle is built and published automatically by
`.github/workflows/offline-bundle.yml` on every push to `main`: it attaches
`devoxx-hol-2026-m2.zip` to a rolling `offline-bundle` release, whose asset URL
the "Getting everything" slide links to. JTaccuino resolves `~/.m2/repository`
as a `file://` remote, so unpacking the zip there is all a student needs.

To build it by hand against GitHub Packages:

```bash
export GOG4J_REPO_PASSWORD=$(gh auth token)
export GOG4J_REPO_USER=$(gh api user -q .login)
jbang tools/package_m2.java
```

## Attribution

Satellite element sets and catalog data are derived from **CelesTrak**
(<https://celestrak.org>). Please keep the attribution if you redistribute the
dataset. See [`data/README.md`](data/README.md).
