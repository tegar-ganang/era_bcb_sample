package com.abiquo.framework.xml.events;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import org.apache.commons.io.IOUtils;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.XMLStreamWriter2;
import org.codehaus.stax2.evt.XMLEvent2;
import org.codehaus.stax2.ri.evt.CharactersEventImpl;
import com.abiquo.framework.config.FrameworkTypes.ExceptionType;
import com.abiquo.framework.domain.InstanceName;
import com.abiquo.framework.domain.types.DataReferenceType;
import com.abiquo.framework.domain.types.DataType;
import com.abiquo.framework.domain.types.BoolType;
import com.abiquo.framework.domain.types.ByteType;
import com.abiquo.framework.domain.types.DoubleType;
import com.abiquo.framework.domain.types.FloatType;
import com.abiquo.framework.domain.types.IBaseType;
import com.abiquo.framework.domain.types.IntType;
import com.abiquo.framework.domain.types.StringType;
import com.abiquo.framework.domain.types.IBaseType.Types;
import com.abiquo.framework.exception.EventCastException;
import com.abiquo.framework.exception.InvalidMessageException;
import com.abiquo.framework.messages.IGridMessage;
import com.abiquo.framework.xml.AbsXMLMessage;
import com.abiquo.framework.xml.EventReader;
import com.abiquo.framework.xml.IXMLStream;
import com.abiquo.framework.xml.transaction.stream.ReadXMLToOutputStream;
import com.abiquo.framework.xml.transaction.stream.WriteXMLFromInputStream;
import com.abiquo.util.OutputStreamDecoderWriter;

/**
 * XML element serialization implementation for all BaseType on the Abiquo XSD schema.<br/>
 * 
 * It use the new capabilities on Typed API on Woodstox (the Stax2 implementation).
 * 
 * <code>
<xs:complexType name="IBaseType"> 
	<xs:complexContent>
		<xs:extension base="xs:anyType"> 
			<xs:attribute name = "name" type = "xs:string" use="optional"/>
 		</xs:extension> 
 	</xs:complexContent>
</xs:complexType>
</code>
 * 
 * */
public class BaseTypeXMLEvent extends AbsXMLMessage {

    /** The underlying BaseType object for this XML representation. */
    private IBaseType baseType;

    public BaseTypeXMLEvent(IBaseType BaseType) {
        super(BaseType.getType());
        baseType = BaseType;
        createTagElements();
    }

    /** Get the underlying BaseType object . */
    public IBaseType getBaseType() {
        return baseType;
    }

    /**
	 * @see com.abiquo.framework.xml.AbsXMLMessage
	 * */
    public IGridMessage getMessage() {
        throw new EventCastException("It isnt a Message, it is : " + elementQName);
    }

    /**
	 * @see com.abiquo.framework.xml.AbsXMLMessage#getAttributesElement()
	 * */
    protected Iterator<Attribute> getAttributesElement() {
        List<Attribute> lstAtt = new ArrayList<Attribute>();
        if (baseType.isLocalId()) {
            lstAtt.add(evntFact.createAttribute(stLocalId, baseType.getLocalId()));
        }
        if (baseType.isList()) {
            lstAtt.add(evntFact.createAttribute(stSize, String.valueOf(baseType.getListSize())));
        }
        return lstAtt.iterator();
    }

