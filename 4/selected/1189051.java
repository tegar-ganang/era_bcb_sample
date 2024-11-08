package net.sf.woko.facets.view;

import net.sourceforge.jfacets.IFacetContext;
import net.sourceforge.jfacets.annotations.FacetKey;
import net.sourceforge.jfacets.web.WebFacets;
import net.sf.woko.facets.BaseFacet;
import net.sf.woko.facets.IFragmentFacet;
import net.sf.woko.facets.FacetConstants;
import net.sf.woko.util.Util;
import net.sf.woko.usermgt.IWokoUserManager;

/**
 * Facet used for RSS2 feeds. Allows to create and display RSS feeds 
 * for objects !
 * <br/><br/>
 * <b>Assignation details :</b>
 * <ul>
 * <li><b>name</b> : rssFeed</li>
 * <li><b>profileId</b> : ROLE_ALL</li>
 * <li><b>targetObjectType</b> : Object</li>
 * </ul>
 */
@FacetKey(name = FacetConstants.rssFeed, profileId = IWokoUserManager.ROLE_ALL)
public class RssFeed extends BaseFacet implements IFragmentFacet {

    private String title;

    private String type;

    @Override
    public void setContext(IFacetContext context) {
        super.setContext(context);
        WebFacets wf = WebFacets.get(getRequest());
        ViewObjectTitle vt = (ViewObjectTitle) wf.getFacet(FacetConstants.viewTitle, getTargetObject(), getRequest());
        title = vt.getTitle();
        type = vt.getType();
    }

    /**
	 * Return true if the object has an associated feed. 
	 * This implementation returns all Feedable objects.
	 */
    public Boolean getShowFeed() {
        return Util.isFeedable(getTargetObject());
    }

    /**
	 * Helper method used to construct links to feeds
	 */
    public String getFeedObjectId() {
        return getPersistenceUtil().getId(getTargetObject());
    }

    /**
	 * Helper method used to construct links to feeds
	 */
    public String getFeedObjectClass() {
        return getTargetObject().getClass().getName();
    }

    /**
	 * Return the title of the RSS channel
	 */
    public String getChannelTitle() {
        return "Woko feed - " + title + " (" + type + ")";
    }

    /**
	 * Return the target object's title
	 */
    public String getTitle() {
        return title;
    }

    /**
	 * Return the target object's type
	 */
    public String getType() {
        return type;
    }

    /**
	 * Return the path to the JSP to be used for creating the feed : 
	 * <code>/WEB-INF/woko/feed.jsp</code>
	 */
    public String getFragmentPath() {
        return "/WEB-INF/woko/feed.jsp";
    }
}
