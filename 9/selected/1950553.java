package vademecum.visualizer.pdeplot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import javax.swing.JDialog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.math.plot.canvas.Plot2DCanvas;
import vademecum.core.experiment.ExperimentNode;
import vademecum.data.ArrayUtils;
import vademecum.data.GridUtils;
import vademecum.data.IDataGrid;
import vademecum.extensionPoint.IDataNode;
import vademecum.math.NumericalUtils;
import vademecum.math.density.pareto.ParetoDensity;
import vademecum.math.statistics.BinWidthStrategies;
import vademecum.ui.visualizer.VisualizerFrame;
import vademecum.ui.visualizer.panel.FigurePanel;
import vademecum.ui.visualizer.utils.ColorPool;
import vademecum.ui.visualizer.vgraphics.IPlotable;
import vademecum.ui.visualizer.vgraphics.VGraphics;
import vademecum.visualizer.pdeplot.dialogs.FineTuning;
import vademecum.visualizer.pdeplot.dialogs.PPDEPlotVariableSelector;
import vademecum.visualizer.pdeplot.dialogs.SphereRadiusDialog;

/**
 * Probability - Pareto Density Estimation Plot
 * PPDE
 * see 
 */
public class PDEPlot extends VGraphics implements IPlotable {

    /** Logger */
    private static Log log = LogFactory.getLog(PDEPlot.class);

    /** the 2D Canvas (JMathTools) */
    Plot2DCanvas plot2DCanvas;

    /** Canvas Layout Parameter */
    int deltaX = 0;

    /** Canvas Layout Parameter */
    int deltaY = 0;

    /** Reference to the data-source */
    IDataGrid sourceGrid;

    /** List which holds the used variable of the data-source */
    ArrayList<Integer> usedGridVariables;

    /** Colors for multiple plots */
    ColorPool colorPool = new ColorPool();

    ParetoDensity pd;

    boolean changed = false;

    public boolean defaultHeuristics = true;

    public int customBins = 0;

    public double customRadius = 2d;

    public double paretoRadius = 1d;

    public PDEPlot() {
        super();
        this.plot2DCanvas = new Plot2DCanvas();
        this.setVisible(true);
        usedGridVariables = new ArrayList<Integer>();
        this.plot2DCanvas.setVisible(true);
        this.plot2DCanvas.removeComponentListener(this.plot2DCanvas);
        plot2DCanvas.setBackground(this.getBackgroundColor());
    }

    /** 
	 * Setting up a 1-dimensional DataSource
	 * @param grid
	 * @param var
	 * @param c
	 */
    public void setDataGrid(IDataGrid grid, int var, Color c) {
        this.sourceGrid = grid;
        usedGridVariables.add(var);
        if (this.changed == false) {
            pd = new ParetoDensity();
        }
        double[] colData = GridUtils.get1DColumnArray(grid, var);
        pd.setDataGrid(GridUtils.singleArrayToGrid(colData));
        double[] kernels;
        if (defaultHeuristics == true) {
            BinWidthStrategies bws = new BinWidthStrategies(grid, var);
            double optimalBinWidth = bws.getKeatingScott();
            int optimalBinNumber = bws.pde1DOptimalNumberOfBins();
            log.debug("Optimal Bin Width : " + optimalBinWidth);
            log.debug("Optimal Bin Number : " + optimalBinNumber);
            customBins = optimalBinNumber - 1;
            kernels = bws.calculatePDEKernels(optimalBinNumber - 1);
            IDataGrid kernelGrid = GridUtils.arrayToGrid(kernels);
            pd.setCenters(kernelGrid);
            pd.calculateDensities();
            log.debug("Pareto Radius : " + pd.getRadius());
            customRadius = pd.getRadius();
            paretoRadius = pd.getRadius();
        } else {
            BinWidthStrategies bws = new BinWidthStrategies(grid, var);
            kernels = bws.calculatePDEKernels(customBins);
            IDataGrid kernelGrid = GridUtils.arrayToGrid(kernels);
            pd.setCenters(kernelGrid);
            pd.calculateDensities(customRadius);
        }
        double[] densities = pd.getDensities();
        log.debug("#Densities : " + densities.length);
        double area = NumericalUtils.trapezoidRule(kernels, densities);
        log.debug("Area : " + area);
        for (int i = 0; i < densities.length; i++) {
            if (area > 0d) {
                densities[i] = densities[i] / (area);
            }
        }
        double[][] XY = ArrayUtils.quickSortAndPackReferencedArrays(kernels, densities);
        plot2DCanvas.addLinePlot("Pareto Density Estimation", c, XY);
    }

    /**
	 * Setting up a 1-dimensional DataSource with DefaultColor:Black
	 * @param grid
	 * @param var
	 */
    public void setDataGrid(IDataGrid grid, int var) {
        this.setDataGrid(grid, var, colorPool.getColor(var));
    }

    /**
	 * Returns the data-source
	 * @return a IDataGrid
	 */
    public IDataGrid getDataGrid() {
        return this.sourceGrid;
    }

    public ParetoDensity getParetoDensity() {
        return this.pd;
    }

    /**
	 * Returning an array of Variable Numbers of the datagrid
	 * e.g. Variables 2 and 3
	 * @return
	 */
    public ArrayList<Integer> getPlotDataVariables() {
        return usedGridVariables;
    }

    /**
	 * Returning the number of used Variables of the grid;
	 * also the number of plots 
	 * @return
	 */
    public int getPlotDataDimensions() {
        return usedGridVariables.size();
    }

