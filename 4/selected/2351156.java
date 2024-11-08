package de.iritgo.openmetix.linechart.gui;

import de.iritgo.openmetix.app.alarm.SensorAlarmEvent;
import de.iritgo.openmetix.app.configurationsensor.ConfigurationSensor;
import de.iritgo.openmetix.app.gui.InstrumentGUIPane;
import de.iritgo.openmetix.app.history.HistoricalDataRequestAction;
import de.iritgo.openmetix.app.instrument.InstrumentDisplay;
import de.iritgo.openmetix.app.userprofile.Preferences;
import de.iritgo.openmetix.app.userprofile.UserProfile;
import de.iritgo.openmetix.app.util.TimeConverter;
import de.iritgo.openmetix.app.util.Tools;
import de.iritgo.openmetix.core.Engine;
import de.iritgo.openmetix.core.action.ActionProcessorRegistry;
import de.iritgo.openmetix.core.command.Command;
import de.iritgo.openmetix.core.gui.GUIPane;
import de.iritgo.openmetix.core.iobject.IObject;
import de.iritgo.openmetix.core.logger.Log;
import de.iritgo.openmetix.core.network.ClientTransceiver;
import de.iritgo.openmetix.framework.action.ActionTools;
import de.iritgo.openmetix.framework.appcontext.AppContext;
import de.iritgo.openmetix.framework.command.CommandTools;
import de.iritgo.openmetix.linechart.LineChart;
import de.iritgo.openmetix.linechart.LineChartSensor;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardLegend;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.StandardXYItemRenderer;
import org.jfree.chart.renderer.XYItemRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This gui pane is used to display line charts.
 *
 * @version $Id: LineChartDisplay.java,v 1.1 2005/04/24 18:10:45 grappendorf Exp $
 */
public class LineChartDisplay extends InstrumentGUIPane implements InstrumentDisplay {

    /** A list of sensor configurations. */
    private List sensorSeriesEntrys;

    /** True if the history was already requested. */
    private List historyRequestAlreadySend;

    /** True if a history request was made. */
    private boolean historyFired;

    /** The line chart. */
    private JFreeChart chart;

    /** Line chart panel. */
    private ChartPanel chartPanel;

    /** True if we had already received a measurement value. */
    private boolean measurementReceived;

    /**
	 * Information about a sensor to display.
	 */
    public static class SensorSeriesEntry {

        /** The id of the sensor. */
        public long sensorId;

        /** The data series for the sensor chart. */
        public TimeSeries series;

        /** The time converter. */
        public TimeConverter converter;

        /**
		 * Create a new SensorSeriesEntry.
		 *
		 * @param sensorId The id of the sensor.
		 * @param series The data series for the sensor chart.
		 * @param ratio The measurement rate in milliseconds (specify 0 for no conversion).
		 */
        public SensorSeriesEntry(long sensorId, TimeSeries series, long ratio) {
            this.sensorId = sensorId;
            this.series = series;
            this.converter = new TimeConverter(ratio);
        }
    }

    /**
	 * Create a new LineChartDisplay.
	 */
    public LineChartDisplay() {
        super("LineChartDisplay");
        sensorSeriesEntrys = new LinkedList();
        historyRequestAlreadySend = new LinkedList();
        historyFired = false;
    }

    /**
	 * Return a sample of the data object that is displayed in this gui pane.
	 *
	 * @return The sample oject.
	 */
    public IObject getSampleObject() {
        return new LineChart();
    }

    /**
	 * Initialize the gui. Subclasses should override this method to create a
	 * custom gui.
	 */
    public GUIPane cloneGUIPane() {
        return new LineChartDisplay();
    }

    /**
	 * Initialize the gui. Subclasses should override this method to create a
	 * custom gui.
	 */
    public void initGUI() {
        chart = ChartFactory.createTimeSeriesChart(null, "Zeit", "", null, true, true, false);
        chart.setBackgroundPaint(content.getBackground());
        NumberAxis axis = (NumberAxis) chart.getXYPlot().getRangeAxis();
        axis.setTickLabelsVisible(false);
        chartPanel = Tools.createInstrumentChartPanel(chart, this);
        content.add(chartPanel, createConstraints(0, 0, 1, 1, GridBagConstraints.BOTH, 100, 100, null));
        configureDisplay();
    }

    /**
	 * Load the gui values from the data object attributes.
	 */
    public void loadFromObject() {
        final LineChart lineChart = (LineChart) iobject;
        if (!isDisplayValid(lineChart)) {
            return;
        }
        final LineChartDisplay display = this;
        for (Iterator i = lineChart.sensorConfigIterator(); i.hasNext(); ) {
            LineChartSensor config = (LineChartSensor) i.next();
            if (config.isValid() && config.getSensor().isValid()) {
                configureSensor(config, lineChart);
            }
        }
        configure(new Command() {

            public void perform() {
                configureChart(chart, lineChart, display);
                setTitle(lineChart.getTitle());
            }
        });
        if (getDisplay().getProperty("metixReload") != null) {
            CommandTools.performSimple("StatusProgressStep");
            getDisplay().removeProperty("metixReload");
        }
    }

