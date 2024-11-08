package com.memoire.vainstall.xui;

import java.awt.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;
import com.memoire.vainstall.VAGlobals;

/**
 * @version      $Id: XuiImagePanel.java,v 1.1.1.1 2001/03/30 21:25:38 vonarnim Exp $
 * @author       Guillaume Desnoix
 */
public class XuiImagePanel extends XuiPanel {

    public static final XuiImagePanel IMAGE_PANEL = new XuiImagePanel();

    public XuiImagePanel() {
        super();
        setBackground(Color.white);
        setForeground(new Color(255, 224, 192));
        InputStream imgStream = VAGlobals.BASE_CLASS.getResourceAsStream("/" + VAGlobals.IMAGE);
        if (imgStream == null) {
        } else {
            ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
            try {
                byte[] buf = new byte[1024];
                int read = imgStream.read(buf, 0, buf.length);
                while (read > 0) {
                    dataStream.write(buf, 0, read);
                    read = imgStream.read(buf, 0, buf.length);
                }
                imgStream.close();
                JLabel img = new JLabel(new ImageIcon(dataStream.toByteArray()));
                dataStream.close();
                add(img);
            } catch (IOException ex) {
            }
        }
    }
}
