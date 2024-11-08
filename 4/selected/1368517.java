package com.kg.emailalbum.mobile.viewer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.zip.ZipFile;
import org.acra.ErrorReporter;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.kg.emailalbum.mobile.AboutDialog;
import com.kg.emailalbum.mobile.EmailAlbumPreferences;
import com.kg.emailalbum.mobile.R;
import com.kg.emailalbum.mobile.util.BitmapLoader;
import com.kg.emailalbum.mobile.util.CacheManager;
import com.kg.emailalbum.mobile.util.Compatibility;
import com.kg.emailalbum.mobile.util.CustomContentResolver;
import com.kg.emailalbum.mobile.util.IntentHelper;
import com.kg.oifilemanager.filemanager.FileManagerProvider;
import com.kg.oifilemanager.filemanager.util.FileUtils;
import com.kg.oifilemanager.intents.FileManagerIntents;

/**
 * Loads an album and displays its content as a list.
 * 
 * @author Kevin Gaudin
 */
public class EmailAlbumViewer extends ListActivity {

    /**
     * Loads an archive provided by an Intent (from Gmail for example). TODO:
     * use AsyncTask
     * 
     * @author Normal
     * 
     */
    private class ArchiveRetriever implements Runnable {

        private static final int MSG_ARCHIVE_RETRIEVED = 0;

        @Override
        public void run() {
            try {
                Random generator = new Random();
                int random = generator.nextInt(99999);
                Log.d(LOG_TAG, "Opening " + mAlbumFileUri.toString());
                InputStream intentInputStream = getContentResolver().openInputStream(mAlbumFileUri);
                File tempArchive = new File(new CacheManager(getApplicationContext()).getInboxDir(), "emailalbum" + random + FileUtils.getExtensionForMimeType(getIntent().getType()));
                OutputStream tempOS = new FileOutputStream(tempArchive);
                Log.d(LOG_TAG, "Write retrieved archive : " + tempArchive.getAbsolutePath());
                byte[] buffer = new byte[256];
                int readBytes = -1;
                while ((readBytes = intentInputStream.read(buffer)) != -1) {
                    tempOS.write(buffer, 0, readBytes);
                }
                mAlbumFileUri = Uri.fromFile(tempArchive);
                tempOS.close();
                intentInputStream.close();
                archiveCopyHandler.sendEmptyMessage(MSG_ARCHIVE_RETRIEVED);
            } catch (IOException e) {
                Message msg = new Message();
                Bundle data = new Bundle();
                msg.arg1 = -1;
                data.putString("EXCEPTION", e.getLocalizedMessage());
                msg.setData(data);
                archiveCopyHandler.sendMessage(msg);
            }
        }
    }

    /**
     * An adapter handling the content of the archive.
     */
    public class PhotoAdapter extends BaseAdapter {

        /**
         * Keeps references to UI elements to avoid looking for them. This
         * should be attached to a row item with View.setTag().
         */
        private class ViewHolder {

            public ImageView image = null;

            public TextView text = null;
        }

        @Override
        public int getCount() {
            if (mContentModel == null) return 0;
            return mContentModel.size();
        }

        @Override
        public Object getItem(int position) {
            return mContentModel.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.album_viewer_line, null, false);
                holder = new ViewHolder();
                holder.image = (ImageView) convertView.findViewById(R.id.thumbnail);
                holder.text = (TextView) convertView.findViewById(R.id.image_name);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            SlideshowItem currentMetaData = mContentModel.get(position);
            if (position < mThumbnailsNames.size()) {
                String thumbName = mThumbnailsNames.get(position);
                Log.d(LOG_TAG, "Let's load " + thumbName);
                if (thumbName != null) {
                    try {
                        holder.image.setImageBitmap(BitmapLoader.load(getApplicationContext(), FileManagerProvider.getContentUri(new File(thumbName)), ThumbnailsCreator.getThumbWidth(getApplicationContext()), null));
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Error : ", e);
                    }
                } else {
                    holder.image.setImageResource(R.drawable.robot);
                }
            } else {
                holder.image.setImageResource(R.drawable.robot);
            }
            String shortName = currentMetaData.getShortName();
            StringBuilder text = new StringBuilder(shortName);
            if (currentMetaData.caption != null && !"".equals(currentMetaData.caption.trim())) {
                text.append("\n\n").append(currentMetaData.caption);
            }
            holder.text.setText(text);
            return convertView;
        }

