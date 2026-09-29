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

# Environment check

- JDK **26+** (gog4j targets 26; this room runs 27)
- JTaccuino, with a **pre-seeded Maven cache** — everything resolves offline
- everything you need is in this repository

Open `notebooks/exercises/01-jtaccuino-basics.ipynb` from the **repository
root**, and we begin.
