package com.appspot.gossipscity.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jdo.PersistenceManager;
import javax.jdo.Query;
import org.datanucleus.store.appengine.query.JDOCursorHelper;
import com.google.appengine.api.datastore.Cursor;

public class AppMisc {

    public static String cursorString;

    public static void createNewPost(String name, String rssLink, int ratingCount) {
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Channels chnl = null;
        ratingCount = 0;
        name = removeInvalidCharacters(name);
        rssLink = removeInvalidCharacters(rssLink);
        if (name.length() == 0 || rssLink.length() == 0) return;
        try {
            chnl = new Channels();
            chnl.setBasicInfo(name, rssLink, ratingCount);
            pm.makePersistent(chnl);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            pm.close();
        }
    }

    /**
	 * It removes tilda and pipe character
	 */
    private static String removeInvalidCharacters(String strPost) {
        if (strPost.contains("|")) strPost = strPost.replace("|", " ");
        if (strPost.contains("~")) strPost = strPost.replace("~", " ");
        return strPost;
    }

    public static List<Channels> getChannels() {
        List<Channels> rslt = null;
        PersistenceManager pm = PMF.get().getPersistenceManager();
        Query query = pm.newQuery(Channels.class);
        query.setOrdering("ratingCount desc");
        try {
            query.setRange(0, 30);
            rslt = (List<Channels>) query.execute();
            Cursor cursor = JDOCursorHelper.getCursor(rslt);
            cursorString = cursor.toWebSafeString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
        return (List<Channels>) pm.detachCopyAll(rslt);
    }

    public static List<Channels> getMorePosts(String cursorString) {
        Query query = null;
        List<Channels> rslt = null;
        PersistenceManager pm = PMF.get().getPersistenceManager();
        try {
            query = pm.newQuery(Channels.class);
            query.setOrdering("ratingCount desc");
            if (cursorString != null) {
                Cursor cursor = Cursor.fromWebSafeString(cursorString);
                Map<String, Object> extensionMap = new HashMap<String, Object>();
                extensionMap.put(JDOCursorHelper.CURSOR_EXTENSION, cursor);
                query.setExtensions(extensionMap);
            }
            query.setRange(0, 30);
            rslt = (List<Channels>) query.execute();
            Cursor cursor = JDOCursorHelper.getCursor(rslt);
            AppMisc.cursorString = cursor.toWebSafeString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
        return rslt;
    }
}
