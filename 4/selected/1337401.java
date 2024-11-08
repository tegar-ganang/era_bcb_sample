package org.esb.model;

import java.io.File;
import java.util.Date;
import org.esb.BaseTest;
import org.esb.dao.MediaFilesDao;
import org.esb.jmx.JHiveRegistryException;

public class MediaFileTest extends BaseTest {

    public MediaFileTest(String name) {
        super(name);
    }

    private MediaFile createTestMediaFile(int id) {
        MediaFile file = new MediaFile();
        file.setPath(File.separator + "tmp");
        file.setFilename("bla.mp4");
        file.setAlbum("album");
        file.setAuthor("author");
        file.setBitrate(101010L);
        file.setComment("comment");
        file.setContainerType("mp4");
        file.setCopyright("copyright");
        file.setDuration(202020L);
        file.setFileType(1);
        file.setGenre(2);
        file.setId(id);
        file.setInsertDate(new Date(0L));
        file.setSize(303030L);
        file.setTitle("title");
        file.setTrack(5);
        file.setYear(2000);
        return file;
    }

    private MediaStream createTestMediaFileStream(int id) {
        MediaStream stream = new MediaStream();
        stream.setId(id);
        assertEquals(id, stream.getId());
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
        return stream;
    }

    private void assertMediaFileStream(MediaStream stream, int id) {
        assertEquals(id, stream.getId());
        assertEquals(1, stream.getCodecType());
        assertEquals(1, stream.getStreamIndex());
        assertEquals(1, stream.getStreamType());
        assertEquals(1, stream.getCodecId());
        assertEquals("testcodec", stream.getCodecName());
        assertEquals(1, stream.getFrameRateNum());
        assertEquals(20, stream.getFrameRateDen());
        assertEquals(new Long(21), stream.getStartTime());
        assertEquals(new Long(22), stream.getFirstDts());
        assertEquals(new Long(23), stream.getDuration());
        assertEquals(new Long(24), stream.getNumFrames());
        assertEquals(25, stream.getTimeBaseNum());
        assertEquals(26, stream.getTimeBaseDen());
        assertEquals(27, stream.getCodecTimeBaseNum());
        assertEquals(28, stream.getCodecTimeBaseDen());
        assertEquals(29, stream.getTicksPerFrame());
        assertEquals(30, stream.getFrameCount());
        assertEquals(320, stream.getWidth());
        assertEquals(240, stream.getHeight());
        assertEquals(250, stream.getGopSize());
        assertEquals(100, stream.getPixelFormat());
        assertEquals(1024, stream.getBitrate());
        assertEquals(1, stream.getRateEmu());
        assertEquals(44100, stream.getSampleRate());
        assertEquals(2, stream.getChannels());
        assertEquals(5, stream.getSampleFormat());
        assertEquals(8, stream.getBitsPerCodedSample());
        assertEquals(815, stream.getFlags());
        assertEquals("blafasel", stream.getExtraCodecFlags());
        assertEquals(10, stream.getPrivateDataSize());
        byte[] tmp = new byte[10];
        assertEquals(new String(tmp), new String(stream.getPrivateData()));
        stream.setExtraDataSize(10);
        assertEquals(10, stream.getExtraDataSize());
        byte[] tmp_e = new byte[10];
        assertEquals(new String(tmp_e), new String(stream.getExtraData()));
    }

    private void assertMediaFile(MediaFile file, int id) {
        assertEquals(File.separator + "tmp", file.getPath());
        assertEquals("bla.mp4", file.getFilename());
        assertEquals("album", file.getAlbum());
        assertEquals("author", file.getAuthor());
        assertEquals(101010L, file.getBitrate().longValue());
        assertEquals("comment", file.getComment());
        assertEquals("mp4", file.getContainerType());
        assertEquals("copyright", file.getCopyright());
        assertEquals(202020L, file.getDuration().longValue());
        assertEquals(1, file.getFileType());
        assertEquals(2, file.getGenre());
        assertEquals(id, file.getId());
        assertEquals(303030L, file.getSize().longValue());
        assertEquals("title", file.getTitle());
        assertEquals(5, file.getTrack());
        assertEquals(2000, file.getYear());
    }

    public void testSimpleMediaFile() {
        assertMediaFile(createTestMediaFile(815), 815);
    }

