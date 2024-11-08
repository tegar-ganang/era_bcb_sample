package org.bookshare.document.beans;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.benetech.beans.error.ValidationError;
import org.benetech.beans.error.ValidationError.Severity;
import org.benetech.collections.Counter;
import org.benetech.collections.ListMap;
import org.benetech.event.EventListener;
import org.benetech.util.FileUtils;
import org.benetech.util.ViolationCollatingErrorHandler;
import org.benetech.util.ZipContents;
import org.bookshare.document.daisy.DTBook;
import org.bookshare.document.daisy.NCXNavigationFile;
import org.bookshare.document.daisy.OPFPackageFile;
import org.bookshare.document.daisy.SMILNavigationFile;
import org.jdom.Document;
import org.jdom.JDOMException;

/**
 * A document set representing either a DAISY 2005 or DAISY 2.02 document set. (TODO, should version accordingly)
 * @author Reuben Firmin
 */
public final class DAISYDocumentSet extends AbstractDocumentSet<DocumentType.DAISY> implements HasOPF, HasXML {

    private ZipContents zipContents;

    public DAISYDocumentSet(final ZipContents zipContents, final File fileBasePath, final EventListener eventListener, final ViolationCollatingErrorHandler errorHandler) throws IOException {
        this(fileBasePath, eventListener, errorHandler);
        this.zipContents = zipContents;
        for (DocumentComponent element : DocumentType.DAISY.getComponents()) {
            addFiles(element, zipContents.getFilesMatchingPattern(element.getFileRegexp()));
        }
    }

    /**
     * Construct a new daisy document set with the given basepath (i.e. where it's been unzipped).
     * @param fileBasePath The unzip location
     * @param eventListener The event listener
	 * @param errorHandler The error handler
     */
    public DAISYDocumentSet(final File fileBasePath, final EventListener eventListener, final ViolationCollatingErrorHandler errorHandler) {
        super(fileBasePath, DocumentType.DAISY, eventListener, errorHandler, true);
    }

    /**
     * {@inheritDoc}
     */
    public Document getOPFDocument() {
        return getFactory().getOPFDocument();
    }

    /**
     * Get the OPF documents associated with this set.
     * @return never null
     */
    public Document getXMLDocument() {
        return getFactory().getXMLDocument();
    }

    /**
     * {@inheritDoc}
     */
    public File getOPFFile() {
        return getFactory().getOPFFile();
    }

    /**
     * Get the XML files associated with this set.
     * @return never null
     */
    public File getXMLFile() {
        return getFactory().getXMLFile();
    }

    /**
     * The image files.
     * @return never null
     */
    public List<File> getImageFiles() {
        return getFiles(DocumentComponent.IMAGE);
    }

    /**
     * The PDF files.
     * @return never null
     */
    public List<File> getPDFFiles() {
        return getFiles(DocumentComponent.PDF);
    }

    /**
     * The NCX file.
     * @return never null
     */
    public File getSMILFile() {
        return getFile(DocumentComponent.SMIL);
    }

    /**
     * The SMIL file.
     * @return never null
     */
    public File getNCXFile() {
        return getFile(DocumentComponent.NCX);
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        final File opf = getOPFFile();
        final String name = opf.getName();
        return name.substring(0, name.lastIndexOf("."));
    }

    /**
     * Create all required documents for this document set (i.e. convert from the files to the documents)
     * @param force Whether to ignore DTD validation errors
     * @param output Whether to output violation details to the listener
     * @param reparse Whether to reparse if the documents have already been parsed
     * @throws IOException If validation or parsing fails
     */
    public void createDocuments(final boolean force, final boolean output, final boolean reparse) throws IOException {
        if (reparse || !hasBeenParsed(DocumentComponent.XML) || !hasBeenParsed(DocumentComponent.OPF)) {
            try {
                createDocuments(DocumentComponent.XML, reparse);
                createDocuments(DocumentComponent.OPF, reparse);
            } catch (JDOMException e) {
                throw new IOException(e.getMessage());
            }
            final Counter typesCounter = new Counter();
            if (output) {
                getEventListener().message("Parsing violations follow:");
            }
            for (List<ValidationError> violations : getErrorHandler().getViolations().values()) {
                for (ValidationError violation : violations) {
                    if (output) {
                        getEventListener().message(violation.toString());
                        typesCounter.increment(violation.getSeverity().toString());
                    }
                    if (violation.getSeverity().equals(Severity.ERROR)) {
                        if (!force) {
                            throw new IOException(violation.toString());
                        } else {
                            getEventListener().message("Ignoring error level violation: " + violation.toString());
                        }
                    }
                }
            }
            if (output) {
                getEventListener().message("Summary:");
                for (Severity severity : Severity.values()) {
                    getEventListener().message(severity.toString() + " : " + typesCounter.getTotal(severity.toString()));
                }
            }
        }
    }

