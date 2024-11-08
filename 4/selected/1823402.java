package org.mobicents.media.server.component.audio;

import java.io.IOException;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.memory.Frame;

/**
 *
 * @author kulikov
 */
public class SoundCard extends AbstractSink {

    private static final AudioFormat LINEAR = FormatFactory.createAudioFormat("LINEAR", 8000, 8, 1);

    private static final Formats formats = new Formats();

    private static final Encoding GSM_ENCODING = new Encoding("GSM0610");

    static {
        formats.add(LINEAR);
    }

    private boolean first;

    private SourceDataLine sourceDataLine = null;

    private javax.sound.sampled.AudioFormat audioFormat = null;

    public SoundCard(Scheduler scheduler) {
        super("soundcard", scheduler);
    }

    @Override
    public void start() {
        first = true;
        super.start();
    }

    @Override
    public void onMediaTransfer(Frame frame) throws IOException {
        System.out.println("Receive " + frame.getFormat() + ", len=" + frame.getLength() + ", header=" + frame.getHeader());
        if (first) {
            first = false;
            AudioFormat fmt = (AudioFormat) frame.getFormat();
            if (fmt == null) {
                return;
            }
            float sampleRate = (float) fmt.getSampleRate();
            int sampleSizeInBits = fmt.getSampleSize();
            int channels = fmt.getChannels();
            int frameSize = (fmt.getSampleSize() / 8);
            float frameRate = 1;
            boolean bigEndian = false;
            Encoding encoding = getEncoding(fmt.getName().toString());
            frameSize = (channels == AudioSystem.NOT_SPECIFIED || sampleSizeInBits == AudioSystem.NOT_SPECIFIED) ? AudioSystem.NOT_SPECIFIED : ((sampleSizeInBits + 7) / 8) * channels;
            audioFormat = new javax.sound.sampled.AudioFormat(encoding, sampleRate, sampleSizeInBits, channels, frameSize, sampleRate, bigEndian);
            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
            try {
                sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
                sourceDataLine.open(audioFormat);
                sourceDataLine.start();
            } catch (LineUnavailableException e) {
                this.stop();
            } catch (IllegalArgumentException e) {
                this.stop();
            }
        }
        byte[] data = frame.getData();
        try {
            sourceDataLine.write(data, frame.getOffset(), frame.getLength());
        } catch (IllegalArgumentException e) {
        } catch (ArrayIndexOutOfBoundsException e) {
        }
    }

    @Override
    public Formats getNativeFormats() {
        return formats;
    }

    private javax.sound.sampled.AudioFormat.Encoding getEncoding(String encodingName) {
        if (encodingName.equalsIgnoreCase("pcma")) {
            return javax.sound.sampled.AudioFormat.Encoding.ALAW;
        } else if (encodingName.equalsIgnoreCase("pcmu")) {
            return javax.sound.sampled.AudioFormat.Encoding.ULAW;
        } else if (encodingName.equalsIgnoreCase("gsm")) {
            return GSM_ENCODING;
        } else {
            return javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
        }
    }
}
