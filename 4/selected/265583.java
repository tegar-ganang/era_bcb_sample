package net.xiaoxishu.util;

import static net.xiaoxishu.util.MessageUtil.getMessage;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.FileChannel;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileFilter;
import org.jdesktop.jdic.desktop.Desktop;
import org.jdesktop.jdic.desktop.DesktopException;
import net.xiaoxishu.util.sys.ClipBoard;
import net.xiaoxishu.util.sys.ScreenCapture;

/**
 * 以JDialog的方式显示各种资源(text,link,image,system msg)
 * 
 * @author lushu
 * 
 */
public class DialogHelper {

    private JDialog jd;

    private JTextArea jt;

    private JPopupMenu popMenu;

    private JMenuItem copy;

    private JMenuItem copyQuit;

    public static void showText(String title, String msg) {
        DialogHelper d = new DialogHelper(title, msg, null);
        d.jd.setVisible(true);
    }

    public static void showLink(String title, String msg, URL url) {
        DialogHelper d = new DialogHelper(title, msg, url);
        d.jd.setVisible(true);
    }

    public static void showImage(String title, URL url) {
        DialogHelper d = new DialogHelper(title, url);
        d.jd.setVisible(true);
    }

    private DialogHelper(String title, String msg, final URL url) {
        jd = new JDialog();
        jd.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        jd.setAlwaysOnTop(true);
        jd.setLayout(new BoxLayout(jd.getContentPane(), BoxLayout.Y_AXIS));
        jd.setTitle(title);
        jd.setPreferredSize(new Dimension(500, 300));
        jt = new JTextArea(20, 40);
        jt.setText(msg);
        jt.setEditable(false);
        jt.setLineWrap(true);
        jt.setWrapStyleWord(true);
        jt.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    if (ClipBoard.isValid && jt.getSelectionStart() != jt.getSelectionEnd()) {
                        copy.setEnabled(true);
                        copyQuit.setEnabled(true);
                    } else {
                        copy.setEnabled(false);
                        copyQuit.setEnabled(false);
                    }
                    popMenu.show(jt, e.getX(), e.getY());
                }
            }
        });
        JScrollPane jsp = new JScrollPane(jt);
        jsp.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10), BorderFactory.createEtchedBorder()));
        jd.add(jsp);
        JPanel jp = new JPanel();
        JButton jb;
        if (url != null) {
            jb = new JButton(getMessage("btn_open_link"));
            jb.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    try {
                        Desktop.browse(url);
                    } catch (DesktopException e1) {
                        warn(jd, getMessage("msg_failed_open_browser"));
                    }
                }
            });
            jp.add(jb);
        }
        jb = new JButton(getMessage("btn_copy"));
        if (!ClipBoard.isValid) jb.setEnabled(false);
        jb.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ClipBoard.save(jt.getSelectionStart() == jt.getSelectionEnd() ? jt.getText() : jt.getSelectedText());
            }
        });
        jp.add(jb);
        jb = new JButton(getMessage("btn_close"));
        jb.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jd.dispose();
            }
        });
        jp.add(jb);
        jd.add(jp);
        popMenu = new JPopupMenu();
        copy = new JMenuItem(getMessage("copy"));
        copy.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ClipBoard.save(jt.getSelectedText());
            }
        });
        copy.setEnabled(ClipBoard.isValid);
        popMenu.add(copy);
        copyQuit = new JMenuItem(getMessage("copy_close"));
        copyQuit.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ClipBoard.save(jt.getSelectedText());
                jd.dispose();
            }
        });
        copyQuit.setEnabled(ClipBoard.isValid);
        popMenu.add(copyQuit);
        popMenu.addSeparator();
        JMenuItem jm = new JMenuItem(getMessage("copy_all"));
        jm.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ClipBoard.save(jt.getText());
            }
        });
        jm.setEnabled(ClipBoard.isValid);
        popMenu.add(jm);
        jm = new JMenuItem(getMessage("copy_all_close"));
        jm.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ClipBoard.save(jt.getText());
                jd.dispose();
            }
        });
        jm.setEnabled(ClipBoard.isValid);
        popMenu.add(jm);
        popMenu.addSeparator();
        jm = new JMenuItem(getMessage("dismiss"));
        jm.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                popMenu.setVisible(false);
            }
        });
        popMenu.add(jm);
        jd.pack();
        setCentral(jd);
    }

    private DialogHelper(String title, final URL imageURL) {
        jd = new JDialog();
        jd.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        jd.setAlwaysOnTop(true);
        jd.setLayout(new BoxLayout(jd.getContentPane(), BoxLayout.Y_AXIS));
        jd.setTitle(title);
        JLabel jl = new JLabel();
        ImageIcon icon = new ImageIcon(imageURL);
        jl.setIcon(icon);
        jd.add(new JScrollPane(jl));
        final JFileChooser chooser = getSaveImageChooser();
        JPanel jp = new JPanel();
        JButton jb = new JButton(getMessage("btn_save_as"));
        jb.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int returnVal = chooser.showSaveDialog(jd);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = chooser.getSelectedFile();
                    String fileName = file.getPath();
                    String ext = StringUtil.getLowerExtension(fileName);
                    if (!"png".equals(ext)) {
                        fileName += ".png";
                        file = new File(fileName);
                    }
                    boolean doIt = true;
                    if (file.exists()) {
                        int i = JOptionPane.showConfirmDialog(jd, getMessage("warn_file_exist"));
                        if (i != JOptionPane.YES_OPTION) doIt = false;
                    } else if (!file.getParentFile().exists()) {
                        doIt = file.getParentFile().mkdirs();
                    }
                    if (doIt) {
                        FileChannel src = null;
                        FileChannel dest = null;
                        try {
                            src = new FileInputStream(imageURL.getPath()).getChannel();
                            dest = new FileOutputStream(fileName).getChannel();
                            src.transferTo(0, src.size(), dest);
                        } catch (FileNotFoundException e1) {
                            warn(jd, getMessage("err_no_source_file"));
                        } catch (IOException e2) {
                            warn(jd, getMessage("err_output_target"));
                        } finally {
                            try {
                                if (src != null) src.close();
                            } catch (IOException e1) {
                            }
                            try {
                                if (dest != null) dest.close();
                            } catch (IOException e1) {
                            }
                            src = null;
                            dest = null;
                        }
                    }
                }
            }
        });
        jp.add(jb);
        jb = new JButton(getMessage("btn_close"));
        jb.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                jd.dispose();
            }
        });
        jp.add(jb);
        jd.add(jp);
        jd.pack();
        setCentral(jd);
    }

    public static void setCentral(Component jc) {
        jc.setLocation((ScreenCapture.SCREEN_SIZE.width - jc.getWidth()) / 2, (ScreenCapture.SCREEN_SIZE.height - jc.getHeight()) / 2);
    }

    private static final String INFO_TITLE = getMessage("info");

    private static final String WARN_TITLE = getMessage("warn");

    private static final String SYSTEM_MSG = getMessage("msg_system");

    private static final String ERROR_TITLE = getMessage("error");

    /**
     * 以JDialog的方式显示一条警告消息
     */
    public static void info(Component comp, String msg) {
        info(comp, msg, INFO_TITLE);
    }

    public static void info(Component comp, String msg, String title) {
        showDialog(comp, msg, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 以JDialog的方式显示一条警告消息
     */
    public static void warn(Component comp, String msg) {
        warn(comp, msg, WARN_TITLE);
    }

    public static void warn(Component comp, String msg, String title) {
        showDialog(comp, msg, title, JOptionPane.WARNING_MESSAGE);
    }

    /**
     * 以JDialog的方式显示一条错误消息
     */
    public static void error(Component comp, String msg) {
        error(comp, msg, ERROR_TITLE);
    }

    public static void error(Component comp, String msg, String title) {
        showDialog(comp, msg, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * 以JDialog的方式显示一条系统消息
     */
    public static void showSystem(Component comp, String msg) {
        info(comp, msg, SYSTEM_MSG);
    }

    private static void showDialog(Component comp, String msg, String title, int level) {
        if (comp != null) JOptionPane.showMessageDialog(comp, msg, title, level); else {
            JDialog jd = new JOptionPane(msg, level).createDialog(null, title);
            jd.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            jd.setAlwaysOnTop(true);
            jd.pack();
            jd.setLocationRelativeTo(null);
            jd.setVisible(true);
        }
    }

    public static JFileChooser getSaveImageChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(new FileFilter() {

            public boolean accept(File pathname) {
                if (pathname.isDirectory()) {
                    return true;
                }
                String ext = StringUtil.getLowerExtension(pathname.getName());
                if (ext != null) {
                    if (ext.equals("png")) return true; else return false;
                }
                return false;
            }

            @Override
            public String getDescription() {
                return "PNG Image(*.png)";
            }
        });
        return chooser;
    }
}
