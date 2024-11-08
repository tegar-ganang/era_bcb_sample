package org.bookshare.document.daisy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.benetech.collections.ListMap;
import org.benetech.event.EventListener;
import org.benetech.exception.WrappedException;
import org.benetech.util.FileUtils;
import org.benetech.util.ViolationCollatingErrorHandler;
import org.benetech.util.XMLParseUtils;
import org.bookshare.document.beans.DAISYDocumentSet;
import org.bookshare.document.beans.DocumentComponent;
import org.jdom.Attribute;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;

/**
 * Represents an existing OPF package file. Provides the ability to modify it, retrieve metadata, etc. Copes with OEB,
 * NIMAS and DAISY variants.
 *
 * @author Reuben Firmin
 */
public final class OPFPackageFile {

    private static final String OEB_NAMESPACE = "http://openebook.org/namespaces/oeb-package/1.0/";

    private static final String DC_NAMESPACE = "http://purl.org/dc/elements/1.1/";

    private File opfFile;

    private Document opfDocument;

    private ListMap<String, Element> elements;

    private String isbn;

    /**
	 * Wrap an existing OPF.
	 *
	 * @param opfFile The file
	 * @param opfDocument The document
	 */
    public OPFPackageFile(final File opfFile, final Document opfDocument) {
        this.opfFile = opfFile;
        this.opfDocument = opfDocument;
        this.elements = XMLParseUtils.getDescendentsAsMap(opfDocument.getRootElement());
    }

    /**
	 * Creates a new OPF.
	 *
	 * @param basePath
	 * @param name
	 */
    public OPFPackageFile(final File basePath, final String name) {
        final Element rootOpf = new Element("package");
        rootOpf.getAttributes().add(new Attribute("unique-identifier", "isbn"));
        final Element metadata = new Element("metadata");
        final Element dcmetadata = new Element("dc-metadata");
        final Element manifest = new Element("manifest");
        final Element spine = new Element("spine");
        final Element guide = new Element("guide");
        rootOpf.addContent(metadata);
        rootOpf.addContent(manifest);
        metadata.addContent(dcmetadata);
        rootOpf.addContent(spine);
        rootOpf.addContent(guide);
        this.opfDocument = new Document(rootOpf, null, basePath.getAbsolutePath() + File.separatorChar + name + ".opf");
        this.opfFile = new File(basePath.getAbsolutePath() + File.separatorChar + name + ".opf");
        try {
            FileUtils.saveDocumentToFile(this.opfDocument, this.opfFile, true);
        } catch (IOException e) {
            throw new WrappedException(e);
        }
        this.elements = XMLParseUtils.getDescendentsAsMap(opfDocument.getRootElement());
    }

    /**
	 * Return the OPFFile.
	 *
	 * @return null if it doesn't exist
	 */
    public File getFile() {
        return this.opfFile;
    }

    /**
	 * Return the manifest.
	 *
	 * @return null if it doesn't exist
	 */
    public Element getManifest() {
        return elements.getFirst("manifest");
    }

    /**
	 * Return the spine.
	 *
	 * @return null if it doesn't exist
	 */
    public Element getSpine() {
        return elements.getFirst("spine");
    }

    /**
	 * Return the guide.
	 *
	 * @return null if it doesn't exist
	 */
    public Element getGuide() {
        return elements.getFirst("guide");
    }

    /**
	 * Returns map of x-metadata and dc-metadata elements; lower case element name -> element.
	 *
	 * @return Never null
	 */
    public ListMap<String, Element> getMetadataMap() {
        final ListMap<String, Element> metadataMap = new ListMap<String, Element>();
        final Element metadata = elements.getFirst("metadata");
        if (metadata != null) {
            final List<Element> children = (List<Element>) metadata.getChildren();
            for (Element child : children) {
                final List<Element> metadataElements = (List<Element>) child.getChildren();
                for (Element metadataElement : metadataElements) {
                    metadataMap.put(metadataElement.getName().toLowerCase(), metadataElement);
                }
            }
        }
        return metadataMap;
    }

