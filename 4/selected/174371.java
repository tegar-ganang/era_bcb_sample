package com.antmanager.execute;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Calendar;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.filters.StringInputStream;

/**
 * A class for counting times.
 *
 * @author pb00141
 *
 */
public class StopWatch extends Task {

    private static Object mutex = new Object();

    private Calendar start = null;

    private Calendar finish = null;

    private String job;

    private PrintStream out;

    public StopWatch(String job, PrintStream out) {
        this();
        this.job = job;
        this.out = out;
    }

    public StopWatch() {
        this.out = null;
        this.job = null;
        this.init();
    }

    public void init() {
        this.start = null;
        this.finish = null;
    }

    public void start() {
        this.init();
        this.start = Calendar.getInstance();
    }

    public void finish() {
        this.finish = Calendar.getInstance();
    }

    public String getDate() {
        return this.start.get(Calendar.YEAR) + "-" + this.start.get(Calendar.MONTH) + "-" + this.start.get(Calendar.DATE) + "_" + this.getStart();
    }

    public String getStart() {
        return this.start.get(Calendar.HOUR) + ":" + this.start.get(Calendar.MINUTE) + ":" + this.start.get(Calendar.SECOND);
    }

    public String getFinish() {
        return this.finish.get(Calendar.HOUR) + ":" + this.finish.get(Calendar.MINUTE) + ":" + this.finish.get(Calendar.SECOND);
    }

    public String getEllapsedTime() {
        return this.getEllapsedHours() + ":" + this.getEllapsedMinutes() + ":" + this.getEllapsedSeconds() + ":" + this.getEllapsedMillis();
    }

    public long getDifference() {
        if (this.start != null || this.finish != null) return this.finish.getTimeInMillis() - this.start.getTimeInMillis(); else return 0;
    }

    public long getEllapsedMillis() {
        return this.getMillis() % 1000;
    }

    public long getEllapsedSeconds() {
        return this.getSeconds() % 60;
    }

    public long getEllapsedMinutes() {
        return this.getMinutes() % 60;
    }

    public long getEllapsedHours() {
        return this.getHours();
    }

    private long getHours() {
        return this.getMinutes() / 60;
    }

    private long getMinutes() {
        return this.getSeconds() / 60;
    }

    private long getSeconds() {
        return this.getDifference() / 1000;
    }

    private long getMillis() {
        return this.getDifference();
    }

    private void write(String txt) {
        InputStream is = new StringInputStream(txt);
        try {
            int item;
            while ((item = is.read()) != -1) this.out.write(item);
            this.out.println();
        } catch (IOException e) {
            throw new BuildException("Error while writing StopWatch");
        }
    }

    public void writeStart() {
        synchronized (StopWatch.mutex) {
            write("********************************************************");
            write("EXECUTION OF " + this.job + " STARTS AT ==> " + this.getStart());
            write("--------------------------------------------------------");
        }
    }

    public void writeEnd() {
        synchronized (StopWatch.mutex) {
            write("EXECUTION OF " + this.job + " ENDS AT ==> " + this.getFinish());
            write("--------------------------------------------------------");
            write("EXECUTION OF " + this.job + " LASTS ==> " + this.getEllapsedTime());
            write("--------------------------------------------------------");
            write("********************************************************");
            write("********************************************************");
        }
    }

    public void writeElapsTime() {
        synchronized (StopWatch.mutex) {
            write("EXECUTION OF " + this.job + " LASTS ==> " + this.getEllapsedTime());
            write("--------------------------------------------------------");
        }
    }
}
