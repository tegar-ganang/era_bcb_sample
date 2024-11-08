package s;

import static data.Tables.ANGLETOFINESHIFT;
import static data.Tables.BITS32;
import static data.Tables.finesine;
import static data.sounds.S_sfx;
import static m.fixed_t.FRACBITS;
import static m.fixed_t.FixedMul;
import p.mobj_t;
import data.Defines;
import data.musicinfo_t;
import data.sfxinfo_t;
import data.sounds;
import data.sounds.musicenum_t;
import data.sounds.sfxenum_t;
import doom.DoomStatus;

/** Some stuff that is not implementation dependant
 *  This includes channel management, sound priorities,
 *  positioning, distance attenuation etc. It's up to 
 *  lower-level "drivers" to actually implements those.
 *  This particular class needs not be a dummy itself, but
 *  the drivers it "talks" to might be. 
 *  
 * 
 * */
public class AbstractDoomAudio implements IDoomSound {

    protected final DoomStatus DS;

    protected final IMusic IMUS;

    protected final ISound ISND;

    protected final int numChannels;

    protected static final boolean D = false;

    /** the set of channels available. These are "soft" descriptor
	   channels,  not to be confused with actual hardware audio 
	   lines, which are an entirely different concern.

	 */
    protected final channel_t[] channels;

    protected int snd_SfxVolume = 15;

    protected int snd_MusicVolume = 15;

    protected boolean mus_paused;

    protected musicinfo_t mus_playing;

    protected int nextcleanup;

    public AbstractDoomAudio(DoomStatus DS, int numChannels) {
        this.DS = DS;
        this.numChannels = numChannels;
        this.channels = new channel_t[numChannels];
        this.IMUS = DS.IMUS;
        this.ISND = DS.ISND;
    }

    /** Volume, pitch, separation  & priority packed for parameter passing */
    protected class vps_t {

        int volume;

        int pitch;

        int sep;

        int priority;
    }

    /**
	 * Initializes sound stuff, including volume
	 * Sets channels, SFX and music volume,
	 *  allocates channel buffer, sets S_sfx lookup.
	 */
    public void Init(int sfxVolume, int musicVolume) {
        int i;
        System.err.printf("S_Init: default sfx volume %d\n", sfxVolume);
        this.snd_SfxVolume = sfxVolume;
        this.snd_MusicVolume = musicVolume;
        ISND.SetChannels(numChannels);
        SetSfxVolume(sfxVolume);
        IMUS.SetMusicVolume(musicVolume);
        for (i = 0; i < numChannels; i++) {
            channels[i] = new channel_t();
        }
        mus_paused = false;
        for (i = 1; i < S_sfx.length; i++) S_sfx[i].lumpnum = S_sfx[i].usefulness = -1;
    }

    public void Start() {
        int cnum;
        int mnum;
        for (cnum = 0; cnum < numChannels; cnum++) if (channels[cnum].sfxinfo != null) StopChannel(cnum);
        mus_paused = false;
        if (DS.isCommercial()) mnum = musicenum_t.mus_runnin.ordinal() + DS.gamemap - 1; else {
            musicenum_t[] spmus = { musicenum_t.mus_e3m4, musicenum_t.mus_e3m2, musicenum_t.mus_e3m3, musicenum_t.mus_e1m5, musicenum_t.mus_e2m7, musicenum_t.mus_e2m4, musicenum_t.mus_e2m6, musicenum_t.mus_e2m5, musicenum_t.mus_e1m9 };
            if (DS.gameepisode < 4) mnum = musicenum_t.mus_e1m1.ordinal() + (DS.gameepisode - 1) * 9 + DS.gamemap - 1; else mnum = spmus[DS.gamemap - 1].ordinal();
        }
        ChangeMusic(mnum, true);
        nextcleanup = 15;
    }

    private vps_t vps = new vps_t();

