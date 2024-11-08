package jhomenet.ui.panel.plot;

import java.util.Calendar;
import java.util.List;
import java.beans.PropertyChangeEvent;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXButton;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.apache.log4j.Logger;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import jhomenet.commons.GeneralApplicationContext;
import jhomenet.commons.hw.HomenetHardware;
import jhomenet.commons.hw.Channel;
import jhomenet.commons.hw.HardwareListener;
import jhomenet.commons.hw.sensor.Sensor;
import jhomenet.commons.hw.data.HardwareData;
import jhomenet.commons.utils.FormatUtils;
import jhomenet.ui.action.*;
import jhomenet.ui.panel.*;
import jhomenet.ui.panel.plot.jfreechart.PlotAdapter;
import jhomenet.ui.panel.plot.jfreechart.PlotAdapterFactory;

/**
 * The abstract plotting panel.
 * 
 * @author David Irwin (jhomenet at gmail dot com)
 */
public abstract class AbstractPlot implements HardwareListener {

    /**
     * The logger
     */
    private static Logger logger = Logger.getLogger(AbstractPlot.class);

    /**
     * 
     */
    final PlotPanel plotPanel;

    /**
     * 
     */
    final GeneralApplicationContext serverContext;

    /**
     * Reference to the hardware object.
     */
    final HomenetHardware hardware;

    /**
     * Plot adapter
     */
    final PlotAdapter plotAdapter;

    /**
     * Plot title.
     */
    private final String plotTitle;

    /**
     * Reference to the collection of time series.
     */
    protected final TimeSeriesCollection timeseriesCollection = new TimeSeriesCollection();

    /**
     * 
     */
    private JXDatePicker startDate_dp, endDate_dp;

    /**
     * The start and end plot dates.
     */
    private Calendar plotStartDate, plotEndDate;

    /**
     * The previous plot start and end date
     */
    Calendar previousPlotStartDate, previousPlotEndDate;

    /**
     * The refresh button.
     */
    private JXButton refresh_b;

    /**
     * A flag to control whether the plot is refreshed on a sensor data update.
     */
    private boolean refreshPlotOnDataUpdate = true;

    /**
     * 
     */
    private JCheckBox update_cb;

    volatile boolean dataBeingUpdated = false;

    /**
     * Default constructor.
     * 
     * @param hardware
     * @param serverContext
     */
    public AbstractPlot(HomenetHardware hardware, PlotPanel plotPanel, GeneralApplicationContext serverContext) {
        super();
        if (hardware == null) throw new IllegalArgumentException("Sensor cannot be null!");
        if (plotPanel == null) throw new IllegalArgumentException("Plot panel cannot be null!");
        if (serverContext == null) throw new IllegalArgumentException("Server context cannot be null!");
        this.hardware = hardware;
        this.plotPanel = plotPanel;
        this.plotTitle = hardware.getHardwareSetupDescription();
        this.serverContext = serverContext;
        plotAdapter = PlotAdapterFactory.getPlotAdapter(hardware);
        initComponents();
    }