    /**
	 * Return a map of the manifest, in order. id of each element serves as the key.
	 *
	 * @return Never null
	 */
    public Map<String, Element> getManifestMap() {
        final Map<String, Element> manifestMap = new LinkedHashMap<String, Element>();
        final Element manifest = getManifest();
        if (manifest != null) {
            final List<Element> children = (List<Element>) manifest.getChildren();
            for (Element child : children) {
                final Attribute id = child.getAttribute("id");
                if (id != null) {
                    manifestMap.put(id.getValue(), child);
                }
            }
        }
        return manifestMap;
    }

    /**
	 * Return a map of the spine, in order. id of each element serves as the key. Map is ordered.
	 *
	 * @return Never null
	 */
    public Map<String, Element> getSpineMap() {
        final Map<String, Element> spineMap = new LinkedHashMap<String, Element>();
        final Element spine = getSpine();
        if (spine != null) {
            final List<Element> children = (List<Element>) spine.getChildren();
            for (Element child : children) {
                final Attribute id = child.getAttribute("idref");
                if (id != null) {
                    spineMap.put(id.getValue(), child);
                }
            }
        }
        return spineMap;
    }

    /**
	 * Make modifications to any OPF documents in the document set, to convert them from NIMAS or OEB to DAISY. Fixes
	 * location in document set if necessary.
	 *
	 * @param documentSet The DAISY document set
	 * @param listener The user feedback class
	 * @param errorHandler Error handler for parsing
	 * @throws IOException If saving the document fails
	 * @throws JDOMException If parsing is necessary and fails
	 */
    public void fix(final DAISYDocumentSet documentSet, final EventListener listener, final ViolationCollatingErrorHandler errorHandler) throws IOException, JDOMException {
        final DocType docType = new DocType("package");
        opfDocument.setDocType(docType);
        docType.setPublicID("+//ISBN 0-9673008-1-9//DTD OEB 1.2 Package//EN");
        docType.setSystemID("http://openebook.org/dtds/oeb-1.0.1/oebpkg101.dtd");
        fixFormatOfElements();
        fixPackage();
        fixMetadata();
        fixManifest(documentSet.getFile(DocumentComponent.XML).getName(), documentSet.getFile(DocumentComponent.NCX).getName(), documentSet.getFile(DocumentComponent.SMIL).getName(), opfFile.getName());
        fixSpine();
        fixGuide();
        if (!opfFile.getParentFile().equals(documentSet.getFileBasePath())) {
            opfFile = FileUtils.copyFileToDirectory(opfFile, documentSet.getFileBasePath());
            documentSet.overwriteFiles(DocumentComponent.OPF, opfFile);
        }
        FileUtils.saveDocumentToFile(opfDocument, opfFile, true);
        elements = XMLParseUtils.getDescendentsAsMap(opfDocument.getRootElement());
        listener.message("Modifying OPF File: " + opfFile.getAbsolutePath());
        documentSet.createDocuments(DocumentComponent.OPF, true);
    }

    /**
	 * Set Title, Author, and Publish to upper Case.
	 */
    private void fixFormatOfElements() {
        final Map<String, List<Element>> metaMap = getMetadataMap();
        if (metaMap.containsKey("Title")) {
            for (Element e : metaMap.get("Title")) {
                final String description = e.getText();
                if (StringUtils.upperCase(description).equals(description)) {
                    e.setText(WordUtils.capitalizeFully(description));
                }
            }
        }
        if (metaMap.containsKey("Creator")) {
            for (Element e : metaMap.get("Creator")) {
                final String description = e.getText();
                if (StringUtils.upperCase(description).equals(description)) {
                    e.setText(WordUtils.capitalizeFully(description));
                }
            }
        }
        if (metaMap.containsKey("Contributor")) {
            for (Element e : metaMap.get("Contributor")) {
                final String description = e.getText();
                if (StringUtils.upperCase(description).equals(description)) {
                    e.setText(WordUtils.capitalizeFully(description));
                }
            }
        }
        if (metaMap.containsKey("Publisher")) {
            for (Element e : metaMap.get("Publisher")) {
                final String description = e.getText();
                if (StringUtils.upperCase(description).equals(description)) {
                    e.setText(WordUtils.capitalizeFully(description));
                }
            }
        }
    }

    /**
	 * Fix the package.
	 */
    private void fixPackage() {
        final Element element = opfDocument.getRootElement();
        element.setNamespace(Namespace.getNamespace(OEB_NAMESPACE));
    }

