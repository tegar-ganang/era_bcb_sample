package eu.irreality.age;

import java.io.*;
import java.net.URL;
import java.nio.ByteOrder;
import java.util.*;
import java.util.logging.Level;
import micromod.*;
import micromod.resamplers.*;
import micromod.output.*;
import micromod.output.converters.*;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.sampled.*;
import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayer;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerException;
import javazoom.jlgui.basicplayer.BasicPlayerListener;
import eu.irreality.age.debug.Debug;
import eu.irreality.age.filemanagement.URLUtils;

public class AGESoundClient implements SoundClient {

    private javax.sound.midi.Sequencer seqr;

    private javax.sound.midi.Sequence curseq;

    private java.util.Hashtable midiPreloaded = new java.util.Hashtable();

    private boolean on = true;

    public boolean isOn() {
        return on;
    }

    public void activate() {
        on = true;
    }

    public void deactivate() {
        stopAllSound();
        on = false;
    }

    private javax.sound.midi.Sequence getPreloadedSequence(URL u) {
        return (javax.sound.midi.Sequence) midiPreloaded.get(u);
    }

    public void midiInit() throws javax.sound.midi.MidiUnavailableException {
        midiClose();
        if (seqr == null) {
            seqr = javax.sound.midi.MidiSystem.getSequencer();
            seqr.open();
        }
    }

    public void midiPreload(URL midfile) throws javax.sound.midi.InvalidMidiDataException, java.io.IOException {
        javax.sound.midi.Sequence seq = javax.sound.midi.MidiSystem.getSequence(midfile);
        midiPreloaded.put(midfile, seq);
    }

    public void midiUnload(URL midfile) throws java.io.IOException {
        midiPreloaded.remove(midfile);
    }

    public void midiUnload(String s) throws java.io.IOException {
        midiUnload(URLUtils.stringToURL(s));
    }

    public void midiStart(URL midfile) throws javax.sound.midi.InvalidMidiDataException, java.io.IOException, MidiUnavailableException {
        if (!isOn()) return;
        curseq = getPreloadedSequence(midfile);
        if (curseq == null) curseq = javax.sound.midi.MidiSystem.getSequence(midfile);
        if (seqr.isRunning()) seqr.stop();
        seqr.setLoopCount(0);
        seqr.setSequence(curseq);
        seqr.start();
    }

    public void midiStart(String f) throws javax.sound.midi.InvalidMidiDataException, java.io.IOException, MidiUnavailableException {
        if (!isOn()) return;
        midiStart(URLUtils.stringToURL(f));
    }

    public void midiPreload(String f) throws javax.sound.midi.InvalidMidiDataException, java.io.IOException {
        midiPreload(URLUtils.stringToURL(f));
    }

    public void midiOpen(URL midfile) throws javax.sound.midi.InvalidMidiDataException, java.io.IOException {
        curseq = getPreloadedSequence(midfile);
        if (curseq == null) curseq = javax.sound.midi.MidiSystem.getSequence(midfile);
    }

    public void midiOpen(String f) throws javax.sound.midi.InvalidMidiDataException, java.io.IOException {
        midiOpen(URLUtils.stringToURL(f));
    }

    public void midiLoop(int loopCount) throws javax.sound.midi.InvalidMidiDataException {
        if (!isOn()) return;
        if (seqr.isRunning()) seqr.stop();
        seqr.setSequence(curseq);
        seqr.setLoopCount(loopCount);
        seqr.start();
    }

    public void midiLoop() throws javax.sound.midi.InvalidMidiDataException {
        if (!isOn()) return;
        midiLoop(javax.sound.midi.Sequencer.LOOP_CONTINUOUSLY);
    }

    public void midiStart() throws javax.sound.midi.InvalidMidiDataException {
        if (!isOn()) return;
        if (seqr.isRunning()) seqr.stop();
        seqr.setSequence(curseq);
        seqr.setLoopCount(0);
        seqr.start();
    }

    public void midiStop() {
        seqr.stop();
    }

    public void midiClose() {
        curseq = null;
        if (seqr != null && seqr.isOpen()) seqr.close();
        seqr = null;
    }

    javax.sound.midi.Synthesizer synthesizer;

    javax.sound.midi.Synthesizer synthDevice;

