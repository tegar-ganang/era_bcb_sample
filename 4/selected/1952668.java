package com.yinzhijie.dt.runtime.makezip.ziputil;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 把多个文件打包到一个文件
 * 
 * @author dingucihao
 * 
 */
public class ZipUtil {

    /**
	 * The buffer.
	 */
    protected static byte buf[] = new byte[1024];

    /**
	 * The encoder.
	 */
    protected static String encoder = "GBK";

    /**
	 * The lever.
	 */
    protected static int def_lever = 7;

    /**
	 * The lever.
	 */
    protected static String UN_ZIP_TMP = "C:/UNZIPTMP";

    /**
	 * 遍历目录并添加文件.
	 * 
	 * @param jos -
	 *            JAR 输出流
	 * @param file -
	 *            目录文件名
	 * @param pathName -
	 *            ZIP中的目录名
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
    private static void recurseFiles(ZipOutputStream jos, File file, String pathName) throws IOException, FileNotFoundException {
        if (file.isDirectory()) {
            pathName = pathName + file.getName() + "/";
            jos.putNextEntry(new ZipEntry(pathName));
            String fileNames[] = file.list();
            if (fileNames != null) {
                for (int i = 0; i < fileNames.length; i++) recurseFiles(jos, new File(file, fileNames[i]), pathName);
            }
        } else {
            ZipEntry jarEntry = new ZipEntry(pathName + file.getName());
            FileInputStream fin = new FileInputStream(file);
            BufferedInputStream in = new BufferedInputStream(fin);
            jos.putNextEntry(jarEntry);
            int len;
            while ((len = in.read(buf)) >= 0) jos.write(buf, 0, len);
            in.close();
            jos.closeEntry();
        }
    }

    /**
	 * 遍历目录并添加文件.
	 * 
	 * @param jos -
	 *            JAR 输出流
	 * @param file -
	 *            目录文件名
	 * @param pathName -
	 *            ZIP中的目录名
	 * @param exceptFileList 不添加到zip包的文件列表
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
    private static void recurseFiles(ZipOutputStream jos, File file, String pathName, List<File> exceptFileList) throws IOException, FileNotFoundException {
        if (exceptFileList != null && exceptFileList.size() != 0) {
            for (int i = 0; i < exceptFileList.size(); i++) {
                if (exceptFileList.get(i).getPath().equals(file.getPath())) {
                    return;
                }
            }
        }
        if (file.isDirectory()) {
            pathName = pathName + file.getName() + "/";
            jos.putNextEntry(new ZipEntry(pathName));
            String fileNames[] = file.list();
            if (fileNames != null) {
                for (int i = 0; i < fileNames.length; i++) recurseFiles(jos, new File(file, fileNames[i]), pathName, exceptFileList);
            }
        } else {
            ZipEntry jarEntry = new ZipEntry(pathName + file.getName());
            FileInputStream fin = new FileInputStream(file);
            BufferedInputStream in = new BufferedInputStream(fin);
            jos.putNextEntry(jarEntry);
            int len;
            while ((len = in.read(buf)) >= 0) jos.write(buf, 0, len);
            in.close();
            jos.closeEntry();
        }
    }

    /**
	 * 遍历目录并添加文件.
	 * 
	 * @param jos -
	 *            JAR 输出流
	 * @param file -
	 *            目录文件名
	 * @param pathName -
	 *            ZIP中的目录名
	 * @param isFileList 可压缩zip包的文件列表
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
    private static void recurseFilesInFiles(ZipOutputStream jos, File file, String pathName, List<File> isFileList) throws IOException, FileNotFoundException {
        boolean zipFlg = false;
        if (isFileList != null && isFileList.size() != 0) {
            for (int i = 0; i < isFileList.size(); i++) {
                if (isFileList.get(i).getPath().equals(file.getPath())) {
                    zipFlg = true;
                }
            }
        }
        if (!zipFlg) {
            return;
        }
        if (file.isDirectory()) {
            pathName = pathName + file.getName() + "/";
            jos.putNextEntry(new ZipEntry(pathName));
            String fileNames[] = file.list();
            if (fileNames != null) {
                for (int i = 0; i < fileNames.length; i++) recurseFilesInFiles(jos, new File(file, fileNames[i]), pathName, isFileList);
            }
        } else {
            ZipEntry jarEntry = new ZipEntry(pathName + file.getName());
            FileInputStream fin = new FileInputStream(file);
            BufferedInputStream in = new BufferedInputStream(fin);
            jos.putNextEntry(jarEntry);
            int len;
            while ((len = in.read(buf)) >= 0) jos.write(buf, 0, len);
            in.close();
            jos.closeEntry();
        }
    }

    /**
	 * 创建 ZIP/JAR 文件.
	 * 
	 * @param directory -
	 *            要添加的目录
	 * @param zipFile -
	 *            保存的 ZIP 文件名
	 * @param zipFolderName -
	 *            ZIP 中的路径名
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
    public static void makeDirectoryToZip(File directory, File zipFile, String zipFolderName) throws IOException, FileNotFoundException {
        makeDirectoryToZip(directory, zipFile, zipFolderName, def_lever);
    }

    /**
	 * 创建 ZIP/JAR 文件.
	 * 
	 * @param directory -
	 *            要添加的目录
	 * @param zipFile -
	 *            保存的 ZIP 文件名
	 * @param zipFolderName -
	 *            ZIP 中的路径名
	 * @param level -
	 *            压缩级别(0~9)
	 * @param exceptFileList 不添加到zip包的文件列表
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
    public static void makeDirTozipExceptFile(File directory, File zipFile, String zipFolderName, int level, List<File> exceptFileList) throws IOException, FileNotFoundException {
        level = checkZipLevel(level);
        if (zipFolderName == null) {
            zipFolderName = "";
        }
        ZipOutputStream jos = new ZipOutputStream(new FileOutputStream(zipFile), encoder);
        jos.setLevel(level);
        String fileNames[] = directory.list();
        if (fileNames != null) {
            for (int i = 0; i < fileNames.length; i++) recurseFiles(jos, new File(directory, fileNames[i]), zipFolderName, exceptFileList);
        }
        jos.close();
    }

    /**
	 * 创建 ZIP/JAR 文件.
	 * 
	 * @param directory -
	 *            要添加的目录
	 * @param zipFile -
	 *            保存的 ZIP 文件名
	 * @param zipFolderName -
	 *            ZIP 中的路径名
	 * @param level -
	 *            压缩级别(0~9)
	 * @param isFileList 可zip包的文件列表
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
    public static void makeDirTozipInFiles(File directory, File zipFile, String zipFolderName, int level, List<File> isFileList) throws IOException, FileNotFoundException {
        level = checkZipLevel(level);
        if (zipFolderName == null) {
            zipFolderName = "";
        }
        ZipOutputStream jos = new ZipOutputStream(new FileOutputStream(zipFile), encoder);
        jos.setLevel(level);
        String fileNames[] = directory.list();
        if (fileNames != null) {
            for (int i = 0; i < fileNames.length; i++) recurseFilesInFiles(jos, new File(directory, fileNames[i]), zipFolderName, isFileList);
        }
        jos.close();
    }

    /**
	 * 创建 ZIP/JAR 文件.
	 * 
	 * @param directory -
	 *            要添加的目录
	 * @param zipFile -
	 *            保存的 ZIP 文件名
	 * @param zipFolderName -
	 *            ZIP 中的路径名
	 * @param level -
	 *            压缩级别(0~9)
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
    public static void makeDirectoryToZip(File directory, File zipFile, String zipFolderName, int level) throws IOException, FileNotFoundException {
        level = checkZipLevel(level);
        if (zipFolderName == null) {
            zipFolderName = "";
        }
        ZipOutputStream jos = new ZipOutputStream(new FileOutputStream(zipFile), encoder);
        jos.setLevel(level);
        String fileNames[] = directory.list();
        if (fileNames != null) {
            for (int i = 0; i < fileNames.length; i++) recurseFiles(jos, new File(directory, fileNames[i]), zipFolderName);
        }
        jos.close();
    }

    /**
	 * 创建 ZIP/JAR 文件.
	 * 
	 * @param files -
	 *            要添加的文件列表
	 * @param zipFile -
	 *            保存的 ZIP 文件名
	 * @param zipFolderName -
	 *            ZIP 中的路径名
	 * @param level -
	 *            压缩级别(0~9)
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
    public static void toZip(List<File> files, File zipFile, String zipFolderName, int level, List<File> exceptFileList) throws IOException, FileNotFoundException {
        level = checkZipLevel(level);
        if (zipFolderName == null) {
            zipFolderName = "";
        }
        ZipOutputStream jos = new ZipOutputStream(new FileOutputStream(zipFile), encoder);
        jos.setLevel(level);
        for (int i = 0; i < files.size(); i++) {
            recurseFiles(jos, files.get(i), zipFolderName, exceptFileList);
        }
        jos.close();
    }

    /**
	 * 创建 ZIP/JAR 文件.
	 * 
	 * @param files -
	 *            要添加的文件列表
	 * @param zipFile -
	 *            保存的 ZIP 文件名
	 * @param zipFolderName -
	 *            ZIP 中的路径名
	 * @param level -
	 *            压缩级别(0~9)
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
    public static void toZip(List<File> files, File zipFile, String zipFolderName, int level) throws IOException, FileNotFoundException {
        level = checkZipLevel(level);
        if (zipFolderName == null) {
            zipFolderName = "";
        }
        ZipOutputStream jos = new ZipOutputStream(new FileOutputStream(zipFile), encoder);
        jos.setLevel(level);
        for (int i = 0; i < files.size(); i++) {
            recurseFiles(jos, files.get(i), zipFolderName);
        }
        jos.close();
    }

    /**
	 * 创建 ZIP/JAR 文件.
	 * 
	 * @param files -
	 *            要添加的文件列表
	 * @param zipFile -
	 *            要添加的 ZIP 文件名
	 * @param zipFolderName -
	 *            ZIP 中的路径名
	 * @param level -
	 *            压缩级别(0~9)
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
    public static void AddFilesToZip(List<File> files, File zipFile, String zipFolderName, int level) throws IOException, FileNotFoundException {
        level = checkZipLevel(level);
        if (zipFolderName == null) {
            zipFolderName = "";
        }
        createDirectory(UN_ZIP_TMP, "");
        String unzipford = UN_ZIP_TMP + "/" + java.util.UUID.randomUUID().toString().replaceAll("-", "");
        createDirectory(unzipford, "");
        unZip(zipFile.getPath(), unzipford);
        ZipOutputStream jos = new ZipOutputStream(new FileOutputStream(zipFile), encoder);
        jos.setLevel(level);
        for (File f : new File(unzipford).listFiles()) {
            files.add(f);
        }
        for (int i = 0; i < files.size(); i++) {
            recurseFiles(jos, files.get(i), zipFolderName);
        }
        deletedir(new File(unzipford));
        jos.close();
    }

    public static boolean deletefile(File f) {
        if (f.isFile()) f.delete();
        return true;
    }

    public static boolean deletedir(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) deletedir(files[i]); else deletefile(files[i]);
            }
        }
        f.delete();
        return true;
    }

    /**
	 * 检查并设置有效的压缩级别.
	 * 
	 * @param level -
	 *            压缩级别
	 * @return 有效的压缩级别或者默认压缩级别
	 */
    public static int checkZipLevel(int level) {
        if (level < 0 || level > 9) level = 7;
        return level;
    }

