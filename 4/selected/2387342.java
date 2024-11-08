package org.eyrene.javaj.io;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * <p>Title: File Map</p>
 * <p>Description: effettua il mappaggio in memoria di un file</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: eyrene</p>
 * @author Francesco Vadicamo
 * @version 1.0
 */
public class FileMap {

    public static final FileChannel.MapMode PRIVATE = FileChannel.MapMode.PRIVATE, READ_ONLY = FileChannel.MapMode.READ_ONLY, READ_WRITE = FileChannel.MapMode.READ_WRITE;

    public final int RECORD_SIZE;

    private String file_name = null;

    private long file_size = -1;

    private MappedByteBuffer buffer = null;

    private RandomAccessFile raf = null;

    private FileChannel file_channel = null;

    private FileChannel.MapMode mode = null;

    private long num_records = -1;

    private byte[] data = null;

    protected boolean INV() {
        return file_name != null && file_size > -1 && buffer != null && mode != null && num_records > -1 && raf == null;
    }

    public FileMap(String file_name, FileChannel.MapMode mode) throws IOException {
        this(file_name, mode, 32);
    }

    public FileMap(String file_name, FileChannel.MapMode mode, int RECORD_SIZE) throws IOException {
        assert (file_name != null && mode != null && RECORD_SIZE > 0) : "PRE-CONDIZIONE VIOLATA!";
        try {
            this.file_name = file_name;
            this.raf = new RandomAccessFile(file_name, "rw");
            this.file_channel = raf.getChannel();
            this.file_size = file_channel.size();
            this.mode = mode;
            this.buffer = file_channel.map(mode, 0, file_size);
            this.RECORD_SIZE = RECORD_SIZE;
            this.num_records = file_size / RECORD_SIZE;
            this.data = new byte[RECORD_SIZE];
        } catch (FileNotFoundException fnfe) {
            throw new IOException("File '" + file_name + "' not found!");
        }
        assert (INV()) : "INVARIANTE VIOLATA!";
    }

    public String getFileName() {
        return file_name;
    }

    public long getFileSize() {
        return file_size;
    }

    public MappedByteBuffer getByteBuffer() {
        return buffer;
    }

    public RandomAccessFile getRandomAccessFile() {
        return raf;
    }

    public FileChannel getFileChannel() {
        return file_channel;
    }

    public FileChannel.MapMode getMode() {
        return mode;
    }

    public long getNumRecords() {
        return num_records;
    }

    public int getPosition() {
        return buffer.position();
    }

    public void setPositionToByte(int position) {
        buffer.position(position);
    }

    public void setPositionToRecord(int record) {
        buffer.position(record * RECORD_SIZE);
    }

    public byte[] getData() {
        buffer.get(data);
        return data;
    }

    public byte[] getData(int record) {
        setPositionToRecord(record);
        return getData();
    }

    public void setData(byte[] data) {
        buffer.put(data);
    }

    public void setData(int toRecord, byte[] data) {
        setPositionToRecord(toRecord);
        setData(data);
    }

    public void force() {
        buffer.force();
    }

    public void close() throws IOException {
        file_channel.close();
    }

    public String toBinaryString() {
        String str = "";
        org.eyrene.javaj.lang.Bits data_bits;
        for (int i = 0; i < this.getNumRecords(); i++) {
            setPositionToRecord(i);
            byte[] data_byte = this.getData();
            data_bits = new org.eyrene.javaj.lang.Bits(data_byte);
            str += "\n" + data_bits;
        }
        return str;
    }

    public static void main(String[] args) {
        try {
            String file_name = FileMap.class.getResource("FileMap.test").getPath();
            FileMap fm = new FileMap(file_name, FileMap.READ_ONLY);
            System.out.println("______________________________");
            System.out.println("FileName: " + fm.getFileName());
            System.out.println("FileSize: " + fm.getFileSize());
            System.out.println("NumRecords: " + fm.getNumRecords());
            System.out.println("______________________________\n");
            System.out.println(fm.toBinaryString());
            fm.close();
            fm = new FileMap(file_name, FileMap.READ_WRITE, 1);
            System.out.println("______________________________");
            System.out.println("FileName: " + fm.getFileName());
            System.out.println("FileSize: " + fm.getFileSize());
            System.out.println("NumRecords: " + fm.getNumRecords());
            System.out.println("______________________________\n");
            System.out.println(fm.toBinaryString());
            fm.close();
        } catch (Throwable th) {
            th.printStackTrace(System.err);
        }
    }
}
