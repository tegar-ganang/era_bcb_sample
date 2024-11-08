package org.jcvi.trace.fourFiveFour.flowgram.sff;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import org.jcvi.common.util.Range;
import org.jcvi.common.util.Range.CoordinateSystem;
import org.jcvi.io.IOUtil;

/**
 * {@code SffWriter} writes Sff formated data to an OutputStream.
 * @author dkatzel
 *
 *
 */
public class SffWriter {

    /**
     * Writes the given SffCommonHeader to the given outputStream.
     * @param header the header to write.
     * @param out the {@link OutputStream} to write to.
     * @throws IOException if there is a problem writing to the {@link OutputStream}.
     * @throws NullPointerException if either parameter is null.
     */
    public static void writeCommonHeader(SFFCommonHeader header, OutputStream out) throws IOException {
        int keyLength = header.getKeySequence().length();
        int size = 31 + header.getNumberOfFlowsPerRead() + keyLength;
        int padding = SFFUtil.caclulatePaddedBytes(size);
        int headerLength = size + padding;
        out.write(SFFUtil.SFF_MAGIC_NUMBER);
        out.write(IOUtil.convertUnsignedLongToByteArray(header.getIndexOffset()));
        out.write(IOUtil.convertUnsignedIntToByteArray(header.getIndexLength()));
        out.write(IOUtil.convertUnsignedIntToByteArray(header.getNumberOfReads()));
        out.write(IOUtil.convertUnsignedShortToByteArray(headerLength));
        out.write(IOUtil.convertUnsignedShortToByteArray(keyLength));
        out.write(IOUtil.convertUnsignedShortToByteArray(header.getNumberOfFlowsPerRead()));
        out.write(SFFUtil.FORMAT_CODE);
        out.write(header.getFlow().getBytes());
        out.write(header.getKeySequence().getBytes());
        out.write(new byte[padding]);
        out.flush();
    }

    /**
     * Writes the given {@link SFFReadHeader} to the given {@link OutputStream}.
     * @param readHeader the readHeader to write.
     * @param out the {@link OutputStream} to write to.
     * @throws IOException if there is a problem writing the data
     * to the {@link OutputStream}.
     * @throws NullPointerException if either parameter is null.
     */
    public static void writeReadHeader(SFFReadHeader readHeader, OutputStream out) throws IOException {
        String name = readHeader.getName();
        final int nameLength = name.length();
        int unpaddedHeaderLength = 16 + nameLength;
        int padding = SFFUtil.caclulatePaddedBytes(unpaddedHeaderLength);
        out.write(IOUtil.convertUnsignedShortToByteArray(unpaddedHeaderLength + padding));
        out.write(IOUtil.convertUnsignedShortToByteArray(nameLength));
        out.write(IOUtil.convertUnsignedIntToByteArray(readHeader.getNumberOfBases()));
        writeClip(readHeader.getQualityClip(), out);
        writeClip(readHeader.getAdapterClip(), out);
        out.write(name.getBytes());
        out.write(new byte[padding]);
        out.flush();
    }

    public static void writeReadData(SFFReadData readData, OutputStream out) throws IOException {
        final short[] flowgramValues = readData.getFlowgramValues();
        ByteBuffer flowValues = ByteBuffer.allocate(flowgramValues.length * 2);
        for (int i = 0; i < flowgramValues.length; i++) {
            flowValues.putShort(flowgramValues[i]);
        }
        out.write(flowValues.array());
        out.write(readData.getFlowIndexPerBase());
        final String basecalls = readData.getBasecalls();
        out.write(basecalls.getBytes());
        out.write(readData.getQualities());
        int readDataLength = SFFUtil.getReadDataLength(flowgramValues.length, basecalls.length());
        int padding = SFFUtil.caclulatePaddedBytes(readDataLength);
        out.write(new byte[padding]);
        out.flush();
    }

    private static void writeClip(Range clip, OutputStream out) throws IOException {
        if (clip == null) {
            out.write(SFFUtil.EMPTY_CLIP_BYTES);
        } else {
            Range oneBasedClip = clip.convertRange(CoordinateSystem.RESIDUE_BASED);
            out.write(IOUtil.convertUnsignedShortToByteArray((int) oneBasedClip.getLocalStart()));
            out.write(IOUtil.convertUnsignedShortToByteArray((int) oneBasedClip.getLocalEnd()));
        }
    }
}
