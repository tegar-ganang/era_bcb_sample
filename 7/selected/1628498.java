package shu.cms.dc.raw;

import shu.cms.image.*;
import shu.math.*;
import shu.math.array.DoubleArray;

/**
 * <p>Title: Colour Management System</p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: </p>
 *
 * @author cms.shu.edu.tw
 * @version 1.0
 * @deprecated
 */
public class CFAInterpolatorOld {

    protected static final void determineThresholdsRB(int i, int j, double[][] in0, double[][] inG, double[][] in2, double[] ave0, double[] Gave, double[] ave2, int[] ind) {
        for (int k = 0; k < ind.length; k++) {
            switch(ind[k]) {
                case 0:
                    ave0[0] = 1. / 2. * (in0[i][j] + in0[i - 2][j]);
                    Gave[0] = inG[i - 1][j];
                    ave2[0] = 1. / 2. * (in2[i - 1][j - 1] + in2[i - 1][j + 1]);
                    break;
                case 1:
                    ave0[1] = 1. / 2. * (in0[i][j] + in0[i][j + 2]);
                    Gave[1] = inG[i][j + 1];
                    ave2[1] = 1. / 2. * (in2[i - 1][j + 1] + in2[i + 1][j + 1]);
                    break;
                case 2:
                    ave0[2] = 1. / 2. * (in0[i][j] + in0[i + 2][j]);
                    Gave[2] = inG[i + 1][j];
                    ave2[2] = 1. / 2. * (in2[i + 1][j - 1] + in2[i + 1][j + 1]);
                    break;
                case 3:
                    ave0[3] = 1. / 2. * (in0[i][j] + in0[i][j - 2]);
                    Gave[3] = inG[i][j - 1];
                    ave2[3] = 1. / 2. * (in2[i - 1][j - 1] + in2[i + 1][j - 1]);
                    break;
                case 4:
                    ave0[4] = 1. / 2. * (in0[i][j] + in0[i - 2][j + 2]);
                    Gave[4] = 1. / 4. * (inG[i][j + 1] + inG[i - 1][j + 2] + inG[i - 1][j] + inG[i - 2][j + 1]);
                    ave2[4] = in2[i - 1][j + 1];
                    break;
                case 5:
                    ave0[5] = 1. / 2. * (in0[i][j] + in0[i + 2][j + 2]);
                    Gave[5] = 1. / 4. * (inG[i][j + 1] + inG[i + 1][j + 2] + inG[i + 1][j] + inG[i + 2][j + 1]);
                    ave2[5] = in2[i + 1][j + 1];
                    break;
                case 6:
                    ave0[6] = 1. / 2. * (in0[i][j] + in0[i - 2][j - 2]);
                    Gave[6] = 1. / 4. * (inG[i][j - 1] + inG[i - 1][j - 2] + inG[i - 1][j] + inG[i - 2][j - 1]);
                    ave2[6] = in2[i - 1][j - 1];
                    break;
                case 7:
                    ave0[7] = 1. / 2. * (in0[i][j] + in0[i + 2][j - 2]);
                    Gave[7] = 1. / 4. * (inG[i][j - 1] + inG[i + 1][j - 2] + inG[i + 1][j] + inG[i + 2][j - 1]);
                    ave2[7] = in2[i + 1][j - 1];
                    break;
            }
        }
    }

    protected static final void determineThresholdsRB(int i, int j, DoubleImage im, double[][] ave, int[] ind, int ch0Index, int ch2Index) {
        for (int k = 0; k < ind.length; k++) {
            switch(ind[k]) {
                case 0:
                    ave[ch0Index][0] = 1. / 2. * (im.getPixel(j, i, ch0Index) + im.getPixel(j, i - 2, ch0Index));
                    ave[1][0] = im.getSubPixel1(j, i - 1);
                    ave[ch2Index][0] = 1. / 2. * (im.getPixel(j - 1, i - 1, ch2Index) + im.getPixel(j + 1, i - 1, ch2Index));
                    break;
                case 1:
                    ave[ch0Index][1] = 1. / 2. * (im.getPixel(j, i, ch0Index) + im.getPixel(j + 2, i, ch0Index));
                    ave[1][1] = im.getSubPixel1(j + 1, i);
                    ave[ch2Index][1] = 1. / 2. * (im.getPixel(j + 1, i - 1, ch2Index) + im.getPixel(j + 1, i + 1, ch2Index));
                    break;
                case 2:
                    ave[ch0Index][2] = 1. / 2. * (im.getPixel(j, i, ch0Index) + im.getPixel(j, i + 2, ch0Index));
                    ave[1][2] = im.getSubPixel1(j, i + 1);
                    ave[ch2Index][2] = 1. / 2. * (im.getPixel(j - 1, i + 1, ch2Index) + im.getPixel(j + 1, i + 1, ch2Index));
                    break;
                case 3:
                    ave[ch0Index][3] = 1. / 2. * (im.getPixel(j, i, ch0Index) + im.getPixel(j - 2, i, ch0Index));
                    ave[1][3] = im.getSubPixel1(j - 1, i);
                    ave[ch2Index][3] = 1. / 2. * (im.getPixel(j - 1, i - 1, ch2Index) + im.getPixel(j - 1, i + 1, ch2Index));
                    break;
                case 4:
                    ave[ch0Index][4] = 1. / 2. * (im.getPixel(j, i, ch0Index) + im.getPixel(j + 2, i - 2, ch0Index));
                    ave[1][4] = 1. / 4. * (im.getSubPixel1(j + 1, i) + im.getSubPixel1(j + 2, i - 1) + im.getSubPixel1(j, i - 1) + im.getSubPixel1(j + 1, i - 2));
                    ave[ch2Index][4] = im.getPixel(j + 1, i - 1, ch2Index);
                    break;
                case 5:
                    ave[ch0Index][5] = 1. / 2. * (im.getPixel(j, i, ch0Index) + im.getPixel(j + 2, i + 2, ch0Index));
                    ave[1][5] = 1. / 4. * (im.getSubPixel1(j + 1, i) + im.getSubPixel1(j + 2, i + 1) + im.getSubPixel1(j, i + 1) + im.getSubPixel1(j + 1, i + 2));
                    ave[ch2Index][5] = im.getPixel(j + 1, i + 1, ch2Index);
                    break;
                case 6:
                    ave[ch0Index][6] = 1. / 2. * (im.getPixel(j, i, ch0Index) + im.getPixel(j - 2, i - 2, ch0Index));
                    ave[1][6] = 1. / 4. * (im.getSubPixel1(j - 1, i) + im.getSubPixel1(j - 2, i - 1) + im.getSubPixel1(j, i - 1) + im.getSubPixel1(j - 1, i - 2));
                    ave[ch2Index][6] = im.getPixel(j - 1, i - 1, ch2Index);
                    break;
                case 7:
                    ave[ch0Index][7] = 1. / 2. * (im.getPixel(j, i, ch0Index) + im.getPixel(j - 2, i + 2, ch0Index));
                    ave[1][7] = 1. / 4. * (im.getSubPixel1(j - 1, i) + im.getSubPixel1(j - 2, i + 1) + im.getSubPixel1(j, i + 1) + im.getSubPixel1(j - 1, i + 2));
                    ave[ch2Index][7] = im.getPixel(j - 1, i + 1, ch2Index);
                    break;
            }
        }
    }

    protected static final void determineThresholdsG(int i, int j, double[][] in0, double[][] inG, double[][] in2, double[] ave0, double[] Gave, double[] ave2, int[] ind) {
        for (int k = 0; k < ind.length; k++) {
            switch(ind[k]) {
                case 0:
                    Gave[0] = 1. / 2. * (inG[i][j] + inG[i - 2][j]);
                    ave2[0] = in2[i - 1][j];
                    ave0[0] = 1. / 4. * (in0[i - 2][j - 1] + in0[i - 2][j + 1] + in0[i][j - 1] + in0[i][j + 1]);
                    break;
                case 1:
                    Gave[1] = 1. / 2. * (inG[i][j] + inG[i][j + 2]);
                    ave0[1] = in0[i][j + 1];
                    ave2[1] = 1. / 4. * (in2[i - 1][j] + in2[i + 1][j] + in2[i - 1][j + 2] + in2[i + 1][j + 2]);
                    break;
                case 2:
                    Gave[2] = 1. / 2. * (inG[i][j] + inG[i + 2][j]);
                    ave2[2] = in2[i + 1][j];
                    ave0[2] = 1. / 4. * (in0[i][j - 1] + in0[i][j + 1] + in0[i + 2][j - 1] + in0[i + 2][j + 1]);
                    break;
                case 3:
                    Gave[3] = 1. / 2. * (inG[i][j] + inG[i][j - 2]);
                    ave0[3] = in0[i][j - 1];
                    ave2[3] = 1. / 4. * (in2[i - 1][j - 2] + in2[i - 1][j] + in2[i + 1][j - 2] + in2[i + 1][j]);
                    break;
                case 4:
                    ave0[4] = 1. / 2. * (in0[i - 2][j + 1] + in0[i][j + 1]);
                    ave2[4] = 1. / 2. * (in2[i - 1][j] + in2[i - 1][j + 2]);
                    Gave[4] = inG[i - 1][j + 1];
                    break;
                case 5:
                    ave0[5] = 1. / 2. * (in0[i][j + 1] + in0[i + 2][j + 1]);
                    ave2[5] = 1. / 2. * (in2[i + 1][j] + in2[i + 1][j + 2]);
                    Gave[5] = inG[i + 1][j + 1];
                    break;
                case 6:
                    ave0[6] = 1. / 2. * (in0[i][j - 1] + in0[i - 2][j - 1]);
                    ave2[6] = 1. / 2. * (in2[i - 1][j - 2] + in2[i - 1][j]);
                    Gave[6] = inG[i - 1][j - 1];
                    break;
                case 7:
                    ave0[7] = 1. / 2. * (in0[i][j - 1] + in0[i + 2][j - 1]);
                    ave2[7] = 1. / 2. * (in2[i + 1][j - 2] + in2[i + 1][j]);
                    Gave[7] = inG[i + 1][j - 1];
                    break;
            }
        }
    }

