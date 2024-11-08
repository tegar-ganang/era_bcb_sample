package shu.cms.lcd;

import java.util.*;
import shu.cms.*;
import shu.cms.colorspace.depend.*;
import shu.cms.colorspace.independ.*;
import shu.cms.measure.meter.*;
import shu.cms.plot.*;
import shu.math.array.*;
import shu.util.*;

/**
 * <p>Title: Colour Management System</p>
 *
 * <p>Description: </p>
 * LCD�ɪ�,�����x�sXYZRGB��Patch
 * �䴩i1pro/spectrolino�H���Я�q���G�Ȫ����x�s���榡����
 * �䴩i1display2�HLab���x�s���榡����
 *
 * <p>Copyright: Copyright (c) 2006</p>
 *
 * <p>Company: </p>
 *
 * @author cms.shu.edu.tw
 * @version 1.0
 */
public final class LCDTarget extends LCDTargetBase {

    /**
   * �C���@��code,�n�[�h��
   * @return double
   */
    public double getStep() {
        Number number = getNumber();
        switch(number) {
            case Ramp257_6Bit:
            case Ramp260:
            case Ramp509:
            case Ramp896:
            case Ramp897:
            case Ramp256W:
            case Ramp256G:
            case Ramp256R_W:
            case Ramp256G_W:
            case Ramp256B_W:
            case Ramp1021:
            case Ramp1024:
            case Ramp1792:
            case Ramp2048:
            case Ramp3577:
            case Ramp4096:
            case Ramp7147:
            case Ramp7168:
                return (number.getStep() / 255) * this.getMaxValue().max;
            default:
                return -1;
        }
    }

    /**
   * �`�@���X��code���ܤ�
   * @return double
   */
    public int getLevel() {
        return (int) (getMaxValue().max / getStep()) + 1;
    }

    public final Meter.Instr getInstrument() {
        return measureMetadata.instrument;
    }

    public final Meter getMeasureMeter() {
        return measureMetadata.meter;
    }

    public final Number getNumber() {
        return measureMetadata.number;
    }

    final void setNumber(Number number) {
        measureMetadata.number = number;
    }

    protected LCDTarget(List<Spectra> spectraList, List<RGB> rgbList, Number type, boolean inverseModeMeasure) {
        this(Patch.Produce.XYZRGBPatches(spectraList, null, rgbList, defaultCMF), type, inverseModeMeasure);
    }

    protected LCDTarget(List<Patch> patchList, Number number, boolean inverseModeMeasure) {
        this(patchList, number, false, inverseModeMeasure);
    }

    protected LCDTarget(List<Patch> patchList, Number number, boolean isRGBPatchOnly, boolean inverseModeMeasure) {
        super(patchList);
        this.inverseModeMeasure = inverseModeMeasure;
        this.isRGBPatchOnly = isRGBPatchOnly;
        if (number == null) {
            Number guess = Number.getNumber(patchList.size());
            guess = (guess == null) ? Number.Unknow : guess;
            this.setNumber(guess);
        } else {
            this.setNumber(number);
            if (number.getPatchCount() > 0 && number.getPatchCount() != patchList.size()) {
                throw new IllegalArgumentException("number.patchNumber != patchList.size()!");
            }
        }
        if (!isRGBPatchOnly && !checkPatchTypeRight()) {
            throw new IllegalArgumentException("Patch type is not right!");
        }
        Patch whitePatch = getWhitePatch();
        if (!isRGBPatchOnly && whitePatch != null) {
            CIEXYZ white = whitePatch.getXYZ();
            luminance = (CIEXYZ) white.clone();
            calculateNormalizedXYZ();
            Patch.Produce.LabPatches(patchList, white);
        }
        if (this.inverseModeMeasure && number.isRamp()) {
            LCDTarget.Operator.clippingFix(this, 254);
            this.inverseModeMeasure = false;
        }
    }

    protected boolean isRGBPatchOnly = false;

    /**
   * �p��Ҧ�Patch��Lab,�Hwhite���Ѧҥ�
   * @param white CIEXYZ
   */
    public void calculatePatchLab(CIEXYZ white) {
        Patch.Produce.LabPatches(patchList, white);
    }

