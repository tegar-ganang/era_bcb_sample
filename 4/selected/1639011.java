package vv.cms.lcd.calibrate.measured;

import java.io.*;
import java.util.*;
import shu.cms.*;
import shu.cms.colorspace.depend.*;
import shu.cms.colorspace.independ.*;
import shu.cms.hvs.gradient.*;
import shu.cms.lcd.*;
import vv.cms.lcd.calibrate.*;
import vv.cms.lcd.calibrate.measured.algo.*;
import vv.cms.lcd.calibrate.measured.util.*;
import vv.cms.lcd.calibrate.parameter.*;
import shu.cms.lcd.material.*;
import shu.cms.measure.*;
import vv.cms.measure.cp.*;
import shu.cms.plot.*;
import shu.cms.util.*;
import shu.math.array.*;
import shu.util.log.*;
import vv.cms.lcd.material.*;

/**
 * <p>Title: Colour Management System</p>
 *
 * <p>Description: a Colour Management System by Java</p>
 * ���q��y�{���ե��{��
 *
 * <p>Copyright: Copyright (c) 2008</p>
 *
 * <p>Company: skygroup</p>
 *
 * @author skyforce
 * @version 1.0
 */
public abstract class MeasuredCalibrator implements CalibratorConst, Plottable {

    protected String rootDir = ".";

    public final void setRootDir(String root) {
        this.rootDir = root;
    }

    /**
   *
   * @param logoLCDTaget LCDTarget
   * @param mc MeasuredCalibrator
   */
    protected MeasuredCalibrator(LCDTarget logoLCDTaget, MeasuredCalibrator mc) {
        this(logoLCDTaget, mc.mm, mc.cp, mc.wp, mc.ap, mc.mp);
        this.rootDir = mc.rootDir;
    }

    /**
   *
   * @param logoLCDTaget LCDTarget
   * @param meterMeasurement MeterMeasurement
   * @param p ColorProofParameter
   * @param wp WhiteParameter
   * @param ap GreenAdjustParameter
   * @param mp MeasureParameter
   */
    public MeasuredCalibrator(LCDTarget logoLCDTaget, MeterMeasurement meterMeasurement, ColorProofParameter p, WhiteParameter wp, AdjustParameter ap, MeasureParameter mp) {
        this.cp = p;
        this.wp = wp;
        this.ap = ap;
        this.mp = mp;
        this.mm = meterMeasurement;
        this.ic = p.icBits;
        this.maxValue = p.calibrateBits;
        this.logoLCDTaget = logoLCDTaget;
        this.mi = this.cpm = getCPCodeMeasurement(meterMeasurement, ic, mp);
        List<RGB> cpcodeList = logoLCDTaget.filter.rgbList();
        this.cpcodeRGBArray = RGBArray.toRGBArray(cpcodeList);
        this.originalCalTarget = LCDTargetUtils.getLCDTargetWithLinearRGB(logoLCDTaget, LCDTarget.Number.Ramp256W);
        this.targetxyYArray = originalCalTarget.filter.xyYArray();
        this.gm = new GSDFGradientModel(originalCalTarget);
        if (!this.considerHKEffect) {
            this.gm.setHKStrategy(GSDFGradientModel.HKStrategy.None);
        }
        this.wcc = new WhiteCodeCalculator(cpcodeRGBArray);
    }

    /**
   * ��ocpm�åB���]�w
   * @param meterMeasurement MeterMeasurement
   * @param icBits MaxValue
   * @param mp MeasureParameter
   * @return CPCodeMeasurement
   */
    static final CPCodeMeasurement getCPCodeMeasurement(MeterMeasurement meterMeasurement, RGBBase.MaxValue icBits, MeasureParameter mp) {
        CPCodeMeasurement cpm = CPCodeMeasurement.getInstance(meterMeasurement, icBits, cpmBufferFilename, mp);
        cpm.setBufferMeasure(mp.bufferMeasure);
        cpm.setAcceptDifference(mp.CPCodeAcceptDifference);
        return cpm;
    }

