package fr.soleil.TangoArchiving.ArchivingTools.Tools;

import fr.esrf.Tango.AttrWriteType;
import fr.esrf.Tango.AttrDataFormat;
import fr.esrf.Tango.DevState;
import fr.esrf.TangoDs.TangoConst;
import fr.soleil.TangoArchiving.ArchivingTools.Tools.GlobalConst;

public class ScalarEvent extends ArchivingEvent {

    private static final String MIN_VALUE = "1e-100";

    private static double minAbsoluteValue = Double.parseDouble(MIN_VALUE);

    /**
	 * Creates a new instance of DhdbEvent
	 */
    public ScalarEvent() {
        super();
    }

    public ScalarEvent(String att_name, int data_type, int writable, long timestamp, Object value) {
        super();
        this.setAttribute_complete_name(att_name);
        this.setData_format(AttrDataFormat._SCALAR);
        this.setData_type(data_type);
        this.setWritable(writable);
        this.setTimeStamp(timestamp);
        setValue(value);
    }

    public Object getReadValue() {
        if (getValue() == null) return null;
        Object read_value = new Object();
        switch(getWritable()) {
            case AttrWriteType._READ:
                switch(getData_type()) {
                    case TangoConst.Tango_DEV_STRING:
                        read_value = (String) getValue();
                        break;
                    case TangoConst.Tango_DEV_STATE:
                        if (getValue() instanceof DevState) {
                            read_value = (DevState) getValue();
                        } else if ((getValue() instanceof Integer[])) {
                            read_value = (Integer[]) getValue();
                        } else read_value = (String) getValue();
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
                    case TangoConst.Tango_DEV_USHORT:
                        read_value = (Short) getValue();
                        break;
                    case TangoConst.Tango_DEV_FLOAT:
                        read_value = (Float) getValue();
                        break;
                    case TangoConst.Tango_DEV_DOUBLE:
                        read_value = (Double) getValue();
                        break;
                    default:
                        read_value = null;
                        break;
                }
                break;
            case AttrWriteType._WRITE:
                switch(getData_type()) {
                    case TangoConst.Tango_DEV_STRING:
                        read_value = (String) getValue();
                        break;
                    case TangoConst.Tango_DEV_STATE:
                        if (getValue() instanceof DevState) {
                            read_value = (DevState) getValue();
                        } else if ((getValue() instanceof Integer[])) {
                            read_value = (Integer[]) getValue();
                        } else read_value = (String) getValue();
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
                    case TangoConst.Tango_DEV_USHORT:
                        read_value = (Short) getValue();
                        break;
                    case TangoConst.Tango_DEV_FLOAT:
                        read_value = (Float) getValue();
                        break;
                    case TangoConst.Tango_DEV_DOUBLE:
                        read_value = (Double) getValue();
                        break;
                    default:
                        read_value = null;
                        break;
                }
                break;
            case AttrWriteType._READ_WITH_WRITE:
            case AttrWriteType._READ_WRITE:
                switch(getData_type()) {
                    case TangoConst.Tango_DEV_STRING:
                        read_value = ((String[]) getValue())[0];
                        break;
                    case TangoConst.Tango_DEV_STATE:
                        if (getValue() instanceof DevState) {
                            read_value = (DevState) getValue();
                        } else if ((getValue() instanceof Integer[])) {
                            read_value = (Integer[]) getValue();
                        } else read_value = (String) getValue();
                        break;
                    case TangoConst.Tango_DEV_UCHAR:
                        read_value = ((Byte[]) getValue())[0];
                        break;
                    case TangoConst.Tango_DEV_LONG:
                    case TangoConst.Tango_DEV_ULONG:
                        read_value = ((Integer[]) getValue())[0];
                        break;
                    case TangoConst.Tango_DEV_BOOLEAN:
                        read_value = ((Boolean[]) getValue())[0];
                        break;
                    case TangoConst.Tango_DEV_SHORT:
                    case TangoConst.Tango_DEV_USHORT:
                        read_value = ((Short[]) getValue())[0];
                        break;
                    case TangoConst.Tango_DEV_FLOAT:
                        read_value = ((Float[]) getValue())[0];
                        break;
                    case TangoConst.Tango_DEV_DOUBLE:
                        read_value = ((Double[]) getValue())[0];
                        break;
                    default:
                        read_value = null;
                        break;
                }
                break;
        }
        return read_value;
    }

