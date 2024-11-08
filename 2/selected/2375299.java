package org.earth.gl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

public class Texture {

    public static int load(Context context, URL url) throws Exception {
        int texture[] = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        int textureId = texture[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        InputStream is = url.openStream();
        Bitmap tmpBmp;
        try {
            tmpBmp = BitmapFactory.decodeStream(is);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
            }
        }
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_NEAREST);
        MyGLUtils.checkGlError("glTexParameterf GL_TEXTURE_MIN_FILTER");
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        MyGLUtils.checkGlError("glTexParameterf GL_TEXTURE_MAG_FILTER");
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, tmpBmp, 0);
        MyGLUtils.checkGlError("texImage2D");
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        MyGLUtils.checkGlError("glGenerateMipmap");
        tmpBmp.recycle();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return textureId;
    }
}
