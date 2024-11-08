package eu.cherrytree.paj.sound.simple;

import java.util.Vector;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import eu.cherrytree.paj.file.EndianConverter;
import eu.cherrytree.paj.gui.Console;
import eu.cherrytree.paj.sound.AudioStreamSource;
import eu.cherrytree.paj.sound.AudioStreamSource.AudioStreamEndException;
import eu.cherrytree.paj.sound.AudioStreamSource.AudioStreamErrorException;
import eu.cherrytree.paj.sound.OggAudioStream;

public class SimpleSoundStreamThread extends Thread {

    private enum StreamState {

        STATE_PLAYING, STATE_PAUSED, STATE_RESTART, STATE_STOP
    }

    static Mixer mixer = null;

    private String file;

    private SimpleSoundStream caller;

    private AudioStreamSource stream;

    private SourceDataLine line;

    private StreamState currentState = StreamState.STATE_PLAYING;

    private boolean looped = false;

    private float volume = 1.0f;

    private boolean systemVolumeControlSupported = true;

    private FloatControl volumeControl = null;

    private FloatControl gainControl = null;

    private byte[] zeroData;

    public SimpleSoundStreamThread(String file, SimpleSoundStream caller) {
        this.file = file;
        this.caller = caller;
        systemVolumeControlSupported = mixer.isControlSupported(FloatControl.Type.MASTER_GAIN) || mixer.isControlSupported(FloatControl.Type.VOLUME);
        start();
    }

    public void run() {
        open();
        createLine();
        while (currentState != StreamState.STATE_STOP) {
            if (currentState == StreamState.STATE_PLAYING) {
                if (!line.isActive()) line.start();
                stream();
            } else if (currentState == StreamState.STATE_RESTART) {
                stream.close();
                open();
                currentState = StreamState.STATE_PLAYING;
            } else if (currentState == StreamState.STATE_PAUSED) {
                if (line.isActive()) line.stop();
                line.write(zeroData, 0, zeroData.length);
            }
        }
        close();
    }

    private void stream() {
        try {
            Vector<Byte> data = stream.streamData();
            byte[] buf = new byte[data.size()];
            if (!systemVolumeControlSupported) {
                byte[] temp = new byte[4];
                for (int i = 0; i < buf.length / 4; i++) {
                    temp[0] = data.get(i * 4 + 0);
                    temp[1] = data.get(i * 4 + 1);
                    temp[2] = data.get(i * 4 + 2);
                    temp[3] = data.get(i * 4 + 3);
                    temp = scaleInt(temp, volume);
                    buf[i * 4 + 0] = temp[0];
                    buf[i * 4 + 1] = temp[1];
                    buf[i * 4 + 2] = temp[2];
                    buf[i * 4 + 3] = temp[3];
                }
            } else {
                for (int i = 0; i < buf.length; i++) buf[i] = data.get(i);
            }
            line.write(buf, 0, buf.length);
        } catch (AudioStreamEndException e) {
            if (looped) currentState = StreamState.STATE_RESTART; else {
                currentState = StreamState.STATE_STOP;
                caller.streamEnded();
            }
        } catch (AudioStreamErrorException e) {
            currentState = StreamState.STATE_STOP;
            e.printStackTrace();
        }
    }

    private byte[] scaleInt(byte[] in, float scale) {
        float val = (float) EndianConverter.toInt(in) * scale;
        return EndianConverter.toByteArray((int) val);
    }

    private void open() {
        if (file.endsWith(".ogg")) stream = new OggAudioStream(file);
    }

    private void createLine() {
        int channels = stream.getChannels();
        int bits = 32;
        int frame = 0;
        int frequency = stream.getSampleRate();
        frame = channels * (bits / 8);
        zeroData = new byte[frame * 100];
        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, frequency / 2, bits, channels, frame, frequency / 2, false);
        SourceDataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        try {
            line = (SourceDataLine) mixer.getLine(info);
            line.open(format);
            line.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            try {
                gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (line.isControlSupported(FloatControl.Type.VOLUME)) {
            try {
                volumeControl = (FloatControl) line.getControl(FloatControl.Type.VOLUME);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void close() {
        line.stop();
        line.drain();
        line.close();
        stream.close();
    }

    void pause() {
        currentState = StreamState.STATE_PAUSED;
    }

    void unPause() {
        currentState = StreamState.STATE_PLAYING;
    }

    void stopPlayback() {
        currentState = StreamState.STATE_STOP;
    }

    void setVolume(float volume) {
        this.volume = volume;
        if (systemVolumeControlSupported) {
            if (volumeControl != null) {
            } else if (gainControl != null) {
            } else {
                Console.print("Can't set volume with system controls.");
                systemVolumeControlSupported = false;
            }
        }
    }

    boolean isPlaying() {
        return currentState == StreamState.STATE_PLAYING || currentState == StreamState.STATE_RESTART;
    }

    public boolean isLooped() {
        return looped;
    }

    public void setLooped(boolean looped) {
        this.looped = looped;
    }
}
