package name.huzhenbo.java.io;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Test;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * This is one of 2 orignal sources of stream, the other is ByteArrayInputStream. Others are all decrator.
 */
public class FileInputStreamTest {

    private FileInputStream is;

    @Test
    public void should_create_file_input_stream_through_file_name() throws IOException {
        is = new FileInputStream("res/input.data");
        byte[] b = new byte[is.available()];
        is.read(b, 0, is.available());
        assertEquals("《藏地密码》", new String(b, "utf-8"));
    }

    @Test
    public void should_create_through_file() throws IOException {
        is = new FileInputStream(new File("res/input.data"));
        byte[] b = new byte[is.available()];
        is.read(b, 0, is.available());
        assertEquals("《藏地密码》", new String(b, "utf-8"));
    }

    @Test
    public void should_throw_exception_if_no_file_found() throws IOException {
        try {
            is = new FileInputStream("res/notexist.file");
            fail();
        } catch (FileNotFoundException e) {
        }
    }

    @Test
    public void should_get_file_descriptor() throws IOException {
        is = new FileInputStream(new File("res/input.data"));
        FileDescriptor fd = is.getFD();
        assertTrue(fd.valid());
    }

    @Test
    public void should_get_file_channel() throws IOException {
        is = new FileInputStream(new File("res/input.data"));
        FileChannel channel = is.getChannel();
        assertEquals(0, channel.position());
        ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
        channel.read(buffer);
        assertTrue(new String(buffer.array(), "utf-8").startsWith("《藏地密码》"));
        assertEquals(channel.size(), channel.position());
    }

    @After
    public void teardown() throws IOException {
        if (is != null) {
            is.close();
        }
    }
}
