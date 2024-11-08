package net.perham.jnap.net;

import java.net.*;
import java.io.*;
import java.util.*;
import net.perham.jnap.*;
import net.perham.jnap.cmd.*;
import net.perham.util.*;

public class DownloadThread extends Thread implements INetworkEventListener {

    class RemoteQueueFullException extends IOException {

        public RemoteQueueFullException() {
            super("");
        }
    }

    public static final int MAX_TRIES = 3;

    /**
	 * Napster has a bug in it where it will sometimes close the
	 * socket before we have gotten the final few bytes.  We'll
	 * check for this condition on download errors so the user
	 * does not get unnecessarily concerned.
	 */
    static final int BYTE_ERROR_MARGIN = 2048;

    static int count = 0;

    String remoteuser;

    String hostname;

    int port;

    long curSize;

    long startSize = 0;

    byte[] readBuf;

    DownloadRequest dr;

    Socket s;

    BufferedInputStream bis;

    RandomAccessFile raf;

    long curBytes;

    long totalBytes;

    long startTime;

    String msg = null;

    String filename;

    boolean isInterrupted_WORKAROUND_FOR_BUG_IN_IBM_JDK = false;

    long getTotalBytes() {
        return totalBytes;
    }

    long getCurrentBytes() {
        return curBytes;
    }

    long getStartTime() {
        return startTime;
    }

    String getDeathMessage() {
        return msg;
    }

    String getFilename() {
        return filename;
    }

    DownloadRequest getRequest() {
        return dr;
    }

    DownloadThread(DownloadRequest dr) {
        super("Download-" + count++);
        Debug.log(Debug.DOWNLOAD, dr.getSearchResult().toString());
        remoteuser = dr.getSearchResult().getUser();
        filename = dr.getSearchResult().getTitle();
        this.dr = dr;
        totalBytes = 0;
        curBytes = 0;
        readBuf = new byte[4096];
        INetwork jn = (INetwork) Main.getConfig().getObjectProperty("net");
        jn.addNetworkEventListener(this);
    }

    /**
	 * Our Napster network event handler
	 *
	 * The only events we care about are the
	 * responses to our initial download queries
	 */
    public void networkEvent(int cmd, String arg) {
        switch(cmd) {
            case INetwork.REMOTE_QUEUE_FULL:
                {
                    if (!arg.startsWith(remoteuser)) {
                        return;
                    }
                    QuotedStringTokenizer st = new QuotedStringTokenizer(arg);
                    String username = st.nextToken();
                    if (!username.equals(remoteuser)) {
                        return;
                    }
                    String thefilename = st.nextToken();
                    if (!thefilename.equals(filename)) {
                        return;
                    }
                    setException(new RemoteQueueFullException());
                    currentCommand.answer();
                    break;
                }
            case INetwork.DOWNLOAD_INFO:
                {
                    if (!arg.startsWith(remoteuser)) {
                        return;
                    }
                    QuotedStringTokenizer st = new QuotedStringTokenizer(arg);
                    String username = st.nextToken();
                    long ipAddr = Long.parseLong(st.nextToken());
                    port = Integer.parseInt(st.nextToken());
                    String theName = st.nextToken();
                    if (!theName.equals(filename)) return;
                    hostname = (ipAddr & 0x000000FF) + "." + ((ipAddr & 0x0000FF00) >> 8) + "." + ((ipAddr & 0x00FF0000) >> 16) + "." + ((ipAddr & 0xFF000000) >>> 24);
                    currentCommand.answer();
                    break;
                }
            case INetwork.REVERSE_DOWNLOAD_CONNECT:
                {
                    if (!arg.startsWith(remoteuser)) {
                        return;
                    }
                    QuotedStringTokenizer st = new QuotedStringTokenizer(arg);
                    String host = st.nextToken();
                    if (remoteuser.equals(host)) {
                        String thefile = st.nextToken();
                        if (filename.equals(thefile)) {
                            Debug.log(Debug.DOWNLOAD, "REVERSE_DOWNLOAD_CONNECT: " + arg);
                            totalBytes = Integer.parseInt(st.nextToken());
                            JnapNetwork jn = (JnapNetwork) Main.getConfig().getObjectProperty("net");
                            s = jn.getListenThread().getAcceptedSocket();
                            if (s != null) {
                                jn.getListenThread().setAcceptedSocket(null);
                                Debug.log(Debug.DOWNLOAD, "Got our accepted socket from the listener thread");
                                currentCommand.answer();
                                break;
                            } else {
                                Debug.log(Debug.DOWNLOAD, "Listen socket is NULL?!?!");
                            }
                        }
                    }
                }
            case INetwork.DOWNLOAD_ERROR:
                {
                    if (!arg.startsWith(remoteuser)) {
                        return;
                    }
                    QuotedStringTokenizer st = new QuotedStringTokenizer(arg);
                    String username = st.nextToken();
                    if (!username.equals(remoteuser)) {
                        return;
                    }
                    String thefilename = st.nextToken();
                    if (!thefilename.equals(filename)) {
                        return;
                    }
                    dr.setStatus(ITransferRequest.ERROR);
                    msg = "File not available";
                    port = -1;
                    setException(new IOException("File not available"));
                    currentCommand.answer();
                    break;
                }
            default:
                return;
        }
    }

