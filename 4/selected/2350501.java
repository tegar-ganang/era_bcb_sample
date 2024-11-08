package org.wwweeeportal.portal.contentstores;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;
import javax.activation.*;
import org.xml.sax.*;
import org.xml.sax.ext.*;
import org.w3c.dom.*;
import javax.ws.rs.core.*;
import com.sun.syndication.fetcher.impl.*;
import com.sun.syndication.fetcher.*;
import com.sun.syndication.feed.synd.*;
import com.sun.syndication.feed.module.*;
import com.sun.syndication.io.*;
import org.wwweeeportal.util.*;
import org.wwweeeportal.util.convert.*;
import org.wwweeeportal.util.logging.*;
import org.wwweeeportal.util.xml.*;
import org.wwweeeportal.util.xml.dom.*;
import org.wwweeeportal.util.xml.html.*;
import org.wwweeeportal.util.xml.sax.*;
import org.wwweeeportal.util.ws.rs.*;
import org.wwweeeportal.portal.*;
import org.wwweeeportal.portal.channels.*;
import org.wwweeeportal.portal.channelplugins.*;
import org.wwweeeportal.portal.feed.fetcher.*;

/**
 * The base for {@link org.wwweeeportal.portal.ContentManager.ContentStore} implementations generated from a Web Feed.
 * 
 * @author <a href="http://www.hubick.com/">Chris Hubick</a>
 */
public abstract class FeedContentStore extends ContentManager.ContentStore {

    public static final String CONF_RESOURCE_PROP = "FeedContentStore.ContentDefinition.Template";

    public static final String DEFAULT_CONF_RESOURCE = "/FeedContentDefinitionTemplate.xml";

    /**
   * @see ConfigManager#getCache(Class, String, RSProperties, String, String, String)
   */
    public static final String FETCHER_JCACHE_PROPERTY_PROP = "FeedContentStore.Fetcher.JCache.Property.";

    /**
   * @see ConfigManager#getCache(Class, String, RSProperties, String, String, String)
   */
    public static final String FETCHER_JCACHE_NAME_PROP = "FeedContentStore.Fetcher.JCache.Name";

    /**
   * @see ConfigManager#getCache(Class, String, RSProperties, String, String, String)
   */
    public static final String FETCHER_JCACHE_PROPERTIES_CACHE_NAME_PROP_PROP = "FeedContentStore.Fetcher.JCache.Properties.CacheNameProp";

    public static final String DEFAULT_REFRESH_INTERVAL_MINUTES_PROP = "FeedContentStore.Refresh.DefaultIntervalMinutes";

    public static final String SCHEDULED_REFRESH_ENABLE_PROP = "FeedContentStore.Refresh.Scheduled.Enable";

    public static final String FEED_TITLE_LINKS_DISABLE_PROP = "FeedContentStore.FeedTitle.Links.Disable";

    protected static final Integer DEFAULT_REFRESH_INTERVAL_MINUTES = Integer.valueOf(10);

    protected final FeedFetcherCache fetcherCache;

    protected final int defaultRefreshIntervalMinutes;

    protected final ScheduledExecutorService scheduledExecutorService;

    protected final boolean disableFeedTitleLinks;

    protected final ContentManager.PageContentContainer templateContentContainer;

    protected final Set<FeedPageDefinition> feedPageDefinitionCache = new HashSet<FeedPageDefinition>(256);

    protected final Map<FeedPageDefinition, Page> pageCache = new HashMap<FeedPageDefinition, Page>(256);

