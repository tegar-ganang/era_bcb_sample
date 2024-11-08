package net.sf.jvdr.mbean.epg;

import java.util.List;
import javax.annotation.PostConstruct;
import net.sf.jvdr.mbean.AbstractJvdrMbean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hampelratte.svdrp.responses.highlevel.Channel;

public class ChannelOverviewBean extends AbstractJvdrMbean {

    static Log logger = LogFactory.getLog(ChannelOverviewBean.class);

    private List<Channel> lChannel;

    @PostConstruct
    public void init() {
        logger.info("Initializing CacheUpdater ...");
        lChannel = this.getCache().getChannelList();
    }

    public List<Channel> getlChannel() {
        return lChannel;
    }

    public void setlChannel(List<Channel> lChannel) {
        this.lChannel = lChannel;
    }
}
