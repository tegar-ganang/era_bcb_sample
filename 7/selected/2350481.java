package com.dukesoftware.utils.interpolation;

import com.dukesoftware.utils.common.ArrayUtils;
import com.dukesoftware.utils.math.function.IFunction;

public class Neville implements IFunction {

    private final double[] x, y;

    public Neville(double[] x, double[] y) {
        this.x = x;
        this.y = y;
    }

    public double f(double t) {
        double[] w = ArrayUtils.copy(y);
        for (int i = 0; i < y.length; i++) {
            for (int j = i - 1; j >= 0; j--) w[j] = w[j + 1] + (w[j + 1] - w[j]) * (t - x[i]) / (x[i] - x[j]);
        }
        return w[0];
    }
}
