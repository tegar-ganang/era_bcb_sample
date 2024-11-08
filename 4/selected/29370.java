package obol.lang;

import java.util.*;
import obol.lang.ObolException;
import obol.tools.Debug;

/** Utility class representing a symbol table.  It now supports per-thread
 * nested transactional updates.  Unfortunately transactions cannot span
 * threads.
 * <P><B>TODO:<B><UL>
* 	<LI>Make proper hierarchical transaction, to support abort/commit of
* 	sub-transactions?  It is flat now, meaning that symbols either have
* 	been commited, or are transitional.
 * </UL>
 * <P><TT>$Id: SymbolTable.java,v 1.1 2007/03/16 16:22:09 perm Exp $</TT>
 */
public class SymbolTable {

    private static final String __me = "obol.lang.SymbolTable";

    protected Hashtable table;

    private byte[] tempLock = { 0 };

    private volatile boolean transactionActive = false;

    private Stack sessions = null;

    private Debug log = Debug.getInstance(__me);

    public static ThreadLocal currentTempTable = new ThreadLocal() {

        protected synchronized Object initialValue() {
            return new HashMap();
        }
    };

    /** Default constructor.
     */
    public SymbolTable() {
        this.table = new Hashtable();
    }

    /** Get transaction temporary table, relative to the calling thread.
     */
    public HashMap getTempTable() {
        return (HashMap) currentTempTable.get();
    }

    private Symbol findSymbol(String name) {
        if (null == this.sessions || false == this.transactionActive) {
            return (Symbol) this.getCommitedSymbol(name);
        }
        for (int _i = this.sessions.size(); _i != 0; _i--) {
            Object _o = ((Map) this.sessions.get(_i - 1)).get(name);
            if (null != _o) {
                return (Symbol) _o;
            }
        }
        return (Symbol) this.getCommitedSymbol(name);
    }

    /** Retrive a symbol by its name from the symbol table, creating it and
     * placing it in the table if it doesn't exist.
     * If the <tt>transactionInit()</tt> method preceeds a call to this
     * method, any subsequent new symbols will be put on a temporal list,
     * until either commited or aborted (via the
     * <tt>transactionCommit()</tt> or <tt>transactionAbort()</tt> methods).
     */
    public synchronized Symbol getSymbol(String name) {
        Symbol _sym = null;
        if (this.transactionActive) {
            _sym = this.findSymbol(name);
            if (null == _sym) {
                _sym = new Symbol(name);
                ((Map) this.sessions.peek()).put(name, _sym);
            }
        } else {
            if (this.table.containsKey(name)) {
                _sym = this.getCommitedSymbol(name);
            } else {
                _sym = new Symbol(name);
                this.table.put(name, _sym);
            }
        }
        return _sym;
    }

    /** Syntactic sugar for <tt>getSymbol(sym.getName())</tt>.
     */
    public Symbol getSymbol(Symbol sym) {
        return this.getSymbol(sym.getName());
    }

    /** Retrive a symbol by its name from the symbol table, ignoring any
     * in-transaction symbols.
     * @param symbolName name of symbol to look up among the symbols
     * commited to the symbol-table.
     * @return Symbol object, or <tt>null</tt> if the symbol don't exist in
     * the table.
     */
    public Symbol getCommitedSymbol(String symbolName) {
        return (Symbol) this.table.get(symbolName);
    }

    /** Syntactic sugar for <tt>getCommitedSymbol(sym.getName())</tt>.
     */
    public Symbol getCommitedSymbol(Symbol sym) {
        return this.getCommitedSymbol(sym.getName());
    }

    /** Make a work-copy for the current transcation-session of the given
     * named symbol, which it is certain that it'll be written to.
     * Only non-initialized non-anonymous symbols, or anonymous symbols may
     * be written to. (Such a certainty is known by e.g. resetSymbol() in
     * obolParser.g).
     * <P>
     * This protects existing committed symbols from manipulation during a
     * transaction. 
     * If the named symbol already exists in the current transaction scope,
     * the in-scope symbol is returned.
     * If no transaction is active, or if the named symbol doesn't exist,
     * this method calls the <tt>getSymbol()</tt> method.
     * @param symbolName String containing name of symbol to protect.
     * @return the transactional symbol.
     */
    public Symbol makeTransactional(String symbolName) throws ObolException {
        Symbol _sym = null;
        if (this.transactionActive) {
            Map _currentMap = (Map) this.sessions.peek();
            _sym = this.findSymbol(symbolName);
            if (null != _sym && false == _currentMap.containsKey(symbolName)) {
                if ((null == _sym.getValue()) || _sym.isAnonymous()) {
                    try {
                        log.debug(__me + ".getSymbol(): making transaction clone of \"" + symbolName + "\"");
                        _sym = (Symbol) _sym.clone();
                    } catch (CloneNotSupportedException e) {
                        throw new RuntimeException(__me + ".getSymbol(\"" + symbolName + "\"): clone failed: " + e);
                    }
                    _currentMap.put(symbolName, _sym);
                } else {
                    throw new ObolException(__me + ".makeTransactional(): " + "Illogical attempt of making transaction write-copy " + "of read-only symbol \"" + symbolName + "\"!");
                }
            } else {
                _sym.incForcedCount();
            }
        }
        if (null == _sym) {
            _sym = this.getSymbol(symbolName);
        }
        return _sym;
    }

