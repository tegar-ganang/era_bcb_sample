package com.jspsmart.upload;

import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;

public class SmartUpload {

    protected byte m_binArray[];

    protected HttpServletRequest m_request;

    protected HttpServletResponse m_response;

    protected ServletContext m_application;

    private int m_totalBytes;

    private int m_currentIndex;

    private int m_startData;

    private int m_endData;

    private String m_boundary;

    private long m_totalMaxFileSize;

    private long m_maxFileSize;

    private Vector m_deniedFilesList;

    private Vector m_allowedFilesList;

    private boolean m_denyPhysicalPath;

    private String m_contentDisposition;

    public static final int SAVE_AUTO = 0;

    public static final int SAVE_VIRTUAL = 1;

    public static final int SAVE_PHYSICAL = 2;

    private Files m_files;

    private Request m_formRequest;

    private boolean uploaded = false;

    public SmartUpload() {
        m_totalBytes = 0;
        m_currentIndex = 0;
        m_startData = 0;
        m_endData = 0;
        m_boundary = new String();
        m_totalMaxFileSize = 0L;
        m_maxFileSize = 0L;
        m_deniedFilesList = new Vector();
        m_allowedFilesList = new Vector();
        m_denyPhysicalPath = false;
        m_contentDisposition = "";
        m_files = new Files();
        m_formRequest = new Request();
    }

    public final void init(ServletConfig config) throws ServletException {
        m_application = config.getServletContext();
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        m_request = request;
        m_response = response;
    }

    public final void initialize(ServletConfig config, HttpServletRequest request, HttpServletResponse response) throws ServletException {
        m_application = config.getServletContext();
        m_request = request;
        m_response = response;
    }

    public final void initialize(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response) throws ServletException {
        m_application = servletContext;
        m_request = request;
        m_response = response;
    }

    public final void initialize(PageContext pageContext) throws ServletException {
        m_application = pageContext.getServletContext();
        m_request = (HttpServletRequest) pageContext.getRequest();
        m_response = (HttpServletResponse) pageContext.getResponse();
    }

    public final void initialize(ServletContext application, HttpSession session, HttpServletRequest request, HttpServletResponse response, JspWriter out) throws ServletException {
        m_application = application;
        m_request = request;
        m_response = response;
    }

