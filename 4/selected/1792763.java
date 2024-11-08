package decode;

import gui.VideoWindow;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import com.xuggle.xuggler.Global;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.Utils;

/**
 * Takes a media container, finds the first video stream,
 * decodes that stream, and then plays the audio and video.
 *
 * This code does a VERY coarse job of matching time-stamps, and thus
 * the audio and video will float in and out of slight sync.  Getting
 * time-stamps syncing-up with audio is very system dependent and left
 * as an exercise for the reader.
 * 
 * @author aclarke
 * @author Petri Tuononen
 *
 * TODO: Sync audio and video better.
 */
public class DecodeAudioAndVideo {

    /**
	 * The audio line we'll output sound to.
	 * It'll be the default audio device on your system if available
	 */
    private static SourceDataLine mLine;

    private static VideoWindow mScreen = null;

    private static long mSystemVideoClockStartTime;

    private static long mFirstVideoTimestampInStream;

    public static void main(String[] args) {
        new DecodeAudioAndVideo("C:\\Users\\Pepe\\Desktop\\test.mp4");
    }

    /**
	 * Takes a media container (file), opens it,
	 * plays audio as quickly as it can, and opens up a Swing window and displays
	 * video frames with <i>roughly</i> the right timing.
	 *  
	 * @param filename Must contain one string which represents a filename
	 */
    @SuppressWarnings("deprecation")
    public DecodeAudioAndVideo(String filename) {
        if (!IVideoResampler.isSupported(IVideoResampler.Feature.FEATURE_COLORSPACECONVERSION)) throw new RuntimeException("You must install the GPL version of Xuggler (with IVideoResampler support) for this demo to work");
        IContainer container = IContainer.make();
        if (container.open(filename, IContainer.Type.READ, null) < 0) throw new IllegalArgumentException("Could not open file: " + filename);
        int numStreams = container.getNumStreams();
        int videoStreamId = -1;
        IStreamCoder videoCoder = null;
        int audioStreamId = -1;
        IStreamCoder audioCoder = null;
        for (int i = 0; i < numStreams; i++) {
            IStream stream = container.getStream(i);
            IStreamCoder coder = stream.getStreamCoder();
            if (videoStreamId == -1 && coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
                videoStreamId = i;
                videoCoder = coder;
            } else if (audioStreamId == -1 && coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
                audioStreamId = i;
                audioCoder = coder;
            }
        }
        if (videoStreamId == -1 && audioStreamId == -1) throw new RuntimeException("Could not find audio or video stream in container: " + filename);
        IVideoResampler resampler = null;
        if (videoCoder != null) {
            if (videoCoder.open() < 0) throw new RuntimeException("Could not open audio decoder for container: " + filename);
            if (videoCoder.getPixelType() != IPixelFormat.Type.BGR24) {
                resampler = IVideoResampler.make(videoCoder.getWidth(), videoCoder.getHeight(), IPixelFormat.Type.BGR24, videoCoder.getWidth(), videoCoder.getHeight(), videoCoder.getPixelType());
                if (resampler == null) throw new RuntimeException("Could not create color space resampler for: " + filename);
            }
            openJavaVideo();
        }
        if (audioCoder != null) {
            if (audioCoder.open() < 0) throw new RuntimeException("Could not open audio decoder for container: " + filename);
            try {
                openJavaSound(audioCoder);
            } catch (LineUnavailableException ex) {
                throw new RuntimeException("Unable to open sound device on your system when playing back container: " + filename);
            }
        }
        IPacket packet = IPacket.make();
        mFirstVideoTimestampInStream = Global.NO_PTS;
        mSystemVideoClockStartTime = 0;
        while (container.readNextPacket(packet) >= 0) {
            if (packet.getStreamIndex() == videoStreamId) {
                IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(), videoCoder.getWidth(), videoCoder.getHeight());
                int bytesDecoded = videoCoder.decodeVideo(picture, packet, 0);
                if (bytesDecoded < 0) throw new RuntimeException("Got error decoding audio in: " + filename);
                if (picture.isComplete()) {
                    IVideoPicture newPic = picture;
                    if (resampler != null) {
                        newPic = IVideoPicture.make(resampler.getOutputPixelFormat(), picture.getWidth(), picture.getHeight());
                        if (resampler.resample(newPic, picture) < 0) throw new RuntimeException("Could not resample video from: " + filename);
                    }
                    if (newPic.getPixelType() != IPixelFormat.Type.BGR24) throw new RuntimeException("Could not decode video as BGR 24 bit data in: " + filename);
                    long delay = millisecondsUntilTimeToDisplay(newPic);
                    try {
                        if (delay > 0) Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        return;
                    }
                    mScreen.setImage(Utils.videoPictureToImage(newPic));
                }
            } else if (packet.getStreamIndex() == audioStreamId) {
                IAudioSamples samples = IAudioSamples.make(1024, audioCoder.getChannels());
                int offset = 0;
                while (offset < packet.getSize()) {
                    int bytesDecoded = audioCoder.decodeAudio(samples, packet, offset);
                    if (bytesDecoded < 0) throw new RuntimeException("Got error decoding audio in: " + filename);
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
        if (videoCoder != null) {
            videoCoder.close();
            videoCoder = null;
        }
        if (audioCoder != null) {
            audioCoder.close();
            audioCoder = null;
        }
        if (container != null) {
            container.close();
            container = null;
        }
        closeJavaSound();
        closeJavaVideo();
    }

    private static long millisecondsUntilTimeToDisplay(IVideoPicture picture) {
        long millisecondsToSleep = 0;
        if (mFirstVideoTimestampInStream == Global.NO_PTS) {
            mFirstVideoTimestampInStream = picture.getTimeStamp();
            mSystemVideoClockStartTime = System.currentTimeMillis();
            millisecondsToSleep = 0;
        } else {
            long systemClockCurrentTime = System.currentTimeMillis();
            long millisecondsClockTimeSinceStartofVideo = systemClockCurrentTime - mSystemVideoClockStartTime;
            long millisecondsStreamTimeSinceStartOfVideo = (picture.getTimeStamp() - mFirstVideoTimestampInStream) / 1000;
            final long millisecondsTolerance = 50;
            millisecondsToSleep = (millisecondsStreamTimeSinceStartOfVideo - (millisecondsClockTimeSinceStartofVideo + millisecondsTolerance));
        }
        return millisecondsToSleep;
    }

    /**
	 * Opens a Swing window on screen.
	 */
    private static void openJavaVideo() {
        mScreen = new VideoWindow();
    }

    /**
	 * Forces the swing thread to terminate; I'm sure there is a right
	 * way to do this in swing, but this works too.
	 */
    private static void closeJavaVideo() {
        System.exit(0);
    }

    private static void openJavaSound(IStreamCoder aAudioCoder) throws LineUnavailableException {
        AudioFormat audioFormat = new AudioFormat(aAudioCoder.getSampleRate(), (int) IAudioSamples.findSampleBitDepth(aAudioCoder.getSampleFormat()), aAudioCoder.getChannels(), true, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        mLine = (SourceDataLine) AudioSystem.getLine(info);
        mLine.open(audioFormat);
        mLine.start();
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
