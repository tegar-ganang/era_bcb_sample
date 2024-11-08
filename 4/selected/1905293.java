package com.listentothesong.audio;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.LinkedList;
import java.util.Map;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.addon.ErrorDetailsDialog;
import org.tritonus.share.sampled.file.TAudioFileFormat;

public class AudioPlayer {

    public static class AudioProperties {

        final String author;

        final String album;

        final String title;

        public String getAlbum() {
            return album;
        }

        public String getAuthor() {
            return author;
        }

        public String getTitle() {
            return title;
        }

        public AudioProperties(String author, String album, String title) {
            this.author = author;
            this.album = album;
            this.title = title;
        }
    }

    private final byte[] buffer = new byte[4096];

    private final byte[] largeBuffer = new byte[40960];

    private final RandomAccessFile raf;

    private final File bufferFile;

    private final SourceDataLine soundOutput;

    private final long frameSize;

    private final float frameRate;

    private final LinkedList<AudioPlayerListener> listeners = new LinkedList<AudioPlayerListener>();

    private Thread thread = null;

    private long loadedFramesCount;

    private long currentFrame;

    private boolean loadCompleted = false;

    private final Runnable runnable = new Runnable() {

        public void run() {
            try {
                int nBytesRead = 0;
                do {
                    if (Math.abs(currentFrame - loadedFramesCount) < 10 * frameSize && !loadCompleted) {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            ErrorDetailsDialog.open(e);
                        } catch (Throwable t) {
                            ErrorDetailsDialog.open(t);
                        }
                    }
                    synchronized (raf) {
                        nBytesRead = raf.read(buffer, 0, buffer.length);
                    }
                    if (nBytesRead <= 0) {
                        nBytesRead = 0;
                        synchronized (raf) {
                            raf.seek(0);
                        }
                        currentFrame = 0;
                        synchronized (listeners) {
                            for (AudioPlayerListener listener : listeners) listener.audioEnded(new AudioPlayerEvent(AudioPlayer.this, currentFrame, loadedFramesCount, loadCompleted));
                        }
                    } else {
                        soundOutput.write(buffer, 0, nBytesRead);
                        synchronized (raf) {
                            AudioPlayer.this.currentFrame = raf.getFilePointer() / frameSize;
                        }
                        synchronized (listeners) {
                            for (AudioPlayerListener listener : listeners) listener.frameMoved(new AudioPlayerEvent(AudioPlayer.this, currentFrame, loadedFramesCount, loadCompleted));
                        }
                    }
                } while (thread != null && nBytesRead > 0);
                soundOutput.drain();
                soundOutput.stop();
            } catch (IOException e) {
                ErrorDetailsDialog.open(e);
            } catch (Throwable t) {
                ErrorDetailsDialog.open(t);
            }
            thread = null;
        }
    };

    public AudioPlayer(URL songFile) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        final AudioInputStream encodedInput = AudioSystem.getAudioInputStream(songFile);
        if (encodedInput == null) throw new AssertionError("Error playing file");
        AudioFormat baseFormat = encodedInput.getFormat();
        AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getSampleRate(), false);
        soundOutput = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, targetFormat));
        soundOutput.open(targetFormat);
        final AudioInputStream decodedInput = AudioSystem.getAudioInputStream(targetFormat, encodedInput);
        frameSize = targetFormat.getFrameSize();
        frameRate = targetFormat.getFrameRate();
        bufferFile = File.createTempFile("audioPlayer", ".mp3");
        bufferFile.deleteOnExit();
        raf = new RandomAccessFile(bufferFile, "rw");
        currentFrame = 0;
        raf.seek(0);
        new Thread(new Runnable() {

            public void run() {
                try {
                    long currentLocation = 0;
                    int nBytesRead = 0;
                    while ((nBytesRead = decodedInput.read(largeBuffer, 0, largeBuffer.length)) != -1) {
                        synchronized (raf) {
                            long previousLocation = raf.getFilePointer();
                            raf.seek(currentLocation);
                            raf.write(largeBuffer, 0, nBytesRead);
                            raf.seek(previousLocation);
                        }
                        currentLocation += nBytesRead;
                        loadedFramesCount = currentLocation / frameSize;
                        synchronized (listeners) {
                            for (AudioPlayerListener listener : listeners) listener.frameLoaded(new AudioPlayerEvent(AudioPlayer.this, currentFrame, loadedFramesCount, loadCompleted));
                        }
                    }
                    loadCompleted = true;
                    synchronized (listeners) {
                        for (AudioPlayerListener listener : listeners) listener.frameLoaded(new AudioPlayerEvent(AudioPlayer.this, currentFrame, loadedFramesCount, loadCompleted));
                    }
                    decodedInput.close();
                    encodedInput.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Throwable t) {
                    ErrorDetailsDialog.open(t);
                }
            }
        }).start();
    }

    public void addFramesMotionListener(AudioPlayerListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public boolean removeFramesMotionListener(AudioPlayerListener listener) {
        synchronized (listeners) {
            return listeners.remove(listener);
        }
    }

    public long getCurrentFrame() {
        return currentFrame;
    }

    public void setCurrentFrame(long currentFrame) throws IOException, InterruptedException {
        currentFrame = Math.max(currentFrame, 0);
        if (Math.abs(this.currentFrame - currentFrame) > 10) {
            if (isPlaying()) {
                stop();
                this.currentFrame = currentFrame;
                synchronized (listeners) {
                    for (AudioPlayerListener listener : listeners) listener.frameMoved(new AudioPlayerEvent(AudioPlayer.this, currentFrame, loadedFramesCount, loadCompleted));
                }
                synchronized (raf) {
                    raf.seek(currentFrame * frameSize);
                }
                play();
            } else {
                this.currentFrame = currentFrame;
                synchronized (listeners) {
                    for (AudioPlayerListener listener : listeners) listener.frameMoved(new AudioPlayerEvent(AudioPlayer.this, currentFrame, loadedFramesCount, loadCompleted));
                }
                synchronized (raf) {
                    raf.seek(currentFrame * frameSize);
                }
            }
        }
    }

    public void advanceInSeconds(long seconds) throws IOException, InterruptedException {
        setCurrentFrame((long) (currentFrame + frameRate * seconds));
    }

    public void play() throws IOException {
        if (thread == null) {
            thread = new Thread(runnable);
            soundOutput.start();
            thread.start();
        }
    }

    public boolean isPlaying() {
        return thread != null;
    }

    public void stop() throws InterruptedException {
        if (thread != null) {
            Thread t = thread;
            thread = null;
            t.join();
        }
    }

    public void close() throws IOException {
        raf.close();
        bufferFile.delete();
    }

    public long getFramesCount() {
        return 0;
    }

    public boolean isLoadCompleted() {
        return loadCompleted;
    }

    @SuppressWarnings("unchecked")
    public static AudioProperties getAudioProperties(URL url) throws UnsupportedAudioFileException, IOException {
        AudioFileFormat baseFileFormat = AudioSystem.getAudioFileFormat(url);
        if (baseFileFormat instanceof TAudioFileFormat) {
            Map properties = ((TAudioFileFormat) baseFileFormat).properties();
            String author = (String) properties.get("author");
            String album = (String) properties.get("album");
            String title = (String) properties.get("title");
            return new AudioProperties(author == null ? null : author.trim(), album == null ? null : album.trim(), title == null ? null : title.trim());
        }
        return null;
    }
}
