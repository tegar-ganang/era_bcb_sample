package com.planes.automirror;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author steve
 */
public class DownloadTask implements Runnable {

    private static final Logger LogPrinter = Logger.getLogger("autosync-mirror.DownloadTask");

    private static int BUFFER_SIZE = 40960;

    private PipedOutputStream Pipe = null;

    private File TheAskedFile;

    private boolean PipeConnected = false;

    private boolean IsAlreadyDownloaded = false;

    private String RequestedFile;

    private Collection<URL> ListFastest;

    private ArrayList<ReportCalculatedStatistique> Listener = new ArrayList();

    private String ContentType;

    private int ContentLength;

    /** Creates a new instance of DownloadTask */
    public DownloadTask(String requestedFile) {
        RequestedFile = requestedFile;
        Pipe = new PipedOutputStream();
        TheAskedFile = new File(System.getProperty("user.dir") + File.separator + requestedFile);
        IsAlreadyDownloaded = TheAskedFile.exists();
    }

    public String getContentType() {
        return ContentType;
    }

    public int getContentLength() {
        return ContentLength;
    }

    public InputStream getInputStream() throws IOException {
        if (!PipeConnected) {
            PipedInputStream ret = new PipedInputStream(Pipe);
            PipeConnected = true;
            return ret;
        } else {
            throw new IOException("Pipe is already connected to the stream");
        }
    }

    public boolean isAlreadyDownloaded() {
        return IsAlreadyDownloaded;
    }

    public void setMirrorList(Collection<URL> list) {
        ListFastest = list;
    }

    public void run() {
        LogPrinter.log(Level.FINEST, "Started Download at : {0, date, long}", new Date());
        if (!PipeConnected) {
            throw new IllegalStateException("You should connect the pipe before with getInputStream()");
        }
        InputStream ins = null;
        if (IsAlreadyDownloaded) {
            LogPrinter.log(Level.FINEST, "The file already Exists open and foward the byte");
            try {
                ContentLength = (int) TheAskedFile.length();
                ContentType = URLConnection.getFileNameMap().getContentTypeFor(TheAskedFile.getName());
                ins = new FileInputStream(TheAskedFile);
                byte[] buffer = new byte[BUFFER_SIZE];
                int read = ins.read(buffer);
                while (read >= 0) {
                    Pipe.write(buffer, 0, read);
                    read = ins.read(buffer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (ins != null) {
                    try {
                        ins.close();
                    } catch (IOException e) {
                    }
                }
            }
        } else {
            LogPrinter.log(Level.FINEST, "the file does not exist locally so we try to download the thing");
            File theDir = TheAskedFile.getParentFile();
            if (!theDir.exists()) {
                theDir.mkdirs();
            }
            for (URL url : ListFastest) {
                FileOutputStream fout = null;
                boolean OnError = false;
                long timestart = System.currentTimeMillis();
                long bytecount = 0;
                try {
                    URL newUrl = new URL(url.toString() + RequestedFile);
                    LogPrinter.log(Level.FINEST, "the download URL = {0}", newUrl);
                    URLConnection conn = newUrl.openConnection();
                    ContentType = conn.getContentType();
                    ContentLength = conn.getContentLength();
                    ins = conn.getInputStream();
                    fout = new FileOutputStream(TheAskedFile);
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int read = ins.read(buffer);
                    while (read >= 0) {
                        fout.write(buffer, 0, read);
                        Pipe.write(buffer, 0, read);
                        read = ins.read(buffer);
                        bytecount += read;
                    }
                    Pipe.flush();
                } catch (IOException e) {
                    OnError = true;
                } finally {
                    if (ins != null) {
                        try {
                            ins.close();
                        } catch (IOException e) {
                        }
                    }
                    if (fout != null) {
                        try {
                            fout.close();
                        } catch (IOException e) {
                        }
                    }
                }
                long timeend = System.currentTimeMillis();
                if (OnError) {
                    continue;
                } else {
                    long timetook = timeend - timestart;
                    BigDecimal speed = new BigDecimal(bytecount).multiply(new BigDecimal(1000)).divide(new BigDecimal(timetook), MathContext.DECIMAL32);
                    for (ReportCalculatedStatistique report : Listener) {
                        report.reportUrlStat(url, speed, timetook);
                    }
                    break;
                }
            }
        }
        LogPrinter.log(Level.FINEST, "download finished at {0,date,long}", new Date());
        if (Pipe != null) {
            try {
                Pipe.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void addReportStatsListener(ReportCalculatedStatistique reportCalculatedStatistique) {
        Listener.add(reportCalculatedStatistique);
    }
}