    /**
	 * Returns an array representation of the object <I>ArchivingEvent</I>.
	 *
	 * @return an array representation of the object <I>ArchivingEvent</I>.
	 */
    public String[] toArray() {
        String[] scalarEvent;
        scalarEvent = new String[8];
        scalarEvent[0] = getAttribute_complete_name();
        scalarEvent[1] = Integer.toString(getData_format());
        scalarEvent[2] = Integer.toString(getData_type());
        scalarEvent[3] = Integer.toString(getWritable());
        scalarEvent[4] = Long.toString(getTimeStamp());
        scalarEvent[5] = getTable_name();
        scalarEvent[6] = valueToString(0);
        scalarEvent[7] = valueToString(1);
        return scalarEvent;
    }

    public String valueToString(int pos) {
        if (getValue() == null) return GlobalConst.ARCHIVER_NULL_VALUE;
        if (getValue() instanceof Object[] && ((Object[]) getValue())[pos] == null) return GlobalConst.ARCHIVER_NULL_VALUE;
        String value = GlobalConst.ARCHIVER_NULL_VALUE;
        switch(getData_format()) {
            case AttrDataFormat._SCALAR:
                switch(getWritable()) {
                    case AttrWriteType._READ:
                        if (pos == 0) {
                            switch(getData_type()) {
                                case TangoConst.Tango_DEV_STRING:
                                    value = (String) getValue();
                                    break;
                                case TangoConst.Tango_DEV_STATE:
                                    if (getValue() instanceof DevState) {
                                        value = "" + ((DevState) getValue()).value();
                                    } else if ((getValue() instanceof Integer[])) {
                                        value = "";
                                        Integer[] tab = (Integer[]) getValue();
                                        for (int i = 0; i < tab.length; i++) {
                                            value += tab[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        if (tab.length > 0) {
                                            value = value.substring(0, value.length() - 1);
                                        }
                                    } else value = (String) getValue();
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
                                case TangoConst.Tango_DEV_USHORT:
                                    value = ((Short) getValue()).toString();
                                    break;
                                case TangoConst.Tango_DEV_FLOAT:
                                    value = ((Float) getValue()).toString();
                                    break;
                                case TangoConst.Tango_DEV_DOUBLE:
                                    value = ((Double) getValue()).toString();
                                    break;
                                default:
                                    value = GlobalConst.ARCHIVER_NULL_VALUE;
                                    break;
                            }
                        }
                        break;
                    case AttrWriteType._READ_WITH_WRITE:
                        switch(getData_type()) {
                            case TangoConst.Tango_DEV_STRING:
                                value = ((String[]) getValue())[pos];
                                break;
                            case TangoConst.Tango_DEV_STATE:
                                if (getValue() instanceof DevState) {
                                    value = "" + ((DevState) getValue()).value();
                                } else if ((getValue() instanceof Integer[])) {
                                    value = "";
                                    Integer[] tab = (Integer[]) getValue();
                                    value += tab[pos];
                                } else value = ((String[]) getValue())[pos];
                                break;
                            case TangoConst.Tango_DEV_UCHAR:
                                value = ((Byte[]) getValue())[pos] + "";
                                break;
                            case TangoConst.Tango_DEV_LONG:
                            case TangoConst.Tango_DEV_ULONG:
                                value = ((Integer[]) getValue())[pos] + "";
                                break;
                            case TangoConst.Tango_DEV_BOOLEAN:
                                value = ((Boolean[]) getValue())[pos] + "";
                                break;
                            case TangoConst.Tango_DEV_SHORT:
                            case TangoConst.Tango_DEV_USHORT:
                                value = ((Short[]) getValue())[pos] + "";
                                break;
                            case TangoConst.Tango_DEV_FLOAT:
                                value = ((Float[]) getValue())[pos] + "";
                                break;
                            case TangoConst.Tango_DEV_DOUBLE:
                                value = ((Double[]) getValue())[pos] + "";
                                break;
                            default:
                                value = GlobalConst.ARCHIVER_NULL_VALUE;
                                break;
                        }
                        break;
                    case AttrWriteType._WRITE:
                        if (pos == 1) {
                            switch(getData_type()) {
                                case TangoConst.Tango_DEV_STRING:
                                    value = (String) getValue();
                                    break;
                                case TangoConst.Tango_DEV_STATE:
                                    if (getValue() instanceof DevState) {
                                        value = "" + ((DevState) getValue()).value();
                                    } else if ((getValue() instanceof Integer[])) {
                                        value = "";
                                        Integer[] tab = (Integer[]) getValue();
                                        for (int i = 0; i < tab.length; i++) {
                                            value += tab[i] + GlobalConst.CLOB_SEPARATOR;
                                        }
                                        if (tab.length > 0) {
                                            value = value.substring(0, value.length() - 1);
                                        }
                                    } else value = (String) getValue();
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
                                case TangoConst.Tango_DEV_USHORT:
                                    value = ((Short) getValue()).toString();
                                    break;
                                case TangoConst.Tango_DEV_FLOAT:
                                    value = ((Float) getValue()).toString();
                                    break;
                                case TangoConst.Tango_DEV_DOUBLE:
                                    value = ((Double) getValue()).toString();
                                    break;
                                default:
                                    value = GlobalConst.ARCHIVER_NULL_VALUE;
                                    break;
                            }
                        }
                        break;
                    case AttrWriteType._READ_WRITE:
                        switch(getData_type()) {
                            case TangoConst.Tango_DEV_STRING:
                                value = ((String[]) getValue())[pos];
                                break;
                            case TangoConst.Tango_DEV_STATE:
                                if (getValue() instanceof DevState) {
                                    value = "" + ((DevState) getValue()).value();
                                } else if ((getValue() instanceof Integer[])) {
                                    value = "";
                                    Integer[] tab = (Integer[]) getValue();
                                    value += tab[pos];
                                } else value = ((String[]) getValue())[pos];
                                break;
                            case TangoConst.Tango_DEV_UCHAR:
                                value = ((Byte[]) getValue())[pos] + "";
                                break;
                            case TangoConst.Tango_DEV_ULONG:
                            case TangoConst.Tango_DEV_LONG:
                                value = ((Integer[]) getValue())[pos] + "";
                                break;
                            case TangoConst.Tango_DEV_BOOLEAN:
                                value = ((Boolean[]) getValue())[pos] + "";
                                break;
                            case TangoConst.Tango_DEV_SHORT:
                            case TangoConst.Tango_DEV_USHORT:
                                value = ((Short[]) getValue())[pos] + "";
                                break;
                            case TangoConst.Tango_DEV_FLOAT:
                                value = ((Float[]) getValue())[pos] + "";
                                break;
                            case TangoConst.Tango_DEV_DOUBLE:
                                value = ((Double[]) getValue())[pos] + "";
                                break;
                            default:
                                value = GlobalConst.ARCHIVER_NULL_VALUE;
                                break;
                        }
                        break;
                }
                break;
            case AttrDataFormat._SPECTRUM:
            case AttrDataFormat._IMAGE:
                value = getValue().toString();
                break;
        }
        return value;
    }

    public String toString() {
        String scEvSt = "";
        String value = ((getWritable() == AttrWriteType._READ || getWritable() == AttrWriteType._READ_WITH_WRITE || getWritable() == AttrWriteType._READ_WRITE) ? "read value :  " + valueToString(0) : "") + ((getWritable() == AttrWriteType._WRITE || getWritable() == AttrWriteType._READ_WITH_WRITE || getWritable() == AttrWriteType._READ_WRITE) ? "\t " + "write value : " + valueToString(1) : "");
        scEvSt = "Source : \t" + getAttribute_complete_name() + "\r\n" + "TimeSt : \t" + getTimeStamp() + "\r\n" + "Value  : \t" + value + "\r\n";
        return scEvSt;
    }

    public void avoidUnderFlow() {
        if (super.getData_type() != TangoConst.Tango_DEV_DOUBLE || super.getValue() == null) {
            return;
        }
        switch(super.getWritable()) {
            case AttrWriteType._READ:
            case AttrWriteType._WRITE:
                Double valueEither = (Double) getValue();
                valueEither = avoidUnderFlow(valueEither);
                super.setValue(valueEither);
                break;
            case AttrWriteType._READ_WRITE:
            case AttrWriteType._READ_WITH_WRITE:
                Double[] valueBoth = (Double[]) getValue();
                valueBoth[0] = avoidUnderFlow(valueBoth[0]);
                valueBoth[1] = avoidUnderFlow(valueBoth[1]);
                super.setValue(valueBoth);
                break;
        }
    }

    private Double avoidUnderFlow(Double value) {
        if (value == null) {
            return value;
        } else {
            double absoluteValue = Math.abs(value.doubleValue());
            double ret;
            if (absoluteValue < minAbsoluteValue) {
                ret = 0.0;
            } else {
                ret = value.doubleValue();
            }
            return new Double(ret);
        }
    }
}
