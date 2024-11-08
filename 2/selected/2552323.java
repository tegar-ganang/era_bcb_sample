package jdos.gui;

import jdos.cpu.core_dynamic.DecodeBlock;
import jdos.sdl.GUI;
import jdos.util.FileHelper;
import jdos.util.Progress;
import jdos.util.StringHelper;
import jdos.util.UnZip;
import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;

public class MainApplet extends Applet implements GUI, KeyListener, Runnable, MouseListener, MouseMotionListener {

    private static final String base_dir = ".jdosbox";

    int[] pixels = new int[16 * 16];

    Image image = Toolkit.getDefaultToolkit().createImage(new MemoryImageSource(16, 16, pixels, 0, 16));

    Cursor transparentCursor = Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(0, 0), "invisibleCursor");

    private String progressMsg = null;

    private int progressPercent = 0;

    private long progressTotal = 0;

    private long progressCompleted = 0;

    private Color backgroundColor = Color.darkGray;

    private static int current_id = 0;

    private int id;

    public void showProgress(String msg, int percent) {
        progressMsg = msg;
        progressPercent = percent;
        repaint();
    }

    public void captureMouse(boolean on) {
        if (MainFrame.robot != null) {
            MainFrame.robotCenter(getLocationOnScreen());
        }
    }

    public void showCursor(boolean on) {
        if (on) setCursor(Cursor.getDefaultCursor()); else setCursor(transparentCursor);
    }

    public void fullScreenToggle() {
    }

    public void setSize(int width, int height) {
    }

    public void dopaint() {
        repaint();
    }

    public void setTitle(String title) {
    }

    static Thread thread;

    Progress progressBar = new Progress() {

        public void set(int value) {
        }

        public void status(String value) {
            progressMsg = value;
            repaint();
        }

        public void done() {
        }

        public boolean hasCancelled() {
            return false;
        }

        public void speed(String value) {
        }

        public void initializeSpeedValue(long totalExpected) {
            progressTotal = totalExpected;
            progressCompleted = 0;
        }

        public void incrementSpeedValue(long value) {
            progressCompleted += value;
            progressPercent = (int) (progressCompleted * 100 / progressTotal);
            repaint();
        }
    };

    private boolean download(String urlLocation, File location) {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            progressMsg = "Downloading " + location.getName();
            progressPercent = 0;
            repaint();
            if (location.exists()) {
                location.delete();
            }
            if (!location.getParentFile().exists()) {
                location.getParentFile().mkdirs();
            }
            URL url = new URL(urlLocation);
            URLConnection urlc = url.openConnection();
            long size = -1;
            String s = urlc.getHeaderField("content-length");
            if (s != null) {
                size = Long.parseLong(s);
            }
            bis = new BufferedInputStream(urlc.getInputStream());
            bos = new BufferedOutputStream(new FileOutputStream(location));
            byte[] buffer = new byte[4096];
            int read = 0;
            if (size > 0) progressBar.initializeSpeedValue(size);
            do {
                read = bis.read(buffer);
                if (read > 0) {
                    progressBar.incrementSpeedValue(read);
                    bos.write(buffer, 0, read);
                }
            } while (read > 0);
            bis.close();
            bis = null;
            bos.close();
            bos = null;
            if (size > 0 && location.length() != size) {
                System.out.println("FAILED to download file: " + location.getAbsolutePath());
                System.out.println("   expected " + size + " bytes and got " + location.length() + " bytes");
                progressMsg = "FAILED to download file: " + urlLocation;
                progressPercent = 0;
                repaint();
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bis != null) try {
                bis.close();
            } catch (Exception e) {
            }
            if (bos != null) try {
                bos.close();
            } catch (Exception e) {
            }
        }
        progressMsg = "FAILED to download file: " + urlLocation;
        progressPercent = 0;
        repaint();
        return false;
    }

    private void unzip(File file, File directory) {
        UnZip.unzip(file.getAbsolutePath(), directory.getAbsolutePath(), progressBar);
    }

    private Color parseColorStr(String s) {
        int r, g, b;
        if (s.length() == 7 && s.charAt(0) == '#') {
            try {
                r = Integer.parseInt(s.substring(1, 3), 16);
                g = Integer.parseInt(s.substring(3, 5), 16);
                b = Integer.parseInt(s.substring(5, 7), 16);
                return (new Color(r, g, b));
            } catch (Exception e) {
            }
        }
        return (Color.black);
    }

    public void run() {
        System.out.println("About to start DosBox");
        for (int i = 1; i < 10; i++) {
            try {
                String url = getParameter("download" + i);
                if (url != null) {
                    String fullName = url.substring(url.lastIndexOf('/') + 1);
                    String name = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.'));
                    File dir = new File(FileHelper.getHomeDirectory() + File.separator + base_dir + File.separator + name);
                    if (!dir.exists()) {
                        File downloadFile = new File(FileHelper.getHomeDirectory() + File.separator + base_dir + File.separator + "temp" + File.separator + fullName);
                        if (download(url, downloadFile)) {
                            unzip(downloadFile, dir);
                            downloadFile.delete();
                        } else {
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("download parameter should be the url of a .zip or .7z file");
                e.printStackTrace();
            }
        }
        String bg = getParameter("background-color");
        if (bg != null) {
            backgroundColor = parseColorStr(bg);
        }
        progressMsg = null;
        progressPercent = 0;
        repaint();
        Vector params = new Vector();
        params.add("-applet");
        for (int i = 1; i < 10; i++) {
            String param = getParameter("param" + i);
            if (param == null) {
                break;
            }
            if (param.startsWith("-")) {
                String[] p = StringHelper.split(param, " ");
                for (int j = 0; j < p.length; j++) {
                    params.addElement(p[j]);
                }
            } else {
                params.add("-c");
                params.add(param);
            }
        }
        String[] cmds = new String[params.size()];
        params.copyInto(cmds);
        setFocusTraversalKeysEnabled(false);
        Main.main(this, cmds);
    }

    public void init() {
        System.out.println("Applet.init()");
        try {
            if (MainFrame.robot == null) {
                MainFrame.robot = new Robot();
            }
        } catch (Throwable e) {
            System.out.println("Applet is not signed");
            System.out.println("    mouse capture will not work");
            System.out.println("    disabling compiler");
            DecodeBlock.compilerEnabled = false;
        }
        if (thread != null) {
            System.out.println("Applet.init force stop");
            i_stop();
        }
        setBackground(Color.black);
        addKeyListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);
        addFocusListener(new FocusListener() {

            private final KeyEventDispatcher altDisabler = new KeyEventDispatcher() {

                public boolean dispatchKeyEvent(KeyEvent e) {
                    if (e.getKeyCode() == 18) {
                        Main.addEvent(e);
                        return true;
                    }
                    return false;
                }
            };

            public void focusGained(FocusEvent e) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(altDisabler);
            }

            public void focusLost(FocusEvent e) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(altDisabler);
            }
        });
    }

    public void destroy() {
        removeKeyListener(this);
        removeMouseMotionListener(this);
        removeMouseListener(this);
    }

    public void start() {
        current_id++;
        id = current_id;
        System.out.println("Applet.start");
        if (thread != null) {
            System.out.println("Applet.start force stop");
            i_stop();
        }
        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        if (id == current_id) i_stop();
    }

    public void i_stop() {
        System.out.println("Applet.stop");
        synchronized (Main.pauseMutex) {
            Main.pauseMutex.notify();
        }
        Main.addEvent(null);
        try {
            thread.join(5000);
        } catch (Exception e) {
        }
        thread = null;
        try {
            Thread.sleep(2000);
        } catch (Exception e) {
        }
    }

    Graphics bufferGraphics;

    Image offscreen;

    private void drawProgress(Graphics g, int width, int height) {
        int barHeight = 0;
        int yOffset = 5;
        FontMetrics fm = g.getFontMetrics(g.getFont());
        java.awt.geom.Rectangle2D rect = fm.getStringBounds(progressMsg, g);
        barHeight = (int) (rect.getHeight() * 1.7);
        int textHeight = (int) (rect.getHeight());
        int textWidth = (int) (rect.getWidth());
        int x = (width - textWidth) / 2;
        int y = height - barHeight + (barHeight - textHeight) / 2 + fm.getAscent();
        int right = width * progressPercent / 100;
        g.setColor(Color.white);
        g.fillRect(0, height - barHeight - yOffset, right, barHeight);
        g.setClip(right, 0, width, height);
        g.setColor(Color.yellow);
        g.drawString(progressMsg, x, y - yOffset);
        g.setClip(0, 0, right, height);
        g.setColor(Color.black);
        g.drawString(progressMsg, x, y - yOffset);
        g.setClip(0, 0, width, height);
    }

    private int getScreenX() {
        if (Main.screen_width < this.getWidth()) {
            return (getWidth() - Main.screen_width) / 2;
        }
        return 0;
    }

    private int getScreenY() {
        if (Main.screen_height < this.getHeight()) {
            return (getHeight() - Main.screen_height) / 2;
        }
        return 0;
    }

    private void draw(Graphics g) {
        int x = 0;
        int y = 0;
        if (Main.screen_width < this.getWidth()) {
            x = getScreenX();
            g.setColor(backgroundColor);
            g.fillRect(0, 0, x, getHeight());
            g.fillRect(x + Main.screen_width, 0, getWidth() - (x + Main.screen_width), getHeight());
        }
        if (Main.screen_height < this.getHeight()) {
            y = getScreenY();
            g.setColor(backgroundColor);
            g.fillRect(0, 0, getWidth(), y);
            g.fillRect(0, y + Main.screen_height, getWidth(), getHeight() - (y + Main.screen_height));
        }
        if (Render.render != null && Render.render.aspect && (Main.screen_height % Main.buffer_height) != 0) {
            BufferedImage resized = MainFrame.resizeImage(Main.buffer, Main.screen_width, Main.screen_height, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(resized, x, y, Main.screen_width + x, Main.screen_height + y, 0, 0, Main.screen_width, Main.screen_height, null);
        } else {
            g.drawImage(Main.buffer, x, y, Main.screen_width + x, Main.screen_height + y, 0, 0, Main.buffer_width, Main.buffer_height, null);
        }
    }

    public void update(Graphics g) {
        if (Main.buffer != null) {
            synchronized (Main.paintMutex) {
                if (progressMsg != null) {
                    drawProgress(Main.buffer.getGraphics(), Main.screen_width, Main.screen_height);
                }
                draw(g);
            }
        } else if (progressMsg != null) {
            if (bufferGraphics == null) {
                Rectangle r = g.getClipBounds();
                offscreen = createImage(r.width, r.height);
                bufferGraphics = offscreen.getGraphics();
            }
            bufferGraphics.clearRect(0, 0, offscreen.getWidth(null), offscreen.getHeight(null));
            drawProgress(bufferGraphics, offscreen.getWidth(null), offscreen.getHeight(null));
            g.drawImage(offscreen, 0, 0, null);
        }
    }

    public void paint(Graphics g) {
        update(g);
    }

    public void keyTyped(KeyEvent e) {
    }

    /** Handle the key pressed event from the text field. */
    public void keyPressed(KeyEvent e) {
        Main.addKeyEvent(e);
    }

    /** Handle the key released event from the text field. */
    public void keyReleased(KeyEvent e) {
        Main.addKeyEvent(e);
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON3) {
            Main.GFX_CaptureMouse();
        }
    }

    public void mousePressed(MouseEvent e) {
        Main.addMouseEvent(e);
    }

    public void mouseReleased(MouseEvent e) {
        Main.addMouseEvent(e);
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
        MainFrame.robotMouse(e, getLocationOnScreen(), getScreenX(), getScreenY());
    }

    public void mouseMoved(MouseEvent e) {
        MainFrame.robotMouse(e, getLocationOnScreen(), getScreenX(), getScreenY());
    }
}
