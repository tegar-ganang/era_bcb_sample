package org.jsresources.apps.jam.audio;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.jsresources.apps.jam.Debug;

public class AudioUtils {

    private static final int EXTERNAL_BUFFER_SIZE = 128000;

    /** Contructor to prevent instantiation.
	 */
    private AudioUtils() {
    }

    /** Reads the whole audio file into a byte array.
	    Before, the audio data are converted to the given audioFormat.
	*/
    public static byte[] getByteArrayFromFile(File file, AudioFormat audioFormat) throws Exception {
        AudioInputStream audioInputStream = getAudioInputStream(file);
        return getByteArrayFromAIS(audioInputStream, audioFormat);
    }

    public static byte[] getByteArrayFromURL(URL url, AudioFormat audioFormat) throws Exception {
        AudioInputStream audioInputStream = getAudioInputStream(url);
        return getByteArrayFromAIS(audioInputStream, audioFormat);
    }

    public static AudioInputStream getConvertedStream(AudioInputStream ais, AudioFormat newFormat) throws Exception {
        AudioFormat targetFormat = newFormat;
        AudioFormat sourceFormat = ais.getFormat();
        if (AudioSystem.isConversionSupported(targetFormat, sourceFormat)) {
            ais = AudioSystem.getAudioInputStream(targetFormat, ais);
        } else {
            if (!isPcm(sourceFormat.getEncoding())) {
                AudioFormat intermediateFormat = new AudioFormat(sourceFormat.getSampleRate(), 16, sourceFormat.getChannels(), true, false);
                boolean bSupported = AudioSystem.isConversionSupported(intermediateFormat, sourceFormat);
                if (bSupported) {
                    ais = AudioSystem.getAudioInputStream(intermediateFormat, ais);
                    sourceFormat = intermediateFormat;
                }
            }
            if (isPcm(sourceFormat.getEncoding()) && targetFormat.getSampleRate() != sourceFormat.getSampleRate()) {
                AudioFormat intermediateFormat = new AudioFormat(sourceFormat.getEncoding(), targetFormat.getSampleRate(), sourceFormat.getSampleSizeInBits(), sourceFormat.getChannels(), sourceFormat.getFrameSize(), targetFormat.getFrameRate(), sourceFormat.isBigEndian());
                boolean bSupported1 = AudioSystem.isConversionSupported(intermediateFormat, sourceFormat);
                boolean bSupported2 = AudioSystem.isConversionSupported(targetFormat, intermediateFormat);
                if (bSupported1 && bSupported2) {
                    ais = AudioSystem.getAudioInputStream(intermediateFormat, ais);
                    ais = AudioSystem.getAudioInputStream(targetFormat, ais);
                } else {
                    Debug.out("AudioUtils.getConvertedStream(): conversion not supported:");
                    Debug.out("AudioUtils.getConvertedStream(): source: " + sourceFormat);
                    Debug.out("AudioUtils.getConvertedStream(): conversion supported: " + bSupported1);
                    Debug.out("AudioUtils.getConvertedStream(): intermediate: " + intermediateFormat);
                    Debug.out("AudioUtils.getConvertedStream(): conversion supported: " + bSupported2);
                    Debug.out("AudioUtils.getConvertedStream(): target: " + targetFormat);
                }
                throw new Exception("Audio file cannot be converted!");
            } else {
                Debug.out("AudioUtils.getConvertedStream(): conversion not supported:");
                Debug.out("AudioUtils.getConvertedStream(): source:" + sourceFormat);
                Debug.out("AudioUtils.getConvertedStream(): target:" + targetFormat);
                throw new Exception("Audio file cannot be converted!");
            }
        }
        return ais;
    }

