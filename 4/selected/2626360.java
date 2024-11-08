package org.rjam.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.rjam.Event;
import org.rjam.EventUnit;
import org.rjam.api.ILineWriter;
import org.rjam.api.IReporter;
import org.rjam.base.BaseQueueReporter;
import org.rjam.io.CRLFLineWriter;
import org.rjam.xml.Token;

/**
 * @author Tony Bringardner
 * 
 * Write events to a log file.
 *
 */
public class LogReporter extends BaseQueueReporter implements IReporter {

    private static final long serialVersionUID = 1L;

    private File file;

    private ILineWriter output;

    private boolean append = false;

    private long maxFileSize = (1024 * 1024) * 10;

    private int outBufSize = 1024 * 4;

    public LogReporter() {
        output = new CRLFLineWriter(System.out, outBufSize);
    }

    public LogReporter(File file) {
        setFile(file);
    }

    public boolean isAppend() {
        return append;
    }

    public void setAppend(boolean append) {
        this.append = append;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        if (this.file != null && output != null) {
            try {
                output.close();
            } catch (Exception ex) {
            }
        }
        output = null;
        this.file = file;
    }

    public ILineWriter getOutput() throws IOException {
        if (output == null) {
            File file = getFile();
            File dir = file.getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            output = new CRLFLineWriter(new FileOutputStream(file, isAppend()), outBufSize);
            ClassLoader loader = getClass().getClassLoader();
            if (loader == null) {
                output.writeLine("#Opening log file. System Loader");
            } else {
                output.writeLine("#Opening log file. Loader =" + loader.getClass().getName());
            }
        }
        return output;
    }

    private void renameFile() {
        if (output != null) {
            try {
                output.close();
            } catch (IOException e) {
                logError("Error closeing file");
            }
            output = null;
        }
        int cnt = 1;
        File dir = file.getParentFile();
        File nf = new File(dir, file.getName() + cnt);
        while (nf.exists()) {
            cnt++;
            nf = new File(dir, file.getName() + cnt);
        }
        file.renameTo(nf);
    }

    public void writeEventUnit(EventUnit unit) throws IOException {
        File file = getFile();
        if (file.length() > getMaxFileSize()) {
            renameFile();
        }
        ILineWriter out = getOutput();
        Event event = unit.getEvent();
        if (unit.getAction().equals(EventUnit.END)) {
            out.writeLine(unit.toString() + "," + unit.getThreadId() + ", time=" + event.getProcessingTime());
        } else if (unit.getAction().equals(EventUnit.SYSTEM)) {
            Event evt = unit.getEvent();
            out.writeLine(evt.getClassName() + "," + evt.getMethodName() + "," + unit.getEncodedData() + "," + unit.getMetricValue());
        } else {
            out.writeLine(unit.toString() + "," + unit.getThreadId());
        }
    }

    public void configure(Token tok) {
        super.configure(tok);
        setFile(new File(getProperty("FileName", "RJamData.txt")));
        setAppend(Boolean.valueOf(getProperty("Append", "false")).booleanValue());
        String tmp = getProperty("MaxFileSize");
        if (tmp != null) {
            long mul = 1;
            tmp = tmp.toLowerCase();
            int idx = tmp.indexOf('m');
            if (idx > 0) {
                mul = (1024 * 1024);
                tmp = tmp.substring(0, idx);
            } else if ((idx = tmp.indexOf('k')) > 0) {
                mul = 1024;
                tmp = tmp.substring(0, idx);
            }
            setMaxFileSize(mul * Long.parseLong(tmp));
        }
        tmp = getProperty("BufferSize");
        if (tmp != null) {
            outBufSize = Integer.parseInt(tmp);
        }
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public void postWriteEvents() {
        try {
            getOutput().flush();
        } catch (IOException e) {
        }
    }

    public void preWriteEvents() {
    }

    public void close() {
        if (output != null) {
            try {
                output.close();
            } catch (Exception ex) {
            }
        }
    }

    public boolean isConnected() {
        return output != null;
    }
}
