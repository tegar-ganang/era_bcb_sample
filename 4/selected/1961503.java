package spellit;

import java.io.*;
import java.util.logging.Logger;
import javax.sound.sampled.*;

public class Sound {

    private static Logger log = Logger.getLogger(OpenSpell.class.getName());

    public interface StopListener {

        public void stop();
    }

    private static final int MODE_READY = 0;

    private static final int MODE_RECORDING = 1;

    private static final int MODE_PLAYING = 2;

    private AudioCapture audioCapture;

    private File audioFile;

    private SourceDataLine line;

    private int mode;

    private boolean isSaved;

    private AudioInputStream audioInputStream;

    private byte[] buffer = new byte[128000];

    private int read;

    private int written;

    public void initAudioCapture() {
        if (audioCapture == null) {
            audioCapture = new AudioCapture();
            audioCapture.addStopListener(new StopListener() {

                public void stop() {
                    synchronized (this) {
                        mode = MODE_READY;
                    }
                }
            });
        }
    }

    public void free() {
        audioCapture.free();
    }

    public void freeAudioCapture() {
        if (audioCapture != null) {
            audioCapture = null;
        }
    }

    public void init() {
        initAudioCapture();
        audioFile = null;
        line = null;
        mode = MODE_READY;
        isSaved = true;
    }

    public Sound() {
        init();
    }

    public void setAudioFile(File f) {
        audioFile = f;
    }

    public boolean startFilePlayback(File file) {
        audioInputStream = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(file);
        } catch (Exception e) {
            log.warning("Exception: Sound: getAudioInputStream: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        AudioFormat audioFormat = audioInputStream.getFormat();
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        if (!AudioSystem.isLineSupported(info)) {
            AudioFormat sourceFormat = audioFormat;
            AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(), sourceFormat.getChannels() * 2, sourceFormat.getSampleRate(), false);
            audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
            audioFormat = audioInputStream.getFormat();
            info = new DataLine.Info(SourceDataLine.class, audioFormat);
            if (!AudioSystem.isLineSupported(info)) {
                log.warning("Error Sound: DataLine not supported");
                return false;
            }
        }
        if (line != null) {
            line.close();
            line = null;
        }
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.addLineListener(new LineListener() {

                public void update(LineEvent event) {
                    if (event.getType() == LineEvent.Type.STOP) {
                        synchronized (this) {
                            mode = MODE_READY;
                        }
                    }
                }
            });
            line.open(audioFormat);
        } catch (LineUnavailableException e) {
            log.warning("WARNING: Sound: Couldn't get sound line.\n" + "Sound device may be in use by another application.");
            return false;
        } catch (Exception e) {
            log.warning("Exception: Sound: getLine: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        line.start();
        written = 0;
        read = 0;
        return true;
    }

    public void stopFilePlayback() {
        synchronized (this) {
            if (mode == MODE_PLAYING) {
                line.drain();
            }
        }
        line.stop();
        line.close();
        line = null;
    }

    public void poll() throws IOException {
        synchronized (this) {
            if (mode != MODE_PLAYING) {
                return;
            }
        }
        if (audioCapture.isUsed()) {
            return;
        }
        if (written >= read) {
            try {
                read = audioInputStream.read(buffer, 0, buffer.length);
                written = 0;
            } catch (IOException e) {
                log.warning("Sound: " + e.getMessage());
                e.printStackTrace();
                throw e;
            }
        }
        if (read > written) {
            int temp = line.write(buffer, written, read - written);
            written += temp;
        }
        if (read == -1) {
            stopFilePlayback();
            synchronized (this) {
                mode = MODE_READY;
            }
        }
    }

    public void record() {
        synchronized (this) {
            switch(mode) {
                case MODE_READY:
                    break;
                case MODE_RECORDING:
                    return;
                case MODE_PLAYING:
                    stop();
                    break;
            }
        }
        if (audioCapture.isUsed()) {
            freeAudioCapture();
            initAudioCapture();
        }
        isSaved = false;
        audioCapture.startCapture();
        synchronized (this) {
            mode = MODE_RECORDING;
        }
    }

    public void stop() {
        synchronized (this) {
            switch(mode) {
                case MODE_READY:
                    return;
                case MODE_RECORDING:
                    audioCapture.stopCapture();
                    break;
                case MODE_PLAYING:
                    if (audioCapture.isPlaying()) {
                        audioCapture.stopPlayback();
                    } else {
                        stopFilePlayback();
                    }
                    break;
            }
            mode = MODE_READY;
        }
    }

    public void play() {
        synchronized (this) {
            switch(mode) {
                case MODE_READY:
                    break;
                case MODE_RECORDING:
                    audioCapture.stopCapture();
                    break;
                case MODE_PLAYING:
                    return;
            }
        }
        if (audioCapture.isUsed()) {
            audioCapture.startPlayback();
            synchronized (this) {
                mode = MODE_PLAYING;
            }
        } else if (audioFile != null && audioFile.exists()) {
            if (startFilePlayback(audioFile)) {
                synchronized (this) {
                    mode = MODE_PLAYING;
                }
            }
        }
    }

    public boolean isPlaying() {
        synchronized (this) {
            return (mode == MODE_PLAYING);
        }
    }

    public void setIsSaved(boolean isSaved) {
        this.isSaved = isSaved;
    }

    public boolean isSaved() {
        return isSaved;
    }

    public void save() throws IOException {
        if (isSaved) {
            throw new IOException("Cannot save, Sound is already saved.");
        }
        if (audioFile == null) {
            throw new IOException("Cannot save, Destination file not specified.");
        }
        if (!audioCapture.isUsed()) {
            throw new IOException("Cannot save, nothing to save.");
        }
        audioCapture.write(audioFile);
        isSaved = true;
    }

    public static void soundCheck(String path) {
        boolean success = true;
        String message = "";
        Exception exception = null;
        log.info("Performing sound check.\t");
        File file = new File(path);
        AudioInputStream audioInputStream = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(file);
        } catch (Exception e) {
            success = false;
            message = "Exception: Sound: getAudioInputStream: " + e.getMessage();
            exception = e;
        }
        if (success) {
            AudioFormat audioFormat = audioInputStream.getFormat();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            if (!AudioSystem.isLineSupported(info)) {
                AudioFormat sourceFormat = audioFormat;
                AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(), sourceFormat.getChannels() * 2, sourceFormat.getSampleRate(), false);
                audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
                audioFormat = audioInputStream.getFormat();
                info = new DataLine.Info(SourceDataLine.class, audioFormat);
                if (!AudioSystem.isLineSupported(info)) {
                    success = false;
                    message = "Error Sound: DataLine not supported";
                }
            }
        }
        if (success) {
            log.info("Sound check passed.");
        } else {
            log.warning("Sound check failed.");
            log.warning("Sound check message: " + message);
            if (exception != null) {
                exception.printStackTrace();
            }
            Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
            for (int i = 0; i < mixerInfo.length; i++) {
                log.info("Mixer info:\n" + "Mixer[" + i + "] desc: " + mixerInfo[i].getDescription() + "\n" + "Mixer[" + i + "] name: " + mixerInfo[i].getName() + "\n" + "Mixer[" + i + "] vendor: " + mixerInfo[i].getVendor() + "\n" + "Mixer[" + i + "] version: " + mixerInfo[i].getVersion());
            }
        }
    }
}
