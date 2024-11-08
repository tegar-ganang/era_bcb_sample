package fr.soleil.hdbtdbArchivingApi.ArchivingTools.Tools;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.Date;
import fr.esrf.Tango.AttrDataFormat;
import fr.esrf.Tango.AttrWriteType;
import fr.esrf.Tango.DevState;
import fr.esrf.TangoDs.TangoConst;
import fr.soleil.commonarchivingapi.ArchivingTools.Tools.GlobalConst;

public class ScalarEvent extends ArchivingEvent {

    private static final String MIN_VALUE = "1e-100";

    private static double minAbsoluteValue = Double.parseDouble(MIN_VALUE);

    /**
     * Creates a new instance of DhdbEvent
     */
    public ScalarEvent() {
        super();
    }

    public ScalarEvent(final String attributeName, final int dataType, final int writable, final long timestamp, final Object value) {
        super();
        setAttribute_complete_name(attributeName);
        setData_format(AttrDataFormat._SCALAR);
        setData_type(dataType);
        setWritable(writable);
        setTimeStamp(timestamp);
        setValue(value);
    }

    public Object getReadValue() {
        Object read_value = null;
        switch(getWritable()) {
            case AttrWriteType._READ:
            case AttrWriteType._WRITE:
                read_value = getValue();
                break;
            case AttrWriteType._READ_WITH_WRITE:
            case AttrWriteType._READ_WRITE:
                if (getValue() != null) {
                    read_value = Array.get(getValue(), 0);
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
    @Override
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

    public String valueToString(final int pos) {
        if (getValue() == null) {
            return GlobalConst.ARCHIVER_NULL_VALUE;
        }
        if (getValue() instanceof Object[] && ((Object[]) getValue())[pos] == null) {
            return GlobalConst.ARCHIVER_NULL_VALUE;
        }
        String value = GlobalConst.ARCHIVER_NULL_VALUE;
        switch(getData_format()) {
            case AttrDataFormat._SCALAR:
                switch(getWritable()) {
                    case AttrWriteType._READ:
                    case AttrWriteType._WRITE:
                        if (getValue() instanceof DevState) {
                            value = String.valueOf(((DevState) getValue()).value());
                        } else {
                            value = getValue().toString();
                        }
                        break;
                    case AttrWriteType._READ_WITH_WRITE:
                    case AttrWriteType._READ_WRITE:
                        value = Array.get(getValue(), pos).toString();
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

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public String toString() {
        String scEvSt = "";
        final String value = (getWritable() == AttrWriteType._READ || getWritable() == AttrWriteType._READ_WITH_WRITE || getWritable() == AttrWriteType._READ_WRITE ? "read value :  " + valueToString(0) : "") + (getWritable() == AttrWriteType._WRITE || getWritable() == AttrWriteType._READ_WITH_WRITE || getWritable() == AttrWriteType._READ_WRITE ? "\t " + "write value : " + valueToString(1) : "");
        scEvSt = getAttribute_complete_name() + "[timestamp: " + DATE_FORMAT.format(new Date(getTimeStamp())) + " - value: " + value + "]";
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
                final Double[] valueBoth = (Double[]) getValue();
                valueBoth[0] = avoidUnderFlow(valueBoth[0]);
                valueBoth[1] = avoidUnderFlow(valueBoth[1]);
                super.setValue(valueBoth);
                break;
        }
    }

    private Double avoidUnderFlow(final Double value) {
        if (value == null) {
            return value;
        } else {
            final double absoluteValue = Math.abs(value.doubleValue());
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
