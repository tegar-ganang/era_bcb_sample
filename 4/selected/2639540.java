package org.cybergarage.light;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import javax.swing.*;
import org.cybergarage.util.*;
import org.cybergarage.upnp.device.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class LightFrame extends JFrame implements WindowListener {

    private static final String DESCRIPTION_FILE_URL = "/description/description.xml";

    private static final long serialVersionUID = 1L;

    private static final String TITLE = "CyberLink Sample Light";

    private LightDevice lightDev;

    private LightPane lightPane;

    private BundleContext bc;

    public LightFrame(BundleContext bc) {
        super(TITLE);
        this.bc = bc;
        try {
            File f = new File("light_description.xml");
            try {
                InputStream inputStream = getClass().getResourceAsStream(DESCRIPTION_FILE_URL);
                OutputStream out = new FileOutputStream(f);
                byte buf[] = new byte[1024];
                int len;
                while ((len = inputStream.read(buf)) > 0) out.write(buf, 0, len);
                out.close();
                inputStream.close();
            } catch (IOException e) {
            }
            lightDev = new LightDevice(f);
        } catch (InvalidDescriptionException e) {
            Debug.warning(e);
        }
        getContentPane().setLayout(new BorderLayout());
        lightPane = new LightPane();
        lightPane.setDevice(lightDev);
        lightDev.setComponent(lightPane);
        getContentPane().add(lightPane, BorderLayout.CENTER);
        addWindowListener(this);
        try {
            Image icon = null;
            String localPath = "/images/Icon16x16.png";
            URL url = getClass().getResource(localPath);
            if (url != null) {
                icon = Toolkit.getDefaultToolkit().createImage(url);
                setIconImage(icon);
            } else {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        pack();
        setVisible(true);
        lightDev.start();
    }

    public LightPane getClockPanel() {
        return lightPane;
    }

    public LightDevice getClockDevice() {
        return lightDev;
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        lightDev.stop();
        try {
            bc.getBundle().stop();
        } catch (BundleException BE) {
        }
    }

    public void windowDeactivated(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

    public static void main(String args[]) {
    }
}
