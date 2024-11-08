package com.pehrs.mailpost.wmlblog;

import java.util.Hashtable;
import java.util.Vector;
import com.pehrs.mailpost.service.MPPlugin;
import com.pehrs.mailpost.wmlblog.sql.*;
import com.pehrs.mailpost.util.PrefsMgr;
import com.pehrs.mailpost.util.Db;
import com.pehrs.mailpost.util.Utils;
import java.util.prefs.Preferences;
import sun.net.ftp.*;
import sun.net.*;
import javax.mail.*;
import javax.mail.event.*;
import javax.mail.internet.*;
import java.sql.*;
import java.text.*;
import java.util.logging.*;
import java.io.*;

/**
 * This is a MPService that "blogs" messages (with images) to a
 * web-server (via FTP). It generates both WML and HTML.
 * @author <a href="mailto:matti.pehrs@home.se">Matti Pehrs</a>
 * @version $Id: WMLBlogger.java,v 1.1.1.1 2004/10/19 22:46:10 mattipehrs Exp $
 * @see com.pehrs.mailpost.service.MPPlugin
 */
public class WMLBlogger implements MPPlugin {

    static Logger log = Logger.getLogger("com.pehrs.mailpost.wmlblog.WMLBlogger");

    static Vector prefNames = null;

    static Hashtable prefClasses = null;

    static Hashtable prefDisplayNames = null;

    static final String WMLBLOG_IMAGE_PATH = "wmlblogger.image.path";

    static final String WMLBLOG_SERVICE_KEY = "wmlblogger.service.key";

    static final String WMLBLOG_CHANNEL_KEY = "wmlblogger.channel.key";

    Db db = null;

    Publisher publisher = null;

    /** Creates a new instance of WMLBlogger */
    public WMLBlogger() {
        db = new Db();
        try {
            publisher = new Publisher();
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Could not create Publisher");
        }
    }

    public Class getPreferenceClass(String prefName) {
        getPreferenceNames();
        return (Class) prefClasses.get(prefName);
    }

    public String getPreferenceDisplayName(String prefName) {
        getPreferenceNames();
        return (String) prefDisplayNames.get(prefName);
    }

    public java.util.Vector getPreferenceNames() {
        if (prefNames == null) {
            prefNames = new Vector();
            prefClasses = new Hashtable();
            prefDisplayNames = new Hashtable();
            prefNames.add(WMLBLOG_IMAGE_PATH);
            prefClasses.put(WMLBLOG_IMAGE_PATH, String.class);
            prefDisplayNames.put(WMLBLOG_IMAGE_PATH, "Image storage path");
            prefNames.add(WMLBLOG_CHANNEL_KEY);
            prefClasses.put(WMLBLOG_CHANNEL_KEY, String.class);
            prefDisplayNames.put(WMLBLOG_CHANNEL_KEY, "Channel Switch Key");
        }
        return prefNames;
    }

    public String getServiceName() {
        return "WML/HTML-Blogger";
    }

    public String getDispatchKey() {
        Preferences prefs = PrefsMgr.getInstance().getPreferences();
        return prefs.get("wmlblogger.service.key", "wml");
    }