    /**
	 * Configure the chart.
	 *
	 * @param chart The chart to configure.
	 * @param lineChart The line chart instrument.
	 * @param lineChartDisplay The line chart display.
	 */
    public static void configureChart(JFreeChart chart, LineChart lineChart, LineChartDisplay lineChartDisplay) {
        XYPlot plot = chart.getXYPlot();
        plot.setDomainGridlinesVisible(lineChart.showDomainRasterLines());
        plot.setRangeGridlinesVisible(lineChart.showRangeRasterLines());
        plot.setDomainGridlinePaint(new Color(lineChart.getGridColor()));
        plot.setRangeGridlinePaint(new Color(lineChart.getGridColor()));
        if (lineChart.showLegend()) {
            chart.setLegend(new StandardLegend());
        } else {
            chart.setLegend(null);
        }
        ValueAxis axis = plot.getDomainAxis();
        axis.setVerticalTickLabels(true);
        axis.setTickLabelsVisible(lineChart.showDomainTickLabels());
        axis.setLabel(lineChart.showDomainLabels() ? lineChart.getFormattedAxisLabel() : "");
        axis.setTickLabelFont(Font.decode(lineChart.getFont()));
        axis.setLabelFont(Font.decode(lineChart.getFont()));
        switch(lineChart.getDomainAxisMode()) {
            case LineChart.MODE_INTERVAL:
                {
                    axis.setAutoRange(false);
                    axis.setRange(lineChart.getDomainStartDate(), lineChart.getDomainStopDate());
                    break;
                }
            case LineChart.MODE_LASTTIME:
                {
                    axis.setAutoRange(false);
                    axis.setRange(System.currentTimeMillis() - lineChart.getHistoryCount(), System.currentTimeMillis());
                    axis.setFixedAutoRange(lineChart.getHistoryCount());
                    axis.setAutoRange(true);
                    break;
                }
        }
    }

    /**
	 * Configure a sensor.
	 *
	 * @param sensor The sensor to configure.
	 * @param lineChart The line chart instrument.
	 */
    public void configureSensor(LineChartSensor sensor, LineChart lineChart) {
        if (historyRequestAlreadySend.contains(sensor)) {
            return;
        }
        incSensorCount();
        double ratioD = lineChart.getHistoryCount() / 360.0;
        long ratio = ratioD < 1000.0 ? 0 : (long) ratioD;
        TimeSeries series = new TimeSeries(sensor.toString(), Millisecond.class);
        TimeSeriesCollection dataset = new TimeSeriesCollection(series);
        series.setHistoryCount((int) lineChart.getHistoryCount());
        sensorSeriesEntrys.add(new SensorSeriesEntry(sensor.getSensorId(), series, ratio));
        Log.logInfo("client", "LineChartDisplay.configureSensor", "Ratio for sensor <" + sensor.getSensorId() + ">: " + ratio);
        XYPlot plot = chart.getXYPlot();
        int sensorNr = sensor.getSensorNr();
        int mappedAxis = sensorNr;
        if (sensor.getAxisRangeMode() != LineChartSensor.MODE_TAKEFROM) {
            NumberAxis axis = new NumberAxis();
            plot.setRangeAxis(sensorNr, axis);
            axis.setLabel(lineChart.showRangeLabels() ? sensor.getFormattedAxisLabel() : "");
            axis.setLabelPaint(Color.black);
            axis.setTickLabelsVisible(lineChart.showRangeTickLabels());
            axis.setTickLabelPaint(Color.black);
            axis.setTickLabelFont(Font.decode(lineChart.getFont()));
            axis.setLabelFont(Font.decode(lineChart.getFont()));
            switch(sensor.getAxisRangeMode()) {
                case LineChartSensor.MODE_AUTO:
                    {
                        axis.setRange(-5, 5);
                        axis.setAutoRange(true);
                        axis.setAutoRangeIncludesZero(false);
                        break;
                    }
                case LineChartSensor.MODE_MANUAL:
                    {
                        axis.setRange(sensor.getAxisRangeStart(), sensor.getAxisRangeStop());
                        break;
                    }
            }
        } else {
            mappedAxis = sensor.getAxisTakeFrom();
        }
        XYItemRenderer renderer = new StandardXYItemRenderer();
        renderer.setSeriesPaint(0, new Color(sensor.getColor()));
        plot.setRenderer(sensorNr, renderer);
        plot.setDataset(sensorNr, dataset);
        plot.mapDatasetToRangeAxis(sensorNr, mappedAxis);
        historyRequestAlreadySend.add(sensor);
        double channelNumber = AppContext.instance().getChannelNumber();
        ClientTransceiver clientTransceiver = new ClientTransceiver(channelNumber);
        clientTransceiver.addReceiver(channelNumber);
        final HistoricalDataRequestAction historyDataServerAction = new HistoricalDataRequestAction(lineChart.getUniqueId(), sensor.getStationId(), sensor.getSensorId(), sensor.getStartDate(), sensor.getStopDate());
        historyDataServerAction.setTransceiver(clientTransceiver);
        final ActionProcessorRegistry actionProcessorRegistry = Engine.instance().getActionProcessorRegistry();
        CommandTools.performAsync(new Command("historyrequest") {

            public boolean canPerform() {
                return true;
            }

            public void perform() {
                ActionTools.sendToServer(historyDataServerAction);
            }
        });
    }