    protected static final void determineThresholdsG(int i, int j, DoubleImage im, double[][] ave, int[] ind, int ch0index, int ch2index) {
        for (int k = 0; k < ind.length; k++) {
            switch(ind[k]) {
                case 0:
                    ave[1][0] = 1. / 2. * (im.getSubPixel1(j, i) + im.getSubPixel1(j, i - 2));
                    ave[ch2index][0] = im.getPixel(j, i - 1, ch2index);
                    ave[ch0index][0] = 1. / 4. * (im.getPixel(j - 1, i - 2, ch0index) + im.getPixel(j + 1, i - 2, ch0index) + im.getPixel(j - 1, i, ch0index) + im.getPixel(j + 1, i, ch0index));
                    break;
                case 1:
                    ave[1][1] = 1. / 2. * (im.getSubPixel1(j, i) + im.getSubPixel1(j + 2, i));
                    ave[ch0index][1] = im.getPixel(j + 1, i, ch0index);
                    ave[ch2index][1] = 1. / 4. * (im.getPixel(j, i - 1, ch2index) + im.getPixel(j, i + 1, ch2index) + im.getPixel(j + 2, i - 1, ch2index) + im.getPixel(j + 2, i + 1, ch2index));
                    break;
                case 2:
                    ave[1][2] = 1. / 2. * (im.getSubPixel1(j, i) + im.getSubPixel1(j, i + 2));
                    ave[ch2index][2] = im.getPixel(j, i + 1, ch2index);
                    ave[ch0index][2] = 1. / 4. * (im.getPixel(j - 1, i, ch0index) + im.getPixel(j + 1, i, ch0index) + im.getPixel(j - 1, i + 2, ch0index) + im.getPixel(j + 1, i + 2, ch0index));
                    break;
                case 3:
                    ave[1][3] = 1. / 2. * (im.getSubPixel1(j, i) + im.getSubPixel1(j - 2, i));
                    ave[ch0index][3] = im.getPixel(j - 1, i, ch0index);
                    ave[ch2index][3] = 1. / 4. * (im.getPixel(j - 2, i - 1, ch2index) + im.getPixel(j, i - 1, ch2index) + im.getPixel(j - 2, i + 1, ch2index) + im.getPixel(j, i + 1, ch2index));
                    break;
                case 4:
                    ave[ch0index][4] = 1. / 2. * (im.getPixel(j + 1, i - 2, ch0index) + im.getPixel(j + 1, i, ch0index));
                    ave[ch2index][4] = 1. / 2. * (im.getPixel(j, i - 1, ch2index) + im.getPixel(j + 2, i - 1, ch2index));
                    ave[1][4] = im.getSubPixel1(j + 1, i - 1);
                    break;
                case 5:
                    ave[ch0index][5] = 1. / 2. * (im.getPixel(j + 1, i, ch0index) + im.getPixel(j + 1, i + 2, ch0index));
                    ave[ch2index][5] = 1. / 2. * (im.getPixel(j, i + 1, ch2index) + im.getPixel(j + 2, i + 1, ch2index));
                    ave[1][5] = im.getSubPixel1(j + 1, i + 1);
                    break;
                case 6:
                    ave[ch0index][6] = 1. / 2. * (im.getPixel(j - 1, i, ch0index) + im.getPixel(j - 1, i - 2, ch0index));
                    ave[ch2index][6] = 1. / 2. * (im.getPixel(j - 2, i - 1, ch2index) + im.getPixel(j, i - 1, ch2index));
                    ave[1][6] = im.getSubPixel1(j - 1, i - 1);
                    break;
                case 7:
                    ave[ch0index][7] = 1. / 2. * (im.getPixel(j - 1, i, ch0index) + im.getPixel(j - 1, i + 2, ch0index));
                    ave[ch2index][7] = 1. / 2. * (im.getPixel(j - 2, i + 1, ch2index) + im.getPixel(j, i + 1, ch2index));
                    ave[1][7] = im.getSubPixel1(j - 1, i + 1);
                    break;
            }
        }
    }

    public static enum Direction {

        Upper, Lower, Left, Right
    }

    public static final double[][][] nearestNeighborReplication(double[][][] image, Direction direction) {
        int m = image[0].length;
        int n = image[0][0].length;
        double[][] inR = image[0];
        double[][] inG = image[1];
        double[][] inB = image[2];
        double[][] outR = DoubleArray.copy(inR);
        double[][] outG = DoubleArray.copy(inG);
        double[][] outB = DoubleArray.copy(inB);
        double[][][] out = new double[][][] { outR, outG, outB };
        int size = n - 1;
        for (int i = 0; i < m - 1; i += 2) {
            for (int j = 0; j < size; j += 2) {
                outR[i][j] = inR[i][j + 1];
            }
        }
        for (int i = 1; i < m; i += 2) {
            for (int j = 0; j < size; j += 2) {
                outR[i][j] = inR[i - 1][j + 1];
                outR[i][j + 1] = inR[i - 1][j + 1];
            }
        }
        for (int i = 0; i < m - 1; i += 2) {
            for (int j = 0; j < size; j += 2) {
                outB[i][j] = inB[i + 1][j];
                outB[i][j + 1] = inB[i + 1][j];
            }
        }
        for (int i = 1; i < m; i += 2) {
            for (int j = 0; j < size; j += 2) {
                outB[i][j + 1] = inB[i][j];
            }
        }
        switch(direction) {
            case Upper:
                for (int i = 1; i < m; i += 2) {
                    for (int j = 0; j < size; j += 2) {
                        outG[i][j] = inG[i - 1][j];
                    }
                }
                for (int i = 2; i < m - 1; i += 2) {
                    for (int j = 0; j < size; j += 2) {
                        outG[i][j + 1] = inG[i - 1][j + 1];
                    }
                }
                for (int i = 1; i < n; i += 2) {
                    outG[0][i] = inG[1][i];
                }
                break;
            case Lower:
                break;
            case Left:
                break;
            case Right:
                break;
        }
        return out;
    }

    public static final DoubleImage variableNumberGradientsMethod(DoubleImage image) {
        int m = image.getHeight();
        int n = image.getWidth();
        DoubleImage out = (DoubleImage) image.clone();
        double k1 = 1.5;
        double k2 = 0.5;
        for (int i = 2; i < m - 3; i += 2) {
            for (int j = 3; j < n - 2; j += 2) {
                double[] gra = gradientsRB(i, j, image, 0, 2);
                double gramax = Maths.max(gra);
                double gramin = Maths.min(gra);
                double T = k1 * gramin + k2 * (gramax - gramin);
                int[] ind = Matlab.find(Matlab.less(gra, T));
                double[] Rave = new double[8];
                double[] Gave = new double[8];
                double[] Bave = new double[8];
                double[][] ave = new double[][] { Rave, Gave, Bave };
                determineThresholdsRB(i, j, image, ave, ind, 0, 2);
                double Rsum = Maths.sum(Rave);
                double Gsum = Maths.sum(Gave);
                double Bsum = Maths.sum(Bave);
                out.setSubPixel1(j, i, image.getSubPixel0(j, i) + (Gsum - Rsum) / ind.length);
                out.setSubPixel2(j, i, image.getSubPixel0(j, i) + (Bsum - Rsum) / ind.length);
            }
        }
        for (int i = 3; i < m - 2; i += 2) {
            for (int j = 2; j < n - 3; j += 2) {
                double[] gra = gradientsRB(i, j, image, 2, 0);
                double gramax = Maths.max(gra);
                double gramin = Maths.min(gra);
                double T = k1 * gramin + k2 * (gramax - gramin);
                int[] ind = Matlab.find(Matlab.less(gra, T));
                double[] Rave = new double[8];
                double[] Gave = new double[8];
                double[] Bave = new double[8];
                double[][] ave = new double[][] { Rave, Gave, Bave };
                determineThresholdsRB(i, j, image, ave, ind, 2, 0);
                double Rsum = Maths.sum(Rave);
                double Gsum = Maths.sum(Gave);
                double Bsum = Maths.sum(Bave);
                out.setSubPixel1(j, i, image.getSubPixel2(j, i) + (Gsum - Bsum) / ind.length);
                out.setSubPixel0(j, i, image.getSubPixel2(j, i) + (Rsum - Bsum) / ind.length);
            }
        }
        for (int i = 2; i < m - 3; i += 2) {
            for (int j = 2; j < n - 3; j += 2) {
                double[] gra = gradientsG(i, j, image, 0, 2);
                double gramax = Maths.max(gra);
                double gramin = Maths.min(gra);
                double T = k1 * gramin + k2 * (gramax - gramin);
                int[] ind = Matlab.find(Matlab.less(gra, T));
                double[] Rave = new double[8];
                double[] Gave = new double[8];
                double[] Bave = new double[8];
                double[][] ave = new double[][] { Rave, Gave, Bave };
                determineThresholdsG(i, j, image, ave, ind, 0, 2);
                double Rsum = Maths.sum(Rave);
                double Gsum = Maths.sum(Gave);
                double Bsum = Maths.sum(Bave);
                out.setSubPixel0(j, i, image.getSubPixel1(j, i) + (Rsum - Gsum) / ind.length);
                out.setSubPixel2(j, i, image.getSubPixel1(j, i) + (Bsum - Gsum) / ind.length);
            }
        }
        for (int i = 3; i < m - 2; i += 2) {
            for (int j = 3; j < n - 2; j += 2) {
                double[] gra = gradientsG(i, j, image, 2, 0);
                double gramax = Maths.max(gra);
                double gramin = Maths.min(gra);
                double T = k1 * gramin + k2 * (gramax - gramin);
                int[] ind = Matlab.find(Matlab.less(gra, T));
                double[] Rave = new double[8];
                double[] Gave = new double[8];
                double[] Bave = new double[8];
                double[][] ave = new double[][] { Rave, Gave, Bave };
                determineThresholdsG(i, j, image, ave, ind, 2, 0);
                double Rsum = Maths.sum(Rave);
                double Gsum = Maths.sum(Gave);
                double Bsum = Maths.sum(Bave);
                out.setSubPixel0(j, i, image.getSubPixel1(j, i) + (Rsum - Gsum) / ind.length);
                out.setSubPixel2(j, i, image.getSubPixel1(j, i) + (Bsum - Gsum) / ind.length);
            }
        }
        out.rationalize();
        return out;
    }

