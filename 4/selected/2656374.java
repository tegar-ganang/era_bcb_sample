package gestalt.impl.jogl.render.plugin;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import javax.media.opengl.GL;
import gestalt.Gestalt;
import gestalt.context.GLContext;
import gestalt.impl.jogl.context.JoglGLContext;
import gestalt.render.plugin.FrameGrabber;
import gestalt.texture.bitmap.ByteBitmap;
import gestalt.util.ImageUtil;

public class JoglFrameGrabber extends FrameGrabber {

    private boolean _myCaptureSingleFrame = false;

    public void draw(final GLContext theRenderContext) {
        final GL gl = ((JoglGLContext) theRenderContext).gl;
        gl.glFlush();
        if (_myImageFileFormat == Gestalt.IMAGE_FILEFORMAT_TGA) {
            grabFrame(gl, region_offset().x, region_offset().y, region_scale().x == 0 ? theRenderContext.displaycapabilities.width : region_scale().x, region_scale().y == 0 ? theRenderContext.displaycapabilities.height : region_scale().y, _myFileName + _myFrameCounter + ".tga");
        } else {
            final ByteBitmap myBitmap = grabFrame(gl, region_offset().x, region_offset().y, region_scale().x == 0 ? theRenderContext.displaycapabilities.width : region_scale().x, region_scale().y == 0 ? theRenderContext.displaycapabilities.height : region_scale().y);
            ImageUtil.save(ImageUtil.flip(ImageUtil.convertByteBitmap2BufferedImageBGR(myBitmap), ImageUtil.VERTICAL), _myFileName + (_myCaptureSingleFrame ? "" : werkzeug.Util.formatNumber(_myFrameCounter, _myDigits)), _myImageFileFormat);
        }
        if (_myCaptureSingleFrame) {
            _myCaptureSingleFrame = false;
            _myIsActive = false;
        }
        _myFrameCounter++;
    }

    private ByteBitmap grabFrame(GL gl, int x, int y, int width, int height) {
        final byte[] myData = new byte[width * height * ByteBitmap.NUMBER_OF_PIXEL_COMPONENTS];
        ByteBuffer myBuffer = ByteBuffer.wrap(myData);
        gl.glReadPixels(x, y, width, height, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, myBuffer);
        ByteBitmap myBitmap = new ByteBitmap(myData, width, height, Gestalt.BITMAP_COMPONENT_ORDER_RGBA);
        return myBitmap;
    }

    public static final int TARGA_HEADER_SIZE = 18;

    private void grabFrame(GL gl, int x, int y, int width, int height, String theFileName) {
        try {
            try {
                File myParent = new File(theFileName).getParentFile();
                if (!myParent.exists()) {
                    myParent.mkdirs();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            RandomAccessFile out = new RandomAccessFile(new File(theFileName), "rw");
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
            gl.glReadPixels(x, y, width, height, GL.GL_BGR, GL.GL_UNSIGNED_BYTE, bgr);
            ch.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void grabSingleFrame() {
        _myCaptureSingleFrame = true;
        _myIsActive = true;
    }
}
