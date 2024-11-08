package lab.data.matrices;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import lab.data.vectors.IMyVector;
import lab.data.vectors.LongSparseVector;
import lab.data.vectors.SparseVector;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * This is a test class. It's not necessary for the project.
 * This class can be used to create test matrices.
 * Use it from command line:
 * $ java lab.data.matrices.FSSparseMatrix size perc_of_zeros
 * 
 * Don't use this class to read the matrices (it's deprecated).
 * Use FilesFormatMatrix instead
 * @author andrei
 *
 */
public class FSSparseMatrix extends MatrixView<Number> implements IWritableMatrix<Double>, IMatrixView<Number> {

    private static final double TIMES_FACTOR = 1;

    private String _path;

    private String _matrixname;

    private boolean _isReadonly;

    private RandomAccessFile _valuesFile;

    private RandomAccessFile _rowsFile;

    private RandomAccessFile _colsFile;

    private int _writeposition;

    public FSSparseMatrix(String dirPath, String matrixName, boolean trueIfReadOnlyFalseIfWriteOnly) throws IOException {
        _path = dirPath;
        _matrixname = matrixName;
        _isReadonly = trueIfReadOnlyFalseIfWriteOnly;
        _writeposition = 0;
        Init();
    }

    protected void Init() throws IOException {
        String access_rights = _isReadonly ? "r" : "rw";
        _valuesFile = new RandomAccessFile(_path + "/" + _matrixname + ".weight", access_rights);
        _rowsFile = new RandomAccessFile(_path + "/" + _matrixname + ".nodes", access_rights);
        _colsFile = new RandomAccessFile(_path + "/" + _matrixname + ".edges", access_rights);
        int size = (int) (_rowsFile.length() / 4) - 1;
        m_height = m_width = size;
        if (!_isReadonly) {
            _rowsFile.writeInt(0);
        }
    }

    public void Cleanup() throws IOException {
        _valuesFile.close();
        _rowsFile.close();
        _colsFile.close();
    }

    @Override
    public Number getCellAt(int height, int weight) throws Exception {
        if (!_isReadonly) {
            throw new Exception("The matrix is for writing. Not allowed to read");
        }
        return this.getRow(height).get(weight);
    }

    @Override
    public IMyVector<Number> getColumn(int i) throws Exception {
        if (!_isReadonly) {
            throw new Exception("The matrix is for writing. Not allowed to read");
        }
        return this.getRow(i);
    }

    @Override
    public IMyVector<Number> getRow(int i) throws Exception {
        if (!_isReadonly) {
            throw new Exception("The matrix is for writing. Not allowed to read");
        }
        _rowsFile.seek(i * Integer.SIZE / 8);
        int offset = _rowsFile.readInt();
        int length = _rowsFile.readInt() - offset;
        IMyVector<Number> vec = new SparseVector();
        _valuesFile.seek(offset * Double.SIZE / 8);
        _colsFile.seek(offset * Integer.SIZE / 8);
        for (int k = 0; k < length; k++) {
            Double v = _valuesFile.readDouble();
            int col = _colsFile.readInt();
            vec.add(col, v);
        }
        return vec;
    }

    @Override
    protected void finalize() throws Throwable {
        Cleanup();
        super.finalize();
    }

    public void loadCommaSeparatedFile(String filepath) throws IOException {
        throw new NotImplementedException();
    }

    public void addRow(IMyVector<Double> row) throws Exception {
        if (_isReadonly) {
            throw new Exception("The matrix is for reading. Not allowed to write");
        }
        for (int i : row.getKeys()) {
            _valuesFile.writeDouble((Double) row.get(i));
            _colsFile.writeInt(i);
        }
        _writeposition += row.length();
        _rowsFile.writeInt(_writeposition);
        m_height++;
        m_width++;
    }

    /**
	 * @param argssendbuf=null;
	 */
    public static void main(String[] args) throws Exception {
        int size = 5;
        double zeros = 0.9;
        if (args.length != 3) {
            System.out.println("Usage: java FSSparseMatrix <size> <zeros> <dir>");
            System.exit(1);
        }
        size = Integer.parseInt(args[0]);
        zeros = Double.parseDouble(args[1]);
        System.out.println("Going to create it!");
        IMyVector<Double> vec = new SparseVector();
        String projPath = args[2];
        String fileName = "rand_matrix_" + size + "_" + zeros;
        FSSparseMatrix mtrx = new FSSparseMatrix(projPath, fileName, false);
        mtrx.Init();
        createBigSymmetricMatrx(mtrx, size, zeros, 1, 20);
        mtrx.Cleanup();
        System.out.println("Created!");
    }

