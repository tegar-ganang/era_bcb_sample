package javax.media.ding3d.audioengines.javasound;

import java.applet.*;
import java.util.*;
import java.lang.String;
import java.net.*;
import java.io.*;
import java.io.InputStream;
import javax.sound.sampled.*;

/**
 * The JSClip Class defines an audio output methods that call JavaSound
 * Hae mixer methods.
 */
class JSClip extends JSChannel {

    Clip line;

    Clip otherChannel = null;

    Clip reverbChannel = null;

    /**
     * Create data line for outputting audio input stream.
     * for a stream that is a sourceDataline
     * @return true is successful in initiallizing DataLine
     */
    DataLine initDataLine(AudioInputStream ais) {
        if (debugFlag) debugPrintln("JSClip: initDataLine(" + ais + ")");
        try {
            if (debugFlag) debugPrintln("JSClip: loadSample - try getting new line ");
            audioFormat = ais.getFormat();
            if ((audioFormat.getEncoding() == AudioFormat.Encoding.ULAW) || (audioFormat.getEncoding() == AudioFormat.Encoding.ALAW)) {
                AudioFormat tmp = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, audioFormat.getSampleRate(), audioFormat.getSampleSizeInBits() * 2, audioFormat.getChannels(), audioFormat.getFrameSize() * 2, audioFormat.getFrameRate(), true);
                ais = AudioSystem.getAudioInputStream(tmp, ais);
                audioFormat = tmp;
            }
            DataLine.Info info = new DataLine.Info(Clip.class, audioFormat);
            line = (Clip) AudioSystem.getLine(info);
            if (debugFlag) debugPrintln("JSClip: open sound Clip");
            line.open(ais);
        } catch (Exception e) {
            if (debugFlag) {
                debugPrint("JSClip: Internal Error loadSample ");
                debugPrintln("get stream failed");
            }
            e.printStackTrace();
            return null;
        }
        return (DataLine) line;
    }

    /**
      * Start TWO Samples
      *
      * used when two samples are associated with a single Point or Cone
      * sound.  This method handles starting both samples, rather than
      * forcing the caller to make two calls to startSample, so that the
      * actual Java Sound start methods called are as immediate (without
      * delay between as possible.
      */
    boolean startSamples(int loopCount, float leftGain, float rightGain, int leftDelay, int rightDelay) {
        if (debugFlag) {
            debugPrint("JSClip: startSamples ");
            debugPrintln("start stream for Left called with ");
            debugPrintln("       gain = " + leftGain + " delay = " + leftDelay);
            debugPrintln("start stream for Right called with ");
            debugPrintln("       gain = " + rightGain + " delay = " + rightDelay);
        }
        if (otherChannel == null || reverbChannel == null) startSample(loopCount, leftGain, leftDelay);
        if (ais == null) {
            if (debugFlag) {
                debugPrint("JSClip: Internal Error startSamples: ");
                debugPrintln("either left or right ais is null");
            }
            return false;
        }
        Clip leftLine;
        Clip rightLine;
        leftLine = line;
        rightLine = otherChannel;
        double ZERO_EPS = 0.0039;
        double leftVolume = (double) leftGain;
        double rightVolume = (double) rightGain;
        startTime = System.currentTimeMillis();
        if (debugFlag) debugPrintln("*****start Stream with new start time " + startTime);
        try {
            line.setLoopPoints(0, -1);
            line.loop(loopCount);
            line.start();
        } catch (Exception e) {
            if (debugFlag) {
                debugPrint("JSClip: startSamples ");
                debugPrintln("audioInputStream.read failed");
            }
            e.printStackTrace();
            startTime = 0;
            return false;
        }
        if (debugFlag) debugPrintln("JSClip: startSamples returns");
        return true;
    }

    boolean startSample(int loopCount, float gain, int delay) {
        if (debugFlag) debugPrintln("JSClip.startSample(): starting sound Clip");
        line.setFramePosition(0);
        line.setLoopPoints(0, -1);
        line.loop(loopCount);
        line.start();
        return true;
    }

    int stopSample() {
        if (debugFlag) debugPrintln("JSClip.stopSample(): stopping sound Clip");
        line.stop();
        startTime = 0;
        return 0;
    }

    int stopSamples() {
        if (debugFlag) debugPrintln("JSClip.stopSample(): stopping sound Clip");
        line.stop();
        startTime = 0;
        return 0;
    }

    public void update(LineEvent event) {
        if (event.getType().equals(LineEvent.Type.STOP)) {
            line.close();
        } else if (event.getType().equals(LineEvent.Type.CLOSE)) {
            if (debugFlag) debugPrint("JSClip.update(CLOSE) entered ");
        }
    }
}
