package com.frinika.videosynth;

import com.frinika.project.FrinikaAudioSystem;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;
import javax.sound.midi.VoiceStatus;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Control;
import javax.sound.sampled.Control.Type;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;

/**
 *
 * @author Peter Johan Salomonsen
 */
public class FrinikaVideoSynth implements Synthesizer, Mixer {

    double microsecondposition = 0;

    @Resource
    Sequencer sequencer;

    ArrayList<Receiver> receivers = new ArrayList<Receiver>();

    MidiDevice.Info info = new FrinikaVideoSynthInfo();

    static FrinikaVideoSynthStageLauncher videoSynthStageLauncher;

    int width = 1280;

    int height = 720;

    ImageMovieViewerChannel[] channels = new ImageMovieViewerChannel[16];

    {
        channels[0] = new ImageMovieViewerChannel("file:///home/peter/Videos/tracker/out/", this);
        channels[1] = new ImageMovieViewerChannel("file:///home/peter/Videos/tracker/out-1/", this);
        channels[2] = new ImageMovieViewerChannel("file:///home/peter/Videos/tracker/out-2/", this);
        channels[3] = new ImageMovieViewerChannel("file:///home/peter/Videos/tracker/out-3/", this);
        channels[4] = new ImageMovieViewerChannel("file:///home/peter/Videos/tracker/out-12/", this);
        channels[5] = new ImageMovieViewerChannel("file:///home/peter/Videos/tracker/xeptologo/", this);
        channels[6] = new ImageMovieViewerChannel("file:///home/peter/Videos/tracker/out-5/", this);
    }

    String renderFolder = "/home/peter/Videos/tracker/rendered/";

    boolean open = false;

    @Override
    public int getMaxPolyphony() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getLatency() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MidiChannel[] getChannels() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public VoiceStatus[] getVoiceStatus() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isSoundbankSupported(Soundbank soundbank) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean loadInstrument(Instrument instrument) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void unloadInstrument(Instrument instrument) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean remapInstrument(Instrument from, Instrument to) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Soundbank getDefaultSoundbank() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Instrument[] getAvailableInstruments() {
        return new Instrument[] {};
    }

    @Override
    public Instrument[] getLoadedInstruments() {
        return new Instrument[] {};
    }

    @Override
    public boolean loadAllInstruments(Soundbank soundbank) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void unloadAllInstruments(Soundbank soundbank) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean loadInstruments(Soundbank soundbank, Patch[] patchList) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void unloadInstruments(Soundbank soundbank, Patch[] patchList) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public MidiDevice.Info getDeviceInfo() {
        return info;
    }

    @Override
    public void open() {
        Logger.getLogger(getClass().getName()).log(Level.INFO, ("Opening Frinika Videosynth"));
        videoSynthStageLauncher.launch(this);
        open = true;
        Logger.getLogger(getClass().getName()).log(Level.INFO, ("Frinika Videosynth opened"));
    }

    @Override
    public void close() {
        open = false;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public long getMicrosecondPosition() {
        return (long) microsecondposition;
    }

    @Override
    public int getMaxReceivers() {
        return -1;
    }

    @Override
    public int getMaxTransmitters() {
        return 0;
    }

    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        Receiver recv = new Receiver() {

            @Override
            public void send(MidiMessage message, long timeStamp) {
                ShortMessage shm = (ShortMessage) message;
                if (shm.getCommand() == ShortMessage.NOTE_ON && shm.getData2() > 0) {
                    channels[shm.getChannel()].noteOn(shm.getData1(), shm.getData2());
                } else if (shm.getCommand() == ShortMessage.NOTE_ON && shm.getData2() == 0) {
                    channels[shm.getChannel()].noteOff(shm.getData1());
                } else if (shm.getCommand() == ShortMessage.CONTROL_CHANGE) {
                    channels[shm.getChannel()].controlChange(shm.getData1(), shm.getData2());
                } else if (shm.getCommand() == ShortMessage.PITCH_BEND) {
                    channels[shm.getChannel()].setPitchBend((0xff & shm.getData1()) + ((0xff & shm.getData2()) << 7));
                }
            }

            @Override
            public void close() {
                receivers.remove(this);
            }
        };
        receivers.add(recv);
        return recv;
    }

