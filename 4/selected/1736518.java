package hu.sztaki.lpds.submitter.grids;

import hu.sztaki.lpds.dcibridge.service.Base;
import hu.sztaki.lpds.dcibridge.service.Job;
import hu.sztaki.lpds.dcibridge.service.LB;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.ggf.schemas.bes._2006._08.bes_factory.ActivityStateEnumeration;

/**
 * @author krisztian karoczkai
 */
public abstract class Middleware extends Thread {

    public static final byte SUBMIT = 0;

    public static final byte GETSTATUS = 1;

    public static final byte ABORT = 10;

    public static final byte UPLOAD = 3;

    public static final long LAST_ACTIVATE_TIMESTAMP = 30000;

    protected static int threadID = 0;

    protected String THIS_MIDDLEWARE = Base.MIDDLEWARE_SYSTEM;

    protected boolean state = true;

    protected BlockingQueue<Job> jobs = new LinkedBlockingQueue<Job>();

    /**
 * Constructor
 */
    public Middleware() {
        setName("guse/dci-bridge:Middleware handler(Base)");
    }

    public Collection getJobs() {
        return jobs;
    }

    /**
 * Updating the configuaration of the given middleware, updating reachable data based on the configuration
 * @throws exceptions refering Infrastructure configuration errors
 */
    public void setConfiguration() throws Exception {
    }

    /**
 * May the thread receive new job instances in order to submit them 
 * @return true= yes
 */
    public boolean isState() {
        return state;
    }

    /**
 * Set the thread to receive new job instances on order to submit them 
 * @param p true= ready to receive new jobs, false= the thread is closed for new jobs. However the existing job instances will be elaborated
 */
    public void setState(boolean p) {
        state = p;
    }

    public void addJob(Job pJob) throws Exception {
        jobs.put(pJob);
    }

    protected abstract void abort(Job pJob) throws Exception;

    protected abstract void submit(Job pJob) throws Exception;

    protected abstract void getOutputs(Job pJob) throws Exception;

    protected abstract void getStatus(Job pJob) throws Exception;

    @Override
    public void run() {
        Base.writeLogg(THIS_MIDDLEWARE, new LB("starting thread"));
        Job tmp = null;
        while (true) {
            try {
                tmp = jobs.take();
                switch(tmp.getFlag()) {
                    case SUBMIT:
                        Base.initLogg(tmp.getId(), "logg.job.submit");
                        submit(tmp);
                        Base.endJobLogg(tmp, LB.INFO, "");
                        tmp.setFlag(GETSTATUS);
                        tmp.setTimestamp(System.currentTimeMillis());
                        tmp.setPubStatus(ActivityStateEnumeration.RUNNING);
                        break;
                    case GETSTATUS:
                        if ((System.currentTimeMillis() - tmp.getTimestamp()) < LAST_ACTIVATE_TIMESTAMP) {
                            try {
                                sleep(LAST_ACTIVATE_TIMESTAMP);
                            } catch (InterruptedException ei) {
                                Base.writeLogg(THIS_MIDDLEWARE, new LB(ei));
                            }
                        }
                        Base.initLogg(tmp.getId(), "logg.job.getstatus");
                        getStatus(tmp);
                        Base.endJobLogg(tmp, LB.INFO, "");
                        tmp.setTimestamp(System.currentTimeMillis());
                        break;
                    case ABORT:
                        tmp.setStatus(ActivityStateEnumeration.CANCELLED);
                        Base.initLogg(tmp.getId(), "logg.job.abort");
                        abort(tmp);
                        Base.endJobLogg(tmp, LB.INFO, "");
                        break;
                }
                if (isEndStatus(tmp)) {
                    Base.initLogg(tmp.getId(), "logg.job.getoutput");
                    getOutputs(tmp);
                    Base.endJobLogg(tmp, LB.INFO, "");
                    Base.getI().finishJob(tmp);
                } else if (isAbortStatus(tmp)) Base.getI().finishJob(tmp); else addJob(tmp);
            } catch (Exception e) {
                if (tmp != null) Base.writeJobLogg(tmp, e, "error.job." + tmp.getFlag());
            }
        }
    }

    protected boolean isEndStatus(Job job) {
        return job.getStatus().equals(ActivityStateEnumeration.FINISHED) || job.getStatus().equals(ActivityStateEnumeration.FAILED);
    }

    protected boolean isAbortStatus(Job job) {
        return job.getStatus().equals(ActivityStateEnumeration.CANCELLED);
    }

    public long getSize() {
        return jobs.size();
    }
}
