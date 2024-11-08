package co.edu.unal.ungrid.client.worker;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.rmi.RemoteException;
import javax.swing.Timer;
import net.jini.core.entry.UnusableEntryException;
import net.jini.core.lease.Lease;
import net.jini.core.transaction.Transaction;
import net.jini.core.transaction.TransactionException;
import net.jini.lease.LeaseRenewalManager;
import co.edu.unal.space.util.SpaceProxy;
import co.edu.unal.ungrid.client.controller.App;
import co.edu.unal.ungrid.client.controller.GroupManager;

public class WorkerThread extends Thread {

    public WorkerThread(int id, final SpaceProxy proxy) {
        assert 0 <= id;
        m_id = id;
        m_proxy = proxy;
        m_leaseRenewalMgr = new LeaseRenewalManager();
        m_taskTmpl = new SpaceTask();
        resetTask();
        setDaemon(true);
    }

    private void resetTask() {
        m_task = null;
        m_nTaskLease = DEF_TSK_LEASE;
    }

    public synchronized void doStop() {
        m_bStop = true;
    }

    public synchronized boolean stopped() {
        return m_bStop;
    }

    public synchronized void setLeaseTime(int leaseTime) {
        m_nTaskLease = leaseTime;
    }

    protected void registerSuspendEventListener(final SpaceTask st) {
        try {
            SuspendEventListener sel = new SuspendEventListener(st);
            m_proxy.notify(new SuspendEntry(st.uuid), null, sel.getRemoteEventListener(), m_nTaskLease, null);
        } catch (Exception exc) {
            System.out.println("WorkerThread::registerSuspendEventListener(): exc=" + exc);
        }
    }

    protected void registerResumeEventListener(final SpaceTask st) {
        try {
            ResumeEventListener rel = new ResumeEventListener(st);
            m_proxy.notify(new ResumeEntry(st.uuid), null, rel.getRemoteEventListener(), m_nTaskLease, null);
        } catch (Exception exc) {
            System.out.println("WorkerThread::registerResumeEventListener(): exc=" + exc);
        }
    }

    protected void registerCancelEventListener(final SpaceTask st) {
        try {
            CancelEventListener cel = new CancelEventListener(st);
            m_proxy.notify(new CancelEntry(st.uuid), null, cel.getRemoteEventListener(), m_nTaskLease, null);
        } catch (Exception exc) {
            System.out.println("WorkerThread::registerResumeEventListener(): exc=" + exc);
        }
    }

    protected void registerEventListeners(final SpaceTask st) {
        if (st.isNotifiable()) {
            registerSuspendEventListener(st);
            registerResumeEventListener(st);
            registerCancelEventListener(st);
        }
    }

    protected SpaceTask takeTask() throws RemoteException, TransactionException, UnusableEntryException, InterruptedException {
        GroupManager.getInstance().updateStatus(AbstractTask.IDLE);
        final SpaceTask st = (SpaceTask) m_proxy.take(m_taskTmpl, m_txn, Lease.FOREVER);
        if (st != null) {
            if (DEBUG) System.out.println("WorkerThread::takeTask(): task=" + st);
            m_task = st;
            m_task.setStatus(AbstractTask.IDLE);
            setPriority(m_task.getPriority());
        }
        return st;
    }

    protected SpaceResult execute(final SpaceTask st) {
        SpaceResult sr = null;
        if (st != null) {
            int nTskPriority = st.getPriority();
            if (Thread.MIN_PRIORITY <= nTskPriority && nTskPriority <= Thread.MAX_PRIORITY) {
                setPriority(nTskPriority);
            }
            GroupManager.getInstance().updateStatus(AbstractTask.RUNNING);
            if (m_bLogging) log("WorkerThread::execute(): task=" + st);
            long started = System.currentTimeMillis();
            App.getInstance().taskStarted(m_id, st, started);
            Serializable result = st.execute(m_proxy);
            long finished = System.currentTimeMillis();
            GroupManager.getInstance().updateStatus(st.getStatus());
            App.getInstance().taskFinished(m_id, st, finished);
            resetTask();
            if (result != null) {
                if (m_bLogging) log("WorkerThread::execute(): result=" + result);
                sr = new SpaceResult(st.uuid, st.department, st.getPriority(), st.name, st.index, new Long(started), new Long(finished), result);
            }
            setPriority(Thread.MIN_PRIORITY);
        } else {
            if (DEBUG) System.err.println("WorkerThread::execute(): null task!");
        }
        return sr;
    }

