package net.kano.joscar.flap;

import net.kano.joscar.BinaryTools;
import net.kano.joscar.ByteBlock;
import net.kano.joscar.DefensiveTools;
import java.io.IOException;
import java.io.InputStream;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the first six bytes of a FLAP packet, the FLAP "header," which
 * contains a sequence number, channel, and data length.
 */
final class FlapHeader {

    /**
     * The first byte of every FLAP command, as defined by the protocol.
     */
    private static final int PARITY_BYTE = 0x2a;

    /**
     * The sequence number of this FLAP header.
     */
    private final int seqnum;

    /**
     * The channel this FLAP header was read on.
     */
    private final int channel;

    /**
     * The length of the data to follow this header.
     */
    private final int length;

    /**
     * Reads a FLAP header from the given input stream, blocking until either
     * a full header has been read, at least the first byte of an invalid FLAP
     * header has been read, the end of the given stream has been reached, or
     * another I/O error occurs. If the end of the stream is reached and no
     * exception has been thrown, <code>null</code> is returned.
     *
     * @param in the stream from which to read the FLAP header
     * @return a FLAP header read from the given stream, or <code>null</code> if
     *         the end of the stream was reached with no errors
     *
     * @throws IOException if an I/O error occurs
     * @throws InvalidFlapHeaderException if an invalid FLAP header is received
     *         from the given stream
     */
    @Nullable
    public static FlapHeader readFLAPHeader(InputStream in) throws InvalidFlapHeaderException, IOException {
        DefensiveTools.checkNull(in, "in");
        final byte[] header = new byte[6];
        int pos = 0;
        boolean paritied = false;
        while (pos < header.length) {
            final int count = in.read(header, pos, header.length - pos);
            if (count == -1) {
                return null;
            }
            pos += count;
            if (!paritied && pos >= 1) {
                paritied = true;
                if (header[0] != PARITY_BYTE) {
                    throw new InvalidFlapHeaderException("first byte of FLAP " + "header must be 0x" + Integer.toHexString(PARITY_BYTE) + ", was 0x" + Integer.toHexString(header[0]));
                }
            }
        }
        return new FlapHeader(ByteBlock.wrap(header));
    }

    /**
     * Creates a new <code>FlapHeader</code> from the given block of six bytes.
     *
     * @param bytes the byte block from which to read the FLAP header
     * @throws IllegalArgumentException if the length of the given block is not
     *         six or if the block does not contain a valid FLAP header
     */
    FlapHeader(ByteBlock bytes) throws IllegalArgumentException {
        DefensiveTools.checkNull(bytes, "bytes");
        if (bytes.getLength() != 6) {
            throw new IllegalArgumentException("FLAP header length (" + bytes.getLength() + ") must be 6");
        }
        if (bytes.get(0) != PARITY_BYTE) {
            throw new IllegalArgumentException("FLAP command must begin " + "with 0x2a (started with 0x" + Integer.toHexString(((int) bytes.get(0)) & 0xff) + "): " + BinaryTools.describeData(bytes.subBlock(0, 6)) + " (data: " + (bytes.getLength() - 6) + " bytes)");
        }
        channel = BinaryTools.getUByte(bytes, 1);
        seqnum = BinaryTools.getUShort(bytes, 2);
        length = BinaryTools.getUShort(bytes, 4);
    }

    /**
     * Returns the sequence number of this FLAP header.
     * @return the sequence number in this FLAP header
     */
    public final int getSeqnum() {
        return seqnum;
    }

    /**
     * Returns the FLAP channel on which this FLAP header was received.
     * @return the FLAP channel on which this FLAP header was received
     */
    public final int getChannel() {
        return channel;
    }

    /**
     * Returns the data length value contained in this FLAP header.
     * @return the prescribed length of the FLAP data to follow this header
     */
    public final int getDataLength() {
        return length;
    }

    public String toString() {
        return "FlapHeader: " + "seqnum=" + seqnum + ", channel=" + channel + ", length=" + length;
    }
}
