package de.iritgo.openmetix.windroseinstrument.gui;

import de.iritgo.openmetix.app.configurationsensor.ConfigurationSensor;
import de.iritgo.openmetix.app.gui.InstrumentGUIPane;
import de.iritgo.openmetix.app.history.HistoricalDataRequestAction;
import de.iritgo.openmetix.app.instrument.InstrumentDisplay;
import de.iritgo.openmetix.app.userprofile.Preferences;
import de.iritgo.openmetix.app.userprofile.UserProfile;
import de.iritgo.openmetix.app.util.Tools;
import de.iritgo.openmetix.core.Engine;
import de.iritgo.openmetix.core.action.ActionProcessorRegistry;
import de.iritgo.openmetix.core.command.Command;
import de.iritgo.openmetix.core.gui.GUIPane;
import de.iritgo.openmetix.core.gui.swing.IMenuItem;
import de.iritgo.openmetix.core.iobject.IObject;
import de.iritgo.openmetix.core.logger.Log;
import de.iritgo.openmetix.core.network.ClientTransceiver;
import de.iritgo.openmetix.framework.action.ActionTools;
import de.iritgo.openmetix.framework.appcontext.AppContext;
import de.iritgo.openmetix.framework.command.CommandTools;
import de.iritgo.openmetix.windroseinstrument.WindRoseInstrument;
import de.iritgo.openmetix.windroseinstrument.WindRoseInstrumentSensor;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.DefaultValueDataset;
import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

/**
 * This gui pane is used to display wind rose instruments.
 *
 * @version $Id: WindRoseInstrumentDisplay.java,v 1.1 2005/04/24 18:10:43 grappendorf Exp $
 */
public class WindRoseInstrumentDisplay extends InstrumentGUIPane implements InstrumentDisplay {

    /** The current value. */
    public JLabel valueText;

    /** The compass plot. */
    private MetixCompassPlot plot;

    /** The compass chart. */
    private JFreeChart chart;

    /** Compass chart panel. */
    private ChartPanel chartPanel;

    /** Chart data. */
    private DefaultValueDataset dataset;

    /** Chart data. */
    private DefaultValueDataset datasetMinExtrema;

    /** Chart data. */
    private DefaultValueDataset datasetHourExtrema;

    /** Our context menu. */
    JPopupMenu contextMenu;

    /** True if the history was already requested. */
    private List historyRequestAlreadySend;

    /** Needletype for specific needle which is depend on storm force. */
    private MetixLongNeedle longNeedle;

    /** Needletype for showing 10-Minutes Extrema */
    private ExtremaNeedle minExtremaNeedle;

    /** Needletype for showing 1-Hour Extrema */
    private ExtremaNeedle hourExtremaNeedle;

    /** Default date format. */
    private SimpleDateFormat fullDateFormat;

    private Calendar calendar;

    /** Store maximum and minimum value */
    private Hashtable historyValues;

    /** Needed to compute the data maximum value*/
    private double tempMinMax = -1;

    /** Needed to compute the data minimum value*/
    private double tempMinMin = -1;

    /** Maximum value of the latest 10-Minutes*/
    private double minMax = 0;

    /** Minimum value of the latest 10-Minutes*/
    private double minMin = 0;

    /** Needed to compute the data maximum value*/
    private double tempHourMax = -1;

    /** Needed to compute the data minimum value*/
    private double tempHourMin = -1;

    /** Maximum value of the latest hour*/
    private double hourMax;

    /** Minimum value of the latest hour*/
    private double hourMin;

    /** Contain and compute max. and min. values of the latest 10-Minutes */
    private ExtremaList minExtremaList;

    /** Contain and compute max. and min. values of the latest hour */
    private ExtremaList hourExtremaList;

    /**
	 * Create a new WindRoseInstrumentDisplay.
	 */
    public WindRoseInstrumentDisplay() {
        super("WindRoseInstrumentDisplay");
        longNeedle = new MetixLongNeedle();
        minExtremaNeedle = new ExtremaNeedle();
        hourExtremaNeedle = new ExtremaNeedle();
        historyRequestAlreadySend = new LinkedList();
        fullDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        minExtremaList = new ExtremaList();
        hourExtremaList = new ExtremaList();
    }

