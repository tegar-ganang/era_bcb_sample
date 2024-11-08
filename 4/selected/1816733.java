package org.devtcg.rssreader.provider;

import org.devtcg.rssreader.provider.RSSReaderProvider;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import org.devtcg.rssreader.R;
import android.content.ContentProvider;
import android.content.ContentProviderDatabaseHelper;
import android.content.ContentURIParser;
import android.content.ContentValues;
import android.content.Context;
import android.content.QueryBuilder;
import android.content.Resources;
import android.database.ArrayListCursor;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.ContentURI;
import android.text.TextUtils;
import android.util.Log;

public class RSSReaderProvider extends ContentProvider {

    private SQLiteDatabase mDB;

    private static final String TAG = "RSSReaderProvider";

    private static final String DATABASE_NAME = "rss_reader.db";

    private static final int DATABASE_VERSION = 14;

    private static HashMap<String, String> CHANNEL_LIST_PROJECTION_MAP;

    private static HashMap<String, String> CHANNEL_ICON_PROJECTION_MAP;

    private static HashMap<String, String> POST_LIST_PROJECTION_MAP;

    private static final int CHANNELS = 1;

    private static final int CHANNEL_ID = 2;

    private static final int POSTS = 3;

    private static final int POST_ID = 4;

    private static final int CHANNEL_POSTS = 5;

    private static final int CHANNELICON_ID = 6;

    private static final ContentURIParser URL_MATCHER;

    private static class DatabaseHelper extends ContentProviderDatabaseHelper {

        protected void onCreateChannels(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE rssreader_channel (_id INTEGER PRIMARY KEY," + "	title TEXT UNIQUE, url TEXT UNIQUE, " + "    icon TEXT, icon_url TEXT, logo TEXT);");
        }

        protected void onCreatePosts(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE rssreader_post (_id INTEGER PRIMARY KEY," + "    channel_id INTEGER, title TEXT, url TEXT, " + "    posted_on DATETIME, body TEXT, author TEXT, category TEXT, georsspoint TEXT, logotype TEXT, enclosure TEXT, read INTEGER(1) DEFAULT '0');");
            db.execSQL("CREATE UNIQUE INDEX unq_post ON rssreader_post (title, url);");
            db.execSQL("CREATE INDEX idx_channel ON rssreader_post (channel_id);");
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            onCreateChannels(db);
            onCreatePosts(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + "...");
            Log.w(TAG, "Wiping out database contents...");
            db.execSQL("DROP TABLE IF EXISTS rssreader_channel;");
            db.execSQL("DROP TABLE IF EXISTS rssreader_post;");
            onCreate(db);
        }
    }

