package com.zwl.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipOutputStream;
import com.zwl.util.zip.AesZipFileDecrypter;
import com.zwl.util.zip.AesZipFileEncrypter;
import com.zwl.util.zip.impl.ExtZipEntry;

public class ZipUtil {

    public Boolean compress(String inFile, String outFile, String key) throws Exception {
        Boolean flag = false;
        ArrayList<String> fileList = new ArrayList<String>();
        fileList.add(inFile);
        compress(fileList, outFile, key);
        flag = true;
        return flag;
    }

    public Boolean compress(List<String> inFiles, String outFile, String key) throws Exception {
        Boolean flag = false;
        String tempFile = outFile;
        if (null != key && !"".equals(key)) {
            tempFile = tempFile + "temp";
        }
        ArrayList<String> filePathList = new ArrayList<String>();
        ArrayList<String> fileNameList = new ArrayList<String>();
        if (inFiles != null && inFiles.size() > 0) {
            File file = null;
            for (String fileStr : inFiles) {
                file = new File(fileStr);
                filePathList.add(file.getAbsolutePath());
                fileNameList.add(file.getName());
            }
            makeZip(filePathList, fileNameList, tempFile);
            if (null != key && !"".equals(key)) {
                File temp = new File(tempFile);
                AesZipFileEncrypter enc = new AesZipFileEncrypter(outFile);
                enc.addAll(temp, key);
                enc.close();
                temp.delete();
            }
        }
        flag = true;
        return flag;
    }

    public Boolean compress(String inDir, String outFile, String key, Boolean ifSubDir) throws Exception {
        System.out.println("--inDir--" + inDir);
        System.out.println("--outFile--" + outFile);
        Boolean flag = false;
        String tempFile = outFile;
        if (null != key && !"".equals(key)) {
            tempFile = tempFile + "temp";
        }
        makeZip(new File(inDir), tempFile, ifSubDir);
        if (null != key && !"".equals(key)) {
            File temp = new File(tempFile);
            AesZipFileEncrypter enc = new AesZipFileEncrypter(outFile);
            enc.addAll(temp, key);
            enc.close();
            temp.delete();
        }
        flag = true;
        return flag;
    }

    public Boolean uncompress(String inFile, String outDir, String key, Boolean ifSubDir) throws Exception {
        boolean flag = false;
        if (key == null || "".equals(key)) {
            unZip(inFile, new File(outDir));
        } else {
            AesZipFileDecrypter zipFile = new AesZipFileDecrypter(new File(inFile));
            for (ExtZipEntry entry : zipFile.getEntryList()) {
                if (!ifSubDir) {
                    String filename = entry.getName();
                    String[] strArray = filename.split(File.separator + File.separator);
                    if (strArray != null && strArray.length == 1) {
                        zipFile.extractEntry(entry, new File(outDir + File.separator + entry.getName()), key);
                    }
                } else {
                    zipFile.extractEntry(entry, new File(outDir + File.separator + entry.getName()), key);
                }
            }
        }
        flag = true;
        return flag;
    }

    private void unZip(String zipFilePath, File toUnzipFold) throws IOException {
        if (!toUnzipFold.exists()) {
            toUnzipFold.mkdirs();
        }
        ZipFile zfile = new ZipFile(zipFilePath);
        Enumeration zList = zfile.getEntries();
        byte[] buf = new byte[1024];
        File tmpfile = null;
        File tmpfold = null;
        while (zList.hasMoreElements()) {
            ZipEntry ze = (ZipEntry) zList.nextElement();
            tmpfile = new File(toUnzipFold.getAbsolutePath() + File.separator + ze.getName());
            if (ze.isDirectory()) {
                continue;
            } else {
                tmpfold = tmpfile.getParentFile();
                if (!tmpfold.exists()) {
                    tmpfold.mkdirs();
                }
                OutputStream os = new BufferedOutputStream(new FileOutputStream(tmpfile.getAbsolutePath()));
                InputStream is = new BufferedInputStream(zfile.getInputStream(ze));
                int readLen = 0;
                while ((readLen = is.read(buf, 0, 1024)) != -1) {
                    os.write(buf, 0, readLen);
                }
                is.close();
                os.close();
            }
        }
    }

    private void makeZip(File fold, String outputZipFileName) throws IOException {
        makeZip(fold, outputZipFileName, false);
    }

    private void makeZip(File fold, String outputZipFileName, boolean recursive) throws IOException {
        if (fold.exists()) {
            ArrayList<File> fileList = new ArrayList<File>();
            listAllFile(fold, fileList, recursive);
            ArrayList<String> filePathList = new ArrayList<String>();
            ArrayList<String> fileNameList = new ArrayList<String>();
            String tmpStr = null;
            int basLen = fold.getAbsolutePath().length();
            for (File f : fileList) {
                tmpStr = f.getAbsolutePath();
                filePathList.add(tmpStr);
                fileNameList.add(tmpStr.substring(basLen + 1, tmpStr.length()));
            }
            makeZip(filePathList, fileNameList, outputZipFileName);
        }
    }

    private void listAllFile(File fold, ArrayList<File> fileList, boolean recursive) {
        File[] files = fold.listFiles();
        for (File f : files) {
            if (recursive && f.isDirectory()) {
                listAllFile(f, fileList, recursive);
            } else if (!f.isDirectory()) {
                fileList.add(f);
            }
        }
    }

    private void makeZip(ArrayList<String> filePathList, ArrayList<String> fileNameList, String outputZipFileName) throws IOException {
        byte[] buf = new byte[1024];
        File outputZipFile = new File(outputZipFileName);
        File outputZipParentFile = outputZipFile.getParentFile();
        if (!outputZipParentFile.exists()) {
            outputZipParentFile.mkdirs();
        }
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outputZipFile));
        int len = -1;
        for (int i = 0; i < filePathList.size(); i++) {
            if (filePathList.get(i) != null) {
                FileInputStream in = new FileInputStream(filePathList.get(i));
                out.putNextEntry(new org.apache.tools.zip.ZipEntry(fileNameList.get(i)));
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.closeEntry();
                in.close();
            }
        }
        out.close();
    }

    public static void main(String args[]) throws Exception {
        ZipUtil z = new ZipUtil();
        z.compress("c:/aa", "c:/testsubk.zip", "123", true);
    }
}
