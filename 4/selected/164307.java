package edu.columbia.hypercontent.filters;

import edu.columbia.filesystem.File;
import edu.columbia.filesystem.IDataLoader;
import edu.columbia.filesystem.FileSystemException;
import edu.columbia.filesystem.io.Utf8StreamWriter;
import edu.columbia.hypercontent.*;
import edu.columbia.hypercontent.util.CachingContentHandler;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.ext.LexicalHandler;

/**
 * Part of the Columbia University Content Management Suite
 *
 * @author Alex Vigdor av317@columbia.edu
 * @version $Revision: 1.5 $
 */
public class XmlIncludeFilter implements IPublicationFilter, Cloneable, IDataLoader {

    public static final int WITH_METADATA = 0;

    public static final int WITHOUT_METADATA = 1;

    public static final int WITH_DATA = 2;

    public static final int WITHOUT_DATA = 3;

    protected static final String CDATA = "CDATA";

    protected static final String namespace = "http://www.ais.columbia.edu/sws/xmlns/cucms#";

    protected static final AttributesImpl emptyAttributes = new AttributesImpl();

    protected static final byte[] first = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n<cms:wrapper xmlns:cms=\"http://www.ais.columbia.edu/sws/xmlns/cucms#\">".getBytes();

    protected static final byte[] last = "</cms:wrapper>".getBytes();

    protected FileHolder.Include[] includes;

    protected FileHolder source;

    protected Project project;

    protected String requestID;

