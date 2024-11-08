package writer2latex.xhtml;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Iterator;
import java.io.InputStream;
import java.io.IOException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import writer2latex.api.Config;
import writer2latex.api.ContentEntry;
import writer2latex.api.ConverterFactory;
import writer2latex.api.OutputFile;
import writer2latex.base.ContentEntryImpl;
import writer2latex.base.ConverterBase;
import writer2latex.office.MIMETypes;
import writer2latex.office.OfficeReader;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.ExportNameCollection;
import writer2latex.util.Misc;
import writer2latex.xhtml.l10n.L10n;

/**
 * <p>This class converts an OpenDocument file to an XHTML(+MathML) or EPUB document.</p>
 *
 */
public class Converter extends ConverterBase {

    private static final String EPUB_STYLES_FOLDER = "styles/";

    private static final String EPUB_STYLESHEET = "styles/styles1.css";

    private static final String EPUB_CUSTOM_STYLESHEET = "styles/styles.css";

    private XhtmlConfig config;

    public Config getConfig() {
        return config;
    }

    protected XhtmlConfig getXhtmlConfig() {
        return config;
    }

    private L10n l10n;

    private StyleConverter styleCv;

    private TextConverter textCv;

    private TableConverter tableCv;

    private DrawConverter drawCv;

    private MathConverter mathCv;

    private XhtmlDocument template = null;

    private CssDocument styleSheet = null;

    private Set<ResourceDocument> resources = new HashSet<ResourceDocument>();

    protected int nType = XhtmlDocument.XHTML10;

    private boolean bOPS = false;

    Vector<XhtmlDocument> outFiles;

    private int nOutFileIndex;

    private XhtmlDocument htmlDoc;

    private Document htmlDOM;

    private boolean bNeedHeaderFooter = false;

    private int nTocFileIndex = -1;

    private int nAlphabeticalIndex = -1;

    Hashtable<String, Integer> targets = new Hashtable<String, Integer>();

    LinkedList<LinkDescriptor> links = new LinkedList<LinkDescriptor>();

    private ExportNameCollection targetNames = new ExportNameCollection(true);

    private Stack<String> contentWidth = new Stack<String>();

    public Converter(int nType) {
        super();
        config = new XhtmlConfig();
        this.nType = nType;
    }

    @Override
    public void readTemplate(InputStream is) throws IOException {
        template = new XhtmlDocument("Template", nType);
        template.read(is);
    }

    @Override
    public void readTemplate(File file) throws IOException {
        readTemplate(new FileInputStream(file));
    }

    @Override
    public void readStyleSheet(InputStream is) throws IOException {
        if (styleSheet == null) {
            styleSheet = new CssDocument(EPUB_CUSTOM_STYLESHEET);
        }
        styleSheet.read(is);
    }

    @Override
    public void readStyleSheet(File file) throws IOException {
        readStyleSheet(new FileInputStream(file));
    }

    @Override
    public void readResource(InputStream is, String sFileName, String sMediaType) throws IOException {
        if (sMediaType == null) {
            sMediaType = "";
            String sfilename = sFileName.toLowerCase();
            if (sfilename.endsWith(MIMETypes.PNG_EXT)) {
                sMediaType = MIMETypes.PNG;
            } else if (sfilename.endsWith(MIMETypes.JPEG_EXT)) {
                sMediaType = MIMETypes.JPEG;
            } else if (sfilename.endsWith(".jpeg")) {
                sMediaType = MIMETypes.JPEG;
            } else if (sfilename.endsWith(MIMETypes.GIF_EXT)) {
                sMediaType = MIMETypes.GIF;
            } else if (sfilename.endsWith(".otf")) {
                sMediaType = "application/vnd.ms-opentype";
            } else if (sfilename.endsWith(".ttf")) {
                sMediaType = "application/x-font-ttf";
            }
        }
        ResourceDocument doc = new ResourceDocument(EPUB_STYLES_FOLDER + sFileName, sMediaType);
        doc.read(is);
        resources.add(doc);
    }

    @Override
    public void readResource(File file, String sFileName, String sMediaType) throws IOException {
        readResource(new FileInputStream(file), sFileName, sMediaType);
    }

    protected String getContentWidth() {
        return contentWidth.peek();
    }

    protected String pushContentWidth(String sWidth) {
        return contentWidth.push(sWidth);
    }

