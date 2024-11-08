package visugraph.plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import visugraph.data.Attribute;
import visugraph.data.AttributeListener;
import visugraph.data.DataUtils;
import visugraph.util.ReflectUtils;

/**
 * <p>Offre les méthodes pour analyser une classe annotée <code>UserPlugin</code>.
 * Cette classe permet donc d'accéder aux propriétés d'une classe et d'en extraire
 * des attributs qui pourront ensuite être utilisées avec les instances de la dite classe.</p>
 *
 * <p>Une propriété est un ensemble de une ou deux méthodes (accesseur et mutateur) qui sont
 * annotées <code>UserProperty</code>. Les 2 méthodes doivent avoir la même valeur d'annotation
 * (appelé titre) et retourner et prendre le même type d'argument. Voir UserProperty pour plus de détails.
 * Si la propriété n'a que le get(), elle est dit en lecture seule, si seul le set() est spécifié,
 * elle est en écriture seule.</p>
 *
 * <p>En plus d'être annoté UserProperty, une propriété peut être également annotée UserData.
 * Dans ce cas, le sous-type pourra être retourné. Cette annotation est facultative est
 * utile que dans le cas des Collections, Map ou Data.</p>
 *
 * @see UserProperty
 */
public class PluginsAnalyser<T> {

    private Class<? extends T> clAna;

    /**
	 * Créer un nouveau PluginsAnalyser à partir d'une classe.
	 * @throws IllegalArgumentException si la classe ne possède pas l'annotation UserPlugin.
	 */
    public PluginsAnalyser(Class<? extends T> clAna) {
        if (getUserPluginValue(clAna) != null) {
            this.clAna = clAna;
        } else {
            throw new IllegalArgumentException("L'objet ne possède pas l'annotation nécessaire pour être analysée.");
        }
    }

    /**
	 * Indique si une propriété existe avec le nom donné.
	 * @param name nom de la propriété à tester
	 * @return true si une propriété avec un tel nom existe
	 */
    public boolean isProperty(String name) {
        Method getter = ReflectUtils.findGetter(this.clAna, name);
        Method setter = ReflectUtils.findSetter(this.clAna, name);
        String getterPropName = getter != null ? getUserPropertyValue(getter) : null;
        String setterPropName = setter != null ? getUserPropertyValue(setter) : null;
        boolean result = getter == null && setter != null && setterPropName != null || setter == null && getter != null && getterPropName != null || setter != null && getter != null && setterPropName.equals(getterPropName);
        return result;
    }

    /**
	 * Indique si une propriété est accessible en lecture.
	 * @param name nom de la propriété à tester
	 * @return true si le get de la propriété existe.
	 * @throws NoSuchMethodException si la propriété n'existe pas.
	 */
    public boolean isReadable(String name) throws NoSuchMethodException {
        this.assertProperty(name);
        return ReflectUtils.findGetter(this.clAna, name) != null;
    }

    /**
	 * Indique si une propriété est accessible en écriture.
	 * @param name nom de la propriété à tester
	 * @return true si le set de la propriété existe.
	 * @throws NoSuchMethodException si la propriété n'existe pas.
	 */
    public boolean isWritable(String name) throws NoSuchMethodException {
        this.assertProperty(name);
        return ReflectUtils.findSetter(this.clAna, name) != null;
    }

    /**
	 * Retourne le type associé à une propriété.
	 * @param name nom de la propriété (nom JavaBean)
	 * @return le type de la propriété
	 * @throws NoSuchMethodException si la propriété demandée n'existe pas ou si une erreur s'est produite
	 */
    public Class<?> getPropertyType(String name) throws NoSuchMethodException {
        this.assertProperty(name);
        Method getter = ReflectUtils.findGetter(this.clAna, name);
        Method setter = ReflectUtils.findSetter(this.clAna, name);
        return getter != null ? getter.getReturnType() : setter.getParameterTypes()[0];
    }

    /**
	 * Retourne le titre de la propriété.
	 * Le titre d'une propriété correspond à la valeur de son annotation UserProperty.
	 * @param name nom de la propriété dont on souhaite le titre
	 * @return le titre de la propriété
	 * @throws NoSuchMethodException si la propriété demandée n'existe pas.
	 */
    public String getPropertyTitle(String name) throws NoSuchMethodException {
        this.assertProperty(name);
        return getUserPropertyValue(ReflectUtils.findGetter(this.clAna, name));
    }

    /**
	 * Extrait toutes les propriétés accessibles en lecture et en écriture
	 * sous-forme d'attributs.
	 */
    public Set<Attribute<T, Object>> extractAttributes() {
        return this.extractAttributes(true, true);
    }

    /**
	 * Extrait un attribut particulier de la classe.
	 * L'attribut doit être accessible en lecture et en écriture.
	 * @param name le nom de l'attribut qui correspond à son annotation {@link UserPlugin}
	 * @throws IllegalArgumentException si aucun attribut du type et du nom donné n'a été trouvé
	 */
    public <V> Attribute<T, V> extractAttribute(String name, Class<V> type) {
        Method[] allMethods = this.clAna.getMethods();
        for (Method oneMethod : allMethods) {
            String userProp = getUserPropertyValue(oneMethod);
            if (!name.equals(userProp)) {
                continue;
            }
            Attribute<T, Object> bridge = this.createAttribute(oneMethod, true, true);
            return DataUtils.cast(bridge, type);
        }
        throw new IllegalArgumentException("Aucun attribut du nom et du type donné n'a été trouvé dans cette classe");
    }

