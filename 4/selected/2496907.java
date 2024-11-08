package org.dml.tools;

import java.util.HashMap;
import org.dml.level010.Symbol;

/**
 * 
 *
 */
public class TwoWayHashMap<KEY, DATA> {

    private final HashMap<KEY, DATA> keyData = new HashMap<KEY, DATA>();

    private final HashMap<DATA, KEY> dataKey = new HashMap<DATA, KEY>();

    /**
	 * @param key
	 * @return true if existed
	 */
    public boolean removeByKey(KEY key) {
        RunTime.assumedNotNull(key);
        DATA data = keyData.get(key);
        if (null != data) {
            KEY key2 = dataKey.get(data);
            RunTime.assumedNotNull(key2);
            RunTime.assumedTrue(key == key2);
            RunTime.assumedTrue(keyData.remove(key) == data);
            RunTime.assumedTrue(dataKey.remove(data) == key2);
            return true;
        }
        return false;
    }

    /**
	 * @param key
	 * @return null if not found
	 */
    public DATA getData(KEY key) {
        RunTime.assumedNotNull(key);
        DATA data = keyData.get(key);
        if (null != data) {
            KEY key2 = dataKey.get(data);
            RunTime.assumedTrue(key == key2);
        }
        return data;
    }

    /**
	 * @param data
	 * @return null if not found
	 */
    public KEY getKey(DATA data) {
        RunTime.assumedNotNull(data);
        KEY key = dataKey.get(data);
        if (null != key) {
            DATA data2 = keyData.get(key);
            RunTime.assumedTrue(data == data2);
        }
        return key;
    }

    /**
	 * @param key
	 * @param data
	 * @return true if already existed
	 */
    public boolean ensure(KEY key, DATA data) {
        RunTime.assumedNotNull(key, data);
        DATA d1 = keyData.put(key, data);
        if (null != d1) {
            if (d1 != data) {
                RunTime.badCall("You attempted to overwrite an already existing key with a new value. Not acceptable");
            }
        }
        KEY k1 = dataKey.put(data, key);
        if (null == d1) {
            RunTime.assumedNull(k1);
        } else {
            RunTime.assumedTrue(k1 == key);
        }
        return (null != d1);
    }

    public void clear() {
        keyData.clear();
        dataKey.clear();
    }
}
