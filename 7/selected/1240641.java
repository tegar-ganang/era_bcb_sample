package shu.cms.devicemodel.lcd;

import java.util.List;
import java.awt.*;
import shu.cms.*;
import shu.cms.colorspace.depend.*;
import shu.cms.colorspace.independ.*;
import shu.cms.devicemodel.lcd.LCDModelBase.*;
import shu.cms.lcd.*;
import shu.cms.plot.*;
import shu.math.*;
import shu.math.array.*;
import shu.math.lut.*;
import shu.math.regress.*;

/**
 * <p>Title: Colour Management System</p>
 *
 * <p>Description: a Colour Management System by Java</p>
 *
 * <p>Copyright: Copyright (c) 2009</p>
 *
 * <p>Company: skygroup</p>
 *
 * @author skyforce
 * @version 1.0
 */
public class ChromaticityModel extends LCDModel {

    static {
        ChromaticityModel.setFlareType(LCDModel.FlareType.Black);
    }

    /**
   * �D�ȼҦ�
   *
   * @param lcdTarget LCDTarget
   */
    public ChromaticityModel(LCDTarget lcdTarget) {
        super(lcdTarget);
    }

    /**
   * �p��RGB,�ϱ��Ҧ�
   *
   * @param relativeXYZ CIEXYZ
   * @param factor Factor[]
   * @return RGB
   */
    protected RGB _getRGB(CIEXYZ relativeXYZ, Factor[] factor) {
        throw new java.lang.UnsupportedOperationException();
    }

    /**
   * �p��XYZ,�e�ɼҦ�
   *
   * @param rgb RGB
   * @param factor Factor[]
   * @return CIEXYZ
   */
    protected CIEXYZ _getXYZ(RGB rgb, Factor[] factor) {
        rgb.getValues(rgbValues, RGB.MaxValue.Double255);
        CIEXYZ[] rgbXYZ = new CIEXYZ[3];
        for (RGB.Channel ch : RGB.Channel.RGBChannel) {
            int index = ch.getArrayIndex();
            double input = rgbValues[index];
            double x = luts[index * 3].getValue(input);
            double y = luts[index * 3 + 1].getValue(input);
            double Y = luts[index * 3 + 2].getValue(input);
            CIExyY xyY = new CIExyY(x, y, Y);
            rgbXYZ[index] = xyY.toXYZ();
        }
        CIEXYZ result = CIEXYZ.plus(rgbXYZ[0], rgbXYZ[1]);
        result = CIEXYZ.plus(result, rgbXYZ[2]);
        CIEXYZ flareXYZ = (CIEXYZ) this.flare.flareXYZ.clone();
        flareXYZ.times(3);
        result = CIEXYZ.minus(result, flareXYZ);
        return result;
    }

    private double[] rgbValues = new double[3];

    private Interpolation1DLUT[] luts = new Interpolation1DLUT[9];

    private PolynomialRegression[][] regress = new PolynomialRegression[3][2];

    private double[][] luminanceSCurveParams = new double[3][];

    private double[] pureMaxLuminance = new double[3];

    private int smoothTimes = 3;

    private boolean smoothByCubic = true;

    public void setSmoothByCubic(boolean byCubic) {
        this.smoothByCubic = byCubic;
    }

    public void setSmoothTimes(int smoothTimes) {
        this.smoothTimes = smoothTimes;
    }

    private static void smooth(double[] curve, int times) {
        int size = curve.length;
        for (int t = 0; t < times; t++) {
            for (int x = 1; x < size - 1; x++) {
                curve[x] = (curve[x - 1] + curve[x + 1]) / 2;
            }
        }
    }

    public static void smoothByCubic(double[] curve, int times) {
        int size = curve.length;
        double[] xn = new double[] { 0, 1, 3, 4 };
        double[] yn = new double[4];
        for (int t = 0; t < times; t++) {
            for (int x = 2; x < size - 2; x++) {
                yn[0] = curve[x - 2];
                yn[1] = curve[x - 1];
                yn[2] = curve[x + 1];
                yn[3] = curve[x + 2];
                curve[x] = Interpolation.cubic(xn, yn, 2);
            }
        }
    }

