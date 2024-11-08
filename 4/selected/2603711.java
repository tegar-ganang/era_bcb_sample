package net.sf.immc.util.file.zip;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipOutputStream;
import org.apache.tools.zip.ZipEntry;

/**
 * 压缩文件夹相关工具类
 * 
 * @author <b>oxidy</b>, Copyright &#169; 2009-2011
 * @version 0.1,2011-2-23
 */
public class ZipUtils {

    private final String outputFilename;

    public ZipUtils(String outputFilename) {
        this.outputFilename = outputFilename;
    }

    public void zip(String inputFileName) throws Exception {
        String zipFileName = outputFilename;
        zip(zipFileName, new File(inputFileName));
    }

    private void zip(String zipFileName, File inputFile) throws Exception {
        try {
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
            zip(out, inputFile, "");
            System.out.println("zip done");
            out.close();
        } catch (Exception ee) {
            ee.printStackTrace();
            throw ee;
        }
    }

    private void zip(ZipOutputStream out, File f, String base) throws Exception {
        if (f.isDirectory()) {
            out.putNextEntry(new ZipEntry(base + "/"));
            base = base.length() == 0 ? "" : base + "/";
            File[] fl = f.listFiles();
            for (int i = 0; i < fl.length; i++) {
                zip(out, fl[i], base + fl[i].getName());
            }
        } else {
            out.putNextEntry(new ZipEntry(base));
            byte[] buf = new byte[1024];
            int readLen = 0;
            FileInputStream in = new FileInputStream(f);
            BufferedInputStream is = new BufferedInputStream(in);
            while ((readLen = is.read(buf, 0, 1024)) != -1) {
                out.write(buf, 0, readLen);
            }
            is.close();
            in.close();
        }
    }
}
