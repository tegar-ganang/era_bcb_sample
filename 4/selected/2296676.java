package org.apache.hadoop.hdfs.server.datanode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.hadoop.hdfs.protocol.Block;
import org.apache.hadoop.hdfs.protocol.FSConstants;
import org.apache.hadoop.hdfs.server.common.HdfsConstants.NodeType;
import org.apache.hadoop.hdfs.server.common.HdfsConstants.StartupOption;
import org.apache.hadoop.hdfs.server.common.HdfsConstants;
import org.apache.hadoop.hdfs.server.common.InconsistentFSStateException;
import org.apache.hadoop.hdfs.server.common.Storage;
import org.apache.hadoop.hdfs.server.common.StorageInfo;
import org.apache.hadoop.hdfs.server.protocol.NamespaceInfo;
import org.apache.hadoop.util.Daemon;
import org.apache.hadoop.fs.FileUtil.HardLink;
import org.apache.hadoop.io.IOUtils;

/** 
 * Data storage information file.
 * <p>
 * @see Storage
 */
public class DataStorage extends Storage {

    static final String BLOCK_SUBDIR_PREFIX = "subdir";

    static final String BLOCK_FILE_PREFIX = "blk_";

    static final String COPY_FILE_PREFIX = "dncp_";

    private String storageID;

    DataStorage() {
        super(NodeType.DATA_NODE);
        storageID = "";
    }

    DataStorage(int nsID, long cT, String strgID) {
        super(NodeType.DATA_NODE, nsID, cT);
        this.storageID = strgID;
    }

    public DataStorage(StorageInfo storageInfo, String strgID) {
        super(NodeType.DATA_NODE, storageInfo);
        this.storageID = strgID;
    }

    public String getStorageID() {
        return storageID;
    }

    void setStorageID(String newStorageID) {
        this.storageID = newStorageID;
    }

    /**
   * Analyze storage directories.
   * Recover from previous transitions if required. 
   * Perform fs state transition if necessary depending on the namespace info.
   * Read storage info. 
   * 
   * @param nsInfo namespace information
   * @param dataDirs array of data storage directories
   * @param startOpt startup option
   * @throws IOException
   */
    void recoverTransitionRead(NamespaceInfo nsInfo, Collection<File> dataDirs, StartupOption startOpt) throws IOException {
        assert FSConstants.LAYOUT_VERSION == nsInfo.getLayoutVersion() : "Data-node and name-node layout versions must be the same.";
        this.storageID = "";
        this.storageDirs = new ArrayList<StorageDirectory>(dataDirs.size());
        ArrayList<StorageState> dataDirStates = new ArrayList<StorageState>(dataDirs.size());
        for (Iterator<File> it = dataDirs.iterator(); it.hasNext(); ) {
            File dataDir = it.next();
            StorageDirectory sd = new StorageDirectory(dataDir);
            StorageState curState;
            try {
                curState = sd.analyzeStorage(startOpt);
                switch(curState) {
                    case NORMAL:
                        break;
                    case NON_EXISTENT:
                        LOG.info("Storage directory " + dataDir + " does not exist.");
                        it.remove();
                        continue;
                    case NOT_FORMATTED:
                        LOG.info("Storage directory " + dataDir + " is not formatted.");
                        LOG.info("Formatting ...");
                        format(sd, nsInfo);
                        break;
                    default:
                        sd.doRecover(curState);
                }
            } catch (IOException ioe) {
                sd.unlock();
                throw ioe;
            }
            addStorageDir(sd);
            dataDirStates.add(curState);
        }
        if (dataDirs.size() == 0) throw new IOException("All specified directories are not accessible or do not exist.");
        for (int idx = 0; idx < getNumStorageDirs(); idx++) {
            doTransition(getStorageDir(idx), nsInfo, startOpt);
            assert this.getLayoutVersion() == nsInfo.getLayoutVersion() : "Data-node and name-node layout versions must be the same.";
            assert this.getCTime() == nsInfo.getCTime() : "Data-node and name-node CTimes must be the same.";
        }
        this.writeAll();
    }

    void format(StorageDirectory sd, NamespaceInfo nsInfo) throws IOException {
        sd.clearDirectory();
        this.layoutVersion = FSConstants.LAYOUT_VERSION;
        this.namespaceID = nsInfo.getNamespaceID();
        this.cTime = 0;
        sd.write();
    }

    protected void setFields(Properties props, StorageDirectory sd) throws IOException {
        super.setFields(props, sd);
        props.setProperty("storageID", storageID);
    }

