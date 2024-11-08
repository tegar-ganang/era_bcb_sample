package dbaccess.util;

import java.io.*;
import java.util.zip.*;

/** * This class is for converting data arrays into byte arrays for storage * in a database blob and for convertions back into the data array from * blobs retrieved from the database.  JDBC uses byte arrays to move data * to and from a database blob. * <p> * <b>NOTE: Currently, compression, scaling and offsets are not implemented.</b> * <p> * The <i>makeBlob(XXX[] data)</i> methods, where XXX is any * java base data type, are used to convert a data array into a byte array * for easy insertion into the blob field in an SQL statement.  The data * type is stored in the blob. * The <i>getXXXArray(byte[] blob)</i> methods, where XXX is * any java base data type,  are used to convert a byte array into a data * array after extracting the blob from the database.  You can return a * data array as any type even though the data was stored as a different type. * <p> * Method <i>setRoundOff()</i> can be used before making a call to a * <i>getXXXArray()</i> to control whether round off should be done when * returning a floating point array stored in a blob as an integer array. * Method <i>setCompress(true)</i> can be used before making a call to a * <i>makeBlob()</i> to control whether compression should be done on the * blob. * <p> * Example usage for updating an array in the database of data type integer: * <p> * <pre> *     // Create prepared statement *     String sql ="update dataTable SET dataCol=? WHERE ..."; *     PreparedStatement stmtUpdate = connection.getConnection().prepareStatement(sql); * *     ArrayBlob ab = new ArrayBlob(); *     int[] data; *     ... *     ... initialize data array *     ... *     byte[] blob = ab.makeBlob(int[] data); *     stmtUpdate.setString(1,blob); *     stmtUpdate.executeUpdate(); * </pre> * <p> * Example usage for retrieving an array from the database of data type integer: * <p> * <pre> *     String sql = "SELECT dataCol FROM dataTable WHERE ..."; * *     // Execute the query *     ResultSet rs = stmt.executeQuery(sql); * *     ArrayBlob ab = new ArrayBlob(); * *     // retrieve the results *     while (rs.next()) { *        blob=rs.getBytes("dataCol"); *        int[] data = ab.getIntArray(blob); *        ... *        ... use data array *        ... *     } *     rs.close(); * </pre> * <p> */
public class ArrayBlob {

    boolean Compress;

    boolean RoundOff;

    float scale;

    float offset;

    /**   * Create a blob.   */
    public ArrayBlob() {
        Compress = false;
        RoundOff = true;
        scale = 1.0f;
        offset = 0.0f;
    }

    /**   * Set whether compression should be done.   * @param cmp True for compression;  false for no compression.   */
    public void setCompress(boolean cmp) {
        Compress = cmp;
    }

    /**   * Set whether round off should be done when converting floats to ints.   * @param round True to round off;  false will truncate.   */
    public void setRoundOff(boolean round) {
        RoundOff = round;
    }

    /**   * Sets the default scaling factor   * @param scaleFactor Default scale factor for each parmater in the list   */
    public void setScale(float scaleFactor) {
        scale = scaleFactor;
    }

    /**   * Sets the default offset amount   * @param offsetAmount Default offset amount for each parmater in the list   */
    public void setOffset(float offsetAmount) {
        offset = offsetAmount;
    }

    /**   * Create a data blob from an array of data.  The blob will be ready for   * insertion into a blob data type in the database.   * @param The data array.   * @return An array of bytes.   */
    public byte[] makeBlob(byte[] data) {
        byte[] blob = new byte[data.length + 1];
        blob[0] = (byte) 'b';
        for (int i = 0; i < data.length; i++) {
            blob[i + 1] = data[i];
        }
        return blob;
    }

    /**   * Create a data blob from an array of data.  The blob will be ready for   * insertion into a blob data type in the database.   * @param The data array.   * @return An array of bytes.   */
    public byte[] makeBlob(short[] data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeByte('s');
        for (int i = 0; i < data.length; i++) {
            dos.writeShort(data[i]);
        }
        return baos.toByteArray();
    }

