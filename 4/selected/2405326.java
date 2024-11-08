package com.jxva.mvc.upload;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import com.jxva.util.RandomUtil;

/**
 * 超高效高速的文件上传框架,带进度显示<br>
 * 支持表单上传,支持SwfUpload上传,单个或批量上传<br>
 * <b>Usage:</b>
 * <pre>
 * Upload uploader = new Upload(request,uploadpath, "UTF-8", 500000);
 * uploader.setSaveInTempFile(false);//是否保存为临时文件
 * uploader.setAutoRenameFile(true);//是否自动重命名
 * UploadMsg upMsg = uploader.save();//上传文件
 * if(upMsg.isSuccessful){
 *   //UploadedFile file=uploader.getUploadedFile("FileData");单个文件
 *   List<UploadedFile> files = uploader.getUploadedFiles();//多个文件
 *   for (int i = 0; i < files.size(); i++) {
 *     UploadedFile upFile =files.get(i);
 *     String fullfilename=upFile.getUploadedAbsoluteFileName();
 *     String filename = new File(fullfilename).getName();
 *     //dosomething
 *   }
 * }
 * </pre>	
 * @author  The Jxva Framework
 * @since   1.0
 * @version 2008-11-27 10:21:24 by Jxva
 */
public class Upload {

    private long totalLength;

    private long totalRead;

    private HttpServletRequest request;

    private ServletInputStream sis;

    private int allowFileSize;

    private String encoding;

    private String uploadingAbsoluteFileName;

    private String uploadingFileName;

    private String uploadingFileExt;

    private String uploadedAbsoluteFileName;

    private String uploadedFileName;

    private String uploadedFileExt;

    private Map<String, UploadedFile> uploadedFiles;

    private String mimeBoundary = "";

    private String destinationPath;

    private String parameterName;

    private boolean saveInTempFile;

    private boolean autoRenameFile;

    private Map<String, String> otherParameters;

    /**
	 * 
	 * @param request  HttpServletRequest
	 * @param path 上传文件保存路径
	 * @param encoding 编码
	 * @param allowFileSize 允许上传的大小,单位:KB
	 * @throws IOException
	 */
    public Upload(HttpServletRequest request, String path, String encoding, int allowFileSize) throws IOException {
        this.otherParameters = new HashMap<String, String>();
        this.saveInTempFile = true;
        if (allowFileSize < 0) {
            this.allowFileSize = 0x100000;
        } else {
            this.allowFileSize = allowFileSize * 1024;
        }
        this.request = request;
        this.encoding = encoding;
        destinationPath = path;
        if (!destinationPath.endsWith("\\") && !destinationPath.endsWith("/")) {
            destinationPath += "\\";
        }
        totalLength = request.getContentLength();
        sis = request.getInputStream();
        totalRead = 0;
        String contentType = request.getContentType();
        if (contentType != null && contentType.indexOf(",") != -1) {
            contentType = contentType.substring(0, contentType.indexOf(","));
        } else if (contentType != null && contentType.startsWith("multipart/form-data")) {
            int i = contentType.indexOf("boundary=") + "boundary=".length();
            mimeBoundary = "--" + contentType.substring(i);
        }
        uploadedFiles = new HashMap<String, UploadedFile>();
    }

    /**
	 * 
	 * @param request  HttpServletRequest
	 * @param path 上传文件保存路径
	 * @param allowFileSize 允许上传的大小,单位:KB
	 * @throws IOException
	 */
    public Upload(HttpServletRequest request, String path, int allowFileSize) throws IOException {
        this(request, path, null, allowFileSize);
    }

    /**
	 * 
	 * @param request  HttpServletRequest
	 * @param path 上传文件保存路径,默认文件为2MB
	 * @throws IOException
	 */
    public Upload(HttpServletRequest request, String path) throws IOException {
        this(request, path, 2000);
    }

    /**
	 * 是否产生临时文件,默认为true
	 * @param saveInTempFile
	 */
    public void setSaveInTempFile(boolean saveInTempFile) {
        this.saveInTempFile = saveInTempFile;
    }

    /**
	 * 是否自动重命名上传的文件,默认为false
	 * @param autoRenameFile
	 */
    public void setAutoRenameFile(boolean autoRenameFile) {
        this.autoRenameFile = autoRenameFile;
    }

