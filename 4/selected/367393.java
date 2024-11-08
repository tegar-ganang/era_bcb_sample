package app.news.main;

import java.util.ArrayList;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.news.client.RequestMethod;
import com.news.client.RestClient;
import com.news.entities.Channel;

public class ChannelsPref extends ListActivity {

    private static String cursorString;

    private static String actionType;

    private static String recID = "0";

    private ProgressDialog myProgressDialog = null;

    private String[] channelsList = null;

    private ArrayList<Channel> m_channels = null;

    public PostAdapter m_adapter;

    public final Context context = this;

    public static ChannelsPref ChannelContext;

    DatabaseHelper dbHelper;

    private int visibleThreshold = 3;

    private int previousTotal = 0;

    private boolean loading = true;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tt_main_pref);
        recID = "0";
        m_channels = new ArrayList<Channel>();
        this.m_adapter = new PostAdapter(this, R.layout.channelpref_row, m_channels);
        setListAdapter(this.m_adapter);
        ListView lv = getListView();
        lv.setTextFilterEnabled(true);
        Button btnSave = (Button) findViewById(R.id.BtnSave);
        btnSave.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                TheMainActivity.tabContext.getTabHost().setCurrentTab(0);
            }
        });
        lv.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ImageView img_Selected = (ImageView) view.findViewById(R.id.imgSelected);
                ImageView img_Unselected = (ImageView) view.findViewById(R.id.imgUnselected);
                Channel selectedItem = (Channel) parent.getItemAtPosition(position);
                selectedItem.setFlag(selectedItem.getFlag() == 0 ? 1 : 0);
                if (selectedItem.getFlag() == 0) {
                    img_Selected.setVisibility(img_Selected.GONE);
                    img_Unselected.setVisibility(img_Unselected.VISIBLE);
                } else {
                    img_Selected.setVisibility(img_Selected.VISIBLE);
                    img_Unselected.setVisibility(img_Unselected.GONE);
                }
                img_Selected.refreshDrawableState();
                img_Unselected.refreshDrawableState();
                dbHelper = new DatabaseHelper(context);
                dbHelper.UpdateChannel(selectedItem);
            }
        });
        lv.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (loading) {
                    if (totalItemCount > previousTotal) {
                        loading = false;
                        previousTotal = totalItemCount;
                    }
                } else if (!loading && ((totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold))) {
                    ChannelsPref.actionType = "getMoreChannels";
                    new LoadMoreEntries().execute(null, null, null);
                    loading = true;
                }
            }
        });
        ChannelsPref.actionType = "getChannels";
        new LoadMoreEntries().execute(null, null, null);
    }

    private void showSettingsMessage() {
        SharedPreferences myprefs = null;
        myprefs = PreferenceManager.getDefaultSharedPreferences(this);
        String isFirstTime;
        isFirstTime = myprefs.getString("isFirstTime", null);
        Editor updater = myprefs.edit();
        if (isFirstTime != null) {
            if (isFirstTime.equalsIgnoreCase("Yes")) {
                updater.putString("isFirstTime", "No");
                updater.commit();
            }
        }
    }

    private void notifyChange() {
        if (m_channels != null && m_channels.size() > 0) {
            m_adapter.notifyDataSetChanged();
            for (int i = 0; i < m_channels.size(); i++) m_adapter.add(m_channels.get(i));
        }
        m_adapter.notifyDataSetChanged();
        m_channels = null;
    }

    private void startFetchingChannels() {
        String[] unitChannel;
        Channel o1 = new Channel();
        try {
            m_channels = new ArrayList<Channel>();
            dbHelper = new DatabaseHelper(context);
            Cursor cur = dbHelper.getChannelsBlock(recID);
            while (cur.moveToNext()) {
                o1 = new Channel();
                o1.setId((cur.getInt((cur.getColumnIndex("_id")))));
                o1.setName(cur.getString(cur.getColumnIndex("ChannelName")));
                o1.setRssLink((cur.getString(cur.getColumnIndex("RssLink"))));
                o1.setFlag(cur.getInt(cur.getColumnIndex("flag")));
                m_channels.add(o1);
                recID = String.valueOf(o1.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String[] getChannels(int itemCount) {
        String[] lstChannel;
        lstChannel = "GEO|http://geo.tv/data/rss/geo_pakistan.xml~AAJ TAK|http://geo.tv/data/rss/geo_pakistan.xml~SUPER SPORTS|http://geo.tv/data/rss/geo_pakistan.xml~CNN|http://geo.tv/data/rss/geo_pakistan.xml~BBC|http://geo.tv/data/rss/geo_pakistan.xml~FOX NEWS|http://geo.tv/data/rss/geo_pakistan.xml~DAWN NEWS|http://geo.tv/data/rss/geo_pakistan.xml~KTN|http://geo.tv/data/rss/geo_pakistan.xml~PTV|http://geo.tv/data/rss/geo_pakistan.xml~WWE|http://geo.tv/data/rss/geo_pakistan.xml~ETC|http://geo.tv/data/rss/geo_pakistan.xml~ETC|http://geo.tv/data/rss/geo_pakistan.xml~ETC|http://geo.tv/data/rss/geo_pakistan.xml~ETC|http://geo.tv/data/rss/geo_pakistan.xml~ETC|http://geo.tv/data/rss/geo_pakistan.xml~ETC|http://geo.tv/data/rss/geo_pakistan.xml~ETC|http://geo.tv/data/rss/geo_pakistan.xml".split("~");
        return lstChannel;
    }

    private String[] getPosts(String actionType) {
        RestClient client = new RestClient(getString(R.string.AppConnection));
        client.AddParam("action", actionType);
        if (actionType == "getMoreChannels") client.AddParam("cursorStr", cursorString);
        String[] posts = null;
        try {
            client.Execute(RequestMethod.GET);
            String response = "";
            response = client.getResponse();
            if (response == null || response.length() == 0) return null;
            cursorString = response.substring(response.lastIndexOf("~") + 1, response.length() - 1);
            response = response.substring(0, response.lastIndexOf("~"));
            posts = response.split("~");
        } catch (Exception e) {
        }
        return posts;
    }

    public static void updateImage(ImageView imgCheck, ImageView imgUncheck, Integer flag) {
        if (flag == 0) {
            imgCheck.setVisibility(imgCheck.GONE);
            imgUncheck.setVisibility(imgUncheck.VISIBLE);
        } else {
            imgCheck.setVisibility(imgCheck.VISIBLE);
            imgUncheck.setVisibility(imgUncheck.GONE);
        }
        imgCheck.refreshDrawableState();
        imgUncheck.refreshDrawableState();
    }

    private class PostAdapter extends ArrayAdapter<Channel> {

        private ArrayList<Channel> items;

        public PostAdapter(Context context, int textViewResourceId, ArrayList<Channel> items) {
            super(context, textViewResourceId, items);
            this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.channelpref_row, null);
            }
            Channel o = items.get(position);
            if (o != null) {
                TextView tt = (TextView) v.findViewById(R.id.toptext);
                tt.setTextSize(17);
                tt.setLines(1);
                if (tt != null) {
                    tt.setText(o.getName());
                }
                ImageView img = (ImageView) v.findViewById(R.id.channel_pref_icon);
                if (tt.getText().toString().contains("Aaj Tv")) {
                    img.setImageDrawable(getResources().getDrawable(R.drawable.aaj_tv));
                } else if (tt.getText().toString().contains("The News")) {
                    img.setImageDrawable(getResources().getDrawable(R.drawable.the_news));
                } else if (tt.getText().toString().contains("CNN")) {
                    img.setImageDrawable(getResources().getDrawable(R.drawable.cnn));
                } else if (tt.getText().toString().contains("Nation ")) {
                    img.setImageDrawable(getResources().getDrawable(R.drawable.the_nation));
                } else if (tt.getText().toString().contains("BBC")) {
                    img.setImageDrawable(getResources().getDrawable(R.drawable.bbc));
                } else if (tt.getText().toString().contains("Fox News")) {
                    img.setImageDrawable(getResources().getDrawable(R.drawable.fox_news_logo));
                } else if (tt.getText().toString().contains("New York Times")) {
                    img.setImageDrawable(getResources().getDrawable(R.drawable.new_york_times));
                } else if (tt.getText().toString().contains("BigNewsNetwork")) {
                    img.setImageDrawable(getResources().getDrawable(R.drawable.big_news_net));
                } else if (tt.getText().toString().contains("CBC -")) {
                    img.setImageDrawable(getResources().getDrawable(R.drawable.cbc_news));
                } else if (tt.getText().toString().contains("OnePak")) {
                    img.setImageDrawable(getResources().getDrawable(R.drawable.one_pakistan));
                }
            }
            updateImage((ImageView) v.findViewById(R.id.imgSelected), (ImageView) v.findViewById(R.id.imgUnselected), o.getFlag());
            return v;
        }

        public int getItemsCount() {
            return this.items.size();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        myProgressDialog = ProgressDialog.show(News.group, "", "Loading.Please wait...", false);
        return myProgressDialog;
    }

    private class LoadMoreEntries extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showDialog(1);
        }

        @Override
        protected Void doInBackground(Void... params) {
            startFetchingChannels();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            myProgressDialog.dismiss();
            notifyChange();
        }
    }
}
