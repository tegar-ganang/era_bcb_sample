package name.huzhenbo.java.io;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileOutputStreamTest {

    private File file;

    @Test
    public void should_create_file_if_not_exist() throws IOException {
        file = new File("res/notexist.file");
        if (file.exists()) {
            file.delete();
        }
        assertFalse(file.exists());
        FileOutputStream os = new FileOutputStream("res/notexist.file");
        assertFalse(!file.exists());
        os.close();
    }

    @Test
    public void should_not_append() throws IOException {
        file = new File("res/notexist.file");
        if (!file.exists()) {
            file.createNewFile();
        }
        FileOutputStream os = new FileOutputStream(file);
        byte[] b = { 97, 98, 99 };
        os.write(b);
        assertEquals(3, os.getChannel().size());
        os.close();
        FileOutputStream os2 = new FileOutputStream(file);
        os2.write(b);
        assertEquals(3, os2.getChannel().size());
        os2.close();
    }

    @Test
    public void should_append() throws IOException {
        file = new File("res/notexist.file");
        if (!file.exists()) {
            file.createNewFile();
        }
        FileOutputStream os = new FileOutputStream(file);
        byte[] b = { 97, 98, 99 };
        os.write(b);
        assertEquals(3, os.getChannel().size());
        os.close();
        FileOutputStream os2 = new FileOutputStream(file, true);
        os2.write(b);
        assertEquals(6, os2.getChannel().size());
        os2.close();
    }

    @Test
    public void should_writable_channel() throws IOException {
        FileOutputStream os = new FileOutputStream("res/notexist.file");
        FileChannel channel = os.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(3);
        buffer.put(new byte[] { 97, 98, 99 });
        channel.write(buffer);
        channel.close();
        os.close();
        file = new File("res/notexist.file");
    }

    @After
    public void teardown() {
        assertTrue(file.delete());
    }
}
