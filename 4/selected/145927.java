package com.croftsoft.core.util.cache;

import java.io.*;
import com.croftsoft.core.util.id.Id;
import com.croftsoft.core.util.id.IntId;

/*********************************************************************
     * Static methods to support Cache implementations.
     *
     * @see
     *   Cache
     *
     * @version
     *   1999-04-20
     * @author
     *   <a href="http://www.CroftSoft.com/">David Wallace Croft</a>
     *********************************************************************/
public class CacheLib {

    private CacheLib() {
    }

    public static Id storeString(Cache cache, String s) throws IOException {
        return cache.store(toInputStream(s));
    }

    public static String retrieveString(Cache cache, Id id) throws IOException {
        return toString(cache.retrieve(id));
    }

    public static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = null;
        try {
            out = new ByteArrayOutputStream();
            int i;
            while ((i = in.read()) > -1) out.write(i);
            return out.toByteArray();
        } finally {
            try {
                in.close();
            } catch (Exception ex) {
            }
            try {
                out.close();
            } catch (Exception ex) {
            }
        }
    }

    public static InputStream toInputStream(String s) {
        return toInputStream(s.getBytes());
    }

    public static InputStream toInputStream(byte[] byteArray) {
        return new ByteArrayInputStream(byteArray);
    }

    public static String toString(InputStream in) throws IOException {
        if (in == null) return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int i;
        while ((i = in.read()) > -1) out.write(i);
        return new String(out.toByteArray());
    }
}
