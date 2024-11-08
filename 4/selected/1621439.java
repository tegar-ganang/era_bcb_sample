package org.leo.traceroute.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Util $Id: Util.java 22 2011-05-28 11:32:56Z leolewis $
 * 
 * @author Leo Lewis
 */
public class Util {

    /** File separator */
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

    /**
	 * Copy a file
	 * 
	 * @param src source file
	 * @param dst destination file
	 * @return flag if the copy succeeded
	 */
    public static boolean copy(File src, File dst) {
        FileInputStream in = null;
        boolean ok = true;
        try {
            in = new FileInputStream(src);
            ok = ok && copy(in, dst);
        } catch (IOException e) {
            e.printStackTrace();
            ok = false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                ok = false;
            }
        }
        return ok;
    }

    /**
	 * Copy a file
	 * 
	 * @param src source stream
	 * @param dst destination file
	 * @return flag if the copy succeeded
	 */
    public static boolean copy(InputStream src, File dst) {
        FileOutputStream out = null;
        boolean ok = true;
        try {
            out = new FileOutputStream(dst);
            byte[] buffer = new byte[256];
            int read = src.read(buffer);
            while (read > 0) {
                out.write(buffer, 0, read);
                read = src.read(buffer);
            }
        } catch (FileNotFoundException e) {
            ok = false;
            e.printStackTrace();
        } catch (IOException e) {
            ok = false;
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                ok = false;
            }
        }
        return ok;
    }

    /**
	 * Read a stream encoded in UTF-8 and return the content in a List of String
	 * 
	 * @param stream stream
	 * @return list of String
	 */
    public static List<String> readUTF8File(InputStream stream) {
        List<String> list = new ArrayList<String>();
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            isr = new InputStreamReader(stream, "UTF-8");
            br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                list.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (isr != null) {
                    isr.close();
                }
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return list;
    }
}
