package de.intarsys.pdf.st;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import de.intarsys.pdf.cds.CDSDate;
import de.intarsys.pdf.cos.COSCatalog;
import de.intarsys.pdf.cos.COSDictionary;
import de.intarsys.pdf.cos.COSDocument;
import de.intarsys.pdf.cos.COSIndirectObject;
import de.intarsys.pdf.cos.COSInfoDict;
import de.intarsys.pdf.cos.COSName;
import de.intarsys.pdf.cos.COSObject;
import de.intarsys.pdf.cos.COSObjectKey;
import de.intarsys.pdf.cos.COSObjectWalkerDeep;
import de.intarsys.pdf.cos.COSTrailer;
import de.intarsys.pdf.cos.COSVisitorException;
import de.intarsys.pdf.crypt.AccessPermissionsFull;
import de.intarsys.pdf.crypt.COSSecurityException;
import de.intarsys.pdf.crypt.IAccessPermissions;
import de.intarsys.pdf.crypt.IAccessPermissionsSupport;
import de.intarsys.pdf.crypt.ISystemSecurityHandler;
import de.intarsys.pdf.crypt.SystemSecurityHandler;
import de.intarsys.pdf.parser.COSDocumentParser;
import de.intarsys.pdf.parser.COSLoadError;
import de.intarsys.pdf.parser.COSLoadException;
import de.intarsys.pdf.writer.COSWriter;
import de.intarsys.tools.attribute.AttributeMap;
import de.intarsys.tools.attribute.IAttributeSupport;
import de.intarsys.tools.locator.ILocator;
import de.intarsys.tools.locator.ILocatorSupport;
import de.intarsys.tools.locator.TransientLocator;
import de.intarsys.tools.message.MessageBundle;
import de.intarsys.tools.randomaccess.BufferedRandomAccess;
import de.intarsys.tools.randomaccess.IRandomAccess;
import de.intarsys.tools.stream.StreamTools;

/**
 * The most physical abstraction of a PDF document. This object handles the
 * random access representation of the PDF file.
 * <p>
 * An STDocument manages the cross ref access to data stream positions from COS
 * level objects. As such the ST and the COS package are highly interdependent.
 */
public class STDocument implements IAttributeSupport, ILocatorSupport {

    /**
	 * A counter for naming new documents
	 */
    private static int COUNTER = 0;

    /** our current fdf version number * */
    public static final STDocType DOCTYPE_FDF = new STDocType("FDF", "1.2");

    /** our current pdf version number * */
    public static final STDocType DOCTYPE_PDF = new STDocType("PDF", "1.4");

    /** The logger to be used in this package */
    private static Logger Log = PACKAGE.Log;

    /**
	 * NLS
	 */
    private static final MessageBundle Msg = PACKAGE.Messages;

    public static final String OPTION_WRITEMODEHINT = "writeModeHint";

    /**
	 * Create a new document representing the data referenced by locator.
	 * 
	 * @param locator
	 *            The locator to the documents data
	 * 
	 * @return A new document representing the data referenced by locator.
	 * @throws IOException
	 * @throws COSLoadException
	 */
    public static STDocument createFromLocator(ILocator locator) throws IOException, COSLoadException {
        return createFromLocator(locator, null);
    }

    /**
	 * Create a new document representing the data referenced by locator using
	 * <code>options</code> to fine tune creation.
	 * 
	 * @param locator
	 *            The locator to the documents data
	 * @param options
	 *            A collection of options
	 * 
	 * @return A new document representing the data referenced by locator.
	 * @throws IOException
	 * @throws COSLoadException
	 */
    public static STDocument createFromLocator(ILocator locator, Map options) throws IOException, COSLoadException {
        if (!locator.exists()) {
            throw new FileNotFoundException("'" + locator.getFullName() + "' not found");
        }
        STDocument result = new STDocument(locator);
        if (options != null) {
            for (Iterator it = options.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();
                result.setAttribute(entry.getKey(), entry.getValue());
            }
        }
        result.initializeFromLocator();
        return result;
    }

    protected static String createName(String typeName) {
        COUNTER++;
        return Msg.getString("STDocument.documentName.new", typeName, new Integer(COUNTER));
    }

