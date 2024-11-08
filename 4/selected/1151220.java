package org.lindenb.berkeley.binding;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.lindenb.io.IOUtils;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

/**
 * helper functions for TupleBinding
 *
 */
public abstract class XTupleBinding<T> extends TupleBinding<T> {

    /** write a String, compressing it if needed */
    protected void writeGZippedString(String s, TupleOutput out) {
        writeGZippedBytes(s == null ? null : s.getBytes(), out);
    }

    /** read a String, compressing it if needed */
    protected String readGZippedString(TupleInput in) {
        byte array[] = readGZippedBytes(in);
        return (array == null ? null : new String(array));
    }

    /** write a byte array, compressing it if needed */
    protected void writeGZippedBytes(byte array[], TupleOutput out) {
        if (array == null || array.length == 0) {
            out.writeBoolean(false);
            writeBytes(array, out);
            return;
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(array.length);
            GZIPOutputStream gzout = new GZIPOutputStream(baos);
            ByteArrayInputStream bais = new ByteArrayInputStream(array);
            IOUtils.copyTo(bais, gzout);
            gzout.finish();
            gzout.close();
            bais.close();
            byte compressed[] = baos.toByteArray();
            if (compressed.length < array.length) {
                out.writeBoolean(true);
                writeBytes(compressed, out);
            } else {
                out.writeBoolean(false);
                writeBytes(array, out);
            }
        } catch (IOException err) {
            throw new RuntimeException(err);
        }
    }

    /** write an array of byte. This array will be ungzipped if needed 
	 * @param in
	 * @return
	 */
    protected byte[] readGZippedBytes(TupleInput in) {
        final boolean is_compressed = in.readBoolean();
        byte array[] = readBytes(in);
        if (array == null) return null;
        if (!is_compressed) {
            return array;
        }
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(array);
            GZIPInputStream gzin = new GZIPInputStream(bais);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(array.length);
            IOUtils.copyTo(gzin, baos);
            gzin.close();
            bais.close();
            return baos.toByteArray();
        } catch (IOException err) {
            throw new RuntimeException(err);
        }
    }

    protected void writeBytes(byte array[], TupleOutput out) {
        if (array == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(array.length);
        out.writeFast(array);
    }

    protected byte[] readBytes(TupleInput in) {
        final int len = in.readInt();
        if (len == -1) return null;
        byte array[] = new byte[len];
        int offset = 0;
        while (offset < len) {
            int n = in.readFast(array, offset, (len - offset));
            if (n == -1) throw new RuntimeException("cannot read " + (len - offset) + " bytes");
            offset += n;
        }
        return array;
    }
}
