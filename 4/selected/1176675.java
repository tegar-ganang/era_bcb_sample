package fr.cnes.sitools.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Utility class to copy a file or a folder and its sub-folder
 * 
 * @author m.gond (AKKA Technologies)
 */
public final class FileCopyUtils {

    /**
   * Private constructor for utility class
   */
    private FileCopyUtils() {
        super();
    }

    /**
   * Copy a folder
   * 
   * @param sourceUrl
   *          source path
   * @param destUrl
   *          destination path
   */
    public static void copyAFolder(final String sourceUrl, final String destUrl) {
        File sourceFile = new File(sourceUrl);
        copyAFolder(sourceFile, destUrl);
    }

    /**
   * Copy a folder excluding some files
   * 
   * @param sourceUrl
   *          source path
   * @param destUrl
   *          destination path
   * @param exclude
   *          excluding string
   */
    public static void copyAFolderExclude(final String sourceUrl, final String destUrl, String exclude) {
        File sourceFile = new File(sourceUrl);
        File[] fileList = sourceFile.listFiles();
        if (fileList != null) {
            for (int i = 0; i < fileList.length; i++) {
                if (!fileList[i].getName().equals(".svn")) {
                    copyAFolderExclude(fileList[i], destUrl, exclude);
                }
            }
        }
    }

    /**
   * Copy the content of a folder and its sub-folders.
   * 
   * @param file
   *          the folder to copy
   * @param destUrl
   *          the destination path
   * @param excludeExt
   *          exclude a folder
   */
    public static void copyAFolderExclude(final File file, final String destUrl, final String excludeExt) {
        if (file != null && file.isFile()) {
            copyAFile(file.getAbsolutePath(), destUrl + "/" + file.getName());
        }
        if (file != null && file.isDirectory() && !file.getName().equals(excludeExt)) {
            File fileDir = new File(destUrl + "/" + file.getName());
            fileDir.mkdirs();
            File[] fileList = file.listFiles();
            for (int i = 0; i < fileList.length; i++) {
                copyAFolderExclude(fileList[i], destUrl + "/" + file.getName(), excludeExt);
            }
        }
    }

    /**
   * Copy the content of a folder and its sub-folders.
   * 
   * @param file
   *          the folder to copy
   * @param destUrl
   *          the destination URL
   */
    public static void copyAFolder(final File file, final String destUrl) {
        if (file != null && file.isFile()) {
            copyAFile(file.getAbsolutePath(), destUrl + "/" + file.getName());
        }
        if (file != null && file.isDirectory()) {
            File fileDir = new File(destUrl + "/" + file.getName());
            fileDir.mkdir();
            File[] fileList = file.listFiles();
            for (int i = 0; i < fileList.length; i++) {
                copyAFolder(fileList[i], destUrl + "/" + file.getName());
            }
        }
    }

    /**
   * Copy a file at the source URL to the destination URL.
   * 
   * @param entree
   *          the source file URL
   * @param sortie
   *          the destination file URL
   */
    public static void copyAFile(final String entree, final String sortie) {
        FileChannel in = null;
        FileChannel out = null;
        try {
            in = new FileInputStream(entree).getChannel();
            out = new FileOutputStream(sortie).getChannel();
            in.transferTo(0, in.size(), out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
