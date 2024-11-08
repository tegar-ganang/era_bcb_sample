package wand.channelControl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JPanel;
import javax.swing.JLabel;

public class IndicatorPanel extends JPanel {

    public int channelID;

    private int x;

    private int y;

    private Color c = Color.black;

    BasicStroke stroke = new BasicStroke(2.0f);

    private Rectangle2D.Double beatBox;

    private Rectangle2D.Double phraseBox;

    private Line2D.Double line;

    private Point2D.Double b0a;

    private Point2D.Double b0b;

    private Point2D.Double b1a;

    private Point2D.Double b1b;

    private Point2D.Double b2a;

    private Point2D.Double b2b;

    private Point2D.Double b3a;

    private Point2D.Double b3b;

    private Double[] phrasePoints = new Double[16];

    private boolean quadLights = true;

    private JLabel info = new JLabel("   Click to enable quad lights   ");

    public IndicatorPanel() {
        setLayout(new GridLayout(1, 1));
        setBackground(Color.white);
        line = new Line2D.Double();
        beatBox = new Rectangle2D.Double();
        phraseBox = new Rectangle2D.Double();
        addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                flipQuadLights();
            }
        });
        flipQuadLights();
        info.setFont(new java.awt.Font("Tahoma", 0, 10));
        info.setForeground(new java.awt.Color(153, 153, 153));
        info.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        add(info);
    }

    public void setChannelID(int chID) {
        channelID = chID;
    }

    public int getChannelID() {
        return channelID;
    }

    public void flipQuadLights() {
        quadLights = !quadLights;
        if (quadLights) {
            info.setVisible(false);
        } else {
            info.setVisible(true);
        }
    }

    public void setPoints() {
        x = this.getWidth();
        y = this.getHeight();
        b0a = new Point2D.Double(0, y);
        b0b = new Point2D.Double(x / 4, y / 6);
        b1a = new Point2D.Double(x / 4, y);
        b1b = new Point2D.Double(x / 2, y / 6);
        b2a = new Point2D.Double(x / 2, y);
        b2b = new Point2D.Double(3 * x / 4, y / 6);
        b3a = new Point2D.Double(3 * x / 4, y);
        b3b = new Point2D.Double(x, y / 6);
        for (int i = 0; i < 16; i++) {
            phrasePoints[i] = new Double(((i + 1) * x) / 16);
        }
        line.setLine(0, y / 6, x, y / 6);
    }

    public void beatRefresh(int beat) {
        phraseBox.setFrameFromDiagonal(phrasePoints[beat % 16], 0, 0, y / 6);
        if (quadLights) {
            if (beat % 4 == 0) {
                beatBox.setFrameFromDiagonal(b0a, b0b);
                c = Color.green;
            }
            if (beat % 4 == 1) {
                beatBox.setFrameFromDiagonal(b1a, b1b);
                c = Color.red;
            }
            if (beat % 4 == 2) {
                beatBox.setFrameFromDiagonal(b2a, b2b);
                c = Color.red;
            }
            if (beat % 4 == 3) {
                beatBox.setFrameFromDiagonal(b3a, b3b);
                c = Color.red;
            }
        }
        repaint();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        if (quadLights) {
            g2.setColor(c);
            g2.fill(beatBox);
            g2.setColor(Color.black);
            g2.setStroke(stroke);
            g2.draw(line);
        }
        g2.fill(phraseBox);
    }
}
