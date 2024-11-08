package auo.cms.test;

import shu.cms.lcd.*;
import shu.cms.colorspace.depend.RGBBase;
import shu.cms.colorspace.independ.*;
import shu.cms.Patch;
import java.util.List;
import shu.cms.plot.Plot2D;
import java.awt.*;
import shu.cms.Illuminant;

/**
 * <p>Title: Colour ManagLCDTarget.Instanceement System</p>
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
public class HookAnalyzer {

    public static void main(String[] args) {
        colorimetric(args);
    }

    public static void colorimetric(String[] args) {
        double d65xyValues[] = Illuminant.D65WhitePoint.getxyValues();
        LCDTarget lcdTarget1 = LCDTarget.Instance.getFromAUORampXLS("0329_panel01.xls", LCDTargetBase.Number.Ramp260);
        LCDTarget lcdTarget2 = LCDTarget.Instance.getFromAUORampXLS("0329_panel02.xls", LCDTargetBase.Number.Ramp260);
        LCDTarget lcdTarget3 = LCDTarget.Instance.getFromAUORampXLS("0329_panel03.xls", LCDTargetBase.Number.Ramp260);
        LCDTarget targets[] = new LCDTarget[] { lcdTarget1, lcdTarget2, lcdTarget3 };
        Plot2D plot = Plot2D.getInstance();
        int index = 1;
        for (LCDTarget target : targets) {
            Color c = RGBBase.Channel.getChannel(index++).color;
            for (RGBBase.Channel ch : new RGBBase.Channel[] { RGBBase.Channel.B, RGBBase.Channel.W }) {
                List<Patch> scale = target.filter.grayScalePatch(ch);
                for (Patch p : scale) {
                    CIExyY xyY = new CIExyY(p.getXYZ());
                    plot.addCacheScatterLinePlot(target.getFilename() + "_" + ch.name(), c, xyY.x, xyY.y);
                }
            }
        }
        plot.addScatterPlot("D65", d65xyValues[0], d65xyValues[1]);
        plot.setAxeLabel(0, "x");
        plot.setAxeLabel(0, "y");
        plot.setVisible();
    }

    public static void saturation(String[] args) {
        LCDTarget lcdTarget = LCDTarget.Instance.getFromAUORampXLS("0329_panel01.xls", LCDTargetBase.Number.Ramp260);
        CIEXYZ whiteXYZ = lcdTarget.getWhitePatch().getXYZ();
        CIExyY whitexyY = new CIExyY(whiteXYZ);
        RGBBase.Channel ch = RGBBase.Channel.B;
        List<Patch> scale = lcdTarget.filter.grayScalePatch(ch);
        Patch saturationPatch = (ch == RGBBase.Channel.W) ? lcdTarget.getBlackPatch() : lcdTarget.getSaturatedChannelPatch(ch);
        CIExyY saturationxyY = new CIExyY(saturationPatch.getXYZ());
        double saturation = saturationxyY.getSaturation(whitexyY);
        Plot2D plot = Plot2D.getInstance();
        for (Patch p : scale) {
            CIEXYZ XYZ = p.getXYZ();
            CIExyY xyY = new CIExyY(XYZ);
            double s = xyY.getSaturation(whitexyY);
            double ratio = s / saturation;
            double v = p.getRGB().getValue(ch);
            plot.addCacheScatterPlot(Double.toString(v), Color.black, xyY.x, xyY.y);
            System.out.println(p.getRGB().getValue(ch) + " " + (ratio * 100) + " " + s);
        }
        for (RGBBase.Channel c : RGBBase.Channel.RGBChannel) {
            for (Patch p : lcdTarget.filter.grayScalePatch(c)) {
                CIExyY xyY = new CIExyY(p.getXYZ());
                double v = p.getRGB().getValue(c);
                plot.addCacheScatterPlot(c.name() + " " + v, c.color, xyY.x, xyY.y);
            }
        }
        plot.setVisible();
    }
}
