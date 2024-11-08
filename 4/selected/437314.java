package eva.io;

import java.io.*;
import java.net.*;
import java.util.*;

public class HttpPostUpload {

    URL actionURL;

    HttpURLConnection connection;

    OutputStream outputStream;

    String boundary = "1234567890";

    List<UploadFile> files;

    List<FormField> fields;

    int uploadFilesDone;

    long uploadCurFileBytes;

    long uploadCurFileBytesDone;

    String curUploadFileName;

    long uploadSumBytesDone;

    long uploadSumBytes;

    boolean uploadStarted;

    boolean uploadFinished;

    private static final String CRLF = "\r\n";

    private static final int FILE_BUFFER_SIZE = 2 * 1024;

    private static final int CHUNK_SIZE = 2 * 1024;

    public HttpPostUpload(URL actionURL) {
        this(actionURL, new LinkedList<UploadFile>());
    }

    public HttpPostUpload(URL actionURL, UploadFile file) {
        this(actionURL, new LinkedList<UploadFile>());
        try {
            addUploadFile(file);
        } catch (UploadException ex) {
        }
    }

    public HttpPostUpload(URL actionURL, List<UploadFile> files) {
        this.actionURL = actionURL;
        this.files = new LinkedList<UploadFile>(files);
        this.fields = new LinkedList<FormField>();
        uploadStarted = false;
    }

    public int getUploadFiles() {
        return files.size();
    }

    public int getUploadFilesDone() {
        return uploadFilesDone;
    }

    public long getUploadCurrentFileBytes() {
        return uploadCurFileBytes;
    }

    public void addUploadFile(UploadFile file) throws UploadException {
        if (!uploadStarted) {
            files.add(file);
        } else {
            throw new UploadException("You can't add a file, upload has already started.");
        }
    }

    public void addFormField(FormField field) throws UploadException {
        if (!uploadStarted) {
            fields.add(field);
        } else {
            throw new UploadException("You can't add a field, upload has already started.");
        }
    }

    public long getUploadCurrentFileBytesDone() {
        return uploadCurFileBytesDone;
    }

    public String getCurrentUploadFileName() {
        return curUploadFileName;
    }

    public long getUploadSumBytesDone() {
        return uploadSumBytesDone;
    }

    public long getUploadSumBytes() {
        return uploadSumBytes;
    }

    public boolean isUploading() {
        return uploadStarted;
    }

    public boolean isFinished() {
        return uploadFinished;
    }

    public void upload() throws UploadException {
        if (uploadStarted) {
            throw new UploadException("upload already started");
        } else {
            uploadStarted = true;
            calculateSumBytes();
            try {
                System.err.println("connecting...");
                connect();
                System.err.println("connected.");
                Iterator<FormField> fieldIter = fields.iterator();
                FormField curField;
                while (fieldIter.hasNext()) {
                    curField = fieldIter.next();
                    System.err.println("sending field " + curField.getName() + "...");
                    uploadFormField(curField);
                    System.err.println("field done!");
                }
                Iterator<UploadFile> fileIter = files.iterator();
                UploadFile curFile;
                while (fileIter.hasNext()) {
                    curFile = fileIter.next();
                    System.err.println("sending file " + curFile.toString() + "...");
                    uploadFile(curFile);
                    System.err.println("file done!");
                }
                writeEpilog();
                outputStream.close();
                connection.disconnect();
                uploadStarted = false;
                uploadFinished = true;
            } catch (IOException ex) {
                UploadException upload_ex = new UploadException("IO Error during upload:\n" + ex.getMessage());
                upload_ex.initCause(ex);
                throw upload_ex;
            }
        }
    }

    public boolean reset() {
        if (!uploadFinished) return false;
        files.clear();
        uploadSumBytes = uploadSumBytesDone = 0;
        uploadFilesDone = 0;
        uploadCurFileBytes = uploadCurFileBytesDone = 0;
        uploadStarted = uploadFinished = false;
        return true;
    }

    private long calculateSumBytes() {
        Iterator<UploadFile> iter = files.iterator();
        uploadSumBytes = 0;
        UploadFile uploadFile;
        while (iter.hasNext()) {
            uploadFile = iter.next();
            uploadSumBytes += uploadFile.getLength();
        }
        return uploadSumBytes;
    }

    private void connect() throws UploadException, IOException {
        if (connection != null) {
            throw new UploadException("already connected");
        } else {
            connection = (HttpURLConnection) actionURL.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", "EVA Schedule Calculator www.evaschedule.org");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setChunkedStreamingMode(CHUNK_SIZE);
            connection.connect();
            outputStream = connection.getOutputStream();
        }
    }

    private void uploadFile(UploadFile file) throws UploadException, IOException {
        curUploadFileName = file.getFileName();
        uploadCurFileBytes = file.getLength();
        uploadCurFileBytesDone = 0;
        writeHTTPFileEntityHeader(file);
        FileInputStream in = new FileInputStream(file.getFile());
        byte buffer[] = new byte[FILE_BUFFER_SIZE];
        int bytes_read;
        do {
            bytes_read = in.read(buffer);
            if (bytes_read != -1) {
                outputStream.write(buffer, 0, bytes_read);
                uploadCurFileBytesDone += bytes_read;
                uploadSumBytesDone += bytes_read;
            }
        } while (bytes_read == FILE_BUFFER_SIZE);
        writeASCIIToOutputStream("\r\n");
    }

    private void writeHTTPFileEntityHeader(UploadFile file) throws UploadException, IOException {
        if (!uploadStarted) {
            throw new UploadException("upload has not been started");
        } else if (outputStream == null) {
            throw new UploadException("outputStream is null");
        } else {
            String entity_header = CRLF + "--" + boundary + CRLF + "Content-Disposition: form-data; name=\"" + file.getFieldName() + "\"; filename=\"" + file.getFileName() + "\"" + CRLF + "Content-Type: " + file.getMimeType() + CRLF + "Content-Length: " + file.getLength() + CRLF + CRLF;
            writeASCIIToOutputStream(entity_header);
        }
    }

    private void uploadFormField(FormField field) throws UploadException, IOException {
        if (!uploadStarted) {
            throw new UploadException("upload has not been started");
        } else if (outputStream == null) {
            throw new UploadException("outputStream is null");
        } else {
            String entity_header = CRLF + "--" + boundary + CRLF + "Content-Disposition: form-data; name=\"" + field.getName() + "\"" + CRLF + CRLF + field.getEncodedValue();
            writeASCIIToOutputStream(entity_header);
        }
    }

    private void writeEpilog() throws IOException {
        writeASCIIToOutputStream(CRLF + "--" + boundary + "--" + CRLF);
    }

    private void writeASCIIToOutputStream(String msg) throws IOException {
        try {
            outputStream.write(msg.getBytes("ascii"));
        } catch (UnsupportedEncodingException ex) {
            System.err.println("encoding failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
