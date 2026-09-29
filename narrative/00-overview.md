---
marp: true
theme: default
paginate: true
---

<!-- _class: lead -->

# Java is for Data Science, Too

### Building an end-to-end ML pipeline without leaving the JVM

**Devoxx Belgium 2026 · hands-on lab**
Sven Reimers · Zoran Sevarac

---

# What you will build

A pipeline over the **CelesTrak satellite catalog**:

1. **JTaccuino** — a notebook that *is* a Java REPL
2. **Hardwood** — read Parquet without reading all of it
3. **dflib** — shape 21,207 rows with a DataFrame
4. **gog4j** — a grammar of graphics on JavaFX
5. **DeepNetts** — predict debris from orbital elements *(Zoran)*

No Python. No context switch. Just the JVM.

---

# Meet the stack · JTaccuino

*A notebook for Java — JShell in cells, with pictures.*

- Java execution by **JShell**; every cell shares one session
- `display(...)` renders any **JavaFX** node inline
- plain JSON notebooks; open from disk
- built for teaching and interactive experimentation

**Get it:** <https://jtaccuino.github.io>
**Source:** <https://github.com/jkost/jtaccuino>

---

# Meet the stack · Hardwood

*A Parquet reader for the JVM — fast, small, no ceremony.*

- read the **footer** for schema, sizes and statistics, without touching data pages
- stream rows through a **filter**, or read whole **column batches** as primitive arrays
- the reader behind this lab's dataset (21,207 rows, 52 columns)

**Get it:** <https://hardwood.dev/>

---

# Meet the stack · dflib

*DataFrames in plain Java. Select, filter, derive, group, sort.*

- a fluent, immutable `DataFrame` API — no SQL, no Python
- 2.0 expressions: `$str(...)`, `$double(...)`, `count()`, `avg()`
- CSV and **Parquet** I/O; reads the catalog straight into a frame

**Get it:** <https://github.com/dflib/dflib>

---

# Meet the stack · gog4j

*A grammar of graphics for JavaFX — ggplot2, for Java.*

- **data + aes + geoms**, composed layer by layer
- points, bars, boxes, densities, **facets**, 3-D, scatterplot matrices
- a plot **is** a JavaFX `Pane`: `display(...)` it, or export to **SVG**

**Get it:** <https://github.com/svenreimers/gog4j>

---

# Meet the stack · Orekit

*The space-flight dynamics library — the one the bonus notebook leans on.*

- reads TLEs and **propagates** them (SGP4/SDP4) to any instant
- positions, velocities, frames, time scales
- used here to place all 21,207 objects where they are **right now**

**Get it:** <https://www.orekit.org/>

---

# The dataset

`data/celestrak_gp_catalog.parquet`

| | |
|---|---|
| rows | **21,207** objects |
| columns | **52** |
| every current CelesTrak GP element set | merged, one row per object |
| enriched with SATCAT | owner, object type, launch date |
| format | Parquet, GZIP, one row group, ~5.4 MB |

A real file, not a toy. And small enough to hold in memory.

---

# What is actually in orbit

```
LEO  19,260   ████████████████████████████████████████
GEO   1,183   ██
HEO     558   █
MEO     206   ▌
```

```
payloads  17,110   █████████████████████████████████████
debris     2,957   ███████
rocket b.    544   █
```

`active,starlink` alone is **10,952** objects.

---

# The shape of the lab

| time | who | what |
|---|---|---|
| 10:30 | Sven | intro + dataset (this deck) |
| 10:40 | Sven | **module 1** — JTaccuino · 5 min talk, 10 min hands-on |
| 10:55 | Sven | **module 2** — Hardwood + dflib · 5 + 10 |
| 11:10 | Sven | **module 3** — gog4j · 5 + 10 |
| 11:25 | Zoran | **ML pipeline** — DeepNetts |
| 12:15 | both | wrap-up, questions |

Every module: **a short talk, then you type.** Solutions are provided.

---

# How this works

Each module has a notebook with two kinds of cells:

- **TODO** — yours. The scaffold runs; it fails exactly where you haven't
  filled it in yet.
- **given** — run it as-is and watch.
- **★ BONUS** — for fast finishers. More wow, clearly marked.

Every exercise has a matching **solution**. No one gets left behind; no one
gets bored.

---

# Bonus

**★ A whole-catalog 3-D view** — `notebooks/bonus/orbits-now-3d.ipynb`.
Propagate all 21,207 objects to the current instant with **Orekit** and plot
where they are *right now*.

For fast finishers, along with the ★ BONUS cells inside the notebooks.

---

# Getting everything

| what | where |
|---|---|
| **JDK 26+** | any recent OpenJDK — this lab is verified on **27** |
| **JTaccuino** | <https://jtaccuino.github.io> |
| **this lab** | `git clone <HOL-REPO-URL>` |
| **Maven dependencies** | `<M2-BUNDLE-URL>` → unpack into `~/.m2/repository` |

The Maven bundle already contains **every artifact the notebooks resolve** —
including the `0.5-SNAPSHOT` builds — so the whole lab runs **offline**.

Then open `notebooks/exercises/01-jtaccuino-basics.ipynb` and we begin.
