package vademecum.visualizer.kdeplot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;
import javax.swing.JDialog;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.math.plot.canvas.Plot2DCanvas;
import vademecum.core.experiment.ExperimentNode;
import vademecum.data.GridUtils;
import vademecum.data.IDataGrid;
import vademecum.extensionPoint.IDataNode;
import vademecum.math.NumericalUtils;
import vademecum.math.density.kernel.KernelDensity1D;
import vademecum.ui.visualizer.VisualizerFrame;
import vademecum.ui.visualizer.panel.FigurePanel;
import vademecum.ui.visualizer.utils.ColorPool;
import vademecum.ui.visualizer.vgraphics.IPlotable;
import vademecum.ui.visualizer.vgraphics.VGraphics;
import vademecum.visualizer.kdeplot.dialogs.BandWidthDialog;
import vademecum.visualizer.kdeplot.dialogs.VariableSelector;

/**
 * Pareto Density Estimation Plot
 *
 */
public class KDEPlot extends VGraphics implements IPlotable {

    /** Logger */
    private static Log log = LogFactory.getLog(KDEPlot.class);

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

    /** The Bandwidth */
    double bandWidth = 0.5;

    FigurePanel xp;

    VisualizerFrame vf;

    public KDEPlot() {
        super();
        this.plot2DCanvas = new Plot2DCanvas();
        this.setVisible(true);
        usedGridVariables = new ArrayList<Integer>();
        this.plot2DCanvas.setVisible(true);
        this.plot2DCanvas.removeComponentListener(this.plot2DCanvas);
        plot2DCanvas.setBackground(this.getBackgroundColor());
    }

    public void setBandWidth(double bw) {
        this.bandWidth = bw;
    }

    public double getBandWidth() {
        return this.bandWidth;
    }

    /** 
	 * Setting up a 1-dimensional DataSource
	 * @param grid
	 * @param var
	 * @param c
	 */
    public void setDataGrid(IDataGrid grid, int var, Color c) {
        FigurePanel xp = this.getFigurePanel();
        if (xp != null) {
            vf = (VisualizerFrame) xp.getGraphicalViewer();
            if (vf != null) {
                vf.showWaitPanel("Preparing Kernel Density. Please Wait ...");
            }
        }
        this.sourceGrid = grid;
        usedGridVariables.add(var);
        double[] data = GridUtils.get1DColumnArray(grid, var);
        KernelDensity1D kd = new KernelDensity1D(data, this.bandWidth);
        int n = 50;
        double[] wcopy = data.clone();
        Arrays.sort(wcopy);
        double xmin = wcopy[0];
        double xmax = wcopy[wcopy.length - 1];
        double[][] XY = new double[n][2];
        XY[0][0] = xmin;
        XY[0][1] = kd.f(xmin);
        double[] xa = new double[n];
        double[] ya = new double[n];
        for (int i = 1; i < n; i++) {
            double x = xmin + (double) i / (double) n * (xmax - xmin);
            double y = kd.f(x);
            XY[i][0] = x;
            XY[i][1] = y;
            xa[i] = x;
            ya[i] = y;
        }
        log.debug("area : " + NumericalUtils.trapezoidRule(xa, ya));
        plot2DCanvas.addScatterPlot("debug", Color.orange, XY);
        plot2DCanvas.addLinePlot("Kernel Density", Color.blue, XY);
        if (vf != null) {
            vf.hideWaitPanel();
        }
    }

    /**
	 * Setting up a 1-dimensional DataSource 
	 * @param grid
	 * @param var
	 */
    public void setDataGrid(IDataGrid grid, int var) {
        this.setDataGrid(grid, var, colorPool.getColor(var));
    }

    public IDataGrid getDataGrid() {
        return this.sourceGrid;
    }

    public void refreshPlot() {
        if (usedGridVariables.size() > 0) {
            int var = usedGridVariables.get(0);
            this.clearPlot();
            setDataGrid(this.sourceGrid, var);
        }
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
        return "KernelDensity";
    }

    public JDialog getDialog(int i) {
        if (i == 0) return new VariableSelector(this); else return new BandWidthDialog(this);
    }

    public String getDialogLabel(int i) {
        if (i == 0) return "Variable Selector"; else return "Bandwidth Selector";
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
        p.setProperty(prefix + "bandwidth", Double.toString(this.getBandWidth()));
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
        double bw = Double.parseDouble(p.getProperty(prefix + "bandwidth"));
        this.setBandWidth(bw);
        int numDim = Integer.parseInt(p.getProperty(prefix + "numDim"));
        if (numDim == 1) {
            int var1 = Integer.parseInt(p.getProperty(prefix + "gridDim_0"));
            try {
                ExperimentNode eNode = this.getFigurePanel().getSourceNode();
                IDataNode dn = eNode.getMethod();
                IDataGrid dataGrid = (IDataGrid) dn.getOutput(IDataGrid.class);
                this.setDataGrid(dataGrid, var1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Object getHelp() {
        return null;
    }
}
