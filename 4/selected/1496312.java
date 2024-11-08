package paolomind.test.notunit;

import java.awt.Frame;
import java.io.IOException;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;
import paolomind.multitalk.irc.IrcListener;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.FlowLayout;
import java.awt.Button;
import java.awt.TextArea;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.BorderLayout;

public final class IrcFrameTest extends Frame implements IrcListener {

    /** */
    private static final long serialVersionUID = 1L;

    /** */
    private PircBot irc;

    /** */
    private TextField textField = null;

    /** */
    private TextField textField1 = null;

    /** */
    private String target;

    /** */
    private TextArea textArea = null;

    /** */
    private TextField textField2 = null;

    /** */
    private final int n01 = 32767;

    /** */
    private final int n02 = 50;

    /** */
    private final int n03 = 204;

    /** */
    private final int n04 = 4;

    /** */
    private final int n05 = 374;

    /** */
    private final int n06 = 42;

    /** */
    private final int n07 = 206;

    /** */
    private final int n08 = 48;

    /** */
    private final int n09 = 372;

    /** */
    private final int n10 = 23;

    /** */
    private final int n11 = 8;

    /** */
    private final int n12 = 92;

    /** */
    private final int n13 = 740;

    /** */
    private final int n14 = 370;

    /** */
    private final int n15 = 200;

    /** */
    private final int n16 = 462;

    /** */
    private final int n17 = 366;

    /** */
    private final int n18 = 25;

    /** */
    private final int n19 = 198;

    /** */
    private final int n20 = 506;

    /** */
    private final int n21 = 40;

    /** */
    private final int n22 = 32;

    /** */
    private final int n23 = 540;

    /** */
    private final int n24 = 750;

    public String getTarget() {
        return this.target;
    }

    public void infoMessage(String channel, String sender, String login, String hostname, String message) {
        textArea.append("incoming message");
        textArea.append("\nchannel:" + channel);
        textArea.append("\nsender: " + sender);
        textArea.append("\nsender: " + login);
        textArea.append("\nsender: " + hostname);
        textArea.append("\n\n");
    }

    public void messageUnparsed(String message) {
        textArea.append("message:" + message);
        textArea.append("\n\n");
    }

    /**
     * This is the default constructor
     */
    public IrcFrameTest() {
        super();
        initialize();
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        this.setLayout(null);
        this.setSize(n24, n23);
        this.setTitle("Frame");
        this.add(getPanel(), null);
        this.add(getPanel1(), null);
        this.add(getPanel2(), null);
        this.add(getPanel3(), null);
        this.add(getPanel4(), null);
    }

    /**
     * This method initializes panel
     * 
     * @return java.awt.Panel
     */
    private Panel getPanel() {
        Panel panel = new Panel();
        panel.setLayout(new FlowLayout());
        panel.setMaximumSize(new Dimension(n01, n02));
        panel.setBounds(new Rectangle(n03, n04, n05, n06));
        panel.add(getTextField(), null);
        panel.add(getConnectButton(), null);
        return panel;
    }

    /**
     * This method initializes panel1
     * 
     * @return java.awt.Panel
     */
    private Panel getPanel1() {
        Panel panel1 = new Panel();
        panel1.setLayout(new FlowLayout());
        panel1.setMaximumSize(new Dimension(n01, n02));
        panel1.setBounds(new Rectangle(n07, n08, n09, n21));
        panel1.add(getTextField1(), null);
        panel1.add(getJoinButton(), null);
        return panel1;
    }

    /**
     * This method initializes panel2
     * 
     * @return java.awt.Panel
     */
    private Panel getPanel2() {
        Panel panel2 = new Panel();
        panel2.setBackground(Color.lightGray);
        panel2.setBounds(new Rectangle(n11, n12, n13, n14));
        panel2.setLayout(new BorderLayout());
        panel2.add(getTextArea(), BorderLayout.NORTH);
        return panel2;
    }

    /**
     * This method initializes textField
     * 
     * @return java.awt.TextField
     */
    private TextField getTextField() {
        if (textField == null) {
            textField = new TextField();
            textField.setColumns(n18);
        }
        return textField;
    }

