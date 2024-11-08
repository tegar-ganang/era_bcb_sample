package kr.pe.javarss.manager;

import kr.pe.javarss.util.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import com.rosaloves.bitlyj.Bitly;
import com.rosaloves.bitlyj.Bitly.Provider;
import de.nava.informa.core.ItemIF;

public class TwitterPublisher {

    private static Log logger = LogFactory.getLog(TwitterPublisher.class);

    private Twitter twitter;

    private Provider bitly;

    private String consumerKey;

    private String consumerSecret;

    private String token;

    private String tokenSecret;

    private String bitlyUsername;

    private String bitlyKey;

    public void setBitlyUsername(String bitlyUsername) {
        this.bitlyUsername = bitlyUsername;
    }

    public void setBitlyKey(String bitlyKey) {
        this.bitlyKey = bitlyKey;
    }

    public void setConsumerKey(String consumerKey) {
        this.consumerKey = consumerKey;
    }

    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setTokenSecret(String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }

    public void init() {
        twitter = new TwitterFactory().getInstance();
        twitter.setOAuthConsumer(consumerKey, consumerSecret);
        twitter.setOAuthAccessToken(new AccessToken(token, tokenSecret));
        bitly = Bitly.as(bitlyUsername, bitlyKey);
        if (logger.isInfoEnabled()) {
            logger.info("TweeterPublisher init : " + twitter + " : " + bitly);
        }
    }

    /**
     * 타임라인에 아이템 정보를 트윗한다.
     *
     * @param newItem
     */
    public synchronized void publishItem(ItemIF item) {
        if (CommonUtils.isRecentItem(item, 30) == false) {
            return;
        }
        String url = bitly.call(Bitly.shorten(item.getLink().toString())).getShortUrl();
        String message = item.getTitle() + " [" + item.getChannel().getTitle() + "]\n" + url + "  #javarss";
        publish(message);
    }

    public long publish(String message) {
        if (message == null) return -1;
        if (message.length() > 140) return -1;
        Status status = null;
        try {
            status = twitter.updateStatus(message);
            if (logger.isInfoEnabled()) {
                logger.info("A new tweet posted to Twitter(@javarss) : " + status.getId());
            }
            return status.getId();
        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Twitter posting error : [" + message + "] => " + e.getMessage());
            }
        }
        return -1;
    }
}
