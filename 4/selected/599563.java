package jogamp.opengl.egl;

import javax.media.opengl.*;
import jogamp.opengl.*;
import com.jogamp.gluegen.runtime.ProcAddressTable;
import com.jogamp.gluegen.runtime.opengl.GLProcAddressResolver;
import java.nio.*;
import java.util.*;
import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;

public abstract class EGLContext extends GLContextImpl {

    private boolean eglQueryStringInitialized;

    private boolean eglQueryStringAvailable;

    private EGLExt eglExt;

    private EGLExtProcAddressTable eglExtProcAddressTable;

    EGLContext(GLDrawableImpl drawable, GLContext shareWith) {
        super(drawable, shareWith);
    }

    public Object getPlatformGLExtensions() {
        return getEGLExt();
    }

    public EGLExt getEGLExt() {
        if (eglExt == null) {
            eglExt = new EGLExtImpl(this);
        }
        return eglExt;
    }

    public final ProcAddressTable getPlatformExtProcAddressTable() {
        return eglExtProcAddressTable;
    }

    public final EGLExtProcAddressTable getEGLExtProcAddressTable() {
        return eglExtProcAddressTable;
    }

    protected Map<String, String> getFunctionNameMap() {
        return null;
    }

    protected Map<String, String> getExtensionNameMap() {
        return null;
    }

    public final boolean isGLReadDrawableAvailable() {
        return true;
    }

    protected void makeCurrentImpl(boolean newCreated) throws GLException {
        if (EGL.EGL_NO_DISPLAY == ((EGLDrawable) drawable).getDisplay()) {
            throw new GLException("drawable not properly initialized, NO DISPLAY: " + drawable);
        }
        if (EGL.eglGetCurrentContext() != contextHandle) {
            if (!EGL.eglMakeCurrent(((EGLDrawable) drawable).getDisplay(), drawable.getHandle(), drawableRead.getHandle(), contextHandle)) {
                throw new GLException("Error making context 0x" + Long.toHexString(contextHandle) + " current: error code " + EGL.eglGetError());
            }
        }
    }

    protected void releaseImpl() throws GLException {
        if (!EGL.eglMakeCurrent(((EGLDrawable) drawable).getDisplay(), EGL.EGL_NO_SURFACE, EGL.EGL_NO_SURFACE, EGL.EGL_NO_CONTEXT)) {
            throw new GLException("Error freeing OpenGL context 0x" + Long.toHexString(contextHandle) + ": error code " + EGL.eglGetError());
        }
    }

    protected void destroyImpl() throws GLException {
        if (!EGL.eglDestroyContext(((EGLDrawable) drawable).getDisplay(), contextHandle)) {
            throw new GLException("Error destroying OpenGL context 0x" + Long.toHexString(contextHandle) + ": error code " + EGL.eglGetError());
        }
    }

    protected long createContextARBImpl(long share, boolean direct, int ctp, int major, int minor) {
        return 0;
    }

    protected void destroyContextARBImpl(long _context) {
    }

