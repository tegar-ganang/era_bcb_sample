package com.siemens.ct.exi.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.xml.namespace.QName;
import com.siemens.ct.exi.CodingMode;
import com.siemens.ct.exi.EXIFactory;
import com.siemens.ct.exi.core.container.DocType;
import com.siemens.ct.exi.core.container.NamespaceDeclaration;
import com.siemens.ct.exi.core.container.ProcessingInstruction;
import com.siemens.ct.exi.datatype.Datatype;
import com.siemens.ct.exi.exceptions.EXIException;
import com.siemens.ct.exi.grammar.event.EventType;
import com.siemens.ct.exi.io.channel.BitDecoderChannel;
import com.siemens.ct.exi.io.channel.ByteDecoderChannel;
import com.siemens.ct.exi.io.channel.DecoderChannel;
import com.siemens.ct.exi.types.BuiltIn;
import com.siemens.ct.exi.values.Value;

/**
 * EXI decoder for bit or byte-aligned streams.
 * 
 * @author Daniel.Peintner.EXT@siemens.com
 * @author Joerg.Heuer@siemens.com
 * 
 * @version 0.8
 */
public class EXIBodyDecoderInOrder extends AbstractEXIBodyDecoder {

    public EXIBodyDecoderInOrder(EXIFactory exiFactory) throws EXIException {
        super(exiFactory);
    }

    public void setInputStream(InputStream is) throws EXIException, IOException {
        CodingMode codingMode = exiFactory.getCodingMode();
        if (codingMode == CodingMode.BIT_PACKED) {
            setInputChannel(new BitDecoderChannel(is));
        } else {
            assert (codingMode == CodingMode.BYTE_PACKED);
            setInputChannel(new ByteDecoderChannel(is));
        }
        initForEachRun();
    }

    public void setInputChannel(DecoderChannel decoderChannel) throws EXIException, IOException {
        this.channel = decoderChannel;
        initForEachRun();
    }

    public DecoderChannel getChannel() {
        return this.channel;
    }

    @Override
    protected void initForEachRun() throws EXIException, IOException {
        super.initForEachRun();
        nextEvent = null;
        nextEventType = EventType.START_DOCUMENT;
    }

    public EventType next() throws EXIException, IOException {
        return nextEventType == EventType.END_DOCUMENT ? null : decodeEventCode();
    }

    public void decodeStartDocument() throws EXIException {
        decodeStartDocumentStructure();
    }

    public void decodeEndDocument() throws EXIException, IOException {
        decodeEndDocumentStructure();
    }

    public QName decodeStartElement() throws EXIException, IOException {
        switch(this.nextEventType) {
            case START_ELEMENT:
                return decodeStartElementStructure();
            case START_ELEMENT_NS:
                return decodeStartElementNSStructure();
            case START_ELEMENT_GENERIC:
                return decodeStartElementGenericStructure();
            case START_ELEMENT_GENERIC_UNDECLARED:
                return decodeStartElementGenericUndeclaredStructure();
            default:
                throw new EXIException("Invalid decode state: " + this.nextEventType);
        }
    }

    public QName decodeEndElement() throws EXIException, IOException {
        ElementContext ec;
        switch(this.nextEventType) {
            case END_ELEMENT:
                ec = decodeEndElementStructure();
                break;
            case END_ELEMENT_UNDECLARED:
                ec = decodeEndElementUndeclaredStructure();
                break;
            default:
                throw new EXIException("Invalid decode state: " + this.nextEventType);
        }
        return ec.qnameContext.getQName();
    }

    public List<NamespaceDeclaration> getDeclaredPrefixDeclarations() {
        return getElementContext().nsDeclarations;
    }

    public String getElementPrefix() {
        return this.getElementContext().prefix;
    }

    public String getElementQNameAsString() {
        return this.getElementContext().getQNameAsString();
    }

    public NamespaceDeclaration decodeNamespaceDeclaration() throws EXIException, IOException {
        return decodeNamespaceDeclarationStructure();
    }