    protected void popContentWidth() {
        contentWidth.pop();
    }

    protected boolean isTopLevel() {
        return contentWidth.size() == 1;
    }

    protected StyleConverter getStyleCv() {
        return styleCv;
    }

    protected TextConverter getTextCv() {
        return textCv;
    }

    protected TableConverter getTableCv() {
        return tableCv;
    }

    protected DrawConverter getDrawCv() {
        return drawCv;
    }

    protected MathConverter getMathCv() {
        return mathCv;
    }

    protected int getType() {
        return nType;
    }

    protected int getOutFileIndex() {
        return nOutFileIndex;
    }

    protected void addContentEntry(String sTitle, int nLevel, String sTarget) {
        converterResult.addContentEntry(new ContentEntryImpl(sTitle, nLevel, htmlDoc, sTarget));
    }

    protected void setTocFile(String sTarget) {
        converterResult.setTocFile(new ContentEntryImpl(l10n.get(L10n.CONTENTS), 1, htmlDoc, sTarget));
        nTocFileIndex = nOutFileIndex;
    }

    protected void setLofFile(String sTarget) {
        converterResult.setLofFile(new ContentEntryImpl("Figures", 1, htmlDoc, sTarget));
    }

    protected void setLotFile(String sTarget) {
        converterResult.setLotFile(new ContentEntryImpl("Tables", 1, htmlDoc, sTarget));
    }

    protected void setIndexFile(String sTarget) {
        converterResult.setIndexFile(new ContentEntryImpl(l10n.get(L10n.INDEX), 1, htmlDoc, sTarget));
        nAlphabeticalIndex = nOutFileIndex;
    }

    protected void setCoverFile(String sTarget) {
        converterResult.setCoverFile(new ContentEntryImpl("Cover", 0, htmlDoc, sTarget));
    }

    protected void setCoverImageFile(OutputFile file, String sTarget) {
        converterResult.setCoverImageFile(new ContentEntryImpl("Cover image", 0, file, sTarget));
    }

    protected Element createElement(String s) {
        return htmlDOM.createElement(s);
    }

    protected Text createTextNode(String s) {
        return htmlDOM.createTextNode(s);
    }

    protected Node importNode(Node node, boolean bDeep) {
        return htmlDOM.importNode(node, bDeep);
    }

    protected L10n getL10n() {
        return l10n;
    }

    public void setOPS(boolean b) {
        bOPS = true;
    }

    public boolean isOPS() {
        return bOPS;
    }

