package asutp;

import java.util.Random;

/** Генератор unif(d, s) за формулою (2.3). */
public class FunRand {
    private final Random rng;

    public FunRand(long seed) {
        rng = new Random(seed);
    }

    /** r = d - s + 2 s zeta, zeta рівномірна на (0; 1). */
    public double unif(double d, double s) {
        double zeta = rng.nextDouble();
        return d - s + 2.0 * s * zeta;
    }
}
