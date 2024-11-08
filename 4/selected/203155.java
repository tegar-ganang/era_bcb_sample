package edu.udo.scaffoldhunter.view.plot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import edu.udo.scaffoldhunter.model.db.Molecule;
import edu.udo.scaffoldhunter.util.I18n;

/**
 *
 * @author Michael Hesse
 */
public class PlotPanel3D extends JPanel implements ModelChangeListener, MouseWheelListener, MouseListener, MouseMotionListener {

    private HashMap<Molecule, Integer> kMeansMap;

    PlotView plotView;

    List<PickChangeListener> pickChangeListenerList = new ArrayList<PickChangeListener>();

    int oldMouseOnScreenPositionX, oldMouseOnScreenPositionY;

    ScrollOMatic scrollOMatic;

    boolean renderingActive = false;

    int selectionColor;

    int publicBannerColor;

    int privateBannerColor;

    PlotViewState plotViewState;

    boolean isScrolledToViewState = false;

    boolean leftMouseButtonPressed = false;

    boolean rightMouseButtonPressed = false;

    /**
     * the logical channelnumber of the channel that provides the data for the x-axis
     */
    public static final int X_CHANNEL = 0;

    /**
     * the logical channelnumber of the channel that provides the data for the y-axis
     */
    public static final int Y_CHANNEL = 1;

    /**
     * the logical channelnumber of the channel that provides the data for the z-axis
     */
    public static final int Z_CHANNEL = 2;

    /**
     * the logical channelnumber of the channel that provides the data for the color-axis
     */
    public static final int COLOR_CHANNEL = 3;

    /**
     * the logical channelnumber of the channel that provides the data for the size-axis
     */
    public static final int SIZE_CHANNEL = 4;

    protected Model model;

    DotRenderer3D dotrenderer;

    TickStrategy tickStrategy;

    protected double[] e1 = new double[3];

    protected double[] e2 = new double[3];

    protected double[] e3 = new double[3];

    protected double[] eOrigin = new double[3];

    protected double dotspaceScaleX, dotspaceScaleY, dotspaceScaleZ;

    protected double metaRotationAlpha, metaRotationBeta;

    protected double metaScale;

    protected boolean showGrid;

    protected boolean showMarking;

    protected boolean showSelection;

    protected double selectionBBleft, selectionBBright, selectionBBtop, selectionBBbottom;

    protected Color defaultDotColor;

    protected Color minDotColor;

    protected Color maxDotColor;

    protected int[] colorPalette;

    protected int defaultDotsize = 2;

    protected int minDotsize = 2;

    protected int maxDotsize = 20;

    protected BufferedImage contentImage;

    protected boolean isContentImageDirty;

    protected int currentContentImageWidth, currentContentImageHeight;

    protected int currentContentImageXPos, currentContentImageYPos;

    protected enum MouseMode {

        NONE, SCROLL, ROTATE, MULTISELECTION_PLUS, MULTISELECTION_MINUS
    }

    ;

    protected MouseMode mouseMode;

    protected enum MultiselectionMode {

        NONE, ADJUSTING, COLLECTING_PLUS, COLLECTING_MINUS
    }

    ;

    protected MultiselectionMode multiselectionMode;

    protected Point multiselectionStart, multiselectionEnd;

    protected List<Integer> multiselection;

    /**
     */
    public enum HighlightingMode {

        /***/
        NONE, /***/
        SELECTION, /***/
        PUBLIC_BANNER, /***/
        PRIVATE_BANNER
    }

    ;

    protected HighlightingMode highlightingMode;

    /**
     * Creates a new JPanel with FlowLayout and the specified buffering strategy.
     * @param plotView 
     * @param isDoubleBuffered
     */
    public PlotPanel3D(PlotView plotView, boolean isDoubleBuffered) {
        super(isDoubleBuffered);
        this.plotView = plotView;
        scrollOMatic = new ScrollOMatic();
        selectionColor = 0xff0000 | 0xff000000;
        publicBannerColor = 0x00cc00 | 0xff000000;
        privateBannerColor = 0x0000ff | 0xff000000;
        initialize();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (plotViewState != null) saveState(plotViewState);
        if (isPaintable()) {
            render(g);
            if (!isScrolledToViewState) {
                if ((plotViewState.getScrollPosX() | plotViewState.getScrollPosY()) != 0) scrollOMatic.scrollTo(1.0, plotViewState.getScrollPosX(), plotViewState.getScrollPosY());
                isScrolledToViewState = true;
            }
        } else {
            clearImage(g);
        }
    }

    /**
     *
     */
    private void initialize() {
        setBackground(Color.white);
        setForeground(Color.black);
        addMouseWheelListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        mouseMode = MouseMode.NONE;
        multiselectionMode = MultiselectionMode.NONE;
        multiselectionStart = new Point();
        multiselectionEnd = new Point();
        multiselection = new ArrayList<Integer>();
        highlightingMode = HighlightingMode.NONE;
        tickStrategy = new SimpleTickStrategy();
        dotrenderer = new DotRenderer3D();
        resetDotspaceScaleFactors();
        resetMetaRotationAngles();
        resetMetaScaleFactors();
        setShowGrid(true);
        setShowMarking(true);
        setDefaultDotColor(new Color(0xff33ff));
        setMinDotColor(new Color(0x0099ff));
        setMaxDotColor(new Color(0xff33ff));
        colorPalette = new int[256];
        contentImage = null;
        isContentImageDirty = true;
        selectionBBleft = -1;
        selectionBBright = -1;
        selectionBBtop = -1;
        selectionBBbottom = -1;
        leftMouseButtonPressed = false;
        rightMouseButtonPressed = false;
    }

    /**
     *
     */
    public void resetCoSys() {
        dotspaceScaleX = 1.0;
        dotspaceScaleY = 1.0;
        dotspaceScaleZ = 1.0;
        metaScale = 1.0;
        metaRotationAlpha = 200.0;
        metaRotationBeta = 45.0;
        repaint();
    }

    /**
     *
     */
    private void resetDotspaceScaleFactors() {
        dotspaceScaleX = 1.0;
        dotspaceScaleY = 1.0;
        dotspaceScaleZ = 1.0;
        repaint();
    }

    /**
     *
     */
    public void resetDotspaceScaleX() {
        dotspaceScaleX = 1.0;
        repaint();
    }

    /**
     *
     */
    public void resetDotspaceScaleY() {
        dotspaceScaleY = 1.0;
        repaint();
    }

    /**
     *
     */
    public void resetDotspaceScaleZ() {
        dotspaceScaleZ = 1.0;
        repaint();
    }

    /**
     *
     */
    private void resetMetaRotationAngles() {
        metaRotationAlpha = 200.0;
        metaRotationBeta = 45.0;
        repaint();
    }

    /**
     *
     */
    private void resetMetaScaleFactors() {
        metaScale = 1.0;
        repaint();
    }

