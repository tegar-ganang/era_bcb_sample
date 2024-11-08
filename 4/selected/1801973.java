package org.mil.bean.dom;

public class AudioTrackDO extends TrackDO {

    protected String ID;

    protected String format;

    protected String format_Info;

    protected String format_version;

    protected String format_profile;

    protected String codec_ID;

    protected String duration;

    protected String channel_s_;

    protected String channel_positions;

    protected String sampling_rate;

    protected String bit_rate;

    protected String compression_mode;

    protected String video_delay;

    protected String title;

    protected String language;

    public String getID() {
        return ID;
    }

    public String getFormat() {
        return format;
    }

    public String getFormat_Info() {
        return format_Info;
    }

    public String getFormat_version() {
        return format_version;
    }

    public String getFormat_profile() {
        return format_profile;
    }

    public String getCodec_ID() {
        return codec_ID;
    }

    public String getDuration() {
        return duration;
    }

    public String getChannel_s() {
        return channel_s_;
    }

    public String getChannel_positions() {
        return channel_positions;
    }

    public String getSampling_rate() {
        return sampling_rate;
    }

    public String getCompression_mode() {
        return compression_mode;
    }

    public String getVideo_delay() {
        return video_delay;
    }

    public String getTitle() {
        return title;
    }

    public String getLanguage() {
        return language;
    }

    @Override
    public void init() {
        this.setType(TrackDO.T_AUDIO);
    }

    public String getBit_rate() {
        return bit_rate;
    }
}