    private static final int CHANGE_VOLUME = 7;

    public void midiResetGain(double gain) {
        if (gain < 0.0d) gain = 0.0d;
        if (gain > 1.0d) gain = 1.0d;
        int midiVolume = (int) (gain * 127.0d);
        System.err.println("Vol " + midiVolume);
        if (synthesizer != null) {
            javax.sound.midi.MidiChannel[] channels = synthesizer.getChannels();
            System.err.println("Channels: " + channels.length);
            for (int c = 0; channels != null && c < channels.length; c++) {
                System.err.println("cc " + midiVolume);
                channels[c].controlChange(CHANGE_VOLUME, midiVolume);
            }
        } else if (synthDevice != null) {
            try {
                ShortMessage volumeMessage = new ShortMessage();
                for (int i = 0; i < 16; i++) {
                    volumeMessage.setMessage(ShortMessage.CONTROL_CHANGE, i, CHANGE_VOLUME, midiVolume);
                    synthDevice.getReceiver().send(volumeMessage, -1);
                }
            } catch (Exception e) {
                System.err.println("Error resetting gain on MIDI device");
                e.printStackTrace();
            }
        } else if (seqr != null && seqr instanceof Synthesizer) {
            synthesizer = (javax.sound.midi.Synthesizer) seqr;
            javax.sound.midi.MidiChannel[] channels = synthesizer.getChannels();
            for (int c = 0; channels != null && c < channels.length; c++) {
                channels[c].controlChange(CHANGE_VOLUME, midiVolume);
            }
        } else {
            try {
                Receiver receiver = MidiSystem.getReceiver();
                ShortMessage volumeMessage = new ShortMessage();
                for (int c = 0; c < 16; c++) {
                    volumeMessage.setMessage(ShortMessage.CONTROL_CHANGE, c, CHANGE_VOLUME, midiVolume);
                    receiver.send(volumeMessage, -1);
                }
            } catch (Exception e) {
                System.err.println("Error resetting gain on MIDI device");
                e.printStackTrace();
            }
        }
    }

    public void midiFadeOut() {
        double volume = 0.6;
        for (; ; ) {
            if (((volume - 0.05) < 0)) {
                break;
            }
            midiResetGain(volume);
            System.err.println("Gain = " + volume);
            try {
                Thread.sleep(150);
            } catch (Exception exception) {
            }
            volume -= 0.025;
        }
        if (synthesizer != null) {
            synthesizer.close();
            synthesizer = null;
        }
        if (seqr != null) {
            if (seqr.isOpen()) {
                seqr.stop();
            }
            seqr.close();
        }
    }

    private java.util.Hashtable audioPreloaded = new java.util.Hashtable();

    public void audioPreload(URL u) throws javax.sound.sampled.UnsupportedAudioFileException, javax.sound.sampled.LineUnavailableException, java.io.IOException {
        javax.sound.sampled.AudioInputStream aii = javax.sound.sampled.AudioSystem.getAudioInputStream(u);
        javax.sound.sampled.AudioFormat af = aii.getFormat();
        javax.sound.sampled.AudioFormat finalFormat = af;
        javax.sound.sampled.AudioInputStream finalStream = aii;
        if (u.getPath().toLowerCase().endsWith(".ogg") || u.getPath().toLowerCase().endsWith(".mp3")) {
            AudioFormat baseFormat = aii.getFormat();
            finalFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
            finalStream = AudioSystem.getAudioInputStream(finalFormat, aii);
        }
        javax.sound.sampled.Clip cl = (javax.sound.sampled.Clip) javax.sound.sampled.AudioSystem.getLine(new javax.sound.sampled.DataLine.Info(javax.sound.sampled.Clip.class, finalFormat));
        cl.open(finalStream);
        audioPreloaded.put(u, cl);
    }

    public void audioUnload(java.io.File afile) throws java.io.IOException {
        audioPreloaded.remove(afile.getCanonicalPath());
    }

    public void audioUnload(String s) throws java.io.IOException {
        audioUnload(new File(s));
    }

    public void audioPreload(String s) throws javax.sound.sampled.UnsupportedAudioFileException, javax.sound.sampled.LineUnavailableException, java.io.IOException {
        audioPreload(URLUtils.stringToURL(s));
    }

