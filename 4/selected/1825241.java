package net.sf.jannot.tabix;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

/**
 * Internal class for reading BAM file indexes.
 */
class TabixFileIndex {

    private static final int MAX_BINS = 37450;

    private static final int BAM_LIDX_SHIFT = 16;

    private File mFile = null;

    private FileInputStream mFileStream = null;

    private FileChannel mFileChannel = null;

    private MappedByteBuffer mFileBuffer = null;

    TabixFileIndex(final File file) {
        mFile = file;
    }

    void close() {
        closeFileStream();
    }

    /**
     * Get list of regions of BAM file that may contain SAMRecords for the given range
     * @param referenceIndex sequence of desired SAMRecords
     * @param startPos 1-based start of the desired interval, inclusive
     * @param endPos 1-based end of the desired interval, inclusive
     * @return array of pairs of virtual file positions.  Each pair is the first and last
     * virtual file position in a range that can be scanned to find SAMRecords that overlap the given
     * positions. The last position in each pair is a virtual file pointer to the first SAMRecord beyond
     * the range that may contain the indicated SAMRecords.
     */
    long[] getSearchBins(final int referenceIndex, final int startPos, final int endPos) {
        openIndex();
        seek(4);
        final int sequenceCount = readInteger();
        System.out.println("# Sequence count: " + sequenceCount);
        if (referenceIndex >= sequenceCount) {
            return null;
        }
        final BitSet regionBins = regionToBins(startPos, endPos);
        if (regionBins == null) {
            return null;
        }
        skipToSequence(referenceIndex);
        final int nIndexBins = readInteger();
        if (nIndexBins == 0) {
            return null;
        }
        List<Chunk> chunkList = new ArrayList<Chunk>();
        for (int i = 0; i < nIndexBins; i++) {
            final int indexBin = readInteger();
            final int nChunks = readInteger();
            if (regionBins.get(indexBin)) {
                for (int ci = 0; ci < nChunks; ci++) {
                    final long chunkBegin = readLong();
                    final long chunkEnd = readLong();
                    chunkList.add(new Chunk(chunkBegin, chunkEnd));
                }
            } else {
                skipBytes(16 * nChunks);
            }
        }
        if (chunkList.isEmpty()) {
            return null;
        }
        final int start = (startPos <= 0) ? 0 : startPos - 1;
        final int regionLinearBin = start >> BAM_LIDX_SHIFT;
        final int nLinearBins = readInteger();
        long minimumOffset = 0;
        if (regionLinearBin < nLinearBins) {
            skipBytes(8 * regionLinearBin);
            minimumOffset = readLong();
        }
        chunkList = optimizeChunkList(chunkList, minimumOffset);
        return convertToArray(chunkList);
    }

    /**
     * Use to get close to the unmapped reads at the end of a BAM file.
     * @return The file offset of the first record in the last linear bin, or -1
     * if there are no elements in linear bins (i.e. no mapped reads).
     */
    long getStartOfLastLinearBin() {
        openIndex();
        seek(4);
        final int sequenceCount = readInteger();
        long lastLinearIndexPointer = -1;
        for (int i = 0; i < sequenceCount; i++) {
            final int nBins = readInteger();
            for (int j1 = 0; j1 < nBins; j1++) {
                skipBytes(4);
                final int nChunks = readInteger();
                skipBytes(16 * nChunks);
            }
            final int nLinearBins = readInteger();
            if (nLinearBins > 0) {
                skipBytes(8 * (nLinearBins - 1));
                lastLinearIndexPointer = readLong();
            }
        }
        return lastLinearIndexPointer;
    }

    private void skipToSequence(final int sequenceIndex) {
        for (int i = 0; i < sequenceIndex; i++) {
            final int nBins = readInteger();
            for (int j = 0; j < nBins; j++) {
                final int bin = readInteger();
                final int nChunks = readInteger();
                skipBytes(16 * nChunks);
            }
            final int nLinearBins = readInteger();
            skipBytes(8 * nLinearBins);
        }
    }

    private List<Chunk> optimizeChunkList(final List<Chunk> chunkList, final long minimumOffset) {
        Chunk lastChunk = null;
        Collections.sort(chunkList);
        final List<Chunk> result = new ArrayList<Chunk>();
        for (final Chunk chunk : chunkList) {
            if (chunk.getChunkEnd() <= minimumOffset) {
                continue;
            }
            if (result.isEmpty()) {
                result.add(chunk);
                lastChunk = chunk;
                continue;
            }
            final long lastFileBlock = getFileBlock(lastChunk.getChunkEnd());
            final long chunkFileBlock = getFileBlock(chunk.getChunkStart());
            if (chunkFileBlock - lastFileBlock > 1) {
                result.add(chunk);
                lastChunk = chunk;
            } else {
                if (chunk.getChunkEnd() > lastChunk.getChunkEnd()) {
                    lastChunk.setChunkEnd(chunk.getChunkEnd());
                }
            }
        }
        return result;
    }

