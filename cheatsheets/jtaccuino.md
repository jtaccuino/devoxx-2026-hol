# JTaccuino cheat sheet

A notebook is one JShell session, split into cells. Anything you define is
visible in every later cell.

## Built-ins

| Call | Does |
|---|---|
| `println(str)` / `println(fmt, args)` | print, `fmt` uses `String.formatted` (default locale) |
| `display(node)` | draw a JavaFX node inline |
| `addDependency("group:artifact:version")` | resolve a Maven dependency into the session — no quotes needed for the file, no semicolon |
| `cwd` | the working directory as a `java.nio.file.Path` |

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

## Threading: JavaFX work belongs on the FX thread

A cell runs on JShell's **worker** thread, *not* on the JavaFX Application
Thread. Anything that touches JavaFX — building a plot's canvas, `SvgExporter`,
adding a node to a live scene — must be marshalled there:

```java
void onFx(Runnable work) {
    if (javafx.application.Platform.isFxApplicationThread()) { work.run(); return; }
    var latch = new java.util.concurrent.CountDownLatch(1);
    javafx.application.Platform.runLater(() -> {
        try { work.run(); } finally { latch.countDown(); }
    });
    latch.await();
}
```

- `display(...)` already marshals for you.
- **Exporting** must go through `onFx(...)` — `SvgExporter` throws *"TextMeasurer...
  must be called on the FX Application Thread"* otherwise.
- After mutating a plot that is **already displayed**, call `onFx(plot::markDirty)`
  — gog4j's fluent setters do not repaint on their own, so the change would never
  reach the screen.

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
