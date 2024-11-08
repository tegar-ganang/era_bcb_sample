package com.patientis.framework.utility;

import java.io.*;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.patientis.framework.logging.Log;

/**
 * Utility class for interaction with the file system.
 *
 * Design Patterns: <a href="/functionality/rm/1000075.html">Utilities</a>
 * <br/>
 */
public class FileSystemUtil {

    /**
	 * Determine if file system is windows
	 */
    private static boolean isWindowsOS = false;

    /**
	 * File separator
	 */
    private static String fileSeparator = "/";

    private static String userDir = ".";

    private static String homePath = ".";

    /**
	 * Static constructor to determine if on windows
	 */
    static {
        String osname = System.getProperty("os.name");
        if (osname != null) {
            isWindowsOS = osname.toLowerCase().contains("windows");
        }
        String userdir = System.getProperty("user.home");
        if (userdir != null) {
            userDir = userdir;
        }
        String homepath = System.getenv("HOMEPATH");
        if (homepath != null) {
            homePath = homepath;
        }
    }

    /**
	 * Return the new line for the operating system
	 * 
	 * @return
	 */
    public static String nl() {
        if (isWindowsOS) {
            return "\r\n";
        } else {
            return "\n";
        }
    }

    /**
	 * Get the file separator e.g /
	 * @return
	 */
    public static String getFilePathSeparator() {
        return fileSeparator;
    }