    protected boolean createImpl() throws GLException {
        long eglDisplay = ((EGLDrawable) drawable).getDisplay();
        EGLGraphicsConfiguration config = ((EGLDrawable) drawable).getGraphicsConfiguration();
        GLProfile glProfile = drawable.getGLProfile();
        long eglConfig = config.getNativeConfig();
        long shareWith = EGL.EGL_NO_CONTEXT;
        if (eglDisplay == 0) {
            throw new GLException("Error: attempted to create an OpenGL context without a display connection");
        }
        if (eglConfig == 0) {
            throw new GLException("Error: attempted to create an OpenGL context without a graphics configuration");
        }
        try {
            if (!EGL.eglBindAPI(EGL.EGL_OPENGL_ES_API)) {
                throw new GLException("eglBindAPI to ES failed , error 0x" + Integer.toHexString(EGL.eglGetError()));
            }
        } catch (GLException glex) {
            if (DEBUG) {
                glex.printStackTrace();
            }
        }
        EGLContext other = (EGLContext) GLContextShareSet.getShareContext(this);
        if (other != null) {
            shareWith = other.getHandle();
            if (shareWith == 0) {
                throw new GLException("GLContextShareSet returned an invalid OpenGL context");
            }
        }
        int[] contextAttrs = new int[] { EGL.EGL_CONTEXT_CLIENT_VERSION, -1, EGL.EGL_NONE };
        if (glProfile.usesNativeGLES2()) {
            contextAttrs[1] = 2;
        } else if (glProfile.usesNativeGLES1()) {
            contextAttrs[1] = 1;
        } else {
            throw new GLException("Error creating OpenGL context - invalid GLProfile: " + glProfile);
        }
        contextHandle = EGL.eglCreateContext(eglDisplay, eglConfig, shareWith, contextAttrs, 0);
        if (contextHandle == 0) {
            throw new GLException("Error creating OpenGL context: eglDisplay " + toHexString(eglDisplay) + ", eglConfig " + toHexString(eglConfig) + ", " + glProfile + ", error " + toHexString(EGL.eglGetError()));
        }
        GLContextShareSet.contextCreated(this);
        if (DEBUG) {
            System.err.println(getThreadName() + ": !!! Created OpenGL context 0x" + Long.toHexString(contextHandle) + ",\n\twrite surface 0x" + Long.toHexString(drawable.getHandle()) + ",\n\tread  surface 0x" + Long.toHexString(drawableRead.getHandle()) + ",\n\t" + this + ",\n\tsharing with 0x" + Long.toHexString(shareWith));
        }
        if (!EGL.eglMakeCurrent(((EGLDrawable) drawable).getDisplay(), drawable.getHandle(), drawableRead.getHandle(), contextHandle)) {
            throw new GLException("Error making context 0x" + Long.toHexString(contextHandle) + " current: error code " + EGL.eglGetError());
        }
        int ctp = CTX_PROFILE_ES | CTX_OPTION_ANY;
        int major;
        if (glProfile.usesNativeGLES2()) {
            ctp |= CTX_PROFILE_ES2_COMPAT;
            major = 2;
        } else {
            major = 1;
        }
        setGLFunctionAvailability(true, major, 0, ctp);
        return true;
    }

    protected final void updateGLXProcAddressTable() {
        AbstractGraphicsConfiguration aconfig = drawable.getNativeSurface().getGraphicsConfiguration().getNativeGraphicsConfiguration();
        AbstractGraphicsDevice adevice = aconfig.getScreen().getDevice();
        String key = adevice.getUniqueID();
        if (DEBUG) {
            System.err.println(getThreadName() + ": !!! Initializing EGLextension address table: " + key);
        }
        eglQueryStringInitialized = false;
        eglQueryStringAvailable = false;
        ProcAddressTable table = null;
        synchronized (mappedContextTypeObjectLock) {
            table = mappedGLXProcAddress.get(key);
        }
        if (null != table) {
            eglExtProcAddressTable = (EGLExtProcAddressTable) table;
            if (DEBUG) {
                System.err.println(getThreadName() + ": !!! GLContext EGL ProcAddressTable reusing key(" + key + ") -> " + table.hashCode());
            }
        } else {
            if (eglExtProcAddressTable == null) {
                eglExtProcAddressTable = new EGLExtProcAddressTable(new GLProcAddressResolver());
            }
            resetProcAddressTable(getEGLExtProcAddressTable());
            synchronized (mappedContextTypeObjectLock) {
                mappedGLXProcAddress.put(key, getEGLExtProcAddressTable());
                if (DEBUG) {
                    System.err.println(getThreadName() + ": !!! GLContext EGL ProcAddressTable mapping key(" + key + ") -> " + getEGLExtProcAddressTable().hashCode());
                }
            }
        }
    }

    public synchronized String getPlatformExtensionsString() {
        if (!eglQueryStringInitialized) {
            eglQueryStringAvailable = getDrawableImpl().getGLDynamicLookupHelper().dynamicLookupFunction("eglQueryString") != 0;
            eglQueryStringInitialized = true;
        }
        if (eglQueryStringAvailable) {
            String ret = EGL.eglQueryString(((EGLDrawable) drawable).getDisplay(), EGL.EGL_EXTENSIONS);
            if (DEBUG) {
                System.err.println("!!! EGL extensions: " + ret);
            }
            return ret;
        } else {
            return "";
        }
    }

    protected void setSwapIntervalImpl(int interval) {
        if (EGL.eglSwapInterval(((EGLDrawable) drawable).getDisplay(), interval)) {
            currentSwapInterval = interval;
        }
    }

    public abstract void bindPbufferToTexture();

    public abstract void releasePbufferFromTexture();

    protected void copyImpl(GLContext source, int mask) throws GLException {
        throw new GLException("Not yet implemented");
    }

    public ByteBuffer glAllocateMemoryNV(int arg0, float arg1, float arg2, float arg3) {
        throw new GLException("Should not call this");
    }

    public boolean offscreenImageNeedsVerticalFlip() {
        throw new GLException("Should not call this");
    }

    public int getOffscreenContextPixelDataType() {
        throw new GLException("Should not call this");
    }
}
