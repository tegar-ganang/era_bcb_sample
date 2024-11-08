package gg.arkehion.store.hadoopimpl;

import gg.arkehion.configuration.Configuration;
import gg.arkehion.exceptions.ArFileException;
import gg.arkehion.exceptions.ArUnvalidIndexException;
import gg.arkehion.store.ArkDirConstants;
import gg.arkehion.store.ArkLegacyInterface;
import gg.arkehion.store.NbFilesAndOutputFiles;
import gg.arkehion.store.abstimpl.ArkAbstractDirFunction;
import gg.arkehion.utils.ArConstants;
import goldengate.common.digest.FilesystemBasedDigest;
import goldengate.common.logging.GgInternalLogger;
import goldengate.common.logging.GgInternalLoggerFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import org.apache.hadoop.fs.ContentSummary;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FsStatus;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.hadoop.fs.permission.FsAction;

/**
 * @author frederic
 * 
 */
public class ArkHadoopDirFunction extends ArkAbstractDirFunction {

    /**
     * Internal Logger
     */
    private static final GgInternalLogger logger = GgInternalLoggerFactory.getLogger(ArkHadoopDirFunction.class);

    /**
     * @param separator
     */
    public ArkHadoopDirFunction() {
        super('/');
    }

    /**
     * 
     * @param dfs
     * @param path
     * @return same as getGlobalUsedAndFreeSpaces(ArkLegacyInterface legacy,
     *         String storage)
     */
    protected final double[] getGlobalUsedAndFreeSpaces(FileSystem dfs, Path path) {
        double[] size = new double[3];
        FsStatus status = null;
        try {
            status = dfs.getStatus(path);
        } catch (IOException e) {
            size[0] = size[1] = size[2] = 0;
            return size;
        }
        size[0] = status.getCapacity() / 1024.0;
        size[1] = status.getUsed() / 1024.0;
        size[2] = status.getRemaining() / 1024.0;
        return size;
    }

    @Override
    public final double[] getGlobalUsedAndFreeSpaces(ArkLegacyInterface legacy, String storage) {
        Path path = new Path(((ArkHadoopLegacy) legacy).getBasePath() + storage);
        return getGlobalUsedAndFreeSpaces(((ArkHadoopLegacy) legacy).getFileSystem(), path);
    }

    /**
     * 
     * @param dfs
     * @param path
     * @return same as getUsedSpaceNbDirNbFile(ArkLegacyInterface legacy, String
     *         storage)
     */
    protected final double[] getUsedSpaceNbDirNbFile(FileSystem dfs, Path path) {
        double[] size = new double[3];
        size[0] = 0.0;
        size[1] = 0.0;
        size[2] = 0.0;
        ContentSummary contentSummary = null;
        try {
            contentSummary = dfs.getContentSummary(path);
            size[0] = contentSummary.getSpaceConsumed() / 1024.0;
            size[1] = contentSummary.getDirectoryCount();
            size[2] = contentSummary.getFileCount();
        } catch (IOException e) {
        }
        return size;
    }

    @Override
    public final double[] getUsedSpaceNbDirNbFile(ArkLegacyInterface legacy, String storage) {
        Path path = new Path(((ArkHadoopLegacy) legacy).getBasePath() + storage);
        return getUsedSpaceNbDirNbFile(((ArkHadoopLegacy) legacy).getFileSystem(), path);
    }

    /**
     * Create the directory associated with the String path
     * 
     * @param dfs
     * @param path
     * @return True if created, False else.
     * @throws ArFileException
     */
    protected final boolean createDir(FileSystem dfs, Path path) throws ArFileException {
        try {
            FileStatus status = dfs.getFileStatus(path);
            if (status.isDirectory()) return true;
        } catch (IOException e1) {
        }
        try {
            return dfs.mkdirs(path);
        } catch (IOException e) {
            throw new ArFileException("Cannot create Directory", e);
        }
    }

