package frost.fcp;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import frost.*;
import frost.fileTransfer.download.FrostDownloadItem;
import frost.threads.getKeyThread;

public class FcpRequest {

    static final boolean DEBUG = true;

    private static Logger logger = Logger.getLogger(FcpRequest.class.getName());

    private static int getActiveThreads(Thread[] threads) {
        int count = 0;
        for (int i = 0; i < threads.length; i++) {
            if (threads[i] != null) {
                if (threads[i].isAlive()) count++;
            }
        }
        return count;
    }

    /**
     * Downloads a FEC splitfile.
     * If downloadItem == null, we download a file not contained in download table
     * (e.g. an index file). Then do not update progress, and remove working files
     * after finished.
     * 
     * @param target  File to download to
     * @param redirect  The downloaded redirect file
     * @param htl  HTL to use for download
     * @param dlItem  the download item to update progress. can be null.
     * @return
     */
    private static boolean getFECSplitFile(File target, File redirect, int htl, FrostDownloadItem dlItem) {
        boolean optionTryAllSegments = MainFrame.frostSettings.getBoolValue("downloadTryAllSegments");
        boolean optionDecodeAfterDownload = MainFrame.frostSettings.getBoolValue("downloadDecodeAfterEachSegment");
        FecSplitfile splitfile = null;
        File t1 = new File(target.getPath() + ".redirect");
        if (t1.exists() == false || t1.length() == 0) {
            t1.delete();
            redirect.renameTo(t1);
            redirect = new File(target.getPath() + ".redirect");
        } else {
            redirect.delete();
            redirect = t1;
        }
        try {
            splitfile = new FecSplitfile(target, redirect);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Exception thrown in getFECSplitFile(File, File, int, FrostDownloadItem)", ex);
            return false;
        }
        int displayedRequiredBlocks = splitfile.getDataBlocks().size();
        int displayedAvailableBlocks = splitfile.getDataBlocks().size() + splitfile.getCheckBlocks().size();
        int displayedFinishedBlocks = 0;
        if (dlItem != null) {
            for (int segmentNo = 0; segmentNo < splitfile.getSegmentCount(); segmentNo++) {
                FecSplitfile.SingleSegmentValues seginf = (FecSplitfile.SingleSegmentValues) splitfile.getValuesForSegment(segmentNo);
                int neededBlockCount = seginf.dataBlockCount;
                int availableBlockCount = seginf.dataBlockCount + seginf.checkBlockCount;
                ArrayList missingBlocks = getBlocksInSegmentWithState(splitfile.getDataBlocks(), segmentNo, FecBlock.STATE_TRANSFER_WAITING);
                missingBlocks.addAll(getBlocksInSegmentWithState(splitfile.getCheckBlocks(), segmentNo, FecBlock.STATE_TRANSFER_WAITING));
                int missingOverallBlockCount = missingBlocks.size();
                ArrayList finishedBlocks = getBlocksInSegmentWithState(splitfile.getDataBlocks(), segmentNo, FecBlock.STATE_TRANSFER_FINISHED);
                finishedBlocks.addAll(getBlocksInSegmentWithState(splitfile.getCheckBlocks(), segmentNo, FecBlock.STATE_TRANSFER_FINISHED));
                int segmentsFinishedBlocks = finishedBlocks.size();
                if (segmentsFinishedBlocks > neededBlockCount) {
                    displayedFinishedBlocks += neededBlockCount;
                } else {
                    displayedFinishedBlocks += segmentsFinishedBlocks;
                }
            }
            dlItem.setFileSize(new Long(splitfile.getDataFileSize()));
            dlItem.setDoneBlocks(displayedFinishedBlocks);
            dlItem.setRequiredBlocks(displayedRequiredBlocks);
            dlItem.setTotalBlocks(displayedAvailableBlocks);
            dlItem.setState(FrostDownloadItem.STATE_PROGRESS);
        }
        boolean[] wasSegmentSuccessful = new boolean[splitfile.getSegmentCount()];
        Arrays.fill(wasSegmentSuccessful, false);
        for (int segmentNo = 0; segmentNo < splitfile.getSegmentCount(); segmentNo++) {
            FecSplitfile.SingleSegmentValues seginf = (FecSplitfile.SingleSegmentValues) splitfile.getValuesForSegment(segmentNo);
            int neededBlockCount = seginf.dataBlockCount;
            int availableBlockCount = seginf.dataBlockCount + seginf.checkBlockCount;
            ArrayList missingBlocks = getBlocksInSegmentWithState(splitfile.getDataBlocks(), segmentNo, FecBlock.STATE_TRANSFER_WAITING);
            missingBlocks.addAll(getBlocksInSegmentWithState(splitfile.getCheckBlocks(), segmentNo, FecBlock.STATE_TRANSFER_WAITING));
            int missingOverallBlockCount = missingBlocks.size();
            ArrayList finishedBlocks = getBlocksInSegmentWithState(splitfile.getDataBlocks(), segmentNo, FecBlock.STATE_TRANSFER_FINISHED);
            finishedBlocks.addAll(getBlocksInSegmentWithState(splitfile.getCheckBlocks(), segmentNo, FecBlock.STATE_TRANSFER_FINISHED));
            int segmentsFinishedBlocks = finishedBlocks.size();
            finishedBlocks = null;
            if (missingOverallBlockCount == 0) {
                logger.info("Segment " + segmentNo + " is already decoded.");
                wasSegmentSuccessful[segmentNo] = true;
                continue;
            }
            int maxThreads = MainFrame.frostSettings.getIntValue("splitfileDownloadThreads");
            if (segmentsFinishedBlocks < neededBlockCount) {
                Collections.shuffle(missingBlocks);
                int actBlockIx = 0;
                Vector runningThreads = new Vector(maxThreads);
                while (segmentsFinishedBlocks < neededBlockCount && actBlockIx < missingBlocks.size()) {
                    boolean threadsFinished = false;
                    for (int y = runningThreads.size() - 1; y >= 0; y--) {
                        GetKeyThread gkt = (GetKeyThread) runningThreads.get(y);
                        if (gkt.isAlive() == false) {
                            if (gkt.getSuccess() == true) {
                                if (segmentsFinishedBlocks < neededBlockCount) {
                                    displayedFinishedBlocks++;
                                }
                                segmentsFinishedBlocks++;
                                if (dlItem != null) {
                                    dlItem.setDoneBlocks(displayedFinishedBlocks);
                                    dlItem.setRequiredBlocks(displayedRequiredBlocks);
                                    dlItem.setTotalBlocks(displayedAvailableBlocks);
                                }
                            }
                            runningThreads.remove(y);
                            threadsFinished = true;
                        }
                    }
                    if (threadsFinished == true) continue;
                    int maxThreadsNeeded = neededBlockCount - segmentsFinishedBlocks;
                    int threadCountAllowedToStart = maxThreads - runningThreads.size();
                    if (maxThreadsNeeded > 0 && threadCountAllowedToStart > 0) {
                        FecBlock block = (FecBlock) missingBlocks.get(actBlockIx);
                        actBlockIx++;
                        GetKeyThread thread = new GetKeyThread(splitfile, block, htl);
                        runningThreads.add(thread);
                        thread.start();
                        Mixed.wait(111);
                        continue;
                    }
                    Mixed.wait(1000);
                }
                while (runningThreads.size() > 0) {
                    boolean threadsFinished = false;
                    for (int y = runningThreads.size() - 1; y >= 0; y--) {
                        GetKeyThread gkt = (GetKeyThread) runningThreads.get(y);
                        if (gkt.isAlive() == false) {
                            if (gkt.getSuccess() == true) {
                                if (segmentsFinishedBlocks < neededBlockCount) {
                                    displayedFinishedBlocks++;
                                }
                                segmentsFinishedBlocks++;
                                if (dlItem != null) {
                                    dlItem.setDoneBlocks(displayedFinishedBlocks);
                                    dlItem.setRequiredBlocks(displayedRequiredBlocks);
                                    dlItem.setTotalBlocks(displayedAvailableBlocks);
                                }
                            }
                            runningThreads.remove(y);
                            threadsFinished = true;
                        }
                    }
                    if (threadsFinished == true) continue;
                    Mixed.wait(1000);
                }
            }
            if (splitfile.isDecodeable(segmentNo)) {
                logger.info("Segment " + segmentNo + " is decodeable...");
                if (optionDecodeAfterDownload == false) {
                    if (dlItem != null) {
                        dlItem.setState(FrostDownloadItem.STATE_DECODING);
                    }
                    try {
                        splitfile.decode(segmentNo);
                    } catch (Throwable e1) {
                        logger.log(Level.SEVERE, "Exception thrown in getFECSplitFile(File, File, int, FrostDownloadItem)", e1);
                        wasSegmentSuccessful[segmentNo] = false;
                        break;
                    }
                    if (dlItem != null) {
                        dlItem.setState(FrostDownloadItem.STATE_PROGRESS);
                    }
                    setBlocksInSegmentFinished(splitfile.getDataBlocks(), segmentNo);
                    setBlocksInSegmentFinished(splitfile.getCheckBlocks(), segmentNo);
                    splitfile.createRedirectFile(true);
                }
                wasSegmentSuccessful[segmentNo] = true;
            } else {
                logger.warning("Segment " + segmentNo + " is NOT decodeable...");
                wasSegmentSuccessful[segmentNo] = false;
                if (optionTryAllSegments == false) {
                    break;
                }
            }
        }
        boolean success = true;
        for (int x = 0; x < wasSegmentSuccessful.length; x++) {
            if (wasSegmentSuccessful[x] == false) {
                success = false;
            }
        }
        if (optionDecodeAfterDownload == true && success == true) {
            if (dlItem != null) {
                dlItem.setState(FrostDownloadItem.STATE_DECODING);
            }
            for (int segmentNo = 0; segmentNo < splitfile.getSegmentCount(); segmentNo++) {
                try {
                    splitfile.decode(segmentNo);
                } catch (Throwable e1) {
                    logger.log(Level.SEVERE, "Exception thrown in getFECSplitFile(File, File, int, FrostDownloadItem)", e1);
                    success = false;
                    wasSegmentSuccessful[segmentNo] = false;
                    break;
                }
            }
            if (dlItem != null) {
                dlItem.setState(FrostDownloadItem.STATE_PROGRESS);
            }
        }
        if (success == true) {
            splitfile.setCorrectDatafileSize();
            if (dlItem != null) {
                splitfile.finishDownload(false);
            } else {
                splitfile.finishDownload(true);
            }
        } else {
            if (dlItem == null) {
                splitfile.finishDownload(true);
            }
        }
        return success;
    }

