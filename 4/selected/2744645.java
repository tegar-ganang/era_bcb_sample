package shu.cms.colorspace.depend;

import java.io.*;
import java.awt.*;
import shu.cms.*;
import shu.cms.colorspace.depend.DeviceDependentSpace.*;
import shu.cms.colorspace.independ.*;
import shu.math.*;
import shu.math.array.*;

/**
 * <p>Title: Colour Management System</p>
 *
 * <p>Description: a Colour Management System by Java</p>
 * ����RGB�B�@����¦�س]
 *
 * <p>Copyright: Copyright (c) 2008</p>
 *
 * <p>Company: skygroup</p>
 *
 * @author skyforce
 * @version 1.0
 */
public abstract class RGBBase extends DeviceDependentSpace {

    /**
   *
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
    public static enum Channel {

        R(1, Color.red, "Red"), G(2, Color.green, "Green"), B(3, Color.blue, "Blue"), Y(4, Color.yellow, "Yellow"), M(5, Color.magenta, "Magenta"), C(6, Color.cyan, "Cyan"), W(7, Color.white, "White");

        public final Channel getChannel(boolean R, boolean G, boolean B) {
            int index = 0;
            index += R ? 1 : 0;
            index += G ? 2 : 0;
            index += B ? 3 : 0;
            return getChannel(index);
        }

        public final boolean isPrimaryColorChannel() {
            return isPrimaryColorChannel(this);
        }

        public final boolean isSecondaryColorChannel() {
            return isSecondaryColorChannel(this);
        }

        private static final boolean isPrimaryColorChannel(Channel channel) {
            return channel == R || channel == G || channel == B;
        }

        private static final boolean isSecondaryColorChannel(Channel channel) {
            return channel == C || channel == M || channel == Y;
        }

        /**
     * ��o�����ȱochannel(�]�N�OminChannel�PmaxChannel�H�~��channel)
     * @param minChannel Channel
     * @param maxChannel Channel
     * @return Channel
     */
        public static Channel getMedChannel(Channel minChannel, Channel maxChannel) {
            return getBesidePrimaryChannel(minChannel, maxChannel);
        }

        /**
     * ��o�D��զ�
     * @param colorChannel Channel
     * @return Channel[]
     */
        public static Channel[] getPrimaryColorChannel(Channel colorChannel) {
            switch(colorChannel) {
                case R:
                    return new Channel[] { R };
                case G:
                    return new Channel[] { G };
                case B:
                    return new Channel[] { B };
                case C:
                    return new Channel[] { G, B };
                case M:
                    return new Channel[] { R, B };
                case Y:
                    return new Channel[] { R, G };
                case W:
                    return new Channel[] { R, G, B };
                default:
                    return null;
            }
        }

        public static Channel getSecondaryChannel(Channel primaryChannel1, Channel primaryChannel2) {
            if (!primaryChannel1.isPrimaryColorChannel() || !primaryChannel2.isPrimaryColorChannel()) {
                throw new IllegalArgumentException("!primaryChannel1.isPrimaryColorChannel() || !primaryChannel2.isPrimaryColorChannel()");
            }
            if (primaryChannel1 == R && primaryChannel2 == G) {
                return Y;
            } else if (primaryChannel1 == R && primaryChannel2 == B) {
                return M;
            } else if (primaryChannel1 == G && primaryChannel2 == B) {
                return C;
            }
            return null;
        }

        /**
     * ��osecondaryColorChannel�H�~��channel.
     * @param secondaryColorChannel Channel
     * @return Channel
     */
        public static Channel getBesideSecondaryChannel(Channel secondaryColorChannel) {
            if (!secondaryColorChannel.isSecondaryColorChannel()) {
                throw new IllegalArgumentException("is not secondaryColorChannel");
            }
            int besideIndex = secondaryColorChannel.index - 3;
            return Channel.getChannel(besideIndex);
        }

        /**
     * ��ochannel1�Pchannel2�H�~��channel.
     * @param primaryColorChannel1 Channel
     * @param primaryColorChannel2 Channel
     * @return Channel
     */
        public static Channel getBesidePrimaryChannel(Channel primaryColorChannel1, Channel primaryColorChannel2) {
            if (!primaryColorChannel1.isPrimaryColorChannel() || !primaryColorChannel2.isPrimaryColorChannel()) {
                throw new IllegalArgumentException("is not primaryColorChannel1");
            }
            int besideIndex = 6 - (primaryColorChannel1.index + primaryColorChannel2.index);
            return Channel.getChannel(besideIndex);
        }