    public static double[] getSmoothCurve(double[] originalCurve, double[] deltaCurve) {
        int size = originalCurve.length;
        double delta = originalCurve[size - 1] - originalCurve[0];
        double sumOfDelta = Maths.sum(deltaCurve);
        double factor = delta / sumOfDelta;
        double[] smoothCurve = new double[size];
        smoothCurve[0] = originalCurve[0];
        for (int x = 1; x < size; x++) {
            smoothCurve[x] = smoothCurve[x - 1] + deltaCurve[x] * factor;
        }
        return smoothCurve;
    }

    protected Factor[] _produceFactor() {
        boolean plotting = true;
        RGB.Channel[] chs = RGB.Channel.RGBChannel;
        Plot2D.setSkin(PlotUtils.AUOSkin);
        Plot2D plot = plotting ? Plot2D.getInstance() : null;
        Plot2D dplot = plotting ? Plot2D.getInstance() : null;
        for (RGB.Channel ch : chs) {
            java.awt.Color c = ch.color;
            List<Patch> grayScalePatch = lcdTarget.filter.grayScalePatch(ch, true);
            int size = grayScalePatch.size();
            int index = ch.getArrayIndex();
            double[] intputGrayLevel = new double[size];
            double[] outputx = new double[size];
            double[] outputy = new double[size];
            double[] outputY = new double[size];
            double[] outputdx = new double[size];
            double[] outputdy = new double[size];
            double[] outputdY = new double[size];
            CIExyY prexyY = null;
            for (int x = 0; x < size; x++) {
                intputGrayLevel[x] = x;
                CIEXYZ XYZ = grayScalePatch.get(x).getXYZ();
                CIExyY xyY = new CIExyY(XYZ);
                outputx[x] = xyY.x;
                outputy[x] = xyY.y;
                outputY[x] = xyY.Y;
                if (null != prexyY) {
                    outputdx[x] = xyY.x - prexyY.x;
                    outputdy[x] = xyY.y - prexyY.y;
                    outputdY[x] = xyY.Y - prexyY.Y;
                }
                prexyY = xyY;
            }
            if (smoothByCubic) {
                smoothByCubic(outputdx, smoothTimes);
                smoothByCubic(outputdy, smoothTimes);
                smoothByCubic(outputdY, smoothTimes);
            } else {
                smooth(outputdx, smoothTimes);
                smooth(outputdy, smoothTimes);
                smooth(outputdY, smoothTimes);
            }
            outputx = getSmoothCurve(outputx, outputdx);
            outputy = getSmoothCurve(outputy, outputdy);
            outputY = getSmoothCurve(outputY, outputdY);
            if (plotting) {
                prexyY = null;
                for (int x = 0; x < size; x++) {
                    CIEXYZ XYZ = grayScalePatch.get(x).getXYZ();
                    CIExyY xyY = new CIExyY(XYZ);
                    plot.addCacheScatterLinePlot(ch.name(), xyY.x, xyY.y);
                    plot.addCacheScatterLinePlot(ch.name() + "smooth", outputx[x], outputy[x]);
                    if (x > 0) {
                        dplot.addCacheScatterLinePlot(ch.name() + "x_", Color.black, x, outputdx[x]);
                        dplot.addCacheScatterLinePlot(ch.name() + "y_", Color.black, x, outputdy[x]);
                    }
                    if (prexyY != null) {
                        dplot.addCacheScatterLinePlot(ch.name() + "x", c, x, xyY.x - prexyY.x);
                        dplot.addCacheScatterLinePlot(ch.name() + "y", c, x, xyY.y - prexyY.y);
                    }
                    prexyY = xyY;
                }
            }
            Interpolation1DLUT xlut = new Interpolation1DLUT(intputGrayLevel, outputx, Interpolation1DLUT.Algo.LINEAR);
            Interpolation1DLUT ylut = new Interpolation1DLUT(intputGrayLevel, outputy, Interpolation1DLUT.Algo.LINEAR);
            Interpolation1DLUT Ylut = new Interpolation1DLUT(intputGrayLevel, outputY, Interpolation1DLUT.Algo.LINEAR);
            luts[index * 3] = xlut;
            luts[index * 3 + 1] = ylut;
            luts[index * 3 + 2] = Ylut;
            if (plotting) {
                for (int x = 1; x < size; x++) {
                    double ciex = luts[index * 3].getValue(x);
                    double ciey = luts[index * 3 + 1].getValue(x);
                    plot.addCacheScatterLinePlot("mod-" + ch.name(), ciex, ciey);
                }
            }
        }
        if (plotting) {
            plot.setVisible();
            dplot.setVisible();
        }
        return new Factor[3];
    }

