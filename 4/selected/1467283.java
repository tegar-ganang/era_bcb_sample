package com.goodcodeisbeautiful.archtea.search;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.goodcodeisbeautiful.archtea.config.ArchteaPropertyFactory;
import com.goodcodeisbeautiful.archtea.config.OpenSearchConfig;
import com.goodcodeisbeautiful.archtea.config.SearchConfig;
import com.goodcodeisbeautiful.archtea.util.ArchteaUtil;
import com.goodcodeisbeautiful.opensearch.OpenSearchDescription;
import com.goodcodeisbeautiful.opensearch.OpenSearchDescriptionFactory;
import com.goodcodeisbeautiful.opensearch.OpenSearchException;
import com.goodcodeisbeautiful.opensearch.osd10.OpenSearchDescription10;
import com.goodcodeisbeautiful.opensearch.osrss10.DefaultOpenSearchRss;
import com.goodcodeisbeautiful.opensearch.osrss10.OpenSearchRssChannel;
import com.goodcodeisbeautiful.opensearch.osrss10.OpenSearchRssItem;

/**
 * @author hata
 *
 */
public class OpenSearchArchteaSearcher implements ArchteaSearcher {

    /** log */
    private static final Log log = LogFactory.getLog(OpenSearchArchteaSearcher.class);

    /** a default encoding for searchTerms for urlencoding */
    private static final String DEFAULT_SEARCHTERMS_ENCODING = "UTF-8";

    /** a key to replace searchTerms */
    private static final String SEARCH_TERMS_KEY = "{searchTerms}";

    /** a key to replace startIndex */
    private static final String START_INDEX_KEY = "{startIndex}";

    /** a key to replace count */
    private static final String COUNT_KEY = "{count}";

    /** a key to replace startPage */
    private static final String START_PAGE_KEY = "{startPage}";

    /** search config. */
    private SearchConfig m_searchConfig;

    /**
     * Get OpenSearchDescription instance via HttpClient or local file.
     * @param searchConfig is used to get configure information and stored OpenSearchDescription.
     * @param client is used to request to a site.
     * @param locales are locale instances to get a better open search description.
     * @return OpenSearchDescription instance if it can get.
     * @exception IOException is thrown if some local file access or network problem.
     * @exception ArchteaSearchException is thrown if some error happened.
     * @exception OpenSearchException is thrown if some error happened.
     */
    private static OpenSearchDescription getOpenSearchDescription(OpenSearchConfig searchConfig, HttpClient client, Locale[] locales) throws IOException, ArchteaSearchException, OpenSearchException {
        OpenSearchConfig osConfig = (OpenSearchConfig) searchConfig;
        String path = osConfig.getOpenSearchDescriptionPath();
        if (path != null) {
            File descFile = new File(path);
            try {
                ArchteaUtil.writeTo(new URL(osConfig.getOpenSearchDescriptionUrl()), path, osConfig.getInt(ArchteaPropertyFactory.PROP_KEY_OPEN_SEARCH_TIMEOUT, ArchteaPropertyFactory.DEFAULT_OPEN_SEARCH_TIMEOUT));
                if (descFile.exists()) {
                    return OpenSearchDescriptionFactory.newInstance().getInstance(descFile, locales);
                }
            } catch (Exception e) {
                log.error(ArchteaUtil.getString("archtea.search.OpenSearchArchteaSearcher.CannotReadLocalOSSDescFile", new Object[] { descFile.getAbsolutePath() }), e);
            }
        }
        String url = osConfig.getOpenSearchDescriptionUrl();
        if (url == null) throw new ArchteaSearchException("archtea.search.OpenSearchArchteaSearcher.SecondCheckOSSDescIsNull", new Object[] { osConfig.getName() });
        GetMethod getOpenSearchDesc = new GetMethod(url);
        int status = client.executeMethod(getOpenSearchDesc);
        if (status != 200) {
            throw new ArchteaSearchException("archtea.search.OpenSearchArchteaSearcher.OSSDescResIsNot200", new Object[] { osConfig.getName(), Integer.valueOf(status), url });
        }
        byte[] responseBodyBytes = ArchteaUtil.readBytes(getOpenSearchDesc.getResponseBodyAsStream());
        OpenSearchDescription desc = OpenSearchDescriptionFactory.newInstance().getInstance(responseBodyBytes);
        return desc;
    }

