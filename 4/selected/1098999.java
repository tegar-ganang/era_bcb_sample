package s;

import static data.sounds.S_sfx;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import pooling.AudioChunkPool;
import data.sounds.sfxenum_t;
import doom.DoomStatus;

/**
 * A close recreation of the classic linux doom sound mixer.
 * 
 * PROS:
 * a) Very faithful to pitch and stereo effects, and original
 *    volume ramping.
 * b) Uses only one audioline and one playback thread
 * 
 * CONS:
 * a) May sound a bit off if production/consumption rates don't match
 * b) Sounds awful when mixing too many sounds together, just like the original.
 * 
 * @author Maes
 */
public class ClassicDoomSoundDriver extends AbstractSoundDriver {

    protected final Semaphore produce;

    protected final Semaphore consume;

    protected int chunk = 0;

    protected SourceDataLine line = null;

    protected HashMap<Integer, byte[]> cachedSounds = new HashMap<Integer, byte[]>();

    public ClassicDoomSoundDriver(DoomStatus DS, int numChannels) {
        super(DS, numChannels);
        channelleftvol_lookup = new int[numChannels][];
        channelrightvol_lookup = new int[numChannels][];
        channelstep = new int[numChannels];
        channelstepremainder = new int[numChannels];
        channels = new byte[numChannels][];
        p_channels = new int[numChannels];
        channelsend = new int[numChannels];
        produce = new Semaphore(1);
        consume = new Semaphore(1);
        produce.drainPermits();
    }

    /**
     * The global mixing buffer. Basically, samples from all active internal
     * channels are modifed and added, and stored in the buffer that is
     * submitted to the audio device. This is a 16-bit stereo signed PCM
     * mixbuffer. Memory order is LSB (?) and channel order is L-R-L-R...
     */
    protected final byte[] mixbuffer = new byte[MIXBUFFERSIZE * 2];

    /** The channel step amount... */
    protected final int[] channelstep;

    /** ... and a 0.16 bit remainder of last step. */
    protected final int[] channelstepremainder;

    /**
     * The channel data pointers, start and end. These were referred to as
     * "channels" in two different source files: s_sound.c and i_sound.c. In
     * s_sound.c they are actually channel_t (they are only informational). In
     * i_sound.c they are actual data channels.
     */
    protected byte[][] channels;

    /**
     * MAES: we'll have to use this for actual pointing. channels[] holds just
     * the data.
     */
    protected int[] p_channels;

    /**
     * The second one is supposed to point at "the end", so I'll make it an int.
     */
    protected int[] channelsend;

    /** Hardware left and right channel volume lookup. */
    protected final int[][] channelleftvol_lookup, channelrightvol_lookup;

    protected volatile boolean mixed = false;

    /**
     * This function loops all active (internal) sound channels, retrieves a
     * given number of samples from the raw sound data, modifies it according to
     * the current (internal) channel parameters, mixes the per channel samples
     * into the global mixbuffer, clamping it to the allowed range, and sets up
     * everything for transferring the contents of the mixbuffer to the (two)
     * hardware channels (left and right, that is). This function currently
     * supports only 16bit.
     */
    public void UpdateSound() {
        mixed = false;
        int sample = 0;
        int dl;
        int dr;
        int leftout;
        int rightout;
        int leftend;
        int step;
        int chan;
        leftout = 0;
        rightout = 2;
        step = 4;
        leftend = SAMPLECOUNT * step;
        for (chan = 0; chan < numChannels; chan++) {
            if (channels[chan] != null) mixed = true;
        }
        while (leftout < leftend) {
            dl = 0;
            dr = 0;
            for (chan = 0; chan < numChannels; chan++) {
                if (channels[chan] != null) {
                    int channel_pointer = p_channels[chan];
                    sample = 0x00FF & channels[chan][channel_pointer];
                    dl += channelleftvol_lookup[chan][sample];
                    dr += channelrightvol_lookup[chan][sample];
                    channelstepremainder[chan] += channelstep[chan];
                    channel_pointer += channelstepremainder[chan] >> 16;
                    channelstepremainder[chan] &= 0xFFFF;
                    if (channel_pointer >= channelsend[chan]) {
                        if (D) System.err.printf("Channel %d handle %d pointer %d thus done, stopping\n", chan, this.channelhandles[chan], channel_pointer);
                        channels[chan] = null;
                        channel_pointer = 0;
                    }
                    p_channels[chan] = channel_pointer;
                }
            }
            if (dl > 0x7fff) dl = 0x7fff; else if (dl < -0x8000) dl = -0x8000;
            mixbuffer[leftout] = (byte) ((dl & 0xFF00) >>> 8);
            mixbuffer[leftout + 1] = (byte) (dl & 0x00FF);
            if (dr > 0x7fff) dr = 0x7fff; else if (dr < -0x8000) dr = -0x8000;
            mixbuffer[rightout] = (byte) ((dr & 0xFF00) >>> 8);
            mixbuffer[rightout + 1] = (byte) (dr & 0x00FF);
            leftout += 4;
            rightout += 4;
        }
    }

