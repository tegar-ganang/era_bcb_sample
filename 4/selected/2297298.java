package icreate.mans;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletInputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;

/** HTTP Form Handler
 */
public class FormHandler {

    private String savePath, filepath, filename, contentType, saveFilename, temp_path;

    private HashMap fields;

    private boolean hasFile = false;

    private String currentFile;

    private int fileSize = 4096;

    /** Getter for property filename
     * @return Value of property filename
     */
    public String getFilename() {
        if (this.filename == null) return "";
        return filename;
    }

    /** Get extension of property filename
     * @return extension
     */
    public String getExtension() {
        try {
            return filename.substring(filename.lastIndexOf("."));
        } catch (Exception e) {
            return null;
        }
    }

    /** Getter for property filepath
     * @return Value of property filepath
     */
    public String getFilepath() {
        return filepath;
    }

    /** Setter for property savePath
     * @param savePath New value for property savePath
     */
    public void setSavePath(String savePath) {
        this.savePath = savePath;
    }

    /** Setter for property saveFilename
     * @param saveFilename New value for property saveFilename
     */
    public void setSaveFilename(String saveFilename) {
        this.saveFilename = saveFilename;
    }

    /** Getter for property saveFilename
     * @return Value of property saveFilename
     */
    public String getSaveFilename() {
        return this.saveFilename;
    }

    /** Getter for property contentType
     * @return Value of property contentType
     */
    public String getContentType() {
        return contentType;
    }

    /** Get value of field in header
     * @param fieldName name of field in form
     * @return value of field in form
     */
    public String getFieldValue(String fieldName) {
        if (fields == null || fieldName == null) return null;
        return (String) fields.get(fieldName);
    }

    /** Save file in form as temporary file
     * @param temp_path path for temporary file
     */
    public void saveAsTempFile(String temp_path) {
        this.temp_path = temp_path;
    }

    /** Setter for property contentType
     * @param s New value for property contentType
     */
    private void setContentType(String s) {
        if (s == null) return;
        int pos = s.indexOf(": ");
        if (pos != -1) contentType = s.substring(pos + 2, s.length());
    }

    /** Upload data from request
     * @param request Form request
     * @throws IOException Unable to read request
     */
    public void doUpload(HttpServletRequest request) throws IOException {
        if (request.getContentType() == null) return;
        ServletInputStream in = request.getInputStream();
        OutputStream out = null;
        String buffer = null;
        byte[] line = new byte[256];
        int i = in.readLine(line, 0, 256);
        if (i < 3) return;
        int boundaryLength = i - 2;
        String boundary = new String(line, 0, boundaryLength);
        fields = new HashMap();
        while (i != -1) {
            String newLine = new String(line, 0, i);
            if (newLine.startsWith("Content-Disposition: form-data; name=\"")) {
                if ((newLine.indexOf("filename=\"") != -1) && this.isHasFile()) {
                    filename = newLine.substring((newLine.indexOf("filename=\"") + 10), newLine.lastIndexOf("\""));
                    if (filename.equalsIgnoreCase("") || filename == null) {
                        i = in.readLine(line, 0, 256);
                        continue;
                    }
                    filename = filename.substring(filename.lastIndexOf("\\") + 1);
                    i = in.readLine(line, 0, 256);
                    setContentType(new String(line, 0, (i - 2)));
                    i = in.readLine(line, 0, 256);
                    newLine = new String(line, 0, i);
                    int bytes_read;
                    String s_path;
                    if (temp_path == null) {
                        if (saveFilename == null) {
                            s_path = savePath + filename;
                        } else {
                            s_path = savePath + saveFilename + getExtension();
                        }
                    } else {
                        s_path = temp_path;
                    }
                    out = new FileOutputStream(s_path);
                    int total_read = 0;
                    int total_write = 0;
                    while ((bytes_read = in.readLine(line, 0, 256)) != -1 && !(newLine.indexOf(boundary) >= 0)) {
                        total_read += bytes_read;
                        newLine = new String(line, 0, bytes_read);
                        if (newLine.indexOf(boundary) >= 0) {
                            break;
                        } else if (total_read > this.fileSize) {
                            int to_write = this.fileSize - total_write;
                            byte[] last_line = new byte[to_write];
                            last_line = line;
                            out.write(last_line, 0, to_write);
                            break;
                        } else {
                            out.write(line, 0, bytes_read);
                            total_write += bytes_read;
                            newLine = new String(line, 0, bytes_read);
                        }
                    }
                    out.close();
                } else {
                    int pos = newLine.indexOf("name=\"");
                    String fieldName = newLine.substring(pos + 6, newLine.lastIndexOf("\""));
                    i = in.readLine(line, 0, 256);
                    i = in.readLine(line, 0, 256);
                    newLine = new String(line, 0, i);
                    StringBuffer fieldValue = new StringBuffer(256);
                    while (i != -1 && !newLine.startsWith(boundary)) {
                        i = in.readLine(line, 0, 256);
                        if ((i == boundaryLength + 2 || i == boundaryLength + 4) && (new String(line, 0, i).startsWith(boundary))) fieldValue.append(newLine.substring(0, newLine.length() - 2)); else fieldValue.append(newLine);
                        newLine = new String(line, 0, i);
                    }
                    fields.put(fieldName, fieldValue.toString());
                }
            }
            i = in.readLine(line, 0, 256);
        }
        in.close();
    }

    /** Getter for property fields.
     * @return Value of property fields.
     */
    public java.util.HashMap getFields() {
        return fields;
    }

    /** Getter for property hasFile.
     * @return Value of property hasFile.
     */
    public boolean isHasFile() {
        return hasFile;
    }

    /** Setter for property hasFile.
     * @param hasFile New value of property hasFile.
     */
    public void setHasFile(boolean hasFile) {
        this.hasFile = hasFile;
    }

    /** Getter for property currentFile.
     * @return Value of property currentFile.
     */
    public String getCurrentFile() {
        return currentFile;
    }

    /** Setter for property currentFile.
     * @param currentFile New value of property currentFile.
     */
    public void setCurrentFile(String currentFile) {
        this.currentFile = currentFile;
    }

    /** Getter for property fileSize.
     * @return Value of property fileSize.
     */
    public int getFileSize() {
        return fileSize;
    }

    /** Setter for property fileSize.
     * @param fileSize New value of property fileSize.
     */
    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }
}