    public static final double[][][] variableNumberGradientsMethod(double[][][] image) {
        int m = image[0].length;
        int n = image[0][0].length;
        double[][] inR = image[0];
        double[][] inG = image[1];
        double[][] inB = image[2];
        double[][] outR = DoubleArray.copy(inR);
        double[][] outG = DoubleArray.copy(inG);
        double[][] outB = DoubleArray.copy(inB);
        double[][][] out = new double[][][] { outR, outG, outB };
        double k1 = 1.5;
        double k2 = 0.5;
        for (int i = 2; i < m - 3; i += 2) {
            for (int j = 3; j < n - 2; j += 2) {
                double[] gra = gradientsRB(i, j, inR, inG, inB);
                double gramax = Maths.max(gra);
                double gramin = Maths.min(gra);
                double T = k1 * gramin + k2 * (gramax - gramin);
                int[] ind = Matlab.find(Matlab.less(gra, T));
                double[] Rave = new double[8];
                double[] Gave = new double[8];
                double[] Bave = new double[8];
                determineThresholdsRB(i, j, inR, inG, inB, Rave, Gave, Bave, ind);
                double Rsum = Maths.sum(Rave);
                double Gsum = Maths.sum(Gave);
                double Bsum = Maths.sum(Bave);
                outG[i][j] = inR[i][j] + (Gsum - Rsum) / ind.length;
                outB[i][j] = inR[i][j] + (Bsum - Rsum) / ind.length;
            }
        }
        for (int i = 3; i < m - 2; i += 2) {
            for (int j = 2; j < n - 3; j += 2) {
                double[] gra = gradientsRB(i, j, inB, inG, inR);
                double gramax = Maths.max(gra);
                double gramin = Maths.min(gra);
                double T = k1 * gramin + k2 * (gramax - gramin);
                int[] ind = Matlab.find(Matlab.less(gra, T));
                double[] Rave = new double[8];
                double[] Gave = new double[8];
                double[] Bave = new double[8];
                determineThresholdsRB(i, j, inB, inG, inR, Bave, Gave, Rave, ind);
                double Rsum = Maths.sum(Rave);
                double Gsum = Maths.sum(Gave);
                double Bsum = Maths.sum(Bave);
                outG[i][j] = inB[i][j] + (Gsum - Bsum) / ind.length;
                outR[i][j] = inB[i][j] + (Rsum - Bsum) / ind.length;
            }
        }
        for (int i = 2; i < m - 3; i += 2) {
            for (int j = 2; j < n - 3; j += 2) {
                double[] gra = gradientsG(i, j, inR, inG, inB);
                double gramax = Maths.max(gra);
                double gramin = Maths.min(gra);
                double T = k1 * gramin + k2 * (gramax - gramin);
                int[] ind = Matlab.find(Matlab.less(gra, T));
                double[] Rave = new double[8];
                double[] Gave = new double[8];
                double[] Bave = new double[8];
                determineThresholdsG(i, j, inR, inG, inB, Rave, Gave, Bave, ind);
                double Rsum = Maths.sum(Rave);
                double Gsum = Maths.sum(Gave);
                double Bsum = Maths.sum(Bave);
                outR[i][j] = inG[i][j] + (Rsum - Gsum) / ind.length;
                outB[i][j] = inG[i][j] + (Bsum - Gsum) / ind.length;
            }
        }
        for (int i = 3; i < m - 2; i += 2) {
            for (int j = 3; j < n - 2; j += 2) {
                double[] gra = gradientsG(i, j, inB, inG, inR);
                double gramax = Maths.max(gra);
                double gramin = Maths.min(gra);
                double T = k1 * gramin + k2 * (gramax - gramin);
                int[] ind = Matlab.find(Matlab.less(gra, T));
                double[] Rave = new double[8];
                double[] Gave = new double[8];
                double[] Bave = new double[8];
                determineThresholdsG(i, j, inB, inG, inR, Bave, Gave, Rave, ind);
                double Rsum = Maths.sum(Rave);
                double Gsum = Maths.sum(Gave);
                double Bsum = Maths.sum(Bave);
                outR[i][j] = inG[i][j] + (Rsum - Gsum) / ind.length;
                outB[i][j] = inG[i][j] + (Bsum - Gsum) / ind.length;
            }
        }
        rationalizeRGB(out);
        return out;
    }

    protected static final void rationalizeRGB(double[][][] image) {
        int ch = image.length;
        int m = image[0].length;
        int n = image[0][0].length;
        for (int x = 0; x < ch; x++) {
            for (int h = 0; h < m; h++) {
                for (int w = 0; w < n; w++) {
                    image[x][h][w] = image[x][h][w] > 4095 ? 4095 : image[x][h][w];
                    image[x][h][w] = image[x][h][w] < 0 ? 0 : image[x][h][w];
                }
            }
        }
    }

    protected static final double[] gradientsRB(int i, int j, double[][] in0, double[][] inG, double[][] in2) {
        double gra_N = Math.abs(inG[i - 1][j] - inG[i + 1][j]) + Math.abs(in0[i - 2][j] - in0[i][j]) + 1. / 2. * Math.abs(in2[i - 1][j - 1] - in2[i + 1][j - 1]) + 1. / 2. * Math.abs(in2[i - 1][j + 1] - in2[i + 1][j + 1]) + 1. / 2. * Math.abs(inG[i - 2][j - 1] - inG[i][j - 1]) + 1. / 2. * Math.abs(inG[i - 2][j + 1] - inG[i][j + 1]);
        double gra_E = Math.abs(inG[i][j + 1] - inG[i][j - 1]) + Math.abs(in0[i][j + 2] - in0[i][j]) + 1. / 2. * Math.abs(in2[i - 1][j + 1] - in2[i - 1][j - 1]) + 1. / 2. * Math.abs(in2[i + 1][j + 1] - in2[i + 1][j - 1]) + 1. / 2. * Math.abs(inG[i - 1][j + 2] - inG[i - 1][j]) + 1. / 2. * Math.abs(inG[i + 1][j + 2] - inG[i + 1][j]);
        double gra_S = Math.abs(inG[i + 1][j] - inG[i - 1][j]) + Math.abs(in0[i + 2][j] - in0[i][j]) + 1. / 2. * Math.abs(in2[i + 1][j + 1] - in2[i - 1][j + 1]) + 1. / 2. * Math.abs(in2[i + 1][j - 1] - in2[i - 1][j - 1]) + 1. / 2. * Math.abs(inG[i + 2][j + 1] - inG[i][j + 1]) + 1. / 2. * Math.abs(inG[i + 2][j - 1] - inG[i][j - 1]);
        double gra_W = Math.abs(inG[i][j - 1] - inG[i][j + 1]) + Math.abs(in0[i][j - 2] - in0[i][j]) + 1. / 2. * Math.abs(in2[i + 1][j - 1] - in2[i + 1][j + 1]) + 1. / 2. * Math.abs(in2[i - 1][j - 1] - in2[i - 1][j + 1]) + 1. / 2. * Math.abs(inG[i + 1][j - 2] - inG[i + 1][j]) + 1. / 2. * Math.abs(inG[i - 1][j - 2] - inG[i - 1][j]);
        double gra_NE = Math.abs(in2[i - 1][j + 1] - in2[i + 1][j - 1]) + Math.abs(in0[i - 2][j + 2] - in0[i][j]) + 1. / 2. * Math.abs(inG[i - 1][j] - inG[i][j - 1]) + 1. / 2. * Math.abs(inG[i][j + 1] - inG[i + 1][j]) + 1. / 2. * Math.abs(inG[i - 2][j + 1] - inG[i - 1][j]) + 1. / 2. * Math.abs(inG[i - 1][j + 2] - inG[i][j + 1]);
        double gra_SE = Math.abs(in2[i + 1][j + 1] - in2[i - 1][j - 1]) + Math.abs(in0[i + 2][j + 2] - in0[i][j]) + 1. / 2. * Math.abs(inG[i][j + 1] - inG[i - 1][j]) + 1. / 2. * Math.abs(inG[i + 1][j] - inG[i][j - 1]) + 1. / 2. * Math.abs(inG[i + 1][j + 2] - inG[i][j + 1]) + 1. / 2. * Math.abs(inG[i + 2][j + 1] - inG[i + 1][j]);
        double gra_NW = Math.abs(in2[i - 1][j - 1] - in2[i + 1][j + 1]) + Math.abs(in0[i - 2][j - 2] - in0[i][j]) + 1. / 2. * Math.abs(inG[i - 1][j] - inG[i][j + 1]) + 1. / 2. * Math.abs(inG[i][j - 1] - inG[i + 1][j]) + 1. / 2. * Math.abs(inG[i - 2][j - 1] - inG[i - 1][j]) + 1. / 2. * Math.abs(inG[i - 1][j - 2] - inG[i][j - 1]);
        double gra_SW = Math.abs(in2[i + 1][j - 1] - in2[i - 1][j + 1]) + Math.abs(in0[i + 2][j - 2] - in0[i][j]) + 1. / 2. * Math.abs(inG[i + 1][j] - inG[i][j + 1]) + 1. / 2. * Math.abs(inG[i][j - 1] - inG[i - 1][j]) + 1. / 2. * Math.abs(inG[i + 2][j - 1] - inG[i + 1][j]) + 1. / 2. * Math.abs(inG[i + 1][j - 2] - inG[i][j - 1]);
        double[] gra = new double[] { gra_N, gra_E, gra_S, gra_W, gra_NE, gra_SE, gra_NW, gra_SW };
        return gra;
    }

