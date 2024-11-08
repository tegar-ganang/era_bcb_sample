package com.jmircwordgames;

import java.util.Vector;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class Database {

    String profilename;

    String nick;

    String altnick;

    String host;

    int port;

    String channels;

    String username;

    String realname;

    String passwd;

    String botname;

    int[] idxarray;

    int profileidx = -1;

    boolean header = true;

    boolean timestamp = false;

    boolean usecolor = true;

    boolean usemirccol = false;

    boolean usepoll = false;

    boolean showinput = false;

    String encoding = "ISO-8859-1";

    boolean utf8detect = true;

    boolean utf8output = false;

    String hilight = "";

    int buflines = 50;

    boolean usehttp = false;

    String gwhost = "";

    int gwport = 8080;

    String gwpasswd = "";

    int polltime = 10;

    private static final String STORE_CONFIG = "jmirccfg";

    private static final String STORE_PROFILE = "jmircprof";

    private static final String STORE_FAVS = "jmircfavs";

    private static final String STORE_CROSSWORDS = "jmirccrosswds";

    private static final String STORE_PROFILE_CREATE = "create table jmircprof (_id integer primary key autoincrement, " + "profilename text not null, " + "nick text not null, " + "altnick text null, " + "host text not null, " + "port text not null, " + "channels text not null, " + "username text not null, " + "realname text not null, " + "password text not null, " + "botname text not null);";

    private static final String STORE_FAVS_CREATE = "create table jmircfavs (fav text not null);";

    private static final String STORE_CROSSWORDS_CREATE = "create table jmirccrosswds(_id integer primary key autoincrement, crosswd text not null);";

    private static final String DATABASE_NAME = "jmirc";

    private static final int DATABASE_VERSION = 1;

    private final Context jmirc;

    private DatabaseHelper dbHelper;

    private SQLiteDatabase db;

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(STORE_PROFILE_CREATE);
            db.execSQL(STORE_FAVS_CREATE);
            db.execSQL(STORE_CROSSWORDS_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w("jmIrc", "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + STORE_PROFILE);
            db.execSQL("DROP TABLE IF EXISTS " + STORE_FAVS);
            db.execSQL("DROP TABLE IF EXISTS " + STORE_CROSSWORDS);
            onCreate(db);
        }
    }

    public Database(Context jmirc) {
        this.jmirc = jmirc;
        this.dbHelper = new DatabaseHelper(jmirc);
        this.db = dbHelper.getWritableDatabase();
    }

    /**
	 * returns 
	 */
    public void load() {
        SharedPreferences config = jmirc.getSharedPreferences(STORE_CONFIG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = config.edit();
        String version = config.getString("version", "");
        if (!version.equals(JmIrc.VERSION)) {
            editor.clear();
            editor.putString("version", JmIrc.VERSION);
            db.execSQL("DROP TABLE IF EXISTS " + STORE_PROFILE);
            save_profile();
            save_advanced();
            save_http();
        } else {
            profileidx = config.getInt("profileidx", -1);
            header = config.getBoolean("header", false);
            timestamp = config.getBoolean("timestamp", false);
            usecolor = config.getBoolean("usecolor", false);
            usemirccol = config.getBoolean("usemirccol", false);
            usepoll = config.getBoolean("usepoll", false);
            showinput = config.getBoolean("showinput", true);
            encoding = config.getString("encoding", "ISO-8859-1");
            utf8detect = config.getBoolean("utf8detect", false);
            utf8output = config.getBoolean("utf8output", false);
            buflines = config.getInt("buflines", 50);
            hilight = config.getString("hilight", "");
            usehttp = config.getBoolean("usehttp", false);
            gwhost = config.getString("gwhost", "");
            gwport = config.getInt("gwport", 8080);
            gwpasswd = config.getString("gwpasswd", "");
            polltime = config.getInt("polltime", 10);
        }
        editor.commit();
        getProfiles();
        setProfile(profileidx);
    }

    public String[] getProfiles() {
        String[] ret = null;
        Cursor c = db.query(STORE_PROFILE, new String[] { "_id", "profilename" }, null, null, null, null, null);
        if (c.getCount() == 0) {
            idxarray = new int[0];
            ret = new String[0];
        } else {
            int profiles = c.getCount();
            ret = new String[profiles];
            idxarray = new int[profiles];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = c.getString(c.getColumnIndex("profilename"));
                idxarray[i] = c.getInt(c.getColumnIndex("_id"));
                if (!c.isLast()) c.moveToNext();
            }
        }
        return ret;
    }

    public void setProfile(int index) {
        if (index < 0) {
            profilename = "jmircuser";
            nick = "jmircuser";
            altnick = "";
            host = "eu.undernet.org";
            port = 6667;
            channels = "#jmircwordgames";
            username = "";
            realname = "jmIrc user";
            passwd = "";
            botname = "elsie";
        } else {
            Cursor c = db.query(STORE_PROFILE, new String[] { "profilename", "nick", "altnick", "host", "port", "channels", "username", "realname", "passwd", "botname" }, "_id = " + index, null, null, "_id", null);
            profilename = c.getString(c.getColumnIndex("profilename"));
            nick = c.getString(c.getColumnIndex("nick"));
            altnick = c.getString(c.getColumnIndex("altnick"));
            host = c.getString(c.getColumnIndex("host"));
            port = c.getInt(c.getColumnIndex("port"));
            channels = c.getString(c.getColumnIndex("channels"));
            username = c.getString(c.getColumnIndex("username"));
            realname = c.getString(c.getColumnIndex("realname"));
            passwd = c.getString(c.getColumnIndex("passwd"));
            botname = c.getString(c.getColumnIndex("botname"));
        }
    }

    public void addProfile() {
        db.insert(STORE_PROFILE, null, getProfileParams());
    }

    public void editProfile(int index) {
        db.update(STORE_PROFILE, getProfileParams(), "_id = " + index, null);
    }

    public void deleteProfile(int index) {
        db.delete(STORE_PROFILE, "_id = " + index, null);
    }

    public String[] getChannels() {
        return Utils.hasNoValue(channels) ? null : Utils.splitString(channels, ",");
    }

    public String[] getFavs() {
        Cursor c = db.query(STORE_FAVS, new String[] { "favs" }, null, null, null, null, null);
        String[] favs = new String[c.getCount()];
        for (int i = 0; i < favs.length; i++) {
            favs[i] = c.getString(0);
            c.moveToNext();
        }
        return favs;
    }

    public Cursor getCrosswords() {
        return db.query(STORE_CROSSWORDS, new String[] { "id", "crosswd" }, null, null, null, null, null);
    }

    public void deleteCrossword(int index) {
        db.delete(STORE_CROSSWORDS, "_id = " + index, null);
    }

    public void saveCrossword(int index, String msg) {
        ContentValues cv = new ContentValues();
        cv.put("crosswd", msg);
        db.update(STORE_CROSSWORDS, cv, "_id = " + index, null);
    }

    public void save_favs(Vector<String> favs) {
        db.beginTransaction();
        db.execSQL("DELETE FROM " + STORE_FAVS);
        ContentValues temp = new ContentValues();
        for (int i = 0; i < favs.size(); i++) {
            temp.put("fav", favs.get(i));
            db.insert(STORE_FAVS, null, temp);
        }
        db.endTransaction();
    }

    public void save_profile() {
        SharedPreferences config = jmirc.getSharedPreferences(STORE_CONFIG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = config.edit();
        editor.putInt("profileidx", profileidx);
        editor.commit();
    }

    public void save_advanced() {
        SharedPreferences config = jmirc.getSharedPreferences(STORE_CONFIG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = config.edit();
        editor.putBoolean("header", header);
        editor.putBoolean("timestamp", timestamp);
        editor.putBoolean("usecolor", usecolor);
        editor.putBoolean("usemirccol", usemirccol);
        editor.putBoolean("usepoll", usepoll);
        editor.putBoolean("showinput", showinput);
        editor.putString("encoding", encoding);
        editor.putBoolean("utf8detect", utf8detect);
        editor.putBoolean("utf8output", utf8output);
        editor.putInt("buflines", buflines);
        editor.putString("hilight", hilight);
        editor.commit();
    }

    public void save_http() {
        SharedPreferences config = jmirc.getSharedPreferences(STORE_CONFIG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = config.edit();
        editor.putBoolean("usehttp", usehttp);
        editor.putString("gwhost", gwhost);
        editor.putInt("gwport", gwport);
        editor.putString("gwpasswd", gwpasswd);
        editor.putInt("polltime", polltime);
        editor.commit();
    }

    public ContentValues getProfileParams() {
        ContentValues params = new ContentValues();
        params.put("profilename", profilename);
        params.put("nick", nick);
        params.put("altnick", altnick);
        params.put("host", host);
        params.put("port", port);
        params.put("channels", channels);
        params.put("username", username);
        params.put("realname", realname);
        params.put("passwd", passwd);
        params.put("botname", botname);
        return params;
    }
}
