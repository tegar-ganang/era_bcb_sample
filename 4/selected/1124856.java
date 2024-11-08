package com.rbnb.sapi;

import java.io.ByteArrayOutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import com.rbnb.compat.StringTokenizer;
import com.rbnb.api.*;

public class ChannelMap {

    /**
	 * Least significant bytes are first in each point, ala Intel.
	 */
    public static final ByteOrderEnum LSB = new ByteOrderEnum(DataBlock.ORDER_LSB);

    /**
	 * Most significant bytes are first in each point, as is the case
	 *  with many UNIX machines, and Java data on export.
	 */
    public static final ByteOrderEnum MSB = new ByteOrderEnum(DataBlock.ORDER_MSB);

    /**
	 * The data is in whichever format is appropriate for this NATIVE
	 *  machine.  Please note that this is not appropriate for Java
	 *  generated data.
	 */
    public static final ByteOrderEnum LOCAL = new ByteOrderEnum(determineLocalByteOrder());

    public static final int TYPE_FLOAT64 = DataBlock.TYPE_FLOAT64, TYPE_FLOAT32 = DataBlock.TYPE_FLOAT32, TYPE_INT64 = DataBlock.TYPE_INT64, TYPE_INT32 = DataBlock.TYPE_INT32, TYPE_INT16 = DataBlock.TYPE_INT16, TYPE_INT8 = DataBlock.TYPE_INT8, TYPE_STRING = DataBlock.TYPE_STRING, TYPE_UNKNOWN = DataBlock.UNKNOWN, TYPE_BYTEARRAY = DataBlock.TYPE_BYTEARRAY, TYPE_USER = DataBlock.TYPE_USER;

    public ChannelMap() {
        initVariables();
        boolean temp = false;
        try {
        } catch (SecurityException se) {
        }
        debugFlag = temp;
    }

    public final int Add(String channelName) throws SAPIException {
        Channel ch = (Channel) channelMap.get(channelName);
        if (ch == null) {
            ch = new Channel(channelList.size(), channelName, null);
            channelList.addElement(ch);
            channelMap.put(channelName, ch);
            return channelList.size() - 1;
        }
        return ch.index;
    }

    /**
	  * Add a folder to this channel map.  Useful primarily for registration.
	  * The name may end in a slash '/', but this is not required.
	  * <p>
	  * @exception SAPIException If there is a problem parsing the channel name.
	  * @exception NullPointerException If the channel name is null.
	  * @see Source#Register(ChannelMap)
	  * @author WHF
	  * @since V2.1
	  */
    public final void AddFolder(String channelName) throws SAPIException {
        addIfFolder(channelName);
    }

    /**
	  * Method to handle folders, which may be overridden by 
	  *  PlugInChannelMap.
	  */
    void addIfFolder(String channelName) throws SAPIException {
        try {
            if (channelName.charAt(channelName.length() - 1) == Rmap.PATHDELIMITER) channelName = channelName.substring(0, channelName.length() - 1);
            if (folderMappings.containsKey(channelName)) return;
            baseFrame.addChannel(channelName);
            folderMappings.put(channelName, channelName);
        } catch (Exception e) {
            throw new SAPIException(e);
        }
    }

    public final void Clear() {
        channelList.removeAllElements();
        channelMap.clear();
        wasTimeout = false;
        folderArray = new String[0];
        folderMappings.clear();
        try {
            clearData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double[] GetTimes(int index) {
        double[] times = ((Channel) channelList.elementAt(index)).dArray.getTime();
        if (times == null) return new double[0];
        return times;
    }

    public double GetTimeStart(int index) {
        return ((Channel) channelList.elementAt(index)).dArray.getStartTime();
    }

    public double GetTimeDuration(int index) {
        return ((Channel) channelList.elementAt(index)).dArray.getDuration();
    }

    public byte[] GetData(int index) {
        Object data = getData(index);
        if (data == null) {
            return null;
        }
        Class cl = data.getClass();
        if (cl == double[].class) return double2Byte((double[]) data); else if (cl == float[].class) return float2Byte((float[]) data); else if (cl == long[].class) return long2Byte((long[]) data); else if (cl == int[].class) return int2Byte((int[]) data); else if (cl == short[].class) return short2Byte((short[]) data); else if (cl == String.class) return ((String) data).getBytes(); else if (cl == String[].class) {
            String[] sdata = (String[]) data;
            int length = 0;
            byte[][] arrays = new byte[sdata.length][];
            for (int idx = 0; idx < sdata.length; ++idx) {
                arrays[idx] = sdata[idx].getBytes();
                length += arrays[idx].length;
            }
            byte[] arrayR = new byte[length];
            length = 0;
            for (int idx = 0; idx < sdata.length; ++idx) {
                System.arraycopy(arrays[idx], 0, arrayR, length, arrays[idx].length);
                length += arrays[idx].length;
            }
            return (arrayR);
        } else if (cl == byte[][].class) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                byte[][] bData = (byte[][]) data;
                for (int ii = 0; ii < bData.length; ++ii) baos.write(bData[ii]);
            } catch (java.io.IOException ioe) {
                ioe.printStackTrace();
            }
            return baos.toByteArray();
        }
        return (byte[]) data;
    }

