package shu.cms.devicemodel.lcd.util;

import flanagan.math.*;
import shu.cms.*;
import shu.cms.colorspace.depend.*;
import shu.cms.colorspace.independ.*;
import shu.cms.devicemodel.lcd.*;

/**
 * <p>Title: Colour Management System</p>
 *
 * <p>Description: a Colour Management System by Java</p>
 *
 * <p>Copyright: Copyright (c) 2008</p>
 *
 * <p>Company: skygroup</p>
 *
 * @author skyforce
 * @version 1.0
 */
public class RBCalculator {

    protected LCDModel lcdModel;

    protected CIEXYZ whiteXYZ = null;

    private LCDModelExposure adapter;

    /**
   * �]�w�p���t�ҨϥΪ����I
   * @param whiteXYZ CIEXYZ
   */
    public void setWhitePoint(CIEXYZ whiteXYZ) {
        this.whiteXYZ = whiteXYZ;
    }

    public RBCalculator(LCDModel lcdModel) {
        this.lcdModel = lcdModel;
        this.minLuminanceConstraint = lcdModel.flare.getFlare().Y;
        this.adapter = new LCDModelExposure(lcdModel);
    }

    /**
   *
   * @param xyY CIExyY �ؼЪ�xyY��
   * @param g double �ؼЪ�G��
   * @param GTolerance double G�i�e�\�����t��
   * @param relativeXYZ boolean XYZ�ȬO�_���۹��
   * @return RGB
   */
    public RGB getRB(CIExyY xyY, double g, double GTolerance, boolean relativeXYZ) {
        return getRGBByLumi(xyY, g, GTolerance, RGBBase.Channel.G, relativeXYZ);
    }

    public RGB getWhiteRGB(CIExyY xyY, double targetValue, double tolerance, boolean relativeXYZ) {
        RGB rb = getRBByLumi(xyY, relativeXYZ, tolerance, targetValue, null, true);
        calculateRBDeltaE(rb, xyY, relativeXYZ);
        return rb;
    }

    /**
   * �w��GB(or RB), ��̱���xyY��RGB�զX
   * @param xyY CIExyY �ؼ�xyY
   * @param rgb RGB �w����GB(or RB)
   * @param tolerance double R(or B)���e�e��
   * @param relativeXYZ boolean XYZ(xyY)�O�_���۹��
   * @param getB boolean
   * @return RGB
   * @deprecated
   */
    public RGB getRorB(CIExyY xyY, final RGB rgb, double tolerance, boolean relativeXYZ, boolean getB) {
        double targetG = rgb.getValue(RGBBase.Channel.G, RGB.MaxValue.Double255);
        RGB initRGB = this.getRB(xyY, targetG, tolerance, relativeXYZ);
        initRGB.G = rgb.G;
        MinimisationFunction func = null;
        if (getB) {
            initRGB.R = rgb.R;
            func = new FunctionRorB(initRGB, xyY.toXYZ(), relativeXYZ, RGBBase.Channel.B);
        } else {
            initRGB.B = rgb.B;
            func = new FunctionRorB(initRGB, xyY.toXYZ(), relativeXYZ, RGBBase.Channel.R);
        }
        Minimisation min = new Minimisation();
        double[] start = new double[] { initRGB.getValue(RGBBase.Channel.R, RGB.MaxValue.Double255) };
        double[] step = new double[] { tolerance };
        double ftol = tolerance;
        min.addConstraint(0, -1, 0);
        min.addConstraint(0, 1, 255);
        min.nelderMead(func, start, step, ftol);
        double[] param = min.getParamValues();
        initRGB.setValue(RGBBase.Channel.R, param[0], RGB.MaxValue.Double255);
        calculateRBDeltaE(initRGB, xyY, relativeXYZ);
        return initRGB;
    }

    protected class FunctionRorB implements MinimisationFunction {

        private RGB rgb;

        private boolean targetRelativeXYZ;

        private CIEXYZ targetXYZ;

        private RGBBase.Channel ch;

        protected FunctionRorB(RGB initGB, CIEXYZ targetXYZ, boolean targetRelativeXYZ, RGBBase.Channel ch) {
            this.rgb = (RGB) initGB.clone();
            this.targetXYZ = targetXYZ;
            this.targetRelativeXYZ = targetRelativeXYZ;
            this.ch = ch;
        }

        public double function(double[] code) {
            rgb.setValue(ch, code[0], RGB.MaxValue.Double255);
            DeltaE dE = lcdModel.calculateGetRGBDeltaE(rgb, targetXYZ, targetRelativeXYZ);
            double deltaE = dE.getCIE2000DeltaE();
            return deltaE;
        }
    }

