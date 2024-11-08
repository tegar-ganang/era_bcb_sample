package net.dieslunae.jgraphite.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JComponent;
import net.dieslunae.jgraphite.Settings;
import net.dieslunae.jgraphite.ToneCurve;

@SuppressWarnings("serial")
public class Histogram extends JComponent implements MouseListener, MouseMotionListener {

    private BufferedImage image = null;

    private int[] histoData = new int[256];

    private Point[] curve = null;

    private final int BORDER = 2;

    private int mouseCurvePointIndex = -1;

    private List<ToneCurveChangeListener> curveChangeListeners = new ArrayList<ToneCurveChangeListener>();

    public Histogram() {
        setBackground(Color.white);
        setForeground(Color.black);
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        calcHistogram();
    }

    public void setToneCurve(ToneCurve toneCurve) {
        if (toneCurve == null) {
            curve = null;
            return;
        }
        curve = new Point[toneCurve.getPointsCount()];
        for (int p = 0; p < toneCurve.getPointsCount(); p++) {
            curve[p] = toScreenPoint(toneCurve.getPoint(p));
        }
    }

    public void addToneCurveChangeListener(ToneCurveChangeListener l) {
        curveChangeListeners.add(l);
    }

    private void notifyCurveChange() {
        if (curveChangeListeners.size() == 0) return;
        Point[] newCurve = new Point[curve.length];
        for (int p = 0; p < curve.length; p++) {
            newCurve[p] = toCurvePoint(curve[p]);
        }
        for (ToneCurveChangeListener l : curveChangeListeners) {
            l.toneCurveChanged(new ToneCurve(Settings.TXT_TC_CUSTOM, newCurve));
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        Dimension dim = getSize();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, dim.width - 1, dim.height - 1);
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, dim.width - 1, dim.height - 1);
        if (image != null) {
            int max = 0;
            for (int v : histoData) {
                if (v > max) max = v;
            }
            float dx = (float) (dim.width - 2 * BORDER) / 256;
            float dy = (float) (dim.height - 2 * BORDER) / max;
            int barWidth = (int) dx < 1 ? 1 : (int) dx;
            g.setColor(Color.GRAY);
            for (int i = 0; i < 256; i++) {
                int x = (int) (i * dx);
                int barHeight = (int) (histoData[i] * dy);
                g.fillRect(x + BORDER, dim.height - barHeight - BORDER, barWidth, barHeight);
            }
        }
        if (curve == null) return;
        g.setColor(Color.BLUE);
        for (int p = 0; p < curve.length; p++) {
            g.drawRect(curve[p].x - 1, curve[p].y - 1, 2, 2);
        }
        for (int p = 0; p < curve.length - 1; p++) {
            g.drawLine(curve[p].x, curve[p].y, curve[p + 1].x, curve[p + 1].y);
        }
    }

    private Point toScreenPoint(Point curvePoint) {
        int px = (int) ((double) getWidth() - 2 * BORDER) * curvePoint.x / 255 + BORDER;
        int py = getHeight() - (int) (((double) getHeight() - 2 * BORDER) * curvePoint.y / 255) - BORDER;
        return new Point(px, py);
    }

    private Point toCurvePoint(Point screenPoint) {
        int cx = (int) ((double) screenPoint.x - BORDER) * 255 / (getWidth() - 2 * BORDER);
        cx++;
        int cy = 255 - (int) ((double) screenPoint.y - BORDER) * 255 / (getHeight() - 2 * BORDER);
        if (cx < 0) cx = 0;
        if (cy < 0) cy = 0;
        if (cx > 255) cx = 255;
        if (cy > 255) cy = 255;
        return new Point(cx, cy);
    }

    /**
	 * @param p Mouse point
	 * @return index in curve points or -1
	 */
    private int findPointUnderMouse(Point p) {
        if (curve == null) return -1;
        int result = -1;
        for (int i = 0; i < curve.length; i++) {
            if (p.x - 2 <= curve[i].x && p.x + 2 >= curve[i].x && p.y - 2 <= curve[i].y && p.y + 2 >= curve[i].y) {
                return i;
            }
        }
        return result;
    }

    private void calcHistogram() {
        if (image == null) return;
        Arrays.fill(histoData, 0);
        WritableRaster imgRast = image.getRaster();
        int width = image.getWidth();
        int height = image.getHeight();
        int bands = imgRast.getNumBands();
        if (bands != 1 && bands != 3) {
            System.out.println("Wrong no of bands: " + bands);
            return;
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = 0;
                if (bands == 1) {
                    value = imgRast.getSample(x, y, 0);
                } else {
                    int r = imgRast.getSample(x, y, 0);
                    int g = imgRast.getSample(x, y, 1);
                    int b = imgRast.getSample(x, y, 2);
                    value = (r + g + b) / 3;
                }
                if (value < 0) value = 0;
                if (value > 255) value = 255;
                histoData[value]++;
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON2) return;
        mouseCurvePointIndex = findPointUnderMouse(e.getPoint());
        if (e.getButton() == MouseEvent.BUTTON1 && mouseCurvePointIndex == -1) {
            Point mp = e.getPoint();
            for (int p = 0; p < curve.length - 1; p++) {
                if (mp.x > curve[p].x && mp.x < curve[p + 1].x) {
                    Point[] newCurve = new Point[curve.length + 1];
                    int i = 0;
                    for (; i <= p; i++) newCurve[i] = curve[i];
                    newCurve[i++] = mp;
                    for (; i < newCurve.length; i++) newCurve[i] = curve[i - 1];
                    curve = newCurve;
                    repaint();
                    mouseCurvePointIndex = p + 1;
                    break;
                }
            }
        } else if (e.getButton() == MouseEvent.BUTTON3 && mouseCurvePointIndex > 0 && mouseCurvePointIndex < curve.length - 1) {
            Point[] newCurve = new Point[curve.length - 1];
            int i = 0;
            for (; i < mouseCurvePointIndex; i++) newCurve[i] = curve[i];
            for (; i < newCurve.length; i++) newCurve[i] = curve[i + 1];
            mouseCurvePointIndex = -1;
            curve = newCurve;
            repaint();
            notifyCurveChange();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (mouseCurvePointIndex > 0 && mouseCurvePointIndex < curve.length - 1) {
            notifyCurveChange();
        }
        mouseCurvePointIndex = -1;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (curve != null && mouseCurvePointIndex > 0 && mouseCurvePointIndex < curve.length - 1) {
            Point p = e.getPoint();
            if (p.x <= curve[mouseCurvePointIndex - 1].x) p.x = curve[mouseCurvePointIndex - 1].x + 1;
            if (p.x >= curve[mouseCurvePointIndex + 1].x) p.x = curve[mouseCurvePointIndex + 1].x - 1;
            if (p.y > getHeight() - BORDER) p.y = getHeight() - BORDER;
            if (p.y < BORDER) p.y = BORDER;
            curve[mouseCurvePointIndex] = p;
            repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }
}