    protected static final double[] gradientsRB(int i, int j, DoubleImage im, int ch0Index, int ch2Index) {
        double gra_N = Math.abs(im.getSubPixel1(j, i - 1) - im.getSubPixel1(j, i + 1)) + Math.abs(im.getPixel(j, i - 2, ch0Index) - im.getPixel(j, i, ch0Index)) + 1. / 2. * Math.abs(im.getPixel(j - 1, i - 1, ch2Index) - im.getPixel(j - 1, i + 1, ch2Index)) + 1. / 2. * Math.abs(im.getPixel(j + 1, i - 1, ch2Index) - im.getPixel(j + 1, i + 1, ch2Index)) + 1. / 2. * Math.abs(im.getSubPixel1(j - 1, i - 2) - im.getSubPixel1(j - 1, i)) + 1. / 2. * Math.abs(im.getSubPixel1(j + 1, i - 2) - im.getSubPixel1(j + 1, i));
        double gra_E = Math.abs(im.getSubPixel1(j + 1, i) - im.getSubPixel1(j - 1, i)) + Math.abs(im.getPixel(j + 2, i, ch0Index) - im.getPixel(j, i, ch0Index)) + 1. / 2. * Math.abs(im.getPixel(j + 1, i - 1, ch2Index) - im.getPixel(j - 1, i - 1, ch2Index)) + 1. / 2. * Math.abs(im.getPixel(j + 1, i + 1, ch2Index) - im.getPixel(j - 1, i + 1, ch2Index)) + 1. / 2. * Math.abs(im.getSubPixel1(j + 2, i - 1) - im.getSubPixel1(j, i - 1)) + 1. / 2. * Math.abs(im.getSubPixel1(j + 2, i + 1) - im.getSubPixel1(j, i + 1));
        double gra_S = Math.abs(im.getSubPixel1(j, i + 1) - im.getSubPixel1(j, i - 1)) + Math.abs(im.getPixel(j, i + 2, ch0Index) - im.getPixel(j, i, ch0Index)) + 1. / 2. * Math.abs(im.getPixel(j + 1, i + 1, ch2Index) - im.getPixel(j + 1, i - 1, ch2Index)) + 1. / 2. * Math.abs(im.getPixel(j - 1, i + 1, ch2Index) - im.getPixel(j - 1, i - 1, ch2Index)) + 1. / 2. * Math.abs(im.getSubPixel1(j + 1, i + 2) - im.getSubPixel1(j + 1, i)) + 1. / 2. * Math.abs(im.getSubPixel1(j - 1, i + 2) - im.getSubPixel1(j - 1, i));
        double gra_W = Math.abs(im.getSubPixel1(j - 1, i) - im.getSubPixel1(j + 1, i)) + Math.abs(im.getPixel(j - 2, i, ch0Index) - im.getPixel(j, i, ch0Index)) + 1. / 2. * Math.abs(im.getPixel(j - 1, i + 1, ch2Index) - im.getPixel(j + 1, i + 1, ch2Index)) + 1. / 2. * Math.abs(im.getPixel(j - 1, i - 1, ch2Index) - im.getPixel(j + 1, i - 1, ch2Index)) + 1. / 2. * Math.abs(im.getSubPixel1(j - 2, i + 1) - im.getSubPixel1(j, i + 1)) + 1. / 2. * Math.abs(im.getSubPixel1(j - 2, i - 1) - im.getSubPixel1(j, i - 1));
        double gra_NE = Math.abs(im.getPixel(j + 1, i - 1, ch2Index) - im.getPixel(j - 1, i + 1, ch2Index)) + Math.abs(im.getPixel(j + 2, i - 2, ch0Index) - im.getPixel(j, i, ch0Index)) + 1. / 2. * Math.abs(im.getSubPixel1(j, i - 1) - im.getSubPixel1(j - 1, i)) + 1. / 2. * Math.abs(im.getSubPixel1(j + 1, i) - im.getSubPixel1(j, i + 1)) + 1. / 2. * Math.abs(im.getSubPixel1(j + 1, i - 2) - im.getSubPixel1(j, i - 1)) + 1. / 2. * Math.abs(im.getSubPixel1(j + 2, i - 1) - im.getSubPixel1(j + 1, i));
        double gra_SE = Math.abs(im.getPixel(j + 1, i + 1, ch2Index) - im.getPixel(j - 1, i - 1, ch2Index)) + Math.abs(im.getPixel(j + 2, i + 2, ch0Index) - im.getPixel(j, i, ch0Index)) + 1. / 2. * Math.abs(im.getSubPixel1(j + 1, i) - im.getSubPixel1(j, i - 1)) + 1. / 2. * Math.abs(im.getSubPixel1(j, i + 1) - im.getSubPixel1(j - 1, i)) + 1. / 2. * Math.abs(im.getSubPixel1(j + 2, i + 1) - im.getSubPixel1(j + 1, i)) + 1. / 2. * Math.abs(im.getSubPixel1(j + 1, i + 2) - im.getSubPixel1(j, i + 1));
        double gra_NW = Math.abs(im.getPixel(j - 1, i - 1, ch2Index) - im.getPixel(j + 1, i + 1, ch2Index)) + Math.abs(im.getPixel(j - 2, i - 2, ch0Index) - im.getPixel(j, i, ch0Index)) + 1. / 2. * Math.abs(im.getSubPixel1(j, i - 1) - im.getSubPixel1(j + 1, i)) + 1. / 2. * Math.abs(im.getSubPixel1(j - 1, i) - im.getSubPixel1(j, i + 1)) + 1. / 2. * Math.abs(im.getSubPixel1(j - 1, i - 2) - im.getSubPixel1(j, i - 1)) + 1. / 2. * Math.abs(im.getSubPixel1(j - 2, i - 1) - im.getSubPixel1(j - 1, i));
        double gra_SW = Math.abs(im.getPixel(j - 1, i + 1, ch2Index) - im.getPixel(j + 1, i - 1, ch2Index)) + Math.abs(im.getPixel(j - 2, i + 2, ch0Index) - im.getPixel(j, i, ch0Index)) + 1. / 2. * Math.abs(im.getSubPixel1(j, i + 1) - im.getSubPixel1(j + 1, i)) + 1. / 2. * Math.abs(im.getSubPixel1(j - 1, i) - im.getSubPixel1(j, i - 1)) + 1. / 2. * Math.abs(im.getSubPixel1(j - 1, i + 2) - im.getSubPixel1(j, i + 1)) + 1. / 2. * Math.abs(im.getSubPixel1(j - 2, i + 1) - im.getSubPixel1(j - 1, i));
        double[] gra = new double[] { gra_N, gra_E, gra_S, gra_W, gra_NE, gra_SE, gra_NW, gra_SW };
        return gra;
    }

