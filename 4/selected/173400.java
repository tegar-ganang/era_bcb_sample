package org.openexi.fujitsu.proc.io.compression;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Inflater;
import org.openexi.fujitsu.proc.common.AlignmentType;
import org.openexi.fujitsu.proc.common.CharacterSequence;
import org.openexi.fujitsu.proc.common.EXIEvent;
import org.openexi.fujitsu.proc.common.EventCode;
import org.openexi.fujitsu.proc.common.EventType;
import org.openexi.fujitsu.proc.common.EventTypeList;
import org.openexi.fujitsu.proc.events.*;
import org.openexi.fujitsu.proc.grammars.DocumentGrammarState;
import org.openexi.fujitsu.proc.grammars.EventTypeSchema;
import org.openexi.fujitsu.proc.grammars.EventTypeSchemaAttribute;
import org.openexi.fujitsu.proc.io.ByteAlignedCommons;
import org.openexi.fujitsu.proc.io.Scanner;
import org.openexi.fujitsu.proc.io.XMLLocusItem;
import org.openexi.fujitsu.proc.util.URIConst;
import org.openexi.fujitsu.schema.EXISchema;

public final class ChannellingScanner extends Scanner {

    private final ChannelKeeper m_channelKeeper;

    private final ArrayList<EXIEvent> m_eventList;

    private final boolean m_compressed;

    private final Inflater m_inflater;

    private int m_bufSize;

    private boolean m_foundED;

    private int m_n_blocks;

    private int m_eventIndex;

    public ChannellingScanner(boolean compressed) {
        super(false);
        if (m_compressed = compressed) m_inflater = new Inflater(true); else m_inflater = null;
        m_eventList = new ArrayList();
        m_channelKeeper = new ChannelKeeper(new ScannerChannelFactory());
    }

    @Override
    public void init(DocumentGrammarState documentGrammarState, int inflatorBufSize) {
        super.init(documentGrammarState, -1);
        m_bufSize = m_compressed ? inflatorBufSize : -1;
    }

    @Override
    public void reset() {
        super.reset();
        m_foundED = false;
        m_n_blocks = 0;
        m_eventList.clear();
        m_eventIndex = 0;
    }

    @Override
    public void setInputStream(InputStream istream) {
        super.setInputStream(m_compressed ? new EXIInflaterInputStream(istream, m_inflater, m_bufSize) : istream);
    }

    @Override
    public final void prepare() throws IOException {
        super.prepare();
        processBlock();
    }

    @Override
    public final void setBlockSize(int blockSize) {
        m_channelKeeper.setBlockSize(blockSize);
    }

    public final int getBlockCount() {
        return m_n_blocks;
    }

    @Override
    public EXIEvent nextEvent() throws IOException {
        EXIEvent event = null;
        if (m_eventIndex < m_eventList.size()) {
            event = m_eventList.get(m_eventIndex++);
        } else if (!m_foundED) {
            processBlock();
            return nextEvent();
        }
        return event;
    }

    private void readValueChannels() throws IOException {
        m_channelKeeper.finish();
        final List<Channel> smallChannels, largeChannels;
        final int n_smallChannels, n_largeChannels;
        int i = 0;
        ScannerChannel channel;
        smallChannels = m_channelKeeper.getSmallChannels();
        if ((n_smallChannels = smallChannels.size()) != 0) {
            if (m_compressed && m_channelKeeper.getTotalValueCount() > 100) resetInflator(m_inflater);
            do {
                channel = (ScannerChannel) smallChannels.get(i);
                ArrayList<ScannerValueHolder> textProviderList = channel.values;
                final int len = textProviderList.size();
                for (int j = 0; j < len; j++) {
                    textProviderList.get(j).scanText(this, m_inputStream);
                }
            } while (++i < n_smallChannels);
        }
        largeChannels = m_channelKeeper.getLargeChannels();
        for (i = 0, n_largeChannels = largeChannels.size(); i < n_largeChannels; i++) {
            if (m_compressed) resetInflator(m_inflater);
            channel = (ScannerChannel) largeChannels.get(i);
            ArrayList<ScannerValueHolder> textProviderList = channel.values;
            final int len = textProviderList.size();
            for (int j = 0; j < len; j++) {
                textProviderList.get(j).scanText(this, m_inputStream);
            }
        }
        if (m_compressed) resetInflator(m_inflater);
    }

