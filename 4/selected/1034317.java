package edu.nctu.csie.jichang.database.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.io.StringWriter;
import org.apache.commons.io.FileUtils;

public class FileUtil {

    public static void writeStringToFile(String pData, File pFile) throws IOException {
        FileUtils.writeStringToFile(pFile, pData, "UTF-8");
    }

    public static String readFileToString(File pSrcFile) throws IOException {
        BufferedReader tReader = null;
        StringWriter tWriter = null;
        UnicodeReader tUnicodeReader = new UnicodeReader(new FileInputStream(pSrcFile), null);
        char[] tBuffer = new char[16 * 1024];
        int read;
        try {
            tReader = new BufferedReader(tUnicodeReader);
            tWriter = new StringWriter();
            while ((read = tReader.read(tBuffer)) != -1) {
                tWriter.write(tBuffer, 0, read);
            }
            tWriter.flush();
            return tWriter.toString();
        } catch (IOException ex) {
            throw ex;
        } finally {
            try {
                tWriter.close();
                tReader.close();
                tUnicodeReader.close();
            } catch (Exception ex) {
            }
        }
    }
}

class UnicodeReader extends Reader {

    PushbackInputStream internalIn;

    InputStreamReader internalIn2 = null;

    String defaultEnc;

    private static final int BOM_SIZE = 4;

    UnicodeReader(InputStream in, String defaultEnc) {
        internalIn = new PushbackInputStream(in, BOM_SIZE);
        this.defaultEnc = defaultEnc;
    }

    public String getDefaultEncoding() {
        return defaultEnc;
    }

    public String getEncoding() {
        if (internalIn2 == null) return null;
        return internalIn2.getEncoding();
    }

    protected void init() throws IOException {
        if (internalIn2 != null) return;
        String encoding;
        byte bom[] = new byte[BOM_SIZE];
        int n, unread;
        n = internalIn.read(bom, 0, bom.length);
        if ((bom[0] == (byte) 0x00) && (bom[1] == (byte) 0x00) && (bom[2] == (byte) 0xFE) && (bom[3] == (byte) 0xFF)) {
            encoding = "UTF-32BE";
            unread = n - 4;
        } else if ((bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE) && (bom[2] == (byte) 0x00) && (bom[3] == (byte) 0x00)) {
            encoding = "UTF-32LE";
            unread = n - 4;
        } else if ((bom[0] == (byte) 0xEF) && (bom[1] == (byte) 0xBB) && (bom[2] == (byte) 0xBF)) {
            encoding = "UTF-8";
            unread = n - 3;
        } else if ((bom[0] == (byte) 0xFE) && (bom[1] == (byte) 0xFF)) {
            encoding = "UTF-16BE";
            unread = n - 2;
        } else if ((bom[0] == (byte) 0xFF) && (bom[1] == (byte) 0xFE)) {
            encoding = "UTF-16LE";
            unread = n - 2;
        } else {
            encoding = defaultEnc;
            unread = n;
        }
        if (unread > 0) internalIn.unread(bom, (n - unread), unread);
        if (encoding == null) {
            internalIn2 = new InputStreamReader(internalIn);
        } else {
            internalIn2 = new InputStreamReader(internalIn, encoding);
        }
    }

    public void close() throws IOException {
        init();
        internalIn2.close();
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
        init();
        return internalIn2.read(cbuf, off, len);
    }
}
