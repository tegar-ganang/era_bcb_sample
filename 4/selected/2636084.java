package com.outlandr.irc.client.replies;

import java.util.Arrays;
import java.util.List;

public class RplNamReply implements RplServerReply {

    private List<String> names;

    private String channelName;

    public RplNamReply(String part1, String part2) {
        String[] temp = part1.split(" ");
        this.channelName = temp[4];
        String[] members = part2.split(" ");
        this.names = Arrays.asList(members);
    }

    public String[] getNames() {
        return names.toArray(new String[names.size()]);
    }

    public String getChannelName() {
        return channelName;
    }

    @Override
    public int getReplyId() {
        return RPL_NAMREPLY;
    }
}
