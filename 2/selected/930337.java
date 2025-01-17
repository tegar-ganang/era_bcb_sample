package com.kxw;

import android.app.Activity;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ArrayAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.util.Log;
import java.util.ArrayList;
import java.net.URL;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import com.kxw.datas.RSSFeed;
import com.kxw.datas.RSSHandler;
import com.kxw.datas.RSSItem;
import android.content.Intent;

public class KanxinwenActivity extends Activity implements OnItemClickListener {

    public final String RSSFEEDOFCHOICE = "http://www.ibm.com/developerworks/views/rss/customrssatom.jsp?zone_by=XML&zone_by=Java&zone_by=Rational&zone_by=Linux&zone_by=Open+source&zone_by=WebSphere&type_by=Tutorials&search_by=&day=1&month=06&year=2007&max_entries=20&feed_by=rss&isGUI=true&Submit.x=48&Submit.y=14";

    public final String tag = "KanXinWen";

    private RSSFeed feed = null;

    /** Called when the activity is first created. */
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);
        feed = getFeed(RSSFEEDOFCHOICE);
        UpdateDisplay();
    }

    private RSSFeed getFeed(String urlToRssFeed) {
        try {
            URL url = new URL(urlToRssFeed);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            XMLReader xmlreader = parser.getXMLReader();
            RSSHandler theRssHandler = new RSSHandler();
            xmlreader.setContentHandler(theRssHandler);
            InputSource is = new InputSource(url.openStream());
            xmlreader.parse(is);
            return theRssHandler.getFeed();
        } catch (Exception ee) {
            return null;
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 0, 0, "Choose RSS Feed");
        menu.add(0, 0, 1, "Refresh");
        Log.i(tag, "onCreateOptionsMenu");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case 0:
                Log.i(tag, "Set RSS Feed");
                return true;
            case 1:
                Log.i(tag, "Refreshing RSS Feed");
                return true;
        }
        return false;
    }

    private void UpdateDisplay() {
        TextView feedtitle = (TextView) findViewById(R.id.feedtitle);
        TextView feedpubdate = (TextView) findViewById(R.id.feedpubdate);
        ListView itemlist = (ListView) findViewById(R.id.itemlist);
        if (feed == null) {
            feedtitle.setText("No RSS Feed Available");
            return;
        }
        feedtitle.setText(feed.getTitle());
        feedpubdate.setText(feed.getPubDate());
        ArrayAdapter<RSSItem> adapter = new ArrayAdapter<RSSItem>(this, android.R.layout.simple_list_item_1, feed.getAllItems());
        itemlist.setAdapter(adapter);
        itemlist.setOnItemClickListener(this);
        itemlist.setSelection(0);
    }

    public void onItemClick(AdapterView parent, View v, int position, long id) {
        Log.i(tag, "item clicked! [" + feed.getItem(position).getTitle() + "]");
        Intent itemintent = new Intent(this, ShowDescription.class);
        Bundle b = new Bundle();
        b.putString("title", feed.getItem(position).getTitle());
        b.putString("description", feed.getItem(position).getDescription());
        b.putString("link", feed.getItem(position).getLink());
        b.putString("pubdate", feed.getItem(position).getPubDate());
        itemintent.putExtra("android.intent.extra.INTENT", b);
        startActivity(itemintent);
    }
}
