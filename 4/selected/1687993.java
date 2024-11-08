package s;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.FloatControl.Type;
import data.sounds;
import data.sounds.sfxenum_t;
import doom.DoomStatus;

/** David Martel's sound driver for Mocha Doom. Excellent work!
 * 
 *  However, it's based on Java Audiolines, and as such has a number
 *  of drawbacks:
 *  
 * a) Sounds are forcibly blown to be stereo, 16-bit otherwise it's 
 *    impossible to get panning controls.
 * b) Volume, master gain, panning, pitch etc. controls are NOT guaranteed
 *    to be granted across different OSes , and your mileage may vary. It's
 *    fairly OK under Windows and OS X, but Linux is very clunky. The only
 *    control that is -somewhat- guaranteed is the volume one.
 * c) Spawns as many threads as channels. Even if semaphore waiting it used,
 *    that can be taxing for slower systems.

 * 
 * @author David
 * @author Velktron
 *
 */
public class DavidSFXModule extends AbstractSoundDriver {

    ArrayList<DoomSound> cachedSounds = new ArrayList<DoomSound>();

    public final float[] linear2db;

    private SoundWorker[] channels;

    private Thread[] soundThread;

    public DavidSFXModule(DoomStatus DS, int numChannels) {
        super(DS, numChannels);
        linear2db = computeLinear2DB();
    }

    private float[] computeLinear2DB() {
        float[] tmp = new float[VOLUME_STEPS];
        for (int i = 0; i < VOLUME_STEPS; i++) {
            float linear = (float) (10 * Math.log10((float) i / (float) VOLUME_STEPS));
            if (linear < -36.0) linear = -36.0f;
            tmp[i] = linear;
        }
        return tmp;
    }

    @Override
    public void InitSound() {
        System.err.println("I_InitSound: ");
        initSound16();
        for (int i = 0; i < sounds.S_sfx.length; i++) {
            DoomSound tmp = new DoomSound(sounds.S_sfx[i], DoomSound.DEFAULT_SAMPLES_FORMAT);
            cachedSounds.add(tmp);
        }
        System.err.print(" pre-cached all sound data\n");
        System.err.print("I_InitSound: sound module ready\n");
    }

    @Override
    public void UpdateSound() {
    }

    @Override
    public void SubmitSound() {
    }

