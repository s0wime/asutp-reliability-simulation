package asutp;

import java.util.function.DoubleUnaryOperator;

/** Пристрій обслуговування однопристроєвої СМО без черги. */
public class Device {
    public static final double INF = Double.POSITIVE_INFINITY;

    final String name;
    // тривалість обслуговування як функція моменту початку
    private final DoubleUnaryOperator service;
    int k = 0;          // K: 0 -- вільний, 1 -- зайнятий
    double tNext = INF; // момент завершення обслуговування

    public Device(String name, DoubleUnaryOperator service) {
        this.name = name;
        this.service = service;
    }

    /** Зайняти пристрій: K := 1, tNext := t + g. */
    public void start(double t) {
        k = 1;
        tNext = t + service.applyAsDouble(t);
    }

    /** Звільнити пристрій: K := 0, tNext := INF. */
    public void release() {
        k = 0;
        tNext = INF;
    }

    public boolean busy() {
        return k == 1;
    }
}
