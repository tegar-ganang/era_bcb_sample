package org.myrobotlab.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import javaFlacEncoder.FLAC_FileEncoder;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.speech.TranscriptionThread;
import org.tritonus.share.sampled.FloatSampleBuffer;

public class GoogleSTT extends Service {

    public static final Logger LOG = Logger.getLogger(GoogleSTT.class.getCanonicalName());

    private static final long serialVersionUID = 1L;

    boolean stopCapture = false;

    ByteArrayOutputStream byteArrayOutputStream;

    AudioFormat audioFormat;

    TargetDataLine targetDataLine;

    AudioInputStream audioInputStream;

    SourceDataLine sourceDataLine;

    CaptureThread captureThread = null;

    String language = "en";

    float sampleRate = 8000.0F;

    int sampleSizeInBits = 16;

    int channels = 1;

    boolean signed = true;

    boolean bigEndian = false;

    public static final int SUCCESS = 1;

    public static final int ERROR = 2;

    public static final int TRANSCRIBING = 3;

    transient TranscriptionThread transcription = null;

    FLAC_FileEncoder encoder;

    float rms;

    float rmsThreshold = 0.0050f;

    public byte[] rawBytes;

    boolean isCapturing = false;

    long captureStartTimeMS;

    long captureTimeMinimumMS = 1200;

    long captureTimeMS;

    private FloatSampleBuffer buffer;

    private int bufferSize = 512;

    public GoogleSTT(String n) {
        super(n, GoogleSTT.class.getCanonicalName());
        encoder = new FLAC_FileEncoder();
    }

    @Override
    public void loadDefaultConfiguration() {
    }

