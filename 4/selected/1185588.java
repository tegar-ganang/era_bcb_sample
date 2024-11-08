package ru.pit.tvlist.persistence.dao.httpclient;

import java.util.GregorianCalendar;
import ru.pit.tvlist.persistence.dao.IChannelSourceDAO;
import ru.pit.tvlist.persistence.exception.PersistenceException;

public class ChannelHTTPClientDAO extends AHTTPClientDAO implements IChannelSourceDAO {

    /**
     * @see ru.pit.tvlist.persistence.dao.ChannelSourceDAO#getChannelHTMLData(java.lang.String, java.util.Date)
     */
    public String getChannelSourceData(String extId, GregorianCalendar day, int epgSourceType) throws PersistenceException {
        return null;
    }
}
