package org.opendte.launcher;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import org.opendte.common.Logger;

public class JobConsole extends Thread {

    private OutputStream mProcessStdOut = System.out;

    private OutputStream mProcessErrOut = System.err;

    private int mSleepTimeInMillis = 20;

    private boolean mHangUpFlag = false;

    private Process mProcess = null;

    public JobConsole(Process p) {
        mProcess = p;
    }

    public void setStdOutputStream(OutputStream out) {
        mProcessStdOut = out;
    }

    public void setErrOutputStream(OutputStream out) {
        mProcessErrOut = out;
    }

    public void setSleepTime(int v) {
        mSleepTimeInMillis = v;
    }

    public void close() {
        mHangUpFlag = true;
    }

    public void run() {
        int nread;
        byte[] buffer = new byte[1024];
        InputStream stdout = mProcess.getInputStream();
        InputStream stderr = mProcess.getErrorStream();
        while (true) {
            try {
                if (stdout.available() > 0) {
                    nread = stdout.read(buffer);
                    mProcessStdOut.write(buffer, 0, nread);
                    mProcessStdOut.flush();
                }
                if (stderr.available() > 0) {
                    nread = stderr.read(buffer);
                    mProcessErrOut.write(buffer, 0, nread);
                    mProcessErrOut.flush();
                }
            } catch (IOException e) {
                Logger.traceException(e);
            }
            if (mHangUpFlag) break;
            try {
                Thread.sleep(mSleepTimeInMillis);
            } catch (InterruptedException e) {
            }
        }
    }
}
