package com.rbnb.plot;

import com.rbnb.sapi.SAPIException;
import com.rbnb.sapi.ChannelMap;

public class Sink {

    private com.rbnb.sapi.Sink sink = null;

    private boolean connected = false;

    private String saveaddress = null;

    private ChannelMap lastMap = null;

    public Sink() {
        sink = new com.rbnb.sapi.Sink();
    }

    public Map getTimeLimits(Map inMap) {
        if (!connected) return inMap;
        try {
            ChannelMap cm = new ChannelMap();
            Channel[] chan = inMap.channelList();
            for (int i = 0; i < chan.length; i++) {
                cm.Add("/" + chan[i].getChannelName());
                chan[i].clear();
            }
            sink.RequestRegistration(cm);
            ChannelMap regMap = sink.Fetch(2000);
            ChannelMap cm2 = null;
            for (int i = 0; i < regMap.NumberOfChannels(); i++) {
                if (regMap.GetTimeDuration(i) == 0) {
                    if (cm2 == null) cm2 = new ChannelMap();
                    cm2.Add(regMap.GetName(i));
                } else {
                    double[] times = new double[2];
                    times[0] = regMap.GetTimeStart(i);
                    times[1] = times[0] + regMap.GetTimeDuration(i);
                    DataTimeStamps dt = new DataTimeStamps(times);
                    Channel ch = inMap.findChannel(regMap.GetName(i).substring(1));
                    if (ch != null) ch.setTimeStamp(dt);
                }
            }
            if (cm2 != null) {
                sink.Request(cm2, 0, 0, "newest");
                ChannelMap newmap = sink.Fetch(2000);
                sink.Request(cm2, 0, 0, "oldest");
                ChannelMap oldmap = sink.Fetch(2000);
                for (int i = 0; i < oldmap.NumberOfChannels(); i++) {
                    Channel ch = inMap.findChannel(oldmap.GetName(i).substring(1));
                    double[] times = new double[2];
                    times[0] = oldmap.GetTimeStart(i);
                    int chnum = newmap.GetIndex(oldmap.GetName(i));
                    if (chnum >= 0) {
                        times[1] = newmap.GetTimeStart(chnum) + newmap.GetTimeDuration(chnum);
                        if (ch != null) ch.setTimeStamp(new DataTimeStamps(times));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Exception in Sink.getTimeLimits");
            e.printStackTrace();
        }
        return inMap;
    }

    public void OpenRBNBConnection() throws Exception {
        saveaddress = "localhost:3333";
        sink.OpenRBNBConnection();
        connected = true;
    }

    public void OpenRBNBConnection(String address, String client) throws Exception {
        saveaddress = address;
        sink.OpenRBNBConnection(address, client);
        connected = true;
    }

    public void OpenRBNBConnection(String address, String client, String user, String pw) {
        try {
            saveaddress = address;
            sink.OpenRBNBConnection(address, client, user, pw);
            connected = true;
        } catch (SAPIException se) {
            se.printStackTrace();
        }
    }

    public Object isActive() {
        if (connected) return this; else return null;
    }

    public void getInformation(Map mapIO) {
    }

    public void terminateRBNB() {
        if (sink != null) {
            try {
                com.rbnb.api.Server server = com.rbnb.api.Server.newServerHandle(null, saveaddress);
                server.stop();
            } catch (Exception e) {
            }
        }
    }

    public void disconnect(boolean that, boolean otherThing) {
        sink.CloseRBNBConnection();
    }

    public String[][] getChannelList(String match) {
        try {
            ChannelMap cm = new ChannelMap();
            cm.Add(sink.GetServerName() + "/*/...");
            sink.RequestRegistration(cm);
            cm = sink.Fetch(-1);
            String[][] chans = new String[3][];
            chans[0] = cm.GetChannelList();
            chans[1] = new String[chans[0].length];
            for (int i = 0; i < chans[0].length; i++) {
                chans[1][i] = cm.GetUserInfo(i);
            }
            chans[2] = new String[chans[0].length];
            for (int i = 0; i < chans[0].length; i++) {
                if (cm.GetType(i) == ChannelMap.TYPE_STRING) {
                    String xmldat = cm.GetDataAsString(i)[0];
                    if (xmldat != null && xmldat.length() > 0) {
                        int i1 = xmldat.indexOf("<mime>");
                        int i2 = xmldat.indexOf("</mime>");
                        if (i1 > -1 && i2 > -1) chans[2][i] = xmldat.substring(i1 + 6, i2);
                    }
                }
            }
            return chans;
        } catch (SAPIException se) {
            se.printStackTrace();
            return null;
        }
    }

    public void setSinkMode(String mode) {
    }

    public void setReadTimeOut(Time timeOut) {
    }

    public void streamSetMap(Map map, Time start, Time duration, int flags) {
    }

    public Map streamGetMap() {
        return null;
    }

    public void synchronizeSink() {
    }

    public Map getData(Map m, Time s, Time d, int f) {
        return getMap(m, s, d, f);
    }

    public Map getMap(Map map, Time startT, Time durationT, int flags) {
        ChannelMap cm = null;
        int retVal = 0;
        int numChan = 0;
        String timeRef = new String("absolute");
        double start = 0;
        double duration = 0;
        if (startT != null) start = startT.getDoubleValue();
        if (durationT != null) duration = durationT.getDoubleValue();
        cm = new ChannelMap();
        Channel[] chan = map.channelList();
        for (int i = 0; i < chan.length; i++) {
            try {
                cm.Add("/" + chan[i].getChannelName());
                chan[i].clear();
            } catch (Exception e) {
                System.err.println("com.rbnb.plot.Sink.getMap: exception ");
                e.printStackTrace();
            }
        }
        if ((flags & DataRequest.newest) == DataRequest.newest) {
            timeRef = new String("newest");
            start = 0;
        } else if ((flags & DataRequest.oldest) == DataRequest.oldest) {
            timeRef = new String("oldest");
            start = 0;
        }
        try {
            sink.Request(cm, start, duration, timeRef);
            cm = sink.Fetch(60000);
            lastMap = cm;
            if (cm.GetIfFetchTimedOut()) System.err.println("***********rbnbPlot fetch timed out*************");
            numChan = cm.NumberOfChannels();
        } catch (SAPIException se) {
            se.printStackTrace();
        }
        for (int i = 0; i <= numChan - 1; i++) {
            boolean getTimeStamp = true;
            Channel ch = map.findChannel(cm.GetName(i));
            if (ch != null) {
                switch(cm.GetType(i)) {
                    case ChannelMap.TYPE_BYTEARRAY:
                        ch.setDataByteArray(cm.GetDataAsByteArray(i), cm.GetMime(i));
                        break;
                    case ChannelMap.TYPE_STRING:
                        ch.setDataString(cm.GetDataAsString(i), cm.GetMime(i));
                        break;
                    case ChannelMap.TYPE_INT8:
                        ch.setDataInt8(cm.GetDataAsInt8(i));
                        break;
                    case ChannelMap.TYPE_INT16:
                        ch.setDataInt16(cm.GetDataAsInt16(i));
                        break;
                    case ChannelMap.TYPE_INT32:
                        ch.setDataInt32(cm.GetDataAsInt32(i));
                        break;
                    case ChannelMap.TYPE_INT64:
                        ch.setDataInt64(cm.GetDataAsInt64(i));
                        break;
                    case ChannelMap.TYPE_FLOAT32:
                        ch.setDataFloat32(cm.GetDataAsFloat32(i));
                        break;
                    case ChannelMap.TYPE_FLOAT64:
                        ch.setDataFloat64(cm.GetDataAsFloat64(i));
                        break;
                    default:
                        for (int j = 0; j < chan.length; j++) {
                            chan[j].clear();
                        }
                        getTimeStamp = false;
                        break;
                }
                if (getTimeStamp) {
                    DataTimeStamps ts = new DataTimeStamps(cm.GetTimes(i));
                    ch.setTimeStamp(ts);
                }
            }
        }
        return map;
    }

    public ChannelMap getLastMap() {
        return lastMap;
    }
}