    private static class GetKeyThread extends Thread {

        FecBlock block;

        int htl;

        boolean success;

        FecSplitfile splitfile;

        public GetKeyThread(FecSplitfile sf, FecBlock b, int h) {
            block = b;
            htl = h;
            splitfile = sf;
        }

        public void run() {
            block.setCurrentState(FecBlock.STATE_TRANSFER_RUNNING);
            this.success = false;
            FcpConnection connection = FcpFactory.getFcpConnectionInstance();
            if (connection != null) {
                try {
                    success = connection.getKeyToBucket(block.getChkKey(), block.getRandomAccessFileBucket(false), htl);
                } catch (FcpToolsException e) {
                    success = false;
                } catch (IOException e) {
                    success = false;
                }
            }
            if (success == true) {
                block.setCurrentState(FecBlock.STATE_TRANSFER_FINISHED);
                splitfile.createRedirectFile(true);
            } else {
                block.setCurrentState(FecBlock.STATE_TRANSFER_WAITING);
            }
        }

        public boolean getSuccess() {
            return success;
        }
    }

    private static ArrayList getBlocksInSegmentWithState(List allBlocks, int segno, int state) {
        ArrayList l = new ArrayList();
        for (int x = 0; x < allBlocks.size(); x++) {
            FecBlock b = (FecBlock) allBlocks.get(x);
            if (b.getSegmentNo() == segno && b.getCurrentState() == state) {
                l.add(b);
            }
        }
        return l;
    }

