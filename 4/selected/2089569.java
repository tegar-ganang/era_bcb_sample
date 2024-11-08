package plugins.voipplugin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import org.xiph.speex.SpeexEncoder;

/**
 * @author Marc Miltenberger
 * Records data from a microphone and compresses it.
 */
public class Recorder {

    private static String streamSync = "recordersync";

    private SpeexEncoder speexEnc;

    private ByteArrayOutputStream stream = new ByteArrayOutputStream();

    public static final boolean bigendian = false;

    public static final AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000, 16, 1, 2, 8000F, bigendian);

    public static final int band = 1;

    private int quality = 10;

    private TargetDataLine line;

    private Timer tmrRecord = new Timer();

    public Recorder() throws LineUnavailableException {
        record(format);
    }

    public ByteArrayOutputStream getOutputStream() throws IOException {
        synchronized (streamSync) {
            byte[] b = stream.toByteArray();
            ByteArrayOutputStream output = new ByteArrayOutputStream(b.length);
            output.write(b);
            stream.reset();
            return output;
        }
    }

    public void improveQuality(boolean improve) {
        if (improve) {
            if (quality != 10) quality++;
        } else {
            if (quality != 0) quality--;
        }
        synchronized (speexEnc) {
            speexEnc.init(band, quality, (int) format.getSampleRate(), format.getChannels());
            speexEnc.getEncoder().setComplexity(2);
        }
    }

    private byte[] encodeWithSPEEX(byte[] buff) {
        if (speexEnc == null) {
            speexEnc = new SpeexEncoder();
            speexEnc.init(band, quality, (int) format.getSampleRate(), format.getChannels());
            speexEnc.getEncoder().setComplexity(2);
        }
        if (bigendian) switchEndianness(buff);
        byte[] out;
        synchronized (speexEnc) {
            speexEnc.processData(buff, 0, buff.length);
            out = new byte[speexEnc.getProcessedDataByteSize()];
            speexEnc.getProcessedData(out, 0);
        }
        return out;
    }

    void record(AudioFormat format) throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!info.isFormatSupported(format)) System.err.println("Not supported");
        line = (TargetDataLine) AudioSystem.getLine(info);
        line.stop();
        while (!line.isOpen()) {
            try {
                line.open(format);
                line.start();
                Thread.sleep(50);
            } catch (Exception e) {
            }
        }
        tmrRecord = new Timer();
        tmrRecord.schedule(new TimerTask() {

            @Override
            public void run() {
                byte[] buff = new byte[640];
                line.read(buff, 0, buff.length);
                try {
                    byte[] bwrite = encodeWithSPEEX(buff);
                    synchronized (streamSync) {
                        stream.write(bwrite);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 150, 5);
    }

    public static void switchEndianness(byte[] samples) {
        for (int i = 0; i < samples.length; i += 2) {
            byte tmp = samples[i];
            samples[i] = samples[i + 1];
            samples[i + 1] = tmp;
        }
    }

    public void close() {
        tmrRecord.cancel();
        line.close();
    }

    public int getQuality() {
        return quality;
    }
}
