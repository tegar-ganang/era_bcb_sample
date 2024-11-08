package com.skruk.elvis.beans;

import com.isavvix.tools.FileInfo;
import com.skruk.elvis.db.xml.DbEngine;
import com.skruk.elvis.xslt.Transformer;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import org.jdom.Document;
import org.jdom.Element;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Description of the Class
 *
 * @author     skruk
 * @created    29 grudzień 2003
 */
public class UploadManager extends TimerTask {

    /** Description of the Field */
    private static final String WBSS_USERS_FILE = "/xml/wbss-users.xml";

    /** Description of the Field */
    private static UploadManager instance = null;

    /** Description of the Field */
    private static final String SQL_USER_COUNT = "SELECT MAX(id) FROM ftpuser";

    /** Description of the Field */
    private static final String SQL_INSERT_USER = "INSERT INTO ftpuser (userid, passwd, uid, gid, homedir, shell, count, accessed) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    /** Description of the Field */
    private static final String SQL_DELETE_OLD_USERS = "DELETE FROM ftpuser WHERE accessed < ?";

    /** Description of the Field */
    private static final String SQL_DELETE_USER = "DELETE FROM ftpuser WHERE userid = ?";

    /** Description of the Field */
    private java.io.File fWbssUsersFile = null;

    /** Description of the Field */
    private String ftpdbUser = null;

    /** Description of the Field */
    private String ftpdbPasswd = null;

    /** Description of the Field */
    private String ftpdbUrl = null;

    /** Description of the Field */
    private Connection connection = null;

    /** Description of the Field */
    private Timer timer = new Timer(true);

    /** Description of the Field */
    private Random random = new Random();

    /** Constructor for the UploadManager object */
    private UploadManager() {
        String ftpdbConnection = ContextKeeper.getContext().getInitParameter("ftpdbConnection");
        String[] ftpdbParams = ftpdbConnection.split(" ");
        this.ftpdbUser = ftpdbParams[0];
        this.ftpdbPasswd = ftpdbParams[1];
        this.ftpdbUrl = ftpdbParams[2];
        this.fWbssUsersFile = new java.io.File(ContextKeeper.getInstallDir() + WBSS_USERS_FILE);
        try {
            Class.forName(ftpdbParams[3]);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        timer.schedule(this, 1000, 100000);
    }

    /**
	 * Gets the instance attribute of the UploadManager class
	 *
	 * @return    The instance value
	 */
    public static UploadManager getInstance() {
        synchronized (UploadManager.class) {
            if (UploadManager.instance == null) {
                instance = new UploadManager();
            }
        }
        return UploadManager.instance;
    }

    /**
	 * Description of the Method
	 *
	 * @param  id                          Description of the Parameter
	 * @param  pass                        Description of the Parameter
	 * @return                             Description of the Return Value
	 * @exception  XmlPullParserException  Description of the Exception
	 * @exception  IOException             Description of the Exception
	 * @exception  FileNotFoundException   Description of the Exception
	 */
    public String loginUser(String id, String pass) throws XmlPullParserException, IOException, FileNotFoundException {
        String result = null;
        if ((id == null) || (pass == null)) {
            return null;
        }
        XmlPullParser xpp = Xpp.borrowParser();
        FileInputStream fis = new FileInputStream(this.fWbssUsersFile);
        Reader reader = new InputStreamReader(fis, java.nio.charset.Charset.forName("UTF-8"));
        xpp.setInput(reader);
        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if ((eventType == XmlPullParser.START_TAG) && xpp.getName().equals("user") && id.equals(xpp.getAttributeValue("", "id")) && pass.equals(xpp.getAttributeValue("", "pass"))) {
                result = xpp.getAttributeValue("", "name");
                break;
            }
            eventType = xpp.next();
        }
        fis.close();
        reader.close();
        Xpp.returnParser(xpp);
        return result;
    }

    /**
	 * Description of the Method
	 *
	 * @param  request  Description of the Parameter
	 * @param  session  Description of the Parameter
	 * @return          Description of the Return Value
	 */
    public Document createFromRequest(java.util.Map request, javax.servlet.http.HttpSession session) {
        Document doc = new Document();
        Element root = new Element("wbss-resource");
        doc.setRootElement(root);
        root.setAttribute("user-id", (String) session.getAttribute("user_name"));
        this.putElement(doc, "title", "pub_title", request);
        this.putElements(doc, "author", "pub_authors", request);
        this.putElement(doc, "digital", "pub_digital", request);
        this.putElement(doc, "category", "pub_category", request);
        this.putElement(doc, "publisher", "pub_publisher", request);
        this.putElement(doc, "pages", "pub_pages", request);
        this.putElement(doc, "isbn", "pub_isbn", request);
        this.putElements(doc, "keyword", "pub_keywords", request);
        this.putElement(doc, "lang", "pub_lang", request);
        this.putElement(doc, "publishing-place", "pub_pubplace", request);
        this.putElement(doc, "publishing-number", "pub_pubnumber", request);
        this.putElements(doc, "cites", "pub_cites", request);
        this.putElements(doc, "pkt", "pub_pkt", request);
        return doc;
    }

