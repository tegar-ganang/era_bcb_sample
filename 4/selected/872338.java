package net.sf.katta.util;

import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.channels.Pipe.SinkChannel;
import java.nio.channels.Pipe.SourceChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipInputStream;
import net.sf.katta.AbstractTest;
import net.sf.katta.testutil.TestResources;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Test;

public class FileUtilTest extends AbstractTest {

    @Test
    public void testUnzipFileFile() {
        File targetFolder = _temporaryFolder.newFolder("unpacked1");
        FileUtil.unzip(TestResources.SHARD1, targetFolder);
        File segment = new File(targetFolder, "segments.gen");
        assertTrue("Unzipped local zip directly to target", segment.exists());
    }

    @Test
    public void testUnzipPathFileFileSystemBoolean() throws IOException {
        Configuration configuration = new Configuration();
        FileSystem fileSystem = FileSystem.getLocal(configuration);
        File targetFolder = _temporaryFolder.newFolder("unpacked2");
        Path zipPath = new Path(TestResources.SHARD1.getAbsolutePath());
        FileUtil.unzip(zipPath, targetFolder, fileSystem, false);
        File segment = new File(targetFolder, "segments.gen");
        assertTrue("Unzipped local zip directly to target", segment.exists());
        targetFolder = _temporaryFolder.newFolder("unpacked3");
        zipPath = new Path(TestResources.SHARD1.getAbsolutePath());
        FileUtil.unzip(zipPath, targetFolder, fileSystem, true);
        segment = new File(targetFolder, "segments.gen");
        assertTrue("Unzipped spooled local zip to target", segment.exists());
    }

    @Test
    public void testUnzipZipInputStreamFile() throws IOException {
        File targetFolder = _temporaryFolder.newFolder("unpacked4");
        final Pipe zipPipe = Pipe.open();
        final SinkChannel sink = zipPipe.sink();
        final SourceChannel source = zipPipe.source();
        final InputStream sourceIn = Channels.newInputStream(source);
        final OutputStream sourceOut = Channels.newOutputStream(sink);
        final ZipInputStream zis = new ZipInputStream(sourceIn);
        final FileInputStream fis = new FileInputStream(TestResources.SHARD1);
        final AtomicBoolean failed = new AtomicBoolean(false);
        Thread writer = new Thread() {

            @Override
            public void run() {
                try {
                    int b;
                    while ((b = fis.read()) >= 0) {
                        sourceOut.write(b);
                    }
                } catch (IOException e) {
                    System.err.println("shard transfer via pipe failed: " + e);
                    e.printStackTrace(System.err);
                    failed.set(true);
                } finally {
                    try {
                        fis.close();
                    } catch (IOException ignore) {
                    }
                    try {
                        sourceOut.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        };
        writer.start();
        FileUtil.unzip(zis, targetFolder);
        File segment = new File(targetFolder, "segments.gen");
        assertTrue("Unzipped streamed zip to target", segment.exists());
    }
}
