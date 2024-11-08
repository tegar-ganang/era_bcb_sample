package net.sourceforge.ivi.core;

public class iviJBuffer implements IBuffer {

    public iviJBuffer() {
        d_data = new byte[0];
        d_readIdx = 0;
        d_writeIdx = 0;
    }

    public iviJBuffer(byte data[], int offset, int length) {
        d_data = new byte[0];
        d_readIdx = 0;
        d_writeIdx = 0;
        SetData(data, offset, length);
    }

    public iviJBuffer(IBuffer buffer) {
        byte data[] = buffer.GetData();
        d_data = new byte[0];
        d_readIdx = 0;
        d_writeIdx = 0;
        SetData(data, 0, data.length);
    }

    public void WriteUint32(int data) {
        check_alloc(4);
        d_data[d_writeIdx++] = (byte) (data);
        d_data[d_writeIdx++] = (byte) (data >> 8);
        d_data[d_writeIdx++] = (byte) (data >> 16);
        d_data[d_writeIdx++] = (byte) (data >> 24);
    }

    public void WriteUint16(short data) {
        check_alloc(2);
        d_data[d_writeIdx++] = (byte) (data);
        d_data[d_writeIdx++] = (byte) (data >> 8);
    }

    public void WriteUint8(byte data) {
        check_alloc(1);
        d_data[d_writeIdx++] = (byte) (data);
    }

    public void WriteString(String str) {
        check_alloc(str.length() + 1);
        for (int i = 0; i < str.length(); i++) {
            d_data[d_writeIdx++] = (byte) str.charAt(i);
        }
        d_data[d_writeIdx++] = 0;
    }

    public void WriteBuffer(IBuffer buffer, int offset, int length) {
        byte data[] = buffer.GetData();
        check_alloc(length);
        for (int i = 0; i < length; i++) {
            d_data[d_writeIdx++] = data[i + offset];
        }
    }

    public int GetLength() {
        return d_writeIdx;
    }

    public void Clear() {
        d_writeIdx = 0;
        d_readIdx = 0;
    }

    public byte[] GetData() {
        return d_data;
    }

    public void SetData(byte[] data, int offset, int length) {
        d_data = new byte[length];
        d_writeIdx = 0;
        for (int i = 0; i < length; i++) {
            d_data[d_writeIdx++] = data[i + offset];
        }
    }

    public int ReadUint32() {
        int ret = 0;
        if (d_readIdx + 4 > d_writeIdx) {
            return 0;
        }
        int tmp;
        tmp = d_data[d_readIdx++];
        ret |= (tmp & 0xFF);
        tmp = d_data[d_readIdx++];
        ret |= ((tmp & 0xFF) << 8);
        tmp = d_data[d_readIdx++];
        ret |= ((tmp & 0xFF) << 16);
        tmp = d_data[d_readIdx++];
        ret |= ((tmp & 0xFF) << 24);
        return ret;
    }

    public short ReadUint16() {
        short ret = 0;
        if (d_readIdx + 2 > d_writeIdx) {
            return 0;
        }
        short tmp;
        tmp = d_data[d_readIdx++];
        ret |= (tmp & 0xFF);
        tmp = d_data[d_readIdx++];
        ret |= ((tmp & 0xFF) << 8);
        return ret;
    }

    public byte ReadUint8() {
        if (d_readIdx + 1 > d_writeIdx) {
            return 0;
        }
        return d_data[d_readIdx++];
    }

    public String ReadString() {
        StringBuffer str = new StringBuffer();
        while (d_readIdx < d_writeIdx && d_data[d_readIdx] != 0) {
            str.append((char) d_data[d_readIdx++]);
        }
        if (d_readIdx < d_writeIdx) {
            d_readIdx++;
        }
        return str.toString();
    }

    private void check_alloc(int new_size) {
        if ((d_writeIdx + new_size) >= d_data.length) {
            byte new_data[] = new byte[d_writeIdx + new_size];
            for (int i = 0; i < d_writeIdx; i++) {
                new_data[i] = d_data[i];
            }
            d_data = new_data;
        }
    }

    /****************************************************************
	 * Private Data
	 ****************************************************************/
    private byte d_data[];

    private int d_readIdx;

    private int d_writeIdx;
}
