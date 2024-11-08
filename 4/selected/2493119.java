package com.javaeye.lonlysky.lforum.comm.utils;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Calendar;
import java.util.Random;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.apache.struts2.ServletActionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.javaeye.lonlysky.lforum.comm.LForumRequest;
import com.javaeye.lonlysky.lforum.comm.MD5;
import com.javaeye.lonlysky.lforum.comm.utils.des.DES;
import com.javaeye.lonlysky.lforum.config.impl.ConfigLoader;
import com.javaeye.lonlysky.lforum.entity.forum.AttachmentInfo;
import com.javaeye.lonlysky.lforum.entity.forum.Users;
import com.javaeye.lonlysky.lforum.entity.global.Config;
import com.sun.image.codec.jpeg.ImageFormatException;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

/**
 * 论坛相关操作
 * 
 * @author 黄磊
 *
 */
public class ForumUtils {

    private static final Logger logger = LoggerFactory.getLogger(ForumUtils.class);

    private ForumUtils() {
    }

    public static Pattern[] patterns = new Pattern[4];

    static {
        patterns[0] = Pattern.compile("(\r\n)");
        patterns[1] = Pattern.compile("(\n)");
        patterns[2] = Pattern.compile("(\r)");
        patterns[3] = Pattern.compile("(<br />)");
    }

    /**
	 * 获取当前用户Response
	 */
    public static HttpServletResponse getResponse() {
        return ServletActionContext.getResponse();
    }

    /**
	 * 返回论坛用户密码cookie明文
	 * @param key 密匙
	 * @return
	 */
    public static String getCookiePassword(String key) {
        DES des = new DES(key);
        return des.getDesString(getCookie("password")).trim();
    }

    /**
	 * 返回论坛用户密码cookie明文
	 * @param password 密碼
	 * @param key 密匙
	 * @return
	 */
    public static String getCookiePassword(String password, String key) {
        DES des = new DES(key);
        return des.getDesString(password).trim();
    }

    /**
	 * 返回密码密文
	 * @param password 密码
	 * @param key 密匙
	 * @return 加密结果
	 */
    public static String setCookiePassword(String password, String key) {
        DES des = new DES(key);
        return des.getEncString(password).trim();
    }

    /**
	 * 返回用户安全问题答案的存储数据
	 * @param questionid 问题ID
	 * @param answer 回答
	 * @return 加密数据
	 */
    public static String getUserSecques(int questionid, String answer) {
        if (questionid > 0) {
            return MD5.encode(answer + MD5.encode(questionid + "")).substring(8, 15);
        }
        return "";
    }

