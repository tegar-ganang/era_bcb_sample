package shu.cms.hvs.gradient;

import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.image.*;
import org.apache.commons.collections.primitives.*;
import shu.cms.colorspace.depend.*;
import shu.cms.colorspace.independ.*;
import shu.cms.devicemodel.lcd.*;
import shu.cms.hvs.*;
import shu.cms.hvs.cam.*;
import shu.cms.hvs.cam.ciecam02.*;
import shu.cms.hvs.cam.ciecam02.CIECAM02;
import shu.cms.hvs.cam.ciecam02.ViewingConditions;
import shu.cms.hvs.hk.*;
import shu.cms.image.*;
import shu.cms.lcd.*;
import shu.cms.profile.*;
import shu.math.*;
import shu.math.array.*;
import shu.util.*;
import shu.util.log.*;

/**
 * <p>Title: Colour Management System</p>
 *
 * <p>Description: a Colour Management System by Java</p>
 * �H�����Gradient��model
 *
 * <p>Copyright: Copyright (c) 2008</p>
 *
 * <p>Company: skygroup</p>
 *
 * @author skyforce
 * @version 1.0
 */
public abstract class GradientModel {

    protected ProfileColorSpace pcs;

    CIEXYZ white = null;

    private BufferedImage image;

    protected int imageStart, imageEnd;

    private RGBBase.Channel imageChannel;

    /**
     * �qRGBW�|�Ӻ��h�ӵ��_�O�_����
     * @return boolean
     */
    public final boolean isSmooth() {
        boolean smooth = true;
        RGBBase.Channel[] channels = (mode == Mode.LuminanceOnly || mode == Mode.JNDIOnly) ? new RGBBase.Channel[] { RGBBase.Channel.W } : RGBBase.Channel.RGBWChannel;
        for (RGBBase.Channel ch : channels) {
            Logger.log.info(ch);
            smooth = isSmooth(ch) && smooth;
        }
        return smooth;
    }

    /**
     * ���_ch�����h�O�_����
     * @param ch Channel
     * @return boolean
     */
    public final boolean isSmooth(RGBBase.Channel ch) {
        setupImage(0, 255, ch, 256, 256);
        PatternAndScore ps = getPatternAndScore();
        List<Pattern> patternList = ps.patternList;
        for (Pattern p : patternList) {
            Logger.log.info(p);
        }
        return ps.isSmooth();
    }

    public GradientModel(LCDTarget target, CIEXYZ white) {
        this(ProfileColorSpace.Instance.get(target, target.getDescription()), target, white);
    }

    public GradientModel(LCDTarget target) {
        this(target, null);
    }

    public GradientModel(LCDModel lcdModel, CIEXYZ white) {
        this(ProfileColorSpace.Instance.get(lcdModel, lcdModel.getDescription()), null, white);
    }

    public GradientModel(LCDModel lcdModel) {
        this(lcdModel, null);
    }

    public GradientModel(double[] YArray) {
        this(YArray, true);
    }

    protected GradientModel() {
    }

    public GradientModel(double[] dataArray, boolean isLuminance) {
        if (isLuminance) {
            this.YArray = dataArray;
            this.hkStrategy = HKStrategy.None;
            mode = Mode.LuminanceOnly;
        } else {
            this.jndiCurve = dataArray;
            this.hkStrategy = HKStrategy.None;
            mode = Mode.JNDIOnly;
        }
    }

    protected static enum Mode {

        LuminanceOnly, JNDIOnly, Normal
    }

    private Mode mode = Mode.Normal;

    private double[] YArray;

    private GradientModel(ProfileColorSpace pcs, LCDTarget target, CIEXYZ white) {
        this.pcs = pcs;
        if (white == null) {
            this.white = (CIEXYZ) pcs.getReferenceWhite().clone();
        } else {
            this.white = (CIEXYZ) white.clone();
        }
        if (target != null) {
            this.target = target;
            this.imageChannel = target.getTargetChannel();
        }
        initCAM();
    }

