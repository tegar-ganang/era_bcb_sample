package se.snigel.net.servlet;

import se.snigel.vojnevojne.VojneVojne;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

/**
 * User: kalle
 * Date: 2004-mar-17
 * Time: 11:22:46
 */
public class TestServlet extends Servlet {

    public void doGet(se.snigel.net.servlet.ServletRequest request, se.snigel.net.servlet.ServletResponse response) throws IOException {
        doHead(request, response);
        doFoot(request, response);
    }

    public void doPost(ServletRequest request, ServletResponse response) throws IOException {
        doHead(request, response);
        doFoot(request, response);
    }

    private void doFoot(se.snigel.net.servlet.ServletRequest request, se.snigel.net.servlet.ServletResponse response) throws IOException {
        response.getWriter().write("parameters:<br>");
        Iterator it = request.getParameterMap().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            response.getWriter().print((String) entry.getKey());
            response.getWriter().print("==");
            String[] values = (String[]) entry.getValue();
            for (int i = 0; i < values.length; i++) response.getWriter().print(values[i] + (i + 1 < values.length ? ", " : ""));
            response.getWriter().print("<br>");
        }
        String form = "<input name='text' type='text' value='" + request.getParameter("text") + "'><input type=submit>";
        response.getWriter().write("form post:<br><form action='test' method='post'>" + form + "</form><br>");
        response.getWriter().write("form get:<br><form action='test' method='get'>" + form + "</form><br>");
        String test = (String) request.getThread().getSession().getAttribute("test");
        if (test == null) test = "new";
        response.getWriter().write("getting session test:" + test + "<br>");
        test = String.valueOf(System.currentTimeMillis());
        response.getWriter().write("setting session test:" + test + "<br>");
        request.getThread().getSession().setAttribute("test", test);
        response.getWriter().println("<form action='/test' method='post' enctype='multipart/form-data'>");
        response.getWriter().println("<input type='hidden' name='action' value='AttachFile'>");
        response.getWriter().println("<input type='hidden' name='do' value='upload'>");
        response.getWriter().println("<table border='0'>");
        response.getWriter().println("<tr><td><b>File to upload</b></td>");
        response.getWriter().println("<td><input type='file' name='file' size='50'></td></tr>");
        response.getWriter().println("<tr><td><b>MIME Type (optional)</b></td>");
        response.getWriter().println("<td><input type='text' name='mime' size='50'></td></tr>");
        response.getWriter().println("");
        response.getWriter().println("<tr><td><b>Rename to (optional)</b></td>");
        response.getWriter().println("<td><input type='text' name='rename' size='50' value=''></td></tr>");
        response.getWriter().println("<tr><td></td><td><input type='submit' value=' Upload '></td></tr>");
        response.getWriter().println("</table>");
        response.getWriter().println("</form>");
        response.getWriter().write("</body></html>");
    }

    private void doHead(se.snigel.net.servlet.ServletRequest request, se.snigel.net.servlet.ServletResponse response) throws IOException {
        response.getWriter().write("<html><head><title>Test servlet</title></head><body>");
        response.getWriter().write("client header:<br><pre>");
        response.getWriter().write(request.getThread().getClientHeader().toString());
        response.getWriter().write("</pre><br>");
    }
}