    /**
     * This method initializes button
     * 
     * @return java.awt.Button
     */
    private Button getConnectButton() {
        Button button = new Button();
        button.setLabel("connect");
        button.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent e) {
                try {
                    irc.connect(textField.getText());
                } catch (NickAlreadyInUseException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (IrcException e1) {
                    e1.printStackTrace();
                }
            }
        });
        return button;
    }

    /**
     * This method initializes textField1
     * 
     * @return java.awt.TextField
     */
    private TextField getTextField1() {
        if (textField1 == null) {
            textField1 = new TextField();
            textField1.setColumns(n18);
        }
        return textField1;
    }

    /**
     * This method initializes button1
     * 
     * @return java.awt.Button
     */
    private Button getJoinButton() {
        Button button1 = new Button();
        button1.setLabel("join");
        button1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent e) {
                irc.joinChannel(textField1.getText());
                target = textField1.getText();
            }
        });
        return button1;
    }

    /**
     * This method initializes panel3
     * 
     * @return java.awt.Panel
     */
    private Panel getPanel3() {
        Panel panel3 = new Panel();
        panel3.setLayout(new FlowLayout());
        panel3.setMaximumSize(new Dimension(n01, n02));
        panel3.setBounds(new Rectangle(n15, n16, n17, n06));
        panel3.add(getTextField2(), null);
        panel3.add(getMessageButton(), null);
        return panel3;
    }

    /**
     * This method initializes textArea
     * 
     * @return java.awt.TextArea
     */
    private TextArea getTextArea() {
        if (textArea == null) {
            textArea = new TextArea();
            textArea.setEditable(true);
            textArea.setRows(n10);
            textArea.setColumns(n18);
        }
        return textArea;
    }

    /**
     * This method initializes textField2
     * 
     * @return java.awt.TextField
     */
    private TextField getTextField2() {
        if (textField2 == null) {
            textField2 = new TextField();
            textField2.setColumns(n18);
        }
        return textField2;
    }

    /**
     * This method initializes button2
     * 
     * @return java.awt.Button
     */
    private Button getMessageButton() {
        Button button2 = new Button();
        button2.setLabel("send");
        button2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent e) {
                irc.sendMessage(target, textField2.getText());
            }
        });
        return button2;
    }

    /**
     * This method initializes panel4
     * 
     * @return java.awt.Panel
     */
    private Panel getPanel4() {
        Panel panel4 = new Panel();
        panel4.setLayout(new FlowLayout());
        panel4.setMaximumSize(new Dimension(n01, n02));
        panel4.setBounds(new Rectangle(n19, n20, n14, n22));
        panel4.add(getChannelsButton(), null);
        panel4.add(getUsersButton(), null);
        return panel4;
    }

    /**
     * This method initializes button3
     * 
     * @return java.awt.Button
     */
    private Button getChannelsButton() {
        Button button3 = new Button();
        button3.setLabel("channel list");
        button3.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(final java.awt.event.ActionEvent e) {
                irc.listChannels();
            }
        });
        return button3;
    }

    /**
     * This method initializes button4
     * 
     * @return java.awt.Button
     */
    private Button getUsersButton() {
        Button button4 = new Button();
        button4.setLabel("user list");
        button4.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(final java.awt.event.ActionEvent e) {
                showUser(irc.getUsers(target));
            }
        });
        return button4;
    }

    private void showUser(User[] users) {
        for (int i = 0; i < users.length; i++) {
            textArea.append("#user#\n");
            textArea.append(users[i].getNick());
            textArea.append("\n");
            textArea.append(users[i].getPrefix());
            textArea.append("\n" + users[i].isOp());
            textArea.append("\n" + users[i].hasVoice());
            textArea.append("\n");
        }
        textArea.append("\n");
    }

    public void showChannel(String channel, int userCount, String topic) {
        textArea.append("#channel#\n");
        textArea.append(channel);
        textArea.append("\n" + userCount);
        textArea.append("\n");
        textArea.append(topic);
        textArea.append("\n\n");
    }

    public void setIrc(PircBot irc) {
        this.irc = irc;
    }

    public void connectionEstablished() {
        textArea.append("#connection established#\n\n");
    }

    public void onUnknown(String line) {
        textArea.append("#unknown message#\n");
        textArea.append(line);
        textArea.append("\n\n");
    }

    public void serverPing(String response) {
        textArea.append("#serverPing#\n");
        textArea.append(response);
        textArea.append("\n\n");
    }

    public void serverResponse(int code, String response) {
        textArea.append("#serverResponse#\n");
        textArea.append(response);
        textArea.append("\n" + code);
        textArea.append("\n\n");
    }
}
