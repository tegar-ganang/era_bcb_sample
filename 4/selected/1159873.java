package shu.cms.colorspace.depend;

import java.awt.*;
import shu.cms.colorspace.independ.*;
import shu.math.*;
import shu.math.array.*;
import shu.util.*;
import java.util.*;
import shu.math.geometry.LinearFunction;
import shu.cms.plot.*;

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
 */
public final class RGB extends RGBBase {

    protected int getNumberBands() {
        return 3;
    }

    public Object clone() {
        RGB clone = (RGB) super.clone();
        clone.rgbColorSpace = this.rgbColorSpace;
        clone.fixed = this.fixed;
        clone.round = this.round;
        return clone;
    }

    public Channel getMaxChannel() {
        double[] rgbValues = getValues();
        int maxIndex = Maths.maxIndex(rgbValues);
        return Channel.getChannelByArrayIndex(maxIndex);
    }

    public Channel getMinChannel() {
        double[] rgbValues = getValues();
        int minIndex = Maths.minIndex(rgbValues);
        return Channel.getChannelByArrayIndex(minIndex);
    }

    public Channel getMedChannel() {
        return Channel.getBesidePrimaryChannel(this.getMaxChannel(), this.getMinChannel());
    }

    /**
   *
   * @param minChannel Channel
   * @param maxChannel Channel
   * @return Channel
   * @deprecated
   */
    public static Channel getMedChannel(Channel minChannel, Channel maxChannel) {
        int medIndex = 3 - (minChannel.index + maxChannel.index);
        return Channel.getChannel(medIndex);
    }

    public double getValue(Channel channel, RGB.MaxValue maxValue) {
        double[] values = this.getValues(new double[3], maxValue);
        switch(channel) {
            case R:
                return values[0];
            case G:
                return values[1];
            case B:
                return values[2];
            case W:
                RGBBase.Channel minCh = getMinChannel();
                return values[minCh.getArrayIndex()];
            case C:
                return Math.min(values[1], values[2]);
            case M:
                return Math.min(values[0], values[2]);
            case Y:
                return Math.min(values[0], values[1]);
            default:
                return -1;
        }
    }

    public double getValue(Channel channel) {
        switch(channel) {
            case R:
                return R;
            case G:
                return G;
            case B:
                return B;
            case W:
                RGBBase.Channel minCh = getMinChannel();
                return getValue(minCh);
            case C:
                return Math.min(G, B);
            case M:
                return Math.min(R, B);
            case Y:
                return Math.min(R, G);
            default:
                return -1;
        }
    }

    public void addValue(Channel channel, double addvalue) {
        if (channel.isPrimaryColorChannel()) {
            double nowvalue = this.getValue(channel);
            this.setValue(channel, nowvalue + addvalue);
        } else {
            Channel[] channels = channel.getPrimaryColorChannel(channel);
            for (Channel ch : channels) {
                addValue(ch, addvalue);
            }
        }
    }

    /**
   * �NRGB�P�ɥ[�Waddvalue. addvalue��MaxValue���A�O�PRGB��MaxValue�ۦP.
   * @param addvalue double
   */
    public void addValues(double addvalue) {
        double[] nowvalues = this.getValues();
        double[] newvalues = DoubleArray.plus(nowvalues, addvalue);
        this.setValues(newvalues);
    }

    /**
   * �NRGB�P�ɥ[�Waddvalue
   * �i��waddvalue��MaxValue���A.
   * @param addvalue double
   * @param maxValue MaxValue
   */
    public void addValues(double addvalue, RGB.MaxValue maxValue) {
        double[] nowvalues = this.getValues(new double[3], maxValue);
        double[] newvalues = DoubleArray.plus(nowvalues, addvalue);
        this.setValues(newvalues, maxValue);
    }

    public void setValue(Channel channel, double value) {
        switch(channel) {
            case R:
                R = value;
                break;
            case G:
                G = value;
                break;
            case B:
                B = value;
                break;
            case C:
                B = G = value;
                break;
            case M:
                R = B = value;
                break;
            case Y:
                R = G = value;
                break;
            case W:
                R = G = B = value;
                break;
        }
    }

    public void setValue(Channel channel, double value, MaxValue type) {
        double[] values = new double[] { value };
        if (type != maxValue) {
            changeMaxValue(values, type, maxValue, false);
        }
        this.setValue(channel, values[0]);
    }

