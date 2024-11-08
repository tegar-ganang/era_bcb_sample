package org.spantus.work.wav;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.spantus.exception.ProcessingException;
import org.spantus.logger.Logger;
import org.spantus.utils.FileUtils;

/**
 * 
 * @author Mindaugas Greibus
 * @since 0.0.1
 *
 */
public class WorkAudioManager implements AudioManager {

    Logger log = Logger.getLogger(getClass());

    public void play(URL fileURL, Float from, Float length) {
        AudioInputStream stream = createInput(fileURL);
        float fromVal = from == null ? 0 : from.floatValue();
        float lengthVal = length == null ? getTotalTime(stream) : length.floatValue();
        play(stream, fromVal, lengthVal);
    }

    private void play(AudioInputStream stream, Float from, Float length) {
        log.debug("[play] from:{0}; length= {1} ", from, length);
        double totalTime = getTotalTime(stream);
        double ends = from + length;
        double adaptedLength = ends > totalTime ? totalTime - from : length;
        if (from > totalTime) {
            log.error("[play] Cannot play due start is more than total time" + from + ">" + totalTime);
            return;
        }
        long startsBytes = (long) ((from * stream.getFormat().getFrameRate()) * stream.getFormat().getFrameSize());
        long lengthBytes = (long) ((adaptedLength * stream.getFormat().getFrameRate()) * stream.getFormat().getFrameSize());
        Playback pl = new Playback(stream, startsBytes, lengthBytes);
        pl.start();
    }

    /**
	 * 
	 */
    public String save(URL fileURL, Float startsObj, Float lengthObj, String pathToSavePrefered) {
        log.debug("[save] from:{0}; lenght:{1}; pathToSave:{2}", startsObj, lengthObj, pathToSavePrefered);
        AudioInputStream stream = createInput(fileURL);
        Float totalTime = getTotalTime(stream);
        Float starts = startsObj == null ? 0 : startsObj.floatValue();
        Float length = lengthObj == null ? totalTime : lengthObj.floatValue();
        double ends = starts + length;
        double adaptedLength = ends > totalTime ? totalTime - starts : length;
        if (starts > totalTime) {
            log.error("[save] Cannot save due stars:" + starts + " more than total time:" + totalTime);
            throw new ProcessingException("Cannot save due stars:" + starts + " more than total time:" + totalTime);
        }
        Long startsBytes = (long) ((starts * stream.getFormat().getFrameRate()) * stream.getFormat().getFrameSize());
        Long lengthBytes = (long) ((adaptedLength * stream.getFormat().getFrameRate()) * stream.getFormat().getFrameSize());
        try {
            long skipedByteTotal = startsBytes;
            long skipedByte = stream.available();
            while ((skipedByte = stream.skip(skipedByteTotal)) != 0) {
                skipedByteTotal -= skipedByte;
            }
            byte[] data = new byte[lengthBytes.intValue()];
            stream.read(data);
            InputStream bais = new ByteArrayInputStream(data);
            AudioInputStream ais = new AudioInputStream(bais, stream.getFormat(), data.length / 2);
            File nextAvaible = FileUtils.findNextAvaibleFile(pathToSavePrefered);
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, FileUtils.findNextAvaibleFile(pathToSavePrefered));
            return nextAvaible.getAbsolutePath();
        } catch (IOException e) {
            throw new ProcessingException(e);
        }
    }

    /**
	 * 
	 * @return
	 */
    protected Float getTotalTime(AudioInputStream stream) {
        Float totalTime = (stream.getFrameLength() / stream.getFormat().getFrameRate());
        return totalTime;
    }

    /**
	 * Set up the audio input stream from the sound file
	 * @param fileURL
	 * @return
	 */
    private AudioInputStream createInput(URL fileURL) {
        AudioInputStream stream = null;
        try {
            stream = createAudioInputStream(fileURL);
        } catch (UnsupportedAudioFileException e) {
            log.error(e);
        } catch (IOException e) {
            log.error(e);
        }
        return stream;
    }

    /**
	 * utils method create input stream
	 * @param fileURL
	 * @return
	 * @throws UnsupportedAudioFileException
	 * @throws IOException
	 */
    public static final AudioInputStream createAudioInputStream(URL fileURL) throws UnsupportedAudioFileException, IOException {
        AudioInputStream stream = null;
        stream = AudioSystem.getAudioInputStream(fileURL);
        AudioFormat format = stream.getFormat();
        if ((format.getEncoding() == AudioFormat.Encoding.ULAW) || (format.getEncoding() == AudioFormat.Encoding.ALAW)) {
            AudioFormat newFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
            stream = AudioSystem.getAudioInputStream(newFormat, stream);
            format = newFormat;
        }
        return stream;
    }

    private class Playback extends Thread {

        private long starts;

        private long length;

        private AudioInputStream stream;

        private boolean playing;

        /**
		 * 
		 */
        public Playback(AudioInputStream stream, long starts, long length) {
            this.stream = stream;
            this.starts = starts;
            this.length = length;
        }

        public void run() {
            playback(stream, starts, length);
        }

        /**
		 * set up the SourceDataLine going to the JVM's mixer
		 * 
		 */
        private SourceDataLine createOutput(AudioFormat format) {
            SourceDataLine line = null;
            try {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                log.debug("[createOutput] opened output line: " + info.toString());
                if (!AudioSystem.isLineSupported(info)) {
                    log.error("[createOutput]Line does not support: " + format);
                }
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format);
            } catch (Exception e) {
                log.error(e);
            }
            return line;
        }

        /**
		 * 
		 * @param stream
		 * @param starts
		 * @param length
		 */
        private void playback(AudioInputStream stream, long starts, long length) {
            SourceDataLine line = createOutput(stream.getFormat());
            byte buffer[] = new byte[line.getBufferSize()];
            line.start();
            try {
                int byteCount;
                long totalByte = 0;
                long skiped = 0;
                long readSize = Math.min(starts, buffer.length);
                setPlaying(true);
                while ((skiped = stream.skip(readSize)) > 0 && totalByte < starts && isPlaying()) {
                    totalByte += skiped;
                    if ((starts - (totalByte + buffer.length)) < buffer.length) {
                        readSize = starts - totalByte;
                    }
                }
                totalByte = 0;
                readSize = Math.min(length, buffer.length);
                while ((byteCount = stream.read(buffer, 0, (int) readSize)) > 0 && totalByte < length && isPlaying()) {
                    byte[] proceedBuf = preprocessSamples(buffer, byteCount);
                    if (byteCount > 0) {
                        line.write(proceedBuf, 0, byteCount);
                    }
                    totalByte += byteCount;
                    readSize = Math.min((length - totalByte), readSize);
                }
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    log.error(e);
                }
                line.drain();
                line.stop();
                line.close();
            } catch (IOException e) {
                setPlaying(false);
                e.printStackTrace();
                log.error(e);
            }
        }

        private byte[] preprocessSamples(byte[] samples, int numBytes) {
            return samples;
        }

        public boolean isPlaying() {
            return playing;
        }

        public void setPlaying(boolean playing) {
            this.playing = playing;
        }
    }
}
