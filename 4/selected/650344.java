package com.scottandjoe.texasholdem.gui;

import com.scottandjoe.texasholdem.cards.Card;
import com.scottandjoe.texasholdem.cards.CardType;
import com.scottandjoe.texasholdem.cards.CardUtils;
import com.scottandjoe.texasholdem.gameplay.Player;
import com.scottandjoe.texasholdem.misc.KillableThread;
import com.scottandjoe.texasholdem.networking.EncryptedMessageReader;
import com.scottandjoe.texasholdem.networking.EncryptedMessageWriter;
import com.scottandjoe.texasholdem.networking.Message;
import com.scottandjoe.texasholdem.misc.Settings;
import com.scottandjoe.texasholdem.networking.EMSCorruptedException;
import com.scottandjoe.texasholdem.networking.EMSException;
import com.scottandjoe.texasholdem.networking.EMSExceptionHandler;
import com.scottandjoe.texasholdem.networking.NetworkUtilities;
import com.scottandjoe.texasholdem.resources.Utilities;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import org.netbeans.lib.awtextra.AbsoluteConstraints;

/**
 *
 * @author Scott DellaTorre
 * @author Joe Stein
 * safe
 */
class GameWindow extends JFrame implements EMSExceptionHandler {

    private static final int EXPECTED = 0;

    private static final int UNEXPECTED = 1;

    private ArrayList<JButton> actionBtns = new ArrayList<JButton>();

    private boolean disconnected = false;

    private ArrayList<JLabel> holeCardLabels = new ArrayList<JLabel>();

    private int rangeMax = 0;

    private int rangeMin = 0;

    private EncryptedMessageReader reader;

    private ServerReaderThread serverReaderThread;

    private Settings settings;

    private GameWindowSwingSafe swingSafe = new GameWindowSwingSafe(this);

    private PokerTablePanel tablePanel;

    private EncryptedMessageWriter writer;

    /** Creates new form GameWindow */
    GameWindow(Settings settings, Player[] players, EncryptedMessageReader reader, EncryptedMessageWriter writer) {
        this.writer = writer;
        writer.setExceptionHandler(this);
        this.reader = reader;
        initComponents();
        initPokerTablePanel();
        addWindowListener(new GameWindowListener());
        getRootPane().setDefaultButton(sendBtn);
        this.settings = settings;
        if (settings.isObserving()) {
            holeCardLabel1.setVisible(false);
            holeCardLabel2.setVisible(false);
        }
        for (Player player : players) {
            tablePanel.addPlayer(player);
        }
        actionBtns.add(actionBtn1);
        actionBtns.add(actionBtn2);
        actionBtns.add(actionBtn3);
        swingSafe.clearBtns();
        holeCardLabels.add(holeCardLabel1);
        holeCardLabels.add(holeCardLabel2);
        swingSafe.clearCards();
        handType.setText("");
        potLabel.setText("...");
        statusLabel.setText("Waiting...");
        try {
            serverReaderThread = new ServerReaderThread();
            serverReaderThread.start();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "There was a problem connecting to the server.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        setVisible(true);
    }

    void bet(int bet) {
        relayInput("2," + bet);
    }

    private void confirmWindowClose() {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to quit?", "Leaving Game", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            setVisible(false);
            if (!disconnected) {
                disconnected = true;
                try {
                    NetworkUtilities.sendMessageAndWait(writer, new Message(Message.Type.LEFT_GAME, settings.getUserName()), false);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
                serverReaderThread.kill();
                tablePanel.kill();
            }
            dispose();
        }
    }

    public synchronized void handleException(EMSException emse) {
        if (!disconnected) {
            Utilities.log(Utilities.LOG_OUTPUT, "Server disconnected unexpectedly.");
            swingSafe.serverDcxn(UNEXPECTED);
        }
    }

    private void initPokerTablePanel() {
        tablePanel = new PokerTablePanel();
        tablePanel.setBorder(BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        tablePanel.setFocusable(false);
        GroupLayout tablePanelLayout = new GroupLayout(tablePanel);
        tablePanel.setLayout(tablePanelLayout);
        tablePanelLayout.setHorizontalGroup(tablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 840, Short.MAX_VALUE));
        tablePanelLayout.setVerticalGroup(tablePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 420, Short.MAX_VALUE));
        getContentPane().add(tablePanel, new AbsoluteConstraints(10, 10, 840, 420));
    }

