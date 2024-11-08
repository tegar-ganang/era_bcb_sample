package org.jnativehook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.EventListener;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.swing.event.EventListenerList;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.jnativehook.mouse.NativeMouseEvent;
import org.jnativehook.mouse.NativeMouseListener;
import org.jnativehook.mouse.NativeMouseMotionListener;
import org.jnativehook.mouse.NativeMouseWheelEvent;
import org.jnativehook.mouse.NativeMouseWheelListener;

/**
 * GlobalScreen is used to represent the native screen area that Java does not 
 * usually have access to. This class can be thought of as the source component 
 * for native events.  
 * <p>
 * This class also handles the loading, unpacking and communication with the 
 * native library. That includes registering new key and button hooks and the 
 * event dispatchers for each.
 * 
 * @author	Alexander Barker (<a href="mailto:alex@1stleg.com">alex@1stleg.com</a>)
 * @version	1.1
 */
public class GlobalScreen {

    /** The GlobalScreen singleton. */
    private static final GlobalScreen instance = new GlobalScreen();

    /** The list of event listeners to notify. */
    private EventListenerList eventListeners;

    /**
	 * Private constructor to prevent multiple instances of the global screen.
	 * The {@link #registerNativeHook} method will be called on construction to unpack
	 * and load the native library.
	 */
    private GlobalScreen() {
        eventListeners = new EventListenerList();
        GlobalScreen.loadNativeLibrary();
    }

    /**
	 * A deconstructor that will perform native cleanup by calling the
	 * {@link #unregisterNativeHook} method.  This method will not run until the
	 * class is garbage collected.
	 *
	 * @throws Throwable The <code>Exception</code> raised by this method.
	 * @see Object#finalize
	 */
    @Override
    protected void finalize() throws Throwable {
        try {
            GlobalScreen.unloadNativeLibrary();
        } catch (Exception e) {
        } finally {
            super.finalize();
        }
    }

    /**
	 * Gets the single instance of GlobalScreen.
	 *
	 * @return single instance of GlobalScreen
	 */
    public static final GlobalScreen getInstance() {
        return GlobalScreen.instance;
    }

    /**
	 * Adds the specified native key listener to receive key events from the 
	 * native system. If listener is null, no exception is thrown and no action 
	 * is performed.
	 *
	 * @param listener the native key listener
	 */
    public void addNativeKeyListener(NativeKeyListener listener) {
        if (listener != null) {
            eventListeners.add(NativeKeyListener.class, listener);
        }
    }

    /**
	 * Removes the specified native key listener so that it no longer receives 
	 * key events from the native system. This method performs no function if 
	 * the listener specified by the argument was not previously added.  If 
	 * listener is null, no exception is thrown and no action is performed.
	 *
	 * @param listener the native key listener
	 */
    public void removeNativeKeyListener(NativeKeyListener listener) {
        if (listener != null) {
            eventListeners.remove(NativeKeyListener.class, listener);
        }
    }

    /**
	 * Adds the specified native mouse listener to receive mouse events from the 
	 * native system. If listener is null, no exception is thrown and no action 
	 * is performed.
	 *
	 * @param listener the native mouse listener
	 */
    public void addNativeMouseListener(NativeMouseListener listener) {
        if (listener != null) {
            eventListeners.add(NativeMouseListener.class, listener);
        }
    }

    /**
	 * Removes the specified native mouse listener so that it no longer receives 
	 * mouse events from the native system. This method performs no function if 
	 * the listener specified by the argument was not previously added.  If 
	 * listener is null, no exception is thrown and no action is performed.
	 *
	 * @param listener the native mouse listener
	 */
    public void removeNativeMouseListener(NativeMouseListener listener) {
        if (listener != null) {
            eventListeners.remove(NativeMouseListener.class, listener);
        }
    }

    /**
	 * Adds the specified native mouse motion listener to receive mouse motion 
	 * events from the native system. If listener is null, no exception is 
	 * thrown and no action is performed.
	 *
	 * @param listener the native mouse motion listener
	 */
    public void addNativeMouseMotionListener(NativeMouseMotionListener listener) {
        if (listener != null) {
            eventListeners.add(NativeMouseMotionListener.class, listener);
        }
    }

    /**
	 * Removes the specified native mouse motion listener so that it no longer 
	 * receives mouse motion events from the native system. This method performs 
	 * no function if the listener specified by the argument was not previously 
	 * added.  If listener is null, no exception is thrown and no action is 
	 * performed.
	 *
	 * @param listener the native mouse motion listener
	 */
    public void removeNativeMouseMotionListener(NativeMouseMotionListener listener) {
        if (listener != null) {
            eventListeners.remove(NativeMouseMotionListener.class, listener);
        }
    }

