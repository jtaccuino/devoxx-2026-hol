---
marp: true
theme: default
paginate: true
---

<!-- _class: lead -->

# Module 1 · JTaccuino

### A notebook is a REPL with cells and state

`notebooks/exercises/01-jtaccuino-basics.ipynb`

---

# Why a notebook at all?

Because data work is a **conversation**.

- you try something, look at the result, adjust
- state should persist between attempts
- the *record* of what you did is the artifact

Jupyter made this normal — in **Python**.

JTaccuino is the same idea for **Java**, running on JShell.

---

# The whole model

A notebook is **one JShell session**, split into cells.

```java
// cell 1
int satellites = 21207;
```

```java
// cell 2 — still defined
println("we have %,d objects", satellites);
```

Variables, imports, methods — anything you define is visible in later cells.
That is the entire trick.

---

# Two things a REPL does not have

**1 · `display(...)`** — hand it a JavaFX node, it draws it inline.

```java
var earth = new Circle(0, 0, 60, Color.ORANGE);
display(earth);
```

**2 · Cells are the unit of re-running.** Change one, re-run one.

---

# One rule that will bite you

JShell executes a snippet when the line is **complete**.

```java
// this does NOT work — the first line is already complete
if (ready) println("go");
else       println("wait");
```

```java
// this does — the open brace keeps the snippet going
if (ready) {
    println("go");
} else {
    println("wait");
}
```

Same for long expressions: keep them on **one line**, or open a bracket.

---

# Your turn

Open `01-jtaccuino-basics.ipynb`.

**5 TODOs.** At the end you will `display` a small orbit diagram — the same
`display` that shows every plot in this lab.

~10 minutes. The solution is next to it if you get stuck.

> On to **module 2** — where the real data starts.
