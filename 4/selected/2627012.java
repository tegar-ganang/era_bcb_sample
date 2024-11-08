package ec.eval;

import java.io.*;
import java.net.*;
import java.util.LinkedList;
import ec.*;
import java.util.*;

/**
 * SlaveConnection.java
 *

 This class contains certain information associated with a slave: its name, connection socket,
 input and output streams, and the job queue.  Additionally, the class sets up an auxillary thread
 which reads and writes to the streams to talk to the slave in the background.  This thread uses
 the SlaveMonitor as its synchronization point (it sleeps with wait() and wakes up when notified()
 to do some work).
 
 <P>Generally SlaveConnection is only seen by communicates only with SlaveMonitor.

 * @author Liviu Panait, Keith Sullivan, and Sean Luke
 * @version 2.0 
 */
class SlaveConnection {

    /** Name of the slave process */
    String slaveName;

    /**  Socket for communication with the slave process */
    Socket evalSocket;

    /**  Used to transmit data to the slave. */
    DataOutputStream dataOut;

    /**  Used to read results and randoms state from slave. */
    public DataInputStream dataIn;

    EvolutionState state;

    SlaveMonitor slaveMonitor;

    Thread reader;

    Thread writer;

    private LinkedList jobs = new LinkedList();

    /**
       The constructor also creates the queue storing the jobs that the slave
       has been asked to evaluate.  It also creates and launches the worker
       thread that is communicating with the remote slave to read back the results
       of the evaluations.
    */
    public SlaveConnection(EvolutionState state, String slaveName, Socket evalSocket, DataOutputStream dataOut, DataInputStream dataIn, SlaveMonitor slaveMonitor) {
        this.slaveName = slaveName;
        this.evalSocket = evalSocket;
        this.dataOut = dataOut;
        this.dataIn = dataIn;
        this.state = state;
        this.slaveMonitor = slaveMonitor;
        buildThreads();
        showDebugInfo = slaveMonitor.showDebugInfo;
    }

    /**
       This method is called whenever there are any communication problems with the slave
       (indicating possibly that the slave might have crashed).  In this case, the jobs will
       be rescheduled for evaluation on other slaves.
    */
    boolean shuttingDown;

    Object shutDownLock = new int[0];

    protected void shutdown(final EvolutionState state) {
        synchronized (shutDownLock) {
            if (shuttingDown) return; else shuttingDown = true;
        }
        try {
            dataOut.writeByte(Slave.V_SHUTDOWN);
        } catch (Exception e) {
        }
        try {
            dataOut.flush();
        } catch (Exception e) {
        }
        try {
            dataOut.close();
        } catch (Exception e) {
        }
        try {
            dataIn.close();
        } catch (Exception e) {
        }
        try {
            evalSocket.close();
        } catch (IOException e) {
        }
        state.output.systemMessage(SlaveConnection.this.toString() + " Slave is shutting down....");
        slaveMonitor.unregisterSlave(this);
        rescheduleJobs(state);
        synchronized (jobs) {
            slaveMonitor.notifyMonitor(jobs);
            reader.interrupt();
            writer.interrupt();
        }
        state.output.systemMessage(SlaveConnection.this.toString() + " Slave exits....");
    }

    public String toString() {
        return "Slave(" + slaveName + ")";
    }

    boolean showDebugInfo;

    final void debug(String s) {
        if (showDebugInfo) {
            System.err.println(Thread.currentThread().getName() + "->" + s);
        }
    }

    /**
       Returns the number of jobs that a slave is in charge of.
    */
    public int numJobs() {
        synchronized (jobs) {
            return jobs.size();
        }
    }

    void buildThreads() {
        reader = new Thread() {

            public void run() {
                while (readLoop()) ;
            }
        };
        writer = new Thread() {

            public void run() {
                while (writeLoop()) ;
            }
        };
        writer.start();
        reader.start();
    }

    Job oldestUnsentJob() {
        Iterator i = jobs.iterator();
        while (i.hasNext()) {
            Job job = (Job) (i.next());
            if (!job.sent) {
                job.sent = true;
                return job;
            }
        }
        return null;
    }

    boolean writeLoop() {
        Job job = null;
        try {
            synchronized (jobs) {
                if ((job = oldestUnsentJob()) == null) {
                    debug("" + Thread.currentThread().getName() + "Waiting for a job to send");
                    slaveMonitor.waitOnMonitor(jobs);
                }
            }
            if (job != null) {
                debug("" + Thread.currentThread().getName() + "Sending Job");
                if (job.type == Slave.V_EVALUATESIMPLE) {
                    dataOut.writeByte(Slave.V_EVALUATESIMPLE);
                } else {
                    dataOut.writeByte(Slave.V_EVALUATEGROUPED);
                    dataOut.writeBoolean(job.countVictoriesOnly);
                }
                dataOut.writeInt(job.inds.length);
                for (int x = 0; x < job.subPops.length; x++) dataOut.writeInt(job.subPops[x]);
                debug("Starting to transmit individuals");
                for (int i = 0; i < job.inds.length; i++) {
                    job.inds[i].writeIndividual(state, dataOut);
                    dataOut.writeBoolean(job.updateFitness[i]);
                }
                dataOut.flush();
            }
        } catch (Exception e) {
            shutdown(state);
            return false;
        }
        return true;
    }

    boolean readLoop() {
        Job job = null;
        try {
            byte val = dataIn.readByte();
            debug(SlaveConnection.this.toString() + " Incoming Job");
            synchronized (jobs) {
                job = (Job) (jobs.getFirst());
            }
            debug("Got job: " + job);
            job.copyIndividualsForward();
            for (int i = 0; i < job.newinds.length; i++) {
                debug(SlaveConnection.this.toString() + " Individual# " + i);
                debug(SlaveConnection.this.toString() + " Reading Byte");
                if (i > 0) val = dataIn.readByte();
                debug(SlaveConnection.this.toString() + " Reading Individual");
                if (val == Slave.V_INDIVIDUAL) {
                    job.newinds[i].readIndividual(state, dataIn);
                } else if (val == Slave.V_FITNESS) {
                    job.newinds[i].evaluated = dataIn.readBoolean();
                    job.newinds[i].fitness.readFitness(state, dataIn);
                } else if (val == Slave.V_NOTHING) {
                }
                debug(SlaveConnection.this.toString() + " Read Individual");
            }
            job.copyIndividualsBack(state);
            synchronized (jobs) {
                jobs.removeFirst();
            }
            slaveMonitor.notifySlaveAvailability(SlaveConnection.this, job, state);
        } catch (IOException e) {
            shutdown(state);
            return false;
        }
        return true;
    }

    /**
       Adds a new jobs to the queue.  This implies that the slave will be in charge of executing
       this particular job.
    */
    public void scheduleJob(final Job job) {
        synchronized (jobs) {
            if (job.sent) state.output.fatal("Tried to reschedule an existing job");
            jobs.addLast(job);
            slaveMonitor.notifyMonitor(jobs);
        }
    }

    void rescheduleJobs(final EvolutionState state) {
        while (true) {
            Job job = null;
            synchronized (jobs) {
                if (jobs.isEmpty()) {
                    return;
                }
                job = (Job) (jobs.removeFirst());
            }
            debug(Thread.currentThread().getName() + " Waiting for a slave to reschedule the evaluation.");
            job.sent = false;
            slaveMonitor.scheduleJobForEvaluation(state, job);
            debug(Thread.currentThread().getName() + " Got a slave to reschedule the evaluation.");
        }
    }
}
