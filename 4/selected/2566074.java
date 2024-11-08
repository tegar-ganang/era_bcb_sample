package gg.arkehion.store.fileimpl;

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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

/**
 * @author frederic
 * 
 */
public class ArkFsDirFunction extends ArkAbstractDirFunction {

    /**
     * Internal Logger
     */
    private static final GgInternalLogger logger = GgInternalLoggerFactory.getLogger(ArkFsDirFunction.class);

    /**
     * @param separator
     */
    public ArkFsDirFunction() {
        super(File.separatorChar);
    }

    protected final double[] getGlobalUsedAndFreeSpaces(File storage) {
        double[] size = new double[3];
        size[0] = storage.getTotalSpace();
        size[1] = size[0] - storage.getUsableSpace();
        size[2] = storage.getFreeSpace();
        return size;
    }

    @Override
    public final double[] getGlobalUsedAndFreeSpaces(ArkLegacyInterface legacy, String storage) {
        File path = new File(legacy.getBasePath() + storage);
        return getGlobalUsedAndFreeSpaces(path);
    }

    protected final double[] getUsedSpaceNbDirNbFile(File storage) {
        double[] size = new double[3];
        size[0] = 0.0;
        size[1] = 0.0;
        size[2] = 0.0;
        if (!storage.exists()) {
            storage = null;
            return size;
        }
        if (!storage.isDirectory()) {
            storage = null;
            return size;
        }
        File[] list = storage.listFiles();
        if ((list == null) || (list.length == 0)) {
            list = null;
            size[1]++;
            return size;
        }
        int len = list.length;
        for (int i = 0; i < len; i++) {
            if (list[i].isDirectory()) {
                size = getRecursiveUsedSpaceNbDirNbFile(list[i], size);
            } else {
                size[2]++;
                size[0] += sizeAsBlockFs(list[i].length());
            }
        }
        list = null;
        size[0] = size[0] / 1024;
        return size;
    }

    /**
     * Get the Used space in KB, number of directories and number of files. This
     * version does used a recursive algorithm.
     * 
     * @param dirsource
     * @param values
     * @return the size in KB, number of directories and number of files as
     *         array of double
     */
    private final double[] getRecursiveUsedSpaceNbDirNbFile(File dirsource, double[] values) {
        if (dirsource == null) {
            return values;
        }
        if (!dirsource.exists()) {
            return values;
        }
        if (dirsource.isDirectory()) {
            values[1]++;
            File[] list = dirsource.listFiles();
            if ((list == null) || (list.length == 0)) {
                list = null;
                return values;
            }
            int len = list.length;
            for (int i = 0; i < len; i++) {
                if (list[i].isDirectory()) {
                    values = getRecursiveUsedSpaceNbDirNbFile(list[i], values);
                } else {
                    values[2]++;
                    values[0] += sizeAsBlockFs(list[i].length());
                }
            }
            list = null;
        } else {
            values[2]++;
            values[0] += sizeAsBlockFs(dirsource.length());
        }
        return values;
    }

    @Override
    public final double[] getUsedSpaceNbDirNbFile(ArkLegacyInterface legacy, String storage) {
        File path = new File(legacy.getBasePath() + storage);
        return getUsedSpaceNbDirNbFile(path);
    }

    /**
     * Create the directory associated with the String path
     * 
     * @param path
     * @return True if created, False else.
     * @throws ArFileException
     */
    protected final boolean createDir(String path) {
        if (path == null) {
            return false;
        }
        File directory = new File(path);
        if (directory.isDirectory()) {
            directory = null;
            return true;
        }
        return directory.mkdirs();
    }

    /**
     * Delete the directory associated with the path if empty
     * 
     * @param path
     * @return True if deleted, False else.
     * @throws ArFileException
     */
    protected final boolean deleteDir(String path) {
        if (path == null) {
            return true;
        }
        File directory = new File(path);
        if (!directory.exists()) {
            directory = null;
            return true;
        }
        if (!directory.isDirectory()) {
            directory = null;
            return false;
        }
        boolean retour = directory.delete();
        directory = null;
        return retour;
    }

