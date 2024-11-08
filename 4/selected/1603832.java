package org.jd3lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import junit.framework.TestCase;

/**
 * @author Grunewald
 */
public class Id3v1TagTest extends TestCase {

    public void testId3v1Tag() {
        File testMP3 = new File("test_data/TestMP3_15_MP3InfoExt_3APIC.mp3");
        FileChannel chan;
        try {
            chan = new FileInputStream(testMP3).getChannel();
            chan.position(chan.size() - 128);
            ByteBuffer mybyte = ByteBuffer.allocate(128);
            chan.read(mybyte);
            mybyte.flip();
            System.out.println(new Id3v1Tag(mybyte));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail("Filenot found");
        } catch (IOException e) {
            e.printStackTrace();
            fail("IOEXception");
        } catch (InstantiationException e) {
            e.printStackTrace();
            fail("No Id3tag");
        }
        System.out.println("END");
    }
}
