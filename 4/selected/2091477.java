package com.rugl;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.Dimension;
import com.rugl.console.Console;
import com.rugl.util.TileRenderer;

/**
 * Makes screengrabs
 * 
 * @author ryanm
 */
public class ScreenGrabber {

    private static final int TARGA_HEADER_SIZE = 18;

    private static int shots = 0;

    private static DecimalFormat dcf = new DecimalFormat("000");

    /**
	 * Indicates that tiled rendering is taking place, and so the
	 * projection matrix stack should not be touched
	 */
    public static boolean doingTiledRender = false;

    /**
	 * The active tiled rendering context
	 */
    public static TileRenderer tr = null;

    /**
	 * Takes a screenshot, saved in the current directory as
	 * "image[count].tga"
	 */
    public static void screenshot() {
        if (!GameBox.secureEnvironment) {
            File f = new File("image" + dcf.format(shots++) + ".tga");
            screenshot(f);
        } else {
            Console.error("Cannot take screenshots in sandbox");
        }
    }

    /**
	 * Takes a screenshot, saved in the current directory as
	 * image[count].tga
	 * 
	 * @param size
	 *           The desired size of the screenshot
	 */
    public static void screenshot(Dimension size) {
        if (!GameBox.secureEnvironment) {
            File f = new File("image" + dcf.format(shots++) + ".tga");
            screenshot(f, size.getWidth(), size.getHeight());
        } else {
            Console.error("Cannot take screenshots in sandbox");
        }
    }

    /**
	 * Takes a screenshot in uncompressed targa format
	 * 
	 * @param file
	 *           The file to save the image in
	 */
    public static void screenshot(File file) {
        if (GLContext.getCapabilities().OpenGL12) {
            int w = Display.getDisplayMode().getWidth();
            int h = Display.getDisplayMode().getHeight();
            GameBox.game.draw();
            try {
                RandomAccessFile out = new RandomAccessFile(file, "rw");
                FileChannel ch = out.getChannel();
                int fileLength = TARGA_HEADER_SIZE + w * h * 3;
                out.setLength(fileLength);
                MappedByteBuffer image = ch.map(FileChannel.MapMode.READ_WRITE, 0, fileLength);
                image.put(0, (byte) 0).put(1, (byte) 0);
                image.put(2, (byte) 2);
                image.put(12, (byte) (w & 0xFF));
                image.put(13, (byte) (w >> 8));
                image.put(14, (byte) (h & 0xFF));
                image.put(15, (byte) (h >> 8));
                image.put(16, (byte) 24);
                image.position(TARGA_HEADER_SIZE);
                ByteBuffer bgr = image.slice();
                GL11.glReadPixels(0, 0, w, h, GL12.GL_BGR, GL11.GL_UNSIGNED_BYTE, bgr);
                ch.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Console.error("Cannot make screenshot, requires OpenGL 1.2");
        }
    }

    /**
	 * Takes a screen capture at an arbitrary resolution
	 * 
	 * @param file
	 *           The file to save to
	 * @param width
	 *           The desired width of the capture
	 * @param height
	 *           The desired height of the capture
	 */
    public static void screenshot(File file, int width, int height) {
        if (GLContext.getCapabilities().OpenGL12) {
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
                tr = new TileRenderer();
                tr.setTileSize(Display.getDisplayMode().getWidth(), Display.getDisplayMode().getHeight(), 0);
                tr.setImageSize(width, height);
                tr.setImageBuffer(GL12.GL_BGR, GL11.GL_UNSIGNED_BYTE, bgr);
                GameBox.game.draw();
                doingTiledRender = true;
                do {
                    tr.beginTile();
                    GameBox.game.draw();
                } while (tr.endTile());
                ch.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            doingTiledRender = false;
            tr = null;
        } else {
            Console.error("Cannot make screenshot, requires OpenGL 1.2");
        }
    }
}
