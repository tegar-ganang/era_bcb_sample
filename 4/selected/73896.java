package gps.mvc;

import gps.BT747Constants;
import gps.GpsEvent;
import gps.convert.Conv;
import gps.log.in.WindowedFile;
import gps.mvc.commands.GpsLinkExecCommand;
import gps.mvc.commands.GpsLinkNmeaCommand;
import bt747.sys.File;
import bt747.sys.Generic;
import bt747.sys.JavaLibBridge;
import bt747.sys.RAFile;
import bt747.sys.interfaces.BT747Exception;
import bt747.sys.interfaces.BT747Path;

final class MTKLogDownloadHandler {

    private final MTKLogDownloadContext context = new MTKLogDownloadContext();

    /**
     * Data that can be reused across states. [Preparation to implement the
     * State Design Pattern]
     * 
     * @author Mario
     * 
     */
    private static final class MTKLogDownloadContext {

        protected MtkModel mtkM;

        protected MtkController mtkC;

        private int logState = MTKLogDownloadHandler.C_LOG_NOLOGGING;

        /**
         * @param logState
         *            the logState to set
         */
        protected void setLogState(final int logState) {
            this.logState = logState;
            switch(logState) {
                case MTKLogDownloadHandler.C_LOG_NOLOGGING:
                case MTKLogDownloadHandler.C_LOG_ERASE_STATE:
                    mtkM.setLogDownloadOngoing(false);
                    break;
                default:
                    mtkM.setLogDownloadOngoing(true);
            }
        }

        /**
         * @return the logState
         */
        protected int getLogState() {
            return logState;
        }

        private int logDownloadEndAddr;

        /**
         * @param logDownloadEndAddr
         *            the logDownloadEndAddr to set
         */
        protected void setLogDownloadEndAddr(int logDownloadEndAddr) {
            this.logDownloadEndAddr = logDownloadEndAddr;
            mtkM.setEndAddr(logDownloadEndAddr);
        }

        /**
         * @return the logDownloadEndAddr
         */
        protected int getLogDownloadEndAddr() {
            return logDownloadEndAddr;
        }

        private int logNextReadAddr;

        /**
         * @param logNextReadAddr
         *            the logNextReadAddr to set
         */
        protected void setLogNextReadAddr(int logNextReadAddr) {
            this.logNextReadAddr = logNextReadAddr;
            mtkM.setNextReadAddr(logNextReadAddr);
        }

        /**
         * @return the logNextReadAddr
         */
        protected int getLogNextReadAddr() {
            return logNextReadAddr;
        }

        protected RAFile logFile = null;

        /**
         * Currently selected file path for download.
         */
        protected BT747Path logPath = null;

        protected int startAddr;

        protected int endAddr;

        protected int logNextReqAddr;

        protected int logRequestStep;

        protected boolean isSmart = true;

        protected boolean disableLogging;

        protected boolean loggingIsActiveBeforeDownload = false;

        protected int logDownloadStartAddr;

        protected int logRequestAhead = 0;

        protected byte[] expectedResult;

        protected boolean forcedErase = false;

        protected boolean getFullLogBlocks = true;

        protected int usedLogRequestAhead = 0;

        /** buffer used for reading data. */
        protected final byte[] readDataBuffer = new byte[0x800];
    }

    private static final int C_LOG_NOLOGGING = 0;

    private static final int C_LOG_CHECK = 1;

    private static final int C_LOG_ACTIVE = 2;

    private static final int C_LOG_RECOVER = 3;

    private static final int C_LOG_ERASE_STATE = 4;

    /**
     * The log download must start, but we are waiting until all commands are
     * sent.
     */
    private static final int C_LOG_START = 5;

    /**
     * Waiting for a reply from the application concerning the authorisation
     * to overwrite data that is not the same.
     */
    private static final int C_LOG_DATA_NOT_SAME_WAITING_FOR_REPLY = 6;

    /** Timeout between log status requests for erase. */
    private static final int C_LOGERASE_TIMEOUT = 2000;

