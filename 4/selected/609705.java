package org.regadou.nalasys.system;

import org.regadou.nalasys.*;
import org.regadou.nalasys.numeric.*;
import java.net.URLDecoder;
import java.util.*;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.awt.Color;

public class Types {

    public static final int HTML_ESCAPE = 1, UNICODE_ESCAPE = 2, ASCII_ESCAPE = 3, URL_ESCAPE = 4, SQL_ESCAPE = 5;

    public static final String[] ESCAPES = { "", "html", "unicode", "ascii", "url", "sql" };

    public static final int GETTER = 0, SETTER = 1;

    public static final int GROUP_START_OFFSET = 1;

    private static Map CONSTRUCTORS = new HashMap();

    private static Map CONVERTERS = new HashMap();

    private static Map CLASSES = new LinkedHashMap();

    private static SimpleDateFormat[] DATE_FORMATS = null;

    private Types() {
    }

    /*********************** Type conversion management *******************************/
    static {
        Class[] classes = { CharSequence.class, String.class, Number.class, Boolean.class, Collection.class, Map.class, Stream.class, Date.class, Color.class };
        for (int i = 0; i < classes.length; i++) {
            Class cl = classes[i];
            String[] parts = cl.getName().split("\\.");
            String method = "to" + parts[parts.length - 1];
            try {
                registerConverter(cl, Types.class.getMethod(method, new Class[] { Object.class }));
            } catch (Exception e) {
            }
        }
        classes = Stream.getStreamables();
        for (int i = 0; i < classes.length; i++) {
            Class cl = classes[i];
            try {
                registerConstructor(cl, Stream.class, Stream.class.getConstructor(new Class[] { Object.class }));
            } catch (Exception e) {
            }
        }
    }

    public static boolean registerConstructor(Class src, Class dst, Constructor cons) {
        if (dst == null) {
            Service.debug("Cannot have a constructor of null");
            return false;
        }
        Map clst = (Map) CONSTRUCTORS.get(dst);
        if (clst == null) {
            clst = new HashMap();
            CONSTRUCTORS.put(dst, clst);
        }
        if (cons == null) clst.remove(src); else clst.put(src, cons);
        Service.debug(((src == null) ? "null" : src.getName()) + " can be converted to " + dst.getName());
        return true;
    }

    public static boolean registerConverter(Class type, Method m) {
        return registerConverter(type, m, false);
    }

    public static boolean registerConverter(Class type, Method m, boolean overwrite) {
        if (type == null || type.equals(Object.class) || m == null || m.getParameterTypes().length != 1 || !m.getParameterTypes()[0].equals(Object.class)) {
            Service.debug(m + " cannot be used to convert to " + type);
            return false;
        }
        if (!overwrite && CONVERTERS.get(type) != null) {
            Service.debug(type + " already have a converter: " + toString(m));
            return false;
        }
        CONVERTERS.put(type, m);
        Service.debug("Converter " + toString(m) + " has been registered for convertion to " + type.getName());
        return true;
    }

    public static Object convert(Object obj, Class type) {
        try {
            if (type == null || type.equals(Object.class) || (obj != null && type.isAssignableFrom(obj.getClass()))) return obj; else if (type.isArray()) {
                if (type.getComponentType() == java.lang.Character.TYPE) return toString(obj).toCharArray(); else return toArray(obj, type.getComponentType());
            } else {
                try {
                    Constructor cons = getClassConstructor(type, obj);
                    if (cons != null) {
                        return cons.newInstance(new Object[] { obj });
                    }
                } catch (Exception e) {
                    Service.debug("Converting " + obj + " to " + type.getName() + " failed: " + e);
                }
                Method m = getClassConverter(type);
                if (m != null) return m.invoke(null, new Object[] { obj });
                if (obj == null) {
                    Constructor cons = type.getConstructor(new Class[] {});
                    if (cons != null) {
                        try {
                            Object dst = cons.newInstance(new Object[] {});
                            registerConstructor(null, type, cons);
                            return dst;
                        } catch (Exception e) {
                        }
                    }
                    cons = type.getConstructor(new Class[] { Object.class });
                    if (cons != null) {
                        try {
                            Object dst = cons.newInstance(new Object[] { null });
                            registerConstructor(Object.class, type, cons);
                            return dst;
                        } catch (Exception e) {
                        }
                    }
                } else {
                    Constructor objectCons = null;
                    for (Class parent = obj.getClass(); parent != null; parent = parent.getSuperclass()) {
                        Constructor cons = type.getConstructor(new Class[] { parent });
                        if (cons != null) {
                            if (parent.equals(Object.class)) {
                                objectCons = cons;
                                continue;
                            }
                            try {
                                Object dst = cons.newInstance(new Object[] { obj });
                                registerConstructor(parent, type, cons);
                                return dst;
                            } catch (Exception e) {
                            }
                        }
                    }
                    Class[] ifaces = type.getInterfaces();
                    for (int i = 0; i < ifaces.length; i++) {
                        Constructor cons = type.getConstructor(new Class[] { ifaces[i] });
                        if (cons != null) {
                            try {
                                Object dst = cons.newInstance(new Object[] { obj });
                                registerConstructor(ifaces[i], type, cons);
                                return dst;
                            } catch (Exception e) {
                            }
                        }
                    }
                    if (objectCons != null) {
                        try {
                            Object dst = objectCons.newInstance(new Object[] { obj });
                            registerConstructor(Object.class, type, objectCons);
                            return dst;
                        } catch (Exception e) {
                        }
                    }
                }
                Service.debug("Cannot find a converter from " + obj.getClass() + " to " + type.getName());
                return null;
            }
        } catch (Exception e) {
            Service.debug("Error while trying converting " + obj + " to " + type.getName() + ": " + e);
            return null;
        }
    }

