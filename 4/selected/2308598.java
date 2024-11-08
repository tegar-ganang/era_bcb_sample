package com.beem.project.beem.service;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.beem.project.beem.providers.AvatarProvider;
import com.beem.project.beem.smack.avatar.AvatarCache;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An implementation of an AvatarCache which store the data of the filesystem.
 */
public class BeemAvatarCache implements AvatarCache {

    private static final String TAG = BeemAvatarCache.class.getSimpleName();

    private Context mContext;

    private ContentResolver mContentResolver;

    /**
	 * Create a BeemAvatarCache.
	 * 
	 * @param ctx
	 *            The android context of the cache.
	 */
    public BeemAvatarCache(final Context ctx) {
        mContext = ctx;
        mContentResolver = mContext.getContentResolver();
    }

    @Override
    public void put(String key, byte[] data) throws IOException {
        Uri uri = AvatarProvider.CONTENT_URI.buildUpon().appendPath(key).build();
        OutputStream os = new BufferedOutputStream(mContentResolver.openOutputStream(uri));
        try {
            os.write(data);
        } finally {
            os.close();
        }
    }

    @Override
    public void put(String key, InputStream in) throws IOException {
        Uri uri = AvatarProvider.CONTENT_URI.buildUpon().appendPath(key).build();
        OutputStream os = new BufferedOutputStream(mContentResolver.openOutputStream(uri));
        try {
            byte[] data = new byte[1024];
            int nbread;
            while ((nbread = in.read(data)) != -1) os.write(data, 0, nbread);
        } finally {
            in.close();
            os.close();
        }
    }

    @Override
    public byte[] get(String key) throws IOException {
        Uri uri = AvatarProvider.CONTENT_URI.buildUpon().appendPath(key).build();
        InputStream is = new BufferedInputStream(mContentResolver.openInputStream(uri));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            byte[] data = new byte[1024];
            is.read(data);
            bos.write(data);
        } finally {
            is.close();
        }
        return bos.toByteArray();
    }

    @Override
    public boolean contains(String key) {
        Uri uri = AvatarProvider.CONTENT_URI.buildUpon().appendPath(key).build();
        Cursor c = mContentResolver.query(uri, null, null, null, null);
        boolean res = c.getCount() > 0;
        c.close();
        return res;
    }
}