        /**
         * Update the Thumbnail of an item.
         * 
         * @param position
         *            The position of the item in the list.
         * @param thumbName
         *            The thumbnail file path.
         */
        public void updateThumbnail(int position, String thumbName) {
            mThumbnailsNames.add(position, thumbName);
            notifyDataSetChanged();
        }
    }

    /**
     * Called Activities IDs
     */
    private static final int ACTIVITY_PICK_DIRECTORY_TO_SAVE_ALL = 2;

    private static final int ACTIVITY_PICK_DIRECTORY_TO_SAVE_SELECTED = 3;

    private static final int ACTIVITY_PICK_FILE = 1;

    public static final String KEY_THMBCREAT_ENTRY_POSITION = "entryPosition";

    public static final String KEY_THMBCREAT_THUMB_NAME = "thumbName";

    private static final String LOG_TAG = EmailAlbumViewer.class.getSimpleName();

    private static final int MENU_LOAD_ALBUM_ID = 1;

    private static final int MENU_SAVE_ALL_ID = 2;

    private static final int MENU_ABOUT_ID = 3;

    private static final int MENU_PREFS_ID = 4;

    private static final int MENU_SEND_ALL_ID = 5;

    private static final int MENUCTX_SAVE_SELECTED_ID = 4;

    protected static final Uri URI_DEMO = Uri.parse("http://www.gaudin.tv/storage/android/curious-creature.jar");

    /**
     * Handler for receiving the archive when it is asynchronously loaded.
     */
    private Handler archiveCopyHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mProgress.dismiss();
            if (msg.arg1 < 0) {
                Toast.makeText(context, R.string.error_saving, Toast.LENGTH_SHORT).show();
            }
            fillData(true);
        }
    };

    private Context context;

    private PhotoAdapter mAdapter;

    private Uri mAlbumFileUri = null;

    private ZipFile mArchive = null;

    private SlideshowList mContentModel;

    private ArrayList<String> mThumbnailsNames = new ArrayList<String>();

    private ProgressDialog mProgress;

    private ThumbnailsCreator mThmbCreator;

    private int posPictureToSave;

    private Handler saveAllHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.arg1 < 0) {
                Toast.makeText(context, msg.getData().getString("EXCEPTION"), Toast.LENGTH_SHORT).show();
                setProgressBarVisibility(false);
            } else {
                int position = msg.what;
                setProgress((position + 1) * 10000 / mContentModel.size());
            }
        }
    };

    /**
     * Receive created thumbnails and give them to the adapter.
     */
    private Handler thumbnailsCreationHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.arg1 < 0) {
                if (msg.getData().getString("EXCEPTION_CLASS").equals(OutOfMemoryError.class.getSimpleName())) {
                    Toast.makeText(context, R.string.error_out_of_mem, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, msg.getData().getString("EXCEPTION"), Toast.LENGTH_SHORT).show();
                }
            } else {
                int position = msg.getData().getInt(KEY_THMBCREAT_ENTRY_POSITION);
                String thumbName = msg.getData().getString(KEY_THMBCREAT_THUMB_NAME);
                setProgress((position + 1) * 10000 / mContentModel.size());
                Log.d(LOG_TAG, "Received thumbnail update for position : " + position + " uri : " + thumbName);
                mAdapter.updateThumbnail(position, thumbName);
            }
        }
    };

    /**
     * Load the content of the archive.
     * 
     * @param clearThumbnails
     *            Set this to true if you want to discard existing thumbnails.
     */
    private void fillData(boolean clearThumbnails) {
        if (mAlbumFileUri != null) {
            if (mAlbumFileUri.toString().startsWith("content://TAGS")) {
                Toast.makeText(this, "TO BE IMPLEMENTED", Toast.LENGTH_SHORT).show();
                finish();
            } else if (mAlbumFileUri.toString().startsWith(Media.EXTERNAL_CONTENT_URI.toString())) {
                mContentModel = new GallerySlideshowList(getApplicationContext(), null);
                setTitle("Gallery");
                registerForContextMenu(getListView());
                getListView().setSelection(getListView().getFirstVisiblePosition());
            } else {
                try {
                    File selectedFile = new File(new URI(mAlbumFileUri.toString().replace(" ", "%20")));
                    mArchive = new ZipFile(selectedFile);
                } catch (Exception e) {
                    Toast.makeText(this, R.string.open_archive_error + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }
                if (mArchive == null) {
                    ErrorReporter.getInstance().handleException(new Exception("No archive file."));
                } else {
                    try {
                        mContentModel = new ArchiveSlideshowList(null, mAlbumFileUri);
                        setTitle(mAlbumFileUri.getLastPathSegment());
                        registerForContextMenu(getListView());
                        getListView().setSelection(getListView().getFirstVisiblePosition());
                    } catch (Exception e) {
                        Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
        if (mContentModel != null) {
            setProgressBarVisibility(true);
            startThumbnailsCreation(clearThumbnails);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case ACTIVITY_PICK_FILE:
                if (resultCode == RESULT_OK && data != null) {
                    Uri filename = data.getData();
                    if (filename != null) {
                        mAlbumFileUri = filename;
                        fillData(true);
                    } else {
                        finish();
                    }
                } else {
                    finish();
                }
                break;
            case ACTIVITY_PICK_DIRECTORY_TO_SAVE_SELECTED:
            case ACTIVITY_PICK_DIRECTORY_TO_SAVE_ALL:
                if (resultCode == RESULT_OK && data != null) {
                    String dirname = data.getDataString();
                    try {
                        if (requestCode == ACTIVITY_PICK_DIRECTORY_TO_SAVE_SELECTED) {
                            savePicture(posPictureToSave, new File(Uri.parse(dirname).getPath()));
                        } else {
                            saveAllPictures(new File(Uri.parse(dirname).getPath()));
                        }
                    } catch (IOException e) {
                        Log.e(this.getClass().getName(), "onActivityResult() exception", e);
                        Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case MENU_SAVE_ALL_ID:
                pickDirectory(ACTIVITY_PICK_DIRECTORY_TO_SAVE_ALL);
                return true;
            case MENUCTX_SAVE_SELECTED_ID:
                pickDirectory(ACTIVITY_PICK_DIRECTORY_TO_SAVE_SELECTED);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.album_viewer);
        findViewById(R.id.album_viewer_root).getBackground().setDither(true);
        mAdapter = new PhotoAdapter();
        setListAdapter(mAdapter);
        context = this;
        if (savedInstanceState != null) {
            if (savedInstanceState.getString("albumFileUri") != null) {
                mAlbumFileUri = Uri.parse(savedInstanceState.getString("albumFileUri"));
                fillData(false);
                if (savedInstanceState.getBoolean("thumbCreatorInterrupted")) {
                    startThumbnailsCreation(false);
                }
            }
        } else if (getIntent() != null && Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            mAlbumFileUri = getIntent().getData();
            if (mAlbumFileUri.getScheme().equals(ContentResolver.SCHEME_CONTENT) && !(mAlbumFileUri.toString().startsWith(Media.EXTERNAL_CONTENT_URI.toString()))) {
                mProgress = ProgressDialog.show(this, getText(R.string.title_prog_retrieve_archive), getText(R.string.msg_prog_retrieve_archive), true, false);
                new Thread(new ArchiveRetriever()).start();
            } else {
                fillData(true);
            }
        }
        if (mAlbumFileUri == null || "".equals(mAlbumFileUri)) {
            Log.d(LOG_TAG, "No album");
            openAlbum();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        posPictureToSave = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
        menu.setHeaderTitle(R.string.menu_save);
        menu.add(0, MENUCTX_SAVE_SELECTED_ID, 0, R.string.menu_save_selected);
        menu.add(0, MENU_SAVE_ALL_ID, 0, R.string.menu_save_all);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        MenuItem item = menu.add(0, MENU_LOAD_ALBUM_ID, 0, R.string.menu_load_album);
        item.setIcon(android.R.drawable.ic_menu_gallery);
        item = menu.add(0, MENU_SAVE_ALL_ID, 0, R.string.menu_save_all);
        item.setIcon(android.R.drawable.ic_menu_save);
        if (Compatibility.isSendMultipleAppAvailable(getApplicationContext())) {
            item = menu.add(0, MENU_SEND_ALL_ID, 0, R.string.menu_share_all);
            item.setIcon(android.R.drawable.ic_menu_share);
        }
        item = menu.add(0, MENU_PREFS_ID, 0, R.string.menu_prefs);
        item.setIcon(android.R.drawable.ic_menu_preferences);
        item = menu.add(0, MENU_ABOUT_ID, 0, R.string.menu_about);
        item.setIcon(android.R.drawable.ic_menu_help);
        return result;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            new CacheManager(getApplicationContext()).clearCache("viewer");
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, ShowPics.class);
        i.putExtra("ALBUM", mAlbumFileUri);
        i.putExtra("POSITION", position);
        startActivity(i);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case MENU_LOAD_ALBUM_ID:
                openAlbum();
                return true;
            case MENU_SAVE_ALL_ID:
                pickDirectory(ACTIVITY_PICK_DIRECTORY_TO_SAVE_ALL);
                return true;
            case MENU_SEND_ALL_ID:
                File dir = new CacheManager(getApplicationContext()).getCacheDir("temp");
                IntentHelper.sendAllPicturesInFolder(this, dir, "", "");
                return true;
            case MENU_ABOUT_ID:
                Intent intent = new Intent(this, AboutDialog.class);
                startActivity(intent);
                return true;
            case MENU_PREFS_ID:
                startPreferencesActivity();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mThmbCreator != null && mThmbCreator.isAlive()) {
            mThmbCreator.stopCreation();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mContentModel == null) {
            menu.findItem(MENU_SAVE_ALL_ID).setEnabled(false);
        } else {
            menu.findItem(MENU_SAVE_ALL_ID).setEnabled(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mThmbCreator == null || (mThmbCreator != null && !mThmbCreator.isAlive())) {
            startThumbnailsCreation(false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAlbumFileUri != null) {
            outState.putString("albumFileUri", mAlbumFileUri.toString());
        }
        if (mThmbCreator != null && mThmbCreator.isAlive()) {
            mThmbCreator.stopCreation();
            outState.putBoolean("thumbCreatorInterrupted", true);
        }
    }

    /**
     * Start the album opening process : start a file chooser activity.
     */
    private void openAlbum() {
        Intent intent = new Intent(FileManagerIntents.ACTION_PICK_FILE);
        if (android.os.Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            intent.setData(Uri.fromFile(android.os.Environment.getExternalStorageDirectory()));
        } else {
            intent.setData(Uri.parse("file:///"));
        }
        intent.putExtra(FileManagerIntents.EXTRA_TITLE, getText(R.string.select_file));
        intent.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT, getText(R.string.btn_select_file));
        try {
            startActivityForResult(intent, ACTIVITY_PICK_FILE);
        } catch (ActivityNotFoundException e) {
            Log.e(LOG_TAG, "Error before picking file", e);
        }
    }

    /**
     * Let the user choose a directory.
     * 
     * @param requestCode
     *            The code which will be returned to allow to select which
     *            action has to be performed on the directory.
     */
    private void pickDirectory(int requestCode) {
        Intent intent = new Intent(FileManagerIntents.ACTION_PICK_DIRECTORY);
        intent.setData(Uri.fromFile(android.os.Environment.getExternalStorageDirectory()));
        intent.putExtra(FileManagerIntents.EXTRA_TITLE, getText(R.string.select_directory));
        intent.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT, getText(R.string.btn_select_directory));
        try {
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            Log.e(LOG_TAG, "Error before picking directory", e);
        }
    }

    /**
     * Save all the archive pictures. TODO: use AsyncTask
     * 
     * @param file
     *            The destination directory.
     * @throws IOException
     */
    private void saveAllPictures(final File file) throws IOException {
        setProgress(0);
        setProgressBarVisibility(true);
        new Thread() {

            @Override
            public void run() {
                for (int i = 0; i < mContentModel.size(); i++) {
                    try {
                        savePicture(i, file);
                        saveAllHandler.sendEmptyMessage(i);
                    } catch (FileNotFoundException e) {
                        Log.e(this.getClass().getSimpleName(), "Error while saving pictures", e);
                        Message msg = new Message();
                        Bundle data = new Bundle();
                        msg.arg1 = -1;
                        data.putString("EXCEPTION", e.getMessage());
                        msg.setData(data);
                        saveAllHandler.sendMessage(msg);
                    } catch (IOException e) {
                        ErrorReporter.getInstance().handleSilentException(e);
                    }
                }
            }
        }.start();
    }

    /**
     * Save one picture from the archive
     * 
     * @param position
     * @param destDir
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private File savePicture(int position, File destDir) throws FileNotFoundException, IOException {
        File destFile;
        SlideshowItem imgModel = mContentModel.get(position);
        destFile = new File(destDir, imgModel.getShortName().toLowerCase());
        OutputStream destFileOS = new FileOutputStream(destFile);
        InputStream imageIS = CustomContentResolver.openInputStream(getApplicationContext(), imgModel.uri);
        byte[] buffer = new byte[2048];
        int len = 0;
        while ((len = imageIS.read(buffer)) >= 0) {
            destFileOS.write(buffer, 0, len);
        }
        destFileOS.close();
        imageIS.close();
        return destFile;
    }

    /**
     * Start the settings activity.
     */
    private void startPreferencesActivity() {
        Intent i = new Intent(getApplicationContext(), EmailAlbumPreferences.class);
        i.putExtra(EmailAlbumPreferences.EXTRA_SCREEN, EmailAlbumPreferences.SCREEN_VIEWER);
        startActivity(i);
    }

    /**
     * Start the asynchronous thumbnails creation process.
     * 
     * @param clearThumbnails
     */
    private void startThumbnailsCreation(boolean clearThumbnails) {
        if (mContentModel != null) {
            ArrayList<Uri> pictureUris = new ArrayList<Uri>();
            Iterator<SlideshowItem> i = mContentModel.iterator();
            while (i.hasNext()) {
                SlideshowItem entry = i.next();
                pictureUris.add(entry.uri);
            }
            mThmbCreator = new ThumbnailsCreator(this, pictureUris, thumbnailsCreationHandler, clearThumbnails);
            mThmbCreator.start();
        }
    }
}