    /**
     * Delete the directory associated with the path if empty
     * 
     * @param dfs
     * @param path
     * @return True if deleted, False else.
     * @throws ArFileException
     */
    protected final boolean deleteDir(FileSystem dfs, Path path) throws ArFileException {
        if (path == null) {
            return true;
        }
        try {
            return dfs.delete(path, false);
        } catch (FileNotFoundException e) {
            return true;
        } catch (IOException e1) {
            throw new ArFileException("Cannot delete Directory", e1);
        }
    }

    /**
     * Delete the directory and its subdirs associated with the path if empty
     * 
     * @param dfs
     * @param path
     * @return True if deleted, False else.
     * @throws ArFileException
     */
    public final boolean deleteRecursiveDir(FileSystem dfs, Path path) throws ArFileException {
        if (path == null) {
            return true;
        }
        ContentSummary contentSummary = null;
        try {
            contentSummary = dfs.getContentSummary(path);
            logger.debug("DelRec: " + path + " : " + contentSummary.getFileCount());
            if (contentSummary.getFileCount() > 0) {
                return false;
            }
        } catch (FileNotFoundException e) {
            return true;
        } catch (IOException e) {
            throw new ArFileException("Cannot delete Directory", e);
        }
        try {
            return dfs.delete(path, true);
        } catch (IOException e1) {
            throw new ArFileException("Cannot delete Directory", e1);
        }
    }

    /**
     * Returns True if the directory is empty (no file), False if not empty or
     * not exist or is not a directory.
     * 
     * @param dfs
     * @param path
     * @return True if the directory is empty, else False.
     * @throws ArFileException
     */
    protected final boolean isDirEmpty(FileSystem dfs, Path path) throws ArFileException {
        if (path == null) {
            return true;
        }
        ContentSummary contentSummary = null;
        try {
            contentSummary = dfs.getContentSummary(path);
            return (contentSummary.getFileCount() == 0);
        } catch (FileNotFoundException e) {
            return true;
        } catch (IOException e) {
            throw new ArFileException("Cannot check Directory", e);
        }
    }

    @Override
    public final long getListOfFiles(ArkLegacyInterface legacy, File out, long maxInFile, String path, long refTime) {
        if (path == null) {
            return -1;
        }
        Path path2 = new Path(((ArkHadoopLegacy) legacy).getBasePath() + path);
        NbFilesAndOutputFiles nbFilesAndOutputFiles = null;
        RemoteIterator<LocatedFileStatus> iterator = null;
        try {
            iterator = ((ArkHadoopLegacy) legacy).getFileSystem().listFiles(path2, true);
            nbFilesAndOutputFiles = new NbFilesAndOutputFiles(out, maxInFile);
            if (!nbFilesAndOutputFiles.newFileOutputStream()) {
                logger.debug("Not open output");
                return -1;
            }
            String sleg = Long.toString(legacy.getLID());
            while (iterator.hasNext()) {
                LocatedFileStatus locatedFileStatus = iterator.next();
                if (locatedFileStatus.isFile()) {
                    if (locatedFileStatus.getPath().getName().endsWith(ArkDirConstants.METAEXT)) {
                        continue;
                    }
                    if (refTime == 0 || locatedFileStatus.getModificationTime() > refTime) {
                        long[] sublist;
                        try {
                            sublist = pathToIdUnique(locatedFileStatus.getPath().toUri().getPath());
                        } catch (ArUnvalidIndexException e) {
                            continue;
                        }
                        if ((sublist != null) && (sublist.length >= 2)) {
                            nbFilesAndOutputFiles.checkOutputAddFile();
                            int length = sublist.length;
                            nbFilesAndOutputFiles.writeCheckFilesMark(sleg, Long.toString(sublist[length - 2]), Long.toString(sublist[length - 1]), null, null);
                        }
                        sublist = null;
                    }
                }
            }
        } catch (FileNotFoundException e1) {
            if (nbFilesAndOutputFiles != null) nbFilesAndOutputFiles.close();
            logger.debug("File not found", e1);
            return -1;
        } catch (IOException e1) {
            if (nbFilesAndOutputFiles != null) nbFilesAndOutputFiles.close();
            logger.debug("Error while reading", e1);
            return -1;
        }
        nbFilesAndOutputFiles.close();
        return nbFilesAndOutputFiles.outputFile;
    }

