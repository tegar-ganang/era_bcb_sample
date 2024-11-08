package net.sourceforge.ephemera.client;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;

public class Helper {

    /**
	 * Helper Functions
	 */
    public void banner(String msg) {
        System.out.println("=== " + msg + " ===");
    }

    public void warning(String msg) {
        System.out.println("!!! " + msg + ".");
    }

    public void info(String msg) {
        System.out.println(msg + ".");
    }

    public void error(String msg) {
        System.out.println("*** " + msg + ".");
        System.exit(1);
    }

    /**
	 * Allocates a float array
	 * 
	 * @param floatarray
	 * @return
	 */
    public static FloatBuffer allocFloats(float[] floatarray) {
        FloatBuffer fb = BufferUtils.createFloatBuffer(floatarray.length);
        fb.put(floatarray).flip();
        return fb;
    }

    /**
	 * Helper function from www.potatoland.com
	 * 
	 * @param filename
	 * @return
	 */
    public ByteBuffer getData(String filename) {
        ByteBuffer buffer = null;
        try {
            BufferedInputStream bis = new BufferedInputStream(this.getClass().getClassLoader().getResourceAsStream(filename));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int bufferLength = 4096;
            byte[] readBuffer = new byte[bufferLength];
            int read = -1;
            while ((read = bis.read(readBuffer, 0, bufferLength)) != -1) {
                baos.write(readBuffer, 0, read);
            }
            bis.close();
            buffer = ByteBuffer.allocateDirect(baos.size());
            buffer.order(ByteOrder.nativeOrder());
            buffer.put(baos.toByteArray());
            buffer.rewind();
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }
        return buffer;
    }
}
