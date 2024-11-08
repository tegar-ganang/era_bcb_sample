package com.isfasiel.util.file;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Properties;
import java.util.UUID;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import com.isfasiel.util.data.Data;

/**
 * @Class Name : FileUtil.java
 * @Description : EgovFormBasedFileUtil�� ���������� FileUtil �Դϴ�.
 * @Modification Information 
 * @  ������      ������              ��������
 * @ ---------   ---------   -------------------------------
 * @ 2010. 12. 14.	�躴��
 * @ 2010. 12. 17.	�躴��		���ϸ� ���� ���� ����
 * @author �躴��
 */
@Component
public class FileUtil {

    public final String FILE_ID = "fileId";

    public final String FILE_NAME = "fileName";

    public final String DIR_ID = "dirId";

    public final String FILE_PHY_NAME = "phyName";

    public final String FILE_TYPE = "fileType";

    public final String EXTENTION = "extention";

    public final String FILE_SIZE = "fileSize";

    private String CHAR_SET = "UTF-8";

    /** Buffer size */
    public final int BUFFER_SIZE = 8192;

    public final String SEPERATOR = File.separator;

    @Resource(name = "pp.file")
    private Properties properties;

    /**
	 * ������ ���ϸ� ��.
	 * 
	 * @return
	 */
    public String getPhysicalFileName() {
        return UUID.randomUUID().toString();
    }

    /**
	 * ������ Upload ó���Ѵ�.
	 * 
	 * @param request
	 * @param path
	 * @param maxFileSize
	 * @return
	 * @throws Exception
	 */
    public Data uploadFiles(HttpServletRequest request, String path, long dirId) throws Exception {
        Data list = new Data();
        int index = 0;
        MultipartHttpServletRequest mptRequest = (MultipartHttpServletRequest) request;
        Iterator fileIter = mptRequest.getFileNames();
        while (fileIter.hasNext()) {
            MultipartFile mFile = mptRequest.getFile((String) fileIter.next());
            String tmp = mFile.getOriginalFilename();
            if (tmp.lastIndexOf("\\") >= 0) {
                tmp = tmp.substring(tmp.lastIndexOf("\\") + 1);
            }
            list.add(index, FILE_NAME, tmp);
            list.add(index, FILE_PHY_NAME, getPhysicalFileName());
            if (tmp.lastIndexOf(".") < 0) {
                list.add(index, EXTENTION, "");
            } else {
                String ext = tmp.substring(tmp.lastIndexOf(".") + 1);
                if (ext.length() > 5) {
                    ext = ext.substring(0, 5);
                }
                list.add(index, EXTENTION, ext);
            }
            list.add(index, FILE_SIZE, mFile.getSize());
            list.add(index, DIR_ID, dirId);
            if (tmp.lastIndexOf(".") >= 0) {
                String phyName = list.getString(index, FILE_PHY_NAME) + tmp.substring(tmp.lastIndexOf("."));
                list.add(index, FILE_PHY_NAME, phyName);
            }
            String filePath = properties.getProperty("baseDir").replaceAll("\\\\", "/") + SEPERATOR + path + SEPERATOR + list.getString(index, FILE_PHY_NAME);
            filePath = filePath.replaceAll("\\\\", "/");
            if (mFile.getSize() > 0) {
                mFile.transferTo(new File(filePath));
            }
            if (filter(list.getString(index, EXTENTION))) {
                index++;
                list.remove(index);
            } else {
                File removed = new File(filePath);
                removed.delete();
                removed = null;
            }
        }
        return list;
    }

    protected boolean filter(String ext) {
        String[] mineTypeList = properties.getProperty("mineType").split(",");
        String mineType = ext;
        for (int i = 0; i < mineTypeList.length; i++) {
            if (mineType.equals(mineTypeList[i])) {
                return false;
            }
        }
        return true;
    }

    public String copyFileToWebServer(String basePath, String path, String phyName) throws Exception {
        String downFileName = properties.getProperty("baseDir") + SEPERATOR + path + SEPERATOR + phyName;
        downFileName = downFileName.replaceAll("\\\\", "/");
        String destFileName = basePath + SEPERATOR + path + SEPERATOR + phyName;
        destFileName = destFileName.replaceAll("\\\\", "/");
        String destPath = null;
        File file = new File(downFileName);
        File destFile = new File(destFileName);
        if (!file.exists()) {
            throw new FileNotFoundException(downFileName);
        }
        if (!file.isFile()) {
            throw new FileNotFoundException(downFileName);
        }
        if (destFile.exists()) {
            return path + "/" + phyName;
        }
        File dir = new File(basePath + SEPERATOR + path + SEPERATOR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        InputStream in = new FileInputStream(file);
        OutputStream out = new FileOutputStream(destFile);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
        return path + "/" + phyName;
    }

    /**
	 * ���� �ٿ�ε�
	 * @param response
	 * @param request
	 * @param basePath
	 * @param path
	 * @param phyName
	 * @param name
	 * @throws Exception
	 */
    public void downloadFile(HttpServletResponse response, HttpServletRequest request, String path, String phyName, String name) throws Exception {
        String downFileName = properties.getProperty("baseDir") + SEPERATOR + path + SEPERATOR + phyName;
        downFileName = downFileName.replaceAll("\\\\", "/");
        File file = new File(downFileName);
        if (!file.exists()) {
            throw new FileNotFoundException(downFileName);
        }
        if (!file.isFile()) {
            throw new FileNotFoundException(downFileName);
        }
        byte[] b = new byte[BUFFER_SIZE];
        response.setContentType("application/octet-stream");
        response.setContentType("application/x-download");
        response.setHeader("Content-Transfer-Encoding", "binary");
        response.setContentType("application/octet-stream; charset=utf-8");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        String userAgent = request.getHeader("User-Agent");
        if (userAgent.indexOf("MSIE 5.5") > -1) {
            response.setHeader("Content-Disposition", "filename=" + URLEncoder.encode(name.replaceAll(" ", "_"), CHAR_SET) + ";");
        } else if (userAgent.indexOf("MSIE") > -1) {
            response.setHeader("Content-Disposition", "attachment; filename=" + java.net.URLEncoder.encode(name.replaceAll(" ", "_"), CHAR_SET) + ";");
        } else {
            response.setHeader("Content-Disposition", "attachment; filename=" + new String(name.replaceAll(" ", "_").getBytes(CHAR_SET), "latin1") + ";");
        }
        BufferedInputStream fin = null;
        BufferedOutputStream outs = null;
        try {
            fin = new BufferedInputStream(new FileInputStream(file));
            outs = new BufferedOutputStream(response.getOutputStream());
            int read = 0;
            while ((read = fin.read(b)) != -1) {
                outs.write(b, 0, read);
            }
        } finally {
            if (outs != null) {
                outs.close();
            }
            if (fin != null) {
                fin.close();
            }
        }
    }

    /**
	 * ������ ��Ʈ������ �б�
	 * @param path
	 * @return
	 */
    public String readFile(String path) throws IOException {
        if (path.length() == 0) {
            return null;
        }
        BufferedReader in = new BufferedReader(new FileReader(path));
        StringBuffer sb = new StringBuffer();
        String s;
        while ((s = in.readLine()) != null) {
            sb.append(s);
        }
        in.close();
        return sb.toString();
    }
}