    /** Remove a symbol by name. Use with care!
     * @return <tt>true</tt> if the symbol was removed.
     */
    public synchronized boolean removeSymbol(String name) {
        boolean _retval = this.exists(name);
        if (_retval) {
            this.table.remove(name);
        }
        return _retval;
    }

    /** See if a given entry exist in the symbol table.  Also checks
     * non-commited/-aborted entries.
     */
    public boolean exists(String name) {
        return (null != this.findSymbol(name));
    }

    /** Syntactic sugar for <tt>exists(sym.getName())</tt>.
     */
    public boolean exists(Symbol sym) {
        return this.exists(sym.getName());
    }

    /** See if a given entry exist in the symbol table.  Also checks
     * non-commited/-aborted entries.
     */
    public boolean existsCommitted(String name) {
        return (null != this.findSymbol(name));
    }

    /** Syntactic sugar for <tt>exists(sym.getName())</tt>.
     */
    public boolean existsCommitted(Symbol sym) {
        return this.exists(sym.getName());
    }

    /** Return an Iterator of all entries in the symboltable, where the
     * commited view is given first, then any any non-commited/-aborted entries.
     * This means that some entries may appear to be duplicated, in which
     * case the first occurance is the commited symboltable, while any
     * others are non-commited (in-transaction).
     */
    public Iterator iterator() {
        return new Iterator() {

            Iterator main = table.entrySet().iterator();

            Iterator session = sessions.iterator();

            Iterator temp = null;

            public boolean hasNext() {
                if (main.hasNext()) {
                    return true;
                }
                if (null == temp) {
                    if (session.hasNext()) {
                        temp = ((Map) session.next()).entrySet().iterator();
                    } else {
                        return false;
                    }
                }
                if (false == temp.hasNext()) {
                    temp = null;
                    return this.hasNext();
                }
                return true;
            }

            public Object next() {
                if (main.hasNext()) {
                    return main.next();
                } else {
                    if (this.hasNext()) {
                        return temp.next();
                    } else {
                        throw new NoSuchElementException(__me + ".iterator()");
                    }
                }
            }

            public void remove() {
                throw new UnsupportedOperationException(__me + ".iterator()");
            }
        };
    }

    /** Initiate new transaction session.
     * This is used by the assign/match mechanisms to allow left-to-right
     * associativity when assigning/matching, i.e. (receive a *foo (verify k *foo))
     * means that the first *foo is assigned to, while the second is
     * matched.  Since this assignment is speculative until the last *foo
     * has been verified, we need transactions.
     */
    public void transactionInit() {
        synchronized (this.tempLock) {
            if (null == this.sessions) {
                this.sessions = new Stack();
            }
            this.sessions.push(new HashMap(3));
            this.transactionActive = true;
            log.debug(".transactionInit: beginning transaction session " + this.sessions.size());
        }
    }

    /** Commit any updates in the current session to the "official" symbol
     * table.  This ends the current session.
     * @return <tt>true</tt> if a transaction was commited, or
     * <tt>false</tt> if it was not (i.e. there were no active
     * transactions!). This does not neccessarily indicate an error, and can
     * bee used e.g. in the case when one wants to commit all outstanding
     * transactions.
     */
    public boolean transactionCommit() {
        boolean _retval = false;
        HashMap _tempTable = this.getTempTable();
        synchronized (this.tempLock) {
            if (null != this.sessions) {
                if (false == this.sessions.isEmpty()) {
                    Map _m = (Map) this.sessions.pop();
                    log.debug(".transactionCommit: committing " + _m.size() + " entries in transaction " + (this.sessions.size() + 1));
                    this.table.putAll(_m);
                    for (Iterator _it = _m.values().iterator(); _it.hasNext(); ) {
                        ((Symbol) _it.next()).clearForcedCount();
                    }
                    _m.clear();
                    _retval = true;
                }
                this.transactionActive = (false == this.sessions.isEmpty());
            } else {
                throw new RuntimeException(__me + ".transactionCommit(): " + "no session stack, must be preceeded by " + "transactionInit() invocation!");
            }
        }
        return _retval;
    }

