import sun.audio.*;
import java.io.*;
import javax.sound.sampled.*;

public class Sound implements LineListener {

    String soundFileName;

    Clip clip;

    AudioInputStream inputStream;

    AudioInputStream loadedInputStream;

    AudioFormat decodedFormat;

    SourceDataLine line;

    private boolean startedPlaying = false;

    private boolean finishedPlaying = false;

    /**
	 * Method Sound
	 *
	 *
	 */
    public Sound(String soundFile) {
        soundFileName = soundFile;
    }

    /**
	 * Load the sound.
	 */
    public void load() {
        if (clip != null || loadedInputStream != null) {
            return;
        }
        try {
            String url = Config.SOUNDS_DIR + soundFileName;
            inputStream = AudioSystem.getAudioInputStream(new File(url));
            if (soundFileName.endsWith(".wav")) {
                clip = AudioSystem.getClip();
                clip.open(inputStream);
                url = null;
                inputStream = null;
            } else if (soundFileName.endsWith(".mp3")) {
                loadedInputStream = null;
                AudioFormat baseFormat = inputStream.getFormat();
                decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
                loadedInputStream = AudioSystem.getAudioInputStream(decodedFormat, inputStream);
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

    /**
	 * Detects if sound is currently playing
	 */
    public boolean isCurrentlyPlaying() {
        if (clip != null) {
            return clip.isRunning();
        } else if (line != null) {
            return line.isRunning();
        } else {
            return false;
        }
    }

    /**
	 * Detects if sound stopped playing
	 */
    public boolean isStopped() {
        if (clip != null) {
            return (clip.isActive() == false);
        } else if (line != null) {
            return (line.isActive() == false);
        } else {
            return false;
        }
    }

    /**
	 * Play the sound
	 */
    public void play() {
        if (clip != null) {
            clip.addLineListener(this);
        }
        Thread soundThread = new Thread(new Runnable() {

            public void run() {
                try {
                    if (clip != null) {
                        clip.stop();
                        clip.setFramePosition(0);
                        clip.start();
                        clip.close();
                        clip = null;
                    } else if (loadedInputStream != null) {
                        rawplayMP3(decodedFormat, loadedInputStream);
                        inputStream.close();
                    }
                } catch (Exception ex) {
                }
            }
        });
        soundThread.setName("Sound");
        soundThread.start();
    }

    /**
	 * Update the sound.
	 */
    public void update(LineEvent e) {
        if (e.getType() == LineEvent.Type.START) {
            startedPlaying = true;
        } else if (e.getType() == LineEvent.Type.STOP) {
            finishedPlaying = true;
        }
    }

    /**
	 * Check if the sound started playing.
	 */
    public boolean startedPlaying() {
        return startedPlaying;
    }

    /**
	 * Check if the sound finished playing.
	 */
    public boolean isFinishedPlaying() {
        return finishedPlaying;
    }

    /**
	 * Stop playing the sound.
	 */
    public void stop() {
        if (clip != null) {
            clip.stop();
        } else if (loadedInputStream != null && line != null) {
            line.stop();
        }
    }

    /**
	 * Play the MP3
	 * Used the example from the MP3 Library
	 * SOURCE: http://www.javazoom.net/mp3spi/documents.html
	 */
    private void rawplayMP3(AudioFormat targetFormat, AudioInputStream din) throws IOException, LineUnavailableException {
        byte[] data = new byte[4096];
        line = getLine(targetFormat);
        if (line != null) {
            line.addLineListener(this);
            line.start();
            int nBytesRead = 0, nBytesWritten = 0;
            while (nBytesRead != -1) {
                nBytesRead = din.read(data, 0, data.length);
                if (nBytesRead != -1) {
                    nBytesWritten = line.write(data, 0, nBytesRead);
                }
            }
            line.drain();
            line.stop();
            line.close();
            line = null;
            din.close();
        }
    }

    /**
	 * Decode the mp3 audio line
	 * Used the example from the MP3 Library
	 * SOURCE: http://www.javazoom.net/mp3spi/documents.html
	 */
    private SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
        SourceDataLine res = null;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        res = (SourceDataLine) AudioSystem.getLine(info);
        res.open(audioFormat);
        return res;
    }

    /**
	 * When sound object is garbage collected, 
	 * close sound inputs and garbage collect all class variables.
	 */
    protected void finalize() throws Throwable {
        stop();
        soundFileName = null;
        clip = null;
        inputStream.close();
        inputStream = null;
        loadedInputStream.close();
        loadedInputStream = null;
        line.close();
        line = null;
        decodedFormat = null;
    }
}