    private static void setBlocksInSegmentFinished(List allBlocks, int segno) {
        for (int x = 0; x < allBlocks.size(); x++) {
            FecBlock b = (FecBlock) allBlocks.get(x);
            if (b.getSegmentNo() == segno && b.getCurrentState() != FecBlock.STATE_TRANSFER_FINISHED) {
                b.setCurrentState(FecBlock.STATE_TRANSFER_FINISHED);
            }
        }
    }

    /**
     * getFile retrieves a file from Freenet. It does detect if this file is a redirect, a splitfile or
     * just a simple file. It checks the size for the file and returns false if sizes do not match.
     * Size is ignored if it is NULL
     *
     * @param key The key to retrieve. All to Freenet known key formats are allowed (passed to node via FCP).
     * @param size Size of the file in bytes. Is ignored if not an integer value or -1 (splitfiles do not need this setting).
     * @param target Target path
     * @param htl request htl
     * @param doRedirect If true, getFile redirects if possible and downloads the file it was redirected to.
     * @return True if download was successful, else false.
     */
    public static FcpResults getFile(String key, Long size, File target, int htl, boolean doRedirect) {
        return getFile(key, size, target, htl, doRedirect, false, true, null);
    }

    public static FcpResults getFile(String key, Long size, File target, int htl, boolean doRedirect, boolean fastDownload) {
        return getFile(key, size, target, htl, doRedirect, fastDownload, true, null);
    }