    /**
	 * Initialize the gui. Subclasses should override this method to create a
	 * custom gui.
	 */
    public void initGUI() {
        try {
            dataset = new DefaultValueDataset(new Double(0.0));
            datasetMinExtrema = new DefaultValueDataset(new Double(0.0));
            datasetHourExtrema = new DefaultValueDataset(new Double(0.0));
            plot = new MetixCompassPlot(dataset);
            plot.addData(datasetMinExtrema, minExtremaNeedle);
            plot.addData(datasetHourExtrema, hourExtremaNeedle);
            chart = new JFreeChart(null, plot);
            chart.setBackgroundPaint(content.getBackground());
            plot.setSeriesNeedle(2);
            plot.setSeriesPaint(0, new Color(204, 0, 51));
            plot.setRosePaint(new Color(255, 153, 0));
            chartPanel = Tools.createInstrumentChartPanel(chart, this);
            content.add(chartPanel, createConstraints(0, 0, 1, 1, GridBagConstraints.BOTH, 100, 100, null));
            contextMenu = new JPopupMenu();
            IMenuItem editItem = new IMenuItem(new AbstractAction() {

                public void actionPerformed(ActionEvent e) {
                    CommandTools.performAsync("EditInstrument");
                }
            });
            editItem.setText("metix.edit");
            contextMenu.add(editItem);
            MouseAdapter mouseListener = new MouseAdapter() {

                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        contextMenu.show(content, e.getX(), e.getY());
                    }
                }

                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        contextMenu.show(content, e.getX(), e.getY());
                    }
                }
            };
            content.addMouseListener(mouseListener);
            configureDisplay();
        } catch (Exception x) {
            Log.logError("client", "WindRoseInstrumentDisplay.initGUI", x.toString());
        }
    }

    /**
	 * Return a sample of the data object that is displayed in this gui pane.
	 *
	 * @return The sample oject.
	 */
    public IObject getSampleObject() {
        return new WindRoseInstrument();
    }

    /**
	 * Return a clone of this gui pane.
	 *
	 * @return The gui pane clone.
	 */
    public GUIPane cloneGUIPane() {
        return new WindRoseInstrumentDisplay();
    }

    /**
	 * Load the gui values from the data object attributes.
	 */
    public void loadFromObject() {
        final WindRoseInstrument windRoseInstrument = (WindRoseInstrument) iobject;
        if (!isDisplayValid(windRoseInstrument)) {
            return;
        }
        WindRoseInstrumentSensor config = (WindRoseInstrumentSensor) windRoseInstrument.getSensorConfig();
        if (config.isValid() && config.getSensor().isValid()) {
            configureSensor(config, windRoseInstrument);
        }
        configure(new Command() {

            public void perform() {
                UserProfile userProfile = (UserProfile) AppContext.instance().getAppObject();
                final Preferences preferences = userProfile.getPreferences();
                chart.setAntiAlias(preferences.getDrawAntiAliased());
                chart.setBackgroundPaint(content.getBackground());
                if (windRoseInstrument.getNeedleType() == 8) {
                    plot.setSeriesNeedle(0, longNeedle);
                    plot.setSeriesPaint(0, new Color(windRoseInstrument.getNeedleColor()));
                } else {
                    plot.setSeriesNeedle(windRoseInstrument.getNeedleType());
                    plot.setSeriesPaint(0, new Color(windRoseInstrument.getNeedleColor()));
                }
                plot.setRosePaint(new Color(windRoseInstrument.getRoseColor()));
                plot.setCompassFont(Font.decode(windRoseInstrument.getFont()));
                setTitle(windRoseInstrument.getTitle());
            }
        });
        if (getDisplay().getProperty("metixReload") != null) {
            CommandTools.performSimple("StatusProgressStep");
            getDisplay().removeProperty("metixReload");
        }
    }

    /**
	 * This method is called when the gui pane starts waiting
	 * for the attributes of it's iobject.
	 */
    public void waitingForNewObject() {
        setConfigured(false);
    }

    /**
	 * Configure a sensor.
	 *
	 * @param sensor The sensor to configure.
	 * @param lineChart The line chart instrument.
	 */
    public void configureSensor(WindRoseInstrumentSensor sensor, WindRoseInstrument windRoseInstrument) {
        if (historyRequestAlreadySend.contains(sensor)) {
            return;
        }
        incSensorCount();
        historyRequestAlreadySend.add(sensor);
        double channelNumber = AppContext.instance().getChannelNumber();
        ClientTransceiver clientTransceiver = new ClientTransceiver(channelNumber);
        clientTransceiver.addReceiver(channelNumber);
        if (windRoseInstrument.getMinExtrema() == 1) {
            Calendar calendar = new GregorianCalendar();
            calendar.add(Calendar.MINUTE, -10);
            Date startDate = calendar.getTime();
            Date stopDate = new Date();
            windRoseInstrument.getSensorConfig().setStartDate(startDate);
            windRoseInstrument.getSensorConfig().setStopDate(stopDate);
            final HistoricalDataRequestAction historyDataServerAction = new HistoricalDataRequestAction(windRoseInstrument.getUniqueId(), sensor.getStationId(), sensor.getSensorId(), sensor.getStartDate(), sensor.getStopDate());
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
        if (windRoseInstrument.getHourExtrema() == 1) {
            Calendar calendar = new GregorianCalendar();
            calendar.add(Calendar.HOUR_OF_DAY, -1);
            Date startDate = calendar.getTime();
            Date stopDate = new Date();
            windRoseInstrument.getSensorConfig().setStartDate(startDate);
            windRoseInstrument.getSensorConfig().setStopDate(stopDate);
            final HistoricalDataRequestAction historyDataServerAction = new HistoricalDataRequestAction(windRoseInstrument.getUniqueId(), sensor.getStationId(), sensor.getSensorId(), sensor.getStartDate(), sensor.getStopDate());
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
        WindRoseInstrument windRoseInstrument = (WindRoseInstrument) iobject;
        if (windRoseInstrument == null) {
            return;
        }
        if (!isDisplayValid(windRoseInstrument)) {
            return;
        }
        if (windRoseInstrument.getSensorConfig(1).getSensorId() == sensorId) {
            longNeedle.setNeedleValue((float) value);
        }
        if (windRoseInstrument.getSensorConfig().getSensorId() == sensorId && value >= 0.0 && value <= 360.0) {
            if (windRoseInstrument.getMinExtrema() == 1) {
                minExtremaList.addValue(timestamp, value, true, false);
                minExtremaList.computeMax();
                minExtremaList.computeMin();
                double tempValue = (minExtremaList.getExtremaMax() - minExtremaList.getExtremaMin()) / 2;
                minExtremaNeedle.setStartPoint(minExtremaList.getExtremaMin() + tempValue);
                minExtremaNeedle.setEndPoint(tempValue);
                plot.setSeriesNeedle(1, minExtremaNeedle);
                plot.setSeriesPaint(1, new Color(windRoseInstrument.getMinExtremaColor()));
                datasetMinExtrema.setValue(new Double(minExtremaList.getExtremaMin() + tempValue));
            }
            if (windRoseInstrument.getMinExtrema() == 0) {
                plot.setSeriesNeedle(1, minExtremaNeedle);
                plot.setSeriesPaint(1, new Color(windRoseInstrument.getMinExtremaColor()));
                minExtremaNeedle.setStartPoint(0.0);
                minExtremaNeedle.setEndPoint(0.0);
                datasetMinExtrema.setValue(new Double(0.0));
            }
            if (windRoseInstrument.getHourExtrema() == 1) {
                hourExtremaList.addValue(timestamp, value, false, true);
                hourExtremaList.computeMax();
                hourExtremaList.computeMin();
                double tempValue = (hourExtremaList.getExtremaMax() - hourExtremaList.getExtremaMin()) / 2;
                hourExtremaNeedle.setStartPoint(hourExtremaList.getExtremaMin() + tempValue);
                hourExtremaNeedle.setEndPoint(tempValue);
                plot.setSeriesNeedle(2, hourExtremaNeedle);
                plot.setSeriesPaint(2, new Color(windRoseInstrument.getHourExtremaColor()));
                datasetHourExtrema.setValue(new Double(hourExtremaList.getExtremaMin() + tempValue));
            }
            if (windRoseInstrument.getHourExtrema() == 0) {
                plot.setSeriesNeedle(2, hourExtremaNeedle);
                plot.setSeriesPaint(2, new Color(windRoseInstrument.getHourExtremaColor()));
                hourExtremaNeedle.setStartPoint(0.0);
                hourExtremaNeedle.setEndPoint(0.0);
                datasetHourExtrema.setValue(new Double(0.0));
            }
            ConfigurationSensor sensorConfig = ((WindRoseInstrument) iobject).getSensorConfig();
            try {
                if (windRoseInstrument.getNeedleType() != 7) {
                    if (value < 180.0) {
                        value += 180.0;
                    } else {
                        value -= 180.0;
                    }
                }
                if (windRoseInstrument.getReverseNeedle() == 0) {
                    dataset.setValue(new Double(value));
                } else {
                    dataset.setValue(new Double(value - 180));
                }
            } catch (Exception x) {
            }
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
        WindRoseInstrument windRoseInstrument = (WindRoseInstrument) iobject;
        if (instumentUniqueId == windRoseInstrument.getUniqueId()) {
            if (windRoseInstrument.getMinExtrema() == 1) {
                minExtremaList.addValue(timestamp, value, true, false);
            }
            if (windRoseInstrument.getHourExtrema() == 1) {
                hourExtremaList.addValue(timestamp, value, false, true);
            }
        }
    }

    /**
	 * Configure the display new.
	 */
    public void configureDisplay() {
    }

    /**
	 * Check wether this display is editable or not.
	 *
	 * @return True if the display is editable.
	 */
    public boolean isEditable() {
        return true;
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
