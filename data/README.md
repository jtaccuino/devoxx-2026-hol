# Dataset — CelesTrak General Perturbations catalog

`celestrak_gp_catalog.parquet` is the single dataset used throughout this lab.
It is a **snapshot** of every current CelesTrak GP element set, merged into one
row per satellite and enriched with SATCAT catalog metadata.

## Files

| Path | Role | Committed |
|---|---|---|
| `celestrak_gp_catalog.parquet` | the lab dataset (52 columns, 21 207 rows) | yes |
| `raw/gp_*.csv` | 50 raw CelesTrak GP group downloads — exact rebuild inputs | yes |
| `raw/satcat.csv` | CelesTrak SATCAT catalog — enrichment input | yes |
| `raw/manifest.json` | per-file fetch timestamps, sizes, SHA-256, row counts | yes |
| `out/` | derived CSV / long-format / verification output | no (regenerated) |

## Provenance

| | |
|---|---|
| Source | CelesTrak — <https://celestrak.org> |
| GP endpoint | `https://celestrak.org/NORAD/elements/gp.php?GROUP=…&FORMAT=csv` |
| SATCAT endpoint | `https://celestrak.org/pub/satcat.csv` |
| GP fetch (UTC) | `2026-09-23T18:24:14.354Z` — pinned via `raw/manifest.json` |
| SATCAT fetch (UTC) | `2026-09-29` (about 6.75 MB, 70 813 rows) |
| `celestrak_gp_catalog.parquet` SHA-256 | `f5954058c7f16017bf2ab22f31e6123d692b089b55bb8a1ebdbb0fbc8a16443d` |
| `raw/satcat.csv` SHA-256 | `d9bfb90caedba0659dea6825e05853af01d549050b1613968a042c1b2fbda63d` |

> **Attribution.** Satellite element sets and catalog data are derived from
> CelesTrak (`celestrak.org`). CelesTrak asks users to download at most once
> per two-hour update window; `dataset/fetch.java` enforces this (120-minute
> TTL plus a 1.5 s gap between requests). Please keep the attribution if you
> redistribute this snapshot.

## Shape

- **21 207 rows** — one per NORAD catalog object
- **52 columns** — 38 OMM/derived + 14 `satcat_*`
- Written with dflib's Parquet writer, single row group, GZIP

## Columns

OMM elements (from the GP files): `norad_cat_id`, `object_name`, `object_id`,
`epoch`, `mean_motion`, `eccentricity`, `inclination`, `ra_of_asc_node`,
`arg_of_pericenter`, `mean_anomaly`, `mean_motion_dot`, `mean_motion_ddot`,
`bstar`, `ephemeris_type`, `classification_type`, `element_set_no`,
`rev_at_epoch`.

Reconstructed TLE: `tle_line1`, `tle_line2`, `tle3_line0`, `tle`, `tle3`,
`tle_available`, `tle_checksum_ok`, `tle_orekit_ok`.

Derived orbital quantities: `r_km`, `v_km_s`, `altitude_km`,
`semimajor_axis_km`, `period_min`, `apogee_km`, `perigee_km`, `orbit_class`,
`epoch_age_hours`.

Provenance: `norad_groups`, `source_kind`, `source_url`, `fetch_utc`.

SATCAT enrichment: `satcat_object_type`, `satcat_ops_status_code`,
`satcat_owner`, `satcat_launch_date`, `satcat_launch_site`, `satcat_decay_date`,
`satcat_period`, `satcat_inclination`, `satcat_apogee`, `satcat_perigee`,
`satcat_rcs`, `satcat_data_status_code`, `satcat_orbit_center`,
`satcat_orbit_type`.

## Facts worth knowing

- **Orbit classes** (`orbit_class`, from `Orbital.orbitClass`):
  LEO 19 260 · GEO 1 183 · HEO 558 · MEO 206.
- **Object types** (`satcat_object_type`):
  PAY 17 110 · DEB 2 957 · R/B 544 · UNK 3 · **unmatched 593**.
- **The 593 unmatched rows** are objects with no SATCAT entry; every
  `satcat_*` column is `null` for them. `satcat_apogee` is therefore `NaN` for
  593 rows.
- **`altitude_km` is `NaN` for 997 rows** — exactly the rows where
  `tle_orekit_ok = false` (SGP4 propagation failed, typically decayed /
  decaying objects). The *analytic* `apogee_km` / `perigee_km` are complete,
  so prefer those for plotting.
- **Owners** (`satcat_owner`, 104 distinct): US 12 980 · PRC 3 634 ·
  CIS 1 486 · UK 704.
- **The catalog is Starlink-dominated**: the group-membership string
  `active,starlink` alone covers 10 952 objects.
- **`epoch_age_hours`** ranges from −98 h to 715 h (mean ≈ 22.7 h); the
  negative end is objects whose GP epoch is slightly ahead of the pinned
  `fetch_utc`.

## Rebuilding

See [`../dataset/README.md`](../dataset/README.md). Because `raw/` is
committed, `jbang dataset/build.java` reproduces the parquet **offline and
deterministically** — `build.java` sorts its inputs, accumulates into a
`TreeMap`, and pins `fetch_utc` from the manifest, so it never consults the
wall clock or the network.
