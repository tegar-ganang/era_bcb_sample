package ftraq.fs;

import ftraq.fs.exceptions.*;
import ftraq.gui.UiOutputConsole;

/**
 * Simple Implementation of LgFileTransferThread
 * 
 * @author <a href="mailto:jssauder@tfh-berlin.de">Steffen Sauder</a>
 * @version 1.0 *
 */
public class LgFileTransferThreadImpl extends Thread implements LgFileTransferThread {

    private static ftraq.util.Logger logger = ftraq.util.Logger.getInstance(LgFileTransferThreadImpl.class);

    /** the source file to read from */
    private LgFile sourceFile;

    /** the target file to write to */
    private LgFile targetFile;

    /** the InputStream to read bytes from the source file */
    private InputStreamWithReadLine inputStream;

    /** the OutputStream to write bytes to the source file */
    private OutputStreamWithWriteLine outputStream;

    /** maximum number of bytes to read into memory before writing them again */
    private int maxBufferSize = 1000;

    /** the long value of the time when the transfer was started, or -1 if it wasn't started yet */
    private long timeWhenTransferWasStarted;

    /** the long value of the time when the thread observers were notified */
    private long timeOfLastStatusUpdateInMillis;

    /** the long value of the time when the transfer was finished, or -1 if it hasn't finished yet */
    private long timeWhenTransferWasFinished;

    /** how many bytes have already been transferred */
    private long numberOfBytesTransferred = 0;

    /** how many bytes are there to transfer */
    private long numberOfTotalBytes = 0;

    /** do we know the size of the file to transfer? */
    private boolean sourceFileSizeKnown = false;

    /** the Thread was inizialized but not yet started */
    private static final int STATUS_NOT_STARTED = 0;

    /** the Thread is trying to connect to source and target files */
    private static final int STATUS_PREPARING = 1;

    /** the Thread is transfering */
    private static final int STATUS_TRANSFERRING = 2;

    /** the Thread has succesfully finished transferring */
    private static final int STATUS_SUCCESFULLY_TRANSFERRED = 3;

    /** the Transfer failed */
    private static final int STATUS_TRANSFER_FAILED = 4;

    /** the current state of the transfer */
    private int transferStatus = LgFileTransferThreadImpl.STATUS_NOT_STARTED;

    /** the transfer was cancelled */
    private static final int STATUS_TRANSFER_CANCELLED = 5;

    /** should the transfer be cancelled? */
    private boolean cancelTransferRequested = false;

    /** a string describing the transfer threads state */
    private String statusString = new String();

    /** a set of observers to notify when a state change occurs */
    private java.util.Set threadObserverSet = new java.util.HashSet(2);

    /** the LgTransferQueueItem this Thread belongs to */
    private ftraq.transferqueue.LgTransferQueueItem queueItem;

    /** true if the file should be transferred in ascii mode (line by line) */
    private boolean asciiMode = false;

    /** true if the transfer is a resume of a previously aborted transfer */
    private boolean resumeMode = false;

    /** the position of the first byte to read when resuming a transfer */
    private long firstByteToRead = -1;

    /**
     * use this constructor if you want to create a TransferThread for a complete Transfer
     */
    public LgFileTransferThreadImpl(LgFileTransferThreadObserver i_observer, LgFile i_sourceFile, LgFile i_targetFile, boolean i_asciiMode) {
        logger.info("created file transfer thread for transfer of " + i_sourceFile.getURL() + " to " + i_targetFile.getURL());
        this.addFileTransferThreadObserver(i_observer);
        this.sourceFile = i_sourceFile;
        this.targetFile = i_targetFile;
        this.asciiMode = i_asciiMode;
    }

    /**
     * use this constructor if you want to create a TransferThread for resuming a Transfer
     */
    public LgFileTransferThreadImpl(LgFileTransferThreadObserver i_observer, LgFile i_sourceFile, LgFile i_targetFile, long i_firstByteToRead, boolean i_asciiMode) {
        this(i_observer, i_sourceFile, i_targetFile, i_asciiMode);
        this.resumeMode = true;
        this.firstByteToRead = i_firstByteToRead;
    }

    public LgFile getSourceFile() {
        return this.sourceFile;
    }

