package org.jhotdraw.samples.uml;

import org.jhotdraw.draw.Drawing;
import org.jhotdraw.draw.TextFigure;
import org.jhotdraw.gui.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import netscape.javascript.JSObject;
import org.jhotdraw.xml.*;

/**
 * UMLLiveConnectApplet. Supports loading and saving of images to JavaScript.
 *
 * @author  wrandels
 * @version $Id: UMLLiveConnectApplet.java,v 1.1 2009/10/18 20:41:48 cfm1 Exp $
 */
public class UMLLiveConnectApplet extends JApplet {

    private static final String VERSION = "7.0.8";

    private static final String NAME = "JHotDraw Draw";

    /** Initializes the applet UMLApplet */
    public void init() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Throwable e) {
        }
        Container c = getContentPane();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        String[] lines = getAppletInfo().split("\n");
        for (int i = 0; i < lines.length; i++) {
            c.add(new JLabel(lines[i]));
        }
        new Worker() {

            public Object construct() {
                Object result;
                try {
                    if (getParameter("data") != null && getParameter("data").length() > 0) {
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

            public void finished(Object result) {
                Container c = getContentPane();
                c.setLayout(new BorderLayout());
                c.removeAll();
                initComponents();
                if (result != null) {
                    if (result instanceof Drawing) {
                        setDrawing((Drawing) result);
                    } else if (result instanceof Throwable) {
                        getDrawing().add(new TextFigure(result.toString()));
                        ((Throwable) result).printStackTrace();
                    }
                }
                boolean isLiveConnect;
                try {
                    Class.forName("netscape.javascript.JSObject");
                    isLiveConnect = true;
                } catch (Throwable t) {
                    isLiveConnect = false;
                }
                loadButton.setEnabled(isLiveConnect && getParameter("dataread") != null);
                saveButton.setEnabled(isLiveConnect && getParameter("datawrite") != null);
                if (isLiveConnect) {
                    String methodName = getParameter("dataread");
                    if (methodName.indexOf('(') > 0) {
                        methodName = methodName.substring(0, methodName.indexOf('(') - 1);
                    }
                    JSObject win = JSObject.getWindow(UMLLiveConnectApplet.this);
                    Object data = win.call(methodName, new Object[0]);
                    if (data instanceof String) {
                        setData((String) data);
                    }
                }
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
        return new String[][] { { "data", "String", "the data to be displayed by this applet." }, { "datafile", "URL", "an URL to a file containing the data to be displayed by this applet." }, { "dataread", "function()", "the name of a JavaScript function which can be used to read the data." }, { "datawrite", "function()", "the name of a JavaScript function which can be used to write the data." } };
    }

    public String getAppletInfo() {
        return NAME + "\nVersion " + VERSION + "\n\nCopyright 1996-2008 (c) by the authors of JHotDraw" + "\nThis software is licensed under LGPL or" + "\nCreative Commons 3.0 BY";
    }

    /** This method is called from within the init() method to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {
        toolButtonGroup = new javax.swing.ButtonGroup();
        drawingPanel = new org.jhotdraw.samples.draw.DrawingPanel();
        jToolBar1 = new javax.swing.JToolBar();
        loadButton = new javax.swing.JButton();
        saveButton = new javax.swing.JButton();
        FormListener formListener = new FormListener();
        getContentPane().add(drawingPanel, java.awt.BorderLayout.CENTER);
        jToolBar1.setFloatable(false);
        loadButton.setText("Laden");
        loadButton.addActionListener(formListener);
        jToolBar1.add(loadButton);
        saveButton.setText("Speichern");
        saveButton.addActionListener(formListener);
        jToolBar1.add(saveButton);
        getContentPane().add(jToolBar1, java.awt.BorderLayout.SOUTH);
    }

    private class FormListener implements java.awt.event.ActionListener {

        public void actionPerformed(java.awt.event.ActionEvent evt) {
            if (evt.getSource() == loadButton) {
                UMLLiveConnectApplet.this.load(evt);
            } else if (evt.getSource() == saveButton) {
                UMLLiveConnectApplet.this.save(evt);
            }
        }
    }

    private void save(java.awt.event.ActionEvent evt) {
        try {
            String methodName = getParameter("datawrite");
            if (methodName.indexOf('(') > 0) {
                methodName = methodName.substring(0, methodName.indexOf('(') - 1);
            }
            JSObject win = JSObject.getWindow(this);
            Object result = win.call(methodName, new Object[] { getData() });
        } catch (Throwable t) {
            TextFigure tf = new TextFigure("Fehler: " + t);
            AffineTransform tx = new AffineTransform();
            tx.translate(10, 20);
            tf.transform(tx);
            getDrawing().add(tf);
        }
    }

    private void load(java.awt.event.ActionEvent evt) {
        try {
            String methodName = getParameter("dataread");
            if (methodName.indexOf('(') > 0) {
                methodName = methodName.substring(0, methodName.indexOf('(') - 1);
            }
            JSObject win = JSObject.getWindow(this);
            Object result = win.call(methodName, new Object[0]);
            if (result instanceof String) {
                setData((String) result);
            }
        } catch (Throwable t) {
            TextFigure tf = new TextFigure("Fehler: " + t);
            AffineTransform tx = new AffineTransform();
            tx.translate(10, 20);
            tf.transform(tx);
            getDrawing().add(tf);
        }
    }

    private org.jhotdraw.samples.draw.DrawingPanel drawingPanel;

    private javax.swing.JToolBar jToolBar1;

    private javax.swing.JButton loadButton;

    private javax.swing.JButton saveButton;

    private javax.swing.ButtonGroup toolButtonGroup;
}
