package com.manning.aip.mymoviesdatabase;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;
import com.manning.aip.mymoviesdatabase.util.ImageCache;
import com.manning.aip.mymoviesdatabase.util.ImageUtil;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class DownloadTask extends AsyncTask<String, Void, Bitmap> {

    private final ImageCache cache;

    private final Drawable placeholder;

    protected final ImageView imageView;

    public DownloadTask(ImageCache cache, ImageView imageView) {
        this.cache = cache;
        this.imageView = imageView;
        Resources resources = imageView.getContext().getResources();
        this.placeholder = resources.getDrawable(android.R.drawable.gallery_thumb);
    }

    @Override
    protected void onPreExecute() {
        imageView.setImageDrawable(placeholder);
    }

    @Override
    protected Bitmap doInBackground(String... inputUrls) {
        Log.d(Constants.LOG_TAG, "making HTTP trip for image:" + inputUrls[0]);
        Bitmap bitmap = null;
        try {
            URL url = new URL(inputUrls[0]);
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            bitmap = BitmapFactory.decodeStream(conn.getInputStream());
            if (bitmap != null) {
                bitmap = ImageUtil.getRoundedCornerBitmap(bitmap, 12);
                cache.put(inputUrls[0], bitmap);
            }
        } catch (MalformedURLException e) {
            Log.e(Constants.LOG_TAG, "Exception loading image, malformed URL", e);
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, "Exception loading image, IO error", e);
        }
        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (bitmap != null) {
            this.imageView.setImageBitmap(bitmap);
        }
    }
}
