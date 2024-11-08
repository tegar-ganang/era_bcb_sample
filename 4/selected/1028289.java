package jw.bznetwork.client.screens;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import jw.bznetwork.client.BZNetwork;
import jw.bznetwork.client.BoxCallback;
import jw.bznetwork.client.VerticalScreen;
import jw.bznetwork.client.data.model.IrcBot;
import jw.bznetwork.client.ui.Header2;
import jw.bznetwork.client.ui.Header3;
import jw.bznetwork.client.ui.Spacer;

public class IRCScreen extends VerticalScreen {

    @Override
    public void deselect() {
    }

    @Override
    public String getName() {
        return "irc";
    }

    @Override
    public String getTitle() {
        return "IRC";
    }

    @Override
    public void init() {
    }

    @Override
    public void reselect() {
        select();
    }

    @Override
    public void select() {
        BZNetwork.authLink.listIrcBots(new BoxCallback<IrcBot[]>() {

            @Override
            public void run(IrcBot[] result) {
                select1(result);
            }
        });
    }

    @SuppressWarnings("deprecation")
    protected void select1(IrcBot[] result) {
        widget.clear();
        widget.add(new Header2("IRC Bots"));
        FlexTable table = new FlexTable();
        FlexCellFormatter format = table.getFlexCellFormatter();
        int row = 0;
        for (final IrcBot bot : result) {
            Label label = new Label(bot.getNick() + "@" + bot.getServer() + ":" + bot.getPort() + "/" + bot.getChannel());
            label.setTitle("Id: " + bot.getBotid());
            table.setWidget(row, 0, label);
            Anchor editLink = new Anchor("edit");
            editLink.setTitle("Opens up a box that you can use to edit this IRC bot.");
            table.setWidget(row, 1, editLink);
            editLink.addClickListener(new ClickListener() {

                @Override
                public void onClick(Widget sender) {
                    showUpdateBotDialog(bot);
                }
            });
            Anchor deleteLink = new Anchor("delete");
            deleteLink.setTitle("Deletes this IRC bot. Any triggers that use this bot " + "as the recipient will be deleted, and the bot will be " + "disconnected from the IRC server that it is currently " + "connected to.");
            table.setWidget(row, 2, deleteLink);
            deleteLink.addClickListener(new ClickListener() {

                @Override
                public void onClick(Widget sender) {
                    deleteIrcBot(bot);
                }
            });
            row += 1;
        }
        widget.add(table);
        Button addButton = new Button("Add");
        widget.add(addButton);
        widget.add(new Spacer("8px", "3px"));
        addButton.setTitle("Opens a dialog where you can add a new IRC bot.");
        addButton.addClickListener(new ClickListener() {

            @Override
            public void onClick(Widget sender) {
                IrcBot newBot = new IrcBot();
                newBot.setBotid(-1);
                newBot.setChannel("");
                newBot.setNick("");
                newBot.setPassword("");
                newBot.setPort(0);
                newBot.setServer("");
                showUpdateBotDialog(newBot);
            }
        });
        Button resyncButton = new Button("<b>Re-sync IRC connections</b>");
        resyncButton.setTitle("Disconnects and then re-connects all IRC bots. This can be " + "useful when the connected IRC bots have somehow gotten out " + "of sync with the actual list of IRC bots.");
        widget.add(resyncButton);
        resyncButton.addClickListener(new ClickListener() {

            @Override
            public void onClick(Widget sender) {
                resyncIrc();
            }
        });
    }

    protected void showUpdateBotDialog(final IrcBot bot) {
        VerticalPanel panel = new VerticalPanel();
        final PopupPanel box = new PopupPanel(false, true);
        box.setWidget(panel);
        boolean isAdding = bot.getBotid() == -1;
        panel.add(new Header3(isAdding ? "Add a new IRC bot" : "Editing an IRC bot"));
        FlexTable table = new FlexTable();
        panel.add(table);
        FlexCellFormatter format = table.getFlexCellFormatter();
        final TextBox nickField = new TextBox();
        final TextBox serverField = new TextBox();
        final TextBox portField = new TextBox();
        final PasswordTextBox passwordField = new PasswordTextBox();
        final TextBox channelField = new TextBox();
        nickField.setText(bot.getNick());
        serverField.setText(bot.getServer());
        portField.setText("" + bot.getPort());
        passwordField.setText(bot.getPassword());
        channelField.setText(bot.getChannel());
        table.setText(0, 0, "Nick");
        table.setText(1, 0, "Server");
        table.setText(2, 0, "Port");
        table.setText(3, 0, "Password");
        table.setText(4, 0, "Channel");
        table.setWidget(0, 1, nickField);
        table.setWidget(1, 1, serverField);
        table.setWidget(2, 1, portField);
        table.setWidget(3, 1, passwordField);
        table.setWidget(4, 1, channelField);
        HorizontalPanel buttonPanel = new HorizontalPanel();
        Button saveButton = new Button(isAdding ? "Create" : "Save");
        Button cancelButton = new Button("Cancel");
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        table.setWidget(5, 1, buttonPanel);
        box.center();
        saveButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                int portNumber;
                try {
                    portNumber = Integer.parseInt(portField.getText());
                } catch (NumberFormatException e) {
                    Window.alert("That port is not a number.");
                    return;
                }
                box.hide();
                BZNetwork.authLink.updateIrcBot(bot.getBotid(), nickField.getText(), serverField.getText(), portNumber, passwordField.getText(), channelField.getText(), new BoxCallback<Void>() {

                    @Override
                    public void run(Void result) {
                        select();
                    }
                });
            }
        });
        cancelButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                box.hide();
            }
        });
    }

    protected void deleteIrcBot(IrcBot bot) {
        if (!Window.confirm("Are you sure you want to delete this IRC bot? This will " + "also delete any triggers that use it as a recipient.")) return;
        BZNetwork.authLink.deleteIrcBot(bot.getBotid(), new BoxCallback<Void>() {

            @Override
            public void run(Void result) {
                select();
            }
        });
    }

    public static void resyncIrc() {
        BZNetwork.authLink.reconnectIrcBots(new BoxCallback<Void>() {

            @Override
            public void run(Void result) {
                Window.alert("IRC connections have been successfully re-synchronized.");
            }
        });
    }
}
