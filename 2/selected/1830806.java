package org.virbo.autoplot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UnsupportedLookAndFeelException;
import org.autoplot.csv.CsvDataSourceFactory;
import org.autoplot.csv.CsvDataSourceFormat;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.event.CrossHairRenderer;
import org.das2.event.MouseModule;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasPlot;
import org.das2.util.ExceptionHandler;
import org.das2.util.AboutUtil;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.util.monitor.ProgressMonitor;
import org.virbo.autoplot.dom.Application;
import org.virbo.autoplot.dom.ApplicationController;
import org.virbo.autoplot.dom.Axis;
import org.virbo.autoplot.dom.Canvas;
import org.virbo.autoplot.dom.Diff;
import org.virbo.autoplot.dom.DomUtil;
import org.virbo.autoplot.dom.Plot;
import org.virbo.das2Stream.Das2StreamDataSourceFormat;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.MutablePropertyDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.datasource.DataSetURI;
import org.virbo.datasource.DataSource;
import org.virbo.datasource.DataSourceRegistry;
import org.virbo.datasource.capability.TimeSeriesBrowse;
import org.virbo.dsutil.AsciiParser;
import org.virbo.qstream.SerializeDelegate;
import org.virbo.qstream.SerializeRegistry;

/**
 *
 * @author jbf
 */
public class AutoplotApplet extends JApplet {

    ApplicationModel model;

    Application dom;

    boolean initializing = true;

    private static final Logger logger = Logger.getLogger("autoplot.applet");

    String statusCallback;

    String timeCallback;

    String clickCallback;

    ProgressMonitor loadInitialMonitor;

    long t0 = System.currentTimeMillis();

    public static final String VERSION = "20110317.1";

    private Image splashImage;

    private JCheckBoxMenuItem overviewMenuItem = null;

    private String getStringParameter(String name, String deft) {
        String result = getParameter(name);
        if (result == null) {
            return deft;
        } else {
            return result;
        }
    }

    private int getIntParameter(String name, int deft) {
        String result = getParameter(name);
        if (result == null) {
            return deft;
        } else {
            return Integer.parseInt(result);
        }
    }

