package s;

import static data.sounds.S_sfx;
import java.util.Collection;
import java.util.HashMap;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.FloatControl.Type;
import w.DoomBuffer;
import data.sounds.sfxenum_t;
import doom.DoomStatus;

/** Experimental Clip based driver. It does work, but it has no
 *  tangible advantages over the Audioline or Classic one. If the
 *  Audioline can be used, there's no reason to fall back to this 
 *  one.
 * 
 * KNOWN ISSUES:
 * 
 * a) Same general restrictions as audiolines (in fact, Clips ARE Audioline 
 *    in disguise)
 * b) Multiple instances of the same sound require multiple clips, so
 *    even caching them is a half-baked solution, and if you have e.g. 40 imps
 *    sound in a room.... 
 *    
 *    
 *  Currently unused.
 * 
 * @author Velktron
 *
 */
public class ClipSFXModule extends AbstractSoundDriver {

    HashMap<Integer, Clip> cachedSounds = new HashMap<Integer, Clip>();

    Clip[] channels;

    public final float[] linear2db;

    public ClipSFXModule(DoomStatus DS, int numChannels) {
        super(DS, numChannels);
        linear2db = computeLinear2DB();
    }

    private float[] computeLinear2DB() {
        float[] tmp = new float[VOLUME_STEPS];
        for (int i = 0; i < VOLUME_STEPS; i++) {
            float linear = (float) (20 * Math.log10((float) i / (float) VOLUME_STEPS));
            if (linear < -36.0) linear = -36.0f;
            tmp[i] = linear;
        }
        return tmp;
    }

    @Override
    public void InitSound() {
        System.err.println("I_InitSound: ");
        initSound16();
        System.err.print(" pre-cached all sound data\n");
        System.err.print("I_InitSound: sound module ready\n");
    }

