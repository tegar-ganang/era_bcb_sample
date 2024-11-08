package vademecum.visualizer.densityscatter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import javax.swing.JLabel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import vademecum.core.experiment.ExperimentNode;
import vademecum.data.GridUtils;
import vademecum.data.IDataGrid;
import vademecum.data.Retina;
import vademecum.extensionPoint.IDataNode;
import vademecum.math.density.pareto.ParetoDensity;
import vademecum.math.statistics.Univariate;
import vademecum.ui.visualizer.VisualizerFrame;
import vademecum.ui.visualizer.panel.FigurePanel;
import vademecum.ui.visualizer.vgraphics.IPlotable;
import vademecum.ui.visualizer.vgraphics.VGraphics;
import vademecum.ui.visualizer.vgraphics.legend.LegendPanel;
import vademecum.visualizer.densityscatter.dialogs.ContourDialog;
import vademecum.visualizer.densityscatter.dialogs.ContourVariableSelector;
import vademecum.visualizer.densityscatter.dialogs.SphereRadiusDialog;
import vademecum.visualizer.heightMatrix.colors.ColorMapChooser;
import vademecum.visualizer.heightMatrix.contour.CalcContour;
import vademecum.visualizer.heightMatrix.contour.ContourPointListMatrix;
import vademecum.visualizer.heightMatrix.contour.Line3DList;
import vademecum.visualizer.heightMatrix.widgets.ColorLegend;

/**
 * PDEScatter
 */
public class VPDEScatter extends VGraphics implements IPlotable, Runnable {

    IDataGrid sourceGrid;

    BufferedImage image;

    /** the info/debug logger */
    private static Log log = LogFactory.getLog(VPDEScatter.class);

    private ColorMapChooser colorMapChooser = new ColorMapChooser();

    private ContourPointListMatrix cplm;

    private Line3DList lines;

    private boolean drawContour = false;

    private int contourSteps = 25;

    private Vector<Vector<Vector<Double>>> lineVec = new Vector<Vector<Vector<Double>>>();

    double xmin, xmax, ymin, ymax, deltax, deltay;

    double[][] cmap;

    double[] densities;

    ParetoDensity pdens;

    /** List which holds the used variable of the data-source */
    ArrayList<Integer> usedGridVariables;

    /** Holds 2 columns of the grid */
    Double[][] sub;

    int numTicks = 5;

    double[] xaxisticks;

    double[] yaxisticks;

    DecimalFormat format = new DecimalFormat("#.###");

    int var1, var2;

    ColorLegend legend;

    JLabel label = new JLabel("");

    public VPDEScatter() {
        super();
        this.colorMapChooser.setColorMap("pcolor");
        usedGridVariables = new ArrayList<Integer>();
    }

    private void initLegend() {
        if (legend == null) {
            legend = new ColorLegend();
        }
        legend.setZWeight(75);
        legend.setPluginID(this.getPluginUID());
        legend.setType(VGraphics.TYPE_WIDGET);
        legend.setOrientation(FigurePanel.NORTHEAST);
        legend.setMin(0);
        log.debug("legend max : " + getMaxHeight());
        legend.setMax(getMaxHeight());
        legend.initInteractions();
        legend.setBounds(591, 15, 89, 396);
        legend.setPreferredSize(new Dimension(89, 396));
    }

    public ColorLegend getLegend() {
        if (legend == null) {
            initLegend();
        }
        return this.legend;
    }

    public JLabel getLabel() {
        return this.label;
    }

    /**
	 * Calculates the Contour Lines
	 * @param value
	 */
    public void setContourSteps(int value) {
        this.contourSteps = value;
        cplm = CalcContour.calcMatrix(cmap, contourSteps);
        lines = CalcContour.matrix2lines(cplm, cmap);
        lineVec.clear();
        for (int i = 0; i < lines.size(); i++) {
            this.lineVec.add(new Vector<Vector<Double>>());
            for (int j = 0; j < lines.getLine(i).getLine().size(); j++) {
                this.lineVec.get(i).add(new Vector<Double>());
                for (int k = 0; k < lines.getLine(i).getLine().get(j).get().length; k++) {
                    this.lineVec.get(i).get(j).add(lines.getLine(i).getLine().get(j).get()[k]);
                }
            }
        }
    }

    /**
	 * Gets the current Steps for the Contour Line
	 * @return
	 */
    public int getContourSteps() {
        return this.contourSteps;
    }

