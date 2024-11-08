package org.javaseis.util.access;

import java.nio.ByteBuffer;
import org.javaseis.properties.PropertyDescription;
import org.javaseis.properties.TraceProperties;
import org.javaseis.util.SeisException;
import edu.mines.jtk.util.ParameterSet;

public class Frame {

    private long _first_trace = -1;

    private long _last_trace = -1;

    private int _traces_in_frame;

    private float[][] _trace_data_array;

    private TraceProperties _properties;

    private int _trace_index = -1;

    private boolean _trace_array_accessed;

    private boolean _trace_array_modified;

    private boolean _trace_array_initialized;

    private boolean _header_accessed;

    private boolean _header_modified;

    private boolean _headers_initialized;

    private boolean _read_write;

    private boolean _using_headers;

    public Frame(boolean read_write) {
        this(read_write, true);
    }

    public Frame(boolean read_write, boolean using_headers) {
        _read_write = read_write;
        _using_headers = using_headers;
    }

    public boolean valid() {
        boolean retval = _first_trace > -1 && _last_trace > -1 && _traces_in_frame == _last_trace - _first_trace + 1 && _trace_array_initialized && headersValid();
        return retval;
    }

    public long getFirstTrace() {
        return _first_trace;
    }

    public int getTraceCount() {
        return (int) (_last_trace - _first_trace + 1);
    }

    public TraceProperties getProperties() {
        return getHeader();
    }

    public float[][] getArray() {
        if (_read_write && _trace_array_initialized) {
            _trace_array_accessed = true;
        }
        return _trace_data_array;
    }

    public TraceProperties getHeader() {
        TraceProperties retval = _properties;
        if (!_using_headers) {
            retval = null;
        } else if (_read_write && _headers_initialized) {
            _header_accessed = true;
        }
        return retval;
    }

    public float[] readTrace() throws SeisException {
        if (!_trace_array_initialized) {
            throw new SeisException("Trace array not initialized");
        }
        float[] retval = new float[_trace_data_array[_trace_index].length];
        for (int k2 = 0; k2 < retval.length; k2++) {
            retval[k2] = _trace_data_array[_trace_index][k2];
        }
        return retval;
    }

    public int writeTrace(float[] trace) throws SeisException {
        if (!_trace_array_initialized) {
            throw new SeisException("Trace array not initialized");
        }
        if (!_read_write) {
            throw new SeisException("Frame not writeable");
        }
        if (trace == null) return 0;
        int retval = Math.min(_trace_data_array[_trace_index].length, trace.length);
        for (int k2 = 0; k2 < retval; k2++) {
            _trace_data_array[_trace_index][k2] = trace[k2];
        }
        _trace_array_modified = true;
        return retval;
    }

    public boolean includes(long trace) {
        boolean retval = trace >= _first_trace && trace <= _last_trace && headersValid();
        return retval;
    }

    public boolean couldHaveChanged() {
        boolean retval = _read_write && _trace_array_initialized && headersValid() && (_trace_array_accessed || _header_accessed || _trace_array_modified || _header_modified);
        return retval;
    }

    public boolean setTraceIndex(long index) {
        boolean retval = index >= Integer.MIN_VALUE && index <= Integer.MAX_VALUE && headersValid() && _traces_in_frame > 0;
        if (retval) {
            _trace_index = (int) index % _traces_in_frame;
            if (_using_headers) _properties.setTraceIndex(_trace_index);
        }
        return retval;
    }

    public void setFrom(Frame input) {
        _first_trace = input._first_trace;
        _last_trace = input._last_trace;
        _traces_in_frame = input._traces_in_frame;
        _trace_index = input._trace_index;
        _trace_data_array = input._trace_data_array;
        _properties = input._properties;
        _using_headers = input._using_headers;
        _header_accessed = input._header_accessed;
        _header_modified = input._header_modified;
        _headers_initialized = input._headers_initialized;
        _trace_array_accessed = input._trace_array_accessed;
        _trace_array_initialized = input._trace_array_initialized;
        _trace_array_modified = input._trace_array_modified;
        _read_write = input._read_write;
    }

