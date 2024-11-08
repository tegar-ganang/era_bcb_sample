package sdl.audio;

/**
 * Format of the SDL audio stream.  All AudioPlay objects must be
 * converted into the format of the current audio stream so they may
 * be mixed and played.
 */
public class SDLAudioSpec {

    private int m_Freq;

    private short m_Format;

    private byte m_Channels;

    private byte m_Silence;

    private short m_Samples;

    private int m_Size;

    private int m_Handle = 0;

    public SDLAudioSpec(int handle) {
        m_Handle = handle;
        if (m_Handle != 0) {
            loadSDLAudioSpec(m_Handle);
        }
    }

    public SDLAudioSpec(int freq, short format, byte channels, byte silence, short samples, int size) {
        m_Freq = freq;
        m_Format = format;
        m_Channels = channels;
        m_Silence = silence;
        m_Samples = samples;
        m_Size = size;
    }

    public String toString() {
        String str = "";
        str += " Freq: " + m_Freq;
        str += " Format: " + m_Format;
        str += " Channels: " + m_Channels;
        str += " Silence: " + m_Silence;
        str += " Samples: " + m_Samples;
        str += " Size: " + m_Size;
        return str;
    }

    public int getHandle() {
        return m_Handle;
    }

    public int save() {
        m_Handle = saveSDLAudioSpec(m_Handle);
        return m_Handle;
    }

    public void free() {
        if (m_Handle != 0) {
            freeSDLAudioSpec(m_Handle);
        }
    }

    public short getFormat() {
        return m_Format;
    }

    public byte getChannels() {
        return m_Channels;
    }

    public int getFreq() {
        return m_Freq;
    }

    private native void loadSDLAudioSpec(int handle);

    private native int saveSDLAudioSpec(int handle);

    private native void freeSDLAudioSpec(int handle);
}
