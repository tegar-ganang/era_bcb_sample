package plugins.voipplugin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import org.xiph.speex.SpeexDecoder;

/**
 * @author marc
 *
 */
public class Player {

    static SpeexDecoder speexDec = null;

    static ByteArrayOutputStream stream = new ByteArrayOutputStream();

    String streamSync = "playersync" + new Random().nextDouble();

    private SourceDataLine line;

    private Timer player;

    public Player() {
        try {
            play(Recorder.format);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeToInputStream(byte[] bytearray) {
        synchronized (streamSync) {
            try {
                if (stream.size() > 1024 * 1024 * 10) return;
                stream.write(bytearray);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static byte[] decodeWithSPEEX(byte[] buff) {
        if (speexDec == null) {
            speexDec = new SpeexDecoder();
            speexDec.init(Recorder.band, (int) Recorder.format.getSampleRate(), Recorder.format.getChannels(), true);
        }
        try {
            speexDec.processData(buff, 0, buff.length);
        } catch (Exception ex) {
        }
        byte[] ret = new byte[speexDec.getProcessedDataByteSize()];
        speexDec.getProcessedData(ret, 0);
        if (Recorder.bigendian) Recorder.switchEndianness(ret);
        return ret;
    }

    private void play(AudioFormat format) throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        line = (SourceDataLine) AudioSystem.getLine(info);
        line.stop();
        try {
            line.open(format);
        } catch (Exception e) {
        }
        line.start();
        player = new Timer();
        player.schedule(new TimerTask() {

            @Override
            public void run() {
                if (!line.isOpen()) return;
                if (stream.size() == 0) {
                    try {
                        Thread.sleep(2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                byte[] bEncoded;
                synchronized (streamSync) {
                    bEncoded = stream.toByteArray();
                }
                byte[] wavedata = decodeWithSPEEX(bEncoded);
                synchronized (streamSync) {
                    stream = new ByteArrayOutputStream();
                }
                line.write(wavedata, 0, wavedata.length);
            }
        }, 100, 5);
    }

    public void close() {
        player.cancel();
        line.close();
    }
}
