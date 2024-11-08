package org.vrspace.vfs;

import java.io.*;
import java.util.*;
import org.vrspace.util.*;

/**
Downloads bytes from input stream and writes them to the output stream.
For asynchronous download use start() method and DownloadListeners;
for blocking download just call run();
@see DownloadListener
*/
public class Download extends Thread {

    InputStream in;

    OutputStream out;

    LinkedList listeners;

    /** Number of bytes read; during active download, use it only in listeners */
    public int length;

    /** Download status */
    public int status = 0;

    /** Download start time in milliseconds */
    public long startTime;

    /** Download finish/fail time in milliseconds */
    public long stopTime;

    /** Time of last downloaded packet in milliseconds */
    public long lastTime;

    /** Packet size in bytes, default 1024 (1k)*/
    public int packetSize = 1024;

    public static final int NONE = 0, STARTED = 1, PROGRESS = 2, FINISHED = 3, FAILED = 4, CANCELED = 5;

    /** Exception that ocurred during download, if any */
    public Throwable exception;

    boolean active = true;

    public Download(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
    }

    public synchronized void addListener(DownloadListener dl) {
        if (listeners == null) listeners = new LinkedList();
        listeners.add(dl);
    }

    public synchronized void removeListener(DownloadListener dl) {
        if (listeners == null) return;
        listeners.remove(dl);
    }

    /**
  Downloads input and writes it to the output stream.
  Closes streams when finished.
  */
    public void run() {
        int size = packetSize;
        byte[] content = new byte[size];
        length = 0;
        int read = 0;
        status = STARTED;
        startTime = System.currentTimeMillis();
        notifyListeners();
        try {
            do {
                read = in.read(content);
                status = PROGRESS;
                length += read;
                lastTime = System.currentTimeMillis();
                if (read > 0) {
                    out.write(content, 0, read);
                    notifyListeners();
                }
            } while (active && read > 0);
            stopTime = System.currentTimeMillis();
            in.close();
            out.close();
        } catch (IOException ioe) {
            stopTime = System.currentTimeMillis();
            status = FAILED;
            exception = ioe;
            notifyListeners();
        }
    }

    /** cancels download and waits for completition */
    public void cancel() {
        active = false;
        while (status == PROGRESS) {
            Util.sleep(10);
        }
        status = CANCELED;
    }

    synchronized void notifyListeners() {
        if (listeners != null) {
            Iterator i = listeners.iterator();
            while (i.hasNext()) {
                DownloadListener dl = (DownloadListener) i.next();
                dl.download(this);
            }
        }
    }
}
