package org.javaseis.util.access;

import org.javaseis.grid.GridDefinition;
import org.javaseis.io.Seisio;
import org.javaseis.parallel.IParallelContext;
import org.javaseis.properties.DataFormat;
import org.javaseis.properties.TraceProperties;
import org.javaseis.util.SeisException;
import org.javaseis.util.datavolume.VolumeEvaluator;

/**
 *
 * TraceAccessor reads or writes a hypercube trace by trace. Designed
 *   for use in parallel, a layered I/O method is used to write and read single traces.
 *   The layering is done internally by writing and reading entire frames during a single
 *   disk access. This is only efficient if the traces are accessed on no more than
 *   a few frames at a time. By this, one can conclude that the class would be a poor 
 *   choice for generally random I/O spread over arbitrary frames. To prevent trace I/O
 *   collisions, while a specific frame is being written, all other processes must be
 *   blocked from reading or writing said frame. Additionally, trace allotments are
 *   assigned to each processor only in complete frames to avoid writing I/O collisions
 *   among the processing ranks. The drawback is that the ranks will likely not do equal
 *   amounts of I/O.
 *
 */
public class TraceAccessor {

    private Seisio _seisio = null;

    private IParallelContext _pc = null;

    private long[] _lengths = null;

    private int _cur_rank = -1;

    private VolumeEvaluator _vol_eval = null;

    protected boolean _read_write;

    protected long _cur_trace = -1;

    protected long _cur_frame = -1;

    protected long _first_trace = -1;

    protected long _last_trace = -1;

    protected long _first_frame = -1;

    protected long _last_frame = -1;

    protected long _first_trace_in_frame = -1;

    protected long _last_trace_in_frame = -1;

    protected int _traces_in_frame = 0;

    protected boolean _work_to_do = false;

    protected FrameBuffer _frame_buffer;

    protected static final long AT_BEG = -1;

    protected static final long AT_END = +1;

    /**
   *
   * TraceAccessor constructor assumes there are at least two dimensions. The
   *   constructor populates all the variables. The constructor is intended to be
   *   used only in a derived class to prevent this class from doing random I/O.
   *
   * @param seisio Seisio object
   * @param pc  parallel context object
   * @param io_mode  Seisio.MODE_READ_WRITE or Seisio.MODE_READ_ONLY
   * @param frame_count  is the number of frames to use as a buffer (1 or more)
   */
    public TraceAccessor(Seisio seisio, IParallelContext pc, String io_mode, int frame_count) {
        _seisio = seisio;
        _pc = pc;
        if (io_mode.equalsIgnoreCase(Seisio.MODE_READ_WRITE)) {
            _read_write = true;
        } else if (io_mode.equalsIgnoreCase(Seisio.MODE_READ_ONLY)) {
            _read_write = false;
        } else {
            throw new RuntimeException("TraceAccessor: invalid rw-" + io_mode);
        }
        _lengths = _seisio.getGridDefinition().getAxisLengths();
        int k2, size;
        if (_pc != null) {
            for (k2 = _lengths.length - 1; _lengths[k2] < 2 && k2 >= 0; k2--) ;
            k2++;
            if (k2 > 0) {
                size = _pc.size();
            } else {
                size = 1;
            }
            _cur_rank = _pc.rank();
            if (size == 1 && _cur_rank != 0) size = 0;
        } else {
            size = 1;
            _cur_rank = 0;
        }
        if (size != 0) {
            assert (GridDefinition.FRAME_INDEX < _lengths.length);
            _traces_in_frame = (int) _lengths[GridDefinition.TRACE_INDEX];
            _vol_eval = new VolumeEvaluator(_seisio);
            long total = _vol_eval.getTotal();
            long[] fts = new long[size];
            for (k2 = 0; k2 < size; k2++) {
                fts[k2] = getFirstTraceInFrame(k2, size, _vol_eval);
            }
            long[] lts = new long[size];
            for (k2 = 1; k2 < size; k2++) {
                lts[k2 - 1] = fts[k2] - 1;
            }
            lts[size - 1] = total - 1;
            if (_cur_rank > 0) {
                if (fts[_cur_rank] < lts[_cur_rank - 1]) {
                    fts[_cur_rank] = lts[_cur_rank - 1] + 1;
                }
            }
            _first_trace = fts[_cur_rank];
            _last_trace = lts[_cur_rank];
            _work_to_do = _first_trace < _last_trace;
            if (frame_count < 1) frame_count = 1;
            _frame_buffer = new FrameBuffer(this, frame_count);
            if (_work_to_do) {
                _first_frame = _first_trace / _traces_in_frame;
                _last_frame = _last_trace / _traces_in_frame;
            }
        }
    }

