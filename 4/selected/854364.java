package org.lindenb.mwrdf;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.text.PlainDocument;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.lang.StringEscapeUtils;
import org.lindenb.io.IOUtils;
import org.lindenb.lang.InvalidXMLException;
import org.lindenb.lang.ThrowablePane;
import org.lindenb.me.Me;
import org.lindenb.sw.PrefixMapping;
import org.lindenb.sw.dom.DOM4RDF;
import org.lindenb.sw.nodes.Statement;
import org.lindenb.sw.nodes.StmtSet;
import org.lindenb.sw.vocabulary.RDF;
import org.lindenb.swing.DocumentAdapter;
import org.lindenb.util.Digest;
import org.lindenb.xml.XMLUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * 
 * MWRdfEditor
 *
 */
public class MWRdfEditor extends JApplet {

    private static final long serialVersionUID = 1L;

    protected static final String ACTION_POST = "action.article.post";

    protected static final String ACTION_ABOUT_ME = "action.about.me";

    protected static final String ACTION_INFO_SYS = "action.info.sys";

    private ActionMap actionMap = new ActionMap();

    private JTextField infoBoxField;

    private JTextArea textArea;

    private DocumentBuilder docBuilder;

    private SAXParser saxParser;

    private Schema schema = null;

    private JCheckBox cBoxValidateRDF;

    private JCheckBox cBoxValidateSchema;

    public static final String PARAM_USERNAME = "userName";

    public static final String PARAM_USERID = "userId";

    public static final String PARAM_SESSION = "_session";

    public static final String PARAM_WIKIPREFIX = "cookiePrefix";

    public static final String PARAM_TITLE = "title";

    private static final String START_TAGS = "<div id=\'rdf-content\'><pre><nowiki>";

    private static final String END_TAGS = "</nowiki></pre></div>";

    /** httpClient */
    private HttpClient httpClient = null;

    /** An action inserting a namespace */
    private class NSInsertAction extends org.lindenb.swing.ActionAdapter {

        private static final long serialVersionUID = 1L;

        NSInsertAction(String prefix, String ns) {
            super(prefix, prefix, ns);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            MWRdfEditor.this.textArea.insert(" xmlns:" + getObject(0) + "=\"" + getObject(1) + "\" ", MWRdfEditor.this.textArea.getCaretPosition());
        }
    }

    /** GetRevisionHandler */
    private static class GetRevisionHandler extends DefaultHandler {

        private StringBuilder sb = null;

        private String content = null;

        GetRevisionHandler() {
        }

        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
            if (this.content == null && name.equals("rev")) {
                this.sb = new StringBuilder();
            }
        }

        @Override
        public void endElement(String uri, String localName, String name) throws SAXException {
            if (sb != null) {
                this.content = sb.toString();
                this.sb = null;
            }
        }

