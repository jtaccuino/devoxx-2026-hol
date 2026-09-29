# Hardwood cheat sheet

Parquet read three ways, cheapest first. Dependency:
`dev.hardwood:hardwood-core:1.1.0.Beta1`.

## Open the file

```java
var catalog = ParquetFileReader.open(InputFile.of(path));
var meta = catalog.getFileMetaData();          // footer
var schema = catalog.getFileSchema();          // columns
```

## 1 · Footer — shape, no data pages

```java
meta.numRows();
meta.rowGroups().size();
meta.createdBy();

for (var c : meta.rowGroups().get(0).columns()) {
    var cm = c.metaData();
    cm.pathInSchema().leafName();   // column name
    cm.codec();                     // GZIP
    cm.totalCompressedSize();
    cm.totalUncompressedSize();
    cm.statistics().minValue();     // raw bytes
}
```

Decode statistics by physical type:

```java
StatisticsDecoder.decodeLong(bytes);
StatisticsDecoder.decodeDouble(bytes);
StatisticsDecoder.decodeBoolean(bytes);
// strings: new String(bytes)
```

**Caution:** a `max` over a column containing `NaN` decodes to `NaN` — the
statistic is then useless for pruning. `altitude_km` in this dataset is such a
column.

## 2 · Row reader — which rows

```java
try (RowReader rows = catalog.rowReader()) {
    while (rows.hasNext()) {
        rows.next();
        rows.getLong("norad_cat_id");
        rows.getDouble("apogee_km");
        rows.getString("object_name");
    }
}
```

With a projection and a filter (a filtered column must be projected too):

```java
try (RowReader rows = catalog.buildRowReader()
        .projection(ColumnProjection.columns("norad_cat_id", "orbit_class"))
        .filter(FilterPredicate.eq("orbit_class", "GEO"))
        .build()) { ... }
```

Predicates: `FilterPredicate.eq / gt / inStrings / and / or / not`.

## 3 · Column reader — what the values are

```java
try (ColumnReader col = catalog.columnReader("inclination")) {
    while (col.nextBatch()) {
        double[] values = col.getDoubles();
        int n = col.getValueCount();          // batch-scoped
    }
}
```

Aligned multi-column batches:

```java
try (ColumnReaders cols = catalog.columnReaders(
        ColumnProjection.columns("altitude_km", "inclination"))) {
    while (cols.nextBatch()) {
        int n = cols.getRecordCount();
        double[] a = cols.getColumnReader("altitude_km").getDoubles();
        double[] i = cols.getColumnReader("inclination").getDoubles();
    }
}
```

## Which level?

| Question | Level |
|---|---|
| how big / how chunked / column range? | footer |
| which rows? | row reader + filter |
| arithmetic over values? | column reader |
| random access, joins, sorting? | a DataFrame (dflib) |
