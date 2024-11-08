package it.uniroma1.dis.omega.qosmediaclient.gui;

import it.uniroma1.dis.omega.qosmediaclient.device.MediaClientDevice;
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

public class MediaClientFrame extends JFrame implements Runnable, WindowListener {

    /**
	 * 	Default SerialVersionUID
	 */
    private static final long serialVersionUID = 1L;

    private static final String DESCRIPTION_FILE_URL = "/description/description.xml";

    private static final String TITLE = "UPnP QoS Media Client";

    private MediaClientPane mcPane;

    private MediaClientDevice mcDev;

    private BundleContext bc;

    /**
	 * 
	 * @param bc
	 */
    public MediaClientFrame(BundleContext bc) {
        super(TITLE);
        this.bc = bc;
        try {
            File f = new File("qos-mediaclient_description.xml");
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
            mcDev = new MediaClientDevice(f, bc);
            getContentPane().setLayout(new BorderLayout());
            mcPane = new MediaClientPane(mcDev);
            getContentPane().add(mcPane, BorderLayout.CENTER);
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
        this.setSize(new Dimension(410, 400));
        setVisible(true);
    }

    private Thread timerThread = null;

    /**
	 * Every 5 seconds it makes the repaint
	 */
    public void run() {
        Thread thisThread = Thread.currentThread();
        while (timerThread == thisThread) {
            mcPane.repaint();
            try {
                Thread.sleep(1000 * 5);
            } catch (InterruptedException e) {
            }
        }
    }

    /**
	 * Start of the thread
	 */
    public void start() {
        timerThread = new Thread(this);
        timerThread.start();
        mcDev.start();
    }

    /**
	 * Stop of the thread
	 */
    public void stop() {
        timerThread = null;
        mcDev.stop();
    }

    /**
	 * not implementation
	 */
    public void windowActivated(WindowEvent e) {
    }

    /**
	 * not implementation
	 */
    public void windowClosed(WindowEvent e) {
    }

    /**
	 * capture windowClosing event
	 */
    public void windowClosing(WindowEvent e) {
        stop();
        try {
            bc.getBundle().stop();
        } catch (BundleException BE) {
        }
    }

    /**
	 * not implementation
	 */
    public void windowDeactivated(WindowEvent e) {
    }

    /**
	 * not implementation
	 */
    public void windowDeiconified(WindowEvent e) {
    }

    /**
	 * not implementation
	 */
    public void windowIconified(WindowEvent e) {
    }

    /**
	 * not implementation
	 */
    public void windowOpened(WindowEvent e) {
    }
}
