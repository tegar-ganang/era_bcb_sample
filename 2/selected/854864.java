package org.jhotdraw.samples.uml;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.geom.Point2D;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.Collections;
import javax.swing.BoxLayout;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.jhotdraw.draw.Drawing;
import org.jhotdraw.draw.ImageFigure;
import org.jhotdraw.draw.TextFigure;
import org.jhotdraw.draw.io.DOMStorableInputOutputFormat;
import org.jhotdraw.draw.io.ImageInputFormat;
import org.jhotdraw.draw.io.ImageOutputFormat;
import org.jhotdraw.draw.io.InputFormat;
import org.jhotdraw.draw.io.OutputFormat;
import org.jhotdraw.draw.io.TextInputFormat;
import org.jhotdraw.gui.Worker;
import org.jhotdraw.xml.NanoXMLDOMInput;
import org.jhotdraw.xml.NanoXMLDOMOutput;

/**
 * UMLApplet.
 *
 * @author  wrandels
 * @version $Id: UMLApplet.java,v 1.1 2009/10/18 20:41:48 cfm1 Exp $
 */
public class UMLApplet extends JApplet {

    private static final String NAME = "JHotDraw Draw";

    private UMLPanel uMLPanel;

    /**
     * We override getParameter() to make it work even if we have no Applet
     * context.
     */
    @Override
    public String getParameter(String name) {
        try {
            return super.getParameter(name);
        } catch (NullPointerException e) {
            return null;
        }
    }

    protected String getVersion() {
        return UMLApplet.class.getPackage().getImplementationVersion();
    }

    /** Initializes the applet UMLApplet */
    @Override
    public void init() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Throwable e) {
        }
        Container c = getContentPane();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        String[] labels = getAppletInfo().split("\n");
        for (int i = 0; i < labels.length; i++) {
            c.add(new JLabel((labels[i].length() == 0) ? " " : labels[i]));
        }
        new Worker() {

            public Object construct() {
                Object result;
                try {
                    if (getParameter("data") != null) {
                        NanoXMLDOMInput domi = new NanoXMLDOMInput(new UMLFigureFactory(), new StringReader(getParameter("data")));
                        result = domi.readObject(0);
                    } else if (getParameter("datafile") != null) {
                        InputStream in = null;
                        try {
                            URL url = new URL(getDocumentBase(), getParameter("datafile"));
                            in = url.openConnection().getInputStream();
                            NanoXMLDOMInput domi = new NanoXMLDOMInput(new UMLFigureFactory(), in);
                            result = domi.readObject(0);
                        } finally {
                            if (in != null) in.close();
                        }
                    } else {
                        result = null;
                    }
                } catch (Throwable t) {
                    result = t;
                }
                return result;
            }

            public void finished(Object value) {
                if (value instanceof Throwable) {
                    ((Throwable) value).printStackTrace();
                }
                Container c = getContentPane();
                c.setLayout(new BorderLayout());
                c.removeAll();
                c.add(uMLPanel = new UMLPanel());
                Object result = getValue();
                initComponents();
                if (result != null) {
                    if (result instanceof Drawing) {
                        Drawing drawing = (Drawing) result;
                        setDrawing(drawing);
                    } else if (result instanceof Throwable) {
                        getDrawing().add(new TextFigure(result.toString()));
                        ((Throwable) result).printStackTrace();
                    }
                }
                initDrawing(getDrawing());
                c.validate();
            }
        }.start();
    }

    private void setDrawing(Drawing d) {
        uMLPanel.setDrawing(d);
    }

    private Drawing getDrawing() {
        return uMLPanel.getDrawing();
    }

    /**
     * Configure Drawing object to support copy and paste.
     */
    @SuppressWarnings("unchecked")
    private void initDrawing(Drawing d) {
        d.setInputFormats((java.util.List<InputFormat>) Collections.EMPTY_LIST);
        d.setOutputFormats((java.util.List<OutputFormat>) Collections.EMPTY_LIST);
        DOMStorableInputOutputFormat ioFormat = new DOMStorableInputOutputFormat(new UMLFigureFactory());
        d.addInputFormat(ioFormat);
        d.addInputFormat(new ImageInputFormat(new ImageFigure()));
        d.addInputFormat(new TextInputFormat(new TextFigure()));
        d.addOutputFormat(ioFormat);
        d.addOutputFormat(new ImageOutputFormat());
    }

    public void setData(String text) {
        if (text != null && text.length() > 0) {
            StringReader in = new StringReader(text);
            try {
                NanoXMLDOMInput domi = new NanoXMLDOMInput(new UMLFigureFactory(), in);
                setDrawing((Drawing) domi.readObject(0));
            } catch (Throwable e) {
                getDrawing().removeAllChildren();
                TextFigure tf = new TextFigure();
                tf.setText(e.getMessage());
                tf.setBounds(new Point2D.Double(10, 10), new Point2D.Double(100, 100));
                getDrawing().add(tf);
                e.printStackTrace();
            } finally {
                if (in != null) in.close();
            }
        }
    }

    public String getData() {
        CharArrayWriter out = new CharArrayWriter();
        try {
            NanoXMLDOMOutput domo = new NanoXMLDOMOutput(new UMLFigureFactory());
            domo.writeObject(getDrawing());
            domo.save(out);
        } catch (IOException e) {
            TextFigure tf = new TextFigure();
            tf.setText(e.getMessage());
            tf.setBounds(new Point2D.Double(10, 10), new Point2D.Double(100, 100));
            getDrawing().add(tf);
            e.printStackTrace();
        } finally {
            if (out != null) out.close();
        }
        return out.toString();
    }

    public String[][] getParameterInfo() {
        return new String[][] { { "data", "String", "the data to be displayed by this applet." }, { "datafile", "URL", "an URL to a file containing the data to be displayed by this applet." } };
    }

    public String getAppletInfo() {
        return NAME + "\nVersion " + getVersion() + "\n\nCopyright 1996-2009 (c) by the original authors of JHotDraw and all its contributors" + "\nThis software is licensed under LGPL or" + "\nCreative Commons 3.0 BY";
    }

    private void initComponents() {
        toolButtonGroup = new javax.swing.ButtonGroup();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                JFrame f = new JFrame("JHotDraw Draw Applet");
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                UMLApplet a = new UMLApplet();
                f.getContentPane().add(a);
                a.init();
                f.setSize(500, 400);
                f.setVisible(true);
                a.start();
            }
        });
    }

    private javax.swing.ButtonGroup toolButtonGroup;
}
