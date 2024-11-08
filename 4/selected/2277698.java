package org.vosao.service.front.impl;

import org.vosao.service.front.ChannelApiService;
import org.vosao.service.impl.AbstractServiceImpl;
import com.google.appengine.api.channel.ChannelServiceFactory;

/**
 * @author Alexander Oleynik
 */
public class ChannelApiServiceImpl extends AbstractServiceImpl implements ChannelApiService {

    @Override
    public String createToken(String clientId) {
        return ChannelServiceFactory.getChannelService().createChannel(clientId);
    }
}
