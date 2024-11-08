package mikera.tyrant;

import java.awt.AWTEventMulticaster;
import java.awt.AWTException;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Chris Grindstaff chris@gstaff.org
 */
public class ImageGadget extends Canvas {

    private final class ForwardingMouseListener implements MouseListener {

        public void mouseClicked(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            e.consume();
            if (actionListener == null) return;
            actionListener.actionPerformed(new ActionEvent(this, -100, null));
        }

        public void mouseReleased(MouseEvent e) {
        }
    }

    private static final long serialVersionUID = 1L;

    protected Image image;

    private boolean noImage = false;

    private int imageWidth = 0;

    protected Image backgroundImage;

    protected String resourceName;

    protected ActionListener actionListener;

    protected boolean loadLater;

    protected static Map images = new HashMap();

    protected boolean loadFailed;

    private String text;

    public ImageGadget() {
        addMouseListener(new ForwardingMouseListener());
    }

    public ImageGadget(String resourceName) {
        this(resourceName, null);
    }

    public ImageGadget(String resourceName, String text) {
        this();
        this.resourceName = resourceName;
        this.text = text;
    }

    public static ImageGadget noImage(String text) {
        ImageGadget me = new ImageGadget();
        me.text = text;
        me.noImage = true;
        return me;
    }

    public void addActionListener(ActionListener listener) {
        setActionListener(AWTEventMulticaster.add(listener, actionListener));
    }

    public static Map getImages() {
        return images;
    }

    public Dimension getMinimumSize() {
        int textWidth = 0;
        int textHeight = 0;
        if (text != null) {
            FontMetrics fontMetrics = getFontMetrics(getFont());
            textWidth = fontMetrics.stringWidth(text);
            textHeight = fontMetrics.getHeight();
        }
        if (noImage) {
            return new Dimension(textWidth, textHeight);
        }
        if (image == null && !loadLater) {
            lookupImage();
        }
        if (image == null) {
            return new Dimension(16 + 2 + textWidth, 16);
        }
        return new Dimension(image.getWidth(null) + 2 + textWidth, image.getHeight(null));
    }

    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    protected static synchronized Image imageFor(ImageGadget imageGadget) throws AWTException {
        String resourceName = imageGadget.resourceName;
        if (resourceName == null) return null;
        Image image = (Image) getImages().get(resourceName);
        if (image == null) {
            imageGadget.loadImage();
            getImages().put(resourceName, imageGadget.image);
            image = imageGadget.image;
        } else {
        }
        return image;
    }

    protected void loadImage() throws AWTException {
        Image theImage = null;
        InputStream imageInputStream = getClass().getResourceAsStream(resourceName);
        if (imageInputStream == null) {
            loadFailed = true;
            throw new AWTException("Image resource " + resourceName + " not found.");
        }
        ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
        int arraySize = 1024;
        byte[] imageArray = new byte[arraySize];
        int readCode;
        if (imageInputStream == null) return;
        try {
            while ((readCode = imageInputStream.read(imageArray, 0, arraySize)) > -1) {
                imageBytes.write(imageArray, 0, readCode);
            }
            imageBytes.close();
            imageArray = imageBytes.toByteArray();
            imageInputStream.close();
            theImage = Toolkit.getDefaultToolkit().createImage(imageArray);
        } catch (IOException ioException) {
            System.out.println("Error reading image data");
            ioException.printStackTrace();
        }
        setImage(theImage);
        if (loadLater) return;
        waitForImage();
    }

    protected void lookupImage() {
        try {
            if (loadFailed) return;
            Image alreadLoaded = imageFor(this);
            setImage(alreadLoaded);
        } catch (AWTException awtE) {
            loadFailed = true;
        }
    }

    public void paint(Graphics g) {
        Dimension mySize = getSize();
        int w = mySize.width;
        int h = mySize.height;
        if (!noImage && resourceName == null || loadFailed) {
            g.setColor(Color.red);
            g.drawLine(0, 0, w, h);
            g.drawLine(w, 0, 0, h);
            if (text != null) g.drawString(text, 16 + 2, h / 2);
            return;
        }
        if (!noImage && image == null) {
            lookupImage();
        }
        if (backgroundImage != null) {
            paintBackground(g, w, h);
        }
        if (image != null) g.drawImage(image, 0, (h - image.getHeight(null)) / 2, this);
        if (text != null) {
            g.setColor(QuestApp.INFOTEXTCOLOUR);
            int ascent = (getSize().height + g.getFontMetrics().getAscent()) / 2;
            g.drawString(text, image == null ? 0 : imageWidth + 2, ascent - 2);
        }
    }

    private void paintBackground(Graphics g, int myWidth, int myHeight) {
        int backgroundWidth = backgroundImage.getWidth(null);
        for (int lx = 0; lx < myWidth; lx += backgroundWidth) {
            for (int ly = 0; ly < myHeight; ly += myHeight) {
                g.drawImage(backgroundImage, lx, ly, null);
            }
        }
    }

    public void removeActionListener(ActionListener listener) {
        setActionListener(AWTEventMulticaster.remove(listener, actionListener));
    }

    public void setActionListener(java.awt.event.ActionListener newActionListener) {
        actionListener = newActionListener;
    }

    public void setImage(java.awt.Image newImage) {
        image = newImage;
        imageWidth = newImage.getWidth(null);
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setNoImage(boolean noImage) {
        this.noImage = noImage;
    }

    public static void setImages(java.util.Hashtable newImages) {
        images = newImages;
    }

    public void waitForImage() {
        MediaTracker mediatracker = new MediaTracker(this);
        mediatracker.addImage(image, 1456);
        try {
            mediatracker.waitForID(1456);
        } catch (InterruptedException ioe) {
            System.out.println("Error reading image data");
            ioe.printStackTrace();
        }
    }

    public void setBackgroundImage(Image backgroundImage) {
        this.backgroundImage = backgroundImage;
    }
}
