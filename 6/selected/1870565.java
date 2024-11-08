package com.zzh.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.OfflineMessageManager;
import org.jivesoftware.smackx.muc.MultiUserChat;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class LoginActivity extends Activity {

    TextView chat;

    EditText username, password;

    Button login;

    String TAG = "XMPPIM";

    MultiUserChat multiUserChat;

    int notification_id = 19172439;

    ChatManager chatmanager;

    Chat newChat;

    private ListView mList;

    private String messages = "";

    private Handler mHandler = new Handler();

    String fromName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loginpage);
        username = (EditText) this.findViewById(R.id.login_edit_account);
        password = (EditText) this.findViewById(R.id.login_edit_pwd);
        login = (Button) this.findViewById(R.id.login_btn_login);
        login.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                XMPPConnection connection = new XMPPConnection("192.168.11.168");
                try {
                    connection.connect();
                    connection.login(username.getText().toString(), password.getText().toString());
                    Presence presence = new Presence(Presence.Type.available);
                    connection.sendPacket(presence);
                    setConnection(connection);
                    multiUserChat = new MultiUserChat(connection, "r157@conference.trial");
                    multiUserChat.join(username.getText().toString());
                    chatmanager = connection.getChatManager();
                    chatPage();
                } catch (XMPPException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void chatPage() {
        setContentView(R.layout.chat);
        chat = (TextView) this.findViewById(R.id.chat);
        setListAdapter();
    }

    public void getOffLineMsg(XMPPConnection connection) {
        OfflineMessageManager offlineManager = new OfflineMessageManager(connection);
        try {
            Iterator<org.jivesoftware.smack.packet.Message> it = offlineManager.getMessages();
            if (0 == offlineManager.getMessageCount()) {
                Log.i(TAG, "������Ϣ����: " + offlineManager.getMessageCount());
                return;
            }
            Map<String, ArrayList<Message>> offlineMsgs = new HashMap<String, ArrayList<Message>>();
            while (it.hasNext()) {
                org.jivesoftware.smack.packet.Message message = it.next();
                Log.i(TAG, "�յ�������Ϣ, Received from ��" + message.getFrom() + "�� message: " + message.getBody());
                String fromUser = message.getFrom().split("/")[0];
                showNotification(R.drawable.icon, "�¤�����å��`��", "From:" + fromUser, message.getBody(), 1);
                if (offlineMsgs.containsKey(fromUser)) {
                    offlineMsgs.get(fromUser).add(message);
                } else {
                    ArrayList<Message> temp = new ArrayList<Message>();
                    temp.add(message);
                    offlineMsgs.put(fromUser, temp);
                }
            }
            offlineManager.deleteMessages();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setConnection(XMPPConnection connection) {
        if (connection != null) {
            PacketFilter filter1 = new MessageTypeFilter(Message.Type.fromString("192.168.11.168"));
            PacketFilter filter = new MessageTypeFilter(Message.Type.groupchat);
            PacketFilter filter2 = new MessageTypeFilter(Message.Type.chat);
            connection.addPacketListener(new PacketListener() {

                public void processPacket(Packet packet) {
                    Message message = (Message) packet;
                    if (message.getBody() != null) {
                        String fromName = StringUtils.parseBareAddress(message.getFrom());
                        Log.i("XMPPClient", "Got text [" + message.getBody() + "] from [" + fromName + "]");
                        showNotification(R.drawable.message, "�¤�����å��`��", "From:" + fromName, message.getBody() + "�����ꤪ����ޤ�", R.raw.message);
                    }
                }
            }, filter);
            connection.addPacketListener(new PacketListener() {

                public void processPacket(Packet packet) {
                    Message message = (Message) packet;
                    if (message.getBody() != null) {
                        String fromName = StringUtils.parseBareAddress(message.getFrom());
                        Log.i("XMPPClient", "Got text [" + message.getBody() + "] from [" + fromName + "]");
                        showNotification(R.drawable.message, "�¤�����å��`��", "From:" + fromName, message.getBody() + "�����ꤪ����ޤ�", R.raw.message);
                    }
                }
            }, filter1);
            connection.addPacketListener(new PacketListener() {

                public void processPacket(Packet packet) {
                    Message message = (Message) packet;
                    if (message.getBody() != null) {
                        fromName = StringUtils.parseBareAddress(message.getFrom());
                        Log.i("XMPPClient", "Got text [" + message.getBody() + "] from [" + fromName + "]");
                        messages += fromName + ":";
                        messages += message.getBody() + "\n";
                        mHandler.post(new Runnable() {

                            public void run() {
                                setListAdapter();
                            }
                        });
                    }
                }
            }, filter2);
        }
    }

    public void showNotification(int icon, String tickertext, String title, String content, int sound) {
    }

    private void setListAdapter() {
        if (!"".equals(fromName)) {
            newChat = chatmanager.createChat(fromName, new MessageListener() {

                public void processMessage(Chat chat, Message message) {
                }
            });
            try {
                newChat.sendMessage("Hello!");
            } catch (XMPPException e) {
                System.out.println("Error Delivering block");
            }
        }
        chat.setText(messages);
    }
}
