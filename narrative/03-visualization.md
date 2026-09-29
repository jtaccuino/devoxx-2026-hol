---
marp: true
theme: default
paginate: true
stream: tooling
slot: 11:10-11:25
title: Module 3 · gog4j
---

<!-- _class: lead -->

# Module 3 · gog4j

### A grammar of graphics, on JavaFX

`notebooks/exercises/04-plotting-with-gog4j.ipynb`

---

# Meet the tool · gog4j

*A grammar of graphics for JavaFX — ggplot2, for Java.*

<div class="grid two">
  <div class="tile"><b>data + aes + geoms</b><span class="d">compose a plot layer by layer</span></div>
  <div class="tile"><b>a full geom set</b><span class="d">points, bars, boxes, densities, facets, 3-D, matrices</span></div>
  <div class="tile"><b>it is a Node</b><span class="d">a plot is a JavaFX <code>Pane</code> &mdash; <code>display(...)</code> it</span></div>
  <div class="tile"><b>export</b><span class="d">write publication SVG straight from the plot</span></div>
</div>

**Get it:** <https://github.com/svenreimers/gog4j>

---

# The grammar

A plot is three pieces, composed:

| | |
|---|---|
| **data** | a `HardwoodTable`, read straight from the Parquet |
| **aes** | which columns drive x, y, colour, … |
| **geoms** | how to draw them |

```java
Ggplot.ggplot(table,
        Aes.aes().x("inclination").y("apogee_km").color("orbit_class"))
    .geoms(Geoms.point());
```

That is a scatterplot of 21,207 satellites.

---

# Layers stack

The same base, refined:

```java
.geoms(Geoms.point())
.labs(Labs.labs("Everything in orbit", "Inclination (deg)", "Apogee (km)"))
.theme(Theme.theme_dark())
.facets(Facets.wrap("orbit_class", 2))
```

<div class="grid three">
  <div class="tile"><b>labs</b><span class="d">title and axes</span></div>
  <div class="tile"><b>theme</b><span class="d">every colour at once</span></div>
  <div class="tile"><b>facets</b><span class="d">small multiples, one panel per class</span></div>
</div>

---

# Geometry tells the story

Faceting by orbit class shows **four different populations**:

```
LEO  ↓ low, dense, inclinations from 0° to 142°
GEO  → a tight band, near-zero inclination, one altitude
MEO  • a sparse shell
HEO  ↗ wildly elliptical
```

One `hline` at **35,786 km** makes the GEO belt obvious.

---

# display, or export

```java
display(plot);                                   // live, in the notebook
```

```java
new SvgExporter().size(1600, 1000).batchPoints(true)
    .write(plot, cwd.resolve("celestrak-orbits.svg"));
```

`batchPoints` keeps 21,000 points editable in the SVG.
SVG → PDF is one external command away.

---

# Meet the tool · Orekit

*The space-flight dynamics library — the one the bonus notebook leans on.*

- reads TLEs and **propagates** them (SGP4/SDP4) to any instant
- positions, velocities, frames, time scales
- used here to place all 21,207 objects where they are **right now**

**Get it:** <https://www.orekit.org/>

---

# ★ Bonus — for fast finishers

Finished early? The notebook has three, plus a standalone one:

<div class="grid two">
  <div class="tile"><b>1 · pairs plot</b><span class="d">a scatterplot matrix of every feature at once</span></div>
  <div class="tile"><b>2 · 3-D orbit cloud</b><span class="d">inclination × period × eccentricity</span></div>
  <div class="tile"><b>3 · the wall chart</b><span class="d">filled density + the GEO belt, dark theme</span></div>
  <div class="tile"><b>4 · orbits now</b><span class="d"><code>notebooks/bonus/orbits-now-3d.ipynb</code> &mdash; propagate all 21,207 objects with Orekit, then <strong>animate</strong> a LEO fleet</span></div>
</div>

---

<!-- _class: lead -->
<!-- _stream: ml -->
<!-- _slot: 11:25-12:15 -->

# Over to Zoran

### A stream of its own

That is the four-tool pipeline over the CelesTrak catalog, done.

**What comes next is Zoran's part.**