    @Override
    public final long getListOfFilesMark(ArkLegacyInterface legacy, File out, long maxInFile, String path, long refTime) {
        if (path == null) {
            return -1;
        }
        Path path2 = new Path(((ArkHadoopLegacy) legacy).getBasePath() + path);
        NbFilesAndOutputFiles nbFilesAndOutputFiles = null;
        RemoteIterator<LocatedFileStatus> iterator = null;
        try {
            iterator = ((ArkHadoopLegacy) legacy).getFileSystem().listFiles(path2, true);
            nbFilesAndOutputFiles = new NbFilesAndOutputFiles(out, maxInFile);
            if (!nbFilesAndOutputFiles.newFileOutputStream()) {
                logger.debug("Not open output");
                return -1;
            }
            String sleg = Long.toString(legacy.getLID());
            while (iterator.hasNext()) {
                LocatedFileStatus locatedFileStatus = iterator.next();
                if (locatedFileStatus.isFile()) {
                    if (locatedFileStatus.getPath().getName().endsWith(ArkDirConstants.METAEXT)) {
                        continue;
                    }
                    if (refTime == 0 || locatedFileStatus.getModificationTime() > refTime) {
                        long[] sublist;
                        try {
                            sublist = pathToIdUnique(locatedFileStatus.getPath().toUri().getPath());
                        } catch (ArUnvalidIndexException e) {
                            continue;
                        }
                        if ((sublist != null) && (sublist.length >= 2)) {
                            nbFilesAndOutputFiles.checkOutputAddFile();
                            String chksum = computeStringMark((ArkHadoopLegacy) legacy, locatedFileStatus.getPath());
                            if (chksum == null) {
                                logger.debug("noMark for " + sublist[sublist.length - 1] + ' ' + sublist[sublist.length - 2]);
                                continue;
                            }
                            int length = sublist.length;
                            nbFilesAndOutputFiles.writeCheckFilesMark(sleg, Long.toString(sublist[length - 2]), Long.toString(sublist[length - 1]), chksum, null);
                        }
                        sublist = null;
                    }
                }
            }
        } catch (FileNotFoundException e1) {
            if (nbFilesAndOutputFiles != null) nbFilesAndOutputFiles.close();
            logger.debug("File not found", e1);
            return -1;
        } catch (IOException e1) {
            if (nbFilesAndOutputFiles != null) nbFilesAndOutputFiles.close();
            logger.debug("Error while reading", e1);
            return -1;
        }
        nbFilesAndOutputFiles.close();
        return nbFilesAndOutputFiles.outputFile;
    }

    /**
     * 
     * @param legacy
     * @param file
     * @param blocksize
     * @return the corresponding InputStream
     * @throws ArFileException
     */
    protected final InputStream getInputStreamInternal(ArkHadoopLegacy legacy, Path file, int blocksize) throws ArFileException {
        if (file == null) {
            throw new ArFileException("Path uncorrect");
        }
        FSDataInputStream fsDataInputStream = null;
        try {
            fsDataInputStream = legacy.getFileSystem().open(file, blocksize);
        } catch (IOException e) {
            throw new ArFileException("File cannot be read: " + file, e);
        }
        if (legacy.isEncrypted()) {
            Cipher cipherIn = legacy.getKeyObject().toDecrypt();
            if (cipherIn == null) {
                throw new ArFileException("CIpher uncorrect");
            }
            CipherInputStream cipherInputStream = new CipherInputStream(fsDataInputStream, cipherIn);
            return cipherInputStream;
        } else {
            return fsDataInputStream;
        }
    }

