package net.moep.ircservices.par;

import java.io.Serializable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.Table;

@NamedQueries({ @NamedQuery(name = "findJoinByUserAndChannel", query = "SELECT Object(o) FROM IRCJoinBean o WHERE o.user = :user AND o.channel = :channel"), @NamedQuery(name = "findJoinByUserAndChannelPlain", query = "SELECT OBJECT(o) FROM IRCJoinBean o WHERE o.user.nickname = :nickname AND o.channel.channelname = :channelname") })
@Entity
@Table(name = "IRCJoin")
public class IRCJoinBean implements Serializable, IDable {

    public static IRCJoinBean findJoinByUserAndChannel(EntityManager em, IRCUserBean user, IRCChannelBean channel) {
        IRCJoinBean val = null;
        try {
            val = (IRCJoinBean) em.createNamedQuery("findJoinByUserAndChannel").setParameter("user", user).setParameter("channel", channel).getSingleResult();
        } catch (NoResultException e) {
            val = null;
        }
        return val;
    }

    public static IRCJoinBean findJoinByUserAndChannelPlain(EntityManager em, String nickname, String channelname) {
        IRCJoinBean val = null;
        try {
            val = (IRCJoinBean) em.createNamedQuery("findJoinByUserAndChannelPlain").setParameter("nickname", nickname).setParameter("channelname", channelname).getSingleResult();
        } catch (NoResultException e) {
            val = null;
        }
        return val;
    }

    private static final long serialVersionUID = 1L;

    private String modeline;

    private IRCChannelBean channel;

    private IRCUserBean user;

    private Long id;

    public IRCJoinBean(IRCChannelBean channel, IRCUserBean user) {
        this.channel = channel;
        this.user = user;
    }

    public IRCJoinBean() {
    }

    /**
     * @return Returns the id.
     */
    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }

    /**
     * @param id
     *        The id to set.
     */
    public void setId(Long id) {
        this.id = id;
    }

    public Object _getID() {
        return getId();
    }

    /**
     * @return Returns the channel.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "channel_id")
    public IRCChannelBean getChannel() {
        return channel;
    }

    /**
     * @param channel
     *        The channel to set.
     */
    public void setChannel(IRCChannelBean channel) {
        this.channel = channel;
    }

    /**
     * @return Returns the user.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    public IRCUserBean getUser() {
        return user;
    }

    /**
     * @param user
     *        The user to set.
     */
    public void setUser(IRCUserBean user) {
        this.user = user;
    }

    /**
     * @return Returns the modeline.
     */
    public String getModeline() {
        return modeline;
    }

    /**
     * @param modeline
     *        The modeline to set.
     */
    public void setModeline(String modeline) {
        this.modeline = modeline;
    }

    @Override
    public String toString() {
        return "[IRCJoin " + getUser().getNickname() + "(" + getUser().getId() + ")|" + getChannel().getChannelname() + " (" + modeline + ")]";
    }

    @Override
    public boolean equals(Object arg0) {
        if (arg0 == this) return true;
        if (arg0 instanceof IRCJoinBean) {
            IRCJoinBean other = (IRCJoinBean) arg0;
            if (other.getChannel() == null) throw new IllegalArgumentException("channel must not be null");
            if (other.getUser() == null) throw new IllegalArgumentException("user must not be null");
            if (this.getChannel() == null) throw new IllegalArgumentException("channel must not be null");
            if (this.getUser() == null) throw new IllegalArgumentException("user must not be null");
            return (other.getChannel().equals(this.getChannel()) && other.getUser().equals(this.getUser()));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getUser().hashCode() + getChannel().hashCode();
    }
}
