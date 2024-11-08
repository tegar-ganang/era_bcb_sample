package PRISM.RobotCtrl.Activities;

import java.util.Vector;
import PRISM.RobotCtrl.Activity;
import PRISM.RobotCtrl.Matrix;
import PRISM.RobotCtrl.SensorData;
import PRISM.RobotCtrl.Utility;
import PRISM.RobotCtrl.VRWEvent;

/**
 * 
 * @author Mauro Dragone
 */
public class ActFeature extends Activity {

    final int MAXK = 20;

    final int MINCLUSTERSIZE = 5;

    final int MAXCLUSTERSIZE = 15;

    final float PROBABILITY_OUTLIER = (float) 0.2;

    final float MAX_WALL_ANGLE_ERROR = 450;

    final float MAX_WALL_DISTANCE_ERROR = 400;

    final float MIN_LABELLING_CONFIDENCE = (float) 0.5;

    final float MIN_GLOBAL_LABELLING_CONFIDENCE = (float) 0.9;

    final float MAX_REGRESSION_RESIDUAL = 500;

    final float MAX_DIFFERENCE_ADJACENT_SONAR = 10;

    final float MAX_SONAR_RANGE = 200;

    final float MIN_SONAR_RANGE = 12;

    final int FIRSTLOG = 0;

    boolean bPrintLength = false;

    boolean bPrintXY = false;

    boolean bPrintRegression = false;

    boolean bPrintFeature = false;

    boolean bPrintAssignments = false;

    boolean bPrintClusters = false;

    boolean bPrintIter = false;

    boolean bPrintRelaxationRelations = false;

    boolean bPrintPos = true;

    boolean bPrintCovs = false;

    boolean bPrintErrors = true;

    boolean bCorrect = true;

    boolean bCorrectOnlyOnce = true;

    boolean bTriggered = false;

    int rPos[] = new int[3];

    int rVel[] = new int[2];

    int cPos[] = new int[3];

    int cOldPos[] = new int[3];

    int x[] = new int[16];

    int y[] = new int[16];

    long xx[] = new long[16];

    long yy[] = new long[16];

    long xy[] = new long[16];

    double b[] = new double[16];

    double m[] = new double[16];

    double e[] = new double[16];

    int t[] = new int[16];

    double w[] = new double[MAXK];

    double wm[] = new double[MAXK];

    double r[] = new double[MAXK];

    int K = 0;

    double mt[][] = new double[2][2];

    boolean bClusterUsed[] = new boolean[MAXK];

    final double VARIABLE = -99999.99;

    int oldX = -20000;

    int oldY = -20000;

    int oldTheta = 0;

    int firstX = 0;

    int firstY = 0;

    int firstTheta = 0;

    float B = (float) 0.24;

    float epsilon = (float) (0.00227 / 1500);

    double cXOld;

    double cYOld;

    double cThetaOld;

    double cX;

    double cY;

    double cTheta;

    float CovS[][] = new float[3][3];

    float help33[][] = new float[3][3];

    float help33b[][] = new float[3][3];

    float identity[][] = new float[3][3];

    float CovU[][] = new float[2][2];

    float help32[][] = new float[3][2];

    float GradS[][] = new float[3][3];

    float GradU[][] = new float[3][2];

    float invCovS[][] = new float[3][3];

    float cosK = 1;

    float cosKold = 1;

    float sinK = 0;

    float sinKold = 0;

    float CovZ[][] = new float[3][3];

    float KGain[][] = new float[3][3];

    float correction[] = new float[3];

    int lr[] = new int[4];

    int lw[] = new int[4];

    SensorData oldSensorData = null;

    double travelled = -1;

    boolean debug = false;

    float dZ[] = new float[3];

    float dZ2[] = new float[3];

    double YCorr = 0;

    double XCorr = 0;

    double errAngle = 0;

    float parRVer[] = new float[MAXK * MAXK];

    float parRHor[] = new float[MAXK * MAXK];

    float ortR[] = new float[MAXK * MAXK];

    int parRVeri[] = new int[MAXK * MAXK];

    int parRVerj[] = new int[MAXK * MAXK];

    int parRHori[] = new int[MAXK * MAXK];

    int parRHorj[] = new int[MAXK * MAXK];

    int ortRi[] = new int[MAXK * MAXK];

    int ortRj[] = new int[MAXK * MAXK];

    float P[][] = new float[MAXK][4];

    float Q[][] = new float[MAXK][4];

    boolean bNeverCorrected = false;

    int iter = 0;

    int obst[] = new int[32];

