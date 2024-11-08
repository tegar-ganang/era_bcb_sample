package database;

import gene.android.Preferences;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

public class BackupDatabase extends AsyncTask<String, Void, Boolean> {

    public static final String CLASS_TAG = BackupDatabase.class.getSimpleName();

    public static final String SDCARD_BACKUP_PATH = Environment.getExternalStorageDirectory() + "/assignment_planner_backup";

    private String errorMessage = "Export Failed";

    private Context context;

    private ProgressDialog dialog;

    private GoogleAnalyticsTracker tracker;

    public BackupDatabase(Context context) {
        this.context = context;
        dialog = new ProgressDialog(context);
        dialog.setIndeterminate(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        tracker = GoogleAnalyticsTracker.getInstance();
        tracker.start("UA-24558831-1", 20, context);
    }

    @Override
    protected void onPreExecute() {
        dialog.setMessage("Exporting database...");
        dialog.show();
    }

    protected Boolean doInBackground(final String... args) {
        File dbFile;
        dbFile = new File(DatabaseHelper.getDatabasePath());
        File exportDir = new File(SDCARD_BACKUP_PATH);
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        File file = new File(exportDir, dbFile.getName());
        try {
            file.createNewFile();
            this.copyFile(dbFile, file);
            tracker.trackEvent(Preferences.TRACKER_PREFERENCE, Preferences.TRACKER_EXPORT_DATABASE_SUCCESS, CLASS_TAG, 0);
            return true;
        } catch (IOException e) {
            errorMessage = "Export of database failed! Please check that the SD card is available!";
            tracker.trackEvent(Preferences.TRACKER_PREFERENCE, Preferences.TRACKER_EXPORT_DATABASE_FAILURE, CLASS_TAG, 0);
            return false;
        }
    }

    protected void onPostExecute(final Boolean success) {
        if (this.dialog.isShowing()) {
            this.dialog.dismiss();
        }
        if (success) {
            Toast.makeText(context, "Export successful!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
        }
        tracker.stop();
    }

    void copyFile(File src, File dst) throws IOException {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }
}