    @Override
    public void ShutdownSound() {
        boolean done = false;
        int i;
        while (!done) {
            for (i = 0; i < numChannels && !(channels[i].isPlaying()); i++) ;
            if (i == numChannels) done = true;
        }
        for (i = 0; i < numChannels; i++) {
            channels[i].terminate = true;
            channels[i].wait.release();
            try {
                this.soundThread[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return;
    }

    @Override
    public void SetChannels(int numChannels) {
        channels = new SoundWorker[numChannels];
        soundThread = new Thread[numChannels];
        for (int i = 0; i < numChannels; i++) {
            channels[i] = new SoundWorker(i);
            soundThread[i] = new Thread(channels[i]);
            soundThread[i].start();
        }
    }

    /** This one will only create datalines for common clip/audioline samples
	 *  directly.
	 * 
	 * @param c
	 * @param sfxid
	 */
    private final void createDataLineForChannel(int c, int sfxid) {
        if (channels[c].auline == null) {
            try {
                DoomSound tmp = cachedSounds.get(sfxid);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, DoomSound.DEFAULT_SAMPLES_FORMAT);
                channels[c].auline = (SourceDataLine) AudioSystem.getLine(info);
                channels[c].auline.open(tmp.format);
            } catch (LineUnavailableException e) {
                e.printStackTrace();
            }
            boolean errors = false;
            if (channels[c].auline.isControlSupported(Type.MASTER_GAIN)) channels[c].vc = (FloatControl) channels[c].auline.getControl(Type.MASTER_GAIN); else {
                System.err.print("MASTER_GAIN, ");
                errors = true;
                if (channels[c].auline.isControlSupported(Type.VOLUME)) channels[c].vc = (FloatControl) channels[c].auline.getControl(Type.VOLUME); else System.err.print("VOLUME, ");
            }
            if (channels[c].auline.isControlSupported(Type.SAMPLE_RATE)) {
                channels[c].pc = (FloatControl) channels[c].auline.getControl(Type.SAMPLE_RATE);
            } else {
                errors = true;
                System.err.print("SAMPLE_RATE, ");
            }
            if (channels[c].auline.isControlSupported(Type.BALANCE)) {
                channels[c].bc = (FloatControl) channels[c].auline.getControl(FloatControl.Type.BALANCE);
            } else {
                System.err.print("BALANCE, ");
                errors = true;
                if (channels[c].auline.isControlSupported(Type.PAN)) {
                    channels[c].bc = (FloatControl) channels[c].auline.getControl(FloatControl.Type.PAN);
                } else {
                    System.err.print("PANNING ");
                }
            }
            if (errors) System.err.printf("for channel %d NOT supported!\n", c);
            channels[c].auline.start();
        }
    }

    @Override
    protected int addsfx(int sfxid, int volume, int pitch, int seperation) {
        int i;
        int rc = -1;
        int oldest = DS.gametic;
        int oldestnum = 0;
        int slot;
        int rightvol;
        int leftvol;
        if (sfxid == sfxenum_t.sfx_sawup.ordinal() || sfxid == sfxenum_t.sfx_sawidl.ordinal() || sfxid == sfxenum_t.sfx_sawful.ordinal() || sfxid == sfxenum_t.sfx_sawhit.ordinal() || sfxid == sfxenum_t.sfx_stnmov.ordinal() || sfxid == sfxenum_t.sfx_pistol.ordinal()) {
            for (i = 0; i < numChannels; i++) {
                if ((channels[i].isPlaying()) && (channelids[i] == sfxid)) {
                    channels[i].stopSound();
                    break;
                }
            }
        }
        for (i = 0; (i < numChannels) && (channels[i] != null); i++) {
            if (channelstart[i] < oldest) {
                oldestnum = i;
                oldest = channelstart[i];
            }
        }
        if (i == numChannels) slot = oldestnum; else slot = i;
        createDataLineForChannel(slot, sfxid);
        if (handlenums == 0) handlenums = MAXHANDLES;
        channelhandles[slot] = rc = handlenums--;
        channelstart[slot] = DS.gametic;
        seperation += 1;
        leftvol = volume - ((volume * seperation * seperation) >> 16);
        seperation = seperation - 257;
        rightvol = volume - ((volume * seperation * seperation) >> 16);
        if (rightvol < 0 || rightvol > 127) DS.I.Error("rightvol out of bounds");
        if (leftvol < 0 || leftvol > 127) DS.I.Error("leftvol out of bounds");
        channelids[slot] = sfxid;
        channels[slot].setVolume(volume);
        channels[slot].setPanning(seperation + 256);
        channels[slot].addSound(cachedSounds.get(sfxid).data, handlenums);
        channels[slot].setPitch(pitch);
        if (D) System.err.println(channelStatus());
        if (D) System.err.printf("Playing %d vol %d on channel %d\n", rc, volume, slot);
        return rc;
    }

    @Override
    public void StopSound(int handle) {
        int hnd = getChannelFromHandle(handle);
        if (hnd >= 0) channels[hnd].stopSound();
    }

    @Override
    public boolean SoundIsPlaying(int handle) {
        return getChannelFromHandle(handle) != BUSY_HANDLE;
    }

    @Override
    public void UpdateSoundParams(int handle, int vol, int sep, int pitch) {
        int i = getChannelFromHandle(handle);
        if (i != BUSY_HANDLE) {
            channels[i].setVolume(vol);
            channels[i].setPitch(pitch);
            channels[i].setPanning(sep);
        }
    }

    /** Internal use. 
	 * 
	 * @param handle
	 * @return the channel that has the handle, or -2 if none has it.
	 */
    private int getChannelFromHandle(int handle) {
        for (int i = 0; i < numChannels; i++) {
            if (channelhandles[i] == handle) return i;
        }
        return BUSY_HANDLE;
    }

    /** A Thread for playing digital sound effects.
	 * 
	 *  Obviously you need as many as channels?
	 *   
	 *  In order not to end up in a hell of effects,
	 *  certain types of sounds must be limited to 1 per object.
	 *
	 */
    private class SoundWorker implements Runnable {

        public Semaphore wait;

        FloatControl vc;

        FloatControl bc;

        FloatControl pc;

        byte[] currentSoundSync;

        byte[] currentSound;

        public SoundWorker(int id) {
            this.id = id;
            this.handle = IDLE_HANDLE;
            wait = new Semaphore(1);
        }

        int id;

        /** Used to find out whether the same object is continuously making
			 *  sounds. E.g. the player, ceilings etc. In that case, they must be
			 *  interrupted.
			 */
        int handle;

        public boolean terminate;

        SourceDataLine auline;

        /** This is how you tell the thread to play a sound,
			 * I suppose.  */
        public void addSound(byte[] ds, int handle) {
            if (D) System.out.printf("Added handle %d to channel %d\n", handle, id);
            this.handle = handle;
            this.currentSound = ds;
            this.auline.stop();
            this.auline.start();
            this.wait.release();
        }

        /** Accepts volume in "Doom" format (0-127).
			 * 
			 * @param volume
			 */
        public void setVolume(int volume) {
            if (vc != null) {
                if (vc.getType() == FloatControl.Type.MASTER_GAIN) {
                    float vol = linear2db[volume];
                    vc.setValue(vol);
                } else if (vc.getType() == FloatControl.Type.VOLUME) {
                    float vol = vc.getMinimum() + (vc.getMaximum() - vc.getMinimum()) * (float) volume / 127f;
                    vc.setValue(vol);
                }
            }
        }

        public void setPanning(int sep) {
            if (bc != null) {
                float pan = bc.getMinimum() + (bc.getMaximum() - bc.getMinimum()) * (float) (sep) / ISound.PANNING_STEPS;
                bc.setValue(pan);
            }
        }

        /** Expects a steptable value between 16K and 256K, with
			 *  64K being the middle.
			 * 
			 * @param pitch
			 */
        public void setPitch(int pitch) {
            if (pc != null) {
                float pan = (float) (pc.getValue() * ((float) pitch / 65536.0));
                pc.setValue(pan);
            }
        }

        public void run() {
            System.err.printf("Sound thread %d started\n", id);
            while (!terminate) {
                currentSoundSync = currentSound;
                if (currentSoundSync != null) {
                    try {
                        auline.write(currentSoundSync, 0, currentSoundSync.length);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    } finally {
                        auline.drain();
                    }
                    currentSound = null;
                    if (handle > 0) channelhandles[this.id] = IDLE_HANDLE;
                    this.handle = IDLE_HANDLE;
                }
                try {
                    wait.acquire();
                } catch (InterruptedException e) {
                }
            }
        }

        public void stopSound() {
            auline.stop();
            auline.flush();
            channelhandles[this.id] = IDLE_HANDLE;
            this.handle = IDLE_HANDLE;
            currentSound = null;
            auline.start();
        }

        public boolean isPlaying() {
            return (this.handle != IDLE_HANDLE || this.currentSound != null);
        }
    }

    StringBuilder sb = new StringBuilder();

    public String channelStatus() {
        sb.setLength(0);
        for (int i = 0; i < numChannels; i++) {
            if (channels[i].isPlaying()) sb.append(i); else sb.append('-');
        }
        return sb.toString();
    }
}
