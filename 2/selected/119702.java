package cx.ath.mancel01.dependencyshot.spi;

import cx.ath.mancel01.dependencyshot.exceptions.ExceptionManager;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ServiceLoader implementation for old versions of java.
 *
 * @author Mathieu ANCELIN
 */
public class DSServiceLoader<T extends Object> implements Iterable<T> {

    private static final Logger logger = Logger.getLogger(DSServiceLoader.class.getSimpleName());

    private Collection<T> iterables;

    private Class<T> loadedClass;

    private DSServiceLoader(Class<T> clazz, Collection<T> iterables) {
        this.iterables = iterables;
        this.loadedClass = clazz;
    }

    public static <T> DSServiceLoader<T> load(Class<T> interf) {
        try {
            return new DSServiceLoader(interf, loadFromServices(interf));
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            throw ExceptionManager.makeException("Error while loading services : ", ex).get();
        }
    }

    private static <T> Collection<T> loadFromServices(Class<T> interf) throws Exception {
        ClassLoader classLoader = DSServiceLoader.class.getClassLoader();
        Enumeration<URL> e = classLoader.getResources("META-INF/services/" + interf.getName());
        Collection<T> services = new ArrayList<T>();
        while (e.hasMoreElements()) {
            URL url = e.nextElement();
            InputStream is = url.openStream();
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                while (true) {
                    String line = r.readLine();
                    if (line == null) {
                        break;
                    }
                    int comment = line.indexOf('#');
                    if (comment >= 0) {
                        line = line.substring(0, comment);
                    }
                    String name = line.trim();
                    if (name.length() == 0) {
                        continue;
                    }
                    Class<?> clz = Class.forName(name, true, classLoader);
                    Class<? extends T> impl = clz.asSubclass(interf);
                    Constructor<? extends T> ctor = impl.getConstructor();
                    T svc = ctor.newInstance();
                    services.add(svc);
                }
            } finally {
                is.close();
            }
        }
        return services;
    }

    public void reload() {
        try {
            this.iterables = loadFromServices(loadedClass);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public Iterator<T> iterator() {
        return this.iterables.iterator();
    }
}