    private javax.sound.sampled.Clip getPreloadedClip(URL u) {
        return (javax.sound.sampled.Clip) audioPreloaded.get(u);
    }

    public void audioStartPreloaded(URL u) throws javax.sound.sampled.UnsupportedAudioFileException, javax.sound.sampled.LineUnavailableException, java.io.IOException {
        if (!isOn()) return;
        audioStartPreloaded(u, 0);
    }

    public void audioStartPreloaded(URL u, int loopTimes) throws javax.sound.sampled.UnsupportedAudioFileException, javax.sound.sampled.LineUnavailableException, java.io.IOException {
        if (!isOn()) return;
        javax.sound.sampled.Clip cl = getPreloadedClip(u);
        if (cl == null) audioPreload(u);
        cl = getPreloadedClip(u);
        cl.setFramePosition(0);
        cl.loop(loopTimes);
    }

    public void audioStopPreloaded(URL u) {
        javax.sound.sampled.Clip cl = getPreloadedClip(u);
        if (cl == null) return;
        cl.stop();
    }

    private Map basicPlayers = Collections.synchronizedMap(new HashMap());

    public void audioStartUnpreloaded(final URL u) throws IOException {
        if (!isOn()) return;
        audioStartUnpreloaded(u, 0);
    }

    public void audioStartUnpreloaded(final URL u, final int loopTimes) throws IOException {
        if (!isOn()) return;
        try {
            java.util.logging.Logger log = java.util.logging.Logger.getLogger("javazoom.jlgui.basicplayer.BasicPlayer");
            log.setLevel(Level.SEVERE);
        } catch (SecurityException se) {
            System.err.println("Restricted security environment, will not take logs of audio issues.");
        }
        final BasicPlayer bp = new BasicPlayer();
        try {
            InputStream theStream = u.openStream();
            if (theStream.markSupported()) {
                bp.open(theStream);
            } else {
                BufferedInputStream bib = new BufferedInputStream(theStream);
                bp.open(bib);
            }
        } catch (BasicPlayerException bpe) {
            bpe.printStackTrace();
            throw new IOException(bpe);
        }
        basicPlayers.put(u, bp);
        bp.addBasicPlayerListener(new BasicPlayerListener() {

            private int loopCount = loopTimes;

            public void opened(Object arg0, Map arg1) {
            }

            public void progress(int arg0, long arg1, byte[] arg2, Map arg3) {
            }

            public void setController(BasicController arg0) {
            }

            public void stateUpdated(BasicPlayerEvent arg0) {
                if (arg0.getCode() == BasicPlayerEvent.EOM) {
                    if (loopCount < 0) {
                        try {
                            if (!isOn()) return;
                            bp.stop();
                            double theGain = getCurrentGain(u);
                            bp.open(u.openStream());
                            bp.play();
                            bp.setGain(theGain);
                        } catch (BasicPlayerException bpe) {
                            bpe.printStackTrace();
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    } else if (loopCount > 0) {
                        loopCount--;
                        try {
                            if (!isOn()) return;
                            bp.stop();
                            double theGain = getCurrentGain(u);
                            bp.open(u.openStream());
                            bp.play();
                            bp.setGain(theGain);
                        } catch (BasicPlayerException bpe) {
                            bpe.printStackTrace();
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    } else {
                        basicPlayers.remove(u);
                        resetCurrentGain(u);
                    }
                }
            }
        });
        try {
            bp.play();
        } catch (BasicPlayerException bpe) {
            bpe.printStackTrace();
            throw new IOException(bpe);
        }
    }

    public void audioStopUnpreloaded(URL u) {
        BasicPlayer bp = (BasicPlayer) basicPlayers.get(u);
        if (bp != null) {
            try {
                bp.stop();
            } catch (BasicPlayerException bpe) {
                bpe.printStackTrace();
            }
            basicPlayers.remove(u);
        }
    }

    public void audioStart(URL u, int loopTimes) throws javax.sound.sampled.UnsupportedAudioFileException, javax.sound.sampled.LineUnavailableException, java.io.IOException {
        javax.sound.sampled.Clip cl = getPreloadedClip(u);
        if (cl == null) audioStartUnpreloaded(u, loopTimes); else audioStartPreloaded(u, loopTimes);
    }

    public void audioStart(URL u) throws javax.sound.sampled.UnsupportedAudioFileException, javax.sound.sampled.LineUnavailableException, java.io.IOException {
        audioStart(u, 0);
    }

    public void audioStart(String s) throws UnsupportedAudioFileException, LineUnavailableException, IOException {
        audioStart(s, 0);
    }

    public void audioStart(String s, int loopTimes) throws javax.sound.sampled.UnsupportedAudioFileException, javax.sound.sampled.LineUnavailableException, java.io.IOException {
        audioStart(URLUtils.stringToURL(s), loopTimes);
    }

    public void audioStop(String s) {
        audioStopUnpreloaded(URLUtils.stringToURL(s));
    }

    public void audioFadeIn(String s, int loopTimes, double seconds, double delay) throws UnsupportedAudioFileException, LineUnavailableException, IOException {
        audioFadeIn(URLUtils.stringToURL(s), loopTimes, seconds, delay);
    }

    public void audioFadeOut(String s, double seconds) {
        audioFadeOut(URLUtils.stringToURL(s), seconds);
    }

    /**
	 * input: time (from 0.0 to 1.0)
	 * output: gain (from 1.0 to 0.0)
	 * contract: should output 0.0 or less for 1.0 or more
	 * @param time
	 */
    private double fadeOutFunction(double time) {
        return expFade(time);
    }

    private double fadeInFunction(double time) {
        return 1 - expFade(time);
    }

    private double cosineFade(double time) {
        double angle = time * Math.PI / 2;
        if (time >= 1.0) return 0.0; else return Math.cos(angle);
    }

    private double expFade(double time) {
        if (time >= 1.0) return 0.0; else return 1.0 / ((6 * time + 1.0) * (6 * time + 1.0));
    }

    public void audioFadeOut(final URL u, final double seconds) {
        final BasicPlayer bp = (BasicPlayer) basicPlayers.get(u);
        if (bp != null) {
            Thread thr = new Thread() {

                public void run() {
                    double gain = 1.0;
                    double iters = 100.0;
                    double itersDone = 0.0;
                    int sleepTime = (int) (seconds * 1000.0 / iters);
                    while (gain > 0.0) {
                        itersDone += 1.0;
                        gain = fadeOutFunction(itersDone / (iters - 1));
                        try {
                            bp.setGain(gain);
                        } catch (BasicPlayerException e1) {
                            e1.printStackTrace();
                        }
                        try {
                            sleep(sleepTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    audioStopUnpreloaded(u);
                }
            };
            thr.start();
        }
    }

    private Hashtable gainsForURLs = new Hashtable();

    /**
	 * Because BasicPlayer API won't give us the gain in a reliable format (if we do setGain, then getGainValue returns different units!)
	 * @param u
	 */
    private void setCurrentGain(final URL u, final double gain) {
        gainsForURLs.put(u, new Double(gain));
    }

    /**
	 * Because BasicPlayer API won't give us the gain in a reliable format (if we do setGain, then getGainValue returns different units!)
	 */
    private double getCurrentGain(final URL u) {
        Double d = (Double) gainsForURLs.get(u);
        if (d == null) return 1.0; else return d.doubleValue();
    }

    /**
	 * Because BasicPlayer API won't give us the gain in a reliable format (if we do setGain, then getGainValue returns different units!)
	 */
    private void resetCurrentGain(final URL u) {
        gainsForURLs.remove(u);
    }

    public void audioSetGain(final URL u, final double gain) {
        final BasicPlayer bp = (BasicPlayer) basicPlayers.get(u);
        try {
            setCurrentGain(u, gain);
            bp.setGain(gain);
        } catch (BasicPlayerException e1) {
            e1.printStackTrace();
        }
    }

    public void audioSetGain(String s, double gain) {
        audioSetGain(URLUtils.stringToURL(s), gain);
    }

    public void audioFadeIn(final URL u, final int loopTimes, final double seconds, final double delay) throws UnsupportedAudioFileException, LineUnavailableException, IOException {
        Thread thr = new Thread() {

            public void run() {
                try {
                    sleep((int) delay * 1000);
                } catch (InterruptedException e2) {
                    e2.printStackTrace();
                }
                try {
                    audioStart(u, loopTimes);
                } catch (UnsupportedAudioFileException e2) {
                    e2.printStackTrace();
                } catch (LineUnavailableException e2) {
                    e2.printStackTrace();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                final BasicPlayer bp = (BasicPlayer) basicPlayers.get(u);
                double gain = 0.0;
                double iters = 100.0;
                double itersDone = 0.0;
                int sleepTime = (int) (seconds * 1000.0 / iters);
                while (gain < 1.0) {
                    itersDone += 1.0;
                    gain = fadeInFunction(itersDone / (iters - 1));
                    try {
                        bp.setGain(gain);
                    } catch (BasicPlayerException e1) {
                        e1.printStackTrace();
                    }
                    try {
                        sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        thr.start();
    }

    PlayerThread pt;

    public void playMOD(File f, int iRepeat) throws Exception {
        if (!isOn()) return;
        playMOD(new DataInputStream(new FileInputStream(f)), iRepeat);
    }

    public void playMOD(URL u, int iRepeat) throws Exception {
        if (!isOn()) return;
        playMOD(new DataInputStream(u.openStream()), iRepeat);
    }

    private void playMOD(DataInput theInput, int iRepeat) throws Exception {
        if (!isOn()) return;
        MODThread mt;
        JavaSoundOutputDevice out = new JavaSoundOutputDevice(new SS16LEAudioFormatConverter(), 44100, 1000);
        Module module = ModuleLoader.read(theInput);
        MicroMod microMod = new MicroMod(module, out, new LinearResampler());
        mt = new MODThread(microMod, out, iRepeat);
        pt = mt;
        pt.setVolume(0x10000);
        mt.start();
        return;
    }

    public void playMOD(String s, int iRepeat) throws Exception {
        if (!isOn()) return;
        playMOD(new File(s), iRepeat);
    }

    public void stopMOD() throws Exception {
        if (pt != null) pt.stopPlaying();
    }

    class MODThread extends Thread implements PlayerThread {

        int soundId;

        boolean running;

        boolean stopped;

        JavaSoundOutputDevice out;

        MicroMod mm;

        int iRepeat;

        MODThread(MicroMod mm, JavaSoundOutputDevice out, int iRepeat) {
            this.mm = mm;
            this.out = out;
            this.iRepeat = iRepeat;
            running = false;
            stopped = false;
        }

        public synchronized void setVolume(int vol) {
            Line l = out.getLine();
            FloatControl ctl = (FloatControl) l.getControl(FloatControl.Type.MASTER_GAIN);
            double gain = (double) vol / (double) 0x10000;
            float dB = (float) (Math.log(gain) / Math.log(10.0) * 20);
            if (ctl != null) ctl.setValue(dB);
        }

        public synchronized void stopPlaying() {
            if (!stopped) {
                running = false;
                stopped = true;
                out.stop();
                out.close();
            }
        }

        synchronized void donePlaying() {
            pt = null;
        }

        public void run() {
            out.start();
            for (int i = 0; !stopped && (iRepeat == -1 || i < iRepeat); i++) {
                running = true;
                mm.setCurrentPatternPos(0);
                while (running && mm.getSequenceLoopCount() == 0) {
                    synchronized (this) {
                        Debug.println("Real Time.");
                        mm.doRealTimePlayback();
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
            synchronized (this) {
                if (!stopped) {
                    running = false;
                    out.stop();
                    out.close();
                    donePlaying();
                }
            }
        }
    }

    /**
	 * Method to stop all current sound, useful e.g. when closing an AGE window.
	 */
    public void stopAllSound() {
        for (Iterator iterator = basicPlayers.values().iterator(); iterator.hasNext(); ) {
            BasicPlayer bp = (BasicPlayer) iterator.next();
            try {
                bp.stop();
            } catch (BasicPlayerException bpe) {
                bpe.printStackTrace();
            }
        }
        basicPlayers.clear();
        if (seqr != null && seqr.isOpen()) seqr.stop();
        if (seqr != null) midiClose();
        try {
            stopMOD();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

interface PlayerThread extends Runnable {

    public abstract void setVolume(int vol);

    public abstract void stopPlaying() throws Exception;
}
