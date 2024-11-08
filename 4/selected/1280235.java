package shu.cms.devicemodel.lcd.xtalk;

import flanagan.math.*;
import shu.cms.*;
import shu.cms.colorspace.depend.*;
import shu.cms.colorspace.independ.*;
import shu.cms.devicemodel.lcd.*;
import shu.cms.lcd.*;
import shu.math.array.*;

/**
 * <p>Title: Colour Management System</p>
 *
 * <p>Description: a Colour Management System by Java</p>
 * ����Xtalk������
 *
 * <p>Copyright: Copyright (c) 2008</p>
 *
 * <p>Company: skygroup</p>
 *
 * @author skyforce
 * @version 1.0
 */
public class XTalkReconstructor extends AbstractXTalkReconstructor {

    protected static enum Method {

        ByMatrix, ByMinimisation
    }

    XTalkReconstructor(MultiMatrixModel mmModel, XTalkProperty xtalkProperty) {
        super(mmModel, xtalkProperty);
    }

    /**
   * �qxtalk�᪺XYZ�H�έ�l��originalRGB(�]�N�O����xtalk�v�T��RGB), ����Xxtalk�ҳy����RGB����
   * @param XYZ CIEXYZ
   * @param originalRGB RGB
   * @param relativeXYZ boolean
   * @return RGB
   */
    public RGB getXTalkRGB(CIEXYZ XYZ, final RGB originalRGB, boolean relativeXYZ) {
        RGB byMatrixRGB = getXTalkRGB(XYZ, originalRGB, relativeXYZ, Method.ByMatrix);
        DeltaE byMatrixRGBDeltaE = getXTalkRGBDeltaE();
        double byMatrixRGBdE = byMatrixRGBDeltaE.getCIE2000DeltaE();
        RGB byMinimisationRGB = getXTalkRGB(XYZ, originalRGB, relativeXYZ, Method.ByMinimisation);
        DeltaE byMinimisationDeltaE = getXTalkRGBDeltaE();
        double byMinimisationdE = byMinimisationDeltaE.getCIE2000DeltaE();
        if (byMatrixRGBdE < byMinimisationdE) {
            this._getXTalkRGBDeltaE = byMatrixRGBDeltaE;
            byMatrixCount++;
            return byMatrixRGB;
        } else {
            this._getXTalkRGBDeltaE = byMinimisationDeltaE;
            byMinimisationxCount++;
            return byMinimisationRGB;
        }
    }

    private int byMatrixCount;

    private int byMinimisationxCount;

    protected RGB getXTalkRGB(CIEXYZ XYZ, final RGB originalRGB, boolean relativeXYZ, Method method) {
        if (!originalRGB.isSecondaryChannel()) {
            return null;
        }
        CIEXYZ fromXYZ = adapter.fromXYZ(XYZ, relativeXYZ);
        fromXYZ.rationalize();
        switch(method) {
            case ByMatrix:
                return getXTalkRGBByMatrix(fromXYZ, originalRGB);
            case ByMinimisation:
                return getXTalkRGBByMinimisation(fromXYZ, originalRGB);
            default:
                return null;
        }
    }

    /**
   * �w��XYZ,�٭�RGB,�B�T�w�FXTalk���W�D,�Q���u�ƪ��覡�D�o�̨θ�
   *
   * �t��k����:
   * (1)�p��Xtalk channel
   * (2)�Q���u�ƪ��覡�D�o�̨θ�
   * (3)�p���Ӻt��k�~�t�ҳy������t
   * @param XYZ CIEXYZ
   * @param originalRGB RGB
   * @return RGB
   */
    protected RGB getXTalkRGBByMinimisation(CIEXYZ XYZ, final RGB originalRGB) {
        RGBBase.Channel selfChannel = xtalkProperty.getSelfChannel(originalRGB.getSecondaryChannel());
        RGB rgb = recover.getXTalkRGB(XYZ, originalRGB, selfChannel);
        _getXTalkRGBDeltaE = mmModel.calculateGetRGBDeltaE(rgb, XYZ, true);
        return rgb;
    }

    protected XTalkRGBRecover recover = new XTalkRGBRecover();

    protected class XTalkRGBRecover implements MinimisationFunction {

