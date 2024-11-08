package com.goodcodeisbeautiful.archtea.search.os;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.List;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.goodcodeisbeautiful.archtea.config.OpenSearchConfig;
import com.goodcodeisbeautiful.archtea.search.ArchteaQuery;
import com.goodcodeisbeautiful.archtea.search.ArchteaSearchException;
import com.goodcodeisbeautiful.archtea.search.ArchteaSearcher;
import com.goodcodeisbeautiful.archtea.search.DefaultSearchResult;
import com.goodcodeisbeautiful.archtea.search.OpenSearchArchteaSearcher;
import com.goodcodeisbeautiful.archtea.search.OpenSearchFoundItem;
import com.goodcodeisbeautiful.archtea.search.SearchResult;
import com.goodcodeisbeautiful.archtea.util.ArchteaUtil;
import com.goodcodeisbeautiful.opensearch.OpenSearchDescription;
import com.goodcodeisbeautiful.opensearch.OpenSearchException;
import com.goodcodeisbeautiful.opensearch.osd10.OpenSearchDescription10;
import com.goodcodeisbeautiful.opensearch.osrss10.DefaultOpenSearchRss;
import com.goodcodeisbeautiful.opensearch.osrss10.OpenSearchRssChannel;
import com.goodcodeisbeautiful.opensearch.osrss10.OpenSearchRssItem;

public class OpenSearch10ArchteaSearcher extends AbstractOpenSearchArchteaSearcher implements ArchteaSearcher {

    /** log */
    private static final Log log = LogFactory.getLog(OpenSearchArchteaSearcher.class);

    public OpenSearch10ArchteaSearcher() {
    }

    /**
     * Search for <code>query</code>.
     * @param query contains query text and some other information.
     * @return the search result.
     * @see ArchteaSearcher#search(ArchteaQuery)
     * @exception ArchteaSearchException is thrown if some error happened.
     */
    public SearchResult search(ArchteaQuery query) throws ArchteaSearchException {
        OpenSearchConfig osConfig = getOpenSearchConfig();
        if (osConfig == null) throw new ArchteaSearchException("archtea.search.OpenSearchArchteaSearcher.ConfigIsNotOpenSearchConfig", new Object[] { "null" });
        String url = osConfig.getOpenSearchDescriptionUrl();
        if (url == null) throw new ArchteaSearchException("archtea.search.OpenSearchArchteaSearcher.OSSDescIsNull", new Object[] { osConfig.getName() });
        int status = -1;
        HttpClient client = null;
        byte[] responseBodyBytes = null;
        try {
            String queryUrl = constructQueryUrl(query, osConfig.getOpenSearchDescription());
            client = new HttpClient();
            client.setTimeout(getHttpClientTimeout());
            client.setConnectionTimeout(getHttpClientTimeout());
            long beginSearchTime = System.currentTimeMillis();
            GetMethod getQuery = new GetMethod(queryUrl);
            status = client.executeMethod(getQuery);
            long endSearchTime = System.currentTimeMillis();
            if (status != 200) {
                throw new ArchteaSearchException("archtea.search.OpenSearchArchteaSearcher.ResponseIsNot200", new Object[] { osConfig.getName(), Integer.valueOf(status), getQuery.getResponseBodyAsString() });
            }
            responseBodyBytes = ArchteaUtil.readBytes(getQuery.getResponseBodyAsStream());
            return getSearchResult(osConfig.getOpenSearchDescription(), responseBodyBytes, endSearchTime - beginSearchTime);
        } catch (OpenSearchException e) {
            log.error(ArchteaUtil.getString("archtea.search.FailToCallOpenSearch", new Object[] { "" + status, "" + (responseBodyBytes != null ? responseBodyBytes.length : -1), (responseBodyBytes != null ? new String(responseBodyBytes) : "no data") }), e);
            throw new ArchteaSearchException(e);
        } catch (MalformedURLException e) {
            log.error(ArchteaUtil.getString("archtea.search.FailToCallOpenSearch", new Object[] { "" + status, "" + (responseBodyBytes != null ? responseBodyBytes.length : -1), (responseBodyBytes != null ? new String(responseBodyBytes) : "no data") }), e);
            throw new ArchteaSearchException(e);
        } catch (HttpException e) {
            log.error(ArchteaUtil.getString("archtea.search.FailToCallOpenSearch", new Object[] { "" + status, "" + (responseBodyBytes != null ? responseBodyBytes.length : -1), (responseBodyBytes != null ? new String(responseBodyBytes) : "no data") }), e);
            throw new ArchteaSearchException(e);
        } catch (IOException e) {
            log.error(ArchteaUtil.getString("archtea.search.FailToCallOpenSearch", new Object[] { "" + status, "" + (responseBodyBytes != null ? responseBodyBytes.length : -1), (responseBodyBytes != null ? new String(responseBodyBytes) : "no data") }), e);
            throw new ArchteaSearchException(e);
        } finally {
        }
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
