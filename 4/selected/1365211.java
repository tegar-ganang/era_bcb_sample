package csiebug.util.rss;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.List;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import csiebug.util.FileUtility;

/**
 * @author George_Tsai
 * @version 2010/10/17
 */
public class RSSFeedUtility {

    private File xmlFile;

    public RSSFeedUtility(File xmlFile, RSSFeed feed) throws IOException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        this.xmlFile = xmlFile;
        initialFeed(feed);
    }

    private void initialFeed(RSSFeed feed) throws IOException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        if (!xmlFile.exists()) {
            Document document = DocumentHelper.createDocument();
            Element rootElement = document.addElement("rss");
            rootElement.addAttribute("version", "2.0");
            Element channelElement = rootElement.addElement("channel");
            if (feed != null && feed.getChannel() != null) {
                addChannelAttribute(channelElement, feed.getChannel());
            }
            OutputFormat format = OutputFormat.createPrettyPrint();
            XMLWriter writer = new XMLWriter(new FileWriter(xmlFile), format);
            writer.write(document);
            writer.close();
        }
    }

    @SuppressWarnings("deprecation")
    private void addChannelAttribute(Element channelElement, RSSChannel channel) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        Method[] methods = RSSChannel.class.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (method.getName().startsWith("get") && !method.getName().equalsIgnoreCase("getClass")) {
                String attributeName = method.getName().replaceFirst("get", "");
                attributeName = attributeName.substring(0, 1).toLowerCase() + attributeName.substring(1, attributeName.length());
                Object obj = method.invoke(channel, (Object[]) null);
                if (obj != null) {
                    String objClass = getPropertyClass(obj);
                    Element element = channelElement.addElement(attributeName);
                    if (objClass.equals("csiebug.util.rss.RSSCategory")) {
                        element.addAttribute("domain", channel.getCategory().getDomain());
                        element.addText(channel.getCategory().getText());
                    } else if (objClass.equals("csiebug.util.rss.RSSImage")) {
                        addImageAttribute(element, channel.getImage());
                    } else if (objClass.equals("java.util.Calendar")) {
                        element.addText(((Calendar) obj).getTime().toGMTString());
                    } else {
                        element.addText(obj.toString());
                    }
                }
            }
        }
    }

    private void addImageAttribute(Element imageElement, RSSImage image) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        Method[] methods = RSSImage.class.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (method.getName().startsWith("get") && !method.getName().equalsIgnoreCase("getClass")) {
                String attributeName = method.getName().replaceFirst("get", "");
                attributeName = attributeName.substring(0, 1).toLowerCase() + attributeName.substring(1, attributeName.length());
                Object obj = method.invoke(image, (Object[]) null);
                if (obj != null) {
                    Element element = imageElement.addElement(attributeName);
                    element.addText(obj.toString());
                }
            }
        }
    }

    private String getPropertyClass(Object obj) throws ClassNotFoundException {
        String className = "java.lang.String";
        String[] propertyClasses = new String[] { "java.util.Calendar", "csiebug.util.rss.RSSCategory", "csiebug.util.rss.RSSImage" };
        for (int i = 0; i < propertyClasses.length; i++) {
            try {
                Class<?> c = Class.forName(propertyClasses[i]);
                c.cast(obj);
                className = propertyClasses[i];
                break;
            } catch (ClassCastException ccex) {
                continue;
            }
        }
        return className;
    }

    @SuppressWarnings("deprecation")
    private void addItemAttribute(Element itemElement, RSSItem item) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        Method[] methods = RSSItem.class.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (method.getName().startsWith("get") && !method.getName().equalsIgnoreCase("getClass")) {
                String attributeName = method.getName().replaceFirst("get", "");
                attributeName = attributeName.substring(0, 1).toLowerCase() + attributeName.substring(1, attributeName.length());
                Object obj = method.invoke(item, (Object[]) null);
                if (obj != null) {
                    String objClass = getPropertyClass(obj);
                    Element element = itemElement.addElement(attributeName);
                    if (objClass.equals("csiebug.util.rss.RSSCategory")) {
                        element.addAttribute("domain", item.getCategory().getDomain());
                        element.addText(item.getCategory().getText());
                    } else if (objClass.equals("java.util.Calendar")) {
                        element.addText(((Calendar) obj).getTime().toGMTString());
                    } else {
                        element.addText(obj.toString());
                    }
                }
            }
        }
    }

    /**
	 * 新增一個item進feed
	 * @param title
	 * @param description
	 * @param link
	 * @throws DocumentException
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
    @SuppressWarnings("deprecation")
    public void addItem(RSSItem item) throws DocumentException, IOException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        Document document = DocumentHelper.parseText(FileUtility.getTextFileContent(xmlFile, "UTF-8"));
        Element channelElement = document.getRootElement().element("channel");
        Element itemElement = channelElement.addElement("item");
        addItemAttribute(itemElement, item);
        if (item.getPubDate() != null) {
            Element pubDateElement;
            if (channelElement.element("pubDate") != null) {
                pubDateElement = channelElement.element("pubDate");
            } else {
                pubDateElement = channelElement.addElement("pubDate");
            }
            pubDateElement.setText(item.getPubDate().getTime().toGMTString());
        }
        Element lastBuildDateElement;
        if (channelElement.element("lastBuildDate") != null) {
            lastBuildDateElement = channelElement.element("lastBuildDate");
        } else {
            lastBuildDateElement = channelElement.addElement("lastBuildDate");
        }
        lastBuildDateElement.setText(Calendar.getInstance().getTime().toGMTString());
        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter writer = new XMLWriter(new FileWriter(xmlFile), format);
        writer.write(document);
        writer.close();
    }

    /**
	 * 新增多筆item進feed
	 * @param items
	 * @throws DocumentException
	 * @throws IOException
	 * @throws ClassNotFoundException 
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
    @SuppressWarnings("deprecation")
    public void addItems(List<RSSItem> items) throws DocumentException, IOException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, ClassNotFoundException {
        Document document = DocumentHelper.parseText(FileUtility.getTextFileContent(xmlFile, "UTF-8"));
        Element channelElement = document.getRootElement().element("channel");
        Calendar latestPubDate = null;
        for (int i = 0; i < items.size(); i++) {
            RSSItem item = items.get(i);
            Element itemElement = channelElement.addElement("item");
            addItemAttribute(itemElement, item);
            if (latestPubDate == null) {
                latestPubDate = item.getPubDate();
            } else if (item.getPubDate() != null && latestPubDate.before(item.getPubDate())) {
                latestPubDate = item.getPubDate();
            }
        }
        Element pubDateElement;
        if (channelElement.element("pubDate") != null) {
            pubDateElement = channelElement.element("pubDate");
        } else {
            pubDateElement = channelElement.addElement("pubDate");
        }
        pubDateElement.setText(latestPubDate.getTime().toGMTString());
        Element lastBuildDateElement;
        if (channelElement.element("lastBuildDate") != null) {
            lastBuildDateElement = channelElement.element("lastBuildDate");
        } else {
            lastBuildDateElement = channelElement.addElement("lastBuildDate");
        }
        lastBuildDateElement.setText(Calendar.getInstance().getTime().toGMTString());
        OutputFormat format = OutputFormat.createPrettyPrint();
        XMLWriter writer = new XMLWriter(new FileWriter(xmlFile), format);
        writer.write(document);
        writer.close();
    }
}
