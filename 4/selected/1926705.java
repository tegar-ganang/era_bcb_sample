package com.jogamp.opengl.test.junit.jogl.offscreen;

import javax.media.opengl.*;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.texture.TextureIO;
import java.io.File;
import java.io.IOException;
import javax.media.nativewindow.*;

public class Surface2File implements SurfaceUpdatedListener {

    GLReadBufferUtil readBufferUtil = new GLReadBufferUtil(false, false);

    int shotNum = 0;

    public void dispose(GL gl) {
        readBufferUtil.dispose(gl);
    }

    public void surfaceUpdated(Object updater, NativeSurface ns, long when) {
        if (updater instanceof GLDrawable) {
            GLDrawable drawable = (GLDrawable) updater;
            GLContext ctx = GLContext.getCurrent();
            if (null != ctx && ctx.getGLDrawable() == drawable) {
                GL gl = ctx.getGL();
                gl.glFinish();
                readBufferUtil.readPixels(gl, drawable, false);
                gl.glFinish();
                try {
                    surface2File("shot");
                } catch (IOException ex) {
                    throw new RuntimeException("can not write survace to file", ex);
                }
            }
        }
    }

    public void surface2File(String basename) throws IOException {
        if (!readBufferUtil.isValid()) {
            return;
        }
        File file = File.createTempFile(basename + shotNum + "-", ".ppm");
        readBufferUtil.write(file);
        System.err.println("Wrote: " + file.getAbsolutePath() + ", ...");
        shotNum++;
    }
}
