package org.apache.harmony.x.print.ipp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

public class IppAttribute {

    public static final byte TAG_UNSUPPORTED = 0x10;

    public static final byte TAG_UNKNOWN = 0x12;

    public static final byte TAG_NO_VALUE = 0x13;

    public static final byte TAG_INTEGER = 0x21;

    public static final byte TAG_BOOLEAN = 0x22;

    public static final byte TAG_ENUM = 0x23;

    public static final byte TAG_OCTETSTRINGUNSPECIFIEDFORMAT = 0x30;

    public static final byte TAG_DATETIME = 0x31;

    public static final byte TAG_RESOLUTION = 0x32;

    public static final byte TAG_RANGEOFINTEGER = 0x33;

    public static final byte TAG_TEXTWITHLANGUAGE = 0x35;

    public static final byte TAG_NAMEWITHLANGUAGE = 0x36;

    public static final byte TAG_TEXTWITHOUTLANGUAGE = 0x41;

    public static final byte TAG_NAMEWITHOUTLANGUAGE = 0x42;

    public static final byte TAG_KEYWORD = 0x44;

    public static final byte TAG_URI = 0x45;

    public static final byte TAG_URISCHEME = 0x46;

    public static final byte TAG_CHARSET = 0x47;

    public static final byte TAG_NATURAL_LANGUAGE = 0x48;

    public static final byte TAG_MIMEMEDIATYPE = 0x49;

    public static String getTagName(byte atag) {
        String sz = "";
        switch(atag) {
            case TAG_BOOLEAN:
                sz = "BOOLEAN";
                break;
            case TAG_INTEGER:
                sz = "INTEGER";
                break;
            case TAG_ENUM:
                sz = "ENUM";
                break;
            case TAG_OCTETSTRINGUNSPECIFIEDFORMAT:
            case TAG_DATETIME:
            case TAG_RESOLUTION:
            case TAG_RANGEOFINTEGER:
            case TAG_TEXTWITHLANGUAGE:
            case TAG_NAMEWITHLANGUAGE:
                sz = "OCTETSTRING";
                break;
            case TAG_TEXTWITHOUTLANGUAGE:
            case TAG_NAMEWITHOUTLANGUAGE:
            case TAG_KEYWORD:
            case TAG_URI:
            case TAG_URISCHEME:
            case TAG_CHARSET:
            case TAG_NATURAL_LANGUAGE:
            case TAG_MIMEMEDIATYPE:
                sz = "CHARACTERSTRING";
                break;
            default:
                sz = "UNKNOWN_ATTRIBUTE_TAG";
                break;
        }
        return sz;
    }

    protected byte atag;

    protected byte[] aname;

    protected Vector avalue;

    public IppAttribute(byte tag, String name, int value) {
        atag = tag;
        aname = name.getBytes();
        avalue = new Vector();
        avalue.add(new Integer(value));
    }

    public IppAttribute(byte tag, String name, String value) {
        atag = tag;
        aname = name.getBytes();
        avalue = new Vector();
        avalue.add(value.getBytes());
    }

    public IppAttribute(byte tag, String name, byte[] value) {
        atag = tag;
        aname = name.getBytes();
        avalue = new Vector();
        avalue.add(value);
    }

    public IppAttribute(byte tag, String name, Vector value) {
        atag = tag;
        aname = name.getBytes();
        avalue = value;
    }

    public byte getTag() {
        return atag;
    }

    public byte[] getName() {
        return aname;
    }

    public Vector getValue() {
        return avalue;
    }

