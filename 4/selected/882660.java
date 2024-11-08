package ch.olsen.products.util.serialize;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import ch.olsen.products.util.serialize.Reflector.Serializer;

public class GenericClass implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final GenericField NOFIELDS[] = new GenericField[0];

    protected String name;

    protected GenericClass superClass;

    protected boolean isPrimitive;

    protected boolean isArray;

    protected boolean isEnum;

    protected boolean isCollection;

    protected boolean isMap;

    protected boolean isClass;

    protected boolean hasSpecializedSerialization;

    protected boolean hasWriteReplace;

    protected boolean hasReadResolve;

    protected GenericField fields[] = NOFIELDS;

    protected int declaredFieldCount;

    protected int fieldCount;

    public GenericClass() {
    }

    public GenericClass(Serializer serializer, Class orig) {
        initialize(serializer, orig);
    }

    public void initialize(Serializer serializer, Class orig) {
        if (orig == Date.class) System.out.print("");
        if (orig.isArray()) {
            this.isArray = true;
            orig = orig.getComponentType();
        } else if (orig == Class.class) {
            isClass = true;
        } else if (Collection.class.isAssignableFrom(orig)) {
            this.isCollection = true;
        } else if (Map.class.isAssignableFrom(orig)) {
            this.isMap = true;
        } else {
            try {
                if (orig.getDeclaredMethod("readObject", java.io.ObjectInputStream.class) != null && orig.getDeclaredMethod("writeObject", java.io.ObjectOutputStream.class) != null) {
                    this.hasSpecializedSerialization = true;
                }
            } catch (Exception e) {
            }
            try {
                if (orig.getDeclaredMethod("writeReplace") != null) {
                    this.hasWriteReplace = true;
                }
            } catch (Exception e) {
            }
            try {
                if (orig.getDeclaredMethod("readResolve") != null) {
                    this.hasReadResolve = true;
                }
            } catch (Exception e) {
            }
        }
        this.name = orig.getName();
        if (!isArray && !isCollection && !isMap && !hasSpecializedSerialization && !isClass && orig.getSuperclass() != null) this.superClass = serializer.getGenericClass(orig.getSuperclass());
        this.isPrimitive = orig.isPrimitive() || orig.equals(String.class);
        this.isEnum = orig.isEnum();
        List<GenericField> fields = new LinkedList<GenericField>();
        int offset = superClass != null ? superClass.getFieldCount() : 0;
        if (!isPrimitive && !isMap && !isCollection && !isClass && !isArray && !hasSpecializedSerialization) {
            int n = 0;
            for (Field f : orig.getDeclaredFields()) {
                if ((f.getModifiers() & (Modifier.TRANSIENT | Modifier.STATIC)) > 0) continue;
                f.setAccessible(true);
                fields.add(new GenericField(serializer, f, n + offset));
                n++;
            }
        }
        this.fields = fields.toArray(new GenericField[0]);
        this.declaredFieldCount = isPrimitive ? 0 : this.fields.length;
        this.fieldCount = this.declaredFieldCount + (superClass != null ? superClass.getFieldCount() : 0);
    }

    public String getName() {
        return name;
    }

    public int getFieldCount() {
        return fieldCount;
    }

    public boolean isPrimitive() {
        return isPrimitive;
    }

    public boolean isArray() {
        return isArray;
    }

    public boolean isEnum() {
        return isEnum;
    }

    public GenericClass getSuperClass() {
        return superClass;
    }

    public GenericField[] getDeclaredFields() {
        return fields;
    }

    public String toString() {
        return "(GC) " + (isArray ? "(Array) " : "") + name;
    }

    public GenericField getField(String name) {
        for (GenericField f : fields) if (f.name.equals(name)) return f;
        return null;
    }
}
