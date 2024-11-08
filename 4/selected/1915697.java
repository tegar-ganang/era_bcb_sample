package ch.laoe.clip;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import ch.laoe.ui.GPersistence;
import ch.laoe.ui.GToolkit;

/***********************************************************

This file is part of LAoE.

LAoE is free software; you can redistribute it and/or modify it
under the terms of the GNU General Public License as published
by the Free Software Foundation; either version 2 of the License,
or (at your option) any later version.

LAoE is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with LAoE; if not, write to the Free Software Foundation,
Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA


Class:			AClipPlotter
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	clip view.

History:
Date:			Description:									Autor:
28.12.00		first draft										oli4
24.01.01		array-based										oli4
26.12.01		play/loop pointers always painted		oli4
29.12.01		global setting of autoscale individual-y
																	oli4
11.02.02		unit added										oli4

***********************************************************/
public class AClipPlotter extends APlotter {

    /**
	* constructor
	*/
    public AClipPlotter(AModel m) {
        super(m);
    }

    public void setDefaultName() {
        name = "";
    }

    public AClip getClipModel() {
        return (AClip) model;
    }

    /**
	*	set the sample range
	*/
    public void setXRange(double offset, double length) {
        for (int i = 0; i < getClipModel().getNumberOfElements(); i++) {
            getClipModel().getLayer(i).getPlotter().setXRange(offset, length);
        }
    }

    /**
	*	set the amplitude range
	*/
    public void setYRange(float offset, float length) {
        for (int i = 0; i < getClipModel().getNumberOfElements(); i++) {
            getClipModel().getLayer(i).getPlotter().setYRange(offset, length);
        }
    }

    /**
	*	translate the sample offset
	*/
    public void translateXOffset(double offset) {
        for (int i = 0; i < getClipModel().getNumberOfElements(); i++) {
            getClipModel().getLayer(i).getPlotter().translateXOffset(offset);
        }
    }

    /**
	*	translate the amplitude offset
	*/
    public void translateYOffset(float offset) {
        for (int i = 0; i < getClipModel().getNumberOfElements(); i++) {
            getClipModel().getLayer(i).getPlotter().translateYOffset(offset);
        }
    }

    /**
	*	zoom x
	*/
    public void zoomX(double factor) {
        for (int i = 0; i < getClipModel().getNumberOfElements(); i++) {
            getClipModel().getLayer(i).getPlotter().zoomX(factor);
        }
    }

    public void zoomX(double factor, double xCenter) {
        for (int i = 0; i < getClipModel().getNumberOfElements(); i++) {
            getClipModel().getLayer(i).getPlotter().zoomX(factor, xCenter);
        }
    }

    /**
	*	zoom y
	*/
    public void zoomY(double factor) {
        for (int i = 0; i < getClipModel().getNumberOfElements(); i++) {
            getClipModel().getLayer(i).getPlotter().zoomY(factor);
        }
    }

    public void zoomY(double factor, double yCenter) {
        for (int i = 0; i < getClipModel().getNumberOfElements(); i++) {
            getClipModel().getLayer(i).getPlotter().zoomY(factor, yCenter);
        }
    }

    private static boolean autoScaleIndividualYEnable = false;

    public static void setAutoScaleIndividualYEnabled(boolean b) {
        autoScaleIndividualYEnable = b;
    }

    public void autoScale() {
        autoScaleX();
        autoScaleY();
    }

    public void autoScaleX() {
        double o = getAutoscaleXOffset();
        double l = getAutoscaleXLength();
        AClip c = getClipModel();
        for (int i = 0; i < c.getNumberOfLayers(); i++) {
            ALayerPlotter lp = c.getLayer(i).getPlotter();
            if (lp.isVisible()) {
                lp.setXRange(o - l * .01f, l * 1.02f);
            }
        }
    }

    public void autoScaleY() {
        autoScaleY(0, getClipModel().getMaxSampleLength());
    }

