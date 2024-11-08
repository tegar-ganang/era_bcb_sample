package gov.sns.apps.xyzcorrelator;

import java.net.*;
import java.io.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import gov.sns.application.*;
import gov.sns.xal.smf.application.*;
import gov.sns.xal.smf.*;
import gov.sns.xal.smf.data.*;
import gov.sns.tools.xml.*;
import gov.sns.tools.data.*;
import gov.sns.ca.*;
import gov.sns.ca.correlator.*;
import gov.sns.tools.correlator.*;
import gov.sns.tools.apputils.SimpleChartPopupMenu;
import com.klg.jclass.chart.*;
import com.klg.jclass.chart3d.data.JCDefault3dPointDataSource;
import com.klg.jclass.util.legend.JCLegend;

/**
 * MyDocument the managing unit for objecs used by an xyzCorrelator instance
 *
 * @author  jdg
 */
public class XyzDocument extends AcceleratorDocument implements CorrelationNotice, Runnable {

    /** the names of the PVs with active connections */
    protected String xpvSelected = null;

    protected String ypvSelected = null;

    protected String zpvSelected = null;

    protected String xDev = null;

    protected String yDev = null;

    protected String zDev = null;

    protected JCChart graph;

    protected ChartDataView dataView;

    protected ChartDataViewSeries series;

    protected JCAxis xAxis;

    protected JCAxis yAxis;

    protected JCAxis zAxis;

    /** Max and min values for the lot scales */
    protected Double xScaleMin, xScaleMax, yScaleMin, yScaleMax, zScaleMin, zScaleMax;

    protected gov.sns.tools.apputils.PVSelection.PVSelector xalTree1, xalTree2, xalTree3;

    /** the correlator */
    protected ChannelCorrelator correlator;

    /** a poster to get correlations at a regular period */
    private PeriodicPoster poster;

    /** boolean indicating whether the correlator is running */
    protected boolean correlatorRunning = false;

    /** data sourse to store correlated data */
    protected XyzDataSource theData;

    /** a class to handle color coding in the z axis */
    protected BubbleColor bubbleColor;

    /** A class to set the z axis color legend */
    protected ColorLegend colorLegend;

    /** time to wait between get attempts (sec) */
    private Double dwellTime;

    /** max timeStamp difference to consitute a correlated set (sec) */
    private Double deltaT;

    /** number of correlated points to collect before updating the plot */
    private int nCorrelations_plotUpdate;

    /** internal correlation counter */
    private int nCorrelations;

    /** number of active PVs cor correlation */
    protected int nCorrelationPVs;

    /** the controller for inputting settings for this doc */
    private PreferenceController preferenceController;

    /** Object to help save and restore settings */
    protected SetupIO setupIO;

    /** Create a new empty document */
    public XyzDocument() {
        super();
        deltaT = new Double(100.);
        dwellTime = new Double(0.2);
        nCorrelations_plotUpdate = 1;
        xScaleMin = new Double(-10.);
        xScaleMax = new Double(10.);
        yScaleMin = new Double(-10.);
        yScaleMax = new Double(10.);
        zScaleMin = new Double(-10.);
        zScaleMax = new Double(10.);
        nCorrelations = 0;
        correlator = new ChannelCorrelator(deltaT.doubleValue());
        poster = new PeriodicPoster(correlator, dwellTime.doubleValue());
        poster.addCorrelationNoticeListener(this);
        setupIO = new SetupIO(this);
        theData = new XyzDataSource(this);
        theData.setBufferLength(200);
        initComponents();
    }

    /** 
     * Create a new document loaded from the URL file 
     * @param url The URL of the file to load into the new document.
     */
    public XyzDocument(java.net.URL url) {
        this();
        if (url == null) {
            return;
        }
        setSource(url);
    }

