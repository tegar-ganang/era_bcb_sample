package flanagan.plot;

import java.awt.*;
import java.io.Serializable;
import flanagan.math.Fmath;
import flanagan.math.ArrayMaths;
import flanagan.interpolation.CubicSpline;

public class Plot extends Canvas implements Serializable {

    protected static final long serialVersionUID = 1L;

    protected double[][] data = null;

    protected double[][] copy = null;

    protected int nCurves = 0;

    protected int[] nPoints = null;

    protected int nmPoints = 0;

    protected int niPoints = 200;

    protected int[] pointOpt = null;

    protected int[] pointSize = null;

    protected int npTypes = 8;

    protected boolean[] errorBar = null;

    protected double[][] errors = null;

    protected double[][] errorsCopy = null;

    protected int[] lineOpt = null;

    protected int[] dashLength = null;

    protected boolean[] minMaxOpt = null;

    protected boolean[] trimOpt = null;

    protected int fontSize = 14;

    protected int xLen = 625;

    protected int yLen = 375;

    protected int xBot = 100;

    protected int xTop = xBot + xLen;

    protected int yTop = 110;

    protected int yBot = yTop + yLen;

    protected double xLow = 0;

    protected double xHigh = 0;

    protected double yLow = 0;

    protected double yHigh = 0;

    protected int xFac = 0;

    protected int yFac = 0;

    protected int xTicks = 0;

    protected int yTicks = 0;

    protected double xMin = 0.0D;

    protected double xMax = 0.0D;

    protected double yMin = 0.0D;

    protected double yMax = 0.0D;

    protected double xOffset = 0.0D;

    protected double yOffset = 0.0D;

    protected boolean noXoffset = false;

    protected boolean noYoffset = false;

    protected double xLowFac = 0.75D;

    protected double yLowFac = 0.75D;

    protected String graphTitle = "  ";

    protected String graphTitle2 = "  ";

    protected String xAxisLegend = "  ";

    protected String xAxisUnits = "  ";

    protected String yAxisLegend = "  ";

    protected String yAxisUnits = "  ";

    protected boolean xZero = false;

    protected boolean yZero = false;

    protected boolean noXunits = true;

    protected boolean noYunits = true;

    protected double[] xAxisNo = new double[50];

    protected double[] yAxisNo = new double[50];

    protected String[] xAxisChar = new String[50];

    protected String[] yAxisChar = new String[50];

    protected int[] axisTicks = new int[50];

    protected static double dataFill = 3.0e200;

    public Plot(double[][] data) {
        this.initialise(data);
    }

    public Plot(double[] xdata, double[] ydata) {
        int xl = xdata.length;
        int yl = ydata.length;
        if (xl != yl) throw new IllegalArgumentException("x-data length is not equal to the y-data length");
        double[][] data = new double[2][xl];
        for (int i = 0; i < xl; i++) {
            data[0][i] = xdata[i];
            data[1][i] = ydata[i];
        }
        this.initialise(data);
    }

    private void initialise(double[][] cdata) {
        this.nCurves = cdata.length / 2;
        this.nPoints = new int[nCurves];
        this.lineOpt = new int[nCurves];
        this.dashLength = new int[nCurves];
        this.trimOpt = new boolean[nCurves];
        this.minMaxOpt = new boolean[nCurves];
        this.pointOpt = new int[nCurves];
        this.pointSize = new int[nCurves];
        this.errorBar = new boolean[nCurves];
        this.nmPoints = 0;
        int ll = 0;
        for (int i = 0; i < 2 * nCurves; i++) {
            if ((ll = cdata[i].length) > nmPoints) nmPoints = ll;
        }
        this.data = new double[2 * nCurves][nmPoints];
        this.copy = new double[2 * nCurves][nmPoints];
        this.errors = new double[nCurves][nmPoints];
        this.errorsCopy = new double[nCurves][nmPoints];
        int k = 0, l1 = 0, l2 = 0;
        boolean testlen = true;
        for (int i = 0; i < nCurves; i++) {
            k = 2 * i;
            testlen = true;
            l1 = cdata[k].length;
            l2 = cdata[k + 1].length;
            if (l1 != l2) throw new IllegalArgumentException("an x and y array length differ");
            nPoints[i] = l1;
        }
        k = 0;
        boolean testopt = true;
        for (int i = 0; i < nCurves; i++) {
            testlen = true;
            l1 = nPoints[i];
            while (testlen) {
                if (l1 < 0) throw new IllegalArgumentException("curve array index  " + k + ": blank array");
                if (cdata[k][l1 - 1] == dataFill) {
                    if (cdata[k + 1][l1 - 1] == dataFill) {
                        l1--;
                        testopt = false;
                    } else {
                        testlen = false;
                    }
                } else {
                    testlen = false;
                }
            }
            nPoints[i] = l1;
            k += 2;
        }
        k = 0;
        for (int i = 0; i < nCurves; i++) {
            double[][] xxx = new double[2][nPoints[i]];
            for (int j = 0; j < nPoints[i]; j++) {
                xxx[0][j] = cdata[k][j];
                xxx[1][j] = cdata[k + 1][j];
            }
            xxx = doubleSelectionSort(xxx);
            for (int j = 0; j < nPoints[i]; j++) {
                cdata[k][j] = xxx[0][j];
                cdata[k + 1][j] = xxx[1][j];
            }
            k += 2;
        }
        k = 0;
        int kk = 1;
        for (int i = 0; i < nCurves; i++) {
            int rev = 1;
            for (int j = 1; j < nPoints[i]; j++) {
                if (cdata[k][j] < cdata[k][j - 1]) rev++;
            }
            if (rev == nPoints[i]) {
                double[] hold = new double[nPoints[i]];
                for (int j = 0; j < nPoints[i]; j++) hold[j] = cdata[k][j];
                for (int j = 0; j < nPoints[i]; j++) cdata[k][j] = hold[nPoints[i] - j - 1];
                for (int j = 0; j < nPoints[i]; j++) hold[j] = cdata[k + 1][j];
                for (int j = 0; j < nPoints[i]; j++) cdata[k + 1][j] = hold[nPoints[i] - j - 1];
            }
            for (int j = 0; j < nPoints[i]; j++) {
                this.data[k][j] = cdata[k][j];
                this.data[k + 1][j] = cdata[k + 1][j];
                this.copy[k][j] = cdata[k][j];
                this.copy[k + 1][j] = cdata[k + 1][j];
            }
            this.lineOpt[i] = 1;
            this.dashLength[i] = 5;
            this.trimOpt[i] = false;
            if (this.lineOpt[i] == 1) trimOpt[i] = true;
            this.minMaxOpt[i] = true;
            this.pointSize[i] = 6;
            this.errorBar[i] = false;
            this.pointOpt[i] = kk;
            k += 2;
            kk++;
            if (kk > npTypes) kk = 1;
        }
    }