    /**
     * ��o�v����RGB code curve
     * @return double[]
     */
    private double[] getCodeCurve() {
        int size = image.getWidth();
        double[] curve = new double[size];
        int[] RGBValues = new int[3];
        for (int x = 0; x < size; x++) {
            image.getRaster().getPixel(x, 0, RGBValues);
            double max = Maths.max(RGBValues);
            curve[x] = max;
        }
        return curve;
    }

    /**
     * �Hcode���ܤƨӰ�����t��index
     * @return int[]
     */
    private int[] detectIdealBorderIndexByCode() {
        double[] codeCurve = getCodeCurve();
        ArrayList<Integer> list = new ArrayList<Integer>();
        int size = codeCurve.length;
        for (int x = 0; x < size - 1; x++) {
            if (codeCurve[x] != codeCurve[x + 1]) {
                list.add(x);
            }
        }
        return Utils.list2IntArray(list);
    }

    /**
     * ��o��ɪ��T��E��
     * @return double[][]
     */
    double[][] getBorderXYZValues() {
        DeviceIndependentImage diImage = DeviceIndependentImage.getInstance(image, pcs);
        int[] indexArray = detectIdealBorderIndexByCode();
        int size = indexArray.length;
        double[][] borderXYZValues = new double[size][3];
        for (int x = 0; x < size; x++) {
            int index = indexArray[x];
            diImage.getXYZValues(index, 0, borderXYZValues[x]);
        }
        return borderXYZValues;
    }

    /**
     * �]�w�v�����Ѽ�
     * @param start int
     * @param end int
     * @param R boolean
     * @param G boolean
     * @param B boolean
     * @param scale int
     * @param width int
     * @param height int
     */
    protected void setupImage(int start, int end, boolean R, boolean G, boolean B, int scale, int width, int height) {
        image = GradientImage.getImage(new Dimension(width, height), start, end, R, G, B, false, false, scale, false, image);
        this.reset();
    }

    /**
     * �]�wsmooth�����B��ɩҨϥΪ�image�v��
     * @param start int
     * @param end int
     * @param ch Channel
     * @param scale int
     * @param width int
     */
    public void setupImage(int start, int end, RGBBase.Channel ch, int scale, int width) {
        setupImage(start, end, ch, scale, width, DEFAULT_IMAGE_HEIGHT);
    }

    /**
     * �w�]���v������, �ѩ󤣦Ҽ{�Ŷ����v�T, �ҥH�]1�N�n�����ܰ�.
     */
    public static final int DEFAULT_IMAGE_HEIGHT = 1;

    /**
     * �]�wsmooth�����B��ɩҨϥΪ�image�v��
     * @param start int code��start
     * @param end int code��end
     * @param ch Channel ��Ϊ�channel
     * @param scale int RGB�`�@���ܤƵ{��(�̦h���ܤƵ{�׬�0~255, �]�N�O256��)
     * @param width int �v�����e
     * @param height int �v������
     */
    public void setupImage(int start, int end, RGBBase.Channel ch, int scale, int width, int height) {
        boolean R = false, G = false, B = false;
        imageStart = start;
        imageEnd = end;
        switch(ch) {
            case R:
                R = true;
                break;
            case G:
                G = true;
                break;
            case B:
                B = true;
                break;
            case W:
                R = G = B = true;
                break;
        }
        setImageChannel(ch);
        setupImage(start, end, R, G, B, scale, width, height);
    }

    public void setImageChannel(RGBBase.Channel ch) {
        this.imageChannel = ch;
    }

    private double[][][] getXYZImage() {
        if (image != null) {
            DeviceIndependentImage di = DeviceIndependentImage.getInstance(image, pcs);
            PlaneImage oppImage = PlaneImage.getInstance(di, PlaneImage.Domain.XYZ);
            return oppImage.getCIEXYZImage();
        } else {
            return null;
        }
    }

    /**
     *
     * <p>Title: Colour Management System</p>
     *
     * <p>Description: a Colour Management System by Java</p>
     * ��񪺭p��覡
     *
     * <p>Copyright: Copyright (c) 2008</p>
     *
     * <p>Company: skygroup</p>
     *
     * @author skyforce
     * @version 1.0
     */
    public static enum Contrast {

