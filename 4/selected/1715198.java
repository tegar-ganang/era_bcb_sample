package net.sf.reqbook.services.io;

import net.sf.reqbook.services.URIConstants;
import net.sf.reqbook.common.InternalErrorException;
import net.sf.reqbook.services.*;
import net.sf.reqbook.services.pipe.DefaultPipeHandler;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

/**
 * $Id: ExternalFilesCopierImpl.java,v 1.15 2006/02/21 10:59:11 poma Exp $
 *
 * @author Pavel Sher
 */
public class ExternalFilesCopierImpl extends DefaultPipeHandler implements ExternalFilesCopier {

    private static final String FILES_DIRNAME = "_files";

    private Set filesToCopy;

    private File outputDir;

    private URI inputFileURI;

    private Locator locator;

    private Stack baseURIs;

    private ErrorReporter errorReporter;

    private Messages messages;

    private ResourceMapper resourceMapper;

    private static final Logger logger = Logger.getLogger(ExternalFilesCopierImpl.class);

    public ExternalFilesCopierImpl() {
        filesToCopy = new HashSet(20);
        baseURIs = new Stack();
    }

    public void setOutputPath(String outputPath) {
        assert outputPath != null;
        outputDir = new File(outputPath).getAbsoluteFile();
        assert outputDir.isDirectory();
    }

    public void setErrorReporter(ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    public void setMessages(Messages messages) {
        this.messages = messages.forPrefix("FilesCopier.");
    }

    public void setResourceMapper(ResourceMapper resourceMapper) {
        this.resourceMapper = resourceMapper;
    }

    public void setDocumentLocator(Locator locator) {
        assert locator.getSystemId() != null;
        this.locator = locator;
        try {
            inputFileURI = ServicesUtil.toURI(locator.getSystemId());
            baseURIs.clear();
            baseURIs.push(inputFileURI);
            assert outputDir != null;
            resourceMapper.mapRootPath(inputFileURI, new File(outputDir, FILES_DIRNAME + File.separatorChar + "file.xml").toURI());
        } catch (InternalErrorException e) {
            errorReporter.error(ErrorReporter.FILES_COPIER_ORIGIN, messages.format("invalidInputSystemId", locator.getSystemId()));
            inputFileURI = null;
        }
        super.setDocumentLocator(locator);
    }

    public void endDocument() throws SAXException {
        copyFiles();
        super.endDocument();
    }

    private void copyFiles() {
        Iterator it = filesToCopy.iterator();
        while (it.hasNext()) {
            CopyInfo ci = (CopyInfo) it.next();
            try {
                FileUtils.copyFile(ci.fromFile, ci.toFile);
                logger.info("File copied: " + ci.fromFile.getAbsolutePath() + " ==> " + ci.toFile.getAbsolutePath());
            } catch (IOException e) {
                errorReporter.warning(ErrorReporter.FILES_COPIER_ORIGIN, messages.format("failedToCopyFile", ci.fromFile.getAbsolutePath(), ci.toFile.getAbsolutePath(), e.getMessage()));
            }
        }
    }

    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        rememberBaseURI(atts);
        if (inputFileURI != null && isTargetElement(uri, localName, atts)) {
            assert resourceMapper != null;
            String url = atts.getValue("url");
            URI base = (URI) baseURIs.peek();
            URI fileURI = base.resolve(url);
            if (fileURI != null) {
                try {
                    ResourceMapper.ResourceInfo res = resourceMapper.mapResource(inputFileURI, fileURI);
                    if (res == null) {
                        errorReporter.warning(ErrorReporter.FILES_COPIER_ORIGIN, messages.format("notHandledFileURL", url), locator.getSystemId(), locator.getLineNumber(), locator.getColumnNumber());
                    } else {
                        addCopyInfo(res);
                        AttributesImpl newAttrs = new AttributesImpl(atts);
                        newAttrs.setValue(newAttrs.getIndex("url"), FILES_DIRNAME + "/" + res.newRelativePath);
                        super.startElement(uri, localName, qName, newAttrs);
                        return;
                    }
                } catch (InvalidResourceURIException e) {
                    errorReporter.warning(ErrorReporter.FILES_COPIER_ORIGIN, messages.format("invalidFileURL", url, e.getMessage()), locator.getSystemId(), locator.getLineNumber(), locator.getColumnNumber());
                } catch (InternalErrorException e) {
                    errorReporter.warning(ErrorReporter.FILES_COPIER_ORIGIN, messages.format("failedToCopyFile2", url, e.getMessage()));
                }
            }
        }
        super.startElement(uri, localName, qName, atts);
    }

    private void rememberBaseURI(Attributes atts) {
        String base = atts.getValue("xml:base");
        if (base != null) baseURIs.push(inputFileURI.resolve(base)); else baseURIs.push(baseURIs.peek());
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        baseURIs.pop();
        super.endElement(uri, localName, qName);
    }

    private boolean isTargetElement(String uri, String localName, Attributes atts) {
        return uri.equals(URIConstants.REQBOOK_NS) && (localName.equals("image") || localName.equals("link")) && atts.getValue("url") != null;
    }

    private void addCopyInfo(ResourceMapper.ResourceInfo res) {
        try {
            filesToCopy.add(new CopyInfo(new File(ServicesUtil.toURI(res.originalPath)), new File(ServicesUtil.toURI(res.newPath))));
        } catch (InternalErrorException e) {
            logger.error(e);
        }
    }

    class CopyInfo {

        public final File fromFile;

        public final File toFile;

        public CopyInfo(File fromFile, File toFile) {
            this.fromFile = fromFile;
            this.toFile = toFile;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CopyInfo)) return false;
            final CopyInfo copyInfo = (CopyInfo) o;
            if (!fromFile.equals(copyInfo.fromFile)) return false;
            return toFile.equals(copyInfo.toFile);
        }

        public int hashCode() {
            int result;
            result = fromFile.hashCode();
            result = 29 * result + toFile.hashCode();
            return result;
        }
    }
}
