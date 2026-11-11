package asutp;

import java.util.function.DoubleUnaryOperator;

/** Статистичні функції експериментального дослідження. */
public final class StatMath {
    private static final double EPS = 1e-15;
    private static final double TINY = 1e-300;

    private StatMath() { }

    /** Середнє значення, формула (2.3) навч. посібника. */
    public static double mean(double[] x) {
        double s = 0;
        for (double v : x) {
            s += v;
        }
        return s / x.length;
    }

    /** Стандартне відхилення, формула (2.4). */
    public static double sd(double[] x) {
        double m = mean(x);
        double s = 0;
        for (double v : x) {
            s += (v - m) * (v - m);
        }
        return Math.sqrt(s / (x.length - 1));
    }

    /** Точність за формулою (3.4) при beta = 0,95. */
    public static double eps(double sigma, int p) {
        return sigma * Math.sqrt(20.0 / p);
    }

    // ---- функції розподілу (числові методи) ----

    /** ln Gamma(x), наближення Ланцоша. */
    static double lnGamma(double x) {
        double[] c = {76.18009172947146, -86.50532032941677,
            24.01409824083091, -1.231739572450155,
            0.1208650973866179e-2, -0.5395239384953e-5};
        double y = x;
        double tmp = x + 5.5;
        tmp -= (x + 0.5) * Math.log(tmp);
        double ser = 1.000000000190015;
        for (double cj : c) {
            ser += cj / ++y;
        }
        return -tmp + Math.log(2.5066282746310005 * ser / x);
    }

    /** Регуляризована неповна гамма-функція P(a, x). */
    static double gammaP(double a, double x) {
        if (x <= 0) {
            return 0;
        }
        double front = Math.exp(-x + a * Math.log(x)
                - lnGamma(a));
        if (x < a + 1) {               // ряд
            double ap = a;
            double del = 1 / a;
            double sum = del;
            while (Math.abs(del) > Math.abs(sum) * EPS) {
                ap++;
                del *= x / ap;
                sum += del;
            }
            return sum * front;
        }
        double b = x + 1 - a;          // ланцюговий дріб
        double c = 1 / TINY;
        double d = 1 / b;
        double h = d;
        for (int i = 1; i < 1000; i++) {
            double an = -i * (i - a);
            b += 2;
            d = fix(an * d + b);
            c = fix(b + an / c);
            d = 1 / d;
            double del = d * c;
            h *= del;
            if (Math.abs(del - 1) < EPS) {
                break;
            }
        }
        return 1 - front * h;
    }

    /** Регуляризована неповна бета-функція I_x(a, b). */
    static double betaI(double a, double b, double x) {
        if (x <= 0) {
            return 0;
        }
        if (x >= 1) {
            return 1;
        }
        double bt = Math.exp(lnGamma(a + b) - lnGamma(a)
                - lnGamma(b) + a * Math.log(x)
                + b * Math.log(1 - x));
        if (x < (a + 1) / (a + b + 2)) {
            return bt * betaCf(a, b, x) / a;
        }
        return 1 - bt * betaCf(b, a, 1 - x) / b;
    }

    private static double betaCf(double a, double b,
                                 double x) {
        double c = 1;
        double d = 1 / fix(1 - (a + b) * x / (a + 1));
        double h = d;
        for (int m = 1; m < 1000; m++) {
            int m2 = 2 * m;
            double aa = m * (b - m) * x
                    / ((a - 1 + m2) * (a + m2));
            d = 1 / fix(1 + aa * d);
            c = fix(1 + aa / c);
            h *= d * c;
            aa = -(a + m) * (a + b + m) * x
                    / ((a + m2) * (a + 1 + m2));
            d = 1 / fix(1 + aa * d);
            c = fix(1 + aa / c);
            double del = d * c;
            h *= del;
            if (Math.abs(del - 1) < EPS) {
                break;
            }
        }
        return h;
    }

    private static double fix(double v) {
        return Math.abs(v) < TINY ? TINY : v;
    }

    /** Функція нормального розподілу F(x). */
    public static double normalCdf(double x, double mu,
                                   double s) {
        if (Double.isInfinite(x)) {
            return x > 0 ? 1 : 0;
        }
        double z = (x - mu) / s;
        double half = 0.5 * gammaP(0.5, z * z / 2);
        return z < 0 ? 0.5 - half : 0.5 + half;
    }

    static double chi2Cdf(double x, int k) {
        return gammaP(k / 2.0, x / 2);
    }

    static double fCdf(double x, int d1, int d2) {
        return betaI(d1 / 2.0, d2 / 2.0,
                d1 * x / (d1 * x + d2));
    }

    static double tCdf(double x, int v) {
        double ib = betaI(v / 2.0, 0.5, v / (v + x * x));
        return x >= 0 ? 1 - 0.5 * ib : 0.5 * ib;
    }

    /** Корінь рівняння cdf(x) = p бісекцією, x > 0. */
    static double quantile(DoubleUnaryOperator cdf,
                           double p) {
        double lo = 0;
        double hi = 1;
        while (cdf.applyAsDouble(hi) < p) {
            hi *= 2;
        }
        for (int i = 0; i < 200; i++) {
            double mid = (lo + hi) / 2;
            if (cdf.applyAsDouble(mid) < p) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) / 2;
    }

    /** chi2_кр = qchisq(0.95, k). */
    public static double chi2Crit(int k) {
        return quantile(x -> chi2Cdf(x, k), 0.95);
    }

    /** F_кр = qF(0.95, d1, d2), формула (2.26). */
    public static double fCrit(int d1, int d2) {
        return quantile(x -> fCdf(x, d1, d2), 0.95);
    }

    /** t_кр = qt(0.95, v), формула (2.29). */
    public static double tCrit(int v) {
        return quantile(x -> tCdf(x, v), 0.95);
    }
}
