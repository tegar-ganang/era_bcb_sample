package com.siemens.ct.exi.core;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import javax.xml.namespace.QName;
import com.siemens.ct.exi.CodingMode;
import com.siemens.ct.exi.Constants;
import com.siemens.ct.exi.EXIFactory;
import com.siemens.ct.exi.context.QNameContext;
import com.siemens.ct.exi.context.RuntimeQNameContextEntries;
import com.siemens.ct.exi.core.container.DocType;
import com.siemens.ct.exi.core.container.NamespaceDeclaration;
import com.siemens.ct.exi.core.container.PreReadValue;
import com.siemens.ct.exi.core.container.ProcessingInstruction;
import com.siemens.ct.exi.core.container.ValueAndDatatype;
import com.siemens.ct.exi.datatype.Datatype;
import com.siemens.ct.exi.exceptions.EXIException;
import com.siemens.ct.exi.grammar.event.EventType;
import com.siemens.ct.exi.io.channel.ByteDecoderChannel;
import com.siemens.ct.exi.io.channel.DecoderChannel;
import com.siemens.ct.exi.types.BuiltIn;
import com.siemens.ct.exi.values.Value;

/**
 * EXI decoder for (pre-)compression streams.
 * 
 * @author Daniel.Peintner.EXT@siemens.com
 * @author Joerg.Heuer@siemens.com
 * 
 * @version 0.8
 */
public class EXIBodyDecoderReordered extends AbstractEXIBodyDecoder {

    protected List<EventType> eventTypes;

    protected int eventTypeIndex;

    protected List<ElementContext> elementEntries;

    protected int elementEntryIndex;

    protected List<QNameEntry> qnameEntries;

    protected int qnameEntryIndex;

    protected List<DocType> docTypeEntries;

    protected int docTypeEntryIndex;

    protected List<char[]> entityReferences;

    protected int entityReferenceIndex;

    protected List<char[]> comments;

    protected int commentIndex;

    protected List<NamespaceDeclaration> nsEntries;

    protected int nsEntryIndex;

    protected List<ProcessingInstruction> processingEntries;

    protected int processingEntryIndex;

    protected int blockValues;

    boolean stillNoEndOfDocument = true;

    ElementContext lastBlockElementContext;

    protected CodingMode codingMode;

    ElementContext currElementEntry;

    protected Map<QNameContext, PreReadValue> contentValues;

    protected List<Value> xsiValues;

    protected int xsiValueIndex;

    protected List<String> xsiPrefixes;

    protected int xsiPrefixIndex;

    protected InputStream resettableInputStream;

    protected InflaterInputStream recentInflaterInputStream;

    protected long bytesRead;

    protected Inflater inflater;

    protected boolean firstChannel;

    protected InputStream is;

    public EXIBodyDecoderReordered(EXIFactory exiFactory) throws EXIException {
        super(exiFactory);
        eventTypes = new ArrayList<EventType>();
        elementEntries = new ArrayList<ElementContext>();
        qnameEntries = new ArrayList<QNameEntry>();
        docTypeEntries = new ArrayList<DocType>();
        entityReferences = new ArrayList<char[]>();
        comments = new ArrayList<char[]>();
        nsEntries = new ArrayList<NamespaceDeclaration>();
        processingEntries = new ArrayList<ProcessingInstruction>();
        contentValues = new HashMap<QNameContext, PreReadValue>();
        xsiValues = new ArrayList<Value>();
        xsiPrefixes = new ArrayList<String>();
        codingMode = exiFactory.getCodingMode();
    }

    @Override
    protected void initForEachRun() throws EXIException, IOException {
        super.initForEachRun();
        nextEvent = null;
        nextEventType = EventType.START_DOCUMENT;
        elementEntries.clear();
        elementEntryIndex = 0;
        qnameEntries.clear();
        qnameEntryIndex = 0;
        docTypeEntries.clear();
        docTypeEntryIndex = 0;
        entityReferences.clear();
        entityReferenceIndex = 0;
        comments.clear();
        commentIndex = 0;
        nsEntries.clear();
        nsEntryIndex = 0;
        processingEntries.clear();
        processingEntryIndex = 0;
        stillNoEndOfDocument = true;
        lastBlockElementContext = null;
        initBlock();
        preReadBlockStructure();
        preReadBlockContent();
    }