    @Override
    public void convertInner() throws IOException {
        sTargetFileName = Misc.trimDocumentName(sTargetFileName, XhtmlDocument.getExtension(nType));
        outFiles = new Vector<XhtmlDocument>();
        nOutFileIndex = -1;
        bNeedHeaderFooter = !bOPS && (ofr.isSpreadsheet() || ofr.isPresentation() || config.getXhtmlSplitLevel() > 0 || config.pageBreakSplit() > XhtmlConfig.NONE || config.getXhtmlUplink().length() > 0);
        l10n = new L10n();
        if (isOPS()) {
            imageLoader.setBaseFileName("image");
            imageLoader.setUseSubdir("images");
        } else {
            imageLoader.setBaseFileName(sTargetFileName + "-img");
            if (config.saveImagesInSubdir()) {
                imageLoader.setUseSubdir(sTargetFileName + "-img");
            }
        }
        imageLoader.setDefaultFormat(MIMETypes.PNG);
        imageLoader.addAcceptedFormat(MIMETypes.JPEG);
        imageLoader.addAcceptedFormat(MIMETypes.GIF);
        if (nType == XhtmlDocument.HTML5 && config.useSVG()) {
            imageLoader.setDefaultVectorFormat(MIMETypes.SVG);
        }
        styleCv = new StyleConverter(ofr, config, this, nType);
        textCv = new TextConverter(ofr, config, this);
        tableCv = new TableConverter(ofr, config, this);
        drawCv = new DrawConverter(ofr, config, this);
        mathCv = new MathConverter(ofr, config, this, nType != XhtmlDocument.XHTML10 && nType != XhtmlDocument.XHTML11);
        StyleWithProperties style = ofr.isSpreadsheet() ? ofr.getDefaultCellStyle() : ofr.getDefaultParStyle();
        if (style != null) {
            if ("fa".equals(style.getProperty(XMLString.STYLE_LANGUAGE_COMPLEX))) {
                l10n.setLocale("fa", "IR");
            } else {
                l10n.setLocale(style.getProperty(XMLString.FO_LANGUAGE), style.getProperty(XMLString.FO_COUNTRY));
            }
        }
        pushContentWidth(getStyleCv().getPageSc().getTextWidth());
        Element body = ofr.getContent();
        if (ofr.isSpreadsheet()) {
            tableCv.convertTableContent(body);
        } else if (ofr.isPresentation()) {
            drawCv.convertDrawContent(body);
        } else {
            textCv.convertTextContent(body);
        }
        if (converterResult.getContent().isEmpty()) {
            converterResult.setTextFile(new ContentEntryImpl("Text", 1, outFiles.get(0), null));
            converterResult.addContentEntry(new ContentEntryImpl("Text", 1, outFiles.get(0), null));
        } else {
            ContentEntry firstHeading = converterResult.getContent().get(0);
            int nFirstPage = converterResult.getCoverFile() != null ? 1 : 0;
            if (outFiles.get(nFirstPage) != firstHeading.getFile() || firstHeading.getTarget() != null) {
                converterResult.setTitlePageFile(new ContentEntryImpl("Title page", 1, outFiles.get(nFirstPage), null));
            }
            converterResult.setTextFile(new ContentEntryImpl("Text", 1, firstHeading.getFile(), firstHeading.getTarget()));
        }
        ListIterator<LinkDescriptor> iter = links.listIterator();
        while (iter.hasNext()) {
            LinkDescriptor ld = iter.next();
            Integer targetIndex = targets.get(ld.sId);
            if (targetIndex != null) {
                int nTargetIndex = targetIndex.intValue();
                if (nTargetIndex == ld.nIndex) {
                    ld.element.setAttribute("href", "#" + targetNames.getExportName(ld.sId));
                } else {
                    ld.element.setAttribute("href", getOutFileName(nTargetIndex, true) + "#" + targetNames.getExportName(ld.sId));
                }
            }
        }
        if (bOPS && styleSheet != null) {
            converterResult.addDocument(styleSheet);
            for (ResourceDocument doc : resources) {
                converterResult.addDocument(doc);
            }
        }
        if (!isOPS() && !config.separateStylesheet()) {
            for (int i = 0; i <= nOutFileIndex; i++) {
                Element head = outFiles.get(i).getHeadNode();
                if (head != null) {
                    Node styles = styleCv.exportStyles(outFiles.get(i).getContentDOM());
                    if (styles != null) {
                        head.appendChild(styles);
                    }
                }
            }
        }
        if (nType == XhtmlDocument.HTML5 && config.useMathJax()) {
            for (int i = 0; i <= nOutFileIndex; i++) {
                if (outFiles.get(i).hasMath()) {
                    XhtmlDocument doc = outFiles.get(i);
                    Element head = doc.getHeadNode();
                    if (head != null) {
                        Element script = doc.getContentDOM().createElement("script");
                        head.appendChild(script);
                        script.setAttribute("type", "text/javascript");
                        script.setAttribute("src", "http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=MML_HTMLorMML");
                    }
                }
            }
        }
        if (ofr.isSpreadsheet()) {
            for (int i = 0; i <= nOutFileIndex; i++) {
                XhtmlDocument doc = outFiles.get(i);
                Document dom = doc.getContentDOM();
                Element header = doc.getHeaderNode();
                Element footer = doc.getFooterNode();
                Element headerPar = dom.createElement("p");
                Element footerPar = dom.createElement("p");
                footerPar.setAttribute("style", "clear:both");
                if (config.getXhtmlUplink().length() > 0) {
                    Element a = dom.createElement("a");
                    a.setAttribute("href", config.getXhtmlUplink());
                    a.appendChild(dom.createTextNode(l10n.get(L10n.UP)));
                    headerPar.appendChild(a);
                    headerPar.appendChild(dom.createTextNode(" "));
                    a = dom.createElement("a");
                    a.setAttribute("href", config.getXhtmlUplink());
                    a.appendChild(dom.createTextNode(l10n.get(L10n.UP)));
                    footerPar.appendChild(a);
                    footerPar.appendChild(dom.createTextNode(" "));
                }
                int nSheets = tableCv.sheetNames.size();
                for (int j = 0; j < nSheets; j++) {
                    if (config.xhtmlCalcSplit()) {
                        addNavigationLink(dom, headerPar, tableCv.sheetNames.get(j), j);
                        addNavigationLink(dom, footerPar, tableCv.sheetNames.get(j), j);
                    } else {
                        addInternalNavigationLink(dom, headerPar, tableCv.sheetNames.get(j), "tableheading" + j);
                        addInternalNavigationLink(dom, footerPar, tableCv.sheetNames.get(j), "tableheading" + j);
                    }
                }
                if (header != null) {
                    header.appendChild(headerPar);
                }
                if (footer != null) {
                    footer.appendChild(footerPar);
                }
            }
        } else if (nOutFileIndex > 0) {
            for (int i = 0; i <= nOutFileIndex; i++) {
                XhtmlDocument doc = outFiles.get(i);
                Document dom = doc.getContentDOM();
                Element header = doc.getHeaderNode();
                if (header != null) {
                    if (ofr.isPresentation()) {
                        header.setAttribute("style", "position:absolute;top:0;left:0");
                    }
                    if (config.getXhtmlUplink().length() > 0) {
                        Element a = dom.createElement("a");
                        a.setAttribute("href", config.getXhtmlUplink());
                        a.appendChild(dom.createTextNode(l10n.get(L10n.UP)));
                        header.appendChild(a);
                        header.appendChild(dom.createTextNode(" "));
                    }
                    addNavigationLink(dom, header, l10n.get(L10n.FIRST), 0);
                    addNavigationLink(dom, header, l10n.get(L10n.PREVIOUS), i - 1);
                    addNavigationLink(dom, header, l10n.get(L10n.NEXT), i + 1);
                    addNavigationLink(dom, header, l10n.get(L10n.LAST), nOutFileIndex);
                    if (textCv.getTocIndex() >= 0) {
                        addNavigationLink(dom, header, l10n.get(L10n.CONTENTS), textCv.getTocIndex());
                    }
                    if (textCv.getAlphabeticalIndex() >= 0) {
                        addNavigationLink(dom, header, l10n.get(L10n.INDEX), textCv.getAlphabeticalIndex());
                    }
                }
                Element footer = doc.getFooterNode();
                if (footer != null && !ofr.isPresentation()) {
                    if (config.getXhtmlUplink().length() > 0) {
                        Element a = dom.createElement("a");
                        a.setAttribute("href", config.getXhtmlUplink());
                        a.appendChild(dom.createTextNode(l10n.get(L10n.UP)));
                        footer.appendChild(a);
                        footer.appendChild(dom.createTextNode(" "));
                    }
                    addNavigationLink(dom, footer, l10n.get(L10n.FIRST), 0);
                    addNavigationLink(dom, footer, l10n.get(L10n.PREVIOUS), i - 1);
                    addNavigationLink(dom, footer, l10n.get(L10n.NEXT), i + 1);
                    addNavigationLink(dom, footer, l10n.get(L10n.LAST), nOutFileIndex);
                    if (textCv.getTocIndex() >= 0) {
                        addNavigationLink(dom, footer, l10n.get(L10n.CONTENTS), textCv.getTocIndex());
                    }
                    if (textCv.getAlphabeticalIndex() >= 0) {
                        addNavigationLink(dom, footer, l10n.get(L10n.INDEX), textCv.getAlphabeticalIndex());
                    }
                }
            }
        } else if (config.getXhtmlUplink().length() > 0) {
            for (int i = 0; i <= nOutFileIndex; i++) {
                XhtmlDocument doc = outFiles.get(i);
                Document dom = doc.getContentDOM();
                Element header = doc.getHeaderNode();
                if (header != null) {
                    Element a = dom.createElement("a");
                    a.setAttribute("href", config.getXhtmlUplink());
                    a.appendChild(dom.createTextNode(l10n.get(L10n.UP)));
                    header.appendChild(a);
                    header.appendChild(dom.createTextNode(" "));
                }
                Element footer = doc.getFooterNode();
                if (footer != null) {
                    Element a = dom.createElement("a");
                    a.setAttribute("href", config.getXhtmlUplink());
                    a.appendChild(dom.createTextNode(l10n.get(L10n.UP)));
                    footer.appendChild(a);
                    footer.appendChild(dom.createTextNode(" "));
                }
            }
        }
        if (config.xhtmlFormatting() > XhtmlConfig.IGNORE_STYLES) {
            if (isOPS()) {
                CssDocument cssDoc = new CssDocument(EPUB_STYLESHEET);
                cssDoc.read(styleCv.exportStyles(false));
                converterResult.addDocument(cssDoc);
            } else if (config.separateStylesheet()) {
                CssDocument cssDoc = new CssDocument(sTargetFileName + "-styles.css");
                cssDoc.read(styleCv.exportStyles(false));
                converterResult.addDocument(cssDoc);
            }
        }
    }