    /**
     * 
     * @param legacy
     * @param file
     * @param blocksize
     * @return the corresponding OutputStream
     * @throws ArFileException
     */
    protected final OutputStream getOutputStreamInternal(ArkHadoopLegacy legacy, Path file, int blocksize) throws ArFileException {
        if (file == null) {
            throw new ArFileException("Path uncorrect");
        }
        FSDataOutputStream fsDataOutputStream = null;
        try {
            fsDataOutputStream = legacy.getFileSystem().create(file, false, blocksize);
        } catch (IOException e) {
            throw new ArFileException("File cannot be written: " + file, e);
        }
        if (legacy.isEncrypted()) {
            Cipher cipherOut = legacy.getKeyObject().toCrypt();
            if (cipherOut == null) {
                throw new ArFileException("CIpher uncorrect");
            }
            CipherOutputStream cipherOutputStream = new CipherOutputStream(fsDataOutputStream, cipherOut);
            return cipherOutputStream;
        } else {
            return fsDataOutputStream;
        }
    }

    /**
     * Compute the Mark (MD5 or SHA1 or CRC32 or ADLER32) in String Hex for the
     * file
     * 
     * @param file
     * @return the Mark in a String Hex format or NULL if an error occurs
     */
    protected final String computeStringMarkInternal(ArkHadoopLegacy legacy, Path file) {
        if (file == null) {
            return null;
        }
        InputStream inputStream = null;
        try {
            inputStream = getInputStream(legacy, file, ArConstants.BUFFERSIZEDEFAULT);
        } catch (ArFileException e1) {
            logger.warn("Cannot open InputStream", e1);
            return null;
        }
        byte[] bmd5;
        try {
            bmd5 = FilesystemBasedDigest.getHash(inputStream, Configuration.algoMark);
        } catch (IOException e1) {
            logger.warn("Cannot read InputStream", e1);
            bmd5 = null;
        }
        try {
            inputStream.close();
        } catch (IOException e) {
        }
        if (bmd5 != null) {
            return FilesystemBasedDigest.getHex(bmd5);
        } else {
            logger.warn("Cannot get Hash");
            return null;
        }
    }

    /**
     * Return the Modification time for the file
     * 
     * @param dfs
     * @param path
     * @return the Modification time as a long (ms)
     * @throws ArFileException
     */
    protected final long getModificationTime(FileSystem dfs, Path path) throws ArFileException {
        try {
            return dfs.getFileStatus(path).getModificationTime();
        } catch (IOException e) {
            throw new ArFileException("Cannot check Directory", e);
        }
    }

    /**
     * 
     * @param dfs
     * @param path
     * @return True if the path can be read
     * @throws ArUnvalidIndexException
     */
    protected final boolean canRead(FileSystem dfs, Path path) throws ArUnvalidIndexException {
        FileStatus status;
        try {
            status = dfs.getFileStatus(path);
        } catch (FileNotFoundException e) {
            throw new ArUnvalidIndexException("Path not found", e);
        } catch (IOException e) {
            throw new ArUnvalidIndexException("Issue while accessing the Path", e);
        }
        return status.getPermission().getUserAction().implies(FsAction.READ);
    }

    /**
     * 
     * @param dfs
     * @param path
     * @return True if the path (directory) can be written
     * @throws ArUnvalidIndexException
     */
    protected final boolean canWrite(FileSystem dfs, Path path) throws ArUnvalidIndexException {
        FileStatus status;
        try {
            status = dfs.getFileStatus(path);
        } catch (FileNotFoundException e) {
            try {
                createDir(dfs, path);
            } catch (ArFileException e2) {
                throw new ArUnvalidIndexException("Issue while accessing the Path", e2);
            }
            try {
                status = dfs.getFileStatus(path);
            } catch (IOException e1) {
                throw new ArUnvalidIndexException("Issue while accessing the Path", e1);
            }
        } catch (IOException e) {
            throw new ArUnvalidIndexException("Issue while accessing the Path", e);
        }
        return status.getPermission().getUserAction().implies(FsAction.WRITE);
    }