    public static double[][] doubleSelectionSort(double[][] aa) {
        int index = 0;
        int lastIndex = -1;
        int n = aa[0].length;
        double holdx = 0.0D;
        double holdy = 0.0D;
        double[][] bb = new double[2][n];
        for (int i = 0; i < n; i++) {
            bb[0][i] = aa[0][i];
            bb[1][i] = aa[1][i];
        }
        while (lastIndex != n - 1) {
            index = lastIndex + 1;
            for (int i = lastIndex + 2; i < n; i++) {
                if (bb[0][i] < bb[0][index]) {
                    index = i;
                }
            }
            lastIndex++;
            holdx = bb[0][index];
            bb[0][index] = bb[0][lastIndex];
            bb[0][lastIndex] = holdx;
            holdy = bb[1][index];
            bb[1][index] = bb[1][lastIndex];
            bb[1][lastIndex] = holdy;
        }
        return bb;
    }

    public static double[][] data(int n, int m) {
        double[][] d = new double[2 * n][m];
        for (int i = 0; i < 2 * n; i++) {
            for (int j = 0; j < m; j++) {
                d[i][j] = dataFill;
            }
        }
        return d;
    }

    public static void setDataFillValue(double dataFill) {
        Plot.dataFill = dataFill;
    }

    public static double getDataFillValue() {
        return Plot.dataFill;
    }

    public void setGraphTitle(String graphTitle) {
        this.graphTitle = graphTitle;
    }

    public void setGraphTitle2(String graphTitle2) {
        this.graphTitle2 = graphTitle2;
    }

    public void setXaxisLegend(String xAxisLegend) {
        this.xAxisLegend = xAxisLegend;
    }

    public void setYaxisLegend(String yAxisLegend) {
        this.yAxisLegend = yAxisLegend;
    }

    public void setXaxisUnitsName(String xAxisUnits) {
        this.xAxisUnits = xAxisUnits;
        this.noXunits = false;
    }

    public void setYaxisUnitsName(String yAxisUnits) {
        this.yAxisUnits = yAxisUnits;
        this.noYunits = false;
    }

    public int getXaxisLen() {
        return this.xLen;
    }

    public int getYaxisLen() {
        return this.yLen;
    }

    public int getXlow() {
        return this.xBot;
    }

    public int getYhigh() {
        return this.yTop;
    }

    public int[] getPointsize() {
        return this.pointSize;
    }

    public int[] getDashlength() {
        return this.dashLength;
    }

    public double getXlowFac() {
        return 1.0D - this.xLowFac;
    }

    public double getYlowFac() {
        return 1.0D - this.yLowFac;
    }

    public double getXmin() {
        return this.xMin;
    }

    public double getXmax() {
        return this.xMax;
    }

    public double getYmin() {
        return this.yMin;
    }

    public double getYmax() {
        return this.yMax;
    }

    public int[] getLine() {
        return this.lineOpt;
    }

    public int[] getPoint() {
        return this.pointOpt;
    }

    public int getNiPoints() {
        return this.niPoints;
    }

    public int getFontSize() {
        return this.fontSize;
    }

    public void setXaxisLen(int xLen) {
        this.xLen = xLen;
        this.update();
    }

    public void setYaxisLen(int yLen) {
        this.yLen = yLen;
        this.update();
    }

    public void setXlow(int xBot) {
        this.xBot = xBot;
        this.update();
    }

    public void setYhigh(int yTop) {
        this.yTop = yTop;
        this.update();
    }

    public void setXlowFac(double xLowFac) {
        this.xLowFac = 1.0D - xLowFac;
    }

    public void setYlowFac(double yLowFac) {
        this.yLowFac = 1.0D - yLowFac;
    }

    public void setNoXoffset(boolean noXoffset) {
        this.noXoffset = noXoffset;
    }

    public void setNoYoffset(boolean noYoffset) {
        this.noYoffset = noYoffset;
    }

    public void setNoOffset(boolean nooffset) {
        this.noXoffset = nooffset;
        this.noYoffset = nooffset;
    }

    public boolean getNoXoffset() {
        return this.noXoffset;
    }

    public boolean getNoYoffset() {
        return this.noYoffset;
    }

    protected void update() {
        this.xTop = this.xBot + this.xLen;
        this.yBot = this.yTop + this.yLen;
    }

    public void setLine(int[] lineOpt) {
        int n = lineOpt.length;
        if (n != nCurves) throw new IllegalArgumentException("input array of wrong length");
        for (int i = 0; i < n; i++) if (lineOpt[i] < 0 || lineOpt[i] > 4) throw new IllegalArgumentException("lineOpt must be 0, 1, 2, 3 or 4");
        this.lineOpt = lineOpt;
        for (int i = 0; i < this.lineOpt.length; i++) {
            if (this.lineOpt[i] == 1 || this.lineOpt[i] == 2) {
                boolean test0 = false;
                for (int j = 1; j < this.nPoints[i]; j++) {
                    if (data[i][j] < data[i][j - 1]) test0 = true;
                }
                if (test0) {
                    int rev = 1;
                    for (int j = 1; j < nPoints[i]; j++) {
                        if (data[2 * i][j] > data[2 * i][j - 1]) rev++;
                    }
                    if (rev == nPoints[i]) {
                        lineOpt[i] = -lineOpt[i];
                    } else {
                        rev = 1;
                        for (int j = 1; j < nPoints[i]; j++) {
                            if (data[2 * i][j] < data[2 * i][j - 1]) rev++;
                        }
                        if (rev == nPoints[i]) {
                            double[] hold = new double[nPoints[i]];
                            for (int j = 0; j < nPoints[i]; j++) hold[j] = data[i][j];
                            for (int j = 0; j < nPoints[i]; j++) data[i][j] = hold[nPoints[i] - j - 1];
                            for (int j = 0; j < nPoints[i]; j++) hold[j] = data[2 * i][j];
                            for (int j = 0; j < nPoints[i]; j++) data[2 * i][j] = hold[nPoints[i] - j - 1];
                            this.lineOpt[i] = -lineOpt[i];
                        } else {
                            System.out.println("Curve " + i + " will not support interpolation");
                            System.out.println("Straight connecting line option used");
                            if (this.lineOpt[i] == 1) this.lineOpt[i] = 3;
                            if (this.lineOpt[i] == 2) this.lineOpt[i] = 4;
                        }
                    }
                }
            }
        }
    }

