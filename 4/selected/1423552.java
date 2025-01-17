package com.kni.etl.ketl.kernel;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import com.kni.etl.ETLJob;
import com.kni.etl.ETLJobManager;
import com.kni.etl.ETLJobManagerStatus;
import com.kni.etl.ETLJobStatus;
import com.kni.etl.EngineConstants;
import com.kni.etl.Metadata;
import com.kni.etl.dbutils.ResourcePool;
import com.kni.etl.scheduler.MetadataScheduler;
import com.kni.etl.util.XMLHelper;

public class KETLKernelImpl implements KETLKernel {

    private static int SleepTime = 500;

    public static boolean shutdown = false;

    public static boolean paused = false;

    /**
	 * Insert the method's description here. Creation date: (4/21/2002 8:40:01
	 * AM)
	 * 
	 * @return java.lang.String[]
	 * @param pArg
	 *            java.lang.String
	 */
    public static String extractArguments(String pArg, String pVarName) {
        String result = null;
        int argPos = -1;
        argPos = pArg.indexOf(pVarName);
        if (argPos != -1) {
            String fields = pArg.substring(pVarName.length());
            if (fields.length() > 0) {
                result = fields;
            }
        }
        return (result);
    }

    /**
	 * Insert the method's description here. Creation date: (4/21/2002 8:40:01
	 * AM)
	 * 
	 * @return java.lang.String[]
	 * @param pArg
	 *            java.lang.String
	 */
    public static String[] extractMultipleArguments(String pArg, String pVarName) {
        String[] result = null;
        int argPos = -1;
        argPos = pArg.indexOf(pVarName);
        if (argPos != -1) {
            String fields = pArg.substring(pVarName.length(), pArg.lastIndexOf(")"));
            if (fields.indexOf(',') != -1) {
                StringTokenizer st = new StringTokenizer(fields, ",");
                int nFields = st.countTokens();
                result = new String[nFields];
                int pos = 0;
                while (st.hasMoreTokens()) {
                    result[pos] = st.nextToken();
                    pos++;
                }
            } else if (fields.length() > 0) {
                result = new String[1];
                result[0] = fields;
            }
        }
        return (result);
    }

    private RandomAccessFile lckFile;

    private FileChannel channel;

    private FileLock exLck;

    private String displayVersionInfo() {
        EngineConstants.getVersion();
        return "KETL Server\n";
    }

