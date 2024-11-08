package org.javaseis.util.access;

import java.io.File;
import org.javaseis.grid.GridDefinition;
import org.javaseis.io.Seisio;
import org.javaseis.parallel.IParallelContext;
import org.javaseis.properties.PropertyDescription;
import org.javaseis.properties.TraceProperties;
import org.javaseis.util.SeisException;
import org.javaseis.util.datavolume.JSC;
import org.javaseis.util.datavolume.VDU;
import org.javaseis.util.datavolume.VolumeParameters;
import edu.mines.jtk.util.ParameterSet;

public class JavaSeisIO implements JSIO {

    public static final int ERROR = -1;

    public static final int HAVE_NOTHING = 0;

    public static final int HAVE_TRACE = 1;

    protected static final int FRAME_COUNT = 3;

    protected TraceAccessor _js;

    protected JavaSeisDescriptor _descr;

    protected boolean _opened_before;

    protected String _io_mode;

    protected boolean _read_write;

    protected String _error_message = "Programming error: Init() has not been called yet";

    protected ParameterSet _history;

    protected String _directory;

    protected int _status = ERROR;

    protected long _trace_num = -1;

    private float[] _trace;

    private ParameterSet _bingrid;

    private static int X = 1;

    private static int Y = 2;

    protected JavaSeisIO(String directory) {
        _directory = getDirectory(directory);
        _opened_before = openedBefore(directory);
    }

    protected String getDirectory(String path) {
        return path;
    }

    protected boolean openedBefore(String path) {
        return (new JavaSeisValidator(path)).isAJavaSeisPath();
    }

    protected void openHelper(Seisio seisio) {
        _status = HAVE_NOTHING;
        _error_message = "";
        try {
            if (!setIndex(1)) {
                printError("JavaSeisIO: Failed to position to first trace", null);
                return;
            }
        } catch (Exception e) {
            printError("JavaSeisIO: Open error - ", e);
        }
        addOpenHelperProperties(seisio);
    }

    protected void addOpenHelperProperties(Seisio seisio) {
    }

    /**
  Returns the JavaSeisVolume object.

  @return         JavaSeisVolume object.
   */
    public Seisio getSeisio() {
        return _js.getSeisio();
    }

    public TraceProperties getTraceProperties() {
        return _js.getHeader();
    }

    public TraceAccessor getTraceAccessor() {
        return _js;
    }

    public int getIntPropertyValue(String name) throws SeisException {
        int retval;
        try {
            retval = getTraceProperties().getInt(name);
        } catch (Exception e) {
            handleError("JavaSeisIO.getIntPropertyValue: Named " + name + ": ", e);
            throw new SeisException(_error_message);
        }
        return retval;
    }

    public float getFloatPropertyValue(String name) throws SeisException {
        float retval;
        try {
            retval = getTraceProperties().getFloat(name);
        } catch (Exception e) {
            handleError("JavaSeisIO.getFloatPropertyValue: Named " + name + ": ", e);
            throw new SeisException(_error_message);
        }
        return retval;
    }

    public double getDoublePropertyValue(String name) throws SeisException {
        double retval;
        try {
            retval = getTraceProperties().getInt(name);
        } catch (Exception e) {
            handleError("JavaSeisIO.getDoublePropertyValue: Named " + name + ": ", e);
            throw new SeisException(_error_message);
        }
        return retval;
    }

    public long getLongPropertyValue(String name) throws SeisException {
        long retval;
        try {
            retval = getTraceProperties().getLong(name);
        } catch (Exception e) {
            handleError("JavaSeisIO.getLongPropertyValue: Named " + name + ": ", e);
            throw new SeisException(_error_message);
        }
        return retval;
    }

    /**
  Returns the status of the object.

  @return         status flag.
   */
    public int status() {
        return _status;
    }

    /**
  Returns a Java String of the error message when the status code is ERROR. If
  the status code is not ERROR, an empty String is returned;

  @return         error code.
   */
    public String errorMessage() {
        return _error_message;
    }

