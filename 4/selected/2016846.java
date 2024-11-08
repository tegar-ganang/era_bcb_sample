package org.moonwave.dconfig.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Jonathan Luo
 */
public class FileUtil {

    private static final Log log = LogFactory.getLog(FileUtil.class);

    public FileUtil() {
    }

    /**
     * Returns URL for a relative file path.
     * 
     * @param filePath file path
     * @return URL for a relative file path.
     */
    public static java.net.URL toURL(String filePath) {
        java.net.URL fileURL = FileUtil.class.getClassLoader().getResource(filePath);
        if (fileURL == null) fileURL = ClassLoader.class.getResource("/" + filePath);
        return fileURL;
    }

    public static void copy(File fromFile, File toFile) throws IOException {
        String fromFilename = fromFile.getPath();
        String toFileName = toFile.getPath();
        if (!fromFile.exists()) throw new IOException("FileCopy: " + "no such source file: " + fromFilename);
        if (!fromFile.isFile()) throw new IOException("FileCopy: " + "can't copy directory: " + fromFilename);
        if (!fromFile.canRead()) throw new IOException("FileCopy: " + "source file is unreadable: " + fromFilename);
        if (toFile.isDirectory()) toFile = new File(toFile, fromFile.getName());
        if (toFile.exists()) {
            if (!toFile.canWrite()) throw new IOException("FileCopy: " + "destination file is unwriteable: " + toFileName);
            if (AppState.isVerbose()) System.out.print("Overwrite existing file " + toFile.getName() + "? (Y/N): ");
            System.out.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            String response = in.readLine();
            if (!response.equals("Y") && !response.equals("y")) throw new IOException("FileCopy: " + "existing file was not overwritten.");
        } else {
            String parent = toFile.getParent();
            if (parent == null) parent = System.getProperty("user.dir");
            File dir = new File(parent);
            if (!dir.exists()) throw new IOException("FileCopy: " + "destination directory doesn't exist: " + parent);
            if (dir.isFile()) throw new IOException("FileCopy: " + "destination is not a directory: " + parent);
            if (!dir.canWrite()) throw new IOException("FileCopy: " + "destination directory is unwriteable: " + parent);
        }
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(fromFile);
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) to.write(buffer, 0, bytesRead);
        } finally {
            if (from != null) try {
                from.close();
            } catch (IOException e) {
                ;
            }
            if (to != null) try {
                to.close();
            } catch (IOException e) {
                ;
            }
        }
    }

    public static void copy(java.net.URI fromFileName, java.net.URI toFileName) throws IOException {
        copy(new File(fromFileName), new File(toFileName));
    }

    public static void copy(String fromFileName, String toFileName) throws IOException {
        try {
            java.net.URL fromURL = toURL(fromFileName);
            java.net.URL toURL = toURL(toFileName);
            copy(fromURL.toURI(), toURL.toURI());
        } catch (Exception e) {
            throw new IOException("File copy failed");
        }
    }

    public static void copyFromJarToLocal(String fromFilename, String localFilename) throws IOException {
        try {
            java.net.URL fromURL = toURL(fromFilename);
            copy(new File(fromURL.toURI()), new File(localFilename));
        } catch (Exception e) {
            throw new IOException("File copy failed");
        }
    }

    /**
     * Copies the contents of source file to a target file. Creates the target file
     * automatically if it does not exist.
     * 
     * @param sFromFile the source file physicla path.
     * @param sToFile the target file physical path.
     * @throws IOException
     */
    public static void copyTextFile(String sFromFile, String sToFile) throws IOException {
        File fromFile = new File(sFromFile);
        if (!fromFile.exists()) {
            throw new IOException("File to copy, " + fromFile.getAbsolutePath() + ", does not exist.");
        }
        File toFile = null;
        try {
            toFile = createFile(sToFile);
        } catch (Exception e) {
            throw new IOException("failed to create file: '" + sToFile + "'");
        }
        FileInputStream inFile = null;
        FileOutputStream outFile = null;
        try {
            inFile = new FileInputStream(fromFile);
            outFile = new FileOutputStream(toFile);
        } catch (FileNotFoundException e) {
            throw new IOException(e.getMessage());
        }
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(inFile));
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outFile)), true);
            while (true) {
                String str = in.readLine();
                if (str == null) break;
                out.println(str);
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Creates a file on the hard drive.
     * 
     * @param filename a file with full path.
     * @return the File object created.
     * @throws IOException
     */
    public static File createFile(String filename) throws IOException {
        File file = new File(filename);
        try {
            if (file.isDirectory()) throw new IOException("The path '" + file.getPath() + "' does not specify a file.");
            if (!file.isFile()) {
                file = file.getAbsoluteFile();
                File parentDir = new File(file.getParent());
                if (!parentDir.exists()) parentDir.mkdirs();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new IOException(e.getMessage());
        }
        return file;
    }

    /**
     * Removes files under given directory for a given filename filter. 
     * 
     * @param sDir the directory name to remove files from.
     * @param sFilter filename fileter, for example, ".log"
     * @return true if operation is successful, false otherwise.
     */
    public static boolean removeFiles(String sDir, String sFilter) {
        boolean bRet = true;
        try {
            File file = new File(sDir);
            if (file.isDirectory()) {
                File[] fileList = file.listFiles();
                for (int i = 0; i < fileList.length; i++) {
                    int idx = fileList[i].getName().lastIndexOf(sFilter);
                    if (idx > 0) {
                        if (fileList[i].delete()) log.info(fileList[i].getAbsolutePath() + " is deleted"); else log.info("failed to delete file " + fileList[i].getAbsolutePath());
                    }
                }
            }
        } catch (Exception e) {
            bRet = false;
            log.error(e);
        }
        return bRet;
    }
}
