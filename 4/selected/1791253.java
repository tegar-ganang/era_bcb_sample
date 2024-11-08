package com.jogamp.opengl.test.junit.jogl.offscreen;

import javax.media.opengl.*;
import com.jogamp.opengl.util.GLReadBufferUtil;

public class ReadBufferBase implements GLEventListener {

    public boolean glDebug = false;

    public boolean glTrace = false;

    protected GLDrawable externalRead;

    GLReadBufferUtil readBufferUtil;

    public ReadBufferBase(GLDrawable externalRead, boolean write2Texture) {
        this.externalRead = externalRead;
        this.readBufferUtil = new GLReadBufferUtil(false, write2Texture);
    }

    public void init(GLAutoDrawable drawable) {
        GL _gl = drawable.getGL();
        _gl.glGetError();
        if (glDebug) {
            try {
                _gl = _gl.getContext().setGL(GLPipelineFactory.create("javax.media.opengl.Debug", null, _gl, null));
            } catch (Exception e) {
                throw new RuntimeException("can not set debug pipeline", e);
            }
        }
        if (glTrace) {
            try {
                _gl = _gl.getContext().setGL(GLPipelineFactory.create("javax.media.opengl.Trace", null, _gl, new Object[] { System.err }));
            } catch (Exception e) {
                throw new RuntimeException("can not set trace pipeline", e);
            }
        }
        System.out.println(_gl);
        _gl.getContext().setGLReadDrawable(externalRead);
        if (_gl.isGL2GL3()) {
            _gl.getGL2GL3().glReadBuffer(GL2GL3.GL_FRONT);
        }
        System.out.println("---------------------------");
        System.out.println(_gl.getContext());
        System.out.println("---------------------------");
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    }

    public void dispose(GLAutoDrawable drawable) {
        readBufferUtil.dispose(drawable.getGL());
    }

    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();
        readBufferUtil.readPixels(gl, drawable, false);
    }
}
