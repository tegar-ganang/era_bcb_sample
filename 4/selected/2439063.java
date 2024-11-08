package net.trieloff.xmlwebgui.servlet;

import java.io.*;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.trieloff.xmlwebgui.util.UserRights;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 *  Description of the Class
 *
 *@author     Lars Trieloff
 *@created    6. M�rz 2002
 */
public class ValidateServlet extends HttpServlet {

    static Document inputDocument;

    static Document outputDocument;

    /**
   *  Description of the Method
   *
   *@param  request               Description of Parameter
   *@param  response              Description of Parameter
   *@exception  ServletException  Description of Exception
   *@exception  IOException       Description of Exception
   *@since
   */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println("[ValidateServlet] start");
        PrintWriter out = response.getWriter();
        response.setContentType("text/html");
        String xmlStream = new String();
        xmlStream = (request.getParameter("xmldata") == null) ? "" : request.getParameter("xmldata");
        ServletConfig conf = this.getServletConfig();
        ServletContext contxt = conf.getServletContext();
        String basepath = contxt.getInitParameter("rootDir");
        String template = request.getParameter("template");
        String saveas = request.getParameter("saveas");
        String styleurl = request.getParameter("styleurl");
        String exit = request.getParameter("save");
        String apppath = contxt.getInitParameter("applicationDir");
        String sessionValidatorURL = contxt.getInitParameter("sessionValidatorURL");
        String sessionName = contxt.getInitParameter("sessionIdParameterName");
        String sessionIdValue = UserRights.getCustomSessionId(request, sessionName);
        String username = UserRights.getCustomUsername(sessionValidatorURL, sessionName, sessionIdValue);
        Element userNode = UserRights.getUserNode(apppath, username);
        if (UserRights.getUserRights(userNode, template, "read") && UserRights.getUserRights(userNode, template, "write")) {
            System.out.println("Authentication: OK");
        } else {
            System.out.println("Authentication: BAD");
            response.sendRedirect(contxt.getInitParameter("errorUrl"));
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document templateDoc;
        DocumentType templateDocType;
        inputDocument = createInputDocument(xmlStream);
        outputDocument = createOutputDocument();
        evaluateXhtmlNodes(inputDocument, outputDocument);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            templateDoc = builder.parse(new File(basepath + template));
            templateDocType = templateDoc.getDoctype();
            String publicId = templateDocType.getPublicId();
            String systemId = templateDocType.getSystemId();
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, systemId);
            if (publicId != null) {
                transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, publicId);
            }
            DOMSource source = new DOMSource(outputDocument);
            StreamResult result = new StreamResult(new File(basepath + saveas));
            transformer.transform(source, result);
            Enumeration parameters = request.getAttributeNames();
            String nameBuffer;
            StringBuffer urlBuffer = new StringBuffer("?");
            for (Enumeration e = request.getParameterNames(); e.hasMoreElements(); ) {
                nameBuffer = (String) e.nextElement();
                if (!nameBuffer.equals("xmldata")) {
                    urlBuffer.append(nameBuffer);
                    urlBuffer.append("=");
                    if (nameBuffer.equals("template") && exit.equals("save")) {
                        urlBuffer.append(saveas);
                    } else {
                        urlBuffer.append(request.getParameter(nameBuffer).toString());
                    }
                    if (e.hasMoreElements()) {
                        urlBuffer.append("&");
                    }
                }
            }
            System.out.println(response.encodeURL(contxt.getInitParameter("exitUrl") + urlBuffer.toString()));
            if (exit.equals("exit")) {
                response.sendRedirect(response.encodeURL(contxt.getInitParameter("exitUrl") + urlBuffer.toString()));
            } else {
                response.sendRedirect(response.encodeURL("editor" + urlBuffer.toString()));
            }
        } catch (Exception e) {
            System.out.println("[ValidateServlet] exception");
            e.printStackTrace();
        }
        System.out.println("[ValidateServlet] stop");
    }

    /**
   *  Description of the Method
   *
   *@param  request               Description of Parameter
   *@param  response              Description of Parameter
   *@exception  ServletException  Description of Exception
   *@exception  IOException       Description of Exception
   *@since
   */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    /**
   *  Description of the Method
   *
   *@param  input   Description of Parameter
   *@param  output  Description of Parameter
   *@since
   */
    private void evaluateXhtmlNodes(org.w3c.dom.Node input, org.w3c.dom.Node output) {
        String elmstr;
        org.w3c.dom.Node myElement;
        if (input.getNodeType() == org.w3c.dom.Node.DOCUMENT_NODE) {
            input = (((Document) input).getDocumentElement());
            elmstr = new String(((Element) input).getAttribute("class").substring(3));
            myElement = ((Document) output).createElement(elmstr);
        } else if (((Element) input).getAttribute("class").equals("cdata")) {
            elmstr = new String(input.getFirstChild().getNodeValue());
            myElement = ((Document) output.getOwnerDocument()).createTextNode(elmstr);
        } else if (((Element) input).getAttribute("class").equals("comment")) {
            elmstr = new String(input.getFirstChild().getNodeValue());
            myElement = ((Document) output.getOwnerDocument()).createComment(elmstr);
        } else {
            elmstr = new String(((Element) input).getAttribute("class").substring(3));
            myElement = ((Document) output.getOwnerDocument()).createElement(elmstr);
        }
        output.appendChild(myElement);
        NodeList attNodes = ((Element) input).getElementsByTagName("DL");
        if (attNodes.getLength() == 0) {
            attNodes = ((Element) input).getElementsByTagName("dl");
        }
        System.out.println("dl/dl length: " + attNodes.getLength());
        NodeList txtNodes;
        String DTbuffer = new String();
        String DDbuffer = new String();
        int Cbuffer = 1;
        if (attNodes.getLength() != 0) {
            attNodes = attNodes.item(0).getChildNodes();
            for (int i = 0; i < attNodes.getLength(); i++) {
                if (attNodes.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    if ((((Element) attNodes.item(i)).getTagName().equals("DT")) || (((Element) attNodes.item(i)).getTagName().equals("dt"))) {
                        txtNodes = attNodes.item(i).getChildNodes();
                        for (int j = 0; j < txtNodes.getLength(); j++) {
                            try {
                                if (txtNodes.item(j).getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
                                    DTbuffer = txtNodes.item(j).getNodeValue();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else if ((((Element) attNodes.item(i)).getTagName().equals("DD")) || (((Element) attNodes.item(i)).getTagName().equals("dd"))) {
                        txtNodes = attNodes.item(i).getChildNodes();
                        for (int j = 0; j < txtNodes.getLength(); j++) {
                            try {
                                if (txtNodes.item(j).getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
                                    DDbuffer = txtNodes.item(j).getNodeValue();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (!DDbuffer.equals("")) {
                        try {
                            ((Element) myElement).setAttribute(DTbuffer, DDbuffer);
                        } catch (Exception e) {
                        }
                    }
                    DDbuffer = "";
                }
            }
        }
        NodeList childNodes = input.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            if ((childNodes.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) && (!((Element) childNodes.item(i)).getAttribute("class").equals("infobox"))) {
                evaluateXhtmlNodes(childNodes.item(i), myElement);
            }
        }
    }

    /**
   *  Description of the Method
   *
   *@return    Description of the Returned Value
   *@since
   */
    private Document createOutputDocument() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document outputDoc = builder.newDocument();
            return outputDoc;
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }
        return null;
    }

    /**
   *  Description of the Method
   *
   *@param  input  Description of Parameter
   *@return        Description of the Returned Value
   *@since
   */
    private Document createInputDocument(String input) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document inputDoc = builder.parse(new org.xml.sax.InputSource(new java.io.StringReader(input)));
            return inputDoc;
        } catch (SAXException sxe) {
            Exception x = sxe;
            if (sxe.getException() != null) {
                x = sxe.getException();
            }
            x.printStackTrace();
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }
        return null;
    }
}