    public static void example(String[] args) {
        LCDTarget lcdTarget = LCDTarget.Instance.get("Dell_M1210", Source.i1display2, Room.Dark, TargetIlluminant.D65, Number.Ramp1021, null, null);
        List<Patch> patchList = lcdTarget.getPatchList();
        List<Patch> LabPatchList = lcdTarget.getLabPatchList();
        for (int x = 0; x < patchList.size(); x++) {
            Patch p1 = patchList.get(x);
            Patch p2 = LabPatchList.get(x);
            System.out.println(p1.getRGB() + " " + p1.getXYZ() + " " + p2.getLab());
        }
        LCDTarget lcdTarget509 = lcdTarget.targetFilter.getRamp509From1021();
        List<Patch> patches = lcdTarget509.getLabPatchList();
        for (Patch p : patches) {
            System.out.println(p.getRGB());
        }
    }

    /**
   * ��Xchannel�̹��M�����
   * @param channel Channel
   * @return Patch
   */
    public Patch getSaturatedChannelPatch(RGBBase.Channel channel) {
        RGB rgb = patchList.get(0).getRGB();
        double max = rgb.getMaxValue().max;
        for (Patch p : patchList) {
            if (Patch.hasOnlyOneValue(p)) {
                rgb = p.getRGB();
                if (rgb.getValue(channel) == max) {
                    return p;
                }
            }
        }
        return null;
    }

    public static void main(String[] args) {
        LCDTarget target = LCDTarget.Instance.get("auo_M240HW01", Source.CA210, Number.Ramp1021, FileType.Logo, "091216-2", null);
        List<Patch> patchList = target.filter.grayScalePatch(RGB.Channel.B);
        Plot2D plot = Plot2D.getInstance();
        for (Patch p : patchList) {
            CIExyY xyY = new CIExyY(p.getXYZ());
            plot.addScatterPlot("", xyY.x, xyY.y);
        }
        plot.setVisible();
    }

    public final class TargetFilter {

        public LCDTarget getWHQL() {
            Number number = getNumber();
            switch(number) {
                case Complex729_46_96_1352:
                    return getWHQLFrom2223();
                case Complex729_46_96:
                    return getWHQLFrom871();
                default:
                    throw new IllegalArgumentException("Unsupported number: " + number);
            }
        }

        private LCDTarget getWHQLFrom2223() {
            Number number = getNumber();
            if (number != Number.Complex729_46_96_1352) {
                throw new IllegalStateException("number != Number.Complex729_46_96_1352");
            }
            return getPartLCDTarget(Number.WHQL, "WHQL from 2223", 729, 775);
        }

        private LCDTarget getWHQLFrom871() {
            Number number = getNumber();
            if (number != Number.Complex729_46_96) {
                throw new IllegalStateException("number != Number.Complex729_46_96");
            }
            return getPartLCDTarget(Number.WHQL, "WHQL from 871", 729, 775);
        }

        public LCDTarget get(LCDTarget.Number number) {
            switch(number) {
                case Ramp1021:
                    return getRamp1021();
                case Ramp256W:
                    return getRamp256W();
                case Ramp257_6Bit:
                    return getRamp257();
                case Ramp260:
                    return getRamp260();
                case Xtalk769:
                    return getXtalk();
                default:
                    throw new IllegalArgumentException("Unsupported number: " + number);
            }
        }

        private LCDTarget parent;

        private TargetFilter(LCDTarget parent) {
            this.parent = parent;
        }

        public LCDTarget getRamp896From1792() {
            if (getNumber() != Number.Ramp1792) {
                throw new IllegalStateException("getNumber() != Number.Ramp17922");
            }
            List<Patch> tmp = new ArrayList<Patch>(896);
            double[] int255Values = new double[3];
            int[] rgbV = new int[3];
            for (Patch p : getPatchList()) {
                RGB rgb = p.getRGB();
                rgb.getValues(int255Values, RGB.MaxValue.Int8Bit);
                rgbV[0] = (int) int255Values[0];
                rgbV[1] = (int) int255Values[1];
                rgbV[2] = (int) int255Values[2];
                if (rgbV[0] != 0 && rgbV[0] != 254 && (rgbV[0] % 2 == 0 || rgbV[0] == 255) || rgbV[1] != 0 && rgbV[1] != 254 && (rgbV[1] % 2 == 0 || rgbV[1] == 255) || rgbV[2] != 0 && rgbV[2] != 254 && (rgbV[2] % 2 == 0 || rgbV[2] == 255) || rgb.isWhite() || rgb.isBlack()) {
                    tmp.add(p);
                }
            }
            LCDTarget lcdTarget = new LCDTarget(tmp, Number.Ramp896, inverseModeMeasure);
            initLCDTarget(lcdTarget, " 896From1792");
            return lcdTarget;
        }

