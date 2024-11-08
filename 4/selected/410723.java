package com.cameocontrol.cameo.control;

import com.cameocontrol.cameo.file.adt.SettingsData;
import com.cameocontrol.cameo.util.HashtableUtils;

public abstract class BasicSettings implements ConsoleSettings {

    RecordMode _mode;

    int _totalChannels;

    int _totalDimmers;

    int _upTime;

    int _downTime;

    int _gotoCueTime;

    int _ChannelsPerLine;

    int _ChannelGrouping;

    int _LineGrouping;

    String _showTitle;

    String _showComment;

    public boolean isTracking() {
        return (_mode == RecordMode.TRACKING);
    }

    public boolean isQueOnly() {
        return (_mode == RecordMode.CUE_ONLY);
    }

    public RecordMode getRecordMode() {
        return _mode;
    }

    public int getChannels() {
        return _totalChannels;
    }

    public int getDimmers() {
        return _totalDimmers;
    }

    public int getChannelsPerLine() {
        return _ChannelsPerLine;
    }

    public int getChannelGrouping() {
        return _ChannelGrouping;
    }

    public int getLineGrouping() {
        return _LineGrouping;
    }

    public String getTitle() {
        return _showTitle;
    }

    public String getComment() {
        return _showComment;
    }

    public int getDefaultUpTime() {
        return _upTime;
    }

    public int getDefaultDownTime() {
        return _downTime;
    }

    public int getDefaultGotoCueTime() {
        return _gotoCueTime;
    }

    public SettingsData distill() {
        SettingsData pd = new SettingsData();
        pd.put(ID_ATTRIB, getIDValue());
        pd.put(RECORD_MODE_TAG, _mode.toString());
        pd.put(TOTAL_CHANNELS_TAG, Integer.toString(_totalChannels));
        pd.put(TOTAL_DIMMERS_TAG, Integer.toString(_totalDimmers));
        pd.put(DEFAULT_UP_TIME_TAG, Integer.toString(_upTime));
        pd.put(DEFAULT_DOWN_TIME_TAG, Integer.toString(_downTime));
        pd.put(DEFAULT_GOTOCUE_TIME_TAG, Integer.toString(_gotoCueTime));
        pd.put(SHOW_TITLE_TAG, _showTitle);
        pd.put(SHOW_COMMENT_TAG, _showComment);
        pd.put(CHANNELS_PER_LINE_TAG, Integer.toString(_ChannelsPerLine));
        pd.put(CHANNELS_PER_HORIZONTEL_GROUP_TAG, Integer.toString(_ChannelGrouping));
        pd.put(CHANNELS_PER_VERTICAL_GROUP_TAG, Integer.toString(_LineGrouping));
        return pd;
    }

    public void extractFrom(SettingsData data) {
        if (data.contains(ID_ATTRIB)) assert (data.get(ID_ATTRIB).equals(getIDValue()));
        if (data.contains(RECORD_MODE_TAG)) {
            String mode = data.get(RECORD_MODE_TAG);
            if (mode.equals(RecordMode.CUE_ONLY.toString())) _mode = RecordMode.CUE_ONLY; else _mode = RecordMode.TRACKING;
        }
        _totalChannels = HashtableUtils.checkInt(_totalChannels, TOTAL_CHANNELS_TAG, data);
        _totalDimmers = HashtableUtils.checkInt(_totalDimmers, TOTAL_DIMMERS_TAG, data);
        _upTime = HashtableUtils.checkInt(_upTime, DEFAULT_UP_TIME_TAG, data);
        _downTime = HashtableUtils.checkInt(_downTime, DEFAULT_DOWN_TIME_TAG, data);
        _gotoCueTime = HashtableUtils.checkInt(_gotoCueTime, DEFAULT_GOTOCUE_TIME_TAG, data);
        _ChannelsPerLine = HashtableUtils.checkInt(_ChannelsPerLine, CHANNELS_PER_LINE_TAG, data);
        _ChannelGrouping = HashtableUtils.checkInt(_ChannelGrouping, CHANNELS_PER_HORIZONTEL_GROUP_TAG, data);
        _LineGrouping = HashtableUtils.checkInt(_LineGrouping, CHANNELS_PER_VERTICAL_GROUP_TAG, data);
        _showTitle = HashtableUtils.check(_showTitle, SHOW_TITLE_TAG, data);
        _showComment = HashtableUtils.check(_showComment, SHOW_COMMENT_TAG, data);
    }
}