    protected MTKLogDownloadHandler(final MtkController controller, final MtkModel model) {
        context.mtkC = controller;
        context.mtkM = controller.getMtkModel();
    }

    /**
     * Initialise the log download.
     * 
     * @param startAddr
     *            Start address for the log download.
     * @param endAddr
     *            End address for the log download.
     * @param requestStep
     *            Size of data to download with each request (chunk size).
     * @param path
     *            The filename to save to.
     * @param isSmart
     *            When true, perform incremental read.
     * @param disableLogging
     *            Disable logging during download when true.
     */
    protected final void getLogInit(final int startAddr, final int endAddr, final int requestStep, final BT747Path path, final boolean isSmart, final boolean disableLogging) {
        context.startAddr = startAddr;
        context.endAddr = endAddr;
        context.logRequestStep = requestStep;
        context.logPath = path;
        context.isSmart = isSmart;
        context.disableLogging = disableLogging;
        MTKLogDownloadHandler.logInit(context);
    }

    private static final void logInit(final MTKLogDownloadContext context) {
        if (context.getLogState() == MTKLogDownloadHandler.C_LOG_NOLOGGING) {
            context.loggingIsActiveBeforeDownload = context.mtkM.isLoggingActive();
            if (context.disableLogging && context.loggingIsActiveBeforeDownload) {
                context.mtkC.cmd(MtkController.CMD_STOPLOG);
                context.mtkC.reqData(MtkModel.DATA_LOG_STATUS);
            }
        }
        context.mtkM.postEvent(GpsEvent.LOG_DOWNLOAD_STARTED);
        if (Generic.isDebug()) {
            Generic.debug((context.isSmart ? "Smart d" : "D") + "ownload request from " + JavaLibBridge.unsigned2hex(context.startAddr, 8) + " to " + JavaLibBridge.unsigned2hex(context.endAddr, 8));
        }
        context.logDownloadStartAddr = context.startAddr;
        context.setLogDownloadEndAddr(((context.endAddr + 0xFFFF) & 0xFFFF0000) - 1);
        context.logNextReqAddr = context.logDownloadStartAddr;
        context.setLogNextReadAddr(context.logDownloadStartAddr);
        if (context.logRequestStep > 0x800) {
            context.usedLogRequestAhead = 0;
        } else {
            context.usedLogRequestAhead = context.logRequestAhead;
        }
        context.mtkM.getHandler().setLogOrEraseOngoing(true);
        context.setLogState(MTKLogDownloadHandler.C_LOG_START);
    }

