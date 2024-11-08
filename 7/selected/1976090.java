package net.sf.RecordEditor.utils.fileStorage;

import java.math.BigInteger;

public class RecordStoreVariableLength extends RecordStoreBase<RecordStoreVariableLength> {

    private static final int INDEX_RECORD = 16;

    private int maxLen;

    private int[] recIndex = null;

    public RecordStoreVariableLength(int length, int recLen, int recs) {
        super(recs, calcBytes(recLen));
        if (length >= 0) {
            this.store = new byte[length];
        }
        this.maxLen = Math.max(1, recLen);
    }

    private static int calcBytes(int len) {
        int ret = 1;
        if (len > 65025) {
            ret = 3;
        } else if (len > 255) {
            ret = 2;
        }
        return ret;
    }

    @Override
    public void add(int idx, byte[] rec) {
        super.add(idx, rec);
        size += rec.length + super.lengthSize;
        if (recIndex != null && (idx < recordCount - 1 || idx % INDEX_RECORD == INDEX_RECORD - 1)) {
            buildIndex();
        }
    }

    @Override
    public byte[] getCompressed() {
        byte[] b = super.getCompressed();
        if (store == null) {
            recIndex = null;
        }
        return b;
    }

    @Override
    public int remove(int idx) {
        int len = super.remove(idx);
        size -= len;
        buildIndex();
        return len;
    }

    @Override
    public void put(int idx, byte[] rec) {
        LineDtls p = getPosLen(idx, rec.length);
        if (idx >= recordCount) {
            size += rec.length + super.lengthSize;
        } else if (rec.length != p.len) {
            super.moveData(p.pos + p.len, p.pos + rec.length);
            if (recIndex != null) {
                synchronized (recIndex) {
                    int index = idx / INDEX_RECORD;
                    int diff = rec.length - p.len;
                    for (int i = Math.max(0, index); i < recordCount / INDEX_RECORD; i++) {
                        recIndex[i] += diff;
                    }
                }
            }
            size += rec.length - p.len;
        }
        put(p, rec);
    }

    @Override
    protected void put(LineDtls p, byte[] rec) {
        System.arraycopy(rec, 0, store, p.pos, rec.length);
        addLength(p.pos, rec.length);
    }

    private void addLength(int pos, int length) {
        byte[] b = BigInteger.valueOf(length).toByteArray();
        store[pos - 1] = b[b.length - 1];
        switch(super.lengthSize) {
            case 3:
                store[pos - 3] = 0;
                if (b.length > 2) {
                    store[pos - 3] = b[b.length - 3];
                }
            case 2:
                store[pos - 2] = 0;
                if (b.length > 1) {
                    store[pos - 2] = b[b.length - 2];
                }
        }
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    protected LineDtls getPosLen(int idx, int newLen) {
        int lastP;
        int l = super.lengthSize;
        if (idx >= super.recordCount) {
            lastP = size;
            l = 0;
        } else {
            int p = 0;
            int index = idx / INDEX_RECORD - 1;
            int count = idx % INDEX_RECORD;
            if (index >= 0) {
                if (recIndex == null) {
                    buildIndex();
                }
                p = recIndex[index];
            }
            lastP = p;
            for (int i = 0; i <= count; i++) {
                lastP = p;
                l = getLength(p);
                p += l + super.lengthSize;
            }
        }
        return new LineDtls(lastP + super.lengthSize, l, newLen, idx);
    }

    protected void buildIndex() {
        if (recordCount < INDEX_RECORD) {
            return;
        }
        int i, l;
        int j = 0;
        int k = 0;
        int p = 0;
        if (recIndex == null || recIndex.length < super.recordCount / INDEX_RECORD) {
            recIndex = new int[Math.max(super.getRecordCount(), store.length / maxLen) / INDEX_RECORD + 10];
        }
        synchronized (recIndex) {
            for (i = 0; i < super.recordCount; i++) {
                l = getLength(p);
                p += l + super.lengthSize;
                j += 1;
                if (j >= INDEX_RECORD) {
                    j = 0;
                    if (k < recIndex.length) {
                        recIndex[k++] = p;
                    }
                }
            }
            size = p;
        }
    }

    private int getLength(int p) {
        byte[] b = { 0, 0, 0, 0 };
        if (p + super.lengthSize > store.length) {
            return 0;
        }
        b[4 - super.lengthSize] = store[p];
        if (super.lengthSize == 2) {
            b[3] = store[p + 1];
        }
        if (super.lengthSize > 2) {
            b[2] = store[p + 1];
            b[3] = store[p + 2];
        }
        return (new BigInteger(b)).intValue();
    }

    @Override
    public RecordStoreVariableLength[] split(int blockSize) {
        buildIndex();
        RecordStoreVariableLength[] ret = new RecordStoreVariableLength[(size - 1) / blockSize + 1];
        int used = 0;
        int last = -1;
        int j = 0;
        for (int i = 0; i < recordCount / INDEX_RECORD; i++) {
            if (recIndex[i] - used > blockSize) {
                ret[j] = new RecordStoreVariableLength(recIndex[i] - used, maxLen, (i - last) * INDEX_RECORD);
                System.arraycopy(store, used, ret[j].store, 0, recIndex[i] - used);
                ret[j].size = recIndex[i] - used;
                used = recIndex[i];
                last = i;
                j += 1;
            }
        }
        if (used != size) {
            int l = size - used;
            ret[j] = new RecordStoreVariableLength(l, maxLen, recordCount - (last + 1) * INDEX_RECORD);
            System.arraycopy(store, used, ret[j].store, 0, l);
            ret[j].size = l;
        }
        return ret;
    }
}
