package com.faunos.util.io;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

/**
 *
 * It's come to this&hellip; standardizing on a workaround interface for coping with
 * outstanding bugs surrounding {@link FileChannel} implementations. 
 *
 * @author Babak Farhang
 */
public class WorkAroundFileChannel extends FilterFileChannel {

    /**
     * Creates a new wrapping instance using the given (buggy) <code>inner
     * FileChannel</code> implementation.
     */
    public WorkAroundFileChannel(FileChannel inner) {
        super(inner);
    }

    /**
     * @see <a href="http://www.google.com/search?q=FileChannel+transferFrom+%22invalid+argument%22">Google bug search</a>
     */
    @Override
    public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
        final long FS_LIMIT = 0x1000000;
        if (count > FS_LIMIT) count = FS_LIMIT;
        if (src instanceof FileChannel) return tranferFromFileChannelWorkAround((FileChannel) src, position, count); else return inner.transferFrom(src, position, count);
    }

    private long tranferFromFileChannelWorkAround(FileChannel src, long position, long count) throws IOException {
        final long remaining = src.size() - src.position();
        if (remaining == 0) return 0;
        if (count > remaining) count = remaining;
        return inner.transferFrom(src, position, count);
    }
}