    private void calculateDotspaceUnitVectors() {
        double metaspaceSize = scrollOMatic.getMetaspaceSize();
        double pointvectorLength = metaspaceSize * 0.707106781 * metaScale;
        double[] pointvectorOrigin = { -0.5 * pointvectorLength * getDotspaceScaleX(), -0.5 * pointvectorLength * getDotspaceScaleY(), -0.5 * pointvectorLength * getDotspaceScaleZ() };
        double[] pointvectorX = { 0.5 * pointvectorLength * getDotspaceScaleX(), -0.5 * pointvectorLength * getDotspaceScaleY(), -0.5 * pointvectorLength * getDotspaceScaleZ() };
        double[] pointvectorY = { -0.5 * pointvectorLength * getDotspaceScaleX(), 0.5 * pointvectorLength * getDotspaceScaleY(), -0.5 * pointvectorLength * getDotspaceScaleZ() };
        double[] pointvectorZ = { -0.5 * pointvectorLength * getDotspaceScaleX(), -0.5 * pointvectorLength * getDotspaceScaleY(), 0.5 * pointvectorLength * getDotspaceScaleZ() };
        double[] rotateAxis1 = { Math.cos(-Math.toRadians(getMetaRotationBeta())), 0, Math.sin(-Math.toRadians(getMetaRotationBeta())) };
        double[] rotateAxis2 = { 0, -1, 0 };
        pointvectorOrigin = Util.rotateVector(pointvectorOrigin, rotateAxis1, getMetaRotationAlpha());
        pointvectorX = Util.rotateVector(pointvectorX, rotateAxis1, getMetaRotationAlpha());
        pointvectorY = Util.rotateVector(pointvectorY, rotateAxis1, getMetaRotationAlpha());
        pointvectorZ = Util.rotateVector(pointvectorZ, rotateAxis1, getMetaRotationAlpha());
        pointvectorOrigin = Util.rotateVector(pointvectorOrigin, rotateAxis2, getMetaRotationBeta());
        pointvectorX = Util.rotateVector(pointvectorX, rotateAxis2, getMetaRotationBeta());
        pointvectorY = Util.rotateVector(pointvectorY, rotateAxis2, getMetaRotationBeta());
        pointvectorZ = Util.rotateVector(pointvectorZ, rotateAxis2, getMetaRotationBeta());
        e1 = Util.subVectors(pointvectorX, pointvectorOrigin);
        e2 = Util.subVectors(pointvectorY, pointvectorOrigin);
        e3 = Util.subVectors(pointvectorZ, pointvectorOrigin);
        {
            double xpos = scrollOMatic.getDiagramCenterX();
            double ypos = scrollOMatic.getDiagramCenterY();
            eOrigin = Util.addVectors(pointvectorOrigin, new double[] { xpos, ypos, 0.0 });
        }
    }

    /**
     * draws the diagram
     * @param g
     */
    public void render(Graphics g) {
        if (!isPaintable()) {
            clearImage(g);
            return;
        }
        scrollOMatic.adjustMetric();
        g.translate(-scrollOMatic.getXPosition(), -scrollOMatic.getYPosition());
        calculateDotspaceUnitVectors();
        if ((scrollOMatic.getViewportWidth() != currentContentImageWidth) | (scrollOMatic.getViewportHeight() != currentContentImageHeight) | (scrollOMatic.getXPosition() != currentContentImageXPos) | (scrollOMatic.getYPosition() != currentContentImageYPos)) {
            isContentImageDirty = true;
            currentContentImageWidth = scrollOMatic.getViewportWidth();
            currentContentImageHeight = scrollOMatic.getViewportHeight();
            currentContentImageXPos = scrollOMatic.getXPosition();
            currentContentImageYPos = scrollOMatic.getYPosition();
        }
        double[] xInterval;
        double[] yInterval;
        double[] zInterval;
        {
            double xmin, xmax, ymin, ymax, zmin, zmax;
            if (isXYplaneMode() | isXZplaneMode() | isXYZplaneMode()) {
                xmin = getModel().getDataMin(X_CHANNEL);
                xmax = getModel().getDataMax(X_CHANNEL);
                if (xmax == xmin) {
                    if (xmin == 0) {
                        xmin -= 0.000005;
                        xmax += 0.000005;
                    } else {
                        xmin *= 0.999;
                        xmax *= 1.001;
                    }
                }
            } else {
                xmin = 0;
                xmax = 1;
            }
            if (isXYplaneMode() | isYZplaneMode() | isXYZplaneMode()) {
                ymin = getModel().getDataMin(Y_CHANNEL);
                ymax = getModel().getDataMax(Y_CHANNEL);
                if (ymax == ymin) {
                    if (ymin == 0) {
                        ymin -= 0.000005;
                        ymax += 0.000005;
                    } else {
                        ymin *= 0.999;
                        ymax *= 1.001;
                    }
                }
            } else {
                ymin = 0;
                ymax = 1;
            }
            if (isXZplaneMode() | isYZplaneMode() | isXYZplaneMode()) {
                zmin = getModel().getDataMin(Z_CHANNEL);
                zmax = getModel().getDataMax(Z_CHANNEL);
                if (zmax == zmin) {
                    if (zmin == 0) {
                        zmin -= 0.000005;
                        zmax += 0.000005;
                    } else {
                        zmin *= 0.999;
                        zmax *= 1.001;
                    }
                }
            } else {
                zmin = 0;
                zmax = 1;
            }
            xInterval = tickStrategy.suggestedInterval(xmin, xmax);
            yInterval = tickStrategy.suggestedInterval(ymin, ymax);
            zInterval = tickStrategy.suggestedInterval(zmin, zmax);
        }
        double[] xAxisMapping = Util.getLinearMapping(xInterval[0], xInterval[1], 0, 1);
        double[] yAxisMapping = Util.getLinearMapping(yInterval[0], yInterval[1], 0, 1);
        double[] zAxisMapping = Util.getLinearMapping(zInterval[0], zInterval[1], 0, 1);
        drawXAxis(g, xInterval, xAxisMapping, true, true);
        drawYAxis(g, yInterval, yAxisMapping, true, true);
        drawZAxis(g, zInterval, zAxisMapping, true, true);
        if ((multiselectionMode == MultiselectionMode.COLLECTING_PLUS) | (multiselectionMode == MultiselectionMode.COLLECTING_MINUS)) {
            multiselection.clear();
            isContentImageDirty = true;
        }
        int multiselectionLeft, multiselectionRight, multiselectionTop, multiselectionBottom;
        if (multiselectionStart.x < multiselectionEnd.x) {
            multiselectionLeft = multiselectionStart.x;
            multiselectionRight = multiselectionEnd.x;
        } else {
            multiselectionLeft = multiselectionEnd.x;
            multiselectionRight = multiselectionStart.x;
        }
        if (multiselectionStart.y < multiselectionEnd.y) {
            multiselectionTop = multiselectionStart.y;
            multiselectionBottom = multiselectionEnd.y;
        } else {
            multiselectionTop = multiselectionEnd.y;
            multiselectionBottom = multiselectionStart.y;
        }
        if (isContentImageDirty | (contentImage != null)) {
            Color foreground = g.getColor();
            double depthMin = 0, depthMax = 0;
            if (e1[2] < 0) {
                depthMin += e1[2];
            } else {
                depthMax += e1[2];
            }
            if (e2[2] < 0) {
                depthMin += e2[2];
            } else {
                depthMax += e2[2];
            }
            if (e3[2] < 0) {
                depthMin += e3[2];
            } else {
                depthMax += e3[2];
            }
            dotrenderer.prepare(currentContentImageWidth, currentContentImageHeight, depthMin, depthMax);
            g.setColor(getDefaultDotColor());
            double x, y, z;
            double px, py, pz;
            double dotsize = getDefaultDotsize();
            int dotcolor = getDefaultDotColor().getRGB() | 0xff000000;
            double colorFactor = 0, colorOffset = 0;
            double sizeFactor = 0, sizeOffset = 0;
            if (model.hasData(COLOR_CHANNEL)) {
                double min = model.getDataMin(COLOR_CHANNEL);
                double max = model.getDataMax(COLOR_CHANNEL);
                if ((max - min) == 0) {
                    if (min == 0) {
                        min -= 0.000005;
                        max += 0.000005;
                    } else {
                        min *= 0.999;
                        max *= 1.001;
                    }
                }
                colorFactor = 255 / (max - min);
                colorOffset = min * colorFactor;
            }
            if (model.hasData(SIZE_CHANNEL)) {
                double min = model.getDataMin(SIZE_CHANNEL);
                double max = model.getDataMax(SIZE_CHANNEL);
                if ((max - min) == 0) {
                    if (min == 0) {
                        min -= 0.000005;
                        max += 0.000005;
                    } else {
                        min *= 0.999;
                        max *= 1.001;
                    }
                }
                sizeFactor = (getMaxDotsize() - getMinDotsize()) / (max - min);
                sizeOffset = min * sizeFactor - getMinDotsize();
            }
            boolean _xpm = (isXYplaneMode() | isXZplaneMode() | isXYZplaneMode());
            boolean _ypm = (isXYplaneMode() | isYZplaneMode() | isXYZplaneMode());
            boolean _zpm = (isXZplaneMode() | isYZplaneMode() | isXYZplaneMode());
            int _defaultDotColor = getDefaultDotColor().getRGB() | 0xff000000;
            selectionBBleft = -1;
            selectionBBright = -1;
            selectionBBtop = -1;
            selectionBBbottom = -1;
            boolean collectMultiselection = (multiselectionMode == MultiselectionMode.COLLECTING_PLUS) | (multiselectionMode == MultiselectionMode.COLLECTING_MINUS);
            int stdDotcolor;
            for (int i = 0; i < model.getDataLength(); i++) {
                x = (_xpm ? model.getData(X_CHANNEL, i) : 0.0);
                y = (_ypm ? model.getData(Y_CHANNEL, i) : 0.0);
                z = (_zpm ? model.getData(Z_CHANNEL, i) : 0.0);
                if (Double.isNaN(x)) continue;
                if (Double.isNaN(y)) continue;
                if (Double.isNaN(z)) continue;
                if (model.hasData(COLOR_CHANNEL)) {
                    if (Double.isNaN(model.getData(COLOR_CHANNEL, i))) {
                        continue;
                    } else {
                        if (kMeansMap == null) {
                            dotcolor = (int) (model.getData(COLOR_CHANNEL, i) * colorFactor - colorOffset);
                            dotcolor = colorPalette[dotcolor];
                        } else {
                            dotcolor = kMeansMap.get(model.getMolecule(i)) * -100000;
                        }
                    }
                } else dotcolor = _defaultDotColor;
                stdDotcolor = dotcolor;
                if (isShowSelection()) {
                    dotcolor = (model.isSelected(i) ? selectionColor : dotcolor);
                } else if (highlightingMode == HighlightingMode.PUBLIC_BANNER) {
                    dotcolor = (model.hasPublicBanner(i) ? publicBannerColor : dotcolor);
                } else if (highlightingMode == HighlightingMode.PRIVATE_BANNER) {
                    dotcolor = (model.hasPrivateBanner(i) ? privateBannerColor : dotcolor);
                }
                if (model.hasData(SIZE_CHANNEL)) {
                    if (Double.isNaN(model.getData(SIZE_CHANNEL, i))) {
                        continue;
                    } else {
                        dotsize = model.getData(SIZE_CHANNEL, i) * sizeFactor - sizeOffset;
                    }
                }
                x = Util.mapLinear(x, xAxisMapping);
                y = Util.mapLinear(y, yAxisMapping);
                z = Util.mapLinear(z, zAxisMapping);
                px = x * e1[0] + y * e2[0] + z * e3[0] + eOrigin[0];
                py = x * e1[1] + y * e2[1] + z * e3[1] + eOrigin[1];
                pz = x * e1[2] + y * e2[2] + z * e3[2] + eOrigin[2];
                if (collectMultiselection) {
                    if ((px >= multiselectionLeft) & (px <= multiselectionRight) & (py >= multiselectionTop) & (py <= multiselectionBottom)) {
                        multiselection.add(i);
                        if (multiselectionMode == MultiselectionMode.COLLECTING_PLUS) dotcolor = selectionColor; else dotcolor = stdDotcolor;
                    }
                }
                if (model.isSelected(i)) {
                    if ((selectionBBleft == -1) | ((int) px < selectionBBleft)) selectionBBleft = px;
                    if ((selectionBBright == -1) | ((int) px > selectionBBright)) selectionBBright = px;
                    if ((selectionBBtop == -1) | ((int) py < selectionBBtop)) selectionBBtop = py;
                    if ((selectionBBbottom == -1) | ((int) py > selectionBBbottom)) selectionBBbottom = py;
                }
                dotrenderer.plotDot((int) px - currentContentImageXPos, (int) py - currentContentImageYPos, pz - eOrigin[2], dotcolor, i, (int) dotsize);
            }
            contentImage = dotrenderer.getContentImage();
            isContentImageDirty = false;
            g.setColor(foreground);
        }
        g.drawImage(contentImage, currentContentImageXPos, currentContentImageYPos, null);
        {
            boolean drawXY = false, drawXZ = false, drawYZ = false;
            if (e1[2] > 0) drawYZ = true;
            if (e2[2] > 0) drawXZ = true;
            if (e3[2] > 0) drawXY = true;
            if (drawXY | drawXZ) drawXAxis(g, xInterval, xAxisMapping, drawXY, drawXZ);
            if (drawXY | drawYZ) drawYAxis(g, yInterval, yAxisMapping, drawXY, drawYZ);
            if (drawXZ | drawYZ) drawZAxis(g, zInterval, zAxisMapping, drawXZ, drawYZ);
        }
        if (multiselectionMode == MultiselectionMode.ADJUSTING) {
            Color formerColor = g.getColor();
            g.setColor(Color.RED);
            int width = multiselectionRight - multiselectionLeft;
            int height = multiselectionBottom - multiselectionTop;
            g.drawRect(multiselectionLeft, multiselectionTop, width, height);
            g.setColor(formerColor);
        }
        if ((multiselectionMode == MultiselectionMode.COLLECTING_PLUS) | (multiselectionMode == MultiselectionMode.COLLECTING_MINUS)) {
            boolean mode = (multiselectionMode == MultiselectionMode.COLLECTING_PLUS);
            model.setSelected(multiselection, mode);
            multiselection.clear();
            multiselectionMode = MultiselectionMode.NONE;
        }
        g.translate(scrollOMatic.getXPosition(), scrollOMatic.getYPosition());
    }

