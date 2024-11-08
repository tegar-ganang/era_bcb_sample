package hu.sztaki.lpds.submitter.service.valery;

import hu.sztaki.lpds.information.local.PropertyLoader;
import hu.sztaki.lpds.submitter.grids.confighandler.JobConfig;
import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.HashMap;
import hu.sztaki.lpds.submitter.com.JobRuntime;
import hu.sztaki.lpds.submitter.service.valery.util.ValeryFileUtil;
import hu.sztaki.lpds.submitter.status.StatusQueueManagerThread;
import hu.sztaki.lpds.submitter.status.calljobs.JobCallerHandler;
import java.util.Calendar;
import java.util.UUID;
import java.util.Vector;

/**
 * @author krisztian
 */
public class Base extends Thread {

    private static Base instance = null;

    private Hashtable statusCollection = new Hashtable();

    private long endedjob = 0;

    private int runninggc = 10;

    private int progressjob = 0;

    private boolean outputfilelog = true;

    private String tmpfilename0 = "";

    private Hashtable<RunnerManagerThread, Hashtable<String, Runner>> mg = new Hashtable<RunnerManagerThread, Hashtable<String, Runner>>();

    private Enumeration<RunnerManagerThread> mgIndex;

    private int maxpoolsize;

    private int count = 0;

    /** 
 *  Class constructor
 */
    public Base() {
    }

    public Hashtable<RunnerManagerThread, Hashtable<String, Runner>> getMgs() {
        return mg;
    }