    /**
	 * Make fixes to the DC Metadata.
	 */
    private void fixMetadata() {
        final Element metadata = elements.getFirst("metadata");
        metadata.setNamespace(Namespace.getNamespace(OEB_NAMESPACE));
        final Element dcMetadataElement = getDCMetadata();
        if (dcMetadataElement != null) {
            dcMetadataElement.getAttributes().clear();
            final List<Namespace> namespaces = dcMetadataElement.getAdditionalNamespaces();
            fixNamespace(dcMetadataElement, true);
            for (List<Element> metadataItems : XMLParseUtils.getChildrenAsMap(dcMetadataElement).values()) {
                for (Element element : metadataItems) {
                    fixNamespace(element, true);
                    if (element.getName().equals("Date")) {
                        element.getAttributes().clear();
                    }
                    if (element.getName().equals("Identifier")) {
                        if (element.getAttribute("scheme") != null && element.getAttributeValue("scheme").equals("ISBN")) {
                            final String isbn = element.getTextNormalize();
                            if (isbn.endsWith("NIMAS")) {
                                this.isbn = isbn.substring(0, isbn.indexOf("NIMAS"));
                                element.setText(isbn);
                            } else {
                                this.isbn = isbn;
                            }
                        } else {
                            final String potentialIsbn = element.getTextTrim();
                            if (potentialIsbn.length() == 10 || potentialIsbn.length() == 13) {
                                if (NumberUtils.isNumber(potentialIsbn)) {
                                    element.setAttribute("scheme", "ISBN");
                                    this.isbn = isbn;
                                }
                            }
                        }
                    }
                }
            }
            final String uid = getUID();
            if (uid != null) {
                final Element packageIdent = new Element("Identifier", "dc", DC_NAMESPACE);
                packageIdent.setAttribute("id", uid);
                packageIdent.setAttribute("scheme", "DTB");
                packageIdent.setText(uid);
                fixNamespace(packageIdent, true);
            }
        } else {
            throw new RuntimeException("No dc metadata found!");
        }
        final List<Element> format = elements.get("Format");
        if (format != null) {
            for (Element formatElement : format) {
                formatElement.setText("ANSI/NISO Z39.86-2002");
            }
        }
    }

    /**
	 * Set the namespace on this element.
	 * @param element The element whose namespace to fix
	 * @param dc Whether it needs the additional dc namespaces
	 */
    private void fixNamespace(final Element element, final boolean dc) {
        if (element.getNamespace() == null) {
            if (!dc) {
                element.setNamespace(Namespace.getNamespace(OEB_NAMESPACE));
            } else {
                element.setNamespace(Namespace.getNamespace(DC_NAMESPACE));
            }
        }
        if (dc) {
            element.addNamespaceDeclaration(Namespace.getNamespace("dc", DC_NAMESPACE));
            element.addNamespaceDeclaration(Namespace.getNamespace("oebpackage", OEB_NAMESPACE));
        }
    }

    /**
	 * Fix the package manifest.
	 *
	 * @param xmlName XML file name
	 * @param ncxName NCX file name
	 * @param smilName SMIL file name
	 * @param opfName OPF file name
	 */
    private void fixManifest(final String xmlName, final String ncxName, final String smilName, final String opfName) {
        final Element manifest = getManifest();
        fixNamespace(manifest, false);
        manifest.removeContent();
        final Element text = new Element("item", OEB_NAMESPACE);
        text.getAttributes().add(new Attribute("id", "text"));
        text.getAttributes().add(new Attribute("href", xmlName));
        text.getAttributes().add(new Attribute("media-type", "text/xml"));
        manifest.addContent(text);
        fixNamespace(text, false);
        final Element ncx = new Element("item", OEB_NAMESPACE);
        ncx.getAttributes().add(new Attribute("id", "ncx"));
        ncx.getAttributes().add(new Attribute("href", ncxName));
        ncx.getAttributes().add(new Attribute("media-type", "text/xml"));
        manifest.addContent(ncx);
        fixNamespace(ncx, false);
        final Element smil = new Element("item", OEB_NAMESPACE);
        smil.getAttributes().add(new Attribute("id", "SMIL"));
        smil.getAttributes().add(new Attribute("href", smilName));
        smil.getAttributes().add(new Attribute("media-type", "application/sml"));
        manifest.addContent(smil);
        fixNamespace(smil, false);
        final Element opf = new Element("item", OEB_NAMESPACE);
        opf.getAttributes().add(new Attribute("id", "opf"));
        opf.getAttributes().add(new Attribute("href", opfName));
        opf.getAttributes().add(new Attribute("media-type", "text/xml"));
        manifest.addContent(opf);
        fixNamespace(opf, false);
    }

