package name.huzhenbo.java.newio;

import static org.junit.Assert.*;
import org.junit.Test;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Set;

/**
 * 1. Charset of external resource.
 * <p/>
 * Should convert to unicode from correct charset when reading data from external resource. see: InputStreamReaderTest.java
 * <p/>
 * 2. Default charset of JVM.
 * <p/>
 * It's determined when JVM startup, and related to the environment and underlying os charset.
 * When you invoke "string".getBytes(), it will use default charset to convert String to bytes.
 * <p/>
 * 3. Charset of Java String: unicode!!!
 * <p/>
 * a. unicode -> another charset: "string".getBytes(), "string".getBytes("GBK");
 * b. one charset -> unicode: new String(new byte[]{   }, "GBK");
 */
public class CharsetTest {

    @Test
    public void should_tell_difference_between_char_and_byte_array() throws UnsupportedEncodingException {
        String hello = "hello world";
        assertEquals(11, hello.toCharArray().length);
        assertTrue(hello.getBytes().length >= hello.toCharArray().length);
        String chineseHello = "你好";
        assertEquals(2, chineseHello.toCharArray().length);
        assertArrayEquals(chineseHello.getBytes(), chineseHello.getBytes(Charset.defaultCharset()));
        if (!Charset.defaultCharset().equals(Charset.forName("utf-8"))) {
            assertFalse(Arrays.equals(chineseHello.getBytes(), chineseHello.getBytes("utf-8")));
        }
        assertTrue(chineseHello.getBytes().length > chineseHello.toCharArray().length);
    }

    @Test
    public void should_decode_byte_array() {
        String hello = "hello world";
        assertEquals(hello, new String(hello.getBytes()));
    }

    @Test
    public void should_go_through_supported_charset() {
        assertEquals(160, Charset.availableCharsets().size());
        Set<String> charsetNames = Charset.availableCharsets().keySet();
        assertTrue(charsetNames.contains("utf-8"));
        assertTrue(charsetNames.contains("utf-16"));
        assertTrue(charsetNames.contains("gb2312"));
        assertTrue(Charset.isSupported("utf-8"));
        assertFalse(Charset.isSupported("utf-7"));
    }

    @Test
    public void should_able_to_decode() throws IOException {
        FileInputStream is = new FileInputStream("res/input2.data");
        FileChannel channel = is.getChannel();
        MappedByteBuffer bb = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        CharBuffer buffer = Charset.forName("GB18030").decode(bb);
        assertTrue(buffer.toString().startsWith("《藏地密码》"));
        channel.close();
        is.close();
    }
}