    @Override
    public boolean onCreate() {
        DatabaseHelper dbHelper = new DatabaseHelper();
        mDB = dbHelper.openDatabase(getContext(), DATABASE_NAME, null, DATABASE_VERSION);
        return (mDB == null) ? false : true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Cursor query(ContentURI url, String[] projection, String selection, String[] selectionArgs, String groupBy, String having, String sort) {
        QueryBuilder qb = new QueryBuilder();
        String defaultSort = null;
        switch(URL_MATCHER.match(url)) {
            case CHANNELS:
                qb.setTables("rssreader_channel");
                qb.setProjectionMap(CHANNEL_LIST_PROJECTION_MAP);
                defaultSort = RSSReader.Channels.DEFAULT_SORT_ORDER;
                break;
            case CHANNEL_ID:
                qb.setTables("rssreader_channel");
                qb.appendWhere("_id=" + url.getPathSegment(1));
                break;
            case CHANNELICON_ID:
                ArrayList<ArrayList> list = new ArrayList<ArrayList>();
                ArrayList<String> ofLists = new ArrayList<String>();
                ofLists.add(getIconPath(Long.parseLong(url.getPathSegment(1))));
                list.add(ofLists);
                return new ArrayListCursor(new String[] { "_data" }, list);
            case CHANNEL_POSTS:
                qb.setTables("rssreader_post");
                qb.appendWhere("channel_id=" + url.getPathSegment(1));
                qb.setProjectionMap(POST_LIST_PROJECTION_MAP);
                defaultSort = RSSReader.Posts.DEFAULT_SORT_ORDER;
                break;
            case POST_ID:
                qb.setTables("rssreader_post");
                qb.appendWhere("_id=" + url.getPathSegment(1));
                break;
            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }
        String orderBy;
        if (TextUtils.isEmpty(sort)) orderBy = defaultSort; else orderBy = sort;
        Cursor c = qb.query(mDB, projection, selection, selectionArgs, groupBy, having, orderBy);
        c.setNotificationUri(getContext().getContentResolver(), url);
        return c;
    }

    @Override
    public String getType(ContentURI url) {
        switch(URL_MATCHER.match(url)) {
            case CHANNELS:
                return "vnd.android.cursor.dir/vnd.rssreader.channel";
            case CHANNEL_ID:
                return "vnd.android.cursor.item/vnd.rssreader.channel";
            case CHANNELICON_ID:
                return "image/x-icon";
            case POSTS:
            case CHANNEL_POSTS:
                return "vnd.android.cursor.dir/vnd.rssreader.post";
            case POST_ID:
                return "vnd.android.cursor.item/vnd.rssreader.post";
            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }
    }

    private String getIconPath(long channelId) {
        try {
            return getContext().getFileStreamPath("channel" + channelId + ".ico").getAbsolutePath();
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    private String createDefaultIcon(long channelId) {
        String icoName = "channel" + channelId + ".ico";
        FileOutputStream ico = null;
        InputStream def = null;
        String name = null;
        try {
            ico = getContext().openFileOutput(icoName, Context.MODE_PRIVATE);
            def = getContext().getResources().openRawResource(R.drawable.feedicon);
            byte[] buf = new byte[1024];
            int n;
            while ((n = def.read(buf)) != -1) ico.write(buf, 0, n);
            name = getIconPath(channelId);
        } catch (Exception e) {
            Log.d(TAG, Log.getStackTraceString(e));
        } finally {
            try {
                if (ico != null) ico.close();
                if (def != null) def.close();
            } catch (Exception e) {
                return null;
            }
        }
        return name;
    }

    private long insertChannels(ContentValues values) {
        Resources r = Resources.getSystem();
        if (values.containsKey(RSSReader.Channels.TITLE) == false) values.put(RSSReader.Channels.TITLE, r.getString(android.R.string.untitled));
        long id = mDB.insert("rssreader_channel", "title", values);
        if (values.containsKey(RSSReader.Channels.ICON) == false) {
            String icoName = createDefaultIcon(id);
            if (icoName != null) {
                ContentValues update = new ContentValues();
                update.put("icon", RSSReader.Channels.CONTENT_URI.addId(id).addPath("icon").toString());
                mDB.update("rssreader_channel", update, "_id=" + id, null);
            }
        }
        return id;
    }

    private long insertPosts(ContentValues values) {
        return mDB.insert("rssreader_post", "title", values);
    }

    @Override
    public ContentURI insert(ContentURI url, ContentValues initialValues) {
        long rowID;
        ContentValues values;
        if (initialValues != null) values = new ContentValues(initialValues); else values = new ContentValues();
        ContentURI uri;
        if (URL_MATCHER.match(url) == CHANNELS) {
            rowID = insertChannels(values);
            uri = RSSReader.Channels.CONTENT_URI.addId(rowID);
        } else if (URL_MATCHER.match(url) == POSTS) {
            rowID = insertPosts(values);
            uri = RSSReader.Posts.CONTENT_URI.addId(rowID);
        } else {
            throw new IllegalArgumentException("Unknown URL " + url);
        }
        if (rowID > 0) {
            assert (uri != null);
            getContext().getContentResolver().notifyChange(uri, null);
            return uri;
        }
        throw new SQLException("Failed to insert row into " + url);
    }

    @Override
    public int delete(ContentURI url, String where, String[] whereArgs) {
        int count;
        String myWhere;
        switch(URL_MATCHER.match(url)) {
            case CHANNELS:
                count = mDB.delete("rssreader_channel", where, whereArgs);
                break;
            case CHANNEL_ID:
                myWhere = "_id=" + url.getPathSegment(1) + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");
                count = mDB.delete("rssreader_channel", myWhere, whereArgs);
                break;
            case POSTS:
                count = mDB.delete("rssreader_post", where, whereArgs);
                break;
            case POST_ID:
                myWhere = "_id=" + url.getPathSegment(1) + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");
                count = mDB.delete("rssreader_post", myWhere, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }
        getContext().getContentResolver().notifyChange(url, null);
        return count;
    }

    @Override
    public int update(ContentURI url, ContentValues values, String where, String[] whereArgs) {
        int count;
        String myWhere;
        switch(URL_MATCHER.match(url)) {
            case CHANNELS:
                count = mDB.update("rssreader_channel", values, where, whereArgs);
                break;
            case CHANNEL_ID:
                myWhere = "_id=" + url.getPathSegment(1) + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");
                count = mDB.update("rssreader_channel", values, myWhere, whereArgs);
                break;
            case POSTS:
                count = mDB.update("rssreader_post", values, where, whereArgs);
                break;
            case POST_ID:
                myWhere = "_id=" + url.getPathSegment(1) + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");
                count = mDB.update("rssreader_post", values, myWhere, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }
        getContext().getContentResolver().notifyChange(url, null);
        return count;
    }

    static {
        URL_MATCHER = new ContentURIParser(ContentURIParser.NO_MATCH);
        URL_MATCHER.addURI(RSSReader.AUTHORITY, "channels", CHANNELS);
        URL_MATCHER.addURI(RSSReader.AUTHORITY, "channels/#", CHANNEL_ID);
        URL_MATCHER.addURI(RSSReader.AUTHORITY, "channels/#/icon", CHANNELICON_ID);
        URL_MATCHER.addURI(RSSReader.AUTHORITY, "posts", POSTS);
        URL_MATCHER.addURI(RSSReader.AUTHORITY, "posts/#", POST_ID);
        URL_MATCHER.addURI(RSSReader.AUTHORITY, "postlist/#", CHANNEL_POSTS);
        CHANNEL_LIST_PROJECTION_MAP = new HashMap<String, String>();
        CHANNEL_LIST_PROJECTION_MAP.put(RSSReader.Channels._ID, "_id");
        CHANNEL_LIST_PROJECTION_MAP.put(RSSReader.Channels.TITLE, "title");
        CHANNEL_LIST_PROJECTION_MAP.put(RSSReader.Channels.URL, "url");
        CHANNEL_LIST_PROJECTION_MAP.put(RSSReader.Channels.ICON, "icon");
        CHANNEL_LIST_PROJECTION_MAP.put(RSSReader.Channels.LOGO, "logo");
        CHANNEL_ICON_PROJECTION_MAP = new HashMap<String, String>();
        CHANNEL_ICON_PROJECTION_MAP.put("_data", "icon_path");
        POST_LIST_PROJECTION_MAP = new HashMap<String, String>();
        POST_LIST_PROJECTION_MAP.put(RSSReader.Posts._ID, "_id");
        POST_LIST_PROJECTION_MAP.put(RSSReader.Posts.CHANNEL_ID, "channel_id");
        POST_LIST_PROJECTION_MAP.put(RSSReader.Posts.READ, "read");
        POST_LIST_PROJECTION_MAP.put(RSSReader.Posts.TITLE, "title");
        POST_LIST_PROJECTION_MAP.put(RSSReader.Posts.URL, "url");
        POST_LIST_PROJECTION_MAP.put(RSSReader.Posts.AUTHOR, "author");
        POST_LIST_PROJECTION_MAP.put(RSSReader.Posts.DATE, "posted_on");
        POST_LIST_PROJECTION_MAP.put(RSSReader.Posts.BODY, "body");
        POST_LIST_PROJECTION_MAP.put(RSSReader.Posts.GEORSSPOINT, "georsspoint");
        POST_LIST_PROJECTION_MAP.put(RSSReader.Posts.ENCLOSURE, "enclosure");
        POST_LIST_PROJECTION_MAP.put(RSSReader.Posts.LOGOTYPE, "logotype");
    }
}