        /**
     *
     * @param XYZ CIEXYZ xtalk�᪺XYZ
     * @param originalRGB RGB ��l��RGB
     * @param xtalkChannel Channel �Qxtalk�v�T��channel
     * @param signConstraint boolean �O�_�n����t��
     * @param negativeXTalk boolean ���t���������V
     * @return RGB xtalk�w��RGB
     */
        private RGB getXTalkRGB(CIEXYZ XYZ, final RGB originalRGB, RGBBase.Channel xtalkChannel, boolean signConstraint, boolean negativeXTalk) {
            this.measureXYZ = XYZ;
            this.recoverRGB = (RGB) originalRGB.clone();
            this.xtalkChannel = xtalkChannel;
            Minimisation min = new Minimisation();
            double[] start = new double[] { recoverRGB.getValue(xtalkChannel) };
            double[] step = new double[] { mmModel.getLCDTarget().getStep() };
            if (signConstraint) {
                if (negativeXTalk) {
                    min.addConstraint(0, 1, start[0]);
                    min.addConstraint(0, -1, 0);
                } else {
                    min.addConstraint(0, 1, originalRGB.getMaxValue().max);
                    min.addConstraint(0, -1, start[0]);
                }
            } else {
                min.addConstraint(0, 1, originalRGB.getMaxValue().max);
                min.addConstraint(0, -1, 0);
            }
            min.nelderMead(this, start, step);
            double[] param = min.getParamValues();
            recoverRGB.setValue(xtalkChannel, param[0]);
            return recoverRGB;
        }

        /**
     *
     * @param XYZ CIEXYZ Crosstalk�v�T�᪺XYZ
     * @param originalRGB RGB ��l��J��RGB�T��
     * @param xtalkChannel Channel Crosstalk�v�T���W�D
     * @param negativeXTalk boolean Crosstalk�O�_���t���v�T
     * @return RGB
     */
        public RGB getXTalkRGB(CIEXYZ XYZ, final RGB originalRGB, RGBBase.Channel xtalkChannel, boolean negativeXTalk) {
            return getXTalkRGB(XYZ, originalRGB, xtalkChannel, true, negativeXTalk);
        }

        public RGB getXTalkRGB(CIEXYZ XYZ, final RGB originalRGB, RGBBase.Channel xtalkChannel) {
            return getXTalkRGB(XYZ, originalRGB, xtalkChannel, false, false);
        }

        protected RGB recoverRGB;

        protected RGBBase.Channel xtalkChannel;

        protected CIEXYZ measureXYZ;

        /**
     * function
     *
     * @param doubleArray double[]
     * @return double
     */
        public double function(double[] doubleArray) {
            double val = doubleArray[0];
            recoverRGB.setValue(xtalkChannel, val);
            DeltaE de = mmModel.calculateGetRGBDeltaE(recoverRGB, measureXYZ, true);
            return de.getCIE2000DeltaE();
        }
    }

    /**
   * �w��XYZ,�٭�RGB,�B�T�w�FXTalk���W�D
   *
   * �t��k����:
   * (1)�p��Xtalk channel
   * (2)�ǥѤw����originalRGB, �ư���XTalk��Channel�H�~��XYZ, �o��besideXYZ
   * (3)�Nbeside XYZ�a�Jmax�x�}�i��B��, �o��ʦ�Luminance RGB
   * (4)���ܯx�}��XYZ, ���_���N�i��B��, �o��̨θ�.
   * (5)�N�̨θ�Luminace RGB�HungammaCorrect�oDAC RGB
   * (6)�p���Ӻt��k�~�t�ҳy������t
   * @param XYZ CIEXYZ
   * @param originalRGB RGB
   * @return RGB
   */
    protected RGB getXTalkRGBByMatrix(CIEXYZ XYZ, final RGB originalRGB) {
        RGBBase.Channel selfChannel = xtalkProperty.getSelfChannel(originalRGB.getSecondaryChannel());
        if (XYZ.X < 0 || XYZ.Y < 0 || XYZ.Z < 0) {
            _getXTalkRGBDeltaE = null;
            return originalRGB;
        }
        CIEXYZ besideXYZ = getBesideXYZ(XYZ, originalRGB, selfChannel);
        RGB rgb = XYZToChannelByMax(besideXYZ, selfChannel);
        adapter.resetTouchMaxIterativeTimes();
        for (int x = 0; x < mmModel.MAX_ITERATIVE_TIMES; x++) {
            RGB newRGB = XYZToChannelByMultiMatrix(rgb, besideXYZ, selfChannel);
            if (newRGB == null || newRGB.equals(rgb)) {
                break;
            }
            rgb = newRGB;
            adapter.setTouchMaxIterativeTimes(x);
        }
        mmModel.correct.gammaUncorrect(rgb);
        rgb.changeMaxValue(mmModel.getLCDTarget().getMaxValue());
        rgb = mmModel.rational.RGBRationalize(rgb);
        RGBBase.Channel[] constChannel = RGBBase.Channel.getBesidePrimaryChannel(selfChannel);
        rgb.setValue(constChannel[0], originalRGB.getValue(constChannel[0]));
        rgb.setValue(constChannel[1], originalRGB.getValue(constChannel[1]));
        _getXTalkRGBDeltaE = mmModel.calculateGetRGBDeltaE(rgb, XYZ, true);
        return rgb;
    }

