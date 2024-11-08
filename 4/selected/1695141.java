package demo;

import java.io.*;
import javax.mail.*;
import javax.mail.internet.*;
import javax.servlet.*;
import javax.servlet.http.*;

/**
 * This servlet gets the input stream for a given msg part and 
 * pushes it out to the browser with the correct content type.
 * Used to display attachments and relies on the browser's
 * content handling capabilities.
 */
public class AttachmentServlet extends HttpServlet {

    /**
     * This method handles the GET requests from the client.
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession();
        ServletOutputStream out = response.getOutputStream();
        int msgNum = Integer.parseInt(request.getParameter("message"));
        int partNum = Integer.parseInt(request.getParameter("part"));
        MailUserBean mailuser = (MailUserBean) session.getAttribute("mailuser");
        if (mailuser.isLoggedIn()) {
            try {
                Message msg = mailuser.getFolder().getMessage(msgNum);
                Multipart multipart = (Multipart) msg.getContent();
                Part part = multipart.getBodyPart(partNum);
                String sct = part.getContentType();
                if (sct == null) {
                    out.println("invalid part");
                    return;
                }
                ContentType ct = new ContentType(sct);
                response.setContentType(ct.getBaseType());
                InputStream is = part.getInputStream();
                int i;
                while ((i = is.read()) != -1) out.write(i);
                out.flush();
                out.close();
            } catch (MessagingException ex) {
                throw new ServletException(ex.getMessage());
            }
        } else {
            getServletConfig().getServletContext().getRequestDispatcher("/index.html").forward(request, response);
        }
    }
}
