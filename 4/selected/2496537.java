package dk.impact.sheeplifter.audio.openal;

import java.util.logging.Logger;
import org.lwjgl.openal.AL10;
import com.jme.math.Vector3f;
import com.jmex.audio.AudioBuffer;
import com.jmex.audio.AudioTrack;
import com.jmex.audio.openal.OpenALAudioBuffer;
import com.jmex.audio.openal.OpenALPropertyTool;
import com.jmex.audio.openal.OpenALSource;
import com.jmex.audio.player.MemoryAudioPlayer;

/**
 * @see MemoryAudioPlayer
 * @author Joshua Slack
 * @version $Id: OpenALMemoryAudioPlayer.java,v 1.3 2007/08/02 22:27:16 nca Exp $
 */
public class SheepOpenALMemoryAudioPlayer extends MemoryAudioPlayer {

    private static final Logger logger = Logger.getLogger(SheepOpenALMemoryAudioPlayer.class.getName());

    private OpenALSource source;

    private boolean isPaused = false;

    public SheepOpenALMemoryAudioPlayer(AudioBuffer buffer, AudioTrack parent) {
        super(buffer, parent);
    }

    @Override
    public void init() {
    }

    @Override
    public void cleanup() {
    }

    @Override
    public boolean isPlaying() {
        return source != null && source.getState() == AL10.AL_PLAYING;
    }

    @Override
    public boolean isActive() {
        return source != null && (source.getState() == AL10.AL_PLAYING || source.getState() == AL10.AL_PAUSED);
    }

    @Override
    public boolean isStopped() {
        return source != null && source.getState() == AL10.AL_STOPPED;
    }

    @Override
    public void pause() {
        isPaused = true;
        AL10.alSourcePause(source.getId());
        setPauseTime(System.currentTimeMillis());
    }

    @Override
    public void play() {
        synchronized (this) {
            if (isPaused) {
                isPaused = false;
                AL10.alSourcePlay(source.getId());
                setStartTime(getStartTime() + System.currentTimeMillis() - getPauseTime());
                return;
            }
            source = ((SheepOpenALSystem) SheepAudioSystem.getSystem()).getNextFreeMemSource();
            if (source == null) return;
            source.setTrack(getTrack());
            applyTrackProperties();
            AL10.alSource3f(source.getId(), AL10.AL_POSITION, 0, 0, 0);
            AL10.alSource3f(source.getId(), AL10.AL_VELOCITY, 0, 0, 0);
            AL10.alSource3f(source.getId(), AL10.AL_DIRECTION, 0, 0, 0);
            AL10.alSourcei(source.getId(), AL10.AL_SOURCE_RELATIVE, getTrack().isRelative() ? AL10.AL_TRUE : AL10.AL_FALSE);
            AL10.alSourcei(source.getId(), AL10.AL_BUFFER, ((OpenALAudioBuffer) getBuffer()).getId());
            AL10.alSourcePlay(source.getId());
            setStartTime(System.currentTimeMillis());
        }
    }

    @Override
    public void applyTrackProperties() {
        OpenALPropertyTool.applyProperties(this, source);
        if (source != null) AL10.alSourcei(source.getId(), AL10.AL_LOOPING, isLoop() ? AL10.AL_TRUE : AL10.AL_FALSE);
    }

    @Override
    public void stop() {
        synchronized (this) {
            if (source == null) return;
            AL10.alSourceStop(source.getId());
            source = null;
        }
    }

    /**
     * checks OpenAL error state
     */
    protected void check() {
        int error = AL10.alGetError();
        if (error != AL10.AL_NO_ERROR) {
            logger.info("OpenAL error was raised. errorCode=" + error);
        }
    }

    @Override
    public void loop(boolean shouldLoop) {
        super.loop(shouldLoop);
        if (source != null) AL10.alSourcei(source.getId(), AL10.AL_LOOPING, shouldLoop ? AL10.AL_TRUE : AL10.AL_FALSE);
    }

    @Override
    public void updateTrackPlacement() {
        Vector3f pos = getTrack().getWorldPosition();
        Vector3f vel = getTrack().getCurrVelocity();
        AL10.alSource3f(source.getId(), AL10.AL_POSITION, pos.x, pos.y, pos.z);
        AL10.alSource3f(source.getId(), AL10.AL_VELOCITY, vel.x, vel.y, vel.z);
    }

    @Override
    public void setVolume(float volume) {
        super.setVolume(volume);
        OpenALPropertyTool.applyChannelVolume(source, volume);
    }

    @Override
    public void setPitch(float pitch) {
        if (pitch > 0f && pitch <= 2.0f) {
            super.setPitch(pitch);
            OpenALPropertyTool.applyChannelPitch(source, getPitch());
        } else logger.warning("Pitch must be > 0 and <= 2.0f");
    }

    @Override
    public void setMaxAudibleDistance(float maxDistance) {
        super.setMaxAudibleDistance(maxDistance);
        OpenALPropertyTool.applyChannelMaxAudibleDistance(source, maxDistance);
    }

    @Override
    public void setMaxVolume(float maxVolume) {
        super.setMaxVolume(maxVolume);
        OpenALPropertyTool.applyChannelMaxVolume(source, maxVolume);
    }

    @Override
    public void setMinVolume(float minVolume) {
        super.setMinVolume(minVolume);
        OpenALPropertyTool.applyChannelMinVolume(source, minVolume);
    }

    @Override
    public void setReferenceDistance(float refDistance) {
        super.setReferenceDistance(refDistance);
        OpenALPropertyTool.applyChannelReferenceDistance(source, refDistance);
    }

    @Override
    public void setRolloff(float rolloff) {
        super.setRolloff(rolloff);
        OpenALPropertyTool.applyChannelRolloff(source, rolloff);
    }

    @Override
    public int getBitRate() {
        return getBuffer().getBitRate();
    }

    @Override
    public int getChannels() {
        return getBuffer().getChannels();
    }

    @Override
    public int getDepth() {
        return getBuffer().getDepth();
    }

    @Override
    public float getLength() {
        return getBuffer().getLength();
    }
}
