package net.jwpa.controller;

import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.jwpa.config.Config;
import net.jwpa.config.LogUtil;
import net.jwpa.config.Permission;
import net.jwpa.config.User;
import net.jwpa.dao.FolderDAO;
import net.jwpa.dao.MediaIndex;
import net.jwpa.dao.VirtualAlbumUtils;
import net.jwpa.model.Folder;
import net.jwpa.model.LocalizedProperty;
import net.jwpa.model.LocalizedPropertyHolder;
import net.jwpa.model.Media;
import net.jwpa.model.Metatag;
import net.jwpa.model.StringHistory;
import net.jwpa.tools.Tools;
import net.jwpa.view.Theme;

public class Utils {

    private static final Logger logger = LogUtil.getLogger();

    public static final String LOGIN_USER_NAME = "LOGIN_USER_NAME";

    public static final String LOGIN_PASSWORD_NAME = "LOGIN_PASSWORD_NAME";

    static final Object TAGS_LOCK = new Object();

    static final Object IMG_SCALE_LOCK = new Object();

    static boolean TAGS_REINDEX = false;

    public static String[] TAGS_REINDEX_STATUS = new String[] { "" };

    public static final String VERSION = "0.20 beta";

    public static final boolean DEV_BUILD = true;

    public static List<Locale> getLocales(HttpServletRequest request) {
        List<Locale> locales = new ArrayList<Locale>();
        Enumeration<Locale> e = request.getLocales();
        while (e.hasMoreElements()) {
            locales.add(e.nextElement());
        }
        return locales;
    }

    public static String getFileNameFromUploadName(String in) {
        int i1 = in.lastIndexOf("/");
        int i2 = in.lastIndexOf("\\");
        if (i1 == -1 && i2 == -1) return in; else return in.substring(Math.max(i1, i2) + 1);
    }

    public static void setLocalizedProperty(String propName, String paramName, LocalizedPropertyHolder holder, HttpServletRequest request) throws IOException {
        setLocalizedProperty(paramName, holder.getLocalizedProperty(propName), request);
    }

    public static void setLocalizedProperty(String paramName, LocalizedProperty lp, HttpServletRequest request) throws IOException {
        Enumeration<String> names = request.getParameterNames();
        String name;
        String defLang = Config.getCurrentConfig().getDefaultCommentLanguage();
        while (names.hasMoreElements()) {
            name = names.nextElement();
            if (name.startsWith(paramName)) {
                String value = request.getParameter(name);
                String lang = name.substring(paramName.length() + 1);
                if (lang.equals(defLang)) lp.setDefaultValue(value); else lp.setValue(value, lang);
            }
        }
    }

    public static String popReindexMessage() {
        String res = "";
        if (TAGS_REINDEX_STATUS[0] != null && TAGS_REINDEX_STATUS[0].length() > 0) res = TAGS_REINDEX_STATUS[0];
        if (!TAGS_REINDEX) {
            TAGS_REINDEX_STATUS[0] = "";
        }
        return res;
    }

    /**
	 * Highly unoptimized, used only for logging.
	 * @param data
	 * @return
	 */
    public static final String dumpArray(String[] data) {
        String res = "";
        for (String s : data) res += s + " ";
        return res;
    }

    public static String getNonNullString(String s) {
        return s == null ? "" : s;
    }

    public static boolean isStringEmpty(String s) {
        return s == null || s.length() == 0;
    }

    public static boolean areStringsEqual(String s, String t) {
        if (s == null && t == null) return true;
        if (s == null || t == null) return false;
        return s.equals(t);
    }

    public static int getPageNumber(HttpSession session, String template) {
        Integer page = null;
        try {
            page = (Integer) session.getAttribute("pageNum");
        } catch (Exception e) {
        }
        if (page == null) return 0; else return page;
    }

    public static Dimension getImageDimension(File image) {
        try {
            FileImageInputStream fiis = new FileImageInputStream(image);
            Iterator it = ImageIO.getImageReaders(fiis);
            fiis.close();
            ImageReader imageReader = (ImageReader) it.next();
            imageReader.setInput(fiis = new FileImageInputStream(image));
            int w = imageReader.getWidth(0);
            int h = imageReader.getHeight(0);
            fiis.close();
            return new Dimension(w, h);
        } catch (Exception e) {
            LogUtil.logWarn(logger, "Unable to detect image size - " + image.getAbsolutePath() + " cause: " + e);
            return null;
        }
    }

