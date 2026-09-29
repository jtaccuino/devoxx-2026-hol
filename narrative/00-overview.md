---
marp: true
theme: default
paginate: true
title: Overview
---

<!-- _class: lead -->

# Java is for Data Science, Too

### Building an end-to-end ML pipeline without leaving the JVM

**Devoxx Belgium 2026 · hands-on lab**
Sven Reimers · Zoran Sevarac

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
| **this lab** | `git clone https://github.com/jtaccuino/devoxx-hol-2026` |
| **Maven dependencies** | [`devoxx-hol-2026-m2.zip`](https://github.com/jtaccuino/devoxx-hol-2026/releases) → unpack into `~/.m2/repository` |

The Maven bundle already contains **every artifact the notebooks resolve** —
including the `0.5-SNAPSHOT` builds — so the whole lab runs **offline**.

Then open `notebooks/exercises/01-jtaccuino-basics.ipynb` and we begin.
