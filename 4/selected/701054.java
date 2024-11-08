package com.germinus.xpression.portlet.cms.rss;

import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import de.nava.informa.core.CategoryIF;
import de.nava.informa.core.ChannelExporterIF;
import de.nava.informa.core.ChannelFormat;
import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ItemIF;
import de.nava.informa.exporters.RSS_0_91_Exporter;
import de.nava.informa.exporters.RSS_1_0_Exporter;
import de.nava.informa.exporters.RSS_2_0_Exporter;
import de.nava.informa.impl.basic.Category;
import de.nava.informa.impl.basic.ChannelBuilder;
import de.nava.informa.impl.basic.Item;
import de.nava.informa.parsers.FeedParser;

/**
 * RSSDocument uses the Informa API to model an RSS Document
 *
 * @author gruiz
 *
 */
public class RSSBuilder {

    private static Log log = LogFactory.getLog(RSSBuilder.class);

    /**
	 * Describes the type of the RSSDocument. It can be 0.91, 1.0 or 2.0
	 * Defect type in the constrcutors are 2.0
	 */
    private String type;

    /**
	 * Describes the title of the Document
	 */
    private String title;

    /**
	 * Describes the Document
	 */
    private String description;

    /**
	 * Defines de date of the document
	 */
    private Date date;

    /**
	 * Includes all items or posts from a RSS document
	 */
    private Collection<ItemIF> items;

    private ChannelBuilder builder;

    /**
	 * Builds a RSSBuilder. It was made thinking to export RSS Documents
	 */
    public RSSBuilder() {
        title = "";
        description = "";
        date = new Date();
        items = new ArrayList<ItemIF>();
        type = "2.0";
        builder = new ChannelBuilder();
    }

    /**
	 * Builds a RSSBuilder from a URL. It was made thinking to read RSS Documents
	 * @param URLRSS
	 *
	 */
    @SuppressWarnings("unchecked")
    public RSSBuilder(String URLRSS) {
        try {
            URL url = new URL(URLRSS);
            ChannelIF channel = FeedParser.parse(new ChannelBuilder(), url);
            title = channel.getTitle();
            description = channel.getDescription();
            date = channel.getPubDate();
            items = channel.getItems();
            type = "2.0";
        } catch (Exception e) {
        }
    }

    /**
	 * Returns the ChannelIF Object of a RSSDocument
	 */
    public ChannelIF getChannelDocument() {
        ChannelIF channel = builder.createChannel(title);
        log.debug("Channel created");
        if (type.equals("2.0")) {
            channel.setFormat(ChannelFormat.RSS_2_0);
            log.debug("RSS type set 2.0");
        } else if (type.equals("1.0")) {
            channel.setFormat(ChannelFormat.RSS_1_0);
            log.debug("RSS type set 1.0");
        } else if (type.equals("0.91")) {
            channel.setFormat(ChannelFormat.RSS_0_91);
            log.debug("RSS type set 0.91");
        } else {
            channel.setFormat(ChannelFormat.RSS_2_0);
            log.debug("RSS type set 2.0");
        }
        log.debug("Description is: " + description);
        channel.setDescription(description);
        log.debug("Number of items in the RSS document: " + items.size());
        for (Iterator<ItemIF> i = items.iterator(); i.hasNext(); ) {
            ItemIF itemAuxiliar = i.next();
            channel.addItem(itemAuxiliar);
            log.debug("Item added: " + i);
        }
        log.debug("Numero de elementos en el canal: " + channel.getItems().size());
        return channel;
    }

    /**
	 * Returns the ChannelExporterIF Object of a RSSDocument depending of the RSS version
	 * @throws IOException
	 */
    public ChannelExporterIF getExporter(String filename) throws IOException {
        ChannelExporterIF exporter = new RSS_2_0_Exporter(filename);
        if (type.equals("2.0")) {
            exporter = new RSS_2_0_Exporter(filename);
        } else if (type.equals("1.0")) {
            exporter = new RSS_1_0_Exporter(filename);
        } else if (type.equals("0.91")) {
            exporter = new RSS_0_91_Exporter(filename);
        }
        return exporter;
    }

