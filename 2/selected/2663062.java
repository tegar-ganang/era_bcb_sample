package com.foobnix.util;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import com.foobnix.model.FModel;
import com.foobnix.model.FModel.TYPE;

/**
 *
 */
public class SongUtil {

    private static final int KB = 1024;

    private static final int MB = KB * 1024;

    private static final int GB = MB * 1024;

    public static String capitilizeWords(String text) {
        String[] split = text.split(" ");
        StringBuilder result = new StringBuilder();
        for (String str : split) {
            String word = str.trim();
            if (StringUtils.isNotEmpty(word)) {
                String first = ("" + word.charAt(0)).toUpperCase();
                if (word.length() > 1) {
                    first = first + word.substring(1);
                }
                result.append(first);
                result.append(" ");
            }
        }
        return result.toString().trim();
    }

    /**
     * Get remove url size in bytes
     * 
     * @param urlPath
     * @return
     */
    public static int getRemoteSize(String urlPath) {
        URL url;
        int len = 0;
        try {
            url = new URL(urlPath);
            URLConnection con = url.openConnection();
            len = con.getContentLength();
            con.getInputStream().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return len;
    }

    public static String getNumWithZero(int num) {
        if (num < 10) {
            return "0" + num;
        }
        return "" + num;
    }

    public static double getMB(double bytes) {
        return (double) bytes / MB;
    }

    public static double getGB(double bytes) {
        return (double) bytes / GB;
    }

    public static void removeFolders(List<FModel> list) {
        Iterator<FModel> iterator = list.iterator();
        while (iterator.hasNext()) {
            FModel next = iterator.next();
            if (next.isFolder()) {
                iterator.remove();
            }
        }
    }

    public static List<FModel> getListWithouFolders(List<FModel> list) {
        List<FModel> result = new ArrayList<FModel>();
        for (FModel line : list) {
            if (line.isFile()) {
                result.add(line);
            }
        }
        return result;
    }

    public static boolean isFileInList(List<FModel> list) {
        if (CollectionUtils.isEmpty(list)) {
            return false;
        }
        for (FModel model : list) {
            if (model.isFile()) {
                return true;
            }
        }
        return false;
    }

    public static String getArtist(String text) {
        int index = text.indexOf("-");
        if (index > 1) {
            return text.substring(0, index).trim();
        }
        return "Undefined";
    }

    public static String getTitle(String text) {
        int index = text.indexOf("-");
        if (index > 0) {
            return text.substring(index + 1, text.length()).trim();
        }
        return text;
    }

    public static boolean isRemote(String path) {
        if (StringUtils.isEmpty(path)) {
            return true;
        }
        if (path.startsWith("http")) {
            return true;
        }
        return false;
    }

    public static void updateArtist(List<FModel> models, String artist) {
        for (FModel model : models) {
            model.setArtist(artist);
        }
    }

    public static void updateType(List<FModel> models, TYPE type) {
        for (FModel model : models) {
            model.setType(type);
        }
    }

    public static void updatePositions(List<FModel> models) {
        int pos = 0;
        for (FModel model : models) {
            model.setPosition(pos);
            pos++;
        }
    }
}