    /** Modified getsfx. The individual length of each sfx is not of interest.
 * However, they must be transformed into 16-bit, signed, stereo samples
 * beforehand, before being "fed" to the audio clips.
 * 
 * @param sfxname
 * @param index
 * @return
 */
    protected byte[] getsfx(String sfxname, int index) {
        byte[] sfx;
        byte[] paddedsfx;
        int i;
        int size;
        int paddedsize;
        String name;
        int sfxlump;
        name = String.format("ds%s", sfxname).toUpperCase();
        if (DS.W.CheckNumForName(name) == -1) sfxlump = DS.W.GetNumForName("dspistol"); else sfxlump = DS.W.GetNumForName(name);
        size = DS.W.LumpLength(sfxlump);
        sfx = DS.W.CacheLumpNumAsRawBytes(sfxlump, 0);
        paddedsize = (size - 8) * 2 * 2;
        paddedsfx = new byte[paddedsize];
        int sample = 0;
        for (i = 8; i < size; i++) {
            final short sam = (short) ((0xFF & sfx[i] - 128) << 8);
            paddedsfx[sample++] = (byte) (0xFF & (sam >> 8));
            paddedsfx[sample++] = (byte) (0xFF & sam);
            paddedsfx[sample++] = (byte) (0xFF & (sam >> 8));
            paddedsfx[sample++] = (byte) (0xFF & sam);
        }
        DS.W.UnlockLumpNum(sfxlump);
        return paddedsfx;
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
            for (i = 0; i < numChannels && ((channels[i] == null) || (!channels[i].isActive())); i++) ;
            if (i == numChannels) done = true;
        }
        for (i = 0; i < numChannels; i++) {
            if (channels[i] != null) channels[i].close();
        }
        Collection<Clip> clips = this.cachedSounds.values();
        for (Clip c : clips) {
            c.close();
        }
        return;
    }

    @Override
    public void SetChannels(int numChannels) {
        channels = new Clip[numChannels];
    }

    private final void getClipForChannel(int c, int sfxid) {
        Clip clip = this.cachedSounds.get(sfxid);
        boolean exists = false;
        if (clip != null) {
            exists = true;
            if (!clip.isActive()) {
                channels[c] = clip;
                return;
            }
        }
        DataLine.Info info = new DataLine.Info(Clip.class, DoomSound.DEFAULT_SAMPLES_FORMAT);
        try {
            clip = (Clip) AudioSystem.getLine(info);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        try {
            clip.open(DoomSound.DEFAULT_SAMPLES_FORMAT, S_sfx[sfxid].data, 0, S_sfx[sfxid].data.length);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        if (!exists) this.cachedSounds.put(sfxid, clip);
        channels[c] = clip;
    }

    protected short handlenums = 0;

    protected int addsfx(int sfxid, int volume, int pitch, int seperation) {
        int i;
        int rc = -1;
        int oldest = DS.gametic;
        int oldestnum = 0;
        int slot;
        if (sfxid == sfxenum_t.sfx_sawup.ordinal() || sfxid == sfxenum_t.sfx_sawidl.ordinal() || sfxid == sfxenum_t.sfx_sawful.ordinal() || sfxid == sfxenum_t.sfx_sawhit.ordinal() || sfxid == sfxenum_t.sfx_stnmov.ordinal() || sfxid == sfxenum_t.sfx_pistol.ordinal()) {
            for (i = 0; i < numChannels; i++) {
                if (channels[i] != null && channels[i].isRunning() && channelids[i] == sfxid) {
                    channels[i].stop();
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
        getClipForChannel(slot, sfxid);
        if (handlenums == 0) handlenums = MAXHANDLES;
        channelhandles[slot] = rc = handlenums--;
        channelstart[slot] = DS.gametic;
        channelids[slot] = sfxid;
        setVolume(slot, volume);
        setPanning(slot, seperation);
        if (D) System.err.println(channelStatus());
        if (D) System.err.printf("Playing %d vol %d on channel %d\n", rc, volume, slot);
        channels[slot].setFramePosition(0);
        channels[slot].start();
        return rc;
    }

    /** Accepts volume in "Doom" format (0-127).
	 * 
	 * @param volume
	 */
    public void setVolume(int chan, int volume) {
        Clip c = channels[chan];
        if (c.isControlSupported(Type.MASTER_GAIN)) {
            FloatControl vc = (FloatControl) c.getControl(Type.MASTER_GAIN);
            float vol = linear2db[volume];
            vc.setValue(vol);
        } else if (c.isControlSupported(Type.VOLUME)) {
            FloatControl vc = (FloatControl) c.getControl(Type.VOLUME);
            float vol = vc.getMinimum() + (vc.getMaximum() - vc.getMinimum()) * (float) volume / 127f;
            vc.setValue(vol);
        }
    }

    public void setPanning(int chan, int sep) {
        Clip c = channels[chan];
        if (c.isControlSupported(Type.PAN)) {
            FloatControl bc = (FloatControl) c.getControl(Type.PAN);
            float pan = bc.getMinimum() + (bc.getMaximum() - bc.getMinimum()) * (float) sep / ISound.PANNING_STEPS;
            bc.setValue(pan);
        }
    }

    @Override
    public void StopSound(int handle) {
        int hnd = getChannelFromHandle(handle);
        if (hnd >= 0) {
            channels[hnd].stop();
            channels[hnd] = null;
        }
    }

    @Override
    public boolean SoundIsPlaying(int handle) {
        return getChannelFromHandle(handle) != BUSY_HANDLE;
    }

    @Override
    public void UpdateSoundParams(int handle, int vol, int sep, int pitch) {
        int i = getChannelFromHandle(handle);
        if (i != BUSY_HANDLE) {
            setVolume(i, vol);
            setPanning(i, sep);
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

    StringBuilder sb = new StringBuilder();

    public String channelStatus() {
        sb.setLength(0);
        for (int i = 0; i < numChannels; i++) {
            if (channels[i] != null && channels[i].isActive()) sb.append(i); else sb.append('-');
        }
        return sb.toString();
    }
}
