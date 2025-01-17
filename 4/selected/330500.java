package com.custom.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import com.custom.utils.Constant.DirType;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;

public class MondifyIndexImageIndex {

    private static final Logger logger = Logger.getLogger(MondifyIndexImageIndex.class);

    private static HashMap<String, int[]> imageIndexs = new HashMap<String, int[]>();

    public static void initImageIndexs(Context context, boolean imageCanMove) {
        try {
            String filePath = "";
            byte[] buf = null;
            if (imageCanMove) {
                filePath = Constant.foldName + "_" + Constant.imageIndexFileName;
                buf = LoadResources.loadFile(context, filePath, DirType.extSd, false);
            } else {
                filePath = Constant.path + File.separator + Constant.foldName + "_" + Constant.imageIndexFileName;
                if (Constant.getExtSdPath() != null && !"".equals(Constant.getExtSdPath())) {
                    buf = LoadResources.loadFile(context, filePath, DirType.extSd);
                }
                if (buf == null && Constant.getSdPath() != null && !"".equals(Constant.getSdPath())) {
                    buf = LoadResources.loadFile(context, filePath, DirType.sd);
                }
                if (buf == null) {
                    buf = LoadResources.loadFile(context, filePath, DirType.file);
                }
                if (buf == null) {
                    buf = LoadResources.loadFile(context, filePath, DirType.assets);
                }
            }
            if (buf == null) {
                return;
            }
            BufferedReader fin = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf)));
            String line = fin.readLine();
            while (line != null) {
                if (line.indexOf("=") > 0) {
                    String indexs = line.substring(line.indexOf("=") + 1);
                    if (indexs.length() < 3 || indexs.indexOf(":") < 0) {
                        imageIndexs.put(line.substring(0, line.indexOf("=")), null);
                    } else {
                        int[] indexArgs = new int[2];
                        try {
                            indexArgs[0] = Integer.parseInt(indexs.substring(0, indexs.indexOf(":")));
                            indexArgs[1] = Integer.parseInt(indexs.substring(indexs.indexOf(":") + 1));
                            imageIndexs.put(line.substring(0, line.indexOf("=")), indexArgs);
                        } catch (Exception e) {
                            e.printStackTrace();
                            imageIndexs.put(line.substring(0, line.indexOf("=")), null);
                        }
                    }
                }
                line = fin.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void modifyImageIndexs(Context context) {
        try {
            String filePath = Constant.getExtSdPath() + File.separator + Constant.foldName + "_" + Constant.imageIndexFileName;
            RandomAccessFile raf = new RandomAccessFile(filePath, "rw");
            raf.setLength(0);
            raf.close();
            FileOutputStream fos = new FileOutputStream(new File(filePath));
            Iterator it = imageIndexs.keySet().iterator();
            while (it.hasNext()) {
                String key = (String) it.next();
                int[] indexArgs = imageIndexs.get(key);
                key = key + "=" + indexArgs[0] + ":" + indexArgs[1] + "\n";
                fos.write(key.getBytes());
            }
            fos.getChannel().force(true);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void modifyImageIndexs(Context context, String btName, int[] indexs) {
        imageIndexs.put(btName, indexs);
        modifyImageIndexs(context);
    }

    public static int[] getImageIndexs(String btName) {
        return imageIndexs.get(btName);
    }
}