    public void run() {
        INetwork jn = (INetwork) Main.getConfig().getObjectProperty("net");
        jn.sendCommand(new DownloadNotifyStartCommand());
        boolean requestProcessed = true;
        try {
            Exception problem = null;
            for (int retryCnt = 0; retryCnt < MAX_TRIES; retryCnt++) {
                try {
                    getFile();
                    Debug.log(Debug.DOWNLOAD_CONTROL, "download successful");
                    return;
                } catch (InterruptedException ex) {
                    Debug.log(Debug.DOWNLOAD_CONTROL, "download interrupted");
                    dr.setStatus(DownloadRequest.CANCELLED);
                    msg = "Interrupted IO.";
                    throw ex;
                } catch (ConnectException ex) {
                    throw ex;
                } catch (NoRouteToHostException ex) {
                    throw ex;
                } catch (SocketException ex) {
                    problem = ex;
                    Debug.error(Debug.DOWNLOAD_CONTROL, "socket problem", ex);
                } catch (RemoteQueueFullException ex) {
                    requestProcessed = false;
                    dr.getGroup().delayRequest(dr);
                    return;
                }
                Debug.log(Debug.DOWNLOAD_CONTROL, "retrying download");
            }
            Debug.log(Debug.DOWNLOAD_CONTROL, "last retry reached");
            throw problem;
        } catch (Exception ex) {
            if (msg == null) {
                msg = ex.getMessage();
            }
            Debug.error(msg, ex);
            dr.setStatus(ITransferRequest.ERROR);
        } finally {
            jn.sendCommand(new DownloadNotifyFinishCommand());
            cleanup();
            Debug.log(Debug.DOWNLOAD, "DownloadThread[" + filename + "] exiting with msg = " + msg);
            dr.getGroup().notify(dr);
            if (requestProcessed) {
                dr.getGroup().finishRequest(dr);
            }
        }
    }

    void halt() throws InterruptedException {
        Debug.log(Debug.SYNC, "(  " + Thread.currentThread().getName() + ": join(" + this + ")");
        isInterrupted_WORKAROUND_FOR_BUG_IN_IBM_JDK = true;
        interrupt();
        join();
        Debug.log(Debug.SYNC, ")  " + Thread.currentThread().getName() + ": join(" + this + ")");
        msg = "Cancelled by user.";
    }

    void cleanup() {
        INetwork jn = (INetwork) Main.getConfig().getObjectProperty("net");
        jn.removeNetworkEventListener(this);
        if (dr.getStatus() == ITransferRequest.STARTING) dr.setStatus(ITransferRequest.ENQUEUED);
        try {
            if (raf != null) raf.close();
        } catch (IOException ie) {
        }
        try {
            if (bis != null) bis.close();
        } catch (IOException ie) {
        }
        try {
            if (s != null) s.close();
        } catch (IOException ie) {
        }
    }

