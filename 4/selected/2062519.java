package tyrex.naming;

import java.io.PrintWriter;
import java.util.NoSuchElementException;
import javax.naming.Reference;
import javax.naming.NamingEnumeration;
import javax.naming.NameClassPair;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.LinkRef;
import javax.naming.CompositeName;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;

/**
 * Name/value bindings for use inside {@link MemoryContext}.
 * This one is never constructed directly but through {@link
 * MemoryContext}, {@link MemoryContextFactory} and related classes.
 * <p>
 * Provides heirarchial storage for name/value binding in memory
 * that is exposed through the JNDI context model. Each context
 * (not in the tree) is represented by one instance of {@link
 * MemoryBinding}, with each sub-context (child node) or bound
 * value represented by a name/value pair.
 * <p>
 * This object is thread-safe.
 *
 * @author <a href="arkin@intalio.com">Assaf Arkin</a>
 * @version $Revision: 1.2 $ $Date: 2002/06/07 09:42:11 $
 */
public final class MemoryBinding {

    /**
     * The initial capacity for the hashtable.
     */
    public static final int INITIAL_CAPACITY = 11;

    /**
     * The maximum capacity for the hashtable.
     */
    public static final int MAXIMUM_CAPACITY = 191;

    /**
     * The load factor for the hashtable.
     */
    public static final float LOAD_FACTOR = 0.75f;

    /**
     * The path of this binding.
     */
    private String _name = "";

    /**
     * The parent memory binding.
     */
    protected MemoryBinding _parent;

    /**
     * The number of bindings in the hash table.
     */
    private int _count;

    /**
     * The threshold for resizing the hash table.
     */
    private int _threshold;

    /**
     * The hashtable of memory binding entries.
     */
    private BindingEntry[] _hashTable;

    public MemoryBinding() {
        _hashTable = new BindingEntry[INITIAL_CAPACITY];
        _threshold = (int) (INITIAL_CAPACITY * LOAD_FACTOR);
    }

    public Context getContext() {
        return new MemoryContext(this, null);
    }

    public synchronized Object get(String name) {
        int hashCode;
        int index;
        BindingEntry entry;
        if (name == null) throw new IllegalArgumentException("Argument name is null");
        hashCode = name.hashCode();
        index = (hashCode & 0x7FFFFFFF) % _hashTable.length;
        entry = _hashTable[index];
        while (entry != null) {
            if (entry._hashCode == hashCode && entry._name.equals(name)) return entry._value;
            entry = entry._next;
        }
        return null;
    }

    public synchronized void put(String name, Object value) {
        int hashCode;
        int index;
        BindingEntry entry;
        BindingEntry next;
        if (name == null) throw new IllegalArgumentException("Argument name is null");
        if (value == null) throw new IllegalArgumentException("Argument value is null");
        if (value instanceof MemoryBinding) {
            ((MemoryBinding) value)._parent = this;
            ((MemoryBinding) value)._name = name;
        }
        hashCode = name.hashCode();
        index = (hashCode & 0x7FFFFFFF) % _hashTable.length;
        entry = _hashTable[index];
        if (entry == null) {
            entry = new BindingEntry(name, hashCode, value);
            _hashTable[index] = entry;
            ++_count;
        } else {
            if (entry._hashCode == hashCode && entry._name.equals(name)) {
                entry._value = value;
                return;
            } else {
                next = entry._next;
                while (next != null) {
                    if (next._hashCode == hashCode && next._name.equals(name)) {
                        next._value = value;
                        return;
                    }
                    entry = next;
                    next = next._next;
                }
                entry._next = new BindingEntry(name, hashCode, value);
                ++_count;
            }
        }
        if (_count >= _threshold) rehash();
    }

