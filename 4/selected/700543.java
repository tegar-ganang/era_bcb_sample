package shu.cms.colorformat.adapter;

import java.util.*;
import shu.cms.colorformat.adapter.TargetAdapter.*;
import shu.cms.colorspace.depend.*;
import shu.cms.lcd.*;
import shu.cms.lcd.LCDTargetBase.Number;

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
public class LCDTargetNumberAdapter extends TargetAdapter {

    private LCDTargetBase.Number number;

    public LCDTargetNumberAdapter(LCDTargetBase.Number number) {
        this.number = number;
    }

    /**
   * estimateLCDTargetNumber
   *
   * @return Number
   */
    public Number estimateLCDTargetNumber() {
        return number;
    }

    /**
   * getAbsolutePath
   *
   * @return String
   */
    public String getAbsolutePath() {
        return null;
    }

    /**
   * getFileDescription
   *
   * @return String
   */
    public String getFileDescription() {
        return number.getDescription();
    }

    /**
   * getFileNameExtension
   *
   * @return String
   */
    public String getFileNameExtension() {
        return null;
    }

    /**
   * getFilename
   *
   * @return String
   */
    public String getFilename() {
        throw new UnsupportedOperationException();
    }

    /**
   * getPatchNameList
   *
   * @return List
   */
    public List<String> getPatchNameList() {
        int size = number.getPatchCount();
        List<String> nameList = new ArrayList<String>(size);
        for (int x = 0; x < size; x++) {
            nameList.add("A" + Integer.toString(x + 1));
        }
        return nameList;
    }

    /**
   * getRGBList
   *
   * @return List
   */
    public List<RGB> getRGBList() {
        if (rgbList == null) {
            rgbList = getRGBList(number);
        }
        return rgbList;
    }

    private List<RGB> rgbList;

    private static final List<RGB> getRGBList(Number number) {
        switch(number.getTargetType()) {
            case Ramp:
                return getRampRGBList(number);
            case XTalk:
                return getXTalkRGBList(number);
            case Test:
                return getTestRGBList(number);
            case Complex:
                return getComplexRGBList(number);
            case Unknow:
                return getUnknowRGBList(number);
            default:
                return null;
        }
    }

    private static final List<RGB> getComplexRGBList(Number number) {
        if (!number.isComplex()) {
            throw new IllegalArgumentException("!number.isComplex()");
        }
        Number[] numbers = number.getNumbers();
        List<RGB> rgbList0 = getRGBList(numbers[0]);
        int size = numbers.length;
        for (int x = 1; x < size; x++) {
            List<RGB> rgbList = getRGBList(numbers[x]);
            rgbList0.addAll(rgbList);
        }
        return rgbList0;
    }

    private RGB.ColorSpace cameraRGBColorSpace;

    private static final List<RGB> getUnknowRGBList(Number number) {
        switch(number) {
            case Surface1352:
                return LCDTargetBase.SurfaceTarget.getSurface((int) number.getStep());
            case Patch218:
                return LCDTarget.Instance.get(LCDTarget.Number.Test729).targetFilter.getPatch218From729().filter.rgbList();
            default:
                return null;
        }
    }

    private static final List<RGB> getRampRGBList(Number number) {
        if (!number.isRamp()) {
            throw new IllegalArgumentException("!number.isRamp()");
        }
        switch(number) {
            case Ramp256_6Bit:
                return RampTarget.getRamp256();
            case Ramp257_6Bit:
                return RampTarget.getRamp257();
            case Ramp260:
                return RampTarget.getRamp260();
            case Ramp1021:
                return RampTarget.getRamp1021();
            case Ramp1024:
                return RampTarget.getRamp1024();
            case Ramp1792:
                return RampTarget.getRamp1792();
            case Ramp256W:
                return RampTarget.getRamp(RGBBase.Channel.W, false);
            case Ramp256R:
                return RampTarget.getRamp(RGBBase.Channel.R, false);
            case Ramp256G:
                return RampTarget.getRamp(RGBBase.Channel.G, false);
            case Ramp256B:
                return RampTarget.getRamp(RGBBase.Channel.B, false);
            case Ramp256R_W:
                return RampTarget.getRamp(RGBBase.Channel.R, true);
            case Ramp256G_W:
                return RampTarget.getRamp(RGBBase.Channel.G, true);
            case Ramp256B_W:
                return RampTarget.getRamp(RGBBase.Channel.B, true);
            case Ramp256RGB_W:
                return RampTarget.getRamp256RGB_W();
            case Ramp897:
                return RampTarget.getRamp897();
            default:
                return null;
        }
    }

