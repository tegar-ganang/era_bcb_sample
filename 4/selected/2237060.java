package logicswarm.net.irc.mods;

import java.util.List;
import logicswarm.net.irc.IrcBot;
import logicswarm.net.irc.IrcModule;
import winterwell.jtwitter.*;
import org.jibble.jmegahal.JMegaHal;

public class Twittable extends IrcModule {

    public static final long serialVersionUID = 1;

    public String username = "";

    public String password = "";

    private List<Twitter.Status> tweets;

    private List<Twitter.Status> tweetCache;

    public int maxTweets = 2;

    Twitter objTwitter;

    public Twittable(String user, String pass, IrcBot owner) {
        super(owner);
        username = user;
        password = pass;
        initialize("Twittable");
    }

    public void onInitialize() {
        objTwitter = new Twitter(username, password);
        this.start();
    }

    public void run() {
        while (true) {
            if (isModLoaded()) {
                try {
                    tweets = objTwitter.getFriendsTimeline();
                    log("got the tweets for " + username + "!");
                } catch (Exception e) {
                    log("could not get tweets: " + e.getMessage());
                }
                sayTweets();
                try {
                    Thread.sleep(30000);
                } catch (Exception e) {
                    log("Error: " + e.getMessage());
                }
            }
        }
    }

    private void sayTweets() {
        if (tweetCache == null) {
            tweetCache = tweets;
            return;
        }
        int chanTweets = 0;
        for (int i = 0; i < tweets.size(); i++) {
            if (!tweets.get(i).user.screenName.equalsIgnoreCase(username) && !tweetIdExists(tweets.get(i).id)) {
                if (tweets.get(i).text.contains("@" + username)) {
                    String myTweet = "";
                    JMH_Brain temp = (JMH_Brain) parent.getModule("Brain");
                    if (temp != null) {
                        do {
                            myTweet = temp.getSentence();
                        } while (myTweet.length() > 150);
                        objTwitter.updateStatus("@" + tweets.get(i).user.screenName + " " + myTweet);
                    }
                } else if (chanTweets < maxTweets) {
                    String[][] tmp = getChannels();
                    for (int j = 0; j < tmp.length; j++) {
                        log(tweets.get(i).user.screenName + " says '" + tweets.get(i).text + "'");
                        parent.SendMessage(tmp[j][0], "" + tweets.get(i).user.screenName + " says '" + tweets.get(i).text + "'");
                    }
                    chanTweets++;
                }
            } else {
                break;
            }
        }
        tweetCache = tweets;
    }

    private boolean tweetIdExists(long id) {
        for (int i = 0; i < tweetCache.size(); i++) {
            if (tweetCache.get(i).id == id) {
                return true;
            }
        }
        return false;
    }
}