    /**
	 * @see com.abiquo.framework.xml.AbsXMLMessage#writeAttributesAndSubElementsAsUnicode
	 * */
    protected void writeAttributesAndSubElementsAsUnicode(Writer writer) throws XMLStreamException {
        String simpleContent = null;
        switch(baseType.getType()) {
            case BYTE:
                simpleContent = String.valueOf(baseType.asByte());
                break;
            case INT:
                simpleContent = String.valueOf(baseType.asInt());
                break;
            case FLOAT:
                simpleContent = String.valueOf(baseType.asFloat());
                break;
            case DOUBLE:
                simpleContent = String.valueOf(baseType.asDouble());
                break;
            case BOOL:
                simpleContent = String.valueOf(baseType.asBool());
                break;
            case STRING:
                simpleContent = baseType.asString();
                break;
            case DATA:
                writeData(baseType, writer);
                break;
            case DATA_REF:
                new InstanceNameXMLEvent(baseType.asDataReference()).writeAsEncodedUnicode(writer);
                break;
            case INT_LIST:
                int[] intList = baseType.asIntList();
                for (int i = 0; i < intList.length; i++) {
                    new BaseTypeXMLEvent(new IntType(intList[i])).writeAsEncodedUnicode(writer);
                }
                break;
            case FLOAT_LIST:
                float[] floatList = baseType.asFloatList();
                for (int i = 0; i < floatList.length; i++) {
                    new BaseTypeXMLEvent(new FloatType(floatList[i])).writeAsEncodedUnicode(writer);
                }
                break;
            case DOUBLE_LIST:
                double[] doubleList = baseType.asDoubleList();
                for (int i = 0; i < doubleList.length; i++) {
                    new BaseTypeXMLEvent(new DoubleType(doubleList[i])).writeAsEncodedUnicode(writer);
                }
                break;
            case BOOL_LIST:
                boolean[] boolList = baseType.asBoolList();
                for (int i = 0; i < boolList.length; i++) {
                    new BaseTypeXMLEvent(new BoolType(boolList[i])).writeAsEncodedUnicode(writer);
                }
                break;
            case STRING_LIST:
                String[] stringList = baseType.asStringList();
                for (int i = 0; i < stringList.length; i++) {
                    new BaseTypeXMLEvent(new StringType(stringList[i])).writeAsEncodedUnicode(writer);
                }
                break;
            case BYTE_LIST:
                byte[] byteList = baseType.asByteList();
                for (int i = 0; i < byteList.length; i++) {
                    new BaseTypeXMLEvent(new ByteType(byteList[i])).writeAsEncodedUnicode(writer);
                }
                break;
            case DATA_LIST:
                byte[][] base64List = baseType.asDataList();
                for (int i = 0; i < base64List.length; i++) {
                    new BaseTypeXMLEvent(new DataType(base64List[i])).writeAsEncodedUnicode(writer);
                }
                break;
            case DATA_REF_LIST:
                InstanceName[] names = baseType.asDataReferenceList();
                for (int i = 0; i < names.length; i++) {
                    new InstanceNameXMLEvent(names[i]).writeAsEncodedUnicode(writer);
                }
                break;
            default:
                throw new XMLStreamException("BaseType invalid type " + baseType.getType().toString());
        }
        if (simpleContent != null) {
            new CharactersEventImpl(startElement.getLocation(), simpleContent, false).writeAsEncodedUnicode(writer);
        }
    }

    /**
	 * TODO redoc (streaming)
	 * */
    private void writeData(IBaseType dataType, Writer writer) throws XMLStreamException {
        InputStream isData;
        DataType data = (DataType) baseType;
        if (data.isSetInputStream()) {
            isData = data.getInputStream();
            try {
                IOUtils.copy(isData, writer);
            } catch (IOException e) {
                throw new XMLStreamException("DataType fail writing streaming data ", e);
            }
        } else if (data.isSetOutputStream()) {
            throw new XMLStreamException("DataType only can write streaming input, its an output stream (only for reading) ");
        } else {
            new CharactersEventImpl(startElement.getLocation(), String.valueOf(baseType.asData()), false).writeAsEncodedUnicode(writer);
        }
    }

    /**
	 * TODO redoc (streaming) NON BLOQUING
	 * */
    private void writeData(IBaseType dataType, XMLStreamWriter2 writer) throws XMLStreamException {
        DataType data = (DataType) baseType;
        if (data.isSetInputStream()) {
            WriteXMLFromInputStream writeXML4is = new WriteXMLFromInputStream(writer, data.getInputStream(), data.isStream());
            Future<Boolean> futureStream = executorStream.submit(writeXML4is);
            if (!data.isStream()) {
                try {
                    futureStream.get();
                } catch (Exception e) {
                    throw new XMLStreamException("Data input stream can not be write ", e);
                }
            }
        } else if (data.isSetOutputStream()) {
            throw new XMLStreamException("DataType only can write streaming input, its an output stream (only for reading) ");
        } else {
            byte[] byData = baseType.asData();
            writer.writeBinary(byData, 0, byData.length);
        }
    }

    static ExecutorService executorStream = Executors.newCachedThreadPool();

    /**
	 * TODO is NON BLOCKIN (on data control future)
	 * */
    private static DataType readData(String typeName, boolean isStream, XMLStreamReader2 readerStream) throws XMLStreamException {
        DataType data;
        if (isStream) {
            PipedOutputStream os = new PipedOutputStream();
            try {
                data = new DataType(typeName, os);
            } catch (IOException e) {
                final String msg = "Data output from TransferResponse can not be connected (piped) ";
                throw new XMLStreamException(msg, e);
            }
            ReadXMLToOutputStream xml2os = new ReadXMLToOutputStream(readerStream, os);
            Future<Integer> futureStream = executorStream.submit(xml2os);
        } else {
            data = new DataType(typeName, readerStream.getElementAsBinary());
        }
        return data;
    }

