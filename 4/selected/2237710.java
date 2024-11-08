package ch.laoe.clip;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import ch.laoe.operation.AOSpline;
import ch.laoe.operation.AOToolkit;
import ch.laoe.ui.Debug;
import ch.laoe.ui.GLanguage;
import ch.laoe.ui.LProgressViewer;
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


Class:			AChannelPlotterSampleCurve
Autor:			olivier g�umann, neuch�tel (switzerland)
JDK:				1.3

Desctription:	channel view.

History:
Date:			Description:									Autor:
28.12.00		first draft										oli4
24.01.01		array-based										oli4
28.03.01		bugfix in selection-painting				oli4
24.06.01		new x/y-skala painting						oli4
04.07.01		add white range on skalas					oli4
08.07.01		don't paint skala if range=0, because
				of hard VM crash !!!							oli4
18.07.01		add different sample drawings, with
				and without points							oli4
26.07.01		3rd order interpolation on big zoom    oli4
30.07.01		draw selection-dimension               oli4
11.01.02		introduce intensity-points					oli4
27.04.2003	"plots nothing on certain ranges" bug 
            fixed												oli4
            

***********************************************************/
public class AChannelPlotterSampleCurve extends AChannelPlotter {

    /**
	* constructor
	*/
    public AChannelPlotterSampleCurve(AModel m, AChannelPlotter p) {
        super(m, p);
        style = FILLED;
    }

    public double getAutoscaleXOffset() {
        return 0;
    }

    public double getAutoscaleXLength() {
        return getChannelModel().getSampleLength();
    }

    public float getAutoscaleYOffset(int xOffset, int xLength) {
        return -getChannelModel().getMaxSampleValue(xOffset, xLength);
    }

    public float getAutoscaleYLength(int xOffset, int xLength) {
        return 2 * getChannelModel().getMaxSampleValue(xOffset, xLength);
    }

    protected float getValidYOffset() {
        return -(1 << (((AClip) getChannelModel().getParent().getParent()).getSampleWidth() - 1));
    }

    protected float getValidYLength() {
        return (1 << ((AClip) getChannelModel().getParent().getParent()).getSampleWidth());
    }

    private int style;

    public static final int FILLED = 1;

    public static final int DRAWED = 2;

    /**
	* set the style of the sample curve
	*/
    public void setStyle(int style) {
        this.style = style;
    }

    private AOSpline spline = AOToolkit.createSpline();

    private MMArray x = null;

    private MMArray y = null;

    private MMArray cachedMin;

    private MMArray cachedMax;

    private static final int reduction = 333;

    private String oldId = "";

    /**
    * cache is reloaded when the clip ID has changed only. 
    */
    private final void reloadCache() {
        AChannel ch = getChannelModel();
        if (!oldId.equals(ch.getChangeId())) {
            LProgressViewer.getInstance().entrySubProgress(GLanguage.translate("reloadCache") + " " + model.getName());
            LProgressViewer.getInstance().entrySubProgress(0.9);
            Debug.println(7, "reload cache of channel sample plotter name=" + ch.getName() + " id=" + ch.getChangeId() + " oldid=" + oldId);
            oldId = ch.getChangeId();
            if (cachedMin == null) {
                cachedMin = new MMArray(ch.getSampleLength() / reduction, 0);
            }
            cachedMin.setLength(ch.getSampleLength() / reduction);
            if (cachedMax == null) {
                cachedMax = new MMArray(ch.getSampleLength() / reduction, 0);
            }
            cachedMax.setLength(ch.getSampleLength() / reduction);
            for (int i = 0; i < cachedMin.getLength(); i++) {
                if (i % 10000 == 0) LProgressViewer.getInstance().setProgress((i + 1) / cachedMin.getLength());
                cachedMin.set(i, getMinSample(i * reduction, (i + 1) * reduction));
                cachedMax.set(i, getMaxSample(i * reduction, (i + 1) * reduction));
            }
            LProgressViewer.getInstance().exitSubProgress();
            LProgressViewer.getInstance().exitSubProgress();
        }
    }

    /**
    * returns the min value of the given original channel's x index range. 
    * @param x1
    * @param x2
    * @return
    */
    private final float getMinSample(int x1, int x2) {
        if (x2 - x1 > reduction * 2) {
            x1 /= reduction;
            x2 /= reduction;
            float y = cachedMin.get(x1);
            for (int j = x1; j <= x2; j++) {
                float s = cachedMin.get(j);
                if (s < y) {
                    y = s;
                }
            }
            return y;
        } else {
            AChannel ch = getChannelModel();
            float y = ch.getSample(x1);
            for (int j = x1; j <= x2; j++) {
                float s = ch.getSample(j);
                if (s < y) {
                    y = s;
                }
            }
            return y;
        }
    }