    public QName decodeAttributeXsiNil() throws EXIException, IOException {
        assert (nextEventType == EventType.ATTRIBUTE_XSI_NIL);
        decodeAttributeXsiNilStructure();
        return this.attributeQNameContext.getQName();
    }

    public QName decodeAttributeXsiType() throws EXIException, IOException {
        assert (nextEventType == EventType.ATTRIBUTE_XSI_TYPE);
        decodeAttributeXsiTypeStructure();
        return this.attributeQNameContext.getQName();
    }

    protected void readAttributeContent(Datatype dt) throws IOException {
        attributeValue = typeDecoder.readValue(dt, decoderContext, attributeQNameContext, channel);
    }

    protected void readAttributeContent() throws IOException, EXIException {
        if (attributeQNameContext.getNamespaceUriID() == decoderContext.getXsiTypeContext().getNamespaceUriID()) {
            int localNameID = attributeQNameContext.getLocalNameID();
            if (localNameID == decoderContext.getXsiTypeContext().getLocalNameID()) {
                decodeAttributeXsiTypeStructure();
            } else if (localNameID == decoderContext.getXsiTypeContext().getLocalNameID() && getCurrentRule().isSchemaInformed()) {
                decodeAttributeXsiNilStructure();
            } else {
                readAttributeContent(BuiltIn.DEFAULT_DATATYPE);
            }
        } else {
            Datatype dt = BuiltIn.DEFAULT_DATATYPE;
            if (getCurrentRule().isSchemaInformed() && attributeQNameContext.getGlobalAttribute() != null) {
                dt = attributeQNameContext.getGlobalAttribute().getDatatype();
            }
            readAttributeContent(dt);
        }
    }

    public QName decodeAttribute() throws EXIException, IOException {
        switch(this.nextEventType) {
            case ATTRIBUTE:
                Datatype dt = decodeAttributeStructure();
                if (this.attributeQNameContext.equals(decoderContext.getXsiTypeContext())) {
                    decodeAttributeXsiTypeStructure();
                } else {
                    readAttributeContent(dt);
                }
                break;
            case ATTRIBUTE_NS:
                decodeAttributeNSStructure();
                readAttributeContent();
                break;
            case ATTRIBUTE_GENERIC:
                decodeAttributeGenericStructure();
                readAttributeContent();
                break;
            case ATTRIBUTE_GENERIC_UNDECLARED:
                decodeAttributeGenericUndeclaredStructure();
                readAttributeContent();
                break;
            case ATTRIBUTE_INVALID_VALUE:
                decodeAttributeStructure();
                readAttributeContent(BuiltIn.DEFAULT_DATATYPE);
                break;
            case ATTRIBUTE_ANY_INVALID_VALUE:
                decodeAttributeAnyInvalidValueStructure();
                readAttributeContent(BuiltIn.DEFAULT_DATATYPE);
                break;
            default:
                throw new EXIException("Invalid decode state: " + this.nextEventType);
        }
        return attributeQNameContext.getQName();
    }

    public Value decodeCharacters() throws EXIException, IOException {
        Datatype dt;
        switch(this.nextEventType) {
            case CHARACTERS:
                dt = decodeCharactersStructure();
                break;
            case CHARACTERS_GENERIC:
                decodeCharactersGenericStructure();
                dt = BuiltIn.DEFAULT_DATATYPE;
                break;
            case CHARACTERS_GENERIC_UNDECLARED:
                decodeCharactersGenericUndeclaredStructure();
                dt = BuiltIn.DEFAULT_DATATYPE;
                break;
            default:
                throw new EXIException("Invalid decode state: " + this.nextEventType);
        }
        return typeDecoder.readValue(dt, decoderContext, getElementContext().qnameContext, channel);
    }

    public char[] decodeEntityReference() throws EXIException, IOException {
        return decodeEntityReferenceStructure();
    }

    public char[] decodeComment() throws EXIException, IOException {
        return decodeCommentStructure();
    }

    public ProcessingInstruction decodeProcessingInstruction() throws EXIException, IOException {
        return decodeProcessingInstructionStructure();
    }

    public DocType decodeDocType() throws EXIException, IOException {
        return decodeDocTypeStructure();
    }
}