    public void autoScaleY(int xOffset, int xLength) {
        float o = getAutoscaleYOffset(xOffset, xLength);
        float l = getAutoscaleYLength(xOffset, xLength);
        AClip c = getClipModel();
        for (int i = 0; i < c.getNumberOfLayers(); i++) {
            ALayerPlotter lp = c.getLayer(i).getPlotter();
            if (lp.isVisible()) {
                if (autoScaleIndividualYEnable) {
                    lp.autoScaleY(xOffset, xLength);
                } else {
                    lp.setYRange(o - l * .03f, l * 1.06f);
                }
            }
        }
    }

    public double getAutoscaleXOffset() {
        double min = Double.MAX_VALUE;
        double f;
        AClip c = getClipModel();
        for (int i = 0; i < c.getNumberOfLayers(); i++) {
            ALayerPlotter lp = c.getLayer(i).getPlotter();
            if (lp.isVisible()) {
                f = lp.getAutoscaleXOffset();
                if (f < min) {
                    min = f;
                }
            }
        }
        return min;
    }

    public double getAutoscaleXLength() {
        double max = Double.MIN_VALUE;
        double f;
        AClip c = getClipModel();
        for (int i = 0; i < c.getNumberOfLayers(); i++) {
            ALayerPlotter lp = c.getLayer(i).getPlotter();
            if (lp.isVisible()) {
                f = lp.getAutoscaleXLength();
                if (f > max) {
                    max = f;
                }
            }
        }
        return max;
    }

    public float getAutoscaleYOffset(int xOffset, int xLength) {
        float min = Float.MAX_VALUE;
        float f;
        AClip c = getClipModel();
        for (int i = 0; i < c.getNumberOfLayers(); i++) {
            ALayerPlotter lp = c.getLayer(i).getPlotter();
            if (lp.isVisible()) {
                f = lp.getAutoscaleYOffset(xOffset, xLength);
                if (f < min) {
                    min = f;
                }
            }
        }
        return min;
    }

    public float getAutoscaleYLength(int xOffset, int xLength) {
        float max = Float.MIN_VALUE;
        float f;
        AClip c = getClipModel();
        for (int i = 0; i < c.getNumberOfLayers(); i++) {
            ALayerPlotter lp = c.getLayer(i).getPlotter();
            if (lp.isVisible()) {
                f = lp.getAutoscaleYLength(xOffset, xLength);
                if (f > max) {
                    max = f;
                }
            }
        }
        return max;
    }

    public static final int X_UNIT_1 = 0;

    public static final int X_UNIT_S = 1;

    public static final int X_UNIT_MS = 2;

    public static final int X_UNIT_PERCENT = 3;

    public static final int X_UNIT_HZ = 4;

    public static final int X_UNIT_FDHZ = 5;

    private static final String X_UNIT_NAMES[] = { "", "s", "ms", "%", "1/s", "Hz" };

    public static final int Y_UNIT_1 = 0;

    public static final int Y_UNIT_PERCENT = 1;

    public static final int Y_UNIT_DB = 2;

    public static final int Y_UNIT_FDHZ = 3;

    private static final String Y_UNIT_NAMES[] = { "", "%", "dB", "Hz" };

    public int plotterXUnit = X_UNIT_S;

    public int plotterYUnit = Y_UNIT_PERCENT;

    public void setPlotterXUnit(int unit) {
        plotterXUnit = unit;
    }

    public int getPlotterXUnit() {
        return plotterXUnit;
    }

    public void togglePlotterXUnit() {
        if (plotterXUnit < 5) {
            plotterXUnit++;
        } else {
            plotterXUnit = 0;
        }
    }

    public void setPlotterYUnit(int unit) {
        plotterYUnit = unit;
    }

    public int getPlotterYUnit() {
        return plotterYUnit;
    }

    public void togglePlotterYUnit() {
        if (plotterYUnit < 3) {
            plotterYUnit++;
        } else {
            plotterYUnit = 0;
        }
    }

