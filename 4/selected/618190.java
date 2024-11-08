package de.hilses.droidreader;

import pcgen.CharacterViewer.R;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.String;
import java.net.URLDecoder;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Bundle;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

public class DroidReaderActivity extends Activity {

    private static final boolean LOG = false;

    private static final int REQUEST_CODE_PICK_FILE = 1;

    private static final int REQUEST_CODE_OPTION_DIALOG = 2;

    private static final int DIALOG_GET_PASSWORD = 1;

    private static final int DIALOG_ABOUT = 2;

    private static final int DIALOG_GOTO_PAGE = 3;

    private static final int DIALOG_WELCOME = 4;

    private static final int DIALOG_ENTER_ZOOM = 5;

    private static final String PREFERENCE_EULA_ACCEPTED = "eula.accepted";

    private static final String PREFERENCES_EULA = "eula";

    protected DroidReaderView mReaderView = null;

    protected DroidReaderDocument mDocument = null;

    protected Menu m_ZoomMenu;

    private String mFilename;

    private String mTemporaryFilename;

    private String mPassword;

    private int mPageNo;

    private boolean mDocumentIsOpen = false;

    private boolean mLoadedDocument = false;

    private boolean mWelcomeShown = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences preferences = getSharedPreferences(PREFERENCES_EULA, Activity.MODE_PRIVATE);
        if (!preferences.getBoolean(PREFERENCE_EULA_ACCEPTED, false)) {
            mWelcomeShown = true;
            preferences.edit().putBoolean(PREFERENCE_EULA_ACCEPTED, true).commit();
            showDialog(DIALOG_WELCOME);
        }
        if (mDocument == null) mDocument = new DroidReaderDocument();
        PdfRender.setFontProvider(new DroidReaderFontProvider(this));
        FrameLayout fl = new FrameLayout(this);
        mReaderView = new DroidReaderView(this, null, mDocument);
        fl.addView(mReaderView);
        setContentView(fl);
        readPreferences();
        if (savedInstanceState != null) {
            mFilename = savedInstanceState.getString("filename");
            if ((new File(mFilename)).exists()) {
                mPassword = savedInstanceState.getString("password");
                mDocument.mZoom = savedInstanceState.getFloat("zoom");
                mDocument.mRotation = savedInstanceState.getInt("rotation");
                mPageNo = savedInstanceState.getInt("page");
                mDocument.mMarginOffsetX = savedInstanceState.getInt("marginOffsetX");
                mDocument.mMarginOffsetY = savedInstanceState.getInt("marginOffsetY");
                mDocument.mContentFitMode = savedInstanceState.getInt("contentFitMode");
                openDocument();
                mLoadedDocument = true;
            }
            savedInstanceState.clear();
        }
        if (!mLoadedDocument) {
            Intent intent = getIntent();
            if (intent.getData() != null) {
                mTemporaryFilename = intent.getData().toString();
                if (mTemporaryFilename.startsWith("file://")) {
                    mTemporaryFilename = mTemporaryFilename.substring(7);
                } else if (mTemporaryFilename.startsWith("/")) {
                } else if (mTemporaryFilename.startsWith("content://com.metago.astro.filesystem/")) {
                    mTemporaryFilename = mTemporaryFilename.substring(37);
                } else {
                    Toast.makeText(this, R.string.error_only_file_uris, Toast.LENGTH_SHORT).show();
                    mTemporaryFilename = null;
                }
                if (mTemporaryFilename != null) {
                    mPassword = "";
                    openDocumentWithDecodeAndLookup();
                    mLoadedDocument = true;
                }
            }
        }
        if (!mLoadedDocument) {
            tryLoadLastFile();
        }
        if (!mLoadedDocument) {
            if (!mWelcomeShown) showDialog(DIALOG_WELCOME);
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        in.close();
        in = null;
        out.flush();
        out.close();
        out = null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putString("last_open_file", mFilename).commit();
        readOrWriteDB(true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if ((mDocument != null) && mDocument.isPageLoaded()) {
            outState.putFloat("zoom", mDocument.mZoom);
            outState.putInt("rotation", mDocument.mRotation);
            outState.putInt("page", mDocument.mPage.no);
            outState.putInt("offsetX", mDocument.mOffsetX);
            outState.putInt("offsetY", mDocument.mOffsetY);
            outState.putInt("marginOffsetX", mDocument.mMarginOffsetX);
            outState.putInt("marginOffsetY", mDocument.mMarginOffsetY);
            outState.putInt("contentFitMode", mDocument.mContentFitMode);
            outState.putString("password", mPassword);
            outState.putString("filename", mFilename);
            mDocument.closeDocument();
        }
    }

    @Override
    protected void onDestroy() {
        if (mDocument != null) mDocument.closeDocument();
        super.onDestroy();
    }

    private void readPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        if (prefs.getString("zoom_type", "0").equals("0")) {
            float zoom = Float.parseFloat(prefs.getString("zoom_percent", "50"));
            if ((1 <= zoom) && (1000 >= zoom)) {
                mDocument.setZoom(zoom / 100, false);
            }
        } else {
            mDocument.setZoom(Float.parseFloat(prefs.getString("zoom_type", "0")), false);
        }
        if (prefs.getBoolean("dpi_auto", true)) {
            mDocument.setDpi((int) metrics.xdpi, (int) metrics.ydpi);
        } else {
            int dpi = Integer.parseInt(prefs.getString("dpi_manual", "160"));
            if ((dpi < 1) || (dpi > 4096)) dpi = 160;
            mDocument.setDpi(dpi, dpi);
        }
        if (prefs.getBoolean("tilesize_by_factor", true)) {
            Float factor = Float.parseFloat(prefs.getString("tilesize_factor", "1.5"));
            mDocument.setTileMax((int) (metrics.widthPixels * factor), (int) (metrics.heightPixels * factor));
        } else {
            int tilesize_x = Integer.parseInt(prefs.getString("tilesize_x", "640"));
            int tilesize_y = Integer.parseInt(prefs.getString("tilesize_x", "480"));
            if (metrics.widthPixels < metrics.heightPixels) {
                mDocument.setTileMax(tilesize_x, tilesize_y);
            } else {
                mDocument.setTileMax(tilesize_y, tilesize_x);
            }
        }
        boolean invert = prefs.getBoolean("invert_display", false);
        mDocument.setDisplayInvert(invert);
        mReaderView.setDisplayInvert(invert);
        if (prefs.getBoolean("full_screen", false)) {
            this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        mDocument.mHorizontalScrollLock = prefs.getBoolean("horizontal_scroll_lock", false);
    }

    /** Creates the menu items */
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    /** Creates the menu items */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem mi;
        super.onPrepareOptionsMenu(menu);
        return true;
    }

