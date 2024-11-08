package com.byjyate.rssdreamwork;

import java.util.*;
import java.io.*;
import java.util.Map;
import javax.cache.*;
import javax.xml.parsers.*;
import com.google.appengine.api.memcache.stdimpl.GCacheFactory;
import org.w3c.dom.*;
import org.xml.sax.*;

public class FetchCarnocRss implements RssTransformer {

    private int expireSecond;

    String[] CarnocRsss = { "http://news.carnoc.com/rss/1.xml", "http://news.carnoc.com/rss/2.xml", "http://news.carnoc.com/rss/3.xml", "http://news.carnoc.com/rss/4.xml", "http://news.carnoc.com/rss/5.xml", "http://news.carnoc.com/rss/6.xml", "http://news.carnoc.com/rss/7.xml", "http://news.carnoc.com/rss/8.xml", "http://news.carnoc.com/rss/9.xml", "http://news.carnoc.com/rss/10.xml", "http://news.carnoc.com/rss/13.xml", "http://news.carnoc.com/rss/14.xml", "http://news.carnoc.com/rss/15.xml", "http://news.carnoc.com/rss/18.xml", "http://news.carnoc.com/rss/20.xml", "http://news.carnoc.com/rss/21.xml", "http://news.carnoc.com/rss/22.xml", "http://news.carnoc.com/rss/23.xml", "http://news.carnoc.com/rss/37.xml", "http://news.carnoc.com/rss/40.xml", "http://news.carnoc.com/rss/41.xml", "http://news.carnoc.com/rss/42.xml", "http://news.carnoc.com/rss/43.xml", "http://news.carnoc.com/rss/44.xml", "http://news.carnoc.com/rss/45.xml", "http://news.carnoc.com/rss/46.xml", "http://news.carnoc.com/rss/47.xml", "http://news.carnoc.com/rss/48.xml", "http://news.carnoc.com/rss/49.xml", "http://news.carnoc.com/rss/50.xml", "http://news.carnoc.com/rss/51.xml", "http://news.carnoc.com/rss/52.xml", "http://news.carnoc.com/rss/53.xml", "http://news.carnoc.com/rss/54.xml", "http://news.carnoc.com/rss/55.xml", "http://news.carnoc.com/rss/56.xml", "http://news.carnoc.com/rss/57.xml", "http://news.carnoc.com/rss/58.xml", "http://news.carnoc.com/rss/59.xml", "http://news.carnoc.com/rss/60.xml", "http://news.carnoc.com/rss/61.xml", "http://news.carnoc.com/rss/62.xml", "http://news.carnoc.com/rss/63.xml", "http://news.carnoc.com/rss/64.xml", "http://news.carnoc.com/rss/65.xml", "http://news.carnoc.com/rss/66.xml", "http://news.carnoc.com/rss/67.xml", "http://news.carnoc.com/rss/68.xml", "http://news.carnoc.com/rss/69.xml", "http://news.carnoc.com/rss/70.xml", "http://news.carnoc.com/rss/74.xml", "http://news.carnoc.com/rss/76.xml", "http://news.carnoc.com/rss/77.xml", "http://news.carnoc.com/rss/78.xml", "http://news.carnoc.com/rss/79.xml", "http://news.carnoc.com/rss/80.xml", "http://news.carnoc.com/rss/81.xml", "http://news.carnoc.com/rss/82.xml", "http://news.carnoc.com/rss/83.xml", "http://news.carnoc.com/rss/84.xml", "http://news.carnoc.com/rss/85.xml", "http://news.carnoc.com/rss/86.xml", "http://news.carnoc.com/rss/87.xml", "http://news.carnoc.com/rss/88.xml", "http://news.carnoc.com/rss/89.xml", "http://news.carnoc.com/rss/90.xml", "http://news.carnoc.com/rss/91.xml", "http://news.carnoc.com/rss/93.xml", "http://news.carnoc.com/rss/94.xml", "http://news.carnoc.com/rss/97.xml", "http://news.carnoc.com/rss/98.xml", "http://news.carnoc.com/rss/103.xml", "http://news.carnoc.com/rss/111.xml", "http://news.carnoc.com/rss/112.xml", "http://news.carnoc.com/rss/121.xml", "http://news.carnoc.com/rss/122.xml", "http://news.carnoc.com/rss/123.xml", "http://news.carnoc.com/rss/124.xml", "http://news.carnoc.com/rss/125.xml", "http://news.carnoc.com/rss/129.xml", "http://news.carnoc.com/rss/138.xml", "http://news.carnoc.com/rss/143.xml", "http://news.carnoc.com/rss/144.xml", "http://news.carnoc.com/rss/145.xml", "http://news.carnoc.com/rss/146.xml", "http://news.carnoc.com/rss/147.xml", "http://news.carnoc.com/rss/149.xml", "http://news.carnoc.com/rss/150.xml", "http://news.carnoc.com/rss/155.xml", "http://news.carnoc.com/rss/157.xml", "http://news.carnoc.com/rss/158.xml", "http://news.carnoc.com/rss/159.xml", "http://news.carnoc.com/rss/160.xml", "http://news.carnoc.com/rss/161.xml", "http://news.carnoc.com/rss/162.xml", "http://news.carnoc.com/rss/164.xml", "http://news.carnoc.com/rss/airbu.xml", "http://news.carnoc.com/rss/boein.xml", "http://news.carnoc.com/rss/embra.xml", "http://news.carnoc.com/rss/bomba.xml", "http://news.carnoc.com/rss/comac.xml", "http://news.carnoc.com/rss/xifei.xml", "http://news.carnoc.com/rss/ameco.xml", "http://news.carnoc.com/rss/mtuzh.xml", "http://news.carnoc.com/rss/ca.xml", "http://news.carnoc.com/rss/mu.xml", "http://news.carnoc.com/rss/ci.xml", "http://news.carnoc.com/rss/mf.xml", "http://news.carnoc.com/rss/hu.xml", "http://news.carnoc.com/rss/zh.xml", "http://news.carnoc.com/rss/8y.xml", "http://news.carnoc.com/rss/gs.xml", "http://news.carnoc.com/rss/8l.xml", "http://news.carnoc.com/rss/pek.xml", "http://news.carnoc.com/rss/pvg.xml", "http://news.carnoc.com/rss/sha.xml", "http://news.carnoc.com/rss/can.xml", "http://news.carnoc.com/rss/hkg.xml" };

    public FetchCarnocRss(String param) {
        try {
            expireSecond = Integer.parseInt(param);
        } catch (Exception e) {
            expireSecond = 0;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void applyTransform(Element channelNode) {
        if (channelNode == null) throw new NullPointerException();
        String errurl = null;
        try {
            Cache cache;
            Map props = new HashMap();
            props.put(GCacheFactory.EXPIRATION_DELTA, expireSecond);
            CacheFactory cacheFactory;
            cacheFactory = CacheManager.getInstance().getCacheFactory();
            cache = cacheFactory.createCache(props);
            for (String url : CarnocRsss) {
                errurl = url;
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
            }
        } catch (Exception e) {
            System.err.println("���� RSS ʱ�����쳣:");
            System.err.println("��������ҳ��: " + errurl);
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