    /**
    * returns the max value of the given original channel's x index range. 
    * @param x1
    * @param x2
    * @return
    */
    private final float getMaxSample(int x1, int x2) {
        if (x2 - x1 > reduction * 2) {
            x1 /= reduction;
            x2 /= reduction;
            float y = cachedMax.get(x1);
            for (int j = x1; j <= x2; j++) {
                float s = cachedMax.get(j);
                if (s > y) {
                    y = s;
                }
            }
            return y;
        } else {
            AChannel ch = getChannelModel();
            float y = ch.getSample(x1);
            for (int j = x1; j <= x2; j++) {
                float s = ch.getSample(j);
                if (s > y) {
                    y = s;
                }
            }
            return y;
        }
    }

    public void paintSamples(Graphics2D g2d, Color color, float colorGamma) {
        try {
            AChannel ch = getChannelModel();
            ALayer layer = (ALayer) ch.getParent();
            int width = rectangle.width;
            switch(layer.getType()) {
                case ALayer.PARAMETER_LAYER:
                    style = DRAWED;
                    break;
                default:
                    style = FILLED;
                    break;
            }
            int xMin;
            int xMax;
            float yTop;
            float yBottom;
            float yZero;
            float oldYTop = 0;
            float oldYBottom = 0;
            boolean firstPointPlotted = true;
            g2d.setClip(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
            Color brighterColor = GToolkit.mixColors(color, Color.WHITE, 0.17f);
            int l = (int) getXLength() + 3;
            if ((width / getXLength()) > 2.5) {
                if (x == null) {
                    x = new MMArray(l, 0);
                } else if (x.getLength() != l) {
                    x.setLength(l);
                }
                if (y == null) {
                    y = new MMArray(l, 0);
                } else if (y.getLength() != l) {
                    y.setLength(l);
                }
                for (int i = 0; i < l; i++) {
                    x.set(i, sampleToGraphX((int) getXOffset() + i));
                    y.set(i, sampleToGraphY(ch.getSample((int) getXOffset() + i)));
                }
                int oldX = (int) x.get(0) - 1;
                int oldY = (int) y.get(0);
                spline.load(x, y);
                g2d.setColor(color);
                for (int i = 0; i < width; i += 1) {
                    int newX = i;
                    int newY = (int) spline.getResult((float) i);
                    if (ch.getSamples().isInRange((int) graphToSampleX(newX))) {
                        g2d.drawLine(oldX, oldY, newX, newY);
                    }
                    oldX = newX;
                    oldY = newY;
                }
            } else {
                g2d.setColor(color);
                reloadCache();
                yZero = sampleToGraphY(0);
                for (int i = 0; i < width; i++) {
                    xMin = (int) graphToSampleX(i);
                    xMax = (int) graphToSampleX(i + 1);
                    if ((xMin >= ch.getSampleLength()) || (xMax >= ch.getSampleLength())) break;
                    if ((xMin < 0) || (xMax < 0)) continue;
                    yBottom = getMinSample(xMin, xMax);
                    yTop = getMaxSample(xMin, xMax);
                    yBottom = sampleToGraphY(yBottom);
                    yTop = sampleToGraphY(yTop);
                    if (firstPointPlotted) {
                        firstPointPlotted = false;
                        oldYBottom = yBottom;
                        oldYTop = yTop;
                    }
                    switch(style) {
                        case DRAWED:
                            g2d.drawLine(i - 1, (int) oldYBottom, i, (int) yBottom);
                            g2d.drawLine(i - 1, (int) oldYTop, i, (int) yTop);
                            break;
                        case FILLED:
                            if (yTop < yZero && yBottom > yZero) {
                                g2d.setColor(brighterColor);
                                g2d.drawLine(i, (int) yZero, i, (int) yTop);
                                g2d.setColor(color);
                                g2d.drawLine(i, (int) yBottom, i, (int) yZero);
                            } else {
                                g2d.setColor(color);
                                g2d.drawLine(i, (int) yBottom, i, (int) yTop);
                            }
                            break;
                    }
                    oldYBottom = yBottom;
                    oldYTop = yTop;
                }
            }
            if ((width / getXLength()) > 5) {
                g2d.setColor(color);
                for (int i = 0; i < l; i++) {
                    if (ch.getSamples().isInRange((int) graphToSampleX((int) x.get(i)))) {
                        g2d.fillRect((int) x.get(i) - 2, (int) y.get(i) - 2, 4, 4);
                    }
                }
            }
        } catch (Exception e) {
            Debug.printStackTrace(5, e);
        }
    }
}