    /**
     * Delete the directory and its subdirs associated with the path if empty
     * 
     * @param dfs
     * @param path
     * @return True if deleted, False else.
     * @throws ArFileException
     */
    protected final boolean deleteRecursiveDir(String path) {
        if (path == null) {
            return true;
        }
        boolean retour = true;
        File directory = new File(path);
        if (!directory.exists()) {
            directory = null;
            return true;
        }
        if (!directory.isDirectory()) {
            directory = null;
            return false;
        }
        File[] list = directory.listFiles();
        if ((list == null) || (list.length == 0)) {
            list = null;
            retour = directory.delete();
            directory = null;
            return retour;
        }
        int len = list.length;
        for (int i = 0; i < len; i++) {
            if (list[i].isDirectory()) {
                if (!deleteRecursiveFileDir(list[i])) {
                    retour = false;
                }
            } else {
                retour = false;
            }
        }
        list = null;
        if (retour) {
            retour = directory.delete();
        }
        directory = null;
        return retour;
    }

    /**
     * Delete the directory and its subdirs associated with the File dir if
     * empty
     * 
     * @param dir
     * @return True if deleted, False else.
     */
    protected boolean deleteRecursiveFileDir(File dir) {
        if (dir == null) {
            return true;
        }
        boolean retour = true;
        if (!dir.exists()) {
            return true;
        }
        File[] list = dir.listFiles();
        if ((list == null) || (list.length == 0)) {
            list = null;
            return dir.delete();
        }
        int len = list.length;
        for (int i = 0; i < len; i++) {
            if (list[i].isDirectory()) {
                if (!deleteRecursiveFileDir(list[i])) {
                    retour = false;
                }
            } else {
                retour = false;
                list = null;
                return retour;
            }
        }
        list = null;
        if (retour) {
            retour = dir.delete();
        }
        return retour;
    }

    /**
     * Returns True if the directory is empty (no file), False if not empty or
     * not exist or is not a directory.
     * 
     * @param path
     * @return True if the directory is empty, else False.
     * @throws ArFileException
     */
    protected final boolean isDirEmpty(String path) {
        if (path == null) {
            return true;
        }
        File directory = new File(path);
        if (directory.isDirectory()) {
            String[] list = directory.list();
            if ((list == null) || (list.length == 0)) {
                list = null;
                directory = null;
                return true;
            }
            list = null;
        }
        directory = null;
        return false;
    }

    /**
     * Returns True if the directory is empty (no file), False if not empty or
     * not exist or is not a directory.
     * 
     * @param path
     * @return True if the directory (and subdirectories) is empty, else False.
     * @throws ArFileException
     */
    protected final boolean isDirRecursiveEmpty(String path) {
        if (path == null) {
            return true;
        }
        File directory = new File(path);
        if (directory.isDirectory()) {
            File[] list = directory.listFiles();
            if ((list == null) || (list.length == 0)) {
                list = null;
                directory = null;
                return true;
            }
            int len = list.length;
            for (int i = 0; i < len; i++) {
                if (list[i].isDirectory()) {
                    if (!isFileDirRecursiveEmpty(list[i])) {
                        list = null;
                        directory = null;
                        return false;
                    }
                } else {
                    list = null;
                    directory = null;
                    return false;
                }
            }
            list = null;
        } else {
            directory = null;
            return false;
        }
        directory = null;
        return true;
    }

    /**
     * Returns True if the directory and its subdirs are empty, False if not
     * empty or not exist or is not a directory.
     * 
     * @param dir
     * @return True if the directory and its subdirs are empty, else False.
     */
    protected final boolean isFileDirRecursiveEmpty(File dir) {
        if (dir == null) {
            return true;
        }
        File[] list = dir.listFiles();
        if ((list == null) || (list.length == 0)) {
            list = null;
            return true;
        }
        int len = list.length;
        for (int i = 0; i < len; i++) {
            if (list[i].isDirectory()) {
                if (!isFileDirRecursiveEmpty(list[i])) {
                    list = null;
                    dir = null;
                    return false;
                }
            } else {
                list = null;
                dir = null;
                return false;
            }
        }
        list = null;
        return true;
    }

