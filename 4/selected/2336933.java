package org.esb;

import java.io.File;
import java.sql.Connection;
import junit.framework.TestCase;
import org.esb.hive.DatabaseService;
import org.esb.hive.Setup;
import org.esb.model.MediaFile;
import org.esb.model.MediaStream;
import org.esb.util.FileSystem;

/**
 *
 * @author HoelscJ
 */
public class BaseTest extends TestCase {

    public static String TEST_VIDEO_XUGGLE_PATH = "target" + File.separator + "dependency" + File.separator + "fixtures";

    public static String TEST_VIDEO_FLV = "testfile.flv";

    public static String TEST_VIDEO_ONLY_FLV = "testfile_videoonly_20sec.flv";

    public static String TEST_AUDIO_MP3 = "testfile.mp3";

    public static String TEST_VIDEO_MPEG = "testfile_mpeg1video_mp2audio.mpg";

    public static String TEST_VIDEO_ONLY_MPEG2TS = "mpeg2_mp2.ts";

    public static final String TEST_DB = "testhive";

    public BaseTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String dir = System.getProperty("user.dir");
        DatabaseService.setDefaultDatabaseName(TEST_DB);
        File dbdir = new File(dir + File.separator + TEST_DB);
        if (!dbdir.exists() || !dbdir.isDirectory()) {
            Setup.install(dir + "/sql/hive-0.1.1.sql");
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    protected void assertTestfileFlv(MediaFile file) {
        assertEquals(TEST_VIDEO_FLV, file.getFilename());
        assertTrue(file.getPath().endsWith(TEST_VIDEO_XUGGLE_PATH));
        assertTrue(file.getSize() > 0);
        assertTrue(file.getDuration() > 0);
        assertEquals(2, file.getStreamCount());
        MediaStream s1 = file.getStreams().get(0);
        assertNotNull(s1);
        assertEquals(0, s1.getStreamIndex());
        assertEquals(0, s1.getStreamType());
        assertEquals(22, s1.getCodecId());
        assertEquals(0, s1.getCodecType());
        assertEquals(424, s1.getWidth());
        assertEquals(176, s1.getHeight());
        assertEquals(0, s1.getBitrate());
        assertEquals(15, s1.getFrameRateNum());
        assertEquals(1, s1.getFrameRateDen());
        assertEquals(0, s1.getSampleRate());
        assertEquals(0, s1.getChannels());
        assertEquals(Long.MIN_VALUE, s1.getDuration().longValue());
        MediaStream s2 = file.getStreams().get(1);
        assertNotNull(s2);
        assertEquals(1, s2.getStreamIndex());
        assertEquals(1, s2.getStreamType());
        assertEquals(86017, s2.getCodecId());
        assertEquals(1, s2.getCodecType());
        assertEquals(0, s2.getWidth());
        assertEquals(0, s2.getHeight());
        assertEquals(64000, s2.getBitrate());
        assertEquals(0, s2.getFrameRateNum());
        assertEquals(0, s2.getFrameRateDen());
        assertEquals(22050, s2.getSampleRate());
        assertEquals(1, s2.getChannels());
        assertEquals(Long.MIN_VALUE, s2.getDuration().longValue());
    }

    protected void assertTestfileFlvFromProfile1(MediaFile file) {
        assertEquals("testfile.mp4", file.getFilename());
        assertEquals("/tmp/", file.getPath());
        assertTrue(file.getSize() == 0);
        assertTrue(file.getDuration() > 0);
        assertEquals(2, file.getStreamCount());
        MediaStream s1 = file.getStreams().get(0);
        assertNotNull(s1);
        assertEquals(0, s1.getStreamIndex());
        assertEquals(0, s1.getStreamType());
        assertEquals(28, s1.getCodecId());
        assertEquals(0, s1.getCodecType());
        assertEquals(320, s1.getWidth());
        assertEquals(240, s1.getHeight());
        assertEquals(1024000, s1.getBitrate());
        assertEquals(30, s1.getFrameRateNum());
        assertEquals(1, s1.getFrameRateDen());
        assertEquals(0, s1.getSampleRate());
        assertEquals(0, s1.getChannels());
        assertEquals(0, s1.getDuration().longValue());
        MediaStream s2 = file.getStreams().get(1);
        assertNotNull(s2);
        assertEquals(1, s2.getStreamIndex());
        assertEquals(1, s2.getStreamType());
        assertEquals(86016, s2.getCodecId());
        assertEquals(1, s2.getCodecType());
        assertEquals(0, s2.getWidth());
        assertEquals(0, s2.getHeight());
        assertEquals(128000, s2.getBitrate());
        assertEquals(0, s2.getFrameRateNum());
        assertEquals(0, s2.getFrameRateDen());
        assertEquals(44100, s2.getSampleRate());
        assertEquals(2, s2.getChannels());
        assertEquals(0, s2.getDuration().longValue());
    }
}