    public FeedContentStore(final ContentManager contentManager, final RSProperties properties) throws WWWeeePortal.Exception {
        contentManager.super(properties);
        fetcherCache = new JCacheFeedFetcherCache(ConfigManager.getCache(FeedContentStore.class, "Fetcher", this.properties, FETCHER_JCACHE_PROPERTY_PROP, FETCHER_JCACHE_NAME_PROP, FETCHER_JCACHE_PROPERTIES_CACHE_NAME_PROP_PROP), getPortal().getPortalID() + ".FeedContentStore.FetcherCache.");
        defaultRefreshIntervalMinutes = getConfigProp(DEFAULT_REFRESH_INTERVAL_MINUTES_PROP, null, null, RSProperties.RESULT_INTEGER_CONVERTER, DEFAULT_REFRESH_INTERVAL_MINUTES, false, false).intValue();
        final boolean scheduledRefreshEnabled = getConfigProp(SCHEDULED_REFRESH_ENABLE_PROP, null, null, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
        scheduledExecutorService = (scheduledRefreshEnabled) ? Executors.newScheduledThreadPool(1) : null;
        disableFeedTitleLinks = getConfigProp(FEED_TITLE_LINKS_DISABLE_PROP, null, null, RSProperties.RESULT_BOOLEAN_CONVERTER, Boolean.FALSE, false, false).booleanValue();
        final Document feedContentDefinitionTemplateDocument = ConfigManager.getConfigProp(this.properties, CONF_RESOURCE_PROP, null, null, new RSProperties.Entry<String>(CONF_RESOURCE_PROP, DEFAULT_CONF_RESOURCE, Locale.ROOT, null), getPortal().getMarkupManager().getConfigDocumentConverter(), null, false, false);
        templateContentContainer = ContentManager.PageContentContainer.parseXML(this.properties, feedContentDefinitionTemplateDocument.getDocumentElement(), null);
        return;
    }

    @Override
    protected void destroyInternal(final LogAnnotation.Message logMessage) {
        super.destroyInternal(logMessage);
        try {
            if (scheduledExecutorService != null) {
                scheduledExecutorService.shutdown();
            }
        } catch (java.lang.Exception e) {
            getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
        try {
            if (scheduledExecutorService != null) {
                scheduledExecutorService.awaitTermination(1, TimeUnit.MINUTES);
            }
        } catch (java.lang.Exception e) {
            getLogger().log(Level.SEVERE, e.getMessage(), e);
        }
        feedPageDefinitionCache.clear();
        for (Page page : pageCache.values()) {
            try {
                page.destroy();
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, e.getMessage(), e);
            }
        }
        pageCache.clear();
        return;
    }

    protected static final String createID(final String value, final String defaultValue) {
        return ((value != null) && (!value.isEmpty())) ? value : defaultValue;
    }

    protected static final FeedPageDefinition getCachedFeedPageDefinition(final Set<FeedPageDefinition> feedPageDefinitionCache, final ContentManager.PageDefinition.Key pageKey) {
        synchronized (feedPageDefinitionCache) {
            for (FeedPageDefinition feedPageDefinition : feedPageDefinitionCache) {
                if (!feedPageDefinition.getFeedPageDescriptor().getPageKey().equals(pageKey)) continue;
                return feedPageDefinition;
            }
        }
        return null;
    }

    protected static final void cacheFeedPageDefinition(final Set<FeedPageDefinition> feedPageDefinitionCache, final Map<FeedPageDefinition, Page> pageCache, final FeedPageDefinition feedPageDefinition) {
        synchronized (feedPageDefinition.getFeedPageDescriptor()) {
            final FeedPageDefinition oldFeedPageDefinition = getCachedFeedPageDefinition(feedPageDefinitionCache, feedPageDefinition.getPageKey());
            if (oldFeedPageDefinition != null) {
                feedPageDefinitionCache.remove(oldFeedPageDefinition);
                if (oldFeedPageDefinition.getScheduledFuture() != null) {
                    oldFeedPageDefinition.getScheduledFuture().cancel(false);
                }
                final Page oldPage = pageCache.remove(oldFeedPageDefinition);
                if (oldPage != null) {
                    oldPage.destroy();
                }
            }
            feedPageDefinitionCache.add(feedPageDefinition);
        }
        return;
    }

    protected static final FeedPageDescriptor getFeedPageDescriptor(final List<FeedPageDescriptor> feedPageDescriptors, final String pageGroupID, final String pageID) {
        for (FeedPageDescriptor feedPageDescriptor : feedPageDescriptors) {
            if ((feedPageDescriptor.getPageKey().getPageGroupID().equals(pageGroupID)) && (feedPageDescriptor.getPageKey().getPageID().equals(pageID))) {
                return feedPageDescriptor;
            }
        }
        return null;
    }

    protected abstract List<FeedPageDescriptor> getFeedPageDescriptors(final SecurityContext securityContext) throws WWWeeePortal.Exception;

    protected abstract FeedPageDescriptor getFeedPageDescriptor(final ContentManager.PageDefinition.Key pageKey) throws WWWeeePortal.Exception;

    protected ContentManager.PageDefinition<?> getTemplatePageDefinition(final String pageGroupID, final String pageID) throws WWWeeePortal.Exception {
        final ContentManager.PageGroupDefinition templatePageGroupDefinition = templateContentContainer.getChildDefinition(pageGroupID);
        if (templatePageGroupDefinition != null) {
            ContentManager.PageDefinition<?> templatePageDefinition = templatePageGroupDefinition.getChildDefinition(pageID);
            if (templatePageDefinition != null) return templatePageDefinition;
            templatePageDefinition = templatePageGroupDefinition.getFirstChildDefinition();
            if (templatePageDefinition != null) return templatePageDefinition;
        }
        return templateContentContainer.getFirstPageDefinition();
    }

    protected ContentManager.LocalChannelDefinition<ProxyChannel> createProxyChannelDefinitionFromEntry(final FeedPageDescriptor feedPageDescriptor, final SyndFeed syndFeed, final ContentManager.ChannelGroupDefinition channelGroupDefinition, final SyndEntry syndEntry, final int entryIndex) throws WWWeeePortal.Exception {
        final String channelID = createID(syndEntry.getUri(), "entry_" + String.valueOf(entryIndex));
        final ContentManager.LocalChannelSpecification<ProxyChannel> localChannelSpecification = new ContentManager.LocalChannelSpecification<ProxyChannel>(channelGroupDefinition, new RSProperties(this, channelGroupDefinition.getProperties()), channelID, null);
        final RSProperties channelProperties = new RSProperties(this, localChannelSpecification.getProperties());
        if (syndEntry.getTitle() != null) channelProperties.setProp(Channel.TITLE_TEXT_PROP, syndEntry.getTitle(), Locale.ROOT, null);
        final String link = syndEntry.getLink();
        channelProperties.setProp(ProxyChannel.BASE_URI_PROP, link, Locale.ROOT, null);
        final ContentManager.LocalChannelDefinition<ProxyChannel> channelDefinition = new ContentManager.LocalChannelDefinition<ProxyChannel>(localChannelSpecification, channelProperties, feedPageDescriptor.getChannelAccessControl(), ProxyChannel.class);
        new ContentManager.ChannelPluginDefinition<ProxyChannelHTMLSource>(channelDefinition, new RSProperties(this, channelDefinition.getProperties()), null, ProxyChannelHTMLSource.class);
        new ContentManager.ChannelPluginDefinition<ChannelTitle>(channelDefinition, new RSProperties(this, channelDefinition.getProperties()), null, ChannelTitle.class);
        new ContentManager.ChannelPluginDefinition<ChannelCache>(channelDefinition, new RSProperties(this, channelDefinition.getProperties()), null, ChannelCache.class);
        return channelDefinition;
    }

    protected Element createContentElement(final Element documentElement, final SyndEntry syndEntry, final SyndContent content, final String coreName) throws WWWeeePortal.Exception {
        final Element contentElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, null, "div", documentElement);
        contentElement.setAttributeNS(null, "id", coreName);
        String contentType = content.getType();
        MimeType contentMimeType = null;
        if ("html".equalsIgnoreCase(contentType)) {
            contentMimeType = HTMLUtil.TEXT_HTML_MIME_TYPE;
        } else if ("xhtml".equalsIgnoreCase(contentType)) {
            contentMimeType = HTMLUtil.APPLICATION_XHTML_XML_MIME_TYPE;
        } else if ("xml".equalsIgnoreCase(contentType)) {
            contentMimeType = XMLUtil.APPLICATION_XML_MIME_TYPE;
        } else {
            try {
                contentMimeType = new MimeType(contentType);
            } catch (MimeTypeParseException mtpe) {
            }
        }
        final String contentString = content.getValue();
        if (HTMLUtil.isHTML(contentMimeType)) {
            final String wrappedContentString = "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>" + syndEntry.getUri() + "</title></head><body>" + contentString + "</body></html>";
            final InputSource inputSource = new InputSource(new StringReader(wrappedContentString));
            final TypedInputSource typedInputSource = new TypedInputSource(inputSource, contentMimeType);
            DefaultHandler2 handler = new DOMBuildingHandler((DefaultHandler2) null, contentElement);
            final Map<String, String> replacements = new HashMap<String, String>();
            replacements.put("body", "div");
            handler = new ProxyChannelHTMLSource.ElementReplacementContentHandler(handler, HTMLUtil.HTML_NS_URI, replacements);
            handler = new ProxyChannelHTMLSource.SubDocumentContentHandler(handler, HTMLUtil.HTML_NS_URI, "body");
            try {
                if (XMLUtil.isXML(typedInputSource.getContentType())) {
                    MarkupManager.parseXMLDocument(typedInputSource.getInputSource(), handler, false);
                } else {
                    MarkupManager.parseHTMLDocument(typedInputSource.getInputSource(), handler, false);
                }
            } catch (Exception e) {
                throw new ContentManager.ContentException("Could not parse the content for channel: " + syndEntry.getUri(), e);
            }
        } else {
            DOMUtil.appendText(contentElement, contentString);
        }
        return contentElement;
    }