    void setupReverseConnection() throws IOException {
        InputStream is = s.getInputStream();
        OutputStream os = s.getOutputStream();
        bis = new BufferedInputStream(is);
        openLocalFile();
        os.write((curSize + "").getBytes());
        os.flush();
    }

    void retrieveFile() throws IOException, InterruptedException {
        s.setSoTimeout(1000);
        startTime = System.currentTimeMillis();
        if (curSize != 0) {
            curBytes += curSize;
        }
        Debug.log(Debug.DOWNLOAD, "Total size = " + totalBytes + ", cur size = " + curBytes);
        dr.setStatus(DownloadRequest.IN_TRANSIT);
        while (curBytes < totalBytes) {
            if (isInterrupted()) {
                dr.setStatus(DownloadRequest.CANCELLED);
                msg = "Interrupted.";
                return;
            }
            int amount = 0;
            try {
                amount = bis.read(readBuf);
            } catch (InterruptedIOException iie) {
                Debug.log(Debug.DOWNLOAD_CONTROL, "io interrupt (read)");
                if (isInterrupted()) {
                    Debug.log(Debug.DOWNLOAD_CONTROL, "thread was interrupted");
                    throw new InterruptedException();
                }
                amount = iie.bytesTransferred;
                if (amount > 0) {
                    curBytes += amount;
                    raf.write(readBuf, 0, amount);
                }
                if (curBytes + BYTE_ERROR_MARGIN > totalBytes) {
                    Debug.log(Debug.DOWNLOAD_CONTROL, "download complete anyway");
                    break;
                }
                continue;
            } catch (SocketException ex) {
                if (isInterrupted_WORKAROUND_FOR_BUG_IN_IBM_JDK) {
                    Debug.log(Debug.DOWNLOAD_CONTROL, "IBM-JDK: thread was interrupted");
                    throw new InterruptedException();
                }
                if (curBytes + BYTE_ERROR_MARGIN > totalBytes) {
                    break;
                }
                throw ex;
            } catch (IOException iie) {
                if (curBytes + BYTE_ERROR_MARGIN > totalBytes) {
                    break;
                }
                throw iie;
            } catch (Exception se) {
                if (curBytes + BYTE_ERROR_MARGIN > totalBytes) {
                    break;
                }
                throw new IOException(se.getMessage());
            }
            if (amount > 0) {
                curBytes += amount;
                try {
                    raf.write(readBuf, 0, amount);
                } catch (InterruptedIOException iie) {
                    Debug.log(Debug.DOWNLOAD_CONTROL, "io interrupt (write)");
                    if (isInterrupted()) {
                        Debug.log(Debug.DOWNLOAD_CONTROL, "thread was interrupted");
                        throw new InterruptedException();
                    }
                }
            } else if (amount < 0) {
                if (curBytes + BYTE_ERROR_MARGIN > totalBytes) {
                    break;
                }
                throw new EOFException("Premature end of data");
            }
        }
        dr.setStatus(DownloadRequest.COMPLETE);
        msg = "Download complete.";
    }

    void openLocalFile() throws IOException {
        raf = new RandomAccessFile(dr.getFile(), "rw");
        curSize = raf.length();
        raf.skipBytes((int) curSize);
        startSize = curSize;
    }

    BaseCommand currentCommand;

    private IOException currentException;

    private void setException(IOException currentException) {
        this.currentException = currentException;
    }

    private void getAnswer() throws IOException {
        if (currentException != null) {
            IOException ex = currentException;
            currentException = null;
            throw ex;
        }
    }

    /**
	 * Make the download request and figure out which way we are going
	 * to do the download - old way (we connect to remote host) or the new
	 * way (remote host connects to us).
	 */
    void getFile() throws IOException, InterruptedException {
        boolean behindFirewall = Main.getConfig().getProperty(JnapConfig.LISTEN_PORT).equals("0");
        INetwork n = (INetwork) Main.getConfig().getObjectProperty("net");
        currentCommand = new DownloadCommand(remoteuser, filename);
        try {
            n.sendCommand(currentCommand);
            currentCommand.sync();
            getAnswer();
        } finally {
            currentCommand = null;
        }
        if (port == 0) {
            if (behindFirewall) {
                throw new IOException("No download from firewalled client");
            }
            doReverseDownload();
        } else if (port > 0) {
            if (behindFirewall) {
                try {
                    doStraightDownload();
                } catch (NoRouteToHostException ex) {
                    n.sendCommand(new PortErrorCommand(remoteuser));
                    throw ex;
                }
            } else {
                doReverseDownload();
            }
        }
    }

