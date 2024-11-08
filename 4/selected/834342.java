package org.susan.java.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipFile;

public class AddFileToZip {

    private static final String source = "D:/work/test";

    public static void main(String args[]) throws Exception {
        File file = new File("D:/work/love.txt");
        @SuppressWarnings("unused") ZipFile zipFile = new ZipFile("D:/work/test1.zip");
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("D:/work/test1.zip"));
        zos.setEncoding("GBK");
        ZipEntry entry = null;
        if (file.isDirectory()) {
            entry = new ZipEntry(getAbsFileName(source, file) + "/");
        } else {
            entry = new ZipEntry(getAbsFileName(source, file));
        }
        entry.setSize(file.length());
        entry.setTime(file.lastModified());
        zos.putNextEntry(entry);
        int readLen = 0;
        byte[] buf = new byte[2048];
        if (file.isFile()) {
            InputStream in = new BufferedInputStream(new FileInputStream(file));
            while ((readLen = in.read(buf, 0, 2048)) != -1) {
                zos.write(buf, 0, readLen);
            }
            in.close();
        }
        zos.close();
    }

    /**
	 * 给定根目录，返回另一个文件名的相对路径，用于zip文件中的路径
	 * 
	 * @param baseDir
	 * @param realFileName
	 * @return
	 */
    private static String getAbsFileName(String baseDir, File realFileName) {
        File real = realFileName;
        File base = new File(baseDir);
        String relName = real.getName();
        while (true) {
            real = real.getParentFile();
            if (real == null) break;
            if (real.equals(base)) break; else {
                relName = real.getName() + "/" + relName;
            }
        }
        return relName;
    }
}
