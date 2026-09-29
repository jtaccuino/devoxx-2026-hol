///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//DEPS org.dflib:dflib:2.0.0-M6
//DEPS org.dflib:dflib-csv:2.0.0-M6
//DEPS org.dflib:dflib-parquet:2.0.0-M6
//DEPS dev.hardwood:hardwood-core:1.1.0.Beta1
//DEPS org.orekit:orekit:13.0.3
//SOURCES Tle.java, Orbital.java

// Merge all raw CelesTrak GP element sets into one wide, interpretable catalog:
//   - one row per NORAD object (latest epoch across groups)
//   - every OMM element as a typed column
//   - reconstructed TLE (2LE) and 3LE text columns
//   - derived orbital quantities + orbit class + data age
//   - SATCAT metadata enrichment (owner, object type, launch/decay dates, ...)
//   - provenance (group memberships, fetch time, source kind)
// Emits celestrak_gp_catalog.csv, celestrak_gp_catalog.parquet, celestrak_gp_by_group.csv.
//
// Usage: jbang build.java

import org.dflib.DataFrame;
import org.dflib.Printers;
import org.dflib.builder.DataFrameArrayAppender;
import org.dflib.builder.DataFrameArrayByRowBuilder;
import org.dflib.csv.Csv;
import org.dflib.parquet.CompressionCodec;
import org.dflib.parquet.Parquet;
import org.dflib.row.RowProxy;
import org.orekit.data.DataContext;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.TimeScale;
import org.orekit.utils.PVCoordinates;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class build {

    static final String[] OMM_COLS = {
            "OBJECT_NAME", "OBJECT_ID", "EPOCH", "MEAN_MOTION", "ECCENTRICITY", "INCLINATION",
            "RA_OF_ASC_NODE", "ARG_OF_PERICENTER", "MEAN_ANOMALY", "EPHEMERIS_TYPE",
            "CLASSIFICATION_TYPE", "NORAD_CAT_ID", "ELEMENT_SET_NO", "REV_AT_EPOCH",
            "BSTAR", "MEAN_MOTION_DOT", "MEAN_MOTION_DDOT"
    };

    static final String[] SATCAT_COLS = {
            "object_type", "ops_status_code", "owner", "launch_date", "launch_site", "decay_date",
            "period", "inclination", "apogee", "perigee", "rcs", "data_status_code",
            "orbit_center", "orbit_type"
    };

    static String[] wideCols(boolean includeSatcat) {
        if (!includeSatcat) return Arrays.copyOf(WIDE_COLS, WIDE_COLS.length);
        String[] cols = Arrays.copyOf(WIDE_COLS, WIDE_COLS.length + SATCAT_COLS.length);
        for (int i = 0; i < SATCAT_COLS.length; i++) {
            cols[WIDE_COLS.length + i] = "satcat_" + SATCAT_COLS[i];
        }
        return cols;
    }

    static final String[] WIDE_COLS = {
            "norad_cat_id", "object_name", "object_id", "epoch",
            "mean_motion", "eccentricity", "inclination", "ra_of_asc_node", "arg_of_pericenter", "mean_anomaly",
            "mean_motion_dot", "mean_motion_ddot", "bstar", "ephemeris_type", "classification_type",
            "element_set_no", "rev_at_epoch",
            "tle_line1", "tle_line2", "tle3_line0", "tle", "tle3", "tle_available", "tle_checksum_ok",
            "tle_orekit_ok", "r_km", "v_km_s", "altitude_km",
            "semimajor_axis_km", "period_min", "apogee_km", "perigee_km", "orbit_class", "epoch_age_hours",
            "norad_groups", "source_kind", "source_url", "fetch_utc"
    };

    static final class Accum {
        String epochStr;
        Map<String, String> row = new LinkedHashMap<>();
        TreeSet<String> groups = new TreeSet<>();
    }

    record OrekitResult(boolean ok, double rKm, double vKmS, double altKm) {}

    // Independent validation + propagation with Orekit (SGP4/SDP4). Uses the TAI timescale so no
    // UTC-TAI leap-second data file is required; the element round-trip and state vectors are
    // unaffected by that choice. Orekit reports positions in meters; we convert to km.
    static OrekitResult orekitVerify(String line1, String line2) {
        TimeScale tai = DataContext.getDefault().getTimeScales().getTAI();
        try {
            TLE t = new TLE(line1, line2, tai);
            PVCoordinates pv = TLEPropagator.selectExtrapolator(t).propagate(t.getDate()).getPVCoordinates();
            double rM = pv.getPosition().getNorm();
            double vMS = pv.getVelocity().getNorm();
            return new OrekitResult(true, rM / 1000.0, vMS / 1000.0, rM / 1000.0 - Orbital.R_E);
        } catch (Exception e) {
            return new OrekitResult(false, Double.NaN, Double.NaN, Double.NaN);
        }
    }

    public static void main(String[] args) throws Exception {
        Path raw = Path.of("data", "raw");
        Path out = Path.of("data", "out");
        Files.createDirectories(out);

        String fetchUtc = maxFetchUtc(raw.resolve("manifest.json"));
        Instant fetchInstant = Instant.parse(fetchUtc);

        List<Path> gpFiles = Files.list(raw)
                .filter(p -> p.getFileName().toString().startsWith("gp_"))
                .filter(p -> p.getFileName().toString().endsWith(".csv"))
                .sorted()
                .toList();
        if (gpFiles.isEmpty()) {
            System.err.println("No raw GP files found in " + raw + ". Run fetch.java first.");
            System.exit(1);
        }

        Map<Long, Map<String, String>> satcat = loadSatcat(raw.resolve("satcat.csv"));

        // ---- union all groups; dedupe by NORAD_CAT_ID keeping latest EPOCH; collect memberships ----
        Map<Long, Accum> byCat = new TreeMap<>();
        List<Object[]> longRows = new ArrayList<>();

        for (Path f : gpFiles) {
            String group = f.getFileName().toString().substring(3).replace(".csv", "");
            DataFrame df = Csv.loader().load(f); // all columns as String
            for (RowProxy r : df) {
                String catnrS = str(r, "NORAD_CAT_ID");
                if (catnrS == null || catnrS.isBlank()) continue;
                long catnr;
                try {
                    catnr = Long.parseLong(catnrS.trim());
                } catch (NumberFormatException e) {
                    continue;
                }

                String epochS = str(r, "EPOCH");
                longRows.add(new Object[]{catnr, group, epochS, str(r, "OBJECT_NAME")});

                Accum acc = byCat.get(catnr);
                if (acc == null) {
                    acc = new Accum();
                    byCat.put(catnr, acc);
                }
                acc.groups.add(group);
                if (epochS != null && (acc.epochStr == null || epochS.compareTo(acc.epochStr) > 0)) {
                    acc.epochStr = epochS;
                    acc.row = copyRow(r);
                }
            }
        }

        // ---- assemble the wide catalog ----
        boolean includeSatcat = !satcat.isEmpty();
        DataFrameArrayByRowBuilder builder = DataFrame.byArrayRow(wideCols(includeSatcat));
        DataFrameArrayAppender appender = builder.appender();

        for (Map.Entry<Long, Accum> en : byCat.entrySet()) {
            long catnr = en.getKey();
            Map<String, String> v = en.getValue().row;

            String name = v.get("OBJECT_NAME");
            String intl = v.get("OBJECT_ID");
            String cls = v.get("CLASSIFICATION_TYPE");

            LocalDateTime epoch = parseLdt(v.get("EPOCH"));
            double mm = parseD(v.get("MEAN_MOTION"));
            double ecc = parseD(v.get("ECCENTRICITY"));
            double inc = parseD(v.get("INCLINATION"));
            double raan = parseD(v.get("RA_OF_ASC_NODE"));
            double argp = parseD(v.get("ARG_OF_PERICENTER"));
            double man = parseD(v.get("MEAN_ANOMALY"));
            double bstar = parseD(v.get("BSTAR"));
            double ndot = parseD(v.get("MEAN_MOTION_DOT"));
            double nddot = parseD(v.get("MEAN_MOTION_DDOT"));
            int ephType = parseInt(v.get("EPHEMERIS_TYPE"));
            int elset = parseInt(v.get("ELEMENT_SET_NO"));
            long rev = parseL(v.get("REV_AT_EPOCH"));

            boolean numericOk = epoch != null
                    && Double.isFinite(mm) && mm > 0
                    && Double.isFinite(ecc) && Double.isFinite(inc)
                    && Double.isFinite(raan) && Double.isFinite(argp) && Double.isFinite(man);
            boolean tleAvailable = Tle.fitsInTle(catnr) && numericOk;

            Tle.Tle3 tle = tleAvailable
                    ? Tle.build(name, catnr, cls, intl, epoch, mm, ecc, inc, raan, argp, man,
                            bstar, ndot, nddot, ephType, elset, rev)
                    : null;
            boolean checksumOk = tleAvailable && checksumsOk(tle);
            OrekitResult orekit = tleAvailable ? orekitVerify(tle.line1(), tle.line2())
                    : new OrekitResult(false, Double.NaN, Double.NaN, Double.NaN);

            double period = numericOk ? Orbital.periodMin(mm) : Double.NaN;
            double sma = numericOk ? Orbital.semiMajorAxisKm(mm) : Double.NaN;
            double apo = numericOk ? Orbital.apogeeKm(sma, ecc) : Double.NaN;
            double peri = numericOk ? Orbital.perigeeKm(sma, ecc) : Double.NaN;
            String orbitClass = numericOk ? Orbital.orbitClass(period, ecc) : "UNKNOWN";
            double ageHours = epoch != null
                    ? Duration.between(epoch.toInstant(ZoneOffset.UTC), fetchInstant).toMillis() / 3600e3
                    : Double.NaN;

            Map<String, String> sc = satcat.getOrDefault(catnr, Map.of());

            List<Object> row = new ArrayList<>(Arrays.asList(
                    catnr, name, intl, epoch,
                    mm, ecc, inc, raan, argp, man,
                    ndot, nddot, bstar, ephType, cls, elset, rev,
                    tle != null ? tle.line1() : null,
                    tle != null ? tle.line2() : null,
                    tle != null ? tle.line0() : null,
                    tle != null ? tle.tle() : null,
                    tle != null ? tle.tle3() : null,
                    tleAvailable, checksumOk,
                    orekit.ok(), orekit.rKm(), orekit.vKmS(), orekit.altKm(),
                    sma, period, apo, peri, orbitClass, ageHours,
                    String.join(",", en.getValue().groups),
                    "GP", "https://celestrak.org/NORAD/elements/", fetchUtc));
            if (includeSatcat) {
                for (String c : SATCAT_COLS) {
                    row.add(sc.get(c));
                }
            }
            appender.append(row.toArray());
        }

        DataFrame wide = appender.toDataFrame();

        // ---- by-group long format ----
        DataFrameArrayByRowBuilder lb = DataFrame.byArrayRow("norad_cat_id", "norad_group", "epoch", "object_name");
        DataFrameArrayAppender la = lb.appender();
        for (Object[] r : longRows) {
            la.append(r);
        }
        DataFrame longDf = la.toDataFrame();

        // ---- export ----
        Path csvPath = out.resolve("celestrak_gp_catalog.csv");
        Path parquetPath = out.resolve("celestrak_gp_catalog.parquet");
        Path longPath = out.resolve("celestrak_gp_by_group.csv");

        Csv.saver().createMissingDirs().save(wide, csvPath.toString());
        Parquet.saver().createMissingDirs().compression(CompressionCodec.GZIP).save(wide, parquetPath.toString());
        Csv.saver().createMissingDirs().save(longDf, longPath.toString());

        System.out.println(Printers.tabular.print(wide.head(5)));
        System.out.println();
        System.out.println("Objects      : " + wide.height());
        System.out.println("Columns      : " + wide.width());
        System.out.println("Group rows   : " + longDf.height());
        System.out.println("Wrote CSV    : " + csvPath.toAbsolutePath());
        System.out.println("Wrote Parquet: " + parquetPath.toAbsolutePath());
        System.out.println("Wrote long   : " + longPath.toAbsolutePath());
    }

    static boolean checksumsOk(Tle.Tle3 tle) {
        String l1 = tle.line1();
        String l2 = tle.line2();
        return l1.charAt(68) == Tle.checksumDigit(l1.substring(0, 68))
                && l2.charAt(68) == Tle.checksumDigit(l2.substring(0, 68));
    }

    static Map<String, String> copyRow(RowProxy r) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String c : OMM_COLS) {
            m.put(c, str(r, c));
        }
        return m;
    }

    static Map<Long, Map<String, String>> loadSatcat(Path p) throws Exception {
        Map<Long, Map<String, String>> out = new TreeMap<>();
        if (!Files.exists(p)) {
            System.out.println("SATCAT file not found at " + p + " - skipping enrichment.");
            return out;
        }
        DataFrame df = Csv.loader().load(p);
        for (RowProxy r : df) {
            String catnrS = str(r, "NORAD_CAT_ID");
            if (catnrS == null || catnrS.isBlank()) continue;
            long catnr;
            try {
                catnr = Long.parseLong(catnrS.trim());
            } catch (NumberFormatException e) {
                continue;
            }
            Map<String, String> m = new LinkedHashMap<>();
            for (String c : SATCAT_COLS) {
                m.put(c, str(r, c.toUpperCase()));
            }
            out.put(catnr, m);
        }
        return out;
    }

    // latest fetch time among GP sources in manifest.json (or now if unavailable)
    static String maxFetchUtc(Path manifest) throws Exception {
        if (!Files.exists(manifest)) return Instant.now().toString();
        String text = Files.readString(manifest);
        Pattern p = Pattern.compile("\"fetchedAtUtc\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(text);
        Instant max = null;
        while (m.find()) {
            try {
                Instant i = Instant.parse(m.group(1));
                if (max == null || i.isAfter(max)) max = i;
            } catch (Exception ignore) {
            }
        }
        return max != null ? max.toString() : Instant.now().toString();
    }

    static String str(RowProxy r, String col) {
        Object o = r.get(col);
        return o == null ? null : o.toString();
    }

    static double parseD(String s) {
        if (s == null || s.isBlank()) return Double.NaN;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    static int parseInt(String s) {
        if (s == null || s.isBlank()) return 0;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static long parseL(String s) {
        if (s == null || s.isBlank()) return 0L;
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    static LocalDateTime parseLdt(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDateTime.parse(s.trim());
        } catch (Exception e) {
            return null;
        }
    }
}