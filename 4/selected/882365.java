package net.sf.jpkgmk.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import net.sf.jpkgmk.PackageException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;

/**
 * @author gommma (gommma AT users.sourceforge.net)
 * @author Last changed by: $Author: gommma $
 * @version $Revision: 2 $ $Date: 2008-08-20 15:14:19 -0400 (Wed, 20 Aug 2008) $
 * @since 1.0
 */
public class FileUtil {

    private static int BUFFER_SIZE = 1024;

    /**
	 * The physical block size on the target device (512 byte by default)
	 */
    private static int BLOCK_SIZE = 512;

    private static Log log = LogFactory.getLog(FileUtil.class);

    public static String UNIX_FILE_SEPARATOR = "/";

    private FileUtil() {
    }

    /**
	 * Creates a ".gz" file of the given input file.
	 * @param inputFile
	 * @return Returns the newly created file
	 */
    public static File createGzip(File inputFile) {
        File targetFile = new File(inputFile.getParentFile(), inputFile.getName() + ".gz");
        if (targetFile.exists()) {
            log.warn("The target file '" + targetFile + "' already exists. Will overwrite");
        }
        FileInputStream in = null;
        GZIPOutputStream out = null;
        try {
            int read = 0;
            byte[] data = new byte[BUFFER_SIZE];
            in = new FileInputStream(inputFile);
            out = new GZIPOutputStream(new FileOutputStream(targetFile));
            while ((read = in.read(data, 0, BUFFER_SIZE)) != -1) {
                out.write(data, 0, read);
            }
            in.close();
            out.close();
            boolean deleteSuccess = inputFile.delete();
            if (!deleteSuccess) {
                log.warn("Could not delete file '" + inputFile + "'");
            }
            log.info("Successfully created gzip file '" + targetFile + "'.");
        } catch (Exception e) {
            log.error("Exception while creating GZIP.", e);
        } finally {
            StreamUtil.tryCloseStream(in);
            StreamUtil.tryCloseStream(out);
        }
        return targetFile;
    }

    /**
	 * Creates a tar file of all files in the given directory.
	 * @param directoryToPack The directory to be packed
	 * @param targetTarFile The target file for storing the new tar
	 * @throws IOException
	 */
    public static void createTar(File directoryToPack, File targetTarFile) throws IOException {
        if (directoryToPack == null) {
            throw new NullPointerException("The parameter 'directoryToPack' must not be null");
        }
        if (targetTarFile == null) {
            throw new NullPointerException("The parameter 'targetTarFile' must not be null");
        }
        if (!directoryToPack.exists() || !directoryToPack.isDirectory()) {
            throw new IllegalArgumentException("The target file '" + directoryToPack + "' does not exist or is not a directory.");
        }
        if (targetTarFile.exists()) {
            log.warn("The target file '" + targetTarFile + "' already exists. Will overwrite");
        }
        log.debug("Creating tar from all files in directory '" + directoryToPack + "'");
        byte buffer[] = new byte[BUFFER_SIZE];
        FileOutputStream targetOutput = new FileOutputStream(targetTarFile);
        TarOutputStream targetOutputTar = new TarOutputStream(targetOutput);
        try {
            List<File> fileList = collectFiles(directoryToPack);
            for (Iterator<File> iter = fileList.iterator(); iter.hasNext(); ) {
                File file = iter.next();
                if (file == null || !file.exists() || file.isDirectory()) {
                    log.info("The file '" + file + "' is ignored - is a directory or non-existent");
                    continue;
                }
                if (file.equals(targetTarFile)) {
                    log.debug("Skipping file: '" + file + "' - is the tar file itself");
                    continue;
                }
                log.debug("Adding to archive: file='" + file + "', archive='" + targetTarFile + "'");
                String filePathInTar = getFilePathInTar(file, directoryToPack);
                log.debug("File path in tar: '" + filePathInTar + "' (file=" + file + ")");
                TarEntry tarAdd = new TarEntry(file);
                tarAdd.setModTime(file.lastModified());
                tarAdd.setName(filePathInTar);
                targetOutputTar.putNextEntry(tarAdd);
                if (file.isFile()) {
                    FileInputStream in = new FileInputStream(file);
                    try {
                        while (true) {
                            int nRead = in.read(buffer, 0, buffer.length);
                            if (nRead <= 0) break;
                            targetOutputTar.write(buffer, 0, nRead);
                        }
                    } finally {
                        StreamUtil.tryCloseStream(in);
                    }
                }
                targetOutputTar.closeEntry();
            }
        } finally {
            StreamUtil.tryCloseStream(targetOutputTar);
            StreamUtil.tryCloseStream(targetOutput);
        }
        log.info("Tar Archive created successfully '" + targetTarFile + "'");
    }

