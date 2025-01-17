package testes.jfreechart;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.NumberFormat;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.PieDataset;
import org.jfree.data.xml.DatasetReader;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

/**
 * A simple demonstration application showing how to create a pie chart from data in an
 * XML file.
 *
 */
public class XMLPieChartDemo extends ApplicationFrame {

    /**
     * Default constructor.
     *
     * @param title  the frame title.
     */
    public XMLPieChartDemo(final String title) {
        super(title);
        PieDataset dataset = null;
        final URL url = getClass().getResource("/org/jfree/chart/demo/piedata.xml");
        try {
            final InputStream in = url.openStream();
            dataset = DatasetReader.readPieDatasetFromXML(in);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
        final JFreeChart chart = ChartFactory.createPieChart("Pie Chart Demo 1", dataset, true, true, false);
        chart.setBackgroundPaint(Color.yellow);
        final PiePlot plot = (PiePlot) chart.getPlot();
        plot.setNoDataMessage("No data available");
        final ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
        setContentPane(chartPanel);
    }

    /**
     * Starting point for the demonstration application.
     *
     * @param args  ignored.
     */
    public static void main(final String[] args) {
        final XMLPieChartDemo demo = new XMLPieChartDemo("XML Pie Chart Demo");
        demo.pack();
        RefineryUtilities.centerFrameOnScreen(demo);
        demo.setVisible(true);
    }
}
