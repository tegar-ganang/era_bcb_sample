package org.sharp.vocreader.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.sharp.android.autils.AIOUtils;
import org.sharp.android.autils.APhoneUtils;
import org.sharp.android.autils.AUtils;
import org.sharp.android.autils.HttpUtils;
import org.sharp.android.gadget.TaskUtils;
import org.sharp.intf.DownloadEventListener;
import org.sharp.intf.Logger;
import org.sharp.intf.Response;
import org.sharp.intf.TextConsumer;
import org.sharp.intf.UnzipEventListener;
import org.sharp.utils.IOUtils;
import org.sharp.vocreader.beans.State;
import org.sharp.vocreader.intf.Constants;
import org.sharp.vocreader.intf.OsSupport;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;

public class OsSupporter implements OsSupport, Logger {

    private MediaPlayer mp = new MediaPlayer();

    private SharedPreferences mSP;

    private TextConsumer mTC;

    private Context mCtx;

    public Response httpGet(String url) {
        Response response = HttpUtils.get(url);
        return response;
    }

    public void copyAssetFile(String srcFileName, String targetFilePath) {
        AIOUtils.copyAssetFile(mCtx, srcFileName, storagePath(targetFilePath));
    }

    public OsSupporter(SharedPreferences sp, TextConsumer tc, Context ctx) {
        mSP = sp;
        mTC = tc;
        mCtx = ctx;
    }

    public String getUniqueId() {
        return APhoneUtils.getUniqueId(mCtx);
    }

    public String getMdn() {
        return APhoneUtils.fetchMdn(mCtx);
    }

    public String getImei() {
        return APhoneUtils.fetchImei(mCtx);
    }

    public void logStoragePath(String path) {
        log("storagePath:" + storagePath(Constants.jpMp3Path));
    }

    String storagePath(String path) {
        String spath = AIOUtils.storagePath(path);
        if (spath == null) return internalStoragePath(path); else return spath;
    }

    private String internalStoragePath(String path) {
        return IOUtils.fullPath(mCtx.getFilesDir().getPath(), path);
    }

    public void playMp3(String mp3Path) {
        mp.reset();
        try {
            mp3Path = storagePath(mp3Path);
            log("about to play file " + mp3Path);
            FileInputStream mp3file = new FileInputStream(mp3Path);
            mp.setDataSource(mp3file.getFD());
            mp.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mp.start();
    }

    @Override
    public void log(String message) {
        AUtils.logMessage(mTC, message);
        AUtils.log(message);
        AIOUtils.log(message);
    }

    public State loadState() {
        State state = new State();
        state.courseNo = mSP.getInt(State.LATEST_COURSENO, Constants.ILLEGAL_INT);
        state.unitNo = mSP.getInt(State.LATEST_UNITNO, Constants.ILLEGAL_INT);
        state.courseOrUnit = mSP.getBoolean(State.COURSE_OR_UNIT, true);
        state.current = mSP.getInt(State.LATEST_INDEX, Constants.ILLEGAL_INT);
        state.level = mSP.getInt(State.LATEST_LEVEL, Constants.ILLEGAL_INT);
        state.lac = mSP.getInt(State.LEVELA_INDEX, Constants.ILLEGAL_INT);
        state.lal = mSP.getInt(State.LEVELA_LAST, Constants.ILLEGAL_INT);
        state.l0c = mSP.getInt(State.LEVEL0_INDEX, Constants.ILLEGAL_INT);
        state.l0l = mSP.getInt(State.LEVEL0_LAST, Constants.ILLEGAL_INT);
        state.l1c = mSP.getInt(State.LEVEL1_INDEX, Constants.ILLEGAL_INT);
        state.l1l = mSP.getInt(State.LEVEL1_LAST, Constants.ILLEGAL_INT);
        state.l2c = mSP.getInt(State.LEVEL2_INDEX, Constants.ILLEGAL_INT);
        state.l2l = mSP.getInt(State.LEVEL2_LAST, Constants.ILLEGAL_INT);
        state.l3c = mSP.getInt(State.LEVEL3_INDEX, Constants.ILLEGAL_INT);
        state.l3l = mSP.getInt(State.LEVEL3_LAST, Constants.ILLEGAL_INT);
        state.l4c = mSP.getInt(State.LEVEL4_INDEX, Constants.ILLEGAL_INT);
        state.l4l = mSP.getInt(State.LEVEL4_LAST, Constants.ILLEGAL_INT);
        state.bonus = mSP.getInt(State.BONUS, Constants.ILLEGAL_INT);
        return state;
    }

    public void saveState(State state) {
        SharedPreferences.Editor editor = mSP.edit();
        editor.putInt(State.LATEST_COURSENO, state.courseNo);
        editor.putInt(State.LATEST_UNITNO, state.unitNo);
        editor.putBoolean(State.COURSE_OR_UNIT, state.courseOrUnit);
        editor.putInt(State.LATEST_INDEX, state.current);
        editor.putInt(State.LATEST_LEVEL, state.level);
        editor.putInt(State.LEVELA_INDEX, state.lac);
        editor.putInt(State.LEVELA_LAST, state.lal);
        editor.putInt(State.LEVEL0_INDEX, state.l0c);
        editor.putInt(State.LEVEL0_LAST, state.l0l);
        editor.putInt(State.LEVEL1_INDEX, state.l1c);
        editor.putInt(State.LEVEL1_LAST, state.l1l);
        editor.putInt(State.LEVEL2_INDEX, state.l2c);
        editor.putInt(State.LEVEL2_LAST, state.l2l);
        editor.putInt(State.LEVEL3_INDEX, state.l3c);
        editor.putInt(State.LEVEL3_LAST, state.l3l);
        editor.putInt(State.LEVEL4_INDEX, state.l4c);
        editor.putInt(State.LEVEL4_LAST, state.l4l);
        editor.putInt(State.BONUS, state.bonus);
        editor.commit();
    }

    @Override
    public Object fromString(String str, Class<?> clazz) {
        return AUtils.fromString(str, clazz);
    }

    @Override
    public String toString(Object object) {
        return AUtils.toString(object);
    }

    @Override
    public void log(String msg, Throwable thrown) {
        AUtils.logMessage(mTC, msg + " " + thrown.getMessage());
        AIOUtils.log(msg, thrown);
    }

    @Override
    public void writeFile(String path, String str) {
        try {
            AIOUtils.writeFile(storagePath(path), str);
        } catch (IOException e) {
            log("", e);
        }
    }

    @Override
    public String readFile(String path) {
        try {
            return AIOUtils.readFile(storagePath(path));
        } catch (IOException e) {
            log("", e);
        }
        return null;
    }

    @Override
    public void downloadFile(String url, String savePath, DownloadEventListener del) {
        TaskUtils.downCancelableFile(mCtx, url, storagePath(savePath), del);
    }

    @Override
    public void unzipFile(String zipPath, String toDir, UnzipEventListener unzipEventListener) {
        TaskUtils.unzipFile(mCtx, storagePath(zipPath), storagePath(toDir), unzipEventListener);
    }

    @Override
    public File[] findMp3File(String path) {
        return IOUtils.findMp3Files(storagePath(path));
    }

    @Override
    public String relativePath(String fullPath, String base) {
        return IOUtils.relativePath(fullPath, storagePath(base));
    }

    @Override
    public boolean pathExists(String path) {
        String fullPath = storagePath(path);
        File file = new File(fullPath);
        return file.exists();
    }
}
