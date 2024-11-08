package com.aliasi.lm;

import com.aliasi.io.BitOutput;
import java.io.IOException;

/**
 * A <code>BitTrieWriter</code> provides a trie writer that wraps a
 * bit-level output.  
 *
 * <p>The reader for the output of a bit trie writer is {@link
 * BitTrieReader}.
 *
 * <p>Counts of trie nodes and differences between
 * successive symbols on transitions are delta coded for compression
 * (see {@link BitOutput#writeDelta(long)}).  
 *
 * <p>The method {@link #copy(TrieReader,TrieWriter)} is available to
 * copy the contents of a reader to a writer.
 *
 * @author  Bob Carpenter
 * @version 2.3
 * @since   LingPipe2.3
 */
public class BitTrieWriter extends BitTrie implements TrieWriter {

    private final BitOutput mBitOutput;

    /**
     * Construct a bit trie writer from the specified bit output
     * with the specified maximum n-gram.
     *
     * @param bitOutput Underlying bit output.
     */
    public BitTrieWriter(BitOutput bitOutput) {
        mBitOutput = bitOutput;
    }

    public void writeCount(long count) throws IOException {
        checkCount(count);
        mBitOutput.writeDelta(count);
        pushValue(-1L);
    }

    public void writeSymbol(long symbol) throws IOException {
        if (symbol == -1L) {
            mBitOutput.writeDelta(1L);
            popValue();
        } else {
            long code = symbol - popValue() + 1L;
            mBitOutput.writeDelta(code);
            pushValue(symbol);
        }
    }

    /**
     * Copies the content of the specified trie reader to the specified
     * trie writer.
     *
     * @param reader Reader from which to read.
     * @param writer Writer to which to write.
     */
    public static void copy(TrieReader reader, TrieWriter writer) throws IOException {
        long count = reader.readCount();
        writer.writeCount(count);
        long symbol;
        while ((symbol = reader.readSymbol()) != -1L) {
            writer.writeSymbol(symbol);
            copy(reader, writer);
        }
        writer.writeSymbol(-1L);
    }
}