    public void upload() throws SmartUploadException, IOException, ServletException {
        if (uploaded) return;
        uploaded = true;
        int totalRead = 0;
        int readBytes = 0;
        long totalFileSize = 0L;
        boolean found = false;
        String dataHeader = "";
        String fieldName = "";
        String fileName = "";
        String fileExt = "";
        String filePathName = "";
        String contentType = "";
        String contentDisp = "";
        String typeMIME = "";
        String subTypeMIME = "";
        boolean isFile = false;
        m_totalBytes = m_request.getContentLength();
        if (m_maxFileSize > 0L && m_maxFileSize < (long) m_totalBytes) throw new SmartUploadException("Too Long File.");
        m_binArray = new byte[m_totalBytes];
        for (; totalRead < m_totalBytes && readBytes >= 0; totalRead += readBytes) try {
            m_request.getInputStream();
            readBytes = m_request.getInputStream().read(m_binArray, totalRead, m_totalBytes - totalRead);
            if (readBytes < 0) throw new SmartUploadException("Read No Data");
        } catch (Exception e) {
            throw new SmartUploadException("Unable to upload.");
        }
        for (; !found && m_currentIndex < m_totalBytes; m_currentIndex++) if (m_binArray[m_currentIndex] == 13) found = true; else m_boundary = m_boundary + (char) m_binArray[m_currentIndex];
        if (m_currentIndex == 1) return;
        m_currentIndex++;
        int protectEndlessDoWhile = 0;
        do {
            protectEndlessDoWhile++;
            if (m_currentIndex >= m_totalBytes || protectEndlessDoWhile > 20) break;
            dataHeader = getDataHeader();
            m_currentIndex = m_currentIndex + 2;
            isFile = dataHeader.indexOf("filename") > 0;
            fieldName = getDataFieldValue(dataHeader, "name");
            if (isFile) {
                filePathName = getDataFieldValue(dataHeader, "filename");
                fileName = getFileName(filePathName);
                fileExt = getFileExt(fileName);
                contentType = getContentType(dataHeader);
                contentDisp = getContentDisp(dataHeader);
                typeMIME = getTypeMIME(contentType);
                subTypeMIME = getSubTypeMIME(contentType);
            }
            getDataSection();
            if (isFile && fileName.length() > 0) {
                if (m_deniedFilesList.contains(fileExt)) throw new SecurityException("The extension of the file is denied to be uploaded (1015).");
                if (!m_allowedFilesList.isEmpty() && !m_allowedFilesList.contains(fileExt)) throw new SecurityException("The extension of the file is not allowed to be uploaded (1010).");
                if (m_maxFileSize > (long) 0 && (long) ((m_endData - m_startData) + 1) > m_maxFileSize) throw new SecurityException(String.valueOf((new StringBuffer("Size exceeded for this file : ")).append(fileName).append(" (1105).")));
                totalFileSize += (m_endData - m_startData) + 1;
                if (m_totalMaxFileSize > (long) 0 && totalFileSize > m_totalMaxFileSize) throw new SecurityException("Total File Size exceeded (1110).");
            }
            if (isFile) {
                com.jspsmart.upload.File newFile = new com.jspsmart.upload.File();
                newFile.setParent(this);
                newFile.setFieldName(fieldName);
                newFile.setFileName(fileName);
                newFile.setFileExt(fileExt);
                newFile.setFilePathName(filePathName);
                newFile.setIsMissing(filePathName.length() == 0);
                newFile.setContentType(contentType);
                newFile.setContentDisp(contentDisp);
                newFile.setTypeMIME(typeMIME);
                newFile.setSubTypeMIME(subTypeMIME);
                if (contentType.indexOf("application/x-macbinary") > 0) m_startData = m_startData + 128;
                newFile.setSize((m_endData - m_startData) + 1);
                newFile.setStartData(m_startData);
                newFile.setEndData(m_endData);
                m_files.addFile(newFile);
            } else {
                String value = new String(m_binArray, m_startData, (m_endData - m_startData) + 1);
                m_formRequest.putParameter(fieldName, value);
            }
            if ((char) m_binArray[m_currentIndex + 1] == '-') break;
            m_currentIndex = m_currentIndex + 2;
        } while (true);
    }

    public int save(String destPathName) throws SmartUploadException, IOException, ServletException {
        return save(destPathName, 0);
    }

    public int save(String destPathName, int option) throws SmartUploadException, IOException, ServletException {
        int count = 0;
        if (destPathName == null) destPathName = m_application.getRealPath("/");
        if (destPathName.indexOf("/") != -1) {
            if (destPathName.charAt(destPathName.length() - 1) != '/') destPathName = String.valueOf(destPathName).concat("/");
        } else if (destPathName.charAt(destPathName.length() - 1) != '\\') destPathName = String.valueOf(destPathName).concat("\\");
        for (int i = 0; i < m_files.getCount(); i++) if (!m_files.getFile(i).isMissing()) {
            m_files.getFile(i).saveAs(destPathName + m_files.getFile(i).getFileName(), option);
            count++;
        }
        return count;
    }

    public int getSize() {
        return m_totalBytes;
    }

    public byte getBinaryData(int index) {
        byte retval;
        try {
            retval = m_binArray[index];
        } catch (Exception e) {
            throw new ArrayIndexOutOfBoundsException("Index out of range (1005).");
        }
        return retval;
    }

    public Files getFiles() {
        return m_files;
    }

    public Request getRequest() {
        return m_formRequest;
    }

    public void downloadFile(String sourceFilePathName) throws SmartUploadException, IOException, ServletException {
        downloadFile(sourceFilePathName, null, null);
    }

    public void downloadFile(String sourceFilePathName, String contentType) throws SmartUploadException, IOException, ServletException {
        downloadFile(sourceFilePathName, contentType, null);
    }

    public void downloadFile(String sourceFilePathName, String contentType, String destFileName) throws SmartUploadException, IOException, ServletException {
        downloadFile(sourceFilePathName, contentType, destFileName, 65000);
    }

