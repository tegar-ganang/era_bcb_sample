package org.sysolar.util.file;

import static org.sysolar.util.Constants.LS;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sysolar.util.Constants;

public final class FileIO {

    private static final boolean DEBUG = true;

    /**
     * 创建文件并返回文件的引用，如果文件所在的文件夹不存在，则首先创建文件夹。
     * 
     * @param path
     * @return
     * @throws IOException
     */
    public static File createFile(String path) throws IOException {
        File file = new File(path);
        File parent = file.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        if (!file.exists()) {
            file.createNewFile();
        }
        return file;
    }

    public static File createFile(String root, String fileName) throws IOException {
        return createFile(new File(root), fileName);
    }

    public static File createFile(File root, String fileName) throws IOException {
        if (!root.exists()) {
            root.mkdirs();
        }
        File file = new File(root, fileName);
        if (!file.exists()) {
            file.createNewFile();
        }
        return file;
    }

    public static File createDir(String root, String dirName) throws IOException {
        return createDir(new File(root), dirName);
    }

    public static File createDir(File root, String dirName) throws IOException {
        if (!root.exists()) {
            root.mkdirs();
        }
        File file = new File(root, dirName);
        if (!file.exists()) {
            file.mkdir();
        }
        return file;
    }

    /**
     * 创建以当前日期yyyyMMdd命名的文件夹。
     * 
     * @param root
     * @return
     */
    public static File createTodyDir(String root) throws IOException {
        return createDir(root, new SimpleDateFormat("yyyyMMdd").format(new Date()));
    }

