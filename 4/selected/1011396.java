package com.generatescape.htmlutils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import com.generatescape.baseobjects.ArticleObject;
import com.generatescape.baseobjects.CONSTANTS;

/*******************************************************************************
 * Copyright (c) 2005, 2007 GenerateScape and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the GNU General Public License which accompanies this distribution, and is
 * available at http://www.gnu.org/copyleft/gpl.html
 * 
 * @author kentgibson : http://www.bigblogzoo.com
 * 
 ******************************************************************************/
public class HTMLPageMaker {

    static Logger log = Logger.getLogger(HTMLPageMaker.class.getName());

    static TreeSet channelTitleSet = new TreeSet();

    static HashMap chanTitleToArticleList = new HashMap();

    static HashMap chanTitleToUrl = new HashMap();

    /**
   * @param articleList
   */
    public static void splitLists(ArrayList articleList) {
        channelTitleSet.clear();
        chanTitleToArticleList.clear();
        for (Iterator iter = articleList.iterator(); iter.hasNext(); ) {
            ArticleObject article = (ArticleObject) iter.next();
            String chantitle = article.getChannelTitle();
            if (channelTitleSet.contains(chantitle)) {
                ArrayList list = (ArrayList) chanTitleToArticleList.get(chantitle);
                list.add(article);
                chanTitleToArticleList.put(chantitle, list);
            } else {
                channelTitleSet.add(chantitle);
                ArrayList list = new ArrayList();
                list.add(article);
                chanTitleToArticleList.put(chantitle, list);
                chanTitleToUrl.put(chantitle, article.getChannelURL());
            }
        }
    }

