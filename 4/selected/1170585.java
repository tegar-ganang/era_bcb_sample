package org.hypergraphdb.transaction;

import java.util.List;
import org.hypergraphdb.util.HGSortedSet;
import org.hypergraphdb.util.RefCountedMap;
import org.hypergraphdb.util.RefResolver;

public class TxCacheSet<Key, E> extends TxSet<E> {

    private Key key;

    private RefCountedMap<Key, SetTxBox<E>> writeMap;

    private RefResolver<Key, ? extends HGSortedSet<E>> loader;

    @SuppressWarnings("unchecked")
    VBoxBody<HGSortedSet<E>> insertBody(long txNumber, HGSortedSet<E> x) {
        txManager.COMMIT_LOCK.lock();
        try {
            if (txNumber >= txManager.mostRecentRecord.transactionNumber) {
                if (S.body.version == -1) {
                    S.body = S.makeNewBody(x, txNumber, S.body.next);
                } else if (S.body.version < txNumber) S.body = S.makeNewBody(x, txNumber, S.body);
                return S.body;
            } else {
                if (S.body.version == -1) {
                    if (txNumber >= ((CacheSetTxBox<Key, E>) S).loadedAt) {
                        S.body = S.makeNewBody(x, ((CacheSetTxBox<Key, E>) S).loadedAt, S.body.next);
                        return S.body;
                    }
                }
                VBoxBody<HGSortedSet<E>> currentBody = S.body;
                while (currentBody.next != null && currentBody.next.version > txNumber) currentBody = currentBody.next;
                if (currentBody.next != null && currentBody.next.version == txNumber) return currentBody.next;
                VBoxBody<HGSortedSet<E>> newBody = S.makeNewBody(x, txNumber, currentBody.next);
                currentBody.setNext(newBody);
                return newBody;
            }
        } finally {
            txManager.COMMIT_LOCK.unlock();
        }
    }

    VBoxBody<HGSortedSet<E>> load(long txNumber) {
        HGSortedSet<E> x = loader.resolve(key);
        return insertBody(txNumber, x);
    }

    @Override
    HGSortedSet<E> read() {
        HGTransaction tx = txManager.getContext().getCurrent();
        if (tx == null) return S.body.value;
        HGSortedSet<E> x = tx.getLocalValue(S);
        if (x == null) {
            VBoxBody<HGSortedSet<E>> b = S.body;
            if (b.version <= tx.getNumber()) {
                if (b.value == null) b = load(tx.getNumber());
            } else {
                while (b.version > tx.getNumber() && b.next != null) b = b.next;
                if (b.version != tx.getNumber()) b = load(tx.getNumber());
            }
            if (!tx.isReadOnly()) tx.bodiesRead.put(S, b);
            return b.value;
        } else {
            return x == HGTransaction.NULL_VALUE ? null : x;
        }
    }

    @Override
    HGSortedSet<E> write() {
        List<LogEntry> log = txManager.getContext().getCurrent().getAttribute(S);
        if (log == null) {
            HGSortedSet<E> readOnly = read();
            HGSortedSet<E> writeable = cloneSet(readOnly);
            S.put(writeable);
            writeMap.put(key, S);
        }
        return S.get();
    }

    public TxCacheSet(final HGTransactionManager txManager, final HGSortedSet<E> backingSet, final Key key, final RefResolver<Key, ? extends HGSortedSet<E>> loader, final RefCountedMap<Key, SetTxBox<E>> writeMap) {
        this.txManager = txManager;
        this.key = key;
        this.loader = loader;
        this.writeMap = writeMap;
        HGTransaction tx = txManager.getContext().getCurrent();
        if (tx == null) {
            S = new CacheSetTxBox<Key, E>(txManager, backingSet, this);
            return;
        }
        S = writeMap.get(key);
        long txNumber = tx.getNumber();
        if (S == null) {
            S = new CacheSetTxBox<Key, E>(txManager, backingSet, this);
            S.body = S.makeNewBody(backingSet, txNumber, null);
            if (txNumber < txManager.mostRecentRecord.transactionNumber) S.body = S.makeNewBody(null, -1, S.body);
        } else insertBody(txNumber, backingSet);
    }

    public static class CacheSetTxBox<Key, E> extends SetTxBox<E> {

        long loadedAt;

        CacheSetTxBox(final HGTransactionManager txManager, final HGSortedSet<E> backingSet, final TxSet<E> thisSet) {
            super(txManager, backingSet, thisSet);
            loadedAt = txManager.mostRecentRecord.transactionNumber;
        }

        @SuppressWarnings("unchecked")
        HGSortedSet<E> getLastCommitted(HGTransaction tx) {
            TxCacheSet<Key, E> s = (TxCacheSet<Key, E>) thisSet;
            HGSortedSet<E> lastCommitted = super.getLastCommitted(tx);
            return (lastCommitted == null) ? s.load(tx.getNumber()).value : lastCommitted;
        }

        @Override
        public VBoxBody<HGSortedSet<E>> commit(HGTransaction tx, HGSortedSet<E> newvalue, long txNumber) {
            VBoxBody<HGSortedSet<E>> latest = super.commit(tx, newvalue, txNumber);
            if (latest.next != null && latest.next.version == -1) latest.setNext(latest.next.next);
            return latest;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void finish(HGTransaction tx) {
            if (tx.getAttribute(this) != null) {
                TxCacheSet<Key, E> s = (TxCacheSet<Key, E>) thisSet;
                s.writeMap.remove(s.key);
            }
        }
    }
}
