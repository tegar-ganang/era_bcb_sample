package org.mil.bean.bo;

import org.mil.bean.dom.AudioTrackDO;
import org.mil.util.TextUtil;

public class AudioTrack extends Track {

    private String ID;

    private String format;

    private String format_Info;

    private String format_version;

    private String format_profile;

    private String codec_ID;

    private int duration;

    private byte num_channels;

    private int sampling_rate;

    private int bit_rate;

    private int video_delay;

    private String title;

    private String language;

    public void load(AudioTrackDO source) {
        ID = source.getID();
        format = source.getFormat();
        format_Info = source.getFormat_Info();
        format_profile = source.getFormat_profile();
        format_version = source.getFormat_version();
        codec_ID = source.getCodec_ID();
        num_channels = (byte) TextUtil.quantify(source.getChannel_s(), "channels");
        sampling_rate = (int) (TextUtil.quantify(source.getSampling_rate(), "KHz") * 1000);
        bit_rate = (int) TextUtil.quantify(source.getBit_rate(), "Kbps");
        title = source.getTitle();
        language = source.getLanguage();
    }

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

    public int getDuration() {
        return duration;
    }

    public byte getNum_channels() {
        return num_channels;
    }

    public int getSampling_rate() {
        return sampling_rate;
    }

    public int getBit_rate() {
        return bit_rate;
    }

    public int getVideo_delay() {
        return video_delay;
    }

    public String getTitle() {
        return title;
    }

    public String getLanguage() {
        return language;
    }
}
