package org.cybergarage.clock;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import javax.swing.*;
import org.cybergarage.util.*;
import org.cybergarage.upnp.device.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class ClockFrame extends JFrame implements Runnable, WindowListener {

    private static final String DESCRIPTION_FILE_URL = "/description/description.xml";

    private static final String TITLE = "CyberLink Sample Clock";

    private ClockDevice clockDev;

    private ClockPane clockPane;

    BundleContext bc;

    public ClockFrame(BundleContext bc) {
        super(TITLE);
        this.bc = bc;
        try {
            File f = new File("clock_description.xml");
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
            clockDev = new ClockDevice(f);
        } catch (InvalidDescriptionException e) {
            Debug.warning(e);
        }
        getContentPane().setLayout(new BorderLayout());
        clockPane = new ClockPane();
        getContentPane().add(clockPane, BorderLayout.CENTER);
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
    }

    public ClockPane getClockPanel() {
        return clockPane;
    }

    public ClockDevice getClockDevice() {
        return clockDev;
    }

    private Thread timerThread = null;

    public void run() {
        Thread thisThread = Thread.currentThread();
        while (timerThread == thisThread) {
            getClockDevice().update();
            getClockPanel().repaint();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
    }

    public void start() {
        clockDev.start();
        timerThread = new Thread(this);
        timerThread.start();
    }

    public void stop() {
        clockDev.stop();
        timerThread = null;
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        stop();
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
