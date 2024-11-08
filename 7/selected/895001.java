package simtools.data;

import java.util.HashMap;
import simtools.data.buffer.Buffer;
import simtools.data.buffer.BufferedDataSource;
import simtools.data.buffer.DelayedBuffer;
import simtools.util.NumberStringComparator;

public class DynamicDataSourceCollection extends DataSourceCollection {

    /** This class contains info necessary to handle the data sources.
	 *  The vector has the same size as this object (reminder: Vector inheritance).
	 */
    protected class SourceInfo {

        public DataInfo info;

        /** See valueProvider 
		 */
        public int kind;

        /** Whether or not values were set, or if they are unititialized. Itself false when uninitialized, so OK
		 */
        boolean hasValue;

        public byte currentByte, cachedByte;

        public byte minByte, maxByte;

        public short currentShort, cachedShort;

        public short minShort, maxShort;

        public int currentInteger, cachedInteger;

        public int minInteger, maxInteger;

        public long currentLong, cachedLong;

        public long minLong, maxLong;

        public float currentFloat, cachedFloat;

        public float minFloat, maxFloat;

        public double currentDouble, cachedDouble;

        public double minDouble, maxDouble;

        public Object currentObject, cachedObject;

        public Object minObject, maxObject;
    }

    protected SourceInfo sourceInfo[];

    protected DataInfo ourInfo;

    public void bufferize(Buffer b) throws UnsupportedOperation {
        if (!(b instanceof DelayedBuffer)) throw new UnsupportedOperation();
        super.bufferize(b);
    }

    public void bufferize(int i, Buffer b) throws UnsupportedOperation {
        if (!(b instanceof DelayedBuffer)) throw new UnsupportedOperation();
        super.bufferize(i, b);
    }

    public void bufferize(int i) throws UnsupportedOperation {
        bufferize(i, new DelayedBuffer());
    }

    /** Other functions have a reasonable default implementation
	*/
    public DataInfo getInformation(int i) {
        return sourceInfo[i].info;
    }

    public Object getMin(int i) throws UnsupportedOperation {
        if (sourceInfo[i].hasValue == false) throw new UnsupportedOperation();
        switch(sourceInfo[i].kind) {
            case ValueProvider.ByteProvider:
                return new Byte(sourceInfo[i].minByte);
            case ValueProvider.ShortProvider:
                return new Short(sourceInfo[i].minShort);
            case ValueProvider.IntegerProvider:
                return new Integer(sourceInfo[i].minInteger);
            case ValueProvider.LongProvider:
                return new Long(sourceInfo[i].minLong);
            case ValueProvider.FloatProvider:
                return new Float(sourceInfo[i].minFloat);
            case ValueProvider.DoubleProvider:
                return new Double(sourceInfo[i].minDouble);
            case ValueProvider.ObjectProvider:
                return sourceInfo[i].minObject;
        }
        return null;
    }

    public Object getMax(int i) throws UnsupportedOperation {
        if (sourceInfo[i].hasValue == false) throw new UnsupportedOperation();
        switch(sourceInfo[i].kind) {
            case ValueProvider.ByteProvider:
                return new Byte(sourceInfo[i].maxByte);
            case ValueProvider.ShortProvider:
                return new Short(sourceInfo[i].maxShort);
            case ValueProvider.IntegerProvider:
                return new Integer(sourceInfo[i].maxInteger);
            case ValueProvider.LongProvider:
                return new Long(sourceInfo[i].maxLong);
            case ValueProvider.FloatProvider:
                return new Float(sourceInfo[i].maxFloat);
            case ValueProvider.DoubleProvider:
                return new Double(sourceInfo[i].maxDouble);
            case ValueProvider.ObjectProvider:
                return sourceInfo[i].maxObject;
        }
        return null;
    }

    public byte getByteMin(int i) throws DataException {
        if (sourceInfo[i].kind == ValueProvider.ByteProvider) return sourceInfo[i].minByte;
        return super.getByteMin(i);
    }

    public byte getByteMax(int i) throws DataException {
        if (sourceInfo[i].kind == ValueProvider.ByteProvider) return sourceInfo[i].maxByte;
        return super.getByteMax(i);
    }

    public short getShortMin(int i) throws DataException {
        if (sourceInfo[i].kind == ValueProvider.ShortProvider) return sourceInfo[i].minShort;
        return super.getShortMin(i);
    }

    public short getShortMax(int i) throws DataException {
        if (sourceInfo[i].kind == ValueProvider.ShortProvider) return sourceInfo[i].maxShort;
        return super.getShortMax(i);
    }

