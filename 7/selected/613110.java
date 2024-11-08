package hypercast.DT;

import hypercast.HyperCastFatalRuntimeException;
import java.lang.*;
import java.util.*;
import java.io.*;

/** This class defines the functions for calculating the logical
 * coordinates with the given measurement results.
 * 
 * @author HyperCast Team
 */
class GNP_Optimization {

    private int numLandmarks;

    private int dimensions;

    private float[][] coordinates;

    private float[][] lmDelays;

    private float[] targetCoors;

    private float[] targetDelays;

    private boolean[] validDelays;

    static final int mytry = 3;

    static final int range = 500;

    static final int lambda = 2000;

    static final int mytry2 = 3;

    static final int restarts = 1;

    static final float ftol = 0.00001f;

    static final float MAX_ERR = 1e20f;

    static final float TINY = 1.0e-10f;

    static final int NMAX = 500000;

    static final int NR_END = 1;

    private boolean randomInit = false;

    /**
	 * Constructor.
	 * 
	 * @param delay		Array storing the delay measurement result.
	 * @param dim		The dimension of the output array.
	 */
    GNP_Optimization(float[][] delay, int dim) {
        dimensions = dim;
        lmDelays = delay;
        numLandmarks = lmDelays.length;
        coordinates = new float[numLandmarks][dim];
        targetCoors = new float[dimensions];
        System.out.println("dimension:" + dim + "  " + "probes:" + numLandmarks);
        fitModel();
    }

    /**
   * Constructor.
   * 
   * @param lmCoor	Array storing the information of landmark nodes.
   */
    GNP_Optimization(float[][] lmCoor) {
        numLandmarks = lmCoor.length;
        dimensions = lmCoor[0].length;
        lmDelays = null;
        coordinates = lmCoor;
        targetCoors = new float[dimensions];
        System.out.println("dimension:" + dimensions + "  " + "probes:" + numLandmarks);
    }

    /** Set random initialization flag. */
    void randomInitiation() {
        randomInit = true;
    }

    /** Unset random initialization flag. */
    void fixInitiation() {
        randomInit = false;
    }

    float[][] getLMCoordinates() {
        return coordinates;
    }

    private void fitModel() {
        float[] model = new float[numLandmarks * dimensions];
        System.out.println("solve the model coordinates");
        float error = solve(model, true, mytry);
        if (error >= MAX_ERR) {
            System.out.println("error too large");
        } else {
        }
        for (int i = 0; i < numLandmarks; i++) {
            for (int j = 0; j < dimensions; j++) {
                coordinates[i][j] = model[i * dimensions + j];
            }
        }
    }

    /**
   * 
   * fit targets only with landmarks indexed.
   */
    float[] fitTargetData(float[] tDelays, boolean[] index) {
        if (tDelays.length != numLandmarks) {
            System.out.println(" measurement dimenstion does not match");
            return null;
        }
        targetDelays = tDelays;
        validDelays = index;
        float[] xy = new float[dimensions];
        float error = solve(xy, false, mytry2);
        if (error >= MAX_ERR) {
            System.out.println("error too large");
        } else {
        }
        for (int i = 0; i < dimensions; i++) targetCoors[i] = xy[i];
        return targetCoors;
    }

