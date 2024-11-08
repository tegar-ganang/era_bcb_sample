package org.exist.exiftool.xquery;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.log4j.Logger;
import org.exist.dom.BinaryDocument;
import org.exist.dom.DocumentImpl;
import org.exist.dom.QName;
import org.exist.security.PermissionDeniedException;
import org.exist.source.Source;
import org.exist.source.SourceFactory;
import org.exist.storage.NativeBroker;
import org.exist.storage.lock.Lock;
import org.exist.xmldb.XmldbURI;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.Cardinality;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.modules.ModuleUtils;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.FunctionReturnSequenceType;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.Type;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author Dulip Withanage <dulip.withanage@gmail.com>
 * @version 1.0
 */
public class MetadataFunctions extends BasicFunction {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(MetadataFunctions.class);

    public static final FunctionSignature getMetadata = new FunctionSignature(new QName("get-metadata", ExiftoolModule.NAMESPACE_URI, ExiftoolModule.PREFIX), "extracts the metadata", new SequenceType[] { new FunctionParameterSequenceType("binary", Type.ANY_URI, Cardinality.ONE, "The binary file from which to extract from") }, new FunctionReturnSequenceType(Type.DOCUMENT, Cardinality.ONE, "Extracted metadata"));

    public MetadataFunctions(XQueryContext context, FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException {
        String uri = args[0].itemAt(0).getStringValue();
        try {
            if (uri.toLowerCase().startsWith("http")) {
                return extractMetadataFromWebResource(uri);
            } else {
                XmldbURI docUri = XmldbURI.xmldbUriFor(uri);
                return extractMetadataFromLocalResource(docUri);
            }
        } catch (URISyntaxException use) {
            throw new XPathException("Could not parse document URI: " + use.getMessage(), use);
        }
    }

    private Sequence extractMetadataFromLocalResource(XmldbURI docUri) throws XPathException {
        DocumentImpl doc = null;
        try {
            doc = context.getBroker().getXMLResource(docUri, Lock.READ_LOCK);
            if (doc instanceof BinaryDocument) {
                File binaryFile = ((NativeBroker) context.getBroker()).getCollectionBinaryFileFsPath(docUri);
                if (!binaryFile.exists()) {
                    throw new XPathException("Binary Document at " + docUri.toString() + " does not exist.");
                }
                return exifToolExtract(binaryFile);
            } else {
                throw new XPathException("The binay document at " + docUri.toString() + " cannot be found.");
            }
        } catch (PermissionDeniedException pde) {
            throw new XPathException("Could not access binary document: " + pde.getMessage(), pde);
        } finally {
            if (doc != null) {
                doc.getUpdateLock().release(Lock.READ_LOCK);
            }
        }
    }

    private Sequence extractMetadataFromWebResource(String uri) throws XPathException {
        URI u;
        try {
            u = new URI(uri);
            return exifToolWebExtract(u);
        } catch (URISyntaxException ex) {
            throw new XPathException("URI syntax error" + ex.getMessage(), ex);
        }
    }

    private Sequence exifToolExtract(File binaryFile) throws XPathException {
        ExiftoolModule module = (ExiftoolModule) getParentModule();
        InputStream stdIn = null;
        ByteArrayOutputStream baos = null;
        try {
            Process p = Runtime.getRuntime().exec(module.getPerlPath() + " " + module.getExiftoolPath() + " -X -struct " + binaryFile.getAbsolutePath());
            stdIn = p.getInputStream();
            baos = new ByteArrayOutputStream();
            int read = -1;
            byte buf[] = new byte[4096];
            while ((read = stdIn.read(buf)) > -1) {
                baos.write(buf, 0, read);
            }
            p.waitFor();
            return ModuleUtils.inputSourceToXML(context, new InputSource(new ByteArrayInputStream(baos.toByteArray())));
        } catch (IOException ex) {
            throw new XPathException("Could not execute the Exiftool " + ex.getMessage(), ex);
        } catch (SAXException saxe) {
            LOG.error("exiftool returned=" + new String(baos.toByteArray()), saxe);
            throw new XPathException("Could not parse output from the Exiftool " + saxe.getMessage(), saxe);
        } catch (InterruptedException ie) {
            throw new XPathException("Could not execute the Exiftool " + ie.getMessage(), ie);
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException ioe) {
                }
            }
            if (stdIn != null) {
                try {
                    stdIn.close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    private Sequence exifToolWebExtract(URI uri) throws XPathException {
        ExiftoolModule module = (ExiftoolModule) getParentModule();
        InputStream stdIn = null;
        ByteArrayOutputStream baos = null;
        try {
            Process p = Runtime.getRuntime().exec(module.getExiftoolPath() + " -fast -X -");
            stdIn = p.getInputStream();
            OutputStream stdOut = p.getOutputStream();
            Source src = SourceFactory.getSource(context.getBroker(), null, uri.toString(), false);
            InputStream isSrc = src.getInputStream();
            int read = -1;
            byte buf[] = new byte[4096];
            while ((read = isSrc.read(buf)) > -1) {
                stdOut.write(buf, 0, read);
            }
            stdOut.flush();
            stdOut.close();
            baos = new ByteArrayOutputStream();
            read = -1;
            while ((read = stdIn.read(buf)) > -1) {
                baos.write(buf, 0, read);
            }
            p.waitFor();
            return ModuleUtils.inputSourceToXML(context, new InputSource(new ByteArrayInputStream(baos.toByteArray())));
        } catch (IOException ex) {
            throw new XPathException("Could not execute the Exiftool " + ex.getMessage(), ex);
        } catch (PermissionDeniedException pde) {
            throw new XPathException("Could not execute the Exiftool " + pde.getMessage(), pde);
        } catch (SAXException saxe) {
            LOG.error("exiftool returned=" + new String(baos.toByteArray()), saxe);
            throw new XPathException("Could not parse output from the Exiftool " + saxe.getMessage(), saxe);
        } catch (InterruptedException ie) {
            throw new XPathException("Could not execute the Exiftool " + ie.getMessage(), ie);
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException ioe) {
                }
            }
            if (stdIn != null) {
                try {
                    stdIn.close();
                } catch (IOException ioe) {
                }
            }
        }
    }
}
