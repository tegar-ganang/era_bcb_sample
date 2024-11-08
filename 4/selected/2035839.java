package net.sf.jerkbot.plugins.svn;

import jerklib.Channel;
import jerklib.Session;
import net.sf.jerkbot.bot.BotService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yves Zoundi <yveszoundi at users dot sf dot net>
 * @version 0.0.1
 *          The SVN poller task using SVNKIT library
 */
public class SVNPollerTask implements Runnable {

    private static final Logger Log = LoggerFactory.getLogger(SVNPollerTask.class.getName());

    private static final int MAX_MESSAGE_LEN = 50;

    private String svnURL;

    private long lastRev = -1;

    private String username = "";

    private String password = "";

    private BotService botService;

    /**
     * Create a new SVN polling task
     * @param botService The irc session holder(the bot)
     * @param svnURL The URL of the SVN repository
     * @param username The SVN username
     * @param password The SVN password
     */
    public SVNPollerTask(BotService botService, String svnURL, String username, String password) {
        this.botService = botService;
        this.svnURL = svnURL;
        this.username = username;
        this.password = password;
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p/>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    public void run() {
        Log.debug("Fetching SVN revisions");
        Session session = botService.getSession();
        if (session == null) {
            Log.warn("No IRC session");
        }
        final List<Channel> channels = session.getChannels();
        if (channels.isEmpty()) {
            Log.warn("No channel in the IRC session");
            return;
        }
        if (StringUtils.isEmpty(svnURL)) {
            Log.warn("No repository provided");
            return;
        }
        SVNRepository repo = null;
        try {
            Log.debug("Creating repository");
            repo = SVNRepositoryFactory.create(SVNURL.parseURIDecoded(svnURL));
            Log.debug("Login in");
            ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(username, password);
            repo.setAuthenticationManager(authManager);
            long currentRev = repo.getLatestRevision();
            Log.info("Revision:" + currentRev);
            if (lastRev == -1) {
                lastRev = currentRev;
            } else {
                if (lastRev >= currentRev) {
                    return;
                } else {
                    lastRev = currentRev;
                }
            }
            Log.debug("Fetching entries");
            @SuppressWarnings("unchecked") ArrayList<SVNLogEntry> entries = new ArrayList<SVNLogEntry>(repo.log(new String[] { "" }, null, currentRev, currentRev, true, true));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < entries.size(); ++i) {
                SVNLogEntry current = entries.get(i);
                String msg = current.getMessage();
                if (msg.length() > MAX_MESSAGE_LEN) {
                    msg = new String(msg.substring(0, MAX_MESSAGE_LEN) + " ...");
                }
                sb.append(current.getDate().toString()).append(", ");
                sb.append("Author:").append(current.getAuthor()).append(", r").append(current.getRevision()).append(" ").append(repo.getLocation().getPath()).append(" : ").append(msg).append(", Changed path(s):").append(current.getChangedPaths().size());
            }
            if (!entries.isEmpty()) {
                for (Channel channel : channels) {
                    channel.say(sb.toString());
                }
            }
        } catch (Exception e) {
            for (Channel channel : channels) {
                channel.say(e.getMessage());
            }
        }
    }
}
