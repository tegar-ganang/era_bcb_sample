package com.tegsoft.tobe.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import net.sf.jasperreports.engine.util.FileResolver;
import org.apache.commons.io.IOUtils;

public class FileUtil implements FileResolver {

    private static File find(final File path, final String fileName) throws Exception {
        if (path == null) {
            return null;
        }
        if (!path.exists()) {
            return null;
        }
        if (new File(path.getAbsolutePath() + File.separator + fileName).exists()) {
            return new File(path.getAbsolutePath() + File.separator + fileName);
        }
        File files[] = path.listFiles();
        for (File file : files) {
            if (file == null) {
                continue;
            }
            if (!file.isDirectory()) {
                continue;
            }
            File result = find(file, fileName);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
	 * Finds a file in sub folders of forms directory in deployment folder. This
	 * function is same as calling find(fileName,false)
	 * 
	 * @param fileName
	 *            Name of the file to find
	 * @return Returns File object for the filename if file is under forms
	 *         folder. Null otherwise.
	 * @throws Exception
	 */
    public static File find(final String fileName) throws Exception {
        return find(fileName, false);
    }

    /**
	 * Finds a file in sub folders of forms directory in deployment folder.
	 * 
	 * @param fileName
	 *            Name of the file to find
	 * @param throwException
	 *            If True, an exception with "File Not Found" message will be
	 *            thrown when file could not be found. Otherwise exception will
	 *            not be thrown.
	 * @return Returns File object for the filename if file is under forms
	 *         folder. Null otherwise.
	 * @throws Exception
	 */
    public static File find(final String fileName, final boolean throwException) throws Exception {
        String contextPath = UiUtil.getParameter("RealPath.Context");
        if (NullStatus.isNotNull(contextPath)) {
            String path = contextPath + "/customForms";
            if (NullStatus.isNotNull(path)) {
                File file = find(new File(path), fileName);
                if (file != null) {
                    return file;
                }
            }
        }
        String path = UiUtil.getParameter("RealPath.forms");
        if (NullStatus.isNotNull(path)) {
            File file = find(new File(path), fileName);
            if (file != null) {
                return file;
            }
        }
        if (throwException) {
            throw new Exception("File Not Found fileName:" + fileName + " path:" + path);
        }
        return null;
    }

    public static String readAll(File file) throws Exception {
        StringBuffer contents = new StringBuffer();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String text = null;
        while ((text = reader.readLine()) != null) {
            contents.append(text).append(System.getProperty("line.separator"));
        }
        if (reader != null) {
            reader.close();
        }
        return contents.toString();
    }

    public File resolveFile(String fileName) {
        try {
            return find(fileName);
        } catch (Exception ex) {
            UiUtil.handleException(ex);
        }
        return null;
    }

    public static boolean deleteDir(String dir) {
        return deleteDir(new File(dir));
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public static boolean deleteAllFilesInDir(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            for (int i = 0; i < children.length; i++) {
                boolean success = children[i].delete();
                if (!success) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean deleteMatchingFilesInDir(File dir, String prefix, String suffix) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            for (int i = 0; i < children.length; i++) {
                if (NullStatus.isNotNull(suffix)) {
                    if (children[i].getName().endsWith(suffix)) {
                        if (NullStatus.isNotNull(prefix)) {
                            if (children[i].getName().startsWith(prefix)) {
                                boolean success = children[i].delete();
                                if (!success) {
                                    return false;
                                }
                            }
                        } else {
                            boolean success = children[i].delete();
                            if (!success) {
                                return false;
                            }
                        }
                    }
                } else {
                    if (NullStatus.isNotNull(prefix)) {
                        if (children[i].getName().startsWith(prefix)) {
                            boolean success = children[i].delete();
                            if (!success) {
                                return false;
                            }
                        }
                    } else {
                        boolean success = children[i].delete();
                        if (!success) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public static void createZipPackage(String srcFolder, String destZipFile) throws Exception {
        createZipPackage(srcFolder, destZipFile, null, null);
    }

    public static void createZipPackage(String srcFolder, String destZipFile, String prefix, String suffix) throws Exception {
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(destZipFile));
        addFolderToZip(null, srcFolder, zip, prefix, suffix);
        zip.close();
    }

    private static void addFolderToZip(String path, String srcFolder, ZipOutputStream zip, String prefix, String suffix) throws Exception {
        File folder = new File(srcFolder);
        String folderName = folder.getName();
        if (NullStatus.isNotNull(path)) {
            folderName = path + "/" + folderName;
        }
        for (String fileName : folder.list()) {
            addFileToZip(folderName, srcFolder + "/" + fileName, zip, prefix, suffix);
        }
    }

    private static void addFileToZip(String path, String srcFile, ZipOutputStream zip, String prefix, String suffix) throws Exception {
        File folder = new File(srcFile);
        if (folder.isDirectory()) {
            addFolderToZip(path, srcFile, zip, prefix, suffix);
        } else {
            if (isFileNameMatch(folder.getName(), prefix, suffix)) {
                FileInputStream fis = new FileInputStream(srcFile);
                zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
                IOUtils.copy(fis, zip);
                fis.close();
            }
        }
    }

    public static void extractZipPackage(String fileName, String destinationFolder) throws Exception {
        if (NullStatus.isNull(destinationFolder)) {
            destinationFolder = "";
        }
        new File(destinationFolder).mkdirs();
        File inputFile = new File(fileName);
        ZipFile zipFile = new ZipFile(inputFile);
        Enumeration<? extends ZipEntry> oEnum = zipFile.entries();
        while (oEnum.hasMoreElements()) {
            ZipEntry zipEntry = oEnum.nextElement();
            File file = new File(destinationFolder + "/" + zipEntry.getName());
            if (zipEntry.isDirectory()) {
                file.mkdirs();
            } else {
                String destinationFolderName = destinationFolder + "/" + zipEntry.getName();
                destinationFolderName = destinationFolderName.substring(0, destinationFolderName.lastIndexOf("/"));
                new File(destinationFolderName).mkdirs();
                FileOutputStream fos = new FileOutputStream(file);
                IOUtils.copy(zipFile.getInputStream(zipEntry), fos);
                fos.close();
            }
        }
    }

    private static boolean isFileNameMatch(String fileName, String prefix, String suffix) {
        if (NullStatus.isNotNull(suffix)) {
            if (fileName.endsWith(suffix)) {
                if (NullStatus.isNotNull(prefix)) {
                    if (fileName.startsWith(prefix)) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
        } else {
            if (NullStatus.isNotNull(prefix)) {
                if (fileName.startsWith(prefix)) {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }
}