    /** initializes components used in this document */
    protected void initComponents() {
        xalTree1 = new gov.sns.tools.apputils.PVSelection.PVSelector("X PV");
        xalTree1.setPVSelectedListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PVSelected(evt);
            }
        });
        xalTree2 = new gov.sns.tools.apputils.PVSelection.PVSelector("Y PV");
        xalTree2.setPVSelectedListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PVSelected(evt);
            }
        });
        xalTree3 = new gov.sns.tools.apputils.PVSelection.PVSelector("Z PV");
        xalTree3.setPVSelectedListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PVSelected(evt);
            }
        });
        graph = new JCChart();
        JCLineStyle jcls0 = new JCLineStyle(1, Color.red, JCLineStyle.NONE);
        JCLineStyle jcls1 = new JCLineStyle(1, Color.blue, JCLineStyle.NONE);
        JCLineStyle jcls2 = new JCLineStyle(1, Color.blue, JCLineStyle.SOLID);
        JCSymbolStyle jcss0 = new JCSymbolStyle(JCSymbolStyle.DOT, Color.red, 5);
        JCSymbolStyle jcss1 = new JCSymbolStyle(JCSymbolStyle.CROSS, Color.black, 10);
        JCSymbolStyle jcss2 = new JCSymbolStyle(JCSymbolStyle.NONE, Color.blue, 6);
        graph.getDataView(0).getChartStyle(1).setLineStyle(jcls1);
        graph.getDataView(0).getChartStyle(1).setSymbolStyle(jcss1);
        graph.getDataView(0).getChartStyle(0).setLineStyle(jcls0);
        graph.getDataView(0).getChartStyle(0).setSymbolStyle(jcss0);
        graph.getDataView(0).getChartStyle(2).setLineStyle(jcls2);
        graph.getDataView(0).getChartStyle(2).setSymbolStyle(jcss2);
        graph.setBackground(Color.white);
        dataView = graph.getDataView(0);
        dataView.setDataSource(theData);
        graph.setTrigger(0, new EventTrigger(0, EventTrigger.ZOOM));
        graph.setAllowUserChanges(true);
        graph.addMouseListener(new CorrelatorPlotPopupMenu(graph, this));
        xAxis = graph.getChartArea().getXAxis(0);
        if (xpvSelected != null) xAxis.setTitle(new JCAxisTitle(xpvSelected));
        xAxis.setMax(xScaleMax.doubleValue());
        xAxis.setMin(xScaleMin.doubleValue());
        yAxis = graph.getChartArea().getYAxis(0);
        if (ypvSelected != null) yAxis.setTitle(new JCAxisTitle(ypvSelected));
        yAxis.getTitle().setPlacement(JCLegend.WEST);
        yAxis.getTitle().setRotation(ChartText.DEG_270);
        yAxis.setMax(yScaleMax.doubleValue());
        yAxis.setMin(yScaleMin.doubleValue());
        series = dataView.getSeries(0);
        bubbleColor = new BubbleColor(series, theData);
        bubbleColor.setZMax(zScaleMax.doubleValue());
        bubbleColor.setZMin(zScaleMin.doubleValue());
        series.getStyle().setSymbolCustomShape(bubbleColor);
        colorLegend = new ColorLegend(bubbleColor);
    }

    /** reset the xalTrees if the accelerator has changed */
    public void acceleratorChanged() {
        xalTree1.setAccelerator(getAccelerator());
        xalTree2.setAccelerator(getAccelerator());
        xalTree3.setAccelerator(getAccelerator());
    }

    /**
     * Make a main window by instantiating the my custom window.  
     * set up any settings, tables etc. as defined in the input file (if any)
     */
    public void makeMainWindow() {
        mainWindow = new XyzWindow(this);
        preferenceController = new PreferenceController((JFrame) mainWindow, this);
        if (getSource() != null) setupIO.readSetupFrom(getSource());
    }

    /**
     * Save the document to the specified URL.
     * @param url The URL to which the document should be saved.
     */
    public void saveDocumentAs(URL url) {
        setupIO.saveSetupTo(url);
        setHasChanges(false);
    }

    /**
     * Convenience method for getting the main window 
     * cast to the proper subclass of XalWindow.
     * This allows me to avoid casting the window every time I reference it.
     * @return The main window cast to its dynamic runtime class
     */
    protected XyzWindow myWindow() {
        return (XyzWindow) mainWindow;
    }

    /** set the dwell time between correlate attempts 
     *@param t new dwell time (sec)
     */
    public void setDwellTime(Double t) {
        poster.setPeriod(t.doubleValue());
        dwellTime = t;
    }

    /** get the dwell time between correlate attempts */
    public Double getDwellTime() {
        return dwellTime;
    }

    /** set the time window for correlated data
     *@param t correlation time window (sec)
     */
    public void setDeltaT(Double t) {
        deltaT = t;
        correlator.setBinTimespan(deltaT.doubleValue());
    }

    /** get the correlation time window */
    public Double getDeltaT() {
        return deltaT;
    }

    /** set the number of successful x-y correlations to collect, before updating the graph */
    public void setNCorrelations_plot(int n) {
        nCorrelations_plotUpdate = n;
    }

    /** set the number of successful x-y correlations to collect, before updating the graph */
    public int getNCorrelations_plot() {
        return nCorrelations_plotUpdate;
    }

    /**
     * the required interface to recieve notification of PV selection
     */
    public void PVSelected(java.awt.event.ActionEvent evt) {
        gov.sns.tools.apputils.PVSelection.PVSelector pvS = (gov.sns.tools.apputils.PVSelection.PVSelector) evt.getSource();
        Channel thePV = pvS.getSelectedChannel();
        if (thePV == null) {
            thePV = ChannelFactory.defaultFactory().getChannel(pvS.getPVText());
        }
        if (pvS.getLabel().equals("X PV")) {
            setXPV(thePV);
        } else if (pvS.getLabel().equals("Y PV")) {
            setYPV(thePV);
        } else if (pvS.getLabel().equals("Z PV")) {
            setZPV(thePV);
        }
        setHasChanges(true);
    }

    /** Set the X PV name and add to the correlator + graph */
    protected boolean setXPV(Channel name) {
        if (name.getId().equals(xpvSelected)) {
            return true;
        }
        if (correlatorRunning) stopCorrelator();
        theData.reset();
        if (xpvSelected != null) correlator.removeSource(xpvSelected);
        if (!checkConnection(name)) return false;
        xpvSelected = name.getId();
        correlator.addChannel(name);
        xAxis.setTitle(new JCAxisTitle(xpvSelected));
        return true;
    }

    /** Set the X PV name and add to the correlator + graph */
    protected boolean setXPV(String name) {
        if (name.equals(xpvSelected)) {
            return true;
        }
        if (correlatorRunning) stopCorrelator();
        theData.reset();
        if (xpvSelected != null) correlator.removeSource(xpvSelected);
        if (!checkConnection(name)) return false;
        xpvSelected = name;
        correlator.addChannel(name);
        xAxis.setTitle(new JCAxisTitle(xpvSelected));
        return true;
    }

    /** Set the Y PV name and add to the correlator + graph */
    protected boolean setYPV(Channel name) {
        if (name.getId().equals(ypvSelected)) {
            return true;
        }
        if (correlatorRunning) stopCorrelator();
        theData.reset();
        if (ypvSelected != null) correlator.removeSource(ypvSelected);
        if (!checkConnection(name)) return false;
        ypvSelected = name.getId();
        correlator.addChannel(name);
        yAxis.setTitle(new JCAxisTitle(ypvSelected));
        yAxis.getTitle().setPlacement(JCLegend.WEST);
        yAxis.getTitle().setRotation(ChartText.DEG_270);
        return true;
    }

    /** Set the Y PV name and add to the correlator + graph */
    protected boolean setYPV(String name) {
        if (name.equals(ypvSelected)) {
            return true;
        }
        if (correlatorRunning) stopCorrelator();
        theData.reset();
        if (ypvSelected != null) correlator.removeSource(ypvSelected);
        if (!checkConnection(name)) return false;
        ypvSelected = name;
        correlator.addChannel(name);
        yAxis.setTitle(new JCAxisTitle(ypvSelected));
        yAxis.getTitle().setPlacement(JCLegend.WEST);
        yAxis.getTitle().setRotation(ChartText.DEG_270);
        return true;
    }

    /** Set the Z PV name and add to the correlator + graph */
    protected boolean setZPV(Channel name) {
        if (name.getId().equals(zpvSelected)) {
            return true;
        }
        if (correlatorRunning) stopCorrelator();
        if (zpvSelected != null) correlator.removeSource(zpvSelected);
        theData.reset();
        if (name.equals("")) {
            zpvSelected = null;
            JCLineStyle jcls0 = new JCLineStyle(1, Color.red, JCLineStyle.NONE);
            JCSymbolStyle jcss0 = new JCSymbolStyle(JCSymbolStyle.DOT, Color.red, 6);
            graph.getDataView(0).getChartStyle(0).setLineStyle(jcls0);
            graph.getDataView(0).getChartStyle(0).setSymbolStyle(jcss0);
            return true;
        }
        if (!checkConnection(name)) return false;
        zpvSelected = name.getId();
        correlator.addChannel(name);
        return true;
    }

    /** Set the Z PV name and add to the correlator + graph */
    protected boolean setZPV(String name) {
        if (name.equals(zpvSelected)) {
            return true;
        }
        if (correlatorRunning) stopCorrelator();
        if (zpvSelected != null) correlator.removeSource(zpvSelected);
        theData.reset();
        if (name.equals("")) {
            zpvSelected = null;
            JCLineStyle jcls0 = new JCLineStyle(1, Color.red, JCLineStyle.NONE);
            JCSymbolStyle jcss0 = new JCSymbolStyle(JCSymbolStyle.DOT, Color.red, 6);
            graph.getDataView(0).getChartStyle(0).setLineStyle(jcls0);
            graph.getDataView(0).getChartStyle(0).setSymbolStyle(jcss0);
            return true;
        }
        if (!checkConnection(name)) return false;
        zpvSelected = name;
        correlator.addChannel(name);
        return true;
    }

    /** check to see if we can connect to this PV: */
    private boolean checkConnection(Channel name) {
        Channel tempChannel = name;
        try {
            tempChannel.checkConnection();
        } catch (ConnectionException e) {
            JOptionPane.showMessageDialog(myWindow(), "Opps - I can't connect to the PV called " + name, "Connection Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    /** check to see if we can connect to this PV: */
    private boolean checkConnection(String name) {
        Channel tempChannel = ChannelFactory.defaultFactory().getChannel(name);
        try {
            tempChannel.checkConnection();
        } catch (ConnectionException e) {
            JOptionPane.showMessageDialog(myWindow(), "Opps - I can't connect to the PV called " + name, "Connection Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    public synchronized void newCorrelation(Object Sender, Correlation correlation) {
        ChannelTimeRecord pvValue1, pvValue2, pvValue3;
        pvValue1 = (ChannelTimeRecord) (correlation.getRecord(xpvSelected));
        pvValue2 = (ChannelTimeRecord) (correlation.getRecord(ypvSelected));
        if (nCorrelationPVs == 2) {
            theData.addNewPoints(pvValue1, pvValue2);
        } else {
            pvValue3 = (ChannelTimeRecord) (correlation.getRecord(zpvSelected));
            theData.addNewPoints(pvValue1, pvValue2, pvValue3);
        }
        nCorrelations++;
        if (nCorrelations >= nCorrelations_plotUpdate) {
            refreshGraph();
            nCorrelations = 0;
        }
    }

    /** update the data view (graph) of the data */
    public void refreshGraph() {
        Thread update = new Thread(this);
        update.start();
    }

    public void run() {
        theData.setPrimitives();
        dataView.setDataSource(theData);
        if (nCorrelationPVs == 2) return;
        series = dataView.getSeries(0);
    }

    public synchronized void noCorrelationCaught(Object sender) {
        System.out.println("No Correlation found");
    }

    /** This method controls the action when the correlator is
     * either stopped or started */
    protected void startStopCorrelator(java.awt.event.ActionEvent event) {
        if (correlatorRunning) {
            stopCorrelator();
            return;
        }
        if (xpvSelected == null || ypvSelected == null) {
            JOptionPane.showMessageDialog(null, "Hmmmm - Looks like X, or Y is not selected yet", "Start Error", JOptionPane.ERROR_MESSAGE);
            System.out.println("Pick alt least  x PV and y PV first, then try start again");
            return;
        }
        if (zpvSelected == null) {
            nCorrelationPVs = 2;
            myWindow().theColorChooser.setVisible(false);
        } else {
            nCorrelationPVs = 3;
            myWindow().theColorChooser.setVisible(true);
        }
        ((JLabel) graph.getFooter()).setText("");
        correlator.startMonitoring();
        poster.start();
        correlatorRunning = true;
        myWindow().startTime = new Date();
        String startTimeText = "start time: " + myWindow().startTime.toString();
        myWindow().startTimeLabel.setText(startTimeText);
        String stopTimeText = "stop time: N/A";
        myWindow().stopTimeLabel.setText(stopTimeText);
        myWindow().startStopButton.setText("Stop");
    }

    /** the  method to handle stop button clicks */
    protected void stopCorrelator() {
        if (!correlatorRunning) return;
        poster.stop();
        correlatorRunning = false;
        myWindow().startStopButton.setText("Start");
        myWindow().stopTime = new Date();
        myWindow().stopTimeText = "stop time: " + myWindow().stopTime.toString();
        myWindow().stopTimeLabel.setText(myWindow().stopTimeText);
    }

    /** start the fitting of the x and y data */
    protected void fitData(java.awt.event.ActionEvent event) {
        if (correlatorRunning) stopCorrelator();
        theData.doFit();
        JLabel foot = new JLabel(theData.fitLabel);
        graph.setFooter((JComponent) foot);
        dataView = graph.getDataView(0);
        dataView.setDataSource(theData);
    }

    /** method to handle resetting of the correlator */
    protected void resetCorrelator(java.awt.event.ActionEvent event) {
        if (correlatorRunning) {
            stopCorrelator();
        }
        theData.reset();
        dataView = graph.getDataView(0);
        dataView.setDataSource(theData);
    }

    /** method to export data */
    private void exportData() throws Exception {
        if (theData.getNPoints() < 1) {
            JOptionPane.showMessageDialog(null, "Hey - you don't have any data collected yet to export!", "Export Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("No collectred data to export");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        int status = chooser.showSaveDialog(myWindow());
        if (status == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            theData.saveToFile(file);
        }
    }

    /** method to handle preferrence edits */
    protected void customizeCommands(Commander commander) {
        Action preferenceAction = new AbstractAction() {

            public void actionPerformed(ActionEvent event) {
                preferenceController.showNear(myWindow());
            }
        };
        preferenceAction.putValue(Action.NAME, "edit-preferences");
        commander.registerAction(preferenceAction);
        Action exportAction = new AbstractAction() {

            public void actionPerformed(ActionEvent event) {
                try {
                    exportData();
                } catch (Exception e) {
                }
            }
        };
        exportAction.putValue(Action.NAME, "export-data");
        commander.registerAction(exportAction);
    }
}
