package net.videgro.oma.persistence;

import java.util.ArrayList;
import net.videgro.oma.domain.Channel;
import net.videgro.oma.domain.Member;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

@SuppressWarnings("unchecked")
public class ChannelManagerDaoImpl extends HibernateDaoSupport implements IChannelManagerDao {

    public Channel getChannel(int id) {
        ArrayList<Channel> list = (ArrayList<Channel>) getHibernateTemplate().find("from Channel m where m.id=?", id);
        Channel channel = null;
        if (list.isEmpty()) {
            channel = null;
        } else {
            channel = (Channel) list.get(0);
        }
        return channel;
    }

    public Channel getChannel(String name) {
        ArrayList<Channel> list = (ArrayList<Channel>) getHibernateTemplate().find("from Channel m where m.name=?", name);
        Channel channel = null;
        if (list.isEmpty()) {
            channel = null;
        } else {
            channel = (Channel) list.get(0);
        }
        return channel;
    }

    public ArrayList<Channel> getChannelList() {
        ArrayList<Channel> channels = (ArrayList<Channel>) getHibernateTemplate().find("from Channel m order by m.name");
        return channels;
    }

    public ArrayList<Channel> getChannelList(Member member) {
        ArrayList<Channel> channels = (ArrayList<Channel>) getHibernateTemplate().find("from Channel c,Member m where m.channels=c.id and m.id=? order by c.name", member.getId());
        return channels;
    }

    public int setChannel(Channel newChannel) {
        Channel channel = newChannel;
        int id = -1;
        ArrayList<Channel> list = (ArrayList<Channel>) getHibernateTemplate().find("from Channel m where m.name=?", newChannel.getName());
        if (!list.isEmpty()) {
            channel = (Channel) list.get(0);
            channel.setDescription(newChannel.getDescription());
            channel.setName(newChannel.getName());
        }
        Session session = getHibernateTemplate().getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        session.saveOrUpdate(channel);
        id = channel.getId();
        transaction.commit();
        session.close();
        return id;
    }

    public void deleteChannel(int id) {
        deleteChannel(this.getChannel(id));
    }

    public void deleteChannel(Channel channel) {
        getHibernateTemplate().delete(channel);
    }
}
