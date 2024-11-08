package rmi.remote.screenshot;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageDecoder;

public class Teacher extends JFrame {

    public static final int PORT = 5555;

    private static final long SCREEN_SHOT_PERIOD = 2000;

    private static final int WINDOW_HEIGHT = 400;

    private static final int WINDOW_WIDTH = 500;

    private final ObjectInputStream in;

    private final ObjectOutputStream out;

    private final String studentName;

    private final JLabel iconLabel = new JLabel();

    private final RobotActionQueue jobs = new RobotActionQueue();

    private final Thread writer;

    private final Timer timer;

    private boolean running = true;

    public Teacher(Socket socket) throws IOException, ClassNotFoundException {
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
        studentName = (String) in.readObject();
        setupUI();
        createReaderThread();
        timer = createScreenShotThread();
        writer = createWriterThread();
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                timer.cancel();
            }
        });
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                try {
                    out.close();
                } catch (IOException ex) {
                }
                try {
                    in.close();
                } catch (IOException ex) {
                }
            }
        });
        System.out.println("finished connecting to " + socket);
    }

    private Timer createScreenShotThread() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            public void run() {
                jobs.add(new ScreenShot());
            }
        }, 1, SCREEN_SHOT_PERIOD);
        return timer;
    }

    private void setupUI() {
        setTitle("Screen from " + studentName);
        getContentPane().add(new JScrollPane(iconLabel));
        iconLabel.addMouseListener(new MouseAdapter() {

            public void mouseClicked(MouseEvent e) {
                if (running) {
                    jobs.add(new MoveMouse(e));
                    jobs.add(new ClickMouse(e));
                    jobs.add(new ScreenShot());
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        });
        iconLabel.addKeyListener(new KeyListener() {

            public void keyPressed(KeyEvent arg0) {
                jobs.add(new KeyPress(arg0));
            }

            public void keyReleased(KeyEvent arg0) {
            }

            public void keyTyped(KeyEvent arg0) {
                jobs.add(new KeyPress(arg0));
            }
        });
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setVisible(true);
    }

    private Thread createWriterThread() {
        Thread writer = new Thread("Writer") {

            public void run() {
                try {
                    while (true) {
                        RobotAction action = jobs.next();
                        out.writeObject(action);
                        out.flush();
                    }
                } catch (Exception e) {
                    System.out.println("Connection to " + studentName + " closed (" + e + ')');
                    setTitle(getTitle() + " - disconnected");
                }
            }
        };
        writer.start();
        return writer;
    }

    private void showIcon(byte[] byteImage) throws IOException {
        ByteArrayInputStream bin = new ByteArrayInputStream(byteImage);
        JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(bin);
        final BufferedImage img = decoder.decodeAsBufferedImage();
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                iconLabel.setIcon(new ImageIcon(img));
            }
        });
    }

    private void createReaderThread() {
        Thread readThread = new Thread() {

            public void run() {
                while (true) {
                    try {
                        byte[] img = (byte[]) in.readObject();
                        System.out.println("Received screenshot of " + img.length + " bytes from " + studentName);
                        showIcon(img);
                    } catch (Exception ex) {
                        System.out.println("Exception occurred: " + ex);
                        writer.interrupt();
                        timer.cancel();
                        running = false;
                        return;
                    }
                }
            }
        };
        readThread.start();
    }

    public static void main(String[] args) throws Exception {
        ServerSocket ss = new ServerSocket(PORT);
        while (true) {
            Socket socket = ss.accept();
            System.out.println("Connection From " + socket);
            new Teacher(socket);
        }
    }
}
