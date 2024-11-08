package jugglinglab.curve;

import jugglinglab.util.*;

public class splineCurve extends Curve {

    protected int n;

    protected double[][] a, b, c, d;

    protected double[] durations;

    public void initCurve(String st) {
    }

    public void calcCurve() throws JuggleExceptionInternal {
        int i, j;
        boolean edgeVelocitiesKnown = ((start_velocity != null) && (end_velocity != null));
        this.n = numpoints - 1;
        if (n < 1) throw new JuggleExceptionInternal("splineCurve error 1");
        this.a = new double[n][3];
        this.b = new double[n][3];
        this.c = new double[n][3];
        this.d = new double[n][3];
        this.durations = new double[n];
        for (i = 0; i < n; i++) {
            durations[i] = times[i + 1] - times[i];
            if (durations[i] < 0.0) throw new JuggleExceptionInternal("splineCurve error 2");
        }
        double[] x = new double[n + 1];
        double[] v = new double[n + 1];
        double t;
        for (i = 0; i < 3; i++) {
            for (j = 0; j < (n + 1); j++) x[j] = positions[j].getIndex(i);
            if (edgeVelocitiesKnown) {
                v[0] = start_velocity.getIndex(i);
                v[n] = end_velocity.getIndex(i);
                findvels_edges_known(v, x, durations, n, jugglinglab.core.Constants.SPLINE_LAYOUT_METHOD);
            } else {
                findvels_edges_unknown(v, x, durations, n, jugglinglab.core.Constants.SPLINE_LAYOUT_METHOD);
            }
            for (j = 0; j < n; j++) {
                a[j][i] = x[j];
                b[j][i] = v[j];
                t = durations[j];
                c[j][i] = (3.0 * (x[j + 1] - x[j]) - (v[j + 1] + 2.0 * v[j]) * t) / (t * t);
                d[j][i] = (-2.0 * (x[j + 1] - x[j]) + (v[j + 1] + v[j]) * t) / (t * t * t);
            }
        }
    }

    public static final int rmsaccel = 0;

    public static final int continaccel = 1;

    public static final int rmsvel = 2;

    protected static void findvels_edges_known(double[] v, double[] x, double[] t, int n, int method) throws JuggleExceptionInternal {
        if (n < 2) return;
        double[] Adiag = new double[n - 1];
        double[] Aoffd = new double[n - 1];
        double[] b = new double[n - 1];
        for (int i = 0; i < n - 1; i++) {
            switch(method) {
                case rmsaccel:
                case continaccel:
                    Adiag[i] = 2.0 / t[i + 1] + 2.0 / t[i];
                    Aoffd[i] = 1.0 / t[i + 1];
                    b[i] = 3.0 * (x[i + 2] - x[i + 1]) / (t[i + 1] * t[i + 1]) + 3.0 * (x[i + 1] - x[i]) / (t[i] * t[i]);
                    if (i == 0) b[0] -= v[0] / t[0];
                    if (i == (n - 2)) b[n - 2] -= v[n] / t[n - 1];
                    break;
                case rmsvel:
                    Adiag[i] = 4.0 * (t[i] + t[i + 1]);
                    Aoffd[i] = -t[i + 1];
                    b[i] = 3.0 * (x[i + 2] - x[i]);
                    if (i == 0) b[0] += v[0] * t[0];
                    if (i == (n - 2)) b[n - 2] += v[n] * t[n - 1];
                    break;
            }
        }
        double[] vtemp = new double[n - 1];
        tridag(Aoffd, Adiag, Aoffd, b, vtemp, n - 1);
        for (int i = 0; i < n - 1; i++) v[i + 1] = vtemp[i];
    }

    protected static void findvels_edges_unknown(double[] v, double[] x, double[] t, int n, int method) throws JuggleExceptionInternal {
        if (n < 2) return;
        double[] Adiag = new double[n];
        double[] Aoffd = new double[n];
        double Acorner = 0.0;
        double[] b = new double[n];
        for (int i = 0; i < n; i++) {
            switch(method) {
                case rmsaccel:
                case continaccel:
                    if (i == 0) {
                        Adiag[0] = 2.0 / t[n - 1] + 2.0 / t[0];
                        Acorner = 1.0 / t[n - 1];
                        b[0] = 3.0 * (x[1] - x[0]) / (t[0] * t[0]) + 3.0 * (x[n] - x[n - 1]) / (t[n - 1] * t[n - 1]);
                    } else {
                        Adiag[i] = 2.0 / t[i - 1] + 2.0 / t[i];
                        b[i] = 3.0 * (x[i + 1] - x[i]) / (t[i] * t[i]) + 3.0 * (x[i] - x[i - 1]) / (t[i - 1] * t[i - 1]);
                    }
                    Aoffd[i] = 1.0 / t[i];
                    break;
                case rmsvel:
                    if (i == 0) {
                        Adiag[0] = 4.0 * (t[n - 1] + t[0]);
                        Acorner = -t[n - 1];
                        b[0] = 3.0 * (x[n] - x[n - 1] + x[1] - x[0]);
                    } else {
                        Adiag[i] = 4.0 * (t[i - 1] + t[i]);
                        b[i] = 3.0 * (x[i + 1] - x[i - 1]);
                    }
                    Aoffd[i] = -t[i];
                    break;
            }
        }
        tridag(Aoffd, Adiag, Aoffd, b, v, n);
        if (n > 2) {
            double[] z1 = new double[n];
            b[0] = Acorner;
            for (int i = 1; i < n; i++) b[i] = 0.0;
            tridag(Aoffd, Adiag, Aoffd, b, z1, n);
            double[] z2 = new double[n];
            b[n - 1] = Acorner;
            for (int i = 0; i < n - 1; i++) b[i] = 0.0;
            tridag(Aoffd, Adiag, Aoffd, b, z2, n);
            double H00, H01, H10, H11;
            H00 = 1.0 + z2[0];
            H01 = -z2[n - 1];
            H10 = -z1[0];
            H11 = 1.0 + z1[n - 1];
            double det = H00 * H11 - H01 * H10;
            H00 /= det;
            H01 /= det;
            H10 /= det;
            H11 /= det;
            double m0 = H00 * v[n - 1] + H01 * v[0];
            double m1 = H10 * v[n - 1] + H11 * v[0];
            for (int i = 0; i < n; i++) v[i] -= (z1[i] * m0 + z2[i] * m1);
        }
        v[n] = v[0];
    }

