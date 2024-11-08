package org.nees.rbnb;

import java.util.StringTokenizer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

public class AudioHelp {

    private static final AudioFormat.Encoding[] encodings = { AudioFormat.Encoding.PCM_SIGNED, AudioFormat.Encoding.PCM_UNSIGNED, AudioFormat.Encoding.ULAW, AudioFormat.Encoding.ALAW };

    private static final float[] rates = { 44100.0f, 22050.0f, 16000.0f, 11025.0f, 8000.0f };

    private static final int[] sampleSizes = { 16, 8 };

    public static AudioFormat findBestFormatForCapture() {
        boolean bigEndian = true;
        AudioFormat format;
        boolean works;
        for (int e = 0; e < encodings.length; e++) for (int r = 0; r < rates.length; r++) for (int s = 0; s < sampleSizes.length; s++) for (int channels = 2; channels > 0; channels--) {
            format = new AudioFormat(encodings[e], rates[r], sampleSizes[s], channels, (sampleSizes[s] / 8) * channels, rates[r], bigEndian);
            works = probFormatForCapture(format);
            if (works) return format;
            format = new AudioFormat(encodings[e], rates[r], sampleSizes[s], channels, (sampleSizes[s] / 8) * channels, rates[r], !bigEndian);
            works = probFormatForCapture(format);
            if (works) return format;
        }
        return null;
    }

