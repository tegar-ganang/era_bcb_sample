package net.sourceforge.olduvai.lrac.util.ivtk;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Robot;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.sourceforge.olduvai.lrac.drawer.structure.strips.DataAggregator;
import net.sourceforge.olduvai.lrac.ui.SelectionConstraints;

/**
 * Implements a Swing-based Range slider, which allows the user to enter a
 * range-based value.
 * 
 * Modified by Peter McLachlan <spark343@cs.ubc.ca> for LiveRAC
 * 
 * @author Ben B. Bederson, Jon Meyer and Jean-Daniel Fekete
 * @version $Revision: 1051 $
 */
public class DoubleRangeSlider extends JComponent implements MouseListener, MouseMotionListener, ChangeListener {

    static final long MILLISECOND = 1;

    static final long SECOND = 1000;

    static final long MINUTE = SECOND * 60;

    static final long HOUR = MINUTE * 60;

    static final long DAY = HOUR * 24;

    static final long WEEK = DAY * 7;

    final int X = 0;

    final int Y = 1;

    static final SimpleDateFormat SHORTFORMAT = new SimpleDateFormat("HH:mm");

    static final SimpleDateFormat MEDIUMFORMAT = new SimpleDateFormat("dd MMM HH:mm");

    static final SimpleDateFormat LONGFORMAT = new SimpleDateFormat("dd MMM");

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private static int PICK_WIDTH = 3;

    static int SZ = 6;

    static final int PICK_NONE = 0;

    static final int PICK_MIN = 1;

    static final int PICK_MAX = 2;

    static final int PICK_MID = 4;

    private DoubleBoundedRangeModel model;

    private boolean enabled = false;

    int[] xPts = new int[7];

    int[] yPts = new int[7];

    int pick;

    double pickOffset;

    double mouseX;

    double scaleRatio = 1;

    int[] densityArray;

    int totalAlarms;

    int[] mouseDownStart = { 0, 0 };

    int[] mouseScrollStart = { 0, 0 };

    /**
     * Store the previous mouse coordinates so we can detect direction changes
     */
    int[] prevMouse = { 0, 0 };

    int[] mouseNow = { 0, 0 };

    /**
     * Flag when the 'bounds' of the range have changed during a mid-slider-drag operation 
     */
    boolean scrolling = false;

    /**
     * Flag to indicate whether scrolling took place between click-and-release
     */
    boolean hasScrolled = false;

    /**
     * How close to the min / max end zone do we have to be before 
     * we scale the range of the slider? 
     */
    double growRangePercent = .05;

    double shrinkRangePercent = .25;

    /**
     * The percentage to modify the visible range in the direction being moved
     */
    double resizeRangeByPercent = .25;

    /**
     * Percentage of the entire range to scroll by when dragging the double edged slider left or right
     */
    double scrollTickPercent = .25;

    static final String blueTickPath = "/net/sourceforge/olduvai/lrac/resources/bluetick.png";

    static BufferedImage blueTick;

    SelectionConstraints c = new SelectionConstraints();

    /**
     * A local space X coordinate for the 'handle' mark when the user has grabbed the midpoint on the bar,
     * showing where the user has grabbed it.  This can differ from the mouseX position when the midpoint is
     * being dragged and scrolling is taking place.  The slider can't slide past the boundaries of the widget
     * but the mouse can, allowing the mouse to leave behind the handle.
     * 
     *   -1 if invalid
     */
    int localHandleX;

    Robot robot;

    Font labelFont;

    private int labelPad = 35;

    private boolean mouseInComponent;

    private Timer timer = new Timer("Range slider scroll timer", false);

    private TimerTask updateTask;

    /**
     * Constructs a new range slider.
     * 
     * @param minimum -
     *            the minimum value of the range.
     * @param maximum -
     *            the maximum value of the range.
     * @param lowValue -
     *            the current low value shown by the range slider's bar.
     * @param highValue -
     *            the current high value shown by the range slider's bar.
     */
    public DoubleRangeSlider(double lowValue, double highValue) {
        final double extent = highValue - lowValue;
        commonConstructor(new DefaultDoubleBoundedRangeModel(lowValue, extent, lowValue - (extent * resizeRangeByPercent), highValue + (extent * resizeRangeByPercent)));
    }

