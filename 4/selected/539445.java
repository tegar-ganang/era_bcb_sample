package com.volantis.shared.net.url;

import com.volantis.cache.Cache;
import com.volantis.cache.CacheBuilder;
import com.volantis.cache.CacheFactory;
import com.volantis.shared.dependency.Cacheability;
import com.volantis.shared.dependency.DependencyContextMock;
import com.volantis.shared.dependency.Freshness;
import com.volantis.shared.net.http.HttpServerMock;
import com.volantis.shared.net.http.WaitTransaction;
import com.volantis.shared.net.impl.RunnableTimerTask;
import com.volantis.shared.net.impl.ThreadInterruptingTimingOutTask;
import com.volantis.shared.net.impl.url.URLContentManagerImpl;
import com.volantis.shared.net.url.http.Cookie;
import com.volantis.shared.net.url.http.Header;
import com.volantis.shared.net.url.http.HttpContent;
import com.volantis.shared.net.url.http.HttpUrlConfiguration;
import com.volantis.shared.system.SystemClock;
import com.volantis.shared.system.SystemClockMock;
import com.volantis.shared.time.Comparator;
import com.volantis.shared.time.Period;
import com.volantis.shared.time.Time;
import com.volantis.shared.time.DateFormats;
import com.volantis.testtools.mock.MockFactory;
import com.volantis.testtools.mock.method.MethodAction;
import com.volantis.testtools.mock.method.MethodActionEvent;
import com.volantis.synergetics.testtools.TestCaseAbstract;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.channels.ClosedChannelException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;

/**
 * Test cases for {@link URLContentManager}.
 */
public class URLContentManagerTestCase extends TestCaseAbstract {

    private static final DateFormat RFC1123 = DateFormats.RFC_1123_GMT.create();

    private static final String UA_STRING = "User-Agent: Jakarta Commons-HttpClient/3.0.1";

    private HttpServerMock serverMock;

    protected void setUp() throws Exception {
        super.setUp();
        serverMock = new HttpServerMock();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        serverMock.close();
    }

    /**
     * Ensure that http request times out.
     */
    public void testTimeOut() throws Exception {
        serverMock.addTransaction(new WaitTransaction(2000));
        final URLContentManagerConfiguration config = new URLContentManagerConfiguration();
        config.setDefaultTimeout(Period.inSeconds(1));
        URLContentManager manager = new URLContentManagerImpl(config);
        URL url = serverMock.getURL("/blah.txt?foo");
        try {
            URLContent content = manager.getURLContent(url, null, null);
            content.getInputStream();
        } catch (InterruptedIOException e) {
            assertEquals("Request to '" + url.toExternalForm() + "' timed out after 1000ms", e.getMessage());
        }
    }