    /**
	 * 写论坛cookie值
	 * @param name	键
	 * @param value 值
	 */
    public static void writeCookie(String name, String value) {
        Cookie[] cookies = LForumRequest.getRequest().getCookies();
        Cookie wcookie = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    wcookie = cookie;
                }
            }
        }
        if (wcookie == null) {
            wcookie = new Cookie(name, Utils.urlEncode(value));
        } else {
            wcookie.setValue(Utils.urlEncode(value));
            if (wcookie.getMaxAge() > 0) {
                wcookie.setMaxAge(wcookie.getMaxAge());
            }
        }
        String domain = ConfigLoader.getConfig().getCookiedomain().trim();
        if (!domain.equals("") && LForumRequest.getRequest().getRemoteHost().indexOf(domain) != -1) {
            wcookie.setDomain(domain);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("写论坛cookie：name - {},value - {},age - " + wcookie.getMaxAge(), wcookie.getName(), wcookie.getValue());
        }
        getResponse().addCookie(wcookie);
    }

    /**
	 * 写论坛cookie值
	 * @param name	键
	 * @param intvalue 值
	 */
    public static void writeCookie(String name, int intvalue) {
        writeCookie(name, intvalue + "");
    }

    /**
	 * 写cookie值
	 * @param name 键
	 * @param value 值
	 * @param expires 保存期限
	 */
    public static void writeCookie(String name, String value, int expires) {
        Cookie[] cookies = LForumRequest.getRequest().getCookies();
        Cookie wCookie = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    wCookie = cookie;
                }
            }
        }
        if (wCookie == null) {
            wCookie = new Cookie(name, value);
        } else {
            wCookie.setValue(Utils.urlEncode(value));
        }
        wCookie.setMaxAge(expires);
        getResponse().addCookie(wCookie);
        if (logger.isDebugEnabled()) {
            logger.debug("写cookie：name - {},value - {},age - " + wCookie.getMaxAge(), wCookie.getName(), wCookie.getValue());
        }
    }

    /**
	 * 获取论坛cookie值
	 * @param name 名字
	 * @return 值
	 */
    public static String getCookie(String name) {
        Cookie[] cookies = LForumRequest.getRequest().getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("获取论坛cookie：name - {},value - {},age - " + cookie.getMaxAge(), cookie.getName(), Utils.urlDecode(cookie.getValue()).trim());
                    }
                    return Utils.urlDecode(cookie.getValue()).trim();
                }
            }
        }
        return "";
    }

    /**
	 * 写论坛登录用户的cookie
	 * @param user 用户信息
	 * @param expires cookie有效期
	 * @param passwordkey 用户密码Key
	 */
    public static void writeUserCookie(Users user, int expires, String passwordkey) {
        writeUserCookie(user, expires, passwordkey, 0, -1);
    }

    /**
	 * 写论坛登录用户的cookie
	 * @param user 用户信息
	 * @param expires cookie有效期
	 * @param passwordkey 用户密码
	 * @param templateid 用户当前要使用的界面风格
	 * @param invisible 用户当前的登录模式(正常或隐身)
	 */
    public static void writeUserCookie(Users user, int expires, String passwordkey, int templateid, int invisible) {
        if (user == null) {
            return;
        }
        Cookie[] cookies = new Cookie[9];
        Cookie cookie = new Cookie("userid", user.getUid().toString());
        cookies[0] = cookie;
        cookie = new Cookie("password", Utils.urlEncode(setCookiePassword(user.getPassword(), passwordkey)));
        cookies[1] = cookie;
        cookie = new Cookie("avatar", Utils.urlEncode(user.getUserfields().getAvatar()));
        cookies[2] = cookie;
        cookie = new Cookie("tpp", user.getTpp().toString());
        cookies[3] = cookie;
        cookie = new Cookie("ppp", user.getPpp().toString());
        cookies[4] = cookie;
        cookie = new Cookie("pmsound", user.getPmsound().toString());
        cookies[5] = cookie;
        if (invisible != 0 || invisible != 1) {
            invisible = user.getInvisible();
        }
        cookie = new Cookie("invisible", user.getInvisible().toString());
        cookies[6] = cookie;
        cookie = new Cookie("referer", "main.action");
        cookies[7] = cookie;
        cookie = new Cookie("sigstatus", user.getSigstatus().toString());
        cookies[8] = cookie;
        String domain = ConfigLoader.getConfig().getCookiedomain().trim();
        for (Cookie cok : cookies) {
            if (expires > 0) {
                cok.setMaxAge(expires);
            }
            if (!domain.equals("") && LForumRequest.getRequest().getRemoteHost().indexOf(domain) != -1) {
                cok.setDomain(domain);
            }
            getResponse().addCookie(cok);
            if (logger.isDebugEnabled()) {
                logger.debug("写用户cookie：name - {},value - {},age - " + cok.getMaxAge(), cok.getName(), cok.getValue());
            }
        }
        if (templateid > 0) {
            writeCookie("templateid", templateid + "", 999999);
        }
    }

    /**
	 * 清除登录用户cookie
	 */
    public static void clearUserCookie() {
        clearUserCookie("userid");
        clearUserCookie("password");
        clearUserCookie("avatar");
        clearUserCookie("tpp");
        clearUserCookie("ppp");
        clearUserCookie("pmsound");
        clearUserCookie("invisible");
        clearUserCookie("referer");
        clearUserCookie("sigstatus");
    }

    /**
	 * 清除用户Cookie
	 * 
	 * @param cookieName Cookie名称
	 */
    public static void clearUserCookie(String cookieName) {
        Cookie[] cookies = LForumRequest.getRequest().getCookies();
        Cookie clearCookie = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(cookieName)) {
                    clearCookie = cookie;
                }
            }
        }
        if (clearCookie != null) {
            clearCookie.setValue("");
            clearCookie.setMaxAge(-1);
            String domain = ConfigLoader.getConfig().getCookiedomain().trim();
            if (!domain.equals("") && LForumRequest.getRequest().getRemoteHost().indexOf(domain) != -1) {
                clearCookie.setDomain(domain);
            }
            getResponse().addCookie(clearCookie);
            if (logger.isDebugEnabled()) {
                logger.debug("清除用户Cookie,name - {}" + cookieName);
            }
        }
    }

    /**
	 * 从cookie中获取转向页
	 * @return
	 */
    public static String getReUrl() {
        if (!LForumRequest.getParamValue("reurl").equals("")) {
            writeCookie("reurl", LForumRequest.getParamValue("reurl"));
            return LForumRequest.getParamValue("reurl");
        } else {
            if (getCookie("reurl").equals("")) {
                return "main.action";
            } else {
                return getCookie("reurl");
            }
        }
    }

    /**
	 * 获得页码显示链接
	 * @param curPage 当前页数
	 * @param countPage 总页数
	 * @param url 超级链接地址
	 * @param extendPage 周边页码显示个数上限
	 * @return 页码html
	 */
    public static String getPageNumbers(int curPage, int countPage, String url, int extendPage) {
        return getPageNumbers(curPage, countPage, url, extendPage, "page");
    }

    /**
	 * 获得页码显示链接
	 * @param curPage 当前页数
	 * @param countPage 总页数
	 * @param url 超级链接地址
	 * @param extendPage 周边页码显示个数上限
	 * @param pagetag 页码标记
	 * @return 页码html
	 */
    public static String getPageNumbers(int curPage, int countPage, String url, int extendPage, String pagetag) {
        return getPageNumbers(curPage, countPage, url, extendPage, pagetag, null);
    }

    /**
	 * 获得页码显示链接
	 * @param curPage 当前页数
	 * @param countPage 总页数
	 * @param url 超级链接地址
	 * @param extendPage 周边页码显示个数上限
	 * @param pagetag 页码标记
	 * @param anchor 锚点
	 * @return 页码html
	 */
    public static String getPageNumbers(int curPage, int countPage, String url, int extendPage, String pagetag, String anchor) {
        if (pagetag == "") pagetag = "page";
        int startPage = 1;
        int endPage = 1;
        if (url.indexOf("?") > 0) {
            url = url + "&";
        } else {
            url = url + "?";
        }
        String t1 = "<a href=\"" + url + "&" + pagetag + "=1";
        String t2 = "<a href=\"" + url + "&" + pagetag + "=" + countPage;
        if (anchor != null) {
            t1 += anchor;
            t2 += anchor;
        }
        t1 += "\">&laquo;</a>";
        t2 += "\">&raquo;</a>";
        if (countPage < 1) countPage = 1;
        if (extendPage < 3) extendPage = 2;
        if (countPage > extendPage) {
            if (curPage - (extendPage / 2) > 0) {
                if (curPage + (extendPage / 2) < countPage) {
                    startPage = curPage - (extendPage / 2);
                    endPage = startPage + extendPage - 1;
                } else {
                    endPage = countPage;
                    startPage = endPage - extendPage + 1;
                    t2 = "";
                }
            } else {
                endPage = extendPage;
                t1 = "";
            }
        } else {
            startPage = 1;
            endPage = countPage;
            t1 = "";
            t2 = "";
        }
        StringBuilder s = new StringBuilder("");
        s.append(t1);
        for (int i = startPage; i <= endPage; i++) {
            if (i == curPage) {
                s.append("<span>");
                s.append(i);
                s.append("</span>");
            } else {
                s.append("<a href=\"");
                s.append(url);
                s.append(pagetag);
                s.append("=");
                s.append(i);
                if (anchor != null) {
                    s.append(anchor);
                }
                s.append("\">");
                s.append(i);
                s.append("</a>");
            }
        }
        s.append(t2);
        return s.toString();
    }

    /**
	 * 增加已访问版块id到历史记录cookie
	 * @param fid 要加入的版块id
	 */
    public static void updateVisitedForumsOptions(int fid) {
        if (getCookie("visitedforums") == "") {
            writeCookie("visitedforums", fid + "");
        } else {
            boolean fidExists = false;
            String[] strfid = getCookie("visitedforums").split(",");
            for (int fidIndex = 0; fidIndex < strfid.length; fidIndex++) {
                if (strfid[fidIndex].equals(fid + "")) {
                    fidExists = true;
                }
            }
            if (!fidExists) {
                writeCookie("visitedforums", fid + "," + getCookie("visitedforums"));
            }
        }
        return;
    }

    /**
	 * 返回当前页面是否是跨站提交
	 * @return 当前页面是否是跨站提交
	 */
    public static boolean isCrossSitePost() {
        if (!LForumRequest.isPost()) {
            return true;
        }
        return isCrossSitePost(LForumRequest.getUrlReferrer(), LForumRequest.getHost());
    }

    /**
	 * 判断是否是跨站提交
	 * @param urlReferrer 上个页面地址
	 * @param host 论坛url
	 * @return
	 */
    public static boolean isCrossSitePost(String urlReferrer, String host) {
        if (urlReferrer.length() < 7) {
            return true;
        }
        return URI.create(urlReferrer).getHost().equals(host);
    }

    /**
	 * 帖子中是否包含[hide]...[/hide]
	 * 
	 * @param str 帖子内容
	 * @return 是否包含
	 */
    public static boolean isHidePost(String str) {
        return (str.indexOf("[hide]") >= 0) && (str.indexOf("[/hide]") > 0);
    }

    /**
	 * 保存上传的文件
	 * @param forumid 版块id
	 * @param maxAllowFileCount 最大允许的上传文件个数
	 * @param maxSizePerDay 每天允许的附件大小总数
	 * @param maxFileSize 单个最大允许的文件字节数
	 * @param todayUploadedSize 今天已经上传的附件字节总数
	 * @param allowFileType 允许的文件类型, 以String[]形式提供
	 * @param watermarkstatus 图片水印位置
	 * @param config 附件保存方式 0=按年/月/日存入不同目录 1=按年/月/日/论坛存入不同目录 2=按论坛存入不同目录 3=按文件类型存入不同目录
	 * @param filekey File控件的Key(即Name属性)
	 * @return 文件信息结构
	 * @throws IOException 
	 * @throws Exception 
	 */
    public static AttachmentInfo[] saveRequestFiles(int forumid, int maxAllowFileCount, int maxSizePerDay, int maxFileSize, int todayUploadedSize, String allowFileType, int watermarkstatus, Config config, File[] postfiles, String[] postfileFileNames, String[] postfileContentTypes) throws IOException {
        System.out.println(allowFileType);
        String[] tmp = allowFileType.split("\r\n");
        String[] allowFileExtName = new String[tmp.length];
        int[] maxSize = new int[tmp.length];
        for (int i = 0; i < tmp.length; i++) {
            allowFileExtName[i] = tmp[i].substring(0, tmp[i].lastIndexOf(","));
            maxSize[i] = Utils.null2Int(tmp[i].substring(tmp[i].lastIndexOf(",") + 1), 0);
        }
        boolean isMultipart = postfiles.length > 0;
        if (isMultipart) {
            int saveFileCount = postfiles.length;
            if (logger.isDebugEnabled()) {
                logger.debug("最大允许的上传文件个数：" + maxAllowFileCount + ",当前附件数：" + saveFileCount);
            }
            AttachmentInfo[] attachmentinfo = new AttachmentInfo[saveFileCount];
            if (saveFileCount > maxAllowFileCount) {
                return attachmentinfo;
            }
            saveFileCount = 0;
            Random random = new Random(System.currentTimeMillis());
            for (int i = 0; i < postfiles.length; i++) {
                String filename = postfileFileNames[i];
                String fileextname = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
                String filetype = postfileContentTypes[i];
                int filesize = Utils.null2Int(postfiles[i].length());
                String newfilename = "";
                attachmentinfo[saveFileCount] = new AttachmentInfo();
                attachmentinfo[saveFileCount].setSys_noupload("");
                if (!(Utils.isImgFilename(filename) && !filetype.startsWith("image"))) {
                    int extnameid = Utils.getInArrayID(fileextname, allowFileExtName);
                    if (extnameid >= 0 && (filesize <= maxSize[extnameid]) && (maxFileSize >= filesize) && (maxSizePerDay - todayUploadedSize >= filesize)) {
                        todayUploadedSize = todayUploadedSize + filesize;
                        String uploadDir = config.getWebpath() + "upload/";
                        StringBuilder savedir = new StringBuilder("");
                        if (config.getAttachsave() == 1) {
                            savedir.append(Calendar.getInstance().get(Calendar.YEAR));
                            savedir.append("/");
                            savedir.append(Calendar.getInstance().get(Calendar.MONTH));
                            savedir.append("/");
                            savedir.append(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
                            savedir.append("/");
                            savedir.append(forumid);
                            savedir.append("/");
                        } else if (config.getAttachsave() == 2) {
                            savedir.append(forumid);
                            savedir.append("/");
                        } else if (config.getAttachsave() == 3) {
                            savedir.append(fileextname);
                            savedir.append("/");
                        } else {
                            savedir.append(Calendar.getInstance().get(Calendar.YEAR));
                            savedir.append("/");
                            savedir.append(Calendar.getInstance().get(Calendar.MONTH));
                            savedir.append("/");
                            savedir.append(Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
                            savedir.append("/");
                        }
                        newfilename = "" + (System.currentTimeMillis() & Integer.MAX_VALUE) + i + random.nextInt() + "." + fileextname;
                        newfilename = savedir.toString() + newfilename;
                        if ((fileextname.equals("bmp") || fileextname.equals("jpg") || fileextname.equals("jpeg") || fileextname.equals("png")) && filetype.startsWith("image")) {
                            BufferedImage img = ImageIO.read(postfiles[i]);
                            if (config.getAttachimgmaxwidth() > 0 && img.getWidth() > config.getAttachimgmaxwidth()) {
                                attachmentinfo[saveFileCount].setSys_noupload("图片宽度为" + img.getWidth() + ", 系统允许的最大宽度为" + config.getAttachimgmaxwidth());
                            }
                            if (config.getAttachimgmaxheight() > 0 && img.getHeight() > config.getAttachimgmaxheight()) {
                                attachmentinfo[saveFileCount].setSys_noupload("图片高度为" + img.getHeight() + ", 系统允许的最大高度为" + config.getAttachimgmaxheight());
                            }
                            if (attachmentinfo[saveFileCount].getSys_noupload().equals("")) {
                                FileUtils.copyFile(postfiles[i], new File(uploadDir + newfilename));
                                if (watermarkstatus != 0) {
                                    if (config.getWatermarktype() == 1 && Utils.fileExists(config.getWebpath() + "watermark/" + config.getWatermarkpic())) {
                                        addImageSignPic(img, uploadDir + newfilename, config.getWebpath() + "watermark/" + config.getWatermarkpic(), config.getWatermarkstatus(), config.getAttachimgquality(), (config.getWatermarktransparency() / 10.0f));
                                    } else {
                                        String watermarkText;
                                        watermarkText = config.getWatermarktext().replace("{1}", config.getForumtitle());
                                        watermarkText = watermarkText.replace("{2}", config.getForumurl());
                                        watermarkText = watermarkText.replace("{3}", Utils.getNowShortDate());
                                        watermarkText = watermarkText.replace("{4}", Utils.getNowShortTime());
                                        addImageSignText(img, uploadDir + newfilename, watermarkText, config.getWatermarkstatus(), config.getAttachimgquality(), config.getWatermarkfontname(), config.getWatermarkfontsize());
                                    }
                                }
                                attachmentinfo[saveFileCount].setFilesize(Utils.null2Int(new File(uploadDir + newfilename).length()));
                                attachmentinfo[saveFileCount].setFilesize(filesize);
                            }
                        } else {
                            attachmentinfo[saveFileCount].setFilesize(filesize);
                            FileUtils.copyFile(postfiles[i], new File(uploadDir + newfilename));
                        }
                    } else {
                        if (extnameid < 0) {
                            attachmentinfo[saveFileCount].setSys_noupload("文件格式无效");
                        } else if (maxSizePerDay - todayUploadedSize < filesize) {
                            attachmentinfo[saveFileCount].setSys_noupload("文件大于今天允许上传的字节数" + (maxSizePerDay - todayUploadedSize));
                        } else if (filesize > maxSize[extnameid]) {
                            attachmentinfo[saveFileCount].setSys_noupload("文件大于该类型附件允许的字节数" + maxSize[extnameid]);
                        } else {
                            attachmentinfo[saveFileCount].setSys_noupload("文件大于单个文件允许上传的字节数");
                        }
                    }
                } else {
                    attachmentinfo[saveFileCount].setSys_noupload("文件格式无效");
                }
                attachmentinfo[saveFileCount].setFilename(newfilename);
                attachmentinfo[saveFileCount].setDescription(fileextname);
                attachmentinfo[saveFileCount].setFiletype(filetype);
                attachmentinfo[saveFileCount].setAttachment(filename);
                attachmentinfo[saveFileCount].setDownloads(0);
                attachmentinfo[saveFileCount].setPostdatetime(Utils.getNowTime());
                attachmentinfo[saveFileCount].setSys_index(i);
                saveFileCount++;
            }
            return attachmentinfo;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("无上传附件");
            }
            return null;
        }
    }

    /**
	 * 保存上传头像
	 * @param file
	 * @param filename
	 * @param fileContentType
	 * @param userid
	 * @param maxAllowFileSize 最大允许的头像文件尺寸(单位:KB)
	 * @return 保存文件的相对路径
	 * @throws IOException 
	 */
    public static String saveRequestAvatarFile(File file, String filename, String fileContentType, int userid, int maxAllowFileSize) throws IOException {
        if (Utils.isImgFilename(filename)) {
            StringBuilder savedir = new StringBuilder();
            savedir.append("avatars/upload/");
            int t1 = (int) ((double) userid / (double) 10000);
            savedir.append(t1);
            savedir.append("/");
            int t2 = (int) ((double) userid / (double) 200);
            savedir.append(t2);
            savedir.append("/");
            String newfilename = savedir.toString() + userid + filename.substring(filename.lastIndexOf(".")).toLowerCase();
            if (file.length() <= maxAllowFileSize) {
                FileUtils.copyFile(file, new File(ConfigLoader.getConfig().getWebpath() + newfilename));
                if (logger.isDebugEnabled()) {
                    logger.debug("保存头像文件：{} 成功", newfilename);
                }
                return newfilename;
            }
        }
        return "";
    }

    /**
	 * 加图片水印
	 * @param img
	 * @param filename 文件名
	 * @param watermarkFilename 水印文件名
	 * @param watermarkStatus 图片水印位置
	 * @param quality 图片质量
	 * @param watermarkTransparency 透明度
	 * @throws IOException 
	 */
    public static void addImageSignPic(BufferedImage img, String filename, String watermarkFilename, int watermarkStatus, float quality, float watermarkTransparency) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("加图片水印:filename:{},watermarkFilename:" + watermarkFilename + ",watermarkStatus:" + watermarkStatus + ",quality:" + quality + ",watermarkTransparency:" + watermarkTransparency, filename);
        }
        Graphics2D g = img.createGraphics();
        BufferedImage watermark = ImageIO.read(new File(watermarkFilename));
        if (watermark.getHeight() >= img.getHeight() || watermark.getWidth() >= img.getWidth()) {
            return;
        }
        int xpos = 0;
        int ypos = 0;
        switch(watermarkStatus) {
            case 1:
                xpos = (int) (img.getWidth() * (float) .01);
                ypos = (int) (img.getHeight() * (float) .01);
                break;
            case 2:
                xpos = (int) ((img.getWidth() * (float) .50) - (watermark.getWidth() / 2));
                ypos = (int) (img.getHeight() * (float) .01);
                break;
            case 3:
                xpos = (int) ((img.getWidth() * (float) .99) - (watermark.getWidth()));
                ypos = (int) (img.getHeight() * (float) .01);
                break;
            case 4:
                xpos = (int) (img.getWidth() * (float) .01);
                ypos = (int) ((img.getHeight() * (float) .50) - (watermark.getHeight() / 2));
                break;
            case 5:
                xpos = (int) ((img.getWidth() * (float) .50) - (watermark.getWidth() / 2));
                ypos = (int) ((img.getHeight() * (float) .50) - (watermark.getHeight() / 2));
                break;
            case 6:
                xpos = (int) ((img.getWidth() * (float) .99) - (watermark.getWidth()));
                ypos = (int) ((img.getHeight() * (float) .50) - (watermark.getHeight() / 2));
                break;
            case 7:
                xpos = (int) (img.getWidth() * (float) .01);
                ypos = (int) ((img.getHeight() * (float) .99) - watermark.getHeight());
                break;
            case 8:
                xpos = (int) ((img.getWidth() * (float) .50) - (watermark.getWidth() / 2));
                ypos = (int) ((img.getHeight() * (float) .99) - watermark.getHeight());
                break;
            case 9:
                xpos = (int) ((img.getWidth() * (float) .99) - (watermark.getWidth()));
                ypos = (int) ((img.getHeight() * (float) .99) - watermark.getHeight());
                break;
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, watermarkTransparency));
        g.drawImage(watermark, xpos, ypos, watermark.getWidth(), watermark.getHeight(), null);
        g.dispose();
        FileOutputStream out = new FileOutputStream(filename);
        JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
        JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(img);
        param.setQuality(quality, true);
        encoder.encode(img, param);
        out.close();
    }

    /**
	 * 增加图片文字水印
	 * @param img
	 * @param filename 文件名
	 * @param watermarkText 水印文字
	 * @param watermarkStatus 图片水印位置
	 * @param quality 
	 * @param fontname 
	 * @param fontsize
	 * @throws ImageFormatException
	 * @throws IOException
	 */
    public static void addImageSignText(BufferedImage img, String filename, String watermarkText, int watermarkStatus, float quality, String fontname, int fontsize) throws ImageFormatException, IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("图片文字水印:filename:{},watermarkText:" + watermarkText + ",watermarkStatus:" + watermarkStatus + ",quality:" + quality + ",fontsize:{},fontname:" + fontname, filename, fontsize);
        }
        Graphics2D g = img.createGraphics();
        Font drawFont = new Font(fontname, Font.BOLD, fontsize);
        Float xpos = new Float(0);
        Float ypos = new Float(0);
        switch(watermarkStatus) {
            case 1:
                xpos = (float) img.getWidth() * (float) .01;
                ypos = (float) img.getHeight() * (float) .01;
                break;
            case 2:
                xpos = ((float) img.getWidth() * (float) .50) - (g.getFontMetrics(drawFont).stringWidth(watermarkText) / 2);
                ypos = (float) img.getHeight() * (float) .01;
                break;
            case 3:
                xpos = ((float) img.getWidth() * (float) .99) - g.getFontMetrics(drawFont).stringWidth(watermarkText);
                ypos = (float) img.getHeight() * (float) .01;
                break;
            case 4:
                xpos = (float) img.getWidth() * (float) .01;
                ypos = ((float) img.getHeight() * (float) .50) - (g.getFontMetrics(drawFont).getHeight() / 2);
                break;
            case 5:
                xpos = ((float) img.getWidth() * (float) .50) - (g.getFontMetrics(drawFont).stringWidth(watermarkText) / 2);
                ypos = ((float) img.getHeight() * (float) .50) - (g.getFontMetrics(drawFont).getHeight() / 2);
                break;
            case 6:
                xpos = ((float) img.getWidth() * (float) .99) - g.getFontMetrics(drawFont).stringWidth(watermarkText);
                ypos = ((float) img.getHeight() * (float) .50) - (g.getFontMetrics(drawFont).getHeight() / 2);
                break;
            case 7:
                xpos = (float) img.getWidth() * (float) .01;
                ypos = ((float) img.getHeight() * (float) .99) - g.getFontMetrics(drawFont).getHeight();
                break;
            case 8:
                xpos = ((float) img.getWidth() * (float) .50) - (g.getFontMetrics(drawFont).stringWidth(watermarkText) / 2);
                ypos = ((float) img.getHeight() * (float) .99) - g.getFontMetrics(drawFont).getHeight();
                break;
            case 9:
                xpos = ((float) img.getWidth() * (float) .99) - g.getFontMetrics(drawFont).stringWidth(watermarkText);
                ypos = ((float) img.getHeight() * (float) .99) - g.getFontMetrics(drawFont).getHeight();
                break;
        }
        g.setFont(drawFont);
        g.setColor(Color.WHITE);
        g.drawString(watermarkText, xpos.intValue() + 1, ypos.intValue() + 1);
        g.setColor(Color.BLACK);
        g.drawString(watermarkText, xpos.intValue(), ypos.intValue());
        g.dispose();
        FileOutputStream out = new FileOutputStream(filename);
        JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
        JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(img);
        param.setQuality(quality, true);
        encoder.encode(img, param);
        out.close();
    }

    /**
	 * 以指定的ContentType输出指定文件文件
	 * @param filepath 文件路径
	 * @param filename 输出的文件名
	 * @param filetype 将文件输出时设置的ContentType
	 * @throws IOException
	 */
    public static void responseFile(String filepath, String filename, String filetype) throws IOException {
        InputStream iStream = null;
        byte[] buffer = new byte[10000];
        int length;
        try {
            iStream = new FileInputStream(filepath);
            ServletActionContext.getResponse().reset();
            ServletActionContext.getResponse().setContentType(filetype);
            ServletActionContext.getResponse().setHeader("Content-Disposition", "attachment;filename=\"" + Utils.urlEncode(filename.trim()).replace("+", " ") + "\"");
            if (logger.isDebugEnabled()) {
                logger.debug("文件类型：" + filetype + ",文件名：" + filename);
                logger.debug("头部信息：" + "attachment;filename=" + Utils.urlEncode(filename.trim()).replace("+", " "));
            }
            while ((length = iStream.read(buffer)) > 0) {
                ServletActionContext.getResponse().getOutputStream().write(buffer, 0, length);
            }
        } catch (Exception ex) {
            ServletActionContext.getResponse().getWriter().write("Error : " + ex.getMessage());
        } finally {
            if (iStream != null) {
                iStream.close();
            }
        }
    }

    /**
	 * 是否是过滤的用户名
	 * @param str
	 * @param stringarray
	 * @return
	 */
    public static boolean isBanUsername(String str, String stringarray) {
        if (stringarray == null || stringarray.equals("")) return false;
        stringarray = stringarray.replace("*", "[\\s\\S]\\*").replace("?", "[\\?]");
        for (String strarray : stringarray.split("\n")) {
            Pattern pattern = Pattern.compile("^" + strarray + "$");
            if (pattern.matcher(str).find() && (!strarray.trim().equals(""))) {
                return true;
            }
        }
        return false;
    }
}
