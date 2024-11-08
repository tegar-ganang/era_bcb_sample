package org.jtomtom.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import javax.xml.ws.http.HTTPException;
import org.apache.log4j.Logger;
import org.jtomtom.Application;
import org.jtomtom.JTomtomException;

public final class JTomTomUtils {

    public static final Logger LOGGER = Logger.getLogger(JTomTomUtils.class);

    /**
	 * Copy a file inside an other even if they are on different physical disc
	 * @param source		Source file
	 * @param target		Target file
	 * @param overwrite		True if you want overwrite existing target file
	 * @return				True if the copy successfully finish
	 */
    public static final boolean copy(File source, File target, boolean overwrite) {
        if (!overwrite && target.exists()) {
            LOGGER.error("Target file exist and it not permitted to overwrite it !");
            return false;
        }
        FileChannel in = null;
        FileChannel out = null;
        try {
            in = new FileInputStream(source).getChannel();
            out = new FileOutputStream(target).getChannel();
            in.transferTo(0, in.size(), out);
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getLocalizedMessage());
            if (LOGGER.isDebugEnabled()) e.printStackTrace();
            return false;
        } catch (IOException e) {
            LOGGER.error(e.getLocalizedMessage());
            if (LOGGER.isDebugEnabled()) e.printStackTrace();
            return false;
        } finally {
            try {
                in.close();
            } catch (Exception e) {
            }
            try {
                out.close();
            } catch (Exception e) {
            }
        }
        return true;
    }

    /**
	 * Copy a file inside an other even if they are on different physical disc
	 * @param source		Source file
	 * @param target		Tagret file
	 * @return				True if the copy successfully finish
	 */
    public static final boolean copy(File source, File target) {
        return copy(source, target, false);
    }

    /**
	 * Move a file inside an other even if they are on different physical disc
	 * @param source		Source file
	 * @param target		Tagret file
	 * @param overwrite		True if you want overwrite existing target file
	 * @return				True if the move successfully finish
	 */
    public static final boolean move(File source, File target, boolean overwrite) {
        boolean result = copy(source, target, overwrite);
        result &= source.delete();
        return result;
    }

    /**
	 * Move a file inside an other even if they are on different physical disc
	 * @param source		Source file
	 * @param target		Tagret file
	 * @return				True if the move successfully finish
	 */
    public static final boolean move(File source, File target) {
        return move(source, target, false);
    }

    public static final Map<String, String> createDeviceMap() {
        final String TOMTOM_DEVICE_LIST_URL = "http://www.tomtom.com/lib/img/cs/javascript/reset.js";
        final String DEVICESN_START = "devicesn=new Array(";
        final String DEVICENAME_START = "devicenames=new Array(";
        final String DEVICE_ARRAY_END = ");";
        try {
            String[] deviceSN = null;
            String[] deviceNames = null;
            Map<String, String> deviceMap = new HashMap<String, String>();
            URL resetJsUrl = new URL(TOMTOM_DEVICE_LIST_URL);
            HttpURLConnection conn = (HttpURLConnection) resetJsUrl.openConnection(Application.getInstance().getProxyServer());
            int response = conn.getResponseCode();
            if (response != HttpURLConnection.HTTP_OK) {
                throw new HTTPException(response);
            }
            InputStream is = conn.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = rd.readLine().trim()) != null) {
                if (line.startsWith(DEVICESN_START)) {
                    int start = line.indexOf(DEVICESN_START) + DEVICESN_START.length();
                    int end = line.indexOf(DEVICE_ARRAY_END, start);
                    deviceSN = line.substring(start, end).replaceAll("\"", "").split(",");
                }
                if (line.startsWith(DEVICENAME_START)) {
                    int start = line.indexOf(DEVICENAME_START) + DEVICENAME_START.length();
                    int end = line.indexOf(DEVICE_ARRAY_END, start);
                    deviceNames = line.substring(start, end).replaceAll("\"", "").split(",");
                }
                if (deviceSN != null && deviceNames != null) break;
            }
            for (int i = 0; i < deviceSN.length; i++) {
                deviceMap.put(deviceSN[i], deviceNames[i]);
            }
            return deviceMap;
        } catch (Exception e) {
            throw new JTomtomException("Unable to create device map !", e);
        }
    }
}
