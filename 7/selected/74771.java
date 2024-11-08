package gov.nasa.gsfc.visbard.model;

import gov.nasa.gsfc.visbard.model.threadtask.ThreadTask;
import java.io.*;
import java.util.*;
import java.math.*;

public class MhdSearch extends ThreadTask {

    static float[][][] vx, vy, vz;

    static int nx, ny, nz;

    static int imin, imax, jmin, jmax, kmin, kmax;

    static float xres, yres, zres, xmin, ymin, zmin, time;

    static float xres2, yres2, zres2;

    static double[][] B, Binv;

    static double[] omega, e1, e2, e3, cross;

    static int nn;

    static int numv;

    static int maxv;

    static int numc;

    static int classthreshold;

    static int vclassptr[];

    static double psi[][], vBx[][][], vBy[][][], vBz[][][], speed[][];

    static int[] vorticesi, vorticesj, vorticesk;

    static double[] vorticesx, vorticesy, vorticesz;

    static int[] vclass;

    static int[] newvclass, labeled, vclassctr;

    static double ywindow;

    static double maxvdist;

    static boolean debug;

    static int nqplus = 0, npts = 0, npclosed = 0, nspeed = 0;

    MhdSearch() {
        super("Performing MHD Vortex Search", true);
        maxv = 1000;
        nn = 2;
        ywindow = 5;
        maxvdist = 3;
        classthreshold = 1;
        numv = 0;
        numc = 0;
        B = new double[3][3];
        Binv = new double[3][3];
        omega = new double[3];
        e1 = new double[3];
        e2 = new double[3];
        e3 = new double[3];
        cross = new double[3];
        psi = new double[2 * nn + 1][2 * nn + 1];
        speed = new double[2 * nn + 1][2 * nn + 1];
        vBx = new double[2 * nn + 1][2 * nn + 1][2 * nn + 1];
        vBy = new double[2 * nn + 1][2 * nn + 1][2 * nn + 1];
        vBz = new double[2 * nn + 1][2 * nn + 1][2 * nn + 1];
        vorticesi = new int[maxv];
        vorticesj = new int[maxv];
        vorticesk = new int[maxv];
        vorticesx = new double[maxv];
        vorticesy = new double[maxv];
        vorticesz = new double[maxv];
    }

    static double getgradv(int iv, int jv, int kv) {
        double dudx, dudy, dudz, dvdx, dvdy, dvdz, dwdx, dwdy, dwdz;
        double qval;
        dudx = ((double) vx[kv][jv][iv + 1] - (double) vx[kv][jv][iv - 1]) / xres2;
        dudy = ((double) vx[kv][jv + 1][iv] - (double) vx[kv][jv - 1][iv]) / yres2;
        dudz = ((double) vx[kv + 1][jv][iv] - (double) vx[kv - 1][jv][iv]) / zres2;
        dvdx = ((double) vy[kv][jv][iv + 1] - (double) vy[kv][jv][iv - 1]) / xres2;
        dvdy = ((double) vy[kv][jv + 1][iv] - (double) vy[kv][jv - 1][iv]) / yres2;
        dvdz = ((double) vy[kv + 1][jv][iv] - (double) vy[kv - 1][jv][iv]) / zres2;
        dwdx = ((double) vz[kv][jv][iv + 1] - (double) vz[kv][jv][iv - 1]) / xres2;
        dwdy = ((double) vz[kv][jv + 1][iv] - (double) vz[kv][jv - 1][iv]) / yres2;
        dwdz = ((double) vz[kv + 1][jv][iv] - (double) vz[kv - 1][jv][iv]) / zres2;
        qval = .5 * (-dudx * dudx - 2 * dudy * dvdx - dvdy * dvdy - 2 * dudz * dwdx - 2 * dvdz * dwdy - dwdz * dwdz);
        return (qval);
    }

    static void getvorticity(int i, int j, int k) {
        omega[0] = ((double) vz[k][j + 1][i] - (double) vz[k][j - 1][i]) / (double) yres2 - ((double) vy[k + 1][j][i] - (double) vy[k - 1][j][i]) / (double) zres2;
        omega[1] = ((double) vx[k + 1][j][i] - (double) vx[k - 1][j][i]) / (double) zres2 - ((double) vz[k][j][i + 1] - (double) vz[k][j][i - 1]) / (double) xres2;
        omega[2] = ((double) vy[k][j][i + 1] - (double) vy[k][j][i - 1]) / (double) xres2 - ((double) vx[k][j + 1][i] - (double) vx[k][j - 1][i]) / (double) yres2;
    }

