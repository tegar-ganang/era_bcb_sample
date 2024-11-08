import java.io.*;
import java.util.*;
import hu.sztaki.lpds.monitor.*;
import hu.sztaki.lpds.monitor.tracefile.*;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 */
public class SubscribeTest implements Runnable {

    private String testAppAbsPath = null;

    private String traceFilesDir = null;

    private int jobNumber;

    private int finishedJobs;

    private String mainMonitorURL = null;

    private int maxMessageForAJob = 0;

    private Hashtable mappings = null;

    private int delay = 0;

    private boolean portalEmul = false;

    private TraceFileMonitorFacade tfm = null;

    public SubscribeTest(String testAppAbsPath, int jobNumber, int maxMessageForAJob, String mainMonitorURL, int delay, boolean portalEmul) {
        System.out.println("SubscribeTest(" + testAppAbsPath + ", " + jobNumber + ", " + maxMessageForAJob + ", " + mainMonitorURL + ", " + delay + ", " + portalEmul + ") called");
        String path = "" + this.getClass().getClassLoader().getResource(".");
        this.traceFilesDir = path.substring(path.indexOf(":") + 1, path.length()) + "testTraces/";
        File tracesDir = new File(this.traceFilesDir);
        if (!tracesDir.exists()) {
            try {
                tracesDir.mkdirs();
            } catch (Exception ex) {
                System.out.println("Failed to create dir: " + tracesDir.getAbsolutePath());
                ex.printStackTrace();
            }
        }
        this.testAppAbsPath = testAppAbsPath;
        this.jobNumber = jobNumber;
        this.finishedJobs = 0;
        this.maxMessageForAJob = maxMessageForAJob;
        this.mainMonitorURL = mainMonitorURL;
        this.mappings = new Hashtable();
        this.delay = delay;
        this.portalEmul = portalEmul;
        if (portalEmul) {
            try {
                this.tfm = TraceFileMonitor.getInstance(this.traceFilesDir);
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(0);
            }
        }
        this.run();
    }

    /**
   *
   * @param args
   */
    public static void main(String args[]) {
        if (!(args.length == 4 || args.length == 5 || args.length == 6)) {
            System.out.println("\n Description: Starts the given application {jobNumber} times at different hosts in the cluster and tries to collect \n" + "              its application.message metric via Mercury's Java interface. \n" + "              - The test application is started by ssh-ing into the given host of the cluster. \n" + "              - The hosts are selected from the output of the 'linuxall' command \n");
            System.out.println(" Usage: java SubscribeTest {test-app_abs-path} {job-number} {max-message-for-a-job} {main-monitor-url} [delay] [-p]");
            System.out.println("      (The [delay] parameter is optional; it means seconds. If not given, it defaults to 0.)");
            System.out.println("      (The [-p] flag is optional; emulates monitoring in the portal, i.e. using the same package. Using this flag, ensure that tracefile.jar is in the classpath.)");
            System.out.println("\n Examples:");
            System.out.println("    1) Without delay: > java SubscribeTest ~/test_app 10 200 monp://localhost:1234");
            System.out.println("    2)    With delay: > java SubscribeTest ~/test_app 10 200 monp://localhost:1234 1");
            System.out.println("    3) Emulating portal usage: Add '-p' as the last argument. \n");
            return;
        }
        if (args.length == 4 || args.length == 5 || args.length == 6) {
            File testApp = new File(args[0]);
            if (!testApp.isFile()) {
                System.out.println("\nTest application: " + testApp.getAbsolutePath() + " does not exists!\n");
                return;
            }
            int jobNumber;
            try {
                jobNumber = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                System.out.println("\n'job-number' has to be number!! ( '" + args[1] + "' is not a number ) \n");
                return;
            }
            int maxMessageForAJob;
            try {
                maxMessageForAJob = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                System.out.println("\n'max-message-for-a-job' has to be number!! ( '" + args[2] + "' is not a number ) \n");
                return;
            }
            try {
            } catch (Exception ex) {
                System.out.println("\nChecking monitor at: " + args[3] + " -- FAILED.");
                ex.printStackTrace();
                return;
            }
            int delay = 0;
            boolean portalEmul = false;
            if (args.length == 5 || args.length == 6) {
                if (!args[4].startsWith("-")) {
                    try {
                        delay = Integer.parseInt(args[4]);
                    } catch (NumberFormatException nfex) {
                        System.out.println("\n'delay' has to be number!! ( '" + args[4] + "' is not a number ) \n");
                        return;
                    }
                } else {
                    if (args[4].equals("-p")) portalEmul = true;
                }
            }
            if (args.length == 6) {
                if (args[5].equals("-p")) portalEmul = true;
            }
            SubscribeTest st = new SubscribeTest(args[0], jobNumber, maxMessageForAJob, args[3], delay, portalEmul);
        }
    }