    public void setRange(long first_trace, long last_trace) {
        assert (!couldHaveChanged());
        _first_trace = first_trace;
        _last_trace = last_trace;
        _traces_in_frame = (int) (last_trace - first_trace) + 1;
    }

    public void setArray(float[][] trace_data_array) {
        if (trace_data_array != null) {
            _trace_array_initialized = true;
            _trace_array_modified = true;
        } else {
            _trace_array_initialized = false;
            _trace_array_modified = false;
        }
        _trace_array_accessed = false;
        _trace_data_array = trace_data_array;
    }

    public void setProperties(TraceProperties properties) {
        if (!_using_headers) {
            properties = null;
        }
        if (properties != null) {
            _headers_initialized = true;
            _header_modified = true;
        } else {
            _headers_initialized = false;
            _header_modified = false;
        }
        _header_accessed = false;
        _properties = properties;
    }

    public boolean copyTo(Frame output) {
        boolean retval = true;
        if (_using_headers) {
            output._properties = copyTo(output._properties);
            retval = output._properties != null;
        }
        if (retval) {
            output._trace_data_array = copyTo(output._trace_data_array);
            retval = output._trace_data_array != null;
            if (retval) {
                output._first_trace = _first_trace;
                output._last_trace = _last_trace;
                output._traces_in_frame = _traces_in_frame;
                output._trace_index = _trace_index;
                output._using_headers = _using_headers;
                output._header_accessed = _header_accessed;
                output._header_modified = _header_modified;
                output._headers_initialized = _headers_initialized;
                output._trace_array_accessed = _trace_array_accessed;
                output._trace_array_initialized = _trace_array_initialized;
                output._trace_array_modified = _trace_array_modified;
                output._read_write = _read_write;
            }
        }
        return retval;
    }

    private float[][] copyTo(float[][] output) {
        if (!compatible(_trace_data_array, output)) output = createFrom(_trace_data_array);
        return copy(_trace_data_array, output);
    }

    private TraceProperties copyTo(TraceProperties output) {
        if (!compatible(_properties, output)) output = createFrom(_properties);
        return copy(_properties, output);
    }

    public boolean copyFrom(Frame input) {
        boolean retval = copyFrom(input._properties);
        if (retval) retval = copyFrom(input._trace_data_array);
        if (retval) {
            _first_trace = input._first_trace;
            _last_trace = input._last_trace;
            _traces_in_frame = input._traces_in_frame;
            _trace_index = input._trace_index;
            _using_headers = input._using_headers;
            _header_accessed = input._header_accessed;
            _header_modified = input._header_modified;
            _headers_initialized = input._headers_initialized;
            _trace_array_accessed = input._trace_array_accessed;
            _trace_array_initialized = input._trace_array_initialized;
            _trace_array_modified = input._trace_array_modified;
            _read_write = input._read_write;
        }
        return retval;
    }

    private boolean copyFrom(float[][] from) {
        boolean retval = false;
        if (!compatible(from, _trace_data_array)) {
            _trace_data_array = createFrom(from);
            if (_trace_data_array != null) _trace_array_initialized = true;
        }
        _trace_data_array = copy(from, _trace_data_array);
        if (_trace_data_array != null) {
            _trace_array_modified = true;
            retval = true;
        }
        return retval;
    }

    private boolean copyFrom(TraceProperties from) {
        boolean retval = false;
        if (!_using_headers) {
            retval = true;
        } else {
            if (!compatible(from, _properties)) {
                _properties = createFrom(from);
                if (_properties != null) _headers_initialized = true;
            }
            _properties = copy(from, _properties);
            if (_properties != null) {
                _header_modified = true;
                retval = true;
            }
        }
        return retval;
    }

    private float[][] createFrom(float[][] input) {
        if (input == null) return null;
        float[][] retval;
        try {
            retval = new float[input.length][];
            for (int k2 = 0; k2 < input.length; k2++) {
                retval[k2] = new float[input[k2].length];
            }
        } catch (Exception e) {
            retval = null;
        }
        return retval;
    }