    public synchronized Object remove(String name) {
        int hashCode;
        int index;
        BindingEntry entry;
        BindingEntry next;
        if (name == null) throw new IllegalArgumentException("Argument name is null");
        hashCode = name.hashCode();
        index = (hashCode & 0x7FFFFFFF) % _hashTable.length;
        entry = _hashTable[index];
        if (entry == null) return null;
        if (entry._hashCode == hashCode && entry._name.equals(name)) {
            _hashTable[index] = entry._next;
            --_count;
            return entry._value;
        }
        next = entry._next;
        while (next != null) {
            if (next._hashCode == hashCode && next._name.equals(name)) {
                entry._next = next._next;
                --_count;
                return next._value;
            }
            entry = next;
            next = next._next;
        }
        return null;
    }

    public String getName() {
        if (_parent != null && _parent.getName().length() > 0) return _parent.getName() + MemoryContext.NameSeparator + _name;
        return _name;
    }

    public boolean isRoot() {
        return (_parent == null);
    }

    public boolean isEmpty() {
        return _count == 0;
    }

    /**
     * Called when destroying the subcontext and binding associated
     * with it.
     */
    public void destroy() {
        _hashTable = null;
    }

    void debug(PrintWriter writer) {
        debug(writer, 0);
    }

    private synchronized void debug(PrintWriter writer, int level) {
        BindingEntry entry;
        Object value;
        for (int j = level; j-- > 0; ) writer.print("  ");
        if (this instanceof MemoryBinding) writer.println("MemoryBinding: " + getName()); else writer.println("ThreadedBinding: " + getName());
        if (_count == 0) writer.println("Empty"); else {
            for (int i = _hashTable.length; i-- > 0; ) {
                entry = _hashTable[i];
                while (entry != null) {
                    for (int j = level; j-- > 0; ) writer.print("  ");
                    value = entry._value;
                    if (value instanceof MemoryBinding) ((MemoryBinding) value).debug(writer, level + 1); else writer.println("  " + entry._name + " = " + value);
                    entry = entry._next;
                }
            }
        }
    }

    protected NamingEnumeration enumerate(Context context, boolean nameOnly) {
        return new MemoryBindingEnumeration(context, nameOnly);
    }

    private void rehash() {
        int newSize;
        BindingEntry[] newTable;
        BindingEntry entry;
        BindingEntry next;
        int index;
        newSize = _hashTable.length * 2 + 1;
        if (newSize > MAXIMUM_CAPACITY) {
            _threshold = Integer.MAX_VALUE;
            return;
        }
        newTable = new BindingEntry[newSize];
        for (int i = _hashTable.length; i-- > 0; ) {
            entry = _hashTable[i];
            while (entry != null) {
                next = entry._next;
                index = (entry._hashCode & 0x7FFFFFFF) % newSize;
                entry._next = newTable[index];
                newTable[index] = entry;
                entry = next;
            }
        }
        _hashTable = newTable;
        _threshold = (int) (newSize * LOAD_FACTOR);
    }

    /**
     * Name to value binding entry in the memory binding hashtable.
     */
    private static class BindingEntry {

        /**
         * The binding name.
         */
        final String _name;

        /**
         * The binding name hash code.
         */
        final int _hashCode;

        /**
         * The bound value.
         */
        Object _value;

        /**
         * The next binding in the hash table entry.
         */
        BindingEntry _next;

        BindingEntry(String name, int hashCode, Object value) {
            _name = name;
            _hashCode = hashCode;
            _value = value;
        }
    }

    /**
     * Naming enumeration supporting {@link NamClassPair} and {@link Binding},
     * created based of a {@link MemoryBinding}.
     *
     * @author <a href="arkin@intalio.com">Assaf Arkin</a>
     * @version $Revision: 1.2 $ $Date: 2002/06/07 09:42:11 $
     * @see MemoryBinding
     */
    private final class MemoryBindingEnumeration implements NamingEnumeration {

        /**
         * Holds a reference to the next entry to be returned by
         * {@link next}. Becomes null when there are no more
         * entries to return.
         */
        private BindingEntry _entry;

        /**
         * Index to the current position in the hash table.
         */
        private int _index;

