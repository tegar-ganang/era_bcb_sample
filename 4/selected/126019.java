package pl.edu.agh.ssm.component.mc.utils;

import java.lang.reflect.Method;

/**
 * Getter with setter represents the attribute in some class, In this class
 * there are references to two methods: getter and setter, moreover it have
 * informations about returning type, and name of the attribute. The 'is' getter
 * is recognised by returning type java.lang.Boolean or boolean
 * 
 * @author SSM-Team
 * 
 */
public class GetterWithSetter {

    private String name = null;

    private Method getter = null;

    private Method setter = null;

    private Class<?> type = null;

    /**
	 * @return type of attribute (returning by getter, taken by setter)
	 */
    public Class<?> getType() {
        return type;
    }

    public GetterWithSetter(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    /**
	 * connects another getterwithsetter object: if this object represents only
	 * getter and connected one is setter after this method this object will
	 * represents both of them (and per analogium setter + getter ->
	 * getterwithsetter)
	 * 
	 * @param gws
	 */
    public void connectGetterWithSetter(GetterWithSetter gws) {
        if (!isSetter() && gws.isSetter()) {
            setSetter(gws.getSetter());
        }
        if (!isGetter() && gws.isGetter()) {
            setGetter(gws.getGetter());
        }
    }

    /**
	 * Returns if this getter is 'is' getter (returns Boolean or boolean)
	 * 
	 * @return
	 */
    public boolean isIs() {
        return type == Boolean.class || type == boolean.class;
    }

    public boolean isGetter() {
        return getter != null;
    }

    public void setGetter(Method getter) {
        this.getter = getter;
    }

    public boolean isSetter() {
        return setter != null;
    }

    public void setSetter(Method setter) {
        this.setter = setter;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return "attribute: " + name + (isGetter() ? " read" : "") + (isSetter() ? " write" : "") + " type: " + type.getSimpleName();
    }

    /**
	 * to getterWithSetters are equal only when both attributes name and type
	 * are equal
	 */
    public boolean equals(Object o) {
        if (o instanceof GetterWithSetter) {
            GetterWithSetter gws = (GetterWithSetter) o;
            return gws.name.equals(name) && gws.type.equals(type);
        }
        return false;
    }

    public Method getGetter() {
        return getter;
    }

    public Method getSetter() {
        return setter;
    }
}