    /**
	 * We know that the network event handler grabbed the
	 * accepted socket from the ListenThread so we just need
	 * to read in the header and suck down the data.
	 */
    void doReverseDownload() throws IOException, InterruptedException {
        INetwork n = (INetwork) Main.getConfig().getObjectProperty("net");
        boolean answered;
        currentCommand = new ReverseDownloadCommand(remoteuser, filename);
        try {
            n.sendCommand(currentCommand);
            answered = currentCommand.sync(45000);
        } finally {
            currentCommand = null;
        }
        if (!answered) {
            msg = "No connect while reverse downloading.";
            throw new IOException(msg);
        } else {
            getAnswer();
            dr.getGroup().notify(dr);
            setupReverseConnection();
            retrieveFile();
        }
    }

    void doStraightDownload() throws IOException, UnknownHostException, InterruptedException {
        Debug.log(Debug.DOWNLOAD, "doStraightDownload: open connection: " + hostname + ":" + port);
        s = new Socket(hostname, port);
        InputStream is = s.getInputStream();
        OutputStream os = s.getOutputStream();
        bis = new BufferedInputStream(is);
        BufferedOutputStream bos = new BufferedOutputStream(os);
        Debug.log(Debug.DOWNLOAD, "doStraightDownload: connected");
        dr.getGroup().notify(dr);
        openLocalFile();
        bis.read(readBuf, 0, 1);
        Debug.log(Debug.DOWNLOAD, "doStraightDownload: hello byte received: '" + (char) (((int) readBuf[0]) & 0xFF) + "'");
        bos.write("GET".getBytes());
        bos.flush();
        bos.write((Main.getConfig().getProperty(JnapConfig.USER_NAME) + " \"" + filename + "\" " + curSize).getBytes());
        bos.flush();
        Debug.log(Debug.DOWNLOAD, "doStraightDownload: request sent.");
        int offset = 0;
        int direct = bis.read(readBuf, offset, 1);
        if (direct == -1) {
            throw new EOFException();
        }
        char chr = (char) readBuf[offset];
        if (!Character.isDigit(chr)) {
            int amount = bis.read(readBuf, 1, readBuf.length - 1);
            System.out.println("Error from client: " + new String(readBuf, 0, amount + 1));
            dr.setStatus(DownloadRequest.ERROR);
        } else {
            offset++;
            while (offset < readBuf.length) {
                bis.read(readBuf, offset, 1);
                chr = (char) readBuf[offset];
                if (!Character.isDigit(chr)) break;
                offset++;
            }
        }
        String sizeStr = new String(readBuf, 0, offset);
        try {
            totalBytes = Integer.parseInt(sizeStr);
        } catch (NumberFormatException nfe) {
            System.out.println("Error from client: " + sizeStr);
            return;
        }
        raf.write(chr);
        retrieveFile();
    }

    public String toString() {
        String text;
        if (isAlive()) {
            long tot = getTotalBytes();
            long cur = getCurrentBytes();
            if (tot != 0 && cur != 0) {
                float secs = (System.currentTimeMillis() - getStartTime()) / 1000;
                text = (cur / 1024) + "k / " + (tot / 1024) + "k";
                text += "   (";
                String percent = ((((float) cur / tot) * 100) + "");
                if (percent.length() > 5) percent = percent.substring(0, 4);
                text += percent + "%)" + "   ";
                String kps = ((((float) ((cur - startSize) / 1024)) / secs) + "");
                if (kps.length() > 5) kps = kps.substring(0, 5);
                text += kps + " k/sec";
            } else {
                text = "Starting transfer";
            }
        } else {
            text = getDeathMessage();
        }
        return text;
    }
}
