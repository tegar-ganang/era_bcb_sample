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
public class VorbisSilenceTestCase extends TestCase {

    private File _sourceFileOgg = new File("sounds/testsilence.ogg");

    private File _destFileWav = new File("sounds/testsilenceout.wav");

    public VorbisSilenceTestCase(String strName) {
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

    public void testConvertSilentOggWithAudioOutStream() throws Exception {
        final AudioInputStream inAIStreamOgg = AudioSystem.getAudioInputStream(_sourceFileOgg);
        final AudioFormat destAudioFormatPCM = new AudioFormat(44100.0F, 16, 1, true, false);
        final AudioInputStream inAIStreamPCM = AudioSystem.getAudioInputStream(destAudioFormatPCM, inAIStreamOgg);
        final AudioOutputStream outAOStreamWavPCM = AudioSystemShadow.getAudioOutputStream(AudioFileFormat.Type.WAVE, destAudioFormatPCM, AudioSystem.NOT_SPECIFIED, _destFileWav);
        class StreamPump extends Thread {

            boolean isRunFinished;

            StreamPump() {
                super("SilenceTest-StreamPump");
            }

            public void run() {
                int readCnt;
                final byte[] buf = new byte[65536];
                int cnt = 0;
                try {
                    while ((readCnt = inAIStreamPCM.read(buf, 0, buf.length)) != -1) {
                        outAOStreamWavPCM.write(buf, 0, readCnt);
                        cnt++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    fail("Exception pumping streams: " + e.getMessage());
                }
                isRunFinished = true;
            }
        }
        ;
        StreamPump t = new StreamPump();
        t.start();
        final int maxWait = 60;
        int waitCnt = 0;
        while (t.isAlive() && waitCnt++ < maxWait) {
            Thread.sleep(1000);
        }
        outAOStreamWavPCM.close();
        inAIStreamPCM.close();
        inAIStreamOgg.close();
        assertTrue("Converted wav file is empty: " + _destFileWav.getAbsolutePath(), _destFileWav.length() > 0);
        assertTrue("Conversion never finished run() method", t.isRunFinished);
        assertTrue("Conversion timeout expired", waitCnt < maxWait);
        final AudioInputStream aisDest = AudioSystem.getAudioInputStream(_destFileWav);
        playStream(aisDest);
    }

    public void testConvertSilentOggWithAudioSystem() throws Exception {
        final AudioInputStream inAIStreamOgg = AudioSystem.getAudioInputStream(_sourceFileOgg);
        final AudioFormat destAudioFormatPCM = new AudioFormat(44100.0F, 16, 1, true, false);
        final AudioInputStream inAIStreamPCM = AudioSystem.getAudioInputStream(destAudioFormatPCM, inAIStreamOgg);
        AudioSystem.write(inAIStreamPCM, AudioFileFormat.Type.WAVE, _destFileWav);
        inAIStreamPCM.close();
        inAIStreamOgg.close();
        assertTrue("Converted wav file is empty: " + _destFileWav.getAbsolutePath(), _destFileWav.length() > 0);
        final AudioInputStream aisDest = AudioSystem.getAudioInputStream(_destFileWav);
        playStream(aisDest);
    }

    /** Play the given audio stream. Closes the stream when finised.
	 * @param streamToPlay the audio stream to play
	 * @throws LineUnavailableException if can't get line for stream's format
	 * @throws IOException if problem occurs reading the stream
	 */
    private static void playStream(final AudioInputStream streamToPlay) throws LineUnavailableException, IOException {
        if (1 == 1) {
            streamToPlay.close();
            System.gc();
            return;
        }
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
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            streamToPlay.close();
            line.stop();
            line.close();
        }
    }
}