    @SuppressWarnings("unchecked")
    protected ContentManager.LocalChannelDefinition<StaticChannel> createStaticChannelDefinitionFromEntry(final FeedPageDescriptor feedPageDescriptor, final SyndFeed syndFeed, final ContentManager.ChannelGroupDefinition channelGroupDefinition, final SyndEntry syndEntry, final int entryIndex) throws WWWeeePortal.Exception {
        final String channelID = createID(syndEntry.getUri(), "entry_" + String.valueOf(entryIndex));
        final ContentManager.LocalChannelSpecification<StaticChannel> localChannelSpecification = new ContentManager.LocalChannelSpecification<StaticChannel>(channelGroupDefinition, new RSProperties(this, channelGroupDefinition.getProperties()), channelID, null);
        final RSProperties channelProperties = new RSProperties(this, localChannelSpecification.getProperties());
        if (syndEntry.getTitle() != null) channelProperties.setProp(Channel.TITLE_TEXT_PROP, syndEntry.getTitle(), Locale.ROOT, null);
        if (syndEntry.getUri() != null) channelProperties.setProp("Feed.Entry.URI", syndEntry.getUri(), Locale.ROOT, null);
        if (syndEntry.getAuthor() != null) channelProperties.setProp("Feed.Entry.Author", syndEntry.getAuthor(), Locale.ROOT, null);
        if (syndEntry.getPublishedDate() != null) channelProperties.setProp("Feed.Entry.PublishedDate", ConversionUtil.invokeConverter(DateUtil.ISO_8601_CALENDAR_STRING_FULL_CONVERTER, ConversionUtil.invokeConverter(DateUtil.DATE_UTC_CALENDAR_CONVERTER, syndEntry.getPublishedDate())), Locale.ROOT, null);
        final String alink = syndEntry.getLink();
        if (alink != null) {
            channelProperties.setProp("Feed.Entry.Link", alink, Locale.ROOT, null);
            if (!disableFeedTitleLinks) channelProperties.setProp(ChannelTitle.TITLE_ANCHOR_HREF_PROP, alink, Locale.ROOT, null);
        }
        final List<SyndLink> links = syndEntry.getLinks();
        if (links != null) {
            for (SyndLink link : links) {
                final String name = "Feed.Entry.Link" + ((link.getRel() != null) ? '.' + link.getRel() : "");
                channelProperties.setProp(name, link.getHref(), Locale.ROOT, null);
                if ((!disableFeedTitleLinks) && ("alternate".equalsIgnoreCase(link.getRel()))) {
                    channelProperties.setProp(ChannelTitle.TITLE_ANCHOR_HREF_PROP, link.getHref(), Locale.ROOT, null);
                    channelProperties.setProp(ChannelTitle.TITLE_ANCHOR_REL_PROP, link.getRel(), Locale.ROOT, null);
                }
            }
        }
        Calendar publishedDateTime = ConversionUtil.invokeConverter(DateUtil.DATE_UTC_CALENDAR_CONVERTER, syndEntry.getPublishedDate());
        if (publishedDateTime == null) publishedDateTime = ConversionUtil.invokeConverter(DateUtil.DATE_UTC_CALENDAR_CONVERTER, syndFeed.getPublishedDate());
        if (publishedDateTime != null) {
            channelProperties.setProp(StaticChannel.VIEW_CONTENT_LAST_MODIFIED_BY_PATH_PROP + "^$", ConversionUtil.invokeConverter(DateUtil.RFC_1123_CALENDAR_STRING_CONVERTER, publishedDateTime), Locale.ROOT, null);
        }
        final Locale contentLocale = Locale.ROOT;
        final Document contentDocument = DOMUtil.newDocument();
        final Element documentElement = DOMUtil.createElement(HTMLUtil.HTML_NS_URI, null, "div", contentDocument);
        final List<SyndContent> contents = syndEntry.getContents();
        if (!contents.isEmpty()) {
            int index = -1;
            for (SyndContent content : contents) {
                index++;
                createContentElement(documentElement, syndEntry, content, "feed_content" + ((index > 0) ? '_' + String.valueOf(index + 1) : ""));
            }
        } else {
            final SyndContent description = syndEntry.getDescription();
            createContentElement(documentElement, syndEntry, description, "feed_description");
        }
        final String channelContent = DOMUtil.createLSSerializer(false, true, false).writeToString(contentDocument);
        channelProperties.setProp(StaticChannel.VIEW_CONTENT_VALUE_BY_PATH_PROP + "^$", channelContent, contentLocale, null);
        final ContentManager.LocalChannelDefinition<StaticChannel> channelDefinition = new ContentManager.LocalChannelDefinition<StaticChannel>(localChannelSpecification, channelProperties, feedPageDescriptor.getChannelAccessControl(), StaticChannel.class);
        new ContentManager.ChannelPluginDefinition<ChannelTitle>(channelDefinition, new RSProperties(this, channelDefinition.getProperties()), null, ChannelTitle.class);
        return channelDefinition;
    }

