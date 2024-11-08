package ch.xwr.dispo.excel.rep2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import javax.servlet.ServletContext;
import org.zkoss.spring.SpringUtil;

public class ExcelTemplate {

    public static int COL_NAME = 1;

    public static int COL_DATE = 14;

    public static int COL_MONDAY = 3;

    public static int COL_TUESDAY = 5;

    public static File copyTemplate(ServletContext context) {
        File work = null;
        try {
            File template = SpringUtil.getApplicationContext().getResource("/templates/Template_Plan.xls").getFile();
            if (!template.canRead()) {
                System.out.println("Can not Read Template");
            }
            work = File.createTempFile("xwrdispo", ".xls");
            copyFile(template, work);
        } catch (Exception e) {
        }
        return work;
    }

    private static void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }
}
