package asutp;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

/** Експериментальне дослідження моделі (розділ 4). */
public class Experiment {
    static final double T_WARM = 15_000;  // T_пер, с
    static final double T_MOD = 30_000;   // T_мод, с
    static final int P = 80;              // кількість замірів
    static final int P_LAW = 200;         // для закону розподілу
    static final double[] TC = {10, 20, 30, 40, 50};  // T_с, с
    static final double[] TP = {1, 3, 5, 7, 9};       // t_п, с

    static String dir;
    static PrintStream log;

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.ROOT);
        dir = args.length > 0 ? args[0] : "results";
        log = new PrintStream(dir + "/experiment.txt",
                StandardCharsets.UTF_8);
        double[] eps = observations();
        transientPeriod(eps[0], eps[1]);
        series();
        plan();
        log.close();
    }

    static void out(String format, Object... a) {
        String s = String.format(format, a);
        System.out.println(s);
        log.println(s);
    }

    /** Один замір: прогін, статистика на [T_пер; T_мод]. */
    static Stats measure(Params p, long seed) {
        Model m = new Model(p, seed);
        m.stats.tWarm = T_WARM;
        return m.run(T_MOD);
    }

    /** sigma окремих спостережень і точність eps = sigma/2. */
    static double[] observations() {
        Model m = new Model(Params.base(), 1);
        m.stats.periods = new ArrayList<>();
        m.stats.lostPerFail = new ArrayList<>();
        m.run(1_000_000);
        double[] tz = m.stats.periods.stream()
                .mapToDouble(Double::doubleValue).toArray();
        double[] np = m.stats.lostPerFail.stream()
                .mapToDouble(Long::doubleValue).toArray();
        double sTz = StatMath.sd(tz);
        double sNp = StatMath.sd(np);
        out("Окремі спостереження, прогін 1 000 000 с, seed 1");
        out("  періодів %d: середнє %.3f с, sigma %.3f с",
                tz.length, StatMath.mean(tz), sTz);
        out("  відмов %d: середнє %.4f, sigma %.4f",
                np.length, StatMath.mean(np), sNp);
        out("  eps = sigma/2: %.3f с; %.4f", sTz / 2, sNp / 2);
        return new double[] {sTz / 2, sNp / 2};
    }

    /** Перехідний період за умовою (3.1), 5 прогонів. */
    static void transientPeriod(double epsTz, double epsNp)
            throws IOException {
        int seeds = 5;
        double step = 250;
        double tEnd = 30_000;
        double tStar = step;
        try (PrintStream csv = new PrintStream(
                dir + "/transient.csv", StandardCharsets.UTF_8)) {
            csv.println("t;tz1;tz2;tz3;tz4;tz5;"
                    + "np1;np2;np3;np4;np5;dtz;dnp");
            for (double t = step; t <= tEnd; t += step) {
                double[] tz = new double[seeds];
                double[] np = new double[seeds];
                for (int s = 0; s < seeds; s++) {
                    Stats st = new Model(Params.base(), s + 1)
                            .run(t);
                    tz[s] = st.meanTz();
                    np[s] = st.meanNp();
                }
                double dTz = range(tz);
                double dNp = range(np);
                if (dTz >= epsTz || dNp >= epsNp) {
                    tStar = t + step;   // (3.1) порушено
                }
                StringBuilder row = new StringBuilder();
                row.append(String.format("%.0f", t));
                for (double v : tz) {
                    row.append(String.format(";%.3f", v));
                }
                for (double v : np) {
                    row.append(String.format(";%.4f", v));
                }
                row.append(String.format(";%.3f;%.4f", dTz, dNp));
                csv.println(row);
            }
        }
        out("Перехідний період: (3.1) виконується з T = %.0f с"
                + " (перевірено до %.0f с)", tStar, tEnd);
    }

    static double range(double[] v) {
        double lo = v[0];
        double hi = v[0];
        for (double x : v) {
            lo = Math.min(lo, x);
            hi = Math.max(hi, x);
        }
        return hi - lo;
    }

    /** Серія замірів за базових значень і закон розподілу. */
    static void series() throws IOException {
        double[] tz = new double[P_LAW];
        double[] np = new double[P_LAW];
        double[] kz = new double[P_LAW];
        double[] pp = new double[P_LAW];
        try (PrintStream csv = new PrintStream(
                dir + "/series.csv", StandardCharsets.UTF_8)) {
            csv.println("seed;Nnadh;Np;Nv;Nz;Tz;Np_mean;kz;pp");
            for (int i = 0; i < P_LAW; i++) {
                Stats s = measure(Params.base(), i + 1);
                tz[i] = s.meanTz();
                np[i] = s.meanNp();
                kz[i] = s.kz();
                pp[i] = s.pp();
                csv.printf("%d;%d;%d;%d;%d;%.3f;%.4f;%.5f;%.5f%n",
                        i + 1, s.nNadh(), s.nP(), s.nV(), s.nZ(),
                        tz[i], np[i], kz[i], pp[i]);
            }
        }
        out("Серія: T_пер = %.0f с, T_мод = %.0f с",
                T_WARM, T_MOD);
        String[] names = {"Tz", "Np", "kz", "pp"};
        double[][] all = {tz, np, kz, pp};
        for (int j = 0; j < names.length; j++) {
            double[] y = java.util.Arrays.copyOf(all[j], P);
            double s = StatMath.sd(y);
            out("  %s (%d замірів): середнє %.4f, sigma %.4f,"
                    + " eps %.4f", names[j], P, StatMath.mean(y),
                    s, StatMath.eps(s, P));
        }
        law("Tz", tz);
        law("Np", np);
    }

    static void law(String name, double[] y) throws IOException {
        Distribution d = new Distribution(y, 20);
        d.write(dir + "/hist_" + name.toLowerCase() + ".csv");
        out("Закон розподілу %s (%d значень): mu %.4f,"
                + " sigma %.4f, min %.4f, h %.4f", name,
                y.length, d.mu, d.sigma, d.min, d.h);
        out("  n_i: %s", java.util.Arrays.toString(d.counts));
        out("  інтервалів після об'єднання %d, ступенів"
                + " свободи %d, chi2 %.3f, chi2_кр %.3f -> %s",
                d.groups, d.df, d.chi2, d.chi2Cr,
                d.accepted() ? "приймається" : "відхиляється");
    }

    /** План 5x5 за факторами T_с, t_п і регресійний аналіз. */
    static void plan() throws IOException {
        int n = TC.length * TP.length;
        double[][] x = new double[n][];
        double[] yTz = new double[n];
        double[] yNp = new double[n];
        double[][] wTz = new double[TC.length][TP.length];
        double[][] wNp = new double[TC.length][TP.length];
        int j = 0;
        try (PrintStream csv = new PrintStream(
                dir + "/plan.csv", StandardCharsets.UTF_8)) {
            csv.println("Tc;tp;x1;x2;Tz;sdTz;epsTz;"
                    + "Np;sdNp;epsNp");
            for (int b = 0; b < TP.length; b++) {
                for (int a = 0; a < TC.length; a++) {
                    double[] tz = new double[P];
                    double[] np = new double[P];
                    Params p = Params.base()
                            .withTcTp(TC[a], TP[b]);
                    for (int i = 0; i < P; i++) {
                        Stats s = measure(p, i + 1);
                        tz[i] = s.meanTz();
                        np[i] = s.meanNp();
                    }
                    double x1 = (TC[a] - 30) / 20;   // (3.6)
                    double x2 = (TP[b] - 5) / 4;
                    x[j] = new double[] {x1, x2};
                    yTz[j] = StatMath.mean(tz);
                    yNp[j] = StatMath.mean(np);
                    wTz[a][b] = yTz[j];
                    wNp[a][b] = yNp[j];
                    double sT = StatMath.sd(tz);
                    double sN = StatMath.sd(np);
                    csv.printf("%.0f;%.0f;%.2f;%.2f;%.3f;%.3f;"
                            + "%.3f;%.4f;%.4f;%.4f%n", TC[a],
                            TP[b], x1, x2, yTz[j], sT,
                            StatMath.eps(sT, P), yNp[j], sN,
                            StatMath.eps(sN, P));
                    j++;
                }
            }
        }
        wide("plan_tz.csv", wTz);
        wide("plan_np.csv", wNp);
        out("План 5x5, %d замірів у точці, T_мод = %.0f с",
                P, T_MOD);
        regression("Tz", x, yTz);
        regression("Np", x, yNp);
    }

    static void wide(String file, double[][] w)
            throws IOException {
        try (PrintStream csv = new PrintStream(dir + "/" + file,
                StandardCharsets.UTF_8)) {
            csv.println("Tc;tp1;tp3;tp5;tp7;tp9");
            for (int a = 0; a < TC.length; a++) {
                StringBuilder row = new StringBuilder(
                        String.format("%.0f", TC[a]));
                for (double v : w[a]) {
                    row.append(String.format(";%.4f", v));
                }
                csv.println(row);
            }
        }
    }

    /** Лінійна з взаємодією і квадратична залежності. */
    static void regression(String name, double[][] x,
                           double[] y) {
        int n = y.length;
        double[][] lin = new double[n][];
        double[][] sq = new double[n][];
        for (int j = 0; j < n; j++) {
            double x1 = x[j][0];
            double x2 = x[j][1];
            lin[j] = new double[] {1, x1, x2, x1 * x2};
            sq[j] = new double[] {1, x1, x2, x1 * x2,
                x1 * x1, x2 * x2};
        }
        report(name + ": b0 + b1x1 + b2x2 + b12x1x2",
                new Regression(lin, y));
        report(name + ": + b11x1^2 + b22x2^2",
                new Regression(sq, y));
    }

    static void report(String title, Regression r) {
        out("  %s", title);
        StringBuilder b = new StringBuilder("    b:");
        StringBuilder t = new StringBuilder("    t:");
        for (int a = 0; a < r.b.length; a++) {
            b.append(String.format(" %.4f", r.b[a]));
            t.append(String.format(" %.2f", r.t[a]));
        }
        out("%s", b);
        out("%s (t_кр %.3f)", t, r.tCr);
        out("    R %.4f, F %.2f, F_кр %.3f", r.r, r.f, r.fCr);
    }
}
