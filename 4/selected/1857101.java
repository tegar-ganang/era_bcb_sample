package krico.javali.controller;

import java.io.*;
import java.awt.Image;
import java.awt.Font;
import krico.javali.util.MessageImage;
import krico.javali.util.CacheInfo;
import krico.javali.util.URLUtils;
import krico.javali.util.JavaliMarshalAgent;
import krico.javali.model.JavaliSession;
import krico.javali.model.JavaliResource;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.Date;
import java.util.Locale;

/**
 * This servlet creates dinamic images and caches them for the next time.
 */
public class JavaliDinamicImage extends HttpServlet {

    public static final String DINAMIC_IMAGE_CACHE_DIR = "DIMAGE-CACHE";

    public static final String CACHE_INFO = "cache-info.xml";

    /**
   * Use this parameter to specify what type of image you want
   * @see #TP_FONT
   * @see #TP_ICON
   * @see #TP_MENU
   */
    public static final String PRM_TYPE = "imageType";

    /**
   * Use this parameter to specify that you want a plain text image. You can define
   * text font, text color, etc.
   * @see #PRM_FONT_SIZE
   * @see #PRM_FONT_NAME
   * @see #PRM_FONT_TYPE
   * @see #PRM_FONT_COLOR
   * @see #PRM_FONT_SHADOWED
   * @see #PRM_FONT_SHADOW_COLOR
   */
    public static final String TP_FONT = "text_type";

    /**
   * Thist parameter specifies the size of the font to be used
   */
    public static final String PRM_FONT_SIZE = "fontSize";

    /**
   * Thist parameter specifies the name of the font to be used
   */
    public static final String PRM_FONT_NAME = "fontName";

    /**
   * Thist parameter specifies the type of the font to be used<br>
   * Can be <b>bold</b>, <b>plain</b>, <b>bolditalic</b>.
   */
    public static final String PRM_FONT_TYPE = "fontType";

    /**
   * Thist parameter specifies the color of the font to be used
   */
    public static final String PRM_FONT_COLOR = "fontColor";

    /**
   * Thist parameter specifies whether the text should be shadowed (boolean)
   */
    public static final String PRM_FONT_SHADOWED = "useShadow";

    /**
   * When the text is shadowed this parameter specifies the color of the font to be used as shadow
   */
    public static final String PRM_FONT_SHADOW_COLOR = "shadowColor";

    /**
   * Use this parameter to specify that you want an icon image.<BR>
   * <b>The icon</b> will be constructed as follows<br>
   * <ul>
   * <li>{@link #PRM_MENU_TYPE PRM_MENU_TYPE} specifies wheter it is text, icon or text and icon
   * <li>if the type needs text, then {@link #PRM_MENU_NAME PRM_MENU_NAME} is used as icon name and fetched using
   * {@link krico.javali.util.JavaliResource#getString JavaliResource.getString("icon." + iconName,loc)} where
   * loc is the locale fetched from the user, or {@link FormConstants#LOCALE_BINDING LOCALE_BINDING} from the session, or default.
   * If this text is null, the {@link #PRM_MENU_NAME PRM_MENU_NAME} is used as text.
   * <li>the iconName is used again to find out the iconFile with the folowing logic.
   * <ul>
   * <li>if the name does <b>not</b> start with a "<b>/</b>" the the file at "/icons/" + iconName + ".gif" is used
   * <li>if the name <b>does</b> start with a slash "<b>/</b>" the the file at iconName + ".gif" is used.
   * </ul>
   * <li>you can specify PRM_FONT_NAME, PRM_FONT_SIZE, PRM_FONT_COLOR and PRM_FONT_TYPE
   * </ul>
   */
    public static final String TP_ICON = "icon_type";

    /**
   * The name of the icon to be created
   * @see #TP_ICON
   */
    public static final String PRM_ICON_NAME = "iconName";

