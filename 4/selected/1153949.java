package com.knowgate.scheduler;

import java.lang.Thread;
import java.util.Properties;
import java.util.LinkedList;
import java.util.ListIterator;
import java.sql.SQLException;
import java.sql.Connection;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import javax.mail.MessagingException;
import com.knowgate.jdc.JDCConnection;
import com.knowgate.dataobjs.DB;
import com.knowgate.dataxslt.*;
import com.knowgate.dataxslt.db.PageSetDB;
import com.knowgate.debug.DebugFile;
import com.knowgate.scheduler.*;
import com.knowgate.crm.DistributionList;

/**
 * <p>Scheduled Job Worker Thread</p>
 * @author Sergio Montoro Ten
 * @version 1.0
 */
public class WorkerThread extends Thread {

    private String sLastError;

    private Job oJob;

    private Atom oAtm;

    private int delay = 1;

    private AtomConsumer oConsumer;

    private WorkerThreadPool oPool;

    private LinkedList oCallbacks;

    private int iCallbacks;

    private boolean bContinue;

    /**
   * Create WorkerThread
   * @param oThreadPool
   * @param oAtomConsumer
   */
    public WorkerThread(WorkerThreadPool oThreadPool, AtomConsumer oAtomConsumer) {
        oConsumer = oAtomConsumer;
        oPool = oThreadPool;
        oCallbacks = new LinkedList();
        iCallbacks = 0;
        oJob = null;
        sLastError = "";
    }

    public void setConsumer(AtomConsumer oAtomConsumer) {
        oConsumer = oAtomConsumer;
    }

    /**
   * Get Environment property from hipergate.cnf
   * @param sKey Property Name
   * @return Property Value or <b>null</b> if not found
   */
    public String getProperty(String sKey) {
        return oPool.getProperty(sKey);
    }

    public Atom activeAtom() {
        return oAtm;
    }

    public Job activeJob() {
        return oJob;
    }

    public String lastError() {
        return sLastError;
    }

    /**
   * Register a thread callback object
   * @param oNewCallback WorkerThreadCallback subclass instance
   * @throws IllegalArgumentException If a callback with same name has oNewCallback was already registered
   */
    public void registerCallback(WorkerThreadCallback oNewCallback) throws IllegalArgumentException {
        WorkerThreadCallback oCallback;
        ListIterator oIter = oCallbacks.listIterator();
        while (oIter.hasNext()) {
            oCallback = (WorkerThreadCallback) oIter.next();
            if (oCallback.name().equals(oNewCallback.name())) {
                throw new IllegalArgumentException("Callback " + oNewCallback.name() + " is already registered");
            }
        }
        oCallbacks.addLast(oNewCallback);
        iCallbacks++;
    }

    /**
   * Unregister a thread callback object
   * @param sCallbackName Name of callback to be unregistered
   * @return <b>true</b> if a callback with such name was found and unregistered,
   * <b>false</b> otherwise
   */
    public boolean unregisterCallback(String sCallbackName) {
        WorkerThreadCallback oCallback;
        ListIterator oIter = oCallbacks.listIterator();
        while (oIter.hasNext()) {
            oCallback = (WorkerThreadCallback) oIter.next();
            if (oCallback.name().equals(sCallbackName)) {
                oIter.remove();
                iCallbacks--;
                return true;
            }
        }
        return false;
    }

    private void callBack(int iOpCode, String sMessage, Exception oXcpt, Object oParam) {
        WorkerThreadCallback oCallback;
        ListIterator oIter = oCallbacks.listIterator();
        while (oIter.hasNext()) {
            oCallback = (WorkerThreadCallback) oIter.next();
            oCallback.call(getName(), iOpCode, sMessage, oXcpt, oParam);
        }
    }

