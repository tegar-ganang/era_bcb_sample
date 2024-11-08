package org.draycasejr.war.foundation;

import java.util.Random;

/**
 * @author Ray
 *
 * 
 */
public class RanUtil {

    private static Random rand = new Random();

    public static final int Z_ELSEWHERE = -10;

    public static void setRandSeed(long n) {
        rand.setSeed(n);
    }

    public static float random() {
        return rand.nextFloat();
    }

    public static final int d(int sides) {
        if (sides <= 0) return 0;
        return rand.nextInt(sides) + 1;
    }

    public static final int pick(int[] a) {
        return a[r(a.length)];
    }

    public static final String pick(String[] a) {
        return a[r(a.length)];
    }

    public static final char pick(String a) {
        return a.charAt(r(a.length()));
    }

    public static final int min(int a, int b) {
        return (a < b) ? a : b;
    }

    public static final int max(int a, int b) {
        return (a > b) ? a : b;
    }

    public static final int middle(int a, int b, int c) {
        if (a > b) {
            if (b > c) return b;
            if (a > c) return c;
            return a;
        }
        if (a > c) return a;
        if (b > c) return c;
        return b;
    }

    public static final int niceNumber(int x) {
        int p = 1;
        while (x >= 100) {
            x /= 10;
            p *= 10;
        }
        if (x > 30) x = 5 * (x / 5);
        return x * p;
    }

    public static int e(int n) {
        if (n <= 0) return 0;
        int result = 0;
        while ((rand.nextInt() % (n + 1)) != 0) result++;
        return result;
    }

    public static int ln(double x) {
        return ln(x, 1.0);
    }

    public static int ln(double x, double s) {
        double n = rand.nextGaussian();
        n = n * s;
        return (int) Math.round(x * Math.exp(n));
    }

    public static int po(double x) {
        int r = 0;
        double a = rand.nextDouble();
        if (a >= 0.99999999) return 0;
        double p = Math.exp(-x);
        while (a >= p) {
            r++;
            a = a - p;
            p = p * x / r;
        }
        return r;
    }

    public static int po(int numerator, int denominator) {
        return po(((double) numerator) / denominator);
    }

    public static boolean sometimes() {
        return (rand.nextFloat() < 0.1);
    }

    public static boolean often() {
        return (rand.nextFloat() < 0.4);
    }

    public static boolean rarely() {
        return (rand.nextFloat() < 0.01);
    }

    public static boolean usually() {
        return (rand.nextFloat() < 0.8);
    }

    public static int rspread(int a, int b) {
        if (a > b) {
            int t = a;
            a = b;
            b = t;
        }
        return rand.nextInt(b - a + 1) + a;
    }

    public static final int sign(double a) {
        return (a < 0) ? -1 : ((a > 0) ? 1 : 0);
    }

    public static final int sign(int a) {
        return (a < 0) ? -1 : ((a > 0) ? 1 : 0);
    }

    public static final int abs(int a) {
        return (a >= 0) ? a : -a;
    }

    /**
	 * Random number from zero to s-1
	 * 
	 * @param s Upper bound (excluded)
	 * @return
	 */
    public static final int r(int s) {
        if (s <= 0) return 0;
        return rand.nextInt(s);
    }

    /**
     *  Returns random number uniformly distributed in [n1, n2] range.
     *  It is allowed to have to n1 > n2, or n1 < n2, or n1 == n2.
     */
    public static final int r(int n1, int n2) {
        int min, max;
        min = Math.min(n1, n2);
        max = Math.max(n1, n2);
        final int diff = max - min;
        final int ret = min + rand.nextInt(diff + 1);
        return ret;
    }

    /**
     *  The method evaluates if some event happened with specified probability.
     *  You pass probability of event as argument. The method calls
     *  Random.nextDouble() and returns true when event happened; otherwise
     *  returns false.
     *
     *  @param prob Probability (in [0,1] range) of event.
     */
    public static boolean p(final double prob) {
        if (prob == 0.) return false;
        return rand.nextDouble() <= prob;
    }

    public static final int round(double n) {
        int i = (int) n;
        if (rand.nextDouble() < (n - i)) i++;
        return i;
    }

    public static final int a(int s) {
        return r(s + 1) + r(s + 1);
    }

    public static final int d3() {
        return d(3);
    }

    public static final int d4() {
        return d(4);
    }

    public static final int d6() {
        return d(6);
    }

    public static final int d8() {
        return d(8);
    }

    public static final int d10() {
        return d(10);
    }

    public static final int d12() {
        return d(12);
    }

    public static final int d20() {
        return d(20);
    }

    public static final int d100() {
        return d(100);
    }

    public static int best(int r, int n, int s) {
        if ((n <= 0) || (r < 0) || (r > n) || (s < 0)) return 0;
        int[] rolls = new int[n];
        for (int i = 0; i < n; i++) rolls[i] = d(s);
        boolean found;
        do {
            found = false;
            for (int x = 0; x < n - 1; x++) {
                if (rolls[x] < rolls[x + 1]) {
                    int t = rolls[x];
                    rolls[x] = rolls[x + 1];
                    rolls[x + 1] = t;
                    found = true;
                }
            }
        } while (found);
        int sum = 0;
        for (int i = 0; i < r; i++) sum += rolls[i];
        return sum;
    }

    public static int d(int number, int sides) {
        int total = 0;
        for (int i = 0; i < number; i++) {
            total += d(sides);
        }
        return total;
    }

    public static final int index(int a, int[] aa) {
        for (int i = 0; i < aa.length; i++) {
            if (aa[i] == a) return i;
        }
        return -1;
    }

    public static int percentile(int var, int base) {
        if (base == 0) {
            return 0;
        }
        int p = var * 100 / base;
        if ((var > 0) && (p == 0)) {
            p = 1;
        }
        return p;
    }
}