    /**
   * �p��xtalkChannel��XYZ��
   * @param relativeXYZ CIEXYZ
   * @param originalRGB RGB
   * @param xtalkChannel Channel
   * @return CIEXYZ
   */
    protected CIEXYZ getBesideXYZ(CIEXYZ relativeXYZ, final RGB originalRGB, RGBBase.Channel xtalkChannel) {
        RGBBase.Channel[] constChannel = RGBBase.Channel.getBesidePrimaryChannel(xtalkChannel);
        RGB rgb = (RGB) originalRGB.clone();
        rgb.setColorBlack();
        rgb.setValue(constChannel[0], originalRGB.getValue(constChannel[0]));
        CIEXYZ ch0XYZ = mmModel.getXYZ(rgb, true);
        rgb.setColorBlack();
        rgb.setValue(constChannel[1], originalRGB.getValue(constChannel[1]));
        CIEXYZ ch1XYZ = mmModel.getXYZ(rgb, true);
        CIEXYZ besideXYZ = CIEXYZ.minus(CIEXYZ.minus(relativeXYZ, ch0XYZ), ch1XYZ);
        return besideXYZ;
    }

    private double[] rMaxInverse = null;

    private double[] gMaxInverse = null;

    private double[] bMaxInverse = null;

    /**
   * �p��channel�U��max XYZ�ҧΦ����ϯx�}
   * @param channel Channel
   * @return double[]
   */
    protected double[] getMaxInverse(RGBBase.Channel channel) {
        switch(channel) {
            case R:
                if (rMaxInverse == null) {
                    CIEXYZ maxXYZ = mmModel.getLCDTarget().getSaturatedChannelPatch(channel).getXYZ();
                    rMaxInverse = DoubleArray.transpose(DoubleArray.pseudoInverse(new double[][] { maxXYZ.getValues() }))[0];
                }
                return rMaxInverse;
            case G:
                if (gMaxInverse == null) {
                    CIEXYZ maxXYZ = mmModel.getLCDTarget().getSaturatedChannelPatch(channel).getXYZ();
                    gMaxInverse = DoubleArray.transpose(DoubleArray.pseudoInverse(new double[][] { maxXYZ.getValues() }))[0];
                }
                return gMaxInverse;
            case B:
                if (bMaxInverse == null) {
                    CIEXYZ maxXYZ = mmModel.getLCDTarget().getSaturatedChannelPatch(channel).getXYZ();
                    bMaxInverse = DoubleArray.transpose(DoubleArray.pseudoInverse(new double[][] { maxXYZ.getValues() }))[0];
                }
                return bMaxInverse;
            default:
                return null;
        }
    }

    /**
   * �qchannel��max XYZ,�h�Ϻ�XRGB��
   * @param XYZ CIEXYZ
   * @param channel Channel
   * @return RGB
   */
    protected final RGB XYZToChannelByMax(final CIEXYZ XYZ, RGBBase.Channel channel) {
        double[] maxInverse = getMaxInverse(channel);
        double relative = DoubleArray.times(XYZ.getValues(), maxInverse);
        RGB rgb = new RGB(RGB.ColorSpace.unknowRGB);
        rgb.setValue(channel, relative, RGB.MaxValue.Double1);
        rgb.changeMaxValue(mmModel.getLCDTarget().getMaxValue());
        rgb = mmModel.rational.RGBRationalize(rgb);
        return rgb;
    }

    /**
   * ��ochannel��code��channelValue�U��XYZ�Ȥϯx�}
   * @param channel Channel
   * @param channelValue double
   * @return double[]
   */
    protected final double[] getChannelInverse(RGBBase.Channel channel, double channelValue) {
        double r = channel == RGBBase.Channel.R ? channelValue : 0;
        double g = channel == RGBBase.Channel.G ? channelValue : 0;
        double b = channel == RGBBase.Channel.B ? channelValue : 0;
        double[] XYZValues = adapter.getXYZ(r, g, b).getValues();
        if (XYZValues[0] == 0 && XYZValues[1] == 0 && XYZValues[2] == 0) {
            return null;
        } else {
            return DoubleArray.transpose(DoubleArray.pseudoInverse(new double[][] { XYZValues }))[0];
        }
    }