    public ActFeature() {
        super("ActFeature", "Sonar processing, feature extraction");
        System.out.println("ActFeature costructor");
        K = 0;
        for (int i = 0; i < MAXK; i++) {
            w[i] = wm[i] = r[i] = 0;
        }
        mt[0][0] = 1;
        mt[0][1] = 0;
        mt[1][0] = 0;
        mt[1][1] = 1;
        CovS[0][0] = 100;
        CovS[0][1] = 0;
        CovS[0][2] = 0;
        CovS[1][0] = 0;
        CovS[1][1] = 100;
        CovS[1][2] = 0;
        CovS[2][0] = 0;
        CovS[2][1] = 0;
        CovS[2][2] = (float) 10;
        invCovS[0][0] = 0;
        invCovS[0][1] = 0;
        invCovS[0][2] = 0;
        invCovS[1][0] = 0;
        invCovS[1][1] = 0;
        invCovS[1][2] = 0;
        invCovS[2][0] = 0;
        invCovS[2][1] = 0;
        invCovS[2][2] = 0;
        GradS[0][0] = 1;
        GradS[0][1] = 0;
        GradS[0][2] = 0;
        GradS[1][0] = 0;
        GradS[1][1] = 1;
        GradS[1][2] = 0;
        GradS[2][0] = 0;
        GradS[2][1] = 0;
        GradS[2][2] = 1;
        CovZ[0][0] = 0;
        CovZ[0][1] = 0;
        CovZ[0][2] = 0;
        CovZ[1][0] = 0;
        CovZ[1][1] = 0;
        CovZ[1][2] = 0;
        CovZ[2][0] = 0;
        CovZ[2][1] = 0;
        CovZ[2][2] = 0;
        GradU[0][0] = 0;
        GradU[0][1] = 0;
        GradU[1][0] = 0;
        GradU[1][1] = 0;
        GradU[2][0] = 0;
        GradU[2][1] = 0;
        CovU[0][0] = 100 * epsilon;
        CovU[0][1] = 0;
        CovU[1][0] = 0;
        CovU[1][1] = 100 * epsilon;
        lr[0] = -686;
        lw[0] = 0;
        lr[1] = 370;
        lw[1] = 900;
        lr[2] = 686;
        lw[2] = 0;
        lr[3] = -370;
        lw[3] = 900;
    }

    public void compact(int i) {
        for (int j = i; j < K - 1; j++) {
            r[j] = r[j + 1];
            w[j] = w[j + 1];
            wm[j] = wm[j + 1];
        }
        K--;
    }

    public void computeCorrection(double XCorr, double YCorr, double errAngle, float dZ[]) {
        if (XCorr == VARIABLE) XCorr = 0;
        if (YCorr == VARIABLE) YCorr = 0;
        dZ[0] = 0;
        dZ[1] = 0;
        dZ[2] = +(float) (errAngle * Math.PI / (float) 1800);
        double cs = Math.cos(-dZ[2]);
        double ss = Math.sin(-dZ[2]);
        double rX = cs * cX + ss * cY;
        double rY = -ss * cX + cs * cY;
        if (debug) System.out.println("  Rotating pos (" + cX + "," + cY + ") of " + dZ[2] + " rad : (" + rX + "," + rY + ")");
        if (debug) System.out.println("  Computing correction for " + XCorr + ", " + YCorr + "," + errAngle);
        dZ[1] = (float) (rY - cY + YCorr) * (float) 0.00254;
        dZ[0] = (float) (rX - cX + XCorr) * (float) 0.00254;
        if (debug) System.out.println("Delta Measure Z=[" + dZ[0] + "," + dZ[1] + "," + dZ[2]);
    }

