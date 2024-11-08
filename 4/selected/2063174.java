package com.ipc.jPipes.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ResourceBundle;
import org.apache.log4j.Logger;
import com.ipc.jPipes.Exception.FileIOException;
import com.ipc.jPipes.commons.jPipeConstants;
import com.ipc.jPipes.commons.jPipeConstants.MessageConstants;

/**
 * The Class SynchronizationUtils. Acquire Lock and Release Lock on the file.
 * 
 * @author Sumedh Sakdeo
 */
public class SynchronizationUtils {

    private RandomAccessFile raf = null;

    private FileLock fLock = null;

    private FileChannel fChannel = null;

    private ResourceBundle resBundle = ResourceBundle.getBundle(jPipeConstants.ERROR_MESSAGES_RESOURCE);

    protected static Logger log = Logger.getLogger(SynchronizationUtils.class);

    /**
	 * Acquire lock.
	 * 
	 * @param pipeName
	 *            the pipe name
	 * 
	 * @throws FileIOException
	 *             the file io exception
	 */
    public void acquireLock(String pipeName) throws FileIOException {
        try {
            raf = new RandomAccessFile(new File(pipeName), "rwd");
            fChannel = raf.getChannel();
        } catch (FileNotFoundException e) {
            log.error(resBundle.getString(MessageConstants.JPipeNotFound.getMessageKey()), e);
            throw new FileIOException(MessageConstants.JPipeNotFound);
        }
        try {
            fLock = fChannel.tryLock();
        } catch (IOException e1) {
        }
        int tryOut = 0;
        while (fLock == null && tryOut < jPipeConstants.TIME_OUT) {
            try {
                fLock = fChannel.tryLock();
            } catch (IOException e) {
                log.error("Exception in acquiring lock", e);
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
            }
            tryOut++;
        }
        if (fLock != null) {
            log.info(resBundle.getString(MessageConstants.JPipeLockAcquired.getMessageKey()) + pipeName);
            return;
        }
        throw new FileIOException(MessageConstants.JPipeAcquireLockFail);
    }

    /**
	 * Release lock.
	 * 
	 * @param pipeName
	 *            the pipe name
	 * 
	 * @throws FileIOException
	 *             the file io exception
	 */
    public void releaseLock(String pipeName) throws FileIOException {
        try {
            if (fLock != null) {
                fLock.release();
            }
            if (raf != null) {
                raf.close();
            }
            if (fChannel != null) {
                fChannel.close();
            }
        } catch (IOException e) {
            log.error(resBundle.getString(MessageConstants.JPipeReleaseLockFail.getMessageKey()), e);
            throw new FileIOException(MessageConstants.JPipeReleaseLockFail);
        }
    }

    /**
	 * Gets the random access file Object.
	 * 
	 * @return the random access file
	 */
    public RandomAccessFile getRandomAccessFile() {
        return raf;
    }
}
