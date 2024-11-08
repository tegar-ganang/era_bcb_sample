package phex.test;

import java.io.*;
import java.util.*;
import java.util.zip.InflaterInputStream;
import junit.framework.TestCase;
import phex.utils.IOUtil;

public class TestIOUtil extends TestCase {

    private byte[] randomData;

    public TestIOUtil(String s) {
        super(s);
    }

    protected void setUp() {
        randomData = new byte[100000];
        Random rand = new Random();
        rand.nextBytes(randomData);
        for (int i = 300; i < 500; i++) {
            randomData[i] = (byte) 0;
        }
        for (int i = 3000; i < 5000; i++) {
            randomData[i] = (byte) 0;
        }
    }

    protected void tearDown() {
    }

    public void testDeflate() throws IOException {
        byte[] compressed = IOUtil.deflate(randomData);
        assertTrue(compressed.length < randomData.length);
        InflaterInputStream stream = new InflaterInputStream(new ByteArrayInputStream(compressed));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int read = 0;
        do {
            read = stream.read(buf);
            if (read > 0) {
                out.write(buf, 0, read);
            }
        } while (read > 0);
        assertTrue(Arrays.equals(out.toByteArray(), randomData));
    }

    public void testSimpleInflate() {
        byte[] compressed = IOUtil.deflate(randomData);
        byte[] decompressed = IOUtil.inflate(compressed);
        assertTrue(Arrays.equals(decompressed, randomData));
    }

    public void testCobs() {
        byte[] data = new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 0, 1, 0, 0, 3, 0, 4, 3, 4, 5, 3, 6, 0, 0, 0, 0, 0, 0, 0, 3, 4, 3, 5, 4, 5, 6, 0, 0, 2, 3, 4, 2, 3 };
        byte[] encoded = IOUtil.cobsEncode(data);
        byte[] decoded = IOUtil.cobsDecode(encoded);
        assertTrue(Arrays.equals(data, decoded));
        data = new byte[] { 0x45, 0x00, 0x00, 0x2C, 0x4C, 0x79, 0x00, 0x00, 0x40, 0x06, 0x4F, 0x37 };
        encoded = IOUtil.cobsEncode(data);
        byte[] expected = new byte[] { 0x02, 0x45, 0x01, 0x04, 0x2C, 0x4C, 0x79, 0x01, 0x05, 0x40, 0x06, 0x4F, 0x37 };
        assertTrue(Arrays.equals(encoded, expected));
        decoded = IOUtil.cobsDecode(expected);
        assertTrue(Arrays.equals(data, decoded));
    }
}
