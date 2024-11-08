package com.pehrs.mailpost.wmlblog;

import java.io.*;
import java.util.*;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.MethodInvocationException;
import com.pehrs.mailpost.util.*;
import com.pehrs.mailpost.wmlblog.sql.*;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import sun.net.ftp.*;
import sun.net.*;
import java.sql.*;
import java.text.*;
import java.util.logging.*;
import java.io.*;
import java.util.prefs.Preferences;

public class Publisher {

    static Logger log = Logger.getLogger("com.pehrs.mailpost.wmlblog.Publisher");

    Db db = null;

    private String webPath;

    static SimpleDateFormat file_dateFormat = new SimpleDateFormat("yyMMddhhmmss");

    private static SimpleDateFormat archive_dateFormat = new SimpleDateFormat("yyyy-MM");

    private static SimpleDateFormat archive_file_dateFormat = new SimpleDateFormat("yyyyMM");

    VelocityGenerator htmlGenerator = null;

    VelocityGenerator wmlGenerator = null;

    public Publisher() throws java.lang.Exception {
        webPath = System.getProperty("mailpost.install.dir") + "/conf/wmlblog/web";
        db = new Db();
        htmlGenerator = new VelocityGenerator();
        wmlGenerator = new VelocityGenerator();
    }

    public void publishChannel(String uid, String channelName) throws SQLException, IOException {
        Connection con = db.getConnection();
        MP_USER mpUser = new MP_USER();
        mpUser.setUserId(uid);
        if (!mpUser.dbSelect(con)) {
            throw new SQLException("User '" + uid + "' not found!");
        }
        publishChannel(mpUser, channelName);
    }

    public void publishChannel(MP_USER mpUser, String channelName) throws SQLException, IOException {
        Connection con = db.getConnection();
        MP_POST_CHANNEL mpChannel = new MP_POST_CHANNEL();
        mpChannel.setUserId(mpUser.getUserId());
        mpChannel.setChannelName(channelName);
        if (!mpChannel.dbSelect(con)) {
            throw new SQLException("Could not find the channel '" + channelName + "' for user '" + mpUser.getUserId() + "'");
        }
        publishChannel(mpUser, mpChannel);
    }

    public void publishChannel(MP_USER mpUser, MP_POST_CHANNEL mpChannel) throws java.sql.SQLException, java.io.IOException, FileNotFoundException {
        Connection con = db.getConnection();
        uploadImages(mpUser, mpChannel);
        uploadHTMLandWML(mpUser, mpChannel);
    }

    public void uploadImages(MP_USER mpUser, MP_POST_CHANNEL mpChannel) throws java.sql.SQLException, java.io.IOException, FileNotFoundException {
        Connection con = db.getConnection();
        log.log(Level.INFO, "uploading images for channel " + mpUser.getUserId() + ":" + mpChannel.getChannelName());
        String query = "select " + "user_id, " + "channel_name, " + "post_email, " + "post_date, " + "title, " + "description, " + "image_mime_type " + "from blog_post " + "where " + " user_id = ? " + "AND " + " channel_name = ? " + " ORDER BY post_date DESC";
        PreparedStatement stmt = con.prepareStatement(query);
        stmt.setString(1, mpUser.getUserId());
        stmt.setString(2, mpChannel.getChannelName());
        ResultSet rs = stmt.executeQuery();
        int row = 0;
        MP_POST mpPost = new MP_POST();
        while (rs.next()) {
            if (row >= 15) break;
            mpPost.setValues(rs);
            uploadImage(mpChannel, mpPost);
            row++;
        }
        stmt.close();
    }

    public void uploadImage(MP_POST_CHANNEL mpChannel, MP_POST mpPost) throws IOException {
        uploadImage(mpChannel, mpPost, getImagePath(mpPost));
    }

