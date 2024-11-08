package net.sf.opendub.xplayer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.util.ArrayList;
import java.util.Properties;
import java.net.URL;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import net.sf.opendub.xplayer.resources.Messages;

public class XPlayer {

    public static String ARCHIVE_EXTENSION = ".xplay";

    public static String INDEX_NAME = "index.property";

    public static int STREAMER_RESOLUTION = 512;

    private ArrayList<XPlayerTrack> tracks = new ArrayList<XPlayerTrack>();

    private Mixer.Info mixer = null;

    SourceDataLine output = null;

    ArrayList<XPlayerListener> listeners = new ArrayList<XPlayerListener>();

    private File file = null;

    private boolean fileIsIndex = false;

    private Properties index = new Properties();

    private String workspace = null;

    boolean playing = false;

    boolean stopping = false;

    Float volume = 1.0F;

    XPlayerStreamer streamer = null;

    private XPlayer(File file) throws IOException, XPlayerIndexEmptyException, UnsupportedAudioFileException, XPlayerSourcesEmptyException {
        this.file = file;
        if (file.getName().endsWith(ARCHIVE_EXTENSION) && file.isFile()) {
        } else {
            index.load(new FileInputStream(file));
            workspace = file.getParent();
            fileIsIndex = true;
        }
        if (!workspace.endsWith(System.getProperty("file.separator"))) {
            workspace += System.getProperty("file.separator");
        }
        int trackId = 0;
        while (true) {
            XPlayerTrack track;
            int sourceDefault = 1;
            float trackAmplitude = 0.8F;
            trackId++;
            track = new XPlayerTrack(index.getProperty("track" + String.valueOf(trackId)));
            if (track.getLabel() == null) {
                break;
            }
            try {
                sourceDefault = Integer.parseInt(index.getProperty("track" + String.valueOf(trackId) + ".default"));
            } catch (NumberFormatException ex) {
            } catch (NullPointerException ex) {
            }
            try {
                trackAmplitude = Float.parseFloat(index.getProperty("track" + String.valueOf(trackId) + ".amp"));
            } catch (NumberFormatException ex) {
            } catch (NullPointerException ex) {
            }
            track.setAmplitude(trackAmplitude);
            int sourceId = 0;
            while (true) {
                XPlayerSource source;
                String sourcePath;
                sourceId++;
                sourcePath = index.getProperty("track" + String.valueOf(trackId) + "." + String.valueOf(sourceId) + ".source");
                if (sourcePath == null) {
                    break;
                }
                if (sourcePath.toLowerCase().startsWith("http://")) {
                    source = new XPlayerSource(new URL(sourcePath));
                } else {
                    source = new XPlayerSource(new File(workspace + sourcePath));
                }
                source.setName(index.getProperty("track" + String.valueOf(trackId) + "." + String.valueOf(sourceId)));
                if (source.getName() == null) {
                    source.setName(Messages.sourceNoName.toString());
                }
                track.getSources().add(source);
                if (sourceDefault == sourceId) {
                    track.setSource(source);
                }
            }
            if (track.getSources().size() < 1) {
                throw new XPlayerSourcesEmptyException();
            }
            tracks.add(track);
        }
        if (tracks.size() < 1) {
            throw new XPlayerIndexEmptyException();
        }
    }

    public synchronized void syncWait() throws InterruptedException {
        wait();
    }

    /**
		 * Load xplayer archive/index
		 * @param file
		 */
    public static XPlayer load(File file) throws LineUnavailableException, IOException, XPlayerIndexEmptyException, UnsupportedAudioFileException, XPlayerSourcesEmptyException {
        return new XPlayer(file);
    }

    public ArrayList<XPlayerTrack> getTracks() {
        return tracks;
    }

    /**
		 * Save xplayer archive
		 * @param file
		 */
    public void save(File file) {
        stop();
    }

    /**
		 * Play selected sources from the begin
		 *
		 */
    public void play() throws LineUnavailableException, IOException, UnsupportedAudioFileException {
        outputOpen();
        streamer = new XPlayerStreamer(this);
        playing = true;
        streamer.start();
        updateState();
    }

    public void stop() {
        stopping = true;
    }

    public boolean isPlaying() {
        return playing;
    }

    public Mixer.Info getMixer() {
        if (mixer == null) {
            mixer = AudioSystem.getMixer(null).getMixerInfo();
        }
        return mixer;
    }

    public void setMixer(Mixer.Info mixer) {
        this.mixer = mixer;
    }

    public boolean isFileIsIndex() {
        return fileIsIndex;
    }

    public void addListener(XPlayerListener listener) {
        listeners.add(listener);
    }

    public void removeListener(XPlayerListener listener) {
        listeners.remove(listener);
    }

    void close() {
        outputClose();
        playing = false;
        stopping = false;
        updateState();
    }