    public void handleMessage(javax.mail.Message orgMsg, String from, String to, String subject, java.util.Date sentDate, javax.mail.Part txtPart, boolean txtPartSwitchRowPresent, javax.mail.Part imgPart) {
        try {
            MP_USER userMatch = new MP_USER();
            userMatch.setPostEmail(from);
            Vector userMatches = userMatch.selectWhere(db.getConnection(), true);
            if (userMatches.size() == 0) {
                log.log(Level.SEVERE, "No user found with postEmail '" + from + "'");
            } else {
                MP_USER mpUser = (MP_USER) userMatches.get(0);
                String disKey = getDispatchKey();
                MP_POST_CHANNEL mpChannel = new MP_POST_CHANNEL();
                mpChannel.setUserId(mpUser.getUserId());
                String channelName = mpUser.getDefaultChannel();
                String txtc = "";
                if (txtPart != null) {
                    txtc = (String) txtPart.getContent();
                    if (txtPartSwitchRowPresent) {
                        txtc = Utils.removeFirstTxtLine(txtc);
                    }
                    String fl = Utils.getFirstTxtLine(txtc);
                    if (fl.startsWith(disKey)) {
                        channelName = fl.substring(disKey.length());
                        txtc = Utils.removeFirstTxtLine(txtc);
                    }
                }
                mpChannel.setChannelName(channelName);
                if (!mpChannel.dbSelect(db.getConnection())) {
                    log.log(Level.SEVERE, "Could not find the default channel '" + mpUser.getDefaultChannel() + "' for user '" + mpUser.getUserId() + "'");
                } else {
                    MP_POST mpPost = new MP_POST();
                    mpPost.setUserId(mpUser.getUserId());
                    mpPost.setChannelName(mpChannel.getChannelName());
                    mpPost.setPostEmail(from);
                    mpPost.setPostDate(new java.sql.Timestamp(sentDate.getTime()));
                    mpPost.setTitle(subject);
                    mpPost.setDescription(txtc);
                    mpPost.setImageMimeType(Utils.getMimeType(imgPart));
                    mpPost.dbInsert(db.getConnection());
                    String imgFilename = null;
                    if (imgPart != null) {
                        Preferences mailProps = PrefsMgr.getInstance().getPreferences();
                        String imgDir = mailProps.get(WMLBLOG_IMAGE_PATH, null);
                        String disp = imgPart.getDisposition();
                        String ext = ".jpg";
                        if (imgPart.isMimeType("application/octet-stream")) {
                            String ifn = imgPart.getFileName();
                            if (ifn != null) {
                                ifn = ifn.toLowerCase();
                                if (ifn.endsWith(".gif")) {
                                    ext = ".gif";
                                }
                            }
                        } else if (imgPart.isMimeType("image/gif")) {
                            ext = ".gif";
                        }
                        String iDir = imgDir + "/" + mpUser.getUserId().replace(' ', '_') + "/" + mpChannel.getChannelName().replace(' ', '_');
                        File iDirFile = new File(iDir);
                        iDirFile.mkdirs();
                        imgFilename = iDir + "/p" + Publisher.file_dateFormat.format(sentDate) + ext;
                        log.log(Level.INFO, "Saving attachment to file " + imgFilename);
                        try {
                            File f = new File(imgFilename);
                            OutputStream os = new BufferedOutputStream(new FileOutputStream(f));
                            InputStream is = imgPart.getInputStream();
                            int c;
                            while ((c = is.read()) != -1) {
                                os.write(c);
                            }
                            os.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                    if (imgFilename != null) {
                        log.log(Level.INFO, "Upload Image");
                        publisher.uploadImage(mpChannel, mpPost, imgFilename);
                    }
                    log.log(Level.INFO, "Upload HTML Blog");
                    publisher.uploadHTMLandWML(mpUser, mpChannel);
                    log.log(Level.INFO, "Post Done!");
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Exception during handleMessage()", e);
        }
    }

    public void init() {
        try {
            if (!db.tableExists("blog_user")) {
                InputStream is = WMLBlogger.class.getResourceAsStream("/com/pehrs/mailpost/wmlblog/wmlblog.sql");
                db.executeSQL(is);
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Exception when initializing the database", ex);
        }
    }

    public void shutdown() {
    }

    static WMLBloggerAdminPanel adminPanel = null;

    /**
     * Must return a JPanel Object
     */
    public javax.swing.JPanel getAdminPanel() {
        if (adminPanel == null) {
            try {
                adminPanel = new WMLBloggerAdminPanel();
            } catch (Exception ex) {
                ex.printStackTrace();
                adminPanel = null;
            }
        }
        return adminPanel;
    }

    public void adminRevert() {
    }

    public void adminSave() {
        getAdminPanel();
        adminPanel.savePrefs();
    }
}