        public LCDTarget getRamp257() {
            switch(getNumber()) {
                case Complex257_4913_3073:
                    return getPartLCDTarget(Number.Ramp257_6Bit, " 257 From 8243", 0, 257);
                case Ramp257_6Bit:
                    return this.parent;
                case Ramp260:
                    return getPartLCDTarget(Number.Ramp257_6Bit, " 257 From 260", 0, 64, 65, 129, 130, 194, 195, 260);
                case Ramp1021:
                case Ramp1024:
                    return getRamp260From102x().targetFilter.getRamp257();
                default:
                    throw new IllegalStateException("Unsupported LCDTargetBase.Number: " + getNumber());
            }
        }

        public LCDTarget getRamp256W() {
            switch(getNumber()) {
                case Ramp1024:
                case Ramp1021:
                    return getRampW256From102x();
                case Ramp256W:
                    return this.parent;
                default:
                    throw new IllegalStateException("Unsupported LCDTargetBase.Number: " + getNumber());
            }
        }

        private LCDTarget getRampW256From102x() {
            if (getNumber() != Number.Ramp1024 && getNumber() != Number.Ramp1021) {
                throw new IllegalStateException("getNumber() != Number.Ramp1024 & Number.Ramp1021");
            }
            String desc = getNumber() == Number.Ramp1024 ? " 256WFrom1024" : " 256WFrom1021";
            return this.getPartLCDTarget(Number.Ramp256W, desc, 0, 256);
        }

        public LCDTarget getRamp256(RGBBase.Channel ch, boolean withWhite) {
            switch(getNumber()) {
                case Ramp1021:
                case Ramp1024:
                    return getRamp256From102x(ch, withWhite);
                case Ramp256R_W:
                    return this.getByNumber(Number.Ramp256R, null);
                case Ramp256G_W:
                    return this.getByNumber(Number.Ramp256G, null);
                case Ramp256B_W:
                    return this.getByNumber(Number.Ramp256B, null);
                default:
                    switch(ch) {
                        case R:
                            return this.getByNumber(Number.Ramp256R, null);
                        case G:
                            return this.getByNumber(Number.Ramp256G, null);
                        case B:
                            return this.getByNumber(Number.Ramp256B, null);
                        default:
                            throw new IllegalStateException("Unsupported Channel: " + ch);
                    }
            }
        }

        private LCDTarget getRamp256From102x(RGBBase.Channel ch, boolean withWhite) {
            Number orgnumber = getNumber();
            if (orgnumber != Number.Ramp1024 && orgnumber != Number.Ramp1021) {
                throw new IllegalStateException("orgnumber != Number.Ramp1024 & Number.Ramp1021");
            }
            if (!ch.isPrimaryColorChannel()) {
                throw new IllegalArgumentException("!ch.isPrimaryColorChannel()");
            }
            Number number = LCDTarget.Number.getRamp256Number(ch, withWhite);
            String desc = " " + number + "From" + orgnumber;
            int start = -1;
            int whiteStart = 255;
            int whiteEnd = withWhite ? whiteStart + 1 : whiteStart;
            switch(orgnumber) {
                case Ramp1024:
                    start = 256 * (ch.getArrayIndex() + 1);
                    return this.getPartLCDTarget(number, desc, start, start + 256, whiteStart, whiteEnd);
                case Ramp1021:
                    start = 256 * (ch.getArrayIndex() + 1) - ch.getArrayIndex() * 1;
                    return this.getPartLCDTarget(number, desc, 0, 1, start, start + 255, whiteStart, whiteEnd);
                default:
                    throw new IllegalStateException("Unsupported LCDTargetBase.Number: " + orgnumber);
            }
        }

        private LCDTarget getRamp260From102x() {
            if (getNumber() != Number.Ramp1024 && getNumber() != Number.Ramp1021) {
                throw new IllegalStateException("getNumber() != Number.Ramp1024 & Number.Ramp1021");
            }
            String desc = getNumber() == Number.Ramp1024 ? " 260From1024" : " 260From1021";
            List<Patch> tmp = new ArrayList<Patch>(260);
            for (RGBBase.Channel ch : RGBBase.Channel.RGBWChannel) {
                for (int x = 0; x < 255; x += 4) {
                    Patch p = getPatch(ch, x, RGB.MaxValue.Double255);
                    tmp.add(p);
                }
                Patch p = getPatch(ch, 255, RGB.MaxValue.Double255);
                tmp.add(p);
            }
            LCDTarget lcdTarget = new LCDTarget(tmp, Number.Ramp260, inverseModeMeasure);
            initLCDTarget(lcdTarget, desc);
            return lcdTarget;
        }

        public LCDTarget getRamp260() {
            return getRamp260From102x();
        }

