package com.jme3.audio.android;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import com.jme3.asset.AssetKey;
import com.jme3.audio.AudioNode.Status;
import com.jme3.audio.*;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is the android implementation for {@link AudioRenderer}
 * 
 * @author larynx
 * @author plan_rich
 */
public class AndroidAudioRenderer implements AudioRenderer, SoundPool.OnLoadCompleteListener, MediaPlayer.OnCompletionListener {

    private static final Logger logger = Logger.getLogger(AndroidAudioRenderer.class.getName());

    private static final int MAX_NUM_CHANNELS = 16;

    private final HashMap<AudioNode, MediaPlayer> musicPlaying = new HashMap<AudioNode, MediaPlayer>();

    private SoundPool soundPool = null;

    private final Vector3f listenerPosition = new Vector3f();

    private final Vector3f distanceVector = new Vector3f();

    private final Context context;

    private final AssetManager assetManager;

    private HashMap<Integer, AudioNode> soundpoolStillLoading = new HashMap<Integer, AudioNode>();

    private Listener listener;

    private boolean audioDisabled = false;

    private final AudioManager manager;

    public AndroidAudioRenderer(Activity context) {
        this.context = context;
        manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        context.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        assetManager = context.getAssets();
    }

    @Override
    public void initialize() {
        soundPool = new SoundPool(MAX_NUM_CHANNELS, AudioManager.STREAM_MUSIC, 0);
        soundPool.setOnLoadCompleteListener(this);
    }

    @Override
    public void updateSourceParam(AudioNode src, AudioParam param) {
        if (audioDisabled) {
            return;
        }
        if (src.getChannel() < 0) {
            return;
        }
        switch(param) {
            case Position:
                if (!src.isPositional()) {
                    return;
                }
                Vector3f pos = src.getWorldTranslation();
                break;
            case Velocity:
                if (!src.isPositional()) {
                    return;
                }
                Vector3f vel = src.getVelocity();
                break;
            case MaxDistance:
                if (!src.isPositional()) {
                    return;
                }
                break;
            case RefDistance:
                if (!src.isPositional()) {
                    return;
                }
                break;
            case ReverbFilter:
                if (!src.isPositional() || !src.isReverbEnabled()) {
                    return;
                }
                break;
            case ReverbEnabled:
                if (!src.isPositional()) {
                    return;
                }
                if (src.isReverbEnabled()) {
                    updateSourceParam(src, AudioParam.ReverbFilter);
                }
                break;
            case IsPositional:
                break;
            case Direction:
                if (!src.isDirectional()) {
                    return;
                }
                Vector3f dir = src.getDirection();
                break;
            case InnerAngle:
                if (!src.isDirectional()) {
                    return;
                }
                break;
            case OuterAngle:
                if (!src.isDirectional()) {
                    return;
                }
                break;
            case IsDirectional:
                if (src.isDirectional()) {
                    updateSourceParam(src, AudioParam.Direction);
                    updateSourceParam(src, AudioParam.InnerAngle);
                    updateSourceParam(src, AudioParam.OuterAngle);
                } else {
                }
                break;
            case DryFilter:
                if (src.getDryFilter() != null) {
                    Filter f = src.getDryFilter();
                    if (f.isUpdateNeeded()) {
                    }
                }
                break;
            case Looping:
                if (src.isLooping()) {
                }
                break;
            case Volume:
                MediaPlayer mp = musicPlaying.get(src);
                if (mp != null) {
                    mp.setVolume(src.getVolume(), src.getVolume());
                } else {
                    soundPool.setVolume(src.getChannel(), src.getVolume(), src.getVolume());
                }
                break;
            case Pitch:
                break;
        }
    }

    @Override
    public void updateListenerParam(Listener listener, ListenerParam param) {
        if (audioDisabled) {
            return;
        }
        switch(param) {
            case Position:
                listenerPosition.set(listener.getLocation());
                break;
            case Rotation:
                Vector3f dir = listener.getDirection();
                Vector3f up = listener.getUp();
                break;
            case Velocity:
                Vector3f vel = listener.getVelocity();
                break;
            case Volume:
                break;
        }
    }

    @Override
    public void update(float tpf) {
        float distance;
        float volume;
        for (AudioNode src : musicPlaying.keySet()) {
            MediaPlayer mp = musicPlaying.get(src);
            distanceVector.set(listenerPosition);
            distanceVector.subtractLocal(src.getLocalTranslation());
            distance = FastMath.abs(distanceVector.length());
            if (distance < src.getRefDistance()) {
                distance = src.getRefDistance();
            }
            if (distance > src.getMaxDistance()) {
                distance = src.getMaxDistance();
            }
            volume = src.getRefDistance() / distance;
            AndroidAudioData audioData = (AndroidAudioData) src.getAudioData();
            if (FastMath.abs(audioData.getCurrentVolume() - volume) > FastMath.FLT_EPSILON) {
                mp.setVolume(volume, volume);
                audioData.setCurrentVolume(volume);
            }
        }
    }

    public void setListener(Listener listener) {
        if (audioDisabled) {
            return;
        }
        if (this.listener != null) {
            this.listener.setRenderer(null);
        }
        this.listener = listener;
        this.listener.setRenderer(this);
    }

