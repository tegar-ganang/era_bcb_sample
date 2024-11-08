package uk.org.toot.synth;

import java.util.List;
import uk.org.toot.audio.core.AudioBuffer;
import uk.org.toot.audio.core.AudioProcess;
import uk.org.toot.audio.core.ChannelFormat;
import uk.org.toot.audio.system.AudioOutput;

/**
 * A PolyphonicSynthChannel is a SynthChannel that generates audio as an AudioProcess.
 * It is polyphonic, supporting multiple Voices.
 * 
 * @author st
 */
public abstract class PolyphonicSynthChannel extends SynthChannel implements AudioOutput {

    private List<Voice> voices = new java.util.ArrayList<Voice>();

    private List<Voice> finished = new java.util.ArrayList<Voice>();

    private int polyphony = 8;

    private AudioBuffer.MetaInfo info;

    private String name;

    private String location;

    public PolyphonicSynthChannel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setLocation(String location) {
        this.location = location;
        info = new AudioBuffer.MetaInfo(name, location);
    }

    public String getLocation() {
        return location;
    }

    public void open() {
    }

    public int processAudio(AudioBuffer buffer) {
        buffer.setMetaInfo(info);
        buffer.setChannelFormat(ChannelFormat.MONO);
        buffer.makeSilence();
        finished.clear();
        int sr = (int) buffer.getSampleRate();
        int nactive = 0;
        synchronized (voices) {
            if (sr != sampleRate) {
                setSampleRate(sr);
                for (Voice voice : voices) {
                    voice.setSampleRate(sampleRate);
                }
            }
            for (Voice voice : voices) {
                if (!voice.mix(buffer)) {
                    finished.add(voice);
                } else {
                    nactive += 1;
                }
            }
            for (Voice voice : finished) {
                voices.remove(voice);
            }
        }
        return nactive > 0 ? AudioProcess.AUDIO_OK : AudioProcess.AUDIO_SILENCE;
    }

    public void close() {
    }

    public void setPolyphony(int p) {
        polyphony = p;
    }

    public int getPolyphony() {
        return polyphony;
    }

    public void noteOn(int pitch, int velocity) {
        Voice v = createVoice(pitch, velocity, sampleRate);
        synchronized (voices) {
            if (voices.size() >= polyphony) {
                for (Voice voice : voices) {
                    if (voice.isReleased()) {
                        voice.stop();
                        voices.remove(voice);
                        break;
                    }
                }
            }
            voices.add(v);
        }
    }

    public void noteOff(int pitch) {
        synchronized (voices) {
            for (Voice voice : voices) {
                if (voice.getPitch() == pitch && !voice.isReleased()) {
                    voice.release();
                    return;
                }
            }
        }
    }

    public void allNotesOff() {
        synchronized (voices) {
            for (Voice voice : voices) {
                voice.release();
            }
        }
    }

    public void allSoundOff() {
        synchronized (voices) {
            for (Voice voice : voices) {
                voice.stop();
            }
        }
    }

    protected abstract Voice createVoice(int pitch, int velocity, int sampleRate);

    public interface Voice {

        int getPitch();

        void release();

        boolean isReleased();

        void stop();

        void setSampleRate(int sr);

        boolean mix(AudioBuffer buffer);
    }

    public abstract class AbstractVoice implements Voice {

        protected int pitch;

        protected int velocity;

        protected float amplitude;

        protected float frequency;

        protected boolean release = false;

        protected boolean stop = false;

        public AbstractVoice(int pitch, int velocity) {
            this.pitch = pitch;
            this.velocity = velocity;
            amplitude = (float) velocity / 128;
            frequency = midiFreq(pitch);
        }

        public int getPitch() {
            return pitch;
        }

        public void release() {
            release = true;
        }

        public boolean isReleased() {
            return release;
        }

        public void stop() {
            stop = true;
        }

        public boolean mix(AudioBuffer buffer) {
            if (stop) return false;
            float[] samples = buffer.getChannel(0);
            int nsamples = buffer.getSampleCount();
            for (int i = 0; i < nsamples; i++) {
                samples[i] += getSample();
            }
            return !isComplete();
        }

        protected abstract float getSample();

        protected abstract boolean isComplete();
    }
}