    protected MeasureInterface mi;

    private static String cpmBufferFilename = null;

    public static final void setCPMBufferFilename(String filename) {
        cpmBufferFilename = filename;
    }

    public void storeCPCodeMeasurement() {
        cpm.storeBuffer("cpm.buf");
    }

    /**
   * �ֿn���q���
   * @return int
   */
    public int getAccumulateMeasureCount() {
        return cpm.getAccumulateMeasureCount();
    }

    /**
   * ��o�ե��L�{����T
   * @return String
   */
    public abstract String getCalibratedInfomation();

    /**
   * ����ե�, �����I�s��
   * @return RGB[]
   */
    protected abstract RGB[] _calibrate();

    /**
   * ����ե�
   * @return RGB[]
   */
    public RGB[] calibrate() {
        if (this.maxValue != null) {
            produceStart();
            RGB[] result = _calibrate();
            produceEnd();
            return Arrays.copyOf(result, result.length);
        } else {
            throw new IllegalStateException("this.maxValue == null");
        }
    }

    /**
   * ��O���`�ɶ�
   * @return long
   */
    public final long getCostTime() {
        return timeConsumption.getCostTime();
    }

    protected void produceStart() {
        timeConsumption.start();
        Logger.log.trace(this.getClass().getName() + " start");
    }

    protected void produceEnd() {
        timeConsumption.end();
        Logger.log.trace(this.getClass().getName() + " end");
        Logger.log.info("costTime: " + timeConsumption.getCostTime());
    }

    protected transient TimeConsumption timeConsumption = new TimeConsumption();

    /**
   * �^�Ǯե����G, �]�LRGB�H�Υؼ�XYZ��
   * @return List
   */
    public abstract List<Patch> getCalibratedPatchList();

    /**
   * ��o��l��LCDTarget
   * @return LCDTarget
   */
    protected LCDTarget getOriginalTarget() {
        return originalCalTarget;
    }

    public void setPlotting(boolean plotting) {
        this.plotting = plotting;
    }

    protected boolean plotting = false;

    private Plot2D plot;

    private GSDFGradientModel gm;

    protected MeterMeasurement mm;

    /**
   * �Nrgb������ramp���ؼЭ�
   */
    private LCDTarget originalCalTarget;

    private LCDTarget relativeCalTarget;

    private RGB[] cpcodeRGBArray;

    protected RGB.MaxValue ic;

    protected RGB.MaxValue maxValue;

    /**
   * �ե��ؼЭ�
   */
    protected LCDTarget logoLCDTaget;

    private CPCodeMeasurement cpm;

    protected MeasureParameter mp;

    protected AdjustParameter ap;

    protected ColorProofParameter cp;

    protected WhiteParameter wp;

    /**
   * �C���q��Ȧs���G
   */
    private LCDTarget measuredLCDTarget;

    /**
   * �ؼ�xyY���}�C
   */
    private CIExyY[] targetxyYArray;

    /**
   * ���Icode�p�⾹
   */
    protected WhiteCodeCalculator wcc;

    protected RGB getWhiteCPCode() {
        return cpcodeRGBArray[cpcodeRGBArray.length - 1];
    }

    /**
   * ��targetLogoFilename�Ҩ�X����lcp code
   * @return RGB[]
   */
    protected RGB[] getCPCodeRGBArray() {
        return cpcodeRGBArray;
    }

    /**
   * ��o�ؼ�xyY���}�C
   * @return CIExyY[]
   */
    protected CIExyY[] getTargetxyYArray() {
        return this.targetxyYArray;
    }

    /**
   * ��o���I���ؼ�xyY
   * @return CIExyY
   */
    protected CIExyY getWhitexyY() {
        int size = targetxyYArray.length;
        return targetxyYArray[size - 1];
    }

    /**
   * ��׬O�_�n���۹�վ�
   */
    private boolean chromaticityRelative = AutoCPOptions.get("MeasuredCalibrator_ChromaticityRelative");

