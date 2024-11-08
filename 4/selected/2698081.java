package com.degani.audio;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

/**
 * Audio recorder thread based on the JavaSound API
 * @author ismaildegani
 *
 */
public class AudioOutput extends Thread implements AudioLoopSystem {

    /**
	 * Map that keeps track of supported input devices
	 */
    private static Map<String, SourceDataLine> outputLines;

    /**
	 * Record in 16bit, 44.1Khz CD Quality audio (mono)
	 */
    private static AudioFormat format = new AudioFormat(44100, 16, 1, true, false);

    /**
     * The chosen input line's name
     */
    String selectedOutputLineName;

    /**
     *	The chosen input line      
     */
    SourceDataLine selectedOutputLine;

    boolean audioLoopRunning;

    boolean stopAudioLoop;

    /**
     * The temporary in-memory buffer where the sound data resides before
     * being compressed and written to disk
     */
    byte[] readBuffer;

    /**
	 * The abstract provider of audio data 
	 */
    AudioDataProvider audioDataProvider;

    public AudioDataProvider getAudioDataProvider() {
        return audioDataProvider;
    }

    public void setAudioDataProvider(AudioDataProvider audioDataProvider) throws InterruptedException {
        stopAudioLoop();
        this.audioDataProvider = audioDataProvider;
        synchronized (audioStartSync) {
            audioStartSync.notifyAll();
        }
    }

    /**
	 * Current audio level (0 - 100%)
	 */
    double level;

    /**
	 * 
	 */
    private Object audioStopSync;

    private Object audioStartSync;

    /**
	 * 
	 * 
	 */
    Thread.State state;

    /**
	 * Start listening on construction
	 * @throws InterruptedException 
	 * @throws SecurityException 
	 */
    public AudioOutput() throws LineUnavailableException, IOException, SecurityException, InterruptedException {
        super();
        if (supportsOutput()) {
            audioStartSync = new Object();
            audioStopSync = new Object();
            setSelectedInputLine(getSupportedOutputLines().iterator().next());
            audioLoopRunning = true;
            start();
        } else {
            throw new LineUnavailableException("No suitable audio input devices found.");
        }
    }

    /**
     * Enumerate the supported output devices so that a user can choose one via a UI
     * @return
     */
    public static Collection<String> getSupportedOutputLines() {
        return getOutputLinesMap().keySet();
    }

    public static boolean supportsOutput() {
        return !getOutputLinesMap().isEmpty();
    }

    private static Map<String, SourceDataLine> getOutputLinesMap() {
        if (outputLines == null) {
            outputLines = new HashMap<String, SourceDataLine>();
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
            for (Mixer.Info mixerInfo : mixerInfos) {
                Mixer m = AudioSystem.getMixer(mixerInfo);
                if (m.isLineSupported(info)) {
                    try {
                        SourceDataLine line = (SourceDataLine) m.getLine(info);
                        outputLines.put(mixerInfo.getName(), line);
                    } catch (LineUnavailableException e) {
                    }
                }
            }
        }
        return outputLines;
    }

    /**
     * This thread acts as the internal audio loop
     */
    public void run() {
        try {
            while (true) {
                selectedOutputLine.flush();
                while (audioLoopRunning) {
                    Arrays.fill(readBuffer, (byte) 0);
                    int numBytesRead = 0;
                    if (audioDataProvider != null) {
                        numBytesRead = audioDataProvider.onDataRequested(readBuffer);
                    }
                    if (numBytesRead <= 0) {
                        stopAudioLoop = true;
                        level = 0.0;
                    } else {
                        selectedOutputLine.write(readBuffer, 0, numBytesRead);
                        computeLevel();
                    }
                    if (stopAudioLoop) {
                        audioLoopRunning = false;
                    }
                }
                synchronized (audioStopSync) {
                    audioStopSync.notifyAll();
                }
                synchronized (audioStartSync) {
                    audioStartSync.wait();
                    audioLoopRunning = true;
                    stopAudioLoop = false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Halts the inner audio loop
     * @return
     * @throws InterruptedException
     */
    private boolean stopAudioLoop() throws InterruptedException {
        if (audioLoopRunning) {
            stopAudioLoop = true;
            synchronized (audioStopSync) {
                audioStopSync.wait();
            }
        }
        if (selectedOutputLine != null) {
            selectedOutputLine.flush();
        }
        return audioLoopRunning;
    }

    private void closeCurrentLine() throws IOException, LineUnavailableException, SecurityException, InterruptedException {
        stopAudioLoop();
        if (selectedOutputLine != null) {
            selectedOutputLine.stop();
            selectedOutputLine.close();
        }
    }

    public String getSelectedInputLine() {
        return selectedOutputLineName;
    }

    public void setSelectedInputLine(String inputLine) throws IOException, LineUnavailableException, SecurityException, InterruptedException {
        closeCurrentLine();
        this.selectedOutputLineName = inputLine;
        this.selectedOutputLine = getOutputLinesMap().get(inputLine);
        selectedOutputLine.open(format, selectedOutputLine.getBufferSize());
        readBuffer = new byte[selectedOutputLine.getBufferSize()];
        selectedOutputLine.start();
        synchronized (audioStartSync) {
            audioStartSync.notifyAll();
        }
    }

    private void computeLevel() {
        double sum = 0;
        double numSamples = readBuffer.length / 2;
        for (int i = 0; i < numSamples; i++) {
            float sample = (float) ((readBuffer[2 * i] & 0xff) | (readBuffer[2 * i + 1] << 8)) / 32768.0f;
            sum += sample * sample;
        }
        level = Math.sqrt(sum / numSamples);
    }

    public double getLevel() {
        return level;
    }
}