        /**
         * target / background
         */
        Luminance, /**
         * (target - background) / (target + background)
         */
        Michelson, MichelsonModified
    }

    private class _Contrast {

        /**
         * ��X��񦱽u
         * @param type Contrast
         * @return double[]
         */
        public double[] getContrastCurve(Contrast type) {
            final double[][][] XYZValuesImage = getXYZImage();
            return getContrastCurve(type, XYZValuesImage);
        }

        /**
         *
         * @param type Contrast
         * @param values double[]
         * @return double
         */
        protected final double getContrast(Contrast type, double[] values) {
            double background = Interpolation.linear(0, 2, values[0], values[2], 1);
            double target = values[1];
            switch(type) {
                case Luminance:
                    return target / background;
                case Michelson:
                    return (target - background) / (target + background);
                case MichelsonModified:
                    double diff = target - background;
                    double angle = Math.atan2((values[2] - values[1]), 2.);
                    target = background = diff * Math.cos(angle);
                    return (target - background) / (target + background);
                default:
                    return -1;
            }
        }

        /**
         *
         * @param type Contrast
         * @param XYZValuesImage double[][][]
         * @return double[]
         */
        protected double[] getContrastCurve(Contrast type, double[][][] XYZValuesImage) {
            int h = XYZValuesImage.length;
            final double[][] row = XYZValuesImage[h / 2];
            int size = row.length;
            final double[] YArray = DoubleArray.transpose(row)[1];
            double[] contrastArray = new double[size - 3];
            for (int x = 2; x < size - 1; x++) {
                double background = Interpolation.linear(0, 2, YArray[x - 1], YArray[x + 1], 1);
                double target = YArray[x];
                switch(type) {
                    case Luminance:
                        contrastArray[x - 2] = target / background;
                        break;
                    case Michelson:
                        contrastArray[x - 2] = (target - background) / (target + background);
                        break;
                    case MichelsonModified:
                        double diff = target - background;
                        double angle = Math.atan2((YArray[x + 1] - YArray[x - 1]), 2.);
                        target = background = diff * Math.cos(angle);
                }
            }
            return contrastArray;
        }
    }

    public void reset() {
        if (mode != Mode.JNDIOnly) {
            jndiCurve = null;
        }
    }

    /**
     * ��o������Pattern�H�Ψ�smooth������
     * @return PatternAndScore
     */
    public abstract PatternAndScore getPatternAndScore();

    protected double thresholdPercent = 10;

    /**
     * �]�wthreshold percent
     * @param percent double
     */
    public void setThresholdPercent(double percent) {
        this.thresholdPercent = percent;
    }

    /**
     * �]�w��ĳ��threshold percent(���D�g���)
     * @param channel Channel
     */
    public void setRecommendThresholdPercent(RGBBase.Channel channel) {
        switch(channel) {
            case W:
                this.setThresholdPercent(10);
                break;
            case R:
                this.setThresholdPercent(30);
                break;
            case G:
                this.setThresholdPercent(10);
                break;
            case B:
                this.setThresholdPercent(20);
                break;
        }
    }

    public double getThresholdPercent() {
        return thresholdPercent;
    }

    protected double signalFixedThreshold = 2;

    public void setSignalFixedThreshold(double signalFixedThreshold) {
        this.signalFixedThreshold = signalFixedThreshold;
    }

    /**
     *
     * <p>Title: Colour Management System</p>
     *
     * <p>Description: a Colour Management System by Java</p>
     * ����pattern�H�ι�Msmooth���ƪ����O
     *
     * <p>Copyright: Copyright (c) 2008</p>
     *
     * <p>Company: skygroup</p>
     *
     * @author skyforce
     * @version 1.0
     */
    public static class PatternAndScore {

        public boolean isSmooth() {
            return patternList.size() == 0;
        }

        public PatternAndScore(List<Pattern> patternList, double score, double overScore, double[] signal, double[] deltaAccelArray) {
            this.patternList = patternList;
            this.score = score;
            this.overScore = overScore;
            this.signal = signal;
            this.deltaAccelArray = deltaAccelArray;
        }

        /**
         * �p��signal�ұĥΪ�signal, �q�`�N�O��JND Index
         */
        public double[] signal;

