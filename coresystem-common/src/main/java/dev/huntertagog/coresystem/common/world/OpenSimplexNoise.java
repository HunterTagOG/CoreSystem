package dev.huntertagog.coresystem.common.world;

import java.util.Random;

public final class OpenSimplexNoise {

    private static final double SQRT3 = Math.sqrt(3.0);
    private static final double F2 = 0.5 * (SQRT3 - 1.0);
    private static final double G2 = (3.0 - SQRT3) / 6.0;

    private final short[] perm;
    private final short[] permMod12;

    public OpenSimplexNoise(long seed) {

        perm = new short[256];
        permMod12 = new short[256];

        short[] p = new short[256];
        for (short i = 0; i < 256; i++) {
            p[i] = i;
        }

        Random random = new Random(seed);

        for (int i = 255; i >= 0; i--) {
            int j = random.nextInt(i + 1);
            short tmp = p[i];
            p[i] = p[j];
            p[j] = tmp;
        }

        // fill perm & permMod12
        for (int i = 0; i < 256; i++) {
            perm[i] = p[i];
            permMod12[i] = (short) (p[i] % 12);
        }
    }

    private static final double[][] GRAD2 = {
            {1, 1}, {-1, 1}, {1, -1}, {-1, -1},
            {1, 0}, {-1, 0}, {1, 0}, {-1, 0},
            {0, 1}, {0, -1}, {0, 1}, {0, -1}
    };

    public static double fast2d(double v, double v1) {
        return Math.floor(v) + Math.floor(v1) * 57;
    }

    public double noise2(double x, double y) {

        double s = (x + y) * F2;
        int i = fastFloor(x + s);
        int j = fastFloor(y + s);

        double t = (i + j) * G2;

        double X0 = i - t;
        double Y0 = j - t;

        double x0 = x - X0;
        double y0 = y - Y0;

        int i1, j1;

        if (x0 > y0) {
            i1 = 1;
            j1 = 0;
        } else {
            i1 = 0;
            j1 = 1;
        }

        double x1 = x0 - i1 + G2;
        double y1 = y0 - j1 + G2;

        double x2 = x0 - 1.0 + 2.0 * G2;
        double y2 = y0 - 1.0 + 2.0 * G2;

        int ii = i & 0xFF;
        int jj = j & 0xFF;

        double n0, n1, n2;

        // Corner 0
        double t0 = 0.5 - x0 * x0 - y0 * y0;
        if (t0 < 0) n0 = 0;
        else {
            int gi0 = permMod12[(ii + perm[jj]) & 0xFF];
            double[] g = GRAD2[gi0];
            t0 *= t0;
            n0 = t0 * t0 * (g[0] * x0 + g[1] * y0);
        }

        // Corner 1
        double t1 = 0.5 - x1 * x1 - y1 * y1;
        if (t1 < 0) n1 = 0;
        else {
            int gi1 = permMod12[(ii + i1 + perm[(jj + j1) & 0xFF]) & 0xFF];
            double[] g = GRAD2[gi1];
            t1 *= t1;
            n1 = t1 * t1 * (g[0] * x1 + g[1] * y1);
        }

        // Corner 2
        double t2 = 0.5 - x2 * x2 - y2 * y2;
        if (t2 < 0) n2 = 0;
        else {
            int gi2 = permMod12[(ii + 1 + perm[(jj + 1) & 0xFF]) & 0xFF];
            double[] g = GRAD2[gi2];
            t2 *= t2;
            n2 = t2 * t2 * (g[0] * x2 + g[1] * y2);
        }

        return 70.0 * (n0 + n1 + n2);
    }

    public double noise3(double x, double y, double z) {
        return noise2(x + noise2(y, z), y + noise2(z, x));
    }

    public double ridge(double x, double z, double freq, double gain) {
        double n = Math.abs(noise2(x * freq, z * freq));
        return gain * (1.0 - n);
    }

    public double domainWarp2(double x, double z) {
        double qx = noise2(x * 0.15, z * 0.15) * 8.0;
        double qz = noise2((x + 1000) * 0.15, (z - 1000) * 0.15) * 8.0;

        return noise2(x + qx, z + qz);
    }

    private static int fastFloor(double x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }
}