    @SuppressWarnings("unchecked")
    protected ContentManager.PageDefinition<?> createPageDefinitionFromFeed(final FeedPageDescriptor feedPageDescriptor, final SyndFeed syndFeed) throws WWWeeePortal.Exception {
        if (feedPageDescriptor == null) throw new IllegalArgumentException("null feedPageDescriptor");
        if (syndFeed == null) throw new IllegalArgumentException("null syndFeed");
        final ContentManager.PageContentContainer contentContainer = feedPageDescriptor.getPageGroupDefinition().getParentDefinition().duplicate(null, null, null, false, null);
        final ContentManager.PageGroupDefinition pageGroupDefinition = feedPageDescriptor.getPageGroupDefinition().duplicate(contentContainer, null, false, null, false, null, false, null);
        final ContentManager.PageDefinition<?> templatePageDefinition = getTemplatePageDefinition(feedPageDescriptor.getPageKey().getPageGroupID(), feedPageDescriptor.getPageKey().getPageID());
        final RSProperties pageProperties = new RSProperties(this, pageGroupDefinition.getProperties());
        if (templatePageDefinition != null) pageProperties.putAll(templatePageDefinition.getProperties());
        pageProperties.putAll(feedPageDescriptor.getPageProperties());
        if (syndFeed.getUri() != null) pageProperties.setProp("Feed.URI", syndFeed.getUri(), Locale.ROOT, null);
        if (syndFeed.getTitle() != null) pageProperties.setProp("Feed.Title", syndFeed.getTitle(), Locale.ROOT, null);
        if (syndFeed.getDescription() != null) pageProperties.setProp("Feed.Description", syndFeed.getDescription(), Locale.ROOT, null);
        if (syndFeed.getAuthor() != null) pageProperties.setProp("Feed.Author", syndFeed.getAuthor(), Locale.ROOT, null);
        if (syndFeed.getCopyright() != null) pageProperties.setProp("Feed.Copyright", syndFeed.getCopyright(), Locale.ROOT, null);
        if (syndFeed.getLanguage() != null) pageProperties.setProp("Feed.Language", syndFeed.getLanguage(), Locale.ROOT, null);
        if (syndFeed.getPublishedDate() != null) pageProperties.setProp("Feed.PublishedDate", ConversionUtil.invokeConverter(DateUtil.ISO_8601_CALENDAR_STRING_FULL_CONVERTER, ConversionUtil.invokeConverter(DateUtil.DATE_UTC_CALENDAR_CONVERTER, syndFeed.getPublishedDate())), Locale.ROOT, null);
        final String alink = syndFeed.getLink();
        if (alink != null) {
            pageProperties.setProp("Feed.Link", alink, Locale.ROOT, null);
            if (!disableFeedTitleLinks) {
                pageProperties.setProp(PageHeadingChannel.PAGE_TITLE_ANCHOR_HREF_PROP, alink, Locale.ROOT, null);
                if (syndFeed.getTitle() != null) pageProperties.setProp(PageHeadingChannel.PAGE_TITLE_ANCHOR_TITLE_PROP, syndFeed.getTitle(), Locale.ROOT, null);
            }
        }
        final List<SyndLink> links = syndFeed.getLinks();
        if (links != null) {
            for (SyndLink link : links) {
                final String name = "Feed.Link" + ((link.getRel() != null) ? '.' + link.getRel() : "");
                pageProperties.setProp(name, link.getHref(), Locale.ROOT, null);
                if ((!disableFeedTitleLinks) && ("alternate".equalsIgnoreCase(link.getRel()))) {
                    pageProperties.setProp(PageHeadingChannel.PAGE_TITLE_ANCHOR_HREF_PROP, link.getHref(), Locale.ROOT, null);
                    pageProperties.setProp(PageHeadingChannel.PAGE_TITLE_ANCHOR_REL_PROP, link.getRel(), Locale.ROOT, null);
                    if (link.getTitle() != null) {
                        pageProperties.setProp(PageHeadingChannel.PAGE_TITLE_ANCHOR_TITLE_PROP, link.getTitle(), Locale.ROOT, null);
                    } else if (syndFeed.getTitle() != null) {
                        pageProperties.setProp(PageHeadingChannel.PAGE_TITLE_ANCHOR_TITLE_PROP, syndFeed.getTitle(), Locale.ROOT, null);
                    }
                }
            }
        }
        final ContentManager.PageDefinition<Page> pageDefinition = new ContentManager.PageDefinition<Page>(pageGroupDefinition, pageProperties, feedPageDescriptor.getPageKey().getPageID(), feedPageDescriptor.getPageAccessControl(), Page.class);
        final List<SyndEntry> entries = syndFeed.getEntries();
        ContentManager.ChannelSpecification<?> placeholderChannel = null;
        if (templatePageDefinition != null) {
            final List<ContentManager.ChannelSpecification<?>> templateChannels = templatePageDefinition.getChannelSpecifications();
            if (templateChannels != null) {
                ContentManager.ChannelGroupDefinition currentTargetGroup = null;
                for (ContentManager.ChannelSpecification<?> templateChannel : templateChannels) {
                    if ("feed_entries".equals(templateChannel.getID())) {
                        placeholderChannel = templateChannel;
                        break;
                    }
                    final ContentManager.ChannelGroupDefinition templateGroup = templateChannel.getParentDefinition();
                    if ((currentTargetGroup == null) || (!currentTargetGroup.getID().equals(templateGroup.getID()))) {
                        currentTargetGroup = templateGroup.duplicate(pageDefinition, null, false, null);
                    }
                    templateChannel.duplicate(currentTargetGroup, null);
                }
            }
        }
        final ContentManager.ChannelGroupDefinition targetChannelGroup;
        if (placeholderChannel != null) {
            final ContentManager.ChannelGroupDefinition existingTargetGroup = pageDefinition.getChildDefinition(placeholderChannel.getParentDefinition().getID());
            targetChannelGroup = (existingTargetGroup != null) ? existingTargetGroup : placeholderChannel.getParentDefinition().duplicate(pageDefinition, null, false, null);
        } else {
            final ContentManager.ChannelGroupDefinition existingTargetGroup = pageDefinition.getChildDefinition("body");
            targetChannelGroup = (existingTargetGroup != null) ? existingTargetGroup : new ContentManager.ChannelGroupDefinition(pageDefinition, new RSProperties(this, pageDefinition.getProperties()), "body", null);
        }
        int entryIndex = -1;
        for (SyndEntry entry : entries) {
            entryIndex++;
            if (feedPageDescriptor.createProxyChannels()) {
                createProxyChannelDefinitionFromEntry(feedPageDescriptor, syndFeed, targetChannelGroup, entry, entryIndex);
            } else {
                createStaticChannelDefinitionFromEntry(feedPageDescriptor, syndFeed, targetChannelGroup, entry, entryIndex);
            }
        }
        if (templatePageDefinition != null) {
            final List<ContentManager.ChannelSpecification<?>> templateChannels = templatePageDefinition.getChannelSpecifications();
            if (templateChannels != null) {
                ContentManager.ChannelGroupDefinition currentTargetGroup = null;
                boolean afterPlaceholder = false;
                for (ContentManager.ChannelSpecification<?> templateChannel : templateChannels) {
                    if (!afterPlaceholder) {
                        if ("feed_entries".equals(templateChannel.getID())) afterPlaceholder = true;
                        continue;
                    }
                    final ContentManager.ChannelGroupDefinition templateGroup = templateChannel.getParentDefinition();
                    if ((currentTargetGroup == null) || (!currentTargetGroup.getID().equals(templateGroup.getID()))) {
                        currentTargetGroup = templateGroup.duplicate(pageDefinition, null, false, null);
                    }
                    templateChannel.duplicate(currentTargetGroup, null);
                }
            }
        }
        contentContainer.init();
        return pageDefinition;
    }

