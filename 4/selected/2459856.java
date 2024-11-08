package edu.ucla.sspace.matrix;

import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseHashDoubleVector;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator for sequentially accessing the rows of a {@link
 * MatrixIO.Format.SVDLIBC_SPARSE_BINARY} formatted file.
 *
 * @author David Jurgens
 */
class SvdlibcSparseBinaryFileRowIterator implements Iterator<SparseDoubleVector> {

    /**
     * The stream of bytes in the matrix file.
     */
    private final ByteBuffer data;

    /**
     * The next {@link SparseDoubleVector} to be returned.
     */
    private SparseDoubleVector next;

    /**
     * The entry number that will next be returned from the matrix
     */
    private int entry;

    /**
     * The total number of non-zero entries in the matrix
     */
    private int nzEntriesInMatrix;

    /**
     * The index of the current column
     */
    private int curCol;

    /**
     * The number of rows in the matrix.
     */
    private final int rows;

    /**
     * The number of columns in the matrix.
     */
    private final int cols;

    /**
     * Creates a new {@link SvdlibcSparseBinaryFileRowIterator} for {@code
     * matrixFile}.
     */
    public SvdlibcSparseBinaryFileRowIterator(File matrixFile) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(matrixFile, "r");
        FileChannel fc = raf.getChannel();
        data = fc.map(MapMode.READ_ONLY, 0, fc.size());
        fc.close();
        this.rows = data.getInt();
        this.cols = data.getInt();
        nzEntriesInMatrix = data.getInt();
        curCol = 0;
        entry = 0;
        advance();
    }

    /**
     * Sets {@code next} to be the next row in the data matrix.
     */
    private void advance() throws IOException {
        if (entry >= nzEntriesInMatrix) {
            next = null;
        } else {
            next = new SparseHashDoubleVector(rows);
            int nzInCol = data.getInt();
            for (int i = 0; i < nzInCol; ++i, ++entry) {
                int row = data.getInt();
                double value = data.getFloat();
                next.set(row, value);
            }
            curCol++;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        return next != null;
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector next() {
        if (next == null) throw new NoSuchElementException("No futher entries");
        SparseDoubleVector curCol = next;
        try {
            advance();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
        return curCol;
    }

    /**
     * Throws an {@link UnsupportedOperationException} if called.
     */
    public void remove() {
        throw new UnsupportedOperationException("Cannot remove from file");
    }

    /**
     * Resets the iterator to the start of the file's data.
     */
    public void reset() {
        data.rewind();
        data.getInt();
        data.getInt();
        data.getInt();
        curCol = 0;
        entry = 0;
        try {
            advance();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }
}