        /**
         * ������pattern
         */
        public List<Pattern> patternList;

        /**
         * smooth������, �V�C�Vsmooth
         */
        public double score;

        /**
         * �P�ؼХ[�t�ת��t��
         */
        public double[] deltaAccelArray;

        /**
         * ����������, �V�C�V���|����
         */
        public double overScore;
    }

    /**
     *
     * @param indexList ArrayDoubleList
     * @param signalList ArrayDoubleList
     * @return double[][]
     * @deprecated
     */
    protected static double[][] producePatternIndexAndSignal(ArrayDoubleList indexList, ArrayDoubleList signalList) {
        double[] indexResult = indexList.toArray();
        double[] signalResult = signalList.toArray();
        double[][] patternIndexAndSignal = DoubleArray.transpose(new double[][] { indexResult, signalResult });
        return patternIndexAndSignal;
    }

    /**
     *
     * @param lists ArrayDoubleList[]
     * @return double[][]
     * @deprecated
     */
    protected static double[][] producePatternIndexAndSignal(ArrayDoubleList... lists) {
        int size = lists.length;
        double[][] result = new double[size][];
        for (int x = 0; x < size; x++) {
            result[x] = lists[x].toArray();
        }
        double[][] patternIndexAndSignal = DoubleArray.transpose(result);
        return patternIndexAndSignal;
    }

    /**
     * ��ochannel������index.
     * @return int
     */
    protected int getChannelIndex() {
        return getChannelIndex(imageChannel);
    }

    /**
     * ��ochannel������index.
     * �]���bRGB��channel����index��k��, white�|������6.
     * �M�ӳo�䪺white�n������3, �ҥH�t�~�g�@�Ӥ�k�p��ChannelIndex
     * @param ch Channel
     * @return int
     */
    protected int getChannelIndex(RGBBase.Channel ch) {
        int index = ch.getArrayIndex();
        index = ch == RGBBase.Channel.W ? 3 : index;
        return index;
    }

    public double[] getJNDIndexCurve(CIExyY[] xyYArray) {
        CIEXYZ[] XYZArray = CIExyY.toXYZArray(xyYArray);
        return getJNDIndexCurve(XYZArray);
    }

    public double[] getJNDIndexCurve(CIEXYZ[] XYZArray) {
        int size = XYZArray.length;
        double[] jndiCurve = new double[size];
        for (int x = 0; x < size; x++) {
            jndiCurve[x] = getJNDIndex(XYZArray[x], hkStrategy);
        }
        return jndiCurve;
    }

    /**
     * �N�v����XYZ�নGSDF���u
     * �åB�Ҷq�F��׹�JNDI���v�T, �Ҷq���覡�ثe�� Nayatani�MCIECAM02.
     * ���MCIECAM02���èS���Ҷq���׹��ת��v�T, ��O��ڴ�յ��G��Nayatani��.
     * @return double[]
     */
    public double[] getJNDIndexCurve() {
        if (jndiCurve != null) {
            return jndiCurve;
        }
        if (mode == Mode.LuminanceOnly) {
            int size = this.YArray.length;
            jndiCurve = new double[size];
            for (int x = 0; x < size; x++) {
                double Y = YArray[x];
                jndiCurve[x] = GSDF.DICOM.getJNDIndex(Y);
            }
        } else {
            final double[][][] XYZValuesImage = getXYZImage();
            int h = XYZValuesImage.length;
            final double[][] row = XYZValuesImage[h / 2];
            int size = row.length;
            jndiCurve = new double[size];
            CIEXYZ XYZ = new CIEXYZ();
            for (int x = 0; x < size; x++) {
                XYZ.setValues(row[x]);
                jndiCurve[x] = getJNDIndex(XYZ, hkStrategy);
            }
        }
        return jndiCurve;
    }

    /**
     * gsdf���u, ��JNDIndex�զ�
     */
    private double[] jndiCurve;

