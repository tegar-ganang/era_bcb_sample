package fi.hip.gb.utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.RemoteException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import fi.hip.gb.core.Config;

/**
 * Utilities for accessing files and directories. Operations are not
 * relative to GBAgent home directory.
 * 
 * @author Juho Karppinen
 */
public class FileUtils {

    private static Log log = LogFactory.getLog(FileUtils.class);

    /**
     * Gets the filename from filepath
     * 
     * @param filepath
     *            path of the file
     * @return filename without any path information
     */
    public static String getFilename(String filepath) {
        int indexOf = filepath.lastIndexOf("/");
        if (indexOf != -1) return filepath.substring(indexOf + 1);
        indexOf = filepath.lastIndexOf("\\");
        if (indexOf != -1) return filepath.substring(indexOf + 1);
        return filepath;
    }

    /**
     * Gets the filename from URL.
     * 
     * @param fileURL URL for the file
     * @return filename without any path information
     */
    public static String getFilename(URL fileURL) {
        return getFilename(convertFromJarURL(fileURL).getPath());
    }

    /**
     * Gets the parent directory for file
     * 
     * @param filepath
     *            path of the file with filename
     * @return directory name
     */
    public static String getPath(String filepath) {
        return new File(filepath).getParent();
    }

    /**
     * Gets the parent directory for file
     * 
     * @param fileURL
     *            URL for the file
     * @return directory name
     */
    public static String getPath(URL fileURL) {
        return getPath(fileURL.getPath());
    }

    /**
     * Gets existence status of the given filename
     * 
     * @param fileName
     *            full path of the file
     * @return true if file exists, false otherwise
     */
    public static boolean exists(String fileName) {
        File file = new File(fileName);
        return file.exists();
    }

    /**
     * Gets existence status of the given URL
     * 
     * @param fileURL
     *            URL for the file, can be on local filesystem or somewhere http
     * @return true if file exists, false otherwise
     */
    public static boolean exists(URL fileURL) {
        if (fileURL.getProtocol().equalsIgnoreCase("file")) return new File(fileURL.getFile()).exists();
        try {
            URLConnection uc = fileURL.openConnection();
            return (uc.getContentLength() > 0);
        } catch (IOException e) {
            log.debug("URL " + fileURL + " does not exits: " + e.getMessage());
            return false;
        }
    }

    /**
     * Convert relative path to absolute.
     * @param path path of the file, can be absolute or relative to GBAgent home directory.
     * @param gbHomeDirectory home directory of GBAgent
     * @return absolute path to the file
     */
    public static URL getAbsolutePath(URL path, String gbHomeDirectory) {
        try {
            if (path.getFile().startsWith("/") || path.getFile().startsWith("\\")) {
                return path;
            }
            return new File(gbHomeDirectory, path.getFile()).toURL();
        } catch (MalformedURLException mue) {
            log.error("Could not convert " + path + " to absolute path", mue);
            return path;
        }
    }

    /**
     * Write given content to the file.
     * 
     * @param targetURL URL of the target file
     * @param content content of the file
     * @throws IOException if operation failed
     */
    public static void writeFile(URL targetURL, String content) throws IOException {
        writeFile(targetURL.getPath(), content);
    }

    /**
     * Write given content to the file.
     * 
     * @param filePath destination file
     * @param content content of the file
     * @throws IOException if operation failed
     */
    public static void writeFile(String filePath, String content) throws IOException {
        PrintWriter out = new PrintWriter(new FileWriter(filePath));
        out.print(content);
        out.flush();
        out.close();
    }

