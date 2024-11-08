package com.frinika.softsynth.string;

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

public class Plucked2 extends SimpleSoundbank {

    static float halfLife = .99f;

    static boolean halfString = false;

    static float tap = 5f;

    static float pluck = 1.0f;

    public static class MyOscillatorStream implements ModelOscillatorStream {

        float pitch = 60;

        float samplerate;

        private CyclicBuffer2 cyclicBuff;

        private RateConverter rateConverter;

        private double freq;

        private float thresh;

        private int cnt;

        private boolean first = true;

        private int cntThresh;

        private MidiChannel channel;

        private int ctrlLast;

        private float srcPeriod;

        public MyOscillatorStream(float samplerate) {
            float wght[] = { 0.25f, 0.5f, 0.25f };
            this.samplerate = samplerate;
            int minFreq = 50;
            double halfLifeInSamples = samplerate * halfLife;
            float attenPerSample = (float) Math.exp(Math.log(0.5) / halfLifeInSamples);
            cyclicBuff = new CyclicBuffer2((int) (samplerate / minFreq) + 1, wght, attenPerSample, halfString);
            rateConverter = new RateConverter(cyclicBuff, 1.0f, 128, 1);
            cnt = 0;
            thresh = 1.0f / 32000.0f;
        }

        public void close() throws IOException {
        }

        public void noteOff(int velocity) {
        }

        public void noteOn(MidiChannel channel, VoiceStatus voice, int noteNumber, int velocity) {
            this.channel = channel;
        }

        public int read(float[][] buffers, int offset, int len) throws IOException {
            int cntrl = channel.getController(23);
            if (cntrl != ctrlLast) {
                System.out.println(cntrl);
                ctrlLast = cntrl;
                cyclicBuff.setTap(cntrl);
            }
            rateConverter.read(buffers, offset, len);
            for (int i = offset; i < len; i++) {
                if (Math.abs(buffers[0][i]) < thresh) {
                    cnt++;
                } else {
                    cnt = 0;
                }
            }
            if (cnt > cntThresh) {
                return -1;
            }
            return len;
        }

        public void setPitch(float pitch) {
            if (pitch == this.pitch) {
                return;
            }
            this.pitch = pitch;
            freq = Math.exp((pitch - 6900) * (Math.log(2.0) / 1200.0)) * (440);
            float len = (float) (samplerate / freq);
            if (halfString) {
                len = len / 2.0f;
            }
            if (first) {
                first = false;
                cyclicBuff.setLength((int) len);
                srcPeriod = cyclicBuff.getPeriod();
                rateConverter.setFactor(srcPeriod / len);
                int pos = channel.getController(24);
                cyclicBuff.pluckAt(pos);
            } else {
                rateConverter.setFactor(srcPeriod / len);
            }
            cntThresh = (int) len;
        }
    }

    public Plucked2() {
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
        if (true) {
            performer.getConnectionBlocks().add(new ModelConnectionBlock(Double.NEGATIVE_INFINITY, new ModelDestination(new ModelIdentifier("eg", "attack", 0))));
            performer.getConnectionBlocks().add(new ModelConnectionBlock(1000.0, new ModelDestination(new ModelIdentifier("eg", "sustain", 0))));
            performer.getConnectionBlocks().add(new ModelConnectionBlock(1200.0, new ModelDestination(new ModelIdentifier("eg", "release", 0))));
        }
        SimpleInstrument ins = new SimpleInstrument();
        ins.setName("Simple Pluck2");
        ins.add(performer);
        ins.setPatch(new Patch(0, 0));
        addInstrument(ins);
    }
}
