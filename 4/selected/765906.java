package org.ethontos.owlwatcher.view.data.xugglerViewLib;

import org.apache.log4j.Logger;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;

public class ProcessedPacketFactory {

    private final int videoIndex;

    private final int audioIndex;

    private final int videoWidth;

    private final int videoHeight;

    private final String source;

    private final IStreamCoder videoCoder;

    private IVideoResampler resampler = null;

    private final IStreamCoder audioCoder;

    private boolean videoLeftOver = false;

    private IPacket leftOverVideoPacket = null;

    private int leftOverVideoOffset = -1;

    private boolean audioLeftOver = false;

    private IPacket leftOverAudioPacket = null;

    private int leftOverAudioOffset = -1;

    static Logger logger = Logger.getLogger(ProcessedPacket.class.getName());

    public ProcessedPacketFactory(int p_videoIndex, int p_audioIndex, IStreamCoder p_audioCoder, IStreamCoder p_videoCoder, String p_source) {
        videoIndex = p_videoIndex;
        audioIndex = p_audioIndex;
        source = p_source;
        audioCoder = p_audioCoder;
        videoCoder = p_videoCoder;
        videoWidth = videoCoder.getWidth();
        videoHeight = videoCoder.getHeight();
        if (videoCoder.getPixelType() != IPixelFormat.Type.BGR24) {
            resampler = IVideoResampler.make(videoWidth, videoHeight, IPixelFormat.Type.BGR24, videoWidth, videoHeight, videoCoder.getPixelType());
            if (resampler == null) throw new RuntimeException("could not create color space resampler for: " + source);
        }
    }

    public ProcessedPacket makePacket(IPacket rawPacket) {
        ProcessedPacketImpl result = new ProcessedPacketImpl(rawPacket);
        return (ProcessedPacket) result;
    }

    public boolean hasLeftOverVideoData() {
        return videoLeftOver;
    }

    public boolean hasLeftOverAudioData() {
        return audioLeftOver;
    }

    public ProcessedPacket makePacketFromLeftOvers() {
        return null;
    }

    public ProcessedPacket makePacketBridgingLeftOvers(IPacket rawPacket) {
        return makePacket(rawPacket);
    }

    class ProcessedPacketImpl implements ProcessedPacket {

        ProcessedPacket.Type myType = Type.UNDEFINED;

        long timeStamp;

        private IAudioSamples samples;

        private IVideoPicture pic;

        ProcessedPacketImpl(IPacket rawPacket) {
            if (rawPacket.getStreamIndex() == videoIndex) {
                myType = Type.VIDEO;
                IVideoPicture rawPicture = IVideoPicture.make(videoCoder.getPixelType(), videoWidth, videoHeight);
                int offset = 0;
                while (offset < rawPacket.getSize()) {
                    int bytesDecoded = videoCoder.decodeVideo(rawPicture, rawPacket, offset);
                    if (bytesDecoded < 0) throw new RuntimeException("got error decoding video in: " + source);
                    offset += bytesDecoded;
                    if (rawPicture.isComplete()) {
                        pic = rawPicture;
                        if (resampler != null) {
                            pic = IVideoPicture.make(resampler.getOutputPixelFormat(), rawPicture.getWidth(), rawPicture.getHeight());
                            if (resampler.resample(pic, rawPicture) < 0) throw new RuntimeException("could not resample video from: " + source);
                        }
                        if (pic.getPixelType() != IPixelFormat.Type.BGR24) throw new RuntimeException("could not decode video" + " as BGR 24 bit data in: " + source);
                        timeStamp = pic.getTimeStamp();
                        timeStamp = rawPicture.getTimeStamp();
                        timeStamp = rawPicture.getPts();
                        if (!pic.isComplete()) logger.warn("resampled picture is not complete");
                    } else {
                        logger.warn("rawPicture is not complete");
                    }
                }
            } else if (rawPacket.getStreamIndex() == audioIndex) {
                myType = Type.AUDIO;
                samples = IAudioSamples.make(1024, audioCoder.getChannels());
                int offset = 0;
                while (offset < rawPacket.getSize()) {
                    int bytesDecoded = audioCoder.decodeAudio(samples, rawPacket, offset);
                    if (bytesDecoded < 0) throw new RuntimeException("could not decode audio");
                    offset += bytesDecoded;
                    if (samples.isComplete()) {
                        timeStamp = samples.getTimeStamp();
                    }
                }
            }
        }

        public IVideoPicture getPicture() {
            return pic;
        }

        /**
         * Remove the reference to pic(ture), so it can be safely deleted
         */
        public void clearPicture() {
            if (pic != null) pic.delete();
            pic = null;
        }

        public IAudioSamples getSamples() {
            return samples;
        }

        /**
         * Remove the reference to samples, so it can be safely deleted
         */
        public void clearSamples() {
            samples.delete();
            samples = null;
        }

        public boolean isAudio() {
            return (myType == Type.AUDIO);
        }

        public boolean isVideo() {
            return (myType == Type.VIDEO);
        }

        public long getTimeStamp() {
            return timeStamp;
        }

        @Override
        public String toString() {
            return "{Processed Packet: type = " + this.myType + "; timestamp = " + this.timeStamp + "}";
        }

        public int compareTo(ProcessedPacket o2) {
            if (getTimeStamp() > o2.getTimeStamp()) return 1;
            if (getTimeStamp() < o2.getTimeStamp()) return -1; else return 0;
        }
    }
}