    /**
     * 
     * @param dfs
     * @param path
     * @return True if Path is an existing Directory
     * @throws ArUnvalidIndexException
     */
    protected final boolean isDirectory(FileSystem dfs, Path path) throws ArUnvalidIndexException {
        try {
            FileStatus status = dfs.getFileStatus(path);
            return status.isDirectory();
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            throw new ArUnvalidIndexException("Issue while accessing the Path", e);
        }
    }

    /**
     * 
     * @param dfs
     * @param path
     * @return True if Path is an existing File
     * @throws ArUnvalidIndexException
     */
    protected final boolean isFile(FileSystem dfs, Path path) throws ArUnvalidIndexException {
        try {
            FileStatus status = dfs.getFileStatus(path);
            return status.isFile();
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            throw new ArUnvalidIndexException("Issue while accessing the Path", e);
        }
    }

    /**
     * Copy from Src to Dst
     * 
     * @param legacySrc
     * @param src
     * @param legacyDst
     * @param dst
     * @throws ArFileException
     */
    protected final void copyPathToPathInternal(ArkHadoopLegacy legacySrc, Path src, ArkHadoopLegacy legacyDst, Path dst, boolean useCipher) throws ArFileException {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        if (src == null) {
            throw new ArFileException("Path uncorrect");
        }
        if (dst == null) {
            throw new ArFileException("Path uncorrect");
        }
        try {
            if ((!useCipher) || legacySrc.isLegacySharingKeyObject(legacyDst)) {
                FSDataInputStream fsDataInputStream = null;
                try {
                    fsDataInputStream = legacySrc.getFileSystem().open(src, ArConstants.BUFFERSIZEDEFAULT);
                } catch (IOException e) {
                    throw new ArFileException("File cannot be read", e);
                }
                inputStream = fsDataInputStream;
                FSDataOutputStream fsDataOutputStream = null;
                try {
                    fsDataOutputStream = legacyDst.getFileSystem().create(dst, false, ArConstants.BUFFERSIZEDEFAULT);
                } catch (IOException e) {
                    throw new ArFileException("File cannot be written", e);
                }
                outputStream = fsDataOutputStream;
            } else {
                inputStream = getInputStream(legacySrc, src, ArConstants.BUFFERSIZEDEFAULT);
                outputStream = getOutputStream(legacyDst, dst, ArConstants.BUFFERSIZEDEFAULT);
            }
            byte[] bytes = new byte[ArConstants.BUFFERSIZEDEFAULT];
            int read = 0;
            while (read >= 0) {
                read = inputStream.read(bytes);
                if (read > 0) {
                    outputStream.write(bytes, 0, read);
                }
            }
            inputStream.close();
            inputStream = null;
            outputStream.flush();
            outputStream.close();
            outputStream = null;
        } catch (IOException e) {
            throw new ArFileException("Error while copying", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e1) {
                }
                inputStream = null;
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e1) {
                }
                outputStream = null;
            }
        }
    }

    @Override
    protected final InputStream getInputStream(ArkLegacyInterface legacy, Object file, int blocksize) throws ArFileException {
        return getInputStreamInternal((ArkHadoopLegacy) legacy, (Path) file, blocksize);
    }

    @Override
    protected final OutputStream getOutputStream(ArkLegacyInterface legacy, Object file, int blocksize) throws ArFileException {
        return getOutputStreamInternal((ArkHadoopLegacy) legacy, (Path) file, blocksize);
    }

    @Override
    protected final void copyPathToPath(ArkLegacyInterface legacySrc, Object src, ArkLegacyInterface legacyDst, Object dst, boolean useCipher) throws ArFileException {
        copyPathToPathInternal((ArkHadoopLegacy) legacySrc, (Path) src, (ArkHadoopLegacy) legacyDst, (Path) dst, useCipher);
    }

    @Override
    protected final String computeStringMark(ArkLegacyInterface legacy, Object file) {
        return computeStringMarkInternal((ArkHadoopLegacy) legacy, (Path) file);
    }
}