    public void setLine(int slineOpt) {
        if (slineOpt < 0 || slineOpt > 3) throw new IllegalArgumentException("lineOpt must be 0, 1, 2 or 3");
        for (int i = 0; i < this.nCurves; i++) this.lineOpt[i] = slineOpt;
    }

    public void setDashLength(int[] dashLength) {
        if (dashLength.length != nCurves) throw new IllegalArgumentException("input array of wrong length");
        this.dashLength = dashLength;
    }

    public void setDashLength(int sdashLength) {
        for (int i = 0; i < this.nCurves; i++) this.dashLength[i] = sdashLength;
    }

    public void setPoint(int[] pointOpt) {
        int n = pointOpt.length;
        if (n != nCurves) throw new IllegalArgumentException("input array of wrong length");
        for (int i = 0; i < n; i++) if (pointOpt[i] < 0 || pointOpt[i] > 8) throw new IllegalArgumentException("pointOpt must be 0, 1, 2, 3, 4, 5, 6, 7, or 8");
        this.pointOpt = pointOpt;
    }

    public void setPoint(int spointOpt) {
        if (spointOpt < 0 || spointOpt > 8) throw new IllegalArgumentException("pointOpt must be 0, 1, 2, 3, 4, 5, 6, 7, or 8");
        for (int i = 0; i < this.nCurves; i++) this.pointOpt[i] = spointOpt;
    }

    public void setPointSize(int[] mpointSize) {
        if (mpointSize.length != nCurves) throw new IllegalArgumentException("input array of wrong length");
        for (int i = 0; i < this.nCurves; i++) {
            if (mpointSize[i] != (mpointSize[i] / 2) * 2) mpointSize[i]++;
            this.pointSize[i] = mpointSize[i];
        }
    }

    public void setPointSize(int spointSize) {
        if (spointSize % 2 != 0) spointSize++;
        for (int i = 0; i < this.nCurves; i++) this.pointSize[i] = spointSize;
    }

    public void setErrorBars(int nc, double[] err) {
        if (err.length != this.nPoints[nc]) throw new IllegalArgumentException("input array of wrong length");
        this.errorBar[nc] = true;
        for (int i = 0; i < this.nPoints[nc]; i++) {
            this.errors[nc][i] = err[i];
            this.errorsCopy[nc][i] = err[i];
        }
    }