    private static final class TestTarget {

        private static final List<RGB> getAUOTest(int step) {
            int level = (int) Math.round((256.) / step) + 1;
            int size = (int) Math.pow(level, 3);
            List<RGB> rgbList = new ArrayList<RGB>(size);
            for (int bstart = 0; bstart <= 32; bstart += 32) {
                for (int gstart = 0; gstart <= 32; gstart += 32) {
                    for (int rstart = 0; rstart <= 32; rstart += 32) {
                        for (int b = bstart; b <= 256; b += 64) {
                            for (int g = gstart; g <= 256; g += 64) {
                                for (int r = rstart; r <= 256; r += 64) {
                                    RGB rgb = new RGB(r, g, b);
                                    rgb.rationalize();
                                    rgbList.add(rgb);
                                }
                            }
                        }
                    }
                }
            }
            return rgbList;
        }

        private static final List<RGB> getTest(int start, int step, boolean autoTail) {
            int level = (int) Math.round((256. - start) / step) + 1;
            int size = (int) Math.pow(level, 3);
            List<RGB> rgbList = new ArrayList<RGB>(size);
            for (int x = 0; x < level; x++) {
                for (int y = 0; y < level; y++) {
                    for (int z = 0; z < level; z++) {
                        double r = x * step + start;
                        double g = y * step + start;
                        double b = z * step + start;
                        if (autoTail) {
                            r = (x == level - 1) ? 255 : r;
                            g = (y == level - 1) ? 255 : g;
                            b = (z == level - 1) ? 255 : b;
                        }
                        RGB rgb = new RGB(r, g, b);
                        rgb.rationalize();
                        rgbList.add(rgb);
                    }
                }
            }
            return rgbList;
        }

        private static final List<RGB> getTest(int step) {
            return getTest(0, step, false);
        }
    }

    private static final class XTalkTarget {

        private static final List<RGB> getXTalk0(int start, int step, RGBBase.Channel[][] channelsSet) {
            int level = (int) Math.rint((256. - start) / step);
            level = (start + step * (level - 1) < 255) ? level + 1 : level;
            int size = level * level * 3 + 1;
            List<RGB> rgbList = new ArrayList<RGB>(size);
            for (RGBBase.Channel[] chs : channelsSet) {
                RGBBase.Channel ch0 = chs[0];
                RGBBase.Channel ch1 = chs[1];
                for (int x = 0; x < level; x++) {
                    for (int y = 0; y < level; y++) {
                        int c0 = x * step + start;
                        int c1 = y * step + start;
                        RGB rgb = new RGB(RGB.ColorSpace.unknowRGB, RGB.MaxValue.Double255);
                        rgb.setValue(ch0, c0);
                        rgb.setValue(ch1, c1);
                        rgb.clip();
                        rgbList.add(rgb);
                    }
                }
            }
            RGB white = (RGB) White.clone();
            rgbList.add(white);
            return rgbList;
        }

        private static final RGBBase.Channel[][] getXTalkChannelsSet() {
            RGBBase.Channel[][] channelsSet = new RGBBase.Channel[3][];
            for (int x = 0; x < 3; x++) {
                RGBBase.Channel ch0 = RGBBase.Channel.getChannelByArrayIndex(x);
                RGBBase.Channel ch1 = RGBBase.Channel.getChannelByArrayIndex((x + 1) % 3);
                if (ch0.getArrayIndex() < ch1.getArrayIndex()) {
                    channelsSet[x] = new RGBBase.Channel[] { ch0, ch1 };
                } else {
                    channelsSet[x] = new RGBBase.Channel[] { ch1, ch0 };
                }
            }
            return channelsSet;
        }

