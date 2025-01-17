package org.pushingpixels.lafwidget;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import javax.swing.JComponent;

/**
 * Repository of LAF widgets.
 * 
 * @author Kirill Grouchnikov
 */
public class LafWidgetRepository {

    /**
	 * All registered widgets. Key is {@link Class} in the UI component
	 * hierarchy, value is a {@link Set} of fully-qualified widget class names.
	 */
    protected Map<Class<?>, Set<LafWidgetClassInfo>> widgets;

    /**
	 * Contains fully qualified class names of widgets that should not be
	 * installed on any components.
	 */
    protected Set<String> widgetClassesToIgnore;

    /**
	 * Currently registered LAF support.
	 */
    protected LafWidgetSupport lafSupport;

    /**
	 * Indicates whether the currently registered LAF support is custom (not
	 * {@link LafWidgetSupport}).
	 */
    protected boolean isCustomLafSupportSet;

    /**
	 * Singleton instance.
	 */
    protected static LafWidgetRepository repository;

    /**
	 * Resource bundle for <b>Substance</b> labels.
	 */
    private static ResourceBundle LABEL_BUNDLE = null;

    /**
	 * Class loader for the {@link #LABEL_BUNDLE}.
	 * 
	 * @since version 1.1
	 */
    private static ClassLoader labelBundleClassLoader;

    /**
	 * Information on a single class.
	 * 
	 * @author Kirill Grouchnikov
	 */
    protected static class LafWidgetClassInfo {

        /**
		 * Class name.
		 */
        public String className;

        /**
		 * Indicates whether the matching should be exact.
		 */
        public boolean isExact;

        /**
		 * Creates a new info object.
		 * 
		 * @param className
		 *            Class name.
		 * @param isExact
		 *            Indicates whether the matching should be exact.
		 */
        public LafWidgetClassInfo(String className, boolean isExact) {
            this.className = className;
            this.isExact = isExact;
        }
    }

    /**
	 * Creates a new repository. Marked private to enforce single instance.
	 */
    private LafWidgetRepository() {
        this.widgets = new HashMap<Class<?>, Set<LafWidgetClassInfo>>();
        this.lafSupport = new LafWidgetSupport();
        this.isCustomLafSupportSet = false;
        this.widgetClassesToIgnore = new HashSet<String>();
    }

    /**
	 * Returns the widget repository.
	 * 
	 * @return Widget repository.
	 */
    public static synchronized LafWidgetRepository getRepository() {
        if (LafWidgetRepository.repository == null) {
            LafWidgetRepository.repository = new LafWidgetRepository();
            LafWidgetRepository.repository.populate();
        }
        return LafWidgetRepository.repository;
    }

