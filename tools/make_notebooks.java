///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21

// Generate the Devoxx 2026 lab notebooks (exercises + solutions).
//
// One definition per notebook; a code cell with a `sol` variant is a TODO whose
// exercise form is the scaffold and whose solution form is the filled-in answer.
// Run from the repository root:
//
//     jbang tools/make_notebooks.java
//
// The cell text lives in verbatim text blocks (content is flush-left so it can
// be read and edited as-is; the helpers strip the one trailing newline).

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class make_notebooks {

    record Cell(String kind, String text, String sol) {}

    static Cell md(String text) { return new Cell("md", text.strip(), null); }
    static Cell code(String text) { String s = text.strip(); return new Cell("code", s, s); }
    static Cell code(String text, String sol) { return new Cell("code", text.strip(), sol.strip()); }

    // JSON is emitted by a tiny hand-rolled writer below, so the tool has no
    // dependencies at all and matches the repository's 4-space JSON house style:
    // "key": value, {} and [] for empty containers, raw UTF-8.

    // ----------------------------------------------------------------------
    // 01 - JTaccuino basics
    // ----------------------------------------------------------------------
    static final List<Cell> E1 = List.of(
            md("""
# 01 · JTaccuino basics — a REPL with cells and state

A JTaccuino notebook is a Java REPL that remembers. Every **code cell shares one
session**, so a variable or a method you define in one cell is still there in the
next. That is the whole idea; the rest is convenience layered on top.

Run the cells top to bottom with **Shift+Enter**.

* **TODO** cells are yours — fill them in.
* **given** cells are ready to run — leave them alone and enjoy the result.
* **★ BONUS** cells are optional, if you finish early.
"""),
            code("""
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.layout.Pane;

String num(long v) { return String.format(java.util.Locale.ROOT, "%,d", v); }
String dec(double v, int p) { return String.format(java.util.Locale.ROOT, "%." + p + "f", v); }
"""),
            md("""
## 1 · A session that remembers

Define a value here; the next cell can still see it. That is all a notebook
really is — an ordered conversation with one JVM.
"""),
            code("""
// ── TODO 1 ────────────────────────────────────────────────────────────
// Declare an int called `satellites` and set it to 21207, the number of
// objects in the CelesTrak catalog you will meet in the next notebooks.
//
// hint: which Java type holds a whole number? Declare one named `satellites`.
""",
                    """
int satellites = 21207;
"""),
            code("""
if (satellites == 21207) {
    println("✓ the catalog holds %s objects", num(satellites));
} else {
    println("✗ expected 21,207 but got %s", num(satellites));
}
"""),
            md("""
## 2 · Methods are just more state

A method you define in one cell is visible in every later cell, exactly like a
variable. Here is the one orbital formula this whole lab leans on: a satellite
that completes `n` revolutions per day has a period of `1440 / n` minutes.
"""),
            code("""
// ── TODO 2 ────────────────────────────────────────────────────────────
// Finish the method so it returns the orbital period in minutes.
//
// hint: a day is 1440 minutes — what do you divide by what?

double periodMinutes(double meanMotionRevPerDay) {
    // your turn
}
""",
                    """
double periodMinutes(double meanMotionRevPerDay) {
    return 1440.0 / meanMotionRevPerDay;
}
"""),
            code("""
println("ISS turns 15.5 times a day -> %s min per orbit", dec(periodMinutes(15.5), 1));
"""),
            code("""
// ── TODO 3 ────────────────────────────────────────────────────────────
// A geostationary satellite turns exactly once per day (mean motion 1.0).
// Store its period in a double called `geoPeriod`.
//
// hint: which mean motion means 'one revolution per day'? Pass it to your method.
""",
                    """
double geoPeriod = periodMinutes(1.0);
"""),
            code("""
println("geostationary period %s min (a full day is 1440)", dec(geoPeriod, 0));
"""),
            md("""
## 3 · From values to pictures

JTaccuino adds one thing a plain REPL does not: `display(...)`. Hand it a JavaFX
node and it draws it inline. Build a node, then show it.
"""),
            code("""
// ── TODO 4 ────────────────────────────────────────────────────────────
// Create a Circle centred on (0, 0) with radius 60 and colour ORANGE.
// Call it `earth`.
//
// hint: a JavaFX shape takes its centre, radius and fill up front.
""",
                    """
var earth = new Circle(0, 0, 60, Color.ORANGE);
"""),
            code("""
// ── TODO 5 ────────────────────────────────────────────────────────────
// Show it on its own.
//
// hint: which JTaccuino built-in draws a node inline?
""",
                    """
display(earth);
"""),
            code("""
// ── given ─────────────────────────────────────────────────────────────
// Everything above in one small picture: Earth, an orbit, a satellite.
var scene = new Pane();
scene.setPrefSize(320, 320);

var orbit = new Circle(160, 160, 120);
orbit.setFill(Color.TRANSPARENT);
orbit.setStroke(Color.web("#3a6ea5"));
orbit.setStrokeWidth(1.5);

var planet = new Circle(160, 160, 34, Color.web("#2e7d32"));
var sat = new Circle(160 + 120, 160, 7, Color.ORANGE);

var label = new Text(160 - 34, 160 + 5, "EARTH");
label.setFill(Color.web("#2e7d32"));

scene.getChildren().addAll(orbit, planet, sat, label);
display(scene);
"""),
            code("""
// ── ★ BONUS ───────────────────────────────────────────────────────────
// A small constellation: twelve satellites evenly spaced on the orbit ring.
var fleet = new Pane();
fleet.setPrefSize(320, 320);
var ring = new Circle(160, 160, 120);
ring.setFill(Color.TRANSPARENT);
ring.setStroke(Color.web("#3a6ea5"));
fleet.getChildren().add(ring);
fleet.getChildren().add(new Circle(160, 160, 34, Color.web("#2e7d32")));
for (int i = 0; i < 12; i++) {
    double angle = i * 2 * Math.PI / 12;
    fleet.getChildren().add(new Circle(160 + 120 * Math.cos(angle), 160 + 120 * Math.sin(angle), 5, Color.ORANGE));
}
display(fleet);
"""),
            md("""
**That is the whole tool.** Cells share state, `println` formats, and
`display` shows a node. Everything else in this lab is Java, a library, and this
dataset.

Next: [`02-parquet-with-hardwood.ipynb`](02-parquet-with-hardwood.ipynb).
"""));

    // ----------------------------------------------------------------------
    // 02 - Parquet with Hardwood
    // ----------------------------------------------------------------------
    static final List<Cell> E2 = List.of(
            md("""
# 02 · Parquet with Hardwood

`data/celestrak_gp_catalog.parquet` holds 21 207 satellites and 52 columns. The
point of a columnar file is that you rarely need to read all of it. Hardwood
gives you three levels, cheapest first:

1. **the footer** — shape and statistics, from the last few kilobytes
2. **row readers** — stream rows through a filter, one at a time
3. **column readers** — whole columns as primitive arrays

This notebook climbs those three levels.
"""),
            code("""
addDependency("dev.hardwood:hardwood-core:1.1.0.Beta1");

import dev.hardwood.InputFile;
import dev.hardwood.internal.predicate.StatisticsDecoder;
import dev.hardwood.reader.ColumnReader;
import dev.hardwood.reader.FilterPredicate;
import dev.hardwood.reader.ParquetFileReader;
import dev.hardwood.reader.RowReader;
import dev.hardwood.schema.ColumnProjection;
import java.nio.file.Files;
import java.nio.file.Path;

String num(double v) { return String.format(java.util.Locale.ROOT, "%,.0f", v); }
String dec(double v, int p) { return String.format(java.util.Locale.ROOT, "%." + p + "f", v); }

// The notebook may live in a sub-folder (exercises/, bonus/, ...), so walk up
// from the notebook's own folder to find the repository's data/ directory.
Path dataset() {
    for (Path p = cwd.toAbsolutePath(); p != null; p = p.getParent()) {
        Path candidate = p.resolve("data/celestrak_gp_catalog.parquet");
        if (Files.isRegularFile(candidate)) return candidate;
    }
    throw new IllegalStateException("data/celestrak_gp_catalog.parquet not found above " + cwd);
}

var catalog = ParquetFileReader.open(InputFile.of(dataset()));
var meta = catalog.getFileMetaData();
"""),
            md("""
## 1 · The footer

Everything here comes from the file trailer. No data page is decompressed, which
is why it costs the same on a 13 MB file and a 13 GB one.
"""),
            code("""
// ── TODO 1 ────────────────────────────────────────────────────────────
// Print how many rows the file has, how many row groups, and who wrote it.
// The values are on `meta` (a var from the setup cell).
//
// hint: which shape questions can the file metadata answer — rows, chunks, author?
""",
                    """
println("rows        %s", num(meta.numRows()));
println("row groups  %d", meta.rowGroups().size());
println("written by  %s", meta.createdBy());
"""),
            md("""
## 2 · The schema

The schema lists every column. Count them — and note how many are the `satcat_*`
enrichment columns added on top of the raw element sets.
"""),
            code("""
// ── TODO 2 ────────────────────────────────────────────────────────────
// Print the total column count, and how many columns start with "satcat_".
//
// hint: ask the schema for its column count; then walk the names and count those
//       beginning `satcat_`.
""",
                    """
var schema = catalog.getFileSchema();
int satcatCols = 0;
for (var c : schema.getColumns()) if (c.name().startsWith("satcat_")) satcatCols++;
println("columns %d, of which %d are satcat_*", schema.getColumnCount(), satcatCols);
"""),
            md("""
## 3 · Streaming the rows

A row reader hands you one row at a time. Count the debris objects — a row is
debris when its `satcat_object_type` is `DEB` or `R/B` — using a single `long` of
state, never a list.
"""),
            code("""
// ── TODO 3 ────────────────────────────────────────────────────────────
// Stream every row and count how many are debris (type "DEB" or "R/B").
//
// hint: open a row reader and stream — how do you step to the next row, and read
//       one text column by name?
""",
                    """
long debris = 0;
try (RowReader rows = catalog.rowReader()) {
    while (rows.hasNext()) {
        rows.next();
        String t = rows.getString("satcat_object_type");
        if ("DEB".equals(t) || "R/B".equals(t)) debris++;
    }
}
println("debris objects: %s", num(debris));
"""),
            md("""
## 4 · Column batches

A column reader returns a whole batch as one primitive array — no row object, no
boxing. Find the highest inclination in the catalog by touching a `double[]`.
"""),
            code("""
// ── TODO 4 ────────────────────────────────────────────────────────────
// Read the `inclination` column as batches and find the maximum value.
//
// hint: a column reader hands you batches — how do you know one is ready, how many
//       values it holds, and which array to scan?
""",
                    """
double maxInc = Double.NEGATIVE_INFINITY;
try (ColumnReader col = catalog.columnReader("inclination")) {
    while (col.nextBatch()) {
        double[] values = col.getDoubles();
        int n = col.getValueCount();
        for (int k = 0; k < n; k++) if (values[k] > maxInc) maxInc = values[k];
    }
}
println("highest inclination: %s deg", dec(maxInc, 2));
"""),
            md("""
## 5 · Statistics without touching the data

Each column chunk stores the minimum and maximum it saw. Read them straight out
of the footer — the values never leave the trailer. They arrive as raw bytes and
`StatisticsDecoder` decodes them per physical type.
"""),
            code("""
// ── TODO 5 ────────────────────────────────────────────────────────────
// Read inclination's min and max from the footer statistics of row group 0.
//
// hint: every column chunk keeps a min and a max. Find the *inclination* chunk,
//       then decode its raw statistics with StatisticsDecoder.
""",
                    """
var chunk = meta.rowGroups().get(0);
for (var c : chunk.columns()) {
    if (!c.metaData().pathInSchema().leafName().equals("inclination")) continue;
    var stats = c.metaData().statistics();
    println("inclination  %s .. %s (from the footer only)",
            dec(StatisticsDecoder.decodeDouble(stats.minValue()), 3),
            dec(StatisticsDecoder.decodeDouble(stats.maxValue()), 3));
}
"""),
            md("""
## 6 · given — where the file's bytes actually live

Still no data page read: every column chunk records how it was encoded and how
many bytes it took. This is the columnar payoff in one table.
"""),
            code("""
// ── given ─────────────────────────────────────────────────────────────
var wanted = java.util.Set.of("norad_cat_id", "mean_motion", "eccentricity",
        "inclination", "altitude_km", "period_min", "orbit_class", "norad_groups");

println("%-16s %-7s %11s %11s %7s", "column", "codec", "stored", "raw", "ratio");
for (var c : meta.rowGroups().get(0).columns()) {
    var cm = c.metaData();
    String name = cm.pathInSchema().leafName();
    if (!wanted.contains(name)) continue;
    println("%-16s %-7s %11s %11s %6sx", name, cm.codec(),
            num(cm.totalCompressedSize()), num(cm.totalUncompressedSize()),
            dec((double) cm.totalUncompressedSize() / cm.totalCompressedSize(), 1));
}
"""),
            code("""
// ── check ─────────────────────────────────────────────────────────────
var pass = meta.numRows() == 21207 && catalog.getFileSchema().getColumnCount() == 52 && debris == 3501 && maxInc > 142.0 && maxInc < 142.1;
if (pass) {
    println("✓ 21,207 rows · 52 columns · %s debris · max inclination %s", num(debris), dec(maxInc, 2));
} else {
    println("✗ something is off — check the TODOs above");
}
"""),
            md("""
**Which level to reach for:** footer for *shape*, row readers for *which
rows*, column readers for *what the values are*. Records and lists only when you
need random access.

Next: [`03-dataframes-with-dflib.ipynb`](03-dataframes-with-dflib.ipynb).
"""));

    // ----------------------------------------------------------------------
    // 03 - DataFrames with dflib
    // ----------------------------------------------------------------------
    static final List<Cell> E3 = List.of(
            md("""
# 03 · DataFrames with dflib

Hardwood streams. **dflib** is the other half: a DataFrame that you *shape* —
select, filter, derive, group, sort. This is the language of everyday data work,
and it is plain Java.

The catalog is loaded once, in memory, on purpose: 21 207 × 52 is small.
"""),
            code("""
addDependency("org.dflib:dflib:2.0.0-M7");
addDependency("org.dflib:dflib-parquet:2.0.0-M7");

import org.dflib.DataFrame;
import org.dflib.parquet.Parquet;
import static org.dflib.Exp.*;
import java.nio.file.Files;
import java.nio.file.Path;

String num(double v) { return String.format(java.util.Locale.ROOT, "%,.0f", v); }
String dec(double v, int p) { return String.format(java.util.Locale.ROOT, "%." + p + "f", v); }

// The notebook may live in a sub-folder (exercises/, bonus/, ...), so walk up
// from the notebook's own folder to find the repository's data/ directory.
Path dataset() {
    for (Path p = cwd.toAbsolutePath(); p != null; p = p.getParent()) {
        Path candidate = p.resolve("data/celestrak_gp_catalog.parquet");
        if (Files.isRegularFile(candidate)) return candidate;
    }
    throw new IllegalStateException("data/celestrak_gp_catalog.parquet not found above " + cwd);
}

var df = Parquet.loader().load(dataset());
println("loaded %s rows x %d columns", num(df.height()), df.width());
"""),
            md("""
## 1 · Select

Keep the columns you care about. `cols(...)` picks them, `select()` turns the
selection back into a frame.
"""),
            code("""
// ── TODO 1 ────────────────────────────────────────────────────────────
// Select object_name, orbit_class and apogee_km, and show the first 5 rows.
//
// hint: which call keeps a subset of columns, which materialises the frame, and
//       which shows the first rows?
""",
                    """
var sel = df.cols("object_name", "orbit_class", "apogee_km").select();
System.out.println(sel.head(5));
"""),
            md("""
## 2 · Filter by text

Conditions are built from expressions. `$str("col").eq("GEO")` gives a
`Condition`; `rows(...)` keeps the matching rows.
"""),
            code("""
// ── TODO 2 ────────────────────────────────────────────────────────────
// Keep only geostationary objects (orbit_class == "GEO") and count them.
//
// hint: you need rows where a text column equals a value — how do you state that
//       condition, and what keeps the matches?
""",
                    """
var geo = df.rows($str("orbit_class").eq("GEO")).select();
println("GEO objects: %s", num(geo.height()));
"""),
            md("""
## 3 · Filter by number

Numeric columns use `$double(...)` and the usual comparisons. Highly eccentric
orbits are where the interesting objects hide.
"""),
            code("""
// ── TODO 3 ────────────────────────────────────────────────────────────
// Count the objects whose eccentricity is greater than 0.25.
//
// hint: as in TODO 2, but the column is numeric and the test is 'greater than'.
""",
                    """
var eccentric = df.rows($double("eccentricity").gt(0.25)).select();
println("eccentricity > 0.25: %s objects", num(eccentric.height()));
"""),
            md("""
## 4 · Derive and aggregate

This is where a DataFrame earns its keep: compute something new *and* summarise
it in one breath — here the mean gap between apogee and perigee, per orbit class.
`$double("apogee_km").sub($double("perigee_km"))` is a derived expression, and
`.avg().as("...")` names the result.
"""),
            code("""
// ── TODO 4 ────────────────────────────────────────────────────────────
// For each orbit class, compute the mean spread between apogee and perigee,
// named "mean_spread_km", and sort by it.
//
// hint: one row per class, with a computed value. Derive apogee minus perigee,
//       average it, name it, then order by it.
""",
                    """
System.out.println(
    df.group("orbit_class").agg(
        $col("orbit_class"),
        $double("apogee_km").sub($double("perigee_km")).avg().as("mean_spread_km"))
      .sort("mean_spread_km", false));
"""),
            md("""
## 5 · Group and count

The classic. `group(...)` then `agg(...)` with `$col(...))` and `count()`.
"""),
            code("""
// ── TODO 5 ────────────────────────────────────────────────────────────
// Count the objects in each orbit class, largest first.
//
// hint: one row per class, counting its members — which aggregation counts rows?
""",
                    """
System.out.println(
    df.group("orbit_class").agg($col("orbit_class"), count()).sort("count", false));
"""),
            md("""
## 6 · Sort

Sort a whole frame by a column, descending, and take the top.
"""),
            code("""
// ── TODO 6 ────────────────────────────────────────────────────────────
// Show the 5 objects with the highest apogee (object_name and apogee_km only).
//
// hint: which sort direction puts the largest first? Then keep two columns and
//       take five.
""",
                    """
System.out.println(
    df.sort("apogee_km", false)
      .cols("object_name", "apogee_km").select()
      .head(5));
"""),
            code("""
// ── check ─────────────────────────────────────────────────────────────
var classes = df.group("orbit_class").agg($col("orbit_class"), count());
var pass = sel.height() == 21207 && geo.height() == 1183 && eccentric.height() == 569 && classes.height() == 4;
if (pass) {
    println("✓ select kept 21,207 rows · GEO 1,183 · ecc>0.25 569 · 4 orbit classes");
} else {
    println("✗ something is off — check the TODOs above");
}
"""),
            md("""
## ★ BONUS — who owns the sky?

Fast finishers: the `satcat_owner` column is 104 distinct owners. Group, count,
sort, and take the top 10.
"""),
            code("""
// ── ★ BONUS ───────────────────────────────────────────────────────────
System.out.println(
    df.group("satcat_owner").agg($col("satcat_owner"), count())
      .sort("count", false).head(10));
"""),
            md("""
Next: [`04-plotting-with-gog4j.ipynb`](04-plotting-with-gog4j.ipynb).
"""));

    // ----------------------------------------------------------------------
    // 04 - Plotting with gog4j
    // ----------------------------------------------------------------------
    static final List<Cell> E4 = List.of(
            md("""
# 04 · Plotting with gog4j

Numbers became a table; now the table becomes a picture. **gog4j** is ggplot2
for JavaFX. The grammar is three pieces:

* **data** — here a `HardwoodTable`, read straight from the Parquet
* **aes** — which columns drive x, y, colour, …
* **geoms** — how to draw them (points, lines, bars, density, …)

Build a plot with `Ggplot.ggplot(...)`, add layers with `.geoms(...)`, then
either `display(...)` it or export it to SVG.
"""),
            code("""
addDependency("org.jtaccuino:gog4j:0.5-SNAPSHOT");
addDependency("org.jtaccuino:gog4j-hardwood:0.5-SNAPSHOT");

import org.jtaccuino.gog.*;
import org.jtaccuino.gog.labs.Labs;
import org.jtaccuino.gog.render.SvgExporter;
import org.jtaccuino.gog.theme.Theme;
import org.jtaccuino.gog.hardwood.HardwoodTable;
import java.nio.file.Files;
import java.nio.file.Path;

// The notebook may live in a sub-folder (exercises/, bonus/, ...), so walk up
// from the notebook's own folder to find the repository's data/ directory.
Path dataset() {
    for (Path p = cwd.toAbsolutePath(); p != null; p = p.getParent()) {
        Path candidate = p.resolve("data/celestrak_gp_catalog.parquet");
        if (Files.isRegularFile(candidate)) return candidate;
    }
    throw new IllegalStateException("data/celestrak_gp_catalog.parquet not found above " + cwd);
}

var full = HardwoodTable.ofFile(dataset());

// Project to just the fields this module plots. Reading and keeping only the
// columns you need is the columnar habit; here it also keeps the frame small.
var table = HardwoodTable.ofColumns(java.util.Map.of(
        "inclination", full.column("inclination"),
        "eccentricity", full.column("eccentricity"),
        "period_min", full.column("period_min"),
        "apogee_km", full.column("apogee_km"),
        "orbit_class", full.column("orbit_class"),
        "satcat_object_type", full.column("satcat_object_type")));

void save(GgFigure figure, String name) {
    try {
        var path = cwd.resolve(name);
        new SvgExporter().size(1200, 800).batchPoints(true).write(figure, path);
        println("saved %s (%s bytes)", name, String.format(java.util.Locale.ROOT, "%,d", Files.size(path)));
    } catch (Exception e) {
        println("could not save %s: %s", name, e);
    }
}
"""),
            md("""
## 1 · Data + aes + geoms

One point per satellite: inclination across, apogee up, coloured by orbit class.
That single `color(...)` is the whole point of a grammar of graphics.
"""),
            code("""
// ── TODO 1 ────────────────────────────────────────────────────────────
// Build the scatter plot into `p`:
//   data  table
//   aes   x = inclination, y = apogee_km, colour = orbit_class
//   geom  points
//
// hint: a plot is data + aesthetics + a geom. How do you start one, map x/y/colour,
//       and add points?
""",
                    """
Plot<HardwoodTable> p = Ggplot.ggplot(table,
        Aes.aes().x("inclination").y("apogee_km").color("orbit_class"))
    .geoms(Geoms.point());
"""),
            code("""
// ── given ── look at it, and save a copy
display(p);
save(p, "01-scatter.svg");
"""),
            md("""
## 2 · Labels

A plot without axis labels is a riddle. `Labs.labs(...)` answers it.
"""),
            code("""
// ── TODO 2 ────────────────────────────────────────────────────────────
// Add a title and axis labels.
//
// hint: title and axis labels come as one builder — which method attaches it?
""",
                    """
p = p.labs(Labs.labs("Everything in orbit", "Inclination (deg)", "Apogee (km)"));
"""),
            code("""
display(p);
save(p, "02-labelled.svg");
"""),
            md("""
## 3 · Theme

A theme changes every colour at once. On a projector, dark wins.
"""),
            code("""
// ── TODO 3 ────────────────────────────────────────────────────────────
// Switch to the dark theme.
//
// hint: themes are ready-made — which one is dark, and how do you attach it?
""",
                    """
p = p.theme(Theme.theme_dark());
"""),
            code("""
display(p);
save(p, "03-dark.svg");
"""),
            md("""
## 4 · Facets

Faceting splits one plot into a small multiple per category — the fastest way to
see that the classes are genuinely different populations.
"""),
            code("""
// ── TODO 4 ────────────────────────────────────────────────────────────
// Split the plot into one panel per orbit class, in a 2-wide grid.
//
// hint: you want one small panel per class — which facet constructor splits by a
//       column into a grid?
""",
                    """
p = p.facets(Facets.wrap("orbit_class", 2));
"""),
            code("""
display(p);
save(p, "04-facets.svg");
"""),
            md("""
## 5 · Reference lines

The geostationary belt sits at 35 786 km. `Geoms.hline(...)` draws it, and one
glance then explains the horizontal stripe of GEO objects.
"""),
            code("""
// ── TODO 5 ────────────────────────────────────────────────────────────
// Add a horizontal line at the geostationary altitude, keeping the points.
//
// hint: keep the points and add a second layer at the geostationary altitude — how
//       does a plot take more than one geom?
""",
                    """
p = p.geoms(Geoms.point(), Geoms.hline(35786));
"""),
            code("""
display(p);
save(p, "05-geobelt.svg");
"""),
            md("""
## 6 · Export

`display` is for you; `SvgExporter` is for the paper. Export the finished figure
at print size, with the 21 000 points batched so the file stays editable.
"""),
            code("""
// ── TODO 6 ────────────────────────────────────────────────────────────
// Write the final plot to "celestrak-orbits.svg" at 1600 x 1000.
//
// hint: which exporter writes a figure to SVG, and how do you set its size and
//       keep thousands of points manageable?
""",
                    """
new SvgExporter().size(1600, 1000).batchPoints(true)
    .write(p, cwd.resolve("celestrak-orbits.svg"));
println("wrote celestrak-orbits.svg");
"""),
            md("""
## The bridge to machine learning

Everything so far *described* the catalog. Now predict something from it. Can the
orbital elements alone tell debris from a working satellite?

`satcat_object_type` gives the ground truth: `PAY` (payload) or `DEB` / `R/B`
(debris). The features are the elements — inclination, eccentricity, period, mean
motion, drag. We stop at the train/test split — a labelled feature matrix, ready
for a model.
"""),
            code("""
// ── given ── turn it into a supervised problem
addDependency("org.dflib:dflib:2.0.0-M7");
addDependency("org.dflib:dflib-parquet:2.0.0-M7");
import org.dflib.DataFrame;
import org.dflib.parquet.Parquet;
import static org.dflib.Exp.*;

var df = Parquet.loader().load(dataset());

// keep only rows with a usable label
var labelled = df.rows(
        $str("satcat_object_type").eq("PAY")
            .or($str("satcat_object_type").eq("DEB"))
            .or($str("satcat_object_type").eq("R/B")))
    .select();

println("labelled %s of %s rows", String.format(java.util.Locale.ROOT, "%,d", labelled.height()),
        String.format(java.util.Locale.ROOT, "%,d", df.height()));
System.out.println(labelled.group("satcat_object_type").agg($col("satcat_object_type"), count()));
"""),
            code("""
// ── given ── features and a deterministic split
var features = labelled
    .cols("inclination", "eccentricity", "period_min", "mean_motion", "bstar")
    .select();

int split = (int) (features.height() * 0.8);
var train = features.rowsRange(0, split).select();
var test  = features.rowsRange(split, features.height()).select();

println("train %s   test %s   features %d",
        String.format(java.util.Locale.ROOT, "%,d", train.height()),
        String.format(java.util.Locale.ROOT, "%,d", test.height()),
        features.width());
println("→ a feature matrix and a labelled split, ready for a model.");
"""),
            md("""
## ★ BONUS 1 — Pairs and composition

Two dozen features at once, as a scatterplot matrix. `matrixPlot` does the grid;
`composedPlot` arranges finished figures side by side.
"""),
            code("""
// ── ★ BONUS 1 ─────────────────────────────────────────────────────────
var pairs = Ggplot.matrixPlot(table,
    Aes.aes().color("orbit_class"),
    "inclination", "eccentricity", "period_min", "apogee_km");
display(pairs);
save(pairs, "bonus-pairs.svg");
"""),
            md("""
## ★ BONUS 2 — A three-dimensional orbit cloud

Add inclination, period and eccentricity as x/y/z and let the points fill the
volume.
"""),
            code("""
// ── ★ BONUS 2 ─────────────────────────────────────────────────────────
var cloud = Ggplot.ggplot3d(table,
        Aes.aes().x("inclination").y("period_min").z("eccentricity").color("orbit_class"))
    .geoms(Geoms.point3d());
display(cloud);
save(cloud, "bonus-3d.svg");
"""),
            md("""
## ★ BONUS 3 — The wall chart

A filled 2-D density instead of 21 000 points, with the GEO belt drawn on top.
This is the figure that goes on the poster.
"""),
            code("""
// ── ★ BONUS 3 ─────────────────────────────────────────────────────────
var wall = Ggplot.ggplot(table,
        Aes.aes().x("inclination").y("apogee_km").fill("orbit_class"))
    .geoms(Geoms.density2dFilled(), Geoms.hline(35786))
    .labs(Labs.labs("Where satellites live", "Inclination (deg)", "Apogee (km)"))
    .theme(Theme.theme_dark());
display(wall);
save(wall, "bonus-wallchart.svg");
"""),
            md("""
That is the whole pipeline: **Hardwood** read it, **dflib** shaped it,
**gog4j** drew it, and you left a labelled train/test split on the table for the
model.

Back to the [overview](../narrative/00-overview.md).
"""));

    // ----------------------------------------------------------------------
    // Fallback - penguins worksheet (bundled data, offline, no Parquet)
    // ----------------------------------------------------------------------
    static final List<Cell> PENGUINS = List.of(
            md("""
# Fallback · Penguins worksheet

If the CelesTrak file is unavailable on your machine, you can still do the whole
data-and-plot exercise. The **penguins** dataset is bundled inside gog4j itself,
so nothing is downloaded and nothing is read from disk.

It is small (344 rows, 8 columns) and does the same jobs: select, filter, group,
sort, plot. Mirrors the TODOs in modules 2 and 3.
"""),
            code("""
addDependency("org.jtaccuino:gog4j:0.5-SNAPSHOT");
addDependency("org.jtaccuino:gog4j-dflib:0.5-SNAPSHOT");
addDependency("org.jtaccuino:gog4j-dflib-data:0.5-SNAPSHOT");
addDependency("org.jtaccuino:gog4j-data:0.5-SNAPSHOT");
addDependency("org.dflib:dflib:2.0.0-M7");
addDependency("org.dflib:dflib-csv:2.0.0-M7");

import org.dflib.DataFrame;
import org.jtaccuino.gog.dflib.data.PenguinsDatasets;
import org.jtaccuino.gog.*;
import org.jtaccuino.gog.labs.Labs;
import org.jtaccuino.gog.render.SvgExporter;
import org.jtaccuino.gog.theme.Theme;
import java.nio.file.Files;
import static org.dflib.Exp.*;

String num(long v) { return String.format(java.util.Locale.ROOT, "%,d", v); }
String dec(double v, int p) { return String.format(java.util.Locale.ROOT, "%." + p + "f", v); }

void save(GgFigure figure, String name) {
    try {
        var path = cwd.resolve(name);
        new SvgExporter().size(1200, 800).batchPoints(true).write(figure, path);
        println("saved %s (%s bytes)", name, num(Files.size(path)));
    } catch (Exception e) {
        println("could not save %s: %s", name, e);
    }
}
"""),
            md("""
## 1 · Load it
"""),
            code("""
// ── TODO 1 ────────────────────────────────────────────────────────────
// Load the penguins frame into `df` and print its size.
//
// hint: which bundled-data class hands you the penguins frame?
""",
                    """
var df = PenguinsDatasets.loadPenguins();
println("penguins: %s rows x %d columns", num(df.height()), df.width());
"""),
            md("""
## 2 · Select
"""),
            code("""
// ── TODO 2 ────────────────────────────────────────────────────────────
// Keep species, bill_length_mm and body_mass_g, and show the first 3 rows.
//
// hint: pick three columns, materialise the frame, then show the first three rows.
""",
                    """
var sel = df.cols("species", "bill_length_mm", "body_mass_g").select();
System.out.println(sel.head(3));
"""),
            md("""
## 3 · Filter
"""),
            code("""
// ── TODO 3 ────────────────────────────────────────────────────────────
// Count the Gentoo penguins.
//
// hint: keep the rows whose species equals a value, then count them.
""",
                    """
var gentoo = df.rows($str("species").eq("Gentoo")).select();
println("Gentoo: %s birds", num(gentoo.height()));
"""),
            md("""
## 4 · Group and aggregate
"""),
            code("""
// ── TODO 4 ────────────────────────────────────────────────────────────
// Mean body mass per species, heaviest first.
//
// hint: one row per species with the mean body mass — then order by it.
""",
                    """
System.out.println(
    df.group("species").agg(
        $col("species"), $double("body_mass_g").avg().as("mean_mass_g"))
      .sort("mean_mass_g", false));
"""),
            md("""
## 5 · Sort
"""),
            code("""
// ── TODO 5 ────────────────────────────────────────────────────────────
// The 3 heaviest penguins that actually have a mass (species and body_mass_g).
//
// hint: some penguins have no mass, and missing values sort first — how do you drop
//       those rows before sorting?
""",
                    """
System.out.println(
    df.rows($double("body_mass_g").isNotNull())
      .sort("body_mass_g", false)
      .cols("species", "body_mass_g").select()
      .head(3));
"""),
            md("""
## 6 · Plot

A `dflib` frame plots through **gog4j-dflib**, exactly like the Parquet table
plots through gog4j-hardwood.
"""),
            code("""
// ── TODO 6 ────────────────────────────────────────────────────────────
// Scatter bill_length_mm (x) against body_mass_g (y), coloured by species,
// titled "Penguins", dark theme; then save it as "penguins.svg".
//
// hint: data + aesthetics + points, then labels and the dark theme; display it,
//       then save it.
""",
                    """
var p = Ggplot.ggplot(df,
        Aes.aes().x("bill_length_mm").y("body_mass_g").color("species"))
    .geoms(Geoms.point())
    .labs(Labs.labs("Penguins", "Bill length (mm)", "Body mass (g)"))
    .theme(Theme.theme_dark());
display(p);
save(p, "penguins.svg");
"""),
            code("""
// ── check ─────────────────────────────────────────────────────────────
var counts = df.group("species").agg($col("species"), count());
var pass = df.height() == 344 && gentoo.height() == 124 && counts.height() == 3;
if (pass) {
    println("✓ 344 penguins · 124 Gentoo · 3 species");
} else {
    println("✗ something is off — check the TODOs above");
}
"""),
            md("""
That is the same workflow, end to end, on data that ships with the library.

Back to the [overview](../narrative/00-overview.md).
"""));

    // ----------------------------------------------------------------------
    // Bonus - propagate the whole catalog to now, plot the 3-D point cloud
    // ----------------------------------------------------------------------
    static final List<Cell> ORBITS3D = List.of(
            md("""
# ★ Bonus · Where is everything, right now?

The catalog stores a **TLE** for every object — the orbital elements stamped at
an **epoch**. To draw *right now* you take each TLE, run it forward from its
epoch to the current instant (**propagation**, via **Orekit**'s SGP4/SDP4), and
keep the resulting position.

20,210 objects propagate in well under a second, so this is the whole catalog —
not a sample. Then we plot x / y / z in 3-D with gog4j.
"""),
            code("""
addDependency("org.orekit:orekit:13.0.3");
addDependency("org.dflib:dflib:2.0.0-M7");
addDependency("org.dflib:dflib-parquet:2.0.0-M7");
addDependency("org.jtaccuino:gog4j:0.5-SNAPSHOT");
addDependency("org.jtaccuino:gog4j-hardwood:0.5-SNAPSHOT");

import org.dflib.DataFrame;
import org.dflib.parquet.Parquet;
import org.orekit.data.DataContext;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScale;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.utils.PVCoordinates;
import org.jtaccuino.gog.*;
import org.jtaccuino.gog.labs.Labs;
import org.jtaccuino.gog.render.SvgExporter;
import org.jtaccuino.gog.theme.Theme;
import org.jtaccuino.gog.hardwood.HardwoodTable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;

String num(long v) { return String.format(java.util.Locale.ROOT, "%,d", v); }
String dec(double v, int p) { return String.format(java.util.Locale.ROOT, "%." + p + "f", v); }

// The notebook may live in a sub-folder (exercises/, bonus/, ...), so walk up
// from the notebook's own folder to find the repository's data/ directory.
Path dataset() {
    for (Path p = cwd.toAbsolutePath(); p != null; p = p.getParent()) {
        Path candidate = p.resolve("data/celestrak_gp_catalog.parquet");
        if (Files.isRegularFile(candidate)) return candidate;
    }
    throw new IllegalStateException("data/celestrak_gp_catalog.parquet not found above " + cwd);
}

var df = Parquet.loader().load(dataset());
var tai = DataContext.getDefault().getTimeScales().getTAI();
var now = new AbsoluteDate(Instant.now(), tai);

void save(GgFigure figure, String name) {
    try {
        var path = cwd.resolve(name);
        new SvgExporter().size(1200, 900).batchPoints(true).write(figure, path);
        println("saved %s (%s bytes)", name, num(Files.size(path)));
    } catch (Exception e) {
        println("could not save %s: %s", name, e);
    }
}
"""),
            md("""
## 1 · Propagate every object to now

Build an Orekit `TLE` from the two stored lines, propagate it to `now`, and keep
the position in km. Some objects will not propagate (they were decaying when the
elements were published) — catch and skip those.
"""),
            code("""
// ── given ── the columns we need, and the lists we will fill
var l1 = df.getColumn("tle_line1");
var l2 = df.getColumn("tle_line2");
var names = df.getColumn("object_name");
var classes = df.getColumn("orbit_class");
var kinds = df.getColumn("satcat_object_type");

var xs = new ArrayList<Double>();
var ys = new ArrayList<Double>();
var zs = new ArrayList<Double>();
var plotNames = new ArrayList<String>();
var plotClasses = new ArrayList<String>();
var plotKinds = new ArrayList<String>();
"""),
            code("""
// ── TODO 1 ────────────────────────────────────────────────────────────
// For each row with TLE lines, propagate to `now` and append the position
// (km) and its labels to the lists above. Skip objects that will not propagate.
//
// hint: build a TLE from its two lines, then ask a TLEPropagator to propagate it to
//       `now`; read the position x/y/z in km. Catch and skip the ones that fail.
""",
                    """
for (int i = 0; i < df.height(); i++) {
    Object a = l1.get(i), b = l2.get(i);
    if (a == null || b == null) continue;
    try {
        var tle = new TLE(a.toString(), b.toString(), tai);
        var pv = TLEPropagator.selectExtrapolator(tle).propagate(now).getPVCoordinates();
        xs.add(pv.getPosition().getX() / 1000.0);
        ys.add(pv.getPosition().getY() / 1000.0);
        zs.add(pv.getPosition().getZ() / 1000.0);
        plotNames.add(String.valueOf(names.get(i)));
        plotClasses.add(String.valueOf(classes.get(i)));
        plotKinds.add(String.valueOf(kinds.get(i)));
    } catch (Exception e) {
        // this object will not propagate - skip it
    }
}
"""),
            code("""
// ── given ─────────────────────────────────────────────────────────────
var radii = new ArrayList<Double>();
for (int i = 0; i < xs.size(); i++) {
    radii.add(Math.sqrt(xs.get(i) * xs.get(i) + ys.get(i) * ys.get(i) + zs.get(i) * zs.get(i)));
}
double minR = radii.stream().mapToDouble(Double::doubleValue).min().orElse(0);
double maxR = radii.stream().mapToDouble(Double::doubleValue).max().orElse(0);
println("propagated %s of %s objects to the current instant", num(xs.size()), num(df.height()));
println("distance from Earth centre: %s km .. %s km", num((long) minR), num((long) maxR));
"""),
            md("""
## 2 · The 3-D point cloud

Look at it from outside: x / y / z, coloured by orbit class. The shells **are**
the orbit families — a dense ball (LEO), a sparse shell (MEO), one thin ring
(GEO), and long streaky arcs (HEO).
"""),
            code("""
// ── TODO 2 ────────────────────────────────────────────────────────────
// Build a HardwoodTable from x / y / z and the labels, then plot a 3-D
// scatter coloured by orbit_class into `p`.
//
// hint: turn the lists into a table, then start a 3-D plot — how do you map x, y
//       and z, and which point geom is the 3-D one?
""",
                    """
var cols = new LinkedHashMap<String, List<?>>();
cols.put("x", xs);
cols.put("y", ys);
cols.put("z", zs);
cols.put("orbit_class", plotClasses);
cols.put("object_name", plotNames);
cols.put("satcat_object_type", plotKinds);
var cloud = HardwoodTable.ofColumns(cols);

Plot<HardwoodTable> p = Ggplot.ggplot3d(cloud,
        Aes.aes().x("x").y("y").z("z").color("orbit_class"))
    .geoms(Geoms.point3d())
    .labs(Labs.labs("The whole catalog, right now", "x (km)", "y (km)"))
    .theme(Theme.theme_dark());
"""),
            code("""
// ── given ─────────────────────────────────────────────────────────────
display(p);
save(p, "orbits-now-3d.svg");
"""),
            md("""
## 3 · A flat view: payloads and debris

Seen from above the pole, colour by `satcat_object_type`. The geostationary ring
is obvious, and the debris from the three big break-ups shows up as clumps.
"""),
            code("""
// ── TODO 3 ────────────────────────────────────────────────────────────
// Same table, but look straight down: x vs y, coloured by satcat_object_type.
//
// hint: the same table, but only x and y — coloured by the object-type column.
""",
                    """
var top = Ggplot.ggplot(cloud,
        Aes.aes().x("x").y("y").color("satcat_object_type"))
    .geoms(Geoms.point())
    .labs(Labs.labs("Top-down: payload vs debris", "x (km)", "y (km)"))
    .theme(Theme.theme_dark());
display(top);
save(top, "orbits-now-topdown.svg");
"""),
            code("""
// ── check ─────────────────────────────────────────────────────────────
var pass = xs.size() >= 20000 && xs.size() <= 21207
        && xs.size() == ys.size() && xs.size() == zs.size();
if (pass) {
    println("✓ %s objects propagated to now · %s km .. %s km from Earth centre",
            num(xs.size()), num((long) minR), num((long) maxR));
} else {
    println("✗ propagated %s objects — check the TODOs above", num(xs.size()));
}
"""),
            md("""
Every point is a real position, computed from a published element set at the
moment you ran the cell. Reload and run it again tomorrow and the cloud is
different.

Back to the [overview](../narrative/00-overview.md).
"""));

    // ----------------------------------------------------------------------
    // Wow - redraw the gog4j 3-D plot a couple of times a second
    // ----------------------------------------------------------------------
    static final List<Cell> ORBITS_LIVE = List.of(
            md("""
# ★ Wow · a live 3-D view

The bonus notebook draws a still point cloud. Here we **redraw the gog4j plot a
couple of times a second** with freshly propagated positions, so the fleet
appears to orbit the Earth.

The trick: `display(...)` the container **once**, then swap a new plot into it on
every tick. The notebook keeps that node in the live scene graph, so the swap
shows.
"""),
            code("""
addDependency("org.orekit:orekit:13.0.3");
addDependency("org.dflib:dflib:2.0.0-M7");
addDependency("org.dflib:dflib-parquet:2.0.0-M7");
addDependency("org.jtaccuino:gog4j:0.5-SNAPSHOT");
addDependency("org.jtaccuino:gog4j-hardwood:0.5-SNAPSHOT");

import org.dflib.DataFrame;
import org.dflib.parquet.Parquet;
import org.orekit.data.DataContext;
import org.orekit.time.AbsoluteDate;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.jtaccuino.gog.*;
import org.jtaccuino.gog.labs.Labs;
import org.jtaccuino.gog.render.SvgExporter;
import org.jtaccuino.gog.theme.Theme;
import org.jtaccuino.gog.hardwood.HardwoodTable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.animation.AnimationTimer;
import javafx.scene.layout.StackPane;
import static org.dflib.Exp.*;

String num(long v) { return String.format(java.util.Locale.ROOT, "%,d", v); }
String dec(double v, int p) { return String.format(java.util.Locale.ROOT, "%." + p + "f", v); }

// The notebook may live in a sub-folder (exercises/, bonus/, ...), so walk up
// from the notebook's own folder to find the repository's data/ directory.
Path dataset() {
    for (Path p = cwd.toAbsolutePath(); p != null; p = p.getParent()) {
        Path candidate = p.resolve("data/celestrak_gp_catalog.parquet");
        if (Files.isRegularFile(candidate)) return candidate;
    }
    throw new IllegalStateException("data/celestrak_gp_catalog.parquet not found above " + cwd);
}

var df = Parquet.loader().load(dataset());
var tai = DataContext.getDefault().getTimeScales().getTAI();
var now = new AbsoluteDate(Instant.now(), tai);

void save(GgFigure figure, String name) {
    try {
        var path = cwd.resolve(name);
        new SvgExporter().size(1200, 900).batchPoints(true).write(figure, path);
        println("saved %s (%s bytes)", name, num(Files.size(path)));
    } catch (Exception e) {
        println("could not save %s: %s", name, e);
    }
}
"""),
            md("""
## 1 · A fleet and its frames

Take a few hundred **LEO** objects and work out where each one is at a series of
instants — one instant per frame. Precomputing the frames keeps the drawing loop
cheap; the loop itself only re-renders.
"""),
            code("""
// ── given ── the fleet, and one position per object per frame
var leo = df.rows($str("orbit_class").eq("LEO")).select();
var leoL1 = leo.getColumn("tle_line1");
var leoL2 = leo.getColumn("tle_line2");
var leoClass = leo.getColumn("orbit_class");

int fleetSize = 500;          // objects drawn at once
int frames = 60;              // one lap of the flip-book
double secondsPerFrame = 90;  // real time advanced per frame

var fleetL1 = new ArrayList<String>();
var fleetL2 = new ArrayList<String>();
var fleetClass = new ArrayList<String>();
for (int i = 0; i < leo.height() && fleetL1.size() < fleetSize; i++) {
    Object a = leoL1.get(i), b = leoL2.get(i);
    if (a != null && b != null) {
        fleetL1.add(a.toString());
        fleetL2.add(b.toString());
        fleetClass.add(String.valueOf(leoClass.get(i)));
    }
}

// positions[frame][object] = x, y, z in km
double[][][] positions = new double[frames][fleetL1.size()][3];
for (int f = 0; f < frames; f++) {
    var at = now.shiftedBy(f * secondsPerFrame);
    for (int i = 0; i < fleetL1.size(); i++) {
        try {
            var tle = new TLE(fleetL1.get(i), fleetL2.get(i), tai);
            var pos = TLEPropagator.selectExtrapolator(tle).propagate(at)
                        .getPVCoordinates().getPosition();
            positions[f][i][0] = pos.getX() / 1000.0;
            positions[f][i][1] = pos.getY() / 1000.0;
            positions[f][i][2] = pos.getZ() / 1000.0;
        } catch (Exception e) {
            // this object will not propagate - leave it at the origin
        }
    }
}
println("precomputed %d frames for %s objects", frames, num(fleetL1.size()));
"""),
            md("""
## 2 · Redraw the plot, twice a second

Build a gog4j 3-D plot for one frame, and swap it into a container we displayed
once. An `AnimationTimer` throttles itself to ~2 fps — enough to read as motion,
cheap enough to re-render a few hundred points.
"""),
            code("""
// ── given ── a fresh plot for one frame
java.util.function.IntFunction<Plot<HardwoodTable>> plotFor = f -> {
    var xs = new ArrayList<Double>();
    var ys = new ArrayList<Double>();
    var zs = new ArrayList<Double>();
    for (int i = 0; i < fleetL1.size(); i++) {
        xs.add(positions[f][i][0]);
        ys.add(positions[f][i][1]);
        zs.add(positions[f][i][2]);
    }
    var cols = new LinkedHashMap<String, List<?>>();
    cols.put("x", xs);
    cols.put("y", ys);
    cols.put("z", zs);
    cols.put("orbit_class", fleetClass);
    var table = HardwoodTable.ofColumns(cols);
    var plot = Ggplot.ggplot3d(table, Aes.aes().x("x").y("y").z("z").color("orbit_class"))
        .geoms(Geoms.point3d())
        .labs(Labs.labs("LEO, live", "x (km)", "y (km)"))
        .theme(Theme.theme_dark());
    plot.setPrefSize(780, 600);
    return plot;
};
println("plotFor is ready");
"""),
            code("""
// ── given ── display once, then swap the plot on every tick
var stage = new StackPane();
stage.setPrefSize(780, 600);
display(stage);
stage.getChildren().setAll(plotFor.apply(0));

var frameNo = new AtomicInteger();
var timer = new AnimationTimer() {
    private long last;
    @Override
    public void handle(long nanos) {
        if (nanos - last < 500_000_000L) return;   // ~ 2 frames per second
        last = nanos;
        int f = frameNo.getAndIncrement() % frames;
        stage.getChildren().setAll(plotFor.apply(f));
    }
};
timer.start();
println("redrawing the plot twice a second — %s points per frame", num(fleetL1.size()));
"""),
            code("""
// ── given ── one frame as a file, for the record
save(plotFor.apply(0), "orbits-live-frame.svg");
"""),
            code("""
// ── check ─────────────────────────────────────────────────────────────
var pass = fleetL1.size() > 100 && frames > 10 && positions.length == frames;
if (pass) {
    println("✓ %s points × %d frames — redrawn live at ~2 fps", num(fleetL1.size()), frames);
} else {
    println("✗ something is off — check the setup above");
}
"""),
            md("""
Reload the notebook and run it again tomorrow: the fleet is somewhere else, and
the animation follows the *current* element sets.

It is the same pipeline as the other notebooks — **Hardwood** reads, **dflib**
shapes, **Orekit** propagates, **gog4j** draws — just on a clock.

Back to the [overview](../narrative/00-overview.md).
"""));

    // ----------------------------------------------------------------------

    static String idFor(String seed, int index) {
        return UUID.nameUUIDFromBytes((seed + "#" + index).getBytes(StandardCharsets.UTF_8)).toString();
    }

    static Map<String, Object> build(String seed, List<Cell> cells, boolean solution) {
        List<Object> cellArray = new ArrayList<>();
        int index = 0;
        for (Cell c : cells) {
            Map<String, Object> node = new LinkedHashMap<>();
            if (c.kind().equals("md")) {
                node.put("cell_type", "markdown");
                node.put("id", idFor(seed, index));
                node.put("metadata", new LinkedHashMap<>());
                node.put("source", c.text());
            } else {
                node.put("cell_type", "code");
                node.put("execution_count", 0);
                node.put("id", idFor(seed, index));
                node.put("metadata", new LinkedHashMap<>());
                node.put("outputs", new ArrayList<>());
                node.put("source", solution ? c.sol() : c.text());
            }
            cellArray.add(node);
            index++;
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("cells", cellArray);
        Map<String, Object> metadata = new LinkedHashMap<>();
        Map<String, Object> kernel = new LinkedHashMap<>();
        kernel.put("name", "JTaccuino");
        kernel.put("version", "0.1");
        metadata.put("kernel_info", kernel);
        Map<String, Object> language = new LinkedHashMap<>();
        language.put("name", "Java");
        language.put("version", "27");
        metadata.put("language_info", language);
        root.put("metadata", metadata);
        root.put("nbformat", 4);
        root.put("nbformat_minor", 5);
        return root;
    }

    static void write(Path path, String seed, List<Cell> cells, boolean solution) throws IOException {
        Files.createDirectories(path.getParent());
        StringBuilder sb = new StringBuilder();
        writeJson(sb, build(seed, cells, solution), 0);
        Files.writeString(path, sb + "\n", StandardCharsets.UTF_8);
        System.out.println("wrote " + path);
    }

    static void writeJson(StringBuilder sb, Object value, int indent) {
        String pad = "    ".repeat(indent);
        String pad2 = "    ".repeat(indent + 1);
        if (value == null) { sb.append("null"); return; }
        if (value instanceof Map<?, ?> map) {
            if (map.isEmpty()) { sb.append("{}"); return; }
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                sb.append(pad2).append(jsonString(String.valueOf(e.getKey()))).append(": ");
                writeJson(sb, e.getValue(), indent + 1);
                if (++i < map.size()) sb.append(',');
                sb.append('\n');
            }
            sb.append(pad).append('}');
            return;
        }
        if (value instanceof List<?> list) {
            if (list.isEmpty()) { sb.append("[]"); return; }
            sb.append("[\n");
            for (int i = 0; i < list.size(); i++) {
                sb.append(pad2);
                writeJson(sb, list.get(i), indent + 1);
                if (i + 1 < list.size()) sb.append(',');
                sb.append('\n');
            }
            sb.append(pad).append(']');
            return;
        }
        if (value instanceof String s) { sb.append(jsonString(s)); return; }
        sb.append(value);
    }

    static String jsonString(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                case '\b' -> b.append("\\b");
                case '\f' -> b.append("\\f");
                default -> {
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
                }
            }
        }
        return b.append('"').toString();
    }

    static void notebook(String name, List<Cell> cells) throws IOException {
        write(Path.of("notebooks", "exercises", name + ".ipynb"), name, cells, false);
        write(Path.of("notebooks", "solutions", name + ".ipynb"), name, cells, true);
    }

    public static void main(String[] args) throws IOException {
        notebook("01-jtaccuino-basics", E1);
        notebook("02-parquet-with-hardwood", E2);
        notebook("03-dataframes-with-dflib", E3);
        notebook("04-plotting-with-gog4j", E4);

        write(Path.of("notebooks", "fallback", "penguins-worksheet.ipynb"),
                "penguins-worksheet", PENGUINS, false);
        write(Path.of("notebooks", "fallback", "penguins-worksheet-solutions.ipynb"),
                "penguins-worksheet", PENGUINS, true);

        write(Path.of("notebooks", "bonus", "orbits-now-3d.ipynb"),
                "orbits-now-3d", ORBITS3D, false);
        write(Path.of("notebooks", "bonus", "orbits-now-3d-solutions.ipynb"),
                "orbits-now-3d", ORBITS3D, true);

        write(Path.of("notebooks", "bonus", "orbits-live-3d.ipynb"),
                "orbits-live-3d", ORBITS_LIVE, false);
        write(Path.of("notebooks", "bonus", "orbits-live-3d-solutions.ipynb"),
                "orbits-live-3d", ORBITS_LIVE, true);
    }
}
