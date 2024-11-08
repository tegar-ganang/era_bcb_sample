package de.cinek.rssview;

import javax.swing.JToolBar;

/**
	Toolbar in the main window (on the left-hand side under
	the channel tree component).
*/
public class RssToolBar extends JToolBar {

    public RssToolBar(RssView parent) {
        super("Channel list toolbar");
        initComponents(parent);
        setRollover(true);
    }

    private void initComponents(RssView parent) {
        add(new de.cinek.rssview.ui.JToolBarButton(parent.getNewChannelAction()));
        add(new de.cinek.rssview.ui.JToolBarButton(parent.getChannelUpAction()));
        add(new de.cinek.rssview.ui.JToolBarButton(parent.getChannelDownAction()));
    }
}