    public int getIntegerMin(int i) throws DataException {
        if (sourceInfo[i].kind == ValueProvider.IntegerProvider) return sourceInfo[i].minInteger;
        return super.getIntegerMin(i);
    }

    public int getIntegerMax(int i) throws DataException {
        if (sourceInfo[i].kind == ValueProvider.IntegerProvider) return sourceInfo[i].maxInteger;
        return super.getIntegerMax(i);
    }

    public long getLongMin(int i) throws DataException {
        if (sourceInfo[i].kind == ValueProvider.LongProvider) return sourceInfo[i].minLong;
        return super.getLongMin(i);
    }

    public long getLongMax(int i) throws DataException {
        if (sourceInfo[i].kind == ValueProvider.LongProvider) return sourceInfo[i].maxLong;
        return super.getLongMax(i);
    }

    public float getFloatMin(int i) throws DataException {
        if (sourceInfo[i].kind == ValueProvider.FloatProvider) return sourceInfo[i].minFloat;
        return super.getFloatMin(i);
    }

    public float getFloatMax(int i) throws DataException {
        if (sourceInfo[i].kind == ValueProvider.FloatProvider) return sourceInfo[i].maxFloat;
        return super.getFloatMax(i);
    }

    public double getDoubleMin(int i) throws DataException {
        if (sourceInfo[i].kind == ValueProvider.DoubleProvider) return sourceInfo[i].minDouble;
        return super.getDoubleMin(i);
    }

    public double getDoubleMax(int i) throws DataException {
        if (sourceInfo[i].kind == ValueProvider.DoubleProvider) return sourceInfo[i].maxDouble;
        return super.getDoubleMax(i);
    }

    public Object computeMin(int i) throws UnsupportedOperation {
        if (sourceInfo[i].hasValue == false) throw new UnsupportedOperation();
        if ((buffers == null) || (buffers[i] == null)) {
            switch(sourceInfo[i].kind) {
                case ValueProvider.ByteProvider:
                    sourceInfo[i].minByte = sourceInfo[i].cachedByte;
                    return new Byte(sourceInfo[i].minByte);
                case ValueProvider.ShortProvider:
                    sourceInfo[i].minShort = sourceInfo[i].cachedShort;
                    return new Short(sourceInfo[i].minShort);
                case ValueProvider.IntegerProvider:
                    sourceInfo[i].minInteger = sourceInfo[i].cachedInteger;
                    return new Integer(sourceInfo[i].minInteger);
                case ValueProvider.LongProvider:
                    sourceInfo[i].minLong = sourceInfo[i].cachedLong;
                    return new Long(sourceInfo[i].minLong);
                case ValueProvider.FloatProvider:
                    sourceInfo[i].minFloat = sourceInfo[i].cachedFloat;
                    return new Float(sourceInfo[i].minFloat);
                case ValueProvider.DoubleProvider:
                    sourceInfo[i].minDouble = sourceInfo[i].cachedDouble;
                    return new Double(sourceInfo[i].minDouble);
                case ValueProvider.ObjectProvider:
                    sourceInfo[i].minObject = sourceInfo[i].cachedObject;
                    return sourceInfo[i].minObject;
            }
            return null;
        }
        int order = sortedOrder(i);
        switch(sourceInfo[i].kind) {
            case ValueProvider.ByteProvider:
                try {
                    if (order == 1) sourceInfo[i].minByte = buffers[i].getByteValue(buffers[i].getStartIndex()); else if (order == -1) sourceInfo[i].minByte = buffers[i].getByteValue(buffers[i].getEndIndex()); else sourceInfo[i].minByte = sourceInfo[i].cachedByte;
                    return new Byte(sourceInfo[i].minByte);
                } catch (DataException e) {
                    sourceInfo[i].minByte = sourceInfo[i].cachedByte;
                    return new Byte(sourceInfo[i].minByte);
                }
            case ValueProvider.ShortProvider:
                try {
                    if (order == 1) sourceInfo[i].minShort = buffers[i].getShortValue(buffers[i].getStartIndex()); else if (order == -1) sourceInfo[i].minShort = buffers[i].getShortValue(buffers[i].getEndIndex()); else sourceInfo[i].minShort = sourceInfo[i].cachedShort;
                    return new Short(sourceInfo[i].minShort);
                } catch (DataException e) {
                    sourceInfo[i].minShort = sourceInfo[i].cachedShort;
                    return new Short(sourceInfo[i].minShort);
                }
            case ValueProvider.IntegerProvider:
                try {
                    if (order == 1) sourceInfo[i].minInteger = buffers[i].getIntegerValue(buffers[i].getStartIndex()); else if (order == -1) sourceInfo[i].minInteger = buffers[i].getIntegerValue(buffers[i].getEndIndex()); else sourceInfo[i].minInteger = sourceInfo[i].cachedInteger;
                    return new Integer(sourceInfo[i].minInteger);
                } catch (DataException e) {
                    sourceInfo[i].minInteger = sourceInfo[i].cachedInteger;
                    return new Integer(sourceInfo[i].minInteger);
                }
            case ValueProvider.LongProvider:
                try {
                    if (order == 1) sourceInfo[i].minLong = buffers[i].getLongValue(buffers[i].getStartIndex()); else if (order == -1) sourceInfo[i].minLong = buffers[i].getLongValue(buffers[i].getEndIndex()); else sourceInfo[i].minLong = sourceInfo[i].cachedLong;
                    return new Long(sourceInfo[i].minLong);
                } catch (DataException e) {
                    sourceInfo[i].minLong = sourceInfo[i].cachedLong;
                    return new Long(sourceInfo[i].minLong);
                }
            case ValueProvider.FloatProvider:
                try {
                    if (order == 1) sourceInfo[i].minFloat = buffers[i].getFloatValue(buffers[i].getStartIndex()); else if (order == -1) sourceInfo[i].minFloat = buffers[i].getFloatValue(buffers[i].getEndIndex()); else sourceInfo[i].minFloat = sourceInfo[i].cachedFloat;
                    return new Float(sourceInfo[i].minFloat);
                } catch (DataException e) {
                    sourceInfo[i].minFloat = sourceInfo[i].cachedFloat;
                    return new Float(sourceInfo[i].minFloat);
                }
            case ValueProvider.DoubleProvider:
                try {
                    if (order == 1) sourceInfo[i].minDouble = buffers[i].getDoubleValue(buffers[i].getStartIndex()); else if (order == -1) sourceInfo[i].minDouble = buffers[i].getDoubleValue(buffers[i].getEndIndex()); else sourceInfo[i].minDouble = sourceInfo[i].cachedDouble;
                    return new Double(sourceInfo[i].minDouble);
                } catch (DataException e) {
                    sourceInfo[i].minDouble = sourceInfo[i].cachedDouble;
                    return new Double(sourceInfo[i].minDouble);
                }
            case ValueProvider.ObjectProvider:
                try {
                    if (order == 1) sourceInfo[i].minObject = buffers[i].getObjectValue(buffers[i].getStartIndex()); else if (order == -1) sourceInfo[i].minObject = buffers[i].getObjectValue(buffers[i].getEndIndex()); else sourceInfo[i].minObject = sourceInfo[i].cachedObject;
                    return sourceInfo[i].minObject;
                } catch (DataException e) {
                    sourceInfo[i].minObject = sourceInfo[i].cachedObject;
                    return sourceInfo[i].minObject;
                }
        }
        return null;
    }

