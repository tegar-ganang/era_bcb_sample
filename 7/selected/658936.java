package dbaccess.util;

import java.io.*;
import java.util.zip.*;

/**
public class ArrayBlob {

    boolean Compress;

    boolean RoundOff;

    float scale;

    float offset;

    /**
    public ArrayBlob() {
        Compress = false;
        RoundOff = true;
        scale = 1.0f;
        offset = 0.0f;
    }

    /**
    public void setCompress(boolean cmp) {
        Compress = cmp;
    }

    /**
    public void setRoundOff(boolean round) {
        RoundOff = round;
    }

    /**
    public void setScale(float scaleFactor) {
        scale = scaleFactor;
    }

    /**
    public void setOffset(float offsetAmount) {
        offset = offsetAmount;
    }

    /**
    public byte[] makeBlob(byte[] data) {
        byte[] blob = new byte[data.length + 1];
        blob[0] = (byte) 'b';
        for (int i = 0; i < data.length; i++) {
            blob[i + 1] = data[i];
        }
        return blob;
    }

    /**
    public byte[] makeBlob(short[] data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeByte('s');
        for (int i = 0; i < data.length; i++) {
            dos.writeShort(data[i]);
        }
        return baos.toByteArray();
    }

    /**
    public byte[] makeBlob(int[] data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeByte('i');
        for (int i = 0; i < data.length; i++) {
            dos.writeInt(data[i]);
        }
        return baos.toByteArray();
    }

    /**
    public byte[] makeBlob(long[] data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeByte('l');
        for (int i = 0; i < data.length; i++) {
            dos.writeLong(data[i]);
        }
        return baos.toByteArray();
    }

    /**
    public byte[] makeBlob(float[] data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeByte('f');
        for (int i = 0; i < data.length; i++) {
            dos.writeFloat(data[i]);
        }
        return baos.toByteArray();
    }

    /**
    public byte[] makeBlob(double[] data) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeByte('d');
        for (int i = 0; i < data.length; i++) {
            dos.writeDouble(data[i]);
        }
        return baos.toByteArray();
    }

    /**
    public byte[] getByteArray(byte[] blob) {
        byte[] dataArray = new byte[blob.length - 1];
        for (int i = 0; i < blob.length - 1; i++) {
            dataArray[i] = blob[i + 1];
        }
        return dataArray;
    }

    /**
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

    /**
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

    /**
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

    /**
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

    /**
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

    /**
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