        /**
     * primaryColorChannel�H�~��PrimaryChannel
     * (ex:R�H�~��PrimaryChannel�N�OG/B; G�N�OR/B...)
     * @param primaryColorChannel Channel
     * @return Channel[]
     */
        public static Channel[] getBesidePrimaryChannel(Channel primaryColorChannel) {
            if (!primaryColorChannel.isPrimaryColorChannel()) {
                throw new IllegalArgumentException("is not primaryColorChannel1");
            }
            switch(primaryColorChannel) {
                case R:
                    return new Channel[] { G, B };
                case G:
                    return new Channel[] { R, B };
                case B:
                    return new Channel[] { R, G };
                default:
                    return null;
            }
        }

        Channel(int index, Color color, String fullname) {
            this.index = index;
            this.color = color;
            this.fullname = fullname;
        }

        public static final Channel RGBYMCChannel[] = new Channel[] { Channel.R, Channel.G, Channel.B, Channel.Y, Channel.M, Channel.C };

        public static final Channel RGBYMCWChannel[] = new Channel[] { Channel.R, Channel.G, Channel.B, Channel.Y, Channel.M, Channel.C, Channel.W };

        public static final Channel RGBChannel[] = new Channel[] { Channel.R, Channel.G, Channel.B };

        public static final Channel RGBWChannel[] = new Channel[] { Channel.R, Channel.G, Channel.B, Channel.W };

        public static final Channel WRGBChannel[] = new Channel[] { Channel.W, Channel.R, Channel.G, Channel.B };

        public static final Channel YMCChannel[] = new Channel[] { Channel.Y, Channel.M, Channel.C };

        public final int index;

        public final Color color;

        public final String fullname;

        /**
     * �H�}�C����RGB Channel�ɩұĥΪ�index
     * @return int
     */
        public final int getArrayIndex() {
            int result = index - 1;
            return result;
        }

        /**
     * �qarrayIndex���۹�����channel
     * @param arrayIndex int
     * @return Channel
     */
        public static final Channel getChannelByArrayIndex(int arrayIndex) {
            return getChannel(arrayIndex + 1);
        }