    public Object computeMax(int i) throws UnsupportedOperation {
        if (sourceInfo[i].hasValue == false) throw new UnsupportedOperation();
        if ((buffers == null) || (buffers[i] == null)) {
            switch(sourceInfo[i].kind) {
                case ValueProvider.ByteProvider:
                    sourceInfo[i].maxByte = sourceInfo[i].cachedByte;
                    return new Byte(sourceInfo[i].maxByte);
                case ValueProvider.ShortProvider:
                    sourceInfo[i].maxShort = sourceInfo[i].cachedShort;
                    return new Short(sourceInfo[i].maxShort);
                case ValueProvider.IntegerProvider:
                    sourceInfo[i].maxInteger = sourceInfo[i].cachedInteger;
                    return new Integer(sourceInfo[i].maxInteger);
                case ValueProvider.LongProvider:
                    sourceInfo[i].maxLong = sourceInfo[i].cachedLong;
                    return new Long(sourceInfo[i].maxLong);
                case ValueProvider.FloatProvider:
                    sourceInfo[i].maxFloat = sourceInfo[i].cachedFloat;
                    return new Float(sourceInfo[i].maxFloat);
                case ValueProvider.DoubleProvider:
                    sourceInfo[i].maxDouble = sourceInfo[i].cachedDouble;
                    return new Double(sourceInfo[i].maxDouble);
                case ValueProvider.ObjectProvider:
                    sourceInfo[i].maxObject = sourceInfo[i].cachedObject;
                    return sourceInfo[i].maxObject;
            }
            return null;
        }
        int order = sortedOrder(i);
        switch(sourceInfo[i].kind) {
            case ValueProvider.ByteProvider:
                try {
                    if (order == -1) sourceInfo[i].maxByte = buffers[i].getByteValue(buffers[i].getStartIndex()); else if (order == 1) sourceInfo[i].maxByte = buffers[i].getByteValue(buffers[i].getEndIndex()); else sourceInfo[i].maxByte = sourceInfo[i].cachedByte;
                    return new Byte(sourceInfo[i].maxByte);
                } catch (DataException e) {
                    sourceInfo[i].maxByte = sourceInfo[i].cachedByte;
                    return new Byte(sourceInfo[i].maxByte);
                }
            case ValueProvider.ShortProvider:
                try {
                    if (order == -1) sourceInfo[i].maxShort = buffers[i].getShortValue(buffers[i].getStartIndex()); else if (order == 1) sourceInfo[i].maxShort = buffers[i].getShortValue(buffers[i].getEndIndex()); else sourceInfo[i].maxShort = sourceInfo[i].cachedShort;
                    return new Short(sourceInfo[i].maxShort);
                } catch (DataException e) {
                    sourceInfo[i].maxShort = sourceInfo[i].cachedShort;
                    return new Short(sourceInfo[i].maxShort);
                }
            case ValueProvider.IntegerProvider:
                try {
                    if (order == -1) sourceInfo[i].maxInteger = buffers[i].getIntegerValue(buffers[i].getStartIndex()); else if (order == 1) sourceInfo[i].maxInteger = buffers[i].getIntegerValue(buffers[i].getEndIndex()); else sourceInfo[i].maxInteger = sourceInfo[i].cachedInteger;
                    return new Integer(sourceInfo[i].maxInteger);
                } catch (DataException e) {
                    sourceInfo[i].maxInteger = sourceInfo[i].cachedInteger;
                    return new Integer(sourceInfo[i].maxInteger);
                }
            case ValueProvider.LongProvider:
                try {
                    if (order == -1) sourceInfo[i].maxLong = buffers[i].getLongValue(buffers[i].getStartIndex()); else if (order == 1) sourceInfo[i].maxLong = buffers[i].getLongValue(buffers[i].getEndIndex()); else sourceInfo[i].maxLong = sourceInfo[i].cachedLong;
                    return new Long(sourceInfo[i].maxLong);
                } catch (DataException e) {
                    sourceInfo[i].maxLong = sourceInfo[i].cachedLong;
                    return new Long(sourceInfo[i].maxLong);
                }
            case ValueProvider.FloatProvider:
                try {
                    if (order == -1) sourceInfo[i].maxFloat = buffers[i].getFloatValue(buffers[i].getStartIndex()); else if (order == 1) sourceInfo[i].maxFloat = buffers[i].getFloatValue(buffers[i].getEndIndex()); else sourceInfo[i].maxFloat = sourceInfo[i].cachedFloat;
                    return new Float(sourceInfo[i].maxFloat);
                } catch (DataException e) {
                    sourceInfo[i].maxFloat = sourceInfo[i].cachedFloat;
                    return new Float(sourceInfo[i].maxFloat);
                }
            case ValueProvider.DoubleProvider:
                try {
                    if (order == -1) sourceInfo[i].maxDouble = buffers[i].getDoubleValue(buffers[i].getStartIndex()); else if (order == 1) sourceInfo[i].maxDouble = buffers[i].getDoubleValue(buffers[i].getEndIndex()); else sourceInfo[i].maxDouble = sourceInfo[i].cachedDouble;
                    return new Double(sourceInfo[i].maxDouble);
                } catch (DataException e) {
                    sourceInfo[i].maxDouble = sourceInfo[i].cachedDouble;
                    return new Double(sourceInfo[i].maxDouble);
                }
            case ValueProvider.ObjectProvider:
                try {
                    if (order == -1) sourceInfo[i].maxObject = buffers[i].getObjectValue(buffers[i].getStartIndex()); else if (order == 1) sourceInfo[i].maxObject = buffers[i].getObjectValue(buffers[i].getEndIndex()); else sourceInfo[i].maxObject = sourceInfo[i].cachedObject;
                    return sourceInfo[i].maxObject;
                } catch (DataException e) {
                    sourceInfo[i].maxObject = sourceInfo[i].cachedObject;
                    return sourceInfo[i].maxObject;
                }
        }
        return null;
    }

