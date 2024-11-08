package com.qarks.util.stream;

import java.net.URL;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLConnection;
import java.io.IOException;

public class CommunicationUtilities {

    private CommunicationUtilities() {
    }

    public static byte[] readBytesContent(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte data[] = new byte[2048];
        int nbread = 0;
        while ((nbread = is.read(data)) > -1) {
            if (nbread > 0) {
                baos.write(data, 0, nbread);
            } else {
                try {
                    Thread.sleep(50);
                } catch (Exception ex) {
                    throw new IOException(ex.getMessage());
                }
            }
        }
        return baos.toByteArray();
    }

    public static String readContent(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte data[] = new byte[2048];
        int nbread = 0;
        while ((nbread = is.read(data)) > 0) {
            baos.write(data, 0, nbread);
        }
        return baos.toString();
    }

    public static URLConnection connectToServer(URL serverURL) throws IOException {
        return connectToServer(serverURL, true, true);
    }

    public static URLConnection connectToServer(URL serverURL, boolean doInput, boolean doOutput) throws IOException {
        URLConnection connection = null;
        connection = serverURL.openConnection();
        connection.setDoInput(doInput);
        connection.setDoOutput(doOutput);
        connection.setUseCaches(false);
        connection.setDefaultUseCaches(false);
        connection.setRequestProperty("Content-Type", "text");
        return connection;
    }
}
