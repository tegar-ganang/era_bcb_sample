package com.j2biz.compote.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import net.sf.hibernate.Hibernate;
import net.sf.hibernate.HibernateException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionForward;
import org.apache.struts.util.LabelValueBean;
import org.apache.struts.util.RequestUtils;
import com.j2biz.compote.IConstants;
import com.j2biz.compote.CompoteContext;
import com.j2biz.compote.IConstants.REQUEST;
import com.j2biz.compote.IConstants.SESSION;
import com.j2biz.compote.exceptions.AssertationException;
import com.j2biz.compote.model.HibernateSession;
import com.j2biz.compote.pojos.ExtensionPoint;
import com.j2biz.compote.pojos.GlobalConfiguration;
import com.j2biz.compote.pojos.Group;
import com.j2biz.compote.pojos.Layout;
import com.j2biz.compote.pojos.Menu;
import com.j2biz.compote.pojos.MenuItem;
import com.j2biz.compote.pojos.User;

/**
 * @author michelson
 * @version $$
 * @since 0.1
 * 
 *  
 */
public class SystemUtils {

    private static Log log = LogFactory.getFactory().getInstance(SystemUtils.class);

    private static Collection dayCollection = null;

    private static Collection monthCollection = null;

    private static Collection yearCollection = null;

    private static Calendar CALENDAR = Calendar.getInstance();

    /**
     * @param from
     * @param to
     * @param subject
     * @param body
     * @throws AddressException
     * @throws MessagingException
     */
    public static final void sendEmail(String from, String to, String subject, String body) throws AddressException, MessagingException {
        Properties props = new Properties();
        GlobalConfiguration globalConfig = CompoteContext.getSystemConfiguration().getGlobalConfig();
        props.put("mail.smtp.host", globalConfig.getSmtpHost());
        Session session = Session.getInstance(props, null);
        session.setDebug(true);
        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        InternetAddress[] address = { new InternetAddress(to) };
        msg.setRecipients(Message.RecipientType.TO, address);
        msg.setSubject(subject);
        msg.setSentDate(new Date());
        msg.setText(body);
        Transport.send(msg);
    }

    /**
     * @param pageContext
     * @param bundle
     * @param srcKey
     * @param altKey
     * @param border
     * @param align
     * @param width
     * @param height
     * @param style
     * @param vspace
     * @return
     */
    public static final String getImageFromBundle(PageContext pageContext, String bundle, String srcKey, String altKey, String border, String align, String width, String height, String style, String vspace) {
        String iconPath = SystemUtils.getMessage(pageContext, bundle, srcKey);
        String iconDesc = "";
        if (StringUtils.isNotEmpty(altKey)) iconDesc = SystemUtils.getMessage(pageContext, bundle, altKey);
        iconPath = ((HttpServletRequest) pageContext.getRequest()).getContextPath() + iconPath;
        return "<img src=\"" + iconPath + "\" border=\"" + border + "\" width=\"" + width + "\" height=\"" + height + "\" align=\"" + align + "\" style=\"" + style + "\"  vspace=\"" + vspace + "\"  alt=\"" + iconDesc + "\" title=\"" + iconDesc + "\"/>";
    }

    /**
     * @param pageContext
     * @param srcKey
     * @param altKey
     * @param border
     * @param align
     * @param width
     * @param height
     * @param style
     * @param vspace
     * @return
     */
    public static final String getImageFromBundle(PageContext pageContext, String srcKey, String altKey, String border, String align, String width, String height, String style, String vspace) {
        String iconPath = SystemUtils.getMessage(pageContext, srcKey);
        String iconDesc = "";
        if (StringUtils.isNotEmpty(altKey)) iconDesc = SystemUtils.getMessage(pageContext, altKey);
        iconPath = ((HttpServletRequest) pageContext.getRequest()).getContextPath() + iconPath;
        return "<img src=\"" + iconPath + "\" border=\"" + border + "\" width=\"" + width + "\" height=\"" + height + "\" align=\"" + align + "\" style=\"" + style + "\"  vspace=\"" + vspace + "\"  alt=\"" + iconDesc + "\" title=\"" + iconDesc + "\"/>";
    }

