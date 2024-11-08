package s;

import static data.sounds.S_sfx;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
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
 * A spiffy new sound system, based on the Classic sound driver.
 * It is entirely asynchronous (runs in its own thread) and even has its own timer.
 * This allows it to continue mixing even when the main loop is not responding 
 * (something which, arguably, could be achieved just with a timer calling
 * UpdateSound and SubmitSound). Uses message passing to deliver channel status
 * info, and mixed audio directly without using an intermediate buffer,
 * saving memory bandwidth.
 * 
 * PROS:
 * a) All those of ClassicSoundDriver plus:
 * b) Continues normal playback even under heavy CPU load, works smoother
 *    even on lower powered CPUs.
 * c) More efficient due to less copying of audio blocks.
 * c) Fewer audio glitches compared to ClassicSoundDriver.
 * 
 * CONS:
 * a) All those of ClassicSoundDriver plus regarding timing accuracy.
 * 
 * @author Maes
 */
public class SuperDoomSoundDriver extends AbstractSoundDriver {

    protected final Semaphore produce;

    protected final Semaphore consume;

    protected final Semaphore update_mixer;

    protected int chunk = 0;

    protected SourceDataLine line = null;

    protected HashMap<Integer, byte[]> cachedSounds = new HashMap<Integer, byte[]>();

    protected final Timer MIXTIMER;

    public SuperDoomSoundDriver(DoomStatus DS, int numChannels) {
        super(DS, numChannels);
        channels = new boolean[numChannels];
        produce = new Semaphore(1);
        consume = new Semaphore(1);
        update_mixer = new Semaphore(1);
        produce.drainPermits();
        update_mixer.drainPermits();
        this.MIXSRV = new MixServer(numChannels);
        MIXTIMER = new Timer(true);
        MIXTIMER.schedule(new SoundTimer(), 0, SOUND_PERIOD);
    }

    /**
     * The global mixing buffer. Basically, samples from all active internal
     * channels are modifed and added, and stored in the buffer that is
     * submitted to the audio device. This is a 16-bit stereo signed PCM
     * mixbuffer. Memory order is LSB (?) and channel order is L-R-L-R...
     */
    protected byte[] mixbuffer;

