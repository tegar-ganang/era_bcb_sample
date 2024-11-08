package com.baozou.framework.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class SdFileUtil {

    public static final int LATEST = 1;

    private static final String D = "SdFileUtil";

    Context context;

    String sdPath;

    String apkPath;

    boolean hasSd;

    String fullFolder;

    private static final String rootFloder = "baozou_img/";

    public SdFileUtil(Context c, String folder) {
        this.context = c;
        this.hasSd = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        this.sdPath = Environment.getExternalStorageDirectory().getPath();
        this.apkPath = this.context.getFilesDir().getPath();
        fullFolder = sdPath + "/" + rootFloder + folder + DateUtil.getToday() + "/";
        File file = new File(fullFolder);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public boolean createImg(String fileName, InputStream in) {
        Log.d(D, "create img : " + fullFolder + fileName);
        File file = new File(fullFolder + fileName);
        boolean isSuccess = true;
        if (!file.exists()) {
            FileOutputStream fout = null;
            BufferedInputStream bin = new BufferedInputStream(in);
            int readLength = 0;
            byte[] buffer = new byte[1024];
            try {
                fout = new FileOutputStream(file);
                while ((readLength = bin.read(buffer, 0, buffer.length)) != -1) {
                    fout.write(buffer, 0, readLength);
                }
            } catch (Exception e) {
                e.printStackTrace();
                isSuccess = false;
                Log.d(D, "create file faile!");
            } finally {
                try {
                    fout.close();
                    bin.close();
                    in.close();
                    if (!isSuccess) {
                        if (file.exists()) {
                            file.delete();
                            Log.d(D, "delete file :" + fullFolder + fileName);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return isSuccess;
    }

    public String readImgPath(String fileName) {
        Log.d(D, "read img : " + fullFolder + fileName);
        File file = new File(fullFolder + fileName);
        if (file.exists() && file.isFile()) {
            return fullFolder + fileName;
        } else {
            return null;
        }
    }

    public InputStream readImg(String fileName) {
        Log.d(D, "read img : " + fullFolder + fileName);
        File file = new File(fullFolder + fileName);
        FileInputStream fin = null;
        if (file.exists() && file.isFile()) {
            try {
                fin = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return fin;
    }
}
