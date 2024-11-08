package ugliML.util;

/**
 * 
 * @author fhho
 *
 */
public class MathUtils {

    /**
	 * The minimum double such that 1.0 + MACHINE_EPS > 1.0
	 */
    public static double MACHINE_EPS = 1.0;

    static {
        while (1.0 + MACHINE_EPS > 1.0) MACHINE_EPS /= 2.0;
        MACHINE_EPS *= 2.0;
    }

    /**
	 * Calculates the variance from a list of doubles
	 * 
	 * @param vals
	 * @return variance of vals
	 */
    public static double calcVariance(double[] vals) {
        double sum = 0.0;
        double sumSquares = 0.0;
        for (double val : vals) {
            sum += val;
            sumSquares += val * val;
        }
        return (sumSquares - sum * sum / vals.length) / vals.length;
    }

    /**
	 * Calculates the dot product of two arrays of doubles
	 * The lengths of the two arrays should be the same. No checking done for performance issues.
	 * 
	 * @param x
	 * @param y
	 * @return the dot product of the two arrays
	 */
    public static double dotProduct(double[] x, double[] y) {
        double result = 0.0;
        for (int i = 0; i < x.length; i++) {
            result += x[i] * y[i];
        }
        return result;
    }

    public static void multInto(double[] x, double[] y) {
        if (!(x.length == y.length)) throw new IllegalArgumentException("Vector lengths do not match!");
        for (int i = 0; i < x.length; i++) x[i] *= y[i];
    }

    public static void sumInto(double[] x, double[] y) {
        if (!(x.length == y.length)) throw new IllegalArgumentException("Vector lengths do not match!");
        for (int i = 0; i < x.length; i++) x[i] += y[i];
    }

    public static double[] setAll(double[] x, double val) {
        for (int i = 0; i < x.length; i++) x[i] = val;
        return x;
    }

    public static void scale(double[] x, double val) {
        for (int i = 0; i < x.length; i++) x[i] *= val;
    }

    public static double safeLog(double x) {
        return (x == 0.0) ? -Double.MIN_VALUE : Math.log(x);
    }

    public static void addAll(double[] x, double val) {
        for (int i = 0; i < x.length; i++) x[i] += val;
    }

    public static void logNormalize(double[] x) {
        double alpha = 0.0;
        for (double v : x) alpha += Math.exp(v);
        addAll(x, -Math.log(alpha));
    }

    public static void normalize(double[] x) {
        double sum = 0.0;
        for (double v : x) sum += v;
        scale(x, 1.0 / sum);
    }

    public static boolean deepEquals(double[] x, double[] y) {
        if (x == null || y == null || x.length != y.length) return false;
        for (int i = 0; i < x.length; i++) if (x[i] != y[i]) return false;
        return true;
    }

    public static void copyInto(double[] x, double[] y) {
        for (int i = 0; i < x.length; i++) x[i] = y[i];
    }

    /**
	 * return one longer array with its elements from the original array shifted to the right
	 * (zeroth index is 0.0)
	 * 
	 * @param x
	 * @return
	 */
    public static double[] arrayShiftRight(double[] x) {
        double[] temp = new double[x.length + 1];
        for (int i = 0; i < x.length; i++) {
            temp[i + 1] = x[i];
        }
        x = temp;
        return x;
    }

    /**
	 * return one shorter array with its elements from the original array shifted to the left
	 * 
	 * @param x
	 * @return
	 */
    public static double[] arrayShiftLeft(double[] x) {
        double[] temp = new double[x.length - 1];
        for (int i = 0; i < x.length; i++) {
            temp[i] = x[i + 1];
        }
        x = temp;
        return x;
    }
}