    /**
     * SFX API Note: this was called by S_Init. However, whatever they did in
     * the old DPMS based DOS version, this were simply dummies in the Linux
     * version. See soundserver initdata().
     */
    @Override
    public void SetChannels(int numChannels) {
        int steptablemid = 128;
        for (int i = 0; i < this.numChannels; i++) {
            channels[i] = null;
        }
        generateStepTable(steptablemid);
        generateVolumeLUT();
    }

    protected MixServer SOUNDSRV;

    protected Thread SOUNDTHREAD;

    @Override
    public void InitSound() {
        int i;
        System.err.println("I_InitSound: ");
        AudioFormat format = new AudioFormat(SAMPLERATE, 16, 2, true, true);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        if (AudioSystem.isLineSupported(info)) try {
            line = (SourceDataLine) AudioSystem.getSourceDataLine(format);
            line.open(format, AUDIOLINE_BUFFER);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.print("Could not play signed 16 data\n");
        }
        if (line != null) System.err.print(" configured audio device\n");
        line.start();
        SOUNDSRV = new MixServer(line);
        SOUNDTHREAD = new Thread(SOUNDSRV);
        SOUNDTHREAD.start();
        System.err.print("I_InitSound: ");
        super.initSound8();
        System.err.print(" pre-cached all sound data\n");
        for (i = 0; i < MIXBUFFERSIZE; i += 4) {
            mixbuffer[i] = (byte) (((int) (0x7FFF * Math.sin(1.5 * Math.PI * (double) i / MIXBUFFERSIZE)) & 0xff00) >>> 8);
            mixbuffer[i + 1] = (byte) ((int) (0x7FFF * Math.sin(1.5 * Math.PI * (double) i / MIXBUFFERSIZE)) & 0xff);
            mixbuffer[i + 2] = (byte) (((int) (0x7FFF * Math.sin(1.5 * Math.PI * (double) i / MIXBUFFERSIZE)) & 0xff00) >>> 8);
            mixbuffer[i + 3] = (byte) ((int) (0x7FFF * Math.sin(1.5 * Math.PI * (double) i / MIXBUFFERSIZE)) & 0xff);
        }
        System.err.print("I_InitSound: sound module ready\n");
    }

    @Override
    protected int addsfx(int sfxid, int volume, int step, int seperation) {
        int i;
        int rc = -1;
        int oldest = DS.gametic;
        int oldestnum = 0;
        int slot;
        int rightvol;
        int leftvol;
        int broken = -1;
        if (sfxid == sfxenum_t.sfx_sawup.ordinal() || sfxid == sfxenum_t.sfx_sawidl.ordinal() || sfxid == sfxenum_t.sfx_sawful.ordinal() || sfxid == sfxenum_t.sfx_sawhit.ordinal() || sfxid == sfxenum_t.sfx_stnmov.ordinal() || sfxid == sfxenum_t.sfx_pistol.ordinal()) {
            for (i = 0; i < numChannels; i++) {
                if ((channels[i] != null) && (channelids[i] == sfxid)) {
                    this.p_channels[i] = 0;
                    this.channels[i] = null;
                    broken = i;
                    break;
                }
            }
        }
        if (broken >= 0) {
            i = broken;
            oldestnum = broken;
        } else for (i = 0; (i < numChannels) && (channels[i] != null); i++) {
            if (channelstart[i] < oldest) {
                oldestnum = i;
            }
        }
        oldest = channelstart[oldestnum];
        if (i == numChannels) slot = oldestnum; else slot = i;
        channels[slot] = S_sfx[sfxid].data;
        p_channels[slot] = 0;
        channelsend[slot] = lengths[sfxid];
        if (handlenums == 0) handlenums = 100;
        channelhandles[slot] = rc = handlenums--;
        channelstep[slot] = step;
        channelstepremainder[slot] = 0;
        channelstart[slot] = DS.gametic;
        seperation += 1;
        leftvol = volume - ((volume * seperation * seperation) >> 16);
        seperation = seperation - 257;
        rightvol = volume - ((volume * seperation * seperation) >> 16);
        if (rightvol < 0 || rightvol > 127) DS.I.Error("rightvol out of bounds");
        if (leftvol < 0 || leftvol > 127) DS.I.Error("leftvol out of bounds");
        channelleftvol_lookup[slot] = vol_lookup[leftvol];
        channelrightvol_lookup[slot] = vol_lookup[rightvol];
        channelids[slot] = sfxid;
        if (D) System.err.println(channelStatus());
        if (D) System.err.printf("Playing sfxid %d handle %d length %d vol %d on channel %d\n", sfxid, rc, S_sfx[sfxid].data.length, volume, slot);
        return rc;
    }

