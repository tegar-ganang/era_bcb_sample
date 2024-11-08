package com.langerra.server.rpc;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.langerra.client.channel.rpc.RemoteChannelService;
import com.langerra.server.channel.ChannelServiceFactory;
import com.langerra.shared.channel.Channel;
import com.langerra.shared.channel.ChannelMessage;

public class RemoteChannelServiceImpl extends RemoteServiceServlet implements RemoteChannelService {

    private static final long serialVersionUID = 7556490504908674916L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String message = new Date().toString();
        ChannelServiceFactory.getChannelService().<ChannelMessage<String>>getChannel("gwt-channel", false).write(new ChannelMessage<String>(message));
        resp.getWriter().write("<html><body><h1>$</h1></body></html>".replace("$", message));
    }

    @Override
    public <T extends Serializable> ChannelMessage<T> get(String channelName, Long key) {
        return new ChannelMessage<T>(ChannelServiceFactory.getChannelService().<T>getServicePool().get(key));
    }

    @Override
    public <T extends Serializable> ChannelMessage<T> put(String channelName, ChannelMessage<T> value) {
        return ChannelServiceFactory.getChannelService().<T>getServicePool().getMessage(value.getValue());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Serializable> ArrayList<ChannelMessage<T>> readAll(String channelName) {
        final ArrayList<T> result = new ArrayList<T>();
        final Channel<T> channel = ChannelServiceFactory.getChannelService().<T>getChannel(channelName, false);
        long deadline = System.currentTimeMillis() + 27 * 1000;
        while (System.currentTimeMillis() < deadline && result.isEmpty()) {
            result.addAll(channel.readAll());
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                break;
            }
        }
        return (ArrayList<ChannelMessage<T>>) result;
    }

    @Override
    public <T extends Serializable> Boolean writeAll(String channelName, ArrayList<ChannelMessage<T>> messages) {
        ChannelServiceFactory.getChannelService().<ChannelMessage<T>>getChannel(channelName, false).writeAll(messages);
        return Boolean.TRUE;
    }
}
