package org.rdv.viz.chart;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.rdv.data.DataChannel;
import org.rdv.data.DataFileReader;
import org.rdv.data.NumericDataSample;
import org.rdv.datapanel.AbstractDataPanel;
import org.rdv.rbnb.Channel;
import com.rbnb.sapi.ChannelMap;

/**
 * A data panel to plot data time series and xy charts. 
 * 
 * @author Jason P. Hanley
 */
public abstract class ChartViz extends AbstractDataPanel {

    /**
   * The logger for this class.
   */
    static Log log = LogFactory.getLog(ChartViz.class.getName());

    /** the data panel property to control the legend visibility */
    private static final String DATA_PANEL_PROPERTY_SHOW_LOGEND = "showLegend";

    /**
   * The chart.
   */
    JFreeChart chart;

    /**
   * The xy plot for this chart.
   */
    XYPlot xyPlot;

    /**
   * The domain (horizontal) axis that contains a value. This will be a number
   * axis for an xy plot or a date axis for a timeseries plot.
   */
    ValueAxis domainAxis;

    /**
   * The range (vertical) axis that contains a number.
   */
    NumberAxis rangeAxis;

    /**
   * The component that renderers the chart.
   */
    ChartPanel chartPanel;

    /**
   * The data set for the chart.
   */
    XYDataset dataCollection;

    /**
   * The legend for the series in the chart.
   */
    LegendTitle seriesLegend;

    /**
   * The container for the chart component.
   */
    JPanel chartPanelPanel;

    /** the menu item to control legend visibility */
    private JCheckBoxMenuItem showLegendMenuItem;

    /**
   * A bit to indicate if we are plotting time series charts of x vs. y charts.
   */
    final boolean xyMode;

    /**
   * The timestamp for the last piece if data displayed.
   */
    double lastTimeDisplayed;

    /**
   * The number of local data series.
   */
    int localSeries;

    /**
   * A channel map used to cache the values of an xy data set when only one
   * channel has been added.
   */
    ChannelMap cachedChannelMap;

    /**
   * Plot colors for each series.
   */
    HashMap<String, Color> colors;

    /**
   * Colors used for the series.
   */
    static final Color[] seriesColors = { Color.decode("#FF0000"), Color.decode("#0000FF"), Color.decode("#009900"), Color.decode("#FF9900"), Color.decode("#9900FF"), Color.decode("#FF0099"), Color.decode("#0099FF"), Color.decode("#990000"), Color.decode("#000099"), Color.black };

    /**
   * The file chooser UI used to select a local data file.
   */
    JFileChooser chooser;

    /** a flag to control the legend visibility, defaults to true */
    private boolean showLegend;

    /**
   * Constructs a chart data panel in time series mode.
   */
    public ChartViz() {
        this(false);
    }

    /**
   * Constructs a chart data panel.
   * 
   * @param xyMode  if true in x vs. y mode, otherwise in time series mode
   */
    public ChartViz(boolean xyMode) {
        super();
        this.xyMode = xyMode;
        lastTimeDisplayed = -1;
        colors = new HashMap<String, Color>();
        showLegend = true;
        initChart();
        setDataComponent(chartPanelPanel);
    }