    public LgFile getTargetFile() {
        return this.targetFile;
    }

    public void startTransfer() {
        logger.info("starting transfer of " + this.sourceFile.getURL() + " to " + this.targetFile.getURL());
        try {
            Thread.sleep(500);
        } catch (InterruptedException ex) {
        }
        try {
            this.transferStatus = this.STATUS_PREPARING;
            this.connectToSourceFile(this.asciiMode);
            this.connectToTargetFile(this.asciiMode);
            this.transferStatus = this.STATUS_TRANSFERRING;
            this.doBinaryTransfer();
            try {
                if (this.inputStream != null) {
                    this.inputStream.close();
                }
                if (this.outputStream != null) {
                    this.outputStream.close();
                }
            } catch (java.io.IOException ex1) {
                logger.warn("IOException while closing streams", ex1);
                ex1.printStackTrace();
            }
            this.transferStatus = this.STATUS_SUCCESFULLY_TRANSFERRED;
            this.notifyTransferSucceded();
            float seconds = (float) this.getTransferDuration() / 1000;
            float kiloBytesPerSecond = (float) (this.getTransferredBytes() / 1024) / ((float) this.getTransferDuration() / 1000);
            logger.info(this.getTransferredBytes() + " bytes transferred in " + seconds + " seconds at " + kiloBytesPerSecond + " KByte/s");
            logger.info("finished transfer of " + this.sourceFile.getName() + " from " + this.sourceFile.getFileSystem() + " to " + this.targetFile.getFileSystem());
            this.targetFile.getParentDirectory().addChildDirectoryEntries(Lg_ListImpl.createLgDirectoryEntry_List(this.targetFile));
        } catch (Exception ex) {
            if (ex instanceof TransferCancelledExc) {
                this.transferStatus = this.STATUS_TRANSFER_CANCELLED;
                this.transferStatus = this.STATUS_TRANSFER_CANCELLED;
                String logString = "the transfer of " + this.sourceFile.getName() + " from " + this.sourceFile.getFileSystem() + " to " + this.targetFile.getFileSystem() + " was cancelled: \n" + ((TransferCancelledExc) ex).getMessages();
                logger.info(logString);
                this.notifyTransferCancelled();
                return;
            } else {
                try {
                    if (this.inputStream != null) {
                        this.inputStream.close();
                    }
                    if (this.outputStream != null) {
                        this.outputStream.close();
                    }
                } catch (java.io.IOException ex1) {
                    logger.warn("IOException while closing streams in the exception handler!", ex1);
                    ex1.printStackTrace();
                }
            }
            this.transferStatus = this.STATUS_TRANSFER_FAILED;
            this.notifyTransferFailed(ex);
        }
    }

    public void stopTransfer() {
        logger.info("received stop request for transfer of " + this.sourceFile.getURL() + " to " + this.targetFile.getURL());
        this.cancelTransferRequested = true;
    }

    public long getTotalBytes() throws InformationNotAvailableExc {
        if (this.sourceFileSizeKnown == false) {
            throw new InformationNotAvailableExc(this.sourceFile, "no size information available for this file");
        }
        return this.numberOfTotalBytes;
    }

    public long getTransferredBytes() {
        return this.numberOfBytesTransferred;
    }

    public long getTransferDuration() {
        if (this.transferStatus == LgFileTransferThreadImpl.STATUS_NOT_STARTED || this.transferStatus == LgFileTransferThreadImpl.STATUS_PREPARING) {
            return 0;
        }
        if (this.transferStatus == LgFileTransferThreadImpl.STATUS_TRANSFERRING) {
            return new java.util.Date().getTime() - this.timeWhenTransferWasStarted;
        }
        return this.timeWhenTransferWasFinished - this.timeWhenTransferWasStarted;
    }

    public String getStatusString() {
        return this.statusString;
    }

    public boolean isRunning() {
        return (this.transferStatus == LgFileTransferThreadImpl.STATUS_TRANSFERRING || this.transferStatus == LgFileTransferThreadImpl.STATUS_PREPARING);
    }

