package de.fhb.defenderTouch.audio;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Klasse realisiert das Abspielen von Sample-Soundfiles.
 * 
 * Die Klasse ist als Thread realisiert, um Nebenlaeufigkeit waehrend des Abspielens 
 * zu gewaehrleisten. 
 * 
 * Nach dem Abspielen eines Sample-Soundfiles wird der zugeordnete {@code SamplePlayer} aufgerufen {@code skip}, 
 * um zum naechsten Song zu schalten.
 * 
 * Es werden alle Standard-Java Sound-Formate unterstuetzt. 
 * Weitere Service Provider koennen als Library hinzugefuegt werden ({@link href="http://java.sun.com/j2se/1.5.0/docs/api/javax/sound/sampled/spi/package-summary.html>spi-Definition})
 * 
 * @author berdux
 * @version 0.9 beta
 * 
 * @see MidiSong
 * @see <a
 *      href="http://java.sun.com/j2se/1.5.0/docs/api/javax/sound/midi/MidiSequencer.html">MidiSequencer</a>
 */
public class SampleThread extends Thread {

    private AudioFormat decodedFormat;

    private AudioInputStream din;

    private SourceDataLine line;

    private AudioInputStream in;

    private boolean play = false;

    private float volume = -18f;

    private boolean paused = false;

    /**
	 * Initialisierungskonstruktor
	 * 
	 * @param fileName Name der Sample-Datei
	 * @param soundPlayer zugeordneter SamplePlayer, der Thread verwaltet
	 */
    public SampleThread(String fileName, float volume) throws FormatProblemException {
        super();
        this.volume = volume;
        File file = new File(fileName);
        try {
            in = AudioSystem.getAudioInputStream(file);
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        din = null;
        try {
            AudioFormat baseFormat = in.getFormat();
            decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
            din = AudioSystem.getAudioInputStream(decodedFormat, in);
            play = true;
        } catch (NullPointerException e) {
            throw new FormatProblemException();
        }
    }

    public SampleThread(String fileName, float volume, boolean wert) throws FormatProblemException {
        super();
        this.volume = volume;
        try {
            in = AudioSystem.getAudioInputStream(getClass().getResource(fileName));
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        din = null;
        try {
            AudioFormat baseFormat = in.getFormat();
            decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
            din = AudioSystem.getAudioInputStream(decodedFormat, in);
            play = true;
        } catch (NullPointerException e) {
            throw new FormatProblemException();
        }
    }

    public SourceDataLine getLine() {
        return line;
    }

    /**
	 * Steuerung des Threads.
	 * 
	 * @param playing flase beendet das Abspielen
	 */
    public void play(boolean playing) {
        play = playing;
    }

    /** 
	 * Abspielen des Samples 
	 * 
	 * @see java.lang.Thread#run()
	 */
    public void run() {
        try {
            line = getLine(decodedFormat);
            FloatControl gainControl = (FloatControl) this.getLine().getControl(FloatControl.Type.MASTER_GAIN);
            if ((gainControl.getMinimum() < volume) && (volume < gainControl.getMaximum())) {
                gainControl.setValue(volume);
            }
            if (line != null) {
                line.start();
                int nBytesRead = 0;
                int nBytesWritten = 0;
                byte[] data = new byte[4096];
                while (nBytesRead != -1 && play) {
                    nBytesRead = din.read(data, 0, data.length);
                    if (nBytesRead != -1) nBytesWritten = line.write(data, 0, nBytesRead);
                    synchronized (this) {
                        while (paused) {
                            try {
                                wait();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                line.drain();
                line.stop();
                line.close();
                din.close();
                in.close();
            }
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        }
    }

    /**
	 * Oeffnen einer @see javax.sound.sampled.SourceDataLine , 
	 * in dem Sample abgespielt werden soll.
	 * 
	 * @param audioFormat Format der Sample-Datei
	 * @return geoeffnete {@code SourceDataLine}
	 * @throws LineUnavailableException
	 */
    private SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
        SourceDataLine res = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        res = (SourceDataLine) AudioSystem.getLine(info);
        res.open(audioFormat);
        return res;
    }

    /**
	 * Freigabe der Sequencer-Ressourcen
	 * @see java.lang.Object#finalize()
	 */
    protected void finalize() {
        try {
            if (line != null) {
                line.drain();
                line.stop();
                line.close();
            }
            if (din != null) din.close();
            if (in != null) in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        paused = true;
    }

    public void proceed() {
        paused = false;
        notify();
    }
}
