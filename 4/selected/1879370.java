package com.googlecode.kanzaki.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import com.googlecode.kanzaki.exception.BadTicketException;
import com.googlecode.kanzaki.exception.NetworkException;
import com.googlecode.kanzaki.exception.ProtocolException;
import com.googlecode.kanzaki.protocol.ConnectedChannelInterface;
import com.googlecode.kanzaki.protocol.Message;
import com.googlecode.kanzaki.ui.ChannelPane;

public class ChannelController {

    private ConnectedChannelInterface channel;

    private ChannelPane channelPane;

    private JTextField messageInputField;

    private JComboBox emotionComboBox;

    private boolean readerStarted;

    private SendMessageActionListener sendMessageActionListener;

    public class SendMessageActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            Message message = new Message();
            message.setBody(messageInputField.getText());
            if (emotionComboBox != null) {
                message.setEmotion(emotionComboBox.getSelectedItem().toString());
                emotionComboBox.setSelectedIndex(0);
            }
            try {
                channel.sendMessage(message);
            } catch (NetworkException e1) {
                e1.printStackTrace();
                System.exit(-1);
            } catch (ProtocolException e1) {
                e1.printStackTrace();
                System.exit(-1);
            } catch (BadTicketException e1) {
                e1.printStackTrace();
                System.exit(-1);
            }
            messageInputField.setText(null);
        }
    }

    /**
	 * Adds nick to recepient list.
	 * @param nick nick to be added
	 */
    public void addRecepient(String nick) {
        messageInputField.setText(nick + ": " + messageInputField.getText());
        messageInputField.requestFocus();
    }

    public ChannelController(ConnectedChannelInterface channelInterface) {
        this.channel = channelInterface;
        readerStarted = false;
        sendMessageActionListener = new SendMessageActionListener();
    }

    public void setChannelPane(ChannelPane channelPane) {
        this.channelPane = channelPane;
    }

    public void setMessageInputField(JTextField messageInputField) {
        this.messageInputField = messageInputField;
        messageInputField.addActionListener(sendMessageActionListener);
    }

    public void setEmotionComboBox(JComboBox comboBox) {
        this.emotionComboBox = comboBox;
        comboBox.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                if (messageInputField != null) {
                    messageInputField.requestFocus();
                }
            }
        });
    }

    public SendMessageActionListener getSendMessageActionListener() {
        return sendMessageActionListener;
    }

    public String getChannelName() {
        return channel.getChannelName();
    }

    public String getUsername() {
        return channel.getUsername();
    }

    /**
	 * Starts reading thread for current channel. If
	 * thread is already running, then nothing happens. 
	 */
    public void startMessageReadingThread() {
        if (readerStarted == false) {
            Thread reader = new Thread(new Runnable() {

                public void run() {
                    Message message = new Message();
                    while (true) {
                        try {
                            message = channel.getNextMessage();
                        } catch (NetworkException e) {
                            e.printStackTrace();
                            System.exit(-1);
                        } catch (ProtocolException e) {
                            e.printStackTrace();
                            System.exit(-1);
                        } catch (BadTicketException e) {
                            e.printStackTrace();
                            System.exit(-1);
                        }
                        channelPane.addMessage(message);
                    }
                }
            });
            reader.start();
            readerStarted = true;
        }
    }
}
