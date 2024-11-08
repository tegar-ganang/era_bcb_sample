package la4j.matrix.sparse;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import la4j.factory.Factory;
import la4j.factory.SparseFactory;
import la4j.matrix.AbstractMatrix;
import la4j.matrix.Matrix;
import la4j.vector.Vector;

public class SparseMatrix extends AbstractMatrix implements Matrix {

    private static final int MINIMUM_SIZE = 32;

    private static final long serialVersionUID = 1L;

    private double values[];

    private int columnIndices[];

    private int rowPointers[];

    private int nonzero;

    public SparseMatrix() {
        this(0, 0);
    }

    public SparseMatrix(int rows, int columns) {
        this(rows, columns, 0);
    }

    public SparseMatrix(double array[][]) {
        this(array.length, array[0].length, 0);
        for (int i = 0; i < rows; i++) {
            rowPointers[i] = nonzero;
            for (int j = 0; j < columns; j++) {
                if (Math.abs(array[i][j]) > EPS) {
                    if (values.length < nonzero + 1) {
                        growup();
                    }
                    values[nonzero] = array[i][j];
                    columnIndices[nonzero] = j;
                    nonzero++;
                }
            }
        }
        rowPointers[rows] = nonzero;
    }

    public SparseMatrix(int rows, int columns, int nonzero) {
        super(new SparseFactory());
        this.rows = rows;
        this.columns = columns;
        this.nonzero = 0;
        int alignedSize = Math.min(rows * columns, ((nonzero % MINIMUM_SIZE) + 1) * MINIMUM_SIZE);
        this.values = new double[alignedSize];
        this.columnIndices = new int[alignedSize];
        this.rowPointers = new int[rows + 1];
    }

    @Override
    public double get(int i, int j) {
        if (i >= rows || i < 0 || j >= columns || j < 0) throw new IndexOutOfBoundsException();
        for (int ii = rowPointers[i]; ii < rowPointers[i + 1]; ii++) {
            if (columnIndices[ii] == j) {
                return values[ii];
            }
        }
        return 0.0;
    }

    @Override
    public void set(int i, int j, double value) {
        if (i >= rows || i < 0 || j >= columns || j < 0) throw new IndexOutOfBoundsException();
        for (int ii = rowPointers[i]; ii < rowPointers[i + 1]; ii++) {
            if (columnIndices[ii] == j) {
                values[ii] = value;
                return;
            }
        }
        if (Math.abs(value) < EPS) return;
        if (values.length < nonzero + 1) {
            growup();
        }
        int position = rowPointers[i];
        while (position < rowPointers[i + 1] && j >= columnIndices[position]) position++;
        for (int k = nonzero; k > position; k--) {
            values[k] = values[k - 1];
            columnIndices[k] = columnIndices[k - 1];
        }
        values[position] = value;
        columnIndices[position] = j;
        for (int k = i + 1; k < rows + 1; k++) {
            rowPointers[k]++;
        }
        nonzero++;
    }

    @Override
    public void resize(int rows, int columns) {
        if (rows < 0 || columns < 0) throw new IllegalArgumentException();
        if (this.rows == rows && this.columns == columns) return;
        if (this.rows >= rows && this.columns >= columns) {
            int position = 0;
            for (int k = 0; k < rowPointers[rows]; k++) {
                if (columns > columnIndices[k]) {
                    values[position++] = values[k];
                }
            }
            nonzero = rowPointers[rows];
        } else if (this.rows < rows) {
            int newRowPointers[] = new int[rows + 1];
            System.arraycopy(rowPointers, 0, newRowPointers, 0, rowPointers.length);
            for (int i = this.rows; i < rows; i++) {
                newRowPointers[i] = nonzero;
            }
            this.rowPointers = newRowPointers;
        }
        this.rows = rows;
        this.columns = columns;
    }

    @Override
    public double[][] toArray() {
        double result[][] = new double[rows][columns];
        int k = 0, i = 0;
        while (k < nonzero) {
            for (int j = rowPointers[i]; j < rowPointers[i + 1]; j++, k++) {
                result[i][columnIndices[j]] = values[j];
            }
            i++;
        }
        return result;
    }

    @Override
    public double[][] toArrayCopy() {
        return toArray();
    }

    @Override
    public void swapRows(int i, int j) {
        if (i >= rows || i < 0 || j >= rows || j < 0) throw new IndexOutOfBoundsException();
        if (i == j) return;
        Vector ii = getRow(i);
        Vector jj = getRow(j);
        setRow(i, jj);
        setRow(j, ii);
    }

    @Override
    public void swapColumns(int i, int j) {
        if (i >= columns || i < 0 || j >= columns || j < 0) throw new IndexOutOfBoundsException();
        if (i == j) return;
        Vector ii = getColumn(i);
        Vector jj = getColumn(j);
        setColumn(i, jj);
        setColumn(j, ii);
    }

    @Override
    public int nonzero() {
        return nonzero;
    }

    @Override
    public Matrix transpose(Factory factory) {
        if (factory == null) throw new NullPointerException();
        Matrix result = factory.createMatrix(columns, rows);
        int k = 0, i = 0;
        while (k < nonzero) {
            for (int j = rowPointers[i]; j < rowPointers[i + 1]; j++, k++) {
                result.set(columnIndices[j], i, values[j]);
            }
            i++;
        }
        return result;
    }