        private static final List<RGB> getXTalk(int start, int step) {
            RGBBase.Channel[][] channelsSet = getXTalkChannelsSet();
            return getXTalk0(start, step, channelsSet);
        }

        private static final List<RGB> getXTalk(int step) {
            RGBBase.Channel[][] channelsSet = getXTalkChannelsSet();
            return getXTalk0(step, step, channelsSet);
        }
    }

    private static final class RampTarget {

        private static final List<RGB> getRamp(RGBBase.Channel ch, boolean withWhite) {
            List<RGB> rgbList = getRamp(new RGBBase.Channel[] { ch }, withWhite, 1, RGB.MaxValue.Int8Bit);
            if (withWhite) {
                RGB white = (RGB) White.clone();
                rgbList.add(white);
            }
            return rgbList;
        }

        private static final List<RGB> getRamp(RGBBase.Channel[] channels, boolean sizePlusOne, int step, RGB.MaxValue maxValue) {
            int size = (256 / step) * channels.length;
            if (sizePlusOne) {
                size++;
            }
            List<RGB> rgbList = new ArrayList<RGB>(size);
            boolean int6bit = RGB.MaxValue.Int6Bit == maxValue;
            for (RGBBase.Channel ch : channels) {
                for (int x = 0; x < 256; x += step) {
                    RGB rgb = new RGB(RGB.ColorSpace.unknowRGB, maxValue);
                    int value = int6bit ? x / 4 : x;
                    rgb.setValue(ch, value);
                    rgbList.add(rgb);
                }
            }
            return rgbList;
        }

        private static final List<RGB> getRamp1024() {
            return getRamp(RGBBase.Channel.WRGBChannel, false, 1, RGB.MaxValue.Int8Bit);
        }

        private static final List<RGB> getRamp897() {
            List<RGB> rgbList = new ArrayList<RGB>(897);
            RGB black = (RGB) RGB.Black.clone();
            rgbList.add(black);
            for (RGBBase.Channel ch : RGBBase.Channel.RGBYMCWChannel) {
                for (int x = 2; x <= 256; x += 2) {
                    RGB rgb = new RGB(RGB.ColorSpace.unknowRGB, RGB.MaxValue.Double255);
                    if (x == 256) {
                        rgb.setValue(ch, 255);
                    } else {
                        rgb.setValue(ch, x);
                    }
                    rgbList.add(rgb);
                }
            }
            return rgbList;
        }

        private static final List<RGB> getRamp1021() {
            List<RGB> rgbList = new ArrayList<RGB>(1021);
            for (double x = 0; x < 256; x++) {
                RGB rgb = new RGB(x, x, x);
                rgbList.add(rgb);
            }
            for (RGBBase.Channel ch : RGBBase.Channel.RGBChannel) {
                for (int x = 1; x < 256; x++) {
                    RGB rgb = new RGB(RGB.ColorSpace.unknowRGB, RGB.MaxValue.Double255);
                    rgb.setValue(ch, x);
                    rgbList.add(rgb);
                }
            }
            return rgbList;
        }

        private static final List<RGB> getRamp256RGB_W() {
            List<RGB> rgbList = new ArrayList<RGB>(767);
            RGB black = (RGB) Black.clone();
            rgbList.add(black);
            for (RGBBase.Channel ch : RGBBase.Channel.RGBChannel) {
                for (int x = 1; x < 256; x++) {
                    RGB rgb = new RGB(RGB.ColorSpace.unknowRGB, RGB.MaxValue.Double255);
                    rgb.setValue(ch, x);
                    rgbList.add(rgb);
                }
            }
            RGB white = (RGB) White.clone();
            rgbList.add(white);
            return rgbList;
        }

        private static final List<RGB> getRamp1792() {
            return getRamp(RGBBase.Channel.RGBYMCWChannel, false, 1, RGB.MaxValue.Int8Bit);
        }

