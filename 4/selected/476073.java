package ge.forms.etx.ant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class FlexPrecompiler extends Task {

    private String src;

    private String out;

    public static final String KA = "ქწერტყუიოპლკჯჰგფდსაზხცვბნმჭღთჟშძჩ";

    public static final String LAT = "ØßÄÒÔÚÖÉÏÐËÊãäÂ×ÃÓÀÆáÝÅÁÍÌàÙÈÑÛÞÜ";

    public void setSrc(String src) {
        this.src = src;
    }

    public void setOut(String out) {
        this.out = out;
    }

    @Override
    public void execute() throws BuildException {
        translateDir(new File(src), new File(out));
    }

    private void translateDir(File src, File out) {
        if (!out.exists()) {
            out.mkdirs();
        }
        String[] children = src.list();
        for (String child : children) {
            if (canIgnore(child, src.getPath())) {
                continue;
            }
            File currentFile = new File(src, child);
            if (currentFile.isDirectory()) {
                File outDir = new File(out, child);
                translateDir(currentFile, outDir);
            } else if (canTranslate(child)) {
                File to = new File(out, child);
                translateFile(currentFile, to);
            } else {
                File to = new File(out, child);
                copyFile(currentFile, to);
            }
        }
    }

    public boolean canIgnore(String fileName, String parent) {
        if (fileName.startsWith(".")) {
            return true;
        }
        boolean initFolder = parent.equals(src);
        if (initFolder && "bin".equals(fileName)) {
            return true;
        }
        if (initFolder && fileName.endsWith(".cache")) {
            return true;
        }
        return false;
    }

    public boolean canTranslate(String fileName) {
        return fileName.endsWith(".mxml") || fileName.endsWith(".as") || fileName.endsWith(".properties");
    }

    public void copyFile(File from, File to) {
        try {
            InputStream in = new FileInputStream(from);
            OutputStream out = new FileOutputStream(to);
            int readCount;
            byte[] bytes = new byte[1024];
            while ((readCount = in.read(bytes)) != -1) {
                out.write(bytes, 0, readCount);
            }
            out.flush();
            in.close();
            out.close();
        } catch (Exception ex) {
            throw new BuildException(ex.getMessage(), ex);
        }
    }

    public String kaToGeo(String text) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int index = KA.indexOf(c);
            if (index != -1) {
                b.append(LAT.charAt(index));
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    private void translateFile(File from, File to) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(from));
            StringBuilder b = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                b.append(kaToGeo(line));
                b.append("\n");
            }
            reader.close();
            byte[] bytes = b.toString().getBytes("UTF-8");
            OutputStream out = new FileOutputStream(to);
            out.write(bytes);
            out.flush();
            out.close();
        } catch (Exception ex) {
            throw new BuildException(ex.toString(), ex);
        }
    }
}