    /**
	 * 解析并保存文件
	 */
    public UploadMsg save() {
        UploadMsg msg = new UploadMsg();
        msg.allowMega = allowFileSize / 1024;
        try {
            if (request.getContentLength() > allowFileSize) {
                msg.isSuccessful = false;
                msg.result = UploadMsg.RESULT_TOO_LARGE;
                return msg;
            }
            String line = "";
            do {
                if (line != null && line.startsWith(mimeBoundary)) {
                } else if (line != null && !line.startsWith(mimeBoundary)) {
                    line = readLine();
                }
                if (line == null || line.length() <= 0) {
                    msg.isSuccessful = false;
                    msg.result = UploadMsg.RESULT_EMPTY;
                    return msg;
                }
                if (line == null || !line.startsWith(mimeBoundary)) {
                    msg.isSuccessful = false;
                    msg.result = UploadMsg.RESULT_BAD_FORMAT;
                    return msg;
                } else {
                    line = readLine();
                    if (line == null || line.length() <= 0) {
                        break;
                    }
                    String disposition = "content-disposition: form-data;";
                    boolean nextFile = false;
                    if (line.toLowerCase().indexOf("filename") == -1 && line.toLowerCase().startsWith(disposition)) {
                        nextFile = true;
                        int paraNamePost = line.indexOf("name=\"") + "name=\"".length();
                        String paraName = line.substring(paraNamePost, line.indexOf("\"", paraNamePost));
                        String paraValue = "";
                        line = this.readLine();
                        do {
                            line = this.readLine();
                            if (line == null) {
                                break;
                            } else if (!line.startsWith(mimeBoundary)) {
                                paraValue += line;
                            }
                        } while (!line.startsWith(mimeBoundary));
                        if (request.getMethod().equalsIgnoreCase("GET")) {
                            paraValue = new String(paraValue.getBytes("ISO-8859-1"), "UTF-8");
                        }
                        this.otherParameters.put(paraName, paraValue);
                    }
                    if (nextFile) {
                        continue;
                    }
                    if (line == null) {
                        break;
                    }
                    if (line.toLowerCase().startsWith(disposition)) {
                        if (line.toLowerCase().indexOf("filename=\"") > -1) {
                            this.getAbsoluteFileName(line);
                            this.getParameterName(line);
                            this.getFileName();
                            this.getFileExt();
                        } else {
                            this.getParameterName(line);
                        }
                    }
                    UploadedFile uploadedFile = new UploadedFile();
                    uploadedFile.setUploadingAbsoluteFileName(this.uploadingAbsoluteFileName);
                    uploadedFile.setUploadingFileName(this.uploadingFileName);
                    uploadedFile.setUploadingFileExt(this.uploadingFileExt);
                    uploadedFile.setParameterName(this.parameterName);
                    readLine();
                    File file = null;
                    if (!this.saveInTempFile) {
                        File dir = new File(destinationPath);
                        if (!dir.exists()) {
                            dir.mkdir();
                        }
                        uploadedFileName = calculateUploadedFileName();
                        file = new File(destinationPath + uploadedFileName);
                        file.createNewFile();
                    } else {
                        file = File.createTempFile("jxva_upload_", "." + this.uploadedFileExt);
                    }
                    this.uploadedFileName = file.getName();
                    this.uploadedAbsoluteFileName = file.getAbsolutePath();
                    uploadedFile.setUploadedAbsoluteFileName(this.uploadedAbsoluteFileName);
                    uploadedFile.setUploadedFileName(this.uploadedFileName);
                    uploadedFile.setUploadedFileExt(this.uploadedFileExt);
                    BufferedOutputStream bufferedoutputstream = new BufferedOutputStream(new FileOutputStream(file));
                    byte buffer[] = new byte[10240];
                    boolean isEnter = false;
                    int lines = 0;
                    do {
                        int i = readLine(buffer, 0, buffer.length);
                        lines++;
                        if (i <= 0) {
                            break;
                        }
                        line = new String(buffer, 0, i);
                        if (lines == 1) {
                            continue;
                        }
                        if (!line.startsWith(mimeBoundary)) {
                            if (isEnter) {
                                bufferedoutputstream.write(13);
                                bufferedoutputstream.write(10);
                                isEnter = false;
                            }
                            if (i >= 2 && buffer[i - 2] == 13 && buffer[i - 1] == 10) {
                                isEnter = true;
                                bufferedoutputstream.write(buffer, 0, i - 2);
                            } else {
                                bufferedoutputstream.write(buffer, 0, i);
                            }
                        }
                    } while (line != null && !line.startsWith(mimeBoundary));
                    bufferedoutputstream.flush();
                    bufferedoutputstream.close();
                    if (lines > 2) {
                        uploadedFiles.put(uploadedFile.getParameterName(), uploadedFile);
                    } else {
                        file.delete();
                    }
                }
            } while (true);
            msg.isSuccessful = true;
            msg.result = UploadMsg.RESULT_SUCCESS;
            return msg;
        } catch (Exception e) {
            e.printStackTrace();
            msg.isSuccessful = false;
            msg.result = UploadMsg.RESULT_EXCEPTION;
            msg.e = e;
            return msg;
        }
    }