    /*********************** Type properties management *******************************/
    public static Map getClassProperties(Class cl) {
        Map props = (Map) CLASSES.get(cl);
        if (props == null) {
            props = new LinkedHashMap();
            CLASSES.put(cl, props);
            Method[] methods = cl.getMethods();
            for (int m = 0; m < methods.length; m++) {
                Method method = methods[m];
                int mod = method.getModifiers();
                if (!Modifier.isPublic(mod) || Modifier.isStatic(mod)) continue;
                String name = method.getName();
                switch(method.getParameterTypes().length) {
                    case 0:
                        if (name.startsWith("get") && Character.isUpperCase(name.charAt(3))) addProperty(props, name.substring(3), method, GETTER); else if (name.startsWith("is") && Character.isUpperCase(name.charAt(2))) addProperty(props, name.substring(2), method, GETTER);
                        break;
                    case 1:
                        if (name.startsWith("set") && Character.isUpperCase(name.charAt(3))) addProperty(props, name.substring(3), method, SETTER); else if (name.startsWith("put") && Character.isUpperCase(name.charAt(3))) addProperty(props, name.substring(3), method, SETTER);
                }
            }
        }
        return props;
    }

    public static Object getProperty(Object obj, Object prop) {
        try {
            if (obj instanceof Data) return ((Data) obj).getProperty(DataFactory.getInstance(prop)); else if (obj == null || prop == null) return null; else if (prop.getClass().isArray() || (prop instanceof Collection)) {
                Object[] plst = toArray(prop);
                Map dst = new LinkedHashMap();
                for (int i = 0; i < plst.length; i++) {
                    prop = plst[i];
                    Object val = getProperty(obj, prop);
                    if (val != null) dst.put(prop, val);
                }
                return dst.isEmpty() ? null : dst;
            } else if (obj instanceof Map) return ((Map) obj).get(prop); else if (prop instanceof Number) {
                int n = ((Number) prop).intValue() - GROUP_START_OFFSET;
                if (n < 0) return null;
                if (obj instanceof List) return ((List) obj).get(n); else if (obj.getClass().isArray()) return Array.get(obj, n); else if (obj instanceof Collection) {
                    Iterator iter = ((Collection) obj).iterator();
                    Object val = null;
                    int i = 0;
                    while (iter.hasNext() && i < n) {
                        iter.next();
                        i++;
                    }
                    return iter.next();
                } else if (obj instanceof CharSequence) {
                    return obj.toString().substring(n, n + 1);
                }
            } else if (prop.equals("length") || prop.equals("size")) {
                if (obj instanceof Collection) return new Integer(((Collection) obj).size()); else if (obj.getClass().isArray()) return new Integer(Array.getLength(obj)); else if (obj instanceof CharSequence) return new Integer(obj.toString().length());
            }
            Map props = (Map) getClassProperties(obj.getClass());
            return ((Method[]) props.get(prop))[GETTER].invoke(obj, null);
        } catch (Exception e) {
        }
        return null;
    }

