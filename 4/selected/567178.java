package org.vexi.widgetdoc.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Map;
import org.ibex.util.Log;
import org.vexi.widgetdoc.Main;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.SimpleCollection;
import freemarker.template.Template;
import freemarker.template.TemplateCollectionModel;
import freemarker.template.TemplateException;

public class Util {

    public static final TemplateCollectionModel EMPTY_COLLECTION = new SimpleCollection(new ArrayList());

    public static String getFileAsString(File f) throws IOException {
        Reader fr = new BufferedReader(new FileReader(f));
        StringWriter sw = new StringWriter();
        int chr;
        while ((chr = fr.read()) != -1) {
            sw.write(chr);
        }
        fr.close();
        sw.close();
        return sw.toString();
    }

    /** Fast & simple file copy. */
    public static void copy(File source, File dest) throws IOException {
        FileChannel in = null, out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(dest).getChannel();
            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
            out.write(buf);
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }

    /**
     * Copies an <code>InputStream</code> to a file using {@link #copy(InputStream, OutputStream)}.
     * 
     * @param in stream to copy from 
     * @param outputFile file to copy to
     * @return the number of bytes copied
     * @throws IOException if an I/O error occurs (may result in partially done work)  
     * @see #copy(InputStream, OutputStream)
     */
    public static long copy(InputStream in, File outputFile) throws IOException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outputFile);
            return copy(in, out);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Copies an <code>InputStream</code> to an <code>OutputStream</code> using a local internal buffer for performance.
     * Compared to {@link #globalBufferCopy(InputStream, OutputStream)} this method allows for better
     * concurrency, but each time it is called generates a buffer which will be garbage.
     * 
     * @param in stream to copy from 
     * @param out stream to copy to
     * @return the number of bytes copied
     * @throws IOException if an I/O error occurs (may result in partially done work)  
     * @see #globalBufferCopy(InputStream, OutputStream)
     */
    public static long copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[1024];
        return copy(in, out, buf);
    }

    /**
     * Copies an <code>InputStream</code> to an <code>OutputStream</code> using the specified buffer. 
     * 
     * @param in stream to copy from 
     * @param out stream to copy to
     * @param copyBuffer buffer used for copying
     * @return the number of bytes copied
     * @throws IOException if an I/O error occurs (may result in partially done work)  
     * @see #globalBufferCopy(InputStream, OutputStream)
     * @see #copy(InputStream, OutputStream)
     */
    public static long copy(InputStream in, OutputStream out, byte[] copyBuffer) throws IOException {
        long bytesCopied = 0;
        int read = -1;
        while ((read = in.read(copyBuffer, 0, copyBuffer.length)) != -1) {
            out.write(copyBuffer, 0, read);
            bytesCopied += read;
        }
        return bytesCopied;
    }

    static Configuration cfg;

    private static Configuration getConfiguration() {
        if (cfg == null) {
            cfg = new Configuration();
            cfg.setClassForTemplateLoading(org.vexi.autodoc.templates.Resource.class, "");
            cfg.setObjectWrapper(new DefaultObjectWrapper());
        }
        return cfg;
    }

    public static void generateSimple(String templateName, String outputDirName, String outputName, Map root) throws IOException, TemplateException {
        Log.uInfo("++++ Generating", outputName);
        Template temp = getConfiguration().getTemplate(templateName);
        File outputDir = new File(outputDirName);
        File outputFile = new File(outputDir, outputName);
        outputFile.getParentFile().mkdirs();
        Writer out = new OutputStreamWriter(new FileOutputStream(outputFile));
        temp.process(root, out);
        out.flush();
    }
}