    private void addNavigationLink(Document dom, Node node, String s, int nIndex) {
        if (nIndex >= 0 && nIndex <= nOutFileIndex) {
            Element a = dom.createElement("a");
            a.setAttribute("href", Misc.makeHref(getOutFileName(nIndex, true)));
            a.appendChild(dom.createTextNode(s));
            node.appendChild(a);
            node.appendChild(dom.createTextNode(" "));
        } else {
            Element span = dom.createElement("span");
            span.setAttribute("class", "nolink");
            node.appendChild(span);
            span.appendChild(dom.createTextNode(s));
            node.appendChild(dom.createTextNode(" "));
        }
    }

    private void addInternalNavigationLink(Document dom, Node node, String s, String sLink) {
        Element a = dom.createElement("a");
        a.setAttribute("href", "#" + sLink);
        a.appendChild(dom.createTextNode(s));
        node.appendChild(a);
        node.appendChild(dom.createTextNode(" "));
    }

    protected String getPlainInlineText(Node node) {
        StringBuffer buf = new StringBuffer();
        Node child = node.getFirstChild();
        while (child != null) {
            short nodeType = child.getNodeType();
            switch(nodeType) {
                case Node.TEXT_NODE:
                    buf.append(child.getNodeValue());
                    break;
                case Node.ELEMENT_NODE:
                    String sName = child.getNodeName();
                    if (sName.equals(XMLString.TEXT_S)) {
                        buf.append(" ");
                    } else if (sName.equals(XMLString.TEXT_LINE_BREAK) || sName.equals(XMLString.TEXT_TAB_STOP) || sName.equals(XMLString.TEXT_TAB)) {
                        buf.append(" ");
                    } else if (OfficeReader.isNoteElement(child)) {
                    } else if (OfficeReader.isTextElement(child)) {
                        buf.append(getPlainInlineText(child));
                    }
                    break;
                default:
            }
            child = child.getNextSibling();
        }
        return buf.toString();
    }