    protected void initBlock() {
        blockValues = 0;
        eventTypes.clear();
        eventTypeIndex = 0;
        decoderContext.initCompressionBlock();
        contentValues.clear();
        xsiValues.clear();
        xsiValueIndex = 0;
        xsiPrefixes.clear();
        xsiPrefixIndex = 0;
    }

    public void setInputStream(InputStream is) throws EXIException, IOException {
        this.is = is;
        firstChannel = true;
        channel = getNextChannel();
        initForEachRun();
    }

    public void setInputChannel(DecoderChannel channel) throws EXIException, IOException {
        throw new RuntimeException("[EXI] Reorderd EXI Body decoder needs to be set via setInputStream(...)");
    }

    public DecoderChannel getNextChannel() throws IOException {
        if (codingMode == CodingMode.COMPRESSION) {
            if (firstChannel) {
                bytesRead = 0;
                resettableInputStream = new BufferedInputStream(is);
                resettableInputStream.mark(Integer.MAX_VALUE);
                inflater = new Inflater(true);
                firstChannel = false;
            } else {
                if (!inflater.finished()) {
                    while (!inflater.finished()) {
                        recentInflaterInputStream.read();
                    }
                }
                bytesRead += inflater.getBytesRead();
                resettableInputStream.reset();
                long skipped = resettableInputStream.skip(bytesRead);
                if (skipped != bytesRead) {
                    do {
                        long skippedLoop = resettableInputStream.skip(bytesRead - skipped);
                        if (skippedLoop <= 0) {
                            throw new IOException("[EXI] Byte skipping impossible on given input stream");
                        }
                        skipped += skippedLoop;
                        assert (skipped <= bytesRead);
                    } while (skipped < bytesRead);
                }
                inflater.reset();
            }
            recentInflaterInputStream = new InflaterInputStream(resettableInputStream, inflater);
            return new ByteDecoderChannel(recentInflaterInputStream);
        } else {
            assert (codingMode == CodingMode.PRE_COMPRESSION);
            if (firstChannel) {
                channel = new ByteDecoderChannel(this.is);
                firstChannel = false;
            }
            return channel;
        }
    }

    protected void updateAttributeToXsiType() throws EXIException, IOException {
        eventTypes.set(eventTypes.size() - 1, EventType.ATTRIBUTE_XSI_TYPE);
        decodeAttributeXsiTypeStructure();
        xsiValues.add(attributeValue);
        xsiPrefixes.add(attributePrefix);
    }

    protected void handleSpecialAttributeCases() throws EXIException, IOException {
        if (decoderContext.getXsiTypeContext().equals(attributeQNameContext)) {
            updateAttributeToXsiType();
        } else if (decoderContext.getXsiNilContext().equals(attributeQNameContext) && getCurrentRule().isSchemaInformed()) {
            eventTypes.set(eventTypes.size() - 1, EventType.ATTRIBUTE_XSI_NIL);
            decodeAttributeXsiNilStructure();
            xsiValues.add(attributeValue);
            xsiPrefixes.add(attributePrefix);
        } else {
            Datatype dt = BuiltIn.DEFAULT_DATATYPE;
            if (getCurrentRule().isSchemaInformed() && attributeQNameContext.getGlobalAttribute() != null) {
                dt = attributeQNameContext.getGlobalAttribute().getDatatype();
            }
            addQNameEntry(new QNameEntry(attributeQNameContext, attributePrefix));
            incrementValues(attributeQNameContext, dt);
        }
    }

