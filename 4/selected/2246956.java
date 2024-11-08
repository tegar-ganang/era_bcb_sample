package org.javaseis.io;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.javaseis.grid.GridDefinition;
import org.javaseis.util.SeisException;

/**
 * The TraceMap class provides mapping support for the JavaSeis.
 * This class maintains a map of the number of traces for each frame in a
 * dataset.  This class reads the fold for an entire volume but only writes
 * the fold for a single frame.  We expect this file to be written to from
 * multiple nodes, however we expect that the job is smart enough not to write
 * to the same frame.
 *
 *  @author Steve Angelovich
 *  @since 8/10/2007
 */
public class TraceMap {

    private static Logger LOG = Logger.getLogger("org.javaseis.io.TraceMap");

    private long[] _axisLengths;

    private int[] _traceMapArray;

    private int _volumeIndex = Integer.MIN_VALUE;

    private IVirtualIO _mapIO;

    private ByteBuffer _mapBuffer;

    private IntBuffer _mapBufferView;

    private long _readCacheHit;

    private long _readCounter;

    private long _writeCounter;

    /**
   * Construct an instance of the TraceMap.
   * @param axisLengths framework lengths
   * @param byteOrder byteOrder on disk
   * @param path path the the dataset
   * @param mode r - read, w - write, wb - write buffered
   * @throws SeisException
   */
    public TraceMap(long[] axisLengths, ByteOrder byteOrder, String path, String mode) throws SeisException {
        _axisLengths = axisLengths;
        _traceMapArray = new int[(int) _axisLengths[GridDefinition.FRAME_INDEX]];
        byte[] b = new byte[4 * (int) _axisLengths[GridDefinition.FRAME_INDEX]];
        _mapBuffer = ByteBuffer.wrap(b);
        _mapBuffer.order(byteOrder);
        _mapBufferView = _mapBuffer.asIntBuffer();
        if (mode.compareToIgnoreCase("r") == 0) {
            _mapIO = new VirtualIO(path + File.separator + Seisio.TRACE_MAP, mode);
        } else if (mode.compareToIgnoreCase("wb") == 0) {
            long totalFrames = 1;
            for (int i = 1; i < _axisLengths.length; i++) {
                totalFrames = totalFrames * _axisLengths[i];
            }
            int framesPerVolume = (int) axisLengths[2];
            int tpf = (int) axisLengths[1];
            int bufSize = framesPerVolume * 4;
            if (framesPerVolume == totalFrames) {
                int f = (int) (framesPerVolume * .01);
                if (f < 2) f = (int) (framesPerVolume * .1);
                if (f < 2) f = 2;
                bufSize = f * 4;
            }
            int blockSize = 0;
            long flen = (long) framesPerVolume * (long) tpf * 4l;
            _mapIO = new VirtualMappedIO(path + File.separator + Seisio.TRACE_MAP, Integer.MAX_VALUE, flen);
        } else {
            _mapIO = new VirtualIO(path + File.separator + Seisio.TRACE_MAP, mode);
        }
        this.initTraceMapArray();
    }

    /**
   * Checks for valid volume index.
   */
    private void checkVolumeIndex() {
        if (_volumeIndex == Integer.MIN_VALUE) {
            throw new RuntimeException("Volume index was not initialized.");
        }
    }

    /** Returns the fold at the position. */
    public int getFold(int[] position) throws SeisException {
        loadVolume(position);
        return (_traceMapArray[position[GridDefinition.FRAME_INDEX]]);
    }

    /** 
   * Causes the cache to be invalidated so that any calls to
   * get/put fold will then be forced to do a read.
   */
    public void emptyCache() {
        _volumeIndex = Integer.MIN_VALUE;
    }