    /**
	 * Extrait l'ensemble des propriétés sous-forme d'attributs.
	 * @param read retourne les propriétés accessibles en lecture.
	 * @param write retourne les propriétés accessibles en écriture.
	 */
    public Set<Attribute<T, Object>> extractAttributes(boolean read, boolean write) {
        Set<String> propertyNames = new LinkedHashSet<String>();
        Set<Attribute<T, Object>> attributes = new LinkedHashSet<Attribute<T, Object>>();
        Method[] allMethods = this.clAna.getMethods();
        for (Method oneMethod : allMethods) {
            String propertyName = ReflectUtils.extractPropertyName(oneMethod);
            if (propertyName == null) {
                continue;
            }
            if (propertyNames.contains(propertyName)) {
                continue;
            }
            try {
                Attribute<T, Object> bridge = this.createAttribute(oneMethod, read, write);
                if (bridge != null) {
                    attributes.add(bridge);
                    propertyNames.add(propertyName);
                }
            } catch (Exception e) {
                System.err.println("Erreur lors du chargement de la propriété " + propertyName + " du plugin " + getUserPluginValue(this.clAna));
            }
        }
        return attributes;
    }

    private Attribute<T, Object> createAttribute(Method oneMethod, boolean read, boolean write) {
        String propertyName = ReflectUtils.extractPropertyName(oneMethod);
        boolean isGetter = ReflectUtils.isGetter(oneMethod);
        boolean isSetter = ReflectUtils.isSetter(oneMethod);
        if (!isGetter && !isSetter) {
            return null;
        }
        Method getter = isGetter ? oneMethod : ReflectUtils.findGetter(this.clAna, propertyName);
        Method setter = isSetter ? oneMethod : ReflectUtils.findSetter(this.clAna, propertyName);
        String getterPropName = getUserPropertyValue(getter);
        String setterPropName = getUserPropertyValue(setter);
        boolean unmatch = read && getter == null || write && setter == null || getter != null && getterPropName == null || setter != null && setterPropName == null || setter != null && getter != null && !getterPropName.equals(setterPropName);
        if (unmatch) {
            return null;
        }
        return new PropertyBridge(getter, setter);
    }

    /**
	 * Certifie qu'une propriété existe.
	 * @throws NoSuchMethodException si la propriété n'existe pas.
	 */
    private void assertProperty(String propertyName) throws NoSuchMethodException {
        if (!this.isProperty(propertyName)) {
            throw new NoSuchMethodException("Aucun paramêtre ne correspond au nom donné");
        }
    }

    /**
	 * Retourne la valeur de l'annotation UserProperty de la méthode fournie ou null
	 * si méthode non annotée.
	 */
    public static String getUserPropertyValue(Method method) {
        return getUserPropertyValue(method, null);
    }

    /**
	 * Retourne la valeur de l'annotation UserProperty de la méthode fournie ou <code>replacement</code>
	 * si méthode non annotée.
	 */
    public static String getUserPropertyValue(Method method, String replacement) {
        if (method == null) {
            return replacement;
        }
        UserProperty anno = method.getAnnotation(UserProperty.class);
        if (anno != null) {
            return anno.value();
        } else {
            return replacement;
        }
    }

    /**
	 * Retourne la valeur (value()) de l'annotation UserPlugin appliquée sur la classe cl.
	 * Si aucune annotation de ce type n'existe sur cl, retourne null.
	 */
    public static String getUserPluginValue(Class<?> cl) {
        return getUserPluginValue(cl, null);
    }

    /**
	 * Retourne la valeur (value()) de l'annotation UserPlugin appliquée su la classe cl.
	 * Si aucune annotation n'existe retourne <code>replacement</code>.
	 */
    public static String getUserPluginValue(Class<?> cl, String replacement) {
        UserPlugin anno = cl.getAnnotation(UserPlugin.class);
        if (anno != null) {
            return anno.value();
        } else {
            return replacement;
        }
    }

    /**
	 * Classe qui fait le lien entre une propriété et l'interface Attribute.
	 */
    private class PropertyBridge implements Attribute<T, Object> {

        private final Method getter;

        private final Method setter;

        private final List<AttributeListener> listeners;

        public PropertyBridge(Method getter, Method setter) {
            this.getter = getter;
            this.setter = setter;
            this.listeners = new ArrayList<AttributeListener>();
        }

        public String getName() {
            return getUserPropertyValue(this.getter != null ? this.getter : this.setter);
        }

        public Class<?> getType() {
            Class<?> type = this.getter != null ? this.getter.getReturnType() : this.setter.getParameterTypes()[0];
            if (ReflectUtils.isBaseType(type)) {
                return ReflectUtils.getWrapperType(type);
            } else {
                return type;
            }
        }

        public Object get(T elem) {
            if (this.getter == null) {
                throw new UnsupportedOperationException("L'accesseur de cet attribut n'est pas accessible");
            }
            try {
                return this.getter.invoke(elem);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Une erreur inattendue s'est produite");
            } catch (InvocationTargetException e) {
                throw new IllegalArgumentException("Une erreur inattendue s'est produite");
            }
        }

        public void set(T elem, Object value) {
            if (this.setter == null) {
                throw new UnsupportedOperationException("Le mutateur de cet attribut n'est pas accessible");
            }
            try {
                this.setter.invoke(elem, value);
                for (AttributeListener listener : this.listeners) {
                    listener.attributeChanged(this, elem);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Une erreur inattendue s'est produite");
            } catch (InvocationTargetException e) {
                throw new IllegalArgumentException(e.getCause().getMessage());
            }
        }

        public void addAttributeListener(AttributeListener listener) {
            this.listeners.add(listener);
        }

        public void removeAttributeListener(AttributeListener listener) {
            this.listeners.remove(listener);
        }
    }
}