    public static FcpResults getFile(String key, Long size, File target, int htl, boolean doRedirect, boolean fastDownload, boolean createTempFile, FrostDownloadItem dlItem) {
        assert htl >= 0;
        File tempFile = null;
        if (createTempFile) {
            try {
                tempFile = File.createTempFile("getFile_", ".tmp", new File(MainFrame.frostSettings.getValue("temp.dir")));
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Exception thrown in getFile(...)", ex);
                return null;
            }
        } else {
            tempFile = new File(target.getPath() + ".tmp");
        }
        FcpResults results = null;
        String[] metadataLines = null;
        if (dlItem != null && dlItem.getRedirect() != null) {
            results = new FcpResults(dlItem.getRedirect().getBytes(), dlItem.getKey());
            logger.info("starting download of an attached redirect");
        } else results = getKey(key, tempFile, htl, fastDownload);
        if (results != null) metadataLines = results.getMetadataAsLines();
        if (results != null && (tempFile.length() > 0 || metadataLines != null)) {
            if (metadataLines != null && doRedirect) {
                String redirectCHK = getRedirectCHK(metadataLines, key);
                if (redirectCHK != null) {
                    logger.info("Redirecting to " + redirectCHK);
                    results = null;
                    results = getKey(redirectCHK, tempFile, htl, fastDownload);
                    if (results == null || tempFile.length() == 0) {
                        tempFile.delete();
                        return null;
                    }
                }
            }
            boolean isSplitfile = false;
            if (metadataLines != null) {
                String content[] = metadataLines;
                String algoName = null;
                for (int i = 0; i < content.length; i++) {
                    if (content[i].startsWith("SplitFile.Size")) isSplitfile = true;
                    if (content[i].startsWith("SplitFile.AlgoName")) {
                        algoName = content[i].substring(content[i].indexOf("=") + 1).trim();
                    }
                }
                if (isSplitfile) {
                    boolean success;
                    if (algoName != null && algoName.equals("OnionFEC_a_1_2")) {
                        FileAccess.writeByteArray(results.getRawMetadata(), tempFile);
                        success = getFECSplitFile(target, tempFile, htl, dlItem);
                        if (success) {
                            return results;
                        }
                        return null;
                    } else {
                        success = getSplitFile(key, tempFile, htl, dlItem);
                    }
                    if (success) {
                        if (target.isFile()) target.delete();
                        tempFile.renameTo(target);
                        return results;
                    } else {
                        tempFile.delete();
                        return null;
                    }
                }
            }
            if (size == null || size.longValue() == tempFile.length()) {
                if (target.isFile()) target.delete();
                boolean wasOK = tempFile.renameTo(target);
                if (wasOK == false) {
                    logger.severe("ERROR: Could not move file '" + tempFile.getPath() + "' to '" + target.getPath() + "'.\n" + "Maybe the locations are on different filesystems where a move is not allowed.\n" + "Please try change the location of 'temp.dir' in the frost.ini file," + " and copy the file to a save location by yourself.");
                }
                return results;
            }
        }
        tempFile.delete();
        return null;
    }

