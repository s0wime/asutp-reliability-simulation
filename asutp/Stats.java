package asutp;

/** Збір статистики і вихідні характеристики (2.4). */
public class Stats {
    long nNadh;      // N_надх -- надійшло вимог «набір даних»
    long nObr;       // N_обр -- вийшли через «оброблено»
    long nP;         // N_п -- вийшли через «пропущено»
    long nV;         // N_в -- відмов (входів у D4)
    long nZ;         // N_з -- завершених періодів гальмування
    double tSum;     // T_Σ -- їх сумарна тривалість
    double tAcc;     // контроль: час гальмування на інтервалах
    boolean braked;  // процес зараз загальмований
    double tStart;   // t_поч поточного періоду гальмування
    double tMod;     // T_мод

    /** Посилка даних з ознакою C (правило 4 у п. 1.3). */
    void onArrival(double t, boolean c) {
        if (!c && !braked) {          // початок періоду
            braked = true;
            tStart = t;
        } else if (c && braked) {     // кінець періоду
            braked = false;
            tSum += t - tStart;
            nZ++;
        }
    }

    /** Статистика на інтервалі (t; tNext). */
    void onInterval(double t, double tNext) {
        if (!warmed && tNext >= tWarm) {
            startCollection();        // кінець T_пер
        }
        observe();
        if (braked) {
            tAcc += tNext - Math.max(t, tWarm);
        }
    }

    public double meanTz() {          // середнє T_з, (2.4)
        return nZ > 0 ? tSum / nZ : 0;
    }

    public double meanNp() {          // середнє N_п, (2.4)
        return nV > 0 ? (double) nP / nV : 0;
    }

    public double kz() {              // k_з, (2.4)
        return tSum / (tMod - tWarm);
    }

    public double pp() {              // p_п, (2.4)
        return nNadh > 0 ? (double) nP / nNadh : 0;
    }

    public long nNadh() { return nNadh; }
    public long nObr() { return nObr; }
    public long nP() { return nP; }
    public long nV() { return nV; }
    public long nZ() { return nZ; }
    public double tSum() { return tSum; }
    public double tAcc() { return tAcc; }

    // ---- Експериментальне дослідження (розділ 4) ----
    double tWarm = 0;   // T_пер -- початок збору статистики
    boolean warmed;     // збір статистики розпочато
    // окремі спостереження (якщо список не null):
    java.util.List<Double> periods;  // тривалості періодів
    java.util.List<Long> lostPerFail; // N_п між відмовами
    private double lastTSum;
    private long lastNV, nPAtFail;

    /** Обнулення лічильників у момент T_пер. */
    private void startCollection() {
        warmed = true;
        nNadh = nObr = nP = nV = nZ = 0;
        tSum = 0;
        // період, що триває в момент T_пер, буде
        // враховано повністю (за моментом закінчення)
        tAcc = braked ? tWarm - tStart : 0;
        lastTSum = 0;
        lastNV = 0;
        nPAtFail = 0;
    }

    /** Окремі спостереження для оцінки sigma. */
    private void observe() {
        if (periods != null && tSum > lastTSum) {
            periods.add(tSum - lastTSum);
            lastTSum = tSum;
        }
        if (lostPerFail != null && nV > lastNV) {
            if (lastNV > 0) {
                lostPerFail.add(nP - nPAtFail);
            }
            nPAtFail = nP;
            lastNV = nV;
        }
    }
}
