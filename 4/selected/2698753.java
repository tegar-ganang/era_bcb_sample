package flames2d.io;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import flames2d.util.LogManager;

/**
 * Writes concurrent a buffered Image to a File.
 * @author AnjoVahldiek
 */
public class ConcurrentBufferedImageWriter implements Runnable {

    private File mOutput;

    private String mFormat;

    private BufferedImage mBI;

    /**
	 * Constructs a BufferedImageWriter which writes the Image in a concurrent way
	 * to a file specified by output with the format specified by format (includes
	 * file ending).
	 * @param output File to wich the image should be wirtten, not null.
	 * @param format Format in which the Image will be wirtten, not null.
	 */
    public ConcurrentBufferedImageWriter(File output, String format) {
        mOutput = output;
        mFormat = format;
    }

    /**
	 * Writes the specified image to a file. Write process ocurrs in another thread.
	 * 
	 * @param bi Image to be written.
	 * @return a new thread
	 */
    public Thread write(BufferedImage bi) {
        if (bi == null) {
            return null;
        }
        mBI = bi;
        Thread t = new Thread(this, "Concurrent Image Writer - " + mOutput.getName());
        t.start();
        return t;
    }

    /**
	 * Writes the image to a file and sets all variables to null.
	 */
    public void run() {
        try {
            ImageIO.write(mBI, mFormat, mOutput);
        } catch (IOException e) {
            LogManager.logWarning(this.getClass().getSimpleName(), e);
        }
        mOutput = null;
        mBI = null;
        mFormat = null;
    }
}
