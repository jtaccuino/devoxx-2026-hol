// Shared helper: rebuild exact TLE (2LE) and 3LE (three-line element set) strings
// from OMM element fields, including checksum digits. Used by build.java.
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;

public class Tle {

    public record Tle3(String line0, String line1, String line2) {
        public String tle() {
            return line1 + "\n" + line2;
        }

        public String tle3() {
            return line0 + "\n" + line1 + "\n" + line2;
        }
    }

    public static boolean fitsInTle(long catnr) {
        return catnr >= 1 && catnr <= 99_999;
    }

    public static Tle3 build(
            String objectName, long catnr, String classification, String intlDesignator,
            LocalDateTime epoch, double meanMotion, double eccentricity, double inclination,
            double raan, double argPerigee, double meanAnomaly, double bstar,
            double meanMotionDot, double meanMotionDdot, int ephemerisType, int elementSetNo,
            long revAtEpoch) {

        String line0 = line0(objectName);
        String line1 = line1(catnr, classification, intlDesignator, epoch,
                meanMotionDot, meanMotionDdot, bstar, ephemerisType, elementSetNo);
        String line2 = line2(catnr, inclination, raan, eccentricity, argPerigee,
                meanAnomaly, meanMotion, revAtEpoch);
        return new Tle3(line0, line1, line2);
    }

    static String line0(String name) {
        String n = name == null ? "" : name.trim();
        if (n.length() > 24) n = n.substring(0, 24);
        return String.format(Locale.US, "%-24s", n);
    }

    static String line1(long catnr, String classification, String intlDesignator,
            LocalDateTime epoch, double ndot, double nddot, double bstar,
            int ephemerisType, int elementSetNo) {

        String prefix = String.format(Locale.US, "1 %05d%c %-8s %s %s %s %s %d %4d",
                catnr,
                clsChar(classification),
                intlField(intlDesignator),
                epochField(epoch),
                ndotField(ndot),
                expField(nddot),
                expField(bstar),
                ephemerisType,
                elementSetNo);
        return prefix + checksumDigit(prefix);
    }

    static String line2(long catnr, double inclination, double raan, double eccentricity,
            double argPerigee, double meanAnomaly, double meanMotion, long revAtEpoch) {

        String prefix = String.format(Locale.US, "2 %05d %8.4f %8.4f %07d %8.4f %8.4f %11.8f%5d",
                catnr, inclination, raan, Math.round(eccentricity * 1e7),
                argPerigee, meanAnomaly, meanMotion, revAtEpoch);
        return prefix + checksumDigit(prefix);
    }

    static char clsChar(String classification) {
        if (classification == null || classification.isBlank()) return 'U';
        return Character.toUpperCase(classification.trim().charAt(0));
    }

    // International designator: "1998-067A" -> "98067A" (2-digit year + launch number + suffix)
    static String intlField(String intlDesignator) {
        if (intlDesignator == null || intlDesignator.isBlank()) return " ".repeat(8);
        String[] parts = intlDesignator.trim().split("-");
        String yy = parts[0].length() >= 2 ? parts[0].substring(parts[0].length() - 2) : parts[0];
        String v = yy + String.join("", Arrays.copyOfRange(parts, 1, parts.length));
        if (v.length() > 8) v = v.substring(v.length() - 8);
        return String.format(Locale.US, "%-8s", v);
    }

    // Epoch field: YYDDD.DDDDDDDD
    static String epochField(LocalDateTime t) {
        int yy = t.getYear() % 100;
        int doy = t.getDayOfYear();
        long todNanos = t.toLocalTime().toNanoOfDay();
        double frac = todNanos / 86_400_000_000_000.0;
        long frac8 = Math.round(frac * 100_000_000.0);
        if (frac8 >= 100_000_000L) {
            frac8 = 0;
            doy++;
        }
        return String.format(Locale.US, "%02d%03d.%08d", yy, doy, frac8);
    }

    // NDOT field, cols 34-43: sign + "." + 8 decimals (e.g. " .00008083")
    static String ndotField(double v) {
        char sign = v < 0 ? '-' : ' ';
        long mag = Math.round(Math.abs(v) * 100_000_000.0);
        return sign + String.format(Locale.US, ".%08d", mag);
    }

    // NDDOT / BSTAR field, cols 45-52 / 54-61: sign + 5-digit mantissa + exponent sign + 1 digit
    static String expField(double v) {
        if (v == 0.0) return " 00000+0";
        int e = (int) Math.floor(Math.log10(Math.abs(v))) + 1;
        double mantissa = Math.abs(v) / Math.pow(10.0, e);
        long digits = Math.round(mantissa * 100_000.0);
        if (digits >= 100_000) {
            digits = 10_000;
            e += 1;
        }
        char vSign = v < 0 ? '-' : ' ';
        char eSign = e < 0 ? '-' : '+';
        return String.format(Locale.US, "%c%05d%c%d", vSign, digits, eSign, Math.abs(e));
    }

    static int checksum(String s) {
        int sum = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') sum += c - '0';
            else if (c == '-') sum += 1;
        }
        return sum % 10;
    }

    static char checksumDigit(String prefix) {
        return (char) ('0' + checksum(prefix));
    }

    // Self-test against golden TLE strings captured live from CelesTrak (stations group).
    public static void main(String[] args) {
        LocalDateTime issEpoch = LocalDateTime.parse("2026-09-23T04:11:03.279264");
        Tle3 iss = build("ISS (ZARYA)", 25544, "U", "1998-067A", issEpoch,
                15.49240021, 0.000473, 51.6317, 175.1352, 170.7326, 189.3751,
                1.534221E-4, 8.083E-5, 0.0, 0, 999, 58694);
        check("ISS line1", iss.line1(), "1 25544U 98067A   26266.17434351  .00008083  00000+0  15342-3 0  9996");
        check("ISS line2", iss.line2(), "2 25544  51.6317 175.1352 0004730 170.7326 189.3751 15.49240021586943");

        LocalDateTime poiskEpoch = LocalDateTime.parse("2026-09-23T04:11:03.279264");
        Tle3 poisk = build("POISK", 36086, "U", "2009-060A", poiskEpoch,
                15.49240021, 0.000473, 51.6317, 175.1352, 170.7326, 189.3751,
                1.534221E-4, 8.083E-5, 0.0, 0, 999, 58696);
        check("POISK line1", poisk.line1(), "1 36086U 09060A   26266.17434351  .00008083  00000+0  15342-3 0  9994");
        check("POISK line2", poisk.line2(), "2 36086  51.6317 175.1352 0004730 170.7326 189.3751 15.49240021586968");

        System.out.println("All golden TLE fixtures matched.");
    }

    static void check(String label, String actual, String expected) {
        if (!actual.equals(expected)) {
            throw new AssertionError(label + " mismatch:\n  actual  : " + actual + "\n  expected: " + expected);
        }
        System.out.println("ok  " + label);
    }
}