    public double[] GetDataAsFloat64(int index) {
        return (double[]) getData(index);
    }

    public float[] GetDataAsFloat32(int index) {
        return (float[]) getData(index);
    }

    public long[] GetDataAsInt64(int index) {
        return (long[]) getData(index);
    }

    public int[] GetDataAsInt32(int index) {
        return (int[]) getData(index);
    }

    public short[] GetDataAsInt16(int index) {
        return (short[]) getData(index);
    }

    public byte[] GetDataAsInt8(int index) {
        return (byte[]) getData(index);
    }

    public String[] GetDataAsString(int index) {
        return ((String[]) getData(index));
    }

    public byte[][] GetDataAsByteArray(int index) {
        return (byte[][]) getData(index);
    }

    public String GetName(int index) {
        return ((Channel) channelList.elementAt(index)).name;
    }

    public final int GetIndex(String channelName) {
        Channel ch = (Channel) channelMap.get(channelName);
        if (ch == null) return -1; else return ch.index;
    }

    public final int GetType(int index) {
        try {
            Object data = getData(index);
            if (data == null) return TYPE_UNKNOWN;
            Class cl = data.getClass();
            if (cl == double[].class) return DataBlock.TYPE_FLOAT64; else if (cl == float[].class) return DataBlock.TYPE_FLOAT32; else if (cl == long[].class) return DataBlock.TYPE_INT64; else if (cl == int[].class) return DataBlock.TYPE_INT32; else if (cl == short[].class) return DataBlock.TYPE_INT16; else if (cl == String[].class) return DataBlock.TYPE_STRING; else if (cl == byte[][].class) {
                return ((Channel) channelList.elementAt(index)).dArray.getDataType();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return DataBlock.TYPE_INT8;
    }

    public final String GetMime(int index) {
        return ((Channel) channelList.elementAt(index)).dArray.getMIMEType();
    }

    public final boolean GetIfFetchTimedOut() {
        return wasTimeout;
    }

    public final String GetUserInfo(int channel) {
        if (GetType(channel) == TYPE_USER) {
            StringBuffer sb = new StringBuffer();
            byte[][] dataArray = GetDataAsByteArray(channel);
            for (int ii = 0; ii < dataArray.length; ++ii) {
                sb.append(new String(dataArray[ii]));
            }
            return sb.toString();
        } else if (!"text/xml".equals(GetMime(channel)) || GetType(channel) != TYPE_STRING) return "";
        StringBuffer sb = new StringBuffer();
        String[] dataArray = GetDataAsString(channel);
        for (int ii = 0; ii < dataArray.length; ++ii) {
            String data = dataArray[ii];
            int index = data.indexOf("<user>");
            if (index >= 0) {
                int index2 = com.rbnb.compat.Utilities.lastIndexOf(data, "</user>");
                if (index2 >= 0) sb.append(data.substring(index + 6, index2));
            }
        }
        return sb.toString();
    }

    public void PutMime(int index, String mime) {
        Channel ch = ((Channel) channelList.elementAt(index));
        if (ch.rmap == null) ch.mime = mime; else try {
            Client.forEachNode(ch.rmap, new SetMimeAction(mime));
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class SetMimeAction implements Client.Action {

        private String mime;

        public SetMimeAction(String mime) {
            this.mime = mime;
        }

        public void doAction(Object o) throws Exception {
            Rmap r = (Rmap) o;
            if (r.getDblock() != null) r.getDblock().setMIMEType(mime);
        }
    }

    public int TypeID(String type) {
        type = type.toLowerCase();
        if ("int8".equals(type) || "i8".equals(type)) return TYPE_INT8;
        if ("int16".equals(type) || "i16".equals(type)) return TYPE_INT16;
        if ("int32".equals(type) || "i32".equals(type)) return TYPE_INT32;
        if ("int64".equals(type) || "i64".equals(type)) return TYPE_INT64;
        if ("float32".equals(type) || "f32".equals(type)) return TYPE_FLOAT32;
        if ("float64".equals(type) || "f64".equals(type)) return TYPE_FLOAT64;
        if ("string".equals(type) || "s".equals(type)) return TYPE_STRING;
        if ("user".equals(type)) return TYPE_USER;
        if ("unknown".equals(type)) return TYPE_UNKNOWN;
        if ("bytearray".equals(type) || "b".equals(type) || "ba".equals(type)) return TYPE_BYTEARRAY;
        throw new IllegalArgumentException("Type unrecognized.");
    }

    public String TypeName(int typeID) {
        return types[typeID];
    }

    public void PutData(int channelIndex, byte[] rawData, int typeID) throws SAPIException {
        PutData(channelIndex, rawData, typeID, LOCAL);
    }

    public void PutData(int channelIndex, byte[] rawData, int typeID, ByteOrderEnum byteOrder) throws SAPIException {
        int size = (typeID == TYPE_STRING || typeID == TYPE_BYTEARRAY || typeID == TYPE_USER) ? rawData.length : getSize(typeID);
        try {
            putData(channelIndex, rawData, rawData.length / size, size, (byte) typeID, byteOrder.getByte());
        } catch (Exception e) {
            throw new SAPIException(e);
        }
    }

    /**
	  * Type safe version of PutData().
	  * @see #PutData(int,byte[],int)
	*/
    public void PutDataAsFloat64(int channelIndex, double[] data) throws SAPIException {
        try {
            putData(channelIndex, data, data.length, 8, DataBlock.TYPE_FLOAT64, DataBlock.ORDER_MSB);
        } catch (Exception e) {
            throw new SAPIException(e);
        }
    }

    /**
	  * Type safe version of PutData().
	  * @see #PutData(int,byte[],int)
	*/
    public void PutDataAsFloat32(int channelIndex, float[] data) throws SAPIException {
        try {
            putData(channelIndex, data, data.length, 4, DataBlock.TYPE_FLOAT32, DataBlock.ORDER_MSB);
        } catch (Exception e) {
            throw new SAPIException(e);
        }
    }

    /**
	  * Type safe version of PutData().
	  * @see #PutData(int,byte[],int)
	*/
    public void PutDataAsInt64(int channelIndex, long[] data) throws SAPIException {
        try {
            putData(channelIndex, data, data.length, 8, DataBlock.TYPE_INT64, DataBlock.ORDER_MSB);
        } catch (Exception e) {
            throw new SAPIException(e);
        }
    }

    /**
	  * Type safe version of PutData().
	  * @see #PutData(int,byte[],int)
	*/
    public void PutDataAsInt32(int channelIndex, int[] data) throws SAPIException {
        try {
            putData(channelIndex, data, data.length, 4, DataBlock.TYPE_INT32, DataBlock.ORDER_MSB);
        } catch (Exception e) {
            throw new SAPIException(e);
        }
    }

    /**
	  * Type safe version of PutData().
	  * @see #PutData(int,byte[],int)
	*/
    public void PutDataAsInt16(int channelIndex, short[] data) throws SAPIException {
        try {
            putData(channelIndex, data, data.length, 2, DataBlock.TYPE_INT16, DataBlock.ORDER_MSB);
        } catch (Exception e) {
            throw new SAPIException(e);
        }
    }

    /**
	  * Type safe version of PutData().
	  * @see #PutData(int,byte[],int)
	*/
    public void PutDataAsInt8(int channelIndex, byte[] data) throws SAPIException {
        try {
            putData(channelIndex, data, data.length, 1, DataBlock.TYPE_INT8, DataBlock.ORDER_MSB);
        } catch (Exception e) {
            throw new SAPIException(e);
        }
    }

    /**
	  * Type safe version of PutData().
	  * @see #PutData(int,byte[],int)
	  *
	*/
    public void PutDataAsString(int channelIndex, String data) throws SAPIException {
        try {
            putData(channelIndex, data, 1, data.length(), DataBlock.TYPE_STRING, DataBlock.ORDER_MSB);
        } catch (Exception e) {
            throw new SAPIException(e);
        }
    }

    /**
	  * Type safe version of PutData().  Places a block of bytes into the
	  *   ChannelMap as a contiguous object.
	  * @see #PutData(int,byte[],int)
	*/
    public void PutDataAsByteArray(int channelIndex, byte[] data) throws SAPIException {
        try {
            putData(channelIndex, data, 1, data.length, DataBlock.TYPE_BYTEARRAY, DataBlock.ORDER_MSB);
        } catch (Exception e) {
            throw new SAPIException(e);
        }
    }

    public final void PutUserInfo(int channelIndex, String data) throws SAPIException {
        try {
            putData(channelIndex, data, 1, data.length(), DataBlock.TYPE_USER, DataBlock.ORDER_MSB);
        } catch (Exception e) {
            throw new SAPIException(e);
        }
    }

    /** 
	  * Transfers a reference of the data in the sourceMap to <code>this</code>
	  *  map. The primary application of this method is in "pass-through"
	  *  PlugIns, such as VSource, which do nothing with the data other than
	  *  transfer it from one channel to another.
	  * <p>
	  * @throws IndexOutOfRange If either <code>destChannel >= 
	  *  this.NumberOfChannels()</code>
	  *  or <code>sourceChannel >= sourceMap.NumberOfChannels()</code>.
	  * @throws SAPIException If there is a problem transferring the data.
	  * @author WHF
	  * @see #PutTimeRef(ChannelMap, int)
	  * @since V2.0B10
	 */
    public void PutDataRef(int destChannel, ChannelMap sourceMap, int sourceChannel) throws SAPIException {
        Channel ch = ((Channel) sourceMap.channelList.elementAt(sourceChannel));
        Object data = ch.dArray.getData();
        byte type = (byte) sourceMap.GetType(sourceChannel);
        int size, pts = ch.dArray.getNumberOfPoints();
        if (pts != 1 && (type == TYPE_USER || type == TYPE_STRING || type == TYPE_BYTEARRAY)) {
            unrollDataRef(destChannel, data, type, pts, sourceMap.GetMime(sourceChannel));
        } else {
            switch(type) {
                case TYPE_USER:
                case TYPE_STRING:
                    data = ((String[]) data)[0];
                    size = ((String) data).length();
                    pts = 1;
                    break;
                case TYPE_BYTEARRAY:
                    data = ((byte[][]) data)[0];
                    size = ((byte[]) data).length;
                    pts = 1;
                    break;
                default:
                    size = getSize(type);
                    pts = ch.dArray.getNumberOfPoints();
            }
            try {
                putData(destChannel, data, pts, size, type, DataBlock.ORDER_MSB);
                PutMime(destChannel, sourceMap.GetMime(sourceChannel));
            } catch (Exception e) {
                throw new SAPIException(e);
            }
        }
    }

    private final void unrollDataRef(int destChannelI, Object dataI, byte typeI, int ptsI, String mimeI) throws SAPIException {
        int oldTimeMode = timeMode;
        DataArray oldTimeReference = timeReference;
        double oldStart = start, oldDuration = duration;
        double[] oldPointTimes = pointTimes;
        double[] theTimes;
        double lDuration = 0.;
        if (timeMode == REFERENCE) {
            theTimes = oldTimeReference.getTime();
        } else {
            TimeRange tr = getTR();
            theTimes = new double[ptsI];
            tr.copyTimes(ptsI, theTimes, 0);
            if (tr.getDuration() != 0.) {
                lDuration = tr.getDuration() / ptsI;
            }
        }
        for (int idx = 0; idx < ptsI; ++idx) {
            PutTime(theTimes[idx], lDuration);
            if (dataI instanceof String[]) {
                PutDataAsString(destChannelI, ((String[]) dataI)[idx]);
            } else if (dataI instanceof byte[][]) {
                PutDataAsByteArray(destChannelI, ((byte[][]) dataI)[idx]);
            }
        }
        PutMime(destChannelI, mimeI);
        timeMode = oldTimeMode;
        timeReference = oldTimeReference;
        start = oldStart;
        duration = oldDuration;
        pointTimes = oldPointTimes;
    }

    public void PutTime(double start, double duration) {
        timeMode = MANUAL;
        this.start = start;
        this.duration = duration;
        onTimeModeSet();
    }

    public void PutTimes(double[] times) {
        if (times != null && times.length == 0) times = null;
        pointTimes = times;
        timeMode = ARRAY;
        onTimeModeSet();
    }

    public void PutTimeRef(ChannelMap sourceMap, int channelIndex) {
        timeReference = ((Channel) sourceMap.channelList.elementAt(channelIndex)).dArray;
        timeMode = REFERENCE;
        onTimeModeSet();
    }

    public void PutTimeAuto(String timeMode) throws IllegalArgumentException {
        if (timeMode != null) {
            timeMode = timeMode.toLowerCase();
            if (timeMode.equals("next")) this.timeMode = NEXT; else if (timeMode.equals("timeofday")) this.timeMode = TIMEOFDAY; else if (timeMode.equals("server")) this.timeMode = SERVERTOD; else throw new IllegalArgumentException(timeStampErrStr);
        } else throw new IllegalArgumentException(timeStampErrStr);
        onTimeModeSet();
    }

    public int NumberOfChannels() {
        return channelList.size();
    }

    public String toString() {
        StringBuffer response = new StringBuffer(this.getClass().getName());
        response.append(" with ").append(channelList.size()).append(" channels.");
        for (int ii = 0; ii < channelList.size(); ++ii) response.append("\n\t").append(channelList.elementAt(ii));
        return response.toString();
    }

    public final String[] GetNodeList() {
        try {
            Rmap r;
            if (response != null) r = response; else {
                if (channelList.size() == 0) return new String[0];
                r = Rmap.createFromName(channelList.elementAt(0).toString());
                for (int ii = 0; ii < channelList.size(); ++ii) {
                    r.addChannel(channelList.elementAt(0).toString());
                    r = r.moveToTop();
                }
            }
            Client.GetListAction getList = new Client.GetListAction();
            Client.forEachNode(r, getList);
            return getList.getNames();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public final String[] GetChannelList() {
        String[] list = new String[channelList.size()];
        for (int ii = 0; ii < list.length; ++ii) list[ii] = channelList.elementAt(ii).toString();
        return list;
    }

    /**
	  * Returns the list of <em>empty</em> folders contained within this 
	  *  ChannelMap.  If there are no such folders, an array of length zero
	  *  is returned.  In general there will only be folders in a ChannelMap
	  *  which is the result of a {@link Sink#RequestRegistration(ChannelMap)}.
	  * <p>
	  * @author WHF
	  * @see Sink#RequestRegistration(ChannelMap)
	  * @since V2.1
	  */
    public final String[] GetFolderList() {
        return folderArray;
    }

    public final String[] GetServerList() {
        try {
            if (response == null) return new String[0];
            Client.GetClientsAction getServers = new Client.GetClientsAction(com.rbnb.api.Server.class);
            Client.forEachNode(response, getServers);
            return getServers.getNames();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public final String[] GetSinkList() {
        try {
            if (response == null) return new String[0];
            Client.GetClientsAction getSinks = new Client.GetClientsAction(com.rbnb.api.Sink.class);
            Client.forEachNode(response, getSinks);
            return getSinks.getNames();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public final String[] GetSourceList() {
        try {
            if (response == null) return new String[0];
            Client.GetClientsAction getSources = new Client.GetClientsAction(com.rbnb.api.Source.class, com.rbnb.api.Sink.class);
            Client.forEachNode(response, getSources);
            return getSources.getNames();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public final String[] GetPlugInList() {
        try {
            if (response == null) return new String[0];
            Client.GetClientsAction getPlugIns = new Client.GetClientsAction(com.rbnb.api.PlugIn.class);
            Client.forEachNode(response, getPlugIns);
            return getPlugIns.getNames();
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public int[] Search(String mimeType, String keywords) {
        if (mimeType != null && mimeType.length() == 0) mimeType = null;
        if (keywords != null && keywords.length() == 0) keywords = null;
        if (mimeType == null && keywords == null) {
            int[] arr = new int[NumberOfChannels()];
            for (int ii = 0; ii < arr.length; ++ii) arr[ii] = ii;
            return arr;
        }
        String[] searchWordArray;
        if (keywords != null) {
            StringTokenizer st = new StringTokenizer(keywords);
            int tok = st.countTokens();
            searchWordArray = new String[tok];
            for (int ii = 0; ii < tok; ++ii) searchWordArray[ii] = st.nextToken().toLowerCase();
        } else searchWordArray = new String[0];
        final Vector hits = new Vector();
        final Hashtable map = new Hashtable();
        for (int ii = 0; ii < NumberOfChannels(); ++ii) {
            String mime = GetMime(ii);
            String dataString = null;
            int type = GetType(ii);
            boolean mimesEqual = mimeType == null || (mimeType != null && mimeType.equals(mime));
            if (searchWordArray.length == 0 && mimesEqual) {
                hits.addElement(new Integer(ii));
                continue;
            }
            if (mimesEqual || mimeType == null && mime != null && mime.startsWith("text/")) {
                switch(type) {
                    case ChannelMap.TYPE_STRING:
                        dataString = GetDataAsString(ii)[0];
                        break;
                    case ChannelMap.TYPE_INT8:
                        dataString = new String(GetDataAsInt8(ii));
                        break;
                    default:
                        continue;
                }
            } else if (mimeType == null && type == ChannelMap.TYPE_STRING) dataString = GetDataAsString(ii)[0]; else continue;
            boolean isMatch = true;
            if (dataString != null) {
                StringTokenizer st = new StringTokenizer(dataString.toLowerCase(), " \t\n\r\f:;'\",.`()[]{}");
                Hashtable tokens = new Hashtable();
                while (st.hasMoreTokens()) tokens.put(st.nextToken(), null);
                for (int iii = 0; iii < searchWordArray.length; ++iii) {
                    if (!tokens.containsKey(searchWordArray[iii])) {
                        isMatch = false;
                        break;
                    }
                }
            } else if (searchWordArray.length != 0) isMatch = false;
            if (isMatch) {
                hits.addElement(new Integer(ii));
            }
        }
        int[] matches = new int[hits.size()];
        {
            for (int ii = 0; ii < matches.length; ++ii) matches[ii] = ((Integer) hits.elementAt(ii)).intValue();
        }
        return matches;
    }

    static final long serialVersionUID = 8274903653155563848L;

    void addFetched(String name, DataArray dArray) {
        Channel ch = new Channel(channelList.size(), name, null);
        ch.dArray = dArray;
        channelList.addElement(ch);
        channelMap.put(name, ch);
    }

    void processResult(Rmap result, boolean tokeep, boolean removeLeadingSlash) throws Exception {
        if (tokeep) response = result;
        if (debugFlag) System.err.println("Processing from fetch " + result);
        if (result != null) {
            setIfFetchTimedOut(false);
            String[] names = result.extractNames();
            for (int ii = 0; ii < names.length; ++ii) {
                String name = names[ii];
                DataArray res = result.extract(name);
                if (removeLeadingSlash && name.charAt(0) == Rmap.PATHDELIMITER) name = name.substring(1);
                addFetched(name, res);
            }
            folderArray = result.extractFolders();
        } else setIfFetchTimedOut(true);
    }

    private void setIfFetchTimedOut(boolean wasTimeout) {
        this.wasTimeout = wasTimeout;
    }

    /**
	  * Creates a DataRequest object that contains any data added
	  *  so far to the Rmap.  Channels which have been added but do not
	  *  contain data will be merged in.
	  *
	  * 11/13/2002  WHF  Added this method.\
	  * 2003/10/14  WHF  Throws meaningful exception when an empty request
	  *  is made.
	  */
    DataRequest produceRequest() throws SAPIException {
        try {
            DataRequest dataReq = new DataRequest();
            Rmap base = (Rmap) produceOutput();
            boolean doThrow = false;
            while (base.getNchildren() > 0) {
                Rmap tmp = base.getChildAt(0);
                base.removeChildAt(0);
                dataReq.addChild(tmp);
                doThrow = true;
            }
            for (int ii = 0; ii < channelList.size(); ++ii) {
                Channel ch = (Channel) channelList.elementAt(ii);
                if (ch.rmap == null) {
                    if (doThrow) throw new IllegalArgumentException("Illegal mixed request of channels with and without data");
                    Client.addDataMarkerAction.doAction(dataReq.addChannel(ch.name));
                }
            }
            clearData();
            if (dataReq.getNchildren() == 0) throw new IllegalArgumentException("Cannot make empty request.");
            for (Rmap curr = dataReq.getChildAt(0); curr.getName() == null; ) {
                if (curr.getTrange() != null) curr.getChildAt(0).setTrange(curr.getTrange());
                Rmap parent = curr.getParent(), child = curr.getChildAt(0);
                parent.removeChildAt(0);
                curr.removeChildAt(0);
                parent.addChild(child);
                curr = child;
            }
            if (debugFlag) System.err.println("DataRequest: " + dataReq);
            return dataReq;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new SAPIException(e);
        }
    }

    final void setResponse(Rmap response) {
        this.response = response;
    }

    /**
	  * Used by Source#Flush in the case of no channels out to pass along a
	  *  response.
	  */
    final Rmap getResponse() {
        return response;
    }

    Rmap getBaseFrame() {
        return baseFrame;
    }

    /**
	  * Combines the data received from the last Fetch(), as well as 
	  *  data Put() to this map, into one Rmap.
	  * <p>As of 05/19/2003, always uses null parent.
	  */
    Rmap produceOutput() throws Exception {
        if (outputRmap == null) outputRmap = new Rmap(null); else while (outputRmap.getNchildren() > 0) outputRmap.removeChildAt(outputRmap.getNchildren() - 1);
        if (baseFrame.getName() != null || baseFrame.getNchildren() > 0) outputRmap.addChild(baseFrame);
        if (response != null) outputRmap.addChild(response);
        if (debugFlag) System.err.println("produceOutput: " + outputRmap);
        return outputRmap;
    }

    /**
	  * Clears out source data, without deleting channels.
	  *  Called by Clear(), Sink, Source.
	  */
    void clearData() throws Exception {
        baseFrame = new Rmap();
        outputRmap = null;
        response = null;
        for (int ii = channelList.size() - 1; ii >= 0; ii--) {
            Channel ch = ((Channel) channelList.elementAt(ii));
            ch.rmap = null;
        }
        channelsPut = 0;
        timePerChannel = false;
    }

    void incrementNext() {
        if (timeMode == NEXT) ++start;
    }

    int getChannelsPut() {
        return channelsPut;
    }

    final boolean debugFlag;

    private boolean wasTimeout = false;

    private Rmap response;

    private class Channel {

        String name;

        int index;

        DataArray dArray;

        Rmap rmap;

        String mime;

        int lastConsistentChild, lastSetTimeCount;

        Channel(int index, String name, Rmap rmap) {
            this.index = index;
            this.name = name;
            this.rmap = rmap;
            lastSetTimeCount = setTimeCounter;
        }

        public boolean equals(Object comp) {
            return name.equals(comp.toString());
        }

        public String toString() {
            return name;
        }
    }

    private int timeMode = SERVERTOD;

    private double start = 0.0, duration = 1.0;

    private double[] pointTimes;

    private DataArray timeReference;

    private int channelsPut = 0;

    private boolean timePerChannel = false;

    private int setTimeCounter = 0;

    private Vector channelList;

    private Hashtable channelMap, folderMappings;

    private Rmap baseFrame, outputRmap = null;

    private String[] folderArray;

    private void writeObject(com.rbnb.compat.ObjectOutputStream out) throws java.io.IOException {
        try {
            response.collapse();
        } catch (Exception e) {
            e.printStackTrace();
        }
        out.writeObject(response);
    }

    private void readObject(com.rbnb.compat.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        try {
            initVariables();
            Rmap rmin = (Rmap) in.readObject();
            processResult(rmin, true, false);
            response.collapse();
        } catch (Exception e) {
            e.printStackTrace();
            throw new java.io.IOException(e.getMessage());
        }
    }

    private final void initVariables() {
        channelList = new Vector();
        channelMap = new Hashtable();
        folderMappings = new Hashtable();
        baseFrame = new Rmap();
        folderArray = new String[0];
    }

    private Object getData(int index) {
        Object t = ((Channel) channelList.elementAt(index)).dArray.getData();
        return t;
    }

    private void onTimeModeSet() {
        if (!timePerChannel && channelsPut > 0) inheritTimes();
        ++setTimeCounter;
    }

    private TimeRange getTR() {
        ++channelsPut;
        TimeRange tr = null;
        switch(timeMode) {
            case MANUAL:
                tr = new TimeRange(start, duration);
                break;
            case ARRAY:
                tr = new TimeRange(pointTimes, 0.0);
                break;
            case NEXT:
                tr = new TimeRange(start + 1.0, 0.0);
                break;
            case TIMEOFDAY:
                tr = new TimeRange((System.currentTimeMillis() / 1000.0), 0.0);
                break;
            case SERVERTOD:
                tr = TimeRange.SERVER_TOD;
                break;
            case REFERENCE:
                throw new IllegalStateException("Cannot get Reference time range.");
        }
        if (!timePerChannel) {
            baseFrame.setTrange(tr);
            return null;
        }
        return tr;
    }

    private void inheritTimes() {
        timePerChannel = true;
        try {
            TimeRange tr = baseFrame.getTrange();
            if (tr == null) return;
            baseFrame.setTrange(null);
            findEndNodes(baseFrame, tr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void putData(int channelIndex, Object data, int data_length, int data_pointsize, byte data_type, byte data_order) throws Exception {
        Rmap descendant;
        Channel ch = (Channel) channelList.elementAt(channelIndex);
        if (ch.rmap == null) {
            ch.rmap = baseFrame.addChannel(ch.name);
        }
        descendant = ch.rmap;
        if (timeMode == REFERENCE) {
            ++channelsPut;
            ch.rmap.addDataWithTimeReference(data, data_length, data_pointsize, data_type, data_order, timeReference);
            return;
        }
        TimeRange newTR = getTR();
        if (descendant.getNchildren() == 0) if (descendant.getDblock() == null) {
            descendant.setDblock(new DataBlock(data, data_length, data_pointsize, data_type, ch.mime, data_order, false, 0, data_pointsize));
            descendant.setTrange(newTR);
            ch.lastSetTimeCount = setTimeCounter;
        } else if (data_type != TYPE_STRING && data_type != TYPE_USER && ((data_type != TYPE_BYTEARRAY) || (data_pointsize == descendant.getDblock().getPtsize())) && (ch.lastSetTimeCount == setTimeCounter)) {
            descendant.getDblock().addData(data, data_length);
        } else {
            ch.lastSetTimeCount = setTimeCounter;
            if (data_type == TYPE_STRING || data_type == TYPE_USER || ((data_type == TYPE_BYTEARRAY) && (data_pointsize != descendant.getDblock().getPtsize())) || !checkConsistency(descendant, true, newTR, data, data_length)) {
                Rmap child = new Rmap(null, descendant.getDblock(), descendant.getTrange());
                descendant.setDblock(null);
                descendant.setTrange(null);
                descendant.addChild(child);
                descendant.addChild(new Rmap(null, new DataBlock(data, data_length, data_pointsize, data_type, ch.mime, data_order, false, 0, data_pointsize), newTR));
                ch.lastConsistentChild = 1;
            } else {
                return;
            }
        } else if (data_type == TYPE_STRING || data_type == TYPE_USER || (data_type == TYPE_BYTEARRAY) || (ch.lastSetTimeCount != setTimeCounter)) {
            ch.lastSetTimeCount = setTimeCounter;
            if (data_type != TYPE_STRING && data_type != TYPE_USER) {
                for (int ii = descendant.getNchildren() - 1; ii >= 0; ii--) if (((data_type != TYPE_BYTEARRAY) || (data_pointsize == descendant.getChildAt(ii).getDblock().getPtsize())) && checkConsistency(descendant.getChildAt(ii), false, newTR, data, data_length)) {
                    ch.lastConsistentChild = ii;
                    return;
                }
            }
            descendant.addChild(new Rmap(null, new DataBlock(data, data_length, data_pointsize, data_type, ch.mime, data_order, false, 0, data_pointsize), newTR));
            for (ch.lastConsistentChild = descendant.getNchildren() - 1; (ch.lastConsistentChild >= 0) && (descendant.getChildAt(ch.lastConsistentChild).getName() != null); --ch.lastConsistentChild) {
            }
            if (ch.lastConsistentChild == -1) {
            }
        } else if ((ch.lastConsistentChild == -1) || descendant.getChildAt(ch.lastConsistentChild).getName() != null) {
            descendant.addChild(new Rmap(null, new DataBlock(data, data_length, data_pointsize, data_type, ch.mime, data_order, false, 0, data_pointsize), newTR));
            for (ch.lastConsistentChild = descendant.getNchildren() - 1; (ch.lastConsistentChild >= 0) && (descendant.getChildAt(ch.lastConsistentChild).getName() != null); --ch.lastConsistentChild) {
            }
            if (ch.lastConsistentChild == -1) {
            }
        } else {
            descendant.getChildAt(ch.lastConsistentChild).getDblock().addData(data, data_length);
        }
    }

    private boolean checkConsistency(Rmap res, boolean expectName, TimeRange newTR, Object data, int data_length) throws Exception {
        TimeRange oldTR = res.getTrange();
        DataBlock oldDB = res.getDblock();
        if (expectName == (res.getName() != null)) {
            if (oldTR != null) {
                if (oldTR.extend(oldDB.getNpts(), newTR, data_length)) {
                    oldDB.addData(data, data_length);
                    return true;
                }
            } else if (newTR == null) {
                oldDB.addData(data, data_length);
                return true;
            }
        }
        return false;
    }

    private static final String timeStampErrStr = "TimeRef must be one of \"Next\" or \"TimeOfDay\" " + "\nfor AutoTimeStamp()";

    private static final String[] types = { "Unknown", "", "", "Int8", "Int16", "Int32", "Int64", "Float32", "Float64", "String", "ByteArray" };

    private static final int MANUAL = 0, NEXT = 1, TIMEOFDAY = 2, ARRAY = 3, REFERENCE = 4, SERVERTOD = 5;

    /**
	 * Byte order enumerated type.  One of LOCAL, LSB, or MSB.
	 */
    public static class ByteOrderEnum {

        private byte b;

        byte getByte() {
            return b;
        }

        ByteOrderEnum(byte _b) {
            b = _b;
        }

        /**
		  * Used to see if LOCAL equals LSB or MSB.  Since the objects are
		  *  singletons, use the == operator to differentiate between all three.
		  */
        public boolean equals(Object o) {
            try {
                return ((ByteOrderEnum) o).getByte() == b;
            } catch (ClassCastException cce) {
                return false;
            }
        }
    }

    private static byte determineLocalByteOrder() {
        return DataBlock.ORDER_MSB;
    }

    private static void findEndNodes(Rmap target, TimeRange tr) throws Exception {
        int N = target.getNchildren();
        if (N == 0) target.setTrange((TimeRange) tr.clone());
        for (int ii = N - 1; ii >= 0; ii--) findEndNodes(target.getChildAt(ii), tr);
    }

    private static int getSize(int typecode) {
        int size = 0;
        switch(typecode) {
            case DataBlock.TYPE_FLOAT64:
                size = 8;
                break;
            case DataBlock.TYPE_FLOAT32:
                size = 4;
                break;
            case DataBlock.TYPE_INT64:
                size = 8;
                break;
            case DataBlock.TYPE_INT32:
                size = 4;
                break;
            case DataBlock.TYPE_INT16:
                size = 2;
                break;
            case DataBlock.TYPE_INT8:
            case DataBlock.TYPE_STRING:
            case DataBlock.TYPE_USER:
                size = 1;
                break;
            default:
                throw new IllegalArgumentException("Unsupported type in PutData().");
        }
        return size;
    }

    private static final byte[] double2Byte(double[] inData) {
        int j = 0;
        int length = inData.length;
        byte[] outData = new byte[length * 8];
        for (int i = 0; i < length; i++) {
            long data = Double.doubleToLongBits(inData[i]);
            outData[j++] = (byte) (data >>> 0);
            outData[j++] = (byte) (data >>> 8);
            outData[j++] = (byte) (data >>> 16);
            outData[j++] = (byte) (data >>> 24);
            outData[j++] = (byte) (data >>> 32);
            outData[j++] = (byte) (data >>> 40);
            outData[j++] = (byte) (data >>> 48);
            outData[j++] = (byte) (data >>> 56);
        }
        return outData;
    }

    private static final byte[] float2Byte(float[] inData) {
        int j = 0;
        int length = inData.length;
        byte[] outData = new byte[length * 4];
        for (int i = 0; i < length; i++) {
            int data = Float.floatToIntBits(inData[i]);
            outData[j++] = (byte) (data >>> 0);
            outData[j++] = (byte) (data >>> 8);
            outData[j++] = (byte) (data >>> 16);
            outData[j++] = (byte) (data >>> 24);
        }
        return outData;
    }

    private static final byte[] long2Byte(long[] inData) {
        int j = 0;
        int length = inData.length;
        byte[] outData = new byte[length * 8];
        for (int i = 0; i < length; i++) {
            long data = inData[i];
            outData[j++] = (byte) (data >>> 0);
            outData[j++] = (byte) (data >>> 8);
            outData[j++] = (byte) (data >>> 16);
            outData[j++] = (byte) (data >>> 24);
            outData[j++] = (byte) (data >>> 32);
            outData[j++] = (byte) (data >>> 40);
            outData[j++] = (byte) (data >>> 48);
            outData[j++] = (byte) (data >>> 56);
        }
        return outData;
    }

    private static final byte[] int2Byte(int[] inData) {
        int j = 0;
        int length = inData.length;
        byte[] outData = new byte[length * 4];
        for (int i = 0; i < length; i++) {
            outData[j++] = (byte) (inData[i] >>> 0);
            outData[j++] = (byte) (inData[i] >>> 8);
            outData[j++] = (byte) (inData[i] >>> 16);
            outData[j++] = (byte) (inData[i] >>> 24);
        }
        return outData;
    }

    private static final byte[] short2Byte(short[] inData) {
        int j = 0;
        int length = inData.length;
        byte[] outData = new byte[length * 2];
        for (int i = 0; i < length; i++) {
            outData[j++] = (byte) (inData[i] >>> 0);
            outData[j++] = (byte) (inData[i] >>> 8);
        }
        return outData;
    }
}
