package limaCity.App;

import java.util.ArrayList;
import limCity.App.R;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class Chat {

    Data data;

    XMPPConnection conn;

    MultiUserChat muc;

    ListView chatpagelayoutcontentoutput;

    EditText chatpagecontentinputtext;

    Button chatpagecontentinputbutton;

    ArrayList<Message> chatContentList;

    ChatListItemAdapter chatListItemAdapter;

    public Chat(Data pData) {
        data = pData;
    }

    public void init() {
        chatpagelayoutcontentoutput = (ListView) data.activity.findViewById(R.id.listViewChatPageLayoutContentOutput);
        chatpagecontentinputtext = (EditText) data.activity.findViewById(R.id.editTextChatPageContentInput);
        chatpagecontentinputbutton = (Button) data.activity.findViewById(R.id.buttonChatPageContentInput);
        chatpagecontentinputtext.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) {
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                if (arg0.length() == 0) {
                    chatpagecontentinputbutton.setEnabled(false);
                } else {
                    chatpagecontentinputbutton.setEnabled(true);
                }
            }
        });
        chatpagecontentinputbutton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                sendMessage(chatpagecontentinputtext.getText().toString());
                chatpagecontentinputtext.setText("");
            }
        });
        chatContentList = new ArrayList<Message>(0);
        chatListItemAdapter = new ChatListItemAdapter(data.context, chatContentList);
        chatpagelayoutcontentoutput.setAdapter(chatListItemAdapter);
        conn = new XMPPConnection("jabber.lima-city.de");
    }

    public void login() {
        try {
            conn.connect();
            conn.login(data.Username, data.Password, "Lima-City Android App");
            DiscussionHistory history = new DiscussionHistory();
            history.setMaxStanzas(5);
            muc = new MultiUserChat(conn, "support@conference.jabber.lima-city.de");
            muc.addMessageListener(new PacketListener() {

                @Override
                public void processPacket(Packet message) {
                    onMessageReceived(message);
                }
            });
            muc.join(data.Username, data.Password, history, SmackConfiguration.getPacketReplyTimeout());
        } catch (XMPPException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String content) {
        Message message = muc.createMessage();
        message.setBody(content);
        try {
            muc.sendMessage(message);
        } catch (XMPPException e) {
            e.printStackTrace();
        }
    }

    private void onMessageReceived(Packet content) {
        Message message = (Message) content;
        if (message.getFrom().matches(".+/{1}.+")) {
            chatContentList.add(message);
            chatListItemAdapter.notifyDataSetChanged();
            chatpagelayoutcontentoutput.postDelayed(new Runnable() {

                public void run() {
                    chatpagelayoutcontentoutput.forceLayout();
                    chatpagelayoutcontentoutput.postDelayed(new Runnable() {

                        public void run() {
                            chatpagelayoutcontentoutput.smoothScrollToPosition(chatListItemAdapter.getCount() - 1);
                        }
                    }, 100L);
                }
            }, 100L);
        }
    }
}
