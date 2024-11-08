package ch.laoe.clip;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import ch.laoe.ui.GPersistence;

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


Class:			ALayerPlotter
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	layer view.

History:
Date:			Description:									Autor:
28.12.00		first draft										oli4
24.01.01		array-based										oli4
24.06.01		new x/y-skala painting						oli4

***********************************************************/
public class ALayerPlotter extends APlotter {

    /**
	* constructor
	*/
    public ALayerPlotter(AModel m) {
        super(m);
        setVisible(true);
    }

    public void setDefaultName() {
        name = "";
    }

    public ALayer getLayerModel() {
        return (ALayer) model;
    }

    private boolean isVisible;

    /**
	*	set the whole layer visible/invisible in a layered representation. this has no
	*	effect on the sample-data, it's only a graphical behaviour
	*/
    public void setVisible(boolean b) {
        isVisible = b;
    }

    /**
	*	get the actual visibility
	*/
    public boolean isVisible() {
        return isVisible;
    }

    private Color color = new Color(GPersistence.createPersistance().getInt("clip.defaultSampleColor"));

    public void setColor(Color c) {
        color = c;
    }

    public Color getColor() {
        return color;
    }

    private float colorGamma = 0.18f;

    public void setColorGamma(float g) {
        colorGamma = g;
    }

    public float getColorGamma() {
        return colorGamma;
    }

    /**
	*	set the sample range
	*/
    public void setXRange(double offset, double length) {
        for (int i = 0; i < getLayerModel().getNumberOfElements(); i++) {
            getLayerModel().getChannel(i).getPlotter().setXRange(offset, length);
        }
    }

    /**
	*	set the sample range
	*/
    public void setYRange(float offset, float length) {
        for (int i = 0; i < getLayerModel().getNumberOfElements(); i++) {
            getLayerModel().getChannel(i).getPlotter().setYRange(offset, length);
        }
    }

    /**
	*	translate the sample offset
	*/
    public void translateXOffset(double offset) {
        for (int i = 0; i < getLayerModel().getNumberOfElements(); i++) {
            getLayerModel().getChannel(i).getPlotter().translateXOffset(offset);
        }
    }

    /**
	*	translate the sample offset
	*/
    public void translateYOffset(float offset) {
        for (int i = 0; i < getLayerModel().getNumberOfElements(); i++) {
            getLayerModel().getChannel(i).getPlotter().translateYOffset(offset);
        }
    }

    /**
	*	zoom x
	*/
    public void zoomX(double factor) {
        for (int i = 0; i < getLayerModel().getNumberOfElements(); i++) {
            getLayerModel().getChannel(i).getPlotter().zoomX(factor);
        }
    }

    public void zoomX(double factor, double xCenter) {
        for (int i = 0; i < getLayerModel().getNumberOfElements(); i++) {
            getLayerModel().getChannel(i).getPlotter().zoomX(factor, xCenter);
        }
    }

    /**
	*	zoom y
	*/
    public void zoomY(double factor) {
        for (int i = 0; i < getLayerModel().getNumberOfElements(); i++) {
            getLayerModel().getChannel(i).getPlotter().zoomY(factor);
        }
    }

    public void zoomY(double factor, double yCenter) {
        for (int i = 0; i < getLayerModel().getNumberOfElements(); i++) {
            getLayerModel().getChannel(i).getPlotter().zoomY(factor, yCenter);
        }
    }

    public void autoScaleX() {
        double o = getAutoscaleXOffset();
        double l = getAutoscaleXLength();
        setXRange(o - l * .01f, l * 1.02f);
    }

    public void autoScaleY() {
        autoScaleY(0, getLayerModel().getMaxSampleLength());
    }

    public void autoScaleY(int xOffset, int xLength) {
        float o = getAutoscaleYOffset(xOffset, xLength);
        float l = getAutoscaleYLength(xOffset, xLength);
        setYRange(o - l * .03f, l * 1.06f);
    }

    public double getAutoscaleXOffset() {
        double min = Double.MAX_VALUE;
        double f;
        ALayer l = getLayerModel();
        for (int i = 0; i < l.getNumberOfChannels(); i++) {
            f = l.getChannel(i).getPlotter().getAutoscaleXOffset();
            if (f < min) {
                min = f;
            }
        }
        return min;
    }

    public double getAutoscaleXLength() {
        double max = Double.MIN_VALUE;
        double f;
        ALayer l = getLayerModel();
        for (int i = 0; i < l.getNumberOfChannels(); i++) {
            f = l.getChannel(i).getPlotter().getAutoscaleXLength();
            if (f > max) {
                max = f;
            }
        }
        return max;
    }

    public float getAutoscaleYOffset(int xOffset, int xLength) {
        float min = Float.MAX_VALUE;
        float f;
        ALayer l = getLayerModel();
        for (int i = 0; i < l.getNumberOfChannels(); i++) {
            f = l.getChannel(i).getPlotter().getAutoscaleYOffset(xOffset, xLength);
            if (f < min) {
                min = f;
            }
        }
        return min;
    }

    public float getAutoscaleYLength(int xOffset, int xLength) {
        float max = Float.MIN_VALUE;
        float f;
        ALayer l = getLayerModel();
        for (int i = 0; i < l.getNumberOfChannels(); i++) {
            f = l.getChannel(i).getPlotter().getAutoscaleYLength(xOffset, xLength);
            if (f > max) {
                max = f;
            }
        }
        return max;
    }

    private int getSkalaSize(Rectangle layerRect, int maxNumberOfChannels) {
        int min = 1;
        int max = 8;
        int s = Math.min(layerRect.width, layerRect.height) / 20;
        return Math.max(Math.min(s, max), min);
    }

