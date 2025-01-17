package org.jhotdraw.samples.pert;

import org.jhotdraw.draw.io.OutputFormat;
import org.jhotdraw.draw.io.InputFormat;
import org.jhotdraw.draw.io.ImageOutputFormat;
import org.jhotdraw.draw.io.DOMStorableInputOutputFormat;
import org.jhotdraw.draw.*;
import org.jhotdraw.gui.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import org.jhotdraw.xml.*;

/**
 * PertApplet.
 *
 * @author Werner Randelshofer
 * @version $Id: PertApplet.java 717 2010-11-21 12:30:57Z rawcoder $
 */
public class PertApplet extends JApplet {

    private static final String NAME = "JHotDraw Pert";

    private PertPanel drawingPanel;

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
        return PertApplet.class.getPackage().getImplementationVersion();
    }

    /**
     * Initializes the applet PertApplet
     */
    @Override
    public void init() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Throwable e) {
        }
        try {
            PopupFactory.setSharedInstance(new PopupFactory());
        } catch (Throwable e) {
        }
        Container c = getContentPane();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        String[] labels = getAppletInfo().split("\n");
        for (int i = 0; i < labels.length; i++) {
            c.add(new JLabel((labels[i].length() == 0) ? " " : labels[i]));
        }
        new Worker<Drawing>() {

            @Override
            public Drawing construct() throws IOException {
                Drawing result;
                System.out.println("getParameter.datafile:" + getParameter("datafile"));
                if (getParameter("data") != null) {
                    NanoXMLDOMInput domi = new NanoXMLDOMInput(new PertFactory(), new StringReader(getParameter("data")));
                    result = (Drawing) domi.readObject(0);
                } else if (getParameter("datafile") != null) {
                    URL url = new URL(getDocumentBase(), getParameter("datafile"));
                    InputStream in = url.openConnection().getInputStream();
                    try {
                        NanoXMLDOMInput domi = new NanoXMLDOMInput(new PertFactory(), in);
                        result = (Drawing) domi.readObject(0);
                    } finally {
                        in.close();
                    }
                } else {
                    result = null;
                }
                return result;
            }

            @Override
            protected void done(Drawing result) {
                Container c = getContentPane();
                c.setLayout(new BorderLayout());
                c.removeAll();
                c.add(drawingPanel = new PertPanel());
                initComponents();
                if (result != null) {
                    setDrawing(result);
                }
            }

            @Override
            protected void failed(Throwable value) {
                Container c = getContentPane();
                c.setLayout(new BorderLayout());
                c.removeAll();
                c.add(drawingPanel = new PertPanel());
                value.printStackTrace();
                initComponents();
                getDrawing().add(new TextFigure(value.toString()));
                value.printStackTrace();
            }

            @Override
            protected void finished() {
                Container c = getContentPane();
                initDrawing(getDrawing());
                c.validate();
            }
        }.start();
    }

    private void setDrawing(Drawing d) {
        drawingPanel.setDrawing(d);
    }

    private Drawing getDrawing() {
        return drawingPanel.getDrawing();
    }

    /**
     * Configure Drawing object to support copy and paste.
     */
    private void initDrawing(Drawing d) {
        LinkedList<InputFormat> inputFormats = new LinkedList<InputFormat>();
        LinkedList<OutputFormat> outputFormats = new LinkedList<OutputFormat>();
        DOMStorableInputOutputFormat ioFormat = new DOMStorableInputOutputFormat(new PertFactory());
        inputFormats.add(ioFormat);
        outputFormats.add(ioFormat);
        outputFormats.add(new ImageOutputFormat());
        d.setInputFormats(inputFormats);
        d.setOutputFormats(outputFormats);
    }

    public void setData(String text) {
        if (text != null && text.length() > 0) {
            StringReader in = new StringReader(text);
            try {
                NanoXMLDOMInput domi = new NanoXMLDOMInput(new PertFactory(), in);
                setDrawing((Drawing) domi.readObject(0));
            } catch (Throwable e) {
                getDrawing().removeAllChildren();
                TextFigure tf = new TextFigure();
                tf.setText(e.getMessage());
                tf.setBounds(new Point2D.Double(10, 10), new Point2D.Double(100, 100));
                getDrawing().add(tf);
                e.printStackTrace();
            } finally {
                in.close();
            }
        }
    }

    public String getData() {
        CharArrayWriter out = new CharArrayWriter();
        try {
            NanoXMLDOMOutput domo = new NanoXMLDOMOutput(new PertFactory());
            domo.writeObject(getDrawing());
            domo.save(out);
        } catch (IOException e) {
            TextFigure tf = new TextFigure();
            tf.setText(e.getMessage());
            tf.setBounds(new Point2D.Double(10, 10), new Point2D.Double(100, 100));
            getDrawing().add(tf);
            e.printStackTrace();
        } finally {
            out.close();
        }
        return out.toString();
    }

    @Override
    public String[][] getParameterInfo() {
        return new String[][] { { "data", "String", "the data to be displayed by this applet." }, { "datafile", "URL", "an URL to a file containing the data to be displayed by this applet." } };
    }

    @Override
    public String getAppletInfo() {
        return NAME + "\nVersion " + getVersion() + "\n\nCopyright 1996-2010 (c) by the original authors of JHotDraw and all its contributors" + "\nThis software is licensed under LGPL or" + "\nCreative Commons 3.0 BY";
    }

    private void initComponents() {
        toolButtonGroup = new javax.swing.ButtonGroup();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                JFrame f = new JFrame("JHotDraw Pert Applet");
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                PertApplet a = new PertApplet();
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