    /**
   *
   * getFrameCount returns the toal number of frames associated with the Seisio defined
   *   when this class was instantiated.
   *
   * @return the total number of frames
   */
    public static int getFrameCount(IParallelContext pc, Seisio seisio) {
        long retval = (new VolumeEvaluator(seisio)).getLengths()[GridDefinition.FRAME_INDEX];
        if (retval > Integer.MAX_VALUE) {
            throw new RuntimeException("TraceAccessor.getFrameCount: number of frames too big");
        }
        return (int) retval;
    }

    /**
   *
   * init is a convenience method used to reset the _cur_rank variable from the parallel
   *   context. It either sets the current trace to be one less than the initial trace for
   *   the current rank or it sets the current trace to be one more than the final trace.
   *
   * @param where  if equal to AT_END, make current position the ending + 1, otherwise
   *               make current position the beginning - 1.
   *
   */
    protected void init(long where) {
        if (_pc != null) {
            _cur_rank = _pc.rank();
        } else {
            _cur_rank = 0;
        }
        if (_work_to_do) {
            if (where == AT_END) {
                _cur_trace = _last_trace + 1;
                _cur_frame = _last_frame + 1;
            } else {
                _cur_trace = _first_trace - 1;
                _cur_frame = _first_frame - 1;
            }
            initFrame();
        }
    }

    protected void initFrame() {
        _first_trace_in_frame = _traces_in_frame * _cur_frame;
        _last_trace_in_frame = _first_trace_in_frame + _traces_in_frame - 1;
    }

    private static long getFirstTraceInFrame(int group, int num_groups, VolumeEvaluator vol_eval) {
        long[] lengths = vol_eval.getLengths();
        int traces_in_frame = (int) lengths[GridDefinition.TRACE_INDEX];
        long total = vol_eval.getTotal();
        long group_total = (long) Math.ceil((double) (total / num_groups));
        long nt = group * group_total;
        long nf = nt / traces_in_frame;
        long retval = nf * traces_in_frame;
        return retval;
    }

    public boolean isReadWrite() {
        return _read_write;
    }

    /**
   *
   * getHeader returns the header TraceProperties from Seisio. Use this call to set header
   *   values for current trace. It is important that the block and unblock methods are used
   *
   * @return  TraceProperties.
   */
    public TraceProperties getHeader() {
        TraceProperties retval = _frame_buffer.getHeader(_cur_trace);
        return retval;
    }

    /**
   *
   * readTrace returns the current trace.
   *
   * @return  float[] trace array.
   * @throws  SeisException
   */
    public float[] readTrace() throws SeisException {
        float[] retval = null;
        if (_work_to_do) {
            if (_traces_in_frame < 1) throw new SeisException("No traces in frame " + _cur_frame);
            retval = _frame_buffer.readTrace();
        }
        return retval;
    }

    /**
   *
   * writeTrace moves the given trace into the current trace.
   *
   * @param trace  float[] array containing the values of the current trace.
   *
   * @return  number of traces written
   */
    public int writeTrace(float[] trace) throws SeisException {
        int retval = 0;
        if (_work_to_do) {
            if (trace == null) throw new SeisException("Trace is null");
            retval = _frame_buffer.writeTrace(trace);
        }
        return retval;
    }

    /**
   *
   * getFirst returns the value of the first trace without affecting the current trace
   *   position.
   *
   * @return  long trace value.
   */
    public long getFirst() {
        return _first_trace;
    }

    /**
   *
   * getLast returns the value of the last trace without affecting the current trace position.
   *
   * @return  long trace value.
   */
    public long getLast() {
        return _last_trace;
    }

    /**
   *
   * getCurrent returns the value of the current trace.
   *
   * @return  long trace value.
   */
    public long getCurrent() {
        return _cur_trace;
    }

    /**
   *
   * getVolumeEvaluator returns the VolumeEvaluator object based upon Seisio
   *
   * @return  VolumeEvaluator object.
   */
    public VolumeEvaluator getVolumeEvaluator() {
        return _vol_eval;
    }

    /**
   *
   * If any traces have been written to the current frame, it will be written immediately.
   *
   */
    public void flush() throws SeisException {
        _frame_buffer.flush();
    }

