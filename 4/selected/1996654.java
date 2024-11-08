package org.sac.browse.datastore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import org.sac.browse.util.Logger;

public class JDOPersister implements AppDataPersister {

    private static final Logger logger = new Logger("JDOPersister");

    private static final int MAX_NO_OF_SPLITS = 500;

    private static final int MAX_BLOB_SIZE = 1000000;

    public JDOPersister() throws NoClassDefFoundError {
        PersistenceManager pm = PMF.get().getPersistenceManager();
    }

    private void persist(String fileKey, byte[] data, String fileName, int splitNo, boolean isLast) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        TmpFile tmpFile = new TmpFile(fileKey, data, fileName, splitNo, isLast);
        pm.makePersistent(tmpFile);
        logger.logIt("Saved File!");
    }

    public String isBatchFinal(String fileNameKey, int batchNo, int batchSize) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        String status = "NOTFINAL";
        int startIndex = batchSize * batchNo;
        int endIndex = startIndex + (batchSize - 1);
        String query = "select from " + TmpFile.class.getName() + " where " + TmpFile.COL_KEY + " == '" + fileNameKey + "'" + " && " + TmpFile.COL_SPLIT_NO + ">=" + startIndex + " && " + TmpFile.COL_SPLIT_NO + "<=" + endIndex + " order by " + TmpFile.COL_SPLIT_NO;
        List<TmpFile> fileList = (List) pm.newQuery(query).execute();
        if (fileList != null && fileList.size() > 0 && fileList.get(fileList.size() - 1).isFinalSplit()) {
            status = "FINAL";
        } else if (fileList == null || fileList.size() < batchSize) {
            status = "WAIT";
        }
        return status;
    }

    public void fetchIntoStream(String fileNameKey, int batchNo, int batchSize, OutputStream out) throws IOException {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            int startIndex = batchSize * batchNo;
            int endIndex = startIndex + (batchSize - 1);
            String query = "select from " + TmpFile.class.getName() + " where " + TmpFile.COL_KEY + " == '" + fileNameKey + "'" + " && " + TmpFile.COL_SPLIT_NO + ">=" + startIndex + " && " + TmpFile.COL_SPLIT_NO + "<=" + endIndex + " order by " + TmpFile.COL_SPLIT_NO;
            List<TmpFile> fileList = (List) pm.newQuery(query).execute();
            logger.logIt("fileList.size()=" + fileList.size() + ";startIndex=" + startIndex + ";endIndex=" + endIndex);
            TmpFile tmpFile;
            for (int i = 0; i < fileList.size(); i++) {
                tmpFile = fileList.get(i);
                logger.logIt("Writing contents of " + tmpFile.toString() + "into outstream");
                out.write(tmpFile.getData().getBytes());
                out.flush();
            }
        } finally {
            if (!pm.isClosed()) {
                pm.close();
            }
        }
    }

    public String getFileName(String fileNameKey) {
        List<TmpFile> fileList = null;
        String name = null;
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            String query = "select from " + TmpFile.class.getName() + " where " + TmpFile.COL_KEY + " == '" + fileNameKey + "' order by " + TmpFile.COL_SPLIT_NO;
            fileList = (List) pm.newQuery(query).execute();
            if (fileList.size() > 0) {
                name = fileList.get(0).getName();
            }
        } finally {
            if (!pm.isClosed()) {
                pm.close();
            }
        }
        return name;
    }

    public boolean deleteFile(String fileNameKey, int batchNo, int batchSize) {
        boolean wasDeleted = true;
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            int startIndex = batchSize * batchNo;
            int endIndex = startIndex + (batchSize - 1);
            Query query = pm.newQuery(TmpFile.class);
            query.setFilter(TmpFile.COL_KEY + " == '" + fileNameKey + "'");
            query.setFilter(TmpFile.COL_SPLIT_NO + " >= " + startIndex);
            query.setFilter(TmpFile.COL_SPLIT_NO + " <= " + endIndex);
            query.deletePersistentAll();
        } catch (Exception e) {
            e.printStackTrace();
            wasDeleted = false;
        } finally {
            if (!pm.isClosed()) {
                pm.close();
            }
        }
        return wasDeleted;
    }

    public void saveFile(InputStream in, String fileName, String fileKey, ProgressListener listener, int batchNo, long maxBatchSize, boolean isFinalBatch) throws IOException {
        int splitNo = 0;
        int readBytes = 0;
        byte buffer[];
        StringBuffer logMessage = new StringBuffer("Saved Entries:\n");
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        Date start = new Date();
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            if (batchNo > 0) {
                int maxEntries = (int) (maxBatchSize / MAX_BLOB_SIZE);
                splitNo = maxEntries * batchNo;
                String query = "select from " + TmpFile.class.getName() + " where " + TmpFile.COL_KEY + " == '" + fileKey + "'" + " && " + TmpFile.COL_SPLIT_NO + ">=" + splitNo + " && " + TmpFile.COL_SPLIT_NO + "<" + (splitNo + maxEntries);
                List<TmpFile> fileList = (List) pm.newQuery(query).execute();
                if (fileList.size() > 0) {
                    pm.deletePersistentAll(fileList);
                    logger.logIt("Removed existing entries from " + splitNo + " to " + (splitNo + fileList.size() - 1));
                }
                listener.resetCurrentSize(splitNo * MAX_BLOB_SIZE);
                logger.logIt("Starting with splitNo:" + splitNo);
            } else {
                logger.logIt("First batch, so Starting with splitNo:" + splitNo);
            }
            do {
                buffer = new byte[MAX_BLOB_SIZE];
                readBytes = in.read(buffer);
                if (readBytes < 0) {
                    if (splitNo == 0) {
                        persist(fileKey, new byte[0], fileName, splitNo, true);
                    }
                    break;
                } else {
                    while (readBytes > -1 && byteStream.size() < MAX_BLOB_SIZE) {
                        byteStream.write(buffer, 0, readBytes);
                        listener.updateStatus(readBytes);
                        buffer = new byte[MAX_BLOB_SIZE - byteStream.size()];
                        readBytes = in.read(buffer);
                    }
                    logMessage.append("SplitNo:").append(splitNo);
                    logMessage.append(", bufferSize:").append(byteStream.size());
                    logMessage.append(", readBytes:").append(readBytes);
                    boolean isFinalPart = (isFinalBatch && readBytes < 0) ? true : false;
                    logMessage.append(", isFinalPart:").append(isFinalPart);
                    logMessage.append(", Start at:").append(new Date());
                    persist(fileKey, byteStream.toByteArray(), fileName, splitNo, isFinalPart);
                    logMessage.append(", End at:").append(new Date()).append("\n");
                    byteStream = new ByteArrayOutputStream();
                    splitNo++;
                }
            } while (readBytes >= 0 && splitNo < MAX_NO_OF_SPLITS);
        } finally {
            if (!pm.isClosed()) {
                pm.close();
            }
            logger.logIt(logMessage.toString());
        }
        Date end = new Date();
        logger.logIt("writeFile - time taken (ms):" + (end.getTime() - start.getTime()));
    }
}
