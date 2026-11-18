package asutp;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Пробні запуски пропозицій (розділ 5). */
public class Proposals {
    static final String[] NAME = {"База", "П1", "П2", "П3"};
    static final double[] TC = {30, 10, 30, 10};  // T_с, с
    static final double[] TP = {5, 5, 1, 1};      // t_п, с
    static final String[] CH = {"Tz", "Np", "kz", "pp", "Nv"};
    // рівняння регресії (4.7): b0, b1, b2, b12
    static final double[][] B = {
        {11.5763, 8.9366, 2.0715, 0.5861},
        {1.8619, 0.5406, 0.2702, -0.0657}};

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.ROOT);
        String dir = args.length > 0 ? args[0] : "results";
        int p = Experiment.P;
        // y[варіант][характеристика][замір]
        double[][][] y = new double[NAME.length][CH.length][p];
        for (int v = 0; v < NAME.length; v++) {
            Params par = Params.base().withTcTp(TC[v], TP[v]);
            for (int i = 0; i < p; i++) {
                Stats s = Experiment.measure(par, i + 1);
                double[] r = {s.meanTz(), s.meanNp(), s.kz(),
                    s.pp(), s.nV()};
                for (int c = 0; c < CH.length; c++) {
                    y[v][c][i] = r[c];
                }
            }
        }
        try (PrintStream csv = new PrintStream(
                dir + "/proposals.csv", StandardCharsets.UTF_8)) {
            csv.println("var;Tc;tp;ch;mean;eps;diff;epsDiff;"
                    + "rel;regr");
            for (int v = 0; v < NAME.length; v++) {
                for (int c = 0; c < CH.length; c++) {
                    row(csv, v, c, y[v][c], y[0][c], p);
                }
            }
        }
        System.out.printf("Пропозиції: %d замірів, T_пер = %.0f с,"
                + " T_мод = %.0f с -> %s/proposals.csv%n", p,
                Experiment.T_WARM, Experiment.T_MOD, dir);
    }

    /** Рядок: середнє, зміна відносно бази та її точність. */
    static void row(PrintStream csv, int v, int c, double[] y,
                    double[] base, int p) {
        double[] d = new double[p];   // різниці пар замірів
        for (int i = 0; i < p; i++) {
            d[i] = y[i] - base[i];
        }
        double m = StatMath.mean(y);
        double mb = StatMath.mean(base);
        double regr = Double.NaN;
        if (c < B.length) {           // прогноз за (4.7)
            double x1 = (TC[v] - 30) / 20;
            double x2 = (TP[v] - 5) / 4;
            double[] b = B[c];
            regr = b[0] + b[1] * x1 + b[2] * x2
                    + b[3] * x1 * x2;
        }
        csv.printf("%s;%.0f;%.0f;%s;%.4f;%.4f;%.4f;%.4f;"
                + "%.1f;%.4f%n", NAME[v], TC[v], TP[v], CH[c], m,
                StatMath.eps(StatMath.sd(y), p),
                StatMath.mean(d), StatMath.eps(StatMath.sd(d), p),
                100 * (m - mb) / mb, regr);
    }
}
