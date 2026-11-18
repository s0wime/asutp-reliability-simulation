package asutp;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Протокол подій і прогони верифікації моделі. */
public class Main {
    static final long SEED = 1;
    static final double T_MOD = 100_000;

    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.ROOT);
        String dir = args.length > 0 ? args[0] : "results";
        protocol(dir + "/protocol.txt");
        verification(dir + "/verification.csv");
    }

    /** Протокол подій перших 480 с модельного часу. */
    static void protocol(String file) throws IOException {
        try (PrintStream out = new PrintStream(file,
                StandardCharsets.UTF_8)) {
            out.println("       t  подія"
                    + "                          K1-K6 C гальм"
                    + " Nнадх Nп Nв        t0");
            Model m = new Model(Params.base(), SEED);
            m.setTrace(out, 0, 480);
            m.run(480);
        }
    }

    /** Прогони з варіюванням вхідних змінних. */
    static void verification(String file) throws IOException {
        Params b = Params.base();
        Object[][] runs = {
            {1, b},
            {2, b.withN(6, 2)},
            {3, b.withN(15, 2)},
            {4, b.withZ(12, 4)},
            {5, b.withZ(30, 4)},
            {6, b.withV(150, 30)},
            {7, b.withV(600, 30)},
        };
        try (PrintStream csv = new PrintStream(file,
                StandardCharsets.UTF_8)) {
            csv.println("run;dN;sN;dZ;sZ;dV;sV;Nnadh;Nobr;Np;"
                    + "Nv;Nz;Tsum;Tacc;Tz;Np_mean;kz;pp");
            for (Object[] r : runs) {
                Params p = (Params) r[1];
                Stats s = new Model(p, SEED).run(T_MOD);
                String line = String.format(
                        "%d;%s;%s;%s;%s;%s;%s;%d;%d;%d;%d;%d;"
                        + "%.3f;%.3f;%.2f;%.3f;%.4f;%.4f",
                        r[0], p.dN(), p.sN(), p.dZ(), p.sZ(),
                        p.dV(), p.sV(), s.nNadh(), s.nObr(),
                        s.nP(), s.nV(), s.nZ(), s.tSum(),
                        s.tAcc(), s.meanTz(), s.meanNp(),
                        s.kz(), s.pp());
                csv.println(line);
                System.out.println(line);
            }
        }
    }
}
