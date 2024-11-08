package gnu.saw.server.screenshot;

import gnu.saw.graphics.device.SAWUsableGraphicalDeviceResolver;
import gnu.saw.graphics.screencapture.SAWAWTScreenCaptureProvider;
import gnu.saw.server.connection.SAWServerConnection;
import gnu.saw.server.session.SAWServerSession;
import java.awt.GraphicsDevice;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.imageio.ImageIO;

public class SAWServerScreenshotTask implements Runnable {

    private volatile boolean finished;

    private volatile boolean drawPointer;

    private volatile Integer deviceNumber;

    private DateFormat dateTimeFormat;

    private BufferedOutputStream photoOutputStream;

    private File photoFile;

    private GregorianCalendar clock;

    private SAWServerConnection connection;

    private SAWServerSession session;

    private SAWAWTScreenCaptureProvider screenshotProvider;

    public SAWServerScreenshotTask(SAWServerSession session) {
        this.session = session;
        this.connection = session.getConnection();
        this.screenshotProvider = session.getScreenshotProvider();
        this.drawPointer = false;
        this.clock = new GregorianCalendar();
        this.dateTimeFormat = new SimpleDateFormat("MM-dd-G][HH.mm.ss.SSS-z]");
        this.finished = true;
        this.deviceNumber = null;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public void setColorQuality(int colorQuality) {
        this.screenshotProvider.setColorQuality(colorQuality);
    }

    public void setDrawPointer(boolean drawPointer) {
        this.drawPointer = drawPointer;
    }

    public void setDeviceNumber(Integer deviceNumber) {
        this.deviceNumber = deviceNumber;
    }

    public void run() {
        try {
            if (deviceNumber != null) {
                GraphicsDevice[] devices = SAWUsableGraphicalDeviceResolver.getRasterDevices();
                if (devices != null) {
                    deviceNumber = Math.min(deviceNumber - 1, devices.length - 1);
                    screenshotProvider.setGraphicsDevice(devices[deviceNumber]);
                }
            } else {
                screenshotProvider.resetGraphicsDevice();
            }
            if (!screenshotProvider.isScreenCaptureInitialized() && !screenshotProvider.initializeScreenCapture()) {
                synchronized (this) {
                    try {
                        connection.getResultWriter().write("\nSAW>SAWSCREENSHOT:Screen capture cannot start on server!\nSAW>");
                        connection.getResultWriter().flush();
                    } catch (Exception e) {
                    }
                    finished = true;
                    return;
                }
            }
            connection.getResultWriter().write("\nSAW>SAWSCREENSHOT:Trying screen capture...\nSAW>");
            connection.getResultWriter().flush();
            clock.setTime(Calendar.getInstance().getTime());
            photoFile = new File(session.getWorkingDirectory(), "[" + clock.get(GregorianCalendar.YEAR) + "-" + dateTimeFormat.format(clock.getTime()) + ".png");
            photoOutputStream = new BufferedOutputStream(Channels.newOutputStream(new FileOutputStream(photoFile).getChannel()));
            BufferedImage screenCapture = screenshotProvider.createScreenCapture(drawPointer);
            ImageIO.write(screenCapture, "png", photoOutputStream);
            photoOutputStream.flush();
            synchronized (this) {
                connection.getResultWriter().write("\nSAW>SAWSCREENSHOT:Screen capture saved in:\nSAW>SAWSCREENSHOT:" + photoFile.getAbsolutePath() + "\nSAW>");
                connection.getResultWriter().flush();
                finished = true;
            }
        } catch (Exception e) {
            synchronized (this) {
                try {
                    connection.getResultWriter().write("\nSAW>SAWSCREENSHOT:Screen capture failed!\nSAW>");
                    connection.getResultWriter().flush();
                } catch (Exception e1) {
                }
                finished = true;
            }
        }
        if (photoOutputStream != null) {
            try {
                photoOutputStream.close();
            } catch (IOException e) {
            }
        }
        finished = true;
    }
}
