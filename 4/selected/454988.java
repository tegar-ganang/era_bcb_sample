package cn.vlabs.duckling.vwb.services.attachment.clb;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import cn.cnic.esac.clb.api.FileSaver;
import cn.vlabs.duckling.util.MimeType;
import cn.vlabs.rest.IFileSaver;

public class AttSaver implements IFileSaver, FileSaver {

    OutputStream out = null;

    HttpServletResponse m_res = null;

    HttpServletRequest m_req = null;

    public AttSaver(HttpServletResponse res, HttpServletRequest req) {
        this.m_res = res;
        this.m_req = req;
    }

    public void save(String filename, InputStream in) {
        try {
            filename = java.net.URLDecoder.decode(filename, "UTF-8");
            String mimetype = getMimeType(m_req, filename);
            m_res.setContentType(mimetype);
            String agent = m_req.getHeader("USER-AGENT");
            String suffix = filename.substring(filename.indexOf(".") + 1, filename.length());
            m_res.setContentType(MimeType.getContentType(suffix));
            if (filename.indexOf("swf") != -1) {
                m_res.setContentType("application/x-shockwave-flash");
            } else {
                if (null != agent && -1 != agent.indexOf("MSIE")) {
                    String codedfilename = java.net.URLEncoder.encode(filename, "UTF-8");
                    codedfilename = StringUtils.replace(codedfilename, "+", "%20");
                    if (codedfilename.length() > 150) {
                        codedfilename = new String(filename.getBytes("GBK"), "ISO8859-1");
                        codedfilename = StringUtils.replace(codedfilename, " ", "%20");
                    }
                    m_res.setHeader("Content-Disposition", "attachment;filename=\"" + codedfilename + "\"");
                } else if (null != agent && -1 != agent.indexOf("Firefox")) {
                    String codedfilename = javax.mail.internet.MimeUtility.encodeText(filename, "UTF-8", "B");
                    m_res.setHeader("Content-Disposition", "attachment;filename=\"" + codedfilename + "\"");
                } else {
                    m_res.setHeader("Content-Disposition", "attachment;filename=\"" + filename + "\"");
                }
            }
            if (out == null) out = m_res.getOutputStream();
            int read = 0;
            byte buf[] = new byte[4096];
            while ((read = in.read(buf, 0, 4096)) != -1) {
                out.write(buf, 0, read);
            }
            if (out != null) {
                out.close();
            }
        } catch (FileNotFoundException e) {
            System.out.println("没有找到文件。");
        } catch (IOException e) {
            System.out.println("文件下载出错。");
        }
    }

    private static String getMimeType(HttpServletRequest req, String fileName) {
        String mimetype = null;
        if (req != null) {
            ServletContext s = req.getSession().getServletContext();
            if (s != null) {
                mimetype = s.getMimeType(fileName.toLowerCase());
            }
        }
        if (mimetype == null) {
            mimetype = "application/binary";
        }
        return mimetype;
    }

    public void save(InputStream in, String filename) throws IOException {
        save(filename, in);
    }
}
