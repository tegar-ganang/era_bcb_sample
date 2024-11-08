package uk.ac.imperial.ma.metric.explorations.numerics;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import java.awt.Color;
import uk.ac.imperial.ma.metric.mathematics.numerics.Interpolation;
import uk.ac.imperial.ma.metric.plotting.*;
import uk.ac.imperial.ma.metric.parsing.*;
import uk.ac.imperial.ma.metric.util.MathsStyleHelper;
import uk.ac.imperial.ma.metric.gui.*;
import uk.ac.imperial.ma.metric.explorations.ExplorationInterface;

/**
 *
 *
 * @author Phil Ramsden
 * @author Daniel J. R. May
 * @version 0.2.0 26 March 2004
 */
public class NewInterpolationExploration extends JPanel implements ExplorationInterface, ActionListener, KeyListener, ItemListener, MathPainterPanelListener {

    private static final ExtendedGridBagLayout mainPanelLayout = new ExtendedGridBagLayout();

    private static final ExtendedGridBagLayout contentSettingsPanelLayout = new ExtendedGridBagLayout();

    private static final ExtendedGridBagLayout windowSettingsPanelLayout = new ExtendedGridBagLayout();

    private static final ExtendedGridBagLayout controlPanelLayout = new ExtendedGridBagLayout();

    private static final ExtendedGridBagLayout stylePanelLayout = new ExtendedGridBagLayout();

    ClickableMathPainterPanel graphicsPanel;

    MathPainter mathPainter;

    GridPlotter gridPlotter;

    AxesPlotter axesPlotter;

    CoordGenerator coordGenerator;

    CurvePlotter curvePlotter;

    PointPlotter pointPlotter;

    MathCoords mathCoords;

    JPanel contentSettingsPanel = new JPanel(true);

    JPanel windowSettingsPanel = new JPanel(true);

    JPanel controlPanel = new JPanel(true);

    JFrame styleFrame = new JFrame();

    JPanel stylePanel = new JPanel(true);

    Dimension imageDimension;

    private static final int TEXT_SIZE = 16;

    private static final int MATH_SIZE = 18;