    /**
     * Get default timeout.
     * @return a default timeout.
     */
    private static int getHttpClientTimeout(SearchConfig config) {
        if (config == null) return ArchteaPropertyFactory.DEFAULT_OPEN_SEARCH_TIMEOUT;
        String s = null;
        try {
            s = config.getProperty(ArchteaPropertyFactory.PROP_KEY_OPEN_SEARCH_TIMEOUT);
            if (s != null) {
                int ret = Integer.parseInt(s);
                if (0 < ret) return ret;
            }
        } catch (NumberFormatException e) {
            log.warn(ArchteaUtil.getString("archtea.search.OpenSearchArchteaSearcher.failToGetPropertyOpenSearchTimeout", new Object[] { s }), e);
        }
        return ArchteaPropertyFactory.DEFAULT_OPEN_SEARCH_TIMEOUT;
    }

    /**
     * The constructor.
     */
    public OpenSearchArchteaSearcher() {
    }

    /**
     * Search for <code>query</code>.
     * @param query contains query text and some other information.
     * @return the search result.
     * @see ArchteaSearcher#search(ArchteaQuery)
     * @exception ArchteaSearchException is thrown if some error happened.
     */
    public SearchResult search(ArchteaQuery query) throws ArchteaSearchException {
        if (m_searchConfig == null || !(m_searchConfig instanceof OpenSearchConfig)) throw new ArchteaSearchException("archtea.search.OpenSearchArchteaSearcher.ConfigIsNotOpenSearchConfig", new Object[] { m_searchConfig != null ? m_searchConfig.getName() : "null" });
        OpenSearchConfig osConfig = (OpenSearchConfig) m_searchConfig;
        String url = osConfig.getOpenSearchDescriptionUrl();
        if (url == null) throw new ArchteaSearchException("archtea.search.OpenSearchArchteaSearcher.OSSDescIsNull", new Object[] { m_searchConfig.getName() });
        int status = -1;
        HttpClient client = null;
        byte[] responseBodyBytes = null;
        try {
            client = new HttpClient();
            client.setTimeout(getHttpClientTimeout(m_searchConfig));
            client.setConnectionTimeout(getHttpClientTimeout(m_searchConfig));
            OpenSearchDescription desc = getOpenSearchDescription(osConfig, client, query.getLocales());
            String queryUrl = constructQueryUrl(query, desc);
            long beginSearchTime = System.currentTimeMillis();
            GetMethod getQuery = new GetMethod(queryUrl);
            long endSearchTime = System.currentTimeMillis();
            status = client.executeMethod(getQuery);
            if (status != 200) {
                throw new ArchteaSearchException("archtea.search.OpenSearchArchteaSearcher.ResponseIsNot200", new Object[] { m_searchConfig.getName(), Integer.valueOf(status), getQuery.getResponseBodyAsString() });
            }
            responseBodyBytes = ArchteaUtil.readBytes(getQuery.getResponseBodyAsStream());
            return getSearchResult(desc, responseBodyBytes, endSearchTime - beginSearchTime);
        } catch (OpenSearchException e) {
            log.error(ArchteaUtil.getString("archtea.search.FailToCallOpenSearch", new Object[] { "" + status, "" + (responseBodyBytes != null ? responseBodyBytes.length : -1), (responseBodyBytes != null ? new String(responseBodyBytes) : "no data") }), e);
            throw new ArchteaSearchException(e);
        } catch (MalformedURLException e) {
            log.error(ArchteaUtil.getString("archtea.search.FailToCallOpenSearch", new Object[] { "" + status, "-1", "no data" }), e);
            throw new ArchteaSearchException(e);
        } catch (HttpException e) {
            log.error(ArchteaUtil.getString("archtea.search.FailToCallOpenSearch", new Object[] { "" + status, "-1", "no data" }), e);
            throw new ArchteaSearchException(e);
        } catch (IOException e) {
            log.error(ArchteaUtil.getString("archtea.search.FailToCallOpenSearch", new Object[] { "" + status, "-1", "no data" }), e);
            throw new ArchteaSearchException(e);
        } finally {
        }
    }

