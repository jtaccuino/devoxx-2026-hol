///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//DEPS org.dflib:dflib:2.0.0-M6
//DEPS org.dflib:dflib-csv:2.0.0-M6

// Classical tabular analysis of the compiled CelesTrak catalog with dflib:
//   descriptive stats, orbit-class rollups, top owners/types, group coverage,
//   outlier detection, data-age analysis and a completeness report.
// Emits stats_*.csv into data/out and prints a summary.
//
// Usage: jbang analyze.java

import org.dflib.DataFrame;
import org.dflib.Printers;
import org.dflib.csv.Csv;
import org.dflib.csv.CsvLoader;
import org.dflib.csv.CsvSaver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class analyze {

    static final String[] NUMERIC = {
            "mean_motion", "eccentricity", "inclination", "ra_of_asc_node",
            "arg_of_pericenter", "mean_anomaly", "mean_motion_dot", "mean_motion_ddot", "bstar",
            "semimajor_axis_km", "period_min", "apogee_km", "perigee_km", "epoch_age_hours",
            "r_km", "v_km_s", "altitude_km"
    };

    public static void main(String[] args) throws Exception {
        Path out = Path.of("data", "out");
        Path csvPath = out.resolve("celestrak_gp_catalog.csv");
        if (!Files.exists(csvPath)) {
            System.err.println("catalog CSV not found - run build.java first");
            System.exit(1);
        }

        CsvLoader loader = Csv.loader().longCol("norad_cat_id").dateTimeCol("epoch");
        for (String c : NUMERIC) loader = loader.doubleCol(c);
        DataFrame df = loader.load(csvPath.toString());

        System.out.println(Printers.tabular.print(df.head(6)));
        System.out.println("catalog rows: " + df.height() + "  cols: " + df.width());
        System.out.println();

        // ---- 1. descriptive statistics per numeric column (plain Java, QL-agnostic) ----
        List<Object[]> statsRows = new ArrayList<>();
        for (String c : NUMERIC) {
            double[] s = stats(df, c);
            statsRows.add(new Object[]{c, (long) s[0], s[1], s[2], s[3], s[4], s[5], s[6], s[7]});
        }
        DataFrame describe = DataFrame
                .byArrayRow("column", "n", "min", "avg", "max", "stdDev", "median", "q25", "q75")
                .appender().append(statsRows).toDataFrame();
        System.out.println("== descriptive statistics ==");
        System.out.println(Printers.tabular.print(describe));
        Csv.saver().save(describe, out.resolve("stats_describe.csv").toString());

        // ---- 2. orbit-class rollup (plain Java: dflib M6 group().agg() with >1 exp is unreliable) ----
        Map<String, double[]> oc = new TreeMap<>();
        for (org.dflib.row.RowProxy r : df) {
            Object clsO = r.get("orbit_class");
            String cls = clsO == null ? null : clsO.toString();
            Object inc = r.get("inclination");
            Object per = r.get("period_min");
            double[] a = oc.computeIfAbsent(cls, k -> new double[]{0, 0, 0, Double.MAX_VALUE, -Double.MAX_VALUE});
            a[0]++;
            if (inc instanceof Number n) a[1] += n.doubleValue();
            if (per instanceof Number n) {
                double p = n.doubleValue();
                a[2] += p;
                a[3] = Math.min(a[3], p);
                a[4] = Math.max(a[4], p);
            }
        }
        List<Object[]> ocRows = new ArrayList<>();
        for (Map.Entry<String, double[]> en : oc.entrySet()) {
            double[] a = en.getValue();
            ocRows.add(new Object[]{en.getKey(), (long) a[0], a[0] > 0 ? a[1] / a[0] : null,
                    a[0] > 0 ? a[2] / a[0] : null,
                    a[3] == Double.MAX_VALUE ? null : a[3],
                    a[4] == -Double.MAX_VALUE ? null : a[4]});
        }
        DataFrame byOrbit = DataFrame
                .byArrayRow("orbit_class", "n", "avg_inclination", "avg_period_min", "min_period", "max_period")
                .appender().append(ocRows).toDataFrame()
                .sort("n desc");
        System.out.println();
        System.out.println("== orbit classes ==");
        System.out.println(Printers.tabular.print(byOrbit));
        Csv.saver().save(byOrbit, out.resolve("stats_by_orbit_class.csv").toString());

        // ---- 3. TLE availability ----
        DataFrame byTle = df.group("tle_available").agg("tle_available, count() as n").sort("n desc");
        System.out.println();
        System.out.println("== TLE availability ==");
        System.out.println(Printers.tabular.print(byTle));
        Csv.saver().save(byTle, out.resolve("stats_tle_availability.csv").toString());

        // ---- 4. Orekit propagation sanity ----
        long orekitBad = df.rows(r -> r.get("tle_orekit_ok") != null && "false".equals(r.get("tle_orekit_ok").toString())).select().height();
        long rNull = df.rows(r -> r.get("r_km") == null).select().height();
        long weirdAlt = df.rows(r -> {
            Object a = r.get("altitude_km");
            return a != null && ((Number) a).doubleValue() > 60000.0;
        }).select().height();
        System.out.println();
        System.out.println("== Orekit verification ==");
        System.out.println("tle_orekit_ok=false : " + orekitBad);
        System.out.println("no propagated r_km  : " + rNull);
        System.out.println("altitude > 60000 km : " + weirdAlt + " (deep-space/HAMR objects expected)");

        // ---- 5. data-age analysis (staleness) ----
        DataFrame stale = df.rows(r -> {
                    Object a = r.get("epoch_age_hours");
                    return a != null && ((Number) a).doubleValue() > 48.0;
                })
                .select()
                .cols("norad_cat_id", "object_name", "epoch", "epoch_age_hours", "orbit_class", "norad_groups")
                .select()
                .sort("epoch_age_hours desc")
                .head(20);
        System.out.println();
        System.out.println("== objects with element data older than 48 h (top 20) ==");
        System.out.println(Printers.tabular.print(stale));
        Csv.saver().save(stale, out.resolve("stats_stale_objects.csv").toString());

        // ---- 6. outliers: inclination far from typical band / high eccentricity ----
        DataFrame outliers = df.rows(r -> {
                    Object i = r.get("inclination");
                    Object e = r.get("eccentricity");
                    boolean incOdd = i != null && Math.abs(((Number) i).doubleValue() - 90.0) > 75.0;
                    boolean eccOdd = e != null && ((Number) e).doubleValue() > 0.6;
                    return incOdd || eccOdd;
                })
                .select()
                .cols("norad_cat_id", "object_name", "inclination", "eccentricity", "period_min", "orbit_class", "norad_groups")
                .select()
                .sort("inclination desc")
                .head(30);
        System.out.println();
        System.out.println("== orbital outliers (|incl-90|>75 or ecc>0.6, top 30) ==");
        System.out.println(Printers.tabular.print(outliers));
        Csv.saver().save(outliers, out.resolve("stats_outliers.csv").toString());

        // ---- 7. group coverage (from the long-format byproduct) ----
        Path longPath = out.resolve("celestrak_gp_by_group.csv");
        if (Files.exists(longPath)) {
            DataFrame byGroup = Csv.load(longPath.toString())
                    .group("norad_group").agg("norad_group, count() as n").sort("n desc");
            System.out.println();
            System.out.println("== objects per source group (long format, overlaps included) ==");
            System.out.println(Printers.tabular.print(byGroup));
            Csv.saver().save(byGroup, out.resolve("stats_by_group.csv").toString());
        }

        // ---- 8. completeness report ----
        boolean hasSatcat = java.util.Arrays.asList(df.getColumnsIndex().toArray()).contains("satcat_object_type");
        long total = df.height();
        long satcatJoined = hasSatcat ? df.rows(r -> r.get("satcat_object_type") != null).select().height() : 0;
        System.out.println();
        System.out.println("== completeness ==");
        System.out.println("total objects                     : " + total);
        System.out.println("with SATCAT enrichment            : " + satcatJoined + " (0 if satcat.csv was unavailable)");
        System.out.println("orbit_class = UNKNOWN             : "
                + df.rows(r -> "UNKNOWN".equals(r.get("orbit_class"))).select().height());
        System.out.println("tle_available = false             : "
                + df.rows(r -> "false".equals(r.get("tle_available"))).select().height());
        System.out.println();
        System.out.println("Wrote stats_*.csv into " + out.toAbsolutePath());
    }

    // returns {n, min, mean, max, populationStdDev, median, q25, q75} over non-null values
    static double[] stats(DataFrame df, String col) {
        List<Double> vals = new ArrayList<>();
        for (org.dflib.row.RowProxy r : df) {
            Object o = r.get(col);
            if (o instanceof Number num) {
                double v = num.doubleValue();
                if (Double.isFinite(v)) vals.add(v);
            }
        }
        vals.sort(Double::compareTo);
        int n = vals.size();
        double[] out = new double[8];
        if (n == 0) return out;
        out[0] = n;
        out[1] = vals.get(0);
        out[3] = vals.get(n - 1);
        double sum = 0;
        for (double v : vals) sum += v;
        out[2] = sum / n;
        double var = 0;
        for (double v : vals) var += (v - out[2]) * (v - out[2]);
        out[4] = Math.sqrt(var / n);
        out[5] = quantile(vals, 0.5);
        out[6] = quantile(vals, 0.25);
        out[7] = quantile(vals, 0.75);
        return out;
    }

    static double quantile(List<Double> sorted, double p) {
        int n = sorted.size();
        double pos = (n - 1) * p;
        int lo = (int) Math.floor(pos);
        int hi = (int) Math.ceil(pos);
        double frac = pos - lo;
        return sorted.get(lo) + frac * (sorted.get(hi) - sorted.get(lo));
    }
}