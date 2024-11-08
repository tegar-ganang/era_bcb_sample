package jgm.gui.tabs;

import jgm.glider.log.*;
import java.awt.*;
import javax.swing.*;

public class ChatTab extends Tab implements Clearable {

    private JTabbedPane tabs;

    public LogTab all;

    public LogTab pub;

    public LogTab whisper;

    public LogTab guild;

    public ChatTab(jgm.gui.GUI gui) {
        super(gui, new BorderLayout(), "Chat");
        tabs = new JTabbedPane();
        all = new LogTab(gui, "All Chat", tabs);
        pub = new LogTab(gui, "Public Chat", tabs);
        whisper = new LogTab(gui, "Whisper/Say/Yell", tabs);
        guild = new LogTab(gui, "Guild", tabs);
        addTab(all);
        addTab(pub);
        addTab(whisper);
        addTab(guild);
        add(tabs, BorderLayout.CENTER);
        validate();
    }

    private void addTab(Tab t) {
        tabs.addTab(t.name, t);
    }

    public void add(ChatLogEntry e) {
        all.add(e);
        String channel = e.getChannel();
        if (channel == null) return;
        if (channel.equals("Whisper") || channel.equals("Say") || channel.equals("Yell")) {
            whisper.add(e);
        } else if (channel.equals("Guild") || channel.equals("Officer")) {
            guild.add(e);
        } else if (e.getType().equals("Public Chat")) {
            pub.add(e);
        }
    }

    public void clear(boolean clearingAll) {
        if (!clearingAll) {
            ((Clearable) tabs.getSelectedComponent()).clear(false);
        } else {
            for (int i = 0; i < tabs.getComponentCount(); i++) {
                ((Clearable) tabs.getComponentAt(i)).clear(true);
            }
        }
    }
}
