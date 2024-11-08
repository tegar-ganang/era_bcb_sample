package org.zkoss.util.resource.impl;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.Enumeration;
import java.net.URL;
import java.io.InputStream;
import java.io.IOException;
import org.zkoss.mesg.MCommon;
import org.zkoss.lang.Library;
import org.zkoss.lang.SystemException;
import org.zkoss.util.Maps;
import org.zkoss.util.Locales;
import org.zkoss.util.resource.LabelLocator;
import org.zkoss.util.resource.ClassLocator;
import org.zkoss.util.logging.Log;
import org.zkoss.util.WaitLock;
import org.zkoss.xel.Expressions;
import org.zkoss.xel.XelContext;
import org.zkoss.xel.VariableResolver;
import org.zkoss.xel.util.SimpleXelContext;

/**
 * The label loader (implementation only).
 *
 * Used to implement {@link org.zkoss.util.resource.Labels}.
 *
 * <p>Notice that the encoding of i3-label.properties is assumed to be
 * UTF-8. If it is not the case, please refer to 
 * <a href="http://docs.zkoss.org/wiki/Developer_reference_Appendix_B._WEB-INF/zk.xml_Library_Properties">Libraries Properties</a>
 * for configuration (avaible since 3.6.0).
 *
 * @author tomyeh
 */
public class LabelLoader {

    private static final Log log = Log.lookup(LabelLoader.class);

    /** A map of (Locale l, Map(String key, String label)). */
    private final Map _labels = new HashMap(6);

    /** A list of LabelLocator. */
    private final List _locators = new LinkedList();

    /** The XEL context. */
    private XelContext _xelc;

    private String _jarcharset, _warcharset;

    /** Returns the label of the specified key, or null if not found.
	 */
    public String getLabel(String key) {
        final String label = getProperty(Locales.getCurrent(), key);
        if (label == null || label.length() == 0 || label.indexOf("${") < 0) return label;
        try {
            return (String) Expressions.evaluate(_xelc, label, String.class);
        } catch (Throwable ex) {
            log.error("Illegal label: key=" + key + " value=" + label, ex);
            return label;
        }
    }

    /** Sets the variable resolver, which is used if an EL expression
	 * is specified.
	 *
	 * @since 3.0.0
	 */
    public VariableResolver setVariableResolver(VariableResolver resolv) {
        final VariableResolver old = _xelc != null ? _xelc.getVariableResolver() : null;
        _xelc = resolv != null ? new SimpleXelContext(resolv, null) : null;
        return old;
    }

    /** Registers a locator which is used to load i3-label*.properties
	 * from other resource, such as servlet contexts.
	 */
    public void register(LabelLocator locator) {
        if (locator == null) throw new NullPointerException("locator");
        synchronized (_locators) {
            for (Iterator it = _locators.iterator(); it.hasNext(); ) if (it.next().equals(locator)) {
                log.warning("Ignored because of replication: " + locator);
                return;
            }
            _locators.add(locator);
        }
        reset();
    }

    /** Resets all cached labels and next call to {@link #getLabel}
	 * will cause re-loading i3-label*.proerties.
	 */
    public void reset() {
        synchronized (_labels) {
            _labels.clear();
        }
    }

    /** Returns the property without interprets any expression.
	 * It searches properties defined in i3-label*.properties
	 * All label accesses are eventually done by this method.
	 *
	 * <p>To alter its behavior, you might override this method.
	 */
    protected String getProperty(Locale locale, String key) {
        String label = (String) getLabels(locale).get(key);
        if (label != null) return label;
        final String lang = locale.getLanguage();
        final String cnty = locale.getCountry();
        final String var = locale.getVariant();
        if (var != null && var.length() > 0) {
            label = (String) getLabels(new Locale(lang, cnty)).get(key);
            if (label != null) return label;
        }
        if (cnty != null && cnty.length() > 0) {
            label = (String) getLabels(new Locale(lang, "")).get(key);
            if (label != null) return label;
        }
        return (String) getLabels(null).get(key);
    }

    /** Returns Map(String key, String label) of the specified locale. */
    private final Map getLabels(Locale locale) {
        WaitLock lock = null;
        for (; ; ) {
            final Object o;
            synchronized (_labels) {
                o = _labels.get(locale);
                if (o == null) _labels.put(locale, lock = new WaitLock());
            }
            if (o instanceof Map) return (Map) o;
            if (o == null) break;
            if (!((WaitLock) o).waitUntilUnlock(5 * 60 * 1000)) log.warning("Take too long to wait loading labels: " + locale + "\nTry to load again automatically...");
        }
        if (_jarcharset == null) _jarcharset = Library.getProperty("org.zkoss.util.label.classpath.charset", "UTF-8");
        if (_warcharset == null) {
            _warcharset = Library.getProperty("org.zkoss.util.label.web.charset", null);
            if (_warcharset == null) _warcharset = Library.getProperty("org.zkoss.util.label.WEB-INF.charset", "UTF-8");
        }
        try {
            log.info("Loading labels for " + locale);
            final Map labels = new HashMap(512);
            final ClassLocator locator = new ClassLocator();
            for (Enumeration en = locator.getResources(locale == null ? "metainfo/i3-label.properties" : "metainfo/i3-label_" + locale + ".properties"); en.hasMoreElements(); ) {
                final URL url = (URL) en.nextElement();
                load(labels, url, _jarcharset);
            }
            for (Iterator it = _locators.iterator(); it.hasNext(); ) {
                final URL url = ((LabelLocator) it.next()).locate(locale);
                if (url != null) load(labels, url, _warcharset);
            }
            synchronized (_labels) {
                _labels.put(locale, labels);
            }
            return labels;
        } catch (Throwable ex) {
            synchronized (_labels) {
                _labels.remove(locale);
            }
            throw SystemException.Aide.wrap(ex);
        } finally {
            lock.unlock();
        }
    }

    /** Loads all labels from the specified URL. */
    private static final void load(Map labels, URL url, String charset) throws IOException {
        log.info(MCommon.FILE_OPENING, url);
        final Map news = new HashMap();
        final InputStream is = url.openStream();
        try {
            Maps.load(news, is, charset);
        } finally {
            try {
                is.close();
            } catch (Throwable ex) {
            }
        }
        for (Iterator it = news.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry me = (Map.Entry) it.next();
            final Object key = me.getKey();
            if (labels.put(key, me.getValue()) != null) log.warning("Label of " + key + " is replaced by " + url);
        }
    }
}