    public void refreshPlot() {
        this.plot2DCanvas.removeAllPlots();
        ArrayList<Integer> vars = (ArrayList<Integer>) this.usedGridVariables.clone();
        this.usedGridVariables.clear();
        this.changed = true;
        for (int var : vars) {
            setDataGrid(this.sourceGrid, var);
        }
        this.repaint();
        this.changed = false;
    }

    @Override
    protected void drawCustomPaintings(Graphics2D g2) {
        if (this.graphicsChanged == true) {
            updateCanvasSize();
            plot2DCanvas.paint(g2);
        }
    }

    /**
	 * Update the JMathTool's Canvas Size 
	 */
    public void updateCanvasSize() {
        plot2DCanvas.panelSize = new int[] { (int) (getSize().getWidth() + deltaX), (int) (getSize().getHeight() + deltaY) };
        plot2DCanvas.setSize(plot2DCanvas.panelSize[0], plot2DCanvas.panelSize[1]);
        plot2DCanvas.setPreferredSize(new Dimension(plot2DCanvas.panelSize[0], plot2DCanvas.panelSize[1]));
        plot2DCanvas.resetBase();
    }

    @Override
    public void setBackgroundColor(Color bg) {
        super.setBackgroundColor(bg);
        this.plot2DCanvas.setBackground(bg);
    }

    @Override
    public void setBackgroundVisible(boolean b) {
        super.setBackgroundVisible(b);
        if (b == false) {
            FigurePanel xp = this.getFigurePanel();
            if (xp != null) {
                setBackground(xp.getBackground());
            }
        }
    }

    /**
	 * Getting the Marker Color of the first Plot
	 * @return
	 */
    public Color getMarkerColor() {
        return plot2DCanvas.getPlot(0).getColor();
    }

    /**
	 * Getting the Marker Color of Plot i 
	 * @param i
	 * @return
	 */
    public Color getMarkerColor(int i) {
        return plot2DCanvas.getPlot(i).getColor();
    }

    /**
	 * Setting the MarkerColor of the first Plot
	 * @param c a Color
	 */
    public void setMarkerColor(Color c) {
        plot2DCanvas.getPlot(0).setColor(c);
    }

    /**
	 * Setting the MarkerColor of the i-th plot
	 * @param c a Color
	 * @param i the plotnumber
	 */
    public void setMarkerColor(Color c, int i) {
        plot2DCanvas.getPlot(i).setColor(c);
    }

    /**
	 * Getting the XAxis Label
	 * @return a String
	 */
    public String getXAxisLabel() {
        return plot2DCanvas.getGrid().getLegend(0);
    }

    /**
	 * Setting the XAxis Label
	 * @param s a String
	 */
    public void setXAxisLabel(String s) {
        plot2DCanvas.getGrid().setLegend(0, s);
    }

    /**
	 * Getting the YAxis Label
	 * @return a String
	 */
    public String getYAxisLabel() {
        return plot2DCanvas.getGrid().getLegend(1);
    }

    /**
	 * Setting the YAxis Label
	 * @param s a String
	 */
    public void setYAxisLabel(String s) {
        plot2DCanvas.getGrid().setLegend(1, s);
    }

    /**
	 * Removing all Plots from the 2D Canvas
	 *
	 */
    public void clearPlot() {
        this.plot2DCanvas.removeAllPlots();
        this.usedGridVariables.clear();
        colorPool.reset();
    }

    @Override
    public void initInteractions() {
        super.initInteractions();
    }

    public void componentResized(ComponentEvent e) {
        if (e.getComponent() == this) {
            this.lastLocation = this.getLocation();
        }
    }

    public String getPlotMenuLabel() {
        return "PDE";
    }

    public JDialog getDialog(int i) {
        if (i == 0) return new PPDEPlotVariableSelector(this); else if (i == 1) {
            return new SphereRadiusDialog(this);
        } else {
            return new FineTuning(this);
        }
    }

    public String getDialogLabel(int i) {
        if (i == 0) return "Variable Selector"; else if (i == 1) return "Sphere Radius"; else {
            return "Fine Tuning";
        }
    }

    public int getNumberOfDialogs() {
        return 2;
    }

    /**
	  * Getting Properties from this VGraphics
	  * and its Features
	  */
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
        } else p.setProperty(prefix + "numDim", "0");
        Enumeration keyEn = sp.keys();
        while (keyEn.hasMoreElements()) {
            String key = (String) keyEn.nextElement();
            p.setProperty(key, sp.getProperty(key));
        }
        return p;
    }

    /**
	  * Setting the Properties of this VGraphics and
	  * its Features 
	  * @param cp
	  */
    @Override
    public void setProperties(Properties p) {
        super.setProperties(p);
        String prefix = "VGraphics" + this.getID() + "_";
        int numDim = Integer.parseInt(p.getProperty(prefix + "numDim"));
        for (int i = 0; i < numDim; i++) {
            int var = Integer.parseInt(p.getProperty(prefix + "gridDim_" + Integer.toString(i)));
            try {
                ExperimentNode eNode = this.getFigurePanel().getSourceNode();
                IDataNode dn = eNode.getMethod();
                IDataGrid dataGrid = (IDataGrid) dn.getOutput(IDataGrid.class);
                this.setDataGrid(dataGrid, var);
            } catch (Exception e) {
                ((VisualizerFrame) this.getFigurePanel().getGraphicalViewer()).hideWaitPanel();
                ((VisualizerFrame) this.getFigurePanel().getGraphicalViewer()).dispose();
                System.err.println("PDEplot : Something went wrong. Please note the following error messages :");
                System.err.println(e);
                e.printStackTrace();
            }
        }
    }

    public Object getHelp() {
        return null;
    }
}
