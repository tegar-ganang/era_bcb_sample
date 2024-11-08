package net.sf.astroobserver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

public class DataBaseHelper2 extends SQLiteOpenHelper {

    private String DB_PATH = Environment.getDataDirectory() + "/data/net.sf.astroobserver/databases";

    private static String DB_NAME = "dbraf.db";

    private SQLiteDatabase myDataBase;

    private final Context myContext;

    private String DbPath2m5 = Environment.getExternalStorageDirectory().getAbsolutePath() + "/astroobserver";

    private boolean boolBase2m5 = false;

    /**
     * Constructeur
     * 
     * @param context
     */
    public DataBaseHelper2(Context context) {
        super(context, DB_NAME, null, 3);
        this.myContext = context;
        File f = new File(DbPath2m5 + "/" + DB_NAME);
        if (f.exists()) {
            DB_PATH = DbPath2m5;
            boolBase2m5 = true;
        }
        ;
    }

    /**
     * On cr� une base vide dans le system Android et on r��cris dessus.
     * */
    public void createDataBase() throws IOException {
        if (boolBase2m5) {
            return;
        }
        boolean dbExist = checkDataBase();
        SQLiteDatabase db_Read = null;
        db_Read = this.getReadableDatabase();
        db_Read.close();
        try {
            copyDataBase();
        } catch (IOException e) {
            throw new Error("Erreur de copie de la base !");
        }
    }

    /**
     * On Check la base pour voir si elle existe ou pas
     *  
     * @return true si la base existe, false si elle existe pas.
     */
    private boolean checkDataBase() {
        SQLiteDatabase checkDB = null;
        try {
            String myPath = DB_PATH + "/" + DB_NAME;
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
        } catch (SQLiteException e) {
        }
        if (checkDB != null) {
            checkDB.close();
        }
        return checkDB != null ? true : false;
    }

    /**
     * On copie la base depuis le dossier assets dans le dossier system des bases sous android dans la base vide fraichement cr��.
     *
     * */
    private void copyDataBase() throws IOException {
        String Path = Environment.getDataDirectory() + "/data/net.sf.astroobserver/databases/";
        File saoFile = new File(Path, "dbraf.db");
        saoFile.delete();
        AssetManager assetManager = myContext.getAssets();
        OutputStream os = new FileOutputStream(saoFile);
        saoFile.createNewFile();
        byte[] b = new byte[1024];
        int i, r;
        String[] Files = assetManager.list("");
        Arrays.sort(Files);
        for (i = 1; i <= 17; i++) {
            String fn = String.format("file_%d.bin", i);
            if (Arrays.binarySearch(Files, fn) < 0) break;
            InputStream is = assetManager.open(fn);
            while ((r = is.read(b)) != -1) os.write(b, 0, r);
            is.close();
        }
        os.close();
    }

    /**
     *Fonction pour acceder � la base en mode lecture seul
     **/
    public void openDataBase() throws SQLException {
        String myPath = DB_PATH + "/" + DB_NAME;
        myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
    }

    @Override
    public synchronized void close() {
        if (myDataBase != null) myDataBase.close();
        super.close();
    }

