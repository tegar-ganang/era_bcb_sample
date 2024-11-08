package com.cameocontrol.cameo.action;

import com.cameocontrol.cameo.control.ChannelSet;

public class ACTChanSelection extends ACTAction {

    private ChannelSet _channels;

    void setChannels(ChannelSet cs) {
        _channels = cs;
    }

    ChannelSet getChannels() {
        return _channels;
    }

    @Override
    public void visit(IVisitor v) {
        v.applyACTChannelSelection(this);
    }
}
