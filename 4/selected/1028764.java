package com.noahsloan.nutils;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import com.noahsloan.nutils.function.SafeFunction1;

/**
 * Utility functions. We reinvent the wheel here because we want to avoid
 * requiring external dependencies.
 * <p>
 * See the unit tests for further examples of usage. Methods that are not unit
 * tested are noted.
 * 
 * @author noah
 * 
 */
public class Utils {

    /**
	 * Base class for iterators that do not support remove.
	 * 
	 * TODO convert to top level
	 * 
	 * @author noah
	 * 
	 * @param <T>
	 */
    public abstract static class StaticIterator<T> implements Iterator<T> {

        public final void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
	 * 
	 * @param <T>
	 * @param value
	 * @param def
	 * @return def, if value is null. Otherwise, value.
	 */
    public static <T> T getValue(T value, T def) {
        return value == null ? def : value;
    }

    /**
	 * Similar to {@link #getValue(Object, Object)}, but allows both return
	 * values to be specified.
	 * 
	 * @param <T>
	 * @param obj
	 * @param isNull
	 *            returned if obj is null
	 * @param isntNull
	 *            returned if obj is not null
	 * @return isNull or isntNull
	 */
    public static <T> T getValue(Object obj, T isNull, T isntNull) {
        return obj == null ? isNull : isntNull;
    }

    /**
	 * Gets the value of the given key from the map, and if it is not null
	 * returns it. Otherwise returns def.
	 * 
	 * @param <K>
	 * @param <V>
	 * @param map
	 * @param key
	 * @param def
	 * @return
	 */
    public static <K, V> V getValue(Map<K, V> map, K key, V def) {
        return getValue(map.get(key), def);
    }

    /**
	 * Checks map for the given key, setting it to the given default if the
	 * value returned is null.
	 * 
	 * @param <K>
	 * @param <V>
	 * @param map
	 * @param key
	 * @param def
	 * @return the value in the map.
	 */
    public static <K, V> V mapDefault(Map<K, V> map, K key, V def) {
        V v = map.get(key);
        if (v == null) {
            map.put(key, def);
            v = def;
        }
        return v;
    }

    /**
	 * 
	 * @param s
	 * @param def
	 *            the default value
	 * @return s if s is not empty according to {@link #isEmpty(String)},
	 *         otherwise def
	 */
    public static String getValue(String s, String def) {
        return isEmpty(s) ? def : s;
    }

    /**
	 * 
	 * @param o
	 * @return true if the {@link #toString(Object)} of the object
	 *         {@link #isEmpty(CharSequence)}
	 */
    public static boolean isEmpty(Object o) {
        return isEmpty(Utils.toString(o));
    }

    /**
	 * 
	 * @param c
	 * @return true if c is null or contains no elements
	 */
    public static boolean isEmpty(Collection<?> c) {
        return c == null || c.isEmpty();
    }

