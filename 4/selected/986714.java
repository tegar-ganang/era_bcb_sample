package phex.download;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import phex.*;
import phex.config.*;
import phex.connection.*;
import phex.download.*;
import phex.host.*;
import phex.utils.*;

public class DownloadWorker implements Runnable {

    private DownloadManager mManager;

    private DownloadFile mDownload = null;

    private Host mHost = null;

    private DownloadWorker() {
    }

    public DownloadWorker(DownloadManager dm) {
        mManager = dm;
        new Thread(this, "DownloadWorker-" + Integer.toHexString(hashCode())).start();
    }

    /**
     * Closes the connection to the remote host
     */
    public void stopDownload() {
        if (mHost != null) {
            mHost.disconnect();
        }
    }

    public void run() {
        byte[] buf = new byte[1024];
        final int bufSize = buf.length;
        RandomAccessFile rFile = null;
        int len;
        while (true) {
            mDownload = mManager.getNextFileToDownload(this);
            if (mDownload == null) {
                ServiceManager.log("DownloadWorker terminated.");
                break;
            }
            String saveFilename = mDownload.getFullLocalFilename();
            String downloadFilename = mDownload.getDownloadName();
            try {
                mManager.incDownloadingCount();
                mDownload.appendLog("Start download.");
                if (!deleteExistingFile(saveFilename)) {
                    mDownload.setStatus(DownloadFile.sStopped);
                    continue;
                }
                File dfile = new File(downloadFilename);
                boolean append = false;
                int startPos = 0;
                long lngStartPos = 0;
                if (dfile.exists()) {
                    append = true;
                    lngStartPos = dfile.length() - 1024;
                    if (lngStartPos < 0) {
                        lngStartPos = 0;
                    }
                    Long tempLong = new Long(lngStartPos);
                    startPos = tempLong.intValue();
                }
                mDownload.appendLog("position to read=" + startPos);
                try {
                    mDownload.appendLog("Download name=" + downloadFilename);
                    rFile = new RandomAccessFile(downloadFilename, "rw");
                } catch (Exception e) {
                    String msg = "Failed to create the file '";
                    msg += saveFilename + "' to save the download content.  ";
                    msg += e.getMessage();
                    JOptionPane.showMessageDialog(ServiceManager.getManager().getMainFrame(), msg, "Failed To Create File", JOptionPane.ERROR_MESSAGE);
                    throw e;
                }
                mDownload.appendLog("Connect " + mDownload.getCurrentRemoteFile().getURL());
                URL url = new URL(mDownload.getCurrentRemoteFile().getURL());
                HostAddress hostAddress = new HostAddress(url.getHost(), url.getPort());
                mHost = new Host();
                mHost.setType(Host.sTypeDownload);
                try {
                    Socket sock = ServiceManager.getConnectionManager().connect(url.getHost(), url.getPort());
                    mDownload.appendLog("Normal connection ok");
                    mHost.setSock(sock);
                    mHost.setOs(sock.getOutputStream());
                    mHost.setIs(sock.getInputStream());
                } catch (Exception e) {
                    mDownload.appendLog("Normal connection failed.  " + e.getMessage());
                    if (ServiceManager.sCfg.isBehindFirewall) {
                        mDownload.removeCurrentCandidate();
                        mDownload.appendLog("Download failed.  " + e.toString());
                        throw new IOException("Download failed.  " + e.toString());
                    }
                    mDownload.setStatus(DownloadFile.sError, e.getMessage());
                    Thread.sleep(5000);
                    mDownload.appendLog("Try push request.");
                    mHost = mManager.pushRequestTransfer(mHost, mDownload);
                    if (mHost == null) {
                        throw new IOException("Time out on requesting push transfer.");
                    }
                }
                if (mDownload.getStatus() == DownloadFile.sStopping || mDownload.getStatus() == DownloadFile.sRemoved) {
                    throw new IOException("Download stopped.");
                }
                String request = mDownload.getCurrentRemoteFile().getGetRequest() + "User-Agent: " + StrUtil.getAppNameVersion() + "\r\n" + mDownload.getRangeHeader((int) startPos) + "\r\n";
                mDownload.appendLog("Send download handshake: " + request);
                len = IOUtil.serializeString(request, buf, 0);
                {
                    mHost.getOs().write(buf, 0, len);
                }
                InputStream is = mHost.getIs();
                StringBuffer strBuf;
                String str;
                len = IOUtil.readToCRLF(is, buf, bufSize, 0);
                strBuf = new StringBuffer();
                IOUtil.deserializeString(buf, 0, len, strBuf);
                str = strBuf.toString().toUpperCase();
                mDownload.appendLog("Remote host replies: " + str);
                if (!str.startsWith("HTTP")) {
                    mDownload.removeCurrentCandidate();
                    throw new Exception("Invalid response from remote host.");
                } else if (str.indexOf("503") != -1) {
                    mDownload.setStatus(DownloadFile.HOST_BUSY);
                    mDownload.appendLog("Remote host is busy.");
                    continue;
                } else if (str.indexOf("404") != -1 || str.indexOf("410") != -1) {
                    mDownload.appendLog("File not available. Host removed.");
                    mDownload.removeCurrentCandidate();
                    continue;
                } else if ((str.indexOf("200 OK") == -1) && (str.indexOf("206 OK") == -1)) {
                    mDownload.removeCurrentCandidate();
                    throw new Exception("Negative response to download request." + str);
                }
                String server = null;
                boolean expectedHeader = false;
                while (true) {
                    len = IOUtil.readToCRLF(is, buf, bufSize, 0);
                    if (len == 0) break;
                    strBuf = new StringBuffer();
                    IOUtil.deserializeString(buf, 0, len, strBuf);
                    str = strBuf.toString().toUpperCase();
                    mDownload.appendLog(str);
                    if (str.startsWith("CONTENT-LENGTH:")) expectedHeader = true;
                    if (str.startsWith("CONTENT-RANGE:")) expectedHeader = true;
                    if (str.startsWith("SERVER:")) server = str.substring(7).trim();
                }
                mDownload.setRemoteAppName(server);
                if (!expectedHeader) {
                    mDownload.removeCurrentCandidate();
                    throw new Exception("Invalid header from remote host.");
                }
                if (startPos > 0 && !hasServerResumeSupport(server)) {
                    mDownload.removeCurrentCandidate();
                    throw new IOException("Candidate doesn't support resuming.");
                }
                int count = 0;
                mDownload.appendLog("Downloading...");
                mDownload.setStartSize(startPos);
                mDownload.setStartingTime(System.currentTimeMillis());
                mDownload.setStatus(DownloadFile.sDownloading);
                ThrottleController bandwidthThrottle = ThrottleController.acquireThrottle();
                try {
                    rFile.seek(lngStartPos);
                    while (count < mDownload.getTransferDataSize()) {
                        len = is.read(buf);
                        if (len == -1) break;
                        rFile.write(buf, 0, len);
                        count += len;
                        mDownload.setTransferredDataSize(count);
                        bandwidthThrottle.setRate(ServiceManager.sCfg.mDownloadMaxBandwidth / mManager.getDownloadingCount());
                        bandwidthThrottle.controlThrottle(len);
                    }
                } catch (Exception e2) {
                    if (count < mDownload.getTransferDataSize()) {
                        mDownload.appendLog("Transfered ended on exception with only " + count + " of " + mDownload.getTransferDataSize() + " bytes transfered!");
                        throw e2;
                    }
                } finally {
                    ThrottleController.releaseThrottle(bandwidthThrottle);
                    bandwidthThrottle = null;
                }
                if (count < mDownload.getTransferDataSize()) {
                    mDownload.appendLog("Transfered ended with only " + count + "/" + mDownload.getTransferDataSize() + " transfered!");
                    throw new IOException("Disconnected from remote host");
                }
                mDownload.setTransferredDataSize(count);
                mDownload.setStatus(DownloadFile.sCompleted);
                mDownload.setStoppingTime(System.currentTimeMillis());
                mDownload.appendLog("Download completed");
                StatisticTracker statTracker = ServiceManager.getStatisticTracker();
                statTracker.incStatDownloadCount(1);
            } catch (Exception e) {
                if (!(e instanceof IOException)) {
                    e.printStackTrace();
                }
                if (mDownload.getStatus() == DownloadFile.sStopping) {
                    mDownload.setStatus(DownloadFile.sStopped);
                    mDownload.setStoppingTime(System.currentTimeMillis());
                } else if (mDownload.getStatus() == DownloadFile.sRemoved) {
                } else {
                    mDownload.setStatus(DownloadFile.sError, e.getMessage());
                    mDownload.setStoppingTime(System.currentTimeMillis());
                    mDownload.appendLog("Error: " + e);
                }
                if (mHost == null) {
                    mDownload.removeCurrentCandidate();
                }
            } finally {
                mManager.decDownloadingCount();
                if (mHost != null) {
                    mHost.disconnect();
                    mHost = null;
                }
                if (rFile != null) {
                    try {
                        rFile.close();
                    } catch (IOException e) {
                    }
                    if (mDownload.getStatus() == DownloadFile.sCompleted) {
                        (new File(downloadFilename)).renameTo(new File(saveFilename));
                        mDownload.appendLog("Rename '" + downloadFilename + "' to '" + saveFilename + "'");
                    }
                }
                rFile = null;
                File f = new File(mDownload.getDownloadName());
                if (f.exists() && (mDownload.getStatus() == DownloadFile.sRemoved)) {
                    f.delete();
                }
                mDownload.setDownloadWorker(null);
                mDownload = null;
            }
        }
    }

    private boolean deleteExistingFile(String saveFilename) throws Exception {
        File file = new File(saveFilename);
        if (file.exists()) {
            int option = JOptionPane.showConfirmDialog(ServiceManager.getManager().getMainFrame(), "The file '" + saveFilename + "' exists already.  Overwrite it with the download file?", "Confirmation", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (option != JOptionPane.YES_OPTION) {
                return false;
            }
            if (file.delete() == false) {
                String msg = "Failed to delete the old file '";
                msg += saveFilename + ".'";
                JOptionPane.showMessageDialog(ServiceManager.getManager().getMainFrame(), msg, "Failed To Delete File", JOptionPane.ERROR_MESSAGE);
                throw new Exception("Failed To Delete File");
            }
            mDownload.appendLog("Old file deleted.");
        }
        return true;
    }

    /**
     * Limewire 1.3 dosn't support download resume!
     */
    private boolean hasServerResumeSupport(String server) {
        if (server == null) {
            return false;
        }
        int idxID = server.indexOf("LIMEWIRE");
        if (idxID != -1) {
            int idxDot = server.indexOf(".");
            if (idxDot == -1) {
                return false;
            }
        }
        return true;
    }
}