    public static byte[] getByteArrayFromAIS(AudioInputStream audioInputStream, AudioFormat audioFormat) throws Exception {
        audioInputStream = getConvertedStream(audioInputStream, audioFormat);
        BAOS baos = new BAOS();
        int nBytesRead = 0;
        byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];
        while (nBytesRead != -1) {
            try {
                nBytesRead = audioInputStream.read(abData, 0, abData.length);
            } catch (IOException e) {
                if (Debug.getTraceAllExceptions()) {
                    Debug.out(e);
                }
            }
            if (nBytesRead >= 0) {
                baos.write(abData, 0, nBytesRead);
            }
        }
        return baos.getInternalBuffer();
    }

    private static boolean isPcm(AudioFormat.Encoding encoding) {
        return encoding == AudioFormat.Encoding.PCM_SIGNED || encoding == AudioFormat.Encoding.PCM_UNSIGNED;
    }

    /**
	   If not already mono, always takes the first channel only.
	   Returns 16 bit signed samples as ints.
	 */
    public static int[] getMonoSamplesFromBytes(byte[] abData, AudioFormat audioFormat) {
        int nNumFrames = abData.length / audioFormat.getFrameSize();
        int[] anSamples = new int[nNumFrames];
        for (int i = 0; i < nNumFrames; i++) {
            int nByteOffset = i * audioFormat.getFrameSize();
            switch(audioFormat.getSampleSizeInBits()) {
                case 8:
                    if (audioFormat.getEncoding() == AudioFormat.Encoding.PCM_SIGNED) {
                        anSamples[i] = abData[nByteOffset] * 256;
                    } else {
                        anSamples[i] = ((abData[nByteOffset] & 0xFF) - 128) * 256;
                    }
                    break;
                case 16:
                    if (audioFormat.isBigEndian()) {
                        anSamples[i] = (abData[nByteOffset] << 8) | (abData[nByteOffset + 1] & 0xFF);
                    } else {
                        anSamples[i] = (abData[nByteOffset + 1] << 8) | (abData[nByteOffset] & 0xFF);
                    }
            }
        }
        return anSamples;
    }

    public static boolean isSoundFile(File file) {
        if (Debug.getTraceAudio()) {
            Debug.out("AudioUtils.isSoundFile(): begin");
        }
        AudioInputStream audioInputStream = getAudioInputStream(file);
        if (audioInputStream != null) {
            try {
                audioInputStream.close();
            } catch (IOException e) {
                if (Debug.getTraceAllExceptions()) {
                    Debug.out(e);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public static AudioFormat getAudioFormat(String sFilename) {
        if (Debug.getTraceAudio()) {
            Debug.out("AudioUtils.getAudioFormat(\"" + sFilename + "\"): called.");
        }
        AudioInputStream audioInputStream = getAudioInputStream(sFilename);
        AudioFormat audioFormat = null;
        if (audioInputStream != null) {
            audioFormat = audioInputStream.getFormat();
            try {
                audioInputStream.close();
            } catch (IOException e) {
                if (Debug.getTraceAllExceptions()) {
                    Debug.out(e);
                }
            }
        }
        return audioFormat;
    }

    public static String getAudioFormatString(String sFilename) {
        if (Debug.getTraceAudio()) {
            Debug.out("org.jsresources.apps.jam.audio.AudioUtils.getAudioFormatString(\"" + sFilename + "\"): called.");
        }
        AudioFormat audioformat = getAudioFormat(sFilename);
        if (audioformat != null) {
            return audioformat.toString();
        } else {
            return null;
        }
    }

    public static long getLength(String sFilename) {
        if (Debug.getTraceAudio()) {
            Debug.out("org.jsresources.apps.jam.audio.AudioUtils.getLength(\"" + sFilename + "\"): called.");
        }
        AudioInputStream audioInputStream = getAudioInputStream(sFilename);
        long lLength = -1;
        if (audioInputStream != null) {
            AudioFormat format = audioInputStream.getFormat();
            long nFrames = audioInputStream.getFrameLength();
            lLength = (long) ((nFrames * 1000) / format.getSampleRate());
            try {
                audioInputStream.close();
            } catch (IOException e) {
                if (Debug.getTraceAllExceptions()) {
                    Debug.out(e);
                }
            }
        }
        return lLength;
    }

    private static AudioInputStream getAudioInputStream(String strFilename) {
        File file = new File(strFilename);
        return getAudioInputStream(file);
    }

    private static AudioInputStream getAudioInputStream(File file) {
        AudioInputStream audioInputStream = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(file);
        } catch (UnsupportedAudioFileException e) {
            if (Debug.getTraceAllExceptions()) {
                Debug.out(e);
            }
        } catch (IOException e) {
            if (Debug.getTraceAllExceptions()) {
                Debug.out(e);
            }
        }
        return audioInputStream;
    }

    private static AudioInputStream getAudioInputStream(URL url) {
        AudioInputStream audioInputStream = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(url);
        } catch (UnsupportedAudioFileException e) {
            if (Debug.getTraceAllExceptions()) {
                Debug.out(e);
            }
        } catch (IOException e) {
            if (Debug.getTraceAllExceptions()) {
                Debug.out(e);
            }
        }
        return audioInputStream;
    }

    public static String formatNumber(long i, int digits) {
        return formatNumberImpl(i, digits, "0");
    }

    public static String formatNumberImpl(long i, int digits, String fill) {
        int ten = 10;
        String res = "";
        while (digits > 1 && fill != null) {
            if (i < ten) {
                res += fill;
            }
            digits--;
            ten *= 10;
        }
        return res + String.valueOf(i);
    }

    public static String formatMinSec(long millis) {
        if (millis < 0) {
            return " error";
        } else {
            return formatNumberImpl(millis / 60000, 3, " ") + ":" + formatNumber((millis % 60000) / 1000, 2);
        }
    }

    public static String formatSecMillis(long millis) {
        if (millis < 0) {
            return " error";
        } else {
            return formatNumberImpl(millis / 1000, 8, null) + ":" + formatNumber(millis % 1000, 3);
        }
    }

    public static String formatBarsBeats(long millis, double tempo, int beatsPerMeasure) {
        if (beatsPerMeasure <= 0 || tempo <= 0) return "naN";
        int barDurationMillis = (int) (60000.0 / tempo * beatsPerMeasure);
        int beatDurationMillis = (int) (60000.0 / tempo);
        String sign = "";
        if (millis < 0) {
            millis = -millis;
            sign = "-";
        }
        long bar = (millis / barDurationMillis) + 1;
        millis -= (bar - 1) * barDurationMillis;
        int beat = (int) (millis / beatDurationMillis) + 1;
        int tenths = (int) ((millis % beatDurationMillis) * 10 / beatDurationMillis);
        return sign + bar + ":" + beat + "." + tenths;
    }

    private static final Class[] EMPTY_CLASS_ARRAY = new Class[0];

    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    private static boolean sm_bTestedForSunMiscPerf = false;

    private static boolean sm_bUseSunMiscPerf = false;

    private static Object sm_sunMiscPerfObject;

    private static long sm_lSunMiscPerfFrequency;

    private static Method sm_sunMiscPerfHighResCounterMethod;

    public static long getCurrentTime() {
        if (!sm_bTestedForSunMiscPerf) {
            testForSunMiscPerf();
        }
        if (sm_bUseSunMiscPerf) {
            try {
                long lCounter = ((Long) sm_sunMiscPerfHighResCounterMethod.invoke(sm_sunMiscPerfObject, EMPTY_OBJECT_ARRAY)).longValue();
                return lCounter * 1000 / sm_lSunMiscPerfFrequency;
            } catch (Exception e) {
                return -1;
            }
        } else {
            return System.currentTimeMillis();
        }
    }

    private static void testForSunMiscPerf() {
        sm_bTestedForSunMiscPerf = true;
        sm_bUseSunMiscPerf = false;
        Class sunMiscPerfClass = null;
        try {
            sunMiscPerfClass = Class.forName("sun.misc.Perf");
        } catch (ClassNotFoundException e) {
            return;
        }
        try {
            Method method = null;
            method = sunMiscPerfClass.getMethod("getPerf", EMPTY_CLASS_ARRAY);
            sm_sunMiscPerfObject = method.invoke(null, EMPTY_OBJECT_ARRAY);
            method = sunMiscPerfClass.getMethod("highResFrequency", EMPTY_CLASS_ARRAY);
            sm_lSunMiscPerfFrequency = ((Long) method.invoke(sm_sunMiscPerfObject, EMPTY_OBJECT_ARRAY)).longValue();
            sm_sunMiscPerfHighResCounterMethod = sunMiscPerfClass.getMethod("highResCounter", EMPTY_CLASS_ARRAY);
        } catch (Exception e) {
            return;
        }
        sm_bUseSunMiscPerf = true;
    }

    public static long millis2samples(long millis, float sampleRate) {
        return (long) ((double) millis * (double) sampleRate / 1000.0);
    }

    public static long samples2millis(long samples, float sampleRate) {
        return (long) ((double) samples * 1000.0 / (double) sampleRate);
    }

    public static class BAOS extends ByteArrayOutputStream {

        public BAOS() {
            super();
        }

        public byte[] getInternalBuffer() {
            return buf;
        }
    }
}