    @Override
    public void cleanup() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        for (AudioNode src : musicPlaying.keySet()) {
            MediaPlayer mp = musicPlaying.get(src);
            {
                mp.stop();
                mp.release();
                src.setStatus(Status.Stopped);
            }
        }
        musicPlaying.clear();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mp.isPlaying()) {
            mp.seekTo(0);
            mp.stop();
        }
        for (AudioNode src : musicPlaying.keySet()) {
            if (musicPlaying.get(src) == mp) {
                src.setStatus(Status.Stopped);
                break;
            }
        }
    }

    /**
     * Plays using the {@link SoundPool} of Android. Due to hard limitation of
     * the SoundPool: After playing more instances of the sound you only have
     * the channel of the last played instance.
     * 
     * It is not possible to get information about the state of the soundpool of
     * a specific streamid, so removing is not possilbe -> noone knows when
     * sound finished.
     */
    public void playSourceInstance(AudioNode src) {
        if (audioDisabled) {
            return;
        }
        AndroidAudioData audioData = (AndroidAudioData) src.getAudioData();
        if (!(audioData.getAssetKey() instanceof AudioKey)) {
            throw new IllegalArgumentException("Asset is not a AudioKey");
        }
        AudioKey assetKey = (AudioKey) audioData.getAssetKey();
        try {
            if (audioData.getId() < 0) {
                int soundId = soundPool.load(assetManager.openFd(assetKey.getName()), 1);
                audioData.setId(soundId);
            }
            int channel = soundPool.play(audioData.getId(), 1f, 1f, 1, 0, 1f);
            if (channel == 0) {
                soundpoolStillLoading.put(audioData.getId(), src);
            } else {
                if (src.getStatus() != Status.Stopped) {
                    soundPool.stop(channel);
                    src.setStatus(Status.Stopped);
                }
                src.setChannel(channel);
                setSourceParams(src);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load sound " + assetKey.getName(), e);
            audioData.setId(-1);
        }
    }

    @Override
    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
        AudioNode src = soundpoolStillLoading.remove(sampleId);
        if (src == null) {
            logger.warning("Something went terribly wrong! onLoadComplete" + " had sampleId which was not in the HashMap of loading items");
            return;
        }
        AudioData audioData = src.getAudioData();
        if (status == 0) {
            int channelIndex;
            channelIndex = soundPool.play(audioData.getId(), 1f, 1f, 1, 0, 1f);
            src.setChannel(channelIndex);
            setSourceParams(src);
        }
    }

    public void playSource(AudioNode src) {
        if (audioDisabled) {
            return;
        }
        AndroidAudioData audioData = (AndroidAudioData) src.getAudioData();
        MediaPlayer mp = musicPlaying.get(src);
        if (mp == null) {
            mp = new MediaPlayer();
            mp.setOnCompletionListener(this);
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
        }
        try {
            if (src.getStatus() == Status.Stopped) {
                mp.reset();
                AssetKey<?> key = audioData.getAssetKey();
                AssetFileDescriptor afd = assetManager.openFd(key.getName());
                mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                mp.prepare();
                setSourceParams(src, mp);
                src.setChannel(0);
                src.setStatus(Status.Playing);
                musicPlaying.put(src, mp);
                mp.start();
            } else {
                mp.start();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setSourceParams(AudioNode src, MediaPlayer mp) {
        mp.setLooping(src.isLooping());
        mp.setVolume(src.getVolume(), src.getVolume());
    }

    private void setSourceParams(AudioNode src) {
        soundPool.setLoop(src.getChannel(), src.isLooping() ? -1 : 0);
        soundPool.setVolume(src.getChannel(), src.getVolume(), src.getVolume());
    }

    /**
     * Pause the current playing sounds. Both from the {@link SoundPool} and the
     * active {@link MediaPlayer}s
     */
    public void pauseAll() {
        if (soundPool != null) {
            soundPool.autoPause();
            for (MediaPlayer mp : musicPlaying.values()) {
                mp.pause();
            }
        }
    }

    /**
     * Resume all paused sounds.
     */
    public void resumeAll() {
        if (soundPool != null) {
            soundPool.autoResume();
            for (MediaPlayer mp : musicPlaying.values()) {
                mp.start();
            }
        }
    }

    public void pauseSource(AudioNode src) {
        if (audioDisabled) {
            return;
        }
        MediaPlayer mp = musicPlaying.get(src);
        if (mp != null) {
            mp.pause();
            src.setStatus(Status.Paused);
        } else {
            int channel = src.getChannel();
            if (channel != -1) {
                soundPool.pause(channel);
            }
        }
    }

    public void stopSource(AudioNode src) {
        if (audioDisabled) {
            return;
        }
        MediaPlayer mp = musicPlaying.get(src);
        if (mp != null) {
            mp.stop();
            mp.reset();
            src.setStatus(Status.Stopped);
        } else {
            int channel = src.getChannel();
            if (channel != -1) {
                soundPool.pause(channel);
            }
        }
    }

    @Override
    public void deleteAudioData(AudioData ad) {
        for (AudioNode src : musicPlaying.keySet()) {
            if (src.getAudioData() == ad) {
                MediaPlayer mp = musicPlaying.remove(src);
                mp.stop();
                mp.release();
                src.setStatus(Status.Stopped);
                src.setChannel(-1);
                ad.setId(-1);
                break;
            }
        }
        if (ad.getId() > 0) {
            soundPool.unload(ad.getId());
            ad.setId(-1);
        }
    }

    @Override
    public void setEnvironment(Environment env) {
    }

    @Override
    public void deleteFilter(Filter filter) {
    }
}
