package QQV4Client;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientTalkTool {

    private static ClientThread clientThread;

    public java.util.List<TeamInfo> teamList = new java.util.ArrayList<TeamInfo>();

    public java.util.List<UserInfo> frienfsList = new java.util.ArrayList<UserInfo>();

    private static ClientTalkTool clientTalkTool;

    private ClientTalkTool() {
    }

    public static ClientTalkTool getClientTalkTool() {
        if (null == clientTalkTool) {
            clientTalkTool = new ClientTalkTool();
        }
        return clientTalkTool;
    }

    public ClientTalkTool(ClientThread cThread) {
        this();
        this.clientThread = cThread;
        clientTalkTool = this;
    }

    private static List<TalkFrame> onlineFriendframeList = new ArrayList<TalkFrame>();

    public static TalkFrame getTalkFrame(int destqq) {
        logger.info("onlineFriendframeList����ʾ�� ��" + destqq);
        return onlineFriendframeList.get(destqq);
    }

    public static void noticeOneUserTextmsg(int msgLength, int msgType, int myQQ, int destQQ, byte[] msgdata) {
        String msg = new String(msgdata);
        for (int i = 0; i < onlineFriendframeList.size(); i++) {
            TalkFrame talkframe = onlineFriendframeList.get(i);
            int dest = talkframe.getDestQQ();
            if (dest == myQQ) {
                talkframe.setMsg(myQQ, msg);
                talkframe.setOwerQQ(destQQ);
                talkframe.setClientThread(clientThread);
                break;
            }
        }
    }

    public static void noticOneUserFileMsg(int msgLength, int msgType, int myQQ, int destQQ, byte filenamelen, byte[] filename, byte[] filedata) {
        String fileShortNmae = new String(filename);
        String filePath = "D:\\" + fileShortNmae;
        String msg = "�ҷ�����һ���ļ���Ϊ" + fileShortNmae + ".\r\n������:" + filePath;
        for (int i = 0; i < onlineFriendframeList.size(); i++) {
            TalkFrame talkframe = onlineFriendframeList.get(i);
            int dest = talkframe.getDestQQ();
            if (dest == myQQ) {
                talkframe.setMsg(myQQ, msg);
                talkframe.setOwerQQ(destQQ);
                talkframe.setClientThread(clientThread);
                talkframe.setVisible(true);
                talkframe.getClientThread().write(filePath, filedata);
                break;
            }
        }
    }

    /**
	 * @param type Ϊ51:�����غ�������Ϣ  52Ϊ���غ�����Ϣ
	 */
    public static void getFriend(int type) {
        int owerqq = ClientThread.ownerqq;
        try {
            clientThread.sendGetFriendMsg(16, type, owerqq, owerqq);
        } catch (IOException e) {
            logger.eeror(e.getMessage());
        }
    }

    public java.util.List<TeamInfo> getTeamList() {
        if (teamList.size() != 0) {
            for (int i = 0; i < teamList.size(); i++) {
                int teamID = teamList.get(i).getId();
                logger.info("��IDΪ��" + teamID);
                for (int r = 0; r < frienfsList.size(); r++) {
                    UserInfo userInfo = frienfsList.get(r);
                    int usersuoshuTeamId = userInfo.getId_team();
                    int userQQ = userInfo.getQq();
                    String userName = userInfo.getName();
                    TalkFrame talkframe = new TalkFrame();
                    talkframe.setDestQQ(userQQ);
                    talkframe.setClientThread(clientThread);
                    talkframe.setTitle("��" + userName + "��̸��");
                    onlineFriendframeList.add(userQQ, talkframe);
                    if (usersuoshuTeamId == teamID) {
                        teamList.get(i).usrs.add(userInfo);
                    }
                }
            }
        } else {
            logger.eeror("�����ĿΪ�㣡��");
        }
        return teamList;
    }

    public void parseTeamData(byte[] teamdata) throws IOException {
        java.io.ByteArrayInputStream in = new ByteArrayInputStream(teamdata);
        java.io.DataInputStream dins = new DataInputStream(in);
        for (int i = 0; i < teamdata.length / 16; i++) {
            int teamID = dins.readInt();
            byte[] teamnamedata = new byte[12];
            dins.read(teamnamedata);
            String teamName = new String(teamnamedata).trim();
            logger.info("===============================================");
            logger.info("-----�����ͻ��յ�����ID teamID:  " + teamID);
            logger.info("-----�����ͻ��յ��������� teamName:  " + teamName);
            logger.info("===============================================");
            TeamInfo teamInfo = new TeamInfo(teamID, teamName);
            teamList.add(teamInfo);
        }
    }

    public void parseUsersData(byte[] userdata) throws IOException {
        java.io.ByteArrayInputStream in = new ByteArrayInputStream(userdata);
        java.io.DataInputStream dins = new DataInputStream(in);
        for (int i = 0; i < userdata.length / 16; i++) {
            int userQQ = dins.readInt();
            int userTeamID = dins.readInt();
            byte[] result = new byte[12];
            dins.read(result);
            String userName = new String(result).trim();
            logger.info("========================" + i + "===========================");
            logger.info(" -----�����ͻ��յ��˺���           QQ:  " + userQQ);
            logger.info(" -----�����ͻ��յ��˺��������� teamID:  " + userTeamID);
            logger.info("-----�����ͻ��յ��˺�������  userName:  " + userName);
            logger.info("=======================================================");
            UserInfo usetinfo = new UserInfo(userQQ, userTeamID, userName);
            frienfsList.add(usetinfo);
        }
    }
}
