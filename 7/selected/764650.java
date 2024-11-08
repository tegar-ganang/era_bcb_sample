package jmri.jmrix.rps;

import javax.vecmath.Point3d;

/**
 * Implementation of 2.1th algorithm for reducing Readings
 * <P>
 * This algorithm was provided by Robert Ashenfelter
 * based in part on the work of Ralph Bucher in his paper
 * "Exact Solution for Three Dimensional Hyperbolic Positioning  Algorithm and 
 * Synthesizable VHDL Model for Hardware Implementation".
 * <P>
 * Neither Ashenfelter nor Bucher provide any guarantee as to the
 * intellectual property status of this algorithm.  
 * Use it at your own risk.
 *
 * <p>
 * 
 * @author	Robert Ashenfelter  Copyright (C) 2007
 * @author	Bob Jacobsen  Copyright (C) 2007
 * @version	$Revision: 1.13 $
 */
public class Ash2_1Algorithm extends AbstractCalculator {

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    public Ash2_1Algorithm(Point3d[] sensors, double vsound, int offset) {
        this(sensors, vsound);
        this.offset = offset;
    }

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "EI_EXPOSE_REP2")
    public Ash2_1Algorithm(Point3d[] sensors, double vsound) {
        this.sensors = sensors;
        this.Vs = vsound;
    }

    public Ash2_1Algorithm(Point3d sensor1, Point3d sensor2, Point3d sensor3, double vsound) {
        this(null, vsound);
        sensors = new Point3d[3];
        sensors[0] = sensor1;
        sensors[1] = sensor2;
        sensors[2] = sensor3;
    }

    public Ash2_1Algorithm(Point3d sensor1, Point3d sensor2, Point3d sensor3, Point3d sensor4, double vsound) {
        this(null, vsound);
        sensors = new Point3d[4];
        sensors[0] = sensor1;
        sensors[1] = sensor2;
        sensors[2] = sensor3;
        sensors[3] = sensor4;
    }

    double Vs;

    double Xt = 0.0;

    double Yt = 0.0;

    double Zt = 0.0;

    public Measurement convert(Reading r) {
        if (log.isDebugEnabled()) {
            log.debug("Reading: " + r.toString());
            log.debug("Sensors: " + sensors.length);
            if (sensors.length >= 1 && sensors[0] != null) log.debug("Sensor[0]: " + sensors[0].x + "," + sensors[0].y + "," + sensors[0].z);
            if (sensors.length >= 2 && sensors[1] != null) log.debug("Sensor[1]: " + sensors[1].x + "," + sensors[1].y + "," + sensors[1].z);
        }
        prep(r);
        RetVal result = RPSpos(nr, Tr, Xr, Yr, Zr, Vs, Xt, Yt, Zt);
        Xt = result.x;
        Yt = result.y;
        Zt = result.z;
        Vs = result.vs;
        log.debug("x = " + Xt + " y = " + Yt + " z0 = " + Zt + " code = " + result.code);
        return new Measurement(r, Xt, Yt, Zt, Vs, result.code, "Ash2_1Algorithm");
    }

    /**
     * Seed the conversion using an estimated position
     */
    public Measurement convert(Reading r, Point3d guess) {
        this.Xt = guess.x;
        this.Yt = guess.y;
        this.Zt = guess.z;
        return convert(r);
    }

    /**
     * Seed the conversion using a last measurement
     */
    public Measurement convert(Reading r, Measurement last) {
        if (last != null) {
            this.Xt = last.getX();
            this.Yt = last.getY();
            this.Zt = last.getZ();
        }
        if (this.Xt > 9.E99) this.Xt = 0;
        if (this.Yt > 9.E99) this.Yt = 0;
        if (this.Zt > 9.E99) this.Zt = 0;
        return convert(r);
    }

    int offset = 0;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "MS_SHOULD_BE_FINAL")
    public static int TMAX = 35000;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "MS_SHOULD_BE_FINAL")
    public static int TMIN = 150;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "MS_SHOULD_BE_FINAL")
    public static int SMAX = 30;

    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "MS_SHOULD_BE_FINAL")
    public static int NMAX = 50;

    RetVal RPSpos(int nr, double Tr[], double Xr[], double Yr[], double Zr[], double Vs, double Xt, double Yt, double Zt) {
        int i, j, jmax, k, ns, nss, nxx, nox, tov, S, cmax;
        int[] ce = new int[NMAX];
        double Rq;
        double[] Rs = new double[NMAX];
        double[] Xs = new double[NMAX];
        double[] Ys = new double[NMAX];
        double[] Zs = new double[NMAX];
        double x, y, z, Rmax;
        double Ww, Xw, Yw, Zw, w, q;
        double err, emax, thr, var, vmax, vmin, vold;
        j = k = jmax = nox = 0;
        w = 0;
        var = 0;
        vmax = SMAX * SMAX * Vs * Vs;
        vmin = 1.0 * Vs * Vs;
        ns = 0;
        Rmax = Vs * TMAX;
        Rs[NMAX - 1] = TMAX;
        for (i = 0; i < nr; i++) {
            if (Tr[i] == 0.0) continue;
            Rq = Vs * (Tr[i] + offset);
            if ((Rq >= Rmax) || (Rq < Vs * TMIN)) continue;
            if (ns == 0) {
                Rs[0] = Rq;
                Xs[0] = Xr[i];
                Ys[0] = Yr[i];
                Zs[0] = Zr[i];
                ns = 1;
            } else {
                j = ((ns == NMAX) ? (ns - 1) : (ns++));
                for (; ; j--) {
                    if ((j > 0) && (Rq < Rs[j - 1])) {
                        Rs[j] = Rs[j - 1];
                        Xs[j] = Xs[j - 1];
                        Ys[j] = Ys[j - 1];
                        Zs[j] = Zs[j - 1];
                    } else {
                        if ((j < NMAX - 1) || (Rq < Rs[j])) {
                            Rs[j] = Rq;
                            Xs[j] = Xr[i];
                            Ys[j] = Yr[i];
                            Zs[j] = Zr[i];
                        }
                        break;
                    }
                }
            }
        }
        for (i = 0; i < ns; i++) ce[i] = 0;
        for (i = 0; i < ns - 1; i++) {
            for (j = i + 1; j < ns; j++) {
                q = Math.sqrt((Xs[i] - Xs[j]) * (Xs[i] - Xs[j]) + (Ys[i] - Ys[j]) * (Ys[i] - Ys[j]) + (Zs[i] - Zs[j]) * (Zs[i] - Zs[j]));
                if ((Rs[i] + Rs[j] < q) || (Rs[i] - Rs[j] > q) || (Rs[j] - Rs[i] > q)) {
                    ++ce[i];
                    ++ce[j];
                }
            }
        }
        cmax = 1;
        nxx = 0;
        while (cmax != 0) {
            cmax = 0;
            for (i = 0; i < ns; i++) {
                if (ce[i] >= cmax) {
                    if (ce[i] > 0) nxx = ((ce[i] == cmax) ? nxx + 1 : 1);
                    cmax = ce[i];
                    j = i;
                }
            }
            if (cmax > 0) {
                for (i = 0; i < ns; i++) {
                    if (i == j) continue;
                    q = Math.sqrt((Xs[i] - Xs[j]) * (Xs[i] - Xs[j]) + (Ys[i] - Ys[j]) * (Ys[i] - Ys[j]) + (Zs[i] - Zs[j]) * (Zs[i] - Zs[j]));
                    if ((Rs[i] + Rs[j] < q) || (Rs[i] - Rs[j] > q) || (Rs[j] - Rs[i] > q)) {
                        --ce[i];
                    }
                }
                for (i = j; i < ns - 1; i++) {
                    Rs[i] = Rs[i + 1];
                    Xs[i] = Xs[i + 1];
                    Ys[i] = Ys[i + 1];
                    Zs[i] = Zs[i + 1];
                    ce[i] = ce[i + 1];
                }
                --ns;
            }
        }
        nss = ns;
        if (ns < 3) {
            Xt = Yt = Zt = 9.9999999e99;
            return new RetVal(0, Xt, Yt, Zt, Vs);
        }
        S = i = tov = 0;
        x = y = 0.0;
        z = -100000.0;
        while (S < 4) {
            if (S == 0) {
                j = k = i % ns;
                w = 1.0;
            } else if (S == 1) {
                while ((j = (int) Math.floor((ns) * Math.random())) == k) {
                }
                k = j;
                w = 1.0;
            } else if (S == 2) {
                --k;
                j = k % ns;
                w = 1.0 - Rs[j] / Rmax;
                w = w * w;
                w *= 0.01 * (k + 1);
            } else if (S == 3) {
            }
            if (S < 3) {
                q = Math.sqrt((Xs[j] - x) * (Xs[j] - x) + (Ys[j] - y) * (Ys[j] - y) + (Zs[j] - z) * (Zs[j] - z));
                q = w * (1.0 - Rs[j] / q);
                x += q * (Xs[j] - x);
                y += q * (Ys[j] - y);
                z += q * (Zs[j] - z);
                ++i;
            }
            if (((S == 1) && (i % 50 == 0)) || ((S == 2) && (k == 0)) || (S == 3)) {
                Ww = Xw = Yw = Zw = emax = 0.0;
                vold = var;
                var = 0.0;
                for (j = 0; j < ns; j++) {
                    q = Math.sqrt((Xs[j] - x) * (Xs[j] - x) + (Ys[j] - y) * (Ys[j] - y) + (Zs[j] - z) * (Zs[j] - z));
                    err = q - Rs[j];
                    err = err * err;
                    q = 1.0 - Rs[j] / q;
                    if (S >= 2) {
                        w = 1.0 - Rs[j] / Rmax;
                        w = w * w;
                    } else w = 1.0;
                    Xw += w * (x + q * (Xs[j] - x));
                    Yw += w * (y + q * (Ys[j] - y));
                    Zw += w * (z + q * (Zs[j] - z));
                    Ww += w;
                    var += w * err;
                    if (w * err > emax) {
                        emax = w * err;
                        jmax = j;
                    }
                }
                x = Xw / Ww;
                y = Yw / Ww;
                z = Zw / Ww;
                var = var / Ww;
                i += ns;
                thr = (10.0 - 30.0 / ns) * Ww / ns;
                if ((S >= 2) && (ns > 3) && (((ns > 4) && (emax > var * thr)) || (var > 3 * vmax))) {
                    tov = ((emax > var * thr) ? 0 : 1);
                    --ns;
                    nox = 0;
                    Rs[jmax] = Rs[ns];
                    Xs[jmax] = Xs[ns];
                    Ys[jmax] = Ys[ns];
                    Zs[jmax] = Zs[ns];
                } else ++nox;
                if ((S == 1) && (((var > 0.999 * vold) && (var < 3 * vmax)) || (var < vmin) || (i >= 750))) {
                    k = 200;
                    nox = 0;
                    ++S;
                }
                if ((S == 2) && (k == 0)) {
                    k = 200;
                    if (((nox >= 2) && (var > 0.999 * vold)) || (var < vmin) || (i >= 2000)) {
                        nox = 0;
                        ++S;
                    }
                }
                if ((S == 3) && (((nox >= 1 + 110 / (ns + 5)) && (var > 0.999 * vold)) || (var < 0.1 * vmin) || (i >= 2500 - ns))) {
                    ++S;
                }
            }
            if ((S == 0) && (i >= 50)) {
                k = j;
                var = 9e9;
                ++S;
            }
        }
        Xt = x;
        Yt = y;
        Zt = z;
        if ((var > vmax) || ((ns == 3) && (var > vmin))) {
            return new RetVal(-ns, Xt, Yt, Zt, Vs);
        }
        if ((ns == 3) && ((nss > 4) || (nxx > 1) || (tov != 0))) {
            return new RetVal(1, Xt, Yt, Zt, Vs);
        }
        if ((ns == 4) && ((nss > 5) || ((nss == 5) && (nxx == 1) && (tov == 1)))) {
            return new RetVal(2, Xt, Yt, Zt, Vs);
        }
        if ((ns >= 5) && (nss > (3 * ns - 3) / 2)) return new RetVal(2, Xt, Yt, Zt, Vs);
        return new RetVal(ns, Xt, Yt, Zt, Vs);
    }

    /**
     * Internal class to handle return value.
     *
     * More of a struct, really
     */
    static class RetVal {

        RetVal(int code, double x, double y, double z, double vs) {
            this.code = code;
            this.x = x;
            this.y = y;
            this.z = z;
            this.vs = vs;
        }

        int code;

        @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "UUF_UNUSED_FIELD")
        double x, y, z, t, vs;
    }

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(Ash2_1Algorithm.class.getName());
}
