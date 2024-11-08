package net.perham.jnap.net;

import java.net.*;
import java.io.*;
import java.util.*;
import net.perham.jnap.*;
import net.perham.jnap.cmd.*;
import net.perham.jnap.gui.IGui;
import net.perham.util.*;

public class UploadThread extends Thread implements ITransferRequest, INetworkEventListener {

    static int count = 0;

    boolean die = false;

    String remoteuser;

    String hostname;

    byte[] readBuf;

    Socket s;

    BufferedOutputStream bos;

    RandomAccessFile fis;

    File f;

    int speed = 0;

    long curBytes;

    long totalBytes;

    long startTime;

    String msg = null;

    String filename;

    int status;

    boolean allowed = true;

    public boolean isFinished() {
        return !isAlive();
    }

    public int getStatus() {
        return status;
    }

    public int getSpeed() {
        return speed;
    }

    public String getUsername() {
        return remoteuser;
    }

    UploadThread(String arg) {
        super("Upload-" + count++);
        INetwork jn = (INetwork) Main.getConfig().getObjectProperty("net");
        jn.addNetworkEventListener(this);
        QuotedStringTokenizer qst = new QuotedStringTokenizer(arg);
        remoteuser = qst.nextToken();
        filename = qst.nextToken();
        speed = Integer.parseInt(qst.nextToken());
        try {
            if (filename.indexOf("\\..\\") != -1) {
                Debug.log(Debug.UPLOAD, "SECURITY: Upload ERROR - Filename: " + filename);
                throw new SecurityException("Suspicious filename: '" + filename + "'");
            }
            String fname = filename.replace('\\', File.separatorChar);
            String dirName = Main.getConfig().getProperty("user.uploadDirectory");
            if (dirName == null) {
                Debug.log(Debug.UPLOAD, "SECURITY: No upload directory definied: Filename: " + fname);
                throw new SecurityException("Upload without directory: '" + fname + "'");
            }
            File dir = new File(dirName);
            try {
                dirName = dir.getCanonicalPath();
            } catch (IOException ex) {
                Debug.log(Debug.UPLOAD, "SECURITY: Can not access upload directory: " + dirName + " Filename: " + fname);
                throw new SecurityException("Upload directory not accessible: '" + dirName + "'");
            }
            if (!fname.toLowerCase().startsWith(dirName.toLowerCase())) {
                Debug.log(Debug.UPLOAD, "SECURITY: Upload ERROR - Dir: " + dirName + " Filename: " + fname);
                throw new SecurityException("Upload directory does not match: '" + fname + "'");
            }
            f = new File(fname);
            if (!f.exists()) {
                throw new SecurityException("Requested non-existent file: '" + fname + "'");
            }
            status = STARTING;
            totalBytes = f.length();
            curBytes = 0;
            readBuf = new byte[4096];
        } catch (SecurityException ex) {
            msg = ex.getMessage();
            status = FAILED;
            return;
        }
    }

    public String getFilename() {
        return filename;
    }

    public void cancel() throws InterruptedException {
        halt();
    }

    public void networkEvent(int cmd, String arg) {
        switch(cmd) {
            case INetwork.UPLOAD_CONNECT:
                {
                    if (!arg.startsWith(remoteuser)) {
                        return;
                    }
                    QuotedStringTokenizer st = new QuotedStringTokenizer(arg);
                    String host = st.nextToken();
                    String thefile = st.nextToken();
                    if (filename.equals(thefile)) {
                        Debug.log(Debug.UPLOAD, "remote host connected for upload");
                        JnapNetwork jn = (JnapNetwork) Main.getConfig().getObjectProperty("net");
                        curBytes = Integer.parseInt(st.nextToken());
                        s = jn.getListenThread().getAcceptedSocket();
                        if (s != null) {
                            jn.getListenThread().setAcceptedSocket(null);
                            Debug.log(Debug.UPLOAD, "Got our accepted socket from the listener thread");
                            currentUploadApprovedCommand.answer();
                            break;
                        }
                    }
                }
            case INetwork.UPLOAD_PUSH:
                {
                    if (s != null || !arg.startsWith(remoteuser)) {
                        return;
                    }
                    QuotedStringTokenizer st = new QuotedStringTokenizer(arg);
                    String username = st.nextToken();
                    String ip = st.nextToken();
                    String port = st.nextToken();
                    String file = st.nextToken();
                    if (!file.equals(filename)) {
                        return;
                    }
                    int p = Integer.parseInt(port);
                    long ipAddr = Long.parseLong(ip);
                    String hostname = (ipAddr & 0x000000FF) + "." + ((ipAddr & 0x0000FF00) >> 8) + "." + ((ipAddr & 0x00FF0000) >> 16) + "." + ((ipAddr & 0xFF000000) >>> 24);
                    try {
                        s = new Socket(hostname, p);
                        OutputStream os = s.getOutputStream();
                        InputStream is = s.getInputStream();
                        is.read();
                        os.write("SEND".getBytes());
                        os.flush();
                        os.write((Main.getConfig().getProperty(JnapConfig.USER_NAME) + " \"" + filename + "\" " + totalBytes).getBytes());
                        os.flush();
                        byte[] readBuf = new byte[20];
                        int amount = is.read(readBuf, 0, readBuf.length);
                        char chr = (char) readBuf[0];
                        if (!Character.isDigit(chr)) {
                            throw new IOException("Error from client: " + new String(readBuf, 0, amount + 1));
                        }
                        String sizeStr = new String(readBuf, 0, amount);
                        try {
                            curBytes = Integer.parseInt(sizeStr);
                        } catch (NumberFormatException nfe) {
                            throw new IOException("Error from client: " + sizeStr);
                        }
                        Debug.log(Debug.UPLOAD, "Created upload push socket to " + hostname + ":" + p);
                    } catch (IOException ex) {
                        currentException = ex;
                    } finally {
                        currentUploadApprovedCommand.answer();
                    }
                }
        }
    }

