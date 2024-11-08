package shu.cms.devicemodel.lcd.xtalk;

import java.awt.*;
import shu.cms.*;
import shu.cms.colorspace.depend.*;
import shu.cms.devicemodel.lcd.*;
import shu.cms.lcd.*;
import shu.cms.plot.*;
import shu.math.lut.*;

/**
 * <p>Title: Colour Management System</p>
 *
 * <p>Description: a Colour Management System by Java</p>
 * ����3+1��Ӫ?�覡�ӸѨMcrosstalk
 *
 * <p>Copyright: Copyright (c) 2008</p>
 *
 * <p>Company: skygroup</p>
 *
 * @author skyforce
 * @version 1.0
 */
public class FourLutAdjacentXtalkModel extends AdjacentPixelXtalkModel {

    /**
   *
   * @param lcdTarget LCDTarget
   * @param xtalkLCDTarget LCDTarget
   * @param channelVI Channel
   * @param xtalkProperty XTalkProperty
   * @param chVIIProperty XTalkProperty
   */
    public FourLutAdjacentXtalkModel(LCDTarget lcdTarget, LCDTarget xtalkLCDTarget, RGBBase.Channel channelVI, XTalkProperty xtalkProperty, XTalkProperty chVIIProperty) {
        super(lcdTarget, xtalkLCDTarget, xtalkProperty);
        this.channelVI = channelVI;
        this.chVIIProperty = chVIIProperty;
    }

    public FourLutAdjacentXtalkModel(LCDTarget lcdTarget, LCDTarget xtalkLCDTarget, RGBBase.Channel channelVI) {
        super(lcdTarget, xtalkLCDTarget);
        this.channelVI = channelVI;
    }

    private XTalkProperty chVIIProperty = XTalkProperty.getOppositeProperty(this.property);

    private RGBBase.Channel channelVI;

    private XTalkEliminator chVIEliminator;

    private FourthLutXTalkReconstructor fourthReconstructor;

    private boolean channelVICorrect = true;

    public void setChannelVICorrect(boolean correct) {
        this.channelVICorrect = correct;
    }

    protected Factor[] _produceFactor() {
        mmModel = new MultiMatrixModel(lcdTarget);
        mmModel.produceFactor();
        mmModel.correct.setCorrectorAlgo(Interpolation1DLUT.Algo.QUADRATIC_POLYNOMIAL);
        codeValues = getXTalkElementValues(RGBBase.Channel.G);
        reconstructor = new SimpleXTalkReconstructor(mmModel, property);
        double[][] rCorrectLut = getCorrectLUT(RGBBase.Channel.R, codeValues, codeValues);
        double[][] gCorrectLut = getCorrectLUT(RGBBase.Channel.G, codeValues, codeValues);
        double[][] bCorrectLut = getCorrectLUT(RGBBase.Channel.B, codeValues, codeValues);
        double[][][] correctLuts = new double[][][] { rCorrectLut, gCorrectLut, bCorrectLut };
        fourthReconstructor = new FourthLutXTalkReconstructor(mmModel, property);
        double[][][] fourthCorrectLut = getCorrectLUT(channelVI, codeValues, chVIIProperty, fourthReconstructor);
        correctLuts[chVIIProperty.getAdjacentChannel(channelVI).getArrayIndex()] = fourthCorrectLut[0];
        eliminator = new XTalkEliminator(codeValues, codeValues, correctLuts[0], correctLuts[1], correctLuts[2]);
        chVIEliminator = new XTalkEliminator(codeValues, codeValues, fourthCorrectLut[1], false);
        return new Factor[3];
    }

    protected double[][][] getCorrectLUT(RGBBase.Channel selfChannel, double[] channelValues, XTalkProperty property, FourthLutXTalkReconstructor reconstructor) {
        RGBBase.Channel adjChannel = property.getAdjacentChannel(selfChannel);
        RGB keyRGB = xtalkLCDTarget.getKeyRGB();
        keyRGB.setColorBlack();
        int size = channelValues.length;
        double[][][] correctLuts = new double[2][size][size];
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                keyRGB.setValue(adjChannel, channelValues[x]);
                keyRGB.setValue(selfChannel, channelValues[y]);
                Patch p = xtalkLCDTarget.getPatch(keyRGB);
                if (p == null || !p.getRGB().isSecondaryChannel()) {
                    continue;
                }
                RGB rgb = p.getRGB();
                RGB xtalkRGB = reconstructor.getXTalkRGB(p.getXYZ(), rgb, false);
                double selfCorrectValue = xtalkRGB.getValue(selfChannel) - keyRGB.getValue(selfChannel);
                double adjCorrectValue = xtalkRGB.getValue(adjChannel) - keyRGB.getValue(adjChannel);
                correctLuts[1][x][y] = selfCorrectValue;
                correctLuts[0][y][x] = adjCorrectValue;
            }
        }
        return correctLuts;
    }

    protected final RGB correctRGB(final RGB rgb, boolean uncorrect) {
        RGB correctRGB = super.correctRGB(rgb, uncorrect);
        if (channelVICorrect) {
            double selfValues = rgb.getValue(channelVI);
            double adjValues = rgb.getValue(chVIIProperty.getAdjacentChannel(channelVI));
            double selfCorrectValues = chVIEliminator.getCorrectionValue(adjValues, selfValues);
            if (uncorrect) {
                correctRGB.setValue(channelVI, correctRGB.getValue(channelVI) - selfCorrectValues);
            } else {
                correctRGB.setValue(channelVI, correctRGB.getValue(channelVI) + selfCorrectValues);
            }
        }
        return correctRGB;
    }

    public Plot3D[] plotCorrectLUT() {
        Plot3D[] plots = new Plot3D[4];
        double[][][] correctLut = eliminator.correctLut;
        for (int x = 0; x < 3; x++) {
            RGBBase.Channel ch = RGBBase.Channel.getChannelByArrayIndex(x);
            plots[x] = plot3DLUT(ch, correctLut[x], ch.color, property, " Correct LUT", "Correct code");
        }
        double[][][] correctLut2 = chVIEliminator.correctLut;
        plots[3] = plot3DLUT(channelVI, correctLut2[0], Color.black, chVIIProperty, " Correct LUT", "Correct code");
        PlotUtils.arrange(plots, 2, 2);
        PlotUtils.setVisible(plots);
        return plots;
    }
}
