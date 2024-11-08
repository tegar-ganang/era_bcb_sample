package com.ufnasoft.dms.gui;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import sun.misc.BASE64Decoder;

public class InitDMS {

    int parentProjectId = 0;

    public String FS = System.getProperty("file.separator");

    public String dms_home = "";

    public String dms_url = "";

    public String usernamefullname = "";

    public String username = "";

    public String key = "";

    public String isDebug = "yes";

    public InitDMS() {
        if (dms_url == "" || dms_home == "") {
            dmsSetUp();
        }
    }

    public void dmsSetUp() {
        try {
            ResourceBundle myResources = ResourceBundle.getBundle("resources.usdmsclient");
            dms_home = myResources.getString("dms_home");
            dms_url = myResources.getString("dms_url");
            if (dms_home == null) log("dms_home is not provided.");
            File f = new File(dms_home);
            if (!f.exists()) {
                log("\ndms_home: " + dms_home + " : folder does not exist on the file system.");
            }
            if (key == "") {
                ReadingUserLoginFile rulf = new ReadingUserLoginFile();
                rulf.getUserLoginInformation(dms_home);
                key = rulf.getKey();
                username = rulf.getUsername();
                usernamefullname = rulf.getUsernamefullname();
            }
        } catch (MissingResourceException e) {
            log("MissingResourceException:" + e.getMessage());
        }
    }

    public boolean downloadFile(String serverFile, String clientFile) {
        boolean rvalue = false;
        try {
            String urlString = dms_url + "/datafiles/" + serverFile;
            URL u = new URL(urlString);
            DataInputStream is = new DataInputStream(u.openStream());
            System.out.println(urlString);
            FileOutputStream os = new FileOutputStream(clientFile);
            int iBufSize = is.available();
            byte inBuf[] = new byte[20000 * 1024];
            int iNumRead;
            while ((iNumRead = is.read(inBuf, 0, iBufSize)) > 0) os.write(inBuf, 0, iNumRead);
            os.close();
            is.close();
            rvalue = true;
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return rvalue;
    }

    public void log(String s1, String s2) {
        System.out.println("dms:" + s1 + ":" + s2);
    }

    public void log(String s) {
        System.out.println("dms:" + s);
    }
}