    public static StringProvider pumpStreamToByteArray(final InputStream is) {
        StringProvider res;
        new Thread(res = new StringProvider() {

            private ByteArrayOutputStream out = new ByteArrayOutputStream();

            private boolean done = false;

            public void run() {
                byte[] b = new byte[1024];
                try {
                    while (true) {
                        int count = is.read(b);
                        if (count < 0) return;
                        out.write(b, 0, count);
                        try {
                            if (count == 0) Thread.sleep(10);
                        } catch (InterruptedException e) {
                        }
                    }
                } catch (IOException e) {
                } finally {
                    done = true;
                }
            }

            public String getString() {
                while (!done) try {
                    Thread.sleep(10);
                } catch (Exception e) {
                }
                return new String(out.toByteArray());
            }
        }).start();
        return res;
    }

    public static void pumpStreamToVoid(final InputStream is) {
        new Thread(new Runnable() {

            public void run() {
                byte[] b = new byte[1024];
                try {
                    while (true) {
                        int count = is.read(b);
                        if (count < 0) return;
                        try {
                            if (count == 0) Thread.sleep(10);
                        } catch (InterruptedException e) {
                        }
                    }
                } catch (IOException e) {
                }
            }
        }).start();
    }

    public interface StringProvider extends Runnable {

        public String getString();
    }

    public static int getMax(Dimension d) {
        return Math.max(d.width, d.height);
    }

    public static BufferedImage getPartialSSImage(File f, int finalSize) throws IOException {
        FileImageInputStream fiis = new FileImageInputStream(f);
        try {
            Iterator it = ImageIO.getImageReaders(fiis);
            fiis.close();
            ImageReader imageReader = (ImageReader) it.next();
            imageReader.setInput(fiis = new FileImageInputStream(f));
            ImageReadParam rp = imageReader.getDefaultReadParam();
            int w = imageReader.getWidth(0);
            int h = imageReader.getHeight(0);
            int sz = Math.max(w, h);
            int ratio = sz / (finalSize * 2);
            if (ratio > 1) rp.setSourceSubsampling(ratio, ratio, 0, 0);
            BufferedImage image = imageReader.read(0, rp);
            return image;
        } catch (IOException e) {
            LogUtil.logError(logger, "Error on file " + f, e);
        } finally {
            fiis.close();
        }
        return null;
    }

    public static BufferedImage getPartialRSImage(File f, int finalSize) {
        try {
            Iterator it = ImageIO.getImageReaders(new FileImageInputStream(f));
            ImageReader imageReader = (ImageReader) it.next();
            imageReader.setInput(new FileImageInputStream(f));
            ImageReadParam rp = imageReader.getDefaultReadParam();
            if (rp.canSetSourceRenderSize()) rp.setSourceRenderSize(new Dimension(finalSize, finalSize));
            BufferedImage image = imageReader.read(0, rp);
            return image;
        } catch (IOException e) {
            LogUtil.logError(logger, "Error on file " + f, e);
        }
        return null;
    }

    public static User getCurrentUser(HttpServletRequest a_req) throws IOException {
        HttpSession session = a_req.getSession(true);
        User u = (User) session.getAttribute("auth.user");
        if (u != null) return u; else return Config.getCurrentConfig().getAnonymousUser();
    }

    public static Properties loadProperties(File file) throws IOException {
        Properties p = new Properties();
        if (file.exists()) {
            InputStream is = new BufferedInputStream(new FileInputStream(file));
            try {
                p.load(is);
            } finally {
                is.close();
            }
        }
        return p;
    }

    public static void saveProperties(Properties p, String title, File dest) throws IOException {
        FileOutputStream fos = new FileOutputStream(dest);
        try {
            p.store(fos, title);
        } finally {
            fos.close();
        }
    }

    public static List<Metatag> loadMetaTags() throws IOException {
        Properties p = new Properties();
        try {
            InputStream is = new BufferedInputStream(new FileInputStream(new File(Config.getCurrentConfig().getConfigFolderName(), "metatags.properties")));
            try {
                p.load(is);
            } finally {
                is.close();
            }
        } catch (IOException e) {
        }
        List<Metatag> res = new ArrayList<Metatag>(p.size());
        Enumeration e = p.propertyNames();
        while (e.hasMoreElements()) {
            Object o = e.nextElement();
            String n = String.valueOf(o);
            String vals = p.getProperty(n);
            String[] vaa = vals.split(",");
            Metatag mt = new Metatag();
            mt.setName(n);
            mt.setTags(Arrays.asList(vaa));
            res.add(mt);
        }
        Collections.sort(res);
        return res;
    }