    static void crossproduct(double[] uval, double[] vval) {
        double uvalx, uvaly, uvalz, vvalx, vvaly, vvalz;
        uvalx = uval[0];
        uvaly = uval[1];
        uvalz = uval[2];
        vvalx = vval[0];
        vvaly = vval[1];
        vvalz = vval[2];
        cross[0] = uvaly * vvalz - uvalz * vvaly;
        cross[1] = uvalz * vvalx - uvalx * vvalz;
        cross[2] = uvalx * vvaly - uvaly * vvalx;
    }

    static double normalize(double[] val) {
        double valx, valy, valz;
        double epsilon, magnitude;
        epsilon = .0000001;
        valx = val[0];
        valy = val[1];
        valz = val[2];
        magnitude = Math.sqrt(valx * valx + valy * valy + valz * valz);
        if (magnitude < epsilon) magnitude = 1;
        return (magnitude);
    }

    static void getBasis() {
        double mag, epsilon;
        int i, j;
        epsilon = .000001;
        e3[0] = omega[0];
        e3[1] = omega[1];
        e3[2] = omega[2];
        mag = normalize(e3);
        e3[0] /= mag;
        e3[1] /= mag;
        e3[2] /= mag;
        e1[0] = -e3[1];
        e1[1] = e3[0];
        e1[2] = 0;
        if ((Math.abs(e1[0]) < epsilon) && (Math.abs(e1[1]) < epsilon)) e1[0] = e3[2];
        mag = normalize(e1);
        e1[0] /= mag;
        e1[1] /= mag;
        e1[2] /= mag;
        crossproduct(e3, e1);
        mag = normalize(cross);
        e2[0] = cross[0] / mag;
        e2[1] = cross[1] / mag;
        e2[2] = cross[2] / mag;
        for (i = 0; i < 3; i++) {
            B[i][0] = e1[i];
            B[i][1] = e2[i];
            B[i][2] = e3[i];
        }
        Binv[0][0] = B[0][0];
        Binv[0][1] = B[1][0];
        Binv[0][2] = B[2][0];
        Binv[1][0] = B[0][1];
        Binv[1][1] = B[1][1];
        Binv[1][2] = B[2][1];
        Binv[2][0] = B[0][2];
        Binv[2][1] = B[1][2];
        Binv[2][2] = B[2][2];
    }

    private double linearval(double dw, double wmin, double wmax) {
        return (dw * wmax + (1 - dw) * wmin);
    }

    private double intermediatevalue(double oldi, double oldj, double oldk, float[][][] v) {
        int imin, jmin, kmin;
        double iminjminkmin, iminjminkmax, iminjmaxkmin, iminjmaxkmax;
        double imaxjminkmin, imaxjminkmax, imaxjmaxkmin, imaxjmaxkmax;
        double jminkmin, jminkmax, jmaxkmin, jmaxkmax;
        double kkmin, kkmax, newval;
        imin = (int) Math.floor(oldi);
        jmin = (int) Math.floor(oldj);
        kmin = (int) Math.floor(oldk);
        try {
            iminjminkmin = (double) v[kmin][jmin][imin];
            iminjminkmax = (double) v[kmin + 1][jmin][imin];
            iminjmaxkmin = (double) v[kmin][jmin + 1][imin];
            iminjmaxkmax = (double) v[kmin + 1][jmin + 1][imin];
            imaxjminkmin = (double) v[kmin][jmin][imin + 1];
            imaxjminkmax = (double) v[kmin + 1][jmin][imin + 1];
            imaxjmaxkmin = (double) v[kmin][jmin + 1][imin + 1];
            imaxjmaxkmax = (double) v[kmin + 1][jmin + 1][imin + 1];
            jminkmin = linearval(oldi - imin, iminjminkmin, imaxjminkmin);
            jminkmax = linearval(oldi - imin, iminjminkmax, imaxjminkmax);
            jmaxkmin = linearval(oldi - imin, iminjmaxkmin, imaxjmaxkmin);
            jmaxkmax = linearval(oldi - imin, iminjmaxkmax, imaxjmaxkmax);
            kkmin = linearval(oldj - jmin, jminkmin, jmaxkmin);
            kkmax = linearval(oldj - jmin, jminkmax, jmaxkmax);
            newval = linearval(oldk - kmin, kkmin, kkmax);
        } catch (Exception e) {
            newval = -222222;
        }
        return (newval);
    }