    /**
	 * create a new empty pdf document.
	 * 
	 * @return A new empty pdf document
	 */
    public static STDocument createNew() {
        return createNew(DOCTYPE_PDF);
    }

    /**
	 * create a new empty document.
	 * 
	 * @return A new empty document
	 */
    public static STDocument createNew(STDocType docType) {
        STDocument doc = new STDocument();
        doc.initializeFromScratch(docType);
        return doc;
    }

    private Object accessLock = new Object();

    /**
	 * Generic attribute support
	 */
    private AttributeMap attributes;

    /**
	 * The collection of changed objects within the document since last save
	 */
    private Set changes = new HashSet();

    private boolean closed = false;

    /**
	 * Flag if this document is changed
	 */
    private boolean dirty = false;

    private COSDocument doc;

    /**
	 * The document's doc type.
	 * 
	 * <p>
	 * This value is read from the file document header.
	 * </p>
	 */
    private STDocType docType;

    /**
	 * A map of indirect objects in the document.
	 */
    private Map keyToObject = new HashMap();

    /**
	 * The locator for the document physics
	 */
    private ILocator locator;

    /**
	 * The next free COSObjectKey to use for a new indirect object
	 */
    private COSObjectKey nextKey;

    /**
	 * The parser used for this document
	 */
    private COSDocumentParser parser;

    /**
	 * The random access stream to read the documents data
	 */
    private IRandomAccess randomAccess;

    /**
	 * The security handler used for decrypting this documents content
	 */
    private ISystemSecurityHandler readSecurityHandler;

    private EnumWriteMode writeModeHint = (EnumWriteMode) EnumWriteMode.META.getDefault();

    /**
	 * The security handler used for encrypting this documents content
	 */
    private ISystemSecurityHandler writeSecurityHandler;

    /**
	 * The most recent x reference section.
	 * <p>
	 * When a new document is created or initialized from a data stream, a new
	 * empty XRef Section is always created for holding the changes to come.
	 */
    private STXRefSection xRefSection;

    /**
	 * A new empty document.
	 * <p>
	 * Use always the factory method, this is not completely initialized.
	 */
    protected STDocument() {
    }

    /**
	 * A new document bound to a locator.
	 * 
	 * @param locator
	 *            The locator to the documents data.
	 */
    protected STDocument(ILocator locator) {
        setLocator(locator);
    }

    /**
	 * Mark object as changed within this document.
	 * 
	 * @param object
	 *            The object that is new or changed
	 */
    public void addChangedReference(COSIndirectObject object) {
        setDirty(true);
        changes.add(object);
    }

    /**
	 * Add another indirect object to the document.
	 * 
	 * @param newRef
	 *            The new indirect object.
	 */
    public void addObjectReference(COSIndirectObject newRef) {
        COSObjectKey key = newRef.getKey();
        getKeyToObject().put(key, newRef);
    }

    protected void checkConsistency() throws COSLoadError {
        if (getDocType() == null) {
            throw new COSLoadError("unknown document type");
        }
        if (getDocType().isPDF()) {
            if (getXRefSection() == null) {
                throw new COSLoadError("x ref section missing");
            }
            if (getXRefSection().cosGetDict() == null) {
                throw new COSLoadError("trailer missing");
            }
        }
    }

    /**
	 * Close the document. Accessing a documents content is undefined after
	 * <code>close</code>.
	 * 
	 * @throws IOException
	 */
    public void close() throws IOException {
        synchronized (getAccessLock()) {
            if (isClosed()) {
                return;
            }
            if (getRandomAccess() != null) {
                getRandomAccess().close();
                setClosed(true);
                setRandomAccess(null);
            }
        }
    }

    /**
	 * Return a deep copy of the document. This will create a copy of the
	 * documents content. The new documents location (random access) is
	 * undefined. The objects will not preserve their key values.
	 * 
	 * @return A deep copy of this.
	 */
    public STDocument copyDeep() {
        STDocument result = STDocument.createNew();
        COSDictionary newTrailer = (COSDictionary) cosGetTrailer().copyDeep();
        newTrailer.remove(COSTrailer.DK_Prev);
        newTrailer.remove(COSTrailer.DK_Size);
        newTrailer.remove(STXRefSection.DK_XRefStm);
        ((STTrailerXRefSection) result.getXRefSection()).cosSetDict(newTrailer);
        result.readSecurityHandler = readSecurityHandler;
        result.writeSecurityHandler = writeSecurityHandler;
        String name = Msg.getString("STDocument.documentName.copyOf", getName());
        result.locator = new TransientLocator(name, getDocType().getTypeName());
        return result;
    }