    /**
     * Modify the XML file to have richer navigation id sets. Fix the OPF to conform to DAISY standard. Generate and
     * add the SMIL and NCX files.
     * @throws IOException If file access fails
     * @throws JDOMException If some navigation fails
     */
    public void createNavigationFiles() throws IOException, JDOMException {
        getEventListener().message("Creating navigation files");
        createDocuments(false, false, false);
        getErrorHandler().clearViolations();
        final DTBook xmlBook = new DTBook(getXMLDocument(), getXMLFile(), getEventListener(), true);
        final OPFPackageFile opf = getOPFPackageFile();
        final SMILNavigationFile smilFile = new SMILNavigationFile(getFileBasePath(), xmlBook, opf, getEventListener());
        addFiles(DocumentComponent.SMIL, smilFile.getSMILFile());
        final NCXNavigationFile ncxFile = new NCXNavigationFile(getFileBasePath(), smilFile, xmlBook, opf, getEventListener());
        addFiles(DocumentComponent.NCX, ncxFile.getNCXFile());
        opf.fix(this, getEventListener(), getErrorHandler());
    }

    /**
     * Fix the paths of the files in the documentset. This should be called as a final step, after navigation files
     * have been created (see {@link #createNavigationFiles()})
     * @throws IOException If file access fails
     * @throws JDOMException If a document couldn't be created
     */
    public void fixPaths() throws IOException, JDOMException {
        getEventListener().message("Fixing paths");
        if (!getXMLFile().getParentFile().equals(getFileBasePath())) {
            final File newFile = FileUtils.copyFileToDirectory(getXMLFile(), getFileBasePath());
            getXMLFile().delete();
            overwriteFiles(DocumentComponent.XML, newFile);
            createDocuments(DocumentComponent.XML, true);
        }
        if (!getNCXFile().getParentFile().equals(getFileBasePath())) {
            final File newFile = FileUtils.copyFileToDirectory(getNCXFile(), getFileBasePath());
            getNCXFile().delete();
            overwriteFiles(DocumentComponent.NCX, newFile);
            createDocuments(DocumentComponent.NCX, true);
        }
        if (!getSMILFile().getParentFile().equals(getFileBasePath())) {
            final File newFile = FileUtils.copyFileToDirectory(getSMILFile(), getFileBasePath());
            getSMILFile().delete();
            overwriteFiles(DocumentComponent.SMIL, getSMILFile());
            createDocuments(DocumentComponent.SMIL, true);
        }
    }

    /**
     * Whether a file is contained within the document set.
     * @param fileHref The name of the file.
     * @return True if the file is in the set
     */
    public boolean containsFile(final String fileHref) {
        return zipContents.contains(getOpfXmlRootPath() + File.separator + fileHref);
    }

    /**
     * Whether a file is contained within the document set with ignored case.
     * @param fileHref The name of the file.
     * @return True if the file is in the set
     */
    public boolean containsFileIgnoreCase(final String fileHref) {
        return zipContents.containsIgnoreCase(getOpfXmlRootPath() + File.separator + fileHref);
    }

    /**
     * Get the root path that both the OPF and XML are located in.
     * @return never null
     */
    private String getOpfXmlRootPath() {
        final File xml = getXMLFile();
        final File opf = getOPFFile();
        if (xml.getParentFile().getAbsolutePath().equals(opf.getParentFile().getAbsolutePath())) {
            return xml.getParentFile().getAbsolutePath();
        } else {
            throw new RuntimeException("xml and opf are not in the same folder");
        }
    }

    /**
     * {@inheritDoc}
     */
    public OPFPackageFile getOPFPackageFile() throws JDOMException, IOException {
        return getFactory().getOPFPackageFile();
    }

    /**
     * Other files, i.e. not XML, OPF, Image or PDF.
     * @return never null.
     */
    public List<File> getOtherFiles() {
        return zipContents.getFilesNotMatchingPattern(new String[] { DocumentComponent.OPF.getFileRegexp(), DocumentComponent.XML.getFileRegexp(), DocumentComponent.IMAGE.getFileRegexp(), DocumentComponent.NCX.getFileRegexp(), DocumentComponent.SMIL.getFileRegexp() });
    }
}
