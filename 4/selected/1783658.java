package org.virbo.binarydatasource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * reader to read Vax floats, motivated by RPWS group need to read ISEE data.
 * vap+bin:file:///opt/project/isee/archive/a1977/77295.arc?recLength=880&type=vaxfloat&recOffset=20
 * @author jbf
 */
public class VaxFloat extends BufferDataSet {

    public VaxFloat(int rank, int reclen, int recoffs, int len0, int len1, int len2, int len3, ByteBuffer back) {
        super(rank, reclen, recoffs, len0, len1, len2, len3, VAX_FLOAT, back);
    }

    /**
     * convert from 4byte vax float to Java double.  See
     * ftp://202.127.24.195/pub/astrolibs/.../java/gsd/.../GSDVAXBytes.java
     * google for "vax float java nio Jenness"
     * @param buf1
     * @param offset
     * @return double value represented.
     */
    private double vaxFloatValue2(ByteBuffer buf, int offset) {
        ByteBuffer tmp = ByteBuffer.allocate(4);
        int e = ((buf.get(1 + offset) << 1) & 0xfe) | ((buf.get(0 + offset) >> 7) & 0x1);
        if (e > 2) {
            e -= 2;
            tmp.put(0, (byte) ((buf.get(1 + offset) & 0x80) | ((e >> 1) & 0x7f)));
            tmp.put(1, (byte) ((buf.get(0 + offset) & 0x7f) | ((e << 7) & 0x80)));
            tmp.put(2, (byte) (buf.get(3 + offset)));
            tmp.put(3, (byte) (buf.get(2 + offset)));
        } else if (e == 0) {
            for (int i = 0; i < 4; i++) {
                tmp.put(i, (byte) 0);
            }
        } else {
            int f = buf.get(2 + offset) | (buf.get(3 + offset) << 8) | ((buf.get(0 + offset) & 0x7f) << 16) | (0x1 << 23);
            f = f >> (3 - e);
            tmp.put(0, (byte) (buf.get(1 + offset) & 0x80));
            tmp.put(1, (byte) ((f >> 16) & 0x7f));
            tmp.put(2, (byte) ((f >> 8) & 0xff));
            tmp.put(3, (byte) (f & 0xff));
        }
        return tmp.getFloat();
    }

    public double value() {
        return vaxFloatValue2(back, offset());
    }

    public double value(int i0) {
        return vaxFloatValue2(back, offset(i0));
    }

    public double value(int i0, int i1) {
        return vaxFloatValue2(back, offset(i0, i1));
    }

    public double value(int i0, int i1, int i2) {
        return vaxFloatValue2(back, offset(i0, i1, i2));
    }

    public double value(int i0, int i1, int i2, int i3) {
        return vaxFloatValue2(back, offset(i0, i1, i2, i3));
    }

    public static void main(String[] args) throws FileNotFoundException, IOException {
        File f = new File("/opt/project/isee/archive/a1977/77295.arc");
        FileChannel fc = new FileInputStream(f).getChannel();
        ByteBuffer buf = fc.map(FileChannel.MapMode.READ_ONLY, 0, 10000);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        VaxFloat vf = new VaxFloat(1, 880, 12, 10, 1, 1, 1, buf);
        System.err.println(vf.value(0));
        Int i = new Int(1, 880, 0, 10, 1, 1, 1, buf);
        System.err.println(i.value(0));
    }

    public void putValue(double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void putValue(int i0, double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void putValue(int i0, int i1, double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void putValue(int i0, int i1, int i2, double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void putValue(int i0, int i1, int i2, int i3, double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
