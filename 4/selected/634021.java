package it.unibo.mortemale.cracker.john.downloader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import alice.logictuple.InvalidTupleOperationException;
import it.unibo.mortemale.tuples.Dictionary;

public class HttpDownloader implements IDictionaryDownloader {

    public static int buffer_size = (int) Math.pow(2, 15);

    public static HttpDownloader get_HttpDownloader() {
        try {
            return new HttpDownloader(File.createTempFile("mmhttpdl_", ".temp").getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private HttpDownloader(String local_cache) {
        this.local_cache = new File(local_cache);
        abort = false;
    }

    File local_cache;

    boolean abort;

    @Override
    public synchronized File download_dictionary(Dictionary dict, String localfpath) {
        abort = false;
        try {
            URL dictionary_location = new URL(dict.getLocation());
            InputStream in = dictionary_location.openStream();
            FileOutputStream w = new FileOutputStream(local_cache, false);
            int b = 0;
            while ((b = in.read()) != -1) {
                w.write(b);
                if (abort) throw new Exception("Download Aborted");
            }
            in.close();
            w.close();
            File lf = new File(localfpath);
            FileInputStream r = new FileInputStream(local_cache);
            FileOutputStream fw = new FileOutputStream(lf);
            int c;
            while ((c = r.read()) != -1) fw.write(c);
            r.close();
            fw.close();
            clearCache();
            return lf;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidTupleOperationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        clearCache();
        return null;
    }

    public void abort_download() {
        abort = true;
    }

    public synchronized void clearCache() {
        local_cache.delete();
    }
}
