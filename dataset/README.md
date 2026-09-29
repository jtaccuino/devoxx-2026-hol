# Dataset build — recreation infrastructure

These [jbang](https://jbang.dev) scripts turn raw CelesTrak downloads into the
single Parquet file the lab uses. They are the *provenance* of
[`../data/celestrak_gp_catalog.parquet`](../data/README.md); students never run
them, but anyone can reproduce or refresh the dataset.

The inputs are **committed** in `../data/raw/`, so a rebuild needs no network
and produces a byte-identical object set.

## Scripts

| Script | Deps | Does |
|---|---|---|
| `fetch.java` | jackson-databind 2.17.2 | downloads 50 GP group/special sets + SATCAT into `data/raw/`, with a 120-min TTL and 1.5 s spacing |
| `build.java` | dflib / dflib-csv / dflib-parquet 2.0.0-M6, hardwood-core 1.1.0.Beta1, orekit 13.0.3 | merges GP files into the 52-column catalog, reconstructs TLEs, derives orbital quantities, enriches from SATCAT |
| `verify.java` | dflib / dflib-csv 2.0.0-M6, orekit 13.0.3 | independent cross-checks (element diffs, stale objects, outliers) |
| `analyze.java` | dflib / dflib-csv 2.0.0-M6 | quick descriptive statistics |
| `read-check.java` | hardwood-core 1.1.0.Beta1, dflib / dflib-csv 2.0.0-M6 | reads the Parquet back with **hardwood** and cross-checks the row count against the CSV |
| `Tle.java` / `Orbital.java` | — | TLE reconstruction + checksums; classical orbital-element formulas. Pulled in by `build.java` via `//SOURCES` |

All scripts declare `//JAVA 21` and are run from the **repository root** (they
resolve `data/raw` and `data/out` relative to the working directory).

## Rebuild from the committed inputs (offline, deterministic)

```bash
jbang dataset/build.java        # -> data/out/celestrak_gp_catalog.parquet
jbang dataset/read-check.java   # hardware read-back + CSV row-count check
```

`build.java` is deterministic: it sorts the GP files, accumulates into a
`TreeMap<Long, …>` keyed by NORAD id, and pins `fetch_utc` to the maximum
`fetchedAtUtc` in `data/raw/manifest.json`. No wall clock, no network.

## Refresh from CelesTrak

```bash
jbang dataset/fetch.java            # respects the 120-min TTL; cached files are reused
jbang dataset/fetch.java --force    # refetch everything
jbang dataset/fetch.java --ttl=720  # 12-hour TTL
```

CelesTrak asks for **at most one download per two-hour update window**;
`fetch.java` enforces this. On a `403 … has not updated since your last`
response it *defers* that source and continues rather than retrying. Refetching
GP files changes the epochs, so re-run `build.java` afterwards and update the
SHA-256 in `../data/README.md`.

## Publish

`build.java` writes to `data/out/` (git-ignored). The committed lab dataset is
the published copy:

```bash
cp data/out/celestrak_gp_catalog.parquet data/celestrak_gp_catalog.parquet
```

## Note on Java versions

`build.java` / `fetch.java` run on **Java 21**. The *notebooks* that plot this
data need **Java 26+**, because `gog4j` (including `gog4j-hardwood`) is compiled
to class-file version 70. The build scripts deliberately depend on `hardwood-core`
directly, not on `gog4j-hardwood`, so they stay on 21.