    /**
	 * 读取一行
	 * 
	 * @return @throws IOException
	 */
    private String readLine() throws IOException {
        int readBytes = -1;
        byte buffer[] = new byte[10240];
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        do {
            readBytes = readLine(buffer, 0, buffer.length);
            if (readBytes != -1) {
                os.write(buffer, 0, readBytes);
            }
        } while (readBytes == buffer.length);
        os.flush();
        byte content[] = os.toByteArray();
        if (content.length == 0) {
            return null;
        } else {
            if (encoding != null && encoding.length() > 0) {
                return new String(content, 0, content.length - 2, encoding);
            } else {
                return new String(content, 0, content.length - 2);
            }
        }
    }

    /**
	 * 读取一行
	 * 
	 * @param buffer
	 * @param offset
	 * @param length
	 * @return @throws
	 *         IOException
	 */
    private int readLine(byte buffer[], int offset, long length) throws IOException {
        if (totalRead >= totalLength) {
            return -1;
        }
        if (length > totalLength - totalRead) {
            length = totalLength - totalRead;
        }
        int readBytes = sis.readLine(buffer, offset, (int) length);
        totalRead += readBytes;
        return readBytes;
    }

    /**
	 * 文件的参数名
	 * 
	 * @param line
	 */
    private void getParameterName(String line) {
        String disposition = "content-disposition: form-data;";
        String paraNameFlag = "form-data; name=\"";
        int i = -1;
        if (!line.toLowerCase().startsWith(disposition)) {
            return;
        }
        i = line.indexOf(paraNameFlag);
        if (i == -1) {
            return;
        }
        i += paraNameFlag.length();
        int k = line.indexOf("\"", i);
        this.parameterName = line.substring(i, k);
    }

    /**
	 * 文件绝对路径名
	 * 
	 * @param line
	 */
    private void getAbsoluteFileName(String line) {
        String disposition = "content-disposition: form-data;";
        String filenameFlag = "filename=\"";
        int i = -1;
        if (!line.toLowerCase().startsWith(disposition)) {
            return;
        }
        i = line.indexOf(filenameFlag);
        if (i == -1) {
            return;
        }
        i += filenameFlag.length();
        int k = line.indexOf("\"", i);
        uploadingAbsoluteFileName = line.substring(i, k);
    }

    /**
	 * 得到文件名
	 *  
	 */
    private void getFileName() {
        if (uploadingAbsoluteFileName.indexOf("\\") > -1) {
            int i = uploadingAbsoluteFileName.lastIndexOf("\\");
            uploadingFileName = uploadingAbsoluteFileName.substring(i + 1, uploadingAbsoluteFileName.length());
        } else {
            uploadingFileName = uploadingAbsoluteFileName;
        }
    }

    /**
	 * 得到文件扩展名
	 * 
	 * @return
	 */
    private void getFileExt() {
        int i = uploadingFileName.lastIndexOf(".") + 1;
        if (i > 0) {
            uploadingFileExt = uploadingFileName.substring(i).toLowerCase();
            uploadedFileExt = uploadingFileExt;
        }
    }

    /**
	 * 计算保存文件名,如果文件存在,在文件名前加"#",如果仍然存在,继续在前面加"#"...
	 *  
	 */
    private String calculateUploadedFileName() {
        String ret = "";
        ret = uploadingFileName;
        while (true) {
            File file = new File(destinationPath + ret);
            if (autoRenameFile) {
                return RandomUtil.getAutoId() + "." + this.uploadedFileExt;
            } else {
                if (file.exists()) {
                    ret = "#" + ret;
                } else {
                    return ret;
                }
            }
        }
    }

    /**
	 * 批量上传时,得到上传文件基本信息列表
	 * @return 
	 */
    public List<UploadedFile> getUploadedFiles() {
        List<UploadedFile> ret = new LinkedList<UploadedFile>();
        ret.addAll(uploadedFiles.values());
        return ret;
    }

    /**
	 * 单个上传时,得到上传文件基本信息
	 * @param paraName
	 * @return
	 */
    public UploadedFile getUploadedFile(String paraName) {
        return (UploadedFile) uploadedFiles.get(paraName);
    }

    /**
	 * 获取其它参数
	 * @return
	 */
    public Map<String, String> getOtherParameters() {
        return this.otherParameters;
    }

    public long getTotalLength() {
        return totalLength;
    }

    public long getTotalRead() {
        return totalRead;
    }

    public int getProgress() {
        if (totalLength <= 0) return 0;
        return (int) (totalRead * 100 / totalLength);
    }
}
