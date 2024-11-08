package com.ideo.sweetforge.plugin.merge.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;

/**
 * Use to read, write , serach files or directory
 * 
 */
public class Util {

    public static final String FILTER_ALLFILE = ".*";

    /**
	 * Use to move a file from a diretory to another
	 * @param pathExport path of the move file
	 * @throws IOException
	 */
    public static void moveFile(File file, String pathExport) throws IOException {
        File dir = new File(pathExport);
        file.renameTo(new File(dir, file.getName()));
    }

    /**
	 * Use to zip a directory  
	 * @param pathExport path where the directory will be zip
	 * @throws IOException
	 */
    public static void zipDirectory(File directory, String nameZip, String pathExport, boolean pathWithFirstDirectory) throws IOException {
        FileOutputStream f = new FileOutputStream(pathExport + File.separator + nameZip);
        CheckedOutputStream csum = new CheckedOutputStream(f, new Adler32());
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(csum));
        Collection<File> listeFiles = getListeFile(directory, FILTER_ALLFILE, true, false);
        Iterator<File> itListeFiles = listeFiles.iterator();
        while (itListeFiles.hasNext()) {
            File fileToZip = (File) itListeFiles.next();
            FileInputStream in = new FileInputStream(fileToZip);
            out.putNextEntry(new ZipEntry(getZipEntryName(directory, fileToZip, pathWithFirstDirectory)));
            int c;
            while ((c = in.read()) != -1) {
                out.write(c);
            }
            in.close();
        }
        out.close();
    }

    protected static String getZipEntryName(File directory, File file, boolean pathWithFirstDirectory) throws IOException {
        String ret = file.getPath();
        String sourcePath = directory.getPath();
        int ind = ret.indexOf(sourcePath);
        if (ind >= 0) {
            if (pathWithFirstDirectory) {
                sourcePath = sourcePath.replaceAll(directory.getName(), "");
            }
            ret = ret.substring(sourcePath.length());
        } else {
            throw new IOException("Erreur: getZipEntryName -- ???");
        }
        StringBuffer tmpBuf = new StringBuffer();
        char tmpChar;
        for (int i = 0; i < ret.length(); i++) {
            tmpChar = ret.charAt(i);
            if (tmpChar == '\\') {
                tmpBuf.append('/');
            } else {
                tmpBuf.append(tmpChar);
            }
        }
        if (tmpBuf.charAt(0) == '/') {
            return tmpBuf.toString().substring(1);
        } else {
            return tmpBuf.toString();
        }
    }

    /**
	 * Use to zip a file  
	 * @param pathExport path where the file will be zip
	 * @throws IOException
	 */
    public static void zipFile(File file, String nameZip, String pathExport) throws IOException {
        FileOutputStream f = new FileOutputStream(pathExport + File.separator + nameZip);
        CheckedOutputStream csum = new CheckedOutputStream(f, new Adler32());
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(csum));
        BufferedReader in = new BufferedReader(new FileReader(file));
        out.putNextEntry(new ZipEntry(file.getAbsolutePath()));
        int c;
        while ((c = in.read()) != -1) {
            out.write(c);
        }
        in.close();
        out.close();
    }

    /**
	 * Return the list of the directories in the right delete order
	 * @param extensionExpression Extension of the search files
	 * @param subDirectory       true if sub directory must be also scan          
	 * @throws IOException
	 */
    public static Collection<File> getListeDirectory(File directory, boolean findSubDirectory) throws IOException {
        Collection<File> allDirectory = new ArrayList<File>();
        FileFilter directoryFilter = new FileFilter() {

            public boolean accept(File file) {
                return file.isDirectory();
            }
        };
        File[] listeDirectory = directory.listFiles(directoryFilter);
        for (int j = 0; j < listeDirectory.length; j++) {
            File subDirectory = listeDirectory[j];
            if (findSubDirectory) {
                allDirectory.addAll(getListeDirectory(subDirectory, findSubDirectory));
            }
            allDirectory.add(subDirectory);
        }
        return allDirectory;
    }

    /**
	 * Return the list of the files
	 * @param expressionSearch     search pattern
	 * @param subDirectory         true if sub directory must be also scan   
	 * @param restrictifExpression Define if search pattern must match with all the name or not (match or find)          
	 * @throws IOException
	 */
    public static Collection<File> getListeFile(File directory, String expressionSearch, boolean parseSubDirectory, boolean restrictifExpression) throws IOException {
        Collection<File> listeFiles = new ArrayList<File>();
        String[] listePathFile = null;
        if (parseSubDirectory) {
            FileFilter directoryFilter = new FileFilter() {

                public boolean accept(File file) {
                    return file.isDirectory();
                }
            };
            File[] listeDirectory = directory.listFiles(directoryFilter);
            for (int j = 0; j < listeDirectory.length; j++) {
                File subDirectory = listeDirectory[j];
                listeFiles.addAll(getListeFile(subDirectory, expressionSearch, true, restrictifExpression));
            }
        }
        listePathFile = directory.list(new FileFilterName(expressionSearch));
        if (listePathFile != null) {
            for (int i = 0; i < listePathFile.length; i++) {
                File file = new File(directory.getAbsolutePath() + File.separator + listePathFile[i]);
                if (!file.isDirectory()) {
                    listeFiles.add(file);
                }
            }
        }
        return listeFiles;
    }

    /**
	 * Use to write content in a file
	 * @param file writed file
	 * @param content value add to the file
	 * @param append  true if the value is append         
	 * @throws IOException
	 */
    public static void writeToFile(File file, String content, boolean append) throws IOException {
        FileWriter out = new FileWriter(file, append);
        out.write(content);
        out.close();
    }

    /**
	 * Use to get the content of a InputStream
	 * @param stream stream to read
	 * @return
	 * @throws IOException
	 */
    public static String getFileContent(InputStream stream) throws IOException {
        InputStreamReader in = null;
        StringBuffer buf = null;
        try {
            buf = new StringBuffer();
            in = new InputStreamReader(stream);
            int c;
            while ((c = in.read()) != -1) {
                buf.append((char) c);
            }
        } finally {
            in.close();
        }
        return buf.toString();
    }

    /**
	 * Use to copy file to another directory
	 * @param file file to copy
	 * @param pathExport directory path to copy the file
	 * @param fileExportName name of the copy file
	 * @throws IOException
	 */
    public static void copyFile(File file, String pathExport, String fileExportName) throws IOException {
        copyFile(file, pathExport + "/" + fileExportName);
    }

    /**
	 * Use to copy file to another directory
	 * @param file file to copy
	 * @param pathExport directory path to copy the file
	 * @throws IOException
	 */
    public static void copyFile(File file, String pathExport) throws IOException {
        File out = new File(pathExport);
        FileChannel sourceChannel = new FileInputStream(file).getChannel();
        FileChannel destinationChannel = new FileOutputStream(out).getChannel();
        sourceChannel.transferTo(0, sourceChannel.size(), destinationChannel);
        sourceChannel.close();
        destinationChannel.close();
    }

    /**
	 * Use to copy directory to another directory
	 * @param directory  directory to copy
	 * @param pathExport directory path to copy the directory
	 * @param copySubDirectory true if sub-directory must be copy
	 * @param createdirectory true if root directory must be copy  
	 * @return true is process is successful
	 * @throws IOException
	 */
    public static boolean copyDirectory(File directory, String pathExport, boolean copySubDirectory, boolean createdirectory) throws IOException {
        boolean succes = true;
        if (directory.exists() && directory.isDirectory()) {
            if (createdirectory) {
                new File(pathExport + "/" + directory.getName()).mkdirs();
            } else {
                new File(pathExport).mkdirs();
            }
            Collection<File> listeFiles = getListeFile(directory, FILTER_ALLFILE, false, false);
            Iterator<File> it = listeFiles.iterator();
            while (it.hasNext()) {
                File file = (File) it.next();
                if (createdirectory) {
                    copyFile(file, pathExport + "/" + directory.getName(), file.getName());
                } else {
                    copyFile(file, pathExport, file.getName());
                }
            }
            if (copySubDirectory) {
                Collection<File> listeDirectory = getListeDirectory(directory, false);
                it = listeDirectory.iterator();
                while (it.hasNext()) {
                    File subdirectory = (File) it.next();
                    boolean opSucces = false;
                    if (createdirectory) {
                        opSucces = copyDirectory(subdirectory, pathExport + "/" + directory.getName(), copySubDirectory, true);
                    } else {
                        opSucces = copyDirectory(subdirectory, pathExport, copySubDirectory, true);
                    }
                    if (!opSucces) {
                        succes = false;
                    }
                }
            }
            return succes;
        } else {
            return false;
        }
    }

    /**
	 * Use to read File content
	 * @param file file to read
	 * @return true is process is successful
	 * @throws IOException
	 */
    public static String getFileContent(File file) throws IOException {
        FileReader in = null;
        StringBuffer buf = null;
        try {
            buf = new StringBuffer();
            in = new FileReader(file);
            int c;
            while ((c = in.read()) != -1) {
                buf.append((char) c);
            }
        } finally {
            in.close();
        }
        return buf.toString();
    }

    /**
	 * Use to delete a directory
	 * @param directory directory to delete
	 * @return true is process is successful
	 * @throws IOException
	 */
    public static boolean deleteDirectory(File directory) throws IOException {
        boolean operationSucces = true;
        if (directory.exists() && directory.isDirectory()) {
            Collection<File> listeFiles = getListeFile(directory, FILTER_ALLFILE, true, false);
            Iterator<File> it = listeFiles.iterator();
            while (it.hasNext()) {
                File file = (File) it.next();
                if (!file.delete()) {
                    operationSucces = false;
                }
            }
            Collection<File> listeDirectory = getListeDirectory(directory, true);
            it = listeDirectory.iterator();
            while (it.hasNext()) {
                File file = (File) it.next();
                if (!file.delete()) {
                    operationSucces = false;
                }
            }
            if (!directory.delete()) {
                operationSucces = false;
            }
        }
        return operationSucces;
    }

    public static String cleanString(String value) {
        if (value != null) {
            value = value.replaceAll("\n", "");
            value = value.replaceAll("\r", "");
            value = value.replaceAll("\t", "");
            value = value.trim();
        }
        return value;
    }

    public static void unzipArchive(File archive, File outputDir) throws IOException {
        ZipFile zipfile = new ZipFile(archive);
        for (Enumeration e = zipfile.entries(); e.hasMoreElements(); ) {
            ZipEntry entry = (ZipEntry) e.nextElement();
            unzipEntry(zipfile, entry, outputDir);
        }
    }

    private static void unzipEntry(ZipFile zipfile, ZipEntry entry, File outputDir) throws IOException {
        if (entry.isDirectory()) {
            createDir(new File(outputDir, entry.getName()));
            return;
        }
        File outputFile = new File(outputDir, entry.getName());
        if (!outputFile.getParentFile().exists()) {
            createDir(outputFile.getParentFile());
        }
        BufferedInputStream inputStream = new BufferedInputStream(zipfile.getInputStream(entry));
        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
        try {
            IOUtils.copy(inputStream, outputStream);
        } finally {
            outputStream.close();
            inputStream.close();
        }
    }

    private static void createDir(File dir) throws IOException {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Can not create dir " + dir);
        }
    }
}
