package com.tysanclan.site.projectewok.ws.mumble;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jeroen
 */
public class MMOMumbleServerStatusTest {

    private static final String SECRET = "c7bd82f1e56b048be64682837e350c72b2eab1d0";

    private static final String TOKEN = "5bec609d8cc41b6fba79e115b0c691548245645f";

    private static Logger log = LoggerFactory.getLogger(MMOMumbleServerStatusTest.class);

    @Test
    @Ignore
    public void testServerOverview() {
        log.info("Test server list");
        List<Server> servers = MMOMumbleServerStatus.getServers(TOKEN, SECRET);
        assertFalse(servers.isEmpty());
        for (Server s : servers) {
            logServerInfo(s);
        }
    }

    @Test
    @Ignore
    public void testSpecificServerOverview() {
        log.info("Test specific servers");
        long[] ids = { 190L, 191L };
        for (long id : ids) {
            logServerInfo(MMOMumbleServerStatus.getServer(id, TOKEN, SECRET));
        }
    }

    @Test
    @Ignore
    public void testChannelsAndUsers() {
        ServerStatus status = MMOMumbleServerStatus.getServerStatus(190L, TOKEN, SECRET);
        for (Channel channel : status.getChannels()) {
            int parent = channel.getParent();
            if (parent == -1) {
                log.info("" + channel.getId() + ". " + channel.getName());
            } else {
                log.info("" + channel.getId() + ". " + channel.getName() + " [child of " + parent + "]");
            }
            log.info("  State: " + channel.getState());
            log.info("  Position: " + channel.getPosition());
        }
        for (MumbleUser user : status.getUsers()) {
            log.info("User " + user.getName() + " [in channel " + user.getChannel() + "]");
            log.info("  Seconds online: " + user.getSecondsOnline());
            log.info("  State: " + user.getState());
            log.info("  Deaf: " + user.isDeaf());
            log.info("  Mute: " + user.isMute());
        }
    }

    private void logServerInfo(Server s) {
        log.info("---=== " + s.getId() + ". " + s.getName() + " ===---");
        log.info("Host: " + s.getHost());
        log.info("MOTD: " + s.getMotd());
        log.info("Murmur ID: " + s.getMurmurId());
        log.info("Murmur Version: " + s.getMurmurVersion());
        log.info("Port: " + s.getPort());
        log.info("Slots: " + s.getSlots());
        log.info("Status: " + s.getStatus());
        log.info("Created at: " + s.getCreatedAt());
        log.info("Location Id: " + s.getLocation().getId());
        log.info("Location Name:: " + s.getLocation().getName());
        log.info("Updated at: " + s.getUpdatedAt());
    }
}
