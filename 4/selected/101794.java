package org.apache.mina.filter.stream;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import org.apache.mina.core.file.DefaultFileRegion;
import org.apache.mina.core.file.FileRegion;

/**
 * Tests {@link StreamWriteFilter}.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public class FileRegionWriteFilterTest extends AbstractStreamWriteFilterTest<FileRegion, FileRegionWriteFilter> {

    @Override
    protected FileRegionWriteFilter createFilter() {
        return new FileRegionWriteFilter();
    }

    @Override
    protected FileRegion createMessage(byte[] data) throws IOException {
        File file = File.createTempFile("mina", "unittest");
        file.deleteOnExit();
        FileChannel channel = new RandomAccessFile(file, "rw").getChannel();
        ByteBuffer buffer = ByteBuffer.wrap(data);
        channel.write(buffer);
        return new DefaultFileRegion(channel);
    }
}