    /**
	 * Fix the spine.
	 */
    private void fixSpine() {
        final Element spine = getSpine();
        fixNamespace(spine, false);
        spine.removeContent();
        final Attribute smilAttr = new Attribute("idref", "SMIL");
        final Element itemref = new Element("itemref", OEB_NAMESPACE);
        itemref.getAttributes().add(smilAttr);
        fixNamespace(itemref, false);
        spine.addContent(itemref);
    }

    /**
	 * Fix the guide.
	 */
    private void fixGuide() {
        final Element guide = getGuide();
        if (guide != null) {
            fixNamespace(guide, false);
            for (Element element : (List<Element>) guide.getChildren()) {
                fixNamespace(element, false);
            }
        }
    }

    /**
	 * Get the base file name, derived from the name of this OPF. E.g. if the OPF is named foo.opf, return foo.
	 *
	 * @return null if the name can't be derived
	 */
    public String getBaseName() {
        if (opfFile != null) {
            final String name = opfFile.getName();
            return name.substring(0, name.lastIndexOf("."));
        }
        return null;
    }

    /**
	 * The "unique-identifier" of the book if it exists, otherwise null.
	 *
	 * @return The identifier of the book.
	 */
    public String getUID() {
        if (opfDocument.getRootElement().getAttribute("unique-identifier") != null) {
            return opfDocument.getRootElement().getAttribute("unique-identifier").getValue();
        }
        return null;
    }

    /**
	 * Get the dublin core metadata element.
	 *
	 * @return Never null
	 */
    public Element getDCMetadata() {
        return elements.getFirst("dc-metadata");
    }

    /**
	 * Get the x metadata element. Creates it and adds it to the elements map if it doesn't exist.
	 *
	 * @return Never null
	 */
    public Element getXMetadata() {
        Element xmetadata = elements.getFirst("x-metadata");
        if (xmetadata == null) {
            final Element metadata = elements.getFirst("metadata");
            xmetadata = new Element("x-metadata");
            metadata.addContent(xmetadata);
            fixNamespace(xmetadata, false);
            elements.put("x-metadata", xmetadata);
        }
        return xmetadata;
    }

    /**
	 * Get an extended metadata element.
	 * @param name The name of the element
	 * @return The element, maybe null
	 */
    private Element getXMetadataElement(final String name) {
        final List<Element> metas = elements.get("meta");
        if (metas != null) {
            for (Element meta : metas) {
                if (name.equals(meta.getAttributeValue("name"))) {
                    return meta;
                }
            }
        }
        return null;
    }

    /**
	 * Get extended metadata elements.
	 * @param name The name of the element
	 * @return The element, maybe null
	 */
    private List<Element> getXMetadataElements(final String name) {
        final List<Element> metas = elements.get("meta");
        if (metas != null) {
            final List<Element> out = new ArrayList<Element>();
            for (Element meta : metas) {
                if (name.equals(meta.getAttributeValue("name"))) {
                    out.add(meta);
                }
            }
            return out;
        }
        return null;
    }

    /**
	 * Update the ISBN in this document by ensuring there is an Identifer element with the ISBN scheme with the given
	 * value. Must call {@link #update()} after updates are complete.
	 *
	 * @param ISBN If null, the method is no-op
	 */
    public void setISBN(final String ISBN) {
        if (ISBN != null) {
            if (!ISBN.equals(isbn)) {
                Element isbnEl = null;
                final List<Element> identifiers = elements.get("Identifier");
                for (Element identifier : identifiers) {
                    if (identifier.getAttribute("scheme") != null && identifier.getAttributeValue("scheme").equals("ISBN")) {
                        isbnEl = identifier;
                        break;
                    }
                }
                if (isbnEl == null) {
                    isbnEl = new Element("Identifier", "dc", DC_NAMESPACE);
                    isbnEl.setAttribute(new Attribute("scheme", "ISBN"));
                    fixNamespace(isbnEl, false);
                    getDCMetadata().addContent(isbnEl);
                }
                isbnEl.setText(ISBN);
                isbn = ISBN;
            }
        }
    }

