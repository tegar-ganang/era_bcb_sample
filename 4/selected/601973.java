package cz.jabbim.android.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.packet.VCard;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.util.Log;

public class AvatarsCache {

    private Context context;

    boolean mediaAvailable = false;

    boolean mediaWritable = false;

    File avatarsPath;

    public AvatarsCache(Context context) {
        this.context = context;
        BroadcastReceiver mediaReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                updateMediaState();
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        this.context.registerReceiver(mediaReceiver, filter);
        updateMediaState();
    }

    public void updateMediaState() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            mediaAvailable = true;
            mediaWritable = true;
            Log.i(this.getClass().getSimpleName(), "External storage mounted in read/write mode, enabling avatars cache.");
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            mediaAvailable = true;
            mediaWritable = false;
            Log.i(this.getClass().getSimpleName(), "External storage mounted in read-only mode, enabling avatars cache in read-only mode.");
        } else {
            mediaAvailable = false;
            mediaWritable = false;
            Log.i(this.getClass().getSimpleName(), "External storage unmounted, disabling avatars cache.");
        }
        avatarsPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/cz.jabbim.android/cache/avatars");
        try {
            avatarsPath.mkdirs();
        } catch (Exception e) {
            Log.w(this.getClass().getSimpleName(), "Unable to write cache directory on External Media!");
        }
    }

    public String getPathToFile(String jid) {
        return "";
    }

    public boolean cacheAvatar(String jid, XMPPConnection con) {
        if (!mediaWritable) {
            Log.w(this.getClass().getSimpleName(), "Unable to write on External Media!");
            return false;
        }
        try {
            VCard vCard = new VCard();
            vCard.load(con, jid);
            if (vCard.getAvatar() != null) {
                File file = new File(avatarsPath, jid);
                OutputStream os = new FileOutputStream(file);
                os.write(vCard.getAvatar());
            }
        } catch (IOException e) {
            Log.e(this.getClass().getSimpleName(), "Error writing avatar file to cache (" + jid + ").");
            return false;
        } catch (XMPPException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