    protected static final double[] gradientsG(int i, int j, double[][] in0, double[][] inG, double[][] in2) {
        double gra_N = Math.abs(in2[i - 1][j] - in2[i + 1][j]) + Math.abs(inG[i - 2][j] - inG[i][j]) + 1. / 2. * Math.abs(inG[i - 1][j - 1] - inG[i + 1][j - 1]) + 1. / 2. * Math.abs(inG[i - 1][j + 1] - inG[i + 1][j + 1]) + 1. / 2. * Math.abs(in0[i - 2][j - 1] - in0[i][j - 1]) + 1. / 2. * Math.abs(in0[i - 2][j + 1] - in0[i][j + 1]);
        double gra_E = Math.abs(in0[i][j + 1] - in0[i][j - 1]) + Math.abs(inG[i][j + 2] - inG[i][j]) + 1. / 2. * Math.abs(inG[i - 1][j + 1] - inG[i - 1][j - 1]) + 1. / 2. * Math.abs(inG[i + 1][j + 1] - inG[i + 1][j - 1]) + 1. / 2. * Math.abs(in2[i - 1][j + 2] - in2[i - 1][j]) + 1. / 2. * Math.abs(in2[i + 1][j + 2] - in2[i + 1][j]);
        double gra_S = Math.abs(in2[i + 1][j] - in2[i - 1][j]) + Math.abs(inG[i + 2][j] - inG[i][j]) + 1. / 2. * Math.abs(inG[i + 1][j + 1] - inG[i - 1][j + 1]) + 1. / 2. * Math.abs(inG[i + 1][j - 1] - inG[i - 1][j - 1]) + 1. / 2. * Math.abs(in0[i + 2][j + 1] - in0[i][j + 1]) + 1. / 2. * Math.abs(in0[i + 2][j - 1] - in0[i][j - 1]);
        double gra_W = Math.abs(in0[i][j - 1] - in0[i][j + 1]) + Math.abs(inG[i][j - 2] - inG[i][j]) + 1. / 2. * Math.abs(inG[i + 1][j - 1] - inG[i + 1][j + 1]) + 1. / 2. * Math.abs(inG[i - 1][j - 1] - inG[i - 1][j + 1]) + 1. / 2. * Math.abs(in2[i + 1][j - 2] - in2[i + 1][j]) + 1. / 2. * Math.abs(in2[i - 1][j - 2] - in2[i - 1][j]);
        double gra_NE = Math.abs(inG[i - 1][j + 1] - inG[i + 1][j - 1]) + Math.abs(inG[i - 2][j + 2] - inG[i][j]) + Math.abs(in0[i - 2][j + 1] - in0[i][j - 1]) + Math.abs(in2[i - 1][j + 2] - in2[i + 1][j]);
        double gra_SE = Math.abs(inG[i + 1][j + 1] - inG[i - 1][j - 1]) + Math.abs(inG[i + 2][j + 2] - inG[i][j]) + Math.abs(in2[i + 1][j + 2] - in2[i - 1][j]) + Math.abs(in0[i + 2][j + 1] - in0[i][j - 1]);
        double gra_NW = Math.abs(inG[i - 1][j - 1] - inG[i + 1][j + 1]) + Math.abs(inG[i - 2][j - 2] - inG[i][j]) + Math.abs(in0[i - 2][j - 1] - in0[i][j + 1]) + Math.abs(in2[i - 1][j - 2] - in2[i + 1][j]);
        double gra_SW = Math.abs(inG[i + 1][j - 1] - inG[i - 1][j + 1]) + Math.abs(inG[i + 2][j - 2] - inG[i][j]) + Math.abs(in0[i + 2][j - 1] - in0[i][j + 1]) + Math.abs(in2[i + 1][j - 2] - inG[i - 1][j]);
        double[] gra = new double[] { gra_N, gra_E, gra_S, gra_W, gra_NE, gra_SE, gra_NW, gra_SW };
        return gra;
    }

    protected static final double[] gradientsG(int i, int j, DoubleImage im, int ch0Index, int ch2Index) {
        double gra_N = Math.abs(im.getPixel(j, i - 1, ch2Index) - im.getPixel(j, i + 1, ch2Index)) + Math.abs(im.getSubPixel1(j, i - 2) - im.getSubPixel1(j, i)) + 1. / 2. * Math.abs(im.getSubPixel1(j - 1, i - 1) - im.getSubPixel1(j - 1, i + 1)) + 1. / 2. * Math.abs(im.getSubPixel1(j + 1, i - 1) - im.getSubPixel1(j + 1, i + 1)) + 1. / 2. * Math.abs(im.getPixel(j - 1, i - 2, ch0Index) - im.getPixel(j - 1, i, ch0Index)) + 1. / 2. * Math.abs(im.getPixel(j + 1, i - 2, ch0Index) - im.getPixel(j + 1, i, ch0Index));
        double gra_E = Math.abs(im.getPixel(j + 1, i, ch0Index) - im.getPixel(j - 1, i, ch0Index)) + Math.abs(im.getSubPixel1(j + 2, i) - im.getSubPixel1(j, i)) + 1. / 2. * Math.abs(im.getSubPixel1(j + 1, i - 1) - im.getSubPixel1(j - 1, i - 1)) + 1. / 2. * Math.abs(im.getSubPixel1(j + 1, i + 1) - im.getSubPixel1(j - 1, i + 1)) + 1. / 2. * Math.abs(im.getPixel(j + 2, i - 1, ch2Index) - im.getPixel(j, i - 1, ch2Index)) + 1. / 2. * Math.abs(im.getPixel(j + 2, i + 1, ch2Index) - im.getPixel(j, i + 1, ch2Index));
        double gra_S = Math.abs(im.getPixel(j, i + 1, ch2Index) - im.getPixel(j, i - 1, ch2Index)) + Math.abs(im.getSubPixel1(j, i + 2) - im.getSubPixel1(j, i)) + 1. / 2. * Math.abs(im.getSubPixel1(j + 1, i + 1) - im.getSubPixel1(j + 1, i - 1)) + 1. / 2. * Math.abs(im.getSubPixel1(j - 1, i + 1) - im.getSubPixel1(j - 1, i - 1)) + 1. / 2. * Math.abs(im.getPixel(j + 1, i + 2, ch0Index) - im.getPixel(j + 1, i, ch0Index)) + 1. / 2. * Math.abs(im.getPixel(j - 1, i + 2, ch0Index) - im.getPixel(j - 1, i, ch0Index));
        double gra_W = Math.abs(im.getPixel(j - 1, i, ch0Index) - im.getPixel(j + 1, i, ch0Index)) + Math.abs(im.getSubPixel1(j - 2, i) - im.getSubPixel1(j, i)) + 1. / 2. * Math.abs(im.getSubPixel1(j - 1, i + 1) - im.getSubPixel1(j + 1, i + 1)) + 1. / 2. * Math.abs(im.getSubPixel1(j - 1, i - 1) - im.getSubPixel1(j + 1, i - 1)) + 1. / 2. * Math.abs(im.getPixel(j - 2, i + 1, ch2Index) - im.getPixel(j, i + 1, ch2Index)) + 1. / 2. * Math.abs(im.getPixel(j - 2, i - 1, ch2Index) - im.getPixel(j, i - 1, ch2Index));
        double gra_NE = Math.abs(im.getSubPixel1(j + 1, i - 1) - im.getSubPixel1(j - 1, i + 1)) + Math.abs(im.getSubPixel1(j + 2, i - 2) - im.getSubPixel1(j, i)) + Math.abs(im.getPixel(j + 1, i - 2, ch0Index) - im.getPixel(j - 1, i, ch0Index)) + Math.abs(im.getPixel(j + 2, i - 1, ch2Index) - im.getPixel(j, i + 1, ch2Index));
        double gra_SE = Math.abs(im.getSubPixel1(j + 1, i + 1) - im.getSubPixel1(j - 1, i - 1)) + Math.abs(im.getSubPixel1(j + 2, i + 2) - im.getSubPixel1(j, i)) + Math.abs(im.getPixel(j + 2, i + 1, ch2Index) - im.getPixel(j, i - 1, ch2Index)) + Math.abs(im.getPixel(j + 1, i + 2, ch0Index) - im.getPixel(j - 1, i, ch0Index));
        double gra_NW = Math.abs(im.getSubPixel1(j - 1, i - 1) - im.getSubPixel1(j + 1, i + 1)) + Math.abs(im.getSubPixel1(j - 2, i - 2) - im.getSubPixel1(j, i)) + Math.abs(im.getPixel(j - 1, i - 2, ch0Index) - im.getPixel(j + 1, i, ch0Index)) + Math.abs(im.getPixel(j - 2, i - 1, ch2Index) - im.getPixel(j, i + 1, ch2Index));
        double gra_SW = Math.abs(im.getSubPixel1(j - 1, i + 1) - im.getSubPixel1(j + 1, i - 1)) + Math.abs(im.getSubPixel1(j - 2, i + 2) - im.getSubPixel1(j, i)) + Math.abs(im.getPixel(j - 1, i + 2, ch0Index) - im.getPixel(j + 1, i, ch0Index)) + Math.abs(im.getPixel(j - 2, i + 1, ch2Index) - im.getSubPixel1(j, i - 1));
        double[] gra = new double[] { gra_N, gra_E, gra_S, gra_W, gra_NE, gra_SE, gra_NW, gra_SW };
        return gra;
    }

