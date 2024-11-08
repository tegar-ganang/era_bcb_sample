package com.handjoys.console;

import java.util.Vector;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.channels.SocketChannel;
import com.handjoys.Broadcaster;
import com.handjoys.conf.ConfigParam;
import com.handjoys.conf.ConfigReader;
import com.handjoys.logger.FileLogger;
import com.handjoys.fastdb.FastDB;
import com.handjoys.packet.MessagePacket;
import com.handjoys.packet.SingleLine;
import com.handjoys.pojo.ServerInfo;
import com.handjoys.socket.RServerFactory;

public class GameState {

    private static final float HASH_LOADFACTOR = 0.5f;

    private static final int MAXROOM = ((Integer) (ConfigReader.getParam(ConfigParam.MAXROOM))).intValue();

    private static final int MAXROOMUSER = ((Integer) (ConfigReader.getParam(ConfigParam.MAXROOMUSER))).intValue();

    private static final int INITIALCAPACITY = MAXROOMUSER * ((int) (1 / HASH_LOADFACTOR));

    private static Map<String, Map<String, MovableObject>> userList;

    private static Map<String, MovableObject> channelList;

    private static Vector<Map<String, MovableObject>> mapPool = new Vector<Map<String, MovableObject>>();

    private static Map<Integer, Integer> roomGenerator = new HashMap<Integer, Integer>(INITIALCAPACITY, HASH_LOADFACTOR);

    public static final int ROOMTYPE_MAP = 1;

    public static final int ROOMTYPE_HOME = 2;

    public static final int ROOMTYPE_MULTIGAMES = 3;

    /**
	 * add by wanggl, 服务器运行信息
	 */
    private static Map<String, ServerRuntimeInfo> serverInfo;

    private GameState() {
    }

    public static void init() {
        userList = new HashMap<String, Map<String, MovableObject>>(MAXROOM * ((int) (1 / HASH_LOADFACTOR)), HASH_LOADFACTOR);
        channelList = new ConcurrentHashMap<String, MovableObject>((MAXROOM * MAXROOMUSER) * (int) (1 / HASH_LOADFACTOR), HASH_LOADFACTOR, 2);
        serverInfo = new ConcurrentHashMap<String, ServerRuntimeInfo>();
        for (int i = 0; i < MAXROOM; i++) {
            Map<String, MovableObject> map = new HashMap<String, MovableObject>(INITIALCAPACITY, HASH_LOADFACTOR);
            mapPool.add(map);
        }
    }

    /**
	 * 获取Server运行信息
	 * @return
	 */
    public static Map<String, ServerRuntimeInfo> getServerInfo() {
        return serverInfo;
    }

    /**
	 * 设置server运行信息
	 * @param serverInfo
	 */
    public static void setServerInfo(Map<String, ServerRuntimeInfo> serverInfo) {
        GameState.serverInfo = serverInfo;
    }

    /**增加server运行信息
	 * @param serverid  服务器的标识 
	 * @param serverInfo 服务运行信息
	 */
    public static void addServerInfo(String serverid, ServerRuntimeInfo serverInfo) {
        if (serverid != null && serverInfo != null) {
            GameState.serverInfo.put(serverid, serverInfo);
        }
    }

    public static void joinRoom(MovableObject mo) {
        Map<String, MovableObject> map;
        if ((map = userList.get(mo.getRoomId() + "")) == null) {
            if (mapPool.size() == 0) {
                map = new HashMap<String, MovableObject>(INITIALCAPACITY, HASH_LOADFACTOR);
            } else {
                map = (HashMap<String, MovableObject>) mapPool.remove(0);
            }
            userList.put(mo.getRoomId() + "", map);
        }
        mo.lastTime = System.currentTimeMillis();
        map.put(mo.getId() + "", mo);
        channelList.put(mo.sessionID, mo);
        try {
            String mp = "<msg t='sys'><body action='10000.00017' r='1'>$41|" + mo.getId() + "|" + mo.getRoomId() + "</body></msg>";
            Broadcaster.write2ASServer(mp);
        } catch (Exception e) {
        }
    }

    public static void exitRoom(MovableObject mo) {
        ((Map<String, MovableObject>) userList.get(mo.getRoomId() + "")).remove(mo.getId() + "");
        channelList.remove(mo.sessionID);
    }

    public static void exitRoomBySession(String sessionID) {
        MovableObject mo = getMoBySession(sessionID);
        ((Map<String, MovableObject>) userList.get(mo.getRoomId() + "")).remove(mo.getId() + "");
        channelList.remove(mo.sessionID);
    }

    public static void updateMo(MovableObject mo) {
        mo.lastTime = System.currentTimeMillis();
        ((Map<String, MovableObject>) userList.get(mo.getRoomId() + "")).put(mo.getId() + "", mo);
        channelList.put(mo.sessionID, mo);
    }

    public static Map getAllMo() {
        return userList;
    }

    public static Map getInteractiveMo(MovableObject mo) {
        return ((Map<String, MovableObject>) userList.get(mo.getRoomId() + ""));
    }

    public static MovableObject getMo(MovableObject mo) {
        Map<String, MovableObject> map = (Map<String, MovableObject>) userList.get(mo.getRoomId() + "");
        if (map == null) return null;
        return (MovableObject) map.get(mo.getId() + "");
    }

    public static MovableObject getMoBySession(String sessionID) {
        return (MovableObject) channelList.get(sessionID);
    }

