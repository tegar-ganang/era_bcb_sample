package eu.cherrytree.paj.sound.openal;

import java.nio.ByteBuffer;
import java.util.Vector;
import com.jogamp.openal.AL;
import eu.cherrytree.paj.gui.Console;
import eu.cherrytree.paj.sound.AudioStreamSource;
import eu.cherrytree.paj.sound.AudioStreamSource.AudioStreamEndException;
import eu.cherrytree.paj.sound.AudioStreamSource.AudioStreamErrorException;
import eu.cherrytree.paj.sound.OggAudioStream;
import eu.cherrytree.paj.utilities.BufferUtil;

class OpenALSoundStreamThread extends Thread {

    private enum StreamState {

        STATE_PLAYING, STATE_PAUSED, STATE_RESTART, STATE_STOP
    }

    private AL al;

    private int[] buffers = new int[2];

    private int source;

    private int format;

    private String file;

    private StreamState currentState = StreamState.STATE_PLAYING;

    private boolean looped = false;

    private OpenALSoundStream caller;

    private AudioStreamSource stream;

    OpenALSoundStreamThread(String file, AL al, OpenALSoundStream caller) {
        this.al = al;
        this.file = file;
        this.caller = caller;
        start();
    }

    public void run() {
        open();
        playback();
        while (currentState != StreamState.STATE_STOP) {
            if (currentState == StreamState.STATE_PLAYING) {
                if (update()) {
                    if (!playing()) playback();
                } else if (currentState == StreamState.STATE_PLAYING) break;
            } else if (currentState == StreamState.STATE_RESTART) {
                release();
                open();
                playback();
                currentState = StreamState.STATE_PLAYING;
            }
        }
        release();
    }

    private void open() {
        if (file.endsWith(".ogg")) stream = new OggAudioStream(file);
        if (stream.getChannels() == 1) format = AL.AL_FORMAT_MONO16; else format = AL.AL_FORMAT_STEREO16;
        int[] t_s = new int[1];
        al.alGenBuffers(2, buffers, 0);
        al.alGenSources(1, t_s, 0);
        source = t_s[0];
    }

    private void release() {
        int[] t_s = { source };
        al.alSourceStop(source);
        empty();
        al.alDeleteSources(1, t_s, 0);
        al.alDeleteBuffers(2, buffers, 0);
        stream.close();
    }

    private boolean playback() {
        if (playing()) return true;
        if (!stream(buffers[0])) return false;
        if (!stream(buffers[1])) return false;
        al.alSourceQueueBuffers(source, 2, buffers, 0);
        al.alSourcePlay(source);
        return true;
    }

    private boolean playing() {
        int[] state = new int[1];
        al.alGetSourcei(source, AL.AL_SOURCE_STATE, state, 0);
        return (state[0] == AL.AL_PLAYING);
    }

    private boolean update() {
        int[] processed = new int[1];
        boolean active = true;
        al.alGetSourcei(source, AL.AL_BUFFERS_PROCESSED, processed, 0);
        while (processed[0]-- > 0) {
            int[] buffer = new int[1];
            al.alSourceUnqueueBuffers(source, 1, buffer, 0);
            active = stream(buffer[0]);
            al.alSourceQueueBuffers(source, 1, buffer, 0);
        }
        return active;
    }

    private boolean stream(int buffer) {
        try {
            Vector<Byte> s_data = stream.streamData();
            int size = s_data.size();
            ByteBuffer data = BufferUtil.newByteBuffer(size);
            for (int i = 0; i < size; i++) data.put((Byte) s_data.get(i));
            data.flip();
            al.alBufferData(buffer, format, data, size, stream.getSampleRate());
            return true;
        } catch (AudioStreamEndException e) {
            if (looped) currentState = StreamState.STATE_RESTART; else {
                currentState = StreamState.STATE_STOP;
                caller.streamEnded();
            }
        } catch (AudioStreamErrorException e) {
            Console.print("Error reading in audio stream " + file);
        }
        return false;
    }

    private void empty() {
        int[] queued = new int[1];
        al.alGetSourcei(source, AL.AL_BUFFERS_QUEUED, queued, 0);
        while (queued[0]-- > 0) {
            int[] buffer = new int[1];
            al.alSourceUnqueueBuffers(source, 1, buffer, 0);
        }
    }

    void pause() {
        currentState = StreamState.STATE_PAUSED;
        al.alSourceStop(source);
        empty();
    }

    void unPause() {
        currentState = StreamState.STATE_PLAYING;
    }

    void stopPlayback() {
        currentState = StreamState.STATE_STOP;
    }

    void setVolume(float volume) {
        al.alSourcef(source, AL.AL_GAIN, volume);
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
