package kr.pe.javarss.manager;

import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.jsp.PageContext;
import kr.pe.javarss.model.Feed;
import org.springframework.web.context.ServletContextAware;
import com.opensymphony.oscache.base.Cache;
import com.opensymphony.oscache.base.CacheEntry;
import com.opensymphony.oscache.web.ServletCacheAdministrator;
import de.nava.informa.core.ChannelIF;

/**
 * 화면 컨텐츠, 메뉴 등 캐쉬 담당 매니저.
 */
public class CacheManager implements ServletContextAware {

    private int channelCachePeriod = CacheEntry.INDEFINITE_EXPIRY;

    private ServletCacheAdministrator cacheAdmin;

    /**
     * application 스코프 캐쉬
     */
    private Cache cache;

    private ServletContext servletContext;

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public int getChannelCachePeriod() {
        return channelCachePeriod;
    }

    public void setChannelCachePeriod(int channelCachePeriod) {
        this.channelCachePeriod = channelCachePeriod;
    }

    public void init() {
        cacheAdmin = ServletCacheAdministrator.getInstance(servletContext);
        cache = cacheAdmin.getAppScopeCache(servletContext);
    }

    public void destroy() {
        ServletCacheAdministrator.destroyInstance(servletContext);
    }

    /**
     * 채널 캐쉬에 대한 키 리턴.
     *
     * @param channel
     * @return
     */
    public String getChannelCacheKey(ChannelIF channel) {
        return "_channel_" + channel.getId();
    }

    /**
     * 관련된 채널의 화면 캐시 갱신(flush).
     *
     * @param channel
     */
    public void flushChannelCache(ChannelIF channel) {
        if (channel == null) return;
        String key = getChannelCacheKey(channel);
        String actualKey = cacheAdmin.generateEntryKey(key, null, PageContext.APPLICATION_SCOPE);
        cache.flushEntry(actualKey);
    }

    public void removeChannelCache(ChannelIF channel) {
        if (channel == null) return;
        String key = getChannelCacheKey(channel);
        String actualKey = cacheAdmin.generateEntryKey(key, null, PageContext.APPLICATION_SCOPE);
        cache.removeEntry(actualKey);
    }

    /**
     * @param channels Map<Feed, ChannelIF>
     */
    public void removeChannelCaches(Map<Feed, ChannelIF> channels) {
        if (channels == null) return;
        for (ChannelIF channel : channels.values()) {
            removeChannelCache(channel);
        }
    }
}
