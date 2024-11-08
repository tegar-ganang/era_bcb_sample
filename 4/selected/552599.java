package org.devtcg.rssreader.pounamu;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.devtcg.rssreader.R;
import org.devtcg.rssreader.activity.ChannelAdd;
import org.devtcg.rssreader.activity.PostList;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ApplicationContext;
import android.app.ProgressDialog;
import android.content.Context;
import android.media.AudioSystem;
import android.media.MediaPlayer;
import android.net.ContentURI;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.Menu.Item;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Esta clase es la encargada de descargar y reproducir los podcast
 * desgraciadamente con la version android_sdk_darwin_m3-rc37a del toolkit no
 * funciona playear directamente desde la red. No somos nadie
 * 
 * Utiliza el parche que propone KimuraShinichi en el grupo de google
 * http://groups.google.com/group/android-developers/browse_thread/thread/461d911d2f6a6447/25362231e56ea8e4#25362231e56ea8e4 
 * 
 * @author gcristofol
 */
public class PlayerActivity extends Activity implements MediaPlayer.OnBufferingUpdateListener {

    private static final String TAG = "PlayerActivity";

    private static final int PLAY_ID = Menu.FIRST + 1;

    private static final int STOP_ID = Menu.FIRST + 2;

    private TextView _text = null;

    private String _enclosure;

    private MP3 _mp3;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.player);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            _enclosure = extras.getString(PostList.ENCLOSURE_ID);
            extras.getString(PostList.LOGOTYPE_ID);
        }
        ImageView iv = (ImageView) findViewById(R.id.podcastImage);
        iv.setImageResource(R.drawable.podcastlogo);
        _text = (TextView) findViewById(R.id.podcastText);
        _text.setText("podcast: " + _enclosure);
        Button buttonPlay = (Button) findViewById(R.id.butttonPlay);
        buttonPlay.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                play();
            }
        });
        Button buttonStop = (Button) findViewById(R.id.butttonStop);
        buttonStop.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                stop();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, PLAY_ID, "Play");
        menu.add(0, STOP_ID, "Stop");
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, Item item) {
        super.onMenuItemSelected(featureId, item);
        switch(item.getId()) {
            case PLAY_ID:
                play();
                _text.setText("playing: " + _enclosure);
                return true;
            case STOP_ID:
                stop();
                _text.setText("podcast: " + _enclosure);
                return true;
        }
        return false;
    }

    private void stop() {
        if (_mp3 != null) _mp3.stop();
    }

    private void play() {
        try {
            _mp3 = new MP3(this, _enclosure);
            _mp3.setOnBufferingUpdateListener(this);
            _mp3.play();
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Error:" + e.getMessage());
            e.printStackTrace();
        }
    }

    public void onBufferingUpdate(MediaPlayer arg0, int i) {
        _text.setText("playing: " + _enclosure + "" + i + "%");
    }
}

class MP3 {

    private String mMp3Uri;

    private String mTempPath;

    private Context mCtx;

    private MediaPlayer mMp;

    public MP3(Context c, String uri) {
        mCtx = c;
        mMp3Uri = uri;
        try {
            mMp = getMediaPlayer(mCtx, mMp3Uri);
        } catch (Exception e) {
            e.printStackTrace();
            if (mMp == null) return;
            mMp.stop();
            mMp.release();
            mMp = null;
        }
    }

    public void stop() {
        if (mMp != null) mMp.stop();
    }

    public void setOnBufferingUpdateListener(PlayerActivity playerActivity) {
        mMp.setOnBufferingUpdateListener(playerActivity);
    }

    public void play() {
        if (mMp == null) return;
        try {
            mMp.setDataSource(mTempPath);
            mMp.seekTo(0);
        } catch (Exception e) {
            e.printStackTrace();
            mMp.stop();
            mMp.release();
            mMp = null;
        }
        if (mMp == null) return;
        mMp.prepare();
        mMp.start();
    }

    private MediaPlayer getMediaPlayer(Context ctx, String url) throws IOException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, URISyntaxException, UnsupportedAudioFileException {
        MediaPlayer mp = null;
        if (url != null) {
            mp = createMediaPlayer(ctx, url);
        }
        return mp;
    }

    public MediaPlayer createMediaPlayer(Context context, String uri) throws IOException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, InvocationTargetException, UnsupportedAudioFileException, URISyntaxException {
        URL u = getURL(uri);
        InputStream stream = u.openStream();
        MediaPlayer mp = createMediaPlayer(stream);
        stream.close();
        return mp;
    }

    private URL getURL(String uri) throws MalformedURLException, URISyntaxException {
        ContentURI u = new ContentURI(uri);
        String protocol = u.getScheme();
        String host = u.getHost();
        String file = u.getPath();
        return new URL(protocol, host, file);
    }

    public MediaPlayer createMediaPlayer(Context context, int resid) throws IOException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, InvocationTargetException {
        InputStream stream = context.getResources().openRawResource(resid);
        MediaPlayer mp = createMediaPlayer(stream);
        stream.close();
        return mp;
    }

    private MediaPlayer createMediaPlayer(InputStream stream) throws IOException, FileNotFoundException, ClassNotFoundException, NoSuchFieldException, IllegalAccessException, InvocationTargetException {
        if (stream == null) return null;
        File temp = File.createTempFile("mediaplayertmp", "dat");
        mTempPath = temp.getAbsolutePath();
        FileOutputStream out = new FileOutputStream(temp);
        byte buf[] = new byte[128];
        do {
            int numread = stream.read(buf);
            if (numread <= 0) break;
            out.write(buf, 0, numread);
        } while (true);
        MediaPlayer mp = new MediaPlayer();
        return mp;
    }
}