    public void run() {
        System.out.println("Test STARTED..");
        System.out.println("Used machines: ");
        String[] machines = this.getMachines();
        if (machines.length == 0) {
            System.out.println("\nNo machine is available between nodes: n2-n27! ( Based upon 'linuxall')\n");
            return;
        }
        for (int i = 0; i < this.jobNumber; i++) {
            String hostname = machines[i % machines.length];
            String rInt = "" + Math.round(Math.random() * 100000);
            StringBuffer rIndex = new StringBuffer();
            for (int j = 0; j < rInt.length() - 5; j++) {
                rIndex.append("0");
            }
            rIndex.append(rInt);
            String jobID = "testJob-" + hostname + "-" + i + "_" + rIndex;
            int msgNumb = this.maxMessageForAJob;
            this.mappings.put(jobID, new Integer(msgNumb));
            this.startAppWithMonitoring(hostname, jobID, msgNumb);
        }
    }

    private boolean finishedAll = false;

    public void finished(String jobId) {
        System.out.println("\nAll trace (" + this.mappings.get(jobId) + "[+2] lines) collected for job with jobID: " + jobId);
        if (++this.finishedJobs == this.jobNumber) {
            this.finishedAll = true;
            System.out.println("\nTEST SUCCESSFUL :) All jobs' trace successfully collected.\n");
            System.exit(0);
        }
    }

    private class StarterThread extends Thread {

        private String hostname = null;

        private String jobId = null;

        private int maxMessageNumber;

        private SubscribeTest main = null;

        public StarterThread(String hostname, String jobId, int maxMessageNumber, SubscribeTest main) {
            this.hostname = hostname;
            this.jobId = jobId;
            this.maxMessageNumber = maxMessageNumber;
            this.main = main;
            this.start();
        }

