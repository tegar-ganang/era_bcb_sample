package shu.cms.devicemodel.lcd.xtalk;

import java.text.*;
import shu.cms.*;
import shu.cms.colorspace.depend.*;
import shu.cms.devicemodel.lcd.*;
import shu.cms.devicemodel.lcd.util.*;
import shu.cms.lcd.*;
import shu.cms.lcd.material.*;
import shu.cms.plot.*;
import shu.math.lut.*;

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
public class TwoWayAdjacentXtalkModel extends AdjacentPixelXtalkModel {

    /**
   * TwoWayAdjacentXtalkModel
   *
   * @param lcdTarget LCDTarget
   * @param xtalkLCDTarget LCDTarget
   */
    public TwoWayAdjacentXtalkModel(LCDTarget lcdTarget, LCDTarget xtalkLCDTarget) {
        super(lcdTarget, xtalkLCDTarget);
    }

    /**
   * �D�Y��
   *
   * @return Factor[]
   */
    protected Factor[] _produceFactor() {
        mmModel = new MultiMatrixModel(lcdTarget);
        mmModel.produceFactor();
        mmModel.correct.setCorrectorAlgo(Interpolation1DLUT.Algo.QUADRATIC_POLYNOMIAL);
        mmModel.setCovertMode(this.covertMode);
        codeValues = getXTalkElementValues(RGBBase.Channel.G);
        leftReconstructor = new TwoWayXTalkReconstructor(mmModel, leftProperty);
        rightReconstructor = new TwoWayXTalkReconstructor(mmModel, rightProperty);
        reconstructors = new AbstractXTalkReconstructor[] { leftReconstructor, rightReconstructor };
        double[][][] rCorrectLut = getTwoWayCorrectLUT(RGBBase.Channel.R, codeValues);
        double[][][] gCorrectLut = getTwoWayCorrectLUT(RGBBase.Channel.G, codeValues);
        double[][][] bCorrectLut = getTwoWayCorrectLUT(RGBBase.Channel.B, codeValues);
        leftEliminator = new XTalkEliminator(codeValues, codeValues, rCorrectLut[0], gCorrectLut[0], bCorrectLut[0], false);
        rightEliminator = new XTalkEliminator(codeValues, codeValues, rCorrectLut[1], gCorrectLut[1], bCorrectLut[1], false);
        eliminators = new XTalkEliminator[] { leftEliminator, rightEliminator };
        return new Factor[3];
    }

    private XTalkEliminator leftEliminator;

    private XTalkEliminator rightEliminator;

    private XTalkEliminator[] eliminators;

