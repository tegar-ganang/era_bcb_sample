package com.byjyate.rssdreamwork;

import java.io.*;
import java.util.*;
import javax.cache.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import com.google.appengine.api.memcache.stdimpl.GCacheFactory;

public class FetchRssTransformer implements RssTransformer {

    private String url;

    private int expireSecond;

    public FetchRssTransformer(String url) {
        if (url == null || url.isEmpty()) throw new NullPointerException();
        {
            String[] param = url.split(" ");
            this.url = param[0];
            try {
                expireSecond = Integer.parseInt(param[1]);
            } catch (Exception e) {
                expireSecond = 0;
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void applyTransform(Element channelNode) {
        if (channelNode == null) throw new NullPointerException();
        try {
            Cache cache;
            Map props = new HashMap();
            props.put(GCacheFactory.EXPIRATION_DELTA, expireSecond);
            CacheFactory cacheFactory;
            cacheFactory = CacheManager.getInstance().getCacheFactory();
            cache = cacheFactory.createCache(props);
            String page = null;
            if (expireSecond > 0) page = (String) cache.get(url);
            if (page == null) {
                HtmlFetcher fetcher = new HtmlFetcher(url);
                page = fetcher.getContent();
                if (expireSecond > 0) cache.put(url, page);
            }
            StringReader sr = new StringReader(page);
            InputSource is = new InputSource(sr);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document rssDocument = db.parse(is);
            NodeList fetchedItems = getChannelNode(rssDocument).getChildNodes();
            for (int i = 0; i < fetchedItems.getLength(); i++) {
                Node item = fetchedItems.item(i);
                if (item.getNodeName().toLowerCase().equals("item")) {
                    channelNode.getOwnerDocument().adoptNode(item);
                    channelNode.appendChild(item);
                }
            }
        } catch (Exception e) {
            System.err.println("���� RSS ʱ�����쳣:");
            System.err.println("��������ҳ��: " + url);
            e.printStackTrace(System.err);
            return;
        }
    }

    private Node getChannelNode(Document rssDocument) {
        NodeList items = rssDocument.getDocumentElement().getChildNodes();
        for (int i = 0; i < items.getLength(); i++) {
            Node item = items.item(i);
            if (item.getNodeName().toLowerCase().equals("channel")) return item;
        }
        return null;
    }
}
