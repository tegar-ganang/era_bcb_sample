package neembuu.util;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import java.util.LinkedList;
import neembuu.common.RangeArray;
import neembuu.common.RangeArrayElement;

/**
 * This is not thread safe. Use new RangeArrayVIntFile(rangeArray.copy()) instead.
 * @author Shashank Tulsyan
 */
public final class RangeArrayVIntFile {

    private RangeArray rangeArray;

    private int checksumGap = DEFAULT_CHECKSUM_GAP;

    private static final int DEFAULT_CHECKSUM_GAP = 10000;

    public RangeArrayVIntFile(RangeArray rangeArray) {
        if (rangeArray.size() > 0) if (rangeArray.get(0).starting() < 0) throw new IllegalArgumentException("RangeArray extending in negative region cannot be converted into a RangeArrayVIntChannel");
        this.rangeArray = rangeArray;
    }

    public int getChecksumGap() {
        return checksumGap;
    }

    public void setChecksumGap(int checksumGap) {
        if (checksumGap < 0) throw new IllegalArgumentException("checksumGap should be a positive integer");
        this.checksumGap = checksumGap;
    }

    public final long[] writeTo(String destPath) throws IOException {
        FileChannel fc = new RandomAccessFile(destPath, "rw").getChannel();
        long[] ret = writeTo(fc);
        fc.force(false);
        fc.close();
        return ret;
    }

    public final long[] writeTo(WritableByteChannel fc) throws IOException {
        LinkedList<Long> checksumOffsets = new LinkedList<Long>();
        long curentDataOffset = 0;
        ByteBuffer b;
        if (rangeArray.size() == 0) return new long[0];
        long previous = 0;
        for (int i = 0; i < rangeArray.size(); i++) {
            if (i % checksumGap == 0) {
                checksumOffsets.add(curentDataOffset);
                b = ByteBuffer.wrap(new byte[1]);
                curentDataOffset += b.capacity();
                fc.write(b);
                b = ByteBuffer.wrap(VIntUtils.toVIntByteArray(previous));
                curentDataOffset += b.capacity();
                fc.write(b);
            }
            b = ByteBuffer.wrap(VIntUtils.toVIntByteArray(rangeArray.get(i).starting() - previous));
            curentDataOffset += b.capacity();
            fc.write(b);
            b = ByteBuffer.wrap(VIntUtils.toVIntByteArray(rangeArray.get(i).getSize()));
            curentDataOffset += b.capacity();
            fc.write(b);
            previous = rangeArray.get(i).ending();
        }
        checksumOffsets.add(curentDataOffset);
        b = ByteBuffer.wrap(new byte[1]);
        curentDataOffset += b.capacity();
        fc.write(b);
        b = ByteBuffer.wrap(VIntUtils.toVIntByteArray(previous));
        curentDataOffset += b.capacity();
        fc.write(b);
        long[] ret = new long[checksumOffsets.size()];
        Iterator<Long> it = checksumOffsets.iterator();
        int index = 0;
        while (it.hasNext()) {
            ret[index] = it.next();
            index++;
        }
        return ret;
    }

    public final void readFrom(String srcPath) throws IOException {
        FileChannel fc = new RandomAccessFile(srcPath, "r").getChannel();
        readFrom(fc);
        fc.close();
    }

    public final void readFrom(ReadableByteChannel fc) throws IOException {
        ByteBuffer b = ByteBuffer.allocate(1024 * 100);
        boolean filled = refillBuffer(b, fc);
        if (!filled) {
            throw new IOException("There is no data available in channel");
        }
        try {
            long previous = 0, starting, ending, currentVInt;
            long checksum;
            while (b.hasRemaining()) {
                currentVInt = VIntUtils.getNextVIntAsLong(b, fc);
                if (!b.hasRemaining()) {
                    filled = refillBuffer(b, fc);
                    if (!filled) {
                        break;
                    }
                }
                if (currentVInt == VIntUtils.LONG_EQUIVALENT_OF_VINT_NULL) {
                    checksum = VIntUtils.getNextVIntAsLong(b, fc);
                    if (checksum != previous) {
                        throw new Exception("Corrupt, expected = " + checksum + " instead of=" + previous);
                    } else {
                        this.rangeArray.addAll(rangeArray);
                    }
                    continue;
                }
                starting = previous + currentVInt;
                previous = starting;
                ending = previous + VIntUtils.getNextVIntAsLong(b, fc) - 1;
                previous = ending;
                RangeArrayElement ele = new RangeArrayElement(starting, ending);
                rangeArray.add(ele);
                if (!b.hasRemaining()) {
                    filled = refillBuffer(b, fc);
                    if (!filled) {
                        break;
                    }
                }
            }
        } catch (Exception ayn) {
            ayn.printStackTrace(System.out);
        }
    }

    private boolean refillBuffer(ByteBuffer b, ReadableByteChannel readChannel) throws IOException {
        b.clear();
        int read = readChannel.read(b);
        if (read < 1) {
            return false;
        }
        b.limit(read);
        b.rewind();
        return true;
    }

    public static void main(String[] args) throws IOException {
        RangeArray test = StatePatternFile.createRangeArrayFrom("J:\\neembuu\\patterns\\deathnote_avi_audio_extract.requeststate");
        RangeArrayVIntFile booleanRangeArrayFile = new RangeArrayVIntFile(test);
        java.io.File pth = new java.io.File("J:\\neembuu\\patterns\\deathnote_avi_audio_extract.booleanrequeststate");
        long[] checkSumOffsets = booleanRangeArrayFile.writeTo(pth.toString());
        System.out.println("Checksum offsets==");
        for (int i = 0; i < checkSumOffsets.length; i++) {
            System.out.println(checkSumOffsets[i]);
        }
        System.out.println("Checksum offsets---");
        booleanRangeArrayFile = new RangeArrayVIntFile(new RangeArray());
        booleanRangeArrayFile.readFrom(pth.toString());
        System.out.println("reason for failure");
        System.out.println(test.notEqualsGetReason(booleanRangeArrayFile.rangeArray));
    }
}
