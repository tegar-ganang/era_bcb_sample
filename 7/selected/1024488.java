package com.eirikb.gwt.chat.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 *
 * @author eirikb
 */
public class Chat implements EntryPoint {

    private String nick;

    private Label label;

    private Label errorLabel;

    private TextBox nickField;

    private Button button;

    private TextArea chatArea;

    private TextBox chatBox;

    private ListBox nickList;

    private long hash;

    private String[] chat;

    private RequestBuilder rb;

    private final int REFRESHRATE = 2;

    private final int CHATLENGTH = 30;

    private final String HOST = "http://localhost:8000";

    public enum Command {

        LOGIN, READ, SAY, NOVALUE;

        public static Command toCommand(String str) {
            try {
                return valueOf(str);
            } catch (Exception e) {
                return NOVALUE;
            }
        }
    }

    public Chat() {
    }

    public void onModuleLoad() {
        rb = new RequestBuilder(RequestBuilder.POST, HOST);
        chat = new String[CHATLENGTH];
        for (int i = 0; i < chat.length; i++) {
            chat[i] = "";
        }
        label = new Label("Enter your nick:");
        nickField = new TextBox();
        nickField.setMaxLength(8);
        button = new Button("Submit");
        errorLabel = new Label("");
        button.addClickListener(new ClickListener() {

            public void onClick(Widget w) {
                nick = nickField.getText();
                nick = nick.replaceAll(" ", "");
                if (nick.length() > 0) {
                    nick = nickField.getText();
                    send("LOGIN", null);
                } else {
                    errorLabel.setText("Please enter a nick!");
                }
            }
        });
        chatArea = new TextArea();
        chatBox = new TextBox();
        nickList = new ListBox();
        nickList.setVisibleItemCount(15);
        chatArea.setSize("640px", "480px");
        chatArea.setReadOnly(true);
        chatBox.setWidth("640px");
        chatBox.addKeyboardListener(new KeyboardListener() {

            public void onKeyDown(Widget arg0, char arg1, int arg2) {
            }

            public void onKeyPress(Widget arg0, char arg1, int arg2) {
                if (arg1 == 13) {
                    appendText(nick + ": " + chatBox.getText());
                    JSONObject o = new JSONObject();
                    o.put("TEXT", new JSONString(chatBox.getText()));
                    chatBox.setText("");
                    send("SAY", o);
                }
            }

            public void onKeyUp(Widget arg0, char arg1, int arg2) {
            }
        });
        RootPanel.get().add(label);
        RootPanel.get().add(nickField);
        RootPanel.get().add(button);
        RootPanel.get().add(errorLabel);
    }

    private void appendText(String text) {
        String all = "";
        for (int i = 0; i < chat.length - 1; i++) {
            chat[i] = chat[i + 1];
            all += chat[i] + "\n";
        }
        chat[chat.length - 1] = text;
        all += text + "\n";
        chatArea.setText(all);
    }

    private void command(JSONValue value) throws Exception {
        JSONObject json = value.isObject();
        switch(Command.toCommand(json.get("CMD").isString().stringValue())) {
            case LOGIN:
                nickList.addItem(nick);
                hash = (long) json.get("HASH").isNumber().doubleValue();
                JSONArray array = json.get("NICKS").isArray();
                for (int i = 0; i < array.size(); i++) {
                    nickList.addItem(array.get(i).isString().stringValue());
                }
                while (RootPanel.get().getWidgetCount() > 0) {
                    RootPanel.get().remove(0);
                }
                appendText("Welcome to GWT-Chat");
                HorizontalPanel hPanel = new HorizontalPanel();
                VerticalPanel vPanel = new VerticalPanel();
                vPanel.add(chatArea);
                vPanel.add(chatBox);
                hPanel.add(vPanel);
                hPanel.add(nickList);
                RootPanel.get().add(hPanel);
                loop();
                break;
            case READ:
                if (json.get("MSG") != null) {
                    array = json.get("MSG").isArray();
                    for (int i = 0; i < array.size(); i++) {
                        JSONObject o = array.get(i).isObject();
                        appendText(o.get("FROM").isString().stringValue() + ": " + o.get("MSG").isString().stringValue());
                    }
                }
                if (json.get("JOINS") != null) {
                    array = json.get("JOINS").isArray();
                    for (int i = 0; i < array.size(); i++) {
                        String nickAdd = array.get(i).isString().stringValue();
                        nickList.addItem(nickAdd);
                        appendText("  >  " + nickAdd + " Joined");
                    }
                }
                if (json.get("LEAVES") != null) {
                    array = json.get("LEAVES").isArray();
                    for (int i = 0; i < array.size(); i++) {
                        int j = 0;
                        String nickRem = array.get(i).isString().stringValue();
                        for (j = 1; j < nickList.getItemCount(); j++) {
                            if (nickList.getItemText(j).equals(nickRem)) {
                                break;
                            }
                        }
                        nickList.removeItem(j);
                        appendText("  <   " + nickRem + " Left");
                    }
                }
                break;
            case NOVALUE:
                break;
        }
    }

    public void send(String cmd, JSONObject object) {
        try {
            JSONObject json = new JSONObject();
            json.put("CMD", new JSONString(cmd));
            if (nick != null) {
                json.put("NICK", new JSONString(nick));
            }
            if (hash > 0) {
                json.put("HASH", new JSONNumber(hash));
            }
            if (object != null) {
                json.put("BODY", object);
            }
            rb.sendRequest(json.toString(), new RequestCallback() {

                public void onResponseReceived(Request arg0, Response arg1) {
                    if (arg1.getStatusCode() == 200) {
                        try {
                            command(JSONParser.parse(arg1.getText()));
                        } catch (Exception e) {
                            appendText(e.toString());
                        }
                    } else if (arg1.getStatusCode() == 403) {
                        errorLabel.setText("Username is already in use");
                    }
                }

                public void onError(Request arg0, Throwable arg1) {
                }
            });
        } catch (Throwable e) {
            Window.alert("ERROR ON READ! " + e);
        }
    }

    private void loop() {
        Timer t = new Timer() {

            public void run() {
                send("READ", null);
            }
        };
        t.scheduleRepeating(REFRESHRATE * 1000);
    }
}