    public boolean isFinished() {
        return (this.transferStatus == LgFileTransferThreadImpl.STATUS_SUCCESFULLY_TRANSFERRED || this.transferStatus == LgFileTransferThreadImpl.STATUS_TRANSFER_FAILED || this.transferStatus == LgFileTransferThreadImpl.STATUS_TRANSFER_CANCELLED);
    }

    public boolean wasSuccessfull() {
        return (this.transferStatus == LgFileTransferThreadImpl.STATUS_SUCCESFULLY_TRANSFERRED);
    }

    public void run() {
        this.statusString = "starting...";
        this.startTransfer();
    }

    public void addFileTransferThreadObserver(LgFileTransferThreadObserver i_observer) {
        this.threadObserverSet.add(i_observer);
    }

    public void removeFileTransferThreadObserver(LgFileTransferThreadObserver i_observer) {
        this.threadObserverSet.remove(i_observer);
    }

    /**
     * notify all observers that the transfer was succesfull
     */
    private void notifyTransferSucceded() {
        java.util.Iterator i = this.threadObserverSet.iterator();
        this.statusString = "succesfull";
        while (i.hasNext()) {
            LgFileTransferThreadObserver observer = (LgFileTransferThreadObserver) i.next();
            observer.transferSucceeded(this);
        }
    }

    /**
     * notify all observers that the transfer failed
     */
    private void notifyTransferFailed(Exception i_exception) {
        java.util.Iterator i = this.threadObserverSet.iterator();
        this.statusString = "failed";
        while (i.hasNext()) {
            LgFileTransferThreadObserver observer = (LgFileTransferThreadObserver) i.next();
            observer.transferFailed(this, i_exception);
        }
    }

    /**
     * notify all observers that the transfer has been cancelled
     */
    private void notifyTransferCancelled() {
        java.util.Iterator i = this.threadObserverSet.iterator();
        this.statusString = "cancelled";
        while (i.hasNext()) {
            LgFileTransferThreadObserver observer = (LgFileTransferThreadObserver) i.next();
            observer.transferCancelled(this);
        }
    }

    /**
     * notify all observers that the status changed.
     */
    private void notifyStatusUpdate() {
        this.timeOfLastStatusUpdateInMillis = System.currentTimeMillis();
        java.util.Iterator i = this.threadObserverSet.iterator();
        while (i.hasNext()) {
            LgFileTransferThreadObserver observer = (LgFileTransferThreadObserver) i.next();
            observer.transferStatusUpdate(this);
        }
    }

    /**
     * transfers the data from the input to the outputstream reading and writing
     * line by line, so that line endings will be in the correct format of
     * the target file system
     */
    private void doAsciiTransfer() throws ReadFromSourceFileFailure, WriteToTargetFileFailure, TransferCancelledExc {
        logger.info("transferring a texrt file from " + this.sourceFile.getURL() + " to " + this.targetFile.getURL() + "...");
        this.statusString = "transferring....";
        this.notifyStatusUpdate();
        this.timeWhenTransferWasStarted = new java.util.Date().getTime();
        boolean transferComplete = false;
        while (!transferComplete) {
            if (this.cancelTransferRequested) {
                throw new TransferCancelledExc(this.sourceFile, this.targetFile);
            }
            String nextLine;
            try {
                nextLine = this.inputStream.readLine();
            } catch (java.io.IOException e) {
                throw new ReadFromSourceFileFailure(e, this.sourceFile);
            }
            if (nextLine == null) {
                transferComplete = true;
                break;
            }
            try {
                this.outputStream.writeLine(nextLine);
            } catch (java.io.IOException e) {
                throw new WriteToTargetFileFailure(e, this.targetFile);
            }
        }
    }