    /**
	 * Adds the specified native mouse wheel listener to receive mouse wheel 
	 * events from the native system. If listener is null, no exception is 
	 * thrown and no action is performed.
	 *
	 * @param listener the native mouse wheel listener
	 * 
	 * @since 1.1
	 */
    public void addNativeMouseWheelListener(NativeMouseWheelListener listener) {
        if (listener != null) {
            eventListeners.add(NativeMouseWheelListener.class, listener);
        }
    }

    /**
	 * Removes the specified native mouse wheel listener so that it no longer 
	 * receives mouse wheel events from the native system. This method performs 
	 * no function if the listener specified by the argument was not previously 
	 * added.  If listener is null, no exception is thrown and no action is 
	 * performed.
	 *
	 * @param listener the native mouse wheel listener
	 * 
	 * @since 1.1
	 */
    public void removeNativeMouseWheelListener(NativeMouseWheelListener listener) {
        if (listener != null) {
            eventListeners.remove(NativeMouseWheelListener.class, listener);
        }
    }

    /**
	 * Enable the native hook if it is not currently running. If it is running
	 * the function has no effect. <b>Note that this method may block the AWT
	 * event dispatching thread.</b> It is recomended to call this method from
	 * outside the scope of the graphical user interface event queue.
	 *
	 * @throws NativeHookException the native hook exception
	 * 
	 * @since 1.1
	 */
    public native void registerNativeHook() throws NativeHookException;

    /**
	 * Disable the native hook if it is currently running. If it is not running
	 * the function has no effect. <b>Note that this method may block the AWT
	 * event dispatching thread.</b> It is recomended to call this method from
	 * outside the scope of the graphical user interface event queue.
	 *
	 * @throws NativeHookException the native hook exception
	 * 
	 * @since 1.1
	 */
    public native void unregisterNativeHook() throws NativeHookException;

    /**
	 * Gets the current state of the native hook.
	 *
	 * @return the state of the native hook.
	 * @throws NativeHookException the native hook exception
	 * 
	 * @since 1.1
	 */
    public native boolean isNativeHookRegistered();

    /**
	 * Returns true if the current thread is the native event dispatching thread.
	 *
	 * @return true if the current thread is the native event dispatching thread.
	 * @throws NativeHookException the native hook exception
	 *
	 * @since 1.1
	 */
    public static native boolean isNativeDispatchThread();

    /**
	 * Dispatches an event to the appropriate processor.  This method is 
	 * generally called by the native library but maybe used to synthesize 
	 * native events from Java.
	 * 
	 * @param e the native input event
	 */
    public final void dispatchEvent(NativeInputEvent e) {
        if (e instanceof NativeKeyEvent) {
            processKeyEvent((NativeKeyEvent) e);
        } else if (e instanceof NativeMouseWheelEvent) {
            processMouseWheelEvent((NativeMouseWheelEvent) e);
        } else if (e instanceof NativeMouseEvent) {
            processMouseEvent((NativeMouseEvent) e);
        }
    }

    /**
	 * Processes native key events by dispatching them to all registered 
	 * <code>NativeKeyListener</code> objects.
	 * 
	 * @param e The <code>NativeKeyEvent</code> to dispatch.
	 * @see NativeKeyEvent
	 * @see NativeKeyListener
	 * @see #addNativeKeyListener(NativeKeyListener)
	 */
    protected void processKeyEvent(NativeKeyEvent e) {
        int id = e.getID();
        EventListener[] listeners = eventListeners.getListeners(NativeKeyListener.class);
        for (int i = 0; i < listeners.length; i++) {
            switch(id) {
                case NativeKeyEvent.NATIVE_KEY_PRESSED:
                    ((NativeKeyListener) listeners[i]).keyPressed(e);
                    break;
                case NativeKeyEvent.NATIVE_KEY_TYPED:
                    ((NativeKeyListener) listeners[i]).keyTyped(e);
                    break;
                case NativeKeyEvent.NATIVE_KEY_RELEASED:
                    ((NativeKeyListener) listeners[i]).keyReleased(e);
                    break;
            }
        }
    }