    protected SyndFeed retrieveFeed(final URL feedURL) throws WWWeeePortal.Exception {
        if (feedURL == null) throw new IllegalArgumentException("null feedURL");
        try {
            if ("http".equalsIgnoreCase(feedURL.getProtocol())) {
                final FeedFetcher feedFetcher = new HttpURLFeedFetcher(fetcherCache);
                return feedFetcher.retrieveFeed(feedURL);
            }
            final SyndFeedInput input = new SyndFeedInput();
            return input.build(new XmlReader(feedURL));
        } catch (IOException ioe) {
            throw LogAnnotation.annotate(new WWWeeePortal.OperationalException(ioe), "FeedURL", feedURL, null, false);
        } catch (FeedException fe) {
            throw LogAnnotation.annotate(new ContentManager.ContentException(fe.getMessage(), fe), "FeedURL", feedURL, null, false);
        } catch (FetcherException fe) {
            throw LogAnnotation.annotate(new WWWeeePortal.OperationalException(fe), "FeedURL", feedURL, null, false);
        }
    }

    protected Calendar getExpires(final SyndFeed syndFeed) {
        if (syndFeed == null) throw new IllegalArgumentException("null syndFeed");
        final int intervalMinutes;
        final SyModule syModule = (SyModule) syndFeed.getModule(SyModule.URI);
        if (syModule != null) {
            String updatePeriod = syModule.getUpdatePeriod();
            if (updatePeriod == null) updatePeriod = SyModule.DAILY;
            int updateFrequency = syModule.getUpdateFrequency();
            if (updateFrequency <= 0) updateFrequency = 1;
            if (SyModule.HOURLY.equalsIgnoreCase(updatePeriod)) {
                intervalMinutes = Math.round(60 / updateFrequency);
            } else if (SyModule.DAILY.equalsIgnoreCase(updatePeriod)) {
                intervalMinutes = Math.round(1440 / updateFrequency);
            } else if (SyModule.WEEKLY.equalsIgnoreCase(updatePeriod)) {
                intervalMinutes = Math.round(10080 / updateFrequency);
            } else if (SyModule.MONTHLY.equalsIgnoreCase(updatePeriod)) {
                intervalMinutes = Math.round(44640 / updateFrequency);
            } else if (SyModule.YEARLY.equalsIgnoreCase(updatePeriod)) {
                intervalMinutes = Math.round(525600 / updateFrequency);
            } else {
                intervalMinutes = defaultRefreshIntervalMinutes;
            }
        } else {
            intervalMinutes = defaultRefreshIntervalMinutes;
        }
        final Calendar expires = new GregorianCalendar(DateUtil.UTC_TIME_ZONE);
        expires.add(Calendar.MINUTE, intervalMinutes);
        return expires;
    }

