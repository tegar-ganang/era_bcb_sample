package org.jcvi.vics.shared.utils;

import org.apache.log4j.Logger;
import java.io.*;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * This class has some common Java I/O utility methods
 *
 * @author Tareq Nabeel
 */
public class FileUtil {

    public static final String FILE_SEPARATOR = File.separator;

    private static final String os = System.getProperties().getProperty("os.name");

    private static final boolean isWindows = os != null && os.toLowerCase().contains("windows");

    private static final Logger logger = Logger.getLogger(FileUtil.class.getName());

    public static void appendOneFileToAnother(File baseFile, File fileToAppend) throws IOException {
        FileWriter writer = new FileWriter(baseFile, true);
        Scanner scanner = new Scanner(fileToAppend);
        try {
            while (scanner.hasNextLine()) {
                writer.append(scanner.nextLine()).append("\n");
            }
        } finally {
            scanner.close();
            writer.close();
        }
    }

    /**
     * This method returns the resource contents as an input stream.  It looks for the resource
     * in the class path
     *
     * @param resource the name of the resource file
     * @return the resource contents as an input stream
     */
    public static InputStream getResourceAsStream(String resource) {
        String stripped = resource.startsWith("/") ? resource.substring(1) : resource;
        InputStream resourceStream = null;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            resourceStream = classLoader.getResourceAsStream(stripped);
        }
        if (resourceStream == null) {
            resourceStream = FileUtil.class.getResourceAsStream(resource);
        }
        if (resourceStream == null) {
            resourceStream = FileUtil.class.getClassLoader().getResourceAsStream(stripped);
        }
        if (resourceStream == null) {
            throw new RuntimeException(resource + " not found");
        }
        return resourceStream;
    }

    public static URL getResourceAsURL(String resource) {
        String stripped = resource.startsWith("/") ? resource.substring(1) : resource;
        URL resourceURL = null;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            resourceURL = classLoader.getResource(stripped);
        }
        if (resourceURL == null) {
            resourceURL = FileUtil.class.getResource(resource);
        }
        if (resourceURL == null) {
            resourceURL = FileUtil.class.getClassLoader().getResource(resource);
        }
        if (resourceURL == null) {
            throw new RuntimeException(resource + " not found");
        }
        return resourceURL;
    }

    /**
     * This method returns the resource contents as a StringBuffer
     *
     * @param resource the name of the resource file on the classpath
     * @return the resource contents as a StringBuffer
     */
    public static StringBuffer getResourceAsStrBuffer(String resource) throws IOException {
        return getStreamContentsAsStrBuffer(getResourceAsStream(resource));
    }

    /**
     * This method returns the resource contents as a String
     *
     * @param resource the name of the resource file on the classpath
     * @return the resource contents as a String
     */
    public static String getResourceAsString(String resource) throws IOException {
        return getStreamContentsAsStrBuffer(getResourceAsStream(resource)).toString();
    }

    /**
     * Returns the input stream contents as a String buffer using UTF8 as the encoding
     *
     * @param inputStream
     * @return
     */
    public static StringBuffer getStreamContentsAsStrBuffer(InputStream inputStream) throws IOException {
        try {
            InputStreamReader isr = new InputStreamReader(inputStream, "UTF8");
            StringBuffer fileContents = new StringBuffer();
            int c;
            while ((c = isr.read()) != -1) {
                fileContents.append((char) c);
            }
            isr.close();
            return fileContents;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            inputStream.close();
        }
    }

    /**
     * Returns the contents of <code>file</code> as a StringBuffer
     *
     * @param file the file whose contents would be returned
     * @return StringBuffer contents
     */
    public static StringBuffer getFileContentsAsStrBuffer(File file) {
        InputStream inputStream;
        try {
            if (file == null || !file.exists()) {
                throw new RuntimeException(file + " does not exist");
            }
            inputStream = new FileInputStream(file);
            return getStreamContentsAsStrBuffer(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the contents of <code>file</code> as a String
     *
     * @param filePath the path to the file whose contents would be returned
     * @return String contents
     */
    public static String getFileContentsAsString(String filePath) throws IOException {
        File fastaFile = checkFileExists(filePath);
        return getFileContentsAsString(fastaFile);
    }

    /**
     * Returns the contents of <code>file</code> as a String
     *
     * @param file the file whose contents would be returned
     * @return String contents
     */
    public static String getFileContentsAsString(File file) {
        return getFileContentsAsStrBuffer(file).toString();
    }

    /**
     * Returns the contents of the file represented by <code>filePath</code> as a Byte array
     *
     * @param filePath the file whose contents would be returned as a byte array
     * @return byte array contents of the file
     */
    public static byte[] getFileContentsAsByteArray(String filePath) throws IOException {
        File fastaFile = checkFileExists(filePath);
        return getFileContentsAsByteArray(fastaFile);
    }

    /**
     * Returns the contents of <code>file</code> as a Byte array
     *
     * @param file the file whose contents would be returned as a byte array
     * @return byte array contents of the file
     */
    public static byte[] getFileContentsAsByteArray(File file) {
        try {
            int length = (int) file.length();
            byte[] contents = new byte[length];
            FileInputStream stream = new FileInputStream(file);
            try {
                stream.read(contents);
            } finally {
                stream.close();
            }
            return contents;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the contents of <code>file</code> as a ByteArrayInputStream
     *
     * @param file the file whose contents would be returned as a ByteArrayInputStream
     * @return ByteArrayInputStream
     */
    public static InputStream getFileContentsAsStream(File file) {
        return new ByteArrayInputStream(getFileContentsAsByteArray(file));
    }

    /**
     * Returns the contents of <code>file</code> as a ByteArrayInputStream
     *
     * @param filePath the path to file whose contents would be returned as a ByteArrayInputStream
     * @return ByteArrayInputStream
     */
    public static InputStream getFileContentsAsStream(String filePath) throws IOException {
        return new ByteArrayInputStream(getFileContentsAsByteArray(checkFileExists(filePath)));
    }

    /**
     * This method creates a file representing the supplied fileName if it doesn't exist
     *
     * @param fileName
     * @return
     * @throws IOException
     */
    public static File ensureFileExists(String fileName) throws IOException {
        File file = new File(fileName);
        if (!file.exists()) {
            fileName = file.getAbsolutePath();
            int lastFileSepIdx = fileName.lastIndexOf(FILE_SEPARATOR);
            if (lastFileSepIdx != -1) {
                String dirPath = fileName.substring(0, fileName.lastIndexOf(FILE_SEPARATOR));
                ensureDirExists(dirPath);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Creating file " + fileName);
            }
            file.createNewFile();
        }
        return file;
    }

    /**
     * This method creates the directory represented by dirName if it doesn't exist
     *
     * @param dirName
     * @return
     * @throws IOException
     */
    public static File ensureDirExists(String dirName) throws IOException {
        File dir = new File(dirName);
        int retry = 5;
        while (retry > 0) {
            if (dir.exists()) {
                break;
            }
            if (retry != 5) {
                try {
                    Thread.sleep(1000);
                } catch (Exception ex) {
                }
            }
            try {
                if (!dir.mkdirs()) {
                    logger.info("Failed. Retries remaining=" + retry + " to create dir=" + dir.getAbsolutePath());
                    throw new IOException("Failed to create directory=" + dir.getAbsolutePath());
                }
            } catch (Exception ex) {
                if (retry <= 0) {
                    throw new IOException("Exhausted all retries and could not create directory=" + dir.getAbsolutePath());
                }
            }
            retry--;
        }
        return dir;
    }

    /**
     * This method creates the directory represented by childDirName (under parentDirName) if it doesn't exist
     *
     * @param parentDirName   the parent directory
     * @param childDirName    the directory is ensure
     * @param createParentDir if parentDirName does not exist and createParentDir is true, parentDirName will
     *                        be created; otherwise an exception is thrown
     * @return
     * @throws IOException
     */
    public static File ensureDirExists(String parentDirName, String childDirName, boolean createParentDir) throws IOException {
        File dir;
        if (createParentDir) {
            dir = ensureDirExists(parentDirName);
        } else {
            dir = checkFileExists(parentDirName);
            if (!dir.isDirectory()) {
                throw new IOException(parentDirName + " must be a directory");
            }
        }
        return ensureDirExists(dir.getAbsolutePath() + File.separator + childDirName);
    }

    /**
     * This method creates the directory represented by childDirName (under parentDirName) if it doesn't exist
     *
     * @param parentDirName   the parent directory
     * @param childFileName   the file to create if it doesn't exist
     * @param createParentDir if parentDirName does not exist and createParentDir is true, parentDirName will
     *                        be created; otherwise an exception is thrown
     * @return
     * @throws IOException
     */
    public static File ensureFileExists(String parentDirName, String childFileName, boolean createParentDir) throws IOException {
        File dir;
        if (createParentDir) {
            dir = ensureDirExists(parentDirName);
        } else {
            dir = checkFileExists(parentDirName);
            if (!dir.isDirectory()) {
                throw new IOException(parentDirName + " must be a directory");
            }
        }
        return ensureFileExists(dir.getAbsolutePath() + File.separator + childFileName);
    }

    /**
     * Deletes the file specified by childFileName under the specified directory
     *
     * @param parentDirName the directory under which the file exists
     * @param childFileName the file to delete
     * @return
     * @throws IOException
     */
    public static boolean deleteFile(String parentDirName, String childFileName) throws IOException {
        File file = checkFileExists(parentDirName);
        File fileToDelete = checkFileExists(file.getAbsolutePath() + File.separator + childFileName);
        return fileToDelete.delete();
    }

    /**
     * This method copies the contents of <code>sourceFilePath</code> to <code>destFilePath</code>
     *
     * @param sourceFilePath the file to copy
     * @param destFilePath   the copy
     * @throws IOException exception if there is a problem with the file
     */
    public static void copyFile(String sourceFilePath, String destFilePath) throws IOException {
        copyFile(checkFileExists(sourceFilePath), new File(destFilePath));
    }

    /**
     * This method copies the contents of <code>sourceFile</code> to <code>destFile</code>
     *
     * @param sourceFile the file to copy
     * @param destFile   the copy
     * @throws IOException exception if there is a problem with the file
     */
    public static void copyFile(File sourceFile, File destFile) throws IOException {
        FileChannel inputFileChannel = new FileInputStream(sourceFile).getChannel();
        FileChannel outputFileChannel = new FileOutputStream(destFile).getChannel();
        long offset = 0L;
        long length = inputFileChannel.size();
        final long MAXTRANSFERBUFFERLENGTH = 1024 * 1024;
        try {
            for (; offset < length; ) {
                offset += inputFileChannel.transferTo(offset, MAXTRANSFERBUFFERLENGTH, outputFileChannel);
                inputFileChannel.position(offset);
            }
        } finally {
            try {
                outputFileChannel.close();
            } catch (Exception ignore) {
            }
            try {
                inputFileChannel.close();
            } catch (IOException ignore) {
            }
        }
    }

    /**
     * Concatinates multiple files into a single file using NIO for speed
     *
     * @param sourceFiles
     * @param destFile
     * @throws IOException
     */
    public static void concatFiles(List<File> sourceFiles, File destFile) throws IOException {
        FileOutputStream outFile = new FileOutputStream(destFile);
        FileChannel outChannel = outFile.getChannel();
        for (File f : sourceFiles) {
            FileInputStream fis = new FileInputStream(f);
            FileChannel channel = fis.getChannel();
            channel.transferTo(0, channel.size(), outChannel);
            channel.close();
            fis.close();
        }
        outChannel.close();
    }

    /**
     * This method copies the contents of <code>sourceFilePath</code> to <code>destFilePath</code> using unix/windows
     * system command
     *
     * @param sourceFilePath the file to copy
     * @param destFilePath   the copy
     * @throws IOException exception if there is a problem with the file
     */
    public static void copyFileUsingSystemCall(String sourceFilePath, String destFilePath) throws IOException {
        copyFileUsingSystemCall(checkFileExists(sourceFilePath), new File(destFilePath));
    }

    /**
     * This method copies the contents of <code>sourceFile</code> to <code>destFile</code>  using unix/windows
     * system command
     *
     * @param sourceFile the file to copy
     * @param destFile   the copy
     * @throws IOException exception if there is a problem with the file
     */
    public static void copyFileUsingSystemCall(File sourceFile, File destFile) throws IOException {
        SystemCall call = new SystemCall(logger);
        String command = null;
        try {
            int returnVal;
            if (isWindows) {
                command = "copy " + sourceFile.getAbsolutePath() + " " + destFile.getAbsolutePath();
                returnVal = call.emulateCommandLine(command, false);
            } else {
                command = "cp " + sourceFile.getAbsolutePath() + " " + destFile.getAbsolutePath();
                returnVal = call.emulateCommandLine(command, true);
            }
            if (returnVal != 0) {
                throw new RuntimeException("Execution of " + command + " failed");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Execution of " + command + " failed", e);
        }
    }

    /**
     * This method moves the <code>sourceFilePath</code> to <code>destFilePath</code> using unix/windows
     * system command
     *
     * @param sourceFilePath the file to copy
     * @param destFilePath   the copy
     * @throws IOException exception if there is a problem with the file
     */
    public static void moveFileUsingSystemCall(String sourceFilePath, String destFilePath) throws IOException {
        moveFileUsingSystemCall(checkFileExists(sourceFilePath), new File(destFilePath));
    }

    /**
     * This method moves the <code>sourceFile</code> to <code>destFile</code>  using unix/windows
     * system command
     *
     * @param sourceFile the file to copy
     * @param destFile   the copy
     * @throws IOException exception if there is a problem with the file
     */
    public static void moveFileUsingSystemCall(File sourceFile, File destFile) throws IOException {
        SystemCall call = new SystemCall(logger);
        String command = null;
        try {
            int returnVal;
            if (isWindows) {
                command = "move " + sourceFile.getAbsolutePath() + " " + destFile.getAbsolutePath();
                returnVal = call.emulateCommandLine(command, false);
            } else {
                command = "mv " + sourceFile.getAbsolutePath() + " " + destFile.getAbsolutePath();
                returnVal = call.emulateCommandLine(command, true);
            }
            if (returnVal != 0) {
                throw new RuntimeException("Execution of " + command + " failed");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Execution of " + command + " failed", e);
        }
    }

    public static void concatFilesUsingSystemCall(List<File> sourceFiles, File destFile) throws IOException {
        int i = 0;
        for (File source : sourceFiles) {
            SystemCall call = new SystemCall(logger);
            String command = null;
            try {
                int returnVal;
                if (isWindows) {
                    if (i == 0) {
                        command = "copy " + source.getAbsolutePath() + " " + destFile.getAbsolutePath();
                    } else {
                        command = "copy " + destFile.getAbsolutePath() + "+" + source.getAbsolutePath();
                    }
                    returnVal = call.emulateCommandLine(command, false);
                } else {
                    command = "cat " + source.getAbsolutePath() + " >> " + destFile.getAbsolutePath();
                    returnVal = call.emulateCommandLine(command, true);
                }
                if (returnVal != 0) {
                    throw new RuntimeException("Execution of " + command + " failed");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Execution of " + command + " failed", e);
            }
            i++;
        }
    }

    /**
     * This method throws an IOException if the file represented by fileName does not exist
     *
     * @param fileName
     * @return
     * @throws IOException
     */
    public static File checkFileExists(String fileName) throws IOException {
        File file = new File(fileName);
        if (file.exists()) {
            return file;
        } else {
            throw new IOException(fileName + " does not exist");
        }
    }

    public static void waitForFile(String filePath) throws Exception {
        int maxRetries = 20;
        int retry = 0;
        boolean success = false;
        File file = new File(filePath);
        while (!success && retry < maxRetries) {
            if (retry > 0) {
                Thread.sleep(1000);
            }
            if (file.exists()) {
                success = true;
            }
            retry++;
            if (!success) {
                logger.info("Could not locate file=" + filePath + ", starting retry " + retry);
            }
        }
        if (!success) {
            throw new Exception("Could not locate file=" + filePath);
        }
    }

    /**
     * Throws exception if fileName does not exist and returns absolute file path
     *
     * @param fileName
     * @return
     * @throws IOException
     */
    public static String checkFilePath(String fileName) throws IOException {
        return checkFileExists(fileName).getAbsolutePath();
    }

    public static boolean fileExists(String fileName) {
        File file = new File(fileName);
        return file.exists();
    }

    /**
     * This method creates a new file represented by fileName if it already exists
     *
     * @param fileName
     * @return
     * @throws IOException
     */
    public static File createNewFile(String fileName) throws IOException {
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }
        return file;
    }

    /**
     * This method creates a new file in an existing directory
     *
     * @param existingDir must exist
     * @param fileName    name of the file to be created
     * @return the newly created file
     * @throws IOException
     */
    public static File createFile(String existingDir, String fileName) throws IOException {
        return ensureFileExists(checkFileExists(existingDir).getAbsolutePath() + File.separator + fileName);
    }

    /**
     * Deletes all the directories and files under directoryPath but leaves
     * directoryPath alone
     *
     * @param directoryPath
     */
    public static void cleanDirectory(String directoryPath) {
        File dir = new File(directoryPath);
        cleanDirectory(dir);
    }

    /**
     * Deletes all the directories and files under dir but leaves
     * directoryPath alone
     *
     * @param dir
     */
    public static void cleanDirectory(File dir) {
        File[] dirFiles = dir.listFiles();
        if (dirFiles != null) {
            for (File dirFile : dirFiles) {
                if (dirFile.isFile()) {
                    dirFile.delete();
                } else if (dirFile.isDirectory()) {
                    cleanDirectory(dirFile);
                }
            }
        }
    }

    /**
     * Deletes the directory represented by dir
     *
     * @param dir File object representing directory to be deleted
     * @return true if the directory was deleted, false otherwise
     */
    public static boolean deleteDirectory(File dir) {
        File[] dirFiles = dir.listFiles();
        if (dirFiles != null) {
            for (File dirFile : dirFiles) {
                if (dirFile.isFile()) {
                    dirFile.delete();
                } else if (dirFile.isDirectory()) {
                    deleteDirectory(dirFile);
                }
            }
        }
        return dir.delete();
    }

    /**
     * Deletes the directory represented by directoryPath
     *
     * @param directoryPath absolute path to the directory
     * @return true if the directory was deleted, false otherwise
     */
    public static boolean deleteDirectory(String directoryPath) {
        return deleteDirectory(new File(directoryPath));
    }

    public static void ensureFileContentsEqual(String fileOne, String fileTwo) throws IOException {
        File file1 = checkFileExists(fileOne);
        File file2 = checkFileExists(fileTwo);
        if (file1.length() != file2.length()) {
            throw new IOException("Comparison failed " + fileOne + " size=" + file1.length() + " " + fileTwo + " size=" + file2.length());
        }
        BufferedInputStream file1Stream = new BufferedInputStream(new FileInputStream(file1));
        BufferedInputStream file2Stream = new BufferedInputStream(new FileInputStream(file2));
        int byteRead;
        int byteToCompare;
        long position = 0;
        while ((byteRead = file1Stream.read()) != -1) {
            byteToCompare = file2Stream.read();
            if (byteRead != byteToCompare) {
                throw new IOException("Comparison failed. Byte at position " + position + " in " + fileOne + " different from that in " + fileTwo);
            }
            position++;
        }
    }

    /**
     * Removes all tokens in a directory provided
     *
     * @param pathToDirectory - directory which contains the tokens
     */
    public static void removeAllTokens(String pathToDirectory) {
        String[] dirFiles = (new File(pathToDirectory)).list();
        for (String dirFile : dirFiles) {
            if (dirFile.endsWith(".token")) {
                (new File(dirFile)).delete();
            }
        }
    }

    /**
     * Removes a specific token from a directory
     *
     * @param pathToDirectory - directory which contains the token
     * @param specificToken   - base string of the token to be removed
     */
    public static void removeToken(String pathToDirectory, String specificToken) {
        String[] dirFiles = (new File(pathToDirectory)).list();
        for (String dirFile : dirFiles) {
            if (dirFile.endsWith(specificToken + ".token")) {
                (new File(dirFile)).delete();
                break;
            }
        }
    }

    /**
     * This method is used to "mark" directories after some processing has been done within them.
     * For example, if regenerating RV images, we need to know when the processing has occurred, just in case the
     * image generation code stops in directory 500 out of 1200.
     *
     * @param pathToDirectory - path to the directory to drop the token
     * @param specificToken   - name of the token file
     * @throws java.io.FileNotFoundException - problem dropping the token file
     */
    public static void dropTokenFile(String pathToDirectory, String specificToken) throws IOException {
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(pathToDirectory + File.separator + specificToken + ".token");
            os.write((specificToken + " - This file is indication that some processing error has occurred.  File may be removed.").getBytes());
        } finally {
            if (null != os) {
                os.flush();
                os.close();
            }
        }
    }

    /**
     * This method is used to check directories to see if they have token files - denoting processing.
     * For example, if regenerating RV images, we need to know when the processing has occurred, so checking for the
     * image.token file.
     *
     * @param pathToDirectory - path to the directory to drop the token
     * @param specificToken   - name of the token file
     * @return boolean if the dir has the token requested
     */
    public static boolean directoryHasTokenFile(String pathToDirectory, String specificToken) {
        return new File(pathToDirectory + File.separator + specificToken + ".token").exists();
    }

    /**
     * This method is used to execute any unix shell command whose output is an integer and retrieve the result
     * // todo This method really needs to go away.  We should be using SystemCall and grabbing the outputstream
     *
     * @param command
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static int getCountUsingUnixCall(String command) throws IOException, InterruptedException {
        String[] args = new String[3];
        args[0] = "sh";
        args[1] = "-c";
        args[2] = command;
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(args);
            process.waitFor();
            if (process.exitValue() != 0) {
                throw new RuntimeException("The command (" + command + ") encountered the following error:" + getOutput(process.getErrorStream()));
            } else {
                String output = getOutput(process.getInputStream());
                if (output == null || output.length() == 0) {
                    throw new RuntimeException("The command (" + command + ") produced no output");
                } else {
                    return Integer.parseInt(output);
                }
            }
        } finally {
            if (null != process) {
                closeStream(process.getOutputStream());
                closeStream(process.getInputStream());
                closeStream(process.getErrorStream());
                process.destroy();
            }
        }
    }

    private static void closeStream(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
            }
        }
    }

    private static String getOutput(InputStream inputStream) throws IOException {
        StringBuilder strBuilder = new StringBuilder();
        int ch;
        if (inputStream != null) {
            while ((ch = inputStream.read()) != -1) {
                if (!Character.isWhitespace(ch)) {
                    strBuilder.append((char) ch);
                }
            }
            inputStream.close();
        }
        return strBuilder.toString();
    }

    /**
     * Lists all files that begin with filePrefix
     *
     * @param dirPath
     * @param filePrefix
     * @return
     * @throws IOException
     */
    public static List<File> listFiles(String dirPath, String filePrefix) throws IOException {
        File dir = FileUtil.checkFileExists(dirPath);
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(dirPath + " is not a directory");
        }
        List<File> fileList = new ArrayList<File>();
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.getName().startsWith(filePrefix)) {
                fileList.add(file);
            }
        }
        return fileList;
    }

    /**
     * Locks the file for read and write as other threads might be accessing it at the same time.  Lock can be released
     * either by close the file or releasing FileLock
     *
     * @param fileToLock
     * @param fileToLockPath used for logging only
     * @param maxRetryCount
     * @return
     * @throws IOException
     */
    public static FileLock lockFile(RandomAccessFile fileToLock, String fileToLockPath, int maxRetryCount) throws IOException {
        FileChannel fileChannel = fileToLock.getChannel();
        for (int numTries = 0; numTries < maxRetryCount; numTries++) {
            try {
                return fileChannel.lock();
            } catch (OverlappingFileLockException e) {
                logger.info("Could not obtain lock on " + fileToLockPath + " numTries=" + numTries);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    throw new RuntimeException(e1);
                }
            }
        }
        throw new RuntimeException("Failed to obtain lock on " + fileToLockPath);
    }

    public static void gzCompress(File sourceFile) throws Exception {
        final int BUFFER_SIZE = 100000;
        if (sourceFile.getName().endsWith(".gz")) {
        } else {
            File targetFile = new File(sourceFile.getAbsolutePath() + ".gz");
            FileInputStream fis = new FileInputStream(sourceFile);
            FileOutputStream fos = new FileOutputStream(targetFile);
            GZIPOutputStream gos = new GZIPOutputStream(new BufferedOutputStream(fos));
            BufferedInputStream bis = new BufferedInputStream(fis, BUFFER_SIZE);
            int count;
            byte data[] = new byte[BUFFER_SIZE];
            while ((count = bis.read(data, 0, BUFFER_SIZE)) != -1) {
                gos.write(data, 0, count);
            }
            bis.close();
            gos.close();
        }
    }

    public static void zipCompress(File sourceFile) throws Exception {
        final int BUFFER_SIZE = 100000;
        if (sourceFile.getName().endsWith(".zip")) {
        } else {
            File targetFile = new File(sourceFile.getAbsolutePath() + ".zip");
            FileInputStream fis = new FileInputStream(sourceFile);
            FileOutputStream fos = new FileOutputStream(targetFile);
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos));
            BufferedInputStream bis = new BufferedInputStream(fis, BUFFER_SIZE);
            ZipEntry entry = new ZipEntry(sourceFile.getName());
            zos.putNextEntry(entry);
            int count;
            byte data[] = new byte[BUFFER_SIZE];
            while ((count = bis.read(data, 0, BUFFER_SIZE)) != -1) {
                zos.write(data, 0, count);
            }
            bis.close();
            zos.close();
        }
    }

    public static String tarCompressDirectoryWithSystemCall(File sourceDirectory, String destinationArchiveFilename) {
        SystemCall call = new SystemCall(logger);
        String command = null;
        File destinationArchiveFile = new File(destinationArchiveFilename);
        try {
            int returnVal;
            if (!destinationArchiveFile.getParentFile().exists() || !destinationArchiveFile.getParentFile().canWrite()) {
                throw new RuntimeException("Unable to write to directory " + destinationArchiveFile.getParentFile().getAbsolutePath());
            }
            if (isWindows) {
                throw new RuntimeException("tar compression not yet supported for Windows");
            } else {
                command = "tar -cvf " + destinationArchiveFile.getAbsolutePath() + " -C " + sourceDirectory.getParentFile().getAbsolutePath() + " " + sourceDirectory.getName();
                returnVal = call.emulateCommandLine(command, true);
            }
            if (returnVal != 0) {
                throw new RuntimeException("Execution of " + command + " failed");
            }
            return destinationArchiveFile.getName();
        } catch (InterruptedException e) {
            throw new RuntimeException("Execution of " + command + " failed", e);
        } catch (IOException e) {
            throw new RuntimeException("Execution of " + command + " failed", e);
        }
    }

    public static void copyDirectory(String sourceDirectoryPath, String destinationDirectoryPath) throws IOException {
        copyDirectory(new File(sourceDirectoryPath), new File(destinationDirectoryPath));
    }

    public static void copyDirectory(File sourceDirectory, File destinationDirectory) throws IOException {
        File[] dirFiles = sourceDirectory.listFiles();
        if (dirFiles != null) {
            for (File dirFile : dirFiles) {
                if (dirFile.isFile()) {
                    copyFile(dirFile, new File(destinationDirectory + File.separator + dirFile.getName()));
                } else if (dirFile.isDirectory()) {
                    copyDirectory(dirFile, destinationDirectory);
                }
            }
        }
    }
}