    /**
   * �O�_�n�Ҽ{HK����? �Ȯɥ�����.
   * �]�����ǭ��O�����I��í�w, �?�ɶq��쪺�Ȥ���I�٭n�j��, ���ɦbCIECAM02���ͪ���Ӫ?,
   * �ѩ�W�X�̤j��, �ҥH�|��clip�{�H, �o�˪�clip�{�H�|�������Ӧ��t�����ƭ��ܦ��ۦP.
   * �Ӧb��M�̱���Ȯ�, �o�{��̬ۦP�����p�U, �N�|�����~���M.
   */
    private boolean considerHKEffect = AutoCPOptions.get("MeasuredCalibrator_ConsiderHKEffect");

    /**
   * ���ͫG�ת��۹�ؼЭ�
   * @param measure LCDTarget
   */
    protected final void initRelativeTarget(LCDTarget measure) {
        initRelativeTarget(measure, null);
    }

    protected LCDTarget getWhiteRelativeTarget(LCDTarget measureTarget) {
        return RelativeTarget.getLuminanceAndChromaticityRelativeInstance(originalCalTarget, measureTarget, cp.turnCode);
    }

    /**
   * ��l�Ƭ۹�Target
   * @param measureTarget LCDTarget �ΨӲ��ͬ۹�Target���ѦҶq��Target
   * @param channel Channel ���ͬ۹�Target��channel
   */
    private final void initRelativeTarget(LCDTarget measureTarget, RGBBase.Channel channel) {
        if (!this.useRelativeTarget) {
            return;
        }
        if (true == chromaticityRelative && cp != null && (channel == RGBBase.Channel.W || channel == null)) {
            relativeCalTarget = getWhiteRelativeTarget(measureTarget);
        } else {
            if (channel != null && channel != RGBBase.Channel.W) {
                relativeCalTarget = RelativeTarget.getLuminanceRelativeInstance(originalCalTarget, measureTarget, channel);
            } else {
                relativeCalTarget = RelativeTarget.getLuminanceRelativeInstance(originalCalTarget, measureTarget, RGBBase.Channel.W);
            }
        }
        LCDTarget.Number number = MeasuredUtils.getMeasureNumber(channel, true, false);
        relativeCalTarget = LCDTargetUtils.getReplacedLCDTarget(relativeCalTarget, number);
        try {
            String className = this.getClass().getSimpleName();
            String filename = rootDir + "/" + MeaRelativeLogoFilename + "-[" + className + "].logo";
            LCDTarget.IO.store(relativeCalTarget, filename);
        } catch (IOException ex) {
            Logger.log.error("", ex);
        }
        this.targetxyYArray = relativeCalTarget.filter.xyYArray();
    }

    protected void loadCPCode(RGB[] rgbArray) {
        CPCodeLoader.load(rgbArray, this.ic);
    }

    /**
   * �Ncp code target���lut��
   */
    protected void loadCPCodeTarget() {
        loadCPCode(this.cpcodeRGBArray);
    }

    /**
   * �O�_���ͬ۹�ؼЭȨӧ@�ե�
   */
    private boolean useRelativeTarget = AutoCPOptions.get("MeasuredCalibrator_UseRelativeTarget");

    protected LCDTarget getCalibratedTarget() {
        return useRelativeTarget ? relativeCalTarget : originalCalTarget;
    }

    protected JNDI jndi = new JNDI();

    public class JNDI implements JNDIInterface {

        protected double[] plotDeltaJNDI(String name, boolean plot, List<Patch> patchList) {
            double[] delta = calculateDeltaJNDICurve(patchList);
            if (plot && plotting) {
                plotDeltaJNDI(delta, name);
            }
            return delta;
        }

        /**
     * �Nmeasure��Target�p��delta JNDI
     * @param measure LCDTarget
     * @return double[]
     */
        protected double[] calculateDeltaJNDICurve(LCDTarget measure) {
            CIEXYZ[] XYZArray = measure.filter.XYZArray();
            if (measure.getNumber() == LCDTargetBase.Number.Ramp256R_W || measure.getNumber() == LCDTargetBase.Number.Ramp256G_W || measure.getNumber() == LCDTargetBase.Number.Ramp256B_W) {
                XYZArray = Arrays.copyOf(XYZArray, XYZArray.length - 1);
            }
            return calculateDeltaJNDICurve(XYZArray);
        }