    public void handleOfficeAnnotation(Node onode, Node hnode) {
        if (config.xhtmlNotes()) {
            StringBuffer buf = new StringBuffer();
            Element creator = null;
            Element date = null;
            Node child = onode.getFirstChild();
            while (child != null) {
                if (Misc.isElement(child, XMLString.TEXT_P)) {
                    if (buf.length() > 0) {
                        buf.append('\n');
                    }
                    buf.append(getPlainInlineText(child));
                } else if (Misc.isElement(child, XMLString.DC_CREATOR)) {
                    creator = (Element) child;
                } else if (Misc.isElement(child, XMLString.DC_DATE)) {
                    date = (Element) child;
                }
                child = child.getNextSibling();
            }
            if (creator != null) {
                if (buf.length() > 0) {
                    buf.append('\n');
                }
                buf.append(getPlainInlineText(creator));
            }
            if (date != null) {
                if (buf.length() > 0) {
                    buf.append('\n');
                }
                buf.append(Misc.formatDate(ofr.getTextContent(date), l10n.getLocale().getLanguage(), l10n.getLocale().getCountry()));
            }
            Node commentNode = htmlDOM.createComment(buf.toString());
            hnode.appendChild(commentNode);
        }
    }

    public String getOutFileName(int nIndex, boolean bWithExt) {
        return sTargetFileName + (nIndex > 0 ? Integer.toString(nIndex) : "") + (bWithExt ? htmlDoc.getFileExtension() : "");
    }

    public boolean outFileHasContent() {
        return htmlDoc.getContentNode().hasChildNodes();
    }

    public void changeOutFile(int nIndex) {
        nOutFileIndex = nIndex;
        htmlDoc = outFiles.get(nIndex);
        htmlDOM = htmlDoc.getContentDOM();
    }

    public Element getPanelNode() {
        return htmlDoc.getPanelNode();
    }

