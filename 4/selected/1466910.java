package org.tritonus.test;

import java.io.File;
import java.io.IOException;
import junit.framework.TestCase;
import org.tritonus.share.sampled.file.AudioOutputStream;
import org.tritonus.share.sampled.AudioSystemShadow;
import javax.sound.sampled.*;

/** Tests conversion of Ogg files with long silence at the start of the file.
 * NOTE: These tests create a large (26mb) temporary audio file in the sounds dir
 * which is deleted after the test completes, but be sure you have sufficient disk space.
 */
public class VorbisTruncateTestCase extends TestCase {

    private static final File _sourceFileOgg = new File("sounds/testtruncate.ogg");

    private static final File _destFileWav = new File("sounds/testtruncate.wav");

    public VorbisTruncateTestCase(String strName) {
        super(strName);
    }

    protected void setUp() {
        assertTrue("Missing required test file: " + _sourceFileOgg.getAbsolutePath(), _sourceFileOgg.exists());
        _destFileWav.deleteOnExit();
    }

    protected void tearDown() {
        assertTrue("Deleted required test file: " + _sourceFileOgg.getAbsolutePath(), _sourceFileOgg.exists());
        if (_destFileWav.exists()) {
            assertTrue("Couldn't delete file: " + _destFileWav.getAbsolutePath() + "; size: " + _destFileWav.length() + ". (0 size may mean file is locked by buffer loop.)", _destFileWav.delete());
        }
    }

    public void testConvertTruncateOggWithAudioOutStream() throws Exception {
        final AudioInputStream inAIStreamOgg = AudioSystem.getAudioInputStream(_sourceFileOgg);
        final AudioFormat destAudioFormatPCM = new AudioFormat(22050.0F, 16, 1, true, false);
        final AudioInputStream inAIStreamPCM = AudioSystem.getAudioInputStream(destAudioFormatPCM, inAIStreamOgg);
        final AudioOutputStream outAOStreamWavPCM = AudioSystemShadow.getAudioOutputStream(AudioFileFormat.Type.WAVE, destAudioFormatPCM, AudioSystem.NOT_SPECIFIED, _destFileWav);
        long readCntPCMTotal = 0;
        int readCnt;
        final byte[] buf = new byte[4 * 1024];
        try {
            while ((readCnt = inAIStreamPCM.read(buf, 0, buf.length)) != -1) {
                readCntPCMTotal += readCnt;
                outAOStreamWavPCM.write(buf, 0, readCnt);
            }
        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception pumping streams: " + e.getMessage());
        }
        outAOStreamWavPCM.close();
        inAIStreamPCM.close();
        inAIStreamOgg.close();
        assertTrue("Converted wav file is empty: " + _destFileWav.getAbsolutePath(), _destFileWav.length() > 0);
        playStream(_destFileWav);
        assertEquals("Missing some PCM data from decoded Vorbis stream.", 369664, readCntPCMTotal);
    }

    public void testConvertTruncateOggWithAudioSystem() throws Exception {
        final AudioInputStream inAIStreamOgg = AudioSystem.getAudioInputStream(_sourceFileOgg);
        final AudioFormat destAudioFormatPCM = new AudioFormat(22050.0F, 16, 1, true, false);
        final AudioInputStream inAIStreamPCM = AudioSystem.getAudioInputStream(destAudioFormatPCM, inAIStreamOgg);
        AudioSystem.write(inAIStreamPCM, AudioFileFormat.Type.WAVE, _destFileWav);
        inAIStreamPCM.close();
        inAIStreamOgg.close();
        assertTrue("Converted wav file is empty: " + _destFileWav.getAbsolutePath(), _destFileWav.length() > 0);
        playStream(_destFileWav);
        assertEquals("Missing some PCM data from decoded Vorbis stream.", 369708, _destFileWav.length());
    }

    /** Play the given audio stream. Closes the stream when finised.
	 * @param fileToPlay the audio file to play
	 * @throws LineUnavailableException if can't get line for stream's format
	 * @throws IOException if problem occurs reading the stream
	 */
    private static void playStream(final File fileToPlay) throws LineUnavailableException, IOException, UnsupportedAudioFileException {
        final AudioInputStream streamToPlay = AudioSystem.getAudioInputStream(fileToPlay);
        final SourceDataLine line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, streamToPlay.getFormat()));
        line.open();
        line.start();
        try {
            byte[] buf = new byte[1024];
            int readCnt;
            while ((readCnt = streamToPlay.read(buf, 0, buf.length)) != -1) {
                line.write(buf, 0, readCnt);
            }
        } finally {
            line.drain();
            streamToPlay.close();
            line.stop();
            line.close();
        }
    }
}