    public void uploadImage(MP_POST_CHANNEL mpChannel, MP_POST mpPost, String imgFilename) {
        try {
            String ext = ".jpg";
            if (mpPost.getImageMimeType().equals("image/gif")) {
                ext = ".gif";
            }
            String uploadName = "p" + file_dateFormat.format(mpPost.getPostDate()) + ext;
            FtpClient client = new FtpClient();
            log.log(Level.INFO, "opening FTP to " + mpChannel.getFtpHost() + " for image " + mpChannel.getFtpPath() + "/" + uploadName);
            client.openServer(mpChannel.getFtpHost());
            client.login(mpChannel.getFtpUser(), mpChannel.getFtpPasswd());
            client.binary();
            client.cd(mpChannel.getFtpPath());
            FileInputStream in = new FileInputStream(imgFilename);
            TelnetOutputStream out = client.put(uploadName);
            byte[] buff = new byte[512];
            for (int len = in.read(buff); len != -1; len = in.read(buff)) {
                out.write(buff, 0, len);
            }
            out.flush();
            out.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void uploadHTMLandWML(MP_USER mpUser, MP_POST_CHANNEL mpChannel) throws java.sql.SQLException, java.io.IOException {
        Connection con = db.getConnection();
        log.log(Level.INFO, "uploading HTML+WML for channel " + mpUser.getUserId() + ":" + mpChannel.getChannelName());
        String query = "select " + "user_id, " + "channel_name, " + "post_email, " + "post_date, " + "title, " + "description, " + "image_mime_type " + "from blog_post " + "where " + " user_id = ? " + "AND " + " channel_name = ? " + " ORDER BY post_date DESC";
        PreparedStatement stmt = con.prepareStatement(query);
        stmt.setString(1, mpUser.getUserId());
        stmt.setString(2, mpChannel.getChannelName());
        SimpleDateFormat udf = new SimpleDateFormat(mpChannel.getChannelDateFormat());
        ResultSet rs = stmt.executeQuery();
        StringBuffer index_xml = new StringBuffer();
        index_xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        index_xml.append("<document>\n  \n  <properties>\n    <title>" + mpChannel.getChannelDisplayName() + "</title>\n  </properties>\n  \n  <body>\n\n");
        index_xml.append("<menu name=\"Main\">\n      <item name=\"Index\"       href=\"index\"/>\n      <item name=\"Archive\"     href=\"archive\"/>\n    </menu>\n");
        int row = 0;
        while (rs.next()) {
            if (row >= 15) break;
            MP_POST post = new MP_POST();
            post.setValues(rs);
            index_xml.append("<post><date>" + udf.format(post.getPostDate()) + "</date><title>" + post.getTitle() + "</title><img height=\"64\" width=\"64\" src=\"" + getPostFilename(post) + "\"></img><message>" + post.getDescription() + "</message></post>");
            row++;
        }
        index_xml.append("</body></document>\n");
        Calendar match_date = new GregorianCalendar();
        Calendar archive_date = null;
        Vector months = new Vector();
        rs.beforeFirst();
        while (rs.next()) {
            MP_POST post = new MP_POST();
            post.setValues(rs);
            match_date.setTime(post.getPostDate());
            if (archive_date == null) {
                archive_date = (Calendar) match_date.clone();
                months.add(archive_date.getTime());
            } else if (archive_date.get(Calendar.MONTH) != match_date.get(Calendar.MONTH)) {
                archive_date = (Calendar) match_date.clone();
                months.add(archive_date.getTime());
            }
        }
        log.log(Level.FINEST, "months=" + months);
        StringBuffer archive_menu_xml = new StringBuffer();
        archive_menu_xml.append("<menu name=\"Main\"><item name=\"Index\" href=\"index\"/><item name=\"Archive\"     href=\"archive\"/></menu> <menu name=\"Sub\">");
        for (Iterator it = months.iterator(); it.hasNext(); ) {
            java.util.Date dd = (java.util.Date) it.next();
            archive_menu_xml.append("<item name=\"" + archive_dateFormat.format(dd) + "\" href=\"archive_" + archive_file_dateFormat.format(dd) + "\"/>");
        }
        archive_menu_xml.append("</menu>");
        Vector archive_xmls = new Vector();
        Vector archive_dates = new Vector();
        StringBuffer archive_xml = null;
        rs.beforeFirst();
        while (rs.next()) {
            MP_POST post = new MP_POST();
            post.setValues(rs);
            match_date.setTime(post.getPostDate());
            if (archive_xml == null) {
                archive_xml = new StringBuffer();
                archive_date.setTime(post.getPostDate());
                archive_xmls.add(archive_xml);
                archive_dates.add(archive_date.clone());
                archive_xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                archive_xml.append("<document>\n  \n  <properties>\n    <title>" + mpChannel.getChannelDisplayName() + "</title>\n  </properties>\n  \n  <body>\n\n");
                archive_xml.append(archive_menu_xml);
            } else {
                if (archive_date.get(Calendar.MONTH) != match_date.get(Calendar.MONTH)) {
                    archive_xml.append("</body></document>\n");
                    archive_xml = new StringBuffer();
                    archive_date.setTime(post.getPostDate());
                    archive_xmls.add(archive_xml);
                    archive_dates.add(archive_date.clone());
                    archive_xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    archive_xml.append("<document>\n  \n  <properties>\n    <title>" + mpChannel.getChannelDisplayName() + "</title>\n  </properties>\n  \n  <body>\n\n");
                    archive_xml.append(archive_menu_xml);
                }
            }
            archive_xml.append("<archive-post><date>" + udf.format(post.getPostDate()) + "</date><title>" + post.getTitle() + "</title><img height=\"64\" width=\"64\" src=\"" + getPostFilename(post) + "\"></img><message>" + post.getDescription() + "</message></archive-post>");
        }
        if (archive_xml != null) {
            archive_xml.append("</body></document>\n");
        }
        archive_xml = new StringBuffer();
        archive_xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        archive_xml.append("<document>\n  \n  <properties>\n    <title>" + mpChannel.getChannelDisplayName() + "</title>\n  </properties>\n  \n  <body>\n\n");
        archive_xml.append(archive_menu_xml);
        archive_xml.append("</body></document>\n");
        stmt.close();
        log.log(Level.FINEST, "archive_menu_xml=" + archive_menu_xml);
        log.log(Level.FINEST, "index_xml=" + index_xml);
        log.log(Level.FINEST, "archive_xml=" + archive_xml);
        log.log(Level.FINEST, "archive_xmls=" + archive_xmls);
        Utils.ftpUploadFile(mpChannel.getFtpHost(), mpChannel.getFtpUser(), mpChannel.getFtpPasswd(), mpChannel.getFtpPath(), "style.css", webPath + "/style.css");
        Utils.ftpUploadFile(mpChannel.getFtpHost(), mpChannel.getFtpUser(), mpChannel.getFtpPasswd(), mpChannel.getFtpPath(), "trmenu.gif", webPath + "/trmenu.gif");
        String index_html = htmlGenerator.generate("html.vsl", index_xml.toString());
        log.log(Level.FINEST, "index_html=" + index_html);
        Utils.ftpUploadTxt(mpChannel.getFtpHost(), mpChannel.getFtpUser(), mpChannel.getFtpPasswd(), mpChannel.getFtpPath(), "index.html", index_html);
        String index_wml = wmlGenerator.generate("wml.vsl", index_xml.toString());
        log.log(Level.FINEST, "index_wml=" + index_wml);
        Utils.ftpUploadTxt(mpChannel.getFtpHost(), mpChannel.getFtpUser(), mpChannel.getFtpPasswd(), mpChannel.getFtpPath(), "index.wml", index_wml);
        String archive_html = htmlGenerator.generate("html.vsl", archive_xml.toString());
        log.log(Level.FINEST, "archive_html=" + archive_html);
        Utils.ftpUploadTxt(mpChannel.getFtpHost(), mpChannel.getFtpUser(), mpChannel.getFtpPasswd(), mpChannel.getFtpPath(), "archive.html", archive_html);
        String archive_wml = wmlGenerator.generate("wml.vsl", archive_xml.toString());
        log.log(Level.FINEST, "archive_wml=" + archive_wml);
        Utils.ftpUploadTxt(mpChannel.getFtpHost(), mpChannel.getFtpUser(), mpChannel.getFtpPasswd(), mpChannel.getFtpPath(), "archive.wml", archive_wml);
        int di = 0;
        for (Iterator it = archive_xmls.iterator(); it.hasNext(); di++) {
            String a_xml = ((StringBuffer) it.next()).toString();
            java.util.Date dd = (java.util.Date) months.get(di);
            String a_html = htmlGenerator.generate("html.vsl", a_xml.toString());
            log.log(Level.FINEST, "a_html=" + a_html);
            Utils.ftpUploadTxt(mpChannel.getFtpHost(), mpChannel.getFtpUser(), mpChannel.getFtpPasswd(), mpChannel.getFtpPath(), "archive_" + archive_file_dateFormat.format(dd) + ".html", a_html);
            String a_wml = wmlGenerator.generate("wml.vsl", a_xml.toString());
            log.log(Level.FINEST, "a_wml=" + a_wml);
            Utils.ftpUploadTxt(mpChannel.getFtpHost(), mpChannel.getFtpUser(), mpChannel.getFtpPasswd(), mpChannel.getFtpPath(), "archive_" + archive_file_dateFormat.format(dd) + ".wml", a_wml);
        }
        log.log(Level.INFO, "HTML+WML uploaded!");
    }

    public void oldUploadHTMLandWML(MP_USER mpUser, MP_POST_CHANNEL mpChannel) throws java.sql.SQLException, java.io.IOException {
        Connection con = db.getConnection();
        log.log(Level.INFO, "uploading HTML+WML for channel " + mpUser.getUserId() + ":" + mpChannel.getChannelName());
        String query = "select " + "user_id, " + "channel_name, " + "post_email, " + "post_date, " + "title, " + "description, " + "image_mime_type " + "from blog_post " + "where " + " user_id = ? " + "AND " + " channel_name = ? " + " ORDER BY post_date DESC";
        PreparedStatement stmt = con.prepareStatement(query);
        stmt.setString(1, mpUser.getUserId());
        stmt.setString(2, mpChannel.getChannelName());
        FtpClient httpClient = new FtpClient();
        log.log(Level.INFO, "opening FTP to " + mpChannel.getFtpHost() + " for HTML: " + mpChannel.getFtpPath() + "/index.html");
        httpClient.openServer(mpChannel.getFtpHost());
        httpClient.login(mpChannel.getFtpUser(), mpChannel.getFtpPasswd());
        httpClient.binary();
        httpClient.cd(mpChannel.getFtpPath());
        FtpClient wmlClient = new FtpClient();
        log.log(Level.INFO, "opening FTP to " + mpChannel.getFtpHost() + " for WML: " + mpChannel.getFtpPath() + "/index.wml");
        wmlClient.openServer(mpChannel.getFtpHost());
        wmlClient.login(mpChannel.getFtpUser(), mpChannel.getFtpPasswd());
        wmlClient.binary();
        wmlClient.cd(mpChannel.getFtpPath());
        PrintWriter html = new PrintWriter(httpClient.put("index.html"));
        html.println("<html>");
        html.println("  <head>");
        html.println("    <title>" + mpUser.getFullName() + " Blog</title>");
        html.println("  </head>");
        html.println("  <body>");
        html.println("    <h1>" + mpUser.getFullName() + " Blog</h1>");
        PrintWriter wml = new PrintWriter(wmlClient.put("index.wml"));
        wml.println("<?xml version=\"1.0\"?>");
        wml.println("<!DOCTYPE wml PUBLIC \"-//WAPFORUM//DTD WML 1.1//EN\" \"http://www.wapforum.org/DTD/wml_1.1.xml\">");
        wml.println("<wml>");
        wml.println(" <card id=\"index\" title=\"" + mpUser.getFullName() + " Blog\">");
        wml.println("   <p>");
        SimpleDateFormat udf = new SimpleDateFormat(mpChannel.getChannelDateFormat());
        ResultSet rs = stmt.executeQuery();
        int row = 0;
        while (rs.next()) {
            if (row >= 15) break;
            MP_POST post = new MP_POST();
            post.setValues(rs);
            html.println("    <b>" + post.getTitle() + "</b><br/>");
            html.println("    <i>" + udf.format(post.getPostDate()) + "</i><br/>");
            if (post.getImageMimeType() != null) {
                html.println("    <a href=\"" + getPostFilename(post) + "\"><img width=\"160\" border=\"0\" src=\"" + getPostFilename(post) + "\"></img></a><br/>");
            }
            html.println("    <span>" + post.getDescription() + "</span><br/>");
            html.println("<br/>");
            wml.println("    <b>" + post.getTitle() + "</b><br/>");
            wml.println("    <i>" + udf.format(post.getPostDate()) + "</i><br/>");
            if (post.getImageMimeType() != null) {
                wml.println("    <a href=\"" + getPostFilename(post) + "\"><img width=\"120\" border=\"0\" src=\"" + getPostFilename(post) + "\"></img></a><br/>");
            }
            wml.println("    " + post.getDescription() + "<br/><br/>");
            row++;
        }
        stmt.close();
        html.println("  </body>");
        html.println("</html>");
        html.flush();
        html.close();
        wml.println("   </p>");
        wml.println(" </card>");
        wml.println("</wml>");
        wml.flush();
        wml.close();
        log.log(Level.INFO, "HTML+WML uploaded!");
    }

    public static String getImagePath(MP_POST mpPost) throws IOException, FileNotFoundException {
        String ext = ".jpg";
        if (mpPost.getImageMimeType().equals("image/gif")) {
            ext = ".gif";
        }
        Preferences mailProps = PrefsMgr.getInstance().getPreferences();
        String imgDir = mailProps.get(WMLBlogger.WMLBLOG_IMAGE_PATH, null);
        String iDir = imgDir + "/" + mpPost.getUserId().replace(' ', '_') + "/" + mpPost.getChannelName().replace(' ', '_');
        File iDirFile = new File(iDir);
        iDirFile.mkdirs();
        return iDir + "/p" + file_dateFormat.format(mpPost.getPostDate()) + ext;
    }

    public static String getPostFilename(MP_POST post) {
        String ext = ".jpg";
        if (post.getImageMimeType().equals("image/gif")) {
            ext = ".gif";
        }
        return "p" + file_dateFormat.format(post.getPostDate()) + ext;
    }
}
