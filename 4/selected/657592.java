package raptor.swt.chat.controller;

import org.apache.commons.lang.StringUtils;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import raptor.Quadrant;
import raptor.Raptor;
import raptor.action.RaptorAction.RaptorActionContainer;
import raptor.chat.ChatEvent;
import raptor.chat.ChatType;
import raptor.connector.Connector;
import raptor.swt.SWTUtils;
import raptor.swt.chat.ChatConsoleController;
import raptor.swt.chat.ChatUtils;

public class ChannelController extends ChatConsoleController {

    protected String channel;

    public ChannelController(Connector connector, String channel) {
        super(connector);
        this.channel = channel;
    }

    @Override
    public void dispose() {
        connector.setSpeakingChannelTells(channel, false);
        super.dispose();
    }

    public String getChannel() {
        return channel;
    }

    @Override
    public String getName() {
        return channel;
    }

    @Override
    public Quadrant getPreferredQuadrant() {
        return Raptor.getInstance().getPreferences().getQuadrant(getConnector().getShortName() + "-" + CHANNEL_TAB_QUADRANT);
    }

    @Override
    public String getPrependText(boolean checkButton) {
        if (isIgnoringActions()) {
            return "";
        }
        if (checkButton && isToolItemSelected(ToolBarItemKey.PREPEND_TEXT_BUTTON)) {
            return connector.getChannelTabPrefix(channel);
        } else if (!checkButton) {
            return connector.getChannelTabPrefix(channel);
        } else {
            return "";
        }
    }

    @Override
    public String getPrompt() {
        return connector.getPrompt();
    }

    @Override
    public Control getToolbar(Composite parent) {
        if (toolbar == null) {
            toolbar = SWTUtils.createToolbar(parent);
            ChatUtils.addActionsToToolbar(this, RaptorActionContainer.ChannelChatConsole, toolbar);
            adjustAwayButtonEnabled();
        } else {
            toolbar.setParent(parent);
        }
        return toolbar;
    }

    @Override
    public boolean isAcceptingChatEvent(ChatEvent inboundEvent) {
        return inboundEvent.getType() == ChatType.CHANNEL_TELL && StringUtils.equals(inboundEvent.getChannel(), channel) || inboundEvent.getType() == ChatType.OUTBOUND && inboundEvent.getMessage().startsWith(connector.getChannelTabPrefix(channel));
    }

    @Override
    public boolean isAwayable() {
        return false;
    }

    @Override
    public boolean isCloseable() {
        return true;
    }

    @Override
    public boolean isPrependable() {
        return true;
    }

    @Override
    public boolean isSearchable() {
        return true;
    }
}
