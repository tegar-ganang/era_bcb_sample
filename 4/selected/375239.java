package org.mobicents.servlet.sip.demo.jruby;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.log4j.Logger;

/**
 * @author <A HREF="mailto:jean.deruelle@gmail.com">Jean Deruelle</A> 
 * 
 */
public class InitializationListener implements ServletContextListener {

    private static Logger logger = Logger.getLogger(InitializationListener.class);

    private static final String AUDIO_DIR = "/audio";

    private static final String FILE_PROTOCOL = "file://";

    private static final String[] AUDIO_FILES = new String[] { "complaint.wav" };

    public void contextDestroyed(ServletContextEvent arg0) {
    }

    public void contextInitialized(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();
        File tempWriteDir = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
        servletContext.setAttribute("audioFilePath", FILE_PROTOCOL + tempWriteDir.getAbsolutePath() + File.separatorChar);
        for (int i = 0; i < AUDIO_FILES.length; i++) {
            String audioFile = AUDIO_FILES[i];
            logger.info("Writing " + audioFile + " to webapp temp dir : " + tempWriteDir);
            InputStream is = servletContext.getResourceAsStream(AUDIO_DIR + "/" + audioFile);
            copyToTempDir(is, tempWriteDir, audioFile);
        }
    }

    private void copyToTempDir(InputStream is, File tempWriteDir, String fileName) {
        File file = new File(tempWriteDir, fileName);
        final int bufferSize = 1000;
        BufferedOutputStream fout = null;
        BufferedInputStream fin = null;
        try {
            fout = new BufferedOutputStream(new FileOutputStream(file));
            fin = new BufferedInputStream(is);
            byte[] buffer = new byte[bufferSize];
            int readCount = 0;
            while ((readCount = fin.read(buffer)) != -1) {
                if (readCount < bufferSize) {
                    fout.write(buffer, 0, readCount);
                } else {
                    fout.write(buffer);
                }
            }
        } catch (IOException e) {
            logger.error("An unexpected exception occured while copying audio files", e);
        } finally {
            try {
                if (fout != null) {
                    fout.flush();
                    fout.close();
                }
            } catch (IOException e) {
                logger.error("An unexpected exception while closing stream", e);
            }
            try {
                if (fin != null) {
                    fin.close();
                }
            } catch (IOException e) {
                logger.error("An unexpected exception while closing stream", e);
            }
        }
    }
}