    public boolean isComparable(int i) {
        return false;
    }

    public long getStartIndex(int i) {
        if (buffers == null) return lastIndex;
        if ((i < buffers.length) && (buffers[i] != null)) return buffers[i].getStartIndex();
        return lastIndex;
    }

    public long getLastIndex(int i) {
        if (buffers == null) return lastIndex;
        if ((i < buffers.length) && (buffers[i] != null)) return buffers[i].getEndIndex();
        return lastIndex;
    }

    public long computeStartIndex(int i) {
        return getStartIndex(i);
    }

    public long computeLastIndex(int i) {
        return getLastIndex(i);
    }

    protected DataSource createDataSource(DataInfo info) {
        return createDataSource(info, ValueProvider.ObjectProvider);
    }

    /**
	 * Add a data source with the given info. Make sure you call this function before calling 
	 * setValue, or just after calling registerNewValues() and before setting the values 
	 * for the next round.
	 * @param info Any information that will describe the data source, null is possible but 
	 * not recommended.
	 * @param kind the data source kind, see ValueProvider. Object is default.
	 * @return DataSource
	 */
    protected DataSource createDataSource(DataInfo info, int kind) {
        int index = size();
        DataSource ret = new CollectiveDataSource(this, index);
        add(ret);
        if (map == null) {
            map = new HashMap();
        }
        map.put(info.id, ret);
        if (sourceInfo == null) sourceInfo = new SourceInfo[index + 1]; else {
            SourceInfo[] tmp = new SourceInfo[index + 1];
            System.arraycopy(sourceInfo, 0, tmp, 0, index);
            sourceInfo = tmp;
        }
        sourceInfo[index] = new SourceInfo();
        sourceInfo[index].info = info;
        sourceInfo[index].kind = kind;
        if (buffers != null) {
            Buffer[] b = new Buffer[index + 1];
            System.arraycopy(buffers, 0, b, 0, index);
            buffers = b;
            buffers[index] = null;
        }
        notifyListenersForDataSourceAdded(ret);
        return ret;
    }