    /**
     * Get SearchConfig.
     * @return search config.
     * @see com.goodcodeisbeautiful.archtea.search.ArchteaSearcher#getConfig()
     */
    public SearchConfig getConfig() {
        return m_searchConfig;
    }

    /**
     * Set a SearchCofig.
     * @param config is a new config.
     * @see com.goodcodeisbeautiful.archtea.search.ArchteaSearcher#setConfig(com.goodcodeisbeautiful.archtea.config.SearchConfig)
     */
    public void setConfig(SearchConfig config) {
        m_searchConfig = config;
    }

    /**
     * Get a search URL to get Open Search RSS result from a server.
     * @param query is a query class to be passed from search.
     * @param desc is Open Search Description.
     * @return a query URL string to send search request to a open search server.
     * @exception ArchteaSearchException is thrown if some error happened.
     */
    private String constructQueryUrl(ArchteaQuery query, OpenSearchDescription baseDesc) throws ArchteaSearchException {
        if (!(baseDesc instanceof OpenSearchDescription10)) throw new ArchteaSearchException("archtea.search.OpenSearchArchteaSearcher.UnSupportedOSDescVer", new Object[] { baseDesc != null ? baseDesc.getXmlns() : "null" });
        OpenSearchDescription10 desc = (OpenSearchDescription10) baseDesc;
        String searchUrl = desc.getUrl();
        StringBuffer urlBuff = new StringBuffer(searchUrl);
        int index = urlBuff.indexOf(SEARCH_TERMS_KEY);
        if (index != -1) {
            try {
                urlBuff.replace(index, index + SEARCH_TERMS_KEY.length(), query.getQuery() != null ? URLEncoder.encode(query.getQuery(), DEFAULT_SEARCHTERMS_ENCODING) : "");
            } catch (UnsupportedEncodingException e) {
                throw new ArchteaSearchException(e);
            }
        }
        index = urlBuff.indexOf(START_INDEX_KEY);
        if (index != -1) {
            long startIndex = query.getStart();
            if (query.getStartOffset() == 0) startIndex += 1;
            urlBuff.replace(index, index + START_INDEX_KEY.length(), "" + startIndex);
        }
        index = urlBuff.indexOf(COUNT_KEY);
        if (index != -1) {
            urlBuff.replace(index, index + COUNT_KEY.length(), "" + query.getItemsPerPage());
        }
        index = urlBuff.indexOf(START_PAGE_KEY);
        if (index != -1) {
            urlBuff.replace(index, index + START_PAGE_KEY.length(), "" + query.getPageNumber());
        }
        return new String(urlBuff);
    }

    /**
     * Get SearchResult from Open Search Rss XML bytes and search time.
     * 
     * @param desc is an open search description instance.
     * @param responseBodyBytes is the xml text of Open Search RSS.
     * @param searchTimeMills is a search time (millseconds).
     * @return SearchResult instance.
     * @exception OpenSearchException is thrown if some error occured.
     */
    private SearchResult getSearchResult(OpenSearchDescription desc, byte[] responseBodyBytes, long searchTimeMills) throws OpenSearchException {
        String searchName = getConfig().getName();
        if (desc instanceof OpenSearchDescription10) {
            searchName = ((OpenSearchDescription10) desc).getShortName();
        }
        DefaultOpenSearchRss osRss = new DefaultOpenSearchRss(responseBodyBytes);
        OpenSearchRssChannel channel = osRss.getChannel();
        List items = channel.items();
        DefaultSearchResult searchResult = new DefaultSearchResult(searchName, searchTimeMills, channel.getTotalResult(), channel.getItemsPerPage());
        for (int i = 0; i < items.size(); i++) {
            searchResult.addFoundItem(new OpenSearchFoundItem(channel.getStartIndex() + i, (OpenSearchRssItem) items.get(i)));
        }
        return searchResult;
    }
}
