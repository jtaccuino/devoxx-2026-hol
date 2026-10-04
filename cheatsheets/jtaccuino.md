# JTaccuino cheat sheet

A notebook is one JShell session, split into cells. Anything you define is
visible in every later cell.

## Built-ins

| Call | Does |
|---|---|
| `println(str)` / `println(fmt, args)` | print into the cell, `fmt` uses `String.formatted` (default locale) |
| `println(obj)` | print any object via its `toString` |
| `display(obj)` | draw a JavaFX node, frame or plot inline |
| `addDependency("group:artifact:version")` | resolve a Maven dependency for the snippets that follow |
| `use("dflib")` | activate an on-demand extension (see below) |
| `cwd` | the working directory as a `java.nio.file.Path` |

## Output: `println` and `display`, never `System.out.println`

`System.out.println` writes to the JVM console — **the notebook shows nothing**.
The builtins are captured by the notebook's print/display sinks.

Extensions make the output nicer. `use("dflib")` activates the dflib extension,
which adds (among other things):

```java
void println(DataFrame df) {
    println(new TabularPrinter().print(df));
}
```

so `println(frame)` renders a **table**. `use("deepnetts")`, `use("file")` and
`use("langchain4j")` activate the other on-demand extensions.

## Rules that bite

A line that is already **complete** ends the snippet.

```java
// NO — the if-line is complete, the else is orphaned
if (ok) println("yes");
else    println("no");
```

```java
// YES — the open brace keeps the snippet alive
if (ok) {
    println("yes");
} else {
    println("no");
}
```

Keep long expressions on **one line**, or open a bracket the continuation stays
inside.

## Numbers and locale

`String.format` follows the **default locale** (a German machine prints
`21.207`). For stable output, format explicitly:

```java
String.format(java.util.Locale.ROOT, "%,d", 21207)   // "21,207"
```

## Displaying a plot

Any JavaFX `Node` works, and a gog4j `Plot` **is** a `Node`:

```java
var plot = Ggplot.ggplot(table, Aes.aes().x("a").y("b")).geoms(Geoms.point());
display(plot);
```

## Threading: display() does it for you

A cell runs on JShell's **worker** thread, *not* on the JavaFX Application
Thread — but in normal notebook code you do not have to care:

- `display(...)` **marshals for you**: `DisplayExtension.display` resolves the
  node on the worker thread and then attaches it with `Platform.runLater`.
  Building a gog4j plot (a plain `Node`) on the worker thread is fine, so just
  `display(plot)`.
- **Re-display is safe.** gog4j's fluent setters mutate the same `Plot` and
  return `this`, so a later cell often calls `display(plot)` on a node that is
  already shown. The sink ignores a node it already holds (as
  `GuiSinks.forOutputBox` does), so there is no "duplicate children" error.
- The one place the FX thread matters is code that touches a **live scene
  graph** yourself. The live 3-D bonus uses an `AnimationTimer`: its
  `handle(...)` already runs on the FX thread, so swapping the plot there is
  safe.
- **Exporting an SVG** (`SvgExporter`) is the other exception — it throws
  *"TextMeasurer ... must be called on the FX Application Thread"* unless it runs
  on the FX thread. The lab notebooks only `display(plot)`; if you add an export,
  marshal it yourself.

## addDependency is a snippet like everything else

A cell is **not** a snippet. `ReactiveJShell.eval` feeds the cell to JShell and
loops over `analyzeCompletion`, so **every statement becomes its own snippet** —
which means `addDependency(...)` and the `import`s below it were already separate
snippets; the cell boundary is irrelevant. `JShell.addToClasspath` is applied
synchronously, so the imports that follow do see the jar.

The lab notebooks therefore just put the `addDependency(...)` calls first, then a
one-line probe naming the jar they are about to use:

```java
try { Class.forName("org.orekit.data.DataContext"); }
catch (ClassNotFoundException notThere) {
    println("warning: org.orekit.data.DataContext is not on the class path yet (%s)",
            notThere.getMessage());
}
```

If you see *"cannot find symbol: TLE"*, that warning tells you the resolver did
not deliver the jar, instead of leaving you guessing.

## A trap with the local execution engine

JShell's **local** engine instruments every snippet with the Class-File API
(JDK 24+, for stop support). To regenerate stack maps it resolves referenced
classes through the **system** class loader — and a class added with
`addToClasspath` cannot be resolved there. So a snippet that puts such a type
(for example Orekit's `TLE`) on a **stack-map merge point** — a branch, loop,
ternary or `try`/`catch` that must merge two different reference types — fails:

```
java.lang.IllegalArgumentException: Could not resolve class TLE
    at ...StackMapGenerator...
    at LocalExecutionControl.instrument(...)
```

Keep such types out of branchy code: put them in one straight-line helper, and
let the branchy caller carry only primitives.

```java
// one straight line of work - no branches, nothing to merge
double[] positionOf(String line1, String line2, AbsoluteDate at, TimeScale scale) {
    var tle = new TLE(line1, line2, scale);
    var pos = TLEPropagator.selectExtrapolator(tle).propagate(at)
                .getPVCoordinates().getPosition();
    return new double[]{pos.getX(), pos.getY(), pos.getZ()};
}
```

Both 3-D bonus notebooks do exactly this. `tools/verify_jshell.java` replays a
notebook against the real local engine, so this class of failure is caught
before it reaches the IDE.
