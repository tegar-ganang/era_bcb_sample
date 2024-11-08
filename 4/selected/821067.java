package org.jwebsocket.plugins.channels;

import java.util.Date;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jwebsocket.logging.Logging;
import org.jwebsocket.storage.ehcache.EhCacheStorage;

public class BasePublisherStore extends EhCacheStorage implements PublisherStore {

    /** logger object */
    private static Logger mLog = Logging.getLogger(BaseSubscriberStore.class);

    /** properties */
    private static final String ID = "id";

    private static final String LOGIN = "login";

    private static final String CHANNEL = "channel";

    private static final String AUTHORIZED_DATE = "authorized_date";

    private static final String LAST_PUBLISHED_DATE = "last_published_date";

    private static final String IS_AUTHORIZED = "authorized";

    /**
	 * default constructor
	 */
    public BasePublisherStore() {
        init();
    }

    /**
	 * initialize the JDBC store properties.
	 */
    private void init() {
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public Publisher getPublisher(String aId) {
        JSONObject lPublisherObject = null;
        try {
            String lStr = (String) super.get(aId);
            JSONTokener lJT = new JSONTokener(lStr);
            lPublisherObject = new JSONObject(lJT);
        } catch (Exception lEx) {
        }
        Publisher publisher = null;
        if (lPublisherObject == null) {
            return null;
        }
        try {
            String login = lPublisherObject.getString(LOGIN);
            String channel = lPublisherObject.getString(CHANNEL);
            Long authorizedDate = lPublisherObject.getLong(AUTHORIZED_DATE);
            Long lastPublishedDate = lPublisherObject.getLong(LAST_PUBLISHED_DATE);
            boolean authorized = lPublisherObject.getBoolean(IS_AUTHORIZED);
            publisher = new Publisher(aId, login, channel, new Date(authorizedDate), new Date(lastPublishedDate), authorized);
        } catch (JSONException lEx) {
            mLog.error("Error parsing json response from the channel repository: ", lEx);
        }
        return publisher;
    }

    @Override
    public boolean storePublisher(Publisher lPublisher) {
        JSONObject lPublisherObject = new JSONObject();
        try {
            lPublisherObject.put(ID, lPublisher.getId());
            lPublisherObject.put(LOGIN, lPublisher.getLogin());
            lPublisherObject.put(CHANNEL, lPublisher.getChannel());
            lPublisherObject.put(AUTHORIZED_DATE, lPublisher.getAuthorizedDate().getTime());
            lPublisherObject.put(LAST_PUBLISHED_DATE, lPublisher.getLastPublishedDate().getTime());
            lPublisherObject.put(IS_AUTHORIZED, lPublisher.isAuthorized());
            super.put(lPublisher.getId(), lPublisherObject.toString());
            return true;
        } catch (JSONException e) {
            mLog.error("Error constructing JSON data for the given publisher '" + lPublisher.getId() + "'", e);
            return false;
        }
    }

    @Override
    public void removePublisher(String aId) {
        super.remove(aId);
    }

    @Override
    public void clearPublishers() {
        super.clear();
    }

    @Override
    public int getPublisherStoreSize() {
        return size();
    }
}
