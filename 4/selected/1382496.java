package net.sf.mzmine.main;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.logging.Level;
import java.util.logging.Logger;

class TmpFileCleanup {

    private static Logger logger = Logger.getLogger(TmpFileCleanup.class.getName());

    static void removeOldTemporaryFiles() {
        logger.fine("Checking for old temporary files...");
        try {
            File tempDir = new File(System.getProperty("java.io.tmpdir"));
            File remainingTmpFiles[] = tempDir.listFiles(new FilenameFilter() {

                public boolean accept(File dir, String name) {
                    return name.matches("mzmine.*\\.scans");
                }
            });
            if (remainingTmpFiles != null) for (File remainingTmpFile : remainingTmpFiles) {
                if (!remainingTmpFile.canWrite()) continue;
                RandomAccessFile rac = new RandomAccessFile(remainingTmpFile, "rw");
                FileLock lock = rac.getChannel().tryLock();
                rac.close();
                if (lock != null) {
                    logger.finest("Removing unused temporary file " + remainingTmpFile);
                    remainingTmpFile.delete();
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error while checking for old temporary files", e);
        }
    }
}