    /**
	 * Return the number of specified new lines
	 * 
	 * @param number
	 * @return
	 */
    public static String nl(int number) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < number; i++) {
            sb.append(nl());
        }
        return sb.toString();
    }

    /**
	 * Create a directory for the given path
	 * 
	 * @param path
	 */
    public static void createDirectory(String path) {
        File file = new File(getFilePath(path, ""));
        file.mkdirs();
    }

    /**
	 * Determine if the filename in the path directory exists
	 * 
	 * @param path
	 * @param filename
	 * @return
	 */
    public static boolean exists(String path, String filename) {
        return exists(getFilePath(path, filename));
    }

    /**
	 * Determine if the file exists
	 * 
	 * @param filepath
	 * @return
	 */
    public static boolean exists(String filepath) {
        File file = new File(filepath);
        return file.exists();
    }

    /**
	 * Return a binary file
	 * 
	 * @param path
	 * @param filename
	 * @return
	 * @throws IOException 
	 */
    public static byte[] getBinaryContents(String path, String filename) throws IOException {
        if (!exists(path, filename)) {
            System.err.println("File does not exist: " + getFilePath(path, filename));
            return null;
        } else {
            File file = new File(getFilePath(path, filename));
            DataInputStream dis = new DataInputStream(new FileInputStream(file));
            byte[] binaryBytes = new byte[(int) file.length()];
            dis.readFully(binaryBytes);
            return binaryBytes;
        }
    }

    /**
	 * Return a binary file
	 * 
	 * @param file file
	 * @return
	 * @throws IOException 
	 */
    public static byte[] getBinaryContents(File file) throws IOException {
        DataInputStream dis = new DataInputStream(new FileInputStream(file));
        byte[] binaryBytes = new byte[(int) file.length()];
        dis.readFully(binaryBytes);
        return binaryBytes;
    }

    /**
	 * Return the text contents of a file
	 * 
	 * @param path
	 * @param filename
	 * @return
	 */
    public static String getTextContents(String path, String filename) {
        return getTextContents(getFilePath(path, filename));
    }

    /**
	 * Returns the string contents of the file 
	 * 
	 * @param path
	 * @param filename
	 * @return
	 * @throws IOException 
	 */
    public static String getTextContents(File file) throws IOException {
        return getTextContents(file, false);
    }

    /**
	 * Returns the string contents of the file 
	 * 
	 * @param path
	 * @param filename
	 * @return
	 * @throws IOException 
	 */
    public static String getTextContents(File file, boolean utf8) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String s = reader.readLine();
        StringBuffer sb = new StringBuffer((int) file.length());
        while (s != null) {
            sb.append(s);
            sb.append("\r\n");
            s = reader.readLine();
        }
        reader.close();
        if (utf8) {
            return new String(sb.toString().getBytes("UTF-8"));
        } else {
            return sb.toString();
        }
    }

    /**
	 * Returns the string contents of the file 
	 * 
	 * @param path
	 * @param filename
	 * @return
	 */
    public static String getTextContents(String filepath) {
        if (!exists(filepath)) {
            System.err.println("File does not exist: " + filepath);
            return "";
        } else {
            try {
                File file = new File(filepath);
                return getTextContents(file);
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
                return "";
            }
        }
    }

    /**
	 * Delete a file
	 * 
	 * @param path
	 * @param filename
	 */
    public static void deleteFile(String path, String filename) {
        File file = new File(getFilePath(path, filename));
        if (file.exists()) {
            if (!file.delete()) {
                System.err.println("Unable to delete " + path + filename);
            }
        }
    }

    /**
	 * Delete a file
	 * TODO replace the System.err with localized exceptions
	 * 
	 * @param path
	 * @param filename
	 */
    public static void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            if (!file.delete()) {
                System.err.println("Unable to delete " + filePath);
            }
        }
    }

    /**
	 * Create a file with contents 
	 * 
	 * @param filepath
	 * @param content
	 * @throws IOException
	 */
    public static File createFile(String filepath, String content) throws IOException {
        try {
            File file = new File(filepath);
            if (file.exists()) {
                if (!file.delete()) {
                    System.err.println("Unable to delete " + filepath);
                }
            }
            file.createNewFile();
            FileWriter fw = new FileWriter(file);
            fw.write(content);
            fw.close();
            return file;
        } catch (IOException ioe) {
            throw new IOException(filepath, ioe);
        }
    }

    /**
	 * Create a file with content
	 * 
	 * @param path
	 * @param filename
	 * @param content
	 * @throws IOException
	 */
    public static void createFile(String path, String filename, String content) throws IOException {
        createDirectory(getFilePath(path, ""));
        createFile(getFilePath(path, filename), content);
    }

    /**
	 * Create a binary file with specified contents
	 * 
	 * @param path
	 * @param filename
	 * @param content
	 * @throws IOException
	 */
    public static void createBinaryFile(String path, String filename, byte[] content) throws IOException {
        createDirectory(getFilePath(path, ""));
        File file = new File(getFilePath(path, filename));
        createBinaryFile(file, content);
    }

    /**
	 * Create a binary file with specified contents
	 * 
	 * @param path
	 * @param filename
	 * @param content
	 * @throws IOException
	 */
    public static void createBinaryFile(File file, byte[] content) throws IOException {
        if (file != null) {
            if (file.exists()) {
                if (!file.delete()) {
                    System.err.println("Unable to delete " + file.getAbsolutePath());
                }
            }
            file.createNewFile();
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
            dos.write(content);
            dos.flush();
            dos.close();
        }
    }

    /**
	 * Return the files in the specified directory
	 * 
	 * @param dirpath
	 * @return
	 * @throws Exception
	 */
    public static File[] getFiles(String dirpath) throws Exception {
        File file = new File(dirpath);
        if (file.exists()) {
            if (file.isDirectory()) {
                return file.listFiles();
            } else {
                throw new IllegalArgumentException("Not a directory: " + dirpath);
            }
        } else {
            throw new FileNotFoundException(dirpath);
        }
    }

    /**
	 * Return the files in the specified directory
	 * 
	 * @param dirpath
	 * @return
	 * @throws Exception
	 */
    public static File[] getFilesAlphabetic(String dirPath) throws Exception {
        File[] files = getFiles(dirPath);
        List<String> filenames = new ArrayList<String>();
        for (File file : files) {
            filenames.add(file.getAbsolutePath());
        }
        Collections.sort(filenames);
        List<File> sortedFiles = new ArrayList<File>();
        for (String filename : filenames) {
            sortedFiles.add(new File(filename));
        }
        return sortedFiles.toArray(new File[] {});
    }

    /**
	 * Get the file path 
	 * 
	 * @param path
	 * @param filename
	 * @return
	 */
    public static String getFilePath(String filepath, String filename) {
        if (filepath == null) {
            return filename;
        }
        String path = filepath;
        if (isWindowsOS) {
            if (!path.endsWith("\\")) {
                path = path + "\\";
            }
        } else {
            if (!path.endsWith("/")) {
                path = path + "/";
            }
        }
        return path + filename;
    }

    /**
	 * Determines if the system property os.name has windows in it
	 * 
	 * @return true if this is likely a windows os
	 */
    public static boolean isWindowsOS() {
        String osname = System.getProperty("os.name");
        if (osname != null) {
            return osname.toLowerCase().contains("windows");
        } else {
            return false;
        }
    }

    /**
	 * Creates a  buffered writer for the file.  Tests the file exists and can be written to.
	 * 
	 * @param file file that exists and can be written to
	 * @return writer
	 * @throws Exception
	 */
    public static BufferedWriter getWriter(File file) throws Exception {
        if (file == null) {
            throw new NullFilePointerException();
        }
        if (!file.exists()) {
            throw new FileDoesNotExistException(file.getAbsolutePath());
        }
        if (!file.canWrite()) {
            throw new FileCanNotBeWrittenToException(file.getAbsolutePath());
        }
        return new BufferedWriter(new FileWriter(file));
    }

    /**
	 * Iterate through all files in the filesystem starting at rootDirectory
	 * 
	 * @param rootDirectory
	 * @param handler
	 */
    public static void navigateFilesystem(File rootDirectory, IHandleFile handler) throws Exception {
        handler.handleFile(rootDirectory);
        if (rootDirectory.isDirectory() && handler.isValidDir(rootDirectory)) {
            for (File file : rootDirectory.listFiles()) {
                navigateFilesystem(file, handler);
            }
        }
    }

    /**
	 * Process the contents of the file one line at a time
	 * 
	 * @param file
	 * @param handler
	 * @throws IOException
	 */
    public static void processTextContents(File file, IHandleLine handler) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String s = reader.readLine();
        while (s != null) {
            handler.processLine(s);
            s = reader.readLine();
        }
        reader.close();
    }

    /**
	 * Create a file with contents 
	 * 
	 * @param filepath
	 * @param content
	 * @throws IOException
	 */
    public static void appendFile(String filepath, String addendum) throws IOException {
        try {
            File file = new File(filepath);
            FileWriter fw = new FileWriter(file, true);
            fw.write(addendum);
            fw.close();
        } catch (IOException ioe) {
            throw new IOException(filepath, ioe);
        }
    }

    /**
	 * @return the userDir
	 */
    public static String getUserDir() {
        return userDir;
    }

    /**
	 * @param userDir the userDir to set
	 */
    public static void setUserDir(String userDir) {
        FileSystemUtil.userDir = userDir;
    }

    /**
	 * @return the homePath
	 */
    public static String getHomePath() {
        return homePath;
    }

    /**
	 * @param homePath the homePath to set
	 */
    public static void setHomePath(String homePath) {
        FileSystemUtil.homePath = homePath;
    }

    /**
	 * 
	 * @param files
	 * @param filename
	 * @return
	 */
    public static File getFileFromList(List<File> files, String filename) {
        for (File file : files) {
            if (file.getName().toLowerCase().contains(filename.toLowerCase())) {
                return file;
            }
        }
        return null;
    }

    /**
	 * Read
	 * 
	 * @param output
	 * @param url
	 * @throws IOException
	 */
    public static final void readHttpURL(File output, HttpURLConnection url) throws IOException {
        url.setAllowUserInteraction(true);
        url.connect();
        DataInputStream in = new DataInputStream(url.getInputStream());
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(output));
        try {
            if (url.getResponseCode() != HttpURLConnection.HTTP_OK) {
                while (true) {
                    out.write((char) in.readUnsignedByte());
                }
            }
        } catch (EOFException ex) {
        } catch (Exception ex) {
            Log.exception(ex);
        } finally {
            out.close();
        }
    }

    /**
	 * 
	 * @param dir
	 * @return
	 */
    public static List<File> getFilesFromDirectory(File dir) throws Exception {
        final List<File> files = new ArrayList<File>();
        if (dir.isDirectory()) {
            navigateFilesystem(dir, new IHandleFile() {

                @Override
                public void handleFile(File file) throws Exception {
                    if (!file.isDirectory() && !file.isHidden()) {
                        files.add(file.getAbsoluteFile());
                    }
                }

                @Override
                public boolean isValidDir(File fileDir) throws Exception {
                    return true;
                }
            });
        }
        return files;
    }
}