    /**
     * Write given content to the file. Destination file will be overriden if it
     * exist.
     * 
     * @param fileName
     *            destination
     * @param content
     *            content of the file
     */
    public static void writeFile(String fileName, byte[] content) {
        try {
            OutputStream out = new FileOutputStream(fileName);
            out.write(content);
            out.flush();
            out.close();
        } catch (EOFException e) {
            log.error("End of stream");
            e.printStackTrace();
        } catch (Exception e) {
            log.error(e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Read file and return its contents.
     * 
     * @param fileName
     *            path to file
     * @return content of the file
     * @throws FileNotFoundException
     *             if file cannot be found
     * @throws IOException
     *             if reading cannot be performed
     */
    public static String readFile(String fileName) throws FileNotFoundException, IOException {
        return readFile(new File(fileName));
    }

    /**
     * Read file and return its contents.
     * 
     * @param file
     *            file to be read
     * @return content of the file
     * @throws FileNotFoundException
     *             if file cannot be found
     * @throws IOException
     *             if reading cannot be performed
     */
    public static String readFile(File file) throws FileNotFoundException, IOException {
        return new String(readBytes(file));
    }

    /**
     * Read content of file from local or remote URL.
     * 
     * @param url
     *            URL of the file
     * @return content of the file in String
     * @throws FileNotFoundException
     *             if file cannot be found
     * @throws IOException
     *             if reading cannot be performed
     */
    public static String readFile(URL url) throws FileNotFoundException, IOException {
        return new String(readBytes(url));
    }

    /**
     * Read file and return its contents as bytes.
     * 
     * @param inputFilename
     *            path to file
     * @return byte array of the file
     * @throws FileNotFoundException
     *             if file cannot be found
     * @throws IOException
     *             if reading cannot be performed
     */
    public static byte[] readBytes(String inputFilename) throws FileNotFoundException, IOException {
        File file = new File(inputFilename);
        return readBytes(file);
    }

    /**
     * Read file and return its contents as bytes.
     * 
     * @param inputFile
     *            file to be read
     * @return byte array of the file
     * @throws FileNotFoundException
     *             if file cannot be found
     * @throws IOException
     *             if reading cannot be performed
     */
    public static byte[] readBytes(File inputFile) throws FileNotFoundException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copyStream(new FileInputStream(inputFile), baos);
        return baos.toByteArray();
    }

    /**
     * Read content of file from local or remote URL.
     * 
     * @param inputURL
     *            URL of the file
     * @return content of the file in byte array
     * @throws FileNotFoundException
     *             if file cannot be found
     * @throws IOException
     *             if reading cannot be performed
     */
    public static byte[] readBytes(URL inputURL) throws FileNotFoundException, IOException {
        if (inputURL.getProtocol().equalsIgnoreCase("file")) return readBytes(new File(inputURL.getFile()));
        URLConnection uc = inputURL.openConnection();
        InputStream in = new BufferedInputStream(uc.getInputStream());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copyStream(in, baos);
        return baos.toByteArray();
    }

    /**
     * Gets the normal URL from URL with jar-scheme.
     * @param jarURL the jar URL
     * @return normal URL
     */
    public static URL convertFromJarURL(URL jarURL) {
        String jarfile = jarURL.toString();
        if (jarfile.toLowerCase().startsWith("jar:")) {
            jarfile = jarfile.substring(4);
        }
        if (jarfile.endsWith("!/")) {
            jarfile = jarfile.substring(0, jarfile.indexOf("!/"));
        }
        try {
            return new URL(jarfile);
        } catch (MalformedURLException e) {
            return jarURL;
        }
    }

    /**
     * Copys the file to given directory. If target file already exists throw
     * RemoteException
     * 
     * @param fileUrl
     *            URL of the source file
     * @param targetDirectory
     *            target directory, created if not already exist
     * @throws RemoteException
     *             if file was already found on the location
     * @throws FileNotFoundException
     *             if input file cannot be found
     * @throws IOException
     *             if error occured while copying file
     */
    public static void copyAskFile(URL fileUrl, String targetDirectory) throws FileNotFoundException, RemoteException, IOException {
        String filename = getFilename(fileUrl);
        if (exists(Config.localizedPath(targetDirectory + "/" + filename))) {
            throw new RemoteException("Target file " + filename + " already exists on directory " + targetDirectory);
        }
        copyFile(fileUrl, targetDirectory);
    }

    /**
     * Copy a local/remote file to given directory overriding the old one.
     * 
     * @param inputURL
     *            URL of the source file
     * @param targetDirectory
     *            target directory under file is copied
     * @return URL of the new location
     * @throws FileNotFoundException
     *             if input file cannot be found
     * @throws IOException
     *             if error occured while copying file
     */
    public static URL copyFile(URL inputURL, String targetDirectory) throws FileNotFoundException, IOException {
        File outputFile = new File(targetDirectory + "/" + getFilename(inputURL));
        return copyFile(inputURL, outputFile);
    }

    /**
     * Copy a local or remote file.
     * 
     * @param inputURL
     *            URL of the file to be copied
     * @param outputFile
     *            target file, if the target directory doesn't exists it will be created
     * @return URL for the new file location
     * @throws FileNotFoundException
     *             if input file cannot be found
     * @throws IOException
     *             if error occured while copying
     */
    public static URL copyFile(URL inputURL, File outputFile) throws FileNotFoundException, IOException {
        createDir(getPath(outputFile.getAbsolutePath()));
        URLConnection uc = inputURL.openConnection();
        InputStream in = new BufferedInputStream(uc.getInputStream());
        copyStream(in, new FileOutputStream(outputFile));
        return new URL("file:" + outputFile.getAbsolutePath());
    }

    /**
     * Copy content of <code>InputStream</code> to given
     * <code>OutputStream</code>. Streams will be closed after copying is
     * completed.
     * 
     * @param in
     *            input stream
     * @param out
     *            target stream
     * @throws IOException
     *             error occured during the operations
     */
    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] line = new byte[16384];
        int bytes = -1;
        while ((bytes = in.read(line)) != -1) out.write(line, 0, bytes);
        in.close();
        out.close();
    }

