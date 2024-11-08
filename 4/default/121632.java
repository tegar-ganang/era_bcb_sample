import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.awt.Point;
import javax.swing.JPanel;

public class SingleWaveformPanel extends JPanel {

    private static final long serialVersionUID = -179386293948136136L;

    private Color BACKGROUND_COLOR = Color.lightGray;

    private Color REFERENCE_LINE_COLOR = Color.black;

    private Color WAVE = Color.blue;

    private Color HIGHLIGHT = Color.green;

    private Color HIGHLIGHT_BCKGRD = Color.darkGray;

    private AudioArray aa;

    private int channelIndex, lineHeight, pixelNumber;

    private boolean drawCursor, highlighted, drawPoints;

    private Point pressPoint, dragPoint;

    private double zoomScale = 1.0;

    private int increment, inc_helper;

    public SingleWaveformPanel(AudioArray aa, int channelIndex) {
        this.aa = aa;
        this.channelIndex = channelIndex;
        this.setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);
        drawCursor = false;
        highlighted = false;
        drawPoints = false;
        addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent m) {
                pressPoint = m.getPoint();
                highlighted = false;
                repaint();
            }
        });
        addMouseListener(new MouseAdapter() {

            public void mouseReleased(MouseEvent m) {
                repaint();
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {

            public void mouseDragged(MouseEvent m) {
                dragPoint = m.getPoint();
                highlighted = true;
                repaint();
            }
        });
        addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent m) {
                drawCursor(m.getX());
            }
        });
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        lineHeight = getHeight() / 2;
        g.setColor(REFERENCE_LINE_COLOR);
        g.drawLine(0, lineHeight, (int) getWidth(), lineHeight);
        drawWaveform(g, aa.getChannel(channelIndex));
    }

    protected void drawWaveform(Graphics g, short[] samples) {
        if (samples == null) {
            return;
        }
        int X = 0;
        int oldY = getHeight() - ((getHeight() * (samples[0] + 32768)) / 65536);
        int newY;
        increment = (int) (aa.getChannelLength() / getWidth());
        inc_helper = (int) (increment / zoomScale);
        if (increment < 1) drawPoints = true; else drawPoints = false;
        for (int i = 1; i < samples.length; i++) {
            newY = getHeight() - ((getHeight() * (samples[i] + 32768)) / 65536);
            if (i == increment) {
                increment += (inc_helper / zoomScale);
                if (highlighted) {
                    if ((X >= pressPoint.x && X <= dragPoint.x) || (X >= dragPoint.x && X <= pressPoint.x)) {
                        g.setColor(HIGHLIGHT);
                        g.drawLine(X, oldY, X++, newY);
                        g.setColor(HIGHLIGHT_BCKGRD);
                        if (oldY > newY) {
                            g.drawLine(X, oldY, X, 0);
                            g.drawLine(X, newY, X, getHeight());
                        } else {
                            g.drawLine(X, newY, X, 0);
                            g.drawLine(X, oldY, X, getHeight());
                        }
                    } else {
                        g.setColor(WAVE);
                        g.drawLine(X, oldY, X++, newY);
                    }
                } else {
                    g.setColor(WAVE);
                    g.drawLine(X, oldY, X++, newY);
                    if (drawCursor) {
                        if (X == pixelNumber) {
                            g.setColor(Color.black);
                            g.drawLine(X, 0, X, getHeight());
                        }
                    }
                }
                oldY = newY;
            }
        }
    }

    public void drawCursor(int pn) {
        drawCursor = true;
        pixelNumber = pn;
        repaint();
    }

    public void stopCursor() {
        drawCursor = false;
        repaint();
    }

    public void zoomIn() {
        zoomScale *= 1.25;
        repaint();
    }

    public void zoomOut() {
        zoomScale *= 0.8;
        repaint();
    }
}