    /**
	 * Adds a feature to the ResourcesFromRequest attribute of the UploadManager object
	 *
	 * @param  doc      The feature to be added to the ResourcesFromRequest attribute
	 * @param  request  The feature to be added to the ResourcesFromRequest attribute
	 * @param  session  The feature to be added to the ResourcesFromRequest attribute
	 * @return          Description of the Return Value
	 */
    public Document addResourcesFromRequest(Document doc, java.util.Map request, javax.servlet.http.HttpSession session) {
        Element root = doc.getRootElement();
        Element resources = new Element("resources");
        resources.setAttribute("homedir", (String) session.getAttribute("ftpdb_homedir"));
        int i = 0;
        while (true) {
            String file_name = (String) request.get("file_" + i);
            String file_descr = (String) request.get("file_descr_" + i);
            if ((file_name == null) || (file_descr == null)) {
                break;
            }
            Element file = new Element("file");
            file.setAttribute("name", file_name);
            file.setText(file_descr);
            resources.addContent(file);
            i++;
        }
        root.addContent(resources);
        return doc;
    }

    /**
	 * Description of the Method
	 *
	 * @param  doc      The feature to be added to the EmailFromRequest attribute
	 * @param  request  Description of the Parameter
	 * @return          Description of the Return Value
	 */
    public Document addEmailFromRequest(Document doc, java.util.Map request) {
        Element root = doc.getRootElement();
        root.setAttribute("user-email", (String) request.get("pub_email"));
        return doc;
    }

    /**
	 * Description of the Method
	 *
	 * @param  doc      Description of the Parameter
	 * @param  name     Description of the Parameter
	 * @param  reqname  Description of the Parameter
	 * @param  params   Description of the Parameter
	 * @return          Description of the Return Value
	 */
    protected Element putElement(Document doc, String name, String reqname, java.util.Map params) {
        Element el = null;
        try {
            String val = new String(((String) params.get(reqname)).getBytes("ISO8859-1"), "UTF-8");
            if (val != null) {
                el = new Element(name);
                el.setText(val);
                if (name.equals("category")) {
                    el.setAttribute("other-name", (String) params.get("pub_categ_other"));
                }
                doc.getRootElement().addContent(el);
            }
        } catch (Exception ex) {
            System.err.println("UploadManager[264]:" + ex);
        }
        return el;
    }

