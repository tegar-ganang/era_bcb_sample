package org.fenggui.actor;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.fenggui.Display;
import org.fenggui.IWidget;
import org.fenggui.binding.render.IOpenGL;
import org.fenggui.event.Event;
import org.fenggui.event.IEventListener;
import org.fenggui.event.key.Key;
import org.fenggui.event.key.KeyReleasedEvent;

/**
 * This actor can be assigned to a Display and will save a screenshot if the F12
 * key is hit. You have to call the renderToDos(..) method after the
 * yourDisplay.display() method call so the actor is able to save a complete screenshot.
 * 
 * @author marcmenghin, last edited by $Author$, $Date$
 * @version $Revision$
 */
public class ScreenshotActor implements IActor {

    private static final int TARGA_HEADER_SIZE = 18;

    private File screenshotFile = null;

    private IEventListener keylistener = new IEventListener() {

        public void processEvent(Event event) {
            if (event instanceof KeyReleasedEvent) if (((KeyReleasedEvent) event).getKeyClass() == Key.F12) {
                takeScreenshot(new File(System.currentTimeMillis() + "_screenshot.tga"));
            }
        }
    };

    /**
   * 
   */
    public ScreenshotActor() {
    }

    public void hook(IWidget widget) {
        if (widget instanceof Display) ((Display) widget).addGlobalEventListener(keylistener);
    }

    public void unHook(IWidget widget) {
        if (widget instanceof Display) ((Display) widget).removeGlobalEventListener(keylistener);
    }

    /**
   * Takes a screenshot and writes it in the given file.
   * 
   * @param screenshotFile
   *          the file to store the screenshot
   */
    private void takeScreenshot(File screenshotFile) {
        this.screenshotFile = screenshotFile;
    }

    /**
   * This method should be called after the Display's display() Method. This creates the
   * screenshot.
   * 
   * @param opengl
   * @param width
   * @param height
   */
    public void renderToDos(IOpenGL opengl, int width, int height) {
        if (screenshotFile != null) {
            screenshot(opengl, width, height, screenshotFile);
            screenshotFile = null;
        }
    }

    /**
   * Takes a screenshot of the current frame. This method is entirely copied from
   * http://www.javagaming.org/forums/index.php?topic=8747.0
   * 
   * @param gl
   *          FengGUIs opengl interface
   * @param width
   *          the width of the screenshot
   * @param height
   *          the height of the screenhost
   * @param file
   *          the file where to store the screenshot
   */
    private static void screenshot(IOpenGL gl, int width, int height, File file) {
        try {
            RandomAccessFile out = new RandomAccessFile(file, "rw");
            FileChannel ch = out.getChannel();
            int fileLength = TARGA_HEADER_SIZE + width * height * 3;
            out.setLength(fileLength);
            MappedByteBuffer image = ch.map(FileChannel.MapMode.READ_WRITE, 0, fileLength);
            image.put(0, (byte) 0).put(1, (byte) 0);
            image.put(2, (byte) 2);
            image.put(12, (byte) (width & 0xFF));
            image.put(13, (byte) (width >> 8));
            image.put(14, (byte) (height & 0xFF));
            image.put(15, (byte) (height >> 8));
            image.put(16, (byte) 24);
            image.position(TARGA_HEADER_SIZE);
            ByteBuffer bgr = image.slice();
            gl.readPixels(0, 0, width, height, bgr);
            ch.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
