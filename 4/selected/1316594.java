package com.rbnb.plot;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import com.rbnb.utility.KeyValueHash;
import com.rbnb.utility.ToString;

public class RBNBInterface implements Runnable {

    private int dg = 0;

    private PlotsContainer plotsContainer = null;

    private PlotContainer[][] pca = null;

    private RegChannel[] cha = new RegChannel[0];

    private Sink connection = null;

    private Map[] map = null;

    private Map cmap = null;

    private Channel[] channel = new Channel[0];

    private boolean newData = false;

    private RBNBPlotMain rbnbPlotMain = null;

    private RunModeCubby runModeCubby = null;

    private LayoutCubby layoutCubby = null;

    private RBNBCubby rbnbCubby = null;

    private PosDurCubby posDurCubby = null;

    private ConfigCubby configCubby = null;

    private int runMode = RunModeDefs.stop;

    private Time start = new Time();

    private boolean firstRT = true;

    private boolean rtTimeout = false;

    private Time[] position = new Time[3];

    private Time[] duration = null;

    private static int flags = DataRequest.extendStart | DataRequest.extendEnd;

    private long speedMant = 2;

    private byte speedExp = -2;

    private Time speed = new Time(speedMant, speedExp);

    private boolean rtReqMode = false;

    private long lastUpdate = (new Date()).getTime();

    private double updatePeriod = 0;

    private Time timeOutMin = new Time(1);

    private Time timeOutMax = new Time(2);

    private int repaintContainer = -1;

    private int repaintContainerCount = 0;

    private int rtWaitTime = 20;

    private int tlFrames = -1;

    private boolean updatePosition = true;

    private Environment environment = null;

    private ExportData ed = null;

    private double zerodurbase = 5.0 / 1000.;

    private ExportToDT exportToDT = null;

    private ExportToMatlab exportToMatlab = null;

    public RBNBInterface(RBNBPlotMain rpm, PlotsContainer pc, RunModeCubby rmc, LayoutCubby loc, RBNBCubby rbc, PosDurCubby pdc, ConfigCubby cc, Environment e) {
        rbnbPlotMain = rpm;
        plotsContainer = pc;
        runModeCubby = rmc;
        layoutCubby = loc;
        rbnbCubby = rbc;
        posDurCubby = pdc;
        configCubby = cc;
        environment = e;
        map = new Map[environment.DISPLAYGROUPS];
        duration = new Time[environment.DISPLAYGROUPS];
        for (int i = 0; i < environment.DISPLAYGROUPS; i++) {
            map[i] = new Map();
            duration[i] = new Time(0.1);
        }
        boolean stat = RBNBOpen(environment.HOST, environment.PORT);
        if (environment.HOST != null) layoutCubby.setStatus(stat);
        if (!stat || !RBNBChannel()) {
            start = new Time(0);
            position[0] = start;
            position[1] = position[0];
            position[2] = start.addTime(new Time(1));
            posDurCubby.setPosition(position, false);
            posDurCubby.setTimeFormat(Time.Unspecified);
            if (environment.DURATION != null) {
                posDurCubby.setDuration(environment.DURATION, false);
            } else posDurCubby.setDuration(new Time(0.1), false);
            cha = new RegChannel[0];
            pca = new PlotContainer[environment.DISPLAYGROUPS][0];
            rbnbCubby.setAvailableChannels(cha);
            return;
        }
        if (!RBNBTimeLimits()) {
            start = new Time(0);
            position[0] = start;
            position[1] = position[0];
            position[2] = start.addTime(new Time(1));
            posDurCubby.setPosition(position, false);
            posDurCubby.setTimeFormat(Time.Unspecified);
        }
        if (!RBNBDuration()) {
            duration[dg] = new Time(0.1);
            posDurCubby.setDuration(duration[dg], false);
        }
    }

    public boolean RBNBOpen(String serverHost, int serverPort) {
        if (connection != null) RBNBClose();
        if (serverHost == null || serverPort <= 0) return false;
        try {
            connection = new Sink();
            connection.OpenRBNBConnection(serverHost + ":" + serverPort, "rbnbPlot");
            if (environment.STATICMODE) {
                runMode = RunModeDefs.bof;
                if (rtReqMode) setRTReqMode(false);
            } else {
                runMode = RunModeDefs.realTime;
                if (!rtReqMode) setRTReqMode(true);
            }
            runModeCubby.set(runMode, false);
        } catch (Exception e) {
            System.out.println("RBNBInterface.RBNBOpen: caught " + e);
            e.printStackTrace();
            runMode = RunModeDefs.stop;
            runModeCubby.set(RunModeDefs.stop, false);
            return false;
        }
        return true;
    }