    public boolean isReadWrite() {
        return _read_write;
    }

    public boolean setIndex(long trace_num) {
        boolean retval;
        if (_status == ERROR) return false;
        if (trace_num == _trace_num) {
            retval = true;
        } else {
            retval = init(trace_num);
        }
        return retval;
    }

    public long getIndex() {
        return _trace_num;
    }

    public int getTrace(float[] trace) {
        float[] trc = getTrace();
        if (trc == null || trace == null) return 0;
        int retval = Math.min(trc.length, trace.length);
        System.arraycopy(trc, 0, trace, 0, retval);
        for (int k2 = retval; k2 < trace.length; k2++) {
            trace[k2] = 0.0f;
        }
        return retval;
    }

    public float[] getTrace() {
        return readTrace();
    }

    public int putTrace(float[] trace) {
        return writeTrace(trace);
    }

    public void close() {
        if (_read_write) {
            closeWrite();
        } else {
            closeRead();
        }
    }

    /**
  Close the JavaSeisFile disabling further reads using this instance.
   */
    private void closeRead() {
        try {
            _js.flush();
            getSeisio().close();
            handleError("JavaSeisIO: File not open", null);
        } catch (Exception e) {
            handleError("JavaSeisIO.closeRead: ", e);
        }
    }

    protected float[] readTrace() {
        float[] retval;
        if (_status == ERROR) {
            retval = null;
        } else if (_status == HAVE_TRACE) {
            retval = _trace;
        } else {
            try {
                _trace = _js.readTrace();
                _status = HAVE_TRACE;
                _error_message = "";
                retval = _trace;
            } catch (Exception e) {
                handleError("JavaSeisIO.readTrace: ", e);
                init();
                retval = null;
            }
        }
        return retval;
    }

    private int writeTrace(float[] trace) {
        if (_status == ERROR || !isReadWrite()) return 0;
        if (trace != null) {
            _trace = trace.clone();
        } else {
            _trace = new float[0];
        }
        int retval = writeTrace();
        if (_status != ERROR) {
            _status = HAVE_TRACE;
            _error_message = "";
            retval = _trace.length;
        }
        return retval;
    }

    protected int writeTrace() {
        int retval;
        try {
            retval = _js.writeTrace(_trace);
        } catch (Exception e) {
            handleError("JavaSeisIO.writeTrace: ", e);
            init();
            retval = 0;
        }
        return retval;
    }

    protected boolean init(long trace_num) {
        boolean retval;
        try {
            if (_js.setIndex(trace_num - 1)) {
                _status = HAVE_NOTHING;
                _error_message = "";
                init();
                _trace_num = trace_num;
                retval = true;
            } else {
                retval = false;
            }
        } catch (Exception e) {
            handleError("JavaSeisIO.init: Unexpected error ", e);
            retval = false;
            init();
        }
        return retval;
    }

    private void init() {
        _trace_num = -1;
        _trace = null;
    }

    public long getTraceCount() {
        long retval = 0;
        if (_status == ERROR) return retval;
        try {
            int num_dim = getSeisio().getGridDefinition().getNumDimensions();
            int k2;
            for (k2 = 1, retval = 1; k2 < num_dim; k2++) {
                retval *= getSeisio().getGridDefinition().getAxisLength(k2);
            }
        } catch (Exception e) {
        }
        return retval;
    }

    public int getSampleCount() {
        int retval = 0;
        if (_status == ERROR) return retval;
        try {
            retval = (int) getSeisio().getGridDefinition().getAxisLength(GridDefinition.SAMPLE_INDEX);
        } catch (Exception e) {
        }
        return retval;
    }

    public int getNumDimensions() {
        int retval = 0;
        if (_status == ERROR) return retval;
        try {
            retval = getSeisio().getGridDefinition().getNumDimensions();
        } catch (Exception e) {
        }
        return retval;
    }

