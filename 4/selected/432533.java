package de.michabrandt.timeview.renderer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import de.michabrandt.timeview.common.Dialog;
import de.michabrandt.timeview.common.DialogChannel;
import de.michabrandt.timeview.common.DialogTrack;
import de.michabrandt.timeview.common.DialogUnit;
import de.michabrandt.timeview.common.DialogUnitStyle;

public class G2DRenderer {

    private static final float BORDER_TOP = 5.0f;

    private static final float BORDER_BOTTOM = 5.0f;

    private static final float BORDER_LEFT = 5.0f;

    private static final float BORDER_RIGHT = 5.0f;

    private static final float BORDER_FG_UNIT_TOP = 5.0f;

    private static final float BORDER_FG_UNIT_MIDDLE = 5.0f;

    private static final float BORDER_FG_UNIT_BOTTOM = 5.0f;

    private static final float BORDER_FG_UNIT_LEFT = 5.0f;

    private static final float BORDER_FG_UNIT_RIGHT = 5.0f;

    private static final float FG_UNIT_BAR_HEIGHT = 10.0f;

    private static final float TRACK_SEPARATION = 10.0f;

    private static final float BORDER_BG_UNIT_TOP = 5.0f;

    private static final float BORDER_BG_UNIT_BOTTOM = 5.0f;

    private static final float BORDER_BG_UNIT_LEFT = 5.0f;

    private static final float BORDER_BG_UNIT_RIGHT = 5.0f;

    private static final Font HRULE_FONT = new Font("Sans-Serif", Font.PLAIN, 9);

    private static final Font DEFAULT_FONT = new Font("Dialog", Font.PLAIN, 12);

    private static final Stroke DEFAULT_STROKE = new BasicStroke(1.0f);

    public float totalWidth = 0.0f;

    public float totalHeight = 0.0f;

    public float legendWidth = 0.0f;

    protected boolean drawText = true;

    protected boolean drawLegend = true;

    protected float secW = 100.0f;

    protected BufferedImage img = null;

    protected Graphics2D g2d = null;

    private Dialog dialog = null;

    private FontRenderContext frc = null;

    private RoundRectangle2D.Float roundRect = new RoundRectangle2D.Float();

    private Rectangle2D.Float rect = new Rectangle2D.Float();

    private AttributedCharacterIterator styledTxt = null;

    private LineBreakMeasurer measurer = null;

    private PropertyChangeSupport listeners = new PropertyChangeSupport(this);