    /**
	 * Resolves the relative path of the given file within the given directoryToPack.
	 * @param file
	 * @param directoryToPack
	 * @return The relative path of the file that can be used as filename in the tar
	 */
    static String getFilePathInTar(File file, File directoryToPack) {
        String result = file.getName();
        for (File currentFileToCheck = file.getParentFile(); !currentFileToCheck.equals(directoryToPack); currentFileToCheck = currentFileToCheck.getParentFile()) {
            result = currentFileToCheck.getName() + "/" + result;
        }
        return result;
    }

    private static List<File> collectFiles(File directoryToPack) {
        List<File> result = new ArrayList<File>();
        File[] fileList = directoryToPack.listFiles();
        for (int i = 0; i < fileList.length; i++) {
            File file = fileList[i];
            result.add(file);
            if (file.isDirectory()) {
                List<File> children = collectFiles(file);
                result.addAll(children);
            }
        }
        return result;
    }

    public static void deleteRecursively(File fileOrDirectory) {
        if (fileOrDirectory.isFile()) {
            boolean success = fileOrDirectory.delete();
            if (!success) {
                throw new RuntimeException("The file '" + fileOrDirectory.getAbsolutePath() + "' could not be deleted");
            } else {
                log.debug("Successfully deleted file '" + fileOrDirectory + "'.");
            }
        } else {
            File[] children = fileOrDirectory.listFiles();
            for (int i = 0; i < children.length; i++) {
                File file = children[i];
                deleteRecursively(file);
            }
            boolean success = fileOrDirectory.delete();
            if (!success) {
                throw new RuntimeException("The directory '" + fileOrDirectory.getAbsolutePath() + "' could not be deleted");
            } else {
                log.debug("Successfully deleted directory '" + fileOrDirectory + "'.");
            }
        }
    }

    public static void writeFile(File target, String content) throws IOException {
        Charset charset = Charset.defaultCharset();
        log.info("Using default character set " + charset + " for writing data to file " + target);
        writeFile(target, content, charset);
    }

    public static void writeFile(File target, String content, Charset charset) throws IOException {
        OutputStream output = new BufferedOutputStream(new FileOutputStream(target));
        try {
            Writer writer = new OutputStreamWriter(output, charset);
            writer.write(content);
            writer.flush();
        } finally {
            StreamUtil.tryCloseStream(output);
        }
    }