    /**
   *
   * @param image IntegerImage
   * @return IntegerImage
   * @deprecated
   */
    public static final IntegerImage variableNumberGradientsMethod(IntegerImage image) {
        int m = image.getHeight();
        int n = image.getWidth();
        IntegerImage out = (IntegerImage) image.clone();
        double k1 = 1.5;
        double k2 = 0.5;
        for (int i = 2; i < m - 3; i += 2) {
            for (int j = 3; j < n - 2; j += 2) {
                double[] gra = gradientsRB(i, j, image, 0, 2);
                double gramax = Maths.max(gra);
                double gramin = Maths.min(gra);
                double T = k1 * gramin + k2 * (gramax - gramin);
                int[] ind = Matlab.find(Matlab.less(gra, T));
                double[] Rave = new double[8];
                double[] Gave = new double[8];
                double[] Bave = new double[8];
                double[][] ave = new double[][] { Rave, Gave, Bave };
                determineThresholdsRB(i, j, image, ave, ind, 0, 2);
                double Rsum = Maths.sum(Rave);
                double Gsum = Maths.sum(Gave);
                double Bsum = Maths.sum(Bave);
                out.setPixelG(j, i, image.getPixelR(j, i) + (Gsum - Rsum) / ind.length);
                out.setPixelB(j, i, image.getPixelR(j, i) + (Bsum - Rsum) / ind.length);
            }
        }
        for (int i = 3; i < m - 2; i += 2) {
            for (int j = 2; j < n - 3; j += 2) {
                double[] gra = gradientsRB(i, j, image, 2, 0);
                double gramax = Maths.max(gra);
                double gramin = Maths.min(gra);
                double T = k1 * gramin + k2 * (gramax - gramin);
                int[] ind = Matlab.find(Matlab.less(gra, T));
                double[] Rave = new double[8];
                double[] Gave = new double[8];
                double[] Bave = new double[8];
                double[][] ave = new double[][] { Rave, Gave, Bave };
                determineThresholdsRB(i, j, image, ave, ind, 2, 0);
                double Rsum = Maths.sum(Rave);
                double Gsum = Maths.sum(Gave);
                double Bsum = Maths.sum(Bave);
                out.setPixelG(j, i, image.getPixelB(j, i) + (Gsum - Bsum) / ind.length);
                out.setPixelR(j, i, image.getPixelB(j, i) + (Rsum - Bsum) / ind.length);
            }
        }
        for (int i = 2; i < m - 3; i += 2) {
            for (int j = 2; j < n - 3; j += 2) {
                double[] gra = gradientsG(i, j, image, 0, 2);
                double gramax = Maths.max(gra);
                double gramin = Maths.min(gra);
                double T = k1 * gramin + k2 * (gramax - gramin);
                int[] ind = Matlab.find(Matlab.less(gra, T));
                double[] Rave = new double[8];
                double[] Gave = new double[8];
                double[] Bave = new double[8];
                double[][] ave = new double[][] { Rave, Gave, Bave };
                determineThresholdsG(i, j, image, ave, ind, 0, 2);
                double Rsum = Maths.sum(Rave);
                double Gsum = Maths.sum(Gave);
                double Bsum = Maths.sum(Bave);
                out.setPixelR(j, i, image.getPixelG(j, i) + (Rsum - Gsum) / ind.length);
                out.setPixelB(j, i, image.getPixelG(j, i) + (Bsum - Gsum) / ind.length);
            }
        }
        for (int i = 3; i < m - 2; i += 2) {
            for (int j = 3; j < n - 2; j += 2) {
                double[] gra = gradientsG(i, j, image, 2, 0);
                double gramax = Maths.max(gra);
                double gramin = Maths.min(gra);
                double T = k1 * gramin + k2 * (gramax - gramin);
                int[] ind = Matlab.find(Matlab.less(gra, T));
                double[] Rave = new double[8];
                double[] Gave = new double[8];
                double[] Bave = new double[8];
                double[][] ave = new double[][] { Rave, Gave, Bave };
                determineThresholdsG(i, j, image, ave, ind, 2, 0);
                double Rsum = Maths.sum(Rave);
                double Gsum = Maths.sum(Gave);
                double Bsum = Maths.sum(Bave);
                out.setPixelR(j, i, image.getPixelG(j, i) + (Rsum - Gsum) / ind.length);
                out.setPixelB(j, i, image.getPixelG(j, i) + (Bsum - Gsum) / ind.length);
            }
        }
        return out;
    }

    protected static final void determineThresholdsG(int i, int j, IntegerImage im, double[][] ave, int[] ind, int ch0index, int ch2index) {
        for (int k = 0; k < ind.length; k++) {
            switch(ind[k]) {
                case 0:
                    ave[1][0] = 1. / 2. * (im.getPixelG(j, i) + im.getPixelG(j, i - 2));
                    ave[ch2index][0] = im.getPixel(j, i - 1, ch2index);
                    ave[ch0index][0] = 1. / 4. * (im.getPixel(j - 1, i - 2, ch0index) + im.getPixel(j + 1, i - 2, ch0index) + im.getPixel(j - 1, i, ch0index) + im.getPixel(j + 1, i, ch0index));
                    break;
                case 1:
                    ave[1][1] = 1. / 2. * (im.getPixelG(j, i) + im.getPixelG(j + 2, i));
                    ave[ch0index][1] = im.getPixel(j + 1, i, ch0index);
                    ave[ch2index][1] = 1. / 4. * (im.getPixel(j, i - 1, ch2index) + im.getPixel(j, i + 1, ch2index) + im.getPixel(j + 2, i - 1, ch2index) + im.getPixel(j + 2, i + 1, ch2index));
                    break;
                case 2:
                    ave[1][2] = 1. / 2. * (im.getPixelG(j, i) + im.getPixelG(j, i + 2));
                    ave[ch2index][2] = im.getPixel(j, i + 1, ch2index);
                    ave[ch0index][2] = 1. / 4. * (im.getPixel(j - 1, i, ch0index) + im.getPixel(j + 1, i, ch0index) + im.getPixel(j - 1, i + 2, ch0index) + im.getPixel(j + 1, i + 2, ch0index));
                    break;
                case 3:
                    ave[1][3] = 1. / 2. * (im.getPixelG(j, i) + im.getPixelG(j - 2, i));
                    ave[ch0index][3] = im.getPixel(j - 1, i, ch0index);
                    ave[ch2index][3] = 1. / 4. * (im.getPixel(j - 2, i - 1, ch2index) + im.getPixel(j, i - 1, ch2index) + im.getPixel(j - 2, i + 1, ch2index) + im.getPixel(j, i + 1, ch2index));
                    break;
                case 4:
                    ave[ch0index][4] = 1. / 2. * (im.getPixel(j + 1, i - 2, ch0index) + im.getPixel(j + 1, i, ch0index));
                    ave[ch2index][4] = 1. / 2. * (im.getPixel(j, i - 1, ch2index) + im.getPixel(j + 2, i - 1, ch2index));
                    ave[1][4] = im.getPixelG(j + 1, i - 1);
                    break;
                case 5:
                    ave[ch0index][5] = 1. / 2. * (im.getPixel(j + 1, i, ch0index) + im.getPixel(j + 1, i + 2, ch0index));
                    ave[ch2index][5] = 1. / 2. * (im.getPixel(j, i + 1, ch2index) + im.getPixel(j + 2, i + 1, ch2index));
                    ave[1][5] = im.getPixelG(j + 1, i + 1);
                    break;
                case 6:
                    ave[ch0index][6] = 1. / 2. * (im.getPixel(j - 1, i, ch0index) + im.getPixel(j - 1, i - 2, ch0index));
                    ave[ch2index][6] = 1. / 2. * (im.getPixel(j - 2, i - 1, ch2index) + im.getPixel(j, i - 1, ch2index));
                    ave[1][6] = im.getPixelG(j - 1, i - 1);
                    break;
                case 7:
                    ave[ch0index][7] = 1. / 2. * (im.getPixel(j - 1, i, ch0index) + im.getPixel(j - 1, i + 2, ch0index));
                    ave[ch2index][7] = 1. / 2. * (im.getPixel(j - 2, i + 1, ch2index) + im.getPixel(j, i + 1, ch2index));
                    ave[1][7] = im.getPixelG(j - 1, i + 1);
                    break;
            }
        }
    }

