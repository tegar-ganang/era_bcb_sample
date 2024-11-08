package de.cabanis.unific.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import org.apache.log4j.Logger;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * TODO javadoc
 * @author Nicolas Cabanis
 */
public class Resources {

    private static Logger logger = Logger.getLogger(Resources.class);

    public static final Color COL_LIGHT_RED = new Color(Display.getCurrent(), 255, 102, 102);

    public static final Color COL_LIGHT_ORANGE = new Color(Display.getCurrent(), 255, 204, 51);

    public static final Color COL_LIGHT_GREEN = new Color(Display.getCurrent(), 153, 204, 102);

    public static final Color COL_BLACK = new Color(Display.getCurrent(), 0, 0, 0);

    public static final Color COL_WHITE = new Color(Display.getCurrent(), 255, 255, 255);

    public static String IMG_CHECKBOX_CHECKED = "small/cb_checked.jpg";

    public static String IMG_CHECKBOX_HALFCHECKED = "small/cb_halfchecked.jpg";

    public static String IMG_CHECKBOX_UNCHECKED = "small/cb_unchecked.jpg";

    public static String IMG_LED_GREEN = "small/led_green.png";

    public static String IMG_LED_YELLOW = "small/led_yellow.png";

    public static String IMG_LED_RED = "small/led_red.png";

    public static String IMG_INFO_64 = "64x64/info.png";

    public static String IMG_HELP_64 = "64x64/khelpcenter.png";

    public static String IMG_WIZARD_64 = "64x64/ktip.png";

    private static final String[] allImages = new String[] { IMG_CHECKBOX_CHECKED, IMG_CHECKBOX_HALFCHECKED, IMG_CHECKBOX_UNCHECKED, IMG_LED_GREEN, IMG_LED_YELLOW, IMG_LED_RED, IMG_INFO_64, IMG_HELP_64, IMG_WIZARD_64 };

    private static ImageRegistry imageRegistry = new ImageRegistry();

    static {
        URL url = null;
        for (String image : allImages) {
            url = Resources.class.getClassLoader().getResource("icons/" + image);
            System.out.println("image: " + image + "\t url: " + url);
            imageRegistry.put(image, ImageDescriptor.createFromURL(url));
        }
    }

    public static Image getImage(String key) {
        return imageRegistry.get(key);
    }

    public static void copyFile(File source, File target) throws Exception {
        if (source == null || target == null) {
            throw new IllegalArgumentException("The arguments may not be null.");
        }
        try {
            FileChannel srcChannel = new FileInputStream(source).getChannel();
            FileChannel dtnChannel = new FileOutputStream(target).getChannel();
            srcChannel.transferTo(0, srcChannel.size(), dtnChannel);
            srcChannel.close();
            dtnChannel.close();
        } catch (Exception e) {
            String message = "Unable to copy file '" + source.getName() + "' to '" + target.getName() + "'.";
            logger.error(message, e);
            throw new Exception(message, e);
        }
    }

    public static File loadFromClassPath(String resource) throws Exception {
        try {
            URL url = Resources.class.getClassLoader().getResource(resource);
            if (url == null) {
                String message = "The resource '" + resource + "' could " + "not be found within this class loader.";
                logger.error(message);
                throw new Exception(message);
            }
            return new File(url.toURI());
        } catch (URISyntaxException e) {
            String message = "Unable to load the resource '" + resource + "' with this class loader.";
            logger.error(message, e);
            throw new Exception(message, e);
        }
    }
}
