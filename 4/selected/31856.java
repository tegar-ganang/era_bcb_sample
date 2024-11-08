package org.xbup.library.audio.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.UIManager;
import org.xbup.library.audio.wave.XBWave;

/**
 * Simple panel audio wave.
 *
 * @version 0.1.0 2011/03/19
 * @author SoundComp Project (http://soundcomp.sf.net)
 */
public class XBWavePanel extends JPanel implements MouseListener, MouseMotionListener {

    private XBWave wave;

    private int cursorPosition;

    private SelectionRange selection;

    private DrawMode drawMode;

    private ToolMode toolMode;

    private Color selectionColor;

    private List<PositionSeekListener> positionSeekListeners;

    public XBWavePanel() {
        super();
        setBackground(Color.WHITE);
        repaint();
        setOpaque(true);
        drawMode = DrawMode.LINE_MODE;
        toolMode = ToolMode.SELECTION;
        cursorPosition = 0;
        selectionColor = UIManager.getColor("TextArea.selectionBackground");
        if (selectionColor == null) selectionColor = Color.LIGHT_GRAY;
        addMouseListener(this);
        addMouseMotionListener(this);
        positionSeekListeners = new ArrayList<PositionSeekListener>();
    }

    @Override
    public void paintComponent(Graphics g) {
        Rectangle clipBounds = g.getClipBounds();
        int rangeStart = 0;
        int rangeEnd;
        int[] zones = { getCursorPosition() - 1, getCursorPosition() + 2, -1, -1 };
        if (selection != null) {
            zones[2] = selection.begin;
            zones[3] = selection.end + 1;
        }
        while (rangeStart < clipBounds.getWidth()) {
            int rangePosition = (clipBounds.x + rangeStart);
            rangeEnd = clipBounds.width;
            for (int i = 0; i < zones.length; i++) {
                int zonePosition = zones[i];
                if ((zonePosition > rangePosition) && (rangeEnd > zonePosition - clipBounds.x)) {
                    rangeEnd = zonePosition - clipBounds.x;
                }
            }
            if ((rangePosition >= zones[0]) && (rangePosition < zones[1])) {
                g.setColor(Color.BLACK);
            } else if ((rangePosition >= zones[2]) && (rangePosition < zones[3])) {
                g.setColor(selectionColor);
            } else g.setColor(getBackground());
            g.fillRect(rangePosition, clipBounds.y, rangeEnd + clipBounds.x - 1, clipBounds.height);
            rangeStart = rangeEnd;
        }
        int stopPos = clipBounds.width + clipBounds.x;
        if (wave != null) {
            g.setColor(Color.BLACK);
            int channelsCount = wave.getAudioFormat().getChannels();
            int[] prev = { -1, -1 };
            if (stopPos >= wave.getLengthInTicks()) stopPos = wave.getLengthInTicks() - 1;
            for (int pos = clipBounds.x - 1; pos <= stopPos; pos++) {
                for (int channel = 0; channel < channelsCount; channel++) {
                    int pomPos = pos;
                    if (pomPos < 0) pomPos = 0;
                    int value = wave.getRatioValue(pomPos, channel, getHeight() / channelsCount) + (channel * getHeight()) / channelsCount;
                    int middle = (getHeight() + (2 * channel * getHeight())) / (2 * channelsCount);
                    switch(drawMode) {
                        case DOTS_MODE:
                            {
                                g.drawLine(pos, value, pos, value);
                                break;
                            }
                        case LINE_MODE:
                            {
                                if (prev[channel] >= 0) {
                                    g.drawLine(pos - 1, prev[channel], pos, value);
                                }
                                prev[channel] = value;
                                break;
                            }
                        case INTEGRAL_MODE:
                            {
                                g.drawLine(pos, value, pos, middle);
                                break;
                            }
                    }
                }
            }
        }
    }

    /**
     * @return the wave
     */
    public XBWave getWave() {
        return wave;
    }

    /**
     * @param wave the wave to set
     */
    public void setWave(XBWave wave) {
        this.wave = wave;
        setPreferredSize(new Dimension(wave.getLengthInTicks(), 0));
        Rectangle rectangle = getBounds();
        rectangle.width = wave.getLengthInTicks();
        setBounds(rectangle);
        repaint();
    }