    protected void getFields(Properties props, StorageDirectory sd) throws IOException {
        super.getFields(props, sd);
        String ssid = props.getProperty("storageID");
        if (ssid == null || !("".equals(storageID) || "".equals(ssid) || storageID.equals(ssid))) throw new InconsistentFSStateException(sd.getRoot(), "has incompatible storage Id.");
        if ("".equals(storageID)) storageID = ssid;
    }

    public boolean isConversionNeeded(StorageDirectory sd) throws IOException {
        File oldF = new File(sd.getRoot(), "storage");
        if (!oldF.exists()) return false;
        RandomAccessFile oldFile = new RandomAccessFile(oldF, "rws");
        FileLock oldLock = oldFile.getChannel().tryLock();
        try {
            oldFile.seek(0);
            int oldVersion = oldFile.readInt();
            if (oldVersion < LAST_PRE_UPGRADE_LAYOUT_VERSION) return false;
        } finally {
            oldLock.release();
            oldFile.close();
        }
        return true;
    }

    /**
   * Analize which and whether a transition of the fs state is required
   * and perform it if necessary.
   * 
   * Rollback if previousLV >= LAYOUT_VERSION && prevCTime <= namenode.cTime
   * Upgrade if this.LV > LAYOUT_VERSION || this.cTime < namenode.cTime
   * Regular startup if this.LV = LAYOUT_VERSION && this.cTime = namenode.cTime
   * 
   * @param sd  storage directory
   * @param nsInfo  namespace info
   * @param startOpt  startup option
   * @throws IOException
   */
    private void doTransition(StorageDirectory sd, NamespaceInfo nsInfo, StartupOption startOpt) throws IOException {
        if (startOpt == StartupOption.ROLLBACK) doRollback(sd, nsInfo);
        sd.read();
        checkVersionUpgradable(this.layoutVersion);
        assert this.layoutVersion >= FSConstants.LAYOUT_VERSION : "Future version is not allowed";
        if (getNamespaceID() != nsInfo.getNamespaceID()) throw new IOException("Incompatible namespaceIDs in " + sd.getRoot().getCanonicalPath() + ": namenode namespaceID = " + nsInfo.getNamespaceID() + "; datanode namespaceID = " + getNamespaceID());
        if (this.layoutVersion == FSConstants.LAYOUT_VERSION && this.cTime == nsInfo.getCTime()) return;
        verifyDistributedUpgradeProgress(nsInfo);
        if (this.layoutVersion > FSConstants.LAYOUT_VERSION || this.cTime < nsInfo.getCTime()) {
            doUpgrade(sd, nsInfo);
            return;
        }
        throw new IOException("Datanode state: LV = " + this.getLayoutVersion() + " CTime = " + this.getCTime() + " is newer than the namespace state: LV = " + nsInfo.getLayoutVersion() + " CTime = " + nsInfo.getCTime());
    }

    /**
   * Move current storage into a backup directory,
   * and hardlink all its blocks into the new current directory.
   * 
   * @param sd  storage directory
   * @throws IOException
   */
    void doUpgrade(StorageDirectory sd, NamespaceInfo nsInfo) throws IOException {
        LOG.info("Upgrading storage directory " + sd.getRoot() + ".\n   old LV = " + this.getLayoutVersion() + "; old CTime = " + this.getCTime() + ".\n   new LV = " + nsInfo.getLayoutVersion() + "; new CTime = " + nsInfo.getCTime());
        File curDir = sd.getCurrentDir();
        File prevDir = sd.getPreviousDir();
        assert curDir.exists() : "Current directory must exist.";
        if (prevDir.exists()) deleteDir(prevDir);
        File tmpDir = sd.getPreviousTmp();
        assert !tmpDir.exists() : "previous.tmp directory must not exist.";
        rename(curDir, tmpDir);
        linkBlocks(tmpDir, curDir, this.getLayoutVersion());
        this.layoutVersion = FSConstants.LAYOUT_VERSION;
        assert this.namespaceID == nsInfo.getNamespaceID() : "Data-node and name-node layout versions must be the same.";
        this.cTime = nsInfo.getCTime();
        sd.write();
        rename(tmpDir, prevDir);
        LOG.info("Upgrade of " + sd.getRoot() + " is complete.");
    }