    protected FeedPageDefinition fetchFeedPageDefinition(final FeedPageDescriptor feedPageDescriptor, final FeedPageDefinition cachedFeedPageDefinition) throws WWWeeePortal.Exception {
        if (feedPageDescriptor == null) throw new IllegalArgumentException("null feedPageDescriptor");
        final SyndFeed syndFeed = retrieveFeed(feedPageDescriptor.getFeedURL());
        if (cachedFeedPageDefinition != null) {
            if (syndFeed == cachedFeedPageDefinition.getSyndFeed()) {
                return new FeedPageDefinition(feedPageDescriptor, cachedFeedPageDefinition.getPageDefinition(), syndFeed, getExpires(syndFeed));
            }
            final Date cachedPublishedDate = cachedFeedPageDefinition.getSyndFeed().getPublishedDate();
            final Date publishedDate = syndFeed.getPublishedDate();
            if ((cachedPublishedDate != null) && (publishedDate != null) && (publishedDate.equals(cachedPublishedDate))) {
                return new FeedPageDefinition(feedPageDescriptor, cachedFeedPageDefinition.getPageDefinition(), syndFeed, getExpires(syndFeed));
            }
        }
        final ContentManager.PageDefinition<?> pageDefinition = createPageDefinitionFromFeed(feedPageDescriptor, syndFeed);
        final Calendar expires = getExpires(syndFeed);
        return new FeedPageDefinition(feedPageDescriptor, pageDefinition, syndFeed, expires);
    }