    public static String readFile(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("The parameter 'file' must not be null");
        }
        InputStream input = new BufferedInputStream(new FileInputStream(file));
        try {
            return FileUtil.readString(input);
        } finally {
            StreamUtil.tryCloseStream(input);
        }
    }

    public static String readString(URL file) throws IOException {
        if (file == null) {
            throw new NullPointerException("The parameter 'file' must not be null");
        }
        InputStream input = file.openStream();
        try {
            return FileUtil.readString(input);
        } finally {
            StreamUtil.tryCloseStream(input);
        }
    }

    /**
	 * Reads the bytes from the given input stream using the default platform character set.
	 * It is strongly recommended that you use the method {@link FileUtil#readString(InputStream, Charset)} to
	 * ensure that the character conversion is correctly done.
	 * @param input
	 * @return
	 * @throws IOException
	 */
    public static String readString(InputStream input) throws IOException {
        log.info("Using default character set " + Charset.defaultCharset() + " for reading input stream...");
        return FileUtil.readString(input, Charset.defaultCharset());
    }

    /**
	 * Reads the string from the given input stream using the given charset. Does not close the stream after reading.
	 * @param input
	 * @param charset
	 * @return
	 * @throws IOException
	 */
    public static String readString(InputStream input, Charset charset) throws IOException {
        Reader reader = new InputStreamReader(input, charset);
        StringBuffer sb = new StringBuffer();
        int character = -1;
        while ((character = reader.read()) != -1) {
            sb.append((char) character);
        }
        return sb.toString();
    }

    public static boolean isSubdir(File subDirToCheck, File basedir) {
        if (subDirToCheck.equals(basedir)) {
            return true;
        } else {
            File parentOfSubdir = subDirToCheck.getParentFile();
            if (parentOfSubdir == null) {
                return false;
            } else {
                return FileUtil.isSubdir(parentOfSubdir, basedir);
            }
        }
    }

    /**
	 * Builds a path and separates the given file or directory using the given pathSeparator.
	 * @param fileOrDir
	 * @param pathSeparator
	 * @return
	 */
    public static String buildPath(File fileOrDir, String pathSeparator) {
        if (File.separator.equals(pathSeparator)) {
            log.info("The file separator is already a '" + File.separator + "'. Nothing to do here.");
            return fileOrDir.getPath();
        }
        String result = "";
        for (File current = fileOrDir; current != null; current = current.getParentFile()) {
            String name = current.getName();
            if (current.isFile()) {
                result = name;
            } else {
                result = name + pathSeparator + result;
            }
        }
        return result;
    }

    /**
	 * Returns all files and directories from the given dir recursively 
	 * @param destDir
	 * @param filter
	 * @return
	 */
    public static List<File> getFiles(File destDir, FileFilter filter) {
        List<File> resultList = new ArrayList<File>();
        File[] children = null;
        if (filter != null) {
            children = destDir.listFiles();
        } else {
            children = destDir.listFiles(filter);
        }
        if (children != null) {
            resultList.addAll(Arrays.asList(children));
            for (int i = 0; i < children.length; i++) {
                List<File> childrenResult = getFiles(children[i], filter);
                resultList.addAll(childrenResult);
            }
        }
        return resultList;
    }

    /**
	 * Recursively copies the source directory with all files to the target directory. Includes all subdirectories and their files.
	 * @param sourceDir
	 * @param targetDir
	 * @param fileFilter File filter used for copying. Can be null which will include all files to the copy process.
	 * @throws IOException
	 */
    public static void copyFiles(File sourceDir, File targetDir, FileFilter fileFilter) throws IOException {
        if (!sourceDir.isDirectory()) {
            throw new IllegalArgumentException("The given sourceDir '" + sourceDir + "' must be a directory.");
        }
        if (!sourceDir.isDirectory()) {
            throw new IllegalArgumentException("The given targetDir '" + targetDir + "' must be a directory.");
        }
        File[] sourceChildren = null;
        if (fileFilter == null) {
            sourceChildren = sourceDir.listFiles();
        } else {
            sourceChildren = sourceDir.listFiles(fileFilter);
        }
        if (sourceChildren == null) {
            return;
        }
        List<File> fileList = Arrays.asList(sourceChildren);
        for (Iterator<File> iter = fileList.iterator(); iter.hasNext(); ) {
            File file = iter.next();
            File fileInTarget = new File(targetDir, file.getName());
            if (file.isDirectory()) {
                boolean dirSuccess = fileInTarget.mkdir();
                if (!dirSuccess) {
                    throw new RuntimeException("Could not create directory " + targetDir + ". Aborting");
                }
                log.debug("Created directory '" + fileInTarget + "'");
                FileUtil.copyFiles(file, fileInTarget, fileFilter);
            } else {
                FileUtil.copyFile(file, fileInTarget);
            }
        }
    }

    /**
	 * Copies one file to a different location
	 * @param sourceFile
	 * @param destFile The destination file
	 * @throws IOException
	 */
    public static void copyFile(File sourceFile, File destFile) throws IOException {
        log.info("Copying file '" + sourceFile + "' to '" + destFile + "'");
        if (!sourceFile.isFile()) {
            throw new IllegalArgumentException("The sourceFile '" + sourceFile + "' does not exist or is not a normal file.");
        }
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        FileChannel source = null;
        FileChannel destination = null;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            long numberOfBytes = destination.transferFrom(source, 0, source.size());
            log.debug("Transferred " + numberOfBytes + " bytes from '" + sourceFile + "' to '" + destFile + "'.");
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

    public static void createDir(File dir, boolean failOnError) {
        boolean success = dir.mkdir();
        if (success) {
            log.debug("Successfully created directory '" + dir + "'");
        } else {
            if (dir.isDirectory()) {
                log.debug("The directory '" + dir + "' already exists. Nothing to do.");
                return;
            }
            String errorMessage = "Could not create '" + dir + "'.";
            File parent = dir.getParentFile();
            if (!parent.exists()) {
                errorMessage += " The parent directory '" + parent + "' does not exist.";
            }
            if (failOnError) {
                throw new IllegalStateException(errorMessage);
            } else {
                log.warn(errorMessage);
            }
        }
    }

    /**
	 * Determines if the given unix path is a relocatable path or not in the sense of a prototype file.
	 * It is relocatable if the path is relative (not starting with a '/').
	 * @param path The path to check.
	 * @return
	 */
    public static boolean isRelocatable(String path) {
        if (path == null) {
            return false;
        }
        return !path.startsWith(UNIX_FILE_SEPARATOR);
    }

    /**
	 * Recursively counts the files in the given directory. Subdirectories are also counted.
	 * @param directory
	 * @return The number of files/directories in the given directory
	 */
    public static int countFilesAndDirectories(File directory) {
        return countFiles(directory, true);
    }

    /**
	 * Recursively counts the files in the given directory. Subdirectories are not counted.
	 * @param directory
	 * @return The number of files in the given directory that are no directories
	 */
    public static int countFiles(File directory) {
        return countFiles(directory, false);
    }

    /**
	 * Recursively counts the files in the given directory. Subdirectories are not counted.
	 * @param directory
	 * @param countDirectories If subdirectories should also be included into the count
	 * @return The number of files in the given directory that are no directories
	 */
    public static int countFiles(File directory, boolean countDirectories) {
        if (directory == null) {
            throw new NullPointerException("The parameter 'directory' must not be null");
        }
        int count = 0;
        File[] files = directory.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    if (countDirectories) count++;
                    count += countFiles(files[i]);
                } else {
                    count++;
                }
            }
        }
        return count;
    }

    /**
	 * Returns the number of 512 byte blocks that are needed to store the given directory.
	 * @param basedir 
	 * @return
	 */
    public static Long getBlockCount(File basedir) {
        if (basedir == null) {
            throw new NullPointerException("The parameter 'basedir' must not be null.");
        }
        long fileSize = FileUtil.getSize(basedir);
        long blockCount = fileSize / BLOCK_SIZE + (fileSize % BLOCK_SIZE > 0 ? 1 : 0);
        return new Long(blockCount);
    }

    /**
	 * @param basedir
	 * @return Returns the size in bytes of the content of the given directory
	 */
    public static long getSize(File basedir) {
        if (basedir == null) {
            throw new NullPointerException("The parameter 'basedir' must not be null.");
        }
        File[] children = basedir.listFiles();
        long overallSize = 0;
        for (int i = 0; i < children.length; i++) {
            File currentFile = children[i];
            if (currentFile.isFile()) {
                overallSize += currentFile.length();
            } else {
                long childSize = getSize(currentFile);
                overallSize += childSize;
            }
        }
        return overallSize;
    }

    /**
	 * Non-recursively checks the content of the given directory against the given array of file objects.
	 * @param dirToCheck
	 * @param expectedFiles
	 */
    public static void assertContainsFiles(File dirToCheck, File[] expectedFiles) {
        File[] actualFiles = dirToCheck.listFiles();
        List<File> actualFileList = Arrays.asList(actualFiles);
        List<File> expectedFileList = Arrays.asList(expectedFiles);
        boolean equal = actualFileList.equals(expectedFileList);
        if (!equal) {
            throw new PackageException("The files in directory '" + dirToCheck + "' are not equal to the given array of files. " + "Expected: " + expectedFileList + ",   Actual: " + actualFileList);
        }
    }
}