    /**
   * <p>Process atoms obtained throught AtomConsumer</p>
   * Each worker WorkerThread will enter an endless loop until the queue is empty
   * or an interrupt signal is received.<br>
   * If an exception is thrown while creating of processing atoms the workerthread
   * will be aborted.
   */
    public void run() {
        String sJob = "";
        JDCConnection oConsumerConnection;
        if (DebugFile.trace) {
            DebugFile.writeln("Begin WorkerThread.run()");
            DebugFile.incIdent();
            DebugFile.writeln("thread=" + getName());
        }
        bContinue = true;
        sLastError = "";
        while (bContinue) {
            try {
                sleep(delay);
                if (DebugFile.trace) DebugFile.writeln(getName() + " getting next atom...");
                oAtm = oConsumer.next();
                if (oAtm == null) {
                    if (DebugFile.trace) DebugFile.writeln(getName() + " no more atoms.");
                    if (iCallbacks > 0) callBack(WorkerThreadCallback.WT_ATOMCONSUMER_NOMORE, "Thread " + getName() + " no more Atoms", null, oConsumer);
                    break;
                }
                if (iCallbacks > 0) callBack(WorkerThreadCallback.WT_ATOM_GET, "Thread " + getName() + " got Atom " + String.valueOf(oAtm.getInt(DB.pg_atom)), null, oAtm);
                oConsumerConnection = oConsumer.getConnection();
                if (DebugFile.trace) DebugFile.writeln(getName() + " AtomConsumer.getConnection() : " + (oConsumerConnection != null ? "[Conenction]" : "null"));
                if (!sJob.equals(oAtm.getString(DB.gu_job))) {
                    sJob = oAtm.getString(DB.gu_job);
                    try {
                        oJob = Job.instantiate(oConsumerConnection, sJob, oPool.getProperties());
                        if (iCallbacks > 0) callBack(WorkerThreadCallback.WT_JOB_INSTANTIATE, "instantiate job " + sJob + " command " + oJob.getString(DB.id_command), null, oJob);
                    } catch (ClassNotFoundException e) {
                        sJob = "";
                        oJob = null;
                        sLastError = "Job.instantiate(" + sJob + ") ClassNotFoundException " + e.getMessage();
                        if (DebugFile.trace) DebugFile.writeln(getName() + " " + sLastError);
                        if (iCallbacks > 0) callBack(-1, sLastError, e, null);
                        bContinue = false;
                    } catch (IllegalAccessException e) {
                        sJob = "";
                        oJob = null;
                        sLastError = "Job.instantiate(" + sJob + ") IllegalAccessException " + e.getMessage();
                        if (DebugFile.trace) DebugFile.writeln(getName() + " " + sLastError);
                        if (iCallbacks > 0) callBack(-1, sLastError, e, null);
                        bContinue = false;
                    } catch (InstantiationException e) {
                        sJob = "";
                        oJob = null;
                        sLastError = "Job.instantiate(" + sJob + ") InstantiationException " + e.getMessage();
                        if (DebugFile.trace) DebugFile.writeln(getName() + " " + sLastError);
                        if (iCallbacks > 0) callBack(-1, sLastError, e, null);
                        bContinue = false;
                    } catch (SQLException e) {
                        sJob = "";
                        oJob = null;
                        sLastError = " Job.instantiate(" + sJob + ") SQLException " + e.getMessage();
                        if (DebugFile.trace) DebugFile.writeln(getName() + " " + sLastError);
                        if (iCallbacks > 0) callBack(-1, sLastError, e, null);
                        bContinue = false;
                    }
                }
                if (null != oJob) {
                    oJob.process(oAtm);
                    if (DebugFile.trace) DebugFile.writeln("Thread " + getName() + " consumed Atom " + String.valueOf(oAtm.getInt(DB.pg_atom)));
                    oAtm.archive(oConsumerConnection);
                    if (iCallbacks > 0) callBack(WorkerThreadCallback.WT_ATOM_CONSUME, "Thread " + getName() + " consumed Atom " + String.valueOf(oAtm.getInt(DB.pg_atom)), null, oAtm);
                    oAtm = null;
                    if (DebugFile.trace) DebugFile.writeln("job " + oJob.getString(DB.gu_job) + " pending " + String.valueOf(oJob.pending()));
                    if (oJob.pending() == 0) {
                        oJob.setStatus(oConsumerConnection, Job.STATUS_FINISHED);
                        if (iCallbacks > 0) callBack(WorkerThreadCallback.WT_JOB_FINISH, "finish", null, oJob);
                    }
                } else {
                    oAtm = null;
                    sLastError = "Job.instantiate(" + sJob + ") returned null";
                    if (DebugFile.trace) DebugFile.writeln("ERROR: " + sLastError);
                    if (iCallbacks > 0) callBack(-1, sLastError, new NullPointerException("Job.instantiate(" + sJob + ")"), null);
                    bContinue = false;
                }
                oConsumerConnection = null;
            } catch (FileNotFoundException e) {
                if (DebugFile.trace) DebugFile.writeln(getName() + " FileNotFoundException " + e.getMessage());
                if (null != oJob) {
                    sLastError = "FileNotFoundException, job " + oJob.getString(DB.gu_job) + " ";
                    if (null != oAtm) sLastError += "atom " + String.valueOf(oAtm.getInt(DB.pg_atom)) + " ";
                    sLastError += e.getMessage();
                    oJob.log(getName() + " FileNotFoundException, job " + oJob.getString(DB.gu_job) + " ");
                    if (null != oAtm) oJob.log("atom " + String.valueOf(oAtm.getInt(DB.pg_atom)) + " ");
                    oJob.log(e.getMessage() + "\n");
                } else sLastError = "FileNotFoundException " + e.getMessage();
                if (iCallbacks > 0) callBack(-1, sLastError, e, oJob);
                bContinue = false;
            } catch (IOException e) {
                if (DebugFile.trace) DebugFile.writeln(getName() + " IOException " + e.getMessage());
                if (null != oJob) {
                    sLastError = "IOException, job " + oJob.getString(DB.gu_job) + " " + e.getMessage();
                    if (null != oAtm) sLastError += "atom " + String.valueOf(oAtm.getInt(DB.pg_atom)) + " ";
                    sLastError += e.getMessage();
                    oJob.log(getName() + " IOException, job " + oJob.getString(DB.gu_job) + " ");
                    if (null != oAtm) oJob.log("atom " + String.valueOf(oAtm.getInt(DB.pg_atom)) + " ");
                    oJob.log(e.getMessage() + "\n");
                } else sLastError = "IOException " + e.getMessage();
                if (iCallbacks > 0) callBack(-1, sLastError, e, oJob);
                bContinue = false;
            } catch (SQLException e) {
                if (DebugFile.trace) DebugFile.writeln(getName() + " SQLException " + e.getMessage());
                if (null != oJob) {
                    sLastError = "SQLException, job " + oJob.getString(DB.gu_job) + " ";
                    if (null != oAtm) sLastError += "atom " + String.valueOf(oAtm.getInt(DB.pg_atom)) + " ";
                    sLastError += e.getMessage();
                    oJob.log(getName() + " SQLException, job " + oJob.getString(DB.gu_job) + " ");
                    if (null != oAtm) oJob.log("atom " + String.valueOf(oAtm.getInt(DB.pg_atom)) + " ");
                    oJob.log(e.getMessage() + "\n");
                } else sLastError = "SQLException " + e.getMessage();
                if (iCallbacks > 0) callBack(-1, sLastError, e, oJob);
                bContinue = false;
            } catch (MessagingException e) {
                if (DebugFile.trace) DebugFile.writeln(getName() + " MessagingException " + e.getMessage());
                if (null != oJob) {
                    sLastError = "MessagingException, job " + oJob.getString(DB.gu_job) + " ";
                    if (null != oAtm) sLastError += "atom " + String.valueOf(oAtm.getInt(DB.pg_atom)) + " ";
                    sLastError += e.getMessage();
                    oJob.log(getName() + " MessagingException, job " + oJob.getString(DB.gu_job) + " ");
                    if (null != oAtm) oJob.log("atom " + String.valueOf(oAtm.getInt(DB.pg_atom)) + " ");
                    oJob.log(e.getMessage() + "\n");
                } else sLastError = "MessagingException " + e.getMessage();
                if (iCallbacks > 0) callBack(-1, sLastError, e, oJob);
                bContinue = false;
            } catch (InterruptedException e) {
                if (DebugFile.trace) DebugFile.writeln(getName() + " InterruptedException " + e.getMessage());
                if (null != oJob) {
                    sLastError = "InterruptedException, job " + oJob.getString(DB.gu_job) + " ";
                    if (null != oAtm) sLastError += "atom " + String.valueOf(oAtm.getInt(DB.pg_atom)) + " ";
                    sLastError += e.getMessage();
                    oJob.log(getName() + " InterruptedException, job " + oJob.getString(DB.gu_job) + " ");
                    if (null != oAtm) oJob.log("atom " + String.valueOf(oAtm.getInt(DB.pg_atom)) + " ");
                    oJob.log(e.getMessage() + "\n");
                } else sLastError = "InterruptedException " + e.getMessage();
                if (iCallbacks > 0) callBack(-1, sLastError, e, oJob);
                bContinue = false;
            } finally {
                sJob = "";
                oJob = null;
                oAtm = null;
            }
        }
        if (DebugFile.trace) {
            DebugFile.decIdent();
            DebugFile.writeln("End WorkerThread.run()");
        }
    }

    /**
   * <p>Halt thread execution commiting all operations in course before stopping</p>
   * If a thread is dead-locked by any reason halting it will not cause any effect.<br>
   * halt() method only sends a signals to the each WokerThread telling it that must
   * finish pending operations and stop.
   */
    public void halt() {
        bContinue = false;
    }
}
