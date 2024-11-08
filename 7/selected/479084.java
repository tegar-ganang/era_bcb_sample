package rr;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Hashtable;
import utils.C2JUtils;
import w.CacheableDoomObject;
import w.DoomBuffer;

public class patch_t implements CacheableDoomObject {

    /** bounding box size */
    public short width, height;

    /** pixels to the left of origin */
    public short leftoffset;

    /** pixels below the origin */
    public short topoffset;

    /** This used to be an implicit array pointing to raw posts of data. 
     * TODO: get rid of it? It's never used
     * only [width] used the [0] is &columnofs[width] */
    public int[] columnofs;

    /** The ACTUAL data is here, nicely deserialized (well, almost) */
    public column_t[] columns;

    /** Added for debug aid purposes */
    public String name;

    /** Synthesizing constructor.
     * You have to provide the columns yourself, a-posteriori.
     * 
     * @param name
     * @param width
     * @param height
     * @param leftoffset
     * @param topoffset
     */
    public patch_t(String name, int width, int height, int leftoffset, int topoffset) {
        this.name = name;
        this.width = (short) width;
        this.height = (short) height;
        this.leftoffset = (short) leftoffset;
        this.columns = new column_t[width];
    }

    public patch_t() {
    }

    /** In the C code, reading is "aided", aka they know how long the header + all
     *  posts/columns actually are on disk, and only "deserialize" them when using them.
     *  Here, we strive to keep stuff as elegant and OO as possible, so each column will get 
     *  deserialized one by one. I thought about reading ALL column data as raw data, but
     *  IMO that's shit in the C code, and would be utter shite here too. Ergo, I cleanly 
     *  separate columns at the patch level (an advantage is that it's now easy to address
     *  individual columns). However, column data is still read "raw".
     */
    @Override
    public void unpack(ByteBuffer b) throws IOException {
        b.position(0);
        b.order(ByteOrder.LITTLE_ENDIAN);
        this.width = b.getShort();
        this.height = b.getShort();
        this.leftoffset = b.getShort();
        this.topoffset = b.getShort();
        this.columnofs = new int[this.width];
        this.columns = new column_t[this.width];
        C2JUtils.initArrayOfObjects(this.columns, column_t.class);
        int[] actualsizes = new int[columns.length];
        for (int i = 0; i < actualsizes.length - 1; i++) {
            actualsizes[i] = columnofs[i + 1] - columnofs[i];
        }
        DoomBuffer.readIntArray(b, this.columnofs, this.columnofs.length);
        for (int i = 0; i < this.width; i++) {
            b.position(this.columnofs[i]);
            try {
                this.columns[i].unpack(b);
            } catch (Exception e) {
                if (i == 0) this.columns[i] = getBadColumn(this.height); else this.columns[i] = this.columns[i - 1];
            }
        }
    }

    private static Hashtable<Integer, column_t> badColumns = new Hashtable<Integer, column_t>();

    private static column_t getBadColumn(int size) {
        if (badColumns.get(size) == null) {
            column_t tmp = new column_t();
            tmp.data = new byte[size + 5];
            for (int i = 3; i < size + 3; i++) {
                tmp.data[i] = (byte) (i - 3);
            }
            tmp.data[size + 4] = (byte) 0xFF;
            tmp.posts = 1;
            tmp.postofs = new int[] { 3 };
            tmp.postdeltas = new short[] { 0 };
            tmp.postlen = new short[] { (short) (size % 256) };
            badColumns.put(size, tmp);
        }
        return badColumns.get(size);
    }
}