    private static final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, TEXT_SIZE - 2);

    private static final Font FIELD_FONT = new Font("SansSerif", Font.PLAIN, TEXT_SIZE);

    private static final boolean ADD_POINT = true;

    private static final boolean DELETE_POINT = false;

    protected int currentTextSize;

    protected int currentMathSize;

    protected Font currentLabelFont;

    protected Font currentFieldFont;

    String[] parseVariables = { "x" };

    Color[] defaultColorWheel = { Color.BLACK, Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA, Color.CYAN };

    int defaultColorWheelSize = 7;

    int defaultColorIndex = 0;

    int plotIndex = 1;

    JLabel degreeLbl = new JLabel("degree");

    JTextField degreeTFd = new JTextField("");

    JCheckBox lagrangianCBx = new JCheckBox("Lagrangian", true);

    JRadioButton addPointRBtn = new JRadioButton("clicking adds", true);

    JRadioButton deletePointRBtn = new JRadioButton("clicking deletes", false);

    ButtonGroup clickActionBGp = new ButtonGroup();

    JLabel xNMeshLbl = new JLabel("plotting mesh");

    JTextField xNMeshTFd = new JTextField("80");

    JButton localStylesBtn = new JButton("Plot Styles...");

    JFrame plotStyleFrame;

    JPanel plotStylePanel;

    JLabel plotLabelLbl = new JLabel("Plot label");

    JTextField plotLabelTFd = new JTextField("Plot");

    JLabel scalingLbl = new JLabel("tgt scaling");

    JTextField scalingTFd = new JTextField("0.3");

    SelectColorButton plotColorBtn = new SelectColorButton(Color.BLACK, "Plot Colour...");

    SelectColorButton dataColorBtn = new SelectColorButton(Color.RED, "Data Colour...");

    String plotLabel;

    Color plotColor = Color.BLACK;

    boolean settingsAltered;

    boolean lagrangian = true;

    boolean mouseDragged = false;

    boolean clickAction = ADD_POINT;

    double yMin;

    double yMax;

    int degree = 1;

    int nInterpolants = 0;

    double[] xdata;

    double[] ydata;

    double[] dragXdata;

    double[] dragYdata;

    int nPoints = 0;

    int dragIndex = -1;

    double previousX = Double.NEGATIVE_INFINITY;

    double nextX = Double.POSITIVE_INFINITY;

    JLabel windowXMinLbl = new JLabel("xMin");

    JTextField windowXMinTFd = new JTextField("-3.0        ");

    JLabel windowXMaxLbl = new JLabel("xMax");

    JTextField windowXMaxTFd = new JTextField("3.0        ");

    JLabel windowYMinLbl = new JLabel("yMin");

    JTextField windowYMinTFd = new JTextField("-18.0        ");

    JLabel windowYMaxLbl = new JLabel("yMax");

    JTextField windowYMaxTFd = new JTextField("18.0        ");

    JButton globalStylesBtn = new JButton("Global Styles...");

    private double windowXMin;

    private double windowXMax;

    private double windowYMin;

    private double windowYMax;

    private int xNMesh;

    JButton eraseCurvesBtn = new JButton("Erase");

    JButton drawCurvesBtn = new JButton("Draw");

    JButton autoscaleBtn = new JButton("Scale");

    JButton textPlusBtn = new JButton("   Text+   ");

    JButton textMinusBtn = new JButton("   Text-   ");

    public NewInterpolationExploration() {
        super(true);
        currentTextSize = TEXT_SIZE;
        currentMathSize = MATH_SIZE;
        currentLabelFont = LABEL_FONT;
        currentFieldFont = FIELD_FONT;
        graphicsPanel = new ClickableMathPainterPanel();
        graphicsPanel.addMathPainterPanelListener(this);
        contentSettingsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED), "Content Settings", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, LABEL_FONT));
        contentSettingsPanel.setLayout(contentSettingsPanelLayout);
        plotLabelLbl.setFont(LABEL_FONT);
        plotLabelTFd.setFont(FIELD_FONT);
        scalingLbl.setFont(LABEL_FONT);
        scalingTFd.setFont(FIELD_FONT);
        plotColorBtn.setButtonFont(LABEL_FONT);
        dataColorBtn.setButtonFont(LABEL_FONT);
        degreeLbl.setFont(LABEL_FONT);
        lagrangianCBx.setFont(LABEL_FONT);
        addPointRBtn.setFont(LABEL_FONT);
        deletePointRBtn.setFont(LABEL_FONT);
        lagrangianCBx.setFont(LABEL_FONT);
        xNMeshLbl.setFont(LABEL_FONT);
        localStylesBtn.setFont(LABEL_FONT);
        degreeTFd.setFont(FIELD_FONT);
        xNMeshTFd.setFont(FIELD_FONT);
        degreeTFd.setEditable(!lagrangian);
        degreeTFd.addKeyListener(this);
        xNMeshTFd.addKeyListener(this);
        plotLabelTFd.addKeyListener(this);
        scalingTFd.addKeyListener(this);
        lagrangianCBx.addItemListener(this);
        localStylesBtn.addActionListener(this);
        addPointRBtn.addActionListener(this);
        deletePointRBtn.addActionListener(this);
        clickActionBGp.add(addPointRBtn);
        clickActionBGp.add(deletePointRBtn);
        contentSettingsPanelLayout.add(degreeLbl, contentSettingsPanel, 0, 0, 1, 1, 100, 100, ExtendedGridBagLayout.NONE, ExtendedGridBagLayout.EAST);
        contentSettingsPanelLayout.add(degreeTFd, contentSettingsPanel, 1, 0, 1, 1, 100, 100, ExtendedGridBagLayout.HORIZONTAL, ExtendedGridBagLayout.CENTER);
        contentSettingsPanelLayout.add(lagrangianCBx, contentSettingsPanel, 2, 0, 2, 1, 100, 100, ExtendedGridBagLayout.NONE, ExtendedGridBagLayout.EAST);
        contentSettingsPanelLayout.add(addPointRBtn, contentSettingsPanel, 0, 1, 2, 1, 100, 100, ExtendedGridBagLayout.HORIZONTAL, ExtendedGridBagLayout.CENTER);
        contentSettingsPanelLayout.add(deletePointRBtn, contentSettingsPanel, 2, 1, 2, 1, 100, 100, ExtendedGridBagLayout.HORIZONTAL, ExtendedGridBagLayout.CENTER);
        contentSettingsPanelLayout.add(xNMeshLbl, contentSettingsPanel, 0, 2, 1, 1, 100, 100, ExtendedGridBagLayout.NONE, ExtendedGridBagLayout.EAST);
        contentSettingsPanelLayout.add(xNMeshTFd, contentSettingsPanel, 1, 2, 1, 1, 100, 100, ExtendedGridBagLayout.HORIZONTAL, ExtendedGridBagLayout.CENTER);
        contentSettingsPanelLayout.add(localStylesBtn, contentSettingsPanel, 1, 3, 2, 1, 100, 100, ExtendedGridBagLayout.HORIZONTAL, ExtendedGridBagLayout.CENTER);
        windowSettingsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED), "Visual Settings", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, LABEL_FONT));
        windowSettingsPanel.setLayout(windowSettingsPanelLayout);
        windowXMinLbl.setFont(LABEL_FONT);
        windowXMaxLbl.setFont(LABEL_FONT);
        windowYMinLbl.setFont(LABEL_FONT);
        windowYMaxLbl.setFont(LABEL_FONT);
        globalStylesBtn.setFont(LABEL_FONT);
        windowXMinTFd.setFont(FIELD_FONT);
        windowXMaxTFd.setFont(FIELD_FONT);
        windowYMinTFd.setFont(FIELD_FONT);
        windowYMaxTFd.setFont(FIELD_FONT);
        windowSettingsPanelLayout.add(windowXMinLbl, windowSettingsPanel, 0, 0, 1, 1, 100, 100, ExtendedGridBagLayout.NONE, ExtendedGridBagLayout.EAST);
        windowSettingsPanelLayout.add(windowXMinTFd, windowSettingsPanel, 1, 0, 1, 1, 100, 100, ExtendedGridBagLayout.HORIZONTAL, ExtendedGridBagLayout.CENTER);
        windowSettingsPanelLayout.add(windowXMaxLbl, windowSettingsPanel, 2, 0, 1, 1, 100, 100, ExtendedGridBagLayout.NONE, ExtendedGridBagLayout.EAST);
        windowSettingsPanelLayout.add(windowXMaxTFd, windowSettingsPanel, 3, 0, 1, 1, 100, 100, ExtendedGridBagLayout.HORIZONTAL, ExtendedGridBagLayout.CENTER);
        windowSettingsPanelLayout.add(windowYMinLbl, windowSettingsPanel, 0, 1, 1, 1, 100, 100, ExtendedGridBagLayout.NONE, ExtendedGridBagLayout.EAST);
        windowSettingsPanelLayout.add(windowYMinTFd, windowSettingsPanel, 1, 1, 1, 1, 100, 100, ExtendedGridBagLayout.HORIZONTAL, ExtendedGridBagLayout.CENTER);
        windowSettingsPanelLayout.add(windowYMaxLbl, windowSettingsPanel, 2, 1, 1, 1, 100, 100, ExtendedGridBagLayout.NONE, ExtendedGridBagLayout.EAST);
        windowSettingsPanelLayout.add(windowYMaxTFd, windowSettingsPanel, 3, 1, 1, 1, 100, 100, ExtendedGridBagLayout.HORIZONTAL, ExtendedGridBagLayout.CENTER);
        windowSettingsPanelLayout.add(globalStylesBtn, windowSettingsPanel, 1, 2, 2, 1, 100, 100, ExtendedGridBagLayout.HORIZONTAL, ExtendedGridBagLayout.CENTER);
        windowXMinTFd.addKeyListener(this);
        windowXMaxTFd.addKeyListener(this);
        windowYMinTFd.addKeyListener(this);
        windowYMaxTFd.addKeyListener(this);
        globalStylesBtn.addActionListener(this);
        controlPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED), "Controls", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, LABEL_FONT));
        controlPanel.setLayout(controlPanelLayout);
        drawCurvesBtn.setFont(LABEL_FONT);
        eraseCurvesBtn.setFont(LABEL_FONT);
        autoscaleBtn.setFont(LABEL_FONT);
        controlPanelLayout.add(drawCurvesBtn, controlPanel, 0, 0, 1, 1, 100, 100, ExtendedGridBagLayout.HORIZONTAL, ExtendedGridBagLayout.CENTER);
        controlPanelLayout.add(eraseCurvesBtn, controlPanel, 1, 0, 1, 1, 100, 100, ExtendedGridBagLayout.HORIZONTAL, ExtendedGridBagLayout.CENTER);
        controlPanelLayout.add(autoscaleBtn, controlPanel, 2, 0, 1, 1, 100, 100, ExtendedGridBagLayout.HORIZONTAL, ExtendedGridBagLayout.CENTER);
        drawCurvesBtn.addActionListener(this);
        eraseCurvesBtn.addActionListener(this);
        autoscaleBtn.addActionListener(this);
        this.setLayout(mainPanelLayout);
        mainPanelLayout.add(contentSettingsPanel, this, 0, 0, 1, 1, 10, 100, ExtendedGridBagLayout.BOTH, ExtendedGridBagLayout.CENTER);
        mainPanelLayout.add(windowSettingsPanel, this, 0, 1, 1, 1, 10, 10, ExtendedGridBagLayout.BOTH, ExtendedGridBagLayout.CENTER);
        mainPanelLayout.add(graphicsPanel, this, 1, 0, 1, 1, 100, 100, ExtendedGridBagLayout.BOTH, ExtendedGridBagLayout.CENTER);
        mainPanelLayout.add(controlPanel, this, 1, 1, 1, 1, 100, 10, ExtendedGridBagLayout.BOTH, ExtendedGridBagLayout.CENTER);
        textPlusBtn.setFont(LABEL_FONT);
        textMinusBtn.setFont(LABEL_FONT);
        textPlusBtn.addActionListener(this);
        textMinusBtn.addActionListener(this);
    }

    public void setupGraphics() {
        this.coordGenerator = new CoordGenerator(this.mathPainter);
        this.mathCoords = new MathCoords(this.mathPainter);
        this.curvePlotter = new CurvePlotter(this.mathPainter, this.coordGenerator);
        this.pointPlotter = new PointPlotter(this.mathPainter, this.mathCoords);
    }

    public void init() {
        mathPainter = graphicsPanel.init();
        this.initializeGraphics();
        setupGraphics();
        this.drawGraphPaper();
        this.graphicsPanel.setBase();
        this.graphicsPanel.update();
    }

    public void initializeGraphics() {
        this.gridPlotter = new GridPlotter(this.mathPainter);
        this.axesPlotter = new AxesPlotter(this.mathPainter);
        windowXMin = new Double(windowXMinTFd.getText()).doubleValue();
        windowXMax = new Double(windowXMaxTFd.getText()).doubleValue();
        windowYMin = new Double(windowYMinTFd.getText()).doubleValue();
        windowYMax = new Double(windowYMaxTFd.getText()).doubleValue();
        xNMesh = new Integer(xNMeshTFd.getText()).intValue();
        mathPainter.setMathArea(windowXMin, windowYMin, windowXMax - windowXMin, windowYMax - windowYMin);
        mathPainter.setScales();
    }

    public void drawGraphPaper() {
        mathPainter.setPaint(Color.white);
        mathPainter.fillRect(windowXMin, windowYMin, windowXMax - windowXMin, windowYMax - windowYMin);
        mathPainter.setPaint(Color.lightGray);
        gridPlotter.drawFineGrid();
        mathPainter.setPaint(Color.gray);
        gridPlotter.drawGrid();
        mathPainter.setPaint(Color.blue);
        axesPlotter.drawAxes();
        axesPlotter.drawTicks(new Font("SansSerif", Font.PLAIN, currentTextSize));
    }

    public void draw() {
        graphicsPanel.clearCompletely();
        initializeGraphics();
        drawGraphPaper();
        graphicsPanel.setBase();
        mathPainter.setPaint(dataColorBtn.getColor());
        pointPlotter.plot();
        if (nPoints > 1) {
            drawInterpolants(xdata, ydata);
        }
        graphicsPanel.update();
    }

    public Component getComponent() {
        return this;
    }

    public void mathPainterPanelResized() {
        System.out.println("resized");
        draw();
    }

    public void itemStateChanged(ItemEvent ie) {
        if (lagrangian) {
            degreeTFd.setText("" + degree);
        } else {
            degree = new Integer(degreeTFd.getText()).intValue();
            degreeTFd.setText("");
        }
        lagrangian = !lagrangian;
        degreeTFd.setEditable(!lagrangian);
        draw();
    }

    public void keyPressed(KeyEvent ke) {
    }

    public void keyTyped(KeyEvent ke) {
    }

    public void keyReleased(KeyEvent ke) {
        if (ke.getKeyCode() == ke.VK_ENTER) {
            draw();
        }
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == localStylesBtn) {
            plotStyleFrame = new JFrame("Plot Style Settings");
            plotStylePanel = new JPanel();
            plotStylePanel.setLayout(stylePanelLayout);
            stylePanelLayout.add(plotLabelLbl, plotStylePanel, 0, 0, 1, 1, 100, 100, ExtendedGridBagLayout.NONE, ExtendedGridBagLayout.CENTER);
            stylePanelLayout.add(plotLabelTFd, plotStylePanel, 1, 0, 1, 1, 100, 100, ExtendedGridBagLayout.HORIZONTAL, ExtendedGridBagLayout.CENTER);
            stylePanelLayout.add(scalingLbl, plotStylePanel, 0, 1, 1, 1, 100, 100, ExtendedGridBagLayout.NONE, ExtendedGridBagLayout.CENTER);
            stylePanelLayout.add(scalingTFd, plotStylePanel, 1, 1, 1, 1, 100, 100, ExtendedGridBagLayout.HORIZONTAL, ExtendedGridBagLayout.CENTER);
            stylePanelLayout.add(plotColorBtn, plotStylePanel, 0, 3, 2, 1, 100, 100, ExtendedGridBagLayout.HORIZONTAL, ExtendedGridBagLayout.CENTER);
            stylePanelLayout.add(dataColorBtn, plotStylePanel, 0, 4, 2, 1, 100, 100, ExtendedGridBagLayout.HORIZONTAL, ExtendedGridBagLayout.CENTER);
            plotStyleFrame.getContentPane().setLayout(new GridLayout(1, 0));
            plotStyleFrame.add(plotStylePanel);
            plotStyleFrame.setSize(300, 300);
            plotStyleFrame.setVisible(true);
        } else if (ae.getSource() == addPointRBtn || ae.getSource() == deletePointRBtn) {
            clickAction = !clickAction;
        } else if (ae.getSource() == eraseCurvesBtn) {
            graphicsPanel.clearCompletely();
            initializeGraphics();
            drawGraphPaper();
            graphicsPanel.setBase();
            graphicsPanel.update();
        } else if (ae.getSource() == drawCurvesBtn) {
            draw();
        } else if (ae.getSource() == globalStylesBtn) {
            styleFrame = new JFrame("Global Style Settings");
            stylePanel = new JPanel();
            stylePanel.setLayout(stylePanelLayout);
            stylePanelLayout.add(textPlusBtn, stylePanel, 0, 0, 1, 1, 100, 100, ExtendedGridBagLayout.NONE, ExtendedGridBagLayout.CENTER);
            stylePanelLayout.add(textMinusBtn, stylePanel, 1, 0, 1, 1, 100, 100, ExtendedGridBagLayout.NONE, ExtendedGridBagLayout.CENTER);
            styleFrame.getContentPane().setLayout(new GridLayout(1, 0));
            styleFrame.add(stylePanel);
            styleFrame.setSize(300, 300);
            styleFrame.setVisible(true);
        } else if (ae.getSource() == textPlusBtn) {
            currentTextSize++;
            currentMathSize++;
            updateFonts();
        } else if (ae.getSource() == textMinusBtn) {
            currentTextSize--;
            currentMathSize--;
            updateFonts();
        } else if (ae.getSource() == autoscaleBtn) {
            System.out.println("autoscale button pressed");
            double newYMin = 1.1 * yMin - 0.1 * yMax;
            double newYMax = 1.1 * yMax - 0.1 * yMin;
            newYMin = Math.round(1000.0 * newYMin) / 1000.0;
            newYMax = Math.round(1000.0 * newYMax) / 1000.0;
            windowYMinTFd.setText("" + newYMin);
            windowYMaxTFd.setText("" + newYMax);
            draw();
        } else {
            try {
                draw();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void updateFonts() {
        currentLabelFont = new Font("SansSerif", Font.BOLD, currentTextSize - 2);
        currentFieldFont = new Font("SansSerif", Font.PLAIN, currentTextSize);
        contentSettingsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED), "Content Settings", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, this.currentLabelFont));
        degreeLbl.setFont(this.currentLabelFont);
        lagrangianCBx.setFont(this.currentLabelFont);
        addPointRBtn.setFont(this.currentLabelFont);
        deletePointRBtn.setFont(this.currentLabelFont);
        lagrangianCBx.setFont(this.currentLabelFont);
        xNMeshLbl.setFont(this.currentLabelFont);
        localStylesBtn.setFont(this.currentLabelFont);
        degreeTFd.setFont(this.currentFieldFont);
        xNMeshTFd.setFont(this.currentFieldFont);
        plotLabelLbl.setFont(this.currentLabelFont);
        plotLabelTFd.setFont(this.currentFieldFont);
        scalingLbl.setFont(this.currentLabelFont);
        scalingTFd.setFont(this.currentFieldFont);
        plotColorBtn.setButtonFont(this.currentLabelFont);
        dataColorBtn.setButtonFont(this.currentLabelFont);
        windowSettingsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED), "Visual Settings", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, currentLabelFont));
        windowXMinLbl.setFont(currentLabelFont);
        windowXMaxLbl.setFont(currentLabelFont);
        windowYMinLbl.setFont(currentLabelFont);
        windowYMaxLbl.setFont(currentLabelFont);
        globalStylesBtn.setFont(currentLabelFont);
        windowXMinTFd.setFont(currentFieldFont);
        windowXMaxTFd.setFont(currentFieldFont);
        windowYMinTFd.setFont(currentFieldFont);
        windowYMaxTFd.setFont(currentFieldFont);
        controlPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED), "Controls", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, currentLabelFont));
        drawCurvesBtn.setFont(currentLabelFont);
        eraseCurvesBtn.setFont(currentLabelFont);
        autoscaleBtn.setFont(currentLabelFont);
        textPlusBtn.setFont(currentLabelFont);
        textMinusBtn.setFont(currentLabelFont);
        graphicsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED), " Plots", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, currentLabelFont));
    }

    public void mathPainterPanelAction(MathPainterPanelEvent mppe) {
        MouseEvent me = mppe.getMouseEvent();
        int eventType = me.getID();
        if (eventType == MouseEvent.MOUSE_PRESSED) mousePressedAction(mppe); else if (eventType == MouseEvent.MOUSE_DRAGGED) mouseDraggedAction(mppe); else if (eventType == MouseEvent.MOUSE_CLICKED) mouseClickedAction(mppe); else if (eventType == MouseEvent.MOUSE_RELEASED) mouseReleasedAction(mppe);
    }

    private int insertIndex(double[] sortedData, double insert) {
        for (int i = 0; i < sortedData.length; i++) {
            if (sortedData[i] > insert) return i;
        }
        return sortedData.length;
    }

    private void drawInterpolants(double[] data1, double[] data2) {
        yMin = Double.POSITIVE_INFINITY;
        yMax = Double.NEGATIVE_INFINITY;
        int workingDegree;
        int workingMesh;
        if (lagrangian) workingDegree = nPoints - 1; else workingDegree = new Integer(degreeTFd.getText()).intValue();
        nInterpolants = (nPoints - 1) / workingDegree;
        for (int interpolantIndex = 0; interpolantIndex < nInterpolants; interpolantIndex++) {
            double[] workingXData = new double[workingDegree + 1];
            double[] workingYData = new double[workingDegree + 1];
            double workingXMin;
            double workingXMax;
            for (int i = 0; i < workingDegree + 1; i++) {
                workingXData[i] = data1[interpolantIndex * workingDegree + i];
                workingYData[i] = data2[interpolantIndex * workingDegree + i];
            }
            if (interpolantIndex == 0) workingXMin = windowXMin; else workingXMin = data1[interpolantIndex * workingDegree];
            if (interpolantIndex == nInterpolants - 1) workingXMax = windowXMax; else workingXMax = data1[(interpolantIndex + 1) * workingDegree];
            workingMesh = (int) (Math.ceil((double) (xNMesh) * (workingXMax - workingXMin) / (windowXMax - windowXMin)));
            Interpolation interpolation = new Interpolation(workingXData, workingYData);
            String funcString = interpolation.polynomial("x");
            try {
                coordGenerator.setPoints(funcString, "x", workingXMin, workingXMax - workingXMin, workingMesh);
                mathPainter.setPaint(plotColorBtn.getColor());
                curvePlotter.plot();
                yMin = Math.min(yMin, coordGenerator.getYMin());
                yMax = Math.max(yMax, coordGenerator.getYMax());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void mouseClickedAction(MathPainterPanelEvent mppe) {
        if (clickAction == ADD_POINT) {
            if (nPoints == 0) {
                xdata = new double[1];
                ydata = new double[1];
                xdata[0] = mppe.getMathSpaceX();
                ydata[0] = mppe.getMathSpaceY();
            } else {
                double newX = mppe.getMathSpaceX();
                double newY = mppe.getMathSpaceY();
                double[] newXData = new double[nPoints + 1];
                double[] newYData = new double[nPoints + 1];
                int ii = insertIndex(xdata, newX);
                for (int i = 0; i < ii; i++) {
                    newXData[i] = xdata[i];
                    newYData[i] = ydata[i];
                }
                newXData[ii] = newX;
                newYData[ii] = newY;
                for (int i = ii + 1; i < nPoints + 1; i++) {
                    newXData[i] = xdata[i - 1];
                    newYData[i] = ydata[i - 1];
                }
                xdata = newXData;
                ydata = newYData;
            }
            nPoints++;
            mathCoords.setPoints(xdata, ydata);
            graphicsPanel.clear();
            mathPainter.setPaint(dataColorBtn.getColor());
            pointPlotter.plot();
            if (nPoints > 1) {
                drawInterpolants(xdata, ydata);
            }
            graphicsPanel.update();
        } else {
            if (nPoints == 0) {
            } else {
                int deleteIndex = -1;
                for (int i = 0; i < nPoints; i++) {
                    if ((Math.abs(mppe.getUserSpaceX() - mathPainter.mathToUserX(xdata[i])) <= 5) && (Math.abs(mppe.getUserSpaceY() - mathPainter.mathToUserY(ydata[i])) <= 5)) {
                        deleteIndex = i;
                        break;
                    }
                }
                double[] newXData = new double[nPoints - 1];
                double[] newYData = new double[nPoints - 1];
                for (int i = 0; i < deleteIndex; i++) {
                    newXData[i] = xdata[i];
                    newYData[i] = ydata[i];
                }
                for (int i = deleteIndex; i < nPoints - 1; i++) {
                    newXData[i] = xdata[i + 1];
                    newYData[i] = ydata[i + 1];
                }
                xdata = newXData;
                ydata = newYData;
                nPoints--;
                mathCoords.setPoints(xdata, ydata);
                graphicsPanel.clear();
                mathPainter.setPaint(dataColorBtn.getColor());
                pointPlotter.plot();
                if (nPoints > 1) {
                    drawInterpolants(xdata, ydata);
                }
                graphicsPanel.update();
            }
        }
    }

    public void mousePressedAction(MathPainterPanelEvent mppe) {
        for (int i = 0; i < nPoints; i++) {
            if ((Math.abs(mppe.getUserSpaceX() - mathPainter.mathToUserX(xdata[i])) <= 5) && (Math.abs(mppe.getUserSpaceY() - mathPainter.mathToUserY(ydata[i])) <= 5)) {
                dragIndex = i;
                if (dragIndex == 0) previousX = Double.NEGATIVE_INFINITY; else previousX = xdata[dragIndex - 1];
                if (dragIndex == nPoints - 1) nextX = Double.POSITIVE_INFINITY; else nextX = xdata[dragIndex + 1];
                break;
            }
        }
    }

    public void mouseDraggedAction(MathPainterPanelEvent mppe) {
        mouseDragged = true;
        if (nPoints == 0) {
        } else if (nPoints == 1) {
            xdata[0] = mppe.getMathSpaceX();
            ydata[0] = mppe.getMathSpaceY();
            mathCoords.setPoints(xdata, ydata);
            graphicsPanel.clear();
            mathPainter.setPaint(dataColorBtn.getColor());
            pointPlotter.plot();
        } else {
            double thisX = mppe.getMathSpaceX();
            double thisY = mppe.getMathSpaceY();
            dragXdata = new double[nPoints];
            dragYdata = new double[nPoints];
            if (thisX > previousX && thisX < nextX) {
                for (int i = 0; i < dragIndex; i++) {
                    dragXdata[i] = xdata[i];
                    dragYdata[i] = ydata[i];
                }
                dragXdata[dragIndex] = thisX;
                dragYdata[dragIndex] = thisY;
                for (int i = dragIndex + 1; i < nPoints; i++) {
                    dragXdata[i] = xdata[i];
                    dragYdata[i] = ydata[i];
                }
            } else if (thisX < previousX) {
                int ii = insertIndex(xdata, thisX);
                for (int i = 0; i < ii; i++) {
                    dragXdata[i] = xdata[i];
                    dragYdata[i] = ydata[i];
                }
                dragXdata[ii] = thisX;
                dragYdata[ii] = thisY;
                for (int i = ii + 1; i < dragIndex + 1; i++) {
                    dragXdata[i] = xdata[i - 1];
                    dragYdata[i] = ydata[i - 1];
                }
                for (int i = dragIndex + 1; i < nPoints; i++) {
                    dragXdata[i] = xdata[i];
                    dragYdata[i] = ydata[i];
                }
            } else {
                int ii = insertIndex(xdata, thisX) - 1;
                for (int i = 0; i < dragIndex; i++) {
                    dragXdata[i] = xdata[i];
                    dragYdata[i] = ydata[i];
                }
                for (int i = dragIndex; i < ii; i++) {
                    dragXdata[i] = xdata[i + 1];
                    dragYdata[i] = ydata[i + 1];
                }
                dragXdata[ii] = thisX;
                dragYdata[ii] = thisY;
                for (int i = ii + 1; i < nPoints; i++) {
                    dragXdata[i] = xdata[i];
                    dragYdata[i] = ydata[i];
                }
            }
            mathCoords.setPoints(dragXdata, dragYdata);
            graphicsPanel.clear();
            mathPainter.setPaint(dataColorBtn.getColor());
            pointPlotter.plot();
            drawInterpolants(dragXdata, dragYdata);
        }
        graphicsPanel.update();
    }

    public void mouseReleasedAction(MathPainterPanelEvent mppe) {
        if (mouseDragged) {
            if (nPoints == 0) {
            } else {
                xdata = dragXdata;
                ydata = dragYdata;
            }
            mathCoords.setPoints(xdata, ydata);
            graphicsPanel.clear();
            mathPainter.setPaint(dataColorBtn.getColor());
            pointPlotter.plot();
            if (nPoints > 1) {
                drawInterpolants(xdata, ydata);
            }
            graphicsPanel.update();
            mouseDragged = false;
        }
    }
}
