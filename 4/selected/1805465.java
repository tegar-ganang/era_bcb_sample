package boccaccio.andrea.mySimpleSynchronizer.filemanager;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Logger;
import boccaccio.andrea.myfilesutils.directorymaker.DirectoryMakerFactory;
import boccaccio.andrea.myfilesutils.directorymaker.IDirectoryMaker;

/**
 * @author Andrea Boccaccio
 *
 */
public class ConcreteSingleThreadOverwriteConflictsIterativeFileManagerGoOnOnIOException extends AbsOverwriteConflictsIterativeFileManager {

    private static ConcreteSingleThreadOverwriteConflictsIterativeFileManagerGoOnOnIOException instance = null;

    private ConcreteSingleThreadOverwriteConflictsIterativeFileManagerGoOnOnIOException() {
        this.setAlgorithm("overwriteConflictsIterativeFileManager");
    }

    public static ConcreteSingleThreadOverwriteConflictsIterativeFileManagerGoOnOnIOException getInstance() {
        if (instance == null) instance = new ConcreteSingleThreadOverwriteConflictsIterativeFileManagerGoOnOnIOException();
        return instance;
    }

    @Override
    protected void iteration(File src, File dst) throws NoSuchAlgorithmException, IOException {
        IDirectoryMaker idm = DirectoryMakerFactory.getInstance().getDirectoryMaker();
        String strSrcPath = src.getAbsolutePath();
        String strDstPath = dst.getAbsolutePath();
        Logger log = Logger.getLogger(AbsFileManager.class.getPackage().getName());
        try {
            log.debug("Computing couple " + strSrcPath + " " + strDstPath + " .");
            if (src.isFile()) {
                log.trace(strSrcPath + " is a file making necessary infrastructure.");
                idm.makeDirectory(dst.getParent());
                this.manageOnlyFile(src, dst);
            } else if (src.isDirectory()) {
                log.trace(strSrcPath + " is a directory making necessary infrastructure.");
                idm.makeDirectory(dst);
            }
            log.debug("OK");
        } catch (IOException ioe) {
            StackTraceElement ste[] = ioe.getStackTrace();
            log.error("Unlocking error");
            log.error(strSrcPath + " " + strDstPath);
            log.fatal(ioe.toString());
            for (int i = 0; i < ste.length; ++i) log.fatal(ste[i].toString());
        }
    }
}