        private List<Patch> filterByIndex(int... index) {
            if (index.length % 2 != 0) {
                throw new IllegalArgumentException("index.length % 2 != 0");
            }
            int size = index.length;
            int[][] indexArray = new int[size / 2][2];
            for (int x = 0; x < size / 2; x++) {
                indexArray[x][0] = index[x * 2];
                indexArray[x][1] = index[x * 2 + 1];
            }
            return filterByIndex(indexArray);
        }

        private List<Patch> filterByIndex(int[][] indexArray) {
            int size = 0;
            for (int[] indexs : indexArray) {
                size += indexs[1] - indexs[0] + 1;
            }
            List<Patch> tmp = new ArrayList<Patch>(size);
            for (int[] indexs : indexArray) {
                for (int x = indexs[0]; x < indexs[1]; x++) {
                    tmp.add(patchList.get(x));
                }
            }
            return tmp;
        }

        private List<Patch> filterByNumber(Number number) {
            int size = number.getPatchCount();
            List<Patch> tmp = new ArrayList<Patch>(size);
            List<RGB> RGBList = Instance.getRGBList(number);
            double[] rgbValues = new double[3];
            for (RGB rgb : RGBList) {
                rgb.getValues(rgbValues, getMaxValue());
                Patch p = getPatch(rgbValues[0], rgbValues[1], rgbValues[2]);
                tmp.add(p);
            }
            return tmp;
        }

        public LCDTarget getByNumber(Number number, String newdescription) {
            List<Patch> tmp = filterByNumber(number);
            LCDTarget lcdTarget = new LCDTarget(tmp, number, inverseModeMeasure);
            newdescription = (newdescription == null) ? " " + number + "From" + getNumber() : newdescription;
            initLCDTarget(lcdTarget, newdescription);
            return lcdTarget;
        }

        public LCDTarget getPatch79From1024() {
            if (getNumber() != Number.Ramp1024) {
                throw new IllegalStateException("getNumber() != Number.Ramp1024");
            }
            LCDTarget lcdTarget = getByNumber(Number.Patch79, " 79From1024");
            return lcdTarget;
        }

        /**
     * �q125����L�o�X62���
     * (��@�W�D��0�P����W�D���۲V�X & ��)
     * @return LCDPatch
     */
        public LCDTarget getPatch62From125() {
            if (getNumber() != Number.Patch125) {
                throw new IllegalStateException("getNumber() != Number.Patch125");
            }
            List<Patch> tmp = new ArrayList<Patch>(62);
            Patch.Filter.leastOneZero(getPatchList(), tmp, true);
            LCDTarget lcdTarget = new LCDTarget(tmp, Number.Patch62, inverseModeMeasure);
            initLCDTarget(lcdTarget, " 62From125");
            return lcdTarget;
        }

        public LCDTarget getRamp509From1021() {
            if (getNumber() != Number.Ramp1021) {
                throw new IllegalStateException("getNumber() != Number.Ramp1021");
            }
            List<Patch> tmp = new ArrayList<Patch>(509);
            double[] int255Values = new double[3];
            int[] rgbV = new int[3];
            for (Patch p : getPatchList()) {
                RGB rgb = p.getRGB();
                rgb.getValues(int255Values, RGB.MaxValue.Int8Bit);
                rgbV[0] = (int) int255Values[0];
                rgbV[1] = (int) int255Values[1];
                rgbV[2] = (int) int255Values[2];
                if (rgbV[0] != 0 && rgbV[0] != 254 && (rgbV[0] % 2 == 0 || rgbV[0] == 255) || rgbV[1] != 0 && rgbV[1] != 254 && (rgbV[1] % 2 == 0 || rgbV[1] == 255) || rgbV[2] != 0 && rgbV[2] != 254 && (rgbV[2] % 2 == 0 || rgbV[2] == 255) || rgb.isWhite() || rgb.isBlack()) {
                    tmp.add(p);
                }
            }
            LCDTarget lcdTarget = new LCDTarget(tmp, Number.Ramp509, inverseModeMeasure);
            initLCDTarget(lcdTarget, " 509From1021");
            return lcdTarget;
        }

        public LCDTarget getRamp1024From2048() {
            if (getNumber() != Number.Ramp2048) {
                throw new IllegalStateException("getNumber() != Number.Ramp2048");
            }
            return getPartLCDTarget(Number.Ramp1024, " 1024 From 2048", 0, 1024);
        }

        public LCDTarget getRamp2048From4096() {
            if (getNumber() != Number.Ramp4096) {
                throw new IllegalStateException("getNumber() != Number.Ramp4096");
            }
            return getPartLCDTarget(Number.Ramp2048, " 2048 From 4096", 0, 1024, 2048, 3072);
        }

