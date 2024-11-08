import tools.ComputationalException;
import tools.Matrix;
import tools.Vector;

public class Task4 {

    private static double ALPHA0 = 2;

    private static double ALPHA1 = -2;

    private static double BETA0 = 0;

    private static double BETA1 = 1;

    private static double A = 0;

    private static double B = -2 / Math.pow(3, 0.5);

    private static double[] a;

    private static double[] b;

    private static double[] c;

    private static double[] d;

    public static void main(String[] arg) {
        int n = 10;
        double a = 0;
        double b = 1;
        double h = (b - a) / n;
        System.out.println("Node amount: " + n);
        System.out.println("a = " + a);
        System.out.println("b = " + b);
        System.out.println("h = " + h);
        System.out.println();
        preciseApprCoeff(n, a, h);
        Vector apprSolution = driving(n);
        apprSolution.print();
        System.out.println();
        Vector solution = getPreciseSolution(n, a, h);
        solution.print();
        System.out.println();
        solution.add(apprSolution.multiply(-1)).print();
        System.out.println();
        System.out.println(solution.add(apprSolution.multiply(-1)).norm());
    }

    static void apprCoeff(int n, double h, double start) {
        a = new double[n + 1];
        b = new double[n + 1];
        c = new double[n + 1];
        d = new double[n + 1];
        b[0] = ALPHA0 - ALPHA1 * h;
        c[0] = ALPHA1;
        d[0] = A * h;
        a[n] = -BETA1;
        b[n] = BETA0 * h + BETA1;
        d[n] = B * h;
        calculateMidCoefficients(n, start, h);
    }

    static void preciseApprCoeff(int n, double start, double h) {
        a = new double[n + 1];
        b = new double[n + 1];
        c = new double[n + 1];
        d = new double[n + 1];
        calculateMidCoefficients(n, start, h);
        b[0] = 2 * h * ALPHA0 + ALPHA1 * (a[1] / c[1] - 3);
        c[0] = ALPHA1 * (b[1] / c[1] + 4);
        d[0] = 2 * h * A + ALPHA1 * d[1] / c[1];
        a[n] = -BETA1 * (4 + b[n - 1] / a[n - 1]);
        b[n] = 2 * h * BETA0 + BETA1 * (3 - c[n - 1] / a[n - 1]);
        d[n] = 2 * h * B - BETA1 * d[n - 1] / a[n - 1];
    }

    static Vector driving(int n) {
        double[] m = new double[n + 1];
        double[] k = new double[n + 1];
        m[1] = -c[0] / b[0];
        k[1] = d[0] / b[0];
        for (int i = 1; i < n; i++) {
            m[i + 1] = -c[i] / (a[i] * m[i] + b[i]);
            k[i + 1] = (d[i] - a[i] * k[i]) / (a[i] * m[i] + b[i]);
        }
        double[] y = new double[n + 1];
        y[n] = (d[n] - a[n] * k[n]) / (a[n] * m[n] + b[n]);
        for (int i = n - 1; i >= 0; i--) {
            y[i] = m[i + 1] * y[i + 1] + k[i + 1];
        }
        return new Vector(y);
    }

    private static void calculateMidCoefficients(int n, double start, double h) {
        for (int i = 1; i < n; i++) {
            a[i] = p(start + i * h) - q(start + i * h) * h / 2;
            b[i] = -2 * p(start + i * h) + r(start + i * h) * Math.pow(h, 2.0);
            c[i] = p(start + i * h) + q(start + i * h) * h / 2;
            d[i] = f(start + i * h) * Math.pow(h, 2.0);
        }
    }

    static double p(double x) {
        return 1;
    }

    static double q(double x) {
        return 0;
    }

    static double r(double x) {
        return -8 / Math.pow(2 * x + 1, 2.0);
    }

    static double f(double x) {
        return 18 / Math.pow(2 * x + 1, 1.5);
    }

    static Vector getPreciseSolution(int n, double a, double h) {
        double[] y = new double[n + 1];
        for (int i = 0; i < n + 1; i++) {
            y[i] = preciseSolution(a + i * h);
        }
        return new Vector(y);
    }

    private static double preciseSolution(double x) {
        return -2 * Math.pow(2 * x + 1, 0.5);
    }
}