    /**
	 * @see com.abiquo.framework.xml.AbsXMLMessage
	 * */
    public void writeSubElements(XMLStreamWriter2 w) throws XMLStreamException {
        Types elementType;
        if (baseType.isLocalId()) {
            w.writeAttribute(stLocalId, baseType.getLocalId());
        }
        if (baseType.isList()) {
            w.writeAttribute(stSize, String.valueOf(baseType.getListSize()));
        }
        elementType = baseType.getType();
        switch(elementType) {
            case BYTE:
                byte[] bytes = { baseType.asByte() };
                w.writeBinary(bytes, 0, 1);
                break;
            case INT:
                w.writeInt(baseType.asInt());
                break;
            case FLOAT:
                w.writeFloat(baseType.asFloat());
                break;
            case DOUBLE:
                w.writeDouble(baseType.asDouble());
                break;
            case BOOL:
                w.writeBoolean(baseType.asBool());
                break;
            case STRING:
                w.writeRaw(baseType.asString());
                break;
            case DATA:
                writeData(baseType, w);
                break;
            case DATA_REF:
                new InstanceNameXMLEvent(baseType.asDataReference()).writeUsing(w);
                break;
            case INT_LIST:
                int[] intList = baseType.asIntList();
                for (int i = 0; i < intList.length; i++) {
                    new BaseTypeXMLEvent(new IntType(intList[i])).writeUsing(w);
                }
                break;
            case FLOAT_LIST:
                float[] floatList = baseType.asFloatList();
                for (int i = 0; i < floatList.length; i++) {
                    new BaseTypeXMLEvent(new FloatType(floatList[i])).writeUsing(w);
                }
                break;
            case DOUBLE_LIST:
                double[] doubleList = baseType.asDoubleList();
                for (int i = 0; i < doubleList.length; i++) {
                    new BaseTypeXMLEvent(new DoubleType(doubleList[i])).writeUsing(w);
                }
                break;
            case BOOL_LIST:
                boolean[] boolList = baseType.asBoolList();
                for (int i = 0; i < boolList.length; i++) {
                    new BaseTypeXMLEvent(new BoolType(boolList[i])).writeUsing(w);
                }
                break;
            case STRING_LIST:
                String[] stringList = baseType.asStringList();
                for (int i = 0; i < stringList.length; i++) {
                    new BaseTypeXMLEvent(new StringType(stringList[i])).writeUsing(w);
                }
                break;
            case BYTE_LIST:
                byte[] byteList = baseType.asByteList();
                for (int i = 0; i < byteList.length; i++) {
                    new BaseTypeXMLEvent(new ByteType(byteList[i])).writeUsing(w);
                }
                break;
            case DATA_LIST:
                byte[][] base64List = baseType.asDataList();
                for (int i = 0; i < base64List.length; i++) {
                    new BaseTypeXMLEvent(new DataType(base64List[i])).writeUsing(w);
                }
                break;
            case DATA_REF_LIST:
                InstanceName[] names = baseType.asDataReferenceList();
                for (int i = 0; i < names.length; i++) {
                    new InstanceNameXMLEvent(names[i]).writeUsing(w);
                }
                break;
            default:
                throw new XMLStreamException("BaseType invalid type " + baseType.getType().toString());
        }
    }