    @Override
    public final long getListOfFiles(ArkLegacyInterface legacy, File out, long maxInFile, String path, long refTime) {
        File dir = new File(legacy.getBasePath() + path);
        logger.debug("directory to check: " + legacy.getBasePath() + " " + path + "\n" + dir.getAbsolutePath());
        return getListOfFiles(legacy.getLID(), out, maxInFile, dir, refTime);
    }

    protected final long getListOfFiles(long leg, File out, long maxInFile, File directory, long refTime) {
        if (directory == null) {
            logger.warn("directory null");
            return -1;
        }
        if (!directory.exists()) {
            logger.debug("directory not exist: " + directory.getAbsolutePath());
            return -1;
        }
        if (!directory.isDirectory()) {
            logger.debug("directory is not directory");
            return -1;
        }
        File[] list = directory.listFiles();
        if ((list == null) || (list.length == 0)) {
            list = null;
            logger.debug("directory empty");
            return -1;
        }
        int len = list.length;
        NbFilesAndOutputFiles nbFilesAndOutputFiles = new NbFilesAndOutputFiles(out, maxInFile);
        if (!nbFilesAndOutputFiles.newFileOutputStream()) {
            logger.warn("cannot open output");
            return -1;
        }
        String sleg = Long.toString(leg);
        for (int i = 0; i < len; i++) {
            if (list[i].isDirectory()) {
                try {
                    listRecursiveFileDir(sleg, nbFilesAndOutputFiles, list[i], refTime);
                } catch (IOException e) {
                    list = null;
                    logger.warn("directory exception", e);
                    nbFilesAndOutputFiles.close();
                    return -1;
                }
            }
        }
        list = null;
        directory = null;
        nbFilesAndOutputFiles.close();
        return nbFilesAndOutputFiles.outputFile;
    }

    /**
     * Get recursively the list of files in the outputStream from the dir. Used
     * a recursive version.
     * 
     * @param sleg
     * @param nfof
     * @param dir
     * @param refTime
     * @throws IOException
     */
    private final void listRecursiveFileDir(String sleg, NbFilesAndOutputFiles nfof, File dir, long refTime) throws IOException {
        if (dir == null) {
            logger.warn("directory null");
            return;
        }
        if (!dir.exists()) {
            logger.debug("directory not exist");
            return;
        }
        if (dir.isDirectory()) {
            File[] list = dir.listFiles();
            if ((list == null) || (list.length == 0)) {
                list = null;
                return;
            }
            int len = list.length;
            for (int i = 0; i < len; i++) {
                if (list[i].isDirectory()) {
                    listRecursiveFileDir(sleg, nfof, list[i], refTime);
                } else {
                    if (list[i].getName().endsWith(ArkDirConstants.METAEXT)) {
                        continue;
                    }
                    if (refTime == 0 || list[i].lastModified() > refTime) {
                        long[] sublist;
                        try {
                            sublist = pathToIdUnique(list[i].getAbsolutePath());
                        } catch (ArUnvalidIndexException e) {
                            continue;
                        }
                        if ((sublist != null) && (sublist.length >= 2)) {
                            nfof.checkOutputAddFile();
                            int length = sublist.length;
                            nfof.writeCheckFilesMark(sleg, Long.toString(sublist[length - 2]), Long.toString(sublist[length - 1]), null, null);
                        }
                        sublist = null;
                    }
                }
            }
            list = null;
        } else {
            if (dir.getName().endsWith(ArkDirConstants.METAEXT)) {
                return;
            }
            if (refTime == 0 || dir.lastModified() > refTime) {
                long[] sublist;
                try {
                    sublist = pathToIdUnique(dir.getAbsolutePath());
                } catch (ArUnvalidIndexException e) {
                    sublist = null;
                }
                if ((sublist != null) && (sublist.length >= 2)) {
                    nfof.checkOutputAddFile();
                    int length = sublist.length;
                    nfof.writeCheckFilesMark(sleg, Long.toString(sublist[length - 2]), Long.toString(sublist[length - 1]), null, null);
                }
                sublist = null;
            }
        }
    }

