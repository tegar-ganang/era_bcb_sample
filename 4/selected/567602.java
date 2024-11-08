package offlinefunction.listenmusic;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.net.URLConnection;
import android.util.Log;
import offlinefunction.generalobject.StaticObject;

public class ListenOnlineController implements Runnable {

    @Override
    public void run() {
        try {
            downloadAudioIncrement(currentfiledownloading);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    File downloadingMediaFile;

    String currentfiledownloading = StaticObject.currentSong.getResource();

    private void downloadAudioIncrement(String mediaUrl) throws IOException {
        if (downloadingMediaFile != null) downloadingMediaFile.delete();
        URLConnection cn = new URL(mediaUrl).openConnection();
        cn.connect();
        InputStream stream = cn.getInputStream();
        if (stream == null) {
            Log.e(getClass().getName(), "Unable to create InputStream for mediaUrl:" + mediaUrl);
        }
        downloadingMediaFile = new File("/sdcard/downloadingMedia_tempmusic" + ".dat");
        RandomAccessFile ras = new RandomAccessFile(downloadingMediaFile, "rw");
        byte buf[] = new byte[16384];
        int totalBytesRead = 0, incrementalBytesRead = 0;
        do {
            int numread = stream.read(buf);
            if (numread <= 0) break;
            ras.write(buf, 0, numread);
            totalBytesRead += numread;
            incrementalBytesRead += numread;
        } while (validate());
        ras.close();
        stream.close();
    }

    private boolean validate() {
        return currentfiledownloading.equals(StaticObject.currentSong.getResource());
    }
}
