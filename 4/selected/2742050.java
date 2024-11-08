package jmri.jmrit.sound;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for the WavBuffer class.
 * <P>
 * Note:  This makes noise!
 *
 * @author	    Bob Jacobsen  Copyright 2006
 * @version         $Revision: 17977 $
 */
public class WavBufferTest extends TestCase {

    public void testCtor() {
        byte[] data = new byte[idata.length];
        for (int i = 0; i < idata.length; i++) data[i] = (byte) idata[i];
        new WavBuffer(data);
    }

    public void testSizes() {
        byte[] data = new byte[idata.length];
        for (int i = 0; i < idata.length; i++) data[i] = (byte) idata[i];
        WavBuffer w = new WavBuffer(data);
        Assert.assertTrue("sample rate", 11025.0 == w.getSampleRate());
        Assert.assertEquals("sample size", 8, w.getSampleSizeInBits());
        Assert.assertEquals("channels   ", 1, w.getChannels());
    }

    public void testLocations() {
        byte[] data = new byte[idata.length];
        for (int i = 0; i < idata.length; i++) data[i] = (byte) idata[i];
        WavBuffer w = new WavBuffer(data);
        Assert.assertEquals("data header offset   ", 48, w.findHeader(0x64, 0x61, 0x74, 0x61));
        Assert.assertEquals("data start offset    ", 56, w.getDataStart());
        Assert.assertEquals("data size            ", 32, w.getDataSize());
        Assert.assertEquals("data end offset      ", 56 + 32 - 1, w.getDataEnd());
    }

    public WavBufferTest(String s) {
        super(s);
    }

    public static void main(String[] args) {
        String[] testCaseName = { WavBufferTest.class.getName() };
        junit.swingui.TestRunner.main(testCaseName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(WavBufferTest.class);
        return suite;
    }

    int[] idata = new int[] { 0x52, 0x49, 0x46, 0x46, 0xC4, 0x00, 0x00, 0x00, 0x57, 0x41, 0x56, 0x45, 0x66, 0x6d, 0x74, 0x20, 0x10, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x11, 0x2b, 0x00, 0x00, 0x11, 0x2b, 0x00, 0x00, 0x01, 0x00, 0x08, 0x00, 0x66, 0x61, 0x63, 0x74, 0x04, 0x00, 0x00, 0x00, 0xc0, 0x24, 0x00, 0x00, 0x64, 0x61, 0x74, 0x61, 0x20, 0x00, 0x00, 0x00, 0x81, 0x83, 0x81, 0x81, 0x83, 0x88, 0x84, 0x86, 0x8a, 0x82, 0x84, 0x85, 0x84, 0x89, 0x80, 0x83, 0x8b, 0x85, 0x89, 0x88, 0x81, 0x85, 0x85, 0x81, 0x80, 0x7f, 0x80, 0x7f, 0x82, 0x83, 0x80, 0x83, 0x4c, 0x49, 0x53, 0x54, 0x42, 0x00, 0x00, 0x00, 0x49, 0x4e, 0x46, 0x4f, 0x49, 0x53, 0x46, 0x54, 0x0f, 0x00, 0x00, 0x00, 0x4d, 0x79, 0x53, 0x6f, 0x75, 0x6e, 0x64, 0x53, 0x74, 0x75, 0x64, 0x69, 0x6f, 0x20, 0x00, 0x00, 0x49, 0x45, 0x4e, 0x47, 0x0b, 0x00, 0x00, 0x00, 0x41, 0x4a, 0x20, 0x49, 0x72, 0x65, 0x6c, 0x61, 0x6e, 0x64, 0x00, 0x00, 0x49, 0x43, 0x4f, 0x50, 0x0a, 0x00, 0x00, 0x00, 0x44, 0x69, 0x67, 0x69, 0x74, 0x72, 0x61, 0x78, 0x20, 0x00, 0x44, 0x49, 0x53, 0x50, 0x19, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x47, 0x50, 0x31, 0x30, 0x20, 0x70, 0x61, 0x73, 0x73, 0x69, 0x6e, 0x67, 0x20, 0x63, 0x61, 0x70, 0x74, 0x75, 0x72, 0x65, 0x00, 0x00 };
}
