package fr.fg.server.core;

import java.util.HashSet;
import java.util.Set;
import fr.fg.client.data.ChatChannelData;
import fr.fg.client.data.ChatChannelsData;
import fr.fg.server.data.DataAccess;
import fr.fg.server.data.Player;
import fr.fg.server.util.JSONStringer;

public class ChatTools {

    public static JSONStringer getChannels(JSONStringer json, Player player) {
        if (json == null) json = new JSONStringer();
        player = DataAccess.getPlayerById(player.getId());
        json.object().key(ChatChannelsData.FIELD_CHANNELS).array();
        Set<String> channels = new HashSet<String>(ChatManager.getInstance().getPlayerChannels(player.getId()));
        for (String channel : channels) {
            json.object().key(ChatChannelData.FIELD_NAME).value(ChatManager.getInstance().getChannelName(channel)).key(ChatChannelData.FIELD_ACTIVE).value(ChatManager.getInstance().isChannelEnabled(player, channel)).endObject();
        }
        json.endArray().endObject();
        return json;
    }
}
