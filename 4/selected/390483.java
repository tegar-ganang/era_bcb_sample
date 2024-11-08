package hu.sztaki.lpds.monitor.tracefile;

import hu.sztaki.lpds.monitor.*;
import java.util.*;
import java.util.Properties;
import java.io.*;

/**

 * <p>Title: TraceFileMonitor</p>

 * <p>Description: The main class for monitoring traces.</p>

 */
public class TraceFileMonitor implements TraceFileMonitorFacade {

    private static TraceFileMonitor instance = null;

    Hashtable monitorConsumers = null;

    TraceFileStore tfStore = null;

    /**

   * Returns instance.

   * @return

   * @throws TraceFileException

   */
    public static TraceFileMonitorFacade getInstance() throws TraceFileException {
        if (!isMonitoringOn()) {
            return (TraceFileMonitorFacade) new TraceFileMonitorEmpty();
        }
        if (TraceFileMonitor.instance == null) {
            TraceFileMonitor.instance = new TraceFileMonitor();
        }
        return (TraceFileMonitorFacade) TraceFileMonitor.instance;
    }

    /**

   * Returns instance.

   * @return

   * @throws TraceFileException

   */
    public static TraceFileMonitorFacade getInstance(String tracefileSaveDir) throws TraceFileException {
        if (!isMonitoringOn()) {
            return (TraceFileMonitorFacade) new TraceFileMonitorEmpty();
        }
        if (TraceFileMonitor.instance == null) {
            TraceFileMonitor.instance = new TraceFileMonitor(tracefileSaveDir);
        }
        return (TraceFileMonitorFacade) TraceFileMonitor.instance;
    }

    private static int inst;

    /**

   * Constructor.

   * @throws TraceFileException

   */
    private TraceFileMonitor() throws TraceFileException {
        System.out.println("TF - TraceFileMonitor - constructor");
        this.tfStore = TraceFileStore.getInstance();
        this.monitorConsumers = new Hashtable();
    }

    /**

   * Constructor.

   * @throws TraceFileException

   */
    private TraceFileMonitor(String tracefileSaveDir) throws TraceFileException {
        this.tfStore = TraceFileStore.getInstance(tracefileSaveDir);
        this.monitorConsumers = new Hashtable();
    }