    /** These are still defined here to decouple them from the mixer's 
     *  ones, however they serve  more as placeholders/status indicators;
     */
    protected volatile boolean[] channels;

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
            channels[i] = false;
        }
        generateStepTable(steptablemid);
        generateVolumeLUT();
    }

    protected PlaybackServer SOUNDSRV;

    protected final MixServer MIXSRV;

    protected Thread MIXTHREAD;

    protected Thread SOUNDTHREAD;

    @Override
    public void InitSound() {
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
        SOUNDSRV = new PlaybackServer(line);
        SOUNDTHREAD = new Thread(SOUNDSRV);
        SOUNDTHREAD.setDaemon(true);
        SOUNDTHREAD.start();
        MIXTHREAD = new Thread(MIXSRV);
        MIXTHREAD.setDaemon(true);
        MIXTHREAD.start();
        System.err.print("I_InitSound: ");
        super.initSound8();
        System.err.print(" pre-cached all sound data\n");
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
                if (channels[i] && (channelids[i] == sfxid)) {
                    MixMessage m = new MixMessage();
                    m.stop = true;
                    broken = i;
                    break;
                }
            }
        }
        if (broken >= 0) {
            i = broken;
            oldestnum = broken;
        } else for (i = 0; (i < numChannels) && channels[i]; i++) {
            if (channelstart[i] < oldest) {
                oldestnum = i;
            }
        }
        oldest = channelstart[oldestnum];
        if (i == numChannels) slot = oldestnum; else slot = i;
        MixMessage m = new MixMessage();
        channels[slot] = true;
        m.channel = slot;
        m.data = S_sfx[sfxid].data;
        m.pointer = 0;
        m.end = lengths[sfxid];
        if (handlenums == 0) handlenums = 100;
        channelhandles[slot] = rc = handlenums--;
        m.step = step;
        m.remainder = 0;
        channelstart[slot] = DS.gametic;
        seperation += 1;
        leftvol = volume - ((volume * seperation * seperation) >> 16);
        seperation = seperation - 257;
        rightvol = volume - ((volume * seperation * seperation) >> 16);
        if (rightvol < 0 || rightvol > 127) DS.I.Error("rightvol out of bounds");
        if (leftvol < 0 || leftvol > 127) DS.I.Error("leftvol out of bounds");
        m.leftvol_lookup = vol_lookup[leftvol];
        m.rightvol_lookup = vol_lookup[rightvol];
        channelids[slot] = sfxid;
        if (D) System.err.println(channelStatus());
        if (D) System.err.printf("Playing sfxid %d handle %d length %d vol %d on channel %d\n", sfxid, rc, S_sfx[sfxid].data.length, volume, slot);
        MIXSRV.submitMixMessage(m);
        return rc;
    }

    @Override
    public void ShutdownSound() {
        boolean done;
        produce.release();
        update_mixer.release();
        int i = 0;
        do {
            done = true;
            for (i = 0; i < numChannels; i++) {
                done &= !channels[i];
            }
        } while (!done);
        this.line.flush();
        SOUNDSRV.terminate = true;
        MIXSRV.terminate = true;
        produce.release();
        update_mixer.release();
        try {
            SOUNDTHREAD.join();
            MIXTHREAD.join();
        } catch (InterruptedException e) {
        }
        System.err.printf("3\n");
        line.close();
        System.err.printf("4\n");
    }

    protected class PlaybackServer implements Runnable {

        public boolean terminate = false;

        public PlaybackServer(SourceDataLine line) {
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

    /** A single channel does carry a lot of crap, figuratively speaking.
     *  Instead of making updates to ALL channel parameters, it makes more
     *  sense having a "mixing queue" with instructions that tell the 
     *  mixer routine to do so-and-so with a certain channel. The mixer
     *  will then "empty" the queue when it has completed a complete servicing
     *  of all messages and mapped them to its internal status.
     *
     */
    protected class MixMessage {

        /** If this is set, the mixer considers that channel "muted" */
        public boolean stop;

        /** This signals an update of a currently active channel. 
    	 * Therefore pointer, remainder and data should remain untouched. 
    	 * However volume and step of a particular channel can change.
    	 */
        public boolean update;

        public int remainder;

        public int end;

        public int channel;

        public byte[] data;

        public int step;

        public int stepremainder;

        public int[] leftvol_lookup;

        public int[] rightvol_lookup;

        public int pointer;
    }

    /** Mixing thread. Mixing and submission must still go on even if
     *  the engine lags behind due to excessive CPU load.
     * 
     * @author Maes
     *
     */
    protected class MixServer implements Runnable {

        private final ArrayBlockingQueue<MixMessage> mixmessages;

        /**
         * MAES: we'll have to use this for actual pointing. channels[] holds just
         * the data.
         */
        protected int[] p_channels;

        /**
         * The second one is supposed to point at "the end", so I'll make it an int.
         */
        protected int[] channelsend;

        private final byte[][] channels;

        /** The channel step amount... */
        protected final int[] channelstep;

        /** ... and a 0.16 bit remainder of last step. */
        protected final int[] channelstepremainder;

        protected final int[][] channelrightvol_lookup;

        protected final int[][] channelleftvol_lookup;

        private volatile boolean update = false;

        public MixServer(int numChannels) {
            mixmessages = new ArrayBlockingQueue<MixMessage>(35 * numChannels);
            this.p_channels = new int[numChannels];
            this.channels = new byte[numChannels][];
            this.channelstepremainder = new int[numChannels];
            this.channelsend = new int[numChannels];
            this.channelstep = new int[numChannels];
            this.channelleftvol_lookup = new int[numChannels][];
            this.channelrightvol_lookup = new int[numChannels][];
        }

        /** Adds a channel mixing message to the queue */
        public void submitMixMessage(MixMessage m) {
            try {
                this.mixmessages.add(m);
            } catch (IllegalStateException e) {
                mixmessages.clear();
                mixmessages.add(m);
            }
        }

        public boolean terminate = false;

        @Override
        public void run() {
            int sample = 0;
            int dl;
            int dr;
            int leftout;
            int rightout;
            final int step = 4;
            int chan;
            final int leftend = SAMPLECOUNT * step;
            while (!terminate) {
                leftout = 0;
                rightout = 2;
                try {
                    update_mixer.acquire();
                } catch (InterruptedException e) {
                }
                int messages = mixmessages.size();
                if (messages > 0) drainAndApply(messages);
                mixed = activeChannels();
                if (mixed) {
                    gunk = audiochunkpool.checkOut();
                    gunk.free = false;
                    mixbuffer = gunk.buffer;
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
                                    if (D) System.err.printf("Channel %d handle %d pointer %d thus done, stopping\n", chan, channelhandles[chan], channel_pointer);
                                    channels[chan] = null;
                                    SuperDoomSoundDriver.this.channels[chan] = false;
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
                        leftout += step;
                        rightout += step;
                    }
                }
                submitSound();
            }
        }

        private AudioChunk gunk;

        private final void submitSound() {
            if (mixed) {
                silence = 0;
                SOUNDSRV.addChunk(gunk);
                chunk++;
                if (consume.tryAcquire()) produce.release();
            } else {
                silence++;
                if (silence > ISound.BUFFER_CHUNKS) {
                    line.flush();
                    silence = 0;
                }
            }
        }

        /** Drains message queue and applies to individual channels. 
    		 *  More recently enqueued messages will trump older ones. This method
    		 *  only changes the STATUS of channels, and actual message submissions 
    		 *  can occur at most every sound frame. 
    		 *  
    		 * @param messages
    		 */
        private void drainAndApply(int messages) {
            MixMessage m;
            for (int i = 0; i < messages; i++) {
                m = this.mixmessages.remove();
                if (m.stop) {
                    stopChannel(m.channel);
                } else if (m.update) {
                    updateChannel(m);
                } else insertChannel(m);
            }
        }

        private final void stopChannel(int channel) {
            this.channels[channel] = null;
            this.p_channels[channel] = 0;
        }

        private final void updateChannel(MixMessage m) {
            this.channelleftvol_lookup[m.channel] = m.leftvol_lookup;
            this.channelrightvol_lookup[m.channel] = m.rightvol_lookup;
            this.channelstep[m.channel] = m.step;
            this.channelsend[m.channel] = m.end;
        }

        private final void insertChannel(MixMessage m) {
            int ch = m.channel;
            this.channels[ch] = m.data;
            this.p_channels[ch] = m.pointer;
            this.channelsend[ch] = m.end;
            this.channelstepremainder[ch] = m.remainder;
            this.channelleftvol_lookup[ch] = m.leftvol_lookup;
            this.channelrightvol_lookup[ch] = m.rightvol_lookup;
            this.channelstep[ch] = m.step;
        }

        private final boolean activeChannels() {
            for (int chan = 0; chan < numChannels; chan++) {
                if (channels[chan] != null) return true;
            }
            return false;
        }

        public final boolean channelIsPlaying(int num) {
            return (channels[num] != null);
        }
    }

    @Override
    public boolean SoundIsPlaying(int handle) {
        int c = getChannelFromHandle(handle);
        return (c != -2 && channels[c]);
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
            channels[hnd] = false;
            this.channelhandles[hnd] = IDLE_HANDLE;
            MixMessage m = new MixMessage();
            m.channel = hnd;
            m.stop = true;
            MIXSRV.submitMixMessage(m);
        }
    }

    @Override
    public void SubmitSound() {
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
        MixMessage m = new MixMessage();
        m.update = true;
        m.channel = chan;
        m.leftvol_lookup = vol_lookup[leftvol];
        m.rightvol_lookup = vol_lookup[rightvol];
        m.step = steptable[pitch];
        m.end = lengths[channelids[chan]];
        MIXSRV.submitMixMessage(m);
    }

    protected StringBuilder sb = new StringBuilder();

    public String channelStatus() {
        sb.setLength(0);
        for (int i = 0; i < numChannels; i++) {
            if (MIXSRV.channelIsPlaying(i)) sb.append(i); else sb.append('-');
        }
        return sb.toString();
    }

    protected class SoundTimer extends TimerTask {

        public void run() {
            update_mixer.release();
        }
    }

    protected final AudioChunk SILENT_CHUNK = new AudioChunk();

    protected final AudioChunkPool audiochunkpool = new AudioChunkPool();
}