    private void readStructureChannel() throws IOException {
        m_channelKeeper.reset();
        boolean reached = false;
        EventCode eventCodeItem;
        while (!reached && (eventCodeItem = m_documentGrammarState.getNextEventCodes()) != null) {
            final EventType eventType;
            eventType = readEventType(eventCodeItem);
            String name;
            String prefix, publicId, systemId;
            CharacterSequence text;
            ScannerValueHolder textProvider;
            ScannerChannel channel;
            final XMLLocusItem locusItem;
            int tp;
            byte itemType;
            switch(itemType = eventType.itemType) {
                case EventCode.ITEM_SD:
                    m_documentGrammarState.startDocument();
                    m_eventList.add(eventType.asEXIEvent());
                    break;
                case EventCode.ITEM_DTD:
                    name = readText().makeString();
                    text = readText();
                    publicId = text.length() != 0 ? text.makeString() : null;
                    text = readText();
                    systemId = text.length() != 0 ? text.makeString() : null;
                    text = readText();
                    m_eventList.add(new EXIEventDTD(name, publicId, systemId, text, eventType));
                    break;
                case EventCode.ITEM_SCHEMA_SE:
                case EventCode.ITEM_SE:
                    readQName(qname, eventType);
                    pushLocusItem(qname.namespaceName, qname.localName);
                    m_documentGrammarState.startElement(eventType.getIndex(), qname.namespaceName, qname.localName);
                    if (m_preserveNS) m_eventList.add(new EXIEventElement(qname.prefix, eventType)); else m_eventList.add((EXIEvent) eventType);
                    break;
                case EventCode.ITEM_SCHEMA_AT:
                case EventCode.ITEM_AT:
                    readQName(qname, eventType);
                    prefix = qname.prefix;
                    if (itemType == EventCode.ITEM_SCHEMA_AT) m_documentGrammarState.attribute(eventType.getIndex(), qname.namespaceName, qname.localName);
                    if (itemType == EventCode.ITEM_AT && "type".equals(qname.localName) && URIConst.W3C_2001_XMLSCHEMA_INSTANCE_URI.equals(qname.namespaceName)) {
                        m_eventList.add(readXsiTypeValue(prefix, eventType));
                    } else {
                        tp = EXISchema.NIL_NODE;
                        EventTypeSchemaAttribute eventTypeSchemaAttribute;
                        if (itemType == EventCode.ITEM_SCHEMA_AT && (eventTypeSchemaAttribute = (EventTypeSchemaAttribute) eventType).useSpecificType()) {
                            int attr = eventTypeSchemaAttribute.getSchemaSubstance();
                            assert EXISchema.ATTRIBUTE_NODE == m_schema.getNodeType(attr);
                            tp = m_schema.getTypeOfAttr(attr);
                        }
                        textProvider = new ScannerValueHolder(qname.localName, qname.namespaceName, tp);
                        channel = (ScannerChannel) m_channelKeeper.getChannel(qname.localName, qname.namespaceName);
                        reached = m_channelKeeper.incrementValueCount(channel);
                        channel.values.add(textProvider);
                        m_eventList.add(new EXIEventAttributeByRef(textProvider, prefix, eventType));
                    }
                    break;
                case EventCode.ITEM_SCHEMA_UNDECLARED_AT_INVALID_VALUE:
                    readQName(qname, eventType);
                    textProvider = new ScannerValueHolder(qname.localName, qname.namespaceName, EXISchema.NIL_NODE);
                    channel = (ScannerChannel) m_channelKeeper.getChannel(qname.localName, qname.namespaceName);
                    reached = m_channelKeeper.incrementValueCount(channel);
                    channel.values.add(textProvider);
                    m_documentGrammarState.attribute(((EventTypeSchema) eventType).serial, qname.namespaceName, qname.localName);
                    m_eventList.add(new EXIEventAttributeByRef(textProvider, (EventType) eventType));
                    break;
                case EventCode.ITEM_SCHEMA_CH:
                    m_documentGrammarState.characters();
                    tp = ((EventTypeSchema) eventType).getSchemaSubstance();
                    locusItem = m_locusStack[m_locusLastDepth];
                    textProvider = new ScannerValueHolder(locusItem.elementLocalName, locusItem.elementURI, tp);
                    channel = (ScannerChannel) m_channelKeeper.getChannel(locusItem.elementLocalName, locusItem.elementURI);
                    reached = m_channelKeeper.incrementValueCount(channel);
                    channel.values.add(textProvider);
                    m_eventList.add(new EXIEventSchemaCharactersByRef(textProvider, eventType));
                    break;
                case EventCode.ITEM_SCHEMA_CH_MIXED:
                    m_documentGrammarState.undeclaredCharacters();
                    locusItem = m_locusStack[m_locusLastDepth];
                    textProvider = new ScannerValueHolder(locusItem.elementLocalName, locusItem.elementURI, EXISchema.NIL_NODE);
                    channel = (ScannerChannel) m_channelKeeper.getChannel(locusItem.elementLocalName, locusItem.elementURI);
                    reached = m_channelKeeper.incrementValueCount(channel);
                    channel.values.add(textProvider);
                    m_eventList.add(new EXIEventSchemaMixedCharactersByRef(textProvider, eventType));
                    break;
                case EventCode.ITEM_CH:
                    m_documentGrammarState.undeclaredCharacters();
                    locusItem = m_locusStack[m_locusLastDepth];
                    textProvider = new ScannerValueHolder(locusItem.elementLocalName, locusItem.elementURI, EXISchema.NIL_NODE);
                    channel = (ScannerChannel) m_channelKeeper.getChannel(locusItem.elementLocalName, locusItem.elementURI);
                    reached = m_channelKeeper.incrementValueCount(channel);
                    channel.values.add(textProvider);
                    m_eventList.add(new EXIEventUndeclaredCharactersByRef(textProvider, eventType));
                    break;
                case EventCode.ITEM_SCHEMA_EE:
                case EventCode.ITEM_EE:
                    m_documentGrammarState.endElement("", "");
                    --m_locusLastDepth;
                    m_eventList.add((EXIEvent) eventType);
                    break;
                case EventCode.ITEM_ED:
                    m_documentGrammarState.endDocument();
                    m_eventList.add(eventType.asEXIEvent());
                    m_foundED = true;
                    break;
                case EventCode.ITEM_SCHEMA_WC_ANY:
                case EventCode.ITEM_SCHEMA_WC_NS:
                    readQName(qname, eventType);
                    pushLocusItem(qname.namespaceName, qname.localName);
                    m_documentGrammarState.startElement(eventType.getIndex(), qname.namespaceName, qname.localName);
                    m_eventList.add(new EXIEventWildcardStartElement(qname.namespaceName, qname.localName, qname.prefix, eventType));
                    break;
                case EventCode.ITEM_SE_WC:
                    readQName(qname, eventType);
                    pushLocusItem(qname.namespaceName, qname.localName);
                    m_documentGrammarState.startUndeclaredElement(qname.namespaceName, qname.localName);
                    m_eventList.add(new EXIEventUndeclaredElement(qname.namespaceName, qname.localName, qname.prefix, eventType));
                    break;
                case EventCode.ITEM_SCHEMA_AT_WC_ANY:
                case EventCode.ITEM_SCHEMA_AT_WC_NS:
                case EventCode.ITEM_AT_WC_ANY_UNTYPED:
                    readQName(qname, eventType);
                    prefix = qname.prefix;
                    m_documentGrammarState.undeclaredAttribute(qname.namespaceName, qname.localName);
                    if ("type".equals(qname.localName) && URIConst.W3C_2001_XMLSCHEMA_INSTANCE_URI.equals(qname.namespaceName)) {
                        assert itemType == EventCode.ITEM_AT_WC_ANY_UNTYPED;
                        m_eventList.add(readXsiTypeValue(prefix, eventType));
                    } else {
                        tp = EXISchema.NIL_NODE;
                        if (itemType != EventCode.ITEM_AT_WC_ANY_UNTYPED) {
                            int ns;
                            if ((ns = m_schema.getNamespaceOfSchema(qname.namespaceName)) != EXISchema.NIL_NODE) {
                                int attr;
                                if ((attr = m_schema.getAttrOfNamespace(ns, qname.localName)) != EXISchema.NIL_NODE) {
                                    tp = m_schema.getTypeOfAttr(attr);
                                }
                            }
                        }
                        textProvider = new ScannerValueHolder(qname.localName, qname.namespaceName, tp);
                        channel = (ScannerChannel) m_channelKeeper.getChannel(qname.localName, qname.namespaceName);
                        reached = m_channelKeeper.incrementValueCount(channel);
                        channel.values.add(textProvider);
                        m_eventList.add(new EXIEventWildcardAttributeByRef(qname.namespaceName, qname.localName, prefix, textProvider, eventType));
                    }
                    break;
                case EventCode.ITEM_SCHEMA_NIL:
                    readQName(qname, eventType);
                    final EXIEventSchemaNil eventSchemaNil = readXsiNilValue(qname.prefix, eventType);
                    if (eventSchemaNil.isNilled()) {
                        m_documentGrammarState.nillify();
                    }
                    m_eventList.add(eventSchemaNil);
                    break;
                case EventCode.ITEM_SCHEMA_TYPE:
                    readQName(qname, eventType);
                    prefix = qname.prefix;
                    m_eventList.add(readXsiTypeValue(prefix, eventType));
                    break;
                case EventCode.ITEM_NS:
                    m_eventList.add(readNS(eventType));
                    break;
                case EventCode.ITEM_SC:
                    throw new UnsupportedOperationException("Event type SC is not supported yet.");
                case EventCode.ITEM_PI:
                    m_documentGrammarState.miscContent();
                    name = readText().makeString();
                    text = readText();
                    m_eventList.add(new EXIEventProcessingInstruction(name, text, eventType));
                    break;
                case EventCode.ITEM_CM:
                    m_documentGrammarState.miscContent();
                    text = readText();
                    m_eventList.add(new EXIEventComment(text, eventType));
                    break;
                case EventCode.ITEM_ER:
                    m_documentGrammarState.miscContent();
                    name = readText().makeString();
                    m_eventList.add(new EXIEventEntityReference(name, eventType));
                    break;
                default:
                    assert false;
                    break;
            }
        }
        if (reached) {
            readMore: do {
                EventTypeList eventTypeList = m_documentGrammarState.getNextEventTypes();
                final int n_eventTypes;
                if ((n_eventTypes = eventTypeList.getLength()) != 1) {
                    assert n_eventTypes > 1;
                    break;
                }
                EventType eventType;
                switch((eventType = eventTypeList.item(0)).itemType) {
                    case EventCode.ITEM_SCHEMA_SE:
                        readQName(qname, eventType);
                        pushLocusItem(qname.namespaceName, qname.localName);
                        m_documentGrammarState.startElement(eventType.getIndex(), qname.namespaceName, qname.localName);
                        if (m_preserveNS) m_eventList.add(new EXIEventElement(qname.prefix, eventType)); else m_eventList.add((EXIEvent) eventType);
                        break;
                    case EventCode.ITEM_SCHEMA_EE:
                    case EventCode.ITEM_EE:
                        m_documentGrammarState.endElement("", "");
                        --m_locusLastDepth;
                        m_eventList.add((EXIEvent) eventType);
                        break;
                    case EventCode.ITEM_ED:
                        m_documentGrammarState.endDocument();
                        m_eventList.add(eventType.asEXIEvent());
                        m_foundED = true;
                    default:
                        break readMore;
                }
            } while (true);
        }
    }

    private void processBlock() throws IOException {
        ++m_n_blocks;
        m_eventList.clear();
        readStructureChannel();
        readValueChannels();
        m_eventIndex = 0;
    }

    private void resetInflator(Inflater inflator) throws IOException {
        for (int bt = 0; !m_inflater.finished() && bt != -1; ) {
            bt = m_inputStream.read();
        }
        m_inflater.reset();
    }

    @Override
    public AlignmentType getAlignmentType() {
        return m_compressed ? AlignmentType.compress : AlignmentType.preCompress;
    }

    @Override
    protected boolean readBoolean(InputStream istream) throws IOException {
        return ByteAlignedCommons.readBoolean(istream);
    }

    @Override
    protected int readNBitUnsigned(int width, InputStream istream) throws IOException {
        return ByteAlignedCommons.readNBitUnsigned(width, istream);
    }
}
