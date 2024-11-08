package net.os.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipOutputStream;

/**
 * 
 * @Description 
 * @Email eliyanfei@126.com
 * @Author foo.li
 * @Time Sep 15, 2010 11:27:10 AM
 */
public class ZipUtils {

    public static void main(String[] args) {
        try {
            zip("c:/up.png", "c:/a.zip");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void zip(InputStream is, OutputStream os) {
    }

    public static void zip(String inputFileName, OutputStream os) throws Exception {
        ZipOutputStream out = new ZipOutputStream(os);
        File f = new File(inputFileName);
        zip(out, f, f.getName());
        out.close();
    }

    public static File zip(String[] inputFileName, String outputFileName) throws Exception {
        if (inputFileName == null) {
            return null;
        }
        File f = new File(outputFileName);
        ZipOutputStream out = new ZipOutputStream(f);
        for (String fn : inputFileName) {
            File i = new File(fn);
            zip(out, i, i.getName());
        }
        out.close();
        return f;
    }

    public static void zip(String inputFileName, String outputFileName) throws Exception {
        File f = new File(outputFileName);
        File i = new File(inputFileName);
        f.getParentFile().mkdirs();
        ZipOutputStream out = new ZipOutputStream(f);
        zip(out, new File(inputFileName), i.getName());
        out.close();
    }

    public static void zip(ZipOutputStream out, File f, String base) throws Exception {
        if (f.isDirectory()) {
            File[] fl = f.listFiles();
            base = base.length() == 0 ? "" : base + File.separator;
            for (int i = 0; i < fl.length; i++) {
                zip(out, fl[i], base + fl[i].getName());
            }
        } else {
            out.putNextEntry(new org.apache.tools.zip.ZipEntry(base));
            FileInputStream in = new FileInputStream(f);
            IOUtils.copyStream(in, out);
            in.close();
        }
        Thread.sleep(10);
    }

    public static void UnZip(String filename, String dirname) throws Exception {
        UnZip(new File(filename), dirname, "GB2312");
    }

    public static void UnZip(File file, String dirname) throws Exception {
        UnZip(file, dirname, "GB2312");
    }

    @SuppressWarnings("rawtypes")
    public static void UnZip(File file, String dirname, String charset) throws Exception {
        Enumeration enu;
        ZipFile zf;
        try {
            zf = new ZipFile(file, charset);
            String parent = dirname;
            enu = zf.getEntries();
            while (enu.hasMoreElements()) {
                try {
                    ZipEntry target = (ZipEntry) enu.nextElement();
                    saveEntry(zf, target, parent);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static void saveEntry(ZipFile zf, ZipEntry target, String parentDir) throws Exception, IOException {
        try {
            if (target.isDirectory()) {
            } else {
                File file = new File(parentDir + File.separator + target.getName());
                file.getParentFile().mkdirs();
                try {
                    file.createNewFile();
                } catch (Exception e) {
                    System.out.println("�����ļ��������" + file.getAbsolutePath());
                    throw e;
                }
                InputStream in = zf.getInputStream(target);
                FileOutputStream out = new FileOutputStream(file);
                byte[] by = new byte[1024];
                int c;
                while ((c = in.read(by)) != -1) {
                    out.write(by, 0, c);
                }
                out.close();
                in.close();
            }
        } catch (Exception e) {
            throw e;
        }
        Thread.sleep(10);
    }
}