    public static String getTimeString() {
        Calendar c = Calendar.getInstance();
        return "[" + (c.get(Calendar.MONTH) + 1) + "." + c.get(Calendar.DAY_OF_MONTH) + " " + c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND) + "] ";
    }

    public boolean startTraceFileMonitoring(Object jobID, Object host) {
        TraceFileMonitorFacadeThread tfmThread = new TraceFileMonitorFacadeThread(this, TraceFileMonitorFacadeThread.FUNCTION_START_MONITORING, "" + jobID, "" + host);
        return true;
    }

    /**

   * Starts the monitoring of a given job's trace.

   * @param jobID Job's ID.

   * @param host Hostname.

   * @return

   */
    public boolean startTraceFileMonitoring_VOLT(Object jobID, Object host) {
        System.out.println(this.getTimeString() + " TF - TraceFileMonitor.startTraceFileMonitoring(" + jobID + "," + host + ") ~ START.");
        JobToMetricMappingStore mappingStore = JobToMetricMappingStore.getInstance();
        String hostClean = this.getCleanHostname("" + host);
        if (mappingStore.get(hostClean, jobID) != null) {
            this.endTraceFileMonitoring(jobID, host);
        }
        TraceFileId tfId = new TraceFileId(hostClean, jobID);
        this.tfStore.addTraceFile(tfId, TraceFileStore.generateUniqueFilename(tfId));
        if (this.subscribe(host, jobID, tfId)) {
            this.tfStore.setStatus(tfId, TraceFileStore.MONITORING);
            System.out.println(this.getTimeString() + " TF - TraceFileMonitor.startTraceFileMonitoring(" + jobID + "," + host + ") : true ~ END.");
            return true;
        } else {
            this.tfStore.deleteTraceFile(tfId);
            System.out.println(this.getTimeString() + " TF - TraceFileMonitor.startTraceFileMonitoring(" + jobID + "," + host + ") : false ~ END.");
            return false;
        }
    }

    private String getCleanHostname(String hostname) {
        return hostname;
    }

    /**

   * Pauses the monitoring of a given job's trace.

   * @param jobID Job's ID.

   * @param host Hostname.

   */
    public void pauseTraceFileMonitoring(Object jobID, Object host) {
        System.out.println(this.getTimeString() + " TF - TraceFileMonitor.pauseTraceFileMonitoring(" + jobID + "," + host + ") ~ START");
        host = this.getCleanHostname("" + host);
        this.tfStore.setStatus(new TraceFileId(host, jobID), TraceFileStore.PAUSED);
        System.out.println(this.getTimeString() + " TF - TraceFileMonitor.pauseTraceFileMonitoring(" + jobID + "," + host + ") ~ END");
    }

    /**

   * Resumes the monitoring of a given job's trace.

   * @param jobID Job's ID.

   * @param host Hostname.

   */
    public void resumeTraceFileMonitoring(Object jobID, Object host) {
        System.out.println(this.getTimeString() + " TF - TraceFileMonitor.resumeTraceFileMonitoring(" + jobID + "," + host + ") ~ START");
        host = this.getCleanHostname("" + host);
        this.tfStore.setStatus(new TraceFileId(host, jobID), TraceFileStore.MONITORING);
        System.out.println(this.getTimeString() + " TF - TraceFileMonitor.resumeTraceFileMonitoring(" + jobID + "," + host + ") ~ END");
    }

    public void endTraceFileMonitoring(Object jobID, Object host) {
        TraceFileMonitorFacadeThread tfmThread = new TraceFileMonitorFacadeThread(this, TraceFileMonitorFacadeThread.FUNCTION_END_MONITORING, "" + jobID, "" + host);
        return;
    }

    /**

   * Ends the monitoring of a given job's trace.

   * @param jobID Job's ID.

   * @param host Hostname.

   */
    public void endTraceFileMonitoring_VOLT(Object jobID, Object host) {
        System.out.println(this.getTimeString() + " TF - TraceFileMonitor.endTraceFileMonitoring(" + jobID + "," + host + ") ~ START");
        host = this.getCleanHostname("" + host);
        TraceFileId tfId = new TraceFileId(host, jobID);
        this.tfStore.deleteTraceFile(tfId);
        JobToMetricMappingStore mappingStore = JobToMetricMappingStore.getInstance();
        MetricNChannelPair mNc = mappingStore.get(host, jobID);
        if (mNc == null) {
            System.out.println("TF - TraceFileMonitor.endTraceFileMonitoring(" + jobID + "," + host + ") ~ mNc == null.");
        } else {
            System.out.println("TF - TraceFileMonitor.endTraceFileMonitoring(" + jobID + "," + host + ") ~ here should timeout be impl.");
            this.monitorConsumerStop(host, mNc.getMid(), mNc.getChannel());
        }
        mappingStore.removeMapping(host, jobID);
        System.out.println(this.getTimeString() + " TF - TraceFileMonitor.endTraceFileMonitoring(" + jobID + "," + host + ") ~ END");
        return;
    }

    /**

   * Stops monitoring by calling MonitorConsumer.stop().

   * @param host Hostname.

   * @param mid Metric ID

   * @param channel Channel ID.

   * @return True on success.

   */
    private boolean monitorConsumerStop(Object host, int mid, int channel) {
        System.out.println("TF - TraceFileMonitor.monitorConsumerStop(" + host + ", " + mid + ", " + channel + ") ~ START");
        MonitorConsumer mc = this.getMonitorConsumer(this.getHostUrl("" + host));
        if (mc == null) {
            System.out.println("TF - TraceFileMonitor.monitorConsumerStop(" + host + ", " + mid + ", " + channel + ") mc null ~ END");
            return false;
        }
        if (!mc.isAlive()) {
            System.out.println("TF - TraceFileMonitor.monitorConsumerStop(" + host + ", " + mid + ", " + channel + ") [host,mid,channel]: isAlive false. ~ END");
            return false;
        }
        try {
            MonitorConsumer.CommandResult stopResult = null;
            stopResult = mc.stop(mid, channel);
            stopResult.waitResult();
            if (stopResult.getStatus() != 0) {
                System.out.println("TF - TraceFileMonitor.monitorConsumerStop(" + host + ", " + mid + ", " + channel + ") ~ mc.native -- STOP failed ~ END.");
                return false;
            } else {
                System.out.println("TF - TraceFileMonitor.monitorConsumerStop(" + host + ", " + mid + ", " + channel + ") ~ mc.native -- STOP successful ~ END.");
                return true;
            }
        } catch (Throwable t) {
            System.out.println("TF - TraceFileMonitor.monitorConsumerStop(" + host + ", " + mid + ", " + channel + ") ~ mc.native -- STOP failed, Thr:" + t.getMessage() + " ~ END");
            return false;
        }
    }

    /**

   * Gets a specified tracefile.

   * @param jobID Job's ID.

   * @param host Hostname.

   * @return

   */
    public TraceFile getTraceFile(Object jobID, Object host) {
        System.out.println("TF - TraceFileMonitor.getTraceFile(" + jobID + ", " + host + ") ~ START");
        host = this.getCleanHostname("" + host);
        System.out.println("TF - TraceFileMonitor.getTraceFile(" + jobID + ", " + host + ") ~ END");
        return this.tfStore.getTraceFile(new TraceFileId(host, jobID));
    }

    /**

   * This port function is to support testing at n26 connecting lm, otherwise mm is used and port is not needed.

   * @param host

   * @return

   */
    private String getPortWithColon(Object host) {
        String port = "";
        try {
            port = TFPropertyLoader.getInstance().getPortWithColon((String) host);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return port;
    }

    private String getHostUrl(String host) {
        String res = "monp://" + host + this.getPortWithColon(host);
        System.out.println("TF - TraceFileMonitor.getHostUrl(" + host + ") - returns:" + res);
        return res;
    }

    /**

   * Subscribes to a given job's trace.

   * @param mc

   * @param ml

   * @param jobId

   * @return

   */
    private boolean subscribe(Object host, Object jobId, TraceFileId tfId) {
        String hostURL = this.getHostUrl("" + host);
        System.out.println("TF - TraceFileMonitor.subscribe(" + hostURL + "," + jobId + ") ~ START");
        int mid;
        int channel;
        MonitorConsumer mc = this.getMonitorConsumer(hostURL);
        System.out.println("TF - TraceFileMonitor.subscribe(" + hostURL + "," + jobId + ") ~ mc = " + mc);
        if (mc == null) {
            System.out.println("TF - TraceFileMonitor.subscribe(" + hostURL + "," + jobId + ") ~ failed: mc == null!");
            return false;
        }
        try {
            System.out.println("TF - TraceFileMonitor.subscribe(" + hostURL + "," + jobId + ") ~ -8 : mc.isAlive: " + mc.isAlive());
            System.out.println("TF - TraceFileMonitor.subscribe(" + hostURL + "," + jobId + ") ~ -7");
            TraceFileMetricListener ml = new TraceFileMetricListener(tfId);
            System.out.println("TF - TraceFileMonitor.subscribe(" + hostURL + "," + jobId + ") ~ -6");
            mc.addMetricListener(ml);
            System.out.println("TF - TraceFileMonitor.subscribe(" + hostURL + "," + jobId + ") ~ -5");
            MonitorArg args[] = new MonitorArg[1];
            System.out.println("TF - TraceFileMonitor.subscribe(" + hostURL + "," + jobId + ") ~ -4");
            args[0] = new MonitorArg("jobid", jobId);
            System.out.println("TF - TraceFileMonitor.subscribe(" + hostURL + "," + jobId + ") ~ -3");
            MonitorConsumer.CollectResult cr = (MonitorConsumer.CollectResult) mc.collect("application.message", args);
            System.out.println("TF - TraceFileMonitor.subscribe(" + hostURL + "," + jobId + ") ~ -2");
            System.out.println("TF - TraceFileMonitor.subscribe(" + hostURL + "," + jobId + ") ~ -2.1 : mc.isAlive: " + mc.isAlive());
            cr.waitResult();
            System.out.println("TF - TraceFileMonitor.subscribe(" + hostURL + "," + jobId + ") ~ -1");
            if (cr.getStatus() != 0) {
                System.out.println("TF - TraceFileMonitor.subscribe(" + hostURL + "," + jobId + ") ~ mc.native -- COLLECT failed: " + cr.getStatusStr());
                return false;
            }
            System.out.println("TF - TraceFileMonitor.subscribe(" + hostURL + "," + jobId + ") ~ mc.native -- COLLECT successful: " + cr.getStatusStr());
            System.out.println("TF - TraceFileMonitor.subscribe(" + hostURL + "," + jobId + ") ~ 0");
            MonitorConsumer.MetricInstance subscribeMetric = cr.getMetricInstance();
            System.out.println("TF - TraceFileMonitor.subscribe(" + hostURL + "," + jobId + ") ~ 1");
            mid = subscribeMetric.getMetricId();
            System.out.println("TF - TraceFileMonitor.subscribe(" + hostURL + "," + jobId + ") ~ 2");
            channel = mc.getChannelId();
            System.out.println("TF - TraceFileMonitor.subscribe(" + hostURL + "," + jobId + ") ~ 3");
            MonitorConsumer.CommandResult subscribeResult = mc.subscribe(mid, channel);
            System.out.println("TF - TraceFileMonitor.subscribe(" + hostURL + "," + jobId + ") ~ 4");
            subscribeResult.waitResult();
            System.out.println("TF - TraceFileMonitor.subscribe(" + hostURL + "," + jobId + ") ~ 5");
            if (subscribeResult.getStatus() != 0) {
                System.out.println("TF - TraceFileMonitor.subscribe(" + hostURL + "," + jobId + ") [mid: " + mid + "] ~ mc.native -- subscribe failed: " + subscribeResult.getStatusStr());
                return false;
            }
            System.out.println("TF - TraceFileMonitor.subscribe(" + hostURL + "," + jobId + ") [mid: " + mid + "] ~ mc.native -- subscribe successful: " + subscribeResult.getStatusStr());
        } catch (MonitorException mex) {
            System.out.println("TF - TraceFileMonitor.subscribe(" + hostURL + "," + jobId + ") ~ mc.native -- subscribe failed, MonitorException: " + mex.getMessage());
            mex.printStackTrace();
            return false;
        } catch (TraceFileException ex) {
            System.out.println("TF - TraceFileMonitor.ssubscribe(" + hostURL + "," + jobId + ") ~ failed - TFException:" + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
        JobToMetricMappingStore mappingStore = JobToMetricMappingStore.getInstance();
        mappingStore.addMapping(host, jobId, mid, channel);
        return true;
    }

    /**

   * Creates a MonitorConsumer instance and performs authentication. On successful instantiation, puts instance to monitorConsumers hashtable.

   * @param host

   * @return

   * @throws java.lang.Throwable

   */
    private MonitorConsumer addAuthedMonitorConsumer(Object host) throws Throwable {
        MonitorConsumer mc = new MonitorConsumer(host.toString());
        if (mc == null) {
            return null;
        }
        MonitorConsumer.CommandResult ar;
        boolean isSSLOn = false;
        if (isSSLOn) {
            mc.wrap("tls");
            ar = mc.auth("transport");
        } else {
            ar = mc.auth();
        }
        ar.waitResult();
        if (ar.getStatus() != 0) {
            return null;
        }
        this.monitorConsumers.put(host, mc);
        System.out.println("TF - TraceFileMonitor.addAuthedMonitorConsumer(" + host + ") ~ mc.native -- AUTH successful");
        return mc;
    }

    /**

   * Returns an already stored MonitorConsumer instance.

   * @param host

   * @return null if

   */
    private MonitorConsumer getStoredMonitorConsumer(Object host) {
        return (MonitorConsumer) this.monitorConsumers.get(host);
    }

    /**

   * Returns an authenticated monitorConsumer for the host, which is alive.

   * @param host

   * @return null If creating such an instance fails.

   */
    private MonitorConsumer getMonitorConsumer(Object host) {
        MonitorConsumer tmp;
        try {
            tmp = (MonitorConsumer) this.getStoredMonitorConsumer(host);
            if (tmp == null) {
                tmp = this.addAuthedMonitorConsumer(host);
            }
            if (tmp != null) {
                if (!tmp.isAlive()) {
                    tmp = this.addAuthedMonitorConsumer(host);
                    if (tmp == null) {
                        System.out.println("TF - TraceFileMonitor.getMonitorConsumer(" + host + ") ~ failed, return null");
                        return null;
                    }
                }
            } else {
                System.out.println("TF - TraceFileMonitor.getMonitorConsumer(" + host + ") ~ failed, return null");
                return null;
            }
            System.out.println("TF - TraceFileMonitor.getMonitorConsumer(" + host + ") ~ successful");
            return tmp;
        } catch (Throwable t) {
            System.out.println("TF - TraceFileMonitor.getMonitorConsumer(" + host + ") ~ failed, Th: " + t.getMessage());
            return null;
        }
    }

    public static boolean isMonitoringOn() {
        try {
            TFPropertyLoader pl = TFPropertyLoader.getInstance();
            String res = pl.getProperty("is.monitoring.on");
            return res.equals("true") ? true : false;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
