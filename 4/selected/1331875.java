package com.aranin.jaiom.mediaprocess.videoprocess.ffmpeg;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.aranin.jaiom.constants.JaiomConstants;
import com.aranin.jaiom.exception.EncoderException;
import com.aranin.jaiom.exception.InputFormatException;
import com.aranin.jaiom.exception.InvalidActionTypeException;
import com.aranin.jaiom.exception.JaiomException;
import com.aranin.jaiom.mediaprocess.processor.VideoProcessor;
import com.aranin.jaiom.utils.MediaUploadUtil;
import com.aranin.jaiom.vo.MediaQueueVO;

/**
 * Concrete implemenatation of Video processor interface using ffmpeg
 * 
 * @author Niraj Singh
 *  
 */
public class FFmpegVideoProcessor extends VideoProcessor {

    protected final Logger log = LoggerFactory.getLogger(FFmpegVideoProcessor.class);

    private final String VIDEO_CODEC = "flv";

    private final String VIDEO_TAG = "DIVX";

    private final Integer VIDEO_BITRATE = 360000;

    private final Integer VIDEO_FRAMERATE = 30;

    private final String AUDIO_CODEC = "libmp3lame";

    private final Integer AUDIO_CHANNEL = 2;

    private final Integer AUDIO_BITRATE = 128000;

    private final Integer AUDIO_SAMPLINGRATE = 44100;

    private final Integer AUDIO_VOLUME = 256;

    private final String ENCODING_FORMAT = "flv";

    private static final Float ENCODING_DURATION = 30F;

    private static final Float ENCODING_OFFSET = 5F;

    protected String source = null;

    protected String target = null;

    public FFmpegVideoProcessor() {
    }

    public FFmpegVideoProcessor(String source, String target) {
        this.source = source;
        this.target = target;
    }

