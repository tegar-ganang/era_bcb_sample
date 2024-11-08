package net.sourceforge.tuned;

import static org.junit.Assert.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import org.junit.Test;

public class ByteBufferOutputStreamTest {

    @Test
    public void growBufferAsNeeded() throws Exception {
        ByteBufferOutputStream buffer = new ByteBufferOutputStream(1, 1.0f);
        buffer.write("asdf".getBytes("utf-8"));
        assertEquals("asdf", Charset.forName("utf-8").decode(buffer.getByteBuffer()).toString());
        assertEquals(4, buffer.capacity());
    }

    @Test
    public void transferFrom() throws Exception {
        InputStream in = new ByteArrayInputStream("asdf".getBytes("utf-8"));
        ByteBufferOutputStream buffer = new ByteBufferOutputStream(4);
        int n = buffer.transferFrom(Channels.newChannel(in));
        assertEquals(4, n);
        assertEquals("asdf", Charset.forName("utf-8").decode(buffer.getByteBuffer()).toString());
    }
}