    /**
     * Tests that a new request is made without validation headers when caching
     * is turned off even when response is cacheable.
     */
    public void testCachingOff() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Content-Type: text/plain", "Cache-Control: public", "ETag: \"aa\"", "", "<p>hello</p>" });
        final URLContentManager manager = createURLContentManager(Period.INDEFINITELY, false, 0);
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.UNCACHEABLE, content.getDependency().getCacheability());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Content-Type: text/plain", "Cache-Control: public", "ETag: \"aa2\"", "", "<p>hello2</p>" });
        content = manager.getURLContent(url, null, null);
        assertEquals("Content was updated", "<p>hello2</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.UNCACHEABLE, content.getDependency().getCacheability());
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests if ETag header values are sent with the validation requests.
     */
    public void testETag() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Content-Type: text/plain", "Cache-Control: public", "ETag: \"aa\"", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-None-Match: \"aa\"", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 304 Not Modified", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "" });
        content = manager.getURLContent(url, null, null);
        assertEquals("Content was not modified, cached entry is returned", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        assertFalse(serverMock.hasTransactions());
    }

    /**.
     * Tests if cached ETag header values are updated when content changes.
     */
    public void testETagContentChanged() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Content-Type: text/plain", "Cache-Control: public", "ETag: \"aa\"", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-None-Match: \"aa\"", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Content-Type: text/plain", "Cache-Control: public", "ETag: \"aa2\"", "", "<p>hello2</p>" });
        content = manager.getURLContent(url, null, null);
        assertEquals("Content was updated", "<p>hello2</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-None-Match: \"aa2\"", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 304 Not Modified", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "" });
        content = manager.getURLContent(url, null, null);
        assertEquals("Not modified, cached content is used", "<p>hello2</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests if Last-Modified header values are sent with the validation
     * requests.
     */
    public void testLastModified() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Last-Modified: Thu, 30 Dec 1999 23:59:59 GMT", "Content-Type: text/plain", "Cache-Control: public", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-Modified-Since: Thu, 30 Dec 1999 23:59:59 GMT", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 304 Not Modified", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "" });
        content = manager.getURLContent(url, null, null);
        assertEquals("Content was not modified, cached content is returned", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests if Last-Modified header values are sent with the validation
     * requests using the right time format and GMT time zone.
     */
    public void testLastModifiedWithDifferentLocale() throws Exception {
        Locale.setDefault(new Locale("hu", "HU"));
        TimeZone.setDefault(TimeZone.getTimeZone("CET"));
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Last-Modified: Thu, 30 Dec 1999 23:59:59 GMT", "Content-Type: text/plain", "Cache-Control: public", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-Modified-Since: Thu, 30 Dec 1999 23:59:59 GMT", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 304 Not Modified", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "" });
        content = manager.getURLContent(url, null, null);
        assertEquals("Content was not modified, cached content is returned", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        assertFalse(serverMock.hasTransactions());
    }

    /**.
     * Tests if cached Last-Modified header values are updated when content
     * changes.
     */
    public void testLastModifiedContentChanged() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Last-Modified: Thu, 30 Dec 1999 23:59:59 GMT", "Content-Type: text/plain", "Cache-Control: public", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-Modified-Since: Thu, 30 Dec 1999 23:59:59 GMT", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 2000 23:59:59 GMT", "Last-Modified: Sat, 30 Dec 2000 23:59:59 GMT", "Content-Type: text/plain", "Cache-Control: public", "", "<p>hello2</p>" });
        content = manager.getURLContent(url, null, null);
        assertEquals("Content was changed", "<p>hello2</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-Modified-Since: Sat, 30 Dec 2000 23:59:59 GMT", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 304 Not Modified", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "" });
        content = manager.getURLContent(url, null, null);
        assertEquals("Content was not modified since the second request", "<p>hello2</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests with both ETag and Last-Modified headers.
     */
    public void testETagAndLastModified() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Last-Modified: Thu, 30 Dec 1999 23:59:59 GMT", "Content-Type: text/plain", "Cache-Control: public", "ETag: \"aa\"", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-Modified-Since: Thu, 30 Dec 1999 23:59:59 GMT", "If-None-Match: \"aa\"", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 304 Not Modified", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "" });
        content = manager.getURLContent(url, null, null);
        assertEquals("Content was not modified, cached value is used", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests the case when the maximum number of cache entries is exceeded.
     */
    public void testMaxEntryExceeded() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Last-Modified: Thu, 30 Dec 1999 23:59:59 GMT", "Content-Type: text/plain", "Cache-Control: public", "ETag: \"aa\"", "", "<p>hello</p>" });
        final URLContentManager manager = createURLContentManager(Period.INDEFINITELY, true, 1);
        final URL urlFoo = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(urlFoo, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-Modified-Since: Thu, 30 Dec 1999 23:59:59 GMT", "If-None-Match: \"aa\"", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 304 Not Modified", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "" });
        content = manager.getURLContent(urlFoo, null, null);
        assertEquals("Same content returned", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        serverMock.addTransaction(new String[] { "GET /blah.txt?bar HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Last-Modified: Thu, 30 Dec 1999 23:59:59 GMT", "Content-Type: text/plain", "Cache-Control: public", "ETag: \"bb\"", "", "<p>world</p>" });
        final URL urlBar = serverMock.getURL("/blah.txt?bar");
        content = manager.getURLContent(urlBar, null, null);
        assertEquals("Content of the second resource is returned", "<p>world</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        serverMock.addTransaction(new String[] { "GET /blah.txt?bar HTTP/1.1", "If-Modified-Since: Thu, 30 Dec 1999 23:59:59 GMT", "If-None-Match: \"bb\"", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 304 Not Modified", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "" });
        content = manager.getURLContent(urlBar, null, null);
        assertEquals("Right response is returned from cache", "<p>world</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Last-Modified: Thu, 30 Dec 1999 23:59:59 GMT", "Content-Type: text/plain", "Cache-Control: public", "ETag: \"aa2\"", "", "<p>hello2</p>" });
        content = manager.getURLContent(urlFoo, null, null);
        assertEquals("New content of the first resource", "<p>hello2</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests if the Expires header is used when present.
     */
    public void testExpiresOK() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Expires: Fri, 31 Dec 2999 23:59:59 GMT", "Content-Type: text/plain", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.FRESH, content.getDependency().freshness(null));
        assertTrue(Comparator.LT.compare(Period.inSeconds(1000 * 365 * 24 * 60 * 60), content.getDependency().getTimeToLive()));
        content = manager.getURLContent(url, null, null);
        assertEquals("Cached content is returned", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.FRESH, content.getDependency().freshness(null));
        assertTrue(Comparator.LT.compare(Period.inSeconds(1000 * 365 * 24 * 60 * 60), content.getDependency().getTimeToLive()));
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests if max-age directive has higher priority than expires header.
     */
    public void testExpiresOKButMaxAge() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Expires: Fri, 31 Dec 2999 23:59:59 GMT", "Cache-Control: max-age=10", "Content-Type: text/plain", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.REVALIDATE, content.getDependency().freshness(null));
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Expires: Fri, 31 Dec 2999 23:59:59 GMT", "Cache-Control: max-age=10", "Content-Type: text/plain", "", "<p>hello2</p>" });
        content = manager.getURLContent(url, null, null);
        assertEquals("New content is returned", "<p>hello2</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.REVALIDATE, content.getDependency().freshness(null));
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Test with Expires header value in past.
     */
    public void testExpiresOld() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Expires: Fri, 31 Dec 2000 23:59:59 GMT", "Content-Type: text/plain", "ETag: \"aa\"", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.REVALIDATE, content.getDependency().freshness(null));
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-None-Match: \"aa\"", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Content-Type: text/plain", "ETag: \"aa2\"", "", "<p>hello2</p>" });
        content = manager.getURLContent(url, null, null);
        assertEquals("New content is returned", "<p>hello2</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.REVALIDATE, content.getDependency().freshness(null));
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Test with Expires header value in past 2.0.
     */
    public void testExpiresOldNoETag() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Expires: Fri, 31 Dec 2000 23:59:59 GMT", "Content-Type: text/plain", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.REVALIDATE, content.getDependency().freshness(null));
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Content-Type: text/plain", "", "<p>hello2</p>" });
        content = manager.getURLContent(url, null, null);
        assertEquals("<p>hello2</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.UNCACHEABLE, content.getDependency().getCacheability());
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests if no-cache directive overrides the Expires header.
     */
    public void testCacheControlNoCacheExpires() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Expires: Fri, 31 Dec 2999 23:59:59 GMT", "Content-Type: text/plain", "Cache-Control: public, no-cache", "ETag: \"aa\"", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.UNCACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.REVALIDATE, content.getDependency().freshness(null));
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-None-Match: \"aa\"", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 304 Not Modified", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "" });
        content = manager.getURLContent(url, null, null);
        assertEquals("Content returned from cache", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.UNCACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.REVALIDATE, content.getDependency().freshness(null));
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests if no-cache directive overrides the max-age directive.
     */
    public void testCacheControlNoCacheMaxAge() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Content-Type: text/plain", "Cache-Control: public, no-cache, max-age=10000000", "ETag: \"aa\"", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.UNCACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.REVALIDATE, content.getDependency().freshness(null));
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-None-Match: \"aa\"", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 304 Not Modified", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "" });
        content = manager.getURLContent(url, null, null);
        assertEquals("Content returned from cache", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.UNCACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.REVALIDATE, content.getDependency().freshness(null));
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests if no-cache directive overrides the s-maxage directive.
     */
    public void testCacheControlNoCacheSMaxAge() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Expires: Fri, 31 Dec 2999 23:59:59 GMT", "Content-Type: text/plain", "Cache-Control: public, no-cache, s-maxage=100000000", "ETag: \"aa\"", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.UNCACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.REVALIDATE, content.getDependency().freshness(null));
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-None-Match: \"aa\"", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 304 Not Modified", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "" });
        content = manager.getURLContent(url, null, null);
        assertEquals("<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.UNCACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.REVALIDATE, content.getDependency().freshness(null));
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests if no-cache directive overrides the Expires header.
     */
    public void testPragmaNoCacheExpires() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Expires: Fri, 31 Dec 2999 23:59:59 GMT", "Content-Type: text/plain", "Cache-Control: public", "Pragma: no-cache", "ETag: \"aa\"", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.UNCACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.REVALIDATE, content.getDependency().freshness(null));
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-None-Match: \"aa\"", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 304 Not Modified", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "" });
        content = manager.getURLContent(url, null, null);
        assertEquals("Content returned from cache", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.UNCACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.REVALIDATE, content.getDependency().freshness(null));
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests if no-cache directive overrides the max-age directive.
     */
    public void testPragmaNoCacheMaxAge() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Content-Type: text/plain", "Cache-Control: public, max-age=10000000", "Pragma: no-cache", "ETag: \"aa\"", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.UNCACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.REVALIDATE, content.getDependency().freshness(null));
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-None-Match: \"aa\"", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 304 Not Modified", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "" });
        content = manager.getURLContent(url, null, null);
        assertEquals("Content returned from cache", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.UNCACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.REVALIDATE, content.getDependency().freshness(null));
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests if no-cache directive overrides the s-maxage directive.
     */
    public void testPragmaNoCacheSMaxAge() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Expires: Fri, 31 Dec 2999 23:59:59 GMT", "Content-Type: text/plain", "Cache-Control: public, s-maxage=100000000", "Pragma: no-cache", "ETag: \"aa\"", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.UNCACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.REVALIDATE, content.getDependency().freshness(null));
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-None-Match: \"aa\"", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 304 Not Modified", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "" });
        content = manager.getURLContent(url, null, null);
        assertEquals("<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.UNCACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.REVALIDATE, content.getDependency().freshness(null));
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests if private directive prohibits caching.
     */
    public void testCacheControlPrivate() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Expires: Fri, 31 Dec 2999 23:59:59 GMT", "Content-Type: text/plain", "Cache-Control: private", "ETag: \"aa\"", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.UNCACHEABLE, content.getDependency().getCacheability());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Expires: Fri, 31 Dec 2999 23:59:59 GMT", "Content-Type: text/plain", "Cache-Control: private", "ETag: \"aa2\"", "", "<p>hello2</p>" });
        content = manager.getURLContent(url, null, null);
        assertEquals("New content is returned", "<p>hello2</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.UNCACHEABLE, content.getDependency().getCacheability());
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests if max-age directive is honoured.
     */
    public void testCacheControlMaxAge() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: " + RFC1123.format(new Date()), "Content-Type: text/plain", "Cache-Control: max-age=50", "ETag: \"aa\"", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.FRESH, content.getDependency().freshness(null));
        assertTrue(Comparator.GE.compare(Period.inSeconds(50), content.getDependency().getTimeToLive()));
        assertTrue(Comparator.LT.compare(Period.inSeconds(45), content.getDependency().getTimeToLive()));
        content = manager.getURLContent(url, null, null);
        assertEquals("Cached entry returned", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.FRESH, content.getDependency().freshness(null));
        assertTrue(Comparator.GE.compare(Period.inSeconds(50), content.getDependency().getTimeToLive()));
        assertTrue(Comparator.LT.compare(Period.inSeconds(45), content.getDependency().getTimeToLive()));
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests expired max-age directive.
     */
    public void testCacheControlMaxAgeExpired() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: " + RFC1123.format(new Date()), "Content-Type: text/plain", "Cache-Control: max-age=10", "ETag: \"aa\"", "", "<p>hello</p>" });
        final long[] timeShift = new long[1];
        timeShift[0] = 0;
        final SystemClockMock clock = new SystemClockMock("clock", expectations);
        clock.expects.getCurrentTime().does(new MethodAction() {

            public Object perform(MethodActionEvent event) throws Throwable {
                return Time.inMilliSeconds(System.currentTimeMillis() + timeShift[0]);
            }
        }).any();
        final URLContentManager manager = createURLContentManager(clock, Period.INDEFINITELY, true, 1000);
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.FRESH, content.getDependency().freshness(null));
        assertTrue(Comparator.GE.compare(Period.inSeconds(10), content.getDependency().getTimeToLive()));
        assertTrue(Comparator.LT.compare(Period.inSeconds(5), content.getDependency().getTimeToLive()));
        timeShift[0] = 20000;
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-None-Match: \"aa\"", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: " + RFC1123.format(new Date(clock.getCurrentTime().inMillis())), "Content-Type: text/plain", "Cache-Control: max-age=10", "ETag: \"aa2\"", "", "<p>hello2</p>" });
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.REVALIDATE, content.getDependency().freshness(null));
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        content = manager.getURLContent(url, null, null);
        assertEquals("New content is returned", "<p>hello2</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.FRESH, content.getDependency().freshness(null));
        assertTrue(Comparator.GE.compare(Period.inSeconds(10), content.getDependency().getTimeToLive()));
        assertTrue(Comparator.LT.compare(Period.inSeconds(5), content.getDependency().getTimeToLive()));
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests if s-maxage is honoured.
     */
    public void testCacheControlSMaxAge() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: " + RFC1123.format(new Date()), "Content-Type: text/plain", "Cache-Control: s-maxage=50, max-age=0", "ETag: \"aa\"", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.FRESH, content.getDependency().freshness(null));
        assertTrue(Comparator.GE.compare(Period.inSeconds(50), content.getDependency().getTimeToLive()));
        assertTrue(Comparator.LT.compare(Period.inSeconds(45), content.getDependency().getTimeToLive()));
        content = manager.getURLContent(url, null, null);
        assertEquals("Cached entry received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.FRESH, content.getDependency().freshness(null));
        assertTrue(Comparator.GE.compare(Period.inSeconds(50), content.getDependency().getTimeToLive()));
        assertTrue(Comparator.LT.compare(Period.inSeconds(45), content.getDependency().getTimeToLive()));
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests expired s-maxage directive.
     */
    public void testCacheControlSMaxAgeExpired() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: " + RFC1123.format(new Date()), "Content-Type: text/plain", "Cache-Control: s-maxage=10, max-age=50", "ETag: \"aa\"", "", "<p>hello</p>" });
        final long[] timeShift = new long[1];
        timeShift[0] = 0;
        final SystemClockMock clock = new SystemClockMock("clock", expectations);
        clock.expects.getCurrentTime().does(new MethodAction() {

            public Object perform(MethodActionEvent event) throws Throwable {
                return Time.inMilliSeconds(System.currentTimeMillis() + timeShift[0]);
            }
        }).any();
        final URLContentManager manager = createURLContentManager(clock, Period.INDEFINITELY, true, 1000);
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.FRESH, content.getDependency().freshness(null));
        assertTrue(Comparator.GE.compare(Period.inSeconds(10), content.getDependency().getTimeToLive()));
        assertTrue(Comparator.LT.compare(Period.inSeconds(5), content.getDependency().getTimeToLive()));
        timeShift[0] = 20000;
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-None-Match: \"aa\"", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: " + RFC1123.format(new Date(clock.getCurrentTime().inMillis())), "Content-Type: text/plain", "Cache-Control: max-age=10", "ETag: \"aa2\"", "", "<p>hello2</p>" });
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.REVALIDATE, content.getDependency().freshness(null));
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        content = manager.getURLContent(url, null, null);
        assertEquals("New content is returned", "<p>hello2</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.FRESH, content.getDependency().freshness(null));
        assertTrue(Comparator.GE.compare(Period.inSeconds(10), content.getDependency().getTimeToLive()));
        assertTrue(Comparator.LT.compare(Period.inSeconds(5), content.getDependency().getTimeToLive()));
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests if Age header is used for age calculation.
     */
    public void testAge() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: " + RFC1123.format(new Date()), "Content-Type: text/plain", "Cache-Control: max-age=50", "Age: 100", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.REVALIDATE, content.getDependency().freshness(null));
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: " + RFC1123.format(new Date()), "Content-Type: text/plain", "", "<p>hello2</p>" });
        content = manager.getURLContent(url, null, null);
        assertEquals("New content is returned", "<p>hello2</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.UNCACHEABLE, content.getDependency().getCacheability());
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests expired max-age directive.
     */
    public void testVary() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: " + RFC1123.format(new Date()), "Content-Type: text/plain", "Cache-Control: max-age=100", "ETag: \"aa\"", "Vary: ETag", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.UNCACHEABLE, content.getDependency().getCacheability());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: " + RFC1123.format(new Date()), "Content-Type: text/plain", "Cache-Control: max-age=10", "ETag: \"aa2\"", "", "<p>hello2</p>" });
        content = manager.getURLContent(url, null, null);
        assertEquals("New content is returned", "<p>hello2</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.FRESH, content.getDependency().freshness(null));
        assertTrue(Comparator.GE.compare(Period.inSeconds(10), content.getDependency().getTimeToLive()));
        assertTrue(Comparator.LT.compare(Period.inSeconds(5), content.getDependency().getTimeToLive()));
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests if response headers are saved in the HttpContent returned.
     */
    public void testResponseHeaders() throws Exception {
        final String currentDate = RFC1123.format(new Date());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: " + currentDate, "Content-Type: text/plain", "Cache-Control: public", "ETag: \"aa\"", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        final HttpContent content = (HttpContent) manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals("HTTP/1.0", content.getHttpVersion());
        assertEquals(200, content.getStatusCode());
        final Map expectedHeaders = new HashMap();
        expectedHeaders.put("date", currentDate);
        expectedHeaders.put("content-type", "text/plain");
        expectedHeaders.put("cache-control", "public");
        expectedHeaders.put("etag", "\"aa\"");
        boolean ageFound = false;
        for (Iterator iter = content.getHeaders(); iter.hasNext(); ) {
            final Header header = (Header) iter.next();
            final String name = header.getName().toLowerCase();
            if ("age".equals(name) && !ageFound) {
                assertTrue(Integer.parseInt(header.getValue()) >= 0);
                ageFound = true;
            } else {
                final String value = (String) expectedHeaders.remove(name);
                assertEquals(value, header.getValue());
            }
        }
        assertTrue(ageFound);
        assertTrue(expectedHeaders.isEmpty());
    }

    /**
     * Checks if hop-by-hop headers are correctly removed.
     */
    public void testHopByHopResponseHeaders() throws Exception {
        String currentDate = RFC1123.format(new Date());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.1 200 OK", "Date: " + currentDate, "Content-Type: text/plain", "Cache-Control: public", "ETag: \"aa\"", "Connection: connection-value", "Keep-Alive: keep-alive-value", "Proxy-Authenticate: proxy-authenticate-value", "Proxy-Authorization: proxy-authorization-value", "TE: te-value", "Trailers: trailers-value", "Transfer-Encoding: transfer-encoding-value", "Upgrade: upgrade-value", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        HttpContent content = (HttpContent) manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals("HTTP/1.1", content.getHttpVersion());
        assertEquals(200, content.getStatusCode());
        Map expectedHeaders = new HashMap();
        expectedHeaders.put("date", currentDate);
        expectedHeaders.put("content-type", "text/plain");
        expectedHeaders.put("cache-control", "public");
        expectedHeaders.put("etag", "\"aa\"");
        boolean ageFound = false;
        for (Iterator iter = content.getHeaders(); iter.hasNext(); ) {
            final Header header = (Header) iter.next();
            final String name = header.getName().toLowerCase();
            if ("age".equals(name) && !ageFound) {
                assertTrue(Integer.parseInt(header.getValue()) >= 0);
                ageFound = true;
            } else {
                final String value = (String) expectedHeaders.remove(name);
                assertEquals(value, header.getValue());
            }
        }
        assertTrue(ageFound);
        assertTrue(expectedHeaders.isEmpty());
        currentDate = RFC1123.format(new Date());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-None-Match: \"aa\"", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.1 200 OK", "Date: " + currentDate, "Content-Type: text/plain", "Cache-Control: private", "Connection: connection-value", "Keep-Alive: keep-alive-value", "Proxy-Authenticate: proxy-authenticate-value", "Proxy-Authorization: proxy-authorization-value", "TE: te-value", "Trailers: trailers-value", "Transfer-Encoding: transfer-encoding-value", "Upgrade: upgrade-value", "", "<p>hello</p>" });
        content = (HttpContent) manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals("HTTP/1.1", content.getHttpVersion());
        assertEquals(200, content.getStatusCode());
        expectedHeaders = new HashMap();
        expectedHeaders.put("date", currentDate);
        expectedHeaders.put("content-type", "text/plain");
        expectedHeaders.put("cache-control", "private");
        expectedHeaders.put("connection", "connection-value");
        expectedHeaders.put("keep-alive", "keep-alive-value");
        expectedHeaders.put("proxy-authenticate", "proxy-authenticate-value");
        expectedHeaders.put("proxy-authorization", "proxy-authorization-value");
        expectedHeaders.put("te", "te-value");
        expectedHeaders.put("trailers", "trailers-value");
        expectedHeaders.put("transfer-encoding", "transfer-encoding-value");
        expectedHeaders.put("upgrade", "upgrade-value");
        for (Iterator iter = content.getHeaders(); iter.hasNext(); ) {
            final Header header = (Header) iter.next();
            final String name = header.getName().toLowerCase();
            final String value = (String) expectedHeaders.remove(name);
            assertEquals(value, header.getValue());
        }
        assertTrue(expectedHeaders.isEmpty());
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Checks if hop-by-hop headers are not removed if the already cached
     * content become uncacheable.
     */
    public void testHopByHopResponseHeaders304Uncacheable() throws Exception {
        String currentDate = RFC1123.format(new Date());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.1 200 OK", "Date: " + currentDate, "Content-Type: text/plain", "Cache-Control: public", "ETag: \"aa\"", "Connection: connection-value", "Keep-Alive: keep-alive-value", "Proxy-Authenticate: proxy-authenticate-value", "Proxy-Authorization: proxy-authorization-value", "TE: te-value", "Trailers: trailers-value", "Transfer-Encoding: transfer-encoding-value", "Upgrade: upgrade-value", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        HttpContent content = (HttpContent) manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals("HTTP/1.1", content.getHttpVersion());
        assertEquals(200, content.getStatusCode());
        Map expectedHeaders = new HashMap();
        expectedHeaders.put("date", currentDate);
        expectedHeaders.put("content-type", "text/plain");
        expectedHeaders.put("cache-control", "public");
        expectedHeaders.put("etag", "\"aa\"");
        boolean ageFound = false;
        for (Iterator iter = content.getHeaders(); iter.hasNext(); ) {
            final Header header = (Header) iter.next();
            final String name = header.getName().toLowerCase();
            if ("age".equals(name) && !ageFound) {
                assertTrue(Integer.parseInt(header.getValue()) >= 0);
                ageFound = true;
            } else {
                final String value = (String) expectedHeaders.remove(name);
                assertEquals(value, header.getValue());
            }
        }
        assertTrue(ageFound);
        assertTrue(expectedHeaders.isEmpty());
        currentDate = RFC1123.format(new Date());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-None-Match: \"aa\"", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 304 Not Modified", "Date: " + currentDate, "Content-Type: text/plain", "Cache-Control: private", "Connection: connection-value", "Keep-Alive: keep-alive-value", "Proxy-Authenticate: proxy-authenticate-value", "Proxy-Authorization: proxy-authorization-value", "TE: te-value", "Trailers: trailers-value", "Transfer-Encoding: transfer-encoding-value", "Upgrade: upgrade-value", "" });
        content = (HttpContent) manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals("HTTP/1.1", content.getHttpVersion());
        assertEquals(200, content.getStatusCode());
        expectedHeaders = new HashMap();
        expectedHeaders.put("date", currentDate);
        expectedHeaders.put("content-type", "text/plain");
        expectedHeaders.put("cache-control", "private");
        expectedHeaders.put("etag", "\"aa\"");
        expectedHeaders.put("connection", "connection-value");
        expectedHeaders.put("keep-alive", "keep-alive-value");
        expectedHeaders.put("proxy-authenticate", "proxy-authenticate-value");
        expectedHeaders.put("proxy-authorization", "proxy-authorization-value");
        expectedHeaders.put("te", "te-value");
        expectedHeaders.put("trailers", "trailers-value");
        expectedHeaders.put("transfer-encoding", "transfer-encoding-value");
        expectedHeaders.put("upgrade", "upgrade-value");
        for (Iterator iter = content.getHeaders(); iter.hasNext(); ) {
            final Header header = (Header) iter.next();
            final String name = header.getName().toLowerCase();
            final String value = (String) expectedHeaders.remove(name);
            assertEquals(value, header.getValue());
        }
        assertTrue(expectedHeaders.isEmpty());
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests if Age header is added correctly to the cached HTTP content
     */
    public void testAgeHeader() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: " + RFC1123.format(new Date()), "Content-Type: text/plain", "Cache-Control: public, max-age=10000", "Age: 20", "", "<p>hello</p>" });
        final long[] timeShift = new long[1];
        timeShift[0] = 10000;
        final SystemClockMock clock = new SystemClockMock("clock", expectations);
        clock.expects.getCurrentTime().does(new MethodAction() {

            public Object perform(MethodActionEvent event) throws Throwable {
                return Time.inMilliSeconds(System.currentTimeMillis() + timeShift[0]);
            }
        }).any();
        final URLContentManager manager = createURLContentManager(clock, Period.INDEFINITELY, true, 1000);
        final URL url = serverMock.getURL("/blah.txt?foo");
        HttpContent content = (HttpContent) manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertTrue(getAgeHeaderValue(content.getHeaders()) >= 10);
        assertEquals(200, content.getStatusCode());
        timeShift[0] = 100000;
        content = (HttpContent) manager.getURLContent(url, null, null);
        assertEquals("Cached content is returned", "<p>hello</p>\n", toString(content.getInputStream()));
        assertTrue(getAgeHeaderValue(content.getHeaders()) >= 100);
        assertEquals(200, content.getStatusCode());
        assertEquals("HTTP/1.0", content.getHttpVersion());
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests if status code and HTTP version of the original response is kept
     * after a 304 Not Modified validation
     */
    public void testStatusCode() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Content-Type: text/plain", "Cache-Control: public", "ETag: \"aa\"", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        HttpContent content = (HttpContent) manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(200, content.getStatusCode());
        assertEquals("HTTP/1.0", content.getHttpVersion());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-None-Match: \"aa\"", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.1 304 Not Modified", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "" });
        content = (HttpContent) manager.getURLContent(url, null, null);
        assertEquals("Content was not modified, cached entry is returned", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(200, content.getStatusCode());
        assertEquals("HTTP/1.0", content.getHttpVersion());
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests if 1xx Warning headers are removed and 2xx Warning headers are kept
     * in the response from a cached content.
     */
    public void testWarningHeaders() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Content-Type: text/plain", "Cache-Control: public", "ETag: \"aa\"", "Warning: 113 Heuristic expiration", "Warning: 214 Transformation applied", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        HttpContent content = (HttpContent) manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(200, content.getStatusCode());
        Set expectedWarningHeaders = new HashSet();
        expectedWarningHeaders.add("113 Heuristic expiration");
        expectedWarningHeaders.add("214 Transformation applied");
        for (Iterator iter = content.getHeaders(); iter.hasNext(); ) {
            final Header header = (Header) iter.next();
            final String name = header.getName().toLowerCase();
            if ("warning".equals(name)) {
                assertTrue(expectedWarningHeaders.remove(header.getValue()));
            }
        }
        assertTrue(expectedWarningHeaders.isEmpty());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-None-Match: \"aa\"", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 304 Not Modified", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "" });
        content = (HttpContent) manager.getURLContent(url, null, null);
        assertEquals("Content was not modified, cached entry is returned", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(200, content.getStatusCode());
        expectedWarningHeaders = new HashSet();
        expectedWarningHeaders.add("214 Transformation applied");
        for (Iterator iter = content.getHeaders(); iter.hasNext(); ) {
            final Header header = (Header) iter.next();
            final String name = header.getName().toLowerCase();
            if ("warning".equals(name)) {
                assertTrue(expectedWarningHeaders.remove(header.getValue()));
            }
        }
        assertTrue(expectedWarningHeaders.isEmpty());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-None-Match: \"aa\"", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 304 Not Modified", "Date: Fri, 31 Dec 1999 23:59:59 GMT", "Warning: 299 Miscellaneous persistent warning", "" });
        content = (HttpContent) manager.getURLContent(url, null, null);
        assertEquals("Content was not modified, cached entry is returned", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(200, content.getStatusCode());
        expectedWarningHeaders = new HashSet();
        expectedWarningHeaders.add("214 Transformation applied");
        expectedWarningHeaders.add("299 Miscellaneous persistent warning");
        for (Iterator iter = content.getHeaders(); iter.hasNext(); ) {
            final Header header = (Header) iter.next();
            final String name = header.getName().toLowerCase();
            if ("warning".equals(name)) {
                assertTrue(expectedWarningHeaders.remove(header.getValue()));
            }
        }
        assertTrue(expectedWarningHeaders.isEmpty());
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests if response headers in the 304 Not Modified response are merged
     * correctly to the original response headers.
     */
    public void testResponseHeaderCombination() throws Exception {
        String currentDate = RFC1123.format(new Date());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: " + currentDate, "Content-Type: text/plain", "Cache-Control: public", "ETag: \"aa\"", "Foo: bar", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        HttpContent content = (HttpContent) manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(200, content.getStatusCode());
        Map expectedHeaders = new HashMap();
        expectedHeaders.put("date", currentDate);
        expectedHeaders.put("content-type", "text/plain");
        expectedHeaders.put("cache-control", "public");
        expectedHeaders.put("etag", "\"aa\"");
        expectedHeaders.put("foo", "bar");
        boolean ageFound = false;
        for (Iterator iter = content.getHeaders(); iter.hasNext(); ) {
            final Header header = (Header) iter.next();
            final String name = header.getName().toLowerCase();
            if ("age".equals(name) && !ageFound) {
                assertTrue(Integer.parseInt(header.getValue()) >= 0);
                ageFound = true;
            } else {
                final String value = (String) expectedHeaders.remove(name);
                assertEquals(value, header.getValue());
            }
        }
        assertTrue(ageFound);
        assertTrue(expectedHeaders.isEmpty());
        currentDate = RFC1123.format(new Date());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-None-Match: \"aa\"", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 304 Not Modified", "Date: " + currentDate, "Foo: baz", "" });
        content = (HttpContent) manager.getURLContent(url, null, null);
        assertEquals("Content was not modified, cached entry is returned", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(200, content.getStatusCode());
        expectedHeaders = new HashMap();
        expectedHeaders.put("date", currentDate);
        expectedHeaders.put("content-type", "text/plain");
        expectedHeaders.put("cache-control", "public");
        expectedHeaders.put("etag", "\"aa\"");
        expectedHeaders.put("foo", "baz");
        ageFound = false;
        for (Iterator iter = content.getHeaders(); iter.hasNext(); ) {
            final Header header = (Header) iter.next();
            final String name = header.getName().toLowerCase();
            if ("age".equals(name) && !ageFound) {
                assertTrue(Integer.parseInt(header.getValue()) >= 0);
                ageFound = true;
            } else {
                final String value = (String) expectedHeaders.remove(name);
                assertEquals(value, header.getValue());
            }
        }
        assertTrue(ageFound);
        assertTrue(expectedHeaders.isEmpty());
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests if response headers in the 304 Not Modified response are merged
     * correctly to the original response headers.
     */
    public void testResponseCookieCombination() throws Exception {
        String currentDate = RFC1123.format(new Date());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: " + currentDate, "Content-Type: text/plain", "Cache-Control: public", "ETag: \"aa\"", "Set-Cookie: foo=bar", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        HttpContent content = (HttpContent) manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(200, content.getStatusCode());
        Iterator cookiesIter = content.getCookies();
        assertTrue(cookiesIter.hasNext());
        final Cookie fooCookie = (Cookie) cookiesIter.next();
        assertFalse(cookiesIter.hasNext());
        assertEquals("foo", fooCookie.getName());
        assertEquals("bar", fooCookie.getValue());
        currentDate = RFC1123.format(new Date());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-None-Match: \"aa\"", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 304 Not Modified", "Date: " + currentDate, "Set-Cookie: hello=world", "" });
        content = (HttpContent) manager.getURLContent(url, null, null);
        assertEquals("Content was not modified, cached entry is returned", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(200, content.getStatusCode());
        cookiesIter = content.getCookies();
        assertTrue(cookiesIter.hasNext());
        Cookie helloCookie = (Cookie) cookiesIter.next();
        assertFalse(cookiesIter.hasNext());
        assertEquals("hello", helloCookie.getName());
        assertEquals("world", helloCookie.getValue());
        currentDate = RFC1123.format(new Date());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-None-Match: \"aa\"", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 304 Not Modified", "Date: " + currentDate, "" });
        content = (HttpContent) manager.getURLContent(url, null, null);
        assertEquals("Content was not modified, cached entry is returned", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(200, content.getStatusCode());
        cookiesIter = content.getCookies();
        assertTrue(cookiesIter.hasNext());
        helloCookie = (Cookie) cookiesIter.next();
        assertFalse(cookiesIter.hasNext());
        assertEquals("hello", helloCookie.getName());
        assertEquals("world", helloCookie.getValue());
        assertFalse(serverMock.hasTransactions());
    }

    /**
     * Tests if cookies sent with the response are stored correctly in the HTTP
     * content.
     */
    public void testResponseCookies() throws Exception {
        final String currentDate = RFC1123.format(new Date());
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.1 200 OK", "Date: " + currentDate, "Content-Type: text/plain", "Cache-Control: public", "Set-Cookie: hello=world; domain=www.example.com; " + "path=/hello/world; comment=\"hello world\"; max-age=100; " + "version=1", "Set-Cookie: foo=bar;secure", "Set-Cookie: name-only", "", "<p>hello</p>" });
        final URLContentManager manager = createDefaultURLContentManager();
        final URL url = serverMock.getURL("/blah.txt?foo");
        final HttpContent content = (HttpContent) manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals("HTTP/1.1", content.getHttpVersion());
        assertEquals(200, content.getStatusCode());
        final Map cookies = new HashMap();
        for (Iterator iter = content.getCookies(); iter.hasNext(); ) {
            final Cookie cookie = (Cookie) iter.next();
            cookies.put(cookie.getName(), cookie);
        }
        assertEquals(3, cookies.size());
        final Cookie helloCookie = (Cookie) cookies.get("hello");
        assertEquals("world", helloCookie.getValue());
        assertEquals("www.example.com", helloCookie.getDomain());
        assertEquals("hello world", helloCookie.getComment());
        assertTrue(helloCookie.getMaxAge() > 95);
        assertEquals(0, helloCookie.getVersion());
        assertFalse(helloCookie.isSecure());
        final Cookie fooCookie = (Cookie) cookies.get("foo");
        assertEquals("bar", fooCookie.getValue());
        assertEquals("localhost", fooCookie.getDomain());
        assertNull(fooCookie.getComment());
        assertEquals(-1, fooCookie.getMaxAge());
        assertEquals(0, fooCookie.getVersion());
        assertTrue(fooCookie.isSecure());
        final Cookie nameOnlyCookie = (Cookie) cookies.get("name-only");
        assertNull(nameOnlyCookie.getValue());
        assertEquals("localhost", nameOnlyCookie.getDomain());
        assertNull(nameOnlyCookie.getComment());
        assertEquals(-1, nameOnlyCookie.getMaxAge());
        assertEquals(0, nameOnlyCookie.getVersion());
        assertFalse(nameOnlyCookie.isSecure());
    }

    /**
     * Tests expired max-age directive.
     */
    public void testDependencyStored() throws Exception {
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: " + RFC1123.format(new Date()), "Content-Type: text/plain", "Cache-Control: max-age=10", "ETag: \"aa\"", "", "<p>hello</p>" });
        final long[] timeShift = new long[1];
        timeShift[0] = 0;
        final SystemClockMock clock = new SystemClockMock("clock", expectations);
        clock.expects.getCurrentTime().does(new MethodAction() {

            public Object perform(MethodActionEvent event) throws Throwable {
                return Time.inMilliSeconds(System.currentTimeMillis() + timeShift[0]);
            }
        }).any();
        final URLContentManager manager = createURLContentManager(clock, Period.INDEFINITELY, true, 1000);
        final URL url = serverMock.getURL("/blah.txt?foo");
        URLContent content = manager.getURLContent(url, null, null);
        assertEquals("Content was received", "<p>hello</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.FRESH, content.getDependency().freshness(null));
        assertTrue(Comparator.GE.compare(Period.inSeconds(10), content.getDependency().getTimeToLive()));
        assertTrue(Comparator.LT.compare(Period.inSeconds(5), content.getDependency().getTimeToLive()));
        timeShift[0] = 20000;
        serverMock.addTransaction(new String[] { "GET /blah.txt?foo HTTP/1.1", "If-None-Match: \"aa\"", UA_STRING, "Host: " + serverMock.getServerAddress() }, new String[] { "HTTP/1.0 200 OK", "Date: " + RFC1123.format(new Date(clock.getCurrentTime().inMillis())), "Content-Type: text/plain", "Cache-Control: max-age=10", "ETag: \"aa2\"", "", "<p>hello2</p>" });
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.REVALIDATE, content.getDependency().freshness(null));
        assertEquals(Period.ZERO, content.getDependency().getTimeToLive());
        final DependencyContextMock dependencyContextMock = new DependencyContextMock("dependencyContextMock", expectations);
        final MockFactory mockFactory = MockFactory.getDefaultInstance();
        final URLContent[] contents = new URLContent[1];
        dependencyContextMock.fuzzy.setProperty(url, mockFactory.expectsAny()).does(new MethodAction() {

            public Object perform(final MethodActionEvent event) throws Throwable {
                contents[0] = (URLContent) event.getArguments()[1];
                return null;
            }
        });
        content.getDependency().revalidate(dependencyContextMock);
        assertEquals("New content is returned", "<p>hello2</p>\n", toString(contents[0].getInputStream()));
        assertEquals(Cacheability.CACHEABLE, contents[0].getDependency().getCacheability());
        assertEquals(Freshness.FRESH, contents[0].getDependency().freshness(null));
        assertTrue(Comparator.GE.compare(Period.inSeconds(10), contents[0].getDependency().getTimeToLive()));
        assertFalse(serverMock.hasTransactions());
        dependencyContextMock.expects.removeProperty(url).does(new MethodAction() {

            public Object perform(final MethodActionEvent event) throws Throwable {
                final URLContent content = contents[0];
                contents[0] = null;
                return content;
            }
        });
        final HttpUrlConfiguration httpConfig = new HttpUrlConfiguration(dependencyContextMock);
        content = manager.getURLContent(url, null, httpConfig);
        assertEquals("New content is returned", "<p>hello2</p>\n", toString(content.getInputStream()));
        assertEquals(Cacheability.CACHEABLE, content.getDependency().getCacheability());
        assertEquals(Freshness.FRESH, content.getDependency().freshness(null));
        assertTrue(Comparator.GE.compare(Period.inSeconds(10), content.getDependency().getTimeToLive()));
        assertTrue(Comparator.LT.compare(Period.inSeconds(5), content.getDependency().getTimeToLive()));
        assertNull(contents[0]);
    }

    /**
     * Returns the value of the Age header from the headers iterator.
     *
     * @param headersIter the iterator over the headers
     * @return the value of the Age header
     */
    private int getAgeHeaderValue(final Iterator headersIter) {
        int age = -1;
        while (headersIter.hasNext()) {
            final Header header = (Header) headersIter.next();
            if ("age".equalsIgnoreCase(header.getName())) {
                assertTrue(age == -1);
                age = Integer.parseInt(header.getValue());
                assertTrue(age != -1);
            }
        }
        assertTrue(age != -1);
        return age;
    }

    /**
     * Returns the content of the input stream as a string. UTF-8 encoding is
     * assumed.
     *
     * @param is the input stream to transform
     * @return the String representation of the content
     * @throws IOException if reading the input stream fails
     */
    public static String toString(final InputStream is) throws IOException {
        final StringBuffer result = new StringBuffer();
        final InputStreamReader reader = new InputStreamReader(is);
        final char[] buffer = new char[1024];
        for (int len = reader.read(buffer); len != -1; len = reader.read(buffer)) {
            result.append(buffer, 0, len);
        }
        return result.toString();
    }

    public void readPauseRead(InputStream is, int pauseInMillis) throws Exception {
        try {
            final InputStreamReader reader = new InputStreamReader(is);
            final char[] buffer = new char[1024];
            reader.read(buffer);
            final long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < pauseInMillis) {
                Thread.yield();
            }
            for (int count = reader.read(buffer); count != -1; count = reader.read(buffer)) {
            }
        } finally {
            is.close();
        }
    }

    /**
     * Creates a content manager with default values: Period.INDEFINITELY used
     * as default timeout, caching is enabled and the maximum number of cached
     * entries is 1000.
     *
     * @return the content manager with the "default" settings
     */
    private URLContentManager createDefaultURLContentManager() {
        return createURLContentManager(Period.INDEFINITELY, true, 1000);
    }

    /**
     * Creates a content manager using the default system clock.
     *
     * @param timeout the default timeout value
     * @param cacheable true, iff caching is enabled
     * @param maxCount the maximum number of cache entries.
     * @return the created content manager
     */
    private URLContentManager createURLContentManager(final Period timeout, final boolean cacheable, final int maxCount) {
        return createURLContentManager(SystemClock.getDefaultInstance(), timeout, cacheable, maxCount);
    }

    /**
     * Creates a content manager.
     *
     * @param clock the clock to be used by the manager
     * @param timeout the default timeout value
     * @param cacheable true, iff caching is enabled
     * @param maxCount the maximum number of cache entries.
     * @return the created content manager
     */
    private URLContentManager createURLContentManager(final SystemClock clock, final Period timeout, final boolean cacheable, final int maxCount) {
        Cache cache = null;
        if (cacheable) {
            final CacheBuilder cacheBuilder = CacheFactory.getDefaultInstance().createCacheBuilder();
            cacheBuilder.setMaxCount(maxCount);
            cacheBuilder.setClock(clock);
            cacheBuilder.setExpirationChecker(new URLContentValidationChecker());
            cache = cacheBuilder.buildCache();
        }
        final URLContentManagerConfiguration config = new URLContentManagerConfiguration();
        config.setDefaultTimeout(timeout);
        config.setCache(cache);
        final URLContentManagerFactory factory = URLContentManagerFactory.getDefaultInstance();
        return factory.createContentManager(config);
    }

    /**
     * Ensure that connection failures do not produce a file descriptor leak.
     *
     * <p>Renamed this to stop it running as it sometimes hangs on some 
     * machines.</p>
     */
    public void notestConnectFailures() throws Exception {
        final URLContentManager manager = createDefaultURLContentManager();
        ServerSocket socket = new ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        for (int i = 0; i < 2000; i += 1) {
            try {
                URLContent content = manager.getURLContent("http://127.0.0.1:" + port, null, null);
                InputStream stream = content.getInputStream();
                byte[] bytes = new byte[1024];
                int read;
                while ((read = stream.read(bytes)) != -1) {
                    System.out.write(bytes, 0, read);
                }
                stream.close();
            } catch (IOException expected) {
                assertEquals("Connection refused when connecting to " + "/127.0.0.1:" + port, expected.getMessage());
            }
        }
    }

    public void testReadFromFileNoTimeout() throws Exception {
        String expected = toString(URLContentManagerTestCase.class.getResourceAsStream("content.xml"));
        Period timeout = null;
        URLContent readContent = readFromFile("content.xml", timeout);
        String readContentAsString = toString(readContent.getInputStream());
        assertEquals(expected, readContentAsString);
    }

    public void testInterruptingDuringReadFromFile() throws Exception {
        final Timer timer = new Timer(true);
        timer.schedule(new RunnableTimerTask(new ThreadInterruptingTimingOutTask()), 500);
        Period timeout = null;
        try {
            URLContent fileContents = readFromFile("content.xml", timeout);
            readPauseRead(fileContents.getInputStream(), 1000);
            fail("ClosedChannelException should be thrown");
        } catch (ClosedChannelException e) {
        }
    }

    public void testReadingFromStreamThatHasBeenTimedOut() throws Exception {
        Period timeout = Period.inMilliSeconds(500);
        try {
            URLContent readContent = readFromFile("content.xml", timeout);
            readPauseRead(readContent.getInputStream(), 1000);
        } catch (ClosedChannelException expectedException) {
        }
    }

    public void testReadingFromStreamWithLongTimeOutPeriod() throws Exception {
        Period timeout = Period.inSeconds(500);
        try {
            URLContent readContent = readFromFile("content.xml", timeout);
            readPauseRead(readContent.getInputStream(), 0);
        } catch (ClosedChannelException expectedException) {
        }
    }

    private URLContent readFromFile(String filename, Period timeout) throws IOException {
        final URLContentManager manager = createDefaultURLContentManager();
        URL fileToRead = URLContentManagerTestCase.class.getResource(filename);
        return manager.getURLContent(fileToRead, timeout, null);
    }
}