    /**
     *
     * <p>Title: Colour Management System</p>
     *
     * <p>Description: a Colour Management System by Java</p>
     * �b�ѨMHK�����ұĥΪ���k
     *
     * <p>Copyright: Copyright (c) 2008</p>
     *
     * <p>Company: skygroup</p>
     *
     * @author skyforce
     * @version 1.0
     */
    public static enum HKStrategy {

        Nayatani, CIECAM02, CIELuv, None
    }

    private HKStrategy hkStrategy = HKStrategy.CIECAM02;

    /**
     * �]�wHK�������ѨM��k
     * @param strategy HKStrategy
     */
    public void setHKStrategy(HKStrategy strategy) {
        this.hkStrategy = strategy;
    }

    CIECAM02 cam = null;

    private ViewingConditions vc = null;

    /**
     * �إ�J<->JNDI�����Y
     */
    private CIECAM02JNDIndex camJNDIndex;

    /**
     * �إ�J<->Y�����Y
     */
    private CIECAM02JNDIndex[] camJNDIArray = null;

    protected LCDTarget target;

    /**
     * ��JNDIndex��^�G��(Y)
     * ����w�W�D, �h�H�w�]���v���W�D�@��M.
     * @param JNDIndex double
     * @return double
     */
    public double getLuminance(double JNDIndex) {
        return getLuminance(JNDIndex, this.hkStrategy);
    }

    /**
     * ��JNDIndex��^�G��(Y)
     * �w��w�W�D, �h�H��w�W�D����M��@��M.
     * @param JNDIndex double
     * @param ch Channel
     * @return double
     */
    public double getLuminance(double JNDIndex, RGBBase.Channel ch) {
        return getLuminance(JNDIndex, this.hkStrategy, ch);
    }

    public double[] getLuminanceCurve(double[] JNDIndexCurve) {
        int size = JNDIndexCurve.length;
        double[] luminanceCurve = new double[size];
        for (int x = 0; x < size; x++) {
            luminanceCurve[x] = getLuminance(JNDIndexCurve[x], this.hkStrategy);
        }
        return luminanceCurve;
    }

    /**
     * �NJNDIndex��^��Luminance, �B�n��HK�����v�h��
     * @param JNDIndex double
     * @param strategy HKStrategy
     * @return double
     */
    private double getLuminance(double JNDIndex, HKStrategy strategy) {
        return getLuminance(JNDIndex, strategy, imageChannel);
    }

    /**
     * �NJNDIndex��^��Luminance, �B�n��HK�����v�h��
     * @param JNDIndex double
     * @param strategy HKStrategy
     * @param ch Channel
     * @return double
     */
    private double getLuminance(double JNDIndex, HKStrategy strategy, RGBBase.Channel ch) {
        switch(strategy) {
            case None:
                return GSDF.DICOM.getLuminance(JNDIndex);
            case CIECAM02:
                return getCIECAM02Luminance(JNDIndex, ch);
            case Nayatani:
                throw new UnsupportedOperationException(strategy.name());
            case CIELuv:
                throw new UnsupportedOperationException(strategy.name());
            default:
                return -1;
        }
    }

    /**
     * ��oJNDIndex, �B�Ҽ{�FHK���v�T���F���v, �åB�H�w�]��HK���v�k�@���v.
     * @param XYZ CIEXYZ
     * @return double
     */
    public double getJNDIndex(final CIEXYZ XYZ) {
        return getJNDIndex(XYZ, this.hkStrategy);
    }

    /**
     * ��oJNDIndex, �B�Ҽ{�FHK���v�T���F���v
     * @param XYZ CIEXYZ
     * @param strategy HKStrategy
     * @return double
     */
    private double getJNDIndex(final CIEXYZ XYZ, HKStrategy strategy) {
        switch(strategy) {
            case None:
                return GSDF.DICOM.getJNDIndex(XYZ.Y);
            case CIECAM02:
                return getCIECAM02JNDIndex(XYZ);
            case Nayatani:
                throw new UnsupportedOperationException();
            case CIELuv:
                return getCIELuvJNDIndex(XYZ);
            default:
                return -1;
        }
    }