    public double R;

    public double G;

    public double B;

    public boolean isWhite() {
        return this.isAtMaxValue();
    }

    public boolean isBlack() {
        return R == 0. && G == 0. && B == 0.;
    }

    public boolean isGray() {
        return R == G && G == B;
    }

    public RGB() {
        this(RGB.ColorSpace.unknowRGB, null, MaxValue.Double1);
    }

    public RGB(RGB.ColorSpace colorSpace) {
        this(colorSpace, null, MaxValue.Double1);
    }

    public RGB(RGB.ColorSpace colorSpace, CIEXYZ XYZ) {
        this(colorSpace, fromXYZValues(XYZ.getValues(), colorSpace), MaxValue.Double1);
    }

    public RGB(RGB.ColorSpace colorSpace, Color c) {
        this(colorSpace, new int[] { c.getRed(), c.getGreen(), c.getBlue() });
    }

    public RGB(RGB.ColorSpace colorSpace, MaxValue maxValue) {
        this(colorSpace, null, maxValue);
    }

    public RGB(RGB.ColorSpace colorSpace, double[] rgbValues, MaxValue maxValue) {
        super(colorSpace, maxValue);
        if (rgbValues != null) {
            setValues(rgbValues);
        }
    }

    public RGB(RGB.ColorSpace colorSpace, double[] rgb) {
        this(colorSpace, rgb, MaxValue.Double1);
    }

    public RGB(RGB.ColorSpace colorSpace, int[] rgb) {
        this(colorSpace, intRGB2double(rgb), MaxValue.Int8Bit);
    }

    public RGB(int r, int g, int b) {
        this(RGB.ColorSpace.unknowRGB, new double[] { r, g, b }, MaxValue.Int8Bit);
    }

    public RGB(double r, double g, double b) {
        this(RGB.ColorSpace.unknowRGB, new double[] { r, g, b }, MaxValue.Double255);
    }

    protected final double[] _getValues(double[] values) {
        values[0] = R;
        values[1] = G;
        values[2] = B;
        return values;
    }

    private static double[] intRGB2double(int[] rgb) {
        double[] doubleRGB = new double[3];
        for (int x = 0; x < 3; x++) {
            doubleRGB[x] = (double) rgb[x];
        }
        return doubleRGB;
    }

    public void setValues(double[] values, MaxValue type) {
        if (values.length != 3) {
            throw new IllegalArgumentException("values.length != 3");
        }
        if (type != maxValue) {
            values = DoubleArray.copy(values);
            changeMaxValue(values, type, maxValue, false);
        }
        setValues(values);
    }

    public final void setColorBlack() {
        this.setColor(Color.black);
    }

    public final void reserveValue(RGBBase.Channel channel) {
        double val = this.getValue(channel);
        this.setColorBlack();
        this.setValue(channel, val);
    }

    public final void setColor(Color color) {
        double[] rgbValues = new double[3];
        rgbValues[0] = color.getRed();
        rgbValues[1] = color.getGreen();
        rgbValues[2] = color.getBlue();
        setValues(rgbValues, RGB.MaxValue.Int8Bit);
    }

    public final Color getColor() {
        double[] rgbValues = new double[3];
        getValues(rgbValues, RGB.MaxValue.Double1);
        this.rationalize(rgbValues, RGB.MaxValue.Double1);
        Color c = new Color((float) rgbValues[0], (float) rgbValues[1], (float) rgbValues[2]);
        return c;
    }

    protected void _setValues(double[] values) {
        R = values[0];
        G = values[1];
        B = values[2];
    }

    /**
   * �W�D���t��0��
   * @return boolean
   */
    public boolean hasZeroChannel() {
        return R == 0 || G == 0 || B == 0;
    }

    /**
   * �u���@���W�D����
   * @return boolean
   */
    public boolean isPrimaryChannel() {
        int hasValueCount = 0;
        double[] RGBValues = this.getValues();
        for (double d : RGBValues) {
            if (d != 0) {
                hasValueCount++;
            }
        }
        return hasValueCount == 1;
    }

    /**
   * �O�_�u���ɦ��W�D����
   * @return boolean
   */
    public boolean isSecondaryChannel() {
        return hasOnlyOneZeroChannel();
    }