    public String getDataType() {
        String retval = null;
        if (_status == ERROR) return retval;
        try {
            retval = getSeisio().getDataDefinition().getDataTypeString();
        } catch (Exception e) {
        }
        return retval;
    }

    public String getTraceFormat() {
        String retval = null;
        if (_status == ERROR) return retval;
        try {
            retval = getSeisio().getDataDefinition().getTraceFormat().getName();
        } catch (Exception e) {
        }
        return retval;
    }

    public boolean readHistory() {
        boolean retval = false;
        if (_status == ERROR) return retval;
        try {
            _history = getSeisio().readHistory();
        } catch (Exception e) {
            _history = new ParameterSet("");
        }
        retval = true;
        return retval;
    }

    public ParameterSet getHistoryCopy() {
        ParameterSet retval = null;
        if (_status == ERROR) return retval;
        if (_history == null) readHistory();
        try {
            retval = (ParameterSet) _history.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return retval;
    }

    public void createHistory(ParameterSet history) {
        if (history == null) {
            _history = new ParameterSet("");
        } else {
            try {
                _history = (ParameterSet) history.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean open() {
        return open(null);
    }

    public boolean open(IParallelContext pc) {
        boolean retval;
        if (_read_write) {
            retval = openWrite(pc);
        } else {
            retval = openRead(pc);
        }
        return retval;
    }

    private boolean openRead(IParallelContext pc) {
        boolean retval = false;
        Seisio seisio = null;
        try {
            seisio = new Seisio(_directory);
            if (pc != null) {
                seisio.open(_io_mode, pc);
            } else {
                seisio.open(_io_mode);
            }
            _opened_before = true;
            _js = new TraceAccessor(seisio, pc, _io_mode, FRAME_COUNT);
            _descr = new JavaSeisDescriptor(seisio);
            openHelper(seisio);
            readHistory();
            retval = true;
        } catch (Exception e) {
            _status = ERROR;
            _error_message = "JavaSeisIO.openRead: could not instantiate Seisio: " + e.getMessage();
            System.out.println(_error_message);
        }
        if (_status != ERROR) {
            try {
                _bingrid = seisio.getBinGrid().toParameterSet();
            } catch (Exception e) {
                _bingrid = null;
            }
        }
        return retval;
    }

    private boolean openWrite(IParallelContext pc) {
        boolean retval = false;
        if (_opened_before) {
            retval = openRead(pc);
            if (retval) retval = openWriteHelper();
            return retval;
        }
        try {
            addOpenWriteProperties();
            long[] sizes = _descr.getLengths();
            boolean sizes_is_null = sizes == null;
            long ext_size = VDU.closestPowerOfTwo(VDU.size(sizes) / 2 / (pc == null ? 1 : pc.size()));
            boolean broadcast_vp = false;
            boolean make_vp = false;
            VolumeParameters vp = null;
            SeisioCreator sc = null;
            Seisio seisio = null;
            Object[] oa = null;
            if (pc != null) {
                if (pc.size() > 1) {
                    broadcast_vp = true;
                }
                if (pc.rank() == 0) {
                    make_vp = true;
                }
            } else {
                make_vp = true;
            }
            if (make_vp) {
                File file = new File(_directory);
                String name = file.getName();
                String parent = file.getParent();
                vp = new VolumeParameters(ext_size, _descr.getDataDefinition().getDataType(), _descr.getDataDefinition().getTraceFormat(), name, parent, JSC.ROOT, JSC.HOSTS, JSC.LOC, JSC.POLICY);
                sc = new SeisioCreator(_descr, vp, _bingrid);
                seisio = sc.getSeisio();
                openWriteHelper();
            }
            if (broadcast_vp) {
                pc.barrier();
                oa = new Object[1];
                if (vp != null) {
                    oa[0] = vp;
                }
                pc.bcast(99, oa, 0, 1, 0);
                vp = (VolumeParameters) oa[0];
                if (vp == null) {
                    _status = ERROR;
                    _error_message = "JavaSeisIO.openWrite: vp from from broadcast is null";
                    System.out.println(_error_message);
                    return retval;
                }
                vp.setInitializeFlag(false);
                if (seisio == null) {
                    sc = new SeisioCreator(_descr, vp, _bingrid);
                    seisio = sc.getSeisio();
                }
            }
            seisio = new Seisio(_directory, _descr.getGridDefinition(), _descr.getDataDefinition(), _descr.getHeaderDefinition());
            if (pc != null) {
                seisio.open(Seisio.MODE_READ_WRITE, pc);
            } else {
                seisio.open(Seisio.MODE_READ_WRITE);
            }
            _opened_before = true;
            _js = new TraceAccessor(seisio, pc, "rw", FRAME_COUNT);
            openHelper(seisio);
            readHistory();
            retval = true;
        } catch (Exception e) {
            printError("JavaSeisIO.openWrite: failed for " + _directory + " ", e);
        }
        return retval;
    }

    protected boolean openWriteHelper() {
        return true;
    }

    protected boolean addOpenWriteProperties() {
        return true;
    }

    public JavaSeisDescriptor getDescr() {
        return _descr;
    }

    protected boolean setIsValid() {
        boolean retval = _read_write && !_opened_before;
        return retval;
    }

    public boolean preExisted() {
        return _opened_before;
    }

    public void closeWrite() {
        Seisio.setHasTraces(_directory, getTraceCount() > 0);
        writeHistory();
        closeRead();
        closeWriteHelper();
    }

    protected void closeWriteHelper() {
    }

    public boolean delete() {
        boolean retval = false;
        if (_js != null) {
            if (_js.isReadWrite()) {
                String path = _js.getSeisio().getBasePath();
                int index = path.lastIndexOf(".js");
                String name = path.substring(0, index);
                try {
                    retval = deleteHelper(name);
                    if (retval) {
                        retval = deleteJavaSeisDirectory();
                    }
                } catch (Exception e) {
                    printError("JavaSeisIO.delete: ", e);
                }
            }
        }
        return retval;
    }

    protected boolean deleteHelper(String name) {
        return true;
    }

    /**
   * getBinGrid returns 18 values describing the
   *   JavaSeis BinGrid
   * @param world
   *   world contains 6 values:
   *     worldx0, worldy0,
   *     worldx1, worldy1,
   *     worldx2, worldy2
   * @param grid
   *   grid contains 4 values:
   *     gridx0, gridy0,
   *     griddx, griddy
   * @param logical
   *   logical contains 8 values:
   *     logicalx0, logicaly0,
   *     logicalx1, logicaly1,
   *     logicalx2, logicaly2,
   *     logicaldx, logicaldy
   */
    public boolean getBinGrid(double[] world, double[] grid, long[] logical) {
        boolean retval = world.length >= 6 && grid.length >= 4 && logical.length >= 8 && _bingrid != null;
        if (retval) {
            world[0] = _bingrid.getDouble("WorldX0", 0);
            world[1] = _bingrid.getDouble("WorldY0", 0);
            world[2] = _bingrid.getDouble("WorldX1", 1);
            world[3] = _bingrid.getDouble("WorldY1", 0);
            world[4] = _bingrid.getDouble("WorldX2", 1);
            world[5] = _bingrid.getDouble("WorldY2", 1);
            grid[0] = _bingrid.getDouble("GridX0", 0);
            grid[1] = _bingrid.getDouble("GridY0", 0);
            grid[2] = _bingrid.getDouble("GridDX", 1);
            grid[3] = _bingrid.getDouble("GridDY", 1);
            logical[0] = _bingrid.getLong("LogicalX0", 0);
            logical[1] = _bingrid.getLong("LogicalY0", 0);
            logical[2] = _bingrid.getLong("LogicalX1", 1);
            logical[3] = _bingrid.getLong("LogicalY1", 0);
            logical[4] = _bingrid.getLong("LogicalX2", 1);
            logical[5] = _bingrid.getLong("LogicalY2", 1);
            logical[6] = _bingrid.getLong("LogicalDX", 1);
            logical[7] = _bingrid.getLong("LogicalDY", 1);
        } else {
            handleError("JavaSeisIO.getBinGrid: Bad parameters", null);
        }
        return retval;
    }

    public boolean setBinGrid(JavaSeisIO js) {
        boolean retval;
        double[] world = new double[6];
        double[] grid = new double[4];
        long[] logical = new long[8];
        TraceProperties prop;
        long length;
        GridDefinition gdef = null;
        try {
            gdef = js.getSeisio().getGridDefinition();
            retval = gdef.getNumDimensions() == 3 && gdef.getAxisLength(X) > 0 && gdef.getAxisLength(Y) > 0;
            if (!retval) {
                handleError("JavaSeisIO.setBinGrid: Bad parameters", null);
                return false;
            }
            length = 1;
            js.setIndex(length);
            prop = js.getTraceAccessor().getHeader();
            world[0] = prop.getDouble("CDP_XD");
            world[1] = prop.getDouble("CDP_YD");
            logical[0] = prop.getInt("XLINE_NO");
            logical[1] = prop.getInt("ILINE_NO");
            length = gdef.getAxisLength(X);
            js.setIndex(length);
            prop = js.getTraceAccessor().getHeader();
            world[2] = prop.getDouble("CDP_XD");
            world[3] = prop.getDouble("CDP_YD");
            logical[2] = prop.getInt("XLINE_NO");
            logical[3] = prop.getInt("ILINE_NO");
            length = gdef.getAxisLength(X) * (gdef.getAxisLength(Y) - 1) + 1;
            js.setIndex(length);
            prop = js.getTraceAccessor().getHeader();
            world[4] = prop.getDouble("CDP_XD");
            world[5] = prop.getDouble("CDP_YD");
            logical[4] = prop.getInt("XLINE_NO");
            logical[5] = prop.getInt("ILINE_NO");
        } catch (Exception e) {
            handleError("JavaSeisIO.setBinGrid: ", e);
            retval = false;
        }
        if (retval) {
            grid[0] = 0;
            grid[1] = 0;
            grid[2] = 1;
            grid[3] = 1;
            if (gdef.getAxisLength(X) == 1) {
                logical[6] = 1;
            } else {
                logical[6] = (logical[2] - logical[0]) / (gdef.getAxisLength(X) - 1);
            }
            if (gdef.getAxisLength(Y) == 1) {
                logical[7] = 1;
            } else {
                logical[7] = (logical[5] - logical[1]) / (gdef.getAxisLength(Y) - 1);
            }
            if (logical[6] != 0 && logical[7] != 0) {
                setBinGrid(world, grid, logical);
            } else {
                handleError("JavaSeisIO.setBinGrid: dx and dy cannot be zero", null);
                retval = false;
            }
        }
        return retval;
    }

    /**
   * setBinGrid sets 18 values describing the
   *   JavaSeis BinGrid
   * @param world
   *   world contains 6 values:
   *     worldx0, worldy0,
   *     worldx1, worldy1,
   *     worldx2, worldy2
   * @param grid
   *   grid contains 4 values:
   *     gridx0, gridy0,
   *     griddx, griddy
   * @param logical
   *   logical contains 8 values:
   *     logicalx0, logicaly0,
   *     logicalx1, logicaly1,
   *     logicalx2, logicaly2,
   *     logicaldx, logicaldy
   */
    public boolean setBinGrid(double[] world, double[] grid, long[] logical) {
        boolean retval = false;
        try {
            retval = world.length >= 6 && grid.length >= 4 && logical.length >= 8;
            if (retval) {
                _bingrid = new ParameterSet();
                _bingrid.setDouble("WorldX0", world[0]);
                _bingrid.setDouble("WorldY0", world[1]);
                _bingrid.setDouble("WorldX1", world[2]);
                _bingrid.setDouble("WorldY1", world[3]);
                _bingrid.setDouble("WorldX2", world[4]);
                _bingrid.setDouble("WorldY2", world[5]);
                _bingrid.setDouble("GridX0", grid[0]);
                _bingrid.setDouble("GridY0", grid[1]);
                _bingrid.setDouble("GridDX", grid[2]);
                _bingrid.setDouble("GridDY", grid[3]);
                _bingrid.setLong("LogicalX0", logical[0]);
                _bingrid.setLong("LogicalY0", logical[1]);
                _bingrid.setLong("LogicalX1", logical[2]);
                _bingrid.setLong("LogicalY1", logical[3]);
                _bingrid.setLong("LogicalX2", logical[4]);
                _bingrid.setLong("LogicalY2", logical[5]);
                _bingrid.setLong("LogicalDX", logical[6]);
                _bingrid.setLong("LogicalDY", logical[7]);
            } else {
                handleError("JavaSeisIO.setBinGrid: Bad parameters", null);
            }
        } catch (Exception e) {
            handleError("JavaSeisIO.setBinGrid: ", e);
        }
        return retval;
    }

    private void writeHistory() {
        if (_history != null) {
            try {
                getSeisio().writeHistory(_history);
            } catch (Exception e) {
                printError("JavaSeisIO.writeHistory: ", e);
            }
        }
    }

    private boolean deleteJavaSeisDirectory() {
        boolean retval = false;
        if (_js != null) {
            if (_js.isReadWrite()) {
                close();
                retval = _js.getSeisio().delete();
            }
        }
        return retval;
    }

    /**
   * 
   * setAxis sets an axis with the following parameters
   * 
   * @param dimension
   *   dimension for which the given axis parameters apply.
   *   0 is fastest changing axis (samples). the highest dimension
   *   number is the slowest changing axis (hypercube).
   * @param length
   *   length of given axis parameters
   * @param domain
   *   choices include "time", "depth", "frequency",
   *   "wavenumber", "semblance", "velocity", and "slowness"
   * @param units
   *   choices include "seconds", "feet", "meters",
   *   "hertz", "null", and "unknown"
   * @param physical_origin
   *   for example the time of the starting sample
   * @param physical_delta
   *   for example the sample rate
   * @return
   *   true if successful
   */
    public boolean setAxis(int dimension, int length, String domain, String units, double physical_origin, double physical_delta) {
        boolean retval;
        if (setIsValid()) {
            try {
                _descr.setAxis(dimension, length, domain, units, physical_origin, physical_delta);
                retval = true;
            } catch (Exception e) {
                handleError("JavaSeisIO.setAxis: ", e);
                retval = false;
            }
        } else {
            handleError("JavaSeisIO.setAxis: Invalid", null);
            retval = false;
        }
        return retval;
    }

    /**
   * 
   * setAxis sets an axis with the following parameters
   * 
   * @param dimension
   *   dimension for which the given axis parameters apply.
   *   0 is fastest changing axis (samples). the highest dimension
   *   number is the slowest changing axis (hypercube).
   * @param length
   *   length of given axis parameters
   * @param domain
   *   choices include "space", "time", "depth", "frequency",
   *   "wavenumber", "semblance", "velocity", and "slowness"
   * @param units
   *   choices include "feet", "meters", "seconds",
   *   "hertz", "null", and "unknown"
   * @param logical_origin
   *   for example the crossline number of the first trace
   * @param logical_delta
   *   for example the crossline number difference between adjacent traces
   * @param physical_origin
   *   for example the distance assigned to the first trace
   * @param physical_delta
   *   for example the distance between adjacent traces
   * @return
   *    true if successful
   */
    public boolean setAxis(int dimension, int length, String domain, String units, long logical_origin, long logical_delta, double physical_origin, double physical_delta) {
        boolean retval;
        if (setIsValid()) {
            try {
                _descr.setAxis(dimension, length, domain, units, logical_origin, logical_delta, physical_origin, physical_delta);
                retval = true;
            } catch (Exception e) {
                handleError("JavaSeisIO.setAxis: ", e);
                retval = false;
            }
        } else {
            handleError("JavaSeisIO.setAxis: Invalid", null);
            retval = false;
        }
        return retval;
    }

    /**
   * 
   * getAxis returns the axis parameters for one of the given dimensions.
   *   Each of the returned values are required to be an array with at
   *   least one element.
   *   
   * @param dimension
   *    Given (0-rel) dimension for which to return the axis parameters
   * @param length
   *    Returned length of axis
   * @param domain
   *    Returned domain of axis
   * @param units
   *    Returned units in the domain of given axis
   * @param logical_origin
   *    Returned logical origin of given axis
   * @param logical_delta
   *    Returned logical delta of given axis
   * @param physical_origin
   *    Returned physical origin of given axis
   * @param physical_delta
   *    Returned physical delta of given axis
   * @return
   *    True if the data is returned normally, otherwise false.
   */
    public boolean getAxis(int dimension, int[] length, String[] domain, String[] units, long[] logical_origin, long[] logical_delta, double[] physical_origin, double[] physical_delta) {
        boolean retval;
        try {
            retval = length.length >= 1 && domain.length >= 1 && units.length >= 1 && logical_origin.length >= 1 && logical_delta.length >= 1 && physical_origin.length >= 1 && physical_delta.length >= 1 && dimension < _descr.getNumDimensions();
            if (retval) {
                length[0] = (int) _descr.getAxes()[dimension].getLength();
                domain[0] = _descr.getAxes()[dimension].getDomain().getName();
                units[0] = _descr.getAxes()[dimension].getUnits().getName();
                logical_origin[0] = _descr.getAxes()[dimension].getLogicalOrigin();
                logical_delta[0] = _descr.getAxes()[dimension].getLogicalDelta();
                physical_origin[0] = _descr.getAxes()[dimension].getPhysicalOrigin();
                physical_delta[0] = _descr.getAxes()[dimension].getPhysicalDelta();
            } else {
                handleError("JavaSeisIO.getAxis: Bad parameters ", null);
            }
        } catch (Exception e) {
            handleError("JavaSeisIO.getAxis: ", e);
            retval = false;
        }
        return retval;
    }

    /**
   * 
   * setDescr sets the JavaSeis descriptor using a Seisio instance
   * 
   * @param seisio
   *    
   * @return
   *    true if successful
   */
    public boolean setDescr(Seisio seisio) {
        boolean retval;
        if (setIsValid()) {
            try {
                JavaSeisDescriptor descr = new JavaSeisDescriptor(seisio);
                _descr = descr;
                retval = true;
            } catch (Exception e) {
                handleError("JavaSeisIO.setDescr: ", e);
                retval = false;
            }
        } else {
            handleError("JavaSeisIO.setDescr: Invalid", null);
            retval = false;
        }
        return retval;
    }

    public boolean addIntProperty(String name, String description) {
        boolean retval;
        if (setIsValid()) {
            try {
                _descr.addProperty(new PropertyDescription(name, description, PropertyDescription.HDR_FORMAT_INTEGER, 1));
                retval = true;
            } catch (Exception e) {
                handleError("JavaSeisIO.addIntProperty: name " + name, e);
                retval = false;
            }
        } else {
            handleError("JavaSeisIO.addIntProperty: Invalid name " + name, null);
            retval = false;
        }
        return retval;
    }

    public boolean addFloatProperty(String name, String description) {
        boolean retval;
        if (setIsValid()) {
            try {
                _descr.addProperty(new PropertyDescription(name, description, PropertyDescription.HDR_FORMAT_FLOAT, 1));
                retval = true;
            } catch (Exception e) {
                handleError("JavaSeisIO.addFloatProperty: name " + name, e);
                retval = false;
            }
        } else {
            handleError("JavaSeisIO.addLongProperty: Invalid name " + name, null);
            retval = false;
        }
        return retval;
    }

    public boolean addDoubleProperty(String name, String description) {
        boolean retval;
        if (setIsValid()) {
            try {
                _descr.addProperty(new PropertyDescription(name, description, PropertyDescription.HDR_FORMAT_DOUBLE, 1));
                retval = true;
            } catch (Exception e) {
                handleError("JavaSeisIO.addDoubleProperty: name " + name, e);
                retval = false;
            }
        } else {
            handleError("JavaSeisIO.addDoubleProperty: Invalid name " + name, null);
            retval = false;
        }
        return retval;
    }

    public boolean addLongProperty(String name, String description) {
        boolean retval;
        if (setIsValid()) {
            try {
                _descr.addProperty(new PropertyDescription(name, description, PropertyDescription.HDR_FORMAT_LONG, 1));
                retval = true;
            } catch (Exception e) {
                handleError("JavaSeisIO.addLongProperty: name " + name, e);
                retval = false;
            }
        } else {
            handleError("JavaSeisIO.addLongProperty: Invalid name " + name, null);
            retval = false;
        }
        return retval;
    }

    public boolean putIntPropertyValue(String name, int value) {
        boolean retval;
        try {
            _js.getHeader().putInt(name, value);
            retval = true;
        } catch (Exception e) {
            handleError("JavaSeisIO.putIntPropertyValue: name " + name, e);
            retval = false;
        }
        return retval;
    }

    public boolean putFloatPropertyValue(String name, float value) {
        boolean retval;
        try {
            _js.getHeader().putFloat(name, value);
            retval = true;
        } catch (Exception e) {
            handleError("JavaSeisIO.putFloatPropertyValue: name " + name, e);
            retval = false;
        }
        return retval;
    }

    public boolean putDoublePropertyValue(String name, double value) {
        boolean retval;
        try {
            _js.getHeader().putDouble(name, value);
            retval = true;
        } catch (Exception e) {
            handleError("JavaSeisIO.putDoublePropertyValue: name " + name, e);
            retval = false;
        }
        return retval;
    }

    public boolean putLongPropertyValue(String name, long value) {
        boolean retval;
        try {
            _js.getHeader().putLong(name, value);
            retval = true;
        } catch (Exception e) {
            handleError("JavaSeisIO.putLongPropertyValue: name " + name, e);
            retval = false;
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
   *    true if successful
   */
    public boolean setDataType(String data_type_name) {
        boolean retval;
        if (setIsValid()) {
            try {
                _descr.setDataType(data_type_name);
                retval = true;
            } catch (Exception e) {
                handleError("JavaSeisIO.setDataType: type " + data_type_name, e);
                retval = false;
            }
        } else {
            retval = false;
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
   *    true if successful
   */
    public boolean setTraceFormat(String format_name) {
        boolean retval;
        if (setIsValid()) {
            try {
                _descr.setFormat(format_name);
                retval = true;
            } catch (Exception e) {
                handleError("JavaSeisIO.setTraceFormat: format " + format_name, e);
                retval = false;
            }
        } else {
            retval = false;
        }
        return retval;
    }

    protected void printError(String prefix, Exception e) {
        handleError(prefix, e);
        System.out.println(_error_message);
    }

    protected void handleError(String prefix, Exception e) {
        _status = ERROR;
        String loc_prefix = prefix != null ? prefix : "";
        String loc_suffix = e != null ? e.getMessage() : "";
        String error_message = loc_prefix + loc_suffix;
        if (error_message.length() > 0) _error_message = error_message;
    }
}
