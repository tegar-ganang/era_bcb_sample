package bw.news;

import java.net.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import java.util.*;
import bw.util.*;

public class NewsXMLHandler extends DefaultHandler {

    public static final String CATEGORY_TAG = "category";

    public static final String SOURCE_TAG = "source";

    public static final String NAME_ATTR = "name";

    public static final String SRC_ATTR = "src";

    public static final String ID_ATTR = "id";

    public static final String REFRESH_ATTR = "refresh";

    public static final String SITE_ATTR = "site";

    public static final String USERNAME_ATTR = "username";

    public static final String PASSWORD_ATTR = "password";

    private Vector _categories = new Vector();

    private int _currentId = 0;

    private NewsCategory _currentCategory = null;

    public NewsXMLHandler() {
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if (qName.equalsIgnoreCase(CATEGORY_TAG)) {
            String name = attributes.getValue(NAME_ATTR);
            _currentCategory = getCategory(name);
        } else if (qName.equalsIgnoreCase(SOURCE_TAG)) {
            String name = attributes.getValue(NAME_ATTR);
            String src = attributes.getValue(SRC_ATTR);
            String site = attributes.getValue(SITE_ATTR);
            String username = attributes.getValue(USERNAME_ATTR);
            String password = attributes.getValue(PASSWORD_ATTR);
            NewsSource source = new NewsSource(_currentId++, name, src);
            if (username != null && username.length() > 0 && password != null && password.length() > 0) {
                source.setUsername(username);
                source.setPassword(password);
            }
            String refreshStr = attributes.getValue(REFRESH_ATTR);
            if (refreshStr != null) {
                try {
                    long refresh = Integer.parseInt(refreshStr) * 60 * 1000L;
                    source.setRefresh(refresh);
                } catch (NumberFormatException ex) {
                    Log.getInstance().write("Problem reading refresh frequency" + " for news source: " + name, ex);
                }
            }
            if (site == null) {
                try {
                    URL srcURL = new URL(src);
                    site = srcURL.getProtocol() + "://" + srcURL.getHost();
                } catch (MalformedURLException ex) {
                    Log.getInstance().write("Problem construcing src url: " + src, ex);
                }
            }
            source.setSiteURL(site);
            _currentCategory.addNewsSource(source);
        }
    }

    public NewsCategory getCategory(String name) {
        NewsCategory cat = null;
        for (Enumeration e = _categories.elements(); e.hasMoreElements(); ) {
            NewsCategory c = (NewsCategory) e.nextElement();
            if (c.getName().equals(name)) {
                cat = c;
                break;
            }
        }
        if (cat == null) {
            cat = new NewsCategory(_categories.size(), name);
            _categories.addElement(cat);
        }
        return cat;
    }

    public Vector getNewsSourcesByCategory() {
        return _categories;
    }
}