        public LCDTarget getRamp3577From7147() {
            if (getNumber() != Number.Ramp7147) {
                throw new IllegalStateException("getNumber() != Number.Ramp7147");
            }
            List<Patch> tmp = new ArrayList<Patch>(3577);
            double[] rgbValues = new double[3];
            for (Patch p : getPatchList()) {
                RGB rgb = p.getRGB();
                rgb.getValues(rgbValues, RGB.MaxValue.Int10Bit);
                rgbValues = DoubleArray.times(rgbValues, 0.25);
                if ((rgb.isWhite() || rgb.isBlack()) || ((rgbValues[0] % 1 == 0 || rgbValues[0] % 1 == 0.5) && (rgbValues[1] % 1 == 0 || rgbValues[1] % 1 == 0.5) && (rgbValues[2] % 1 == 0 || rgbValues[2] % 1 == 0.5))) {
                    tmp.add(p);
                }
            }
            LCDTarget lcdTarget = new LCDTarget(tmp, Number.Ramp3577, inverseModeMeasure);
            initLCDTarget(lcdTarget, " 3577From7147");
            return lcdTarget;
        }

        /**
     * �q729����L�o�X218���
     * (��@�W�D��0�P����W�D���۲V�X & ��)
     * @return LCDPatch
     */
        public LCDTarget getPatch218From729() {
            if (getNumber() != Number.Test729) {
                throw new IllegalStateException("getNumber() != Number.Test729");
            }
            List<Patch> tmp = new ArrayList<Patch>(218);
            Patch.Filter.leastOneZero(getPatchList(), tmp, true);
            LCDTarget lcdTarget = new LCDTarget(tmp, Number.Patch218, isRGBPatchOnly, inverseModeMeasure);
            initLCDTarget(lcdTarget, " 218From729");
            return lcdTarget;
        }

        public LCDTarget getRamp1021() {
            Number number = getNumber();
            if (number.isComplex()) {
                return getRamp();
            } else {
                switch(getNumber()) {
                    case Ramp1024:
                        return getRamp1021From1024();
                    case Ramp1021:
                        return parent;
                    default:
                        throw new IllegalStateException("Unsupported LCDTargetBase.Number: " + getNumber());
                }
            }
        }

        private final LCDTarget getTargetFromComplex(Number targetNumber) {
            Number number = getNumber();
            int[] indexs = Number.getIndexInComplex(number, targetNumber);
            return getPartLCDTarget(targetNumber, " " + targetNumber.getPatchCount() + "From" + number.getPatchCount(), indexs[0], indexs[1]);
        }

        public LCDTarget getRamp() {
            Number number = getNumber();
            if (!number.isComplex()) {
                throw new IllegalArgumentException("!number.isComplex()");
            }
            Number rampNumber = Number.getRampNumberFromComplex(number);
            return getTargetFromComplex(rampNumber);
        }

        public LCDTarget getSurface1352From2223() {
            if (getNumber() != Number.Complex729_46_96_1352) {
                throw new IllegalStateException("getNumber() != Number.Complex729_46_96_1352");
            }
            return getPartLCDTarget(Number.Surface1352, "Surface 1352 from 2223", 871, 2223);
        }

        public LCDTarget getTest() {
            Number number = getNumber();
            if (!number.isComplex()) {
                throw new IllegalArgumentException("!number.isComplex()");
            }
            Number testNumber = Number.getTestNumberFromComplex(number);
            return getTargetFromComplex(testNumber);
        }

        public LCDTarget getXtalk() {
            Number number = getNumber();
            if (!number.isComplex()) {
                throw new IllegalArgumentException("!number.isComplex()");
            }
            Number xtalkNumber = Number.getXtalkNumberFromComplex(number);
            return getTargetFromComplex(xtalkNumber);
        }

        private LCDTarget getRamp1021From1024() {
            if (getNumber() != Number.Ramp1024) {
                throw new IllegalStateException("getNumber() != Number.Ramp1024");
            }
            return getPartLCDTarget(Number.Ramp1021, " 1021 From 1024", 0, 256, 257, 512, 514, 768, 770, 1024);
        }

        /**
     * ��o������patch�����s��LCDTarget
     * @param partNumber Number
     * @param newdescription String
     * @param index int[]
     * @return LCDTarget
     */
        private LCDTarget getPartLCDTarget(Number partNumber, String newdescription, int... index) {
            List<Patch> filterPatchList = filterByIndex(index);
            LCDTarget lcdTarget = new LCDTarget(filterPatchList, partNumber, inverseModeMeasure);
            initLCDTarget(lcdTarget, newdescription);
            return lcdTarget;
        }

