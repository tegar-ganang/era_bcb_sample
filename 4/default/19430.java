import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;

public class Plei extends Thread {

    private WaveformPanelContainer wfpc;

    private SourceDataLine sdline;

    private DataLine.Info info;

    private Boolean paused, stopped, firstBlock;

    private Cursor cursor;

    private DPlayer dplayer;

    private AudioFormat decodedFormat;

    private AudioArray audioArray;

    private int numChannels;

    private Mixer mixer;

    public Plei(WaveformPanelContainer wfpc, DPlayer dplayer) {
        this.wfpc = wfpc;
        this.audioArray = wfpc.getAudioArray();
        numChannels = audioArray.getNumChannels();
        this.dplayer = dplayer;
        paused = true;
        stopped = false;
        firstBlock = true;
        decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, audioArray.getSampleRate(), audioArray.getSampleSizeInBits(), numChannels, audioArray.getFrameSize(), audioArray.getFrameRate(), false);
        try {
            info = new DataLine.Info(SourceDataLine.class, audioArray.getFormat());
            sdline = (SourceDataLine) AudioSystem.getLine(info);
            sdline.open(decodedFormat);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        cursor = new Cursor(wfpc, this);
        cursor.start();
        try {
            if (sdline != null) {
                short s[][];
                while ((s = readNextBlock()) != null) {
                    if (paused) {
                        synchronized (this) {
                            wait();
                        }
                    }
                    if (stopped) {
                        break;
                    }
                    writeNextBlock(s);
                }
                dplayer.alertEndPlay();
                firstBlock = true;
                closeAudio();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    private void writeNextBlock(short[][] block) {
        byte[] data = toByteArray(block);
        try {
            sdline.write(data, 0, data.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private short[][] readNextBlock() {
        return audioArray.read();
    }

    private byte[] toByteArray(short[][] block) {
        int length = block[0].length;
        byte[] b = new byte[2 * numChannels * length];
        int bi = 0, si = 0;
        short s;
        while (length-- > 0) {
            for (short[] ch : block) {
                s = ch[si];
                b[bi++] = (byte) s;
                b[bi++] = (byte) (s >> 8);
            }
            si++;
        }
        return b;
    }

    public void playLine() {
        paused = false;
        stopped = false;
        synchronized (this) {
            notifyAll();
        }
        sdline.start();
        cursor.play();
    }

    public void pauseLine() {
        paused = true;
        stopped = false;
        sdline.stop();
        cursor.pause();
    }

    public void stopLine() {
        paused = false;
        stopped = true;
        audioArray.reset();
        sdline.stop();
        cursor.kill();
    }

    private void closeAudio() {
        audioArray.reset();
        sdline.drain();
        sdline.stop();
        sdline.close();
        cursor.kill();
    }

    public int getFramePos() {
        return sdline.getFramePosition();
    }

    public int getChannelLength() {
        return audioArray.getChannelLength();
    }
}
