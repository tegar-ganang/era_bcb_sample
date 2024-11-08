package org.apache.jetspeed.portal.portlets.admin;

import org.apache.ecs.html.A;
import org.apache.ecs.html.Center;
import org.apache.ecs.html.Form;
import org.apache.ecs.html.Input;
import org.apache.ecs.html.Option;
import org.apache.ecs.html.P;
import org.apache.ecs.html.Select;
import org.apache.ecs.html.Table;
import org.apache.ecs.html.TextArea;
import org.apache.ecs.html.TD;
import org.apache.ecs.html.TR;
import org.apache.ecs.ConcreteElement;
import org.apache.ecs.ElementContainer;
import org.apache.ecs.StringElement;
import org.apache.jetspeed.portal.PortletException;
import org.apache.jetspeed.portal.portlets.AbstractPortlet;
import org.apache.jetspeed.cache.disk.DiskCacheEntry;
import org.apache.jetspeed.cache.disk.JetspeedDiskCache;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.xml.api.jcm.Content;
import org.apache.jetspeed.xml.api.jcm.Entry;
import org.apache.jetspeed.xml.api.jcm.Item;
import org.apache.turbine.util.ParameterParser;
import org.apache.turbine.util.DynamicURI;
import org.apache.turbine.util.RunData;
import org.apache.jetspeed.services.resources.JetspeedResources;
import java.io.Reader;
import java.io.Writer;
import java.util.Hashtable;
import java.util.Vector;

/**
Handles enumerating Portlets that are also applications

@author <a href="mailto:burton@apache.org">Kevin A. Burton</a>
@version $Id: JetspeedContentAdmin.java,v 1.28 2004/02/23 03:26:19 jford Exp $ 
*/
public class JetspeedContentAdmin extends AbstractPortlet {

    /**
     * Static initialization of the logger for this class
     */
    private static final JetspeedLogger logger = JetspeedLogFactoryService.getLogger(JetspeedContentAdmin.class.getName());

    /**
    Used as the provider name key
    */
    public static final String PROVIDER_NAME_KEY = "provider-name";

    public static final String POST_ARTICLE = "Post Article";

    private Hashtable content = new Hashtable();

    /**
    Get the content for this JCP Admin
    */
    public ConcreteElement getContent(RunData rundata) {
        ParameterParser params = rundata.getParameters();
        String provider = params.getString(PROVIDER_NAME_KEY);
        if (provider == null) {
            return this.getProviders(rundata);
        } else if (params.getString(POST_ARTICLE) != null) {
            params.remove(PROVIDER_NAME_KEY);
            params.remove(POST_ARTICLE);
            return this.postArticle(provider, rundata);
        } else {
            params.remove(PROVIDER_NAME_KEY);
            return this.getForm(provider, rundata);
        }
    }

    /**
    */
    public String getURL(String provider) {
        return JetspeedResources.getString("content.provider." + provider + ".url");
    }

    /**
    */
    public synchronized ConcreteElement postArticle(String provider, RunData rundata) {
        ElementContainer ec = new ElementContainer();
        ParameterParser params = rundata.getParameters();
        String topic = params.getString("topic", "");
        String title = params.getString("title", "");
        String link = params.getString("link", "");
        String description = params.getString("description", "");
        Item item = new Item();
        item.setTopic(topic);
        item.setTitle(title);
        item.setLink(link);
        item.setDescription(description);
        Content content = null;
        try {
            content = this.getContentMarkup(this.getURL(provider)).getContent();
            Vector v = new Vector();
            Item[] items = content.getChannel().getItem();
            for (int i = 0; i < items.length; ++i) {
                v.addElement(items[i]);
            }
            v.insertElementAt(item, 0);
            Item[] newItems = new Item[v.size()];
            v.copyInto(newItems);
            content.getChannel().removeAllItem();
            for (int i = 0; i < newItems.length; ++i) {
                content.getChannel().addItem(newItems[i]);
            }
            this.getContentMarkup(this.getURL(provider)).save();
        } catch (Throwable t) {
            logger.error("Throwable", t);
            return new StringElement("Can't use this provider: " + t.getMessage());
        }
        ec.addElement("Your article '" + title + "' has been posted within '" + topic + "'");
        return ec;
    }

    /**
    Get a list of providers an provide a form for them.
    */
    public ConcreteElement getProviders(RunData rundata) {
        ElementContainer root = new ElementContainer();
        root.addElement(new P().addElement("Select a content provider: "));
        Vector v = JetspeedResources.getVector(JetspeedResources.CONTENT_PROVIDER_LIST_KEY);
        for (int i = 0; i < v.size(); ++i) {
            String provider = (String) v.elementAt(i);
            String title = JetspeedResources.getString("content.provider." + provider + ".title");
            DynamicURI uri = new DynamicURI(rundata);
            uri.addQueryData(rundata.getParameters());
            uri.addQueryData(PROVIDER_NAME_KEY, provider);
            P row = new P().addElement(new A(uri.toString()).addElement(title)).addElement(" ( ").addElement(new A(this.getBookmarklet(provider, rundata)).addElement("Bookmarklet")).addElement(" ) ");
            root.addElement(row);
        }
        return root;
    }

