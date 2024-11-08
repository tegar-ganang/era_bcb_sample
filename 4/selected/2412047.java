package green.search.lsi.matrix;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileSparseMatrixElement {

    private static final int BUFF_NUM = 1024;

    private static final int BUFF_STEP = 1024 * 128;

    private static final int INT_SIZE = (Integer.SIZE / 8);

    private static final int DOUBLE_SIZE = (Double.SIZE / 8);

    int datasize = -1;

    /** �s��̊i�[�f�B���N�g�� */
    File dir;

    /** �s��̎��ʎq */
    private String idf;

    private String ptrstr;

    private File f;

    private FileChannel fc;

    private ByteBuffer bf;

    private long indexStep = -1;

    private long fileStep = 0;

    int vals = 0;

    /**
	 * @param dir
	 * @param idf
	 * @param ptrstr
	 * @param datasize
	 * @throws IOException
	 */
    FileSparseMatrixElement(File dir, String idf, String ptrstr, int datasize) throws IOException {
        this.dir = dir;
        this.idf = idf;
        this.ptrstr = ptrstr;
        this.datasize = datasize;
        init();
    }

    /**
	 * @throws IOException
	 */
    private void init() throws IOException {
        if (!this.dir.exists()) {
            this.dir.mkdir();
        }
        open();
    }

    /**
	 * @throws IOException
	 */
    private void open() throws IOException {
        try {
            this.fc = getFileToChannel(fileStep);
            this.bf = ByteBuffer.allocate(datasize * BUFF_NUM);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * @param num
	 * @return
	 * @throws IOException
	 */
    private FileChannel getFileToChannel(long num) throws IOException {
        this.f = new File(dir.getPath() + "/" + this.idf + this.ptrstr + "_" + num);
        FileChannel fc = new RandomAccessFile(f, "rw").getChannel();
        fc.force(true);
        return fc;
    }

    /**
	 * @return
	 * @throws IOException
	 */
    public int getCountCols() throws IOException {
        int cnt = 0;
        int bcnt = 0;
        int ret_d = 0;
        for (int i = 0; i <= this.fileStep; i++) {
            fc.close();
            fc = getFileToChannel(i);
            while (fc.read(bf) != -1) {
                bf.flip();
                while (bf.hasRemaining()) {
                    cnt++;
                    int ind = bf.getInt();
                    if (ret_d < ind) {
                        ret_d = ind;
                    }
                }
                bf.clear();
                bcnt++;
            }
        }
        this.vals = cnt;
        return ret_d + 1;
    }

    /**
	 * @return
	 * @throws IOException
	 */
    public int getCountRows() throws IOException {
        int cnt = 0;
        int bcnt = 0;
        for (int i = 0; i <= this.fileStep; i++) {
            fc.close();
            fc = getFileToChannel(i);
            while (fc.read(bf) != -1) {
                bf.flip();
                while (bf.hasRemaining()) {
                    int ptr = bf.getInt();
                    cnt++;
                }
                bf.clear();
                bcnt++;
            }
        }
        return cnt - 1;
    }

    /**
	 * @return
	 */
    public int getCountVals() {
        return this.vals;
    }

    /**
	 * @param index
	 * @return
	 * @throws IOException
	 */
    public int getIntValue(long index) throws IOException {
        int idx = readReady(index * INT_SIZE);
        int res = bf.getInt(idx);
        return res;
    }

    /**
	 * @param index
	 * @return
	 * @throws IOException
	 */
    public double getDoubleValue(long index) throws IOException {
        int idx = readReady(index * DOUBLE_SIZE);
        double res = bf.getDouble(idx);
        return res;
    }

    /**
	 * @param position
	 * @return
	 * @throws IOException
	 */
    private int readReady(long position) throws IOException {
        long step = position / bf.capacity();
        long fstep = step / BUFF_STEP;
        long idx = position % bf.capacity();
        if (indexStep != step) {
            if (fileStep != fstep) {
                fc.close();
                fc = getFileToChannel(fstep);
                fileStep = fstep;
            }
            fc.read(bf, bf.capacity() * (step % BUFF_STEP));
            bf.clear();
            indexStep = step;
        }
        return (int) idx;
    }

    /**
	 * @param value
	 * @throws IOException
	 */
    public void storeIntValue(int value) throws IOException {
        fc = this.storeReady(bf, fc, f, ptrstr);
        bf.putInt(value);
    }

    /**
	 * @param value
	 * @throws IOException
	 */
    public void storeDoubleValue(double value) throws IOException {
        fc = this.storeReady(bf, fc, f, ptrstr);
        bf.putDouble(value);
    }

    /**
	 * @param bb
	 * @param fc
	 * @param f
	 * @param idf
	 * @return
	 * @throws IOException
	 */
    private FileChannel storeReady(ByteBuffer bb, FileChannel fc, File f, String idf) throws IOException {
        if (bb.capacity() == bb.position()) {
            bb.flip();
            fc.write(bb);
            bb.clear();
            long ps = fc.position();
            long step = ps / bb.capacity();
            long fstep = step / BUFF_STEP;
            if (ps == bb.capacity() * BUFF_STEP) {
                fc.close();
                this.fileStep += fstep;
                fc = getFileToChannel(this.fileStep);
            }
        }
        return fc;
    }

    public void flush() throws IOException {
        bf.flip();
        fc.write(bf);
        bf.clear();
    }

    /**
	 * @throws IOException
	 */
    public void close() throws IOException {
        fc.close();
    }

    /**
	 * 
	 */
    public void relese() {
        f.delete();
    }
}
