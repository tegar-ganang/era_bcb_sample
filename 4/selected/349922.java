package com.jspx.utils;

import com.jspx.io.file.MultiFile;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.channels.Channels;
import java.nio.ByteBuffer;

/**
 * Created by IntelliJ IDEA.
 * User: other
 * Date: 2004-4-1
 * Time: 11:30:53
 * To change this template use | Settings | File Templates.
 */
public final class FileUtil {

    public static final int BUFFER_SIZE = 1024 * 4;

    private FileUtil() {
    }

    public static boolean copy(String inputFilename, String outputFilename) {
        return copy(new File(inputFilename), new File(outputFilename));
    }

    /**
     * 拷贝文件
     * @param input in file
     * @param output out file
     * @return
     */
    public static boolean copy(File input, File output) {
        MultiFile multiFile = new MultiFile();
        if (input.isDirectory()) {
            if (!FileUtil.makeDirectory(output)) return false;
            if (!multiFile.copyDirectory(input, output)) return false;
        } else {
            if (!multiFile.copyFile(input, output)) return false;
        }
        return true;
    }

    public static String readTextFile(File f) throws IOException {
        StringBuffer buf = new StringBuffer();
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            buf.append(inputLine);
            buf.append('\n');
        }
        in.close();
        return buf.toString();
    }

    /**
     * 修改目录或文件的名称
     *
     * @param oldDirOrFile old dir
     * @param newDirOrFile new dir
     * @return boolean
     */
    public static boolean renameTo(String oldDirOrFile, String newDirOrFile) {
        File file = new File(oldDirOrFile);
        return file.renameTo(new File(newDirOrFile));
    }

    /**
     * return true if the directory contains files with the extension
     */
    public static boolean dirContainsFiles(File dir, String extension, boolean recursive) {
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(extension)) {
                return true;
            }
            if (recursive && file.isDirectory()) {
                return FileUtil.dirContainsFiles(file, extension, recursive);
            }
        }
        return false;
    }

    public static FileInputStream OpenInputStream(String name) throws IOException {
        File file = new File(name);
        if (!file.isFile()) {
            throw new IOException(name + " is not a file.");
        }
        if (!file.canRead()) {
            throw new IOException(name + " is not readable.");
        }
        return (new FileInputStream(file));
    }

    public static synchronized FileOutputStream OpenOutputStream(String name) throws IOException {
        File file = new File(name);
        if (file.isDirectory()) {
            throw new IOException(name + " is a directory.");
        }
        if (file.exists()) file.delete();
        return (new FileOutputStream(file));
    }

    /**
     * Copy the contents of the given Reader to the given Writer.
     * Closes both when done.
     *
     * @param in  the Reader to copy from
     * @param out the Writer to copy to
     * @return the number of characters copied
     * @throws IOException in case of I/O errors
     */
    public static int copy(Reader in, Writer out) throws IOException {
        try {
            int byteCount = 0;
            char[] buffer = new char[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                byteCount += bytesRead;
            }
            out.flush();
            return byteCount;
        } finally {
            try {
                in.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            try {
                out.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 从文件路径得到文件名。
     *
     * @param source 文件的路径，可以是相对路径也可以是绝对路径
     * @return 对应的文件名
     * @since 0.4
     */
    public static String getFileName(String source) {
        if (StringUtil.isNULL(source)) return "";
        source = mendFile(source);
        int len = source.lastIndexOf("/");
        if (len < 0) return source;
        return source.substring(len + 1);
    }

    /**
     * 得到最后文件的修改时间
     *
     * @param fileName
     * @return long
     */
    public static long lastModified(String fileName) {
        File file = new File(fileName);
        if (!file.isFile()) return 0;
        return file.lastModified();
    }

    /**
     * 从文件名得到文件绝对路径。
     *
     * @param fileName 文件名
     * @return 对应的文件路径
     * @since 0.4
     */
    public static String getFilePath(String fileName) {
        File file = new File(fileName);
        return file.getAbsolutePath();
    }

    /**
     * 得到文件的类型。
     * 实际上就是得到文件名中最后一个“.”后面的部分。
     *
     * @param fileName 文件名
     * @return 文件名中的类型部分
     * @since 0.5
     */
    public static String getTypePart(String fileName) {
        if (StringUtil.isNULL(fileName)) return "";
        int point = fileName.lastIndexOf(".");
        if (point != -1) {
            return fileName.substring(point + 1, fileName.length());
        }
        return "";
    }

    /**
     * 得到文件的类型。
     * 实际上就是得到文件名中最后一个“.”后面的部分。
     *
     * @param file 文件
     * @return 文件名中的类型部分
     * @since 0.5
     */
    public static String getFileType(File file) {
        return getTypePart(file.getName());
    }

    public static String getNamePart(String fileName) {
        String result = getFileNamePart(fileName);
        if (result == null) return result;
        int point = result.indexOf(".");
        if (point != -1 && result.length() >= point) {
            return result.substring(0, point);
        }
        return result;
    }

    /**
     * 得到文件的名字部分。
     * 实际上就是路径中的最后一个路径分隔符后的部分。
     *
     * @param fileName 文件名
     * @return 文件名中的名字部分
     * @since 0.5
     */
    public static String getFileNamePart(String fileName) {
        int point = getPathLastIndex(fileName);
        int length = fileName.length();
        if (point == -1) {
            return fileName;
        } else if (point == length - 1) {
            int secondPoint = getPathLastIndex(fileName, point - 1);
            if (secondPoint == -1) {
                if (length == 1) {
                    return fileName;
                } else {
                    return fileName.substring(0, point);
                }
            } else {
                return fileName.substring(secondPoint + 1, point);
            }
        } else {
            return fileName.substring(point + 1);
        }
    }

    /**
     * 得到文件名中的父路径部分。
     * 对两种路径分隔符都有效。
     * 不存在时返回""。
     * 如果文件名是以路径分隔符结尾的则不考虑该分隔符，例如"/path/"返回""。
     *
     * @param fileName 文件名
     * @return 父路径，不存在或者已经是父目录时返回""
     * @since 0.5
     */
    public static String getPathPart(String fileName) {
        if (StringUtil.isNULL(fileName)) return "";
        int point = getPathLastIndex(fileName);
        int length = fileName.length();
        if (point == -1) {
            return "";
        } else if (point == length - 1) {
            int secondPoint = getPathLastIndex(fileName, point - 1);
            if (secondPoint == -1) {
                return "";
            } else {
                return mendPath(fileName.substring(0, secondPoint));
            }
        } else {
            return mendPath(fileName.substring(0, point));
        }
    }

    /**
     * 得到路径分隔符在文件路径中首次出现的位置。
     * 对于DOS或者UNIX风格的分隔符都可以。
     *
     * @param fileName 文件路径
     * @return 路径分隔符在路径中首次出现的位置，没有出现时返回-1。
     * @since 0.5
     */
    public static int getPathIndex(String fileName) {
        if (StringUtil.isNULL(fileName)) return -1;
        int point = fileName.indexOf('/');
        if (point == -1) {
            point = fileName.indexOf('\\');
        }
        return point;
    }

    /**
     * 得到路径分隔符在文件路径中指定位置后首次出现的位置。
     * 对于DOS或者UNIX风格的分隔符都可以。
     *
     * @param fileName  文件路径
     * @param fromIndex 开始查找的位置
     * @return 路径分隔符在路径中指定位置后首次出现的位置，没有出现时返回-1。
     * @since 0.5
     */
    public static int getPathIndex(String fileName, int fromIndex) {
        if (StringUtil.isNULL(fileName)) return -1;
        int point = fileName.indexOf('/', fromIndex);
        if (point == -1) {
            point = fileName.indexOf('\\', fromIndex);
        }
        return point;
    }

    /**
     * 得到路径分隔符在文件路径中最后出现的位置。
     * 对于DOS或者UNIX风格的分隔符都可以。
     *
     * @param fileName 文件路径
     * @return 路径分隔符在路径中最后出现的位置，没有出现时返回-1。
     * @since 0.5
     */
    public static int getPathLastIndex(String fileName) {
        if (StringUtil.isNULL(fileName)) return -1;
        int point = fileName.lastIndexOf('/');
        if (point == -1) {
            point = fileName.lastIndexOf('\\');
        }
        return point;
    }

    /**
     * 得到路径分隔符在文件路径中指定位置前最后出现的位置。
     * 对于DOS或者UNIX风格的分隔符都可以。
     *
     * @param fileName  文件路径
     * @param fromIndex 开始查找的位置
     * @return 路径分隔符在路径中指定位置前最后出现的位置，没有出现时返回-1。
     * @since 0.5
     */
    public static int getPathLastIndex(String fileName, int fromIndex) {
        if (StringUtil.isNULL(fileName)) return -1;
        int point = fileName.lastIndexOf('/', fromIndex);
        if (point == -1) {
            point = fileName.lastIndexOf('\\', fromIndex);
        }
        return point;
    }

    /**
     * 将文件名中的类型部分去掉。
     *
     * @param fileName 文件名
     * @return 去掉类型部分的结果
     * @since 0.5
     */
    public static String trimType(String fileName) {
        if (StringUtil.isNULL(fileName)) return "";
        int index = fileName.lastIndexOf(".");
        if (index != -1) {
            return fileName.substring(0, index);
        } else {
            return fileName;
        }
    }

    /**
     * 得到相对路径。
     * 文件名不是目录名的子节点时返回文件名。
     *
     * @param pathName 目录名
     * @param fileName 文件名
     * @return 得到文件名相对于目录名的相对路径，目录下不存在该文件时返回文件名
     * @since 0.5
     */
    public static String getSubpath(String pathName, String fileName) {
        if (StringUtil.isNULL(fileName)) return "";
        int index = fileName.indexOf(pathName);
        if (index != -1) {
            return fileName.substring(index + pathName.length() + 1);
        } else {
            return fileName;
        }
    }

    /**
     * 删除文件
     *
     * @param fileName file name
     * @return int
     */
    public static int delete(String fileName) {
        if (fileName == null || fileName.equals("") || fileName.equalsIgnoreCase("null")) return 0;
        int result = 0;
        File f = new File(fileName);
        if (!f.exists()) {
            return 0;
        } else if (!f.canWrite()) {
            result = -1;
        }
        if (f.isDirectory()) {
            String[] files = f.list();
            if (files.length > 0) {
                result = -1;
            }
        }
        boolean success = f.delete();
        if (!success) {
            result = -1;
        }
        return result;
    }

    /**
     * 删除指定目录及其中的所有内容。
     *
     * @param dir 要删除的目录
     * @return 删除成功时返回true，否则返回false。
     * @since 0.1
     */
    public static boolean deleteDirectory(File dir) {
        if ((dir == null) || !dir.isDirectory()) {
            throw new IllegalArgumentException("Argument " + dir + " is not a directory. ");
        }
        File[] entries = dir.listFiles();
        int sz = entries.length;
        for (int i = 0; i < sz; i++) {
            if (entries[i].isDirectory()) {
                deleteDirectory(entries[i]);
            } else {
                if (entries[i].canWrite()) {
                    if (!entries[i].delete()) {
                        entries[i].deleteOnExit();
                    }
                } else {
                    entries[i].deleteOnExit();
                }
            }
        }
        return dir.delete();
    }

    /**
     * 判断指定的文件是否存在。
     *
     * @param fileName 要判断的文件的文件名
     * @return 存在时返回true，否则返回false。
     * @since 0.1
     */
    public static boolean isDirectory(String fileName) {
        return !StringUtil.isNULL(fileName) && new File(fileName).isDirectory();
    }

    public static long getLastModified(String fileName) {
        if (StringUtil.isNULL(fileName)) return 0;
        File file = new File(fileName);
        if (file.exists()) return file.lastModified();
        return 0;
    }

    /**
     * 判断指定的文件是否存在。
     *
     * @param fileName 要判断的文件的文件名
     * @return 存在时返回true，否则返回false。
     * @since 0.1
     */
    public static boolean isFileExist(String fileName) {
        if (StringUtil.isNULL(fileName)) return false;
        int i = fileName.indexOf(".jar!");
        if (i == -1) {
            File file = new File(fileName);
            return file.isFile() && file.exists();
        }
        fileName = mendFile(fileName);
        i = fileName.indexOf(".jar!");
        String jarFileName = fileName.substring(0, i + 4);
        String entryName = fileName.substring(jarFileName.length() + 2, fileName.length());
        File file = new File(jarFileName);
        if (!file.isFile()) return false;
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(jarFileName);
            Enumeration<JarEntry> fileEnum = jarFile.entries();
            while (fileEnum.hasMoreElements()) {
                JarEntry entry = fileEnum.nextElement();
                if (entryName.equalsIgnoreCase(entry.getName())) return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (jarFile != null) try {
                jarFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 创建指定的目录。
     * 如果指定的目录的父目录不存在则创建其目录书上所有需要的父目录。
     * <b>注意：可能会在返回false的时候创建部分父目录。</b>
     *
     * @param file 要创建的目录
     * @return 完全创建成功时返回true，否则返回false。
     * @since 0.2
     */
    public static boolean makeDirectory(File file) {
        return file.exists() || file.mkdirs();
    }

    /**
     * 创建指定的目录。
     * 如果指定的目录的父目录不存在则创建其目录书上所有需要的父目录。
     * <b>注意：可能会在返回false的时候创建部分父目录。</b>
     *
     * @param fileName 要创建的目录的目录名
     * @return 完全创建成功时返回true，否则返回false。
     * @since 0.1
     */
    public static boolean makeDirectory(String fileName) {
        File file = new File(fileName);
        return makeDirectory(file);
    }

    /**
     * 清空指定目录中的文件。
     * 这个方法将尽可能删除所有的文件，但是只要有一个文件没有被删除都会返回false。
     * 另外这个方法不会迭代删除，即不会删除子目录及其内容。
     *
     * @param directory 要清空的目录
     * @return 目录下的所有文件都被成功删除时返回true，否则返回false.
     * @since 0.1
     */
    public static boolean emptyDirectory(File directory) {
        File[] entries = directory.listFiles();
        for (File entry : entries) {
            if (!entry.delete()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 清空指定目录中的文件。
     * 这个方法将尽可能删除所有的文件，但是只要有一个文件没有被删除都会返回false。
     * 另外这个方法不会迭代删除，即不会删除子目录及其内容。
     *
     * @param directoryName 要清空的目录的目录名
     * @return 目录下的所有文件都被成功删除时返回true，否则返回false。
     * @since 0.1
     */
    public static boolean emptyDirectory(String directoryName) {
        File dir = new File(directoryName);
        return emptyDirectory(dir);
    }

    /**
     * 删除指定目录及其中的所有内容。
     *
     * @param dirName 要删除的目录的目录名
     * @return 删除成功时返回true，否则返回false。
     * @since 0.1
     */
    public static boolean deleteDirectory(String dirName) {
        return deleteDirectory(new File(dirName));
    }

    /**
     * 判断文件是否可写
     *
     * @param filename 要判断文件名
     * @return 返回true，否则返回false。
     * @since 0.1
     */
    public static boolean canWrite(String filename) {
        if (StringUtil.isNULL(filename)) return true;
        File file = new File(filename);
        return !file.exists() || file.exists() && file.isFile() && file.canWrite();
    }

    /**
     * 判断文件是否可读
     *
     * @param filename 要判断文件名
     * @return 返回true，否则返回false。
     * @since 0.1
     */
    public static boolean canRead(String filename) {
        if (StringUtil.isNULL(filename)) return false;
        File file = new File(filename);
        return file.exists() && file.isFile() && file.canRead();
    }

    public static String getParentPath(String filename) {
        File f = new File(filename);
        if (f.exists()) {
            return f.getParent();
        }
        return "";
    }

    /**
     * 修复路径 让路径后边都有一个  /
     *
     * @param spath 需要修复的路径
     * @return 修复后的路径
     * @since 0.2
     */
    public static String mendPath(String spath) {
        if (spath == null || spath.length() < 1) return "";
        String result = mendFile(spath);
        if (!result.endsWith("/")) {
            result += "/";
        }
        return result;
    }

    public static String toURLFile(String spath) {
        if (spath == null || spath.length() < 1) return "";
        String result = StringUtil.replace(spath, "/", "\\");
        if (result.startsWith("file:\\\\")) return result;
        return "file:\\\\" + result;
    }

    /**
     * 修复路径 是文件的/
     *
     * @param fileName 需要修复的路径
     * @return 修复后的路径
     * @since 0.2
     */
    public static String mendFile(String fileName) {
        if (fileName == null || fileName.length() < 1) return "";
        String result = StringUtil.replace(fileName, "\\", "/");
        if (SystemUtil.WINDOWS.equalsIgnoreCase(SystemUtil.getOS())) {
            if (result.startsWith("file://")) {
                result = result.substring(7, result.length());
            } else if (result.startsWith("file:/")) result = result.substring(6, result.length());
        } else {
            if (result.startsWith("file:")) result = result.substring(5, result.length());
        }
        return StringUtil.replace(result, "//", "/");
    }

    public static byte[] readFileByte(String filename) {
        if (filename == null) return null;
        try {
            FileInputStream fis = new FileInputStream(filename);
            FileChannel fileC = fis.getChannel();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            WritableByteChannel outC = Channels.newChannel(baos);
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
            while (true) {
                int i = fileC.read(buffer);
                if (i == 0 || i == -1) {
                    break;
                }
                buffer.flip();
                outC.write(buffer);
                buffer.clear();
            }
            fis.close();
            return baos.toByteArray();
        } catch (IOException fnfe) {
            fnfe.printStackTrace();
        }
        return null;
    }

    public static boolean writeFile(String filename, byte data[]) {
        try {
            if (filename == null) return false;
            File fileout = new File(filename);
            if (!fileout.exists()) {
                fileout.createNewFile();
            }
            if (fileout.canWrite()) {
                FileOutputStream out = new FileOutputStream(filename, false);
                out.write(data);
                out.flush();
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static String getFileListDateSort(String folder, String fen) {
        if (StringUtil.isNULL(folder)) return "";
        List<FileInfo> filelist = new ArrayList<FileInfo>();
        File dir = new File(folder);
        if (!dir.isDirectory()) return "";
        File fileList[] = dir.listFiles();
        for (File aFileList : fileList) {
            if (aFileList.isFile()) {
                FileInfo fileInfo = new FileInfo();
                fileInfo.setName(aFileList.getName());
                fileInfo.setIsDir(0);
                fileInfo.setSize(aFileList.length());
                fileInfo.setDate(new Date(aFileList.lastModified()));
                filelist.add(fileInfo);
            }
        }
        FileInfo mfile1, mfile2, mfile;
        for (int i = 0; i < filelist.size() - 1; i++) {
            for (int j = i + 1; j < filelist.size(); j++) {
                mfile1 = filelist.get(i);
                mfile2 = filelist.get(j);
                if (mfile1.getDate().compareTo(mfile2.getDate()) < 0) {
                    mfile = filelist.get(j);
                    filelist.set(j, filelist.get(i));
                    filelist.set(i, mfile);
                }
            }
        }
        StringBuffer restul = new StringBuffer();
        for (FileInfo tfile : filelist) {
            restul.append(tfile.getName()).append(fen);
        }
        filelist.clear();
        return restul.toString();
    }

    public static List<FileInfo> getFileListSort(String folder, String find, String order) {
        List<FileInfo> filelist = new ArrayList<FileInfo>();
        if (StringUtil.isNULL(folder)) return filelist;
        File dir = new File(folder);
        if (!dir.exists()) return filelist;
        File fileList[] = dir.listFiles();
        if (fileList != null) {
            for (File aFileList : fileList) {
                if (StringUtil.isNULL(find)) {
                    FileInfo fileInfo = new FileInfo();
                    if (aFileList.isFile()) fileInfo.setIsDir(0);
                    if (aFileList.isDirectory()) {
                        fileInfo.setIsDir(1);
                        fileInfo.setType("folder");
                    } else {
                        fileInfo.setType(getTypePart(aFileList.getName()));
                    }
                    if (aFileList.isHidden()) fileInfo.setIsDir(-1);
                    fileInfo.setName(aFileList.getName());
                    fileInfo.setIsDir(0);
                    fileInfo.setSize(aFileList.length());
                    fileInfo.setDate(new Date(aFileList.lastModified()));
                    fileInfo.setAbsolutePath(aFileList.getAbsolutePath());
                    filelist.add(fileInfo);
                } else if (aFileList.getName().indexOf(find) > -1) {
                    FileInfo fileInfo = new FileInfo();
                    if (aFileList.isFile()) fileInfo.setIsDir(0);
                    if (aFileList.isDirectory()) {
                        fileInfo.setIsDir(1);
                        fileInfo.setType("folder");
                    } else {
                        fileInfo.setType(getTypePart(aFileList.getName()));
                    }
                    if (aFileList.isHidden()) fileInfo.setIsDir(-1);
                    fileInfo.setName(aFileList.getName());
                    fileInfo.setIsDir(0);
                    fileInfo.setSize(aFileList.length());
                    fileInfo.setDate(new Date(aFileList.lastModified()));
                    fileInfo.setAbsolutePath(aFileList.getAbsolutePath());
                    filelist.add(fileInfo);
                }
            }
        }
        FileInfo mfile1, mfile2, mfile;
        for (int i = 0; i < filelist.size() - 1; i++) {
            for (int j = i + 1; j < filelist.size(); j++) {
                mfile1 = filelist.get(i);
                mfile2 = filelist.get(j);
                if (order == null) {
                    if (mfile1.getDate().compareTo(mfile2.getDate()) < 0) {
                        mfile = filelist.get(j);
                        filelist.set(j, filelist.get(i));
                        filelist.set(i, mfile);
                    }
                } else if (order.equalsIgnoreCase("name")) {
                    if (mfile1.getName().compareTo(mfile2.getName()) > 0) {
                        mfile = filelist.get(j);
                        filelist.set(j, filelist.get(i));
                        filelist.set(i, mfile);
                    }
                } else if (order.equalsIgnoreCase("size")) {
                    if (mfile1.getSize() > mfile2.getSize()) {
                        mfile = filelist.get(j);
                        filelist.set(j, filelist.get(i));
                        filelist.set(i, mfile);
                    }
                } else {
                    if (mfile1.getDate().compareTo(mfile2.getDate()) < 0) {
                        mfile = filelist.get(j);
                        filelist.set(j, filelist.get(i));
                        filelist.set(i, mfile);
                    }
                }
            }
        }
        return filelist;
    }

    /**
     * 得到指定文件类型的列表
     *
     * @param folder
     * @param fen
     * @param type
     * @param chid
     * @return String
     */
    public static String getFileList(String folder, String fen, String type, boolean chid) {
        String file = "";
        File dir = new File(folder);
        if (!dir.exists()) return "";
        File fileList[] = dir.listFiles();
        int I;
        if (fileList != null) {
            for (I = 0; I < fileList.length; I++) {
                if (fileList[I].isFile()) {
                    if (type == null || type.equalsIgnoreCase("*")) {
                        file = file + fen + fileList[I].toString();
                    } else if (type.toLowerCase().indexOf(getTypePart(fileList[I].toString().toLowerCase())) != -1) {
                        file = file + fen + fileList[I].toString();
                    }
                } else if (fileList[I].isDirectory()) {
                    if (chid) {
                        file += getFileList(fileList[I].toString(), fen, type, chid);
                    }
                }
            }
        }
        return file;
    }

    /**
     * 得到文件名列表,只有名字
     *
     * @param folder
     * @param fen
     * @param chid
     * @return String
     */
    public static String getFileNameList(String folder, String fen, boolean chid) {
        String file = "";
        File dir = new File(folder);
        if (!dir.exists()) return "";
        File fileList[] = dir.listFiles();
        int I;
        if (fileList != null) {
            for (I = 0; I < fileList.length; I++) {
                if (fileList[I].isFile()) {
                    file = file + fen + fileList[I].getName();
                } else if (fileList[I].isDirectory()) {
                    if (chid) {
                        file += getFileNameList(fileList[I].toString(), fen, chid);
                    }
                }
            }
        }
        return file;
    }

    public static boolean hasPath(String fileName) {
        if (StringUtil.isNULL(fileName)) return false;
        File file = new File(fileName);
        return file.exists() || fileName.indexOf(":/") != -1 && fileName.indexOf(":/") <= 3 || fileName.startsWith("/");
    }

    /**
     * 得到两个路径的差
     *
     * @param path1
     * @param path2
     * @return String
     */
    public static String getDecrease(String path1, String path2) {
        if (path1 == null) return "";
        if (path2 == null) return "";
        if (path1.length() == path2.length()) return "";
        if (path2.length() > path1.length()) {
            return path2.substring(path1.length(), path2.length());
        }
        if (path1.length() > path2.length()) {
            return path1.substring(path2.length(), path1.length());
        }
        return "/";
    }

    /**
     * 修复相对路径
     *
     * @param fileName
     * @param basePath
     * @return String
     */
    public static String fixPath(String fileName, String basePath) {
        String result;
        if (StringUtil.isNULL(fileName)) {
            result = basePath;
        } else if (hasPath(fileName)) {
            result = fileName;
        } else {
            result = basePath + fileName;
        }
        if (SystemUtil.WINDOWS.equalsIgnoreCase(SystemUtil.getOS())) {
            if (result.startsWith("/")) {
                result = result.substring(1, result.length());
            }
        }
        return result;
    }

    public static File[] append(File[] files, File file) {
        if (files == null) {
            File[] resultfiles = new File[1];
            resultfiles[0] = file;
            return resultfiles;
        } else {
            File[] resultfiles = new File[files.length + 1];
            System.arraycopy(files, 0, resultfiles, 0, files.length);
            resultfiles[files.length] = file;
            return resultfiles;
        }
    }

    /**
     * 得到盘符
     *
     * @param fileName
     * @return String
     */
    public static String getDiskVolume(String fileName) {
        if (StringUtil.isNULL(fileName)) return "";
        String dir = FileUtil.mendPath(fileName);
        if (dir.indexOf(":/") != -1) {
            dir = dir.substring(0, dir.indexOf(":/"));
            if (dir != null) {
                if (dir.startsWith("/")) {
                    dir = dir.substring(1) + ":/";
                } else {
                    dir = StringUtil.fristSplit(dir, ":") + ":/";
                }
                if (StringUtil.countMatches(dir, "/") > 1) {
                    if (dir.startsWith("/")) {
                        dir = "/" + StringUtil.fristSplit(dir.substring(1), "/") + "/";
                    }
                }
            }
        } else {
            dir = StringUtil.fristSplit(dir.substring(1), "/") + "/";
        }
        if (dir != null && StringUtil.countMatches(dir, "/") > 1) {
            dir = dir.substring(0, dir.indexOf("/")) + "/";
        }
        return dir;
    }

    /**
     * 调用System.out.println(getDiskInfo("c:"))
     * 参数为盘符 eg:"c:"
     *
     * @param dirPath
     * @return long
     */
    public static long getDiskFreeSpace(String dirPath) {
        try {
            long space = -1;
            Process process;
            Runtime run = Runtime.getRuntime();
            String command;
            if (SystemUtil.WINDOWS.equals(SystemUtil.getOS())) {
                if (dirPath.length() > 2) {
                    dirPath = dirPath.substring(0, 2);
                }
                command = "cmd.exe /c dir " + dirPath;
            } else {
                command = "command.com /c dir " + dirPath;
            }
            process = run.exec(command);
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String freeSpace = "", line;
            while ((line = in.readLine()) != null) {
                freeSpace = line;
            }
            if (freeSpace == null) {
                return -1;
            }
            process.destroy();
            freeSpace = freeSpace.trim();
            freeSpace = freeSpace.replaceAll(",", "");
            String[] strs = freeSpace.split(" ");
            for (int i = 1; i < strs.length; i++) {
                try {
                    if (StringUtil.isNULL(strs[i])) {
                        return Long.parseLong(strs[i]);
                    }
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
            }
            return space;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }
}