    private final void realDownloadStart() throws BT747Exception {
        try {
            if (context.isSmart && (new File(context.logPath)).exists()) {
                closeLog();
                final WindowedFile windowedLogFile = new WindowedFile(context.logPath, File.READ_ONLY);
                windowedLogFile.setBufferSize(0x200);
                if ((windowedLogFile != null) && windowedLogFile.isOpen()) {
                    if (windowedLogFile.getSize() >= (MTKLogDownloadHandler.C_BLOCKVERIF_START + MTKLogDownloadHandler.C_BLOCKVERIF_SIZE)) {
                        int blockHeadPos = 0;
                        boolean continueLoop;
                        do {
                            byte[] bytes;
                            bytes = windowedLogFile.fillBuffer(blockHeadPos);
                            continueLoop = (windowedLogFile.getBufferFill() >= 2);
                            if (continueLoop) {
                                continueLoop = !((bytes[0] == (byte) 0xFF) && (bytes[1] == (byte) 0xFF));
                            }
                            if (continueLoop) {
                                blockHeadPos += 0x10000;
                                continueLoop = (blockHeadPos <= (windowedLogFile.getSize() & 0xFFFF0000));
                            }
                        } while (continueLoop);
                        if (blockHeadPos > windowedLogFile.getSize()) {
                            context.setLogNextReadAddr(windowedLogFile.getSize());
                            context.logNextReqAddr = context.getLogNextReadAddr();
                        } else {
                            context.setLogNextReadAddr(blockHeadPos + 0x200);
                            continueLoop = true;
                            do {
                                final byte[] rBuffer = windowedLogFile.fillBuffer(context.getLogNextReadAddr());
                                continueLoop = (windowedLogFile.getBufferFill() >= 0x200);
                                if (continueLoop) {
                                    for (int i = 0; continueLoop && (i < 0x200); i++) {
                                        continueLoop = (rBuffer[i] == (byte) 0xFF);
                                    }
                                    continueLoop = !continueLoop;
                                    if (continueLoop) {
                                        context.setLogNextReadAddr(context.getLogNextReadAddr() + 0x200);
                                    }
                                }
                            } while (continueLoop);
                            context.setLogNextReadAddr(context.getLogNextReadAddr() - 0x200);
                            context.logNextReqAddr = context.getLogNextReadAddr();
                        }
                        final int potentialEndAddress = ((context.getLogNextReadAddr() + 0xFFFF) & 0xFFFF0000) - 1;
                        if (potentialEndAddress > context.getLogDownloadEndAddr()) {
                            if (Generic.isDebug()) {
                                Generic.debug("Adjusted end address from " + JavaLibBridge.unsigned2hex(context.getLogDownloadEndAddr(), 8) + " to " + JavaLibBridge.unsigned2hex(potentialEndAddress, 8));
                            }
                            context.setLogDownloadEndAddr(potentialEndAddress);
                        }
                        context.expectedResult = new byte[MTKLogDownloadHandler.C_BLOCKVERIF_SIZE];
                        byte[] b;
                        b = windowedLogFile.fillBuffer(MTKLogDownloadHandler.C_BLOCKVERIF_START);
                        for (int i = context.expectedResult.length - 1; i >= 0; i--) {
                            context.expectedResult[i] = b[i];
                        }
                        context.setLogState(MTKLogDownloadHandler.C_LOG_CHECK);
                        context.mtkM.getHandler().resetLogTimeOut();
                        requestCheckBlock();
                    }
                }
                context.mtkM.getHandler().updateIgnoreNMEA();
                windowedLogFile.close();
            }
            if (!(context.getLogState() == MTKLogDownloadHandler.C_LOG_CHECK)) {
                openNewLog(context.logPath);
                if (Generic.isDebug()) {
                    Generic.debug("Starting download from " + JavaLibBridge.unsigned2hex(context.getLogNextReadAddr(), 8) + " to " + JavaLibBridge.unsigned2hex(context.getLogDownloadEndAddr(), 8));
                }
                context.setLogState(MTKLogDownloadHandler.C_LOG_ACTIVE);
            }
            if (context.getLogState() == MTKLogDownloadHandler.C_LOG_NOLOGGING) {
                context.mtkM.postEvent(GpsEvent.LOG_DOWNLOAD_DONE);
            }
        } catch (final BT747Exception e) {
            context.setLogState(MTKLogDownloadHandler.C_LOG_NOLOGGING);
            context.mtkM.postEvent(GpsEvent.LOG_DOWNLOAD_DONE);
            throw e;
        } catch (final Exception e) {
            context.setLogState(MTKLogDownloadHandler.C_LOG_NOLOGGING);
            context.mtkM.postEvent(GpsEvent.LOG_DOWNLOAD_DONE);
            Generic.debug("getLogInit", e);
        }
    }

    protected final void openNewLog(final BT747Path path) throws BT747Exception {
        try {
            if ((context.logFile != null) && context.logFile.isOpen()) {
                context.logFile.close();
            }
            context.logFile = new RAFile(path, bt747.sys.File.DONT_OPEN);
            context.logPath = path;
            if (context.logFile.exists()) {
                context.logFile.delete();
            }
            context.logFile = new RAFile(path, bt747.sys.File.CREATE);
            context.logPath = path;
            context.logFile.close();
            context.logFile = new RAFile(path, bt747.sys.File.WRITE_ONLY);
            context.logPath = path;
            if ((context.logFile == null) || !(context.logFile.isOpen())) {
                throw new BT747Exception(BT747Exception.ERR_COULD_NOT_OPEN, new Throwable(path.toString()));
            }
        } catch (BT747Exception e) {
            throw e;
        } catch (Exception e) {
            Generic.debug("openNewLog", e);
            throw new BT747Exception("open", new Throwable(path.toString()));
        }
    }

