package com.germinus.xpression.portlet.cms.rss;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.portlet.PortletPreferences;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.germinus.portlet.advanced_content_menu.model.AdvancedContentMenuPreferences;
import com.germinus.portlet.advanced_content_menu.model.BaseAdvancedContentMenuFilter;
import com.germinus.portlet.advanced_content_menu.model.FilterResult;
import com.germinus.xpression.cms.CMSQuery;
import com.germinus.xpression.cms.CMSResult;
import com.germinus.xpression.cms.contents.ContentIF;
import com.germinus.xpression.cms.contents.ContentNotFoundException;
import com.germinus.xpression.cms.service.SelectedContentsService;
import com.germinus.xpression.cms.util.ManagerRegistry;
import com.germinus.xpression.groupware.GroupwareUser;
import com.germinus.xpression.groupware.NotAuthorizedException;
import com.germinus.xpression.groupware.communities.Community;
import com.germinus.xpression.groupware.communities.CommunityNotFoundException;
import com.germinus.xpression.groupware.util.GroupwareManagerRegistry;
import com.germinus.xpression.portlet.SearchCriteriaPreferences;
import com.germinus.xpression.portlet.cms.CmsToolsConfig;
import de.nava.informa.core.ChannelExporterIF;
import de.nava.informa.core.ChannelIF;

/**
 * PublishManager is a class to manage RSSDocument creation
 *
 * @author gruiz
 */
public class PublishManager {

    private static Log log = LogFactory.getLog(PublishManager.class);

    private SelectedContentsService service = new SelectedContentsService();

    private String maxRSSItems;

    private List<ContentIF> contents;

    private String title;

    private String showDescription;

    private String portletType;

    private RSSUrl rssParams;

    /**
     * Create a Publishmanager instance associated with a Portlet. Portlet preferences
     * are required to get Portlet contents to export in a RSSDocument
     * @param params 
     *
     * @param prefs Portlet preferences
     * @param layoutId Portlet layout Id
     * @param portletId Portlet Id
     * @param companyId Portlet company Id
     * @throws ContentNotFoundException
     * @throws NotAuthorizedException
     * @throws Exception
     */
    public PublishManager(RSSUrl params, GroupwareUser user, PortletPreferences prefs, String encoding, Long plid) throws NotAuthorizedException, Exception {
        this.rssParams = params;
        this.portletType = params.getPortletType();
        log.info("PortletType: " + this.portletType);
        this.contents = getContentsFromPrefs(user, params.getPortletId(), prefs, params.getLayoutId(), plid);
    }

