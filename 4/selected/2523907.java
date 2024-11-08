package magoffin.matt.meta.video;

import java.awt.Dimension;
import java.awt.Image;
import java.io.File;
import javax.media.Buffer;
import javax.media.Duration;
import javax.media.Processor;
import javax.media.Time;
import javax.media.control.FrameGrabbingControl;
import javax.media.control.TrackControl;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.util.BufferToImage;
import magoffin.matt.meta.MetadataResource;
import magoffin.matt.meta.support.BasicMetadataImage;

/**
 * {@link MetadataResource} implemention using JMF.
 * 
 * @author Matt Magoffin (spamsqr@msqr.us)
 * @version $Revision: 1.3 $ $Date: 2007/05/19 21:36:02 $
 */
public class JMFMetadataResource extends AbstractVideoMetadataResource {

    /**
	 * Constructor.
	 * 
	 * @param processor the processor
	 * @param mediaResource the video file
	 */
    public JMFMetadataResource(Processor processor, File mediaResource) {
        JMFLoader l = new JMFLoader(processor);
        TrackControl videoTrack = l.getVideoTrackControl(mediaResource);
        Time duration = processor.getDuration();
        if (duration != Duration.DURATION_UNKNOWN) {
            if (log.isDebugEnabled()) {
                log.debug("Movie duration " + duration.getSeconds() + " for " + mediaResource.getAbsolutePath());
            }
            long ns = duration.getNanoseconds();
            long ms = ns / 1000000;
            setValue(VideoMetadataType.DURATION, Long.valueOf(ms));
            setValue(VideoMetadataType.DURATION_TIME, getDurationTime(ms));
        }
        VideoFormat videoFormat = (VideoFormat) videoTrack.getFormat();
        setValue(VideoMetadataType.VIDEO_FORMAT, videoFormat.getEncoding());
        setValue(VideoMetadataType.FPS, Float.valueOf(videoFormat.getFrameRate()));
        Dimension dim = videoFormat.getSize();
        setValue(VideoMetadataType.WIDTH, Integer.valueOf((int) dim.getWidth()));
        setValue(VideoMetadataType.HEIGHT, Integer.valueOf((int) dim.getHeight()));
        processor.prefetch();
        if (!l.waitForState(Processor.Prefetched)) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to prefetch the player for " + mediaResource.getAbsolutePath());
            }
        } else {
            FrameGrabbingControl fgc = (FrameGrabbingControl) processor.getControl("javax.media.control.FrameGrabbingControl");
            if (fgc == null) {
                if (log.isDebugEnabled()) {
                    log.error("The processor " + processor + " does not support FrameGrabbingControl for " + mediaResource.getAbsolutePath());
                }
            } else {
                Buffer bufferFrame = fgc.grabFrame();
                if (bufferFrame == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Buffer not returned from FrameGrabbingController for " + mediaResource.getAbsolutePath());
                    }
                } else {
                    BufferToImage bufferToImage = new BufferToImage((VideoFormat) bufferFrame.getFormat());
                    Image image = bufferToImage.createImage(bufferFrame);
                    BasicMetadataImage metaImage = new BasicMetadataImage(image);
                    setValue(VideoMetadataType.POSTER, metaImage);
                }
            }
        }
        TrackControl audioTrack = l.getAudioTrack(mediaResource);
        if (audioTrack != null) {
            AudioFormat audioFormat = (AudioFormat) audioTrack.getFormat();
            StringBuilder buf = new StringBuilder();
            buf.append(audioFormat.getEncoding()).append(", ");
            if (audioFormat.getSampleRate() != AudioFormat.NOT_SPECIFIED) {
                buf.append(audioFormat.getSampleRate()).append(" Hz, ");
            }
            if (audioFormat.getSampleSizeInBits() != AudioFormat.NOT_SPECIFIED) {
                buf.append(audioFormat.getSampleSizeInBits()).append(" bit, ");
            }
            switch(audioFormat.getChannels()) {
                case 1:
                    buf.append("mono");
                    break;
                case 2:
                    buf.append("stereo");
                    break;
                default:
                    buf.append(audioFormat.getChannels()).append(" channels");
            }
            setValue(VideoMetadataType.AUDIO_FORMAT, buf.toString());
        }
    }
}