    private boolean projectB(int iv, int jv, int kv) {
        int i, j, k, kstart, kend, jstart, jend, istart, iend;
        double oldi, oldj, oldk, oldvx, oldvy, oldvz;
        kstart = kv - nn;
        kend = kv + nn;
        jstart = jv - nn;
        jend = jv + nn;
        istart = iv - nn;
        iend = iv + nn;
        if ((kstart < 0) || (kend >= nz)) return (false);
        if ((jstart < 0) || (jend >= ny)) return (false);
        if ((istart < 0) || (iend >= nx)) return (false);
        for (k = -nn; k <= nn; k++) {
            for (j = -nn; j <= nn; j++) {
                for (i = -nn; i <= nn; i++) {
                    oldi = B[0][0] * (i) + B[0][1] * (j) + B[0][2] * (k) + iv;
                    if (oldi < 0 || oldi >= nx) return (false);
                    oldj = B[1][0] * (i) + B[1][1] * (j) + B[1][2] * (k) + jv;
                    if (oldj < 0 || oldj >= ny) return (false);
                    oldk = B[2][0] * (i) + B[2][1] * (j) + B[2][2] * (k) + kv;
                    if (oldk < 0 || oldk >= (double) nz) return (false);
                    oldvx = intermediatevalue(oldi, oldj, oldk, vx);
                    if (oldvx == -222222) return (false);
                    oldvy = intermediatevalue(oldi, oldj, oldk, vy);
                    if (oldvy == -222222) return (false);
                    oldvz = intermediatevalue(oldi, oldj, oldk, vz);
                    if (oldvz == -222222) return (false);
                    vBx[k + nn][j + nn][i + nn] = Binv[0][0] * oldvx + Binv[0][1] * oldvy + Binv[0][2] * oldvz;
                    vBy[k + nn][j + nn][i + nn] = Binv[1][0] * oldvx + Binv[1][1] * oldvy + Binv[1][2] * oldvz;
                    vBz[k + nn][j + nn][i + nn] = Binv[2][0] * oldvx + Binv[2][1] * oldvy + Binv[2][2] * oldvz;
                }
            }
        }
        return (true);
    }

    private double i2x(int i) {
        double x;
        x = xmin + (double) i * xres;
        return (x);
    }

    private double j2y(int j) {
        double y;
        y = ymin + (double) j * yres;
        return (y);
    }

    private double k2z(int k) {
        double z;
        z = zmin + (double) k * zres;
        return (z);
    }

    private boolean getPsiP(int iv, int jv, int kv) {
        int i, j;
        double x, y;
        if (iv < nn || iv > (nx - nn + 1)) return (false);
        if (jv < nn || jv > (ny - nn + 1)) return (false);
        x = i2x(iv);
        y = j2y(jv);
        if ((i2x(iv) < 0) && (Math.abs(j2y(jv)) < ywindow)) return (false);
        for (j = -nn; j <= nn; j++) {
            for (i = -nn; i <= nn; i++) {
                if (Math.abs(vBx[nn][j + nn][i + nn]) > 1000000.0) return (false);
                if (Math.abs(vBy[nn][j + nn][i + nn]) > 1000000.0) return (false);
            }
        }
        psi[nn][nn] = 0;
        for (j = nn - 1; j >= 0; j--) {
            psi[nn][j] = psi[nn][j + 1] + vBy[nn][nn][j] * xres;
        }
        for (j = nn + 1; j < 2 * nn + 1; j++) {
            psi[nn][j] = psi[nn][j - 1] - vBy[nn][nn][j] * xres;
        }
        for (i = nn + 1; i < 2 * nn + 1; i++) {
            for (j = 0; j < 2 * nn + 1; j++) {
                psi[i][j] = psi[i - 1][j] + yres * vBx[nn][i][j];
            }
        }
        for (i = nn - 1; i >= 0; i--) {
            for (j = 0; j < 2 * nn + 1; j++) {
                psi[i][j] = psi[i + 1][j] - yres * vBx[nn][i][j];
            }
        }
        return (true);
    }

    private static void printmatrix(String label, double[][] a) {
        int i;
        System.out.println(label);
        for (i = 0; i < a.length; i++) {
            System.out.println(a[i][0] + " " + a[i][1] + " " + a[i][2]);
        }
    }

