package xplayer;

import org.netbeans.modules.nbplayer.lib.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import javax.sound.sampled.*;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;
import org.tritonus.share.sampled.file.TAudioFileFormat;

public class JPlayerThread extends Thread {

    public enum Status {

        playing, stopped, paused, finishing
    }

    ;

    private SourceDataLine line = null;

    private AudioInputStream din = null;

    private Clip clip = null;

    private AudioFormat decodedFormat;

    private String openedFile = "";

    private File currentFile;

    private int currentTrack = 0;

    private float volume = 1;

    private boolean opened = false;

    private long length;

    private String title;

    private Status status;

    private ArrayList playlist;

    /** Creates a new instance of JPlayerThread */
    public JPlayerThread() {
        status = Status.stopped;
    }

    public synchronized void run() {
        int nBytesRead;
        byte[] data = new byte[4096];
        status = Status.playing;
        while (true) {
            if (status.equals(Status.finishing)) break;
            if (line != null) {
                try {
                    line.open(decodedFormat);
                } catch (LineUnavailableException ex) {
                    ex.printStackTrace();
                }
                line.start();
                setVolume(volume);
                try {
                    while ((nBytesRead = din.read(data, 0, data.length)) != -1) {
                        if (status.equals(Status.finishing)) break;
                        if (status.equals(Status.stopped)) break;
                        if (status.equals(Status.paused)) {
                            try {
                                wait(100);
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }
                        } else {
                            line.write(data, 0, nBytesRead);
                        }
                    }
                    if (playlist != null && playlist.size() > 0 && currentTrack < playlist.size() && !(status.equals(Status.stopped)) && !(status.equals(Status.finishing))) {
                        next();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            try {
                wait(100);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void play() {
        if (openedFile.equals("")) return;
        if (!this.isAlive()) {
            start();
        } else {
            restart();
        }
        status = Status.playing;
    }

    public void pause() {
        if (!(status.equals(Status.paused))) {
            line.flush();
            status = Status.paused;
        } else {
            status = Status.playing;
        }
    }

    public void restart() {
        try {
            if (!opened) openFile(openedFile, false);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        status = Status.playing;
    }

    public void setVolume(float volume) {
        this.volume = volume;
        if (line != null && line.isOpen()) {
            FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            double gain = volume;
            float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
            gainControl.setValue(dB);
        }
    }

    public void stopPlayback() {
        status = Status.stopped;
        opened = false;
        if (line != null) {
            line.flush();
            line.drain();
            line.stop();
            line.close();
        }
        try {
            if (din != null) {
                din.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void openFile(String filename, boolean resetPlaylist) throws FileNotFoundException {
        if (resetPlaylist) playlist = null;
        if (!status.equals(Status.paused) && !status.equals(Status.stopped)) {
            stopPlayback();
        }
        if (filename.endsWith(".m3u") || filename.endsWith(".pls")) {
            openPlaylist(filename);
            filename = (String) playlist.get(0);
        }
        MpegAudioFileReader reader = new MpegAudioFileReader();
        currentFile = new File(filename);
        openedFile = filename;
        AudioInputStream in = null;
        try {
            in = reader.getAudioInputStream(currentFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (UnsupportedAudioFileException ex) {
            ex.printStackTrace();
        }
        AudioFormat baseFormat = in.getFormat();
        decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
        din = AudioSystem.getAudioInputStream(decodedFormat, in);
        AudioFileFormat baseFileFormat = null;
        try {
            baseFileFormat = AudioSystem.getAudioFileFormat(currentFile);
        } catch (UnsupportedAudioFileException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (baseFileFormat instanceof TAudioFileFormat) {
            Map properties = ((TAudioFileFormat) baseFileFormat).properties();
            String key = "duration";
            length = (Long) properties.get(key);
            key = "title";
            title = (String) properties.get(key);
            if (title == null || title.equals("")) title = currentFile.getName();
        }
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, decodedFormat);
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        }
        opened = true;
        play();
    }

    public void openDef() throws FileNotFoundException {
        openFile("c:/mp3/Hair/01-Aquarius.mp3", true);
    }

    public ArrayList getPlaylist() {
        return playlist;
    }

    public Status getStatus() {
        return status;
    }

    public void openPlaylist(String filename) throws FileNotFoundException {
        File f = new File(filename);
        if (!f.exists()) {
            throw new FileNotFoundException();
        }
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(f));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        String line;
        String path = new File(filename).getParentFile().getAbsolutePath();
        playlist = new ArrayList();
        try {
            while ((line = in.readLine()) != null) {
                File temp = new File(path.concat(File.separator + line));
                if (!temp.exists()) {
                    System.out.println("Nonexistent file in playlist: " + path.concat(File.separator + line));
                } else {
                    System.out.println("Added to playlist: " + path.concat(File.separator + line));
                    playlist.add(path.concat(File.separator + line));
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        try {
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        currentTrack = 0;
        if (!this.isAlive()) {
            start();
        } else {
            restart();
        }
    }

    public void exit() {
        stopPlayback();
        status = Status.finishing;
    }

    public void next() {
        currentTrack++;
        if (playlist == null) {
            System.out.println("No playlist available.");
            return;
        }
        if (currentTrack > playlist.size() - 1) {
            System.out.println("No next track available.");
            currentTrack--;
            return;
        }
        openedFile = (String) playlist.get(currentTrack);
        if (!status.equals(Status.paused) && !status.equals(Status.stopped)) {
            stopPlayback();
            restart();
        } else if (status.equals(Status.paused)) {
            stopPlayback();
        }
    }

    public void prev() {
        currentTrack--;
        if (playlist == null) {
            System.out.println("No playlist available.");
            return;
        }
        if (currentTrack < 0) {
            System.out.println("No prev track available.");
            currentTrack++;
            return;
        }
        openedFile = (String) playlist.get(currentTrack);
        if (!status.equals(Status.paused) && !status.equals(Status.stopped)) {
            stopPlayback();
            restart();
        } else if (status.equals(Status.paused)) {
            stopPlayback();
        }
    }

    public int getPosition() {
        if (line != null) {
            float pos = (float) line.getMicrosecondPosition();
            float len = (float) length;
            float position = (pos / len) * 100;
            return (int) position;
        } else {
            return 0;
        }
    }

    public long getLength() {
        return length;
    }

    public long getPositionInMSec() {
        if (line != null) {
            return line.getMicrosecondPosition();
        } else {
            return 0;
        }
    }

    public String getTitle() {
        return title;
    }

    public void moveProgress(int progress) {
        System.out.println("Seeking is not implemented yet.");
    }
}
