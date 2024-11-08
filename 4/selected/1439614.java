package org.esb.util;

import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IProperty;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import java.io.File;
import java.util.Collection;
import org.esb.model.MediaFile;
import org.esb.model.MediaStream;
import org.esb.model.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author HoelscJ
 */
public class Util {

    private static Logger _log = LoggerFactory.getLogger(Util.class);

    private Util() {
    }

    public static MediaFile toMediaFile(IContainer c) {
        return toMediaFile(c, new MediaFile());
    }

    public static void createOutputMediaFile(MediaFile infile, MediaFile outfile, Profile profile) throws Exception {
        if (outfile.getPath() == null) {
            throw new Exception("Out File must have a directory set");
        }
        outfile.setParent(infile);
        String outfilename = infile.getFilename();
        outfilename = outfilename.substring(0, outfilename.lastIndexOf("."));
        outfilename += "." + profile.getAttribute("v_format_ext");
        outfile.setFilename(outfilename);
        outfile.setDuration(infile.getDuration());
        outfile.setContainerType(profile.getAttribute("v_format"));
        int idx = 0;
        for (MediaStream s : infile.getStreams()) {
            if (s.getCodecType() != 0 && s.getCodecType() != 1) continue;
            MediaStream outs = new MediaStream();
            outs.setStreamIndex(idx++);
            outs.setStreamType(s.getStreamType());
            outs.setCodecType(s.getCodecType());
            if (s.getStreamType() == 0) {
                outs.setCodecId(Integer.parseInt(profile.getAttribute("v_codec")));
                outs.setBitrate(Integer.parseInt(profile.getAttribute("v_bitrate")));
                outs.setWidth(Integer.parseInt(profile.getAttribute("v_width")));
                outs.setHeight(Integer.parseInt(profile.getAttribute("v_height")));
                int framerate_num = 0;
                int framerate_den = 0;
                String framerate = profile.getAttribute("v_framerate");
                if (framerate.equals("0/0")) {
                    framerate_num = s.getFrameRateNum();
                    framerate_den = s.getFrameRateDen();
                } else {
                    String[] elem = framerate.split("/");
                    if (elem.length == 2) {
                        framerate_num = Integer.parseInt(elem[0]);
                        framerate_den = Integer.parseInt(elem[1]);
                    }
                }
                outs.setFrameRateDen(framerate_num);
                outs.setFrameRateNum(framerate_den);
                outs.setCodecTimeBaseNum(outs.getFrameRateDen());
                outs.setCodecTimeBaseDen(outs.getFrameRateNum());
                outs.setTimeBaseNum(s.getTimeBaseNum());
                outs.setTimeBaseDen(s.getTimeBaseDen());
                outs.setGopSize(20);
                outs.setPixelFormat(1);
                outs.setExtraCodecFlags(profile.getAttribute("v_extra"));
            }
            if (s.getStreamType() == 1) {
                outs.setCodecId(Integer.parseInt(profile.getAttribute("a_codec")));
                outs.setBitrate(Integer.parseInt(profile.getAttribute("a_bitrate")));
                outs.setSampleRate(Integer.parseInt(profile.getAttribute("a_samplerate")));
                outs.setTimeBaseNum(1);
                outs.setTimeBaseDen(outs.getSampleRate());
                outs.setChannels(Integer.parseInt(profile.getAttribute("a_channels")));
                outs.setCodecTimeBaseNum(outs.getTimeBaseNum());
                outs.setCodecTimeBaseDen(outs.getTimeBaseDen());
                outs.setExtraCodecFlags(profile.getAttribute("a_extra"));
                outs.setSampleFormat(1);
            }
            outfile.addStream(outs);
        }
    }

    public static MediaFile toMediaFile(IContainer c, MediaFile file) {
        File f = new File(c.getURL());
        file.setFilename(f.getName());
        file.setPath(f.getParent());
        file.setSize(c.getFileSize());
        file.setContainerType(c.getContainerFormat().getInputFormatShortName());
        file.setTitle(c.getMetaData().getValue("title"));
        file.setAuthor(c.getMetaData().getValue("author"));
        file.setCopyright(c.getMetaData().getValue("copyright"));
        file.setComment(c.getMetaData().getValue("comment"));
        file.setAlbum(c.getMetaData().getValue("album"));
        if (c.getMetaData().getValue("year") != null) {
            file.setYear(Integer.parseInt(c.getMetaData().getValue("year")));
        }
        if (c.getMetaData().getValue("track") != null) {
            file.setTrack(Integer.parseInt(c.getMetaData().getValue("track")));
        }
        if (c.getMetaData().getValue("genre") != null) {
            file.setGenre(Integer.parseInt(c.getMetaData().getValue("genre")));
        }
        file.setDuration(c.getDuration());
        file.setBitrate(new Long(c.getBitRate()));
        file.setInsertDate(new java.util.Date());
        file.setParent(null);
        file.setFileType(0);
        int nb_streams = c.getNumStreams();
        for (int a = 0; a < nb_streams; a++) {
            IStream stream = c.getStream(a);
            IStreamCoder codec = stream.getStreamCoder();
            if (codec.getCodecType() != ICodec.Type.CODEC_TYPE_VIDEO && codec.getCodecType() != ICodec.Type.CODEC_TYPE_AUDIO) continue;
            MediaStream s = new MediaStream();
            s.setStreamIndex(stream.getIndex());
            s.setStreamType(codec.getCodecType().swigValue());
            s.setCodecType(codec.getCodecType().swigValue());
            s.setCodecId(codec.getCodecID().swigValue());
            s.setFrameRateNum(stream.getFrameRate().getNumerator());
            s.setFrameRateDen(stream.getFrameRate().getDenominator());
            s.setStartTime(stream.getStartTime());
            s.setFirstDts(stream.getFirstDts());
            s.setDuration(stream.getDuration());
            s.setNumFrames(stream.getNumFrames());
            s.setTimeBaseNum(stream.getTimeBase().getNumerator());
            s.setTimeBaseDen(stream.getTimeBase().getDenominator());
            s.setCodecTimeBaseNum(codec.getTimeBase().getNumerator());
            s.setCodecTimeBaseDen(codec.getTimeBase().getDenominator());
            if (codec.getPropertyAsLong("ticks_per_frame") > 0) {
                s.setTicksPerFrame(new Long(codec.getPropertyAsLong("ticks_per_frame")).intValue());
            }
            s.setWidth(codec.getWidth());
            s.setHeight(codec.getHeight());
            s.setGopSize(codec.getNumPicturesInGroupOfPictures());
            s.setPixelFormat(codec.getPixelType().swigValue());
            s.setBitrate(codec.getBitRate());
            s.setSampleRate(codec.getSampleRate());
            s.setChannels(codec.getChannels());
            s.setSampleFormat(codec.getSampleFormat().swigValue());
            if (codec.getPropertyAsLong("bits_per_coded_sample") > 0) {
                s.setBitsPerCodedSample(new Long(codec.getPropertyAsLong("bits_per_coded_sample")).intValue());
            }
            Collection<String> keys = codec.getPropertyNames();
            for (String key : keys) {
                s.setAttribute(key, codec.getPropertyAsString(key));
            }
            file.addStream(s);
        }
        return file;
    }
}
