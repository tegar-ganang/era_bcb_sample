package org.jdamico.ircivelaclient.view;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLEditorKit;
import jerklib.Channel;
import jerklib.Session;
import org.jdamico.ircivelaclient.config.Constants;
import org.jdamico.ircivelaclient.util.IRCIvelaClientStringUtils;

public class PvtChatPanel extends JPanel {

    private JEditorPane mainContentArea;

    private JTextArea messageArea;

    private JButton close;

    private String title;

    private HandleApplet parent;

    private Document doc = null;

    private int row = 0;

    private Session session;

    public PvtChatPanel(final HandleApplet parent, final String title, Session session) {
        this.setLayout(null);
        this.title = title;
        this.parent = parent;
        this.session = session;
        this.mainContentArea = new JEditorPane();
        this.messageArea = new JTextArea();
        this.mainContentArea.setEditable(false);
        this.mainContentArea.setContentType(Constants.MAINCONTENT_CONTENT_TYPE);
        this.mainContentArea.setEditorKit(new HTMLEditorKit());
        JScrollPane mainScrollPane = new JScrollPane(this.mainContentArea);
        this.messageArea.setEnabled(true);
        this.messageArea.setAutoscrolls(true);
        this.messageArea.setLineWrap(true);
        this.messageArea.setWrapStyleWord(false);
        messageArea.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                msgKeyPressed(evt);
            }

            private void msgKeyPressed(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
                    String tempMsg = messageArea.getText().replaceAll("Constants.TEACHER_IDENTIFIER", Constants.BLANK_STRING);
                    tempMsg = messageArea.getText().replaceAll(Constants.LINE_BREAK, Constants.BLANK_STRING);
                    updateMainContentArea("Me: " + tempMsg, "blue", StaticData.isTeacher);
                    sendMessage();
                }
            }
        });
        this.close = new JButton("Close");
        this.close.setLocation(810, 5);
        this.close.setSize(100, 20);
        this.close.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                parent.removeTab(title);
            }
        });
        mainScrollPane.setBounds(5, 5, 800, 390);
        this.mainContentArea.setBackground(Color.WHITE);
        messageArea.setBounds(5, 400, 904, 70);
        messageArea.setBorder(BorderFactory.createLineBorder(Color.black));
        this.add(mainScrollPane);
        this.add(this.messageArea);
        this.add(close);
    }

    public void updateMainContentArea(String msg, String color, boolean isTeacher) {
        mainContentArea.scrollRectToVisible(new Rectangle(0, mainContentArea.getBounds(null).height, 1, 1));
        appendText(mainContentArea, IRCIvelaClientStringUtils.singleton().setMessage(msg, row, color, isTeacher, true));
        row++;
        mainContentArea.setFocusable(true);
        mainContentArea.setVisible(true);
        mainContentArea.setEnabled(true);
    }

    private JEditorPane appendText(JEditorPane tA, String text) {
        doc = (Document) tA.getDocument();
        try {
            ((HTMLEditorKit) tA.getEditorKit()).read(new java.io.StringReader(text), tA.getDocument(), tA.getDocument().getLength());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        tA.setCaretPosition(doc.getLength());
        return tA;
    }

    public void sendMessage() {
        String tempMsg = messageArea.getText().replaceAll("Constants.TEACHER_IDENTIFIER", Constants.BLANK_STRING);
        tempMsg = messageArea.getText().replaceAll(Constants.LINE_BREAK, Constants.BLANK_STRING);
        Channel channel = session.getChannel(StaticData.channel);
        session.sayPrivate(title.trim(), tempMsg);
        messageArea.setText(Constants.BLANK_STRING);
        messageArea.setFocusable(true);
    }
}
