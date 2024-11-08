package org.esb.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MediaStream extends MemberMap {

    private List<Filter> _filter_list = new ArrayList<Filter>();

    int id;

    MediaFile file;

    int streamIndex;

    int streamType;

    int codecId;

    String CodecName;

    int frameRateNum;

    int frameRateDen;

    Long startTime;

    Long firstDts;

    Long duration;

    Long numFrames;

    int timeBaseNum;

    int timeBaseDen;

    int codecTimeBaseNum;

    int codecTimeBaseDen;

    int ticksPerFrame;

    int frameCount;

    int width;

    int height;

    int gopSize;

    int pixelFormat;

    int bitrate;

    int rateEmu;

    int sampleRate;

    int channels;

    int sampleFormat;

    int bitsPerCodedSample;

    int privateDataSize;

    byte[] _privateData;

    byte[] _extraData;

    int flags;

    int _codec_type;

    String extraCodecFlags;

    public MediaStream() {
    }

    public MediaStream(Map<String, Object> data) {
        super(data);
    }

    public void setId(int id) {
        this.id = id;
        setAttribute("id", id);
    }

    public int getId() {
        return getIntegerAttribute("id");
    }

    public void setCodecType(int type) {
        setAttribute("codec_type", type);
    }

    public int getCodecType() {
        return getIntegerAttribute("codec_type");
    }

    public void setStreamIndex(int streamIndex) {
        setAttribute("stream_index", streamIndex);
    }

    public int getStreamIndex() {
        return getIntegerAttribute("stream_index");
    }

    public void setStreamType(int streamType) {
        setAttribute("stream_type", streamType);
    }

    public int getStreamType() {
        return getIntegerAttribute("stream_type");
    }

    public void setCodecId(int codecId) {
        setAttribute("codec_id", codecId);
    }

    public int getCodecId() {
        return getIntegerAttribute("codec_id");
    }

    public void setCodecName(String CodecName) {
        setAttribute("codec_name", CodecName);
    }

    public String getCodecName() {
        return getAttribute("codec_name");
    }

    public void setFrameRateNum(int frameRateNum) {
        setAttribute("frame_rate_num", frameRateNum);
    }

    public int getFrameRateNum() {
        return getIntegerAttribute("frame_rate_num");
    }

    public void setFrameRateDen(int frameRateDen) {
        setAttribute("frame_rate_den", frameRateDen);
    }

    public int getFrameRateDen() {
        return getIntegerAttribute("frame_rate_den");
    }

    public void setStartTime(Long startTime) {
        setAttribute("start_time", startTime);
    }

    public Long getStartTime() {
        return getLongAttribute("start_time");
    }

    public void setFirstDts(Long firstDts) {
        setAttribute("first_dts", firstDts);
    }

    public Long getFirstDts() {
        return getLongAttribute("first_dts");
    }

    public void setDuration(Long duration) {
        setAttribute("duration", duration);
    }

    public Long getDuration() {
        return getLongAttribute("duration");
    }

    public void setNumFrames(Long numFrames) {
        setAttribute("num_frames", numFrames);
    }

    public Long getNumFrames() {
        return getLongAttribute("num_frames");
    }

    public void setTimeBaseNum(int timeBaseNum) {
        setAttribute("time_base_num", timeBaseNum);
    }

    public int getTimeBaseNum() {
        return getIntegerAttribute("time_base_num");
    }

    public void setTimeBaseDen(int timeBaseDen) {
        setAttribute("time_base_den", timeBaseDen);
    }

    public int getTimeBaseDen() {
        return getIntegerAttribute("time_base_den");
    }

    public void setCodecTimeBaseNum(int codecTimeBaseNum) {
        setAttribute("codec_time_base_num", codecTimeBaseNum);
    }

    public int getCodecTimeBaseNum() {
        return getIntegerAttribute("codec_time_base_num");
    }

    public void setCodecTimeBaseDen(int codecTimeBaseDen) {
        setAttribute("codec_time_base_den", codecTimeBaseDen);
    }

    public int getCodecTimeBaseDen() {
        return getIntegerAttribute("codec_time_base_den");
    }

    public void setTicksPerFrame(int ticksPerFrame) {
        setAttribute("tick_per_frame", ticksPerFrame);
    }

    public int getTicksPerFrame() {
        return getIntegerAttribute("tick_per_frame");
    }

    public void setFrameCount(int framecount) {
        setAttribute("framecount", framecount);
    }

    public int getFrameCount() {
        return getIntegerAttribute("framecount");
    }

    public void setWidth(int width) {
        setAttribute("width", width);
    }

    public int getWidth() {
        return getIntegerAttribute("width");
    }

    public void setHeight(int height) {
        setAttribute("height", height);
    }

    public int getHeight() {
        return getIntegerAttribute("height");
    }

    public void setGopSize(int gopSize) {
        setAttribute("gop_size", gopSize);
    }

    public int getGopSize() {
        return getIntegerAttribute("gop_size");
    }

    public void setPixelFormat(int pixelFormat) {
        setAttribute("pixel_format", pixelFormat);
    }

    public int getPixelFormat() {
        return getIntegerAttribute("pixel_format");
    }

    public void setBitrate(int bitrate) {
        setAttribute("bitrate", bitrate);
    }

    public int getBitrate() {
        return getIntegerAttribute("bitrate");
    }

    public void setRateEmu(int rateEmu) {
        setAttribute("rate_emu", rateEmu);
    }

    public int getRateEmu() {
        return getIntegerAttribute("rate_emu");
    }

    public void setSampleRate(int sampleRate) {
        setAttribute("sample_rate", sampleRate);
    }

    public int getSampleRate() {
        return getIntegerAttribute("sample_rate");
    }

    public void setChannels(int channels) {
        setAttribute("channels", channels);
    }

    public int getChannels() {
        return getIntegerAttribute("channels");
    }

    public void setSampleFormat(int sampleFormat) {
        setAttribute("sample_format", sampleFormat);
    }

    public int getSampleFormat() {
        return getIntegerAttribute("sample_format");
    }

    public void setBitsPerCodedSample(int bitsPerCodedSample) {
        setAttribute("bits_per_coded_sample", bitsPerCodedSample);
    }

    public int getBitsPerCodedSample() {
        return getIntegerAttribute("bits_per_coded_sample");
    }

    public void setPrivateDataSize(int privateDataSize) {
        setAttribute("private_data_size", privateDataSize);
    }

    public int getPrivateDataSize() {
        return getIntegerAttribute("private_data_size");
    }

    public void setPrivateData(byte[] privateData) {
        _privateData = privateData;
    }

    public byte[] getPrivateData() {
        return _privateData;
    }

    public void setExtraDataSize(int privateDataSize) {
        setAttribute("extra_data_size", privateDataSize);
    }

    public int getExtraDataSize() {
        return getIntegerAttribute("extra_data_size");
    }

    public void setExtraData(byte[] privateData) {
        _extraData = privateData;
    }

    public byte[] getExtraData() {
        return _extraData;
    }

    public void setFlags(int flags) {
        setAttribute("flags", flags);
    }

    public int getFlags() {
        return getIntegerAttribute("flags");
    }

    public void setExtraCodecFlags(String extraCodecFlags) {
        setAttribute("extra_codec_flags", extraCodecFlags);
    }

    public String getExtraCodecFlags() {
        return getAttribute("extra_codec_flags");
    }

    public void setFile(MediaFile file) {
        this.file = file;
    }

    public MediaFile getFile() {
        return file;
    }

    /**
   * @return the _filter_list
   */
    public List<Filter> getFilter() {
        return _filter_list;
    }

    /**
   * @param filter_list the _filter_list to set
   */
    public void setFilter(List<Filter> filter_list) {
        this._filter_list = filter_list;
    }

    public void addFilter(Filter f) {
        _filter_list.add(f);
    }

    public void setAttributes(Map<String, Object> data) {
        this.putAll(data);
    }

    public Map getAttributes() {
        return this;
    }
}
