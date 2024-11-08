package gov.sns.apps.timestamptest;

import gov.sns.ca.*;
import gov.sns.tools.plot.*;
import java.text.NumberFormat;
import javax.swing.*;

public class CAMonitor {

    ChannelFactory caF = ChannelFactory.defaultFactory();

    Channel ca;

    double val;

    int maxCount = 1000;

    double[][] data;

    int counter = 0;

    boolean connected = false;

    String timestamp;

    Monitor caMonitor;

    PVPanel myPVPanel;

    TimeStampDocument myTSD;

    String pvName = "";

    NumberFormat nf = NumberFormat.getNumberInstance();

    BasicGraphData plotData = new BasicGraphData();

    boolean firstTime = true;

    protected int myTimeRange = 300;

    double min = 1000000000.;

    double max = -1000000000.;

    double yMin;

    double yMax;

    double a = 1.;

    double b = 0.;

    public CAMonitor(PVPanel pvPanel) {
        myPVPanel = pvPanel;
        pvName = pvPanel.getPVName();
        ca = caF.getChannel(pvName);
        ca.connectAndWait();
        connected = ca.isConnected();
        ca.addConnectionListener(new ConnectionListener() {

            public void connectionMade(Channel aChannel) {
                connected = true;
            }

            public void connectionDropped(Channel aChannel) {
                connected = false;
            }
        });
        nf.setMaximumFractionDigits(5);
        plotData.setImmediateContainerUpdate(false);
    }

    public String getPVName() {
        return pvName;
    }

    /**
	 * start a monitor
	 */
    protected void startMon() {
        if (connected) {
            try {
                caMonitor = ca.addMonitorValTime(new IEventSinkValTime() {

                    public void eventValue(ChannelTimeRecord newRecord, Channel chan) {
                        val = newRecord.doubleValue();
                        if (val > max) max = val;
                        if (val < min) min = val;
                        ((JTextField) (myTSD.myWindow().getYRangeField().get(getPVName()))).setText("min:" + nf.format(min) + ", max:" + nf.format(max) + ", last pt:" + nf.format(val));
                        String valS = nf.format(val);
                        timestamp = newRecord.getTimestamp().toString();
                        myPVPanel.getTextArea().append(timestamp + "  " + valS + "\n");
                        data[0][counter] = newRecord.getTimestamp().getSeconds();
                        data[1][counter] = val;
                        double tmin = data[0][counter] - myTimeRange;
                        myTSD.myWindow().getPlotPanel().setLimitsAndTicksX(tmin, myTimeRange / 2, 2);
                        double newPoint = a * data[1][counter] + b;
                        try {
                            plotData.addPoint(data[0][counter], newPoint);
                            if (!firstTime) {
                                plotData.removePoint(0);
                            }
                        } catch (ArrayIndexOutOfBoundsException e) {
                        }
                        myTSD.myWindow().getPlotPanel().refreshGraphJPanel();
                        if (counter >= maxCount - 1) {
                            counter = 0;
                            firstTime = false;
                            myPVPanel.getTextArea().setText("");
                        } else {
                            counter++;
                        }
                    }
                }, Monitor.VALUE);
            } catch (ConnectionException e) {
                System.out.println("Cannot connect to " + ca.getId());
            } catch (MonitorException e) {
            }
        }
    }

    protected BasicGraphData getGraphData() {
        return plotData;
    }

    protected void reset() {
        data = new double[2][maxCount];
        myPVPanel.getTextArea().setText("");
    }

    /**
	 * stop the running monitor
	 */
    protected void stopMon() {
        if (caMonitor != null) caMonitor.clear();
    }

    protected double[][] getData() {
        return data;
    }

    protected void setMaxLength(int max) {
        maxCount = max;
        data = new double[2][maxCount];
    }

    protected int getMaxLength() {
        return maxCount;
    }

    protected void setTSDocument(TimeStampDocument tsd) {
        myTSD = tsd;
    }

    protected void setDisplayTimeRange(int timeRange) {
        myTimeRange = timeRange;
    }

    protected double getYMin() {
        return yMin;
    }

    protected double getYMax() {
        return yMax;
    }

    protected void setDisplayLimits(double dmin, double dmax, double globalMin, double globalMax) {
        stopMon();
        a = (globalMax - globalMin) / (dmax - dmin);
        b = (dmax * globalMin - dmin * globalMax) / (dmax - dmin);
        double[] xArray = new double[counter];
        double[] yArray = new double[counter];
        for (int i = 0; i < counter; i++) {
            xArray[i] = data[0][i];
            yArray[i] = data[1][i] * a + b;
        }
        plotData.removeAllPoints();
        for (int i = 0; i < counter; i++) {
            plotData.addPoint(xArray[i], yArray[i]);
        }
        startMon();
    }

    protected void resetDisplayLimits() {
        stopMon();
        double[] xArray = new double[counter];
        double[] yArray = new double[counter];
        for (int i = 0; i < counter; i++) {
            xArray[i] = data[0][i];
            yArray[i] = data[1][i];
        }
        plotData.removeAllPoints();
        for (int i = 0; i < counter; i++) {
            plotData.addPoint(xArray[i], yArray[i]);
        }
        a = 1.;
        b = 0.;
        startMon();
    }
}