    /**
	 * Remove the given data source. Make sure you call this function before calling 
	 * setValue, or just after calling registerNewValues() and before setting the values 
	 * for the next round.
	 * @param ds The data source to remove
	 */
    protected void removeDataSource(int i) {
        DataSource ds = (DataSource) get(i);
        if (sourceInfo != null) {
            SourceInfo[] si = new SourceInfo[sourceInfo.length - 1];
            for (int j = 0; j < i; ++j) si[j] = sourceInfo[j];
            for (int j = i; j < si.length; ++j) si[j] = sourceInfo[j + 1];
            sourceInfo = si;
        }
        if (buffers != null) {
            Buffer[] bf = new Buffer[buffers.length - 1];
            for (int j = 0; j < i; ++j) bf[j] = buffers[j];
            for (int j = i; j < bf.length; ++j) bf[j] = buffers[j + 1];
            buffers = bf;
        }
        remove(i);
        for (; i < size(); ++i) {
            Object o = get(i);
            if (o instanceof BufferedDataSource) {
                o = ((BufferedDataSource) o).dataSource;
            }
            if (o instanceof CollectiveDataSource) {
                ((CollectiveDataSource) o).myIndex = i;
            }
        }
        if (ds instanceof BufferedDataSource) {
            ds = ((BufferedDataSource) ds).dataSource;
        }
        if (ds instanceof CollectiveDataSource) {
            ((CollectiveDataSource) ds).myIndex = -1;
        }
        notifyListenersForDataSourceRemoved(ds);
    }

    protected void removeDataSource(DataSource ds) {
        int i = indexOf(ds);
        if (i == -1) return;
        removeDataSource(i);
    }

    /**
	 * Change the information relative to a datasource. May be called any time, and will notify
	 * the data source listeners.
	 */
    protected void changeDataSourceInfo(DataSource ds, DataInfo di) {
        int i = indexOf(ds);
        sourceInfo[i].info = di;
        ds.notifyListenersForInfoChange(di);
    }

    /**
	 * Change our own information. May be called any time, and will notify
	 * the our listeners.
	 */
    protected void changeInfo(DataInfo di) {
        ourInfo = di;
        notifyListenersForInfoChange(di);
    }

    /**
	 * Sets the value of the Datasource numbered dsnum to the byte v
	 * Once you call this function, you should not add/remove any datasource so long
	 * as you haven't called registerNewValues(). Only add/remove datasources before 
	 * calling setValue.
	 * @param dsnum
	 * @param v the byte value to set
	 */
    protected void setByteValue(int dsnum, byte v) {
        sourceInfo[dsnum].currentByte = v;
    }

    /**
	 * Sets the value of the Datasource numbered dsnum to the short v
	 * Once you call this function, you should not add/remove any datasource so long
	 * as you haven't called registerNewValues(). Only add/remove datasources before 
	 * calling setValue.
	 * @param dsnum
	 * @param v the short value to set
	 */
    protected void setShortValue(int dsnum, short v) {
        sourceInfo[dsnum].currentShort = v;
    }