    /**
   * @param heritage
   * @param links
   */
    public static void createStubPage(String heritage, ArrayList links) {
        try {
            heritage = URLEncoder.encode(heritage, "UTF-8");
            heritage = StringUtils.replace(heritage, "C%3A%5C", "c:\\");
            heritage = StringUtils.replace(heritage, "%5C", "\\");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        String html = createStubHeader(heritage, links);
        File dirs = new File(heritage);
        dirs.mkdirs();
        File file = new File(heritage + ".html");
        log.info(("Trying to create : " + file.getAbsolutePath()));
        try {
            FileWriter fw = new FileWriter(file);
            fw.write(html.toString());
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
   * @param articleList
   * @param heritage
   * @param links
   * @param nextarticle
   * @param drilldown
   * @param hasarchive
   * @return
   */
    public static String createStartPage(ArrayList articleList, String heritage, ArrayList links, int currentarticle, int nextarticle, boolean drilldown, boolean hasarchive) {
        log.info("heritage: " + heritage);
        log.info("currentarticle: " + currentarticle);
        log.info("nextarticle: " + nextarticle);
        boolean archivepage = false;
        boolean firstarchive = true;
        String html = createHeader(articleList, heritage, links, currentarticle, drilldown, archivepage, firstarchive, hasarchive, true, "Real Start Page", nextarticle, 0);
        archivepage = true;
        String htmlforarchive = createHeader(articleList, heritage, links, currentarticle, archivepage, firstarchive, archivepage, hasarchive, true, "Start Page X", nextarticle, 0);
        File dirs = new File(heritage);
        dirs.mkdirs();
        File file = new File(heritage + ".html");
        File dirsforarchive = new File(heritage);
        dirsforarchive.mkdirs();
        File fileforartchive = new File(heritage + "\\" + getDateFilename(currentarticle) + ".html");
        try {
            FileWriter fw = new FileWriter(file);
            fw.write(html.toString());
            fw.close();
            FileWriter fw2 = new FileWriter(fileforartchive);
            fw2.write(htmlforarchive.toString());
            fw2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return html;
    }

    public static String getDateFilename(int daysold) {
        if (daysold < 0) {
            System.out.println("Days negative!");
            log.info("Days negative!");
        }
        String pattern = "yyyy_MM_dd";
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        Date today = new Date(System.currentTimeMillis() - (CONSTANTS.DAY_IN_MILIS * daysold));
        return "ARCHIVE_" + formatter.format(today);
    }

    public static String getDate(int daysold) {
        String pattern = "yyyy MM dd";
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        Date today = new Date(System.currentTimeMillis() - (CONSTANTS.DAY_IN_MILIS * daysold));
        return formatter.format(today);
    }

    /**
   * @param articleList
   * @param heritage
   * @param daysold
   * @param links
   * @param nextarticle
   * @param firstarchive
   * @param lastarchive
   * @return
   */
    public static String createMiddlePage(ArrayList articleList, String heritage, int daysold, ArrayList links, boolean firstarchive, int nextarticle, int prevArticle) {
        String html = createHeader(articleList, heritage, links, daysold, false, true, firstarchive, true, false, " Middle ", nextarticle, prevArticle);
        File dirs = new File(heritage);
        dirs.mkdirs();
        File file = new File(heritage + "\\" + getDateFilename(daysold) + ".html");
        log.info("Abs path " + file.getAbsolutePath());
        try {
            FileWriter fw = new FileWriter(file);
            fw.write(html.toString());
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return html;
    }

    /**
   * @param heritage
   * @param twoup
   * @return
   */
    private static String getPrevAsLink(String heritage, boolean twoup) {
        int lastslash = heritage.lastIndexOf("/");
        String prev = "Undefined";
        if (lastslash > heritage.length()) {
            log.info("Too Short on: " + prev);
        } else {
            prev = heritage.substring(0, lastslash);
            int indexfirst = prev.lastIndexOf("/");
            prev = heritage.substring(indexfirst, prev.length());
        }
        log.info("Prev is : " + prev);
        StringBuffer prevlink = new StringBuffer();
        prevlink.append("<a href=\"");
        prevlink.append("..");
        if (twoup) {
            prevlink.append("/..");
        }
        prevlink.append(prev + ".html");
        prevlink.append("\">");
        prevlink.append(prev.substring(1, prev.length()));
        prevlink.append("</a>");
        prevlink.append("</br>\n");
        return prevlink.toString();
    }

    /**
   * @param heritage
   * @return
   */
    private static String getPrev(String heritage) {
        int lastslash = heritage.lastIndexOf("/");
        String prev = "Undefined";
        if (lastslash > heritage.length()) {
            log.info("Too Short on: " + prev);
        } else {
            prev = heritage.substring(lastslash + 1);
        }
        return prev;
    }

    /**
   * @param heritage
   * @param links
   * @return
   */
    private static String createStubHeader(String heritage, ArrayList links) {
        StringBuffer html = new StringBuffer();
        html.append("<HTML>");
        html.append("<BODY>");
        html.append("<h1>STUB</h1>");
        log.info("Heritage is : " + heritage);
        String prev = getPrevAsLink(heritage, false);
        html.append(prev);
        String prevtext = getPrev(heritage);
        for (Iterator iterator = links.iterator(); iterator.hasNext(); ) {
            String link = (String) iterator.next();
            log.info("link is : " + link);
            StringBuffer alink = new StringBuffer();
            String result = prevtext + "/" + link + ".html";
            log.info("Result " + result);
            alink.append("<a href=\"");
            alink.append(result);
            alink.append("\">");
            alink.append(link);
            alink.append("</a>");
            alink.append("</br>\n");
            log.info("alink " + alink.toString());
            html.append(alink.toString());
        }
        html.append("</HTML>");
        return html.toString();
    }

    /**
   * @param articleList
   * @param heritage
   * @param links
   * @param currentarticle
   * @param drilldown
   * @param archivepage
   * @param firstarchive
   * @param hasarchive
   * @param firstpage
   * @param debug
   * @param nextarticle
   * @return
   */
    private static String createHeader(ArrayList articleList, String heritage, ArrayList links, int currentarticle, boolean drilldown, boolean archivepage, boolean firstarchive, boolean hasarchive, boolean firstpage, String debug, int nextarticle, int prevArticle) {
        splitLists(articleList);
        StringBuffer html = new StringBuffer();
        html.append("<HTML>");
        html.append("<BODY>");
        html.append(getPrevAsLink(heritage, archivepage) + " + " + debug);
        log.info("nextarticle is : " + currentarticle);
        String prevArticleFilename = "NULL";
        String prevArticleDateString = "NULL";
        prevArticleFilename = getDateFilename(prevArticle);
        prevArticleDateString = getDate(prevArticle);
        String lastarticlename = getDateFilename(nextarticle);
        String lastArticleDateString = getDate(nextarticle);
        int lastslash = heritage.lastIndexOf("/");
        log.info("1. Heritage was : " + heritage);
        heritage = heritage.substring(lastslash + 1, heritage.length());
        log.info("1. Heritage is : " + heritage);
        html.append("<h2>" + heritage + "</h2>");
        for (Iterator iterator = links.iterator(); iterator.hasNext(); ) {
            String link = (String) iterator.next();
            log.info("link is : " + link);
            StringBuffer alink = new StringBuffer();
            alink.append("&nbsp;&nbsp;&nbsp;<a href=\"");
            if (!archivepage) {
                alink.append(heritage + "/" + link + ".html");
            } else {
                alink.append(link + ".html");
            }
            alink.append("\">");
            alink.append(link);
            alink.append("</a>");
            alink.append("</br>\n");
            log.info("alink " + alink.toString());
            html.append(alink.toString());
        }
        StringBuffer prevArticleLink = new StringBuffer();
        if (firstpage) {
            prevArticleLink.append("<a href=\"");
            if (drilldown) {
                prevArticleLink.append(heritage + "/" + lastarticlename + ".html");
            } else {
                prevArticleLink.append(lastarticlename + ".html");
            }
            prevArticleLink.append("\">");
            prevArticleLink.append("Archive");
            prevArticleLink.append("</a>");
            prevArticleLink.append("</br>\n");
        } else if (archivepage) {
            if (currentarticle > -1) {
                prevArticleLink.append("<a href=\"");
                if (drilldown) {
                    prevArticleLink.append(heritage + "/" + prevArticleFilename + ".html");
                } else {
                    prevArticleLink.append(prevArticleFilename + ".html");
                }
                prevArticleLink.append("\">");
                prevArticleLink.append("Back to Archive " + prevArticleDateString);
                prevArticleLink.append("</a>");
            }
            if (!firstarchive) {
                prevArticleLink.append("&nbsp;&nbsp;");
                prevArticleLink.append("<a href=\"");
                if (drilldown) {
                    prevArticleLink.append(heritage + "/" + lastarticlename + ".html");
                } else {
                    prevArticleLink.append(lastarticlename + ".html");
                }
                prevArticleLink.append("\">");
                prevArticleLink.append("Forward to Archive " + lastArticleDateString);
                prevArticleLink.append("</a>");
            }
            prevArticleLink.append("</br>\n");
        }
        html.append(prevArticleLink.toString());
        for (Iterator iter = channelTitleSet.iterator(); iter.hasNext(); ) {
            String chantitle = (String) iter.next();
            String chanurl = (String) chanTitleToUrl.get(chantitle);
            html.append("</br>\n");
            html.append("<strong>");
            html.append("<a href=\"http://www.syndicatescape.com/feedproviderservlet?url=");
            html.append(chanurl);
            html.append("\">");
            html.append(chantitle);
            html.append("</a>");
            html.append("</strong>");
            html.append("</br>\n");
            ArrayList list = (ArrayList) chanTitleToArticleList.get(chantitle);
            for (Iterator iterator = list.iterator(); iterator.hasNext(); ) {
                ArticleObject element = (ArticleObject) iterator.next();
                String articleTitle = element.getTitle();
                String articleDesc = element.getDescription();
                html.append("</br>\n");
                html.append("<strong>");
                html.append("<a href=\"");
                html.append(element.getUrl());
                html.append("\">");
                html.append(articleTitle);
                html.append("</a>");
                html.append("</strong>");
                html.append("\n");
                html.append("</br>\n");
                html.append(articleDesc);
                html.append("</br>\n");
            }
        }
        html.append("</BODY>");
        html.append("</HTML>");
        return html.toString();
    }

    public static String createEndPage() {
        return "";
    }
}
