package org.wportal;

import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.providers.WikiPageProvider;
import org.wportal.core.Utils;
import java.io.*;
import java.net.URLDecoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

/**
 * User: SimonLei
 * Date: 2004-10-13
 * Time: 10:24:38
 * $Id: PageDataConverter.java,v 1.1 2004/10/28 01:39:14 simon_lei Exp $
 */
public class PageDataConverter {

    private String pageDir;

    private WikiPageProvider provider;

    public PageDataConverter(String pageDir) {
        this.pageDir = pageDir;
        provider = (WikiPageProvider) JspWiki2WikiPortal.context.getBean("convertPageProviderDao");
    }

    public void convert() throws IOException, FileNotFoundException, ProviderException {
        File root = new File(pageDir);
        String[] datas = root.list(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                if (name.endsWith(".txt")) return true;
                return false;
            }
        });
        for (String page : datas) {
            convertPage(page);
        }
    }

    private void convertPage(String page) throws IOException, FileNotFoundException, ProviderException {
        String encodedPageName = page.substring(0, page.length() - 4);
        String pageName = URLDecoder.decode(encodedPageName, "UTF-8");
        System.out.println("pageName = " + pageName);
        String oldPageDir = pageDir + "/" + "OLD" + "/" + encodedPageName;
        File propFile = new File(oldPageDir + "/page.properties");
        if (propFile.exists()) {
            multiVersionPage(propFile, pageName, oldPageDir, page);
        } else {
            propFile = new File(pageDir + "/" + encodedPageName + ".properties");
            if (propFile.exists()) {
                nonVersionPage(propFile, pageName, page);
            } else {
                nonPropertyPage(pageName, page);
                return;
            }
        }
    }

    private void multiVersionPage(File propFile, String pageName, String oldPageDir, String page) throws IOException, ProviderException {
        Properties prop = new Properties();
        prop.load(new FileInputStream(propFile));
        HashMap<Integer, String> map = new HashMap<Integer, String>();
        for (Object obj : prop.keySet()) {
            String key = (String) obj;
            int index = key.indexOf('.');
            map.put(Integer.valueOf(key.substring(0, index)), (String) prop.get(key));
        }
        WikiPage wikiPage = new WikiPage(pageName);
        for (int i = 1; i < map.size(); i++) {
            String verFileName = oldPageDir + "/" + i + ".txt";
            wikiPage.setAuthor(map.get(i));
            String pageText = getPageText(wikiPage, verFileName);
            provider.putPageText(wikiPage, pageText);
        }
        wikiPage.setAuthor(map.get(map.size()));
        String pageText = getPageText(wikiPage, pageDir + "/" + page);
        provider.putPageText(wikiPage, pageText);
    }

    private void nonPropertyPage(String pageName, String page) throws ProviderException, IOException {
        WikiPage wikiPage = new WikiPage(pageName);
        wikiPage.setAuthor("����");
        String pageText = getPageText(wikiPage, pageDir + "/" + page);
        provider.putPageText(wikiPage, pageText);
    }

    private void nonVersionPage(File propFile, String pageName, String page) throws IOException, ProviderException {
        Properties prop = new Properties();
        prop.load(new FileInputStream(propFile));
        WikiPage wikiPage = new WikiPage(pageName);
        wikiPage.setAuthor((String) prop.get("author"));
        String pageText = getPageText(wikiPage, pageDir + "/" + page);
        provider.putPageText(wikiPage, pageText);
    }

    private String getPageText(WikiPage page, String fileName) throws IOException {
        File file = new File(fileName);
        Date createdDate = new Date(file.lastModified());
        page.setCreatedTime(createdDate);
        page.setLastModified(createdDate);
        FileInputStream reader = new FileInputStream(file);
        ByteArrayOutputStream writer = new ByteArrayOutputStream();
        Utils.copyInput2Output(reader, writer);
        return new String(writer.toByteArray(), "UTF-8");
    }
}