    public static boolean probFormatForCapture(AudioFormat format) {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            return false;
        }
        try {
            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format, line.getBufferSize());
            line.close();
            return true;
        } catch (Throwable t) {
        }
        return false;
    }

    public static AudioFormat findBestFormatForPlayback() {
        boolean bigEndian = true;
        AudioFormat format;
        boolean works;
        for (int e = 0; e < encodings.length; e++) for (int r = 0; r < rates.length; r++) for (int s = 0; s < sampleSizes.length; s++) for (int channels = 2; channels > 0; channels--) {
            format = new AudioFormat(encodings[e], rates[r], sampleSizes[s], channels, (sampleSizes[s] / 8) * channels, rates[r], bigEndian);
            works = probFormatForPlayback(format);
            if (works) return format;
            format = new AudioFormat(encodings[e], rates[r], sampleSizes[s], channels, (sampleSizes[s] / 8) * channels, rates[r], !bigEndian);
            works = probFormatForPlayback(format);
            if (works) return format;
        }
        return null;
    }

    public static boolean probFormatForPlayback(AudioFormat format) {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            return false;
        }
        try {
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format, line.getBufferSize());
            line.close();
            return true;
        } catch (Throwable t) {
        }
        return false;
    }

    public class UserInfoHolder {

        public String packageName = null;

        public String content = null;

        String encodingString = null;

        String channelsString = null;

        String sampleRateString = null;

        String sampleSizeString = null;

        String frameRateString = null;

        String frameSizeString = null;

        String bigendianString = null;

        String endianString = null;

        String signedString = null;

        String startAtString = null;

        public AudioFormat.Encoding encoding = null;

        public int channels = 0;

        public float sampleRate = -1.0f;

        public int sampleSize = -1;

        public float frameRate = -1.0f;

        public int frameSize = -1;

        public boolean bigEndian = true;

        public int endian = -1;

        public int signed = -1;

        public long startAt = 0;

        public UserInfoHolder(AudioFormat selectedFormat, long time) {
            content = "audio";
            packageName = "package=javax.sound.sampled";
            encoding = selectedFormat.getEncoding();
            channels = selectedFormat.getChannels();
            sampleRate = selectedFormat.getSampleRate();
            sampleSize = selectedFormat.getSampleSizeInBits();
            frameRate = selectedFormat.getFrameRate();
            frameSize = selectedFormat.getFrameSize();
            bigEndian = selectedFormat.isBigEndian();
            endian = (selectedFormat.isBigEndian()) ? -1 : 0;
            signed = 0;
            if (selectedFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) signed = -1;
            startAt = time;
        }

        public UserInfoHolder(String in) throws Exception {
            StringTokenizer st = new StringTokenizer(in, ",");
            while (st.hasMoreElements()) {
                String element = st.nextToken();
                StringTokenizer nv = new StringTokenizer(element, "=");
                if (nv.countTokens() != 2) throw new Exception("UserInfo: Found unparsable name-value pair " + element + " in info string, " + in);
                String name = nv.nextToken().trim();
                String value = nv.nextToken().trim();
                if (name.equals("content")) content = value; else if (name.equals("package")) packageName = value; else if (name.equals("encoding")) encodingString = value; else if (name.equals("channels")) channelsString = value; else if (name.equals("samplerate")) sampleRateString = value; else if (name.equals("samplesize")) sampleSizeString = value; else if (name.equals("frameRate")) frameRateString = value; else if (name.equals("frameSize")) frameSizeString = value; else if (name.equals("bigendian")) bigendianString = value; else if (name.equals("endian")) endianString = value; else if (name.equals("signed")) signedString = value; else if (name.equals("startAt")) startAtString = value;
            }
            if ((content == null) || (!content.equals("audio"))) throw new Exception("UserInfo: only audio RBNB channels," + "content not set to audio = " + content);
            if ((packageName == null) || (!packageName.equals("javax.sound.sampled"))) throw new Exception("UserInfo: package is not avax.sound.sampled -" + " only audio format of that type can be processed now.");
            if (encodingString == null) throw new Exception("UserInfo: missing tag for encoding");
            if (channelsString == null) throw new Exception("UserInfo: missing tag for channels");
            if (sampleRateString == null) throw new Exception("UserInfo: missing tag for sample rate");
            if (sampleSizeString == null) throw new Exception("UserInfo: missing tag for sample zise");
            if (frameRateString == null) throw new Exception("UserInfo: missing tag for frame rate");
            if (frameSizeString == null) throw new Exception("UserInfo: missing tag for frame size");
            if (bigendianString == null) throw new Exception("UserInfo: missing tag for bigendian flag");
            if (encodingString.equals("PCM_SIGNED")) encoding = AudioFormat.Encoding.PCM_SIGNED; else if (encodingString.equals("PCM_UNSIGNED")) encoding = AudioFormat.Encoding.PCM_UNSIGNED; else if (encodingString.equals("ULAW")) encoding = AudioFormat.Encoding.ULAW; else if (encodingString.equals("ALAW")) encoding = AudioFormat.Encoding.ALAW; else throw new Exception("UserInfo: unrecognezed encoding = " + encodingString);
            try {
                channels = Integer.parseInt(channelsString);
            } catch (Throwable t) {
                throw new Exception("UserInfo: can not parse channels = " + channelsString);
            }
            try {
                sampleSize = Integer.parseInt(sampleSizeString);
            } catch (Throwable t) {
                throw new Exception("UserInfo: can not parse sample size = " + sampleSizeString);
            }
            try {
                frameSize = Integer.parseInt(frameSizeString);
            } catch (Throwable t) {
                throw new Exception("UserInfo: can not frame size = " + frameSizeString);
            }
            try {
                sampleRate = Float.parseFloat(sampleRateString);
            } catch (Throwable t) {
                throw new Exception("UserInfo: can not frame size = " + sampleRateString);
            }
            try {
                frameRate = Float.parseFloat(frameRateString);
            } catch (Throwable t) {
                throw new Exception("UserInfo: can not frame size = " + frameRateString);
            }
            try {
                bigEndian = Boolean.parseBoolean(bigendianString);
            } catch (Throwable t) {
                throw new Exception("UserInfo: can not frame size = " + frameRateString);
            }
            if (endianString != null) try {
                endian = Integer.parseInt(endianString);
            } catch (Throwable ignore) {
            }
            if (signedString != null) try {
                signed = Integer.parseInt(signedString);
            } catch (Throwable ignore) {
            }
            if (startAtString != null) try {
                startAt = Long.parseLong(startAtString);
            } catch (Throwable ignore) {
            }
        }

        public String toString() {
            return "content=audio" + ",package=javax.sound.sampled" + ",encoding=" + encoding + ",channels=" + channels + ",samplerate=" + sampleRate + ",samplesize=" + sampleSize + ",frameRate=" + frameRate + ",frameSize=" + frameSize + ",bigendian=" + bigEndian + ",endian=" + endian + ",signed=" + signed + ",startAt=" + startAt;
        }
    }
}