    /**
     * 
     * @param g
     */
    protected void clearImage(Graphics g) {
        if (isContentImageDirty) {
            Rectangle rec = g.getClipBounds();
            currentContentImageWidth = rec.width;
            currentContentImageHeight = rec.height;
            currentContentImageXPos = rec.x;
            currentContentImageYPos = rec.y;
            dotrenderer.prepare(g.getClipBounds().width, g.getClipBounds().height, 0, 10);
            contentImage = dotrenderer.getContentImage();
            isContentImageDirty = false;
        }
        g.drawImage(contentImage, 0, 0, null);
    }

    protected void drawXAxis(Graphics g, double[] xInterval, double[] xAxisMapping, boolean xyVisible, boolean xzVisible) {
        if (isYZplaneMode()) return;
        double[] ticks = tickStrategy.getTicks(xInterval[0], xInterval[1]);
        Color foreground = g.getColor();
        if (isShowMarking()) {
            double labelWidth, labelHeight;
            labelHeight = g.getFontMetrics().getHeight();
            double l = 1 / Math.sqrt((e2[0] + e3[0]) * (e2[0] + e3[0]) + (e2[1] + e3[1]) * (e2[1] + e3[1]) + (e2[2] + e3[2]) * (e2[2] + e3[2]));
            for (double t : ticks) {
                double x = Util.mapLinear(t, xAxisMapping);
                g.setColor(foreground);
                drawLine(g, x, 0, 0, x, -10 * l, -10 * l);
                g.setColor(Color.gray);
                labelWidth = g.getFontMetrics().stringWidth(t + "");
                g.drawString(t + "", (int) (x * e1[0] - 20 * l * e2[0] - 20 * l * e3[0] + eOrigin[0] - labelWidth / 2), (int) (x * e1[1] - 20 * l * e2[1] - 20 * l * e3[1] + eOrigin[1] + labelHeight / 2));
            }
            l = 1 / Math.sqrt(e1[0] * e1[0] + e1[1] * e1[1] + e1[2] * e1[2]);
            g.drawString(I18n.get("PlotView.Hyperplane.XAxisShortcut"), (int) ((40 * l + 1) * e1[0] + 0 * e2[0] + 0 * e3[0] + eOrigin[0]), (int) ((20 * l + 1) * e1[1] + 0 * e2[1] + 0 * e3[1] + eOrigin[1] + labelHeight / 2));
        }
        if (isShowGrid()) {
            g.setColor(Color.lightGray);
            for (int i = 1; i < ticks.length; i++) {
                double x = Util.mapLinear(ticks[i], xAxisMapping);
                if (xyVisible) drawLine(g, x, 0, 0, x, 1, 0);
                if (xzVisible) drawLine(g, x, 0, 0, x, 0, 1);
            }
            if (model.getHyperplanePanel().hasMinHpBound(X_CHANNEL)) {
                g.setColor(Color.DARK_GRAY);
                double x = Util.mapLinear(model.getHyperplanePanel().getMinValue(X_CHANNEL), xAxisMapping);
                if (xyVisible) drawLine(g, x, 0, 0, x, 1, 0);
                if (xzVisible) drawLine(g, x, 0, 0, x, 0, 1);
            }
            if (model.getHyperplanePanel().hasMaxHpBound(X_CHANNEL)) {
                g.setColor(Color.DARK_GRAY);
                double x = Util.mapLinear(model.getHyperplanePanel().getMaxValue(X_CHANNEL), xAxisMapping);
                if (xyVisible) drawLine(g, x, 0, 0, x, 1, 0);
                if (xzVisible) drawLine(g, x, 0, 0, x, 0, 1);
            }
        }
        g.setColor(foreground);
        drawLine(g, 1, 0, 0, 0, 0, 0);
    }

