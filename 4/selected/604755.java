package eyes.blue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class DownloadService extends IntentService {

    static String logTag = null;

    static final String funcInto = "Function Into";

    static final String funcLeave = "Function Leave";

    DownloadListener listener = null;

    DownloaderBinder downloaderBinder = new DownloaderBinder();

    boolean downloading = false;

    static ArrayList<RemoteSource> remoteResources = new ArrayList<RemoteSource>();

    Thread downloadThread = null;

    int downloadingIndex = -1;

    Object lockKey = new Object();

    public DownloadService() {
        super("DownloadService");
    }

    public void onCreate() {
        logTag = getString(R.string.app_name);
        if (remoteResources.size() == 0) {
            GoogleRemoteSource grs = new GoogleRemoteSource(this);
            remoteResources.add(grs);
            Log.d(logTag, "Add remote resource site: " + grs.getName() + ", there are " + remoteResources.size() + " site in list.");
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
    }

    public IBinder onBind(Intent intent) {
        return downloaderBinder;
    }

    public class DownloaderBinder extends Binder {

        DownloadService getService() {
            return DownloadService.this;
        }
    }

    public void setDownloadListener(DownloadListener dwListener) {
        this.listener = dwListener;
    }

    public void prepareFile(final int fileIndex) {
        Log.d(funcInto, "**** preparePlay ****");
        if (fileIndex == downloadingIndex) {
            Log.d(logTag, "The target index is the same with downloading one " + fileIndex + " skip the request.");
            return;
        }
        Log.d(logTag, "The target index(" + fileIndex + ") is difference with downloading index(" + downloadingIndex + ")");
        synchronized (lockKey) {
            downloadingIndex = fileIndex;
            lockKey.notify();
            lockKey = new Object();
        }
        downloadThread = new Thread("DownloadThread") {

            public void run() {
                Log.d(logTag, "The new request is (" + fileIndex + "), downloading index is (" + downloadingIndex + ").");
                if (!(FileSysManager.isFileValid(fileIndex, getResources().getInteger(R.integer.SUBTITLE_TYPE)))) {
                    Log.d(logTag, "The subtitle file index(" + fileIndex + ") is not exist, download now.");
                    downloadFileFromRemote(fileIndex, getResources().getInteger(R.integer.SUBTITLE_TYPE));
                }
                if (fileIndex != downloadingIndex) return;
                if (!(FileSysManager.isFileValid(fileIndex, getResources().getInteger(R.integer.MEDIA_TYPE)))) {
                    Log.d(logTag, "The media file index(" + fileIndex + ") is not exist, download now.");
                    downloadFileFromRemote(fileIndex, getResources().getInteger(R.integer.MEDIA_TYPE));
                }
                if (fileIndex != downloadingIndex) {
                    Log.d(logTag, Thread.currentThread().getName() + ": Download target has changed, skip download now!");
                    return;
                }
                Log.d(logTag, "Notify index(" + fileIndex + ") has prepared.");
                listener.prepareFinish(fileIndex);
                downloadingIndex = -1;
            }
        };
        downloadThread.start();
        Log.d(funcLeave, "**** preparePlay ****");
    }

    public void downloadFileFromRemote(int fileIndex, int resType) {
        Log.d(logTag, Thread.currentThread().getName() + ": Download file from remote ");
        listener.downloadPreExec(fileIndex, resType);
        final int MEDIA_FILE = getResources().getInteger(R.integer.MEDIA_TYPE);
        final int SUBTITLE_FILE = getResources().getInteger(R.integer.SUBTITLE_TYPE);
        final int THEORY_FILE = getResources().getInteger(R.integer.THEORY_TYPE);
        String resTypeMsg[] = new String[3];
        resTypeMsg[MEDIA_FILE] = "Media File";
        resTypeMsg[SUBTITLE_FILE] = "Subtitle File";
        resTypeMsg[THEORY_FILE] = "Theory File";
        long contentLength = -1;
        InputStream is = null;
        int targetSite = -1;
        File outputPath = null;
        File orgPath = null;
        Log.d(logTag, Thread.currentThread().getName() + ": Create " + remoteResources.size() + " thread for check remote file.");
        CheckRemoteThread[] crt = new CheckRemoteThread[remoteResources.size()];
        for (int i = 0; i < crt.length; i++) {
            if (fileIndex != downloadingIndex) {
                Log.d(logTag, Thread.currentThread().getName() + ": Download target has changed, skip download now!");
                return;
            }
            RemoteSource rs = remoteResources.get(i);
            String remotePath = null;
            if (resType == MEDIA_FILE) {
                remotePath = rs.getMediaFileAddress(fileIndex);
                orgPath = FileSysManager.getLocalMediaFile(fileIndex);
                outputPath = new File(orgPath.getParent() + File.separator + getString(R.string.DL_TMP_PREFIX) + orgPath.getName());
            } else if (resType == SUBTITLE_FILE) {
                remotePath = rs.getSubtitleFileAddress(fileIndex);
                orgPath = FileSysManager.getLocalSubtitleFile(fileIndex);
                outputPath = new File(orgPath.getParent() + File.separator + getString(R.string.DL_TMP_PREFIX) + orgPath.getName());
            } else if (resType == THEORY_FILE) {
                remotePath = rs.getTheoryFileAddress(fileIndex);
                orgPath = FileSysManager.getLocalTheoryFile(fileIndex);
                outputPath = new File(orgPath.getParent() + File.separator + getString(R.string.DL_TMP_PREFIX) + orgPath.getName());
            }
            crt[i] = new CheckRemoteThread(remotePath, lockKey, i, ("ChechThread" + i), getResources().getInteger(R.integer.DL_WAIT_TIME));
            Log.d(logTag, Thread.currentThread().getName() + ": Create Thread: " + ("ChechThread" + i) + " for check " + resTypeMsg[resType] + " from " + rs.getName() + " source, target: " + remotePath);
            crt[i].start();
        }
        if (fileIndex != downloadingIndex) {
            Log.d(logTag, Thread.currentThread().getName() + ": Download target has changed, skip download now!");
            return;
        }
        try {
            Log.d(logTag, "Wait for any check thread weak up main thread");
            for (int i = 0; i < remoteResources.size(); i++) {
                Log.d(logTag, Thread.currentThread().getName() + ": goto to sleep " + i);
                synchronized (lockKey) {
                    lockKey.wait(getResources().getInteger(R.integer.DL_WAIT_TIME));
                }
                Log.d(logTag, "Download Thread weak up at " + System.currentTimeMillis());
                if (fileIndex != downloadingIndex) {
                    Log.d(logTag, Thread.currentThread().getName() + ": Download target has changed, skip download now!");
                    return;
                }
                Log.d(logTag, Thread.currentThread().getName() + ": waked up");
                for (CheckRemoteThread t : crt) {
                    if (t.getResponseCode() == 200) {
                        is = t.getInputStream();
                        targetSite = t.getFromSite();
                        contentLength = t.getContentLength();
                        break;
                    }
                    if (is != null) break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (fileIndex != downloadingIndex) {
            Log.d(logTag, Thread.currentThread().getName() + ": Download target has changed, skip download now!");
            return;
        }
        if (is == null) {
            Log.d(logTag, Thread.currentThread().getName() + ": There is no valid remote site for download this file");
            listener.downloadFail(fileIndex, resType);
            return;
        }
        Log.d(logTag, Thread.currentThread().getName() + ": Download from " + remoteResources.get(targetSite).getName() + ", Length: " + contentLength);
        Log.d(logTag, Thread.currentThread().getName() + ": Download Thread started");
        long startTime = System.currentTimeMillis();
        int readLen = -1;
        int counter = 0;
        int bufLen = getResources().getInteger(R.integer.downloadBufferSize);
        Checksum checksum = new CRC32();
        FileOutputStream fos = null;
        listener.setMaxProgress(contentLength);
        try {
            fos = new FileOutputStream(outputPath);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        try {
            byte[] buf = new byte[bufLen];
            Log.d(logTag, Thread.currentThread().getName() + ": Start read stream from remote site, is=" + ((is == null) ? "NULL" : "exist") + ", buf=" + ((buf == null) ? "NULL" : "exist"));
            while ((readLen = is.read(buf)) != -1) {
                if (fileIndex != downloadingIndex) {
                    Log.d(logTag, Thread.currentThread().getName() + ": Download target has changed, skip download now!");
                    is.close();
                    fos.close();
                    Log.d(logTag, Thread.currentThread().getName() + ": I/O Stream has closed, call stopDownload().");
                    return;
                }
                counter += readLen;
                fos.write(buf, 0, readLen);
                checksum.update(buf, 0, readLen);
                listener.setDownloadProgress(counter);
            }
            is.close();
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(logTag, Thread.currentThread().getName() + ": IOException happen while download media.");
            listener.downloadFail(fileIndex, resType);
            return;
        }
        if (fileIndex != downloadingIndex) return;
        if (MEDIA_FILE == resType) {
            Log.d(logTag, Thread.currentThread().getName() + ": Download finish, notify downloadFinishListener");
            long sum = checksum.getValue();
            boolean isCorrect = (FileSysManager.mp3FileCRC32[fileIndex] == sum);
            int spend = (int) (System.currentTimeMillis() - startTime);
            Log.d(logTag, Thread.currentThread().getName() + ": File index: " + fileIndex + " Read length: " + counter + ", CRC32 check: " + ((isCorrect) ? " Correct!" : " Error!" + " (" + sum + "/" + FileSysManager.mp3FileCRC32[fileIndex]) + "), spend time: " + spend + "ms");
            if (!isCorrect) {
                if (fileIndex != downloadingIndex) return;
                listener.downloadFail(fileIndex, resType);
                return;
            }
        }
        if (fileIndex != downloadingIndex) return;
        outputPath.renameTo(orgPath);
        listener.downloadFinish(fileIndex, resType);
        return;
    }

    @Override
    public void onDestroy() {
        stopSelf();
    }
}