    /**
   * �ѪR�XselfChannel�Q�v�T����Ӫ�.
   * �p�GselfChannel��G, �h�ѪR�XR->G(selfChannel����)�H��B->G(selfChannel���k)���v�T��.
   * @param selfChannel Channel
   * @param channelValues double[]
   * @return double[][][]
   */
    protected double[][][] getTwoWayCorrectLUT(RGBBase.Channel selfChannel, double[] channelValues) {
        RGBBase.Channel leftChannel = this.leftProperty.getAdjacentChannel(selfChannel);
        RGBBase.Channel rightChannel = this.rightProperty.getAdjacentChannel(selfChannel);
        RGBBase.Channel[] channels = new RGBBase.Channel[] { leftChannel, rightChannel };
        RGB keyRGB = xtalkLCDTarget.getKeyRGB();
        int size = channelValues.length;
        double[][] leftCorrectLut = new double[size][size];
        double[][] rightCorrectLut = new double[size][size];
        double[][][] correctLut = new double[][][] { leftCorrectLut, rightCorrectLut };
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int c = 0; c < channels.length; c++) {
                    RGBBase.Channel adjChannel = channels[c];
                    keyRGB.setColorBlack();
                    keyRGB.setValue(selfChannel, channelValues[y]);
                    keyRGB.setValue(adjChannel, channelValues[x]);
                    Patch p = xtalkLCDTarget.getPatch(keyRGB);
                    if (p == null || !p.getRGB().isSecondaryChannel()) {
                        continue;
                    }
                    RGB rgb = p.getRGB();
                    RGB xtalkRGB = reconstructors[c].getXTalkRGB(p.getXYZ(), rgb, false);
                    double correctValue = xtalkRGB.getValue(selfChannel) - keyRGB.getValue(selfChannel);
                    correctLut[c][x][y] = correctValue;
                }
            }
        }
        return new double[][][] { leftCorrectLut, rightCorrectLut };
    }

    public final Plot3D[] plotCorrectLUT() {
        Plot3D[] plots = new Plot3D[6];
        int index = 0;
        for (int x = 0; x < 3; x++) {
            for (int c = 0; c < 2; c++) {
                double[][][] correctLut = eliminators[c].correctLut;
                RGBBase.Channel ch = RGBBase.Channel.getChannelByArrayIndex(x);
                RGBBase.Channel adjch = properties[c].getAdjacentChannel(ch);
                plots[index] = Plot3D.getInstance(adjch.name() + "->" + ch.name() + " Correct LUT");
                plots[index].addGridPlot(ch.name(), ch.color, codeValues, codeValues, correctLut[x]);
                plots[index].setAxeLabel(0, adjch + " code");
                plots[index].setAxeLabel(1, ch + " code");
                plots[index].setAxeLabel(2, "Correct code");
                plots[index].setFixedBounds(0, 0, 255);
                plots[index].setFixedBounds(1, 0, 255);
                plots[index].setFixedBounds(2, -10, 10);
                index++;
            }
        }
        PlotUtils.arrange(plots, 3, 2);
        PlotUtils.setVisible(plots);
        return plots;
    }

    public static void main(String[] args) {
        LCDTarget complexLCDTarget = LCDTargetMaterial.getCPT320WF01SC_0227();
        LCDTarget ramplcdTarget = complexLCDTarget.targetFilter.getRamp();
        LCDTarget testlcdTarget = complexLCDTarget.targetFilter.getTest();
        LCDTarget xtalklcdTarget = complexLCDTarget.targetFilter.getXtalk();
        LCDModel model = new TwoWayAdjacentXtalkModel(ramplcdTarget, xtalklcdTarget);
        model.produceFactor();
        if (model instanceof AdjacentPixelXtalkModel) {
            ((AdjacentPixelXtalkModel) model).plotCorrectLUT();
        }
        DeltaEReport.setDecimalFormat(new DecimalFormat("##.####"));
        ModelReport report2 = model.report.getModelReport(testlcdTarget, 1);
        System.out.println(report2);
    }

    protected final RGB getCorrectRGB(final RGB rgb, boolean uncorrect) {
        if (rgb.isPrimaryChannel() || !crosstalkCorrect) {
            return rgb;
        }
        RGB correctRGB = (RGB) rgb.clone();
        correctRGB = correctRGB(correctRGB, uncorrect);
        correctRGB.rationalize();
        return correctRGB;
    }

    private XTalkProperty leftProperty = XTalkProperty.getLeftXTalkProperty();

    private XTalkProperty rightProperty = XTalkProperty.getRightXTalkProperty();

    private XTalkProperty[] properties = new XTalkProperty[] { leftProperty, rightProperty };

    private AbstractXTalkReconstructor leftReconstructor;

    private AbstractXTalkReconstructor rightReconstructor;

    private AbstractXTalkReconstructor[] reconstructors;

    protected final RGB correctRGB(final RGB rgb, boolean uncorrect) {
        RGB correctRGB = (RGB) rgb.clone();
        for (RGBBase.Channel ch : RGBBase.Channel.RGBChannel) {
            RGBBase.Channel leftchannel = this.leftProperty.getAdjacentChannel(ch);
            RGBBase.Channel rightchannel = this.rightProperty.getAdjacentChannel(ch);
            double selfValues = rgb.getValue(ch);
            double leftValues = rgb.getValue(leftchannel);
            double rightValues = rgb.getValue(rightchannel);
            double leftCorrectValues = leftEliminator.getCorrectionValue(ch, selfValues, leftValues);
            double rightCorrectValues = rightEliminator.getCorrectionValue(ch, selfValues, rightValues);
            if (uncorrect) {
                correctRGB.setValue(ch, correctRGB.getValue(ch) - leftCorrectValues - rightCorrectValues);
            } else {
                correctRGB.setValue(ch, correctRGB.getValue(ch) + leftCorrectValues + rightCorrectValues);
            }
        }
        return correctRGB;
    }
}
