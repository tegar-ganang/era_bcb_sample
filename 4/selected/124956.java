package com.directmodelling.stm.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import com.directmodelling.api.Value;
import com.directmodelling.stm.Storage;

public class TransactionImpl extends VersionImpl {

    protected HashMap<Value<?>, Object> reads = new HashMap<Value<?>, Object>();

    public TransactionImpl() {
        this(null);
    }

    public TransactionImpl(final Storage parentTransaction) {
        super(parentTransaction);
    }

    @Override
    public TransactionImpl createChild() {
        return new TransactionImpl(this);
    }

    @Override
    public <T> T get(final Value<T> v) {
        final Object a = values.get(v);
        if (null != a) {
            return a == nullMarker ? null : (T) a;
        }
        final Object b = reads.get(v);
        if (null != b) {
            return b == nullMarker ? null : (T) b;
        }
        final T val = parent.get(v);
        reads.put(v, val == null ? nullMarker : val);
        return val;
    }

    /**
	 * Try to accept all changes in t. This will work when none of the reads in
	 * t have been changed already. This only makes sense when both transactions
	 * emerge from the same parent.
	 * 
	 * @return whether the commit succeeded.
	 */
    public boolean mergeAfter(final TransactionImpl t) {
        assert t.parent == parent;
        return mergeAfter(t.getReads(), t.getWrites());
    }

    public HashMap<Value<?>, Object> getReads() {
        return reads;
    }

    @Override
    public void reset() {
        super.reset();
        reads.clear();
    }

    /**
	 * Like {@link TransactionImpl#mergeAfter(TransactionImpl)}, only reads and
	 * writes are specified seperately.
	 */
    public boolean mergeAfter(final Map<Value<?>, Object> reads, final Map<Value.Mutable<?>, Object> writes) {
        final boolean success = Collections.disjoint(getWrites().keySet(), reads.keySet());
        if (success) {
            getWrites().putAll(writes);
            reads.putAll(reads);
        }
        return success;
    }

    /** Writes all values to the given storage. */
    @Override
    public void commitTo(final Storage other) {
        for (final Entry<Value<?>, Object> entry : reads.entrySet()) {
            if (!equals(other.get(entry.getKey()), entry.getValue())) {
                throw new CommitAbortedException("Value of " + entry.getKey() + " was changed: expected (" + entry.getValue() + ") but got (" + other.get(entry.getKey()) + ").");
            }
        }
        super.commitTo(other);
    }

    private boolean equals(final Object a, final Object b) {
        if (a == b) {
            return true;
        }
        if (null == a) {
            return false;
        }
        return a.equals(b);
    }
}
