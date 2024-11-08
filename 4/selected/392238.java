package ch.laoe.clip;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;

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


Class:			AChannelPlotter
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
06.05.2003	paint numeric values in rulers			oli4

***********************************************************/
public abstract class AChannelPlotter extends APlotter {

    /**
	* constructor
	*/
    public AChannelPlotter(AModel m) {
        super(m);
        xLength = 1;
        yLength = 1;
        rectangle = new Rectangle(1, 1);
    }

    /**
	* constructor
	*/
    public AChannelPlotter(AModel m, AChannelPlotter p) {
        this(m);
        if (p != null) {
            xOffset = p.xOffset;
            xLength = p.xLength;
            yOffset = p.yOffset;
            yLength = p.yLength;
        }
    }

    public AChannel getChannelModel() {
        return (AChannel) model;
    }

    public void setDefaultName() {
        name = "";
    }

    private double xOffset, xLength;

    private float yOffset, yLength;

    protected Rectangle rectangle;

    /**
	*	returns the actual x offset
	*/
    public double getXOffset() {
        return xOffset;
    }

    /**
	*	returns the actual x length
	*/
    public double getXLength() {
        return xLength;
    }

    /**
	*	returns the actual y offset
	*/
    public float getYOffset() {
        return yOffset;
    }

    /**
	*	returns the actual y length
	*/
    public float getYLength() {
        return yLength;
    }

    public abstract double getAutoscaleXOffset();

    public abstract double getAutoscaleXLength();

    public abstract float getAutoscaleYOffset(int xOffset, int xLength);

    public abstract float getAutoscaleYLength(int xOffset, int xLength);

    protected abstract float getValidYOffset();

    protected abstract float getValidYLength();

    /**
	*	set the sample range
	*/
    public void setXRange(double offset, double length) {
        xOffset = offset;
        xLength = length;
        limitXRange();
    }

    /**
	*	set the amplitude range
	*/
    public void setYRange(float offset, float length) {
        yOffset = offset;
        yLength = length;
        limitYRange();
    }

    /**
	*	translate the sample offset
	*/
    public void translateXOffset(double offset) {
        xOffset += offset;
        limitXRange();
    }

    /**
	*	translate the amplitude offset
	*/
    public void translateYOffset(float offset) {
        yOffset += offset;
        limitYRange();
    }

    /**
	*	limits x-ranges
	*/
    private void limitXRange() {
        if (xOffset < Integer.MIN_VALUE / 2) xOffset = Integer.MIN_VALUE / 2; else if (xOffset > Integer.MAX_VALUE / 2) xOffset = Integer.MAX_VALUE / 2;
        if (xLength > Integer.MAX_VALUE) xLength = Integer.MAX_VALUE; else if (xLength < .1f) xLength = .1f;
    }

    /**
	*	limits y-ranges
	*/
    private void limitYRange() {
        if (yOffset > Integer.MAX_VALUE) yOffset = Integer.MAX_VALUE; else if (yOffset < Integer.MIN_VALUE) yOffset = Integer.MIN_VALUE;
        if (yLength > Integer.MAX_VALUE) yLength = Integer.MAX_VALUE;
        if (Math.abs(yLength) < 1e-12) yLength = 1e-12f;
    }

    /**
	 *	zoom x by the given factor, zooming into the plot center
	 */
    public void zoomX(double factor) {
        xOffset += xLength * (1 - 1 / factor) / 2;
        xLength /= factor;
        limitXRange();
    }

    /**
    * zoom x by the given factor, zooming into xCenter
    */
    public void zoomX(double factor, double xCenter) {
        double a1 = xCenter - xOffset;
        double a2 = a1 / factor;
        xOffset += a1 - a2;
        xLength /= factor;
        limitXRange();
    }

    /**
	*	zoom y
	*/
    public void zoomY(double factor) {
        yOffset += yLength * (1 - 1 / factor) / 2;
        yLength /= factor;
        limitYRange();
    }

    /**
    * zoom y by the given factor, zooming into yCenter
    */
    public void zoomY(double factor, double yCenter) {
        double a1 = yCenter - yOffset;
        double a2 = a1 / factor;
        yOffset += a1 - a2;
        yLength /= factor;
        limitYRange();
    }

