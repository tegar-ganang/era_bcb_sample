package gawky.file;

import gawky.global.Constant;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Tool {

    public static final boolean MODE_OVERWRITE = false;

    public static final boolean MODE_ATTACH = true;

    public static String regbuilder(String dummy) {
        if (dummy == null) return null;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < dummy.length(); i++) {
            switch(dummy.charAt(i)) {
                case '.':
                    buf.append("\\.");
                    break;
                case '?':
                    buf.append(".");
                    break;
                case '*':
                    buf.append(".+");
                    break;
                default:
                    buf.append(dummy.charAt(i));
                    break;
            }
        }
        return buf.toString();
    }

    public static ArrayList<String> getFiles(String path) {
        ArrayList<String> filelist = new ArrayList<String>();
        File[] files = new File(getFolder(path)).listFiles();
        String map = getFilename(path);
        map = regbuilder(map);
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().matches(map)) filelist.add(files[i].getPath().replaceAll("\\\\", "/"));
        }
        return filelist;
    }

    public static boolean createFolder(String filename) {
        try {
            String folder = filename.substring(0, Math.max(filename.lastIndexOf("/"), filename.lastIndexOf("\\")));
            return new File(folder).mkdirs();
        } catch (Exception e) {
            return false;
        }
    }

    public static void unzip(String filename) throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(filename));
        ZipInputStream zin = new ZipInputStream(in);
        ZipEntry e;
        while ((e = zin.getNextEntry()) != null) {
            FileOutputStream out = new FileOutputStream(e.getName());
            byte[] b = new byte[512];
            int len = 0;
            while ((len = zin.read(b)) != -1) {
                out.write(b, 0, len);
            }
            out.close();
        }
        zin.close();
    }

    public static void zip(String file, String target) throws IOException {
        zip(new String[] { file }, target);
    }

    public static void zip(String files[], String target) throws IOException {
        byte b[] = new byte[512];
        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(target));
        for (int i = 0; i < files.length; i++) {
            InputStream in = new FileInputStream(files[i]);
            ZipEntry e = new ZipEntry(files[i].replace(File.separatorChar, '/'));
            zout.putNextEntry(e);
            int len = 0;
            while ((len = in.read(b)) != -1) {
                zout.write(b, 0, len);
            }
            zout.closeEntry();
        }
        zout.close();
    }

    public void copy(String in, String out) throws Exception {
        copy(new File(in), new File(out));
    }

    public void copy(File in, File out) throws Exception {
        FileChannel src = new FileInputStream(in).getChannel();
        FileChannel dest = new FileOutputStream(out).getChannel();
        src.transferTo(0, src.size(), dest);
        src.close();
        dest.close();
    }

    public static class TypeFilter implements FilenameFilter {

        String type;

        public TypeFilter(String type) {
            this.type = type;
        }

        public boolean accept(File dir, String name) {
            return (name.endsWith("." + type));
        }
    }

    public static void delete(String path, String prefix) throws Exception {
        File folder = new File(path);
        String files[] = folder.list(new TypeFilter(prefix));
        for (int i = 0; i < files.length; i++) new File(folder + "/" + files[i]).delete();
    }

    public static String getFolder(String fullpath) {
        try {
            return fullpath.substring(0, fullpath.lastIndexOf("/") + 1);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getFilename(String fullpath) {
        try {
            return fullpath.substring(fullpath.lastIndexOf("/") + 1);
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] readbytearray(String filename) throws Exception {
        File f = new File(filename);
        byte[] buffer = new byte[(int) f.length()];
        InputStream in = new FileInputStream(f);
        in.read(buffer);
        in.close();
        return buffer;
    }

    public static char[] readchararray(String filename) throws Exception {
        File f = new File(filename);
        char[] buffer = new char[(int) f.length()];
        InputStreamReader in = new InputStreamReader(new FileInputStream(f), "UTF8");
        in.read(buffer);
        in.close();
        return buffer;
    }

    /**
		 * xml in Datei schreiben (Overwrite mode)
		 * @param filename
		 * @param xml
		 * @throws Exception
		 */
    public static void write(String filename, StringBuilder xml) throws Exception {
        write(filename, xml, MODE_OVERWRITE);
    }

    /**
		 * xml in Datei schreiben
		 * @param filename
		 * @param xml
		 * @param attach
		 * @throws Exception
		 */
    public static void write(String filename, StringBuilder xml, boolean attach) throws Exception {
        write(filename, xml.toString(), attach);
    }

    public static void write(String filename, String xml, boolean attach) throws Exception {
        write(filename, xml, attach, Constant.ENCODE_UTF8);
    }

    public static void write(String filename, StringBuilder xml, boolean attach, String encoding) throws Exception {
        write(filename, xml.toString(), attach, encoding);
    }

    public static void write(String filename, String xml, boolean attach, String encoding) throws Exception {
        createFolder(filename);
        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(filename, attach), encoding);
        out.write(xml);
        out.close();
    }
}