    public void correct(double XCorr, double YCorr, double errAngle) {
        double pX = cX;
        double pY = cY;
        if (XCorr == VARIABLE) XCorr = 0;
        if (YCorr == VARIABLE) YCorr = 0;
        System.out.println("Features before correction (" + XCorr + "," + YCorr + "," + errAngle + ")");
        for (int i = 0; i < K; i++) {
            System.out.println(i + "] " + w[i] + "," + r[i] + " (" + wm[i] + ")");
        }
        computeCorrection(XCorr, YCorr, errAngle, dZ);
        Matrix.AddMatrix(CovS, CovZ, help33);
        if (debug) Matrix.Print("Help33", help33);
        Matrix.Inverse(help33, help33b);
        if (debug) Matrix.Print("Inverse", help33b);
        Matrix.MultiplyMatrix(CovS, help33b, KGain);
        if (debug) Matrix.Print("KGain", KGain);
        Matrix.MultiplyMatrixVector(KGain, dZ, correction);
        if (debug) System.out.println("Final correction =[" + correction[0] + "," + correction[1] + "," + correction[2]);
        if ((CovS[2][2] < 1) && (CovS[1][1] < 1) && (CovS[2][2] < 1)) {
            cTheta += correction[2] * 1800 / Math.PI;
            cY += correction[1] / (float) 0.00254;
            cX += correction[0] / (float) 0.00254;
        } else {
            if (debug) System.out.println("Error variance too big, applying measurement corrections without Kalman Filter");
            cTheta += dZ[2] * 1800 / Math.PI;
            cY += dZ[1] / (float) 0.00254;
            cX += dZ[0] / (float) 0.00254;
        }
        for (int yr = 0; yr < 3; yr++) {
            for (int yc = 0; yc < 3; yc++) {
                help33[yr][yc] = -KGain[yr][yc];
                help33b[yr][yc] = CovS[yr][yc];
            }
            help33[yr][yr] += 1;
        }
        if (bPrintCovs) Matrix.Print("CovS before correction", CovS);
        Matrix.MultiplyMatrix(help33, help33b, CovS);
        if (bPrintCovs) Matrix.Print("Covs after correction", CovS);
        float dErr = (float) Math.sqrt((cX - rPos[0]) * (cX - rPos[0]) + (cY - rPos[1]) * (cY - rPos[1]));
        float dA = (float) Math.abs((int) (Utility.deltaAngle(cTheta, rPos[2])));
        if (dA > 1800) dA -= 3600;
        if (Math.abs(dErr) > 200 || Math.abs(dA) > 200) System.out.println("BIGERROR");
        if (bPrintErrors) System.out.println("CORRECTION AT ITER" + iter + " Errors: " + dErr + ", " + dA);
        double movX = rPos[0] - firstX;
        double movY = rPos[1] - firstY;
        double movTheta = (rPos[2] - firstTheta);
        System.out.println("Movement is" + movX + "," + movY + "," + movTheta);
        double rotTheta = -(cTheta - rPos[2]) * Math.PI / 1800;
        double rot[][] = new double[2][2];
        rot[0][0] = Math.cos(rotTheta);
        rot[0][1] = Math.sin(rotTheta);
        rot[1][0] = -rot[0][1];
        rot[1][1] = rot[0][0];
        movX = movX * rot[0][0] + movY * rot[0][1];
        movY = movX * rot[1][0] + movY * rot[1][1];
        System.out.println("Rotated movement is" + movX + "," + movY);
        System.out.println("Corrected position is" + cX + "," + cY);
        System.out.println("Original pos was :" + (cX - movX) + "," + (cY - movY) + "," + (cTheta - movTheta));
        int pos[] = new int[3];
        pos[0] = (int) Math.floor(cX);
        rPos[0] = pos[0];
        pos[1] = (int) Math.floor(cY);
        rPos[1] = pos[1];
        pos[2] = (int) Math.floor(cTheta);
        rPos[2] = pos[2];
        getImpl().set_position(pos);
        double deltaTheta = (rPos[2] - cTheta) * Math.PI / 1800;
        mt[0][0] = Math.cos(deltaTheta);
        mt[0][1] = Math.sin(deltaTheta);
        mt[1][0] = -mt[0][1];
        mt[1][1] = mt[0][0];
        oldX = rPos[0];
        oldY = rPos[1];
        oldTheta = rPos[2];
        cXOld = cX;
        cYOld = cY;
        cThetaOld = cTheta;
        System.out.println("Features after correction");
        double t = Math.sqrt(XCorr * XCorr + YCorr * YCorr);
        System.out.println("t=sqrt(" + (XCorr * XCorr + YCorr * YCorr) + ") =" + t);
        double alpha;
        if (XCorr != 0) {
            if (XCorr > 0) alpha = 0; else alpha = 1800;
        } else {
            if (YCorr > 0) alpha = 900; else alpha = -900;
        }
        for (int i = 0; i < K; i++) {
            r[i] = r[i] + t * Math.cos((double) ((w[i] - alpha) * Math.PI / 1800));
            w[i] += errAngle;
            if (w[i] > 1800) {
                r[i] = -r[i];
                w[i] -= 1800;
            }
            System.out.println(i + "] " + w[i] + "," + r[i] + " (" + wm[i] + ")");
        }
    }

