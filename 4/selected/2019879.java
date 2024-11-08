package org.rg.scanner.extractors.OOo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.sun.star.beans.PropertyValue;
import com.sun.star.document.XStorageBasedDocument;
import com.sun.star.embed.ElementModes;
import com.sun.star.embed.XStorage;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XStorable;
import com.sun.star.io.XInputStream;
import com.sun.star.io.XOutputStream;
import com.sun.star.io.XSeekable;
import com.sun.star.io.XStream;
import com.sun.star.io.XTruncate;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XCloseable;
import com.sun.star.lang.XComponent;
import org.rg.common.util.io.FileUtils;

/**
 * Use local OOo to convert local files. The file is passed into OOo giving a path name.
 * @author xliao
 */
public class LocalDocumentConverter extends AbstractDocumentConverter {

    /**
    * Logger
    */
    private static final Log LOG = LogFactory.getLog(LocalDocumentConverter.class);

    /**
    * Object used to load a document into OOo
    */
    private XComponentLoader _documentLoader = null;

    /**
    * initialize document loader
    * @throws Exception
    */
    public LocalDocumentConverter() throws Exception {
        OOoConnection connection = OOoConnectionManager.getOOoConnectionManager().getOOoConnection();
        if (connection != null) {
            _documentLoader = connection.getDocumentLoader();
        }
        if (_documentLoader == null) throw new Exception("OpenOffice.org is unavailable. Will try later.");
    }

    /**
    * @see com.rg.irobot.OOo.DocumentConverter#convertToHTML(java.lang.String)
    */
    public byte[] convertToHTML(String docpath) {
        byte[] b = null;
        try {
            XComponent document = loadDocumentAsOOoModel(docpath);
            if (document != null) {
                OOoExportFilterType filter = getHTMLFilterType(document);
                LOG.debug("Converting " + docpath);
                b = convert(document, filter);
                LOG.debug("Converted " + docpath);
            } else LOG.debug("No documented loaded");
        } catch (Exception e) {
            LOG.warn("Can't convert document at " + docpath, e);
        }
        return b;
    }

    /**
    * @see com.rg.irobot.OOo.DocumentConverter#convertToHTML(java.lang.String, java.lang.String)
    */
    public void convertToHTML(String originalUrl, String storageUrl) {
        byte[] bytes = convertToHTML(originalUrl);
        if (bytes != null) writeToFile(bytes, storageUrl);
    }

    /**
    * @see com.rg.irobot.OOo.DocumentConverter#convertToPDF(java.lang.String)
    */
    public byte[] convertToPDF(String docpath) {
        byte[] b = null;
        try {
            LOG.debug("Converting " + docpath);
            XComponent document = loadDocumentAsOOoModel(docpath);
            if (document != null) {
                OOoExportFilterType filter = getPDFFilterType(document);
                b = convert(document, filter);
                LOG.debug("Converted " + docpath);
            }
        } catch (Exception e) {
            LOG.warn("Can't convert document at " + docpath, e);
        }
        return b;
    }

    /**
    * @see com.rg.irobot.OOo.DocumentConverter#convertToPDF(java.lang.String, java.lang.String)
    */
    public void convertToPDF(String originalUrl, String storageUrl) {
        byte[] bytes = convertToPDF(originalUrl);
        if (bytes != null) writeToFile(bytes, storageUrl);
    }

    /**
    * Load a document into OOo
    * @param path is the document path
    * @return OOo internal document model
    * @throws Exception anything can happen.
    */
    private XComponent loadDocumentAsOOoModel(String path) throws Exception {
        LOG.debug("Opening " + path);
        PropertyValue[] loaderValues = createDocumentLoadingProps();
        XComponent document = null;
        File f = new File(path);
        try {
            document = _documentLoader.loadComponentFromURL(getSunURLForFile(f), "_blank", 0, loaderValues);
            LOG.debug("Opened " + path);
        } catch (final com.sun.star.lang.IllegalArgumentException iae) {
            final File temp = File.createTempFile("ooo", "tmp");
            temp.deleteOnExit();
            FileUtils.copyFile(f, temp);
            document = _documentLoader.loadComponentFromURL(getSunURLForFile(temp), "_blank", 0, loaderValues);
        }
        return document;
    }

    /**
    * Given a File object, returns an OOo-friendly URL for the file.
    * @param f the File object.
    * @return an OOo-friendly URL for the file. 
    */
    private String getSunURLForFile(final File f) {
        StringBuffer url = new StringBuffer("file:///");
        try {
            url.append(f.getCanonicalPath().replace("\\", "/"));
        } catch (Exception e) {
            url.append(f.getAbsolutePath().replace("\\", "/"));
        }
        return url.toString();
    }