    private boolean localmaxima(double[][] array) {
        int ii, jj, npts;
        double center;
        center = array[nn][nn];
        for (jj = -nn; jj <= nn; jj++) {
            for (ii = -nn; ii <= nn; ii++) {
                if (ii == 0 && jj == 0) continue;
                if (array[nn + jj][nn + ii] > center) return (false);
            }
        }
        return (true);
    }

    private boolean localminima(double[][] array) {
        int ii, jj, npts;
        double center;
        center = array[nn][nn];
        for (jj = -nn; jj <= nn; jj++) {
            for (ii = -nn; ii <= nn; ii++) {
                if (ii == 0 && jj == 0) continue;
                if (array[nn + jj][nn + ii] < center) return (false);
            }
        }
        return (true);
    }

    private void calcSpeed() {
        int ii, jj;
        for (jj = -nn; jj <= nn; jj++) {
            for (ii = -nn; ii <= nn; ii++) {
                double vbxsq = vBx[nn][jj + nn][ii + nn] * vBx[nn][jj + nn][ii + nn];
                double vbysq = vBy[nn][jj + nn][ii + nn] * vBy[nn][jj + nn][ii + nn];
                double vbzsq = vBz[nn][jj + nn][ii + nn] * vBz[nn][jj + nn][ii + nn];
                speed[jj + nn][ii + nn] = Math.sqrt(vbxsq + vbysq + vbzsq);
            }
        }
    }

    private boolean vdist(double[] v1, double[] v2) {
        double vval;
        vval = (v1[0] - v2[0]) * (v1[0] - v2[0]) + (v1[1] - v2[1]) * (v1[1] - v2[1]) + (v1[2] - v2[2]) * (v1[2] - v2[2]);
        if (vval <= maxvdist) return (true);
        return (false);
    }

    void mergevortices() {
        int i, j, pass, mergers;
        double[] v1, v2;
        v1 = new double[3];
        v2 = new double[3];
        vclass = new int[numv];
        for (i = 0; i < numv; i++) vclass[i] = i + 1;
        mergers = 1;
        pass = 1;
        while (mergers > 0) {
            mergers = 0;
            for (i = 0; i < numv; i++) {
                for (j = 0; j < numv; j++) {
                    if (vclass[i] == vclass[j]) continue;
                    v1[0] = (double) vorticesi[i];
                    v1[1] = (double) vorticesj[i];
                    v1[2] = (double) vorticesk[i];
                    v2[0] = (double) vorticesi[j];
                    v2[1] = (double) vorticesj[j];
                    v2[2] = (double) vorticesk[j];
                    if (vdist(v1, v2) == false) continue;
                    vclass[i] = (int) Math.min((double) vclass[i], (double) vclass[j]);
                    vclass[j] = (int) Math.min((double) vclass[i], (double) vclass[j]);
                    mergers++;
                }
            }
            pass++;
        }
        return;
    }

    void relabelclasses() {
        int i, j, oldclass, nextclass, nhits;
        newvclass = new int[numv];
        labeled = new int[numv];
        vclassctr = new int[numv];
        nextclass = 1;
        for (i = 0; i < numv; i++) {
            nhits = 0;
            for (j = 0; j < numv; j++) {
                if (vclass[j] == i + 1 && labeled[j] == 0) {
                    newvclass[j] = nextclass;
                    labeled[j] = 1;
                    nhits++;
                }
                vclassctr[nextclass - 1] = nhits;
            }
            if (nhits > 0) nextclass++;
        }
        numc = nextclass - 1;
        return;
    }

    void sortclasses() {
        int i, j;
        boolean domore;
        vclassptr = new int[numc];
        for (i = 0; i < numc; i++) vclassptr[i] = i;
        domore = true;
        while (domore == true) {
            domore = false;
            for (i = 0; i < numc - 1; i++) {
                if (vclassctr[vclassptr[i]] < vclassctr[vclassptr[i + 1]]) {
                    int temp = vclassptr[i];
                    vclassptr[i] = vclassptr[i + 1];
                    vclassptr[i + 1] = temp;
                    domore = true;
                }
            }
        }
    }