    /** Abort any updates in the current transaction session. This ends the
     * current session.
     * @return <tt>true</tt> if a transaction was aborted, or
     * <tt>false</tt> if it was not (i.e. there were no active
     * transactions!). This does not neccessarily indicate an error, and can
     * bee used e.g. in the case when one wants to abort all outstanding
     * transactions.
     */
    public boolean transactionAbort() {
        boolean _retval = false;
        HashMap _tempTable = this.getTempTable();
        synchronized (this.tempLock) {
            if (null != this.sessions) {
                if (false == this.sessions.isEmpty()) {
                    Map _m = (Map) this.sessions.pop();
                    for (Iterator _it = _m.values().iterator(); _it.hasNext(); ) {
                        ((Symbol) _it.next()).clearForcedCount();
                    }
                    log.debug(".transactionAbort: aborting " + _m.size() + " entries in transaction " + (this.sessions.size() + 1));
                    _m.clear();
                    _retval = true;
                }
                this.transactionActive = (false == this.sessions.isEmpty());
            } else {
                throw new RuntimeException(__me + ".transactionAbort(): " + "no session stack, must be preceeded by " + "transactionInit() invocation!");
            }
        }
        return _retval;
    }

    /** Returns a string containing a comma-separated list of this
     * symboltable's entries (symbol names), including currently uncommited
     * entries, followed by a list of non-commited transactions (see this
     * class' <tt>dumpOutstandingTransactions()</tt> method).
     */
    public String dump() {
        StringBuffer _sb = new StringBuffer();
        boolean _first = true;
        for (Iterator _it = this.iterator(); _it.hasNext(); ) {
            if (_first) {
                _first = false;
            } else {
                _sb.append(", ");
            }
            _sb.append(_it.next().toString());
        }
        return _sb.append(this.dumpOutstandingTransactions()).toString();
    }

    /** Returns a string containing a comma-separated list (including
     * currently uncommited entries) of this symboltable's entries, with
     * both symbol names and values: eg "(name value), (name2, value2)",
     * followed by a list of non-commited transactions (see this class'
     * <tt>dumpOutstandingTransactions()</tt> method).
     */
    public String dumpAll() {
        StringBuffer _sb = new StringBuffer();
        boolean _first = true;
        for (Iterator _it = this.iterator(); _it.hasNext(); ) {
            if (_first) {
                _first = false;
            } else {
                _sb.append(", ");
            }
            Symbol _s = (Symbol) _it.next();
            _sb.append("(" + _s.toString() + " " + _s.dumpValue() + ")");
        }
        return _sb.append(this.dumpOutstandingTransactions()).toString();
    }

    /** Returns a string containing symbol names of all outstanding
     * transactions, or an empty string if there were no active
     * transactions.
     * Transaction sessions are preceeded by a numbered "T" and a colon.
     * E.g.: <tt>T0:name1 name2 T1:name3 name4 T2:T3:name5</tt>, where
     * transaction session 3 (T2) is empty.
     */
    public String dumpOutstandingTransactions() {
        if (false == this.transactionActive) {
            return "";
        }
        StringBuffer _sb = new StringBuffer().append(' ');
        int _t = 0;
        for (Iterator _it = this.sessions.iterator(); _it.hasNext(); ) {
            _sb.append(_t).append(':');
            for (Iterator _li = ((List) _it.next()).iterator(); _li.hasNext(); ) {
                _sb.append((String) _li.next()).append(' ');
            }
        }
        return _sb.toString();
    }

    public String dumpCurrentTransactionSession() {
        if (false == this.transactionActive) {
            return "<none>";
        }
        StringBuffer _sb = new StringBuffer().append(' ');
        for (Iterator _li = ((List) this.sessions.peek()).iterator(); _li.hasNext(); ) {
            _sb.append((String) _li.next()).append(' ');
        }
        return _sb.toString();
    }

    /** Clear symbol table.  Only used internally in this package!
     */
    void clear() {
        synchronized (this.tempLock) {
            if (null != this.table) {
                this.table.clear();
                this.table = null;
            }
            this.getTempTable().clear();
            if (null != this.sessions) {
                while (false == this.sessions.empty()) {
                    ((List) this.sessions.pop()).clear();
                }
                this.sessions.clear();
                this.sessions = null;
            }
        }
    }
}
