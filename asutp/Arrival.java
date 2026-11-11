package asutp;

/** Надходження вимог «набір даних» ззовні. */
public class Arrival {
    private final FunRand rand;
    private final Params p;
    double tNext;   // t_0 -- наступний момент надходження

    public Arrival(FunRand rand, Params p) {
        this.rand = rand;
        this.p = p;
    }

    /** Інтервал між посилками за формулою (2.2). */
    public double interval(boolean c) {
        return c ? rand.unif(p.dN(), p.sN())
                 : rand.unif(p.dZ(), p.sZ());
    }

    /** t_0 := t + t_надх. */
    public void plan(double t, boolean c) {
        tNext = t + interval(c);
    }
}