    public byte[] getBytes() {
        ByteArrayOutputStream bbuf = new ByteArrayOutputStream();
        DataOutputStream dbuf = new DataOutputStream(bbuf);
        byte[] bv;
        try {
            for (int ii = avalue.size(), i = 0; i < ii; i++) {
                dbuf.writeByte(atag);
                if (i == 0) {
                    dbuf.writeShort(aname.length);
                    dbuf.write(aname);
                } else {
                    dbuf.writeShort(0);
                }
                switch(atag) {
                    case TAG_BOOLEAN:
                        dbuf.writeShort(1);
                        dbuf.write(((Integer) avalue.get(i)).intValue());
                        break;
                    case TAG_INTEGER:
                    case TAG_ENUM:
                        dbuf.writeShort(4);
                        dbuf.writeInt(((Integer) avalue.get(i)).intValue());
                        break;
                    case TAG_OCTETSTRINGUNSPECIFIEDFORMAT:
                    case TAG_DATETIME:
                    case TAG_RESOLUTION:
                    case TAG_RANGEOFINTEGER:
                    case TAG_TEXTWITHLANGUAGE:
                    case TAG_NAMEWITHLANGUAGE:
                        bv = (byte[]) avalue.get(i);
                        dbuf.writeShort(bv.length);
                        dbuf.write(bv);
                        break;
                    case TAG_TEXTWITHOUTLANGUAGE:
                    case TAG_NAMEWITHOUTLANGUAGE:
                    case TAG_KEYWORD:
                    case TAG_URI:
                    case TAG_URISCHEME:
                    case TAG_CHARSET:
                    case TAG_NATURAL_LANGUAGE:
                    case TAG_MIMEMEDIATYPE:
                        bv = (byte[]) avalue.get(i);
                        dbuf.writeShort(bv.length);
                        dbuf.write(bv);
                        break;
                    default:
                        break;
                }
            }
            dbuf.flush();
            dbuf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bbuf.toByteArray();
    }

    public String toString() {
        ByteArrayOutputStream bbuf = new ByteArrayOutputStream();
        DataOutputStream dbuf = new DataOutputStream(bbuf);
        try {
            dbuf.writeBytes("attribute tag: 0x" + Integer.toHexString(atag) + "(" + getTagName(atag) + ")" + "\n");
            dbuf.writeBytes("attribute name: " + new String(aname) + "\n");
            switch(atag) {
                case TAG_INTEGER:
                case TAG_BOOLEAN:
                case TAG_ENUM:
                    for (int ii = avalue.size(), i = 0; i < ii; i++) {
                        Integer v = (Integer) avalue.get(i);
                        dbuf.writeBytes(v.toString() + "\n");
                    }
                    break;
                case TAG_OCTETSTRINGUNSPECIFIEDFORMAT:
                case TAG_RESOLUTION:
                case TAG_TEXTWITHLANGUAGE:
                case TAG_NAMEWITHLANGUAGE:
                    for (int ii = avalue.size(), i = 0; i < ii; i++) {
                        byte[] bv = (byte[]) avalue.get(i);
                        dbuf.writeBytes(new String(bv) + "\n");
                    }
                    break;
                case TAG_DATETIME:
                    for (int ii = avalue.size(), i = 0; i < ii; i++) {
                        byte[] bv = (byte[]) avalue.get(i);
                        ByteArrayInputStream bi = new ByteArrayInputStream(bv);
                        DataInputStream di = new DataInputStream(bi);
                        dbuf.writeBytes(Integer.toString(di.readShort()) + "-" + Integer.toString(di.readByte()) + "-" + Integer.toString(di.readByte()) + "," + Integer.toString(di.readByte()) + ":" + Integer.toString(di.readByte()) + ":" + Integer.toString(di.readByte()) + "." + Integer.toString(di.readByte()) + "," + (char) di.readByte() + Integer.toString(di.readByte()) + ":" + Integer.toString(di.readByte()) + "\n");
                    }
                    break;
                case TAG_RANGEOFINTEGER:
                    for (int ii = avalue.size(), i = 0; i < ii; i++) {
                        byte[] bv = (byte[]) avalue.get(i);
                        ByteArrayInputStream bi = new ByteArrayInputStream(bv);
                        DataInputStream di = new DataInputStream(bi);
                        dbuf.writeBytes(Integer.toString(di.readInt()) + "..." + Integer.toString(di.readInt()) + "\n");
                    }
                    break;
                case TAG_TEXTWITHOUTLANGUAGE:
                case TAG_NAMEWITHOUTLANGUAGE:
                case TAG_KEYWORD:
                case TAG_URI:
                case TAG_URISCHEME:
                case TAG_CHARSET:
                case TAG_NATURAL_LANGUAGE:
                case TAG_MIMEMEDIATYPE:
                    for (int ii = avalue.size(), i = 0; i < ii; i++) {
                        byte[] bv = (byte[]) avalue.get(i);
                        dbuf.writeBytes(new String(bv) + "\n");
                    }
                    break;
                default:
                    for (int ii = avalue.size(), i = 0; i < ii; i++) {
                    }
                    break;
            }
            dbuf.flush();
            dbuf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bbuf.toString();
    }
}