    private long[] convertToArray(final List<Chunk> chunkList) {
        final int count = chunkList.size() * 2;
        if (count == 0) {
            return null;
        }
        int index = 0;
        final long[] result = new long[count];
        for (final Chunk chunk : chunkList) {
            result[index++] = chunk.getChunkStart();
            result[index++] = chunk.getChunkEnd();
        }
        return result;
    }

    /**
     * Get candidate bins for the specified region
     * @param startPos 1-based start of target region, inclusive.
     * @param endPos 1-based end of target region, inclusive.
     * @return bit set for each bin that may contain SAMRecords in the target region.
     */
    private BitSet regionToBins(final int startPos, final int endPos) {
        final int maxPos = 0x1FFFFFFF;
        final int start = (startPos <= 0) ? 0 : (startPos - 1) & maxPos;
        final int end = (endPos <= 0) ? maxPos : (endPos - 1) & maxPos;
        if (start > end) {
            return null;
        }
        int k;
        final BitSet bitSet = new BitSet(MAX_BINS);
        bitSet.set(0);
        for (k = 1 + (start >> 26); k <= 1 + (end >> 26); ++k) bitSet.set(k);
        for (k = 9 + (start >> 23); k <= 9 + (end >> 23); ++k) bitSet.set(k);
        for (k = 73 + (start >> 20); k <= 73 + (end >> 20); ++k) bitSet.set(k);
        for (k = 585 + (start >> 17); k <= 585 + (end >> 17); ++k) bitSet.set(k);
        for (k = 4681 + (start >> 14); k <= 4681 + (end >> 14); ++k) bitSet.set(k);
        return bitSet;
    }

    private long getFileBlock(final long bgzfOffset) {
        return ((bgzfOffset >> 16L) & 0xFFFFFFFFFFFFL);
    }

    private void openIndex() {
        if (mFileBuffer != null) {
            return;
        }
        openFileStream();
        seek(0);
        final byte[] buffer = new byte[4];
        readBytes(buffer);
        if (!Arrays.equals(buffer, TabixFileConstants.BAM_INDEX_MAGIC)) {
            System.err.println("Invalid file header in BAM index " + mFile + ": " + new String(buffer));
        }
    }

    private void readBytes(final byte[] buffer) {
        mFileBuffer.get(buffer);
    }

    private int readInteger() {
        return mFileBuffer.getInt();
    }

    private long readLong() {
        return mFileBuffer.getLong();
    }

    private void skipBytes(final int count) {
        mFileBuffer.position(mFileBuffer.position() + count);
    }

    private void seek(final int position) {
        mFileBuffer.position(position);
    }

    private void openFileStream() {
        if (mFileStream != null) {
            return;
        }
        try {
            mFileStream = new FileInputStream(mFile);
            mFileChannel = mFileStream.getChannel();
            mFileBuffer = mFileChannel.map(FileChannel.MapMode.READ_ONLY, 0L, mFileChannel.size());
            mFileBuffer.order(ByteOrder.LITTLE_ENDIAN);
        } catch (IOException exc) {
            throw new RuntimeIOException(exc.getMessage(), exc);
        }
    }

    private void closeFileStream() {
        try {
            mFileBuffer = null;
            if (mFileChannel != null) {
                mFileChannel.close();
                mFileChannel = null;
            }
            if (mFileStream != null) {
                mFileStream.close();
                mFileStream = null;
            }
        } catch (IOException exc) {
            throw new RuntimeIOException(exc.getMessage(), exc);
        }
    }

    private static class Chunk implements Comparable<Chunk> {

        private long mChunkStart;

        private long mChunkEnd;

        Chunk(final long start, final long end) {
            mChunkStart = start;
            mChunkEnd = end;
        }

        long getChunkStart() {
            return mChunkStart;
        }

        void setChunkStart(final long value) {
            mChunkStart = value;
        }

        long getChunkEnd() {
            return mChunkEnd;
        }

        void setChunkEnd(final long value) {
            mChunkEnd = value;
        }

        public int compareTo(final Chunk chunk) {
            int result = Long.signum(mChunkStart - chunk.mChunkStart);
            if (result == 0) {
                result = Long.signum(mChunkEnd - chunk.mChunkEnd);
            }
            return result;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final Chunk chunk = (Chunk) o;
            if (mChunkEnd != chunk.mChunkEnd) return false;
            if (mChunkStart != chunk.mChunkStart) return false;
            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (mChunkStart ^ (mChunkStart >>> 32));
            result = 31 * result + (int) (mChunkEnd ^ (mChunkEnd >>> 32));
            return result;
        }
    }
}
