package com.jogamp.opengl.test.junit.jogl.offscreen;

import java.io.IOException;
import javax.media.opengl.*;
import com.jogamp.opengl.util.texture.TextureIO;
import java.io.File;

public class ReadBuffer2File extends ReadBufferBase {

    public ReadBuffer2File(GLDrawable externalRead) {
        super(externalRead, false);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        super.dispose(drawable);
    }

    int shotNum = 0;

    void copyTextureData2File() throws IOException {
        if (!readBufferUtil.isValid()) {
            return;
        }
        File file = File.createTempFile("shot" + shotNum + "-", ".ppm");
        readBufferUtil.write(file);
        System.out.println("Wrote: " + file.getAbsolutePath() + ", ...");
        shotNum++;
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        super.display(drawable);
        try {
            copyTextureData2File();
        } catch (IOException ex) {
            throw new RuntimeException("can not read buffer to file", ex);
        }
    }
}