    /**
	 * 
	 * @param s
	 * @return true if s is null or contains only whitespace characters
	 *         according to {@link Character#isWhitespace(char)}. Otherwise,
	 *         false.
	 */
    public static boolean isEmpty(CharSequence s) {
        if (s != null) {
            for (int i = 0; i < s.length(); i++) {
                if (!Character.isWhitespace(s.charAt(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
	 * Same as join(array, del, 0, array.length)
	 * 
	 * @param array
	 * @param del
	 * @return
	 */
    public static String join(final Object[] array, Object del) {
        return array == null ? "" : join(array, del, 0, array.length);
    }

    /**
	 * Joins the given array by creating an
	 * {@link #arrayIterator(Object[], int, int)} and passing it to
	 * {@link #join(Iterator, Object)}
	 * 
	 * @param array
	 * @param del
	 * @param startIndex
	 *            0 to array.length
	 * @param length
	 *            number of element to return
	 * @return
	 */
    public static String join(final Object[] array, Object del, final int startIndex, final int length) {
        return join(arrayIterator(array, startIndex, length), del);
    }

    /**
	 * Factory for iterators over a particular array. Simply delegates to
	 * {@link Utils#arrayIterator(Object[], int, int)}.
	 * 
	 * @author noah
	 * 
	 * TODO convert to top level
	 * 
	 * @param <T>
	 */
    public static class ArrayIterable<T> implements Iterable<T> {

        private final T[] array;

        private final int startIndex;

        private final int length;

        public ArrayIterable(final T[] array) {
            this(array, 0, array.length);
        }

        /**
		 * 
		 * @see Utils#arrayIterator(Object[], int, int)
		 */
        public ArrayIterable(final T[] array, final int startIndex, final int length) {
            super();
            this.array = array;
            this.startIndex = startIndex;
            this.length = length;
        }

        /**
		 * @see Utils#arrayIterator(Object[], int, int)
		 */
        public Iterator<T> iterator() {
            return arrayIterator(array, startIndex, length);
        }

        /**
		 * @see Utils#arrayIterator(Object[], int, int)
		 */
        public static <T> Iterable<T> iterable(T[] array, int startIndex, int length) {
            return new ArrayIterable<T>(array, startIndex, length);
        }
    }

    /**
	 * 
	 * @param <T>
	 * @param array
	 *            the elements t iterate over.
	 * @return an iterator over the given elements.
	 */
    public static <T> Iterator<T> arrayIterator(final T... array) {
        return arrayIterator(array, 0, array.length);
    }

    /**
	 * Creates an iterator that returns elements from the given array. If length <
	 * 0, the iterator will traverse backwards from startIndex(inclusive),
	 * returning a total of length elements. Array indecies that go above
	 * array.length or below 0 are wrapped according to the semantics of
	 * {@link #wrap(int, int)} e.g.
	 * <p>
	 * array = {"foo","bar","baz"} <br>
	 * elements of arrayIterator(array,0,3) = "foo","bar","baz" <br>
	 * elements of arrayIterator(array,3,0) = empty <br>
	 * elements of arrayIterator(array,1,2) = "bar","baz" <br>
	 * elements of arrayIterator(array,1,1) = "bar" <br>
	 * elements of arrayIterator(array,1,-1) = "bar" <br>
	 * elements of arrayIterator(array,1,-2) = "bar","foo" <br>
	 * elements of arrayIterator(array,3,-2) = "foo","baz" <br>
	 * elements of arrayIterator(array,-1,-2) = "baz","bar" <br>
	 * elements of arrayIterator(array,-1,-5) = "baz","bar","foo","baz","bar"
	 * <br>
	 * 
	 * @param <T>
	 * @param array
	 * @param startIndex
	 * @param length
	 * @return
	 */
    public static <T> Iterator<T> arrayIterator(final T[] array, final int startIndex, final int length) {
        if (length < 0) {
            return new StaticIterator<T>() {

                int i = wrap(startIndex, array.length);

                int remaining = array.length > 0 ? -length : 0;

                public boolean hasNext() {
                    return remaining > 0;
                }

                public T next() {
                    T t = array[i];
                    i = wrap(i - 1, array.length);
                    remaining--;
                    return t;
                }
            };
        }
        return new StaticIterator<T>() {

            int i = wrap(startIndex, array.length);

            int remaining = array.length > 0 ? length : 0;

            public boolean hasNext() {
                return remaining > 0;
            }

            public T next() {
                T t = array[i];
                i = wrap(i + 1, array.length);
                remaining--;
                return t;
            }
        };
    }

    /**
	 * Wraps values with modulo arithmetic. Negative values are considered
	 * offsets from top. e.g. -1 wraps to top - 1. -top wraps to 0. top + 1
	 * wraps to 1. If top &lt;= 0, returns 0.
	 * 
	 * @param value
	 * @param top
	 * @return a number between 0 (inclusive) and top (exclusive)
	 */
    public static int wrap(int value, int top) {
        if (top <= 0) {
            return 0;
        }
        value %= top;
        return value < 0 ? top + value : value;
    }

    /**
	 * Convenience methods. Gets an iterator and calls join.
	 * 
	 * @param it
	 * @param del
	 * @return
	 */
    public static String join(Iterable<?> it, Object del) {
        return join(it.iterator(), del);
    }

    /**
	 * Concatenates the {@link Object#toString()} of elements from it, appending
	 * del between elements. e.g. joining "foo","bar","baz" with del "," =
	 * "foo,bar,baz"
	 * 
	 * @param it
	 * @param del
	 * @return
	 */
    public static String join(Iterator<?> it, Object del) {
        StringBuilder sb = new StringBuilder();
        if (it.hasNext()) {
            sb.append(it.next());
            while (it.hasNext()) {
                sb.append(del);
                sb.append(it.next());
            }
        }
        return sb.toString();
    }

    /**
	 * Join with a default.
	 * 
	 * @param it
	 * @param delimeter
	 * @param def
	 *            the default value to be returned if it is null or has no next
	 *            element
	 * @return
	 */
    public static String join(Iterator<?> it, Object delimeter, String def) {
        if (it == null || !it.hasNext()) {
            return def;
        }
        return join(it, delimeter);
    }

    /**
	 * Convenience method. Gets an iterator and calls join.
	 * 
	 * @param it
	 * @param delimeter
	 * @param def
	 * @return
	 */
    public static String join(Iterable<?> it, Object delimeter, String def) {
        return join(it.iterator(), delimeter, def);
    }

    /**
	 * {@link #toString(Object)}'s object and calls split on it.
	 * 
	 * @param object
	 * @param del
	 * @return
	 */
    public static String[] split(Object object, String del) {
        return split(object, del, 0);
    }

    /**
	 * 
	 * @param object
	 * @param del
	 * @param limit
	 *            same as {@link String#split(String, int)}
	 * @return
	 */
    public static String[] split(Object object, String del, int limit) {
        return toString(object).split(del, limit);
    }

    /**
	 * 
	 * @param obj
	 * @return the {@link Object#toString()} of obj if it is not null, or the
	 *         empty string otherwise.
	 */
    public static String toString(Object obj) {
        return toString(obj, "");
    }

    /**
	 * 
	 * @param obj
	 * @param def
	 * @return the {@link Object#toString()} if obj is not null, def otherwise.
	 */
    public static String toString(Object obj, String def) {
        return obj == null ? def : obj.toString();
    }

    /**
	 * Finds a method on the given class that would accept parameters of the
	 * given types and has the given return type (if specified).
	 * 
	 * @param clazz
	 * @param returnType
	 *            the return type of the method, or null if it doesn't matter.
	 * @param paramTypes
	 *            the types of the parameters to be passed.
	 * @return the {@link Method}, if found.
	 */
    public static Method findMethod(Class<?> clazz, Class<?> returnType, Class<?>... paramTypes) {
        Method[] methods = clazz.getMethods();
        methodLoop: for (Method method : methods) {
            Class<?>[] types = method.getParameterTypes();
            if (types.length == paramTypes.length && (returnType == null || method.getReturnType().isAssignableFrom(returnType))) {
                for (int i = 0; i < types.length; i++) {
                    if (!types[i].isAssignableFrom(paramTypes[i])) {
                        continue methodLoop;
                    }
                }
                return method;
            }
        }
        return null;
    }

    /**
	 * null safe version of {@link #findMethod(Class, Class, Class[])}. If
	 * object is null, returns null, otherwise calls
	 * {@link #findMethod(Class, Class, Class[])} with object.getClass().
	 * 
	 * @param object
	 * @param returnType
	 * @param paramTypes
	 * @return
	 */
    public static Method findMethod(Object object, Class<?> returnType, Class<?>... paramTypes) {
        return object == null ? null : findMethod(object.getClass(), returnType, paramTypes);
    }

    /**
	 * Gets the specified index from the list if the list is not null and index
	 * &lt; list.size(). Otherwise returns null.
	 * 
	 * @param <T>
	 * @param list
	 * @param index
	 * @return
	 */
    public static <T> T get(List<T> list, int index) {
        if (list != null && list.size() > index && index >= 0) {
            return list.get(index);
        }
        return null;
    }

    /**
	 * Null safe equals. o1 and o2 may be null. If o1 and o2 are null, returns
	 * true.
	 * 
	 * @param o1
	 * @param o2
	 * @return
	 */
    public static boolean equals(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        }
        return o1 != null && o1.equals(o2);
    }

    /**
	 * 
	 * @param <T>
	 * @param c1
	 * @param c2
	 * @return compare(c1, c2, true)
	 */
    public static <T extends Comparable<? super T>> int compare(T c1, T c2) {
        return compare(c1, c2, true);
    }

    /**
	 * Null safe compare to. Last argument controls whether null elements appear
	 * first or last when sorting.
	 * 
	 * @param <T>
	 * @param c1
	 * @param c2
	 * @param nullFirst
	 *            should null values be considered less than non-null values?
	 * @return
	 * @see Comparator#compare(Object, Object)
	 */
    public static <T extends Comparable<? super T>> int compare(T c1, T c2, boolean nullFirst) {
        if (c1 == c2) {
            return 0;
        }
        if (c1 == null) {
            return nullFirst ? -1 : 1;
        }
        if (c2 == null) {
            return nullFirst ? 1 : -1;
        }
        return c1.compareTo(c2);
    }

    /**
	 * Used to compare values where one or both values may be null.
	 * 
	 * @see Utils#compare(Comparable, Comparable, boolean)
	 * 
	 * @author noah
	 * 
	 * @param <T>
	 */
    public static class SafeComparator<T extends Comparable<? super T>> implements Comparator<T> {

        private boolean nullFirst;

        public SafeComparator() {
            this(true);
        }

        public SafeComparator(boolean nullFirst) {
            super();
            this.nullFirst = nullFirst;
        }

        public int compare(T o1, T o2) {
            return Utils.compare(o1, o2, nullFirst);
        }
    }

    /**
	 * Loads a class path resource as an {@link InputStream} and calls
	 * {@link #toString(InputStream, String)}.
	 * 
	 * @param resource
	 * @param encoding
	 * @return
	 * @throws IOException
	 *             if resource is not found.
	 */
    public static String resourceToString(String resource, String encoding) throws IOException {
        InputStream stream = Utils.class.getResourceAsStream(resource);
        if (stream == null) {
            throw new FileNotFoundException(resource);
        }
        return toString(stream, encoding);
    }

    /**
	 * Similar to {@link #resourceToString(String, String)}, but returns a
	 * default value if anything goes wrong (file not found, couldn't be read,
	 * etc.)
	 * 
	 * @param resource
	 * @param encoding
	 * @param def
	 *            the default value
	 * @return
	 */
    public static String resourceToString(String resource, String encoding, String def) {
        try {
            return resourceToString(resource, encoding);
        } catch (IOException e) {
            return def;
        }
    }

    /**
	 * Reads the given stream in the given encoding, into a String.
	 * 
	 * @param in
	 * @param encoding
	 * @return
	 * @throws IOException
	 */
    public static String toString(InputStream in, String encoding) throws IOException {
        return toString(new InputStreamReader(in, getCharset(encoding)));
    }

    /**
	 * Converts the given bytes into a string. If encoding is null, the platform
	 * default is used.
	 * 
	 * @param bytes
	 * @param encoding
	 * @return
	 * @throws UnsupportedEncodingException
	 */
    public static String toString(byte[] bytes, String encoding) throws UnsupportedEncodingException {
        return encoding == null ? new String(bytes) : new String(bytes, encoding);
    }

    /**
	 * Reads all the characters and returns them as a string.
	 * 
	 * @param reader
	 * @return
	 * @throws IOException
	 */
    public static String toString(Reader reader) throws IOException {
        StringWriter writer = new StringWriter();
        writeAll(reader, writer);
        return writer.toString();
    }

    /**
	 * Gets the supported charset with the given name, returning the default
	 * charset if the named charset is null or not supported.
	 * 
	 * @param charsetName
	 * @return
	 */
    public static Charset getCharset(String charsetName) {
        return charsetName != null && Charset.isSupported(charsetName) ? Charset.forName(charsetName) : Charset.defaultCharset();
    }

    /**
	 * Size of default byte and char buffers.
	 */
    private static final int BUFFER_SIZE = 16384;

    /**
	 * Copies characters from the given reader to the give writter until the
	 * reader returns EOS.
	 * 
	 * @param in
	 * @param out
	 * @throws IOException
	 */
    public static void writeAll(Reader in, Writer out) throws IOException {
        writeAll(in, out, new char[BUFFER_SIZE]);
    }

    /**
	 * Same as {@link #writeAll(Reader, Writer)}, but uses the given buffer.
	 * 
	 * @param in
	 * @param out
	 * @param b
	 * @throws IOException
	 */
    public static void writeAll(Reader in, Writer out, char[] b) throws IOException {
        int read;
        while ((read = in.read(b)) != -1) {
            out.write(b, 0, read);
        }
    }

    /**
	 * Copies all the bytes from in to out, using the given buffer.
	 * 
	 * @param in
	 * @param out
	 * @param b
	 * @throws IOException
	 */
    public static void writeAll(InputStream in, OutputStream out, byte[] b) throws IOException {
        int read;
        while ((read = in.read(b)) != -1) {
            out.write(b, 0, read);
        }
    }

    /**
	 * Same as {@link #writeAll(InputStream, OutputStream, byte[])}, but uses a
	 * buffer of the default size.
	 * 
	 * @param in
	 * @param out
	 * @throws IOException
	 */
    public static void writeAll(InputStream in, OutputStream out) throws IOException {
        writeAll(in, out, new byte[BUFFER_SIZE]);
    }

    public static void writeAllAndClose(InputStream in, OutputStream out) throws IOException {
        writeAll(in, out);
        out.close();
    }

    /**
	 * 
	 * @param string
	 * @return string, with the first char in uppercase.
	 */
    public static String capitalize(String string) {
        return string != null && string.length() > 0 ? Character.toUpperCase(string.charAt(0)) + string.substring(1) : "";
    }

    /**
	 * 
	 * @param string
	 * @return string, with the first character lowercase.
	 */
    public static String uncapitalize(String string) {
        return string != null && string.length() > 0 ? Character.toLowerCase(string.charAt(0)) + string.substring(1) : "";
    }

    /**
	 * Concatenates the two 'arrays', returning the new array.
	 * 
	 * @param array
	 * @param append
	 * @return
	 */
    @SuppressWarnings("unchecked")
    public static String[] append(String[] array, String... append) {
        return doAppend(new String[array.length + append.length], array, append);
    }

    /**
	 * Concatenates the two 'arrays'.
	 * 
	 * @param array
	 * @param append
	 * @return
	 */
    public static Object[] append(Object[] array, Object... append) {
        return doAppend(new Object[array.length + append.length], array, append);
    }

    /**
	 * Copies contents of first, then second into _new.
	 * 
	 * @param <T>
	 * @param _new
	 * @param first
	 * @param second
	 * @return _new
	 * @throws ArrayIndexOutOfBoundsException
	 *             if _new.length &lt; first.length + second.length
	 */
    public static <T> T[] doAppend(T[] _new, T[] first, T[] second) {
        System.arraycopy(first, 0, _new, 0, first.length);
        System.arraycopy(second, 0, _new, first.length, second.length);
        return _new;
    }

    /**
	 * Gets the value of the named property via introspection.
	 * 
	 * @param <T>
	 * @param obj
	 * @param property
	 * @return
	 * @throws IntrospectionException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */
    @SuppressWarnings("unchecked")
    public static <T> T getProperty(Object obj, String property) throws IntrospectionException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        PropertyDescriptor pd = new PropertyDescriptor(property, obj.getClass());
        return (T) pd.getReadMethod().invoke(obj);
    }

    /**
	 * same as get(array, index, null)
	 * 
	 * @param <T>
	 * @param array
	 * @param index
	 * @return
	 */
    public static <T> T get(T[] array, int index) {
        return get(array, index, null);
    }

    /**
	 * Gets the given array index, if it exists, otherwise returns def.
	 * 
	 * @param <T>
	 * @param array
	 * @param index
	 * @param def
	 * @return
	 */
    public static <T> T get(T[] array, int index, T def) {
        return array != null && index < array.length && index >= 0 ? array[index] : def;
    }

    /**
	 * Creates a new set from the given items, the order they are given will be
	 * their iteration order.
	 * 
	 * @param <T>
	 * @param items
	 * @return
	 */
    public static <T> LinkedHashSet<T> asSet(T... items) {
        LinkedHashSet<T> set = new LinkedHashSet<T>();
        if (items != null) {
            for (T item : items) {
                set.add(item);
            }
        }
        return set;
    }

    /**
	 * Collects the values of the map associated with the given keys into a set.
	 * 
	 * @param <K>
	 * @param <V>
	 * @param map
	 * @param keys
	 * @return
	 */
    public static <K, V> Set<V> getValues(Map<K, V> map, Iterator<K> keys) {
        Set<V> values = new HashSet<V>();
        while (keys.hasNext()) {
            values.add(map.get(keys.next()));
        }
        return values;
    }

    /**
	 * Fills the given array with the given value.
	 * 
	 * @param array
	 * @param value
	 * @return array
	 */
    public static boolean[] fill(boolean[] array, boolean value) {
        Arrays.fill(array, value);
        return array;
    }

    /**
	 * Fills the given array with the given value.
	 * 
	 * @param array
	 * @param value
	 * @return array
	 */
    public static <T> T[] fill(T[] array, T value) {
        Arrays.fill(array, value);
        return array;
    }

    /**
	 * Casts the given ints down to bytes.
	 * 
	 * @param bytes
	 * @return
	 */
    public static byte[] truncate(int[] bytes) {
        byte[] array = new byte[bytes.length];
        for (int i = 0; i < array.length; i++) {
            array[i] = (byte) bytes[i];
        }
        return array;
    }

    /**
	 * Casts the given ints to chars.
	 * 
	 * @param chars
	 * @return
	 */
    public static char[] toCharArray(int[] chars) {
        char[] array = new char[chars.length];
        for (int i = 0; i < array.length; i++) {
            array[i] = (char) chars[i];
        }
        return array;
    }

    /**
	 * Set the array index to true, returning array. For chaining. Untested.
	 * 
	 * @param array
	 * @param index
	 * @return
	 */
    public static boolean[] set(boolean[] array, int index) {
        array[index] = true;
        return array;
    }

    /**
	 * Set the array index to false, returning array. For chaining. Untested.
	 * 
	 * @param array
	 * @param index
	 * @return
	 */
    public static boolean[] unset(boolean[] array, int index) {
        array[index] = false;
        return array;
    }

    /**
	 * 
	 * @param array
	 * @return an array with null elements removed.
	 */
    public static Object[] prune(Object[] array) {
        List<Object> list = new ArrayList<Object>(array.length);
        for (Object o : array) {
            if (o != null) {
                list.add(o);
            }
        }
        return list.toArray();
    }

    /**
	 * 
	 * @param array
	 * @return a new array with the empty strings removed
	 */
    public static String[] prune(String[] array) {
        List<String> list = new ArrayList<String>(array.length);
        for (String o : array) {
            if (!Utils.isEmpty(o)) {
                list.add(o);
            }
        }
        return list.toArray(new String[list.size()]);
    }

    /**
	 * 
	 * @param obj
	 * @return a string representing the given object. e.g. "null" if it is
	 *         null, or ""Objects's toString"" (i.e. a quoted literal)
	 */
    public static String toStringConstant(Object obj) {
        return obj == null ? "null" : "\"" + toString(obj).replaceAll("\"", "\\\"").replaceAll("\n", "\\n") + "\"";
    }

    /**
	 * See {@link #toStringConstantArray(Iterator)}.
	 * 
	 * @param array
	 * @return
	 */
    public static String toStringConstantArray(Object... array) {
        return toStringConstantArray(arrayIterator(array));
    }

    /**
	 * See {@link #toStringConstantArray(Iterator)}.
	 * 
	 * @param it
	 * @return
	 */
    public static String toStringConstantArray(Iterable<?> it) {
        return toStringConstantArray(it.iterator());
    }

    /**
	 * Converts each value in it using {@link #toStringConstant(Object)}, puts
	 * commas between them, and wraps everything in {}. e.g.
	 * toStringConstantArray("foo","bar",5); returns "{ \"foo\", \"bar\", \"5\"
	 * }".
	 * 
	 * @param it
	 * @return see above
	 */
    public static String toStringConstantArray(Iterator<?> it) {
        StringBuilder sb = new StringBuilder("{ ");
        if (it != null && it.hasNext()) {
            sb.append(toStringConstant(it.next()));
            while (it.hasNext()) {
                sb.append(", ").append(toStringConstant(it.next()));
            }
        }
        sb.append(" }");
        return sb.toString();
    }

    /**
	 * Converts a camelCase name to a constant name, e.g. camelCase ->
	 * CAMEL_CASE. Non-alphanumeric ( as defined by
	 * {@link Character#isLetterOrDigit(char)}) characters are also converted
	 * to underscores.
	 * 
	 * @param name
	 * @return
	 */
    public static String toConstantName(CharSequence name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (Character.isUpperCase(ch)) {
                sb.append('_');
            }
            sb.append(Character.isLetterOrDigit(ch) ? Character.toUpperCase(ch) : '_');
        }
        return sb.toString();
    }

    /**
	 * Throws a {@link NullPointerException} if value is null.
	 * 
	 * @param value
	 * @param message
	 * @throws NullPointerException
	 */
    public static void notNull(Object value, String message) throws NullPointerException {
        if (value == null) {
            throw new NullPointerException(message);
        }
    }

    /**
	 * Throws an {@link IllegalArgumentException} with the given message if
	 * string is empty.
	 * 
	 * @param string
	 * @param message
	 */
    public static void notEmpty(CharSequence string, String message) {
        if (isEmpty(string)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
	 * Puts the elements of <code>it</code> into a list. Untested.
	 * 
	 * @param <T>
	 * @param it
	 * @return
	 */
    public static <T> List<T> asList(Iterator<T> it) {
        List<T> list = new ArrayList<T>();
        while (it.hasNext()) {
            list.add(it.next());
        }
        return list;
    }

    /**
	 * 
	 * @param document
	 *            the document to serialize
	 * @param out
	 *            the stream to serialize to
	 * @throws TransformerException
	 *             if there is a problem transforming the document into XML
	 */
    public static void write(Document document, OutputStream out) throws TransformerException {
        Transformer serializer = TransformerFactory.newInstance().newTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        serializer.setOutputProperty(OutputKeys.METHOD, "xml");
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        serializer.transform(new DOMSource(document), new StreamResult(out));
    }

    /**
	 * Attempts to create the given file's parent directories if they do not
	 * exist.
	 * 
	 * @param file
	 *            the file to create
	 * @return true if the file;s parents exist after this call. false if the
	 *         parents could not be created for some reason that did not result
	 *         in an exception.
	 * @throws IOException
	 * @throws SecurityException
	 */
    public static boolean createFileParents(final File file) {
        return file.getParentFile().exists() || file.getParentFile().mkdirs();
    }

    /**
	 * Escapes & and <
	 * 
	 * @param string
	 * @return
	 */
    public static String xmlEscape(String string) {
        return Utils.toString(string).replaceAll("&", "&amp;").replaceAll("<", "&lt;");
    }

    /**
	 * Escapes quotes as well as {@link #xmlEscape(String)}ing.
	 * 
	 * @param string
	 * @return
	 */
    public static String xmlAttribEscape(String string) {
        return xmlEscape(string).replaceAll("\"", "&quot;");
    }

    /**
	 * 
	 * @param string
	 * @param del1
	 * @param del2
	 * @param limit1
	 * @param limit2
	 * @return a limit1(max) by limit2 (max) array. Individual arrays may be as
	 *         short as length 1.
	 */
    public static String[][] doubleSplit(String string, String del1, String del2, int limit1, int limit2) {
        String[] split = split(string, del1, limit1);
        String[][] matrix = new String[split.length][];
        int i = 0;
        for (String item : split) {
            matrix[i++] = item.split(del2, limit2);
        }
        return matrix;
    }

    /**
	 * 
	 * @param keyValues
	 *            a 2-D array of key:value pairs. If the sub-array is only of
	 *            length 1, then the value will be null. If the array is of
	 *            length 0, then null:null is inserted.
	 * @return an equivalent map
	 */
    public static Map<String, String> asMap(String[][] keyValues) {
        Map<String, String> map = new HashMap<String, String>();
        for (String[] pair : keyValues) {
            map.put(get(pair, 0), get(pair, 1));
        }
        return map;
    }

    /**
	 * Safe parseInt.
	 * 
	 * @param number
	 * @param def
	 * @return def if number cannot be parsed into an int, otherwise the parsed
	 *         value.
	 */
    public static int getInt(String number, int def) {
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
	 * Concatenates times copies of s together.
	 * 
	 * @param s
	 * @param times
	 * @return the {@link StringBuilder} containing the characters
	 */
    public static StringBuilder repeat(CharSequence s, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) {
            sb.append(s);
        }
        return sb;
    }

    /**
	 * If o is null, returns an iterator with no elements. If it is an array,
	 * returns {@link #arrayIterator(Object[])}. If it is an {@link Iterable},
	 * returns {@link Iterable#iterator()}, otherwise, returns an
	 * {@link Iterator} that returns o.
	 * 
	 * @param o
	 * @return an {@link Iterator}.
	 */
    public static Iterator<?> getIterator(Object o) {
        if (o == null) {
            return Collections.EMPTY_LIST.iterator();
        } else if (o.getClass().isArray()) {
            return Utils.arrayIterator((Object[]) o);
        } else if (o instanceof Iterable) {
            return ((Iterable<?>) o).iterator();
        } else {
            return Collections.singleton(o).iterator();
        }
    }

    /**
	 * Prints the exception stack trace to a String.
	 * 
	 * @param t
	 * @return a String copy of the stack trace.
	 */
    public static String toString(Throwable t) {
        StringWriter w = new StringWriter();
        PrintWriter pw = new PrintWriter(w);
        t.printStackTrace(pw);
        pw.close();
        return w.toString();
    }

    /**
	 * 
	 * @param it
	 * @return toStringArray(it, null)
	 */
    public static String[] toStringArray(Iterator<?> it) {
        return toStringArray(it, null);
    }

    /**
	 * Converts the elements of the given iterator to strings and returns the
	 * strings in an array.
	 * 
	 * @param it
	 * @param def
	 *            the default string for null elements
	 * @return a String array
	 */
    public static String[] toStringArray(Iterator<?> it, String def) {
        List<String> list = new ArrayList<String>();
        while (it.hasNext()) {
            list.add(toString(it.next(), def));
        }
        return list.toArray(new String[list.size()]);
    }

    /**
	 * Calls {@link #binarySearch(List, Object, Comparator)} with a comparator
	 * that uses each {@link Comparable}'s compareTo method.
	 * 
	 * @param <T>
	 * @param list
	 * @param key
	 * @return
	 */
    public static <T extends Comparable<? super T>> int binarySearch(List<T> list, T key) {
        return binarySearch(list, key, new Comparator<T>() {

            public int compare(T o1, T o2) {
                return o1.compareTo(o2);
            }
        });
    }

    /**
	 * Binary search the given list for the given key. Equivalent to
	 * {@link Arrays#binarySearch(Object[], Object, Comparator)} but for lists.
	 * The list has to be sorted for this to work.
	 * 
	 * @param <T>
	 * @param list
	 * @param key
	 * @param c
	 * @return the index of the key it is found, otherwise the negative index of
	 *         where it would be inserted. If the list is empty, -1 will be
	 *         returned.
	 */
    public static <T> int binarySearch(List<T> list, T key, Comparator<? super T> c) {
        int low = 0;
        int high = list.size() - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            int result = c.compare(key, list.get(mid));
            if (result < 0) {
                low = mid + 1;
            } else if (result > 0) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return -(low + 1);
    }

    public static <E> int hashCode(Iterable<E> iterable) {
        int h = 0;
        for (E e : iterable) {
            if (e != null) {
                h += e.hashCode();
            }
        }
        return h;
    }

    public static int hashCode(Object o) {
        return o == null ? 0 : o.hashCode();
    }

    /**
	 * 
	 * @param <E>
	 * @param iterable
	 * @param toString
	 * @return
	 */
    public static <E> StringBuilder toString(Iterable<E> iterable) {
        return toString(iterable.iterator(), new SafeFunction1<E, CharSequence>() {

            public String apply(E p1) throws RuntimeException {
                return p1 == null ? null : p1.toString();
            }
        });
    }

    public static <E> StringBuilder toString(Iterable<E> iterable, SafeFunction1<E, ? extends CharSequence> toString) {
        return toString(iterable.iterator(), toString);
    }

    /**
	 * "item1, item2, item2, ... itemN"
	 * 
	 * @param <E>
	 * @param it
	 * @return
	 */
    public static <E> StringBuilder toString(Iterator<E> it, SafeFunction1<E, ? extends CharSequence> toString) {
        StringBuilder sb = new StringBuilder("[");
        if (it.hasNext()) {
            sb.append(toString.apply(it.next()));
        }
        while (it.hasNext()) {
            sb.append(", ").append(toString.apply(it.next()));
        }
        return sb;
    }
}