    /**
	 * Creates a BaseTypeXMLEvent from the incoming source.
	 * 
	 * In this class underlying XMLStreamReader2 access form EventReader is required to full support typed API on Stax2.
	 * 
	 * @param eventStart
	 *            the allocated BaseType startElement.
	 * @param readerEvent
	 *            the source to obtain the BaseType subElements.
	 * @throws InvalidMessageException
	 *             the construction mechanism (EventReader allocation sequence) determine if the element is valid
	 * @throws XMLStreamException
	 *             underlying XMLStreamReader2 problem
	 * */
    public static BaseTypeXMLEvent allocate(StartElement eventStart, EventReader readerEvent) throws XMLStreamException, InvalidMessageException {
        IBaseType baseType;
        List<IBaseType> typesList;
        String eventType;
        XMLStreamReader2 readerStream;
        String typeName = null;
        int size = -1;
        eventType = eventStart.getName().getLocalPart();
        readerStream = readerEvent.getStreamReader();
        Attribute att;
        Iterator<Attribute> itAtt = eventStart.getAttributes();
        boolean dataStream = false;
        if (!eventType.equalsIgnoreCase(stInstanceName)) {
            while (itAtt.hasNext()) {
                att = itAtt.next();
                final String attName = att.getName().getLocalPart();
                if (stLocalId.equalsIgnoreCase(attName)) {
                    typeName = att.getValue();
                } else if (stSize.equalsIgnoreCase(attName)) {
                    size = Integer.parseInt(att.getValue());
                } else if (stStream.equalsIgnoreCase(attName)) {
                    dataStream = Boolean.parseBoolean(att.getValue());
                } else {
                    throw new InvalidMessageException("Unexpected attribute for BaseType " + attName);
                }
            }
        }
        if (stInt.equalsIgnoreCase(eventType)) {
            baseType = new IntType(typeName, readerStream.getElementAsInt());
        } else if (stFloat.equalsIgnoreCase(eventType)) {
            baseType = new FloatType(typeName, readerStream.getElementAsFloat());
        } else if (stDouble.equalsIgnoreCase(eventType)) {
            baseType = new DoubleType(typeName, readerStream.getElementAsDouble());
        } else if (stBool.equalsIgnoreCase(eventType)) {
            baseType = new BoolType(typeName, readerStream.getElementAsBoolean());
        } else if (stString.equalsIgnoreCase(eventType)) {
            baseType = new StringType(typeName, readerStream.getElementText());
        } else if (stByte.equalsIgnoreCase(eventType)) {
            baseType = new ByteType(typeName, readerStream.getElementAsBinary()[0]);
        } else if (stData.equalsIgnoreCase(eventType)) {
            baseType = readData(typeName, dataStream, readerStream);
        } else if (stDataRef.equalsIgnoreCase(eventType)) {
            XMLEvent2 eventName = readerEvent.allocateNextEvent();
            if (eventName.isStartElement()) {
                if (stInstanceName.equalsIgnoreCase(eventName.asStartElement().getName().getLocalPart())) {
                    InstanceNameXMLEvent name = new InstanceNameXMLEvent(eventName.asStartElement().getAttributes());
                    baseType = new DataReferenceType(typeName, name.getInstanceName());
                } else {
                    throw new XMLStreamException("Expected startElement InstanceName afeter DataReference, instead arribe " + eventName.asStartElement().getName().getLocalPart());
                }
            } else {
                throw new XMLStreamException("Expected InstanceName startElement after DataReference");
            }
            if (!stInstanceName.equalsIgnoreCase(readerEvent.allocateNextEvent().asEndElement().getName().getLocalPart())) {
                throw new InvalidMessageException("Expected InstanceName endElement closing DataReference");
            }
            if (!stDataRef.equalsIgnoreCase(readerEvent.allocateNextEvent().asEndElement().getName().getLocalPart())) {
                throw new InvalidMessageException("Expected DataReference endElement closing DataReference");
            }
        } else if (stInstanceName.equalsIgnoreCase(eventType)) {
            InstanceNameXMLEvent name = new InstanceNameXMLEvent(eventStart.getAttributes());
            baseType = new DataReferenceType(typeName, name.getInstanceName());
            if (!stInstanceName.equalsIgnoreCase(readerEvent.allocateNextEvent().asEndElement().getName().getLocalPart())) {
                throw new InvalidMessageException("Expected InstanceName endElement closing DataReference inside a list");
            }
        } else if (eventType.endsWith("List")) {
            String listType = eventType.substring(0, eventType.length() - 4);
            boolean openList = true;
            typesList = new LinkedList<IBaseType>();
            int typesListType = 0;
            XMLEvent2 listItem;
            while (openList) {
                listItem = readerEvent.allocateNextEvent();
                if (listItem.isStartElement()) {
                    String currentItemName = listItem.asStartElement().getName().getLocalPart();
                    if (listType.equalsIgnoreCase(currentItemName) | (stDataRef.equalsIgnoreCase(listType) & stInstanceName.equalsIgnoreCase(currentItemName))) {
                        BaseTypeXMLEvent baseXML = allocate(listItem.asStartElement(), readerEvent);
                        typesListType = baseXML.getEventType();
                        typesList.add(baseXML.getBaseType());
                    } else {
                        final String msg = "Unexpected eventType inside the typed list, expected (list start) " + listType + " arribe " + listItem.asStartElement().getName().getLocalPart();
                        throw new InvalidMessageException(msg);
                    }
                } else if (listItem.isEndElement()) {
                    if (eventType.equalsIgnoreCase(listItem.asEndElement().getName().getLocalPart())) {
                        openList = false;
                    } else {
                        final String msg = "Unexpected endElement on " + eventType + " arribe a " + listItem.asEndElement().getName().getLocalPart();
                        throw new InvalidMessageException(msg);
                    }
                } else {
                    final String msg = "Unexpected eventType after opening an BaseTypeList " + listItem.getEventType();
                    throw new InvalidMessageException(msg);
                }
            }
            if (typesList.size() != size) {
                throw new InvalidMessageException("BaseType list inconsisten size, attribute " + size + " allocated " + typesList.size());
            }
            baseType = listToBaseType(typesListType, typeName, typesList);
        } else {
            final String msg = "Unexpected startEvent allocating a BaseTypeXMLEvent " + eventType;
            throw new InvalidMessageException(msg);
        }
        return new BaseTypeXMLEvent(baseType);
    }

