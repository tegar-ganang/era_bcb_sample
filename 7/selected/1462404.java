package com.dukesoftware.utils.interpolation;

import com.dukesoftware.utils.math.function.IFunction;

/**
 * int n = 11;<br>
 * double[] x = new double[n];<br>
 * double[] y = new double[n];<br>
 * for(int i=0; i < n; i++) {<br>
 * &nbsp;x[i] = -5 + i;<br>
 * &nbsp;y[i] = 1/(1 + x[i]*x[i]);<br>
 * }<br>
 * Spline s = new Spline(x, y);<br>
 * for(int i=0; i < 2*n+1; i++) {<br>
 * &nbsp;double u = -5.5+i*0.5;<br>
 * &nbsp;double v = s.map(u);<br>
 * &nbsp;System.out.println("f("+u+")="+v);<br>
 *  * * 
 * 
 *
 *
 */
public class SplineAnotherImpl implements IFunction {

    private final double[] x, y, a;

    private final int n;

    public SplineAnotherImpl(double[] x, double[] y) {
        this.x = x;
        this.y = y;
        n = x.length;
        a = maketable(x, y, n);
    }

    /**
	 * Create table.
	 *
	 */
    private static double[] maketable(double[] x, double[] y, int n) {
        double[] a = new double[n];
        double[] h = new double[n];
        double[] d = new double[n];
        int n_1 = n - 1;
        int n_2 = n - 2;
        a[0] = 0;
        a[n_1] = 0;
        for (int i = 0; i < n_1; i++) {
            h[i] = x[i + 1] - x[i];
            d[i + 1] = (y[i + 1] - y[i]) / h[i];
        }
        a[1] = d[2] - d[1] - h[0] * a[0];
        d[1] = 2 * (x[2] - x[0]);
        for (int i = 1; i < n_2; i++) {
            double t = h[i] / d[i];
            a[i + 1] = d[i + 2] - d[i + 1] - a[i] * t;
            d[i + 1] = 2 * (x[i + 2] - x[i]) - h[i] * t;
        }
        a[n_2] -= h[n_2] * a[n_1];
        for (int i = n_2; i > 0; i--) {
            a[i] = (a[i] - h[i] * a[i + 1]) / d[i];
        }
        return a;
    }

    public double f(double t) {
        int i = 0;
        int j = n - 1;
        while (i < j) {
            int k = (i + j) >> 1;
            if (x[k] < t) i = k + 1; else j = k;
        }
        if (i > 0) i--;
        double h = x[i + 1] - x[i];
        double d = t - x[i];
        return (((a[i + 1] - a[i]) * d / h + a[i] * 3) * d + ((y[i + 1] - y[i]) / h - (a[i] * 2 + a[i + 1]) * h)) * d + y[i];
    }
}
