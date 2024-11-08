package app.news.main;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.news.entities.Channel;

public class DatabaseHelper extends SQLiteOpenHelper {

    static final String dbName = "NewsDB2";

    static final String channelsTable = "Channels";

    static final String colID = "_id";

    static final String channelName = "ChannelName";

    static final String flag = "flag";

    static final String rssLink = "RssLink";

    static final String FaltucolDept = "Dept";

    static final String faltudeptTable = "Dept";

    static final String falutcolDeptID = "DeptID";

    static final String faltucolDeptName = "DeptName";

    static final String faltuviewEmps = "ViewEmps";

    private Context context;

    public DatabaseHelper(Context context) {
        super(context, dbName, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + channelsTable + " (" + colID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + channelName + " TEXT, " + rssLink + " TEXT," + "flag INTEGER)");
        InsertDepts(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    void AddEmployee(Channel emp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(channelName, emp.getName());
        cv.put(rssLink, emp.getRssLink());
        db.insert(channelsTable, channelName, cv);
        db.close();
    }

    int getEmployeeCount() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cur = db.rawQuery("Select * from " + channelsTable, null);
        int x = cur.getCount();
        cur.close();
        return x;
    }

    public Cursor getAllEmployees() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cur = db.rawQuery("SELECT * FROM " + channelsTable, null);
        return cur;
    }

    public Cursor getChannelsBlock(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cur;
        if (id.equals("0")) {
            cur = db.rawQuery("SELECT * FROM " + channelsTable + " LIMIT 20", null);
        } else {
            cur = db.rawQuery("SELECT * FROM " + channelsTable + " WHERE " + colID + " > " + id + " LIMIT 20", null);
        }
        return cur;
    }

    public Cursor getChannelsBlock_Feed(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cur = db.rawQuery("SELECT * FROM " + channelsTable + " WHERE FLAG=1 AND " + colID + " > " + id + " LIMIT 20", null);
        return cur;
    }

