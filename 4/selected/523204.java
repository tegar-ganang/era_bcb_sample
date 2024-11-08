package net.xtool.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class FileManager {

    private static final Logger logger = Logger.getLogger(FileManager.class.getName());

    private static boolean deleteFolder(File folder) {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                deleteFolder(file);
            } else {
                file.delete();
            }
        }
        return folder.delete();
    }

    public static boolean deleteFile(File file) {
        if (file == null) return true;
        if (file.isDirectory()) {
            return deleteFolder(file);
        } else return file.delete();
    }

    public static List<File> copyDirectory(File srcDir, File dstDir) throws IOException {
        List<File> subFolderList = new ArrayList();
        for (File file : srcDir.listFiles()) {
            if (file.isDirectory()) {
                File newDir = new File(dstDir, file.getName());
                newDir.mkdir();
                logger.info("Create folder : " + newDir.getPath());
                subFolderList.add(file);
            } else if (file.isFile()) {
                createFile(file, dstDir);
            }
        }
        return subFolderList;
    }

    public static void copyAllDirectories(List<File> folderList, File dstDir) throws IOException {
        for (File folder : folderList) {
            File srcFolder = new File(dstDir, folder.getName());
            if (srcFolder.exists()) {
                copyAllDirectories(copyDirectory(folder, srcFolder), srcFolder);
            }
        }
    }

    public static void copy(File srcFile, File dstFile, String newFileName) throws IOException {
        if (srcFile.isFile() && dstFile.isDirectory()) {
            File newFile = new File(dstFile, newFileName);
            if (newFile.createNewFile()) {
                copyFile(srcFile, newFile);
            }
        }
    }

    public static void copy(File srcFile, File dstFile) throws IOException {
        if (srcFile.isDirectory() && dstFile.isDirectory()) {
            List<File> folderList = copyDirectory(srcFile, dstFile);
            copyAllDirectories(folderList, dstFile);
        } else if (srcFile.isFile() && dstFile.isDirectory()) {
            createFile(srcFile, dstFile);
        }
    }

    public static void createFile(File srcFile, File dstDir) throws IOException {
        File newFile = new File(dstDir, srcFile.getName());
        if (newFile.createNewFile()) {
            copyFile(srcFile, newFile);
        }
    }

    public static void copyFile(File srcFile, File dstFile) {
        logger.info("Create file : " + dstFile.getPath());
        try {
            FileChannel srcChannel = new FileInputStream(srcFile).getChannel();
            FileChannel dstChannel = new FileOutputStream(dstFile).getChannel();
            dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
            srcChannel.close();
            dstChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        copy(new File("C:/Documents and Settings/saarnsam.EU/Desktop/docs"), new File("C:/TEMP/to"));
    }
}