    public Vector<VRWEvent> process() {
        int i, c, j;
        int n = 3;
        double bx, mx, by, my;
        double avgErr, avgErrY, avgErrX;
        double minErr = 10;
        int nFits = 0;
        boolean bPrint = false;
        boolean corrected = false;
        int featureUsed = -1;
        getImpl().get_position(rPos);
        getImpl().get_velocity(rVel);
        iter++;
        if (oldX == -20000) {
            cX = 0;
            cY = 0;
            cTheta = 0;
            double deltaTheta = (0 - cTheta) * Math.PI / 1800;
            mt[0][0] = Math.cos(deltaTheta);
            mt[0][1] = Math.sin(deltaTheta);
            mt[1][0] = -mt[0][1];
            mt[1][1] = mt[0][0];
            oldX = rPos[0];
            oldY = rPos[1];
            oldTheta = rPos[2];
            cXOld = cX;
            cYOld = cY;
            cThetaOld = cTheta;
            firstX = oldX;
            firstY = firstY;
            firstTheta = oldTheta;
        } else {
            int moveX = rPos[0] - oldX;
            int moveY = rPos[1] - oldY;
            long deltaTheta = rPos[2] - oldTheta;
            cX = cXOld + mt[0][0] * moveX + mt[0][1] * moveY;
            cY = cYOld + mt[1][0] * moveX + mt[1][1] * moveY;
            cTheta = cThetaOld + deltaTheta;
            if (cTheta > 3600) cTheta -= 3600;
            if (cTheta < 0) cTheta += 3600;
        }
        if (travelled == -1) travelled = 0; else {
            travelled = Math.sqrt((cX - cOldPos[0]) * (cX - cOldPos[0]) + (cY - cOldPos[1]) * (cY - cOldPos[1]));
            travelled *= 0.00254;
            if (debug) System.out.println("Travelled=" + travelled);
        }
        cPos[0] = (int) Math.floor(cX);
        cPos[1] = (int) Math.floor(cY);
        cPos[2] = (int) Math.floor(cTheta);
        cOldPos[0] = cPos[0];
        cOldPos[1] = cPos[1];
        cOldPos[2] = cPos[2];
        double roAngle = cPos[0] * Math.PI / 1800;
        cosK = (float) Math.cos(roAngle);
        sinK = (float) Math.sin(roAngle);
        GradS[0][2] = -(float) travelled * sinK;
        GradS[1][2] = +(float) travelled * cosK;
        float kD = (float) travelled / (float) (2 * B);
        float sinKD = sinK * kD;
        float cosKD = cosK * kD;
        GradU[0][0] = cosK / 2 + sinKD;
        GradU[0][1] = cosK / 2 - sinKD;
        GradU[1][0] = sinK / 2 - cosKD;
        GradU[1][1] = sinK / 2 + cosKD;
        GradU[2][0] = -1 / B;
        GradU[2][1] = -GradU[2][0];
        Matrix.MultiplyMatrix(GradS, CovS, help33);
        Matrix.MultiplyMatrixTranspose(help33, GradS, CovS);
        Matrix.MultiplyMatrix(GradU, CovU, help32);
        Matrix.MultiplyMatrixTranspose(help32, GradU, help33);
        Matrix.AddMatrix(CovS, help33, CovS);
        if ((iter > FIRSTLOG) && bPrintIter) System.out.println("--- iter:" + iter + " ---");
        if (debug) Matrix.Print("help32", help32);
        if (debug) Matrix.Print("help33", help33);
        if (debug) Matrix.Print("CovS", CovS);
        if ((iter > FIRSTLOG) && bPrintPos) System.out.println(rPos[0] + "," + rPos[1] + "," + rPos[2] + "," + cPos[0] + "," + cPos[1] + "," + cPos[2]);
        if (Math.abs(rVel[1]) > 120) {
            System.out.println("Excluded because too fast");
            return null;
        }
        getImpl().m_sensorData = getImpl().get_sensor_data();
        double dangle = (360 / 16.0) * 3.1416 / 180;
        double angle = cPos[2] * 3.1416 / 1800;
        int length;
        getLocalSpace().getObstaclePolarHistogram(obst);
        for (i = 0; i < 16; i++) {
            length = obst[i * 2] * 10;
            x[i] = (int) Math.floor(Math.cos(angle) * length) + cPos[0];
            y[i] = (int) Math.floor(Math.sin(angle) * length) + cPos[1];
            xx[i] = x[i] * x[i];
            yy[i] = y[i] * y[i];
            xy[i] = x[i] * y[i];
            angle += dangle;
            if (bPrintLength) System.out.println("length[" + i + "] is " + length / 10);
        }
        for (c = 0; c < 16; c++) {
            double sumx = 0.0;
            double sumxx = 0.0;
            double sumy = 0.0;
            double sumxy = 0.0;
            double sumyy = 0.0;
            boolean bSkip = false;
            int minL = 255;
            int maxL = 0;
            if (bPrintXY) System.out.println(+x[c] * (float) 0.00254 + "," + y[c] * (float) 0.00254);
            for (i = 0; (i < n) && (!bSkip); i++) {
                j = (c + i) % 16;
                length = obst[j * 2];
                if (length > MAX_SONAR_RANGE) bSkip = true;
                if (length < MIN_SONAR_RANGE) bSkip = true;
                if (length < minL) minL = length;
                if (length > maxL) maxL = length;
                sumx += x[j];
                sumy += y[j];
                sumxx += xx[j];
                sumyy += yy[j];
                sumxy += xy[j];
            }
            if ((maxL - minL) > MAX_DIFFERENCE_ADJACENT_SONAR) bSkip = true;
            if (bSkip) {
                e[c] = 10000;
                continue;
            }
            double den = sumxx - sumx * sumx / n;
            double den2 = sumyy - sumy * sumy / n;
            double covar = (sumxy - sumx * sumy / n);
            if (den != 0) mx = covar / den; else mx = 3.14 / 2.0;
            if (den2 != 0) my = covar / den2; else my = 3.14 / 2.0;
            bx = (sumy - mx * sumx) / n;
            by = (sumx - my * sumy) / n;
            avgErrX = 0;
            for (i = 0; i < n; i++) {
                j = (c + i) % 16;
                double err = (y[j] - bx - mx * x[j]);
                avgErrX += err * err;
            }
            avgErrY = 0;
            for (i = 0; i < n; i++) {
                j = (c + i) % 16;
                double err = (x[j] - by - my * y[j]);
                avgErrY += err * err;
            }
            if (avgErrX < avgErrY) {
                b[c] = bx;
                m[c] = mx;
                t[c] = 0;
                e[c] = avgErrX;
                if (bPrintRegression) if (e[c] < MAX_REGRESSION_RESIDUAL) System.out.println(" c=" + c + " y= " + m[c] + "x + " + b[c] + " err=" + e[c]);
            } else {
                b[c] = by;
                m[c] = my;
                t[c] = 1;
                e[c] = avgErrY;
                if (bPrintRegression) if (e[c] < MAX_REGRESSION_RESIDUAL) System.out.println(" c=" + c + " x= " + m[c] + "y + " + b[c] + " err=" + e[c]);
            }
        }
        double ro, alphal, den;
        int alpha;
        for (c = 0; c < 16; c++) {
            boolean debug = false;
            if (e[c] < MAX_REGRESSION_RESIDUAL) {
                if (t[c] == 0) {
                    alphal = Math.atan(m[c]);
                    ro = b[c] * Math.cos(alphal);
                    alphal = Math.PI / 2 + alphal;
                } else {
                    alphal = Math.PI - Math.atan(m[c]);
                    if (alphal > Math.PI) alphal -= Math.PI;
                    ro = b[c] * Math.cos(alphal);
                }
                alpha = (int) Math.floor(alphal * 1800 / 3.1416);
                int winner = MAXK + 1;
                double maxDistance = 1e+30;
                double distanceAlpha;
                double distanceRo;
                double distance;
                if (bPrintFeature) System.out.println(alpha + "," + ro + "," + rVel[0] + "," + rVel[1] + "  iter:" + iter + " c=" + c);
                for (i = 0; i < K; i++) {
                    distanceAlpha = Math.abs(Utility.deltaAngle(alpha, (int) Math.floor(w[i])));
                    distanceRo = Math.abs(ro - r[i]);
                    distance = distanceAlpha + distanceRo;
                    if (distance < maxDistance) {
                        maxDistance = distance;
                        winner = i;
                    }
                }
                if ((maxDistance > 200) || (K == 0)) {
                    if (K < MAXK) {
                        w[K] = alpha;
                        wm[K] = 1;
                        r[K] = ro;
                        K++;
                        continue;
                    } else continue;
                }
                if (winner >= MAXK) continue;
                double peso;
                for (j = 0; j <= 0; j++) {
                    i = (winner + j + 36) % 36;
                    peso = (3 - Math.abs(j) + 1) / 4.0;
                    distance = Math.abs(Utility.deltaAngle(alpha, (int) Math.floor(w[i])));
                    if (wm[i] > 0) {
                        w[i] += distance / wm[i];
                        r[i] = (r[i] * wm[i] + ro * peso) / (wm[i] + peso);
                        if (w[i] > 1800) {
                            w[i] -= 1800;
                            r[i] = -r[i];
                        }
                    } else {
                        w[i] = alpha;
                        r[i] = ro;
                    }
                    wm[i] += peso;
                    if (wm[i] > MAXCLUSTERSIZE) wm[i] = MAXCLUSTERSIZE;
                }
            }
        }
        if (K == 0) return null;
        if (debug) if (iter > FIRSTLOG) Matrix.Print("CovS", CovS);
        if (!bNeverCorrected) Matrix.Inverse(CovS, invCovS);
        if (debug) if (iter > FIRSTLOG) Matrix.Print("Inverse", invCovS);
        if ((iter > FIRSTLOG) && (bPrintClusters)) {
            System.out.println("Clusters at iter:" + iter);
            for (i = 0; i < K; i++) {
                System.out.println("Cluster " + w[i] + "," + r[i] + "," + wm[i]);
            }
        }
        int nParRVer = 0;
        int nParRHor = 0;
        int nOrtR = 0;
        int clusterExamined = 0;
        for (i = 0; i < K; i++) {
            float totP = 0;
            for (j = 0; j < 4; j++) {
                P[i][j] = 0;
                Q[i][j] = 0;
                if (wm[i] <= MINCLUSTERSIZE) continue;
                XCorr = VARIABLE;
                YCorr = VARIABLE;
                if (iter > FIRSTLOG && debug) System.out.println("  Using hypothesis, feature " + i + " is wall " + j);
                errAngle = lw[j] - w[i];
                if (lw[j] == 900) YCorr = lr[j] - r[i]; else {
                    if (lr[j] * r[i] < 0) {
                        errAngle += 1800;
                        XCorr = lr[j] + r[i];
                    } else XCorr = lr[j] - r[i];
                }
                computeCorrection(XCorr, YCorr, errAngle, dZ);
                float penalty = 0;
                if (debug) System.out.println("Pos would go to " + (cX + dZ[0] / (float) 0.00254) + ", " + (cY + dZ[1] / (float) 0.00254));
                if ((YCorr != VARIABLE) && (Math.abs(cY + dZ[1] / (float) 0.00254) > lr[1]) || (XCorr != VARIABLE) && (Math.abs(cX + dZ[0] / (float) 0.00254) > lr[2])) {
                    if (debug) System.out.println("Penalty");
                    penalty = (float) 0.8;
                }
                Matrix.MultiplyMatrixVector(invCovS, dZ, dZ2);
                double mahalanobis = dZ[0] * dZ2[0] + dZ[1] * dZ2[1] + dZ[2] * dZ2[2];
                double probIsWall = (0.5 + 0.5 * (wm[i] - MINCLUSTERSIZE) / (MAXCLUSTERSIZE - MINCLUSTERSIZE));
                double probIsWallj = 0;
                probIsWallj += -mahalanobis / 2;
                probIsWallj = Math.exp(probIsWallj);
                if ((iter > FIRSTLOG) && bPrintAssignments) System.out.println(" P(" + i + " is " + j + ") = e(" + (-mahalanobis / 2) + ")=" + probIsWallj);
                P[i][j] = (float) (probIsWall * probIsWallj) * (1 - penalty);
                totP += P[i][j];
            }
            if (wm[i] <= MINCLUSTERSIZE) continue;
            clusterExamined++;
            for (j = 0; j < 4; j++) {
                if (totP == 0) P[i][j] = 0; else P[i][j] /= totP;
            }
            for (j = i + 1; j < K; j++) {
                if (wm[j] <= MINCLUSTERSIZE) continue;
                float errorDistanceWallsHor = Math.abs((int) (Math.abs(r[i] - r[j]) - lr[2] * 2));
                float errorDistanceWallsVer = Math.abs((int) (Math.abs(r[i] - r[j]) - lr[1] * 2));
                int errorDifferenceAngles = Math.abs((int) (Utility.deltaAngle((int) Math.floor(w[i]), (int) Math.floor(w[j]))));
                float probParallel = errorDifferenceAngles;
                float probOrtogonal = Math.abs(errorDifferenceAngles - 900);
                if (probParallel < MAX_WALL_ANGLE_ERROR) {
                    if (errorDistanceWallsHor < MAX_WALL_DISTANCE_ERROR) {
                        parRHor[nParRHor] = ((MAX_WALL_ANGLE_ERROR - probParallel) + (MAX_WALL_DISTANCE_ERROR - errorDistanceWallsHor)) / (MAX_WALL_ANGLE_ERROR + MAX_WALL_DISTANCE_ERROR);
                        parRHori[nParRHor] = i;
                        parRHorj[nParRHor] = j;
                        nParRHor++;
                    }
                    if (errorDistanceWallsVer < MAX_WALL_DISTANCE_ERROR) {
                        parRVer[nParRVer] = ((MAX_WALL_ANGLE_ERROR - probParallel) + (MAX_WALL_DISTANCE_ERROR - errorDistanceWallsVer)) / (MAX_WALL_ANGLE_ERROR + MAX_WALL_DISTANCE_ERROR);
                        parRVeri[nParRVer] = i;
                        parRVerj[nParRVer] = j;
                        nParRVer++;
                    }
                }
                if (probOrtogonal < MAX_WALL_ANGLE_ERROR) {
                    ortR[nOrtR] = (MAX_WALL_ANGLE_ERROR - probOrtogonal) / MAX_WALL_ANGLE_ERROR;
                    ortRi[nOrtR] = i;
                    ortRj[nOrtR] = j;
                    nOrtR++;
                }
            }
        }
        if ((iter > FIRSTLOG) && bPrintRelaxationRelations) {
            if (clusterExamined > 0) Matrix.Print("P", P);
            if (nParRHor > 0) System.out.println("The following walls could be the horrizzontal walls");
            for (int t = 0; t < nParRHor; t++) System.out.println(parRHori[t] + "," + parRHorj[t] + " R=" + parRHor[t]);
            if (nParRVer > 0) {
                System.out.println("");
                System.out.println("The following walls could be the vertical walls");
            }
            for (int t = 0; t < nParRVer; t++) System.out.println(parRVeri[t] + "," + parRVerj[t] + " R=" + parRVer[t]);
            if (nOrtR > 0) {
                System.out.println("");
                System.out.println("The following walls could be ortogonal walls");
            }
            for (int t = 0; t < nOrtR; t++) System.out.println(ortRi[t] + "," + ortRj[t] + " R=" + ortR[t]);
        }
        int maxStep = 1;
        if (nOrtR + nParRVer + nParRHor > 0) maxStep = 5;
        float F = 0;
        for (int step = 0; step < maxStep; step++) {
            for (i = 0; i < K; i++) for (j = 0; j < 4; j++) Q[i][j] = 0;
            for (int r = 0; r < nOrtR; r++) {
                Q[ortRi[r]][0] += (float) 0.5 * (P[ortRj[r]][1] + P[ortRj[r]][3]) * ortR[r];
                Q[ortRi[r]][1] += (float) 0.5 * (P[ortRj[r]][0] + P[ortRj[r]][2]) * ortR[r];
                Q[ortRi[r]][2] += (float) 0.5 * (P[ortRj[r]][1] + P[ortRj[r]][3]) * ortR[r];
                Q[ortRi[r]][3] += (float) 0.5 * (P[ortRj[r]][0] + P[ortRj[r]][2]) * ortR[r];
                Q[ortRj[r]][0] += (float) 0.5 * (P[ortRi[r]][1] + P[ortRi[r]][3]) * ortR[r];
                Q[ortRj[r]][1] += (float) 0.5 * (P[ortRi[r]][0] + P[ortRi[r]][2]) * ortR[r];
                Q[ortRj[r]][2] += (float) 0.5 * (P[ortRi[r]][1] + P[ortRi[r]][3]) * ortR[r];
                Q[ortRj[r]][3] += (float) 0.5 * (P[ortRi[r]][0] + P[ortRi[r]][2]) * ortR[r];
            }
            for (int r = 0; r < nParRHor; r++) {
                Q[parRHori[r]][0] += P[parRHorj[r]][2] * parRHor[r];
                Q[parRHori[r]][2] += P[parRHorj[r]][0] * parRHor[r];
                Q[parRHorj[r]][0] += P[parRHori[r]][2] * parRHor[r];
                Q[parRHorj[r]][2] += P[parRHori[r]][0] * parRHor[r];
            }
            for (int r = 0; r < nParRVer; r++) {
                Q[parRVeri[r]][1] += P[parRVerj[r]][3] * parRVer[r];
                Q[parRVeri[r]][3] += P[parRVerj[r]][1] * parRVer[r];
                Q[parRVerj[r]][1] += P[parRVeri[r]][3] * parRVer[r];
                Q[parRVerj[r]][3] += P[parRVeri[r]][1] * parRVer[r];
            }
            if (nOrtR + nParRVer + nParRHor > 0) if (iter > FIRSTLOG && bPrintRelaxationRelations) Matrix.Print("Q", Q);
            F = 0;
            if (nOrtR + nParRVer + nParRHor > 0) {
                for (i = 0; i < K; i++) {
                    if (wm[i] <= MINCLUSTERSIZE) continue;
                    float totP = 0;
                    for (j = 0; j < 4; j++) {
                        P[i][j] = P[i][j] * Q[i][j];
                        F += P[i][j];
                    }
                }
            }
            for (i = 0; i < K; i++) {
                if (wm[i] <= MINCLUSTERSIZE) continue;
                float totP = 0;
                for (j = 0; j < 4; j++) totP += P[i][j];
                for (j = 0; j < 4; j++) if (totP == 0) P[i][j] = 0; else P[i][j] /= totP;
            }
            if (clusterExamined > 0) {
                if ((iter > FIRSTLOG) && bPrintRelaxationRelations) {
                    Matrix.Print("New P after step " + step, P);
                    System.out.println("Global confidence is: " + F);
                }
            }
        }
        boolean bInconsistency = false;
        boolean bFirst = true;
        if (F > MIN_GLOBAL_LABELLING_CONFIDENCE) {
            double xErr = VARIABLE;
            double yErr = VARIABLE;
            double alphaErr = VARIABLE;
            double totWeight = 0;
            for (i = 0; (i < K) && (!bInconsistency); i++) {
                if (wm[i] <= MINCLUSTERSIZE) continue;
                for (j = 0; (j < 4) && (!bInconsistency); j++) {
                    if (P[i][j] < MIN_LABELLING_CONFIDENCE) continue;
                    if (iter > FIRSTLOG && debug) System.out.println("  Using hypothesis, feature " + i + " is wall " + j);
                    XCorr = VARIABLE;
                    YCorr = VARIABLE;
                    errAngle = lw[j] - w[i];
                    if (lw[j] == 900) YCorr = lr[j] - r[i]; else {
                        if (lr[j] * r[i] < 0) {
                            errAngle += 1800;
                            XCorr = lr[j] + r[i];
                        } else XCorr = lr[j] - r[i];
                    }
                    double distanceAlpha = Math.abs(Utility.deltaAngle(errAngle, alphaErr));
                    double distanceX, distanceY, distance;
                    double weight = wm[i];
                    wm[i] = 0;
                    if (XCorr == VARIABLE || xErr == VARIABLE) distanceX = 0; else distanceX = Math.abs(XCorr - xErr);
                    if (YCorr == VARIABLE || yErr == VARIABLE) distanceY = 0; else distanceY = Math.abs(YCorr - yErr);
                    distance = distanceAlpha + distanceX + distanceY;
                    if (distance > (MAX_WALL_ANGLE_ERROR / 2 + MAX_WALL_DISTANCE_ERROR)) if (!bFirst) {
                        System.out.println("Inconsistent labelling");
                        bInconsistency = true;
                        continue;
                    }
                    if (iter > FIRSTLOG && debug) {
                        System.out.print("    Updating err from: " + xErr + "," + yErr + "," + alphaErr + "," + totWeight + "  with: ");
                        System.out.print(XCorr + "," + YCorr + "," + errAngle + "," + weight + "  to: ");
                    }
                    if (alphaErr == VARIABLE) alphaErr = errAngle; else alphaErr += Utility.deltaAngle(errAngle, alphaErr) * (weight / (totWeight + weight));
                    if (XCorr != VARIABLE) {
                        if (xErr == VARIABLE) xErr = XCorr; else xErr = (xErr * totWeight + XCorr * weight) / (totWeight + weight);
                    }
                    if (YCorr != VARIABLE) {
                        if (yErr == VARIABLE) yErr = YCorr; else yErr = (yErr * totWeight + YCorr * weight) / (totWeight + weight);
                    }
                    totWeight += weight;
                    if (iter > FIRSTLOG && debug) System.out.println(xErr + "," + yErr + "," + alphaErr + "," + totWeight);
                    bFirst = false;
                }
            }
            if (xErr == VARIABLE) CovZ[0][0] = (float) 50; else CovZ[0][0] = (float) 1.0;
            if (yErr == VARIABLE) CovZ[1][1] = (float) 50; else CovZ[1][1] = (float) 1.0;
            CovZ[2][2] = (float) 1;
            if (bCorrect && alphaErr != VARIABLE && !bInconsistency) {
                correct(xErr, yErr, alphaErr);
                i = 0;
                while (i < K) {
                    if (wm[i] == 0) compact(i);
                    i++;
                }
            }
        }
        if (K == 19) K = 0;
        if (!bTriggered && (CovS[0][0] < 1) && (CovS[1][1] < 1) && (CovS[2][2] < 1)) {
            if (bCorrectOnlyOnce) bCorrect = false;
            bTriggered = true;
            Vector<VRWEvent> ret = new Vector<VRWEvent>();
            ret.addElement(new VRWEvent("location", new String[] { "localized" }));
            System.out.println("LOCALISED");
            return ret;
        }
        return null;
    }
}
