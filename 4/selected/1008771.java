package org.javaseis.util.access;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class JavaSeisWrapper {

    public static final int NORMAL = 0;

    public static final int ERROR = -1;

    protected int _status = NORMAL;

    protected String _error_message = "";

    protected JavaSeisIO _js;

    protected JSIO _jsw;

    protected ArrayList<String> _method_list = new ArrayList<String>();

    protected AxisData _ad = new AxisData(1);

    protected BinData _bd = new BinData();

    protected String _path;

    protected String _rw_mode;

    protected String _property_name;

    protected String _property_description;

    protected String _axis_domain;

    protected String _axis_units;

    protected String _data_type;

    protected String _trace_format;

    protected static final int GET_TRACE_NUMBER = 0;

    protected static final int SET_TRACE_NUMBER = 1;

    protected static final int GET_TRACE = 2;

    protected static final int SET_TRACE = 3;

    protected static final int STATUS = 4;

    protected static final int IS_A_JAVASEIS_PATH = 5;

    protected static final int IS_READ_WRITE = 6;

    protected static final int PROPERTY_NAME = 7;

    protected static final int PROPERTY_DESCRIPTION = 8;

    protected static final int ADD_INT_PROPERTY = 9;

    protected static final int ADD_FLOAT_PROPERTY = 10;

    protected static final int ADD_DOUBLE_PROPERTY = 11;

    protected static final int ADD_LONG_PROPERTY = 12;

    protected static final int GET_INT_PROPERTY = 13;

    protected static final int GET_FLOAT_PROPERTY = 14;

    protected static final int GET_DOUBLE_PROPERTY = 15;

    protected static final int GET_LONG_PROPERTY = 16;

    protected static final int SET_INT_PROPERTY = 17;

    protected static final int SET_FLOAT_PROPERTY = 18;

    protected static final int SET_DOUBLE_PROPERTY = 19;

    protected static final int SET_LONG_PROPERTY = 20;

    protected static final int SAMPLE_COUNT = 21;

    protected static final int TRACE_COUNT = 22;

    protected static final int DIMENSION_COUNT = 23;

    protected static final int ERROR_MESSAGE = 24;

    protected static final int AXIS_DIMENSION = 25;

    protected static final int GET_AXIS = 26;

    protected static final int GET_AXIS_LENGTH = 27;

    protected static final int GET_AXIS_DOMAIN = 28;

    protected static final int GET_AXIS_UNITS = 29;

    protected static final int GET_AXIS_LOGICALS = 30;

    protected static final int GET_AXIS_PHYSICALS = 31;

    protected static final int SET_AXIS_LENGTH = 32;

    protected static final int SET_AXIS_DOMAIN = 33;

    protected static final int SET_AXIS_UNITS = 34;

    protected static final int SET_AXIS_LOGICALS = 35;

    protected static final int SET_AXIS_PHYSICALS = 36;

    protected static final int SET_AXIS = 37;

    protected static final int GET_DATA_TYPE = 38;

    protected static final int SET_DATA_TYPE = 39;

    protected static final int GET_TRACE_FORMAT = 40;

    protected static final int SET_TRACE_FORMAT = 41;

    protected static final int GET_BIN = 42;

    protected static final int GET_BIN_WORLD = 43;

    protected static final int GET_BIN_GRID = 44;

    protected static final int GET_BIN_LOGICAL = 45;

    protected static final int SET_BIN_WORLD = 46;

    protected static final int SET_BIN_GRID = 47;

    protected static final int SET_BIN_LOGICAL = 48;

    protected static final int SET_BIN = 49;

    protected static final int PATH = 50;

    protected static final int RW_MODE = 51;

    protected static final int INIT = 52;

    protected static final int OPEN = 53;

    protected static final int CLOSE = 54;

    protected static final int DELETE = 55;

    protected static final int NUM_METHODS = 56;

    public JavaSeisWrapper() {
        _method_list.add(GET_TRACE_NUMBER, "getTraceNumber");
        _method_list.add(SET_TRACE_NUMBER, "setTraceNumber");
        _method_list.add(GET_TRACE, "getTrace");
        _method_list.add(SET_TRACE, "setTrace");
        _method_list.add(STATUS, "status");
        _method_list.add(IS_A_JAVASEIS_PATH, "isAJavaSeisPath");
        _method_list.add(IS_READ_WRITE, "isReadWrite");
        _method_list.add(PROPERTY_NAME, "propertyName");
        _method_list.add(PROPERTY_DESCRIPTION, "propertyDescription");
        _method_list.add(ADD_INT_PROPERTY, "addIntProperty");
        _method_list.add(ADD_FLOAT_PROPERTY, "addFloatProperty");
        _method_list.add(ADD_DOUBLE_PROPERTY, "addDoubleProperty");
        _method_list.add(ADD_LONG_PROPERTY, "addLongProperty");
        _method_list.add(GET_INT_PROPERTY, "getIntProperty");
        _method_list.add(GET_FLOAT_PROPERTY, "getFloatProperty");
        _method_list.add(GET_DOUBLE_PROPERTY, "getDoubleProperty");
        _method_list.add(GET_LONG_PROPERTY, "getLongProperty");
        _method_list.add(SET_INT_PROPERTY, "setIntProperty");
        _method_list.add(SET_FLOAT_PROPERTY, "setFloatProperty");
        _method_list.add(SET_DOUBLE_PROPERTY, "setDoubleProperty");
        _method_list.add(SET_LONG_PROPERTY, "setLongProperty");
        _method_list.add(SAMPLE_COUNT, "sampleCount");
        _method_list.add(TRACE_COUNT, "traceCount");
        _method_list.add(DIMENSION_COUNT, "dimensionCount");
        _method_list.add(ERROR_MESSAGE, "errorMessage");
        _method_list.add(AXIS_DIMENSION, "axisDimension");
        _method_list.add(GET_AXIS, "getAxis");
        _method_list.add(GET_AXIS_LENGTH, "getAxisLength");
        _method_list.add(GET_AXIS_DOMAIN, "getAxisDomain");
        _method_list.add(GET_AXIS_UNITS, "getAxisUnits");
        _method_list.add(GET_AXIS_LOGICALS, "getAxisLogicals");
        _method_list.add(GET_AXIS_PHYSICALS, "getAxisPhysicals");
        _method_list.add(SET_AXIS_LENGTH, "setAxisLength");
        _method_list.add(SET_AXIS_DOMAIN, "setAxisDomain");
        _method_list.add(SET_AXIS_UNITS, "setAxisUnits");
        _method_list.add(SET_AXIS_LOGICALS, "setAxisLogicals");
        _method_list.add(SET_AXIS_PHYSICALS, "setAxisPhysicals");
        _method_list.add(SET_AXIS, "setAxis");
        _method_list.add(GET_DATA_TYPE, "getDataType");
        _method_list.add(SET_DATA_TYPE, "setDataType");
        _method_list.add(GET_TRACE_FORMAT, "getTraceFormat");
        _method_list.add(SET_TRACE_FORMAT, "setTraceFormat");
        _method_list.add(GET_BIN, "getBin");
        _method_list.add(GET_BIN_WORLD, "getBinWorld");
        _method_list.add(GET_BIN_GRID, "getBinGrid");
        _method_list.add(GET_BIN_LOGICAL, "getBinLogical");
        _method_list.add(SET_BIN_WORLD, "setBinWorld");
        _method_list.add(SET_BIN_GRID, "setBinGrid");
        _method_list.add(SET_BIN_LOGICAL, "setBinLogical");
        _method_list.add(SET_BIN, "setBin");
        _method_list.add(PATH, "path");
        _method_list.add(RW_MODE, "rwMode");
        _method_list.add(INIT, "init");
        _method_list.add(OPEN, "open");
        _method_list.add(CLOSE, "close");
        _method_list.add(DELETE, "delete");
    }

    public int none(String description) {
        int retval = verifyStatus();
        if (retval < 1) return retval;
        try {
            switch(_method_list.indexOf(description)) {
                case ADD_INT_PROPERTY:
                    retval = addIntProperty();
                    break;
                case ADD_FLOAT_PROPERTY:
                    retval = addFloatProperty();
                    break;
                case ADD_DOUBLE_PROPERTY:
                    retval = addDoubleProperty();
                    break;
                case ADD_LONG_PROPERTY:
                    retval = addLongProperty();
                    break;
                case OPEN:
                    retval = open();
                    break;
                case CLOSE:
                    close();
                    break;
                case GET_AXIS:
                    retval = getAxis();
                    break;
                case SET_AXIS:
                    retval = setAxis();
                    break;
                case GET_BIN:
                    retval = getBin();
                    break;
                case SET_BIN:
                    retval = setBin();
                    break;
                case INIT:
                    retval = init();
                    break;
                case DELETE:
                    retval = delete();
                    break;
                default:
                    handleError("JavaSeisWrapper.noargs: Unrecognized request: " + description, null);
                    retval = verifyStatus();
            }
        } catch (Exception e) {
            handleError("JavaSeisWrapper.noargs: Failed: " + description, e);
            retval = verifyStatus();
        }
        return retval;
    }

    public int bytes(String description, byte[] byts) {
        int retval = verifyStatus();
        if (retval < 1) return retval;
        try {
            switch(_method_list.indexOf(description)) {
                case PROPERTY_NAME:
                    retval = setPropertyName(new String(byts));
                    break;
                case PROPERTY_DESCRIPTION:
                    retval = setPropertyDescription(new String(byts));
                    break;
                case ERROR_MESSAGE:
                    retval = getBytes(getErrorMessage(), byts);
                    break;
                case GET_AXIS_UNITS:
                    retval = getBytes(getAxisUnits(), byts);
                    break;
                case SET_AXIS_UNITS:
                    retval = setAxisUnits(new String(byts));
                    break;
                case GET_AXIS_DOMAIN:
                    retval = getBytes(getAxisDomain(), byts);
                    break;
                case SET_AXIS_DOMAIN:
                    retval = setAxisDomain(new String(byts));
                    break;
                case GET_DATA_TYPE:
                    retval = getBytes(getDataType(), byts);
                    break;
                case SET_DATA_TYPE:
                    retval = setDataType(new String(byts));
                    break;
                case GET_TRACE_FORMAT:
                    retval = getBytes(getTraceFormat(), byts);
                    break;
                case SET_TRACE_FORMAT:
                    retval = setTraceFormat(new String(byts));
                    break;
                case PATH:
                    retval = setPath(new String(byts));
                    break;
                case RW_MODE:
                    retval = setRWMode(new String(byts));
                    break;
                default:
                    handleError("JavaSeisWrapper.bytes: Unrecognized request: " + description, null);
                    retval = verifyStatus();
            }
        } catch (Exception e) {
            handleError("JavaSeisWrapper.bytes: Failed: " + description, e);
            retval = verifyStatus();
        }
        return retval;
    }

    public int floats(String description, float[] flts) {
        int retval = verifyStatus();
        if (retval < 1) return retval;
        try {
            switch(_method_list.indexOf(description)) {
                case GET_TRACE:
                    retval = getTrace(flts);
                    break;
                case SET_TRACE:
                    retval = setTrace(flts);
                    break;
                case GET_FLOAT_PROPERTY:
                    flts[0] = getFloatProperty();
                    retval = verifyStatus();
                    break;
                case SET_FLOAT_PROPERTY:
                    retval = setFloatProperty(flts[0]);
                    break;
                default:
                    handleError("JavaSeisWrapper.floats: Unrecognized request: " + description, null);
                    retval = verifyStatus();
            }
        } catch (Exception e) {
            handleError("JavaSeisWrapper.floats: Failed: " + description, e);
            retval = verifyStatus();
        }
        return retval;
    }

    public int doubles(String description, double[] dbls) {
        int retval = verifyStatus();
        if (retval < 1) return retval;
        try {
            switch(_method_list.indexOf(description)) {
                case GET_DOUBLE_PROPERTY:
                    dbls[0] = getDoubleProperty();
                    break;
                case SET_DOUBLE_PROPERTY:
                    retval = setDoubleProperty(dbls[0]);
                    break;
                case GET_AXIS_PHYSICALS:
                    retval = getAxisPhysicals(dbls);
                    break;
                case SET_AXIS_PHYSICALS:
                    retval = setAxisPhysicals(dbls);
                    break;
                case GET_BIN_WORLD:
                    retval = getBinWorld(dbls);
                    break;
                case SET_BIN_WORLD:
                    retval = setBinWorld(dbls);
                    break;
                case GET_BIN_GRID:
                    retval = getBinGrid(dbls);
                    break;
                case SET_BIN_GRID:
                    retval = setBinGrid(dbls);
                    break;
                default:
                    handleError("JavaSeisWrapper.doubles: Unrecognized request: " + description, null);
                    retval = verifyStatus();
            }
        } catch (Exception e) {
            handleError("JavaSeisWrapper.doubles: Failed: " + description, e);
            retval = verifyStatus();
        }
        return retval;
    }

    public int ints(String description, int[] ints) {
        int retval = verifyStatus();
        if (retval < 1) return retval;
        try {
            switch(_method_list.indexOf(description)) {
                case STATUS:
                    ints[0] = getStatus();
                    retval = verifyStatus();
                    break;
                case IS_A_JAVASEIS_PATH:
                    ints[0] = isAJavaSeisPath();
                    retval = verifyStatus();
                    break;
                case IS_READ_WRITE:
                    ints[0] = isReadWrite();
                    retval = verifyStatus();
                    break;
                case GET_INT_PROPERTY:
                    ints[0] = getIntProperty();
                    retval = verifyStatus();
                    break;
                case SET_INT_PROPERTY:
                    retval = setIntProperty(ints[0]);
                    break;
                case SAMPLE_COUNT:
                    ints[0] = getSampleCount();
                    retval = verifyStatus();
                    break;
                case DIMENSION_COUNT:
                    ints[0] = getNumDimensions();
                    retval = verifyStatus();
                    break;
                case AXIS_DIMENSION:
                    retval = setAxisDimension(ints[0]);
                    break;
                case GET_AXIS_LENGTH:
                    ints[0] = getAxisLength();
                    retval = verifyStatus();
                    break;
                case SET_AXIS_LENGTH:
                    retval = setAxisLength(ints[0]);
                    break;
                default:
                    handleError("JavaSeisWrapper.ints: Unrecognized request: " + description, null);
                    retval = verifyStatus();
            }
        } catch (Exception e) {
            handleError("JavaSeisWrapper.ints: Failed: " + description, e);
            retval = verifyStatus();
        }
        return retval;
    }

    public int longs(String description, long[] lngs) {
        int retval = verifyStatus();
        if (retval < 1) return retval;
        try {
            switch(_method_list.indexOf(description)) {
                case GET_TRACE_NUMBER:
                    lngs[0] = getTraceNumber();
                    break;
                case SET_TRACE_NUMBER:
                    retval = setTraceNumber(lngs[0]);
                    break;
                case GET_LONG_PROPERTY:
                    lngs[0] = getLongProperty();
                    break;
                case SET_LONG_PROPERTY:
                    retval = setLongProperty(lngs[0]);
                    break;
                case TRACE_COUNT:
                    lngs[0] = getTraceCount();
                    break;
                case GET_AXIS_LOGICALS:
                    retval = getAxisLogicals(lngs);
                    break;
                case SET_AXIS_LOGICALS:
                    retval = setAxisLogicals(lngs);
                    break;
                case GET_BIN_LOGICAL:
                    retval = getBinLogical(lngs);
                    break;
                case SET_BIN_LOGICAL:
                    retval = setBinLogical(lngs);
                    break;
                default:
                    handleError("JavaSeisWrapper.longs: Unrecognized request: " + description, null);
                    retval = verifyStatus();
            }
        } catch (Exception e) {
            handleError("JavaSeisWrapper.longs: Failed: " + description, e);
            retval = verifyStatus();
        }
        return retval;
    }

    public int setPropertyName(String property_name) {
        _property_name = property_name;
        return verifyString(_property_name);
    }

    public int setPropertyDescription(String property_description) {
        _property_description = property_description;
        return verifyString(_property_description);
    }

    public String getAxisUnits() {
        String retval;
        if (!_ad._just_got_data) {
            _status = ERROR;
            retval = null;
        } else if (_status == NORMAL) {
            retval = _ad._units[0];
        } else {
            retval = null;
        }
        return retval;
    }

    /**
   * setAxisUnits
   * 
   * @param units
   *   choices include "seconds", "feet", "meters",
   *   "hertz", "null", and "unknown"
   *
   * @return
   *    1 if successful
   */
    public int setAxisUnits(String axis_units) {
        try {
            if (_status == NORMAL && !_ad._units[0].equalsIgnoreCase(axis_units)) {
                _ad._units[0] = axis_units;
                _ad._just_got_data = false;
            }
        } catch (Exception e) {
            handleError("JavaSeisWrapper.setAxisUnits: ", e);
        }
        return verifyStatus();
    }

    public String getAxisDomain() {
        String retval;
        if (!_ad._just_got_data) {
            _status = ERROR;
            retval = null;
        } else if (_status == NORMAL) {
            retval = _ad._domain[0];
        } else {
            retval = null;
        }
        return retval;
    }

    /**
   * setAxisDomain
   * 
   * @param domain
   *   choices include "time", "depth", "frequency",
   *   "wavenumber", "semblance", "velocity", and "slowness"
   *   
   * @return
   *    1 if successful
   */
    public int setAxisDomain(String axis_domain) {
        try {
            if (_status == NORMAL && !_ad._domain[0].equalsIgnoreCase(axis_domain)) {
                _ad._domain[0] = axis_domain;
                _ad._just_got_data = false;
            }
        } catch (Exception e) {
            handleError("JavaSeisWrapper.setAxisDomain: ", e);
        }
        return verifyStatus();
    }

    public String getDataType() {
        String retval;
        try {
            retval = _js.getDataType();
        } catch (Exception e) {
            handleError("JavaSeisWrapper.getDataType: ", e);
            retval = null;
        }
        return retval;
    }

    /**
   * set the data type
   * 
   * @param data_type_name
   *    valid choices include "STACK", "CMP", "OFFSET_BIN",
   *    "RECEIVER", "SOURCE", and "CUSTOM"
   * @return
   *    1 if successful
   */
    public int setDataType(String data_type_name) {
        try {
            if (!_jsw.setDataType(data_type_name)) {
                handleError("JavaSeisWrapper.setDataType: Name " + data_type_name, null);
            }
        } catch (Exception e) {
            handleError("JavaSeisWrapper.setDataType: Name " + data_type_name, e);
        }
        return verifyStatus();
    }

    public String getTraceFormat() {
        String retval;
        try {
            retval = _js.getTraceFormat();
        } catch (Exception e) {
            handleError("JavaSeisWrapper.getTraceFormat: ", e);
            retval = null;
        }
        return retval;
    }

    /**
   * set the trace format
   *  
   * @param trace_format
   *    valid choices include "FLOAT", "INT16", "INT08",
   *    "COMPRESSED_INT16, and "COMPRESSED_INT08"
   * @return
   *    1 if successful
   */
    public int setTraceFormat(String format_name) {
        try {
            if (!_jsw.setTraceFormat(format_name)) {
                handleError("JavaSeisWrapper.setTraceFormat: Name " + format_name, null);
            }
        } catch (Exception e) {
            _status = ERROR;
            handleError("JavaSeisWrapper.setTraceFormat: Name " + format_name, e);
        }
        return verifyStatus();
    }

    public int setPath(String path) {
        _path = path;
        return verifyString(_path);
    }

    public int setRWMode(String rw_mode) {
        _rw_mode = rw_mode;
        return verifyString(_rw_mode);
    }

    /**
   * Populates a preallocated float array with trace values from the JavaSeis path
   *   based on the previous call to setTraceNumber.
   * 
   * @param trace
   *    given float array preallocated for the trace size
   * @return
   *    length of populated trace array in values.
   */
    public int getTrace(float[] trace) {
        if (_status != NORMAL) {
            return 0;
        } else {
            return _js.getTrace(trace);
        }
    }

    /**
   * Writes a float array with trace values into the JavaSeisFile
   *   based on the previous call to setTraceNumber.
   * 
   * @param trace
   *    given float trace array to write
   * @return
   *    number of trace array values written.
   */
    public int setTrace(float[] trace) {
        int retval;
        if (_status != NORMAL) {
            retval = 0;
        } else {
            try {
                int count = _jsw.putTrace(trace);
                retval = Math.min(trace.length, count);
            } catch (Exception e) {
                handleError("JavaSeisWrapper.setTrace: ", e);
                retval = 0;
            }
        }
        return retval;
    }

    public int getAxisPhysicals(double[] dbls) {
        if (!_ad._just_got_data) {
            _status = ERROR;
        } else if (_status == NORMAL) {
            try {
                dbls[0] = _ad._physical_origin[0];
                dbls[1] = _ad._physical_delta[0];
            } catch (Exception e) {
                handleError("JavaSeisWrapper.getAxisPhysicals: ", e);
            }
        }
        return verifyStatus();
    }

    /**
   * setAxisPhysicals
   * 
   * @param dbls
   *   dbls[0] = physical_origin for example the time of the
   *             starting sample
   *   dbls[1] = physical_delta for example the sample rate
   *   
   * @return
   *    1 if successful
   */
    public int setAxisPhysicals(double[] dbls) {
        if (_status == NORMAL) {
            try {
                if (_ad._physical_origin[0] != dbls[0] || _ad._physical_delta[0] != dbls[1]) {
                    _ad._physical_origin[0] = dbls[0];
                    _ad._physical_delta[0] = dbls[1];
                    _ad._just_got_data = false;
                }
            } catch (Exception e) {
                handleError("JavaSeisWrapper.setAxisPhysicals: ", e);
            }
        }
        return verifyStatus();
    }

    /**
   * setAxisDimension
   * 
   * @param dimension
   *   dimension for which subsequent axis parameters apply.
   *   0 is fastest changing axis (samples). the highest dimension
   *   number is the slowest changing axis (hypercube).
   */
    public int setAxisDimension(int dimension) {
        if (_status == NORMAL && _ad._dimension != dimension) {
            _ad._dimension = dimension;
            _ad._just_got_data = false;
        }
        return verifyStatus();
    }

    public int getAxis() {
        if (_status == NORMAL) {
            if (!_js.getAxis(_ad._dimension, _ad._length, _ad._domain, _ad._units, _ad._logical_origin, _ad._logical_delta, _ad._physical_origin, _ad._physical_delta)) {
                handleError("JavaSeisWrapper.getAxis: " + _js.errorMessage(), null);
            }
            _ad._just_got_data = _status == NORMAL;
        }
        return verifyStatus();
    }

    public int getAxisLength() {
        int retval;
        if (!_ad._just_got_data) {
            _status = ERROR;
            retval = 0;
        } else if (_status == NORMAL) {
            retval = _ad._length[0];
        } else {
            retval = 0;
        }
        return retval;
    }

    /**
   * setAxisLength
   * 
   * @param length
   *   length of current axis
   *   
   * @return
   *    1 if successful
   */
    public int setAxisLength(int length) {
        if (_status == NORMAL && _ad._length[0] != length) {
            _ad._length[0] = length;
            _ad._just_got_data = false;
        }
        return verifyStatus();
    }

    public int setAxis() {
        if (_status == NORMAL) {
            if (!_jsw.setAxis(_ad._dimension, _ad._length[0], _ad._domain[0], _ad._units[0], _ad._logical_origin[0], _ad._logical_delta[0], _ad._physical_origin[0], _ad._physical_delta[0])) {
                handleError("JavaSeisWrapper.setAxis: " + _jsw.errorMessage(), null);
            }
        }
        return verifyStatus();
    }

    public int getAxisLogicals(long[] lngs) {
        if (!_ad._just_got_data) {
            _status = ERROR;
        } else if (_status == NORMAL) {
            try {
                lngs[0] = _ad._logical_origin[0];
                lngs[1] = _ad._logical_delta[0];
            } catch (Exception e) {
                handleError("JavaSeisWrapper.getAxisLogicals: ", e);
            }
        }
        return verifyStatus();
    }

    /**
   * setAxisLogicals
   * 
   * @param lngs
   *   lngs[0] = logical_origin for example the crossline
   *             number of the first trace
   *   lngs[1] = logical_delta for example the crossline
   *             number difference between adjacent traces
   *
   * @return
   *    1 if successful
   */
    public int setAxisLogicals(long[] lngs) {
        if (_status == NORMAL) {
            try {
                if (_ad._logical_origin[0] != lngs[0] || _ad._logical_delta[0] != lngs[1]) {
                    _ad._logical_origin[0] = lngs[0];
                    _ad._logical_delta[0] = lngs[1];
                    _ad._just_got_data = false;
                }
            } catch (Exception e) {
                handleError("JavaSeisWrapper.setAxisLogicals: ", e);
            }
        }
        return verifyStatus();
    }

    public int init() {
        boolean read_write;
        if (_rw_mode.equalsIgnoreCase("rw") || _rw_mode.equalsIgnoreCase("w")) {
            read_write = true;
        } else {
            read_write = false;
        }
        if (!read_write) {
            JavaSeisReader jsr = new JavaSeisReader(_path);
            if (!jsr.preExisted()) {
                _status = ERROR;
                _error_message = "JavaSeisWrapper.init: file not found " + _path;
            } else {
                if (!jsr.open() || jsr.status() == JavaSeisIO.ERROR) {
                    _status = ERROR;
                    _error_message = "JavaSeisWrapper.init: " + jsr.errorMessage();
                }
                _js = jsr;
            }
        } else {
            _jsw = (JSIO) (new JavaSeisWriter(_path));
            if (_jsw.preExisted()) {
                if (!_jsw.open() || _jsw.status() == JavaSeisIO.ERROR) {
                    _status = ERROR;
                    _error_message = "JavaSeisWrapper.init: " + _jsw.errorMessage();
                }
            }
            _js = (JavaSeisIO) _jsw;
        }
        if (_js == null) {
            _status = ERROR;
            _error_message = "JavaSeisWrapper.init: Failed";
        }
        return verifyStatus();
    }

    /**
   * Returns the status of the object. The result is NORMAl if the given file
   *   in the constructor is a valid JavaSeisFile, otherwise ERROR is returned.
   *   
   * @return
   *   status flag.
   */
    public int getStatus() {
        return _status;
    }

    /**
   * Returns a Java String of the error message when the status code is ERROR. If
   *   the status code is NORMAL, an empty String is returned;
   * 
   * @return
   *    error code.
   */
    public String getErrorMessage() {
        return _error_message;
    }

    /**
   * Returns a 1 if the file name string given for the path is the
   *   name of a valid JavaSeis directory, otherwise return 0. This function
   *   uses the result of an instance of the Java class: JavaSeisValidator which
   *   was temporarily used in the constructor.
   *   
   * @return
   *    1 if file is valid, otherwise 0.
   */
    public int isAJavaSeisPath() {
        if (_status == NORMAL) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
   * Returns a 1 if the file name string given as the path is
   *   writable.
   *      
   * @return
   *    1 if file is writable, otherwise 0.
   */
    public int isReadWrite() {
        if (_status == NORMAL) {
            return _js.isReadWrite() ? 1 : 0;
        } else {
            return 0;
        }
    }

    /**
   * Return the number of traces in the JavaSeisFile.
   * 
   * @return
   *    number of traces.
   */
    public long getTraceCount() {
        if (_status != NORMAL) {
            return 0;
        } else {
            return _js.getTraceCount();
        }
    }

    public int getNumDimensions() {
        if (_status != NORMAL) {
            return 0;
        } else {
            return _js.getNumDimensions();
        }
    }

    /**
  Return the number of samples in each trace in the JavaSeisFile.

  @return         number of samples.
   */
    public int getSampleCount() {
        if (_status != NORMAL) {
            return 0;
        } else {
            return _js.getSampleCount();
        }
    }

    /**
   * Returns a float array with trace values from the JavaSeisFile
   *   based on the previous call to setTraceNumber.
   * 
   * @return
   *    the seismic trace.
   */
    public float[] getTrace() {
        if (_status != NORMAL) {
            return null;
        } else {
            return _js.getTrace();
        }
    }

    public int setTraceNumber(long trace_num) {
        int retval;
        if (_js.setIndex(trace_num)) {
            retval = 1;
        } else {
            retval = 0;
        }
        return retval;
    }

    public long getTraceNumber() {
        long retval;
        if (_status != NORMAL) {
            retval = 0;
        } else {
            retval = _js.getIndex();
        }
        return retval;
    }

    public int open() {
        int retval = 1;
        if (_status == NORMAL) {
            if (_js.isReadWrite()) {
                if (!_js.preExisted()) {
                    if (!_jsw.open()) {
                        retval = 0;
                    }
                }
            }
        } else {
            retval = 0;
        }
        return retval;
    }

    private class AxisData {

        private int _dimension;

        private int[] _length = new int[1];

        private String[] _domain = new String[1];

        private String[] _units = new String[1];

        private long[] _logical_origin = new long[1];

        private long[] _logical_delta = new long[1];

        private double[] _physical_origin = new double[1];

        private double[] _physical_delta = new double[1];

        private boolean _just_got_data;

        private AxisData(int dimension) {
            _dimension = dimension;
            _length[0] = 1;
            switch(_dimension) {
                case 1:
                    _domain[0] = "time";
                    _units[0] = "miliseconds";
                    break;
                case 2:
                case 3:
                    _domain[0] = "space";
                    _units[0] = "feet";
                    break;
                default:
                    _domain[0] = "offset";
                    _units[0] = "unitless";
            }
            _logical_origin[0] = 0;
            _logical_delta[0] = 1;
            _physical_origin[0] = 0;
            _physical_delta[0] = 1;
        }
    }

    /**
   * setBinWorld sets 6 values which describe the
   *   JavaSeis BinGrid world coordinates
   * @param world
   *   world contains 6 values:
   *     worldx0, worldy0,
   *     worldx1, worldy1,
   *     worldx2, worldy2
   * @return
   *   normal return is 1
   */
    public int setBinWorld(double[] world) {
        if (_status == NORMAL) {
            try {
                boolean changed;
                int k2;
                for (k2 = 0, changed = false; !changed && k2 < _bd._world.length; k2++) {
                    changed = _bd._world[k2] != world[k2];
                }
                if (changed) {
                    for (k2 = 0; k2 < _bd._world.length; k2++) {
                        _bd._world[k2] = world[k2];
                    }
                    _bd._just_got_data = false;
                }
            } catch (Exception e) {
                handleError("JavaSeisWrapper.setBinWorld: ", e);
            }
        }
        return verifyStatus();
    }

    /**
   * setBinGrid sets 4 values which describe the
   *   JavaSeis BinGrid grid coordinates
   * @param grid
   *   grid contains 4 values:
   *     gridx0, gridy0,
   *     griddx, griddy
   * @return
   *   normal return is 1
   */
    public int setBinGrid(double[] grid) {
        if (_status == NORMAL) {
            try {
                boolean changed;
                int k2;
                for (k2 = 0, changed = false; !changed && k2 < _bd._grid.length; k2++) {
                    changed = _bd._grid[k2] != grid[k2];
                }
                if (changed) {
                    for (k2 = 0; k2 < _bd._grid.length; k2++) {
                        _bd._grid[k2] = grid[k2];
                    }
                    _bd._just_got_data = false;
                }
            } catch (Exception e) {
                handleError("JavaSeisWrapper.setBinGrid: ", e);
            }
        }
        return verifyStatus();
    }

    /**
   * setBinLogical sets 8 values which describe the
   *   JavaSeis BinGrid logical coordinates
   * @param logical
   *   logical contains 8 values:
   *     logicalx0, logicaly0,
   *     logicalx1, logicaly1,
   *     logicalx2, logicaly2,
   *     logicaldx, logicaldy
   * @return
   *   normal return is 1
   */
    public int setBinLogical(long[] logical) {
        if (_status == NORMAL) {
            try {
                boolean changed;
                int k2;
                for (k2 = 0, changed = false; !changed && k2 < _bd._logical.length; k2++) {
                    changed = _bd._logical[k2] != logical[k2];
                }
                if (changed) {
                    for (k2 = 0; k2 < _bd._logical.length; k2++) {
                        _bd._logical[k2] = logical[k2];
                    }
                    _bd._just_got_data = false;
                }
            } catch (Exception e) {
                handleError("JavaSeisWrapper.setBinLogical: ", e);
            }
        }
        return verifyStatus();
    }

    public int setBin() {
        if (_status == NORMAL) {
            if (!_jsw.setBinGrid(_bd._world, _bd._grid, _bd._logical)) {
                handleError("JavaSeisWrapper.setBinGrid: " + _jsw.errorMessage(), null);
            }
        }
        return verifyStatus();
    }

    public int addIntProperty() {
        try {
            if (!_jsw.addIntProperty(_property_name, _property_description)) {
                handleError("JavaSeisWrapper.addIntProperty: Name " + _property_name, null);
            }
        } catch (Exception e) {
            handleError("JavaSeisWrapper.addIntProperty: Name " + _property_name + ":", e);
        }
        return verifyStatus();
    }

    public int addFloatProperty() {
        try {
            if (!_jsw.addFloatProperty(_property_name, _property_description)) {
                handleError("JavaSeisWrapper.addFloatProperty: Name " + _property_name, null);
            }
        } catch (Exception e) {
            handleError("JavaSeisWrapper.addFloatProperty: Name " + _property_name + ":", e);
        }
        return verifyStatus();
    }

    public int addDoubleProperty() {
        try {
            if (!_jsw.addDoubleProperty(_property_name, _property_description)) {
                handleError("JavaSeisWrapper.addDoubleProperty: Name " + _property_name, null);
            }
        } catch (Exception e) {
            handleError("JavaSeisWrapper.addDoubleProperty: Name " + _property_name + ": ", e);
        }
        return verifyStatus();
    }

    public int addLongProperty() {
        try {
            if (!_jsw.addLongProperty(_property_name, _property_description)) {
                handleError("JavaSeisWrapper.addLongProperty: Name " + _property_name, null);
            }
        } catch (Exception e) {
            handleError("JavaSeisWrapper.addLongProperty: Name " + _property_name + ":", e);
        }
        return verifyStatus();
    }

    public int setIntProperty(int value) {
        try {
            if (!_jsw.putIntPropertyValue(_property_name, value)) {
                handleError("JavaSeisWrapper.putIntPropertyValue: Name " + _property_name, null);
            }
        } catch (Exception e) {
            handleError("JavaSeisWrapper.putIntPropertyValue: Name " + _property_name + ":", e);
        }
        return verifyStatus();
    }

    public int setFloatProperty(float value) {
        try {
            if (!_jsw.putFloatPropertyValue(_property_name, value)) {
                handleError("JavaSeisWrapper.putFloatPropertyValue: Name " + _property_name, null);
            }
        } catch (Exception e) {
            handleError("JavaSeisWrapper.putFloatPropertyValue: Name " + _property_name + ":", e);
        }
        return verifyStatus();
    }

    public int setDoubleProperty(double value) {
        try {
            if (!_jsw.putDoublePropertyValue(_property_name, value)) {
                handleError("JavaSeisWrapper.putDoublePropertyValue: Name " + _property_name, null);
            }
        } catch (Exception e) {
            handleError("JavaSeisWrapper.putDoublePropertyValue: Name " + _property_name + ":", e);
        }
        return verifyStatus();
    }

    public int setLongProperty(long value) {
        try {
            if (!_jsw.putLongPropertyValue(_property_name, value)) {
                handleError("JavaSeisWrapper.putLongPropertyValue: Name " + _property_name, null);
            }
        } catch (Exception e) {
            handleError("JavaSeisWrapper.putLongPropertyValue: Name " + _property_name + ":", e);
        }
        return verifyStatus();
    }

    public int getIntProperty() {
        int retval;
        try {
            retval = _js.getIntPropertyValue(_property_name);
        } catch (Exception e) {
            handleError("JavaSeisWrapper.getIntPropertyValue: Name " + _property_name + ": ", e);
            retval = -999;
        }
        return retval;
    }

    public float getFloatProperty() {
        float retval;
        try {
            retval = _js.getFloatPropertyValue(_property_name);
        } catch (Exception e) {
            handleError("JavaSeisWrapper.getFloatPropertyValue: Name " + _property_name + ": ", e);
            retval = -999;
        }
        return retval;
    }

    public double getDoubleProperty() {
        double retval;
        try {
            retval = _js.getDoublePropertyValue(_property_name);
        } catch (Exception e) {
            handleError("JavaSeisWrapper.getDoublePropertyValue: Name " + _property_name + ": ", e);
            retval = -999;
        }
        return retval;
    }

    public long getLongProperty() {
        long retval;
        try {
            retval = _js.getLongPropertyValue(_property_name);
        } catch (Exception e) {
            handleError("JavaSeisWrapper.getLongPropertyValue: Name " + _property_name + ": ", e);
            retval = -999;
        }
        return retval;
    }

    private class BinData {

        private double[] _world;

        private double[] _grid;

        private long[] _logical;

        private boolean _just_got_data;

        private BinData() {
            _world = new double[6];
            _world[0] = 0;
            _world[1] = 0;
            _world[2] = 1;
            _world[3] = 0;
            _world[4] = 0;
            _world[5] = 1;
            _grid = new double[4];
            _grid[0] = 0;
            _grid[1] = 0;
            _grid[2] = 1;
            _grid[3] = 1;
            _logical = new long[8];
            _logical[0] = 0;
            _logical[1] = 0;
            _logical[2] = 1;
            _logical[3] = 0;
            _logical[4] = 0;
            _logical[5] = 1;
            _logical[6] = 1;
            _logical[7] = 1;
        }
    }

    public int getBin() {
        if (_status == NORMAL) {
            if (!_js.getBinGrid(_bd._world, _bd._grid, _bd._logical)) {
                handleError("JavaSeisWrapper.getBin: " + _js.errorMessage(), null);
            }
            _bd._just_got_data = _status == NORMAL;
        }
        return verifyStatus();
    }

    /**
   * getBinWorld returns 6 values describing the
   *   JavaSeis BinGrid world coordinates
   *   
   * @param world
   *   upon normal return, the array will contain 6 values:
   *     worldx0, worldy0,
   *     worldx1, worldy1,
   *     worldx2, worldy2
   * @return
   *   1 for normal return, otherwise 0
   */
    public int getBinWorld(double[] world) {
        if (!_bd._just_got_data) {
            _status = ERROR;
        } else if (_status == NORMAL) {
            try {
                for (int k2 = 0; k2 < _bd._world.length; k2++) {
                    world[k2] = _bd._world[k2];
                }
            } catch (Exception e) {
                handleError("JavaSeisWrapper.getBinWorld: ", e);
            }
        }
        return verifyStatus();
    }

    /**
   * getBinGrid returns 4 values describing the
   *   JavaSeis BinGrid grid coordinates
   *   
   * @param grid
   *   upon normal return, the array will contain 4 values:
   *     gridx0, gridy0,
   *     griddx, griddy
   * @return
   *   1 for normal return, otherwise 0
   */
    public int getBinGrid(double[] grid) {
        if (!_bd._just_got_data) {
            _status = ERROR;
        } else if (_status == NORMAL) {
            try {
                for (int k2 = 0; k2 < _bd._grid.length; k2++) {
                    grid[k2] = _bd._grid[k2];
                }
            } catch (Exception e) {
                handleError("JavaSeisWrapper.getBinGrid: ", e);
            }
        }
        return verifyStatus();
    }

    /**
   * getBinLogical returns 8 values describing the
   *   JavaSeis BinGrid logical coordinates
   *   
   * @param logical
   *   upon normal return, the array will contain 8 values:
   *     logicalx0, logicaly0,
   *     logicalx1, logicaly1,
   *     logicalx2, logicaly2,
   *     logicaldx, logicaldy
   * @return
   *   1 for normal return, otherwise 0
   */
    public int getBinLogical(long[] logical) {
        if (!_bd._just_got_data) {
            _status = ERROR;
        } else if (_status == NORMAL) {
            try {
                for (int k2 = 0; k2 < _bd._logical.length; k2++) {
                    logical[k2] = _bd._logical[k2];
                }
            } catch (Exception e) {
                handleError("JavaSeisWrapper.getBinLogical: ", e);
            }
        }
        return verifyStatus();
    }

    /**
  Close the JavaSeisFile rendering reading defunct with this instance.
   */
    public void close() {
        if (_status == NORMAL) {
            _js.close();
            handleError("JavaSeisWrapper.close: File not open", null);
        }
    }

    public int delete() {
        int retval = 1;
        close();
        try {
            if (!_jsw.delete()) {
                handleError("JavaSeisWrapper.delete: " + _jsw.errorMessage(), null);
                retval = 0;
            }
        } catch (Exception e) {
            handleError("JavaSeisWrapper.delete: ", e);
            retval = 0;
        }
        return retval;
    }

    protected int getBytes(String string, byte[] byts) {
        Arrays.fill(byts, (byte) 0);
        int retval = Math.min(string.trim().length(), byts.length);
        if (retval < 1) return retval;
        try {
            ByteBuffer bb = ByteBuffer.wrap(string.getBytes("US-ASCII"), 0, retval);
            bb.get(byts, 0, retval);
        } catch (Exception e) {
            handleError("JavaSeisWrapper.getBytes: ", e);
            retval = 0;
        }
        return retval;
    }

    protected int verifyString(String string) {
        try {
            _status = _status == NORMAL && string.length() > 0 ? NORMAL : ERROR;
        } catch (Exception e) {
            handleError("JavaSeisWrapper.verifyString: ", e);
        }
        return verifyStatus();
    }

    protected int verifyStatus() {
        int retval = _status == NORMAL ? 1 : 0;
        return retval;
    }

    protected void handleError(String prefix, Exception e) {
        _status = ERROR;
        String loc_prefix = prefix != null ? prefix : "";
        String loc_suffix = e != null ? e.getMessage() : "";
        String error_message = loc_prefix + loc_suffix;
        if (error_message.length() > 0) {
            _error_message = error_message;
        }
    }
}