    /**
     * Moves the file to given target. Directory must already exists.
     * 
     * @param inputFile the source file
     * @param outputFile target URL for the file.
     * @return URL for the new location
     * @throws FileNotFoundException if input file not found
     * @throws IOException if error occured while moving file
     */
    public static URL moveFile(File inputFile, File outputFile) throws FileNotFoundException, IOException {
        log.debug("Moving file " + inputFile + " of " + inputFile.length() + " bytes to " + outputFile);
        if (inputFile.exists() == false) {
            throw new FileNotFoundException(inputFile.getPath() + " can't be moved, not found");
        }
        if (inputFile.renameTo(outputFile) == false) {
            copyStream(new FileInputStream(inputFile), new FileOutputStream(outputFile));
            inputFile.delete();
        }
        return outputFile.toURL();
    }

    /**
     * Move the file to given directory. Directory must already exists.
     * 
     * @param inputFile the source file
     * @param targetDirectory
     *            target directory where file is moved
     * @return URL of the new location
     * @throws FileNotFoundException
     *             if input file cannot be found
     * @throws IOException
     *             if error occured while copying file
     */
    public static URL moveFile(File inputFile, String targetDirectory) throws FileNotFoundException, IOException {
        File outputFile = new File(targetDirectory + "/" + inputFile.getName());
        moveFile(inputFile, outputFile);
        return outputFile.toURL();
    }

    /**
     * Gets size of file
     * 
     * @param fileURL
     *            URL of the file
     * @return size in bytes
     */
    public static long getFileSize(URL fileURL) {
        return new File(fileURL.getPath()).length();
    }

    /**
     * Gets size of file prensented user friendly way
     * 
     * @param fileURL
     *            URL of the file
     * @return size in bytes/kilobytes/megabytes depending on the size of the
     *         file
     */
    public static String getFormatedFileSize(URL fileURL) {
        long size = getFileSize(fileURL);
        return TextUtils.getFileSize(size);
    }

    /**
     * Creates empty directory if it doesn't exist.
     * 
     * @param dir
     *            directorypath of created directory
     */
    public static void createDir(String dir) {
        File directory = new File(dir);
        if (!directory.exists()) directory.mkdirs();
    }

    /**
     * Recreate directory and remove its all files and subdirectories. After
     * deletion create empty directory with the same name.
     * 
     * @param dir
     *            directorypath of recreated directory
     */
    public static void recreateDir(String dir) {
        File directory = new File(dir);
        removeDir(directory);
        directory.mkdirs();
    }

    /**
     * Remove the file
     * 
     * @param file
     *            file to be removed
     * @return true if file was succesfully removed
     */
    public static boolean removeFile(String file) {
        return new File(file).delete();
    }

    /**
     * Recursively remove directory and its all subdirectories and files.
     * 
     * @param dir
     *            directory to be removed
     * @return true if directory was successfully removed
     */
    public static boolean removeDir(File dir) {
        if (dir.exists() == true) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) removeDir(files[i]); else files[i].delete();
            }
            return dir.delete();
        }
        return false;
    }
}