    /**
     * 
     */
    private void initComponents() {
        resetPlotDates();
        update_cb = new JCheckBox("Refresh plot on data update");
        update_cb.setSelected(refreshPlotOnDataUpdate);
        update_cb.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (update_cb.isSelected()) refreshPlotOnDataUpdate = true; else refreshPlotOnDataUpdate = false;
            }
        });
        startDate_dp = new JXDatePicker();
        startDate_dp.setDate(plotStartDate.getTime());
        startDate_dp.addActionListener(new ActionListener() {

            /**
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            public void actionPerformed(ActionEvent arg0) {
                plotStartDate.setTime(startDate_dp.getDate());
                logger.debug("Plot start date set to: " + FormatUtils.formatDateTime(plotStartDate.getTime()));
            }
        });
        endDate_dp = new JXDatePicker();
        endDate_dp.setDate(plotEndDate.getTime());
        endDate_dp.addActionListener(new ActionListener() {

            /**
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            public void actionPerformed(ActionEvent arg0) {
                plotEndDate.setTime(endDate_dp.getDate());
                logger.debug("Plot end date set to: " + FormatUtils.formatDateTime(plotEndDate.getTime()));
            }
        });
        this.refresh_b = new JXButton();
        this.refresh_b.setText("Refresh");
        this.refresh_b.setToolTipText("Refresh the plot data");
        this.refresh_b.addActionListener(new ActionListener() {

            /**
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            public void actionPerformed(ActionEvent ae) {
                buildPlot();
            }
        });
    }

    /**
     * Get the Y-label for the plot.
     * 
     * @return
     */
    protected String getYLabel() {
        return plotAdapter.getYAxisLabel();
    }

    /**
     * Build the actual plot.
     * 
     * @return <code>JPanel</code> containing the plot@return
     */
    public BackgroundPanel buildPlotPanel() {
        FormLayout panelLayout = new FormLayout("fill:default:grow", "fill:default:grow, 4dlu, pref");
        BackgroundPanel panel = new BackgroundPanel();
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(panelLayout, panel);
        builder.add(createChartPanel(), cc.xy(1, 1));
        builder.add(createOptionsPanel(), cc.xy(1, 3));
        panel = (BackgroundPanel) builder.getPanel();
        panel.redraw();
        return panel;
    }

    /**
     * Create the options panel.
     * 
     * @return
     */
    private JComponent createOptionsPanel() {
        FormLayout panelLayout = new FormLayout("4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu", "4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu");
        CellConstraints cc = new CellConstraints();
        PanelBuilder builder = new PanelBuilder(panelLayout);
        builder.add(update_cb, cc.xyw(2, 2, 3));
        builder.addSeparator("Plot window", cc.xyw(2, 4, 5));
        builder.addLabel("Set the plot window (the plot's start and end time)", cc.xyw(2, 6, 5));
        builder.addLabel("Plot start/end times: ", cc.xy(2, 8));
        builder.add(startDate_dp, cc.xy(4, 8));
        builder.add(endDate_dp, cc.xy(6, 8));
        builder.add(ButtonBarFactory.buildRightAlignedBar(refresh_b), cc.xy(6, 10));
        JPanel panel = builder.getPanel();
        panel.setBorder(javax.swing.BorderFactory.createTitledBorder("Plot Options"));
        return panel;
    }

    /**
     * Create the actual chart given the XY data set.
     * 
     * @param xydataset
     * @return A reference to a newly create <code>JFreeChart</code>
     */
    private final JComponent createChartPanel() {
        JFreeChart jfreechart = ChartFactory.createTimeSeriesChart(plotTitle, "Time", getYLabel(), timeseriesCollection, true, true, false);
        plotAdapter.configure(jfreechart);
        ChartPanel chartPanel = new ChartPanel(jfreechart);
        chartPanel.setPreferredSize(new Dimension(250, 225));
        return chartPanel;
    }

    /**
     * Reset the plot dates.
     */
    protected void resetPlotDates() {
        this.plotStartDate = Calendar.getInstance();
        this.plotStartDate.add(Calendar.DAY_OF_MONTH, -3);
        this.previousPlotStartDate = this.plotStartDate;
        this.plotEndDate = Calendar.getInstance();
        this.previousPlotEndDate = this.plotEndDate;
    }

    /**
     * Builds the plot.
     */
    private void buildPlot() {
        logger.debug("Building the plot");
        logger.debug("  Plot start date: " + this.plotStartDate.toString());
        logger.debug("  Plot end date: " + this.plotEndDate.toString());
        boolean origUpdateValue = this.refreshPlotOnDataUpdate;
        this.refreshPlotOnDataUpdate = false;
        RetrieveHardwareDataAction retrieveDataAction = new RetrieveHardwareDataAction(hardware.getHardwareAddr(), plotStartDate.getTime(), plotEndDate.getTime(), serverContext);
        try {
            retrieveDataAction.runWithProgressWindow();
            List<HardwareData> dataList = retrieveDataAction.getResultSynchronously();
            plotData(dataList, Boolean.TRUE);
        } catch (ActionException ae) {
            logger.error("Error while loading hardware data: " + ae.getMessage());
        } finally {
            this.refreshPlotOnDataUpdate = origUpdateValue;
        }
    }

    /**
     * 
     * @param hardware
     * @param channel
     */
    final void clearAllTimeseries() {
        List<Channel> channels = hardware.getChannels();
        for (Channel channel : channels) {
            TimeSeries ts = timeseriesCollection.getSeries(buildTimeSeriesKey(hardware, channel.getChannelNum()));
            ts.clear();
        }
    }

    /**
     * 
     * @param dataList
     * @param clearExistingData
     */
    final void plotData(List<HardwareData> dataList, Boolean clearExistingData) {
        logger.debug("Adding data to time series: " + hardware.getHardwareAddr() + ", size: " + dataList.size());
        if (clearExistingData) this.clearAllTimeseries();
        for (HardwareData data : dataList) plotData(data);
    }

    /**
     * 
     * @param dataList
     */
    final void plotData(List<HardwareData> dataList) {
        plotData(dataList, Boolean.FALSE);
    }

    /**
     * 
     * @param originalData
     */
    final void plotData(final HardwareData originalData) {
        final HardwareData data = modifyData(originalData);
        Runnable updateRunnable = new Runnable() {

            public void run() {
                plotDataOnEDT(data);
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            updateRunnable.run();
        } else {
            SwingUtilities.invokeLater(updateRunnable);
        }
    }

    /**
     * Add data to the plot. 
     * <p>
     * This method should only be called from the EDT.
     * 
     * @param data Hardware data to add to the plot
     */
    private void plotDataOnEDT(final HardwareData data) {
        final Calendar dataDate = Calendar.getInstance();
        dataDate.setTime(data.getTimestamp());
        if (this.refreshPlotOnDataUpdate || (dataDate.after(plotStartDate) && dataDate.before(plotEndDate))) {
            TimeSeries ts = timeseriesCollection.getSeries(buildTimeSeriesKey(hardware, data.getChannel()));
            addDataInternal(ts, data);
        }
    }

    /**
     * This method is intended to be overriden by implementing classes in order
     * to provide the ability to modify the data before is it plotted.
     * 
     * @param data
     * @return
     */
    HardwareData modifyData(HardwareData data) {
        return data;
    }

    /**
     * 
     * @param data
     * @return
     */
    abstract void addDataInternal(TimeSeries ts, HardwareData data);

    /**
     * 
     * @param hardware
     * @param channel
     * @return
     */
    String buildTimeSeriesKey(HomenetHardware hardware, Integer channel) {
        return hardware.getChannelDescription(channel) + " [CH-" + channel + "]";
    }

    /**
     * 
     * @param sensor
     * @param channel
     * @return
     */
    String buildMovingAverageKey(Sensor sensor, Integer channel) {
        return buildTimeSeriesKey(sensor, channel) + " moving average";
    }

    /**
     * Reset the time series objects.
     */
    public void resetAllTimeseries() {
        for (int i = 0; i < timeseriesCollection.getSeriesCount(); i++) timeseriesCollection.getSeries(i).clear();
    }

    /**
     * Listen for changes in the hardware properties. By default, this method does
     * nothing. Interested sub-classes should override this method to recieve the
     * sensor property change events.
     * 
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent event) {
        this.plotPanel.getPanelGroupManager().updatePanels();
    }
}