    /**
     * Creates a new RangeSlider object.
     * 
     * @param model
     *            the DoubleBoundedRangeModel
     */
    public DoubleRangeSlider(DoubleBoundedRangeModel model) {
        commonConstructor(model);
    }

    private void commonConstructor(DoubleBoundedRangeModel model) {
        this.model = model;
        model.addChangeListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        loadImage();
        PICK_WIDTH = blueTick.getWidth(null);
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
        String family = "Sans";
        int style = Font.PLAIN;
        int size = 10;
        labelFont = new Font(family, style, size);
    }

    private void loadImage() {
        if (blueTick != null) return;
        InputStream s = getClass().getResourceAsStream(blueTickPath);
        try {
            blueTick = ImageIO.read(s);
        } catch (IOException e) {
            System.err.println("Error loading image: " + blueTickPath);
        }
    }

    /**
     * Returns the current "low" value shown by the range slider's bar. The low
     * value meets the constraint minimum &lt;= lowValue &lt;= highValue &lt;=
     * maximum.
     * 
     * @return the current "low" value shown by the range slider's bar.
     */
    public double getLowValue() {
        return model.getValue();
    }

    /**
     * Returns the current "high" value shown by the range slider's bar. The
     * high value meets the constraint minimum &lt;= lowValue &lt;= highValue
     * &lt;= maximum.
     * 
     * @return the current "high" value shown by the range slider's bar.
     */
    public double getHighValue() {
        return model.getValue() + model.getExtent();
    }

    /**
     * Returns the minimum possible value for either the low value or the high
     * value.
     * 
     * @return the minimum possible value for either the low value or the high
     *         value.
     */
    public double getMinimum() {
        return model.getMinimum();
    }

    /**
     * Returns the maximum possible value for either the low value or the high
     * value.
     * 
     * @return the maximum possible value for either the low value or the high
     *         value.
     */
    public double getMaximum() {
        return model.getMaximum();
    }

    /**
     * Returns true if the specified value is within the range indicated by this
     * range slider, i&dot;e&dot; lowValue 1 &lt;= v &lt;= highValue.
     * 
     * @param v
     *            value
     * 
     * @return true if the specified value is within the range indicated by this
     *         range slider.
     */
    public boolean contains(double v) {
        return (v >= getLowValue() && v <= getHighValue());
    }

    /**
     * Sets the low value shown by this range slider. This causes the range
     * slider to be repainted and a RangeEvent to be fired.
     * 
     * @param lowValue
     *            the low value shown by this range slider
     */
    public void setLowValue(double lowValue) {
        final double totalExtent = getMaximum() - getMinimum();
        final double minPosNorm = (getLowValue() - getMinimum()) / totalExtent;
        final boolean movingRight = getLowValue() < lowValue;
        if (lowValue < getMinimum()) {
            final double lowToHighExtent = getHighValue() - lowValue;
            model.setMinimum(lowValue - (lowToHighExtent * resizeRangeByPercent));
        } else if (minPosNorm < growRangePercent && !movingRight) {
            model.setMinimum(getMinimum() - (totalExtent * resizeRangeByPercent));
            if (mouseInComponent) {
                Point newMousePos = new Point(toScreenX(lowValue), 0);
                SwingUtilities.convertPointToScreen(newMousePos, this);
                newMousePos.y = mouseNow[Y];
                robot.mouseMove(newMousePos.x, newMousePos.y);
            }
        } else if (shouldShrink(lowValue, getHighValue())) {
            shrinkFrame(lowValue, getHighValue());
            if (mouseInComponent) {
                System.out.println("Reset low value 5");
                Point newMousePos = new Point(toScreenX(lowValue), 0);
                SwingUtilities.convertPointToScreen(newMousePos, this);
                newMousePos.y = mouseNow[Y];
                robot.mouseMove(newMousePos.x, newMousePos.y);
            }
        }
        final double extent = getHighValue() - lowValue;
        model.setRangeProperties(lowValue, extent, getMinimum(), getMaximum(), true);
    }

