package sdl4java.audio;

/**
 * The AudioSpec structure is used to describe the format of some audio data.
 */
public class AudioSpec {

    /** Unsigned 8-bit samples */
    public static final int AUDIO_U8 = 0x0008;

    /** Signed 8-bit samples */
    public static final int AUDIO_S8 = 0x8008;

    /** Unsigned 16-bit samples */
    public static final int AUDIO_U16LSB = 0x0010;

    /** Signed 16-bit samples */
    public static final int AUDIO_S16LSB = 0x8010;

    /** As above, but big-endian byte order */
    public static final int AUDIO_U16MSB = 0x1010;

    /** As above, but big-endian byte order */
    public static final int AUDIO_S16MSB = 0x9010;

    public static final int AUDIO_U16 = AUDIO_U16LSB;

    public static final int AUDIO_S16 = AUDIO_S16LSB;

    /** Native audio byte ordering */
    public static final int AUDIO_U16SYS = AUDIO_U16LSB;

    /** Native audio byte ordering */
    public static final int AUDIO_S16SYS = AUDIO_S16LSB;

    /** CD Quality frequency */
    public static final int CD_QUALITY = 44100;

    /** Radio Quality frequency */
    public static final int RADIO_QUALITY = 22050;

    /** Telephone Quality frequency */
    public static final int TELEPHONE_QUALITY = 11025;

    /** Stereo channels */
    public static final byte STEREO = (byte) 2;

    /** Mono channel */
    public static final byte MONO = (byte) 1;

    protected int pointer = 0;

    /**
	 * Creates a new uninitialized AudioSpec structure.
	 */
    public AudioSpec() {
        pointer = n_init();
    }

    protected AudioSpec(int ptr) {
        pointer = ptr;
    }

    /**
	 * The Audio Stream Listener
	 */
    protected AudioStreamListener listener;

    /**
	 * Gets the audio frequency in samples per second
	 * @return the frequency
	 */
    public int getFreq() {
        return n_getFreq(pointer);
    }

    /**
	 * Sets the audio frequency in samples per second
	 * @param f the frequency
	 */
    public void setFreq(int f) {
        n_setFreq(pointer, f);
    }

    /**
	 * Gets the audio data format
	 * @return the data format
	 */
    public int getFormat() {
        return n_getFormat(pointer);
    }

    /**
	 * Sets the audio data format
	 * @param f the format
	 */
    public void setFormat(int f) {
        n_setFormat(pointer, f);
    }

    /**
	 * Gets the number of channels
	 * @return 1 mono, 2 stereo
	 */
    public byte getChannels() {
        return n_getChannels(pointer);
    }

    /**
	 * Gets the number of channels
	 * @param c 1 for mono, 2 for stereo
	 */
    public void setChannels(byte c) {
        n_setChannels(pointer, c);
    }

    /**
	 * Gets the audio buffer silence value (calculated)
	 * @return the buffer silence
	 */
    public byte getSilence() {
        return n_getSilence(pointer);
    }

    /**
	 * Sets the audio buffer silence value. <br>
	 * This value is calculated so it shouldn't be set.
	 * @param s the buffer silence
	 */
    public void setSilence(byte s) {
        n_setSilence(pointer, s);
    }

    /**
	 * Gets the audio buffer size in samples
	 * @return the samples
	 */
    public short getSamples() {
        return n_getSamples(pointer);
    }

    /**
	 * Sets the audio buffer size in samples
	 * @param s the number of samples
	 */
    public void setSamples(short s) {
        n_setSamples(pointer, s);
    }

    /**
	 * Gets the audio buffer size in bytes (calculated)
	 * @return the number of bytes
	 */
    public int getSize() {
        return n_getSize(pointer);
    }

    /**
	 * Sets the audio buffer size in bytes<br>
	 * This value is calculated and shouldn't be set.
	 * @param s the number of bytes that reside in buffer
	 */
    public void setSize(int s) {
        n_setSize(pointer, s);
    }

    public void setAudioStreamListener(AudioStreamListener listener) {
        this.listener = listener;
    }

    public AudioStreamListener getAudioStreamListener() {
        return listener;
    }

    /**
	 * This returns a string representation of the buffer returning the value of its properties.
	 * @return the values that this spec holds.
	 */
    public String toString() {
        return "Frequency: " + getFreq() + "\nFormat: " + getFormat() + "\nChannels: " + getChannels() + "\nSilence: " + getSilence() + "\nSamples: " + getSamples() + "\nSize: " + getSize();
    }

    /**
	 * Function used to deliver the stream back to the listener
	 * @param s the stream that should be delivered to the listener
	 */
    protected void audioCallback(AudioStream s) {
        if (getAudioStreamListener() != null) {
            getAudioStreamListener().fillAudio(s);
        }
    }

    protected void finalize() throws Throwable {
        n_free(pointer);
        super.finalize();
    }

    private static native int n_init();

    private static native void n_free(int pointer);

    private static native int n_getFreq(int pointer);

    private static native void n_setFreq(int ptr, int f);

    private static native int n_getFormat(int pointer);

    private static native void n_setFormat(int ptr, int f);

    private static native byte n_getChannels(int pointer);

    private static native void n_setChannels(int ptr, byte c);

    private static native byte n_getSilence(int pointer);

    private static native void n_setSilence(int ptr, byte s);

    private static native short n_getSamples(int pointer);

    private static native void n_setSamples(int ptr, short s);

    private static native int n_getSize(int pointer);

    private static native void n_setSize(int ptr, int s);
}
