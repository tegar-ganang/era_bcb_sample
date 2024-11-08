package com.myJava.file.delta;

import com.myJava.object.ToStringHelper;

public class DeltaReadInstruction {

    protected int writeOffset;

    private long readFrom;

    private long readTo;

    public int getWriteOffset() {
        return writeOffset;
    }

    public void setWriteOffset(int writeOffset) {
        this.writeOffset = writeOffset;
    }

    public void setReadFrom(long readFrom) {
        this.readFrom = readFrom;
    }

    public long getReadTo() {
        return readTo;
    }

    public long getReadFrom() {
        return readFrom;
    }

    public void setReadTo(long readTo) {
        this.readTo = readTo;
    }

    public String toString() {
        StringBuffer sb = ToStringHelper.init(this);
        ToStringHelper.append("ReadFrom", readFrom, sb);
        ToStringHelper.append("ReadTo", readTo, sb);
        ToStringHelper.append("Length", readTo - readFrom + 1, sb);
        ToStringHelper.append("WriteFrom", writeOffset, sb);
        ToStringHelper.append("WriteTo", writeOffset + readTo - readFrom, sb);
        return ToStringHelper.close(sb);
    }
}
