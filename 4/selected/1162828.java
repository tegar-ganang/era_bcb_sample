package net.sourceforge.processdash.tool.export;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.sourceforge.processdash.tool.export.jarsurf.JarData;
import net.sourceforge.processdash.tool.export.jarsurf.Main;

public class JarArchiveWriter extends ZipArchiveWriter {

    JarData jarData = new JarData();

    protected ZipOutputStream createArchiveOutputStream(OutputStream out) throws IOException {
        Manifest mf = new Manifest();
        Attributes attrs = mf.getMainAttributes();
        attrs.putValue("Manifest-Version", "1.0");
        attrs.putValue("Main-Class", Main.class.getName());
        return new JarOutputStream(out, mf);
    }

    protected void addingZipEntry(String path, String contentType) {
        jarData.setContentType(path, contentType);
        super.addingZipEntry(path, contentType);
    }

    public void finishArchive() throws IOException {
        writeJarData();
        writeJarSurfClassfiles();
        super.finishArchive();
    }

    private void writeJarData() throws IOException {
        jarData.setDefaultFile(defaultPath);
        zipOut.putNextEntry(new ZipEntry(Main.JARDATA_FILENAME.substring(1)));
        ObjectOutputStream objOut = new ObjectOutputStream(zipOut);
        objOut.writeObject(jarData);
        objOut.flush();
        zipOut.closeEntry();
    }

    private void writeJarSurfClassfiles() throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("jarsurf/classes.txt")));
        String filename;
        while ((filename = in.readLine()) != null) writeClassFile(filename);
    }

    private void writeClassFile(String filename) throws IOException {
        InputStream in = JarArchiveWriter.class.getResourceAsStream(filename);
        if (in == null) throw new FileNotFoundException(filename);
        zipOut.putNextEntry(new ZipEntry(filename.substring(1)));
        int b;
        while ((b = in.read()) != -1) zipOut.write(b);
        zipOut.closeEntry();
    }
}
