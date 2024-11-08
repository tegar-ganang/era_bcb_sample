package com.rapidminer.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.Ports;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.Parameters;
import com.rapidminer.parameter.conditions.ParameterCondition;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.documentation.ExampleProcess;
import com.rapidminer.tools.xml.XHTMLEntityResolver;

/**
 * This class loads the operator's descriptions either live from the internet wiki or from the resources. The latter
 * requires that a custom Bot which gets all operator description sites from the MediaWiki was executed during build
 * time. Those html files retrieved there for an operator must have been parsed and saved in the resources folder in
 * "doc/namespace/operatorname". If the user does not have an internet connection all operators are loaded from this
 * resource. If the user does have an internet connection the operators are loaded directly from the RapidWiki site. The
 * operator description is shown in the RapidMiner Help Window.
 * 
 * @author Miguel Buescher, Sebastian Land
 * 
 */
public class OperatorDocLoader {

    private static final long serialVersionUID = 1L;

    private static final String WIKI_PREFIX_FOR_IMAGES = "http://www.rapid-i.com";

    ;

    private static final String WIKI_PREFIX_FOR_OPERATORS = "http://rapid-i.com/wiki/index.php?title=";

    private static final Logger logger = Logger.getLogger(OperatorDocLoader.class.getName());

    private static String CORRECT_HTML_STRING_DIRTY = "<html xmlns=\"http://www.w3.org/1999/xhtml\" dir=\"ltr\" lang=\"en\">";

    private static String CURRENT_OPERATOR_NAME_READ_FROM_RAPIDWIKI;

    private static String ERROR_TEXT_FOR_WIKI;

    private static String ERROR_TEXT_FOR_LOCAL;

    private static final String RESOURCE_SUB_DIR = "com/rapidminer/resources/doc";

    private static HashMap<OperatorDescription, String> OPERATOR_CACHE_MAP = new HashMap<OperatorDescription, String>();

    static {
        ERROR_TEXT_FOR_WIKI = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">" + "<html xmlns=\"http://www.w3.org/1999/xhtml\" dir=\"ltr\" lang=\"en\" xml:lang=\"en\">" + "<head>" + "<table cellpadding=0 cellspacing=0>" + "<tr><td>" + "<img src=\"" + SwingTools.getIconPath("48/bug_yellow_error.png") + "\"/>" + "</td>" + "<td width=\"5\">" + "</td>" + "<td>" + "Could not retrieve documentation of selected operator from wiki." + "</td></tr>" + "</table>" + "</head>" + "</html>";
        ERROR_TEXT_FOR_LOCAL = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">" + "<html xmlns=\"http://www.w3.org/1999/xhtml\" dir=\"ltr\" lang=\"en\" xml:lang=\"en\">" + "<head>" + "<table cellpadding=0 cellspacing=0>" + "<tr><td>" + "<img src=\"" + SwingTools.getIconPath("48/bug_yellow_error.png") + "\"/>" + "</td>" + "<td width=\"5\">" + "</td>" + "<td>" + "Selected Operator not documented locally. Try online version." + "</td></tr>" + "</table>" + "</head>" + "</html>";
    }

    /**
     * This is the method for loading an operator's documentation within the program.
     */
    public static String loadOperatorDocumentation(final boolean online, final boolean activateCache, final OperatorDescription dirtyOpDesc) {
        String toShowText;
        OperatorDescription opDesc = dirtyOpDesc;
        if (opDesc == null) {
            toShowText = ERROR_TEXT_FOR_WIKI;
        } else {
            if (activateCache && OPERATOR_CACHE_MAP.containsKey(opDesc)) {
                return OPERATOR_CACHE_MAP.get(opDesc);
            } else {
                try {
                    if (online) {
                        toShowText = loadSelectedOperatorDocuFromWiki(opDesc);
                    } else {
                        toShowText = loadSelectedOperatorDocuLocally(opDesc);
                    }
                } catch (Exception e) {
                    SwingTools.showFinalErrorMessage("rapid_doc_bot_importer_showInBrowser", e, true, e.getMessage());
                    toShowText = ERROR_TEXT_FOR_WIKI;
                }
                if (activateCache && StringUtils.isNotBlank(toShowText) && StringUtils.isNotEmpty(toShowText)) {
                    OPERATOR_CACHE_MAP.put(opDesc, toShowText);
                }
            }
        }
        return toShowText;
    }