    public Element nextOutFile() {
        htmlDoc = new XhtmlDocument(getOutFileName(++nOutFileIndex, false), nType);
        htmlDoc.setConfig(config);
        if (template != null) {
            htmlDoc.readFromTemplate(template);
        } else if (bNeedHeaderFooter) {
            htmlDoc.createHeaderFooter();
        }
        outFiles.add(nOutFileIndex, htmlDoc);
        converterResult.addDocument(htmlDoc);
        htmlDOM = htmlDoc.getContentDOM();
        Element rootElement = htmlDOM.getDocumentElement();
        styleCv.applyDefaultLanguage(rootElement);
        rootElement.insertBefore(htmlDOM.createComment("This file was converted to xhtml by " + (ofr.isText() ? "Writer" : (ofr.isSpreadsheet() ? "Calc" : "Impress")) + "2xhtml ver. " + ConverterFactory.getVersion() + ". See http://writer2latex.sourceforge.net for more info."), rootElement.getFirstChild());
        if (!ofr.isPresentation()) {
            StyleInfo pageInfo = new StyleInfo();
            styleCv.getPageSc().applyDefaultWritingDirection(pageInfo);
            styleCv.getPageSc().applyStyle(pageInfo, htmlDoc.getContentNode());
        }
        Element title = htmlDoc.getTitleNode();
        if (title != null) {
            String sTitle = metaData.getTitle();
            if (sTitle == null) {
                sTitle = htmlDoc.getFileName();
            }
            title.appendChild(htmlDOM.createTextNode(sTitle));
        }
        Element head = htmlDoc.getHeadNode();
        if (head != null) {
            if (nType == XhtmlDocument.XHTML10) {
                Element meta = htmlDOM.createElement("meta");
                meta.setAttribute("http-equiv", "Content-Type");
                meta.setAttribute("content", "text/html; charset=" + htmlDoc.getEncoding().toLowerCase());
                head.appendChild(meta);
            } else if (nType == XhtmlDocument.HTML5) {
                Element meta = htmlDOM.createElement("meta");
                meta.setAttribute("charset", htmlDoc.getEncoding().toUpperCase());
                head.appendChild(meta);
            }
            if (!bOPS) {
                createMeta(head, "description", metaData.getDescription());
                createMeta(head, "keywords", metaData.getKeywords());
                if (config.xhtmlUseDublinCore()) {
                    head.setAttribute("profile", "http://dublincore.org/documents/2008/08/04/dc-html/");
                    Element dclink = htmlDOM.createElement("link");
                    dclink.setAttribute("rel", "schema.DC");
                    dclink.setAttribute("href", "http://purl.org/dc/elements/1.1/");
                    head.appendChild(dclink);
                    createMeta(head, "DC.title", metaData.getTitle());
                    String sDCSubject = "";
                    if (metaData.getSubject() != null && metaData.getSubject().length() > 0) {
                        sDCSubject = metaData.getSubject();
                    }
                    if (metaData.getKeywords() != null && metaData.getKeywords().length() > 0) {
                        if (sDCSubject.length() > 0) {
                            sDCSubject += ", ";
                        }
                        sDCSubject += metaData.getKeywords();
                    }
                    createMeta(head, "DC.subject", sDCSubject);
                    createMeta(head, "DC.description", metaData.getDescription());
                    createMeta(head, "DC.creator", metaData.getCreator());
                    createMeta(head, "DC.date", metaData.getDate());
                    createMeta(head, "DC.language", metaData.getLanguage());
                }
            }
            if (!bOPS && config.xhtmlCustomStylesheet().length() > 0) {
                Element htmlStyle = htmlDOM.createElement("link");
                htmlStyle.setAttribute("rel", "stylesheet");
                htmlStyle.setAttribute("type", "text/css");
                htmlStyle.setAttribute("media", "all");
                htmlStyle.setAttribute("href", config.xhtmlCustomStylesheet());
                head.appendChild(htmlStyle);
            }
            if (!bOPS && config.separateStylesheet() && config.xhtmlFormatting() > XhtmlConfig.IGNORE_STYLES) {
                Element htmlStyle = htmlDOM.createElement("link");
                htmlStyle.setAttribute("rel", "stylesheet");
                htmlStyle.setAttribute("type", "text/css");
                htmlStyle.setAttribute("media", "all");
                htmlStyle.setAttribute("href", sTargetFileName + "-styles.css");
                head.appendChild(htmlStyle);
            }
            if (bOPS && styleSheet != null) {
                Element sty = htmlDOM.createElement("link");
                sty.setAttribute("rel", "stylesheet");
                sty.setAttribute("type", "text/css");
                sty.setAttribute("href", EPUB_CUSTOM_STYLESHEET);
                head.appendChild(sty);
            }
            if (isOPS() && config.xhtmlFormatting() > XhtmlConfig.IGNORE_STYLES) {
                Element htmlStyle = htmlDOM.createElement("link");
                htmlStyle.setAttribute("rel", "stylesheet");
                htmlStyle.setAttribute("type", "text/css");
                htmlStyle.setAttribute("href", EPUB_STYLESHEET);
                head.appendChild(htmlStyle);
            }
        }
        if (!textCv.sections.isEmpty()) {
            Iterator<Node> iter = textCv.sections.iterator();
            while (iter.hasNext()) {
                Element section = (Element) iter.next();
                String sStyleName = Misc.getAttribute(section, XMLString.TEXT_STYLE_NAME);
                Element div = htmlDOM.createElement("div");
                htmlDoc.getContentNode().appendChild(div);
                htmlDoc.setContentNode(div);
                StyleInfo sectionInfo = new StyleInfo();
                styleCv.getSectionSc().applyStyle(sStyleName, sectionInfo);
                styleCv.getSectionSc().applyStyle(sectionInfo, div);
            }
        }
        return htmlDoc.getContentNode();
    }

