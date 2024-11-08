package com.google.code.guidatv.server.service.rest;

import java.util.ArrayList;
import java.util.List;
import org.restlet.resource.ServerResource;
import com.google.code.guidatv.client.service.ChannelService;
import com.google.code.guidatv.client.service.impl.ChannelServiceImpl;
import com.google.code.guidatv.model.Channel;
import com.google.code.guidatv.rest.ChannelsResource;

public class ChannelsServerResource extends ServerResource implements ChannelsResource {

    private ChannelService channelService = new ChannelServiceImpl();

    @Override
    public List<Channel> retrieve() {
        return new ArrayList<Channel>(channelService.getChannels());
    }
}