    public void run() {
        IGui jg = (IGui) Main.getConfig().getObjectProperty("gui");
        jg.notifyTransferStart(this);
        if (status != STARTING) return;
        try {
            sendFile();
        } catch (Exception ie) {
            if (msg == null) {
                msg = ie.getMessage();
            }
            status = FAILED;
            Debug.error("UploadThread exiting with error", ie);
        } finally {
            cleanup();
            Debug.log(Debug.UPLOAD, "UploadThread[" + filename + "] exiting with msg = " + msg);
        }
    }

    void halt() throws InterruptedException {
        msg = "Upload cancelled.";
        die = true;
        status = CANCELLED;
        Debug.log(Debug.SYNC, "(  " + Thread.currentThread().getName() + ": join(" + this + ")");
        interrupt();
        join();
        Debug.log(Debug.SYNC, ")  " + Thread.currentThread().getName() + ": join(" + this + ")");
    }

    void cleanup() {
        JnapNetwork jn = (JnapNetwork) Main.getConfig().getObjectProperty("net");
        jn.removeNetworkEventListener(this);
        TransferManager.getInstance().transferComplete(this);
        try {
            if (fis != null) fis.close();
        } catch (IOException ie) {
        }
        try {
            if (bos != null) bos.close();
        } catch (IOException ie) {
        }
        try {
            if (s != null) s.close();
        } catch (IOException ie) {
        }
    }

    IOException currentException;

    UploadApprovedCommand currentUploadApprovedCommand;

    void sendFile() throws IOException, InterruptedException {
        INetwork jn = (INetwork) Main.getConfig().getObjectProperty("net");
        currentUploadApprovedCommand = new UploadApprovedCommand(remoteuser, filename);
        jn.sendCommand(currentUploadApprovedCommand);
        if (!currentUploadApprovedCommand.sync(120000)) {
            msg = "Answer to UPLOAD timed out.";
            status = FAILED;
            return;
        }
        if (currentException != null) {
            throw currentException;
        }
        Debug.log(Debug.UPLOAD, "Starting upload.");
        jn.sendCommand(new UploadNotifyStartCommand());
        status = IN_TRANSIT;
        try {
            s.setSoTimeout(1000);
            bos = new BufferedOutputStream(s.getOutputStream());
            fis = new RandomAccessFile(f, "r");
            if (curBytes > 0) {
                fis.skipBytes((int) curBytes);
            }
            long bytes = totalBytes - curBytes;
            String b = String.valueOf(bytes);
            bos.write(b.getBytes());
            bos.flush();
            startTime = System.currentTimeMillis();
            while (curBytes < totalBytes) {
                if (die) return;
                int amount = 0;
                try {
                    amount = fis.read(readBuf);
                } catch (InterruptedIOException iie) {
                    continue;
                }
                curBytes += amount;
                bos.write(readBuf, 0, amount);
            }
            status = COMPLETE;
            msg = "Upload complete.";
        } finally {
            jn.sendCommand(new UploadNotifyFinishCommand());
        }
    }

    public String toString() {
        String text = "";
        if (status == IN_TRANSIT) {
            long tot = getTotalBytes();
            long cur = getCurrentBytes();
            if (tot != 0 && cur != 0) {
                float secs = (System.currentTimeMillis() - getStartTime()) / 1000;
                text = (cur / 1024) + "k / " + (tot / 1024) + "k";
                text += "   (";
                String percent = ((((float) cur / tot) * 100) + "");
                if (percent.length() > 5) percent = percent.substring(0, 4);
                text += percent + "%)" + "   ";
                String kps = ((((float) (cur / 1024)) / secs) + "");
                if (kps.length() > 5) kps = kps.substring(0, 5);
                text += kps + " k/sec";
            }
        } else {
            text = STATUS_STRING[status];
        }
        return text;
    }

    long getCurrentBytes() {
        return curBytes;
    }

    long getTotalBytes() {
        return totalBytes;
    }

    long getStartTime() {
        return startTime;
    }

    String getDeathMessage() {
        return msg;
    }
}