    public void downloadFile(String sourceFilePathName, String contentType, String destFileName, int blockSize) throws SmartUploadException, IOException, ServletException {
        String tempFilename = destFileName;
        destFileName = java.net.URLEncoder.encode(tempFilename, "UTF-8");
        if (destFileName.length() > 150) {
            String guessCharset = "gb2312";
            destFileName = new String(tempFilename.getBytes(guessCharset), "ISO8859-1");
        }
        if (sourceFilePathName == null) throw new IllegalArgumentException(String.valueOf((new StringBuffer("File '")).append(sourceFilePathName).append("' not found (1040).")));
        if (sourceFilePathName.equals("")) throw new IllegalArgumentException(String.valueOf((new StringBuffer("File '")).append(sourceFilePathName).append("' not found (1040).")));
        if (!isVirtual(sourceFilePathName) && m_denyPhysicalPath) throw new SecurityException("Physical path is denied (1035).");
        if (isVirtual(sourceFilePathName)) sourceFilePathName = m_application.getRealPath(sourceFilePathName);
        java.io.File file = new java.io.File(sourceFilePathName);
        FileInputStream fileIn = new FileInputStream(file);
        long fileLen = file.length();
        int readBytes = 0;
        int totalRead = 0;
        byte b[] = new byte[blockSize];
        if (contentType == null) m_response.setContentType("application/x-msdownload"); else if (contentType.length() == 0) m_response.setContentType("application/x-msdownload"); else m_response.setContentType(contentType);
        m_response.setContentLength((int) fileLen);
        m_contentDisposition = m_contentDisposition != null ? m_contentDisposition : "attachment;";
        if (destFileName == null) m_response.setHeader("Content-Disposition", String.valueOf((new StringBuffer(String.valueOf(m_contentDisposition))).append(" filename=").append(getFileName(sourceFilePathName)))); else if (destFileName.length() == 0) m_response.setHeader("Content-Disposition", m_contentDisposition); else m_response.setHeader("Content-Disposition", String.valueOf((new StringBuffer(String.valueOf(m_contentDisposition))).append(" filename=").append(destFileName)));
        while ((long) totalRead < fileLen) {
            readBytes = fileIn.read(b, 0, blockSize);
            totalRead += readBytes;
            m_response.getOutputStream().write(b, 0, readBytes);
        }
        fileIn.close();
    }

    public void downloadField(ResultSet rs, String columnName, String contentType, String destFileName) throws SQLException, IOException, ServletException {
        if (rs == null) throw new IllegalArgumentException("The RecordSet cannot be null (1045).");
        if (columnName == null) throw new IllegalArgumentException("The columnName cannot be null (1050).");
        if (columnName.length() == 0) throw new IllegalArgumentException("The columnName cannot be empty (1055).");
        byte b[] = rs.getBytes(columnName);
        if (contentType == null) m_response.setContentType("application/x-msdownload"); else if (contentType.length() == 0) m_response.setContentType("application/x-msdownload"); else m_response.setContentType(contentType);
        m_response.setContentLength(b.length);
        if (destFileName == null) m_response.setHeader("Content-Disposition", "attachment;"); else if (destFileName.length() == 0) m_response.setHeader("Content-Disposition", "attachment;"); else m_response.setHeader("Content-Disposition", "attachment; filename=".concat(String.valueOf(destFileName)));
        m_response.getOutputStream().write(b, 0, b.length);
    }

    public void fieldToFile(ResultSet rs, String columnName, String destFilePathName) throws SQLException, SmartUploadException, IOException, ServletException {
        try {
            if (m_application.getRealPath(destFilePathName) != null) destFilePathName = m_application.getRealPath(destFilePathName);
            InputStream is_data = rs.getBinaryStream(columnName);
            FileOutputStream file = new FileOutputStream(destFilePathName);
            int c;
            while ((c = is_data.read()) != -1) file.write(c);
            file.close();
        } catch (Exception e) {
            throw new SmartUploadException("Unable to save file from the DataBase (1020).");
        }
    }

    private String getDataFieldValue(String dataHeader, String fieldName) {
        String token = "";
        String value = "";
        int pos = 0;
        int i = 0;
        int start = 0;
        int end = 0;
        token = String.valueOf((new StringBuffer(String.valueOf(fieldName))).append("=").append('"'));
        pos = dataHeader.indexOf(token);
        if (pos > 0) {
            i = pos + token.length();
            start = i;
            token = "\"";
            end = dataHeader.indexOf(token, i);
            if (start > 0 && end > 0) value = dataHeader.substring(start, end);
        }
        return value;
    }

    private String getFileExt(String fileName) {
        String value = "";
        int start = 0;
        int end = 0;
        if (fileName == null) return null;
        start = fileName.lastIndexOf(46) + 1;
        end = fileName.length();
        value = fileName.substring(start, end);
        if (fileName.lastIndexOf(46) > 0) return value; else return "";
    }

