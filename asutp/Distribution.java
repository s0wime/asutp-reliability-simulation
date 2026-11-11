package asutp;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Ідентифікація закону розподілу: гіпотеза -- нормальний. */
public class Distribution {
    final double[] x;        // масив спостережуваних значень
    final int k;             // кількість інтервалів гістограми
    final double min, h;     // (2.1), (2.2)
    final int[] counts;      // n_i
    final double mu, sigma;  // параметри закону, (2.3), (2.4)
    int groups;              // інтервалів після об'єднання
    int df;                  // ступенів свободи
    double chi2, chi2Cr;     // (2.10) і критичне значення

    public Distribution(double[] x, int k) {
        this.x = x;
        this.k = k;
        double lo = x[0];
        double hi = x[0];
        for (double v : x) {
            lo = Math.min(lo, v);
            hi = Math.max(hi, v);
        }
        min = lo;
        h = (hi - lo) / k;
        counts = new int[k];
        for (double v : x) {
            int i = (int) ((v - min) / h);
            counts[Math.min(i, k - 1)]++;   // max -- в останній
        }
        mu = StatMath.mean(x);
        sigma = StatMath.sd(x);
        chiSquare();
    }

    /** Ймовірність влучення в інтервал i..j, формула (2.5). */
    double prob(int i, int j) {
        double a = i == 0 ? Double.NEGATIVE_INFINITY
                : min + i * h;
        double b = j == k - 1 ? Double.POSITIVE_INFINITY
                : min + (j + 1) * h;
        return StatMath.normalCdf(b, mu, sigma)
                - StatMath.normalCdf(a, mu, sigma);
    }

    /** Критерій (2.10) з об'єднанням інтервалів, n_i >= 5. */
    private void chiSquare() {
        List<int[]> g = new ArrayList<>();   // {з, по, n}
        int from = 0;
        int n = 0;
        for (int i = 0; i < k; i++) {
            n += counts[i];
            if (n >= 5) {
                g.add(new int[] {from, i, n});
                from = i + 1;
                n = 0;
            }
        }
        if (n > 0) {                         // залишок справа
            int[] last = g.get(g.size() - 1);
            last[1] = k - 1;
            last[2] += n;
        }
        chi2 = 0;
        for (int[] gr : g) {
            double e = x.length * prob(gr[0], gr[1]);
            chi2 += (gr[2] - e) * (gr[2] - e) / e;
        }
        groups = g.size();
        df = groups - 1 - 2;                 // 2 параметри
        chi2Cr = StatMath.chi2Crit(df);
    }

    public boolean accepted() {
        return chi2 < chi2Cr;
    }

    /** Гістограма і теоретична кількість влучень у CSV. */
    public void write(String file) throws IOException {
        try (PrintStream out = new PrintStream(file,
                StandardCharsets.UTF_8)) {
            out.println("left;mid;n;npT");
            for (int i = 0; i < k; i++) {
                double a = min + i * h;
                double e = x.length
                        * (StatMath.normalCdf(a + h, mu, sigma)
                        - StatMath.normalCdf(a, mu, sigma));
                out.printf("%.4f;%.4f;%d;%.3f%n",
                        a, a + h / 2, counts[i], e);
            }
            out.printf("%.4f;%.4f;0;nan%n",
                    min + k * h, min + k * h);
        }
    }
}