    /**
    * convert document into bytes[] in a specified format defined in filter.
    * @param document the document to convert.
    * @param filter the filter type.
    * @return converted document bytes
    * @throws Exception Anything can happen.
    */
    private byte[] convert(XComponent document, OOoExportFilterType filter) throws Exception {
        XCloseable xCloseable = null;
        try {
            if (filter.getFilterName().equals("impress_html_Export")) {
                return presentationHTMLConvert(document);
            }
            XStorageBasedDocument xStorageBasedDocument = (XStorageBasedDocument) UnoRuntime.queryInterface(XStorageBasedDocument.class, document);
            XStorage xStorage = xStorageBasedDocument.getDocumentStorage();
            XStorage substorage = xStorage.openStorageElement("Versions", ElementModes.READWRITE);
            XStream stream = substorage.openStreamElement("0.0.1", ElementModes.READWRITE);
            XOutputStream os = stream.getOutputStream();
            XTruncate truncate = (XTruncate) UnoRuntime.queryInterface(XTruncate.class, os);
            truncate.truncate();
            PropertyValue[] argh = new PropertyValue[2];
            argh[0] = new PropertyValue();
            argh[0].Name = "FilterName";
            argh[0].Value = filter.getFilterName();
            argh[1] = new PropertyValue();
            argh[1].Name = "OutputStream";
            argh[1].Value = os;
            XStorable xStorable = (XStorable) UnoRuntime.queryInterface(XStorable.class, document);
            xStorable.storeToURL("private:stream", argh);
            XInputStream is = (XInputStream) UnoRuntime.queryInterface(XInputStream.class, os);
            XSeekable xSeekable = (XSeekable) UnoRuntime.queryInterface(XSeekable.class, os);
            xSeekable.seek(0);
            byte[][] t_bytes = new byte[1][(int) xSeekable.getLength()];
            is.readBytes(t_bytes, (int) xSeekable.getLength());
            return t_bytes[0];
        } finally {
            if (document != null) {
                xCloseable = (XCloseable) UnoRuntime.queryInterface(XCloseable.class, document);
                if (xCloseable != null) {
                    try {
                        xCloseable.close(false);
                    } catch (com.sun.star.util.CloseVetoException ex) {
                        XComponent xComp = (XComponent) UnoRuntime.queryInterface(XComponent.class, document);
                        xComp.dispose();
                    }
                } else {
                    XComponent xComp = (XComponent) UnoRuntime.queryInterface(XComponent.class, document);
                    xComp.dispose();
                }
                LOG.debug("Closed document");
            }
        }
    }

    /**
    * create document loading properties
    * @return properties
    */
    private PropertyValue[] createDocumentLoadingProps() {
        PropertyValue[] loaderValues = new PropertyValue[1];
        loaderValues[0] = new PropertyValue();
        loaderValues[0].Name = "Hidden";
        loaderValues[0].Value = new Boolean(true);
        return loaderValues;
    }

    /**
    * write converted bytes into a file located at path
    * @param doc document bytes
    * @param path where to write
    */
    private void writeToFile(byte[] doc, String path) {
        FileOutputStream writer = null;
        try {
            writer = new FileOutputStream(path);
            writer.write(doc);
        } catch (IOException ioe) {
            LOG.warn("Can't write to file at " + path, ioe);
        } finally {
            if (writer != null) try {
                writer.flush();
                writer.close();
            } catch (IOException ioe) {
            }
        }
    }

    /**
    * @see com.rg.irobot.OOo.DocumentConverter#convertToHTML(byte[], String)
    */
    public byte[] convertToHTML(byte[] doc, String docid) {
        return null;
    }

    /**
    * @see com.rg.irobot.OOo.DocumentConverter#convertToHTML(byte[], java.lang.String, String)
    */
    public void convertToHTML(byte[] doc, String docid, String storagepath) {
    }

    /**
    * @see com.rg.irobot.OOo.DocumentConverter#convertToPDF(byte[])
    */
    public byte[] convertToPDF(byte[] doc) {
        return null;
    }

    /**
    * @see com.rg.irobot.OOo.DocumentConverter#convertToPDF(byte[], java.lang.String)
    */
    public void convertToPDF(byte[] doc, String storagepath) {
    }

    /**
    * Usage: java LocalDocumentConverter The test is done via OOo server installed on localhost
    * @param args
    */
    public static void main(String[] args) {
        try {
            LocalDocumentConverter converter = new LocalDocumentConverter();
            converter.convertToPDF("c:/openofficetest/PlainText.txt", "c:/openofficetest/PlainText.pdf");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
