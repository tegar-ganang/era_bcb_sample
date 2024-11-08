package org.spantus.core.wav;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
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

    public Float findLength(URL file) {
        AudioInputStream stream = createInput(file);
        return getTotalTime(stream);
    }

    public void play(URL fileURL) {
        play(fileURL, null, null);
    }

    public void play(URL fileURL, Float from, Float length) {
        AudioInputStream stream = createInput(fileURL);
        if (stream == null) {
            throw new ProcessingException("file not found" + fileURL);
        }
        float totalTime = getTotalTime(stream);
        float fromVal = from == null ? 0 : from;
        float lengthVal = length == null ? totalTime : length;
        lengthVal = lengthVal == fromVal ? totalTime : lengthVal;
        play(stream, fromVal, lengthVal);
    }

    public void play(AudioInputStream stream, Float from, Float length) {
        log.debug("[play] from:{0}; length= {1} ", from, length);
        double totalTime = getTotalTime(stream);
        float start = 0f;
        float flength = (float) totalTime;
        if (from != null) {
            start = from;
        }
        if (length != null) {
            flength = length;
        }
        double ends = start + flength;
        double adaptedLength = ends > totalTime ? totalTime - start : flength;
        if (start > totalTime) {
            log.error("[play] Cannot play due start is more than total time" + from + ">" + totalTime);
            return;
        }
        long startsBytes = (long) ((start * stream.getFormat().getFrameRate()) * stream.getFormat().getFrameSize());
        long lengthBytes = (long) ((adaptedLength * stream.getFormat().getFrameRate()) * stream.getFormat().getFrameSize());
        Playback pl = new Playback(stream, startsBytes, lengthBytes);
        pl.start();
    }

    /**
     *
     */
    public String save(URL fileURL, Float starts, Float length, String pathToSavePrefered) {
        log.debug("[save] from:{0}; lenght:{1}; pathToSave:{2}", starts, length, pathToSavePrefered);
        AudioInputStream ais = findInputStream(fileURL, starts, length);
        File nextAvaible = FileUtils.findNextAvaibleFile(pathToSavePrefered);
        try {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, nextAvaible);
            return nextAvaible.getAbsolutePath();
        } catch (IOException ex) {
            throw new ProcessingException(ex);
        }
    }

    public String save(AudioInputStream ais, String pathToSavePrefered) {
        File nextAvaible = FileUtils.findNextAvaibleFile(pathToSavePrefered);
        try {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, nextAvaible);
            return nextAvaible.getAbsolutePath();
        } catch (IOException ex) {
            throw new ProcessingException(ex);
        }
    }

    /**
     * 
     * @param file
     * @param starts
     * @param length
     * @return
     */
    public AudioInputStream findInputStream(URL file, Float starts, Float length) {
        log.debug("[findInputStream] {2} from:{0}; lenght:{1}; ", starts, length, file);
        AudioInputStream stream = createInput(file);
        return findInputStream(stream, starts, length);
    }

    /**
     * 
     * @param inputStream
     * @param starts
     * @param length
     * @return
     */
    protected AudioInputStream findInputStream(AudioInputStream inputStream, Float starts, Float length) {
        log.debug("[findInputStream]  from:{0}; lenght:{1}; ", starts, length);
        Float totalTime = getTotalTime(inputStream);
        starts = starts == null ? 0L : starts;
        length = length == null ? totalTime : length;
        double ends = starts + length;
        double adaptedLength = ends > totalTime ? totalTime - starts : length;
        if (starts > totalTime) {
            throw new ProcessingException("Cannot save due stars:" + starts + " more than total time:" + totalTime);
        }
        Long startsBytes = (long) ((starts * inputStream.getFormat().getFrameRate()) * inputStream.getFormat().getFrameSize());
        Long lengthBytes = (long) ((adaptedLength * inputStream.getFormat().getFrameRate()) * inputStream.getFormat().getFrameSize());
        try {
            long skipedByteTotal = startsBytes;
            long skipedByte = inputStream.available();
            while ((skipedByte = inputStream.skip(skipedByteTotal)) != 0) {
                skipedByteTotal -= skipedByte;
            }
            byte[] data = new byte[lengthBytes.intValue()];
            inputStream.read(data);
            InputStream bais = new ByteArrayInputStream(data);
            AudioInputStream ais = new AudioInputStream(bais, inputStream.getFormat(), data.length / 2);
            return ais;
        } catch (IOException e) {
            throw new ProcessingException(e);
        }
    }

    /**
     * 
     * @param file
     * @param starts
     * @param length
     * @return
     */
    public AudioInputStream findInputStreamInMils(URL file, Long starts, Long length) {
        return findInputStream(file, Float.valueOf(starts.floatValue() / 1000), Float.valueOf(length.floatValue() / 1000));
    }

    /**
     * 
     * @param outputStream
     * @param starts
     * @param length
     * @param audioFormat
     * @return
     */
    public AudioInputStream findInputStreamInMils(ByteArrayOutputStream outputStream, Long starts, Long length, AudioFormat audioFormat) {
        byte audioData[] = outputStream.toByteArray();
        InputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
        AudioInputStream inputStream = new AudioInputStream(byteArrayInputStream, audioFormat, audioData.length / audioFormat.getFrameSize());
        return findInputStream(inputStream, Float.valueOf(starts.floatValue() / 1000), Float.valueOf(length.floatValue() / 1000));
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
            throw new ProcessingException(e);
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
        try {
            stream = AudioSystem.getAudioInputStream(fileURL);
        } catch (NullPointerException npe) {
            throw new IOException(npe);
        }
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
            } catch (LineUnavailableException lue) {
                log.error("Line Unavailable Exception", lue);
                throw new ProcessingException(lue);
            } catch (Exception e) {
                log.error("Exception", e);
                throw new ProcessingException(e);
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
            if (line == null) {
                log.error("Output line not cretated.");
                return;
            }
            if (isPlaying()) {
                log.error("already playing");
                return;
            }
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
                line.drain();
                line.stop();
                line.close();
            } catch (IOException e) {
                setPlaying(false);
                log.error(e);
            }
            setPlaying(false);
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
