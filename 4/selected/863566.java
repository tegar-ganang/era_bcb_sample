package net.sf.jvibes.sandbox.jplot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.channels.FileChannel;
import org.apache.log4j.Logger;

class DataFile {

    private static final Logger __logger = Logger.getLogger(DataFile.class);

    private static final int BUFFER_SIZE = 8 * 1024;

    private static final int HEADER_OFFSET = 8;

    private final File _file;

    private int _cols;

    private int _rows;

    private long _values;

    private FileChannel _ch;

    private ByteBuffer _bb;

    public DataFile(File f) {
        _file = f;
    }

    public int getRowCount() {
        return _rows;
    }

    public int getColumnCount() {
        return _cols;
    }

    public double getMinimum(int col) throws IOException {
        checkAccess();
        int offset = 0;
        double min = Double.MAX_VALUE;
        DoubleBuffer db;
        while ((db = readBlock(offset)).limit() > col) {
            do {
                db.position(db.position() + col);
                min = Math.min(min, db.get());
            } while (db.position() + col < db.limit());
            offset += db.limit();
        }
        return min;
    }

    /**
     * Reads a block of double values starting at offset.
     * 
     * @param offset
     *            number of double values to be skipped before reading
     * @return
     * 
     * @throws IOException
     */
    public DoubleBuffer readBlock(int offset) throws IOException {
        return readBlock(offset, ByteBuffer.allocate(BUFFER_SIZE));
    }

    public DoubleBuffer readBlock(int offset, ByteBuffer bb) throws IOException {
        checkAccess();
        _ch.position(HEADER_OFFSET + 8 * offset);
        _ch.read(bb);
        bb.flip();
        return bb.asDoubleBuffer();
    }

    public DoubleBuffer readBlock(int offset, int length) throws IOException {
        return readBlock(offset, ByteBuffer.allocate(length * 8));
    }

    private void checkAccess() throws IOException {
        if (_ch == null || !_ch.isOpen()) throw new IOException("Could not access file channel");
    }

    public double getValueAt(int row, int col) throws IOException {
        checkAccess();
        ByteBuffer bb = ByteBuffer.allocate(8);
        _ch.position((row * _cols + col) * 8 + HEADER_OFFSET);
        _ch.read(bb);
        return bb.asDoubleBuffer().get();
    }

    public DoubleBuffer getColumn(int rStart, int rEnd, int col, int numValues) throws IOException {
        if (rStart < 0 || rStart > _rows || rEnd < 0 || rEnd > _rows || col < 0 || col > _cols) throw new IllegalArgumentException();
        if (rEnd < rStart) throw new IllegalArgumentException();
        checkAccess();
        int numRows = rEnd - rStart;
        int valueMultiplier = 1;
        if (numValues > numRows) numValues = numRows; else if (numValues < numRows) valueMultiplier = numRows / (numValues - 1);
        DoubleBuffer colBuffer = ByteBuffer.allocate(8 * numValues).asDoubleBuffer();
        int offset = rStart * _cols;
        int valIdx = 0;
        while (colBuffer.position() < numValues) {
            DoubleBuffer db = readBlock(offset);
            int pos = col;
            do {
                db.position(pos);
                if (valIdx % valueMultiplier == 0) colBuffer.put(db.get());
                valIdx++;
                pos += _cols;
            } while (pos < db.limit() && colBuffer.position() < numValues);
            offset += pos - col;
        }
        colBuffer.flip();
        return colBuffer;
    }

    public DoubleBuffer getColumn(int rStart, int rEnd, int col) throws IOException {
        if (rStart < 0 || rStart > _rows || rEnd < 0 || rEnd > _rows || col < 0 || col > _cols) throw new IllegalArgumentException();
        if (rEnd < rStart) throw new IllegalArgumentException();
        checkAccess();
        int numRows = rEnd - rStart;
        DoubleBuffer colBuffer = ByteBuffer.allocate(8 * numRows).asDoubleBuffer();
        int offset = rStart * _cols;
        while (offset < _values && colBuffer.position() < numRows) {
            DoubleBuffer db = readBlock(offset);
            int pos = col;
            while (pos < db.limit() && colBuffer.position() < numRows) {
                db.position(pos);
                colBuffer.put(db.get());
                pos += _cols;
            }
            offset += pos - col;
        }
        colBuffer.flip();
        return colBuffer;
    }

    public DoubleBuffer getColumn(int col) throws IOException {
        return getColumn(0, _rows - 1, col);
    }

    public DoubleBuffer getRows(int sRow, int eRow) throws IOException {
        checkAccess();
        if (sRow < 0 || sRow >= _rows || eRow < sRow || eRow >= _rows) throw new IllegalArgumentException();
        return readBlock(sRow * _cols, (eRow - sRow) * _cols);
    }

    public DoubleBuffer getRow(int row) throws IOException {
        checkAccess();
        if (row < 0 || row >= _rows) throw new IllegalArgumentException();
        return readBlock(row * _cols, _cols);
    }

    public void open() throws IOException {
        __logger.trace("Opening " + _file.getAbsolutePath());
        FileInputStream f = new FileInputStream(_file);
        _ch = f.getChannel();
        if (_ch.size() < HEADER_OFFSET) throw new IOException("Illegal header size");
        long dataBlockSize = _ch.size() - HEADER_OFFSET;
        if (dataBlockSize % 8 != 0) throw new IOException("Illegal data block size");
        _values = dataBlockSize / 8;
        __logger.trace("Data block contains " + _values + " value(s)");
        ByteBuffer bb = ByteBuffer.allocate(HEADER_OFFSET);
        int nRead;
        _cols = 0;
        nRead = _ch.read(bb);
        if (nRead <= 0) throw new IOException("Could not read header");
        bb.flip();
        _cols = (int) bb.asDoubleBuffer().get();
        bb.clear();
        if (_values % _cols != 0) throw new IOException("Illegal data block size");
        _rows = (int) _values / _cols;
        __logger.trace("Data block size (rows/cols): " + _rows + "/" + _cols);
    }

    public void close() throws IOException {
        if (_ch == null) return;
        _ch.close();
    }
}
