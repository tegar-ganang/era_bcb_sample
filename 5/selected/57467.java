package malgnsoft.util;

import java.io.*;
import java.util.*;
import javax.servlet.jsp.JspWriter;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.compress.archivers.zip.*;
import malgnsoft.util.Malgn;

public class Zip {

    private JspWriter out = null;

    private boolean debug = false;

    public String errMsg;

    public void setDebug(JspWriter out) {
        this.debug = true;
        this.out = out;
    }

    public void setError(String msg) {
        this.errMsg = msg;
        if (debug == true && out != null) {
            try {
                out.println("<hr>" + msg + "<hr>");
            } catch (Exception e) {
            }
        }
    }

    public boolean compress(String path, String filename) {
        String[] paths = { path };
        return compress(paths, filename, null);
    }

    public boolean compress(String path, String filename, HttpServletResponse response) {
        String[] paths = { path };
        return compress(paths, filename, response);
    }

    public boolean compress(String[] paths, String filename) {
        return compress(paths, filename, null);
    }

    private void addZipEntry(ZipArchiveOutputStream zos, File f, String filename, String folder) throws Exception {
        byte[] buf = new byte[1024];
        FileInputStream in = new FileInputStream(f);
        setError(folder + f.getName());
        zos.putArchiveEntry(new ZipArchiveEntry(folder + filename));
        int len;
        while ((len = in.read(buf)) > 0) {
            zos.write(buf, 0, len);
        }
        zos.closeArchiveEntry();
        in.close();
    }

    private void addFolder(ZipArchiveOutputStream zos, File f, String folder) throws Exception {
        File[] files = f.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                addFolder(zos, files[i], folder + files[i].getName() + "/");
            } else if (files[i].isFile()) {
                addZipEntry(zos, files[i], files[i].getName(), folder);
            }
        }
    }

    public boolean compress(String[] paths, String filename, HttpServletResponse response) {
        try {
            OutputStream os;
            if (response != null) {
                response.setContentType("application/octet-stream;");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + new String(filename.getBytes("KSC5601"), "8859_1") + "\"");
                os = response.getOutputStream();
            } else {
                os = new FileOutputStream(filename);
            }
            ZipArchiveOutputStream zos = new ZipArchiveOutputStream(os);
            for (int i = 0; i < paths.length; i++) {
                String[] arr = paths[i].split("=>");
                File f = new File(arr[0]);
                if (f.isDirectory()) addFolder(zos, f, f.getName() + "/"); else if (f.isFile()) addZipEntry(zos, f, (arr.length > 1 ? arr[1] : f.getName()), "");
            }
            zos.close();
            return true;
        } catch (Exception ex) {
            Malgn.errorLog("{Zip.zip} " + ex.getMessage());
            return false;
        }
    }

    public void copyInputStream(InputStream in, OutputStream out) throws IOException {
        if (in == null || out == null) return;
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
    }

    public boolean extract(String file, String folder) {
        return extract(new File(file), folder);
    }

    public boolean extract(File f, String folder) {
        Enumeration entries;
        ZipFile zipFile;
        try {
            zipFile = new ZipFile(f);
            entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = (ZipArchiveEntry) entries.nextElement();
                if (entry == null) continue;
                String path = folder + "/" + entry.getName().replace('\\', '/');
                if (!entry.isDirectory()) {
                    File destFile = new File(path);
                    String parent = destFile.getParent();
                    if (parent != null) {
                        File parentFile = new File(parent);
                        if (!parentFile.exists()) {
                            parentFile.mkdirs();
                        }
                    }
                    copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(destFile)));
                }
            }
            zipFile.close();
        } catch (IOException ioe) {
            this.errMsg = ioe.getMessage();
            Malgn.errorLog("{Zip.unzip} " + ioe.getMessage());
            return false;
        }
        return true;
    }
}
