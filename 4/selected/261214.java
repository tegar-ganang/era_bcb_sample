package net.sourceforge.mythtvj.mythtvprotocol;

import java.util.Date;

/**
 *
 * @author jjwin2k
 */
public class Program {

    private Channel channel;

    private Date starttime;

    private Date endtime;

    private String title;

    private String subtitle;

    private String description;

    private String[] categories;

    public Program(Channel channel, Date starttime, Date endtime, String title, String subtitle, String description, String[] categories) {
        this.channel = channel;
        this.starttime = starttime;
        this.endtime = endtime;
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;
        this.categories = categories;
    }

    public Channel getChannel() {
        return channel;
    }

    public Date getStarttime() {
        return starttime;
    }

    public Date getEndtime() {
        return endtime;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getDescription() {
        return description;
    }

    public String[] getCategory() {
        return categories;
    }
}
