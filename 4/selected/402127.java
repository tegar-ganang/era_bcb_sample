package fr.soleil.snapArchivingApi.SnapshotingTools.Tools;

import fr.esrf.Tango.AttrDataFormat;
import fr.esrf.Tango.AttrWriteType;
import fr.esrf.TangoApi.DeviceData;
import fr.esrf.TangoDs.TangoConst;

public class SnapAttributeExtract extends SnapAttribute {

    private int dimX;

    public SnapAttributeExtract(String[] argin) {
        setAttribute_complete_name(argin[0]);
        setId_att(Integer.parseInt(argin[1]));
        data_type = Integer.parseInt(argin[2]);
        data_format = Integer.parseInt(argin[3]);
        writable = Integer.parseInt(argin[4]);
        Object value = new Object();
        switch(data_format) {
            case AttrDataFormat._SCALAR:
                switch(writable) {
                    case AttrWriteType._READ:
                        value = new Object();
                        switch(this.data_type) {
                            case TangoConst.Tango_DEV_STRING:
                                value = new String((String) argin[5]);
                                break;
                            case TangoConst.Tango_DEV_STATE:
                                value = new Integer((String) argin[5]);
                                break;
                            case TangoConst.Tango_DEV_UCHAR:
                                value = new Byte((String) argin[5]);
                                break;
                            case TangoConst.Tango_DEV_LONG:
                                value = new Integer((String) argin[5]);
                                break;
                            case TangoConst.Tango_DEV_ULONG:
                                value = new Integer((String) argin[5]);
                                break;
                            case TangoConst.Tango_DEV_BOOLEAN:
                                value = new Boolean((String) argin[5]);
                                break;
                            case TangoConst.Tango_DEV_SHORT:
                                value = new Short((String) argin[5]);
                                break;
                            case TangoConst.Tango_DEV_FLOAT:
                                value = new Float((String) argin[5]);
                                break;
                            case TangoConst.Tango_DEV_DOUBLE:
                                value = new Double((String) argin[5]);
                                break;
                            default:
                                value = new Double((String) argin[5]);
                                break;
                        }
                        break;
                    case AttrWriteType._READ_WITH_WRITE:
                        value = new Object[2];
                        switch(this.data_type) {
                            case TangoConst.Tango_DEV_STRING:
                                String[] valueString = { new String(argin[5]), new String(argin[6]) };
                                value = valueString;
                                break;
                            case TangoConst.Tango_DEV_STATE:
                                Integer[] valueInteger = { new Integer(argin[5]), new Integer(argin[6]) };
                                value = valueInteger;
                                break;
                            case TangoConst.Tango_DEV_UCHAR:
                                Byte[] valueByte = { new Byte(argin[5]), new Byte(argin[6]) };
                                value = valueByte;
                                break;
                            case TangoConst.Tango_DEV_LONG:
                                Integer[] valueLong = { new Integer(argin[5]), new Integer(argin[6]) };
                                value = valueLong;
                                break;
                            case TangoConst.Tango_DEV_ULONG:
                                Integer[] valueULong = { new Integer(argin[5]), new Integer(argin[6]) };
                                value = valueULong;
                                break;
                            case TangoConst.Tango_DEV_BOOLEAN:
                                Boolean[] valueBoolean = { new Boolean(argin[5]), new Boolean(argin[6]) };
                                value = valueBoolean;
                                break;
                            case TangoConst.Tango_DEV_SHORT:
                                Short[] valueShort = { new Short(argin[5]), new Short(argin[6]) };
                                value = valueShort;
                                break;
                            case TangoConst.Tango_DEV_FLOAT:
                                Float[] valueFloat = { new Float(argin[5]), new Float(argin[6]) };
                                value = valueFloat;
                                break;
                            case TangoConst.Tango_DEV_DOUBLE:
                                Double[] valueDouble = { new Double(argin[5]), new Double(argin[6]) };
                                value = valueDouble;
                                break;
                            default:
                                Double[] valueDouble2 = { new Double(argin[5]), new Double(argin[6]) };
                                value = valueDouble2;
                                break;
                        }
                        break;
                    case AttrWriteType._WRITE:
                        value = new Object();
                        switch(this.data_type) {
                            case TangoConst.Tango_DEV_STRING:
                                value = new String((String) argin[6]);
                                break;
                            case TangoConst.Tango_DEV_STATE:
                                value = new Integer((String) argin[6]);
                                break;
                            case TangoConst.Tango_DEV_UCHAR:
                                value = new Byte((String) argin[6]);
                                break;
                            case TangoConst.Tango_DEV_LONG:
                                value = new Integer((String) argin[6]);
                                break;
                            case TangoConst.Tango_DEV_ULONG:
                                value = new Integer((String) argin[6]);
                                break;
                            case TangoConst.Tango_DEV_BOOLEAN:
                                value = new Boolean((String) argin[6]);
                                break;
                            case TangoConst.Tango_DEV_SHORT:
                                value = new Short((String) argin[6]);
                                break;
                            case TangoConst.Tango_DEV_FLOAT:
                                value = new Float((String) argin[6]);
                                break;
                            case TangoConst.Tango_DEV_DOUBLE:
                                value = new Double((String) argin[6]);
                                break;
                            default:
                                value = new Double((String) argin[6]);
                                break;
                        }
                        break;
                    case AttrWriteType._READ_WRITE:
                        value = new Object[2];
                        switch(this.data_type) {
                            case TangoConst.Tango_DEV_STRING:
                                String[] valueString = { new String(argin[5]), new String(argin[6]) };
                                value = valueString;
                                break;
                            case TangoConst.Tango_DEV_STATE:
                                Integer[] valueInteger = { new Integer(argin[5]), new Integer(argin[6]) };
                                value = valueInteger;
                                break;
                            case TangoConst.Tango_DEV_UCHAR:
                                Byte[] valueByte = { new Byte(argin[5]), new Byte(argin[6]) };
                                value = valueByte;
                                break;
                            case TangoConst.Tango_DEV_LONG:
                                Integer[] valueLong = { new Integer(argin[5]), new Integer(argin[6]) };
                                value = valueLong;
                                break;
                            case TangoConst.Tango_DEV_ULONG:
                                Integer[] valueULong = { new Integer(argin[5]), new Integer(argin[6]) };
                                value = valueULong;
                                break;
                            case TangoConst.Tango_DEV_BOOLEAN:
                                Boolean[] valueBoolean = { new Boolean(argin[5]), new Boolean(argin[6]) };
                                value = valueBoolean;
                                break;
                            case TangoConst.Tango_DEV_SHORT:
                                Short[] valueShort = { new Short(argin[5]), new Short(argin[6]) };
                                value = valueShort;
                                break;
                            case TangoConst.Tango_DEV_FLOAT:
                                Float[] valueFloat = { new Float(argin[5]), new Float(argin[6]) };
                                value = valueFloat;
                                break;
                            case TangoConst.Tango_DEV_DOUBLE:
                                Double[] valueDouble = { new Double(argin[5]), new Double(argin[6]) };
                                value = valueDouble;
                                break;
                            default:
                                Double[] valueDouble2 = { new Double(argin[5]), new Double(argin[6]) };
                                value = valueDouble2;
                                break;
                        }
                        break;
                }
                break;
            case AttrDataFormat._SPECTRUM:
                String toSplitRead = argin[5];
                String toSplitWrite = argin[6];
                String[] stringArrayRead;
                String[] stringArrayWrite;
                switch(writable) {
                    case AttrWriteType._WRITE:
                        if (toSplitWrite == null || "NaN".equalsIgnoreCase(toSplitWrite.trim()) || "[]".equalsIgnoreCase(toSplitWrite.trim())) {
                            value = "NaN";
                            break;
                        }
                        stringArrayWrite = toSplitWrite.substring(1, toSplitWrite.length() - 1).split(GlobalConst.CLOB_SEPARATOR);
                        switch(this.data_type) {
                            case TangoConst.Tango_DEV_BOOLEAN:
                                value = new Boolean[stringArrayWrite.length];
                                for (int i = 0; i < stringArrayWrite.length; i++) {
                                    try {
                                        ((Boolean[]) value)[i] = new Boolean(Double.valueOf(stringArrayWrite[i]).byteValue() != 0);
                                    } catch (NumberFormatException n) {
                                        ((Boolean[]) value)[i] = new Boolean("true".equalsIgnoreCase(stringArrayWrite[i]));
                                    }
                                }
                                break;
                            case TangoConst.Tango_DEV_STRING:
                                value = new String[stringArrayWrite.length];
                                for (int i = 0; i < stringArrayWrite.length; i++) {
                                    ((String[]) value)[i] = new String(stringArrayWrite[i]);
                                }
                                break;
                            case TangoConst.Tango_DEV_CHAR:
                            case TangoConst.Tango_DEV_UCHAR:
                                value = new Byte[stringArrayWrite.length];
                                for (int i = 0; i < stringArrayWrite.length; i++) {
                                    try {
                                        ((Byte[]) value)[i] = Byte.valueOf(stringArrayWrite[i]);
                                    } catch (NumberFormatException n) {
                                        ((Byte[]) value)[i] = new Byte(Double.valueOf(stringArrayWrite[i]).byteValue());
                                    }
                                }
                                break;
                            case TangoConst.Tango_DEV_LONG:
                            case TangoConst.Tango_DEV_ULONG:
                                value = new Integer[stringArrayWrite.length];
                                for (int i = 0; i < stringArrayWrite.length; i++) {
                                    try {
                                        ((Integer[]) value)[i] = Integer.valueOf(stringArrayWrite[i]);
                                    } catch (NumberFormatException n) {
                                        ((Integer[]) value)[i] = new Integer(Double.valueOf(stringArrayWrite[i]).intValue());
                                    }
                                }
                                break;
                            case TangoConst.Tango_DEV_USHORT:
                            case TangoConst.Tango_DEV_SHORT:
                                value = new Short[stringArrayWrite.length];
                                for (int i = 0; i < stringArrayWrite.length; i++) {
                                    try {
                                        ((Short[]) value)[i] = Short.valueOf(stringArrayWrite[i]);
                                    } catch (NumberFormatException n) {
                                        ((Short[]) value)[i] = new Short(Double.valueOf(stringArrayWrite[i]).shortValue());
                                    }
                                }
                                break;
                            case TangoConst.Tango_DEV_FLOAT:
                                value = new Float[stringArrayWrite.length];
                                for (int i = 0; i < stringArrayWrite.length; i++) {
                                    ((Float[]) value)[i] = Float.valueOf(stringArrayWrite[i]);
                                }
                                break;
                            case TangoConst.Tango_DEV_DOUBLE:
                                value = new Double[stringArrayWrite.length];
                                for (int i = 0; i < stringArrayWrite.length; i++) {
                                    ((Double[]) value)[i] = Double.valueOf(stringArrayWrite[i]);
                                }
                                break;
                            default:
                                value = "NaN";
                        }
                        break;
                    case AttrWriteType._READ:
                        if (toSplitRead == null || "NaN".equalsIgnoreCase(toSplitRead.trim()) || "[]".equalsIgnoreCase(toSplitRead.trim())) {
                            value = "NaN";
                            break;
                        }
                        stringArrayRead = toSplitRead.substring(1, toSplitRead.length() - 1).split(GlobalConst.CLOB_SEPARATOR);
                        switch(this.data_type) {
                            case TangoConst.Tango_DEV_BOOLEAN:
                                value = new Boolean[stringArrayRead.length];
                                for (int i = 0; i < stringArrayRead.length; i++) {
                                    try {
                                        ((Boolean[]) value)[i] = new Boolean(Double.valueOf(stringArrayRead[i]).byteValue() != 0);
                                    } catch (NumberFormatException n) {
                                        ((Boolean[]) value)[i] = new Boolean("true".equalsIgnoreCase(stringArrayRead[i]));
                                    }
                                }
                                break;
                            case TangoConst.Tango_DEV_STRING:
                                value = new String[stringArrayRead.length];
                                for (int i = 0; i < stringArrayRead.length; i++) {
                                    ((String[]) value)[i] = new String(stringArrayRead[i]);
                                }
                                break;
                            case TangoConst.Tango_DEV_CHAR:
                            case TangoConst.Tango_DEV_UCHAR:
                                value = new Byte[stringArrayRead.length];
                                for (int i = 0; i < stringArrayRead.length; i++) {
                                    try {
                                        ((Byte[]) value)[i] = Byte.valueOf(stringArrayRead[i]);
                                    } catch (NumberFormatException n) {
                                        ((Byte[]) value)[i] = new Byte(Double.valueOf(stringArrayRead[i]).byteValue());
                                    }
                                }
                                break;
                            case TangoConst.Tango_DEV_STATE:
                            case TangoConst.Tango_DEV_LONG:
                            case TangoConst.Tango_DEV_ULONG:
                                value = new Integer[stringArrayRead.length];
                                for (int i = 0; i < stringArrayRead.length; i++) {
                                    try {
                                        ((Integer[]) value)[i] = Integer.valueOf(stringArrayRead[i]);
                                    } catch (NumberFormatException n) {
                                        ((Integer[]) value)[i] = new Integer(Double.valueOf(stringArrayRead[i]).intValue());
                                    }
                                }
                                break;
                            case TangoConst.Tango_DEV_USHORT:
                            case TangoConst.Tango_DEV_SHORT:
                                value = new Short[stringArrayRead.length];
                                for (int i = 0; i < stringArrayRead.length; i++) {
                                    try {
                                        ((Short[]) value)[i] = Short.valueOf(stringArrayRead[i]);
                                    } catch (NumberFormatException n) {
                                        ((Short[]) value)[i] = new Short(Double.valueOf(stringArrayRead[i]).shortValue());
                                    }
                                }
                                break;
                            case TangoConst.Tango_DEV_FLOAT:
                                value = new Float[stringArrayRead.length];
                                for (int i = 0; i < stringArrayRead.length; i++) {
                                    ((Float[]) value)[i] = Float.valueOf(stringArrayRead[i]);
                                }
                                break;
                            case TangoConst.Tango_DEV_DOUBLE:
                                value = new Double[stringArrayRead.length];
                                for (int i = 0; i < stringArrayRead.length; i++) {
                                    ((Double[]) value)[i] = Double.valueOf(stringArrayRead[i]);
                                }
                                break;
                            default:
                                value = "NaN";
                        }
                        break;
                    case AttrWriteType._READ_WRITE:
                    case AttrWriteType._READ_WITH_WRITE:
                        if (toSplitWrite == null || "NaN".equalsIgnoreCase(toSplitWrite.trim()) || "[]".equalsIgnoreCase(toSplitWrite.trim())) {
                            value = "NaN";
                            break;
                        }
                        if (toSplitRead == null || "NaN".equalsIgnoreCase(toSplitRead.trim()) || "[]".equalsIgnoreCase(toSplitRead.trim())) {
                            value = "NaN";
                            break;
                        }
                        stringArrayRead = toSplitRead.substring(1, toSplitRead.length() - 1).split(GlobalConst.CLOB_SEPARATOR);
                        stringArrayWrite = toSplitWrite.substring(1, toSplitWrite.length() - 1).split(GlobalConst.CLOB_SEPARATOR);
                        value = new Object[2];
                        switch(this.data_type) {
                            case TangoConst.Tango_DEV_BOOLEAN:
                                ((Object[]) value)[0] = new Boolean[stringArrayRead.length];
                                ((Object[]) value)[1] = new Boolean[stringArrayWrite.length];
                                for (int i = 0; i < stringArrayRead.length; i++) {
                                    try {
                                        ((Boolean[]) ((((Object[]) value))[0]))[i] = new Boolean(Double.valueOf(stringArrayRead[i]).byteValue() != 0);
                                    } catch (NumberFormatException n) {
                                        ((Boolean[]) ((((Object[]) value))[0]))[i] = new Boolean("true".equalsIgnoreCase(stringArrayRead[i]));
                                    }
                                }
                                for (int i = 0; i < stringArrayWrite.length; i++) {
                                    try {
                                        ((Boolean[]) ((((Object[]) value))[1]))[i] = new Boolean(Double.valueOf(stringArrayWrite[i]).byteValue() != 0);
                                    } catch (NumberFormatException n) {
                                        ((Boolean[]) ((((Object[]) value))[1]))[i] = new Boolean("true".equalsIgnoreCase(stringArrayWrite[i]));
                                    }
                                }
                                break;
                            case TangoConst.Tango_DEV_STRING:
                                ((Object[]) value)[0] = new String[stringArrayRead.length];
                                ((Object[]) value)[1] = new String[stringArrayWrite.length];
                                for (int i = 0; i < stringArrayRead.length; i++) {
                                    ((String[]) ((((Object[]) value))[0]))[i] = new String(stringArrayRead[i]);
                                }
                                for (int i = 0; i < stringArrayWrite.length; i++) {
                                    ((String[]) ((((Object[]) value))[1]))[i] = new String(stringArrayWrite[i]);
                                }
                                break;
                            case TangoConst.Tango_DEV_CHAR:
                            case TangoConst.Tango_DEV_UCHAR:
                                ((Object[]) value)[0] = new Byte[stringArrayRead.length];
                                ((Object[]) value)[1] = new Byte[stringArrayWrite.length];
                                for (int i = 0; i < stringArrayRead.length; i++) {
                                    try {
                                        ((Byte[]) ((((Object[]) value))[0]))[i] = Byte.valueOf(stringArrayRead[i]);
                                    } catch (NumberFormatException n) {
                                        ((Byte[]) ((((Object[]) value))[0]))[i] = new Byte(Double.valueOf(stringArrayRead[i]).byteValue());
                                    }
                                }
                                for (int i = 0; i < stringArrayWrite.length; i++) {
                                    try {
                                        ((Byte[]) ((((Object[]) value))[1]))[i] = Byte.valueOf(stringArrayWrite[i]);
                                    } catch (NumberFormatException n) {
                                        ((Byte[]) ((((Object[]) value))[1]))[i] = new Byte(Double.valueOf(stringArrayWrite[i]).byteValue());
                                    }
                                }
                                break;
                            case TangoConst.Tango_DEV_LONG:
                            case TangoConst.Tango_DEV_ULONG:
                                ((Object[]) value)[0] = new Integer[stringArrayRead.length];
                                ((Object[]) value)[1] = new Integer[stringArrayWrite.length];
                                for (int i = 0; i < stringArrayRead.length; i++) {
                                    try {
                                        ((Integer[]) ((((Object[]) value))[0]))[i] = Integer.valueOf(stringArrayRead[i]);
                                    } catch (NumberFormatException n) {
                                        ((Integer[]) ((((Object[]) value))[0]))[i] = new Integer(Double.valueOf(stringArrayRead[i]).intValue());
                                    }
                                }
                                for (int i = 0; i < stringArrayWrite.length; i++) {
                                    try {
                                        ((Integer[]) ((((Object[]) value))[1]))[i] = Integer.valueOf(stringArrayWrite[i]);
                                    } catch (NumberFormatException n) {
                                        ((Integer[]) ((((Object[]) value))[1]))[i] = new Integer(Double.valueOf(stringArrayWrite[i]).intValue());
                                    }
                                }
                                break;
                            case TangoConst.Tango_DEV_USHORT:
                            case TangoConst.Tango_DEV_SHORT:
                                ((Object[]) value)[0] = new Short[stringArrayRead.length];
                                ((Object[]) value)[1] = new Short[stringArrayWrite.length];
                                for (int i = 0; i < stringArrayRead.length; i++) {
                                    try {
                                        ((Short[]) ((((Object[]) value))[0]))[i] = Short.valueOf(stringArrayRead[i]);
                                    } catch (NumberFormatException n) {
                                        ((Short[]) ((((Object[]) value))[0]))[i] = new Short(Double.valueOf(stringArrayRead[i]).shortValue());
                                    }
                                }
                                for (int i = 0; i < stringArrayWrite.length; i++) {
                                    try {
                                        ((Short[]) ((((Object[]) value))[1]))[i] = Short.valueOf(stringArrayWrite[i]);
                                    } catch (NumberFormatException n) {
                                        ((Short[]) ((((Object[]) value))[1]))[i] = new Short(Double.valueOf(stringArrayWrite[i]).shortValue());
                                    }
                                }
                                break;
                            case TangoConst.Tango_DEV_FLOAT:
                                ((Object[]) value)[0] = new Float[stringArrayRead.length];
                                ((Object[]) value)[1] = new Float[stringArrayWrite.length];
                                for (int i = 0; i < stringArrayRead.length; i++) {
                                    ((Float[]) ((((Object[]) value))[0]))[i] = Float.valueOf(stringArrayRead[i]);
                                }
                                for (int i = 0; i < stringArrayWrite.length; i++) {
                                    ((Float[]) ((((Object[]) value))[1]))[i] = Float.valueOf(stringArrayWrite[i]);
                                }
                                break;
                            case TangoConst.Tango_DEV_DOUBLE:
                                ((Object[]) value)[0] = new Double[stringArrayRead.length];
                                ((Object[]) value)[1] = new Double[stringArrayWrite.length];
                                for (int i = 0; i < stringArrayRead.length; i++) {
                                    ((Double[]) ((((Object[]) value))[0]))[i] = Double.valueOf(stringArrayRead[i]);
                                }
                                for (int i = 0; i < stringArrayWrite.length; i++) {
                                    ((Double[]) ((((Object[]) value))[1]))[i] = Double.valueOf(stringArrayWrite[i]);
                                }
                                break;
                            default:
                                value = "NaN";
                        }
                        break;
                }
                break;
            default:
                value = "NaN";
        }
        setValue(value);
    }