    protected void drawYAxis(Graphics g, double[] yInterval, double[] yAxisMapping, boolean xyVisible, boolean yzVisible) {
        if (isXZplaneMode()) return;
        double[] ticks = tickStrategy.getTicks(yInterval[0], yInterval[1]);
        Color foreground = g.getColor();
        if (isShowMarking()) {
            double labelWidth, labelHeight;
            labelHeight = g.getFontMetrics().getHeight();
            double l = 1 / Math.sqrt((e1[0] + e3[0]) * (e1[0] + e3[0]) + (e1[1] + e3[1]) * (e1[1] + e3[1]) + (e1[2] + e3[2]) * (e1[2] + e3[2]));
            for (double t : ticks) {
                double y = Util.mapLinear(t, yAxisMapping);
                g.setColor(foreground);
                drawLine(g, 0, y, 0, -10 * l, y, -10 * l);
                g.setColor(Color.gray);
                labelWidth = g.getFontMetrics().stringWidth(t + "");
                g.drawString(t + "", (int) (-20 * l * e1[0] + y * e2[0] - 20 * l * e3[0] + eOrigin[0] - labelWidth / 2), (int) (-20 * l * e1[1] + y * e2[1] - 20 * l * e3[1] + eOrigin[1] + labelHeight / 2));
            }
            l = 1 / Math.sqrt(e2[0] * e2[0] + e2[1] * e2[1] + e2[2] * e2[2]);
            g.drawString(I18n.get("PlotView.Hyperplane.YAxisShortcut"), (int) (0 * e1[0] + (20 * l + 1) * e2[0] + 0 * e3[0] + eOrigin[0]), (int) (0 * e1[1] + (30 * l + 1) * e2[1] + 0 * e3[1] + eOrigin[1] + labelHeight / 2));
        }
        if (isShowGrid()) {
            g.setColor(Color.lightGray);
            for (int i = 1; i < ticks.length; i++) {
                double y = Util.mapLinear(ticks[i], yAxisMapping);
                if (xyVisible) drawLine(g, 0, y, 0, 1, y, 0);
                if (yzVisible) drawLine(g, 0, y, 0, 0, y, 1);
            }
            if (model.getHyperplanePanel().hasMinHpBound(Y_CHANNEL)) {
                g.setColor(Color.DARK_GRAY);
                double y = Util.mapLinear(model.getHyperplanePanel().getMinValue(Y_CHANNEL), yAxisMapping);
                if (xyVisible) drawLine(g, 0, y, 0, 1, y, 0);
                if (yzVisible) drawLine(g, 0, y, 0, 0, y, 1);
            }
            if (model.getHyperplanePanel().hasMaxHpBound(Y_CHANNEL)) {
                g.setColor(Color.DARK_GRAY);
                double y = Util.mapLinear(model.getHyperplanePanel().getMaxValue(Y_CHANNEL), yAxisMapping);
                if (xyVisible) drawLine(g, 0, y, 0, 1, y, 0);
                if (yzVisible) drawLine(g, 0, y, 0, 0, y, 1);
            }
        }
        g.setColor(foreground);
        drawLine(g, 0, 1, 0, 0, 0, 0);
    }

    protected void drawZAxis(Graphics g, double[] zInterval, double[] zAxisMapping, boolean xzVisible, boolean yzVisible) {
        if (isXYplaneMode()) return;
        double[] ticks = tickStrategy.getTicks(zInterval[0], zInterval[1]);
        Color foreground = g.getColor();
        if (isShowMarking()) {
            double labelWidth, labelHeight;
            labelHeight = g.getFontMetrics().getHeight();
            double l = 1 / Math.sqrt((e1[0] + e2[0]) * (e1[0] + e2[0]) + (e1[1] + e2[1]) * (e1[1] + e2[1]) + (e1[2] + e2[2]) * (e1[2] + e2[2]));
            for (double t : ticks) {
                double z = Util.mapLinear(t, zAxisMapping);
                g.setColor(foreground);
                drawLine(g, 0, 0, z, -10 * l, -10 * l, z);
                g.setColor(Color.gray);
                labelWidth = g.getFontMetrics().stringWidth(t + "");
                g.drawString(t + "", (int) (-20 * l * e1[0] - 20 * l * e2[0] + z * e3[0] + eOrigin[0] - labelWidth / 2), (int) (-20 * l * e1[1] - 20 * l * e2[1] + z * e3[1] + eOrigin[1] + labelHeight / 2));
            }
            l = 1 / Math.sqrt(e3[0] * e3[0] + e3[1] * e3[1] + e3[2] * e3[2]);
            g.drawString(I18n.get("PlotView.Hyperplane.ZAxisShortcut"), (int) (0 * e1[0] + 0 * e2[0] + (40 * l + 1) * e3[0] + eOrigin[0]), (int) (0 * e1[1] + 0 * e2[1] + (20 * l + 1) * e3[1] + eOrigin[1] + labelHeight / 2));
        }
        if (isShowGrid()) {
            g.setColor(Color.lightGray);
            for (int i = 1; i < ticks.length; i++) {
                double z = Util.mapLinear(ticks[i], zAxisMapping);
                if (xzVisible) drawLine(g, 0, 0, z, 1, 0, z);
                if (yzVisible) drawLine(g, 0, 0, z, 0, 1, z);
            }
            if (model.getHyperplanePanel().hasMinHpBound(Z_CHANNEL)) {
                g.setColor(Color.DARK_GRAY);
                double z = Util.mapLinear(model.getHyperplanePanel().getMinValue(Z_CHANNEL), zAxisMapping);
                if (xzVisible) drawLine(g, 0, 0, z, 1, 0, z);
                if (yzVisible) drawLine(g, 0, 0, z, 0, 1, z);
            }
            if (model.getHyperplanePanel().hasMaxHpBound(Z_CHANNEL)) {
                g.setColor(Color.DARK_GRAY);
                double z = Util.mapLinear(model.getHyperplanePanel().getMaxValue(Z_CHANNEL), zAxisMapping);
                if (xzVisible) drawLine(g, 0, 0, z, 1, 0, z);
                if (yzVisible) drawLine(g, 0, 0, z, 0, 1, z);
            }
        }
        g.setColor(foreground);
        drawLine(g, 0, 0, 1, 0, 0, 0);
    }

    /**
     * draws a line (int metaspace coordinates, from 0 to 1)
     * @param g
     * @param x1
     * @param y1
     * @param z1
     * @param x2
     * @param y2
     * @param z2
     */
    protected void drawLine(Graphics g, double x1, double y1, double z1, double x2, double y2, double z2) {
        g.drawLine((int) (x1 * e1[0] + y1 * e2[0] + z1 * e3[0] + eOrigin[0]), (int) (x1 * e1[1] + y1 * e2[1] + z1 * e3[1] + eOrigin[1]), (int) (x2 * e1[0] + y2 * e2[0] + z2 * e3[0] + eOrigin[0]), (int) (x2 * e1[1] + y2 * e2[1] + z2 * e3[1] + eOrigin[1]));
    }