    /**
	 * Get the ISBN.
	 *
	 * @return Possibly null.
	 */
    public String getISBN() {
        if (isbn != null) {
            return isbn;
        }
        if (elements.get("Identifier") != null) {
            for (Element identifier : elements.get("Identifier")) {
                if ((identifier.getAttribute("scheme") != null) && (identifier.getAttributeValue("scheme").equals("ISBN"))) {
                    isbn = identifier.getTextTrim();
                }
            }
        }
        return isbn;
    }

    /**
	 * Ensure the title metadata is set accordingly. Must call {@link #update()} after updates are complete.
	 *
	 * @param title If null, the method is a no-op
	 */
    public void setTitle(final String title) {
        if (title != null) {
            if (!title.equals(getTitle())) {
                Element titleEl = elements.getFirst("Title");
                if (titleEl == null) {
                    titleEl = new Element("Title", "dc", DC_NAMESPACE);
                    getDCMetadata().addContent(titleEl);
                    fixNamespace(titleEl, true);
                }
                titleEl.setText(title);
            }
        }
    }

    /**
	 * Get the Title of the book.
	 *
	 * @return null if there's no title
	 */
    public String getTitle() {
        if (elements.containsKey("Title")) {
            return elements.getFirst("Title").getTextTrim();
        }
        return null;
    }

    /**
	 * Sets the authors to this group. Must call {@link #update()} after updates are complete.
	 *
	 * @param authors list of authors of this book; if null or empty, the method is a no-op
	 */
    public void setAuthors(final List<String> authors) {
        if (authors != null && authors.size() > 0) {
            final List<Element> authorEls = elements.get("Creator");
            if (authorEls != null) {
                for (Element authorEl : authorEls) {
                    authorEl.detach();
                }
            }
            for (String author : authors) {
                final Element authorEl = new Element("Creator", "dc", DC_NAMESPACE);
                fixNamespace(authorEl, true);
                authorEl.setText(author);
                getDCMetadata().addContent(authorEl);
            }
        }
    }

    /**
	 * Get the Author of the File.
	 *
	 * @return empty list if no authors
	 */
    public List<String> getAuthors() {
        final List<String> authors = new ArrayList<String>();
        if (elements.containsKey("Creator")) {
            for (Element creator : elements.get("Creator")) {
                final String author = creator.getTextTrim();
                if (author.contains(" and ")) {
                    for (String individual : author.split(" and ")) {
                        authors.add(individual);
                    }
                } else {
                    authors.add(author);
                }
            }
        }
        return authors;
    }

    /**
	 * Adds synopsis as an extended metadata element. Must call {@link #update()} after updates are complete.
	 *
	 * @param synopsis The synopsis.
	 */
    public void setSynopsis(final String synopsis) {
        if (synopsis != null) {
            Element synopsisEl = getXMetadataElement("Synopsis");
            if (synopsisEl == null) {
                synopsisEl = new Element("meta");
                synopsisEl.setAttribute("name", "Synopsis");
                getXMetadata().addContent(synopsisEl);
                fixNamespace(synopsisEl, false);
            }
            synopsisEl.setText(synopsis);
        }
    }

    /**
	 * Get the synopsis of the book.
	 *
	 * @return Null if not set
	 */
    public String getSynopsis() {
        final Element el = getXMetadataElement("Synopsis");
        if (el != null) {
            return el.getTextTrim();
        }
        return null;
    }

    /**
	 * Overrides or sets the publisher as a dc metadata. Must call {@link #update()} after updates are complete.
	 *
	 * @param publisher The publisher name; if null, the method is a no-op.
	 */
    public void setPublisher(final String publisher) {
        if (publisher != null) {
            Element pubEl = elements.getFirst("Publisher");
            if (pubEl == null) {
                pubEl = new Element("Publisher", "dc", DC_NAMESPACE);
                getDCMetadata().addContent(pubEl);
                fixNamespace(pubEl, false);
            }
            pubEl.setText(publisher);
        }
    }

    /**
	 * Get the publisher.
	 *
	 * @return Null if not found
	 */
    public String getPublisher() {
        if (elements.containsKey("Publisher")) {
            return elements.getFirst("Publisher").getTextTrim();
        }
        return null;
    }

