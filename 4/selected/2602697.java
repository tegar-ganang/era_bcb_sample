package org.jsmtpd.plugins.deliveryServices;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsmtpd.core.common.PluginInitException;
import org.jsmtpd.core.common.delivery.FatalDeliveryException;
import org.jsmtpd.core.common.delivery.IDeliveryService;
import org.jsmtpd.core.common.delivery.TemporaryDeliveryException;
import org.jsmtpd.core.common.io.InvalidStreamParserInitialisation;
import org.jsmtpd.core.common.io.dataStream.DataStreamParser;
import org.jsmtpd.core.mail.Email;
import org.jsmtpd.core.mail.EmailAddress;
import org.jsmtpd.core.mail.Rcpt;
import org.jsmtpd.tools.ByteArrayTool;
import org.jsmtpd.tools.DateUtil;
import org.jsmtpd.tools.rights.IChown;
import org.jsmtpd.tools.rights.RightException;
import org.jsmtpd.tools.rights.UnixChown;

/**
 * Re implemented (23/07/2005), should work as first intended
 * Multiple write against different files
 * Now uses FileLock + a Map to maintain lock states
 * @author Jean-Francois POUX
 */
public class UnixMailboxWriter implements IDeliveryService {

    private Log log = LogFactory.getLog(UnixMailboxWriter.class);

    private String mailboxDir = null;

    private Map<String, FileLock> locks = Collections.synchronizedMap(new HashMap<String, FileLock>());

    private boolean tryChown = false;

    private IChown chown = new UnixChown();

    public void setTryChown(boolean tryChown) {
        this.tryChown = tryChown;
    }

    public void doDelivery(Email in, List<Rcpt> rcpts) {
        log.debug("Begin  ");
        for (Rcpt rcpt : rcpts) {
            try {
                doSingleDelivery(in, rcpt.getEmailAddress());
                rcpt.setDelivered(Rcpt.STATUS_DELIVERED);
            } catch (FatalDeliveryException e) {
                rcpt.setDelivered(Rcpt.STATUS_ERROR_FATAL);
            } catch (TemporaryDeliveryException e) {
                rcpt.setDelivered(Rcpt.STATUS_ERROR_NOT_FATAL);
            }
        }
        log.debug("end");
    }

    /**
     * Delivers to one box
     * @param in the message
     * @param rcpt one recipient
     * @throws FatalDeliveryException
     * @throws TemporaryDeliveryException
     */
    public void doSingleDelivery(Email in, EmailAddress rcpt) throws FatalDeliveryException, TemporaryDeliveryException {
        RandomAccessFile fp = null;
        String mailbox = mailboxDir + "/" + rcpt.getUser().toLowerCase();
        boolean exist = new File(mailbox).exists();
        try {
            fp = openMBox(mailbox);
            log.debug("Locked mailbox: " + rcpt.getUser());
            fp.seek(fp.length());
            try {
                DataStreamParser dsp = new DataStreamParser(512, 512);
                dsp.appendString("From " + in.getFrom().toString() + " " + DateUtil.currentMailboxDate());
                fp.write(ByteArrayTool.replaceBytes(dsp.getData(), ByteArrayTool.CRLF, ByteArrayTool.LF));
            } catch (InvalidStreamParserInitialisation e1) {
            }
            byte[] pattern = { '\n', 'F', 'r', 'o', 'm', ' ' };
            byte[] replace = { '\n', '>', 'F', 'r', 'o', 'm', ' ' };
            byte[] tempo = ByteArrayTool.replaceBytes(in.getDataAsByte(), ByteArrayTool.CRLF, ByteArrayTool.LF);
            tempo = ByteArrayTool.replaceBytes(tempo, pattern, replace);
            fp.write(tempo);
            byte[] lf = new byte[1];
            lf[0] = 10;
            fp.write(lf);
            if ((!exist) && tryChown) chown.chown(mailbox, rcpt.getUser().toLowerCase());
            log.debug("Mail " + in.getDiskName() + " delivered to : " + rcpt.getUser());
        } catch (TemporaryDeliveryException tde) {
            log.info("Could not open/lock local mailbox: " + rcpt.getUser() + ", delivery delayed");
            throw tde;
        } catch (IOException e) {
            log.info("IO Error delivering mail from " + in.getFrom().toString() + " to mailbox " + rcpt.toString() + ", mail to this rcpt is lost", e);
            throw new FatalDeliveryException();
        } catch (RightException e) {
            log.error("Things are written, but I could not perfom chown on file ...", e);
        } finally {
            closeMBox(mailbox, fp);
        }
    }