    /**
	* returns a rectangle which defines the shape of a channel in the layer representation
	*/
    private Rectangle createChannelRect(Rectangle layerRect, int channelIndex, int maxNumberOfChannels) {
        int lW = layerRect.width;
        int lH = layerRect.height;
        int skalaSize = getSkalaSize(layerRect, maxNumberOfChannels);
        int chH = lH / maxNumberOfChannels - skalaSize;
        int chW = lW - skalaSize;
        return new Rectangle(skalaSize, channelIndex * (chH + skalaSize), chW, chH);
    }

    /**
	*	returns a rectangle which defines the shape between two channels
	*/
    private Rectangle createYSkalaRect(Rectangle layerRect, int channelIndex, int maxNumberOfChannels) {
        int lH = layerRect.height;
        int skalaSize = getSkalaSize(layerRect, maxNumberOfChannels);
        int chH = lH / maxNumberOfChannels - skalaSize;
        return new Rectangle(0, channelIndex * (chH + skalaSize), skalaSize, chH);
    }

    /**
	*	returns a rectangle which defines the shape of the x-skala of a channel
	*/
    private Rectangle createXSkalaRect(Rectangle layerRect, int channelIndex, int maxNumberOfChannels) {
        int lW = layerRect.width;
        int lH = layerRect.height;
        int skalaSize = getSkalaSize(layerRect, maxNumberOfChannels);
        int chH = lH / maxNumberOfChannels - skalaSize;
        int chW = lW - skalaSize;
        return new Rectangle(skalaSize, channelIndex * (chH + skalaSize) + chH, chW - skalaSize, skalaSize);
    }

    /**
	*	paints a whole layer: channels and its frames
	*/
    public void paintLayer(Graphics2D g2d, Rectangle layerRect, int maxSampleLength, int maxNumberOfChannels, boolean skalaEnable, boolean volumeSensitive, Color bgColor, float alpha, boolean paintBackground, Color gridColor) {
        if (isVisible || !volumeSensitive) {
            ALayer l = getLayerModel();
            for (int i = 0; i < maxNumberOfChannels; i++) {
                Rectangle channelRect = createChannelRect(layerRect, i, maxNumberOfChannels);
                if (i < l.getNumberOfChannels()) {
                    l.getChannel(i).getPlotter().setRectangle(channelRect);
                    if (i < l.getNumberOfChannels()) {
                        if (paintBackground) {
                            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.f));
                            l.getChannel(i).getPlotter().paintBackground(g2d, bgColor);
                        }
                        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                        l.getChannel(i).getPlotter().paintSamples(g2d, color, colorGamma);
                        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.f));
                        l.getChannel(i).getPlotter().paintFrame(g2d);
                    }
                    if (skalaEnable && (i < l.getNumberOfChannels())) {
                        Rectangle xr = createXSkalaRect(layerRect, i, maxNumberOfChannels);
                        Rectangle yr = createYSkalaRect(layerRect, i, maxNumberOfChannels);
                        l.getChannel(i).getPlotter().paintXSkala(g2d, xr, gridColor);
                        l.getChannel(i).getPlotter().paintYSkala(g2d, yr, gridColor);
                        if (AClipPlotter.isGridVisible()) {
                            l.getChannel(i).getPlotter().paintGrid(g2d, channelRect, gridColor);
                        }
                    }
                }
            }
        }
    }

    /**
	*	paints all selections int the channels of this layer.
	*/
    public void paintAllSelections(Graphics2D g2d, Rectangle layerRect, Color surfaceColor, Color lineColor, int maxSampleLength, int numberOfChannels) {
        if (isVisible) {
            ALayer l = getLayerModel();
            for (int i = 0; i < numberOfChannels; i++) {
                Rectangle subRect = createChannelRect(layerRect, i, numberOfChannels);
                if (i < l.getNumberOfChannels()) {
                    l.getChannel(i).getPlotter().setRectangle(subRect);
                    l.getChannel(i).getPlotter().paintSelection(g2d, surfaceColor, lineColor);
                }
            }
        }
    }

    /**
	*	paint the volume mask of the channels.
	*/
    public void paintMasks(Graphics2D g2d, Rectangle layerRect, int maxSampleLength, int numberOfChannels) {
        if (isVisible) {
            ALayer l = getLayerModel();
            for (int i = 0; i < numberOfChannels; i++) {
                Rectangle subRect = createChannelRect(layerRect, i, numberOfChannels);
                if (i < l.getNumberOfChannels()) {
                    l.getChannel(i).getPlotter().setRectangle(subRect);
                    l.getChannel(i).getPlotter().paintMask(g2d);
                }
            }
        }
    }

    /**
	*	paint the markers of the channels.
	*/
    public void paintMarkers(Graphics2D g2d, Rectangle layerRect, int maxSampleLength, int numberOfChannels) {
        if (isVisible) {
            ALayer l = getLayerModel();
            for (int i = 0; i < numberOfChannels; i++) {
                if (i < l.getNumberOfChannels()) {
                    Rectangle chr = createChannelRect(layerRect, i, numberOfChannels);
                    Rectangle xr = createXSkalaRect(layerRect, i, numberOfChannels);
                    l.getChannel(i).getPlotter().paintMarker(g2d, chr, xr);
                }
            }
        }
    }

    /**
	*	returns the channel index, in which the point p is residing. If
	*	no channel is concerned, it returns -1
	*/
    public int getInsideChannelIndex(Point p) {
        try {
            ALayer l = getLayerModel();
            for (int i = 0; i < l.getNumberOfChannels(); i++) {
                if (l.getChannel(i).getPlotter().isInsideChannel(p)) return i;
            }
        } catch (NullPointerException npe) {
        }
        return -1;
    }
}
