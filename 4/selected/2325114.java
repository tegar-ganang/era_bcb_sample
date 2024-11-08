package net.moep.ircservices.ejb;

import static net.moep.ircservices.common.Labels.ALREADY_EXISTS;
import static net.moep.ircservices.common.Labels.NOTFOUND;
import static net.moep.ircservices.common.Labels.NULL;
import static net.moep.ircservices.common.Labels.REMOVED;
import static net.moep.ircservices.common.Sources.*;
import static net.moep.util.Util.flag;
import static net.moep.util.Util.resolve;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import net.moep.ircservices.common.Sources;
import net.moep.ircservices.par.IRCChannelBean;
import net.moep.ircservices.par.IRCJoinBean;
import net.moep.ircservices.par.IRCUserBean;
import net.moep.ircservices.par.common.ReturnValue;
import net.moep.ircservices.par.common.Source;
import org.hibernate.exception.ConstraintViolationException;

;

@Stateless
public class TestBean extends ConvenientBean implements Test {

    @PersistenceContext
    EntityManager em;

    public ReturnValue<IRCUserBean> createUser(String nickname, String username, String hostname, String realname) {
        ReturnValue<IRCUserBean> retval = new ReturnValue<IRCUserBean>();
        setFlags(retval, NULL, flag(nickname == null, NICKNAME), flag(username == null, USERNAME), flag(hostname == null, HOSTNAME), flag(realname == null, REALNAME));
        if (!retval.hasMessages()) {
            IRCUserBean oldObj = null;
            try {
                oldObj = IRCUserBean.findByNickname(em, nickname);
            } catch (NoResultException e) {
                oldObj = null;
            }
            if (oldObj == null) {
                IRCUserBean newObj = new IRCUserBean(nickname, username, hostname, realname);
                newObj.setServername("ficken");
                try {
                    em.persist(newObj);
                    retval.setValue(newObj);
                } catch (ConstraintViolationException e) {
                    retval.addMessage(ALREADY_EXISTS, NICKNAME, nickname);
                }
            } else {
                retval.addMessage(ALREADY_EXISTS, NICKNAME, nickname);
            }
        }
        return retval;
    }

    public ReturnValue<IRCUserBean> findUser(String nickname) {
        ReturnValue<IRCUserBean> retval = new ReturnValue<IRCUserBean>();
        IRCUserBean val = null;
        Source source = Sources.NICKNAME;
        try {
            if (nickname == null) {
                setNullFlag(retval, source, nickname);
            } else {
                val = IRCUserBean.findByNickname(em, nickname);
            }
        } catch (NoResultException e) {
            setNotFoundFlag(retval, source, nickname);
        }
        if (val != null) retval.setValue(val);
        return retval;
    }

    public ReturnValue<IRCChannelBean> createChannel(String channelname) {
        ReturnValue<IRCChannelBean> retval = new ReturnValue<IRCChannelBean>();
        Source source = Sources.CHANNEL;
        if (channelname == null) {
            setNullFlag(retval, source, channelname);
        } else {
            IRCChannelBean newObj = new IRCChannelBean();
            IRCChannelBean oldObj = resolve(em, newObj, channelname);
            if (oldObj == null) {
                newObj.setChannelname(channelname);
                try {
                    em.persist(newObj);
                    retval.setValue(newObj);
                } catch (ConstraintViolationException e) {
                    retval.addMessage(ALREADY_EXISTS, CHANNEL, channelname);
                }
            } else {
                retval.addMessage(ALREADY_EXISTS, source, channelname);
            }
        }
        return retval;
    }

    public ReturnValue<IRCChannelBean> findChannelWithJoins(String channelname) {
        ReturnValue<IRCChannelBean> retval = new ReturnValue<IRCChannelBean>();
        if (channelname == null) setNullFlag(retval, CHANNEL, channelname); else {
            IRCChannelBean channel = new IRCChannelBean();
            channel = resolve(em, channel, channelname);
            if (channel != null) {
                channel.getJoins().size();
                retval.setValue(channel);
            } else {
                retval.addMessage(NOTFOUND, CHANNEL, channelname);
            }
        }
        return retval;
    }

    public ReturnValue<IRCJoinBean> joinUser(IRCUserBean user, IRCChannelBean channel, String modeline) {
        ReturnValue<IRCJoinBean> retval = new ReturnValue<IRCJoinBean>();
        if (!setFlags(retval, NULL, flag(user == null, USERNAME), flag(channel == null, CHANNEL))) {
            user = resolve(em, user);
            channel = resolve(em, channel);
            if (!setFlags(retval, NOTFOUND, flag(user == null, USERNAME), flag(channel == null, CHANNEL))) {
                IRCJoinBean newObj = new IRCJoinBean(channel, user);
                newObj.setModeline(modeline);
                IRCJoinBean oldObj = IRCJoinBean.findJoinByUserAndChannel(em, user, channel);
                if (oldObj == null) {
                    try {
                        em.persist(newObj);
                        retval.setValue(newObj);
                    } catch (ConstraintViolationException e) {
                        retval.addMessage(ALREADY_EXISTS, JOIN, user.getNickname(), user.getId() + "", channel.getChannelname());
                    }
                } else {
                    retval.addMessage(ALREADY_EXISTS, JOIN);
                }
            }
        }
        return retval;
    }

    public ReturnValue partUser(IRCUserBean user, IRCChannelBean channel) {
        ReturnValue retval = new ReturnValue();
        try {
            if (!setFlags(retval, NULL, flag(user == null, USERID, user._getID()), flag(channel == null, CHANNEL, channel.getChannelname()))) {
                user = resolve(em, user);
                channel = resolve(em, channel);
                if (!setFlags(retval, NOTFOUND, flag(user == null, USERID, user.getNickname(), user), flag(channel == null, CHANNEL, channel))) {
                    try {
                        ReturnValue<IRCJoinBean> joinRV = getJoin(channel, user);
                        List<IRCJoinBean> currentjoines = channel.getJoins();
                        if (currentjoines.size() == 1) {
                            em.remove(channel);
                            retval.addMessage(REMOVED, CHANNEL);
                        } else {
                            em.remove(em.find(IRCJoinBean.class, joinRV.getValue()._getID()));
                            em.refresh(channel);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
        return retval;
    }

    public ReturnValue<IRCJoinBean> getJoin(IRCChannelBean channel, IRCUserBean user) {
        ReturnValue<IRCJoinBean> retval = new ReturnValue<IRCJoinBean>();
        if (!setFlags(retval, NULL, flag(channel == null, CHANNEL, channel.getChannelname()), flag(user == null, USERID, user._getID(), user.getNickname()))) {
            IRCJoinBean asdf = IRCJoinBean.findJoinByUserAndChannel(em, user, channel);
            if (asdf == null) setNotFoundFlag(retval, JOIN, user._getID(), user.getNickname(), channel.getChannelname()); else {
                retval.setValue(asdf);
            }
        }
        return retval;
    }
}