    private List<ContentIF> getContentsFromPrefs(GroupwareUser user, String portletId, PortletPreferences prefs, long layoutId, Long plid) throws NotAuthorizedException, CommunityNotFoundException {
        List<ContentIF> result;
        this.title = prefs.getValue("title", "");
        this.maxRSSItems = prefs.getValue("maxRSSItems", "10");
        this.showDescription = prefs.getValue("showDescription", "true");
        if (portletType.equals("several_contents")) {
            result = service.getVisibleSelectedContents(plid, portletId, user.getCompanyId());
        } else if (portletType.equals("stored_search")) {
            result = contentsFromStoredSearch(user, prefs);
        } else if (portletType.equals("advanced_content_menu")) {
            AdvancedContentMenuPreferences acmPreferences = new AdvancedContentMenuPreferences(prefs, null);
            String filterId = rssParams.getMorePath().get(0);
            BaseAdvancedContentMenuFilter filter = acmPreferences.getFilter(filterId);
            filter.setPageSize(this.maxRSSItems);
            this.title = filter.getTitle();
            try {
                FilterResult filterResults = filter.getFilterResults(1, false);
                result = filterResults.getContentsFilteredByUser();
            } catch (ContentNotFoundException e) {
                log.error("Error generating RSS document for ACM. Content Not Found: " + e);
                result = new ArrayList<ContentIF>();
            }
        } else {
            log.debug("Not valid portlet to generate a RSS Document ");
            result = new ArrayList<ContentIF>();
        }
        if (log.isDebugEnabled()) {
            log.debug("Contents from prefs " + prefs + " are " + contents);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<ContentIF> contentsFromStoredSearch(GroupwareUser user, PortletPreferences prefs) throws CommunityNotFoundException {
        List<ContentIF> result;
        SearchCriteriaPreferences searchCriteriaPreferences = new SearchCriteriaPreferences(prefs);
        if (searchCriteriaPreferences.isNotEmptyCriteria()) {
            if (StringUtils.isEmpty(searchCriteriaPreferences.getWorldId())) {
                Community searchCommunity = GroupwareManagerRegistry.getCommunityManager().getPersonalCommunity(user);
                String searchWorld = searchCommunity.getWorld().getId();
                searchCriteriaPreferences.setWorldId(searchWorld);
            }
            CMSQuery cmsQuery = searchCriteriaPreferences.generateQuery();
            cmsQuery.setPageSize(new Integer(this.maxRSSItems));
            CMSResult cmsResult = ManagerRegistry.getContentManager().searchContents(cmsQuery);
            result = (List<ContentIF>) cmsResult.getItems();
        } else {
            result = new ArrayList<ContentIF>();
        }
        return result;
    }

    public List<ContentIF> getContents() throws Exception {
        return contents;
    }

    public String getTitle() throws Exception {
        return title;
    }

    /**
     * Returns a String, with portlet contents in a RSS document with
     * XML structure
     */
    public String getContentsRSS() throws Exception {
        String documentoRSS;
        RSSBuilder doc = new RSSBuilder();
        if (log.isDebugEnabled()) log.debug("RSSBuilder object created");
        doc.setTitle(title);
        if (log.isDebugEnabled()) log.debug("Title and description established");
        int maxItems = Integer.parseInt(maxRSSItems);
        if (log.isDebugEnabled()) log.debug("There are " + contents.size() + " items");
        if (contents.size() < maxItems) {
            maxItems = contents.size();
        }
        for (int i = 0; i < maxItems; i++) {
            if (log.isDebugEnabled()) {
                log.debug("Elemento iterado : " + i);
                log.debug("Longitud contents : " + contents.size());
                log.debug("Contenido i : " + contents.get(i));
            }
            ContentIF item = (ContentIF) contents.get(i);
            doc.addItem(item.getName(), getDescription(item), item.getPublicationDate(), item.getCategories(), this.rssParams.getContentLink(item.getId(), item.getVersion()));
        }
        doc.setType(getRSSVersion());
        ChannelIF canal = doc.getChannelDocument();
        if (log.isDebugEnabled()) {
            log.debug("RSS type of channel is: " + canal.getFormat());
            log.debug("Channel generated");
            log.debug("Exporter generated");
        }
        StringWriter buffer = new StringWriter();
        ChannelExporterIF exporter = doc.getExporter(buffer, "UTF-8");
        if (log.isDebugEnabled()) log.debug("Exporter class is: " + exporter.getClass().getName());
        exporter.write(canal);
        documentoRSS = buffer.toString();
        return documentoRSS;
    }

    private String getDescription(ContentIF item) {
        String description;
        if (log.isDebugEnabled()) log.debug("Show Description is: " + showDescription);
        if (showDescription.equals("true")) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Description is established");
                    log.debug("Content class is: " + item.getContentData().getClass().toString());
                }
                description = item.getBriefDesc();
                if (log.isDebugEnabled()) log.debug("Description is: " + description);
            } catch (Exception e) {
                log.debug("Error: " + e);
                description = "";
            }
        } else {
            if (log.isDebugEnabled()) log.debug("Description is not established");
            description = "";
        }
        return description;
    }

    /**
     * Return a RSS Document as a String, with error messages when
     * a exception is thrown in the RSS Document generation proccess
     */
    public static String getErrorRSSDocument(Exception error) throws Exception {
        String documentoRSS;
        RSSBuilder doc = new RSSBuilder();
        String errorName = error.getClass().getName();
        if (errorName.equals(ContentNotFoundException.class.getName())) {
            doc.setTitle(CmsToolsConfig.getRSSErrorTitle(errorName));
            doc.setDescription(CmsToolsConfig.getRSSErrorMessage(errorName));
        } else if (errorName.equals(NotAuthorizedException.class.getName())) {
            doc.setTitle(CmsToolsConfig.getRSSErrorTitle(errorName));
            doc.setDescription(CmsToolsConfig.getRSSErrorMessage(errorName));
        } else {
            doc.setTitle(CmsToolsConfig.getRSSErrorTitle(errorName));
            doc.setDescription(CmsToolsConfig.getRSSErrorMessage(errorName));
        }
        doc.setType(getRSSVersion());
        ChannelIF canal = doc.getChannelDocument();
        if (log.isDebugEnabled()) {
            log.debug("Channel generated");
            log.debug("Exporter generated");
        }
        StringWriter buffer = new StringWriter();
        ChannelExporterIF exporter = doc.getExporter(buffer, "iso-8859-1");
        exporter.write(canal);
        documentoRSS = buffer.toString();
        return documentoRSS;
    }

    private static String getRSSVersion() {
        return CmsToolsConfig.getRSSVersion();
    }
}
