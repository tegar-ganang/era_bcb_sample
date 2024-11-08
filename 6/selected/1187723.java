package com.clanwts.bncs.chat;

import com.clanwts.bncs.client.BattleNetChatClient;
import com.clanwts.bncs.client.BattleNetChatClientFactory;
import com.clanwts.bncs.client.SimpleBattleNetChatClientListener;
import com.clanwts.bncs.codec.standard.messages.Platform;
import com.clanwts.bncs.codec.standard.messages.Product;
import com.clanwts.bncs.codec.standard.messages.ProductType;
import edu.cmu.ece.agora.futures.FutureListener;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 *
 * @author Administrator
 */
public class BNCSChatApplet extends javax.swing.JApplet {

    private static BattleNetChatClientFactory createFactory() {
        BattleNetChatClientFactory fact = new BattleNetChatClientFactory();
        fact.setPlatform(Platform.X86);
        fact.setProduct(Product.W3TFT_1_24C);
        fact.setKeys("ABCDEFGHIJKLMNOPQRSTUVWXYZ", "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        fact.setKeyOwner("Richard Milhous Nixon");
        return fact;
    }

    private DefaultListModel channel_list_model = new DefaultListModel();

    private StyledDocument chat_log_doc;

    private Style chat_default_style;

    private Style chat_info_style;

    private Style chat_error_style;

    private Style chat_name_style;

    private Style chat_channel_text_style;

    private Style chat_whisper_text_style;

    private BattleNetChatClient chat_client;

    /** Initializes the applet BNCSChatApplet */
    public void init() {
        try {
            java.awt.EventQueue.invokeAndWait(new Runnable() {

                public void run() {
                    initComponents();
                    setupChatLog();
                    setupChatClient();
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setupChatLog() {
        chat_log_doc = chat_log.getStyledDocument();
        chat_default_style = chat_log_doc.addStyle(null, null);
        this.chat_channel_text_style = chat_log_doc.addStyle(null, chat_default_style);
        StyleConstants.setForeground(chat_channel_text_style, Color.WHITE);
        this.chat_error_style = chat_log_doc.addStyle(null, chat_default_style);
        StyleConstants.setForeground(chat_error_style, Color.RED);
        this.chat_info_style = chat_log_doc.addStyle(null, chat_default_style);
        StyleConstants.setForeground(chat_info_style, Color.CYAN);
        this.chat_name_style = chat_log_doc.addStyle(null, chat_default_style);
        StyleConstants.setForeground(chat_name_style, Color.YELLOW);
        this.chat_whisper_text_style = chat_log_doc.addStyle(null, chat_default_style);
        StyleConstants.setForeground(chat_whisper_text_style, Color.GREEN);
    }

    private void setupChatClient() {
        BattleNetChatClientFactory fact = createFactory();
        chat_client = fact.createClient();
        chat_client.addListener(new ChatClientListener());
        chat_client.connect(new InetSocketAddress("clanwts.com", 6112)).addListener(new FutureListener<Void>() {

            @Override
            public void onCancellation(Throwable cause) {
                cause.printStackTrace();
                System.exit(-1);
            }

            @Override
            public void onCompletion(Void result) {
                chat_client.login("Nixon'sBitch", "dijkstra213", true).addListener(new FutureListener<Void>() {

                    @Override
                    public void onCancellation(Throwable cause) {
                        cause.printStackTrace();
                        System.exit(-1);
                    }

                    @Override
                    public void onCompletion(Void result) {
                        chat_client.join("W3").addListener(new FutureListener<Void>() {

                            @Override
                            public void onCancellation(Throwable cause) {
                                cause.printStackTrace();
                                System.exit(-1);
                            }

                            @Override
                            public void onCompletion(Void result) {
                                java.awt.EventQueue.invokeLater(new Runnable() {

                                    @Override
                                    public void run() {
                                        channel_name.setText("W3");
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    private class ChatClientListener extends SimpleBattleNetChatClientListener {

        @Override
        public void forcedDisconnect() {
            java.awt.EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    channel_list_model.clear();
                    channel_name.setText("(disconnected)");
                }
            });
        }

        @Override
        public void forcedJoin(final String channel) {
            java.awt.EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    channel_list_model.clear();
                    channel_name.setText(channel);
                }
            });
        }

        @Override
        public void otherJoined(final String user, final ProductType type) {
            java.awt.EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    channel_list_model.addElement(user);
                }
            });
        }

        @Override
        public void otherParted(final String user) {
            java.awt.EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    channel_list_model.removeElement(user);
                }
            });
        }

        @Override
        public void otherShown(final String user, final ProductType type) {
            java.awt.EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    if (!user.equals("Nixon'sBitch")) {
                        channel_list_model.addElement(user);
                    }
                }
            });
        }

        @Override
        public void channelChatReceived(final String user, final String message) {
            java.awt.EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    try {
                        chat_log_doc.insertString(chat_log_doc.getLength(), user + ":", chat_name_style);
                        chat_log_doc.insertString(chat_log_doc.getLength(), " ", chat_default_style);
                        chat_log_doc.insertString(chat_log_doc.getLength(), message, chat_channel_text_style);
                        chat_log_doc.insertString(chat_log_doc.getLength(), "\n", chat_default_style);
                    } catch (BadLocationException ex) {
                        Logger.getLogger(BNCSChatApplet.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        }

        @Override
        public void errorMessageReceived(final String msg) {
            java.awt.EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    try {
                        chat_log_doc.insertString(chat_log_doc.getLength(), msg, chat_error_style);
                        chat_log_doc.insertString(chat_log_doc.getLength(), "\n", chat_default_style);
                    } catch (BadLocationException ex) {
                        Logger.getLogger(BNCSChatApplet.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        }

        @Override
        public void infoMessageReceived(final String msg) {
            java.awt.EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    try {
                        chat_log_doc.insertString(chat_log_doc.getLength(), msg, chat_info_style);
                        chat_log_doc.insertString(chat_log_doc.getLength(), "\n", chat_default_style);
                    } catch (BadLocationException ex) {
                        Logger.getLogger(BNCSChatApplet.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        }

        @Override
        public void privateChatReceived(final String user, final String message) {
            java.awt.EventQueue.invokeLater(new Runnable() {

                @Override
                public void run() {
                    try {
                        chat_log_doc.insertString(chat_log_doc.getLength(), user + " whispers:", chat_name_style);
                        chat_log_doc.insertString(chat_log_doc.getLength(), " ", chat_default_style);
                        chat_log_doc.insertString(chat_log_doc.getLength(), message, chat_whisper_text_style);
                        chat_log_doc.insertString(chat_log_doc.getLength(), "\n", chat_default_style);
                    } catch (BadLocationException ex) {
                        Logger.getLogger(BNCSChatApplet.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        }
    }

    /** This method is called from within the init() method to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        jScrollPane1 = new javax.swing.JScrollPane();
        chat_log = new javax.swing.JTextPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        channel_list = new javax.swing.JList();
        chat_input = new javax.swing.JTextField();
        chat_send = new javax.swing.JButton();
        channel_name = new javax.swing.JLabel();
        setBackground(java.awt.Color.lightGray);
        setForeground(java.awt.Color.white);
        chat_log.setBackground(java.awt.Color.darkGray);
        chat_log.setEditable(false);
        chat_log.setForeground(new java.awt.Color(255, 255, 255));
        jScrollPane1.setViewportView(chat_log);
        channel_list.setBackground(java.awt.Color.darkGray);
        channel_list.setForeground(java.awt.Color.white);
        channel_list.setModel(channel_list_model);
        channel_list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(channel_list);
        chat_input.setBackground(java.awt.Color.darkGray);
        chat_input.setForeground(java.awt.Color.white);
        chat_input.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                chat_inputKeyPressed(evt);
            }
        });
        chat_send.setText("Send");
        chat_send.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chat_sendMouseClicked(evt);
            }
        });
        channel_name.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        channel_name.setText("Channel Name");
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(chat_input, javax.swing.GroupLayout.DEFAULT_SIZE, 408, Short.MAX_VALUE).addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 408, Short.MAX_VALUE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 105, Short.MAX_VALUE).addComponent(channel_name, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 105, Short.MAX_VALUE).addComponent(chat_send, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 105, Short.MAX_VALUE)).addContainerGap()));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 248, Short.MAX_VALUE).addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup().addComponent(channel_name).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 228, Short.MAX_VALUE))).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(chat_send, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(chat_input, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)).addContainerGap()));
    }

    private void chat_inputKeyPressed(java.awt.event.KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            sendChat_EQ();
        }
    }

    private void chat_sendMouseClicked(java.awt.event.MouseEvent evt) {
        sendChat_EQ();
    }

    private void sendChat_EQ() {
        String message = chat_input.getText().trim();
        chat_client.sendChannelChat(message);
        chat_input.setText("");
        if (!message.startsWith("/")) {
            try {
                chat_log_doc.insertString(chat_log_doc.getLength(), "Nixon'sBitch" + ":", chat_name_style);
                chat_log_doc.insertString(chat_log_doc.getLength(), " ", chat_default_style);
                chat_log_doc.insertString(chat_log_doc.getLength(), message, chat_channel_text_style);
                chat_log_doc.insertString(chat_log_doc.getLength(), "\n", chat_default_style);
            } catch (BadLocationException ex) {
                Logger.getLogger(BNCSChatApplet.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private javax.swing.JList channel_list;

    private javax.swing.JLabel channel_name;

    private javax.swing.JTextField chat_input;

    private javax.swing.JTextPane chat_log;

    private javax.swing.JButton chat_send;

    private javax.swing.JScrollPane jScrollPane1;

    private javax.swing.JScrollPane jScrollPane2;
}
