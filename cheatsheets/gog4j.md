# gog4j cheat sheet

A grammar of graphics on JavaFX. Dependencies:
`org.jtaccuino:gog4j:0.5-SNAPSHOT`, `org.jtaccuino:gog4j-hardwood:0.5-SNAPSHOT`.

Requires **JDK 26+**.

## Data

```java
var table = HardwoodTable.ofFile(path);         // stream from Parquet
var table = HardwoodTable.ofColumns(Map.of(     // project to selected columns
    "x", full.column("x"), "y", full.column("y")));
```

`HardwoodTable` is not a dflib `DataFrame`, but `Ggplot` plots it directly
through the gog4j-hardwood SPI.

## The grammar

| Piece | Factory |
|---|---|
| base plot | `Ggplot.ggplot(data, Aes.aes()...)` → `Plot<DF>` |
| 3-D | `Ggplot.ggplot3d(data, aes)` |
| pairs matrix | `Ggplot.matrixPlot(data, aes, "col1", "col2", ...)` |
| combine | `Ggplot.composedPlot(figures...)` |

## Aesthetics (`Aes`)

```java
Aes.aes()
   .x("inclination").y("apogee_km")
   .color("orbit_class").fill("orbit_class")
   .size("...").alpha("...").shape("...")
   .group("...")
```

## Geoms (`Geoms`)

```java
Geoms.point()  Geoms.jitter()  Geoms.line()  Geoms.smooth()
Geoms.bar()    Geoms.col()     Geoms.area()  Geoms.histogram()
Geoms.boxplot() Geoms.violin() Geoms.density() Geoms.density2dFilled()
Geoms.tile()   Geoms.text()
Geoms.hline(35786)  Geoms.vline(x)  Geoms.abline(slope, intercept)
// 3-D
Geoms.point3d() Geoms.surface3d() Geoms.function3d(op) ...
```

## Refinement (fluent, returns `Plot`)

```java
plot.geoms(Geoms.point(), Geoms.hline(35786))
    .labs(Labs.labs("Title", "x label", "y label"))
    .theme(Theme.theme_dark())
    .facets(Facets.wrap("orbit_class", 2))
    .coord(Coords.coord3d());
```

Themes: `Theme.theme_dark() / theme_light() / theme_gray() / theme_bw() /
theme_minimal()`. Facets: `Facets.wrap(col, n)` / `Facets.grid(row, col)`.

## Show, or export

```java
display(plot);                       // a Plot *is* a JavaFX Pane

new SvgExporter()
    .size(1600, 1000)
    .batchPoints(true)               // keep thousands of points as circles
    .rasterizeAbove(10_000)          // optional: rasterize huge layers
    .write(plot, path);
```

`SvgExporter` and `Plot` construction must run on the **JavaFX Application
Thread** — which is where JTaccuino runs notebooks, so nothing to do in class.

## Known issue

`HardwoodTable.ofFile(...)` + `.facets(...)` currently throws, because faceting
materialises every column and gog4j-hardwood decodes a local-wall-clock
timestamp column (`epoch` here) with the wrong accessor. Project to the columns
you need with `ofColumns(...)` first (as the lab does). See the repository
README, "Known gog4j issues".