    void printnewclasses() {
        int i, j, ic;
        double x, y, z;
        System.out.println("original vortex order:");
        for (i = 0; i < numv; i++) {
            System.out.println("class " + newvclass[i] + ": (" + (vorticesi[i] + 1) + "," + (vorticesj[i] + 1) + "," + (vorticesk[i] + 1) + ")");
        }
        System.out.println("vortices ordered by class:");
        for (i = 0; i < numc; i++) {
            for (j = 0; j < numv; j++) {
                if (newvclass[j] == i + 1 && vclassctr[i] >= classthreshold) System.out.println("class " + (i + 1) + ": (" + (vorticesi[j] + 1) + "," + (vorticesj[j] + 1) + "," + (vorticesk[j] + 1) + ")");
            }
        }
        System.out.println("xyz vortices ordered by class:");
        for (i = 0; i < numc; i++) {
            for (j = 0; j < numv; j++) {
                if (newvclass[j] == i + 1 && vclassctr[i] >= classthreshold) System.out.println("class " + (i + 1) + ": (" + vorticesx[j] + "," + vorticesy[j] + "," + vorticesz[j] + ")");
            }
        }
        for (i = 0; i < numc; i++) {
            System.out.println("# members of class " + (i + 1) + " = " + vclassctr[i]);
        }
        return;
    }

    void setn(int nx, int ny, int nz) {
        this.nx = nx;
        this.ny = ny;
        this.nz = nz;
        vx = new float[nz][ny][nx];
        vy = new float[nz][ny][nx];
        vz = new float[nz][ny][nx];
    }

    void setijk(int imin, int imax, int jmin, int jmax, int kmin, int kmax) {
        this.imin = imin;
        this.imax = imax;
        this.jmin = jmin;
        this.jmax = jmax;
        this.kmin = kmin;
        this.kmax = kmax;
    }

    void setx(float fXMin, float fXRes) {
        xmin = fXMin;
        xres = fXRes;
        xres2 = 2 * xres;
    }

    void sety(float fYMin, float fYRes) {
        ymin = fYMin;
        yres = fYRes;
        yres2 = 2 * yres;
    }

    void setz(float fZMin, float fZRes) {
        zmin = fZMin;
        zres = fZRes;
        zres2 = 2 * zres;
    }

    void setv(float[][][] vx, float[][][] vy, float[][][] vz) {
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
    }

    void setdebug(boolean debug) {
        this.debug = debug;
        System.out.println("in debug mode -- verbose output");
    }

    void printstats() {
        System.out.println("main: # pts w/ q>0 = " + nqplus + " out of " + npts);
        System.out.println("main: # pts w/ closed streamlines = " + npclosed);
        System.out.println("main: # pts w/ min speed = " + nspeed);
        System.out.println("main: # vortices stored = " + numv);
    }

    int getNumHits() {
        return (numv);
    }

    int getNumClasses() {
        return (numc);
    }

    int getClassSize(int iclass) {
        if (iclass < 1 || iclass > numc) return (0);
        return (vclassctr[iclass - 1]);
    }

    int getSortedClassSize(int iclass) {
        if (iclass < 1 || iclass > numc) return (0);
        return (vclassctr[vclassptr[iclass - 1]]);
    }

    double[] getXvortices() {
        return (vorticesx);
    }

    double[] getYvortices() {
        return (vorticesy);
    }

    double[] getZvortices() {
        return (vorticesz);
    }

    double[] getXvortices(int iclass) {
        int i, j;
        int realclass = vclassptr[iclass - 1];
        int classSize = getClassSize(realclass + 1);
        double[] classvorticesx = new double[classSize];
        i = 0;
        for (j = 0; j < numv; j++) {
            if ((newvclass[j] == realclass + 1) && (vclassctr[realclass] >= classthreshold)) {
                classvorticesx[i++] = vorticesx[j];
            }
        }
        return (classvorticesx);
    }

    double[] getYvortices(int iclass) {
        int i, j;
        int realclass = vclassptr[iclass - 1];
        int classSize = getClassSize(realclass + 1);
        double[] classvorticesy = new double[classSize];
        i = 0;
        for (j = 0; j < numv; j++) {
            if ((newvclass[j] == realclass + 1) && (vclassctr[realclass] >= classthreshold)) {
                classvorticesy[i++] = vorticesy[j];
            }
        }
        return (classvorticesy);
    }

    double[] getZvortices(int iclass) {
        int i, j;
        int realclass = vclassptr[iclass - 1];
        int classSize = getClassSize(realclass + 1);
        double[] classvorticesz = new double[classSize];
        i = 0;
        for (j = 0; j < numv; j++) {
            if ((newvclass[j] == realclass + 1) && (vclassctr[realclass] >= classthreshold)) {
                classvorticesz[i++] = vorticesz[j];
            }
        }
        return (classvorticesz);
    }