    protected void createColorPalette() {
        double r, g, b, rMin, gMin, bMin, rMax, gMax, bMax;
        rMin = getMinDotColor().getRed();
        gMin = getMinDotColor().getGreen();
        bMin = getMinDotColor().getBlue();
        rMax = getMaxDotColor().getRed();
        gMax = getMaxDotColor().getGreen();
        bMax = getMaxDotColor().getBlue();
        for (int i = 0; i < 256; i++) {
            r = (rMax - rMin) / 256 * i + rMin;
            g = (gMax - gMin) / 256 * i + gMin;
            b = (bMax - bMin) / 256 * i + bMin;
            colorPalette[i] = ((((int) r) & 0xff) << 16) | ((((int) g) & 0xff) << 8) | ((((int) b) & 0xff)) | 0xff000000;
        }
    }

    /**
     * @param x
     * @param y
     * @return
     *  the object number at position x,y
     */
    public int getObjectAtPosition(int x, int y) {
        int n = -1;
        if (isPaintable()) n = dotrenderer.getObjectNumber(x, y);
        return n;
    }

    /**
     * @return
     *   the scalefactor for the unit vector X
     */
    public double getDotspaceScaleX() {
        return dotspaceScaleX;
    }

    /**
     * @return
     *   the scalefactor for the unit vector Y
     */
    public double getDotspaceScaleY() {
        return dotspaceScaleY;
    }

    /**
     * @return
     *   the scalefactor for the unit vector Z
     */
    public double getDotspaceScaleZ() {
        return dotspaceScaleZ;
    }

    /**
     * @param sx
     *   the scalefactor for the unit vector X (must be >0.1 and <10)
     */
    public void setDotspaceScaleX(double sx) {
        if ((sx >= 0.1) & (sx <= 10)) {
            dotspaceScaleX = sx;
            repaint();
        }
    }

    /**
     * @param sy
     *   the scalefactor for the unit vector Y (must be >0.1 and <10)
     */
    public void setDotspaceScaleY(double sy) {
        if ((sy >= 0.1) & (sy <= 10)) {
            dotspaceScaleY = sy;
            repaint();
        }
    }

    /**
     * @param sz
     *   the scalefactor for the unit vector Z (must be >0.1 and <10)
     */
    public void setDotspaceScaleZ(double sz) {
        if ((sz >= 0.1) & (sz <= 10)) {
            dotspaceScaleZ = sz;
            repaint();
        }
    }

    /**
     * @return
     *   the scalefactor for the coordinate system of the window (the metaspace)
     */
    public double getMetaScale() {
        return metaScale;
    }

    /**
     * @param ms
     *   the scalefactor for the coordinate system of the window (the metaspace)
     *   (must be between 0.1 and 20)
     */
    public void setMetaScale(double ms) {
        if (ms < 0.5) ms = 0.5;
        metaScale = ms;
        isContentImageDirty = true;
        repaint();
    }

    /**
     * @return
     *   the rotation of the coordinate system in the window (in degrees)
     */
    public double getMetaRotationAlpha() {
        if (isXYplaneMode()) return 180.0;
        if (isYZplaneMode()) return 180.0;
        if (isXZplaneMode()) return 90.0;
        return metaRotationAlpha;
    }

    /**
     * @return
     *   the rotation of the coordinate system in the window (in degrees)
     */
    public double getMetaRotationBeta() {
        if (isXYplaneMode()) return 0.0;
        if (isYZplaneMode()) return -90.0;
        if (isXZplaneMode()) return 0.0;
        return metaRotationBeta;
    }

    /**
     * @param ma
     *   the rotation of the coordinate system in the window (in degrees)
     */
    public void setMetaRotationAlpha(double ma) {
        if (isXYZplaneMode()) metaRotationAlpha = ma;
        repaint();
    }

    /**
     * @param mb
     *   the rotation of the coordinate system in the window (in degrees)
     */
    public void setMetaRotationBeta(double mb) {
        if (isXYZplaneMode()) metaRotationBeta = mb;
        repaint();
    }

    /**
     * 
     * @return
     *  the current used model
     */
    public Model getModel() {
        return model;
    }

    /**
     * 
     * @param model
     * @return
     *  true if the model fits to this plot. a model fits if it
     *  is not null.
     */
    public boolean setModel(Model model) {
        if (model == null) return false;
        if (this.model != null) this.model.removeModelChangeListener(this);
        this.model = model;
        model.addModelChangeListener(this);
        selectionBBleft = -1;
        selectionBBright = -1;
        selectionBBtop = -1;
        selectionBBbottom = -1;
        repaint();
        return true;
    }

    /**
     * 
     * @return
     *   true if there is a model
     */
    public boolean hasModel() {
        return !(model == null);
    }

    /**
     * @return
     *   true if markings are shown, false otherwise
     */
    public boolean isShowMarking() {
        return showMarking;
    }

    /**
     * true, if the markings should be shown
     * 
     * @param showMarking
     */
    public void setShowMarking(boolean showMarking) {
        if (showMarking != this.showMarking) {
            this.showMarking = showMarking;
            repaint();
        }
    }

    /**
     * gets the highlighting mode
     * @return 
     *  the current highlighting mode
     */
    public HighlightingMode getHighlightingMode() {
        return highlightingMode;
    }

    /**
     * sets the highlighting mode
     * @param mode
     */
    public void setHighlightingMode(HighlightingMode mode) {
        highlightingMode = mode;
        repaint();
    }

    /**
     * 
     * @return
     *  true, if the grid should be shown
     */
    public boolean isShowGrid() {
        return showGrid;
    }

    /**
     * 
     * @param showGrid
     */
    public void setShowGrid(boolean showGrid) {
        if (showGrid != this.showGrid) {
            this.showGrid = showGrid;
            repaint();
        }
    }

    /**
     * @return
     *  true if the selection should be marked as such
     */
    public boolean isShowSelection() {
        return (highlightingMode == HighlightingMode.SELECTION);
    }

    /**
     * 
     * @param showSelection
     */
    public void setShowSelection(boolean showSelection) {
        if (showSelection != isShowSelection()) {
            plotView.setHighlightingMode(HighlightingMode.SELECTION);
        }
    }

    /**
     * sets the default color
     * 
     * @param color
     */
    public void setDefaultDotColor(Color color) {
        defaultDotColor = color;
        repaint();
    }

    /**
     * 
     * @return
     *  the default color
     */
    public Color getDefaultDotColor() {
        return defaultDotColor;
    }

    /**
     * sets the minimal color
     * 
     * @param color
     */
    public void setMinDotColor(Color color) {
        minDotColor = color;
        if (model != null) if (model.hasData(COLOR_CHANNEL)) createColorPalette();
        repaint();
    }

    /**
     * 
     * @return
     *  the minimum color, as set by the user
     */
    public Color getMinDotColor() {
        return minDotColor;
    }

    /**
     * sets the maximum color
     * 
     * @param color
     */
    public void setMaxDotColor(Color color) {
        maxDotColor = color;
        if (model != null) if (model.hasData(COLOR_CHANNEL)) createColorPalette();
        repaint();
    }

    /**
     * 
     * @return
     *  the maximum color, as set by the user
     */
    public Color getMaxDotColor() {
        return maxDotColor;
    }

    /**
     * sets the default dotsize
     * 
     * @param defaultDotsize
     */
    public void setDefaultDotsize(int defaultDotsize) {
        if ((defaultDotsize >= 1) & (defaultDotsize <= 20)) {
            this.defaultDotsize = defaultDotsize;
            repaint();
        }
    }

    /**
     * 
     * @return
     *  the default dotsize
     */
    public int getDefaultDotsize() {
        return defaultDotsize;
    }

    /**
     * sets the minimal dotsize
     * 
     * @param minDotsize
     */
    public void setMinDotsize(int minDotsize) {
        if ((minDotsize >= 1) & (minDotsize <= 19)) {
            this.minDotsize = minDotsize;
            if (minDotsize >= maxDotsize) maxDotsize = minDotsize + 1;
            repaint();
        }
    }

    /**
     * 
     * @return
     *  the minimum dotsize, as set by the use
     */
    public int getMinDotsize() {
        return minDotsize;
    }