    protected static final void determineThresholdsRB(int i, int j, IntegerImage im, double[][] ave, int[] ind, int ch0Index, int ch2Index) {
        for (int k = 0; k < ind.length; k++) {
            switch(ind[k]) {
                case 0:
                    ave[ch0Index][0] = 1. / 2. * (im.getPixel(j, i, ch0Index) + im.getPixel(j, i - 2, ch0Index));
                    ave[1][0] = im.getPixelG(j, i - 1);
                    ave[ch2Index][0] = 1. / 2. * (im.getPixel(j - 1, i - 1, ch2Index) + im.getPixel(j + 1, i - 1, ch2Index));
                    break;
                case 1:
                    ave[ch0Index][1] = 1. / 2. * (im.getPixel(j, i, ch0Index) + im.getPixel(j + 2, i, ch0Index));
                    ave[1][1] = im.getPixelG(j + 1, i);
                    ave[ch2Index][1] = 1. / 2. * (im.getPixel(j + 1, i - 1, ch2Index) + im.getPixel(j + 1, i + 1, ch2Index));
                    break;
                case 2:
                    ave[ch0Index][2] = 1. / 2. * (im.getPixel(j, i, ch0Index) + im.getPixel(j, i + 2, ch0Index));
                    ave[1][2] = im.getPixelG(j, i + 1);
                    ave[ch2Index][2] = 1. / 2. * (im.getPixel(j - 1, i + 1, ch2Index) + im.getPixel(j + 1, i + 1, ch2Index));
                    break;
                case 3:
                    ave[ch0Index][3] = 1. / 2. * (im.getPixel(j, i, ch0Index) + im.getPixel(j - 2, i, ch0Index));
                    ave[1][3] = im.getPixelG(j - 1, i);
                    ave[ch2Index][3] = 1. / 2. * (im.getPixel(j - 1, i - 1, ch2Index) + im.getPixel(j - 1, i + 1, ch2Index));
                    break;
                case 4:
                    ave[ch0Index][4] = 1. / 2. * (im.getPixel(j, i, ch0Index) + im.getPixel(j + 2, i - 2, ch0Index));
                    ave[1][4] = 1. / 4. * (im.getPixelG(j + 1, i) + im.getPixelG(j + 2, i - 1) + im.getPixelG(j, i - 1) + im.getPixelG(j + 1, i - 2));
                    ave[ch2Index][4] = im.getPixel(j + 1, i - 1, ch2Index);
                    break;
                case 5:
                    ave[ch0Index][5] = 1. / 2. * (im.getPixel(j, i, ch0Index) + im.getPixel(j + 2, i + 2, ch0Index));
                    ave[1][5] = 1. / 4. * (im.getPixelG(j + 1, i) + im.getPixelG(j + 2, i + 1) + im.getPixelG(j, i + 1) + im.getPixelG(j + 1, i + 2));
                    ave[ch2Index][5] = im.getPixel(j + 1, i + 1, ch2Index);
                    break;
                case 6:
                    ave[ch0Index][6] = 1. / 2. * (im.getPixel(j, i, ch0Index) + im.getPixel(j - 2, i - 2, ch0Index));
                    ave[1][6] = 1. / 4. * (im.getPixelG(j - 1, i) + im.getPixelG(j - 2, i - 1) + im.getPixelG(j, i - 1) + im.getPixelG(j - 1, i - 2));
                    ave[ch2Index][6] = im.getPixel(j - 1, i - 1, ch2Index);
                    break;
                case 7:
                    ave[ch0Index][7] = 1. / 2. * (im.getPixel(j, i, ch0Index) + im.getPixel(j - 2, i + 2, ch0Index));
                    ave[1][7] = 1. / 4. * (im.getPixelG(j - 1, i) + im.getPixelG(j - 2, i + 1) + im.getPixelG(j, i + 1) + im.getPixelG(j - 1, i + 2));
                    ave[ch2Index][7] = im.getPixel(j - 1, i + 1, ch2Index);
                    break;
            }
        }
    }

    protected static final double[] gradientsG(int i, int j, IntegerImage im, int ch0Index, int ch2Index) {
        double gra_N = Math.abs(im.getPixel(j, i - 1, ch2Index) - im.getPixel(j, i + 1, ch2Index)) + Math.abs(im.getPixelG(j, i - 2) - im.getPixelG(j, i)) + 1. / 2. * Math.abs(im.getPixelG(j - 1, i - 1) - im.getPixelG(j - 1, i + 1)) + 1. / 2. * Math.abs(im.getPixelG(j + 1, i - 1) - im.getPixelG(j + 1, i + 1)) + 1. / 2. * Math.abs(im.getPixel(j - 1, i - 2, ch0Index) - im.getPixel(j - 1, i, ch0Index)) + 1. / 2. * Math.abs(im.getPixel(j + 1, i - 2, ch0Index) - im.getPixel(j + 1, i, ch0Index));
        double gra_E = Math.abs(im.getPixel(j + 1, i, ch0Index) - im.getPixel(j - 1, i, ch0Index)) + Math.abs(im.getPixelG(j + 2, i) - im.getPixelG(j, i)) + 1. / 2. * Math.abs(im.getPixelG(j + 1, i - 1) - im.getPixelG(j - 1, i - 1)) + 1. / 2. * Math.abs(im.getPixelG(j + 1, i + 1) - im.getPixelG(j - 1, i + 1)) + 1. / 2. * Math.abs(im.getPixel(j + 2, i - 1, ch2Index) - im.getPixel(j, i - 1, ch2Index)) + 1. / 2. * Math.abs(im.getPixel(j + 2, i + 1, ch2Index) - im.getPixel(j, i + 1, ch2Index));
        double gra_S = Math.abs(im.getPixel(j, i + 1, ch2Index) - im.getPixel(j, i - 1, ch2Index)) + Math.abs(im.getPixelG(j, i + 2) - im.getPixelG(j, i)) + 1. / 2. * Math.abs(im.getPixelG(j + 1, i + 1) - im.getPixelG(j + 1, i - 1)) + 1. / 2. * Math.abs(im.getPixelG(j - 1, i + 1) - im.getPixelG(j - 1, i - 1)) + 1. / 2. * Math.abs(im.getPixel(j + 1, i + 2, ch0Index) - im.getPixel(j + 1, i, ch0Index)) + 1. / 2. * Math.abs(im.getPixel(j - 1, i + 2, ch0Index) - im.getPixel(j - 1, i, ch0Index));
        double gra_W = Math.abs(im.getPixel(j - 1, i, ch0Index) - im.getPixel(j + 1, i, ch0Index)) + Math.abs(im.getPixelG(j - 2, i) - im.getPixelG(j, i)) + 1. / 2. * Math.abs(im.getPixelG(j - 1, i + 1) - im.getPixelG(j + 1, i + 1)) + 1. / 2. * Math.abs(im.getPixelG(j - 1, i - 1) - im.getPixelG(j + 1, i - 1)) + 1. / 2. * Math.abs(im.getPixel(j - 2, i + 1, ch2Index) - im.getPixel(j, i + 1, ch2Index)) + 1. / 2. * Math.abs(im.getPixel(j - 2, i - 1, ch2Index) - im.getPixel(j, i - 1, ch2Index));
        double gra_NE = Math.abs(im.getPixelG(j + 1, i - 1) - im.getPixelG(j - 1, i + 1)) + Math.abs(im.getPixelG(j + 2, i - 2) - im.getPixelG(j, i)) + Math.abs(im.getPixel(j + 1, i - 2, ch0Index) - im.getPixel(j - 1, i, ch0Index)) + Math.abs(im.getPixel(j + 2, i - 1, ch2Index) - im.getPixel(j, i + 1, ch2Index));
        double gra_SE = Math.abs(im.getPixelG(j + 1, i + 1) - im.getPixelG(j - 1, i - 1)) + Math.abs(im.getPixelG(j + 2, i + 2) - im.getPixelG(j, i)) + Math.abs(im.getPixel(j + 2, i + 1, ch2Index) - im.getPixel(j, i - 1, ch2Index)) + Math.abs(im.getPixel(j + 1, i + 2, ch0Index) - im.getPixel(j - 1, i, ch0Index));
        double gra_NW = Math.abs(im.getPixelG(j - 1, i - 1) - im.getPixelG(j + 1, i + 1)) + Math.abs(im.getPixelG(j - 2, i - 2) - im.getPixelG(j, i)) + Math.abs(im.getPixel(j - 1, i - 2, ch0Index) - im.getPixel(j + 1, i, ch0Index)) + Math.abs(im.getPixel(j - 2, i - 1, ch2Index) - im.getPixel(j, i + 1, ch2Index));
        double gra_SW = Math.abs(im.getPixelG(j - 1, i + 1) - im.getPixelG(j + 1, i - 1)) + Math.abs(im.getPixelG(j - 2, i + 2) - im.getPixelG(j, i)) + Math.abs(im.getPixel(j - 1, i + 2, ch0Index) - im.getPixel(j + 1, i, ch0Index)) + Math.abs(im.getPixel(j - 2, i + 1, ch2Index) - im.getPixelG(j, i - 1));
        double[] gra = new double[] { gra_N, gra_E, gra_S, gra_W, gra_NE, gra_SE, gra_NW, gra_SW };
        return gra;
    }

