---
marp: true
theme: default
paginate: true
title: Module 1 · JTaccuino
---

<!-- _class: lead -->

# Module 1 · JTaccuino

### A notebook is a REPL with cells and state

`notebooks/exercises/01-jtaccuino-basics.ipynb`

---

# Meet the tool · JTaccuino

*A notebook for Java — JShell in cells, with pictures.*

<div class="grid two">
  <div class="tile"><b>one session</b><span class="d">Java execution by <strong>JShell</strong>; every cell shares it</span></div>
  <div class="tile"><b>pictures inline</b><span class="d"><code>display(...)</code> renders any JavaFX node</span></div>
  <div class="tile"><b>plain JSON</b><span class="d">notebooks are files you can open from disk</span></div>
  <div class="tile"><b>built for teaching</b><span class="d">and interactive experimentation</span></div>
</div>

**Get it:** <https://jtaccuino.github.io> · **Source:** <https://github.com/jkost/jtaccuino>

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

<div class="grid two">
  <div class="tile"><b>1 · display(...)</b><span class="d">hand it a JavaFX node and it draws it inline</span></div>
  <div class="tile"><b>2 · cells re-run</b><span class="d">change one cell, re-run just that cell</span></div>
</div>

```java
var earth = new Circle(0, 0, 60, Color.ORANGE);
display(earth);
```

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
