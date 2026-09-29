// Shared helper: derived orbital quantities for classical data analysis.
public class Orbital {

    public static final double MU = 398600.4418; // km^3/s^2 (WGS84)
    public static final double R_E = 6378.137;   // km (mean Earth radius)
    public static final double TWO_PI = 2 * Math.PI;

    public static double periodMin(double meanMotionRevPerDay) {
        return 1440.0 / meanMotionRevPerDay;
    }

    public static double semiMajorAxisKm(double meanMotionRevPerDay) {
        double n = meanMotionRevPerDay * TWO_PI / 86400.0; // rad/s
        return Math.cbrt(MU / (n * n));
    }

    // altitudes over mean Earth radius
    public static double apogeeKm(double semiMajorAxisKm, double e) {
        return semiMajorAxisKm * (1 + e) - R_E;
    }

    public static double perigeeKm(double semiMajorAxisKm, double e) {
        return semiMajorAxisKm * (1 - e) - R_E;
    }

    public static String orbitClass(double periodMin, double e) {
        if (!(periodMin > 0) || Double.isNaN(periodMin)) return "UNKNOWN";
        if (e > 0.25 && periodMin > 225) return "HEO";
        if (periodMin >= 1350 && periodMin <= 1500) return "GEO";
        if (periodMin >= 225) return "MEO";
        return "LEO";
    }
}