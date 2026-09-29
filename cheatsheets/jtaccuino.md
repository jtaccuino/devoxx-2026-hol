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