    static {
        emptyAttributes.addAttribute("http://www.w3.org/2000/xmlns/", "cms", "xmlns:cms", "CDATA", namespace);
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setRequestID(String requestID) {
        this.requestID = requestID;
    }

    public XmlIncludeFilter() {
        includes = new FileHolder.Include[0];
    }

    public synchronized void addIncludes(FileHolder.Include[] includes) {
        FileHolder.Include[] newInc = new FileHolder.Include[this.includes.length + includes.length];
        int i;
        for (i = 0; i < this.includes.length; i++) {
            newInc[i] = this.includes[i];
        }
        for (int j = 0; j < includes.length; j++) {
            newInc[i + j] = includes[j];
        }
        this.includes = newInc;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public void setParameters(Map parms) {
    }

    public void filter(FileHolder holder) throws CMSException {
        source = holder.getClone();
        File input = source.getFile();
        File output = new File(input.getPath(), this, source, input.getCreator(), input.getDate());
        holder.setFile(output);
        holder.setSAXCache(new IncludesCachingContentHandler());
    }

    public InputStream getInputStream() throws FileSystemException {
        edu.columbia.filesystem.impl.InputStream2D loader = new edu.columbia.filesystem.impl.InputStream2D();
        loader.addBytes(first);
        try {
            writeXmlElement(source, "cms:source", WITH_DATA, WITH_METADATA, loader);
            for (int i = 0; i < includes.length; i++) {
                FileHolder inf = project.getFileHolder(includes[i].path, requestID);
                if ((inf != null) && !(inf.isHidden())) {
                    writeXmlElement(inf, "cms:include", includes[i].dataMode, includes[i].metadataMode, loader);
                }
            }
            loader.addBytes(last);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return loader;
    }

    private static void writeXmlElement(FileHolder holder, String tagname, int dataMode, int metadataMode, edu.columbia.filesystem.impl.InputStream2D loader) throws IOException, FileSystemException {
        File xmlfile = holder.getFile();
        String contentType = holder.getContentType();
        String fname = xmlfile.getName();
        String basename = fname;
        if (fname.lastIndexOf(".") > 0) {
            basename = fname.substring(0, fname.lastIndexOf("."));
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        Utf8StreamWriter writer = Utf8StreamWriter.getThreadLocal().setOutputStream(baos);
        writer.write("<");
        writer.write(tagname);
        writer.write(" directory=\"");
        writer.write(xmlfile.getDirectory());
        writer.write("\" path=\"");
        writer.write(xmlfile.getPath());
        writer.write("\" filename=\"");
        writer.write(fname);
        String pattern = holder.getPattern();
        if (pattern != null) {
            writer.write("\" pattern=\"");
            writer.write(pattern);
        }
        writer.write("\" basename=\"");
        writer.write(basename);
        writer.write("\" type=\"");
        writer.write(contentType);
        writer.write("\">");
        writer.close();
        loader.addBytes(baos.toByteArray());
        if ((dataMode == WITH_DATA) && (contentType != null) && (contentType.startsWith("text") || (contentType.indexOf("xml") > 0))) {
            byte[] document = holder.getDataBytes();
            int i = 0;
            while (true) {
                if ((i + 1) >= document.length) {
                    break;
                }
                if (document[i] == '<') {
                    if ((document[i + 1] != '?') && (document[i + 1] != '!')) {
                        break;
                    }
                }
                i++;
            }
            loader.addBytes(document, i, document.length - i);
        }
        if (metadataMode == WITH_METADATA) {
            loader.addBytes(holder.getMetaBytes());
        }
        baos = new ByteArrayOutputStream(32);
        writer = Utf8StreamWriter.getThreadLocal().setOutputStream(baos);
        writer.write("</");
        writer.write(tagname);
        writer.write(">");
        writer.close();
        loader.addBytes(baos.toByteArray());
    }

    protected Attributes getIncludeAttributes(FileHolder holder) {
        File xmlfile = holder.getFile();
        String contentType = holder.getContentType();
        String fname = xmlfile.getName();
        String basename = fname;
        if (fname.lastIndexOf(".") > 0) {
            basename = fname.substring(0, fname.lastIndexOf("."));
        }
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute("", "directory", "", CDATA, xmlfile.getDirectory());
        atts.addAttribute("", "path", "", CDATA, xmlfile.getPath());
        atts.addAttribute("", "filename", "", CDATA, fname);
        atts.addAttribute("", "pattern", "", CDATA, holder.getPattern());
        atts.addAttribute("", "basename", "", CDATA, basename);
        atts.addAttribute("", "type", "", CDATA, contentType);
        return atts;
    }

    protected void parseInclude(FileHolder holder, int dataMode, int metadataMode, ContentHandler conHandler, LexicalHandler lexHandler, ErrorHandler errHandler) throws Exception {
        String contentType = holder.getContentType();
        if ((dataMode == WITH_DATA) && (contentType != null) && (contentType.startsWith("text") || (contentType.indexOf("xml") > 0))) {
            holder.getSAXCache(project.getResolver()).parse(conHandler, lexHandler, errHandler, true);
        }
        if (metadataMode == WITH_METADATA) {
            holder.getMetaSAXCache(project.getResolver()).parse(conHandler, lexHandler, errHandler, true);
        }
    }

    protected class IncludesCachingContentHandler extends CachingContentHandler {

        public void parse(ContentHandler conHandler, LexicalHandler lexHandler, ErrorHandler errHandler, boolean asFragment) throws Exception {
            conHandler.startDocument();
            conHandler.startElement(namespace, "wrapper", "cms:wrapper", emptyAttributes);
            conHandler.startElement(namespace, "source", "cms:source", getIncludeAttributes(source));
            parseInclude(source, WITH_DATA, WITH_METADATA, conHandler, lexHandler, errHandler);
            conHandler.endElement(namespace, "source", "cms:source");
            for (int i = 0; i < includes.length; i++) {
                FileHolder inf = project.getFileHolder(includes[i].path, requestID);
                if ((inf != null) && !(inf.isHidden())) {
                    conHandler.startElement(namespace, "include", "cms:include", getIncludeAttributes(inf));
                    parseInclude(inf, includes[i].dataMode, includes[i].metadataMode, conHandler, lexHandler, errHandler);
                    conHandler.endElement(namespace, "include", "cms:include");
                }
            }
            conHandler.endElement(namespace, "wrapper", "cms:wrapper");
            conHandler.endDocument();
        }
    }
}
