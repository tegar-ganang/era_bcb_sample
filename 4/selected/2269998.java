package com.peterhi.launcher;

import java.util.zip.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;

public class Downloader implements Runnable {

    private static final int WAIT_TIMEOUT = 5000;

    private static final Downloader instance = new Downloader();

    public static Downloader getInstance() {
        return instance;
    }

    private boolean running;

    private Object lock = new Object();

    private DownloadObserver observer;

    private URL src;

    private File dest;

    public boolean download(String src, String dest, DownloadObserver observer) throws MalformedURLException, IOException {
        if (src == null || src.length() <= 0 || dest == null || dest.length() <= 0) {
            throw new NullPointerException();
        }
        if (running) {
            return false;
        }
        this.src = new URL(src);
        this.dest = new File(dest);
        this.observer = observer;
        if (!this.dest.mkdirs()) {
            throw new IOException();
        }
        running = true;
        new Thread(this).start();
        return true;
    }

    public boolean abort() {
        if (!running) {
            return false;
        }
        running = false;
        synchronized (lock) {
            try {
                lock.wait(WAIT_TIMEOUT);
            } catch (Exception ex) {
            }
        }
        return true;
    }

    public void run() {
        try {
            fireUpdate(DownloadObserver.BEGINNING, null);
            fireUpdate(DownloadObserver.CONTENT_LENGTH, src.openConnection().getContentLength());
            ZipInputStream zin = new ZipInputStream(src.openStream());
            ZipEntry ze;
            byte[] buf = new byte[2048];
            int read;
            while ((ze = zin.getNextEntry()) != null) {
                File outFile = new File(dest, ze.getName());
                outFile.createNewFile();
                FileOutputStream fout = new FileOutputStream(outFile);
                fireUpdate(DownloadObserver.NEW_FILE, outFile);
                while ((read = zin.read(buf)) != -1) {
                    fout.write(buf, 0, read);
                    fireUpdate(DownloadObserver.PROGRESS, new Object[] { buf, 0, read });
                }
                fout.close();
            }
            zin.close();
            fireUpdate(DownloadObserver.END, null);
        } catch (Exception ex) {
            fireUpdate(DownloadObserver.ERROR, ex);
        }
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    private void fireUpdate(int type, Object data) {
        if (observer != null) {
            observer.update(type, data);
        }
    }
}