    private static long getValueIndexInFlatVector(int i, int j, int size) {
        long l_i = (long) i;
        long l_j = (long) j;
        long l_size = (long) size;
        return ((l_i * l_size) + l_j) - ((l_i * (l_i + 1)) / 2);
    }

    private static void CreateBigSparseSymmetricMatrix(FSSparseMatrix mtrx, int size, double zero_percent, int from, int to) throws Exception {
        SubMatrixByRows<Double> m = new SubMatrixByRows<Double>(0.0);
        long numofelems = ((long) size * (long) (size - 1)) / 2;
        long numofNonZeros = Math.round(numofelems * (1.0 - zero_percent));
        long k = 0;
        int i = 0;
        int j = 0;
        double val;
        while (k < numofNonZeros) {
            i = (int) Math.round(Math.random() * (size - 1));
            j = (int) Math.round(Math.random() * (size - 1));
            val = m.getCellAt(i, j);
            if (val != 0) {
                continue;
            }
            if (i == j) {
                continue;
            }
            val = (Math.random() * (to - from)) + from;
            val = Math.round(val);
            m.setCellAt(i, j, val);
            m.setCellAt(j, i, val);
            k++;
            if (k % 1000 == 0) {
                System.out.println("Generating: " + ((double) k * 100) / numofNonZeros + "%");
            }
        }
        double sum = 0.0;
        IMyVector<Double> vec = new SparseVector();
        double diag = 0.0;
        for (i = 0; i < size; i++) {
            HashMap<Integer, Double> h = m.getRow(i);
            for (int col : h.keySet()) {
                val = h.get(col);
                vec.add(col, val);
                sum += val;
            }
            diag = (sum + 1) * TIMES_FACTOR;
            vec.add(i, diag);
            mtrx.addRow(vec);
            vec.clear();
            sum = 0;
            if (i % 1000 == 0) {
                System.out.println("Writing: " + ((double) i * 100) / size + "%");
                System.out.flush();
            }
        }
    }

    private static void createBigSymmetricMatrx(FSSparseMatrix mtrx, int size, double random, int from, int to) throws Exception {
        LongSparseVector vals = new LongSparseVector();
        long index = 0;
        for (int i = 0; i < size; i++) {
            for (int j = i; j < size; j++) {
                double rand = Math.random();
                double val = 0;
                if (rand <= random) {
                    val = 0;
                } else {
                    val = (Math.random() * (to - from)) + from;
                    val = Math.round(val);
                }
                if (i == j) {
                    val = Double.NaN;
                } else {
                    if (Double.isNaN(val)) {
                        throw new RuntimeException("Additional nan value found!");
                    }
                }
                if (val != 0.0) {
                    vals.add(index, val);
                }
                index++;
            }
            if (i % 100 == 0) {
                System.out.println(".. done with IN MEMORY " + i + " (" + (((double) i * 100.0) / size) + " %)");
            }
        }
        IMyVector<Double> vec = new SparseVector();
        for (int i = 0; i < size; i++) {
            int nanIndex = -1;
            double sum = 0;
            vec.clear();
            for (int j = 0; j < size; j++) {
                long idx;
                if (i <= j) {
                    idx = getValueIndexInFlatVector(i, j, size);
                } else {
                    idx = getValueIndexInFlatVector(j, i, size);
                }
                double val = vals.get(idx).doubleValue();
                if (Double.isNaN(val)) {
                    if (i != j) {
                        throw new Exception("Shit!!! " + i + " " + j);
                    }
                    nanIndex = j;
                } else {
                    sum += Math.abs(val);
                    if (val != 0.0) {
                        vec.add(j, val);
                    }
                }
            }
            double diagValue = (sum + 1.0) * TIMES_FACTOR;
            vec.add(nanIndex, diagValue);
            mtrx.addRow(vec);
            if (i % 100 == 0) {
                System.out.println(".. done with IN MATRIX " + i + " (" + (((double) i * 100.0) / size) + " %)");
            }
        }
    }

