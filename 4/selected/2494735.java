package javacream.scene.sprite;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javacream.io.GenericInputStream;
import javacream.resource.ImageResourceHandler;
import javacream.resource.ResourceException;
import javacream.resource.ResourceHandler;
import javacream.resource.ResourceManager;
import javacream.resource.XMLResourceHandler;
import javacream.util.TreeIterator;
import javacream.xml.XML;
import javacream.xml.XMLParser;

/**
 * SpriteResourceHandler
 *
 * @author Glenn Powell
 *
 */
public class SpriteResourceHandler implements ResourceHandler<Sprite> {

    private static final Logger logger = Logger.getLogger(SpriteResourceHandler.class.getName());

    public static final String DEFAULT_IMAGE_FORMAT = "png";

    public Sprite read(InputStream input) throws ResourceException {
        try {
            GenericInputStream data = new GenericInputStream(input);
            byte[] xmlBytes = data.readObject(byte[].class);
            byte[] imageBytes = data.readObject(byte[].class);
            ByteArrayInputStream xmlInput = new ByteArrayInputStream(xmlBytes);
            ByteArrayInputStream imageInput = new ByteArrayInputStream(imageBytes);
            return readSprite(xmlInput, imageInput);
        } catch (IOException e) {
            throw new ResourceException(e);
        } catch (ClassNotFoundException e) {
            throw new ResourceException(e);
        } catch (ClassCastException e) {
            throw new ResourceException(e);
        }
    }

    public static Sprite readSprite(InputStream xmlInput, InputStream imageInput) throws ResourceException {
        try {
            XML xml = ResourceManager.read(XML.class, new XMLResourceHandler(), xmlInput);
            if (xml != null) return readSprite(xml, imageInput);
        } catch (ResourceException e) {
            logger.log(Level.SEVERE, "Failed to import Sprite", e);
        }
        return null;
    }

    public static Sprite readSprite(XML xml, InputStream imageInput) throws ResourceException {
        try {
            BufferedImage image = ResourceManager.read(BufferedImage.class, new ImageResourceHandler(), imageInput);
            return readSprite(xml, image);
        } catch (ResourceException e) {
            logger.log(Level.SEVERE, "Failed to import Sprite", e);
        }
        return null;
    }

    public static Sprite readSprite(InputStream xmlInput, BufferedImage image) throws ResourceException {
        try {
            XML xml = ResourceManager.read(XML.class, new XMLResourceHandler(), xmlInput);
            if (xml != null) return readSprite(xml, image);
        } catch (ResourceException e) {
            logger.log(Level.SEVERE, "Failed to import Sprite", e);
        }
        return null;
    }

    public static Sprite readSprite(XML xml, BufferedImage image) throws ResourceException {
        Sprite sprite = new Sprite();
        if (xml.isName("sprite")) {
            for (TreeIterator<XML> itr = xml.childIterator(); itr.hasNext(); ) {
                XML child = itr.next();
                if (child.isName("animation")) {
                    SpriteAnimation animation = new SpriteAnimation();
                    String animationName = child.findAttributeString("name", null);
                    if (animationName != null) animation.setName(animationName);
                    String animationRepeat = child.findAttributeString("repeat", null);
                    if (animationRepeat != null) animation.setRepeat(Integer.parseInt(animationRepeat));
                    animation.setLastFrameTime(child.findFloatAttribute("lastFrameTime", 0));
                    sprite.addAnimation(animation);
                    for (TreeIterator<XML> itr2 = child.childIterator(); itr2.hasNext(); ) {
                        XML child2 = itr2.next();
                        if (child2.isName("frame")) {
                            SpriteFrame frame = new SpriteFrame();
                            int boundsX = child2.findIntegerAttribute("x", 0);
                            int boundsY = child2.findIntegerAttribute("y", 0);
                            int boundsWidth = child2.findIntegerAttribute("width", 0);
                            int boundsHeight = child2.findIntegerAttribute("height", 0);
                            int anchorX = child2.findIntegerAttribute("anchorX", 0);
                            int anchorY = child2.findIntegerAttribute("anchorY", 0);
                            float time = child2.findFloatAttribute("time", 0);
                            frame.setBounds(boundsX, boundsY, boundsWidth, boundsHeight);
                            frame.setAnchor(anchorX, anchorY);
                            frame.setTime(time);
                            animation.addFrame(frame);
                        }
                    }
                }
            }
        }
        sprite.setImage(image);
        return sprite;
    }

    public void write(Sprite resource, OutputStream output) throws ResourceException {
        writeSprite(output, resource, null, null, null);
    }

    public static void writeSprite(OutputStream output, InputStream xmlInput, InputStream imageInput, String imageFormat) throws ResourceException {
        try {
            ObjectOutputStream data = new ObjectOutputStream(output);
            ByteArrayOutputStream xmlByteOutput = new ByteArrayOutputStream();
            while (xmlInput.available() > 0) xmlByteOutput.write(xmlInput.read());
            data.writeObject(xmlByteOutput.toByteArray());
            ByteArrayOutputStream imageByteOutput = new ByteArrayOutputStream();
            while (imageInput.available() > 0) imageByteOutput.write(imageInput.read());
            data.writeObject(imageByteOutput.toByteArray());
        } catch (IOException e) {
            throw new ResourceException(e);
        }
    }

