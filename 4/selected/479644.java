package com.isavvix.tools;

import java.io.*;
import java.util.*;
import javax.servlet.*;

/**
  * This class provides methods for parsing a HTML multi-part form.  Each
  * method returns a Hashtable which contains keys for all parameters sent
  * from the web browser.  The corresponding values are either type "String"
  * or "FileInfo" depending on the type of data in the corresponding part.
  * <P>
  * The following is a sample InputStream expected by the methods in this
  * class:<PRE>

    -----------------------------7ce23a18680
    Content-Disposition: form-data; name="SomeTextField1"

    on
    -----------------------------7ce23a18680
    Content-Disposition: form-data; name="LocalFile1"; filename="C:\temp\testit.c"
    Content-Type: text/plain

    #include <stdlib.h>


    int main(int argc, char **argv)
    {
       printf("Testing\n");
       return 0;
    }

    -----------------------------7ce23a18680--
    </PRE>
 * @see com.isavvix.tools.FileInfo
 * @author  Anil Hemrajani
*/
public class HttpMultiPartParser {

    private final String lineSeparator = System.getProperty("line.separator", "\n");

    private final int ONE_MB = 1024 * 1024 * 1;

    /** 
     * Parses the InputStream, separates the various parts and returns
     * them as key=value pairs in a Hashtable.  Any incoming files are
     * saved in directory "saveInDir" using the client's file name; the
     * file information is stored as java.io.File object in the Hashtable
     * ("value" part).
     */
    public Hashtable parseData(ServletInputStream data, String boundary, String saveInDir) throws IllegalArgumentException, IOException {
        return processData(data, boundary, saveInDir);
    }

    /** 
     * Parses the InputStream, separates the various parts and returns
     * them as key=value pairs in a Hashtable.  Any incoming files are
     * saved as byte arrays; the file information is stored as java.io.File
     * object in the Hashtable ("value" part).
     */
    public Hashtable parseData(ServletInputStream data, String boundary) throws IllegalArgumentException, IOException {
        return processData(data, boundary, null);
    }

