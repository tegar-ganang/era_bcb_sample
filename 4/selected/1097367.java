package Pump;

import java.util.*;
import java.io.*;
import java.nio.channels.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.logging.*;

public class PumpCollector {

    private static Logger myLogger = Logger.getLogger("Pump.PumpCollector");

    private static FileHandler myHandler;

    private FileInputStream myInStream;

    private Vector urls;

    private Vector imgs;

    private Vector raw_tags;

    /***************************************************************************
     * CTor for this class
     */
    public PumpCollector(FileInputStream in, Vector u_list, Vector img_list) {
        try {
            myHandler = new FileHandler("log.log");
        } catch (IOException e) {
            myLogger.log(Level.SEVERE, "Error in PumpCollector!");
        }
        myLogger.addHandler(myHandler);
        myInStream = in;
        urls = u_list;
        imgs = img_list;
        raw_tags = new Vector();
        myLogger.setLevel(Level.ALL);
        myLogger.fine("Pumpcollector: ctor done.");
    }

    /***************************************************************************
     * this method starts to collect the stuff
     */
    public boolean collect() {
        boolean result = false;
        result = this.checkReadiness();
        if (result) {
            long numOfByte = 0;
            FileChannel inCh = myInStream.getChannel();
            int fileSize = 0;
            try {
                fileSize = (int) inCh.size();
            } catch (IOException e) {
                myLogger.log(Level.FINER, "File size exc");
            }
            if (fileSize == 0) return false;
            ByteBuffer buffer = ByteBuffer.allocate(fileSize);
            try {
                numOfByte = inCh.read(buffer);
                buffer.flip();
            } catch (IOException e) {
                myLogger.log(Level.FINER, "Exception when reading from buffer :" + e.getMessage());
            }
            buffer.position(0);
            myLogger.log(Level.FINEST, "size >>" + numOfByte + " >> " + fileSize);
            StringBuffer k;
            try {
                k = new StringBuffer();
            } catch (NegativeArraySizeException e) {
                myLogger.log(Level.FINER, "StringBuffer allocation error. Exception: " + e.getMessage());
                return false;
            }
            char kl;
            boolean put = false;
            String tag = new String("");
            while (buffer.remaining() > 0) {
                kl = (char) buffer.get();
                if (kl == '<') put = true;
                if (put) k.append(kl);
                if (kl == '>') {
                    put = false;
                    raw_tags.add(k.toString());
                    clearBuffer(k);
                }
            }
            if (put) raw_tags.add(k.toString());
            if (!raw_tags.isEmpty()) {
                collect_result();
            }
        } else {
            return result;
        }
        myLogger.log(Level.FINEST, "u list size:" + urls.size() + "; img list size: " + imgs.size());
        return result;
    }

    /***************************************************************************
     *This method collects the urls and img url from raw result into url and img list
     */
    protected void collect_result() {
        String temp = new String(), tempLower = new String(), urlString = new String();
        boolean isUrl, isImg = false;
        int index, start_pos, end_pos = 0;
        while (!raw_tags.isEmpty()) {
            temp = (String) raw_tags.remove(0);
            tempLower = temp.toLowerCase();
            if ((tempLower.lastIndexOf("href=")) >= 0) {
                isUrl = true;
                isImg = false;
                urlString = get_url(temp);
                if (urlString.length() > 0) {
                    urls.add(urlString);
                }
            }
            if (tempLower.lastIndexOf("src=") >= 0) {
                isImg = true;
                isUrl = false;
                urlString = get_url(temp);
                if (urlString.length() > 0) {
                    imgs.add(urlString);
                }
            }
        }
    }

    /***************************************************************************
     * this method reads url from the whole href or from img tag
     */
    protected String get_url(String from) {
        int start_pos, end_pos = 0;
        String ret = new String("");
        start_pos = from.indexOf('"');
        end_pos = from.indexOf('"', (start_pos + 1));
        if (end_pos <= start_pos) {
        } else {
            ret = from.substring(start_pos + 1, end_pos);
        }
        return ret;
    }

    /***************************************************************************
     * this method clears buffer
     */
    protected void clearBuffer(StringBuffer what) {
        what = what.delete(0, what.length());
    }

    /***************************************************************************
     * This method checks whether the instream ready to read or not
     */
    protected boolean checkReadiness() {
        boolean r = false;
        try {
            r = myInStream.getFD().valid();
        } catch (IOException e) {
            r = false;
        }
        return r;
    }
}