    public Channel getSecondaryChannel() {
        if (!isSecondaryChannel()) {
            return null;
        }
        Channel[] ch = Channel.getBesidePrimaryChannel(getMinChannel());
        return Channel.getSecondaryChannel(ch[0], ch[1]);
    }

    public boolean hasOnlyOneZeroChannel() {
        return getZeroChannelCount() == 1;
    }

    /**
   * �O�_���W�D�w�g���M
   * @return boolean
   */
    public final boolean hasChannelSaturation() {
        double saturation = maxValue.max;
        return R == saturation || G == saturation || B == saturation;
    }

    /**
   * ��F���M���W�D�ƶq
   * @return int
   */
    public final int getSaturationChannelCount() {
        int channels = 0;
        double saturation = maxValue.max;
        channels += R == saturation ? 1 : 0;
        channels += G == saturation ? 1 : 0;
        channels += B == saturation ? 1 : 0;
        return channels;
    }

    public final int getZeroChannelCount() {
        int channels = 0;
        channels += R == 0 ? 1 : 0;
        channels += G == 0 ? 1 : 0;
        channels += B == 0 ? 1 : 0;
        return channels;
    }

    public static void main(String[] args) {
        CIEXYZ XYZ = RGB.toXYZ(RGB.Red, RGB.ColorSpace.CIERGB);
        double[] rgbValues = RGB.fromXYZValuesToRationalRGBValues(XYZ.getValues(), RGB.ColorSpace.sRGB);
    }

    public double[] getRGrg() {
        return new double[] { getRGr(), getRGg() };
    }

    protected double getRGr() {
        double total = R + G + B;
        return total == 0 ? 0 : R / total;
    }

    protected double getRGg() {
        double total = R + G + B;
        return total == 0 ? 0 : G / total;
    }

    public double getRG() {
        return R - G;
    }

    public double getYB() {
        return .5 * (R + G) - B;
    }

    public boolean isAtMaxValue() {
        double[] values = this.getValues();
        for (double d : values) {
            if (d != maxValue.max) {
                return false;
            }
        }
        return true;
    }

    public MaxValue getMaxValue() {
        return maxValue;
    }

    public static final void quantization(RGB[] rgbArray, RGB.MaxValue maxValue) {
        int size = rgbArray.length;
        for (int x = 0; x < size; x++) {
            RGB rgb = rgbArray[x];
            rgb.quantization(maxValue);
        }
    }

    /**
   * ���X�z�Ȫ��`�M
   * @return double
   */
    public double irrationalValueSum() {
        double irrationalValue = 0;
        irrationalValue += (R > maxValue.max) ? R - maxValue.max : 0;
        irrationalValue += (R < 0) ? 0 - R : 0;
        irrationalValue += (G > maxValue.max) ? G - maxValue.max : 0;
        irrationalValue += (G < 0) ? 0 - G : 0;
        irrationalValue += (B > maxValue.max) ? B - maxValue.max : 0;
        irrationalValue += (B < 0) ? 0 - B : 0;
        return irrationalValue;
    }

    public void clip() {
        rationalize();
    }

    public final int[] getInteger255Values() {
        int[] values = new int[3];
        values[0] = (int) this.getValue(RGBBase.Channel.R, RGB.MaxValue.Int8Bit);
        values[1] = (int) this.getValue(RGBBase.Channel.G, RGB.MaxValue.Int8Bit);
        values[2] = (int) this.getValue(RGBBase.Channel.B, RGB.MaxValue.Int8Bit);
        return values;
    }

    public final short[] get10BitValues() {
        double[] values = getValues(new double[3], MaxValue.Int10Bit);
        short[] values10Bit = new short[3];
        values10Bit[0] = (short) values[0];
        values10Bit[1] = (short) values[1];
        values10Bit[2] = (short) values[2];
        return values10Bit;
    }

    public final double[] getValues(double[] values, MaxValue type) {
        getValues(values);
        if (type != maxValue) {
            changeMaxValue(values, this.maxValue, type, false);
        }
        return values;
    }

    /**
   * ��o�ɼ�
   * @return RGB
   */
    public RGB getComplementary() {
        RGB clone = (RGB) this.clone();
        double max = clone.getMaxValue().max;
        clone.R = max - clone.R;
        clone.G = max - clone.G;
        clone.B = max - clone.B;
        return clone;
    }