    private String getContentType(String dataHeader) {
        String token = "";
        String value = "";
        int start = 0;
        int end = 0;
        token = "Content-Type:";
        start = dataHeader.indexOf(token) + token.length();
        if (start != -1) {
            end = dataHeader.length();
            value = dataHeader.substring(start, end);
        }
        return value;
    }

    private String getTypeMIME(String ContentType) {
        int pos = 0;
        pos = ContentType.indexOf("/");
        if (pos != -1) return ContentType.substring(1, pos); else return ContentType;
    }

    private String getSubTypeMIME(String ContentType) {
        int start = 0;
        int end = 0;
        start = ContentType.indexOf("/") + 1;
        if (start != -1) {
            end = ContentType.length();
            return ContentType.substring(start, end);
        } else {
            return ContentType;
        }
    }

    private String getContentDisp(String dataHeader) {
        String value = "";
        int start = 0;
        int end = 0;
        start = dataHeader.indexOf(":") + 1;
        end = dataHeader.indexOf(";");
        value = dataHeader.substring(start, end);
        return value;
    }

    private void getDataSection() {
        int searchPos = m_currentIndex;
        int keyPos = 0;
        int boundaryLen = m_boundary.length();
        m_startData = m_currentIndex;
        m_endData = 0;
        do {
            if (searchPos >= m_totalBytes) break;
            if (m_binArray[searchPos] == (byte) m_boundary.charAt(keyPos)) {
                if (keyPos == boundaryLen - 1) {
                    m_endData = ((searchPos - boundaryLen) + 1) - 3;
                    break;
                }
                searchPos++;
                keyPos++;
            } else {
                searchPos++;
                keyPos = 0;
            }
        } while (true);
        m_currentIndex = m_endData + boundaryLen + 3;
    }

    private String getDataHeader() {
        int start = m_currentIndex;
        int end = 0;
        boolean found = false;
        while (!found) if (m_binArray[m_currentIndex] == 13 && m_binArray[m_currentIndex + 2] == 13) {
            found = true;
            end = m_currentIndex - 1;
            m_currentIndex = m_currentIndex + 2;
        } else {
            m_currentIndex++;
        }
        String dataHeader = new String(m_binArray, start, (end - start) + 1);
        return dataHeader;
    }

    private String getFileName(String filePathName) {
        int pos = 0;
        pos = filePathName.lastIndexOf(47);
        if (pos != -1) return filePathName.substring(pos + 1, filePathName.length());
        pos = filePathName.lastIndexOf(92);
        if (pos != -1) return filePathName.substring(pos + 1, filePathName.length()); else return filePathName;
    }

    public void setDeniedFilesList(String deniedFilesList) throws SQLException, IOException, ServletException {
        if (deniedFilesList == null) return;
        if (deniedFilesList.equals("")) return;
        String ext = "";
        if (deniedFilesList != null) {
            ext = "";
            for (int i = 0; i < deniedFilesList.length(); i++) if (deniedFilesList.charAt(i) == ',') {
                if (!m_deniedFilesList.contains(ext)) m_deniedFilesList.addElement(ext);
                ext = "";
            } else {
                ext = ext + deniedFilesList.charAt(i);
            }
            if (ext != "") m_deniedFilesList.addElement(ext);
        } else {
            m_deniedFilesList = null;
        }
    }

    public void setAllowedFilesList(String allowedFilesList) {
        if (allowedFilesList == null) return;
        if (allowedFilesList.equals("")) return;
        String ext = "";
        if (allowedFilesList != null) {
            ext = "";
            for (int i = 0; i < allowedFilesList.length(); i++) if (allowedFilesList.charAt(i) == ',') {
                if (!m_allowedFilesList.contains(ext)) m_allowedFilesList.addElement(ext);
                ext = "";
            } else {
                ext = ext + allowedFilesList.charAt(i);
            }
            if (ext != "") m_allowedFilesList.addElement(ext);
        } else {
            m_allowedFilesList = null;
        }
    }

    public void setDenyPhysicalPath(boolean deny) {
        m_denyPhysicalPath = deny;
    }

    public void setForcePhysicalPath(boolean force) {
    }

    public void setContentDisposition(String contentDisposition) {
        m_contentDisposition = contentDisposition;
    }