    void doRollback(StorageDirectory sd, NamespaceInfo nsInfo) throws IOException {
        File prevDir = sd.getPreviousDir();
        if (!prevDir.exists()) return;
        DataStorage prevInfo = new DataStorage();
        StorageDirectory prevSD = prevInfo.new StorageDirectory(sd.getRoot());
        prevSD.read(prevSD.getPreviousVersionFile());
        if (!(prevInfo.getLayoutVersion() >= FSConstants.LAYOUT_VERSION && prevInfo.getCTime() <= nsInfo.getCTime())) throw new InconsistentFSStateException(prevSD.getRoot(), "Cannot rollback to a newer state.\nDatanode previous state: LV = " + prevInfo.getLayoutVersion() + " CTime = " + prevInfo.getCTime() + " is newer than the namespace state: LV = " + nsInfo.getLayoutVersion() + " CTime = " + nsInfo.getCTime());
        LOG.info("Rolling back storage directory " + sd.getRoot() + ".\n   target LV = " + nsInfo.getLayoutVersion() + "; target CTime = " + nsInfo.getCTime());
        File tmpDir = sd.getRemovedTmp();
        assert !tmpDir.exists() : "removed.tmp directory must not exist.";
        File curDir = sd.getCurrentDir();
        assert curDir.exists() : "Current directory must exist.";
        rename(curDir, tmpDir);
        rename(prevDir, curDir);
        deleteDir(tmpDir);
        LOG.info("Rollback of " + sd.getRoot() + " is complete.");
    }

    void doFinalize(StorageDirectory sd) throws IOException {
        File prevDir = sd.getPreviousDir();
        if (!prevDir.exists()) return;
        final String dataDirPath = sd.getRoot().getCanonicalPath();
        LOG.info("Finalizing upgrade for storage directory " + dataDirPath + ".\n   cur LV = " + this.getLayoutVersion() + "; cur CTime = " + this.getCTime());
        assert sd.getCurrentDir().exists() : "Current directory must exist.";
        final File tmpDir = sd.getFinalizedTmp();
        rename(prevDir, tmpDir);
        new Daemon(new Runnable() {

            public void run() {
                try {
                    deleteDir(tmpDir);
                } catch (IOException ex) {
                    LOG.error("Finalize upgrade for " + dataDirPath + " failed.", ex);
                }
                LOG.info("Finalize upgrade for " + dataDirPath + " is complete.");
            }

            public String toString() {
                return "Finalize " + dataDirPath;
            }
        }).start();
    }

    void finalizeUpgrade() throws IOException {
        for (Iterator<StorageDirectory> it = storageDirs.iterator(); it.hasNext(); ) {
            doFinalize(it.next());
        }
    }

    static void linkBlocks(File from, File to, int oldLV) throws IOException {
        if (!from.isDirectory()) {
            if (from.getName().startsWith(COPY_FILE_PREFIX)) {
                IOUtils.copyBytes(new FileInputStream(from), new FileOutputStream(to), 16 * 1024, true);
            } else {
                if (oldLV >= PRE_GENERATIONSTAMP_LAYOUT_VERSION) {
                    to = new File(convertMetatadataFileName(to.getAbsolutePath()));
                }
                HardLink.createHardLink(from, to);
            }
            return;
        }
        if (!to.mkdir()) throw new IOException("Cannot create directory " + to);
        String[] blockNames = from.list(new java.io.FilenameFilter() {

            public boolean accept(File dir, String name) {
                return name.startsWith(BLOCK_SUBDIR_PREFIX) || name.startsWith(BLOCK_FILE_PREFIX) || name.startsWith(COPY_FILE_PREFIX);
            }
        });
        for (int i = 0; i < blockNames.length; i++) linkBlocks(new File(from, blockNames[i]), new File(to, blockNames[i]), oldLV);
    }

    protected void corruptPreUpgradeStorage(File rootDir) throws IOException {
        File oldF = new File(rootDir, "storage");
        if (oldF.exists()) return;
        if (!oldF.createNewFile()) throw new IOException("Cannot create file " + oldF);
        RandomAccessFile oldFile = new RandomAccessFile(oldF, "rws");
        try {
            writeCorruptedData(oldFile);
        } finally {
            oldFile.close();
        }
    }

    private void verifyDistributedUpgradeProgress(NamespaceInfo nsInfo) throws IOException {
        UpgradeManagerDatanode um = DataNode.getDataNode().upgradeManager;
        assert um != null : "DataNode.upgradeManager is null.";
        um.setUpgradeState(false, getLayoutVersion());
        um.initializeUpgrade(nsInfo);
    }

    private static final Pattern PRE_GENSTAMP_META_FILE_PATTERN = Pattern.compile("(.*blk_[-]*\\d+)\\.meta$");

    /**
   * This is invoked on target file names when upgrading from pre generation 
   * stamp version (version -13) to correct the metatadata file name.
   * @param oldFileName
   * @return the new metadata file name with the default generation stamp.
   */
    private static String convertMetatadataFileName(String oldFileName) {
        Matcher matcher = PRE_GENSTAMP_META_FILE_PATTERN.matcher(oldFileName);
        if (matcher.matches()) {
            return FSDataset.getMetaFileName(matcher.group(1), Block.GRANDFATHER_GENERATION_STAMP);
        }
        return oldFileName;
    }
}