        private void initLCDTarget(LCDTarget lcdTarget, String newdescription) {
            lcdTarget.luminance = luminance;
            lcdTarget.setDescription(description + newdescription);
            lcdTarget.device = device;
        }

        public LCDTarget getRamp1792From7147() {
            if (getNumber() != Number.Ramp7147) {
                throw new IllegalStateException("getNumber() != Number.Ramp7147");
            }
            List<Patch> tmp = new ArrayList<Patch>(1792);
            double[] rgbValues = new double[3];
            for (Patch p : getPatchList()) {
                RGB rgb = p.getRGB();
                rgb.getValues(rgbValues, RGB.MaxValue.Int10Bit);
                rgbValues = DoubleArray.times(rgbValues, 0.25);
                if ((rgb.isWhite() || rgb.isBlack()) || (rgbValues[0] % 1 == 0 && rgbValues[1] % 1 == 0 && rgbValues[2] % 1 == 0)) {
                    tmp.add(p);
                }
            }
            LCDTarget lcdTarget = new LCDTarget(tmp, Number.Ramp1792, inverseModeMeasure);
            initLCDTarget(lcdTarget, " 1792From7147");
            return lcdTarget;
        }

        public LCDTarget getRamp7147From7168() {
            if (getNumber() != Number.Ramp7168) {
                throw new IllegalStateException("getNumber() != Number.Ramp7168");
            }
            List<Integer> removeIndex = new ArrayList<Integer>();
            int size = size();
            for (int x = 1792 + 255; x < size; x += 256) {
                removeIndex.add(x);
            }
            List<Patch> tmp = new ArrayList<Patch>(getPatchList());
            for (int x = removeIndex.size() - 1; x >= 0; x--) {
                int index = removeIndex.get(x);
                tmp.remove(index);
            }
            LCDTarget lcdTarget = new LCDTarget(tmp, Number.Ramp7147, inverseModeMeasure);
            initLCDTarget(lcdTarget, " 7147From7168");
            return lcdTarget;
        }
    }

    public final class Filter {

        /**
     * �L�o�XRGB��gray�����(R=G=B)
     * @return List
     */
        public final List<Patch> grayPatch() {
            return grayPatch(false);
        }

        public final List<Patch> grayPatch(boolean withBlack) {
            List<Patch> tmp = new ArrayList<Patch>();
            if (withBlack) {
                tmp.add(getBlackPatch());
            }
            Patch.Filter.grayPatch(getPatchList(), tmp);
            return tmp;
        }

        public final List<Patch> getRange(int fromIndex, int toIndex) {
            List<Patch> range = new ArrayList<Patch>(toIndex - fromIndex + 1);
            for (int x = fromIndex; x < toIndex; x++) {
                Patch p = patchList.get(x);
                range.add(p);
            }
            return range;
        }

        /**
     * �L�o�XRGB��gray�����(R=G=B), �B�T�O�G�׬O���W��
     * @return List
     */
        public final List<Patch> grayScalePatch() {
            Set<Patch> tmp = new TreeSet<Patch>();
            Patch.Filter.grayPatch(getPatchList(), tmp);
            List<Patch> listTmp = new ArrayList<Patch>(tmp);
            return listTmp;
        }

        public final Set<Patch> grayScalePatchSet(RGBBase.Channel ch, boolean withBlack) {
            Set<Patch> singleChannelPatch = new TreeSet<Patch>();
            if (withBlack) {
                Patch darkestPatch = getBlackPatch();
                singleChannelPatch.add(darkestPatch);
            }
            List<Patch> oneValueChannel = null;
            if (ch == RGBBase.Channel.W) {
                oneValueChannel = filter.grayPatch();
            } else {
                oneValueChannel = filter.oneValueChannel(ch);
            }
            singleChannelPatch.addAll(oneValueChannel);
            return singleChannelPatch;
        }

        public final Set<Patch> grayScalePatchSet(RGBBase.Channel ch) {
            return grayScalePatchSet(ch, true);
        }

        public final List<Patch> grayScalePatch(RGBBase.Channel ch) {
            return grayScalePatch(ch, true);
        }

        public final List<Patch> grayScalePatch(RGBBase.Channel ch, boolean withBlack) {
            Set<Patch> singleChannelPatch = grayScalePatchSet(ch, withBlack);
            List<Patch> list = new ArrayList<Patch>(singleChannelPatch);
            return list;
        }

        public final List<RGB> rgbList() {
            return Patch.Filter.rgbList(getPatchList());
        }