    /**
     * @param pageContext
     * @param bundle
     * @param srcKey
     * @param altKey
     * @param border
     * @param width
     * @param height
     * @param style
     * @return
     */
    public static final String getImageFromBundle(PageContext pageContext, String bundle, String srcKey, String altKey, String border, String width, String height, String style) {
        String iconPath = SystemUtils.getMessage(pageContext, bundle, srcKey);
        String iconDesc = "";
        if (StringUtils.isNotEmpty(altKey)) iconDesc = SystemUtils.getMessage(pageContext, bundle, altKey);
        iconPath = ((HttpServletRequest) pageContext.getRequest()).getContextPath() + iconPath;
        return "<img src=\"" + iconPath + "\" border=\"" + border + "\" width=\"" + width + "\" height=\"" + height + "\" style=\"" + style + "\"   alt=\"" + iconDesc + "\" title=\"" + iconDesc + "\"/>";
    }

    /**
     * @param pageContext
     * @param srcKey
     * @param altKey
     * @param border
     * @param width
     * @param height
     * @param style
     * @return
     */
    public static final String getImageFromBundle(PageContext pageContext, String srcKey, String altKey, String border, String width, String height, String style) {
        String iconPath = SystemUtils.getMessage(pageContext, srcKey);
        String iconDesc = "";
        if (StringUtils.isNotEmpty(altKey)) iconDesc = SystemUtils.getMessage(pageContext, altKey);
        iconPath = ((HttpServletRequest) pageContext.getRequest()).getContextPath() + iconPath;
        return "<img src=\"" + iconPath + "\" border=\"" + border + "\" width=\"" + width + "\" height=\"" + height + "\" style=\"" + style + "\"   alt=\"" + iconDesc + "\" title=\"" + iconDesc + "\"/>";
    }

    /**
     * @param pageContext
     * @param bundle
     * @param srcKey
     * @param altKey
     * @param border
     * @return
     */
    public static final String getImageFromBundle(PageContext pageContext, String bundle, String srcKey, String altKey, String border) {
        String iconPath = SystemUtils.getMessage(pageContext, bundle, srcKey);
        String iconDesc = "";
        if (StringUtils.isNotEmpty(altKey)) iconDesc = SystemUtils.getMessage(pageContext, bundle, altKey);
        iconPath = ((HttpServletRequest) pageContext.getRequest()).getContextPath() + iconPath;
        return "<img src=\"" + iconPath + "\" border=\"" + border + "\"  alt=\"" + iconDesc + "\" title=\"" + iconDesc + "\"/>";
    }

    /**
     * @param pageContext
     * @param srcKey
     * @param altKey
     * @param border
     * @return
     */
    public static final String getImageFromBundle(PageContext pageContext, String srcKey, String altKey, String border) {
        String iconPath = SystemUtils.getMessage(pageContext, srcKey);
        String iconDesc = "";
        if (StringUtils.isNotEmpty(altKey)) iconDesc = SystemUtils.getMessage(pageContext, altKey);
        iconPath = ((HttpServletRequest) pageContext.getRequest()).getContextPath() + iconPath;
        return "<img src=\"" + iconPath + "\" border=\"" + border + "\" alt=\"" + iconDesc + "\"  title=\"" + iconDesc + "\"/>";
    }

    /**
     * @param request
     * @param relativeImagePath
     * @param alt
     * @param border
     * @return
     */
    public static final String getImage(HttpServletRequest request, String relativeImagePath, String alt, String border) {
        String layoutDirectory = (String) request.getAttribute(IConstants.REQUEST.CURRENT_LAYOUT_DIR);
        String ctxPath = request.getContextPath();
        String path = (layoutDirectory + "/" + relativeImagePath);
        String absPath = CompoteContext.getServletContext().getRealPath(path.substring(ctxPath.length()));
        try {
            File f = new File(absPath);
            if (f.exists()) {
                return "<img src=\"" + path + "\" border=\"" + border + "\" alt=\"" + alt + "\"  title=\"" + alt + "\"/>";
            }
        } catch (Exception e) {
            log.error("Unable to read file " + path, e);
        }
        String imgPath = ctxPath + "/" + relativeImagePath;
        return "<img src=\"" + imgPath + "\" border=\"" + border + "\" alt=\"" + alt + "\"  title=\"" + alt + "\"/>";
    }