    private static void createDirectory(String directory, String subDirectory) {
        String dir[];
        File fl = new File(directory);
        try {
            if (subDirectory == "" && fl.exists() != true) fl.mkdir(); else if (subDirectory != "") {
                dir = subDirectory.replace('\\', '/').split("/");
                for (int i = 0; i < dir.length; i++) {
                    File subFile = new File(directory + File.separator + dir[i]);
                    if (subFile.exists() == false) subFile.mkdir();
                    directory += File.separator + dir[i];
                }
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
	 * 解压缩zip文件
	 * 
	 * @param zipFileName
	 * @param outputDirectory
	 * @throws Exception
	 */
    public static void unZip(String unZipfileName, String outputDirectory) throws IOException, FileNotFoundException {
        FileOutputStream fileOut;
        File file;
        ZipEntry zipEntry;
        ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(new FileInputStream(unZipfileName)), encoder);
        while ((zipEntry = zipIn.getNextEntry()) != null) {
            file = new File(outputDirectory + File.separator + zipEntry.getName());
            if (zipEntry.isDirectory()) {
                createDirectory(file.getPath(), "");
            } else {
                File parent = file.getParentFile();
                if (!parent.exists()) {
                    createDirectory(parent.getPath(), "");
                }
                fileOut = new FileOutputStream(file);
                int readedBytes;
                while ((readedBytes = zipIn.read(buf)) > 0) {
                    fileOut.write(buf, 0, readedBytes);
                }
                fileOut.close();
            }
            zipIn.closeEntry();
        }
    }
}
