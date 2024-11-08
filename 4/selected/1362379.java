package com.townsfolkdesigns.lucene.jedit;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.gjt.sp.util.Log;
import com.townsfolkdesigns.lucene.jedit.manager.IndexManager;
import com.townsfolkdesigns.lucene.jedit.manager.IndexStatsManager;
import com.townsfolkdesigns.lucene.jedit.manager.OptionsManager;
import com.townsfolkdesigns.lucene.parser.DefaultFileDocumentParser;

/**
 * @author elberry
 * 
 */
public class LucenePluginIndexer extends JEditFileTypeDelegatingIndexer {

    private static final OptionsManager optionsManager = OptionsManager.getInstance();

    private IndexStatsManager indexStatsManager;

    public LucenePluginIndexer() {
    }

    public long getIndexInterval() {
        return optionsManager.getIndexInterval();
    }

    public IndexStatsManager getIndexStatsManager() {
        return indexStatsManager;
    }

    @Override
    public void index() {
        List<String> directories = optionsManager.getDirectories();
        String[] locations = directories.toArray(new String[0]);
        setLocations(locations);
        Log.log(Log.DEBUG, this, "Indexing - locations: " + Arrays.toString(locations));
        Date startDate = new Date();
        long startTime = startDate.getTime();
        indexStatsManager.setIndexStartTime(startDate);
        indexStatsManager.setIndexing(true);
        super.index();
        Date endDate = new Date();
        long endTime = endDate.getTime();
        long indexingTime = endTime - startTime;
        indexStatsManager.setIndexEndTime(endDate);
        indexStatsManager.setDirectoriesIndexed(getDirectoriesIndexed());
        indexStatsManager.setFilesIndexed(getFilesIndexed());
        Log.log(Log.DEBUG, this, "Indexing complete - time: " + indexingTime + " | directories: " + getDirectoriesIndexed() + " | files: " + getFilesIndexed());
        File indexStoreDir = new LucenePlugin().getIndexStoreDirectory();
        indexStoreDir = new File(indexStoreDir, "LucenePlugin");
        File readIndexLocation = new File(indexStoreDir, "read");
        File writeIndexLocation = new File(indexStoreDir, "write");
        if (readIndexLocation.exists()) {
            Log.log(Log.DEBUG, this, "Replacing old index files with new ones.");
            replaceReadOnlyIndex(readIndexLocation, writeIndexLocation);
        } else {
            Log.log(Log.DEBUG, this, "Couldn't find old index files, moving new ones in place.");
            copyWriteIndexToReadIndex(readIndexLocation, writeIndexLocation);
        }
    }

    private void copyWriteIndexToReadIndex(File readIndexLocation, File writeIndexLocation) {
        IndexManager.getInstance().aquireWriteLock();
        String indexFileName = null;
        File renameFile = null;
        for (File indexFile : writeIndexLocation.listFiles()) {
            if (indexFile.isFile()) {
                indexFileName = indexFile.getName();
                renameFile = new File(readIndexLocation, indexFileName);
                indexFile.renameTo(renameFile);
            }
        }
        IndexManager.getInstance().releaseWriteLock();
    }

    private void replaceReadOnlyIndex(File readIndexLocation, File writeIndexLocation) {
        IndexManager.getInstance().aquireWriteLock();
        for (File indexFile : readIndexLocation.listFiles()) {
            if (indexFile.isFile()) {
                indexFile.delete();
            }
        }
        copyWriteIndexToReadIndex(readIndexLocation, writeIndexLocation);
        IndexManager.getInstance().releaseWriteLock();
    }

    @Override
    public void init() {
        File indexStoreDir = new LucenePlugin().getIndexStoreDirectory();
        if (!indexStoreDir.exists()) {
            indexStoreDir.mkdirs();
        }
        File indexStoreFile = new File(indexStoreDir, "LucenePlugin");
        indexStoreFile = new File(indexStoreFile, "write");
        setIndexStoreDirectory(indexStoreFile);
        setDefaultDocumentParser(new DefaultFileDocumentParser());
        setIndexStatsManager(new IndexStatsManager());
        setRecursivelyIndexDirectoriesOn(true);
        super.init();
    }

    public void setIndexStatsManager(IndexStatsManager indexStatsManager) {
        this.indexStatsManager = indexStatsManager;
    }
}