    public void testMediaFileStream() {
        MediaFile file = createTestMediaFile(0);
        MediaStream stream = createTestMediaFileStream(1);
        MediaStream stream2 = createTestMediaFileStream(2);
        assertMediaFileStream(stream, 1);
        assertMediaFileStream(stream2, 2);
        file.addStream(stream);
        file.addStream(stream2);
        assertMediaFileStream(file.getStreams().get(0), 1);
        assertMediaFileStream(file.getStreams().get(1), 2);
    }

    public void testMediaFilePersistence() throws JHiveRegistryException {
        MediaFile file = createTestMediaFile(0);
        MediaStream stream = createTestMediaFileStream(1);
        MediaStream stream2 = createTestMediaFileStream(2);
        file.addStream(stream);
        file.addStream(stream2);
        int id = MediaFilesDao.setMediaFile(file);
        MediaFile revert = MediaFilesDao.getMediaFile(id);
        assertMediaFile(revert, id);
        stream = revert.getStreams().get(0);
        assertTrue(stream.getId() > 0);
        assertEquals(1, stream.getCodecType());
        assertEquals(1, stream.getStreamIndex());
        assertEquals(1, stream.getStreamType());
        assertEquals(1, stream.getCodecId());
        assertEquals("testcodec", stream.getCodecName());
        assertEquals(1, stream.getFrameRateNum());
        assertEquals(20, stream.getFrameRateDen());
        assertEquals(new Long(21), stream.getStartTime());
        assertEquals(new Long(22), stream.getFirstDts());
        assertEquals(new Long(23), stream.getDuration());
        assertEquals(new Long(24), stream.getNumFrames());
        assertEquals(25, stream.getTimeBaseNum());
        assertEquals(26, stream.getTimeBaseDen());
        assertEquals(27, stream.getCodecTimeBaseNum());
        assertEquals(28, stream.getCodecTimeBaseDen());
        assertEquals(29, stream.getTicksPerFrame());
        assertEquals(30, stream.getFrameCount());
        assertEquals(320, stream.getWidth());
        assertEquals(240, stream.getHeight());
        assertEquals(250, stream.getGopSize());
        assertEquals(100, stream.getPixelFormat());
        assertEquals(1024, stream.getBitrate());
        assertEquals(0, stream.getRateEmu());
        assertEquals(44100, stream.getSampleRate());
        assertEquals(2, stream.getChannels());
        assertEquals(5, stream.getSampleFormat());
        assertEquals(8, stream.getBitsPerCodedSample());
        assertEquals(815, stream.getFlags());
        assertEquals("blafasel", stream.getExtraCodecFlags());
        assertEquals(0, stream.getPrivateDataSize());
        assertEquals(10, stream.getExtraDataSize());
        byte[] tmp_e = new byte[10];
        assertEquals(new String(tmp_e), new String(stream.getExtraData()));
        stream = revert.getStreams().get(1);
        assertTrue(stream.getId() > 0);
        assertEquals(1, stream.getCodecType());
        assertEquals(1, stream.getStreamIndex());
        assertEquals(1, stream.getStreamType());
        assertEquals(1, stream.getCodecId());
        assertEquals("testcodec", stream.getCodecName());
        assertEquals(1, stream.getFrameRateNum());
        assertEquals(20, stream.getFrameRateDen());
        assertEquals(new Long(21), stream.getStartTime());
        assertEquals(new Long(22), stream.getFirstDts());
        assertEquals(new Long(23), stream.getDuration());
        assertEquals(new Long(24), stream.getNumFrames());
        assertEquals(25, stream.getTimeBaseNum());
        assertEquals(26, stream.getTimeBaseDen());
        assertEquals(27, stream.getCodecTimeBaseNum());
        assertEquals(28, stream.getCodecTimeBaseDen());
        assertEquals(29, stream.getTicksPerFrame());
        assertEquals(30, stream.getFrameCount());
        assertEquals(320, stream.getWidth());
        assertEquals(240, stream.getHeight());
        assertEquals(250, stream.getGopSize());
        assertEquals(100, stream.getPixelFormat());
        assertEquals(1024, stream.getBitrate());
        assertEquals(0, stream.getRateEmu());
        assertEquals(44100, stream.getSampleRate());
        assertEquals(2, stream.getChannels());
        assertEquals(5, stream.getSampleFormat());
        assertEquals(8, stream.getBitsPerCodedSample());
        assertEquals(815, stream.getFlags());
        assertEquals("blafasel", stream.getExtraCodecFlags());
        assertEquals(0, stream.getPrivateDataSize());
        assertEquals(10, stream.getExtraDataSize());
        assertEquals(new String(tmp_e), new String(stream.getExtraData()));
    }
}