    /**
	 * Fills on a BaseType of the corresponding type the input list.
	 * 
	 * @param type the type of the elements on lstBase (also the returned BaseType type)
	 * @param lstBase
	 *            a list of baseTypes
	 * @param name
	 *            the LocalId for the resulting BaseType
	 * @return a BaseType containing the input list
	 * */
    private static IBaseType listToBaseType(int type, String name, List<IBaseType> lstBase) throws XMLStreamException {
        IBaseType baseType;
        int i;
        switch(type) {
            case ET_INT:
                i = 0;
                int[] ints = new int[lstBase.size()];
                for (IBaseType b : lstBase) {
                    ints[i] = b.asInt();
                    i++;
                }
                baseType = new IntType(name, ints);
                break;
            case ET_FLOAT:
                i = 0;
                float[] floats = new float[lstBase.size()];
                for (IBaseType b : lstBase) {
                    floats[i] = b.asFloat();
                    i++;
                }
                baseType = new FloatType(name, floats);
                break;
            case ET_DOUBLE:
                i = 0;
                double[] doubles = new double[lstBase.size()];
                for (IBaseType b : lstBase) {
                    doubles[i] = b.asDouble();
                    i++;
                }
                baseType = new DoubleType(name, doubles);
                break;
            case ET_BOOL:
                i = 0;
                boolean[] booleans = new boolean[lstBase.size()];
                for (IBaseType b : lstBase) {
                    booleans[i] = b.asBool();
                    i++;
                }
                baseType = new BoolType(name, booleans);
                break;
            case ET_STRING:
                i = 0;
                String[] strings = new String[lstBase.size()];
                for (IBaseType b : lstBase) {
                    strings[i] = b.asString();
                    i++;
                }
                baseType = new StringType(name, strings);
                break;
            case ET_BYTE:
                i = 0;
                byte[] bytes = new byte[lstBase.size()];
                for (IBaseType b : lstBase) {
                    bytes[i] = b.asByte();
                    i++;
                }
                baseType = new ByteType(name, bytes);
                break;
            case ET_DATA:
                i = 0;
                byte[][] datas = new byte[lstBase.size()][];
                for (IBaseType b : lstBase) {
                    datas[i] = b.asData();
                    i++;
                }
                baseType = new DataType(name, datas);
                break;
            case ET_DATA_REF:
                i = 0;
                InstanceName[] references = new InstanceName[lstBase.size()];
                for (IBaseType b : lstBase) {
                    references[i] = b.asDataReference();
                    i++;
                }
                baseType = new DataReferenceType(name, references);
                break;
            default:
                throw new XMLStreamException("Invaid BaseType list item type for a BaseTypeList, a " + type);
        }
        return baseType;
    }

    /**
	 * Allocate a generic BaseTypeList (it can contain any other BaseType)
	 * @see allocate
	 * */
    public static List<BaseTypeXMLEvent> allocateList(StartElement startEvent, EventReader reader) throws XMLStreamException, InvalidMessageException {
        List<BaseTypeXMLEvent> typeList = new ArrayList<BaseTypeXMLEvent>();
        boolean openList = true;
        if (!startEvent.getName().equals(QN_BASE_TYPE_LIST)) {
            throw new XMLStreamException("BaseTypeXMLEvent try to allocate a list " + startEvent.getName());
        }
        while (openList) {
            XMLEvent2 event = reader.allocateNextEvent();
            if (event.isStartElement()) {
                typeList.add(BaseTypeXMLEvent.allocate(event.asStartElement(), reader));
            } else if (event.isEndElement()) {
                if (event.asEndElement().getName().equals(QN_BASE_TYPE_LIST)) {
                    openList = false;
                }
            } else {
                throw new XMLStreamException("Allocating BaseTypeList is not a Start/EndElement event, ist (see XMLStreamConstants) " + event.getEventType());
            }
        }
        return typeList;
    }
}
