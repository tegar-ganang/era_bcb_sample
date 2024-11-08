package org.cybergarage.tv;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import javax.swing.*;
import org.cybergarage.upnp.device.InvalidDescriptionException;
import org.cybergarage.util.Debug;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class TvFrame extends JFrame implements Runnable, WindowListener {

    private static final String DESCRIPTION_FILE_URL = "/description/description.xml";

    private static final String TITLE = "CyberLink Sample TV";

    private TvDevice tvDev;

    private TvPane tvPane;

    private BundleContext bc;

    public TvFrame(BundleContext bc) {
        super(TITLE);
        this.bc = bc;
        try {
            File f = new File("tv_description.xml");
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
            tvDev = new TvDevice(f);
        } catch (InvalidDescriptionException e) {
            Debug.warning(e);
        }
        getContentPane().setLayout(new BorderLayout());
        tvPane = new TvPane();
        tvDev.setComponent(tvPane);
        tvPane.setDevice(tvDev);
        getContentPane().add(tvPane, BorderLayout.CENTER);
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

    public TvPane getTvPanel() {
        return tvPane;
    }

    public TvDevice getTvDevice() {
        return tvDev;
    }

    private Thread timerThread = null;

    public void run() {
        Thread thisThread = Thread.currentThread();
        while (timerThread == thisThread) {
            tvDev.setMessage("");
            tvPane.repaint();
            try {
                Thread.sleep(1000 * 5);
            } catch (InterruptedException e) {
            }
        }
    }

    public void start() {
        tvDev.start();
        timerThread = new Thread(this);
        timerThread.start();
    }

    public void stop() {
        tvDev.stop();
        timerThread = null;
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        tvDev.off();
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