    protected void preReadBlockStructure() throws EXIException, IOException {
        boolean stillBlockReadable = true;
        boolean deferredStartElement = false;
        if (lastBlockElementContext != null) {
            updateElementContext(lastBlockElementContext);
        }
        while (stillNoEndOfDocument && stillBlockReadable) {
            if (deferredStartElement) {
                switch(nextEventType) {
                    case ATTRIBUTE:
                    case ATTRIBUTE_INVALID_VALUE:
                    case ATTRIBUTE_ANY_INVALID_VALUE:
                    case ATTRIBUTE_GENERIC:
                    case ATTRIBUTE_NS:
                    case ATTRIBUTE_GENERIC_UNDECLARED:
                    case START_ELEMENT:
                    case START_ELEMENT_NS:
                    case START_ELEMENT_GENERIC:
                    case START_ELEMENT_GENERIC_UNDECLARED:
                    case END_ELEMENT:
                    case END_ELEMENT_UNDECLARED:
                    case CHARACTERS:
                    case CHARACTERS_GENERIC:
                    case CHARACTERS_GENERIC_UNDECLARED:
                    case DOC_TYPE:
                    case ENTITY_REFERENCE:
                    case COMMENT:
                    case PROCESSING_INSTRUCTION:
                        elementEntries.add(getElementContext());
                        deferredStartElement = false;
                }
            }
            eventTypes.add(nextEventType);
            switch(nextEventType) {
                case START_DOCUMENT:
                    decodeStartDocumentStructure();
                    break;
                case START_ELEMENT:
                    decodeStartElementStructure();
                    deferredStartElement = true;
                    break;
                case START_ELEMENT_NS:
                    decodeStartElementNSStructure();
                    deferredStartElement = true;
                    break;
                case START_ELEMENT_GENERIC:
                    decodeStartElementGenericStructure();
                    deferredStartElement = true;
                    break;
                case START_ELEMENT_GENERIC_UNDECLARED:
                    decodeStartElementGenericUndeclaredStructure();
                    deferredStartElement = true;
                    break;
                case NAMESPACE_DECLARATION:
                    nsEntries.add(decodeNamespaceDeclarationStructure());
                    break;
                case ATTRIBUTE_XSI_TYPE:
                    decodeAttributeXsiTypeStructure();
                    xsiPrefixes.add(attributePrefix);
                    xsiValues.add(attributeValue);
                    break;
                case ATTRIBUTE_XSI_NIL:
                    decodeAttributeXsiNilStructure();
                    xsiPrefixes.add(attributePrefix);
                    xsiValues.add(attributeValue);
                    break;
                case ATTRIBUTE:
                    Datatype dtAT = decodeAttributeStructure();
                    if (this.decoderContext.getXsiTypeContext().equals(this.attributeQNameContext)) {
                        updateAttributeToXsiType();
                    } else {
                        addQNameEntry(new QNameEntry(this.attributeQNameContext, attributePrefix));
                        incrementValues(this.attributeQNameContext, dtAT);
                    }
                    break;
                case ATTRIBUTE_INVALID_VALUE:
                    decodeAttributeStructure();
                    addQNameEntry(new QNameEntry(attributeQNameContext, attributePrefix));
                    incrementValues(attributeQNameContext, BuiltIn.DEFAULT_DATATYPE);
                    break;
                case ATTRIBUTE_ANY_INVALID_VALUE:
                    decodeAttributeAnyInvalidValueStructure();
                    addQNameEntry(new QNameEntry(attributeQNameContext, attributePrefix));
                    incrementValues(attributeQNameContext, BuiltIn.DEFAULT_DATATYPE);
                    break;
                case ATTRIBUTE_NS:
                    decodeAttributeNSStructure();
                    handleSpecialAttributeCases();
                    break;
                case ATTRIBUTE_GENERIC:
                    decodeAttributeGenericStructure();
                    handleSpecialAttributeCases();
                    break;
                case ATTRIBUTE_GENERIC_UNDECLARED:
                    decodeAttributeGenericUndeclaredStructure();
                    handleSpecialAttributeCases();
                    break;
                case CHARACTERS:
                    QNameContext qnc = getElementContext().qnameContext;
                    incrementValues(qnc, decodeCharactersStructure());
                    addQNameEntry(new QNameEntry(qnc, null));
                    break;
                case CHARACTERS_GENERIC:
                    decodeCharactersGenericStructure();
                    qnc = getElementContext().qnameContext;
                    incrementValues(qnc, BuiltIn.DEFAULT_DATATYPE);
                    addQNameEntry(new QNameEntry(qnc, null));
                    break;
                case CHARACTERS_GENERIC_UNDECLARED:
                    decodeCharactersGenericUndeclaredStructure();
                    qnc = getElementContext().qnameContext;
                    incrementValues(qnc, BuiltIn.DEFAULT_DATATYPE);
                    addQNameEntry(new QNameEntry(qnc, null));
                    break;
                case END_ELEMENT:
                    decodeEndElementStructure();
                    elementEntries.add(getElementContext());
                    break;
                case END_ELEMENT_UNDECLARED:
                    decodeEndElementUndeclaredStructure();
                    elementEntries.add(getElementContext());
                    break;
                case END_DOCUMENT:
                    decodeEndDocumentStructure();
                    stillNoEndOfDocument = false;
                    continue;
                case DOC_TYPE:
                    docTypeEntries.add(decodeDocTypeStructure());
                    break;
                case ENTITY_REFERENCE:
                    entityReferences.add(decodeEntityReferenceStructure());
                    break;
                case COMMENT:
                    comments.add(decodeCommentStructure());
                    break;
                case PROCESSING_INSTRUCTION:
                    processingEntries.add(decodeProcessingInstructionStructure());
                    break;
                default:
                    throw new RuntimeException("Unknown Event " + nextEventType);
            }
            if (blockValues == exiFactory.getBlockSize()) {
                stillBlockReadable = false;
                lastBlockElementContext = getElementContext();
                assert (deferredStartElement == false);
            } else {
                decodeEventCode();
            }
        }
        updateElementContext(lastBlockElementContext);
    }