    public static void clearOperatorCache() {
        OPERATOR_CACHE_MAP.clear();
    }

    public static boolean hasCache(OperatorDescription opDesc) {
        return OPERATOR_CACHE_MAP.containsKey(opDesc);
    }

    private static String customizeHTMLStringDirty(String HTMLString, String operatorIconPath) {
        HTMLString = HTMLString.replaceFirst("\\<[^\\>]*>", "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
        HTMLString = HTMLString.replaceFirst(CORRECT_HTML_STRING_DIRTY, "<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\" dir=\"ltr\">" + "<head>" + "<table cellpadding=0 cellspacing=0>" + "<tr><td>" + "<img src=\"" + operatorIconPath + "\" /></td>" + "<td width=\"5\">" + "</td>" + "<td>" + "<h2 class=\"firstHeading\" id=\"firstHeading\">" + CURRENT_OPERATOR_NAME_READ_FROM_RAPIDWIKI + "</h2>" + "</td></tr>" + "</table>" + "<hr noshade=\"true\">" + "</head>");
        HTMLString = HTMLString.replaceAll("<h2>", "<h4>");
        HTMLString = HTMLString.replaceAll("</h2>", "</h4>");
        HTMLString = HTMLString.replaceAll("<ul>", "<ul class=\"ports\">");
        HTMLString = HTMLString.replaceAll("<div class=\"visualClear\"/>", "");
        Pattern pattern = Pattern.compile("\\</div\\>[\\s]*\\<h4\\>|" + "\\</p\\>[\\s]*\\<h4\\>|" + "\\</h4\\>[\\s]*\\<h4\\>|" + "\\</ul\\>[\\s]*\\<h4\\>|" + "\\</h4\\>[\\s]*\\</div\\>");
        Matcher matcher = pattern.matcher(HTMLString);
        while (matcher.find()) {
            String match = matcher.group();
            String replaceString = StringUtils.EMPTY;
            if (match.startsWith("</div")) {
                replaceString = "</div><br/><h4>";
            } else if (match.startsWith("</p")) {
                replaceString = "</p><br/><h4>";
            } else if (match.startsWith("</h4") && !match.contains("div")) {
                replaceString = "</h4><br/><h4>";
            } else if (match.startsWith("</ul")) {
                replaceString = "</ul><br/><h4>";
            } else if (match.startsWith("</h4") && match.contains("div")) {
                replaceString = "</h4><br/><div>";
            }
            HTMLString = HTMLString.replace(match, replaceString);
        }
        HTMLString = HTMLString.replaceAll("<pre", "<table class=pre border=0 bordercolor=black style=border-style:dashed;");
        HTMLString = HTMLString.replaceAll("</pre", "</table");
        return HTMLString;
    }

    /**
     * 
     * @param operatorWikiName
     * @param opDesc
     * @return The parsed <tt>Document</tt> (not finally parsed) of the selected operator.
     * @throws MalformedURLException
     * @throws ParserConfigurationException
     */
    private static Document parseDocumentForOperator(String operatorWikiName, OperatorDescription opDesc) throws MalformedURLException, ParserConfigurationException {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setIgnoringComments(true);
        builderFactory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
        documentBuilder.setEntityResolver(new XHTMLEntityResolver());
        Document document = null;
        URL url = new URL(WIKI_PREFIX_FOR_OPERATORS + operatorWikiName);
        if (url != null) {
            try {
                document = documentBuilder.parse(url.openStream());
            } catch (IOException e) {
                logger.warning("Could not open " + url.toExternalForm() + ": " + e.getMessage());
            } catch (SAXException e) {
                logger.warning("Could not parse operator documentation: " + e.getMessage());
            }
            int i = 0;
            if (document != null) {
                Element contentElement = document.getElementById("content");
                if (contentElement != null) {
                    contentElement.getParentNode().removeChild(contentElement);
                }
                NodeList bodies = document.getElementsByTagName("body");
                for (int k = 0; k < bodies.getLength(); k++) {
                    Node body = bodies.item(k);
                    while (body.hasChildNodes()) {
                        body.removeChild(body.getFirstChild());
                    }
                    if (k == 0) {
                        body.appendChild(contentElement);
                    }
                }
                NodeList heads = document.getElementsByTagName("head");
                for (int k = 0; k < heads.getLength(); k++) {
                    Node head = heads.item(k);
                    while (head.hasChildNodes()) {
                        head.removeChild(head.getFirstChild());
                    }
                }
                if (heads != null) {
                    while (i < heads.getLength()) {
                        Node head = heads.item(i);
                        head.getParentNode().removeChild(head);
                    }
                }
                Element jumpToNavElement = document.getElementById("jump-to-nav");
                if (jumpToNavElement != null) {
                    jumpToNavElement.getParentNode().removeChild(jumpToNavElement);
                }
                Element mwNormalCatlinksElement = document.getElementById("mw-normal-catlinks");
                if (mwNormalCatlinksElement != null) {
                    mwNormalCatlinksElement.getParentNode().removeChild(mwNormalCatlinksElement);
                }
                Element tocElement = document.getElementById("toc");
                if (tocElement != null) {
                    tocElement.getParentNode().removeChild(tocElement);
                }
                NodeList nodeListDiv = document.getElementsByTagName("div");
                for (int k = 0; k < nodeListDiv.getLength(); k++) {
                    Element div = (Element) nodeListDiv.item(k);
                    if (div.getAttribute("class").equals("printfooter")) {
                        div.getParentNode().removeChild(div);
                    }
                }
                NodeList spanList = document.getElementsByTagName("span");
                for (int k = 0; k < spanList.getLength(); k++) {
                    Element span = (Element) spanList.item(k);
                    if (span.getAttribute("class").equals("editsection")) {
                        span.getParentNode().removeChild(span);
                    }
                }
                boolean doIt = true;
                NodeList pList = document.getElementsByTagName("p");
                for (int k = 0; k < pList.getLength(); k++) {
                    if (doIt) {
                        Node p = pList.item(k);
                        NodeList pChildList = p.getChildNodes();
                        for (int j = 0; j < pChildList.getLength(); j++) {
                            Node pChild = pChildList.item(j);
                            if (pChild.getNodeType() == Node.TEXT_NODE && pChild.getNodeValue() != null && StringUtils.isNotBlank(pChild.getNodeValue()) && StringUtils.isNotEmpty(pChild.getNodeValue())) {
                                String pChildString = pChild.getNodeValue();
                                Element newPWithoutSpaces = document.createElement("p");
                                newPWithoutSpaces.setTextContent(pChildString);
                                Node synopsis = document.createTextNode("Synopsis");
                                Element span = document.createElement("span");
                                span.setAttribute("class", "mw-headline");
                                span.setAttribute("id", "Synopsis");
                                span.appendChild(synopsis);
                                Element h2 = document.createElement("h2");
                                h2.appendChild(span);
                                Element div = document.createElement("div");
                                div.setAttribute("id", "synopsis");
                                div.appendChild(h2);
                                div.appendChild(newPWithoutSpaces);
                                Node pChildParentParent = pChild.getParentNode().getParentNode();
                                Node pChildParent = pChild.getParentNode();
                                pChildParentParent.replaceChild(div, pChildParent);
                                doIt = false;
                                break;
                            }
                        }
                    } else {
                        break;
                    }
                }
                NodeList brList = document.getElementsByTagName("br");
                while (i < brList.getLength()) {
                    Node br = brList.item(i);
                    Node parentBrNode = br.getParentNode();
                    parentBrNode.removeChild(br);
                }
                NodeList scriptList = document.getElementsByTagName("script");
                while (i < scriptList.getLength()) {
                    Node scriptNode = scriptList.item(i);
                    Node parentNode = scriptNode.getParentNode();
                    parentNode.removeChild(scriptNode);
                }
                NodeList pList2 = document.getElementsByTagName("p");
                int ccc = 0;
                while (ccc < pList2.getLength()) {
                    Node p = pList2.item(ccc);
                    NodeList pChilds = p.getChildNodes();
                    int kk = 0;
                    while (kk < pChilds.getLength()) {
                        Node pChild = pChilds.item(kk);
                        if (pChild.getNodeType() == Node.TEXT_NODE) {
                            String pNodeValue = pChild.getNodeValue();
                            if (pNodeValue == null || StringUtils.isBlank(pNodeValue) || StringUtils.isEmpty(pNodeValue)) {
                                kk++;
                            } else {
                                ccc++;
                                break;
                            }
                        } else {
                            ccc++;
                            break;
                        }
                        if (kk == pChilds.getLength()) {
                            Node parentBrNode = p.getParentNode();
                            parentBrNode.removeChild(p);
                        }
                    }
                }
                Element firstHeadingElement = document.getElementById("firstHeading");
                if (firstHeadingElement != null) {
                    CURRENT_OPERATOR_NAME_READ_FROM_RAPIDWIKI = firstHeadingElement.getFirstChild().getNodeValue();
                    firstHeadingElement.getParentNode().removeChild(firstHeadingElement);
                }
                Element siteSubElement = document.getElementById("siteSub");
                if (siteSubElement != null) {
                    siteSubElement.getParentNode().removeChild(siteSubElement);
                }
                Element contentSubElement = document.getElementById("contentSub");
                if (contentSubElement != null) {
                    contentSubElement.getParentNode().removeChild(contentSubElement);
                }
                Element catlinksElement = document.getElementById("catlinks");
                if (catlinksElement != null) {
                    catlinksElement.getParentNode().removeChild(catlinksElement);
                }
                NodeList aList = document.getElementsByTagName("a");
                if (aList != null) {
                    int k = 0;
                    while (k < aList.getLength()) {
                        Node a = aList.item(k);
                        Element aElement = (Element) a;
                        if (aElement.getAttribute("class").equals("internal")) {
                            a.getParentNode().removeChild(a);
                        } else {
                            Node aChild = a.getFirstChild();
                            if (aChild != null && (aChild.getNodeValue() != null && aChild.getNodeType() == Node.TEXT_NODE && StringUtils.isNotBlank(aChild.getNodeValue()) && StringUtils.isNotEmpty(aChild.getNodeValue()) || aChild.getNodeName() != null)) {
                                Element aChildElement = null;
                                if (aChild.getNodeName().startsWith("img")) {
                                    aChildElement = (Element) aChild;
                                    Element imgElement = document.createElement("img");
                                    imgElement.setAttribute("alt", aChildElement.getAttribute("alt"));
                                    imgElement.setAttribute("class", aChildElement.getAttribute("class"));
                                    imgElement.setAttribute("height", aChildElement.getAttribute("height"));
                                    imgElement.setAttribute("src", WIKI_PREFIX_FOR_IMAGES + aChildElement.getAttribute("src"));
                                    imgElement.setAttribute("width", aChildElement.getAttribute("width"));
                                    imgElement.setAttribute("border", "1");
                                    Node aParent = a.getParentNode();
                                    aParent.replaceChild(imgElement, a);
                                } else {
                                    k++;
                                }
                            } else {
                                a.getParentNode().removeChild(a);
                            }
                        }
                    }
                }
            }
        }
        return document;
    }

    /**
     * This method loads the documentation of the operator referenced by the operator description from the local
     * resources if present. Otherwise an exception is thrown.
     */
    private static String loadSelectedOperatorDocuLocally(OperatorDescription opDesc) throws UnsupportedEncodingException, ParserConfigurationException, URISyntaxException, IOException {
        String namespace = opDesc.getProviderNamespace();
        String documentationResource = "/" + RESOURCE_SUB_DIR + "/" + namespace + "/" + opDesc.getKey() + ".html";
        InputStream resourceStream = OperatorDocLoader.class.getResourceAsStream(documentationResource);
        if (resourceStream != null) {
            BufferedReader input = new BufferedReader(new InputStreamReader(resourceStream));
            try {
                String contents = Tools.readOutput(input);
                return contents;
            } finally {
                input.close();
            }
        } else {
            try {
                return makeOperatorDocumentation(opDesc.createOperatorInstance());
            } catch (OperatorCreationException e) {
                LogService.getRoot().log(Level.WARNING, "Failed to create operator: " + e, e);
                return ERROR_TEXT_FOR_LOCAL;
            }
        }
    }

    /**
     * This loads the documentation of the operator referenced by the operator description from the Wiki in the
     * internet.
     */
    private static String loadSelectedOperatorDocuFromWiki(OperatorDescription opDesc) throws IOException, ParserConfigurationException, OperatorCreationException, TransformerException, URISyntaxException {
        String operatorWikiName = StringUtils.EMPTY;
        if (!opDesc.isDeprecated()) {
            operatorWikiName = opDesc.getName().replace(" ", "_");
            if (opDesc.getProvider() != null) {
                String prefix = opDesc.getProvider().getPrefix();
                prefix = Character.toUpperCase(prefix.charAt(0)) + prefix.substring(1);
                operatorWikiName = prefix + ":" + operatorWikiName;
            }
            Document documentOperator = parseDocumentForOperator(operatorWikiName, opDesc);
            if (documentOperator != null) {
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                StreamResult result = new StreamResult(new StringWriter());
                DOMSource source = new DOMSource(documentOperator);
                transformer.transform(source, result);
                String HTMLString = result.getWriter().toString();
                HTMLString = customizeHTMLStringDirty(HTMLString, SwingTools.getIconPath("24/" + opDesc.getIconName()));
                return HTMLString;
            }
        }
        return loadSelectedOperatorDocuLocally(opDesc);
    }

    private static String makeOperatorDocumentation(Operator displayedOperator) {
        OperatorDescription descr = displayedOperator.getOperatorDescription();
        StringBuilder buf = new StringBuilder("<html>");
        buf.append("<table cellpadding=0 cellspacing=0><tr><td>");
        String iconName = "icons/24/" + displayedOperator.getOperatorDescription().getIconName();
        URL resource = Tools.getResource(iconName);
        if (resource != null) {
            buf.append("<img src=\"" + resource + "\"/> ");
        }
        buf.append("</td><td style=\"padding-left:4px;\">");
        buf.append("<h2>" + descr.getName());
        String wikiName;
        try {
            wikiName = URLEncoder.encode(descr.getName(), "UTF-8");
            buf.append(" <small><a href=\"http://rapid-i.com/wiki/index.php?title=").append(wikiName).append("\">(Wiki)</a></small>");
        } catch (UnsupportedEncodingException e) {
            LogService.getRoot().log(Level.WARNING, "Failed to URL-encode operator name: " + descr.getName() + ": " + e, e);
        }
        buf.append("</h2>");
        buf.append("</td></tr></table>");
        buf.append("<hr noshade=\"true\"/><br/>");
        buf.append("<h4>Synopsis</h4>");
        buf.append("<p>");
        buf.append(descr.getShortDescription());
        buf.append("</p>");
        buf.append("</p><br/>");
        buf.append("<h4>Description</h4>");
        String descriptionText = descr.getLongDescriptionHTML();
        if (descriptionText != null) {
            if (!descriptionText.trim().startsWith("<p>")) {
                buf.append("<p>");
            }
            buf.append(descriptionText);
            if (!descriptionText.trim().endsWith("</p>")) {
                buf.append("</p>");
            }
            buf.append("<br/>");
        }
        appendPortsToDocumentation(displayedOperator.getInputPorts(), "Input", null, buf);
        appendPortsToDocumentation(displayedOperator.getOutputPorts(), "Output", "outPorts", buf);
        Parameters parameters = displayedOperator.getParameters();
        if (parameters.getKeys().size() > 0) {
            buf.append("<h4>Parameters</h4><dl>");
            for (String key : parameters.getKeys()) {
                ParameterType type = parameters.getParameterType(key);
                if (type == null) {
                    LogService.getRoot().warning("Unknown parameter key: " + displayedOperator.getName() + "# " + key);
                    continue;
                }
                buf.append("<dt>");
                if (type.isExpert()) {
                    buf.append("<i>");
                }
                buf.append(makeParameterHeader(type));
                if (type.isExpert()) {
                    buf.append("</i>");
                }
                buf.append("</dt><dd style=\"padding-bottom:10px\">");
                buf.append(" ");
                buf.append(type.getDescription() + "<br/><font color=\"#777777\" size=\"-2\">");
                if (type.getDefaultValue() != null) {
                    if (!type.toString(type.getDefaultValue()).equals("")) {
                        buf.append(" Default value: ");
                        buf.append(type.toString(type.getDefaultValue()));
                        buf.append("<br/>");
                    }
                }
                if (type.isExpert()) {
                    buf.append("Expert parameter<br/>");
                }
                if (type.getDependencyConditions().size() > 0) {
                    buf.append("Depends on:<ul class=\"param_dep\">");
                    for (ParameterCondition condition : type.getDependencyConditions()) {
                        buf.append("<li>");
                        buf.append(condition.toString());
                        buf.append("</li>");
                    }
                    buf.append("</ul>");
                }
                buf.append("</small></dd>");
            }
            buf.append("</dl>");
        }
        if (!descr.getOperatorDocumentation().getExamples().isEmpty()) {
            buf.append("<h4>Examples</h4><ul>");
            int i = 0;
            for (ExampleProcess exampleProcess : descr.getOperatorDocumentation().getExamples()) {
                buf.append("<li>");
                buf.append(exampleProcess.getComment());
                buf.append(makeExampleFooter(i));
                buf.append("</li>");
                i++;
            }
            buf.append("</ul>");
        }
        buf.append("</html>");
        return buf.toString();
    }

    private static Object makeExampleFooter(int exampleIndex) {
        return "<br/><a href=\"show_example_" + exampleIndex + "\">Show example process</a>.";
    }

    private static void appendPortsToDocumentation(Ports<? extends Port> ports, String title, String ulClass, StringBuilder buf) {
        if (ports.getNumberOfPorts() > 0) {
            buf.append("<h4>" + title + "</h4><ul class=\"ports\">");
            for (Port port : ports.getAllPorts()) {
                if (ulClass != null) buf.append("<li class=\"" + ulClass + "\"><strong>"); else buf.append("<li><strong>");
                buf.append(port.getName());
                buf.append("</strong>");
                if (port.getDescription() != null && port.getDescription().length() > 0) {
                    buf.append(": ");
                    buf.append(port.getDescription());
                }
                buf.append("</li>");
            }
            buf.append("</ul><br/>");
        }
    }

    private static String makeParameterHeader(ParameterType type) {
        return type.getKey().replace('_', ' ');
    }
}