    private static void createSimplematrix(IMyVector<Double> vec, FSSparseMatrix mtrx, int size) throws Exception {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int val = 1;
                if (i == j) val = size + 4;
                vec.add(j, val + 0.0);
            }
            mtrx.addRow(vec);
            vec.clear();
        }
    }

    private static void create4on4Matrix(IMyVector<Double> vec, FSSparseMatrix mtrx) throws Exception {
        vec.add(0, 30.0);
        vec.add(1, 4.0);
        vec.add(3, 2.0);
        mtrx.addRow(vec);
        vec.clear();
        vec.add(0, 4.0);
        vec.add(1, 22.0);
        vec.add(2, 6.0);
        vec.add(3, 5.0);
        mtrx.addRow(vec);
        vec.clear();
        vec.add(1, 6.0);
        vec.add(2, 23.0);
        vec.add(3, 4.0);
        mtrx.addRow(vec);
        vec.clear();
        vec.add(0, 2.0);
        vec.add(1, 5.0);
        vec.add(2, 4.0);
        vec.add(3, 40.0);
        mtrx.addRow(vec);
        vec.clear();
    }

    private static void create3on3Matrix(IMyVector<Double> vec, FSSparseMatrix mtrx) throws Exception {
        vec.add(0, 10.0);
        vec.add(1, 2.0);
        vec.add(2, 3.0);
        mtrx.addRow(vec);
        vec.clear();
        vec.add(0, 2.0);
        vec.add(1, 20.0);
        vec.add(2, 6.0);
        mtrx.addRow(vec);
        vec.clear();
        vec.add(0, 3.0);
        vec.add(1, 6.0);
        vec.add(2, 30.0);
        mtrx.addRow(vec);
        vec.clear();
    }

    private static void createPaperMatrix(IMyVector<Double> vec, FSSparseMatrix mtrx) throws Exception {
        vec.add(0, 1.0);
        vec.add(1, -2.0);
        vec.add(2, 3.0);
        mtrx.addRow(vec);
        vec.clear();
        vec.add(0, -2.0);
        vec.add(1, 1.0);
        mtrx.addRow(vec);
        vec.clear();
        vec.add(0, 3.0);
        vec.add(2, 1.0);
        mtrx.addRow(vec);
        vec.clear();
    }

    private static void createPaperMatrix3(IMyVector<Double> vec, FSSparseMatrix mtrx) throws Exception {
        vec.add(0, 5.1);
        vec.add(1, -2.0);
        vec.add(2, 3.0);
        mtrx.addRow(vec);
        vec.clear();
        vec.add(0, -2.0);
        vec.add(1, 2.1);
        mtrx.addRow(vec);
        vec.clear();
        vec.add(0, 3.0);
        vec.add(2, 3.1);
        mtrx.addRow(vec);
        vec.clear();
    }

    private static void createPaperMatrix2(IMyVector<Double> vec, FSSparseMatrix mtrx) throws Exception {
        vec.add(0, -1 / 12.0);
        vec.add(1, -1 / 6.0);
        vec.add(2, 1 / 4.0);
        mtrx.addRow(vec);
        vec.clear();
        vec.add(0, -1 / 6.0);
        vec.add(1, 2 / 3.0);
        vec.add(2, 1 / 2.0);
        mtrx.addRow(vec);
        vec.clear();
        vec.add(0, 1 / 4.0);
        vec.add(1, 1 / 2.0);
        vec.add(2, 1 / 4.0);
        mtrx.addRow(vec);
        vec.clear();
    }

    @Override
    public Set<Integer> getNonZeroIndicesInColumn(int i) throws Exception {
        return getNonZeroIndicesInRow(i);
    }

    @Override
    public Set<Integer> getNonZeroIndicesInRow(int i) throws Exception {
        _rowsFile.seek(i * Integer.SIZE / 8);
        int offset = _rowsFile.readInt();
        int length = _rowsFile.readInt() - offset;
        Set<Integer> result = new HashSet<Integer>();
        _colsFile.seek(offset * Integer.SIZE / 8);
        for (int k = 0; k < length; k++) {
            result.add(_colsFile.readInt());
        }
        return result;
    }

    public void init(int firstNode, int numOfNodes) throws Exception {
    }
}