    @Override
    public List<Receiver> getReceivers() {
        return receivers;
    }

    @Override
    public Transmitter getTransmitter() throws MidiUnavailableException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Transmitter> getTransmitters() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Mixer.Info getMixerInfo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Line.Info[] getSourceLineInfo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Line.Info[] getTargetLineInfo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Line.Info[] getSourceLineInfo(Line.Info info) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Line.Info[] getTargetLineInfo(Line.Info info) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isLineSupported(Line.Info info) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public BufferedImage getCurrentImage() {
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (ImageMovieViewerChannel channel : channels) {
            if (channel != null) {
                channel.updateImage(getMicrosecondPosition() / 1000);
                channel.paint(bi.createGraphics());
            }
        }
        return bi;
    }

    @Override
    public Line getLine(Line.Info info) throws LineUnavailableException {
        if (info.getLineClass() == TargetDataLine.class) {
            return new TargetDataLine() {

                AudioFormat format;

                double renderStartMicroSecondPosition = 0;

                int frameRate = 25;

                long lastRenderFrameNo = -1;

                @Override
                public void open(AudioFormat format, int bufferSize) throws LineUnavailableException {
                    Logger.getLogger(getClass().getName()).log(Level.INFO, format.toString());
                    this.format = format;
                }

                @Override
                public void open(AudioFormat format) throws LineUnavailableException {
                    Logger.getLogger(getClass().getName()).log(Level.INFO, format.toString());
                    this.format = format;
                }

                @Override
                public int read(byte[] b, int off, int len) {
                    double newMicroSecondPosition = microsecondposition + ((len / format.getFrameSize()) * 1000000.0 / format.getSampleRate());
                    if (!FrinikaAudioSystem.getAudioServer().isRealTime()) {
                        while (microsecondposition < newMicroSecondPosition) {
                            long frameNo = (long) (((microsecondposition - renderStartMicroSecondPosition) * frameRate) / 1000000) + 1;
                            if (frameNo != lastRenderFrameNo) {
                                try {
                                    ImageIO.write(getCurrentImage(), "jpg", new File(renderFolder + frameNo + ".jpg"));
                                } catch (IOException ex) {
                                    Logger.getLogger(FrinikaVideoSynth.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                lastRenderFrameNo = frameNo;
                            }
                            microsecondposition += (1000000.0 / frameRate);
                        }
                    } else {
                        renderStartMicroSecondPosition = microsecondposition;
                    }
                    microsecondposition = newMicroSecondPosition;
                    return len;
                }

                @Override
                public void drain() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public void flush() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public void start() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public void stop() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public boolean isRunning() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public boolean isActive() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public AudioFormat getFormat() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public int getBufferSize() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public int available() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public int getFramePosition() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public long getLongFramePosition() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public long getMicrosecondPosition() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public float getLevel() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public Info getLineInfo() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public void open() throws LineUnavailableException {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public void close() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public boolean isOpen() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public Control[] getControls() {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public boolean isControlSupported(Type control) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public Control getControl(Type control) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public void addLineListener(LineListener listener) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }

                @Override
                public void removeLineListener(LineListener listener) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            };
        } else return null;
    }

    @Override
    public int getMaxLines(Line.Info info) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Line[] getSourceLines() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Line[] getTargetLines() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void synchronize(Line[] lines, boolean maintainSync) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void unsynchronize(Line[] lines) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isSynchronizationSupported(Line[] lines, boolean maintainSync) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Line.Info getLineInfo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Control[] getControls() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isControlSupported(Type control) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Control getControl(Type control) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addLineListener(LineListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeLineListener(LineListener listener) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
