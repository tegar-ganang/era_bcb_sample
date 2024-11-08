package org.alcibiade.eternity.editor.solver;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.alcibiade.eternity.editor.log.SolverLog;
import org.alcibiade.eternity.editor.model.GridModel;
import org.alcibiade.eternity.editor.stats.ScoreStats;

public class ClusterManager implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String PARAM_BLOCK_SIZE = "stats.block";

    private ThreadLocal<Long> timeStart;

    private SolverLog log;

    private GridModel bestSolution;

    private int bestScore;

    private String resultFileName;

    private boolean solutionFound;

    private ScoreStats stats;

    private Set<ClusterListener> clusterListeners;

    public ClusterManager(SolverLog log) {
        this.log = log;
        this.timeStart = new ThreadLocal<Long>();
        this.bestSolution = new GridModel();
        this.bestScore = 0;
        this.solutionFound = false;
        String blockString = System.getProperties().getProperty(PARAM_BLOCK_SIZE);
        if (blockString == null) {
            stats = null;
        } else {
            stats = new ScoreStats(log, Integer.parseInt(blockString));
        }
        clusterListeners = new HashSet<ClusterListener>();
    }

    public ClusterManager(SolverLog log, String resultFileName) {
        this(log);
        this.resultFileName = resultFileName;
    }

    public int getBestScore() {
        return bestScore;
    }

    public GridModel getBestSolution() {
        return bestSolution;
    }

    public synchronized void showStartMessage() {
        logMessage("Solver starting");
        timeStart.set(System.currentTimeMillis());
    }

    public SolverLog getSolverLog() {
        return log;
    }

    public synchronized void showStats(long iterations) {
        long duration = getElapsedTime();
        if (duration > 150) {
            logMessage("Duration:   %s", durationToString(duration));
            logMessage("Iterations: %d = %dit/s", iterations, iterations * 1000 / duration);
        }
    }

    public long getElapsedTime() {
        long elapsed = 0;
        Long timeStart = this.timeStart.get();
        if (timeStart != null) {
            long time_now = System.currentTimeMillis();
            elapsed = time_now - timeStart;
        }
        return elapsed;
    }

    private static long[] splitUnits(long value, long... bases) {
        long[] units = new long[bases.length + 1];
        long remaining = value;
        for (int bIndex = bases.length - 1; bIndex >= 0; bIndex--) {
            long base = bases[bIndex];
            units[bIndex + 1] = remaining % base;
            remaining /= base;
        }
        units[0] = remaining;
        return units;
    }

    private static String durationToString(long millis) {
        long[] timeUnits = splitUnits(millis, 24, 60, 60, 1000);
        return String.format("%d days, %d hours, %d minutes, %d seconds", timeUnits[0], timeUnits[1], timeUnits[2], timeUnits[3]);
    }

    public boolean isSolutionFound() {
        return solutionFound;
    }

    public boolean submitSolution(GridModel grid) {
        int gridScore = grid.countPairs();
        return submitSolution(grid, gridScore);
    }

    public boolean submitSolution(GridModel grid, int gridScore) {
        int targetScore = grid.countConnections();
        synchronized (this) {
            if (stats != null) {
                stats.recordScore(gridScore);
            }
            if (bestScore < gridScore) {
                grid.copyTo(bestSolution);
                bestScore = gridScore;
                logMessage(String.format("Best score: %3d/%d", bestScore, targetScore));
                writeResultToFile(grid, targetScore);
                notifyListeners(bestScore);
            }
        }
        if (!solutionFound && gridScore == targetScore) {
            solutionFound = true;
            logMessage("Solution found !");
        }
        return solutionFound;
    }

    private void writeResultToFile(GridModel grid, int targetScore) {
        if (resultFileName != null) {
            try {
                SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                PrintWriter writer = new PrintWriter(new FileWriter(resultFileName));
                writer.println("#");
                writer.println(String.format("# Grid format: %dx%dx%d", grid.getSize(), grid.getSize(), grid.getPatternStats().getPatterns().size()));
                writer.println(String.format("#  Grid score: %d/%d", bestScore, targetScore));
                writer.println("#        Time: " + df.format(new Date()));
                writer.println("#     Elapsed: " + durationToString(getElapsedTime()));
                writer.println("#        Host: " + InetAddress.getLocalHost().getHostName());
                writer.println("#      Thread: " + Thread.currentThread().getName());
                writer.println("#");
                writer.println();
                writer.println(bestSolution.toQuadString());
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void logMessage(String message, Object... args) {
        log.logMessage(String.format(message, args));
    }

    protected void notifyListeners(int bestScore) {
        for (ClusterListener listener : clusterListeners) {
            listener.bestSolutionUpdated(bestScore);
        }
    }

    public void addClusterListener(ClusterListener listener) {
        clusterListeners.add(listener);
    }

    public void removeClusterListener(ClusterListener listener) {
        clusterListeners.remove(listener);
    }
}