    private Hashtable processData(ServletInputStream is, String boundary, String saveInDir) throws IllegalArgumentException, IOException {
        if (is == null) throw new IllegalArgumentException("InputStream");
        if (boundary == null || boundary.trim().length() < 1) throw new IllegalArgumentException("boundary");
        boundary = "--" + boundary;
        StringTokenizer stLine = null, stFields = null;
        FileInfo fileInfo = null;
        Hashtable dataTable = new Hashtable(5);
        String line = null, field = null, paramName = null;
        boolean saveFiles = (saveInDir != null && saveInDir.trim().length() > 0), isFile = false;
        if (saveFiles) {
            File f = new File(saveInDir);
            f.mkdirs();
        }
        line = getLine(is);
        if (line == null || !line.startsWith(boundary)) throw new IOException("Boundary not found;" + " boundary = " + boundary + ", line = " + line);
        while (line != null) {
            if (line == null || !line.startsWith(boundary)) return dataTable;
            line = getLine(is);
            if (line == null) return dataTable;
            stLine = new StringTokenizer(line, ";\r\n");
            if (stLine.countTokens() < 2) throw new IllegalArgumentException("Bad data in second line");
            line = stLine.nextToken().toLowerCase();
            if (line.indexOf("form-data") < 0) throw new IllegalArgumentException("Bad data in second line");
            stFields = new StringTokenizer(stLine.nextToken(), "=\"");
            if (stFields.countTokens() < 2) throw new IllegalArgumentException("Bad data in second line");
            fileInfo = new FileInfo();
            stFields.nextToken();
            paramName = stFields.nextToken();
            isFile = false;
            if (stLine.hasMoreTokens()) {
                field = stLine.nextToken();
                stFields = new StringTokenizer(field, "=\"");
                if (stFields.countTokens() > 1) {
                    if (stFields.nextToken().trim().equalsIgnoreCase("filename")) {
                        fileInfo.setName(paramName);
                        String value = stFields.nextToken();
                        if (value != null && value.trim().length() > 0) {
                            fileInfo.setClientFileName(value);
                            isFile = true;
                        } else {
                            line = getLine(is);
                            line = getLine(is);
                            line = getLine(is);
                            line = getLine(is);
                            continue;
                        }
                    }
                } else if (field.toLowerCase().indexOf("filename") >= 0) {
                    line = getLine(is);
                    line = getLine(is);
                    line = getLine(is);
                    line = getLine(is);
                    continue;
                }
            }
            boolean skipBlankLine = true;
            if (isFile) {
                line = getLine(is);
                if (line == null) return dataTable;
                if (line.trim().length() < 1) skipBlankLine = false; else {
                    stLine = new StringTokenizer(line, ": ");
                    if (stLine.countTokens() < 2) throw new IllegalArgumentException("Bad data in third line");
                    stLine.nextToken();
                    fileInfo.setFileContentType(stLine.nextToken());
                }
            }
            if (skipBlankLine) {
                line = getLine(is);
                if (line == null) return dataTable;
            }
            if (!isFile) {
                line = getLineISO(is);
                if (line == null) return dataTable;
                Object prev = dataTable.get(paramName);
                if (prev == null) {
                    dataTable.put(paramName, line);
                } else if (prev instanceof String) {
                    String[] curr = new String[2];
                    curr[0] = (String) prev;
                    curr[1] = line;
                    dataTable.put(paramName, curr);
                } else if (prev instanceof String[]) {
                    String[] aPrev = (String[]) prev;
                    String[] curr = new String[aPrev.length + 1];
                    for (int i = 0; i < aPrev.length; i++) curr[i] = aPrev[i];
                    curr[aPrev.length] = line;
                    dataTable.put(paramName, curr);
                }
                line = getLine(is);
                continue;
            }
            try {
                OutputStream os = null;
                String path = null;
                if (saveFiles) os = new FileOutputStream(path = getFileName(saveInDir, fileInfo.getClientFileName())); else os = new ByteArrayOutputStream(ONE_MB);
                boolean readingContent = true;
                byte previousLine[] = new byte[2 * ONE_MB];
                byte temp[] = null;
                byte currentLine[] = new byte[2 * ONE_MB];
                int read, read3;
                if ((read = is.readLine(previousLine, 0, previousLine.length)) == -1) {
                    line = null;
                    break;
                }
                while (readingContent) {
                    if ((read3 = is.readLine(currentLine, 0, currentLine.length)) == -1) {
                        line = null;
                        break;
                    }
                    if (compareBoundary(boundary, currentLine)) {
                        os.write(previousLine, 0, read);
                        os.flush();
                        line = new String(currentLine, 0, read3);
                        break;
                    } else {
                        os.write(previousLine, 0, read);
                        os.flush();
                        temp = currentLine;
                        currentLine = previousLine;
                        previousLine = temp;
                        read = read3;
                    }
                }
                os.close();
                temp = null;
                previousLine = null;
                currentLine = null;
                if (!saveFiles) {
                    ByteArrayOutputStream baos = (ByteArrayOutputStream) os;
                    fileInfo.setFileContents(baos.toByteArray());
                } else {
                    fileInfo.setLocalFile(new File(path));
                    os = null;
                }
                dataTable.put(paramName, fileInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return dataTable;
    }

    private boolean compareBoundary(String boundary, byte ba[]) {
        byte b;
        if (boundary == null || ba == null) return false;
        for (int i = 0; i < boundary.length(); i++) if ((byte) boundary.charAt(i) != ba[i]) return false;
        return true;
    }

    /** Convenience method to read HTTP header lines */
    private synchronized String getLine(ServletInputStream sis) throws IOException {
        byte b[] = new byte[1024];
        int read = sis.readLine(b, 0, b.length), index;
        String line = null;
        if (read != -1) {
            line = new String(b, 0, read);
            if ((index = line.indexOf('\n')) >= 0) line = line.substring(0, index - 1);
        }
        b = null;
        return line;
    }

    /** Convenience method to read HTTP header lines */
    private synchronized String getLineISO(ServletInputStream sis) throws IOException {
        byte b[] = new byte[1024];
        int read = sis.readLine(b, 0, b.length), index;
        String line = null;
        if (read != -1) {
            try {
                line = new String(b, 0, read, "UTF-8");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            ;
            if ((index = line.indexOf('\n')) >= 0) line = line.substring(0, index - 1);
        }
        b = null;
        return line;
    }

    /**
     * Concats the directory and file names.
     */
    private String getFileName(String dir, String fileName) throws IllegalArgumentException {
        String path = null;
        if (dir == null || fileName == null) throw new IllegalArgumentException("dir or fileName is null");
        int index = fileName.lastIndexOf('/');
        String name = null;
        if (index >= 0) name = fileName.substring(index + 1); else name = fileName;
        index = name.lastIndexOf('\\');
        if (index >= 0) fileName = name.substring(index + 1);
        path = dir + File.separator + fileName;
        if (File.separatorChar == '/') return path.replace('\\', File.separatorChar); else return path.replace('/', File.separatorChar);
    }
}