    private void reOpenLogWrite(final BT747Path path) {
        closeLog();
        try {
            context.logFile = new RAFile(path, File.WRITE_ONLY);
            context.logPath = path;
        } catch (final Exception e) {
            Generic.debug("reOpenLogWrite", e);
        }
    }

    private void getNextLogPart() {
        if (context.getLogState() != MTKLogDownloadHandler.C_LOG_NOLOGGING) {
            int z_Step;
            z_Step = context.getLogDownloadEndAddr() - context.logNextReqAddr + 1;
            switch(context.getLogState()) {
                case C_LOG_ACTIVE:
                    if (context.getLogDownloadEndAddr() <= context.getLogNextReadAddr()) {
                        endGetLog();
                    }
                    if (context.logNextReqAddr > context.getLogNextReadAddr() + context.logRequestStep * context.usedLogRequestAhead) {
                        z_Step = 0;
                    }
                    break;
                case C_LOG_RECOVER:
                    if (context.getLogDownloadEndAddr() <= context.getLogNextReadAddr()) {
                        endGetLog();
                    }
                    if (context.logNextReqAddr > context.getLogNextReadAddr()) {
                        z_Step = 0;
                    } else if (z_Step > 0x800) {
                        z_Step = 0x800;
                    }
                    break;
                default:
                    z_Step = 0;
            }
            if (z_Step > 0) {
                if (z_Step > context.logRequestStep) {
                    z_Step = context.logRequestStep;
                }
                final int stepUntilBoundary = ((context.logNextReqAddr + 0x10000) & ~0xFFFF) - context.logNextReqAddr;
                if (z_Step > stepUntilBoundary) {
                    z_Step = stepUntilBoundary;
                }
                readLog(context.logNextReqAddr, z_Step);
                context.logNextReqAddr += z_Step;
                if (context.getLogState() == MTKLogDownloadHandler.C_LOG_ACTIVE) {
                    getNextLogPart();
                }
            }
        }
    }

    private void getLogPartNoOutstandingRequests() throws BT747Exception {
        switch(context.getLogState()) {
            case C_LOG_ACTIVE:
            case C_LOG_RECOVER:
                context.logNextReqAddr = context.getLogNextReadAddr();
                getNextLogPart();
                break;
            case C_LOG_CHECK:
                requestCheckBlock();
                break;
            case C_LOG_START:
                realDownloadStart();
                break;
            default:
                break;
        }
    }

    private void recoverFromLogError() {
        context.setLogState(MTKLogDownloadHandler.C_LOG_RECOVER);
    }