        /**
         * True to return an enumeration of {@link NameClassPair},
         * false to return an enumeration of {@link Binding}
         */
        private final boolean _nameOnly;

        /**
         * The context is required to create a duplicate.
         */
        private final Context _context;

        /**
         * The class name of the context class
         */
        private final String _contextClassName;

        /**
         * The value of the next element to return. Can be null.
         */
        private Object _nextValue;

        /**
         * The name of the next element to return. Can be null.
         * <P>
         * If the value is null this means that there are no element
         * to return.
         *
         * @see #hasMore
         */
        private String _nextName;

        /**
         * The class name of the next element to return. Can be null.
         */
        private String _nextClassName;

        MemoryBindingEnumeration(Context context, boolean nameOnly) {
            if (context == null) throw new IllegalArgumentException("Argument context is null");
            _context = context;
            _contextClassName = nameOnly ? context.getClass().getName() : null;
            _nameOnly = nameOnly;
            _index = _hashTable.length;
        }

        public boolean hasMoreElements() {
            return hasMore();
        }

        public Object nextElement() {
            return next();
        }

        public void close() {
            _entry = null;
            _index = -1;
            _nextValue = null;
            _nextName = null;
            _nextClassName = null;
        }

        public boolean hasMore() {
            if (-1 == _index) {
                return false;
            }
            if (null != _nextName) {
                return true;
            }
            return internalHasMore();
        }

        public Object next() throws NoSuchElementException {
            Object value;
            String name;
            if (!hasMore()) {
                throw new NoSuchElementException("No more elements in enumeration");
            }
            name = _nextName;
            _nextName = null;
            if (_nameOnly) return new NameClassPair(name, _nextClassName, true);
            value = _nextValue;
            _nextValue = null;
            return new Binding(name, _nextClassName, value, true);
        }

        /**
         * Return true if there are more elements in the 
         * enumeration.
         * <P>
         * This method has the side effects of setting 
         * {@link #_nextValue}, {@link #_nextName}, 
         * {@link #_nextClassName}.
         *
         * @return true if there are more elements in the 
         *      enumeration.
         */
        private boolean internalHasMore() {
            BindingEntry entry;
            Object value;
            entry = nextEntry();
            if (null == entry) {
                return false;
            }
            value = entry._value;
            if (value instanceof MemoryBinding) {
                if (_nameOnly) {
                    _nextClassName = _contextClassName;
                } else {
                    try {
                        _nextValue = _context.lookup(entry._name);
                        _nextClassName = _nextValue.getClass().getName();
                    } catch (NamingException except) {
                        return internalHasMore();
                    }
                }
                _nextName = entry._name;
            } else if ((value instanceof LinkRef)) {
                try {
                    _nextValue = _context.lookup(entry._name);
                    _nextClassName = (null == _nextValue) ? null : _nextValue.getClass().getName();
                } catch (NamingException except) {
                    return internalHasMore();
                }
                if (_nameOnly) {
                    _nextValue = null;
                }
                _nextName = entry._name;
            } else if (value instanceof Reference) {
                if (!_nameOnly) {
                    try {
                        _nextValue = NamingManager.getObjectInstance(value, new CompositeName(entry._name), _context, null);
                    } catch (Exception except) {
                        return internalHasMore();
                    }
                }
                _nextClassName = ((Reference) value).getClassName();
                _nextName = entry._name;
            } else {
                if (!_nameOnly) {
                    _nextValue = value;
                }
                _nextClassName = (null == value) ? null : value.getClass().getName();
                _nextName = entry._name;
            }
            return true;
        }

        private BindingEntry nextEntry() {
            BindingEntry entry;
            int index;
            BindingEntry[] table;
            entry = _entry;
            if (entry != null) {
                _entry = entry._next;
                return entry;
            }
            table = _hashTable;
            index = _index;
            while (index > 0) {
                entry = table[--index];
                if (entry != null) {
                    _entry = entry._next;
                    _index = index;
                    return entry;
                }
            }
            return null;
        }
    }
}
