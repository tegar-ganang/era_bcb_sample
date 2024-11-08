package org.wcb.plugins.speech;

import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.util.StringTokenizer;
import java.net.URL;

public class Talker {

    private SourceDataLine line = null;

    /**
      * This method speaks a phonetic word specified on the command line.
      */
    public static void main(String args[]) {
        Talker player = new Talker();
        if (args.length > 0) player.sayPhoneWord(args[0]);
        System.exit(0);
    }

    /**
      * This method speaks the given phonetic word.
      */
    public void sayPhoneWord(String word) {
        byte[] previousSound = null;
        StringTokenizer st = new StringTokenizer(word, "|", false);
        while (st.hasMoreTokens()) {
            String thisPhoneFile = st.nextToken();
            thisPhoneFile = "/allophones/" + thisPhoneFile + ".au";
            byte[] thisSound = getSound(thisPhoneFile);
            if (previousSound != null) {
                int mergeCount = 0;
                if (previousSound.length >= 500 && thisSound.length >= 500) mergeCount = 500;
                for (int i = 0; i < mergeCount; i++) {
                    previousSound[previousSound.length - mergeCount + i] = (byte) ((previousSound[previousSound.length - mergeCount + i] + thisSound[i]) / 2);
                }
                playSound(previousSound);
                byte[] newSound = new byte[thisSound.length - mergeCount];
                for (int ii = 0; ii < newSound.length; ii++) newSound[ii] = thisSound[ii + mergeCount];
                previousSound = newSound;
            } else previousSound = thisSound;
        }
        playSound(previousSound);
        drain();
    }

    /**
      * This method plays a sound sample.
      */
    private void playSound(byte[] data) {
        if (data.length > 0) line.write(data, 0, data.length);
    }

    /**
 * This method flushes the sound channel.
 */
    private void drain() {
        if (line != null) line.drain();
        try {
            Thread.sleep(100);
        } catch (Exception e) {
        }
    }

    /**
 * This method reads the file for a single allophone and
 * constructs a byte vector.
 */
    private byte[] getSound(String fileName) {
        try {
            URL url = Talker.class.getResource(fileName);
            AudioInputStream stream = AudioSystem.getAudioInputStream(url);
            AudioFormat format = stream.getFormat();
            if ((format.getEncoding() == AudioFormat.Encoding.ULAW) || (format.getEncoding() == AudioFormat.Encoding.ALAW)) {
                AudioFormat tmpFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), format.getSampleSizeInBits() * 2, format.getChannels(), format.getFrameSize() * 2, format.getFrameRate(), true);
                stream = AudioSystem.getAudioInputStream(tmpFormat, stream);
                format = tmpFormat;
            }
            DataLine.Info info = new DataLine.Info(Clip.class, format, ((int) stream.getFrameLength() * format.getFrameSize()));
            if (line == null) {
                DataLine.Info outInfo = new DataLine.Info(SourceDataLine.class, format);
                if (!AudioSystem.isLineSupported(outInfo)) {
                    System.out.println("Line matching " + outInfo + " not supported.");
                    throw new Exception("Line matching " + outInfo + " not supported.");
                }
                line = (SourceDataLine) AudioSystem.getLine(outInfo);
                line.open(format, 50000);
                line.start();
            }
            int frameSizeInBytes = format.getFrameSize();
            int bufferLengthInFrames = line.getBufferSize() / 8;
            int bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
            byte[] data = new byte[bufferLengthInBytes];
            int numBytesRead = 0;
            if ((numBytesRead = stream.read(data)) != -1) {
                int numBytesRemaining = numBytesRead;
            }
            byte[] newData = new byte[numBytesRead];
            for (int i = 0; i < numBytesRead; i++) newData[i] = data[i];
            return newData;
        } catch (Exception e) {
            return new byte[0];
        }
    }
}