    int x2i(double x) {
        return ((int) ((x - xmin) / xres + .5));
    }

    int y2j(double y) {
        return ((int) ((y - ymin) / yres + .5));
    }

    int z2k(double z) {
        return ((int) ((z - zmin) / zres + .5));
    }

    double[] getVorticity(double x, double y, double z) {
        int i, j, k;
        double[] vorticity = new double[3];
        i = x2i(x);
        j = y2j(y);
        k = z2k(z);
        vorticity[0] = ((double) vz[k][j + 1][i] - (double) vz[k][j - 1][i]) / (double) yres2 - ((double) vy[k + 1][j][i] - (double) vy[k - 1][j][i]) / (double) zres2;
        vorticity[1] = ((double) vx[k + 1][j][i] - (double) vx[k - 1][j][i]) / (double) zres2 - ((double) vz[k][j][i + 1] - (double) vz[k][j][i - 1]) / (double) xres2;
        vorticity[2] = ((double) vy[k][j][i + 1] - (double) vy[k][j][i - 1]) / (double) xres2 - ((double) vx[k][j + 1][i] - (double) vx[k][j - 1][i]) / (double) yres2;
        return (vorticity);
    }

    public void execute() {
        int i, j, k;
        double fi, fj, fk;
        boolean ireturn;
        double qval;
        numv = 0;
        debug = false;
        if (debug == true) {
            imin = nx / 2;
            imax = nx / 2;
            jmin = ny / 2;
            jmax = ny / 2;
            kmin = nz / 2;
            kmax = nz / 2;
        }
        float total = (kmax - kmin + 1) * (jmax - jmin + 1) * (imax - imin + 1);
        float doneinc = (imax - imin + 1) * (jmax - jmin + 1);
        float done = 0;
        nspeed = 0;
        npclosed = 0;
        nqplus = 0;
        for (k = kmin; k <= kmax; k++) {
            for (j = jmin; j <= jmax; j++) {
                for (i = imin; i <= imax; i++) {
                    if (debug == true) System.out.println("k = " + k + " j = " + j + " i = " + i);
                    if (debug == true) System.out.println("x = " + i2x(i) + " y = " + j2y(j) + " z = " + k2z(k));
                    npts++;
                    qval = getgradv(i, j, k);
                    if (debug == true) System.out.println("q = " + qval);
                    if (qval <= 0) continue;
                    nqplus++;
                    getvorticity(i, j, k);
                    if (debug == true) {
                        System.out.println("vorticity = " + omega[0] + " " + omega[1] + " " + omega[2]);
                        System.out.println("vx = " + vx[k][j][i]);
                        System.out.println("vy = " + vy[k][j][i]);
                        System.out.println("vz = " + vz[k][j][i]);
                    }
                    getBasis();
                    if (debug == true) printmatrix("B:", B);
                    ireturn = projectB(i, j, k);
                    if (ireturn == false) continue;
                    if (debug == true) printmatrix("vBx:", vBx[nn]);
                    if (debug == true) printmatrix("vBy:", vBy[nn]);
                    if (debug == true) printmatrix("vBz:", vBz[nn]);
                    ireturn = getPsiP(i, j, k);
                    if (ireturn == false) continue;
                    ireturn = localmaxima(psi);
                    if (debug == true) printmatrix("psi:", psi);
                    if (ireturn == false) continue;
                    npclosed++;
                    calcSpeed();
                    ireturn = localminima(speed);
                    if (debug == true) printmatrix("speed:", speed);
                    if (ireturn == false) continue;
                    nspeed++;
                    getvorticity(i, j, k);
                    if (debug == true) System.out.println("vorticity = " + omega[0] + "," + omega[1] + "," + omega[2]);
                    if (numv < maxv) {
                        vorticesi[numv] = i;
                        vorticesj[numv] = j;
                        vorticesk[numv] = k;
                        vorticesx[numv] = i2x(i);
                        vorticesy[numv] = j2y(j);
                        vorticesz[numv] = k2z(k);
                    }
                    numv++;
                    if (this.isInterrupted()) {
                        return;
                    }
                }
            }
            done += doneinc;
            this.setProgress(done / total);
        }
    }
}