    /**
   * This creates a dinamic menu that can have the attributes
   * <table>
   * <tr><th>Property</th><th>Function</th></tr>
   *
   * <tr>
   * <td>{@link #PRM_MENU_NAME PRM_MENU_NAME}</td>
   * <td>The text to be used for this menu</td>
   * </tr>
   *
   *
   * <tr>
   * <td>{@link #PRM_MENU_COLOR PRM_MENU_COLOR} that is used only if
   * {@link #PRM_MENU_SELECTED PRM_MENU_SELECTED} is set to true</td>
   * <td>The color of this menu, can be any of {@link #MENU_COLOR_RED MENU_COLOR_RED},
   * {@link #MENU_COLOR_BLUE MENU_COLOR_BLUE},
   * {@link #MENU_COLOR_GREEN MENU_COLOR_GREEN},
   * {@link #MENU_COLOR_PURPLE MENU_COLOR_PURPLE},
   * {@link #MENU_COLOR_GRAY MENU_COLOR_GRAY},
   * {@link #MENU_COLOR_PLATINUM MENU_COLOR_PLATINUM}</td>
   * </tr>
   *
   *
   * <tr>
   * <td>{@link #PRM_MENU_TYPE PRM_MENU_TYPE}</td>
   * <td>The type of this menu</td>
   * </tr>
   *
   * </table>
   */
    public static final String TP_MENU = "menu_type";

    /**
   * @see #TP_MENU
   * @see #TP_ICON
   */
    public static final String PRM_MENU_NAME = "menuName";

    /**
   * @see #TP_MENU
   */
    public static final String PRM_MENU_COLOR = "menuColor";

    /**
   * @see #TP_MENU
   */
    public static final int MENU_COLOR_RED = 0;

    /**
   * @see #TP_MENU
   */
    public static final int MENU_COLOR_BLUE = 1;

    /**
   * @see #TP_MENU
   */
    public static final int MENU_COLOR_GREEN = 2;

    /**
   * @see #TP_MENU
   */
    public static final int MENU_COLOR_PURPLE = 3;

    /**
   * @see #TP_MENU
   */
    public static final int MENU_COLOR_GRAY = 4;

    /**
   * @see #TP_MENU
   */
    public static final int MENU_COLOR_PLATINUM = 5;

    /**
   * @see #TP_MENU
   */
    public static final String PRM_MENU_SELECTED = "selected";

    /**
   * Specify wether the type should be {@link #MENU_TYPE_TEXT MENU_TYPE_TEXT},
   * {@link #MENU_TYPE_TEXT MENU_TYPE_ICON}, 
   * {@link #MENU_TYPE_TEXT MENU_TYPE_TEXTICON}.
   * @see #TP_MENU
   * @see #TP_ICON
   */
    public static final String PRM_MENU_TYPE = "menuType";

    /**
   * @see #TP_MENU
   * @see #TP_ICON
   */
    public static final int MENU_TYPE_TEXT = 0;

    /**
   * @see #TP_MENU
   * @see #TP_ICON
   */
    public static final int MENU_TYPE_ICON = 1;

    /**
   * @see #TP_MENU
   * @see #TP_ICON
   */
    public static final int MENU_TYPE_TEXTICON = 2;

    public static final String PREFIX = "img";

    public static final String SUFIX = ".gif";

    File cacheDir = null;

    CacheInfo cacheInfo = null;