    public void StartSoundAtVolume(ISoundOrigin origin_p, int sfx_id, int volume) {
        boolean rc;
        int sep = 0;
        int pitch;
        int priority;
        sfxinfo_t sfx;
        int cnum;
        ISoundOrigin origin = (ISoundOrigin) origin_p;
        if (sfx_id < 1 || sfx_id > NUMSFX) {
            Exception e = new Exception();
            e.printStackTrace();
            DS.I.Error("Bad sfx #: %d", sfx_id);
        }
        sfx = S_sfx[sfx_id];
        if (sfx.link != null) {
            pitch = sfx.pitch;
            priority = sfx.priority;
            volume += sfx.volume;
            if (volume < 1) return;
            if (volume > snd_SfxVolume) volume = snd_SfxVolume;
        } else {
            pitch = NORM_PITCH;
            priority = NORM_PRIORITY;
        }
        if ((origin != null) && origin != DS.players[DS.consoleplayer].mo) {
            vps.volume = volume;
            vps.pitch = pitch;
            vps.sep = sep;
            rc = AdjustSoundParams(DS.players[DS.consoleplayer].mo, origin, vps);
            volume = vps.volume;
            pitch = vps.pitch;
            sep = vps.sep;
            if (origin.getX() == DS.players[DS.consoleplayer].mo.x && origin.getY() == DS.players[DS.consoleplayer].mo.y) {
                sep = NORM_SEP;
            }
            if (!rc) {
                return;
            }
        } else {
            sep = NORM_SEP;
        }
        if (sfx_id >= sfxenum_t.sfx_sawup.ordinal() && sfx_id <= sfxenum_t.sfx_sawhit.ordinal()) {
            pitch += 8 - (DS.RND.M_Random() & 15);
            if (pitch < 0) pitch = 0; else if (pitch > 255) pitch = 255;
        } else if (sfx_id != sfxenum_t.sfx_itemup.ordinal() && sfx_id != sfxenum_t.sfx_tink.ordinal()) {
            pitch += 16 - (DS.RND.M_Random() & 31);
            if (pitch < 0) pitch = 0; else if (pitch > 255) pitch = 255;
        }
        StopSound(origin);
        cnum = getChannel(origin, sfx);
        if (cnum < 0) return;
        if (sfx.lumpnum < 0) sfx.lumpnum = ISND.GetSfxLumpNum(sfx);
        if (sfx.usefulness++ < 0) sfx.usefulness = 1;
        channels[cnum].handle = ISND.StartSound(sfx_id, volume, sep, pitch, priority);
        if (D) System.err.printf("Handle %d for channel %d for sound %s vol %d sep %d\n", channels[cnum].handle, cnum, sfx.name, volume, sep);
    }

    public void StartSound(ISoundOrigin origin, sfxenum_t sfx_id) {
        if (sfx_id != null && sfx_id.ordinal() > 0) StartSound(origin, sfx_id.ordinal());
    }

    public void StartSound(ISoundOrigin origin, int sfx_id) {
        StartSoundAtVolume(origin, sfx_id, snd_SfxVolume);
    }

    public void StopSound(ISoundOrigin origin) {
        int cnum;
        for (cnum = 0; cnum < numChannels; cnum++) {
            if (channels[cnum].sfxinfo != null && channels[cnum].origin == origin) {
                StopChannel(cnum);
                break;
            }
        }
    }

    public void PauseSound() {
        if (mus_playing != null && !mus_paused) {
            IMUS.PauseSong(mus_playing.handle);
            mus_paused = true;
        }
    }

    public void ResumeSound() {
        if (mus_playing != null && mus_paused) {
            IMUS.ResumeSong(mus_playing.handle);
            mus_paused = false;
        }
    }

    @Override
    public void UpdateSounds(mobj_t listener) {
        boolean audible;
        int cnum;
        sfxinfo_t sfx;
        channel_t c;
        for (cnum = 0; cnum < numChannels; cnum++) {
            c = channels[cnum];
            sfx = c.sfxinfo;
            if (c.sfxinfo != null) {
                if (ISND.SoundIsPlaying(c.handle)) {
                    vps.volume = snd_SfxVolume;
                    vps.pitch = NORM_PITCH;
                    vps.sep = NORM_SEP;
                    sfx = c.sfxinfo;
                    if (sfx.link != null) {
                        vps.pitch = sfx.pitch;
                        vps.volume += sfx.volume;
                        if (vps.volume < 1) {
                            StopChannel(cnum);
                            continue;
                        } else if (vps.volume > snd_SfxVolume) {
                            vps.volume = snd_SfxVolume;
                        }
                    }
                    if (c.origin != null && (listener != c.origin)) {
                        audible = AdjustSoundParams(listener, c.origin, vps);
                        if (!audible) {
                            StopChannel(cnum);
                        } else ISND.UpdateSoundParams(c.handle, vps.volume, vps.sep, vps.pitch);
                    }
                } else {
                    StopChannel(cnum);
                }
            }
        }
    }

    public void SetMusicVolume(int volume) {
        if (volume < 0 || volume > 127) {
            DS.I.Error("Attempt to set music volume at %d", volume);
        }
        IMUS.SetMusicVolume(volume);
        snd_MusicVolume = volume;
    }

    public void SetSfxVolume(int volume) {
        if (volume < 0 || volume > 127) DS.I.Error("Attempt to set sfx volume at %d", volume);
        snd_SfxVolume = volume;
    }

