package org.zkoss.web.util.resource;

import java.net.URL;
import java.io.InputStream;
import org.zkoss.lang.Library;
import org.zkoss.lang.Exceptions;
import org.zkoss.io.Files;
import org.zkoss.util.logging.Log;
import org.zkoss.util.resource.Loader;

/**
 * A skeletal implementation of the loader used to implement an extendlet.
 * All you have to do is to implement {@link #parse}
 * and {@link #getExtendletContext}.
 *
 * @author tomyeh
 * @see Extendlet
 * @since 3.0.6
 */
public abstract class ExtendletLoader implements Loader {

    private static final Log log = Log.lookup(ExtendletLoader.class);

    private int _checkPeriod;

    protected ExtendletLoader() {
        _checkPeriod = getInitCheckPeriod();
    }

    public boolean shallCheck(Object src, long expiredMillis) {
        return expiredMillis > 0;
    }

    /** Returns the last modified time.
	 */
    public long getLastModified(Object src) {
        if (getCheckPeriod() < 0) return 1;
        try {
            final URL url = getExtendletContext().getResource((String) src);
            return url != null ? url.openConnection().getLastModified() : -1;
        } catch (Throwable ex) {
            return -1;
        }
    }

    public Object load(Object src) throws Exception {
        final String path = (String) src;
        InputStream is = null;
        if (getCheckPeriod() >= 0) {
            try {
                URL real = getExtendletContext().getResource(path);
                if (real != null) is = real.openStream();
            } catch (Throwable ex) {
                log.warningBriefly("Unable to read from URL: " + path, ex);
            }
        }
        if (is == null) {
            is = getExtendletContext().getResourceAsStream(path);
            if (is == null) return null;
        }
        try {
            return parse(is, path);
        } catch (Exception ex) {
            if (log.debugable()) log.realCauseBriefly("Failed to parse " + path, ex); else log.error("Failed to parse " + path + "\nCause: " + ex.getClass().getName() + " " + Exceptions.getMessage(ex) + "\n" + Exceptions.getBriefStackTrace(ex));
            return null;
        } finally {
            Files.close(is);
        }
    }

    /** It is called to parse the resource into an intermediate format
	 * depending on {@link Extendlet}.
	 *
	 * @param is the content of the resource
	 * @param path the path of the resource
	 */
    protected abstract Object parse(InputStream is, String path) throws Exception;

    /** Returns the extendlet context.
	 */
    protected abstract ExtendletContext getExtendletContext();

    /** Returns the check period, or -1 if the content is never changed.
	 * Unit: milliseconds.
	 *
	 * <p>Default: It checks if an integer (unit: second) is assigned
	 * to a system property called org.zkoss.util.resource.extendlet.checkPeriod.
	 * If no such system property, -1 is assumed (never change).
	 * For the runtime environment the content is never changed,
	 * since all extendlet resources are packed in JAR files.
	 */
    public int getCheckPeriod() {
        return _checkPeriod;
    }

    private static int getInitCheckPeriod() {
        final int v = Library.getIntProperty("org.zkoss.util.resource.extendlet.checkPeriod", -1);
        return v > 0 ? v * 1000 : v;
    }
}
