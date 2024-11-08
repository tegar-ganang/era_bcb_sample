package gnu.activation.viewers;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Graphics;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import javax.activation.CommandObject;
import javax.activation.DataHandler;

/**
 * Simple image display component.
 *
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
 * @version 1.0.2
 */
public class ImageViewer extends Component implements CommandObject {

    private Image image;

    /**
   * Returns the preferred size for this component (the image size).
   */
    public Dimension getPreferredSize() {
        Dimension ps = new Dimension(0, 0);
        if (image != null) {
            ps.width = image.getWidth(this);
            ps.height = image.getHeight(this);
        }
        return ps;
    }

    public void setCommandContext(String verb, DataHandler dh) throws IOException {
        InputStream in = dh.getInputStream();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        for (int len = in.read(buf); len != -1; len = in.read(buf)) bytes.write(buf, 0, len);
        in.close();
        Toolkit toolkit = getToolkit();
        Image img = toolkit.createImage(bytes.toByteArray());
        try {
            MediaTracker tracker = new MediaTracker(this);
            tracker.addImage(img, 0);
            tracker.waitForID(0);
        } catch (InterruptedException e) {
        }
        toolkit.prepareImage(img, -1, -1, this);
    }

    /**
   * Image bits arrive.
   */
    public boolean imageUpdate(Image image, int flags, int x, int y, int width, int height) {
        if ((flags & ALLBITS) != 0) {
            this.image = image;
            invalidate();
            repaint();
            return false;
        }
        return ((flags & ERROR) == 0);
    }

    /**
   * Scale the image into this component's bounds.
   */
    public void paint(Graphics g) {
        if (image != null) {
            Dimension is = new Dimension(image.getWidth(this), image.getHeight(this));
            if (is.width > -1 && is.height > -1) {
                Dimension cs = getSize();
                g.drawImage(image, 0, 0, cs.width, cs.height, 0, 0, is.width, is.height, this);
            }
        }
    }
}
