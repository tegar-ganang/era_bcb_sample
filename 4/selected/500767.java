package gullsview;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.util.jar.*;

public class JarFilter {

    private InputStream in;

    private OutputStream out;

    private Filter filter;

    public interface Filter {

        public boolean processEntry(String name);

        public void processManifest(java.util.Map<String, String> map);

        public void addEntries(FileDumper fd) throws IOException;
    }

    public JarFilter(InputStream in, OutputStream out, Filter filter) {
        this.in = in;
        this.out = out;
        this.filter = filter;
    }

    public java.util.Map<String, String> run() throws IOException {
        JarInputStream jis = new JarInputStream(this.in);
        Manifest mf = jis.getManifest();
        Attributes atts = mf.getMainAttributes();
        java.util.Map<String, String> matts = new HashMap<String, String>();
        for (Object key : atts.keySet()) matts.put(((Attributes.Name) key).toString(), (String) atts.get(key));
        this.filter.processManifest(matts);
        final ZipOutputStream zos = new ZipOutputStream(this.out);
        zos.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
        writeManifest(matts, zos);
        JarEntry entry;
        while ((entry = jis.getNextJarEntry()) != null) {
            String name = entry.getName();
            if (!this.filter.processEntry(name)) continue;
            JarEntry newEntry = new JarEntry(name);
            zos.putNextEntry(newEntry);
            pump(jis, zos, 1024);
        }
        zos.flush();
        this.filter.addEntries(new FileDumper() {

            public void next(String path) throws IOException {
                zos.putNextEntry(new ZipEntry(path));
            }

            public void write(byte[] buffer, int offset, int length) throws IOException {
                zos.write(buffer, offset, length);
            }

            public void close() throws IOException {
            }
        });
        zos.flush();
        zos.close();
        jis.close();
        return matts;
    }

    private void pump(InputStream in, OutputStream out, int bufferSize) throws IOException {
        byte[] buffer = new byte[bufferSize];
        int count;
        while ((count = in.read(buffer, 0, buffer.length)) >= 0) out.write(buffer, 0, count);
        out.flush();
    }

    public static void writeManifest(java.util.Map<String, String> map, OutputStream out) throws IOException {
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out));
        for (Object key : map.keySet()) {
            pw.print((String) key);
            pw.print(": ");
            pw.print((String) map.get(key));
            pw.print("\r\n");
        }
        pw.flush();
    }
}