    /**
     *  sets the maximum dotsize
     * 
     * @param maxDotsize
     */
    public void setMaxDotsize(int maxDotsize) {
        if ((maxDotsize >= 2) & (maxDotsize <= 20)) {
            this.maxDotsize = maxDotsize;
            if (maxDotsize <= minDotsize) minDotsize = maxDotsize - 1;
            repaint();
        }
    }

    /**
     * 
     * @return
     *  the maximum dotsize, as set by the use
     */
    public int getMaxDotsize() {
        return maxDotsize;
    }

    /**
     * checks if there are enough channels to paint the diagram
     * 
     * @return
     *  true if there are enough channels to paint the diagram
     */
    private boolean isPaintable() {
        if (!hasModel()) return false;
        return (isXYplaneMode() | isYZplaneMode() | isXZplaneMode() | isXYZplaneMode());
    }

    private boolean isXYplaneMode() {
        boolean mode = false;
        if (getModel().hasData(X_CHANNEL) & getModel().hasData(Y_CHANNEL) & (!getModel().hasData(Z_CHANNEL))) {
            mode = true;
            if (Double.isNaN(model.getDataMin(X_CHANNEL)) | Double.isNaN(model.getDataMax(X_CHANNEL))) {
                mode = false;
            }
            if (Double.isNaN(model.getDataMin(Y_CHANNEL)) | Double.isNaN(model.getDataMax(Y_CHANNEL))) {
                mode = false;
            }
        }
        return mode;
    }

    private boolean isXZplaneMode() {
        boolean mode = false;
        if (getModel().hasData(X_CHANNEL) & (!getModel().hasData(Y_CHANNEL)) & getModel().hasData(Z_CHANNEL)) {
            mode = true;
            if (Double.isNaN(model.getDataMin(X_CHANNEL)) | Double.isNaN(model.getDataMax(X_CHANNEL))) {
                mode = false;
            }
            if (Double.isNaN(model.getDataMin(Z_CHANNEL)) | Double.isNaN(model.getDataMax(Z_CHANNEL))) {
                mode = false;
            }
        }
        return mode;
    }

    private boolean isYZplaneMode() {
        boolean mode = false;
        if ((!getModel().hasData(X_CHANNEL)) & getModel().hasData(Y_CHANNEL) & getModel().hasData(Z_CHANNEL)) {
            mode = true;
            if (Double.isNaN(model.getDataMin(Y_CHANNEL)) | Double.isNaN(model.getDataMax(Y_CHANNEL))) {
                mode = false;
            }
            if (Double.isNaN(model.getDataMin(Z_CHANNEL)) | Double.isNaN(model.getDataMax(Z_CHANNEL))) {
                mode = false;
            }
        }
        return mode;
    }

    private boolean isXYZplaneMode() {
        boolean mode = false;
        if (getModel().hasData(X_CHANNEL) & getModel().hasData(Y_CHANNEL) & getModel().hasData(Z_CHANNEL)) {
            mode = true;
            if (Double.isNaN(model.getDataMin(X_CHANNEL)) | Double.isNaN(model.getDataMax(X_CHANNEL))) {
                mode = false;
            }
            if (Double.isNaN(model.getDataMin(Y_CHANNEL)) | Double.isNaN(model.getDataMax(Y_CHANNEL))) {
                mode = false;
            }
            if (Double.isNaN(model.getDataMin(Z_CHANNEL)) | Double.isNaN(model.getDataMax(Z_CHANNEL))) {
                mode = false;
            }
        }
        return mode;
    }

    /**
     * zooms to the selection
     */
    public void zoomToSelection() {
        if (selectionBBleft == -1) return;
        double sf;
        double vpWidth = getWidth() - 50;
        double vpHeight = getHeight() - 50;
        double bbWidth = selectionBBright - selectionBBleft;
        if (bbWidth == 0) {
            bbWidth = vpWidth;
            selectionBBleft -= bbWidth / 2;
            selectionBBright += bbWidth / 2;
        }
        double bbHeight = selectionBBbottom - selectionBBtop;
        if (bbHeight == 0) {
            bbHeight = vpHeight;
            selectionBBtop -= bbHeight / 2;
            selectionBBbottom += bbHeight / 2;
        }
        sf = ((vpWidth / bbWidth) < (vpHeight / bbHeight) ? (vpWidth / bbWidth) : (vpHeight / bbHeight));
        if (((int) sf) == 1) sf = 3;
        vpWidth += 50;
        vpHeight += 50;
        double offsetX = (vpWidth - bbWidth * sf) / 2;
        double offsetY = (vpHeight - bbHeight * sf) / 2;
        double xPos = selectionBBleft * sf - offsetX + 8;
        double yPos = selectionBBtop * sf - offsetY + 8;
        scrollOMatic.scrollTo(sf, (int) xPos, (int) yPos);
    }

    /**
     * saves the current state to the plotViewState object
     * @param pvs 
     */
    public void saveState(PlotViewState pvs) {
        plotViewState = pvs;
        if (pvs.isApplied()) {
            pvs.setDefaultDotcolor(getDefaultDotColor());
            pvs.setMinDotcolor(getMinDotColor());
            pvs.setMaxDotcolor(getMaxDotColor());
            pvs.setMetaScale(getMetaScale());
            pvs.setXAxisScaleScale(getDotspaceScaleX());
            pvs.setYAxisScaleScale(getDotspaceScaleY());
            pvs.setZAxisScaleScale(getDotspaceScaleZ());
            pvs.setShowGrid(isShowGrid());
            pvs.setShowTicks(isShowMarking());
            pvs.setMetaRotationAlpha(getMetaRotationAlpha());
            pvs.setMetaRotationBeta(getMetaRotationBeta());
            if (isScrolledToViewState) {
                pvs.setScrollPosX(scrollOMatic.getXPosition());
                pvs.setScrollPosY(scrollOMatic.getYPosition());
            }
        }
    }

    /**
     * loads the current state from the plotViewState object
     * @param pvs
     */
    public void loadState(PlotViewState pvs) {
        metaRotationAlpha = pvs.getMetaRotationAlpha();
        metaRotationBeta = pvs.getMetaRotationBeta();
        setDefaultDotColor(pvs.getDefaultDotcolor());
        setMinDotColor(pvs.getMinDotcolor());
        setMaxDotColor(pvs.getMaxDotcolor());
        setMetaScale(pvs.getMetaScale());
        setDotspaceScaleX(pvs.getXAxisScale());
        setDotspaceScaleY(pvs.getYAxisScale());
        setDotspaceScaleZ(pvs.getZAxisScale());
        setShowGrid(pvs.isShowGrid());
        setShowMarking(pvs.isShowTicks());
        plotViewState = pvs;
    }

    /**
     * Listenes to changes in the model
     *
     * @param model
     *   the model which changed
     * @param channel
     *   the channel whichs data changed
     * @param moreToCome
     *   true if there will be more model changes right away (more than one
     *   channel changed), so there is no need to repaint it at once
     */
    @Override
    public void modelChanged(Model model, int channel, boolean moreToCome) {
        if (!moreToCome) {
            if (model.hasData(COLOR_CHANNEL)) createColorPalette();
            isContentImageDirty = true;
            selectionBBleft = -1;
            selectionBBright = -1;
            selectionBBtop = -1;
            selectionBBbottom = -1;
            repaint();
        }
    }