    private static String getRedirectCHK(String[] metadata, String key) {
        String searchedFilename = null;
        int pos1 = key.lastIndexOf("/");
        if (pos1 > -1) {
            searchedFilename = key.substring(pos1 + 1).trim();
            if (searchedFilename.length() == 0) searchedFilename = null;
        }
        if (searchedFilename == null) return null;
        final String keywordName = "Name=";
        final String keywordRedirTarget = "Redirect.Target=";
        String actualFilename = null;
        String actualCHK = null;
        String resultCHK = null;
        for (int lineno = 0; lineno < metadata.length; lineno++) {
            String line = metadata[lineno].trim();
            if (line.length() == 0) continue;
            if (line.equals("Document")) {
                actualFilename = null;
                actualCHK = null;
            } else if (line.equals("End") || line.equals("EndPart")) {
                if (actualCHK != null && actualFilename != null) {
                    if (actualFilename.equals(searchedFilename)) {
                        resultCHK = actualCHK;
                        return resultCHK;
                    }
                }
            } else if (line.startsWith(keywordName)) {
                actualFilename = line.substring(keywordName.length()).trim();
            } else if (line.startsWith(keywordRedirTarget)) {
                actualCHK = line.substring(keywordRedirTarget.length()).trim();
            }
        }
        return null;
    }

