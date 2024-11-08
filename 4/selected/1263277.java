package org.cybergarage.washer;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import javax.swing.*;
import org.cybergarage.util.*;
import org.cybergarage.upnp.device.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class WasherFrame extends JFrame implements WindowListener {

    private static final String DESCRIPTION_FILE_URL = "/description/description.xml";

    private static final String TITLE = "CyberLink Sample Washer";

    private WasherDevice washerDev;

    private WasherPane washerPane;

    private BundleContext bc;

    public WasherFrame(BundleContext bc) {
        super(TITLE);
        this.bc = bc;
        try {
            File f = new File("washer_description.xml");
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
            washerDev = new WasherDevice(f);
        } catch (InvalidDescriptionException e) {
            Debug.warning(e);
        }
        getContentPane().setLayout(new BorderLayout());
        washerPane = new WasherPane();
        washerPane.setDevice(washerDev);
        washerDev.setPanel(washerPane);
        getContentPane().add(washerPane, BorderLayout.CENTER);
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
        washerDev.start();
    }

    public WasherPane getPanel() {
        return washerPane;
    }

    public WasherDevice getDevice() {
        return washerDev;
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        washerDev.stop();
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
