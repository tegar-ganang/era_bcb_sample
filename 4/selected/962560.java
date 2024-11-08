package com.musparke.midi.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;

/**
 * a tookit contains .
 * @author Alex Mao
 *
 */
public class FileTool {

    /**
	 * convert file size in bytes to man can read format
	 * for example: 102434231->2.43G
	 * @param l always the file length in bytes
	 * @return
	 */
    public static String byteCountToDisplaySize(long l) {
        return FileUtils.byteCountToDisplaySize(l);
    }

    /**
	 * format long data to time format.
	 * @param t microseconds of a date
	 * @return formated time in yyyy-MM-dd hh:mm:ss
	 */
    public static String formatTime(long t) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(t));
    }

    /**
	 * 
	 * @param m a time period in microseconds
	 * @return format can read, for example, 6000-> 00m:06s
	 */
    public static String convertTime(long m) {
        m = m / 1000;
        long r = 0;
        String s = "s";
        for (int i = 0; i < 3; i++) {
            r = m % 60;
            s = r + s;
            if (r < 10) s = ":0" + s; else s = ":" + s;
            m = m / 60;
        }
        return s.substring(1);
    }

    /**
	 * encoding a byte array from a given encoding, if failed, return a byte array length of zero
	 * @param s
	 * @param incoding
	 * @return
	 */
    public static byte[] getBytes(String s, String incoding) {
        byte[] ret = {};
        try {
            ret = s.getBytes(incoding);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
	 * 把一个文件夹压缩成压缩包，但是文件名不支持中文
	 * @param source 文件路径 like "E:\\a\\"
	 * @param zipfile 压缩文件完全路径
	 * @return 是否压缩成功
	 */
    public static boolean zip(String source, String zipfile) throws Exception {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipfile));
        File f = new File(source);
        doZip(out, f, "", new NoFilter());
        return false;
    }

    /**
	 * 解压压缩包文件，但是文件名不支持中文
	 * @param target 解压目标路径 "like E:\\a\\"
	 * @param zipfile 压缩包文件完全路径 "E:\\a.zip"
	 * @return 是否解压缩成功
	 * @throws IOException 
	 */
    public static boolean unZip(String target, String zipfile) throws IOException {
        ZipFile zf = new ZipFile(zipfile);
        Enumeration<? extends ZipEntry> en = zf.entries();
        while (en.hasMoreElements()) {
            ZipEntry fi = (ZipEntry) en.nextElement();
            if (fi.isDirectory()) {
                File f = new File(target + fi.getName());
                f.mkdirs();
            } else {
                InputStream in = zf.getInputStream(fi);
                FileOutputStream out = new FileOutputStream(target + fi.getName());
                byte[] buf = new byte[2048];
                int len = 0;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                out.close();
                in.close();
            }
        }
        zf.close();
        return true;
    }

    /**
	 * 专门的压缩musicxml文件为一个mxl文件
	 * 
	 * @param source
	 *            压缩文件夹路径
	 * @param mxlfile
	 *            目的压缩包路名名
	 * @return 是否压缩成功
	 */
    public static boolean zipMxl(String source, String mxlfile) throws Exception {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(mxlfile));
        File f = new File(source);
        doZip(out, f, "", new MusicXmlFileFilter());
        return false;
    }

    /**
	 * 专门的解压缩musicxml文件mxl包
	 * @param target
	 * @param mxlfile
	 * @return
	 */
    public static boolean upZipMxl(String target, String mxlfile) throws Exception {
        ZipFile zf = new ZipFile(mxlfile);
        Enumeration<? extends ZipEntry> en = zf.entries();
        List<String> subMxls = new ArrayList<String>();
        while (en.hasMoreElements()) {
            ZipEntry fi = (ZipEntry) en.nextElement();
            if (fi.isDirectory()) {
                File f = new File(target + fi.getName());
                f.mkdirs();
            } else {
                if (fi.getName().toLowerCase().endsWith("mxl")) subMxls.add(target + fi.getName());
                InputStream in = zf.getInputStream(fi);
                FileOutputStream out = new FileOutputStream(target + fi.getName());
                byte[] buf = new byte[2048];
                int len = 0;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                out.close();
                in.close();
            }
        }
        zf.close();
        for (String subMxl : subMxls) {
            String subDir = subMxl.substring(subMxl.lastIndexOf(File.separator), subMxl.lastIndexOf("."));
            upZipMxl(target + subDir, subMxl);
        }
        return true;
    }

    private static void doZip(ZipOutputStream out, File f, String base, FilenameFilter filter) throws Exception {
        if (f.isDirectory()) {
            File[] fl = f.listFiles(filter);
            out.putNextEntry(new ZipEntry(base + "/"));
            base = base.length() == 0 ? "" : base + "/";
            for (int i = 0; i < fl.length; i++) {
                doZip(out, fl[i], base + fl[i].getName(), filter);
            }
        } else {
            out.putNextEntry(new ZipEntry(base));
            FileInputStream in = new FileInputStream(f);
            int b;
            System.out.println(base);
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            in.close();
        }
    }

    private static class MusicXmlFileFilter implements FilenameFilter {

        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith("xml") || name.toLowerCase().endsWith("mxl");
        }
    }

    private static class NoFilter implements FilenameFilter {

        public boolean accept(File dir, String name) {
            return true;
        }
    }
}
