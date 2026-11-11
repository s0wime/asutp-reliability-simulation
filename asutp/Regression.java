package asutp;

/** Регресійний аналіз за методом найменших квадратів. */
public class Regression {
    final double[] b;    // параметри, формула (3.9)
    final double r;      // індекс кореляції, (2.23)
    final double f;      // критерій Фішера, (2.25)
    final double fCr;    // F_кр, (2.26)
    final double[] t;    // критерій Ст'юдента, (2.28)
    final double tCr;    // t_кр, (2.29)

    /** x -- рядки (1, f1(x), ..., fm(x)), y -- відгуки. */
    public Regression(double[][] x, double[] y) {
        int n = y.length;
        int m = x[0].length;
        double[][] xtx = new double[m][m];
        double[] xty = new double[m];
        for (int j = 0; j < n; j++) {
            for (int a = 0; a < m; a++) {
                xty[a] += x[j][a] * y[j];
                for (int c = 0; c < m; c++) {
                    xtx[a][c] += x[j][a] * x[j][c];
                }
            }
        }
        double[][] inv = inverse(xtx);
        b = new double[m];
        for (int a = 0; a < m; a++) {
            for (int c = 0; c < m; c++) {
                b[a] += inv[a][c] * xty[c];
            }
        }
        double my = StatMath.mean(y);
        double sFact = 0;
        double sAll = 0;
        double sRes = 0;
        for (int j = 0; j < n; j++) {
            double fj = value(x[j]);
            sFact += (fj - my) * (fj - my);
            sAll += (y[j] - my) * (y[j] - my);
            sRes += (fj - y[j]) * (fj - y[j]);
        }
        r = Math.sqrt(sFact / sAll);
        int k = m - 1;
        f = r * r / (1 - r * r) * (n - k - 1) / k;
        fCr = StatMath.fCrit(k, n - k - 1);
        double s2 = sRes / (n - k - 1);
        t = new double[m];
        for (int a = 0; a < m; a++) {
            t[a] = Math.abs(b[a]) / Math.sqrt(inv[a][a] * s2);
        }
        tCr = StatMath.tCrit(n - k - 1);
    }

    /** Значення рівняння регресії f(x, b), (3.7). */
    public double value(double[] row) {
        double s = 0;
        for (int a = 0; a < b.length; a++) {
            s += b[a] * row[a];
        }
        return s;
    }

    /** Обернена матриця методом Гаусса -- Жордана. */
    static double[][] inverse(double[][] a) {
        int m = a.length;
        double[][] w = new double[m][2 * m];
        for (int i = 0; i < m; i++) {
            System.arraycopy(a[i], 0, w[i], 0, m);
            w[i][m + i] = 1;
        }
        for (int c = 0; c < m; c++) {
            int p = c;
            for (int i = c + 1; i < m; i++) {
                if (Math.abs(w[i][c]) > Math.abs(w[p][c])) {
                    p = i;
                }
            }
            double[] tmp = w[c];
            w[c] = w[p];
            w[p] = tmp;
            double d = w[c][c];
            for (int j = 0; j < 2 * m; j++) {
                w[c][j] /= d;
            }
            for (int i = 0; i < m; i++) {
                if (i != c) {
                    double q = w[i][c];
                    for (int j = 0; j < 2 * m; j++) {
                        w[i][j] -= q * w[c][j];
                    }
                }
            }
        }
        double[][] inv = new double[m][m];
        for (int i = 0; i < m; i++) {
            System.arraycopy(w[i], m, inv[i], 0, m);
        }
        return inv;
    }
}
