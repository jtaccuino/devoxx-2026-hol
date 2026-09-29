# dflib cheat sheet

A DataFrame you shape in plain Java. Dependency (as used by gog4j):
`org.dflib:dflib:2.0.0-M7` — the API below is **2.0**.

## Load

```java
DataFrame df = Parquet.loader().load(path);   // dflib-parquet
DataFrame df = Csv.load(path.toString());     // dflib-csv
```

## Select columns

`cols(...)` returns a `ColumnSet`; `select()` turns it back into a frame:

```java
df.cols("object_name", "orbit_class", "apogee_km").select();
df.cols("name").selectAs("label");            // rename on the way out
```

## Filter rows

Conditions come from expressions:

```java
df.rows($str("orbit_class").eq("GEO")).select();
df.rows($double("eccentricity").gt(0.25)).select();
df.rows($str("t").eq("DEB").or($str("t").eq("R/B"))).select();
df.rowsRange(0, 100).select();
```

## Derive a column

`select(Exp...)` needs **one expression per selected column**; name results
with `.as(...)`.

```java
df.cols("object_name", "apogee_km", "perigee_km").select(
    $str("object_name"),
    $double("apogee_km"),
    $double("apogee_km").sub($double("perigee_km")).as("spread_km"));
```

## Group and aggregate

`df.group(col).agg(Exp...)` — this is where aliases work:

```java
df.group("orbit_class").agg($col("orbit_class"), count());
df.group("orbit_class").agg(
    $col("orbit_class"),
    $double("inclination").avg().as("mean_inc"));
```

Numeric aggregations: `sum() avg() min() max() median() stdDev()`.

## Sort

```java
df.sort("apogee_km", true);    // ascending
df.sort("apogee_km", false);   // descending  ← note: the flag is *ascending*
```

## Inspect

```java
df.height();  df.width();
df.head(5);
System.out.println(df.head(5));   // in JShell, print explicitly
df.toMaps();                      // list of row maps
```

## Static expression helpers

```java
import static org.dflib.Exp.*;
$col("x")  $str("x")  $int("x")  $double("x")  $long("x")
count()  and(...)  or(...)  not(...)
```