    public boolean RBNBChannel() {
        boolean returnVal = true;
        boolean newChans = false;
        RegChannel[] oldcha = null;
        PlotContainer[][] oldpca = null;
        Map imap = null;
        Channel[] chanInfo = null;
        if (connection == null) return false;
        try {
            if (rtReqMode) setRTReqMode(false);
            try {
                String[][] chanList = connection.getChannelList("*");
                cmap = new Map(chanList);
            } catch (Exception e) {
                System.err.println("RBNBInterface.RBNBChannel: exception");
                e.printStackTrace();
                return false;
            }
            channel = cmap.channelList();
            for (int i = 0; i < channel.length; i++) {
                if (channel[i].getChannelName().indexOf("_Log") != -1) {
                    cmap.removeChannel(channel[i]);
                }
            }
            channel = cmap.channelList();
            if (runMode == RunModeDefs.realTime) setRTReqMode(true);
            if (cha.length > 0) {
                oldcha = cha;
                oldpca = pca;
            } else newChans = true;
            cha = new RegChannel[channel.length];
            pca = new PlotContainer[environment.DISPLAYGROUPS][channel.length];
            for (int i = 0; i < channel.length; i++) {
                cha[i] = new RegChannel(channel[i]);
                for (int j = 0; j < environment.DISPLAYGROUPS; j++) pca[j][i] = null;
            }
            if (oldcha != null) {
                boolean[] kept = new boolean[oldcha.length];
                for (int i = 0; i < oldcha.length; i++) kept[i] = false;
                for (int i = 0; i < cha.length; i++) {
                    for (int j = 0; j < oldcha.length; j++) {
                        if (cha[i].name.equals(oldcha[j].name)) {
                            for (int k = 0; k < environment.DISPLAYGROUPS; k++) {
                                pca[k][i] = oldpca[k][j];
                            }
                            kept[j] = true;
                        }
                    }
                }
                for (int i = 0; i < oldcha.length; i++) if (!kept[i]) {
                    for (int j = 0; j < environment.DISPLAYGROUPS; j++) {
                        if (oldpca[j][i] != null) {
                            plotsContainer.removePlot(oldpca[j][i], j);
                            map[j].removeChannel(map[j].findChannel(oldcha[i].name));
                        }
                    }
                }
            }
            rbnbCubby.setAvailableChannels(cha);
            if (newChans) {
                String[] dgName = new String[channel.length];
                String[] dgLabel = new String[environment.DISPLAYGROUPS];
                for (int i = 0; i < channel.length; i++) {
                    if (channel[i].channelUserDataType == 1) {
                        KeyValueHash kvh = new KeyValueHash(channel[i].channelUserData);
                        dgName[i] = (String) kvh.get("group");
                    } else dgName[i] = null;
                }
                int j = 0;
                for (int i = 0; i < channel.length; i++) {
                    if (dgName[i] != null) {
                        boolean found = false;
                        for (int k = 0; k < j; k++) {
                            if (dgName[i].equals(dgLabel[k])) {
                                pca[k][i] = plotsContainer.addPlot(cha[i], k);
                                pca[k][i].setAbscissa(duration[dg]);
                                map[k].addChannel(cmap.findChannel(cha[i].name));
                                found = true;
                                k = j;
                            }
                        }
                        if (!found) {
                            dgLabel[j] = dgName[i];
                            pca[j][i] = plotsContainer.addPlot(cha[i], j);
                            pca[j][i].setAbscissa(duration[dg]);
                            map[j].addChannel(cmap.findChannel(cha[i].name));
                            j++;
                        }
                    }
                }
                int num = 0;
                if (environment.SHOWALLCHANNELS) {
                    for (int i = 0; i < channel.length; i++) {
                        if (j >= environment.DISPLAYGROUPS) break;
                        if (dgName[i] == null) {
                            pca[j][i] = plotsContainer.addPlot(cha[i], j);
                            pca[j][i].setAbscissa(duration[dg]);
                            map[j].addChannel(cmap.findChannel(cha[i].name));
                            if (++num % environment.CHANSPERDG == 0) j++;
                        }
                    }
                }
                plotsContainer.labelDisplayGroups(dgLabel);
            }
            int j = 0;
            for (int i = 0; i < cha.length; i++) if (pca[dg][i] != null) j++;
            RegChannel[] sca = new RegChannel[j];
            j = 0;
            for (int i = 0; i < cha.length; i++) if (pca[dg][i] != null) sca[j++] = cha[i];
            rbnbCubby.setSelectedChannels(sca, false);
            plotsContainer.invalidate();
            plotsContainer.validate();
            plotsContainer.repaint();
        } catch (Exception e) {
            System.out.println("RBNBInterface.RBNBChannel: caught " + e);
            e.printStackTrace();
            runMode = RunModeDefs.stop;
            if (rtReqMode) setRTReqMode(false);
            runModeCubby.set(RunModeDefs.stop, false);
            returnVal = false;
        }
        return returnVal;
    }