    public static boolean setProperty(Object obj, Object prop, Object val) {
        try {
            if (obj instanceof Data) {
                ((Data) obj).setProperty(DataFactory.getInstance(prop), DataFactory.getInstance(val));
                return true;
            } else if (obj == null) return false; else if (prop == null) {
                if (val instanceof Map) {
                    Iterator entries = ((Map) prop).entrySet().iterator();
                    while (entries.hasNext()) {
                        Map.Entry entry = (Map.Entry) entries.next();
                        Types.setProperty(obj, entry.getKey(), entry.getValue());
                    }
                    return true;
                } else return false;
            } else if (prop.getClass().isArray() || (prop instanceof Collection)) {
                Object[] props = toArray(prop);
                Object[] vals = toArray(val);
                if (vals.length < props.length) {
                    if (vals.length > 1) {
                        Object[] old = vals;
                        vals = new Object[props.length];
                        System.arraycopy(old, 0, vals, 0, old.length);
                    } else vals = null;
                }
                boolean result = false;
                for (int i = 0; i < props.length; i++) {
                    prop = props[i];
                    Object value = (vals == null) ? val : vals[i];
                    if (prop == null) continue;
                    if (setProperty(obj, prop, value)) result = true;
                }
                return result;
            } else if (obj instanceof Map) {
                ((Map) obj).put(prop, val);
                return true;
            } else if (prop instanceof Number) {
                int pos = ((Number) prop).intValue() - GROUP_START_OFFSET;
                if (obj instanceof List) {
                    List lst = (List) obj;
                    if (pos < 0) lst.add(val); else {
                        while (pos >= lst.size()) lst.add(null);
                        lst.set(pos, val);
                    }
                    return true;
                } else if (obj.getClass().isArray()) {
                    Array.set(obj, pos, val);
                    return true;
                }
            }
            Map props = (Map) getClassProperties(obj.getClass());
            ((Method[]) props.get(prop))[SETTER].invoke(obj, new Object[] { val });
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    public static boolean copy(Object src, Object dst) {
        if (src == null || dst == null) return false;
        try {
            Stream s0 = new Stream(src);
            Stream s1 = new Stream(dst);
            try {
                s1.write(s0.read());
                return true;
            } catch (Exception e) {
                return false;
            }
        } catch (Exception e) {
        }
        if ((dst instanceof Collection) || dst.getClass().isArray()) {
            if ((src instanceof Collection) || src.getClass().isArray()) {
                Object[] a1 = toArray(src);
                Object[] a2 = toArray(dst);
                System.arraycopy(a1, 0, a2, 0, (a1.length < a2.length) ? a1.length : a2.length);
                return true;
            }
        }
        Map m0 = toMap(src);
        Map m1 = toMap(dst);
        Iterator iter = m0.entrySet().iterator();
        int copied = 0;
        while (iter.hasNext()) {
            try {
                Map.Entry entry = (Map.Entry) iter.next();
                m1.put(entry.getKey(), entry.getValue());
                copied++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return copied > 0;
    }

    public static boolean isNull(Object src) {
        if (src == null || src.toString().trim().equals("")) return true; else if (src instanceof Map) return ((Map) src).isEmpty(); else if (src instanceof Collection) return ((Collection) src).isEmpty(); else if (src.getClass().isArray()) return Array.getLength(src) == 0; else return false;
    }

    public static String toCharSequence(Object src) {
        return toString(src);
    }

    public static String toString(Object src) {
        try {
            if (src == null) return ""; else if (src instanceof CharSequence) return src.toString(); else if (src instanceof char[]) return new String((char[]) src); else if (src instanceof Date) {
                Date dt = (Date) src;
                return getDateFormats()[0].format(dt);
            } else if (src instanceof Map) {
                Map map = (Map) src;
                Iterator it = map.entrySet().iterator();
                String txt = "(";
                for (int i = 0; it.hasNext(); i++) {
                    if (i > 0) txt += ",";
                    Map.Entry entry = (Map.Entry) it.next();
                    txt += toString(entry.getKey()) + Data.ASSIGN_CHAR + toString(entry.getValue());
                }
                return txt + ")";
            } else if (src instanceof Collection) return toString(((Collection) src).toArray()); else if (src.getClass().isArray()) {
                int len = Array.getLength(src);
                String txt = "(";
                for (int i = 0; i < len; i++) {
                    if (i > 0) txt += ",";
                    txt += toString(Array.get(src, i));
                }
                return txt + ")";
            } else if (src instanceof Class) return ((Class) src).getName(); else if (src instanceof Method) {
                Method m = (Method) src;
                return m.getDeclaringClass().getName() + "." + m.getName() + toString(m.getParameterTypes());
            } else return src.toString();
        } catch (Exception e) {
            return e.toString();
        }
    }

    public static String escapeString(Object val, String quote, int esc) {
        String src;
        if (val == null) src = ""; else if (val instanceof char[]) src = new String((char[]) val); else src = val.toString();
        char escChar = (char) 0;
        if (quote == null) quote = ""; else if (!quote.equals("")) escChar = quote.charAt(0);
        StringBuffer txt = new StringBuffer(quote);
        int nb = src.length();
        for (int i = 0; i < nb; i++) {
            char c = src.charAt(i);
            if (c == escChar) txt.append(escapeChar(c, esc)); else if (c >= ' ' && c < 0x7f) {
                String trans = null;
                switch(esc) {
                    case HTML_ESCAPE:
                        switch(c) {
                            case '<':
                                trans = "&lt;";
                                break;
                            case '>':
                                trans = "&gt;";
                                break;
                            case '&':
                                trans = "&amp;";
                                break;
                        }
                        break;
                    case URL_ESCAPE:
                        if (c < '0' || c > 'z' || (c > '9' && c < 'A') || (c > 'Z' && c < 'a')) trans = escapeChar(c, esc);
                        break;
                }
                if (trans == null) txt.append(c); else txt.append(trans);
            } else txt.append(escapeChar(c, esc));
        }
        return txt.append(quote).toString();
    }

    public static Number toNumber(Object src) {
        if (src == null) return new Integer(0); else if (src instanceof Number) return (Number) src; else if (src instanceof Boolean) return new Probability(((Boolean) src).booleanValue()); else if ((src instanceof char[]) || (src instanceof CharSequence)) {
            Object num = DataFactory.getNumeric(src, null);
            return (num instanceof Number) ? (Number) num : null;
        } else if (src instanceof Collection) return new Integer(((Collection) src).size()); else if (src.getClass().isArray()) return new Integer(Array.getLength(src)); else return null;
    }

    public static boolean toBoolean(Object src) {
        char c;
        try {
            if (src == null) return false; else if (src instanceof Boolean) return ((Boolean) src).booleanValue(); else if (src instanceof Probability) return ((Probability) src).booleanValue(); else if (src instanceof Number) return ((Number) src).intValue() != 0; else if (src instanceof CharSequence) c = src.toString().charAt(0); else if (src instanceof char[]) c = Array.getChar(src, 0); else if (src.getClass().isArray()) return Array.getLength(src) > 0; else if (src instanceof Collection) return ((Collection) src).size() > 0; else if (src instanceof Map) return ((Map) src).size() > 0; else return true;
        } catch (Exception e) {
            return false;
        }
        switch(c) {
            case 'T':
            case 'V':
            case 'X':
            case 'O':
            case 'Y':
            case 't':
            case 'v':
            case 'x':
            case 'o':
            case 'y':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return true;
            default:
                return false;
        }
    }

    public static Collection toCollection(Object src) {
        if (src == null) return new ArrayList(); else if (src instanceof char[]) return toCollection(new String((char[]) src)); else if (src instanceof CharSequence) {
            Object[] elems = Types.toArray(src);
            List dst = new ArrayList(elems.length);
            for (int i = 0; i < elems.length; i++) dst.add(elems[i]);
            return dst;
        } else if (src instanceof Collection) return (Collection) src; else if (src.getClass().isArray()) {
            int n = Array.getLength(src);
            List dst = new ArrayList(n);
            for (int i = 0; i < n; i++) dst.add(Array.get(src, i));
            return dst;
        } else {
            List dst = new ArrayList();
            dst.add(src);
            return dst;
        }
    }

    public static Object[] toArray(Object src) {
        if (src == null) return new Object[0]; else if (src instanceof char[]) return toArray(new String((char[]) src)); else if (src.getClass().isArray()) {
            if (src.getClass().getComponentType().isPrimitive()) {
                int n = Array.getLength(src);
                Object[] dst = new Object[n];
                for (int i = 0; i < n; i++) dst[i] = Array.get(src, i);
                return dst;
            } else return (Object[]) src;
        } else if (src instanceof Collection) return ((Collection) src).toArray(); else if (src instanceof CharSequence) {
            String txt = src.toString().trim();
            String sep;
            if (txt.equals("")) return new Object[0]; else if (txt.indexOf(',') >= 0) sep = ","; else if (txt.indexOf(' ') >= 0) sep = " "; else return new Object[] { src };
            boolean done = false;
            while (!done) {
                if (txt.length() == 0) return new Object[0]; else if (txt.charAt(0) == '(' && txt.charAt(txt.length() - 1) == ')') txt = txt.substring(1, txt.length() - 1); else if (txt.charAt(0) == '{' && txt.charAt(txt.length() - 1) == '}') txt = txt.substring(1, txt.length() - 1); else if (txt.charAt(0) == '[' && txt.charAt(txt.length() - 1) == ']') txt = txt.substring(1, txt.length() - 1); else done = true;
            }
            return txt.split(sep);
        } else return new Object[] { src };
    }

    public static Object[] toArray(Object src, Class type) {
        Object[] a = toArray(src);
        if (type == null || type.equals(Object.class)) return a;
        Object[] dst = (Object[]) Array.newInstance(type, a.length);
        for (int i = 0; i < a.length; i++) dst[i] = convert(a[i], type);
        return dst;
    }

    public static Map toMap(Object src) {
        if (src == null) return new LinkedHashMap(); else if (src instanceof Map) return (Map) src; else if (src instanceof CharSequence) {
            Map map = new LinkedHashMap();
            String txt = src.toString();
            int p = txt.indexOf('?');
            if (p < 0) {
                if (txt.charAt(0) == '(' && txt.charAt(txt.length() - 1) == ')') txt = txt.substring(1, txt.length() - 1);
                String[] parts = txt.split(",");
                for (int i = 0; i < parts.length; i++) addMapEntry(map, parts[i]);
            } else {
                txt = txt.substring(p + 1);
                p = txt.indexOf('#');
                if (p >= 0) txt = txt.substring(0, p);
                String[] parts = txt.split("&");
                for (int i = 0; i < parts.length; i++) {
                    try {
                        addMapEntry(map, URLDecoder.decode(parts[i], "UTF-8"));
                    } catch (Exception e) {
                    }
                }
            }
            return map;
        } else if (src instanceof char[]) return toMap(new String((char[]) src)); else if ((src instanceof Number) || (src instanceof Boolean) || (src instanceof Throwable)) {
            Map map = new LinkedHashMap();
            addMapEntry(map, src);
            return map;
        } else if (src instanceof Collection) return toMap(((Collection) src).toArray()); else if (src.getClass().isArray()) {
            Map map = new LinkedHashMap();
            int n = Array.getLength(src);
            for (int i = 0; i < n; i++) addMapEntry(map, Array.get(src, i));
            return map;
        } else return new Entity("", src);
    }

    public static Stream toStream(Object obj) {
        if (obj instanceof Stream) return (Stream) obj; else if (obj == null) return null; else if (!Stream.isStreamable(obj)) obj = obj.toString();
        try {
            return new Stream(obj);
        } catch (Exception e) {
            return null;
        }
    }

    public static Date toDate(Object src) {
        if (src instanceof Date) return (Date) src; else if (src == null || src.toString().trim().equals("")) return new Date(); else if (src instanceof Number) return new Date(((Number) src).longValue()); else if (src instanceof char[]) return toDate(new String((char[]) src)); else if (src instanceof CharSequence) {
            String txt = src.toString();
            SimpleDateFormat[] formats = getDateFormats();
            for (int f = 0; f < formats.length; f++) {
                try {
                    return formats[f].parse(txt);
                } catch (Exception e) {
                }
            }
        }
        return null;
    }

    public static Color toColor(Object src) {
        if (src == null) return Color.BLACK; else if (src instanceof CharSequence) {
            String txt = src.toString().trim();
            if (txt.charAt(0) != '#') return null;
            txt = txt.substring(1);
            switch(txt.length()) {
                case 0:
                    return Color.BLACK;
                case 1:
                    txt += "00000ff";
                    break;
                case 2:
                    txt += "0000ff";
                    break;
                case 3:
                    txt += "000ff";
                    break;
                case 4:
                    txt += "00ff";
                    break;
                case 5:
                    txt += "0ff";
                    break;
                case 6:
                    txt += "ff";
                    break;
                case 7:
                    txt += "0";
                    break;
                case 8:
                    break;
                default:
                    return null;
            }
            int[] c = new int[4];
            for (int i = 0; i < 4; i++) {
                try {
                    c[i] = Integer.parseInt(txt.substring(i * 2, (i + 1) * 2), 16);
                } catch (Exception e) {
                    return null;
                }
            }
            return new Color(c[0], c[1], c[2], c[3]);
        } else if (src instanceof char[]) return toColor(new String((char[]) src)); else if (src instanceof Number) {
            int rgba = ((Number) src).intValue();
            return new Color(rgba, rgba > 0xffffff || rgba < 0);
        } else if (src.getClass().isArray()) {
            Object[] array = Types.toArray(src);
            int[] c = new int[] { 0, 0, 0, 255 };
            for (int i = 0; i < 4 && i < array.length; i++) {
                try {
                    c[i] = (array[i] == null) ? 0 : Integer.parseInt(array[i].toString());
                } catch (Exception e) {
                    return null;
                }
            }
            return new Color(c[0], c[1], c[2], c[3]);
        } else if (src instanceof Collection) return toColor(((Collection) src).toArray());
        return null;
    }

    /***************************** private method helpers *************************************/
    private static void addMapEntry(Map map, Object obj) {
        String name = null;
        if ((obj instanceof CharSequence) || (obj instanceof char[])) {
            String txt = toString(obj);
            int p = txt.indexOf('=');
            if (p < 0) p = txt.indexOf(':');
            if (p > 0) {
                name = txt.substring(0, p);
                obj = txt.substring(p + 1);
            } else if (p == 0) obj = txt.substring(1);
        }
        if (name == null) {
            name = (obj == null) ? "null" : obj.getClass().getName().toLowerCase();
            name = name.substring(name.lastIndexOf('.') + 1);
        }
        map.put(name, obj);
    }

    private static String escapeChar(char c, int esc) {
        switch(esc) {
            case HTML_ESCAPE:
                return "&#" + ((int) c) + ";";
            case UNICODE_ESCAPE:
                return "\\u" + Integer.toHexString(c + 0x10000).substring(1);
            case ASCII_ESCAPE:
                return "\\x" + Integer.toHexString(c + 0x100).substring(1);
            case URL_ESCAPE:
                return "%" + Integer.toHexString(c + 0x100).substring(1);
            case SQL_ESCAPE:
                if (c == '\'') return "''";
            default:
                return "" + c;
        }
    }

    private static void addProperty(Map props, String name, Method method, int action) {
        name = name.substring(0, 1).toLowerCase() + name.substring(1);
        Method[] methods = (Method[]) props.get(name);
        if (methods == null) {
            methods = new Method[2];
            props.put(name, methods);
        }
        methods[action] = method;
    }

    private static Constructor getClassConstructor(Class dst, Object obj) {
        Map clst = (Map) CONSTRUCTORS.get(dst);
        if (clst == null) return null;
        if (obj == null) {
            Constructor cons = (Constructor) clst.get(null);
            if (cons != null) return cons;
            return (Constructor) clst.get(Object.class);
        }
        Constructor objectCons = null;
        for (Class parent = obj.getClass(); parent != null; parent = parent.getSuperclass()) {
            Constructor cons = (Constructor) clst.get(parent);
            if (parent.equals(Object.class)) objectCons = cons; else if (cons != null) return cons;
        }
        Class[] ifaces = obj.getClass().getInterfaces();
        for (int i = 0; i < ifaces.length; i++) {
            Constructor cons = (Constructor) clst.get(ifaces[i]);
            if (cons != null) return cons;
        }
        return objectCons;
    }

    private static Method getClassConverter(Class type) {
        for (Class parent = type; parent != null; parent = parent.getSuperclass()) {
            Method m = (Method) CONVERTERS.get(parent);
            if (m != null) return m;
        }
        Class[] ifaces = type.getInterfaces();
        for (int i = 0; i < ifaces.length; i++) {
            Method m = (Method) CONVERTERS.get(ifaces[i]);
            if (m != null) return m;
        }
        return null;
    }

    private static SimpleDateFormat[] getDateFormats() {
        if (DATE_FORMATS == null) {
            String format = Context.getConfigValue("dateformat");
            if (format == null || format.equals("")) DATE_FORMATS = new SimpleDateFormat[] { new SimpleDateFormat("t") }; else {
                int space = format.trim().indexOf(' ');
                if (space > 0) DATE_FORMATS = new SimpleDateFormat[] { new SimpleDateFormat(format), new SimpleDateFormat(format.substring(0, space)) }; else DATE_FORMATS = new SimpleDateFormat[] { new SimpleDateFormat(format) };
            }
        }
        return DATE_FORMATS;
    }
}
