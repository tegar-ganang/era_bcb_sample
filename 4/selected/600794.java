package org.jwebsocket.plugins.channels;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jwebsocket.logging.Logging;
import org.jwebsocket.storage.ehcache.EhCacheStorage;

/**
 * JDBC store based extension of SubscriberStore interface.
 * 
 * @author puran
 * @version $Id: BaseSubscriberStore.java 1441 2011-01-17 16:42:52Z fivefeetfurther $
 */
public class BaseSubscriberStore extends EhCacheStorage implements SubscriberStore {

    /** logger object */
    private static Logger logger = Logging.getLogger(BaseSubscriberStore.class);

    /** properties */
    private static final String ID = "id";

    private static final String CHANNELS = "channels";

    private static final String LOGGED_IN_TIME = "logged_in_time";

    /**
	 * default constructor
	 */
    public BaseSubscriberStore() {
        init();
    }

    /**
	 * initialize the JDBC store properties.
	 */
    private void init() {
    }

    @Override
    public Subscriber getSubscriber(String aId) {
        JSONObject lSubscriberObject = null;
        try {
            String lStr = (String) super.get(aId);
            JSONTokener lJT = new JSONTokener(lStr);
            lSubscriberObject = new JSONObject(lJT);
        } catch (Exception lEx) {
        }
        if (lSubscriberObject == null) {
            return null;
        }
        List<String> lChannels = new ArrayList<String>();
        Subscriber lSubscriber = null;
        try {
            long lLoggedInTime = lSubscriberObject.getLong(LOGGED_IN_TIME);
            JSONArray lSubscribersArray = lSubscriberObject.getJSONArray(CHANNELS);
            if (lSubscribersArray != null) {
                for (int lIdx = 0; lIdx < lSubscribersArray.length(); lIdx++) {
                    JSONObject lObj = lSubscribersArray.getJSONObject(lIdx);
                    String lChannelId = lObj.getString(ID);
                    lChannels.add(lChannelId);
                }
            }
            lSubscriber = new Subscriber(aId, new Date(lLoggedInTime), lChannels);
        } catch (JSONException lEx) {
            logger.error("Error parsing json response from the channel repository:", lEx);
        }
        return lSubscriber;
    }

    @Override
    public boolean storeSubscriber(Subscriber aSubscriber) {
        JSONObject lSubscriberObject = new JSONObject();
        try {
            lSubscriberObject.put(ID, aSubscriber.getId());
            lSubscriberObject.put(LOGGED_IN_TIME, aSubscriber.getLoggedInTime().getTime());
            JSONArray lJSONArray = new JSONArray();
            for (String lChannel : aSubscriber.getChannels()) {
                JSONObject lChannelObject = new JSONObject();
                lChannelObject.put(ID, lChannel);
                lJSONArray.put(lChannelObject);
            }
            lSubscriberObject.put(CHANNELS, lJSONArray);
            super.put(aSubscriber.getId(), lSubscriberObject.toString());
            return true;
        } catch (JSONException lEx) {
            logger.error("Error constructing JSON data for the given subscriber '" + aSubscriber.getId() + "'", lEx);
            return false;
        }
    }

    @Override
    public void removeSubscriber(String id) {
        super.remove(id);
    }

    @Override
    public void clearSubscribers() {
        super.clear();
    }

    @Override
    public int getSubscribersStoreSize() {
        return super.size();
    }
}