    protected final void analyzeLogPart(final int startAddr, final String sData) {
        int dataLength;
        dataLength = Conv.hexStringToBytes(sData, context.readDataBuffer) / 2;
        switch(context.getLogState()) {
            case C_LOG_ACTIVE:
            case C_LOG_RECOVER:
                if (context.getLogNextReadAddr() == startAddr) {
                    context.setLogState(MTKLogDownloadHandler.C_LOG_ACTIVE);
                    int j = 0;
                    if ((dataLength != 0x800) && (dataLength != context.logRequestStep) && ((context.getLogNextReadAddr() + dataLength) != context.logNextReqAddr) && (dataLength != ((context.getLogNextReadAddr() + 0x10000) & ~0xFFFF) - context.getLogNextReadAddr())) {
                        if (Generic.isDebug()) {
                            Generic.debug("Unexpected datalength: " + JavaLibBridge.unsigned2hex(dataLength, 8));
                        }
                        context.setLogState(MTKLogDownloadHandler.C_LOG_RECOVER);
                    } else {
                        for (int i = dataLength; i > 0; i -= MTKLogDownloadHandler.C_MAX_FILEBLOCK_WRITE) {
                            int l = i;
                            if (l > MTKLogDownloadHandler.C_MAX_FILEBLOCK_WRITE) {
                                l = MTKLogDownloadHandler.C_MAX_FILEBLOCK_WRITE;
                            }
                            try {
                                if ((context.logFile.writeBytes(context.readDataBuffer, j, l)) != l) {
                                    cancelGetLog();
                                }
                            } catch (final Exception e) {
                                Generic.debug("analyzeLogPart", e);
                                cancelGetLog();
                            }
                            j += l;
                        }
                        context.setLogNextReadAddr(context.getLogNextReadAddr() + dataLength);
                        if (context.getFullLogBlocks && (((startAddr - 1 + dataLength) & 0xFFFF0000) >= startAddr)) {
                            final int blockStart = 0xFFFF & (0x10000 - (startAddr & 0xFFFF));
                            if (!(((context.readDataBuffer[blockStart] & 0xFF) == 0xFF) && ((context.readDataBuffer[blockStart + 1] & 0xFF) == 0xFF))) {
                                int minEndAddr;
                                minEndAddr = (startAddr & 0xFFFF0000) + 0x20000 - 1;
                                if (minEndAddr > context.mtkM.getLogMemSize() - 1) {
                                    minEndAddr = context.mtkM.getLogMemSize() - 1;
                                }
                                if (minEndAddr > context.getLogDownloadEndAddr()) {
                                    context.setLogDownloadEndAddr(minEndAddr);
                                }
                            }
                        }
                    }
                    if (context.getLogNextReadAddr() > context.getLogDownloadEndAddr()) {
                        context.mtkM.postEvent(GpsEvent.LOG_DOWNLOAD_SUCCESS);
                        endGetLog();
                    } else {
                        getNextLogPart();
                    }
                } else {
                    Generic.debug("Expected:" + JavaLibBridge.unsigned2hex(context.getLogNextReadAddr(), 8) + " Got:" + JavaLibBridge.unsigned2hex(startAddr, 8) + " (" + JavaLibBridge.unsigned2hex(dataLength, 8) + ")", null);
                    recoverFromLogError();
                }
                break;
            case C_LOG_CHECK:
                context.setLogState(MTKLogDownloadHandler.C_LOG_NOLOGGING);
                if ((startAddr == MTKLogDownloadHandler.C_BLOCKVERIF_START) && (dataLength == MTKLogDownloadHandler.C_BLOCKVERIF_SIZE)) {
                    boolean success;
                    success = true;
                    for (int i = dataLength - 1; i >= 0; i--) {
                        if (context.readDataBuffer[i] != context.expectedResult[i]) {
                            success = false;
                            break;
                        }
                    }
                    if (success) {
                        reOpenLogWrite(context.logPath);
                        try {
                            context.logFile.setPos(context.getLogNextReadAddr());
                        } catch (final Exception e) {
                            Generic.debug("C_LOG_CHECK", e);
                        }
                        if (Generic.isDebug()) {
                            Generic.debug("Starting incremental download from " + JavaLibBridge.unsigned2hex(context.getLogNextReadAddr(), 8) + " to " + JavaLibBridge.unsigned2hex(context.getLogDownloadEndAddr(), 8));
                        }
                        context.setLogState(MTKLogDownloadHandler.C_LOG_ACTIVE);
                        getNextLogPart();
                    } else {
                        context.setLogState(MTKLogDownloadHandler.C_LOG_DATA_NOT_SAME_WAITING_FOR_REPLY);
                        if (Generic.isDebug()) {
                            Generic.debug("Different data - requesting overwrite confirmation");
                        }
                        context.mtkM.postEvent(GpsEvent.DOWNLOAD_DATA_NOT_SAME_NEEDS_REPLY);
                    }
                } else {
                    if (Generic.isDebug()) {
                        Generic.debug("Expected:" + JavaLibBridge.unsigned2hex(MTKLogDownloadHandler.C_BLOCKVERIF_START, 8) + " Got:" + JavaLibBridge.unsigned2hex(startAddr, 8) + " (" + JavaLibBridge.unsigned2hex(dataLength, 8) + ")", null);
                    }
                    context.setLogState(MTKLogDownloadHandler.C_LOG_CHECK);
                }
                break;
            default:
                break;
        }
        if (context.getLogState() == C_LOG_NOLOGGING) {
            context.mtkM.getHandler().setLogOrEraseOngoing(false);
        }
        context.mtkM.postEvent(GpsEvent.DOWNLOAD_STATE_CHANGE);
    }

