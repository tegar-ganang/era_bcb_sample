package com.googlecode.kanzaki.ui;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * This is class which should hold all channel-specific
 * view elements.
 * 
 * Disclaimer: This is fuzzy point of design
 * @author knick
 *
 */
public class ChannelViewContainer {

    private ChannelPane channelPane;

    private JTextField messageInputField;

    private JButton sendButton;

    private String channelName;

    private JPanel usersPanel;

    private JPanel topPanel;

    private JComboBox emotionComboBox;

    public JComboBox getEmotionComboBox() {
        return emotionComboBox;
    }

    public void setEmotionComboBox(JComboBox comboBox) {
        this.emotionComboBox = comboBox;
    }

    public JPanel getTopPanel() {
        return topPanel;
    }

    public void setTopPanel(JPanel topPanel) {
        this.topPanel = topPanel;
    }

    public ChannelViewContainer(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelName() {
        return channelName;
    }

    public ChannelPane getChannelPane() {
        return channelPane;
    }

    public void setChannelPane(ChannelPane channelPane) {
        this.channelPane = channelPane;
    }

    public JTextField getMessageInputField() {
        return messageInputField;
    }

    public void setMessageInputField(JTextField messageInputField) {
        this.messageInputField = messageInputField;
    }

    public JButton getSendButton() {
        return sendButton;
    }

    public void setSendButton(JButton sendButton) {
        this.sendButton = sendButton;
    }

    public JPanel getUsersPanel() {
        return usersPanel;
    }

    public void setUsersPanel(JPanel usersPanel) {
        this.usersPanel = usersPanel;
    }
}