    /**
   * �H�h��Matrix���覡,�o��̹G��channelXYZ��RGB code
   * @param luminanceRoughRGB RGB
   * @param channelXYZ CIEXYZ
   * @param channel Channel
   * @return RGB
   */
    protected RGB XYZToChannelByMultiMatrix(final RGB luminanceRoughRGB, final CIEXYZ channelXYZ, final RGBBase.Channel channel) {
        RGB rgb = (RGB) luminanceRoughRGB.clone();
        rgb.getValues(luminanceRGBValues);
        mmModel.correct.gammaUncorrect(rgb);
        double[] inverseMatrix = getChannelInverse(channel, rgb.getValue(channel));
        if (inverseMatrix != null) {
            rgb = XYZToChannelByMatrix(channelXYZ, inverseMatrix, channel);
            rgb.setValue(channel, rgb.getValue(channel, RGB.MaxValue.Double1) * luminanceRGBValues[channel.getArrayIndex()]);
            return rgb;
        } else {
            return null;
        }
    }

    private double[] luminanceRGBValues = new double[3];

    /**
   * �NXYZValues���WinverseMatrix,�o���Channel luminance RGB
   * @param XYZValues double[]
   * @param inverseMatrix double[]
   * @return double
   */
    protected final double XYZToChannel(double[] XYZValues, double[] inverseMatrix) {
        return DoubleArray.times(XYZValues, inverseMatrix);
    }

    /**
   * �NXYZ�PinveseMatrix�p��o��luminance RGB
   * @param XYZ CIEXYZ
   * @param inverseMatrix double[]
   * @param channel Channel
   * @return RGB
   */
    protected final RGB XYZToChannelByMatrix(CIEXYZ XYZ, double[] inverseMatrix, final RGBBase.Channel channel) {
        double channelValues = XYZToChannel(XYZ.getValues(), inverseMatrix);
        RGB rgb = new RGB(RGB.ColorSpace.unknowRGB);
        rgb.setValue(channel, channelValues, RGB.MaxValue.Double1);
        rgb.changeMaxValue(mmModel.getLCDTarget().getMaxValue());
        return rgb;
    }

    public static void main(String[] args) {
        LCDTarget.setRGBNormalize(false);
        LCDTarget lcdTarget = LCDTarget.Instance.get("cpt_17inch No.3", LCDTarget.Source.CA210, LCDTarget.Room.Dark, LCDTarget.TargetIlluminant.Native, LCDTargetBase.Number.Ramp1792, LCDTarget.FileType.VastView, null, null);
        MultiMatrixModel mmModel = new MultiMatrixModel(lcdTarget);
        mmModel.produceFactor();
        RGB keyRGB = mmModel.getLCDTarget().getKeyRGB();
        keyRGB.setValues(new double[] { 130, 130, 0 }, RGB.MaxValue.Double255);
        CIEXYZ XYZ1 = mmModel.getLCDTarget().getPatch(keyRGB).getXYZ();
        RGB rgb1 = mmModel.getRGB(XYZ1, false);
        System.out.println("org:" + keyRGB);
        System.out.println("mm:" + rgb1);
        System.out.println(mmModel.getRGBDeltaE().getCIE2000DeltaE());
        XTalkReconstructor reconstruct = new XTalkReconstructor(mmModel, XTalkProperty.getLeftXTalkProperty());
        RGB rgb2 = reconstruct.getXTalkRGB(XYZ1, keyRGB, false, XTalkReconstructor.Method.ByMatrix);
        System.out.println(reconstruct.getXTalkRGBDeltaE().getCIE2000DeltaE());
        RGB rgb3 = reconstruct.getXTalkRGB(XYZ1, keyRGB, false, XTalkReconstructor.Method.ByMinimisation);
        System.out.println(reconstruct.getXTalkRGBDeltaE().getCIE2000DeltaE());
        System.out.println("by mm:" + rgb2);
        System.out.println("by min:" + rgb3);
    }

    public int getByMatrixCount() {
        return byMatrixCount;
    }

    public int getByMinimisationxCount() {
        return byMinimisationxCount;
    }
}
