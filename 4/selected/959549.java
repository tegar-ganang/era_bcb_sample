package orxatas.travelme.sync;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import orxatas.travelme.activity.AsyncActivity;
import orxatas.travelme.databases.TravelmeDatabase;
import orxatas.travelme.databases.exceptions.PhotoNoInLocal;
import orxatas.travelme.entity.Photo;
import orxatas.travelme.manager.AccountManager;
import orxatas.travelme.manager.DataManager;
import orxatas.travelme.manager.PhotoManager;
import orxatas.travelme.sync.SyncOptions.ParseAnswer;
import orxatas.travelme.sync.SyncOptions.WithoutObject;

public class SyncPhotos extends SyncOptions {

    private AsyncActivity asyncActivity;

    private PhotoManager photoManager;

    /**
	 * Base de datos.
	 * */
    private SQLiteDatabase trvlmdb;

    private TravelmeDatabase databaseHelper;

    public SyncPhotos(PhotoManager photoManager, AsyncActivity asyncActivity) {
        this.photoManager = photoManager;
        this.asyncActivity = asyncActivity;
        databaseHelper = new TravelmeDatabase(asyncActivity.getActivity(), new AccountManager(asyncActivity).getUserLogged().getId());
    }

    /**
	 * Get the requested photo entry
	 * @throws PhotoNoInLocal 
	 * */
    public Photo getPhoto(int idPhotoL) throws PhotoNoInLocal {
        Photo p = getPhotoOrNull(idPhotoL);
        if (p == null) throw new PhotoNoInLocal(idPhotoL);
        return p;
    }

    private Photo getPhotoOrNull(int idPhotoL) {
        trvlmdb = databaseHelper.getReadableDatabase();
        Cursor c = trvlmdb.rawQuery("SELECT * FROM " + TravelmeDatabase.PHOTO_TNAME + " WHERE " + TravelmeDatabase.PHOTO_CIDL + " = " + idPhotoL, null);
        if (!c.moveToFirst()) {
            return null;
        }
        int cid_cidL = c.getColumnIndex(TravelmeDatabase.PHOTO_CIDL);
        int cid_cidO = c.getColumnIndex(TravelmeDatabase.PHOTO_CIDO);
        int cid_cdir = c.getColumnIndex(TravelmeDatabase.PHOTO_CLOCALDIR);
        int cid_curl = c.getColumnIndex(TravelmeDatabase.PHOTO_CURL);
        int cid_creq = c.getColumnIndex(TravelmeDatabase.PHOTO_CREQUESTED);
        int cid_sync = c.getColumnIndex(TravelmeDatabase.PHOTO_CSYNC);
        int idL = c.getInt(cid_cidL);
        int idO = c.getInt(cid_cidO);
        String filename = c.getString(cid_cdir);
        String url = c.getString(cid_curl);
        int req = c.getInt(cid_creq);
        int sync = c.getInt(cid_sync);
        if (sync != 0 && url.length() == 0) {
            c.close();
            trvlmdb.close();
            new GetPhotoCall(idL).execute();
            return null;
        }
        java.io.File file = new java.io.File(filename);
        if (!file.isFile() && sync != 0) {
            if (req == 0) {
                ContentValues v = new ContentValues();
                v.put(TravelmeDatabase.PHOTO_CREQUESTED, 1);
                trvlmdb.update(TravelmeDatabase.PHOTO_TNAME, v, TravelmeDatabase.PHOTO_CIDL + " = " + idPhotoL, null);
            }
            new DownloadImageInternet(idPhotoL, idO + ".jpg", url).execute();
            c.close();
            trvlmdb.close();
            return null;
        } else if (!file.isFile()) {
            c.close();
            trvlmdb.close();
            return null;
        }
        Photo p = new Photo(idL, idO, url);
        p.setLocalPath(filename);
        c.close();
        trvlmdb.close();
        return p;
    }

    private class AnswerPhoto extends Answer<Photo> {
    }

    public class GetPhotoCall extends AsyncInternetConnection {

        private int eid;

        private int idP;

        public GetPhotoCall(int idP) {
            super(TRVLMWS);
            this.eid = eid;
            this.idP = idP;
            trvlmdb = databaseHelper.getReadableDatabase();
            Cursor c = trvlmdb.rawQuery("SELECT * FROM " + TravelmeDatabase.PHOTO_TNAME + " WHERE " + TravelmeDatabase.PHOTO_CIDL + " = " + idP, null);
            c.moveToFirst();
            int idPhotoO = c.getInt(c.getColumnIndex(TravelmeDatabase.PHOTO_CIDO));
            trvlmdb.close();
            c.close();
            List<NameValuePair> pairs = basicGETParams(new AccountManager(asyncActivity), METHOD_EM, ACTION_EM_GETPHOTO);
            pairs.add(new BasicNameValuePair("phid", "" + idPhotoO));
            addGETParams(pairs);
        }

        @Override
        protected void onPostExecute(String response) {
            try {
                Answer<Photo> answer = new ParseAnswer<AnswerPhoto>().parse(response, true, AnswerPhoto.class);
                if (answer.getState() != 0) Log.d("ENTRY", "Bad photo upload!");
                Photo newP = answer.getObj();
                trvlmdb = databaseHelper.getWritableDatabase();
                ContentValues v = new ContentValues();
                v.put(TravelmeDatabase.PHOTO_CSYNC, 1);
                v.put(TravelmeDatabase.PHOTO_CIDO, newP.getIdOnline());
                v.put(TravelmeDatabase.PHOTO_CURL, newP.getUrl());
                v.put(TravelmeDatabase.PHOTO_CREQUESTED, 0);
                v.put(TravelmeDatabase.PHOTO_CLOCALDIR, "");
                trvlmdb.update(TravelmeDatabase.PHOTO_TNAME, v, TravelmeDatabase.PHOTO_CIDL + " = " + idP, null);
                trvlmdb.close();
                new DataManager(asyncActivity).asyncNotice(AsyncNoticeCode.PHOTO_CHANGES);
            } catch (WithoutObject e) {
                Log.d("ENTRY", "Bad photo upload!");
            }
        }
    }

    public class DownloadImageInternet extends AsyncTask<Void, Void, Void> {

        private String imgpath;

        private String url;

        private int idL;

        public DownloadImageInternet(int idL, String imgpath, String url) {
            this.idL = idL;
            this.imgpath = imgpath;
            this.url = url;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            new DataManager(asyncActivity).asyncNotice(AsyncNoticeCode.PHOTO_CHANGES);
        }

        @Override
        protected Void doInBackground(Void... params) {
            InputStream is = null;
            OutputStream os = null;
            try {
                URL urlresource = new URL(TRVLMWS + url);
                is = (InputStream) urlresource.getContent();
                String dir = Environment.getExternalStorageDirectory() + "/travelme/imagescache/";
                File directories = new File(dir);
                directories.mkdirs();
                File file = new File(directories, imgpath);
                os = new FileOutputStream(file);
                byte[] buffer = new byte[4096];
                for (int n; (n = is.read(buffer)) != -1; ) os.write(buffer, 0, n);
                trvlmdb = databaseHelper.getWritableDatabase();
                ContentValues v = new ContentValues();
                v.put(TravelmeDatabase.PHOTO_CREQUESTED, 0);
                v.put(TravelmeDatabase.PHOTO_CLOCALDIR, dir + imgpath);
                trvlmdb.update(TravelmeDatabase.PHOTO_TNAME, v, TravelmeDatabase.PHOTO_CIDL + " = " + idL, null);
                trvlmdb.close();
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
            }
            if (is != null) try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (os != null) try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
