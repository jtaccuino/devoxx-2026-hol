///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//DEPS org.dflib:dflib:2.0.0-M6
//DEPS org.dflib:dflib-csv:2.0.0-M6
//DEPS org.orekit:orekit:13.0.3

// Independent TLE verification with Orekit across the whole compiled catalog:
//   - re-parses every reconstructed TLE/3LE with Orekit (checksum + format + elements)
//   - compares Orekit's parsed elements to the original OMM values (round-trip)
//   - propagates each TLE to its epoch (SGP4/SDP4) and sanity-checks the state vectors
// Emits verify_report.csv (per-object) and prints a summary of failures/mismatches.
//
// Uses the TAI timescale so no UTC-TAI leap-second data file is required; the element
// round-trip and position magnitudes are unaffected by that choice.
//
// Usage: jbang verify.java

import org.dflib.DataFrame;
import org.dflib.Printers;
import org.dflib.csv.Csv;
import org.dflib.csv.CsvLoader;
import org.dflib.row.RowProxy;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.TimeScale;
import org.orekit.utils.PVCoordinates;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class verify {

    static final TimeScale TAI = DataContext.getDefault().getTimeScales().getTAI();

    public static void main(String[] args) throws Exception {
        Path out = Path.of("data", "out");
        Path csvPath = out.resolve("celestrak_gp_catalog.csv");
        if (!Files.exists(csvPath)) {
            System.err.println("catalog CSV not found - run build.java first");
            System.exit(1);
        }

        CsvLoader loader = Csv.loader()
                .longCol("norad_cat_id")
                .longCol("rev_at_epoch");
        for (String c : new String[]{"mean_motion", "eccentricity", "inclination", "ra_of_asc_node",
                "arg_of_pericenter", "mean_anomaly", "bstar"}) {
            loader = loader.doubleCol(c);
        }
        DataFrame df = loader.load(csvPath.toString());

        long total = 0, parsed = 0, formatBad = 0, checksumBad = 0, propFail = 0, sanityFail = 0;
        long revMismatch = 0, elsetMismatch = 0, satnumMismatch = 0;

        double[] maxDiff = new double[7];   // mm, e, i, raan, argp, man, bstar
        double[] sumSq = new double[7];
        long[] ndiff = new long[7];

        List<Object[]> report = new ArrayList<>();
        List<Object[]> mismatches = new ArrayList<>();

        for (RowProxy r : df) {
            String l1 = str(r, "tle_line1");
            String l2 = str(r, "tle_line2");
            long catnr = r.getLong("norad_cat_id");
            if (l1 == null || l1.isBlank() || l2 == null || l2.isBlank()) continue; // 6-digit / analyst objects without TLE
            total++;

            boolean checksumOk = true;
            boolean formatOk = true;
            try {
                TLE.isFormatOK(l1, l2);
            } catch (OrekitException e) {
                // throws only on checksum mismatch
                checksumOk = false;
            } catch (Exception e) {
                formatOk = false;
            }
            if (!formatOk) formatBad++;
            if (!checksumOk) checksumBad++;

            Double[] omm = {(Double) r.get("mean_motion"), (Double) r.get("eccentricity"), (Double) r.get("inclination"),
                    (Double) r.get("ra_of_asc_node"), (Double) r.get("arg_of_pericenter"), (Double) r.get("mean_anomaly"),
                    (Double) r.get("bstar")};
            long rev = r.getLong("rev_at_epoch");
            double rKm = Double.NaN, vKmS = Double.NaN, altKm = Double.NaN;
            Double[] orek = new Double[7];
            boolean ok = false;

            if (formatOk && checksumOk) {
                try {
                    TLE t = new TLE(l1, l2, TAI);
                    ok = true;
                    parsed++;
                    orek[0] = t.getMeanMotion() * 86400 / (2 * Math.PI); // rev/day
                    orek[1] = t.getE();
                    orek[2] = Math.toDegrees(t.getI());
                    orek[3] = Math.toDegrees(t.getRaan());
                    orek[4] = Math.toDegrees(t.getPerigeeArgument());
                    orek[5] = Math.toDegrees(t.getMeanAnomaly());
                    orek[6] = t.getBStar();

                    if (t.getSatelliteNumber() != (int) catnr) satnumMismatch++;
                    if (t.getRevolutionNumberAtEpoch() != (int) rev) revMismatch++;

                    PVCoordinates pv = TLEPropagator.selectExtrapolator(t).propagate(t.getDate()).getPVCoordinates();
                    rKm = pv.getPosition().getNorm() / 1000.0;
                    vKmS = pv.getVelocity().getNorm() / 1000.0;
                    altKm = rKm - 6378.137;

                    if (!(altKm > -1000 && altKm < 70000) || !(vKmS > 0.5 && vKmS < 12.0)) sanityFail++;
                } catch (Exception e) {
                    propFail++;
                    ok = false;
                }
            }

            for (int i = 0; i < 7; i++) {
                if (omm[i] != null && orek[i] != null) {
                    double d = Math.abs(omm[i] - orek[i]);
                    if (d > maxDiff[i]) maxDiff[i] = d;
                    sumSq[i] += d * d;
                    ndiff[i]++;
                }
            }

            report.add(new Object[]{catnr, str(r, "object_name"), formatOk, checksumOk, ok,
                    omm[0], orek[0], diff(omm[0], orek[0]),
                    omm[1], orek[1], diff(omm[1], orek[1]),
                    omm[2], orek[2], diff(omm[2], orek[2]),
                    rKm, vKmS, altKm});

            if (!formatOk || !checksumOk || !ok || bigDiff(omm, orek)) {
                mismatches.add(new Object[]{catnr, str(r, "object_name"), formatOk, checksumOk, ok,
                        omm[0], orek[0], omm[1], orek[1], omm[2], orek[2], rKm, vKmS, altKm});
            }
        }

        DataFrame reportDf = DataFrame
                .byArrayRow("norad_cat_id", "object_name", "format_ok", "checksum_ok", "orekit_ok",
                        "omm_mean_motion", "orek_mean_motion", "d_mean_motion",
                        "omm_eccentricity", "orek_eccentricity", "d_eccentricity",
                        "omm_inclination", "orek_inclination", "d_inclination",
                        "r_km", "v_km_s", "altitude_km")
                .appender().append(report).toDataFrame();
        Csv.saver().save(reportDf, out.resolve("verify_report.csv").toString());

        String[] names = {"mean_motion", "eccentricity", "inclination", "raan", "argp", "man", "bstar"};
        System.out.println("== Orekit verification summary ==");
        System.out.println("TLE rows inspected      : " + total);
        System.out.println("parsed OK               : " + parsed);
        System.out.println("format invalid          : " + formatBad);
        System.out.println("checksum invalid        : " + checksumBad);
        System.out.println("propagation failure     : " + propFail);
        System.out.println("state sanity failures   : " + sanityFail);
        System.out.println("satnum mismatch         : " + satnumMismatch);
        System.out.println("rev-at-epoch mismatch   : " + revMismatch);
        System.out.println();
        System.out.printf("%-14s %12s %12s%n", "element", "max abs diff", "rms diff");
        for (int i = 0; i < 7; i++) {
            double rms = ndiff[i] > 0 ? Math.sqrt(sumSq[i] / ndiff[i]) : Double.NaN;
            System.out.printf("%-14s %12.4e %12.4e%n", names[i], maxDiff[i], rms);
        }

        List<Object[]> mmRows = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            mmRows.add(new Object[]{names[i], maxDiff[i], ndiff[i] > 0 ? Math.sqrt(sumSq[i] / ndiff[i]) : Double.NaN});
        }
        DataFrame mm = DataFrame.byArrayRow("element", "max_abs_diff", "rms_diff")
                .appender().append(mmRows).toDataFrame();
        Csv.saver().save(mm, out.resolve("verify_element_diffs.csv").toString());

        if (!mismatches.isEmpty()) {
            System.out.println();
            System.out.println("== mismatches (checksum/parse/diff) - first 20 ==");
            DataFrame mis = DataFrame
                    .byArrayRow("norad_cat_id", "object_name", "format_ok", "checksum_ok", "orekit_ok",
                            "omm_mm", "orek_mm", "omm_e", "orek_e", "omm_i", "orek_i",
                            "r_km", "v_km_s", "altitude_km")
                    .appender().append(mismatches).toDataFrame();
            System.out.println(Printers.tabular.print(mis.head(20)));
        }
        System.out.println();
        System.out.println("Wrote verify_report.csv and verify_element_diffs.csv into " + out.toAbsolutePath());
    }

    static boolean bigDiff(Double[] omm, Double[] orek) {
        // tolerances reflect TLE encoding precision (angles 4 dp, ecc 7 dp, bstar 5-digit mantissa)
        double[] tol = {1e-6, 1e-6, 1e-4, 1e-4, 1e-4, 1e-4, 1e-4};
        for (int i = 0; i < 7; i++) {
            if (omm[i] != null && orek[i] != null && Math.abs(omm[i] - orek[i]) > tol[i]) return true;
        }
        return false;
    }

    static Double diff(Double a, Double b) {
        return (a != null && b != null) ? Math.abs(a - b) : null;
    }

    static String str(RowProxy r, String col) {
        Object o = r.get(col);
        return o == null ? null : o.toString();
    }
}