    /**
   * �O�_�O�D���
   * @return boolean
   */
    public final boolean isPrimaryHue() {
        return getSaturationChannelCount() == 1 && getZeroChannelCount() == 1;
    }

    /**
   * �ХܬO�_���L�ץ�
   */
    protected boolean fixed = false;

    /**
   * �O�_���L�ץ�
   * @return boolean
   */
    public final boolean isFixed() {
        return fixed;
    }

    public final void setFixed(boolean fixed) {
        this.fixed = fixed;
    }

    public String toString() {
        if (maxValue.integer) {
            double[] vals = getValues();
            int size = vals.length;
            int[] intVals = new int[size];
            for (int x = 0; x < size; x++) {
                intVals[x] = (int) vals[x];
            }
            return "(" + Utils.toString(intVals).trim() + ")";
        } else {
            return super.toString();
        }
    }

    /**
   * Indicates whether some other object is "equal to" this one.
   *
   * @param obj the reference object with which to compare.
   * @return <code>true</code> if this object is the same as the obj argument;
   *   <code>false</code> otherwise.
   */
    public boolean equals(Object obj) {
        if (!(obj instanceof RGB)) {
            return false;
        }
        RGB that = (RGB) obj;
        if (this.rgbColorSpace.equals(that.rgbColorSpace) && this.R == that.R && this.G == that.G && this.B == that.B && this.maxValue == that.maxValue) {
            return true;
        } else {
            return false;
        }
    }

    /**
   * �I�s������P�Q�q�ƫ�O�_�۵�
   * @param maxValue MaxValue
   * @return boolean
   */
    public boolean equalsAfterQuantization(RGB.MaxValue maxValue) {
        RGB that = (RGB) this.clone();
        that.quantization(maxValue);
        return this.equals(that);
    }

    /**
   * �I�s������Prgb�����q�Ƥ���O�_�۵�
   * @param rgb RGB
   * @param maxValue MaxValue
   * @return boolean
   */
    public boolean equalsAfterQuantization(RGB rgb, RGB.MaxValue maxValue) {
        RGB ths = (RGB) this.clone();
        RGB that = (RGB) rgb.clone();
        if (ths.rgbColorSpace != that.rgbColorSpace) {
            return false;
        }
        ths.changeMaxValue(maxValue);
        that.changeMaxValue(maxValue);
        return ths.equals(that);
    }

    /**
   * Returns a hash code value for the object.
   *
   * @return a hash code value for this object.
   */
    public int hashCode() {
        if (rgbColorSpace == null) {
            throw new IllegalStateException("rgbColorSpace == null");
        }
        int hashCode = this.rgbColorSpace.hashCode() + Double.valueOf(R).hashCode() + Double.valueOf(G).hashCode() + Double.valueOf(B).hashCode() + maxValue.hashCode();
        return hashCode;
    }

    /**
   * RGB�ƭȬO�_�X�k
   * @return boolean
   */
    public boolean isLegal() {
        return isLegal(R) && isLegal(G) && isLegal(B);
    }

    /**
   * �ƭȬO�_�X�k
   * @param value double
   * @return boolean
   */
    protected final boolean isLegal(double value) {
        return isLegal(value, maxValue);
    }

    /**
   * �ƭȬO�_�X�k(>0�B<�̤j��)
   * @param value double
   * @param maxValue MaxValue
   * @return boolean
   */
    public static final boolean isLegal(double value, MaxValue maxValue) {
        return value >= 0 && value <= maxValue.max && !Double.isNaN(value);
    }

    /**
   * �HmaxValue�q�Ƥ���O�_�X�k
   * @param maxValue MaxValue
   * @return boolean
   */
    public final boolean isLegalAfterQuantization(MaxValue maxValue) {
        double[] normal100 = normalizeTo100(this.getValues(), maxValue);
        double[] integer = changeIntegerMaxValue(normal100, this.maxValue, maxValue, true);
        return isLegal(integer[0], maxValue) && isLegal(integer[1], maxValue) && isLegal(integer[2], maxValue);
    }

    /**
   * 8bit�q�Ƥ���O�_�X�k
   * @return boolean
   */
    public boolean isLegalAfter8BitQuantization() {
        return isLegalAfterQuantization(MaxValue.Int8Bit);
    }

