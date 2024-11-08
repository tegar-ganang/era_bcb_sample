package eu.vph.predict.vre.in_silico.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import eu.vph.predict.vre.base.exception.MessageKeys;
import eu.vph.predict.vre.base.exception.VRESystemException;

/**
 * Utility class for (de-)compressing stuff.
 *
 * @author Geoff Williams
 */
public class ZipUtil {

    public static final String ZIP_SUFFIX = ".zip";

    private static final Log log = LogFactory.getLog(ZipUtil.class);

    /**
   * Compress the job directories.
   * 
   * @param simulationDirectory Base simulation directory.
   * @param directoriesToIgnore Names of directories to ignore.
   * @return Compressed zip file paths.
   */
    public static Map<String, String> jobDirectoryCompressor(final String simulationDirectoryPath, Set<String> directoriesToIgnore) {
        log.debug("~jobDirectoryCompressor(String) : About to compress simulation job directories for [" + simulationDirectoryPath + "]");
        if (directoriesToIgnore == null) directoriesToIgnore = new HashSet<String>(0);
        final Map<String, String> zipFiles = new HashMap<String, String>();
        final String[] jobDirectories = new File(simulationDirectoryPath).list();
        Arrays.sort(jobDirectories);
        for (int idx = 0; idx < jobDirectories.length; idx++) {
            final String jobDirectoryName = jobDirectories[idx];
            if (directoriesToIgnore.contains(jobDirectoryName)) continue;
            final String jobDirectoryPath = simulationDirectoryPath.concat(jobDirectoryName);
            final String zipFilePath = zipDirectory(jobDirectoryPath);
            if (zipFilePath != null) zipFiles.put(jobDirectoryName.concat(ZIP_SUFFIX), zipFilePath);
        }
        return zipFiles;
    }

    /**
   * Zip a directory (and any subdirectories).
   * 
   * @param directoryPath Directory path to zip.
   * @return Zip file path (or null if <code>directoryPath</code> wasn't a directory).
   */
    public static String zipDirectory(final String directoryPath) {
        log.debug("~zipDirectory(String) : About to zip a directory [" + directoryPath + "] (and subdirectories)");
        final File jobDirectoryFile = new File(directoryPath);
        if (!jobDirectoryFile.isDirectory()) {
            log.warn("~zipDirectory(String) : Path [" + directoryPath + "] was not a directory - ignored!");
            return null;
        }
        final String zipFilePath = directoryPath.concat(ZIP_SUFFIX);
        try {
            final ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFilePath));
            doZip(directoryPath, directoryPath, zipOutputStream);
            zipOutputStream.close();
        } catch (IOException ioe) {
            log.error("~zipDirectory(String) : IO Exception [" + ioe.getMessage() + "] caught zipping directory [" + directoryPath + "]");
            ioe.printStackTrace();
            throw new VRESystemException(MessageKeys.CREATION_FAIL_GENERIC, new Object[] { zipFilePath, ioe.getMessage() });
        }
        return zipFilePath;
    }

    /**
   * Zip a directory relative to a specific path.
   * 
   * @param basePath Base path of directory to zip.
   * @param directory Directory to zip.
   * @param zipOutputStream Outputstream to write zip file to.
   * @throws IOException If IO problems.
   */
    public static void doZip(final String basePath, final String directory, final ZipOutputStream zipOutputStream) throws IOException {
        log.debug("~doZip(..) : About to zip contents of directory [" + directory + "]");
        final File zipDirectory = new File(directory);
        final String[] zipDirectoryList = zipDirectory.list();
        final byte[] readBuffer = new byte[4096];
        int bytesIn = 0;
        for (int dirIdx = 0; dirIdx < zipDirectoryList.length; dirIdx++) {
            final File file = new File(zipDirectory, zipDirectoryList[dirIdx]);
            if (file.isDirectory()) {
                final String filePath = file.getPath();
                doZip(basePath, filePath, zipOutputStream);
                continue;
            }
            final FileInputStream fileInputStream = new FileInputStream(file);
            final String zipFilePath = file.getCanonicalPath().substring(basePath.length() + 1);
            final ZipEntry zipEntry = new ZipEntry(zipFilePath);
            zipOutputStream.putNextEntry(zipEntry);
            while ((bytesIn = fileInputStream.read(readBuffer)) != -1) {
                zipOutputStream.write(readBuffer, 0, bytesIn);
            }
            fileInputStream.close();
        }
    }
}
