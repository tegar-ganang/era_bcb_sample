package com.dcivision.user.bean;

import com.dcivision.framework.ApplicationException;
import com.dcivision.framework.bean.AbstractBaseObject;

public class ChannelPreferenceBean extends AbstractBaseObject {

    private String channelTitle = null;

    private String channelName = null;

    private String disabledDisplay = null;

    private String perPage = null;

    private String channelSequence = null;

    private String suffix = null;

    private boolean perPageTextOnly = false;

    private boolean channelSequenceTextOnly = false;

    public void setChannelPreferenceStr(String formatStr) throws ApplicationException {
        try {
            String[] args = formatStr.split("_");
            channelName = args[0];
            disabledDisplay = args[1];
            perPage = args[2];
            channelSequence = args[3];
        } catch (Exception ex) {
            throw new ApplicationException("illegal channelPreference String");
        }
    }

    public String getFormatChannelPreferenceStr() {
        StringBuffer bstr = new StringBuffer();
        bstr.append(channelName);
        bstr.append("_").append(disabledDisplay);
        bstr.append("_").append(perPage);
        bstr.append("_").append(channelSequence);
        return bstr.toString();
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelSequence() {
        return channelSequence;
    }

    public void setChannelSequence(String channelSequence) {
        this.channelSequence = channelSequence;
    }

    public String getPerPage() {
        return perPage;
    }

    public void setPerPage(String perPage) {
        this.perPage = perPage;
    }

    public String getDisabledDisplay() {
        return disabledDisplay;
    }

    public void setDisabledDisplay(String disabledDisplay) {
        this.disabledDisplay = disabledDisplay;
    }

    public boolean getPerPageTextOnly() {
        return perPageTextOnly;
    }

    public void setPerPageTextOnly(boolean perPageTextOnly) {
        this.perPageTextOnly = perPageTextOnly;
    }

    public boolean getChannelSequenceTextOnly() {
        return channelSequenceTextOnly;
    }

    public void setChannelSequenceTextOnly(boolean channelSequenceTextOnly) {
        this.channelSequenceTextOnly = channelSequenceTextOnly;
    }

    public String getChannelTitle() {
        return channelTitle;
    }

    public void setChannelTitle(String channelTitle) {
        this.channelTitle = channelTitle;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }
}