    private void relayInput(String input) {
        swingSafe.clearBtns();
        NetworkUtilities.sendMessage(writer, new Message(Message.Type.DELIVER_INPUT, settings.getUserName(), input), false);
    }

    private class GameWindowListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (disconnected) {
                setVisible(false);
                dispose();
            } else {
                confirmWindowClose();
            }
        }
    }

    private class GameWindowSwingSafe extends SwingSafe {

        private GameWindowSwingSafe(GameWindow parent) {
            super(parent);
        }

        private void addHoleCard(final Card card) {
            invokeLater(new Runnable() {

                public void run() {
                    for (JLabel l : holeCardLabels) {
                        if ((l.getToolTipText() == null || l.getToolTipText().equals(""))) {
                            l.setToolTipText(card.toString());
                            l.setIcon(card.getImage());
                            break;
                        }
                    }
                }
            });
        }

        private void appendToChat(final String text) {
            invokeLater(new Runnable() {

                public void run() {
                    chatArea.setText(chatArea.getText() + text + "\n");
                    chatArea.setCaretPosition(chatArea.getText().length());
                }
            });
        }

        private void clearBtns() {
            invokeLater(new Runnable() {

                public void run() {
                    for (JButton btn : actionBtns) {
                        btn.setText(" ");
                        btn.setEnabled(false);
                    }
                }
            });
        }

        private void clearCards() {
            final ImageIcon faceDownCard = CardUtils.getFaceDownCard();
            invokeLater(new Runnable() {

                public void run() {
                    for (JLabel l : holeCardLabels) {
                        l.setIcon(faceDownCard);
                        l.setToolTipText("");
                    }
                }
            });
            tablePanel.clearCommunityCards();
        }

        private void serverDcxn(int dcxnType) {
            disconnected = true;
            serverReaderThread.kill();
            tablePanel.kill();
            clearBtns();
            clearCards();
            if (dcxnType == EXPECTED) {
                showMessageDialog("Host closed room.", "Game Over", JOptionPane.ERROR_MESSAGE);
                setStatus("Room closed.");
            } else if (dcxnType == UNEXPECTED) {
                showMessageDialog("Unexpectedly disconnected from server.", "Error", JOptionPane.ERROR_MESSAGE);
                setStatus("Disconnected.");
            }
            invokeLater(new Runnable() {

                public void run() {
                    chatArea.setEnabled(false);
                    chatField.setEnabled(false);
                    sendBtn.setEnabled(false);
                }
            });
        }

        private void setHandTypeText(final String text) {
            invokeLater(new Runnable() {

                public void run() {
                    handType.setText(text);
                }
            });
        }

        private void setPotLabelText(final String text) {
            invokeLater(new Runnable() {

                public void run() {
                    potLabel.setText(text);
                }
            });
        }

        private void setStatus(final String text) {
            invokeLater(new Runnable() {

                public void run() {
                    statusLabel.setText(text);
                }
            });
        }

        private void updateBtns(final String[] stats) {
            invokeLater(new Runnable() {

                public void run() {
                    for (int i = 0; i < 3; i++) {
                        actionBtns.get(i).setText(stats[i]);
                        if (stats[i].equals("") || stats[i].equals(null) || stats[i] == null) {
                            actionBtns.get(i).setEnabled(false);
                        } else {
                            actionBtns.get(i).setEnabled(true);
                        }
                    }
                }
            });
        }
    }

    private class ServerReaderThread extends KillableThread {

        Message mes = null;

        private ServerReaderThread() {
            super("Game Window Server Reader Thread");
        }

        public void doRun() {
            try {
                mes = reader.readMessage();
                Utilities.logParcial(Utilities.LOG_OUTPUT, "Message received from server: ");
                if (mes.getType() == Message.Type.CHAT) {
                    swingSafe.appendToChat(mes.getName() + " (" + Utilities.getCurrentTimeStamp() + "): " + ((String) mes.getContent()).replace("\n", "\n  "));
                    Utilities.log(Utilities.LOG_OUTPUT, "Chat message received");
                } else if (mes.getType() == Message.Type.INDICATE_DEALER) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Dealer updated");
                    tablePanel.setDealer((String) mes.getContent());
                } else if (mes.getType() == Message.Type.INDICATE_PLAYERS_REMOVED) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Member " + (String) mes.getContent() + " was removed.");
                    tablePanel.removePlayer((String) mes.getContent());
                    if (settings.getUserName().equals((String) mes.getContent())) {
                        swingSafe.showMessageDialog("You have been removed from the game.", "Finished Game", JOptionPane.INFORMATION_MESSAGE);
                    }
                } else if (mes.getType() == Message.Type.INDICATE_POT) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Pot value updated");
                    swingSafe.setPotLabelText((String) mes.getContent());
                } else if (mes.getType() == Message.Type.INDICATE_WINNERS) {
                    Utilities.log(Utilities.LOG_OUTPUT, (String) mes.getContent());
                    swingSafe.clearCards();
                } else if (mes.getType() == Message.Type.INFORMATION) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Info message - " + (String) mes.getContent());
                    String info = (String) mes.getContent();
                    String[] infoMsgs = info.split("/");
                    if (infoMsgs[0].equals("CHIPS")) {
                        tablePanel.setChips(infoMsgs[1], Integer.parseInt(infoMsgs[2]));
                    } else if (infoMsgs[0].equals("BREAK")) {
                        swingSafe.setStatus("Players are now on break.  The game will resume in " + infoMsgs[1] + " minutes.");
                    } else if (infoMsgs[0].equals("TURN")) {
                        tablePanel.setTurn(infoMsgs[1]);
                    }
                } else if (mes.getType() == Message.Type.LEFT_GAME) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Member " + mes.getName() + " left game");
                    tablePanel.removePlayer(mes.getName());
                } else if (mes.getType() == Message.Type.NEW_CARDS) {
                    Utilities.log(Utilities.LOG_OUTPUT, "New cards");
                    Serializable[] content = (Serializable[]) mes.getContent();
                    Card[] cards = (Card[]) content[0];
                    for (Card card : cards) {
                        if (card.getType() == CardType.COMMUNITY_CARD) {
                            tablePanel.addCommunityCard(card);
                        } else if (card.getType() == CardType.HOLE_CARD) {
                            swingSafe.addHoleCard(card);
                        }
                    }
                    swingSafe.setHandTypeText((String) content[1]);
                } else if (mes.getType() == Message.Type.OPTIONS) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Options delivered");
                    String[] content = (String[]) mes.getContent();
                    String[] labels = new String[] { content[0], content[1], content[2] };
                    swingSafe.updateBtns(labels);
                    if (content[3] != null) {
                        String[] range = content[3].split("-");
                        rangeMin = Integer.valueOf(range[0]);
                        rangeMax = Integer.valueOf(range[1]);
                    } else {
                        rangeMin = 0;
                        rangeMax = 0;
                    }
                } else if (mes.getType() == Message.Type.PROBABILITY_UPDATE) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Probability update - " + mes.getName() + ": " + mes.getContent());
                    tablePanel.setProbability(mes.getName(), (Double) mes.getContent());
                } else if (mes.getType() == Message.Type.ROOM_CLOSED) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Room closed.");
                    swingSafe.serverDcxn(EXPECTED);
                } else if (mes.getType() == Message.Type.STATISTICS_UPDATE) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Status update - " + (String) mes.getContent());
                    swingSafe.setStatus((String) mes.getContent());
                } else if (mes.getType() == Message.Type.VERIFY_FOLD) {
                    Utilities.log(Utilities.LOG_OUTPUT, "Verify fold");
                    int result = swingSafe.showConfirmDialog("Are you sure you want to fold?", "Fold", JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.YES_OPTION) {
                        relayInput("y");
                    } else {
                        relayInput("n");
                    }
                } else {
                    Utilities.log(Utilities.LOG_OUTPUT, mes);
                }
            } catch (EMSCorruptedException emsce) {
                if (!isDying()) {
                    handleException(emsce);
                }
            }
        }

        public void postDeath() {
        }

        public void preDeath() {
            try {
                reader.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        public void preRun() {
        }
    }

    /** This method is called from within the constructor to
    * initialize the form.
    * WARNING: Do NOT modify this code. The content of this method is
    * always regenerated by the Form Editor.
    */
    @SuppressWarnings("unchecked")
    private void initComponents() {
        jPanel3 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        statusLabel = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        holeCardLabel1 = new javax.swing.JLabel();
        holeCardLabel2 = new javax.swing.JLabel();
        handType = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        potLabel = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        chatArea = new javax.swing.JTextArea();
        chatField = new javax.swing.JTextField();
        sendBtn = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        actionBtn1 = new javax.swing.JButton();
        actionBtn2 = new javax.swing.JButton();
        actionBtn3 = new javax.swing.JButton();
        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        jPanel3.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        jLabel3.setFont(new java.awt.Font("DejaVu Sans", 1, 18));
        jLabel3.setText("Status");
        statusLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        statusLabel.setText("statusLabel");
        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel3Layout.createSequentialGroup().addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel3Layout.createSequentialGroup().addContainerGap().addComponent(statusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 452, Short.MAX_VALUE)).addGroup(jPanel3Layout.createSequentialGroup().addGap(211, 211, 211).addComponent(jLabel3))).addContainerGap()));
        jPanel3Layout.setVerticalGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup().addComponent(jLabel3).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE).addComponent(statusLabel).addContainerGap()));
        getContentPane().add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 440, 480, 50));
        jPanel5.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        jLabel1.setFont(new java.awt.Font("DejaVu Sans", 1, 18));
        jLabel1.setText("Hand");
        holeCardLabel1.setText("holeCardLabel[1]");
        holeCardLabel1.setMaximumSize(new java.awt.Dimension(71, 96));
        holeCardLabel1.setMinimumSize(new java.awt.Dimension(71, 96));
        holeCardLabel1.setPreferredSize(new java.awt.Dimension(71, 96));
        holeCardLabel2.setText("holeCardLabel[2]");
        holeCardLabel2.setMaximumSize(new java.awt.Dimension(71, 96));
        holeCardLabel2.setMinimumSize(new java.awt.Dimension(71, 96));
        holeCardLabel2.setPreferredSize(new java.awt.Dimension(71, 96));
        handType.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        handType.setText("handType");
        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel5Layout.createSequentialGroup().addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false).addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel5Layout.createSequentialGroup().addContainerGap().addComponent(handType, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel5Layout.createSequentialGroup().addGap(65, 65, 65).addComponent(jLabel1)).addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel5Layout.createSequentialGroup().addContainerGap().addComponent(holeCardLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(holeCardLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        jPanel5Layout.setVerticalGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel5Layout.createSequentialGroup().addContainerGap().addComponent(jLabel1).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(holeCardLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addComponent(holeCardLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(handType).addContainerGap(34, Short.MAX_VALUE)));
        getContentPane().add(jPanel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(500, 440, 180, 200));
        jPanel6.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        jLabel6.setFont(new java.awt.Font("DejaVu Sans", 1, 18));
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("Pot");
        potLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        potLabel.setText("potLabel");
        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel6Layout.createSequentialGroup().addContainerGap().addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, 132, Short.MAX_VALUE).addComponent(potLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 132, Short.MAX_VALUE)).addContainerGap()));
        jPanel6Layout.setVerticalGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel6Layout.createSequentialGroup().addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(potLabel).addContainerGap(14, Short.MAX_VALUE)));
        getContentPane().add(jPanel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(690, 580, 160, 60));
        chatArea.setColumns(20);
        chatArea.setEditable(false);
        chatArea.setFont(new java.awt.Font("SansSerif", 0, 11));
        chatArea.setLineWrap(true);
        chatArea.setRows(5);
        chatArea.setWrapStyleWord(true);
        jScrollPane2.setViewportView(chatArea);
        getContentPane().add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 500, 480, 110));
        chatField.setDocument(new LimitedDocument(100));
        chatField.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                chatFieldKeyPressed(evt);
            }

            public void keyReleased(java.awt.event.KeyEvent evt) {
                chatFieldKeyReleased(evt);
            }
        });
        getContentPane().add(chatField, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 620, 400, 20));
        sendBtn.setText("Send");
        sendBtn.setEnabled(false);
        sendBtn.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendBtnActionPerformed(evt);
            }
        });
        getContentPane().add(sendBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 620, 70, 20));
        jPanel4.setBorder(javax.swing.BorderFactory.createEtchedBorder(javax.swing.border.EtchedBorder.RAISED));
        jLabel4.setFont(new java.awt.Font("DejaVu Sans", 1, 18));
        jLabel4.setText("Actions");
        actionBtn1.setText("actionBtn1");
        actionBtn1.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                actionBtn1ActionPerformed(evt);
            }
        });
        actionBtn2.setText("actionBtn2");
        actionBtn2.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                actionBtn2ActionPerformed(evt);
            }
        });
        actionBtn3.setText("actionBtn3");
        actionBtn3.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                actionBtn3ActionPerformed(evt);
            }
        });
        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel4Layout.createSequentialGroup().addContainerGap().addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel4Layout.createSequentialGroup().addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(actionBtn1, javax.swing.GroupLayout.DEFAULT_SIZE, 132, Short.MAX_VALUE).addComponent(actionBtn2, javax.swing.GroupLayout.DEFAULT_SIZE, 132, Short.MAX_VALUE).addComponent(actionBtn3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 132, Short.MAX_VALUE)).addContainerGap()).addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup().addComponent(jLabel4).addGap(41, 41, 41)))));
        jPanel4Layout.setVerticalGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGroup(jPanel4Layout.createSequentialGroup().addComponent(jLabel4).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(actionBtn1).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(actionBtn2).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(actionBtn3).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
        getContentPane().add(jPanel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(690, 440, 160, 130));
        pack();
    }

    private void actionBtn1ActionPerformed(java.awt.event.ActionEvent evt) {
        relayInput("1");
    }

    private void actionBtn2ActionPerformed(java.awt.event.ActionEvent evt) {
        if (rangeMin != 0 && rangeMax != 0) {
            if (actionBtn2.getText().equals("Raise")) {
                new BetDialog(this, rangeMin, rangeMax, BetDialog.RAISE);
            } else {
                new BetDialog(this, rangeMin, rangeMax, BetDialog.BET);
            }
        } else {
            relayInput("2");
        }
    }

    private void actionBtn3ActionPerformed(java.awt.event.ActionEvent evt) {
        relayInput("3");
    }

    private void chatFieldKeyPressed(java.awt.event.KeyEvent evt) {
        if (chatField.getText().equals("") && evt.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            evt.consume();
        }
    }

    private void chatFieldKeyReleased(java.awt.event.KeyEvent evt) {
        if (chatField.getText().equals("")) {
            sendBtn.setEnabled(false);
        } else {
            sendBtn.setEnabled(true);
        }
    }

    private void sendBtnActionPerformed(java.awt.event.ActionEvent evt) {
        NetworkUtilities.sendMessage(writer, new Message(Message.Type.CHAT, settings.getUserName(), chatField.getText()), false);
        chatField.setText("");
        sendBtn.setEnabled(false);
    }

    private javax.swing.JButton actionBtn1;

    private javax.swing.JButton actionBtn2;

    private javax.swing.JButton actionBtn3;

    private javax.swing.JTextArea chatArea;

    private javax.swing.JTextField chatField;

    private javax.swing.JLabel handType;

    private javax.swing.JLabel holeCardLabel1;

    private javax.swing.JLabel holeCardLabel2;

    private javax.swing.JLabel jLabel1;

    private javax.swing.JLabel jLabel3;

    private javax.swing.JLabel jLabel4;

    private javax.swing.JLabel jLabel6;

    private javax.swing.JPanel jPanel3;

    private javax.swing.JPanel jPanel4;

    private javax.swing.JPanel jPanel5;

    private javax.swing.JPanel jPanel6;

    private javax.swing.JScrollPane jScrollPane2;

    private javax.swing.JLabel potLabel;

    private javax.swing.JButton sendBtn;

    private javax.swing.JLabel statusLabel;
}