    public boolean RBNBTimeLimits() {
        if (connection == null) return false;
        try {
            if (rtReqMode) setRTReqMode(false);
            channel = map[dg].channelList();
            if (channel.length == 0) return false;
            map[dg] = connection.getTimeLimits(map[dg]);
            if (runMode == RunModeDefs.realTime) setRTReqMode(true);
            channel = map[dg].channelList();
            tlFrames = -1;
            Time begin = null;
            Time end = null;
            Time newpt = null;
            for (int i = 0; i < channel.length; i++) {
                if (channel[i].timeStamp != null) {
                    newpt = channel[i].timeStamp.getStartOfInterval(DataTimeStamps.first);
                    if (begin == null) begin = newpt; else if (newpt.compareTo(begin) == -1) begin = newpt;
                    newpt = newpt.addTime(channel[i].timeStamp.getDurationOfInterval(DataTimeStamps.first));
                    if (end == null) end = newpt; else if (newpt.compareTo(end) == 1) end = newpt;
                }
                if (channel[i].frames != null) {
                    int numFrames = channel[i].frames.getDurationOfInterval(0).getIntValue();
                    if (numFrames > tlFrames) tlFrames = numFrames;
                }
            }
            if (begin == null) begin = new Time(0);
            if (end == null) end = begin.addTime(new Time(1));
            position[1] = begin;
            position[2] = end;
            if (position[0] == null) position[0] = begin;
            if (position[0].compareTo(position[1]) < 0) position[1] = position[0];
            if (position[0].compareTo(position[2]) > 0) position[2] = position[0];
            posDurCubby.setPosition(position, false);
            for (int i = 0; i < channel.length; i++) {
                if (channel[i].timeStamp != null) {
                    posDurCubby.setTimeFormat(channel[i].timeStamp.format);
                    i = channel.length;
                }
            }
        } catch (Exception e) {
            System.out.println("RBNBInterface.RBNBTimeLimits: caught " + e);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean RBNBDuration() {
        Time newDuration = null;
        if (connection == null) return false;
        if (environment.DURATION != null) {
            for (int i = 0; i < environment.DISPLAYGROUPS; i++) {
                duration[i] = new Time(environment.DURATION);
            }
            posDurCubby.setDuration(duration[dg], false);
            return true;
        }
        try {
            if (rtReqMode) setRTReqMode(false);
            if (cmap.channelList().length == 0) {
                return false;
            }
            connection.getInformation(cmap);
            if (runMode == RunModeDefs.realTime) setRTReqMode(true);
            channel = cmap.channelList();
            Time newdur = null;
            int npts = 0;
            for (int i = 0; i < channel.length; i++) {
                if (channel[i].timeStamp != null && channel[i].numberOfPoints > 0) {
                    newdur = channel[i].timeStamp.getEndOfInterval(DataTimeStamps.last).subtractTime(channel[i].timeStamp.getStartOfInterval(DataTimeStamps.first));
                    if (channel[i].numberOfPoints < 200 && newdur.compareTo(new Time(0)) == 1) {
                        newdur = newdur.multiplyBy(200).divideBy(channel[i].numberOfPoints);
                    }
                    if (newdur.compareTo(new Time(0)) == 1) {
                        if (newDuration == null) newDuration = newdur; else if (newdur.compareTo(newDuration) == -1) newDuration = newdur;
                    }
                }
            }
            if (newDuration == null || newDuration.compareTo(new Time(0)) == 0) newDuration = new Time(0.1);
            duration[dg] = newDuration;
            posDurCubby.setDuration(duration[dg], false);
        } catch (Exception e) {
            System.out.println("RBNBInterface.RBNBDuration: caught " + e);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void RBNBClose() {
        if (connection != null) {
            if (rtReqMode) setRTReqMode(false);
            if (environment.KILLRBNB) try {
                connection.terminateRBNB();
            } catch (Exception e) {
                e.printStackTrace();
            } else connection.disconnect(true, true);
            connection = null;
            cmap = null;
            cha = new RegChannel[0];
            rbnbCubby.setAvailableChannels(cha);
            for (int i = 0; i < pca.length; i++) for (int j = 0; j < pca[i].length; j++) if (pca[i][j] != null) plotsContainer.removePlot(pca[i][j], i);
            pca = new PlotContainer[environment.DISPLAYGROUPS][0];
            for (int i = 0; i < environment.DISPLAYGROUPS; i++) map[i].clear();
            String dgLabel[] = new String[environment.DISPLAYGROUPS];
            for (int i = 0; i < environment.DISPLAYGROUPS; i++) dgLabel[i] = String.valueOf(i + 1);
            plotsContainer.labelDisplayGroups(dgLabel);
        }
        if (exportToDT != null) {
            exportToDT.setVisible(false);
            exportToDT = null;
        }
    }

    private void RBNBSaveConfig(Channel lchan, byte[] configByte) {
        System.err.println("RBNBInterface: SaveConfig not implemented");
    }

    private Channel[] RBNBConfigChannels() {
        System.err.println("RBNBInterface: ConfigChannels not implemented");
        Channel[] chName = null;
        return chName;
    }

    public void RBNBApplyConfig(Hashtable ht) {
        for (int i = 0; i < environment.DISPLAYGROUPS; i++) {
            for (int j = 0; j < pca[i].length; j++) {
                if (pca[i][j] != null) {
                    plotsContainer.removePlot(pca[i][j], i);
                    pca[i][j] = null;
                }
                map[i].clear();
            }
        }
        for (int i = 0; i < environment.DISPLAYGROUPS; i++) {
            int numChan = Integer.parseInt((String) ht.get("dg[" + i + "].chans"));
            for (int j = 0; j < numChan; j++) {
                String chan = (String) ht.get("dg[" + i + "][" + j + "].name");
                for (int k = 0; k < cha.length; k++) {
                    if (chan.equals(cha[k].name)) {
                        pca[i][k] = plotsContainer.addPlot(new RegChannel(chan), i);
                        pca[i][k].setAbscissa(duration[dg]);
                        Channel ch = new Channel();
                        ch.setName(chan);
                        map[i].addChannel(ch);
                        break;
                    }
                }
            }
        }
        for (int i = 0; i < environment.DISPLAYGROUPS; i++) {
            String durString = (String) ht.get("duration[" + i + "]");
            if (durString != null) duration[i] = new Time(Double.parseDouble(durString));
        }
    }

    public void run() {
        int layout = 0;
        Integer newVal = null;
        RegChannel[] newRC = null;
        Time newDur = null;
        Time[] newPosition = new Time[3];
        while (true) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                System.out.println("thread sleep error: " + e);
            }
            if ((newRC = rbnbCubby.getSelectedChannels(false)) != null) {
                firstRT = true;
                changeChannels(newRC);
                if (runMode == RunModeDefs.stop) runMode = RunModeDefs.current;
            } else if ((newVal = rbnbCubby.getGroup()) != null) {
                firstRT = true;
                if (rtReqMode) setRTReqMode(false);
                dg = newVal.intValue();
                posDurCubby.setDuration(duration[dg], false);
                if (runMode == RunModeDefs.realTime) setRTReqMode(true);
                for (int i = 0; i < cha.length; i++) if (pca[dg][i] != null) {
                    pca[dg][i].setAbscissa(duration[dg]);
                }
                int j = 0;
                for (int i = 0; i < cha.length; i++) if (pca[dg][i] != null) j++;
                RegChannel[] sca = new RegChannel[j];
                j = 0;
                for (int i = 0; i < cha.length; i++) if (pca[dg][i] != null) sca[j++] = cha[i];
                rbnbCubby.setSelectedChannels(sca, false);
                if (runMode == RunModeDefs.stop) runMode = RunModeDefs.current;
            } else if ((newVal = layoutCubby.get()) != null) {
                firstRT = true;
                changeLayout(newVal.intValue());
            } else if ((newDur = posDurCubby.getDuration(false)) != null) {
                firstRT = true;
                if (posDurCubby.getPositionAtStart()) {
                    duration[dg] = newDur;
                } else {
                    start = start.addTime(duration[dg]).subtractTime(newDur);
                    duration[dg] = newDur;
                }
                for (int i = 0; i < pca[dg].length; i++) {
                    if (pca[dg][i] != null) {
                        pca[dg][i].setAbscissa(duration[dg]);
                    }
                }
                resetPosition(false);
                if (runMode == RunModeDefs.stop) {
                    runMode = RunModeDefs.current;
                }
            } else if ((newPosition = posDurCubby.getPosition(false)) != null) {
                firstRT = true;
                updatePosition = false;
                if ((newPosition[0].compareTo(position[1]) <= 0) || (newPosition[0].addTime(duration[dg]).compareTo(position[2]) >= 0)) {
                    resetPosition(false);
                    position[0] = newPosition[0];
                    updatePosition = true;
                } else position = newPosition;
                start = position[0];
                runMode = RunModeDefs.current;
                if (rtReqMode) setRTReqMode(false);
            } else if ((newVal = runModeCubby.get(false)) != null) {
                firstRT = true;
                rtTimeout = false;
                int newRunMode = newVal.intValue();
                if (newRunMode == RunModeDefs.fwdPlay || newRunMode == RunModeDefs.revPlay) if (newRunMode == runMode) incrementSpeed(false); else incrementSpeed(true);
                runMode = newRunMode;
                if (runMode == RunModeDefs.realTime && rtReqMode == false) setRTReqMode(true); else if (runMode != RunModeDefs.realTime && rtReqMode == true) setRTReqMode(false);
                if (runMode == RunModeDefs.quit) {
                    RBNBClose();
                    break;
                }
                if (runMode == RunModeDefs.realTime) rtWaitTime = 20;
            } else {
                oneDataStep();
            }
        }
    }

    private void setRTReqMode(boolean intoRT) {
        boolean quitRT = false;
        if (connection == null) return;
        if (environment.STREAMING == false) {
            try {
                connection.setSinkMode("Request");
            } catch (Exception e) {
                e.printStackTrace();
            }
            rtReqMode = false;
            runModeCubby.setStreaming(false);
            return;
        }
        if (intoRT && !rtTimeout) {
            rtReqMode = true;
            try {
                if (map[dg].channelList().length == 0 || duration[dg].mantissa == 0) {
                    rtReqMode = false;
                } else {
                    connection.setSinkMode("Stream");
                    Time timeOut = duration[dg].multiplyBy(2);
                    if (timeOut.compareTo(timeOutMin) == -1) timeOut = timeOutMin; else if (timeOut.compareTo(timeOutMax) == 1) timeOut = timeOutMax;
                    connection.setReadTimeOut(timeOut);
                    connection.streamSetMap(map[dg], null, duration[dg], flags | DataRequest.newest | DataRequest.aligned | DataRequest.realTime);
                    if (!environment.SLAVEMODE) {
                        Map oldMap = map[dg];
                        map[dg] = connection.streamGetMap();
                        Channel[] lch = map[dg].channelList();
                        if (lch == null || lch.length < 1 || lch[0].numberOfPoints == 0 || lch[0].timeStamp == null) {
                            map[dg] = oldMap;
                            quitRT = true;
                            rtTimeout = true;
                        } else {
                            Time returnedDuration = lch[0].timeStamp.getEndOfInterval(DataTimeStamps.last).subtractTime(lch[0].timeStamp.getStartOfInterval(DataTimeStamps.first));
                            if (lch[0].numberOfPoints > 1) {
                                returnedDuration = returnedDuration.addTime(returnedDuration.divideBy(lch[0].numberOfPoints - 1).multiplyBy(2));
                            }
                            if (returnedDuration.compareTo(duration[dg]) == -1) quitRT = true;
                        }
                    }
                    if (quitRT) {
                        rtReqMode = false;
                        connection.setSinkMode("Request");
                    } else {
                        runModeCubby.setStreaming(true);
                        runModeCubby.set(RunModeDefs.realTime, false);
                    }
                }
            } catch (Exception e) {
                System.out.println("RBNBInterface.setRTReqMode: exception");
                e.printStackTrace();
            }
        } else if (!intoRT) {
            rtReqMode = false;
            runModeCubby.setStreaming(false);
            try {
                connection.synchronizeSink();
                connection.setSinkMode("Request");
            } catch (Exception e) {
                System.out.println("RBNBInterface.setRTReqMode: exception");
                e.printStackTrace();
            }
        }
    }

    private void changeChannels(RegChannel[] sca) {
        if (rtReqMode) setRTReqMode(false);
        newData = false;
        if (sca.length == 0) {
            for (int j = 0; j < pca[dg].length; j++) {
                if (pca[dg][j] != null) {
                    plotsContainer.removePlot(pca[dg][j], dg);
                    pca[dg][j] = null;
                    map[dg].removeChannel(map[dg].findChannel(cha[j].name));
                }
            }
            runMode = RunModeDefs.stop;
            if (rtReqMode) setRTReqMode(false);
            runModeCubby.set(RunModeDefs.stop, false);
        } else {
            int j = 0;
            for (int i = 0; i < sca.length; i++) {
                while (sca[i].name.equals(cha[j].name) == false) {
                    if (pca[dg][j] != null) {
                        plotsContainer.removePlot(pca[dg][j], dg);
                        pca[dg][j] = null;
                        map[dg].removeChannel(map[dg].findChannel(cha[j].name));
                    }
                    j++;
                }
                if (pca[dg][j] == null) {
                    pca[dg][j] = plotsContainer.addPlot(cha[j], dg);
                    pca[dg][j].setAbscissa(duration[dg]);
                    Channel ch = new Channel();
                    ch.setName(cha[j].name);
                    map[dg].addChannel(ch);
                }
                j++;
            }
            for (int i = j; i < cha.length; i++) {
                if (pca[dg][i] != null) {
                    plotsContainer.removePlot(pca[dg][i], dg);
                    pca[dg][i] = null;
                    map[dg].removeChannel(map[dg].findChannel(cha[i].name));
                }
            }
        }
        if (runMode == RunModeDefs.realTime) setRTReqMode(true);
        plotsContainer.invalidate();
        plotsContainer.validate();
        plotsContainer.repaint();
    }

    private void changeLayout(int layout) {
        switch(layout) {
            case LayoutCubby.LoadConfig:
                Hashtable ht = configCubby.getHash();
                if (ht != null) RBNBApplyConfig(ht);
                configCubby.setChannel(new Channel("foo"));
                break;
            case LayoutCubby.SaveConfig:
                Hashtable configHash = configCubby.getHash();
                for (int i = 0; i < environment.DISPLAYGROUPS; i++) {
                    configHash.put("duration[" + i + "]", Double.toString(duration[i].getDoubleValue()));
                }
                configCubby.setHash(configHash);
                configCubby.setChannel(new Channel("foo"));
                break;
            case LayoutCubby.OpenRBNB:
                boolean stat = false;
                RBNBClose();
                stat = RBNBOpen(environment.HOST, environment.PORT);
                layoutCubby.setStatus(stat);
                if (stat) {
                    RBNBChannel();
                    RBNBTimeLimits();
                    RBNBDuration();
                }
                break;
            case LayoutCubby.RefreshRBNB:
                RBNBChannel();
                RBNBTimeLimits();
                break;
            case LayoutCubby.CloseRBNB:
                RBNBClose();
                break;
            case LayoutCubby.PlotMode:
                plotsContainer.setDisplayMode(LayoutCubby.PlotMode);
                newData = false;
                break;
            case LayoutCubby.TableMode:
                plotsContainer.setDisplayMode(LayoutCubby.TableMode);
                newData = false;
                break;
            case LayoutCubby.ExportToCB:
                System.err.println("rbnbPlot: Export to Clipboard is disabled");
                break;
            case LayoutCubby.ExportToDT:
                if (connection != null) {
                    if (exportToDT == null) {
                        exportToDT = new ExportToDT(rbnbPlotMain.frame, false, connection, environment);
                    }
                    exportToDT.setVisible(true);
                }
                break;
            case LayoutCubby.ExportToMatlab:
                if (connection != null) {
                    exportToMatlab = new ExportToMatlab(rbnbPlotMain.frame, false, connection, environment);
                    exportToMatlab.setVisible(true);
                }
                break;
            default:
                System.out.println("RBNBInterface.changeLayout: unknown layout " + layout);
                break;
        }
    }

    private void oneDataStep() {
        int retData = 0;
        Time oldStart = null;
        Time newStart = null;
        if (!newData) {
            switch(runMode) {
                case RunModeDefs.bof:
                    newStart = getExtremeData(false);
                    if (newStart != null) {
                        start = newStart;
                        position[0] = start;
                        position[1] = position[0];
                        if (position[0].addTime(duration[dg]).compareTo(position[2]) == 1) position[2] = position[0].addTime(duration[dg]);
                        newData = true;
                        setUpdateRate();
                    }
                    runMode = RunModeDefs.stop;
                    runModeCubby.set(RunModeDefs.stop, false);
                    break;
                case RunModeDefs.revPlay:
                    oldStart = start;
                    if (start.compareTo(position[1]) <= 0 && start.addTime(duration[dg]).compareTo(position[2]) >= 0) {
                        resetPosition(false);
                        if (start.compareTo(position[1]) <= 0 && start.addTime(duration[dg]).compareTo(position[2]) >= 0) {
                            runMode = RunModeDefs.stop;
                            runModeCubby.set(RunModeDefs.stop, false);
                        } else if (start.compareTo(position[1]) <= 0) {
                            start = position[1];
                            runMode = RunModeDefs.stop;
                            runModeCubby.set(RunModeDefs.stop, false);
                        }
                    } else {
                        double localdur = duration[dg].getDoubleValue();
                        if (localdur == 0) localdur = zerodurbase;
                        start = start.subtractTime(new Time(localdur * speed.getDoubleValue()));
                        if (start.compareTo(position[1]) == -1) {
                            resetPosition(false);
                            if (start.compareTo(position[1]) == -1) {
                                start = new Time(position[1]);
                                runMode = RunModeDefs.stop;
                                runModeCubby.set(RunModeDefs.stop, false);
                            }
                        }
                    }
                    retData = getData(false);
                    if (true) {
                        position[0] = start;
                        if (position[0].compareTo(position[1]) == -1) position[1] = position[0];
                        if (position[0].addTime(duration[dg]).compareTo(position[2]) == 1) position[2] = position[0].addTime(duration[dg]);
                        newData = true;
                        setUpdateRate();
                    } else {
                        start = start.addTime(new Time(duration[dg].getDoubleValue() * speed.getDoubleValue()));
                        runMode = RunModeDefs.stop;
                        runModeCubby.set(RunModeDefs.stop, false);
                    }
                    if (retData == -1) {
                    }
                    break;
                case RunModeDefs.revStep:
                    start = start.subtractTime(duration[dg]);
                    retData = getData(false);
                    if (retData != 0) {
                        position[0] = start;
                        if (position[0].compareTo(position[1]) == -1) position[1] = position[0];
                        if (position[0].addTime(duration[dg]).compareTo(position[2]) == 1) position[2] = position[0].addTime(duration[dg]);
                        newData = true;
                        setUpdateRate();
                    }
                    runMode = RunModeDefs.stop;
                    runModeCubby.set(RunModeDefs.stop, false);
                    break;
                case RunModeDefs.stop:
                    break;
                case RunModeDefs.fwdStep:
                    start = start.addTime(duration[dg]);
                    retData = getData(true);
                    if (retData != 0) {
                        position[0] = start;
                        if (position[0].compareTo(position[1]) == -1) position[1] = position[0];
                        if (position[0].addTime(duration[dg]).compareTo(position[2]) == 1) position[2] = position[0].addTime(duration[dg]);
                        newData = true;
                        setUpdateRate();
                    }
                    runMode = RunModeDefs.stop;
                    runModeCubby.set(RunModeDefs.stop, false);
                    break;
                case RunModeDefs.fwdPlay:
                    oldStart = start;
                    if (start.compareTo(position[1]) <= 0 && start.addTime(duration[dg]).compareTo(position[2]) >= 0) {
                        resetPosition(false);
                        if (start.compareTo(position[1]) <= 0 && start.addTime(duration[dg]).compareTo(position[2]) >= 0) {
                            runMode = RunModeDefs.stop;
                            runModeCubby.set(RunModeDefs.stop, false);
                        } else if (start.addTime(duration[dg]).compareTo(position[2]) >= 0) {
                            start = position[2].subtractTime(duration[dg]);
                            runMode = RunModeDefs.stop;
                            runModeCubby.set(RunModeDefs.stop, false);
                        }
                    } else {
                        double localdur = duration[dg].getDoubleValue();
                        if (localdur == 0) localdur = zerodurbase;
                        start = start.addTime(new Time(localdur * speed.getDoubleValue()));
                        if (start.addTime(duration[dg]).compareTo(position[2]) == 1) {
                            resetPosition(false);
                            if (start.addTime(duration[dg]).compareTo(position[2]) == 1) {
                                start = position[2].subtractTime(duration[dg]);
                                runMode = RunModeDefs.stop;
                                runModeCubby.set(RunModeDefs.stop, false);
                            }
                        }
                    }
                    retData = getData(true);
                    if (true) {
                        position[0] = start;
                        if (position[0].compareTo(position[1]) == -1) position[1] = position[0];
                        if (position[0].addTime(duration[dg]).compareTo(position[2]) == 1) position[2] = position[0].addTime(duration[dg]);
                        newData = true;
                        setUpdateRate();
                    } else {
                        start = start.subtractTime(new Time(duration[dg].getDoubleValue() * speed.getDoubleValue()));
                        runMode = RunModeDefs.stop;
                        runModeCubby.set(RunModeDefs.stop, false);
                    }
                    if (retData == -1) {
                    }
                    break;
                case RunModeDefs.eof:
                    newStart = getExtremeData(true);
                    if (newStart != null) {
                        start = newStart;
                        position[0] = start;
                        if (position[0].compareTo(position[1]) == -1) position[1] = position[0];
                        position[2] = position[0].addTime(duration[dg]);
                        newData = true;
                        setUpdateRate();
                    }
                    runMode = RunModeDefs.stop;
                    runModeCubby.set(RunModeDefs.stop, false);
                    break;
                case RunModeDefs.realTime:
                    newStart = getExtremeData(true);
                    if ((newStart != null) && (!firstRT) && (newStart.compareTo(start) == 0)) {
                        rtWaitTime *= 2;
                        try {
                            Thread.sleep(rtWaitTime);
                        } catch (InterruptedException e) {
                            System.out.println("thread sleep error: " + e);
                        }
                    } else if (newStart != null) {
                        start = newStart;
                        position[0] = start;
                        if (position[0].compareTo(position[1]) == -1) position[1] = position[0];
                        position[2] = position[0].addTime(duration[dg]);
                        newData = true;
                        setUpdateRate();
                        rtWaitTime /= 2;
                    } else {
                        try {
                            Thread.sleep(75);
                        } catch (InterruptedException e) {
                            System.out.println("thread sleep error: " + e);
                        }
                    }
                    firstRT = false;
                    if (rtWaitTime < 10) rtWaitTime = 10; else if (rtWaitTime > 640) rtWaitTime = 640;
                    if (environment.RTWAIT > 0) {
                        try {
                            Thread.sleep(environment.RTWAIT);
                        } catch (InterruptedException e) {
                            System.err.println("thread sleep error: " + e);
                        }
                    }
                    break;
                case RunModeDefs.allData:
                    getAllData();
                    runMode = RunModeDefs.stop;
                    runModeCubby.set(RunModeDefs.stop, false);
                    newData = false;
                    setUpdateRate();
                    break;
                case RunModeDefs.current:
                    retData = getData(true);
                    if (retData != 0) {
                        position[0] = start;
                        if (position[0].compareTo(position[1]) == -1) position[1] = position[0];
                        if (position[0].addTime(duration[dg]).compareTo(position[2]) == 1) position[2] = position[0].addTime(duration[dg]);
                        newData = true;
                        setUpdateRate();
                    }
                    newData = true;
                    Integer newRunMode = runModeCubby.get(false);
                    if (newRunMode != null) {
                        runMode = newRunMode.intValue();
                    } else {
                        runMode = RunModeDefs.stop;
                    }
                    runModeCubby.set(runMode, false);
                    break;
                default:
                    System.out.println("runMode not implemented: " + runMode);
                    runMode = RunModeDefs.stop;
                    runModeCubby.set(RunModeDefs.stop, false);
                    break;
            }
        }
        if (newData) {
            PlotContainer[] pc = new PlotContainer[channel.length];
            for (int i = 0; i < channel.length; i++) {
                pc[i] = findPC(channel[i].channelName);
            }
            repaintContainer = -1;
            if (updatePosition) posDurCubby.setPosition(position, false); else updatePosition = true;
            for (int i = 0; i < channel.length; i++) if (pc[i] != null) pc[i].setChannelData(channel[i], position[0]);
            newData = false;
        }
    }

    private void setUpdateRate() {
        long thisUpdate = (new Date()).getTime();
        if ((thisUpdate - lastUpdate) > 1000 || updatePeriod == 0 || updatePeriod > 1000) {
            updatePeriod = thisUpdate - lastUpdate;
        } else {
            updatePeriod = ((thisUpdate - lastUpdate) + 7 * updatePeriod) / 8;
        }
        lastUpdate = thisUpdate;
        try {
            posDurCubby.setUpdateRate(ToString.toString("%.2g", 1000.0 / updatePeriod) + " Updates/Sec");
        } catch (Exception e) {
            System.out.println("RBNBInterface.setUpdateRate exception");
            e.printStackTrace();
        }
    }

    /********************
     *
     *  getExtremeData()
     *
     *******************/
    public Time getExtremeData(boolean atEOF) {
        Time newStart = null;
        if (map[dg].channelList().length > 0 && connection != null) {
            try {
                if (rtReqMode) {
                    Map lmap = connection.streamGetMap();
                    if (lmap == null) {
                        rtTimeout = true;
                        setRTReqMode(false);
                    } else {
                        Channel[] ch = lmap.channelList();
                        if (ch == null || ch.length < 1 || ch[0].numberOfPoints == 0 || ch[0].timeStamp == null) {
                            if (environment.SLAVEMODE) {
                                rtTimeout = false;
                                setRTReqMode(true);
                                return null;
                            } else {
                                rtTimeout = true;
                                setRTReqMode(false);
                            }
                        } else {
                            map[dg] = lmap;
                        }
                    }
                }
                if (!rtReqMode) {
                    if (atEOF) {
                        connection.getMap(map[dg], null, duration[dg], flags | DataRequest.newest | DataRequest.aligned);
                    } else connection.getMap(map[dg], null, duration[dg], flags | DataRequest.oldest | DataRequest.aligned);
                }
                channel = map[dg].channelList();
                for (int i = 0; i < channel.length; i++) {
                    if (channel[i].timeStamp != null) {
                        posDurCubby.setTimeFormat(channel[i].timeStamp.format);
                        i = channel.length;
                    }
                }
                if (atEOF) {
                    Time last = null;
                    Time newlast = null;
                    for (int i = 0; i < channel.length; i++) {
                        if (channel[i].numberOfPoints > 0) {
                            newlast = channel[i].timeStamp.getEndOfInterval(DataTimeStamps.last);
                            if (last == null) last = newlast; else if (newlast.compareTo(last) == 1) last = newlast;
                        }
                    }
                    if (last == null) newStart = last; else newStart = last.subtractTime(duration[dg]);
                } else {
                    Time first = null;
                    Time newfirst = null;
                    for (int i = 0; i < channel.length; i++) {
                        if (channel[i].numberOfPoints > 0) {
                            newfirst = channel[i].timeStamp.getStartOfInterval(0);
                            if (first == null) first = newfirst; else if (newfirst.compareTo(first) == -1) first = newfirst;
                        }
                    }
                    newStart = first;
                }
            } catch (Exception e) {
                System.out.println("RBNB data exception: " + e);
                e.printStackTrace();
                runMode = RunModeDefs.stop;
                if (rtReqMode) setRTReqMode(false);
                runModeCubby.set(RunModeDefs.stop, false);
                return null;
            }
        } else {
        }
        return newStart;
    }

    public int getData(boolean fwdDirection) {
        boolean gotData = false, adjusted = false;
        if (map[dg].channelList().length > 0 && connection != null) {
            try {
                connection.getData(map[dg], start, duration[dg], flags);
                channel = map[dg].channelList();
                for (int i = 0; i < channel.length; i++) {
                    if (channel[i].numberOfPoints > 0) {
                        posDurCubby.setTimeFormat(channel[i].timeStamp.format);
                        i = channel.length;
                    }
                }
                Time first = null, last = null, time = null;
                for (int i = 0; i < channel.length; i++) {
                    if (channel[i].numberOfPoints > 0) {
                        time = channel[i].timeStamp.getStartOfInterval(DataTimeStamps.first);
                        if (!gotData) first = time; else if (time.compareTo(first) == -1) first = time;
                        time = channel[i].timeStamp.getEndOfInterval(DataTimeStamps.last);
                        if (!gotData) {
                            gotData = true;
                            last = time;
                        } else if (time.compareTo(last) == 1) last = time;
                    }
                }
                if (!gotData) return 0;
            } catch (Exception e) {
                System.out.println("RBNB data exception: " + e);
                e.printStackTrace();
                runMode = RunModeDefs.stop;
                runModeCubby.set(RunModeDefs.stop, false);
                return 0;
            }
            if (!gotData) for (int i = 0; i < channel.length; i++) {
                if (!gotData) if (channel[i].numberOfPoints > 0) gotData = true;
            }
            if (!gotData) return 0;
        } else {
            return 0;
        }
        if (adjusted) return -1; else return 1;
    }

    private void getAllData() {
        Time dur = null;
        if (map[dg].channelList().length > 0 && connection != null) {
            try {
                connection.getTimeLimits(map[dg]);
                channel = map[dg].channelList();
                Time first = null;
                Time last = null;
                Time newtime = null;
                for (int i = 0; i < channel.length; i++) {
                    if (channel[i].timeStamp != null) {
                        newtime = channel[i].timeStamp.getStartOfInterval(DataTimeStamps.first);
                        if (first == null) first = newtime; else if (newtime.compareTo(first) == -1) first = newtime;
                        newtime = channel[i].timeStamp.getEndOfInterval(DataTimeStamps.last);
                        if (last == null) last = newtime; else if (newtime.compareTo(last) == 1) last = newtime;
                    }
                }
                if (first == null || last == null) {
                    System.out.println("RBNBInterface.getAllData: no data available!");
                    runMode = RunModeDefs.stop;
                    runModeCubby.set(RunModeDefs.stop, false);
                    return;
                }
                dur = last.subtractTime(first);
                connection.getData(map[dg], first, dur, flags);
                channel = map[dg].channelList();
                for (int i = 0; i < channel.length; i++) {
                    if (channel[i].numberOfPoints > 0) {
                        posDurCubby.setTimeFormat(channel[i].timeStamp.format);
                        i = channel.length;
                    }
                }
                start = first;
                duration[dg] = dur;
                position[0] = start;
                position[1] = position[0];
                position[2] = last;
                posDurCubby.setPosition(position, false);
                posDurCubby.setDuration(duration[dg], false);
            } catch (Exception e) {
                System.out.println("RBNB data exception: " + e);
                e.printStackTrace();
                runMode = RunModeDefs.stop;
                runModeCubby.set(RunModeDefs.stop, false);
                return;
            }
            for (int i = 0; i < channel.length; i++) {
                PlotContainer pc = findPC(channel[i].channelName);
                if (pc != null) {
                    pc.setAbscissa(duration[dg]);
                    pc.setChannelData(channel[i], start);
                }
            }
        } else {
        }
    }

    public void resetPosition(boolean adjustStart) {
        Time bof = null, eof = null;
        if (map[dg].channelList().length > 0 && connection != null) {
            try {
                if (rtReqMode) setRTReqMode(false);
                connection.getTimeLimits(map[dg]);
                if (runMode == RunModeDefs.realTime) setRTReqMode(true);
                channel = map[dg].channelList();
                Time newtime = null;
                for (int i = 0; i < channel.length; i++) {
                    if (channel[i].timeStamp != null) {
                        newtime = channel[i].timeStamp.getStartOfInterval(DataTimeStamps.first);
                        if (bof == null) bof = newtime; else if (newtime.compareTo(bof) == -1) bof = newtime;
                        newtime = channel[i].timeStamp.getEndOfInterval(DataTimeStamps.last);
                        if (eof == null) eof = newtime; else if (newtime.compareTo(eof) == 1) eof = newtime;
                    }
                }
                if (bof == null || eof == null) return;
                position[1] = bof;
                position[2] = eof;
                if (position[0].compareTo(position[1]) == -1) position[0] = position[1];
                if (position[0].addTime(duration[dg]).compareTo(position[2]) == 1) position[0] = position[2].subtractTime(duration[dg]);
                if (adjustStart) start = position[0];
                posDurCubby.setPosition(position, false);
            } catch (Exception e) {
                System.out.println("RBNBInterface.init: caught " + e);
                e.printStackTrace();
            }
        } else {
        }
    }

    private PlotContainer findPC(String chanName) {
        for (int i = 0; i < cha.length; i++) {
            if (chanName.equals(cha[i].name)) return pca[dg][i];
        }
        System.out.println("RBNBInterface.findPC: channel not found: " + chanName);
        return (null);
    }

    /********************
     *
     *  incrementSpeed()
     *
     *******************/
    private void incrementSpeed(boolean reset) {
        if (reset) {
            speedMant = 2;
            speedExp = -2;
        } else {
            if (speedMant == 1) speedMant = 2; else if (speedMant == 2) speedMant = 5; else {
                speedMant = 1;
                speedExp += 1;
            }
        }
        speed = new Time(speedMant, speedExp);
    }

    /********************
     *
     *  decrementSpeed()
     *
     *******************/
    private void decrementSpeed() {
        if (speedMant == 5) speedMant = 2; else if (speedMant == 2) speedMant = 1; else {
            speedMant = 5;
            speedExp -= 1;
        }
        speed = new Time(speedMant, speedExp);
    }
}