    /**
     * 获得文件大小。
     * 
     * @param file
     * @return
     */
    public static int getFileSize(File file) {
        if (!file.exists()) {
            return -1;
        }
        int size = -1;
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            size = in.available();
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        } finally {
            close(in);
        }
        return size;
    }

    public static void close(InputStream in) {
        if (null != in) {
            try {
                in.close();
            } catch (IOException ex) {
            }
        }
    }

    public static void close(OutputStream out) {
        if (null != out) {
            try {
                out.close();
            } catch (IOException ex) {
            }
        }
    }

    public static void close(Reader in) {
        if (null != in) {
            try {
                in.close();
            } catch (IOException ex) {
            }
        }
    }

    public static void close(Writer out) {
        if (null != out) {
            try {
                out.close();
            } catch (IOException ex) {
            }
        }
    }

    /**
     * 获得一个文件夹里的非隐藏的子文件夹。
     * 
     * @param pathname 文件夹的绝对路径
     * @return 文件夹里的非隐藏的子文件夹数组对象。
     */
    public static File[] listDirs(File pathname) {
        if (!pathname.exists() || !pathname.isDirectory()) {
            System.err.println(pathname + " is not a directory.");
            return new File[] {};
        }
        return pathname.listFiles(new FileFilter() {

            public boolean accept(File file) {
                return file.isDirectory() && !file.isHidden();
            }
        });
    }

    /**
     * 获得一个文件夹里的非隐藏的子文件夹。
     * 
     * @param pathname 文件夹的绝对路径
     * @return 文件夹里的非隐藏的子文件夹数组对象。
     */
    public static File[] listDirs(String pathname) {
        return listDirs(new File(pathname));
    }

    /**
     * 获得一个文件夹里的非隐藏的文件，不包含文件夹。
     * 
     * @param pathname 文件夹的绝对路径
     * @return 文件夹里的非隐藏的文件数组对象。
     */
    public static File[] listFiles(File pathname) {
        if (!pathname.exists() || !pathname.isDirectory()) {
            System.err.println(pathname + " is not a directory.");
            return new File[] {};
        }
        return pathname.listFiles(new FileFilter() {

            public boolean accept(File file) {
                return file.isFile() && !file.isHidden();
            }
        });
    }

    /**
     * 获得一个文件夹里的非隐藏的文件，不包含文件夹。
     * 
     * @param pathname 文件夹的绝对路径
     * @return 文件夹里的非隐藏的文件数组对象。
     */
    public static File[] listFiles(String pathname) {
        return listFiles(new File(pathname));
    }

    /**
     * 获得一个文件夹里所有非隐藏的文件夹和子文件夹。
     * 
     * @param pathname 文件夹的绝对路径
     * @return 文件夹列表。
     */
    @SuppressWarnings("unchecked")
    public static List<File> listDirsSubdirs(File pathname) {
        if (!pathname.exists() || !pathname.isDirectory()) {
            return Collections.EMPTY_LIST;
        }
        List<File> dirsSubdirs = new ArrayList<File>(50);
        List<File> curLoop = new ArrayList<File>(20);
        List<File> nextLoop = new ArrayList<File>(20);
        curLoop.addAll(Arrays.asList(listDirs(pathname)));
        while (!curLoop.isEmpty()) {
            for (File dir : curLoop) {
                nextLoop.addAll(Arrays.asList(listDirs(dir)));
            }
            dirsSubdirs.addAll(curLoop);
            curLoop.clear();
            curLoop.addAll(nextLoop);
            nextLoop.clear();
        }
        Collections.sort(dirsSubdirs);
        return dirsSubdirs;
    }

    /**
     * 获得一个文件夹里所有非隐藏的文件夹和子文件夹。
     * 
     * @param pathname 文件夹的绝对路径
     * @return 文件夹列表。
     */
    public static List<File> listDirsSubdirs(String pathname) {
        return listDirsSubdirs(new File(pathname));
    }

    /**
     * 获得一个文件夹以及非隐藏的子文件夹里所有非隐藏的文件。
     * 
     * @param pathname 文件夹的绝对路径
     * @return 文件列表。
     */
    @SuppressWarnings("unchecked")
    public static List<File> listFilesInDirSubdirs(File pathname) {
        if (!pathname.exists() || !pathname.isDirectory()) {
            return Collections.EMPTY_LIST;
        }
        List<File> files = new ArrayList<File>(200);
        List<File> dirsSubdirs = new ArrayList<File>(50);
        dirsSubdirs.add(pathname);
        dirsSubdirs.addAll(listDirsSubdirs(pathname));
        for (File dir : dirsSubdirs) {
            files.addAll(Arrays.asList(listFiles(dir)));
        }
        Collections.sort(files);
        return files;
    }

    /**
     * 获得一个文件夹以及非隐藏的子文件夹里所有非隐藏的文件。
     * 
     * @param dirPath 文件夹的绝对路径
     * @return 文件列表。
     */
    public static List<File> listFilesInDirSubdirs(String dirPath) {
        return listFilesInDirSubdirs(new File(dirPath));
    }

    /**
     * 修改文本文件的编码。
     * 
     * @param filePath  文件路径
     * @param oldEnc    旧编码
     * @param newEnc    新编码
     * @return true：修改成功，false：修改失败
     */
    public static boolean changeFileEnc(String filePath, String oldEnc, String newEnc) {
        return changeFileEnc(new File(filePath), oldEnc, newEnc);
    }

    /**
     * 修改文本文件的编码。
     * 
     * @param file      文件对象
     * @param oldEnc    旧编码
     * @param newEnc    新编码
     * @return true：修改成功，false：修改失败
     */
    public static boolean changeFileEnc(File file, String oldEnc, String newEnc) {
        return writeToFile(file, readAsString(file, oldEnc), false, newEnc);
    }

    /**
     * 修改一个文件夹下（包括其所有的子文件夹）所有文本文件的编码，。
     * 
     * @param dir       文件夹的路径
     * @param oldEnc    旧编码
     * @param newEnc    新编码
     * @return true：修改成功，false：修改失败
     */
    public static boolean changeFileEncFromDir(String dir, String oldEnc, String newEnc) {
        return changeFileEncFromDir(new File(dir), oldEnc, newEnc);
    }

    /**
     * 修改一个文件夹下（包括其所有的子文件夹）所有文本文件的编码，。
     * 
     * @param dir       表示文件夹的文件对象
     * @param oldEnc    旧编码
     * @param newEnc    新编码
     * @return true：修改成功，false：修改失败
     */
    public static boolean changeFileEncFromDir(File dir, String oldEnc, String newEnc) {
        for (File file : listFilesInDirSubdirs(dir)) {
            if (!changeFileEnc(file, oldEnc, newEnc)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 以操作系统默认的编码方式或者指定的编码方式读取文件里的内容，作为一个字符串返回。
     * 
     * @param filePath  文件的路径
     * @param enc       文件的编码方式，如果没有此参数则以操作系统默认编码
     * @return 包含文件全部内容的字符串
     */
    public static String readAsString(String filePath, String... enc) {
        return readAsString(new File(filePath), enc);
    }

    /**
     * 以操作系统默认的编码方式或者指定的编码方式读取文件里的内容，作为一个字符串返回。
     * 
     * @param file  文件对象
     * @param enc   文件的编码方式，如果没有此参数则以操作系统默认编码
     * @return 包含文件全部内容的字符串
     */
    public static String readAsString(File file, String... enc) {
        StringBuilder buffer = new StringBuilder(1000);
        BufferedReader in = null;
        try {
            if (enc != null && enc.length > 0) {
                in = new BufferedReader(new InputStreamReader(new FileInputStream(file), enc[0]));
            } else {
                in = new BufferedReader(new FileReader(file));
            }
            String oneLine = null;
            while ((oneLine = in.readLine()) != null) {
                buffer.append(oneLine).append(LS);
            }
        } catch (Exception e) {
            e.printStackTrace(System.out);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return buffer.toString();
    }

    /**
     * 以操作系统默认的编码方式或者指定的编码方式读取文件里的内容，每行内容作为List的一个元素，返回List。
     * 
     * @param filePath  文件的路径
     * @param enc       文件的编码方式，如果没有此参数则以操作系统默认编码
     * @return 包含文件内容的List
     */
    public static List<String> readAsList(String filePath, String... enc) {
        return readAsList(new File(filePath), enc);
    }

    /**
     * 以操作系统默认的编码方式或者指定的编码方式读取文件里的内容，每行内容作为List的一个元素，返回List。
     * 
     * @param file  文件对象
     * @param enc   文件的编码方式，如果没有此参数则以操作系统默认编码
     * @return 包含文件内容的List
     */
    public static List<String> readAsList(File file, String... enc) {
        List<String> contentList = new ArrayList<String>();
        BufferedReader in = null;
        try {
            if (enc != null && enc.length > 0) {
                in = new BufferedReader(new InputStreamReader(new FileInputStream(file), enc[0]));
            } else {
                in = new BufferedReader(new FileReader(file));
            }
            String oneLine = null;
            while ((oneLine = in.readLine()) != null) {
                contentList.add(oneLine);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace(System.out);
        } catch (IOException e) {
            e.printStackTrace(System.out);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return contentList;
    }

    /**
     * 把 键-值 对文件内容读取到 Map 里。
     * 
     * @param path
     * @param enc
     * @return
     */
    public static Map<String, String> readToMap(String path, String enc) {
        BufferedReader in = null;
        Map<String, String> map = new HashMap<String, String>();
        String msg = null;
        try {
            FileInputStream is = new FileInputStream(path);
            in = new BufferedReader(new InputStreamReader(is, enc));
            String[] entry = null;
            while ((msg = in.readLine()) != null) {
                if ("".equals(msg.trim())) {
                    continue;
                }
                entry = msg.split("[:=]", 2);
                map.put(entry[0], entry[1]);
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
        } finally {
            close(in);
        }
        return map;
    }

    /**
     * 以操作系统默认的编码或者指定的编码往文件里写内容。
     * 
     * @param filePath  文件的路径
     * @param content   内容
     * @param append    true：不清空原内容，false：清空原内容
     * @param enc       编码方式
     */
    public static boolean writeToFile(String filePath, String content, boolean append, String enc) {
        return writeToFile(new File(filePath), content, append, enc);
    }

    public static boolean writeToFile(File file, String content, boolean append, String enc) {
        if (DEBUG) System.out.println(file.getAbsolutePath());
        File parent = file.getParentFile();
        if (!parent.exists()) {
            if (DEBUG) System.out.println(">>> make directories: " + parent.getAbsolutePath());
            parent.mkdirs();
        }
        boolean result = true;
        BufferedWriter out = null;
        try {
            if (null != enc) {
                out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append), enc));
            } else {
                out = new BufferedWriter(new FileWriter(file, append));
            }
            out.write(content);
        } catch (IOException e) {
            e.printStackTrace(System.out);
            result = false;
        } finally {
            close(out);
        }
        return result;
    }

    /**
     * 以特定的编码把 Map 里的内容写至文件。
     * 
     * @param path
     * @param map
     * @param enc
     * @return
     */
    public static boolean writeToFile(String path, Map<String, String> map, String enc) {
        StringBuilder content = new StringBuilder(1024);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            content.append(entry.getKey()).append(":").append(entry.getValue()).append(Constants.LS);
        }
        return writeToFile(path, content.toString(), true, enc);
    }

    public static void main(String[] args) throws Exception {
        createTodyDir("D:/temp/aoetec/");
    }
}
