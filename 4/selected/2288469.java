package edu.vub.at.android.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

public class AssetInstaller extends Activity {

    private final class CopyAsyncTask extends AsyncTask<Void, String, Void> {

        final ProgressDialog pd = ProgressDialog.show(AssetInstaller.this, "Copying assets", "Please wait");

        @Override
        protected Void doInBackground(Void... params) {
            long then = System.currentTimeMillis();
            if (needToCopyDefaultAssets()) {
                Log.d("AssetInstaller", "Copying AmbientTalk assets ");
                publishProgress("Copying AmbientTalk assets");
                copyATLibFile(getResources(), getAssets(), _ASSET_ROOT_);
            }
            Log.d("AssetInstaller", "Copying project assets ");
            publishProgress("Copying project assets");
            copyAssets(getAssets(), "", _ASSET_ROOT_);
            long now = System.currentTimeMillis();
            Log.d("AssetInstaller", "Copying assets took " + (now - then) + "ms");
            try {
                marker_.createNewFile();
            } catch (IOException e) {
                Log.e("ATLibInstaller", "Could not create marker file", e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            pd.cancel();
            setResult(RESULT_OK);
            finish();
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            pd.setMessage(values[0]);
        }
    }

    private static final File _ASSET_ROOT_ = IATSettings._ENV_AT_HOME_;

    private File marker_;

    protected boolean development;

    protected boolean copyDefaultAssets;

    public AssetInstaller(boolean defaultAssets) {
        super();
        marker_ = new File(_ASSET_ROOT_, "." + getClass().getName());
        development = false;
        copyDefaultAssets = defaultAssets;
    }

    public boolean needToCopyDefaultAssets() {
        if (!copyDefaultAssets) return false;
        File defaultMarker = new File(_ASSET_ROOT_, "version.at");
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(defaultMarker)));
            int installed_version = Integer.parseInt(br.readLine());
            br.close();
            InputStream is = getResources().openRawResource(R.raw.atlib);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                if (ze.getName().equals("version.at")) {
                    br = new BufferedReader(new InputStreamReader(zis));
                    int packaged_version = Integer.parseInt(br.readLine());
                    br.close();
                    return packaged_version > installed_version;
                }
            }
            Log.e("AssetInstaller", "No version.at file in included atlib zipfile!");
            return true;
        } catch (FileNotFoundException e) {
            return true;
        } catch (IOException e) {
            return true;
        }
    }

    public AssetInstaller() {
        this(true);
    }

    private static void copyAssets(AssetManager am, String path, File destRoot) {
        try {
            String[] contents = am.list(Constants._ENV_AT_ASSETS_BASE_ + path);
            for (String f : contents) {
                String newPath = path + "/" + f;
                Log.v("AssetInstaller", newPath);
                if (f.endsWith(".at")) {
                    try {
                        copyFile(am, newPath, destRoot);
                    } catch (IOException e) {
                        Log.e("AssetInstaller", "Could not copy file " + newPath, e);
                    }
                } else {
                    copyAssets(am, newPath, destRoot);
                }
            }
        } catch (IOException e) {
            Log.v("AssetInstaller", "Could not get path " + path, e);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.installer);
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            AlertDialog alert = new AlertDialog.Builder(this).setTitle("SD card required").setMessage("An SD card is required to run AmbientTalk").setCancelable(false).setIcon(R.drawable.at_icon).setPositiveButton("OK", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int id) {
                    setResult(Constants._RESULT_FAIL_);
                    finish();
                }
            }).create();
            alert.show();
            return;
        }
        File dest = new File(Environment.getExternalStorageDirectory(), Constants._AT_TEMP_FILES_PATH);
        dest.mkdirs();
        if (!needToCopyDefaultAssets() && marker_.exists() && !development) {
            setResult(RESULT_OK);
            finish();
            return;
        }
        new CopyAsyncTask().execute((Void) null);
    }

    private static void copyATLibFile(Resources r, AssetManager am, File destRoot) {
        InputStream is = r.openRawResource(R.raw.atlib);
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
        try {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int count;
                while ((count = zis.read(buffer)) != -1) {
                    baos.write(buffer, 0, count);
                }
                String filename = ze.getName();
                byte[] bytes = baos.toByteArray();
                File dest = new File(destRoot, filename);
                if (dest.exists()) {
                    dest.delete();
                }
                if (filename.endsWith(".at") && !(filename.contains("MACOSX"))) {
                    try {
                        Log.v("AssetInstaller", filename);
                        dest.getParentFile().mkdirs();
                        FileOutputStream fos = new FileOutputStream(dest);
                        fos.write(bytes);
                    } catch (IOException e) {
                        Log.v("AssetInstaller", "Error while copying " + filename);
                    }
                }
            }
        } catch (IOException e) {
            Log.e("AssetInstaller", "Error while copying atlib zip file ", e);
        } finally {
            try {
                zis.close();
            } catch (IOException e) {
                Log.e("AssetInstaller", "Error while closing atlib zip file ", e);
            }
        }
    }

    private static void copyFile(AssetManager am, String path, File destRoot) throws IOException {
        InputStream is;
        int size;
        byte buf[] = new byte[4096];
        try {
            is = am.open(Constants._ENV_AT_ASSETS_BASE_ + path);
        } catch (IOException e) {
            throw e;
        }
        File dest = new File(destRoot, path);
        if (dest.exists()) {
            dest.delete();
        }
        try {
            dest.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(dest);
            while (-1 != (size = is.read(buf))) fos.write(buf, 0, size);
        } catch (IOException e) {
            throw e;
        } finally {
            is.close();
        }
    }
}
