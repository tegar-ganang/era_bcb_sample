package kr.pe.javarss;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;
import kr.pe.javarss.dao.FeedDao;
import kr.pe.javarss.manager.CacheManager;
import kr.pe.javarss.manager.ChannelManager;
import kr.pe.javarss.manager.PollingManager;
import kr.pe.javarss.model.Category;
import kr.pe.javarss.model.Feed;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import de.nava.informa.core.ChannelIF;

/**
 * RSS 컨텐츠 정보(즉, Channel)를 보관하는 클래스.
 *
 * Channel 객체는 RSS XML의 내용물을 담고있으며, 실제 Channel 객체를 이용하여
 * HTML을 렌더링하는 작업은 JSP에서 이루어진다.
 *
 * 모든 Channel 객체는 카테고리별로 분류되어 내부적으로 Map에 보관된다.
 *
 * ※ 레포지토리 구축 프로세스 : Feed 조회 => Channel 빌딩 => Polling 시작
 *
 * TODO internalMap에 대한 syncronization 고려
 */
public class ChannelRepository {

    private static Log logger = LogFactory.getLog(ChannelRepository.class);

    private FeedDao feedDao;

    private ChannelManager channelManager;

    private PollingManager pollingManager;

    private CacheManager cacheManager;

    /**
     * 모든 생성된 채널 객체들을 카테고리별로 저장하고 있는 내부 map.
     *
     * key : Category
     * value : TreeMap<Feed, ChannelIF>
     */
    private SortedMap<Category, SortedMap<Feed, ChannelIF>> categoryMap = new TreeMap<Category, SortedMap<Feed, ChannelIF>>();

    public void setChannelManager(ChannelManager channelManager) {
        this.channelManager = channelManager;
    }

    public void setPollingManager(PollingManager pollingManager) {
        this.pollingManager = pollingManager;
    }

    public void setFeedDao(FeedDao feedManager) {
        this.feedDao = feedManager;
    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public ChannelRepository() {
    }

    /**
     * 레포지토리 구축(모든 카테고리 포함)
     */
    public void init() {
        Collection<Category> categories = feedDao.getCategories(true);
        for (Category category : categories) {
            addCategory(category);
        }
        if (logger.isInfoEnabled()) {
            logger.info("모든 카테고리에 대한 레포지토리 구축 완료.");
        }
    }

    /**
     * 레포지토리 소멸
     */
    public void destroy() {
        for (Category category : getCategories()) {
            removeCategory(category);
        }
        categoryMap.clear();
        if (logger.isInfoEnabled()) {
            logger.info("모든 카테고리에 대한 레포지토리 비움 완료.");
        }
    }

    /**
     * 레포지토리에서 주어진 카테고리의 피드-채널 맵 리턴.
     *
     * @param category
     * @return 순서를 유지하는 TreeMap<Feed, ChannelIF>
     */
    public SortedMap<Feed, ChannelIF> getChannelMap(Category category) {
        return categoryMap.get(category);
    }

    /**
     * 현재 레포지토리에 포함된 카테고리 목록
     * @return Collection<Category>
     */
    public Collection<Category> getCategories() {
        return categoryMap.keySet();
    }

    public Category getDefaultCategory() {
        return (Category) categoryMap.firstKey();
    }

    public SortedMap<Feed, ChannelIF> getDefaultChannelMap() {
        return getChannelMap(getDefaultCategory());
    }

    public void addChannel(Feed newFeed) {
        if (hasNotAvailableCategory(newFeed)) return;
        ChannelIF newChannel = channelManager.buildChannel(newFeed);
        if (newChannel == null) return;
        SortedMap<Feed, ChannelIF> channelMap = getChannelMap(newFeed.getCategory());
        channelMap.put(newFeed, newChannel);
        pollingManager.registerChannel(newChannel);
        if (logger.isInfoEnabled()) {
            logger.info("레포지토리에 [" + newFeed.getXmlUrl() + "] 채널 추가.");
        }
    }

    public void removeChannel(Feed oldFeed) {
        if (hasNotAvailableCategory(oldFeed)) return;
        SortedMap<Feed, ChannelIF> channelMap = getChannelMap(oldFeed.getCategory());
        ChannelIF oldChannel = (ChannelIF) channelMap.get(oldFeed);
        channelMap.remove(oldFeed);
        cacheManager.removeChannelCache(oldChannel);
        pollingManager.unregisterChannel(oldChannel);
        if (logger.isInfoEnabled()) {
            logger.info("레포지토리에서 [" + oldFeed.getXmlUrl() + "] 채널 제거.");
        }
    }

    /**
     * 피드주소변경,카테고리변경,순서변경.
     *
     * 로직의 단순함을 위해 채널객체의 재활용은 고려하지 않음.
     *
     * @param oldFeed
     * @param newFeed
     */
    public void updateChannel(Feed oldFeed, Feed newFeed) {
        removeChannel(oldFeed);
        addChannel(newFeed);
    }

    /**
     * 레포지토리에 카테고리 및 하위 채널들 추가
     *
     * @param newCategory
     */
    public void addCategory(Category newCategory) {
        if (categoryMap.containsKey(newCategory)) return;
        categoryMap.put(newCategory, new TreeMap<Feed, ChannelIF>());
        Collection<Feed> feeds = feedDao.getFeeds(newCategory.getId().toString(), true);
        for (Feed feed : feeds) {
            addChannel(feed);
        }
        if (logger.isInfoEnabled()) {
            logger.info("레포지토리에 [" + newCategory.getName() + "] 카테고리 추가.");
        }
    }

    public void removeCategory(Category oldCategory) {
        SortedMap<Feed, ChannelIF> channelMap = getChannelMap(oldCategory);
        if (channelMap != null) {
            cacheManager.removeChannelCaches(channelMap);
            pollingManager.unregisterChannels(channelMap);
            channelMap.clear();
        }
        categoryMap.remove(oldCategory);
        if (logger.isInfoEnabled()) {
            logger.info("레포지토리에서 [" + oldCategory.getName() + "] 카테고리 제거.");
        }
    }

    /**
     * 이름변경,순서변경
     *
     * @param oldCategory
     * @param newCategory
     */
    public void updateCategory(Category oldCategory, Category newCategory) {
        SortedMap<Feed, ChannelIF> channelMap = getChannelMap(oldCategory);
        categoryMap.remove(oldCategory);
        categoryMap.put(newCategory, channelMap);
        if (logger.isInfoEnabled()) {
            logger.info("레포지토리에서 [" + newCategory.getName() + "(ID: " + newCategory.getId() + ")] 카테고리 업데이트.");
        }
    }

    private boolean hasNotAvailableCategory(Feed feed) {
        if (!feed.getCategory().isUseYn()) return true;
        if (!categoryMap.containsKey(feed.getCategory())) return true;
        return false;
    }
}
