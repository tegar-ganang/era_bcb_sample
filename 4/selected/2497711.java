package net.mym.bcnmetro;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

public class FileContentProvider extends ContentProvider {

    private Context ctx;

    private ParcelFileDescriptor parcel;

    @Override
    public boolean onCreate() {
        ctx = this.getContext();
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) {
        try {
            AssetManager am = ctx.getAssets();
            InputStream is = am.open("genplano.pdf");
            File dir = new File("data/data/net.mym.bcnmetro/files/");
            dir.mkdirs();
            File file = new File("data/data/net.mym.bcnmetro/files/genplano.pdf");
            file.createNewFile();
            OutputStream out = new FileOutputStream(file);
            byte buf[] = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0) out.write(buf, 0, len);
            out.close();
            is.close();
            parcel = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE);
        } catch (Exception e) {
            Log.e("Exception", e.toString());
        }
        return parcel;
    }

    @Override
    public int delete(Uri uri, String s, String[] as) {
        throw new UnsupportedOperationException("Not supported by this provider");
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException("Not supported by this provider");
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentvalues) {
        throw new UnsupportedOperationException("Not supported by this provider");
    }

    @Override
    public Cursor query(Uri uri, String[] as, String s, String[] as1, String s1) {
        throw new UnsupportedOperationException("Not supported by this provider");
    }

    @Override
    public int update(Uri uri, ContentValues contentvalues, String s, String[] as) {
        throw new UnsupportedOperationException("Not supported by this provider");
    }
}
