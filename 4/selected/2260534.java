package net.moep.ircservices.ejb;

import javax.ejb.Stateless;
import static net.moep.util.Util.flag;
import static net.moep.util.Util.resolve;
import static net.moep.ircservices.common.Labels.*;
import static net.moep.ircservices.common.Sources.*;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import com.sun.org.apache.xerces.internal.impl.XMLEntityManager.Entity;
import net.moep.ircservices.ejb.Zustandsabfrage;
import net.moep.ircservices.par.IRCChannelBean;
import net.moep.ircservices.par.IRCJoinBean;
import net.moep.ircservices.par.IRCUserBean;
import net.moep.ircservices.par.common.ReturnValue;

/**
 * @author schuppi
 * 
 */
@Stateless
public class ZustandsabfrageBean extends ConvenientBean implements Zustandsabfrage {

    @PersistenceContext
    EntityManager em;

    public ReturnValue<IRCChannelBean> getChannel(String channelname) {
        ReturnValue<IRCChannelBean> retval = new ReturnValue<IRCChannelBean>();
        if (channelname == null) {
            setNullFlag(retval, CHANNEL);
        } else {
            IRCChannelBean channel = resolve(em, IRCChannelBean.class, channelname);
            if (channel == null) {
                retval.setValue(channel);
            } else {
                retval.addMessage(NOTFOUND, CHANNEL);
            }
        }
        return retval;
    }

    public ReturnValue<IRCChannelBean> getChannelWithJoins(String channelname) {
        ReturnValue<IRCChannelBean> retval = new ReturnValue<IRCChannelBean>();
        if (channelname == null) {
            setNullFlag(retval, CHANNEL);
        } else {
            IRCChannelBean channel = resolve(em, IRCChannelBean.class, channelname);
            if (channel != null) {
                channel.getJoins().size();
                retval.setValue(channel);
            } else {
                retval.addMessage(NOTFOUND, CHANNEL);
            }
        }
        return retval;
    }

    public ReturnValue<IRCJoinBean> getJoin(String channelname, String nickname) {
        ReturnValue<IRCJoinBean> retval = new ReturnValue<IRCJoinBean>();
        if (channelname == null) retval.addMessage(NULL, CHANNEL);
        if (nickname == null) retval.addMessage(NULL, NICKNAME);
        if (!retval.hasMessages()) {
            IRCJoinBean join = IRCJoinBean.findJoinByUserAndChannelPlain(em, nickname, channelname);
            if (join != null) {
                retval.setValue(join);
            } else {
                setNotFoundFlag(retval, JOIN);
            }
        }
        return retval;
    }

    public ReturnValue<IRCUserBean> getUser(String nickname) {
        ReturnValue<IRCUserBean> retval = new ReturnValue<IRCUserBean>();
        if (nickname == null) {
            setNullFlag(retval, NICKNAME);
        } else {
            IRCUserBean user = IRCUserBean.findByNickname(em, nickname);
            if (user != null) {
                retval.setValue(user);
            } else {
                setNotFoundFlag(retval, NICKNAME);
            }
        }
        return retval;
    }

    public ReturnValue<IRCUserBean> getUserWithJoins(String nickname) {
        ReturnValue<IRCUserBean> retval = new ReturnValue<IRCUserBean>();
        if (nickname == null) {
            setNullFlag(retval, NICKNAME);
        } else {
            IRCUserBean user = IRCUserBean.findByNickname(em, nickname);
            if (user != null) {
                user.getJoins().size();
                retval.setValue(user);
            } else {
                setNotFoundFlag(retval, NICKNAME);
            }
        }
        return retval;
    }

    public Integer getUserCount() {
        return (Integer) em.createQuery("SELECT count(o) FROM IRCUserBean o").getSingleResult();
    }
}