    private AudioFormat getAudioFormat() {
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    public void captureAudio() {
        try {
            audioFormat = getAudioFormat();
            LOG.info("sample rate         " + sampleRate);
            LOG.info("channels            " + channels);
            LOG.info("sample size in bits " + sampleSizeInBits);
            LOG.info("signed              " + signed);
            LOG.info("bigEndian           " + bigEndian);
            LOG.info("data rate is " + sampleRate * sampleSizeInBits / 8 + " bytes per second");
            DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
            targetDataLine = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
            targetDataLine.open(audioFormat);
            targetDataLine.start();
            buffer = new FloatSampleBuffer(targetDataLine.getFormat().getChannels(), bufferSize, targetDataLine.getFormat().getSampleRate());
            captureThread = new CaptureThread(this);
            captureThread.start();
        } catch (Exception e) {
            LOG.error(Service.stackToString(e));
        }
    }

    public void stopAudioCapture() {
        stopCapture = true;
    }

    private Boolean isListening = true;

    public synchronized boolean setListening(boolean b) {
        isListening = b;
        isListening.notifyAll();
        return b;
    }

    /**
	 * @author grog Does the audio capturing, rms, and data copying. Should
	 *         probably be refactored into an AudioCaptureThread which could be
	 *         shared with other Services.
	 */
    class CaptureThread extends Thread {

        private Service myService = null;

        CaptureThread(Service s) {
            this(s, s.getName() + "_capture");
        }

        CaptureThread(Service s, String n) {
            super(n);
            myService = s;
        }

        public void run() {
            boolean x = true;
            int transcriptionIndex = 0;
            while (x) {
                synchronized (isListening) {
                    try {
                        while (!isListening) {
                            isListening.wait();
                        }
                    } catch (InterruptedException ex) {
                        LOG.debug("capture thread interrupted");
                        return;
                    }
                }
                int byteBufferSize = buffer.getByteArrayBufferSize(targetDataLine.getFormat());
                rawBytes = new byte[byteBufferSize];
                LOG.info("starting capture with " + bufferSize + " buffer size and " + byteBufferSize + " byte buffer length");
                byteArrayOutputStream = new ByteArrayOutputStream();
                stopCapture = false;
                try {
                    while (!stopCapture) {
                        int cnt = targetDataLine.read(rawBytes, 0, rawBytes.length);
                        buffer.setSamplesFromBytes(rawBytes, 0, targetDataLine.getFormat(), 0, buffer.getSampleCount());
                        rms = level(buffer.getChannel(0));
                        if (rms > rmsThreshold) {
                            LOG.info("rms " + rms + " will begin recording ");
                            isCapturing = true;
                            captureStartTimeMS = System.currentTimeMillis();
                        }
                        if (cnt > 0 && isCapturing) {
                            byteArrayOutputStream.write(rawBytes, 0, cnt);
                        }
                        captureTimeMS = System.currentTimeMillis() - captureStartTimeMS;
                        if (isCapturing == true && captureTimeMS > captureTimeMinimumMS && rms < rmsThreshold) {
                            isCapturing = false;
                            stopCapture = true;
                        }
                    }
                    byteArrayOutputStream.flush();
                    byteArrayOutputStream.close();
                    ++transcriptionIndex;
                    saveWavAsFile(byteArrayOutputStream.toByteArray(), audioFormat, "googletts_" + transcriptionIndex + ".wav");
                    encoder.encode(new File("googletts_" + transcriptionIndex + ".wav"), new File("googletts_" + transcriptionIndex + ".flac"));
                    transcribe("googletts_" + transcriptionIndex + ".flac");
                    stopCapture = false;
                } catch (Exception e) {
                    LOG.error(Service.stackToString(e));
                }
            }
        }
    }

    public float level(float[] samples) {
        float level = 0;
        for (int i = 0; i < samples.length; i++) {
            level += (samples[i] * samples[i]);
        }
        level /= samples.length;
        level = (float) Math.sqrt(level);
        return level;
    }

    public static int toInt(byte[] bytes) {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) - Byte.MIN_VALUE + (int) bytes[i];
        }
        return result;
    }

    private void transcribe(String path) {
        Service.logTime("t1", "start");
        Service.logTime("t1", "pre new transcription " + path);
        TranscriptionThread transcription = new TranscriptionThread(this.getName() + "_transcriber", language);
        transcription.debug = true;
        Service.logTime("t1", "pre new thread start");
        transcription.start();
        Service.logTime("t1", "pre transcription");
        transcription.startTranscription(path);
        Service.logTime("t1", "post transcription");
    }

    public static void saveWavAsFile(byte[] byte_array, AudioFormat audioFormat, String file) {
        try {
            long length = (long) (byte_array.length / audioFormat.getFrameSize());
            ByteArrayInputStream bais = new ByteArrayInputStream(byte_array);
            AudioInputStream audioInputStreamTemp = new AudioInputStream(bais, audioFormat, length);
            File fileOut = new File(file);
            AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
            if (AudioSystem.isFileTypeSupported(fileType, audioInputStreamTemp)) {
                AudioSystem.write(audioInputStreamTemp, fileType, fileOut);
            }
        } catch (Exception e) {
        }
    }

    public static double toDouble(byte[] data) {
        if (data == null || data.length != 8) return 0x0;
        return Double.longBitsToDouble(toLong(data));
    }

    public static long toLong(byte[] data) {
        if (data == null || data.length != 8) return 0x0;
        return (long) ((long) (0xff & data[0]) << 56 | (long) (0xff & data[1]) << 48 | (long) (0xff & data[2]) << 40 | (long) (0xff & data[3]) << 32 | (long) (0xff & data[4]) << 24 | (long) (0xff & data[5]) << 16 | (long) (0xff & data[6]) << 8 | (long) (0xff & data[7]) << 0);
    }

    @Override
    public String getToolTip() {
        return "Uses the Google Speech To Text service";
    }

    public static void main(String[] args) {
        org.apache.log4j.BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.DEBUG);
        GoogleSTT stt = new GoogleSTT("stt");
        stt.captureAudio();
        stt.stopAudioCapture();
    }
}
