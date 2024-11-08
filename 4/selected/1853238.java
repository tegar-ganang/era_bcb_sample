package com.qarks.util.stream;

import java.io.*;

public class ResourceHelper {

    private static ResourceHelper instance = null;

    public static synchronized ResourceHelper getInstance() {
        if (instance == null) {
            instance = new ResourceHelper();
        }
        return instance;
    }

    public void extractResourceIfNotExisting(String resource, String destinationFile) {
        File file = new File(destinationFile);
        if (!file.exists() || file.length() == 0) {
            extractResource(resource, destinationFile);
        }
    }

    public void extractResource(String resource, String destinationFile) {
        File file = new File(destinationFile);
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(resource);
            byte array[] = new byte[2048];
            int nbread = 0;
            FileOutputStream fos = new FileOutputStream(file);
            while ((nbread = is.read(array)) > -1) {
                if (nbread > 0) {
                    fos.write(array, 0, nbread);
                } else {
                    Thread.sleep(50);
                }
            }
            fos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