    protected FeedPageDefinition getFeedPageDefinition(final ContentManager.PageDefinition.Key pageKey) throws WWWeeePortal.Exception {
        FeedPageDefinition cachedFeedPageDefinition = getCachedFeedPageDefinition(feedPageDefinitionCache, pageKey);
        if (cachedFeedPageDefinition != null) {
            if (scheduledExecutorService != null) {
                return cachedFeedPageDefinition;
            }
            synchronized (cachedFeedPageDefinition.getFeedPageDescriptor()) {
                cachedFeedPageDefinition = getCachedFeedPageDefinition(feedPageDefinitionCache, pageKey);
                final Calendar currentTime = new GregorianCalendar(DateUtil.UTC_TIME_ZONE);
                if (cachedFeedPageDefinition.getExpires().after(currentTime)) {
                    return cachedFeedPageDefinition;
                }
                final FeedPageDefinition newFeedPageDefinition = fetchFeedPageDefinition(cachedFeedPageDefinition.getFeedPageDescriptor(), cachedFeedPageDefinition);
                cacheFeedPageDefinition(feedPageDefinitionCache, pageCache, newFeedPageDefinition);
                return newFeedPageDefinition;
            }
        }
        final FeedPageDescriptor feedPageDescriptor = getFeedPageDescriptor(pageKey);
        if ((feedPageDescriptor == null) || (feedPageDescriptor.getFeedURL() == null)) {
            return null;
        }
        synchronized (feedPageDescriptor) {
            cachedFeedPageDefinition = getCachedFeedPageDefinition(feedPageDefinitionCache, pageKey);
            if (cachedFeedPageDefinition != null) {
                return cachedFeedPageDefinition;
            }
            final FeedPageDefinition newFeedPageDefinition = fetchFeedPageDefinition(feedPageDescriptor, null);
            cacheFeedPageDefinition(feedPageDefinitionCache, pageCache, newFeedPageDefinition);
            return newFeedPageDefinition;
        }
    }

    @Override
    public List<ContentManager.PageDefinition<?>> getPageDefinitions(final Map<String, String> withMatchingPageContainerProps, final Map<String, String> withoutMatchingPageContainerProps, final Map<String, String> withMatchingPageGroupProps, final Map<String, String> withoutMatchingPageGroupProps, final Map<String, String> withMatchingPageProps, final Map<String, String> withoutMatchingPageProps, final boolean checkAccessControl, final UriInfo uriInfo, final SecurityContext securityContext, final HttpHeaders httpHeaders, final boolean channelDefinitionsRequired, final int startIndex, final int count) throws WWWeeePortal.Exception {
        final List<FeedPageDescriptor> feedPageDescriptors = getFeedPageDescriptors(securityContext);
        if (feedPageDescriptors == null) return null;
        final ArrayList<ContentManager.PageDefinition<?>> pageDefinitions = new ArrayList<ContentManager.PageDefinition<?>>(feedPageDescriptors.size());
        for (FeedPageDescriptor feedPageDescriptor : feedPageDescriptors) {
            if ((startIndex >= 0) && (pageDefinitions.size() < startIndex)) {
                pageDefinitions.add(null);
                if ((count >= 0) && (startIndex >= 0) && (pageDefinitions.size() >= startIndex + count)) break;
                continue;
            }
            final FeedPageDefinition feedPageDefinition = getFeedPageDefinition(feedPageDescriptor.getPageKey());
            pageDefinitions.add(feedPageDefinition.getPageDefinition());
            if ((count >= 0) && (startIndex >= 0) && (pageDefinitions.size() >= startIndex + count)) break;
        }
        if (pageDefinitions.isEmpty()) return null;
        pageDefinitions.trimToSize();
        return pageDefinitions;
    }