    /**
   * �D�Y��
   *
   * @return Factor[]
   */
    protected Factor[] _produceFactor_() {
        boolean plotting = true;
        CIEXYZ flareXYZ = this.flare.getFlare();
        RGB.Channel[] chs = RGB.Channel.RGBChannel;
        double[][][] rgbxyYCurve = new double[3][3][];
        Plot2D plot = plotting ? Plot2D.getInstance() : null;
        for (RGB.Channel ch : chs) {
            List<Patch> grayScalePatch = lcdTarget.filter.grayScalePatch(ch, true);
            int size = grayScalePatch.size();
            int n = size - 1;
            int index = ch.getArrayIndex();
            double[] inputx = new double[n];
            double[] inputy = new double[n];
            rgbxyYCurve[index] = new double[3][n];
            for (int x = 1; x < size; x++) {
                CIEXYZ XYZ = grayScalePatch.get(x).getXYZ();
                CIExyY xyY = new CIExyY(XYZ);
                rgbxyYCurve[index][0][x - 1] = xyY.x;
                rgbxyYCurve[index][1][x - 1] = xyY.y;
                rgbxyYCurve[index][2][x - 1] = XYZ.Y;
                inputx[x - 1] = x;
                inputy[x - 1] = xyY.x;
                if (plotting) {
                    plot.addCacheScatterLinePlot(ch.name(), xyY.x, xyY.y);
                }
            }
            double[] luminanceCurve = rgbxyYCurve[index][2];
            double[] relativeLuminanceCurve = DoubleArray.minus(luminanceCurve, flareXYZ.Y);
            double max = relativeLuminanceCurve[relativeLuminanceCurve.length - 1];
            pureMaxLuminance[index] = max;
            Maths.normalize(relativeLuminanceCurve, max);
            double[] YParam = SCurveModel.estimateSCurveParameter(relativeLuminanceCurve);
            luminanceSCurveParams[index] = YParam;
            PolynomialRegression xRegress = new PolynomialRegression(inputx, rgbxyYCurve[index][0], Polynomial.COEF_1.BY_4C);
            PolynomialRegression yRegress = new PolynomialRegression(inputy, rgbxyYCurve[index][1], Polynomial.COEF_1.BY_4C);
            xRegress.regress();
            yRegress.regress();
            regress[index][0] = xRegress;
            regress[index][1] = yRegress;
            if (plotting) {
                for (int x = 1; x < size; x++) {
                    double ciex = xRegress.getPredict(new double[] { x })[0];
                    double ciey = yRegress.getPredict(new double[] { ciex })[0];
                    plot.addCacheScatterLinePlot("mod", ciex, ciey);
                }
                plot.setVisible();
            }
        }
        return new Factor[3];
    }

    /**
   * getDescription
   *
   * @return String
   */
    public String getDescription() {
        return "";
    }

    public static void main(String[] args) {
        LCDTarget target = LCDTarget.Instance.getFromAUORampXLS("Measurement Files\\Monitor\\auo_T370HW02\\ca210\\darkroom\\native\\110922\\Measurement00_.xls", LCDTarget.Number.Ramp1024);
        int gl = 0;
        Patch p = target.getPatch(RGB.Channel.R, gl, RGB.MaxValue.Double255);
        CIEXYZ rXYZ = p.getXYZ();
        System.out.println(rXYZ);
        Patch bp = target.getBlackPatch();
        LCDTarget.Operator.gradationReverseFix(target);
        ChromaticityModel model = new ChromaticityModel(target);
        model.produceFactor();
        CIEXYZ XYZ = model.getXYZ(new RGB(gl, 0, 0), false);
        System.out.println(XYZ);
    }
}
