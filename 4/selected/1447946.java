package org.ibex.util;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class GetDep {

    public static void main(String[] s) throws Exception {
        if (s.length < 2) {
            System.out.println("usage: java " + GetDep.class.getName() + " <url> <jarfile>");
            return;
        }
        fetch(s[1], s[0]);
    }

    public static void fetch(String path, String url) throws Exception {
        InputStream is = fetch(url);
        FileOutputStream fos = new FileOutputStream(path);
        while (true) {
            byte[] buf = new byte[1024 * 16];
            int numread = is.read(buf, 0, buf.length);
            if (numread == -1) break;
            fos.write(buf, 0, numread);
        }
        fos.close();
    }

    public static InputStream fetch(String url) throws Exception {
        String scheme = url.substring(0, url.indexOf(':'));
        if (scheme.equals("zip") || scheme.equals("tgz")) {
            int bang = url.lastIndexOf('!');
            String path = url.substring(bang + 1);
            url = url.substring(url.indexOf(':') + 1, bang);
            InputStream rest = fetch(url);
            if (scheme.equals("zip")) {
                ZipInputStream zis = new ZipInputStream(rest);
                while (true) {
                    ZipEntry ze = zis.getNextEntry();
                    if (ze == null) break;
                    if (ze.getName().equals(path)) return zis;
                }
                throw new RuntimeException("could not find file within archive");
            } else {
                Tar.TarInputStream tis = new Tar.TarInputStream(new GZIPInputStream(rest));
                while (true) {
                    Tar.TarEntry te = tis.getNextEntry();
                    if (te == null) break;
                    if (te.getName().equals(path)) return tis;
                }
                throw new RuntimeException("could not find file within archive");
            }
        } else {
            URL u = new URL(url);
            return u.openConnection().getInputStream();
        }
    }
}
