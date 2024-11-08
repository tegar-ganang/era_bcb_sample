package ch.dvbern.lib.jampp.multipart;

import java.io.*;
import java.util.*;

/**
 * The main-parser class. Parses one level of multipart content and ueses itself for a recursive parsing.
 *
 *@author   $Author: dmilic $
 *@version  $Revision: 1.1.1.1 $
 */
public class MultipartParser {

    private static final int PARAMETER_BUFFER_LENGTH = 8 * 1024;

    private final byte[] parameterBuffer = new byte[PARAMETER_BUFFER_LENGTH];

    private final byte[] lineBuffer = new byte[1024];

    private final byte[] boundaryBytes;

    private final LineInput lineInput;

    private final String boundary;

    private boolean eof;

    private final File temporaryDirectory;

    private static final String MULTIPART_MIME_TYPE = "multipart/form-data";

    private static final String CONTENT_DISPOSITION = "Content-Disposition";

    private static final String BOUNDARY = "boundary=";

    /**
     * Constructs a new upload parser using the lineInput, boundary nad temporaryDirectory for the file storage.
     *
     *@param lineInput                              LineInput for the parsing.
     *@param boundary                               String representing the boundary for the multipart.
     *@param temporaryDirectory                     Directory where temportary files are going to be stored.
     *@throws java.lang.IllegalArgumentException    Is thrown if some of parameter are <code>null</code> or boundary length is 0.
     */
    public MultipartParser(LineInput lineInput, String boundary, File temporaryDirectory) {
        if (lineInput == null) throw new IllegalArgumentException("lineInput must not be null");
        this.lineInput = lineInput;
        if (temporaryDirectory == null) throw new IllegalArgumentException("temporaryDirectory must not be null");
        this.temporaryDirectory = temporaryDirectory;
        if (boundary == null) throw new IllegalArgumentException("boundary must not be null");
        if (boundary.length() == 0) throw new IllegalArgumentException("boundary length should be >=1");
        this.boundary = boundary;
        try {
            this.boundaryBytes = boundary.getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException usec) {
            throw new RuntimeException("this should never happend!");
        }
    }

    /**
     * This method parses the input and returns the <code>java.lang.List</code>
     *  containing all <code>ch.dvbern.lib.jampp.multipart.Parametter</code> instances that were parsed from the input.
     *
     *@return    List containing the parsed parameter.
     *@exception IOException  Thrown if there were problems reading <b>and parsing</b> the input stream.
     */
    public List parse() throws IOException {
        String startBoundary = readLine();
        if (!boundary.equals(startBoundary)) throw new IOException("starting boundary not found");
        List ret = new ArrayList();
        while (!eof) {
            Parameter p = readNextParameter();
            ret.add(p);
        }
        return ret;
    }