    /**
     * �HCIECAM�@���������ഫ�Ŷ�(XYZ->JCh->JNDI)
     * @param XYZ CIEXYZ
     * @return double
     */
    private double getCIECAM02JNDIndex(final CIEXYZ XYZ) {
        CIEXYZ clone = (CIEXYZ) XYZ.clone();
        clone.normalize(white);
        clone.normalize(NormalizeY.Normal100);
        CIECAM02Color color = cam.forward(clone);
        double JNDI = camJNDIndex.getJNDIndex(color.J);
        return JNDI;
    }

    private double getCIECAM02Luminance(double JNDIndex, RGBBase.Channel ch) {
        double lightness = camJNDIndex.getLightness(JNDIndex);
        int index = getChannelIndex(ch);
        return camJNDIArray[index].getMonochromeLuminance(lightness);
    }

    /**
     *
     * @param JNDIndex double
     * @param ch Channel
     * @return double
     * @todo H  CIELuv HK
     */
    private double getCIELuvLuminance(double JNDIndex, RGBBase.Channel ch) {
        return -1;
    }

    private CIELuvHKModel.Type LuvType = CIELuvHKModel.Type.SandersWyszecki;

    private double getCIELuvJNDIndex(final CIEXYZ XYZ) {
        CIELuv Luv = new CIELuv(XYZ, white);
        double L = CIELuvHKModel.getHKLightness(Luv, LuvType);
        Luv.L = L;
        double hkLumi = Luv.toXYZ().Y;
        return GSDF.DICOM.getJNDIndex(hkLumi);
    }

    private final boolean ignoreChannel(RGBBase.Channel ch) {
        RGBBase.Channel targetch = target != null ? target.getTargetChannel() : null;
        return target != null && (targetch == null ? false : ch != targetch);
    }

    private static int startCode = 0;

    private static int endCode = 255;

    public static final void setAnalyzeRange(int start, int end) {
        startCode = start;
        endCode = end;
    }

    private void initCAM() {
        CIEXYZ white = (CIEXYZ) this.white.clone();
        double La = white.Y * 0.2;
        white.normalizeY100();
        vc = new ViewingConditions(white, La, 20, Surround.Dim, "Dim");
        cam = new CIECAM02(vc);
        camJNDIndex = new CIECAM02JNDIndex(this.cam, this.white);
        camJNDIArray = new CIECAM02JNDIndex[4];
        for (RGBBase.Channel ch : RGBBase.Channel.RGBWChannel) {
            if (ignoreChannel(ch)) {
                continue;
            }
            this.setupImage(startCode, endCode, ch, 256, endCode - startCode + 1, 1);
            double[][][] XYZValuesImage = getXYZImage();
            double[][] row = XYZValuesImage[0];
            CIECAM02JNDIndex camjndi = new CIECAM02JNDIndex(this.cam, this.white);
            camjndi.setupMonochromeLUT(CIEXYZ.getCIEXYZArray(row));
            camJNDIArray[this.getChannelIndex(ch)] = camjndi;
        }
    }

    /**
     * �z�Q����tsignal��index��
     * @return int[]
     * @deprecated
     */
    private int[] getIdealBorderSignalIndex() {
        int[] borderIndexes = detectIdealBorderIndexByCode();
        int size = borderIndexes.length - 1;
        int[] signalIndex = new int[size * 2];
        for (int x = 0; x < size; x++) {
            signalIndex[x * 2] = borderIndexes[x];
            signalIndex[x * 2 + 1] = borderIndexes[x] + 1;
        }
        return signalIndex;
    }

    /**
     * �p��pattern���`�M, �]�N�Osmooth������, �V�C�V�n
     * @param patternList List
     * @return double
     */
    protected static final double getPatternScore(List<Pattern> patternList) {
        double score = 0;
        for (Pattern p : patternList) {
            score += Math.abs(p.pattern);
        }
        return score;
    }

    /**
     * �p��F��H���i���Ѫ�pattern������`�M
     * @param patternList List
     * @return double
     */
    protected static final double getOverScore(List<Pattern> patternList) {
        double score = 0;
        for (Pattern p : patternList) {
            double overRatio = Math.abs(p.overRatio);
            if (overRatio > 100f) {
                score += overRatio / 100.;
            }
        }
        return score;
    }
}