    /**
	 * Description of the Method
	 *
	 * @param  doc      Description of the Parameter
	 * @param  name     Description of the Parameter
	 * @param  reqname  Description of the Parameter
	 * @param  request  Description of the Parameter
	 */
    protected void putElements(Document doc, String name, String reqname, java.util.Map request) {
        try {
            String val = new String(((String) request.get(reqname)).getBytes("ISO8859-1"), "UTF-8");
            if (val != null) {
                String[] vals = val.split("\n");
                if ((vals != null) && (vals.length > 0)) {
                    for (int i = 0; i < vals.length; i++) {
                        Element el = new Element(name);
                        el.setText(vals[i]);
                        doc.getRootElement().addContent(el);
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("UploadManager[300]:" + ex);
        }
    }

    /**
	 * Description of the Method
	 *
	 * @param  doc            Description of the Parameter
	 * @return                Description of the Return Value
	 * @exception  Exception  Description of the Exception
	 */
    public String storeRequest(Document doc) throws Exception {
        DbEngine dbe = DbEngine.borrowEngine(DbEngine.S_ELVIS_REQUEST_COL);
        String result = dbe.storeJdom(doc);
        DbEngine.returnEngine(dbe);
        return result;
    }

    /**
	 * Description of the Method
	 *
	 * @param  doc    Description of the Parameter
	 * @param  id     Description of the Parameter
	 * @param  email  Description of the Parameter
	 */
    public void sendRequest(Document doc, String id, String email) {
        try {
            SendHTML.send(ContextKeeper.getContext().getInitParameter("adminEmail"), null, null, "New WBSS request [" + id + "]", ContextKeeper.getContext().getInitParameter("fromEmail"), ContextKeeper.getContext().getInitParameter("smtpServer"), true, new java.io.BufferedReader(new java.io.StringReader(PrettyXML.getInstance().printElement(doc.getRootElement()).toString())));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
	 * Description of the Method
	 *
	 * @param  params  Description of the Parameter
	 * @param  dir     Description of the Parameter
	 */
    public void storeFiles(java.util.Map params, String dir) {
        java.util.Iterator it = params.keySet().iterator();
        File fDir = new java.io.File(dir);
        if (fDir.isDirectory()) {
            while (it.hasNext()) {
                Object key = it.next();
                try {
                    if (params.get(key) instanceof FileInfo) {
                        FileInfo file = (FileInfo) params.get(key);
                        File ftmp = new File(fDir, file.getClientFileName());
                        FileOutputStream fos = new FileOutputStream(ftmp);
                        FileChannel fch = fos.getChannel();
                        ReadableByteChannel rbch = Channels.newChannel(new ByteArrayInputStream(file.getFileContents()));
                        long pos = 0;
                        long cnt = 0;
                        java.nio.channels.FileLock flock = fch.lock();
                        while ((cnt = fch.transferFrom(rbch, pos, file.getFileContents().length)) > 0) {
                            pos += cnt;
                        }
                        flock.release();
                        fos.close();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } else {
            System.err.println("NO A DIRECTORY " + dir);
        }
    }

    /**
	 * Description of the Method
	 *
	 * @return                   Description of the Return Value
	 * @exception  SQLException  Description of the Exception
	 */
    public String[] registerUser() throws SQLException {
        int id = this.getUserCount() + 1;
        String userid = "U" + Math.abs(random.nextInt()) + "_" + id;
        String passwd = "P" + Math.abs(random.nextInt());
        int uid = ContextKeeper.getIntParameter("wbssUID");
        int gid = ContextKeeper.getIntParameter("wbssGID");
        String homedir = ContextKeeper.getContext().getInitParameter("ftpdbTmpUserDir") + id;
        String shell = "/sbin/nologin";
        int count = 10;
        Timestamp accessed = new Timestamp(new java.util.Date().getTime());
        PreparedStatement stmt = this.getConnection().prepareStatement(SQL_INSERT_USER);
        File fdir = new File(homedir);
        if (!fdir.exists()) {
            fdir.mkdirs();
        }
        System.out.println("GID: " + gid);
        stmt.setString(1, userid);
        stmt.setString(2, passwd);
        stmt.setInt(3, uid);
        stmt.setInt(4, gid);
        stmt.setString(5, homedir);
        stmt.setString(6, shell);
        stmt.setInt(7, count);
        stmt.setTimestamp(8, accessed);
        stmt.executeUpdate();
        return new String[] { userid, passwd, homedir };
    }

    /**
	 * Description of the Method
	 *
	 * @param  userid            Description of the Parameter
	 * @exception  SQLException  Description of the Exception
	 */
    public void unregisterUser(String userid) throws SQLException {
        PreparedStatement stmt = this.getConnection().prepareStatement(SQL_DELETE_USER);
        stmt.setString(1, userid);
        stmt.executeUpdate();
    }

    /**
	 * Gets the connection attribute of the UploadManager object
	 *
	 * @return    The connection value
	 */
    protected Connection getConnection() {
        try {
            if ((connection != null) && connection.isClosed()) {
                connection = null;
            }
        } catch (Exception ex) {
            connection = null;
            System.err.println("UploadManager[]:" + ex);
        }
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(ftpdbUrl, ftpdbUser, ftpdbPasswd);
            } catch (Exception ex) {
            }
        }
        return connection;
    }

    /**
	 * Gets the userCount attribute of the UploadManager object
	 *
	 * @return                   The userCount value
	 * @exception  SQLException  Description of the Exception
	 */
    protected int getUserCount() throws SQLException {
        Connection con = this.getConnection();
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery(SQL_USER_COUNT);
        int result = -1;
        if ((rs != null) && rs.next()) {
            result = rs.getInt(1);
        }
        rs.close();
        return result;
    }

    /** Main processing method for the UploadManager object */
    public void run() {
        if (!ContextKeeper.isContext()) {
            this.cancel();
        }
        try {
            PreparedStatement stmt = this.getConnection().prepareStatement(SQL_DELETE_OLD_USERS);
            stmt.setTimestamp(1, new Timestamp(new java.util.Date().getTime() - 3600000));
            stmt.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("UploadManager[488]:" + ex);
        }
    }

    /**
	 * Description of the Method
	 *
	 * @param  doc                                                        Description of the Parameter
	 * @return                                                            Description of the Return Value
	 * @exception  org.jdom.JDOMException                                 Description of the Exception
	 * @exception  java.io.IOException                                    Description of the Exception
	 * @exception  java.net.MalformedURLException                         Description of the Exception
	 * @exception  javax.xml.transform.TransformerConfigurationException  Description of the Exception
	 * @exception  javax.xml.transform.TransformerException               Description of the Exception
	 */
    public String showWbssRequest(Document doc) throws org.jdom.JDOMException, java.io.IOException, java.net.MalformedURLException, javax.xml.transform.TransformerConfigurationException, javax.xml.transform.TransformerException {
        StringWriter sw = new StringWriter();
        javax.xml.transform.Source xmlSource = new javax.xml.transform.dom.DOMSource(new org.jdom.output.DOMOutputter().output(doc));
        javax.xml.transform.Transformer transformer = Transformer.borrowTransformer("wbss-request");
        javax.xml.transform.stream.StreamResult sr = new javax.xml.transform.stream.StreamResult(sw);
        transformer.transform(xmlSource, sr);
        Transformer.returnTransformer(transformer);
        return sw.toString();
    }
}