    Cursor getAllDepts() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cur = db.rawQuery("SELECT " + falutcolDeptID + " as _id, " + faltucolDeptName + " from " + faltudeptTable, new String[] {});
        return cur;
    }

    void InsertDepts(SQLiteDatabase db) {
        ContentValues cv = new ContentValues();
        cv.put(channelName, "Aaj Tv - National News");
        cv.put(rssLink, "http://www.aaj.tv/national/feed/");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Aaj Tv - World News");
        cv.put(rssLink, "http://www.aaj.tv/world/feed/");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Aaj Tv - Business News");
        cv.put(rssLink, "http://www.aaj.tv/business/feed/");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Aaj Tv - Sports News");
        cv.put(rssLink, "http://www.aaj.tv/sports/feed/");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Aaj Tv - Entertainment");
        cv.put(rssLink, "http://www.aaj.tv/entertainment/feed/");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "The News - Latest Stories");
        cv.put(rssLink, "http://old.thenews.com.pk/rss/thenews_updates.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "The News - Top Stories");
        cv.put(rssLink, "http://old.thenews.com.pk/rss/thenews_topstories.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "The News - World News");
        cv.put(rssLink, "http://old.thenews.com.pk/rss/thenews_world.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "The News - National News");
        cv.put(rssLink, "http://old.thenews.com.pk/rss/thenews_national.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "The News - Business News");
        cv.put(rssLink, "http://old.thenews.com.pk/rss/thenews_business.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "The News - Karachi News");
        cv.put(rssLink, "http://old.thenews.com.pk/rss/thenews_karachi.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "The News - Lahore News");
        cv.put(rssLink, "http://old.thenews.com.pk/rss/thenews_lahore.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "The News - Islamabad News");
        cv.put(rssLink, "http://old.thenews.com.pk/rss/thenews_islamabad.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "The News - Peshawar News");
        cv.put(rssLink, "http://old.thenews.com.pk/rss/thenews_peshawar.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "CNN - Top Stories");
        cv.put(rssLink, "http://rss.cnn.com/rss/edition.rss");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "CNN - World");
        cv.put(rssLink, "http://rss.cnn.com/rss/edition_world.rss");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "CNN - Africa");
        cv.put(rssLink, "http://rss.cnn.com/rss/edition_africa.rss");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "CNN - Americas");
        cv.put(rssLink, "http://rss.cnn.com/rss/edition_americas.rss");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "CNN - Asia");
        cv.put(rssLink, "http://rss.cnn.com/rss/edition_asia.rss");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "CNN - Europe");
        cv.put(rssLink, "http://rss.cnn.com/rss/edition_europe.rss");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "CNN - Middle East");
        cv.put(rssLink, "http://rss.cnn.com/rss/edition_meast.rss");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "CNN - U.S.");
        cv.put(rssLink, "http://rss.cnn.com/rss/edition_us.rss");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "CNN - World Business");
        cv.put(rssLink, "http://rss.cnn.com/rss/edition_business.rss");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "CNN - Technology");
        cv.put(rssLink, "http://rss.cnn.com/rss/edition_technology.rss");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "CNN - Science & Space");
        cv.put(rssLink, "http://rss.cnn.com/rss/edition_space.rss");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "CNN - Entertainment");
        cv.put(rssLink, "http://rss.cnn.com/rss/edition_entertainment.rss");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "CNN - World Sport");
        cv.put(rssLink, "http://rss.cnn.com/rss/edition_sport.rss");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "CNN - Football");
        cv.put(rssLink, "http://rss.cnn.com/rss/edition_football.rss");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Nation - Breaking News");
        cv.put(rssLink, "http://feeds.feedburner.com/pakistan-news-newspaper-daily-english-online/Breaking");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Nation - Headline");
        cv.put(rssLink, "http://feeds.feedburner.com/pakistan-news-newspaper-daily-english-online/headlines");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Nation - Today's Headlines");
        cv.put(rssLink, "http://feeds.feedburner.com/pakistan-news-newspaper-daily-english-online/today-headlines");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Nation - Politics");
        cv.put(rssLink, "http://feeds.feedburner.com/pakistan-news-newspaper-daily-english-online/Politics");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Nation - Business");
        cv.put(rssLink, "http://feeds.feedburner.com/pakistan-news-newspaper-daily-english-online/Business");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Nation - International");
        cv.put(rssLink, "http://feeds.feedburner.com/pakistan-news-newspaper-daily-english-online/International");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Nation - Sports");
        cv.put(rssLink, "http://feeds.feedburner.com/pakistan-news-newspaper-daily-english-online/Sports");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Nation - Entertainment");
        cv.put(rssLink, "http://feeds.feedburner.com/pakistan-news-newspaper-daily-english-online/Entertainment");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Nation - Lahore");
        cv.put(rssLink, "http://feeds.feedburner.com/pakistan-news-newspaper-daily-english-online/lahore");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Nation - Karachi");
        cv.put(rssLink, "http://feeds.feedburner.com/pakistan-news-newspaper-daily-english-online/karachi");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Nation - Islamabad");
        cv.put(rssLink, "http://feeds.feedburner.com/pakistan-news-newspaper-daily-english-online/islamabad");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "BBC - Top Stories");
        cv.put(rssLink, "http://feeds.bbci.co.uk/news/rss.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "BBC - World");
        cv.put(rssLink, "http://feeds.bbci.co.uk/news/world/rss.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "BBC - UK");
        cv.put(rssLink, "http://feeds.bbci.co.uk/news/uk/rss.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "BBC - Business");
        cv.put(rssLink, "http://feeds.bbci.co.uk/news/business/rss.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "BBC - Politics");
        cv.put(rssLink, "http://feeds.bbci.co.uk/news/politics/rss.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "BBC - Health");
        cv.put(rssLink, "http://feeds.bbci.co.uk/news/health/rss.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "BBC - Education & Family");
        cv.put(rssLink, "http://feeds.bbci.co.uk/news/education/rss.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "BBC - Science & Environment");
        cv.put(rssLink, "http://feeds.bbci.co.uk/news/science_and_environment/rss.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "BBC - Technology");
        cv.put(rssLink, "http://feeds.bbci.co.uk/news/technology/rss.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "BBC - Entertainment & Arts");
        cv.put(rssLink, "http://feeds.bbci.co.uk/news/entertainment_and_arts/rss.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Fox News - Latest Headlines");
        cv.put(rssLink, "http://feeds.foxnews.com/foxnews/latest");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Fox News - National");
        cv.put(rssLink, "http://feeds.foxnews.com/foxnews/national");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Fox News - World");
        cv.put(rssLink, "http://feeds.foxnews.com/foxnews/world");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Fox News - Politics");
        cv.put(rssLink, "http://feeds.foxnews.com/foxnews/politics");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Fox News - Business");
        cv.put(rssLink, "http://feeds.foxnews.com/foxnews/business");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Fox News - SciTech");
        cv.put(rssLink, "http://feeds.foxnews.com/foxnews/scitech");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Fox News - Health");
        cv.put(rssLink, "http://feeds.foxnews.com/foxnews/health");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Fox News - Entertainment");
        cv.put(rssLink, "http://feeds.foxnews.com/foxnews/entertainment");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Fox News - Views");
        cv.put(rssLink, "http://feeds.foxnews.com/foxnews/views");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Fox News - National");
        cv.put(rssLink, "http://feeds.foxnews.com/foxnews/national");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "Fox News - World");
        cv.put(rssLink, "http://feeds.foxnews.com/foxnews/world");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "New York Times - World");
        cv.put(rssLink, "http://feeds.nytimes.com/nyt/rss/World");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "New York Times - Africa");
        cv.put(rssLink, "http://feeds.nytimes.com/nyt/rss/Africa");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "New York Times - Americas");
        cv.put(rssLink, "http://feeds.nytimes.com/nyt/rss/Americas");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "New York Times - Asia Pacific");
        cv.put(rssLink, "http://feeds.nytimes.com/nyt/rss/AsiaPacific");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "New York Times - Europe");
        cv.put(rssLink, "http://feeds.nytimes.com/nyt/rss/Europe");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "New York Times - Middle East");
        cv.put(rssLink, "http://feeds.nytimes.com/nyt/rss/MiddleEast");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "New York Times - Technology");
        cv.put(rssLink, "http://feeds.nytimes.com/nyt/rss/Technology");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "New York Times - Internet");
        cv.put(rssLink, "http://feeds.nytimes.com/nyt/rss/internet");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "BigNewsNetwork - Pakistan");
        cv.put(rssLink, "http://www.bignewsnetwork.com/?rss=8c3d7d78943a99c7");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "CBC - Top Stories");
        cv.put(rssLink, "http://rss.cbc.ca/lineup/topstories.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "CBC - World");
        cv.put(rssLink, "http://rss.cbc.ca/lineup/world.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "CBC - Canada");
        cv.put(rssLink, "http://rss.cbc.ca/lineup/canada.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "CBC - Politics");
        cv.put(rssLink, "http://rss.cbc.ca/lineup/politics.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "CBC - Business");
        cv.put(rssLink, "http://rss.cbc.ca/lineup/business.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "CBC - Health");
        cv.put(rssLink, "http://rss.cbc.ca/lineup/health.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "CBC - Arts & Entertainment");
        cv.put(rssLink, "http://rss.cbc.ca/lineup/arts.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "CBC - Technology & Science");
        cv.put(rssLink, "http://rss.cbc.ca/lineup/technology.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "CBC - Sports");
        cv.put(rssLink, "http://rss.cbc.ca/lineup/sports.xml");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "OnePak - Top Stories");
        cv.put(rssLink, "http://feeds.feedburner.com/News-Topstories");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "OnePak - National");
        cv.put(rssLink, "http://feeds.feedburner.com/News-National");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "OnePak - World");
        cv.put(rssLink, "http://feeds.feedburner.com/News-World-Pakistan");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "OnePak - US");
        cv.put(rssLink, "http://feeds.feedburner.com/News-World-USA-Pakistan");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "OnePak - UK");
        cv.put(rssLink, "http://feeds.feedburner.com/News-World-UK-Pakistan");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "OnePak - Afghanistan");
        cv.put(rssLink, "http://feeds.feedburner.com/News-World-Afghanistan-Pakistan");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "OnePak - India");
        cv.put(rssLink, "http://feeds.feedburner.com/News-World-India-Pakistan");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "OnePak - Middle East");
        cv.put(rssLink, "http://feeds.feedburner.com/News-Middle-East-Pakistan");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "OnePak - Local");
        cv.put(rssLink, "http://feeds.feedburner.com/News-Local");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "OnePak - Peshawar");
        cv.put(rssLink, "http://feeds.feedburner.com/News-Local-Peshawar");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "OnePak - Islamabad");
        cv.put(rssLink, "http://feeds.feedburner.com/News-Local-Islamabad");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "OnePak - Lahore");
        cv.put(rssLink, "http://feeds.feedburner.com/News-Local-Lahore");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "OnePak - Karachi");
        cv.put(rssLink, "http://feeds.feedburner.com/News-Local-Karachi");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "OnePak - Quetta");
        cv.put(rssLink, "http://feeds.feedburner.com/News-Local-Quetta");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "OnePak - Tribal Areas");
        cv.put(rssLink, "http://feeds.feedburner.com/News-Local-Tribal-Areas");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "OnePak - Kashmir");
        cv.put(rssLink, "http://feeds.feedburner.com/News-Kashmir");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "OnePak - Sports");
        cv.put(rssLink, "http://feeds.feedburner.com/News-Sports");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "OnePak - Education");
        cv.put(rssLink, "http://feeds.feedburner.com/News-Pakistan-Education");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "OnePak - Science");
        cv.put(rssLink, "http://feeds.feedburner.com/News-Science-Pakistan");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "OnePak - Technology");
        cv.put(rssLink, "http://feeds.feedburner.com/News-Technology-Pakistan");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "OnePak - Health");
        cv.put(rssLink, "http://feeds.feedburner.com/News-Health-Pakistan");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "OnePak - Cricket News");
        cv.put(rssLink, "http://feeds.feedburner.com/Pakistan-Cricket-News");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
        cv.put(channelName, "OnePak - ShowBiz");
        cv.put(rssLink, "http://feeds.feedburner.com/Showbiz-News");
        cv.put(flag, "0");
        db.insert(channelsTable, channelName, cv);
    }

    public void FillDB() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + channelsTable);
        db.execSQL("CREATE TABLE " + channelsTable + " (" + colID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + channelName + " TEXT, " + rssLink + " TEXT," + "flag INTEGER)");
        InsertDepts(db);
    }

    public int UpdateChannel(Channel emp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(flag, emp.getFlag());
        return db.update(channelsTable, cv, colID + "=?", new String[] { String.valueOf(emp.getId()) });
    }

    public void getEmployees(String actionType) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cur = db.rawQuery("SELECT * FROM " + channelsTable, null);
    }

    public int getSelectedChannelsCount() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cur = db.rawQuery("SELECT * FROM " + channelsTable + " WHERE FLAG=1", null);
        int x = cur.getCount();
        return x;
    }

    public void openDB() {
        SQLiteDatabase db = this.getWritableDatabase();
    }

    public boolean isChannelsEmpty() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cur = db.rawQuery("SELECT * FROM " + channelsTable + " WHERE FLAG=1 LIMIT 20", null);
        if (cur.getCount() == 0) return true; else return false;
    }
}