    private Parameter readNextParameter() throws IOException {
        Map headers = readHeaders();
        ParameterInformation dinfo = parseParameterInfo(headers);
        String mt = dinfo.getMimeType();
        if (dinfo.getFileName() != null) {
            File tmp = File.createTempFile("upload", "tmp", temporaryDirectory);
            tmp.deleteOnExit();
            try {
                FileOutputStream fos = new FileOutputStream(tmp);
                try {
                    BufferedOutputStream bos = new BufferedOutputStream(fos, 4096);
                    try {
                        readData(bos);
                    } finally {
                        bos.close();
                    }
                } finally {
                    fos.close();
                }
            } catch (IOException ioe) {
                tmp.delete();
                throw ioe;
            }
            UploadedFile uf = new UploadedFile(dinfo.getFileName(), dinfo.getMimeType(), tmp);
            FileParameter fp = new FileParameter(dinfo.getName());
            fp.addUploadedFile(uf);
            return fp;
        } else {
            if (mt != null && (mt.startsWith("multipart/mixed"))) {
                MultipartParser up = new MultipartParser(lineInput, extractBoundary((String) headers.get("content-type")), temporaryDirectory);
                List l = up.parse();
                String line = readLine();
                if (line.length() == (boundary.length() + 2) && line.endsWith("--") && line.startsWith(boundary)) {
                    eof = true;
                } else if (line.length() != boundary.length() || !line.equals(boundary)) {
                    throw new IOException("input stream corrupted");
                }
                FileParameter fp = new FileParameter(dinfo.getName());
                for (Iterator i = l.iterator(); i.hasNext(); ) {
                    Object ref = i.next();
                    if (!(ref instanceof FileParameter)) {
                        throw new IOException("Multipart/Mixed format error");
                    }
                    fp.addUploadedFile(((FileParameter) ref).getUploadedFile());
                }
                return fp;
            } else {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    readData(baos);
                } finally {
                    baos.close();
                }
                return new StandardParameter(dinfo.getName(), baos.toByteArray());
            }
        }
    }

    private void readData(OutputStream stream) throws IOException {
        int boundaryLength = boundary.length();
        int read = 0;
        boolean reachedEnd = false;
        while (!reachedEnd) {
            if (read > 1) {
                parameterBuffer[0] = parameterBuffer[read - 2];
                parameterBuffer[1] = parameterBuffer[read - 1];
                read = 2;
            }
            while (!reachedEnd && (parameterBuffer.length - read - boundaryLength - 4) >= 0) {
                int num = lineInput.readLine(parameterBuffer, read, parameterBuffer.length - read);
                if (num == -1) throw new IOException("premature EOF"); else if (num - 2 == boundaryLength) {
                    boolean matched = true;
                    for (int i = 0; matched && (i < boundaryLength); i++) {
                        matched &= boundaryBytes[i] == parameterBuffer[read + i];
                    }
                    if (matched) {
                        reachedEnd = true;
                    } else read += num;
                } else if (num - 4 == boundaryLength) {
                    boolean matched = true;
                    for (int i = 0; matched && (i < boundaryLength); i++) {
                        matched &= boundaryBytes[i] == parameterBuffer[read + i];
                        if (!matched) ;
                    }
                    if (matched && (parameterBuffer[read + boundaryLength] == '-') && parameterBuffer[read + boundaryLength + 1] == '-' && parameterBuffer[read + boundaryLength + 2] == '\r') {
                        reachedEnd = true;
                        eof = true;
                    } else read += num;
                } else read += num;
            }
            if (read > 2) {
                stream.write(parameterBuffer, 0, read - 2);
            }
        }
        stream.close();
    }

    private ParameterInformation parseParameterInfo(Map headers) throws IOException {
        String cd = (String) headers.get(CONTENT_DISPOSITION);
        if (cd == null) throw new IOException("no " + CONTENT_DISPOSITION + " header found!");
        Map attributes = attributeParser(cd);
        String ct = (String) headers.get("content-type");
        if (ct != null) {
            int idx = ct.indexOf(';');
            if (idx != -1) {
                ct = ct.substring(0, idx);
            }
        } else {
            ct = "text/plain";
        }
        return new ParameterInformation((String) attributes.get("name"), (String) attributes.get("filename"), ct);
    }

    private Map readHeaders() throws IOException {
        Map headers = new TreeMap(String.CASE_INSENSITIVE_ORDER);
        for (; ; ) {
            String line = readLine();
            if (line.length() == 0) return headers;
            int index = line.indexOf(':');
            if (index < 0) throw new IOException("header is corrupted");
            String lineString = line.substring(index + 1).trim();
            String name = line.substring(0, index);
            headers.put(name, lineString);
        }
    }

    private String readLine() throws IOException {
        int read = lineInput.readLine(lineBuffer, 0, lineBuffer.length);
        if (read == -1) throw new IOException("premature EOF");
        if (lineBuffer[read - 2] == '\r') {
            read += -2;
        } else {
            read += -1;
        }
        return new String(lineBuffer, 0, read, "ISO-8859-1");
    }

    /**
     * This helper method extracts Boundary value from the Content-Type String. 
     *
     *@param type             Content-Type string.
     *@return                 Boundary (if one is found)
     *@exception IOException  Is thrown if the boundary could not be found or the Content-Type string is corrupt.
     */
    public static String extractBoundary(String type) throws IOException {
        int i = type.lastIndexOf(BOUNDARY);
        if (i == -1) throw new IOException(BOUNDARY + " not found in " + type);
        String ret = type.substring(i + BOUNDARY.length());
        if (ret.charAt(0) == '"') {
            i = ret.lastIndexOf('"');
            ret = ret.substring(1, i);
        }
        return "--" + ret;
    }

    private static Map attributeParser(String attributeString) {
        int fromIndex;
        int toIndex = attributeString.length() - 1;
        HashMap ret = new HashMap();
        while ((fromIndex = attributeString.lastIndexOf(';', toIndex)) != -1) {
            String piece = attributeString.substring(fromIndex + 1, toIndex + 1).trim();
            int eqIndex = piece.indexOf('=');
            if (eqIndex != -1) {
                String name = piece.substring(0, eqIndex);
                if (eqIndex + 1 < piece.length()) {
                    String value;
                    if (piece.charAt(eqIndex + 1) == '"') {
                        value = piece.substring(eqIndex + 2, piece.length() - 1);
                    } else {
                        value = piece.substring(eqIndex + 1, piece.length());
                    }
                    ret.put(name, value);
                }
            }
            toIndex = fromIndex - 1;
        }
        return ret;
    }

    /**
     * This class defines a value object containing information about a parameter extracted from its headers.
     */
    private static class ParameterInformation {

        private String name;

        private String fileName;

        private String mimeType;

        /**
         * Constructs <code>ParameterInformation</code> object with the name, fileName and mimeType.
         *
         *@param name      Name of the parameter.
         *@param fileName  Filename of the parameter (null if the parameter is not a file)
         *@param mimeType  Mime type of the parameter.
         */
        public ParameterInformation(String name, String fileName, String mimeType) {
            this.name = name;
            this.fileName = fileName;
            this.mimeType = mimeType;
        }

        /**
         * Getter method for the parameter name.
         *
         *@return   The value of the parameter name.
         */
        public String getName() {
            return name;
        }

        /**
         * Getter method for the file name.
         *
         *@return   File name of the parameter if the parameter is a file-parameter null otherwise.
         */
        public String getFileName() {
            return fileName;
        }

        /**
         * Getter method for the parameter mime type
         *
         *@return   MimeType of the parameter.
         */
        public String getMimeType() {
            return mimeType;
        }
    }
}
