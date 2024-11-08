package com.parasite;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import javacream.net.Channel;
import javacream.net.User;
import javacream.swing.Listable;

/**
 * Message
 *
 * @author Glenn Powell
 *
 */
public class Message implements Listable {

    private User user;

    private Channel channel;

    private String message;

    public Message(User user, Channel channel, String message) {
        this.user = user;
        this.channel = channel;
        this.message = message;
    }

    public User getUser() {
        return user;
    }

    public Channel getChannel() {
        return channel;
    }

    public String getMessage() {
        return message;
    }

    public Color getListableBackground(int row, int column, boolean isSelected, boolean hasFocus, boolean dropTarget, Object argument) {
        return null;
    }

    public Font getListableFont(int row, int column, boolean isSelected, boolean hasFocus, boolean dropTarget, Object argument) {
        return null;
    }

    public Color getListableForeground(int row, int column, boolean isSelected, boolean hasFocus, boolean dropTarget, Object argument) {
        return null;
    }

    public String getListableText(int row, int column, boolean isSelected, boolean hasFocus, boolean dropTarget, Object argument) {
        return user + ": " + message;
    }

    public boolean isListableEnabled(int row, int column, boolean isSelected, boolean hasFocus, boolean dropTarget, Object argument) {
        return true;
    }

    public boolean paintListableIcon(int row, int column, boolean isSelected, boolean hasFocus, boolean dropTarget, Graphics2D g, int width, int height, Object argument) {
        return false;
    }
}