    /**
	 * Calculates the map size from dataset Size
	 * Ratio : 3/4 (height/width)
	 * @param datasetSize
	 * @return height and width of the map, where p.x is the height and p.y the width 
	 * 
	 */
    public Point calcMapSize(int datasetSize) {
        Point hw = new Point();
        float x = (float) Math.sqrt(datasetSize / 12);
        int height = Math.round(3 * x);
        int width = Math.round(4 * x);
        hw.x = height;
        hw.y = width;
        return hw;
    }

    public Point getMapIndex(double x, double y, int rows, int cols) {
        double maxx = xmax + deltax;
        double maxy = ymax + deltay;
        x = x + deltax;
        y = y + deltay;
        int indexx = (int) ((x * rows - 1) / maxx);
        int indexy = (int) ((y * cols - 1) / maxy);
        Point p = new Point(indexx, indexy);
        if (x == maxx) {
            log.info("x max found : " + x);
            log.info("index for x will be : " + indexx);
        }
        if (y == maxy) {
            log.info("y max found : " + y);
            log.info("index for y will be : " + indexy);
        }
        return p;
    }

    public void setDataSource(IDataGrid grid, Vector<Integer> vars) {
        int[] var = new int[2];
        for (int i = 0; i < vars.size(); i++) {
            var[i] = vars.get(i);
        }
        this.setDataSource(grid, var[0], var[1]);
    }

    public void setDataSource(IDataGrid grid, int var1, int var2) {
        sourceGrid = grid;
        usedGridVariables.add(var1);
        usedGridVariables.add(var2);
        IDataGrid subgrid = sourceGrid.getSubGrid(new int[] { var1, var2 }, sourceGrid.getNumRows());
        log.debug(subgrid.toString());
        sub = subgrid.doubleColsToDoubleArray();
        double[] colx = new double[sub.length];
        double[] coly = new double[sub.length];
        for (int i = 0; i < sub.length; i++) {
            colx[i] = sub[i][0];
            coly[i] = sub[i][1];
        }
        xmin = Univariate.getMin(colx);
        xmax = Univariate.getMax(colx);
        ymin = Univariate.getMin(coly);
        ymax = Univariate.getMax(coly);
        log.debug("xmin " + xmin);
        log.debug("xmax " + xmax);
        log.debug("ymin " + ymin);
        log.debug("ymax " + ymax);
        deltax = 0;
        deltay = 0;
        if (xmin < 0) {
            deltax = Math.abs(xmin);
        }
        if (ymin <= 0) {
            deltay = Math.abs(ymin);
        }
        xaxisticks = new double[numTicks];
        yaxisticks = new double[numTicks];
        double h = (xmax - xmin) / (double) (numTicks - 1);
        for (int i = 0; i < numTicks; i++) {
            xaxisticks[i] = xmin + (double) i * h;
        }
        for (int i = 0; i < numTicks; i++) {
            log.debug("X Axis_ Tick No " + i + " : " + xaxisticks[i]);
        }
        h = (ymax - ymin) / (double) (numTicks - 1);
        for (int i = 0; i < numTicks; i++) {
            yaxisticks[i] = ymin + (double) i * h;
        }
        for (int i = 0; i < numTicks; i++) {
            log.debug("Y Axis_ Tick No " + i + " : " + yaxisticks[i]);
        }
        if (pdens == null) {
            pdens = new ParetoDensity();
            pdens.setDataGrid(subgrid);
            pdens.setParetoRadius();
        } else {
        }
        this.densities = pdens.getDensities();
        Point hw = calcMapSize(Math.min(sub.length, 1000));
        int mapwidth = hw.y;
        int mapheight = hw.x;
        log.debug("map width height =" + mapheight + " and width =" + mapwidth);
        double[][] map = new double[mapheight][mapwidth];
        for (int i = 0; i < Math.min(sub.length, 1000); i++) {
            Point p = getMapIndex(sub[i][0], sub[i][1], mapwidth, mapheight);
            if (i < 500) {
                log.debug(i + ": " + p);
            }
            int pym = Math.abs(mapheight - 1 - p.y);
            if (pym >= hw.x) {
                pym = hw.x - 1;
            }
            int pxm = Math.abs(p.x % mapwidth);
            map[pym][pxm] = densities[i];
        }
        cmap = map;
        this.setContourSteps(this.contourSteps);
        this.drawContour = true;
        buildImage(map);
        log.debug("densities _max : " + Univariate.getMax(this.densities));
        label.setText("min : " + Univariate.getMin(this.densities) + " max: " + Univariate.getMax(this.densities) + " pdens.r : " + pdens.getRadius());
        label.repaint();
    }

    public IDataGrid getDataSource() {
        return this.sourceGrid;
    }

    public ArrayList<Integer> getPlotDataVariables() {
        return this.usedGridVariables;
    }

    public void clearPlot() {
        this.usedGridVariables.clear();
    }