    /**
	 * The documents trailer dictionary
	 * 
	 * @return The documents trailer dictionary
	 */
    public COSDictionary cosGetTrailer() {
        return getXRefSection().cosGetDict();
    }

    public STXRefSection createNewXRefSection() {
        if (getXRefSection().getOffset() != -1) {
            return getXRefSection().createSuccessor();
        }
        return getXRefSection();
    }

    /**
	 * Create a new valid key for use in the document.
	 * 
	 * @return A new valid key for use in the document.
	 */
    public COSObjectKey createObjectKey() {
        nextKey = nextKey.createNextKey();
        return nextKey;
    }

    /**
	 * Create a new random access object for the document data.
	 * 
	 * @param pLocator
	 *            The locator to the document data.
	 * @return Create a new random access object for the document data.
	 * @throws IOException
	 */
    protected IRandomAccess createRandomAccess(ILocator pLocator) throws IOException {
        if (pLocator == null) {
            return null;
        }
        IRandomAccess baseAccess = pLocator.getRandomAccess();
        BufferedRandomAccess bufferedAccess = new BufferedRandomAccess(baseAccess, 4096);
        return bufferedAccess;
    }

    /**
	 * Start a garbage collection for the receiver. In a garbage collection
	 * every indirect object currently unused (unreachable from the catalog) is
	 * removed.
	 * 
	 */
    public void garbageCollect() {
        COSObjectWalkerDeep walker = new COSObjectWalkerDeep();
        try {
            cosGetTrailer().accept(walker);
        } catch (COSVisitorException e) {
        }
        STTrailerXRefSection emptyXRefSection = new STTrailerXRefSection(this);
        COSDictionary emptyTrailer = emptyXRefSection.cosGetDict();
        emptyTrailer.addAll(cosGetTrailer());
        emptyTrailer.remove(COSTrailer.DK_Prev);
        emptyTrailer.remove(COSTrailer.DK_Size);
        emptyTrailer.remove(STXRefSection.DK_XRefStm);
        setXRefSection(emptyXRefSection);
        getKeyToObject().clear();
        getChanges().clear();
        nextKey = new COSObjectKey(0, 0);
        for (Iterator i = walker.getVisited().iterator(); i.hasNext(); ) {
            COSIndirectObject o = (COSIndirectObject) i.next();
            o.setKey(null);
            addObjectReference(o);
            o.setDirty(true);
        }
    }

    public Object getAccessLock() {
        return accessLock;
    }

    /**
	 * If a document contains a permissions dictionary, it is "pushed" to this
	 * by the parser. Otherwise the document will have full permissions set.
	 * 
	 * @return The document access permissions
	 */
    public IAccessPermissions getAccessPermissions() {
        if (getReadSecurityHandler() instanceof IAccessPermissionsSupport) {
            return ((IAccessPermissionsSupport) getReadSecurityHandler()).getAccessPermissions();
        }
        return AccessPermissionsFull.get();
    }

    public synchronized Object getAttribute(Object key) {
        if (attributes == null) {
            return null;
        }
        return attributes.get(key);
    }

    public Collection getChanges() {
        return changes;
    }

    public COSDocument getDoc() {
        return doc;
    }

    public STDocType getDocType() {
        return docType;
    }

    public int getIncrementalCount() {
        return getXRefSection().getIncrementalCount();
    }

    /**
	 * THe documents objects.
	 * 
	 * @return THe documents objects.
	 */
    protected Map getKeyToObject() {
        return keyToObject;
    }