        public void run() {
            try {
                Runtime.getRuntime().exec("ssh " + hostname + " " + main.testAppAbsPath + " -j " + jobId + " -p 1 -c " + maxMessageNumber + " -d " + main.delay);
                if (!main.portalEmul) {
                    TestClient tc = new TestClient(main.mainMonitorURL, this.jobId, this.maxMessageNumber, main);
                } else {
                    main.tfm.startTraceFileMonitoring(this.jobId, main.mainMonitorURL);
                    PortalEmulWatcher pew = new PortalEmulWatcher(this.main);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private class PortalEmulWatcher extends Thread {

        private SubscribeTest main = null;

        public PortalEmulWatcher(SubscribeTest main) {
            this.main = main;
            this.start();
        }

        public void run() {
            Enumeration keys;
            while (!this.main.finishedAll) {
                keys = this.main.mappings.keys();
                while (keys.hasMoreElements()) {
                    String jobId = "" + keys.nextElement();
                    TraceFile tf = this.main.tfm.getTraceFile(jobId, this.main.mainMonitorURL);
                    if (tf != null) {
                        if (tf.getLinesNumber() == (this.main.maxMessageForAJob + 2)) this.main.finished(jobId);
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    private void startAppWithMonitoring(String hostname, String jobId, int maxMessageNumber) {
        StarterThread st = new StarterThread(hostname, jobId, maxMessageNumber, this);
    }

    private String[] getMachines() {
        String[] linuxAll = this.execSingleLineOutputCmd("linuxall");
        if (linuxAll == null) {
            System.exit(0);
        }
        StringBuffer goodMachines = new StringBuffer();
        for (int i = 0; i < linuxAll.length; i++) {
            try {
                int nodeIndex;
                nodeIndex = Integer.parseInt(linuxAll[i].substring(1));
                if (nodeIndex >= 2 && nodeIndex <= 27) {
                    goodMachines.append(linuxAll[i] + " ");
                }
            } catch (Exception ex) {
            }
        }
        System.out.println(" " + goodMachines);
        return goodMachines.toString().split(" ");
    }

    private String[] execSingleLineOutputCmd(String cmdWithParams) {
        String result = "";
        try {
            Process p = Runtime.getRuntime().exec(cmdWithParams.split(" "));
            BufferedReader sin = new BufferedReader(new InputStreamReader(p.getInputStream()));
            result = sin.readLine();
            sin.close();
            return result.split(" ");
        } catch (Exception ex) {
            System.out.println("ERROR: " + ex.getMessage());
            return null;
        }
    }

    private class TestClient implements MetricListener {

        private MonitorConsumer.CommandResult queryResult;

        private File tf = null;

        private String monitorURL = null;

        private String jobId = null;

        private int metricId;

        private int maxMessageNumber;

        private int currentMessageNumber = 0;

        private SubscribeTest cbInterface = null;

        public TestClient(String monitorURL, String jobId, int maxMessageNumber, SubscribeTest cbInterface) {
            this.cbInterface = cbInterface;
            this.monitorURL = monitorURL;
            this.jobId = jobId;
            this.maxMessageNumber = maxMessageNumber;
            this.subscribe();
        }

        private void subscribe() {
            MetricListener ml = this;
            try {
                this.initFile(jobId);
                MonitorConsumer mc = new MonitorConsumer(this.monitorURL);
                mc.addMetricListener(ml);
                mc.auth();
                MonitorArg args[] = new MonitorArg[1];
                args[0] = new MonitorArg("jobid", jobId);
                MonitorConsumer.CollectResult cr = (MonitorConsumer.CollectResult) mc.collect("application.message", args);
                cr.waitResult();
                MonitorConsumer.MetricInstance suscribeMetric = cr.getMetricInstance();
                int mid = suscribeMetric.getMetricId();
                this.metricId = mid;
                int channel = mc.getChannelId();
                System.out.println("\n\tTestClient.subscibe(" + this.monitorURL + ", " + jobId + ") - mid:" + mid);
                MonitorConsumer.CommandResult sr = mc.subscribe(mid, channel);
                sr.waitResult();
                int status = sr.getStatus();
                if (status != 0) {
                    System.out.println("\n\tTestClient.subscribe(" + this.monitorURL + ", " + jobId + ") -Suscribe failed: " + sr.getStatusStr());
                    System.exit(1);
                }
                System.out.println("\n\tTestClient.subscibe(" + this.monitorURL + ", " + jobId + ") - Subscribe SUCCESSFUL. Waiting for data...");
                while (true) {
                    try {
                        java.lang.Thread.sleep(1000);
                    } catch (Exception e) {
                    }
                }
            } catch (MonitorException mex) {
                System.out.println("\n\tTestClient.subscribe(" + ml + "," + this.monitorURL + ", " + this.jobId + ") -- FAILED.");
                mex.printStackTrace();
            }
        }

        public boolean processMetric(int metricId, MetricValue value) {
            if (this.metricId != metricId) {
                return false;
            }
            this.writeToFile(value.getValue().toString());
            return true;
        }

        private void initFile(String fileName) {
            this.tf = new File(this.cbInterface.traceFilesDir + "/" + fileName + ".trace");
            try {
                if (this.tf.exists()) {
                    this.tf.renameTo(new File(tf.getName() + ".bak"));
                } else {
                    if (!this.tf.createNewFile()) {
                        System.out.println("\tTestClient.initFile(" + fileName + ") : tf.createNewFile(): FAILED!");
                        System.exit(0);
                        return;
                    }
                }
            } catch (Exception ex) {
                System.out.println("Error initfile: = " + this.tf.getAbsolutePath());
                ex.printStackTrace();
                System.exit(0);
            }
        }

        private synchronized void writeToFile(String line) {
            try {
                PrintWriter out = new PrintWriter(new FileOutputStream(this.tf, true));
                out.println(line);
                out.close();
            } catch (IOException ex) {
                System.out.println("\tTestClient.writeToFile(" + line + ") ~ FAILED, IOException: " + ex.getMessage());
                ex.printStackTrace(System.out);
                return;
            }
            System.out.print(".");
            if (this.currentMessageNumber++ == this.maxMessageNumber + 1) {
                this.cbInterface.finished(this.jobId);
            }
        }
    }
}
