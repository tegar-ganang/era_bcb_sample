package com.codeberry.yws;

import static com.codeberry.yws.Response.ErrorType.FILE_NOT_FOUND;
import com.codeberry.yws.exception.ContentException;
import java.io.*;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.SimpleTimeZone;

public class FileContextHandler implements ContextHandler {

    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("d MMM yyyy HH:mm:ss 'GMT'");

    private static final Calendar CAL_GMT = Calendar.getInstance(new SimpleTimeZone(0, "GMT"));

    static {
        FORMAT.setCalendar(CAL_GMT);
    }

    private File baseDir;

    private String contextPath;

    public FileContextHandler(String contextPath, String baseDir) {
        this.contextPath = contextPath;
        this.baseDir = new File(baseDir);
    }

    public String getContextPath() {
        return contextPath;
    }

    public Request handle(Request request, Response response) {
        String path = request.getTranslatedPath(this);
        if (path.isEmpty()) {
            path = "index.html";
        }
        File toServe = findFile(path);
        if (toServe != null) {
            handleFile(request, toServe, response);
        } else {
            handleError(response);
        }
        return null;
    }

    private void handleFile(Request request, File file, Response response) {
        String contentType = URLConnection.guessContentTypeFromName(file.getName());
        FileData fileData;
        if (contentType != null && contentType.startsWith("text/")) {
            fileData = new TranslatedData(file, request.getArgs());
        } else {
            fileData = new OriginalData(file);
        }
        writeContent(fileData.getInputStream(), response, file.lastModified(), contentType, fileData.getContentLength());
    }

    private void writeContent(InputStream in, Response response, long lastModified, String contentType, long contentLength) {
        response.setLastModified(lastModified);
        response.setContentType(contentType);
        response.setContentLength(contentLength);
        copyAndClose(in, response.ready());
    }

    private void handleError(Response response) {
        response.setError(FILE_NOT_FOUND);
    }

    private ByteArrayOutputStream translateFile(File file, Map<String, Object> args) {
        try {
            String content = fileToString(file);
            return translateContent(content, args);
        } catch (IOException e) {
            throw new ContentException("Error while translating content: " + e.getMessage());
        }
    }

    private ByteArrayOutputStream translateContent(String content, Map<String, Object> args) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream((int) (content.length() * 1.001));
        OutputStreamWriter writer = new OutputStreamWriter(out);
        int lastIndex = 0;
        int tagStart;
        while ((tagStart = content.indexOf("<%=", lastIndex)) != -1) {
            writer.append(content.substring(lastIndex, tagStart));
            lastIndex = tagStart + 3;
            int tagEnd = content.indexOf("%>", lastIndex);
            if (tagEnd != -1) {
                String varName = content.substring(lastIndex, tagEnd);
                Object value = args.get(varName);
                if (value == null) {
                    value = "";
                }
                writer.write(value.toString());
                lastIndex = tagEnd + 2;
            } else {
                lastIndex = tagStart;
                break;
            }
        }
        writer.write(content.substring(lastIndex));
        writer.close();
        return out;
    }

    private String fileToString(File file) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            StringBuilder buf = new StringBuilder((int) file.length());
            int readChar;
            while ((readChar = reader.read()) != -1) {
                buf.append((char) readChar);
            }
            return buf.toString();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    private static void copyAndClose(InputStream inputStream, BufferedOutputStream output) {
        byte[] buf = new byte[1024 * 8];
        try {
            int read;
            while ((read = inputStream.read(buf)) >= 0) {
                output.write(buf, 0, read);
            }
        } catch (IOException e) {
            throw new ContentException("error copying stream");
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    private File findFile(String filepath) {
        if (filepath.indexOf("..") >= 0) {
            System.err.println("*** Possible Hack Attempt: filepath=" + filepath);
            return null;
        }
        File ret = new File(baseDir, filepath);
        if (ret.exists() && ret.isFile()) {
            return ret;
        }
        return null;
    }

    private interface FileData {

        InputStream getInputStream();

        long getContentLength();
    }

    private class TranslatedData implements FileData {

        private ByteArrayInputStream in;

        private int contentLength;

        private TranslatedData(File file, Map<String, Object> args) {
            ByteArrayOutputStream modifiedContent = translateFile(file, args);
            in = new ByteArrayInputStream(modifiedContent.toByteArray());
            contentLength = modifiedContent.size();
        }

        public InputStream getInputStream() {
            return in;
        }

        public long getContentLength() {
            return contentLength;
        }
    }

    private class OriginalData implements FileData {

        private FileInputStream in;

        private long contentLength;

        private OriginalData(File file) {
            try {
                in = new FileInputStream(file);
                contentLength = file.length();
            } catch (FileNotFoundException e) {
                throw new ContentException(e.getMessage());
            }
        }

        public InputStream getInputStream() {
            return in;
        }

        public long getContentLength() {
            return contentLength;
        }
    }
}
