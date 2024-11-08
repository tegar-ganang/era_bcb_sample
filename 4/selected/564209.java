package net.moep.ircservices.par;

import java.io.Serializable;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@javax.persistence.Entity
@javax.persistence.Table(name = "IRCChannel")
public class IRCChannelBean implements Serializable, IDable {

    private static final long serialVersionUID = 1L;

    private String channelname, topic;

    private List<IRCJoinBean> joins;

    private String modeline;

    /**
     * 
     */
    public IRCChannelBean() {
        super();
    }

    /**
     * @param channelname
     */
    public IRCChannelBean(String channelname) {
        super();
        this.channelname = channelname;
    }

    /**
	 * @return Returns the channelname.
	 */
    @Id
    public String getChannelname() {
        return channelname;
    }

    /**
	 * @param channelname The channelname to set.
	 */
    public void setChannelname(String channelname) {
        this.channelname = channelname;
    }

    public Object _getID() {
        return getChannelname();
    }

    /**
	 * @return Returns the topic.
	 */
    public String getTopic() {
        return topic;
    }

    /**
	 * @param topic The topic to set.
	 */
    public void setTopic(String topic) {
        this.topic = topic;
    }

    /**
	 * @return Returns the joins.
	 */
    @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.ALL, CascadeType.MERGE }, mappedBy = "channel")
    public List<IRCJoinBean> getJoins() {
        return joins;
    }

    /**
	 * @param joins The joins to set.
	 */
    public void setJoins(List<IRCJoinBean> joins) {
        this.joins = joins;
    }

    /**
	 * @return Returns the modeline.
	 */
    public String getModeline() {
        return modeline;
    }

    /**
	 * @param modeline The modeline to set.
	 */
    public void setModeline(String modeline) {
        this.modeline = modeline;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("[IRCChannel ");
        sb.append(channelname);
        sb.append(": ");
        sb.append(topic);
        if (joins != null) {
            for (IRCJoinBean join : joins) {
                sb.append("\n\tjoin: ");
                sb.append(join);
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object arg0) {
        if (arg0 == this) return true;
        if (arg0 instanceof IRCChannelBean) {
            IRCChannelBean other = (IRCChannelBean) arg0;
            return (other.getChannelname().equals(this.getChannelname()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getChannelname().hashCode();
    }
}
