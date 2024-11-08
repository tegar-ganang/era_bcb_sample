package org.dsgt.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileUtils {

    public static File findFirstFileInstance(List<String> directoryList, String fileName) {
        for (String directory : directoryList) {
            File file = new File(StringUtils.pathCombine(directory, fileName));
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    public static List<File> findFirstFileInstances(List<String> directoryList, List<String> fileNameList) {
        List<File> fileList = new ArrayList<File>();
        Set<String> foundFiles = new HashSet<String>();
        for (String fileName : fileNameList) {
            for (String directory : directoryList) {
                File file = new File(StringUtils.pathCombine(directory, fileName));
                if (file.exists() && !foundFiles.contains(fileName)) {
                    fileList.add(file);
                    foundFiles.add(fileName);
                }
            }
        }
        return fileList;
    }

    public static boolean copyFile(String sourceFileName, String destFileName) {
        FileChannel ic = null;
        FileChannel oc = null;
        try {
            ic = new FileInputStream(sourceFileName).getChannel();
            oc = new FileOutputStream(destFileName).getChannel();
            ic.transferTo(0, ic.size(), oc);
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                ic.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                oc.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