        /**
     * �qpatchList�p��delta JNDI
     * @param patchList List
     * @return double[]
     */
        protected double[] calculateDeltaJNDICurve(List<Patch> patchList) {
            List<CIEXYZ> XYZList = Patch.Filter.XYZList(patchList);
            CIEXYZ[] XYZArray = XYZList.toArray(new CIEXYZ[XYZList.size()]);
            double[] deltaJNDICurve = calculateDeltaJNDICurve(XYZArray);
            return deltaJNDICurve;
        }

        /**
     * �qXYZArray�p��delta JNDI
     * @param XYZArray CIEXYZ[]
     * @return double[]
     */
        protected double[] calculateDeltaJNDICurve(CIEXYZ[] XYZArray) {
            double[] measureJNDICurve = gm.getJNDIndexCurve(XYZArray);
            double[] deltaJNDICurve = DoubleArray.minus(measureJNDICurve, getTargetJNDICurve());
            return deltaJNDICurve;
        }

        private double[] targetJNDICurve;

        protected double[] getJNDICurve(LCDTarget lcdTarget) {
            return gm.getJNDIndexCurve(lcdTarget.filter.XYZArray());
        }

        protected double[] getTargetJNDICurve() {
            if (targetJNDICurve == null) {
                targetJNDICurve = getJNDICurve(getCalibratedTarget());
            }
            return targetJNDICurve;
        }

        protected void setTargetJNDICurve(double[] targetJNDICurve) {
            this.targetJNDICurve = targetJNDICurve;
        }

        protected final double getJNDI(LCDTargetInterpolator interp, double code, RGBBase.Channel channel, GSDFGradientModel gm) {
            Patch patch = interp.getPatch(channel, code);
            CIEXYZ XYZ = patch.getXYZ();
            return gm.getJNDIndex(XYZ);
        }

        /**
     * �p��JNDI, �f�tGSDFGradientModel�p��XJNDI
     * @param XYZ CIEXYZ
     * @return double
     */
        public double getJNDI(CIEXYZ XYZ) {
            return gm.getJNDIndex(XYZ);
        }

        /**
     * �p��JNDI
     * @param lcdTarget LCDTarget
     * @param code int
     * @return double
     */
        protected double getJNDI(LCDTarget lcdTarget, int code) {
            Patch patch = lcdTarget.getPatch(RGBBase.Channel.W, code, RGB.MaxValue.Int8Bit);
            return getJNDI(patch.getXYZ());
        }

        protected double getJNDI(LCDTargetInterpolator interp, double code, RGBBase.Channel channel) {
            Patch patch = interp.getPatch(channel, code);
            return getJNDI(patch.getXYZ());
        }

        protected Plot2D plotDeltaJNDI(double[] deltaJNDICurve, String name) {
            if (!plotting) {
                return null;
            }
            if (plot == null) {
                plot = Plot2D.getInstance("delta JNDI");
            }
            plot.addLinePlot(name, 0, deltaJNDICurve.length - 1, deltaJNDICurve);
            plot.setAxeLabel(0, "code");
            plot.setAxeLabel(1, "deltaJNDI");
            plot.addLegend();
            plot.setVisible();
            plot.setFixedBounds(0, 0, 255);
            return plot;
        }
    }

    /**
   *
   * <p>Title: Colour Management System</p>
   *
   * <p>Description: a Colour Management System by Java</p>
   * �Ψӷf�tĲ�o�q����
   *
   * <p>Copyright: Copyright (c) 2008</p>
   *
   * <p>Company: skygroup</p>
   *
   * @author skyforce
   * @version 1.0
   */
    protected static class Trigger implements MeasureTrigger {

        private boolean[] calibrated;