    public int sampleToGraphX(double x) {
        return (int) ((x - xOffset) * rectangle.width / xLength) + rectangle.x;
    }

    public int sampleToGraphY(float y) {
        return (int) ((yLength - y + yOffset) * rectangle.height / yLength) + rectangle.y;
    }

    public int percentToGraphY(float y) {
        return (int) ((1.f - y) * rectangle.height) + rectangle.y;
    }

    public double graphToSampleX(int x) {
        return xOffset + ((x - rectangle.x) * xLength) / rectangle.width;
    }

    public float graphToSampleY(int y) {
        return yOffset + ((rectangle.height - y + rectangle.y) * yLength / rectangle.height);
    }

    public float graphToPercentY(float y) {
        return (rectangle.height - y + rectangle.y) / rectangle.height;
    }

    /**
	* set the pixel-rectangle, in which the track is painted
	*/
    public void setRectangle(Rectangle r) {
        rectangle = r;
    }

    /**
	*	get the pixel-rectangle
	*/
    public Rectangle getRectangle() {
        return rectangle;
    }

    /**
	*	returns true, if the point p is inside the sample-curve-rectangle
	*/
    public boolean isInsideChannel(Point p) {
        return ((p.y >= rectangle.y) && (p.y <= (rectangle.y + rectangle.height)) && (p.x >= rectangle.x) && (p.y <= (rectangle.x + rectangle.width)));
    }

