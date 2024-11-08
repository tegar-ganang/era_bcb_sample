package genericirc.irccore;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.*;
import java.awt.*;
import genericirc.JFrameUtils;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.net.UnknownHostException;
import javax.swing.text.DefaultCaret;

/**
 * IRCCoreTest is a basic test class to provide a simple GUI for irc communication.
 * This is intended to be a test bed for the various functions of IRCCore
 * @author Steve "Uru" West <uruwolf@users.sourceforge.net>
 * @version 2011-07-28
 */
public class IRCCoreTest {

    private JFrame window;

    private JButton send;

    private JTextField text;

    private JTextArea textArea;

    private IRCCore io;

    private CommandProcessor cp;

    private IRCCoreTest(String server, String user) {
        createWindow();
        try {
            io = new IRCCore(server, user, "genericCore", 6667);
            io.addIRCEventsListener(new IRCEventsAdaptor() {

                @Override
                public void newMessage(NewMessageEvent nme) {
                    putMessage(nme.getMessage());
                }

                @Override
                public void topicUpdated(TopicUpdatedEvent tue) {
                    System.out.println("New topic for: " + tue.getChannel() + " -> " + tue.getNewTopic());
                }

                @Override
                public void ping(PingEvent pe) {
                    io.sendRawIRC("PONG :" + pe.getQuery());
                }

                @Override
                public void userList(UserListEvent ule) {
                    System.out.println("New user list for:" + ule.getChannel() + " contains " + ule.getUserList().size() + " users");
                }
            });
        } catch (UnknownHostException uhe) {
            putMessage("Unable to reach server");
        }
        cp = new CommandProcessor("/", io);
    }

    private void createWindow() {
        JFrameUtils.loadDefaultLookAndFeel();
        window = new JFrame("genericIRC test window");
        window.setLayout(new BorderLayout());
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setIconImage(genericirc.Main.PROGRAM_ICON.getImage());
        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        JScrollPane scroll = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        window.add(scroll, BorderLayout.CENTER);
        JPanel outContainer = new JPanel();
        outContainer.setLayout(new BorderLayout());
        text = new JTextField();
        text.addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });
        outContainer.add(text, BorderLayout.CENTER);
        send = new JButton("Send");
        send.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        outContainer.add(send, BorderLayout.EAST);
        window.add(outContainer, BorderLayout.SOUTH);
        window.setSize(700, 500);
        window.setVisible(true);
    }

    private void sendMessage() {
        putMessage(text.getText());
        cp.process(text.getText());
        text.setText(null);
    }

    private void putMessage(String msg) {
        textArea.append(msg + "\n");
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("<server> <user>");
            System.exit(0);
        }
        new IRCCoreTest(args[0], args[1]);
    }
}
