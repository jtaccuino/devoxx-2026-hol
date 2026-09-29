---
marp: true
theme: default
paginate: true
---

<!-- _class: lead -->

# Module 3 · gog4j

### A grammar of graphics, on JavaFX

`notebooks/exercises/04-plotting-with-gog4j.ipynb`

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

- `labs` — title and axes
- `theme` — every colour at once
- `facets` — small multiples, one panel per class

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

# ★ Bonus rounds

Finished early? The notebook has four:

1. **Pairs plot** — `matrixPlot` of every feature at once
2. **3-D orbit cloud** — inclination × period × eccentricity
3. **The wall chart** — filled density + the GEO belt, dark theme
4. *(module 2)* **Top owners** — a bar chart of who launches
5. *(`notebooks/bonus/orbits-now-3d.ipynb`)* **Where is everything right now?**
   — propagate all 21,207 objects to this instant with Orekit, plot the cloud

Low effort. High wow. Clearly marked.

---

<!-- _class: lead -->

# Over to Zoran

### From table to trained model

The notebooks end with a clean, labelled dataset — every object described by its
orbital elements, with a ground-truth label for payload versus debris.

**What happens next is Zoran's part of the story.**
