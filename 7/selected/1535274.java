package fhi.bg.fachklassen;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.ui.RefineryUtilities;
import fhi.bg.fachklassen.Auswertung.Typ;

public class Diagramm {

    private String xLegend = null;

    private String yLegend = null;

    private String[] dataLegends = null;

    private Object[][] auswertung = null;

    private int n = 0;

    private int m = 0;

    public Diagramm(Object[][] auswertung, Typ cmd) {
        this.n = auswertung.length - 1;
        this.m = auswertung[1].length - 1;
        this.auswertung = new Object[this.n][this.m + 1];
        String[] tmpLegends = new String[this.n];
        for (int n = 0; n < this.n; n++) {
            int betrag = 0;
            for (int m = 0; m < this.m; m++) {
                this.auswertung[n][m] = auswertung[n + 1][m];
                betrag += (Integer) this.auswertung[n][m];
            }
            this.auswertung[n][m] = betrag;
            tmpLegends[n] = auswertung[n + 1][m].toString();
        }
        this.xLegend = new String("Runde");
        this.yLegend = cmd.name();
        this.dataLegends = tmpLegends;
    }

    private String getXLegend() {
        return this.xLegend;
    }

    private String getYLegend() {
        return this.yLegend;
    }

    private String[] getDataLegends() {
        return this.dataLegends;
    }

    private Object[][] getAuswertung() {
        return auswertung;
    }

    @SuppressWarnings("deprecation")
    public static void lineDiagrammErzeugen(Object[][] auswertung, int startRunde, int endRunde, Typ cmd, File file, Dimension d) {
        if (auswertung != null) {
            Diagramm diagramm = new Diagramm(auswertung, cmd);
            startRunde--;
            if (endRunde > diagramm.m) {
                endRunde = diagramm.m;
            }
            if (startRunde < 0) {
                startRunde = 0;
            }
            if (startRunde >= endRunde) {
                startRunde = 0;
                endRunde = diagramm.m;
            }
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for (int n = diagramm.n - 1; n >= 0; n--) {
                for (int m = startRunde; m < endRunde; m++) {
                    dataset.addValue((Integer) diagramm.getAuswertung()[n][m], diagramm.getDataLegends()[n], new Integer(m + 1));
                }
            }
            JFreeChart chart = ChartFactory.createLineChart("Auswertung: " + auswertung[0][0], diagramm.getXLegend(), diagramm.getYLegend(), dataset, PlotOrientation.VERTICAL, true, false, false);
            chart.setBackgroundPaint(new Color(255, 255, 255, 0));
            CategoryPlot plot = (CategoryPlot) chart.getPlot();
            plot.setBackgroundPaint(Color.white);
            plot.setRangeGridlinePaint(Color.black);
            LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
            renderer.setShapesVisible(true);
            renderer.setDrawOutlines(true);
            NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
            rangeAxis.setAutoRange(true);
            CategoryAxis timeAxis = plot.getDomainAxis();
            timeAxis.setLowerMargin(0.00);
            timeAxis.setUpperMargin(0.00);
            timeAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
            if (file != null) {
                saveChart(file, chart, d);
            }
        }
    }

    public static void pieDiagrammErzeugen(Object[][] auswertung, Typ cmd, File file, Dimension d) {
        if (auswertung != null) {
            Diagramm diagramm = new Diagramm(auswertung, cmd);
            DefaultPieDataset dataset = new DefaultPieDataset();
            for (int n = 0; n < diagramm.n; n++) {
                dataset.setValue(diagramm.dataLegends[n] + " " + (Integer) diagramm.auswertung[n][diagramm.m], (Integer) diagramm.auswertung[n][diagramm.m]);
            }
            JFreeChart chart = ChartFactory.createPieChart("Auswertung: " + auswertung[0][0].toString(), dataset, true, false, false);
            if (file != null) {
                saveChart(file, chart, d);
            }
        }
    }

    private static void saveChart(File file, JFreeChart chart, Dimension d) {
        try {
            ChartUtilities.saveChartAsPNG(file, chart, d.width, d.height, null, true, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    private static void displayChart(JFreeChart chart) {
        ChartFrame frame = new ChartFrame("Auswertung", chart);
        frame.pack();
        RefineryUtilities.centerFrameOnScreen(frame);
        frame.setVisible(true);
    }
}
