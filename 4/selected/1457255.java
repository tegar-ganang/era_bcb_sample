package fetchExc;

import java.net.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.nio.channels.*;

class SendMbox implements MsgForwarder {

    private String fromSrv;

    private String mboxFile;

    SendMbox(String fromSrv, String mboxFile) {
        this.fromSrv = fromSrv;
        this.mboxFile = mboxFile;
    }

    private static final Format LONG_FORMAT = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z (z)");

    private static final Format SHORT_FORMAT = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");

    public boolean processMessage(String from, InputStream fwdMsg) {
        Date now = new Date();
        final String curDateTime = LONG_FORMAT.format(now);
        final String curDateTime2 = SHORT_FORMAT.format(now);
        String hostAddress;
        try {
            hostAddress = InetAddress.getByName(fromSrv).getHostAddress();
        } catch (Exception e) {
            hostAddress = "unknown";
        }
        final String recFrom = "Received: from " + fromSrv + " ([" + hostAddress + "]) by localhost with webDAV (fetchExc) for mbox (single-drop); " + curDateTime;
        BufferedOutputStream bos = null;
        FileLock lock;
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(mboxFile, true);
        } catch (IOException ioe) {
            System.err.println("Can't open file:" + mboxFile);
            return false;
        }
        try {
            lock = lockFile(fos.getChannel(), false);
        } catch (IOException ioe) {
            System.out.println("error in locking file:" + mboxFile);
            ioe.printStackTrace();
            return false;
        }
        bos = new BufferedOutputStream(fos);
        try {
            int data;
            int fromCnt = 0;
            boolean fromOn = false;
            boolean cr = false, lf = false;
            String fromQuote = "From ";
            int fromLen = fromQuote.length();
            String fromLine = "From " + from + "  " + curDateTime2;
            bos.write(fromLine.getBytes(), 0, fromLine.length());
            bos.write('\n');
            String returnPath = "Return-Path: <" + from + ">";
            bos.write(returnPath.getBytes(), 0, returnPath.length());
            bos.write('\n');
            bos.write(recFrom.getBytes(), 0, recFrom.length());
            bos.write('\n');
            while (true) {
                data = fwdMsg.read();
                if (data < 0) break;
                if (data == '\r' && fromOn == false) {
                    cr = true;
                    lf = false;
                    continue;
                }
                if (data == '\n' && fromOn == false) {
                    lf = true;
                    bos.write(data);
                    continue;
                }
                if (cr == true && lf == true && data == fromQuote.charAt(0)) {
                    cr = false;
                    lf = false;
                    fromOn = true;
                    fromCnt = 1;
                    continue;
                }
                if (fromOn == true && data == fromQuote.charAt(fromCnt)) {
                    if (fromCnt < fromLen - 1) {
                        fromCnt++;
                        continue;
                    } else {
                        bos.write('>');
                        bos.write(fromQuote.getBytes(), 0, fromLen);
                        fromOn = false;
                        continue;
                    }
                }
                if (fromOn == true) {
                    bos.write(fromQuote.getBytes(), 0, fromCnt);
                    fromOn = false;
                }
                cr = false;
                lf = false;
                if (data != '\r') bos.write(data);
            }
            bos.write('\n');
            bos.flush();
            lock.release();
            bos.close();
        } catch (IOException e) {
            System.out.println("Error in writing to mbox: " + e);
            e.printStackTrace();
            return false;
        } finally {
            try {
                lock.release();
            } catch (IOException ioe) {
            }
        }
        return true;
    }

    /**
	 * Tries to lock the file channel provided. We try a spin lock first and if
	 * that fails, we optionally fall back on a blocking lock.
	 * 
	 * @returns a FileLock for the file or null if no lock could be obtained.
	 */
    private FileLock lockFile(FileChannel lockChannel, boolean spinLockOnly) throws IOException {
        final int LOCK_COUNT = 5;
        FileLock lock = null;
        for (int i = 0; i < LOCK_COUNT; i++) {
            lock = lockChannel.tryLock();
            if (lock != null) {
                break;
            }
        }
        if (lock == null && !spinLockOnly) {
            lock = lockChannel.lock();
        }
        return lock;
    }
}