    @Override
    public void ShutdownSound() {
        boolean done = false;
        produce.release();
        int i;
        while (!done) {
            for (i = 0; i < numChannels && (channels[i] == null); i++) {
            }
            UpdateSound();
            SubmitSound();
            if (i == numChannels) done = true;
        }
        this.line.drain();
        SOUNDSRV.terminate = true;
        produce.release();
        try {
            SOUNDTHREAD.join();
        } catch (InterruptedException e) {
        }
        line.close();
    }

    protected class MixServer implements Runnable {

        public boolean terminate = false;

        public MixServer(SourceDataLine line) {
            this.auline = line;
        }

        private SourceDataLine auline;

        private ArrayBlockingQueue<AudioChunk> audiochunks = new ArrayBlockingQueue<AudioChunk>(BUFFER_CHUNKS * 2);

        public void addChunk(AudioChunk chunk) {
            audiochunks.offer(chunk);
        }

        public volatile int currstate = 0;

        public void run() {
            while (!terminate) {
                try {
                    produce.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int chunks = 0;
                int atMost = Math.min(ISound.BUFFER_CHUNKS, audiochunks.size());
                while (atMost-- > 0) {
                    AudioChunk chunk = null;
                    try {
                        chunk = audiochunks.take();
                    } catch (InterruptedException e1) {
                    }
                    auline.write(chunk.buffer, 0, MIXBUFFERSIZE);
                    chunks++;
                    chunk.free = true;
                    audiochunkpool.checkIn(chunk);
                }
                consume.release();
            }
        }
    }

    @Override
    public boolean SoundIsPlaying(int handle) {
        int c = getChannelFromHandle(handle);
        return (c != -2 && channels[c] == null);
    }

    /**
     * Internal use.
     * 
     * @param handle
     * @return the channel that has the handle, or -2 if none has it.
     */
    protected int getChannelFromHandle(int handle) {
        for (int i = 0; i < numChannels; i++) {
            if (channelhandles[i] == handle) return i;
        }
        return BUSY_HANDLE;
    }

    @Override
    public void StopSound(int handle) {
        int hnd = getChannelFromHandle(handle);
        if (hnd >= 0) {
            channels[hnd] = null;
            p_channels[hnd] = 0;
            this.channelhandles[hnd] = IDLE_HANDLE;
        }
    }

    @Override
    public void SubmitSound() {
        if (mixed) {
            silence = 0;
            AudioChunk gunk = audiochunkpool.checkOut();
            gunk.free = false;
            System.arraycopy(mixbuffer, 0, gunk.buffer, 0, MIXBUFFERSIZE);
            this.SOUNDSRV.addChunk(gunk);
            chunk++;
            if (consume.tryAcquire()) produce.release();
        } else {
            silence++;
            if (silence > ISound.BUFFER_CHUNKS * 5) {
                line.flush();
                silence = 0;
            }
        }
    }

    private int silence = 0;

    @Override
    public void UpdateSoundParams(int handle, int vol, int sep, int pitch) {
        int chan = this.getChannelFromHandle(handle);
        int leftvol = vol - ((vol * sep * sep) >> 16);
        sep = sep - 257;
        int rightvol = vol - ((vol * sep * sep) >> 16);
        if (rightvol < 0 || rightvol > 127) DS.I.Error("rightvol out of bounds");
        if (leftvol < 0 || leftvol > 127) DS.I.Error("leftvol out of bounds");
        channelleftvol_lookup[chan] = vol_lookup[leftvol];
        channelrightvol_lookup[chan] = vol_lookup[rightvol];
        this.channelstep[chan] = steptable[pitch];
        channelsend[chan] = this.lengths[this.channelids[chan]];
    }

    protected StringBuilder sb = new StringBuilder();

    public String channelStatus() {
        sb.setLength(0);
        for (int i = 0; i < numChannels; i++) {
            if (channels[i] != null) sb.append(i); else sb.append('-');
        }
        return sb.toString();
    }

    protected final AudioChunk SILENT_CHUNK = new AudioChunk();

    protected final AudioChunkPool audiochunkpool = new AudioChunkPool();
}
