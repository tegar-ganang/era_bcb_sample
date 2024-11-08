package org.kablink.teaming.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import org.mozilla.universalchardet.UniversalDetector;

public class FileCharsetDetectorUtil {

    public static String charDetect(File file) throws java.io.IOException {
        byte[] buf = new byte[4096];
        java.io.FileInputStream fis = new java.io.FileInputStream(file);
        try {
            UniversalDetector detector = new UniversalDetector(null);
            int nread;
            while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
                detector.handleData(buf, 0, nread);
            }
            detector.dataEnd();
            String encoding = detector.getDetectedCharset();
            detector.reset();
            return encoding;
        } finally {
            fis.close();
        }
    }

    public static void convertEncoding(File infile, File outfile, String from, String to) throws IOException, UnsupportedEncodingException {
        InputStream in;
        if (infile != null) in = new FileInputStream(infile); else in = System.in;
        OutputStream out;
        outfile.createNewFile();
        if (outfile != null) out = new FileOutputStream(outfile); else out = System.out;
        if (from == null) from = System.getProperty("file.encoding");
        if (to == null) to = "Unicode";
        Reader r = new BufferedReader(new InputStreamReader(in, from));
        Writer w = new BufferedWriter(new OutputStreamWriter(out, to));
        char[] buffer = new char[4096];
        int len;
        while ((len = r.read(buffer)) != -1) w.write(buffer, 0, len);
        r.close();
        w.close();
    }
}