    public G2DRenderer(Dialog dialog) {
        this.dialog = dialog;
        img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        g2d = img.createGraphics();
        frc = g2d.getFontRenderContext();
        prepare_rendering();
        if (needsBufferedImage()) {
            img = new BufferedImage(1 + (int) this.totalWidth, 1 + (int) this.totalHeight, BufferedImage.TYPE_INT_RGB);
            g2d = img.createGraphics();
            frc = g2d.getFontRenderContext();
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        this.listeners.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        this.listeners.removePropertyChangeListener(l);
    }

    protected boolean needsBufferedImage() {
        return false;
    }

    /**
     * Render the chart and output to an output stream.
     *
     * @param dialog     	Dialog
     * @param os            the output stream to write to
     * @throws IOException  if an I/O error occurs
     */
    public void render() {
        if (dialog == null) return;
        if (g2d instanceof Graphics2D) {
        }
        this.frc = g2d.getFontRenderContext();
        g2d.setColor(Color.WHITE);
        g2d.fill(new Rectangle2D.Float(0, 0, totalWidth, totalHeight));
        Point2D.Float pos = new Point2D.Float(BORDER_TOP, BORDER_LEFT);
        for (DialogTrack track : dialog.getTracks()) {
            renderTrack(track, pos);
            pos.y += TRACK_SEPARATION;
        }
        if (!dialog.getTracks().isEmpty()) pos.y -= TRACK_SEPARATION;
        this.drawLegend = false;
    }

    private void prepare_rendering() {
        if (dialog == null) return;
        float dialogW = dialog.getDuration() * secW;
        float dialogH = 0;
        float trackH = 0.0f;
        float channelH = 0.0f;
        float unitW = 0.0f;
        float unitH = 0.0f;
        float textW = 0.0f;
        for (DialogTrack track : dialog.getTracks()) {
            trackH = 0.0f;
            if (track.hasFg()) {
                channelH = 0.0f;
                for (DialogUnit unit : track.getFg().getUnits()) {
                    unitH = BORDER_FG_UNIT_TOP + BORDER_FG_UNIT_MIDDLE + BORDER_FG_UNIT_BOTTOM + FG_UNIT_BAR_HEIGHT;
                    unitW = unit.getDuration() * secW;
                    textW = unitW - BORDER_FG_UNIT_LEFT - BORDER_BG_UNIT_RIGHT;
                    if (textW < 1) textW = 1;
                    unitH += getTextHeight(unit, textW);
                    channelH = Math.max(channelH, unitH);
                    unit.height = unitH;
                    unit.width = unitW;
                }
                track.getFg().height = channelH;
                trackH += channelH;
            }
            if (track.hasBg()) {
                channelH = 0.0f;
                for (DialogUnit unit : track.getBg().getUnits()) {
                    unitH = BORDER_BG_UNIT_TOP + BORDER_BG_UNIT_BOTTOM;
                    unitW = unit.getDuration() * secW;
                    textW = unitW - BORDER_BG_UNIT_LEFT - BORDER_BG_UNIT_RIGHT;
                    unitH += getTextHeight(unit, textW);
                    channelH = Math.max(channelH, unitH);
                    unit.height = unitH;
                    unit.width = unitW;
                }
                track.getBg().height = channelH;
                trackH += channelH;
            }
            dialogH += trackH + TRACK_SEPARATION;
        }
        if (!dialog.getTracks().isEmpty()) dialogH -= dialog.getTracks().size();
        dialogW += BORDER_LEFT + BORDER_RIGHT;
        dialogH += BORDER_TOP + BORDER_BOTTOM;
        this.totalWidth = dialogW;
        this.totalHeight = dialogH;
        this.legendWidth = 0.0f;
        for (DialogChannel channel : dialog.getChannels()) {
            String label = channel.getLabel();
            if (label != null) {
                Rectangle2D bounds = g2d.getFont().getStringBounds(label, frc);
                float width = (float) bounds.getWidth();
                this.legendWidth = Math.max(this.legendWidth, width);
            }
        }
        if (this.legendWidth != 0.0f) {
            this.legendWidth += BORDER_LEFT + BORDER_RIGHT + 2 * BORDER_LEFT;
        }
    }

    private float getTextHeight(DialogUnit unit, float width) {
        float h = 0.0f;
        this.styledTxt = unit.getAttributedString().getIterator();
        this.measurer = new LineBreakMeasurer(styledTxt, frc);
        int length = unit.getText().length();
        while (measurer.getPosition() < length) {
            TextLayout layout = measurer.nextLayout(width);
            h += layout.getAscent();
            h += layout.getDescent();
            h += layout.getLeading();
        }
        this.styledTxt = null;
        this.measurer = null;
        return h;
    }

    private void renderTrack(DialogTrack track, Point2D.Float pos) {
        float fg_h = 0.0f;
        float bg_h = 0.0f;
        if (track.hasFg()) fg_h = track.getFg().height;
        if (track.hasBg()) bg_h = track.getBg().height;
        if (track.hasBg()) for (DialogUnit unit : track.getBg().getUnits()) renderBgUnit(unit, pos, fg_h, bg_h);
        if (track.hasFg()) for (DialogUnit unit : track.getFg().getUnits()) renderFgUnit(unit, pos, fg_h, bg_h);
        pos.y += fg_h + bg_h;
    }

    private void renderFgUnit(DialogUnit unit, Point2D.Float pos, float fg_h, float bg_h) {
        float x = unit.getStart() * secW;
        float y = pos.y + BORDER_FG_UNIT_TOP;
        float w = unit.getDuration() * secW;
        Rectangle2D clip = g2d.getClipBounds();
        if (clip != null && !clip.intersects(x, y, w, fg_h)) {
            return;
        }
        this.rect.setRect(x, y, w, FG_UNIT_BAR_HEIGHT);
        g2d.setColor(unit.getStyle().getBgColor() != null ? unit.getStyle().getBgColor() : Color.gray);
        g2d.fill(this.rect);
        g2d.setColor(unit.getStyle().getBorderColor() != null ? unit.getStyle().getBorderColor() : Color.black);
        g2d.setStroke(unit.getStyle().getStroke() != null ? unit.getStyle().getStroke() : DEFAULT_STROKE);
        g2d.draw(this.rect);
        y += FG_UNIT_BAR_HEIGHT;
        y += BORDER_FG_UNIT_MIDDLE;
        g2d.setFont(unit.getStyle().getFont() != null ? unit.getStyle().getFont() : DEFAULT_FONT);
        if (this.drawText) {
            renderText(unit, x + BORDER_FG_UNIT_LEFT, y, w - BORDER_FG_UNIT_RIGHT - BORDER_BG_UNIT_LEFT);
        }
    }

    private void renderBgUnit(DialogUnit unit, Point2D.Float pos, float fg_h, float bg_h) {
        float x = unit.getStart() * secW;
        float y = pos.y;
        float w = unit.getDuration() * secW;
        this.roundRect.setRoundRect(x, y, w, bg_h + fg_h, 5, 5);
        if (unit.getStyle().getBgColor() != null) {
            g2d.setColor(unit.getStyle().getBgColor());
            g2d.fill(this.roundRect);
        }
        g2d.setStroke(unit.getStyle().getStroke() != null ? unit.getStyle().getStroke() : DEFAULT_STROKE);
        g2d.setColor(unit.getStyle().getBorderColor() != null ? unit.getStyle().getBorderColor() : Color.LIGHT_GRAY);
        g2d.draw(this.roundRect);
        if (this.drawText) renderText(unit, x + BORDER_BG_UNIT_LEFT, y + fg_h + BORDER_BG_UNIT_TOP, w - BORDER_BG_UNIT_RIGHT - BORDER_BG_UNIT_LEFT);
    }

    private void renderText(DialogUnit unit, float x, float y, float width) {
        this.styledTxt = unit.getAttributedString().getIterator();
        this.measurer = new LineBreakMeasurer(styledTxt, frc);
        g2d.setColor(unit.getStyle().getColor() != null ? unit.getStyle().getColor() : Color.black);
        int length = unit.getText().length();
        while (measurer.getPosition() < length) {
            TextLayout layout = measurer.nextLayout(width);
            y += layout.getAscent();
            int align = unit.getStyle().getTextAlign();
            switch(align) {
                case DialogUnitStyle.TEXT_ALIGN_RIGHT:
                    layout.draw(g2d, (float) (x + width - layout.getBounds().getWidth()), y);
                    break;
                case DialogUnitStyle.TEXT_ALIGN_CENTER:
                    float x2 = x;
                    x2 += 0.5 * (width - layout.getBounds().getWidth());
                    layout.draw(g2d, x2, y);
                    break;
                case DialogUnitStyle.TEXT_ALIGN_LEFT:
                default:
                    layout.draw(g2d, x, y);
                    break;
            }
            y += layout.getDescent();
            y += layout.getLeading();
        }
        styledTxt = null;
        measurer = null;
    }

    public void renderHorizontalLegend(Graphics2D g, float scale) {
        Rectangle clip = g.getClipBounds();
        g.setColor(Color.white);
        g.fillRect(clip.x, clip.y, clip.width, clip.height);
        g.setColor(Color.black);
        float from = clip.x / (secW * scale);
        float to = (clip.x + clip.width) / (secW * scale);
        for (float f = from; f <= to; f += 1) {
            int s = (int) (f);
            int x = (int) (s * secW * scale);
            if (s % 2 == 0) g.drawLine(x, 0, x, 15); else if (s % 1 == 0) g.drawLine(x, 0, x, 5);
            if (s % 2 == 0) {
                g.setFont(HRULE_FONT);
                g.drawString(Integer.toString(s) + "s", x + 2, 20);
            }
        }
    }

    public void renderVerticalLegend(Graphics2D g) {
        if (this.dialog == null) return;
        g.setColor(Color.black);
        Point2D.Float pos = new Point2D.Float(BORDER_TOP, BORDER_LEFT);
        for (DialogTrack track : this.dialog.getTracks()) {
            pos.y += TRACK_SEPARATION;
            float fg_h = 0.0f;
            float bg_h = 0.0f;
            if (track.hasFg()) fg_h = track.getFg().height;
            if (track.hasBg()) bg_h = track.getBg().height;
            if (track.hasFg()) {
                String label = track.getFg().getLabel();
                if (label != null) {
                    float y = pos.y + BORDER_FG_UNIT_TOP + BORDER_FG_UNIT_MIDDLE + FG_UNIT_BAR_HEIGHT;
                    g.drawString(label, BORDER_LEFT, y);
                }
            }
            if (track.hasBg()) {
                String label = track.getBg().getLabel();
                if (label != null) {
                    float y = pos.y + fg_h + BORDER_BG_UNIT_TOP;
                    g.drawString(label, (int) BORDER_LEFT, y);
                }
            }
            pos.y += fg_h + bg_h;
        }
    }
}