        protected Trigger(boolean[] calibrated) {
            this.calibrated = calibrated;
        }

        /**
     * hasNextMeasure
     * �O�_�٦���h�q��ݭn�i��
     *
     * @return boolean
     */
        public boolean hasNextMeasure() {
            return getUncalibratedCount() != 0;
        }

        public int getUncalibratedCount() {
            int count = 0;
            for (boolean cal : calibrated) {
                if (cal == false) {
                    count++;
                }
            }
            return count;
        }
    }

    /**
   * �ե�����l��
   * ��l�Ƨ@�F:
   * 1. ��J��¦cp code
   * 2. �q��256��Ϊ̥|��
   * 3. �̷ӤW�@�B�J���q��, ���� �۹�LCDTarget, �����s���ؼ�Target
   * �۹�LCDTarget���ηN�b��, ���F���CLCD�G�ת��ܤƼv�T��ե����G.
   *
   * @param channel Channel
   * @param ramp256Measure boolean �O�_�n��256�Ƕ����q��?
   * @return LCDTarget
   */
    protected final LCDTarget initMeasure(RGBBase.Channel channel, boolean ramp256Measure) {
        cpm.reset();
        LCDTargetBase.Number number = MeasuredUtils.getMeasureNumber(channel, ramp256Measure, false);
        LCDTarget ramp = null;
        if (number == LCDTargetBase.Number.FiveColor) {
            RGB[] wrgbk = new RGB[5];
            for (int x = 0; x < 4; x++) {
                wrgbk[x] = (RGB) cpcodeRGBArray[cpcodeRGBArray.length - 1].clone();
            }
            for (int x = 1; x < 4; x++) {
                RGBBase.Channel ch = RGBBase.Channel.getChannel(x);
                wrgbk[x].reserveValue(ch);
            }
            wrgbk[4] = (RGB) cpcodeRGBArray[0].clone();
            ramp = this.measure(wrgbk);
        } else if (number == LCDTargetBase.Number.BlackAndWhite) {
            RGB[] bw = new RGB[2];
            bw[0] = (RGB) cpcodeRGBArray[0].clone();
            bw[1] = (RGB) cpcodeRGBArray[cpcodeRGBArray.length - 1].clone();
            ramp = mp.whiteSequenceMeasure ? this.measureWhite(bw) : this.measure(bw);
        } else {
            ramp = this.measure(this.cpcodeRGBArray, channel, null, false, true);
            number = ramp.getNumber();
        }
        ramp = LCDTargetUtils.getLCDTargetWithLinearRGB(ramp, number);
        this.initRelativeTarget(ramp, channel);
        return ramp;
    }

    protected void setChromaticityRelative(boolean chromaticityRelative) {
        this.chromaticityRelative = chromaticityRelative;
    }

    /**
   * �O�_�nlog��detail
   */
    private static final boolean LoggingDetail = AutoCPOptions.get("MeasuredCalibrator_LoggingDetail");

    /**
   * log��detail����T
   * @param msg String
   */
    protected static final void traceDetail(String msg) {
        if (LoggingDetail) {
            Logger.log.trace(msg);
        }
    }

    /**
   * ��RGB�]�w���y����RGB
   * @param patchList List
   * @param channel Channel
   */
    private void setSerialIntegerRGB(List<Patch> patchList, RGBBase.Channel channel) {
        int size = patchList.size();
        for (int x = 0; x < size; x++) {
            Patch patch = patchList.get(x);
            RGB rgb = (RGB) patch.getRGB().clone();
            RGB orgRGB = (RGB) patch.getOriginalRGB().clone();
            rgb.setValue(channel, x);
            orgRGB.setValue(channel, x);
            Patch.Operator.setRGB(patch, rgb);
            Patch.Operator.setOriginalRGB(patch, orgRGB);
        }
    }

