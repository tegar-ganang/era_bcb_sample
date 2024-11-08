package org.racsor.jmeter.flex.messaging.io.amf;

import java.io.IOException;
import java.io.UTFDataFormatException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Amf0Input extends AbstractAmfInput implements AmfTypes {

    protected ActionMessageInput avmPlusInput;

    protected List objectsTable;

    public Amf0Input() {
        objectsTable = new ArrayList(64);
    }

    /**
	 * Clear all object reference information so that the instance can be used
	 * to deserialize another data structure.
	 * 
	 * Reset should be called before reading a top level object, such as a new
	 * header or a new body.
	 */
    public void reset() {
        super.reset();
        objectsTable.clear();
        if (avmPlusInput != null) avmPlusInput.reset();
    }

    /**
	 * Public entry point to read a top level AMF Object, such as a header value
	 * or a message body.
	 */
    public Object readObject() {
        int type;
        try {
            type = in.readByte();
            Object value = readObjectValue(type);
            return value;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    protected Object readObjectValue(int type) throws Exception {
        Object value = null;
        switch(type) {
            case kNumberType:
                double d = readDouble();
                if (isDebug) trace.write(d);
                value = new Double(d);
                break;
            case kBooleanType:
                value = Boolean.valueOf(readBoolean());
                if (isDebug) trace.write(value);
                break;
            case kStringType:
                value = readString();
                break;
            case kAvmPlusObjectType:
                if (avmPlusInput == null) {
                    avmPlusInput = new Amf3Input();
                    avmPlusInput.setDebugTrace(trace);
                    avmPlusInput.setInputStream(in);
                }
                value = avmPlusInput.readObject();
                break;
            case kStrictArrayType:
                value = readArrayValue();
                break;
            case kTypedObjectType:
                String typeName = in.readUTF();
                value = readScriptObject(typeName);
                break;
            case kLongStringType:
                value = readLongUTF();
                if (isDebug) trace.writeString((String) value);
                break;
            case kObjectType:
                value = readScriptObject(null);
                break;
            case kXMLObjectType:
                value = readXml();
                break;
            case kNullType:
                if (isDebug) trace.writeNull();
                break;
            case kDateType:
                value = readDate();
                break;
            case kECMAArrayType:
                value = readECMAArrayValue();
                break;
            case kReferenceType:
                int refNum = in.readUnsignedShort();
                if (isDebug) trace.writeRef(refNum);
                value = objectsTable.get(refNum);
                break;
            case kUndefinedType:
                if (isDebug) trace.writeUndefined();
                break;
            case kUnsupportedType:
                if (isDebug) trace.write("UNSUPPORTED");
                RuntimeException ex2 = new RuntimeException("10302: Unsupported type found in AMF stream.");
                throw ex2;
            case kObjectEndType:
                if (isDebug) trace.write("UNEXPECTED OBJECT END");
                RuntimeException ex3 = new RuntimeException("10302: Unsupported type found in AMF stream.");
                throw ex3;
            case kRecordsetType:
                if (isDebug) trace.write("UNEXPECTED RECORDSET");
                RuntimeException ex4 = new RuntimeException("1034: AMF Recordsets are not supported.");
                throw ex4;
            default:
                if (isDebug) trace.write("UNKNOWN TYPE");
                RuntimeException ex5 = new RuntimeException("10301: Unknown type: " + type + ".");
                throw ex5;
        }
        return value;
    }

    protected Date readDate() throws IOException {
        long time = (long) in.readDouble();
        in.readShort();
        Date d = new Date(time);
        if (isDebug) trace.write(d.toString());
        return d;
    }

    /**
	 * Deserialize the bits of an ECMA array w/o a prefixing type byte.
	 * @throws Exception 
	 */
    protected Map readECMAArrayValue() throws Exception {
        int size = in.readInt();
        HashMap h;
        if (size == 0) h = new HashMap(); else h = new HashMap(size);
        rememberObject(h);
        if (isDebug) trace.startECMAArray(objectsTable.size() - 1);
        String name = in.readUTF();
        int type = in.readByte();
        while (type != kObjectEndType) {
            if (type != kObjectEndType) {
                if (isDebug) trace.namedElement(name);
                Object value = readObjectValue(type);
                if (!name.equals("length")) h.put(name, value);
            }
            name = in.readUTF();
            type = in.readByte();
        }
        if (isDebug) trace.endAMFArray();
        return h;
    }

    protected String readString() throws IOException {
        String s = readUTF();
        if (isDebug) trace.writeString(s);
        return s;
    }

    /**
	 * Deserialize the bits of an array w/o a prefixing type byte.
	 * @throws Exception 
	 */
    protected Object readArrayValue() throws Exception {
        int size = in.readInt();
        ArrayList l = new ArrayList(size);
        rememberObject(l);
        if (isDebug) trace.startAMFArray(objectsTable.size() - 1);
        for (int i = 0; i < size; ++i) {
            int type = in.readByte();
            if (isDebug) trace.arrayElement(i);
            l.add(readObjectValue(type));
        }
        if (isDebug) trace.endAMFArray();
        return l;
    }

    /**
	 * Deserialize the bits of a map w/o a prefixing type byte. Method named
	 * changed for AMF Explorer.
	 * @throws Exception 
	 */
    public Object readScriptObject(String className) throws Exception {
        Object object;
        if (className.equalsIgnoreCase("undefined") || className == null || className.length() == 0) {
            object = new ASObject();
        } else {
            object = new ASObject();
            ((ASObject) object).setType(className);
        }
        int objectId = this.rememberObject(object);
        if (isDebug) trace.writeString("amf0Input.readScriptObject.startAMFObject" + className);
        String propertyName = in.readUTF();
        byte type = in.readByte();
        while (type != kObjectEndType) {
            if (isDebug) trace.writeString("amf0Input.readScriptObject.namedElement" + propertyName);
            Object value = this.readObjectValue(type);
            ((ASObject) object).put(propertyName, value);
            propertyName = in.readUTF();
            type = in.readByte();
        }
        if (isDebug) trace.writeString("amf0Input.readScriptObject.endAMFObject");
        return object;
    }

    /**
	 * Reads in a string that has been encoded using a modified UTF-8 format
	 * with a 32-bit string length.
	 */
    protected String readLongUTF() throws IOException {
        int utflen = in.readInt();
        int c, char2, char3;
        char[] charr = getTempCharArray(utflen);
        byte bytearr[] = getTempByteArray(utflen);
        int count = 0;
        int chCount = 0;
        in.readFully(bytearr, 0, utflen);
        while (count < utflen) {
            c = (int) bytearr[count] & 0xff;
            switch(c >> 4) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    count++;
                    charr[chCount] = (char) c;
                    break;
                case 12:
                case 13:
                    count += 2;
                    if (count > utflen) throw new UTFDataFormatException();
                    char2 = (int) bytearr[count - 1];
                    if ((char2 & 0xC0) != 0x80) throw new UTFDataFormatException();
                    charr[chCount] = (char) (((c & 0x1F) << 6) | (char2 & 0x3F));
                    break;
                case 14:
                    count += 3;
                    if (count > utflen) throw new UTFDataFormatException();
                    char2 = (int) bytearr[count - 2];
                    char3 = (int) bytearr[count - 1];
                    if (((char2 & 0xC0) != 0x80) || ((char3 & 0xC0) != 0x80)) throw new UTFDataFormatException();
                    charr[chCount] = (char) (((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | ((char3 & 0x3F) << 0));
                    break;
                default:
                    throw new UTFDataFormatException();
            }
            chCount++;
        }
        return new String(charr, 0, chCount);
    }

    protected Object readXml() throws IOException {
        String xml = readLongUTF();
        if (isDebug) trace.write(xml);
        return stringToDocument(xml);
    }

    /**
	 * Remember a deserialized object so that you can use it later through a
	 * reference.
	 */
    protected int rememberObject(Object obj) {
        int id = objectsTable.size();
        objectsTable.add(obj);
        return id;
    }
}