    public static MovableObject getMoById(MovableObject mo) {
        MovableObject retMo = null;
        for (Iterator ir = userList.keySet().iterator(); ir.hasNext(); ) {
            String roomID = (String) ir.next();
            Map<String, MovableObject> moList = (Map<String, MovableObject>) userList.get(roomID);
            for (Iterator irr = moList.keySet().iterator(); irr.hasNext(); ) {
                int spriteid = Integer.parseInt((String) irr.next());
                if (spriteid == mo.id) {
                    retMo = (MovableObject) moList.get(spriteid + "");
                    return retMo;
                }
            }
        }
        return retMo;
    }

    public static Map getInteractiveMoBySession(String sessionID) {
        MovableObject mo = (MovableObject) channelList.get(sessionID);
        if (mo == null) return null;
        Map<String, MovableObject> map = (Map<String, MovableObject>) userList.get(mo.getRoomId() + "");
        return map;
    }

    public static final int ROOMTYPE_HOME_FEED = 100000;

    public static final int ROOMTYPE_MULTIGAMES_FEED = 1000000;

    public static int registerRoom(int roomType, int roomid) {
        int retRoomid = 0;
        Integer retRoomidO = new Integer(retRoomid);
        switch(roomType) {
            case ROOMTYPE_MAP:
                retRoomid = roomid;
                break;
            case ROOMTYPE_HOME:
                retRoomid = ROOMTYPE_HOME_FEED + roomid;
                break;
            case ROOMTYPE_MULTIGAMES:
                synchronized (retRoomidO) {
                    roomType = roomid;
                    Integer multGames_roomID = (Integer) roomGenerator.get(new Integer(roomType));
                    if (multGames_roomID == null) {
                        retRoomid = ROOMTYPE_MULTIGAMES_FEED + roomid;
                    } else {
                        retRoomid = multGames_roomID.intValue();
                    }
                }
                break;
            default:
                retRoomid = 0;
                break;
        }
        roomGenerator.put(new Integer(roomType), new Integer(retRoomid));
        return retRoomid;
    }

    public static void releaseRoom(int roomType, int roomid) {
        if (ROOMTYPE_MULTIGAMES == roomType) {
            roomGenerator.remove(new Integer(roomid));
        }
    }

    public static void updateGameRoom(int roomid) {
        int gameid = roomid - ROOMTYPE_MULTIGAMES_FEED;
        Integer multGames_roomID = (Integer) roomGenerator.get(new Integer(gameid));
        roomGenerator.put(new Integer(gameid), new Integer(roomid + 1));
    }

    public static int getRoomSecondID(int roomid) {
        if (roomid < ROOMTYPE_HOME_FEED) return roomid;
        if (roomid >= ROOMTYPE_HOME_FEED && roomid < ROOMTYPE_MULTIGAMES_FEED) return roomid - ROOMTYPE_HOME_FEED;
        if (roomid >= ROOMTYPE_MULTIGAMES_FEED) return roomid - ROOMTYPE_MULTIGAMES_FEED;
        return roomid;
    }

    public static int getRoomType(int roomid) {
        if (roomid < ROOMTYPE_HOME_FEED) return ROOMTYPE_MAP;
        if (roomid >= ROOMTYPE_HOME_FEED && roomid < ROOMTYPE_MULTIGAMES_FEED) return ROOMTYPE_HOME;
        if (roomid >= ROOMTYPE_MULTIGAMES_FEED) return ROOMTYPE_MULTIGAMES;
        return ROOMTYPE_MAP;
    }

    public static Map<String, MovableObject> getChannelList() {
        return channelList;
    }

    public static String printUserList() {
        StringBuffer sb = new StringBuffer();
        FileLogger.info("userList size: " + userList.size());
        int count = 0;
        for (Iterator ir = userList.keySet().iterator(); ir.hasNext(); ) {
            String roomID = (String) ir.next();
            FileLogger.info("room ID: " + roomID);
            Map moList = (Map) userList.get(roomID);
            for (Iterator irr = moList.keySet().iterator(); irr.hasNext(); ) {
                String spriteid = (String) irr.next();
                MovableObject mo = (MovableObject) moList.get(spriteid);
                FileLogger.info("		sprite id: " + spriteid);
                FileLogger.info("		sprite sessionID: " + mo.sessionID);
                sb.append("%$5001|");
                sb.append(spriteid).append("|").append(roomID).append("|").append(mo.x).append("|").append(mo.y).append("|").append(mo.z).append("|").append(mo.dir).append("|").append(mo.state).append("|").append(mo.lastTime).append("|").append(mo.client).append("|").append(mo.sessionID);
                sb.append("%$!@$");
                count++;
            }
        }
        if (count > 0) return sb.toString().substring(0, sb.toString().length() - 4); else return sb.toString();
    }

    /**
	 * 向管理服务器发送到管理服务器
	 * @param islogin 是登陆么？true代表登陆，false代表登出
	 * @param id 用户或者宠物的id
	 * @param type 类型 1代表用户 2 代表宠物 3代表家长登陆
	 **/
    public static final void sendLogOnInfo2ManageServer(boolean islogin, long id, int type) {
        MessagePacket mp = new MessagePacket();
        mp.setAction("50000.90001");
        SingleLine line = new SingleLine();
        line.setCode("$9001");
        line.addItem(id + "");
        line.addItem(islogin ? "1" : "0");
        line.addItem(type + "");
        mp.insertALine(line);
        mp.setType("admin");
        mp.setResult(true);
        try {
            Broadcaster.write(mp.toString(), RServerFactory.getRServer("ManageServer"));
        } catch (Exception e) {
        }
    }
}
