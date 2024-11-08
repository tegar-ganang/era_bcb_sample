package org.colorvision;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

/**
 * Provides assets under content://org.colorvision/.
 * @author Adam Crume
 */
public class AssetContentProvider extends ContentProvider {

    private static final String TAG = "ColorVision";

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        Log.v(TAG, "AssetContentProvider.openFile(\"" + uri + "\", \"" + mode + "\")");
        try {
            File assetsCache = new File(getContext().getCacheDir(), "assets");
            checkVersion(assetsCache);
            String path = uri.getPath().replaceAll("^/", "");
            File cached = cache(assetsCache, path);
            return ParcelFileDescriptor.open(cached, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Unable to find " + uri, e);
            throw e;
        } catch (IOException e) {
            Log.w(TAG, "Unable to find " + uri, e);
            FileNotFoundException e2 = new FileNotFoundException(e.toString());
            e2.initCause(e);
            throw e2;
        }
    }

    /**
	 * Caches an asset.
	 * @param assetsCache directory containing the asset cache
	 * @param path asset to cache
	 * @return file containing the cached asset
	 * @throws IOException if something went wrong
	 */
    private File cache(File assetsCache, String path) throws IOException {
        File file = new File(assetsCache, path);
        if (!file.exists()) {
            InputStream in = getContext().getAssets().open(path);
            try {
                file.getParentFile().mkdirs();
                OutputStream out = new FileOutputStream(file);
                try {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }
        }
        return file;
    }

    /**
	 * Checks that the version of the cache matches the version of the application.
	 * Necessary so old cached files aren't used after the app is updated.
	 * @param assetsCache
	 * @throws IOException
	 */
    private synchronized void checkVersion(File assetsCache) throws IOException {
        Context context = getContext();
        String packageName = context.getPackageName();
        String currentVersion;
        try {
            currentVersion = context.getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (NameNotFoundException e) {
            IOException e2 = new IOException(e.toString());
            e2.initCause(e);
            throw e2;
        }
        try {
            String version;
            BufferedReader in = new BufferedReader(new FileReader(new File(assetsCache, "version.txt")));
            try {
                version = in.readLine();
            } finally {
                in.close();
            }
            if (!currentVersion.equals(version)) {
                clearCache(assetsCache, currentVersion);
            }
        } catch (IOException e) {
            clearCache(assetsCache, currentVersion);
        }
    }

    /**
	 * Removes everything from the cache folder, ensures it exists, and writes the application version to version.txt.
	 * @param assetsCache cache location
	 * @param currentVersion current version of the app
	 * @throws IOException if something went wrong
	 */
    private void clearCache(File assetsCache, String currentVersion) throws IOException {
        if (assetsCache.exists()) {
            for (File f : assetsCache.listFiles()) {
                delete(f);
            }
        }
        assetsCache.mkdirs();
        FileWriter w = new FileWriter(new File(assetsCache, "version.txt"));
        try {
            w.write(currentVersion);
        } finally {
            w.close();
        }
    }

    /**
	 * Deletes a file and all subfiles, if it's a folder.
	 * @param f file to delete
	 * @throws IOException if a file could not be deleted
	 */
    private void delete(File f) throws IOException {
        if (f.isDirectory()) {
            for (File sub : f.listFiles()) {
                delete(sub);
            }
        } else if (f.exists()) {
            if (!f.delete()) {
                throw new IOException("Unable to delete " + f);
            }
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean onCreate() {
        Log.v(TAG, "AssetContentProvider.onCreate()");
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