    /**
	 * This method receives sensor measurements.
	 *
	 * The measurement values are sent from the server to the client
	 * instrument displays. A display should check if it really displays
	 * measurments for the given sensor. In this case it should
	 * update itself accordingly to the measurement values.
	 *
	 * @param timestamp The timestamp on which the measurement was done.
	 * @param value The measurement value.
	 * @param stationId The id of the gaging station.
	 * @param sensorId The id of the gaging sensor.
	 */
    public void receiveSensorValue(Timestamp timestamp, double value, long stationId, long sensorId) {
        LineChart lineChart = (LineChart) iobject;
        if (!isDisplayValid(lineChart)) {
            return;
        }
        try {
            long nowMillis = timestamp.getTime();
            for (Iterator iter = sensorSeriesEntrys.iterator(); iter.hasNext(); ) {
                SensorSeriesEntry entry = (SensorSeriesEntry) iter.next();
                if (entry.sensorId == sensorId && lineChart.isValidTimeStamp(nowMillis)) {
                    if (entry.converter.putValue(value, timestamp)) {
                        Millisecond nowDate = new Millisecond(timestamp);
                        double avgValue = entry.converter.getAverageValue();
                        entry.series.add(nowDate, avgValue);
                        entry.converter.reset();
                    }
                }
            }
            for (Iterator i = lineChart.sensorConfigIterator(); i.hasNext(); ) {
                ConfigurationSensor sensorConfig = (ConfigurationSensor) i.next();
                if (sensorConfig.getStationId() == stationId && sensorConfig.getSensorId() == sensorId && (sensorConfig.isWarnMin() && value <= sensorConfig.getWarnMinValue()) || (sensorConfig.isWarnMax() && value >= sensorConfig.getWarnMaxValue())) {
                    Engine.instance().getEventRegistry().fire("sensoralarm", new SensorAlarmEvent(this));
                }
            }
        } catch (Exception x) {
        }
    }

    /**
	 * This method receives historical sensor measurements.
	 *
	 * The measurement values are sent from the server to the client
	 * instrument displays. A display should check if it really displays
	 * measurments for the given sensor. In this case it should
	 * update itself accordingly to the measurement values.
	 *
	 * @param timestamp The timestamp on which the measurement was done.
	 * @param value The measurement value.
	 * @param stationId The id of the gaging station.
	 * @param sensorId The id of the gaging sensor.
	 */
    public void receiveHistoricalSensorValue(long instumentUniqueId, Timestamp timestamp, double value, long stationId, long sensorId) {
        LineChart lineChart = (LineChart) iobject;
        if (instumentUniqueId == lineChart.getUniqueId()) {
            if (!isDisplayValid(lineChart)) {
                return;
            }
            for (Iterator iter = sensorSeriesEntrys.iterator(); iter.hasNext(); ) {
                SensorSeriesEntry entry = (SensorSeriesEntry) iter.next();
                if (entry.sensorId == sensorId && lineChart.isValidTimeStamp(timestamp.getTime())) {
                    Millisecond convDate = new Millisecond(timestamp);
                    entry.series.add(convDate, value);
                }
            }
        }
    }

    /**
	 * Configure the display.
	 */
    public void configureDisplay() {
        UserProfile userProfile = (UserProfile) AppContext.instance().getAppObject();
        Preferences preferences = userProfile.getPreferences();
        boolean antiAlised = preferences.getDrawAntiAliased();
        chart.setAntiAlias(antiAlised);
        Log.logInfo("client", "LineChartDisplay.configureDisplay", "Antialised: " + antiAlised);
    }

    /**
	 * Check wether this display is editable or not.
	 *
	 * @return True if the display is editable.
	 */
    public boolean isEditable() {
        return false;
    }

    /**
	 * Check wether this display is printable or not.
	 *
	 * @return True if the display is printable.
	 */
    public boolean isPrintable() {
        return true;
    }

    /**
	 * Print the display.
	 */
    public void print() {
        chartPanel.createChartPrintJob();
    }
}