    /**
     * @param date
     * @return
     */
    public static final String getFormattedDate(HttpServletRequest request, Date date) {
        if (date == null) return "";
        GlobalConfiguration gConfig = CompoteContext.getSystemConfiguration().getGlobalConfig();
        String pattern = gConfig.getDatetimeFormat();
        String charset = CompoteContext.getCharset(request);
        Locale dateLocale = getCurrentLocale(request);
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, dateLocale);
        String formatted = sdf.format(date);
        return encodeText(formatted, charset);
    }

    /**
     * @param request
     * @param locale
     * @return
     */
    public static final String getFormattedLanguage(HttpServletRequest request, Locale locale) {
        if (locale == null) return "";
        String charset = CompoteContext.getCharset(request);
        return encodeText(locale.getDisplayLanguage(getCurrentLocale(request)), charset);
    }

    public static final String getFormattedTimezone(HttpServletRequest request, TimeZone timezone) {
        if (timezone == null) return "";
        String charset = CompoteContext.getCharset(request);
        return encodeText(timezone.getDisplayName(getCurrentLocale(request)), charset);
    }

    /**
     * @param request
     * @return
     */
    public static Locale getCurrentLocale(HttpServletRequest request) {
        GlobalConfiguration gConfig = CompoteContext.getSystemConfiguration().getGlobalConfig();
        User user = CompoteContext.getUser(request);
        Locale defaultLocale = gConfig.getDefaultLocale();
        if (user != null) defaultLocale = user.getLanguage();
        return defaultLocale;
    }

    public static String encodeText(String text, String charset) {
        String encoded = text;
        try {
            encoded = new String(text.getBytes(charset), "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            log.error(e);
        }
        return encoded;
    }

    /**
     * @param date
     * @param month
     * @param year
     * @return
     */
    public static final String getFormattedDate(HttpServletRequest request, int date, int month, int year) {
        CALENDAR.set(year, month, date);
        return getFormattedDate(request, CALENDAR.getTime());
    }

    /**
     * @param date
     * @param month
     * @param year
     * @param hour
     * @param minute
     * @return
     */
    public static final String getFormattedDate(HttpServletRequest request, int date, int month, int year, int hour, int minute) {
        CALENDAR.set(year, month, date, hour, minute);
        return getFormattedDate(request, CALENDAR.getTime());
    }

    /**
     * @param date
     * @param month
     * @param year
     * @param hour
     * @param minute
     * @param second
     * @return
     */
    public static final String getFormattedDate(HttpServletRequest request, int date, int month, int year, int hour, int minute, int second) {
        CALENDAR.set(year, month, date, hour, minute, second);
        return getFormattedDate(request, CALENDAR.getTime());
    }

    /**
     * @param email
     * @return
     */
    public static final boolean isValidEmailAddress(String email) {
        String regex = "^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[_A-Za-z0-9-]+)";
        return email.matches(regex);
    }

    /**
     * @param request
     * @param e
     * @return
     */
    public static final ActionForward getPreparedErrorForward(HttpServletRequest request, Exception e) {
        request.setAttribute(REQUEST.SYSTEM_ERROR, getStackTrace(e));
        return IConstants.CONFIG.GLOBAL_ERROR_FORWARD;
    }

    /**
     * @param e
     * @return
     */
    public static final String getStackTrace(Throwable e) {
        return ExceptionUtils.getStackTrace(e);
    }

    /**
     * @param pageContext
     * @param key
     * @return
     */
    public static final String getMessage(PageContext pageContext, String key) {
        return getMessage(pageContext, null, key);
    }

    /**
     * @param pageContext
     * @param resourceBundle
     * @param key
     * @return
     */
    public static final String getMessage(PageContext pageContext, String resourceBundle, String key) {
        User u = CompoteContext.getUser((HttpServletRequest) pageContext.getRequest());
        Locale locale = null;
        if (u != null) locale = u.getLanguage();
        if (locale == null) locale = CompoteContext.getSystemConfiguration().getGlobalConfig().getDefaultLocale();
        String msg = key;
        try {
            msg = RequestUtils.message(pageContext, resourceBundle, Globals.LOCALE_KEY, key);
        } catch (Exception e) {
            log.error("Unable to retrieve message from resource bundle!", e);
        }
        return msg;
    }

    /**
     * @param pageContext
     * @param key
     * @param args
     * @return
     */
    public static final String getMessage(PageContext pageContext, String key, Object[] args) {
        return getMessage(pageContext, null, key, args);
    }

    /**
     * @param pageContext
     * @param resourceBundle
     * @param key
     * @param args
     * @return
     */
    public static final String getMessage(PageContext pageContext, String resourceBundle, String key, Object[] args) {
        User u = CompoteContext.getUser((HttpServletRequest) pageContext.getRequest());
        Locale locale = null;
        if (u != null) locale = u.getLanguage();
        if (locale == null) locale = CompoteContext.getSystemConfiguration().getGlobalConfig().getDefaultLocale();
        String msg = key;
        try {
            msg = RequestUtils.message(pageContext, resourceBundle, Globals.LOCALE_KEY, key, args);
        } catch (Exception e) {
            log.error("Unable to retrieve message from resource bundle!", e);
        }
        return msg;
    }

    /**
     * @param e
     * @return
     */
    public static final String getStackTrace(Object o) {
        if (o != null) return o.toString(); else return "";
    }

    /**
     * @param request
     * @return @throws
     *         Exception
     */
    public static Collection getLocaleCollection(HttpServletRequest request) throws Exception {
        java.util.ArrayList myLocales = new java.util.ArrayList();
        TreeSet locTree = new TreeSet(new LocaleComparator());
        String[] localeArray = Locale.getISOLanguages();
        for (int i = 0; i < localeArray.length; i++) {
            locTree.add(new Locale(localeArray[i]));
        }
        java.util.Iterator it = locTree.iterator();
        while (it.hasNext()) {
            Locale loc = (Locale) it.next();
            myLocales.add(new LabelValueBean(getFormattedLanguage(request, loc), loc.toString()));
        }
        return myLocales;
    }

    /**
     * @param request
     * @return
     */
    public static Collection getTimezoneCollection(HttpServletRequest request) {
        TreeSet myZones = new TreeSet(new TimeZoneComparator());
        String[] zoneArray = TimeZone.getAvailableIDs();
        for (int i = 0; i < zoneArray.length; i++) {
            myZones.add(TimeZone.getTimeZone(zoneArray[i]));
        }
        ArrayList zones = new ArrayList();
        java.util.Iterator it = myZones.iterator();
        while (it.hasNext()) {
            TimeZone timezone = (TimeZone) it.next();
            zones.add(new LabelValueBean(timezone.getID(), timezone.getID()));
        }
        return zones;
    }

    /**
     *  
     */
    public static Collection getDayCollection() {
        if (dayCollection == null) {
            ArrayList myDayList = new ArrayList();
            for (int i = 1; i < 32; i++) {
                LabelValueBean x = new LabelValueBean(String.valueOf(i), String.valueOf(i));
                myDayList.add(x);
            }
            dayCollection = myDayList;
        }
        return dayCollection;
    }

    /**
     *  
     */
    public static Collection getMonthCollection() {
        if (monthCollection == null) {
            ArrayList myMonthList = new ArrayList();
            for (int i = 1; i < 13; i++) {
                LabelValueBean x = new LabelValueBean(String.valueOf(i), String.valueOf(i));
                myMonthList.add(x);
            }
            monthCollection = myMonthList;
        }
        return monthCollection;
    }

    /**
     *  
     */
    public static Collection getYearCollection() {
        if (yearCollection == null) {
            ArrayList myYearList = new ArrayList();
            for (int i = 1940; i < 2000; i++) {
                LabelValueBean x = new LabelValueBean(String.valueOf(i), String.valueOf(i));
                myYearList.add(x);
            }
            yearCollection = myYearList;
        }
        return yearCollection;
    }

    /**
     * @param request
     * @param width
     * @param height
     * @return
     */
    public static String getPixelGif(HttpServletRequest request, String width, String height) {
        String contextName = request.getContextPath();
        return "<img src=\"" + contextName + "/images/1px.gif\" width=\"" + width + "\" height=\"" + height + "\" boder=\"0\">";
    }

    /**
     * @param pageContext
     * @param width
     * @param height
     * @return
     */
    public static String getPixelGif(PageContext pageContext, String width, String height) {
        return getPixelGif((HttpServletRequest) pageContext.getRequest(), width, height);
    }

    /**
     * Returns all groups except "everybody"-group
     * 
     * @return
     */
    public static Collection getGroupCollection() {
        try {
            StringBuffer buffer = null;
            ArrayList groupCollection = new ArrayList();
            net.sf.hibernate.Session session = HibernateSession.openSession();
            Iterator it = session.find("from Group as g").iterator();
            while (it.hasNext()) {
                Group g = (Group) it.next();
                if (g.getParentGroups().size() != 0) {
                    buffer = new StringBuffer();
                    int depth = g.getDepth();
                    for (int i = 0; i < depth; i++) {
                        buffer.append("-");
                    }
                    buffer.append(" ");
                    String label = buffer.toString() + g.getName();
                    groupCollection.add(new LabelValueBean(label, String.valueOf(g.getId())));
                }
            }
            HibernateSession.closeSession(session);
            return groupCollection;
        } catch (HibernateException e) {
            log.error("Unable to get groups from database!", e);
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Returns full list of group saved in database.
     * 
     * @return
     */
    public static Collection getFullGroupCollection() {
        try {
            StringBuffer buffer = null;
            ArrayList groupCollection = new ArrayList();
            net.sf.hibernate.Session session = HibernateSession.openSession();
            Iterator it = session.find("from Group as g").iterator();
            while (it.hasNext()) {
                Group g = (Group) it.next();
                buffer = new StringBuffer();
                int depth = g.getDepth();
                for (int i = 0; i < depth; i++) {
                    buffer.append("-");
                }
                buffer.append(" ");
                String label = buffer.toString() + g.getName();
                groupCollection.add(new LabelValueBean(label, String.valueOf(g.getId())));
            }
            HibernateSession.closeSession(session);
            return groupCollection;
        } catch (HibernateException e) {
            log.error("Unable to get groups from database!", e);
        }
        return Collections.EMPTY_LIST;
    }

    public static Group getEverybodyGroup() {
        return getGroupByName("everybody");
    }

    public static Group getUsersGroup() {
        return getGroupByName("users");
    }

    public static Group getAdministratorsGroup() {
        return getGroupByName("administrators");
    }

    public static Group getGroupByName(String name) {
        Group g = null;
        try {
            net.sf.hibernate.Session s = HibernateSession.openSession();
            List list = s.find("from Group as g where g.name = ?", name, Hibernate.STRING);
            if (list.size() != 1) {
                HibernateSession.closeSession(s);
                throw new IllegalArgumentException("Unable to find group with name=" + name);
            }
            g = (Group) list.get(0);
            HibernateSession.closeSession(s);
        } catch (Exception e) {
            log.error("Unable to find group with name=" + name, e);
        }
        return g;
    }

    public static void assertIsAdministrator(User user) {
        boolean result = user.isInGroup(getAdministratorsGroup());
        if (!result) throw new AssertationException("User '" + user.getUsername() + "' is not a member of 'administrators'-group.");
    }

    public static void assertIsUserInGroup(User user, Group group) {
        boolean result = user.isInGroup(group);
        if (!result) throw new AssertationException("User '" + user.getUsername() + "' is not a member of '" + group.getName() + "'-group.");
    }

    public static void assertIsUserInGroup(User user, String groupName) {
        assertIsUserInGroup(user, getGroupByName(groupName));
    }

    /**
     * @param request
     * @param group
     */
    public static void checkIsLoggedUserInGroup(HttpServletRequest request, Group group) {
        HttpSession s = request.getSession();
        User u = (User) s.getAttribute(SESSION.USER);
        if (u == null) throw new AssertationException("There is no logged user found within current session!");
        assertIsUserInGroup(u, group);
    }

    /**
     * @param request
     */
    public static void checkIsLoggedUserInAdministratorsGroup(HttpServletRequest request) {
        checkIsLoggedUserInGroup(request, getAdministratorsGroup());
    }

    /**
     * Filters groups by removing redundant forefather groups.
     * 
     * @param groupIDs
     * @return
     */
    public static Set filterRedundantForefatherGroups(net.sf.hibernate.Session s, String[] groupIDs) {
        ArrayList groups = new ArrayList();
        Set filteredGroups = new HashSet();
        for (int i = 0; i < groupIDs.length; i++) {
            String gID = groupIDs[i];
            try {
                Group g = (Group) s.load(Group.class, Long.valueOf(gID));
                groups.add(g);
            } catch (Exception e) {
                log.error("Unable to get group with id=" + gID, e);
            }
        }
        Group g1 = null;
        Group g2 = null;
        boolean isForefather = false;
        for (int i = 0; i < groups.size(); i++) {
            g1 = (Group) groups.get(i);
            isForefather = false;
            for (int j = 0; j < groups.size(); j++) {
                g2 = (Group) groups.get(j);
                if (!g1.equals(g2)) {
                    if (g1.isForefatherOf(g2)) {
                        isForefather = true;
                        break;
                    }
                }
            }
            if (!isForefather) {
                filteredGroups.add(g1);
            }
        }
        return filteredGroups;
    }

    /**
     * Checks, if a given Group is one of the 5 preinstalled groups: everybody, users, mdoerators, redaktors or
     * adminsitrators.
     * 
     * @param group
     *            to check.
     * @return true, if given group is one of the preinstalled groups. otherwise returns false.
     */
    public static final boolean isOneOfPreinstalledGroups(Group g) {
        if (g == null) return false;
        if (g.getName().equals("administrators") || g.getName().equals("moderators") || g.getName().equals("users") || g.getName().equals("everybody") || g.getName().equals("redaktors")) return true;
        return false;
    }

    /**
     * @param pageContext
     * @param group
     * @return
     */
    public static final String getFormattedGroup(PageContext pageContext, Group group) {
        StringBuffer out = new StringBuffer();
        out.append(SystemUtils.getImageFromBundle(pageContext, "imagesBundle", "icon.group", null, "0"));
        out.append(" ");
        out.append(group.getName());
        return out.toString();
    }

    /**
     * @param pageContext
     * @param user
     * @return
     */
    public static final String getFormattedUser(PageContext pageContext, User user) {
        StringBuffer out = new StringBuffer();
        out.append(SystemUtils.getImageFromBundle(pageContext, "imagesBundle", "icon.user", null, "0"));
        out.append(" ");
        out.append(user.getUsername());
        return out.toString();
    }

    /**
     * @param pageContext
     * @param menu
     * @return
     */
    public static final String getFormattedMenu(PageContext pageContext, Menu menu) {
        StringBuffer out = new StringBuffer();
        out.append(SystemUtils.getImageFromBundle(pageContext, "imagesBundle", "icon.menu", "icon.menu.desc", "0"));
        out.append(" ");
        out.append(menu.getNameKey());
        return out.toString();
    }

    /**
     * @param pageContext
     * @param item
     * @return
     */
    public static final String getFormattedMenuItem(PageContext pageContext, MenuItem item) {
        StringBuffer out = new StringBuffer();
        out.append(SystemUtils.getImageFromBundle(pageContext, "imagesBundle", "icon.menuitem", "icon.menuitem.desc", "0"));
        out.append(" ");
        if (StringUtils.isNotEmpty(item.getName())) out.append(item.getName()); else out.append(item.getNameKey());
        return out.toString();
    }

    /**
     * @param pageContext
     * @param user
     * @return
     */
    public static final String getUserProfileLink(PageContext pageContext, User user) {
        StringBuffer out = new StringBuffer();
        out.append("<a href=\"#\">");
        out.append(getFormattedUser(pageContext, user));
        out.append("</a>");
        return out.toString();
    }

    /**
     * @param dir
     * @return
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    /**
     * @param fromDir
     * @param toDir
     */
    public static void move(File fromDir, File toDir) {
        File[] allFiles = fromDir.listFiles();
        boolean success;
        for (int i = 0; i < allFiles.length; i++) {
            File renameTo = new File(toDir, allFiles[i].getName());
            success = allFiles[i].renameTo(renameTo);
            if (!success) {
                log.error("Unable to copy : " + allFiles[i] + " to " + renameTo);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Copied : " + allFiles[i] + " to " + renameTo);
                }
            }
        }
    }

    /**
     * Copy directory or file "from" to directory "to".
     * 
     * @param from
     * @param to
     */
    public static void copy(File from, File to) {
        boolean result;
        if (from.isDirectory()) {
            File[] subFiles = from.listFiles();
            for (int i = 0; i < subFiles.length; i++) {
                File newDir = new File(to, subFiles[i].getName());
                result = false;
                if (subFiles[i].isDirectory()) {
                    if (newDir.exists()) result = true; else result = newDir.mkdirs();
                } else if (subFiles[i].isFile()) {
                    try {
                        result = newDir.createNewFile();
                    } catch (IOException e) {
                        log.error("unable to create new file: " + newDir, e);
                        result = false;
                    }
                }
                if (result) copy(subFiles[i], newDir);
            }
        } else if (from.isFile()) {
            FileInputStream in = null;
            FileOutputStream out = null;
            try {
                in = new FileInputStream(from);
                out = new FileOutputStream(to);
                int fileLength = (int) from.length();
                char charBuff[] = new char[fileLength];
                int len;
                int oneChar;
                while ((oneChar = in.read()) != -1) {
                    out.write(oneChar);
                }
            } catch (FileNotFoundException e) {
                log.error("File not found!", e);
            } catch (IOException e) {
                log.error("Unable to read from file!", e);
            } finally {
                try {
                    if (in != null) in.close();
                    if (out != null) out.close();
                } catch (IOException e1) {
                    log.error("Error closing file reader/writer", e1);
                }
            }
        }
    }

    /**
     * Returns global configuration.
     * 
     * @param session
     * @return @throws
     *         HibernateException
     */
    public static GlobalConfiguration getGlobalConfiguration(net.sf.hibernate.Session session) throws HibernateException {
        GlobalConfiguration config = (GlobalConfiguration) session.load(GlobalConfiguration.class, new Long(1));
        return config;
    }

    /**
     * Returns user with given ID.
     * 
     * @param session
     * @param id
     * @return @throws
     *         HibernateException
     */
    public static User getUserByID(net.sf.hibernate.Session session, Long id) throws HibernateException {
        return (User) session.load(User.class, id);
    }

    /**
     * Returns user with given name.
     * 
     * @param session
     * @param name
     * @return @throws
     *         HibernateException
     */
    public static User getUserByName(net.sf.hibernate.Session session, String name) throws HibernateException {
        List list = session.find("from User as u where u.username = ?", name, Hibernate.STRING);
        if (list.size() != 1) {
            throw new IllegalArgumentException("Unable to find user with name=" + name);
        }
        return (User) list.get(0);
    }

    /**
     * Returns instance of a default layout.
     * 
     * @param session
     * @return @throws
     *         HibernateException
     */
    public static Layout getDefaultLayout(net.sf.hibernate.Session session) throws HibernateException {
        List list = session.find("from Layout as l");
        Iterator it = list.iterator();
        while (it.hasNext()) {
            Layout l = (Layout) it.next();
            if (l.isDefault()) return l;
        }
        throw new HibernateException("Unable to find default layout!");
    }

    /**
     * Returns the website administrator.
     * 
     * @param session
     * @return @throws
     *         HibernateException
     */
    public static User getSystemAdministrator(net.sf.hibernate.Session session) throws HibernateException {
        return getGlobalConfiguration(session).getAdministrator();
    }

    /**
     * Returns group with a given ID.
     * 
     * @param session
     * @param id
     * @return @throws
     *         HibernateException
     */
    public static Group getGroupByID(net.sf.hibernate.Session session, Long id) throws HibernateException {
        return (Group) session.load(Group.class, id);
    }

    /**
     * Returns group with a given name.
     * 
     * @param session
     * @param name
     * @return @throws
     *         HibernateException
     */
    public static Group getGroupByName(net.sf.hibernate.Session session, String name) throws HibernateException {
        List list = session.find("from Group as g where g.name = ?", name, Hibernate.STRING);
        if (list.size() != 1) {
            throw new IllegalArgumentException("Unable to find group with name=" + name);
        }
        return (Group) list.get(0);
    }

    /**
     * Returns administrators-group.
     * 
     * @param session
     * @return @throws
     *         HibernateException
     */
    public static Group getAdministratorsGroup(net.sf.hibernate.Session session) throws HibernateException {
        return getGroupByName(session, "administrators");
    }

    /**
     * Returns moderators-group.
     * 
     * @param session
     * @return @throws
     *         HibernateException
     */
    public static Group getModeratorsGroup(net.sf.hibernate.Session session) throws HibernateException {
        return getGroupByName(session, "moderators");
    }

    /**
     * Returns users-group.
     * 
     * @param session
     * @return @throws
     *         HibernateException
     */
    public static Group getUsersGroup(net.sf.hibernate.Session session) throws HibernateException {
        return getGroupByName(session, "users");
    }

    /**
     * Returns everybody-group.
     * 
     * @param session
     * @return @throws
     *         HibernateException
     */
    public static Group getEverybodyGroup(net.sf.hibernate.Session session) throws HibernateException {
        return getGroupByName(session, "everybody");
    }

    /**
     * Return menu with a given ID.
     * 
     * @param session
     * @param id
     * @return @throws
     *         HibernateException
     */
    public static Menu getMenuByID(net.sf.hibernate.Session session, Long id) throws HibernateException {
        return (Menu) session.load(Menu.class, id);
    }

    /**
     * @param session
     * @param name
     * @return @throws
     *         HibernateException
     */
    public static Menu getMenuByName(net.sf.hibernate.Session session, String name) throws HibernateException {
        List list = session.find("from Menu as m where m.nameKey = ?", name, Hibernate.STRING);
        if (list.size() != 1) {
            throw new IllegalArgumentException("Unable to find menu with name=" + name);
        }
        return (Menu) list.get(0);
    }

    /**
     * @param session
     * @param id
     * @return @throws
     *         HibernateException
     */
    public static ExtensionPoint getExtensionPointByID(net.sf.hibernate.Session session, Long id) throws HibernateException {
        return (ExtensionPoint) session.load(ExtensionPoint.class, id);
    }

    /**
     * @param session
     * @param name
     * @return @throws
     *         HibernateException
     */
    public static ExtensionPoint getExtensionPointByName(net.sf.hibernate.Session session, String name) throws HibernateException {
        List list = session.find("from ExtensionPoint as m where m.name = ?", name, Hibernate.STRING);
        if (list.size() != 1) {
            throw new IllegalArgumentException("Unable to find extension-point with name=" + name);
        }
        return (ExtensionPoint) list.get(0);
    }

    /**
     * @param session
     * @return @throws
     *         HibernateException
     */
    public static Menu getSiteMenu(net.sf.hibernate.Session session) throws HibernateException {
        return getMenuByName(session, "SiteMenu");
    }

    /**
     * @param session
     * @return @throws
     *         HibernateException
     */
    public static Menu getUserMenu(net.sf.hibernate.Session session) throws HibernateException {
        return getMenuByName(session, "UserMenu");
    }

    /**
     * @param session
     * @return @throws
     *         HibernateException
     */
    public static Menu getSystemAdministrationMenu(net.sf.hibernate.Session session) throws HibernateException {
        return getMenuByName(session, "SystemAdminMenu");
    }

    /**
     * @param session
     * @return @throws
     *         HibernateException
     */
    public static Menu getPluginAdministrationMenu(net.sf.hibernate.Session session) throws HibernateException {
        return getMenuByName(session, "PluginAdminMenu");
    }

    /**
     * @param text
     * @return
     */
    public static String escapeHtml(String text) {
        text = text.replaceAll("<", "&lt;");
        text = text.replaceAll(">", "&gt;");
        return text;
    }

    /**
     * @param text
     * @return
     */
    public static String nl2br(String text) {
        return text.replaceAll("\n", "<br>");
    }

    public static String bbcode(String text) {
        return BBCodeRenderer.format(text);
    }
}
