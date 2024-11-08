package org.gridbus.broker.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

/**
 * This class has utility methods for operations such as finding and replacing
 * specific strings / lines in a file.  
 * @author Srikumar Venugopal (srikumar@cs.mu.oz.au)
 * @version 1.0
 */
public class FileOpsUtil {

    /**
	 * Returns the complete line which contains the string 'param' in the file 'filename',
	 * or null if the param is not found in the file
	 * @param filename
	 * @param param
	 * @return string - line containing the param value in the file or null if the param is not found
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    public static String findParaminFile(String filename, String param) throws FileNotFoundException, IOException {
        String fileline;
        File fin = new File(filename);
        BufferedReader bfin = new BufferedReader(new InputStreamReader(new FileInputStream(fin)));
        while ((fileline = bfin.readLine()) != null) {
            if (fileline.startsWith("#")) continue;
            for (int ix = 0; ix < fileline.length() - param.length(); ix++) {
                if (fileline.regionMatches(ix, param, 0, param.length())) return fileline;
            }
        }
        bfin.close();
        return null;
    }

    /**
	 * @param filename
	 * @param content
	 * @param append
	 * @throws Exception
	 */
    public static void writeToFile(String filename, String content, boolean append) throws Exception {
        FileWriter fw = new FileWriter(filename, append);
        fw.write(content);
        fw.close();
    }

    /**
	 * Replaces the complete line 'oldline' in the 'oldfile' with the 'newline' in the 'newfile'
	 * or null if the param is not found in the file
	 * @param oldfile : file to search
	 * @param oldline : line to search for
	 * @param newfile : new file to be written out
	 * @param newline : new line which will replace the old line
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    public static void replaceParaminFile(String oldfile, String oldline, String newfile, String newline) throws FileNotFoundException, IOException {
        String fileline;
        File fin = new File(oldfile);
        File fout = new File(newfile);
        BufferedReader bfin = new BufferedReader(new InputStreamReader(new FileInputStream(fin)));
        BufferedWriter bfout = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fout)));
        while ((fileline = bfin.readLine()) != null) {
            fileline = replaceString(fileline, oldline, newline);
            bfout.write(fileline);
            bfout.write(System.getProperty("line.separator"));
        }
        bfout.close();
        bfin.close();
    }

    /**
	 * Replaces the string pattern in an input string with another string 
	 * @param str : string to search
	 * @param pattern : pattern to search for
	 * @param replace : string which will replace the pattern
	 * @return new string which has the pattern replaced
	 */
    public static String replaceString(String str, String pattern, String replace) {
        int s = 0;
        int e = 0;
        StringBuffer result = new StringBuffer();
        while ((e = str.indexOf(pattern, s)) >= 0) {
            result.append(str.substring(s, e));
            result.append(replace);
            s = e + pattern.length();
        }
        result.append(str.substring(s));
        return result.toString();
    }

    /**
     * @param source
     * @param destination
     * @param overwrite
     */
    public static void copyFile(String source, String destination, boolean overwrite) {
        File sourceFile = new File(source);
        try {
            File destinationFile = new File(destination);
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sourceFile));
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(destinationFile));
            int temp = 0;
            while ((temp = bis.read()) != -1) bos.write(temp);
            bis.close();
            bos.close();
        } catch (Exception e) {
        }
        return;
    }
}