        public String getContent() {
            return this.content;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (sb != null) {
                sb.append(ch, start, length);
            }
        }
    }

    /** constructor */
    public MWRdfEditor() {
    }

    /** anwser the http client */
    public HttpClient getHttpClient() {
        return httpClient;
    }

    /** get cookiePrefix */
    protected String getWikiPrefix() {
        return getNonNullParameter(PARAM_WIKIPREFIX);
    }

    /** anwser _session */
    protected String getSession() {
        return getNonNullParameter(PARAM_SESSION);
    }

    public Schema getSchema() {
        return schema;
    }

    /** answer user name */
    protected String getUserName() {
        return getNonNullParameter(PARAM_USERNAME);
    }

    protected String getParameter(String key, String defaultValue) {
        String s = getParameter(key);
        return s == null ? defaultValue : s;
    }

    protected String getNonNullParameter(String key) {
        return getParameter(key, "");
    }

    protected ActionMap getActionMap() {
        return actionMap;
    }

    /** answer user ID */
    protected String getUserId() {
        return getNonNullParameter(PARAM_USERID);
    }

    /** answer page title */
    protected String getPageTitle() {
        return getNonNullParameter(PARAM_TITLE);
    }

    private String getMediaWikiApiUrl() {
        String path = getDocumentBase().toExternalForm();
        int i = path.lastIndexOf('?');
        if (i != -1) path = path.substring(0, i);
        i = path.lastIndexOf('/');
        if (i == -1) return path;
        return path.substring(0, i) + "/api.php";
    }

    private String getSchemaUrl() {
        String path = getDocumentBase().toExternalForm();
        int i = path.lastIndexOf('?');
        if (i != -1) path = path.substring(0, i);
        i = path.lastIndexOf('/');
        if (i == -1) return path;
        return path.substring(0, i) + "/mwrdf/schema.rdf";
    }

    public String getRevisionContent() throws IOException {
        NameValuePair params[] = new NameValuePair[] { new NameValuePair("action", "query"), new NameValuePair("format", "xml"), new NameValuePair("revids", getNonNullParameter("revId")), new NameValuePair("prop", "revisions"), new NameValuePair("rvprop", "content|ids") };
        GetMethod method = new GetMethod(getMediaWikiApiUrl());
        method.setQueryString(params);
        InputStream in = null;
        try {
            GetRevisionHandler h = new GetRevisionHandler();
            getHttpClient().executeMethod(method);
            in = method.getResponseBodyAsStream();
            this.saxParser.parse(in, h);
            if (h.getContent() == null) {
                return "";
            }
            return h.getContent();
        } catch (IOException e1) {
            throw e1;
        } catch (SAXException e1) {
            throw new IOException(e1);
        } finally {
            if (in != null) in.close();
            if (method != null) method.releaseConnection();
        }
    }

    @Override
    public void init() {
        try {
            this.httpClient = new HttpClient();
            this.httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(10000);
            SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
            saxParserFactory.setNamespaceAware(false);
            saxParserFactory.setValidating(false);
            this.saxParser = saxParserFactory.newSAXParser();
            DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
            domFactory.setCoalescing(true);
            domFactory.setExpandEntityReferences(true);
            domFactory.setIgnoringComments(true);
            domFactory.setNamespaceAware(true);
            domFactory.setValidating(false);
            domFactory.setIgnoringElementContentWhitespace(false);
            this.docBuilder = domFactory.newDocumentBuilder();
            this.docBuilder.setErrorHandler(new ErrorHandler() {

                @Override
                public void error(SAXParseException exception) {
                }

                @Override
                public void fatalError(SAXParseException err) throws SAXException {
                    throw err;
                }

                @Override
                public void warning(SAXParseException err) throws SAXException {
                    throw err;
                }
            });
            Document schemaAsDom = this.docBuilder.parse(getSchemaUrl());
            this.schema = new Schema(DOM4RDF.getStatements(schemaAsDom), new PrefixMapping(schemaAsDom));
            JMenuBar bar = new JMenuBar();
            JMenu menu = new JMenu("Applet");
            bar.add(menu);
            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.setBorder(new CompoundBorder(new LineBorder(Color.GRAY, 1), new EmptyBorder(5, 5, 5, 5)));
            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
            JLabel label = new JLabel(getPageTitle());
            label.setToolTipText(getPageTitle());
            top.add(label);
            contentPanel.add(top, BorderLayout.NORTH);
            AbstractAction action = new AbstractAction("About me") {

                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    showAboutMe();
                }
            };
            action.putValue(AbstractAction.SHORT_DESCRIPTION, "About me");
            action.putValue(AbstractAction.LONG_DESCRIPTION, "About me");
            getActionMap().put(ACTION_ABOUT_ME, action);
            menu.add(action);
            action = new AbstractAction("System Info") {

                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    showSysInfo();
                }
            };
            action.putValue(AbstractAction.SHORT_DESCRIPTION, "Show System Info");
            action.putValue(AbstractAction.LONG_DESCRIPTION, "Show System Info");
            getActionMap().put(ACTION_INFO_SYS, action);
            menu.add(action);
            menu.add(new JSeparator());
            JPanel bot = new JPanel(new FlowLayout(FlowLayout.LEADING));
            contentPanel.add(bot, BorderLayout.SOUTH);
            this.infoBoxField = new JTextField(50);
            this.infoBoxField.setEditable(false);
            bot.add(this.infoBoxField);
            action = new AbstractAction("Post") {

                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    postArticle();
                }
            };
            action.putValue(AbstractAction.SHORT_DESCRIPTION, "Post");
            action.putValue(AbstractAction.LONG_DESCRIPTION, "Post");
            getActionMap().put(ACTION_POST, action);
            bot.add(new JButton(action));
            action.setEnabled(false);
            menu.add(action);
            PlainDocument doc = new PlainDocument();
            this.textArea = new JTextArea(doc);
            this.textArea.setFont(new Font("Courier", Font.PLAIN, 12));
            contentPanel.add(new JScrollPane(this.textArea), BorderLayout.CENTER);
            String content = getRevisionContent();
            int i = content.indexOf(START_TAGS);
            if (content.length() == 0) {
                content = getSchema().createEmptyRDFDocument();
            } else if (i != -1) {
                i += START_TAGS.length();
                int j = content.indexOf(END_TAGS, i);
                if (j != -1) {
                    content = StringEscapeUtils.unescapeXml(content.substring(i, j));
                } else {
                    content = "CANNOT GET RDF IN :\n" + content;
                }
            } else {
                content = "CANNOT GET RDF IN :\n" + content;
            }
            this.textArea.setText(content);
            menu.add(new AbstractAction("Insert RDF") {

                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    textArea.insert(schema.createEmptyRDFDocument(), 0);
                }
            });
            menu.add(new AbstractAction("View Schema") {

                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent ae) {
                    StringBuilder b = new StringBuilder();
                    for (Statement stmt : MWRdfEditor.this.getSchema().getOntology()) {
                        b.append(stmt.asN3()).append("\n");
                    }
                    JScrollPane scroll = new JScrollPane(new JTextArea(b.toString()));
                    scroll.setPreferredSize(MWRdfEditor.this.getSize());
                    JOptionPane.showMessageDialog(MWRdfEditor.this, scroll);
                }
            });
            action = new AbstractAction("revalidate") {

                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    validateRDF();
                }
            };
            menu.add(this.cBoxValidateRDF = new JCheckBox("Validate RDF"));
            this.cBoxValidateRDF.setSelected(true);
            this.cBoxValidateRDF.addActionListener(action);
            menu.add(this.cBoxValidateSchema = new JCheckBox("Validate Schema"));
            this.cBoxValidateSchema.setSelected(true);
            this.cBoxValidateSchema.addActionListener(action);
            menu = new JMenu("Namespaces");
            bar.add(menu);
            for (String p : this.getSchema().getPrefixMapping().getPrefixes()) {
                menu.add(new NSInsertAction(p, this.getSchema().getPrefixMapping().getNsPrefixURI(p)));
            }
            doc.addDocumentListener(new DocumentAdapter() {

                @Override
                public void documentChanged(DocumentEvent e) {
                    validateRDF();
                }
            });
            setContentPane(contentPanel);
            setJMenuBar(bar);
        } catch (Exception error) {
            setContentPane(new ThrowablePane(error, error.getMessage()));
        }
    }

    public String getArticleContent() throws SAXException, IOException {
        Document dom = getDocument();
        if (dom == null) throw new SAXException("No document");
        StringBuilder article = new StringBuilder();
        article.append(START_TAGS);
        article.append(StringEscapeUtils.escapeXml(this.textArea.getText()));
        article.append(END_TAGS);
        article.append("\n[[Category:RDF doc]]");
        for (String cat : getSchema().getCategories()) {
            article.append("\n[[Category:" + cat + "]]");
        }
        return article.toString();
    }

    private Document getDocument() throws SAXException, IOException {
        return this.docBuilder.parse(new InputSource(new StringReader(this.textArea.getText())));
    }

    private void validateRDF() {
        try {
            Document dom = getDocument();
            Element root = dom.getDocumentElement();
            if (root == null) throw new InvalidXMLException(dom, "No root");
            if (this.cBoxValidateRDF.isSelected()) {
                if (!XMLUtilities.isA(root, RDF.NS, "RDF")) throw new InvalidXMLException(dom, "Not a RDF root");
                if (XMLUtilities.count(root) != 1) throw new InvalidXMLException(root, "Expected one and only one element under rdf:RDF");
                StmtSet stmts = DOM4RDF.getStatements(dom);
                if (this.cBoxValidateSchema.isSelected()) {
                    this.getSchema().validate(stmts);
                }
            }
            getActionMap().get(ACTION_POST).setEnabled(true);
            this.infoBoxField.setText("OK");
            this.infoBoxField.setForeground(Color.GREEN);
        } catch (Exception err) {
            String msg = err.getMessage();
            if (msg == null) msg = err.getClass().getName();
            getActionMap().get(ACTION_POST).setEnabled(false);
            this.infoBoxField.setText(msg);
            this.infoBoxField.setForeground(Color.RED);
        } finally {
            this.infoBoxField.setCaretPosition(0);
        }
    }

    public String getSummary() {
        return "" + new Date();
    }

    public void showAboutMe() {
        JOptionPane.showMessageDialog(null, Me.FIRST_NAME + " " + Me.LAST_NAME + " " + Me.MAIL, "About me", JOptionPane.PLAIN_MESSAGE);
    }

    public void showSysInfo() {
        StringBuilder b = new StringBuilder();
        b.append("username " + this.getUserName() + "\n");
        b.append("userId " + this.getUserId() + "\n");
        b.append("_session " + getSession() + "\n");
        b.append("wikiPrefix " + getWikiPrefix() + "\n");
        b.append("base " + this.getCodeBase() + "\n");
        b.append("base.path " + this.getDocumentBase().getPath() + "\n");
        b.append("base.file " + this.getDocumentBase().getFile() + "\n");
        b.append("base.host " + this.getDocumentBase().getHost() + "\n");
        b.append("base.userinfo " + this.getDocumentBase().getUserInfo() + "\n");
        b.append("base.authority " + this.getDocumentBase().getAuthority() + "\n");
        b.append("base.protocol " + this.getDocumentBase().getProtocol() + "\n");
        b.append("base.port " + this.getDocumentBase().getPort() + "\n");
        b.append("base.query " + this.getDocumentBase().getQuery() + "\n");
        b.append("base.ref " + this.getDocumentBase().getRef() + "\n");
        b.append("docbase " + this.getDocumentBase() + "\n");
        b.append("java " + System.getProperty("java.version", "?") + "\n");
        b.append("java.vm " + System.getProperty("java.vm.version", "?") + "\n");
        for (String s : new String[] { "wpSection", "wpEdittime", "wpScrolltop", "wpStarttime", "wpEditToken", "wpTextbox1" }) {
            b.append(s + " " + getNonNullParameter(s) + "\n");
        }
        JTextArea textArea = new JTextArea(b.toString());
        textArea.setPreferredSize(this.getSize());
        JOptionPane.showMessageDialog(null, new JScrollPane(textArea), "System", JOptionPane.PLAIN_MESSAGE);
    }

    public void postArticle() {
        try {
            sendData("wpSave", "Save page");
            String base = getDocumentBase().toExternalForm();
            final String editToken = "&action=edit";
            int i = base.indexOf(editToken);
            if (i != -1) {
                base = base.substring(0, i) + ((i + editToken.length()) < base.length() ? base.substring(i + editToken.length()) : "");
            }
            getAppletContext().showDocument(new URL(base));
        } catch (Exception err) {
        }
    }

    protected int sendData(String submitName, String submitValue) throws HttpException, IOException, SAXException {
        PostMethod postMethod = null;
        try {
            postMethod = new PostMethod(getDocumentBase().toString());
            postMethod.getParams().setCookiePolicy(org.apache.commons.httpclient.cookie.CookiePolicy.IGNORE_COOKIES);
            postMethod.addRequestHeader("Cookie", getWikiPrefix() + "_session=" + getSession() + "; " + getWikiPrefix() + "UserID=" + getUserId() + "; " + getWikiPrefix() + "UserName=" + getUserName() + "; ");
            List<Part> parts = new ArrayList<Part>();
            for (String s : new String[] { "wpSection", "wpEdittime", "wpScrolltop", "wpStarttime", "wpEditToken" }) {
                parts.add(new StringPart(s, StringEscapeUtils.unescapeJava(getNonNullParameter(s))));
            }
            parts.add(new StringPart("action", "edit"));
            parts.add(new StringPart("wpTextbox1", getArticleContent()));
            parts.add(new StringPart("wpSummary", getSummary()));
            parts.add(new StringPart("wpAutoSummary", Digest.MD5.isImplemented() ? Digest.MD5.encrypt(getSummary()) : ""));
            parts.add(new StringPart(submitName, submitValue));
            MultipartRequestEntity requestEntity = new MultipartRequestEntity(parts.toArray(new Part[parts.size()]), postMethod.getParams());
            postMethod.setRequestEntity(requestEntity);
            int status = getHttpClient().executeMethod(postMethod);
            IOUtils.copyTo(postMethod.getResponseBodyAsStream(), System.err);
            return status;
        } catch (HttpException err) {
            throw err;
        } catch (IOException err) {
            throw err;
        } finally {
            if (postMethod != null) postMethod.releaseConnection();
        }
    }
}
