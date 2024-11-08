package com.potix.util.resource.impl;

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
import javax.servlet.jsp.el.VariableResolver;
import com.potix.mesg.MCommon;
import com.potix.lang.SystemException;
import com.potix.util.Maps;
import com.potix.util.Locales;
import com.potix.util.resource.LabelLocator;
import com.potix.util.resource.ClassLocator;
import com.potix.util.logging.Log;
import com.potix.util.WaitLock;
import com.potix.el.EvaluatorImpl;

/**
 * The label loader (implementation only).
 *
 * Used to implement {@link com.potix.util.resource.Labels}.
 *
 * @author <a href="mailto:tomyeh@potix.com">tomyeh@potix.com</a>
 */
public class LabelLoader {

    private static final Log log = Log.lookup(LabelLoader.class);

    /** A map of (Locale l, Map(String key, String label)). */
    private final Map _labels = new HashMap(6);

    /** A list of LabelLocator. */
    private final List _locators = new LinkedList();

    /** The variable resolver. */
    private VariableResolver _resolv;

    /** Returns the label of the specified key, or null if not found.
	 */
    public String getLabel(String key) {
        final String label = getProperty(Locales.getCurrent(), key);
        if (label == null || label.length() == 0 || label.indexOf("${") < 0) return label;
        try {
            return (String) new EvaluatorImpl().evaluate(label, String.class, _resolv, null);
        } catch (Throwable ex) {
            log.error("Illegal label: key=" + key + " value=" + label, ex);
            return label;
        }
    }

    /** Sets the variable resolver, which is used if an EL expression
	 * is specified.
	 */
    public VariableResolver setVariableResolver(VariableResolver resolv) {
        final VariableResolver old = _resolv;
        _resolv = resolv;
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
        if (!locale.equals(Locale.getDefault())) {
            label = (String) getLabels(Locale.getDefault()).get(key);
            if (label != null) return label;
        }
        return "en".equals(lang) || "en".equals(Locale.getDefault().getLanguage()) ? null : (String) getLabels(Locale.ENGLISH).get(key);
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
        try {
            log.info("Loading labels for " + locale);
            final Map labels = new HashMap(617);
            final ClassLocator locator = new ClassLocator();
            for (Enumeration en = locator.getResources(getI3LabelPath(locale)); en.hasMoreElements(); ) {
                final URL url = (URL) en.nextElement();
                load(labels, url);
            }
            for (Iterator it = _locators.iterator(); it.hasNext(); ) {
                final URL url = ((LabelLocator) it.next()).locate(locale);
                if (url != null) load(labels, url);
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

    /** Returns the path of metainfo/i3-label.properties. */
    private static final String getI3LabelPath(Locale locale) {
        return locale.equals(Locale.ENGLISH) ? "metainfo/i3-label.properties" : "metainfo/i3-label_" + locale + ".properties";
    }

    /** Loads all labels from the specified URL. */
    private static final void load(Map labels, URL url) throws IOException {
        log.info(MCommon.FILE_OPENING, url);
        final Map news = new HashMap();
        Maps.load(news, url.openStream());
        for (Iterator it = news.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry me = (Map.Entry) it.next();
            final Object key = me.getKey();
            if (labels.put(key, me.getValue()) != null) log.warning("Label of " + key + " is replaced by " + url);
        }
    }
}