    public void refreshPlot() {
        this.densities = pdens.getDensities();
        Point hw = calcMapSize(Math.min(sub.length, 1000));
        int mapwidth = hw.y;
        int mapheight = hw.x;
        double[][] map = new double[mapheight][mapwidth];
        for (int i = 0; i < Math.min(sub.length, 1000); i++) {
            Point p = getMapIndex(sub[i][0], sub[i][1], mapwidth, mapheight);
            map[Math.abs(mapheight - 1 - p.y)][p.x % mapwidth] = densities[i];
        }
        cmap = map;
        this.setContourSteps(this.contourSteps);
        this.drawContour = true;
        buildImage(map);
        repaint();
        if (legend != null) {
            legend.repaint();
        }
        label.setText("min : " + Univariate.getMin(this.densities) + " max: " + Univariate.getMax(this.densities) + " pdens.r : " + pdens.getRadius());
        label.repaint();
    }

    public ParetoDensity getParetoDensity() {
        return this.pdens;
    }

    public int coordinates2index(int row, int col, int columns) {
        return (row * columns) + col;
    }

    public void buildImage(double[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        image = new BufferedImage(matrix[0].length, matrix.length, BufferedImage.TYPE_INT_RGB);
        double matrixMax = Univariate.getMax(pdens.getDensities());
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double colorDVal = matrix[i][j] / matrixMax;
                float colorVal = (float) colorDVal;
                if (colorVal > 1) {
                    colorVal = 1;
                }
                int colIndex = Math.round((float) (this.colorMapChooser.numOfValues() - 1) * colorVal);
                Vector<Float> rgbColor = this.colorMapChooser.getFloatRGBValues(colIndex);
                float[] col = new float[3];
                for (int index = 0; index < col.length; index++) {
                    col[index] = rgbColor.get(index);
                }
                image.setRGB(j, i, new Color(col[0], col[1], col[2]).getRGB());
            }
        }
    }

    public double getMaxHeight() {
        return Univariate.getMax(this.pdens.getDensities());
    }

    public double getMinHeight() {
        return Univariate.getMin(this.pdens.getDensities());
    }

    @Override
    protected void drawCustomPaintings(Graphics2D g2) {
        int belowsp = 30;
        int leftsp = 70;
        g2.drawImage(image, 0 + leftsp, 0, this.getWidth() - leftsp, this.getHeight() - belowsp, this);
        if (this.drawContour) {
            double sizeFacX = (double) (this.getWidth() - leftsp) / (double) this.cmap[0].length;
            double sizeFacY = (double) (this.getHeight() - belowsp) / (double) this.cmap.length;
            g2.setColor(Color.BLACK);
            for (int i = 0; i < this.lineVec.size(); i++) {
                g2.drawLine(Math.round(Math.round(this.lineVec.get(i).get(0).get(1) * sizeFacX + (sizeFacX / 2))) + leftsp, Math.round(Math.round(this.lineVec.get(i).get(0).get(0) * sizeFacY + (sizeFacY / 2))), Math.round(Math.round(this.lineVec.get(i).get(1).get(1) * sizeFacX + (sizeFacX / 2)) + leftsp), Math.round(Math.round(this.lineVec.get(i).get(1).get(0) * sizeFacY + (sizeFacY / 2))));
            }
        }
        if (xaxisticks != null) {
            drawAxes(g2, leftsp, belowsp);
        }
    }

    public void drawAxes(Graphics2D g2, int leftsp, int belowsp) {
        FontMetrics fm = g2.getFontMetrics();
        Rectangle2D frect;
        g2.setColor(Color.black);
        int linexsp = 4;
        int he = this.getHeight() - belowsp + linexsp;
        g2.drawLine(leftsp, he, this.getWidth() - 4, he);
        int range = this.getWidth() - leftsp;
        double h = (double) range / (double) (numTicks - 1);
        for (int i = 1; i < numTicks - 1; i++) {
            int nextX = leftsp + Math.round((float) i * (float) h);
            g2.drawLine(nextX, he - 3, nextX, he + 3);
            String number = format.format(xaxisticks[i]);
            frect = fm.getStringBounds(number, g2);
            g2.drawString(number, nextX - (int) frect.getWidth() / 2, he + 13);
        }
        g2.drawLine(this.getWidth() - 4, he, this.getWidth() - 4, he - 3);
        String number = format.format(xaxisticks[numTicks - 1]);
        frect = fm.getStringBounds(number, g2);
        g2.drawString(number, this.getWidth() - (int) frect.getWidth(), he + 13);
        number = format.format(xaxisticks[0]);
        frect = fm.getStringBounds(number, g2);
        g2.drawString(number, leftsp - (int) frect.getWidth() / 2, he + 13);
        int axs = 15;
        g2.setColor(Color.black);
        g2.drawLine(leftsp, 4, leftsp, he);
        int tw = 1;
        int yRange = this.getHeight() - axs;
        h = (double) yRange / (double) (numTicks - 1);
        for (int i = 1; i < numTicks - 1; i++) {
            int nextY = Math.round((float) i * (float) h);
            g2.drawLine(leftsp - tw, nextY, leftsp + tw, nextY);
            number = format.format(yaxisticks[numTicks - 1 - i]);
            frect = fm.getStringBounds(number, g2);
            g2.drawString(number, leftsp - tw - (int) frect.getWidth(), nextY + 5);
        }
        g2.drawLine(leftsp, 4, leftsp + 3, 4);
        number = format.format(yaxisticks[numTicks - 1]);
        frect = fm.getStringBounds(number, g2);
        g2.drawString(number, leftsp - tw - (int) frect.getWidth(), 4 + 9);
        number = format.format(yaxisticks[0]);
        frect = fm.getStringBounds(number, g2);
        g2.drawString(number, leftsp - tw - (int) frect.getWidth(), he);
    }

    public Object getDialog(int i) {
        if (i == 0) return new ContourVariableSelector(this); else return new ContourDialog(this);
    }

    public String getDialogLabel(int i) {
        if (i == 0) return "Variable Selector"; else return "Contour Settings";
    }

    public int getNumberOfDialogs() {
        return 2;
    }

    public String getPlotMenuLabel() {
        return "Density 2D";
    }

    @Override
    public Properties getProperties() {
        Properties sp = super.getProperties();
        Properties p = new Properties();
        String prefix = "VGraphics" + this.getID() + "_";
        if (usedGridVariables != null) {
            p.setProperty(prefix + "numDim", Integer.toString(this.usedGridVariables.size()));
            for (int i = 0; i < usedGridVariables.size(); i++) {
                p.setProperty(prefix + "gridDim_" + i, Integer.toString(usedGridVariables.get(i)));
            }
        }
        double radius = pdens.getRadius();
        p.setProperty(prefix + "radius", Double.toString(radius));
        int numclusters = pdens.getClusters();
        p.setProperty(prefix + "expclusters", Integer.toString(numclusters));
        Enumeration keyEn = sp.keys();
        while (keyEn.hasMoreElements()) {
            String key = (String) keyEn.nextElement();
            p.setProperty(key, sp.getProperty(key));
        }
        return p;
    }

    @Override
    public void setProperties(Properties p) {
        super.setProperties(p);
        String prefix = "VGraphics" + this.getID() + "_";
        if (pdens == null) {
            pdens = new ParetoDensity();
        }
        int numDim = Integer.parseInt(p.getProperty(prefix + "numDim"));
        if (numDim == 2) {
            var1 = Integer.parseInt(p.getProperty(prefix + "gridDim_0"));
            var2 = Integer.parseInt(p.getProperty(prefix + "gridDim_1"));
            new Thread(this).start();
        }
    }

    public void run() {
        try {
            ((VisualizerFrame) this.getFigurePanel().getGraphicalViewer()).showWaitPanel("Preparing PDEScatter. Please Wait ...");
            ExperimentNode eNode = this.getFigurePanel().getSourceNode();
            IDataNode dn = eNode.getMethod();
            IDataGrid dataGrid = (IDataGrid) dn.getOutput(IDataGrid.class);
            ((VisualizerFrame) this.getFigurePanel().getGraphicalViewer()).showWaitPanel("Preparing PDEScatter. Please Wait ...");
            IDataGrid subgrid = dataGrid.getSubGrid(new int[] { var1, var2 }, dataGrid.getNumRows());
            this.pdens.setDataGrid(subgrid);
            pdens.setParetoRadius();
            log.debug("build up dens _ max: " + Univariate.getMax(pdens.getDensities()));
            this.setDataSource(dataGrid, var1, var2);
            ((VisualizerFrame) this.getFigurePanel().getGraphicalViewer()).hideWaitPanel();
            ((VisualizerFrame) this.getFigurePanel().getGraphicalViewer()).initToolbox();
        } catch (Exception e) {
            e.printStackTrace();
            ((VisualizerFrame) this.getFigurePanel().getGraphicalViewer()).hideWaitPanel();
            ((VisualizerFrame) this.getFigurePanel().getGraphicalViewer()).dispose();
            System.err.println("PDEScatter : Something went wrong. Please note the following error messages :");
            System.err.println(e);
            e.printStackTrace();
        }
    }

    public Object getHelp() {
        return null;
    }
}