    /**
   * Create the chart and setup it's UI.
   */
    private void initChart() {
        XYToolTipGenerator toolTipGenerator;
        if (xyMode) {
            dataCollection = new XYTimeSeriesCollection();
            NumberAxis domainAxis = new NumberAxis();
            domainAxis.setAutoRangeIncludesZero(false);
            domainAxis.addChangeListener(new AxisChangeListener() {

                public void axisChanged(AxisChangeEvent ace) {
                    boundsChanged();
                }
            });
            this.domainAxis = domainAxis;
            toolTipGenerator = new StandardXYToolTipGenerator("{0}: {1} , {2}", new DecimalFormat(), new DecimalFormat());
        } else {
            dataCollection = new TimeSeriesCollection();
            domainAxis = new FixedAutoAdjustRangeDateAxis();
            domainAxis.setLabel("Time");
            domainAxis.setAutoRange(false);
            toolTipGenerator = new StandardXYToolTipGenerator("{0}: {1} , {2}", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"), new DecimalFormat());
        }
        rangeAxis = new NumberAxis();
        rangeAxis.setAutoRangeIncludesZero(false);
        rangeAxis.addChangeListener(new AxisChangeListener() {

            public void axisChanged(AxisChangeEvent ace) {
                boundsChanged();
            }
        });
        FastXYItemRenderer renderer = new FastXYItemRenderer(StandardXYItemRenderer.LINES, toolTipGenerator);
        renderer.setBaseCreateEntities(false);
        renderer.setBaseStroke(new BasicStroke(0.5f));
        if (xyMode) {
            renderer.setCursorVisible(true);
        }
        xyPlot = new XYPlot(dataCollection, domainAxis, rangeAxis, renderer);
        chart = new JFreeChart(xyPlot);
        chart.setAntiAlias(false);
        seriesLegend = chart.getLegend();
        chart.removeLegend();
        chartPanel = new ChartPanel(chart, true);
        chartPanel.setInitialDelay(0);
        JPopupMenu popupMenu = chartPanel.getPopupMenu();
        final JMenuItem copyChartMenuItem = new JMenuItem("Copy");
        copyChartMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                copyChart();
            }
        });
        popupMenu.insert(copyChartMenuItem, 2);
        popupMenu.insert(new JPopupMenu.Separator(), 3);
        popupMenu.add(new JPopupMenu.Separator());
        showLegendMenuItem = new JCheckBoxMenuItem("Show Legend", true);
        showLegendMenuItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                setShowLegend(showLegendMenuItem.isSelected());
            }
        });
        popupMenu.add(showLegendMenuItem);
        if (xyMode) {
            popupMenu.add(new JPopupMenu.Separator());
            JMenuItem addLocalSeriesMenuItem = new JMenuItem("Add local series...");
            addLocalSeriesMenuItem.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent ae) {
                    addLocalSeries();
                }
            });
            popupMenu.add(addLocalSeriesMenuItem);
        }
        chartPanelPanel = new JPanel();
        chartPanelPanel.setLayout(new BorderLayout());
        chartPanelPanel.add(chartPanel, BorderLayout.CENTER);
    }

    /**
   * Takes the chart and puts it on the clipboard as an image.
   */
    private void copyChart() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Dimension preferredDimension = chartPanel.getPreferredSize();
        Image image = chart.createBufferedImage((int) preferredDimension.getWidth(), (int) preferredDimension.getHeight());
        ImageSelection contents = new ImageSelection(image);
        clipboard.setContents(contents, null);
    }

    /**
   * Add data from a local file as a series to this chart. This will ask the
   * user for the file name, and which channels to use.
   */
    private void addLocalSeries() {
        if (chooser == null) {
            chooser = new JFileChooser();
        }
        int returnVal = chooser.showOpenDialog(getDataComponent());
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        if (file == null || !file.isFile() || !file.exists()) {
            return;
        }
        DataFileReader reader;
        try {
            reader = new DataFileReader(file);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(getDataComponent(), e.getMessage(), "Problem reading data file", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<DataChannel> channels = reader.getChannels();
        if (channels.size() < 2) {
            JOptionPane.showMessageDialog(getDataComponent(), "There must be at least 2 channels in the data file", "Problem with data file", JOptionPane.ERROR_MESSAGE);
            return;
        }
        DataChannel xChannel;
        DataChannel yChannel;
        if (channels.size() == 2) {
            xChannel = channels.get(0);
            yChannel = channels.get(1);
        } else {
            xChannel = (DataChannel) JOptionPane.showInputDialog(getDataComponent(), "Select the x channel:", "Add local channel", JOptionPane.PLAIN_MESSAGE, null, channels.toArray(), null);
            if (xChannel == null) {
                return;
            }
            yChannel = (DataChannel) JOptionPane.showInputDialog(getDataComponent(), "Select the y channel:", "Add local channel", JOptionPane.PLAIN_MESSAGE, null, channels.toArray(), null);
            if (yChannel == null) {
                return;
            }
        }
        String xChannelName = xChannel.getName();
        if (xChannel.getUnit() != null) {
            xChannelName += " (" + xChannel.getUnit() + ")";
        }
        int xChannelIndex = channels.indexOf(xChannel);
        String yChannelName = yChannel.getName();
        if (yChannel.getUnit() != null) {
            yChannelName += " (" + yChannel.getUnit() + ")";
        }
        int yChannelIndex = channels.indexOf(yChannel);
        String seriesName = xChannelName + " vs. " + yChannelName;
        XYTimeSeries data = new XYTimeSeries(seriesName, FixedMillisecond.class);
        try {
            NumericDataSample sample;
            while ((sample = reader.readSample()) != null) {
                double timestamp = sample.getTimestamp();
                Number[] values = sample.getValues();
                FixedMillisecond time = new FixedMillisecond((long) (timestamp * 1000));
                XYTimeSeriesDataItem dataItem = new XYTimeSeriesDataItem(time);
                if (values[xChannelIndex] != null && values[yChannelIndex] != null) {
                    dataItem.setX(values[xChannelIndex]);
                    dataItem.setY(values[yChannelIndex]);
                }
                data.add(dataItem, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        Color color = getLeastUsedColor();
        colors.put(seriesName, color);
        ((XYTimeSeriesCollection) dataCollection).addSeries(data);
        localSeries++;
        setSeriesColors();
        updateTitle();
        updateLegend();
    }

    /**
   * Remove the local series from the chart.
   * 
   * @param seriesName  the name of the local series.
   */
    private void removeLocalSeries(String seriesName) {
        XYTimeSeries series = ((XYTimeSeriesCollection) dataCollection).getSeries(seriesName);
        if (series == null) {
            return;
        }
        localSeries--;
        ((XYTimeSeriesCollection) dataCollection).removeSeries(series);
        colors.remove(seriesName);
        setSeriesColors();
        updateTitle();
        updateLegend();
    }

    /**
   * Called when the bounds of an axis are changed. This updates the data panel
   * properties for these values.
   */
    private void boundsChanged() {
        if (xyMode) {
            if (domainAxis.isAutoRange()) {
                properties.remove("domainLowerBound");
                properties.remove("domainUpperBound");
            } else {
                properties.setProperty("domainLowerBound", Double.toString(domainAxis.getLowerBound()));
                properties.setProperty("domainUpperBound", Double.toString(domainAxis.getUpperBound()));
            }
        }
        if (rangeAxis.isAutoRange()) {
            properties.remove("rangeLowerBound");
            properties.remove("rangeUpperBound");
        } else {
            properties.setProperty("rangeLowerBound", Double.toString(rangeAxis.getLowerBound()));
            properties.setProperty("rangeUpperBound", Double.toString(rangeAxis.getUpperBound()));
        }
    }

    /**
   * Indicates that this data panel can support multiple channels. This always
   * returns true.
   * 
   * @return  always true
   */
    public boolean supportsMultipleChannels() {
        return true;
    }

    /**
   * Called when a channel has been added.
   * 
   * @param channelName  the new channel
   */
    protected void channelAdded(String channelName) {
        String channelDisplay = getChannelDisplay(channelName);
        String seriesName = null;
        Color color = null;
        if (xyMode) {
            if (channels.size() % 2 == 0) {
                String firstChannelName = (String) channels.get(channels.size() - 2);
                String firstChannelDisplay = getChannelDisplay(firstChannelName);
                seriesName = firstChannelDisplay + " vs. " + channelDisplay;
                color = getLeastUsedColor();
                XYTimeSeries data = new XYTimeSeries(seriesName, FixedMillisecond.class);
                data.setMaximumItemAge((long) (timeScale * 1000), (long) (time * 1000));
                int position = dataCollection.getSeriesCount() - localSeries;
                ((XYTimeSeriesCollection) dataCollection).addSeries(position, data);
            }
        } else {
            seriesName = channelDisplay;
            color = getLeastUsedColor();
            FastTimeSeries data = new FastTimeSeries(seriesName, FixedMillisecond.class);
            data.setMaximumItemAge((long) (timeScale * 1000), (long) (time * 1000));
            ((TimeSeriesCollection) dataCollection).addSeries(data);
        }
        if (seriesName != null) {
            colors.put(seriesName, color);
            setSeriesColors();
        }
        updateTitle();
        updateLegend();
    }

    /**
   * Remove the channel from the data panel.
   * 
   * @param channelName  the channel to remove
   * @return             true if the channel was removed, false otherwise
   */
    public boolean removeChannel(String channelName) {
        if (xyMode) {
            if (!channels.contains(channelName)) {
                return false;
            }
            int channelIndex = channels.indexOf(channelName);
            String firstChannel, secondChannel;
            if (channelIndex % 2 == 0) {
                firstChannel = channelName;
                if (channelIndex + 1 < channels.size()) {
                    secondChannel = (String) channels.get(channelIndex + 1);
                } else {
                    secondChannel = null;
                }
            } else {
                firstChannel = (String) channels.get(channelIndex - 1);
                secondChannel = channelName;
            }
            rbnbController.unsubscribe(firstChannel, this);
            channels.remove(firstChannel);
            if (secondChannel != null) {
                rbnbController.unsubscribe(secondChannel, this);
                channels.remove(secondChannel);
                String firstChannelDisplay = getChannelDisplay(firstChannel);
                String secondChannelDisplay = getChannelDisplay(secondChannel);
                String seriesName = firstChannelDisplay + " vs. " + secondChannelDisplay;
                XYTimeSeriesCollection dataCollection = (XYTimeSeriesCollection) this.dataCollection;
                XYTimeSeries data = dataCollection.getSeries(seriesName);
                dataCollection.removeSeries(data);
                colors.remove(seriesName);
            }
            channelRemoved(channelName);
            return true;
        } else {
            return super.removeChannel(channelName);
        }
    }

    /**
   * Called when a channel has been removed.
   * 
   * @param  the name of the channel that was removed
   */
    protected void channelRemoved(String channelName) {
        if (!xyMode) {
            String channelDisplay = getChannelDisplay(channelName);
            TimeSeriesCollection dataCollection = (TimeSeriesCollection) this.dataCollection;
            TimeSeries data = dataCollection.getSeries(channelDisplay);
            dataCollection.removeSeries(data);
            colors.remove(channelDisplay);
        }
        setSeriesColors();
        updateTitle();
        updateLegend();
    }

    /**
   * Return a color that is least used.
   * 
   * @return            the color
   */
    private Color getLeastUsedColor() {
        int usage = -1;
        Color color = null;
        for (int i = 0; i < seriesColors.length; i++) {
            int seriesUsingColor = getSeriesUsingColor(seriesColors[i]);
            if (usage == -1 || seriesUsingColor < usage) {
                usage = seriesUsingColor;
                color = seriesColors[i];
            }
        }
        return color;
    }

    /**
   * Count the number of series using the specified color for their series
   * plot.
   * 
   * @param color          the color to find
   * @return               the number of series using this color
   */
    private int getSeriesUsingColor(Color color) {
        if (color == null) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < dataCollection.getSeriesCount(); i++) {
            Paint p = xyPlot.getRenderer().getSeriesPaint(i);
            if (p.equals(color)) {
                count++;
            }
        }
        return count;
    }

    /**
   * Set the color for all the series.
   */
    private void setSeriesColors() {
        for (int i = 0; i < dataCollection.getSeriesCount(); i++) {
            String series = (String) dataCollection.getSeriesKey(i);
            xyPlot.getRenderer().setSeriesPaint(i, colors.get(series));
        }
    }

    /**
   * Shows or hides the legend.
   * 
   * @param showLegend  if true, the legend will show, otherwise it will not
   */
    private void setShowLegend(boolean showLegend) {
        if (this.showLegend == showLegend) {
            return;
        }
        this.showLegend = showLegend;
        showLegendMenuItem.setSelected(showLegend);
        if (showLegend) {
            properties.remove(DATA_PANEL_PROPERTY_SHOW_LOGEND);
        } else {
            properties.setProperty(DATA_PANEL_PROPERTY_SHOW_LOGEND, "false");
        }
        updateLegend();
    }

    /**
   * Update the legend and axis labels based on the series being viewed.
   */
    private void updateLegend() {
        int series = dataCollection.getSeriesCount();
        int chans = channels.size();
        if (xyMode) {
            if (series == 0 && chans == 1) {
                String channelDisplay = getChannelDisplay((String) channels.get(0));
                domainAxis.setLabel(channelDisplay);
                rangeAxis.setLabel(null);
            } else if (series == 1 && chans == 0) {
                XYTimeSeries xySeries = ((XYTimeSeriesCollection) dataCollection).getSeries(0);
                String seriesName = (String) xySeries.getKey();
                String[] channelNames = seriesName.split(" vs. ");
                if (channelNames.length == 2) {
                    domainAxis.setLabel(channelNames[0]);
                    rangeAxis.setLabel(channelNames[1]);
                }
            } else if (series == 1 && chans == 2) {
                String channelDisplay1 = getChannelDisplay((String) channels.get(0));
                domainAxis.setLabel(channelDisplay1);
                String channelDisplay2 = getChannelDisplay((String) channels.get(1));
                rangeAxis.setLabel(channelDisplay2);
            } else {
                domainAxis.setLabel(null);
                rangeAxis.setLabel(null);
            }
        } else {
            if (series == 1) {
                String channelDisplay = getChannelDisplay((String) channels.get(0));
                rangeAxis.setLabel(channelDisplay);
            } else {
                rangeAxis.setLabel(null);
            }
        }
        if (showLegend && series >= 2) {
            if (chart.getLegend() == null) {
                chart.addLegend(seriesLegend);
            }
        } else {
            if (chart.getLegend() != null) {
                seriesLegend = chart.getLegend();
            }
            chart.removeLegend();
        }
    }

    /**
   * Get the title of this data panel. This overides the super class
   * implementation to deal with x vs. y plots.
   * 
   * @return  the title of the data panel
   */
    @Override
    protected String getTitle() {
        if (xyMode) {
            int remoteSeries = dataCollection.getSeriesCount() - localSeries;
            String title = new String();
            Iterator<String> i = channels.iterator();
            while (i.hasNext()) {
                String firstChannel = i.next();
                title += firstChannel;
                if (i.hasNext()) {
                    String secondChannel = i.next();
                    title += " vs. " + secondChannel;
                    if (i.hasNext() || localSeries > 0) {
                        title += ", ";
                    }
                }
            }
            for (int j = remoteSeries; j < remoteSeries + localSeries; j++) {
                String seriesName = (String) dataCollection.getSeriesKey(j);
                title += seriesName;
                if (j < remoteSeries + localSeries - 1) {
                    title += ", ";
                }
            }
            return title;
        } else {
            return super.getTitle();
        }
    }

    /**
   * Get the component to display the channels in the header of the data panel.
   * This overides the super class implementation to deal with x vs. y plots.
   * 
   * @return  the component displaying the channels for the data panel
   */
    @Override
    protected JComponent getChannelComponent() {
        if (xyMode) {
            int remoteSeries = dataCollection.getSeriesCount() - localSeries;
            if (channels.size() == 0 && localSeries == 0) {
                return null;
            }
            JPanel titleBar = new JPanel();
            titleBar.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
            titleBar.setOpaque(false);
            if (isShowChannelsInTitle()) {
                Iterator<String> i = channels.iterator();
                while (i.hasNext()) {
                    String firstChannel = i.next();
                    String series = firstChannel;
                    if (i.hasNext()) {
                        series += " vs. " + i.next();
                    }
                    titleBar.add(new ChannelTitle(series, firstChannel));
                }
                for (int j = remoteSeries; j < remoteSeries + localSeries; j++) {
                    String seriesName = (String) dataCollection.getSeriesKey(j);
                    titleBar.add(new LocalChannelTitle(seriesName));
                }
            }
            return titleBar;
        } else {
            return super.getChannelComponent();
        }
    }

    /**
   * A channel title component for local channels.
   */
    class LocalChannelTitle extends ChannelTitle {

        /** serialization version identifier */
        private static final long serialVersionUID = -6278564478512657058L;

        /**
     * Create a local channel title.
     * 
     * @param seriesName  the name of the series
     */
        public LocalChannelTitle(String seriesName) {
            super(seriesName, seriesName);
        }

        /**
     * Return an actionlistener to remove this series.
     */
        protected ActionListener getActionListener(final String seriesName, final String channelName) {
            return new ActionListener() {

                public void actionPerformed(ActionEvent ae) {
                    removeLocalSeries(seriesName);
                }
            };
        }
    }

    /**
   * Get the string for this channel to display in the UI. This will show the
   * channel units if there are any.
   *  
   * @param channelName  the name of the channel
   * @return             the string to display the channel in the UI
   */
    private String getChannelDisplay(String channelName) {
        String seriesName = channelName;
        Channel channel = rbnbController.getChannel(channelName);
        if (channel != null) {
            String unit = channel.getMetadata("units");
            if (unit != null) {
                seriesName += " (" + unit + ")";
            }
        }
        return seriesName;
    }

    /**
   * Called when the time scale changes. This updates the maximum age of the
   * dataset.
   * 
   * @param newTimeScale  the new time scale
   */
    public void timeScaleChanged(double newTimeScale) {
        super.timeScaleChanged(newTimeScale);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                int series = dataCollection.getSeriesCount();
                if (xyMode) {
                    series -= localSeries;
                }
                for (int i = 0; i < series; i++) {
                    if (xyMode) {
                        XYTimeSeriesCollection xyTimeSeriesCollection = (XYTimeSeriesCollection) dataCollection;
                        XYTimeSeries data = xyTimeSeriesCollection.getSeries(i);
                        data.setMaximumItemAge((long) (timeScale * 1000), (long) (time * 1000));
                    } else {
                        TimeSeriesCollection timeSeriesCollection = (TimeSeriesCollection) dataCollection;
                        FastTimeSeries data = (FastTimeSeries) timeSeriesCollection.getSeries(i);
                        data.setMaximumItemAge((long) (timeScale * 1000), (long) (time * 1000));
                    }
                }
                if (!xyMode) {
                    domainAxis.setRange((time - timeScale) * 1000, time * 1000);
                    ((FixedAutoAdjustRangeDateAxis) domainAxis).setAutoAdjustRange((time - timeScale) * 1000, time * 1000);
                }
            }
        });
    }

    /**
   * Posts new data to the data panel.
   * 
   * @param channelMap  the channel map with the new data
   */
    public void postData(final ChannelMap channelMap) {
        cachedChannelMap = this.channelMap;
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                ChartViz.this.channelMap = channelMap;
            }
        });
    }

    /**
   * Posts the data in the channel map when in time series mode.
   * 
   * @param channelMap  the channel map with the new data
   */
    private void postDataTimeSeries(ChannelMap channelMap) {
        for (String channelName : channels) {
            int channelIndex = channelMap.GetIndex(channelName);
            if (channelIndex != -1) {
                postDataTimeSeries(channelMap, channelName, channelIndex);
            }
        }
    }

    /**
   * Posts the data in the channel map to the specified channel when in time
   * series mode.
   * 
   * @param channelMap    the channel map containing the new data
   * @param channelName   the name of the channel to post data to
   * @param channelIndex  the index of the channel in the channel map
   */
    private void postDataTimeSeries(ChannelMap channelMap, String channelName, int channelIndex) {
        TimeSeriesCollection dataCollection = (TimeSeriesCollection) this.dataCollection;
        FastTimeSeries timeSeriesData = (FastTimeSeries) dataCollection.getSeries(getChannelDisplay(channelName));
        if (timeSeriesData == null) {
            log.error("We don't have a data collection to post this data.");
            return;
        }
        try {
            double[] times = channelMap.GetTimes(channelIndex);
            int typeID = channelMap.GetType(channelIndex);
            FixedMillisecond time;
            chart.setNotify(false);
            timeSeriesData.startAdd(times.length);
            switch(typeID) {
                case ChannelMap.TYPE_FLOAT64:
                    double[] doubleData = channelMap.GetDataAsFloat64(channelIndex);
                    for (int i = 0; i < doubleData.length; i++) {
                        time = new FixedMillisecond((long) (times[i] * 1000));
                        timeSeriesData.add(time, doubleData[i]);
                    }
                    break;
                case ChannelMap.TYPE_FLOAT32:
                    float[] floatData = channelMap.GetDataAsFloat32(channelIndex);
                    for (int i = 0; i < floatData.length; i++) {
                        time = new FixedMillisecond((long) (times[i] * 1000));
                        timeSeriesData.add(time, floatData[i]);
                    }
                    break;
                case ChannelMap.TYPE_INT64:
                    long[] longData = channelMap.GetDataAsInt64(channelIndex);
                    for (int i = 0; i < longData.length; i++) {
                        time = new FixedMillisecond((long) (times[i] * 1000));
                        timeSeriesData.add(time, longData[i]);
                    }
                    break;
                case ChannelMap.TYPE_INT32:
                    int[] intData = channelMap.GetDataAsInt32(channelIndex);
                    for (int i = 0; i < intData.length; i++) {
                        time = new FixedMillisecond((long) (times[i] * 1000));
                        timeSeriesData.add(time, intData[i]);
                    }
                    break;
                case ChannelMap.TYPE_INT16:
                    short[] shortData = channelMap.GetDataAsInt16(channelIndex);
                    for (int i = 0; i < shortData.length; i++) {
                        time = new FixedMillisecond((long) (times[i] * 1000));
                        timeSeriesData.add(time, shortData[i]);
                    }
                    break;
                case ChannelMap.TYPE_INT8:
                    byte[] byteData = channelMap.GetDataAsInt8(channelIndex);
                    for (int i = 0; i < byteData.length; i++) {
                        time = new FixedMillisecond((long) (times[i] * 1000));
                        timeSeriesData.add(time, byteData[i]);
                    }
                    break;
                case ChannelMap.TYPE_STRING:
                case ChannelMap.TYPE_UNKNOWN:
                case ChannelMap.TYPE_BYTEARRAY:
                    log.error("Got byte array type for channel " + channelName + ". Don't know how to handle.");
                    break;
            }
            timeSeriesData.fireSeriesChanged();
            chart.setNotify(true);
            chart.fireChartChanged();
        } catch (Exception e) {
            log.error("Problem plotting data for channel " + channelName + ".");
            e.printStackTrace();
        }
    }

    /**
   * Posts a new time. This pulls data out of a posted channel map when in x vs.
   * y mode.
   * 
   * @param time  the new time
   */
    public void postTime(double time) {
        if (time < this.time) {
            clearData();
        }
        super.postTime(time);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                if (xyMode) {
                    postDataXY(channelMap, cachedChannelMap);
                } else if (channelMap != null) {
                    postDataTimeSeries(channelMap);
                    channelMap = null;
                }
                setTimeAxis();
            }
        });
    }

    /**
   * Posts the data in the channel map when in x vs. y mode.
   * 
   * @param channelMap        the new channel map
   * @param cachedChannelMap  the cached channel map
   */
    private void postDataXY(ChannelMap channelMap, ChannelMap cachedChannelMap) {
        int seriesCount = dataCollection.getSeriesCount() - localSeries;
        for (int i = 0; i < seriesCount; i++) {
            postDataXY(channelMap, cachedChannelMap, i);
        }
        lastTimeDisplayed = time;
    }

    /**
   * Posts the data in the channel map to the specified channel when in x vs. y
   * mode.
   * 
   * @param channelMap        the new channel map
   * @param cachedChannelMap  the cached channel map
   * @param series            the index of the series
   */
    private void postDataXY(ChannelMap channelMap, ChannelMap cachedChannelMap, int series) {
        if (!xyMode) {
            log.error("Tried to post X vs. Y data when not in xy mode.");
            return;
        }
        if (channelMap == null) {
            return;
        }
        Object[] channelsArray = channels.toArray();
        String xChannelName = (String) channelsArray[series * 2];
        String yChannelName = (String) channelsArray[series * 2 + 1];
        int xChannelIndex = channelMap.GetIndex(xChannelName);
        int yChannelIndex = channelMap.GetIndex(yChannelName);
        int firstXChannelIndex = -1;
        if (yChannelIndex == -1) {
            return;
        } else if (xChannelIndex == -1) {
            firstXChannelIndex = (cachedChannelMap == null) ? -1 : cachedChannelMap.GetIndex(xChannelName);
            if (firstXChannelIndex == -1) {
                cachedChannelMap = null;
                return;
            }
        }
        try {
            double[] times = channelMap.GetTimes(yChannelIndex);
            int startIndex = -1;
            double dataStartTime;
            if (lastTimeDisplayed == time) {
                dataStartTime = time - timeScale;
            } else {
                dataStartTime = lastTimeDisplayed;
            }
            for (int i = 0; i < times.length; i++) {
                if (times[i] > dataStartTime && times[i] <= time) {
                    startIndex = i;
                    break;
                }
            }
            if (startIndex == -1) {
                return;
            }
            int endIndex = startIndex;
            for (int i = times.length - 1; i > startIndex; i--) {
                if (times[i] <= time) {
                    endIndex = i;
                    break;
                }
            }
            XYTimeSeriesCollection dataCollection = (XYTimeSeriesCollection) this.dataCollection;
            XYTimeSeries xySeriesData = (XYTimeSeries) dataCollection.getSeries(series);
            int typeID = channelMap.GetType(yChannelIndex);
            FixedMillisecond time;
            chart.setNotify(false);
            xySeriesData.startAdd(times.length);
            switch(typeID) {
                case ChannelMap.TYPE_FLOAT64:
                    double[] xDoubleData = firstXChannelIndex == -1 ? channelMap.GetDataAsFloat64(xChannelIndex) : cachedChannelMap.GetDataAsFloat64(firstXChannelIndex);
                    double[] yDoubleData = channelMap.GetDataAsFloat64(yChannelIndex);
                    for (int i = startIndex; i <= endIndex; i++) {
                        time = new FixedMillisecond((long) (times[i] * 1000));
                        xySeriesData.add(time, xDoubleData[i], yDoubleData[i], false);
                    }
                    break;
                case ChannelMap.TYPE_FLOAT32:
                    float[] xFloatData = firstXChannelIndex == -1 ? channelMap.GetDataAsFloat32(xChannelIndex) : cachedChannelMap.GetDataAsFloat32(firstXChannelIndex);
                    float[] yFloatData = channelMap.GetDataAsFloat32(yChannelIndex);
                    for (int i = startIndex; i <= endIndex; i++) {
                        time = new FixedMillisecond((long) (times[i] * 1000));
                        xySeriesData.add(time, xFloatData[i], yFloatData[i], false);
                    }
                    break;
                case ChannelMap.TYPE_INT64:
                    long[] xLongData = firstXChannelIndex == -1 ? channelMap.GetDataAsInt64(xChannelIndex) : cachedChannelMap.GetDataAsInt64(firstXChannelIndex);
                    long[] yLongData = channelMap.GetDataAsInt64(yChannelIndex);
                    for (int i = startIndex; i <= endIndex; i++) {
                        time = new FixedMillisecond((long) (times[i] * 1000));
                        xySeriesData.add(time, xLongData[i], yLongData[i], false);
                    }
                    break;
                case ChannelMap.TYPE_INT32:
                    int[] xIntData = firstXChannelIndex == -1 ? channelMap.GetDataAsInt32(xChannelIndex) : cachedChannelMap.GetDataAsInt32(firstXChannelIndex);
                    int[] yIntData = channelMap.GetDataAsInt32(yChannelIndex);
                    for (int i = startIndex; i <= endIndex; i++) {
                        time = new FixedMillisecond((long) (times[i] * 1000));
                        xySeriesData.add(time, xIntData[i], yIntData[i], false);
                    }
                    break;
                case ChannelMap.TYPE_INT16:
                    short[] xShortData = firstXChannelIndex == -1 ? channelMap.GetDataAsInt16(xChannelIndex) : cachedChannelMap.GetDataAsInt16(firstXChannelIndex);
                    short[] yShortData = channelMap.GetDataAsInt16(yChannelIndex);
                    for (int i = startIndex; i <= endIndex; i++) {
                        time = new FixedMillisecond((long) (times[i] * 1000));
                        xySeriesData.add(time, xShortData[i], yShortData[i], false);
                    }
                    break;
                case ChannelMap.TYPE_INT8:
                    byte[] xByteData = firstXChannelIndex == -1 ? channelMap.GetDataAsInt8(xChannelIndex) : cachedChannelMap.GetDataAsInt8(firstXChannelIndex);
                    byte[] yByteData = channelMap.GetDataAsInt8(yChannelIndex);
                    for (int i = startIndex; i <= endIndex; i++) {
                        time = new FixedMillisecond((long) (times[i] * 1000));
                        xySeriesData.add(time, xByteData[i], yByteData[i], false);
                    }
                    break;
                case ChannelMap.TYPE_BYTEARRAY:
                case ChannelMap.TYPE_STRING:
                case ChannelMap.TYPE_UNKNOWN:
                    log.error("Don't know how to handle data type for " + xChannelName + " and " + yChannelName + ".");
                    break;
            }
            xySeriesData.fireSeriesChanged();
            chart.setNotify(true);
            chart.fireChartChanged();
            cachedChannelMap = null;
        } catch (Exception e) {
            log.error("Problem plotting data for channels " + xChannelName + " and " + yChannelName + ".");
            e.printStackTrace();
        }
    }

    /**
   * Sets the time axis to display within the current time and time scale. This
   * assumes it is called in the event dispatch thread.
   */
    private void setTimeAxis() {
        if (chart == null) {
            log.warn("Chart object is null. This shouldn't happen.");
            return;
        }
        int series = dataCollection.getSeriesCount();
        if (xyMode) {
            series -= localSeries;
        }
        for (int i = 0; i < series; i++) {
            if (xyMode) {
                XYTimeSeriesCollection xyTimeSeriesDataCollection = (XYTimeSeriesCollection) dataCollection;
                XYTimeSeries data = xyTimeSeriesDataCollection.getSeries(i);
                data.removeAgedItems((long) (time * 1000));
            } else {
                TimeSeriesCollection timeSeriesDataCollection = (TimeSeriesCollection) dataCollection;
                TimeSeries data = timeSeriesDataCollection.getSeries(i);
                data.removeAgedItems((long) (time * 1000), true);
            }
        }
        if (!xyMode) {
            domainAxis.setRange((time - timeScale) * 1000, time * 1000);
            ((FixedAutoAdjustRangeDateAxis) domainAxis).setAutoAdjustRange((time - timeScale) * 1000, time * 1000);
        }
    }

    /**
   * Removes all data from all the series.
   */
    void clearData() {
        if (chart == null) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                lastTimeDisplayed = -1;
                int series = dataCollection.getSeriesCount();
                if (xyMode) {
                    series -= localSeries;
                }
                for (int i = 0; i < series; i++) {
                    if (xyMode) {
                        XYTimeSeriesCollection xyTimeSeriesDataCollection = (XYTimeSeriesCollection) dataCollection;
                        XYTimeSeries data = xyTimeSeriesDataCollection.getSeries(i);
                        data.clear();
                    } else {
                        TimeSeriesCollection timeSeriesDataCollection = (TimeSeriesCollection) dataCollection;
                        TimeSeries data = timeSeriesDataCollection.getSeries(i);
                        data.clear();
                    }
                }
            }
        });
        log.info("Cleared data display.");
    }

    /**
   * Sets properties for the data panel.
   * 
   * @param key    the key for the property
   * @param value  the value for the property
   */
    public void setProperty(String key, String value) {
        super.setProperty(key, value);
        if (key != null && value != null) {
            if (key.equals("domainLowerBound")) {
                domainAxis.setLowerBound(Double.parseDouble(value));
            } else if (key.equals("domainUpperBound")) {
                domainAxis.setUpperBound(Double.parseDouble(value));
            } else if (key.equals("rangeLowerBound")) {
                rangeAxis.setLowerBound(Double.parseDouble(value));
            } else if (key.equals("rangeUpperBound")) {
                rangeAxis.setUpperBound(Double.parseDouble(value));
            } else if (key.equals(DATA_PANEL_PROPERTY_SHOW_LOGEND) && !Boolean.parseBoolean(value)) {
                setShowLegend(false);
            }
        }
    }

    /**
   * Get the name of this data panel.
   */
    public String toString() {
        return "JFreeChart Data Panel";
    }
}