    public void setAndFrameValues(double lowValue, double highValue) {
        shrinkFrame(lowValue, highValue);
        final double extent = highValue - lowValue;
        model.setRangeProperties(lowValue, extent, getMinimum(), getMaximum(), true);
    }

    /**
     * Sets the high value shown by this range slider. This causes the range
     * slider to be repainted and a RangeEvent to be fired.
     * 
     * @param highValue
     *            the high value shown by this range slider
     */
    public void setHighValue(double highValue) {
        final double totalExtent = getMaximum() - getMinimum();
        final double maxPosNorm = (getHighValue() - getMinimum()) / totalExtent;
        final boolean movingRight = getHighValue() < highValue;
        if (highValue > getMaximum()) {
            final double lowToHighExtent = highValue - getLowValue();
            model.setMaximum(highValue + (lowToHighExtent * resizeRangeByPercent));
        } else if (maxPosNorm > 1d - growRangePercent && movingRight) {
            model.setMaximum(getMaximum() + (totalExtent * resizeRangeByPercent));
            if (mouseInComponent) {
                Point newMousePos = new Point(toScreenX(highValue), 0);
                SwingUtilities.convertPointToScreen(newMousePos, this);
                newMousePos.y = mouseNow[Y];
                robot.mouseMove(newMousePos.x, newMousePos.y);
            }
        } else if (shouldShrink(getLowValue(), highValue)) {
            shrinkFrame(getLowValue(), highValue);
            if (mouseInComponent) {
                Point newMousePos = new Point(toScreenX(highValue), 0);
                SwingUtilities.convertPointToScreen(newMousePos, this);
                newMousePos.y = mouseNow[Y];
                robot.mouseMove(newMousePos.x, newMousePos.y);
            }
        }
        model.setExtent(highValue - getLowValue());
    }

    /**
     * Tests whether we meet the conditions necessary for shrinkage. 
     * (how cold is the water?)
     * 
     * @param lowValue
     * @param highValue
     * @return
     */
    private boolean shouldShrink(double lowValue, double highValue) {
        final double curMinimum = getMinimum();
        final double curMaximum = getMaximum();
        final double curTotalExtent = curMaximum - curMinimum;
        final double selectedExtent = highValue - lowValue;
        if (selectedExtent / curTotalExtent < shrinkRangePercent && selectedExtent > HOUR) {
            return true;
        }
        return false;
    }

    /**
     * Shrinks in both sides of the displayed time range to 'frame'
     * the selected data values evenly on the left & right. 
     * 
     * @param lowValue
     * @param highValue
     */
    private void shrinkFrame(double lowValue, double highValue) {
        final double selectedExtent = highValue - lowValue;
        final double resizeBy = selectedExtent * resizeRangeByPercent;
        model.setMinimum(lowValue - resizeBy);
        model.setMaximum(highValue + resizeBy);
    }

