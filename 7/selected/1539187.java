package gov.lanl.Web;

import java.util.*;
import com.klg.jclass.chart.ChartDataModel;
import com.klg.jclass.chart.*;
import com.klg.jclass.chart.data.JCDefaultDataSource;
import com.klg.jclass.schart.beans.ServerChart;
import com.klg.jclass.util.legend.JCLegend;

/**
 * configure JClassServerChart to plot CSV table where independent variable is date
 * takes as input the JavaChart, the file to be read, and the properties file
 * It can handle multiple column charts with a single independent variable
 */
public class JClassHistoryChart {

    /**
     * data file to be plotted
     */
    private String filename;

    /**
     * internal table generated from csv file above
     */
    private CSVTable csvTable;

    private String writeDirectory = "";

    private ServerChart chart;

    private String plot;

    private String plotType = "area";

    private int first = 1;

    private int barWidth = 30;

    /**
     *  property to be read in.
     */
    private String props;

    /**
     *  Log4j Category object
     */
    private static org.apache.log4j.Logger cat = org.apache.log4j.Logger.getLogger(JClassHistoryChart.class.getName());

    public JClassHistoryChart() {
    }

    /**
     * Constructor
     */
    public JClassHistoryChart(ServerChart chart, String file, String props) {
        this.chart = chart;
        setFileName(file);
        this.props = props;
    }

    /**
     * Constructor without filename
     */
    public JClassHistoryChart(ServerChart chart, String props) {
        this.chart = chart;
        this.props = props;
        csvTable = null;
    }

    /**
     *  Constructor with only Properties.
     */
    public JClassHistoryChart(String props) {
        chart = new ServerChart();
        this.props = props;
    }

    /**
     * set Properties file to be read
     */
    public void setProps(String props) {
        this.props = props;
    }

    /**
     * set the CSV filename to be read
     */
    public void setFileName(String file) {
        filename = file;
        csvTable = new CSVTable(filename, "/csv");
    }

    /**
     *  set the directory for writing the plot image
     */
    public void setDirectory(String directory) {
        writeDirectory = directory;
    }

    /**
     *  replace Chart
     */
    public void setChart(ServerChart chart) {
        this.chart = chart;
    }

    /**
     *  set the plot file name (without png suffix or directory)
     */
    public void setPlot(String plotName) {
        plot = plotName;
    }

    /**
     * get the plot file name without the suffix "Plot.png"
     */
    public String getPlot() {
        return plot;
    }

    /**
     *  set the Plot type   (should be "line", "area")
     */
    public void setPlotType(String type) {
        plotType = type;
    }

    /**
     * Set the index of the last point in the first series.
     */
    public void setIndex(int first) {
        this.first = first;
    }

    /**
     *  set the width of the bar cluster (0-100)
     */
    public void setBarWidth(int width) {
        barWidth = width;
    }

    /**
     * plot with fileName;
     */
    public String plot(String filename) {
        setFileName(filename);
        plot();
        String directory = writeDirectory + "/" + plot + ".png";
        draw(directory);
        return plot + ".png";
    }

    /**
     * plot
     */
    public void plot() {
        cat.debug("filename to read: " + filename);
        if (csvTable == null) csvTable = new CSVTable(filename, "/csv");
        if (chart == null) chart = new ServerChart();
        PropertyResourceBundle rb = (PropertyResourceBundle) PropertyResourceBundle.getBundle(props);
        Enumeration keys = rb.getKeys();
        while (keys.hasMoreElements()) {
            String prop = (String) keys.nextElement();
            String value = rb.getString(prop);
            gov.lanl.Utility.BeanProperties.setProperty(chart, prop, value);
        }
        String[] labels = csvTable.getLabels();
        if (labels.length == 0) {
            cat.warn("NO Data read from " + filename + "!");
            return;
        }
        String[] x = csvTable.getValues(labels[0]);
        double[] dat = new double[x.length];
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("MM/dd/yyyy");
        java.text.SimpleDateFormat dform = new java.text.SimpleDateFormat("MM/dd");
        JCAxis xAxis = chart.getChartArea().getXAxis(0);
        xAxis.setTimeUnit(JCAxis.MONTHS);
        xAxis.setAnnotationMethod(JCAxis.TIME_LABELS);
        chart.getLegend().setVisible(true);
        JCAxis yAxis = chart.getChartArea().getYAxis(0);
        yAxis.getTitle().setPlacement(JCLegend.WEST);
        yAxis.getTitle().setRotation(ChartText.DEG_270);
        try {
            for (int k = 0; k < x.length; k++) {
                Date d = df.parse(x[k]);
                if (k == 0) xAxis.setTimeBase(d);
                dat[k] = xAxis.dateToValue(d);
            }
            double[][] xx = new double[1][dat.length];
            xx[0] = dat;
            String[] labs = new String[first];
            double[][] zz = new double[first][dat.length];
            for (int i = 0; i < first; i++) {
                String[] v = csvTable.getValues(labels[i + 1]);
                zz[i] = new double[v.length];
                labs[i] = labels[i + 1];
                for (int k = 0; k < v.length; k++) {
                    zz[i][k] = new Double(v[k]).doubleValue();
                }
            }
            ChartDataView dataView = chart.getDataView(0);
            dataView.setDataSource(new JCDefaultDataSource(xx, zz, null, labs, ""));
            dataView.setChartType(JCChart.BAR);
            ((JCBarChartFormat) dataView.getChartFormat()).setClusterWidth(barWidth);
            double[][] yy = new double[labels.length - first - 1][];
            labs = new String[labels.length - first - 1];
            for (int i = first + 1; i < labels.length; i++) {
                String[] v = csvTable.getValues(labels[i]);
                labs[i - first - 1] = labels[i];
                yy[i - first - 1] = new double[v.length];
                for (int k = 0; k < v.length; k++) {
                    double f = new Double(v[k]).doubleValue();
                    yy[i - first - 1][k] = f;
                }
            }
            chart.addDataView(1);
            dataView = chart.getDataView(1);
            dataView.setDataSource(new JCDefaultDataSource(xx, yy, null, labs, ""));
            if (plotType.equals("line")) dataView.setChartType(JCChart.PLOT); else dataView.setChartType(JCChart.STACKING_AREA);
        } catch (Exception e) {
            cat.error("Plot failed due to :" + e, e);
        }
    }

    /**
     * return graph name and trigger drawing
     */
    public void draw(String file) {
        try {
            chart.encodeToPNGFile(file);
        } catch (Exception e) {
            cat.error("encoding to PNG failed: " + e);
        }
    }

    /**
     * Main test program
     */
    public static void main(String[] argv) {
        JClassHistoryChart history = new JClassHistoryChart("viralchart");
        history.setDirectory(".");
        history.plot(argv[0]);
        try {
        } catch (Exception e) {
            cat.error(e);
        }
        System.exit(0);
    }
}