    public RGB getRGB(CIEXYZ XYZ, final RGB initRGB, double tolerance, RGBBase.Channel channel, boolean relativeXYZ) {
        RGB rgb = (RGB) initRGB.clone();
        MinimisationFunction func = new FunctionRorB(initRGB, XYZ, relativeXYZ, channel);
        Minimisation min = new Minimisation();
        double[] start = new double[] { rgb.getValue(channel, RGB.MaxValue.Double255) };
        double[] step = new double[] { RGB.MaxValue.Int10Bit.getStepIn255() };
        double ftol = tolerance;
        min.addConstraint(0, -1, 0);
        min.addConstraint(0, 1, 255);
        min.nelderMead(func, start, step, ftol);
        double[] param = min.getParamValues();
        rgb.setValue(channel, param[0]);
        calculateRBDeltaE(rgb, new CIExyY(XYZ), relativeXYZ);
        return rgb;
    }

    /**
   * �w���ؼ�channel ch, �B�w���ؼ�channel��������targetValue, �n�D�۹�����xyY�U��RGB
   * @param xyY CIExyY �w��xyY
   * @param targetValue double �w���ƭ�
   * @param tolerance double
   * @param ch Channel �w���ƭ�(targetValue)��channel
   * @param relativeXYZ boolean XYZ(xyY)�O�_���۹��
   * @return RGB
   */
    public RGB getRGBByLumi(CIExyY xyY, double targetValue, double tolerance, RGBBase.Channel ch, boolean relativeXYZ) {
        RGB rb = getRBByLumi(xyY, relativeXYZ, tolerance, targetValue, ch, false);
        calculateRBDeltaE(rb, xyY, relativeXYZ);
        return rb;
    }

    private void calculateRBDeltaE(RGB rb, CIExyY xyY, boolean relativeXYZ) {
        _getRBDeltaE = getDeltaE(rb, xyY, relativeXYZ);
    }

    private DeltaE getDeltaE(RGB rb, CIExyY xyY, boolean relativeXYZ) {
        CIEXYZ XYZ = xyY.toXYZ();
        CIEXYZ rbXYZ = lcdModel.getXYZ(rb, relativeXYZ);
        DeltaE dE = lcdModel.getDeltaE(XYZ, rbXYZ);
        return dE;
    }

    public DeltaE getRBDeltaE() {
        return _getRBDeltaE;
    }

    /**
   * �Q���u�ƪ��覡�վ�G��, ���̱��񪺸�
   * @param targetxyY CIExyY �ؼЪ�xyY
   * @param relativeXYZ boolean �ؼ�XYZ(xyY)�O�_�O�۹��
   * @param tolerance double �e�e��
   * @param targetValue double �ؼмƭ�
   * @param ch Channel �ؼмƭȪ�channel
   * @param whiteRGB boolean �O�_�OwhiteRGB;
   * �p�G�OwhiteRGB, �h����ch���]�w, �ؼмƭȷ|�H�̤j�ƭȪ�channel���D,
   *  �B�C��maxLuminanceConstraint�|����, ���T�O�@�w�n��쵲�G.
   * @return RGB
   */
    private RGB getRBByLumi(final CIExyY targetxyY, boolean relativeXYZ, double tolerance, double targetValue, RGBBase.Channel ch, boolean whiteRGB) {
        double initStep = INIT_STEP;
        if (maxLuminanceConstraint == -1) {
            this.luminanceConstraint = this.lcdModel.getLuminance().Y;
        } else {
            this.luminanceConstraint = maxLuminanceConstraint;
            initStep = 1;
        }
        touchMaxIterativeTime = false;
        RGB minDeltaGRGB = null;
        double absDelta = Double.MAX_VALUE;
        double minDelta = Double.MAX_VALUE;
        double minDeltaE = Double.MAX_VALUE;
        CIExyY clone = (CIExyY) targetxyY.clone();
        this.targetChannel = ch;
        double dEIndex = Double.MAX_VALUE;
        for (int x = 0; x < MAX_ITERATIVE_TIME && (absDelta > tolerance || dEIndex > .25); x++, initStep *= 2) {
            RGB rgb = getRB0(clone, relativeXYZ, initStep, targetValue, tolerance, whiteRGB, ch);
            double estimate = 0;
            if (whiteRGB) {
                estimate = rgb.getValue(rgb.getMaxChannel(), RGB.MaxValue.Double255);
            } else {
                estimate = rgb.getValue(ch, RGB.MaxValue.Double255);
            }
            absDelta = Math.abs(estimate - targetValue);
            DeltaE ciede = getDeltaE(rgb, targetxyY, relativeXYZ);
            dEIndex = getDeltaIndex(ciede);
            if (absDelta < minDelta && dEIndex < minDeltaE) {
                minDelta = absDelta;
                minDeltaE = dEIndex;
                minDeltaGRGB = rgb;
            }
            clone.Y *= 0.9;
            if (whiteRGB && luminanceConstraint != -1) {
                luminanceConstraint *= 0.99;
            }
            if (x == (MAX_ITERATIVE_TIME - 1)) {
                touchMaxIterativeTime = true;
            }
        }
        return minDeltaGRGB;
    }

    protected boolean touchMaxIterativeTime = false;

    public boolean touchMaxIterativeTime() {
        return touchMaxIterativeTime;
    }