    @Override
    public Vector getRow(int i) {
        if (i >= rows || i < 0) throw new IndexOutOfBoundsException();
        Vector result = factory.createVector(columns);
        for (int ii = rowPointers[i]; ii < rowPointers[i + 1]; ii++) {
            result.set(columnIndices[ii], values[ii]);
        }
        return result;
    }

    @Override
    public Vector getColumn(int i) {
        if (i >= columns || i < 0) throw new IndexOutOfBoundsException();
        Vector result = factory.createVector(rows);
        int k = 0, ii = 0;
        while (k < nonzero) {
            for (int jj = rowPointers[ii]; jj < rowPointers[ii + 1]; jj++, k++) {
                if (columnIndices[jj] == i) {
                    result.set(ii, values[jj]);
                }
            }
            ii++;
        }
        return result;
    }

    @Override
    public void setColumn(int i, Vector column) {
        if (i >= columns || i < 0) throw new IndexOutOfBoundsException();
        for (int ii = 0; ii < column.length(); ii++) {
            int position = rowPointers[ii], limit = rowPointers[ii + 1];
            while (position < limit && columnIndices[position] < i) position++;
            if (Math.abs(column.get(ii)) > EPS) {
                if (columnIndices[position] != i) {
                    if (values.length < nonzero + 1) {
                        growup();
                    }
                    for (int k = nonzero; k > position; k--) {
                        values[k] = values[k - 1];
                        columnIndices[k] = columnIndices[k - 1];
                    }
                }
                for (int k = ii + 1; k < rows + 1; k++) {
                    rowPointers[k]++;
                }
                values[position] = column.get(ii);
                columnIndices[position] = i;
                nonzero++;
            } else if (columnIndices[position] == i && position < limit) {
                for (int k = position; k < nonzero - 1; k++) {
                    values[k] = values[k + 1];
                    columnIndices[k] = columnIndices[k + 1];
                }
                for (int k = ii + 1; k < rows + 1; k++) {
                    rowPointers[k]--;
                }
                nonzero--;
            }
        }
    }

    @Override
    public void setRow(int i, Vector row) {
        if (i >= rows || i < 0) throw new IndexOutOfBoundsException();
        int position = rowPointers[i], limit = rowPointers[i + 1];
        rowPointers[i] = limit;
        for (int ii = 0; ii < row.length(); ii++) {
            if (Math.abs(row.get(ii)) > EPS) {
                if (position >= limit) {
                    if (values.length < nonzero + 1) {
                        growup();
                    }
                    for (int k = nonzero; k > position; k--) {
                        values[k] = values[k - 1];
                        columnIndices[k] = columnIndices[k - 1];
                    }
                    nonzero++;
                } else {
                    rowPointers[i]--;
                }
                values[position] = row.get(ii);
                columnIndices[position] = ii;
                position++;
            }
        }
        if (limit > position) {
            nonzero -= (limit - position);
            for (int k = position; k < nonzero; k++) {
                values[k] = values[k + (limit - position)];
                columnIndices[k] = columnIndices[k + (limit - position)];
            }
            rowPointers[i] -= (limit - position);
        }
        for (int k = i + 1; k < rows + 1; k++) {
            rowPointers[k] += (position - limit);
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(rows);
        out.writeInt(columns);
        out.writeInt(nonzero);
        out.writeByte(META_MARKER);
        int k = 0, i = 0;
        while (k < nonzero) {
            for (int j = rowPointers[i]; j < rowPointers[i + 1]; j++, k++) {
                out.writeInt(i);
                out.writeInt(columnIndices[j]);
                out.writeDouble(values[j]);
                out.writeByte(ELEMENT_MARKER);
            }
            i++;
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        rows = in.readInt();
        columns = in.readInt();
        nonzero = in.readInt();
        in.readByte();
        int alignedSize = Math.min(rows * columns, ((nonzero % MINIMUM_SIZE) + 1) * MINIMUM_SIZE);
        values = new double[alignedSize];
        columnIndices = new int[alignedSize];
        rowPointers = new int[rows + 1];
        for (int k = 0; k < nonzero; k++) {
            int i = in.readInt();
            columnIndices[k] = in.readInt();
            values[k] = in.readDouble();
            in.readByte();
            rowPointers[i + 1] = k + 1;
        }
    }

    @Override
    public Matrix clone() {
        SparseMatrix dolly = (SparseMatrix) super.clone();
        dolly.values = values.clone();
        dolly.columnIndices = columnIndices.clone();
        dolly.rowPointers = rowPointers.clone();
        return dolly;
    }

    private void growup() {
        int newSize = Math.min(rows * columns, (nonzero * 3) / 2 + 1);
        double newValues[] = new double[newSize];
        int newColumnIndices[] = new int[newSize];
        System.arraycopy(values, 0, newValues, 0, nonzero);
        System.arraycopy(columnIndices, 0, newColumnIndices, 0, nonzero);
        this.values = newValues;
        this.columnIndices = newColumnIndices;
    }
}
