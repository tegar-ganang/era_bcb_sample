package fr.sportes.secrets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import java.util.ConcurrentModificationException;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("serial")
public class AdminServlet extends HttpServlet {

    public static final Logger log = Logger.getLogger("fr.sportes");

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");
        byte[] buf = new byte[4096];
        int l = 0;
        OutputStream os = resp.getOutputStream();
        InputStream is = getServletContext().getResource("/admin.html").openStream();
        while ((l = is.read(buf)) > 0) os.write(buf, 0, l);
        is.close();
    }

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        InputStream is = req.getInputStream();
        byte[] buf = new byte[4096];
        ByteArrayOutputStream os = new ByteArrayOutputStream(16192);
        int l = 0;
        while ((l = is.read(buf)) > 0) os.write(buf, 0, l);
        String body = os.toString("UTF-8");
        os.close();
        is.close();
        try {
            JSONObject obj = (JSONObject) JSONValue.parse(body);
            if (obj == null) {
                log.log(Level.SEVERE, "POST argument absent ou mal formé");
                resp.sendError(521);
                return;
            }
            long cmd = 0;
            String prefix = null;
            String md5Pin = null;
            String book = null;
            cmd = (Long) obj.get("cmd");
            prefix = (String) obj.get("prefix");
            md5Pin = (String) obj.get("md5Pin");
            book = (String) obj.get("book");
            AdminTransaction tr = new AdminTransaction((int) cmd, prefix, md5Pin, book);
            int status = tr.getStatus();
            if (status == 0) {
                resp.setContentType("application/json");
                resp.getWriter().print(tr.getResult());
            } else {
                resp.sendError(520 + status);
            }
        } catch (ClassCastException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            resp.sendError(501);
        } catch (ConcurrentModificationException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            resp.sendError(502);
        }
    }
}
