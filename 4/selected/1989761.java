package be.lassi.ui.main;

import javax.swing.JComponent;
import be.lassi.context.ShowContext;
import be.lassi.ui.base.BasicFrame;

/**
 *
 *
 *
 */
public class ChannelsFrame extends BasicFrame {

    private ChannelsPanel panelChannels;

    public ChannelsFrame(final ShowContext context) {
        super(context, "Channels");
        init();
    }

    protected JComponent createPanel() {
        panelChannels = new ChannelsPanel(getShow().getChannels());
        return panelChannels;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isObsolete() {
        return true;
    }
}
