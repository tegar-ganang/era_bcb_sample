package com.ajoniec.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;
import javax.microedition.lcdui.*;

/**
 *
 * @author Adam Joniec
 */
public class Utils {

    public static final ByteArrayInputStream readISToBAIS(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte buff[] = new byte[1024];
        while (is.available() > 0) {
            int readed = is.read(buff, 0, 1024);
            baos.write(buff, 0, readed);
        }
        buff = null;
        return new ByteArrayInputStream(baos.toByteArray());
    }

    public static final int TEXT_ALIGN_LEFT = -1, TEXT_ALIGN_CENTER = 0, TEXT_ALIGN_RIGHT = 1;

    public static void drawString(String str, int offsetX, int y, int alignment, Graphics g, Font f) {
        g.setFont(f);
        int charsWidth = f.charsWidth(str.toCharArray(), 0, str.length());
        int width = g.getClipWidth();
        int x = g.getClipX();
        switch(alignment) {
            case -1:
                x += offsetX;
                break;
            case 0:
                x = (width - charsWidth) / 2;
                break;
            case 1:
                x = width - charsWidth - offsetX;
                break;
        }
        g.drawString(str, x, y, 0);
    }

    public static Vector copyOf(Vector vector) {
        Vector newVector = new Vector();
        for (int i = 0; i < vector.size(); ++i) {
            newVector.addElement(vector.elementAt(i));
        }
        return newVector;
    }

    public static Object[] asArray(Vector v) {
        Object[] ret = new Object[v.size()];
        for (int i = 0; i < v.size(); ++i) {
            ret[i] = v.elementAt(i);
        }
        return ret;
    }

    public static String shortTimeString(long timeMilis) {
        long timeInSec = timeMilis / 1000, hours = timeInSec / 3600, hoursSec = timeInSec % 3600, min = hoursSec / 60, sec = hoursSec % 60;
        String str = (min <= 9 ? "0" : "") + min + ":" + (sec <= 9 ? "0" : "") + sec;
        return str;
    }
}