    private static FcpResults getKey(String key, File target, int htl, boolean fastDownload) {
        if (key == null || key.length() == 0 || key.startsWith("null")) return null;
        FcpResults results = null;
        FcpConnection connection = FcpFactory.getFcpConnectionInstance();
        if (connection != null) {
            int tries = 0;
            int maxtries = 3;
            while (tries < maxtries || results != null) {
                try {
                    results = connection.getKeyToFile(key, target.getPath(), htl, fastDownload);
                    break;
                } catch (java.net.ConnectException e) {
                    tries++;
                    continue;
                } catch (DataNotFoundException ex) {
                    logger.log(Level.INFO, "FcpRequest.getKey(1): DataNotFoundException (usual if not found)", ex);
                    break;
                } catch (FcpToolsException e) {
                    logger.log(Level.SEVERE, "FcpRequest.getKey(1): FcpToolsException", e);
                    break;
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "FcpRequest.getKey(1): FcpToolsException", e);
                    break;
                }
            }
        }
        String printableKey = null;
        if (DEBUG) {
            String keyPrefix = "";
            if (key.indexOf("@") > -1) keyPrefix = key.substring(0, key.indexOf("@") + 1);
            String keyUrl = "";
            if (key.indexOf("/") > -1) keyUrl = key.substring(key.indexOf("/"));
            printableKey = new StringBuffer().append(keyPrefix).append("...").append(keyUrl).toString();
        }
        boolean metadataAvailable = results != null && results.getRawMetadata() != null && results.getRawMetadata().length > 0;
        if (results != null && (target.length() > 0 || metadataAvailable)) {
            logger.info("getKey - Success: " + printableKey);
            return results;
        }
        target.delete();
        logger.info("getKey - Failed: " + printableKey);
        return null;
    }

    private static boolean getSplitFile(String key, File target, int htl, FrostDownloadItem dlItem) {
        logger.warning("ATTENTION: Using old, non-FEC download method!\n" + "           This could run, but is'nt really supported any longer.");
        String blockCount = SettingsFun.getValue(target.getPath(), "SplitFile.BlockCount");
        String splitFileSize = SettingsFun.getValue(target.getPath(), "SplitFile.Size");
        String splitFileBlocksize = SettingsFun.getValue(target.getPath(), "SplitFile.Blocksize");
        int maxThreads = 3;
        maxThreads = MainFrame.frostSettings.getIntValue("splitfileDownloadThreads");
        int intBlockCount = 0;
        try {
            intBlockCount = Integer.parseInt(blockCount, 16);
        } catch (NumberFormatException e) {
        }
        long intSplitFileSize = -1;
        try {
            intSplitFileSize = Long.parseLong(splitFileSize, 16);
        } catch (NumberFormatException e) {
        }
        int intSplitFileBlocksize = -1;
        try {
            intSplitFileBlocksize = Integer.parseInt(splitFileBlocksize, 16);
        } catch (NumberFormatException e) {
        }
        int[] blockNumbers = new int[intBlockCount];
        for (int i = 0; i < intBlockCount; i++) blockNumbers[i] = i + 1;
        Random rand = new Random(System.currentTimeMillis());
        for (int i = 0; i < intBlockCount; i++) {
            int tmp = blockNumbers[i];
            int randomNumber = Math.abs(rand.nextInt()) % intBlockCount;
            blockNumbers[i] = blockNumbers[randomNumber];
            blockNumbers[randomNumber] = tmp;
        }
        if (dlItem != null) {
            if (dlItem.getFileSize() == null) {
                dlItem.setFileSize(new Long(intSplitFileSize));
            } else {
                if (dlItem.getFileSize().longValue() != intSplitFileSize) {
                    logger.warning("WARNING: size of fec splitfile differs from size given from download table. MUST not happen!");
                }
            }
            dlItem.setDoneBlocks(0);
            dlItem.setRequiredBlocks(intBlockCount);
            dlItem.setTotalBlocks(intBlockCount);
            dlItem.setState(FrostDownloadItem.STATE_PROGRESS);
        }
        boolean success = true;
        boolean[] results = new boolean[intBlockCount];
        Thread[] threads = new Thread[intBlockCount];
        for (int i = 0; i < intBlockCount; i++) {
            int j = blockNumbers[i];
            String chk = SettingsFun.getValue(target.getPath(), "SplitFile.Block." + Integer.toHexString(j));
            while (getActiveThreads(threads) >= maxThreads) {
                Mixed.wait(5000);
                if (dlItem != null) {
                    int doneBlocks = 0;
                    for (int z = 0; z < intBlockCount; z++) {
                        if (results[z] == true) {
                            doneBlocks++;
                        }
                    }
                    dlItem.setDoneBlocks(doneBlocks);
                    dlItem.setRequiredBlocks(intBlockCount);
                    dlItem.setTotalBlocks(intBlockCount);
                }
            }
            logger.info("Requesting: SplitFile.Block." + Integer.toHexString(j) + "=" + chk);
            int checkSize = intSplitFileBlocksize;
            if (blockNumbers[i] == intBlockCount && intSplitFileBlocksize != -1) checkSize = (int) (intSplitFileSize - (intSplitFileBlocksize * (intBlockCount - 1)));
            threads[i] = new getKeyThread(chk, new File(MainFrame.keypool + target.getName() + "-chunk-" + j), htl, results, i, checkSize);
            threads[i].start();
            if (dlItem != null) {
                int doneBlocks = 0;
                for (int z = 0; z < intBlockCount; z++) {
                    if (results[z] == true) {
                        doneBlocks++;
                    }
                }
                dlItem.setDoneBlocks(doneBlocks);
                dlItem.setRequiredBlocks(intBlockCount);
                dlItem.setTotalBlocks(intBlockCount);
            }
        }
        while (getActiveThreads(threads) > 0) {
            Mixed.wait(5000);
            if (dlItem != null) {
                int doneBlocks = 0;
                for (int z = 0; z < intBlockCount; z++) {
                    if (results[z] == true) {
                        doneBlocks++;
                    }
                }
                dlItem.setDoneBlocks(doneBlocks);
                dlItem.setRequiredBlocks(intBlockCount);
                dlItem.setTotalBlocks(intBlockCount);
            }
        }
        for (int i = 0; i < intBlockCount; i++) {
            if (!results[i]) {
                success = false;
                logger.info("NO SUCCESS");
            } else {
                logger.info("SUCCESS");
            }
        }
        if (success) {
            FileOutputStream fileOut;
            try {
                fileOut = new FileOutputStream(target);
                logger.info("Connecting chunks");
                for (int i = 1; i <= intBlockCount; i++) {
                    logger.fine("Adding chunk " + i + " to " + target.getName());
                    File toRead = new File(MainFrame.keypool + target.getName() + "-chunk-" + i);
                    fileOut.write(FileAccess.readByteArray(toRead));
                    toRead.deleteOnExit();
                    toRead.delete();
                }
                fileOut.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Write Error: " + target.getPath(), e);
            }
        } else {
            target.delete();
            logger.warning("!!!!!! Download of " + target.getName() + " failed.");
        }
        return success;
    }
}