    /**
	 * Returns the ChannelExporterIF Object of a RSSDocument depending of the RSS version
	 * @throws IOException
	 */
    public ChannelExporterIF getExporter(Writer writer, String encoding) throws IOException {
        ChannelExporterIF exporter = new RSS_1_0_Exporter(writer, encoding);
        if (type.equals("2.0")) {
            exporter = new RSS_2_0_Exporter(writer, encoding);
        } else if (type.equals("1.0")) {
            exporter = new RSS_1_0_Exporter(writer, encoding);
        } else if (type.equals("0.91")) {
            exporter = new RSS_0_91_Exporter(writer, encoding);
        }
        return exporter;
    }

    /**
	 * Returns the date of the RSSDocument
	 */
    public Date getDate() {
        return date;
    }

    /**
	 * Sets the date of the RSSDocument
	 */
    public void setDate(Date date) {
        this.date = date;
    }

    /**
	 * Returns the description of the RSSDocument
	 */
    public String getDescription() {
        return description;
    }

    /**
	 * Sets the description of the RSSDocument
	 */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
	 * Returns the items (posts or news) of the RSSDocument as a Collection Object of Items
	 */
    public Collection<ItemIF> getItems() {
        return items;
    }

    /**
	 * Sets all new items and delete old items of the RSSDocument as a Collection Object of Items
	 */
    public void setItems(Collection<ItemIF> items) {
        this.items = items;
    }

    /**
	 * Add a new Item as a Item Object
	 */
    public void addItem(ItemIF item) {
        items.add(item);
    }

    /**
	 * @param itemTitle Describes the title of the post or the Item
	 * @param descriptionItem Describes the Item
	 * @param itemDate Describes the Date of the Item
	 * @param itemCategories An unordered set of <code>com.germinus.xpression.cms.categories.Category</code> instances
	 * @param itemLink Describes the link to access the Item
	 * Provide an easier interface to add new Items (news or posts).
	 */
    @SuppressWarnings("unchecked")
    public void addItem(String itemTitle, String descriptionItem, Date itemDate, Set itemCategories, String itemLink) throws MalformedURLException {
        Item newItem = new Item(itemTitle, descriptionItem, new URL(itemLink));
        newItem.setDescription(descriptionItem);
        newItem.setDate(itemDate);
        Collection<Category> categories = new ArrayList<Category>();
        int numCategories;
        com.germinus.xpression.cms.categories.Category itemCategory;
        if (itemCategories != null) {
            Iterator<Category> contentCategories = itemCategories.iterator();
            for (numCategories = 0; contentCategories.hasNext(); numCategories++) {
                Object next = contentCategories.next();
                if (next instanceof com.germinus.xpression.cms.categories.Category) {
                    itemCategory = (com.germinus.xpression.cms.categories.Category) next;
                    log.debug("Category:" + "Id: " + itemCategory.getId() + "class: " + itemCategory.getClass().getName());
                    Category rssCategory = new Category();
                    rssCategory.setId(itemCategory.getId().hashCode());
                    rssCategory.setTitle(itemCategory.getLabel());
                    categories.add(rssCategory);
                } else {
                    log.warn("Invalid category class: " + next.getClass() + " expected was: " + CategoryIF.class);
                }
            }
            if (numCategories == 0) {
                categories = null;
            }
            newItem.setCategories(categories);
            log.debug("Categories :" + newItem.getCategories());
        }
        items.add(newItem);
        log.debug("Item added");
    }

    /**
	 * Returns the title of the RSSDocument
	 */
    public String getTitle() {
        return title;
    }

    /**
	 * Sets the title of the RSSDocument
	 */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
	 * Returns the type of the RSSDocument
	 */
    public String getType() {
        return type;
    }

    /**
	 * Sets the type of the RSSDocument
	 */
    public void setType(String type) throws Exception {
        if (!(type.equals("2.0") || type.equals("1.0") || type.equals("0.91"))) {
            throw new Exception("RSS error Type. Not valid RSS version");
        } else {
            this.type = type;
        }
    }
}