    private void goSleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
    }

    public String getPluginName() {
        return "UnixMailboxWriter plugin for jsmtpd";
    }

    public void initPlugin() throws PluginInitException {
    }

    public void shutdownPlugin() {
    }

    public void setMailDir(String mailboxesDirectory) {
        this.mailboxDir = mailboxesDirectory;
    }

    /**
     * Opens the RandomAccesFile of the mailbox, and ensures its locked system wide (the file lock) and in the vm (in the hashmap)
     * @param fileName name of the mailbox (full path)
     * @return 
     * @throws TemporaryDeliveryException if the mbox is no lockable
     */
    private synchronized RandomAccessFile openMBox(String fileName) throws TemporaryDeliveryException {
        RandomAccessFile target = null;
        FileLock lock = null;
        if (locks.containsKey(fileName)) throw new TemporaryDeliveryException(); else {
            locks.put(fileName, null);
            try {
                target = getRandomAccessFile(fileName);
                lock = getLock(target.getChannel(), fileName);
                if (!lock.isValid()) {
                    throw new TemporaryDeliveryException();
                }
                locks.put(fileName, lock);
            } catch (TemporaryDeliveryException e) {
                locks.remove(fileName);
                throw e;
            }
        }
        return target;
    }

    /**
     * Tries to acquire a lock on the given channel
     * @param channel
     * @param mbox
     * @return
     * @throws TemporaryDeliveryException after 10 failed tries
     */
    private FileLock getLock(FileChannel channel, String mbox) throws TemporaryDeliveryException {
        FileLock lock = null;
        int tryCount = 0;
        while (lock == null) {
            try {
                lock = channel.tryLock();
            } catch (IOException e) {
                lock = null;
            }
            if (lock == null) {
                log.debug("Can't lock mailbox:" + mbox + "  retrying");
                tryCount++;
                goSleep();
            }
            if (tryCount > 10) throw new TemporaryDeliveryException();
        }
        return lock;
    }

    private RandomAccessFile getRandomAccessFile(String mailbox) throws TemporaryDeliveryException {
        RandomAccessFile fp = null;
        int tryCount = 0;
        while (fp == null) {
            if (tryCount > 10) throw new TemporaryDeliveryException();
            try {
                tryCount++;
                fp = new RandomAccessFile(mailbox, "rw");
            } catch (FileNotFoundException fileNotFoundException) {
                log.debug("Can't open mailbox: " + mailbox + "retrying");
            }
            if (fp == null) goSleep();
        }
        return fp;
    }

    private synchronized void closeMBox(String fileName, RandomAccessFile fp) {
        if (locks.containsKey(fileName)) {
            FileLock lock = locks.get(fileName);
            locks.remove(fileName);
            if (lock != null) {
                try {
                    lock.release();
                    log.debug("UNlocked mailbox: " + fileName);
                } catch (IOException e) {
                    log.error("Error Unlocking mailbox: " + fileName, e);
                }
            } else log.error("Lock is not a file lock ?! " + lock);
        }
        if (fp != null) {
            try {
                fp.close();
            } catch (IOException e) {
                log.error("Error closing File ", e);
            }
        }
    }
}
