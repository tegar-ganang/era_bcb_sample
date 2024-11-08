package net.sf.jvdr.mbean.epg;

import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import net.sf.jvdr.mbean.AbstractJvdrMbean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hampelratte.svdrp.responses.highlevel.Channel;
import org.hampelratte.svdrp.responses.highlevel.EPGEntry;

public class ChannelEpgBean extends AbstractJvdrMbean {

    static Log logger = LogFactory.getLog(ChannelEpgBean.class);

    private Channel channel;

    private List<EPGEntry> lEPG;

    @PostConstruct
    public void init() {
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        HttpServletRequest request = (HttpServletRequest) ec.getRequest();
        int chNu = new Integer(request.getParameter("chNu"));
        channel = this.getCache().getChannel(chNu);
        lEPG = this.getCache().getEpgForChNu(chNu, new Date());
        logger.debug(lEPG.size() + " EPGs fetched for " + channel.getChannelNumber());
    }

    public Channel getChannel() {
        return channel;
    }

    public List<EPGEntry> getlEPG() {
        return lEPG;
    }
}
