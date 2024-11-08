package com.frinika.softsynth.osc;

import com.frinika.softsynth.string.*;
import java.io.IOException;
import java.util.Random;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.Patch;
import javax.sound.midi.VoiceStatus;
import com.sun.media.sound.ModelConnectionBlock;
import com.sun.media.sound.ModelDestination;
import com.sun.media.sound.ModelIdentifier;
import com.sun.media.sound.ModelOscillator;
import com.sun.media.sound.ModelOscillatorStream;
import com.sun.media.sound.ModelPerformer;
import com.sun.media.sound.SimpleInstrument;
import com.sun.media.sound.SimpleSoundbank;

public class Osc extends SimpleSoundbank {

    static float halfLife = .01f;

    static boolean halfString = false;

    static float tap = 5f;

    static float pluck = 10.0f;

    public static class MyOscillatorStream implements ModelOscillatorStream {

        float pitch = 60;

        float samplerate;

        private double freq;

        private int cnt;

        private MidiChannel channel;

        public MyOscillatorStream(float samplerate) {
            this.samplerate = samplerate;
            cnt = 0;
        }

        public void close() throws IOException {
        }

        public void noteOff(int velocity) {
        }

        public void noteOn(MidiChannel channel, VoiceStatus voice, int noteNumber, int velocity) {
            this.channel = channel;
        }

        public int read(float[][] buffers, int offset, int len) throws IOException {
            for (int i = offset; i < len; i++) {
                cnt++;
                buffers[0][i] = 0.9f * (float) Math.cos(cnt / samplerate * 2 * Math.PI * freq);
            }
            return len;
        }

        public void setPitch(float pitch) {
            if (pitch == this.pitch) {
                return;
            }
            this.pitch = pitch;
            freq = Math.exp((pitch - 6900) * (Math.log(2.0) / 1200.0)) * (440);
        }
    }

    public Osc() {
        ModelOscillator osc = new ModelOscillator() {

            public float getAttenuation() {
                return 0;
            }

            public int getChannels() {
                return 1;
            }

            public ModelOscillatorStream open(float samplerate) {
                return new MyOscillatorStream(samplerate);
            }
        };
        ModelPerformer performer = new ModelPerformer();
        performer.getOscillators().add(osc);
        performer.getConnectionBlocks().add(new ModelConnectionBlock(Double.NEGATIVE_INFINITY, new ModelDestination(new ModelIdentifier("eg", "attack", 0))));
        performer.getConnectionBlocks().add(new ModelConnectionBlock(1000.0, new ModelDestination(new ModelIdentifier("eg", "sustain", 0))));
        performer.getConnectionBlocks().add(new ModelConnectionBlock(1200.0, new ModelDestination(new ModelIdentifier("eg", "release", 0))));
        SimpleInstrument ins = new SimpleInstrument();
        ins.setName("Simple Pluck2");
        ins.add(performer);
        ins.setPatch(new Patch(0, 0));
        addInstrument(ins);
    }
}
