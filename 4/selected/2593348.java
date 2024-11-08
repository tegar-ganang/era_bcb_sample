package com.decentric;

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.html.*;
import org.w3c.dom.Document;
import org.apache.xerces.parsers.DOMParser;
import org.apache.xml.serialize.DOMSerializer;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.Serializer;
import org.apache.xml.serialize.XMLSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import net.decentric.*;
import net.decentric.util.*;

public class Browser implements ActionListener {

    private JEditorPane outPane = null;

    private JEditorPane inPane = null;

    private Qore qore = new Qore();

    public void actionPerformed(java.awt.event.ActionEvent e) {
        if (e.getActionCommand().equals("new")) {
            try {
                FileReader reader = new FileReader("resources/defaultMessage.xml");
                StringWriter writer = new StringWriter();
                while (reader.ready()) {
                    writer.write(reader.read());
                }
                StringBuffer strBuffer = writer.getBuffer();
                inPane.setText(strBuffer.toString());
            } catch (FileNotFoundException ex) {
                System.err.println(ex);
            } catch (IOException ex) {
                System.err.println(ex);
            }
        } else if (e.getActionCommand().equals("sign")) {
            StringReader strReader = new StringReader(inPane.getText());
            StringWriter strWriter = new StringWriter();
            DOMParser domp = new DOMParser();
            Document doc = null;
            Document signedDocument = null;
            try {
                domp.parse(new InputSource(strReader));
                doc = domp.getDocument();
                signedDocument = qore.sign(doc);
                OutputFormat format = new OutputFormat(signedDocument, "UTF-8", false);
                format.setPreserveSpace(true);
                Serializer ser = new XMLSerializer(format);
                ser.setOutputCharStream(strWriter);
                DOMSerializer domser = ser.asDOMSerializer();
                domser.serialize(signedDocument);
                inPane.setText(strWriter.toString());
                strWriter.close();
            } catch (IOException ex) {
                System.err.println("Exception: Browser::actionPerformed() : " + ex);
            } catch (SAXException ex) {
                System.err.println("Exception: Browser::actionPerformed() : " + ex);
            }
        } else if (e.getActionCommand().equals("enqueue")) {
        }
    }

    /**
     * The GUI to the Q
     */
    public Browser() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            JFrame frame = new JFrame("Decentric Peer");
            GridBagLayout gridbag = new GridBagLayout();
            GridBagConstraints c = new GridBagConstraints();
            Container contentPane = frame.getContentPane();
            contentPane.setLayout(gridbag);
            JLabel inLabel = new JLabel("Input");
            JButton newButton = new JButton("New");
            JButton signButton = new JButton("Sign");
            JButton enqueueButton = new JButton("Enqueue");
            JScrollPane inScroller = new JScrollPane();
            inPane = new JEditorPane("text/plain", "Hello world...");
            inPane.setEditable(true);
            inScroller.getViewport().add(inPane);
            newButton.setActionCommand("new");
            newButton.addActionListener(this);
            signButton.setActionCommand("sign");
            signButton.addActionListener(this);
            enqueueButton.setActionCommand("enqueue");
            enqueueButton.addActionListener(this);
            JLabel outLabel = new JLabel("Output");
            JScrollPane outScroller = new JScrollPane();
            outPane = new JEditorPane(new URL("http://www.fsf.org/"));
            outPane.setEditable(false);
            outPane.addHyperlinkListener(createHyperLinkListener());
            outScroller.getViewport().add(outPane);
            c.fill = GridBagConstraints.BOTH;
            c.anchor = GridBagConstraints.CENTER;
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 1;
            c.weighty = 0;
            gridbag.setConstraints(inLabel, c);
            contentPane.add(inLabel);
            c.gridx = 1;
            c.gridy = 0;
            gridbag.setConstraints(newButton, c);
            contentPane.add(newButton);
            c.gridx = 2;
            c.gridy = 0;
            gridbag.setConstraints(signButton, c);
            contentPane.add(signButton);
            c.gridx = 3;
            c.gridy = 0;
            gridbag.setConstraints(enqueueButton, c);
            contentPane.add(enqueueButton);
            c.gridwidth = 4;
            c.gridx = 0;
            c.gridy = 1;
            c.weighty = 1;
            gridbag.setConstraints(inScroller, c);
            contentPane.add(inScroller);
            c.gridx = 0;
            c.gridy = 2;
            c.weighty = 0;
            gridbag.setConstraints(outLabel, c);
            contentPane.add(outLabel);
            c.gridx = 0;
            c.gridy = 3;
            c.weighty = 1;
            gridbag.setConstraints(outScroller, c);
            contentPane.add(outScroller);
            frame.addWindowListener(new WindowAdapter() {

                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });
            frame.setLocation(10, 10);
            frame.setSize(420, 420);
            frame.setVisible(true);
        } catch (IOException ex) {
            System.err.println(ex);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public HyperlinkListener createHyperLinkListener() {
        return new HyperlinkListener() {

            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (e instanceof HTMLFrameHyperlinkEvent) {
                        ((HTMLDocument) outPane.getDocument()).processHTMLFrameHyperlinkEvent((HTMLFrameHyperlinkEvent) e);
                    } else {
                        try {
                            outPane.setPage(e.getURL());
                        } catch (IOException ioe) {
                            System.out.println("IOE: " + ioe);
                        }
                    }
                }
            }
        };
    }

    public static void main(String args[]) {
        new Browser();
    }
}
