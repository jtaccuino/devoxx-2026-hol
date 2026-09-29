---
marp: true
theme: default
paginate: true
title: Module 2 · Hardwood + dflib
---

<!-- _class: lead -->

# Module 2 · Hardwood + dflib

### Reading data, and shaping it

`notebooks/exercises/02-parquet-with-hardwood.ipynb`
`notebooks/exercises/03-dataframes-with-dflib.ipynb`

---

# Meet the tool · Hardwood

*A Parquet reader for the JVM — fast, small, no ceremony.*

- read the **footer** for schema, sizes and statistics, without touching data pages
- stream rows through a **filter**, or read whole **column batches** as primitive arrays
- the reader behind this lab's dataset (21,207 rows, 52 columns)

**Get it:** <https://hardwood.dev/>

---

# Meet the tool · dflib

*DataFrames in plain Java. Select, filter, derive, group, sort.*

- a fluent, immutable `DataFrame` API — no SQL, no Python
- 2.0 expressions: `$str(...)`, `$double(...)`, `count()`, `avg()`
- CSV and **Parquet** I/O; reads the catalog straight into a frame

**Get it:** <https://github.com/dflib/dflib>

---

# Parquet is not a text file

It is **columnar**: values of one column sit together, so you can read one
column without reading the others.

That gives you three levels of access — cheapest first:

1. **footer** — shape and statistics, from the last few kilobytes
2. **row readers** — stream rows through a filter
3. **column readers** — a whole column as a primitive array

You pick the level that answers your question.

---

# Level 1 · the footer

21,207 rows, 52 columns, 1 row group, 5.4 MB —

all known **without decompressing a single data page**.

Even the min/max of a column is in the footer:

```
inclination  0.003 .. 142.042   (from the footer only)
```

---

# Level 2 · stream the rows

```java
long debris = 0;
try (RowReader rows = catalog.rowReader()) {
    while (rows.hasNext()) {
        rows.next();
        String t = rows.getString("satcat_object_type");
        if ("DEB".equals(t) || "R/B".equals(t)) debris++;
    }
}
```

One pass, one `long` of state, **3,501** debris.

---

# Level 3 · column batches

```java
try (ColumnReader col = catalog.columnReader("inclination")) {
    while (col.nextBatch()) {
        double[] values = col.getDoubles();
        for (int k = 0; k < col.getValueCount(); k++)
            if (values[k] > maxInc) maxInc = values[k];
    }
}
```

No row objects. No boxing. Just a `double[]`.

---

# dflib is the other half

Hardwood *streams*. **dflib** *shapes*.

```java
var df = Parquet.loader().load(path);
```

Then, in plain Java:

```java
df.cols("object_name", "orbit_class", "apogee_km").select();   // select
df.rows($str("orbit_class").eq("GEO")).select();               // filter
df.group("orbit_class").agg($col("orbit_class"), count());     // group
df.sort("apogee_km", false);                                   // sort
```

---

# Derive and aggregate in one breath

The mean apogee–perigee spread per class:

```java
df.group("orbit_class").agg(
    $col("orbit_class"),
    $double("apogee_km").sub($double("perigee_km")).avg().as("mean_spread_km"))
  .sort("mean_spread_km", false);
```

```
HEO  45,138 km    MEO  1,125 km    GEO  472 km    LEO  43 km
```

A derived column *and* a summary, in one expression.

---

# One real-data wart

`altitude_km` is **NaN for 997 rows** — exactly where SGP4 propagation failed
(decayed objects).

The *analytic* `apogee_km` / `perigee_km` are always present. Real data is
messy; the labs use the columns that are complete.

---

# Your turn

`02-parquet-with-hardwood.ipynb` — **5 TODOs.** Climb the three levels.

`03-dataframes-with-dflib.ipynb` — **6 TODOs.** Select, filter, group, sort.

Plus a ★ bonus: **who owns the sky?** (US 12,980 · PRC 3,634 · CIS 1,486)

~10 minutes each.

> On to **module 3** — pictures.