    protected static final double INIT_STEP = 1;

    protected static final int MAX_ITERATIVE_TIME = 180;

    protected double maxLuminanceConstraint = -1;

    protected double minLuminanceConstraint = 0;

    /**
   * �ѹB��Ϊ��G�׭���
   */
    private double luminanceConstraint = -1;

    private RGB getRB0(CIExyY targetxyY, boolean relativeXYZ, double initStep, double targetValue, double tolerance, boolean whiteRGB, RGBBase.Channel ch) {
        Minimisation min = new Minimisation();
        double[] start = new double[] { whiteRGB ? luminanceConstraint * .95 : targetxyY.Y };
        double[] step = new double[] { initStep };
        min.addConstraint(0, -1, minLuminanceConstraint);
        if (luminanceConstraint != -1) {
            min.addConstraint(0, 1, luminanceConstraint);
        }
        FunctionRBLumi func = new FunctionRBLumi((CIExyY) targetxyY.clone(), ch, targetValue, relativeXYZ, whiteRGB);
        min.nelderMead(func, start, step, tolerance);
        double[] param = min.getParamValues();
        targetxyY.Y = param[0];
        CIEXYZ getRBXYZ = CIExyY.toXYZ(targetxyY);
        RGB rgb = adapter.getRGB(getRBXYZ, relativeXYZ, true);
        return rgb;
    }

    protected RGBBase.Channel targetChannel = RGBBase.Channel.G;

    protected DeltaE _getRBDeltaE;

    protected class FunctionRBLumi implements MinimisationFunction {

        private CIExyY iterativexyY;

        private RGBBase.Channel targetChannel = RGBBase.Channel.G;

        private double targetValue;

        private boolean targetRelativeXYZ;

        private boolean whiteRGBMode = false;

        protected FunctionRBLumi(CIExyY iterativexyY, RGBBase.Channel targetChannel, double targetValue, boolean targetRelativeXYZ, boolean whiteRGBMode) {
            this.iterativexyY = iterativexyY;
            this.targetChannel = targetChannel;
            this.targetValue = targetValue;
            this.targetRelativeXYZ = targetRelativeXYZ;
            this.whiteRGBMode = whiteRGBMode;
        }

        /**
     * �̨ΤƥؼШ��.
     * �γ̨Τƪ��覡,�bxyY domain�W�վ�G��,�Ϩ�G�׹�����G�ȲŦX�һ�.
     * delta���ؼШ��,���_�վ�Y�o��̤p��delta,���̨Τƪ��L�{.
     * @param Y double[]
     * @return double
     */
        public double function(double[] Y) {
            iterativexyY.Y = Y[0];
            CIEXYZ resultXYZ = CIExyY.toXYZ(iterativexyY);
            RGB rgb = adapter.getRGB(resultXYZ, targetRelativeXYZ, true);
            double estimate = 0;
            if (whiteRGBMode) {
                estimate = rgb.getValue(rgb.getMaxChannel(), RGB.MaxValue.Double255);
            } else {
                estimate = rgb.getValue(targetChannel, RGB.MaxValue.Double255);
            }
            double delta = Math.abs(targetValue - estimate);
            DeltaE dE = getDeltaE(rgb, iterativexyY, targetRelativeXYZ);
            double result = getDeltaIndex(dE) + delta;
            return result;
        }
    }

    private boolean concernChromaticityOnly = true;

    private double getDeltaIndex(DeltaE dE) {
        if (concernChromaticityOnly) {
            return dE.getCIE2000Deltaab();
        } else {
            return dE.getCIE2000DeltaE();
        }
    }

    public static void main(String[] args) {
        ProfileColorSpaceModel model = new ProfileColorSpaceModel(RGB.ColorSpace.sRGB);
        model.produceFactor();
        model.setAutoRGBChangeMaxValue(true);
        RBCalculator calc = new RBCalculator(model);
        CIEXYZ XYZ = model.getXYZ(new RGB(30, 40, 50), false);
        RGB rgb = model.getRGB(XYZ, false);
        RGB rb = calc.getRB(new CIExyY(XYZ), rgb.G + 20, 0.25, false);
        DeltaE de = calc.getRBDeltaE();
        System.out.println(de.getCIE2000DeltaE());
        System.out.println(de.getCIE2000Deltaab());
        System.out.println(de.getCIE2000DeltaL());
        CIEXYZ XYZ1 = model.getXYZ(rgb, false);
        CIEXYZ XYZ2 = model.getXYZ(rb, false);
        System.out.println(new CIExyY(XYZ1));
        System.out.println(new CIExyY(XYZ2));
    }

    /**
   * ���N�ɪ��̤j�G�׭���.
   * ���gamma���઺���O�ܦ��γB.
   * @param maxLuminanceConstraint double
   */
    public void setMaxLuminanceConstraint(double maxLuminanceConstraint) {
        this.maxLuminanceConstraint = maxLuminanceConstraint;
    }
}
