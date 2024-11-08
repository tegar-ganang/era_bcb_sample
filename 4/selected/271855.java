package qq2009Betal03.QQClient;

import java.util.ArrayList;
import java.util.List;

public class ClientTalkTool {

    private static ClientThread clientThread;

    public ClientTalkTool() {
    }

    public ClientTalkTool(ClientThread cThread) {
        this();
        this.clientThread = cThread;
    }

    private static List<TalkFrame> onlineFriendframeList = new ArrayList<TalkFrame>();

    static {
        for (int i = 0; i < 50; i++) {
            TalkFrame talkframe = new TalkFrame();
            talkframe.setDestQQ(i);
            talkframe.setClientThread(clientThread);
            talkframe.setTitle("��" + i + "��̸��");
            onlineFriendframeList.add(i, talkframe);
        }
    }

    public static TalkFrame getTalkFrame(int destqq) {
        System.out.println("onlineFriendframeList����ʾ�� ��" + destqq);
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
}