    public void initBase() {
        try {
            System.out.println("---Submitter Base start---");
            outputfilelog = PropertyLoader.getInstance().getProperty("guse.subbmitter.valery.serviceloggdir") != null;
            System.out.println("---:" + outputfilelog + ":---");
            if (outputfilelog) {
                tmpfilename0 = PropertyLoader.getInstance().getProperty("prefix.dir") + PropertyLoader.getInstance().getProperty("guse.subbmitter.valery.serviceloggdir") + "/" + PropertyLoader.getInstance().getProperty("service.url");
                ValeryFileUtil.secureCreateDirs(tmpfilename0);
                tmpfilename0 = tmpfilename0 + "/submitter.service.logg." + System.nanoTime();
                System.out.println("INIT file log:" + tmpfilename0);
                ValeryFileUtil.secureCreateFile(tmpfilename0);
                ValeryFileUtil.secureWriteFile(tmpfilename0, "<submitter time=\"" + Calendar.getInstance().getTime() + "\" utc=\"" + System.currentTimeMillis() + "\">\n", true);
            }
            maxpoolsize = Integer.parseInt(PropertyLoader.getInstance().getProperty("guse.submitter.maxthreads"));
            new File(PropertyLoader.getInstance().getProperty("prefix.dir") + "/submitter/").mkdirs();
            runninggc = Integer.parseInt(PropertyLoader.getInstance().getProperty("gc.rt.system"));
            int workerthread = Integer.parseInt(PropertyLoader.getInstance().getProperty("guse.subbmitter.valery.workerthread"));
            for (int i = 0; i < workerthread; i++) {
                mg.put(new RunnerManagerThread("" + i), new Hashtable<String, Runner>());
            }
            mgIndex = mg.keys();
            JobCallerHandler.getI().startCallerThread();
            StatusQueueManagerThread.propertyTrigger();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** 
 *  Singleton kezelo
 *  @return Statikus osztalypeldany
 *  @see Base
 */
    public static Base getI() {
        if (instance == null) {
            instance = new Base();
        }
        return instance;
    }

    public int getMaxPoolSize() {
        return maxpoolsize;
    }

    public boolean getEnabledLogg() {
        return outputfilelog;
    }

    public Enumeration<Runner> getAllJob(RunnerManagerThread pID) {
        if (mg.get(pID) != null) {
            return mg.get(pID).elements();
        }
        return new Hashtable().elements();
    }

    public Vector<String> getUserJobs(String pPortalID, String pUserID) {
        Vector<String> res = new Vector<String>();
        Enumeration<RunnerManagerThread> enm = mg.keys();
        Enumeration<String> enm0;
        RunnerManagerThread jobPool;
        Runner job;
        while (enm.hasMoreElements()) {
            jobPool = enm.nextElement();
            enm0 = mg.get(jobPool).keys();
            while (enm0.hasMoreElements()) {
                job = mg.get(jobPool).get(enm0.nextElement());
                if (pPortalID.equals(job.getJobRuntime().getPortalID()) && pUserID.equals(job.getJobRuntime().getUserID())) res.add(job.getId());
            }
        }
        return res;
    }

    /**
 * Egy futo jobot felugyelo szal lekerdezes
 * @param pValue Job Runtime id
 * @return Job felugyelo thread
 */
    public Runner getRunner(String pValue) throws NullPointerException {
        Vector<String> res = new Vector<String>();
        Enumeration<RunnerManagerThread> enm = mg.keys();
        Enumeration<String> enm0;
        RunnerManagerThread jobPool;
        Runner job;
        while (enm.hasMoreElements()) {
            jobPool = enm.nextElement();
            enm0 = mg.get(jobPool).keys();
            while (enm0.hasMoreElements()) {
                job = mg.get(jobPool).get(enm0.nextElement());
                if (pValue.equals(job.getId())) return job;
            }
        }
        throw new NullPointerException("runnerID:" + pValue);
    }

    /**
 * Futo jobok szamanak lekerdezese
 * @return futo jobok szama
 */
    public int getRunnerSize() {
        int activateJobsNumber = 0;
        Enumeration enm = mg.elements();
        while (enm.hasMoreElements()) {
            activateJobsNumber += ((Hashtable) enm.nextElement()).size();
        }
        return activateJobsNumber + progressjob;
    }

    /**
 * Egy manager threadben futo jobok szamanak lekerdezese
 * @return futo jobok szama
 */
    public int getRunnerSize(RunnerManagerThread pValue) {
        return mg.get(pValue).size();
    }

    public int getFreeRunnerCount() {
        int activateJobsNumber = 0;
        Enumeration enm = mg.elements();
        while (enm.hasMoreElements()) {
            activateJobsNumber += ((Hashtable) enm.nextElement()).size();
        }
        return maxpoolsize - (activateJobsNumber + progressjob);
    }

    /**
 * Futo jobok szamanak novelese
 */
    public void incProgressJobs(int pValue) {
        progressjob += pValue;
    }

    /**
 * Futo jobok szamanak csokkentese
 */
    public void decProgressJobs() {
        progressjob--;
    }

    /** Elindit egy jobot
 *  @param pData    Jobleiro
 *  @param pJxml job leiro
 *  @return inditas sikeressege
 *  @see JobRuntime
 */
    public synchronized boolean runningJob(JobRuntime pData, long pWaiting, JobConfig pJc, String pJproxy) {
        if (!mgIndex.hasMoreElements()) {
            mgIndex = mg.keys();
        }
        RunnerManagerThread ttemp = mgIndex.nextElement();
        Hashtable<String, Runner> tlist = mg.get(ttemp);
        HashMap desc = new HashMap();
        try {
            String tmp = UUID.randomUUID().toString();
            while (tlist.get(tmp) != null) {
                tmp = UUID.randomUUID().toString();
            }
            tlist.put(tmp, new Runner(pData, desc, pWaiting, pJc, pJproxy, tmp));
            decProgressJobs();
            writeServiceLogg("jobsubmit thread=\"" + ttemp.getName() + "\" poolsize=\"" + getRunnerSize() + "\" freesize=\"" + getFreeRunnerCount() + "\" threadpool=\"" + tlist.size() + "\"", 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /** 
 *  Eltavolit egy Job-ot a nyilvantartasbol, es kitakarit utanna
 *  @param pData    Jobleiro
 *  @see JobRuntime
 */
    public synchronized void endJob(RunnerManagerThread pThread, String pJobId) {
        endedjob++;
        try {
            if ((mg != null) && (pThread != null)) {
                if (mg.get(pThread) != null) {
                    mg.get(pThread).remove(pJobId);
                    String log = "endjob state=\"pooling\" id=\"" + pJobId + "\" poolsize=\"" + getRunnerSize() + "\" freesize=\"" + getFreeRunnerCount() + "\" threadpool=\"" + mg.get(pThread).size() + "\"";
                    Base.getI().writeServiceLogg(log, 1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if ((endedjob % runninggc) == 0) {
            String log = "system state=\"rungc\"";
            Base.getI().writeServiceLogg(log, 1);
            System.gc();
        }
    }

    /** 
 *  Job megszakitasa
 *  @param pZenID workflow peldany azonosito
 *  @see JobRuntime 
 */
    public synchronized void abortJobs(String pZenID, long pSubmitID) {
        int abortJobCnt = 1;
        while (abortJobCnt > 0) {
            try {
                abortJobCnt = 0;
                Enumeration<RunnerManagerThread> enm0 = mg.keys();
                Enumeration<String> enm1;
                String key1;
                String zenID;
                long subID = 0;
                while (enm0.hasMoreElements()) {
                    RunnerManagerThread key0 = enm0.nextElement();
                    enm1 = mg.get(key0).keys();
                    while (enm1.hasMoreElements()) {
                        key1 = enm1.nextElement();
                        try {
                            zenID = mg.get(key0).get(key1).getJobRuntime().getWorkflowRuntimeID();
                            subID = mg.get(key0).get(key1).getJobRuntime().getWorkflowSubmitID();
                            if ((pZenID.equals(zenID)) && (pSubmitID == subID)) {
                                abortJobCnt++;
                                if (mg.get(key0).get(key1) != null) {
                                    try {
                                        if (mg.get(key0).get(key1) != null) {
                                            mg.get(key0).get(key1).abort();
                                        }
                                        if (mg.get(key0) != null) {
                                            mg.get(key0).remove(key1);
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /** 
 *  Kotegelt statuszok lekerdezese
 *  @return Kotegelt status leirok
 *  @see Hashtable
 */
    public Hashtable getStatusCollectionData() {
        return statusCollection;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        if (outputfilelog) {
            ValeryFileUtil.secureWriteFile(tmpfilename0, "</submitter time=\"" + Calendar.getInstance().getTime() + "\" utc=\"" + System.currentTimeMillis() + "\">\n", true);
        }
        System.out.println("---Submitter Finalize---");
    }

    public void writeServiceLogg(String plogg, int t) {
        if (outputfilelog) {
            String s = "";
            long memory = 0;
            memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            synchronized (this) {
                count++;
                if (count > 10000) {
                    count = 0;
                    ValeryFileUtil.secureWriteFile(tmpfilename0, "</submitter time=\"" + Calendar.getInstance().getTime() + "\" utc=\"" + System.currentTimeMillis() + "\">\n", true);
                    tmpfilename0 = PropertyLoader.getInstance().getProperty("prefix.dir") + PropertyLoader.getInstance().getProperty("guse.subbmitter.valery.serviceloggdir");
                    ValeryFileUtil.secureCreateDirs(tmpfilename0);
                    tmpfilename0 = tmpfilename0 + "/submitter.service.logg." + System.nanoTime();
                    ValeryFileUtil.secureCreateFile(tmpfilename0);
                    ValeryFileUtil.secureWriteFile(tmpfilename0, "<submitter time=\"" + Calendar.getInstance().getTime() + "\" utc=\"" + System.currentTimeMillis() + "\">\n", true);
                }
                for (int i = 0; i < t; i++) s = s + "\t";
                ValeryFileUtil.secureWriteFile(tmpfilename0, s + "<" + plogg + "  time=\"" + Calendar.getInstance().getTime() + "\" utc=\"" + System.currentTimeMillis() + "\" usememory=\"" + memory + "\"/>\n", true);
            }
        }
    }
}
