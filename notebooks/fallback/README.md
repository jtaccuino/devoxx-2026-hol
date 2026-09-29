# Fallback worksheet

An escape hatch for anyone who cannot read the CelesTrak Parquet — missing file,
broken download, slow machine.

The **[penguins](penguins-worksheet.ipynb)** worksheet repeats the whole
data-and-plot workflow (select, filter, group, sort, plot) on the small
**penguins** dataset that ships *inside* gog4j. No file, no download, no
Parquet:

```
org.jtaccuino:gog4j-data          # the bundled CSV resources
org.jtaccuino:gog4j-dflib-data    # PenguinsDatasets.loadPenguins() -> DataFrame
```

It has the same **TODO** structure as the main notebooks, so a student who is
blocked still finishes the exercise and sees every idea. The matching solution
is `penguins-worksheet-solutions.ipynb`.

`DiamondsDatasets` / `HardwoodDiamondsDatasets` (53 940 rows) are also bundled,
if you want a larger dataset for a fast-finisher bonus.
