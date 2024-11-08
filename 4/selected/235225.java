package fr.soleil.bensikin.data.snapshot;

import fr.esrf.TangoDs.TangoConst;
import fr.soleil.bensikin.tools.Messages;

/**
 * Represents the difference between an attribute effective read value and its
 * write value.
 *
 * @author CLAISSE
 */
public class SnapshotAttributeDeltaAbsValue extends SnapshotAttributeValue {

    /**
     * Builds a SnapshotAttributeDeltaAbsValue from the difference between a
     * read value and a write value.
     *
     * @param writeAbsValue
     *            The write abs value
     * @param readAbsValue
     *            The read abs value
     */
    public SnapshotAttributeDeltaAbsValue(SnapshotAttributeWriteAbsValue writeAbsValue, SnapshotAttributeReadAbsValue readAbsValue) {
        this(writeAbsValue, readAbsValue, false);
    }

    public SnapshotAttributeDeltaAbsValue(SnapshotAttributeWriteAbsValue writeAbsValue, SnapshotAttributeReadAbsValue readAbsValue, boolean manageAllTypes) {
        super(writeAbsValue.getDataFormat(), writeAbsValue.getDataType(), null);
        Object deltaValue = getDeltaAbsValue(writeAbsValue, readAbsValue, manageAllTypes);
        if (readAbsValue == null || writeAbsValue == null) {
            this.setNotApplicable(true);
        } else if (readAbsValue.isNotApplicable() || writeAbsValue.isNotApplicable()) {
            this.setNotApplicable(true);
        } else if (deltaValue == null) {
            this.setNotApplicable(true);
        }
        if ((deltaValue instanceof String) || (deltaValue instanceof String[])) {
            this.setDataType(TangoConst.Tango_DEV_STRING);
        }
        this.setValue(deltaValue);
    }

    /**
     * Builds a SnapshotAttributeDeltaAbsValue directly, given its Object value
     * and the format of this Object.
     *
     * @param _dataFormat
     *            The Tango type of _value
     * @param _value
     *            The Object value
     */
    public SnapshotAttributeDeltaAbsValue(int _dataFormat, Object _value) {
        super(_dataFormat, 0, _value);
    }

    private Object getDeltaAbsValue(SnapshotAttributeWriteAbsValue writeAbsValue, SnapshotAttributeReadAbsValue readAbsValue, boolean manageAllTypes) {
        switch(this.dataFormat) {
            case SCALAR_DATA_FORMAT:
                return getScalarDeltaAbsValue(writeAbsValue, readAbsValue, manageAllTypes);
            case SPECTRUM_DATA_FORMAT:
                return getSpectrumDeltaAbsValue(writeAbsValue, readAbsValue, manageAllTypes);
            case IMAGE_DATA_FORMAT:
                return getImageDeltaAbsValue(writeAbsValue, readAbsValue, manageAllTypes);
            default:
                return null;
        }
    }

