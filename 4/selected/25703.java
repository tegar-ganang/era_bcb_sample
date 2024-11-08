package wand.filterChannel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;
import wand.genericChannel.*;

public class FilterPanel extends JPanel {

    private static final int ENGINE_PANEL_WIDTH = 140;

    private static final int ENGINE_PANEL_HEIGHT = 700;

    public DisplayPanel channelDisplay;

    public DisplayPanel outputPreviewPanel;

    public DisplayPanel outputDisplayPanel;

    public FilterFader filterFader;

    public FilterKills filterKills;

    public ChannelTransitions channelTransitions;

    private int channelID;

    public ChannelBeat channelBeat;

    public Border subPanelBorder = BorderFactory.createLineBorder(Color.black, 1);

    public FilterPanel() {
        setLayout(new GridLayout(6, 1));
        setPreferredSize(new Dimension(ENGINE_PANEL_WIDTH, ENGINE_PANEL_HEIGHT));
        setBorder(BorderFactory.createLineBorder(Color.gray, 0));
        setBackground(Color.lightGray);
        channelDisplay = new DisplayPanel(false);
        outputPreviewPanel = new DisplayPanel(false);
        outputDisplayPanel = new DisplayPanel(true);
        filterFader = new FilterFader();
        filterKills = new FilterKills();
        channelTransitions = new ChannelTransitions();
        channelDisplay.setBorder(subPanelBorder);
        add(channelDisplay);
        add(filterFader);
        add(filterKills);
        add(channelTransitions);
        channelBeat = new ChannelBeat(channelID);
        this.setFocusable(true);
        channelDisplay.addMouseListener(new java.awt.event.MouseAdapter() {

            public void mouseClicked(java.awt.event.MouseEvent evt) {
                channelDisplayClicked(evt);
            }
        });
    }

    private void channelDisplayClicked(java.awt.event.MouseEvent evt) {
        showFilterChooser();
    }

    public void showFilterChooser() {
        wand.ChannelFrame.filterChooserFrame.setVisible(true);
    }

    public void setPoints() {
        channelDisplay.setPoints();
        outputPreviewPanel.setPoints();
        outputDisplayPanel.setPoints();
    }

    public void beatRefresh(int channelBeat) {
        channelDisplay.beatRefresh(channelBeat);
        outputPreviewPanel.beatRefresh(channelBeat);
        outputDisplayPanel.beatRefresh(channelBeat);
    }

    public ChannelBeat getChannelBeat() {
        return channelBeat;
    }

    public DisplayPanel getChannelDisplay() {
        return channelDisplay;
    }

    public DisplayPanel getOutputPreviewPanel() {
        return outputPreviewPanel;
    }

    public DisplayPanel getOutputDisplayPanel() {
        return outputDisplayPanel;
    }

    public void clockCheck() {
        filterKills.clockCheck();
    }
}
