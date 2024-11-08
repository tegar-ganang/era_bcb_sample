package org.susan.java.io;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class CompressZipWriter {

    public static void main(String args[]) throws Exception {
        compressZip("D:/work/test", "D:/work/test1.zip");
    }

    /**
	 * 压缩的主过程，从一个源目录压缩整个文件夹到目标ZIP文件
	 * @param source
	 * @param dest
	 * @throws Exception
	 */
    private static void compressZip(String source, String dest) throws Exception {
        File baseFolder = new File(source);
        if (baseFolder.exists()) {
            if (baseFolder.isDirectory()) {
                List<File> fileList = getSubFiles(new File(source));
                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dest));
                zos.setEncoding("GBK");
                ZipEntry entry = null;
                byte[] buf = new byte[2048];
                int readLen = 0;
                for (int i = 0; i < fileList.size(); i++) {
                    File file = fileList.get(i);
                    if (file.isDirectory()) {
                        entry = new ZipEntry(getAbsFileName(source, file) + "/");
                    } else {
                        entry = new ZipEntry(getAbsFileName(source, file));
                    }
                    entry.setSize(file.length());
                    entry.setTime(file.lastModified());
                    zos.putNextEntry(entry);
                    if (file.isFile()) {
                        InputStream in = new BufferedInputStream(new FileInputStream(file));
                        while ((readLen = in.read(buf, 0, 1024)) != -1) {
                            zos.write(buf, 0, readLen);
                        }
                        in.close();
                    }
                }
                zos.close();
            } else {
                throw new Exception("Can not do this operation!.");
            }
        } else {
            baseFolder.mkdirs();
            compressZip(source, dest);
        }
    }

    /**
	 * 获取一个目录下边的所有子目录和文件
	 * 
	 * @param baseDir
	 * @return
	 */
    private static List<File> getSubFiles(File baseDir) {
        List<File> fileList = new ArrayList<File>();
        File[] temp = baseDir.listFiles();
        for (int i = 0; i < temp.length; i++) {
            if (temp[i].isFile()) {
                fileList.add(temp[i]);
            }
            if (temp[i].isDirectory()) {
                fileList.add(temp[i]);
                fileList.addAll(getSubFiles(temp[i]));
            }
        }
        return fileList;
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
