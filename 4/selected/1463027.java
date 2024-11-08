package it.uniroma1.dis.omega.qospolicyhandler.gui;

import it.uniroma1.dis.omega.qospolicyhandler.device.PolicyHandlerDevice;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import javax.swing.JFrame;
import org.cybergarage.upnp.device.InvalidDescriptionException;
import org.cybergarage.util.Debug;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class PolicyHandlerFrame extends JFrame implements Runnable, WindowListener {

    /**
	 * 	Default SerialVersionUID
	 */
    private static final long serialVersionUID = 1L;

    private static final String DESCRIPTION_FILE_URL = "/description/description.xml";

    private static final String TITLE = "UPnP QoS Policy Handler";

    private PolicyHandlerPane phPane;

    private PolicyHandlerDevice phDev;

    private BundleContext bc;

    public PolicyHandlerFrame(BundleContext bc) {
        super(TITLE);
        this.bc = bc;
        try {
            File f = new File("qos-policyhandler_description.xml");
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
            phDev = new PolicyHandlerDevice(f);
            getContentPane().setLayout(new BorderLayout());
            phPane = new PolicyHandlerPane(phDev);
            getContentPane().add(phPane, BorderLayout.CENTER);
            addWindowListener(this);
        } catch (InvalidDescriptionException e) {
            Debug.warning(e);
        }
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
        this.setSize(new Dimension(280, 230));
        this.setVisible(true);
    }

    public PolicyHandlerPane getMediaClientPanel() {
        return phPane;
    }

    private Thread timerThread = null;

    public void run() {
        Thread thisThread = Thread.currentThread();
        while (timerThread == thisThread) {
            phPane.repaint();
            try {
                Thread.sleep(1000 * 5);
            } catch (InterruptedException e) {
            }
        }
    }

    public void start() {
        timerThread = new Thread(this);
        timerThread.start();
        phDev.start();
    }

    public void stop() {
        timerThread = null;
        phDev.stop();
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
