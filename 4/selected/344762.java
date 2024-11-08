package blomo.util.loadbalancing;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import blomo.util.SimpleChart;

/**
 * @author Malte Schulze
 * 
 * Extends the BLoMoLoadBalancerImpl with debug functionality.
 */
public class BLoMoLoadBalancerDebugImpl extends BLoMoLoadBalancerImpl implements LoadBalancer {

    private Logger logger = LoggerFactory.getLogger(BLoMoLoadBalancerDebugImpl.class);

    protected long droppedTasks;

    protected long executionExceptions;

    protected double averageExecutionTime;

    protected double averageWaitTime;

    private long recordedExecutionThreads;

    private long recordedWaitingThreads;

    private Object execLock = new Object();

    private Object waitLock = new Object();

    protected long probeTime;

    protected long numProbes;

    protected String loggingDir;

    protected DebugThread debugThread;

    private class StatStorage {

        long time;

        int pending;

        int occupied;

        long dropped;

        long exceptions;

        int currentWeight;

        double executionTime;

        double waitTime;
    }

    private StatStorage parse(String[] parts) {
        StatStorage stat = new StatStorage();
        stat.time = Long.parseLong(parts[0]);
        stat.pending = Integer.parseInt(parts[1]);
        stat.occupied = Integer.parseInt(parts[2]);
        stat.dropped = Long.parseLong(parts[3]);
        stat.exceptions = Long.parseLong(parts[4]);
        stat.currentWeight = Integer.parseInt(parts[5]);
        stat.waitTime = Double.parseDouble(parts[6]);
        stat.executionTime = Double.parseDouble(parts[7]);
        return stat;
    }

    private class DebugThread extends Thread {

