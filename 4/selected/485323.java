package net.siuying.any2rss.handler;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ItemIF;
import net.siuying.any2rss.util.URLUtil;
import org.apache.commons.configuration.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Administrator
 * 
 */
public class PatternContentHandler extends AbstractContentHandler {

    private static Log log = LogFactory.getLog(PatternContentHandler.class);

    public static final String KEY_PREFIX = "handler.patterns.";

    protected Map<String, Integer> itemPatternOrder;

    protected Pattern itemPattern;

    protected SimpleDateFormat dateFormatter;

    /**
     * handle request by pattern
     * 
     * @see net.siuying.any2rss.handler.AbstractContentHandler#handle(java.lang.String)
     */
    public ChannelIF handle(String content) throws ContentHandlerException {
        log.trace("Handling content using handler ... " + this.getTitle());
        Matcher m = itemPattern.matcher(content);
        while (m.find()) {
            String title = null;
            String description = null;
            String link = null;
            String date = null;
            try {
                Integer titleOrder = itemPatternOrder.get("title");
                Integer descOrder = itemPatternOrder.get("description");
                Integer linkOrder = itemPatternOrder.get("link");
                Integer dateOrder = itemPatternOrder.get("date");
                log.info("pattern order ... title=" + titleOrder + ", desc=" + descOrder + ", link=" + linkOrder + ", date=" + dateOrder);
                title = (titleOrder == null) ? "Untitled" : m.group(itemPatternOrder.get("title"));
                description = (descOrder == null) ? "" : m.group(itemPatternOrder.get("description"));
                link = (linkOrder == null) ? "http://unknown" : m.group(itemPatternOrder.get("link"));
                date = (dateOrder == null) ? "" : m.group(itemPatternOrder.get("date"));
                URL linkUrl = URLUtil.getAbsoluteLink(this.getSite(), link);
                log.debug(title + "," + description + "," + linkUrl);
                ItemIF item = addItem(title, description, linkUrl);
                try {
                    Date feedDate = dateFormatter.parse(date);
                    item.setDate(feedDate);
                } catch (ParseException e) {
                    log.warn("Date malformatted: " + e.getMessage() + "; Src:" + date);
                }
            } catch (MalformedURLException e) {
                try {
                    log.warn("URL malformatted: " + e.getMessage());
                    this.addItem(title, description, new URL("http://unknown.com/"));
                } catch (Exception error) {
                    log.warn("URL malformatted! Cannot recovered the link, ignored.");
                }
            }
        }
        return this.getChannel();
    }

    /**
     * Configure the handler, read parameters:
     * @see net.siuying.any2rss.handler.AbstractContentHandler#configure(net.siuying.any2rss.cfg.ConfiguratorIF)
     */
    private void configure(String handlerId, Configuration config) throws ConfigurationException {
        String generator = null;
        String title = null;
        String siteUrl = null;
        String desc = null;
        String itemPatternStr = null;
        String[] itemPatternOrderStr = null;
        String datePatternStr = null;
        String titleKey = KEY_PREFIX + handlerId + ".title";
        String urlKey = KEY_PREFIX + handlerId + ".url";
        String descKey = KEY_PREFIX + handlerId + ".description";
        String patternKey = KEY_PREFIX + handlerId + ".pattern";
        String datePatternKey = KEY_PREFIX + handlerId + ".date";
        try {
            generator = config.getString("any2rss.generator", "Any2Rss");
            title = config.getString(titleKey, "Untitled");
            siteUrl = config.getString(urlKey, "http://nourl.org");
            desc = config.getString(descKey, "No Descriptions");
            itemPatternStr = config.getString(patternKey);
        } catch (NoSuchElementException ne) {
            throw new ConfigurationException("Missing required parameters: ", ne);
        } catch (ConversionException ce) {
            throw new ConfigurationException("Error data type: ", ce);
        }
        try {
            datePatternStr = config.getString(datePatternKey);
        } catch (NoSuchElementException ne) {
            log.warn("Date pattern is not specified for handler, key=" + datePatternKey);
        } catch (ConversionException ce) {
            throw new ConfigurationException("Error data type: ", ce);
        }
        configure(generator, title, siteUrl, desc, itemPatternStr, itemPatternOrderStr, datePatternStr);
    }