    protected static void tridag(double[] a, double[] b, double[] c, double[] r, double[] u, int n) throws JuggleExceptionInternal {
        int j;
        double bet;
        double[] gam = new double[n];
        if (b[0] == 0.0) throw new JuggleExceptionInternal("Error 1 in TRIDAG");
        bet = b[0];
        u[0] = r[0] / bet;
        for (j = 1; j < n; j++) {
            gam[j] = c[j - 1] / bet;
            bet = b[j] - a[j - 1] * gam[j];
            if (bet == 0.0) throw new JuggleExceptionInternal("Error 2 in TRIDAG");
            u[j] = (r[j] - a[j - 1] * u[j - 1]) / bet;
        }
        for (j = (n - 1); j > 0; j--) u[j - 1] -= gam[j] * u[j];
    }

    public void getCoordinate(double time, Coordinate newPosition) {
        if ((time < times[0]) || (time > times[n])) return;
        int i;
        for (i = 0; i < n; i++) if (time <= times[i + 1]) break;
        if (i == n) i = n - 1;
        time -= times[i];
        newPosition.setCoordinate(a[i][0] + time * (b[i][0] + time * (c[i][0] + time * d[i][0])), a[i][1] + time * (b[i][1] + time * (c[i][1] + time * d[i][1])), a[i][2] + time * (b[i][2] + time * (c[i][2] + time * d[i][2])));
    }

    protected Coordinate getMax2(double begin, double end) {
        if ((end < times[0]) || (begin > times[n])) return null;
        Coordinate result = null;
        double tlow = Math.max(times[0], begin);
        double thigh = Math.min(times[n], end);
        result = check(result, tlow, true);
        result = check(result, thigh, true);
        for (int i = 0; i <= n; i++) {
            if ((tlow <= times[i]) && (times[i] <= thigh)) result = check(result, times[i], true);
            if (i != n) {
                double tlowtemp = Math.max(tlow, times[i]);
                double thightemp = Math.min(thigh, times[i + 1]);
                if (tlowtemp < thightemp) {
                    result = check(result, tlowtemp, true);
                    result = check(result, thightemp, true);
                    for (int index = 0; index < 3; index++) {
                        if (Math.abs(d[i][index]) > 1.0e-6) {
                            double k = c[i][index] * c[i][index] - 3.0 * b[i][index] * d[i][index];
                            if (k > 0.0) {
                                double te = times[i] + (-c[i][index] - Math.sqrt(k)) / (3 * d[i][index]);
                                if ((tlowtemp < te) && (te < thightemp)) result = check(result, te, true);
                            }
                        } else if (c[i][index] < 0.0) {
                            double te = -b[i][index] / (2.0 * c[i][index]);
                            te += times[i];
                            if ((tlowtemp < te) && (te < thightemp)) result = check(result, te, true);
                        }
                    }
                }
            }
        }
        return result;
    }

    protected Coordinate getMin2(double begin, double end) {
        if ((end < times[0]) || (begin > times[n])) return null;
        Coordinate result = null;
        double tlow = Math.max(times[0], begin);
        double thigh = Math.min(times[n], end);
        result = check(result, tlow, false);
        result = check(result, thigh, false);
        for (int i = 0; i <= n; i++) {
            if ((tlow <= times[i]) && (times[i] <= thigh)) result = check(result, times[i], false);
            if (i != n) {
                double tlowtemp = Math.max(tlow, times[i]);
                double thightemp = Math.min(thigh, times[i + 1]);
                if (tlowtemp < thightemp) {
                    result = check(result, tlowtemp, false);
                    result = check(result, thightemp, false);
                    for (int index = 0; index < 3; index++) {
                        if (Math.abs(d[i][index]) > 1.0e-6) {
                            double k = c[i][index] * c[i][index] - 3.0 * b[i][index] * d[i][index];
                            if (k > 0.0) {
                                double te = times[i] + (-c[i][index] + Math.sqrt(k)) / (3 * d[i][index]);
                                if ((tlowtemp < te) && (te < thightemp)) result = check(result, te, false);
                            }
                        } else if (c[i][index] > 0.0) {
                            double te = -b[i][index] / (2.0 * c[i][index]);
                            te += times[i];
                            if ((tlowtemp < te) && (te < thightemp)) result = check(result, te, false);
                        }
                    }
                }
            }
        }
        return result;
    }
}
