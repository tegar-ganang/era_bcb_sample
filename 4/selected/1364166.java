package genj.fo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.spi.ServiceRegistry;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

/**
 * A document format
 */
public abstract class Format {

    protected static final Logger LOG = Logger.getLogger("genj.fo");

    /** available formats */
    private static Format[] formats;

    /** default formats */
    public static final Format DEFAULT = new HTMLFormat();

    /** this format */
    private String format;

    /** file extension */
    private String extension;

    /** whether this format requires externalization of referenced files (imagedata) */
    private boolean isExternalizedFiles;

    /** caching for xsl templates */
    private Map<File, TemplatesCache> xslCache = new HashMap<File, TemplatesCache>();

    /** 
   * Constructor 
   */
    protected Format(String format, String extension, boolean isExternalizedFiles) {
        this.format = format;
        this.extension = extension;
        this.isExternalizedFiles = isExternalizedFiles;
    }

    /**
   * Valid file extension without dot (e.g. xml, pdf, html, fo)
   * @return file extension without dot of null if streaming output is not supported
   */
    public String getFileExtension() {
        return extension;
    }

    /**
   * Format Name 
   */
    public String getFormat() {
        return format;
    }

    /**
   * Text representation
   */
    public String toString() {
        return format;
    }

    /**
   * equals
   */
    public boolean equals(Object that) {
        return that instanceof Format ? this.format.equals(((Format) that).format) : false;
    }

    /**
   * Externalize files resolving imagedata references
   */
    private void externalizeFiles(Document doc, File out) throws IOException {
        File[] files = doc.getImages();
        if (files.length > 0) {
            File dir = new File(out.getParentFile(), out.getName() + ".images");
            if (!dir.mkdirs()) throw new IOException("cannot create directory " + dir);
            if (dir.exists()) {
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    File copy = new File(dir, file.getName());
                    FileChannel from = null, to = null;
                    long count = -1;
                    try {
                        from = new FileInputStream(file).getChannel();
                        count = from.size();
                        to = new FileOutputStream(copy).getChannel();
                        from.transferTo(0, count, to);
                        doc.setImage(file, dir.getName() + "/" + copy.getName());
                    } catch (Throwable t) {
                        LOG.log(Level.WARNING, "Copying '" + file + "' to '" + copy + "' failed (size=" + count + ")", t);
                    } finally {
                        try {
                            to.close();
                        } catch (Throwable t) {
                        }
                        try {
                            from.close();
                        } catch (Throwable t) {
                        }
                    }
                }
            }
        }
    }

    /**
   * By default all formats support all documents
   */
    public boolean supports(Document doc) {
        return true;
    }

    /**
   * Format a document
   */
    public void format(Document doc, File file) throws IOException {
        FileOutputStream out = null;
        if (getFileExtension() != null) {
            if (file == null) throw new IOException("Formatter requires output file");
            out = new FileOutputStream(file);
            if (isExternalizedFiles) externalizeFiles(doc, file);
        }
        format(doc, out);
    }

    /**
   * Format a document
   */
    public void format(Document doc, OutputStream out) throws IOException {
        doc.close();
        try {
            formatImpl(doc, out);
        } catch (Throwable t) {
            LOG.log(Level.WARNING, "unexpected expection formatting " + doc.getTitle(), t);
            if (t instanceof OutOfMemoryError) throw new IOException("out of memory");
            if (t instanceof IOException) throw (IOException) t;
            throw new IOException(t.getMessage());
        } finally {
            try {
                out.close();
            } catch (Throwable t) {
            }
        }
    }

    /**
   * Format implementation
   */
    protected abstract void formatImpl(Document doc, OutputStream out) throws Throwable;

    /**
   * Get transformation templates for given file
   */
    private class TemplatesCache {

        Templates templates;

        long timestamp;
    }

    protected Templates getTemplates(String filename) {
        File file = new File(filename);
        long lastModified = file.lastModified();
        TemplatesCache cache = (TemplatesCache) xslCache.get(file);
        if (cache != null && cache.timestamp == lastModified) return cache.templates;
        cache = new TemplatesCache();
        cache.timestamp = lastModified;
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            cache.templates = factory.newTemplates(new StreamSource(file));
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException("Exception reading templates from " + filename + ": " + e.getMessage());
        }
        xslCache.put(file, cache);
        return cache.templates;
    }

    /**
   * Return format by key
   */
    public static Format getFormat(String format) {
        Format[] fs = getFormats();
        for (int i = 0; i < fs.length; i++) {
            if (fs[i].getFormat().equals(format)) return fs[i];
        }
        return DEFAULT;
    }

    /**
   * Resolve available formats
   */
    public static Format[] getFormats() {
        if (formats != null) return formats;
        List<Format> list = new ArrayList<Format>(10);
        list.add(DEFAULT);
        Iterator<Format> it = ServiceRegistry.lookupProviders(Format.class);
        while (it.hasNext()) {
            try {
                Format f = (Format) it.next();
                if (!list.contains(f)) list.add(f);
            } catch (Throwable t) {
                if (t.getCause() != t) t = t.getCause();
                LOG.log(Level.WARNING, "Encountered exception loading Format: " + t.getMessage());
            }
        }
        formats = (Format[]) list.toArray(new Format[list.size()]);
        return formats;
    }
}
