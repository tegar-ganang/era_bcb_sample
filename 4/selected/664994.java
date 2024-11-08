package org.fto.jthink.j2ee.web.fileload;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import org.fto.jthink.exception.JThinkRuntimeException;
import org.fto.jthink.log.LogManager;
import org.fto.jthink.log.Logger;

/**
 * 文件下载, 通过HTTP协议下载文件。
 * 
 * <p><pre><b>
 * 历史更新记录:</b>
 * 2005-07-5  创建此类型
 * </pre></p>
 * 
 * 
 * @author   wenjian
 * @version  1.0
 * @since    JThink 1.0
 */
public class FileDownload {

    private static Logger logger = LogManager.getLogger(FileDownload.class);

    protected HttpServletResponse response;

    protected ServletContext application;

    private boolean denyPhysicalPath;

    private String contentDisposition;

    /**
	 * 创建类型FileDownload的实例
	 * 
	 * @param application ServletContext类型的实例
	 * @param response HttpServletResponse类型的实例
	 */
    public FileDownload(ServletContext application, HttpServletResponse response) {
        this.response = response;
        this.application = application;
        this.denyPhysicalPath = false;
    }

    /**
	 * 设置是否拒绝物理路径
	 * 
	 * @param deny true,拒绝；false,允许
	 */
    public void setDenyPhysicalPath(boolean deny) {
        this.denyPhysicalPath = deny;
    }

    /**
	 * 设置下载文件内容部署信息
	 * 
	 * @param contentDisposition 部署信息
	 */
    public void setContentDisposition(String contentDisposition) {
        this.contentDisposition = contentDisposition;
    }

    /**
	 *下载文件
	 * 
	 * @param sourceFilePathName 源文件名,包含文件路径名
	 */
    public void downloadFile(String sourceFilePathName) {
        downloadFile(sourceFilePathName, null, null);
    }

    /**
	 * 下载文件
	 * 
	 * @param sourceFilePathName 源文件名,包含文件路径名
	 * @param contentType 内容类型
	 */
    public void downloadFile(String sourceFilePathName, String contentType) {
        downloadFile(sourceFilePathName, contentType, null);
    }

    /**
	 * 下载文件
	 * 
	 * @param sourceFilePathName 源文件名,包含文件路径名
	 * @param contentType 内容类型
	 * @param destFileName 目标文件名名称
	 */
    public void downloadFile(String sourceFilePathName, String contentType, String destFileName) {
        downloadFile(sourceFilePathName, contentType, destFileName, 65000);
    }

    /**
	 * 下载文件
	 * 
	 * @param sourceFilePathName 源文件名,包含文件路径名
	 * @param contentType 内容类型
	 * @param destFileName 目标文件名名称
	 * @param blockSize 一次下载的文件块大小,单位字节
	 */
    public void downloadFile(String sourceFilePathName, String contentType, String destFileName, int blockSize) {
        if (sourceFilePathName == null || sourceFilePathName.trim().equals("")) {
            logger.error("文件 '" + sourceFilePathName + "' 没有找到.");
            throw new IllegalArgumentException("文件 '" + sourceFilePathName + "' 没有找到.");
        }
        if (!isVirtual(sourceFilePathName) && denyPhysicalPath) {
            logger.error("物理路径被拒绝.");
            throw new SecurityException("Physical path is denied.");
        }
        ServletOutputStream servletoutputstream = null;
        BufferedOutputStream bufferedoutputstream = null;
        FileInputStream fileIn = null;
        try {
            if (isVirtual(sourceFilePathName)) {
                sourceFilePathName = application.getRealPath(sourceFilePathName);
            }
            File file = new File(sourceFilePathName);
            fileIn = new FileInputStream(file);
            long fileLen = file.length();
            int readBytes = 0;
            int totalRead = 0;
            byte b[] = new byte[blockSize];
            if (contentType == null || contentType.trim().length() == 0) {
                response.setContentType("application/x-msdownload");
            } else {
                response.setContentType(contentType);
            }
            contentDisposition = contentDisposition != null ? contentDisposition : "attachment;";
            if (destFileName == null || destFileName.trim().length() == 0) {
                response.setHeader("Content-Disposition", contentDisposition + " filename=" + toUtf8String(getFileName(sourceFilePathName)));
            } else {
                response.setHeader("Content-Disposition", String.valueOf((new StringBuffer(String.valueOf(contentDisposition))).append(" filename=").append(toUtf8String(destFileName))));
            }
            servletoutputstream = response.getOutputStream();
            bufferedoutputstream = new BufferedOutputStream(servletoutputstream);
            while ((long) totalRead < fileLen) {
                readBytes = fileIn.read(b, 0, blockSize);
                totalRead += readBytes;
                bufferedoutputstream.write(b, 0, readBytes);
            }
            fileIn.close();
        } catch (JThinkRuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("下载文件时发生异常.", e);
            throw new JThinkRuntimeException(e);
        } finally {
            if (bufferedoutputstream != null) {
                try {
                    bufferedoutputstream.close();
                } catch (IOException e1) {
                    logger.error("关闭BufferedOutputStream时发生异常.", e1);
                    e1.printStackTrace();
                }
            }
            if (fileIn != null) {
                try {
                    fileIn.close();
                } catch (IOException e1) {
                    logger.error("关闭FileInputStream时发生异常.", e1);
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
	 * 判断是否虚拟目录
	 * 
	 * @param pathName
	 * @return
	 */
    private boolean isVirtual(String pathName) {
        if (application.getRealPath(pathName) != null) {
            File virtualFile = new File(application.getRealPath(pathName));
            return virtualFile.exists();
        } else {
            return false;
        }
    }

    /**
	 * 返回文件名称
	 * 
	 * @param filePathName
	 * @return
	 */
    private String getFileName(String filePathName) {
        int pos = 0;
        pos = filePathName.lastIndexOf('/');
        if (pos != -1) return filePathName.substring(pos + 1, filePathName.length());
        pos = filePathName.lastIndexOf('\\');
        if (pos != -1) return filePathName.substring(pos + 1, filePathName.length()); else return filePathName;
    }

    /**
	 * 将串编码为UTF-8格式的URI统一编码格式,如:空格,编码后为%20
	 * @param s
	 * @return
	 * @throws UnsupportedEncodingException
	 */
    private String toUtf8String(String s) throws UnsupportedEncodingException {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0 && c <= 255) {
                sb.append(c);
            } else {
                byte[] b;
                b = String.valueOf(c).getBytes("utf-8");
                for (int j = 0; j < b.length; j++) {
                    int k = b[j];
                    if (k < 0) k += 256;
                    sb.append("%" + Integer.toHexString(k).toUpperCase());
                }
            }
        }
        return sb.toString();
    }
}