    public void StartMusic(int m_id) {
        ChangeMusic(m_id, false);
    }

    public void StartMusic(musicenum_t m_id) {
        ChangeMusic(m_id.ordinal(), false);
    }

    public void ChangeMusic(musicenum_t musicnum, boolean looping) {
        ChangeMusic(musicnum.ordinal(), false);
    }

    public void ChangeMusic(int musicnum, boolean looping) {
        musicinfo_t music = null;
        String namebuf;
        if ((musicnum <= musicenum_t.mus_None.ordinal()) || (musicnum >= musicenum_t.NUMMUSIC.ordinal())) {
            DS.I.Error("Bad music number %d", musicnum);
        } else music = sounds.S_music[musicnum];
        if (mus_playing == music) return;
        StopMusic();
        if (music.lumpnum == 0) {
            namebuf = String.format("d_%s", music.name);
            music.lumpnum = DS.W.GetNumForName(namebuf);
        }
        music.data = DS.W.CacheLumpNumAsRawBytes(music.lumpnum, Defines.PU_MUSIC);
        music.handle = IMUS.RegisterSong(music.data);
        IMUS.PlaySong(music.handle, looping);
        SetMusicVolume(this.snd_MusicVolume);
        mus_playing = music;
    }

    public void StopMusic() {
        if (mus_playing != null) {
            if (mus_paused) IMUS.ResumeSong(mus_playing.handle);
            IMUS.StopSong(mus_playing.handle);
            IMUS.UnRegisterSong(mus_playing.handle);
            mus_playing.data = null;
            mus_playing = null;
        }
    }

    /** This is S_StopChannel. There's another StopChannel
	 *  with a similar contract in ISound. Don't confuse the two.
	 *  
	 * 
	 *  
	 * @param cnum
	 */
    protected void StopChannel(int cnum) {
        int i;
        channel_t c = channels[cnum];
        if (c.sfxinfo != null) {
            if (ISND.SoundIsPlaying(c.handle)) {
                ISND.StopSound(c.handle);
            }
            for (i = 0; i < numChannels; i++) {
                if (cnum != i && c.sfxinfo == channels[i].sfxinfo) {
                    break;
                }
            }
            c.sfxinfo.usefulness--;
            c.sfxinfo = null;
        }
    }

    protected boolean AdjustSoundParams(mobj_t listener, ISoundOrigin source, vps_t vps) {
        int approx_dist;
        int adx;
        int ady;
        long angle;
        adx = Math.abs(listener.x - source.getX());
        ady = Math.abs(listener.y - source.getY());
        approx_dist = adx + ady - ((adx < ady ? adx : ady) >> 1);
        if (DS.gamemap != 8 && approx_dist > S_CLIPPING_DIST) {
            return false;
        }
        angle = rr.RendererState.PointToAngle(listener.x, listener.y, source.getX(), source.getY());
        if (angle > listener.angle) angle = angle - listener.angle; else angle = angle + (0xffffffffL - listener.angle & BITS32);
        angle &= BITS32;
        angle >>= ANGLETOFINESHIFT;
        vps.sep = 128 - (FixedMul(S_STEREO_SWING, finesine[(int) angle]) >> FRACBITS);
        if (approx_dist < S_CLOSE_DIST) {
            vps.volume = snd_SfxVolume;
        } else if (DS.gamemap == 8) {
            if (approx_dist > S_CLIPPING_DIST) approx_dist = S_CLIPPING_DIST;
            vps.volume = 15 + ((snd_SfxVolume - 15) * ((S_CLIPPING_DIST - approx_dist) >> FRACBITS)) / S_ATTENUATOR;
        } else {
            vps.volume = (snd_SfxVolume * ((S_CLIPPING_DIST - approx_dist) >> FRACBITS)) / S_ATTENUATOR;
        }
        return (vps.volume > 0);
    }

    protected int getChannel(ISoundOrigin origin, sfxinfo_t sfxinfo) {
        int cnum;
        channel_t c;
        for (cnum = 0; cnum < numChannels; cnum++) {
            if (channels[cnum].sfxinfo == null) break; else if (origin != null && channels[cnum].origin == origin) {
                StopChannel(cnum);
                break;
            }
        }
        if (cnum == numChannels) {
            for (cnum = 0; cnum < numChannels; cnum++) if (channels[cnum].sfxinfo.priority >= sfxinfo.priority) break;
            if (cnum == numChannels) {
                return -1;
            } else {
                StopChannel(cnum);
            }
        }
        c = channels[cnum];
        c.sfxinfo = sfxinfo;
        c.origin = origin;
        return cnum;
    }
}
