package com.sen.imageslider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ImageContainer {

    public static final int LOAD_ALL_IMAGES = 0;

    public static final int LOAD_LAST_IMAGE = 1;

    public static final int LOAD_FIRST_IMAGE = 2;

    private ArrayList<Bitmap> garbage;

    private ImageList list;

    private int imageCount;

    private int startPosition;

    private Bitmap[] images;

    public ImageContainer(List<String> arrayList, int startPosition, Context context) {
        this(arrayList, startPosition, context, 3);
    }

    public ImageContainer(List<String> arrayList, int startPosition, Context context, int imageCount) {
        list = new ImageList(arrayList, startPosition, context);
        garbage = new ArrayList<Bitmap>();
        this.imageCount = imageCount;
        this.startPosition = imageCount / 2;
        images = new Bitmap[imageCount];
        new LoadImageTask().execute(new Integer[] { LOAD_ALL_IMAGES });
    }

    private void loadImages() {
        int listPosition = list.getPosition();
        if (images[startPosition] != null) garbage.add(images[startPosition]);
        images[startPosition] = list.getImage(listPosition);
        boolean listEnd = !((listPosition + 1 < list.count()) || (listPosition - 1 >= 0));
        for (int i = 1; (i <= imageCount / 2) && !listEnd; i++) {
            if (listPosition + i < list.count()) {
                if (images[startPosition + i] != null) garbage.add(images[startPosition + i]);
                images[startPosition + i] = list.getImage(listPosition + i);
            }
            if (listPosition - i >= 0) {
                if (images[startPosition - i] != null) garbage.add(images[startPosition - i]);
                images[startPosition - i] = list.getImage(listPosition - i);
            }
            listEnd = !((listPosition + i + 1 < list.count()) || (listPosition - i - 1 >= 0));
        }
    }

    public Bitmap getPrevImage() {
        if (list.getPosition() != list.toPrev()) {
            if (images[imageCount - 1] != null) {
                garbage.add(images[imageCount - 1]);
                images[imageCount - 1] = null;
            }
            for (int i = imageCount - 1; i > 0; i--) images[i] = images[i - 1];
            images[0] = null;
            new LoadImageTask().execute(new Integer[] { LOAD_FIRST_IMAGE });
        }
        return images[startPosition];
    }

    public Bitmap getNextImage() {
        Log.w("Debug main", Long.toString(Thread.currentThread().getId()));
        if (list.getPosition() != list.toNext()) {
            if (images[0] != null) {
                garbage.add(images[0]);
                images[0] = null;
            }
            for (int i = 0; i < imageCount - 1; i++) images[i] = images[i + 1];
            images[imageCount - 1] = null;
            new LoadImageTask().execute(new Integer[] { LOAD_LAST_IMAGE });
        }
        return images[startPosition];
    }

    public Bitmap getCurrentImage() {
        return list.getCurrentImage();
    }

    private void clean() {
        if (garbage.size() > 0) {
            for (Bitmap b : garbage) {
                if (b != null) b.recycle();
                b = null;
            }
            garbage.clear();
        }
    }

    public boolean isLast() {
        return list.isLast();
    }

    public boolean isFirst() {
        return list.isFirst();
    }

    class MyHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            Log.w("Debug", Long.toString(Thread.currentThread().getId()));
            switch(msg.what) {
                case LOAD_ALL_IMAGES:
                    ImageContainer.this.loadImages();
                    break;
                case LOAD_FIRST_IMAGE:
                    ImageContainer.this.loadFirstImage();
                    break;
                case LOAD_LAST_IMAGE:
                    ImageContainer.this.loadLastImage();
                    break;
            }
            ImageContainer.this.clean();
            Log.w("Debug", "MyHandler: ");
        }

        public boolean loadImage(int what) {
            this.removeMessages(what);
            return sendMessage(obtainMessage(what));
        }
    }

    public void loadFirstImage() {
        synchronized (images) {
            int target = -1;
            for (int i = startPosition; i >= 0; i--) if (images[i] == null) {
                target = i;
                break;
            }
            if (target >= 0 && (list.getPosition() - (startPosition - target)) >= 0) images[target] = list.getImage(list.getPosition() - (startPosition - target));
        }
    }

    public void loadLastImage() {
        synchronized (images) {
            int target = -1;
            for (int i = startPosition; i < imageCount; i++) if (images[i] == null) {
                target = i;
                break;
            }
            if (target >= 0 && (list.getPosition() + target - startPosition) < list.count()) images[target] = list.getImage(list.getPosition() + target - startPosition);
        }
    }

    private class LoadImageTask extends AsyncTask<Integer, Void, Void> {

        protected Void doInBackground(Integer... params) {
            switch(params[0]) {
                case LOAD_ALL_IMAGES:
                    ImageContainer.this.loadImages();
                    break;
                case LOAD_FIRST_IMAGE:
                    ImageContainer.this.loadFirstImage();
                    break;
                case LOAD_LAST_IMAGE:
                    ImageContainer.this.loadLastImage();
                    break;
            }
            Log.w("Debug async", Long.toString(Thread.currentThread().getId()));
            ImageContainer.this.clean();
            return null;
        }
    }

    public String getCurrentImagePath() {
        return list.getCurrentImagePath();
    }

    public Bitmap tryGetNextImage() {
        return images[startPosition + 1];
    }

    public Bitmap tryGetPrevImage() {
        return images[startPosition - 1];
    }

    public int getCurrentPosition() {
        return list.getPosition();
    }

    public void cleanAll() {
        clean();
        for (Bitmap bmp : images) {
            if (bmp != null) {
                bmp.recycle();
                bmp = null;
            }
        }
    }

    public void updateCurrentImage(Bitmap scaledBitmap, Integer pos) {
        int current = list.getPosition();
        int index = pos - current + startPosition;
        if (index >= 0 && index < imageCount) {
            clean();
            garbage.add(images[index]);
            images[index] = Bitmap.createBitmap(scaledBitmap);
        }
    }

    public int getSize() {
        return list.count();
    }
}