    /** Sets the fold at the logical position. */
    public void putFold(int[] position, int numTraces) throws SeisException {
        _writeCounter++;
        _traceMapArray[position[GridDefinition.FRAME_INDEX]] = numTraces;
        int volumeIndex = getVolumeIndex(position);
        int frameIndex = position[GridDefinition.FRAME_INDEX];
        long oldMapFilePosition = 4 * (_axisLengths[GridDefinition.FRAME_INDEX] * volumeIndex + frameIndex);
        if (Seisio._logger.isLoggable(Level.FINE)) {
            Seisio.log(Level.INFO, "Writing to disk at offset " + oldMapFilePosition + " for volume index " + _volumeIndex);
        }
        long newMapFilePosition = _mapIO.setPosition(oldMapFilePosition);
        if (newMapFilePosition != oldMapFilePosition) {
            throw new SeisException("Unable to seek to file offset: " + oldMapFilePosition);
        }
        _mapBuffer.clear();
        _mapBufferView.clear();
        _mapBufferView.put(_traceMapArray[frameIndex]);
        _mapBuffer.position(0);
        _mapBuffer.limit(4);
        _mapIO.write(_mapBuffer);
    }

    /** 
   * Sets the fold for an entire volume.  This does not attempt to merge
   * the fold values for frames within this volume.  The typical use for
   * this method is during initialization of the foldmap.  (Use the method
   * above to only write the fold for a single frame when writting to the
   * dataset in parallel. 
   **/
    private void putFold(int[] position, int[] fold) throws SeisException {
        loadVolume(position);
        this.checkVolumeIndex();
        for (int i = 0; i < _traceMapArray.length; i++) _traceMapArray[i] = fold[i];
        writeVolume();
    }

    /**
   * Re-initializes the trace map array.
   */
    private void initTraceMapArray() {
        Arrays.fill(_traceMapArray, Integer.MIN_VALUE);
    }

    /**
   * Initialize the trace map on disk by setting all values in the map to zero
   * This is not thread safe or parallel IO safe. The caller should make sure
   * that this is only done by a single node.
   *
   * @throws SeisException
   */
    public void intializeTraceMapOnDisk() throws SeisException {
        for (int frameIndex = 0; frameIndex < _traceMapArray.length; frameIndex++) {
            _traceMapArray[frameIndex] = 0;
        }
        int[] position = new int[5];
        int ndim = _axisLengths.length;
        int nhyp = 1;
        int nvol = 1;
        int nfrm = 1;
        if (ndim > 4) nhyp = (int) _axisLengths[4];
        if (ndim > 3) nvol = (int) _axisLengths[3];
        if (ndim > 2) nfrm = (int) _axisLengths[2];
        for (int ihyp = 0; ihyp < nhyp; ihyp++) {
            position[4] = ihyp;
            for (int ivol = 0; ivol < nvol; ivol++) {
                position[3] = ivol;
                position[2] = 0;
                for (int ifrm = 0; ifrm < nfrm; ifrm++) {
                    position[2] = ifrm;
                    putFold(position, 0);
                }
            }
        }
        initTraceMapArray();
        try {
            _mapIO.flush();
        } catch (IOException e) {
            throw new SeisException("Error flushing map to disk: " + e.getMessage());
        }
    }

    /**
   * Write a frame of fold to disk.  This assumes that the volumeIndex
   * has already been set and that the fold has been put into the
   * array.  This is not intended to be called externally.
   */
    private void writeFrame(int frameIndex) throws SeisException {
        _writeCounter++;
        this.checkVolumeIndex();
        long oldMapFilePosition = 4 * (_axisLengths[GridDefinition.FRAME_INDEX] * _volumeIndex + frameIndex);
        if (Seisio._logger.isLoggable(Level.FINE)) {
            Seisio.log(Level.INFO, "Writing to disk at offset " + oldMapFilePosition + " for volume index " + _volumeIndex);
        }
        long newMapFilePosition = _mapIO.setPosition(oldMapFilePosition);
        if (newMapFilePosition != oldMapFilePosition) {
            throw new SeisException("Unable to seek to file offset: " + oldMapFilePosition);
        }
        _mapBuffer.clear();
        _mapBufferView.clear();
        _mapBufferView.put(_traceMapArray);
        _mapBuffer.position(4 * frameIndex);
        _mapBuffer.limit(4 * (frameIndex + 1));
        _mapIO.write(_mapBuffer);
    }

