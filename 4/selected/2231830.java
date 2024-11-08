package com.handjoys.socket;

import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.TimerTask;
import com.handjoys.Broadcaster;
import com.handjoys.logger.FileLogger;
import com.handjoys.conf.ConfigParam;
import com.handjoys.conf.ConfigReader;
import com.handjoys.fastdb.FastDB;
import com.handjoys.socket.GSession;
import com.handjoys.console.GameState;
import com.handjoys.console.MovableObject;
import com.handjoys.account.AccountState;
import com.handjoys.dbpool.DBProxy;

public class ConnectionCleanerTask extends TimerTask {

    private int maxIdleTime = ((Integer) (ConfigReader.getParam(ConfigParam.MAXUSERIDLETIME))).intValue();

    private LinkedList<GSession> killableChannels;

    private long lastTime;

    private long currTime;

    public ConnectionCleanerTask() {
    }

    public void run() {
        killableChannels = new LinkedList<GSession>();
        Map<String, MovableObject> channelList = GameState.getChannelList();
        FileLogger.info("SessionCleaner starting......, channelList size=" + channelList.size());
        Iterator it = channelList.keySet().iterator();
        currTime = System.currentTimeMillis();
        while (it.hasNext()) {
            try {
                String sessionID = (String) it.next();
                MovableObject mo = (MovableObject) channelList.get(sessionID);
                SocketChannel client = mo.client;
                if (client != null && client.isConnected()) {
                    lastTime = mo.lastTime;
                    FileLogger.debug("lastTime=" + lastTime);
                    FileLogger.debug("currTime=" + currTime);
                    FileLogger.debug("maxIdleTime=" + maxIdleTime);
                    if (((currTime - lastTime) > (long) maxIdleTime) && !mo.isAdmin) {
                        addKillable(new GSession(sessionID, client));
                    }
                } else {
                    GameState.exitRoom(mo);
                    AccountState.logout(new GSession(sessionID, client));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        killEmAll();
        FastDB.maintanceDBConnection();
        DBProxy.checkConncetions();
    }

    private void addKillable(GSession session) {
        killableChannels.add(session);
    }

    private void killEmAll() {
        if (killableChannels.size() > 0) {
            for (Iterator i = killableChannels.iterator(); i.hasNext(); ) {
                GSession session = (GSession) i.next();
                if (session != null) {
                    FileLogger.info("Session: " + session.sessionID + " was found idle and was disconnected.");
                    String msg = "<msg t='sls' s='@" + session.sessionID + "'><body action='0.0' r='1'></body></msg>";
                    Broadcaster.write(msg, session);
                }
                i.remove();
            }
        }
    }
}