    /**
	*	paint the frame around the sample-curve
	*/
    public void paintFrame(Graphics2D g2d) {
        int width = rectangle.width;
        int height = rectangle.height;
        int x = rectangle.x;
        int y = rectangle.y;
        int y0 = sampleToGraphY(0);
        int yBottomLimit = sampleToGraphY(getValidYOffset());
        int yTopLimit = sampleToGraphY(getValidYOffset() + getValidYLength());
        g2d.setClip(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
        g2d.setColor(new Color(0x7F3F3F3F, true));
        g2d.drawLine(x, y0, x + width - 1, y0);
        g2d.setColor(new Color(0x7F555555, true));
        g2d.drawLine(x, yTopLimit, x + width - 1, yTopLimit);
        g2d.drawLine(x, yBottomLimit, x + width - 1, yBottomLimit);
        g2d.setColor(Color.gray);
        g2d.drawRect(x, y, width - 2, height - 2);
        g2d.setColor(Color.white);
        g2d.drawRect(x + 1, y + 1, width - 2, height - 2);
    }

    public void paintBackground(Graphics2D g2d, Color color) {
        g2d.setClip(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
        GradientPaint gp = new GradientPaint(rectangle.x, rectangle.y, color.brighter().brighter(), rectangle.x, rectangle.y + rectangle.height, color, false);
        g2d.setPaint(gp);
        g2d.fillRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }

    /**
	*	paint the sample-curve
	*/
    public abstract void paintSamples(Graphics2D g2d, Color color, float colorGamma);

    /**
	*	paints the mask
	*/
    public void paintMask(Graphics2D g2d) {
        getChannelModel().getMask().paintOntoClip(g2d, rectangle);
    }

    /**
	*	paints the markers
	*/
    public void paintMarker(Graphics2D g2d, Rectangle channelRect, Rectangle scalaRect) {
        getChannelModel().getMarker().paintOntoClip(g2d, channelRect, scalaRect);
    }

    /**
	*	highlight the actually selected part of the sample-curve
	*/
    public void paintSelection(Graphics2D g2d, Color surfaceColor, Color lineColor) {
        AChannelSelection s = getChannelModel().getSelection();
        AClipPlotter cp = ((AClip) model.getParent().getParent()).getPlotter();
        if (s.isSelected()) {
            int xLeft = sampleToGraphX(s.getOffset());
            int y = rectangle.y + 2;
            int xRight = sampleToGraphX(s.getOffset() + s.getLength());
            int h = rectangle.height - 5;
            if (xLeft < 0) xLeft = -50; else if (xLeft > rectangle.width) return;
            if (xRight > rectangle.width) xRight = rectangle.width + 50; else if (xRight < 0) return;
            g2d.setColor(surfaceColor);
            g2d.setClip(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .18f));
            g2d.fillRect(xLeft, y, xRight - xLeft, h);
            float dash[] = { 4.f, 4.f };
            g2d.setStroke(new BasicStroke(1.f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 1.f, dash, 0.f));
            g2d.setColor(lineColor);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.f));
            g2d.drawRect(xLeft, y, xRight - xLeft, h);
            g2d.setStroke(new BasicStroke());
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.f));
            int oldPx = 0;
            int oldPy = 0;
            for (int i = xLeft; i < xRight; i += 2) {
                int px = i;
                float intY = s.getIntensity((int) graphToSampleX(i));
                int py = (int) ((1 - intY) * h) + y;
                g2d.setColor(lineColor);
                if (i != xLeft) {
                    g2d.drawLine(oldPx, oldPy, px, py);
                }
                oldPx = px;
                oldPy = py;
            }
            for (int i = 0; i < s.getIntensityPoints().size(); i++) {
                AChannelSelection.Point sp = (AChannelSelection.Point) s.getIntensityPoints().get(i);
                int px = sampleToGraphX(s.getOffset() + (s.getLength() * sp.x));
                int py = (int) ((1 - sp.y) * h) + y;
                if (s.getActiveIntensityPointIndex() == i) {
                    g2d.setColor(Color.red);
                    g2d.drawRect(px - 4, py - 4, 8, 8);
                } else {
                    g2d.setColor(lineColor);
                }
                g2d.fillRect(px - 2, py - 2, 5, 5);
                float yy = (float) sp.y;
                String ss = "" + yy;
                ss = ss.substring(0, Math.min(ss.length(), 5));
                g2d.setColor(lineColor);
                AChannelPlotter.paintText(g2d, ss, 10, px, yy > 0.5 ? py + 10 : py - 10, false);
            }
            NumberFormat nf = NumberFormat.getInstance();
            nf.setMaximumFractionDigits(4);
            nf.setGroupingUsed(false);
            g2d.setColor(lineColor);
            g2d.setFont(new Font("Courrier", Font.PLAIN, 12));
            FontMetrics fm = g2d.getFontMetrics();
            String topStr = "" + nf.format(cp.toPlotterXUnit(s.getOffset())) + cp.getPlotterXUnitName();
            String bottomStr = "" + nf.format(cp.toPlotterXUnit(s.getLength())) + cp.getPlotterXUnitName();
            int mx = (xRight + xLeft) / 2;
            int my = y + (h / 2);
            g2d.drawString(topStr, mx - fm.stringWidth(topStr) / 2, my - fm.getHeight() / 2);
            g2d.drawString(bottomStr, mx - fm.stringWidth(bottomStr) / 2, my + fm.getHeight() / 2);
        }
    }

    /**
	 * paints a text, given the center coordinates
	 * @param g2d
	 * @param text text to paint
	 * @param x coordinate of middle of text
	 * @param y coordinate of middle of text
	 * @param vertical unused yet
	 */
    public static void paintText(Graphics2D g2d, String text, int size, int x, int y, boolean vertical) {
        if (vertical) {
            g2d.setFont(new Font("Courrier", Font.PLAIN, size));
            FontMetrics fm = g2d.getFontMetrics();
            int w = fm.stringWidth(text) * 2;
            BufferedImage im = new BufferedImage(w, w, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D tg2d = (Graphics2D) im.getGraphics();
            tg2d.setColor(g2d.getColor());
            tg2d.setFont(new Font("Courrier", Font.PLAIN, size));
            tg2d.drawString(text, w / 2, w / 2 + fm.getAscent() - fm.getHeight() / 2);
            AffineTransform at = new AffineTransform();
            at.rotate(-Math.PI / 2, w / 2, w / 2);
            AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
            im = op.filter(im, null);
            g2d.drawImage(im, x - w / 2, y - w / 2, null);
        } else {
            g2d.setFont(new Font("Courrier", Font.PLAIN, size));
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(text, x - fm.stringWidth(text) / 2, y + fm.getAscent() - fm.getHeight() / 2);
        }
    }

    /**
	 * same as paintText, but paints formatted numeric value
	 * @param g2d
	 * @param value
	 * @param size
	 * @param x
	 * @param y
	 */
    private static String doubleToString(double value) {
        String multi = "";
        if (Math.abs(value) >= 1e9) {
            value /= 1e9;
            multi = "G";
        } else if (Math.abs(value) >= 1e6) {
            value /= 1e6;
            multi = "M";
        } else if (Math.abs(value) >= 1e3) {
            value /= 1e3;
            multi = "k";
        } else if (Math.abs(value) >= 1e0) {
            multi = "";
        } else if (Math.abs(value) >= 1e-3) {
            value /= 1e-3;
            multi = "m";
        } else if (Math.abs(value) >= 1e-6) {
            value /= 1e-6;
            multi = "u";
        }
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(5);
        nf.setGroupingUsed(false);
        return nf.format(value) + multi;
    }

    /**
	*	highlight the actually selected part of the sample-curve
	*/
    public void paintXSkala(Graphics2D g2d, Rectangle rect, Color color) {
        if (getXLength() < .1) return;
        g2d.setClip(rect.x, rect.y - 200, rect.width, rect.height + 200);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.f));
        AChannel ch = getChannelModel();
        int rangeBegin = Math.max(0, sampleToGraphX(0));
        int rangeEnd = Math.min(rect.x + rect.width, sampleToGraphX(ch.getSampleLength()));
        g2d.setColor(Color.white);
        g2d.fillRect(rangeBegin, rect.y, rangeEnd - rangeBegin, rect.height);
        AClipPlotter cp = ((AClip) model.getParent().getParent()).getPlotter();
        double oldU = cp.toPlotterXUnit(graphToSampleX(rect.x));
        int oldL = -1000;
        int deltaL = 20;
        int deltaP = 7;
        int minorLineLength = (int) (rect.height * 0.3);
        int mediumLineLength = (int) (rect.height * 0.5);
        int majorLineLength = (int) (rect.height * 0.8);
        for (int i = 0; i < rect.width; i++) {
            double u = cp.toPlotterXUnit(graphToSampleX(rect.x + i));
            double deltaU = u - cp.toPlotterXUnit(graphToSampleX(rect.x + i + deltaP));
            double decadedU = Math.pow(10, Math.round(Math.log10(Math.abs(deltaU))));
            if (((int) (u / decadedU / 10)) != ((int) (oldU / decadedU / 10)) || Math.signum(u) != Math.signum(oldU)) {
                g2d.setColor(Color.gray);
                g2d.drawLine(rect.x + i, rect.y, rect.x + i, rect.y + majorLineLength);
                if (AClipPlotter.isSkalaValuesVisible()) {
                    oldL = i;
                    double printedU = Math.round(u / decadedU) * decadedU;
                    g2d.setColor(color);
                    paintText(g2d, doubleToString(printedU) + cp.getPlotterXUnitName(), 10, rect.x + i, rect.y - 6, true);
                }
            } else if (((int) (u / decadedU / 5)) != ((int) (oldU / decadedU / 5)) || Math.signum(u) != Math.signum(oldU)) {
                g2d.setColor(Color.gray);
                g2d.drawLine(rect.x + i, rect.y, rect.x + i, rect.y + mediumLineLength);
                if (i > (oldL + deltaL) && AClipPlotter.isSkalaValuesVisible()) {
                    double printedU = Math.round(u / decadedU) * decadedU;
                    g2d.setColor(color);
                    paintText(g2d, doubleToString(printedU) + cp.getPlotterXUnitName(), 10, rect.x + i, rect.y - 6, true);
                }
            } else if (((int) (u / decadedU)) != ((int) (oldU / decadedU)) || Math.signum(u) != Math.signum(oldU)) {
                g2d.setColor(Color.gray);
                g2d.drawLine(rect.x + i, rect.y, rect.x + i, rect.y + minorLineLength);
            }
            oldU = u;
        }
    }

    /**
	*	highlight the actually selected part of the sample-curve
	*/
    public void paintYSkala(Graphics2D g2d, Rectangle rect, Color color) {
        if (getYLength() < .1) return;
        g2d.setClip(rect.x, rect.y, rect.width + 100, rect.height);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.f));
        int rangeTop = Math.max(0, sampleToGraphY(getValidYOffset() + getValidYLength()));
        int rangeBottom = Math.min(rect.y + rect.height, sampleToGraphY(getValidYOffset()));
        g2d.setColor(Color.white);
        g2d.fillRect(rect.x, rangeTop, rect.width, rangeBottom - rangeTop);
        AClipPlotter cp = ((AClip) model.getParent().getParent()).getPlotter();
        float oldU = cp.toPlotterYUnit(graphToSampleY(rect.y));
        int oldL = -1000;
        int deltaL = 20;
        int deltaP = 5;
        int minorLineLength = (int) (rect.width * 0.3);
        int mediumLineLength = (int) (rect.width * 0.5);
        int majorLineLength = (int) (rect.width * 0.8);
        for (int i = 0; i < rect.height; i++) {
            float u = cp.toPlotterYUnit(graphToSampleY(rect.y + i));
            float deltaU = u - cp.toPlotterYUnit(graphToSampleY(rect.y + i + deltaP));
            double decadedU = Math.pow(10, Math.round(Math.log10(Math.abs(deltaU))));
            if (((int) (u / decadedU / 10)) != ((int) (oldU / decadedU / 10)) || Math.signum(u) != Math.signum(oldU)) {
                g2d.setColor(Color.gray);
                g2d.drawLine(rect.x + rect.width - majorLineLength, rect.y + i, rect.x + rect.width, rect.y + i);
                if (AClipPlotter.isSkalaValuesVisible()) {
                    oldL = i;
                    float printedU = (float) (Math.round(u / decadedU) * decadedU);
                    g2d.setColor(color);
                    paintText(g2d, doubleToString(printedU) + cp.getPlotterYUnitName(), 10, rect.x + 30, rect.y + i, false);
                }
            } else if (((int) (u / decadedU / 5)) != ((int) (oldU / decadedU / 5)) || Math.signum(u) != Math.signum(oldU)) {
                g2d.setColor(Color.gray);
                g2d.drawLine(rect.x + rect.width - mediumLineLength, rect.y + i, rect.x + rect.width, rect.y + i);
                if (i > (oldL + deltaL) && AClipPlotter.isSkalaValuesVisible()) {
                    float printedU = (float) (Math.round(u / decadedU) * decadedU);
                    g2d.setColor(color);
                    paintText(g2d, doubleToString(printedU) + cp.getPlotterYUnitName(), 10, rect.x + 30, rect.y + i, false);
                }
            } else if (((int) (u / decadedU)) != ((int) (oldU / decadedU)) || Math.signum(u) != Math.signum(oldU)) {
                g2d.setColor(Color.gray);
                g2d.drawLine(rect.x + rect.width - minorLineLength, rect.y + i, rect.x + rect.width, rect.y + i);
            }
            oldU = u;
        }
    }

    public void paintGrid(Graphics2D g2d, Rectangle rect, Color color) {
        if (getXLength() < .1) return;
        g2d.setClip(rect.x, rect.y, rect.width, rect.height);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
        g2d.setColor(color);
        AClipPlotter cp = ((AClip) model.getParent().getParent()).getPlotter();
        double oldU = cp.toPlotterXUnit(graphToSampleX(rect.x));
        int deltaP = 20;
        for (int i = 0; i < rect.width; i++) {
            double u = cp.toPlotterXUnit(graphToSampleX(rect.x + i));
            double deltaU = u - cp.toPlotterXUnit(graphToSampleX(rect.x + i + deltaP));
            double decadedU = Math.pow(10, Math.round(Math.log10(Math.abs(deltaU))));
            if (((int) (u / decadedU)) != ((int) (oldU / decadedU)) || Math.signum(u) != Math.signum(oldU)) {
                g2d.drawLine(rect.x + i, rect.y, rect.x + i, rect.y + rect.height);
            }
            oldU = u;
        }
        oldU = cp.toPlotterYUnit(graphToSampleY(rect.y));
        for (int i = 0; i < rect.height; i++) {
            float u = cp.toPlotterYUnit(graphToSampleY(rect.y + i));
            float deltaU = u - cp.toPlotterYUnit(graphToSampleY(rect.y + i + deltaP));
            double decadedU = Math.pow(10, Math.round(Math.log10(Math.abs(deltaU))));
            if (((int) (u / decadedU)) != ((int) (oldU / decadedU)) || Math.signum(u) != Math.signum(oldU)) {
                g2d.drawLine(rect.x, rect.y + i, rect.x + rect.width, rect.y + i);
            }
            oldU = u;
        }
    }
}
