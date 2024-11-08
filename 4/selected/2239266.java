package net.preindl.paper2bibtex.modules.readers;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import net.preindl.paper2bibtex.container.DataTransferJob;
import net.preindl.paper2bibtex.modules.IModule;
import net.preindl.paper2bibtex.modules.ModuleType;
import net.preindl.paper2bibtex.modules.ProcessException;

public class FileSystemReader implements IModule {

    public void cleanup() {
    }

    public String getFormat() {
        return "filesystem";
    }

    public ModuleType getType() {
        return ModuleType.reader;
    }

    public void initialize(Properties properties) {
    }

    public void process(DataTransferJob job) throws ProcessException {
        File f = new File(job.getJobProperties().getProperty("sourceFile"));
        FileInputStream is = null;
        ByteArrayOutputStream bos = null;
        try {
            is = new FileInputStream(f);
            bos = new ByteArrayOutputStream(512);
            copy(is, bos);
        } catch (Exception x) {
            throw new RuntimeException("!open file " + f.getAbsolutePath());
        } finally {
            close(bos);
            close(is);
        }
        job.setInputData(bos.toByteArray());
    }

    public static void copy(InputStream from, OutputStream to) {
        byte[] buf = new byte[512];
        int numRead;
        try {
            while ((numRead = from.read(buf)) != -1) to.write(buf, 0, numRead);
        } catch (IOException x) {
            throw new RuntimeException(x);
        }
    }

    public static void close(InputStream is) {
        if (is == null) return;
        try {
            is.close();
        } catch (Exception x) {
        }
    }

    public static void close(OutputStream is) {
        if (is == null) return;
        try {
            is.close();
        } catch (Exception x) {
        }
    }
}
