package wand.genericChannel;

import java.awt.*;
import javax.swing.JPanel;
import wand.patterns.*;
import wand.patterns.filter.*;
import wand.ChannelFrame;

public class DisplayPanel extends JPanel {

    private PatternInterface p;

    private boolean antiAlias;

    private float alpha = 1.0f;

    public DisplayPanel(boolean setAntiAlias) {
        p = new TestPattern();
        antiAlias = setAntiAlias;
        this.setOpaque(true);
    }

    public void setPoints() {
        p.updatePanelLimits(this.getWidth(), this.getHeight());
        p.setPatternPoints();
        p.setColor(this);
        beatRefresh(0);
        repaint();
    }

    public void beatRefresh(int beat) {
        p.refresh(beat, this);
        repaint();
    }

    public void setPatternType(PatternInterface selectedPattern) {
        p = selectedPattern;
        setPoints();
    }

    public PatternInterface getPattern() {
        return p;
    }

    public void setAlpha(float a, boolean displayOnly) {
        alpha = a;
        refreshAlpha(displayOnly);
    }

    public void refreshAlpha(boolean displayOnly) {
        if (alpha >= 0.99) {
            alpha = 1.0f;
            if (ChannelFrame.channelGridPanel.getOutPutChannel().getChannelType().equals("clip")) {
                this.setOpaque(true);
                System.out.println("Opaque set to true");
            }
        }
        if (alpha < 0.99 && ChannelFrame.channelGridPanel.getOutPutChannel().getChannelType().equals("clip")) this.setOpaque(false);
        if (alpha > 0) setVisible(true);
        if (alpha <= 0 && !this.isOpaque()) {
            alpha = 0.0f;
            if (displayOnly) setVisible(false);
        }
        repaint();
    }

    public float getAlpha() {
        return alpha;
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, getAlpha());
        g2.setComposite(ac);
        g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        if (antiAlias) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        super.paintComponent(g2);
        p.drawIt(g2, this.getWidth(), this.getHeight(), this);
    }
}
