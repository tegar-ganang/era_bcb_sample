package org.makagiga.opendocument;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import javax.swing.table.TableModel;
import org.makagiga.commons.FS;
import org.makagiga.commons.MApplication;
import org.makagiga.commons.MDate;
import org.makagiga.commons.MZip;
import org.makagiga.commons.XMLBuilder;

/**
 * @deprecated See http://odftoolkit.openoffice.org/ instead
 */
@Deprecated
public abstract class DocumentWriter implements Closeable {

    protected ByteArrayOutputStream byteStream;

    protected Content content;

    protected Manifest manifest = new Manifest();

    protected MZip zip;

    protected Object defaultContent;

    protected PrintStream printStream;

    public DocumentWriter(final OutputStream output) throws IOException {
        init(output);
    }

    public DocumentWriter(final String path) throws IOException {
        File file = new File(path);
        String filePath = path;
        String name = file.getName();
        if (FS.getExtension(name) == null) filePath += getExtension();
        init(new FS.BufferedFileOutput(filePath));
    }

    /**
	 * @since 2.0
	 */
    public void close() throws IOException {
        endContent();
        writeManifest();
        FS.close(zip);
    }

    public Content beginDefaultContent() throws IOException {
        beginContent();
        if (this instanceof SpreadsheetWriter) {
            defaultContent = this;
            content.beginSpreadsheet();
        } else if (this instanceof TextWriter) {
            defaultContent = this;
            content.beginText();
        } else {
            defaultContent = null;
            throw new UnsupportedOperationException("Cannot detect the default content");
        }
        return content;
    }

    public abstract String getExtension();

    public abstract String getMimeType();

    /**
	 * @since 2.0
	 */
    public static void write(final String text, final OutputStream output) throws IOException {
        DocumentWriter writer = null;
        try {
            writer = new TextWriter(output);
            Content content = writer.beginDefaultContent();
            content.addTextBlock(text);
        } finally {
            FS.close(writer);
        }
    }

    /**
	 * @since 2.0
	 */
    public static void write(final String title, final TableModel model, final Content.TableModelFilter filter, final OutputStream output) throws IOException {
        DocumentWriter writer = null;
        try {
            writer = new SpreadsheetWriter(output);
            Content content = writer.beginDefaultContent();
            content.beginTable(title);
            content.addTableModel(model, filter);
            content.endTable();
        } finally {
            FS.close(writer);
        }
    }

    /**
	 * @since 2.0
	 */
    protected Content beginContent() throws IOException {
        beginEntry("content.xml", "text/xml");
        content = new Content();
        return content;
    }

    protected void beginEntry(final String path) throws IOException {
        zip.beginEntry(path);
        byteStream = new ByteArrayOutputStream(4096);
        printStream = new PrintStream(byteStream, false, "UTF8");
    }

    protected void beginEntry(final String path, final String mediaType) throws IOException {
        beginEntry(path);
        manifest.addFileEntry(mediaType, path);
    }

    /**
	 * @since 2.0
	 */
    protected void endContent() throws IOException {
        if (defaultContent instanceof SpreadsheetWriter) content.endSpreadsheet(); else if (defaultContent instanceof TextWriter) content.endText();
        content.end();
        content.save(zip.getOutputStream(), false);
        content = null;
        endEntry();
    }

    protected void endEntry() throws IOException {
        zip.copyToEntry(new ByteArrayInputStream(byteStream.toByteArray()));
        FS.close(byteStream);
        FS.close(printStream);
        byteStream = null;
        printStream = null;
    }

    protected void writeManifest() throws IOException {
        beginEntry("META-INF/manifest.xml");
        manifest.write(printStream, getMimeType());
        endEntry();
    }

    /**
	 * @since 2.0
	 */
    protected void writeMeta() throws IOException {
        beginEntry("meta.xml", "text/xml");
        XMLBuilder xml = new XMLBuilder();
        xml.beginTag("office:document-meta", "xmlns:office", "urn:oasis:names:tc:opendocument:xmlns:office:1.0", "xmlns:meta", "urn:oasis:names:tc:opendocument:xmlns:meta:1.0", "office:version", "1.0");
        xml.beginTag("office:meta");
        xml.doubleTag("meta:generator", XMLBuilder.escape(MApplication.getFullName() + "/" + MApplication.getFullVersion()));
        xml.doubleTag("meta:creation-date", XMLBuilder.escape(MDate.now().format(MDate.DEFAULT_DATE_FORMAT + "'T'" + MDate.DEFAULT_TIME_FORMAT)));
        xml.endTag("office:meta");
        xml.endTag("office:document-meta");
        xml.save(printStream, false);
        endEntry();
    }

    protected void writeMimeType() throws IOException {
        beginEntry("mimetype");
        printStream.print(getMimeType());
        endEntry();
    }

    /**
	 * @since 2.0
	 */
    protected void writeSettings() throws IOException {
        beginEntry("settings.xml", "text/xml");
        XMLBuilder xml = new XMLBuilder();
        xml.beginTag("office:document-settings", "xmlns:office", "urn:oasis:names:tc:opendocument:xmlns:office:1.0", "xmlns:config", "urn:oasis:names:tc:opendocument:xmlns:config:1.0", "office:version", "1.0");
        xml.endTag("office:document-settings");
        xml.save(printStream, false);
        endEntry();
    }

    /**
	 * @since 2.0
	 */
    protected void writeStyles() throws IOException {
        beginEntry("styles.xml", "text/xml");
        XMLBuilder xml = new XMLBuilder();
        xml.beginTag("office:document-styles", "xmlns:office", "urn:oasis:names:tc:opendocument:xmlns:office:1.0", "office:version", "1.0");
        xml.endTag("office:document-styles");
        xml.save(printStream, false);
        endEntry();
    }

    private void init(final OutputStream output) throws IOException {
        zip = MZip.write(output);
        writeMeta();
        writeMimeType();
        writeSettings();
        writeStyles();
    }
}
