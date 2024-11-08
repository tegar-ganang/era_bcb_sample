package org.vrspace.vrmlclient;

import org.vrspace.util.Logger;
import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.net.*;
import vrml.external.*;
import vrml.external.field.*;
import vrml.external.exception.*;

public class LoginManager extends NodeManager implements KeyListener {

    VRSpace applet;

    VRMLSceneManager scene;

    public Node node;

    EventInMFString conText;

    EventInSFVec2f conPos;

    EventInSFBool conHide;

    int viewNumberRows;

    int viewNumberCols;

    int curRow = 9;

    int curCol = 0;

    String[] text;

    String login;

    String password;

    TextField textField;

    boolean readingLogin;

    String host;

    int port;

    boolean locked = false;

    boolean displayed = true;

    public LoginManager(VRSpace applet, VRMLSceneManager scene) throws Exception {
        this.applet = applet;
        this.scene = scene;
        Browser browser = scene.browser;
        host = applet.host;
        port = applet.port;
        textField = applet.getOutput();
        node = browser.getNode("VRSpace_Login");
        try {
            conText = (EventInMFString) node.getEventIn("set_string");
            conPos = (EventInSFVec2f) node.getEventIn("set_cursor");
            conHide = (EventInSFBool) node.getEventIn("set_hide");
            viewNumberRows = 10;
            viewNumberCols = 60;
        } catch (Exception e) {
            Logger.logError("LoginManager load unsuccessful: " + e);
            throw e;
        }
        text = new String[viewNumberRows];
    }

    public void doLogin() {
        doLogin("");
    }

    public void doLogin(String errorMessage) {
        if (!locked) {
            locked = true;
            readingLogin = true;
            applet.requestFocus(this);
            setVisible(true);
            clear();
            writeat(0, 0, "VRSpace server " + host + ":" + port);
            writeat(1, 0, errorMessage);
            writeat(2, 0, "login   :");
            writeat(3, 0, "password:");
            setCaretPosition(2, 10);
        }
    }

    public void setVisible(boolean visible) {
        Logger.logDebug("Visible: " + displayed);
        if (visible && !displayed) {
            conHide.setValue(false);
            displayed = true;
        } else if (!visible && displayed) {
            conHide.setValue(true);
            displayed = false;
        }
    }

    public void loginSucceeded() {
        applet.releaseFocus(this);
        setVisible(false);
    }

    /**
   * Login & password input, connect to server,
   * start Connection and MovmementManager
  */
    private void readLogin(String arg) {
        String text = textField.getText().trim();
        if (!arg.equals("Enter")) {
            if (readingLogin) {
                writeLogin(text);
            } else {
                StringBuffer s = new StringBuffer(text.length());
                for (int i = 0; i < text.length(); i++) {
                    s.append('*');
                }
                writePassword(s.toString());
            }
        } else if (text.length() > 0) {
            if (readingLogin) {
                login = text;
                writePassword("");
                readingLogin = false;
            } else {
                locked = false;
                scene.login(login, text);
            }
            textField.setText("");
        }
    }

    public void writeLogin(String text) {
        writeat(2, 10, text);
    }

    public void writePassword(String text) {
        writeat(3, 10, text);
    }

    public void clear() {
        for (int i = 0; i < viewNumberRows; i++) {
            text[i] = "";
        }
        conText.setValue(text);
    }

    public void setCaretPosition(int row, int col) {
        float[] position = { row, col };
        conPos.setValue(position);
        curCol = col;
        curRow = row;
    }

    public void writeat(int row, int col, String s) {
        try {
            if (col >= text[row].length()) {
                StringBuffer tmp = new StringBuffer(text[row]);
                for (int i = text[row].length(); i < col; i++) {
                    tmp.append(' ');
                }
                tmp.append(s);
                text[row] = tmp.toString();
            } else {
                text[row] = text[row].substring(0, col) + s;
            }
            conText.setValue(text);
            setCaretPosition(row, text[row].length() + 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void write(String s) {
        writeat(curRow, curCol, s);
    }

    public void writeln(String s) {
        try {
            addLine(s);
            conText.setValue(text);
            setCaretPosition(curRow, -2);
            curCol = 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void addLine(String s) {
        if (curRow < text.length - 1) {
            text[++curRow] = s;
        } else {
            for (int i = 0; i < text.length - 1; i++) {
                text[i] = text[i + 1];
            }
            text[text.length - 1] = s;
        }
    }

    public void writelns(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            writeln(lines[i]);
        }
    }

    public void keyPressed(KeyEvent e) {
        String code = e.getKeyText(e.getKeyCode());
        if (code.equals("Enter")) {
            readLogin(code);
        }
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
        readLogin(new String(new char[] { e.getKeyChar() }));
    }
}
