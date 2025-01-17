package com.jme3.audio.lwjgl;

import com.jme3.audio.AudioNode.Status;
import com.jme3.audio.*;
import com.jme3.math.Vector3f;
import com.jme3.util.BufferUtils;
import com.jme3.util.NativeObjectManager;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.lwjgl.LWJGLException;
import static org.lwjgl.openal.AL10.*;
import org.lwjgl.openal.*;

public class LwjglAudioRenderer implements AudioRenderer, Runnable {

    private static final Logger logger = Logger.getLogger(LwjglAudioRenderer.class.getName());

    private final NativeObjectManager objManager = new NativeObjectManager();

    private static final int BUFFER_SIZE = 35280;

    private static final int STREAMING_BUFFER_COUNT = 5;

    private static final int MAX_NUM_CHANNELS = 64;

    private IntBuffer ib = BufferUtils.createIntBuffer(1);

    private final FloatBuffer fb = BufferUtils.createVector3Buffer(2);

    private final ByteBuffer nativeBuf = BufferUtils.createByteBuffer(BUFFER_SIZE);

    private final byte[] arrayBuf = new byte[BUFFER_SIZE];

    private int[] channels;

    private AudioNode[] chanSrcs;

    private int nextChan = 0;

    private ArrayList<Integer> freeChans = new ArrayList<Integer>();

    private Listener listener;

    private boolean audioDisabled = false;

    private boolean supportEfx = false;

    private int auxSends = 0;

    private int reverbFx = -1;

    private int reverbFxSlot = -1;

    private static final float UPDATE_RATE = 0.05f;

    private final Thread audioThread = new Thread(this, "jME3 Audio Thread");

    private final AtomicBoolean threadLock = new AtomicBoolean(false);

    public LwjglAudioRenderer() {
    }

    public void initialize() {
        if (!audioThread.isAlive()) {
            audioThread.setDaemon(true);
            audioThread.setPriority(Thread.NORM_PRIORITY + 1);
            audioThread.start();
        } else {
            throw new IllegalStateException("Initialize already called");
        }
    }

    private void checkDead() {
        if (audioThread.getState() == Thread.State.TERMINATED) throw new IllegalStateException("Audio thread is terminated");
    }