    private void outputOpen() throws LineUnavailableException, IOException, UnsupportedAudioFileException {
        AudioFormat format = getTracks().get(0).getSource().getStream().getFormat();
        DataLine.Info info;
        format = new AudioFormat(format.getEncoding(), format.getSampleRate(), format.getSampleSizeInBits(), format.getChannels(), format.getFrameSize(), format.getFrameRate(), format.isBigEndian());
        info = new DataLine.Info(SourceDataLine.class, format);
        output = (SourceDataLine) AudioSystem.getMixer(getMixer()).getLine(info);
        output.open(format);
        output.start();
    }

    private void outputClose() {
        output.stop();
        output.drain();
        output.close();
        output = null;
    }

    void updateState() {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                for (XPlayerListener listener : listeners) {
                    listener.updateState();
                }
            }
        });
    }

    public Float getVolume() {
        return volume;
    }

    public void setVolume(Float volume) {
        this.volume = volume;
    }

    public int getPosition() {
        if (streamer == null) {
            return 0;
        }
        return streamer.samples;
    }

    public int getPositionMax() {
        if (streamer == null) {
            return 0;
        }
        return streamer.samplesTotal;
    }
}

class XPlayerStreamer extends Thread {

    XPlayer player = null;

    int samples = 0;

    int samplesTotal = 0;

    public XPlayerStreamer(XPlayer player) {
        this.player = player;
    }

    /**
	 * Play track using selected source
	 */
    public void run() {
        int maxLenght = 0;
        Exception exception = null;
        try {
            for (XPlayerTrack track : player.getTracks()) {
                if (track.getSource().getLength(player.output.getFormat().getFrameRate()) > maxLenght) {
                    maxLenght = (int) track.getSource().getLength(player.output.getFormat().getFrameRate());
                }
            }
        } catch (IOException ex) {
            exception = ex;
        } catch (UnsupportedAudioFileException ex) {
            exception = ex;
        }
        if (exception != null) {
            System.out.println("CATCHED---------" + exception);
            exception.printStackTrace();
            player.stopping = true;
        }
        samplesTotal = maxLenght * player.output.getFormat().getFrameSize();
        XPlayerStreamerWriter writer = new XPlayerStreamerWriter(this);
        writer.start();
        while (!player.stopping) {
            byte[] data = new byte[XPlayer.STREAMER_RESOLUTION];
            exception = null;
            try {
                int framesRead = 0;
                boolean end = true;
                for (XPlayerTrack track : player.getTracks()) {
                    byte[] read = new byte[XPlayer.STREAMER_RESOLUTION];
                    track.getSource().getStream().setAmplitudeLinear(track.getAmplitude() * player.volume);
                    framesRead = track.getSource().getStream().read(read);
                    if (framesRead != -1) {
                        end = false;
                        if (!track.mute) {
                            for (int readId = 0; readId < XPlayer.STREAMER_RESOLUTION; readId++) {
                                int compute = data[readId];
                                compute += read[readId];
                                data[readId] = (byte) (compute & 0xFF);
                            }
                        }
                    }
                }
                if (end) {
                    player.stopping = true;
                } else {
                    writer.buffer.add(data);
                    while (writer.buffer.size() > (XPlayer.STREAMER_RESOLUTION / 8)) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException ex) {
                            break;
                        }
                    }
                }
            } catch (IOException ex) {
                exception = ex;
            } catch (UnsupportedAudioFileException ex) {
                exception = ex;
            }
            if (exception != null) {
                System.out.println("CATCHED---------" + exception);
                exception.printStackTrace();
                player.stopping = true;
            }
        }
        writer.running = false;
        for (XPlayerTrack track : player.getTracks()) {
            track.getSource().setStream(null);
        }
        player.close();
    }
}

class XPlayerStreamerWriter extends Thread {

    boolean running = false;

    ArrayList<byte[]> buffer = new ArrayList<byte[]>();

    private XPlayerStreamer streamer;

    XPlayerStreamerWriter(XPlayerStreamer streamer) {
        this.streamer = streamer;
    }

    public void run() {
        running = true;
        while (buffer.size() < XPlayer.STREAMER_RESOLUTION / 16) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                running = false;
                break;
            }
        }
        while (running) {
            while (buffer.size() > 0 && running) {
                byte[] data = buffer.remove(0);
                streamer.player.output.write(data, 0, XPlayer.STREAMER_RESOLUTION);
                streamer.samples += XPlayer.STREAMER_RESOLUTION;
                if (buffer.size() < (XPlayer.STREAMER_RESOLUTION / 64)) {
                    int warning = 8;
                    while (buffer.size() < (XPlayer.STREAMER_RESOLUTION / 64)) {
                        warning--;
                        if (warning == 0) {
                            System.out.println("WARNING: Buffer LOW =" + buffer.size() + "/" + (XPlayer.STREAMER_RESOLUTION / 8));
                            warning = 8;
                        }
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException ex) {
                            running = false;
                            break;
                        }
                    }
                }
            }
        }
    }
}