    /**
	 * Populates the repository from the specified URL. The URL should point to
	 * a properties file, the key being the fully-qualified class name of the
	 * widget implementation, the value being semicolon-separated
	 * fully-qualified class names of classes in UI component hierarchy. Sample
	 * property file:
	 * 
	 * <pre>
	 * org.pushingpixels.lafwidget.text.PasswordStrengthCheckerWidget = javax.swing.JPasswordField
	 *             org.pushingpixels.lafwidget.text.LockBorderWidget = javax.swing.text.JTextComponent;javax.swing.JComboBox
	 * </pre>
	 * 
	 * @param url
	 *            URL that points to a properties file.
	 */
    protected void populateFrom(URL url) {
        Properties props = new Properties();
        InputStream is = null;
        try {
            is = url.openStream();
            props.load(is);
            Enumeration<?> names = props.propertyNames();
            while (names.hasMoreElements()) {
                String name = (String) names.nextElement();
                String value = props.getProperty(name);
                String[] values = value.split(";");
                for (int i = 0; i < values.length; i++) {
                    String className = values[i].trim();
                    boolean isExact = className.startsWith("%");
                    if (isExact) className = className.substring(1);
                    try {
                        this.registerWidget(name, Class.forName(className), isExact);
                    } catch (ClassNotFoundException cnfe) {
                    }
                }
            }
        } catch (IOException ioe) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {
                }
            }
        }
    }

    /**
	 * Populates the widget repository. The classpath is scanned for all
	 * resources that match the name <code>META-INF/lafwidget.properties</code>.
	 * 
	 * @see #populateFrom(URL)
	 */
    public void populate() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Enumeration<?> rs = cl.getResources("META-INF/lafwidget.properties");
            while (rs.hasMoreElements()) {
                URL rUrl = (URL) rs.nextElement();
                this.populateFrom(rUrl);
            }
        } catch (IOException ioe) {
        }
    }

    /**
	 * Registers a new widget for the specified UI classes. The list should
	 * contain {@link Class} instances.
	 * 
	 * @param widgetClassName
	 *            Full-qualified class name for the widget.
	 * @param supportedClasses
	 *            Classes supported by the widget.
	 */
    public synchronized void registerWidget(String widgetClassName, List<Class<?>> supportedClasses) {
        for (Class<?> clazz : supportedClasses) this.registerWidget(widgetClassName, clazz, false);
    }

    /**
	 * Registers a new widget for the specified UI class.
	 * 
	 * @param widgetClassName
	 *            Full-qualified class name for the widget.
	 * @param supportedClass
	 *            Class supported by the widget.
	 * @param isExact
	 *            if <code>true</code>, the widget will be available only for
	 *            the components of the specified class. If <code>false</code>,
	 *            the widget be available for the components of the specified
	 *            class and all its descendants (as defined in the
	 *            {@link Class#isAssignableFrom(Class)} ).
	 */
    public synchronized void registerWidget(String widgetClassName, Class<?> supportedClass, boolean isExact) {
        if (JComponent.class.isAssignableFrom(supportedClass)) {
            if (!this.widgets.containsKey(supportedClass)) this.widgets.put(supportedClass, new HashSet<LafWidgetClassInfo>());
        }
        for (LafWidgetClassInfo registered : this.widgets.get(supportedClass)) {
            if (registered.className.equals(widgetClassName)) return;
        }
        this.widgets.get(supportedClass).add(new LafWidgetClassInfo(widgetClassName, isExact));
    }

    /**
	 * Returns a set of widgets that match the specified component. The
	 * component hierarchy is scanned bottom-up and all matching widget classes
	 * are used to instantiate new instance of widgets. In case the
	 * {@link #isCustomLafSupportSet} is <code>false</code>, only widgets that
	 * return <code>false</code> in {@link LafWidget#requiresCustomLafSupport()}
	 * are returned.
	 * 
	 * 
	 * @param jcomp
	 *            UI component.
	 * @return Set of widgets that match the specified component.
	 */
    public synchronized Set<LafWidget> getMatchingWidgets(JComponent jcomp) {
        Set<LafWidget> result = new HashSet<LafWidget>();
        Class<?> clazz = jcomp.getClass();
        boolean isOriginator = true;
        while (clazz != null) {
            Set<LafWidgetClassInfo> registered = this.widgets.get(clazz);
            if (registered != null) {
                for (Iterator<LafWidgetClassInfo> it = registered.iterator(); it.hasNext(); ) {
                    LafWidgetClassInfo widgetClassInfo = it.next();
                    if (widgetClassInfo.isExact && !isOriginator) continue;
                    try {
                        String widgetClassName = widgetClassInfo.className;
                        if (this.widgetClassesToIgnore.contains(widgetClassName)) continue;
                        Object widgetObj = Class.forName(widgetClassName).newInstance();
                        if (widgetObj instanceof LafWidget) {
                            LafWidget widget = (LafWidget) widgetObj;
                            if (!widget.requiresCustomLafSupport() || this.isCustomLafSupportSet) {
                                widget.setComponent(jcomp);
                                result.add(widget);
                            }
                        }
                    } catch (InstantiationException ie) {
                    } catch (IllegalAccessException iae) {
                    } catch (ClassNotFoundException cnfe) {
                    }
                }
            }
            clazz = clazz.getSuperclass();
            isOriginator = false;
        }
        return result;
    }

    /**
	 * Sets LAF support.
	 * 
	 * @param lafSupport
	 *            LAF support.
	 * @throws IllegalArgumentException
	 *             If the LAF support is <code>null</code>.
	 */
    public void setLafSupport(LafWidgetSupport lafSupport) {
        if (lafSupport == null) throw new IllegalArgumentException("LAF support can't be null");
        this.lafSupport = lafSupport;
        this.isCustomLafSupportSet = (this.lafSupport.getClass() != LafWidgetSupport.class);
    }

    /**
	 * Unsets custom LAF support and reverts to the base LAF support.
	 */
    public void unsetLafSupport() {
        this.lafSupport = new LafWidgetSupport();
        this.isCustomLafSupportSet = false;
    }

    /**
	 * Returns the currently set LAF support. The result is guaranteed to be
	 * non-<code>null</code>.
	 * 
	 * @return Currently set non-<code>null</code> LAF support.
	 */
    public LafWidgetSupport getLafSupport() {
        return this.lafSupport;
    }

    /**
	 * Retrieves the current label bundle.
	 * 
	 * @return The current label bundle.
	 * @see #resetLabelBundle()
	 */
    public static synchronized ResourceBundle getLabelBundle() {
        if (LafWidgetRepository.labelBundleClassLoader == null) {
            LafWidgetRepository.LABEL_BUNDLE = ResourceBundle.getBundle("org.pushingpixels.lafwidget.resources.Labels", Locale.getDefault());
        } else {
            LafWidgetRepository.LABEL_BUNDLE = ResourceBundle.getBundle("org.pushingpixels.lafwidget.resources.Labels", Locale.getDefault(), LafWidgetRepository.labelBundleClassLoader);
        }
        return LafWidgetRepository.LABEL_BUNDLE;
    }

    /**
	 * Retrieves the label bundle for the specified locale.
	 * 
	 * @param locale
	 *            Locale.
	 * @return The label bundle for the specified locale.
	 */
    public static synchronized ResourceBundle getLabelBundle(Locale locale) {
        if (LafWidgetRepository.labelBundleClassLoader == null) {
            return ResourceBundle.getBundle("org.pushingpixels.lafwidget.resources.Labels", locale);
        } else {
            return ResourceBundle.getBundle("org.pushingpixels.lafwidget.resources.Labels", locale, LafWidgetRepository.labelBundleClassLoader);
        }
    }

    /**
	 * Resets the current label bundle. Useful when the application changes
	 * Locale at runtime.
	 * 
	 * @see #getLabelBundle()
	 */
    public static synchronized void resetLabelBundle() {
        LafWidgetRepository.LABEL_BUNDLE = null;
    }

    /**
	 * Sets the class loader for {@link #LABEL_BUNDLE}.
	 * 
	 * @param labelBundleClassLoader
	 *            Class loader for {@link #LABEL_BUNDLE}.
	 * @since version 1.1
	 */
    public static void setLabelBundleClassLoader(ClassLoader labelBundleClassLoader) {
        LafWidgetRepository.labelBundleClassLoader = labelBundleClassLoader;
    }

    /**
	 * Marks widget with the specified class name to never be installed on any
	 * components.
	 * 
	 * @param widgetClassName
	 *            Fully qualified widget class name.
	 */
    public synchronized void addToIgnoreWidgets(String widgetClassName) {
        this.widgetClassesToIgnore.add(widgetClassName);
    }
}
