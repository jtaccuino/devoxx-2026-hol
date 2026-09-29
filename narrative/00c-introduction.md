---
marp: true
theme: default
paginate: true
title: Introduction
---

<!-- _class: lead -->

# What we will be doing

### One small Java program, end to end

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

# How this works

Each module has a notebook with two kinds of cells:

- **TODO** — yours. The scaffold runs; it fails exactly where you haven't
  filled it in yet.
- **given** — run it as-is and watch.
- **★ BONUS** — for fast finishers. More wow, clearly marked.

Every exercise has a matching **solution**. No one gets left behind; no one
gets bored.

---

# The plan

- **module 1 · JTaccuino** — the notebook itself
- **module 2 · Hardwood + dflib** — read the data, shape the data
- **module 3 · gog4j** — draw the data
- **then Zoran** — turn it into a model

Each module opens with a one-slide intro to its tool: what it is and where to
get it. Everything you need is installed already.
