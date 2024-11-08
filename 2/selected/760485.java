package com.hadeslee.yoyoplayer.util;

import com.hadeslee.yoyoplayer.lyric.*;
import com.hadeslee.yoyoplayer.playlist.PlayListItem;
import com.hadeslee.yoyoplayer.setting.OptionDialog;
import com.hadeslee.yoyoplayer.setting.SettingPanel;
import com.sun.jna.examples.WindowUtils;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeListener;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

/**
 * 一个工具类，主要负责分析歌词
 * 并找到歌词下载下来，然后保存成标准格式的文件
 * 还有一些常用的方法
 * @author hadeslee
 */
public final class Util {

    public static String VERSION = "1.1.1";

    private static Logger log = Logger.getLogger(Util.class.getName());

    private static final JPanel panel = new JPanel();

    private static final JFileChooser jfc = new JFileChooser();

    private Util() {
    }

    /**
     * 检查更新新的一个很方便的方法,
     * 很多地方都可以调用到此方法,并且此方法
     * 负责弹出对话框让用户选择或者其它一些实现
     * @param ver 得到的版本对象
     */
    public static void checkUpdate(Version remote) {
        if (remote != null && remote.getVersion() != null) {
            if (Util.canUpdate(remote.getVersion())) {
                int i = JOptionPane.showConfirmDialog(null, Config.getResource("Util.currentVersion") + Util.VERSION + "\n" + Config.getResource("Util.remoteVersion") + remote.getVersion() + "\n" + Config.getResource("Util.versionDescription") + remote.getDescription() + Config.getResource("Util.areyouupdate"), Config.getResource("Util.hasUpdate"), JOptionPane.YES_NO_OPTION);
                if (i == JOptionPane.YES_OPTION) {
                    try {
                        Desktop.getDesktop().browse(new java.net.URI(remote.getUrl()));
                    } catch (Exception ex) {
                        Logger.getLogger(SettingPanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(null, Config.getResource("Util.currentVersion") + Util.VERSION + "\n" + Config.getResource("Util.remoteVersion") + remote.getVersion() + Config.getResource("Util.noUpdate"));
            }
        }
    }

    /**
     * 根据远程取到的版本和现在的版本对比
     * 看能不能更新
     * @param version 远程的版本
     * @return 能不能更新
     */
    private static boolean canUpdate(String version) {
        if (version == null) {
            return false;
        }
        return VERSION.compareTo(version) < 0;
    }

    public static boolean voteOpen() {
        try {
            HttpClient http = new HttpClient();
            http.getParams().setContentCharset("GBK");
            GetMethod get = new GetMethod("http://www.blogjava.net/Files/hadeslee/voteOpen.zip");
            get.addRequestHeader("Host", "www.blogjava.net");
            get.addRequestHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.8.1.11) Gecko/20071127 Firefox/2.0.0.11");
            get.addRequestHeader("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
            get.addRequestHeader("Accept-Language", "zh-cn,zh;q=0.5");
            get.addRequestHeader("Accept-Charset", "x-gbk,utf-8;q=0.7,*;q=0.7");
            get.addRequestHeader("Connection", "keep-alive");
            get.addRequestHeader("Referer", "http://www.blogjava.net/hadeslee");
            int i = http.executeMethod(get);
            get.releaseConnection();
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    public static boolean voteOneHour() {
        try {
            HttpClient http = new HttpClient();
            http.getParams().setContentCharset("GBK");
            GetMethod get = new GetMethod("http://www.blogjava.net/Files/hadeslee/voteOneHour.zip");
            get.addRequestHeader("Host", "www.blogjava.net");
            get.addRequestHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.8.1.11) Gecko/20071127 Firefox/2.0.0.11");
            get.addRequestHeader("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
            get.addRequestHeader("Accept-Language", "zh-cn,zh;q=0.5");
            get.addRequestHeader("Accept-Charset", "x-gbk,utf-8;q=0.7,*;q=0.7");
            get.addRequestHeader("Connection", "keep-alive");
            get.addRequestHeader("Referer", "http://www.blogjava.net/hadeslee");
            int i = http.executeMethod(get);
            get.releaseConnection();
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * 得到远程的版本的方法,返回一个字符串,
     * 格式为x.x.x然后再用方法做为比较
     * 版本的信息从哪里取的问题,一般版本的信息URL
     * 也是写在本类里面,因为所有的程序中,最有可能更新的就是
     * 本类,因为本类搜索歌词的代码可能会要改动
     * 因为可能网站会变化
     * @return 远程的版本对象
     */
    public static Version getRemoteVersion() {
        try {
            HttpClient http = new HttpClient();
            http.getParams().setContentCharset("GBK");
            GetMethod get = new GetMethod("http://www.blogjava.net/Files/hadeslee/version.zip");
            get.addRequestHeader("Host", "www.blogjava.net");
            get.addRequestHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.8.1.11) Gecko/20071127 Firefox/2.0.0.11");
            get.addRequestHeader("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
            get.addRequestHeader("Accept-Language", "zh-cn,zh;q=0.5");
            get.addRequestHeader("Accept-Charset", "x-gbk,utf-8;q=0.7,*;q=0.7");
            get.addRequestHeader("Connection", "keep-alive");
            get.addRequestHeader("Referer", "http://www.blogjava.net/hadeslee");
            int i = http.executeMethod(get);
            ZipInputStream zip = new ZipInputStream(get.getResponseBodyAsStream());
            ZipEntry entry = zip.getNextEntry();
            Properties pro = new Properties();
            pro.load(zip);
            String version = pro.getProperty("Version");
            String url = pro.getProperty("URL");
            String des = pro.getProperty("Description");
            log.log(Level.INFO, "RemoteVersion=" + version);
            get.releaseConnection();
            return new Version(version, url, des);
        } catch (IOException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    /**
     * 一个简便的生成一系列渐变颜色的方法,一般是生成128
     * 个颜个,供可视化窗口用
     * @param c1 第一种颜色
     * @param c2 第二种颜色
     * @param c3 第三种颜色
     * @param count 生成几种颜色
     * @return 渐变色
     */
    public static Color[] getColors(Color c1, Color c2, Color c3, int count) {
        Color[] cs = new Color[count];
        int half = count / 2;
        float addR = (c2.getRed() - c1.getRed()) * 1.0f / half;
        float addG = (c2.getGreen() - c1.getGreen()) * 1.0f / half;
        float addB = (c2.getBlue() - c1.getBlue()) * 1.0f / half;
        int r = c1.getRed();
        int g = c1.getGreen();
        int b = c1.getBlue();
        for (int i = 0; i < half; i++) {
            cs[i] = new Color((int) (r + i * addR), (int) (g + i * addG), (int) (b + i * addB));
        }
        addR = (c3.getRed() - c2.getRed()) * 1.0f / half;
        addG = (c3.getGreen() - c2.getGreen()) * 1.0f / half;
        addB = (c3.getBlue() - c2.getBlue()) * 1.0f / half;
        r = c2.getRed();
        g = c2.getGreen();
        b = c2.getBlue();
        for (int i = half; i < count; i++) {
            cs[i] = new Color((int) (r + (i - half) * addR), (int) (g + (i - half) * addG), (int) (b + (i - half) * addB));
        }
        return cs;
    }

    /**
     * 根据特定的颜色生成一个图标的方法
     * @param c 颜色
     * @param width 宽度
     * @param height 高度
     * @return 图标
     */
    public static ImageIcon createColorIcon(Color c, int width, int height) {
        BufferedImage bi = createImage(c, width, height);
        return new ImageIcon(bi);
    }

    /**
     * 根据特定的颜色,生成这个颜色的一张图片
     * 一般用于显示在图片按钮上做为ICON的
     * @param c 颜色
     * @param width 图片的宽度
     * @param height 图片的高度
     * @return 生成的图片
     */
    public static BufferedImage createImage(Color c, int width, int height) {
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = bi.createGraphics();
        g.setColor(c);
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(128, 128, 128));
        g.drawRect(0, 0, width - 1, height - 1);
        g.setColor(new Color(236, 233, 216));
        g.drawRect(1, 1, width - 3, height - 3);
        return bi;
    }

    /**
     * 快速地生成歌词秀上下文菜单的方法
     * @param pop 菜单 
     * @param lp 歌词面板
     */
    public static void generateLyricMenu(JMenu pop, final LyricPanel lp) {
        final Config config = Config.getConfig();
        JMenu adjust = new JMenu(Config.getResource("Util.adjustLyric"));
        JMenu showType = new JMenu(Config.getResource("Util.displayMode"));
        adjust.add(Config.getResource("Util.ff0.5")).addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                lp.getLyric().adjustTime(-500);
            }
        });
        adjust.add(Config.getResource("Util.ss0.5")).addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                lp.getLyric().adjustTime(500);
            }
        });
        adjust.add(Config.getResource("Util.adjustAll")).addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String s = JOptionPane.showInputDialog(Config.getResource("Util.inputAdjustTime") + Config.getResource("Util.adjustTip"));
                if (s != null) {
                    try {
                        int time = Integer.parseInt(s);
                        lp.getLyric().adjustTime(time);
                    } catch (Exception exe) {
                    }
                }
            }
        });
        adjust.addSeparator();
        final JCheckBoxMenuItem check = new JCheckBoxMenuItem(Config.getResource("Util.mouseWheelAdjust"));
        check.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                config.setMouseScrollAjustTime(check.isSelected());
            }
        });
        check.setSelected(config.isMouseScrollAjustTime());
        adjust.add(check);
        pop.add(Config.getResource("lyric.hideLyric")).addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                lp.hideMe();
            }
        });
        pop.add(Config.getResource("Util.searchOnline")).addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                new WebSearchDialog(lp.getPlayer().getCurrentItem(), lp).setVisible(true);
            }
        });
        pop.add(Config.getResource("Util.relativeLyric")).addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                JOptionPane.showMessageDialog(lp, Config.getResource("Util.relativeTip"));
            }
        });
        pop.add(Config.getResource("Util.undoLyric")).addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                lp.getPlayer().getCurrentItem().setLyricFile(null);
                lp.getLyric().setEnabled(false);
            }
        });
        pop.add(Config.getResource("Util.reloadLyric")).addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                Lyric ly = new Lyric(lp.getPlayer().getCurrentItem());
                lp.getPlayer().setLyric(ly);
                lp.setLyric(ly);
            }
        });
        ButtonGroup bg = new ButtonGroup();
        JRadioButtonMenuItem showH = new JRadioButtonMenuItem(Config.getResource("lyric.showLyricH"), config.getLpState() == LyricPanel.H);
        JRadioButtonMenuItem showV = new JRadioButtonMenuItem(Config.getResource("lyric.showLyricV"), config.getLpState() == LyricPanel.V);
        bg.add(showH);
        bg.add(showV);
        showType.add(showH);
        showType.add(showV);
        showType.addSeparator();
        showH.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                lp.setState(LyricPanel.H);
                config.setLpState(LyricPanel.H);
            }
        });
        showV.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                lp.setState(LyricPanel.V);
                config.setLpState(LyricPanel.V);
            }
        });
        final JCheckBoxMenuItem isAuto = new JCheckBoxMenuItem(Config.getResource("lyric.isAutoResize"), config.isAutoResize());
        isAuto.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                config.setAutoResize(isAuto.isSelected());
                if (isAuto.isSelected()) {
                    lp.setResized(false);
                }
            }
        });
        final JCheckBoxMenuItem isKaraoke = new JCheckBoxMenuItem(Config.getResource("lyric.isKaraoke"), config.isKaraoke());
        isKaraoke.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                config.setKaraoke(isKaraoke.isSelected());
            }
        });
        final JCheckBoxMenuItem isAnti = new JCheckBoxMenuItem(Config.getResource("Util.textAnti"), config.isAntiAliasing());
        isAnti.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                config.setAntiAliasing(isAnti.isSelected());
            }
        });
        final JCheckBoxMenuItem trans = new JCheckBoxMenuItem(Config.getResource("Util.bgTrans"), config.isTransparency());
        trans.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                config.setTransparency(trans.isSelected());
                if (trans.isSelected()) {
                    lp.getPlayer().getLyricUI().setBorderEnabled(config.isShowLrcBorder());
                    JDialog jd = lp.getPlayer().getLoader().changeLrcDialog();
                    WindowUtils.setWindowTransparent(jd, true);
                    lp.start();
                } else {
                    lp.getPlayer().getLyricUI().setBorderEnabled(true);
                    WindowUtils.setWindowTransparent(config.getLrcWindow(), false);
                }
            }
        });
        final JCheckBoxMenuItem showBorder = new JCheckBoxMenuItem(Config.getResource("Util.showBorder"), config.isShowLrcBorder());
        showBorder.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                boolean b = showBorder.isSelected();
                config.setShowLrcBorder(b);
                lp.getPlayer().getLyricUI().setBorderEnabled(b);
            }
        });
        showBorder.setEnabled(config.isTransparency());
        if (config.isLinux()) {
            trans.setEnabled(false);
            showBorder.setEnabled(false);
        }
        showType.add(isAuto);
        showType.add(isKaraoke);
        showType.add(isAnti);
        showType.add(trans);
        showType.add(showBorder);
        pop.add(adjust);
        pop.add(showType);
        final JCheckBoxMenuItem topShow = new JCheckBoxMenuItem(Config.getResource("Util.showOnTop"), config.isLyricTopShow());
        topShow.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                config.setLyricTopShow(topShow.isSelected());
                config.getLrcWindow().setAlwaysOnTop(topShow.isSelected());
            }
        });
        pop.add(topShow);
        pop.add(Config.getResource("Util.option")).addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                OptionDialog jd = config.getOptionDialog();
                jd.setSelected(Config.getResource("Util.lyric"));
                jd.setVisible(true);
            }
        });
    }

    /**
     * 快速地生成歌词秀上下文菜单的方法
     * @param pop 菜单 
     * @param lp 歌词面板
     */
    public static void generateLyricMenu(JPopupMenu pop, final LyricPanel lp) {
        final Config config = Config.getConfig();
        JMenu adjust = new JMenu(Config.getResource("Util.adjustLyric"));
        JMenu showType = new JMenu(Config.getResource("Util.displayMode"));
        adjust.add(Config.getResource("Util.ff0.5")).addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                lp.getLyric().adjustTime(-500);
            }
        });
        adjust.add(Config.getResource("Util.ss0.5")).addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                lp.getLyric().adjustTime(500);
            }
        });
        adjust.add(Config.getResource("Util.adjustAll")).addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String s = JOptionPane.showInputDialog(Config.getResource("Util.inputAdjustTime") + Config.getResource("Util.adjustTip"));
                if (s != null) {
                    try {
                        int time = Integer.parseInt(s);
                        lp.getLyric().adjustTime(time);
                    } catch (Exception exe) {
                    }
                }
            }
        });
        adjust.addSeparator();
        final JCheckBoxMenuItem check = new JCheckBoxMenuItem(Config.getResource("Util.mouseWheelAdjust"));
        check.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                config.setMouseScrollAjustTime(check.isSelected());
            }
        });
        check.setSelected(config.isMouseScrollAjustTime());
        adjust.add(check);
        pop.add(Config.getResource("lyric.hideLyric")).addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                lp.hideMe();
            }
        });
        pop.add(Config.getResource("Util.searchOnline")).addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                new WebSearchDialog(lp.getPlayer().getCurrentItem(), lp).setVisible(true);
            }
        });
        pop.add(Config.getResource("Util.relativeLyric")).addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                JOptionPane.showMessageDialog(lp, Config.getResource("Util.relativeTip"));
            }
        });
        pop.add(Config.getResource("Util.undoLyric")).addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                lp.getPlayer().getCurrentItem().setLyricFile(null);
                lp.getLyric().setEnabled(false);
            }
        });
        pop.add(Config.getResource("Util.reloadLyric")).addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                Lyric ly = new Lyric(lp.getPlayer().getCurrentItem());
                lp.getPlayer().setLyric(ly);
                lp.setLyric(ly);
            }
        });
        ButtonGroup bg = new ButtonGroup();
        JRadioButtonMenuItem showH = new JRadioButtonMenuItem(Config.getResource("lyric.showLyricH"), config.getLpState() == LyricPanel.H);
        JRadioButtonMenuItem showV = new JRadioButtonMenuItem(Config.getResource("lyric.showLyricV"), config.getLpState() == LyricPanel.V);
        bg.add(showH);
        bg.add(showV);
        showType.add(showH);
        showType.add(showV);
        showType.addSeparator();
        showH.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                lp.setState(LyricPanel.H);
                config.setLpState(LyricPanel.H);
            }
        });
        showV.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                lp.setState(LyricPanel.V);
                config.setLpState(LyricPanel.V);
            }
        });
        final JCheckBoxMenuItem isAuto = new JCheckBoxMenuItem(Config.getResource("lyric.isAutoResize"), config.isAutoResize());
        isAuto.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                config.setAutoResize(isAuto.isSelected());
                if (isAuto.isSelected()) {
                    lp.setResized(false);
                }
            }
        });
        final JCheckBoxMenuItem isKaraoke = new JCheckBoxMenuItem(Config.getResource("lyric.isKaraoke"), config.isKaraoke());
        isKaraoke.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                config.setKaraoke(isKaraoke.isSelected());
            }
        });
        final JCheckBoxMenuItem isAnti = new JCheckBoxMenuItem(Config.getResource("Util.textAnti"), config.isAntiAliasing());
        isAnti.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                config.setAntiAliasing(isAnti.isSelected());
            }
        });
        final JCheckBoxMenuItem trans = new JCheckBoxMenuItem(Config.getResource("Util.bgTrans"), config.isTransparency());
        trans.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                config.setTransparency(trans.isSelected());
                if (trans.isSelected()) {
                    lp.getPlayer().getLyricUI().setBorderEnabled(config.isShowLrcBorder());
                    JDialog jd = lp.getPlayer().getLoader().changeLrcDialog();
                    WindowUtils.setWindowTransparent(jd, true);
                    lp.start();
                } else {
                    lp.getPlayer().getLyricUI().setBorderEnabled(true);
                    WindowUtils.setWindowTransparent(config.getLrcWindow(), false);
                }
            }
        });
        final JCheckBoxMenuItem showBorder = new JCheckBoxMenuItem(Config.getResource("Util.showBorder"), config.isShowLrcBorder());
        showBorder.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                boolean b = showBorder.isSelected();
                config.setShowLrcBorder(b);
                lp.getPlayer().getLyricUI().setBorderEnabled(b);
            }
        });
        showBorder.setEnabled(config.isTransparency());
        if (config.isLinux()) {
            trans.setEnabled(false);
            showBorder.setEnabled(false);
        }
        showType.add(isAuto);
        showType.add(isKaraoke);
        showType.add(isAnti);
        showType.add(trans);
        showType.add(showBorder);
        pop.add(adjust);
        pop.add(showType);
        final JCheckBoxMenuItem topShow = new JCheckBoxMenuItem(Config.getResource("Util.showOnTop"), config.isLyricTopShow());
        topShow.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                config.setLyricTopShow(topShow.isSelected());
                config.getLrcWindow().setAlwaysOnTop(topShow.isSelected());
            }
        });
        pop.add(topShow);
        pop.add(Config.getResource("Util.option")).addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                OptionDialog jd = config.getOptionDialog();
                jd.setSelected(Config.getResource("Util.lyric"));
                jd.setVisible(true);
            }
        });
    }

    /**
     * 一个简单的方法,得到传进去的歌手和标题的
     * 歌词搜索结果,以一个列表形式返回
     * @param artist 歌手名,可能为空
     * @param title 歌名,不能为空
     * @return
     */
    public static List<SearchResult> getSearchResults(String artist, String title) {
        List<SearchResult> list = new ArrayList<SearchResult>();
        try {
            String s = Util.getSearchContent(artist, title);
            list.addAll(Util.parseURLContentToSearchResult(s));
        } catch (IOException ex) {
        }
        return list;
    }

    /**
     * 从一个int值得到它所代表的字节数组
     * @param i 值 
     * @return 字节数组
     */
    public static byte[] getBytesFromInt(int i) {
        byte[] data = new byte[4];
        data[0] = (byte) (i & 0xff);
        data[1] = (byte) ((i >> 8) & 0xff);
        data[2] = (byte) ((i >> 16) & 0xff);
        data[3] = (byte) ((i >> 24) & 0xff);
        return data;
    }

    /**
     * 把字符串转成系统预设的编码
     * @param source
     * @return
     */
    public static String convertString(String source) {
        return convertString(source, Config.getConfig().getEncoding());
    }

    /**
     * 一个简便的方法，把一个字符串的转成另一种字符串
     * @param source 源字符串
     * @param encoding 编码
     * @return 新的字符串
     */
    public static String convertString(String source, String encoding) {
        try {
            byte[] data = source.getBytes("ISO8859-1");
            return new String(data, encoding);
        } catch (UnsupportedEncodingException ex) {
            return source;
        }
    }

    /**
     * 转码的一个方便的方法
     * @param source 要转的字符串
     * @param sourceEnc 字符串原来的编码
     * @param distEnc 要转成的编码
     * @return 转后的字符串
     */
    public static String convertString(String source, String sourceEnc, String distEnc) {
        try {
            byte[] data = source.getBytes(sourceEnc);
            return new String(data, distEnc);
        } catch (UnsupportedEncodingException ex) {
            return source;
        }
    }

    /**
     * 从传进来的数得到这个数组
     * 组成的整型的大小
     * @param data 数组
     * @return 整型
     */
    public static int getInt(byte[] data) {
        if (data.length != 4) {
            throw new IllegalArgumentException("数组长度非法,要长度为4!");
        }
        return (data[0] & 0xff) | ((data[1] & 0xff) << 8) | ((data[2] & 0xff) << 16) | ((data[3] & 0xff) << 24);
    }

    /**
     * 从传进来的字节数组得到
     * 这个字节数组能组成的长整型的结果
     * @param data 字节数组
     * @return 长整型
     */
    public static long getLong(byte[] data) {
        if (data.length != 8) {
            throw new IllegalArgumentException("数组长度非法,要长度为4!");
        }
        return (data[0] & 0xff) | ((data[1] & 0xff) << 8) | ((data[2] & 0xff) << 16) | ((data[3] & 0xff) << 24) | ((data[4] & 0xff) << 32) | ((data[5] & 0xff) << 40) | ((data[6] & 0xff) << 48) | ((data[7] & 0xff) << 56);
    }

    /**
     * 得到相应的文件选择框,因为要求不一定一样
     * @param filter 
     * @param canDir 
     * @return
     */
    public static JFileChooser getFileChooser(FileNameFilter filter, int mode) {
        jfc.resetChoosableFileFilters();
        jfc.setFileSelectionMode(mode);
        jfc.setAcceptAllFileFilterUsed(false);
        jfc.setFileFilter(filter);
        jfc.setFileHidingEnabled(true);
        return jfc;
    }

    /**
     * 根据一个文件的全路径得到它的扩展名
     * @param path 全路径
     * @return  扩展名
     */
    public static String getExtName(String path) {
        return path.substring(path.lastIndexOf(".") + 1);
    }

    /**
     * 得到两个矩形的距离
     * @param rec1 矩形1
     * @param rec2 矩形2
     * @return 距离
     */
    public static int getDistance(Rectangle rec1, Rectangle rec2) {
        if (rec1.intersects(rec2)) {
            return Integer.MAX_VALUE;
        }
        int x1 = (int) rec1.getCenterX();
        int y1 = (int) rec1.getCenterY();
        int x2 = (int) rec2.getCenterX();
        int y2 = (int) rec2.getCenterY();
        int dis1 = Math.abs(x1 - x2) - rec1.width / 2 - rec2.width / 2;
        int dis2 = Math.abs(y1 - y2) - rec1.height / 2 - rec2.height / 2;
        return Math.max(dis1, dis2) - 1;
    }

    /**
     * 根据一些条件,统一生成一个滚动条
     * @param min 最小值 
     * @param max 最大值
     * @param value 当前值
     * @param ball1 球1
     * @param ball2 球2
     * @param ball3 球3
     * @param bg1 背景1
     * @param bg2 背景2
     * @param listener 监听器
     * @param orientation 方向
     * @return 滚动条
     */
    public static YOYOSlider createSlider(int min, int max, int value, Image ball1, Image ball2, Image ball3, Image bg1, Image bg2, ChangeListener listener, int orientation) {
        YOYOSlider yoyo = new YOYOSlider();
        YOYOSliderUI ui = new YOYOSliderUI(yoyo);
        yoyo.setOpaque(false);
        yoyo.setMaximum(max);
        yoyo.setMinimum(min);
        yoyo.setValue(value);
        yoyo.setOrientation(orientation);
        ui.setThumbImage(ball1);
        ui.setThumbOverImage(ball2);
        ui.setThumbPressedImage(ball3);
        ui.setBackgroundImages(bg1);
        ui.setActiveBackImage(bg2);
        yoyo.setUI(ui);
        yoyo.addChangeListener(listener);
        return yoyo;
    }

    /**
     * 根据一些参数快速地构造出按钮来
     * 这些按钮从外观上看都是一些特殊的按钮
     * @param name 按钮图片的相对地址
     * @param cmd 命令
     * @param listener 监听器
     * @return 按钮
     */
    public static JButton createJButton(String name, String cmd, ActionListener listener) {
        Image[] icons = Util.getImages(name, 3);
        JButton jb = new JButton();
        jb.setBorderPainted(false);
        jb.setFocusPainted(false);
        jb.setContentAreaFilled(false);
        jb.setDoubleBuffered(true);
        jb.setIcon(new ImageIcon(icons[0]));
        jb.setRolloverIcon(new ImageIcon(icons[1]));
        jb.setPressedIcon(new ImageIcon(icons[2]));
        jb.setOpaque(false);
        jb.setFocusable(false);
        jb.setActionCommand(cmd);
        jb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        jb.addActionListener(listener);
        return jb;
    }

    /**
     * 根据一些参数快速地构造出按钮来
     * 这些按钮从外观上看都是一些特殊的按钮
     * @param name 按钮图片的相对地址
     * @param cmd 命令
     * @param listener 监听器
     * @param selected 是否被选中了
     * @return 按钮
     */
    public static JToggleButton createJToggleButton(String name, String cmd, ActionListener listener, boolean selected) {
        Image[] icons = Util.getImages(name, 3);
        JToggleButton jt = new JToggleButton();
        jt.setBorder(null);
        jt.setContentAreaFilled(false);
        jt.setFocusPainted(false);
        jt.setDoubleBuffered(true);
        jt.setIcon(new ImageIcon(icons[0]));
        jt.setRolloverIcon(new ImageIcon(icons[1]));
        jt.setSelectedIcon(new ImageIcon(icons[2]));
        jt.setOpaque(false);
        jt.setFocusable(false);
        jt.setActionCommand(cmd);
        jt.setSelected(selected);
        jt.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        jt.addActionListener(listener);
        return jt;
    }

    /**
     * 得到一系列的图片，以数字递增做为序列的
     * @param who 图片的基名
     * @param count 数量
     * @return 图片数组
     */
    public static Image[] getImages(String who, int count) {
        Image[] imgs = new Image[3];
        MediaTracker mt = new MediaTracker(panel);
        Toolkit tk = Toolkit.getDefaultToolkit();
        for (int i = 1; i <= count; i++) {
            URL url = Util.class.getResource("/com/hadeslee/yoyoplayer/pic/" + who + i + ".png");
            imgs[i - 1] = tk.createImage(url);
            mt.addImage(imgs[i - 1], i);
        }
        try {
            mt.waitForAll();
        } catch (Exception exe) {
            exe.printStackTrace();
        }
        return imgs;
    }

    /**
     * 根据某个URL得到这个URL代表的图片
     * 并且把该图片导入内存
     * @param name URL
     * @return 图片
     */
    public static Image getImage(String name) {
        URL url = Util.class.getResource("/com/hadeslee/yoyoplayer/pic/" + name);
        Image im = Toolkit.getDefaultToolkit().createImage(url);
        try {
            MediaTracker mt = new MediaTracker(panel);
            mt.addImage(im, 0);
            mt.waitForAll();
        } catch (Exception exe) {
            exe.printStackTrace();
        }
        return im;
    }

    /**
     * 根据一个比例得到两种颜色之间的渐变色
     * @param c1 第一种颜色
     * @param c2 第二种颜色
     * @param f 比例
     * @return 新的颜色
     */
    public static Color getGradientColor(Color c1, Color c2, float f) {
        int deltaR = c2.getRed() - c1.getRed();
        int deltaG = c2.getGreen() - c1.getGreen();
        int deltaB = c2.getBlue() - c1.getBlue();
        int r1 = (int) (c1.getRed() + f * deltaR);
        int g1 = (int) (c1.getGreen() + f * deltaG);
        int b1 = (int) (c1.getBlue() + f * deltaB);
        Color c = new Color(r1, g1, b1);
        return c;
    }

    /**
     * 得到两种颜色的混合色
     * @param c1 第一种颜色
     * @param c2 第二种颜色
     * @return 混合色
     */
    public static Color getColor(Color c1, Color c2) {
        int r = (c2.getRed() + c1.getRed()) / 2;
        int g = (c2.getGreen() + c1.getGreen()) / 2;
        int b = (c2.getBlue() + c1.getBlue()) / 2;
        return new Color(r, g, b);
    }

    /**
     * 一个简便地获取字符串高度的方法
     * @param s 字符串
     * @param g 画笔
     * @return 高度
     */
    public static int getStringHeight(String s, Graphics g) {
        return (int) g.getFontMetrics().getStringBounds(s, g).getHeight();
    }

    /**
     * 一个简便地获取字符串宽度的方法
     * @param s 字符串
     * @param g 画笔
     * @return 宽度
     */
    public static int getStringWidth(String s, Graphics g) {
        return (int) g.getFontMetrics().getStringBounds(s, g).getWidth();
    }

    /**
     * 自定义的画字符串的方法，从字符串的左上角开始画
     * 不是JAVA的从左下角开始的画法
     * @param g 画笔
     * @param s 字符串
     * @param x X坐标
     * @param y Y坐标
     */
    public static void drawString(Graphics g, String s, int x, int y) {
        FontMetrics fm = g.getFontMetrics();
        int asc = fm.getAscent();
        g.drawString(s, x, y + asc);
    }

    /**
     * 一个便捷的方法,画字符串右对齐的方法
     * @param g 画笔
     * @param s 字符串
     * @param x 右对齐的X座标
     * @param y 右对齐的Y座标
     */
    public static void drawStringRight(Graphics g, String s, int x, int y) {
        FontMetrics fm = g.getFontMetrics();
        int asc = fm.getAscent();
        int width = getStringWidth(s, g);
        g.drawString(s, x - width, y + asc);
    }

    /**
     * 得到文件的格式
     * @param f 文件
     * @return 格式
     */
    public static String getType(File f) {
        String name = f.getName();
        return name.substring(name.lastIndexOf(".") + 1);
    }

    /**
     * 根据文件名得到歌曲的名字
     * @param f 文件名
     * @return 歌曲名
     */
    public static String getSongName(File f) {
        String name = f.getName();
        name = name.substring(0, name.lastIndexOf("."));
        return name;
    }

    /**
     * 根据文件名得到歌曲的名字
     * @param name 文件名
     * @return 歌曲名
     */
    public static String getSongName(String name) {
        try {
            int index = name.lastIndexOf(File.separator);
            name = name.substring(index + 1, name.lastIndexOf("."));
            return name;
        } catch (Exception exe) {
            return name;
        }
    }

    /**
     * 根据歌曲的信息去下载歌词内容
     * @param fileName 文件本名
     * @param info 歌曲信息
     * @return 歌词内容
     */
    public static String getLyric(PlayListItem info) throws IOException {
        log.log(Level.INFO, "进来找歌词了");
        log.log(Level.INFO, "先去JPWY搜索");
        String ly = getLyricJPWY(info);
        if (ly == null) {
            log.log(Level.INFO, "JPWY没有搜到，再去百度搜");
            ly = getLyricBaidu(info);
            log.log(Level.INFO, ly);
            return ly;
        } else {
            log.log(Level.INFO, "JPWY搜到了");
            log.log(Level.INFO, ly);
            return ly;
        }
    }

    /**
     * 根据地址去取得歌词的内容
     * 此方法专用于ＪＰＷＹ的内容
     * @param url 地址
     * @return 内容
     * @throws java.io.IOException
     */
    public static String getJPWY_Lyric(String url) throws IOException {
        HttpClient http = new HttpClient();
        Config config = Config.getConfig();
        if (config.isUseProxy()) {
            if (config.getProxyUserName() != null && config.getProxyPwd() != null) {
                http.getState().setProxyCredentials(new AuthScope(config.getProxyHost(), Integer.parseInt(config.getProxyPort())), new UsernamePasswordCredentials(config.getProxyUserName(), config.getProxyPwd()));
            }
            http.getHostConfiguration().setProxy(config.getProxyHost(), Integer.parseInt(config.getProxyPort()));
        }
        http.getParams().setContentCharset("GBK");
        GetMethod get = new GetMethod(url);
        get.addRequestHeader("Host", "www.jpwy.net");
        get.addRequestHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.8.1.9) Gecko/20071025 Firefox/2.0.0.9");
        get.addRequestHeader("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
        get.addRequestHeader("Accept-Language", "zh-cn,zh;q=0.5");
        get.addRequestHeader("Keep-Alive", "300");
        get.addRequestHeader("Connection", "keep-alive");
        get.addRequestHeader("Referer", "http://www.jpwy.net/gc/");
        int i = http.executeMethod(get);
        String s = getString(get.getResponseBodyAsStream());
        get.releaseConnection();
        return s;
    }

    /**
     * 从一个网页源码里面分析出歌词的下载地址
     * @param content 网页源码
     * @return 下载地址
     */
    private static List<String> parseURLContent(String content) {
        Matcher m = Pattern.compile("(?<=list.gif width=16 height=16></A></TD><TD align=middle><A href=\").*?(?=\"\\>\\<img border=0 src=image\\/lrc.gif>)").matcher(content);
        List<String> list = new ArrayList<String>();
        while (m.find()) {
            String url = m.group();
            list.add(url);
        }
        return list;
    }

    private static List<SearchResult> parseURLContentToSearchResult(String content) {
        Matcher m = Pattern.compile("(?<=<TD class=r1>).*?(?=src=image/lrc.gif></A></TD></TR>)").matcher(content);
        List<SearchResult> list = new ArrayList<SearchResult>();
        while (m.find()) {
            String s = m.group();
            SearchResult result = parseSearchResult(s);
            log.log(Level.INFO, result.toString());
            if (result != null) {
                list.add(result);
            }
        }
        return list;
    }

    private static SearchResult parseSearchResult(String content) {
        String title = null;
        String artist = null;
        final StringBuilder url = new StringBuilder();
        Matcher m = Pattern.compile("(?<=target=_blank>).*?(?=</A></TD><TD>&nbsp;</TD>)").matcher(content);
        if (m.find()) {
            title = m.group();
        }
        m = Pattern.compile("(?<=singer=([%[0-9a-fA-F]{2}|[\\w\\p{Punct}]]){0,100}\">).*?(?=</A></TD><TD>)").matcher(content);
        if (m.find()) {
            artist = m.group();
        }
        m = Pattern.compile("(?<=list.gif width=16 height=16></A></TD><TD align=middle><A href=\").*?(?=\"\\>\\<img border=)").matcher(content);
        if (m.find()) {
            url.append(m.group());
        }
        if (url.length() == 0) {
            return null;
        } else {
            return new SearchResult(artist, title, new SearchResult.Task() {

                public String getLyricContent() throws IOException {
                    return Util.getJPWY_Lyric(url.toString());
                }
            });
        }
    }

    /**
     * 根据歌名分析去得查询到的网页内容
     * 并原封不动把网页内容返回做为分析的数据
     * @param artist 歌手名,可能为空
     * @param title 歌曲名,不能为空
     * @throws java.io.IOException
     */
    private static String getSearchContent(String artist, String title) throws IOException {
        HttpClient http = new HttpClient();
        Config config = Config.getConfig();
        if (config.isUseProxy()) {
            log.log(Level.INFO, "使用了代理了：host:" + config.getProxyHost() + ",port:" + config.getProxyPort());
            if (config.getProxyUserName() != null && config.getProxyPwd() != null && !config.getProxyUserName().equals("") && !config.getProxyPwd().equals("")) {
                http.getState().setProxyCredentials(new AuthScope(config.getProxyHost(), Integer.parseInt(config.getProxyPort())), new UsernamePasswordCredentials(config.getProxyUserName(), config.getProxyPwd()));
            }
            http.getHostConfiguration().setProxy(config.getProxyHost(), Integer.parseInt(config.getProxyPort()));
        }
        http.getParams().setContentCharset("GBK");
        PostMethod get = postMethod();
        if (artist == null || artist.equals("")) {
            get.addParameter("song", title);
        } else {
            get.addParameter("singer", artist);
            get.addParameter("song", title);
        }
        int i = http.executeMethod(get);
        String s = getString(get.getResponseBodyAsStream());
        get.releaseConnection();
        return s;
    }

    /**
     * 根据歌名分析去得查询到的网页内容,也可能会为
     * 但尽最大可能去搜索到匹配的内容 
     * @param info 歌曲信息
     * @return 网页内容
     * @throws java.io.IOException
     */
    private static String getSearchContent(PlayListItem info) throws IOException {
        String urlContent = null;
        String song = info.getTitle();
        String singer = info.getArtist();
        HttpClient http = new HttpClient();
        Config config = Config.getConfig();
        if (config.isUseProxy()) {
            if (config.getProxyUserName() != null && config.getProxyPwd() != null) {
                http.getState().setProxyCredentials(new AuthScope(config.getProxyHost(), Integer.parseInt(config.getProxyPort())), new UsernamePasswordCredentials(config.getProxyUserName(), config.getProxyPwd()));
            }
            http.getHostConfiguration().setProxy(config.getProxyHost(), Integer.parseInt(config.getProxyPort()));
        }
        http.getParams().setContentCharset("GBK");
        PostMethod get = postMethod();
        if (singer == null || singer.equals("")) {
            get.addParameter("song", song);
        } else {
            get.addParameter("singer", singer);
            get.addParameter("song", song);
        }
        StringBuilder sb = new StringBuilder();
        int i = http.executeMethod(get);
        sb.append(getString(get.getResponseBodyAsStream()));
        get.releaseConnection();
        if (sb.toString().contains("对不起，没有找到")) {
            log.log(Level.INFO, "第一遍没有搜到,将第二次");
            sb = new StringBuilder();
            get = postMethod();
            get.addParameter("song", info.getName());
            i = http.executeMethod(get);
            log.log(Level.INFO, "第二次的I=" + i);
            sb.append(getString(get.getResponseBodyAsStream()));
            get.releaseConnection();
            if (sb.toString().contains("对不起，没有找到")) {
                if (info.getTitle().equals(info.getName())) {
                    log.log(Level.INFO, "第二次还没有搜到,放弃..");
                } else {
                    sb = new StringBuilder();
                    log.log(Level.INFO, "第二次还没有搜到,将第三次");
                    get = postMethod();
                    get.addParameter("song", info.getTitle());
                }
            } else {
                log.log(Level.INFO, "第二次搜到了");
                urlContent = sb.toString();
            }
        } else {
            log.log(Level.INFO, "第一次就搜到了");
            urlContent = sb.toString();
        }
        return urlContent;
    }

    /**
     * 得到那个Post方法
     * @return POST方法
     */
    private static PostMethod postMethod() {
        PostMethod post = new PostMethod("http://www.jpwy.net/gc/search.php");
        post.addRequestHeader("Host", "www.jpwy.net");
        post.addRequestHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.8.1.9) Gecko/20071025 Firefox/2.0.0.9");
        post.addRequestHeader("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
        post.addRequestHeader("Accept-Language", "zh-cn,zh;q=0.5");
        post.addRequestHeader("Keep-Alive", "300");
        post.addRequestHeader("Connection", "keep-alive");
        post.addRequestHeader("Referer", "http://www.jpwy.net/gc/");
        post.addParameter("submit", "歌词搜索");
        return post;
    }

    /**
     * 从一个流里面得到这个流的字符串
     * 表现形式
     * @param is 流
     * @return 字符串
     */
    private static String getString(InputStream is) {
        InputStreamReader r = null;
        try {
            StringBuilder sb = new StringBuilder();
            r = new InputStreamReader(is, "GBK");
            char[] buffer = new char[128];
            int length = -1;
            while ((length = r.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, length));
            }
            return sb.toString();
        } catch (Exception ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        } finally {
            try {
                r.close();
            } catch (Exception ex) {
                Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * 秒数转成00:00之类的字符串
     * @param sec 秒数
     * @return 字符串
     */
    public static String secondToString(int sec) {
        DecimalFormat df = new DecimalFormat("00");
        StringBuilder sb = new StringBuilder();
        sb.append(df.format(sec / 60)).append(":").append(df.format(sec % 60));
        return sb.toString();
    }

    /**
     * 得到URL的内容,最好是只限百度使用
     * @param url URL
     * @return 内容,可能是NULL
     * @throws java.lang.Exception
     */
    private static String getURLContent(String url) throws Exception {
        HttpClient http = new HttpClient();
        Config config = Config.getConfig();
        if (config.isUseProxy()) {
            if (config.getProxyUserName() != null && config.getProxyPwd() != null) {
                http.getState().setProxyCredentials(new AuthScope(config.getProxyHost(), Integer.parseInt(config.getProxyPort())), new UsernamePasswordCredentials(config.getProxyUserName(), config.getProxyPwd()));
            }
            http.getHostConfiguration().setProxy(config.getProxyHost(), Integer.parseInt(config.getProxyPort()));
        }
        http.getParams().setContentCharset("GBK");
        GetMethod get = new GetMethod();
        URI uri = new URI(url, false, "GBK");
        get.setURI(uri);
        int i = http.executeMethod(get);
        return getString(get.getResponseBodyAsStream());
    }

    /**
     * 得到在百度上搜索到的歌词的内容
     * @param key 关键内容
     * @return 内容
     * @throws java.lang.Exception
     */
    private static String getBaidu_Lyric(String key) throws Exception {
        HttpClient http = new HttpClient();
        Config config = Config.getConfig();
        if (config.isUseProxy()) {
            if (config.getProxyUserName() != null && config.getProxyPwd() != null) {
                http.getState().setProxyCredentials(new AuthScope(config.getProxyHost(), Integer.parseInt(config.getProxyPort())), new UsernamePasswordCredentials(config.getProxyUserName(), config.getProxyPwd()));
            }
            http.getHostConfiguration().setProxy(config.getProxyHost(), Integer.parseInt(config.getProxyPort()));
        }
        http.getParams().setContentCharset("GBK");
        GetMethod get = new GetMethod("http://mp3.baidu.com/m?f=ms&tn=baidump3lyric&ct=150994944&lf=2&rn=10&word=" + URLEncoder.encode(key, "GBK") + "&lm=0");
        get.addRequestHeader("Host", "mp3.baidu.com");
        get.addRequestHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.8.1.11) Gecko/20071127 Firefox/2.0.0.11");
        get.addRequestHeader("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
        get.addRequestHeader("Accept-Language", "zh-cn,zh;q=0.5");
        get.addRequestHeader("Keep-Alive", "300");
        get.addRequestHeader("Connection", "keep-alive");
        int i = http.executeMethod(get);
        String temp = getString(get.getResponseBodyAsStream());
        Pattern p = Pattern.compile("找到相关结果\\d+篇，用时\\d+\\.\\d+秒");
        Matcher m = p.matcher(temp);
        if (m.find()) {
            log.log(Level.INFO, m.group());
        } else {
            log.log(Level.INFO, "没有找到...");
        }
        m = Pattern.compile("(?<=<a href=\").*?(?=\"\\s*>LRC歌词</a>)").matcher(temp);
        String s = null;
        if (m.find()) {
            String url = m.group();
            log.log(Level.INFO, url);
            s = getURLContent(url);
        }
        get.releaseConnection();
        return s;
    }

    /**
     * 从百度去搜索歌词
     * @param info 播放项
     * @return 歌词内容，可能为NULL
     */
    private static String getLyricBaidu(PlayListItem info) {
        try {
            String song = info.getTitle();
            String name = info.getFormattedName();
            String s = getBaidu_Lyric(name);
            if (s == null) {
                s = getBaidu_Lyric(song);
                return s;
            } else {
                return s;
            }
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 从精品网页这个网站去搜索歌词
     * @param info 播放项
     * @return 歌词内容，可能为NULL
     */
    private static String getLyricJPWY(PlayListItem info) {
        try {
            String urlContent = getSearchContent(info);
            if (urlContent != null) {
                List<String> url = parseURLContent(urlContent);
                if (url.size() > 0) {
                    String ly = getJPWY_Lyric(url.get(0));
                    return ly;
                }
            }
            log.log(Level.INFO, "什么也没有找到");
            return null;
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * 用JDK标准的方式得到URL的内容
     * @param s URL
     * @throws java.lang.Exception
     */
    private static void getURLContentByJDK(String s) throws Exception {
        log.log(Level.INFO, "进来获取URL");
        URL url = new URL(s);
        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(), "GBK"));
        char[] buffer = new char[256];
        int length = -1;
        while ((length = br.read(buffer)) != -1) {
            System.out.print(new String(buffer, 0, length));
        }
    }

    static enum Test {

        Album, TITLE
    }

    public static void main(String[] args) throws Exception {
    }
}
