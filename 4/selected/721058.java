package cn.edu.zucc.leyi.db;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import cn.edu.zucc.leyi.R;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

/**
 * 
 * @Title: DateSource
 * @Description:��ȡ��ݿ����ӣ�ŵû����ݿ��ļ��������ݿ��ļ���assets�ļ����б�����SD��
 * @param @return �趨�ļ�
 * @return SQLiteDatabase ��������
 * @throws
 */
public class DateSource {

    private static final String DATABASE_PATH = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/LeYiDict";

    private static final String DATABASE_FILENAME = "LeYiDict.db";

    private static SQLiteDatabase database = null;

    private static String databaseFilename = DATABASE_PATH + "/" + DATABASE_FILENAME;

    public static SQLiteDatabase getDateSource(Context mContext) {
        try {
            File dir = new File(DATABASE_PATH);
            if (!dir.exists()) dir.mkdir();
            if (!(new File(databaseFilename)).exists()) {
                if (ZipDateSource(mContext, R.raw.leyi_zip) == 0) Toast.makeText(mContext, "��ݿ���س����", Toast.LENGTH_LONG).show();
            }
            if (database == null) database = SQLiteDatabase.openOrCreateDatabase(databaseFilename, null);
            return database;
        } catch (Exception e) {
            Log.i("error open datebase", e.toString());
            Toast.makeText(mContext, R.string.cant_write_db, Toast.LENGTH_LONG);
        }
        return null;
    }

    public static int ZipDateSource(Context mCtx, int mDBRawResource) {
        int len = 1024;
        int readCount = 0, readSum = 0;
        byte[] buffer = new byte[len];
        int StreamLen = 0;
        InputStream inputStream;
        OutputStream output;
        try {
            inputStream = mCtx.getResources().openRawResource(mDBRawResource);
            output = new FileOutputStream(databaseFilename);
            ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(inputStream));
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            BufferedInputStream b = new BufferedInputStream(zipInputStream);
            StreamLen = (int) zipEntry.getSize();
            while ((readCount = b.read(buffer)) != -1) {
                output.write(buffer, 0, readCount);
                readSum = readSum + readCount;
            }
            output.flush();
            output.close();
            inputStream.close();
        } catch (IOException e) {
            Log.i("zip io", e.toString());
        }
        return readSum;
    }

    public static int nDateSource(Context mCtx, int mDBRawResource) {
        int count = 0;
        try {
            InputStream is = mCtx.getResources().openRawResource(mDBRawResource);
            FileOutputStream fos = new FileOutputStream(databaseFilename);
            byte[] buffer = new byte[8192];
            while ((count = is.read(buffer)) > 0) {
                fos.write(buffer, 0, count);
            }
            fos.close();
            is.close();
        } catch (IOException e) {
            Log.i("nDateSource io exception", e.toString());
        }
        return count;
    }
}
