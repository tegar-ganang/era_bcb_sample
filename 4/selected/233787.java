package com.smb.MMUtil.handler.base;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ZipPackageManager {

    private static Log logger = LogFactory.getLog(ZipPackageManager.class);

    /**
         * @param baseDir 所要压缩的目录名（包含绝对路径）
         * @param objFileName 压缩后的文件名
         */
    private long timesmpt = System.currentTimeMillis();

    public void createZip(String baseDir, String objFileName) throws Exception {
        logger.info("createZip: [ " + baseDir + "]   [" + objFileName + "]");
        baseDir = baseDir + "/" + timesmpt;
        File folderObject = new File(baseDir);
        if (folderObject.exists()) {
            List<?> fileList = getSubFiles(new File(baseDir));
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(objFileName));
            ZipEntry ze = null;
            byte[] buf = new byte[1024];
            int readLen = 0;
            for (int i = 0; i < fileList.size(); i++) {
                File f = (File) fileList.get(i);
                ze = new ZipEntry(getAbsFileName(baseDir, f));
                ze.setSize(f.length());
                ze.setTime(f.lastModified());
                zos.putNextEntry(ze);
                InputStream is = new BufferedInputStream(new FileInputStream(f));
                while ((readLen = is.read(buf, 0, 1024)) != -1) {
                    zos.write(buf, 0, readLen);
                }
                is.close();
            }
            zos.close();
        } else {
            throw new Exception("this folder isnot exist!");
        }
    }

    /**
         * 取得指定目录下的所有文件列表，包括子目录.
         * @param baseDir   ,   File 指定的目录
         * @return 包含java.io.File的List
         */
    private List<File> getSubFiles(File baseDir) {
        List<File> ret = new ArrayList<File>();
        File[] tmp = baseDir.listFiles();
        for (int i = 0; i < tmp.length; i++) {
            if (tmp[i].isFile()) {
                ret.add(tmp[i]);
            }
            if (tmp[i].isDirectory()) {
                ret.addAll(getSubFiles(tmp[i]));
            }
        }
        return ret;
    }

    /**
         * 给定根目录，返回另一个文件名的相对路径，用于zip文件中的路径.
         * @param baseDir  ,  java.lang.String 根目录
         * @param realFileName    java.io.File 实际的文件名
         * @return 相对文件名
         */
    private String getAbsFileName(String baseDir, File realFileName) {
        File real = realFileName;
        File base = new File(baseDir);
        String ret = real.getName();
        while (true) {
            real = real.getParentFile();
            if (real == null) break;
            if (real.equals(base)) break; else {
                ret = real.getName() + "/" + ret;
            }
        }
        return ret;
    }

    public void createPackageDir(String baseDir, String packageName) throws Exception {
        logger.info("createPackageDir: [ " + baseDir + "]   [" + packageName + "]");
        String FMTpackageName = packageName.replaceAll("\\.", "/");
        baseDir = baseDir + "/" + timesmpt + "/" + FMTpackageName;
        File file = new File(baseDir);
        file.mkdirs();
    }

    public static void main(String args[]) {
        ZipPackageManager manager = new ZipPackageManager();
        try {
            manager.createPackageDir("c:\\test", "com.cn.smb.xxx");
            manager.createZip("c:\\test", "c:\\test.zip");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