    public String encodeVideo(long id, String source, String target) throws JaiomException {
        EncoderProgressListener encoderProgressListener = new JaiomEncoderProgressListener();
        log.debug(" source : " + source);
        log.debug(" target : " + target);
        File sourceFile = new File(source);
        File targetFile = new File(target);
        Encoder encoder = new Encoder(new JaiomFFMpegLocator());
        String mpResult = null;
        try {
            MultimediaInfo videoInfo = encoder.getInfo(sourceFile);
            EncodingAttributes attrs = this.setEncodingAttributes(videoInfo);
            String mediaType = JaiomConstants.VIDEO_TYPE;
            String fileName = targetFile.getName();
            String fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf(".") - 1);
            String contentFileName = sourceFile.getName();
            String fileNameTarget = fileName;
            String fileWidth = videoInfo.getVideo().getSize().getWidth() + "";
            String fileHeight = videoInfo.getVideo().getSize().getHeight() + "";
            String fileDuration = videoInfo.getDuration() + "";
            String videoFrameRate = videoInfo.getVideo().getFrameRate() + "";
            String audioFrameRate = videoInfo.getAudio().getSamplingRate() + "";
            String videoCodec = videoInfo.getVideo().getDecoder();
            String audioCodec = videoInfo.getAudio().getDecoder();
            String audioChannel = videoInfo.getAudio().getChannels() + "";
            String audioSamplingRate = videoInfo.getAudio().getSamplingRate() + "";
            String audioBitRate = videoInfo.getAudio().getBitRate() + "";
            String videoBitRate = videoInfo.getVideo().getBitRate() + "";
            encoder.encode(sourceFile, targetFile, attrs, encoderProgressListener);
            mpResult = MediaUploadUtil.createVideoMPResultString(mediaType, contentFileName, fileNameTarget, fileWidth, fileHeight, fileDuration, videoFrameRate, videoBitRate, audioFrameRate, videoCodec, audioCodec, audioChannel, audioSamplingRate, audioBitRate, "", "", "none");
        } catch (IllegalArgumentException e) {
            log.error("IllegalArgumentException while encoding Video : " + e);
            throw new JaiomException("IllegalArgumentException while encoding Video : " + e);
        } catch (InputFormatException e) {
            log.error("InputFormatException while encoding Video : " + e);
            throw new JaiomException("InputFormatException while encoding Video : " + e);
        } catch (EncoderException e) {
            log.error("EncoderException while encoding Video : " + e);
            throw new JaiomException("EncoderException while encoding Video : " + e);
        }
        return mpResult;
    }

    public String getVideoInfo(long id, String source) throws JaiomException {
        EncoderProgressListener encoderProgressListener = new JaiomEncoderProgressListener();
        log.debug(" source : " + source);
        log.debug(" target : " + target);
        File sourceFile = new File(source);
        String mpResult = null;
        boolean error = false;
        String errorString = "";
        Encoder encoder = new Encoder(new JaiomFFMpegLocator());
        MultimediaInfo videoInfo;
        try {
            videoInfo = encoder.getInfo(sourceFile);
            EncodingAttributes attrs = this.setEncodingAttributes(videoInfo);
            String mediaType = JaiomConstants.VIDEO_TYPE;
            String fileName = "";
            String fileNameWithoutExtension = "";
            String contentFileName = sourceFile.getName();
            String fileNameTarget = fileName;
            String fileWidth = videoInfo.getVideo().getSize().getWidth() + "";
            String fileHeight = videoInfo.getVideo().getSize().getHeight() + "";
            String fileDuration = videoInfo.getDuration() + "";
            String videoFrameRate = videoInfo.getVideo().getFrameRate() + "";
            String audioFrameRate = videoInfo.getAudio().getSamplingRate() + "";
            String videoCodec = videoInfo.getVideo().getDecoder();
            String audioCodec = videoInfo.getAudio().getDecoder();
            String audioChannel = videoInfo.getAudio().getChannels() + "";
            String audioSamplingRate = videoInfo.getAudio().getSamplingRate() + "";
            String audioBitRate = videoInfo.getAudio().getBitRate() + "";
            String videoBitRate = videoInfo.getVideo().getBitRate() + "";
            mpResult = MediaUploadUtil.createVideoMPResultString(mediaType, contentFileName, fileNameTarget, fileWidth, fileHeight, fileDuration, videoFrameRate, videoBitRate, audioFrameRate, videoCodec, audioCodec, audioChannel, audioSamplingRate, audioBitRate, "", "", "none");
        } catch (InputFormatException e) {
            log.error("InputFormatException while encoding Video : " + e);
            throw new JaiomException("InputFormatException while getting Video info: " + e);
        } catch (EncoderException e) {
            log.error("EncoderException while encoding Video : " + e);
            throw new JaiomException("EncoderException while encoding Video info: " + e);
        } catch (Exception e) {
            log.error("Exception while encoding Video  : " + e);
            throw new JaiomException("Exception while encoding Video  : " + e);
        }
        return mpResult;
    }

    public String pullSnapShots(long id, String source, int number, int offset) throws JaiomException {
        EncoderProgressListener encoderProgressListener = new JaiomEncoderProgressListener();
        Encoder encoder = new Encoder(new JaiomFFMpegLocator());
        File sourceFile = new File(source);
        String mpResult = null;
        try {
            MultimediaInfo videoInfo = encoder.getInfo(sourceFile);
            EncodingAttributes attrs = this.setEncodingAttributes(videoInfo);
            String mediaType = JaiomConstants.VIDEO_TYPE;
            String fileName = "";
            String fileNameWithoutExtension = "";
            String contentFileName = sourceFile.getName();
            String fileNameFlv = "";
            String fileWidth = videoInfo.getVideo().getSize().getWidth() + "";
            String fileHeight = videoInfo.getVideo().getSize().getHeight() + "";
            String fileDuration = videoInfo.getDuration() + "";
            String videoFrameRate = videoInfo.getVideo().getFrameRate() + "";
            String audioFrameRate = videoInfo.getAudio().getSamplingRate() + "";
            String videoCodec = videoInfo.getVideo().getDecoder();
            String audioCodec = videoInfo.getAudio().getDecoder();
            String audioChannel = videoInfo.getAudio().getChannels() + "";
            String audioSamplingRate = videoInfo.getAudio().getSamplingRate() + "";
            String audioBitRate = videoInfo.getAudio().getBitRate() + "";
            String videoBitRate = videoInfo.getVideo().getBitRate() + "";
            encoder.pullSnapShots(sourceFile, number, offset, encoderProgressListener);
            mpResult = MediaUploadUtil.createVideoMPResultString(mediaType, contentFileName, fileNameFlv, fileWidth, fileHeight, fileDuration, videoFrameRate, videoBitRate, audioFrameRate, videoCodec, audioCodec, audioChannel, audioSamplingRate, audioBitRate, number + "", offset + "", "none");
        } catch (IllegalArgumentException e) {
            log.error("IllegalArgumentException while grabbing Video frame: " + e);
            throw new JaiomException("IllegalArgumentException while grabbing Video frame : " + e);
        } catch (InputFormatException e) {
            log.error("InputFormatException while grabbing Video frame : " + e);
            throw new JaiomException("InputFormatException while grabbing Video frame : " + e);
        } catch (EncoderException e) {
            log.error("EncoderException while grabbing Video frame : " + e);
            throw new JaiomException("EncoderException while grabbing Video frame : " + e);
        } catch (Exception e) {
            log.error("Exception while grabbing Video frame : " + e);
            throw new JaiomException("Exception while grabbing Video frame : " + e);
        } finally {
        }
        return mpResult;
    }

    public EncodingAttributes setEncodingAttributes(MultimediaInfo videoInfo) {
        AudioAttributes audio = this.setAudioAttributes(videoInfo);
        VideoAttributes video = this.setVideoAttributes(videoInfo);
        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setFormat(this.ENCODING_FORMAT);
        attrs.setAudioAttributes(audio);
        attrs.setVideoAttributes(video);
        return attrs;
    }

    /**
	 * this method sets Attributes controlling the video encoding process.
	 * @param videoInfo 
	 * @return com.aranin.samayikprocessor.VideoAttributes
	 */
    public VideoAttributes setVideoAttributes(MultimediaInfo videoInfo) {
        VideoAttributes video = new VideoAttributes();
        video.setCodec(this.VIDEO_CODEC);
        video.setBitRate(videoInfo.getVideo().getBitRate());
        video.setFrameRate((int) videoInfo.getVideo().getFrameRate());
        return video;
    }

    /**
	 * this method sets Attributes controlling the audio encoding process.
	 * @param videoInfo 
	 * @return com.aranin.samayikprocessor.AudioAttributes
	 */
    public AudioAttributes setAudioAttributes(MultimediaInfo videoInfo) {
        AudioAttributes audio = new AudioAttributes();
        audio.setCodec(this.AUDIO_CODEC);
        audio.setBitRate(videoInfo.getAudio().getBitRate());
        audio.setSamplingRate(this.AUDIO_SAMPLINGRATE);
        audio.setChannels(videoInfo.getAudio().getChannels());
        audio.setVolume(this.AUDIO_VOLUME);
        return audio;
    }

    public String getStatus(long id, String action) throws JaiomException {
        return null;
    }

    public static void main(String[] args) {
        String source = "d:/vids/MOV00399.mpg";
        String target = "d:/vids/MOV00399.flv";
        try {
            FFmpegVideoProcessor vp = new FFmpegVideoProcessor(source, target);
            vp.encodeVideo(1, source, target);
            vp.pullSnapShots(1, source, 5, 0);
        } catch (JaiomException e) {
        }
    }

    public String getProcessorName() {
        return "FFMpeg processor";
    }

    @Override
    public String getProcessorInfo() {
        return "Processing Video using FFmpegVideoProcessor";
    }
}