    /**
     * Returns the difference between the values of a
     * SnapshotAttributewriteAbsValue and a SnapshotAttributeReadAbsValue,
     * provided they are scalar.
     *
     * @param writeAbsValue
     *            The write value
     * @param readAbsValue
     *            The read value
     * @param manageAllTypes
     *            A boolean to manage all types or not. If <code>false</code>,
     *            the delta value will be build only if type corresponds to a
     *            Number. otherwise, all types are managed, and the returned
     *            value for other types is a string that says whether there is a
     *            difference.
     * @return 29 juin 2005
     */
    private Object getScalarDeltaAbsValue(SnapshotAttributeWriteAbsValue writeAbsValue, SnapshotAttributeReadAbsValue readAbsValue, boolean manageAllTypes) {
        if (writeAbsValue == null || readAbsValue == null) {
            if (manageAllTypes) {
                if (writeAbsValue == null && readAbsValue == null) {
                    return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_SAME");
                } else {
                    return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_DIFFERENT");
                }
            } else {
                return null;
            }
        }
        switch(this.getDataType()) {
            case TangoConst.Tango_DEV_USHORT:
            case TangoConst.Tango_DEV_SHORT:
                try {
                    Short writeDouble = (Short) writeAbsValue.getValue();
                    Short readDouble = (Short) readAbsValue.getValue();
                    if (writeDouble == null || readDouble == null) {
                        if (manageAllTypes) {
                            if (writeDouble == null && readDouble == null) {
                                return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_SAME");
                            } else {
                                return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_DIFFERENT");
                            }
                        } else {
                            return null;
                        }
                    }
                    return new Short((short) Math.abs(readDouble.shortValue() - writeDouble.shortValue()));
                } catch (ClassCastException e) {
                    String writeDouble_s = "" + writeAbsValue.getValue();
                    String readDouble_s = "" + readAbsValue.getValue();
                    if ("null".equals(writeDouble_s) || writeDouble_s.equals("") || "null".equals(readDouble_s) || readDouble_s.equals("")) {
                        if (manageAllTypes) {
                            if ("null".equals(writeDouble_s) && "null".equals(readDouble_s)) {
                                return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_SAME");
                            } else if ("".equals(writeDouble_s) && "".equals(readDouble_s)) {
                                return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_SAME");
                            } else {
                                return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_DIFFERENT");
                            }
                        } else {
                            return null;
                        }
                    }
                    double readDouble = Double.parseDouble(readDouble_s);
                    double writeDouble = Double.parseDouble(writeDouble_s);
                    return new Short((short) Math.abs(readDouble - writeDouble));
                }
            case TangoConst.Tango_DEV_DOUBLE:
                try {
                    Double writeDouble = (Double) writeAbsValue.getValue();
                    Double readDouble = (Double) readAbsValue.getValue();
                    if (writeDouble == null || readDouble == null) {
                        if (manageAllTypes) {
                            if (writeDouble == null && readDouble == null) {
                                return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_SAME");
                            } else {
                                return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_DIFFERENT");
                            }
                        } else {
                            return null;
                        }
                    }
                    return new Double(Math.abs(readDouble.doubleValue() - writeDouble.doubleValue()));
                } catch (ClassCastException e) {
                    String writeDouble_s = "" + writeAbsValue.getValue();
                    String readDouble_s = "" + readAbsValue.getValue();
                    if ("null".equals(writeDouble_s) || writeDouble_s.equals("") || "null".equals(readDouble_s) || readDouble_s.equals("")) {
                        if (manageAllTypes) {
                            if ("null".equals(writeDouble_s) && "null".equals(readDouble_s)) {
                                return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_SAME");
                            } else if ("".equals(writeDouble_s) && "".equals(readDouble_s)) {
                                return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_SAME");
                            } else {
                                return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_DIFFERENT");
                            }
                        } else {
                            return null;
                        }
                    }
                    double readDouble = Double.parseDouble(readDouble_s);
                    double writeDouble = Double.parseDouble(writeDouble_s);
                    return new Double(Math.abs(readDouble - writeDouble));
                }
            case TangoConst.Tango_DEV_ULONG:
            case TangoConst.Tango_DEV_LONG:
                try {
                    Integer writeLong = (Integer) writeAbsValue.getValue();
                    Integer readLong = (Integer) readAbsValue.getValue();
                    if (writeLong == null || readLong == null) {
                        if (manageAllTypes) {
                            if (writeLong == null && readLong == null) {
                                return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_SAME");
                            } else {
                                return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_DIFFERENT");
                            }
                        } else {
                            return null;
                        }
                    }
                    return new Integer(Math.abs(readLong.intValue() - writeLong.intValue()));
                } catch (ClassCastException e) {
                    String writeLong_s = "" + writeAbsValue.getValue();
                    String readLong_s = "" + readAbsValue.getValue();
                    if ("null".equals(writeLong_s) || writeLong_s.equals("") || "null".equals(readLong_s) || readLong_s.equals("")) {
                        if (manageAllTypes) {
                            if ("null".equals(writeLong_s) && "null".equals(readLong_s)) {
                                return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_SAME");
                            } else if ("".equals(writeLong_s) && "".equals(readLong_s)) {
                                return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_SAME");
                            } else {
                                return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_DIFFERENT");
                            }
                        } else {
                            return null;
                        }
                    }
                    double readDouble = Double.parseDouble(readLong_s);
                    double writeDouble = Double.parseDouble(writeLong_s);
                    return new Integer((int) Math.abs(readDouble - writeDouble));
                }
            case TangoConst.Tango_DEV_FLOAT:
                try {
                    Float writeFloat = (Float) writeAbsValue.getValue();
                    Float readFloat = (Float) readAbsValue.getValue();
                    if (writeFloat == null || readFloat == null) {
                        if (manageAllTypes) {
                            if (writeFloat == null && readFloat == null) {
                                return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_SAME");
                            } else {
                                return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_DIFFERENT");
                            }
                        } else {
                            return null;
                        }
                    }
                    return new Float(Math.abs(readFloat.longValue() - writeFloat.longValue()));
                } catch (ClassCastException e) {
                    String writeFloat_s = "" + writeAbsValue.getValue();
                    String readFloat_s = "" + readAbsValue.getValue();
                    if ("null".equals(writeFloat_s) || writeFloat_s.equals("") || "null".equals(readFloat_s) || readFloat_s.equals("")) {
                        if (manageAllTypes) {
                            if ("null".equals(writeFloat_s) && "null".equals(readFloat_s)) {
                                return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_SAME");
                            } else if ("".equals(writeFloat_s) && "".equals(readFloat_s)) {
                                return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_SAME");
                            } else {
                                return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_DIFFERENT");
                            }
                        } else {
                            return null;
                        }
                    }
                    float readFloat = Float.parseFloat(readFloat_s);
                    float writeFloat = Float.parseFloat(writeFloat_s);
                    return new Float(Math.abs(readFloat - writeFloat));
                }
            default:
                if (manageAllTypes) {
                    Object write = writeAbsValue.getValue();
                    Object read = readAbsValue.getValue();
                    if (write == null && read == null) {
                        if (write == null && read == null) {
                            return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_SAME");
                        } else {
                            return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_DIFFERENT");
                        }
                    } else {
                        if (write != null && write.equals(read)) {
                            return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_SAME");
                        } else {
                            return Messages.getMessage("SNAPSHOT_COMPARE_VALUE_DIFFERENT");
                        }
                    }
                } else {
                    return null;
                }
        }
    }