    /**
	 * Sets the value of the Datasource numbered dsnum to the int v
	 * Once you call this function, you should not add/remove any datasource so long
	 * as you haven't called registerNewValues(). Only add/remove datasources before 
	 * calling setValue.
	 * @param dsnum
	 * @param v the int value to set
	 */
    protected void setIntegerValue(int dsnum, int v) {
        sourceInfo[dsnum].currentInteger = v;
    }

    /**
	 * Sets the value of the Datasource numbered dsnum to the long v
	 * Once you call this function, you should not add/remove any datasource so long
	 * as you haven't called registerNewValues(). Only add/remove datasources before 
	 * calling setValue.
	 * @param dsnum
	 * @param v the long value to set
	 */
    protected void setLongValue(int dsnum, long v) {
        sourceInfo[dsnum].currentLong = v;
    }

    /**
	 * Sets the value of the Datasource numbered dsnum to the float v
	 * Once you call this function, you should not add/remove any datasource so long
	 * as you haven't called registerNewValues(). Only add/remove datasources before 
	 * calling setValue.
	 * @param dsnum
	 * @param v the float value to set
	 */
    protected void setFloatValue(int dsnum, float v) {
        sourceInfo[dsnum].currentFloat = v;
    }

    /**
	 * Sets the value of the Datasource numbered dsnum to the double v
	 * Once you call this function, you should not add/remove any datasource so long
	 * as you haven't called registerNewValues(). Only add/remove datasources before 
	 * calling setValue.
	 * @param dsnum
	 * @param v the double value to set
	 */
    protected void setDoubleValue(int dsnum, double v) {
        sourceInfo[dsnum].currentDouble = v;
    }

    /**
	 * Sets the value of the Datasource numbered dsnum to the Object v
	 * Once you call this function, you should not add/remove any datasource so long
	 * as you haven't called registerNewValues(). Only add/remove datasources before 
	 * calling setValue.
	 * @param dsnum
	 * @param v the Object value to set
	 */
    protected void setObjectValue(int dsnum, Object v) {
        sourceInfo[dsnum].currentObject = v;
    }