        public final List<CIEXYZ> XYZList() {
            return Patch.Filter.XYZList(getPatchList());
        }

        public final double[] YArray() {
            List<Patch> patchList = getPatchList();
            int size = patchList.size();
            double[] YArray = new double[size];
            for (int x = 0; x < size; x++) {
                YArray[x] = patchList.get(x).getXYZ().Y;
            }
            return YArray;
        }

        public final CIEXYZ[] XYZArray() {
            List<Patch> patchList = getPatchList();
            int size = patchList.size();
            CIEXYZ[] XYZArray = new CIEXYZ[size];
            for (int x = 0; x < size; x++) {
                XYZArray[x] = patchList.get(x).getXYZ();
            }
            return XYZArray;
        }

        public final CIExyY[] xyYArray() {
            return CIExyY.toxyYArray(XYZArray());
        }

        public final List<String> nameList() {
            return Utils.filterNameList(getPatchList());
        }

        /**
     * �L�o�X �ܤ֤@���W�D��0��
     * @return List
     */
        public final List<Patch> leastOneZeroChannel() {
            List<Patch> tmp = new LinkedList<Patch>();
            Patch.Filter.leastOneZero(getPatchList(), tmp, false);
            return tmp;
        }

        /**
     * �L�o�X �ܤ�Channel�W�D��0��
     * @param zeroChannel Channel
     * @return List
     */
        public final List<Patch> leastOneZeroChannel(RGBBase.Channel zeroChannel) {
            return Patch.Filter.patch(getPatchList(), zeroChannel, 0);
        }

        /**
     * �N��@�W�D���Ȫ��L�o�X��
     * @param channel Channel
     * @return List
     */
        public final List<Patch> oneValueChannel(RGBBase.Channel channel) {
            if (channel == RGBBase.Channel.W) {
                return this.grayPatch();
            }
            Set<Patch> tmp = new TreeSet<Patch>();
            Patch.Filter.oneValueChannel(getPatchList(), tmp, channel);
            List<Patch> listTmp = new ArrayList<Patch>(tmp);
            return listTmp;
        }
    }

    public transient Filter filter = new Filter();

    public transient TargetFilter targetFilter = new TargetFilter(this);

    /**
   * �নLab��Patch List
   * @param whitePatch Patch
   * @return List
   */
    public List<Patch> getLabPatchList(Patch whitePatch) {
        CIEXYZ whitePoint = whitePatch.getXYZ();
        return getLabPatchList(whitePoint);
    }

    /**
   * �নLab��Patch List
   * @param whitePoint CIEXYZ
   * @return List
   */
    public List<Patch> getLabPatchList(CIEXYZ whitePoint) {
        List<Patch> labPatchList = Patch.Produce.LabPatches(patchList, whitePoint);
        return labPatchList;
    }

    /**
   * �নLab��Patch List
   * @return List
   */
    public List<Patch> getLabPatchList() {
        return getLabPatchList(getWhitePatch());
    }

    /**
   * ����Patch�����A�O�_���T
   * (LCDPatch�u�౵��Patch.TYPE.XYZRGB)
   * (ps:�����ҲĤ@�Ӧ���Ӥw)
   * @return boolean
   */
    private boolean checkPatchTypeRight() {
        Patch p = patchList.get(0);
        return p.getXYZ() != null && p.getRGB() != null;
    }

    private int blackPatchIndex = -1;

    private int whitePatchIndex = -1;

    private int darkestPatchIndex = -1;

    private int brightestPatchIndex = -1;

    boolean inverseModeMeasure = false;

    /**
   * �q��ɬO�_�H����Ҧ���q?
   * @return boolean
   */
    public final boolean isInverseModeMeasure() {
        return inverseModeMeasure;
    }

    public double[][] getCalibrationMatrix() {
        return calibrationMatrix;
    }

    private double[][] calibrationMatrix;

    /**
   * ��o�¦��(�HRGB�����)
   * @return Patch
   */
    public Patch getBlackPatch() {
        if (blackPatchIndex == -1) {
            for (int x = 0; x < patchList.size(); x++) {
                Patch p = patchList.get(x);
                if (p.getRGB().isBlack()) {
                    blackPatchIndex = x;
                    break;
                }
            }
        }
        return patchList.get(blackPatchIndex);
    }

    /**
   * ��oY�̤p��patch
   * @return Patch
   */
    public Patch getDarkestPatch() {
        if (darkestPatchIndex == -1) {
            double darkestY = Double.MAX_VALUE;
            for (int x = 0; x < patchList.size(); x++) {
                Patch p = patchList.get(x);
                double Y = p.getXYZ().Y;
                if (Y < darkestY) {
                    darkestY = Y;
                    darkestPatchIndex = x;
                }
            }
        }
        return patchList.get(darkestPatchIndex);
    }