    /**
   * Write an entire volume of fold to disk.  This assumes that the volumeIndex
   * has already been set and that the fold has been put into the
   * array.  This is not intended to be called externally.
   */
    private void writeVolume() throws SeisException {
        this.checkVolumeIndex();
        _mapBuffer.clear();
        _mapBufferView.clear();
        _mapBufferView.put(_traceMapArray);
        _mapBuffer.position(0);
        _mapBuffer.limit(4 * _traceMapArray.length);
        _mapIO.write(_mapBuffer);
    }

    /**
   * Based on the position find the correct volume and load the
   * data.  We use the position array so that we can use this to
   * index to the correct position in 3, 4 and 5D datasets.
   */
    public void loadVolume(int[] position) throws SeisException {
        int volumeIndex = getVolumeIndex(position);
        if (_volumeIndex == volumeIndex) {
            _readCacheHit++;
            return;
        } else {
            _readCounter++;
        }
        _mapBuffer.clear();
        _mapBuffer.position(0);
        _mapBufferView.position(0);
        long oldMapFilePosition = 4 * _axisLengths[GridDefinition.FRAME_INDEX] * volumeIndex;
        if (Seisio._logger.isLoggable(Level.FINE)) {
            Seisio.log(Level.INFO, "Reading from disk at offset " + oldMapFilePosition + " for volume index " + volumeIndex);
        }
        long newMapFilePosition = _mapIO.setPosition(oldMapFilePosition);
        if (newMapFilePosition != oldMapFilePosition) {
            throw new SeisException("Unable to seek to file offset: " + oldMapFilePosition);
        }
        int numBytes = _mapIO.read(_mapBuffer);
        if (numBytes != (4 * _axisLengths[GridDefinition.FRAME_INDEX])) {
            this.initTraceMapArray();
        } else {
            _mapBufferView.get(_traceMapArray);
        }
        _volumeIndex = volumeIndex;
    }

    /** Returns the volume index based on the position */
    private int getVolumeIndex(int[] position) throws SeisException {
        int volumeIndex = -1;
        if (_axisLengths.length == 3) volumeIndex = 0; else if (_axisLengths.length == 4) volumeIndex = position[GridDefinition.VOLUME_INDEX]; else if (_axisLengths.length == 5) {
            long volumesPerHypberCube = _axisLengths[GridDefinition.VOLUME_INDEX];
            long hcOffset = position[GridDefinition.HYPERCUBE_INDEX] * volumesPerHypberCube;
            volumeIndex = position[GridDefinition.VOLUME_INDEX];
            volumeIndex += hcOffset;
        }
        if (volumeIndex < 0) {
            throw new SeisException("Invalid volume index: " + volumeIndex);
        }
        return volumeIndex;
    }

    /** Return the tracemap for the current volume */
    public int[] getTraceMapArray() {
        return _traceMapArray;
    }

    /**
   * Asserts that all trace map values have been initialized (i.e. > 0). Used
   * for testing.
   */
    protected void assertAllValuesInitialized() {
        for (int frameIndex = 0; frameIndex < _traceMapArray.length; frameIndex++) {
            if (_traceMapArray[frameIndex] < 1) {
                throw new RuntimeException("Invalid trace map value (" + _traceMapArray[frameIndex] + ") found at frame index " + frameIndex);
            }
        }
    }

    public IVirtualIO getMapIO() {
        return _mapIO;
    }

    public void close() throws SeisException {
        LOG.fine("TraceMap reads = " + _readCounter + " read cached hits " + _readCacheHit + " TraceMap writes = " + _writeCounter);
        if (_mapIO instanceof BufferedVirtualIO) {
            long[] stats = ((BufferedVirtualIO) _mapIO).getWriteStats();
            LOG.info("writes = " + stats[0] + " actual disk writes = " + stats[1] + " gaps filled = " + stats[2] + " gaps larger than a single block = " + stats[3]);
        }
        _mapIO.close();
    }

    public void trackTime(boolean flag) {
        _mapIO.trackTime(flag);
    }

    public long getIoBytes() {
        return _mapIO.getIoBytes();
    }

    public float getIoTime() {
        return _mapIO.getIoTime();
    }
}
