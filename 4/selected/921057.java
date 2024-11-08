package wyklad;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class WysylanieArchiwum extends javax.servlet.http.HttpServlet implements javax.servlet.Servlet {

    int BUF_LEN = 1024;

    String FILE_NAME = "X:\\mimuw\\Java - wybrane technologie\\IDEA Project\\Lab 03 - Serwlety\\resources\\test.jar";

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/jar");
        byte[] bufor = new byte[BUF_LEN];
        FileInputStream in = new FileInputStream(FILE_NAME);
        OutputStream out = response.getOutputStream();
        while (in.read(bufor) != -1) out.write(bufor);
        in.close();
        out.close();
    }
}