    protected void preReadBlockContent() throws EXIException {
        try {
            if (blockValues <= Constants.MAX_NUMBER_OF_VALUES) {
                List<QNameContext> order = decoderContext.getChannelOrders();
                for (QNameContext o : order) {
                    RuntimeQNameContextEntries rqnce = decoderContext.getRuntimeQNameContextEntries(o);
                    List<ValueAndDatatype> lvd = rqnce.getValuesAndDataypes();
                    Value[] contentValues = new Value[lvd.size()];
                    for (int i = 0; i < lvd.size(); i++) {
                        ValueAndDatatype vd = lvd.get(i);
                        contentValues[i] = typeDecoder.readValue(vd.datatype, decoderContext, o, channel);
                    }
                    PreReadValue prv = new PreReadValue(contentValues);
                    rqnce.setPreReadValue(prv);
                }
            } else {
                DecoderChannel bdcLessEqual100 = null;
                List<QNameContext> orderLeq100 = decoderContext.getChannelOrders();
                for (QNameContext o : orderLeq100) {
                    RuntimeQNameContextEntries rqnce = decoderContext.getRuntimeQNameContextEntries(o);
                    List<ValueAndDatatype> lvd = rqnce.getValuesAndDataypes();
                    if (lvd.size() <= Constants.MAX_NUMBER_OF_VALUES) {
                        Value[] contentValues = new Value[lvd.size()];
                        if (bdcLessEqual100 == null) {
                            bdcLessEqual100 = getNextChannel();
                        }
                        for (int i = 0; i < lvd.size(); i++) {
                            ValueAndDatatype vd = lvd.get(i);
                            contentValues[i] = typeDecoder.readValue(vd.datatype, decoderContext, o, bdcLessEqual100);
                        }
                        PreReadValue prv = new PreReadValue(contentValues);
                        rqnce.setPreReadValue(prv);
                    }
                }
                List<QNameContext> orderGr100 = decoderContext.getChannelOrders();
                for (QNameContext o : orderGr100) {
                    RuntimeQNameContextEntries rqnce = decoderContext.getRuntimeQNameContextEntries(o);
                    List<ValueAndDatatype> lvd = rqnce.getValuesAndDataypes();
                    if (lvd.size() > Constants.MAX_NUMBER_OF_VALUES) {
                        DecoderChannel bdcGreater100 = getNextChannel();
                        Value[] contentValues = new Value[lvd.size()];
                        for (int i = 0; i < lvd.size(); i++) {
                            ValueAndDatatype vd = lvd.get(i);
                            contentValues[i] = typeDecoder.readValue(vd.datatype, decoderContext, o, bdcGreater100);
                        }
                        PreReadValue prv = new PreReadValue(contentValues);
                        rqnce.setPreReadValue(prv);
                    }
                }
            }
        } catch (IOException e) {
            throw new EXIException(e);
        }
    }

    protected void setContentValues(DecoderChannel bdc, QNameContext channelContext, int occs, List<Datatype> datatypes) throws IOException {
        assert (datatypes.size() == occs);
        Value[] decodedValues = new Value[occs];
        for (int k = 0; k < occs; k++) {
            Datatype dt = datatypes.get(k);
            decodedValues[k] = typeDecoder.readValue(dt, decoderContext, channelContext, bdc);
        }
        contentValues.put(channelContext, new PreReadValue(decodedValues));
    }

    private final void setupNewBlock() throws IOException, EXIException {
        initBlock();
        channel = getNextChannel();
        decodeEventCode();
        preReadBlockStructure();
        preReadBlockContent();
    }

