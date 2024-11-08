package com.orientechnologies.tools.oexplorer;

import java.io.*;

/**
 * <p>Title: Orient Explorer</p>
 * <p>Description: Orient explorer visual tool</p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: Orient Technologies</p>
 * @author Orient Staff
 * @version 2.0
 */
public class LogOutputStream extends OutputStream {

    public void write(int b) throws java.io.IOException {
        MainFrame.getInstance().appendLog(String.valueOf((char) b));
    }

    public void showAsynchProcessOutput(Process iProc) {
        asynch.startConsume(iProc);
    }

    public void stopAsynchProcessOutput() {
        asynch.stopConsume();
    }

    public void showProcessOutputs(Process iProc) {
        try {
            int b;
            InputStream in = iProc.getInputStream();
            b = in.read();
            if (b != -1) {
                Application.getInstance().writeLog("OUTPUT:\n");
                write(b);
                while ((b = in.read()) != -1) write(b);
            }
            InputStream err = iProc.getInputStream();
            b = err.read();
            if (b != -1) {
                Application.getInstance().writeLog("ERROR:\n");
                write(b);
                while ((b = err.read()) != -1) write(b);
            }
        } catch (IOException e) {
        }
    }

    public static LogOutputStream getInstance() {
        if (_instance == null) synchronized (LogOutputStream.class) {
            if (_instance == null) _instance = new LogOutputStream();
        }
        return _instance;
    }

    private static LogOutputStream _instance = null;

    private static CommandOutputConsumer asynch = new CommandOutputConsumer();
}
