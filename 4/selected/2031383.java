package com.rbnb.plot;

public class Channel {

    public static final byte MSB = com.rbnb.api.DataBlock.ORDER_MSB;

    public static final byte LSB = com.rbnb.api.DataBlock.ORDER_LSB;

    public String channelName = null;

    public short channelUserDataType = 0;

    public byte[] channelUserData = null;

    public byte[] data = null;

    private byte[][] dataByteArray = null;

    private String[] dataString = null;

    private String mimeType;

    private byte[] dataInt8 = null;

    private short[] dataInt16 = null;

    private int[] dataInt32 = null;

    private long[] dataInt64 = null;

    private float[] dataFloat32 = null;

    private double[] dataFloat64 = null;

    public DataTimeStamps timeStamp = null;

    public int numberOfPoints = 0;

    public short pointSize = 0;

    public DataTimeStamps frames = null;

    public boolean isByteArray = false;

    public boolean isString = false;

    public boolean isInt8 = false;

    public boolean isInt16 = false;

    public boolean isInt32 = false;

    public boolean isInt64 = false;

    public boolean isFloat32 = false;

    public boolean isFloat64 = false;

    public short byteOrder = MSB;

    public Channel() {
    }

    public Channel(String name, String userinfo, String mime) {
        this(name, userinfo);
        mimeType = mime;
    }

    public Channel(String name, String userinfo) {
        this(name);
        if (userinfo != null) {
            channelUserDataType = 1;
            channelUserData = userinfo.getBytes();
        }
    }

    public Channel(String name) {
        if ((name != null) && (name.length() >= 1) && (name.charAt(0) == '/')) {
            name = name.substring(1);
        }
        channelName = name;
    }

    public String toString() {
        return channelName;
    }

    public void clear() {
        numberOfPoints = 0;
        pointSize = 0;
        data = null;
        dataInt16 = null;
        dataInt32 = null;
        dataInt64 = null;
        dataFloat32 = null;
        dataFloat64 = null;
        dataByteArray = null;
        dataString = null;
        isInt8 = false;
        isInt16 = false;
        isInt32 = false;
        isInt64 = false;
        isFloat32 = false;
        isFloat64 = false;
        isByteArray = false;
        isString = false;
    }

    public void setName(String name) {
        channelName = name;
    }

    public void setTimeStamp(DataTimeStamps ts) {
        timeStamp = ts;
    }

    public DataTimeStamps getTimeStamp() {
        return timeStamp;
    }

    public int getNumberOfPoints() {
        return numberOfPoints;
    }

    public byte getByteOrder() {
        return MSB;
    }

    public short getPointSize() {
        return pointSize;
    }

    public String getChannelName() {
        return channelName;
    }

    public boolean equals(Object o) {
        if (o instanceof Channel) {
            Channel c = (Channel) o;
            if (c.channelName.equals(channelName)) return true;
        }
        return false;
    }

    public void setDataInt8(byte[] inData) {
        dataInt8 = inData;
        numberOfPoints = inData.length;
        pointSize = 1;
        isInt8 = true;
        data = new byte[0];
    }

    public byte[] getDataInt8() {
        return dataInt8;
    }

    public void setDataByteArray(byte[][] inData, String mime) {
        dataByteArray = inData;
        numberOfPoints = inData.length;
        pointSize = -1;
        isByteArray = true;
        data = new byte[0];
        mimeType = mime;
    }

    public byte[][] getDataByteArray() {
        return dataByteArray;
    }

    public void setDataString(String[] inData, String mime) {
        dataString = inData;
        numberOfPoints = inData.length;
        pointSize = 1;
        isString = true;
        mimeType = mime;
        data = new byte[0];
    }

    public String[] getDataString() {
        return dataString;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setDataInt16(short[] inData) {
        dataInt16 = inData;
        numberOfPoints = inData.length;
        pointSize = 2;
        isInt16 = true;
        data = new byte[0];
    }

    public short[] getDataInt16() {
        return dataInt16;
    }

    public void setDataInt32(int[] inData) {
        dataInt32 = inData;
        numberOfPoints = inData.length;
        pointSize = 4;
        isInt32 = true;
        data = new byte[0];
    }

    public int[] getDataInt32() {
        return dataInt32;
    }

    public void setDataInt64(long[] inData) {
        double[] d = new double[inData.length];
        for (int ii = 0; ii < inData.length; ++ii) d[ii] = (double) inData[ii];
        setDataFloat64(d);
    }

    public long[] getDataInt64() {
        return dataInt64;
    }

    public void setDataFloat32(float[] inData) {
        dataFloat32 = inData;
        numberOfPoints = inData.length;
        pointSize = 4;
        isFloat32 = true;
        data = new byte[0];
    }

    public float[] getDataFloat32() {
        return dataFloat32;
    }

    public void setDataFloat64(double[] inData) {
        dataFloat64 = inData;
        numberOfPoints = inData.length;
        pointSize = 8;
        isFloat64 = true;
        data = new byte[0];
    }

    public double[] getDataFloat64() {
        return dataFloat64;
    }
}