    public String getPlotterXUnitName() {
        return X_UNIT_NAMES[plotterXUnit];
    }

    public static String[] getPlotterXUnitNames() {
        return X_UNIT_NAMES;
    }

    public String getPlotterYUnitName() {
        return Y_UNIT_NAMES[plotterYUnit];
    }

    public static String[] getPlotterYUnitNames() {
        return Y_UNIT_NAMES;
    }

    private static boolean skalaValuesVisible = true;

    /**
    * set the numeric values of the skala visibility
	 * @param b
	 */
    public static void setSkalaValuesVisible(boolean b) {
        skalaValuesVisible = b;
    }

    public static boolean isSkalaValuesVisible() {
        return skalaValuesVisible;
    }

    private static boolean gridVisible = true;

    /**
    * set the grid visibility
    * @param b
    */
    public static void setGridVisible(boolean b) {
        gridVisible = b;
    }

    public static boolean isGridVisible() {
        return gridVisible;
    }

    public double toPlotterXUnit(double d) {
        switch(plotterXUnit) {
            case X_UNIT_1:
                return d;
            case X_UNIT_S:
                return d / getClipModel().getSampleRate();
            case X_UNIT_MS:
                return d / getClipModel().getSampleRate() * 1000;
            case X_UNIT_PERCENT:
                return d / getClipModel().getMaxSampleLength() * 100;
            case X_UNIT_HZ:
                return getClipModel().getSampleRate() / d;
            case X_UNIT_FDHZ:
                return d / (2 * getClipModel().getMaxSampleLength()) * getClipModel().getSampleRate();
        }
        return d;
    }

    public float toPlotterYUnit(float d) {
        try {
            switch(plotterYUnit) {
                case Y_UNIT_1:
                    return d;
                case Y_UNIT_PERCENT:
                    return d / (1 << (getClipModel().getSampleWidth() - 1)) * 100;
                case Y_UNIT_DB:
                    if (d >= 0) {
                        return (float) (8.685890 * Math.log(Math.abs(d * 2) / Math.pow(2., getClipModel().getSampleWidth())));
                    } else {
                        return (float) (8.685890 * Math.log(Math.abs(d * 2) / Math.pow(2., getClipModel().getSampleWidth())));
                    }
                case Y_UNIT_FDHZ:
                    return d;
            }
        } catch (Exception e) {
        }
        return d;
    }

    double fromPlotterXUnit(double d) {
        switch(plotterXUnit) {
            case X_UNIT_1:
                return d;
            case X_UNIT_S:
                return d * getClipModel().getSampleRate();
            case X_UNIT_MS:
                return d * getClipModel().getSampleRate() / 1000;
            case X_UNIT_PERCENT:
                return d * getClipModel().getMaxSampleLength() / 100;
            case X_UNIT_HZ:
                return getClipModel().getSampleRate() / d;
            case X_UNIT_FDHZ:
                return d * (2 * getClipModel().getMaxSampleLength()) / getClipModel().getSampleRate();
        }
        return d;
    }

    float fromPlotterYUnit(float d) {
        try {
            switch(plotterYUnit) {
                case Y_UNIT_1:
                    return d;
                case Y_UNIT_PERCENT:
                    return d * (1 << (getClipModel().getSampleWidth() - 1)) / 100;
                case Y_UNIT_DB:
                    if (d >= 0) {
                        return (float) (Math.exp(d / 8.685890) * Math.pow(2., getClipModel().getSampleWidth()) / 2);
                    } else {
                        return (float) (Math.exp(d / 8.685890) * Math.pow(2., getClipModel().getSampleWidth()) / 2);
                    }
                case Y_UNIT_FDHZ:
                    return d;
            }
        } catch (Exception e) {
        }
        return d;
    }

    private Color bgColor = new Color(GPersistence.createPersistance().getInt("clip.defaultBackgroundColor"));

