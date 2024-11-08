package org.fit.cssbox.swingbox.demo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.html.HTMLEditorKit;
import org.fit.cssbox.css.CSSNorm;
import org.fit.cssbox.css.DOMAnalyzer;
import org.fit.cssbox.demo.DOMSource;
import org.fit.cssbox.layout.BrowserCanvas;
import org.fit.cssbox.swingbox.BrowserPane;
import org.fit.cssbox.swingbox.SwingBoxViewFactory;
import org.fit.cssbox.swingbox.util.DefaultHyperlinkHandler;
import org.fit.cssbox.swingbox.util.GeneralEvent;
import org.fit.cssbox.swingbox.util.GeneralEvent.EventType;
import org.fit.cssbox.swingbox.util.GeneralEventListener;
import org.w3c.dom.Document;

/**
 * The Class DemoBrowser.
 * This demo provides 3 result of same location by 3 renderrers.
 * You will see CSSBox, SwingBox and JEditorPane + HTMLEditorKit.
 * Use the "GO!" button to start action.
 *
 * @author Peter Bielik
 * @version 1.0
 * @since 1.0 - 22.4.2011
 */
public class DemoBrowser extends JFrame {

    private static final long serialVersionUID = 3078719188136612454L;

    BrowserPane swingbox = new BrowserPane();

    JPanel cssbox = new JPanel();

    JEditorPane editorkit = new JEditorPane();

    JTextField txt = new JTextField("http://www.phoronix.com", 60);

    JScrollPane contentScroll = new JScrollPane();

    /**
     * Creates new instance of this demo application.
     */
    public DemoBrowser() {
        init();
    }

    private void init() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel tmp = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btn = new JButton("  GO!  ");
        tmp.add(txt);
        tmp.add(btn);
        btn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        loadPage(txt.getText());
                    }
                });
                t.start();
            }
        });
        JTabbedPane tab = new JTabbedPane(JTabbedPane.TOP);
        tab.addTab("JEditorPane + HTMLEditorKit", new JScrollPane(editorkit));
        tab.addTab("CSSBox", contentScroll);
        tab.addTab("SwingBox", new JScrollPane(swingbox));
        panel.add(tmp, BorderLayout.NORTH);
        panel.add(tab, BorderLayout.CENTER);
        setContentPane(panel);
        swingbox.addHyperlinkListener(new DefaultHyperlinkHandler());
        swingbox.addGeneralEventListener(new GeneralEventListener() {

            private long time;

            @Override
            public void generalEventUpdate(GeneralEvent e) {
                if (e.event_type == EventType.page_loading_begin) {
                    time = System.currentTimeMillis();
                } else if (e.event_type == EventType.page_loading_end) {
                    System.out.println("SwingBox: page loaded in: " + (System.currentTimeMillis() - time) + " ms");
                }
            }
        });
        editorkit.setEditorKit(new HTMLEditorKit());
        editorkit.setEditable(false);
        editorkit.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) try {
                    editorkit.setPage(e.getURL());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        contentScroll.setViewportView(cssbox);
        contentScroll.addComponentListener(new java.awt.event.ComponentAdapter() {

            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (cssbox != null && cssbox instanceof BrowserCanvas) {
                    ((BrowserCanvas) cssbox).createLayout(contentScroll.getSize());
                    contentScroll.repaint();
                }
            }
        });
        setResizable(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(800, 600));
        setTitle("Demo");
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void loadPage(String page) {
        if (!page.startsWith("http:") && !page.startsWith("ftp:") && !page.startsWith("file:")) {
            page = "http://" + page;
        }
        loadPage_editorkit(page);
        loadPage_cssbox(page);
        loadPage_swingbox(page);
    }

    private void loadPage_swingbox(String url) {
        try {
            swingbox.setPage(new URL(url));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPage_cssbox(String adr) {
        InputStream is = null;
        try {
            URL url = new URL(adr);
            URLConnection con = url.openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; BoxBrowserTest/2.x; Linux) CSSBox/2.x (like Gecko)");
            is = con.getInputStream();
            DOMSource parser = new DOMSource(is);
            parser.setContentType(con.getHeaderField("Content-Type"));
            Document doc = parser.parse();
            DOMAnalyzer da = new DOMAnalyzer(doc, url);
            da.attributesToStyles();
            da.addStyleSheet(null, CSSNorm.stdStyleSheet());
            da.addStyleSheet(null, CSSNorm.userStyleSheet());
            da.getStyleSheets();
            cssbox = new BrowserCanvas(da.getRoot(), da, contentScroll.getSize(), url);
            contentScroll.setViewportView(cssbox);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void loadPage_editorkit(String url) {
        try {
            editorkit.setPage(new URL(url));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The main method.
     *
     * @param args
     *            no arguments needed.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                new DemoBrowser();
            }
        });
    }

    void renderImage() {
        View view = null;
        ViewFactory factory = swingbox.getEditorKit().getViewFactory();
        if (factory instanceof SwingBoxViewFactory) {
            view = ((SwingBoxViewFactory) factory).getViewport();
        }
        if (view != null) {
            int w = (int) view.getPreferredSpan(View.X_AXIS);
            int h = (int) view.getPreferredSpan(View.Y_AXIS);
            Rectangle rec = new Rectangle(w, h);
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setClip(rec);
            view.paint(g, rec);
            FileOutputStream fos;
            try {
                fos = new FileOutputStream("image.png");
                ImageIO.write(img, "png", fos);
                fos.close();
            } catch (Exception ignored) {
            }
        }
    }
}
