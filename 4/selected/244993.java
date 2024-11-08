package com.frinika.fokaosynth.sine;

import com.sun.media.sound.ModelConnectionBlock;
import com.sun.media.sound.ModelDestination;
import com.sun.media.sound.ModelIdentifier;
import com.sun.media.sound.ModelOscillator;
import com.sun.media.sound.ModelOscillatorStream;
import com.sun.media.sound.ModelPerformer;
import javax.sound.midi.Patch;
import com.sun.media.sound.SimpleInstrument;
import com.sun.media.sound.SimpleSoundbank;
import java.io.IOException;
import java.io.InputStream;
import java.lang.String;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.VoiceStatus;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Testsine extends SimpleSoundbank {

    public enum WAVETYPE {

        Sinus, WhiteNoise, Rect, Sawtooth, BackSawtooth
    }

    ;

    public class Wavedata {

        public WAVETYPE sType;

        public float FreqFactor;

        public float VolumeFactor;

        float baseFreq;

        float lastPos;

        float loopLen;

        public Wavedata() {
            sType = WAVETYPE.Sinus;
            FreqFactor = 1.0f;
            VolumeFactor = 1.0f;
        }

        public Wavedata(Wavedata pIn) {
            sType = pIn.sType;
            FreqFactor = pIn.FreqFactor;
            VolumeFactor = pIn.VolumeFactor;
        }
    }

    public class MyOscillatorStream implements ModelOscillatorStream {

        float second_val;

        List<Wavedata> theData;

        float samplerate = -1.0f;

        float pitch = -1f;

        float volume01 = 0;

        float timepixel;

        public MyOscillatorStream(float samplerateIn, MyModelOscillator Ossi) {
            samplerate = samplerateIn;
            second_val = (float) java.lang.StrictMath.pow(2.0, 1.0 / 12.0);
            timepixel = 1 / samplerate;
            Object O[] = Ossi.theData.toArray();
            theData = new ArrayList<Wavedata>();
            for (Object Ob : O) {
                theData.add(new Wavedata((Wavedata) Ob));
            }
        }

        public void close() throws IOException {
        }

        public void noteOff(int velocity) {
        }

        public void noteOn(MidiChannel channel, VoiceStatus voice, int noteNumber, int velocity) {
            volume01 = (float) velocity / 127.0f;
        }

        private Random rand = new Random();

        public int read(float[][] buffers, int offset, int len) throws IOException {
            long time1 = System.nanoTime();
            for (int i = offset; i < offset + len; i++) {
                buffers[0][i] = 0.0f;
                for (Wavedata sDat : theData) {
                    if (sDat.sType == WAVETYPE.Sinus) {
                        buffers[0][i] += (float) Math.sin(sDat.lastPos * sDat.baseFreq * 2 * Math.PI) * sDat.VolumeFactor;
                    } else if (sDat.sType == WAVETYPE.WhiteNoise) {
                        buffers[0][i] += sDat.VolumeFactor * (0.5f - rand.nextFloat());
                    } else if (sDat.sType == WAVETYPE.Rect) {
                        if (sDat.lastPos < sDat.loopLen / 2.0) {
                            buffers[0][i] += sDat.VolumeFactor;
                        } else {
                            buffers[0][i] -= sDat.VolumeFactor;
                        }
                    } else if (sDat.sType == WAVETYPE.Sawtooth) {
                        buffers[0][i] += sDat.VolumeFactor * (1 - (2. * (sDat.loopLen - sDat.lastPos) / sDat.loopLen));
                    } else if (sDat.sType == WAVETYPE.BackSawtooth) {
                        buffers[0][i] += sDat.VolumeFactor * ((2. * (sDat.loopLen - sDat.lastPos) / sDat.loopLen) - 1);
                    }
                    sDat.lastPos += timepixel;
                    if (sDat.lastPos > sDat.loopLen) sDat.lastPos -= sDat.loopLen;
                }
                buffers[0][i] *= volume01;
            }
            long time2 = System.nanoTime();
            return len;
        }

        float baseFreq;

        float tt;

        public void setPitch(float pitchIn) {
            if (pitch == pitchIn) return;
            pitch = pitchIn;
            tt = 0;
            baseFreq = (float) (440.0 * java.lang.StrictMath.pow(2.0, ((pitch / 100.0) - 69) / 12.0));
            for (Wavedata sDat : theData) {
                sDat.baseFreq = baseFreq * sDat.FreqFactor;
                sDat.loopLen = 1.0f / sDat.baseFreq;
                sDat.lastPos = 0;
            }
        }
    }

    public class MyModelOscillator implements ModelOscillator {

        public List<Wavedata> theData = new ArrayList<Wavedata>();

        public float getAttenuation() {
            return 0;
        }

        public int getChannels() {
            return 1;
        }

        public ModelOscillatorStream open(float samplerate) {
            return new MyOscillatorStream(samplerate, this);
        }
    }

    public Testsine() throws ParserConfigurationException, SAXException, IOException {
        Runtime sR = Runtime.getRuntime();
        InputStream streamF = getClass().getResourceAsStream("FokaoSynthies.xml");
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        try {
            Document doc = db.parse(streamF);
            System.out.println(streamF.toString() + " Found");
            Element documentElement = doc.getDocumentElement();
            NodeList nodeLst = documentElement.getElementsByTagName("Instruments");
            if (nodeLst.getLength() == 1) {
                Element Instruments = (Element) nodeLst.item(0);
                NodeList InstrumentsList = Instruments.getElementsByTagName("Instrument");
                int iPatch = 0;
                for (int iInst = 0; iInst < InstrumentsList.getLength(); iInst++) {
                    Element Instrument = (Element) InstrumentsList.item((iInst));
                    iPatch = iInst;
                    String Name = "<noname>";
                    if (Instrument.getElementsByTagName("Name").getLength() == 1) {
                        Name = ((Element) Instrument.getElementsByTagName("Name").item(0)).getTextContent();
                    }
                    if (Instrument.getElementsByTagName("Patch").getLength() == 1) {
                        iPatch = Integer.parseInt(((Element) Instrument.getElementsByTagName("Patch").item(0)).getTextContent());
                    }
                    SimpleInstrument ins = new SimpleInstrument();
                    MyModelOscillator osc = new MyModelOscillator();
                    if (Instrument.getElementsByTagName("Generators").getLength() == 1) {
                        Element GeneratorsList = (Element) Instrument.getElementsByTagName("Generators").item(0);
                        if (GeneratorsList.getElementsByTagName("Generator").getLength() > 0) {
                            NodeList GeneratorList = GeneratorsList.getElementsByTagName("Generator");
                            for (int iGen = 0; iGen < GeneratorList.getLength(); iGen++) {
                                Element Generator = (Element) GeneratorList.item((iGen));
                                Wavedata sData = new Wavedata();
                                String sGeneratorType = Generator.getAttribute("Type").toUpperCase();
                                if (sGeneratorType.equals("WHITENOISE")) {
                                    sData.sType = WAVETYPE.WhiteNoise;
                                }
                                if (sGeneratorType.equals("RECT")) {
                                    sData.sType = WAVETYPE.Rect;
                                }
                                if (sGeneratorType.equals("SAWTOOTH")) {
                                    sData.sType = WAVETYPE.Sawtooth;
                                }
                                if (sGeneratorType.equals("BACKSAWTOOTH")) {
                                    sData.sType = WAVETYPE.BackSawtooth;
                                }
                                if (Generator.getElementsByTagName("GeneratorFreqFactor").getLength() == 1) {
                                    Element GeneratorFreqFactor = ((Element) Generator.getElementsByTagName("GeneratorFreqFactor").item(0));
                                    sData.FreqFactor = Float.parseFloat(GeneratorFreqFactor.getTextContent());
                                }
                                if (Generator.getElementsByTagName("GeneratorVolFactor").getLength() == 1) {
                                    Element GeneratorVolFactor = ((Element) Generator.getElementsByTagName("GeneratorVolFactor").item(0));
                                    sData.VolumeFactor = Float.parseFloat(GeneratorVolFactor.getTextContent());
                                }
                                osc.theData.add(sData);
                            }
                        }
                    }
                    if (osc.theData.size() == 0) {
                        Wavedata sData = new Wavedata();
                        osc.theData.add(sData);
                    }
                    ModelPerformer performer = new ModelPerformer();
                    performer.getOscillators().add(osc);
                    if (Instrument.getElementsByTagName("Envelope").getLength() == 1) {
                        Element Envelope = ((Element) Instrument.getElementsByTagName("Envelope").item(0));
                        double dVal = 0.0;
                        if (Envelope.getElementsByTagName("Delay").getLength() == 1) {
                            Element Attack = ((Element) Envelope.getElementsByTagName("Delay").item(0));
                            dVal = Double.parseDouble(Attack.getTextContent());
                            if (dVal <= -10000) dVal = Double.NEGATIVE_INFINITY;
                        } else {
                            dVal = Double.NEGATIVE_INFINITY;
                        }
                        performer.getConnectionBlocks().add(new ModelConnectionBlock(dVal, new ModelDestination(new ModelIdentifier("eg", "delay", 0))));
                        if (Envelope.getElementsByTagName("Attack").getLength() == 1) {
                            Element Attack = ((Element) Envelope.getElementsByTagName("Attack").item(0));
                            dVal = Double.parseDouble(Attack.getTextContent());
                            if (dVal <= -10000) dVal = Double.NEGATIVE_INFINITY;
                        } else {
                            dVal = Double.NEGATIVE_INFINITY;
                        }
                        performer.getConnectionBlocks().add(new ModelConnectionBlock(dVal, new ModelDestination(new ModelIdentifier("eg", "attack", 0))));
                        if (Envelope.getElementsByTagName("Hold").getLength() == 1) {
                            Element Attack = ((Element) Envelope.getElementsByTagName("Hold").item(0));
                            dVal = Double.parseDouble(Attack.getTextContent());
                        } else {
                            dVal = 0.0;
                        }
                        performer.getConnectionBlocks().add(new ModelConnectionBlock(dVal, new ModelDestination(new ModelIdentifier("eg", "hold", 0))));
                        if (Envelope.getElementsByTagName("Decay").getLength() == 1) {
                            Element Attack = ((Element) Envelope.getElementsByTagName("Decay").item(0));
                            dVal = Double.parseDouble(Attack.getTextContent());
                        } else {
                            dVal = 0.0;
                        }
                        performer.getConnectionBlocks().add(new ModelConnectionBlock(dVal, new ModelDestination(new ModelIdentifier("eg", "decay", 0))));
                        if (Envelope.getElementsByTagName("Sustain").getLength() == 1) {
                            Element Attack = ((Element) Envelope.getElementsByTagName("Sustain").item(0));
                            dVal = Double.parseDouble(Attack.getTextContent());
                        } else {
                            dVal = 0.0;
                        }
                        performer.getConnectionBlocks().add(new ModelConnectionBlock(dVal, new ModelDestination(new ModelIdentifier("eg", "sustain", 0))));
                        if (Envelope.getElementsByTagName("Release").getLength() == 1) {
                            Element Attack = ((Element) Envelope.getElementsByTagName("Release").item(0));
                            dVal = Double.parseDouble(Attack.getTextContent());
                        } else {
                            dVal = 0.0;
                        }
                        performer.getConnectionBlocks().add(new ModelConnectionBlock(dVal, new ModelDestination(new ModelIdentifier("eg", "release", 0))));
                        if (Envelope.getElementsByTagName("Shutdown").getLength() == 1) {
                            Element Attack = ((Element) Envelope.getElementsByTagName("Shutdown").item(0));
                            dVal = Double.parseDouble(Attack.getTextContent());
                        } else {
                            dVal = 0.0;
                        }
                        performer.getConnectionBlocks().add(new ModelConnectionBlock(dVal, new ModelDestination(new ModelIdentifier("eg", "shutdown", 0))));
                    }
                    System.out.println("Instrument[" + iPatch + "]: " + Name);
                    ins.setName(Name);
                    ins.add(performer);
                    ins.setPatch(new Patch(0, iPatch));
                    addInstrument(ins);
                }
            } else {
                System.out.println("misformed XML or No Instruments found");
            }
        } catch (java.io.FileNotFoundException sFN) {
            JOptionPane.showMessageDialog(null, sFN.getLocalizedMessage());
            SimpleInstrument ins = new SimpleInstrument();
            ModelOscillator osc = new ModelOscillator() {

                public float getAttenuation() {
                    return 0;
                }

                public int getChannels() {
                    return 1;
                }

                public ModelOscillatorStream open(float samplerate) {
                    return new MyOscillatorStream(samplerate, null);
                }
            };
            ModelPerformer performer = new ModelPerformer();
            performer.getOscillators().add(osc);
            performer.getConnectionBlocks().add(new ModelConnectionBlock(Double.NEGATIVE_INFINITY, new ModelDestination(new ModelIdentifier("eg", "attack", 0))));
            performer.getConnectionBlocks().add(new ModelConnectionBlock(1000.0, new ModelDestination(new ModelIdentifier("eg", "sustain", 0))));
            performer.getConnectionBlocks().add(new ModelConnectionBlock(1200.0, new ModelDestination(new ModelIdentifier("eg", "release", 0))));
            System.out.println("Instrument[0]: Fokao Test Sine");
            ins.setName("Fokao Test Sine");
            ins.add(performer);
            ins.setPatch(new Patch(0, 0));
            addInstrument(ins);
        }
    }
}