    public void exec(String req) {
        myDataBase.execSQL(req);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public List<StarSao> selectAll() {
        List<StarSao> list = new ArrayList<StarSao>();
        Cursor cursor = myDataBase.query("sao", new String[] { "ra", "dec", "saonum", "mag" }, " id <= 20000 ", null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                StarSao st = new StarSao();
                AstroCompute.raDecDegToXYZ(st.xyz, cursor.getFloat(0), cursor.getFloat(1));
                st.saoNumber = cursor.getInt(2);
                st.magnitude = cursor.getFloat(3);
                list.add(st);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return list;
    }

    public ArrayList<StarSearch> selectNameStar(String where) {
        ArrayList<StarSearch> list = new ArrayList<StarSearch>();
        Cursor cursor = myDataBase.query("sao", new String[] { "name", "ra", "dec", "saonum", "_id" }, where, null, null, null, "name");
        if (cursor.moveToFirst()) {
            do {
                StarSearch st = new StarSearch();
                st.name = cursor.getString(0);
                AstroCompute.raDecDegToXYZ(st.xyz, cursor.getFloat(1), cursor.getFloat(2));
                st.saoNumber = cursor.getInt(3);
                st._id = cursor.getInt(4);
                list.add(st);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return list;
    }

    public void selectWhereStarSao(String _where) {
        Cursor cursor;
        if (boolBase2m5) {
            cursor = myDataBase.query("tycho", new String[] { "ra", "dec", "tyc", "vmag" }, _where, null, null, null, null);
        } else {
            cursor = myDataBase.query("sao", new String[] { "ra", "dec", "saonum", "mag" }, _where, null, null, null, null);
        }
        double cpt = 0;
        Global.listStarSao250.clear();
        if (cursor.moveToFirst()) {
            do {
                StarSao st = new StarSao();
                AstroCompute.raDecDegToXYZ(st.xyz, cursor.getFloat(0), cursor.getFloat(1));
                st.saoNumber = cursor.getInt(2);
                st.magnitude = cursor.getFloat(3);
                Global.listStarSao250.add(st);
                cpt++;
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    public void selectWhereStar(String _where) {
        Cursor cursor = myDataBase.query("sao", new String[] { "ra", "dec", "saonum", "mag", "name" }, _where, null, null, null, null);
        double cpt = 0;
        String starName = "";
        Global.listStar.clear();
        if (cursor.moveToFirst()) {
            do {
                Star st = new Star();
                AstroCompute.raDecDegToXYZ(st.xyz, cursor.getFloat(0), cursor.getFloat(1));
                st.saoNumber = "" + cursor.getInt(2);
                st.name = "" + cursor.getInt(2);
                if (!st.saoNumber.equals("0")) st.name = "SAO" + st.saoNumber; else st.name = "SAO unknow ";
                starName = cursor.getString(4);
                if (starName == null) starName = "";
                st.starName = starName;
                st.magnitude = (float) Math.floor(cursor.getFloat(3) * 100.0) / 100;
                Global.listStar.add(st);
                cpt++;
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    public void selectWhereNgcIC(String _where, Context ctx) {
        Cursor cursor = myDataBase.query("ngcic", new String[] { "ra", "dec", "name", "shortype", "constellation", "mag", "size", "num", "type" }, _where, null, null, null, null);
        double cpt = 0;
        String shortype = "";
        if (cursor.moveToFirst()) {
            do {
                NgcIc ngc = new NgcIc();
                AstroCompute.raDecDegToXYZ(ngc.xyz, cursor.getFloat(0), cursor.getFloat(1));
                ngc.name = cursor.getString(2);
                shortype = cursor.getString(3);
                ngc.magnitude = (float) Math.floor(cursor.getFloat(5) * 100.0) / 100;
                ngc.size = Math.floor(cursor.getFloat(6) * 100.0) / 100;
                ngc.num = cursor.getInt(7);
                ngc.icOrNgc = cursor.getString(8);
                ngc.typeShort = shortype;
                if (shortype.equalsIgnoreCase("Gx")) {
                    ngc.type = ctx.getString(R.string.galaxy);
                } else if (shortype.equalsIgnoreCase("OC")) {
                    ngc.type = ctx.getString(R.string.openCluster);
                } else if (shortype.equalsIgnoreCase("gb")) {
                    ngc.type = ctx.getString(R.string.globularCluster);
                } else if (shortype.equalsIgnoreCase("nb")) {
                    ngc.type = ctx.getString(R.string.brightEmission);
                } else if (shortype.equalsIgnoreCase("pl")) {
                    ngc.type = ctx.getString(R.string.planetaryNebula);
                } else if (shortype.equalsIgnoreCase("C+N")) {
                    ngc.type = ctx.getString(R.string.clusterWithNebulosity);
                } else if (shortype.equalsIgnoreCase("Ast")) {
                    ngc.type = ctx.getString(R.string.asterism);
                } else {
                    ngc.type = "";
                }
                Global.listNGC.add(ngc);
                cpt++;
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }
}
