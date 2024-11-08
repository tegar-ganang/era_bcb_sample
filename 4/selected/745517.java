package page.file;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javabean.UserInfo;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import util.Constant;
import util.PubFun;
import page.inc.HtmlPage;
import util.InitServlet;

public class DownLoadPage extends HtmlPage {

    @Override
    public String print(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        HttpSession session = req.getSession(true);
        UserInfo user = (UserInfo) session.getAttribute("userInfo");
        if (user == null) {
            PubFun.ajaxPrint("alert('页面失效；请重新登陆。');parent.location.reload();", resp);
            return null;
        }
        int type = this.getIntParameter("type", Constant.FILE_PATH_TYPE, req);
        String filepath = req.getParameter("filepath");
        String names = req.getParameter("name");
        String path = InitServlet.WEB_SITE_PATH;
        if (type == Constant.TEMPLATE_PATH_TYPE) path = InitServlet.CONTENT_REALPATH;
        File file = new File(path + filepath + "/" + names);
        resp.setContentType("application/force-download");
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
        resp.setHeader("Content-length", String.valueOf(in.available()));
        resp.setHeader("content-disposition", "attachment;filename=" + names);
        BufferedOutputStream out = new BufferedOutputStream(resp.getOutputStream());
        readAndWrite(in, out);
        return null;
    }

    private void readAndWrite(InputStream in, OutputStream out) throws IOException {
        byte[] read = new byte[1024];
        int readByte = 0;
        while (-1 != (readByte = in.read(read, 0, read.length))) {
            out.write(read, 0, readByte);
        }
        in.close();
        out.close();
    }
}