    /**
	 * Call this function once you have set up all the values with setValue.
	 * Unset data sources for this round will keep their old values.
	 * @param dsnum
	 * @param v
	 */
    protected void registerNewValues() {
        lastIndex++;
        for (int i = 0; i < size(); ++i) {
            boolean vRangeChanged = false;
            switch(sourceInfo[i].kind) {
                case ValueProvider.ByteProvider:
                    sourceInfo[i].cachedByte = sourceInfo[i].currentByte;
                    if ((buffers != null) && (buffers[i] != null)) try {
                        buffers[i].setByteValue(lastIndex, sourceInfo[i].cachedByte);
                    } catch (DataException e) {
                    }
                    if (sourceInfo[i].hasValue == false) {
                        sourceInfo[i].hasValue = true;
                        sourceInfo[i].minByte = sourceInfo[i].maxByte = sourceInfo[i].cachedByte;
                        vRangeChanged = true;
                    } else {
                        if (sortedOrder(i) != 0) {
                            try {
                                computeMin(i);
                                computeMax(i);
                            } catch (UnsupportedOperation e) {
                            }
                            vRangeChanged = true;
                        } else {
                            if (sourceInfo[i].cachedByte < sourceInfo[i].minByte) {
                                sourceInfo[i].minByte = sourceInfo[i].cachedByte;
                                vRangeChanged = true;
                            }
                            if (sourceInfo[i].cachedByte > sourceInfo[i].maxByte) {
                                sourceInfo[i].maxByte = sourceInfo[i].cachedByte;
                                vRangeChanged = true;
                            }
                        }
                    }
                    break;
                case ValueProvider.ShortProvider:
                    sourceInfo[i].cachedShort = sourceInfo[i].currentShort;
                    if ((buffers != null) && (buffers[i] != null)) try {
                        buffers[i].setShortValue(lastIndex, sourceInfo[i].cachedShort);
                    } catch (DataException e) {
                    }
                    if (sourceInfo[i].hasValue == false) {
                        sourceInfo[i].hasValue = true;
                        sourceInfo[i].minShort = sourceInfo[i].maxShort = sourceInfo[i].cachedShort;
                        vRangeChanged = true;
                    } else {
                        if (sortedOrder(i) != 0) {
                            try {
                                computeMin(i);
                                computeMax(i);
                            } catch (UnsupportedOperation e) {
                            }
                            vRangeChanged = true;
                        } else {
                            if (sourceInfo[i].cachedShort < sourceInfo[i].minShort) {
                                sourceInfo[i].minShort = sourceInfo[i].cachedShort;
                                vRangeChanged = true;
                            }
                            if (sourceInfo[i].cachedShort > sourceInfo[i].maxShort) {
                                sourceInfo[i].maxShort = sourceInfo[i].cachedShort;
                                vRangeChanged = true;
                            }
                        }
                    }
                    break;
                case ValueProvider.IntegerProvider:
                    sourceInfo[i].cachedInteger = sourceInfo[i].currentInteger;
                    if ((buffers != null) && (buffers[i] != null)) try {
                        buffers[i].setIntegerValue(lastIndex, sourceInfo[i].cachedInteger);
                    } catch (DataException e) {
                    }
                    if (sourceInfo[i].hasValue == false) {
                        sourceInfo[i].hasValue = true;
                        sourceInfo[i].minInteger = sourceInfo[i].maxInteger = sourceInfo[i].cachedInteger;
                        vRangeChanged = true;
                    } else {
                        if (sortedOrder(i) != 0) {
                            try {
                                computeMin(i);
                                computeMax(i);
                            } catch (UnsupportedOperation e) {
                            }
                            vRangeChanged = true;
                        } else {
                            if (sourceInfo[i].cachedInteger < sourceInfo[i].minInteger) {
                                sourceInfo[i].minInteger = sourceInfo[i].cachedInteger;
                                vRangeChanged = true;
                            }
                            if (sourceInfo[i].cachedInteger > sourceInfo[i].maxInteger) {
                                sourceInfo[i].maxInteger = sourceInfo[i].cachedInteger;
                                vRangeChanged = true;
                            }
                        }
                    }
                    break;
                case ValueProvider.LongProvider:
                    sourceInfo[i].cachedLong = sourceInfo[i].currentLong;
                    if ((buffers != null) && (buffers[i] != null)) try {
                        buffers[i].setLongValue(lastIndex, sourceInfo[i].cachedLong);
                    } catch (DataException e) {
                    }
                    if (sourceInfo[i].hasValue == false) {
                        sourceInfo[i].hasValue = true;
                        sourceInfo[i].minLong = sourceInfo[i].maxLong = sourceInfo[i].cachedLong;
                        vRangeChanged = true;
                    } else {
                        if (sortedOrder(i) != 0) {
                            try {
                                computeMin(i);
                                computeMax(i);
                            } catch (UnsupportedOperation e) {
                            }
                            vRangeChanged = true;
                        } else {
                            if (sourceInfo[i].cachedLong < sourceInfo[i].minLong) {
                                sourceInfo[i].minLong = sourceInfo[i].cachedLong;
                                vRangeChanged = true;
                            }
                            if (sourceInfo[i].cachedLong > sourceInfo[i].maxLong) {
                                sourceInfo[i].maxLong = sourceInfo[i].cachedLong;
                                vRangeChanged = true;
                            }
                        }
                    }
                    break;
                case ValueProvider.FloatProvider:
                    sourceInfo[i].cachedFloat = sourceInfo[i].currentFloat;
                    if ((buffers != null) && (buffers[i] != null)) try {
                        buffers[i].setFloatValue(lastIndex, sourceInfo[i].cachedFloat);
                    } catch (DataException e) {
                    }
                    if (sourceInfo[i].hasValue == false) {
                        sourceInfo[i].hasValue = true;
                        sourceInfo[i].minFloat = sourceInfo[i].maxFloat = sourceInfo[i].cachedFloat;
                        vRangeChanged = true;
                    } else {
                        if (sortedOrder(i) != 0) {
                            try {
                                computeMin(i);
                                computeMax(i);
                            } catch (UnsupportedOperation e) {
                            }
                            vRangeChanged = true;
                        } else {
                            if (sourceInfo[i].cachedFloat < sourceInfo[i].minFloat) {
                                sourceInfo[i].minFloat = sourceInfo[i].cachedFloat;
                                vRangeChanged = true;
                            }
                            if (sourceInfo[i].cachedFloat > sourceInfo[i].maxFloat) {
                                sourceInfo[i].maxFloat = sourceInfo[i].cachedFloat;
                                vRangeChanged = true;
                            }
                        }
                    }
                    break;
                case ValueProvider.DoubleProvider:
                    sourceInfo[i].cachedDouble = sourceInfo[i].currentDouble;
                    if ((buffers != null) && (buffers[i] != null)) try {
                        buffers[i].setDoubleValue(lastIndex, sourceInfo[i].cachedDouble);
                    } catch (DataException e) {
                    }
                    if (sourceInfo[i].hasValue == false) {
                        sourceInfo[i].hasValue = true;
                        sourceInfo[i].minDouble = sourceInfo[i].maxDouble = sourceInfo[i].cachedDouble;
                        vRangeChanged = true;
                    } else {
                        if (sortedOrder(i) != 0) {
                            try {
                                computeMin(i);
                                computeMax(i);
                            } catch (UnsupportedOperation e) {
                            }
                            vRangeChanged = true;
                        } else {
                            if (sourceInfo[i].cachedDouble < sourceInfo[i].minDouble) {
                                sourceInfo[i].minDouble = sourceInfo[i].cachedDouble;
                                vRangeChanged = true;
                            }
                            if (sourceInfo[i].cachedDouble > sourceInfo[i].maxDouble) {
                                sourceInfo[i].maxDouble = sourceInfo[i].cachedDouble;
                                vRangeChanged = true;
                            }
                        }
                    }
                    break;
                case ValueProvider.ObjectProvider:
                    sourceInfo[i].cachedObject = sourceInfo[i].currentObject;
                    if ((buffers != null) && (buffers[i] != null)) try {
                        buffers[i].setValue(lastIndex, sourceInfo[i].cachedObject);
                    } catch (DataException e) {
                    }
                    if (sourceInfo[i].hasValue == false) {
                        sourceInfo[i].hasValue = true;
                        sourceInfo[i].minObject = sourceInfo[i].maxObject = sourceInfo[i].cachedObject;
                        vRangeChanged = true;
                    } else {
                        if (sortedOrder(i) != 0) {
                            try {
                                computeMin(i);
                                computeMax(i);
                            } catch (UnsupportedOperation e) {
                            }
                            vRangeChanged = true;
                        } else {
                            try {
                                if (NumberStringComparator.numStringCompare(sourceInfo[i].cachedObject, sourceInfo[i].minObject) < 0) {
                                    sourceInfo[i].minObject = sourceInfo[i].cachedObject;
                                    vRangeChanged = true;
                                }
                            } catch (ClassCastException de) {
                            }
                            try {
                                if (NumberStringComparator.numStringCompare(sourceInfo[i].cachedObject, sourceInfo[i].maxObject) > 0) {
                                    sourceInfo[i].maxObject = sourceInfo[i].cachedObject;
                                    vRangeChanged = true;
                                }
                            } catch (ClassCastException de) {
                            }
                        }
                    }
                    break;
            }
            DataSource ds = (DataSource) get(i);
            ds.updateSortedOrder();
            if (vRangeChanged) ds.notifyListenersForValueRangeChange();
            ds.notifyListenersForIndexRangeChange(getStartIndex(i), getLastIndex(i));
        }
        notifyEndNotificationListeners();
    }

