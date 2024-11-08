package net.sourceforge.recman.backend.parser;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FileUtils;
import net.sourceforge.recman.backend.Config;
import net.sourceforge.recman.backend.manager.pojo.Channel;
import net.sourceforge.recman.backend.parser.exception.VDRParserException;

/**
 * ChannelParserImpl
 * 
 * @author Marcus Kessel
 * 
 */
public class ChannelParserImpl implements ChannelParser {

    private Map<String, Channel> channelMap = new HashMap<String, Channel>();

    private File channels;

    /**
     * Constructor
     */
    public ChannelParserImpl() {
        try {
            this.channels = Config.get().getChannelsConf();
        } catch (IOException e) {
            throw new IllegalArgumentException("channels.conf does not exist! " + channels, e);
        }
    }

    @SuppressWarnings("unchecked")
    public Channel resolveChannel(String channelId) throws VDRParserException {
        if (this.channelMap.containsKey(channelId)) {
            return this.channelMap.get(channelId);
        }
        List<String> lines = Collections.emptyList();
        try {
            lines = FileUtils.readLines(this.channels);
        } catch (IOException e) {
            throw new VDRParserException("Problem parsing channels.conf", e);
        }
        String matches = null;
        String[] chanIds = channelId.split("-");
        Channel channel = new Channel();
        if (chanIds.length == 1) {
            matches = this.getChannelLine(lines, Integer.parseInt(chanIds[0]));
        } else {
            String source = chanIds[0];
            String nid = chanIds[1];
            String tid = chanIds[2];
            String sid = chanIds[3];
            String rid = null;
            if (chanIds.length > 4) {
                rid = chanIds[4];
                channel.setRid(rid);
            }
            matches = this.getChannelLine(lines, source, nid, tid, sid, rid);
            channel.setNid(nid);
            channel.setSid(sid);
            channel.setTid(tid);
            channel.setSource(source);
        }
        if (matches == null) {
            throw new VDRParserException("Cannot find channel entry: " + channelId);
        }
        String channelName = matches.split(":")[0];
        if (channelName.contains(";")) {
            channelName = channelName.split(";")[0];
        }
        if (channelName == null) {
            throw new VDRParserException("Cannot find channel entry: " + channelId);
        }
        channel.setName(channelName);
        this.channelMap.put(channelId, channel);
        return channel;
    }

    /**
     * Return the channel name given by line number. Ignore lines starting with
     * ":->"
     * 
     * @param lines
     *            List<String> of channels.conf lines
     * @param lineNumber
     *            Number of line to return
     * @return Channel line
     */
    private String getChannelLine(List<String> lines, int lineNumber) {
        int i = 0;
        for (String line : lines) {
            if (!line.startsWith(":->")) {
                i++;
                if (lineNumber == i) {
                    return line;
                }
            }
        }
        return null;
    }

    /**
     * Get channel line from channels.conf
     * 
     * @param lines
     * @param source
     * @param nid
     * @param tid
     * @param sid
     * @param rid
     * @return
     */
    private String getChannelLine(List<String> lines, String source, String nid, String tid, String sid, String rid) {
        String regex = "^.*" + source + ".*" + sid + ".*" + nid + ".*" + tid + ".*" + (rid != null ? rid + ".*" : "");
        Pattern p = Pattern.compile(regex);
        String matches = null;
        for (String line : lines) {
            Matcher m = p.matcher(line);
            if (m.find()) {
                matches = m.group();
                break;
            }
        }
        return matches;
    }
}