    @Override
    public final long getListOfFilesMark(ArkLegacyInterface legacy, File out, long maxInFile, String path, long refTime) {
        File dir = new File(legacy.getBasePath() + path);
        return getListOfFilesMark((ArkFsLegacy) legacy, out, maxInFile, dir, refTime);
    }

    protected final long getListOfFilesMark(ArkFsLegacy legacy, File out, long maxInFile, File directory, long refTime) {
        if (directory == null) {
            logger.warn("directory null");
            return -1;
        }
        if (!directory.exists()) {
            logger.debug("directory not exist: " + directory.getAbsolutePath());
            return -1;
        }
        if (!directory.isDirectory()) {
            logger.warn("directory not directory");
            return -1;
        }
        File[] list = directory.listFiles();
        if ((list == null) || (list.length == 0)) {
            list = null;
            logger.debug("directory empty");
            return -1;
        }
        int len = list.length;
        NbFilesAndOutputFiles nbFilesAndOutputFiles = new NbFilesAndOutputFiles(out, maxInFile);
        if (!nbFilesAndOutputFiles.newFileOutputStream()) {
            logger.warn("output not created");
            return -1;
        }
        String sleg = Long.toString(legacy.getLID());
        for (int i = 0; i < len; i++) {
            if (list[i].isDirectory()) {
                try {
                    listRecursiveFileDirMark(sleg, legacy, nbFilesAndOutputFiles, list[i], refTime);
                } catch (IOException e) {
                    list = null;
                    logger.warn("directory exception", e);
                    nbFilesAndOutputFiles.close();
                    return -1;
                }
            }
        }
        list = null;
        directory = null;
        nbFilesAndOutputFiles.close();
        return nbFilesAndOutputFiles.outputFile;
    }

    /**
     * Get the list of files in the OutputStream from the dir and recursively.
     * This version does used a recursive algorithm.
     * 
     * @param sleg
     * @param legacy
     * @param nfof
     * @param dirsource
     * @param refTime
     * @throws IOException
     */
    private final void listRecursiveFileDirMark(String sleg, ArkFsLegacy legacy, NbFilesAndOutputFiles nfof, File dirsource, long refTime) throws IOException {
        if (dirsource == null) {
            logger.warn("directory null");
            return;
        }
        if (!dirsource.exists()) {
            logger.debug("directory not exist");
            return;
        }
        String shash = null;
        if (dirsource.isDirectory()) {
            File[] list = dirsource.listFiles();
            if ((list == null) || (list.length == 0)) {
                list = null;
                return;
            }
            int len = list.length;
            for (int i = 0; i < len; i++) {
                if (list[i].isDirectory()) {
                    listRecursiveFileDirMark(sleg, legacy, nfof, list[i], refTime);
                } else {
                    if (list[i].getName().endsWith(ArkDirConstants.METAEXT)) {
                        continue;
                    }
                    if (refTime == 0 || list[i].lastModified() > refTime) {
                        long[] sublist;
                        try {
                            sublist = pathToIdUnique(list[i].getAbsolutePath());
                        } catch (ArUnvalidIndexException e) {
                            continue;
                        }
                        if ((sublist != null) && (sublist.length >= 2)) {
                            nfof.checkOutputAddFile();
                            shash = computeStringMark(legacy, list[i]);
                            if (shash == null) {
                                logger.debug("noMark for " + sublist[sublist.length - 1] + ' ' + sublist[sublist.length - 2]);
                                continue;
                            }
                            int length = sublist.length;
                            nfof.writeCheckFilesMark(sleg, Long.toString(sublist[length - 2]), Long.toString(sublist[length - 1]), shash, null);
                        }
                        sublist = null;
                    }
                }
            }
            list = null;
        } else if (refTime == 0 || dirsource.lastModified() > refTime) {
            if (dirsource.getName().endsWith(ArkDirConstants.METAEXT)) {
                return;
            }
            long[] sublist;
            try {
                sublist = pathToIdUnique(dirsource.getAbsolutePath());
            } catch (ArUnvalidIndexException e) {
                sublist = null;
            }
            if ((sublist != null) && (sublist.length >= 2)) {
                nfof.checkOutputAddFile();
                shash = computeStringMark(legacy, dirsource);
                if (shash == null) {
                    logger.debug("noMark for " + sublist[sublist.length - 1] + ' ' + sublist[sublist.length - 2]);
                } else {
                    int length = sublist.length;
                    nfof.writeCheckFilesMark(sleg, Long.toString(sublist[length - 2]), Long.toString(sublist[length - 1]), shash, null);
                }
            }
            sublist = null;
        }
    }