    /**
   * �D�q��Ƕ��ɨϥ�
   *
   * @param rgbArray RGB[]
   * @return LCDTarget
   */
    protected LCDTarget measure(RGB[] rgbArray) {
        List<Patch> patchList = cpm.measure(rgbArray, true);
        LCDTarget measureLCDTarget = LCDTarget.Instance.get(patchList, LCDTarget.Number.Unknow, this.mm.isDo255InverseMode());
        this.measuredLCDTarget = measureLCDTarget;
        return measureLCDTarget;
    }

    protected LCDTarget measureWhite(RGB[] whiteRGBArray) {
        int sequenceMeasureCount = mp.sequenceMeasureCount;
        RGB[] measureRGBArray = getMeasureWhiteRGBArray(whiteRGBArray, sequenceMeasureCount);
        List<Patch> patchList = cpm.directMeasureResult(RGBArray.toRGBList(measureRGBArray)).result;
        int width = sequenceMeasureCount + 1;
        int realSize = patchList.size() / width;
        List<Patch> realPatchList = new ArrayList<Patch>(realSize);
        for (int x = 0; x < realSize; x++) {
            int index = -1 + width * (x + 1);
            Patch p = patchList.get(index);
            realPatchList.add(p);
        }
        LCDTarget measureLCDTarget = LCDTarget.Instance.get(realPatchList, LCDTarget.Number.Unknow, this.mm.isDo255InverseMode());
        return measureLCDTarget;
    }

    static final RGB[] getMeasureWhiteRGBArray(RGB[] whiteRGBArray, int sequenceMeasureCount) {
        int size = whiteRGBArray.length;
        RGB[] result = new RGB[size * (1 + sequenceMeasureCount)];
        int index = 0;
        for (int x = 0; x < size; x++) {
            RGB measurergb = whiteRGBArray[x];
            for (int c = 0; c < sequenceMeasureCount; c++) {
                RGB rgb = (RGB) measurergb.clone();
                rgb.addValues(-(sequenceMeasureCount - c), RGB.MaxValue.Double255);
                rgb.rationalize();
                result[index++] = rgb;
            }
            result[index++] = measurergb;
        }
        return result;
    }

    /**
   * �̷ӳ]�w�åB�q��rgbArray�����e, �æ^�Ǭ�LCDTarget
   * �q��Ƕ��ɨϥ�
   *
   * @param rgbArray RGB[]
   * @param channel Channel
   * @param whiteRGB RGB �b�D�q��W���ɭ�, �n�ĥΪ�white RGB code. �p�G�]�w��null,
   *  �h�۰ʱĥ�rgbArray���̫�@�ӷ�@white
   * @param serialIntegerRGB boolean �q���NRGB���������RGB(����LCDTarget��ramp�q��ĪG)
   * @param withWhite boolean �p�G�S�����I���ɭԬO�_�n�a��
   * @return LCDTarget
   */
    protected LCDTarget measure(final RGB[] rgbArray, RGBBase.Channel channel, RGB whiteRGB, boolean serialIntegerRGB, boolean withWhite) {
        RGB[] measureRGBArray = MeasuredUtils.getMeasureRGBArray(rgbArray, channel);
        List<Patch> patchList = cpm.measure(measureRGBArray, true);
        if (serialIntegerRGB) {
            setSerialIntegerRGB(patchList, channel);
        }
        if (whiteRGB == null) {
            whiteRGB = rgbArray[rgbArray.length - 1];
        }
        if (channel != RGBBase.Channel.W && withWhite) {
            Patch white = cpm.measure(whiteRGB, true);
            if (serialIntegerRGB) {
                Patch.Operator.setRGB(white, RGB.White);
                Patch.Operator.setOriginalRGB(white, RGB.White);
            }
            patchList.add(white);
        }
        LCDTarget.Number number = MeasuredUtils.getMeasureNumber(channel, true, withWhite);
        LCDTarget measureLCDTarget = LCDTarget.Instance.get(patchList, number, this.mm.isDo255InverseMode());
        this.measuredLCDTarget = measureLCDTarget;
        return measureLCDTarget;
    }

    protected LCDTarget getMeasuredLCDTarget() {
        return measuredLCDTarget;
    }
}
