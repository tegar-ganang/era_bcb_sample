package net.charabia.ac.update;

import java.io.*;
import java.util.*;
import java.net.*;
import net.charabia.ac.*;

/**
 * Downloads a file using an http connection.
 *
 */
public class Downloader {

    private URL m_location;

    private HttpURLConnection m_connection;

    private int m_size;

    protected boolean m_cancelled = false;

    private File m_tempFile = null;

    private long m_fileSize;

    private Vector m_transferListeners = new Vector();

    private boolean m_doneInit = false;

    public interface TransferListener {

        public void transferUpdated(double percent);

        public void transferComplete(File tmpFile);

        public void transferFailed();
    }

    public Downloader(URL loc) {
        m_location = loc;
    }

    public void addTransferListener(TransferListener tul) {
        m_transferListeners.addElement(tul);
    }

    public void removeTransferListener(TransferListener tul) {
        m_transferListeners.removeElement(tul);
    }

    public void init() {
        m_doneInit = true;
        try {
            m_connection = (HttpURLConnection) m_location.openConnection();
            System.out.println("conn = " + m_connection.getClass().toString());
            m_connection.setDoInput(true);
            m_connection.setDoOutput(false);
            m_fileSize = m_connection.getContentLength();
            System.out.println("SIZE :" + m_connection.getContentLength());
            System.out.println("DATE : " + m_connection.getLastModified());
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    public long getFileSize() {
        return m_fileSize;
    }

    protected void fireTransferUpdateEvent(double complete) {
        for (Enumeration e = m_transferListeners.elements(); e.hasMoreElements(); ) {
            ((TransferListener) e.nextElement()).transferUpdated(complete);
        }
    }

    protected void fireTransferCompleteEvent() {
        for (Enumeration e = m_transferListeners.elements(); e.hasMoreElements(); ) {
            ((TransferListener) e.nextElement()).transferComplete(m_tempFile);
        }
    }

    protected void fireTransferFailedEvent() {
        for (Enumeration e = m_transferListeners.elements(); e.hasMoreElements(); ) {
            ((TransferListener) e.nextElement()).transferFailed();
        }
    }

    public void start() {
        if (!m_doneInit) init();
        Thread t = new Thread(new Runnable() {

            public void run() {
                InputStream is = null;
                BufferedInputStream bis = null;
                FileOutputStream fos = null;
                BufferedOutputStream bos = null;
                try {
                    is = m_connection.getInputStream();
                    bis = new BufferedInputStream(is, 1024);
                    m_tempFile = File.createTempFile("aclocator.", ".tmp");
                    fos = new FileOutputStream(m_tempFile);
                    bos = new BufferedOutputStream(fos, 1024);
                    byte[] buffer = new byte[1024];
                    int readcount;
                    double totalcount = (double) m_connection.getContentLength();
                    double totalreadcount = 0;
                    while ((!m_cancelled) && ((readcount = bis.read(buffer, 0, buffer.length)) != -1)) {
                        bos.write(buffer, 0, readcount);
                        totalreadcount += (double) readcount;
                        fireTransferUpdateEvent(totalreadcount / totalcount);
                    }
                    bos.flush();
                    fos.flush();
                    bos.close();
                    fos.close();
                    bis.close();
                    is.close();
                    if (m_cancelled) {
                        m_connection.disconnect();
                    }
                    fireTransferCompleteEvent();
                    return;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    try {
                        if (bos != null) bos.close();
                    } catch (IOException iox) {
                    }
                    try {
                        if (fos != null) fos.close();
                    } catch (IOException iox) {
                    }
                    try {
                        if (bis != null) bis.close();
                    } catch (IOException iox) {
                    }
                    try {
                        if (is != null) is.close();
                    } catch (IOException iox) {
                    }
                }
                m_cancelled = true;
                fireTransferFailedEvent();
            }
        });
        t.start();
    }

    public void cancel() {
        m_cancelled = true;
    }

    public boolean isCancelled() {
        return m_cancelled;
    }

    public void disconnect() {
        m_connection.disconnect();
    }

    public static void main(String[] args) throws Exception {
        URL url = new URL("http://aclocator.dyndns.org/");
        Downloader sd = new Downloader(url);
        sd.addTransferListener(new Downloader.TransferListener() {

            public void transferUpdated(double percent) {
                System.out.println("transferUpdated : " + percent);
            }

            public void transferComplete(File tmpFile) {
                System.out.println("transferComplete : " + tmpFile);
            }

            public void transferFailed() {
                System.out.println("transferFailed : ");
            }
        });
        sd.init();
        sd.start();
    }
}
