package sk.tuke.ess.jar;

import java.io.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 *
 * Programovú reprezentáciu jar-ka zapíše do súboru alebo poľa bajtov.  
 */
public class JarFileBuilder {

    public File build(Jar jar, String dir, String name) throws IOException {
        File jarFile = new File(dir, name);
        FileOutputStream out = new FileOutputStream(jarFile);
        build(jar, out);
        return jarFile;
    }

    public File build(Jar jar, String name) throws IOException {
        return build(jar, null, name);
    }

    public byte[] build(Jar jar) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        build(jar, out);
        return out.toByteArray();
    }

    private void build(Jar jar, OutputStream out) throws IOException {
        JarOutputStream target = new JarOutputStream(out, jar.getManifest());
        try {
            writeEntries(jar.getEntries(), target);
        } finally {
            target.close();
        }
    }

    private void writeEntries(Entry[] entries, JarOutputStream target) throws IOException {
        for (Entry e : entries) {
            writeEntry(e, target);
        }
    }

    private void writeEntry(Entry entry, JarOutputStream target) throws IOException {
        InputStream source = entry.getAsStream();
        if (source != null) {
            try {
                JarEntry je = new JarEntry(entry.getName());
                target.putNextEntry(je);
                copy(source, target);
            } finally {
                target.closeEntry();
                source.close();
            }
        }
    }

    private void copy(InputStream from, OutputStream to) throws IOException {
        byte[] buffer = new byte[4096];
        for (int read = from.read(buffer); read > -1; read = from.read(buffer)) {
            to.write(buffer, 0, read);
        }
    }
}