    /**
	 * The /Linearized dictionary of the document. The /Linearized dictionary is
	 * represented by the first entry in the (logically) first XRef section.
	 * <p>
	 * Note that this method may NOT return a dictionary even if the document
	 * contains a /Linearized dictionary as the first object. This is the case
	 * when the document was linearized and was written with an incremental
	 * change so that the linearization is obsolete.
	 * 
	 * @return The valid /Linearized dictionary of the document.
	 */
    public COSDictionary getLinearizedDict() {
        int objectNumber = 0;
        Iterator it = getXRefSection().entryIterator();
        while (it.hasNext()) {
            STXRefEntry entry = (STXRefEntry) it.next();
            if (entry.getObjectNumber() != 0) {
                objectNumber = entry.getObjectNumber();
                break;
            }
        }
        try {
            COSObject result = load(objectNumber);
            if (result != null && result.asDictionary() != null) {
                COSObject version = result.asDictionary().get(COSName.constant("Linearized"));
                if (!version.isNull()) {
                    return result.asDictionary();
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    /**
	 * THe locator for the document data.
	 * 
	 * @return THe locator for the document data.
	 */
    public ILocator getLocator() {
        return locator;
    }

    /**
	 * A name for the document.
	 * <p>
	 * This is either a "local" name or the name of the locator reference if
	 * present.
	 * 
	 * @return A name for the document
	 */
    public String getName() {
        return getLocator().getLocalName();
    }

    /**
	 * The indirect object with object number objNum and generation number
	 * genNum is looked up in the document. If the indirect object is not yet
	 * available, it is created and registered.
	 * 
	 * @param key
	 * 
	 * @return The indirect object with object number objNum and generation
	 *         number genNum
	 */
    public COSIndirectObject getObjectReference(COSObjectKey key) {
        COSIndirectObject result = (COSIndirectObject) getKeyToObject().get(key);
        if (result == null) {
            result = COSIndirectObject.create(this, key);
            addObjectReference(result);
        }
        return result;
    }

    /**
	 * The parser used for decoding the document data stream.
	 * 
	 * @return The parser used for decoding the document data stream.
	 */
    public COSDocumentParser getParser() {
        return parser;
    }

    /**
	 * The random access object for the documents data. Be aware that using the
	 * IRandomAccess after it is closed will throw an IOException.
	 * 
	 * @return The random access object for the documents data.
	 */
    public IRandomAccess getRandomAccess() {
        return randomAccess;
    }

    /**
	 * The documents security handler for decrypting.
	 * 
	 * @return The documents security handler for decrypting.
	 */
    public ISystemSecurityHandler getReadSecurityHandler() {
        return readSecurityHandler;
    }

    public COSTrailer getTrailer() {
        return (COSTrailer) COSTrailer.META.createFromCos(cosGetTrailer());
    }

    /**
	 * The version of the PDF spec for this document
	 * 
	 * @return The version of the PDF spec for this document
	 */
    public String getVersion() {
        return getDocType().toString();
    }

    /**
	 * The write mode to be used when the document is written the next time. If
	 * defined this overrides any hint that is used when saving the document.
	 * The write mode is reset after each "save".
	 * 
	 * @return The write mode to be used when the document is written.
	 */
    public EnumWriteMode getWriteModeHint() {
        return writeModeHint;
    }

    /**
	 * The documents security handler for encrypting.
	 * 
	 * @return The documents security handler for encrypting.
	 */
    public ISystemSecurityHandler getWriteSecurityHandler() {
        return writeSecurityHandler;
    }

    /**
	 * The most recent STXrefSection of the document.
	 * 
	 * @return The most recent STXrefSection of the document.
	 */
    public STXRefSection getXRefSection() {
        return xRefSection;
    }

    public void incrementalGarbageCollect() {
        final Set unknown = new HashSet(getChanges());
        COSObjectWalkerDeep stripper = new COSObjectWalkerDeep(false) {

            @Override
            public Object visitFromIndirectObject(COSIndirectObject io) throws COSVisitorException {
                unknown.remove(io);
                return super.visitFromIndirectObject(io);
            }
        };
        try {
            cosGetTrailer().accept(stripper);
        } catch (COSVisitorException e) {
        }
        getChanges().removeAll(unknown);
    }

    /**
	 * Initialize the security handler context.
	 * 
	 * @throws IOException
	 * 
	 */
    protected void initEncryption() throws IOException {
        readSecurityHandler = null;
        writeSecurityHandler = null;
        try {
            readSecurityHandler = SystemSecurityHandler.createFromSt(this);
            if (readSecurityHandler != null) {
                readSecurityHandler.authenticate();
            }
        } catch (COSSecurityException e) {
            IOException ioe = new IOException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
        writeSecurityHandler = readSecurityHandler;
    }

    /**
	 * Initialize the document from its data.
	 * 
	 * @throws IOException
	 * @throws COSLoadException
	 */
    protected void initializeFromLocator() throws IOException, COSLoadException {
        parser = new COSDocumentParser(this);
        streamLoad();
    }

    /**
	 * Initialize a new empty document
	 */
    protected void initializeFromScratch(STDocType pDocType) {
        setDocType(pDocType);
        String name = createName(getDocType().getTypeName());
        locator = new TransientLocator(name, pDocType.getTypeName());
        parser = new COSDocumentParser(this);
        setXRefSection(new STTrailerXRefSection(this));
        nextKey = new COSObjectKey(0, 0);
        cosGetTrailer().put(COSTrailer.DK_Root, COSCatalog.META.createNew().cosGetDict());
        setDirty(true);
    }

    public boolean isClosed() {
        return closed;
    }

    /**
	 * <code>true</code> if this has been changed.
	 * 
	 * @return <code>true</code> if this has been changed.
	 */
    public boolean isDirty() {
        return dirty;
    }

    /**
	 * @return if the document has an {@link ISystemSecurityHandler}
	 */
    public boolean isEncrypted() {
        return getReadSecurityHandler() != null;
    }

    /**
	 * <code>true</code> if this document is linearized.
	 * <p>
	 * When linearized reading is truly implemented, this check should be made
	 * using the document length instead for performance reasons.
	 * 
	 * @return <code>true</code> if this document is linearized.
	 */
    public boolean isLinearized() {
        return getLinearizedDict() != null;
    }

    public boolean isNew() {
        return (getXRefSection().getOffset() == -1) && (getXRefSection().getPrevious() == null);
    }

    /**
	 * <code>true</code> if this is read only.
	 * 
	 * @return <code>true</code> if this is read only.
	 */
    public boolean isReadOnly() {
        return (getRandomAccess() == null) || getRandomAccess().isReadOnly();
    }

    /**
	 * <code>true</code> if this has only streamed xref sections.
	 * 
	 * @return <code>true</code> if this has only streamed xref sections.
	 */
    public boolean isStreamed() {
        if (getXRefSection() != null) {
            return getXRefSection().isStreamed();
        }
        return false;
    }

    /**
	 * Load a COSObject from the documents data.
	 * 
	 * @param ref
	 *            The object reference to be loaded.
	 * @throws IOException
	 * @throws COSLoadException
	 */
    public COSObject load(COSIndirectObject ref) throws IOException, COSLoadException {
        int objectNumber = ref.getKey().getObjectNumber();
        return load(objectNumber);
    }

    protected COSObject load(int objectNumber) throws IOException, COSLoadException {
        synchronized (getAccessLock()) {
            return getXRefSection().load(objectNumber, getReadSecurityHandler());
        }
    }

    public void loadAll() throws IOException, COSLoadException {
        synchronized (getAccessLock()) {
            for (int i = 0; i < getXRefSection().getSize(); i++) {
                getXRefSection().load(i, getReadSecurityHandler());
            }
        }
    }

    /**
	 * The number of objects currently loaded.
	 * 
	 * @return The number of objects currently loaded.
	 */
    public int loadedSize() {
        int result = 0;
        for (Iterator it = getKeyToObject().values().iterator(); it.hasNext(); ) {
            COSIndirectObject ref = (COSIndirectObject) it.next();
            if (!ref.isSwapped()) {
                result++;
            }
        }
        return result;
    }

    /**
	 * An iterator on the indirect objects of the storage layer document. This
	 * includes garbage and purely technical objects like x ref streams.
	 * 
	 * @return An iterator on the indirect objects of the storage layer
	 *         document. This includes garbage and purely technical objects like
	 *         x ref streams.
	 */
    public Iterator objects() {
        return new Iterator() {

            int i = 1;

            public boolean hasNext() {
                return i < getXRefSection().getSize();
            }

            public Object next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("");
                }
                COSObjectKey key = new COSObjectKey(i++, 0);
                return getObjectReference(key);
            }

            public void remove() {
                throw new UnsupportedOperationException("remove not supported");
            }
        };
    }

    /**
	 * @throws IOException
	 * 
	 */
    protected void open() throws IOException {
        synchronized (getAccessLock()) {
            if ((randomAccess != null) && !isClosed()) {
                throw new IllegalStateException("can't open an open document");
            }
            setRandomAccess(createRandomAccess(getLocator()));
        }
    }

    public synchronized Object removeAttribute(Object key) {
        if (attributes != null) {
            return attributes.remove(key);
        }
        return null;
    }

    /**
	 * Reparses the XREF sections without actually instantiating. Used for
	 * collecting errors on XREF level
	 * 
	 * @throws IOException
	 * @throws COSLoadException
	 */
    public void reparseFromLocator() throws IOException, COSLoadException {
        synchronized (getAccessLock()) {
            int offset = getParser().searchLastStartXRef(getRandomAccess());
            AbstractXRefParser xRefParser;
            if (getParser().isTokenXRefAt(getRandomAccess(), offset)) {
                xRefParser = new XRefTrailerParser(this, getParser());
            } else {
                xRefParser = new XRefStreamParser(this, getParser());
            }
            getRandomAccess().seek(offset);
            xRefParser.parse(getRandomAccess());
        }
    }

    /**
	 * Assign a new locator to the document.
	 * <p>
	 * The documents data is completely copied to the new location.
	 * 
	 * @param newLocator
	 *            The new locator for the documents data.
	 * 
	 * @throws IOException
	 */
    protected void replaceLocator(ILocator newLocator) throws IOException {
        synchronized (getAccessLock()) {
            if (newLocator.equals(getLocator())) {
                return;
            }
            ILocator oldLocator = getLocator();
            IRandomAccess oldRandomAccess = getRandomAccess();
            try {
                setLocator(newLocator);
                setRandomAccess(null);
                open();
                IRandomAccess newRandomAccess = getRandomAccess();
                if (newRandomAccess.isReadOnly()) {
                    throw new FileNotFoundException();
                }
                if (newRandomAccess.getLength() > 0) {
                    newRandomAccess.setLength(0);
                }
                if (oldRandomAccess != null) {
                    InputStream is = oldRandomAccess.asInputStream();
                    OutputStream os = newRandomAccess.asOutputStream();
                    oldRandomAccess.seek(0);
                    StreamTools.copyStream(is, false, os, false);
                }
                StreamTools.close(oldRandomAccess);
            } catch (Exception e) {
                StreamTools.close(getRandomAccess());
                setLocator(oldLocator);
                setRandomAccess(oldRandomAccess);
            }
        }
    }

    public void restore(ILocator newLocator) throws IOException, COSLoadException {
        synchronized (getAccessLock()) {
            if (newLocator.equals(getLocator())) {
                return;
            }
            IRandomAccess oldRandomAccess = getRandomAccess();
            StreamTools.close(oldRandomAccess);
            setRandomAccess(null);
            setLocator(newLocator);
            changes.clear();
            keyToObject.clear();
            closed = false;
            dirty = false;
            streamLoad();
        }
        getDoc().triggerChangedAll();
    }

    public void save() throws IOException {
        save(getLocator(), null);
    }

    public void save(ILocator pLocator) throws IOException {
        save(pLocator, null);
    }

    public void save(ILocator pLocator, Map options) throws IOException {
        if (options == null) {
            options = new HashMap();
        }
        if ((pLocator != null) && (pLocator != getLocator())) {
            replaceLocator(pLocator);
        }
        boolean incremental = true;
        EnumWriteMode writeMode = doc.getWriteModeHint();
        doc.setWriteModeHint(EnumWriteMode.UNDEFINED);
        if (writeMode.isUndefined()) {
            Object tempHint = options.get(OPTION_WRITEMODEHINT);
            if (tempHint instanceof EnumWriteMode) {
                writeMode = (EnumWriteMode) tempHint;
            }
        }
        if (writeMode.isFull()) {
            incremental = false;
        }
        IRandomAccess tempRandomAccess = getRandomAccess();
        if (tempRandomAccess == null) {
            throw new IOException("nowhere to write to");
        }
        if (tempRandomAccess.isReadOnly()) {
            throw new FileNotFoundException("destination is read only");
        }
        COSWriter writer = new COSWriter(tempRandomAccess, getWriteSecurityHandler());
        writer.setIncremental(incremental);
        writer.writeDocument(this);
        readSecurityHandler = writeSecurityHandler;
    }

    public synchronized Object setAttribute(Object key, Object value) {
        if (attributes == null) {
            attributes = new AttributeMap();
        }
        return attributes.put(key, value);
    }

    protected void setClosed(boolean closed) {
        this.closed = closed;
    }

    /**
	 * Set the change flag of this.
	 * 
	 * @param dirty
	 *            <code>true</code> if this should be marked as changed
	 */
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
        if (!dirty) {
            changes.clear();
        }
    }

    public void setDoc(COSDocument doc) {
        this.doc = doc;
        getXRefSection().setCOSDoc(getDoc());
    }

    protected void setDocType(STDocType docType) {
        this.docType = docType;
    }

    protected void setLocator(ILocator locator) {
        this.locator = locator;
    }

    /**
	 * Rename the document locally.
	 * <p>
	 * This has no effect if a locator is present.
	 * 
	 * @param name
	 *            The new local name of this
	 */
    public void setName(String name) {
        if (getLocator() instanceof TransientLocator) {
            ((TransientLocator) getLocator()).setLocalName(name);
        }
    }

    /**
	 * Assign the {@link IRandomAccess} to the raw data.
	 * 
	 * @param randomAccess
	 *            the {@link IRandomAccess} to the raw data.
	 */
    protected void setRandomAccess(IRandomAccess randomAccess) {
        this.randomAccess = randomAccess;
    }

    /**
	 * Set the ISystemSecurityHandler in order to change document's encryption.
	 * 
	 * @param handler
	 *            the ISystemSecurityHandler to the documents data.
	 * @throws COSSecurityException
	 */
    public void setSystemSecurityHandler(ISystemSecurityHandler handler) throws COSSecurityException {
        if (writeSecurityHandler != null) {
            writeSecurityHandler.detach(this);
        }
        this.writeSecurityHandler = handler;
        if (writeSecurityHandler != null) {
            writeSecurityHandler.attach(this);
        }
    }

    /**
	 * The write mode to be used when the document is written the next time. If
	 * defined this overrides any hint that is used when saving the document.
	 * The write mode is reset after each "save".
	 * 
	 * @param writeMode
	 *            The write mode to be used when the document is written.
	 */
    public void setWriteModeHint(EnumWriteMode writeMode) {
        if (writeMode == null) {
            throw new IllegalArgumentException("write mode can't be null");
        }
        this.writeModeHint = writeMode;
    }

    /**
	 * Attach the most recent x ref section to the document.
	 * 
	 * @param xRefSection
	 *            The x ref section representing the most recent document
	 *            changes.
	 */
    public void setXRefSection(STXRefSection xRefSection) {
        this.xRefSection = xRefSection;
        if (getDoc() != null) {
            getXRefSection().setCOSDoc(getDoc());
        }
    }

    protected void streamLoad() throws IOException, COSLoadException {
        try {
            open();
            STXRefSection initialXRefSection;
            setDocType(getParser().parseHeader(getRandomAccess()));
            try {
                int offset = getParser().searchLastStartXRef(getRandomAccess());
                AbstractXRefParser xRefParser;
                if (getParser().isTokenXRefAt(getRandomAccess(), offset)) {
                    xRefParser = new XRefTrailerParser(this, getParser());
                } else {
                    xRefParser = new XRefStreamParser(this, getParser());
                }
                getRandomAccess().seek(offset);
                initialXRefSection = xRefParser.parse(getRandomAccess());
            } catch (Exception ex) {
                Log.log(Level.FINEST, "error parsing " + getLocator().getFullName(), ex);
                initialXRefSection = new XRefFallbackParser(this, getParser()).parse(getRandomAccess());
            }
            setXRefSection(initialXRefSection);
            nextKey = new COSObjectKey(initialXRefSection.getSize() - 1, 0);
            initEncryption();
            checkConsistency();
        } catch (IOException e) {
            try {
                close();
            } catch (IOException ce) {
            }
            throw e;
        } catch (COSLoadException e) {
            try {
                close();
            } catch (IOException ce) {
            }
            throw e;
        }
    }

    public void updateModificationDate() {
        COSDictionary infoDict = cosGetTrailer().get(COSTrailer.DK_Info).asDictionary();
        if (infoDict == null) {
            return;
        }
        infoDict.put(COSInfoDict.DK_ModDate, new CDSDate().cosGetObject());
    }
}
