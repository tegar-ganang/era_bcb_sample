package dev.httpservice;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class HttpXmlEchoService extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/xml");
        InputStreamReader isr = new InputStreamReader(request.getInputStream());
        OutputStreamWriter osw = new OutputStreamWriter(response.getOutputStream());
        char[] buf = new char[4096];
        int read = 0;
        while ((read = isr.read(buf, 0, buf.length)) != -1) {
            osw.write(buf, 0, read);
        }
        isr.close();
        osw.flush();
        osw.close();
    }
}
