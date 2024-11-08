package org.wsmostudio.integration.ssb.deployment;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class IOUtils {

    public static boolean copyProcessFilesSecure(File srcFile, File targetDir) throws Exception {
        if (srcFile == null) {
            return false;
        }
        File targetFile = new File(targetDir, srcFile.getName());
        boolean response = false == targetFile.exists();
        if (false == targetFile.equals(srcFile)) {
            copyDirectory(srcFile, targetFile);
        }
        return response;
    }

    public static File archiveBundle(File projectDir, File targetDir) throws Exception {
        File workDirectory = new File(targetDir, "bin");
        if (false == workDirectory.exists()) {
            workDirectory.mkdir();
        }
        File newProjectDir = new File(workDirectory, projectDir.getName());
        if (newProjectDir.exists() && newProjectDir.isDirectory()) {
            deleteDirectory(newProjectDir);
        }
        newProjectDir.mkdir();
        String[] files = projectDir.list();
        for (int i = 0; i < files.length; i++) {
            File projectEntry = new File(projectDir, files[i]);
            if (projectEntry.isDirectory() && false == projectEntry.getName().equalsIgnoreCase("META-INF")) {
                File archiveFile = new File(newProjectDir, projectEntry.getName() + ".zip");
                archiveFolder(projectEntry.getAbsolutePath(), archiveFile.getAbsolutePath());
            } else {
                copyDirectory(projectEntry, new File(newProjectDir, projectEntry.getName()));
            }
        }
        File resultBundleFile = new File(workDirectory, newProjectDir.getName() + ".zip");
        archiveFolder(newProjectDir.getAbsolutePath(), resultBundleFile.getAbsolutePath());
        deleteDirectory(newProjectDir);
        return (resultBundleFile.exists()) ? resultBundleFile : null;
    }

    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    public static void copyDirectory(File srcPath, File dstPath) throws IOException {
        if (srcPath.isDirectory()) {
            if (!dstPath.exists()) {
                dstPath.mkdir();
            }
            String files[] = srcPath.list();
            for (int i = 0; i < files.length; i++) {
                copyDirectory(new File(srcPath, files[i]), new File(dstPath, files[i]));
            }
        } else {
            if (!srcPath.exists()) {
                System.out.println("File or directory does not exist.");
                return;
            } else {
                InputStream in = new FileInputStream(srcPath);
                OutputStream out = new FileOutputStream(dstPath);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
            }
        }
    }

    public static boolean archiveFolder(String folderPath, String archFullName) throws Exception {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(archFullName));
        archiveFolder(folderPath, zos, folderPath);
        zos.flush();
        zos.close();
        return true;
    }

    public static boolean archiveFolder(String folderPath, ZipOutputStream zos, String ignoredPathPrefix) {
        try {
            File zipDir = new File(folderPath);
            String[] dirList = zipDir.list();
            byte[] readBuffer = new byte[2156];
            int bytesIn = 0;
            for (int i = 0; i < dirList.length; i++) {
                File f = new File(zipDir, dirList[i]);
                if (f.isDirectory()) {
                    String filePath = f.getPath();
                    archiveFolder(filePath, zos, ignoredPathPrefix);
                    continue;
                }
                FileInputStream fis = new FileInputStream(f);
                String entryPath = f.getPath();
                if (entryPath.startsWith(ignoredPathPrefix)) {
                    entryPath = entryPath.substring(ignoredPathPrefix.length() + 1);
                }
                ZipEntry anEntry = new ZipEntry(entryPath.replace('\\', '/'));
                zos.putNextEntry(anEntry);
                while ((bytesIn = fis.read(readBuffer)) != -1) {
                    zos.write(readBuffer, 0, bytesIn);
                }
                fis.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}