    /**   * Create a data blob from an array of data.  The blob will be ready for   * insertion into a blob data type in the database.   * @param The data array.   * @return An array of bytes.   */
    public byte[] makeBlob(int[] data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeByte('i');
        for (int i = 0; i < data.length; i++) {
            dos.writeInt(data[i]);
        }
        return baos.toByteArray();
    }

    /**   * Create a data blob from an array of data.  The blob will be ready for   * insertion into a blob data type in the database.   * @param The data array.   * @return An array of bytes.   */
    public byte[] makeBlob(long[] data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeByte('l');
        for (int i = 0; i < data.length; i++) {
            dos.writeLong(data[i]);
        }
        return baos.toByteArray();
    }

    /**   * Create a data blob from an array of data.  The blob will be ready for   * insertion into a blob data type in the database.   * @return The data array converted to an array of bytes.   */
    public byte[] makeBlob(float[] data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeByte('f');
        for (int i = 0; i < data.length; i++) {
            dos.writeFloat(data[i]);
        }
        return baos.toByteArray();
    }

    /**   * Create a data blob from an array of data.  The blob will be ready for   * insertion into a blob data type in the database.   * @return The data array converted to an array of bytes.   */
    public byte[] makeBlob(double[] data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeByte('d');
        for (int i = 0; i < data.length; i++) {
            dos.writeDouble(data[i]);
        }
        return baos.toByteArray();
    }

    /**   * Create a data blob from an array of data.  The blob will be ready for   * insertion into a blob data type in the database.   * @param An array of bytes.   * @return The data array.   */
    public byte[] getByteArray(byte[] blob) {
        byte[] dataArray = new byte[blob.length - 1];
        for (int i = 0; i < blob.length - 1; i++) {
            dataArray[i] = blob[i + 1];
        }
        return dataArray;
    }