    /**
	 * Sets number of pages as an extended metadata element. Must call {@link #update()} after updates are complete.
	 *
	 * @param numberOfPages If null, the method is a no-op
	 */
    public void setNumberOfPages(final Integer numberOfPages) {
        if (numberOfPages != null) {
            Element numPagesEl = getXMetadataElement("NumberOfPages");
            if (numPagesEl == null) {
                numPagesEl = new Element("meta");
                numPagesEl.setAttribute("name", "NumberOfPages");
                getXMetadata().addContent(numPagesEl);
                fixNamespace(numPagesEl, false);
            }
            numPagesEl.setText(String.valueOf(numberOfPages));
        }
    }

    /**
	 * Get the number of pages.
	 *
	 * @return May be null
	 */
    public String getNumberOfPages() {
        final Element e = getXMetadataElement("NumberOfPages");
        if (e != null) {
            return e.getTextTrim();
        }
        return null;
    }

    /**
	 * Sets restrictions as a series of extended metadata elements. Must call {@link #update()} after updates are
	 * complete.
	 *
	 * @param restrictions If null or empty, the method is a no-op
	 */
    public void setRestrictions(final List<String> restrictions) {
        if (restrictions != null && restrictions.size() > 0) {
            final List<Element> restrictionsList = elements.get("Restriction");
            if (restrictionsList != null) {
                for (Element restriction : restrictionsList) {
                    restriction.detach();
                }
            }
            if (restrictions == null) {
                final Element xMetadata = getXMetadata();
                for (String restriction : restrictions) {
                    final Element restrictionEl = new Element("meta");
                    restrictionEl.setAttribute("name", "Restriction");
                    xMetadata.addContent(restrictionEl);
                    restrictionEl.setText(restriction);
                    fixNamespace(restrictionEl, false);
                }
            }
        }
    }

    /**
	 * Get the list of restrictions.
	 *
	 * @return May be null
	 */
    public List<String> getRestrictions() {
        final List<Element> e = getXMetadataElements("Restriction");
        if (e != null) {
            final List<String> out = new ArrayList<String>();
            for (Element restriction : e) {
                out.add(restriction.getTextTrim());
            }
            return out;
        }
        return null;
    }

    /**
	 * Get the copyright date.
	 *
	 * @return May be null
	 */
    public String getCopyrightDate() {
        if (elements.containsKey("Date")) {
            return elements.getFirst("Date").getTextTrim();
        }
        return null;
    }

    /**
	 * Set the copyright date.
	 *
	 * @param copyright If null, this method behaves as a no-op.
	 */
    public void setCopyrightDate(final String copyright) {
        if (copyright != null) {
            Element date = elements.getFirst("Date");
            if (date == null) {
                date = new Element("Date", "dc", DC_NAMESPACE);
                getDCMetadata().addContent(date);
                fixNamespace(date, true);
            }
            date.setText(copyright);
        }
    }

    /**
	 * Set the bookshare id.
	 * @param id The id
	 */
    public void setBookshareId(final String id) {
        final Element isbnIdent = new Element("Identifier", "dc", DC_NAMESPACE);
        isbnIdent.setAttribute("id", "uid");
        isbnIdent.setAttribute("scheme", "BKSH");
        isbnIdent.setText("Bookshare-" + id);
        fixNamespace(isbnIdent, true);
        getDCMetadata().addContent(isbnIdent);
    }

    /**
	 * Get the bookshare id.
	 * @return Null if not found
	 */
    public String getBookshareId() {
        final List<Element> identifiers = elements.get("Identifier");
        if (identifiers != null) {
            for (Element identifier : identifiers) {
                if (identifier.getAttributeValue("scheme") != null && identifier.getAttributeValue("scheme").equals("BKSH")) {
                    return identifier.getTextTrim().substring("Bookshare-".length());
                }
            }
        }
        return null;
    }

    /**
	 * Updates the file with any changes made internally, and updates internal data structures.
	 *
	 * @throws IOException If file access fails
	 */
    public void update() throws IOException {
        FileUtils.saveDocumentToFile(opfDocument, opfFile, true);
        elements = XMLParseUtils.getDescendentsAsMap(opfDocument.getRootElement());
    }

    /**
	 * Updates the location of the opfFile. Does not reparse the document.
	 * @param opfFile The new location
	 */
    public void update(final File opfFile) {
        this.opfFile = opfFile;
    }
}