    /**
     * 
     * @param legacy
     * @param file
     * @param blocksize
     * @return the corresponding InputStream
     * @throws ArFileException
     */
    protected final InputStream getInputStreamInternal(ArkFsLegacy legacy, File file, int blocksize) throws ArFileException {
        if (file == null) {
            throw new ArFileException("Path uncorrect");
        }
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
        } catch (IOException e) {
            throw new ArFileException("File cannot be read", e);
        }
        if (legacy.isEncrypted()) {
            Cipher cipherIn = legacy.getKeyObject().toDecrypt();
            if (cipherIn == null) {
                throw new ArFileException("CIpher uncorrect");
            }
            CipherInputStream cipherInputStream = new CipherInputStream(inputStream, cipherIn);
            return cipherInputStream;
        } else {
            return inputStream;
        }
    }

    @Override
    protected final InputStream getInputStream(ArkLegacyInterface legacy, Object file, int blocksize) throws ArFileException {
        return getInputStreamInternal((ArkFsLegacy) legacy, (File) file, blocksize);
    }

    @Override
    protected final OutputStream getOutputStream(ArkLegacyInterface legacy, Object file, int blocksize) throws ArFileException {
        return getOutputStreamInternal((ArkFsLegacy) legacy, (File) file, blocksize);
    }

    @Override
    protected final String computeStringMark(ArkLegacyInterface legacy, Object file) {
        return computeStringMarkInternal((ArkFsLegacy) legacy, (File) file);
    }

    protected final OutputStream getOutputStreamInternal(ArkFsLegacy legacy, File file, int blocksize) throws ArFileException {
        if (file == null) {
            throw new ArFileException("Path uncorrect");
        }
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
        } catch (IOException e) {
            throw new ArFileException("File cannot be written", e);
        }
        if (legacy.isEncrypted()) {
            Cipher cipherOut = legacy.getKeyObject().toCrypt();
            if (cipherOut == null) {
                throw new ArFileException("CIpher uncorrect");
            }
            CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipherOut);
            return cipherOutputStream;
        } else {
            return outputStream;
        }
    }

    /**
     * Compute the Mark (MD5 or SHA1 or CRC32 or ADLER32) in String Hex for the
     * file
     * 
     * @param legacy
     * @param file
     * @return the Mark in a String Hex format or NULL if an error occurs
     */
    protected final String computeStringMarkInternal(ArkFsLegacy legacy, File file) {
        if (file == null) {
            return null;
        }
        InputStream inputStream = null;
        try {
            inputStream = getInputStream(legacy, file, ArConstants.BUFFERSIZEDEFAULT);
        } catch (ArFileException e1) {
            return null;
        }
        byte[] bmd5;
        try {
            bmd5 = FilesystemBasedDigest.getHash(inputStream, Configuration.algoMark);
        } catch (IOException e1) {
            bmd5 = null;
        }
        try {
            inputStream.close();
        } catch (IOException e) {
        }
        if (bmd5 != null) {
            return FilesystemBasedDigest.getHex(bmd5);
        } else {
            return null;
        }
    }

    /**
     * Return the Modification time for the File
     * 
     * @param file
     * @return the Modification time as a long ms
     */
    protected final long getModificationTime(File file) {
        return file.lastModified();
    }

    /**
     * 
     * @param file
     * @return True if the path can be read
     * @throws ArUnvalidIndexException
     */
    protected final boolean canRead(File file) throws ArUnvalidIndexException {
        return file.canRead();
    }

    /**
     * 
     * @param file
     * @return True if the path (directory) can be written
     * @throws ArUnvalidIndexException
     */
    protected final boolean canWrite(File file) throws ArUnvalidIndexException {
        return file.canWrite();
    }

    /**
     * 
     * @param file
     * @return True if Path is an existing Directory
     * @throws ArUnvalidIndexException
     */
    protected final boolean isDirectory(File file) throws ArUnvalidIndexException {
        return file.isDirectory();
    }

    /**
     * 
     * @param file
     * @return True if Path is an existing File
     * @throws ArUnvalidIndexException
     */
    protected final boolean isFile(File file) throws ArUnvalidIndexException {
        return file.isFile();
    }

    protected final void copyPathToPath(InputStream inputStream, OutputStream outputStream) throws ArFileException {
        try {
            int read;
            byte[] bytes = new byte[ArConstants.BUFFERSIZEDEFAULT];
            while ((read = inputStream.read(bytes)) > 0) {
                outputStream.write(bytes, 0, read);
            }
            inputStream.close();
            inputStream = null;
            outputStream.flush();
            outputStream.close();
            outputStream = null;
        } catch (IOException e) {
            throw new ArFileException("Error while copying", e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                }
                outputStream = null;
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
                inputStream = null;
            }
        }
    }

    /**
     * Copy internal without encryption
     * @param src
     * @param dst
     * @throws ArFileException
     */
    protected final void copyPathToPathInternal(File src, File dst) throws ArFileException {
        if (src == null) {
            throw new ArFileException("Path uncorrect");
        }
        if (dst == null) {
            throw new ArFileException("Path uncorrect");
        }
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            try {
                inputStream = new FileInputStream(src);
            } catch (FileNotFoundException e) {
                throw new ArFileException("Error while copying", e);
            }
            try {
                outputStream = new FileOutputStream(dst);
            } catch (FileNotFoundException e) {
                throw new ArFileException("Error while copying", e);
            }
            copyPathToPath(inputStream, outputStream);
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

    /**
     * Copy from Src to Dst
     * 
     * @param legacySrc
     * @param src
     * @param legacyDst
     * @param dst
     * @param useCipher
     * @return the Digest if asked for, else null or throw an error
     * @throws ArFileException
     */
    protected final void copyPathToPathInternal(ArkFsLegacy legacySrc, File src, ArkFsLegacy legacyDst, File dst, boolean useCipher) throws ArFileException {
        if ((!useCipher) || legacySrc.isLegacySharingKeyObject(legacyDst)) {
            copyPathToPathInternal(src, dst);
            return;
        }
        if (src == null) {
            throw new ArFileException("Path uncorrect");
        }
        if (dst == null) {
            throw new ArFileException("Path uncorrect");
        }
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = getInputStream(legacySrc, src, ArConstants.BUFFERSIZEDEFAULT);
            outputStream = getOutputStream(legacyDst, dst, ArConstants.BUFFERSIZEDEFAULT);
            copyPathToPath(inputStream, outputStream);
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
    protected final void copyPathToPath(ArkLegacyInterface legacySrc, Object src, ArkLegacyInterface legacyDst, Object dst, boolean useCipher) throws ArFileException {
        copyPathToPathInternal((ArkFsLegacy) legacySrc, (File) src, (ArkFsLegacy) legacyDst, (File) dst, useCipher);
    }
}