    public Element createTarget(String sId) {
        Element a = htmlDOM.createElement("a");
        a.setAttribute("id", targetNames.getExportName(sId));
        targets.put(sId, new Integer(nOutFileIndex));
        return a;
    }

    public void addTarget(Element node, String sId) {
        node.setAttribute("id", targetNames.getExportName(sId));
        targets.put(sId, new Integer(nOutFileIndex));
    }

    public Element createLink(String sId) {
        Element a = htmlDOM.createElement("a");
        LinkDescriptor ld = new LinkDescriptor();
        ld.element = a;
        ld.sId = sId;
        ld.nIndex = nOutFileIndex;
        links.add(ld);
        return a;
    }

    public Element createLink(Element onode) {
        String sHref = onode.getAttribute(XMLString.XLINK_HREF);
        Element anchor;
        if (sHref.startsWith("#")) {
            anchor = createLink(sHref.substring(1));
        } else {
            anchor = htmlDOM.createElement("a");
            sHref = ofr.fixRelativeLink(sHref);
            if (sHref.indexOf("?") == -1) {
                int n3F = sHref.indexOf("%3F");
                if (n3F > 0) {
                    sHref = sHref.substring(0, n3F) + "?" + sHref.substring(n3F + 3);
                }
            }
            anchor.setAttribute("href", sHref);
            String sName = Misc.getAttribute(onode, XMLString.OFFICE_NAME);
            if (sName != null) {
                if (sName.indexOf(";") == -1 && sName.indexOf("=") == -1) {
                    anchor.setAttribute("name", sName);
                    anchor.setAttribute("title", sName);
                } else {
                    String[] sElements = sName.split(";");
                    for (String sElement : sElements) {
                        String[] sNameVal = sElement.split("=");
                        if (sNameVal.length >= 2) {
                            anchor.setAttribute(sNameVal[0].trim(), sNameVal[1].trim());
                        }
                    }
                }
            }
            String sTarget = Misc.getAttribute(onode, XMLString.OFFICE_TARGET_FRAME_NAME);
            if (sTarget != null) {
                anchor.setAttribute("target", sTarget);
            }
        }
        String sStyleName = onode.getAttribute(XMLString.TEXT_STYLE_NAME);
        String sVisitedStyleName = onode.getAttribute(XMLString.TEXT_VISITED_STYLE_NAME);
        StyleInfo anchorInfo = new StyleInfo();
        styleCv.getTextSc().applyAnchorStyle(sStyleName, sVisitedStyleName, anchorInfo);
        styleCv.getTextSc().applyStyle(anchorInfo, anchor);
        return anchor;
    }

    private void createMeta(Element head, String sName, String sValue) {
        if (sValue == null) {
            return;
        }
        Element meta = htmlDOM.createElement("meta");
        meta.setAttribute("name", sName);
        meta.setAttribute("content", sValue);
        head.appendChild(meta);
    }
}