    private TraceProperties createFrom(TraceProperties input) {
        if (input == null) return null;
        TraceProperties output;
        ParameterSet ps = new ParameterSet();
        input.toParameterSet(ps);
        try {
            output = new TraceProperties(ps);
        } catch (SeisException e) {
            return null;
        }
        return output;
    }

    /**
   * copy copies a two dimensional array from the input array to the output array.
   *   if output is not the identical size of input, then a properly sized output is
   *   created, copied to from input and returned. Otherwise, output is merely populated
   *   from input and the given output is returned.
   * @param input
   *    input array
   * @param output
   *    output array
   * @return
   *    the output array for convenience or null on a failure
   */
    private float[][] copy(float[][] input, float[][] output) {
        if (input == null || output == null) return null;
        try {
            for (int k2 = 0; k2 < input.length; k2++) {
                System.arraycopy(input[k2], 0, output[k2], 0, input[k2].length);
            }
        } catch (Exception e) {
            return null;
        }
        return output;
    }

    /**
   * copy assumes the input and output TraceProperties instances exist
   * 
   * @param input
   *    input properties
   * @param output
   *    output properties
   * @return
   *    the output properties for convenience or null on failure
   */
    private TraceProperties copy(TraceProperties input, TraceProperties output) {
        if (input == null || output == null) return null;
        ByteBuffer ibb = input.getBuffer();
        int position = ibb.position();
        int limit = ibb.limit();
        ByteBuffer obb = output.getBuffer();
        if (obb == null) {
            obb = ByteBuffer.allocateDirect(ibb.capacity());
            obb.order(ibb.order());
            output.setBuffer(obb);
        }
        try {
            ibb.position(0);
            ibb.limit(ibb.capacity());
            obb.position(0);
            obb.limit(ibb.capacity());
            for (int k2 = 0; k2 < ibb.capacity(); k2++) {
                obb.put(ibb.get());
            }
            ibb.position(position);
            ibb.limit(limit);
            obb.position(position);
            obb.limit(limit);
        } catch (Exception e) {
            return null;
        }
        return output;
    }

    private boolean compatible(float[][] input, float[][] output) {
        if (input == null || output == null) return false;
        boolean retval = input.length == output.length;
        for (int k2 = 0; retval && k2 < input.length; k2++) {
            retval = input[k2].length == output[k2].length;
        }
        return retval;
    }

    private boolean compatible(TraceProperties input, TraceProperties output) {
        if (input == null || output == null) return false;
        PropertyDescription[] ipd = input.getTraceProperties();
        PropertyDescription[] opd = output.getTraceProperties();
        boolean retval = ipd.length == opd.length;
        ByteBuffer ibb = input.getBuffer();
        ByteBuffer obb = output.getBuffer();
        boolean test = ibb != null && obb != null;
        retval = (ibb == null && obb == null) || test;
        if (test) {
            retval = ibb.capacity() == obb.capacity();
        }
        for (int k2 = 0; retval && k2 < ipd.length; k2++) {
            retval = ipd[k2].getCount() == opd[k2].getCount() && ipd[k2].getFormat() == opd[k2].getFormat() && ipd[k2].getLabel().equalsIgnoreCase(opd[k2].getLabel());
        }
        return retval;
    }

    public void resetWrite() {
        _header_accessed = false;
        _header_modified = false;
        _trace_array_accessed = false;
        _trace_array_modified = false;
    }

    public void init() {
        _first_trace = -1;
        _last_trace = -1;
        _traces_in_frame = 0;
        _trace_index = -1;
        _properties = null;
        _trace_data_array = null;
        _header_accessed = false;
        _header_modified = false;
        _headers_initialized = false;
        _trace_array_accessed = false;
        _trace_array_initialized = false;
        _trace_array_modified = false;
    }

    private boolean headersValid() {
        boolean retval;
        if (_properties != null) {
            retval = _headers_initialized;
        } else {
            retval = !_using_headers;
        }
        return retval;
    }
}