    public static void writeSprite(OutputStream output, InputStream xmlInput, BufferedImage image) throws ResourceException {
        writeSprite(output, xmlInput, image, null);
    }

    public static void writeSprite(OutputStream output, InputStream xmlInput, BufferedImage image, String imageFormat) throws ResourceException {
        ByteArrayOutputStream imageByteOutput = new ByteArrayOutputStream();
        exportImage(imageByteOutput, image, imageFormat);
        InputStream imageInput = new ByteArrayInputStream(imageByteOutput.toByteArray());
        writeSprite(output, xmlInput, imageInput, imageFormat);
    }

    public static void writeSprite(OutputStream output, XML xml, BufferedImage image, String imageFormat) throws ResourceException {
        ByteArrayOutputStream imageByteOutput = new ByteArrayOutputStream();
        exportImage(imageByteOutput, image, imageFormat);
        InputStream imageInput = new ByteArrayInputStream(imageByteOutput.toByteArray());
        writeSprite(output, xml, imageInput, imageFormat);
    }

    public static void writeSprite(OutputStream output, XML xml, InputStream imageInput, String imageFormat) throws ResourceException {
        ByteArrayOutputStream xmlOutput = new ByteArrayOutputStream();
        XMLParser.write(xmlOutput, xml);
        ByteArrayInputStream xmlInput = new ByteArrayInputStream(xmlOutput.toByteArray());
        writeSprite(output, xmlInput, imageInput, imageFormat);
    }

    public static void writeSprite(OutputStream output, Sprite sprite, OutputStream xmlOutput, OutputStream imageOutput, String imageFormat) throws ResourceException {
        ByteArrayOutputStream xmlByteOutput = new ByteArrayOutputStream();
        ByteArrayOutputStream imageByteOutput = new ByteArrayOutputStream();
        exportXML(xmlByteOutput, sprite);
        exportImage(imageByteOutput, sprite, imageFormat);
        byte[] xmlBytes = xmlByteOutput.toByteArray();
        byte[] imageBytes = imageByteOutput.toByteArray();
        try {
            if (xmlOutput != null) xmlOutput.write(xmlBytes);
            if (imageOutput != null) imageOutput.write(imageBytes);
            InputStream xmlInput = new ByteArrayInputStream(xmlBytes);
            InputStream imageInput = new ByteArrayInputStream(imageBytes);
            writeSprite(output, xmlInput, imageInput, imageFormat);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to export Sprite", e);
        }
    }

    public static void exportXML(OutputStream output, Sprite sprite) throws ResourceException {
        XML xml = extractXML(sprite);
        if (xml != null) XMLParser.write(output, xml); else throw new ResourceException("Failed to export Sprite XML");
    }

    public static XML extractXML(Sprite sprite) throws ResourceException {
        XML xmlSprite = new XML("sprite");
        for (Iterator<SpriteAnimation> itr = sprite.getAnimations(); itr.hasNext(); ) {
            SpriteAnimation animation = itr.next();
            XML xmlAnimation = new XML(xmlSprite, "animation");
            if (animation.getName() != null) xmlAnimation.setAttribute(null, null, "name", null, animation.getName());
            xmlAnimation.setAttribute(null, null, "repeat", null, String.valueOf(animation.getRepeat()));
            xmlAnimation.setAttribute(null, null, "lastFrameTime", null, String.valueOf(animation.getLastFrameTime()));
            for (Iterator<SpriteFrame> itr2 = animation.getFrames(); itr2.hasNext(); ) {
                SpriteFrame frame = itr2.next();
                XML xmlFrame = new XML(xmlAnimation, "frame");
                xmlFrame.setAttribute(null, null, "x", null, String.valueOf(frame.getBounds().x));
                xmlFrame.setAttribute(null, null, "y", null, String.valueOf(frame.getBounds().y));
                xmlFrame.setAttribute(null, null, "width", null, String.valueOf(frame.getBounds().width));
                xmlFrame.setAttribute(null, null, "height", null, String.valueOf(frame.getBounds().height));
                xmlFrame.setAttribute(null, null, "anchorX", null, String.valueOf(frame.getAnchor().x));
                xmlFrame.setAttribute(null, null, "anchorY", null, String.valueOf(frame.getAnchor().y));
                xmlFrame.setAttribute(null, null, "time", null, String.valueOf(frame.getTime()));
            }
        }
        return xmlSprite;
    }

    public static void exportImage(OutputStream output, Sprite sprite) throws ResourceException {
        exportImage(output, sprite, null);
    }

    public static void exportImage(OutputStream output, Sprite sprite, String imageFormat) throws ResourceException {
        exportImage(output, sprite.getImage(), imageFormat);
    }

    public static void exportImage(OutputStream output, BufferedImage image, String imageFormat) throws ResourceException {
        if (imageFormat == null) imageFormat = DEFAULT_IMAGE_FORMAT;
        try {
            if (!ImageIO.write(image, imageFormat, output)) throw new ResourceException("Failed to export Sprite Image");
        } catch (IOException e) {
            throw new ResourceException(e);
        }
    }
}
