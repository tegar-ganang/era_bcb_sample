package com.frinika.softsynth.string;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.IOException;
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
import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

public class Bowed extends SimpleSoundbank {

    static final int POS_CNTRL = 16;

    static final int VEL_CNTRL = 14;

    static final int PRES_CNTRL = 15;

    static final int DAMP_CNTRL = 17;

    static boolean halfString = false;

    Valueizer posmap;

    Valueizer velmap;

    Valueizer pressmap;

    Valueizer dampmap;

    int nChannels = -11;

    BowStringFactory factory;

    Collection<Valueizer> valueizers;

    private MyOscillatorStream lastOsc;

    boolean plucked = false;

    public class MyOscillatorStream implements ModelOscillatorStream {

        float pitch = 60;

        float samplerate;

        BowString cyclicBuff;

        private RateConverter rateConverter;

        private double freq;

        private float thresh;

        private int cnt;

        private boolean first = true;

        private MidiChannel channel;

        private float srcPeriod;

        private int ctrlLastPos = -1;

        private int ctrlLastVel = -1;

        private int ctrlLastPressure = -1;

        private int ctrlLastAtten = -1;

        private int cntThresh = 50;

        private float noteVel = 0.0f;

        public MyOscillatorStream(float samplerate) {
            this.samplerate = samplerate;
            int minFreq = 10;
            cyclicBuff = factory.createString((int) ((samplerate / minFreq)) + 1);
            plucked = factory.isPlucked();
            rateConverter = new RateConverter(cyclicBuff, 1.0f, 128, nChannels);
            cnt = 0;
            thresh = 4.0f / 32000.0f;
        }

        public void close() throws IOException {
        }

        public void noteOff(int velocity) {
            noteVel = 0.0f;
            cyclicBuff.setBowVelocity((float) 0.0);
        }

        public void noteOn(MidiChannel channel, VoiceStatus voice, int noteNumber, int velocity) {
            this.channel = channel;
            noteVel = velocity;
        }

        public int read(float[][] buffers, int offset, int len) throws IOException {
            setControls();
            rateConverter.read(buffers, offset, len);
            for (int i = offset; i < len; i++) {
                if (Math.abs(buffers[0][i]) < thresh) {
                    cnt++;
                } else {
                    cnt = 0;
                }
            }
            if (noteVel == 0.0f && cnt > cntThresh) {
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
                setControls();
                if (plucked) cyclicBuff.pluckAt();
            } else {
                rateConverter.setFactor(srcPeriod / len);
            }
            cntThresh = (int) len;
        }

        private void setControls() {
            int cntrl = channel.getController(16);
            if (cntrl != ctrlLastPos) {
                ctrlLastPos = cntrl;
                cyclicBuff.setBowPosition(posmap.map(cntrl / 128.0f));
            }
            cntrl = channel.getController(14);
            if (cntrl != ctrlLastVel) {
                ctrlLastVel = cntrl;
                cyclicBuff.setBowVelocity(velmap.map(cntrl / 128.0f));
            }
            if (!plucked) cntrl = channel.getController(15); else cntrl = 0;
            if (cntrl != ctrlLastPressure) {
                ctrlLastPressure = cntrl;
                cyclicBuff.setBowPressure(pressmap.map(cntrl / 128.0f));
            }
            cntrl = channel.getController(17);
            if (cntrl != ctrlLastAtten) {
                ctrlLastAtten = cntrl;
                cyclicBuff.setBowAttenuation(dampmap.map(cntrl / 128.0f));
            }
        }
    }

    public Bowed(final BowStringFactory factory, Patch patch) {
        this.factory = factory;
        nChannels = factory.chans();
        valueizers = new Vector<Valueizer>();
        String name = "Bowed";
        valueizers.add(velmap = new Valueizer("Velocity", name));
        valueizers.add(pressmap = new Valueizer("Pressure", name));
        valueizers.add(posmap = new Valueizer("Position ", name));
        valueizers.add(dampmap = new Valueizer("Damping", name));
        ModelOscillator osc = new ModelOscillator() {

            public float getAttenuation() {
                return 0;
            }

            public int getChannels() {
                return nChannels;
            }

            public ModelOscillatorStream open(float samplerate) {
                return lastOsc = new MyOscillatorStream(samplerate);
            }
        };
        ModelPerformer performer = new ModelPerformer();
        performer.getOscillators().add(osc);
        performer.getConnectionBlocks().add(new ModelConnectionBlock(Double.NEGATIVE_INFINITY, new ModelDestination(new ModelIdentifier("eg", "attack", 0))));
        performer.getConnectionBlocks().add(new ModelConnectionBlock(0.0, new ModelDestination(new ModelIdentifier("eg", "hold", 0))));
        performer.getConnectionBlocks().add(new ModelConnectionBlock(0.0, new ModelDestination(new ModelIdentifier("eg", "decay", 0))));
        performer.getConnectionBlocks().add(new ModelConnectionBlock(1000.0, new ModelDestination(new ModelIdentifier("eg", "sustain", 0))));
        performer.getConnectionBlocks().add(new ModelConnectionBlock(1200.0, new ModelDestination(new ModelIdentifier("eg", "release", 0))));
        SimpleInstrument ins = new BowedInstrument();
        ins.setName(factory.getPatchName());
        ins.add(performer);
        ins.setPatch(patch);
        addInstrument(ins);
    }

    class BowedInstrument extends SimpleInstrument implements InstrumentGuiIF {

        float buff[];

        private int maxLen;

        public Iterable<Valueizer> getValueizers() {
            return valueizers;
        }

        public void paint(Graphics2D g, Rectangle rect) {
            if (lastOsc == null) return;
            BowString str = lastOsc.cyclicBuff;
            maxLen = str.getMaxLengthInSamples();
            if (buff == null || buff.length < maxLen) {
                buff = new float[maxLen];
            } else {
                Arrays.fill(buff, 0.0f);
            }
            str.getStringState(buff);
            int midY = rect.y + rect.height / 2;
            g.setColor(Color.WHITE);
            float yScale = rect.height / 2;
            float x1 = 0;
            int y1 = (int) (midY + yScale * buff[0]);
            float dx = 10 * rect.width / buff.length;
            for (int i = 1; i < buff.length; i++) {
                float x2 = x1 + dx;
                int y2 = (int) (midY + yScale * buff[i]);
                g.drawLine((int) x1, y1, (int) x2, y2);
                x1 = x2;
                y1 = y2;
            }
        }
    }
}