    public void setNiPoints(int niPoints) {
        this.niPoints = niPoints;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public void setTrimOpt(boolean[] trim) {
        this.trimOpt = trim;
    }

    public void setMinMaxOpt(boolean[] minmax) {
        this.minMaxOpt = minmax;
    }

    public static int scale(double mmin, double mmax) {
        int fac = 0;
        double big = 0.0D;
        boolean test = false;
        if (mmin >= 0.0 && mmax > 0.0) {
            big = mmax;
            test = true;
        } else {
            if (mmin < 0.0 && mmax <= 0.0) {
                big = -mmin;
                test = true;
            } else {
                if (mmax > 0.0 && mmin < 0.0) {
                    big = Math.max(mmax, -mmin);
                    test = true;
                }
            }
        }
        if (test) {
            if (big > 100.0) {
                while (big > 1.0) {
                    big /= 10.0;
                    fac--;
                }
            }
            if (big <= 0.01) {
                while (big <= 0.10) {
                    big *= 10.0;
                    fac++;
                }
            }
        }
        return fac;
    }

    public static void limits(double low, double high, double lowfac, double[] limits) {
        double facl = 1.0D;
        double fach = 1.0D;
        if (Math.abs(low) < 1.0D) facl = 10.0D;
        if (Math.abs(low) < 0.1D) facl = 100.0D;
        if (Math.abs(high) < 1.0D) fach = 10.0D;
        if (Math.abs(high) < 0.1D) fach = 100.0D;
        double ld = Math.floor(10.0 * low * facl) / facl;
        double hd = Math.ceil(10.0 * high * fach) / fach;
        if (ld >= 0.0D && hd > 0.0D) {
            if (ld < lowfac * hd) {
                ld = 0.0;
            }
        }
        if (ld < 0.0D && hd <= 0.0D) {
            if (-hd <= -lowfac * ld) {
                hd = 0.0;
            }
        }
        limits[0] = ld / 10.0;
        limits[1] = hd / 10.0;
    }

    public static double offset(double low, double high) {
        double diff = high - low;
        double sh = Fmath.sign(high);
        double sl = Fmath.sign(low);
        double offset = 0.0D;
        int eh = 0, ed = 0;
        if (sh == sl) {
            ed = (int) Math.floor(Fmath.log10(diff));
            if (sh == 1) {
                eh = (int) Math.floor(Fmath.log10(high));
                if (eh - ed > 1) offset = Math.floor(low * Math.pow(10, -ed)) * Math.pow(10, ed);
            } else {
                eh = (int) Math.floor(Fmath.log10(Math.abs(low)));
                if (eh - ed > 1) offset = Math.floor(high * Math.pow(10, -ed)) * Math.pow(10, ed);
            }
        }
        return offset;
    }

    public void axesScaleOffset() {
        double[] limit = new double[2];
        int k = 0;
        for (int i = 0; i < nCurves; i++) {
            for (int j = 0; j < nPoints[i]; j++) {
                this.data[k][j] = this.copy[k][j];
                this.data[k + 1][j] = this.copy[k + 1][j];
                this.errors[i][j] = this.errorsCopy[i][j];
                if (this.errorBar[i]) this.errors[i][j] += this.data[k + 1][j];
            }
            k += 2;
        }
        minMax();
        if (!noXoffset) this.xOffset = offset(this.xMin, this.xMax);
        if (this.xOffset != 0.0) {
            k = 0;
            for (int i = 0; i < this.nCurves; i++) {
                for (int j = 0; j < this.nPoints[i]; j++) {
                    this.data[k][j] -= this.xOffset;
                }
                k += 2;
            }
            this.xMin -= this.xOffset;
            this.xMax -= this.xOffset;
        }
        if (!noYoffset) this.yOffset = offset(this.yMin, this.yMax);
        if (this.yOffset != 0.0) {
            k = 1;
            for (int i = 0; i < this.nCurves; i++) {
                for (int j = 0; j < this.nPoints[i]; j++) {
                    this.data[k][j] -= this.yOffset;
                    if (this.errorBar[i]) this.errors[i][j] -= this.yOffset;
                }
                k += 2;
            }
            this.yMin -= this.yOffset;
            this.yMax -= this.yOffset;
        }
        this.xFac = scale(this.xMin, this.xMax);
        if (this.xFac != 0) {
            k = 0;
            for (int i = 0; i < this.nCurves; i++) {
                for (int j = 0; j < this.nPoints[i]; j++) {
                    this.data[k][j] *= Math.pow(10, this.xFac + 1);
                }
                k += 2;
            }
            this.xMin *= Math.pow(10, this.xFac + 1);
            this.xMax *= Math.pow(10, this.xFac + 1);
        }
        this.yFac = scale(this.yMin, this.yMax);
        if (this.yFac != 0) {
            k = 1;
            for (int i = 0; i < this.nCurves; i++) {
                for (int j = 0; j < this.nPoints[i]; j++) {
                    this.data[k][j] *= Math.pow(10, yFac + 1);
                    if (this.errorBar[i]) this.errors[i][j] *= Math.pow(10, this.yFac + 1);
                }
                k += 2;
            }
            this.yMin *= Math.pow(10, this.yFac + 1);
            this.yMax *= Math.pow(10, this.yFac + 1);
        }
        limits(this.xMin, this.xMax, this.xLowFac, limit);
        this.xLow = limit[0];
        this.xHigh = limit[1];
        if (xLow < 0 && xHigh > 0) xZero = true;
        limits(this.yMin, this.yMax, this.yLowFac, limit);
        this.yLow = limit[0];
        this.yHigh = limit[1];
        if (yLow < 0 && yHigh > 0) yZero = true;
        this.xTicks = ticks(this.xLow, this.xHigh, this.xAxisNo, this.xAxisChar);
        this.xHigh = this.xAxisNo[this.xTicks - 1];
        if (this.xLow != this.xAxisNo[0]) {
            if (this.xOffset != 0.0D) {
                this.xOffset = this.xOffset - this.xLow + this.xAxisNo[0];
            }
            this.xLow = this.xAxisNo[0];
        }
        this.yTicks = ticks(this.yLow, this.yHigh, this.yAxisNo, this.yAxisChar);
        this.yHigh = this.yAxisNo[this.yTicks - 1];
        if (this.yLow != this.yAxisNo[0]) {
            if (this.yOffset != 0.0D) {
                this.yOffset = this.yOffset - this.yLow + this.yAxisNo[0];
            }
            this.yLow = this.yAxisNo[0];
        }
    }

    public static int ticks(double low, double high, double[] tickval, String[] tickchar) {
        int[] trunc = { 1, 1, 1, 2, 3 };
        double[] scfac1 = { 1.0, 10.0, 1.0, 0.1, 0.01 };
        double[] scfac2 = { 1.0, 1.0, 0.1, 0.01, 0.001 };
        double rmax = Math.abs(high);
        double temp = Math.abs(low);
        if (temp > rmax) rmax = temp;
        int range = 0;
        if (rmax <= 100.0D) {
            range = 1;
        }
        if (rmax <= 10.0D) {
            range = 2;
        }
        if (rmax <= 1.0D) {
            range = 3;
        }
        if (rmax <= 0.1D) {
            range = 4;
        }
        if (rmax > 100.0D || rmax < 0.01) range = 0;
        double inc = 0.0D;
        double bot = 0.0D;
        double top = 0.0D;
        int sgn = 0;
        int dirn = 0;
        if (high > 0.0D && low >= 0.0D) {
            inc = Math.ceil((high - low) / scfac1[range]) * scfac2[range];
            dirn = 1;
            bot = low;
            top = high;
            sgn = 1;
        } else {
            if (high <= 0 && low < 0.0D) {
                inc = Math.ceil((high - low) / scfac1[range]) * scfac2[range];
                dirn = -1;
                bot = high;
                top = low;
                sgn = -1;
            } else {
                double up = Math.abs(Math.ceil(high));
                double down = Math.abs(Math.floor(low));
                int np = 0;
                if (up >= down) {
                    dirn = 2;
                    np = (int) Math.rint(10.0 * up / (up + down));
                    inc = Math.ceil((high * 10 / np) / scfac1[range]) * scfac2[range];
                    bot = 0.0D;
                    top = high;
                    sgn = 1;
                } else {
                    dirn = -2;
                    np = (int) Math.rint(10.0D * down / (up + down));
                    inc = Math.ceil((Math.abs(low * 10 / np)) / scfac1[range]) * scfac2[range];
                    bot = 0.0D;
                    top = low;
                    sgn = -1;
                }
            }
        }
        int nticks = 1;
        double sum = bot;
        boolean test = true;
        while (test) {
            sum = sum + sgn * inc;
            nticks++;
            if (Math.abs(sum) >= Math.abs(top)) test = false;
        }
        int npExtra = 0;
        double[] ttickval = null;
        ;
        switch(dirn) {
            case 1:
                ttickval = new double[nticks];
                tickval[0] = Fmath.truncate(low, trunc[range]);
                for (int i = 1; i < nticks; i++) {
                    tickval[i] = Fmath.truncate(tickval[i - 1] + inc, trunc[range]);
                }
                break;
            case -1:
                ttickval = new double[nticks];
                ttickval[0] = Fmath.truncate(high, trunc[range]);
                for (int i = 1; i < nticks; i++) {
                    ttickval[i] = Fmath.truncate(ttickval[i - 1] - inc, trunc[range]);
                }
                ttickval = Fmath.reverseArray(ttickval);
                for (int i = 0; i < nticks; i++) tickval[i] = ttickval[i];
                break;
            case 2:
                npExtra = (int) Math.ceil(-low / inc);
                nticks += npExtra;
                ttickval = new double[nticks];
                tickval[0] = Fmath.truncate(-npExtra * inc, trunc[range]);
                for (int i = 1; i < nticks; i++) {
                    tickval[i] = Fmath.truncate(tickval[i - 1] + inc, trunc[range]);
                }
                break;
            case -2:
                npExtra = (int) Math.ceil(high / inc);
                nticks += npExtra;
                ttickval = new double[nticks];
                ttickval[0] = Fmath.truncate(npExtra * inc, trunc[range]);
                for (int i = 1; i < nticks; i++) {
                    ttickval[i] = Fmath.truncate(ttickval[i - 1] - inc, trunc[range]);
                }
                ttickval = Fmath.reverseArray(ttickval);
                for (int i = 0; i < nticks; i++) tickval[i] = ttickval[i];
                break;
        }
        ArrayMaths am = new ArrayMaths(tickval);
        double max = am.maximum();
        double min = Math.abs(am.minimum());
        boolean testZero = true;
        int counter = 0;
        while (testZero) {
            if (Math.abs(tickval[counter]) < max * 1e-4 || Math.abs(tickval[counter]) < min * 1e-4) {
                tickval[counter] = 0.0;
                testZero = false;
            } else {
                counter++;
                if (counter >= nticks) testZero = false;
            }
        }
        for (int i = 0; i < nticks; i++) {
            tickchar[i] = String.valueOf(tickval[i]);
            tickchar[i] = tickchar[i].trim();
        }
        return nticks;
    }

    public void minMax() {
        boolean test = true;
        int ii = 0;
        while (test) {
            if (this.minMaxOpt[ii]) {
                test = false;
                this.xMin = this.data[2 * ii][0];
                this.xMax = this.data[2 * ii][0];
                this.yMin = this.data[2 * ii + 1][0];
                if (this.errorBar[ii]) this.yMin = 2.0D * this.yMin - this.errors[ii][0];
                this.yMax = this.data[2 * ii + 1][0];
                if (this.errorBar[ii]) this.yMax = errors[ii][0];
            } else {
                ii++;
                if (ii > nCurves) throw new IllegalArgumentException("At least one curve must be included in the maximum/minimum calculation");
            }
        }
        int k = 0;
        double yMint = 0.0D, yMaxt = 0.0D;
        for (int i = 0; i < this.nCurves; i++) {
            if (minMaxOpt[i]) {
                for (int j = 0; j < this.nPoints[i]; j++) {
                    if (this.xMin > this.data[k][j]) this.xMin = this.data[k][j];
                    if (this.xMax < this.data[k][j]) this.xMax = this.data[k][j];
                    yMint = this.data[k + 1][j];
                    if (errorBar[i]) yMint = 2.0D * yMint - errors[i][j];
                    if (this.yMin > yMint) this.yMin = yMint;
                    yMaxt = this.data[k + 1][j];
                    if (errorBar[i]) yMaxt = errors[i][j];
                    if (this.yMax < yMaxt) this.yMax = yMaxt;
                }
            }
            k += 2;
        }
        if (this.xMin == this.xMax) {
            if (this.xMin == 0.0D) {
                this.xMin = 0.1D;
                this.xMax = 0.1D;
            } else {
                if (this.xMin < 0.0D) {
                    this.xMin = this.xMin * 1.1D;
                } else {
                    this.xMax = this.xMax * 1.1D;
                }
            }
        }
        if (this.yMin == this.yMax) {
            if (this.yMin == 0.0D) {
                this.yMin = 0.1D;
                this.yMax = 0.1D;
            } else {
                if (this.yMin < 0.0D) {
                    this.yMin = this.yMin * 1.1D;
                } else {
                    this.yMax = this.yMax * 1.1D;
                }
            }
        }
    }

    protected static String offsetString(double offset) {
        String stroffset = String.valueOf(offset);
        String substr1 = "", substr2 = "", substr3 = "";
        String zero = "0";
        int posdot = stroffset.indexOf('.');
        int posexp = stroffset.indexOf('E');
        if (posexp == -1) {
            return stroffset;
        } else {
            substr1 = stroffset.substring(posexp + 1);
            int n = Integer.parseInt(substr1);
            substr1 = stroffset.substring(0, posexp);
            if (n >= 0) {
                for (int i = 0; i < n; i++) {
                    substr1 = substr1 + zero;
                }
                return substr1;
            } else {
                substr2 = substr1.substring(0, posdot + 1);
                substr3 = substr1.substring(posdot + 1);
                for (int i = 0; i < -n; i++) {
                    substr2 = substr1 + zero;
                }
                substr2 = substr2 + substr3;
                return substr2;
            }
        }
    }

    public boolean printCheck(boolean trim, int xoldpoint, int xnewpoint, int yoldpoint, int ynewpoint) {
        boolean btest2 = true;
        if (trim) {
            if (xoldpoint < xBot) btest2 = false;
            if (xoldpoint > xTop) btest2 = false;
            if (xnewpoint < xBot) btest2 = false;
            if (xnewpoint > xTop) btest2 = false;
            if (yoldpoint > yBot) btest2 = false;
            if (yoldpoint < yTop) btest2 = false;
            if (ynewpoint > yBot) btest2 = false;
            if (ynewpoint < yTop) btest2 = false;
        }
        return btest2;
    }

    public void graph(Graphics g) {
        g.setFont(new Font("serif", Font.PLAIN, this.fontSize));
        FontMetrics fm = g.getFontMetrics();
        axesScaleOffset();
        String xoffstr = offsetString(xOffset);
        String yoffstr = offsetString(yOffset);
        String bunit1 = "  /( ";
        String bunit2 = " )";
        String bunit3 = "  / ";
        String bunit4 = " ";
        String bunit5 = " x 10";
        String bunit6 = "10";
        String nounit = " ";
        String xbrack1 = bunit1;
        String xbrack2 = bunit2;
        String xbrack3 = bunit5;
        if (this.xFac == 0) {
            xbrack1 = bunit3;
            xbrack2 = "";
            xbrack3 = "";
        }
        String ybrack1 = bunit1;
        String ybrack2 = bunit2;
        String ybrack3 = bunit5;
        if (this.yFac == 0) {
            ybrack1 = bunit3;
            ybrack2 = "";
            ybrack3 = "";
        }
        if (noXunits) {
            if (xFac == 0) {
                xbrack1 = nounit;
                xbrack2 = nounit;
                xbrack3 = nounit;
            } else {
                xbrack1 = bunit3;
                xbrack2 = bunit4;
                xbrack3 = bunit6;
            }
        }
        if (noYunits) {
            if (yFac == 0) {
                ybrack1 = nounit;
                ybrack2 = nounit;
                ybrack3 = nounit;
            } else {
                ybrack1 = bunit3;
                ybrack2 = bunit4;
                ybrack3 = bunit6;
            }
        }
        double xLen = xTop - xBot;
        double yLen = yBot - yTop;
        String sp = " + ", sn = " - ";
        String ss = sn;
        g.drawString(this.graphTitle + " ", 15, 15);
        g.drawString(this.graphTitle2 + " ", 15, 35);
        if (this.xOffset < 0) {
            ss = sp;
            xOffset = -xOffset;
        }
        int sw = 0;
        String ssx = "", ssy = "", sws1 = "", sws2 = "";
        if (this.xFac == 0 && this.xOffset == 0) {
            g.drawString(this.xAxisLegend + xbrack1 + this.xAxisUnits + xbrack2, xBot - 4, yBot + 32);
        } else {
            if (this.xOffset == 0) {
                ssx = this.xAxisLegend + xbrack1 + this.xAxisUnits + xbrack3;
                sw = fm.stringWidth(ssx);
                g.drawString(ssx, xBot - 4, yBot + 42);
                sws1 = String.valueOf(-this.xFac - 1);
                g.drawString(sws1, xBot - 4 + sw + 1, yBot + 32);
                sw += fm.stringWidth(sws1);
                g.drawString(xbrack2, xBot - 4 + sw + 1, yBot + 42);
            } else {
                if (this.xFac == 0) {
                    g.drawString(this.xAxisLegend + ss + xoffstr + xbrack1 + this.xAxisUnits + xbrack2, xBot - 4, yBot + 30);
                } else {
                    ssx = this.xAxisLegend + ss + xoffstr + xbrack1 + this.xAxisUnits + xbrack3;
                    sw = fm.stringWidth(ssx);
                    g.drawString(ssx, xBot - 4, yBot + 37);
                    sws1 = String.valueOf(-this.xFac - 1);
                    g.drawString(sws1, xBot - 4 + sw + 1, yBot + 32);
                    sw += fm.stringWidth(sws1);
                    g.drawString(xbrack2, xBot - 4 + sw + 1, yBot + 37);
                }
            }
        }
        ss = sn;
        if (yOffset < 0) {
            ss = sp;
            yOffset = -yOffset;
        }
        if (yFac == 0 && yOffset == 0) {
            g.drawString(this.yAxisLegend + " ", 15, yTop - 25);
            g.drawString(ybrack1 + this.yAxisUnits + ybrack2, 15, yTop - 10);
        } else {
            if (yOffset == 0) {
                g.drawString(this.yAxisLegend, 15, yTop - 35);
                sws1 = ybrack1 + this.yAxisUnits + ybrack3;
                g.drawString(sws1, 15, yTop - 15);
                sw = fm.stringWidth(sws1);
                sws2 = String.valueOf(-this.yFac - 1);
                g.drawString(sws2, 15 + sw + 1, yTop - 20);
                sw += fm.stringWidth(sws2);
                g.drawString(ybrack2, 15 + sw + 1, yTop - 15);
            } else {
                if (yFac == 0) {
                    g.drawString(this.yAxisLegend + ss + yoffstr, 15, yTop - 25);
                    g.drawString(ybrack1 + this.yAxisUnits + ybrack2, 15, yTop - 10);
                } else {
                    ssy = this.yAxisLegend + ss + yoffstr;
                    g.drawString(ssy, 15, yTop - 35);
                    sws1 = ybrack1 + this.yAxisUnits + ybrack3;
                    g.drawString(sws1, 15, yTop - 15);
                    sw = fm.stringWidth(sws1);
                    sws2 = String.valueOf(-this.yFac - 1);
                    g.drawString(sws2, 15 + sw + 1, yTop - 20);
                    sw += fm.stringWidth(sws2);
                    g.drawString(ybrack2, 15 + sw + 1, yTop - 15);
                }
            }
        }
        int zdif = 0, zold = 0, znew = 0, zzer = 0;
        double csstep = 0.0D;
        double xdenom = (xHigh - xLow);
        double ydenom = (yHigh - yLow);
        g.drawLine(xBot, yBot, xTop, yBot);
        g.drawLine(xBot, yTop, xTop, yTop);
        g.drawLine(xBot, yBot, xBot, yTop);
        g.drawLine(xTop, yBot, xTop, yTop);
        if (xZero) {
            zdif = 8;
            zzer = xBot + (int) (((0.0 - xLow) / xdenom) * xLen);
            g.drawLine(zzer, yTop, zzer, yTop + 8);
            g.drawLine(zzer, yBot, zzer, yBot - 8);
            zold = yTop;
            while (zold + zdif < yBot) {
                znew = zold + zdif;
                g.drawLine(zzer, zold, zzer, znew);
                zold = znew + zdif;
            }
        }
        if (yZero) {
            zdif = 8;
            zzer = yBot - (int) (((0.0 - yLow) / ydenom) * yLen);
            g.drawLine(xBot, zzer, xBot + 8, zzer);
            g.drawLine(xTop, zzer, xTop - 8, zzer);
            zold = xBot;
            while (zold + zdif < xTop) {
                znew = zold + zdif;
                g.drawLine(zold, zzer, znew, zzer);
                zold = znew + zdif;
            }
        }
        int xt = 0;
        for (int ii = 0; ii < this.xTicks; ii++) {
            xt = xBot + (int) (((this.xAxisNo[ii] - xLow) / xdenom) * xLen);
            g.drawLine(xt, yBot, xt, yBot - 8);
            g.drawLine(xt, yTop, xt, yTop + 8);
            g.drawString(xAxisChar[ii] + " ", xt - 4, yBot + 18);
        }
        int yt = 0;
        int yCharLenMax = yAxisChar[0].length();
        for (int ii = 1; ii < this.yTicks; ii++) if (yAxisChar[ii].length() > yCharLenMax) yCharLenMax = yAxisChar[ii].length();
        int shift = (yCharLenMax - 3) * 5;
        double ytep = (double) (-yTop + yBot) / ((double) (this.yTicks - 1));
        for (int ii = 0; ii < this.yTicks; ii++) {
            yt = yBot - (int) Math.round(ii * ytep);
            yt = yBot - (int) (((this.yAxisNo[ii] - yLow) / ydenom) * yLen);
            g.drawLine(xBot, yt, xBot + 8, yt);
            g.drawLine(xTop, yt, xTop - 8, yt);
            g.drawString(yAxisChar[ii] + " ", xBot - 30 - shift, yt + 4);
        }
        int dsum = 0;
        boolean dcheck = true;
        int kk = 0;
        int xxp = 0, yyp = 0, yype = 0;
        int xoldpoint = 0, xnewpoint = 0, yoldpoint = 0, ynewpoint = 0;
        int ps = 0, psh = 0, nxpoints = 0;
        double ics[] = new double[niPoints];
        boolean btest2 = true;
        for (int i = 0; i < this.nCurves; i++) {
            nxpoints = this.nPoints[i];
            double xcs[] = new double[nxpoints];
            double ycs[] = new double[nxpoints];
            if (lineOpt[i] == 1 || lineOpt[i] == 2) {
                CubicSpline cs = new CubicSpline(this.nPoints[i]);
                for (int ii = 0; ii < nxpoints; ii++) {
                    xcs[ii] = this.data[kk][ii];
                }
                csstep = (xcs[nxpoints - 1] - xcs[0]) / (niPoints - 1);
                ics[0] = xcs[0];
                for (int ii = 1; ii < niPoints; ii++) {
                    ics[ii] = ics[ii - 1] + csstep;
                }
                ics[niPoints - 1] = xcs[nxpoints - 1];
                for (int ii = 0; ii < nxpoints; ii++) {
                    ycs[ii] = this.data[kk + 1][ii];
                }
                cs.resetData(xcs, ycs);
                cs.calcDeriv();
                xoldpoint = xBot + (int) (((xcs[0] - xLow) / xdenom) * xLen);
                yoldpoint = yBot - (int) (((ycs[0] - yLow) / ydenom) * yLen);
                for (int ii = 1; ii < niPoints; ii++) {
                    xnewpoint = xBot + (int) (((ics[ii] - xLow) / xdenom) * xLen);
                    ynewpoint = yBot - (int) (((cs.interpolate(ics[ii]) - yLow) / ydenom) * yLen);
                    btest2 = printCheck(trimOpt[i], xoldpoint, xnewpoint, yoldpoint, ynewpoint);
                    if (btest2) {
                        if (this.lineOpt[i] == 2) {
                            dsum++;
                            if (dsum > dashLength[i]) {
                                dsum = 0;
                                if (dcheck) {
                                    dcheck = false;
                                } else {
                                    dcheck = true;
                                }
                            }
                        }
                        if (dcheck) g.drawLine(xoldpoint, yoldpoint, xnewpoint, ynewpoint);
                    }
                    xoldpoint = xnewpoint;
                    yoldpoint = ynewpoint;
                }
            }
            if (lineOpt[i] == -1 || lineOpt[i] == -2) {
                CubicSpline cs = new CubicSpline(this.nPoints[i]);
                for (int ii = 0; ii < nxpoints; ii++) {
                    xcs[ii] = this.data[kk][ii];
                }
                for (int ii = 0; ii < nxpoints; ii++) {
                    ycs[ii] = this.data[kk + 1][ii];
                }
                csstep = (ycs[nxpoints - 1] - ycs[0]) / (niPoints - 1);
                ics[0] = ycs[0];
                for (int ii = 1; ii < niPoints; ii++) {
                    ics[ii] = ics[ii - 1] + csstep;
                }
                ics[niPoints - 1] = ycs[nxpoints - 1];
                cs.resetData(ycs, xcs);
                cs.calcDeriv();
                xoldpoint = xBot + (int) (((xcs[0] - xLow) / xdenom) * xLen);
                yoldpoint = yBot - (int) (((ycs[0] - yLow) / ydenom) * yLen);
                for (int ii = 1; ii < niPoints; ii++) {
                    ynewpoint = yBot + (int) (((ics[ii] - yLow) / ydenom) * yLen);
                    xnewpoint = xBot - (int) (((cs.interpolate(ics[ii]) - xLow) / xdenom) * xLen);
                    btest2 = printCheck(trimOpt[i], xoldpoint, xnewpoint, yoldpoint, ynewpoint);
                    if (btest2) {
                        if (this.lineOpt[i] == 2) {
                            dsum++;
                            if (dsum > dashLength[i]) {
                                dsum = 0;
                                if (dcheck) {
                                    dcheck = false;
                                } else {
                                    dcheck = true;
                                }
                            }
                        }
                        if (dcheck) g.drawLine(xoldpoint, yoldpoint, xnewpoint, ynewpoint);
                    }
                    xoldpoint = xnewpoint;
                    yoldpoint = ynewpoint;
                }
            }
            if (lineOpt[i] == 3) {
                dsum = 0;
                dcheck = true;
                xoldpoint = xBot + (int) ((((this.data[kk][0]) - xLow) / xdenom) * xLen);
                yoldpoint = yBot - (int) ((((this.data[kk + 1][0]) - yLow) / ydenom) * yLen);
                for (int ii = 1; ii < nxpoints; ii++) {
                    xnewpoint = xBot + (int) ((((this.data[kk][ii]) - xLow) / xdenom) * xLen);
                    ynewpoint = yBot - (int) ((((this.data[kk + 1][ii]) - yLow) / ydenom) * yLen);
                    btest2 = printCheck(trimOpt[i], xoldpoint, xnewpoint, yoldpoint, ynewpoint);
                    if (btest2) g.drawLine(xoldpoint, yoldpoint, xnewpoint, ynewpoint);
                    xoldpoint = xnewpoint;
                    yoldpoint = ynewpoint;
                }
            }
            if (lineOpt[i] == 4) {
                int[] lengths = new int[nxpoints - 1];
                double[] gradients = new double[nxpoints - 1];
                double[] intercepts = new double[nxpoints - 1];
                int totalLength = 0;
                xoldpoint = xBot + (int) ((((this.data[kk][0]) - xLow) / xdenom) * xLen);
                yoldpoint = yBot - (int) ((((this.data[kk + 1][0]) - yLow) / ydenom) * yLen);
                for (int ii = 1; ii < nxpoints; ii++) {
                    xnewpoint = xBot + (int) ((((this.data[kk][ii]) - xLow) / xdenom) * xLen);
                    ynewpoint = yBot - (int) ((((this.data[kk + 1][ii]) - yLow) / ydenom) * yLen);
                    lengths[ii - 1] = (int) Fmath.hypot((double) (xnewpoint - xoldpoint), (double) (ynewpoint - yoldpoint));
                    totalLength += lengths[ii - 1];
                    gradients[ii - 1] = (double) (ynewpoint - yoldpoint) / (double) (xnewpoint - xoldpoint);
                    intercepts[ii - 1] = (double) yoldpoint - gradients[ii - 1] * xoldpoint;
                    xoldpoint = xnewpoint;
                    yoldpoint = ynewpoint;
                }
                int incrmt = totalLength / (4 * niPoints - 1);
                int nlpointsold = 0;
                int nlpointsnew = 0;
                int totalLpoints = 1;
                for (int ii = 1; ii < nxpoints; ii++) {
                    totalLpoints++;
                    nlpointsnew = lengths[ii - 1] / incrmt;
                    for (int jj = nlpointsold + 1; jj < (nlpointsnew + nlpointsold); jj++) totalLpoints++;
                    nlpointsold = nlpointsold + nlpointsnew;
                }
                int[] xdashed = new int[totalLpoints];
                int[] ydashed = new int[totalLpoints];
                nlpointsold = 0;
                nlpointsnew = 0;
                xdashed[0] = xBot + (int) ((((this.data[kk][0]) - xLow) / xdenom) * xLen);
                ydashed[0] = yBot - (int) ((((this.data[kk + 1][0]) - yLow) / ydenom) * yLen);
                for (int ii = 1; ii < nxpoints; ii++) {
                    nlpointsnew = lengths[ii - 1] / incrmt;
                    xdashed[nlpointsnew + nlpointsold] = xBot + (int) ((((this.data[kk][ii]) - xLow) / xdenom) * xLen);
                    ydashed[nlpointsnew + nlpointsold] = yBot - (int) ((((this.data[kk + 1][ii]) - yLow) / ydenom) * yLen);
                    if (Math.abs(gradients[ii - 1]) > 0.5) {
                        int diff = (ydashed[nlpointsnew + nlpointsold] - ydashed[nlpointsold]) / nlpointsnew;
                        for (int jj = nlpointsold + 1; jj < (nlpointsnew + nlpointsold); jj++) {
                            ydashed[jj] = ydashed[jj - 1] + diff;
                            if (Fmath.isInfinity(Math.abs(gradients[ii - 1]))) {
                                xdashed[jj] = xdashed[nlpointsnew + nlpointsold];
                            } else {
                                xdashed[jj] = (int) (((double) ydashed[jj] - intercepts[ii - 1]) / gradients[ii - 1]);
                            }
                        }
                    } else {
                        int diff = (xdashed[nlpointsnew + nlpointsold] - xdashed[nlpointsold]) / nlpointsnew;
                        for (int jj = nlpointsold + 1; jj < (nlpointsnew + nlpointsold); jj++) {
                            xdashed[jj] = xdashed[jj - 1] + diff;
                            ydashed[jj] = (int) (gradients[ii - 1] * ydashed[jj] + intercepts[ii - 1]);
                        }
                    }
                    nlpointsold = nlpointsold + nlpointsnew;
                }
                dsum = 0;
                dcheck = true;
                for (int ii = 1; ii < totalLpoints; ii++) {
                    dsum++;
                    if (dsum > dashLength[i]) {
                        dsum = 0;
                        if (dcheck) {
                            dcheck = false;
                        } else {
                            dcheck = true;
                        }
                    }
                    if (dcheck) g.drawLine(xdashed[ii - 1], ydashed[ii - 1], xdashed[ii], ydashed[ii]);
                }
            }
            if (pointOpt[i] > 0) {
                for (int ii = 0; ii < nxpoints; ii++) {
                    ps = this.pointSize[i];
                    psh = ps / 2;
                    xxp = xBot + (int) (((this.data[kk][ii] - xLow) / xdenom) * xLen);
                    yyp = yBot - (int) (((this.data[kk + 1][ii] - yLow) / ydenom) * yLen);
                    switch(pointOpt[i]) {
                        case 1:
                            g.drawOval(xxp - psh, yyp - psh, ps, ps);
                            break;
                        case 2:
                            g.drawRect(xxp - psh, yyp - psh, ps, ps);
                            break;
                        case 3:
                            g.drawLine(xxp - psh, yyp, xxp, yyp + psh);
                            g.drawLine(xxp, yyp + psh, xxp + psh, yyp);
                            g.drawLine(xxp + psh, yyp, xxp, yyp - psh);
                            g.drawLine(xxp, yyp - psh, xxp - psh, yyp);
                            break;
                        case 4:
                            g.fillOval(xxp - psh, yyp - psh, ps, ps);
                            break;
                        case 5:
                            g.fillRect(xxp - psh, yyp - psh, ps, ps);
                            break;
                        case 6:
                            for (int jj = 0; jj < psh; jj++) g.drawLine(xxp - jj, yyp - psh + jj, xxp + jj, yyp - psh + jj);
                            for (int jj = 0; jj <= psh; jj++) g.drawLine(xxp - psh + jj, yyp + jj, xxp + psh - jj, yyp + jj);
                            break;
                        case 7:
                            g.drawLine(xxp - psh, yyp - psh, xxp + psh, yyp + psh);
                            g.drawLine(xxp - psh, yyp + psh, xxp + psh, yyp - psh);
                            break;
                        case 8:
                            g.drawLine(xxp - psh, yyp, xxp + psh, yyp);
                            g.drawLine(xxp, yyp + psh, xxp, yyp - psh);
                            break;
                        default:
                            g.drawLine(xxp - psh, yyp - psh, xxp + psh, yyp + psh);
                            g.drawLine(xxp - psh, yyp + psh, xxp + psh, yyp - psh);
                            break;
                    }
                    if (this.errorBar[i]) {
                        yype = yBot - (int) (((errors[i][ii] - yLow) / ydenom) * yLen);
                        g.drawLine(xxp, yyp, xxp, yype);
                        g.drawLine(xxp - 4, yype, xxp + 4, yype);
                        yype = 2 * yyp - yype;
                        g.drawLine(xxp, yyp, xxp, yype);
                        g.drawLine(xxp - 4, yype, xxp + 4, yype);
                    }
                }
            }
            kk += 2;
        }
    }

    public static long getSerialVersionUID() {
        return Plot.serialVersionUID;
    }
}