    /**
   * ��o�զ��(�HXYZ��Y�����)
   * @return Patch
   */
    public Patch getBrightestPatch() {
        if (brightestPatchIndex == -1) {
            double brightestY = Double.MIN_VALUE;
            for (int x = 0; x < patchList.size(); x++) {
                Patch p = patchList.get(x);
                double Y = p.getXYZ().Y;
                if (Y > brightestY) {
                    brightestY = Y;
                    brightestPatchIndex = x;
                }
            }
        }
        return patchList.get(brightestPatchIndex);
    }

    /**
   * ��o�զ��(�HRGB�����)
   * @return Patch
   */
    public Patch getWhitePatch() {
        if (whitePatchIndex == -1) {
            for (int x = 0; x < patchList.size(); x++) {
                Patch p = patchList.get(x);
                RGB rgb = p.getRGB();
                if (rgb.isWhite()) {
                    whitePatchIndex = x;
                    break;
                }
            }
            if (whitePatchIndex == -1) {
                return null;
            }
        }
        return patchList.get(whitePatchIndex);
    }

    /**
   * ���G�פ����I
   * @return double
   */
    public final double getInverseLuminanceCode() {
        if (this.getNumber().isRamp()) {
            throw new IllegalStateException("!this.getNumber().ramp");
        } else {
            for (int x = 1; x < 256; x++) {
                Patch pre = this.getPatch(RGBBase.Channel.W, x - 1);
                Patch now = this.getPatch(RGBBase.Channel.W, x);
                if (now.getXYZ().Y < pre.getXYZ().Y) {
                    return x;
                }
            }
            return -1;
        }
    }

    public void setCalibrationMatrix(double[][] calibrationMatrix) {
        this.calibrationMatrix = calibrationMatrix;
    }

    public boolean hasRGBWPatch() {
        Patch patchW = getWhitePatch();
        Patch patchR = getSaturatedChannelPatch(RGBBase.Channel.R);
        Patch patchG = getSaturatedChannelPatch(RGBBase.Channel.G);
        Patch patchB = getSaturatedChannelPatch(RGBBase.Channel.B);
        if (patchW == null || patchR == null || patchG == null || patchB == null) {
            return false;
        }
        return true;
    }

    private RGBBase.Channel targetChannel;

    public void setTargetChannel(RGBBase.Channel ch) {
        this.targetChannel = ch;
    }

    public RGBBase.Channel getTargetChannel() {
        if (targetChannel != null) {
            return targetChannel;
        } else {
            Number number = this.getNumber();
            RGBBase.Channel[] channels = number.getChannels();
            if (channels != null && channels.length == 1) {
                return channels[0];
            } else {
                return null;
            }
        }
    }

    public Hook hook = new Hook();

    public final class Hook {

        private List<Patch> patchList;

        private List<CIEXYZ> XYZList;

        private void init() {
            if (patchList == null) {
                patchList = filter.grayScalePatch(RGB.Channel.B);
                XYZList = Patch.Filter.XYZList(patchList);
            }
        }

        public final void fixBlueHook() {
            init();
            int hookedIndex = getBlueHookedIndex();
            int size = patchList.size();
            Patch hookedPatch = patchList.get(hookedIndex);
            CIExyY hookedxyY = new CIExyY(hookedPatch.getXYZ());
            for (int x = size - 1; x > hookedIndex; x--) {
                Patch p = patchList.get(x);
                CIEXYZ XYZ = p.getXYZ();
                CIExyY xyY = new CIExyY(XYZ);
                xyY.x = hookedxyY.x;
                xyY.y = hookedxyY.y;
                XYZ = xyY.toXYZ();
                Patch.Operator.setXYZ(p, XYZ);
                CIEXYZ normalizeXYZ = p.getNormalizedXYZ();
                xyY.Y = normalizeXYZ.Y;
                XYZ = xyY.toXYZ();
                Patch.Operator.setNormalizedXYZ(p, XYZ);
            }
        }

        public final int getBlueHookedIndex() {
            init();
            int size = patchList.size();
            double minyValue = Double.MAX_VALUE;
            int minyIndex = -1;
            for (int x = 0; x < size; x++) {
                CIExyY xyY = new CIExyY(XYZList.get(x));
                if (xyY.y < minyValue) {
                    minyValue = xyY.y;
                    minyIndex = x;
                }
            }
            return minyIndex;
        }
    }
}