    Rectangle getInBounds() {
        Dimension sz = getSize();
        Insets insets = getInsets();
        return new Rectangle(insets.left, insets.top, sz.width - insets.left - insets.right, sz.height - insets.top - insets.bottom);
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g.setColor(Color.white);
        Rectangle rect = getInBounds();
        final Color veryLightBlue = Color.getHSBColor(230f / 360f, 9f / 100f, 1f);
        g.setColor(veryLightBlue);
        for (int i = 0; i < rect.height; i++) {
            g.fillRect(rect.x, i, rect.width, 1);
            if (g.getColor() == veryLightBlue) g.setColor(Color.white); else g.setColor(veryLightBlue);
        }
        RoundRectangle2D outside = new RoundRectangle2D.Float(rect.x + 1, rect.y + 1, rect.width - 2, rect.height - 2, 8, 8);
        g.setColor(Color.gray);
        g2.draw(outside);
        final int tickWidth = blueTick.getWidth(null);
        final int halfTickWidth = (tickWidth / 2);
        final int halfTickHeight = (blueTick.getHeight(null) / 2);
        final int quarterBoxHeight = rect.height / 4 + 3;
        final int tickY = quarterBoxHeight - halfTickHeight;
        int minX = toScreenX(getLowValue());
        int maxX = toScreenX(getHighValue());
        g.setColor(Color.black);
        g.drawLine(minX, tickY, minX, tickY + 7);
        g.drawLine(maxX, tickY, maxX, tickY + 7);
        minX -= halfTickWidth;
        maxX -= halfTickWidth;
        g.drawImage(blueTick, minX, tickY, null);
        g.drawImage(blueTick, maxX, tickY, null);
        Color barColor = Color.getHSBColor(230f / 360f, 50f / 100f, 1f);
        Color vGradientStartColor = barColor.darker().darker();
        Color vGradientEndColor = barColor.brighter().brighter();
        final int barHeight = 4;
        Paint vPaint = new GradientPaint(0, quarterBoxHeight - 2, vGradientStartColor, 0, quarterBoxHeight - 1 + barHeight, vGradientEndColor, false);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(vPaint);
        g2.fillRoundRect(minX + halfTickWidth + 5, quarterBoxHeight - 2, (maxX - minX - halfTickWidth - 4), barHeight, 1, 1);
        setToolTipText("RangeSlider");
        g2.setFont(labelFont);
        g2.setPaint(Color.darkGray);
        final SimpleDateFormat df = getCorrectDf(getMinimum(), getMaximum());
        double nextDate = getNextIntervalDate(getMinimum(), df);
        int prevMaxPos = 0;
        while (true) {
            final int drawPos = toScreenX(nextDate);
            final Date date = new Date((long) nextDate);
            final String label = df.format(date);
            final Rectangle2D labelRect = labelFont.getStringBounds(label, g2.getFontRenderContext());
            final int halfLabelWidth = (int) labelRect.getWidth() / 2;
            final int labelHeight = (int) labelRect.getHeight();
            if (drawPos + halfLabelWidth > getWidth()) break;
            if (prevMaxPos < drawPos - halfLabelWidth - labelPad) {
                final int[] labelPos = { drawPos, (int) (rect.getHeight() - 2) };
                g2.drawLine(labelPos[0], labelPos[1] - labelHeight, labelPos[0], labelPos[1] - labelHeight + 3);
                g2.drawString(label, labelPos[0] - halfLabelWidth, labelPos[1]);
                prevMaxPos = drawPos + halfLabelWidth;
            }
            nextDate = getNextIntervalDate(nextDate, df);
            ;
        }
    }

    /**
     * Given a date, this will return the next date after 
     * choosing an interval based on the date range. 
     * 
     * @param prevDate
     * @return
     */
    private static final double getNextIntervalDate(double prevDate, SimpleDateFormat df) {
        if (df == LONGFORMAT) {
            return DataAggregator.rounddate(prevDate / 1000, DataAggregator.WEEK, DataAggregator.FORWARD) * 1000;
        } else if (df == MEDIUMFORMAT) {
            return DataAggregator.rounddate(prevDate / 1000, DataAggregator.HOUR, DataAggregator.FORWARD) * 1000;
        }
        return DataAggregator.rounddate(prevDate / 1000, DataAggregator.TENMINUTE, DataAggregator.FORWARD) * 1000;
    }

    private static SimpleDateFormat getCorrectDf(double minimum, double maximum) {
        final long range = (long) (maximum - minimum);
        if (range > WEEK * 10) return LONGFORMAT; else if (range > DAY * 3) return MEDIUMFORMAT;
        return SHORTFORMAT;
    }

    private double toLocalX(double x) {
        int width = getWidth();
        double xScale = (width - 3) / (getMaximum() - getMinimum());
        return ((x - 1) / xScale) + getMinimum();
    }