    /**
     * Response from the application indicating if overwrite of log is ok nor
     * not.
     * 
     * @param overwrite
     * @throws BT747Exception
     */
    protected final void replyToOkToOverwrite(final boolean overwrite) throws BT747Exception {
        if (context.getLogState() == MTKLogDownloadHandler.C_LOG_DATA_NOT_SAME_WAITING_FOR_REPLY) {
            if (overwrite) {
                openNewLog(context.logPath);
                context.setLogNextReadAddr(0);
                context.setLogNextReadAddr(0);
                context.setLogState(MTKLogDownloadHandler.C_LOG_ACTIVE);
            } else {
                endGetLog();
            }
        }
    }

    /**
     * Start of block position to verify if log in device corresponds to log
     * in file.
     */
    private static final int C_BLOCKVERIF_START = 0x200;

    /** Size of block to validate that log in device is log in file. */
    private static final int C_BLOCKVERIF_SIZE = 0x200;

    private static final int C_MAX_FILEBLOCK_WRITE = 0x800;

    /**
     * Request the block to validate that log in device is log in file.
     */
    private void requestCheckBlock() {
        readLog(MTKLogDownloadHandler.C_BLOCKVERIF_START, MTKLogDownloadHandler.C_BLOCKVERIF_SIZE);
    }

    private void closeLog() {
        try {
            if (context.logFile != null) {
                if (context.logFile.isOpen()) {
                    context.logFile.close();
                    context.logFile = null;
                }
            }
        } catch (final Exception e) {
            Generic.debug("CloseLog", e);
        }
    }

    private void endGetLog() {
        context.setLogState(MTKLogDownloadHandler.C_LOG_NOLOGGING);
        closeLog();
        if (context.loggingIsActiveBeforeDownload) {
            context.mtkC.cmd(MtkController.CMD_STARTLOG);
            context.mtkC.reqData(MtkModel.DATA_LOG_STATUS);
        }
        context.mtkM.postEvent(GpsEvent.LOG_DOWNLOAD_DONE);
    }

    protected final int getStartAddr() {
        return context.logDownloadStartAddr;
    }

    protected final int getNextReadAddr() {
        return context.getLogNextReadAddr();
    }

    protected final void cancelGetLog() {
        endGetLog();
    }

    /**
     * erase the log - takes a while.<br>
     * TODO: Find out a way to follow up on erasal (status) (check response on
     * cmd)
     */
    protected final void eraseLog() {
        if (context.mtkM.getHandler().isConnected()) {
            context.mtkM.getHandler().setEraseOngoing(true);
            context.mtkC.doSendCmd(new GpsLinkNmeaCommand("PMTK" + BT747Constants.PMTK_CMD_LOG_STR + "," + BT747Constants.PMTK_LOG_ERASE + "," + BT747Constants.PMTK_LOG_ERASE_YES_STR));
            waitEraseDone();
        }
    }

    protected final void recoveryEraseLog() {
        context.mtkC.cmd(MtkController.CMD_STOPLOG);
        context.mtkC.reqData(MtkModel.DATA_LOG_STATUS);
        context.mtkC.reqData(MtkModel.DATA_LOG_FLASH_SECTOR_STATUS);
        context.mtkC.sendCmd(new GpsLinkNmeaCommand("PMTK" + BT747Constants.PMTK_CMD_LOG_STR + "," + BT747Constants.PMTK_LOG_ENABLE, false));
        context.mtkC.reqData(MtkModel.DATA_LOG_STATUS);
        context.forcedErase = true;
        eraseLog();
    }

