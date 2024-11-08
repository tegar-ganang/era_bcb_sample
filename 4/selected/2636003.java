package eu.cherrytree.paj.graphics;

import javax.media.opengl.GL2;

public class Framebuffer {

    public class FramebufferNotCreatedException extends Exception {

        private static final long serialVersionUID = -865793093857220481L;

        public FramebufferNotCreatedException(String msg) {
            super("Couldn't create the framebuffer. " + msg);
        }
    }

    private int framebufferId;

    private int renderbufferId;

    private int[] textureId;

    private int width;

    private int height;

    private boolean mipmaps = false;

    public Framebuffer() throws FramebufferNotCreatedException {
        this(1);
    }

    public Framebuffer(int numberOfTargets) throws FramebufferNotCreatedException {
        this(Graphics.getWidth(), Graphics.getHeight(), numberOfTargets);
    }

    public Framebuffer(int width, int height) throws FramebufferNotCreatedException {
        this(width, height, 1, false, false);
    }

    public Framebuffer(int width, int height, int numberOfTargets) throws FramebufferNotCreatedException {
        this(width, height, numberOfTargets, false, false);
    }

    public Framebuffer(int width, int height, int numberOfTargets, boolean mipmaps, boolean linearFilter) throws FramebufferNotCreatedException {
        GL2 gl = Graphics.getGL();
        this.width = width;
        this.height = height;
        this.mipmaps = mipmaps;
        if (numberOfTargets > getMaxNumberOfTargets()) throw new FramebufferNotCreatedException("The system does not support so much (" + numberOfTargets + ") frambuffer targets");
        int[] tmp = new int[1];
        textureId = new int[numberOfTargets];
        for (int i = 0; i < textureId.length; i++) {
            gl.glGenTextures(1, tmp, 0);
            textureId[i] = tmp[0];
            gl.glBindTexture(GL2.GL_TEXTURE_2D, textureId[i]);
            if (linearFilter) {
                gl.glTexParameterf(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
                if (mipmaps) gl.glTexParameterf(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_LINEAR); else gl.glTexParameterf(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
            } else {
                gl.glTexParameterf(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
                if (mipmaps) gl.glTexParameterf(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST_MIPMAP_NEAREST); else gl.glTexParameterf(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
            }
            gl.glTexParameterf(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
            gl.glTexParameterf(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
            gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA, width, height, 0, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, null);
        }
        gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
        gl.glGenRenderbuffers(1, tmp, 0);
        renderbufferId = tmp[0];
        gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, renderbufferId);
        gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_DEPTH_COMPONENT, width, height);
        gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, 0);
        gl.glGenFramebuffers(1, tmp, 0);
        framebufferId = tmp[0];
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, framebufferId);
        for (int i = 0; i < textureId.length; i++) gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0 + i, GL2.GL_TEXTURE_2D, textureId[i], 0);
        gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT, GL2.GL_RENDERBUFFER, renderbufferId);
        int status = gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER);
        if (status != GL2.GL_FRAMEBUFFER_COMPLETE) throw new FramebufferNotCreatedException(getErrorMessage(status));
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
    }

    private static String getErrorMessage(int status) {
        switch(status) {
            case GL2.GL_FRAMEBUFFER_COMPLETE:
                return "No Error.";
            case GL2.GL_FRAMEBUFFER_UNSUPPORTED:
                return "Framebuffer object format is unsupported by the video hardware.";
            case GL2.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                return "Incomplete attachment.";
            case GL2.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                return "Incomplete missing attachment.";
            case GL2.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
                return "Incomplete dimensions.";
            case GL2.GL_FRAMEBUFFER_INCOMPLETE_FORMATS:
                return "Incomplete formats.";
            case GL2.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
                return "Incomplete draw buffer.";
            case GL2.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
                return "Incomplete read buffer.";
            case GL2.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE:
                return "Incomplete multisample buffer.";
            default:
                return "Unknown error.";
        }
    }

    public void destroy() {
        GL2 gl = Graphics.getGL();
        int[] tmp = new int[1];
        tmp[0] = framebufferId;
        gl.glDeleteFramebuffers(1, tmp, 0);
        tmp[0] = renderbufferId;
        gl.glDeleteRenderbuffers(1, tmp, 0);
        for (int i = 0; i < textureId.length; i++) {
            tmp[0] = textureId[i];
            gl.glDeleteTextures(1, tmp, 0);
        }
    }

    public boolean getMipmaps() {
        return mipmaps;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void bind(int target) {
        GL2 gl = Graphics.getGL();
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, framebufferId);
        gl.glPushAttrib(GL2.GL_VIEWPORT_BIT);
        gl.glViewport(0, 0, width, height);
        gl.glDrawBuffer(GL2.GL_COLOR_ATTACHMENT0 + target);
    }

    public void unbind(int target) {
        GL2 gl = Graphics.getGL();
        gl.glPopAttrib();
        gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
        if (mipmaps) {
            gl.glBindTexture(GL2.GL_TEXTURE_2D, textureId[target]);
            gl.glGenerateMipmap(GL2.GL_TEXTURE_2D);
            gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
        }
    }

    public String getTextureString(int target) {
        return "*framebuffer* *" + textureId[target] + "|" + width + "|" + height + "*";
    }

    public int getTexture(int target) {
        return textureId[target];
    }

    public static int getMaxNumberOfTargets() {
        int[] maxbuffers = new int[1];
        Graphics.getGL().glGetIntegerv(GL2.GL_MAX_COLOR_ATTACHMENTS, maxbuffers, 0);
        return maxbuffers[0];
    }

    public static void copyBuffers(Framebuffer read, int readTarget, Framebuffer write, int writeTarget) {
        GL2 gl = Graphics.getGL();
        gl.glBindFramebuffer(GL2.GL_READ_FRAMEBUFFER, read.framebufferId);
        gl.glBindFramebuffer(GL2.GL_DRAW_FRAMEBUFFER, write.framebufferId);
        gl.glReadBuffer(GL2.GL_COLOR_ATTACHMENT0 + readTarget);
        gl.glDrawBuffer(GL2.GL_COLOR_ATTACHMENT0 + writeTarget);
        gl.glBlitFramebuffer(0, 0, read.width, read.height, 0, 0, write.width, write.height, GL2.GL_COLOR_BUFFER_BIT, GL2.GL_LINEAR);
        gl.glBlitFramebuffer(0, 0, read.width, read.height, 0, 0, write.width, write.height, GL2.GL_DEPTH_BUFFER_BIT, GL2.GL_LINEAR);
        gl.glBindFramebuffer(GL2.GL_READ_FRAMEBUFFER, 0);
        gl.glBindFramebuffer(GL2.GL_DRAW_FRAMEBUFFER, 0);
    }
}