    /**
   * setIndex will set the current trace index if possible. If not possible, it will not
   *   alter the current trace index. If necessary, it will change the current frame.
   *   
   * @param trace_index
   *    given global trace index
   * @return
   *    true if given new trace index is established successfully. If request fails,
   *    false is returned.
   * @throws SeisException
   */
    public boolean setIndex(long trace_index) throws SeisException {
        boolean retval;
        if (_work_to_do) {
            if (trace_index == _cur_trace) return true;
            retval = updateCurrentTraceAndFrame(trace_index);
            if (retval) {
                if (_cur_trace < _first_trace_in_frame || _cur_trace > _last_trace_in_frame) {
                    if (_read_write) {
                        if (_frame_buffer.isFull()) {
                            retval = writeOldestFrame();
                        }
                    }
                    initFrame();
                    _frame_buffer.setTraceIndex(_cur_trace);
                    if (_read_write) {
                        readCurrentFrame();
                    } else {
                        retval = readCurrentFrame();
                    }
                } else {
                    retval = _frame_buffer.setTraceIndex(_cur_trace);
                }
            }
        } else {
            retval = false;
        }
        return retval;
    }

    private boolean updateCurrentTraceAndFrame(long cur_trace) {
        boolean retval;
        if (cur_trace > _last_trace || cur_trace < _first_trace) {
            retval = false;
        } else {
            long cur_frame = cur_trace / _traces_in_frame;
            retval = cur_frame <= _last_frame;
            if (retval) {
                _cur_trace = cur_trace;
                _cur_frame = cur_frame;
            }
        }
        return retval;
    }

    protected boolean readCurrentFrame() throws SeisException {
        boolean retval = _frame_buffer.readFrame(_first_trace_in_frame, _last_trace_in_frame, _cur_trace) != null;
        return retval;
    }

    protected boolean writeOldestFrame() throws SeisException {
        if (!_read_write) {
            throw new SeisException("Attempt to write when ReadOnly");
        }
        boolean retval = true;
        int count;
        int frame_save = _frame_buffer.getFrameIndex();
        int frame = _frame_buffer.oldestModifiedFrame();
        _frame_buffer.setFrameIndex(frame);
        if (_frame_buffer.couldHaveChanged()) {
            count = _frame_buffer.writeFrame();
            retval = count > 0;
        }
        _frame_buffer.setFrameIndex(frame_save);
        return retval;
    }

    /**
   *
   * getSeisioPosition returns a position vector useful with Seisio given a trace index,
   *   and VolumeEvaluator.
   *
   *
   * @param index  given trace index
   * @return
   */
    public int[] getSeisioPosition(long index) {
        long[] lengths = _vol_eval.getLengths();
        long[] pos = _vol_eval.getPosition(index);
        int[] retval = new int[lengths.length];
        for (int k2 = 1; k2 < lengths.length; k2++) retval[k2] = (int) pos[k2 - 1];
        retval[0] = 0;
        return retval;
    }

    public Seisio getSeisio() {
        return _seisio;
    }

    /**
   *
   * getFirstTraceByteInFrame returns a byte index aligned with the first trace in the nearest
   *   frame to the given group of traces.
   *
   * @param group  is which specific group of traces. e.g. group could be a process rank or
   *               an extent.
   * @param num_groups  is the total number of groups of traces. e.g. num_groups could be the
   *                    number of processes or extents.
   * @param vol_eval  a VolumeEvaluator which describes the volume of interest
   * @return  the trace byte index
   */
    public static long getFirstTraceByteInFrame(int group, int num_groups, VolumeEvaluator vol_eval, DataFormat df) {
        long[] lengths = vol_eval.getLengths();
        int samples_in_trace = (int) lengths[GridDefinition.SAMPLE_INDEX];
        long retval = samples_in_trace * getFirstTraceInFrame(group, num_groups, vol_eval) * df.getBytesPerSample();
        return retval;
    }

    /**
   *
   * getFirstHeaderByteInFrame returns a byte index aligned with the first header in the
   *   nearest frame to the given group of traces.
   *
   * @param group  is which specific group of traces. e.g. group could be a process rank or
   *               an extent.
   * @param num_groups  is the total number of groups of traces. e.g. num_groups could be the
   *                    number of processes or extents.
   * @param vol_eval  a VolumeEvaluator which describes the volume of interest
   * @param header  describes the trace header records
   * @return  the header byte index
   */
    public static long getFirstHeaderByteInFrame(int group, int num_groups, VolumeEvaluator vol_eval, TraceProperties header) {
        int bytes_in_header = header.getRecordLength();
        long retval = bytes_in_header * getFirstTraceInFrame(group, num_groups, vol_eval);
        return retval;
    }
}