    private void postRecoveryEraseLog() {
        context.mtkC.reqData(MtkModel.DATA_LOG_STATUS);
        context.mtkC.reqData(MtkModel.DATA_LOG_FLASH_SECTOR_STATUS);
        context.mtkC.sendCmd("PMTK" + BT747Constants.PMTK_CMD_LOG_STR + "," + BT747Constants.PMTK_LOG_INIT);
        context.mtkC.reqData(MtkModel.DATA_LOG_FLASH_SECTOR_STATUS);
        context.mtkC.reqData(MtkModel.DATA_LOG_STATUS);
    }

    private void waitEraseDone() {
        context.setLogState(MTKLogDownloadHandler.C_LOG_ERASE_STATE);
        context.mtkM.getHandler().setLogOrEraseOngoing(true);
        context.mtkM.getHandler().resetLogTimeOut();
    }

    protected void signalEraseDone() {
        context.setLogState(MTKLogDownloadHandler.C_LOG_NOLOGGING);
        context.mtkM.getHandler().setLogOrEraseOngoing(true);
        context.mtkM.setEraseOngoing(false);
    }

    protected final void stopErase() {
        if (context.mtkM.isEraseOngoing() && (context.getLogState() == MTKLogDownloadHandler.C_LOG_ERASE_STATE)) {
            context.mtkM.getHandler().updateIgnoreNMEA();
            signalEraseDone();
        } else {
            context.mtkM.setEraseOngoing(false);
        }
    }

    /**
     * Called from within run of GPSstate (regularly called).
     * 
     * @throws BT747Exception
     */
    protected void notifyRun() throws BT747Exception {
        if ((context.mtkM.getHandler().getOutStandingCmdsCount() == 0) && (context.getLogState() != MTKLogDownloadHandler.C_LOG_NOLOGGING) && (context.getLogState() != MTKLogDownloadHandler.C_LOG_ERASE_STATE)) {
            getLogPartNoOutstandingRequests();
        } else if (context.getLogState() == MTKLogDownloadHandler.C_LOG_ACTIVE) {
            getNextLogPart();
        } else if (context.getLogState() == MTKLogDownloadHandler.C_LOG_ERASE_STATE) {
            if (context.mtkM.getHandler().timeSinceLastStamp() > MTKLogDownloadHandler.C_LOGERASE_TIMEOUT) {
                context.mtkC.reqData(MtkModel.DATA_LOG_FLASH_STATUS);
            }
        }
    }

    protected final void notifyDisconnected() {
        if (context.getLogState() != MTKLogDownloadHandler.C_LOG_NOLOGGING) {
            endGetLog();
        }
    }

    protected final void handleLogFlashStatReply(final String s) {
        if (context.getLogState() == MTKLogDownloadHandler.C_LOG_ERASE_STATE) {
            switch(JavaLibBridge.toInt(s)) {
                case 1:
                    if (context.mtkM.isEraseOngoing()) {
                        signalEraseDone();
                    }
                    if (context.forcedErase) {
                        context.forcedErase = false;
                        postRecoveryEraseLog();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * A single request to get information from the device's log.
     * 
     * @param startAddr
     *            start address of the data range requested
     * @param size
     *            size of the data range requested
     */
    protected final void readLog(final int startAddr, final int size) {
        context.mtkC.sendCmd("PMTK" + BT747Constants.PMTK_CMD_LOG_STR + "," + BT747Constants.PMTK_LOG_Q_LOG + "," + JavaLibBridge.unsigned2hex(startAddr, 8) + "," + JavaLibBridge.unsigned2hex(size, 8));
    }

    protected final void setLogRequestAhead(final int logRequestAhead) {
        context.logRequestAhead = logRequestAhead;
    }

    /**
     * Temporary for Wonde Proud
     */
    protected final File getLogFile() {
        return context.logFile;
    }
}