    /** Handles item selections */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_CODE_PICK_FILE:
                if (resultCode == RESULT_OK && data != null) {
                    setIntent(data);
                    mTemporaryFilename = data.getDataString();
                    if (mTemporaryFilename != null) {
                        if (mTemporaryFilename.startsWith("file://")) {
                            mTemporaryFilename = mTemporaryFilename.substring(7);
                        }
                        mPassword = "";
                        openDocumentWithDecodeAndLookup();
                    }
                }
                break;
            case REQUEST_CODE_OPTION_DIALOG:
                readPreferences();
                tryLoadLastFile();
                break;
        }
    }

    protected void tryLoadLastFile() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mFilename = prefs.getString("last_open_file", "");
        if (mFilename != null) {
            if ((mFilename.length() > 0) && ((new File(mFilename)).exists())) {
                mPassword = "";
                openDocumentWithLookup();
                mLoadedDocument = true;
            }
        }
    }

    protected void openDocumentWithDecodeAndLookup() {
        try {
            mTemporaryFilename = URLDecoder.decode(mTemporaryFilename, "utf-8");
            File f = new File(mTemporaryFilename);
            if ((f.exists()) && (f.isFile()) && (f.canRead())) {
                mFilename = mTemporaryFilename;
                openDocumentWithLookup();
            } else {
                Toast.makeText(this, R.string.error_file_open_failed, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_opening_document, Toast.LENGTH_LONG).show();
        }
    }

    protected void openDocumentWithLookup() {
        readOrWriteDB(false);
        openDocument();
    }

    protected void openDocument() {
        if (mDocumentIsOpen) {
            mDocument.closeDocument();
            mDocumentIsOpen = false;
        }
        try {
            this.setTitle(mFilename);
            mDocument.open(mFilename, mPassword, mPageNo);
            openPage(0, true);
            mDocumentIsOpen = true;
        } catch (PasswordNeededException e) {
            showDialog(DIALOG_GET_PASSWORD);
        } catch (WrongPasswordException e) {
            Toast.makeText(this, R.string.error_wrong_password, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_opening_document, Toast.LENGTH_LONG).show();
        }
    }

    protected void openPage(int no, boolean isRelative) {
        try {
            if (!(no == 0 && isRelative)) mDocument.openPage(no, isRelative);
            this.setTitle(new File(mFilename).getName() + String.format(" (%d/%d)", mDocument.mPage.no, mDocument.mDocument.pagecount));
            mPageNo = mDocument.mPage.no;
        } catch (PageLoadException e) {
        }
    }

    protected void setZoom(float newZoom) {
        newZoom = newZoom / (float) 100.0;
        if (newZoom > 16.0) newZoom = (float) 16.0;
        if (newZoom < 0.0625) newZoom = (float) 0.0625;
        mDocument.setZoom(newZoom, false);
    }

    public void onTap(float X, float Y) {
        float left, right, top, bottom;
        float width = mDocument.mDisplaySizeX;
        float height = mDocument.mDisplaySizeY;
        boolean prev = false;
        boolean next = false;
        if (mDocumentIsOpen) {
            left = width * (float) 0.25;
            right = width * (float) 0.75;
            top = height * (float) 0.25;
            bottom = height * (float) 0.75;
            if ((X < left) && (Y < top)) prev = true;
            if ((X < left) && (Y > bottom)) next = true;
            if ((X > right) && (Y < top)) prev = true;
            if ((X > right) && (Y > bottom)) next = true;
            if ((X > left) && (X < right) && (Y > bottom)) {
                Log.d("DroidReaderMetrics", String.format("Zoom = %5.2f%%", mDocument.mZoom * 100.0));
                Log.d("DroidReaderMetrics", String.format("Page size = (%2.0f,%2.0f)", mDocument.mPage.mMediabox[2] - mDocument.mPage.mMediabox[0], mDocument.mPage.mMediabox[3] - mDocument.mPage.mMediabox[1]));
                Log.d("DroidReaderMetrics", String.format("Display size = (%d,%d)", mDocument.mDisplaySizeX, mDocument.mDisplaySizeY));
                Log.d("DroidReaderMetrics", String.format("DPI = (%d, %d)", mDocument.mDpiX, mDocument.mDpiY));
                Log.d("DroidReaderMetrics", String.format("Content size = (%2.0f,%2.0f)", mDocument.mPage.mContentbox[2] - mDocument.mPage.mContentbox[0], mDocument.mPage.mContentbox[3] - mDocument.mPage.mContentbox[1]));
                Log.d("DroidReaderMetrics", String.format("Content offset = (%2.0f,%2.0f)", mDocument.mPage.mContentbox[0], mDocument.mPage.mContentbox[1]));
                Log.d("DroidReaderMetrics", String.format("Document offset = (%d,%d)", mDocument.mOffsetX, mDocument.mOffsetY));
            }
            if (next) {
                if (mDocument.havePage(1, true)) openPage(1, true);
            } else if (prev) {
                if (mDocument.havePage(-1, true)) openPage(-1, true);
            }
        }
    }

    protected Dialog onCreateDialog(int id) {
        switch(id) {
            case DIALOG_GET_PASSWORD:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.prompt_password);
                View passwordinput = getLayoutInflater().inflate(R.layout.passworddialog, (ViewGroup) findViewById(R.id.input_password));
                builder.setView(passwordinput);
                builder.setCancelable(false);
                builder.setPositiveButton(R.string.button_pwddialog_open, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        DroidReaderActivity.this.mPassword = ((EditText) ((AlertDialog) dialog).findViewById(R.id.input_password)).getText().toString();
                        DroidReaderActivity.this.openDocumentWithLookup();
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton(R.string.button_pwddialog_cancel, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                AlertDialog dialog = builder.create();
                return dialog;
            case DIALOG_GOTO_PAGE:
                AlertDialog.Builder gotoBuilder = new AlertDialog.Builder(this);
                gotoBuilder.setMessage(R.string.prompt_goto_page);
                View pageinput = getLayoutInflater().inflate(R.layout.pagedialog, (ViewGroup) findViewById(R.id.input_page));
                gotoBuilder.setView(pageinput);
                gotoBuilder.setCancelable(false);
                gotoBuilder.setPositiveButton(R.string.button_page_open, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            DroidReaderActivity.this.openPage(Integer.parseInt(((EditText) ((AlertDialog) dialog).findViewById(R.id.input_page)).getText().toString()), false);
                        } catch (NumberFormatException e) {
                        }
                        dialog.dismiss();
                    }
                });
                gotoBuilder.setNegativeButton(R.string.button_page_cancel, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                AlertDialog gotoDialog = gotoBuilder.create();
                return gotoDialog;
        }
        return null;
    }

    AlertDialog showHtmlDialog(int titleResource, String location) {
        AlertDialog.Builder htmlBuilder = new AlertDialog.Builder(this);
        WebView htmlWebView = new WebView(this);
        htmlWebView.loadUrl("file:///android_asset/" + location);
        htmlBuilder.setView(htmlWebView);
        htmlBuilder.setCancelable(false);
        htmlBuilder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog htmlDialog = htmlBuilder.create();
        htmlDialog.setTitle(getString(titleResource));
        return htmlDialog;
    }

    protected void readOrWriteDB(boolean doWrite) {
        SQLiteDatabase pdfDB = null;
        try {
            pdfDB = this.openOrCreateDatabase("DroidReaderPDFDB", MODE_PRIVATE, null);
            pdfDB.execSQL("CREATE TABLE IF NOT EXISTS LastReadPoint (" + "Filename VARCHAR, Zoom DECIMAL(10,5), " + "Rotation INTEGER, Page INTEGER, " + "OffsetX INTEGER, OffsetY INTEGER, " + "MarginOffsetX INTEGER, MarginOffsetY INTEGER, " + "ContentFitMode INTEGER, MemoryMode INTEGER, " + "Password VARCHAR );");
            Cursor c = pdfDB.rawQuery("SELECT * FROM LastReadPoint WHERE Filename = '" + mFilename + "'", null);
            if (c != null) {
                if (c.getCount() > 0) {
                    c.moveToFirst();
                    if (doWrite) {
                        pdfDB.execSQL("UPDATE LastReadPoint SET " + "Zoom = " + mDocument.mZoom + " , " + "Rotation = " + mDocument.mRotation + " , " + "Page = " + mPageNo + " , " + "OffsetX = " + mDocument.mOffsetX + " , " + "OffsetY = " + mDocument.mOffsetY + " , " + "MarginOffsetX = " + mDocument.mMarginOffsetX + " , " + "MarginOffsetY = " + mDocument.mMarginOffsetY + " , " + "ContentFitMode = " + mDocument.mContentFitMode + " , MemoryMode = 0 ," + "Password = '" + mPassword + "' " + "WHERE Filename = '" + mFilename + "';");
                    } else {
                        mDocument.mZoom = c.getFloat(c.getColumnIndex("Zoom"));
                        mDocument.mRotation = c.getInt(c.getColumnIndex("Rotation"));
                        mPageNo = c.getInt(c.getColumnIndex("Page"));
                        if (mPageNo == 0) mPageNo = 1;
                        mDocument.mMarginOffsetX = c.getInt(c.getColumnIndex("MarginOffsetX"));
                        mDocument.mMarginOffsetY = c.getInt(c.getColumnIndex("MarginOffsetY"));
                        mDocument.mContentFitMode = c.getInt(c.getColumnIndex("ContentFitMode"));
                    }
                } else {
                    if (doWrite) {
                        pdfDB.execSQL("INSERT INTO LastReadPoint VALUES ('" + mFilename + "', " + mDocument.mZoom + " , " + mDocument.mRotation + " , " + mPageNo + " , " + mDocument.mOffsetX + " , " + mDocument.mOffsetY + " , " + mDocument.mMarginOffsetX + " , " + mDocument.mMarginOffsetY + " , " + mDocument.mContentFitMode + " , 0 , '" + mPassword + "');");
                    } else {
                        mDocument.mZoom = DroidReaderDocument.ZOOM_FIT;
                        mDocument.mRotation = 0;
                        mDocument.mMarginOffsetX = 0;
                        mDocument.mMarginOffsetY = 0;
                        mDocument.mContentFitMode = 0;
                        mPageNo = 1;
                    }
                }
                c.close();
            } else {
                if (LOG) Log.d("DroidReaderDB", "Problem here... no Cursor, query must have failed");
            }
        } catch (SQLiteException se) {
            Log.e(getClass().getSimpleName(), "Could not create or open the database");
        } finally {
            if (pdfDB != null) {
                pdfDB.close();
            }
        }
    }
}
