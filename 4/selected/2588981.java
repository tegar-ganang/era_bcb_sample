package net.videgro.oma.services;

import java.util.List;
import net.videgro.oma.domain.Channel;
import net.videgro.oma.domain.Member;
import org.springframework.remoting.jaxrpc.ServletEndpointSupport;

@SuppressWarnings("unchecked")
public class ChannelServiceEP extends ServletEndpointSupport implements IChannelService {

    private IChannelService service;

    protected void onInit() {
        this.service = (IChannelService) getWebApplicationContext().getBean("channelService");
    }

    public List getChannelList() {
        return service.getChannelList();
    }

    public List getChannelListByMember(Member member) {
        return service.getChannelListByMember(member);
    }

    public Channel getChannel(int id) {
        return service.getChannel(id);
    }

    public Channel getChannelByName(String name) {
        return service.getChannelByName(name);
    }

    public int setChannel(Channel m) {
        return service.setChannel(m);
    }

    public void deleteChannel(int id) {
        service.deleteChannel(id);
    }

    public void setChannelList(Channel[] l, int who) {
        service.setChannelList(l, who);
    }
}