    protected static final double[] gradientsRB(int i, int j, IntegerImage im, int ch0Index, int ch2Index) {
        double gra_N = Math.abs(im.getPixelG(j, i - 1) - im.getPixelG(j, i + 1)) + Math.abs(im.getPixel(j, i - 2, ch0Index) - im.getPixel(j, i, ch0Index)) + 1. / 2. * Math.abs(im.getPixel(j - 1, i - 1, ch2Index) - im.getPixel(j - 1, i + 1, ch2Index)) + 1. / 2. * Math.abs(im.getPixel(j + 1, i - 1, ch2Index) - im.getPixel(j + 1, i + 1, ch2Index)) + 1. / 2. * Math.abs(im.getPixelG(j - 1, i - 2) - im.getPixelG(j - 1, i)) + 1. / 2. * Math.abs(im.getPixelG(j + 1, i - 2) - im.getPixelG(j + 1, i));
        double gra_E = Math.abs(im.getPixelG(j + 1, i) - im.getPixelG(j - 1, i)) + Math.abs(im.getPixel(j + 2, i, ch0Index) - im.getPixel(j, i, ch0Index)) + 1. / 2. * Math.abs(im.getPixel(j + 1, i - 1, ch2Index) - im.getPixel(j - 1, i - 1, ch2Index)) + 1. / 2. * Math.abs(im.getPixel(j + 1, i + 1, ch2Index) - im.getPixel(j - 1, i + 1, ch2Index)) + 1. / 2. * Math.abs(im.getPixelG(j + 2, i - 1) - im.getPixelG(j, i - 1)) + 1. / 2. * Math.abs(im.getPixelG(j + 2, i + 1) - im.getPixelG(j, i + 1));
        double gra_S = Math.abs(im.getPixelG(j, i + 1) - im.getPixelG(j, i - 1)) + Math.abs(im.getPixel(j, i + 2, ch0Index) - im.getPixel(j, i, ch0Index)) + 1. / 2. * Math.abs(im.getPixel(j + 1, i + 1, ch2Index) - im.getPixel(j + 1, i - 1, ch2Index)) + 1. / 2. * Math.abs(im.getPixel(j - 1, i + 1, ch2Index) - im.getPixel(j - 1, i - 1, ch2Index)) + 1. / 2. * Math.abs(im.getPixelG(j + 1, i + 2) - im.getPixelG(j + 1, i)) + 1. / 2. * Math.abs(im.getPixelG(j - 1, i + 2) - im.getPixelG(j - 1, i));
        double gra_W = Math.abs(im.getPixelG(j - 1, i) - im.getPixelG(j + 1, i)) + Math.abs(im.getPixel(j - 2, i, ch0Index) - im.getPixel(j, i, ch0Index)) + 1. / 2. * Math.abs(im.getPixel(j - 1, i + 1, ch2Index) - im.getPixel(j + 1, i + 1, ch2Index)) + 1. / 2. * Math.abs(im.getPixel(j - 1, i - 1, ch2Index) - im.getPixel(j + 1, i - 1, ch2Index)) + 1. / 2. * Math.abs(im.getPixelG(j - 2, i + 1) - im.getPixelG(j, i + 1)) + 1. / 2. * Math.abs(im.getPixelG(j - 2, i - 1) - im.getPixelG(j, i - 1));
        double gra_NE = Math.abs(im.getPixel(j + 1, i - 1, ch2Index) - im.getPixel(j - 1, i + 1, ch2Index)) + Math.abs(im.getPixel(j + 2, i - 2, ch0Index) - im.getPixel(j, i, ch0Index)) + 1. / 2. * Math.abs(im.getPixelG(j, i - 1) - im.getPixelG(j - 1, i)) + 1. / 2. * Math.abs(im.getPixelG(j + 1, i) - im.getPixelG(j, i + 1)) + 1. / 2. * Math.abs(im.getPixelG(j + 1, i - 2) - im.getPixelG(j, i - 1)) + 1. / 2. * Math.abs(im.getPixelG(j + 2, i - 1) - im.getPixelG(j + 1, i));
        double gra_SE = Math.abs(im.getPixel(j + 1, i + 1, ch2Index) - im.getPixel(j - 1, i - 1, ch2Index)) + Math.abs(im.getPixel(j + 2, i + 2, ch0Index) - im.getPixel(j, i, ch0Index)) + 1. / 2. * Math.abs(im.getPixelG(j + 1, i) - im.getPixelG(j, i - 1)) + 1. / 2. * Math.abs(im.getPixelG(j, i + 1) - im.getPixelG(j - 1, i)) + 1. / 2. * Math.abs(im.getPixelG(j + 2, i + 1) - im.getPixelG(j + 1, i)) + 1. / 2. * Math.abs(im.getPixelG(j + 1, i + 2) - im.getPixelG(j, i + 1));
        double gra_NW = Math.abs(im.getPixel(j - 1, i - 1, ch2Index) - im.getPixel(j + 1, i + 1, ch2Index)) + Math.abs(im.getPixel(j - 2, i - 2, ch0Index) - im.getPixel(j, i, ch0Index)) + 1. / 2. * Math.abs(im.getPixelG(j, i - 1) - im.getPixelG(j + 1, i)) + 1. / 2. * Math.abs(im.getPixelG(j - 1, i) - im.getPixelG(j, i + 1)) + 1. / 2. * Math.abs(im.getPixelG(j - 1, i - 2) - im.getPixelG(j, i - 1)) + 1. / 2. * Math.abs(im.getPixelG(j - 2, i - 1) - im.getPixelG(j - 1, i));
        double gra_SW = Math.abs(im.getPixel(j - 1, i + 1, ch2Index) - im.getPixel(j + 1, i - 1, ch2Index)) + Math.abs(im.getPixel(j - 2, i + 2, ch0Index) - im.getPixel(j, i, ch0Index)) + 1. / 2. * Math.abs(im.getPixelG(j, i + 1) - im.getPixelG(j + 1, i)) + 1. / 2. * Math.abs(im.getPixelG(j - 1, i) - im.getPixelG(j, i - 1)) + 1. / 2. * Math.abs(im.getPixelG(j - 1, i + 2) - im.getPixelG(j, i + 1)) + 1. / 2. * Math.abs(im.getPixelG(j - 2, i + 1) - im.getPixelG(j - 1, i));
        double[] gra = new double[] { gra_N, gra_E, gra_S, gra_W, gra_NE, gra_SE, gra_NW, gra_SW };
        return gra;
    }

    /**
   *
   * @param im IntegerImage
   * @return IntegerImage
   * @deprecated
   */
    public static final IntegerImage variableNumberGradientsMethodFast(IntegerImage im) {
        int m = im.getHeight();
        int n = im.getWidth();
        double k1 = 1.5;
        double k2 = 0.5;
        for (int i = 2; i < m - 3; i += 2) {
            for (int j = 3; j < n - 2; j += 2) {
                double[] gra = gradientsRB(i, j, im, 0, 2);
                double gramax = Maths.max(gra);
                double gramin = Maths.min(gra);
                double T = k1 * gramin + k2 * (gramax - gramin);
                int[] ind = Matlab.find(Matlab.less(gra, T));
                double[] Rave = new double[8];
                double[] Gave = new double[8];
                double[] Bave = new double[8];
                double[][] ave = new double[][] { Rave, Gave, Bave };
                determineThresholdsRB(i, j, im, ave, ind, 0, 2);
                double Rsum = Maths.sum(Rave);
                double Gsum = Maths.sum(Gave);
                double Bsum = Maths.sum(Bave);
                im.setPixelG(j, i, im.getPixelR(j, i) + (Gsum - Rsum) / ind.length);
                im.setPixelB(j, i, im.getPixelR(j, i) + (Bsum - Rsum) / ind.length);
            }
        }
        for (int i = 3; i < m - 2; i += 2) {
            for (int j = 2; j < n - 3; j += 2) {
                double[] gra = gradientsRB(i, j, im, 2, 0);
                double gramax = Maths.max(gra);
                double gramin = Maths.min(gra);
                double T = k1 * gramin + k2 * (gramax - gramin);
                int[] ind = Matlab.find(Matlab.less(gra, T));
                double[] Rave = new double[8];
                double[] Gave = new double[8];
                double[] Bave = new double[8];
                double[][] ave = new double[][] { Rave, Gave, Bave };
                determineThresholdsRB(i, j, im, ave, ind, 2, 0);
                double Rsum = Maths.sum(Rave);
                double Gsum = Maths.sum(Gave);
                double Bsum = Maths.sum(Bave);
                im.setPixelG(j, i, im.getPixelB(j, i) + (Gsum - Bsum) / ind.length);
                im.setPixelR(j, i, im.getPixelB(j, i) + (Rsum - Bsum) / ind.length);
            }
        }
        for (int i = 2; i < m - 3; i += 2) {
            for (int j = 2; j < n - 3; j += 2) {
                double[] gra = gradientsG(i, j, im, 0, 2);
                double gramax = Maths.max(gra);
                double gramin = Maths.min(gra);
                double T = k1 * gramin + k2 * (gramax - gramin);
                int[] ind = Matlab.find(Matlab.less(gra, T));
                double[] Rave = new double[8];
                double[] Gave = new double[8];
                double[] Bave = new double[8];
                double[][] ave = new double[][] { Rave, Gave, Bave };
                determineThresholdsG(i, j, im, ave, ind, 0, 2);
                double Rsum = Maths.sum(Rave);
                double Gsum = Maths.sum(Gave);
                double Bsum = Maths.sum(Bave);
                im.setPixelR(j, i, im.getPixelG(j, i) + (Rsum - Gsum) / ind.length);
                im.setPixelB(j, i, im.getPixelG(j, i) + (Bsum - Gsum) / ind.length);
            }
        }
        for (int i = 3; i < m - 2; i += 2) {
            for (int j = 3; j < n - 2; j += 2) {
                double[] gra = gradientsG(i, j, im, 2, 0);
                double gramax = Maths.max(gra);
                double gramin = Maths.min(gra);
                double T = k1 * gramin + k2 * (gramax - gramin);
                int[] ind = Matlab.find(Matlab.less(gra, T));
                double[] Rave = new double[8];
                double[] Gave = new double[8];
                double[] Bave = new double[8];
                double[][] ave = new double[][] { Rave, Gave, Bave };
                determineThresholdsG(i, j, im, ave, ind, 2, 0);
                double Rsum = Maths.sum(Rave);
                double Gsum = Maths.sum(Gave);
                double Bsum = Maths.sum(Bave);
                im.setPixelR(j, i, im.getPixelG(j, i) + (Rsum - Gsum) / ind.length);
                im.setPixelB(j, i, im.getPixelG(j, i) + (Bsum - Gsum) / ind.length);
            }
        }
        return im;
    }
}