    /**
   * 16bit�q�Ƥ���O�_�X�k
   * @return boolean
   */
    public boolean isLegalAfter16BitQuantization() {
        return isLegalAfterQuantization(MaxValue.Int16Bit);
    }

    /**
   * 10bit�q�Ƥ���O�_�X�k
   * @return boolean
   */
    public boolean isLegalAfter10BitQuantization() {
        return isLegalAfterQuantization(MaxValue.Int10Bit);
    }

    public static final double[] rationalize(final double[] rgbValues, RGB.MaxValue maxValue) {
        rgbValues[0] = rationalize(rgbValues[0], maxValue);
        rgbValues[1] = rationalize(rgbValues[1], maxValue);
        rgbValues[2] = rationalize(rgbValues[2], maxValue);
        return rgbValues;
    }

    /**
   * ��rgb�i��X�z��
   * @param rgb RGB
   * @return RGB
   */
    public static final boolean rationalize(RGB rgb) {
        double r = rationalize(rgb.R, rgb.maxValue);
        double g = rationalize(rgb.G, rgb.maxValue);
        double b = rationalize(rgb.B, rgb.maxValue);
        boolean doRational = (r != rgb.R || g != rgb.G || b != rgb.B);
        rgb.R = r;
        rgb.G = g;
        rgb.B = b;
        return doRational;
    }

    /**
   * ��rgb�i��X�z��
   * @return boolean �_�u���i��rational
   */
    public boolean rationalize() {
        return rationalize(this);
    }

    /**
   * �NRGB�X�z��,R/G/B���Ӹ��b0~1��,�p�G�W�X���d��,�N�?�s�b�ӭ�
   * @param RGBValuesArray double[][]
   * @param maxValue MaxValue
   * @return double[][]
   */
    public static final double[][] rationalize(double[][] RGBValuesArray, RGB.MaxValue maxValue) {
        int size = RGBValuesArray.length;
        for (int x = 0; x < size; x++) {
            double[] rgbValues = RGBValuesArray[x];
            rgbValues[0] = rationalize(rgbValues[0], maxValue);
            rgbValues[1] = rationalize(rgbValues[1], maxValue);
            rgbValues[2] = rationalize(rgbValues[2], maxValue);
        }
        return RGBValuesArray;
    }

    /**
   * GCR
   * @return double[]
   */
    public final double[] grayComponentReplacement() {
        double gray = this.getValue(RGBBase.Channel.W);
        double[] gcr = getValues();
        return DoubleArray.minus(gcr, gray);
    }

    public static final double[] fromXYZValues(double[] XYZValues, RGBBase.ColorSpace colorSpace) {
        double[] linearRGBValues = XYZ2LinearRGBValues(XYZValues, colorSpace);
        double[] rgbValues = linearToRGBValues(linearRGBValues, colorSpace);
        return rgbValues;
    }

    private static final boolean isLegal(double[] normalizedRGBValues) {
        boolean legal = isLegal(normalizedRGBValues[0], RGB.MaxValue.Double1) && isLegal(normalizedRGBValues[1], RGB.MaxValue.Double1) && isLegal(normalizedRGBValues[2], RGB.MaxValue.Double1);
        return legal;
    }

    private static final boolean isNegative(double[] normalizedRGBValues) {
        boolean negative = normalizedRGBValues[0] < 0 || normalizedRGBValues[1] < 0 || normalizedRGBValues[2] < 0;
        return negative;
    }

    public static final double[] fromXYZValuesToRationalRGBValues(double[] XYZValues, RGBBase.ColorSpace colorSpace) {
        double[] rgbValues = fromXYZValues(XYZValues, colorSpace);
        boolean legal = isLegal(rgbValues);
        if (legal) {
            return rgbValues;
        } else {
            CIEXYZ whiteXYZ = colorSpace.getReferenceWhiteXYZ();
            double[] whitexyValues = whiteXYZ.getxyValues();
            CIEXYZ XYZ = new CIEXYZ(XYZValues);
            double[] xyValues = XYZ.getxyValues();
            LinearFunction lf = LinearFunction.getInstance(whitexyValues, xyValues);
            for (int p = 1; p < 100; p++) {
                double dx = p / 100. * (whitexyValues[0] - xyValues[0]);
                double x = xyValues[0] + dx;
                double y = lf.getY(x);
                double[] newxyYValues = new double[] { x, y, 1 };
                CIExyY xyY = new CIExyY(newxyYValues);
                double[] newXYZValues = xyY.toXYZ().getValues();
                double[] whiteXYZValues = whiteXYZ.getValues();
                int index = newXYZValues[0] > newXYZValues[2] ? 0 : 2;
                double factor = whiteXYZValues[index] / newXYZValues[index];
                newXYZValues = DoubleArray.times(newXYZValues, factor);
                rgbValues = fromXYZValues(newXYZValues, colorSpace);
                while (!isNegative(rgbValues)) {
                    newXYZValues = DoubleArray.times(newXYZValues, 0.99);
                    rgbValues = fromXYZValues(newXYZValues, colorSpace);
                    legal = isLegal(rgbValues);
                    if (legal) {
                        return rgbValues;
                    }
                }
                legal = isLegal(rgbValues);
                if (legal) {
                    return rgbValues;
                }
            }
            return null;
        }
    }

