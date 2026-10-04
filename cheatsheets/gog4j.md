# gog4j cheat sheet

A grammar of graphics on JavaFX. Dependencies: `org.jtaccuino:gog4j:0.5.0` plus a
data provider — `org.jtaccuino:gog4j-dflib:0.5.0` (a dflib `DataFrame`) or
`org.jtaccuino:gog4j-hardwood:0.5.0` (streamed from Parquet).

Requires **JDK 26+**.

## Data

```java
var df = Parquet.loader().load(path);                    // dflib: whole frame
var table = df.cols("x", "y", "orbit_class").select();   // project the columns

var table = HardwoodDataFrame.of(path);                  // or stream with hardwood
```

`Ggplot` plots either kind directly: `gog4j-dflib` and `gog4j-hardwood` are
`DataExtractor` SPI providers, so the same `Ggplot.ggplot(data, aes)` works for
a dflib `DataFrame` and for a `HardwoodDataFrame`.

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
plot.setPrefHeight(400);             // required: a Plot has no default height
display(plot);                       // a Plot *is* a JavaFX Pane

new SvgExporter()
    .size(1600, 1000)
    .batchPoints(true)               // keep thousands of points as circles
    .rasterizeAbove(10_000)          // optional: rasterize huge layers
    .write(plot, path);
```

A `Plot` is a plain `Node`, so building it off the JavaFX thread is fine and
`display(plot)` marshals it onto the FX thread itself. `SvgExporter` is the
exception: its snapshot needs the FX Application Thread, so an export must be
marshalled (`Platform.runLater`) — the lab notebooks only `display(...)`.

> **A `Plot` has no default size.** Without an explicit `setPrefHeight` it
> displays as a sliver. Every notebook cell here sets `plot.setPrefHeight(400)`
> before `display(...)`. See the repository README, "Known gog4j issues".

## Known issue

`HardwoodDataFrame.of(...)` + `.facets(...)` currently throws, because faceting
materialises every column and gog4j-hardwood decodes a local-wall-clock
timestamp column (`epoch` here) with the wrong accessor. Project to the columns
you need with `ofColumns(...)` first (as the lab does). See the repository
README, "Known gog4j issues".
