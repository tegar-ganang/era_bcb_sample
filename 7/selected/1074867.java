package com.dukesoftware.utils.interpolation;

import com.dukesoftware.utils.common.ArrayUtils;
import com.dukesoftware.utils.data.Data2D;

public class Interpolation {

    public static void main(String[] args) {
        double[][] d = new double[2][1000];
        int len = d[0].length;
        for (int i = 0; i < len; i++) {
            d[0][i] = i;
            d[1][i] = i * 2;
        }
        System.out.println(neville(d, 25));
    }

    public static final double neville(double[] x, double[] y, double t) {
        double[] w = ArrayUtils.copy(y);
        for (int i = 0; i < x.length; i++) {
            for (int j = i - 1; j >= 0; j--) {
                w[j] = w[j + 1] + (w[j + 1] - w[j]) * (t - x[i]) / (x[i] - x[j]);
            }
        }
        return w[0];
    }

    public static final double neville(Data2D d, double t) {
        return neville(d.x, d.y, t);
    }

    public static final double neville(double[][] d, double t) {
        return neville(d[0], d[1], t);
    }

    public static final double aitken(double[] x, double[] y, double t) {
        double[] w = ArrayUtils.copy(y);
        for (int i = 1; i < x.length; i++) {
            for (int j = x.length - 1; j > i; j--) {
                w[j] = ((w[j - 1] * x[j] - t) - w[j] * (x[j - i] - t)) / (x[j] - x[j - i]);
            }
        }
        return w[x.length - 1];
    }
}
