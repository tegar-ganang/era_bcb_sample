package edu.mit.csail.sls.wami.applet.sound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import org.spantus.logger.Logger;

/**
 * Controls recording of audio
 * 
 * Unless otherwise noted, methods are not thread-safe.
 * 
 */
class AudioRecorder implements Recorder {

    private static Logger LOG = Logger.getLogger(AudioRecorder.class);

    protected AudioFormat currentAudioFormat;

    protected AudioFormat convertFormat;

    protected AudioFormat lineFormat;

    protected volatile TargetDataLine line = null;

    protected volatile boolean recording = false;

    LinkedList<Listener> listeners = new LinkedList<Listener>();

    protected Mixer.Info preferredMixerInfo;

    /**
	 * Add an event listener
	 * 
	 * @param l
	 *            The listener
	 * 
	 */
    public void addListener(Listener l) {
        listeners.add(l);
    }

    /**
	 * Remove an event listener
	 * 
	 * @param l
	 *            The listener
	 * 
	 */
    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    void recordingHasStarted() {
        for (Listener listener : listeners) {
            listener.recordingHasStarted();
        }
    }

    void recordingHasEnded() {
        for (Listener listener : listeners) {
            listener.recordingHasEnded();
        }
    }

    /**
	 * Find a line for a desired format
	 * 
	 */
    protected void setLine(AudioFormat desiredAudioFormat) {
        if (currentAudioFormat != null && currentAudioFormat.matches(desiredAudioFormat)) return;
        AudioFormat minFormat = null;
        AudioFormatComparator comp = new AudioFormatComparator(desiredAudioFormat) {

            @Override
            public int conversionCompare(AudioFormat f1, AudioFormat f2) {
                boolean c1 = AudioSystem.isConversionSupported(desiredAudioFormat, f1);
                boolean c2 = AudioSystem.isConversionSupported(desiredAudioFormat, f2);
                if (c1) {
                    if (!c2) {
                        return -1;
                    }
                } else if (!c2) {
                    return 1;
                }
                return 0;
            }
        };
        ArrayList<Mixer.Info> minfoList = new ArrayList<Mixer.Info>(Arrays.asList(AudioSystem.getMixerInfo()));
        LOG.debug("[setLine](recorder) preferred mixer is: {0}", preferredMixerInfo);
        if (preferredMixerInfo != null) {
            minfoList.remove(preferredMixerInfo);
            minfoList.add(0, preferredMixerInfo);
        }
        Mixer.Info[] minfo = minfoList.toArray(new Mixer.Info[minfoList.size()]);
        for (int i = 0; i < minfo.length; i++) {
            Mixer mixer = AudioSystem.getMixer(minfo[i]);
            System.out.format("Mixer: %s%n", minfo[i].getName());
            Line.Info[] linfo = mixer.getTargetLineInfo();
            for (int j = 0; j < linfo.length; j++) {
                if (!(linfo[j] instanceof DataLine.Info)) {
                    continue;
                }
                DataLine.Info dinfo = (DataLine.Info) linfo[j];
                AudioFormat[] formats = dinfo.getFormats();
                for (int k = 0; k < formats.length; k++) {
                    AudioFormat f = formats[k];
                    if (comp.compare(f, minFormat) == -1) {
                        System.out.println("set minFormat to " + f + " on mixer " + mixer.getMixerInfo());
                        minFormat = f;
                    }
                }
            }
        }
        currentAudioFormat = desiredAudioFormat;
        if (lineFormat != null && !lineFormat.matches(minFormat)) {
            closeLine();
        }
        lineFormat = minFormat;
        if (lineFormat.getSampleRate() == AudioSystem.NOT_SPECIFIED) {
            lineFormat = new AudioFormat(lineFormat.getEncoding(), desiredAudioFormat.getSampleRate(), lineFormat.getSampleSizeInBits(), lineFormat.getChannels(), lineFormat.getFrameSize(), desiredAudioFormat.getFrameRate(), lineFormat.isBigEndian());
        }
        AudioFormat cdf = AudioFormatComparator.channelFormat(desiredAudioFormat, lineFormat.getChannels());
        convertFormat = AudioSystem.isConversionSupported(cdf, lineFormat) ? cdf : lineFormat;
    }

    /**
	 * Stop recording.
	 * 
	 */
    public synchronized void closeLine() {
        if (recording) {
            line.stop();
            line.drain();
            line.close();
            line = null;
            recording = false;
            notifyAll();
            recordingHasEnded();
        }
    }

    /**
	 * Get the recording stream
	 * 
	 * @param desiredAudioFormat
	 *            The audio format
	 * 
	 */
    public AudioInputStream getAudioInputStream(AudioFormat desiredAudioFormat) throws LineUnavailableException {
        System.err.println("desired audio format: " + desiredAudioFormat);
        setLine(desiredAudioFormat);
        line = null;
        try {
            LOG.debug("[getAudioInputStream](recording) line format: {0}" + lineFormat);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, lineFormat);
            if (preferredMixerInfo != null) {
                Mixer mixer = AudioSystem.getMixer(preferredMixerInfo);
                try {
                    line = (TargetDataLine) mixer.getLine(info);
                } catch (IllegalArgumentException e) {
                    line = (TargetDataLine) AudioSystem.getLine(info);
                }
            } else {
                line = (TargetDataLine) AudioSystem.getLine(info);
            }
            line.open(lineFormat, 8192);
            synchronized (this) {
                recording = true;
                notifyAll();
            }
            AudioInputStream lineStream = new AudioInputStream(line);
            AudioInputStream ais = convertFormat.matches(lineFormat) ? lineStream : AudioSystem.getAudioInputStream(convertFormat, lineStream);
            if (!desiredAudioFormat.matches(ais.getFormat())) {
                LOG.debug("[getAudioInputStream] Resampling! Frame size {0}", lineStream.getFormat().getFrameSize());
                ais = new ResampleAudioInputStream(desiredAudioFormat, lineStream);
            }
            LOG.debug("[getAudioInputStream] Converted to {0}", ais.getFormat());
            line.start();
            recordingHasStarted();
            return ais;
        } catch (LineUnavailableException e) {
            line = null;
            throw e;
        }
    }

    /**
	 * Returns true if recording
	 * 
	 * Can be called from any thread
	 * 
	 */
    public synchronized boolean isRecording() {
        return recording;
    }

    /**
	 * Sets the preferred mixer to use for recording (note not thread safe at
	 * the moment)
	 */
    public void setPreferredMixer(Mixer.Info mInfo) {
        preferredMixerInfo = mInfo;
    }

    public void setPreferredMixer(String name) {
        for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
            if (mi.getName().equals(name)) {
                setPreferredMixer(mi);
                return;
            }
        }
    }

    public Mixer.Info getPreferredMixer() {
        return preferredMixerInfo;
    }
}