        public static final Channel getChannel(int chIndex) {
            switch(chIndex) {
                case 1:
                    return Channel.R;
                case 2:
                    return Channel.G;
                case 3:
                    return Channel.B;
                case 4:
                    return Channel.Y;
                case 5:
                    return Channel.M;
                case 6:
                    return Channel.C;
                case 7:
                    return Channel.W;
                default:
                    return null;
            }
        }
    }

    public static final class ColorSpace implements Serializable {

        public static enum GammaType {

            Simple, sRGB, LStar
        }

        public static final ColorSpace getInstance(java.awt.color.ColorSpace colorSpace) {
            if (colorSpace.isCS_sRGB()) {
                return ColorSpace.sRGB;
            } else {
                return ColorSpace.unknowRGB;
            }
        }

        public static final ColorSpace AdobeRGB = new ColorSpace(Illuminant.D65, 2.19921875, 0.64, 0.33, 0.21, 0.71, 0.15, 0.06);

        public static final ColorSpace sRGB = new ColorSpace(Illuminant.D65, GammaType.sRGB, 0.64, 0.33, 0.30, 0.60, 0.15, 0.06);

        public static final ColorSpace ProPhotoRGB = new ColorSpace(Illuminant.D50, 1.8, 0.7347, 0.2653, 0.1596, 0.8404, 0.0366, 0.0001);

        public static final ColorSpace unknowRGB = new ColorSpace(Illuminant.D65, 2.2, 0.64, 0.33, 0.21, 0.71, 0.15, 0.06);

        public static final ColorSpace WideGamutRGB = new ColorSpace(Illuminant.D50, 2.2, 0.7350, 0.2650, 0.1150, 0.8260, 0.1570, 0.0180);

        public static final ColorSpace BetaRGB = new ColorSpace(Illuminant.D50, 2.2, 0.6888, 0.3112, 0.1986, 0.7551, 0.1265, 0.0352);

        public static final ColorSpace BruceRGB = new ColorSpace(Illuminant.D65, 2.2, 0.6400, 0.3300, 0.2800, 0.6500, 0.1500, 0.0600);

        public static final ColorSpace ECIRGB = new ColorSpace(Illuminant.D50, 1.8, 0.6700, 0.3300, 0.2100, 0.7100, 0.1400, 0.0800);

        public static final ColorSpace AppleRGB = new ColorSpace(Illuminant.D65, 1.8, 0.6250, 0.3400, 0.2800, 0.5950, 0.1550, 0.0700);

        public static final ColorSpace BestRGB = new ColorSpace(Illuminant.D50, 2.2, 0.7347, 0.2653, 0.2150, 0.7750, 0.1300, 0.0350);

        public static final ColorSpace LStarRGB = new ColorSpace(Illuminant.D50, 1.8, 0.6700, 0.3300, 0.2100, 0.7100, 0.1400, 0.0800);

        public static final ColorSpace skyRGB = new ColorSpace(Illuminant.D50, 2.2, 0.7350, 0.2650, 0.1150, 0.8260, 0.1570, 0.0180);

        public static final ColorSpace NTSCRGB = new ColorSpace(Illuminant.C, 2.2, 0.6700, 0.3300, 0.2100, 0.7100, 0.1400, 0.0800);

        public static final ColorSpace EktaSpacePS5RGB = new ColorSpace(Illuminant.D50, 2.2, 0.6950, 0.3050, 0.2600, 0.7000, 0.1100, 0.0050);

        public static final ColorSpace sRGB_gamma22 = new ColorSpace(Illuminant.D65, 2.2, 0.64, 0.33, 0.30, 0.60, 0.15, 0.06);

        public static final ColorSpace CIERGB = new ColorSpace(Illuminant.E, 2.2, 0.7347, 0.2653, 0.2738, 0.7174, 0.1666, 0.0089);

        public static final ColorSpace sRGB_gamma18 = new ColorSpace(Illuminant.D65, 1.8, 0.64, 0.33, 0.30, 0.60, 0.15, 0.06);

        public static final ColorSpace SMPTE_C = new ColorSpace(Illuminant.D65, 2.2, 0.6300, 0.3400, 0.3100, 0.5950, 0.1550, 0.0700);

        public final Illuminant referenceWhite;

        public final double[][] toXYZMatrix;

        public final double[][] toRGBMatrix;

        public final double gamma;

        private final GammaType gammaType;

        private final CIEXYZ referenceWhiteXYZ;

        public CIEXYZ getReferenceWhiteXYZ() {
            return referenceWhiteXYZ;
        }

        public final double rx, ry, gx, gy, bx, by;

        private ColorSpace(Illuminant referenceWhite, double gamma, GammaType gammaType, double rx, double ry, double gx, double gy, double bx, double by) {
            this.rx = rx;
            this.ry = ry;
            this.gx = gx;
            this.gy = gy;
            this.bx = bx;
            this.by = by;
            this.referenceWhite = referenceWhite;
            this.gamma = gamma;
            this.gammaType = gammaType;
            referenceWhiteXYZ = referenceWhite.getNormalizeXYZ();
            this.toXYZMatrix = calculateRGBXYZMatrix(rx, ry, gx, gy, bx, by, referenceWhiteXYZ.getValues());
            this.toRGBMatrix = toXYZMatrix != null ? DoubleArray.inverse(toXYZMatrix) : null;
        }

        public ColorSpace(Illuminant referenceWhite, double gamma, double rx, double ry, double gx, double gy, double bx, double by) {
            this(referenceWhite, gamma, GammaType.Simple, rx, ry, gx, gy, bx, by);
        }

        public ColorSpace(Illuminant referenceWhite, GammaType gammaType, double rx, double ry, double gx, double gy, double bx, double by) {
            this(referenceWhite, 2.2, gammaType, rx, ry, gx, gy, bx, by);
        }
    }

    /**
   * �q�T��⪺��׮y��(x,y)�H�Υ��I��XYZ,�p��XRGB->XYZ���ഫ�x�}
   * @param r CIExyY
   * @param g CIExyY
   * @param b CIExyY
   * @param white CIEXYZ
   * @return double[][]
   */
    public static final double[][] calculateRGBXYZMatrix(CIExyY r, CIExyY g, CIExyY b, CIEXYZ white) {
        return calculateRGBXYZMatrix(r.x, r.y, g.x, g.y, b.x, b.y, white.getValues());
    }

    public static final double[][] calculateRGBXYZMatrix(double rx, double ry, double gx, double gy, double bx, double by, double[] whiteXYZValues) {
        double[] rXYZ = new double[] { rx / ry, 1, (1 - rx - ry) / ry };
        double[] gXYZ = new double[] { gx / gy, 1, (1 - gx - gy) / gy };
        double[] bXYZ = new double[] { bx / by, 1, (1 - bx - by) / by };
        double[][] inv = DoubleArray.inverse(new double[][] { rXYZ, gXYZ, bXYZ });
        double[] S = DoubleArray.times(whiteXYZValues, inv);
        rXYZ = DoubleArray.times(rXYZ, S[0]);
        gXYZ = DoubleArray.times(gXYZ, S[1]);
        bXYZ = DoubleArray.times(bXYZ, S[2]);
        double[][] m = new double[][] { rXYZ, gXYZ, bXYZ };
        return m;
    }

    /**
   * linearRGBValues���RGBValues
   * @param linearRGBValues double[]
   * @param colorSpace RGBColorSpace
   * @return double[]
   */
    public static final double[] linearToRGBValues(double[] linearRGBValues, RGBBase.ColorSpace colorSpace) {
        double[] rgbValues = linearRGBValues.clone();
        boolean negative = false;
        switch(colorSpace.gammaType) {
            case Simple:
                for (int x = 0; x < 3; x++) {
                    negative = rgbValues[x] < 0;
                    if (negative) {
                        rgbValues[x] = -rgbValues[x];
                    }
                    rgbValues[x] = Math.pow(rgbValues[x], 1. / colorSpace.gamma);
                    if (negative) {
                        rgbValues[x] = -rgbValues[x];
                    }
                }
                break;
            case LStar:
                double[] tmpValues = new double[3];
                double[] whiteValues = Illuminant.D50WhitePoint.getValues();
                for (int x = 0; x < 3; x++) {
                    negative = rgbValues[x] < 0;
                    if (negative) {
                        rgbValues[x] = -rgbValues[x];
                    }
                    tmpValues[1] = rgbValues[x];
                    rgbValues[x] = CIELab.fromXYZValues(tmpValues, whiteValues)[0] / 100.;
                    if (negative) {
                        rgbValues[x] = -rgbValues[x];
                    }
                }
                break;
            case sRGB:
                for (int x = 0; x < 3; x++) {
                    negative = rgbValues[x] < 0;
                    if (negative) {
                        rgbValues[x] = -rgbValues[x];
                    }
                    if (rgbValues[x] <= 0.0031308) {
                        rgbValues[x] *= 12.92;
                    } else {
                        rgbValues[x] = 1.055 * Math.pow(rgbValues[x], 1. / 2.4) - 0.055;
                    }
                    if (negative) {
                        rgbValues[x] = -rgbValues[x];
                    }
                }
                break;
            default:
                throw new IllegalStateException("unsupport RGBColorSpace.");
        }
        return rgbValues;
    }

    /**
   * RGBValues���linearRGBValues
   * @param RGBValues double[]
   * @param colorSpace RGBColorSpace
   * @return double[]
   */
    public static final double[] toLinearRGBValues(double[] RGBValues, RGBBase.ColorSpace colorSpace) {
        double[] linearRGBValues = RGBValues.clone();
        boolean negative = false;
        switch(colorSpace.gammaType) {
            case Simple:
                for (int x = 0; x < 3; x++) {
                    negative = linearRGBValues[x] < 0;
                    if (negative) {
                        linearRGBValues[x] = -linearRGBValues[x];
                    }
                    linearRGBValues[x] = Math.pow(linearRGBValues[x], colorSpace.gamma);
                    if (negative) {
                        linearRGBValues[x] = -linearRGBValues[x];
                    }
                }
                break;
            case LStar:
                double[] tmpValues = new double[3];
                for (int x = 0; x < 3; x++) {
                    negative = linearRGBValues[x] < 0;
                    if (negative) {
                        linearRGBValues[x] = -linearRGBValues[x];
                    }
                    tmpValues[0] = linearRGBValues[x] * 100.;
                    linearRGBValues[x] = CIELab.toXYZValues(tmpValues, Illuminant.D50WhitePoint.getValues())[1];
                    if (negative) {
                        linearRGBValues[x] = -linearRGBValues[x];
                    }
                }
                break;
            case sRGB:
                for (int x = 0; x < 3; x++) {
                    negative = linearRGBValues[x] < 0;
                    if (negative) {
                        linearRGBValues[x] = -linearRGBValues[x];
                    }
                    if (linearRGBValues[x] <= 0.04045) {
                        linearRGBValues[x] = linearRGBValues[x] / 12.92;
                    } else {
                        linearRGBValues[x] = Math.pow((linearRGBValues[x] + 0.055) / 1.055, 2.4);
                    }
                    if (negative) {
                        linearRGBValues[x] = -linearRGBValues[x];
                    }
                }
                break;
            default:
                throw new IllegalStateException("unsupport RGBColorSpace: " + colorSpace);
        }
        return linearRGBValues;
    }

    public static void main(String[] args) {
        Object[] objs = ColorSpace.skyRGB.getClass().getSigners();
        for (Object obj : objs) {
            System.out.println(obj);
        }
    }

    protected ColorSpace rgbColorSpace = ColorSpace.unknowRGB;

    protected Round round = Round.NotYet;

    protected MaxValue maxValue = MaxValue.DoubleUnlimited;

    public void setRGBColorSpace(ColorSpace rgbColorSpace) {
        this.rgbColorSpace = rgbColorSpace;
    }

    public ColorSpace getRGBColorSpace() {
        return rgbColorSpace;
    }

    /**
   *
   * @param type MaxValue
   * @param integerRoundDown boolean ��ƪ������O�_�ĵL���˥h
   */
    public void changeMaxValue(MaxValue type, boolean integerRoundDown) {
        if (this.maxValue == type || this.maxValue == MaxValue.DoubleUnlimited || type == MaxValue.DoubleUnlimited) {
            return;
        }
        double[] values = this.getValues();
        changeMaxValue(values, this.maxValue, type, integerRoundDown);
        this.maxValue = type;
        this.setValues(values);
        values = null;
        round = integerRoundDown ? Round.RoundDown : Round.RoundOff;
    }

    public void changeMaxValue(MaxValue type) {
        changeMaxValue(type, false);
    }

    public void quantization(RGB.MaxValue maxValue, RGBBase.Channel channel) {
        quantization(maxValue, channel, false);
    }

    /**
   *
   * @param maxValue MaxValue
   * @param channel Channel
   * @param integerRoundDown boolean
   */
    public void quantization(RGB.MaxValue maxValue, RGBBase.Channel channel, boolean integerRoundDown) {
        if (channel == RGBBase.Channel.W) {
            quantization(maxValue, integerRoundDown);
            return;
        }
        if (!channel.isPrimaryColorChannel()) {
            throw new IllegalArgumentException("!channel.isPrimaryColorChannel()");
        }
        double[] originalValues = this.getValues();
        MaxValue origin = this.maxValue;
        this.changeMaxValue(maxValue, integerRoundDown);
        this.changeMaxValue(origin, integerRoundDown);
        double[] quantizedValues = this.getValues();
        originalValues[channel.getArrayIndex()] = quantizedValues[channel.getArrayIndex()];
        this.setValues(originalValues);
    }

    public void quantization(RGB.MaxValue maxValue) {
        quantization(maxValue, false);
    }

    public void quantization(RGB.MaxValue maxValue, boolean integerRoundDown) {
        MaxValue origin = this.maxValue;
        this.changeMaxValue(maxValue, integerRoundDown);
        this.changeMaxValue(origin, integerRoundDown);
    }

    /**
   * ��rgb�i��X�z��(����0�PmaxValue����)
   * @param val double
   * @param maxValue MaxValue
   * @return double
   */
    public static final double rationalize(final double val, RGB.MaxValue maxValue) {
        double result = val;
        result = result < 0 ? 0 : result;
        result = result > maxValue.max ? maxValue.max : result;
        return result;
    }

    public static final class Delta {

        /**
     * �p��RG�Ŷ��U����t
     * @param rgb1 RGB
     * @param rgb2 RGB
     * @return double
     */
        public static double deltaRG(RGB rgb1, RGB rgb2) {
            double[] rg1 = new double[] { rgb1.getRGr(), rgb1.getRGg() };
            double[] rg2 = new double[] { rgb2.getRGr(), rgb2.getRGg() };
            return Maths.RMSD(rg1, rg2);
        }

        public static double deltaRGr(RGB rgb1, RGB rgb2) {
            return Math.abs(rgb1.getRGr() - rgb2.getRGr());
        }

        public static double deltaRGg(RGB rgb1, RGB rgb2) {
            return Math.abs(rgb1.getRGg() - rgb2.getRGg());
        }

        public static double[] deltaRGB(RGB rgb1, RGB rgb2) {
            double[] v1 = rgb1.getValues();
            double[] v2 = rgb2.getValues();
            double[] delta = DoubleArray.minus(v1, v2);
            for (int x = 0; x < 3; x++) {
                delta[x] = Math.abs(delta[x]);
            }
            return delta;
        }
    }

    public static enum Round {

        NotYet, RoundOff, RoundDown
    }

    public RGBBase(RGB.ColorSpace colorSpace, MaxValue maxValue) {
        this.rgbColorSpace = colorSpace;
        if (this.rgbColorSpace == null) {
            this.rgbColorSpace = RGB.ColorSpace.unknowRGB;
        }
        this.maxValue = maxValue;
    }
}
