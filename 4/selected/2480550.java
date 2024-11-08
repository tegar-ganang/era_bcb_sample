package com.baldwin.www.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLEncoder;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.struts.upload.FormFile;
import sun.net.TelnetInputStream;
import sun.net.TelnetOutputStream;
import sun.net.ftp.FtpClient;

public class FtpClientServlet extends HttpServlet {

    private static final long serialVersionUID = 5749639231810880378L;

    private FtpClient ftpClient_sun;

    public FtpClientServlet() {
        super();
    }

    public void destroy() {
        super.destroy();
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">");
        out.println("<HTML>");
        out.println("  <HEAD><TITLE>A Servlet</TITLE></HEAD>");
        out.println("  <BODY>");
        out.print("    This is ");
        out.print(this.getClass());
        out.println(", using the GET method");
        out.println("  </BODY>");
        out.println("</HTML>");
        out.flush();
        out.close();
    }

    /**
	 * The doPost method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to post.
	 * 
	 * @param request the request send by the client to the server
	 * @param response the response send by the server to the client
	 * @throws ServletException if an error occurred
	 * @throws IOException if an error occurred
	 */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">");
        out.println("<HTML>");
        out.println("  <HEAD><TITLE>A Servlet</TITLE></HEAD>");
        out.println("  <BODY>");
        out.print("    This is ");
        out.print(this.getClass());
        out.println(", using the POST method");
        out.println("  </BODY>");
        out.println("</HTML>");
        out.flush();
        out.close();
    }

    private void connectServer(String server, String user, String password, String path) throws IOException {
        ftpClient_sun = new FtpClient();
        ftpClient_sun = new FtpClient();
        ftpClient_sun.openServer(server, 22);
        ftpClient_sun.login(user, password);
        if (path.length() != 0) ftpClient_sun.cd(path);
        ftpClient_sun.sendServer("XMKD test\r\n");
        ftpClient_sun.readServerResponse();
        ftpClient_sun.cd("test");
        ftpClient_sun.binary();
        System.out.println("��¼�ɹ�");
    }

    private String upload(FormFile formFile) throws Exception {
        TelnetOutputStream os = null;
        InputStream is = null;
        try {
            os = ftpClient_sun.put("upftp" + getName() + "." + getExtName(formFile.getFileName()));
            is = formFile.getInputStream();
            byte[] bytes = new byte[1024];
            int c;
            while ((c = is.read(bytes)) != -1) {
                os.write(bytes, 0, c);
            }
        } finally {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }
        return "�ϴ��ļ��ɹ�!";
    }

    /**
     * �����ļ�
     * 
     * @param fileName
     *            FormFile����
     * @param HttpServletResponse
     *            HTTP��Ӧ
     * @throws IOException
     */
    private void download(String fileName, HttpServletResponse response) throws IOException {
        TelnetInputStream ftpIn = ftpClient_sun.get(fileName);
        response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
        OutputStream out = null;
        try {
            out = response.getOutputStream();
            IOUtils.copy(ftpIn, out);
        } finally {
            if (ftpIn != null) {
                ftpIn.close();
            }
        }
    }

    /**
     * չʾͼƬ     * 
     * @param fileName
     *            FormFile����
     * @param HttpServletResponse
     *            HTTP��Ӧ
     * @throws IOException
     */
    private void show(String fileName, HttpServletResponse response) throws IOException {
        TelnetInputStream ftpIn = ftpClient_sun.get(fileName);
        OutputStream out = null;
        try {
            out = response.getOutputStream();
            IOUtils.copy(ftpIn, out);
        } finally {
            if (ftpIn != null) {
                ftpIn.close();
            }
        }
    }
}