    private int toScreenX(double x) {
        int width = getWidth();
        double xScale = (width - 3) / (getMaximum() - getMinimum());
        return (int) ((x - getMinimum()) * xScale);
    }

    private int pickHandle(double x) {
        double minX = toScreenX(getLowValue());
        int maxX = toScreenX(getHighValue());
        int pick = 0;
        if (Math.abs(x - minX) < PICK_WIDTH) {
            pick |= PICK_MIN;
        }
        if (Math.abs(x - maxX) < PICK_WIDTH) {
            pick |= PICK_MAX;
        }
        if ((pick == 0) && (x > minX) && (x < maxX)) {
            pick = PICK_MID;
        }
        return pick;
    }

    final boolean RIGHT = true;

    final boolean LEFT = false;

    boolean direction = true;

    private void scrollAdvance() {
        if (direction == RIGHT) {
            offset((model.getMaximum() - model.getMinimum()) * scrollTickPercent);
        } else {
            offset(-(model.getMaximum() - model.getMinimum()) * scrollTickPercent);
        }
    }

    private void offset(double dx) {
        if (getLowValue() + dx < model.getMinimum() || getHighValue() + dx > model.getMaximum()) {
            if (scrolling == false) {
                mouseScrollStart[X] = mouseNow[X];
                mouseScrollStart[Y] = mouseNow[Y];
            }
            setScrolling(true);
            boolean neg = false;
            if (dx < 0) neg = true;
            direction = !neg;
            dx = Math.abs(dx);
            dx = dx + (model.getMaximum() - model.getMinimum()) * scrollTickPercent;
            if (neg == true) dx = dx * -1;
            model.setRangeProperties(getLowValue() + dx, model.getExtent(), getMinimum() + dx, getMaximum() + dx, true);
        } else {
            setScrolling(false);
            model.setValue(getLowValue() + dx);
        }
    }

