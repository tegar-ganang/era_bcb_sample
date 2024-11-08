package jgloss.dictionary;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Index container which stores index data in a file.
 *
 * @author Michael Koch
 */
public class FileIndexContainer implements IndexContainer {

    public static void main(String[] args) throws Exception {
        FileIndexContainer f = new FileIndexContainer(new File(args[0]), false);
        System.err.println(f.indexes);
        f.close();
    }

    /**
     * Meta data for indexes stored in the container.
     */
    protected class IndexMetaData {

        protected static final int INDEX_OFFSET = 3 * 4;

        protected long start;

        protected int type;

        protected int length;

        protected int offset;

        protected MappedByteBuffer data = null;

        public IndexMetaData(RandomAccessFile indexFile) throws IOException {
            start = indexFile.getFilePointer();
            type = indexFile.readInt();
            length = indexFile.readInt();
            offset = indexFile.readInt();
        }

        public IndexMetaData(int _type, RandomAccessFile indexFile, int dataLength) throws IOException {
            start = indexFile.getFilePointer();
            type = _type;
            length = dataLength;
            offset = INDEX_OFFSET;
            indexFile.writeInt(type);
            indexFile.writeInt(length);
            indexFile.writeInt(offset);
        }

        public long nextIndexMetaDataOffset() {
            return start + offset + length;
        }

        public long getHeaderOffset() {
            return start;
        }

        public int getType() {
            return type;
        }

        public int getDataLength() {
            return length;
        }

        public long getDataOffset() {
            return start + offset;
        }

        public ByteBuffer getIndexData(FileChannel indexFile) throws IOException {
            if (data == null) {
                data = indexFile.map(FileChannel.MapMode.READ_ONLY, getDataOffset(), getDataLength());
                data.order(indexByteOrder);
                return data;
            } else return data.duplicate();
        }

        public String toString() {
            return "Index data: " + Integer.toHexString(type) + "/" + start + "/" + length;
        }
    }

    /**
     * Standard filename extension for indexes in this format.
     */
    public static final String EXTENSION = ".index";

    private static final byte INDEXCONTAINER_HEADER_LENGTH = 4 * 4;

    private static final byte FIRST_INDEX_POINTER_OFFSET = 2 * 4;

    private static final int BIG_ENDIAN = 1;

    private static final int LITTLE_ENDIAN = 2;

    /**
     * Magic number used in the index file header.
     */
    public static final int MAGIC = 0x4a474958;

    /**
     * Version number of the index format.
     */
    public static final int VERSION = 1000;

    protected RandomAccessFile indexFile;

    protected boolean editMode;

    protected List indexes = new ArrayList(5);

    protected ByteOrder indexByteOrder;

    /**
     * Create a new file index container or open an existing file in edit or access mode.
     *
     * @exception FileNotFoundException if the index container is opened in access mode and the
     *            index file does not already exist.
     * @exception IndexException if the selected file exists but does not contain a valid index
     *            structure.
     */
    public FileIndexContainer(File _indexfile, boolean _editMode) throws FileNotFoundException, IOException, IndexException {
        this.editMode = _editMode;
        boolean indexExists = _indexfile.exists();
        indexFile = new RandomAccessFile(_indexfile, editMode ? "rw" : "r");
        if (editMode && !indexExists) createIndexFile(); else {
            readHeader();
            readIndexMetaData();
        }
    }

    public boolean hasIndex(int indexType) {
        return getIndexMetaData(indexType) != null;
    }

    public ByteBuffer getIndexData(int indexType) throws IndexException, IllegalStateException {
        if (editMode) throw new IllegalStateException();
        IndexMetaData index = getIndexMetaData(indexType);
        if (index == null) throw new IndexException("No index data of type " + indexType + " available");
        try {
            return index.getIndexData(indexFile.getChannel());
        } catch (IOException ex) {
            throw new IndexException(ex);
        }
    }

    public ByteOrder getIndexByteOrder() {
        return indexByteOrder;
    }

    public void createIndex(int indexType, ByteBuffer data) throws IndexException, IllegalStateException {
        if (!editMode) throw new IllegalStateException();
        if (hasIndex(indexType)) throw new IndexException("Index data of type " + indexType + " already exists");
        try {
            indexFile.seek(indexFile.length());
            IndexMetaData index = new IndexMetaData(indexType, indexFile, data.remaining());
            indexFile.getChannel().write(data);
            indexes.add(index);
        } catch (IOException ex) {
            throw new IndexException(ex);
        }
    }

    public boolean canAccess() {
        return !editMode;
    }

    public boolean canEdit() {
        return editMode;
    }

    public void deleteIndex(int indexType) throws IndexException, IllegalStateException {
        if (!editMode) throw new IllegalStateException();
        IndexMetaData index = getIndexMetaData(indexType);
        if (index == null) throw new IndexException("No index data of type " + indexType + " available");
        FileChannel indexChannel = indexFile.getChannel();
        try {
            long remainder = indexChannel.size() - index.nextIndexMetaDataOffset();
            if (remainder > 0) {
                indexChannel.position(index.getHeaderOffset());
                indexChannel.transferFrom(indexChannel, index.nextIndexMetaDataOffset(), remainder);
                indexChannel.truncate(index.getHeaderOffset() + remainder);
            }
            readIndexMetaData();
        } catch (IOException ex) {
            throw new IndexException(ex);
        }
    }

    public void endEditing() throws IndexException, IllegalStateException {
        if (!editMode) throw new IllegalStateException();
        editMode = false;
    }

    public void close() {
        try {
            indexFile.close();
        } catch (IOException ex) {
        }
        indexes.clear();
    }

    /**
     * Creates a new empty index file with just a header.
     */
    protected void createIndexFile() throws IOException {
        indexFile.writeInt(MAGIC);
        indexFile.writeInt(VERSION);
        indexFile.writeInt(INDEXCONTAINER_HEADER_LENGTH);
        indexByteOrder = ByteOrder.nativeOrder();
        indexFile.writeInt(indexByteOrder == ByteOrder.BIG_ENDIAN ? BIG_ENDIAN : LITTLE_ENDIAN);
    }

    protected void readHeader() throws IOException, IndexException {
        try {
            int data = indexFile.readInt();
            if (data != MAGIC) throw new IndexException("Index file does not start with magic number");
            data = indexFile.readInt();
            if (data != VERSION) throw new IndexException("Index version " + data + " not supported");
            indexFile.readInt();
            data = indexFile.readInt();
            indexByteOrder = (data == BIG_ENDIAN) ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
        } catch (EOFException ex) {
            throw new IndexException("Premature end of index file");
        }
    }

    protected void readIndexMetaData() throws IOException, IndexException {
        indexes.clear();
        try {
            indexFile.seek(FIRST_INDEX_POINTER_OFFSET);
            long indexOffset = indexFile.readInt();
            long length = indexFile.length();
            while (indexOffset < length) {
                indexFile.seek(indexOffset);
                IndexMetaData index = new IndexMetaData(indexFile);
                indexes.add(index);
                indexOffset = index.nextIndexMetaDataOffset();
            }
        } catch (EOFException ex) {
            throw new IndexException("Premature end of index file");
        }
    }

    protected IndexMetaData getIndexMetaData(int indexType) {
        for (Iterator i = indexes.iterator(); i.hasNext(); ) {
            IndexMetaData data = (IndexMetaData) i.next();
            if (data.getType() == indexType) return data;
        }
        return null;
    }
}
