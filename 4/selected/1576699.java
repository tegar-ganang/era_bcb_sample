package seventhsense.sound.engine.input;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.tritonus.share.sampled.file.TAudioFileFormat;

/**
 * Decoder stream for wrapping the java audio input
 * 
 * @author Parallan
 *
 */
public class JavaSoundDecoderStream implements IAudioInputStream {

    private static final Logger LOGGER = Logger.getLogger(JavaSoundDecoderStream.class.getName());

    private AudioInputStream _audioStream;

    private final AudioFormat _decodedFormat;

    private final AudioFileFormat _fileFormat;

    private final File _file;

    private long _position;

    /**
	 * Creates the decoder with a given file, that will be decoded
	 * 
	 * @param file file to play
	 * @throws IOException if an IO error occured
	 * @throws UnsupportedAudioFileException if the audio file was not recognized
	 */
    public JavaSoundDecoderStream(final File file) throws IOException {
        _file = file;
        try {
            _fileFormat = AudioSystem.getAudioFileFormat(_file);
        } catch (UnsupportedAudioFileException e) {
            throw new IOException(e);
        }
        final AudioFormat baseFormat = _fileFormat.getFormat();
        LOGGER.log(Level.FINE, "Source Format: " + baseFormat);
        int sampleSize;
        AudioFormat.Encoding encoding;
        if (baseFormat.getSampleSizeInBits() == 8) {
            sampleSize = 8;
            encoding = AudioFormat.Encoding.PCM_UNSIGNED;
        } else {
            sampleSize = 16;
            encoding = AudioFormat.Encoding.PCM_SIGNED;
        }
        _decodedFormat = new AudioFormat(encoding, baseFormat.getSampleRate(), sampleSize, baseFormat.getChannels(), baseFormat.getChannels() * sampleSize / 8, baseFormat.getSampleRate(), false);
        resetStream();
    }

    /**
	 * Resets the internal stream to its origin for replay
	 * 
	 * @throws IOException if an io exception occured, should not happen!
	 */
    private void resetStream() throws IOException {
        if (_audioStream != null) {
            _audioStream.close();
        }
        try {
            final AudioInputStream stream = AudioSystem.getAudioInputStream(_file);
            _audioStream = AudioSystem.getAudioInputStream(_decodedFormat, stream);
            _position = 0;
        } catch (UnsupportedAudioFileException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int read(final byte[] buffer, final int off, final int len) throws IOException {
        final int result = _audioStream.read(buffer, off, len);
        if (result > 0) {
            _position += result;
        }
        return result;
    }

    @Override
    public void close() {
        try {
            _audioStream.close();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
        _audioStream = null;
        _position = 0;
    }

    @Override
    public long getPosition() {
        return _position;
    }

    @Override
    public void setPosition(final long position) throws IOException {
        if (position < _position) {
            try {
                resetStream();
            } catch (IOException e) {
                throw new IllegalStateException("File exceptions should be caught on construction", e);
            }
        }
        final long bytesToSkip = position;
        final byte[] dummyBuffer = new byte[getFrameSize() * getSampleRate()];
        long skippedBytes = _position;
        while (skippedBytes < bytesToSkip) {
            final int bytesToRead = (int) Math.min(dummyBuffer.length, bytesToSkip - skippedBytes);
            final int readBytes = _audioStream.read(dummyBuffer, 0, (int) bytesToRead);
            if (readBytes < 0) {
                LOGGER.log(Level.FINE, "read < 0: " + readBytes);
                break;
            }
            skippedBytes += readBytes;
        }
        _position = skippedBytes;
    }

    @Override
    public long getLength() {
        if (_fileFormat instanceof TAudioFileFormat) {
            final Map<?, ?> props = ((TAudioFileFormat) _fileFormat).properties();
            final Long duration = (Long) props.get("duration");
            if (duration != null) {
                final long uSec = duration.longValue();
                return uSec * (getSampleRate() * getFrameSize()) / 1000000;
            }
        } else if (_fileFormat.getFrameLength() > 0) {
            return _fileFormat.getFrameLength() * getFrameSize();
        } else if ((_audioStream != null) && (_audioStream.getFrameLength() > 0)) {
            return _audioStream.getFrameLength() * getFrameSize();
        }
        return 0;
    }

    @Override
    public int getSampleSize() {
        return _decodedFormat.getSampleSizeInBits() / 8;
    }

    @Override
    public int getFrameSize() {
        return _decodedFormat.getFrameSize();
    }

    @Override
    public int getChannels() {
        return _decodedFormat.getChannels();
    }

    @Override
    public int getSampleRate() {
        return (int) _decodedFormat.getSampleRate();
    }

    @Override
    public String getName() {
        return _fileFormat.getType().toString();
    }

    @Override
    public String toString() {
        return _file.toString();
    }
}