    public void setTotalMaxFileSize(long totalMaxFileSize) {
        if (totalMaxFileSize <= 0) return;
        m_totalMaxFileSize = totalMaxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        if (maxFileSize <= 0) return;
        m_maxFileSize = maxFileSize;
    }

    protected String getPhysicalPath(String filePathName, int option) throws IOException {
        String path = "";
        String fileName = "";
        String fileSeparator = "";
        boolean isPhysical = false;
        fileSeparator = System.getProperty("file.separator");
        if (filePathName == null) throw new IllegalArgumentException("There is no specified destination file (1140).");
        if (filePathName.equals("")) throw new IllegalArgumentException("There is no specified destination file (1140).");
        if (filePathName.lastIndexOf("\\") >= 0) {
            path = filePathName.substring(0, filePathName.lastIndexOf("\\"));
            fileName = filePathName.substring(filePathName.lastIndexOf("\\") + 1);
        }
        if (filePathName.lastIndexOf("/") >= 0) {
            path = filePathName.substring(0, filePathName.lastIndexOf("/"));
            fileName = filePathName.substring(filePathName.lastIndexOf("/") + 1);
        }
        path = path.length() != 0 ? path : "/";
        java.io.File physicalPath = new java.io.File(path);
        if (physicalPath.exists()) isPhysical = true; else {
            physicalPath.mkdirs();
            isPhysical = true;
        }
        if (option == 0) {
            if (isVirtual(path)) {
                path = m_application.getRealPath(path);
                if (path.endsWith(fileSeparator)) path = path + fileName; else path = String.valueOf((new StringBuffer(String.valueOf(path))).append(fileSeparator).append(fileName));
                return path;
            }
            if (isPhysical) {
                if (m_denyPhysicalPath) throw new IllegalArgumentException("Physical path is denied (1125)."); else return filePathName;
            } else {
                throw new IllegalArgumentException("This path does not exist (1135).");
            }
        }
        if (option == 1) {
            if (isVirtual(path)) {
                path = m_application.getRealPath(path);
                if (path.endsWith(fileSeparator)) path = path + fileName; else path = String.valueOf((new StringBuffer(String.valueOf(path))).append(fileSeparator).append(fileName));
                return path;
            }
            if (isPhysical) throw new IllegalArgumentException("The path is not a virtual path."); else throw new IllegalArgumentException("This path does not exist (1135).");
        }
        if (option == 2) {
            if (isPhysical) if (m_denyPhysicalPath) throw new IllegalArgumentException("Physical path is denied (1125)."); else return filePathName;
            if (isVirtual(path)) throw new IllegalArgumentException("The path is not a physical path."); else throw new IllegalArgumentException("This path does not exist (1135).");
        } else {
            return null;
        }
    }

    public void uploadInFile(String destFilePathName) throws SmartUploadException, IOException {
        int intsize = 0;
        int pos = 0;
        int readBytes = 0;
        if (destFilePathName == null) throw new IllegalArgumentException("There is no specified destination file (1025).");
        if (destFilePathName.length() == 0) throw new IllegalArgumentException("There is no specified destination file (1025).");
        if (!isVirtual(destFilePathName) && m_denyPhysicalPath) throw new SecurityException("Physical path is denied (1035).");
        intsize = m_request.getContentLength();
        m_binArray = new byte[intsize];
        for (; pos < intsize; pos += readBytes) try {
            readBytes = m_request.getInputStream().read(m_binArray, pos, intsize - pos);
        } catch (Exception e) {
            throw new SmartUploadException("Unable to upload.");
        }
        if (isVirtual(destFilePathName)) destFilePathName = m_application.getRealPath(destFilePathName);
        try {
            java.io.File file = new java.io.File(destFilePathName);
            FileOutputStream fileOut = new FileOutputStream(file);
            fileOut.write(m_binArray);
            fileOut.close();
        } catch (Exception e) {
            throw new SmartUploadException("The Form cannot be saved in the specified file (1030).");
        }
    }

    private boolean isVirtual(String pathName) {
        if (m_application.getRealPath(pathName) != null) {
            java.io.File virtualFile = new java.io.File(m_application.getRealPath(pathName));
            return virtualFile.exists();
        } else {
            return false;
        }
    }

    public void stop() {
        try {
            m_binArray = null;
        } catch (Exception e) {
        }
    }
}
