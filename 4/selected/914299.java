package annone.engine.jsp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Locale;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.xml.sax.InputSource;
import annone.util.Text;
import annone.util.Tools;
import annone.util.xml.XmlDocument;
import annone.util.xml.XmlNode;

public class JspProxy extends JspServlet {

    private static final long serialVersionUID = 6467745593149237453L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Enumeration<Locale> it = request.getLocales();
        String path = request.getRequestURI();
        path = path.substring(request.getContextPath().length());
        if (path.equals("/")) path = "/main"; else {
            InputStream in = null;
            while ((in == null) && it.hasMoreElements()) in = getClass().getResourceAsStream("resources/" + it.nextElement().toString() + path);
            if (in == null) in = getClass().getResourceAsStream("resources/root" + path);
            if (in != null) {
                byte[] b = new byte[4096];
                int count;
                ServletOutputStream out = response.getOutputStream();
                while ((count = in.read(b)) >= 0) out.write(b, 0, count);
                out.close();
                return;
            }
        }
        path += ".xml";
        InputStream in = null;
        while ((in == null) && it.hasMoreElements()) in = getClass().getResourceAsStream("resources/" + it.nextElement().toString() + path);
        if (in == null) in = getClass().getResourceAsStream("resources/root" + path);
        if (in == null) {
            response.sendError(404, Text.get("Resource \"{0}\" not found.", path));
            return;
        }
        XmlDocument doc = Tools.Xml.toDocument(new InputSource(in));
        XmlNode root = doc.getRoot();
        if (!"layer".equals(root.getName())) {
            response.sendError(500, Text.get("Resource \"{0}\" doesn't have the main layer.", path));
            return;
        }
        String html = createRoot(root, request);
        response.getWriter().println(html);
    }

    protected String createRoot(XmlNode root, HttpServletRequest request) {
        HttpSession session = request.getSession();
        StringBuilder b = new StringBuilder();
        b.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">");
        b.append("<html lang=\"").append(JspSession.getLanguage(session)).append("\">");
        b.append("<head>");
        b.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
        b.append("<link rel=\"stylesheet\" href=\"").append(JspSession.getTheme(session)).append("\">");
        b.append("<script type=\"text/javascript\" src=\"scripts/prototype.js\"></script>");
        XmlNode titleNode = root.getNode("title");
        if (titleNode != null) b.append("<title>").append(titleNode.getText()).append(" | Annone</title>"); else b.append("<title>Annone</title>");
        b.append("</head>");
        b.append("<body id=\"workspace\">");
        b.append("	<div id=\"header\">Annone</div>");
        b.append("	<div id=\"contents\">");
        b.append("		<noscript>");
        b.append("			<div class=\"dialog error_dialog\">");
        b.append("				<div class=\"title\">").append(Text.get("Scripts disabled")).append("</div>");
        b.append("				<div class=\"icon\"></div>");
        b.append("				<div class=\"contents\">");
        b.append("					<p>").append(Text.get("Annone requires javascript to be enabled in your browser.")).append("</p>");
        b.append("					<p>").append(Text.get("Please, enable javascript then refresh this page.")).append("</p>");
        b.append("				</div>");
        b.append("				<form action=\"").append(request.getRequestURI()).append("\">");
        b.append("					<div class=\"actions\">");
        b.append("						<input type=\"submit\" value=\"").append(Text.get("Refresh")).append("\">");
        b.append("					</div>");
        b.append("				</form>");
        b.append("			</div>");
        b.append("		</noscript>");
        b.append("	</div>");
        b.append("	<div id=\"footer\">Copyright &copy; 2011 Novabyte s.n.c.</div>");
        b.append("</body>");
        b.append("</html>");
        return b.toString();
    }
}
