package info.davidmcdonald.comics;

import info.davidmcdonald.comics.data.MyDB;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class Main extends ListActivity {

    private ArrayList<String> dirEntries = new ArrayList<String>();

    private ArrayList<String> dirEntriesTitles = new ArrayList<String>();

    private String myDir = "/sdcard/Comics/";

    private File currentDir = new File("/sdcard/Comics/");

    private MyDB db;

    private int showPosition = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new MyDB(this);
        db.open();
        if (savedInstanceState != null) myDir = savedInstanceState.getString("dir");
        browseTo(new File(myDir));
    }

    private void goToParent() {
        if (this.currentDir.getParent() != null) {
            this.browseTo(this.currentDir.getParentFile());
        }
    }

    private void browseTo(File dir) {
        if (dir.isDirectory()) {
            currentDir = dir;
            populateList(dir.listFiles());
        } else {
            Intent ComicReaderIntent = new Intent(Main.this, ComicReader.class);
            ComicReaderIntent.putExtra("comic", dir.getPath());
            startActivity(ComicReaderIntent);
        }
    }

    private void populateList(File[] files) {
        this.dirEntries.clear();
        for (File file : files) {
            dirEntries.add(file.getPath().replace(currentDir.getAbsolutePath() + "/", ""));
        }
        Collections.sort(dirEntries);
        dirEntriesTitles = updateListings(dirEntries);
        findLatest(dirEntries);
        setListAdapter(new ArrayAdapter<String>(this, R.layout.file_row, dirEntriesTitles));
        getListView().setSelection(showPosition);
    }

    private void findLatest(ArrayList<String> dirEntries2) {
        long lastDateIndex = 0;
        showPosition = 0;
        for (int i = 0; i < dirEntries.size(); i++) {
            long date = 0;
            if (db.Exists(dirEntries.get(i)) && db.getLastIndex(dirEntries.get(i)) != 0) date = db.getRecordDate(dirEntries.get(i));
            if (lastDateIndex < date) showPosition = i;
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        String selectedFileString = this.dirEntries.get(position);
        File file = null;
        file = new File(currentDir.getAbsolutePath() + "/" + selectedFileString);
        browseTo(file);
    }

    public ArrayList<String> updateListings(ArrayList<String> comics) {
        ArrayList<String> newList = new ArrayList<String>();
        ;
        for (String s : comics) {
            if (db.Exists(s)) if (db.getLastIndex(s) == db.getPageCount(s)) s = s + " - Completed"; else if (db.getLastIndex(s) != db.getPageCount(s) && db.getLastIndex(s) != 0) s = s + " - At page " + db.getLastIndex(s) + " of " + db.getPageCount(s);
            newList.add(s);
        }
        return newList;
    }

    public static boolean fileExists(String f) {
        File file = new File(f);
        if (file.exists()) return true;
        return false;
    }

    public void dumpDB(String in, String out) {
        try {
            FileChannel inChannel = new FileInputStream(in).getChannel();
            FileChannel outChannel = new FileOutputStream(out).getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
            inChannel.close();
            outChannel.close();
        } catch (Exception e) {
            Log.d("exception", e.toString());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putString("dir", currentDir.getPath());
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void onResume() {
        super.onResume();
        browseTo(currentDir);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) if (!(currentDir.toString().equals("/sdcard/Comics"))) this.goToParent();
        return true;
    }
}
