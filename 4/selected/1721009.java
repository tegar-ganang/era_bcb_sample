package net.trieloff.xmlwebgui.servlet;

import java.io.*;
import java.util.Enumeration;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import net.trieloff.xmlwebgui.util.UserRights;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 *  The Editor Servlet contains the first editing part of xnlwebgui. This means
 *  opening a file, applying the correct xsl stylesheet and writing it into the
 *  servlet output stream.
 *
 *@author     Lars Trieloff
 *@created    28. Februar 2002
 */
public class EditorServlet extends HttpServlet {

    /**
   *  All requests are handled by the post method. (this method).
   *
   *@param  request               The servlet's HttpRequest
   *@param  response              The servlet's HttpResponse
   *@exception  ServletException  An exception that can be raised by servlets
   *@exception  IOException       An exeception that can be raised when opening
   *      files
   *@since
   */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("[EditorServlet] start");
        PrintWriter out = response.getWriter();
        response.setContentType("text/html");
        ServletConfig conf = this.getServletConfig();
        ServletContext contxt = conf.getServletContext();
        String basepath = contxt.getInitParameter("rootDir");
        String apppath = contxt.getInitParameter("applicationDir");
        String sessionValidatorURL = contxt.getInitParameter("sessionValidatorURL");
        String sessionName = contxt.getInitParameter("sessionIdParameterName");
        String template = request.getParameter("template");
        String saveas = request.getParameter("saveas");
        String styleurl = (request.getParameter("styleurl") == null) ? "random?template=" + template : request.getParameter("styleurl");
        String sessionIdValue = UserRights.getCustomSessionId(request, sessionName);
        String username = UserRights.getCustomUsername(sessionValidatorURL, sessionName, sessionIdValue);
        Element userNode = UserRights.getUserNode(apppath, username);
        if (UserRights.getUserRights(userNode, template, "read") && UserRights.getUserRights(userNode, template, "write")) {
            System.out.println("[EditorServlet] Authentication: OK");
        } else {
            System.out.println("[EditorServlet] Authentication: BAD");
            response.sendRedirect(contxt.getInitParameter("errorUrl"));
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilderFactory myfactory = DocumentBuilderFactory.newInstance();
        myfactory.setNamespaceAware(true);
        try {
            File stylesheet = new File(apppath + "xslstyles/style.xsl");
            File datafile = new File(basepath + template);
            DocumentBuilder builder = factory.newDocumentBuilder();
            DocumentBuilder mybuilder = myfactory.newDocumentBuilder();
            Document document = builder.parse(datafile);
            Document styler = mybuilder.parse(stylesheet);
            ((Element) styler.getElementsByTagName("script").item(3)).setAttribute("src", "dtdparse?template=" + template);
            ((Element) styler.getElementsByTagName("form").item(0)).setAttribute("action", "validate?template=" + template);
            ((Element) styler.getElementsByTagName("link").item(0)).setAttribute("href", styleurl);
            Element formElement = (Element) styler.getElementsByTagName("form").item(0);
            Element fieldsetElement = styler.createElement("fieldset");
            formElement.appendChild(fieldsetElement);
            Element inputElement = styler.createElement("input");
            inputElement.setAttribute("type", "hidden");
            Enumeration parameters = request.getAttributeNames();
            String nameBuffer;
            for (Enumeration e = request.getParameterNames(); e.hasMoreElements(); ) {
                nameBuffer = (String) e.nextElement();
                if (!nameBuffer.equals("save")) {
                    inputElement.setAttribute("name", nameBuffer);
                    inputElement.setAttribute("value", request.getParameter(nameBuffer).toString());
                    fieldsetElement.appendChild(inputElement.cloneNode(true));
                }
            }
            TransformerFactory tFactory = TransformerFactory.newInstance();
            DOMSource stylesource = new DOMSource(styler);
            Transformer transformer = tFactory.newTransformer();
            try {
                transformer = tFactory.newTransformer(stylesource);
            } catch (Exception e) {
                System.out.println("[EditorServlet] exception");
                e.printStackTrace();
            }
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(out);
            transformer.transform(source, result);
        } catch (Exception e) {
            e.printStackTrace();
            out.println("exception");
        }
        System.out.println("[EditorServlet] stop");
        out.close();
    }

    /**
   *  All requests are handled by the post method, so this method calls doPost()
   *
   *@param  request               The servlet's HttpRequest
   *@param  response              The servlet's HttpResponse
   *@exception  ServletException  An exception that can be raised by servlets
   *@exception  IOException       An exeception that can be raised when opening
   *      files
   *@since
   */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }
}