        private static final List<RGB> getRamp257() {
            List<RGB> rgbList = getRamp(RGBBase.Channel.RGBWChannel, true, 4, RGB.MaxValue.Int6Bit);
            RGB white = (RGB) White.clone();
            rgbList.add(white);
            return rgbList;
        }

        private static final List<RGB> getRamp256() {
            List<RGB> rgbList = getRamp(RGBBase.Channel.WRGBChannel, false, 4, RGB.MaxValue.Int6Bit);
            return rgbList;
        }

        private static final List<RGB> getRamp260() {
            List<RGB> rgbList = new ArrayList<RGB>(260);
            RGB.Channel channels[] = new RGB.Channel[] { RGB.Channel.W, RGB.Channel.R, RGB.Channel.G, RGB.Channel.B };
            for (RGBBase.Channel ch : channels) {
                for (int x = 0; x <= 252; x += 4) {
                    RGB rgb = new RGB(RGB.ColorSpace.unknowRGB, RGB.MaxValue.Double255);
                    rgb.setValue(ch, x);
                    rgbList.add(rgb);
                }
                RGB max = new RGB(RGB.ColorSpace.unknowRGB, RGB.MaxValue.Double255);
                max.setValue(ch, 255);
                rgbList.add(max);
            }
            return rgbList;
        }
    }

    private static final List<RGB> getTestRGBList(Number number) {
        if (!number.isTest()) {
            throw new IllegalArgumentException("!number.isTest()");
        }
        switch(number) {
            case Test9261:
            case Test4096:
            case Test4913:
            case Test1728:
                return TestTarget.getTest((int) number.getStep());
            case Test512:
                return TestTarget.getTest((int) number.getStep());
            case Test4913_6bit:
                return TestTarget.getTest(8, (int) number.getStep(), false);
            case Test729:
                return TestTarget.getTest(-1, (int) number.getStep(), false);
            case AUO729:
                return TestTarget.getAUOTest((int) number.getStep());
            default:
                return null;
        }
    }

    private static final List<RGB> getXTalkRGBList(Number number) {
        if (!number.isXTalk()) {
            throw new IllegalArgumentException("!number.isXTalk()");
        }
        switch(number) {
            case Xtalk769:
            case Xtalk12289:
            case Xtalk7804:
            case Xtalk5548:
            case Xtalk4108:
                return XTalkTarget.getXTalk((int) number.getStep());
            case Xtalk3073_6Bit:
                return XTalkTarget.getXTalk(4, (int) number.getStep());
            case Xtalk589_6Bit:
                return XTalkTarget.getXTalk(4, (int) number.getStep());
            case Xtalk4333:
                return XTalkTarget.getXTalk(0, (int) number.getStep());
            default:
                return null;
        }
    }

    /**
   * getReflectSpectraList
   *
   * @return List
   */
    public List getReflectSpectraList() {
        throw new UnsupportedOperationException();
    }

    /**
   * getSpectraList
   *
   * @return List
   */
    public List getSpectraList() {
        throw new UnsupportedOperationException();
    }

    /**
   * getStyle
   *
   * @return Style
   */
    public Style getStyle() {
        return Style.RGB;
    }

    /**
   * getXYZList
   *
   * @return List
   */
    public List getXYZList() {
        throw new UnsupportedOperationException();
    }

    /**
   * isInverseModeMeasure
   *
   * @return boolean
   */
    public boolean isInverseModeMeasure() {
        return false;
    }

    /**
   * probeParsable
   *
   * @return boolean
   */
    public boolean probeParsable() {
        return false;
    }

    private static final RGB White = new RGB(255., 255., 255.);

    private static final RGB Black = new RGB(0., 0., 0.);

    public static void main(String[] args) {
        LCDTargetNumberAdapter adapter = new LCDTargetNumberAdapter(LCDTargetBase.Number.Test729);
        List<RGB> rgbList = adapter.getRGBList();
        int index = 1;
        for (RGB rgb : rgbList) {
            System.out.println(index++ + " " + rgb);
        }
    }
}