    public static String generatePasswd() {
        String letters = "aezrtuyoipmljkhgfdqswxcvnb";
        String pass = "";
        while (pass.length() < 6) {
            pass += letters.charAt((int) (Math.random() * letters.length()));
        }
        return pass;
    }

    public static Theme getTheme(int theme, String themeLang, List<Locale> _locales) throws IOException {
        return new Theme(Config.getCurrentConfig().getThemes().get(theme), themeLang, _locales);
    }

    public static File getRelativeFile(String f) {
        return new File(Config.getCurrentConfig().getRootFolderName(), f);
    }

    public static String toRelativePath(File f) {
        return Tools.getRelativeFilename(f);
    }

    public static String reindexTags(Theme theme) throws IOException {
        if (!TAGS_REINDEX) {
            new Thread(new ReindexProcess(theme)).start();
            return theme.getLabel("label.tags.reindexStarted");
        } else return theme.getLabel("label.tags.reindexAlreadyStarted");
    }

    public static void updateFoldersIcons(String oldDir, String newDir, TplSerCtx context) throws IOException {
        updateFoldersIcons(oldDir, newDir, FolderDAO.getRootFolder());
    }

    public static void updateFoldersIcons(String oldDir, String newDir, Folder root) throws IOException {
        Properties p = root.getProperties();
        String icon = p.getProperty("thumb");
        if (icon != null) {
            if (icon.indexOf(oldDir) == 0) {
                icon = newDir + icon.substring(oldDir.length());
                root.setThumbFileName(Media.getInstance(icon));
            }
        }
        List<Folder> list = root.getFolders(true);
        for (Folder f : list) {
            updateFoldersIcons(oldDir, newDir, (Folder) f);
        }
    }

    public static Map<String, String> getContextMap(String img, String tags) {
        Map<String, String> res = new HashMap<String, String>();
        if (img != null && img.length() > 0) res.put("img", img);
        if (tags != null && tags.length() > 0) res.put("tags", tags);
        return res;
    }

    private static String getUrlParam(String value, String name, String prefix) throws IOException {
        if (value != null) return prefix + name + "=" + Tools.formatForURL(value); else return "";
    }

    public static void serveImage(String img, HttpServletResponse response) throws IOException {
        response.setHeader("content-type", "image/jpeg");
        response.setHeader("Content-Disposition", "inline; filename=\"" + new File(img).getName() + "\"");
        Calendar expires = new GregorianCalendar();
        expires.set(Calendar.MONTH, expires.get(Calendar.MONTH) + 1);
        response.addDateHeader("Expires", expires.getTimeInMillis());
        response.addIntHeader("Content-Length", (int) (new File(img).length()));
        OutputStream stream = response.getOutputStream();
        serveFile(img, stream);
    }

    public static void serveFile(String f, OutputStream stream) throws IOException {
        serveFile(new File(f), stream);
    }

    public static long getCRC(File f) throws IOException {
        InputStream is = new FileInputStream(f);
        CRC32 crc = new CRC32();
        byte[] data = new byte[1024];
        int read;
        while ((read = is.read(data)) > -1) {
            crc.update(data, 0, read);
        }
        return crc.getValue();
    }

    public static long getDummyCRC(long count) throws IOException {
        CRC32 crc = new CRC32();
        byte[] data = new byte[1024];
        int done = 0;
        while (done < count) {
            int tbw = (int) Math.min(count - done, data.length);
            crc.update(data, 0, tbw);
            done += tbw;
        }
        return crc.getValue();
    }