    /**
     * transfers the data in binary mode
     */
    private void doBinaryTransfer() throws ReadFromSourceFileFailure, WriteToTargetFileFailure, TransferCancelledExc {
        logger.info("transferring from " + this.sourceFile.getURL() + " to " + this.targetFile.getURL() + "...");
        this.statusString = "transferring...";
        this.notifyStatusUpdate();
        this.timeWhenTransferWasStarted = new java.util.Date().getTime();
        try {
            boolean transferComplete = false;
            while (!transferComplete) {
                int bytesReceived;
                byte[] buffer;
                if (System.currentTimeMillis() - this.timeOfLastStatusUpdateInMillis > 1000) {
                    this.notifyStatusUpdate();
                }
                try {
                    int availableBytes = this.inputStream.available();
                    if (availableBytes > this.maxBufferSize) {
                        availableBytes = this.maxBufferSize;
                    }
                    buffer = new byte[availableBytes + 10];
                    logger.debug(" > trying to get " + availableBytes + " bytes from " + this.sourceFile.getURL());
                    bytesReceived = this.inputStream.read(buffer);
                } catch (java.io.IOException ex) {
                    throw new ReadFromSourceFileFailure(ex, this.sourceFile);
                }
                if (bytesReceived == -1) {
                    logger.info(" > there are no more bytes available from " + this.sourceFile.getURL());
                    this.outputStream.flush();
                    break;
                }
                logger.debug(" > received " + bytesReceived + " bytes from source");
                if (this.cancelTransferRequested) {
                    logger.info("the transfer will be cancelled, close the input stream..");
                    try {
                        this.inputStream.close();
                    } catch (Exception e) {
                        logger.error("failed to close input stream after transfer was cancelled", e);
                    }
                    try {
                        this.outputStream.close();
                    } catch (Exception e) {
                        logger.error("failed to close output stream after transfer was cancelled", e);
                    }
                    throw new TransferCancelledExc(this.sourceFile, this.targetFile);
                }
                try {
                    logger.debug(" > trying to write " + bytesReceived + " bytes to " + this.targetFile.getURL());
                    this.outputStream.write(buffer, 0, bytesReceived);
                    logger.debug(" > wrote bytes to target file");
                } catch (java.io.IOException ex) {
                    throw new WriteToTargetFileFailure(ex, this.targetFile);
                }
                this.numberOfBytesTransferred += bytesReceived;
            }
            this.notifyStatusUpdate();
            this.timeWhenTransferWasFinished = new java.util.Date().getTime();
            logger.info("succesfully wrote the last byte.");
        } catch (ReadFromSourceFileFailure ex1) {
            this.timeWhenTransferWasFinished = new java.util.Date().getTime();
            throw ex1;
        } catch (WriteToTargetFileFailure ex2) {
            this.timeWhenTransferWasFinished = new java.util.Date().getTime();
            throw ex2;
        } catch (TransferCancelledExc ex3) {
            this.timeWhenTransferWasFinished = new java.util.Date().getTime();
            throw ex3;
        } catch (Exception e) {
            throw new WriteToTargetFileFailure(e, this.targetFile);
        }
    }

    /**
     * creates the input stream to the source file
     */
    private void connectToTargetFile(boolean i_asciiMode) throws WriteToTargetFileFailure {
        logger.info("trying to connect to target " + this.targetFile.getURL());
        this.statusString = "connecting to target...";
        this.notifyStatusUpdate();
        try {
            if (this.resumeMode) {
                this.outputStream = this.targetFile.getAppendOutputStream(this.firstByteToRead, i_asciiMode);
            } else {
                this.outputStream = this.targetFile.getOutputStream(i_asciiMode);
            }
            logger.info("succesfully connected to target " + this.targetFile.getURL());
        } catch (Exception e) {
            if (e instanceof WriteToTargetFileFailure) {
                throw (WriteToTargetFileFailure) e;
            }
            throw new WriteToTargetFileFailure(e, this.targetFile);
        }
    }

    /**
     * creates the output stream to the target file
     */
    private void connectToSourceFile(boolean i_asciiMode) throws ReadFromSourceFileFailure {
        logger.info("trying to connect to source " + this.sourceFile.getURL());
        this.statusString = "connecting to source...";
        this.notifyStatusUpdate();
        try {
            if (this.resumeMode) {
                this.inputStream = this.sourceFile.getResumeInputStream(this.firstByteToRead, i_asciiMode);
            } else {
                this.inputStream = this.sourceFile.getInputStream(i_asciiMode);
            }
            logger.info("succesfully connected to source " + this.sourceFile.getURL());
        } catch (Exception ex) {
            if (ex instanceof ReadFromSourceFileFailure) {
                throw (ReadFromSourceFileFailure) ex;
            }
            throw new ReadFromSourceFileFailure(ex, this.sourceFile);
        }
    }
}