    public SnapAttributeExtract(SnapAttributeLight snapAttributeLight) {
        super.setAttribute_complete_name(snapAttributeLight.getAttribute_complete_name());
        super.setId_att(snapAttributeLight.getAttribute_id());
        data_format = snapAttributeLight.getData_format();
        data_type = snapAttributeLight.getData_type();
        writable = snapAttributeLight.getWritable();
    }

    public String valueToString(int pos) {
        String nullvalue = "NULL";
        String value = nullvalue;
        if (getValue() == null) {
            return nullvalue;
        }
        if (getValue() instanceof Object[]) {
            Object[] valTab = (Object[]) getValue();
            if (valTab[pos] == null) {
                return nullvalue;
            }
        }
        switch(data_format) {
            case AttrDataFormat._SCALAR:
                switch(writable) {
                    case AttrWriteType._READ:
                        if (pos == 0) {
                            switch(this.data_type) {
                                case TangoConst.Tango_DEV_STRING:
                                    value = (String) getValue();
                                    break;
                                case TangoConst.Tango_DEV_STATE:
                                    value = ((Integer) getValue()).toString();
                                    break;
                                case TangoConst.Tango_DEV_UCHAR:
                                    value = ((Byte) getValue()).toString();
                                    break;
                                case TangoConst.Tango_DEV_LONG:
                                    value = ((Integer) getValue()).toString();
                                    break;
                                case TangoConst.Tango_DEV_ULONG:
                                    value = ((Integer) getValue()).toString();
                                    break;
                                case TangoConst.Tango_DEV_BOOLEAN:
                                    value = ((Boolean) getValue()).toString();
                                    break;
                                case TangoConst.Tango_DEV_SHORT:
                                    value = ((Short) getValue()).toString();
                                    break;
                                case TangoConst.Tango_DEV_FLOAT:
                                    value = ((Float) getValue()).toString();
                                    break;
                                case TangoConst.Tango_DEV_DOUBLE:
                                    value = ((Double) getValue()).toString();
                                    break;
                                default:
                                    value = ((Double) getValue()).toString();
                                    break;
                            }
                        }
                        break;
                    case AttrWriteType._READ_WITH_WRITE:
                        switch(this.data_type) {
                            case TangoConst.Tango_DEV_STRING:
                                value = ((String) ((Object[]) getValue())[pos]).toString();
                                break;
                            case TangoConst.Tango_DEV_STATE:
                                value = ((Integer) ((Object[]) getValue())[pos]).toString();
                                break;
                            case TangoConst.Tango_DEV_UCHAR:
                                value = ((Byte) ((Object[]) getValue())[pos]).toString();
                                break;
                            case TangoConst.Tango_DEV_LONG:
                                value = ((Integer) ((Object[]) getValue())[pos]).toString();
                                break;
                            case TangoConst.Tango_DEV_ULONG:
                                value = ((Integer) ((Object[]) getValue())[pos]).toString();
                                break;
                            case TangoConst.Tango_DEV_BOOLEAN:
                                value = ((Boolean) ((Object[]) getValue())[pos]).toString();
                                break;
                            case TangoConst.Tango_DEV_SHORT:
                                value = ((Short) ((Object[]) getValue())[pos]).toString();
                                break;
                            case TangoConst.Tango_DEV_FLOAT:
                                value = ((Float) ((Object[]) getValue())[pos]).toString();
                                break;
                            case TangoConst.Tango_DEV_DOUBLE:
                                value = ((Double) ((Object[]) getValue())[pos]).toString();
                                break;
                            default:
                                value = ((Double) ((Object[]) getValue())[pos]).toString();
                                break;
                        }
                        break;
                    case AttrWriteType._WRITE:
                        if (pos == 1) {
                            switch(this.data_type) {
                                case TangoConst.Tango_DEV_STRING:
                                    value = (String) getValue();
                                    break;
                                case TangoConst.Tango_DEV_STATE:
                                    value = ((Integer) getValue()).toString();
                                    break;
                                case TangoConst.Tango_DEV_UCHAR:
                                    value = ((Byte) getValue()).toString();
                                    break;
                                case TangoConst.Tango_DEV_LONG:
                                    value = ((Integer) getValue()).toString();
                                    break;
                                case TangoConst.Tango_DEV_ULONG:
                                    value = ((Integer) getValue()).toString();
                                    break;
                                case TangoConst.Tango_DEV_BOOLEAN:
                                    value = ((Boolean) getValue()).toString();
                                    break;
                                case TangoConst.Tango_DEV_SHORT:
                                    value = ((Short) getValue()).toString();
                                    break;
                                case TangoConst.Tango_DEV_FLOAT:
                                    value = ((Float) getValue()).toString();
                                    break;
                                case TangoConst.Tango_DEV_DOUBLE:
                                    value = ((Double) getValue()).toString();
                                    break;
                                default:
                                    value = ((Double) getValue()).toString();
                                    break;
                            }
                        }
                        break;
                    case AttrWriteType._READ_WRITE:
                        switch(this.data_type) {
                            case TangoConst.Tango_DEV_STRING:
                                value = ((String) ((Object[]) getValue())[pos]).toString();
                                break;
                            case TangoConst.Tango_DEV_STATE:
                                value = ((Integer) ((Object[]) getValue())[pos]).toString();
                                break;
                            case TangoConst.Tango_DEV_UCHAR:
                                value = ((Byte) ((Object[]) getValue())[pos]).toString();
                                break;
                            case TangoConst.Tango_DEV_LONG:
                                value = ((Integer) ((Object[]) getValue())[pos]).toString();
                                break;
                            case TangoConst.Tango_DEV_ULONG:
                                value = ((Integer) ((Object[]) getValue())[pos]).toString();
                                break;
                            case TangoConst.Tango_DEV_BOOLEAN:
                                value = ((Boolean) ((Object[]) getValue())[pos]).toString();
                                break;
                            case TangoConst.Tango_DEV_SHORT:
                                value = ((Short) ((Object[]) getValue())[pos]).toString();
                                break;
                            case TangoConst.Tango_DEV_FLOAT:
                                value = ((Float) ((Object[]) getValue())[pos]).toString();
                                break;
                            case TangoConst.Tango_DEV_DOUBLE:
                                value = ((Double) ((Object[]) getValue())[pos]).toString();
                                break;
                            default:
                                value = ((Double) ((Object[]) getValue())[pos]).toString();
                                break;
                        }
                        break;
                }
                break;
            case AttrDataFormat._SPECTRUM:
                if (getValue() == null) return value;
                value = "[";
                if (pos == 0) {
                    switch(writable) {
                        case AttrWriteType._READ:
                            switch(this.data_type) {
                                case TangoConst.Tango_DEV_BOOLEAN:
                                    Boolean[] valb = (Boolean[]) getValue();
                                    if (valb != null) {
                                        for (int i = 0; i < valb.length - 1; i++) {
                                            value += valb[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += valb[valb.length - 1];
                                    }
                                    break;
                                case TangoConst.Tango_DEV_STRING:
                                    String[] valstr = (String[]) getValue();
                                    if (valstr != null) {
                                        for (int i = 0; i < valstr.length - 1; i++) {
                                            value += valstr[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += valstr[valstr.length - 1];
                                    }
                                    break;
                                case TangoConst.Tango_DEV_CHAR:
                                case TangoConst.Tango_DEV_UCHAR:
                                    Byte[] valc = (Byte[]) getValue();
                                    if (valc != null) {
                                        for (int i = 0; i < valc.length - 1; i++) {
                                            value += valc[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += valc[valc.length - 1];
                                    }
                                    break;
                                case TangoConst.Tango_DEV_STATE:
                                case TangoConst.Tango_DEV_LONG:
                                case TangoConst.Tango_DEV_ULONG:
                                    Integer[] vall = (Integer[]) getValue();
                                    if (vall != null) {
                                        for (int i = 0; i < vall.length - 1; i++) {
                                            value += vall[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += vall[vall.length - 1];
                                    }
                                    break;
                                case TangoConst.Tango_DEV_USHORT:
                                case TangoConst.Tango_DEV_SHORT:
                                    Short[] vals = (Short[]) getValue();
                                    if (vals != null) {
                                        for (int i = 0; i < vals.length - 1; i++) {
                                            value += vals[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += vals[vals.length - 1];
                                    }
                                    break;
                                case TangoConst.Tango_DEV_FLOAT:
                                    Float[] valf = (Float[]) getValue();
                                    if (valf != null) {
                                        for (int i = 0; i < valf.length - 1; i++) {
                                            value += valf[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += valf[valf.length - 1];
                                    }
                                    break;
                                case TangoConst.Tango_DEV_DOUBLE:
                                    Double[] vald = (Double[]) getValue();
                                    if (vald != null) {
                                        for (int i = 0; i < vald.length - 1; i++) {
                                            value += vald[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += vald[vald.length - 1];
                                    }
                                    break;
                                default:
                                    value += "NaN";
                            }
                            break;
                        case AttrWriteType._READ_WITH_WRITE:
                        case AttrWriteType._READ_WRITE:
                            Object[] temp = (Object[]) getValue();
                            switch(this.data_type) {
                                case TangoConst.Tango_DEV_BOOLEAN:
                                    Boolean[] valb = (Boolean[]) temp[pos];
                                    if (valb != null) {
                                        for (int i = 0; i < valb.length - 1; i++) {
                                            value += valb[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += valb[valb.length - 1];
                                    }
                                    break;
                                case TangoConst.Tango_DEV_STRING:
                                    String[] valstr = (String[]) temp[pos];
                                    if (valstr != null) {
                                        for (int i = 0; i < valstr.length - 1; i++) {
                                            value += valstr[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += valstr[valstr.length - 1];
                                    }
                                    break;
                                case TangoConst.Tango_DEV_CHAR:
                                case TangoConst.Tango_DEV_UCHAR:
                                    Byte[] valc = (Byte[]) temp[pos];
                                    if (valc != null) {
                                        for (int i = 0; i < valc.length - 1; i++) {
                                            value += valc[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += valc[valc.length - 1];
                                    }
                                    break;
                                case TangoConst.Tango_DEV_STATE:
                                case TangoConst.Tango_DEV_LONG:
                                case TangoConst.Tango_DEV_ULONG:
                                    Integer[] vall = (Integer[]) temp[pos];
                                    if (vall != null) {
                                        for (int i = 0; i < vall.length - 1; i++) {
                                            value += vall[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += vall[vall.length - 1];
                                    }
                                    break;
                                case TangoConst.Tango_DEV_USHORT:
                                case TangoConst.Tango_DEV_SHORT:
                                    Short[] vals = (Short[]) temp[pos];
                                    if (vals != null) {
                                        for (int i = 0; i < vals.length - 1; i++) {
                                            value += vals[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += vals[vals.length - 1];
                                    }
                                    break;
                                case TangoConst.Tango_DEV_FLOAT:
                                    Float[] valf = (Float[]) temp[pos];
                                    if (valf != null) {
                                        for (int i = 0; i < valf.length - 1; i++) {
                                            value += valf[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += valf[valf.length - 1];
                                    }
                                    break;
                                case TangoConst.Tango_DEV_DOUBLE:
                                    Double[] vald = (Double[]) temp[pos];
                                    if (vald != null) {
                                        for (int i = 0; i < vald.length - 1; i++) {
                                            value += vald[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += vald[vald.length - 1];
                                    }
                                    break;
                                default:
                                    value += "NaN";
                            }
                            break;
                    }
                } else if (pos == 1) {
                    switch(writable) {
                        case AttrWriteType._WRITE:
                            switch(this.data_type) {
                                case TangoConst.Tango_DEV_BOOLEAN:
                                    Boolean[] valb = (Boolean[]) getValue();
                                    if (valb != null) {
                                        for (int i = 0; i < valb.length - 1; i++) {
                                            value += valb[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += valb[valb.length - 1];
                                    }
                                    break;
                                case TangoConst.Tango_DEV_STRING:
                                    String[] valstr = (String[]) getValue();
                                    if (valstr != null) {
                                        for (int i = 0; i < valstr.length - 1; i++) {
                                            value += valstr[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += valstr[valstr.length - 1];
                                    }
                                    break;
                                case TangoConst.Tango_DEV_CHAR:
                                case TangoConst.Tango_DEV_UCHAR:
                                    Byte[] valc = (Byte[]) getValue();
                                    if (valc != null) {
                                        for (int i = 0; i < valc.length - 1; i++) {
                                            value += valc[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += valc[valc.length - 1];
                                    }
                                    break;
                                case TangoConst.Tango_DEV_LONG:
                                case TangoConst.Tango_DEV_ULONG:
                                    Integer[] vall = (Integer[]) getValue();
                                    if (vall != null) {
                                        for (int i = 0; i < vall.length - 1; i++) {
                                            value += vall[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += vall[vall.length - 1];
                                    }
                                    break;
                                case TangoConst.Tango_DEV_USHORT:
                                case TangoConst.Tango_DEV_SHORT:
                                    Short[] vals = (Short[]) getValue();
                                    if (vals != null) {
                                        for (int i = 0; i < vals.length - 1; i++) {
                                            value += vals[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += vals[vals.length - 1];
                                    }
                                    break;
                                case TangoConst.Tango_DEV_FLOAT:
                                    Float[] valf = (Float[]) getValue();
                                    if (valf != null) {
                                        for (int i = 0; i < valf.length - 1; i++) {
                                            value += valf[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += valf[valf.length - 1];
                                    }
                                    break;
                                case TangoConst.Tango_DEV_DOUBLE:
                                    Double[] vald = (Double[]) getValue();
                                    if (vald != null) {
                                        for (int i = 0; i < vald.length - 1; i++) {
                                            value += vald[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += vald[vald.length - 1];
                                    }
                                    break;
                                default:
                                    value += "NaN";
                            }
                            break;
                        case AttrWriteType._READ_WITH_WRITE:
                        case AttrWriteType._READ_WRITE:
                            Object[] temp = (Object[]) getValue();
                            switch(this.data_type) {
                                case TangoConst.Tango_DEV_BOOLEAN:
                                    Boolean[] valb = (Boolean[]) temp[pos];
                                    if (valb != null) {
                                        for (int i = 0; i < valb.length - 1; i++) {
                                            value += valb[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += valb[valb.length - 1];
                                    }
                                    break;
                                case TangoConst.Tango_DEV_STRING:
                                    String[] valstr = (String[]) temp[pos];
                                    if (valstr != null) {
                                        for (int i = 0; i < valstr.length - 1; i++) {
                                            value += valstr[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += valstr[valstr.length - 1];
                                    }
                                    break;
                                case TangoConst.Tango_DEV_CHAR:
                                case TangoConst.Tango_DEV_UCHAR:
                                    Byte[] valc = (Byte[]) temp[pos];
                                    if (valc != null) {
                                        for (int i = 0; i < valc.length - 1; i++) {
                                            value += valc[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += valc[valc.length - 1];
                                    }
                                    break;
                                case TangoConst.Tango_DEV_LONG:
                                case TangoConst.Tango_DEV_ULONG:
                                    Integer[] vall = (Integer[]) temp[pos];
                                    if (vall != null) {
                                        for (int i = 0; i < vall.length - 1; i++) {
                                            value += vall[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += vall[vall.length - 1];
                                    }
                                    break;
                                case TangoConst.Tango_DEV_USHORT:
                                case TangoConst.Tango_DEV_SHORT:
                                    Short[] vals = (Short[]) temp[pos];
                                    if (vals != null) {
                                        for (int i = 0; i < vals.length - 1; i++) {
                                            value += vals[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += vals[vals.length - 1];
                                    }
                                    break;
                                case TangoConst.Tango_DEV_FLOAT:
                                    Float[] valf = (Float[]) temp[pos];
                                    if (valf != null) {
                                        for (int i = 0; i < valf.length - 1; i++) {
                                            value += valf[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += valf[valf.length - 1];
                                    }
                                    break;
                                case TangoConst.Tango_DEV_DOUBLE:
                                    Double[] vald = (Double[]) temp[pos];
                                    if (vald != null) {
                                        for (int i = 0; i < vald.length - 1; i++) {
                                            value += vald[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        value += vald[vald.length - 1];
                                    }
                                    break;
                                default:
                                    value += "NaN";
                            }
                            break;
                    }
                }
                value += "]";
                break;
            case AttrDataFormat._IMAGE:
                value = ((String) getValue()).toString();
                break;
        }
        return value;
    }

    /**
	 * 
	 * @param device_data
	 */
    public void insertsnapAttributeValue(DeviceData device_data) {
        switch(data_format) {
            case AttrDataFormat._SCALAR:
                switch(writable) {
                    case AttrWriteType._READ:
                        switch(this.data_type) {
                            case TangoConst.Tango_DEV_STRING:
                                device_data.insert((String) getValue());
                                break;
                            case TangoConst.Tango_DEV_STATE:
                                device_data.insert((Integer) getValue());
                                break;
                            case TangoConst.Tango_DEV_UCHAR:
                                device_data.insert((Byte) getValue());
                                break;
                            case TangoConst.Tango_DEV_LONG:
                                device_data.insert((Integer) getValue());
                                break;
                            case TangoConst.Tango_DEV_ULONG:
                                device_data.insert((Integer) getValue());
                                break;
                            case TangoConst.Tango_DEV_BOOLEAN:
                                device_data.insert((Boolean) getValue());
                                break;
                            case TangoConst.Tango_DEV_SHORT:
                                device_data.insert((Short) getValue());
                                break;
                            case TangoConst.Tango_DEV_FLOAT:
                                device_data.insert((Float) getValue());
                                break;
                            case TangoConst.Tango_DEV_DOUBLE:
                                device_data.insert((Double) getValue());
                                break;
                            default:
                                device_data.insert((Double) getValue());
                                break;
                        }
                        break;
                    case AttrWriteType._READ_WITH_WRITE:
                        switch(this.data_type) {
                            case TangoConst.Tango_DEV_STRING:
                                device_data.insert((String) ((Object[]) getValue())[0]);
                                break;
                            case TangoConst.Tango_DEV_STATE:
                                device_data.insert((Integer) ((Object[]) getValue())[0]);
                                break;
                            case TangoConst.Tango_DEV_UCHAR:
                                device_data.insert((Byte) ((Object[]) getValue())[0]);
                                break;
                            case TangoConst.Tango_DEV_LONG:
                                device_data.insert((Integer) ((Object[]) getValue())[0]);
                                break;
                            case TangoConst.Tango_DEV_ULONG:
                                device_data.insert((Integer) ((Object[]) getValue())[0]);
                                break;
                            case TangoConst.Tango_DEV_BOOLEAN:
                                device_data.insert((Boolean) ((Object[]) getValue())[0]);
                                break;
                            case TangoConst.Tango_DEV_SHORT:
                                device_data.insert((Short) ((Object[]) getValue())[0]);
                                break;
                            case TangoConst.Tango_DEV_FLOAT:
                                device_data.insert((Float) ((Object[]) getValue())[0]);
                                break;
                            case TangoConst.Tango_DEV_DOUBLE:
                                device_data.insert((Double) ((Object[]) getValue())[0]);
                                break;
                            default:
                                device_data.insert((Double) ((Object[]) getValue())[0]);
                                break;
                        }
                        break;
                    case AttrWriteType._WRITE:
                        break;
                    case AttrWriteType._READ_WRITE:
                        switch(this.data_type) {
                            case TangoConst.Tango_DEV_STRING:
                                device_data.insert((String) ((Object[]) getValue())[0]);
                                break;
                            case TangoConst.Tango_DEV_STATE:
                                device_data.insert((Integer) ((Object[]) getValue())[0]);
                                break;
                            case TangoConst.Tango_DEV_UCHAR:
                                device_data.insert((Byte) ((Object[]) getValue())[0]);
                                break;
                            case TangoConst.Tango_DEV_LONG:
                                device_data.insert((Integer) ((Object[]) getValue())[0]);
                                break;
                            case TangoConst.Tango_DEV_ULONG:
                                device_data.insert((Integer) ((Object[]) getValue())[0]);
                                break;
                            case TangoConst.Tango_DEV_BOOLEAN:
                                device_data.insert((Boolean) ((Object[]) getValue())[0]);
                                break;
                            case TangoConst.Tango_DEV_SHORT:
                                device_data.insert((Short) ((Object[]) getValue())[0]);
                                break;
                            case TangoConst.Tango_DEV_FLOAT:
                                device_data.insert((Float) ((Object[]) getValue())[0]);
                                break;
                            case TangoConst.Tango_DEV_DOUBLE:
                                device_data.insert((Double) ((Object[]) getValue())[0]);
                                break;
                            default:
                                device_data.insert((Double) ((Object[]) getValue())[0]);
                                break;
                        }
                        break;
                }
                break;
            case AttrDataFormat._SPECTRUM:
                switch(writable) {
                    case AttrWriteType._READ:
                        switch(this.data_type) {
                            case TangoConst.Tango_DEV_STRING:
                                device_data.insert((String[]) getValue());
                                break;
                            case TangoConst.Tango_DEV_STATE:
                                Integer[] val_St = (Integer[]) getValue();
                                int[] val = new int[val_St.length];
                                for (int i = 0; i < val_St.length; i++) {
                                    val[i] = val_St[i].intValue();
                                }
                                device_data.insert(val);
                                break;
                            case TangoConst.Tango_DEV_UCHAR:
                                Byte[] val_UChar = (Byte[]) getValue();
                                byte[] val_uch = new byte[val_UChar.length];
                                for (int i = 0; i < val_UChar.length; i++) {
                                    val_uch[i] = val_UChar[i].byteValue();
                                }
                                device_data.insert(val_uch);
                                break;
                            case TangoConst.Tango_DEV_LONG:
                                Integer[] val_Lg = (Integer[]) getValue();
                                int[] val_lg = new int[val_Lg.length];
                                for (int i = 0; i < val_Lg.length; i++) {
                                    val_lg[i] = val_Lg[i].intValue();
                                }
                                device_data.insert(val_lg);
                                break;
                            case TangoConst.Tango_DEV_ULONG:
                                Integer[] val_ULg = (Integer[]) getValue();
                                int[] val_ulg = new int[val_ULg.length];
                                for (int i = 0; i < val_ULg.length; i++) {
                                    val_ulg[i] = val_ULg[i].intValue();
                                }
                                device_data.insert(val_ulg);
                                break;
                            case TangoConst.Tango_DEV_BOOLEAN:
                                break;
                            case TangoConst.Tango_DEV_SHORT:
                                Short[] val_Short = (Short[]) getValue();
                                short[] val_sh = new short[val_Short.length];
                                for (int i = 0; i < val_Short.length; i++) {
                                    val_sh[i] = val_Short[i].shortValue();
                                }
                                device_data.insert(val_sh);
                                break;
                            case TangoConst.Tango_DEV_FLOAT:
                                Float[] val_Float = (Float[]) getValue();
                                float[] val_f = new float[val_Float.length];
                                for (int i = 0; i < val_Float.length; i++) {
                                    val_f[i] = val_Float[i].floatValue();
                                }
                                device_data.insert(val_f);
                                break;
                            case TangoConst.Tango_DEV_DOUBLE:
                                Double[] val_Double = (Double[]) getValue();
                                double[] val_d = new double[val_Double.length];
                                for (int i = 0; i < val_Double.length; i++) {
                                    val_d[i] = val_Double[i].doubleValue();
                                }
                                device_data.insert(val_d);
                                break;
                            default:
                                val_Double = (Double[]) getValue();
                                val_d = new double[val_Double.length];
                                for (int i = 0; i < val_Double.length; i++) {
                                    val_d[i] = val_Double[i].doubleValue();
                                }
                                device_data.insert(val_d);
                                break;
                        }
                        break;
                    case AttrWriteType._READ_WITH_WRITE:
                        switch(this.data_type) {
                            case TangoConst.Tango_DEV_STRING:
                                device_data.insert((String[]) ((Object[]) getValue())[0]);
                                break;
                            case TangoConst.Tango_DEV_STATE:
                                Integer[] val_St = (Integer[]) ((Object[]) getValue())[0];
                                int[] val = new int[val_St.length];
                                for (int i = 0; i < val_St.length; i++) {
                                    val[i] = val_St[i].intValue();
                                }
                                device_data.insert(val);
                                break;
                            case TangoConst.Tango_DEV_UCHAR:
                                Byte[] val_UChar = (Byte[]) ((Object[]) getValue())[0];
                                byte[] val_uch = new byte[val_UChar.length];
                                for (int i = 0; i < val_UChar.length; i++) {
                                    val_uch[i] = val_UChar[i].byteValue();
                                }
                                device_data.insert(val_uch);
                                break;
                            case TangoConst.Tango_DEV_LONG:
                                Integer[] val_Lg = (Integer[]) ((Object[]) getValue())[0];
                                int[] val_lg = new int[val_Lg.length];
                                for (int i = 0; i < val_Lg.length; i++) {
                                    val_lg[i] = val_Lg[i].intValue();
                                }
                                device_data.insert(val_lg);
                                break;
                            case TangoConst.Tango_DEV_ULONG:
                                Integer[] val_ULg = (Integer[]) ((Object[]) getValue())[0];
                                int[] val_ulg = new int[val_ULg.length];
                                for (int i = 0; i < val_ULg.length; i++) {
                                    val_ulg[i] = val_ULg[i].intValue();
                                }
                                device_data.insert(val_ulg);
                                break;
                            case TangoConst.Tango_DEV_BOOLEAN:
                                break;
                            case TangoConst.Tango_DEV_SHORT:
                                Short[] val_Short = (Short[]) ((Object[]) getValue())[0];
                                short[] val_sh = new short[val_Short.length];
                                for (int i = 0; i < val_Short.length; i++) {
                                    val_sh[i] = val_Short[i].shortValue();
                                }
                                device_data.insert(val_sh);
                                break;
                            case TangoConst.Tango_DEV_FLOAT:
                                Float[] val_Float = (Float[]) getValue();
                                float[] val_f = new float[val_Float.length];
                                for (int i = 0; i < val_Float.length; i++) {
                                    val_f[i] = val_Float[i].floatValue();
                                }
                                device_data.insert(val_f);
                                break;
                            case TangoConst.Tango_DEV_DOUBLE:
                                Double[] val_Double = (Double[]) ((Object[]) getValue())[0];
                                double[] val_d = new double[val_Double.length];
                                for (int i = 0; i < val_Double.length; i++) {
                                    val_d[i] = val_Double[i].doubleValue();
                                }
                                device_data.insert(val_d);
                                break;
                            default:
                                val_Double = (Double[]) ((Object[]) getValue())[0];
                                val_d = new double[val_Double.length];
                                for (int i = 0; i < val_Double.length; i++) {
                                    val_d[i] = val_Double[i].doubleValue();
                                }
                                device_data.insert(val_d);
                                break;
                        }
                        break;
                    case AttrWriteType._WRITE:
                        break;
                    case AttrWriteType._READ_WRITE:
                        switch(this.data_type) {
                            case TangoConst.Tango_DEV_STRING:
                                device_data.insert((String[]) ((Object[]) getValue())[0]);
                                break;
                            case TangoConst.Tango_DEV_STATE:
                                Integer[] val_St = (Integer[]) ((Object[]) getValue())[0];
                                int[] val = new int[val_St.length];
                                for (int i = 0; i < val_St.length; i++) {
                                    val[i] = val_St[i].intValue();
                                }
                                device_data.insert(val);
                                break;
                            case TangoConst.Tango_DEV_UCHAR:
                                Byte[] val_UChar = (Byte[]) ((Object[]) getValue())[0];
                                byte[] val_uch = new byte[val_UChar.length];
                                for (int i = 0; i < val_UChar.length; i++) {
                                    val_uch[i] = val_UChar[i].byteValue();
                                }
                                device_data.insert(val_uch);
                                break;
                            case TangoConst.Tango_DEV_LONG:
                                Integer[] val_Lg = (Integer[]) ((Object[]) getValue())[0];
                                int[] val_lg = new int[val_Lg.length];
                                for (int i = 0; i < val_Lg.length; i++) {
                                    val_lg[i] = val_Lg[i].intValue();
                                }
                                device_data.insert(val_lg);
                                break;
                            case TangoConst.Tango_DEV_ULONG:
                                Integer[] val_ULg = (Integer[]) ((Object[]) getValue())[0];
                                int[] val_ulg = new int[val_ULg.length];
                                for (int i = 0; i < val_ULg.length; i++) {
                                    val_ulg[i] = val_ULg[i].intValue();
                                }
                                device_data.insert(val_ulg);
                                break;
                            case TangoConst.Tango_DEV_BOOLEAN:
                                break;
                            case TangoConst.Tango_DEV_SHORT:
                                Short[] val_Short = (Short[]) ((Object[]) getValue())[0];
                                short[] val_sh = new short[val_Short.length];
                                for (int i = 0; i < val_Short.length; i++) {
                                    val_sh[i] = val_Short[i].shortValue();
                                }
                                device_data.insert(val_sh);
                                break;
                            case TangoConst.Tango_DEV_FLOAT:
                                Float[] val_Float = (Float[]) getValue();
                                float[] val_f = new float[val_Float.length];
                                for (int i = 0; i < val_Float.length; i++) {
                                    val_f[i] = val_Float[i].floatValue();
                                }
                                device_data.insert(val_f);
                                break;
                            case TangoConst.Tango_DEV_DOUBLE:
                                Double[] val_Double = (Double[]) ((Object[]) getValue())[0];
                                double[] val_d = new double[val_Double.length];
                                for (int i = 0; i < val_Double.length; i++) {
                                    val_d[i] = val_Double[i].doubleValue();
                                }
                                device_data.insert(val_d);
                                break;
                            default:
                                val_Double = (Double[]) ((Object[]) getValue())[0];
                                val_d = new double[val_Double.length];
                                for (int i = 0; i < val_Double.length; i++) {
                                    val_d[i] = val_Double[i].doubleValue();
                                }
                                device_data.insert(val_d);
                                break;
                        }
                        break;
                }
                break;
            case AttrDataFormat._IMAGE:
                break;
        }
    }

    public Object getWriteValue() {
        Object write_value = null;
        switch(data_format) {
            case AttrDataFormat._SCALAR:
                switch(writable) {
                    case AttrWriteType._READ:
                        break;
                    case AttrWriteType._READ_WITH_WRITE:
                        switch(this.data_type) {
                            case TangoConst.Tango_DEV_STRING:
                                write_value = (String) ((Object[]) getValue())[1];
                                break;
                            case TangoConst.Tango_DEV_STATE:
                                write_value = (Integer) ((Object[]) getValue())[1];
                                break;
                            case TangoConst.Tango_DEV_UCHAR:
                                write_value = (Byte) ((Object[]) getValue())[1];
                                break;
                            case TangoConst.Tango_DEV_LONG:
                                write_value = (Integer) ((Object[]) getValue())[1];
                                break;
                            case TangoConst.Tango_DEV_ULONG:
                                write_value = (Integer) ((Object[]) getValue())[1];
                                break;
                            case TangoConst.Tango_DEV_BOOLEAN:
                                write_value = (Boolean) ((Object[]) getValue())[1];
                                break;
                            case TangoConst.Tango_DEV_SHORT:
                                write_value = (Short) ((Object[]) getValue())[1];
                                break;
                            case TangoConst.Tango_DEV_FLOAT:
                                write_value = (Float) ((Object[]) getValue())[1];
                                break;
                            case TangoConst.Tango_DEV_DOUBLE:
                                write_value = (Double) ((Object[]) getValue())[1];
                                break;
                            default:
                                write_value = (Double) ((Object[]) getValue())[1];
                                break;
                        }
                        break;
                    case AttrWriteType._WRITE:
                        switch(this.data_type) {
                            case TangoConst.Tango_DEV_STRING:
                                write_value = (String) getValue();
                                break;
                            case TangoConst.Tango_DEV_STATE:
                                write_value = (Integer) getValue();
                                break;
                            case TangoConst.Tango_DEV_UCHAR:
                                write_value = (Byte) getValue();
                                break;
                            case TangoConst.Tango_DEV_LONG:
                                write_value = (Integer) getValue();
                                break;
                            case TangoConst.Tango_DEV_ULONG:
                                write_value = (Integer) getValue();
                                break;
                            case TangoConst.Tango_DEV_BOOLEAN:
                                write_value = (Boolean) getValue();
                                break;
                            case TangoConst.Tango_DEV_SHORT:
                                write_value = (Short) getValue();
                                break;
                            case TangoConst.Tango_DEV_FLOAT:
                                write_value = (Float) getValue();
                                break;
                            case TangoConst.Tango_DEV_DOUBLE:
                                write_value = (Double) getValue();
                                break;
                            default:
                                write_value = (Double) getValue();
                                break;
                        }
                        break;
                    case AttrWriteType._READ_WRITE:
                        switch(this.data_type) {
                            case TangoConst.Tango_DEV_STRING:
                                write_value = (String) ((Object[]) getValue())[1];
                                break;
                            case TangoConst.Tango_DEV_STATE:
                                write_value = (Integer) ((Object[]) getValue())[1];
                                break;
                            case TangoConst.Tango_DEV_UCHAR:
                                write_value = (Byte) ((Object[]) getValue())[1];
                                break;
                            case TangoConst.Tango_DEV_LONG:
                                write_value = (Integer) ((Object[]) getValue())[1];
                                break;
                            case TangoConst.Tango_DEV_ULONG:
                                write_value = (Integer) ((Object[]) getValue())[1];
                                break;
                            case TangoConst.Tango_DEV_BOOLEAN:
                                write_value = (Boolean) ((Object[]) getValue())[1];
                                break;
                            case TangoConst.Tango_DEV_SHORT:
                                write_value = (Short) ((Object[]) getValue())[1];
                                break;
                            case TangoConst.Tango_DEV_FLOAT:
                                write_value = (Float) ((Object[]) getValue())[1];
                                break;
                            case TangoConst.Tango_DEV_DOUBLE:
                                write_value = (Double) ((Object[]) getValue())[1];
                                break;
                            default:
                                write_value = (Double) ((Object[]) getValue())[1];
                                break;
                        }
                        break;
                }
                break;
            case AttrDataFormat._SPECTRUM:
            case AttrDataFormat._IMAGE:
                switch(this.writable) {
                    case AttrWriteType._READ:
                        break;
                    case AttrWriteType._WRITE:
                        if (getValue() == null || "NaN".equals(getValue())) {
                            return "NaN";
                        }
                        write_value = getValue();
                        break;
                    case AttrWriteType._READ_WITH_WRITE:
                    case AttrWriteType._READ_WRITE:
                        if (getValue() == null || "NaN".equals(getValue())) {
                            return "NaN";
                        }
                        Object[] temp = (Object[]) getValue();
                        if (temp[1] == null || "NaN".equals(temp[1])) {
                            return "NaN";
                        }
                        write_value = temp[1];
                        break;
                }
                break;
        }
        return write_value;
    }

    /**
	 * 
	 * @return
	 */
    public Object getReadValue() {
        Object read_value = new Object();
        switch(data_format) {
            case AttrDataFormat._SCALAR:
                switch(writable) {
                    case AttrWriteType._READ:
                        switch(this.data_type) {
                            case TangoConst.Tango_DEV_STRING:
                                read_value = (String) getValue();
                                break;
                            case TangoConst.Tango_DEV_STATE:
                                read_value = (Integer) getValue();
                                break;
                            case TangoConst.Tango_DEV_UCHAR:
                                read_value = (Byte) getValue();
                                break;
                            case TangoConst.Tango_DEV_LONG:
                                read_value = (Integer) getValue();
                                break;
                            case TangoConst.Tango_DEV_ULONG:
                                read_value = (Integer) getValue();
                                break;
                            case TangoConst.Tango_DEV_BOOLEAN:
                                read_value = (Boolean) getValue();
                                break;
                            case TangoConst.Tango_DEV_SHORT:
                                read_value = (Short) getValue();
                                break;
                            case TangoConst.Tango_DEV_FLOAT:
                                read_value = (Float) getValue();
                                break;
                            case TangoConst.Tango_DEV_DOUBLE:
                                read_value = (Double) getValue();
                                break;
                            default:
                                read_value = (Double) getValue();
                                break;
                        }
                        break;
                    case AttrWriteType._READ_WITH_WRITE:
                        switch(this.data_type) {
                            case TangoConst.Tango_DEV_STRING:
                                read_value = (String) ((Object[]) getValue())[0];
                                break;
                            case TangoConst.Tango_DEV_STATE:
                                read_value = (Integer) ((Object[]) getValue())[0];
                                break;
                            case TangoConst.Tango_DEV_UCHAR:
                                read_value = (Byte) ((Object[]) getValue())[0];
                                break;
                            case TangoConst.Tango_DEV_LONG:
                                read_value = (Integer) ((Object[]) getValue())[0];
                                break;
                            case TangoConst.Tango_DEV_ULONG:
                                read_value = (Integer) ((Object[]) getValue())[0];
                                break;
                            case TangoConst.Tango_DEV_BOOLEAN:
                                read_value = (Boolean) ((Object[]) getValue())[0];
                                break;
                            case TangoConst.Tango_DEV_SHORT:
                                read_value = (Short) ((Object[]) getValue())[0];
                                break;
                            case TangoConst.Tango_DEV_FLOAT:
                                read_value = (Float) ((Object[]) getValue())[0];
                                break;
                            case TangoConst.Tango_DEV_DOUBLE:
                                read_value = (Double) ((Object[]) getValue())[0];
                                break;
                            default:
                                read_value = (Double) ((Object[]) getValue())[0];
                                break;
                        }
                        break;
                    case AttrWriteType._WRITE:
                        break;
                    case AttrWriteType._READ_WRITE:
                        switch(this.data_type) {
                            case TangoConst.Tango_DEV_STRING:
                                read_value = (String) ((Object[]) getValue())[0];
                                break;
                            case TangoConst.Tango_DEV_STATE:
                                read_value = (Integer) ((Object[]) getValue())[0];
                                break;
                            case TangoConst.Tango_DEV_UCHAR:
                                read_value = (Byte) ((Object[]) getValue())[0];
                                break;
                            case TangoConst.Tango_DEV_LONG:
                                read_value = (Integer) ((Object[]) getValue())[0];
                                break;
                            case TangoConst.Tango_DEV_ULONG:
                                read_value = (Integer) ((Object[]) getValue())[0];
                                break;
                            case TangoConst.Tango_DEV_BOOLEAN:
                                read_value = (Boolean) ((Object[]) getValue())[0];
                                break;
                            case TangoConst.Tango_DEV_SHORT:
                                read_value = (Short) ((Object[]) getValue())[0];
                                break;
                            case TangoConst.Tango_DEV_FLOAT:
                                read_value = (Float) ((Object[]) getValue())[0];
                                break;
                            case TangoConst.Tango_DEV_DOUBLE:
                                read_value = (Double) ((Object[]) getValue())[0];
                                break;
                            default:
                                read_value = (Double) ((Object[]) getValue())[0];
                                break;
                        }
                        break;
                }
                break;
            case AttrDataFormat._SPECTRUM:
            case AttrDataFormat._IMAGE:
                switch(this.writable) {
                    case AttrWriteType._READ:
                        if (getValue() == null || "NaN".equals(getValue())) {
                            return "NaN";
                        }
                        read_value = getValue();
                        break;
                    case AttrWriteType._WRITE:
                        break;
                    case AttrWriteType._READ_WITH_WRITE:
                    case AttrWriteType._READ_WRITE:
                        if (getValue() == null || "NaN".equals(getValue())) {
                            return "NaN";
                        }
                        Object[] temp = (Object[]) getValue();
                        if (temp[0] == null || "NaN".equals(temp[0])) {
                            return "NaN";
                        }
                        read_value = temp[0];
                        break;
                }
                break;
        }
        return read_value;
    }

    public Object getNewValue(String stringValue) {
        Object newValue = new Object();
        switch(data_format) {
            case AttrDataFormat._SCALAR:
                switch(data_type) {
                    case TangoConst.Tango_DEV_BOOLEAN:
                        {
                            newValue = Boolean.parseBoolean(stringValue);
                            break;
                        }
                    case TangoConst.Tango_DEV_DOUBLE:
                        {
                            newValue = Double.parseDouble(stringValue);
                            break;
                        }
                    case TangoConst.Tango_DEV_FLOAT:
                        {
                            newValue = Float.parseFloat(stringValue);
                            break;
                        }
                    case TangoConst.Tango_DEV_INT:
                    case TangoConst.Tango_DEV_LONG:
                    case TangoConst.Tango_DEV_STATE:
                    case TangoConst.Tango_DEV_ULONG:
                        {
                            newValue = Integer.parseInt(stringValue);
                            break;
                        }
                    case TangoConst.Tango_DEV_SHORT:
                        {
                            newValue = Short.parseShort(stringValue);
                            break;
                        }
                    case TangoConst.Tango_DEV_UCHAR:
                        {
                            newValue = Byte.parseByte(stringValue);
                            break;
                        }
                    case TangoConst.Tango_DEV_STRING:
                    default:
                        newValue = stringValue;
                        break;
                }
                break;
            case AttrDataFormat._SPECTRUM:
                {
                    String[] stringTable = stringValue.split(GlobalConst.CLOB_SEPARATOR_IMAGE_COLS);
                    Object[] newTable = null;
                    switch(data_type) {
                        case TangoConst.Tango_DEV_BOOLEAN:
                            {
                                newTable = new Boolean[dimX];
                                for (int i = 0; i < stringTable.length; i++) {
                                    newTable[i] = Boolean.parseBoolean(stringTable[i]);
                                }
                                break;
                            }
                        case TangoConst.Tango_DEV_DOUBLE:
                            {
                                newTable = new Double[dimX];
                                for (int i = 0; i < stringTable.length; i++) {
                                    newTable[i] = Double.parseDouble(stringTable[i]);
                                }
                                break;
                            }
                        case TangoConst.Tango_DEV_FLOAT:
                            {
                                newTable = new Float[dimX];
                                for (int i = 0; i < stringTable.length; i++) {
                                    newTable[i] = Float.parseFloat(stringTable[i]);
                                }
                                break;
                            }
                        case TangoConst.Tango_DEV_INT:
                        case TangoConst.Tango_DEV_LONG:
                        case TangoConst.Tango_DEV_STATE:
                        case TangoConst.Tango_DEV_ULONG:
                            {
                                newTable = new Integer[dimX];
                                for (int i = 0; i < stringTable.length; i++) {
                                    newTable[i] = Integer.parseInt(stringTable[i]);
                                }
                                break;
                            }
                        case TangoConst.Tango_DEV_SHORT:
                            {
                                newTable = new Short[dimX];
                                for (int i = 0; i < stringTable.length; i++) {
                                    newTable[i] = Short.parseShort(stringTable[i]);
                                }
                                break;
                            }
                        case TangoConst.Tango_DEV_UCHAR:
                            {
                                newTable = new Byte[dimX];
                                for (int i = 0; i < stringTable.length; i++) {
                                    newTable[i] = Byte.parseByte(stringTable[i]);
                                }
                                break;
                            }
                        case TangoConst.Tango_DEV_STRING:
                            {
                                newTable = stringTable;
                                break;
                            }
                        default:
                            newTable = stringTable;
                            break;
                    }
                    newValue = newTable;
                    break;
                }
            case AttrDataFormat._IMAGE:
                {
                    String[][] stringMatrix = new String[dimX][dimX];
                    String[] stringTable = stringValue.split(GlobalConst.CLOB_SEPARATOR_IMAGE_ROWS);
                    for (int i = 0; i < stringTable.length; i++) {
                        stringMatrix[i] = stringTable[i].split(GlobalConst.CLOB_SEPARATOR_IMAGE_COLS);
                    }
                    Object[][] newMatrix = null;
                    switch(data_type) {
                        case TangoConst.Tango_DEV_BOOLEAN:
                            newMatrix = new Boolean[dimX][dimX];
                            for (int i = 0; i < dimX; i++) {
                                for (int j = 0; j < dimX; j++) {
                                    newMatrix[i][j] = Boolean.parseBoolean(stringMatrix[i][j]);
                                }
                            }
                            break;
                        case TangoConst.Tango_DEV_DOUBLE:
                            newMatrix = new Double[dimX][dimX];
                            for (int i = 0; i < dimX; i++) {
                                for (int j = 0; j < dimX; j++) {
                                    newMatrix[i][j] = Double.parseDouble(stringMatrix[i][j]);
                                }
                            }
                            break;
                        case TangoConst.Tango_DEV_FLOAT:
                            newMatrix = new Float[dimX][dimX];
                            for (int i = 0; i < dimX; i++) {
                                for (int j = 0; j < dimX; j++) {
                                    newMatrix[i][j] = Float.parseFloat(stringMatrix[i][j]);
                                }
                            }
                            break;
                        case TangoConst.Tango_DEV_INT:
                        case TangoConst.Tango_DEV_LONG:
                        case TangoConst.Tango_DEV_STATE:
                        case TangoConst.Tango_DEV_ULONG:
                            newMatrix = new Integer[dimX][dimX];
                            for (int i = 0; i < dimX; i++) {
                                for (int j = 0; j < dimX; j++) {
                                    newMatrix[i][j] = Integer.parseInt(stringMatrix[i][j]);
                                }
                            }
                            break;
                        case TangoConst.Tango_DEV_SHORT:
                            newMatrix = new Short[dimX][dimX];
                            for (int i = 0; i < dimX; i++) {
                                for (int j = 0; j < dimX; j++) {
                                    newMatrix[i][j] = Short.parseShort(stringMatrix[i][j]);
                                }
                            }
                            break;
                        case TangoConst.Tango_DEV_UCHAR:
                            newMatrix = new Byte[dimX][dimX];
                            for (int i = 0; i < dimX; i++) {
                                for (int j = 0; j < dimX; j++) {
                                    newMatrix[i][j] = Byte.parseByte(stringMatrix[i][j]);
                                }
                            }
                            break;
                        case TangoConst.Tango_DEV_STRING:
                            newMatrix = stringMatrix;
                            break;
                        default:
                            newMatrix = stringMatrix;
                            break;
                    }
                    newValue = newMatrix;
                    break;
                }
            default:
                break;
        }
        return newValue;
    }

    public void setWriteValue(Object writeValue) {
        switch(getData_format()) {
            case AttrDataFormat._SCALAR:
            case AttrDataFormat._SPECTRUM:
                switch(writable) {
                    case AttrWriteType._READ:
                        break;
                    case AttrWriteType._READ_WITH_WRITE:
                    case AttrWriteType._READ_WRITE:
                        Object[] newValue = (Object[]) getValue();
                        newValue[1] = writeValue;
                        setValue(newValue);
                        break;
                    case AttrWriteType._WRITE:
                        setValue(writeValue);
                        break;
                }
                break;
            case AttrDataFormat._IMAGE:
                setValue(writeValue);
                break;
        }
    }

    public String toString() {
        String snapStr = "";
        String value = ((writable == AttrWriteType._READ || writable == AttrWriteType._READ_WITH_WRITE || writable == AttrWriteType._READ_WRITE) ? "read value :  " + valueToString(0) : "") + ((writable == AttrWriteType._WRITE || writable == AttrWriteType._READ_WITH_WRITE || writable == AttrWriteType._READ_WRITE) ? "\t " + "write value : " + valueToString(1) : "");
        snapStr = "attribute ID   : \t" + getId_att() + "\r\n" + "attribute Name : \t" + getAttribute_complete_name() + "\r\n" + "attribute value : \t" + value + "\r\n";
        return snapStr;
    }

    public String[] toArray() {
        String[] snapAttExt;
        snapAttExt = new String[7];
        snapAttExt[0] = getAttribute_complete_name();
        snapAttExt[1] = Integer.toString(getId_att());
        snapAttExt[2] = Integer.toString(data_type);
        snapAttExt[3] = Integer.toString(data_format);
        snapAttExt[4] = Integer.toString(writable);
        snapAttExt[5] = valueToString(0);
        snapAttExt[6] = valueToString(1);
        return snapAttExt;
    }

    /**
	 * @return Returns the dimX.
	 */
    public int getDimX() {
        return dimX;
    }

    /**
	 * @param dimX
	 *            The dimX to set.
	 */
    public void setDimX(int dimX) {
        this.dimX = dimX;
    }

    public String getdeviceName() {
        String name = this.getAttribute_complete_name();
        return name.substring(0, name.lastIndexOf("/"));
    }
}
