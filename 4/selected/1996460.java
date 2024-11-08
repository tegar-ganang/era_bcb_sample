package org.lindenb.bloggerdf;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.lindenb.io.IOUtils;
import org.lindenb.lang.InvalidXMLException;
import org.lindenb.sw.dom.DOM4RDF;
import org.lindenb.sw.vocabulary.Atom;
import org.lindenb.sw.vocabulary.RDF;
import org.lindenb.util.Base64;
import org.lindenb.util.Compilation;
import org.lindenb.xml.XMLUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class BloggeRDF {

    private static Logger LOG = Logger.getLogger(BloggeRDF.class.getName());

    /** API Version  {@linkplain http://code.google.com/apis/blogger/docs/2.0/developers_guide_protocol.html#Versioning }*/
    private static final int GDataVersion = 2;

    /** DOM Builder */
    private DocumentBuilder domBuilder;

    /** blogger id */
    private BigInteger blogId = null;

    /** httpClient */
    private HttpClient httpClient = null;

    private String userEmail;

    private char userPassword[];

    /** Google Authentication Token */
    private String AuthToken = null;

    /** serialize xml */
    private Transformer xml2ascii;

    private BloggeRDF() throws ParserConfigurationException, TransformerConfigurationException {
        this.httpClient = new HttpClient();
        this.httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(10000);
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setCoalescing(true);
        domFactory.setExpandEntityReferences(true);
        domFactory.setIgnoringComments(true);
        domFactory.setNamespaceAware(true);
        domFactory.setValidating(false);
        domFactory.setNamespaceAware(true);
        domFactory.setIgnoringElementContentWhitespace(true);
        this.domBuilder = domFactory.newDocumentBuilder();
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setAttribute("indent-number", new Integer(4));
        this.xml2ascii = factory.newTransformer();
        this.xml2ascii.setOutputProperty(OutputKeys.INDENT, "yes");
    }

    private HttpClient getHttpClient() {
        return httpClient;
    }

    private void post(Document dom) throws HttpException, IOException, TransformerException, InvalidXMLException {
        Element root = dom.getDocumentElement();
        if (root == null) throw new InvalidXMLException(dom, "No Document Root");
        if (XMLUtilities.isA(root, RDF.NS, "RDF")) throw new InvalidXMLException(root, "Not a rdf:RDF root");
        new DOM4RDF().parse(dom);
        ServiceLoader<RDFTypeHandler> service = ServiceLoader.load(RDFTypeHandler.class);
        for (Node n1 = root.getFirstChild(); n1 != null; n1 = n1.getNextSibling()) {
            if (n1.getNodeType() != Node.ELEMENT_NODE) continue;
            String rdfType = n1.getNamespaceURI() + n1.getLocalName();
            Iterator<RDFTypeHandler> iter = service.iterator();
            RDFTypeHandler handler = null;
            while (iter.hasNext()) {
                handler = iter.next();
                if (handler != null && handler.getNamespaceURI().equals(rdfType)) break;
            }
            if (handler == null) throw new InvalidXMLException(n1, "Cannot handle this type" + rdfType);
            handler.setElement(Element.class.cast(n1));
            String title = handler.getTitle();
            Set<String> tags = handler.getTags();
            post(title, handler.getDocument(), tags);
        }
    }

    private void post(String title, Document content, Set<String> tags) throws HttpException, IOException, TransformerException {
        PostMethod method = null;
        try {
            method = new PostMethod("http://www.blogger.com/feeds/" + this.blogId + "/posts/default");
            method.addRequestHeader("GData-Version", String.valueOf(GDataVersion));
            method.addRequestHeader("Authorization", "GoogleLogin auth=" + this.AuthToken);
            Document dom = this.domBuilder.newDocument();
            Element entry = dom.createElementNS(Atom.NS, "entry");
            dom.appendChild(entry);
            entry.setAttribute("xmlns", Atom.NS);
            Element titleNode = dom.createElementNS(Atom.NS, "title");
            entry.appendChild(titleNode);
            titleNode.setAttribute("type", "text");
            titleNode.appendChild(dom.createTextNode(title));
            Element contentNode = dom.createElementNS(Atom.NS, "content");
            entry.appendChild(contentNode);
            contentNode.setAttribute("type", "xhtml");
            contentNode.appendChild(dom.importNode(content.getDocumentElement(), true));
            for (String tag : tags) {
                Element category = dom.createElementNS(Atom.NS, "category");
                category.setAttribute("scheme", "http://www.blogger.com/atom/ns#");
                category.setAttribute("term", tag);
                entry.appendChild(category);
            }
            StringWriter out = new StringWriter();
            this.xml2ascii.transform(new DOMSource(dom), new StreamResult(out));
            method.setRequestEntity(new StringRequestEntity(out.toString(), "application/atom+xml", "UTF-8"));
            int status = getHttpClient().executeMethod(method);
            if (status == 201) {
                IOUtils.copyTo(method.getResponseBodyAsStream(), System.out);
            } else {
                throw new HttpException("post returned http-code=" + status + " expected 201 (CREATE)");
            }
        } catch (TransformerException err) {
            throw err;
        } catch (HttpException err) {
            throw err;
        } catch (IOException err) {
            throw err;
        } finally {
            if (method != null) method.releaseConnection();
        }
    }

    private void listBlogs() throws HttpException, IOException {
        GetMethod method = null;
        try {
            method = new GetMethod("http://www.blogger.com/feeds/default/blogs");
            method.addRequestHeader("GData-Version", String.valueOf(GDataVersion));
            method.addRequestHeader("Authorization", "GoogleLogin auth=" + this.AuthToken);
            int status = getHttpClient().executeMethod(method);
            if (status == 200) {
                TransformerFactory factory = TransformerFactory.newInstance();
                factory.setAttribute("indent-number", new Integer(4));
                Transformer transformer = factory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.transform(new StreamSource(method.getResponseBodyAsStream()), new StreamResult(System.out));
            } else {
                throw new HttpException("login returned http-code=" + status);
            }
        } catch (HttpException err) {
            throw err;
        } catch (IOException err) {
            throw err;
        } catch (Exception err) {
            throw new RuntimeException(err);
        } finally {
            if (method != null) method.releaseConnection();
        }
    }

    /**
 * login to google {@linkplain http://code.google.com/apis/blogger/docs/2.0/developers_guide_protocol.html#ClientLogin}
 * @return the <b>Auth</b> token
 * @throws HttpException
 * @throws IOException
 */
    private String login() throws HttpException, IOException {
        LOG.fine("login");
        this.AuthToken = null;
        PostMethod postMethod = null;
        try {
            postMethod = new PostMethod("https://www.google.com/accounts/ClientLogin");
            postMethod.addParameters(new NameValuePair[] { new NameValuePair("accountType", "GOOGLE"), new NameValuePair("Email", this.userEmail), new NameValuePair("Passwd", new String(this.userPassword)), new NameValuePair("service", "blogger"), new NameValuePair("service", "PierreLindenbaum-bloggerdf-0.1") });
            int status = getHttpClient().executeMethod(postMethod);
            if (status == 200) {
                BufferedReader r = new BufferedReader(new InputStreamReader(postMethod.getResponseBodyAsStream()));
                String line;
                while ((line = r.readLine()) != null) {
                    LOG.info(line);
                    if (line.startsWith("Auth=")) {
                        this.AuthToken = line.substring(5);
                        break;
                    }
                }
                r.close();
                if (this.AuthToken == null) throw new IOException("Cannot get Auth token");
                return this.AuthToken;
            } else {
                throw new HttpException("login returned http-code=" + status);
            }
        } catch (HttpException err) {
            throw err;
        } catch (IOException err) {
            throw err;
        } finally {
            if (postMethod != null) postMethod.releaseConnection();
        }
    }

    public static void main(String[] args) {
        try {
            BigInteger blogId = null;
            String userMail = System.getProperty("user.name") + "@gmail.com";
            char userPassword[] = null;
            File googleFile = new File(System.getProperty("user.home"), ".google.properties");
            if (!googleFile.exists()) {
                System.err.println("Default params doesn't exists: " + googleFile);
            } else {
                Properties properties = new Properties();
                InputStream in = null;
                try {
                    in = new FileInputStream(googleFile);
                    properties.loadFromXML(in);
                    userMail = properties.getProperty("user.mail");
                    String s = properties.getProperty("user.password.base64");
                    if (s != null) {
                        userPassword = new String(Base64.decode(s)).toCharArray();
                    } else {
                        s = properties.getProperty("user.password");
                        if (s != null) {
                            userPassword = s.toCharArray();
                        }
                    }
                    s = properties.getProperty("blog.id");
                    if (s != null) {
                        blogId = new BigInteger(s);
                    }
                } catch (IOException err) {
                    System.err.println("Cannot read properties from " + googleFile + " " + err.getMessage());
                } finally {
                    if (in != null) in.close();
                    properties.clear();
                    properties = null;
                }
            }
            int optind = 0;
            String command = null;
            while (optind < args.length) {
                if (args[optind].equals("-h")) {
                    System.err.println("Pierre Lindenbaum PhD.");
                    System.err.println(Compilation.getLabel());
                    System.err.println("-h this screen");
                    System.err.println("-i <integer> blog id");
                    System.err.println("-m <mail>  default is:" + userMail);
                    System.err.println("-p <password> (or prompted)");
                    System.err.println("-c <command>");
                    System.err.println("  * 'post' post a RDF file");
                    System.err.println("<files>");
                    return;
                } else if (args[optind].equals("-i")) {
                    blogId = new BigInteger(args[++optind].trim());
                } else if (args[optind].equals("-m")) {
                    userMail = args[++optind];
                } else if (args[optind].equals("-p")) {
                    userPassword = args[++optind].toCharArray();
                } else if (args[optind].equals("--")) {
                    ++optind;
                    break;
                } else if (args[optind].startsWith("-")) {
                    System.err.println("bad argument " + args[optind]);
                    System.exit(-1);
                } else {
                    break;
                }
                ++optind;
            }
            if (command == null) {
                System.err.println("Undefined command");
                return;
            }
            userPassword = "".toCharArray();
            userMail = "@gmail.com";
            if (blogId == null) {
                System.err.println("Undefined blog ID.");
                return;
            }
            if (userPassword == null) {
                Console console = System.console();
                if (console == null) {
                    System.err.println("Undefined Password.");
                    return;
                }
                userPassword = console.readPassword("Blogger Password ? : ");
                if (userPassword == null || userPassword.length == 0) {
                    System.err.println("Cannot read Password.");
                    return;
                }
            }
            BloggeRDF app = new BloggeRDF();
            app.userEmail = userMail;
            app.userPassword = userPassword;
            app.blogId = blogId;
            if (command.equals("post")) {
                if (optind == args.length) {
                    Document dom = app.domBuilder.parse(System.in);
                    app.post(dom);
                } else {
                    while (optind < args.length) {
                        Document dom = app.domBuilder.parse(new File(args[optind++]));
                        app.post(dom);
                    }
                }
            } else if (command.equals("blogs")) {
                app.login();
                app.listBlogs();
            } else {
                System.err.println("Undefined blog ID.");
                return;
            }
            HashSet<String> tags = new HashSet<String>();
            tags.add("t1");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
