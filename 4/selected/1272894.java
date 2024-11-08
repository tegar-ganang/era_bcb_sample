package org.esb.model;

import junit.framework.TestCase;
import org.esb.BaseTest;
import org.esb.hive.DatabaseService;
import org.esb.model.Filter;
import org.esb.model.MediaStream;
import org.hibernate.Session;

/**
 *
 * @author HoelscJ
 */
public class MediaStreamTest extends BaseTest {

    public MediaStreamTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSimpleMediaStreamAttributes() {
        MediaStream stream = new MediaStream();
        stream.setId(1);
        assertEquals(1, stream.getId());
        stream.setCodecType(1);
        assertEquals(1, stream.getCodecType());
        stream.setStreamIndex(1);
        assertEquals(1, stream.getStreamIndex());
        stream.setStreamType(1);
        assertEquals(1, stream.getStreamType());
        stream.setCodecId(1);
        assertEquals(1, stream.getCodecId());
        stream.setCodecName("testcodec");
        assertEquals("testcodec", stream.getCodecName());
        stream.setFrameRateNum(1);
        assertEquals(1, stream.getFrameRateNum());
        stream.setFrameRateDen(20);
        assertEquals(20, stream.getFrameRateDen());
        stream.setStartTime(21L);
        assertEquals(new Long(21), stream.getStartTime());
        stream.setFirstDts(22L);
        assertEquals(new Long(22), stream.getFirstDts());
        stream.setDuration(23L);
        assertEquals(new Long(23), stream.getDuration());
        stream.setNumFrames(24L);
        assertEquals(new Long(24), stream.getNumFrames());
        stream.setTimeBaseNum(25);
        assertEquals(25, stream.getTimeBaseNum());
        stream.setTimeBaseDen(26);
        assertEquals(26, stream.getTimeBaseDen());
        stream.setCodecTimeBaseNum(27);
        assertEquals(27, stream.getCodecTimeBaseNum());
        stream.setCodecTimeBaseDen(28);
        assertEquals(28, stream.getCodecTimeBaseDen());
        stream.setTicksPerFrame(29);
        assertEquals(29, stream.getTicksPerFrame());
        stream.setFrameCount(30);
        assertEquals(30, stream.getFrameCount());
        stream.setWidth(320);
        assertEquals(320, stream.getWidth());
        stream.setHeight(240);
        assertEquals(240, stream.getHeight());
        stream.setGopSize(250);
        assertEquals(250, stream.getGopSize());
        stream.setPixelFormat(100);
        assertEquals(100, stream.getPixelFormat());
        stream.setBitrate(1024);
        assertEquals(1024, stream.getBitrate());
        stream.setRateEmu(1);
        assertEquals(1, stream.getRateEmu());
        stream.setSampleRate(44100);
        assertEquals(44100, stream.getSampleRate());
        stream.setChannels(2);
        assertEquals(2, stream.getChannels());
        stream.setSampleFormat(5);
        assertEquals(5, stream.getSampleFormat());
        stream.setBitsPerCodedSample(8);
        assertEquals(8, stream.getBitsPerCodedSample());
        stream.setFlags(815);
        assertEquals(815, stream.getFlags());
        stream.setExtraCodecFlags("blafasel");
        assertEquals("blafasel", stream.getExtraCodecFlags());
        stream.setPrivateDataSize(10);
        assertEquals(10, stream.getPrivateDataSize());
        byte[] tmp = new byte[10];
        stream.setPrivateData(tmp);
        assertEquals(new String(tmp), new String(stream.getPrivateData()));
        stream.setExtraDataSize(10);
        assertEquals(10, stream.getExtraDataSize());
        byte[] tmp_e = new byte[10];
        stream.setExtraData(tmp_e);
        assertEquals(new String(tmp_e), new String(stream.getExtraData()));
    }

    public void _testSimpleMediaStreamFilterPersistence() {
        MediaStream stream = new MediaStream();
        stream.put("first_attribute", "first_val");
        Filter filter_a = new Filter();
        filter_a.setFilterName("filter_a");
        Filter filter_b = new Filter();
        filter_b.setFilterName("filter_b");
        stream.addFilter(filter_a);
        stream.addFilter(filter_b);
        Session session = DatabaseService.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        session.save(stream);
        assertTrue(stream.getId() > 0);
        session.getTransaction().commit();
    }
}
