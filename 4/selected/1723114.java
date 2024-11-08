package kr.pe.javarss.manager;

import java.util.Iterator;
import java.util.Map;
import kr.pe.javarss.model.Feed;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ItemIF;
import de.nava.informa.utils.poller.Poller;
import de.nava.informa.utils.poller.PollerObserverIF;

/**
 * RSS URL을 폴러에 등록하고 주기적인 폴링작업을 통해
 * 최신의 RSS 내용을 수집한다.
 */
public class PollingManager {

    private static Log logger = LogFactory.getLog(PollingManager.class);

    public static final int ONE_MINUTE = 60 * 1000;

    static ThreadLocal<ChannelIF> context = new ThreadLocal<ChannelIF>();

    /**
     * 기본 폴링 주기(단위: 분)
     */
    private int defaultPollingPeriod;

    private Poller poller;

    private CacheManager cacheManager;

    private FacebookPublisher facebookPublisher;

    private TwitterPublisher twitterPublisher;

    public void setTwitterPublisher(TwitterPublisher twitterPublisher) {
        this.twitterPublisher = twitterPublisher;
    }

    public void setFacebookPublisher(FacebookPublisher facebookPublisher) {
        this.facebookPublisher = facebookPublisher;
    }

    public void setDefaultPollingPeriod(int period) {
        this.defaultPollingPeriod = period;
    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * 폴러 초기화
     */
    public void init() {
        poller = new Poller();
        poller.setPeriod(defaultPollingPeriod * ONE_MINUTE);
        poller.addObserver(new JavarssPollerObserver());
        if (logger.isInfoEnabled()) {
            logger.info("Init Success : default polling period = " + defaultPollingPeriod + " min.");
        }
    }

    public void destroy() {
        poller = null;
    }

    /**
     * 채널을 폴러에 등록
     * @param channel
     */
    public void registerChannel(ChannelIF channel) {
        poller.registerChannel(channel, defaultPollingPeriod * ONE_MINUTE, defaultPollingPeriod * ONE_MINUTE);
    }

    /**
     * 채널을 폴러에서 제거
     * @param channel
     */
    public void unregisterChannel(ChannelIF channel) {
        if (channel == null) return;
        poller.unregisterChannel(channel);
    }

    /**
     * @param channels Map<Feed, ChannelIF>
     */
    public void unregisterChannels(Map<Feed, ChannelIF> channels) {
        if (channels == null) return;
        for (ChannelIF channel : channels.values()) {
            unregisterChannel(channel);
        }
    }

    /**
     * 폴더 관찰자.
     */
    class JavarssPollerObserver implements PollerObserverIF {

        /**
         * 새 아이템 발견(기존 아이템 제목 수정 포함) 이벤트 발생시 자동 실행
         * Note : Informa는 기존 아이템이 삭제된 경우는 고려하지 않음.
         */
        public void itemFound(ItemIF newItem, ChannelIF existingChannel) {
            context.remove();
            context.set(newItem.getChannel());
            if (logger.isInfoEnabled()) {
                logger.info("New Item Found : " + existingChannel.getLocation());
            }
        }

        public void pollFinished(ChannelIF existingChannel) {
            ChannelIF newChannel = context.get();
            if (newChannel == null) return;
            for (Iterator<ItemIF> iter = existingChannel.getItems().iterator(); iter.hasNext(); ) {
                ItemIF item = iter.next();
                existingChannel.removeItem(item);
            }
            int i = 0;
            for (Iterator<ItemIF> iter = newChannel.getItems().iterator(); iter.hasNext(); i++) {
                ItemIF item = iter.next();
                existingChannel.addItem(item);
                if (i == 0 && twitterPublisher != null) {
                    twitterPublisher.publishItem(item);
                }
            }
            cacheManager.flushChannelCache(existingChannel);
            context.remove();
        }

        public void pollStarted(ChannelIF existingChannel) {
            context.remove();
            if (logger.isInfoEnabled()) {
                logger.info("Polling Start : " + existingChannel.getLocation());
            }
        }

        public void channelChanged(ChannelIF existingChannel) {
            cacheManager.flushChannelCache(existingChannel);
        }

        public void channelErrored(ChannelIF existingChannel, Exception e) {
            context.remove();
            if (logger.isWarnEnabled()) {
                logger.warn("Polling Error : " + existingChannel.getLocation() + " => " + e.getMessage());
            }
        }
    }
}