    protected Value getNextContentValue(QNameContext qname) throws EXIException, IOException {
        blockValues--;
        RuntimeQNameContextEntries rqnce = decoderContext.getRuntimeQNameContextEntries(qname);
        Value v = rqnce.getPreReadValue().getNextContantValue();
        return v;
    }

    public EventType next() throws EXIException, IOException {
        if (stillNoEndOfDocument && blockValues == 0) {
            setupNewBlock();
        }
        if (stillNoEndOfDocument || eventTypes.size() > eventTypeIndex) {
            return eventTypes.get(eventTypeIndex++);
        } else {
            return null;
        }
    }

    protected void incrementValues(QNameContext valueContext, Datatype datatype) {
        blockValues++;
        this.decoderContext.addValueAndDatatype(valueContext, new ValueAndDatatype(null, datatype));
    }

    public void decodeStartDocument() {
    }

    protected final ElementContext setNextElemementEntry() {
        return (currElementEntry = elementEntries.get(elementEntryIndex++));
    }

    public QName decodeStartElement() throws IOException, EXIException {
        return setNextElemementEntry().qnameContext.getQName();
    }

    public QName decodeEndElement() throws EXIException {
        QName eeBefore = currElementEntry.qnameContext.getQName();
        setNextElemementEntry();
        return eeBefore;
    }

    public List<NamespaceDeclaration> getDeclaredPrefixDeclarations() {
        return currElementEntry.nsDeclarations;
    }

    public String getElementPrefix() {
        return currElementEntry.prefix;
    }

    public String getElementQNameAsString() {
        return currElementEntry.getQNameAsString();
    }

    public NamespaceDeclaration decodeNamespaceDeclaration() throws EXIException {
        return nsEntries.get(nsEntryIndex++);
    }

    public QName decodeAttributeXsiNil() throws EXIException, IOException {
        this.attributeQNameContext = decoderContext.getXsiNilContext();
        attributePrefix = xsiPrefixes.get(xsiPrefixIndex++);
        attributeValue = xsiValues.get(xsiValueIndex++);
        return attributeQNameContext.getQName();
    }

    public QName decodeAttributeXsiType() throws EXIException, IOException {
        this.attributeQNameContext = decoderContext.getXsiTypeContext();
        attributePrefix = xsiPrefixes.get(xsiPrefixIndex++);
        attributeValue = xsiValues.get(xsiValueIndex++);
        return attributeQNameContext.getQName();
    }

    protected final void addQNameEntry(QNameEntry qne) {
        qnameEntries.add(qne);
    }

    protected final QNameEntry getNextQNameEntry() {
        return qnameEntries.get(qnameEntryIndex++);
    }

    public QName decodeAttribute() throws EXIException, IOException {
        QNameEntry at = getNextQNameEntry();
        this.attributeQNameContext = at.qnContext;
        attributePrefix = at.prefix;
        attributeValue = getNextContentValue(attributeQNameContext);
        return attributeQNameContext.getQName();
    }

    public Value decodeCharacters() throws EXIException, IOException {
        QNameEntry ch = getNextQNameEntry();
        Value chVal = getNextContentValue(ch.qnContext);
        return chVal;
    }

    public Value decodeCharactersGeneric() throws EXIException, IOException {
        return decodeCharacters();
    }

    public Value decodeCharactersGenericUndeclared() throws EXIException, IOException {
        return decodeCharacters();
    }

    public void decodeEndDocument() throws EXIException {
    }

    public DocType decodeDocType() throws EXIException {
        return docTypeEntries.get(docTypeEntryIndex++);
    }

    public char[] decodeEntityReference() throws EXIException {
        return entityReferences.get(entityReferenceIndex++);
    }

    public char[] decodeComment() throws EXIException {
        return comments.get(commentIndex++);
    }

    public ProcessingInstruction decodeProcessingInstruction() throws EXIException {
        return processingEntries.get(processingEntryIndex++);
    }

    static class EndElementEntry {

        final ElementContext before;

        final ElementContext after;

        public EndElementEntry(ElementContext before, ElementContext after) {
            this.before = before;
            this.after = after;
        }
    }

    static class QNameEntry {

        final QNameContext qnContext;

        final String prefix;

        public QNameEntry(QNameContext qnContext, String prefix) {
            this.qnContext = qnContext;
            this.prefix = prefix;
        }
    }
}