    public static final RGB fromXYZ(CIEXYZ XYZ, RGB.ColorSpace colorSpace) {
        return fromXYZ(XYZ, colorSpace, false);
    }

    public static final RGB fromXYZ(CIEXYZ XYZ, RGB.ColorSpace colorSpace, boolean rationalRGB) {
        double[] XYZValues = null;
        if (XYZ.getNormalizeY() != NormalizeY.Not) {
            XYZValues = XYZ.getValues(new double[3], NormalizeY.Normal1);
        } else {
            XYZValues = XYZ.getValues();
        }
        double[] rgbValues = rationalRGB ? fromXYZValuesToRationalRGBValues(XYZValues, colorSpace) : fromXYZValues(XYZValues, colorSpace);
        RGB rgb = new RGB(colorSpace, rgbValues, RGB.MaxValue.Double1);
        rgbValues = null;
        return rgb;
    }

    public static final double[] toXYZValues(double[] rgbValues, RGB.ColorSpace colorSpace) {
        double[] actualRGBValues = toLinearRGBValues(rgbValues, colorSpace);
        double[] XYZValues = linearToXYZValues(actualRGBValues, colorSpace);
        return XYZValues;
    }

    public static final double[] linearToXYZValues(double[] rgbValues, RGB.ColorSpace colorSpace) {
        double[] XYZValues = DoubleArray.times(rgbValues, colorSpace.toXYZMatrix);
        return XYZValues;
    }

    public final CIEXYZ toXYZ() {
        return toXYZ(this);
    }

    public final CIEXYZ toXYZ(RGB.ColorSpace rgbColorSpace) {
        return toXYZ(this, rgbColorSpace);
    }

    public static final CIEXYZ toXYZ(final RGB rgb) {
        return toXYZ(rgb, rgb.rgbColorSpace);
    }

    public static final CIEXYZ toXYZ(final RGB rgb, RGB.ColorSpace rgbColorSpace) {
        double[] rgbValues = rgb.getValues(new double[3], RGB.MaxValue.Double1);
        double[] XYZValues = toXYZValues(rgbValues, rgbColorSpace);
        CIEXYZ XYZ = new CIEXYZ(XYZValues, rgbColorSpace.getReferenceWhiteXYZ(), NormalizeY.Normal1);
        XYZValues = null;
        return XYZ;
    }

    private static final long serialVersionUID = 8683452581122892183L;

    public String[] getBandNames() {
        return new String[] { "R", "G", "B" };
    }

    public static final RGB Black = new RGB(RGB.ColorSpace.unknowRGB, Color.black);

    public static final RGB Gray = new RGB(RGB.ColorSpace.unknowRGB, Color.gray);

    public static final RGB White = new RGB(RGB.ColorSpace.unknowRGB, Color.white);

    public static final RGB Red = new RGB(RGB.ColorSpace.unknowRGB, Color.red);

    public static final RGB Green = new RGB(RGB.ColorSpace.unknowRGB, Color.green);

    public static final RGB Blue = new RGB(RGB.ColorSpace.unknowRGB, Color.blue);

    public static final RGB Cyan = new RGB(RGB.ColorSpace.unknowRGB, Color.cyan);

    public static final RGB Magenta = new RGB(RGB.ColorSpace.unknowRGB, Color.magenta);

    public static final RGB Yellow = new RGB(RGB.ColorSpace.unknowRGB, Color.yellow);
}
