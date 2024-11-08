package com.frameworkset.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class FileUtil {

    public static final String apppath;

    static {
        URL location = (FileUtil.class).getProtectionDomain().getCodeSource().getLocation();
        File appDir = computeApplicationDir(location, new File("."));
        apppath = appDir.getParentFile().getPath();
    }

    /**
	 * determine the OS name
	 * 
	 * @return The name of the OS
	 */
    public static final String getOS() {
        return System.getProperty("os.name");
    }

    /**
	 * @return True if the OS is a Windows derivate.
	 */
    public static final boolean isWindows() {
        return getOS().startsWith("Windows");
    }

    /**
	 * @return True if the OS is a Linux derivate.
	 */
    public static final boolean isLinux() {
        return getOS().startsWith("Linux");
    }

    private static File computeApplicationDir(URL location, File defaultDir) {
        if (location == null) {
            System.out.println("Warning: Cannot locate the program directory. Assuming default.");
            return defaultDir;
        }
        if (!"file".equalsIgnoreCase(location.getProtocol())) {
            System.out.println("Warning: Unrecognized location type. Assuming default.");
            return new File(".");
        }
        String file = location.getFile();
        if (!file.endsWith(".jar") && !file.endsWith(".zip")) {
            try {
                return (new File(URLDecoder.decode(location.getFile(), "UTF-8"))).getParentFile().getParentFile();
            } catch (UnsupportedEncodingException e) {
            }
            System.out.println("Warning: Unrecognized location type. Assuming default.");
            return new File(location.getFile());
        } else {
            try {
                File path = null;
                if (!isLinux()) {
                    path = new File(URLDecoder.decode(location.toExternalForm().substring(6), "UTF-8")).getParentFile().getParentFile();
                } else {
                    path = new File(URLDecoder.decode(location.toExternalForm().substring(5), "UTF-8")).getParentFile().getParentFile();
                }
                return path;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Warning: Unrecognized location type. Assuming default.");
        return new File(location.getFile());
    }

    public FileUtil() {
    }

    /**
     * Description:��ȡ�ļ������ݣ����䱣����StringBuffer�����з��أ�
     * 
     * @param file
     * @return StringBuffer
     * @throws Exception
     *             StringBuffer
     */
    public static StringBuffer read(String file) throws Exception {
        BufferedReader in = null;
        StringBuffer sb = new StringBuffer();
        String s = null;
        StringBuffer stringbuffer;
        try {
            in = new BufferedReader(new FileReader(file));
            while ((s = in.readLine()) != null) sb.append(s).append('\n');
            stringbuffer = sb;
        } finally {
            if (in != null) in.close();
        }
        return stringbuffer;
    }

    /**
     * Description:��ȡ�����ļ�������
     * 
     * @param propsFile
     * @return Properties
     * @throws Exception
     *             Properties
     */
    public static Properties getProperties(String propsFile) throws Exception {
        return getProperties(propsFile, false);
    }

    /**
     * Description:��ȡ�����ļ������ݣ����Ҹ��addToSystemProps��ֵ�Ƿ�װ��ϵͳ����
     * 
     * @param propsFile
     * @param addToSystemProps
     *            true:װ��ϵͳ���ԣ�false��װ��ϵͳ����
     * @return Properties
     * @throws Exception
     *             Properties
     */
    public static Properties getProperties(String propsFile, boolean addToSystemProps) throws Exception {
        FileInputStream fis = null;
        Properties props = null;
        try {
            fis = new FileInputStream(propsFile);
            props = addToSystemProps ? new Properties(System.getProperties()) : new Properties();
            props.load(fis);
            fis.close();
        } finally {
            if (fis != null) fis.close();
        }
        return props;
    }

    public static File createNewFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) return file;
        File dir = file.getParentFile();
        if (!dir.exists()) dir.mkdirs();
        try {
            file.createNewFile();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    public static File createNewFileOnExist(String filePath) {
        File file = new File(filePath);
        if (file.exists()) file.delete();
        File dir = file.getParentFile();
        if (!dir.exists()) dir.mkdirs();
        try {
            file.createNewFile();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    public static File createNewDirectory(String directorPath) {
        File dir = new File(directorPath);
        if (dir.exists()) return dir;
        dir.mkdirs();
        return dir;
    }

    public static void copy(File sourceFile, String destinction) throws IOException {
        if (!sourceFile.exists()) return;
        File dest_f = new File(destinction);
        if (!dest_f.exists()) dest_f.mkdirs();
        if (sourceFile.isDirectory()) {
            java.io.File[] files = sourceFile.listFiles();
            for (int i = 0; files != null && i < files.length; i++) {
                File temp = files[i];
                if (temp.isDirectory()) {
                    String fileName = temp.getName();
                    copy(temp, destinction + "/" + fileName);
                } else {
                    fileCopy(temp.getAbsolutePath(), destinction + "/" + temp.getName());
                }
            }
        } else {
            File destinctionFile = new File(destinction);
            if (!destinctionFile.exists()) {
                destinctionFile.mkdirs();
            }
            String dest = destinction + "/" + sourceFile.getName();
            fileCopy(sourceFile, dest);
        }
    }

    /**
     * Ŀ¼����,���ڶ�Ŀ¼�������ļ�����Ŀ¼���еݹ鿽��
     * 
     * @param source
     * @param destinction
     *            ����ΪĿ¼
     * @throws IOException
     */
    public static void copy(String source, String destinction) throws IOException {
        File sourceFile = new File(source);
        copy(sourceFile, destinction);
    }

    public static void makeFile(String destinctionFile) {
        File f = new File(destinctionFile);
        File pf = f.getParentFile();
        if (f.exists()) return;
        if (!pf.exists()) {
            pf.mkdirs();
        }
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void fileCopy(String sourcefile, String destinctionFile) throws IOException {
        fileCopy(new File(sourcefile), destinctionFile);
    }

    public static void fileCopy(File sourcefile, String destinctionFile) throws IOException {
        FileInputStream stFileInputStream = null;
        FileOutputStream stFileOutputStream = null;
        try {
            makeFile(destinctionFile);
            stFileInputStream = new FileInputStream(sourcefile);
            stFileOutputStream = new FileOutputStream(destinctionFile);
            int arraySize = 1024;
            byte buffer[] = new byte[arraySize];
            int bytesRead;
            while ((bytesRead = stFileInputStream.read(buffer)) != -1) {
                stFileOutputStream.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (stFileInputStream != null) try {
                stFileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (stFileOutputStream != null) try {
                stFileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * ��ȡ�ļ�������
     * 
     * @param filePath
     *            �ļ�������·��
     * @return
     * @throws IOException
     */
    public static String getFileContent(String filePath, String charSet) throws IOException {
        return getFileContent(new File(filePath), charSet);
    }

    /**
     * ��ȡ�ļ�������
     * 
     * @param filePath
     *            �ļ�������·��
     * @return
     * @throws IOException
     */
    public static String getFileContent(File file, String charSet) throws IOException {
        ByteArrayOutputStream swriter = null;
        OutputStream temp = null;
        InputStream reader = null;
        try {
            reader = new FileInputStream(file);
            swriter = new ByteArrayOutputStream();
            temp = new BufferedOutputStream(swriter);
            int len = 0;
            byte[] buffer = new byte[1024];
            while ((len = reader.read(buffer)) > 0) {
                temp.write(buffer, 0, len);
            }
            temp.flush();
            if (charSet != null && !charSet.equals("")) return swriter.toString(charSet); else return swriter.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "";
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (reader != null) try {
                reader.close();
            } catch (IOException e) {
            }
            if (swriter != null) try {
                swriter.close();
            } catch (IOException e) {
            }
            if (temp != null) try {
                temp.close();
            } catch (IOException e) {
            }
        }
    }

    public static void main(String[] args) {
        try {
            System.out.println(FileUtil.getFileContent("D:\\workspace\\bbossgroup-2.0-RC2\\bboss-mvc/WebRoot/jsp/databind/table.jsp", "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final int EOF = -1;

    public Vector getFileNames(String pathName, String suffix) throws Exception {
        Vector v = new Vector();
        String[] fileNames = null;
        File file = new File(pathName);
        fileNames = file.list();
        if (fileNames == null) throw new Exception();
        for (int i = 0; i < fileNames.length; i++) {
            if (suffix.equals("*") || fileNames[i].toLowerCase().endsWith(suffix.toLowerCase())) v.addElement(fileNames[i]);
        }
        return v;
    }

    /**
     * ɾ���ļ�Ŀ¼�µ��������ļ�����Ŀ¼������һ��ҪС��
     * 
     * @param publishTemppath
     */
    public static void deleteFile(String path) {
        File file = new File(path);
        if (!file.exists() || file.isFile()) return;
        if (file.isDirectory()) deleteSubfiles(file.getAbsolutePath());
        file.delete();
    }

    /**
     * ֻɾ��Ŀ���ļ�
     * 
     * @param path
     *            �ļ����·��
     * @author da.wei200710171007
     */
    public static void deleteFileOnly(String path) {
        File file = new File(path);
        if (file.exists() && file.isFile()) {
            file.delete();
        }
    }

    /**
     * �ƶ��ļ�
     */
    public static void moveFile(String sourceFileName, String destPath) throws Exception {
        File src = new File(sourceFileName);
        if (!src.exists()) {
            throw new Exception("save file[" + sourceFileName + "] to file[" + destPath + "] failed:" + sourceFileName + " not exist.");
        }
        try {
            FileUtil.fileCopy(sourceFileName, destPath);
        } catch (Exception e) {
            System.out.println("save file[" + sourceFileName + "] to file[" + destPath + "]" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * �������ļ���ԭ�����ļ��ᱻɾ��
     * @param source
     * @param dest
     */
    public static void renameFile(String source, String dest) {
        File file = new File(source);
        file.renameTo(new File(dest));
    }

    /**
     * �����ļ���ɾ��ԭ�����ļ�
     * @param source
     * @param dest
     * @throws IOException
     */
    public static void bakFile(String source, String dest) throws IOException {
        File file = new File(source);
        boolean state = file.renameTo(new File(dest));
        if (!state) {
            fileCopy(source, dest);
            deleteFileOnly(source);
        }
    }

    public static void moveSubFiles(String sourceFileName, String destPath) {
        File src = new File(sourceFileName);
        File dest = new File(destPath);
        if (!dest.exists()) dest.mkdirs();
        if (src.isFile()) return; else {
            File[] files = src.listFiles();
            String destFile = null;
            for (int i = 0; files != null && i < files.length; i++) {
                if (files[i].isDirectory()) {
                    String temp_name = files[i].getName();
                    try {
                        moveSubFiles(files[i].getAbsolutePath(), destPath + "/" + temp_name);
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                } else {
                    destFile = destPath + "/" + files[i].getName();
                    try {
                        moveFile(files[i].getAbsolutePath(), destFile);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static List upzip(ZipInputStream zip, String destPath) throws ZipException, IOException {
        List fileNames = new ArrayList();
        ZipEntry azipfile = null;
        while ((azipfile = zip.getNextEntry()) != null) {
            String name = azipfile.getName();
            fileNames.add(name);
            if (!azipfile.isDirectory()) {
                File targetFile = new File(destPath, name);
                targetFile.getParentFile().mkdirs();
                if (targetFile.exists()) {
                    targetFile.delete();
                }
                targetFile.createNewFile();
                BufferedOutputStream diskfile = new BufferedOutputStream(new FileOutputStream(targetFile));
                byte[] buffer = new byte[1024];
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    diskfile.write(buffer, 0, read);
                }
                diskfile.close();
            }
        }
        return fileNames;
    }

    /**
     * ��zip�ļ���ѹ��destPath·������
     * 
     * @param sourceFileName
     * @param destPath
     * @return
     * @throws ZipException
     * @throws IOException
     *             FileUtil.java
     * @author: ge.tao
     */
    public static List unzip(String sourceFileName, String destPath) throws ZipException, IOException {
        if (sourceFileName.endsWith(".zip")) {
            ZipFile zf = new ZipFile(sourceFileName);
            Enumeration en = zf.entries();
            List v = new ArrayList();
            while (en.hasMoreElements()) {
                ZipEntry zipEnt = (ZipEntry) en.nextElement();
                saveEntry(destPath, zipEnt, zf);
                if (!zipEnt.isDirectory()) {
                    v.add(zipEnt.getName());
                }
            }
            zf.close();
            return v;
        } else {
            return null;
        }
    }

    public static void saveEntry(String destPath, ZipEntry target, ZipFile zf) throws ZipException, IOException {
        try {
            File file = new File(destPath + "/" + target.getName());
            if (target.isDirectory()) {
                file.mkdirs();
            } else {
                InputStream is = zf.getInputStream(target);
                BufferedInputStream bis = new BufferedInputStream(is);
                File dir = new File(file.getParent());
                dir.mkdirs();
                FileOutputStream fos = new FileOutputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                int c;
                while ((c = bis.read()) != EOF) {
                    bos.write((byte) c);
                }
                bos.close();
                fos.close();
            }
        } catch (ZipException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        }
    }

    public static boolean createDir(String dirName) {
        File file = new File(dirName);
        if (!file.exists()) return file.mkdir();
        return true;
    }

    public static void createFile(String fileName) throws IOException {
        File file = new File(fileName);
        if (!file.exists()) {
            if (!file.createNewFile()) throw new IOException("Create file fail!");
        }
    }

    public static void writeFile(String fileName, String text) throws IOException {
        FileWriter fw = new FileWriter(fileName, true);
        try {
            fw.write(text, 0, text.length());
        } catch (IOException ioe) {
            throw new IOException("Write text to " + fileName + " fail!");
        } finally {
            fw.close();
        }
    }

    public static void writeFile(String fileName, String text, boolean isAppend) throws IOException {
        FileWriter fw = new FileWriter(fileName, isAppend);
        try {
            fw.write(text, 0, text.length());
        } catch (IOException ioe) {
            throw new IOException("Write text to " + fileName + " fail!");
        } finally {
            fw.close();
        }
    }

    /**
     * ɾ���ļ�Ŀ¼�µ��������ļ�����Ŀ¼������һ��ҪС��
     * 
     * @param publishTemppath
     */
    public static void deleteSubfiles(String publishTemppath) {
        File file = new File(publishTemppath);
        if (!file.exists() || file.isFile()) return;
        File[] files = file.listFiles();
        for (int i = 0; files != null && i < files.length; i++) {
            File temp = files[i];
            if (temp.isDirectory()) {
                deleteSubfiles(temp.getAbsolutePath());
            }
            temp.delete();
        }
    }

    public static String getFileExtByFileName(String fileName) {
        if (fileName == null) return ""; else {
            int idx = fileName.lastIndexOf(".");
            if (idx != -1) return fileName.substring(idx + 1); else return "";
        }
    }

    /**
     * ��ȡ�ļ�������
     * 
     * @param filePath
     *            �ļ�������·��
     * @return
     * @throws IOException
     */
    public static String getFileContent(String filePath) throws IOException {
        Writer swriter = null;
        Reader reader = null;
        try {
            reader = new FileReader(filePath);
            swriter = new StringWriter();
            int len = 0;
            char[] buffer = new char[1024];
            while ((len = reader.read(buffer)) != -1) {
                swriter.write(buffer, 0, len);
            }
            return swriter.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "";
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (reader != null) try {
                reader.close();
            } catch (IOException e) {
            }
            if (swriter != null) try {
                swriter.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * 
     * @param path
     * @return
     */
    public static boolean hasSubDirectory(String path, String uri) {
        File file = null;
        if (uri == null || uri.trim().length() == 0) {
            file = new File(path);
        } else {
            file = new File(path, uri);
        }
        if (!file.exists() || file.isFile()) return false;
        File[] subFiles = file.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                if (pathname.isDirectory()) return true; else return false;
            }
        });
        return subFiles.length > 0;
    }

    /**
     * 
     * @param path
     * @return
     */
    public static boolean hasSubDirectory(String path) {
        return hasSubDirectory(path, null);
    }

    /**
     * 
     * @param path
     * @return
     */
    public static boolean hasSubFiles(String path, String uri) {
        File file = new File(path, uri);
        if (!file.exists() || file.isFile()) return false;
        File[] subFiles = file.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                if (!pathname.isDirectory()) return true; else return false;
            }
        });
        return subFiles.length > 0;
    }

    /**
     * 
     * @param path
     * @return
     */
    public static boolean hasSubFiles(String path) {
        File file = new File(path);
        if (!file.exists() || file.isFile()) return false;
        File[] subFiles = file.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                if (!pathname.isDirectory()) return true; else return false;
            }
        });
        return subFiles.length > 0;
    }

    public static File[] getSubDirectories(String parentpath, String uri) {
        File file = null;
        if (uri == null || uri.trim().length() == 0) {
            file = new File(parentpath);
        } else {
            file = new File(parentpath, uri);
        }
        if (!file.exists() || file.isFile()) return null;
        File[] subFiles = file.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                if (pathname.isDirectory()) return true; else return false;
            }
        });
        return subFiles;
    }

    public static File[] getSubDirectories(String parentpath) {
        return getSubDirectories(parentpath, null);
    }

    /**
     * ��ȡĳ��·���µ������ļ�(�������ļ���)
     */
    public static File[] getSubFiles(String parentpath) {
        return getSubFiles(parentpath, (String) null);
    }

    /**
     * ��ȡĳ��·���µ������ļ�(�������ļ���)
     */
    public static File[] getSubFiles(String parentpath, String uri) {
        File file = null;
        if (uri == null || uri.trim().length() == 0) {
            file = new File(parentpath);
        } else {
            file = new File(parentpath, uri);
        }
        if (!file.exists() || file.isFile()) return null;
        File[] subFiles = file.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                if (pathname.isFile()) return true; else return false;
            }
        });
        return subFiles;
    }

    public static File[] getSubFiles(String parentpath, FileFilter fileFilter) {
        return getSubFiles(parentpath, null, fileFilter);
    }

    public static File[] getSubFiles(String parentpath, String uri, FileFilter fileFilter) {
        File file = null;
        if (uri == null || uri.trim().length() == 0) {
            file = new File(parentpath);
        } else {
            file = new File(parentpath, uri);
        }
        if (!file.exists() || file.isFile()) return null;
        File[] files = null;
        if (fileFilter != null) {
            files = file.listFiles(fileFilter);
        } else {
            files = file.listFiles();
        }
        int rLen = 0;
        for (int i = 0; files != null && i < files.length; i++) {
            if (files[i].isFile()) {
                files[rLen] = files[i];
                rLen++;
            }
        }
        File[] r = new File[rLen];
        System.arraycopy(files, 0, r, 0, rLen);
        return r;
    }

    /**
     * �ο�getSubDirectorieAndFiles(String parentpath,String uri,FileFilter
     * fileFilter)����
     */
    public static File[] getSubDirectorieAndFiles(String parentpath) {
        return getSubDirectorieAndFiles(parentpath, null, null);
    }

    /**
     * �ο�getSubDirectorieAndFiles(String parentpath,String uri,FileFilter
     * fileFilter)����
     */
    public static File[] getSubDirectorieAndFiles(String parentpath, String uri) {
        return getSubDirectorieAndFiles(parentpath, uri, null);
    }

    /**
     * �ο�getSubDirectorieAndFiles(String parentpath,String uri,FileFilter
     * fileFilter)����
     */
    public static File[] getSubDirectorieAndFiles(String parentpath, FileFilter fileFilter) {
        return getSubDirectorieAndFiles(parentpath, null, fileFilter);
    }

    /**
     * ��ȡĳ��·���µ��ļ�
     * 
     * @param parentpath
     *            ���·��
     * @param uri
     *            ����� parentpath�����·��
     * @param fileFilter
     *            ����ĳЩ�ļ�,���Ȩ��������ʹ�ø÷������û�
     * @return
     */
    public static File[] getSubDirectorieAndFiles(String parentpath, String uri, FileFilter fileFilter) {
        File file = null;
        if (uri == null || uri.trim().length() == 0) {
            file = new File(parentpath);
        } else {
            file = new File(parentpath, uri);
        }
        if (!file.exists() || file.isFile()) return null;
        if (fileFilter != null) {
            return file.listFiles(fileFilter);
        } else {
            return file.listFiles();
        }
    }

    public static String getFileContent(File file) {
        Writer swriter = null;
        Reader reader = null;
        try {
            reader = new FileReader(file);
            swriter = new StringWriter();
            int len = 0;
            char[] buffer = new char[1024];
            while ((len = reader.read(buffer)) > 0) {
                swriter.write(buffer, 0, len);
            }
            swriter.flush();
            return swriter.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return "";
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        } finally {
            if (reader != null) try {
                reader.close();
            } catch (IOException e) {
            }
            if (swriter != null) try {
                swriter.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * ���������ж�ȡ�ֽ�����
     * 
     * @param in
     * @return
     * @throws IOException
     */
    public static byte[] readFully(InputStream in) throws IOException {
        if (in instanceof ByteArrayInputStream) {
            int size = in.available();
            byte[] bytes = new byte[size];
            int offset = 0;
            int numRead = 0;
            while (offset < size) {
                numRead = in.read(bytes, offset, size - offset);
                if (numRead >= 0) {
                    offset += numRead;
                } else {
                    break;
                }
            }
            return bytes;
        }
        byte[] xfer = new byte[2048];
        ByteArrayOutputStream out = new ByteArrayOutputStream(xfer.length);
        for (int bytesRead = in.read(xfer, 0, xfer.length); bytesRead >= 0; bytesRead = in.read(xfer, 0, xfer.length)) {
            if (bytesRead > 0) {
                out.write(xfer, 0, bytesRead);
            }
        }
        in.close();
        out.close();
        return out.toByteArray();
    }
}