    /**
     * @param writeAbsValue
     *            The write value
     * @param readAbsValue
     *            The read value
     * @return 29 juin 2005
     */
    private Object getImageDeltaAbsValue(SnapshotAttributeWriteAbsValue writeAbsValue, SnapshotAttributeReadAbsValue readAbsValue, boolean manageAllTypes) {
        return null;
    }

    /**
     * @param writeAbsValue
     *            The write value
     * @param readAbsValue
     *            The read value
     * @return 29 juin 2005
     */
    private Object getSpectrumDeltaAbsValue(SnapshotAttributeWriteAbsValue writeAbsValue, SnapshotAttributeReadAbsValue readAbsValue, boolean manageAllTypes) {
        if (writeAbsValue == null || readAbsValue == null) {
            return null;
        }
        Object readAbsValueTab = readAbsValue.getSpectrumValue();
        Object writeAbsValueTab = writeAbsValue.getSpectrumValue();
        int readLength = 0;
        int writeLength = 0;
        Byte[] readChar = null, writeChar = null, diffChar = null;
        Integer[] readLong = null, writeLong = null, diffLong = null;
        Short[] readShort = null, writeShort = null, diffShort = null;
        Float[] readFloat = null, writeFloat = null, diffFloat = null;
        Double[] readDouble = null, writeDouble = null, diffDouble = null;
        Boolean[] readBoolean = null, writeBoolean = null;
        String[] readString = null, writeString = null, diffString = null;
        switch(dataType) {
            case TangoConst.Tango_DEV_DOUBLE:
                if (readAbsValueTab != null && !"Nan".equals(readAbsValueTab)) {
                    readDouble = (Double[]) readAbsValueTab;
                    readLength = readDouble.length;
                }
                if (writeAbsValueTab != null && !"Nan".equals(writeAbsValueTab)) {
                    writeDouble = (Double[]) writeAbsValueTab;
                    writeLength = writeDouble.length;
                }
                break;
            case TangoConst.Tango_DEV_FLOAT:
                if (readAbsValueTab != null && !"Nan".equals(readAbsValueTab)) {
                    readFloat = (Float[]) readAbsValueTab;
                    readLength = readFloat.length;
                }
                if (writeAbsValueTab != null && !"Nan".equals(writeAbsValueTab)) {
                    writeFloat = (Float[]) writeAbsValueTab;
                    writeLength = writeFloat.length;
                }
                break;
            case TangoConst.Tango_DEV_USHORT:
            case TangoConst.Tango_DEV_SHORT:
                if (readAbsValueTab != null && !"Nan".equals(readAbsValueTab)) {
                    readShort = (Short[]) readAbsValueTab;
                    readLength = readShort.length;
                }
                if (writeAbsValueTab != null && !"Nan".equals(writeAbsValueTab)) {
                    writeShort = (Short[]) writeAbsValueTab;
                    writeLength = writeShort.length;
                }
                break;
            case TangoConst.Tango_DEV_UCHAR:
            case TangoConst.Tango_DEV_CHAR:
                if (readAbsValueTab != null && !"Nan".equals(readAbsValueTab)) {
                    readChar = (Byte[]) readAbsValueTab;
                    readLength = readChar.length;
                }
                if (writeAbsValueTab != null && !"Nan".equals(writeAbsValueTab)) {
                    writeChar = (Byte[]) writeAbsValueTab;
                    writeLength = writeChar.length;
                }
                break;
            case TangoConst.Tango_DEV_STATE:
                if (!manageAllTypes) {
                    break;
                }
            case TangoConst.Tango_DEV_ULONG:
            case TangoConst.Tango_DEV_LONG:
                if (readAbsValueTab != null && !"Nan".equals(readAbsValueTab)) {
                    readLong = (Integer[]) readAbsValueTab;
                    readLength = readLong.length;
                }
                if (writeAbsValueTab != null && !"Nan".equals(writeAbsValueTab)) {
                    writeLong = (Integer[]) writeAbsValueTab;
                    writeLength = writeLong.length;
                }
                break;
            case TangoConst.Tango_DEV_BOOLEAN:
                if (manageAllTypes) {
                    if (readAbsValueTab != null && !"Nan".equals(readAbsValueTab)) {
                        readBoolean = (Boolean[]) readAbsValueTab;
                        readLength = readBoolean.length;
                    }
                    if (writeAbsValueTab != null && !"Nan".equals(writeAbsValueTab)) {
                        writeBoolean = (Boolean[]) writeAbsValueTab;
                        writeLength = writeBoolean.length;
                    }
                }
                break;
            case TangoConst.Tango_DEV_STRING:
                if (manageAllTypes) {
                    if (readAbsValueTab != null && !"Nan".equals(readAbsValueTab)) {
                        readString = (String[]) readAbsValueTab;
                        readLength = readString.length;
                    }
                    if (writeAbsValueTab != null && !"Nan".equals(writeAbsValueTab)) {
                        writeString = (String[]) writeAbsValueTab;
                        writeLength = writeString.length;
                    }
                }
                break;
            default:
        }
        if (readAbsValueTab == null || readLength == 0) {
            return null;
        }
        if (writeAbsValueTab == null || writeLength == 0) {
            return null;
        }
        if (readLength != writeLength) {
            return null;
        }
        Object[] ret = null;
        switch(dataType) {
            case TangoConst.Tango_DEV_DOUBLE:
                diffDouble = new Double[readLength];
                for (int i = 0; i < diffDouble.length; i++) {
                    diffDouble[i] = new Double(Math.abs(readDouble[i].doubleValue() - writeDouble[i].doubleValue()));
                }
                ret = diffDouble;
                break;
            case TangoConst.Tango_DEV_FLOAT:
                diffFloat = new Float[readLength];
                for (int i = 0; i < diffFloat.length; i++) {
                    diffFloat[i] = new Float(Math.abs(readFloat[i].floatValue() - writeFloat[i].floatValue()));
                }
                ret = diffFloat;
                break;
            case TangoConst.Tango_DEV_USHORT:
            case TangoConst.Tango_DEV_SHORT:
                diffShort = new Short[readLength];
                for (int i = 0; i < diffShort.length; i++) {
                    diffShort[i] = new Short((short) Math.abs(readShort[i].shortValue() - writeShort[i].shortValue()));
                }
                ret = diffShort;
                break;
            case TangoConst.Tango_DEV_UCHAR:
            case TangoConst.Tango_DEV_CHAR:
                diffChar = new Byte[readLength];
                for (int i = 0; i < diffChar.length; i++) {
                    diffChar[i] = new Byte((byte) Math.abs(readChar[i].byteValue() - writeChar[i].byteValue()));
                }
                ret = diffChar;
                break;
            case TangoConst.Tango_DEV_ULONG:
            case TangoConst.Tango_DEV_LONG:
                diffLong = new Integer[readLength];
                for (int i = 0; i < diffLong.length; i++) {
                    diffLong[i] = new Integer(Math.abs(readLong[i].intValue() - writeLong[i].intValue()));
                }
                ret = diffLong;
                break;
            case TangoConst.Tango_DEV_STATE:
                if (manageAllTypes) {
                    diffString = new String[readLength];
                    for (int i = 0; i < diffString.length; i++) {
                        if (readLong[i] == null && writeLong[i] == null) {
                            diffString[i] = Messages.getMessage("SNAPSHOT_COMPARE_VALUE_SAME");
                        } else if (readLong[i] != null && readLong[i].equals(writeLong[i])) {
                            diffString[i] = Messages.getMessage("SNAPSHOT_COMPARE_VALUE_SAME");
                        } else {
                            diffString[i] = Messages.getMessage("SNAPSHOT_COMPARE_VALUE_DIFFERENT");
                        }
                    }
                    ret = diffString;
                }
                break;
            case TangoConst.Tango_DEV_BOOLEAN:
                if (manageAllTypes) {
                    diffString = new String[readLength];
                    for (int i = 0; i < diffString.length; i++) {
                        if (readBoolean[i] == null && writeBoolean[i] == null) {
                            diffString[i] = Messages.getMessage("SNAPSHOT_COMPARE_VALUE_SAME");
                        } else if (readBoolean[i] != null && readBoolean[i].equals(writeBoolean[i])) {
                            diffString[i] = Messages.getMessage("SNAPSHOT_COMPARE_VALUE_SAME");
                        } else {
                            diffString[i] = Messages.getMessage("SNAPSHOT_COMPARE_VALUE_DIFFERENT");
                        }
                    }
                    ret = diffString;
                }
                break;
            case TangoConst.Tango_DEV_STRING:
                if (manageAllTypes) {
                    diffString = new String[readLength];
                    for (int i = 0; i < diffString.length; i++) {
                        if (readString[i] == null && writeString[i] == null) {
                            diffString[i] = Messages.getMessage("SNAPSHOT_COMPARE_VALUE_SAME");
                        } else if (readString[i] != null && readString[i].equals(writeString[i])) {
                            diffString[i] = Messages.getMessage("SNAPSHOT_COMPARE_VALUE_SAME");
                        } else {
                            diffString[i] = Messages.getMessage("SNAPSHOT_COMPARE_VALUE_DIFFERENT");
                        }
                    }
                    ret = diffString;
                }
                break;
            default:
        }
        return ret;
    }