    /**
	 * Processes native mouse events by dispatching them to all registered 
	 * <code>NativeMouseListener</code> objects.
	 * 
	 * @param e The <code>NativeMouseEvent</code> to dispatch.
	 * @see NativeMouseEvent
	 * @see NativeMouseListener
	 * @see #addNativeMouseListener(NativeMouseListener)
	 */
    protected void processMouseEvent(NativeMouseEvent e) {
        int id = e.getID();
        EventListener[] listeners;
        if (id == NativeMouseEvent.NATIVE_MOUSE_MOVED || id == NativeMouseEvent.NATIVE_MOUSE_DRAGGED) {
            listeners = eventListeners.getListeners(NativeMouseMotionListener.class);
        } else {
            listeners = eventListeners.getListeners(NativeMouseListener.class);
        }
        for (int i = 0; i < listeners.length; i++) {
            switch(id) {
                case NativeMouseEvent.NATIVE_MOUSE_CLICKED:
                    ((NativeMouseListener) listeners[i]).mouseClicked(e);
                    break;
                case NativeMouseEvent.NATIVE_MOUSE_PRESSED:
                    ((NativeMouseListener) listeners[i]).mousePressed(e);
                    break;
                case NativeMouseEvent.NATIVE_MOUSE_RELEASED:
                    ((NativeMouseListener) listeners[i]).mouseReleased(e);
                    break;
                case NativeMouseEvent.NATIVE_MOUSE_MOVED:
                    ((NativeMouseMotionListener) listeners[i]).mouseMoved(e);
                    break;
                case NativeMouseEvent.NATIVE_MOUSE_DRAGGED:
                    ((NativeMouseMotionListener) listeners[i]).mouseDragged(e);
                    break;
            }
        }
    }

    /**
	 * Processes native mouse wheel events by dispatching them to all registered 
	 * <code>NativeMouseWheelListener</code> objects.
	 * 
	 * @param e The <code>NativeMouseWheelEvent</code> to dispatch.
	 * @see NativeMouseWheelEvent
	 * @see NativeMouseWheelListener
	 * @see #addNativeMouseWheelListener(NativeMouseWheelListener)
	 * 
	 * @since 1.1
	 */
    protected void processMouseWheelEvent(NativeMouseWheelEvent e) {
        EventListener[] listeners = eventListeners.getListeners(NativeMouseWheelListener.class);
        for (int i = 0; i < listeners.length; i++) {
            ((NativeMouseWheelListener) listeners[i]).mouseWheelMoved(e);
        }
    }

    /**
	 * Perform procedures to interface with the native library. These procedures 
	 * include unpacking and loading the library into the Java Virtual Machine.
	 */
    protected static void loadNativeLibrary() {
        String libName = "JNativeHook";
        try {
            System.loadLibrary(libName);
        } catch (UnsatisfiedLinkError linkError) {
            try {
                String jarLibPath = "org/jnativehook/lib/" + NativeSystem.getFamily().toString().toLowerCase() + "/" + NativeSystem.getArchitecture().toString().toLowerCase() + "/";
                File classFile = new File(GlobalScreen.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsoluteFile();
                if (classFile.isFile()) {
                    JarFile jarFile = new JarFile(classFile);
                    JarEntry jarLibEntry = jarFile.getJarEntry(jarLibPath + System.mapLibraryName(libName));
                    File tmpLibFile = new File(System.getProperty("java.io.tmpdir") + System.getProperty("file.separator", File.separator) + System.mapLibraryName(libName));
                    InputStream jarInputStream = jarFile.getInputStream(jarLibEntry);
                    FileOutputStream tempLibOutputStream = new FileOutputStream(tmpLibFile);
                    byte[] array = new byte[8192];
                    int read = 0;
                    while ((read = jarInputStream.read(array)) > 0) {
                        tempLibOutputStream.write(array, 0, read);
                    }
                    tempLibOutputStream.close();
                    tmpLibFile.deleteOnExit();
                    System.load(tmpLibFile.getPath());
                } else if (classFile.isDirectory()) {
                    File libFolder = new File(classFile.getAbsoluteFile() + "/" + jarLibPath);
                    if (libFolder.isDirectory()) {
                        System.setProperty("java.library.path", System.getProperty("java.library.path", ".") + System.getProperty("path.separator", ":") + libFolder.getPath());
                        Field sysPath = ClassLoader.class.getDeclaredField("sys_paths");
                        sysPath.setAccessible(true);
                        if (sysPath != null) {
                            sysPath.set(System.class.getClassLoader(), null);
                        }
                        System.loadLibrary(libName);
                    }
                }
            } catch (URISyntaxException e) {
                throw new RuntimeException(e.getMessage());
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }
    }

    /**
	 * Perform procedures to cleanup the native library. This method is called 
	 * on garbage collection to ensure proper native cleanup.
	 */
    protected static void unloadNativeLibrary() {
        try {
            instance.unregisterNativeHook();
        } catch (NativeHookException e) {
            e.printStackTrace();
        }
    }
}