    public void run() {
        initInThread();
        synchronized (threadLock) {
            threadLock.set(true);
            threadLock.notifyAll();
        }
        long updateRateNanos = (long) (UPDATE_RATE * 1000000000);
        mainloop: while (true) {
            long startTime = System.nanoTime();
            if (Thread.interrupted()) break;
            synchronized (threadLock) {
                updateInThread(UPDATE_RATE);
            }
            long endTime = System.nanoTime();
            long diffTime = endTime - startTime;
            if (diffTime < updateRateNanos) {
                long desiredEndTime = startTime + updateRateNanos;
                while (System.nanoTime() < desiredEndTime) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException ex) {
                        break mainloop;
                    }
                }
            }
        }
        synchronized (threadLock) {
            cleanupInThread();
        }
    }

    public void initInThread() {
        try {
            if (!AL.isCreated()) {
                AL.create();
            }
        } catch (OpenALException ex) {
            logger.log(Level.SEVERE, "Failed to load audio library", ex);
            audioDisabled = true;
            return;
        } catch (LWJGLException ex) {
            logger.log(Level.SEVERE, "Failed to load audio library", ex);
            audioDisabled = true;
            return;
        } catch (UnsatisfiedLinkError ex) {
            logger.log(Level.SEVERE, "Failed to load audio library", ex);
            audioDisabled = true;
            return;
        }
        ALCdevice device = AL.getDevice();
        String deviceName = ALC10.alcGetString(device, ALC10.ALC_DEVICE_SPECIFIER);
        logger.log(Level.FINER, "Audio Device: {0}", deviceName);
        logger.log(Level.FINER, "Audio Vendor: {0}", alGetString(AL_VENDOR));
        logger.log(Level.FINER, "Audio Renderer: {0}", alGetString(AL_RENDERER));
        logger.log(Level.FINER, "Audio Version: {0}", alGetString(AL_VERSION));
        ArrayList<Integer> channelList = new ArrayList<Integer>();
        for (int i = 0; i < MAX_NUM_CHANNELS; i++) {
            int chan = alGenSources();
            if (alGetError() != 0) {
                break;
            } else {
                channelList.add(chan);
            }
        }
        channels = new int[channelList.size()];
        for (int i = 0; i < channels.length; i++) {
            channels[i] = channelList.get(i);
        }
        ib = BufferUtils.createIntBuffer(channels.length);
        chanSrcs = new AudioNode[channels.length];
        logger.log(Level.INFO, "AudioRenderer supports {0} channels", channels.length);
        supportEfx = ALC10.alcIsExtensionPresent(device, "ALC_EXT_EFX");
        if (supportEfx) {
            ib.position(0).limit(1);
            ALC10.alcGetInteger(device, EFX10.ALC_EFX_MAJOR_VERSION, ib);
            int major = ib.get(0);
            ib.position(0).limit(1);
            ALC10.alcGetInteger(device, EFX10.ALC_EFX_MINOR_VERSION, ib);
            int minor = ib.get(0);
            logger.log(Level.INFO, "Audio effect extension version: {0}.{1}", new Object[] { major, minor });
            ALC10.alcGetInteger(device, EFX10.ALC_MAX_AUXILIARY_SENDS, ib);
            auxSends = ib.get(0);
            logger.log(Level.INFO, "Audio max auxilary sends: {0}", auxSends);
            ib.position(0).limit(1);
            EFX10.alGenAuxiliaryEffectSlots(ib);
            reverbFxSlot = ib.get(0);
            ib.position(0).limit(1);
            EFX10.alGenEffects(ib);
            reverbFx = ib.get(0);
            EFX10.alEffecti(reverbFx, EFX10.AL_EFFECT_TYPE, EFX10.AL_EFFECT_REVERB);
            EFX10.alAuxiliaryEffectSloti(reverbFxSlot, EFX10.AL_EFFECTSLOT_EFFECT, reverbFx);
        } else {
            logger.log(Level.WARNING, "OpenAL EFX not available! Audio effects won't work.");
        }
    }

    public void cleanupInThread() {
        if (audioDisabled) {
            AL.destroy();
            return;
        }
        for (int i = 0; i < chanSrcs.length; i++) {
            if (chanSrcs[i] != null) {
                clearChannel(i);
            }
        }
        ib.clear();
        ib.put(channels);
        ib.flip();
        alDeleteSources(ib);
        objManager.deleteAllObjects(this);
        if (supportEfx) {
            ib.position(0).limit(1);
            ib.put(0, reverbFx);
            EFX10.alDeleteEffects(ib);
            ib.position(0).limit(1);
            ib.put(0, reverbFxSlot);
            EFX10.alDeleteAuxiliaryEffectSlots(ib);
        }
        AL.destroy();
    }

    public void cleanup() {
        if (audioThread.isAlive()) {
            audioThread.interrupt();
        }
    }

    private void updateFilter(Filter f) {
        int id = f.getId();
        if (id == -1) {
            ib.position(0).limit(1);
            EFX10.alGenFilters(ib);
            id = ib.get(0);
            f.setId(id);
            objManager.registerForCleanup(f);
        }
        if (f instanceof LowPassFilter) {
            LowPassFilter lpf = (LowPassFilter) f;
            EFX10.alFilteri(id, EFX10.AL_FILTER_TYPE, EFX10.AL_FILTER_LOWPASS);
            EFX10.alFilterf(id, EFX10.AL_LOWPASS_GAIN, lpf.getVolume());
            EFX10.alFilterf(id, EFX10.AL_LOWPASS_GAINHF, lpf.getHighFreqVolume());
        } else {
            throw new UnsupportedOperationException("Filter type unsupported: " + f.getClass().getName());
        }
        f.clearUpdateNeeded();
    }

    public void updateSourceParam(AudioNode src, AudioParam param) {
        checkDead();
        synchronized (threadLock) {
            while (!threadLock.get()) {
                try {
                    threadLock.wait();
                } catch (InterruptedException ex) {
                }
            }
            if (audioDisabled) return;
            if (src.getChannel() < 0) return;
            assert src.getChannel() >= 0;
            int id = channels[src.getChannel()];
            switch(param) {
                case Position:
                    if (!src.isPositional()) return;
                    Vector3f pos = src.getWorldTranslation();
                    alSource3f(id, AL_POSITION, pos.x, pos.y, pos.z);
                    break;
                case Velocity:
                    if (!src.isPositional()) return;
                    Vector3f vel = src.getVelocity();
                    alSource3f(id, AL_VELOCITY, vel.x, vel.y, vel.z);
                    break;
                case MaxDistance:
                    if (!src.isPositional()) return;
                    alSourcef(id, AL_MAX_DISTANCE, src.getMaxDistance());
                    break;
                case RefDistance:
                    if (!src.isPositional()) return;
                    alSourcef(id, AL_REFERENCE_DISTANCE, src.getRefDistance());
                    break;
                case ReverbFilter:
                    if (!supportEfx || !src.isPositional() || !src.isReverbEnabled()) return;
                    int filter = EFX10.AL_FILTER_NULL;
                    if (src.getReverbFilter() != null) {
                        Filter f = src.getReverbFilter();
                        if (f.isUpdateNeeded()) {
                            updateFilter(f);
                        }
                        filter = f.getId();
                    }
                    AL11.alSource3i(id, EFX10.AL_AUXILIARY_SEND_FILTER, reverbFxSlot, 0, filter);
                    break;
                case ReverbEnabled:
                    if (!supportEfx || !src.isPositional()) return;
                    if (src.isReverbEnabled()) {
                        updateSourceParam(src, AudioParam.ReverbFilter);
                    } else {
                        AL11.alSource3i(id, EFX10.AL_AUXILIARY_SEND_FILTER, 0, 0, EFX10.AL_FILTER_NULL);
                    }
                    break;
                case IsPositional:
                    if (!src.isPositional()) {
                        alSourcei(id, AL_SOURCE_RELATIVE, AL_TRUE);
                        alSource3f(id, AL_POSITION, 0, 0, 0);
                        alSource3f(id, AL_VELOCITY, 0, 0, 0);
                    } else {
                        alSourcei(id, AL_SOURCE_RELATIVE, AL_FALSE);
                        updateSourceParam(src, AudioParam.Position);
                        updateSourceParam(src, AudioParam.Velocity);
                        updateSourceParam(src, AudioParam.MaxDistance);
                        updateSourceParam(src, AudioParam.RefDistance);
                        updateSourceParam(src, AudioParam.ReverbEnabled);
                    }
                    break;
                case Direction:
                    if (!src.isDirectional()) return;
                    Vector3f dir = src.getDirection();
                    alSource3f(id, AL_DIRECTION, dir.x, dir.y, dir.z);
                    break;
                case InnerAngle:
                    if (!src.isDirectional()) return;
                    alSourcef(id, AL_CONE_INNER_ANGLE, src.getInnerAngle());
                    break;
                case OuterAngle:
                    if (!src.isDirectional()) return;
                    alSourcef(id, AL_CONE_OUTER_ANGLE, src.getOuterAngle());
                    break;
                case IsDirectional:
                    if (src.isDirectional()) {
                        updateSourceParam(src, AudioParam.Direction);
                        updateSourceParam(src, AudioParam.InnerAngle);
                        updateSourceParam(src, AudioParam.OuterAngle);
                        alSourcef(id, AL_CONE_OUTER_GAIN, 0);
                    } else {
                        alSourcef(id, AL_CONE_INNER_ANGLE, 360);
                        alSourcef(id, AL_CONE_OUTER_ANGLE, 360);
                        alSourcef(id, AL_CONE_OUTER_GAIN, 1f);
                    }
                    break;
                case DryFilter:
                    if (!supportEfx) return;
                    if (src.getDryFilter() != null) {
                        Filter f = src.getDryFilter();
                        if (f.isUpdateNeeded()) {
                            updateFilter(f);
                            alSourcei(id, EFX10.AL_DIRECT_FILTER, f.getId());
                        }
                    } else {
                        alSourcei(id, EFX10.AL_DIRECT_FILTER, EFX10.AL_FILTER_NULL);
                    }
                    break;
                case Looping:
                    if (src.isLooping()) {
                        if (!(src.getAudioData() instanceof AudioStream)) {
                            alSourcei(id, AL_LOOPING, AL_TRUE);
                        }
                    } else {
                        alSourcei(id, AL_LOOPING, AL_FALSE);
                    }
                    break;
                case Volume:
                    alSourcef(id, AL_GAIN, src.getVolume());
                    break;
                case Pitch:
                    alSourcef(id, AL_PITCH, src.getPitch());
                    break;
            }
        }
    }

    private void setSourceParams(int id, AudioNode src, boolean forceNonLoop) {
        if (src.isPositional()) {
            Vector3f pos = src.getWorldTranslation();
            Vector3f vel = src.getVelocity();
            alSource3f(id, AL_POSITION, pos.x, pos.y, pos.z);
            alSource3f(id, AL_VELOCITY, vel.x, vel.y, vel.z);
            alSourcef(id, AL_MAX_DISTANCE, src.getMaxDistance());
            alSourcef(id, AL_REFERENCE_DISTANCE, src.getRefDistance());
            alSourcei(id, AL_SOURCE_RELATIVE, AL_FALSE);
            if (src.isReverbEnabled() && supportEfx) {
                int filter = EFX10.AL_FILTER_NULL;
                if (src.getReverbFilter() != null) {
                    Filter f = src.getReverbFilter();
                    if (f.isUpdateNeeded()) {
                        updateFilter(f);
                    }
                    filter = f.getId();
                }
                AL11.alSource3i(id, EFX10.AL_AUXILIARY_SEND_FILTER, reverbFxSlot, 0, filter);
            }
        } else {
            alSourcei(id, AL_SOURCE_RELATIVE, AL_TRUE);
            alSource3f(id, AL_POSITION, 0, 0, 0);
            alSource3f(id, AL_VELOCITY, 0, 0, 0);
        }
        if (src.getDryFilter() != null && supportEfx) {
            Filter f = src.getDryFilter();
            if (f.isUpdateNeeded()) {
                updateFilter(f);
                alSourcei(id, EFX10.AL_DIRECT_FILTER, f.getId());
            }
        }
        if (forceNonLoop) {
            alSourcei(id, AL_LOOPING, AL_FALSE);
        } else {
            alSourcei(id, AL_LOOPING, src.isLooping() ? AL_TRUE : AL_FALSE);
        }
        alSourcef(id, AL_GAIN, src.getVolume());
        alSourcef(id, AL_PITCH, src.getPitch());
        alSourcef(id, AL11.AL_SEC_OFFSET, src.getTimeOffset());
        if (src.isDirectional()) {
            Vector3f dir = src.getDirection();
            alSource3f(id, AL_DIRECTION, dir.x, dir.y, dir.z);
            alSourcef(id, AL_CONE_INNER_ANGLE, src.getInnerAngle());
            alSourcef(id, AL_CONE_OUTER_ANGLE, src.getOuterAngle());
            alSourcef(id, AL_CONE_OUTER_GAIN, 0);
        } else {
            alSourcef(id, AL_CONE_INNER_ANGLE, 360);
            alSourcef(id, AL_CONE_OUTER_ANGLE, 360);
            alSourcef(id, AL_CONE_OUTER_GAIN, 1f);
        }
    }

    public void updateListenerParam(Listener listener, ListenerParam param) {
        checkDead();
        synchronized (threadLock) {
            while (!threadLock.get()) {
                try {
                    threadLock.wait();
                } catch (InterruptedException ex) {
                }
            }
            if (audioDisabled) return;
            switch(param) {
                case Position:
                    Vector3f pos = listener.getLocation();
                    alListener3f(AL_POSITION, pos.x, pos.y, pos.z);
                    break;
                case Rotation:
                    Vector3f dir = listener.getDirection();
                    Vector3f up = listener.getUp();
                    fb.rewind();
                    fb.put(dir.x).put(dir.y).put(dir.z);
                    fb.put(up.x).put(up.y).put(up.z);
                    fb.flip();
                    alListener(AL_ORIENTATION, fb);
                    break;
                case Velocity:
                    Vector3f vel = listener.getVelocity();
                    alListener3f(AL_VELOCITY, vel.x, vel.y, vel.z);
                    break;
                case Volume:
                    alListenerf(AL_GAIN, listener.getVolume());
                    break;
            }
        }
    }

    private void setListenerParams(Listener listener) {
        Vector3f pos = listener.getLocation();
        Vector3f vel = listener.getVelocity();
        Vector3f dir = listener.getDirection();
        Vector3f up = listener.getUp();
        alListener3f(AL_POSITION, pos.x, pos.y, pos.z);
        alListener3f(AL_VELOCITY, vel.x, vel.y, vel.z);
        fb.rewind();
        fb.put(dir.x).put(dir.y).put(dir.z);
        fb.put(up.x).put(up.y).put(up.z);
        fb.flip();
        alListener(AL_ORIENTATION, fb);
        alListenerf(AL_GAIN, listener.getVolume());
    }

    private int newChannel() {
        if (freeChans.size() > 0) return freeChans.remove(0); else if (nextChan < channels.length) {
            return nextChan++;
        } else {
            return -1;
        }
    }

    private void freeChannel(int index) {
        if (index == nextChan - 1) {
            nextChan--;
        } else {
            freeChans.add(index);
        }
    }

    public void setEnvironment(Environment env) {
        checkDead();
        synchronized (threadLock) {
            while (!threadLock.get()) {
                try {
                    threadLock.wait();
                } catch (InterruptedException ex) {
                }
            }
            if (audioDisabled || !supportEfx) return;
            EFX10.alEffectf(reverbFx, EFX10.AL_REVERB_DENSITY, env.getDensity());
            EFX10.alEffectf(reverbFx, EFX10.AL_REVERB_DIFFUSION, env.getDiffusion());
            EFX10.alEffectf(reverbFx, EFX10.AL_REVERB_GAIN, env.getGain());
            EFX10.alEffectf(reverbFx, EFX10.AL_REVERB_GAINHF, env.getGainHf());
            EFX10.alEffectf(reverbFx, EFX10.AL_REVERB_DECAY_TIME, env.getDecayTime());
            EFX10.alEffectf(reverbFx, EFX10.AL_REVERB_DECAY_HFRATIO, env.getDecayHFRatio());
            EFX10.alEffectf(reverbFx, EFX10.AL_REVERB_REFLECTIONS_GAIN, env.getReflectGain());
            EFX10.alEffectf(reverbFx, EFX10.AL_REVERB_REFLECTIONS_DELAY, env.getReflectDelay());
            EFX10.alEffectf(reverbFx, EFX10.AL_REVERB_LATE_REVERB_GAIN, env.getLateReverbGain());
            EFX10.alEffectf(reverbFx, EFX10.AL_REVERB_LATE_REVERB_DELAY, env.getLateReverbDelay());
            EFX10.alEffectf(reverbFx, EFX10.AL_REVERB_AIR_ABSORPTION_GAINHF, env.getAirAbsorbGainHf());
            EFX10.alEffectf(reverbFx, EFX10.AL_REVERB_ROOM_ROLLOFF_FACTOR, env.getRoomRolloffFactor());
            EFX10.alAuxiliaryEffectSloti(reverbFxSlot, EFX10.AL_EFFECTSLOT_EFFECT, reverbFx);
        }
    }

    private boolean fillBuffer(AudioStream stream, int id) {
        int size = 0;
        int result;
        while (size < arrayBuf.length) {
            result = stream.readSamples(arrayBuf, size, arrayBuf.length - size);
            if (result > 0) {
                size += result;
            } else {
                break;
            }
        }
        if (size == 0) return false;
        nativeBuf.clear();
        nativeBuf.put(arrayBuf, 0, size);
        nativeBuf.flip();
        alBufferData(id, convertFormat(stream), nativeBuf, stream.getSampleRate());
        return true;
    }

    private boolean fillStreamingSource(int sourceId, AudioStream stream) {
        if (!stream.isOpen()) return false;
        boolean active = true;
        int processed = alGetSourcei(sourceId, AL_BUFFERS_PROCESSED);
        if (processed > 0) {
            int buffer;
            ib.position(0).limit(1);
            alSourceUnqueueBuffers(sourceId, ib);
            buffer = ib.get(0);
            active = fillBuffer(stream, buffer);
            ib.position(0).limit(1);
            ib.put(0, buffer);
            alSourceQueueBuffers(sourceId, ib);
        }
        if (!active && stream.isOpen()) stream.close();
        return active;
    }

    private boolean attachStreamToSource(int sourceId, AudioStream stream) {
        boolean active = true;
        for (int id : stream.getIds()) {
            active = fillBuffer(stream, id);
            ib.position(0).limit(1);
            ib.put(id).flip();
            alSourceQueueBuffers(sourceId, ib);
        }
        return active;
    }

    private boolean attachBufferToSource(int sourceId, AudioBuffer buffer) {
        alSourcei(sourceId, AL_BUFFER, buffer.getId());
        return true;
    }

    private boolean attachAudioToSource(int sourceId, AudioData data) {
        if (data instanceof AudioBuffer) {
            return attachBufferToSource(sourceId, (AudioBuffer) data);
        } else if (data instanceof AudioStream) {
            return attachStreamToSource(sourceId, (AudioStream) data);
        }
        throw new UnsupportedOperationException();
    }

    private void clearChannel(int index) {
        if (chanSrcs[index] != null) {
            AudioNode src = chanSrcs[index];
            int sourceId = channels[index];
            alSourceStop(sourceId);
            if (src.getAudioData() instanceof AudioStream) {
                AudioStream str = (AudioStream) src.getAudioData();
                ib.position(0).limit(STREAMING_BUFFER_COUNT);
                ib.put(str.getIds()).flip();
                alSourceUnqueueBuffers(sourceId, ib);
            } else if (src.getAudioData() instanceof AudioBuffer) {
                alSourcei(sourceId, AL_BUFFER, 0);
            }
            if (src.getDryFilter() != null && supportEfx) {
                alSourcei(sourceId, EFX10.AL_DIRECT_FILTER, EFX10.AL_FILTER_NULL);
            }
            if (src.isPositional()) {
                AudioNode pas = (AudioNode) src;
                if (pas.isReverbEnabled() && supportEfx) {
                    AL11.alSource3i(sourceId, EFX10.AL_AUXILIARY_SEND_FILTER, 0, 0, EFX10.AL_FILTER_NULL);
                }
            }
            chanSrcs[index] = null;
        }
    }

    public void update(float tpf) {
    }

    public void updateInThread(float tpf) {
        if (audioDisabled) return;
        for (int i = 0; i < channels.length; i++) {
            AudioNode src = chanSrcs[i];
            if (src == null) continue;
            int sourceId = channels[i];
            boolean boundSource = i == src.getChannel();
            boolean streaming = src.getAudioData() instanceof AudioStream;
            assert (boundSource && streaming) || (!streaming);
            int state = alGetSourcei(sourceId, AL_SOURCE_STATE);
            boolean wantPlaying = src.getStatus() == Status.Playing;
            boolean stopped = state == AL_STOPPED;
            if (streaming && wantPlaying) {
                AudioStream stream = (AudioStream) src.getAudioData();
                if (stream.isOpen()) {
                    fillStreamingSource(sourceId, stream);
                    if (stopped) alSourcePlay(sourceId);
                } else {
                    if (stopped) {
                        src.setStatus(Status.Stopped);
                        src.setChannel(-1);
                        clearChannel(i);
                        freeChannel(i);
                        deleteAudioData(stream);
                    }
                }
            } else if (!streaming) {
                boolean paused = state == AL_PAUSED;
                assert (src.getStatus() == Status.Paused && paused) || (!paused);
                if (stopped) {
                    if (boundSource) {
                        src.setStatus(Status.Stopped);
                        src.setChannel(-1);
                    }
                    clearChannel(i);
                    freeChannel(i);
                }
            }
        }
        objManager.deleteUnused(this);
    }

    public void setListener(Listener listener) {
        checkDead();
        synchronized (threadLock) {
            while (!threadLock.get()) {
                try {
                    threadLock.wait();
                } catch (InterruptedException ex) {
                }
            }
            if (audioDisabled) return;
            if (this.listener != null) {
                this.listener.setRenderer(null);
            }
            this.listener = listener;
            this.listener.setRenderer(this);
            setListenerParams(listener);
        }
    }

    public void playSourceInstance(AudioNode src) {
        checkDead();
        synchronized (threadLock) {
            while (!threadLock.get()) {
                try {
                    threadLock.wait();
                } catch (InterruptedException ex) {
                }
            }
            if (audioDisabled) return;
            if (src.getAudioData() instanceof AudioStream) throw new UnsupportedOperationException("Cannot play instances " + "of audio streams. Use playSource() instead.");
            if (src.getAudioData().isUpdateNeeded()) {
                updateAudioData(src.getAudioData());
            }
            int index = newChannel();
            if (index == -1) return;
            int sourceId = channels[index];
            clearChannel(index);
            setSourceParams(sourceId, src, true);
            attachAudioToSource(sourceId, src.getAudioData());
            chanSrcs[index] = src;
            alSourcePlay(sourceId);
        }
    }

    public void playSource(AudioNode src) {
        checkDead();
        synchronized (threadLock) {
            while (!threadLock.get()) {
                try {
                    threadLock.wait();
                } catch (InterruptedException ex) {
                }
            }
            if (audioDisabled) return;
            if (src.getStatus() == Status.Playing) {
                return;
            } else if (src.getStatus() == Status.Stopped) {
                int index = newChannel();
                if (index == -1) {
                    logger.log(Level.WARNING, "No channel available to play {0}", src);
                    return;
                }
                clearChannel(index);
                src.setChannel(index);
                AudioData data = src.getAudioData();
                if (data.isUpdateNeeded()) updateAudioData(data);
                chanSrcs[index] = src;
                setSourceParams(channels[index], src, false);
                attachAudioToSource(channels[index], data);
            }
            alSourcePlay(channels[src.getChannel()]);
            src.setStatus(Status.Playing);
        }
    }

    public void pauseSource(AudioNode src) {
        checkDead();
        synchronized (threadLock) {
            while (!threadLock.get()) {
                try {
                    threadLock.wait();
                } catch (InterruptedException ex) {
                }
            }
            if (audioDisabled) return;
            if (src.getStatus() == Status.Playing) {
                assert src.getChannel() != -1;
                alSourcePause(channels[src.getChannel()]);
                src.setStatus(Status.Paused);
            }
        }
    }

    public void stopSource(AudioNode src) {
        synchronized (threadLock) {
            while (!threadLock.get()) {
                try {
                    threadLock.wait();
                } catch (InterruptedException ex) {
                }
            }
            if (audioDisabled) return;
            if (src.getStatus() != Status.Stopped) {
                int chan = src.getChannel();
                assert chan != -1;
                src.setStatus(Status.Stopped);
                src.setChannel(-1);
                clearChannel(chan);
                freeChannel(chan);
                if (src.getAudioData() instanceof AudioStream) {
                    AudioStream stream = (AudioStream) src.getAudioData();
                    if (stream.isOpen()) {
                        stream.close();
                    }
                    deleteAudioData(src.getAudioData());
                }
            }
        }
    }

    private int convertFormat(AudioData ad) {
        switch(ad.getBitsPerSample()) {
            case 8:
                if (ad.getChannels() == 1) return AL_FORMAT_MONO8; else if (ad.getChannels() == 2) return AL_FORMAT_STEREO8;
                break;
            case 16:
                if (ad.getChannels() == 1) return AL_FORMAT_MONO16; else return AL_FORMAT_STEREO16;
        }
        throw new UnsupportedOperationException("Unsupported channels/bits combination: " + "bits=" + ad.getBitsPerSample() + ", channels=" + ad.getChannels());
    }

    private void updateAudioBuffer(AudioBuffer ab) {
        int id = ab.getId();
        if (ab.getId() == -1) {
            ib.position(0).limit(1);
            alGenBuffers(ib);
            id = ib.get(0);
            ab.setId(id);
            objManager.registerForCleanup(ab);
        }
        ab.getData().clear();
        alBufferData(id, convertFormat(ab), ab.getData(), ab.getSampleRate());
        ab.clearUpdateNeeded();
    }

    private void updateAudioStream(AudioStream as) {
        if (as.getIds() != null) {
            deleteAudioData(as);
        }
        int[] ids = new int[STREAMING_BUFFER_COUNT];
        ib.position(0).limit(STREAMING_BUFFER_COUNT);
        alGenBuffers(ib);
        ib.position(0).limit(STREAMING_BUFFER_COUNT);
        ib.get(ids);
        as.setIds(ids);
        as.clearUpdateNeeded();
    }

    private void updateAudioData(AudioData ad) {
        if (ad instanceof AudioBuffer) {
            updateAudioBuffer((AudioBuffer) ad);
        } else if (ad instanceof AudioStream) {
            updateAudioStream((AudioStream) ad);
        }
    }

    public void deleteFilter(Filter filter) {
        int id = filter.getId();
        if (id != -1) {
            EFX10.alDeleteFilters(id);
        }
    }

    public void deleteAudioData(AudioData ad) {
        synchronized (threadLock) {
            while (!threadLock.get()) {
                try {
                    threadLock.wait();
                } catch (InterruptedException ex) {
                }
            }
            if (audioDisabled) return;
            if (ad instanceof AudioBuffer) {
                AudioBuffer ab = (AudioBuffer) ad;
                int id = ab.getId();
                if (id != -1) {
                    ib.put(0, id);
                    ib.position(0).limit(1);
                    alDeleteBuffers(ib);
                    ab.resetObject();
                }
            } else if (ad instanceof AudioStream) {
                AudioStream as = (AudioStream) ad;
                int[] ids = as.getIds();
                if (ids != null) {
                    ib.clear();
                    ib.put(ids).flip();
                    alDeleteBuffers(ib);
                    as.resetObject();
                }
            }
        }
    }
}
