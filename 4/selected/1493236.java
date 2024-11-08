package com.handjoys.console;

import java.util.*;
import java.nio.channels.SocketChannel;
import com.handjoys.account.AccountState;
import com.handjoys.conf.ConfigParam;
import com.handjoys.conf.ConfigReader;
import com.handjoys.gcm.GameComponent;
import com.handjoys.packet.MessagePacket;
import com.handjoys.logger.FileLogger;
import com.handjoys.fastdb.FastDB;
import com.handjoys.GameServer;
import com.handjoys.Broadcaster;
import com.handjoys.socket.GSession;
import com.handjoys.socket.GBBServer;

public class Console extends GameComponent {

    public String handle(MessagePacket mp, GSession session) throws Exception {
        StringBuffer bf = new StringBuffer("");
        switch(Integer.parseInt(mp.getLActionPara())) {
            case 1:
                bf.append("<cross-domain-policy><allow-access-from domain='*' to-ports='*' /></cross-domain-policy>");
                break;
            case 2:
                bf.append("<msg t='admin'><body action='50000.00002' r='1'>");
                bf.append(GameState.printUserList());
                bf.append("</body></msg>");
                break;
            case 3:
                bf.append("<msg t='admin'><body action='50000.00003' r='1'>");
                bf.append(printSockets());
                bf.append("</body></msg>");
                break;
            case 5:
                bf.append("<msg t='admin'><body action='50000.00005' r='1'>");
                bf.append(printFastDB(mp));
                bf.append("</body></msg>");
                break;
            case 6:
                flushEntity(mp);
                bf.append("<msg t='admin'><body action='50000.00006' r='1'>");
                bf.append("</body></msg>");
                break;
            case 7:
                bf.append("<msg t='admin'><body action='50000.00007' r='1'>");
                bf.append(reloadEntity(mp));
                bf.append("</body></msg>");
                break;
            case 20:
                printAccountList();
                break;
            case 10000:
                lostClient(mp, session);
                break;
            case 20000:
                bf.append(checkRServer(mp, session));
                break;
            case 30000:
                updateFastDB(mp);
                break;
            case 40000:
                bf.append(checkGBBServer(mp, session));
                break;
            default:
                break;
        }
        return bf.toString();
    }

    private String printSockets() {
        StringBuffer buffer = new StringBuffer();
        int count = 0;
        Queue clients = GameServer.getInstance().getChannels();
        for (Iterator ir = clients.iterator(); ir.hasNext(); ) {
            SocketChannel client = (SocketChannel) ir.next();
            buffer.append("%$5002|").append(client.socket().getRemoteSocketAddress().toString());
            buffer.append("|");
            buffer.append("0").append("|");
            buffer.append(client.isConnected());
            buffer.append("$!@$");
            count++;
        }
        if (count > 0) return buffer.toString().substring(0, buffer.toString().length() - 4); else return buffer.toString();
    }

    private String printFastDB(MessagePacket mp) {
        List<String> line = mp.getLineContent(1);
        String entityName = line.get(0);
        FastDB.printEntity(entityName);
        return "";
    }

    private String checkRServer(MessagePacket mp, GSession session) {
        String sessionID = mp.getSession();
        String str[] = sessionID.split("#");
        FastDB.checkRServer(str[0], session);
        return "";
    }

    private String checkGBBServer(MessagePacket mp, GSession session) {
        GBBServer.setInstance(session);
        return "";
    }

    private void updateFastDB(MessagePacket mp) {
        List<String> line = mp.getLineContent(1);
        String entityName = line.get(0);
        String optType = line.get(1);
        Long primaryKey = new Long(Long.parseLong(line.get(2)));
        if (optType.equals(FastDB.OPTTYPE_UPDATE)) {
            FastDB.updateEntityInMemory(entityName, primaryKey);
        } else if (optType.equals(FastDB.OPTTYPE_REMOVE)) {
            FastDB.removeEntityInMemory(entityName, primaryKey);
        }
    }

    private void lostClient(MessagePacket mp, GSession session) {
        String sessionID = mp.getSession();
        try {
            Map userList = GameState.getInteractiveMoBySession(sessionID);
            if (userList != null) {
                StringBuffer msg = new StringBuffer("<msg t='sls'><body action='60000.00002' r='1'>%$1005|").append(GameState.getMoBySession(sessionID).getId()).append("%</body></msg>");
                MovableObject myMo = null;
                for (Iterator ir = userList.values().iterator(); ir.hasNext(); ) {
                    MovableObject mo = (MovableObject) ir.next();
                    if (mo.sessionID.equals(sessionID)) {
                        myMo = mo;
                        continue;
                    }
                    Broadcaster.write(msg.toString(), new GSession(mo.sessionID, mo.client));
                }
                if (myMo != null) {
                    FastDB.onSpriteLeaveWorld(new Integer(myMo.getId()).longValue());
                    GameState.exitRoom(myMo);
                }
            }
            AccountState.logout(session);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printAccountList() {
        Map getPlayerMap = AccountState.getPlayerMap();
        FileLogger.debug("AccountState=" + getPlayerMap);
    }

    private String reloadEntity(MessagePacket mp) {
        List<String> line = mp.getLineContent(1);
        String entityName = line.get(0);
        Long a = Long.parseLong(line.get(1));
        Long b = Long.parseLong(line.get(2));
        FileLogger.debug("entityName=" + entityName + " : a=" + a + " : b=" + b);
        com.handjoys.pojo.IgnoreList bb = new com.handjoys.pojo.IgnoreList();
        bb.setSpriteid(a);
        bb.setIgnoreid(b);
        try {
            Long key = FastDB.getPrimaryKey(bb);
            System.out.println("key=" + key);
            FastDB.printEntity("IgnoreList");
            FastDB.removeEntity("IgnoreList", key);
            FastDB.printEntity("IgnoreList");
            Vector<Object> IgnoreListV = null;
            try {
                IgnoreListV = FastDB.queryEntityListByIndex("IgnoreList", "spriteid", new Long(a));
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("IgnoreListV=" + IgnoreListV);
            if (IgnoreListV == null) {
                return "";
            }
            for (Iterator ir = IgnoreListV.iterator(); ir.hasNext(); ) {
                com.handjoys.pojo.IgnoreList ignore = (com.handjoys.pojo.IgnoreList) ir.next();
                System.out.println("ignore.id=" + ignore.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private void flushEntity(MessagePacket mp) {
        List<String> line = mp.getLineContent(1);
        String userName = line.get(0);
        System.out.println(userName);
        com.handjoys.pojo.Player player = new com.handjoys.pojo.Player();
        player.setPlayerid(10L);
        player.setUsername(userName);
        player.setPassword("test");
        player.setFatherpassword("test");
        player.setEmail("wuthering_ware@sina.com");
        player.setTelephone1("");
        player.setTelephone2("");
        player.setTelephone3("");
        player.setFullname("");
        player.setCharactertype("1");
        player.setCharacternum("111111");
        player.setAddress("");
        player.setMobiletele("");
        player.setBirthday("1980");
        player.setSex(new Integer(1));
        player.setCountry("china");
        player.setProvince("hefei");
        player.setCity("hefei");
        player.setPostcode("");
        player.setRecommend("");
        player.setDegree(1);
        player.setState(1);
        player.setType(new Integer(1));
        player.setCreatetime(new java.util.Date());
        try {
            FastDB.updateEntity(player);
            FastDB.printEntity("Player");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
