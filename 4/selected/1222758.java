package edu.ucla.cs.typecast.app.news;

import java.io.*;
import java.util.*;
import edu.ucla.cs.typecast.app.news.type.AbstractNewsArticle;
import edu.ucla.cs.typecast.event.EventListener;
import edu.ucla.cs.typecast.event.EventManager;
import edu.ucla.cs.typecast.event.EventSink;

public class NewsReceiver implements EventListener {

    protected Map<Class, Integer> receiveCount = new HashMap<Class, Integer>();

    protected File logFile;

    protected long writeWaitStart;

    protected long writeWaitTime;

    protected boolean changed = false;

    protected static void usage() {
        System.err.println("Usage: java [<parameters>] NewsReceiver <log file> <write wait time>");
        System.exit(1);
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            usage();
        }
        int writeWaitTime = 0;
        try {
            writeWaitTime = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            usage();
        }
        final NewsReceiver receiver = new NewsReceiver(new File(args[0]), writeWaitTime);
        final int writeWaitTimeCopy = writeWaitTime;
        Thread updateThread = new Thread() {

            public void run() {
                super.run();
                while (true) {
                    receiver.update();
                    try {
                        Thread.sleep(writeWaitTimeCopy);
                    } catch (InterruptedException e) {
                    }
                }
            }
        };
        updateThread.setDaemon(true);
        updateThread.start();
    }

    public NewsReceiver(File logFile, int writeWaitTime) throws IOException {
        this.logFile = logFile;
        this.writeWaitTime = writeWaitTime;
        EventSink sink = EventManager.getInstance().getEventSink(NewsArticle.class);
        sink.addEventListener(this, null);
    }

    public void notify(Class[] types, Object event, Object handback) {
        if (event instanceof NewsArticle) {
            updateCount(event);
            update();
        } else {
        }
    }

    public void update() {
        if (changed && System.currentTimeMillis() - writeWaitStart >= writeWaitTime * 1000) {
            writeLog();
        }
    }

    public synchronized void updateCount(Object event) {
        Class c = event.getClass();
        while (c != null && c != Object.class && c != NewsArticle.class && c != AbstractNewsArticle.class) {
            if (receiveCount.get(c) != null) {
                receiveCount.put(c, receiveCount.get(c) + 1);
            } else {
                receiveCount.put(c, 1);
            }
            changed = true;
            writeWaitStart = System.currentTimeMillis();
            c = c.getSuperclass();
        }
    }

    public synchronized void writeLog() {
        try {
            String runNum = Integer.toString(getRunNumber(logFile) + 1);
            String time = Long.toString(System.currentTimeMillis());
            BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true));
            for (Map.Entry<Class, Integer> entry : receiveCount.entrySet()) {
                bw.write(runNum);
                bw.write("\t");
                bw.write(time);
                bw.write("\t");
                bw.write(entry.getKey().getName());
                bw.write("\t");
                bw.write(Integer.toString(entry.getValue()));
                bw.write("\n");
            }
            bw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        changed = false;
    }

    public int getRunNumber(File f) throws IOException {
        if (!f.exists()) return 0;
        BufferedReader br = new BufferedReader(new FileReader(logFile));
        int result = 0;
        while (true) {
            String line = br.readLine();
            if (line == null) break;
            String[] parts = line.split("\t");
            if (parts.length > 0) {
                try {
                    result = Integer.parseInt(parts[0]);
                } catch (NumberFormatException e) {
                }
            }
        }
        br.close();
        return result;
    }
}