    public void listCurrentThreads() {
        try {
            ThreadGroup currentGroup = Thread.currentThread().getThreadGroup();
            int numThreads = currentGroup.activeCount();
            Thread[] listOfThreads = new Thread[numThreads];
            currentGroup.enumerate(listOfThreads, true);
            for (int i = 0; i < numThreads; i++) {
                try {
                    ResourcePool.LogMessage(Thread.currentThread().getName(), ResourcePool.INFO_MESSAGE, "Thread #" + (i + 1) + " = " + listOfThreads[i].getName() + " of " + Thread.activeCount());
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
    }

    /**
	 * Starts the application.
	 * 
	 * @param args
	 *            an array of command-line arguments
	 */
    private static final String SERVER_LOCK = "ketlServer.lck";

    public boolean lockServerInstance() {
        try {
            if (lckFile == null) {
                lckFile = new RandomAccessFile(new File(SERVER_LOCK), "rw");
            }
            channel = lckFile.getChannel();
            exLck = channel.tryLock(1, 1, false);
            if (exLck != null) {
                return true;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
        ResourcePool.logError("A " + SERVER_LOCK + " file exists! A server maybe already running.");
        closeServerInstance(-1);
        return false;
    }

    public void closeServerInstance(int exitCode) {
        if (exLck != null) {
            try {
                exLck.release();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (channel != null) {
            try {
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (lckFile != null) {
            try {
                lckFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            lckFile = null;
        }
        if (exitCode != 0) System.exit(exitCode);
    }

    public void run(java.lang.String[] args) {
        fixOS();
        String[] mdUser = null;
        String mdServer = null;
        Vector<ETLJob> submittedJobs = new Vector<ETLJob>();
        Object[][] jobManagers = null;
        ETLJob baseJob;
        Thread.currentThread().setName("ETLDaemon");
        if (lockServerInstance() == false) ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.WARNING_MESSAGE, "A server lock could not be assigned, duplicate server instances can be started");
        try {
            baseJob = new ETLJob();
        } catch (Exception e2) {
            e2.printStackTrace();
            return;
        }
        Document serverXMLConfig = null;
        String appPath = null;
        for (int index = 0; index < args.length; index++) {
            if (args[index].indexOf("APP_PATH=") != -1) {
                appPath = extractArguments(args[index], "APP_PATH=");
                ResourcePool.LogMessage("Using KETL Path = " + appPath);
            }
        }
        for (int index = 0; index < args.length; index++) {
            if (args[index].indexOf("CONFIG=") != -1) {
                String filename = extractArguments(args[index], "CONFIG=");
                ResourcePool.LogMessage("Using config file " + filename + " to start server");
                serverXMLConfig = Metadata.LoadConfigFile(appPath, filename);
            }
            if ((mdUser == null) && (args[index].indexOf("MD_USER=(") != -1)) {
                mdUser = extractMultipleArguments(args[index], "MD_USER=(");
            }
            if ((mdServer == null) && (args[index].indexOf("SERVERNAME=") != -1)) {
                mdServer = extractArguments(args[index], "SERVERNAME=");
            }
        }
        if ((mdUser == null) & (mdServer == null) & (serverXMLConfig == null)) {
            ResourcePool.LogMessage("Using default config file to start server");
            serverXMLConfig = Metadata.LoadConfigFile(appPath, Metadata.CONFIG_FILE);
        }
        ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.INFO_MESSAGE, this.displayVersionInfo());
        String passphrase = null;
        if (serverXMLConfig != null) {
            Node serverNode = null;
            if (mdServer == null) {
                Node n = XMLHelper.findElementByName(serverXMLConfig, "SERVERS", null, null);
                if (n == null) {
                    ResourcePool.LogMessage("KETLServers.xml is missing the root node SERVERS, please review file");
                    closeServerInstance(-1);
                }
                String servername = XMLHelper.getAttributeAsString(n.getAttributes(), "DEFAULTSERVER", "localhost");
                serverNode = XMLHelper.findElementByName(serverXMLConfig, "SERVER", "NAME", servername);
                if (serverNode == null) {
                    serverNode = XMLHelper.findElementByName(serverXMLConfig, "SERVER", "NAME", "LOCALHOST");
                }
                if (serverNode == null) {
                    InetAddress thisIp;
                    try {
                        thisIp = InetAddress.getLocalHost();
                        mdServer = thisIp.getHostName();
                    } catch (UnknownHostException e) {
                        ResourcePool.LogMessage(Thread.currentThread().getName(), ResourcePool.ERROR_MESSAGE, "Problems getting localhost name " + e.getMessage());
                        closeServerInstance(com.kni.etl.EngineConstants.SERVER_NAME_ERROR_EXIT_CODE);
                    }
                }
            }
            if (serverNode == null) {
                serverNode = XMLHelper.findElementByName(serverXMLConfig, "SERVER", "NAME", mdServer);
            }
            if (serverNode == null) {
                ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.ERROR_MESSAGE, "Problems getting server name, check config file");
                closeServerInstance(com.kni.etl.EngineConstants.SERVER_NAME_ERROR_EXIT_CODE);
            }
            mdUser = new String[5];
            mdUser[0] = XMLHelper.getChildNodeValueAsString(serverNode, "USERNAME", null, null, null);
            mdUser[1] = XMLHelper.getChildNodeValueAsString(serverNode, "PASSWORD", null, null, null);
            mdUser[2] = XMLHelper.getChildNodeValueAsString(serverNode, "URL", null, null, null);
            mdUser[3] = XMLHelper.getChildNodeValueAsString(serverNode, "DRIVER", null, null, null);
            mdUser[4] = XMLHelper.getChildNodeValueAsString(serverNode, "MDPREFIX", null, null, "");
            passphrase = XMLHelper.getChildNodeValueAsString(serverNode, "PASSPHRASE", null, null, null);
        }
        if (mdUser == null) {
            ResourcePool.LogMessage("Wrong arguments:  [CONFIG=KETLServers.xml|MD_USER=(USER,PWD,JDBCURL,JDBCDriver,MDPrefix)] <SERVERNAME=KNI01>");
            ResourcePool.LogMessage("example:  MD_USER=(ETLUSER,ETLPWD,jdbc:oracle:oci8:@DEV3ORA,oracle.jdbc.driver.OracleDriver,QA) SERVERNAME=KNI01");
            closeServerInstance(com.kni.etl.EngineConstants.WRONG_ARGUMENT_EXIT_CODE);
        }
        MetadataScheduler md = null;
        String mdPrefix = null;
        if ((mdUser != null) && (mdUser.length == 5)) {
            mdPrefix = mdUser[4];
        }
        try {
            md = new MetadataScheduler(true, passphrase);
            md.setRepository(mdUser[0], mdUser[1], mdUser[2], mdUser[3], mdPrefix);
            ResourcePool.setMetadata(md);
        } catch (Exception e1) {
            ResourcePool.LogMessage(Thread.currentThread().getName(), ResourcePool.ERROR_MESSAGE, "Connecting to metadata - " + e1.getMessage());
            closeServerInstance(com.kni.etl.EngineConstants.METADATA_ERROR_EXIT_CODE);
        }
        if (mdServer == null) {
            try {
                InetAddress thisIp = InetAddress.getLocalHost();
                mdServer = thisIp.getHostName();
            } catch (UnknownHostException e) {
                ResourcePool.LogMessage(Thread.currentThread().getName(), ResourcePool.ERROR_MESSAGE, "Could not get system hostname please supply servername");
                return;
            }
        }
        int serverID;
        try {
            serverID = md.registerServer(mdServer);
            Object[][] serverExecutors = md.getServerExecutors(serverID);
            if (serverExecutors == null) {
                ResourcePool.LogMessage(Thread.currentThread().getName(), ResourcePool.ERROR_MESSAGE, "Server has no job executors specified");
                shutdown = true;
            } else {
                jobManagers = new Object[serverExecutors.length][2];
                for (int i = 0; i < serverExecutors.length; i++) {
                    jobManagers[i][0] = new ETLJobManager((String) serverExecutors[i][0], ((Integer) serverExecutors[i][1]).intValue(), ((Integer) serverExecutors[i][2]).intValue(), (String) serverExecutors[i][3], (String) serverExecutors[i][4]);
                    jobManagers[i][1] = md.getServerExecutorJobTypes((String) serverExecutors[i][0]);
                }
            }
            md.recoverServerJobs(serverID);
        } catch (Exception e) {
            ResourcePool.LogException(e, null);
            return;
        }
        boolean completeShutdown = false;
        long mdCheckStatusTimer = System.currentTimeMillis();
        while (completeShutdown == false) {
            try {
                ETLJob job = null;
                ETLJob[] submittedJobsToCheck = new ETLJob[submittedJobs.size()];
                submittedJobs.toArray(submittedJobsToCheck);
                for (int pos = 0; pos < submittedJobsToCheck.length; pos++) {
                    switch((submittedJobsToCheck[pos]).getStatus().getStatusCode()) {
                        case ETLJobStatus.PENDING_CLOSURE_FAILED:
                            (submittedJobsToCheck[pos]).writeLog();
                            if ((submittedJobsToCheck[pos]).getMaxRetries() > (submittedJobsToCheck[pos]).getRetryAttempts()) {
                                (submittedJobsToCheck[pos]).getStatus().setStatusCode(ETLJobStatus.WAITING_TO_BE_RETRIED);
                                md.setJobStatus(submittedJobsToCheck[pos]);
                            } else {
                                md.setJobStatus(submittedJobsToCheck[pos]);
                            }
                            removeJobFromSubmittedJobs(submittedJobs, submittedJobsToCheck[pos], md);
                            (submittedJobsToCheck[pos]).cleanup();
                            break;
                        case ETLJobStatus.PENDING_CLOSURE_CANCELLED:
                        case ETLJobStatus.PENDING_CLOSURE_SKIP:
                        case ETLJobStatus.PENDING_CLOSURE_SUCCESSFUL:
                            (submittedJobsToCheck[pos]).writeLog();
                            md.setJobStatus(submittedJobsToCheck[pos]);
                            removeJobFromSubmittedJobs(submittedJobs, submittedJobsToCheck[pos], md);
                            (submittedJobsToCheck[pos]).cleanup();
                            break;
                        case ETLJobStatus.CRITICAL_FAILURE_PAUSE_LOAD:
                            (submittedJobsToCheck[pos]).writeLog();
                            (submittedJobsToCheck[pos]).getStatus().setStatusCode(ETLJobStatus.PENDING_CLOSURE_FAILED);
                            md.setJobStatus(submittedJobsToCheck[pos]);
                            removeJobFromSubmittedJobs(submittedJobs, submittedJobsToCheck[pos], md);
                            (submittedJobsToCheck[pos]).cleanup();
                            md.pauseServer(mdServer, true);
                            paused = true;
                            break;
                        case ETLJobStatus.REJECTED:
                            (submittedJobsToCheck[pos]).getStatus().setStatusCode(ETLJobStatus.READY_TO_RUN);
                            job = submittedJobsToCheck[pos];
                            break;
                        case ETLJobStatus.EXECUTING:
                            long currentTime = System.currentTimeMillis();
                            if (currentTime > (mdCheckStatusTimer + 1000)) {
                                mdCheckStatusTimer = currentTime;
                                int xStatus = md.getJobStatusByExecutionId((submittedJobsToCheck[pos]).getJobExecutionID());
                                if (xStatus == ETLJobStatus.FATAL_STATE) {
                                    xStatus = ETLJobStatus.ATTEMPT_CANCEL;
                                }
                                if (xStatus == ETLJobStatus.ATTEMPT_CANCEL) {
                                    (submittedJobsToCheck[pos]).getStatus().setStatusCode(ETLJobStatus.ATTEMPT_CANCEL);
                                    if ((submittedJobsToCheck[pos]).isCancelled() == false) (submittedJobsToCheck[pos]).cancelJob();
                                    submittedJobsToCheck[pos].getStatus().statusChanged = true;
                                }
                            }
                        case ETLJobStatus.ATTEMPT_CANCEL:
                            if ((submittedJobsToCheck[pos]).getStatus().statusChanged) {
                                (submittedJobsToCheck[pos]).getStatus().statusChanged = false;
                                md.setJobStatus(submittedJobsToCheck[pos]);
                            } else if ((submittedJobsToCheck[pos]).getStatus().messageChanged) {
                                (submittedJobsToCheck[pos]).getStatus().messageChanged = false;
                                md.setJobMessage(submittedJobsToCheck[pos]);
                            }
                            break;
                        case ETLJobStatus.QUEUED_FOR_EXECUTION:
                            break;
                        default:
                            ResourcePool.LogMessage(Thread.currentThread().getName(), ResourcePool.ERROR_MESSAGE, "Job in unmanaged status please contact support: Status ID = " + submittedJobsToCheck[pos].getStatus().getStatusCode() + ", Message = " + (submittedJobsToCheck[pos]).getStatus().getStatusMessage());
                            break;
                    }
                }
                if (shutdown == false) {
                    paused = false;
                    switch(md.shutdownServer(serverID)) {
                        case com.kni.etl.ETLServerStatus.PAUSED:
                            paused = true;
                            break;
                        case com.kni.etl.ETLServerStatus.SERVER_ALIVE:
                            shutdown = false;
                            break;
                        case com.kni.etl.ETLServerStatus.SERVER_SHUTDOWN:
                            shutdown = true;
                            break;
                        case com.kni.etl.ETLServerStatus.SERVER_SHUTTING_DOWN:
                            shutdown = true;
                            break;
                        case com.kni.etl.ETLServerStatus.SERVER_KILLED:
                            ResourcePool.LogMessage("WARNING: Server shutdown down immediately, jobs will not be shutdown gracefully! Exceptions will be shown");
                            for (int pos = 0; pos < jobManagers.length; pos++) {
                                ((ETLJobManager) jobManagers[pos][0]).kill();
                            }
                            ResourcePool.LogMessage("Shutdown complete");
                            md.closeMetadata();
                            closeServerInstance(0);
                            break;
                    }
                    if (shutdown == true) {
                        ResourcePool.LogMessage(Thread.currentThread().getName(), ResourcePool.INFO_MESSAGE, "Waiting for any executing jobs to end before shutdown.");
                    }
                }
                if ((submittedJobsToCheck.length == 0) && (shutdown == true)) {
                    completeShutdown = true;
                }
                if ((shutdown == false) && (paused == false)) {
                    Set<String[]> jobTypesToRequest = new HashSet();
                    for (int pos = 0; pos < jobManagers.length; pos++) {
                        if (((ETLJobManager) jobManagers[pos][0]).getStatus().getStatusCode() == ETLJobManagerStatus.READY) {
                            jobTypesToRequest.add(new String[] { ((ETLJobManager) jobManagers[pos][0]).getJobType(), ((ETLJobManager) jobManagers[pos][0]).getPool() });
                        }
                    }
                    if (job == null) {
                        job = md.getNextJobInQueue(jobTypesToRequest, serverID);
                    }
                    if (job != null) {
                        ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.INFO_MESSAGE, "Finding available executor for " + job.getJobID() + "[" + job.getLoadID() + "]");
                        if (job.getClass().getName().compareTo(baseJob.getClass().getName()) == 0) {
                            job.getStatus().setStatusCode(ETLJobStatus.PENDING_CLOSURE_SUCCESSFUL);
                            submittedJobs.add(job);
                        } else {
                            if (this.submitJob(submittedJobs, jobManagers, job) == false) {
                                md.setJobStatus(job);
                            }
                        }
                    }
                }
                ResourcePool.releaseTimedOutConnections();
                try {
                    if (job == null) {
                        if (shutdown) {
                            SleepTime = 100;
                        } else if (SleepTime < 5000) {
                            SleepTime = SleepTime * 2;
                        }
                        Thread.sleep(SleepTime);
                    } else {
                        if (SleepTime > 0) {
                            if (SleepTime > 2000) {
                                SleepTime = 2000;
                            } else if (SleepTime < 10) {
                                SleepTime = 10;
                            } else {
                                SleepTime = SleepTime / 2;
                            }
                        }
                        Thread.sleep(SleepTime);
                    }
                } catch (InterruptedException e) {
                }
            } catch (SQLException e) {
                ResourcePool.LogException(e, null);
                md.closeMetadata();
            } catch (Exception e) {
                ResourcePool.LogException(e, null);
            }
        }
        if (jobManagers != null) {
            for (int pos = 0; pos < jobManagers.length; pos++) {
                ((ETLJobManager) jobManagers[pos][0]).shutdown();
            }
        }
        ResourcePool.LogMessage("Flushing caches to disk");
        ResourcePool.releaseAllLookups();
        ResourcePool.LogMessage("Shutdown complete");
        md.closeMetadata();
        closeServerInstance(0);
    }

    private void fixOS() {
        try {
            java.security.Security.removeProvider("SunPKCS11-Solaris");
        } catch (Exception e) {
            ResourcePool.logException(e);
        }
    }

    private void removeJobFromSubmittedJobs(Vector<ETLJob> submittedJobs, ETLJob job, Metadata md) throws SQLException, Exception {
        ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.INFO_MESSAGE, "Remove job from execuitng list " + job.getJobID() + ", final status - " + job.getStatus().getStatusMessage());
        int initStatus = job.getStatus().getStatusCode();
        submittedJobs.remove(job);
        int finalStatus = md.getJobStatusByExecutionId(job.getJobExecutionID());
        if (finalStatus != ETLJobStatus.FATAL_STATE && initStatus != finalStatus) {
            ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.ERROR_MESSAGE, "Status issue, expected " + initStatus + " found " + finalStatus);
        }
    }

    private boolean submitJob(Vector<ETLJob> submittedJobs, Object[][] jobManagers, ETLJob job) {
        job.getStatus().setStatusCode(ETLJobStatus.READY_TO_RUN);
        for (int pos = 0; pos < jobManagers.length; pos++) {
            if (((ETLJobManager) jobManagers[pos][0]).getStatus().getStatusCode() == ETLJobManagerStatus.READY) {
                if (((ETLJobManager) jobManagers[pos][0]).submitJob(job) == true) {
                    submittedJobs.add(job);
                    ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.INFO_MESSAGE, "Submitting job " + job.getJobID() + " for execution, type = " + ((ETLJobManager) jobManagers[pos][0]).getJobType() + ",  pool = " + ((ETLJobManager) jobManagers[pos][0]).getPool());
                    return true;
                } else {
                    job.getStatus().setStatusCode(ETLJobStatus.READY_TO_RUN);
                }
            }
        }
        job.getStatus().setStatusCode(ETLJobStatus.READY_TO_RUN);
        ResourcePool.LogMessage(Thread.currentThread(), ResourcePool.DEBUG_MESSAGE, "Job not submitted " + job.getJobID() + " no executors available, return to pending list");
        return false;
    }

    /**
	 * Insert the method's description here. Creation date: (6/3/2002 8:18:30
	 * PM)
	 */
    public final void shutdown() {
        shutdown = true;
    }
}