    private void setInitializationStatus(String val) {
        if (!statusCallback.equals("")) {
            try {
                getAppletContext().showDocument(new URL("javascript:" + statusCallback + "(\"" + val + "\")"));
            } catch (MalformedURLException ex) {
                Logger.getLogger(AutoplotApplet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void timeCallback(String val) {
        if (!timeCallback.equals("")) {
            try {
                getAppletContext().showDocument(new URL("javascript:" + timeCallback + "(\"" + val + "\")"));
            } catch (MalformedURLException ex) {
                Logger.getLogger(AutoplotApplet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void clickCallback(String plotid, DasPlot plot, MouseEvent e) {
        try {
            Datum xdatum = plot.getXAxis().invTransform(e.getX());
            Datum ydatum = plot.getYAxis().invTransform(e.getY());
            String jscall = String.format("%s('%s','%s','%s',%d,%d,%d )", clickCallback, plotid, xdatum, ydatum, e.getX(), e.getY(), e.getID());
            getAppletContext().showDocument(new URL("javascript:" + jscall));
        } catch (MalformedURLException ex) {
            Logger.getLogger(AutoplotApplet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void drawString(Graphics g, String s, int x, int y) {
        Color c0 = g.getColor();
        g.setColor(Color.WHITE);
        g.drawString(s, x - 1, y - 1);
        g.setColor(c0);
        g.drawString(s, x, y);
    }

    private ProgressMonitor myMon() {
        return new NullProgressMonitor() {

            @Override
            public void setTaskProgress(long position) throws IllegalArgumentException {
                super.setTaskProgress(position);
                repaint();
            }

            @Override
            public void setTaskSize(long taskSize) {
                super.setTaskSize(taskSize);
                repaint();
            }
        };
    }

    private JComponent progressComponent = new JComponent() {

        @Override
        protected void paintComponent(Graphics g1) {
            Graphics2D g = (Graphics2D) g1;
            int leftJust = 70;
            if (initializing) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int em = g.getFontMetrics().getHeight();
                if (splashImage != null) {
                    if (!g.drawImage(splashImage, 0, 0, this)) {
                        drawString(g, "loading splash", leftJust, getHeight() / 2 - em);
                    }
                }
                drawString(g, "initializing...", leftJust, getHeight() / 2);
                if (loadInitialMonitor != null) {
                    Color c0 = g.getColor();
                    g.setColor(new Color(0, 0, 255, 200));
                    long size = loadInitialMonitor.getTaskSize();
                    long pos = loadInitialMonitor.getTaskProgress();
                    int x0 = leftJust;
                    int y0 = getHeight() / 2 + em / 2;
                    int w = 100;
                    int h = 5;
                    if (size == -1) {
                        long t = System.currentTimeMillis() % 2000;
                        int x = (int) (t * w / 2000);
                        int x1 = (int) (t * w / 2000) + h * 2;
                        int ww = x1 - x;
                        g.fillRect(x0 + x, y0, Math.min(w - x, x1 - x), h);
                        Timer timer = new Timer(100, new ActionListener() {

                            public void actionPerformed(ActionEvent e) {
                                repaint();
                            }
                        });
                        timer.setRepeats(false);
                        timer.restart();
                    } else {
                        if (pos > size) {
                            pos = size;
                        }
                        g.fillRect(x0, y0, (int) (pos * w / size), h);
                    }
                    g.setColor(c0);
                    g.drawRect(x0, y0, w, h);
                }
            } else {
                drawString(g, "done initializing", leftJust, getHeight() / 2);
            }
        }
    };

    @Override
    public void init() {
        super.init();
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(AutoplotApplet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(AutoplotApplet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(AutoplotApplet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(AutoplotApplet.class.getName()).log(Level.SEVERE, null, ex);
        }
        String fontParam = getParameter("font");
        if (fontParam != null) {
            Font f = Font.decode(fontParam);
            f = f.deriveFont(f.getSize2D() + 2);
            setFont(f);
        }
        loadInitialMonitor = myMon();
        String si = getStringParameter("splashImage", "");
        if (!si.equals("")) {
            this.splashImage = getImage(getDocumentBase(), si);
            repaint();
        }
        initializing = true;
        getContentPane().add(progressComponent);
        validate();
        System.err.println("init AutoplotApplet " + VERSION + " @ " + (System.currentTimeMillis() - t0) + " msec");
        System.err.println("done init AutoplotApplet " + VERSION + " @ " + (System.currentTimeMillis() - t0) + " msec");
        repaint();
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public void stop() {
        System.err.println("stop AutoplotApplet" + VERSION);
        remove(model.getCanvas());
        this.model = null;
        this.dom = null;
    }

    @Override
    public void start() {
        System.err.println("start AutoplotApplet " + VERSION + " @ " + (System.currentTimeMillis() - t0) + " msec");
        super.start();
        model = new ApplicationModel();
        model.setExceptionHandler(new ExceptionHandler() {

            public void handle(Throwable t) {
                t.printStackTrace();
            }

            public void handleUncaught(Throwable t) {
                t.printStackTrace();
            }
        });
        model.setApplet(true);
        model.dom.getOptions().setAutolayout(false);
        System.err.println("ApplicationModel created @ " + (System.currentTimeMillis() - t0) + " msec");
        model.addDasPeersToApp();
        System.err.println("done addDasPeersToApp @ " + (System.currentTimeMillis() - t0) + " msec");
        try {
            System.err.println("Formatters: " + DataSourceRegistry.getInstance().getFormatterExtensions());
        } catch (Exception ex) {
            Logger.getLogger(AutoplotApplet.class.getName()).log(Level.SEVERE, null, ex);
        }
        ApplicationModel appmodel = model;
        dom = model.getDocumentModel();
        String debug = getParameter("debug");
        if (debug != null && !debug.equals("true")) {
        }
        int width = getIntParameter("width", 700);
        int height = getIntParameter("height", 400);
        String fontParam = getStringParameter("font", "");
        String column = getStringParameter("column", "");
        String row = getStringParameter("row", "");
        String scolor = getStringParameter("color", "");
        String srenderType = getStringParameter("renderType", "");
        String stimeRange = getStringParameter("timeRange", "");
        String sfillColor = getStringParameter("fillColor", "");
        String sforegroundColor = getStringParameter("foregroundColor", "");
        String sbackgroundColor = getStringParameter("backgroundColor", "");
        String title = getStringParameter("plot.title", "");
        String xlabel = getStringParameter("plot.xaxis.label", "");
        String xrange = getStringParameter("plot.xaxis.range", "");
        String xlog = getStringParameter("plot.xaxis.log", "");
        String xdrawTickLabels = getStringParameter("plot.xaxis.drawTickLabels", "");
        String ylabel = getStringParameter("plot.yaxis.label", "");
        String yrange = getStringParameter("plot.yaxis.range", "");
        String ylog = getStringParameter("plot.yaxis.log", "");
        String ydrawTickLabels = getStringParameter("plot.yaxis.drawTickLabels", "");
        String zlabel = getStringParameter("plot.zaxis.label", "");
        String zrange = getStringParameter("plot.zaxis.range", "");
        String zlog = getStringParameter("plot.zaxis.log", "");
        String zdrawTickLabels = getStringParameter("plot.zaxis.drawTickLabels", "");
        statusCallback = getStringParameter("statusCallback", "");
        timeCallback = getStringParameter("timeCallback", "");
        clickCallback = getStringParameter("clickCallback", "");
        if (srenderType.equals("fill_to_zero")) {
            srenderType = "fillToZero";
        }
        setInitializationStatus("readParameters");
        System.err.println("done readParameters @ " + (System.currentTimeMillis() - t0) + " msec");
        String vap = getParameter("vap");
        if (vap != null) {
            InputStream in = null;
            try {
                URL url = new URL(vap);
                System.err.println("load vap " + url + " @ " + (System.currentTimeMillis() - t0) + " msec");
                in = url.openStream();
                System.err.println("open vap stream " + url + " @ " + (System.currentTimeMillis() - t0) + " msec");
                appmodel.doOpen(in, null);
                System.err.println("done open vap @ " + (System.currentTimeMillis() - t0) + " msec");
                appmodel.waitUntilIdle(false);
                System.err.println("done load vap and waitUntilIdle @ " + (System.currentTimeMillis() - t0) + " msec");
                Canvas cc = appmodel.getDocumentModel().getCanvases(0);
                System.err.println("vap height, width= " + cc.getHeight() + "," + cc.getWidth());
                width = getIntParameter("width", cc.getWidth());
                height = getIntParameter("height", cc.getHeight());
                System.err.println("output height, width= " + width + "," + height);
            } catch (InterruptedException ex) {
                Logger.getLogger(AutoplotApplet.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(AutoplotApplet.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    in.close();
                } catch (IOException ex) {
                    Logger.getLogger(AutoplotApplet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        appmodel.getCanvas().setSize(width, height);
        appmodel.getCanvas().revalidate();
        appmodel.getCanvas().setPrintingTag("");
        dom.getOptions().setAutolayout("true".equals(getParameter("autolayout")));
        if (!dom.getOptions().isAutolayout() && vap == null) {
            if (!row.equals("")) {
                dom.getController().getCanvas().getController().setRow(row);
            }
            if (!column.equals("")) {
                dom.getController().getCanvas().getController().setColumn(column);
            }
            dom.getCanvases(0).getRows(0).setTop("0%");
            dom.getCanvases(0).getRows(0).setBottom("100%");
        }
        if (!fontParam.equals("")) {
            appmodel.canvas.setBaseFont(Font.decode(fontParam));
        }
        JMenuItem item;
        item = new JMenuItem(new AbstractAction("Reset Zoom") {

            public void actionPerformed(ActionEvent e) {
                resetZoom();
            }
        });
        dom.getPlots(0).getController().getDasPlot().getDasMouseInputAdapter().addMenuItem(item);
        overviewMenuItem = new JCheckBoxMenuItem(new AbstractAction("Context Overview") {

            public void actionPerformed(ActionEvent e) {
                addOverview();
            }
        });
        dom.getPlots(0).getController().getDasPlot().getDasMouseInputAdapter().addMenuItem(overviewMenuItem);
        if (sforegroundColor != null && !sforegroundColor.equals("")) {
            appmodel.canvas.setForeground(Color.decode(sforegroundColor));
        }
        if (sbackgroundColor != null && !sbackgroundColor.equals("")) {
            appmodel.canvas.setBackground(Color.decode(sbackgroundColor));
        }
        getContentPane().setLayout(new BorderLayout());
        System.err.println("done set parameters @ " + (System.currentTimeMillis() - t0) + " msec");
        String surl = getParameter("url");
        String process = getStringParameter("process", "");
        String script = getStringParameter("script", "");
        if (surl == null) {
            surl = getParameter("dataSetURL");
        }
        if (surl != null && !surl.equals("")) {
            DataSource dsource;
            try {
                dsource = DataSetURI.getDataSource(surl);
                System.err.println("get dsource for " + surl + " @ " + (System.currentTimeMillis() - t0) + " msec");
                System.err.println("  got dsource=" + dsource);
                System.err.println("  dsource.getClass()=" + dsource.getClass());
            } catch (NullPointerException ex) {
                throw new RuntimeException("No such data source: ", ex);
            } catch (Exception ex) {
                ex.printStackTrace();
                dsource = null;
            }
            DatumRange timeRange1 = null;
            if (!stimeRange.equals("")) {
                timeRange1 = DatumRangeUtil.parseTimeRangeValid(stimeRange);
                TimeSeriesBrowse tsb = dsource.getCapability(TimeSeriesBrowse.class);
                if (tsb != null) {
                    System.err.println("do tsb.setTimeRange @ " + (System.currentTimeMillis() - t0) + " msec");
                    tsb.setTimeRange(timeRange1);
                    System.err.println("done tsb.setTimeRange @ " + (System.currentTimeMillis() - t0) + " msec");
                }
            }
            QDataSet ds;
            if (dsource != null) {
                TimeSeriesBrowse tsb = dsource.getCapability(TimeSeriesBrowse.class);
                if (tsb == null) {
                    try {
                        System.err.println("do getDataSet @ " + (System.currentTimeMillis() - t0) + " msec");
                        System.err.println("  dsource=" + dsource);
                        System.err.println("  dsource.getClass()=" + dsource.getClass());
                        if (dsource.getClass().toString().contains("CsvDataSource")) System.err.println(" WHY IS THIS CsvDataSource!?!?");
                        ds = dsource == null ? null : dsource.getDataSet(loadInitialMonitor);
                        for (int i = 0; i < Math.min(12, ds.length()); i++) {
                            System.err.printf("ds[%d]=%s\n", i, ds.slice(i));
                        }
                        System.err.println("loaded ds: " + ds);
                        System.err.println("done getDataSet @ " + (System.currentTimeMillis() - t0) + " msec");
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
            System.err.println("do setDataSource @ " + (System.currentTimeMillis() - t0) + " msec");
            appmodel.setDataSource(dsource);
            System.err.println("done setDataSource @ " + (System.currentTimeMillis() - t0) + " msec");
            setInitializationStatus("dataSourceSet");
            if (stimeRange != null && !stimeRange.equals("")) {
                try {
                    System.err.println("wait for idle @ " + (System.currentTimeMillis() - t0) + " msec (due to stimeRange)");
                    appmodel.waitUntilIdle(true);
                    System.err.println("done wait for idle @ " + (System.currentTimeMillis() - t0) + " msec");
                } catch (InterruptedException ex) {
                    Logger.getLogger(AutoplotApplet.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (UnitsUtil.isTimeLocation(dom.getTimeRange().getUnits())) {
                    dom.setTimeRange(timeRange1);
                }
            }
            setInitializationStatus("dataSetLoaded");
        }
        System.err.println("done dataSetLoaded @ " + (System.currentTimeMillis() - t0) + " msec");
        Plot p = dom.getController().getPlot();
        if (!title.equals("")) {
            p.setTitle(title);
        }
        Axis axis = p.getXaxis();
        if (!xlabel.equals("")) {
            axis.setLabel(xlabel);
        }
        if (!xrange.equals("")) {
            try {
                Units u = axis.getController().getDasAxis().getUnits();
                DatumRange newRange = DatumRangeUtil.parseDatumRange(xrange, u);
                axis.setRange(newRange);
            } catch (ParseException ex) {
                Logger.getLogger(AutoplotApplet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (!xlog.equals("")) {
            axis.setLog("true".equals(xlog));
        }
        if (!xdrawTickLabels.equals("")) {
            axis.setDrawTickLabels("true".equals(xdrawTickLabels));
        }
        axis = p.getYaxis();
        if (!ylabel.equals("")) {
            axis.setLabel(ylabel);
        }
        if (!yrange.equals("")) {
            try {
                Units u = axis.getController().getDasAxis().getUnits();
                DatumRange newRange = DatumRangeUtil.parseDatumRange(yrange, u);
                axis.setRange(newRange);
            } catch (ParseException ex) {
                Logger.getLogger(AutoplotApplet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (!ylog.equals("")) {
            axis.setLog("true".equals(ylog));
        }
        if (!ydrawTickLabels.equals("")) {
            axis.setDrawTickLabels("true".equals(ydrawTickLabels));
        }
        axis = p.getZaxis();
        if (!zlabel.equals("")) {
            axis.setLabel(zlabel);
        }
        if (!zrange.equals("")) {
            try {
                Units u = axis.getController().getDasAxis().getUnits();
                DatumRange newRange = DatumRangeUtil.parseDatumRange(zrange, u);
                axis.setRange(newRange);
            } catch (ParseException ex) {
                Logger.getLogger(AutoplotApplet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (!zlog.equals("")) {
            axis.setLog("true".equals(zlog));
        }
        if (!zdrawTickLabels.equals("")) {
            axis.setDrawTickLabels("true".equals(zdrawTickLabels));
        }
        if (srenderType != null && !srenderType.equals("")) {
            try {
                RenderType renderType = RenderType.valueOf(srenderType);
                dom.getController().getPlotElement().setRenderType(renderType);
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            }
        }
        System.err.println("done setRenderType @ " + (System.currentTimeMillis() - t0) + " msec");
        if (!scolor.equals("")) {
            try {
                dom.getController().getPlotElement().getStyle().setColor(Color.decode(scolor));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (!sfillColor.equals("")) {
            try {
                dom.getController().getPlotElement().getStyle().setFillColor(Color.decode(sfillColor));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (!sforegroundColor.equals("")) {
            try {
                dom.getOptions().setForeground(Color.decode(sforegroundColor));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (!sbackgroundColor.equals("")) {
            try {
                dom.getOptions().setBackground(Color.decode(sbackgroundColor));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        surl = getParameter("dataSetURL");
        if (surl != null) {
            if (surl.startsWith("about:")) {
                setDataSetURL(surl);
            } else {
            }
        }
        getContentPane().remove(progressComponent);
        getContentPane().add(model.getCanvas());
        System.err.println("done add to applet @ " + (System.currentTimeMillis() - t0) + " msec");
        validate();
        System.err.println("done applet.validate @ " + (System.currentTimeMillis() - t0) + " msec");
        repaint();
        appmodel.getCanvas().setVisible(true);
        initializing = false;
        repaint();
        System.err.println("ready @ " + (System.currentTimeMillis() - t0) + " msec");
        setInitializationStatus("ready");
        dom.getController().getPlot().getXaxis().addPropertyChangeListener(Axis.PROP_RANGE, new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                timeCallback(String.valueOf(evt.getNewValue()));
            }
        });
        if (!clickCallback.equals("")) {
            String clickCallbackLabel = "Applet Click";
            int i = clickCallback.indexOf(",");
            if (i != -1) {
                int i2 = clickCallback.indexOf("label=");
                if (i2 != -1) clickCallbackLabel = clickCallback.substring(i2 + 6).trim();
                clickCallback = clickCallback.substring(0, i).trim();
            }
            final DasPlot plot = dom.getPlots(0).getController().getDasPlot();
            MouseModule mm = new MouseModule(plot, new CrossHairRenderer(plot, null, plot.getXAxis(), plot.getYAxis()), clickCallbackLabel) {

                @Override
                public void mousePressed(MouseEvent e) {
                    e = SwingUtilities.convertMouseEvent(plot, e, plot.getCanvas());
                    clickCallback(dom.getPlots(0).getId(), plot, e);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    e = SwingUtilities.convertMouseEvent(plot, e, plot.getCanvas());
                    clickCallback(dom.getPlots(0).getId(), plot, e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    e = SwingUtilities.convertMouseEvent(plot, e, plot.getCanvas());
                    clickCallback(dom.getPlots(0).getId(), plot, e);
                }
            };
            plot.getDasMouseInputAdapter().setPrimaryModule(mm);
        }
        p.getController().getDasPlot().getDasMouseInputAdapter().removeMenuItem("Properties");
        dom.getPlots(0).getXaxis().getController().getDasAxis().getDasMouseInputAdapter().removeMenuItem("Properties");
        dom.getPlots(0).getYaxis().getController().getDasAxis().getDasMouseInputAdapter().removeMenuItem("Properties");
        dom.getPlots(0).getZaxis().getController().getDasAxis().getDasMouseInputAdapter().removeMenuItem("Properties");
        if (getStringParameter("contextOverview", "off").equals("on")) {
            Runnable run = new Runnable() {

                public void run() {
                    dom.getController().waitUntilIdle();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(AutoplotApplet.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    dom.getController().waitUntilIdle();
                    doSetOverview(true);
                }
            };
            new Thread(run).start();
        }
        System.err.println("done start AutoplotApplet " + VERSION + " @ " + (System.currentTimeMillis() - t0) + " msec");
    }

    private void createAppletTester() {
        JFrame frame = new JFrame();
        JButton button = new JButton(new AbstractAction("pushme") {

            public void actionPerformed(ActionEvent e) {
                URL url = getCodeBase();
                String surl = "" + url.toString() + "Capture_00158.jpg?channel=red";
                System.err.println("************************************************");
                System.err.println("************************************************");
                System.err.println(surl);
                System.err.println("************************************************");
                System.err.println("************************************************");
                setDataSetURL(surl);
            }
        });
        frame.getContentPane().add(button);
        frame.pack();
        frame.setVisible(true);
    }

    public void setDataSetURL(final String surl) {
        try {
            logger.info(surl);
            System.err.println("***************");
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    if (surl.equals("about:plugins")) {
                        String text = DataSourceRegistry.getPluginsText();
                        JOptionPane.showMessageDialog(AutoplotApplet.this, text);
                        return;
                    } else if (surl.equals("about:autoplot")) {
                        try {
                            StringBuilder buffy = new StringBuilder();
                            URL aboutHtml = ApplicationModel.class.getResource("aboutAutoplot.html");
                            BufferedReader reader = new BufferedReader(new InputStreamReader(aboutHtml.openStream()));
                            String s = reader.readLine();
                            while (s != null) {
                                buffy.append(s).append("");
                                s = reader.readLine();
                            }
                            reader.close();
                            buffy.append("    <h2>Build Information:</h2>");
                            buffy.append("<ul>");
                            buffy.append("<li>release tag: ").append(AboutUtil.getReleaseTag()).append("</li>");
                            List<String> bi = Util.getBuildInfos();
                            for (String ss : bi) {
                                buffy.append("    <li>").append(ss).append("");
                            }
                            buffy.append("<ul>    </p></html>");
                            JOptionPane.showMessageDialog(AutoplotApplet.this, buffy.toString());
                            return;
                        } catch (IOException iOException) {
                            iOException.printStackTrace();
                        }
                    }
                    model.setDataSourceURL(surl);
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected String timeRange;

    public static final String PROP_TIMERANGE = "timeRange";

    public String getTimeRange() {
        return dom.getTimeRange().toString();
    }

    public void setTimeRange(final String timeRange) {
        Runnable run = new Runnable() {

            public void run() {
                try {
                    String oldv = getTimeRange();
                    dom.getController().getPlot().getController().getDasPlot().getXAxis().setDatumRange(DatumRangeUtil.parseTimeRangeValid(timeRange));
                    firePropertyChange(PROP_TIMERANGE, oldv, timeRange);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        SwingUtilities.invokeLater(run);
    }

    protected String font;

    public static final String PROP_FONT = "canvasFont";

    public String getCanvasFont() {
        return model.getCanvas().getBaseFont().toString();
    }

    public void setCanvasFont(final String font) {
        Runnable run = new Runnable() {

            public void run() {
                try {
                    String oldFont = getCanvasFont();
                    model.getCanvas().setBaseFont(Font.decode(font));
                    model.getCanvas().repaint();
                    firePropertyChange(PROP_FONT, oldFont, font);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        SwingUtilities.invokeLater(run);
    }

    /**
     * return the application dom.
     * @return
     */
    public Application getDom() {
        return this.dom;
    }

    public void dumpDom() {
        List<Diff> diffs = new Application().diffs(dom);
        for (Diff d : diffs) {
            System.err.println(d);
        }
    }

    public void printDomNode(final String node) {
        Runnable run = new Runnable() {

            public void run() {
                try {
                    Object o = DomUtil.getPropertyValue(dom, node);
                    System.err.println("dom." + node + "=" + o);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        SwingUtilities.invokeLater(run);
    }

    public void setDomNode(final String node, final String sval) {
        getAppletContext().showStatus("setDomNode( " + node + "," + sval + ")");
        Runnable run = new Runnable() {

            public void run() {
                try {
                    Class c = DomUtil.getPropertyType(dom, node);
                    SerializeDelegate sd = SerializeRegistry.getDelegate(c);
                    if (sd == null) {
                        System.err.println("unable to find serialize delegate for " + c.getCanonicalName());
                        return;
                    }
                    Object val = sd.parse(sd.typeId(c), sval);
                    DomUtil.setPropertyValue(dom, node, val);
                    getAppletContext().showStatus("dom." + node + "=" + DomUtil.getPropertyValue(dom, node));
                    System.err.println("dom." + node + "=" + DomUtil.getPropertyValue(dom, node));
                } catch (ParseException ex) {
                    ex.printStackTrace();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        SwingUtilities.invokeLater(run);
    }

    /**
     * reset the plot zoom to the initial settings.
     */
    public void resetZoom() {
        Runnable run = new Runnable() {

            public void run() {
                dom.getController().getPlot().getController().resetZoom(true, true, true);
            }
        };
        SwingUtilities.invokeLater(run);
    }

    private void doSetOverview(boolean t) {
        Plot domPlot = dom.getPlots(0);
        if (t) {
            ApplicationController controller = dom.getController();
            Plot that = controller.copyPlotAndPlotElements(domPlot, null, false, false);
            that.setTitle("");
            controller.bind(domPlot.getZaxis(), Axis.PROP_RANGE, that.getZaxis(), Axis.PROP_RANGE);
            controller.bind(domPlot.getZaxis(), Axis.PROP_LOG, that.getZaxis(), Axis.PROP_LOG);
            controller.bind(domPlot.getZaxis(), Axis.PROP_LABEL, that.getZaxis(), Axis.PROP_LABEL);
            controller.addConnector(domPlot, that);
            dom.getCanvases(0).getRows(0).setBottom("60%-2em");
            dom.getCanvases(0).getRows(1).setTop("60%+2em");
            that.getXaxis().getController().getDasAxis().getDasMouseInputAdapter().removeMenuItem("Properties");
            that.getYaxis().getController().getDasAxis().getDasMouseInputAdapter().removeMenuItem("Properties");
            that.getZaxis().getController().getDasAxis().getDasMouseInputAdapter().removeMenuItem("Properties");
            that.getController().getDasPlot().getDasMouseInputAdapter().removeMenuItem("Properties");
            that.getController().getDasPlot().getDasMouseInputAdapter().removeMenuItem("Connector Properties");
        } else {
            ApplicationController controller = dom.getController();
            controller.deletePlotElement(dom.getPlotElements(1));
            controller.deletePlot(dom.getPlots(1));
        }
        overviewMenuItem.setSelected(t);
    }

    public void addOverview() {
        Runnable run = new Runnable() {

            public void run() {
                doSetOverview(overviewMenuItem.isSelected());
            }
        };
        SwingUtilities.invokeLater(run);
    }

    /**
     * plot the data in the string.
     * @param sdata
     */
    public void plotData(final String fsdata) {
        Runnable run = new Runnable() {

            public void run() {
                try {
                    String sdata = fsdata;
                    AsciiParser p = new AsciiParser();
                    int i = sdata.indexOf(";");
                    if (i != -1) {
                        sdata = sdata.replaceAll(";", "\n");
                        i = sdata.indexOf("\n");
                    }
                    p.guessDelimParser(sdata.substring(0, i));
                    DasCanvas c = dom.getController().getCanvas().getController().getDasCanvas();
                    QDataSet data = p.readStream(new StringReader(sdata), dom.getController().getMonitorFactory().getMonitor(c, "reading data", "reading data"));
                    MutablePropertyDataSet y = DataSetOps.slice1(data, 1);
                    y.putProperty(QDataSet.DEPEND_0, DataSetOps.slice1(data, 0));
                    model.setDataSet(y);
                } catch (IOException ex) {
                    Logger.getLogger(AutoplotApplet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        SwingUtilities.invokeLater(run);
    }
}
