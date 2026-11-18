package asutp;

import java.io.PrintStream;
import java.util.List;

/** Мережа МО рис. 2.1 і алгоритм її імітації. */
public class Model {
    private final Params p;
    private final FunRand rand;
    final Arrival arrival;
    final Device d1, d2, d3, d4, d5, d6;
    private final List<Device> devices;
    final Stats stats = new Stats();

    double t = 0;        // поточний момент часу
    boolean c = true;    // ознака C (формула 2.2)

    private PrintStream trace;   // протокол подій
    private double traceFrom, traceTo;

    public Model(Params p, long seed) {
        this.p = p;
        rand = new FunRand(seed);
        arrival = new Arrival(rand, p);
        d1 = new Device("D1", s -> p.tObr());
        d2 = new Device("D2", s -> p.tObr());
        d3 = new Device("D3",
                s -> rand.unif(p.dV(), p.sV()));
        d4 = new Device("D4",
                s -> p.tC() * Math.ceil(s / p.tC()) - s);
        d5 = new Device("D5", s -> p.tVidn() - p.tP());
        d6 = new Device("D6", s -> p.tP());
        devices = List.of(d1, d2, d3, d4, d5, d6);
    }

    public void setTrace(PrintStream out, double from,
                         double to) {
        trace = out;
        traceFrom = from;
        traceTo = to;
    }

    // Блокування (2.1): true -- маршрут заблоковано
    boolean b1() { return !d3.busy(); }      // L_3 = 0
    boolean b2() { return !d5.busy(); }      // L_5 = 0
    boolean b3() { return !(b1() && b2()); }
    boolean b4() { return !d3.busy(); }      // L_3 = 0
    boolean b5() { return d3.busy(); }       // L_3 = 1

    /** Алгоритм імітації: просування до найближчої події. */
    public Stats run(double tMod) {
        stats.tMod = tMod;
        d3.start(t);           // вимога стану в D3
        arrival.plan(t, c);    // перша посилка через tau_н
        log("початковий стан");
        while (t < tMod) {
            double tNext = arrival.tNext;   // найближча подія
            for (Device d : devices) {
                tNext = Math.min(tNext, d.tNext);
            }
            stats.onInterval(t, Math.min(tNext, tMod));
            if (tNext > tMod) {
                t = tMod;
                break;
            }
            t = tNext;                      // просування часу
            changeState();                  // зміна стану
        }
        return stats;
    }

    /** Спочатку завершення в D1...D6, потім надходження. */
    private void changeState() {
        for (Device d : devices) {
            if (d.tNext == t) {
                finish(d);
            }
        }
        if (arrival.tNext == t) {
            arrive();
        }
    }

    private void finish(Device d) {
        d.release();
        if (d == d1) {
            if (!b4()) {
                exitProcessed("D1 -> оброблено (B4)");
            } else {
                exitLost("D1 -> пропущено (B5)");
            }
        } else if (d == d2) {
            exitProcessed("D2 -> оброблено");
        } else if (d == d3) {
            stats.nV++;
            d4.start(t);
            log("D3 -> D4: відмова");
        } else if (d == d4) {
            d6.start(t);
            log("D4 -> D6: виявлення");
        } else if (d == d6) {
            c = true;                 // нормальний темп
            stats.onConnected(t);
            d5.start(t);
            log("D6 -> D5: резерв підключено");
        } else {
            d3.start(t);
            log("D5 -> D3: відновлення");
        }
    }

    private void arrive() {
        boolean cNow = c;   // C на момент посилки
        stats.nNadh++;
        stats.onArrival(t, cNow);
        arrival.plan(t, cNow);
        String how;
        if (!b1()) {
            d1.start(t);
            how = "-> D1";
        } else if (!b2()) {
            d2.start(t);
            how = "-> D2";
        } else {
            stats.nP++;
            c = false;
            how = "-> пропущено (B3)";
        }
        log("надходження " + how);
    }

    private void exitProcessed(String what) {
        stats.nObr++;
        c = true;
        log(what);
    }

    private void exitLost(String what) {
        stats.nP++;
        if (b2()) {          // резерв ще не підключено
            c = false;
        }
        log(what);
    }

    private void log(String event) {
        if (trace == null || t < traceFrom || t > traceTo) {
            return;
        }
        trace.printf("%8.3f  %-30s %d%d%d%d%d%d  %d  %d"
                        + "  %3d  %2d  %2d  %8s%n",
                t, event, d1.k, d2.k, d3.k, d4.k, d5.k, d6.k,
                c ? 1 : 0, stats.braked ? 1 : 0,
                stats.nNadh, stats.nP, stats.nV,
                fmt(arrival.tNext));
    }

    static String fmt(double x) {
        return Double.isInfinite(x) ? "inf"
                : String.format("%.3f", x);
    }
}