    /**   * Create a data blob from an array of data.  The blob will be ready for   * insertion into a blob data type in the database.   * @param An array of bytes.   * @return The data array.   */
    public short[] getShortArray(byte[] blob) {
        ByteArrayInputStream bais = new ByteArrayInputStream(blob);
        DataInputStream dis = new DataInputStream(bais);
        byte type = 0;
        try {
            type = dis.readByte();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        int elementSize = 0;
        switch(type) {
            case 'b':
                {
                    elementSize = 1;
                    break;
                }
            case 's':
                {
                    elementSize = 2;
                    break;
                }
            case 'i':
                {
                    elementSize = 4;
                    break;
                }
            case 'l':
                {
                    elementSize = 4;
                    break;
                }
            case 'f':
                {
                    elementSize = 4;
                    break;
                }
            case 'd':
                {
                    elementSize = 8;
                    break;
                }
        }
        int arraySize = (blob.length - 1) / elementSize;
        short[] dataArray = new short[arraySize];
        try {
            switch(type) {
                case 'b':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            dataArray[i] = (short) dis.readByte();
                        }
                        break;
                    }
                case 's':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            dataArray[i] = dis.readShort();
                        }
                        break;
                    }
                case 'i':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            dataArray[i] = (short) dis.readInt();
                        }
                        break;
                    }
                case 'l':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            dataArray[i] = (short) dis.readLong();
                        }
                        break;
                    }
                case 'f':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            float fdata = dis.readFloat();
                            if (RoundOff) {
                                if (fdata >= 0.0f) {
                                    fdata += .5f;
                                } else {
                                    fdata -= .5f;
                                }
                            }
                            dataArray[i] = (short) fdata;
                        }
                        break;
                    }
                case 'd':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            double ddata = dis.readDouble();
                            if (RoundOff) {
                                if (ddata >= 0.0) {
                                    ddata += .5;
                                } else {
                                    ddata -= .5;
                                }
                            }
                            dataArray[i] = (short) ddata;
                        }
                        break;
                    }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return dataArray;
    }

    /**   * Create a data blob from an array of data.  The blob will be ready for   * insertion into a blob data type in the database.   * @param An array of bytes.   * @return The data array.   */
    public int[] getIntArray(byte[] blob) {
        ByteArrayInputStream bais = new ByteArrayInputStream(blob);
        DataInputStream dis = new DataInputStream(bais);
        byte type = 0;
        try {
            type = dis.readByte();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        int elementSize = 0;
        switch(type) {
            case 'b':
                {
                    elementSize = 1;
                    break;
                }
            case 's':
                {
                    elementSize = 2;
                    break;
                }
            case 'i':
                {
                    elementSize = 4;
                    break;
                }
            case 'l':
                {
                    elementSize = 4;
                    break;
                }
            case 'f':
                {
                    elementSize = 4;
                    break;
                }
            case 'd':
                {
                    elementSize = 8;
                    break;
                }
        }
        int arraySize = (blob.length - 1) / elementSize;
        int[] dataArray = new int[arraySize];
        try {
            switch(type) {
                case 'b':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            dataArray[i] = (int) dis.readByte();
                        }
                        break;
                    }
                case 's':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            dataArray[i] = (int) dis.readShort();
                        }
                        break;
                    }
                case 'i':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            dataArray[i] = dis.readInt();
                        }
                        break;
                    }
                case 'l':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            dataArray[i] = (int) dis.readLong();
                        }
                        break;
                    }
                case 'f':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            float fdata = dis.readFloat();
                            if (RoundOff) {
                                if (fdata >= 0.0f) {
                                    fdata += .5f;
                                } else {
                                    fdata -= .5f;
                                }
                            }
                            dataArray[i] = (int) fdata;
                        }
                        break;
                    }
                case 'd':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            double ddata = dis.readDouble();
                            if (RoundOff) {
                                if (ddata >= 0.0) {
                                    ddata += .5;
                                } else {
                                    ddata -= .5;
                                }
                            }
                            dataArray[i] = (int) ddata;
                        }
                        break;
                    }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return dataArray;
    }

    /**   * Create a data blob from an array of data.  The blob will be ready for   * insertion into a blob data type in the database.   * @param An array of bytes.   * @return The data array.   */
    public long[] getLongArray(byte[] blob) {
        ByteArrayInputStream bais = new ByteArrayInputStream(blob);
        DataInputStream dis = new DataInputStream(bais);
        byte type = 0;
        try {
            type = dis.readByte();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        int elementSize = 0;
        switch(type) {
            case 'b':
                {
                    elementSize = 1;
                    break;
                }
            case 's':
                {
                    elementSize = 2;
                    break;
                }
            case 'i':
                {
                    elementSize = 4;
                    break;
                }
            case 'l':
                {
                    elementSize = 4;
                    break;
                }
            case 'f':
                {
                    elementSize = 4;
                    break;
                }
            case 'd':
                {
                    elementSize = 8;
                    break;
                }
        }
        int arraySize = (blob.length - 1) / elementSize;
        long[] dataArray = new long[arraySize];
        try {
            switch(type) {
                case 'b':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            dataArray[i] = (long) dis.readByte();
                        }
                        break;
                    }
                case 's':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            dataArray[i] = (long) dis.readShort();
                        }
                        break;
                    }
                case 'i':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            dataArray[i] = (long) dis.readInt();
                        }
                        break;
                    }
                case 'l':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            dataArray[i] = dis.readLong();
                        }
                        break;
                    }
                case 'f':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            float fdata = dis.readFloat();
                            if (RoundOff) {
                                if (fdata >= 0.0f) {
                                    fdata += .5f;
                                } else {
                                    fdata -= .5f;
                                }
                            }
                            dataArray[i] = (long) fdata;
                        }
                        break;
                    }
                case 'd':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            double ddata = dis.readDouble();
                            if (RoundOff) {
                                if (ddata >= 0.0) {
                                    ddata += .5;
                                } else {
                                    ddata -= .5;
                                }
                            }
                            dataArray[i] = (long) ddata;
                        }
                        break;
                    }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return dataArray;
    }

    /**   * Create a data blob from an array of data.  The blob will be ready for   * insertion into a blob data type in the database.   * @param An array of bytes.   * @return The data array.   */
    public float[] getFloatArray(byte[] blob) {
        ByteArrayInputStream bais = new ByteArrayInputStream(blob);
        DataInputStream dis = new DataInputStream(bais);
        byte type = 0;
        try {
            type = dis.readByte();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        int elementSize = 0;
        switch(type) {
            case 'b':
                {
                    elementSize = 1;
                    break;
                }
            case 's':
                {
                    elementSize = 2;
                    break;
                }
            case 'i':
                {
                    elementSize = 4;
                    break;
                }
            case 'l':
                {
                    elementSize = 4;
                    break;
                }
            case 'f':
                {
                    elementSize = 4;
                    break;
                }
            case 'd':
                {
                    elementSize = 8;
                    break;
                }
        }
        int arraySize = (blob.length - 1) / elementSize;
        float[] dataArray = new float[arraySize];
        try {
            switch(type) {
                case 'b':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            dataArray[i] = (float) dis.readByte();
                        }
                        break;
                    }
                case 's':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            dataArray[i] = (float) dis.readShort();
                        }
                        break;
                    }
                case 'i':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            dataArray[i] = (float) dis.readInt();
                        }
                        break;
                    }
                case 'l':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            dataArray[i] = (float) dis.readLong();
                        }
                        break;
                    }
                case 'f':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            dataArray[i] = dis.readFloat();
                        }
                        break;
                    }
                case 'd':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            dataArray[i] = (float) dis.readDouble();
                        }
                        break;
                    }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return dataArray;
    }

    /**   * Create a data blob from an array of data.  The blob will be ready for   * insertion into a blob data type in the database.   * @param An array of bytes.   * @return The data array.   */
    public double[] getDouble(byte[] blob) {
        ByteArrayInputStream bais = new ByteArrayInputStream(blob);
        DataInputStream dis = new DataInputStream(bais);
        byte type = 0;
        try {
            type = dis.readByte();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        int elementSize = 0;
        switch(type) {
            case 'b':
                {
                    elementSize = 1;
                    break;
                }
            case 's':
                {
                    elementSize = 2;
                    break;
                }
            case 'i':
                {
                    elementSize = 4;
                    break;
                }
            case 'l':
                {
                    elementSize = 4;
                    break;
                }
            case 'f':
                {
                    elementSize = 4;
                    break;
                }
            case 'd':
                {
                    elementSize = 8;
                    break;
                }
        }
        int arraySize = (blob.length - 1) / elementSize;
        double[] dataArray = new double[arraySize];
        try {
            switch(type) {
                case 'b':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            dataArray[i] = (double) dis.readByte();
                        }
                        break;
                    }
                case 's':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            dataArray[i] = (double) dis.readShort();
                        }
                        break;
                    }
                case 'i':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            dataArray[i] = (double) dis.readInt();
                        }
                        break;
                    }
                case 'l':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            dataArray[i] = (double) dis.readLong();
                        }
                        break;
                    }
                case 'f':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            dataArray[i] = (double) dis.readFloat();
                        }
                        break;
                    }
                case 'd':
                    {
                        for (int i = 0; i < arraySize; i++) {
                            dataArray[i] = dis.readDouble();
                        }
                        break;
                    }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return dataArray;
    }

    /**   * Create a data blob from an array of data.  The blob will be ready for   * insertion into a blob data type in the database.   * @return The data array converted to an array of bytes.   */
    public byte[] makeCBlob(int Nobs, int[] data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        DataOutputStream dos = null;
        if (Compress) {
            ZipEntry ze = new ZipEntry("data");
            zos.putNextEntry(ze);
            dos = new DataOutputStream(zos);
        } else {
            dos = new DataOutputStream(baos);
        }
        for (int i = 0; i < Nobs; i++) {
            dos.writeInt(data[i]);
        }
        if (Compress) {
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}