    public void configure(Configuration config) throws ConfigurationException {
        try {
            String defaultIdKey = KEY_PREFIX + ".default";
            String defaultId = config.getString(defaultIdKey);
            log.info("Loading handler: " + defaultId);
            configure(defaultId, config);
        } catch (NoSuchElementException ne) {
            throw new ConfigurationException("Missing required parameters: ", ne);
        } catch (ConversionException ce) {
            log.warn("Error data type during configuration: " + ce.getMessage());
            throw new ConfigurationException("Error data type: ", ce);
        }
    }

    public void configure(String generator, String title, String siteUrl, String desc, String itemPatternStr, String[] itemPatternOrderStr, String datePatternStr) throws ConfigurationException {
        this.setGenerator(generator);
        this.setTitle(title);
        try {
            this.setSite(new URL(siteUrl));
        } catch (MalformedURLException me) {
            log.warn("Malformatted URL, ignored: " + siteUrl);
        }
        this.setDescription(desc);
        if (itemPatternStr == null) {
            log.error("Pattern undefined.");
            throw new ConfigurationException("Missing configuration parameter of pattern! ");
        }
        try {
            itemPattern = Pattern.compile(itemPatternStr);
        } catch (PatternSyntaxException pe) {
            log.error("Pattern is invalid: " + itemPatternStr);
            throw new ConfigurationException("Pattern is invalid: " + itemPatternStr, pe);
        }
        itemPatternOrder = new HashMap<String, Integer>();
        for (int i = 0; i < itemPatternOrderStr.length; i++) {
            itemPatternOrder.put(itemPatternOrderStr[i], i + 1);
        }
        try {
            dateFormatter = new SimpleDateFormat(datePatternStr, Locale.ENGLISH);
        } catch (IllegalArgumentException iae) {
            log.error("Date Pattern is invalid: " + datePatternStr);
            throw new ConfigurationException("Date Pattern is invalid: " + datePatternStr, iae);
        } catch (NullPointerException ne) {
            log.warn("Date pattern not specified.");
        }
    }

    public String toString() {
        return this.getTitle() + ", " + this.getDescription() + ", " + this.getSite() + ", " + this.getItemPattern() + ", " + this.getItemPatternOrder();
    }

    /**
     * Compare if two pattern content handler is equals
     */
    public boolean equals(Object target) {
        if (target == null || !(target instanceof PatternContentHandler)) {
            return false;
        }
        PatternContentHandler ptnTarget = (PatternContentHandler) target;
        if (this.getTitle().equals(ptnTarget.getTitle()) && this.getDescription().equals(ptnTarget.getDescription()) && this.getSite().equals(ptnTarget.getSite()) && this.getItemPattern().equals(ptnTarget.getItemPattern()) && this.getItemPatternOrder().equals(ptnTarget.getItemPatternOrder())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return Returns the itemPattern.
     */
    private Pattern getItemPattern() {
        return itemPattern;
    }

    /**
     * @param itemPattern The itemPattern to set.
     */
    private void setItemPattern(Pattern itemPattern) {
        this.itemPattern = itemPattern;
    }

    /**
     * @return Returns the itemPatternOrder.
     */
    private Map<String, Integer> getItemPatternOrder() {
        return itemPatternOrder;
    }

    /**
     * @param itemPatternOrder The itemPatternOrder to set.
     */
    private void setItemPatternOrder(Map<String, Integer> itemPatternOrder) {
        this.itemPatternOrder = itemPatternOrder;
    }

    public Set<String> getSites() {
        return itemPatternOrder.keySet();
    }
}