    public DataInfo getInformation() {
        return ourInfo;
    }

    public Object getValue(int i, long index) throws DataException {
        if (index == lastIndex) switch(sourceInfo[i].kind) {
            case ValueProvider.ByteProvider:
                return new Byte(sourceInfo[i].cachedByte);
            case ValueProvider.ShortProvider:
                return new Short(sourceInfo[i].cachedShort);
            case ValueProvider.IntegerProvider:
                return new Integer(sourceInfo[i].cachedInteger);
            case ValueProvider.LongProvider:
                return new Long(sourceInfo[i].cachedLong);
            case ValueProvider.FloatProvider:
                return new Float(sourceInfo[i].cachedFloat);
            case ValueProvider.DoubleProvider:
                return new Double(sourceInfo[i].cachedDouble);
            default:
                return sourceInfo[i].cachedObject;
        }
        if ((buffers == null) || (i >= buffers.length) || (buffers[i] == null)) throw new NoSuchIndex(index);
        long start = buffers[i].getStartIndex();
        long last = buffers[i].getEndIndex();
        if ((index < start) || (index > last)) throw new NoSuchIndex(index);
        return buffers[i].getValue(index);
    }

    public int getKind(int i) {
        return sourceInfo[i].kind;
    }

    public Class valueClass(int i) {
        switch(sourceInfo[i].kind) {
            case ValueProvider.ByteProvider:
                return Byte.class;
            case ValueProvider.ShortProvider:
                return Short.class;
            case ValueProvider.IntegerProvider:
                return Integer.class;
            case ValueProvider.LongProvider:
                return Long.class;
            case ValueProvider.FloatProvider:
                return Float.class;
            case ValueProvider.DoubleProvider:
                return Double.class;
        }
        return Object.class;
    }
}
