package com.cirnoworks.spk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.concurrent.ConcurrentHashMap;
import com.cirnoworks.urlresource.GenericURLStreamHandlerFactory;

/**
 * spk:{apk url}|{seed}!{entry}
 * 
 * @author Cloudee
 * 
 */
public class Handler extends URLStreamHandler {

    private ConcurrentHashMap<String, SPKReader> readers = new ConcurrentHashMap<String, SPKReader>();

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        return new SPKURLConnection(u);
    }

    class SPKURLConnection extends URLConnection {

        private URL spkFileURL;

        private String entryName;

        private SPKReader reader;

        private long seed;

        private boolean connected = false;

        /**
		 * @param url
		 * @throws MalformedURLException
		 */
        protected SPKURLConnection(URL url) throws MalformedURLException {
            super(url);
            String spec = url.getFile();
            int separator = spec.indexOf("!/");
            if (separator == -1) {
                throw new MalformedURLException("no !/ found in url spec:" + spec);
            }
            String spk = spec.substring(0, separator++);
            int sep = spk.indexOf("|");
            if (sep < 0) {
                seed = 0;
            } else {
                if (sep == 0 || sep == spk.length() - 1) {
                    throw new MalformedURLException(spk);
                }
                try {
                    seed = Long.parseLong(spk.substring(sep + 1));
                } catch (NumberFormatException e) {
                    throw new MalformedURLException("Illegal seed:" + spk.substring(sep + 1));
                }
                spk = spk.substring(0, sep);
            }
            spkFileURL = new URL(spk);
            entryName = null;
            if (++separator != spec.length()) {
                entryName = spec.substring(separator, spec.length());
                if (!entryName.startsWith("/")) {
                    entryName = "/" + entryName;
                }
            }
        }

        @Override
        public synchronized void connect() throws IOException {
            String key = spkFileURL + "|" + seed;
            reader = readers.get(key);
            if (reader == null) {
                reader = new SPKReader(spkFileURL, seed);
                readers.put(key, reader);
            }
            connected = true;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            try {
                synchronized (this) {
                    if (!connected) {
                        connect();
                    }
                }
            } catch (IOException e) {
                return null;
            }
            return reader == null ? null : reader.getDecStream(entryName);
        }
    }

    public static void main(String[] args) {
        GenericURLStreamHandlerFactory.getInstance().register("spk", new SPKHandlerFactory());
        try {
            InputStream is = new URL("spk:" + (new File("out.spk")).toURI().toASCIIString() + "|1A21A0BE14!/src/main/java/util/Random.java").openStream();
            int read;
            while ((read = is.read()) >= 0) {
                System.out.write(read);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
