package net.itsite.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipOutputStream;

/**
 * ����ZIP��TARѹ���ļ���ʽ�Ĺ�����
 * 
 * @Company ��������Ƽ����޹�˾
 * @author QianFei Xu
 */
public class ZipUtils {

    public static final void zip(final String inputFileName, final OutputStream os) throws Exception {
        final ZipOutputStream out = new ZipOutputStream(os);
        final File f = new File(inputFileName);
        zip(out, f, f.getName());
        out.close();
    }

    public static final File zip(final String[] inputFileName, final String outputFileName) throws Exception {
        if (inputFileName == null) {
            return null;
        }
        final File f = new File(outputFileName);
        final ZipOutputStream out = new ZipOutputStream(f);
        for (final String fn : inputFileName) {
            final File i = new File(fn);
            zip(out, i, i.getName());
        }
        out.close();
        return f;
    }

    public static final void zip(final String inputFileName, final String outputFileName) throws Exception {
        final File f = new File(outputFileName);
        final File i = new File(inputFileName);
        f.getParentFile().mkdirs();
        final ZipOutputStream out = new ZipOutputStream(f);
        zip(out, new File(inputFileName), i.getName());
        out.close();
    }

    public static final void zip(final ZipOutputStream out, final File f, String base) throws Exception {
        if (f.isDirectory()) {
            final File[] fl = f.listFiles();
            base = base.length() == 0 ? "" : base + File.separator;
            for (final File element : fl) {
                zip(out, element, base + element.getName());
            }
        } else {
            out.putNextEntry(new org.apache.tools.zip.ZipEntry(base));
            final FileInputStream in = new FileInputStream(f);
            IOUtils.copyStream(in, out);
            in.close();
        }
        Thread.sleep(10);
    }

    public static Collection<File> unZip(final String filename) throws IOException {
        return unZip(new File(filename), filename + "_swap", "GB2312");
    }

    public static Collection<File> unZip(final File file) throws IOException {
        return unZip(file, file.getAbsolutePath() + "_swap", "GB2312");
    }

    public static Collection<File> unZip(final String filename, final String dirname) throws IOException {
        return unZip(new File(filename), dirname, "GB2312");
    }

    public static Collection<File> unZip(final File file, final String dirname) throws IOException {
        return unZip(file, dirname, "GB2312");
    }

    public static Collection<File> unZip(final File file, final String dirname, final String charset) throws IOException {
        @SuppressWarnings("unchecked") Enumeration enu;
        ZipFile zf;
        final Collection<File> result = new ArrayList<File>(4);
        try {
            zf = new ZipFile(file, charset);
            final String parent = dirname;
            enu = zf.getEntries();
            while (enu.hasMoreElements()) {
                try {
                    final ZipEntry target = (ZipEntry) enu.nextElement();
                    final File fsFile = saveEntry(zf, target, parent);
                    if (null != fsFile) result.add(fsFile);
                } catch (final Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        } catch (final IOException e) {
            e.printStackTrace();
            throw e;
        }
        return result;
    }

    public static final File saveEntry(final ZipFile zf, final ZipEntry target, final String parentDir) throws Exception, IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            if (!target.isDirectory()) {
                final File file = new File(parentDir + File.separator + target.getName());
                file.getParentFile().mkdirs();
                try {
                    file.createNewFile();
                } catch (final Exception e) {
                    System.out.println("�����ļ��������" + file.getAbsolutePath());
                    throw e;
                }
                in = zf.getInputStream(target);
                out = new FileOutputStream(file);
                final byte[] bytes = new byte[10240];
                int readed = -1;
                while (true) {
                    readed = in.read(bytes);
                    if (readed == -1) break;
                    out.write(bytes, 0, readed);
                }
                return file;
            }
        } catch (final Exception e) {
            throw e;
        } finally {
            IOUtils.closeIO(in, out);
        }
        return null;
    }

    public static void main(final String[] args) throws IOException {
        final long st = System.currentTimeMillis();
        final Collection<File> files = IOUtils.unZip("G:\\TDDOWNLOAD\\PowerWord2010Oxfpojieban.zip");
        System.out.println(StringsUtils.join(files, "\r\n"));
        System.out.println(DateUtils.onLineTimeInfo(st));
    }
}