    protected void writeResult(final SpaceResult sr) throws RemoteException, TransactionException {
        if (sr != null) {
            if (DEBUG) System.err.println("WorkerThread::writeResult(): writing " + sr.index + " ...");
            m_proxy.write(sr, m_txn, DEF_RES_LEASE);
        }
    }

    protected void createTransaction() {
        if (m_bUseTxn) {
            try {
                Transaction.Created c = m_proxy.getTransaction(m_nTaskLease);
                m_txn = c.transaction;
                m_lease = c.lease;
            } catch (Exception exc) {
                System.err.println("WorkerThread::createTransaction(): exc=" + exc);
            }
        }
    }

    protected boolean validTransaction() {
        return (m_bUseTxn ? m_txn != null && m_lease != null : true);
    }

    protected void releaseTransaction() {
        if (m_bUseTxn) {
            m_txn = null;
            m_lease = null;
        }
    }

    protected void renewLease() {
        if (m_lease != null) {
            m_leaseRenewalMgr.renewFor(m_lease, m_nTaskLease, null);
            if (DEBUG) {
                System.out.println("WorkerThread::renewFor(): transaction renewed for " + (m_nTaskLease / 1000) + " seconds");
            }
        }
    }

    protected void startLeaseTimer() {
        if (m_bUseTxn) {
            if (m_timer == null) {
                m_timer = new Timer(m_nTaskLease - MINUTE, new ActionListener() {

                    public void actionPerformed(ActionEvent ae) {
                        renewLease();
                    }
                });
            }
            if (m_timer != null) {
                m_timer.start();
            }
        }
    }

    protected void stopLeaseTimer() {
        if (m_timer != null) {
            m_timer.stop();
            m_timer = null;
        }
    }

    protected void commit() {
        if (m_txn != null) {
            try {
                m_txn.commit();
                if (DEBUG) System.out.println("WorkerThread::commit(): transaction commited ok.");
            } catch (Exception exc) {
                System.out.println("WorkerThread::commit(): exc=" + exc);
            } finally {
                stopLeaseTimer();
            }
        }
    }

    protected void rollback() {
        if (m_txn != null) {
            try {
                m_txn.abort();
                if (DEBUG) System.out.println("WorkerThread::rollback(): transaction rolled back.");
            } catch (Exception exc) {
                System.out.println("WorkerThread::rollback(): exc=" + exc);
            } finally {
                stopLeaseTimer();
            }
        }
    }

    public int getTaskLength() {
        return (m_task != null ? m_task.getTaskLength() : AbstractTask.UNKNOWN);
    }

    public int getTaskProgress() {
        return (m_task != null ? m_task.getTaskProgress() : AbstractTask.UNKNOWN);
    }

    public int getTaskTimerTo() {
        return (m_task != null ? m_task.getTaskTimerTo() : AbstractTask.ONE_SECOND);
    }

    public void run() {
        while (!stopped()) {
            createTransaction();
            if (validTransaction()) {
                startLeaseTimer();
                try {
                    SpaceTask st = takeTask();
                    writeResult(execute(st));
                    commit();
                } catch (Exception exc) {
                    rollback();
                    App.getInstance().warn("WorkerThread::run(): exc=" + exc);
                } finally {
                    releaseTransaction();
                }
            }
        }
        m_proxy.clear();
    }

    private void log(String msg) {
        App.getInstance().log(msg);
    }

    public void setLogging(boolean log) {
        m_bLogging = log;
    }

    private int m_id;

    private SpaceProxy m_proxy;

    private Transaction m_txn;

    private Lease m_lease;

    private Timer m_timer;

    private LeaseRenewalManager m_leaseRenewalMgr;

    private SpaceTask m_task;

    private SpaceTask m_taskTmpl;

    private int m_nTaskLease;

    private boolean m_bLogging;

    private boolean m_bUseTxn;

    private boolean m_bStop;

    public static final int SECOND = 1000;

    public static final int MINUTE = 60 * SECOND;

    public static final int HOUR = 60 * MINUTE;

    public static final int DEF_TSK_LEASE = 2 * MINUTE;

    public static final long DEF_RES_LEASE = Lease.FOREVER;

    private static final boolean DEBUG = !true;
}