    public void setBgColor(Color c) {
        bgColor = c;
    }

    public Color getBgColor() {
        return bgColor;
    }

    private Color gridColor = new Color(GPersistence.createPersistance().getInt("clip.defaultGridColor"));

    public void setGridColor(Color c) {
        gridColor = c;
    }

    public Color getGridColor() {
        return gridColor;
    }

    protected int maxNumberOfChannels;

    protected int maxSampleLength;

    /**
	*	paint all layers in layered mode, mixed considering volume-transparency, 
	*	adapted for clip-editor	window. 
	*/
    public void paintFullClip(Graphics2D g2d, Rectangle rect) {
        if ((rect.width > 0) && (rect.height > 0)) {
            maxNumberOfChannels = getClipModel().getMaxNumberOfChannels();
            maxSampleLength = getClipModel().getMaxSampleLength();
            ALayer selectedLayer = getClipModel().getSelectedLayer();
            boolean bgPainted = false;
            for (int i = 0; i < getClipModel().getNumberOfLayers(); i++) {
                ALayer l = getClipModel().getLayer(i);
                if (l != selectedLayer) {
                    l.getPlotter().paintLayer(g2d, rect, maxSampleLength, maxNumberOfChannels, false, true, bgColor, 0.3f, !bgPainted, gridColor);
                    bgPainted = true;
                }
            }
            selectedLayer.getPlotter().paintLayer(g2d, rect, maxSampleLength, maxNumberOfChannels, true, true, bgColor, 1.0f, !bgPainted, gridColor);
        }
    }

    private static final Color playPointerColor = Color.GREEN.darker();

    private static final Color loopPointerColor = Color.RED.darker();

    private static final Color selectionSurfaceColor = Color.YELLOW;

    private static final Color selectionLineColor = GToolkit.mixColors(Color.RED, Color.YELLOW, 0.4f);

    /**
	*	paints all the selections of the top layer only
	*/
    public void paintDetailsOfSelectedLayer(Graphics2D g2d, Rectangle rect) {
        if (getClipModel().getNumberOfLayers() > 0) {
            getClipModel().getSelectedLayer().getPlotter().paintAllSelections(g2d, rect, selectionSurfaceColor, selectionLineColor, maxSampleLength, maxNumberOfChannels);
            getClipModel().getAudio().getPlotter().paintPlayPointer(g2d, rect, playPointerColor);
            getClipModel().getAudio().getPlotter().paintLoopPointer(g2d, rect, loopPointerColor);
            getClipModel().getSelectedLayer().getPlotter().paintMasks(g2d, rect, maxSampleLength, maxNumberOfChannels);
            getClipModel().getSelectedLayer().getPlotter().paintMarkers(g2d, rect, maxSampleLength, maxNumberOfChannels);
        }
    }

    /**
	* 	paints only one layer, without volume-transparency, without selection, adapted
	*	for thumbnail-representation of a layer
	*/
    public void paintLayerThumbnail(Graphics2D g2d, Rectangle rect, int layerIndex) {
        ALayer l = getClipModel().getLayer(layerIndex);
        l.getPlotter().paintLayer(g2d, rect, l.getMaxSampleLength(), l.getNumberOfChannels(), false, false, bgColor, 1.0f, true, gridColor);
    }

    /**
	* 	paints only one channel, without volume-transparency, without selection, adapted
	*	for thumbnail-representation of a channel
	*/
    public void paintChannelThumbnail(Graphics2D g2d, Rectangle rect, int layerIndex, int channelIndex) {
        ALayer l = getClipModel().getLayer(layerIndex);
        AChannel ch = l.getChannel(channelIndex);
        ch.getPlotter().setRectangle(rect);
        ch.getPlotter().paintBackground(g2d, bgColor);
        ch.getPlotter().paintSamples(g2d, l.getPlotter().getColor(), l.getPlotter().getColorGamma());
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.f));
        ch.getPlotter().paintFrame(g2d);
    }
}
