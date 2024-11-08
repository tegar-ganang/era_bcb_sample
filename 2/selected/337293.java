package org.eclipse.osgi.framework.internal.protocol;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.*;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;

public class MultiplexingURLStreamHandler extends URLStreamHandler {

    private static Method openConnectionMethod;

    private static Method equalsMethod;

    private static Method getDefaultPortMethod;

    private static Method getHostAddressMethod;

    private static Method hashCodeMethod;

    private static Method hostsEqualMethod;

    private static Method parseURLMethod;

    private static Method sameFileMethod;

    private static Method setURLMethod;

    private static Method toExternalFormMethod;

    private static Field handlerField;

    private static boolean methodsInitialized = false;

    private String protocol;

    private StreamHandlerFactory factory;

    private static synchronized void initializeMethods(StreamHandlerFactory factory) {
        if (methodsInitialized) return;
        try {
            openConnectionMethod = URLStreamHandler.class.getDeclaredMethod("openConnection", new Class[] { URL.class });
            openConnectionMethod.setAccessible(true);
            equalsMethod = URLStreamHandler.class.getDeclaredMethod("equals", new Class[] { URL.class, URL.class });
            equalsMethod.setAccessible(true);
            getDefaultPortMethod = URLStreamHandler.class.getDeclaredMethod("getDefaultPort", null);
            getDefaultPortMethod.setAccessible(true);
            getHostAddressMethod = URLStreamHandler.class.getDeclaredMethod("getHostAddress", new Class[] { URL.class });
            getHostAddressMethod.setAccessible(true);
            hashCodeMethod = URLStreamHandler.class.getDeclaredMethod("hashCode", new Class[] { URL.class });
            hashCodeMethod.setAccessible(true);
            hostsEqualMethod = URLStreamHandler.class.getDeclaredMethod("hostsEqual", new Class[] { URL.class, URL.class });
            hostsEqualMethod.setAccessible(true);
            parseURLMethod = URLStreamHandler.class.getDeclaredMethod("parseURL", new Class[] { URL.class, String.class, Integer.TYPE, Integer.TYPE });
            parseURLMethod.setAccessible(true);
            sameFileMethod = URLStreamHandler.class.getDeclaredMethod("sameFile", new Class[] { URL.class, URL.class });
            sameFileMethod.setAccessible(true);
            setURLMethod = URLStreamHandler.class.getDeclaredMethod("setURL", new Class[] { URL.class, String.class, String.class, Integer.TYPE, String.class, String.class, String.class, String.class, String.class });
            setURLMethod.setAccessible(true);
            toExternalFormMethod = URLStreamHandler.class.getDeclaredMethod("toExternalForm", new Class[] { URL.class });
            toExternalFormMethod.setAccessible(true);
            handlerField = URL.class.getDeclaredField("handler");
            handlerField.setAccessible(true);
        } catch (Exception e) {
            factory.adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingURLStreamHandler.class.getName(), "initializeMethods", FrameworkLogEntry.ERROR, e, null));
            throw new RuntimeException(e.getMessage());
        }
        methodsInitialized = true;
    }

    public MultiplexingURLStreamHandler(String protocol, StreamHandlerFactory factory) {
        this.protocol = protocol;
        this.factory = factory;
        initializeMethods(factory);
    }

    protected URLConnection openConnection(URL url) throws IOException {
        URLStreamHandler handler = factory.findAuthorizedURLStreamHandler(protocol);
        if (handler != null) {
            try {
                return (URLConnection) openConnectionMethod.invoke(handler, new Object[] { url });
            } catch (Exception e) {
                factory.adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingURLStreamHandler.class.getName(), "openConnection", FrameworkLogEntry.ERROR, e, null));
                throw new RuntimeException(e.getMessage());
            }
        }
        throw new MalformedURLException();
    }

    protected boolean equals(URL url1, URL url2) {
        URLStreamHandler handler = factory.findAuthorizedURLStreamHandler(protocol);
        if (handler != null) {
            try {
                return ((Boolean) equalsMethod.invoke(handler, new Object[] { url1, url2 })).booleanValue();
            } catch (Exception e) {
                factory.adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingURLStreamHandler.class.getName(), "equals", FrameworkLogEntry.ERROR, e, null));
                throw new RuntimeException(e.getMessage());
            }
        }
        throw new IllegalStateException();
    }

    protected int getDefaultPort() {
        URLStreamHandler handler = factory.findAuthorizedURLStreamHandler(protocol);
        if (handler != null) {
            try {
                return ((Integer) getDefaultPortMethod.invoke(handler, null)).intValue();
            } catch (Exception e) {
                factory.adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingURLStreamHandler.class.getName(), "getDefaultPort", FrameworkLogEntry.ERROR, e, null));
                throw new RuntimeException(e.getMessage());
            }
        }
        throw new IllegalStateException();
    }

    protected InetAddress getHostAddress(URL url) {
        URLStreamHandler handler = factory.findAuthorizedURLStreamHandler(protocol);
        if (handler != null) {
            try {
                return (InetAddress) getHostAddressMethod.invoke(handler, new Object[] { url });
            } catch (Exception e) {
                factory.adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingURLStreamHandler.class.getName(), "hashCode", FrameworkLogEntry.ERROR, e, null));
                throw new RuntimeException(e.getMessage());
            }
        }
        throw new IllegalStateException();
    }

    protected int hashCode(URL url) {
        URLStreamHandler handler = factory.findAuthorizedURLStreamHandler(protocol);
        if (handler != null) {
            try {
                return ((Integer) hashCodeMethod.invoke(handler, new Object[] { url })).intValue();
            } catch (Exception e) {
                factory.adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingURLStreamHandler.class.getName(), "hashCode", FrameworkLogEntry.ERROR, e, null));
                throw new RuntimeException(e.getMessage());
            }
        }
        throw new IllegalStateException();
    }

    protected boolean hostsEqual(URL url1, URL url2) {
        URLStreamHandler handler = factory.findAuthorizedURLStreamHandler(protocol);
        if (handler != null) {
            try {
                return ((Boolean) hostsEqualMethod.invoke(handler, new Object[] { url1, url2 })).booleanValue();
            } catch (Exception e) {
                factory.adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingURLStreamHandler.class.getName(), "hostsEqual", FrameworkLogEntry.ERROR, e, null));
                throw new RuntimeException(e.getMessage());
            }
        }
        throw new IllegalStateException();
    }

    protected void parseURL(URL arg0, String arg1, int arg2, int arg3) {
        URLStreamHandler handler = factory.findAuthorizedURLStreamHandler(protocol);
        if (handler != null) {
            try {
                handlerField.set(arg0, handler);
                parseURLMethod.invoke(handler, new Object[] { arg0, arg1, new Integer(arg2), new Integer(arg3) });
                handlerField.set(arg0, this);
                return;
            } catch (Exception e) {
                factory.adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingURLStreamHandler.class.getName(), "parseURL", FrameworkLogEntry.ERROR, e, null));
                throw new RuntimeException(e.getMessage());
            }
        }
        throw new IllegalStateException();
    }

    protected boolean sameFile(URL url1, URL url2) {
        URLStreamHandler handler = factory.findAuthorizedURLStreamHandler(protocol);
        if (handler != null) {
            try {
                return ((Boolean) sameFileMethod.invoke(handler, new Object[] { url1, url2 })).booleanValue();
            } catch (Exception e) {
                factory.adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingURLStreamHandler.class.getName(), "sameFile", FrameworkLogEntry.ERROR, e, null));
                throw new RuntimeException(e.getMessage());
            }
        }
        throw new IllegalStateException();
    }

    protected void setURL(URL arg0, String arg1, String arg2, int arg3, String arg4, String arg5, String arg6, String arg7, String arg8) {
        URLStreamHandler handler = factory.findAuthorizedURLStreamHandler(protocol);
        if (handler != null) {
            try {
                handlerField.set(arg0, handler);
                setURLMethod.invoke(handler, new Object[] { arg0, arg1, arg2, new Integer(arg3), arg4, arg5, arg6, arg7, arg8 });
                handlerField.set(arg0, this);
                return;
            } catch (Exception e) {
                factory.adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingURLStreamHandler.class.getName(), "setURL", FrameworkLogEntry.ERROR, e, null));
                throw new RuntimeException(e.getMessage());
            }
        }
        throw new IllegalStateException();
    }

    protected String toExternalForm(URL url) {
        URLStreamHandler handler = factory.findAuthorizedURLStreamHandler(protocol);
        if (handler != null) {
            try {
                return (String) toExternalFormMethod.invoke(handler, new Object[] { url });
            } catch (Exception e) {
                factory.adaptor.getFrameworkLog().log(new FrameworkLogEntry(MultiplexingURLStreamHandler.class.getName(), "toExternalForm", FrameworkLogEntry.ERROR, e, null));
                throw new RuntimeException(e.getMessage());
            }
        }
        throw new IllegalStateException();
    }
}