    /**
    Return a form that refresh the feed daemon
    
    @param provider The provider that you want to publish content to.
    */
    private ConcreteElement getForm(String provider, RunData rundata) {
        DynamicURI duri = new DynamicURI(rundata);
        Form form = new Form().setAction(duri.toString());
        Table table = new Table().setBorder(0);
        form.addElement(table);
        ParameterParser params = rundata.getParameters();
        String topic = params.getString("topic", "");
        String title = params.getString("title", "");
        String link = params.getString("link", "");
        String description = params.getString("description", "");
        Content content = null;
        try {
            content = this.getContentMarkup(this.getURL(provider)).getContent();
        } catch (Exception e) {
            logger.error("Exception", e);
            return new StringElement("Can't use this provider: " + e.getMessage());
        }
        Select select = new Select();
        select.setName("topic");
        Entry[] topics = content.getChannel().getTopics().getEntry();
        for (int i = 0; i < topics.length; ++i) {
            String name = topics[i].getName();
            select.addElement(new Option(name).addElement(name));
        }
        table.addElement(getRow("Topic: ", select));
        table.addElement(getRow("Title: ", new Input().setType("text").setName("title").setValue(title)));
        table.addElement(getRow("Link: ", new Input().setType("text").setName("link").setValue(link)));
        table.addElement(new TR().addElement(new TD().setColSpan(2).addElement(new TextArea().setName("description").setCols(65).setRows(15).addElement(description))));
        form.addElement(new Input().setType("hidden").setName(PROVIDER_NAME_KEY).setValue(provider));
        form.addElement(new Input().setType("submit").setName(POST_ARTICLE).setValue(POST_ARTICLE));
        return new Center(form);
    }

    /**
    Get a row for adding it to a table.
    */
    private TR getRow(String title, ConcreteElement ce) {
        int TITLE_WIDTH = 30;
        TR tr = new TR().addElement(new TD().setWidth(TITLE_WIDTH).addElement(title)).addElement(new TD().addElement(ce));
        return tr;
    }

    /**
    Get the jetspeed content from disk.
    */
    public ContentMarkup getContentMarkup(String url) throws Exception {
        ContentMarkup cm = (ContentMarkup) this.content.get(url);
        if (cm == null) {
            cm = new ContentMarkup(url);
            this.content.put(url, cm);
        }
        return cm;
    }

    /**
    Init this 
    */
    public void init() throws PortletException {
        this.setTitle("Jetspeed Content");
        this.setDescription("Publish Jetspeed Content.");
    }

    /**
    Get the content of a bookmarklet for the given provider.
    */
    private String getBookmarklet(String provider, RunData rundata) {
        ParameterParser params = rundata.getParameters();
        StringBuffer buff = new StringBuffer();
        buff.append("JavaScript:");
        buff.append("top.location = '" + rundata.getServerScheme() + "://" + rundata.getServerName() + (rundata.getServerPort() != 80 ? ":" + rundata.getServerPort() : "") + rundata.getScriptName() + "?select-panel=JetspeedContentAdmin");
        buff.append("&provider-name=" + provider);
        buff.append("&title=' + escape( document.title ) + '&link=' + escape( top.location );");
        return buff.toString();
    }

    /**
    */
    public boolean getAllowEdit(RunData rundata) {
        return false;
    }

    /**
    */
    public boolean getAllowMaximize(RunData rundata) {
        return false;
    }
}

/**
Class for abstracting Jetspeed content markup parsing.  Handles updating the
content if it has been modified on disk.

@author <a href="mailto:burton@apache.org">Kevin A. Burton</a>
@version $Id: JetspeedContentAdmin.java,v 1.28 2004/02/23 03:26:19 jford Exp $ 
*/
class ContentMarkup {

    /**
    The last modified date of this URL,
    */
    private long lastModified;

    /**
    The Castor generated API.
    */
    private Content content = null;

    /**
    The URL on which the JCM is based.
    */
    private String url = null;

    /**
    Create a ContentMarkup and point it to the URL you want.
    */
    public ContentMarkup(String url) throws Exception {
        System.err.println("Content Markup url => " + url);
        this.url = url;
        this.lastModified = JetspeedDiskCache.getInstance().getEntry(this.url).getLastModified();
        this.parse();
    }

    /**
    Get the Castor generated content for this ContentMarkup
    */
    public Content getContent() throws Exception {
        long recent = JetspeedDiskCache.getInstance().getEntry(this.url).getLastModified();
        if (recent == 0 || this.lastModified < recent) {
            this.parse();
        }
        return this.content;
    }

    /**
    Save this JCM (hopefully updated JCM) to disk.
    */
    public synchronized void save() throws Exception {
        DiskCacheEntry pde = JetspeedDiskCache.getInstance().getEntry(this.url);
        Writer filewriter = pde.getWriter();
        this.content.marshal(filewriter);
        filewriter.close();
    }

    /**
    Parse out the JCM and store it in memory
    */
    public synchronized void parse() throws Exception {
        Reader stream = JetspeedDiskCache.getInstance().getEntry(this.url).getReader();
        this.content = Content.unmarshal(stream);
        stream.close();
    }
}