    /**
     * Listenes to movements of the mousewheel (for zooming)
     * @param e
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double factor = (-e.getWheelRotation()) / 10.0;
        setMetaScale(getMetaScale() * (1 + factor));
    }

    /**
     * unused
     * @param e
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        int objectNumber = getObjectAtPosition(e.getX(), e.getY());
        if (!((objectNumber == 0x7fffffff) | (objectNumber == -1))) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                if (model.isSelected(objectNumber)) model.setSelected(objectNumber, false); else model.setSelected(objectNumber, true);
                setShowSelection(true);
            } else if (e.getButton() == MouseEvent.BUTTON2) {
                if (e.isShiftDown()) {
                    model.togglePrivateBanner(objectNumber);
                    plotView.setHighlightingMode(HighlightingMode.PRIVATE_BANNER);
                } else {
                    model.togglePublicBanner(objectNumber);
                    plotView.setHighlightingMode(HighlightingMode.PUBLIC_BANNER);
                }
            }
        }
    }

    /**
     * listends to mouse-pressed events, for rotating, scrolling etc.
     * @param e
     */
    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) leftMouseButtonPressed = true;
        if (e.getButton() == MouseEvent.BUTTON3) rightMouseButtonPressed = true;
        if (isXYZplaneMode()) {
            oldMouseOnScreenPositionX = e.getXOnScreen();
            oldMouseOnScreenPositionY = e.getYOnScreen();
        }
    }

    /**
     * unused
     * @param e
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) leftMouseButtonPressed = false;
        if (e.getButton() == MouseEvent.BUTTON3) rightMouseButtonPressed = false;
        if (mouseMode == MouseMode.MULTISELECTION_PLUS) {
            multiselectionEnd = e.getPoint();
            multiselectionEnd.x += scrollOMatic.getXPosition();
            multiselectionEnd.y += scrollOMatic.getYPosition();
            mouseMode = MouseMode.NONE;
            multiselectionMode = MultiselectionMode.COLLECTING_PLUS;
            repaint();
        } else if (mouseMode == MouseMode.MULTISELECTION_MINUS) {
            multiselectionEnd = e.getPoint();
            multiselectionEnd.x += scrollOMatic.getXPosition();
            multiselectionEnd.y += scrollOMatic.getYPosition();
            mouseMode = MouseMode.NONE;
            multiselectionMode = MultiselectionMode.COLLECTING_MINUS;
            repaint();
        }
        mouseMode = MouseMode.NONE;
    }

    /**
     * unused
     * @param e
     */
    @Override
    public void mouseEntered(MouseEvent e) {
        leftMouseButtonPressed = false;
        rightMouseButtonPressed = false;
    }

    /**
     * set the picker to - when the mouse exited
     * @param e
     */
    @Override
    public void mouseExited(MouseEvent e) {
        leftMouseButtonPressed = false;
        rightMouseButtonPressed = false;
        firePickChange(-1);
    }

    /**
     * listenes to mouse drag events, for rotating
     * @param e
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        if ((mouseMode == MouseMode.MULTISELECTION_PLUS) & (!e.isShiftDown())) {
            mouseMode = MouseMode.NONE;
            multiselectionMode = MultiselectionMode.COLLECTING_PLUS;
            multiselectionEnd = e.getPoint();
            multiselectionEnd.x += scrollOMatic.getXPosition();
            multiselectionEnd.y += scrollOMatic.getYPosition();
        }
        if ((mouseMode == MouseMode.MULTISELECTION_MINUS) & (!e.isControlDown())) {
            mouseMode = MouseMode.NONE;
            multiselectionMode = MultiselectionMode.COLLECTING_PLUS;
            multiselectionEnd = e.getPoint();
            multiselectionEnd.x += scrollOMatic.getXPosition();
            multiselectionEnd.y += scrollOMatic.getYPosition();
        }
        if ((!e.isControlDown()) & (!e.isShiftDown()) & leftMouseButtonPressed) {
            if (mouseMode != MouseMode.SCROLL) {
                mouseMode = MouseMode.SCROLL;
                oldMouseOnScreenPositionX = e.getXOnScreen();
                oldMouseOnScreenPositionY = e.getYOnScreen();
            }
            int x = e.getXOnScreen();
            int y = e.getYOnScreen();
            int dx = oldMouseOnScreenPositionX - x;
            int dy = oldMouseOnScreenPositionY - y;
            oldMouseOnScreenPositionX = x;
            oldMouseOnScreenPositionY = y;
            scrollOMatic.scrollToDelta(dx, dy);
        } else if (e.isShiftDown() & (!e.isControlDown())) {
            if (mouseMode != MouseMode.MULTISELECTION_PLUS) {
                mouseMode = MouseMode.MULTISELECTION_PLUS;
                multiselectionMode = MultiselectionMode.ADJUSTING;
                multiselectionStart = e.getPoint();
                multiselectionStart.x += scrollOMatic.getXPosition();
                multiselectionStart.y += scrollOMatic.getYPosition();
                this.setShowSelection(true);
            }
            multiselectionEnd = e.getPoint();
            multiselectionEnd.x += scrollOMatic.getXPosition();
            multiselectionEnd.y += scrollOMatic.getYPosition();
            repaint();
        } else if ((!e.isShiftDown() & e.isControlDown())) {
            if (mouseMode != MouseMode.MULTISELECTION_MINUS) {
                mouseMode = MouseMode.MULTISELECTION_MINUS;
                multiselectionMode = MultiselectionMode.ADJUSTING;
                multiselectionStart = e.getPoint();
                multiselectionStart.x += scrollOMatic.getXPosition();
                multiselectionStart.y += scrollOMatic.getYPosition();
                this.setShowSelection(true);
            }
            multiselectionEnd = e.getPoint();
            multiselectionEnd.x += scrollOMatic.getXPosition();
            multiselectionEnd.y += scrollOMatic.getYPosition();
            repaint();
        } else if (rightMouseButtonPressed) {
            if (isXYZplaneMode()) {
                if (mouseMode != MouseMode.ROTATE) {
                    mouseMode = MouseMode.ROTATE;
                    oldMouseOnScreenPositionX = e.getXOnScreen();
                    oldMouseOnScreenPositionY = e.getYOnScreen();
                }
                int x = e.getXOnScreen();
                int y = e.getYOnScreen();
                int dx = oldMouseOnScreenPositionX - x;
                int dy = oldMouseOnScreenPositionY - y;
                oldMouseOnScreenPositionX = x;
                oldMouseOnScreenPositionY = y;
                setMetaRotationBeta(getMetaRotationBeta() + dx / 2);
                setMetaRotationAlpha(getMetaRotationAlpha() - dy / 2);
                isContentImageDirty = true;
                repaint();
            }
        }
    }

    /**
     * listenes to mouse movements, for picking
     * @param e
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        int xPos = e.getX();
        int yPos = e.getY();
        int objectNumber = getObjectAtPosition(xPos, yPos);
        if (objectNumber == 0x7fffffff) firePickChange(-1); else firePickChange(objectNumber);
    }

    /**
     *
     * @param pickChangeListener
     */
    public void addPickChangeListener(PickChangeListener pickChangeListener) {
        if (pickChangeListener == null) return;
        if (!pickChangeListenerList.contains(pickChangeListener)) {
            pickChangeListenerList.add(pickChangeListener);
        }
    }

    /**
     *
     * @param pickChangeListener
     */
    public void removePickChangeListener(PickChangeListener pickChangeListener) {
        if (pickChangeListener == null) return;
        if (pickChangeListenerList.contains(pickChangeListener)) {
            pickChangeListenerList.remove(pickChangeListener);
        }
    }

    /**
     *
     * @param objectNumber
     */
    public void firePickChange(int objectNumber) {
        PickChangeListener.DataPack dataPack = new PickChangeListener.DataPack();
        Molecule molecule = model.getMolecule(objectNumber);
        if (molecule != null) {
            dataPack.structure = molecule;
            dataPack.xTitle = model.getChannelTitle(X_CHANNEL);
            if (dataPack.xTitle.isEmpty()) {
                dataPack.xTitle = null;
            } else {
                dataPack.xValue = model.getData(X_CHANNEL, objectNumber);
            }
            dataPack.yTitle = model.getChannelTitle(Y_CHANNEL);
            if (dataPack.yTitle.isEmpty()) {
                dataPack.yTitle = null;
            } else {
                dataPack.yValue = model.getData(Y_CHANNEL, objectNumber);
            }
            dataPack.zTitle = model.getChannelTitle(Z_CHANNEL);
            if (dataPack.zTitle.isEmpty()) {
                dataPack.zTitle = null;
            } else {
                dataPack.zValue = model.getData(Z_CHANNEL, objectNumber);
            }
            dataPack.colorTitle = model.getChannelTitle(COLOR_CHANNEL);
            if (dataPack.colorTitle.isEmpty()) {
                dataPack.colorTitle = null;
            } else {
                dataPack.colorValue = model.getData(COLOR_CHANNEL, objectNumber);
            }
            dataPack.sizeTitle = model.getChannelTitle(SIZE_CHANNEL);
            if (dataPack.sizeTitle.isEmpty()) {
                dataPack.sizeTitle = null;
            } else {
                dataPack.sizeValue = model.getData(SIZE_CHANNEL, objectNumber);
            }
        }
        for (PickChangeListener listener : pickChangeListenerList) {
            listener.pickChanged(dataPack);
        }
    }

    class ScrollOMatic implements AdjustmentListener {

        private int viewSize, viewportWidth, viewportHeight;

        private int xPosition, yPosition;

        private JScrollBar horizontalScrollBar, verticalScrollBar;

        private boolean isAdjusting;

        public ScrollOMatic() {
            setLayout(new BorderLayout());
            horizontalScrollBar = new JScrollBar(JScrollBar.HORIZONTAL);
            horizontalScrollBar.setVisible(false);
            horizontalScrollBar.addAdjustmentListener(this);
            add(horizontalScrollBar, BorderLayout.SOUTH);
            verticalScrollBar = new JScrollBar(JScrollBar.VERTICAL);
            verticalScrollBar.setVisible(false);
            verticalScrollBar.addAdjustmentListener(this);
            add(verticalScrollBar, BorderLayout.EAST);
            xPosition = 0;
            yPosition = 0;
            viewSize = 1;
            isAdjusting = false;
        }

        /**
         * 
         */
        public void adjustMetric() {
            int newViewSize;
            int newViewportWidth = getWidth();
            int newViewportHeight = getHeight();
            isAdjusting = true;
            {
                double metaspaceSize = getMetaspaceSize();
                double pointvectorLength = metaspaceSize * 0.707106781 * metaScale;
                double axisScaleX = getDotspaceScaleX();
                double axisScaleY = getDotspaceScaleY();
                double axisScaleZ = getDotspaceScaleZ();
                double axisScale = (axisScaleX > axisScaleY ? axisScaleX : axisScaleY);
                axisScale = (axisScale > axisScaleZ ? axisScale : axisScaleZ);
                newViewSize = (int) (pointvectorLength * axisScale / (isXYZplaneMode() ? 0.55 : 0.8));
            }
            if ((newViewSize != viewSize) | (newViewportWidth != viewportWidth) | (newViewportHeight != viewportHeight)) {
                int viewCenterX, viewCenterY;
                int newViewCenterX, newViewCenterY;
                int newXPosition, newYPosition;
                Point mouse = getMousePosition();
                if (mouse == null) {
                    viewCenterX = xPosition + viewportWidth / 2;
                    viewCenterY = yPosition + viewportHeight / 2;
                } else {
                    if ((mouse.x > (viewportWidth - verticalScrollBar.getWidth())) | (mouse.y > (viewportHeight - horizontalScrollBar.getHeight()))) {
                        viewCenterX = xPosition + viewportWidth / 2;
                        viewCenterY = yPosition + viewportHeight / 2;
                    } else {
                        viewCenterX = xPosition + mouse.x;
                        viewCenterY = yPosition + mouse.y;
                    }
                }
                newViewCenterX = newViewSize * viewCenterX / viewSize;
                newViewCenterY = newViewSize * viewCenterY / viewSize;
                newXPosition = newViewCenterX - (viewCenterX - xPosition);
                newYPosition = newViewCenterY - (viewCenterY - yPosition);
                if (newXPosition < 0) newXPosition = 0;
                if (newYPosition < 0) newYPosition = 0;
                if (newXPosition + newViewportWidth > newViewSize) newXPosition = newViewSize - newViewportWidth;
                if (newYPosition + newViewportHeight > newViewSize) newYPosition = newViewSize - newViewportHeight;
                if (newViewSize < newViewportWidth) newXPosition = -(newViewportWidth - newViewSize) / 2;
                if (newViewSize < newViewportHeight) newYPosition = -(newViewportHeight - newViewSize) / 2;
                viewSize = newViewSize;
                viewportWidth = newViewportWidth;
                viewportHeight = newViewportHeight;
                xPosition = newXPosition;
                yPosition = newYPosition;
                horizontalScrollBar.setMaximum(viewSize);
                horizontalScrollBar.setVisibleAmount(viewportWidth);
                horizontalScrollBar.setValue(xPosition);
                horizontalScrollBar.setVisible(viewSize > viewportWidth);
                verticalScrollBar.setMaximum(viewSize);
                verticalScrollBar.setVisibleAmount(viewportHeight);
                verticalScrollBar.setValue(yPosition);
                verticalScrollBar.setVisible(viewSize > viewportHeight);
            }
            isAdjusting = false;
        }

        /**
         * 
         * @return
         *  the size of the metaspace
         */
        public double getMetaspaceSize() {
            return Math.min(getWidth(), getHeight()) * (isXYZplaneMode() ? 0.55 : 0.8);
        }

        public int getViewportWidth() {
            return viewportWidth;
        }

        public int getViewportHeight() {
            return viewportHeight;
        }

        public int getViewSize() {
            return viewSize;
        }

        public int getXPosition() {
            return xPosition;
        }

        public int getYPosition() {
            return yPosition;
        }

        public int getDiagramCenterX() {
            return viewSize / 2;
        }

        public int getDiagramCenterY() {
            return viewSize / 2;
        }

        @Override
        public void adjustmentValueChanged(AdjustmentEvent event) {
            if (!isAdjusting) {
                boolean repaint = false;
                if (event.getSource() == horizontalScrollBar) {
                    if (xPosition != event.getValue()) {
                        xPosition = event.getValue();
                        repaint = true;
                    }
                } else if (event.getSource() == verticalScrollBar) {
                    if (yPosition != event.getValue()) {
                        yPosition = event.getValue();
                        repaint = true;
                    }
                }
                if (repaint) {
                    repaint();
                }
            }
        }

        /**
         * 
         * @return
         *  the width
         */
        public int width() {
            int w = getViewportWidth() - verticalScrollBar.getWidth();
            return w;
        }

        public int dWidth() {
            return horizontalScrollBar.getVisibleAmount();
        }

        /**
         * 
         * @return
         *  the height
         */
        public int height() {
            int h = getViewportHeight() - horizontalScrollBar.getHeight();
            return h;
        }

        public int dHeight() {
            return verticalScrollBar.getVisibleAmount();
        }

        /**
         * 
         * @param x
         * @param y
         */
        void scrollTo(double scaleFactor, int x, int y) {
            isAdjusting = true;
            setMetaScale(getMetaScale() * scaleFactor);
            viewSize = (int) (viewSize * scaleFactor);
            xPosition = x;
            yPosition = y;
            horizontalScrollBar.setMaximum(viewSize);
            horizontalScrollBar.setVisibleAmount(viewportWidth);
            horizontalScrollBar.setValue(xPosition);
            horizontalScrollBar.setVisible(viewSize > viewportWidth);
            verticalScrollBar.setMaximum(viewSize);
            verticalScrollBar.setVisibleAmount(viewportHeight);
            verticalScrollBar.setValue(yPosition);
            verticalScrollBar.setVisible(viewSize > viewportHeight);
            isAdjusting = false;
            repaint();
        }

        /**
         * 
         * @param deltaX
         * @param deltaY
         */
        void scrollToDelta(int deltaX, int deltaY) {
            int nxp = xPosition + deltaX;
            int nyp = yPosition + deltaY;
            boolean repaintNeccessary = false;
            if ((nxp > 0) & (nxp < viewSize)) {
                isAdjusting = true;
                xPosition = nxp;
                repaintNeccessary = true;
            }
            if ((nyp > 0) & (nyp < viewSize)) {
                isAdjusting = true;
                yPosition = nyp;
                repaintNeccessary = true;
            }
            if (repaintNeccessary) {
                horizontalScrollBar.setValue(xPosition);
                verticalScrollBar.setValue(yPosition);
                isAdjusting = false;
                repaint();
            }
        }
    }

    /**
     * @param kMeansMap the kMeansMap to set
     */
    public void setkMeansMap(HashMap<Molecule, Integer> kMeansMap) {
        this.kMeansMap = kMeansMap;
    }
}