    float solve(float[] solution, boolean lmObjective, int mytry) {
        float min, fit, localftol;
        float[] myxy;
        float[][] p;
        float[] y;
        int i, j, restarted, num;
        num = solution.length;
        localftol = ftol;
        myxy = new float[num + 1];
        p = new float[num + 1 + 1][];
        p[1] = myxy;
        for (i = 2; i <= num + 1; i++) {
            p[i] = new float[num + 1];
        }
        y = new float[num + 1 + 1];
        while (true) {
            min = MAX_ERR;
            for (i = 0; i < mytry; i++) {
                restarted = 0;
                for (j = 1; j <= num; j++) {
                    if (randomInit) myxy[j] = (float) (Math.random() * range) - (range / 2); else myxy[j] = 0;
                }
                boolean restart;
                restart = true;
                while (restart) {
                    float[] tmpstore = copyOne(myxy);
                    y[1] = funObjective(tmpstore, lmObjective);
                    tmpstore = null;
                    for (j = 2; j <= num + 1; j++) {
                        for (int tmpc = 0; tmpc <= num; tmpc++) p[j][tmpc] = myxy[tmpc];
                        p[j][j - 1] += lambda;
                        tmpstore = copyOne(p[j]);
                        y[j] = funObjective(tmpstore, lmObjective);
                        tmpstore = null;
                    }
                    amoeba(p, y, num, localftol, lmObjective, j);
                    if (j < 0) {
                        System.out.println("No answer");
                        restart = false;
                        break;
                    }
                    if (restarted < restarts) {
                        restarted++;
                        continue;
                    } else {
                        break;
                    }
                }
                fit = funObjective(copyOne(myxy), lmObjective);
                if (restart) {
                    float[] tmpstore = copyOne(myxy);
                    fit = funObjective(tmpstore, lmObjective);
                    if (fit < min) {
                        min = fit;
                        for (j = 0; j < num; j++) {
                            solution[j] = myxy[j + 1];
                        }
                    }
                }
            }
            if (min == MAX_ERR) {
                localftol = localftol * 10;
                continue;
            } else {
                break;
            }
        }
        y = null;
        myxy = null;
        for (i = 2; i <= num + 1; i++) {
            p[i] = null;
        }
        p = null;
        myxy = null;
        return min;
    }

    private float[] vector(int nl, int nh) {
        float[] v;
        v = new float[nh - nl + 1 + NR_END];
        return v;
    }

    private void free_vector(float[] v, int nl, int nh) {
        v = null;
    }

    float host_dist(float[] x, float[] y) {
        float dist = 0;
        for (int i = 0; i < x.length; i++) {
            dist += Math.pow(Math.abs(x[i] - y[i]), 2);
        }
        return (float) Math.pow(dist, 1.0 / 2.0);
    }

    float host_dist(float[] host, int lmIndex) {
        float dist = 0;
        for (int i = 0; i < host.length; i++) {
            dist += Math.pow(Math.abs(host[i] - coordinates[lmIndex][i]), 2);
        }
        return (float) Math.pow(dist, 1.0 / 2.0);
    }

    private float funObjective(float[] array, boolean lmObjective) {
        if (lmObjective) return landmarkObjective(array); else return targetObjective(array);
    }

    private float landmarkObjective(float[] array) {
        float dist, sum, error;
        int i, j;
        if (array.length != dimensions * numLandmarks) {
            throw new HyperCastFatalRuntimeException(" land mark objective evaluation error");
        }
        sum = 0;
        for (i = 0; i < numLandmarks; i++) {
            for (j = 0; j < i; j++) {
                float[] lmx = new float[dimensions];
                float[] lmy = new float[dimensions];
                for (int k = 0; k < dimensions; k++) {
                    lmx[k] = array[i * dimensions + k];
                    lmy[k] = array[j * dimensions + k];
                }
                dist = host_dist(lmx, lmy);
                error = (lmDelays[i][j] - dist) / lmDelays[i][j];
                sum += error * error;
            }
        }
        return sum;
    }

    float targetObjective(float[] array) {
        float dist, sum, error;
        int i;
        if (array.length != dimensions) {
            throw new HyperCastFatalRuntimeException(" target objective evaluation error");
        }
        sum = 0;
        for (i = 0; i < numLandmarks && validDelays[i]; i++) {
            dist = host_dist(array, i);
            error = (targetDelays[i] - dist) / targetDelays[i];
            sum += error * error;
        }
        return sum;
    }

