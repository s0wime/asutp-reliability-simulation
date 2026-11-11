package asutp;

/** Вхідні змінні (табл. 1.1) і параметри (табл. 1.2), с. */
public record Params(
        double dN, double sN,  // tau_н = unif(dN, sN)
        double dZ, double sZ,  // tau_з = unif(dZ, sZ)
        double dV, double sV,  // tau_в = unif(dV, sV)
        double tObr,           // t_обр, обробка даних
        double tC,             // T_с, період сигналу
        double tP,             // t_п, підключення резерву
        double tVidn) {        // t_відн, відновлення

    /** Значення з тексту завдання. */
    public static Params base() {
        return new Params(10, 2, 20, 4, 300, 30,
                3, 30, 5, 100);
    }

    public Params withN(double d, double s) {
        return new Params(d, s, dZ, sZ, dV, sV,
                tObr, tC, tP, tVidn);
    }

    public Params withZ(double d, double s) {
        return new Params(dN, sN, d, s, dV, sV,
                tObr, tC, tP, tVidn);
    }

    public Params withV(double d, double s) {
        return new Params(dN, sN, dZ, sZ, d, s,
                tObr, tC, tP, tVidn);
    }

    /** Фактори експерименту (розділ 4): T_с і t_п. */
    public Params withTcTp(double tc, double tp) {
        return new Params(dN, sN, dZ, sZ, dV, sV,
                tObr, tc, tp, tVidn);
    }
}
