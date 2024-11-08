package org.garret.ptl.startup;

import java.io.*;
import java.util.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.apache.catalina.connector.RequestFacade;
import org.apache.commons.lang.RandomStringUtils;
import org.garret.ptl.template.tags.FileUpload;
import org.garret.ptl.util.SystemException;
import com.oreilly.servlet.multipart.*;

/**
 * @author A. Zabaluev
 * @create 27.06.2006
 */
public class MultipartRequestWrapper extends HttpServletRequestWrapper {

    public static final String MULTIPART_CONF_ATTR = "multipart.upload";

    protected Hashtable<String, Vector<String>> parameters = new Hashtable<String, Vector<String>>();

    protected List<UploadedFile> files = new ArrayList<UploadedFile>();

    private final int EXCEPTION_FILE_SIZE = 100 * 1024 * 1024;

    private final int UNIQUE_NAME_LENGTH = 32;

    private File uploadDir;

    private int maxPostSize;

    public MultipartRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        String path = configMapAttribute(MULTIPART_CONF_ATTR, "directory", System.getProperty("java.io.tmpdir", "/tmp"));
        path += File.separator + RandomStringUtils.randomAlphanumeric(32);
        uploadDir = new File(path);
        uploadDir.mkdirs();
        maxPostSize = configMapAttribute(MULTIPART_CONF_ATTR, "maxsize", 10 * 1024 * 1024);
        if (request == null) throw new IllegalArgumentException("request cannot be null");
        if (uploadDir.getPath() == null) throw new IllegalArgumentException("saveDirectory cannot be null");
        if (maxPostSize <= 0) {
            throw new IllegalArgumentException("maxPostSize must be positive");
        }
        if (!uploadDir.isDirectory()) throw new IllegalArgumentException("Not a directory: " + uploadDir.getPath());
        if (!uploadDir.canWrite()) throw new IllegalArgumentException("Not writable: " + uploadDir.getPath());
        MultipartParser parser = new MultipartParser(request, EXCEPTION_FILE_SIZE);
        parser.setEncoding(request.getCharacterEncoding());
        Part part;
        long sumFileSize = maxPostSize;
        while ((part = parser.readNextPart()) != null) {
            String name = part.getName();
            if (part.isParam()) {
                ParamPart paramPart = (ParamPart) part;
                String value = paramPart.getStringValue();
                Vector<String> existingValues = (Vector<String>) parameters.get(name);
                if (existingValues == null) {
                    existingValues = new Vector<String>();
                    parameters.put(name, existingValues);
                }
                existingValues.addElement(value);
            } else if (part.isFile() && sumFileSize >= 0) {
                FilePart filePart = (FilePart) part;
                String fileName = filePart.getFileName();
                if (fileName != null) {
                    String systemName = RandomStringUtils.randomAlphanumeric(UNIQUE_NAME_LENGTH);
                    File fn = new File(uploadDir.getPath() + '/' + systemName);
                    FileOutputStream fis = new FileOutputStream(fn);
                    Vector<String> tokens = parameters.get(FileUpload.UPLOAD_NAME);
                    String uploadId = null;
                    if (tokens != null && tokens.size() > 0) {
                        uploadId = FileUpload.getUploadId(request, tokens.get(0));
                        FileUpload.declareUploadStart(uploadId, fn, fileName, request.getContentLength() - 500);
                    }
                    Map result = writeFilePart(filePart, fis);
                    if (uploadId != null) {
                        FileUpload.declareUploadEnd(uploadId);
                    }
                    long size = 0;
                    boolean isAscii = false;
                    if (result != null && result.size() == 2) {
                        size = (Long) result.get("size");
                        isAscii = (Boolean) result.get("isAscii");
                    }
                    if (sumFileSize - size >= 0) {
                        files.add(new UploadedFile(name, uploadDir.toString(), fileName, systemName, filePart.getContentType(), isAscii));
                        sumFileSize -= size;
                    } else {
                        files.add(new UploadedFile(name, null, fileName, null, null, false));
                        File file = new File(uploadDir.getPath() + '/' + systemName);
                        file.delete();
                    }
                } else {
                    files.add(new UploadedFile(name, null, null, null, null, false));
                }
            }
        }
    }

    private String configMapAttribute(String mapName, String itemName, String def) {
        Map<String, String> map = Configuration.getMapAttribute(mapName);
        String res = (map != null ? map.get(itemName) : null);
        return res != null ? res : def;
    }

    private int configMapAttribute(String mapName, String itemName, int def) {
        Map<String, String> map = Configuration.getMapAttribute(mapName);
        String res = (map != null ? map.get(itemName) : null);
        int intRes = def;
        try {
            intRes = Integer.parseInt(res);
        } catch (Exception e) {
        }
        return intRes;
    }

    private Map writeFilePart(FilePart part, OutputStream out) throws IOException {
        boolean isAscii = true;
        long size = 0;
        if (part.getContentType().equals("application/x-macbinary")) {
            out = new MacBinaryDecoderOutputStream(out);
        }
        int read;
        byte[] buf = new byte[8 * 1024];
        while ((read = part.getInputStream().read(buf)) != -1) {
            out.write(buf, 0, read);
            if (isAscii) {
                for (int i = 0; i < read; i++) {
                    if (buf[i] >= 127) {
                        isAscii = false;
                        break;
                    }
                }
            }
            size += read;
        }
        Map result = new HashMap();
        result.put("size", size);
        result.put("isAscii", isAscii);
        return result;
    }

    public void removeFiles() {
        if (uploadDir.exists() && uploadDir.isDirectory()) {
            File[] files = uploadDir.listFiles();
            for (File f : files) f.delete();
            uploadDir.delete();
        }
    }

    public void finalize() {
        removeFiles();
    }

    public String getParameter(String name) {
        try {
            Vector values = (Vector) parameters.get(name);
            if (values == null || values.size() == 0) {
                return null;
            }
            String value = (String) values.elementAt(values.size() - 1);
            return value;
        } catch (Exception e) {
            return null;
        }
    }

    public Enumeration getParameterNames() {
        return parameters.keys();
    }

    public String[] getParameterValues(String name) {
        try {
            Vector values = (Vector) parameters.get(name);
            if (values == null || values.size() == 0) {
                return null;
            }
            String[] valuesArray = new String[values.size()];
            values.copyInto(valuesArray);
            return valuesArray;
        } catch (Exception e) {
            return null;
        }
    }

    public Map getParameterMap() {
        Map map = new HashMap();
        for (Enumeration en = getParameterNames(); en.hasMoreElements(); ) {
            String key = (String) en.nextElement();
            map.put(key, getParameterValues(key));
        }
        return map;
    }

    public IFileParam getFileParam(String name) {
        for (UploadedFile f : files) {
            if (name.equals(f.paramName)) return f;
        }
        return null;
    }

    public List<IFileParam> getFileParams(String name) {
        List<IFileParam> list = new ArrayList<IFileParam>();
        for (UploadedFile f : files) {
            if (name.equals(f.paramName)) list.add(f);
        }
        return list;
    }

    public List<? extends IFileParam> getFileParams() {
        return files;
    }

    @Deprecated
    public File getFile(String name) throws SystemException {
        IFileParam file = getFileParam(name);
        return file != null ? file.file() : null;
    }

    @Deprecated
    public List<String> getFileNames() {
        List<String> list = new ArrayList<String>();
        for (UploadedFile f : files) {
            if (!list.contains(f.paramName)) list.add(f.paramName);
        }
        return list;
    }

    @Deprecated
    public String getFilesystemName(String name) {
        UploadedFile file = (UploadedFile) getFileParam(name);
        return file.getFilesystemName();
    }

    @Deprecated
    public String getOriginalFileName(String name) {
        IFileParam file = getFileParam(name);
        return file != null ? file.originalName() : null;
    }

    @Deprecated
    public String getFileContentType(String name) {
        IFileParam file = getFileParam(name);
        return file != null ? file.contentType() : null;
    }

    @Deprecated
    public boolean isAsciiFile(String name) {
        UploadedFile file = (UploadedFile) getFileParam(name);
        return file != null ? file.isAscii() : false;
    }

    class UploadedFile implements IFileParam {

        private String paramName;

        private String dir;

        private String systemFilename;

        private String originalFilename;

        private String type;

        private boolean isAscii;

        UploadedFile(String paramName, String dir, String originalFilename, String systemFilename, String type, boolean isAscii) {
            this.paramName = paramName;
            this.dir = dir;
            this.originalFilename = originalFilename;
            this.systemFilename = systemFilename;
            this.type = type;
            this.isAscii = isAscii;
        }

        @Override
        public String contentType() {
            return type;
        }

        public String getFilesystemName() {
            return systemFilename;
        }

        @Override
        public String originalName() {
            return originalFilename;
        }

        @Override
        public File file() throws SystemException {
            if (dir == null || systemFilename == null) {
                return null;
            } else {
                return new File(dir + File.separator + systemFilename);
            }
        }

        public boolean isAscii() {
            return isAscii;
        }
    }

    public int getMaxPostSize() {
        return maxPostSize;
    }
}
