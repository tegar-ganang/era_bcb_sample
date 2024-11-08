package glsound;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.lwjgl.openal.AL10;

/**
 * $Id: WaveData.java,v 1.15 2004/02/26 21:51:58 matzon Exp $
 *
 * Utitlity class for loading wavefiles.
 *
 * @author Brian Matzon <brian@matzon.dk>
 * @version $Revision: 1.15 $
 */
public class WaveData {

    /** actual wave data */
    public final ByteBuffer data;

    /** format type of data */
    public final int format;

    /** sample rate of data */
    public final int samplerate;

    /**
	 * Creates a new WaveData
	 *
	 * @param data actual wavedata
	 * @param format format of wave data
	 * @param samplerate sample rate of data
	 */
    private WaveData(ByteBuffer data, int format, int samplerate) {
        this.data = data;
        this.format = format;
        this.samplerate = samplerate;
    }

    /**
	 * Disposes the wavedata
	 */
    public void dispose() {
        data.clear();
    }

    /**
	 * Creates a WaveData container from the specified filename
	 *
	 * @param filepath path to file (relative, and in classpath)
	 * @return WaveData containing data, or null if a failure occured
	 */
    public static WaveData create(String filepath) {
        try {
            return create(AudioSystem.getAudioInputStream(new BufferedInputStream(WaveData.class.getClassLoader().getResourceAsStream(filepath))));
        } catch (Exception e) {
            System.out.println("WaveData.create(): Unable to load file: " + filepath);
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * Creates a WaveData container from the specified bytes
	 *
	 * @param buffer array of bytes containing the complete wave file
	 * @return WaveData containing data, or null if a failure occured
	 */
    public static WaveData create(byte[] buffer) {
        try {
            return create(AudioSystem.getAudioInputStream(new BufferedInputStream(new ByteArrayInputStream(buffer))));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * Creates a WaveData container from the specified stream
	 *
	 * @param ais AudioInputStream to read from
	 * @return WaveData containing data, or null if a failure occured
	 */
    public static WaveData create(AudioInputStream ais) {
        AudioFormat audioformat = ais.getFormat();
        int channels = 0;
        if (audioformat.getChannels() == 1) {
            if (audioformat.getSampleSizeInBits() == 8) {
                channels = AL10.AL_FORMAT_MONO8;
            } else if (audioformat.getSampleSizeInBits() == 16) {
                channels = AL10.AL_FORMAT_MONO16;
            } else {
                System.out.println("WaveData.create(): Illegal sample size");
            }
        } else if (audioformat.getChannels() == 2) {
            if (audioformat.getSampleSizeInBits() == 8) {
                channels = AL10.AL_FORMAT_STEREO8;
            } else if (audioformat.getSampleSizeInBits() == 16) {
                channels = AL10.AL_FORMAT_STEREO16;
            } else {
                System.out.println("WaveData.create(): Illegal sample size");
            }
        } else {
            System.out.println("WaveData.create(): Only mono or stereo is supported");
        }
        byte[] buf = new byte[audioformat.getChannels() * (int) ais.getFrameLength() * audioformat.getSampleSizeInBits() / 8];
        int read = 0, total = 0;
        try {
            while ((read = ais.read(buf, total, buf.length - total)) != -1 && total < buf.length) {
                total += read;
            }
        } catch (IOException ioe) {
            return null;
        }
        ByteBuffer buffer = convertAudioBytes(buf, audioformat.getSampleSizeInBits() == 16);
        WaveData wavedata = new WaveData(buffer, channels, (int) audioformat.getSampleRate());
        try {
            ais.close();
        } catch (IOException ioe) {
        }
        return wavedata;
    }

    private static ByteBuffer convertAudioBytes(byte[] audio_bytes, boolean two_bytes_data) {
        ByteBuffer dest = ByteBuffer.allocateDirect(audio_bytes.length);
        dest.order(ByteOrder.nativeOrder());
        ByteBuffer src = ByteBuffer.wrap(audio_bytes);
        src.order(ByteOrder.LITTLE_ENDIAN);
        if (two_bytes_data) {
            ShortBuffer dest_short = dest.asShortBuffer();
            ShortBuffer src_short = src.asShortBuffer();
            while (src_short.hasRemaining()) dest_short.put(src_short.get());
        } else {
            while (src.hasRemaining()) dest.put(src.get());
        }
        dest.rewind();
        return dest;
    }
}
