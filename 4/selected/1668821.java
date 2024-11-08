package com.hcs.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import android.os.Environment;

public class FileUtils {

    private String SDPATH;

    public String getSDPATH() {
        return SDPATH;
    }

    public FileUtils() {
        SDPATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
    }

    /**
	 * 在SD卡上创建文件
	 * 
	 * @throws IOException
	 */
    public File creatSDFile(String fileName) throws IOException {
        File file = new File(SDPATH + fileName);
        file.createNewFile();
        return file;
    }

    /**
	 * 在SD卡上创建目录
	 * 
	 * @param dirName
	 */
    public File creatSDDir(String dirName) {
        File dir = new File(SDPATH + dirName);
        if (!dir.exists()) {
            dir.mkdir();
        }
        return dir;
    }

    /**
	 * 判断SD卡上的文件夹是否存在
	 */
    public boolean isFileExist(String fileName) {
        File file = new File(SDPATH + fileName);
        return file.exists();
    }

    /**
	 * 将一个InputStream里面的数据写入到SD卡中
	 */
    public File write2SDFromInput(String path, String fileName, InputStream input) {
        File file = null;
        FileOutputStream fos = null;
        try {
            creatSDDir(path);
            file = creatSDFile(path + fileName);
            fos = new FileOutputStream(file);
            byte buf[] = new byte[128];
            do {
                int numread = input.read(buf);
                if (numread <= 0) {
                    break;
                }
                fos.write(buf, 0, numread);
            } while (true);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.flush();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return file;
    }
}