    private float amotry(float[][] p, float[] y, float[] psum, int ndim, boolean lmObjective, int ihi, float fac) {
        int j;
        float fac1, fac2, ytry;
        float[] ptry;
        ptry = vector(1, ndim);
        fac1 = (1.0f - fac) / ndim;
        fac2 = fac1 - fac;
        for (j = 1; j <= ndim; j++) ptry[j] = psum[j] * fac1 - p[ihi][j] * fac2;
        float[] tmpstore = copyOne(ptry);
        ytry = funObjective(tmpstore, lmObjective);
        tmpstore = null;
        if (ytry < y[ihi]) {
            y[ihi] = ytry;
            for (j = 1; j <= ndim; j++) {
                psum[j] += ptry[j] - p[ihi][j];
                p[ihi][j] = ptry[j];
            }
        }
        free_vector(ptry, 1, ndim);
        return ytry;
    }

    void amoeba(float[][] p, float[] y, int ndim, float ftol, boolean lmObjective, int nfunk) {
        int i, ihi, ilo, inhi, j, mpts = ndim + 1;
        float rtol, sum, swap, ysave, ytry;
        float[] psum;
        psum = vector(1, ndim);
        nfunk = 0;
        for (j = 1; j <= ndim; j++) {
            for (sum = 0.0f, i = 1; i <= mpts; i++) sum += p[i][j];
            psum[j] = sum;
        }
        for (; ; ) {
            ilo = 1;
            if (y[1] > y[2]) {
                ihi = 1;
                inhi = 2;
            } else {
                ihi = 2;
                inhi = 1;
            }
            for (i = 1; i <= mpts; i++) {
                if (y[i] <= y[ilo]) ilo = i;
                if (y[i] > y[ihi]) {
                    inhi = ihi;
                    ihi = i;
                } else if (y[i] > y[inhi] && i != ihi) inhi = i;
            }
            rtol = (float) (2.0 * Math.abs(y[ihi] - y[ilo]) / (Math.abs(y[ihi]) + Math.abs(y[ilo]) + TINY));
            if (rtol < ftol || y[ilo] < 1.0e-6) {
                SWAP(y[1], y[ilo]);
                for (i = 1; i <= ndim; i++) {
                    SWAP(p[1][i], p[ilo][i]);
                }
                break;
            }
            if (nfunk >= NMAX) {
                SWAP(y[1], y[ilo]);
                for (i = 1; i <= ndim; i++) {
                    SWAP(p[1][i], p[ilo][i]);
                }
                break;
            }
            nfunk += 2;
            ytry = amotry(p, y, psum, ndim, lmObjective, ihi, -1.0f);
            if (ytry <= y[ilo]) ytry = amotry(p, y, psum, ndim, lmObjective, ihi, 2.0f); else if (ytry >= y[inhi]) {
                ysave = y[ihi];
                ytry = amotry(p, y, psum, ndim, lmObjective, ihi, 0.5f);
                if (ytry >= ysave) {
                    for (i = 1; i <= mpts; i++) {
                        if (i != ilo) {
                            for (j = 1; j <= ndim; j++) p[i][j] = psum[j] = 0.5f * (p[i][j] + p[ilo][j]);
                            float[] tmpstore = copyOne(psum);
                            y[i] = funObjective(tmpstore, lmObjective);
                            tmpstore = null;
                        }
                    }
                    nfunk += ndim;
                    for (j = 1; j <= ndim; j++) {
                        for (sum = 0.0f, i = 1; i <= mpts; i++) sum += p[i][j];
                        psum[j] = sum;
                    }
                }
            } else --(nfunk);
        }
        free_vector(psum, 1, ndim);
    }

    private float[] copyOne(float src[]) {
        float[] tmpstore = new float[src.length - 1];
        for (int tmpc = 0; tmpc < src.length - 1; tmpc++) tmpstore[tmpc] = src[tmpc + 1];
        return tmpstore;
    }

    float tmp;

    private void SWAP(float a, float b) {
        tmp = a;
        a = b;
        b = tmp;
    }

    public static void main(String argv[]) throws Exception {
    }
}