    public static void serveFile(File f, OutputStream stream) throws IOException {
        InputStream is = new FileInputStream(f);
        try {
            copyStream(is, stream);
        } finally {
            try {
                is.close();
            } catch (Exception e) {
            }
            try {
                stream.flush();
            } catch (Exception e) {
            }
        }
    }

    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] data = new byte[1024];
        int read;
        while ((read = in.read(data)) > -1) {
            out.write(data, 0, read);
        }
    }

    public static void serveDummyData(long count, OutputStream out) throws IOException {
        byte[] data = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        int done = 0;
        while (done < count) {
            int tbw = (int) Math.min(count - done, data.length);
            out.write(data, 0, tbw);
            done += tbw;
        }
    }

    public static void setOneMonthExpirationDate(HttpServletResponse response) {
        Calendar expires = new GregorianCalendar();
        expires.set(Calendar.MONTH, expires.get(Calendar.MONTH) + 1);
        response.addDateHeader("Expires", expires.getTimeInMillis());
        response.addHeader("Cache-Control", "public");
    }

    public static void setNoCache(HttpServletResponse response) {
        response.addHeader("Cache-Control", "no-cache");
        response.addHeader("Pragma", "no-cache");
    }

    public static void prepareImage(TplSerCtx context, String img, String mode, HttpServletResponse response) throws IOException {
        OutputStream out = response.getOutputStream();
        try {
            Media m = Media.getInstance(img);
            m.serveMedia(mode, response, out, context);
        } finally {
            try {
                out.flush();
            } catch (Exception e) {
            }
        }
    }

    public static void writeImage(BufferedImage im, OutputStream out) throws IOException {
        ImageWriter writer = null;
        Iterator iter = ImageIO.getImageWritersByFormatName("jpg");
        if (iter.hasNext()) {
            writer = (ImageWriter) iter.next();
        }
        ImageOutputStream ios = null;
        try {
            ios = ImageIO.createImageOutputStream(out);
            writer.setOutput(ios);
            ImageWriteParam iwparam = new JPEGImageWriteParam(Locale.getDefault());
            iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            iwparam.setCompressionQuality(0.90f);
            writer.write(null, new IIOImage(im, null, null), iwparam);
            ios.flush();
        } finally {
            try {
                writer.dispose();
            } catch (Exception e) {
            }
            try {
                ios.close();
            } catch (Exception e) {
            }
        }
    }

    public static BufferedImage readImage(File file, int[] sizes, int targetSize) throws IOException {
        BufferedImage i = getPartialSSImage(file, targetSize);
        sizes[0] = i.getWidth();
        sizes[1] = i.getHeight();
        return i;
    }

    public static BufferedImage scaleBufferedImage(BufferedImage im, double factor, ImageTransform targets) {
        if (factor < 0.5) {
            return scaleBufferedImage(scaleBufferedImage(im, 0.5d, null), factor / 0.5d, targets);
        } else {
            BufferedImage view = null;
            AffineTransform tx = new AffineTransform();
            if (targets == null) {
                view = new BufferedImage((int) Math.round(im.getWidth() * factor), (int) Math.round(im.getHeight() * factor), im.getType());
                tx.scale(view.getWidth() / ((double) im.getWidth()), view.getHeight() / ((double) im.getHeight()));
            } else {
                Dimension target = targets.getBounds();
                view = new BufferedImage(target.width, target.height, im.getType());
                tx.scale(target.width / ((double) im.getWidth()), target.height / ((double) im.getHeight()));
            }
            RenderingHints hints = new RenderingHints(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            hints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            hints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            hints.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            AffineTransformOp op = new AffineTransformOp(tx, hints);
            return op.filter(im, view);
        }
    }

    public static BufferedImage getBI(java.awt.Image i) {
        BufferedImage bi = new BufferedImage(i.getWidth(null), i.getHeight(null), BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics g = bi.getGraphics();
        g.drawImage(i, 0, 0, null);
        g.dispose();
        return bi;
    }

    public static boolean moveImageTo(String image, String destination) {
        File dest = new File(destination);
        if (!dest.exists()) dest.mkdir();
        if (new File(image).renameTo(new File(destination, new File(image).getName()))) {
            File[] list = new File(image).getParentFile().listFiles();
            for (File f : list) {
                String imagename = new File(image).getName();
                String n = f.getName();
                if (n.indexOf(imagename) == 0) {
                    f.renameTo(new File(destination, n));
                }
            }
            return true;
        }
        return false;
    }

    public static List<Folder> getFolderList(StringHistory sh) throws IOException {
        if (sh == null) return null;
        List<String> data = sh.get();
        List res = new ArrayList<Folder>(data.size());
        for (String s : data) {
            Folder f = FolderDAO.getFolder(s);
            res.add(f);
        }
        return res;
    }

    public static int getThemeIndex(String themeFullPath) {
        List<String> THEMES = Config.getCurrentConfig().getThemes();
        for (int i = 0; i < THEMES.size(); i++) {
            if (THEMES.get(i).equals(themeFullPath)) {
                return i;
            }
        }
        LogUtil.logError(logger, "Unable to find theme " + themeFullPath);
        return 0;
    }

    public static File getNewFile(File f) {
        if (!f.exists()) return f;
        int i = 2;
        while (true) {
            File res = new File(f.getParentFile(), Utils.getFileNameWithSequence(f, i));
            if (!res.exists()) return res;
            i++;
        }
    }

    public static boolean isZipFile(File f) {
        return Utils.getFileExtension(f).toLowerCase().equals("zip");
    }

    public static String getFileNameWithSequence(File f, int sequence) {
        String ext = Utils.getFileExtension(f);
        if (ext.length() > 0) ext = "." + ext;
        return Utils.getFileBaseName(f) + "_" + sequence + ext;
    }

    public static String getFileExtension(File f) {
        if (f.getName().lastIndexOf(".") < 0) return "";
        return f.getName().substring(f.getName().lastIndexOf(".") + 1);
    }

    public static String getFileBaseName(File f) {
        if (f.getName().lastIndexOf(".") < 0) return f.getName();
        return f.getName().substring(0, f.getName().lastIndexOf("."));
    }

    public static void addParameter(StringBuilder sb, String name, String value) throws UnsupportedEncodingException {
        if (sb.indexOf("?") > -1) sb.append("&"); else sb.append("?");
        sb.append(name).append("=").append(URLEncoder.encode(value, "UTF-8"));
    }

    public static void addTransmitParams(HttpServletRequest request, StringBuilder url) throws UnsupportedEncodingException {
        String params = request.getParameter("controller.transmitParams");
        if (!Utils.isStringEmpty(params)) {
            String[] pa = params.split(",");
            for (String paramName : pa) {
                String value = request.getParameter(paramName);
                if (!Utils.isStringEmpty(value)) {
                    addParameter(url, paramName, value);
                }
            }
        }
    }
}

class ReindexProcess implements Runnable {

    private static final Logger logger = LogUtil.getLogger();

    Theme theme;

    long startTime = System.currentTimeMillis();

    long imagesIndexCounter = 0l;

    Map<String, Long> imagesIndex = new HashMap<String, Long>();

    public ReindexProcess(Theme _theme) {
        theme = _theme;
    }

    public void run() {
        if (Utils.TAGS_REINDEX) return;
        synchronized (Utils.TAGS_LOCK) {
            if (Utils.TAGS_REINDEX) return;
            Utils.TAGS_REINDEX = true;
            try {
                startTime = System.currentTimeMillis();
                doReindexTags();
            } catch (Exception e) {
                Utils.TAGS_REINDEX_STATUS[0] = "Exception occured: " + e.getMessage();
                LogUtil.logError(logger, e);
            } finally {
                Utils.TAGS_REINDEX = false;
            }
        }
    }

    private void doReindexTags() throws IOException {
        Map<String, List<String>> props = new HashMap<String, List<String>>();
        int count = reindexTags(0, props, FolderDAO.getRootFolder());
        Properties p = new Properties();
        for (Map.Entry<String, List<String>> entry : props.entrySet()) {
            List<String> value = entry.getValue();
            StringBuffer valStr = new StringBuffer();
            for (String s : value) {
                if (valStr.length() > 0) valStr.append(",");
                valStr.append(s);
            }
            p.setProperty(entry.getKey(), valStr.toString());
        }
        File f = new File(Config.getCurrentConfig().getConfigFolderName(), "tags.properties");
        Utils.saveProperties(p, "Indexed tags", f);
        saveImageIndex();
        new VirtualAlbumUtils().reindexAllAlbums();
        Utils.TAGS_REINDEX_STATUS[0] += " " + theme.getLabel("label.tags.reindexDone", count, (System.currentTimeMillis() - startTime) / 1000);
    }

    private int reindexTags(int count, Map<String, List<String>> tags, Folder root) throws IOException {
        List<Media> images = root.getMediaList();
        for (Media i : images) {
            List<String> t = i.getTagsForIndex();
            if (t != null) {
                for (String tag : t) {
                    tag = tag.trim().toLowerCase();
                    if (tag.length() > 0) {
                        List<String> taglist = tags.get(tag);
                        if (taglist == null) {
                            taglist = new ArrayList<String>();
                            tags.put(tag, taglist);
                        }
                        taglist.add(String.valueOf(getImageIndex(i.getFile().getAbsolutePath())));
                    }
                }
            }
            count++;
            if (count % 1 == 0) {
                Utils.TAGS_REINDEX_STATUS[0] = theme.getLabel("label.tags.reindexInProgress", count, (System.currentTimeMillis() - startTime) / 1000);
            }
        }
        List<Folder> folders = root.getFolders(false);
        for (Folder f : folders) {
            count = reindexTags(count, tags, f);
        }
        return count;
    }

    private long getImageIndex(String imagePath) {
        Long l = imagesIndex.get(imagePath);
        if (l == null) {
            l = imagesIndexCounter++;
            imagesIndex.put(imagePath, l);
        }
        return l;
    }

    private void saveImageIndex() throws IOException {
        Properties p = new Properties();
        for (Map.Entry<String, Long> e : imagesIndex.entrySet()) {
            p.setProperty(String.valueOf(e.getValue()), e.getKey());
        }
        new MediaIndex().setIndex(p);
    }
}