    /**
     * @return the drawMode
     */
    public DrawMode getDrawMode() {
        return drawMode;
    }

    /**
     * @param drawMode the drawMode to set
     */
    public void setDrawMode(DrawMode drawMode) {
        this.drawMode = drawMode;
        repaint();
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
    }

    /**
     * @return the position
     */
    public int getCursorPosition() {
        return cursorPosition;
    }

    /**
     * @param position the position to set
     */
    public void setCursorPosition(int position) {
        this.cursorPosition = position;
    }

    public void seekPosition(int position) {
        setCursorPosition(position);
        for (int i = 0; i < positionSeekListeners.size(); i++) {
            positionSeekListeners.get(i).positionSeeked(position);
        }
    }

    public void copy() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void cut() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void paste() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * @return the toolMode
     */
    public ToolMode getToolMode() {
        return toolMode;
    }

    /**
     * @param toolMode the toolMode to set
     */
    public void setToolMode(ToolMode toolMode) {
        this.toolMode = toolMode;
    }

    public enum DrawMode {

        DOTS_MODE, LINE_MODE, INTEGRAL_MODE
    }

    public enum ToolMode {

        SELECTION, PENCIL
    }

    public boolean inSelection(int pos) {
        if (selection == null) return false;
        return (pos >= selection.getBegin()) && (pos <= selection.getEnd());
    }

    public void selectAll() {
        if (wave != null) {
            selection = new SelectionRange(0, wave.getLengthInTicks());
            repaint();
        }
    }

    public void selectNone() {
        selection = null;
        repaint();
    }

    boolean mouseDown = false;

    int mousePressPosition;

    @Override
    public void mousePressed(MouseEvent me) {
        if (me.getButton() == MouseEvent.BUTTON1) {
            if (toolMode == ToolMode.SELECTION) {
                selectNone();
                int oldPosition = getCursorPosition();
                seekPosition(me.getX());
                repaint(oldPosition - 1, 0, 3, getHeight());
                repaint(getCursorPosition() - 1, 0, 3, getHeight());
            } else {
                int position = me.getX();
                int channel = 0;
                if (me.getY() > (getHeight() / 2)) channel = 1;
                wave.setRatioValue(position, me.getY() - ((getHeight() / 2) * channel), channel, getHeight() / 2);
                repaint(position, 0, position, getHeight());
            }
            mousePressPosition = getCursorPosition();
        }
        mouseDown = true;
    }

    @Override
    public void mouseReleased(MouseEvent me) {
        mouseDown = false;
    }

    @Override
    public void mouseDragged(MouseEvent me) {
        if (mouseDown) {
            int oldPosition = getCursorPosition();
            setCursorPosition(me.getX());
            repaint(getCursorPosition() - 1, 0, 3, getHeight());
            repaint(oldPosition - 1, 0, 3, getHeight());
            if ((selection != null) || (getCursorPosition() > mousePressPosition + 3) || (getCursorPosition() < mousePressPosition - 3)) {
                if (getCursorPosition() > mousePressPosition) {
                    selection = new SelectionRange(mousePressPosition, getCursorPosition());
                } else {
                    selection = new SelectionRange(getCursorPosition(), mousePressPosition);
                }
                repaint(selection.begin, 0, selection.end - selection.begin, getHeight());
            }
        }
    }

    public class SelectionRange {

        private int begin;

        private int end;

        public SelectionRange() {
            begin = 0;
            end = 0;
        }

        public SelectionRange(int begin, int end) {
            this.begin = begin;
            this.end = end;
        }

        /**
         * @return the begin
         */
        public int getBegin() {
            return begin;
        }

        /**
         * @param begin the begin to set
         */
        public void setBegin(int begin) {
            this.begin = begin;
        }

        /**
         * @return the end
         */
        public int getEnd() {
            return end;
        }

        /**
         * @param end the end to set
         */
        public void setEnd(int end) {
            this.end = end;
        }
    }

    public interface PositionSeekListener extends EventListener {

        public void positionSeeked(int position);
    }

    public void addPositionSeekListener(PositionSeekListener listener) {
        positionSeekListeners.add(listener);
    }

    public void removePositionSeekListener(PositionSeekListener listener) {
        positionSeekListeners.remove(listener);
    }
}
