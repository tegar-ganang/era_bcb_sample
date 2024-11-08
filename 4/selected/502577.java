package org.utupiu.nibbana.log;

import java.util.Collection;

public class LogThread extends Thread {

    private static long DELAY = 1000;

    private boolean status = true;

    public void setDelay(int delay) {
        DELAY = delay;
    }

    public void run() {
        while (status) {
            printLogs();
            try {
                Thread.sleep(DELAY);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void printLogs() {
        for (LogComponent component : LogComponent.values) {
            for (LogWriter writer : component.AsyncWriters) {
                Collection<LogRow> logs = writer.pullLogs();
                WriterThread child = new WriterThread(writer, logs);
                child.start();
            }
        }
    }

    public void close() {
        this.status = false;
    }

    @Override
    protected void finalize() throws Throwable {
        printLogs();
        super.finalize();
    }
}
