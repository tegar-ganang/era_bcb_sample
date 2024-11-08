package org.esb.model;

import java.sql.Connection;
import org.esb.BaseTest;
import org.esb.dao.JobDao;
import org.esb.dao.MediaFilesDao;
import org.esb.dao.ProfilesDao;
import org.esb.hive.DatabaseService;
import org.esb.hive.FileImporter;
import org.esb.jmx.JHiveRegistryException;
import org.esb.util.Util;

public class JobTest extends BaseTest {

    public JobTest(String name) {
        super(name);
    }

    public void testJobDaoSave() throws Exception {
        MediaFile file = new MediaFile(TEST_VIDEO_XUGGLE_PATH, TEST_VIDEO_FLV);
        int id = FileImporter.importFile(file);
        Profile profile = ProfilesDao.getProfile(1);
        String outdir = "/tmp/";
        MediaFile outfile = new MediaFile();
        outfile.setPath(outdir);
        Util.createOutputMediaFile(file, outfile, profile);
        MediaFilesDao.setMediaFile(outfile);
        Job job = new Job();
        job.setInputFile(file);
        job.setOutputFile(outfile);
        JobDao.setJob(job);
        Connection con = DatabaseService.getConnection();
    }

    public void _testSimpleJob() throws Exception {
        MediaFile file = new MediaFile(TEST_VIDEO_XUGGLE_PATH, TEST_VIDEO_FLV);
        int id = FileImporter.importFile(file);
        Profile profile = ProfilesDao.getProfile(1);
        String outdir = "/tmp/";
        MediaFile outfile = new MediaFile();
        outfile.setPath(outdir);
        Util.createOutputMediaFile(file, outfile, profile);
        Job job = new Job();
        job.setInputFile(file);
        job.setOutputFile(outfile);
        assertEquals(file, job.getInputFile());
        assertEquals(outfile, job.getOutputFile());
        assertEquals(2, job.getStreams().size());
        assertEquals(Job.STATUS.NONE, job.getStatus());
        JobStream js1 = job.getStreams().get(0);
        MediaStream s1 = js1.getOutputStream();
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
        JobStream js2 = job.getStreams().get(1);
        MediaStream s2 = js2.getOutputStream();
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