    private void setScrolling(boolean scrolling) {
        if (this.scrolling == scrolling) return;
        this.scrolling = scrolling;
        if (scrolling) hasScrolled = true;
        if (scrolling) {
            updateTask = new TimerTask() {

                @Override
                public void run() {
                    scrollAdvance();
                }
            };
            timer.schedule(updateTask, 400, 400);
        } else {
            updateTask.cancel();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mousePressed(MouseEvent e) {
        if (enabled == false) {
            return;
        }
        Point point = e.getPoint();
        SwingUtilities.convertPointToScreen(point, (Component) e.getSource());
        mouseNow[X] = (int) point.getX();
        mouseNow[Y] = (int) point.getY();
        setScrolling(false);
        mouseDownStart[X] = mouseNow[X];
        mouseDownStart[Y] = mouseNow[Y];
        pick = pickHandle(e.getX());
        pickOffset = e.getX() - toScreenX(getLowValue());
        mouseX = e.getX();
        model.setValueIsAdjusting(true);
    }

    /**
     * {@inheritDoc}
     */
    public void mouseDragged(MouseEvent e) {
        if (enabled == false) {
            return;
        }
        Point point = e.getPoint();
        SwingUtilities.convertPointToScreen(point, (Component) e.getSource());
        mouseNow[X] = (int) point.getX();
        mouseNow[Y] = (int) point.getY();
        double xpos = e.getX();
        double x = toLocalX(xpos);
        if (x < getMinimum()) {
            x = getMinimum();
        }
        if (x > getMaximum()) {
            x = getMaximum();
        }
        if (pick == (PICK_MIN | PICK_MAX)) {
            if ((xpos - mouseX) > 2) {
                pick = PICK_MAX;
            } else if ((xpos - mouseX) < -2) {
                pick = PICK_MIN;
            } else {
                return;
            }
        }
        mouseX = xpos;
        switch(pick) {
            case PICK_MIN:
                if (x < getHighValue()) {
                    setLowValue(x);
                }
                localHandleX = -1;
                break;
            case PICK_MAX:
                if (x > getLowValue()) {
                    setHighValue(x);
                }
                localHandleX = -1;
                break;
            case PICK_MID:
                double dx = toLocalX(xpos - pickOffset) - getLowValue();
                localHandleX = e.getX();
                if (dx != 0) {
                    if (scrolling != true) {
                        if (hasScrolled) {
                            if (direction == RIGHT && mouseNow[X] < mouseScrollStart[X]) offset(dx); else if (direction == LEFT && mouseNow[X] > mouseScrollStart[X]) offset(dx);
                        } else {
                            offset(dx);
                        }
                    } else {
                        if (direction == RIGHT && mouseNow[X] < prevMouse[X]) setScrolling(false); else if (direction == LEFT && mouseNow[X] > prevMouse[X]) setScrolling(false);
                    }
                }
                break;
        }
        prevMouse[X] = mouseNow[X];
        prevMouse[Y] = mouseNow[Y];
    }

    /**
     * {@inheritDoc}
     */
    public void mouseReleased(MouseEvent e) {
        setScrolling(false);
        hasScrolled = false;
        model.setValueIsAdjusting(false);
        localHandleX = -1;
    }

    private void setCurs(int c) {
        Cursor cursor = Cursor.getPredefinedCursor(c);
        setCursor(cursor);
    }

    /**
     * {@inheritDoc}
     */
    public void mouseMoved(MouseEvent e) {
        if (enabled == false) {
            return;
        }
        switch(pickHandle(e.getX())) {
            case PICK_MIN:
            case PICK_MIN | PICK_MAX:
                setCurs(Cursor.W_RESIZE_CURSOR);
                break;
            case PICK_MAX:
                setCurs(Cursor.E_RESIZE_CURSOR);
                break;
            case PICK_MID:
                setCurs(Cursor.MOVE_CURSOR);
                break;
            case PICK_NONE:
                setCurs(Cursor.DEFAULT_CURSOR);
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void mouseClicked(MouseEvent e) {
        if (enabled == false) {
            return;
        }
        if (e.getClickCount() == 2) {
            model.setRangeProperties(model.getMinimum(), model.getMaximum() - model.getMinimum(), model.getMinimum(), model.getMaximum(), false);
            repaint();
        }
    }

    /**
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    public void mouseEntered(MouseEvent e) {
        mouseInComponent = true;
    }

    /**
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    public void mouseExited(MouseEvent e) {
        setCurs(Cursor.DEFAULT_CURSOR);
        mouseInComponent = false;
        setScrolling(false);
    }

    /**
     * @see javax.swing.JComponent#getPreferredSize()
     */
    public Dimension getPreferredSize() {
        return new Dimension(300, 20);
    }

    /**
     * @see javax.swing.JComponent#setEnabled(boolean)
     */
    public void setEnabled(boolean v) {
        enabled = v;
        repaint();
    }

    /**
     * @see javax.swing.event.ChangeListener#stateChanged(ChangeEvent)
     */
    public void stateChanged(ChangeEvent e) {
        repaint();
    }

    /**
     * Returns the doubleBoundedRangeModel.
     * 
     * @return BoundedRangeModel
     */
    public DoubleBoundedRangeModel getModel() {
        return model;
    }

    /**
     * Sets the doubleBoundedRangeModel.
     * 
     * @param model
     *            The doubleBoundedRangeModel to set
     */
    public void setModel(DoubleBoundedRangeModel model) {
        this.model = model;
        repaint();
    }

    /**
     * @see javax.swing.JComponent#getToolTipText(MouseEvent)
     */
    public String getToolTipText(MouseEvent event) {
        Date minDate = new Date((long) getMinimum());
        Date maxDate = new Date((long) getMaximum());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm");
        return sdf.format(minDate) + " to " + sdf.format(maxDate);
    }

    public void setDensity(int[] results) {
        totalAlarms = results[0];
        densityArray = new int[results.length - 1];
        for (int i = 0; i < densityArray.length; i++) {
            densityArray[i] = results[i + 1];
        }
    }
}
