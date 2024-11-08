package spaceopera.gui.helper;

import javax.sound.midi.*;
import javax.sound.sampled.*;
import spaceopera.gui.SpaceOpera;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.util.Vector;

/** The PlayMusic class is used to play music in a separate thread
 * bug: playing music makes the soundcard unavailable for other applications!
 */
public class PlayMusic implements Runnable, LineListener, MetaEventListener {

    private SpaceOpera spaceOpera;

    private Thread thread;

    private Sequencer sequencer;

    private boolean midiEnd, audioEnd;

    private boolean musicOn;

    private Synthesizer synthesizer;

    private MidiChannel channels[];

    private Object soundObject;

    private int playSequence[];

    private boolean init = true;

    private int oldNumberOfSongs = 0;

    private DataLine.Info info;

    private Clip clip;

    private void close() {
        if (sequencer != null) {
            sequencer.close();
        }
    }

    private int[] getPlaySequence(int numberOfSongs) {
        int sequence[] = new int[numberOfSongs];
        boolean numberUsed[] = new boolean[numberOfSongs];
        for (int i = 0; i < numberOfSongs; i++) {
            numberUsed[i] = false;
        }
        for (int i = 0; i < numberOfSongs; i++) {
            boolean numberFound = false;
            while (!numberFound) {
                int playNumber = (int) (Math.random() * numberOfSongs);
                if (!numberUsed[playNumber]) {
                    numberFound = true;
                    numberUsed[playNumber] = true;
                    sequence[i] = playNumber;
                }
            }
        }
        return (sequence);
    }

    private boolean loadSound(Object o) {
        try {
            soundObject = AudioSystem.getAudioInputStream((File) o);
        } catch (Exception e1) {
            try {
                FileInputStream fis = new FileInputStream((File) o);
                soundObject = new BufferedInputStream(fis, 1024);
            } catch (Exception e2) {
                e2.printStackTrace();
                soundObject = null;
                return false;
            }
        }
        if (sequencer == null) {
            soundObject = null;
            return false;
        }
        if (soundObject instanceof AudioInputStream) {
            try {
                AudioInputStream stream = (AudioInputStream) soundObject;
                AudioFormat format = stream.getFormat();
                if ((format.getEncoding() == AudioFormat.Encoding.ULAW) || (format.getEncoding() == AudioFormat.Encoding.ALAW)) {
                    AudioFormat tmp = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                    stream = AudioSystem.getAudioInputStream(tmp, stream);
                    format = tmp;
                }
                DataLine.Info info = new DataLine.Info(Clip.class, stream.getFormat(), ((int) stream.getFrameLength() * format.getFrameSize()));
                Clip clip = (Clip) AudioSystem.getLine(info);
                clip.addLineListener(this);
                clip.open(stream);
                soundObject = clip;
            } catch (Exception ex) {
                ex.printStackTrace();
                soundObject = null;
                return false;
            }
        } else if (soundObject instanceof Sequence || soundObject instanceof BufferedInputStream) {
            try {
                sequencer.open();
                if (soundObject instanceof Sequence) {
                    sequencer.setSequence((Sequence) soundObject);
                } else {
                    sequencer.setSequence((BufferedInputStream) soundObject);
                }
            } catch (InvalidMidiDataException e3) {
                System.out.println("Unsupported audio file. " + e3);
                soundObject = null;
                return false;
            } catch (Exception e3) {
                e3.printStackTrace();
                soundObject = null;
                return false;
            }
        }
        return true;
    }

    public static void main(String args[]) {
        String sound = "test.au";
        PlayMusic ps = new PlayMusic(null);
    }

    public void meta(MetaMessage message) {
        if (message.getType() == 47) {
            midiEnd = true;
        }
    }

    private void open() {
        try {
            sequencer = MidiSystem.getSequencer();
            if (sequencer instanceof Synthesizer) {
                synthesizer = (Synthesizer) sequencer;
                channels = synthesizer.getChannels();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        sequencer.addMetaEventListener(this);
    }

    public PlayMusic(SpaceOpera so) {
        spaceOpera = so;
        open();
    }

    private void playMusic() {
        midiEnd = audioEnd = false;
        if (soundObject instanceof Sequence || soundObject instanceof BufferedInputStream && thread != null) {
            sequencer.start();
            while (!midiEnd && thread != null && musicOn) {
                try {
                    Thread.sleep(99);
                } catch (Exception e) {
                    break;
                }
            }
            if (sequencer.isRunning()) {
                sequencer.stop();
                sequencer.close();
            }
        } else if (soundObject instanceof Clip && thread != null && musicOn) {
            Clip clip = (Clip) soundObject;
            clip.start();
            try {
                Thread.sleep(99);
            } catch (Exception e) {
            }
            while ((clip.isActive()) && thread != null && musicOn) {
                try {
                    Thread.sleep(99);
                } catch (Exception e) {
                    break;
                }
            }
            clip.stop();
            clip.close();
            clip = null;
        }
        soundObject = null;
    }

    private void playMusicFromDir() {
        File dir = new File(spaceOpera.getMusicDirectory());
        if (dir.isDirectory()) {
            File musicArray[] = dir.listFiles();
            int numberOfSongs = musicArray.length;
            if (init || (numberOfSongs != oldNumberOfSongs)) {
                playSequence = getPlaySequence(numberOfSongs);
                init = false;
                oldNumberOfSongs = numberOfSongs;
            }
            for (int i = 0; i < numberOfSongs; i++) {
                try {
                    if (musicArray[playSequence[i]].isFile()) {
                        if (loadSound(musicArray[playSequence[i]]) == true) {
                            if (musicOn) {
                                System.out.println("playing " + musicArray[playSequence[i]]);
                                playMusic();
                            }
                        } else {
                            System.out.println("could not play " + musicArray[playSequence[i]]);
                        }
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                }
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                }
            }
            try {
                Thread.sleep(500);
            } catch (Exception e) {
            }
        }
    }

    private void playMusicFromList() {
        Vector musicList = spaceOpera.getMusicList();
        int numberOfSongs = musicList.size();
        if (init || (numberOfSongs != oldNumberOfSongs)) {
            playSequence = getPlaySequence(numberOfSongs);
            init = false;
            oldNumberOfSongs = numberOfSongs;
        }
        for (int i = 0; i < numberOfSongs; i++) {
            try {
                String songName = (String) musicList.get(playSequence[i]);
                File song = new File(songName);
                if (song.isFile()) {
                    if (loadSound(song) == true) {
                        if (musicOn) {
                            System.out.println("playing " + songName);
                            playMusic();
                        }
                    }
                }
            } catch (ArrayIndexOutOfBoundsException e) {
            }
            try {
                Thread.sleep(500);
            } catch (Exception e) {
            }
        }
        try {
            Thread.sleep(500);
        } catch (Exception e) {
        }
    }

    public void run() {
        while (musicOn) {
            if (spaceOpera.getMusicFromDir()) {
                playMusicFromDir();
            } else {
                playMusicFromList();
            }
        }
    }

    public void setMusicOn(boolean on) {
        init = true;
        musicOn = on;
        if (musicOn) {
            start();
        } else {
            stop();
        }
    }

    public void start() {
        thread = new Thread(this);
        thread.setName("SpaceOpera Background Music");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        if (thread != null) {
            thread.interrupt();
        }
        thread = null;
    }

    public Thread getThread() {
        return thread;
    }

    public void update(LineEvent event) {
        if (event.getType() == LineEvent.Type.STOP) {
            audioEnd = true;
        }
    }
}
