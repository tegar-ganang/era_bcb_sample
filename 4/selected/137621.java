package decode;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.ICodec;

/**
 * Takes a media container, finds the first audio stream,
 * decodes that stream, and then plays
 * it on the default system device.
 * 
 * @author aclarke
 * @author Petri Tuononen
 *
 */
public class DecodeAudio {

    /**
	 * The audio line we'll output sound to; it'll be the default audio device on your system if available
	 */
    private static SourceDataLine mLine;

    /**
	 * Takes a media container (file), opens it,
	 * opens up the default audio device on your system,
	 * and plays back the audio.
	 *  
	 * @param filename String which represents a filename
	 */
    public DecodeAudio(String filename) {
        IContainer container = IContainer.make();
        if (container.open(filename, IContainer.Type.READ, null) < 0) throw new IllegalArgumentException("could not open file: " + filename);
        int numStreams = container.getNumStreams();
        int audioStreamId = -1;
        IStreamCoder audioCoder = null;
        for (int i = 0; i < numStreams; i++) {
            IStream stream = container.getStream(i);
            IStreamCoder coder = stream.getStreamCoder();
            if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
                audioStreamId = i;
                audioCoder = coder;
                break;
            }
        }
        if (audioStreamId == -1) throw new RuntimeException("could not find audio stream in container: " + filename);
        if (audioCoder.open() < 0) throw new RuntimeException("could not open audio decoder for container: " + filename);
        openJavaSound(audioCoder);
        IPacket packet = IPacket.make();
        while (container.readNextPacket(packet) >= 0) {
            if (packet.getStreamIndex() == audioStreamId) {
                IAudioSamples samples = IAudioSamples.make(1024, audioCoder.getChannels());
                int offset = 0;
                while (offset < packet.getSize()) {
                    int bytesDecoded = audioCoder.decodeAudio(samples, packet, offset);
                    if (bytesDecoded < 0) throw new RuntimeException("got error decoding audio in: " + filename);
                    offset += bytesDecoded;
                    if (samples.isComplete()) {
                        playJavaSound(samples);
                    }
                }
            } else {
                do {
                } while (false);
            }
        }
        closeJavaSound();
        if (audioCoder != null) {
            audioCoder.close();
            audioCoder = null;
        }
        if (container != null) {
            container.close();
            container = null;
        }
    }

    private static void openJavaSound(IStreamCoder aAudioCoder) {
        AudioFormat audioFormat = new AudioFormat(aAudioCoder.getSampleRate(), (int) IAudioSamples.findSampleBitDepth(aAudioCoder.getSampleFormat()), aAudioCoder.getChannels(), true, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        try {
            mLine = (SourceDataLine) AudioSystem.getLine(info);
            mLine.open(audioFormat);
            mLine.start();
        } catch (LineUnavailableException e) {
            throw new RuntimeException("could not open audio line");
        }
    }

    private static void playJavaSound(IAudioSamples aSamples) {
        byte[] rawBytes = aSamples.getData().getByteArray(0, aSamples.getSize());
        mLine.write(rawBytes, 0, aSamples.getSize());
    }

    private static void closeJavaSound() {
        if (mLine != null) {
            mLine.drain();
            mLine.close();
            mLine = null;
        }
    }
}
