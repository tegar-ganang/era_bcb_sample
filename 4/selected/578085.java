package ru.pit.tvlist.persistence.service;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import ru.pit.tvlist.epgparser.EPGSourceFactory;
import ru.pit.tvlist.epgparser.IEPGSource;
import ru.pit.tvlist.epgparser.exceptions.EPGParserException;
import ru.pit.tvlist.persistence.dao.DAOFactory;
import ru.pit.tvlist.persistence.dao.IBroadcastDAO;
import ru.pit.tvlist.persistence.dao.IBroadcastSourceDAO;
import ru.pit.tvlist.persistence.dao.IChannelDAO;
import ru.pit.tvlist.persistence.dao.IChannelSourceDAO;
import ru.pit.tvlist.persistence.dao.httpclient.HTTPClientSessionFactory;
import ru.pit.tvlist.persistence.dao.httpclient.IHTTPClientSessionFactory;
import ru.pit.tvlist.persistence.domain.Broadcast;
import ru.pit.tvlist.persistence.domain.Channel;
import ru.pit.tvlist.persistence.exception.PersistenceException;
import ru.pit.tvlist.persistence.exception.TVListServiceException;

public class TVListService {

    private IChannelDAO channelDAO = null;

    private IBroadcastDAO broadcastDAO = null;

    /**
     * Constructs TVListService object using specified DAOs
     * @param channelDAO {@link IChannelDAO}
     * @param broadcastDAO {@line {@link IBroadcastDAO}}
     * @param channelSourceDAO {@link IChannelSourceDAO}
     * @param broadcastSourceDAO {@link IBroadcastSourceDAO}
     */
    public TVListService(IChannelDAO channelDAO, IBroadcastDAO broadcastDAO) {
        this.channelDAO = channelDAO;
        this.broadcastDAO = broadcastDAO;
    }

    public IBroadcastDAO getBroadcastDAO() {
        return broadcastDAO;
    }

    public void setBroadcastDAO(IBroadcastDAO broadcastDAO) {
        this.broadcastDAO = broadcastDAO;
    }

    public IChannelDAO getChannelDAO() {
        return channelDAO;
    }

    public void setChannelDAO(IChannelDAO channelDAO) {
        this.channelDAO = channelDAO;
    }

    /**
     * Returns list of all channels
     * @return a List of {@link Channel}
     * @throws PersistenceException
     */
    public List getChannels() throws PersistenceException {
        return channelDAO.getAll();
    }

    /**
     * Returns list of broadcast for particular channel,day and time period
     * @param channelId ID of channel
     * @param day day of broadcasts
     * @param fromTime from time
     * @param toTime to time
     * @return list of {@link Channel}
     * @throws PersistenceException
     */
    public List getBroadcastsByChannel(Long channelId, GregorianCalendar day, GregorianCalendar fromTime, GregorianCalendar toTime) throws PersistenceException {
        fromTime.set(day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH));
        toTime.set(day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH));
        return broadcastDAO.getByChannel(channelId, fromTime, toTime);
    }

    /**
     * Returns list of broadcast for particular day and time period.
     * see {@link TVListService#getBroadcastsByChannel(Long, Date, Date, Date)}
     * @param day day of broadcasts
     * @param startTime from time
     * @param stopTime to time
     * @return
     * @throws PersistenceException
     */
    public List getBroadcasts(GregorianCalendar day, GregorianCalendar fromTime, GregorianCalendar toTime) throws PersistenceException {
        return getBroadcastsByChannel(null, day, fromTime, toTime);
    }

    /**
     * Save broadcasts data to DB
     * @param broadcasts List of {@link Broadcast}
     * @param channelId ID of broadcasts channel
     * @throws PersistenceException 
     */
    public void saveBroadcasts(List broadcasts, Long channelId) throws PersistenceException {
        Channel channel = channelDAO.getById(channelId);
        Iterator iter = broadcasts.iterator();
        while (iter.hasNext()) {
            Broadcast broadcast = (Broadcast) iter.next();
            broadcast.setChannel(channel);
            broadcastDAO.save(broadcast);
        }
    }

    /**
     * Save channel to DB
     * @param channel {@link Channel}
     * @throws PersistenceException 
     */
    public Long saveChannel(Channel channel) throws PersistenceException {
        return channelDAO.save(channel);
    }

    public void loadBroadcastsByChannel(List channelsIds, GregorianCalendar date, int epgSourceType) throws TVListServiceException {
        IHTTPClientSessionFactory clientSessionFactory = null;
        try {
            clientSessionFactory = new HTTPClientSessionFactory();
            IChannelSourceDAO channelSourceDAO = DAOFactory.getChannelSourceDAO(clientSessionFactory);
            IEPGSource epgSource = EPGSourceFactory.getEPGSource(epgSourceType);
            Iterator iter = channelsIds.iterator();
            while (iter.hasNext()) {
                Long channelId = (Long) iter.next();
                String channelExtId = channelDAO.getExtIdById(channelId);
                String htmlData = channelSourceDAO.getChannelSourceData(channelExtId, date, epgSourceType);
                List broadcasts = epgSource.parseChannelSource(htmlData, date);
                saveBroadcasts(broadcasts, channelId);
            }
        } catch (PersistenceException e) {
            throw new TVListServiceException(e);
        } catch (EPGParserException e) {
            throw new TVListServiceException(e);
        } finally {
            try {
                clientSessionFactory.closeSessions();
            } catch (PersistenceException e) {
            }
        }
    }
}
