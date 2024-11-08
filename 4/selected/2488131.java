package com.ivy.code2web.playground;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.ivy.code2web.utils.JavaFileBean;

public class CopyFilesInDirectory {

    static List<JavaFileBean> fileBeanList = new ArrayList<JavaFileBean>();

    public static void main(String args[]) {
        File mainDir = new File("G:\\Documents\\Musicipod\\");
        String destDir = "G:\\Documents\\MusicipodMerge\\";
        fetchAllFiles(mainDir, destDir);
    }

    public static void fetchAllFiles(File parentDir, String destDir) {
        File[] fileArray = parentDir.listFiles();
        for (File aFile : fileArray) {
            JavaFileBean fBean = new JavaFileBean();
            if (aFile.isFile()) {
                String fullName = aFile.getAbsolutePath();
                String fName = aFile.getName();
                fBean.setFileFullName(fullName);
                System.out.println(fBean.getFileFullName());
                copyFile(fullName, destDir + fName);
            } else if (aFile.isDirectory()) {
                fetchAllFiles(aFile, destDir);
            }
        }
    }

    public static void copyFile(String fromFile, String toFile) {
        FileInputStream from = null;
        FileOutputStream to = null;
        try {
            from = new FileInputStream(fromFile);
            to = new FileOutputStream(toFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = from.read(buffer)) != -1) to.write(buffer, 0, bytesRead);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
}