        @Override
        public void run() {
            File dir = new File(loggingDir);
            if (!dir.isDirectory()) {
                logger.error("Logging directory \"" + dir.getAbsolutePath() + "\" does not exist.");
                return;
            }
            File file = new File(dir, new Date().toString().replaceAll("[ ,:]", "") + "LoadBalancerLog.txt");
            FileWriter writer;
            try {
                writer = new FileWriter(file);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            int counter = 0;
            while (!isInterrupted() && counter < numProbes) {
                try {
                    writer.write(System.currentTimeMillis() + "," + currentPending + "," + currentThreads + "," + droppedTasks + "," + executionExceptions + "," + currentWeight + "," + averageWaitTime + "," + averageExecutionTime + "#");
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
                counter++;
                try {
                    sleep(probeTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            FileReader reader;
            try {
                reader = new FileReader(file);
            } catch (FileNotFoundException e2) {
                e2.printStackTrace();
                return;
            }
            Vector<StatStorage> dataV = new Vector<StatStorage>();
            int c;
            try {
                c = reader.read();
            } catch (IOException e1) {
                e1.printStackTrace();
                c = -1;
            }
            String entry = "";
            Date startTime = null;
            Date stopTime = null;
            while (c != -1) {
                if (c == 35) {
                    String parts[] = entry.split(",");
                    if (startTime == null) startTime = new Date(Long.parseLong(parts[0]));
                    if (parts.length > 0) dataV.add(parse(parts));
                    stopTime = new Date(Long.parseLong(parts[0]));
                    entry = "";
                } else {
                    entry += (char) c;
                }
                try {
                    c = reader.read();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (dataV.size() > 0) {
                int[] dataPending = new int[dataV.size()];
                int[] dataOccupied = new int[dataV.size()];
                long[] dataDropped = new long[dataV.size()];
                long[] dataException = new long[dataV.size()];
                int[] dataWeight = new int[dataV.size()];
                long[] dataExecution = new long[dataV.size()];
                long[] dataWait = new long[dataV.size()];
                for (int i = 0; i < dataV.size(); i++) {
                    dataPending[i] = dataV.get(i).pending;
                    dataOccupied[i] = dataV.get(i).occupied;
                    dataDropped[i] = dataV.get(i).dropped;
                    dataException[i] = dataV.get(i).exceptions;
                    dataWeight[i] = dataV.get(i).currentWeight;
                    dataExecution[i] = (long) dataV.get(i).executionTime;
                    dataWait[i] = (long) dataV.get(i).waitTime;
                }
                String startName = startTime.toString();
                startName = startName.replaceAll("[ ,:]", "");
                file = new File(dir, startName + "pending.gif");
                SimpleChart.drawChart(file, 640, 480, dataPending, startTime, stopTime, new Color(0, 0, 0));
                file = new File(dir, startName + "occupied.gif");
                SimpleChart.drawChart(file, 640, 480, dataOccupied, startTime, stopTime, new Color(0, 0, 0));
                file = new File(dir, startName + "dropped.gif");
                SimpleChart.drawChart(file, 640, 480, dataDropped, startTime, stopTime, new Color(0, 0, 0));
                file = new File(dir, startName + "exceptions.gif");
                SimpleChart.drawChart(file, 640, 480, dataException, startTime, stopTime, new Color(0, 0, 0));
                file = new File(dir, startName + "weight.gif");
                SimpleChart.drawChart(file, 640, 480, dataWeight, startTime, stopTime, new Color(0, 0, 0));
                file = new File(dir, startName + "execution.gif");
                SimpleChart.drawChart(file, 640, 480, dataExecution, startTime, stopTime, new Color(0, 0, 0));
                file = new File(dir, startName + "wait.gif");
                SimpleChart.drawChart(file, 640, 480, dataWait, startTime, stopTime, new Color(0, 0, 0));
            }
            recordedExecutionThreads = 0;
            recordedWaitingThreads = 0;
            averageExecutionTime = 0;
            averageWaitTime = 0;
            if (!isLocked) {
                debugThread = new DebugThread();
                debugThread.start();
            }
        }
    }

    private void addExecutionTime(long time) {
        synchronized (execLock) {
            recordedExecutionThreads++;
            averageExecutionTime = ((recordedExecutionThreads - 1) * averageExecutionTime + time) / recordedExecutionThreads;
        }
    }

    private void addWaitTime(long time) {
        synchronized (waitLock) {
            recordedWaitingThreads++;
            averageWaitTime = ((recordedWaitingThreads - 1) * averageWaitTime + time) / recordedWaitingThreads;
        }
    }

    /**
	 * @param maxWeight 
	 * @param minimumThreads 
	 */
    public BLoMoLoadBalancerDebugImpl(int maxWeight, int minimumThreads) {
        this(maxWeight, minimumThreads, -1, 2 * minimumThreads, 60000, 60, "../../logs");
    }

    /**
	 * @param maxWeight
	 * @param minimumThreads
	 * @param maxPending
	 * @param maximumThreads 
	 * @param probeTime 
	 * @param numProbes 
	 * @param loggingDir 
	 */
    public BLoMoLoadBalancerDebugImpl(int maxWeight, int minimumThreads, int maxPending, int maximumThreads, long probeTime, long numProbes, String loggingDir) {
        super(maxWeight, minimumThreads, maxPending, maximumThreads);
        this.loggingDir = loggingDir;
        this.probeTime = probeTime;
        this.numProbes = numProbes;
        droppedTasks = 0;
        executionExceptions = 0;
    }

    @Override
    protected void buildStaticThreads() {
        for (int i = staticThreads.size(); i < minimumThreads; i++) {
            RunTask t = new RunWrapperDebug(null);
            t.start();
            staticThreads.add(t);
        }
    }

    private class RunWrapperDebug extends Thread implements RunTask {

        public MethodExecutorWrapper r;

        protected boolean runOnce;

        public RunWrapperDebug(MethodExecutorWrapper r) {
            this(r, false);
        }

        public RunWrapperDebug(MethodExecutorWrapper r, boolean runOnce) {
            this.r = r;
            this.runOnce = runOnce;
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                if (r != null) {
                    long start = System.currentTimeMillis();
                    r.getMethodExecutor().execute();
                    if (r.getMethodExecutor().getException() == null) addExecutionTime(System.currentTimeMillis() - start);
                    threadEnded(r);
                    r = null;
                    if (runOnce) break;
                }
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
            occupiedThreads.remove(this);
            synchronized (occupiedThreads) {
                currentThreads--;
            }
        }

        @Override
        public synchronized void start() {
            synchronized (occupiedThreads) {
                currentThreads++;
            }
            super.start();
            occupiedThreads.add(this);
        }

        @Override
        public MethodExecutorWrapper getMethodExecutorWrapper() {
            return r;
        }

        @Override
        public void setMethodExecutor(MethodExecutorWrapper mew) {
            r = mew;
        }

        @Override
        public boolean isReady() {
            return r == null;
        }
    }

    @Override
    public synchronized boolean addExecutor(int weight, MethodExecutor r) {
        boolean res = super.addExecutor(weight, r);
        if (!res) droppedTasks++;
        return res;
    }

    @Override
    protected void dropTask(MethodExecutorWrapper executor) {
        droppedTasks++;
        super.dropTask(executor);
        addWaitTime(System.currentTimeMillis() - executor.getStartTime());
    }

    @Override
    protected synchronized void threadEnded(MethodExecutorWrapper r) {
        super.threadEnded(r);
        if (r.getMethodExecutor().getException() != null) executionExceptions++;
    }

    @Override
    public void run() {
        debugThread = new DebugThread();
        debugThread.start();
        MethodExecutorWrapper r = null;
        while (!isInterrupted()) {
            if (isLocked) {
                r = null;
                try {
                    waitForNewThread();
                } catch (InterruptedException e) {
                    break;
                }
            }
            if (r == null) r = nextRunnable();
            if (r != null) {
                if (currentWeight == 0 || ((currentWeight + r.getWeight()) <= maxWeight)) {
                    RunTask t = getNextFreeThread();
                    if (t == null) {
                        if (currentThreads < maximumThreads) {
                            synchronized (this) {
                                currentWeight += r.getWeight();
                            }
                            t = new RunWrapperDebug(r, true);
                            t.start();
                            addWaitTime(System.currentTimeMillis() - r.getStartTime());
                            r = null;
                        } else {
                            try {
                                waitForNewThread(r.getStartTime() - System.currentTimeMillis() + getTimeout(r));
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    } else {
                        synchronized (this) {
                            currentWeight += r.getWeight();
                        }
                        t.setMethodExecutor(r);
                        synchronized (t) {
                            t.notify();
                        }
                        addWaitTime(System.currentTimeMillis() - r.getStartTime());
                        r = null;
                    }
                } else if (hasTimedOut(r)) {
                    dropTask(r);
                    r = null;
                } else {
                    try {
                        waitForNewThread(r.getStartTime() - System.currentTimeMillis() + getTimeout(r));
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            } else {
                try {
                    waitForNewThread();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    @Override
    public void lock() {
        super.lock();
        debugThread.interrupt();
    }
}
