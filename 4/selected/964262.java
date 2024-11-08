package com.goodcodeisbeautiful.archtea.search.rss20;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.goodcodeisbeautiful.archtea.config.ArchteaPropertyFactory;
import com.goodcodeisbeautiful.archtea.config.SearchConfig;
import com.goodcodeisbeautiful.archtea.search.ArchteaQuery;
import com.goodcodeisbeautiful.archtea.search.ArchteaSearchException;
import com.goodcodeisbeautiful.archtea.search.ArchteaSearcher;
import com.goodcodeisbeautiful.archtea.search.DefaultSearchResult;
import com.goodcodeisbeautiful.archtea.search.SearchResult;
import com.goodcodeisbeautiful.archtea.util.ArchteaUtil;
import com.goodcodeisbeautiful.syndic8.rss20.Rss20;
import com.goodcodeisbeautiful.syndic8.rss20.Rss20Channel;
import com.goodcodeisbeautiful.syndic8.rss20.Rss20Exception;
import com.goodcodeisbeautiful.syndic8.rss20.Rss20Factory;
import com.goodcodeisbeautiful.syndic8.rss20.Rss20Item;

/**
 * ArchteaSearcher class for RSS 2.0.
 * This class support to show searchconfig.
 * This searcher can use when you set external-search to archtea-serach-config.
 * <pre>
 * &lt;search-config name="rss20-example"&gt;
 *   &lt;external-search&gt;
 *     &lt;property name="archtea.config.externalArchteaSearcherClass"
 *      value=" com.goodcodeisbeautiful.archtea.search.rss20.Rss20ArchteaSearcher"/&gt;
 *   &lt;/external-search&gt;
 * &lt;/search-config&gt;
        &lt;open-search-description&gt;
            &lt;path&gt;main-opensearch-description.xml&lt;/path&gt;
        &lt;/open-search-description&gt;
 * </pre>
 */
public class Rss20ArchteaSearcher implements ArchteaSearcher {

    /** log */
    private static final Log log = LogFactory.getLog(Rss20ArchteaSearcher.class);

    /** items per page */
    private static final int DEFAULT_ITEMS_PER_PAGE = 10;

    /** index number start with 1 */
    private static final int DEFAULT_INDEX_OFFSET = 1;

    /**
     * config information for rss 2.0.
     */
    private SearchConfig m_config;

    /**
     * Get default timeout.
     * @return a default timeout.
     */
    private static int getHttpClientTimeout(SearchConfig config) {
        return ArchteaPropertyFactory.DEFAULT_OPEN_SEARCH_TIMEOUT;
    }

    /**
     * Search method.
     */
    public SearchResult search(ArchteaQuery query) throws ArchteaSearchException {
        int status = -1;
        HttpClient client = null;
        byte[] responseBodyBytes = null;
        try {
            client = new HttpClient();
            client.setTimeout(getHttpClientTimeout(m_config));
            client.setConnectionTimeout(getHttpClientTimeout(m_config));
            String queryUrl = constructQueryUrl(query);
            long beginSearchTime = System.currentTimeMillis();
            GetMethod getQuery = new GetMethod(queryUrl);
            long endSearchTime = System.currentTimeMillis();
            status = client.executeMethod(getQuery);
            if (status != 200) {
                throw new ArchteaSearchException("archtea.search.Rss20ArchteaSearcher.ResponseIsNot200", new Object[] { m_config.getName(), Integer.valueOf(status), getQuery.getResponseBodyAsString() });
            }
            responseBodyBytes = ArchteaUtil.readBytes(getQuery.getResponseBodyAsStream());
            return getSearchResult(responseBodyBytes, endSearchTime - beginSearchTime);
        } catch (Rss20Exception e) {
            log.error(ArchteaUtil.getString("archtea.search.FailToCallRss20", new Object[] { "" + status, "" + (responseBodyBytes != null ? responseBodyBytes.length : -1), (responseBodyBytes != null ? new String(responseBodyBytes) : "no data") }), e);
            throw new ArchteaSearchException(e);
        } catch (MalformedURLException e) {
            log.error(ArchteaUtil.getString("archtea.search.FailToCallRss20", new Object[] { "" + status, "-1", "" + responseBodyBytes }), e);
            throw new ArchteaSearchException(e);
        } catch (HttpException e) {
            log.error(ArchteaUtil.getString("archtea.search.FailToCallRss20", new Object[] { "" + status, "-1", "" + responseBodyBytes }), e);
            throw new ArchteaSearchException(e);
        } catch (IOException e) {
            log.error(ArchteaUtil.getString("archtea.search.FailToCallRss20", new Object[] { "" + status, "-1", "" + responseBodyBytes }), e);
            throw new ArchteaSearchException(e);
        } finally {
        }
    }

    /**
     * Get config information.
     * @return config.
     */
    public SearchConfig getConfig() {
        return m_config;
    }

    /**
     * Set a new config information.
     * @param config is a new config.
     */
    public void setConfig(SearchConfig config) {
        m_config = config;
    }

    /**
     * Get a search URL to get Open Search RSS result from a server.
     * @param query is a query class to be passed from search.
     * @param desc is Open Search Description.
     * @return a query URL string to send search request to a open search server.
     * @exception ArchteaSearchException is thrown if some error happened.
     */
    private String constructQueryUrl(ArchteaQuery query) throws ArchteaSearchException {
        String url = null;
        if (m_config != null) {
            url = m_config.getProperty(ArchteaPropertyFactory.PROP_KEY_EXTERNAL_URL);
        }
        if (url != null) return url; else throw new ArchteaSearchException("archtea.search.Rss20ArchteaSearcher.CannotGetExternalURL", new Object[] { m_config != null ? m_config.getName() : null });
    }

    /**
     * Get SearchResult from Rss 2.0 bytes and search time.
     * 
     * @param responseBodyBytes is the xml text of RSS.
     * @param searchTimeMills is a search time (millseconds).
     * @return SearchResult instance.
     */
    private SearchResult getSearchResult(byte[] responseBodyBytes, long searchTimeMills) throws Rss20Exception {
        String searchName = getConfig().getName();
        Rss20Factory factory = new Rss20Factory();
        Rss20 rss20 = factory.newInstance(new ByteArrayInputStream(responseBodyBytes));
        Rss20Channel channel = rss20.getChannel();
        Rss20Item[] items = channel.getItems();
        if (channel.getTitle() != null) searchName = channel.getTitle();
        DefaultSearchResult searchResult = new DefaultSearchResult(searchName, searchTimeMills, items.length, DEFAULT_ITEMS_PER_PAGE);
        for (int i = 0; i < items.length; i++) {
            searchResult.addFoundItem(new Rss20FoundItem(DEFAULT_INDEX_OFFSET + i, items[i]));
        }
        return searchResult;
    }
}