    /**
     * Returns a SnapshotAttributeDeltaAbsValue calculated from the values of
     * writeAbsValue and readAbsValue when possible. When impossible, returns a
     * "Not applicable" SnapshotAttributeDeltaAbsValue.
     *
     * @param writeAbsValue
     *            The write value
     * @param readAbsValue
     *            The read value
     * @return A SnapshotAttributeDeltaAbsValue calculated from the values of
     *         writeAbsValue and readAbsValue when possible
     */
    public static SnapshotAttributeDeltaAbsValue getInstance(SnapshotAttributeWriteAbsValue writeAbsValue, SnapshotAttributeReadAbsValue readAbsValue) {
        switch(writeAbsValue.getDataType()) {
            case TangoConst.Tango_DEV_FLOAT:
            case TangoConst.Tango_DEV_ULONG:
            case TangoConst.Tango_DEV_LONG:
            case TangoConst.Tango_DEV_DOUBLE:
            case TangoConst.Tango_DEV_USHORT:
            case TangoConst.Tango_DEV_SHORT:
            case TangoConst.Tango_DEV_UCHAR:
            case TangoConst.Tango_DEV_CHAR:
                return new SnapshotAttributeDeltaAbsValue(writeAbsValue, readAbsValue);
            default:
                return new SnapshotAttributeDeltaAbsValue(SnapshotAttributeValue.NOT_APPLICABLE_DATA_FORMAT, null);
        }
    }
}
