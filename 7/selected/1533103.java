package javacream.math;

/**
 * SplineInterpolator
 * 
 * @author Glenn Powell
 *
 */
public class SplineFloatInterpolator implements Interpolator {

    public static enum MODE {

        POLYNOMIAL, CUBIC
    }

    private MODE mode = MODE.POLYNOMIAL;

    public float interpolate(InterpolationCurve curve, float x) throws InterpolationException {
        int size = curve.getPointCount();
        if (size > 0) {
            if (size < 2) return curve.getY(0);
            float px[] = new float[size];
            float py[] = new float[size];
            for (int i = 0; i < size; ++i) {
                px[i] = curve.getX(i);
                py[i] = curve.getY(i);
            }
            switch(mode) {
                case POLYNOMIAL:
                    {
                        for (int i = 1; i <= size - 1; i++) {
                            for (int j = 0; j <= size - 1 - i; j++) {
                                py[j] = (py[j + 1] - py[j]) / (px[j + i] - px[j]);
                            }
                        }
                        float y = py[0];
                        for (int i = 1; i <= size - 1; i++) {
                            y = y * (x - px[i]) + py[i];
                        }
                        return y;
                    }
                case CUBIC:
                    {
                        float a[] = new float[size];
                        float dpx[] = new float[size];
                        for (int i = 1; i <= size - 1; i++) {
                            dpx[i] = px[i] - px[i - 1];
                        }
                        if (size > 2) {
                            float sub[] = new float[size - 1];
                            float diag[] = new float[size - 1];
                            float sup[] = new float[size - 1];
                            for (int i = 1; i <= size - 2; i++) {
                                diag[i] = (dpx[i] + dpx[i + 1]) / 3;
                                sup[i] = dpx[i + 1] / 6;
                                sub[i] = dpx[i] / 6;
                                a[i] = (py[i + 1] - py[i]) / dpx[i + 1] - (py[i] - py[i - 1]) / dpx[i];
                            }
                            solveTridiag(sub, diag, sup, a, size - 2);
                        }
                    }
            }
        }
        throw new InterpolationException();
    }

    private static void solveTridiag(float sub[], float diag[], float sup[], float b[], int n) {
        int i;
        for (i = 2; i <= n; i++) {
            sub[i] = sub[i] / diag[i - 1];
            diag[i] = diag[i] - sub[i] * sup[i - 1];
            b[i] = b[i] - sub[i] * b[i - 1];
        }
        b[n] = b[n] / diag[n];
        for (i = n - 1; i >= 1; i--) {
            b[i] = (b[i] - sup[i] * b[i + 1]) / diag[i];
        }
    }
}