    @Override
    public ContentManager.PageDefinition<?> getPageDefinition(final ContentManager.PageDefinition.Key pageKey, final UriInfo uriInfo, final SecurityContext securityContext, final HttpHeaders httpHeaders, final boolean channelDefinitionsRequired) throws WWWeeePortal.Exception {
        final FeedPageDefinition feedPageDefinition = getFeedPageDefinition(pageKey);
        return (feedPageDefinition != null) ? feedPageDefinition.getPageDefinition() : null;
    }

    @Override
    public Page getPage(final ContentManager.PageDefinition.Key pageKey, final UriInfo uriInfo, final SecurityContext securityContext, final HttpHeaders httpHeaders) throws WWWeeePortal.Exception {
        final FeedPageDefinition feedPageDefinition = getFeedPageDefinition(pageKey);
        if (feedPageDefinition == null) return null;
        Page cachedPage = pageCache.get(feedPageDefinition);
        if (cachedPage != null) {
            return cachedPage;
        }
        synchronized (feedPageDefinition) {
            cachedPage = pageCache.get(feedPageDefinition);
            if (cachedPage != null) {
                return cachedPage;
            }
            final Page newPage = feedPageDefinition.getPageDefinition().newInstance(getPortal());
            newPage.init();
            pageCache.put(feedPageDefinition, newPage);
            return newPage;
        }
    }

    /**
   * A feed page description based on a Web Feed.
   */
    protected interface FeedPageDescriptor {

        public ContentManager.PageDefinition.Key getPageKey();

        public ContentManager.PageGroupDefinition getPageGroupDefinition();

        public RSProperties getPageProperties();

        public URL getFeedURL();

        public RSAccessControl getPageAccessControl();

        public RSAccessControl getChannelAccessControl();

        public boolean createProxyChannels();
    }

    /**
   * Manages and updates a {@link org.wwweeeportal.portal.ContentManager.PageDefinition} based on a
   * {@link com.sun.syndication.feed.synd.SyndFeed Feed}.
   */
    protected class FeedPageDefinition implements Runnable {

        protected final FeedPageDescriptor feedPageDescriptor;

        protected final ContentManager.PageDefinition<?> pageDefinition;

        protected final SyndFeed syndFeed;

        protected final Calendar expires;

        protected final ScheduledFuture<?> scheduledFuture;

        public FeedPageDefinition(final FeedPageDescriptor feedPageDescriptor, final ContentManager.PageDefinition<?> pageDefinition, final SyndFeed syndFeed, final Calendar expires) throws WWWeeePortal.Exception {
            if (feedPageDescriptor == null) throw new IllegalArgumentException("null feedPageDescriptor");
            if (pageDefinition == null) throw new IllegalArgumentException("null pageDefinition");
            if (syndFeed == null) throw new IllegalArgumentException("null syndFeed");
            if (expires == null) throw new IllegalArgumentException("null expires");
            this.feedPageDescriptor = feedPageDescriptor;
            this.pageDefinition = pageDefinition;
            this.syndFeed = syndFeed;
            this.expires = expires;
            if (scheduledExecutorService != null) {
                scheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(this, defaultRefreshIntervalMinutes, defaultRefreshIntervalMinutes, TimeUnit.MINUTES);
            } else {
                scheduledFuture = null;
            }
            return;
        }

        public ContentManager.PageDefinition.Key getPageKey() {
            return feedPageDescriptor.getPageKey();
        }

        public FeedPageDescriptor getFeedPageDescriptor() {
            return feedPageDescriptor;
        }

        public ContentManager.PageDefinition<?> getPageDefinition() {
            return pageDefinition;
        }

        public SyndFeed getSyndFeed() {
            return syndFeed;
        }

        public Calendar getExpires() {
            return expires;
        }

        public ScheduledFuture<?> getScheduledFuture() {
            return scheduledFuture;
        }

        @Override
        public boolean equals(final Object otherObject) {
            if (otherObject == null) return false;
            if (!(otherObject instanceof FeedPageDefinition)) return false;
            final FeedPageDefinition otherFeedPage = (FeedPageDefinition) otherObject;
            return feedPageDescriptor.equals(otherFeedPage.getFeedPageDescriptor());
        }

        @Override
        public int hashCode() {
            return feedPageDescriptor.hashCode();
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + '#' + feedPageDescriptor;
        }

        @Override
        public void run() {
            synchronized (feedPageDescriptor) {
                try {
                    try {
                        final FeedPageDefinition newFeedPageDefinition = fetchFeedPageDefinition(feedPageDescriptor, this);
                        cacheFeedPageDefinition(feedPageDefinitionCache, pageCache, newFeedPageDefinition);
                    } catch (WWWeeePortal.Exception wpe) {
                        throw wpe;
                    } catch (Exception e) {
                        throw new WWWeeePortal.SoftwareException(e);
                    }
                } catch (WWWeeePortal.Exception wpe) {
                    @SuppressWarnings("synthetic-access") final Logger logger = getLogger();
                    LogAnnotation.log(logger, LogAnnotation.annotate(wpe, "FeedPageDescriptor", feedPageDescriptor, null, false), getClass(), wpe);
                }
                return;
            }
        }
    }
}
