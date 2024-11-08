package fr.soleil.commonarchivingapi.ArchivingTools.Tools;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import fr.esrf.Tango.AttrDataFormat;
import fr.esrf.Tango.AttrWriteType;
import fr.esrf.Tango.DevError;
import fr.esrf.Tango.DevFailed;
import fr.esrf.Tango.TimeVal;
import fr.esrf.TangoDs.TangoConst;
import fr.esrf.TangoDs.TimedAttrData;

public class DbData implements java.io.Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -3201706378068433964L;

    protected String name;

    /**
     * Max size for spectrums and images
     */
    private int maxX;

    private int maxY;

    /**
     * Data type
     */
    protected int data_type;

    /**
     * Data format
     */
    protected int dataFormat;

    /**
     * Writable
     */
    protected int writable;

    /**
     * Data found in HDB/TDB
     */
    protected NullableTimedData[] timedData;

    public DbData(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public int getData_type() {
        return data_type;
    }

    public void setData_type(final int data_type) {
        this.data_type = data_type;
    }

    public int getData_format() {
        return dataFormat;
    }

    public void setData_format(final int data_format) {
        this.dataFormat = data_format;
    }

    public int getWritable() {
        return writable;
    }

    public void setWritable(final int writable) {
        this.writable = writable;
    }

    public int getMax_x() {
        return maxX;
    }

    public void setMax_x(final int max_x) {
        this.maxX = max_x;
    }

    public int getMax_y() {
        return maxY;
    }

    public void setMax_y(final int max_y) {
        this.maxY = max_y;
    }

    public NullableTimedData[] getData_timed() {
        return timedData;
    }

    public void setData_timed(final NullableTimedData[] data) {
        timedData = data;
    }

    public int size() {
        return timedData != null ? timedData.length : 0;
    }

    /**
     * This method is used to cope with today attribute tango java limitations
     * (read/write attribute are not supported !!).
     * 
     * @return A two DbData object array; the first DbData is for read values;
     *         the second one for write values.
     */
    public DbData[] splitDbData() throws DevFailed {
        final DbData[] argout = new DbData[2];
        final DbData dbData_r = new DbData(name);
        dbData_r.setData_type(data_type);
        dbData_r.setData_format(dataFormat);
        dbData_r.setWritable(writable);
        dbData_r.setMax_x(maxX);
        dbData_r.setMax_y(maxY);
        final DbData dbData_w = new DbData(name);
        dbData_w.setData_type(data_type);
        dbData_w.setData_format(dataFormat);
        dbData_w.setWritable(writable);
        dbData_w.setMax_x(0);
        dbData_w.setMax_y(maxY);
        if (timedData == null) {
            dbData_r.timedData = null;
            dbData_w.timedData = null;
            dbData_r.maxX = 0;
            dbData_r.maxY = 0;
            dbData_w.maxX = 0;
            dbData_w.maxY = 0;
            argout[0] = dbData_r;
            argout[1] = dbData_w;
            return argout;
        }
        final NullableTimedData[] timedAttrData_r = new NullableTimedData[timedData.length];
        final NullableTimedData[] timedAttrData_w = new NullableTimedData[timedData.length];
        for (int i = 0; i < timedData.length; i++) {
            int dimWrite = 0;
            switch(getData_type()) {
                case TangoConst.Tango_DEV_USHORT:
                case TangoConst.Tango_DEV_SHORT:
                case TangoConst.Tango_DEV_UCHAR:
                    final Short[] sh_ptr_init = (Short[]) timedData[i].value;
                    Short[] sh_ptr_read = null;
                    Short[] sh_ptr_write = null;
                    if (sh_ptr_init != null) {
                        if (dataFormat == AttrDataFormat._IMAGE) {
                            sh_ptr_read = new Short[timedData[i].x * timedData[i].y];
                            dimWrite = sh_ptr_init.length - timedData[i].x * timedData[i].y;
                        } else {
                            sh_ptr_read = new Short[timedData[i].x];
                            dimWrite = sh_ptr_init.length - timedData[i].x;
                            if (dimWrite > dbData_w.getMax_x()) {
                                dbData_w.setMax_x(dimWrite);
                            }
                        }
                        if (dimWrite < 0) {
                            dimWrite = 0;
                        }
                        sh_ptr_write = new Short[dimWrite];
                        if (dataFormat == AttrDataFormat._IMAGE) {
                            for (int j = 0; j < timedData[i].x * timedData[i].y; j++) {
                                sh_ptr_read[j] = sh_ptr_init[j];
                            }
                            for (int j = timedData[i].x * timedData[i].y; j < sh_ptr_init.length; j++) {
                                sh_ptr_write[j - timedData[i].x] = sh_ptr_init[j];
                            }
                        } else {
                            for (int j = 0; j < timedData[i].x; j++) {
                                sh_ptr_read[j] = sh_ptr_init[j];
                            }
                            for (int j = timedData[i].x; j < sh_ptr_init.length; j++) {
                                sh_ptr_write[j - timedData[i].x] = sh_ptr_init[j];
                            }
                        }
                    }
                    timedAttrData_r[i] = new NullableTimedData();
                    timedAttrData_r[i].data_type = data_type;
                    timedAttrData_r[i].x = timedData[i].x;
                    timedAttrData_r[i].y = timedData[i].y;
                    timedAttrData_r[i].time = timedData[i].time;
                    timedAttrData_w[i] = new NullableTimedData();
                    timedAttrData_w[i].data_type = data_type;
                    timedAttrData_w[i].x = timedData[i].x;
                    timedAttrData_w[i].y = timedData[i].y;
                    timedAttrData_w[i].time = timedData[i].time;
                    if (writable == AttrWriteType._WRITE) {
                        timedAttrData_r[i].value = sh_ptr_write;
                        timedAttrData_w[i].value = sh_ptr_read;
                    } else {
                        timedAttrData_r[i].value = sh_ptr_read;
                        timedAttrData_w[i].value = sh_ptr_write;
                    }
                    break;
                case TangoConst.Tango_DEV_DOUBLE:
                    final Double[] db_ptr_init = (Double[]) timedData[i].value;
                    Double[] db_ptr_read = null;
                    Double[] db_ptr_write = null;
                    if (db_ptr_init != null) {
                        if (dataFormat == AttrDataFormat._IMAGE) {
                            db_ptr_read = new Double[timedData[i].x * timedData[i].y];
                            dimWrite = db_ptr_init.length - timedData[i].x * timedData[i].y;
                        } else {
                            db_ptr_read = new Double[timedData[i].x];
                            dimWrite = db_ptr_init.length - timedData[i].x;
                            if (dimWrite > dbData_w.getMax_x()) {
                                dbData_w.setMax_x(dimWrite);
                            }
                        }
                        if (dimWrite < 0) {
                            dimWrite = 0;
                        }
                        db_ptr_write = new Double[dimWrite];
                        if (dataFormat == AttrDataFormat._IMAGE) {
                            for (int j = 0; j < timedData[i].x * timedData[i].y; j++) {
                                db_ptr_read[j] = db_ptr_init[j];
                            }
                            for (int j = timedData[i].x * timedData[i].y; j < db_ptr_init.length; j++) {
                                db_ptr_write[j - timedData[i].x] = db_ptr_init[j];
                            }
                        } else {
                            for (int j = 0; j < timedData[i].x; j++) {
                                db_ptr_read[j] = db_ptr_init[j];
                            }
                            for (int j = timedData[i].x; j < db_ptr_init.length; j++) {
                                db_ptr_write[j - timedData[i].x] = db_ptr_init[j];
                            }
                        }
                    }
                    timedAttrData_r[i] = new NullableTimedData();
                    timedAttrData_r[i].data_type = data_type;
                    timedAttrData_r[i].x = timedData[i].x;
                    timedAttrData_r[i].y = timedData[i].y;
                    timedAttrData_r[i].time = timedData[i].time;
                    timedAttrData_w[i] = new NullableTimedData();
                    timedAttrData_w[i].data_type = data_type;
                    timedAttrData_w[i].x = timedData[i].x;
                    timedAttrData_w[i].y = timedData[i].y;
                    timedAttrData_w[i].time = timedData[i].time;
                    if (writable == AttrWriteType._WRITE) {
                        timedAttrData_r[i].value = db_ptr_write;
                        timedAttrData_w[i].value = db_ptr_read;
                    } else {
                        timedAttrData_r[i].value = db_ptr_read;
                        timedAttrData_w[i].value = db_ptr_write;
                    }
                    break;
                case TangoConst.Tango_DEV_FLOAT:
                    final Float[] fl_ptr_init = (Float[]) timedData[i].value;
                    Float[] fl_ptr_read = null;
                    Float[] fl_ptr_write = null;
                    if (fl_ptr_init != null) {
                        if (dataFormat == AttrDataFormat._IMAGE) {
                            fl_ptr_read = new Float[timedData[i].x * timedData[i].y];
                            dimWrite = fl_ptr_init.length - timedData[i].x * timedData[i].y;
                        } else {
                            fl_ptr_read = new Float[timedData[i].x];
                            dimWrite = fl_ptr_init.length - timedData[i].x;
                            if (dimWrite > dbData_w.getMax_x()) {
                                dbData_w.setMax_x(dimWrite);
                            }
                        }
                        if (dimWrite < 0) {
                            dimWrite = 0;
                        }
                        fl_ptr_write = new Float[dimWrite];
                        if (dataFormat == AttrDataFormat._IMAGE) {
                            for (int j = 0; j < timedData[i].x * timedData[i].y; j++) {
                                fl_ptr_read[j] = fl_ptr_init[j];
                            }
                            for (int j = timedData[i].x * timedData[i].y; j < fl_ptr_init.length; j++) {
                                fl_ptr_write[j - timedData[i].x] = fl_ptr_init[j];
                            }
                        } else {
                            for (int j = 0; j < timedData[i].x; j++) {
                                fl_ptr_read[j] = fl_ptr_init[j];
                            }
                            for (int j = timedData[i].x; j < fl_ptr_init.length; j++) {
                                fl_ptr_write[j - timedData[i].x] = fl_ptr_init[j];
                            }
                        }
                    }
                    timedAttrData_r[i] = new NullableTimedData();
                    timedAttrData_r[i].data_type = data_type;
                    timedAttrData_r[i].x = timedData[i].x;
                    timedAttrData_r[i].y = timedData[i].y;
                    timedAttrData_r[i].time = timedData[i].time;
                    timedAttrData_w[i] = new NullableTimedData();
                    timedAttrData_w[i].data_type = data_type;
                    timedAttrData_w[i].x = timedData[i].x;
                    timedAttrData_w[i].y = timedData[i].y;
                    timedAttrData_w[i].time = timedData[i].time;
                    if (writable == AttrWriteType._WRITE) {
                        timedAttrData_r[i].value = fl_ptr_write;
                        timedAttrData_w[i].value = fl_ptr_read;
                    } else {
                        timedAttrData_r[i].value = fl_ptr_read;
                        timedAttrData_w[i].value = fl_ptr_write;
                    }
                    break;
                case TangoConst.Tango_DEV_STATE:
                case TangoConst.Tango_DEV_ULONG:
                case TangoConst.Tango_DEV_LONG:
                    final Integer[] lg_ptr_init = (Integer[]) timedData[i].value;
                    Integer[] lg_ptr_read = null;
                    Integer[] lg_ptr_write = null;
                    if (lg_ptr_init != null) {
                        if (dataFormat == AttrDataFormat._IMAGE) {
                            lg_ptr_read = new Integer[timedData[i].x * timedData[i].y];
                            dimWrite = lg_ptr_init.length - timedData[i].x * timedData[i].y;
                        } else {
                            lg_ptr_read = new Integer[timedData[i].x];
                            dimWrite = lg_ptr_init.length - timedData[i].x;
                            if (dimWrite > dbData_w.getMax_x()) {
                                dbData_w.setMax_x(dimWrite);
                            }
                        }
                        if (dimWrite < 0) {
                            dimWrite = 0;
                        }
                        lg_ptr_write = new Integer[dimWrite];
                        if (dataFormat == AttrDataFormat._IMAGE) {
                            for (int j = 0; j < timedData[i].x * timedData[i].y; j++) {
                                lg_ptr_read[j] = lg_ptr_init[j];
                            }
                            for (int j = timedData[i].x * timedData[i].y; j < lg_ptr_init.length; j++) {
                                lg_ptr_write[j - timedData[i].x] = lg_ptr_init[j];
                            }
                        } else {
                            for (int j = 0; j < timedData[i].x; j++) {
                                lg_ptr_read[j] = lg_ptr_init[j];
                            }
                            for (int j = timedData[i].x; j < lg_ptr_init.length; j++) {
                                lg_ptr_write[j - timedData[i].x] = lg_ptr_init[j];
                            }
                        }
                    }
                    timedAttrData_r[i] = new NullableTimedData();
                    timedAttrData_r[i].data_type = data_type;
                    timedAttrData_r[i].x = timedData[i].x;
                    timedAttrData_r[i].y = timedData[i].y;
                    timedAttrData_r[i].time = timedData[i].time;
                    timedAttrData_w[i] = new NullableTimedData();
                    timedAttrData_w[i].data_type = data_type;
                    timedAttrData_w[i].x = timedData[i].x;
                    timedAttrData_w[i].y = timedData[i].y;
                    timedAttrData_w[i].time = timedData[i].time;
                    if (writable == AttrWriteType._WRITE) {
                        timedAttrData_r[i].value = lg_ptr_write;
                        timedAttrData_w[i].value = lg_ptr_read;
                    } else {
                        timedAttrData_r[i].value = lg_ptr_read;
                        timedAttrData_w[i].value = lg_ptr_write;
                    }
                    break;
                case TangoConst.Tango_DEV_LONG64:
                case TangoConst.Tango_DEV_ULONG64:
                    final Long[] lg_ptr_init2 = (Long[]) timedData[i].value;
                    Long[] lg_ptr_read2 = null;
                    Long[] lg_ptr_write2 = null;
                    if (lg_ptr_init2 != null) {
                        if (dataFormat == AttrDataFormat._IMAGE) {
                            lg_ptr_read2 = new Long[timedData[i].x * timedData[i].y];
                            dimWrite = lg_ptr_init2.length - timedData[i].x * timedData[i].y;
                        } else {
                            lg_ptr_read2 = new Long[timedData[i].x];
                            dimWrite = lg_ptr_init2.length - timedData[i].x;
                            if (dimWrite > dbData_w.getMax_x()) {
                                dbData_w.setMax_x(dimWrite);
                            }
                        }
                        if (dimWrite < 0) {
                            dimWrite = 0;
                        }
                        lg_ptr_write2 = new Long[dimWrite];
                        if (dataFormat == AttrDataFormat._IMAGE) {
                            for (int j = 0; j < timedData[i].x * timedData[i].y; j++) {
                                lg_ptr_read2[j] = lg_ptr_init2[j];
                            }
                            for (int j = timedData[i].x * timedData[i].y; j < lg_ptr_init2.length; j++) {
                                lg_ptr_write2[j - timedData[i].x] = lg_ptr_init2[j];
                            }
                        } else {
                            for (int j = 0; j < timedData[i].x; j++) {
                                lg_ptr_read2[j] = lg_ptr_init2[j];
                            }
                            for (int j = timedData[i].x; j < lg_ptr_init2.length; j++) {
                                lg_ptr_write2[j - timedData[i].x] = lg_ptr_init2[j];
                            }
                        }
                    }
                    timedAttrData_r[i] = new NullableTimedData();
                    timedAttrData_r[i].data_type = data_type;
                    timedAttrData_r[i].x = timedData[i].x;
                    timedAttrData_r[i].y = timedData[i].y;
                    timedAttrData_r[i].time = timedData[i].time;
                    timedAttrData_w[i] = new NullableTimedData();
                    timedAttrData_w[i].data_type = data_type;
                    timedAttrData_w[i].x = timedData[i].x;
                    timedAttrData_w[i].y = timedData[i].y;
                    timedAttrData_w[i].time = timedData[i].time;
                    if (writable == AttrWriteType._WRITE) {
                        timedAttrData_r[i].value = lg_ptr_write2;
                        timedAttrData_w[i].value = lg_ptr_read2;
                    } else {
                        timedAttrData_r[i].value = lg_ptr_read2;
                        timedAttrData_w[i].value = lg_ptr_write2;
                    }
                    break;
                case TangoConst.Tango_DEV_BOOLEAN:
                    final Boolean[] bool_ptr_init = (Boolean[]) timedData[i].value;
                    Boolean[] bool_ptr_read = null;
                    Boolean[] bool_ptr_write = null;
                    if (bool_ptr_init != null) {
                        if (dataFormat == AttrDataFormat._IMAGE) {
                            bool_ptr_read = new Boolean[timedData[i].x * timedData[i].y];
                            dimWrite = bool_ptr_init.length - timedData[i].x * timedData[i].y;
                        } else {
                            bool_ptr_read = new Boolean[timedData[i].x];
                            dimWrite = bool_ptr_init.length - timedData[i].x;
                            if (dimWrite > dbData_w.getMax_x()) {
                                dbData_w.setMax_x(dimWrite);
                            }
                        }
                        if (dimWrite < 0) {
                            dimWrite = 0;
                        }
                        bool_ptr_write = new Boolean[dimWrite];
                        if (dataFormat == AttrDataFormat._IMAGE) {
                            for (int j = 0; j < timedData[i].x * timedData[i].y; j++) {
                                bool_ptr_read[j] = bool_ptr_init[j];
                            }
                            for (int j = timedData[i].x * timedData[i].y; j < bool_ptr_init.length; j++) {
                                bool_ptr_write[j - timedData[i].x] = bool_ptr_init[j];
                            }
                        } else {
                            for (int j = 0; j < timedData[i].x; j++) {
                                bool_ptr_read[j] = bool_ptr_init[j];
                            }
                            for (int j = timedData[i].x; j < bool_ptr_init.length; j++) {
                                bool_ptr_write[j - timedData[i].x] = bool_ptr_init[j];
                            }
                        }
                    }
                    timedAttrData_r[i] = new NullableTimedData();
                    timedAttrData_r[i].data_type = data_type;
                    timedAttrData_r[i].x = timedData[i].x;
                    timedAttrData_r[i].y = timedData[i].y;
                    timedAttrData_r[i].time = timedData[i].time;
                    timedAttrData_w[i] = new NullableTimedData();
                    timedAttrData_w[i].data_type = data_type;
                    timedAttrData_w[i].x = timedData[i].x;
                    timedAttrData_w[i].y = timedData[i].y;
                    timedAttrData_w[i].time = timedData[i].time;
                    if (writable == AttrWriteType._WRITE) {
                        timedAttrData_r[i].value = bool_ptr_write;
                        timedAttrData_w[i].value = bool_ptr_read;
                    } else {
                        timedAttrData_r[i].value = bool_ptr_read;
                        timedAttrData_w[i].value = bool_ptr_write;
                    }
                    break;
                case TangoConst.Tango_DEV_STRING:
                    final String[] str_ptr_init = (String[]) timedData[i].value;
                    String[] str_ptr_read = null;
                    String[] str_ptr_write = null;
                    if (str_ptr_init != null) {
                        if (dataFormat == AttrDataFormat._IMAGE) {
                            str_ptr_read = new String[timedData[i].x * timedData[i].y];
                            dimWrite = str_ptr_init.length - timedData[i].x * timedData[i].y;
                        } else {
                            str_ptr_read = new String[timedData[i].x];
                            dimWrite = str_ptr_init.length - timedData[i].x;
                            if (dimWrite > dbData_w.getMax_x()) {
                                dbData_w.setMax_x(dimWrite);
                            }
                        }
                        if (dimWrite < 0) {
                            dimWrite = 0;
                        }
                        str_ptr_write = new String[dimWrite];
                        if (dataFormat == AttrDataFormat._IMAGE) {
                            for (int j = 0; j < timedData[i].x * timedData[i].y; j++) {
                                str_ptr_read[j] = str_ptr_init[j];
                            }
                            for (int j = timedData[i].x * timedData[i].y; j < str_ptr_init.length; j++) {
                                str_ptr_write[j - timedData[i].x] = str_ptr_init[j];
                            }
                        } else {
                            for (int j = 0; j < timedData[i].x; j++) {
                                if (j < str_ptr_init.length) {
                                    str_ptr_read[j] = str_ptr_init[j];
                                } else {
                                    str_ptr_read[j] = "";
                                }
                            }
                            for (int j = str_ptr_read.length; j < str_ptr_init.length; j++) {
                                str_ptr_write[j - str_ptr_read.length] = str_ptr_init[j];
                            }
                        }
                    }
                    timedAttrData_r[i] = new NullableTimedData();
                    timedAttrData_r[i].data_type = data_type;
                    timedAttrData_r[i].x = timedData[i].x;
                    timedAttrData_r[i].y = timedData[i].y;
                    timedAttrData_r[i].time = timedData[i].time;
                    timedAttrData_w[i] = new NullableTimedData();
                    timedAttrData_w[i].data_type = data_type;
                    timedAttrData_w[i].x = timedData[i].x;
                    timedAttrData_w[i].y = timedData[i].y;
                    timedAttrData_w[i].time = timedData[i].time;
                    if (writable == AttrWriteType._WRITE) {
                        timedAttrData_r[i].value = str_ptr_write;
                        timedAttrData_w[i].value = str_ptr_read;
                    } else {
                        timedAttrData_r[i].value = str_ptr_read;
                        timedAttrData_w[i].value = str_ptr_write;
                    }
                    break;
            }
        }
        dbData_r.setData_timed(timedAttrData_r);
        dbData_w.setData_timed(timedAttrData_w);
        switch(writable) {
            case AttrWriteType._READ:
                argout[0] = dbData_r;
                argout[1] = null;
                break;
            case AttrWriteType._WRITE:
                argout[0] = null;
                argout[1] = dbData_w;
                break;
            default:
                argout[0] = dbData_r;
                argout[1] = dbData_w;
        }
        return argout;
    }

    public TimedAttrData[] getDataAsTimedAttrData() {
        TimedAttrData[] attrData = null;
        if (timedData == null) {
            return null;
        } else {
            attrData = new TimedAttrData[timedData.length];
        }
        for (int i = 0; i < timedData.length; i++) {
            if (timedData[i] == null) {
                attrData[i] = null;
            } else {
                int sec = 0;
                if (timedData[i].time != null) {
                    sec = (int) (timedData[i].time.longValue() / 1000);
                }
                TimeVal timeVal = new TimeVal(sec, 0, 0);
                switch(data_type) {
                    case TangoConst.Tango_DEV_BOOLEAN:
                        boolean[] boolval;
                        if (timedData[i].value == null) {
                            boolval = new boolean[0];
                        } else {
                            boolval = new boolean[timedData[i].value.length];
                            for (int j = 0; j < timedData[i].value.length; j++) {
                                boolean value = false;
                                if (timedData[i].value[j] != null) {
                                    value = ((Boolean) timedData[i].value[j]).booleanValue();
                                }
                                boolval[j] = value;
                            }
                        }
                        attrData[i] = new TimedAttrData(boolval, timeVal);
                        break;
                    case TangoConst.Tango_DEV_SHORT:
                    case TangoConst.Tango_DEV_USHORT:
                    case TangoConst.Tango_DEV_UCHAR:
                        short[] sval;
                        if (timedData[i].value == null) {
                            sval = new short[0];
                        } else {
                            sval = new short[timedData[i].value.length];
                            for (int j = 0; j < timedData[i].value.length; j++) {
                                short value = 0;
                                if (timedData[i].value[j] != null) {
                                    value = ((Short) timedData[i].value[j]).shortValue();
                                }
                                sval[j] = value;
                            }
                        }
                        attrData[i] = new TimedAttrData(sval, timeVal);
                        break;
                    case TangoConst.Tango_DEV_LONG:
                    case TangoConst.Tango_DEV_ULONG:
                        int[] ival;
                        if (timedData[i].value == null) {
                            ival = new int[0];
                        } else {
                            ival = new int[timedData[i].value.length];
                            for (int j = 0; j < timedData[i].value.length; j++) {
                                int value = 0;
                                if (timedData[i].value[j] != null) {
                                    value = ((Integer) timedData[i].value[j]).intValue();
                                }
                                ival[j] = value;
                            }
                        }
                        attrData[i] = new TimedAttrData(ival, timeVal);
                        break;
                    case TangoConst.Tango_DEV_LONG64:
                    case TangoConst.Tango_DEV_ULONG64:
                        long[] lval;
                        if (timedData[i].value == null) {
                            lval = new long[0];
                        } else {
                            lval = new long[timedData[i].value.length];
                            for (int j = 0; j < timedData[i].value.length; j++) {
                                int value = 0;
                                if (timedData[i].value[j] != null) {
                                    value = ((Integer) timedData[i].value[j]).intValue();
                                }
                                lval[j] = value;
                            }
                        }
                        attrData[i] = new TimedAttrData(lval, timeVal);
                        break;
                    case TangoConst.Tango_DEV_FLOAT:
                        float[] fval;
                        if (timedData[i].value == null) {
                            fval = new float[0];
                        } else {
                            fval = new float[timedData[i].value.length];
                            for (int j = 0; j < timedData[i].value.length; j++) {
                                float value = 0;
                                if (timedData[i].value[j] != null) {
                                    value = ((Float) timedData[i].value[j]).floatValue();
                                }
                                fval[j] = value;
                            }
                        }
                        attrData[i] = new TimedAttrData(fval, timeVal);
                        break;
                    case TangoConst.Tango_DEV_DOUBLE:
                        double[] dval;
                        if (timedData[i].value == null) {
                            dval = new double[0];
                        } else {
                            dval = new double[timedData[i].value.length];
                            for (int j = 0; j < timedData[i].value.length; j++) {
                                double value = Double.NaN;
                                if (timedData[i].value[j] != null) {
                                    value = ((Double) timedData[i].value[j]).doubleValue();
                                }
                                dval[j] = value;
                            }
                        }
                        attrData[i] = new TimedAttrData(dval, timeVal);
                        break;
                    case TangoConst.Tango_DEV_STRING:
                        String[] strval;
                        if (timedData[i].value == null) {
                            strval = new String[0];
                        } else {
                            strval = new String[timedData[i].value.length];
                            for (int j = 0; j < timedData[i].value.length; j++) {
                                String value = "";
                                if (timedData[i].value[j] != null) {
                                    value = new String((String) timedData[i].value[j]);
                                }
                                strval[j] = value;
                            }
                        }
                        attrData[i] = new TimedAttrData(strval, timeVal);
                        strval = null;
                        break;
                    case TangoConst.Tango_DEV_STATE:
                        String[] strstate;
                        if (timedData[i].value == null) {
                            strstate = new String[0];
                        } else {
                            strstate = new String[timedData[i].value.length];
                            for (int j = 0; j < timedData[i].value.length; j++) {
                                String value = "";
                                if (timedData[i].value[j] != null) {
                                    value = TangoConst.Tango_DevStateName[(Integer) timedData[i].value[j]];
                                }
                                strstate[j] = value;
                            }
                        }
                        attrData[i] = new TimedAttrData(strstate, timeVal);
                        strstate = null;
                        break;
                    default:
                        attrData[i] = new TimedAttrData(new DevError[0], timeVal);
                }
                attrData[i].qual = timedData[i].qual;
                attrData[i].x = timedData[i].x;
                attrData[i].y = timedData[i].y;
                attrData[i].data_type = data_type;
                timeVal = null;
            }
        }
        return attrData;
    }

    @Override
    public String toString() {
        final ReflectionToStringBuilder reflectionToStringBuilder = new ReflectionToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
        return reflectionToStringBuilder.toString();
    }
}