    JavaliMarshalAgent agent = null;

    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("image/gif");
        InputStream in = createImage(req, res);
        if (in == null) return;
        OutputStream out = res.getOutputStream();
        byte b[] = new byte[in.available()];
        in.read(b);
        in.close();
        out.write(b);
        out.close();
    }

    protected InputStream createImage(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String type = req.getParameter(PRM_TYPE);
        if (type == null || type.equals("")) type = TP_FONT;
        if (TP_FONT.equals(type)) return createTextType(req, res);
        if (TP_ICON.equals(type)) return createIconType(req, res);
        if (TP_MENU.equals(type)) return createMenuType(req, res);
        res.sendError(res.SC_NOT_FOUND);
        return null;
    }

    protected InputStream createMenuType(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        JavaliController.debug(JavaliController.LG_VERBOSE, "Creating menuType");
        String cHash = PRM_TYPE + "=" + TP_MENU;
        String menuName = req.getParameter("menuName");
        if (menuName == null) {
            res.sendError(res.SC_NOT_FOUND);
            return null;
        }
        Locale loc = null;
        HttpSession sess = req.getSession(false);
        JavaliSession jsess = null;
        if (sess != null) jsess = (JavaliSession) sess.getAttribute(FormConstants.SESSION_BINDING);
        if (jsess != null && jsess.getUser() != null) loc = jsess.getUser().getLocale(); else if (sess != null) loc = (Locale) sess.getAttribute(FormConstants.LOCALE_BINDING);
        if (loc == null) loc = Locale.getDefault();
        String menuText = JavaliResource.getString("menu." + menuName, loc);
        if (menuText == null) menuText = menuName;
        cHash += ", " + PRM_MENU_NAME + "=" + menuName + ", text=" + menuText;
        boolean selected = false;
        try {
            selected = new Boolean(req.getParameter(PRM_MENU_SELECTED)).booleanValue();
        } catch (Exception e) {
        }
        cHash += ", " + PRM_MENU_SELECTED + "=" + selected;
        int menuColor = 0;
        try {
            menuColor = new Integer(req.getParameter(PRM_MENU_COLOR)).intValue();
        } catch (Exception e) {
        }
        if (selected) cHash += ", " + PRM_MENU_SELECTED + "=" + menuColor;
        String fontName = req.getParameter(PRM_FONT_NAME);
        if (fontName == null) {
            fontName = "Helvetica";
        }
        cHash += "," + PRM_FONT_NAME + "=" + fontName;
        String fontTypeString = req.getParameter(PRM_FONT_TYPE);
        int fontType = Font.PLAIN;
        if ("PLAIN".equalsIgnoreCase(fontTypeString)) fontType = Font.PLAIN;
        if ("BOLD".equalsIgnoreCase(fontTypeString)) fontType = Font.BOLD;
        if ("ITALIC".equalsIgnoreCase(fontTypeString)) fontType = Font.ITALIC;
        if ("ITALICBOLD".equalsIgnoreCase(fontTypeString) || "BOLDITALIC".equalsIgnoreCase(fontTypeString) || "BOLD_ITALIC".equalsIgnoreCase(fontTypeString) || "ITALIC_BOLD".equalsIgnoreCase(fontTypeString)) {
            fontType = Font.ITALIC | Font.BOLD;
        }
        cHash += "," + PRM_FONT_TYPE + "=" + fontType;
        String fontColor = req.getParameter(PRM_FONT_COLOR);
        if (fontColor == null || fontColor.equals("")) fontColor = "0x000000";
        cHash += "," + PRM_FONT_COLOR + "=" + fontColor;
        String menuTypeString = req.getParameter(PRM_MENU_TYPE);
        int menuType = MENU_TYPE_TEXTICON;
        try {
            menuType = new Integer(menuTypeString).intValue();
        } catch (Exception e) {
        }
        cHash += "," + PRM_MENU_TYPE + "=" + menuType;
        String fontSizeString = req.getParameter(PRM_FONT_SIZE);
        int fontSize;
        try {
            fontSize = Integer.parseInt(fontSizeString);
        } catch (NumberFormatException nfe) {
            fontSize = 14;
        }
        cHash += "," + PRM_FONT_SIZE + "=" + fontSize;
        String fName = cacheInfo.file(cHash);
        JavaliController.debug(JavaliController.LG_VERBOSE, "Called for: " + fName);
        if (fName == null) {
            JavaliController.debug(JavaliController.LG_VERBOSE, "No cache found for: " + cHash);
            if (getServletConfig() != null && getServletConfig().getServletContext() != null) {
                String pref = selected ? "s" : "u";
                String suf = selected ? "" + menuColor : "";
                String left = getServletConfig().getServletContext().getRealPath("/icons/" + pref + "dbl" + suf + ".gif");
                String center = getServletConfig().getServletContext().getRealPath("/icons/" + pref + "dbc" + suf + ".gif");
                String right = getServletConfig().getServletContext().getRealPath("/icons/" + pref + "dbr" + suf + ".gif");
                File leftFile = new File(left);
                File centerFile = new File(center);
                File rightFile = new File(right);
                if (!leftFile.exists()) {
                    JavaliController.debug(JavaliController.LG_VERBOSE, "Could not find: " + leftFile);
                    res.sendError(res.SC_NOT_FOUND);
                    return null;
                }
                if (!centerFile.exists()) {
                    JavaliController.debug(JavaliController.LG_VERBOSE, "Could not find: " + centerFile);
                    res.sendError(res.SC_NOT_FOUND);
                    return null;
                }
                if (!rightFile.exists()) {
                    JavaliController.debug(JavaliController.LG_VERBOSE, "Could not find: " + rightFile);
                    res.sendError(res.SC_NOT_FOUND);
                    return null;
                }
                JavaliController.debug(JavaliController.LG_VERBOSE, "files l:" + leftFile + " c:" + centerFile + " r:" + rightFile + " and cHash=" + cHash);
                File tmp = File.createTempFile(PREFIX, SUFIX, cacheDir);
                OutputStream out = new FileOutputStream(tmp);
                File iconFile = new File(getServletConfig().getServletContext().getRealPath("/icons/" + menuName + ".gif"));
                if (menuType == MENU_TYPE_TEXT || iconFile == null || !iconFile.exists()) MessageImage.sendAsGIF(MessageImage.makeMenuImage(leftFile.getAbsolutePath(), centerFile.getAbsolutePath(), rightFile.getAbsolutePath(), menuText, fontName, fontColor, fontSize, fontType), out); else {
                    if (menuType == MENU_TYPE_TEXTICON) MessageImage.sendAsGIF(MessageImage.makeMenuIconImage(iconFile.getAbsolutePath(), leftFile.getAbsolutePath(), centerFile.getAbsolutePath(), rightFile.getAbsolutePath(), menuText, fontName, fontColor, fontSize, fontType), out); else MessageImage.sendAsGIF(MessageImage.makeMenuIconImage(iconFile.getAbsolutePath(), leftFile.getAbsolutePath(), centerFile.getAbsolutePath(), rightFile.getAbsolutePath(), fontName, fontColor, fontSize, fontType), out);
                }
                out.close();
                cacheInfo.putFile(cHash, tmp);
                fName = cacheInfo.file(cHash);
            } else {
                JavaliController.debug(JavaliController.LG_VERBOSE, "No ServletConfig=" + getServletConfig() + " or servletContext");
                res.sendError(res.SC_NOT_FOUND);
                return null;
            }
        }
        return new FileInputStream(new File(cacheDir, fName));
    }

    protected InputStream createIconType(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        JavaliController.debug(JavaliController.LG_VERBOSE, "Creating iconType");
        String cHash = PRM_TYPE + "=" + TP_ICON;
        String iconName = req.getParameter("iconName");
        if (iconName == null) {
            res.sendError(res.SC_NOT_FOUND);
            return null;
        }
        Locale loc = null;
        HttpSession sess = req.getSession(false);
        JavaliSession jsess = null;
        int menuType = -1;
        String menuTypeString = req.getParameter(PRM_MENU_TYPE);
        try {
            menuType = new Integer(menuTypeString).intValue();
        } catch (Exception e) {
        }
        if (sess != null) jsess = (JavaliSession) sess.getAttribute(FormConstants.SESSION_BINDING);
        if (jsess != null && jsess.getUser() != null) loc = jsess.getUser().getLocale(); else if (sess != null) loc = (Locale) sess.getAttribute(FormConstants.LOCALE_BINDING);
        if (loc == null) loc = Locale.getDefault();
        if (menuType == -1) menuType = MENU_TYPE_TEXTICON;
        String iconText = JavaliResource.getString("icon." + iconName, loc);
        if (iconText == null) {
            iconText = req.getParameter(PRM_MENU_NAME);
            if (iconText == null) iconText = "";
        }
        cHash += ", " + PRM_ICON_NAME + "=" + iconName + ", text=" + iconText + ", menuType=" + menuType;
        String iconFileName = null;
        String fontName = req.getParameter(PRM_FONT_NAME);
        if (fontName == null) {
            fontName = "Helvetica";
        }
        cHash += "," + PRM_FONT_NAME + "=" + fontName;
        String fontSizeString = req.getParameter(PRM_FONT_SIZE);
        int fontSize;
        try {
            fontSize = Integer.parseInt(fontSizeString);
        } catch (NumberFormatException nfe) {
            fontSize = 12;
        }
        cHash += "," + PRM_FONT_SIZE + "=" + fontSize;
        String fontTypeString = req.getParameter(PRM_FONT_TYPE);
        int fontType = Font.BOLD;
        if ("PLAIN".equalsIgnoreCase(fontTypeString)) fontType = Font.PLAIN;
        if ("BOLD".equalsIgnoreCase(fontTypeString)) fontType = Font.BOLD;
        if ("ITALIC".equalsIgnoreCase(fontTypeString)) fontType = Font.ITALIC;
        if ("ITALICBOLD".equalsIgnoreCase(fontTypeString) || "BOLDITALIC".equalsIgnoreCase(fontTypeString) || "BOLD_ITALIC".equalsIgnoreCase(fontTypeString) || "ITALIC_BOLD".equalsIgnoreCase(fontTypeString)) {
            fontType = Font.ITALIC | Font.BOLD;
        }
        cHash += "," + PRM_FONT_TYPE + "=" + fontType;
        String fontColor = req.getParameter(PRM_FONT_COLOR);
        if (fontColor == null || fontColor.equals("")) fontColor = "0x000000";
        cHash += "," + PRM_FONT_COLOR + "=" + fontColor;
        String fName = cacheInfo.file(cHash);
        JavaliController.debug(JavaliController.LG_VERBOSE, "Called for: " + fName);
        if (fName == null) {
            JavaliController.debug(JavaliController.LG_VERBOSE, "No cache found for: " + cHash);
            if (getServletConfig() != null && getServletConfig().getServletContext() != null) {
                if (iconName != null && iconName.startsWith("/")) iconFileName = getServletConfig().getServletContext().getRealPath(iconName + ".gif"); else iconFileName = getServletConfig().getServletContext().getRealPath("/icons/" + iconName + ".gif");
                File iconFile = new File(iconFileName);
                if (!iconFile.exists()) {
                    JavaliController.debug(JavaliController.LG_VERBOSE, "Could not find: " + iconFileName);
                    res.sendError(res.SC_NOT_FOUND);
                    return null;
                }
                iconFileName = iconFile.getAbsolutePath();
                JavaliController.debug(JavaliController.LG_VERBOSE, "file: " + iconFileName + " and cHash=" + cHash);
            } else {
                JavaliController.debug(JavaliController.LG_VERBOSE, "No ServletConfig=" + getServletConfig() + " or servletContext");
                res.sendError(res.SC_NOT_FOUND);
                return null;
            }
            File tmp = File.createTempFile(PREFIX, SUFIX, cacheDir);
            OutputStream out = new FileOutputStream(tmp);
            if (menuType == MENU_TYPE_ICON) {
                FileInputStream in = new FileInputStream(iconFileName);
                byte buf[] = new byte[2048];
                int read = -1;
                while ((read = in.read(buf)) != -1) out.write(buf, 0, read);
            } else if (menuType == MENU_TYPE_TEXT) MessageImage.sendAsGIF(MessageImage.makeMessageImage(iconText, fontName, fontSize, fontType, fontColor, false, "0x000000", true), out); else MessageImage.sendAsGIF(MessageImage.makeIconImage(iconFileName, iconText, fontName, fontColor, fontSize, fontType), out);
            out.close();
            cacheInfo.putFile(cHash, tmp);
            fName = cacheInfo.file(cHash);
        }
        return new FileInputStream(new File(cacheDir, fName));
    }

    protected InputStream createTextType(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        JavaliController.debug(JavaliController.LG_VERBOSE, "Creating textType");
        String cHash = PRM_TYPE + "=" + TP_FONT;
        String uri = req.getRequestURI();
        String ctx = req.getContextPath() + req.getServletPath() + "/";
        int idx = uri.indexOf(ctx);
        if (idx == -1) {
            res.sendError(res.SC_NOT_FOUND);
            return null;
        }
        String message = uri.substring(idx + ctx.length(), uri.length());
        message = URLUtils.decode(message);
        if ((message == null) || (message.length() == 0)) {
            message = "Missing 'message' parameter";
        }
        cHash += ",message=" + message;
        String fontName = req.getParameter(PRM_FONT_NAME);
        if (fontName == null) {
            fontName = "Serif";
        }
        cHash += "," + PRM_FONT_NAME + "=" + fontName;
        String fontSizeString = req.getParameter(PRM_FONT_SIZE);
        int fontSize;
        try {
            fontSize = Integer.parseInt(fontSizeString);
        } catch (NumberFormatException nfe) {
            fontSize = 12;
        }
        cHash += "," + PRM_FONT_SIZE + "=" + fontSize;
        String fontTypeString = req.getParameter(PRM_FONT_TYPE);
        int fontType = Font.PLAIN;
        if ("PLAIN".equalsIgnoreCase(fontTypeString)) fontType = Font.PLAIN;
        if ("BOLD".equalsIgnoreCase(fontTypeString)) fontType = Font.BOLD;
        if ("ITALIC".equalsIgnoreCase(fontTypeString)) fontType = Font.ITALIC;
        if ("ITALICBOLD".equalsIgnoreCase(fontTypeString) || "BOLDITALIC".equalsIgnoreCase(fontTypeString) || "BOLD_ITALIC".equalsIgnoreCase(fontTypeString) || "ITALIC_BOLD".equalsIgnoreCase(fontTypeString)) {
            fontType = Font.ITALIC | Font.BOLD;
        }
        cHash += "," + PRM_FONT_TYPE + "=" + fontType;
        String fontColor = req.getParameter(PRM_FONT_COLOR);
        if (fontColor == null || fontColor.equals("")) fontColor = "0x000000";
        cHash += "," + PRM_FONT_COLOR + "=" + fontColor;
        String useShadowString = req.getParameter(PRM_FONT_SHADOWED);
        boolean useShadow = false;
        try {
            useShadow = new Boolean(useShadowString).booleanValue();
        } catch (Exception e) {
        }
        cHash += "," + PRM_FONT_SHADOWED + "=" + useShadow;
        String shadowColor = req.getParameter(PRM_FONT_SHADOW_COLOR);
        if (shadowColor == null || shadowColor.equals("")) shadowColor = "0xcccccc";
        cHash += "," + PRM_FONT_SHADOW_COLOR + "=" + shadowColor;
        String fName = cacheInfo.file(cHash);
        JavaliController.debug(JavaliController.LG_VERBOSE, "Called for: " + fName);
        if (fName == null) {
            JavaliController.debug(JavaliController.LG_VERBOSE, "No cache found for: " + fName);
            File tmp = File.createTempFile(PREFIX, SUFIX, cacheDir);
            OutputStream out = new FileOutputStream(tmp);
            MessageImage.sendAsGIF(MessageImage.makeMessageImage(message, fontName, fontSize, fontType, fontColor, useShadow, shadowColor), out);
            out.close();
            cacheInfo.putFile(cHash, tmp);
            fName = cacheInfo.file(cHash);
            JavaliController.debug(JavaliController.LG_VERBOSE, "added cache with fName: " + fName + " and cHash=" + cHash);
        }
        return new FileInputStream(new File(cacheDir, fName));
    }

    public synchronized void destroy() {
        try {
            if (agent == null) JavaliController.debug(JavaliController.LG_WARN, "JavaliDinamicImage[marshal agent is null]"); else agent.marshal(cacheInfo, new File(cacheDir, CACHE_INFO).getAbsolutePath());
            JavaliController.debug(JavaliController.LG_WARN, "JavaliDinamicImage[DESTROYED unmarshaled cacheInfo]");
        } catch (Exception e) {
            JavaliController.debug(JavaliController.LG_WARN, "JavaliDinamicImage[Could not marshal cacheInfo]", e);
        }
    }

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        cacheDir = new File(JavaliController.getSystemConfig().getCacheDir());
        if (cacheDir == null || !cacheDir.exists() || !cacheDir.isDirectory()) {
            JavaliController.tell("JavaliDinamicImage[could not determine a cache dir.]");
            throw new ServletException("could not determina a cache dir");
        }
        cacheDir = new File(cacheDir, DINAMIC_IMAGE_CACHE_DIR);
        if (!cacheDir.exists()) {
            try {
                cacheDir.mkdir();
                JavaliController.debug(JavaliController.LG_WARN, "JavaliDinamicImage[Created cache dir dir=\"" + cacheDir.getAbsolutePath() + "\"]");
            } catch (Exception e) {
                JavaliController.tell("JavaliDinamicImage[could not create sub-directory for image caching dir=\"" + cacheDir.getAbsolutePath() + "\"]");
                throw new ServletException("could not create sub-directory for image caching dir=\"" + cacheDir.getAbsolutePath() + "\"");
            }
        } else {
            if (!cacheDir.isDirectory()) {
                JavaliController.tell("JavaliDinamicImage[could not create directory for caching for it is not a dir file=\"" + cacheDir.getAbsolutePath() + "\"]");
                throw new ServletException("could not create directory for caching for it is not a dir file=\"" + cacheDir.getAbsolutePath() + "\"");
            }
        }
        File info = new File(cacheDir, CACHE_INFO);
        agent = JavaliController.getSystemConfig().getMarshalAgent();
        if (!info.exists()) {
            JavaliController.debug(JavaliController.LG_WARN, "JavaliDinamicImage[NO CACHE INFO FOUND WILL CREATE A NEW ONE]");
            cacheInfo = new CacheInfo();
        } else {
            try {
                cacheInfo = (CacheInfo) agent.unmarshal(info.getAbsolutePath());
            } catch (Exception e) {
                JavaliController.tell("JavaliDinamicImage[Could not unmarshal cache info, will create a new one]", e);
                cacheInfo = new CacheInfo();
            }
        }
        JavaliController.tell("JavaliDinamicImage[STARTED]");
    }
}
