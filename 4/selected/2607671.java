package com.uusee.crawler.job.baike;

import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import com.uusee.crawler.dbwriter.BaikeDbWriter;
import com.uusee.crawler.fetcher.FetchHTTP;
import com.uusee.crawler.model.CrawlStatusCodes;
import com.uusee.crawler.model.CrawlURI;
import com.uusee.crawler.pageprocessor.baike.BaikePageProcessor;
import com.uusee.crawler.pageprocessor.baike.douban.DoubanSearchResultPageProcessor;
import com.uusee.crawler.xmlwriter.BaikeXmlWriter;
import com.uusee.framework.bo.UniversalBo;
import com.uusee.framework.util.query.CriteriaInfo;
import com.uusee.framework.util.query.Sort;
import com.uusee.shipshape.bk.model.Baike;
import com.uusee.util.StringUtils;

public class DoubanSearchResultPageCrawlJob {

    private static Log log = LogFactory.getLog(DoubanSearchResultPageCrawlJob.class);

    FetchHTTP fetch = new FetchHTTP();

    DoubanSearchResultPageProcessor searchResultPageProcessor = new DoubanSearchResultPageProcessor();

    DoubanFinalPageCrawlJob doubanFinalPageCrawlJob = new DoubanFinalPageCrawlJob();

    UniversalBo universalBo;

    BaikeXmlWriter xmlwriter = new BaikeXmlWriter();

    public void doExecute() {
        try {
            CriteriaInfo ci = new CriteriaInfo();
            int count = 50;
            log.info("百科数:" + count);
            int start = 0;
            int offset = 50;
            while (start < count) {
                ci = new CriteriaInfo();
                ci.eq("channelCode", "teleplay");
                ci.setOffset(true);
                ci.setFirstResult(start);
                ci.setMaxResult(offset);
                ci.addSort("year", Sort.DESC);
                List<Baike> bkList = universalBo.getEntitiesByCriteriaInfo(Baike.class, ci);
                for (Baike bk : bkList) {
                    long id = bk.getId();
                    String name = bk.getName();
                    String director = getFirstValue(bk.getDirectors());
                    String star = getFirstValue(bk.getStars());
                    try {
                        StringBuffer sb = new StringBuffer();
                        sb.append(name);
                        String keyword = URLEncoder.encode(sb.toString(), "utf-8");
                        String crawlUrl = "http://www.douban.com/subject_search?cat=1002&search_text=" + keyword;
                        CrawlURI crawlURI = new CrawlURI();
                        crawlURI.setSourceSite("www.douban.com");
                        crawlURI.setCrawlUrl(crawlUrl);
                        crawl(bk, crawlURI);
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error(id + "-" + name + "-豆瓣匹配开始失败", e);
                    }
                }
                log.info("finished:" + (start + offset));
                start = start + offset;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getFirstValue(String values) {
        String firstvalue = "";
        if (StringUtils.isNotEmpty(values)) {
            firstvalue = values.split(",")[0];
        }
        return firstvalue;
    }

    public void crawl(Baike bk, CrawlURI crawlURI) {
        try {
            fetch.process(crawlURI);
            searchResultPageProcessor.process(crawlURI);
            if (crawlURI.getCrawlStatus() == CrawlStatusCodes.PAGE_PROCESS_SUCCESS) {
                List<CrawlURI> finalCrawlURIList = (List<CrawlURI>) crawlURI.getModel();
                if (finalCrawlURIList.size() > 1) {
                    log.info("匹配数：" + finalCrawlURIList.size());
                }
                CrawlURI finalCrawlURI = finalCrawlURIList.get(0);
                Baike baike = (Baike) finalCrawlURI.getModel();
                doubanFinalPageCrawlJob.crawl(finalCrawlURI);
                if (finalCrawlURI.getCrawlStatus() == CrawlStatusCodes.PAGE_PROCESS_SUCCESS) {
                    String area = baike.getArea();
                    if (StringUtils.isNotEmpty(area)) {
                        area = area.replaceAll("\\s*/\\s*", ",");
                    }
                    String language = baike.getLanguage();
                    if (StringUtils.isNotEmpty(language)) {
                        language = language.replaceAll("\\s*/\\s*", ",");
                    }
                    String channelName = baike.getChannelName();
                    if ("电影".equalsIgnoreCase(channelName)) {
                        baike.setArea(area);
                        baike.setLanguage(language);
                        baike.setId(bk.getId());
                        baike.setChannelCode(bk.getChannelCode());
                        xmlwriter.process(finalCrawlURI);
                        log.info("时光网百科：" + bk.getName() + "-" + bk.getDirectors() + "-" + bk.getStars());
                        log.info("豆瓣百科：" + baike.getName() + "-" + baike.getDirectors() + "-" + baike.getStars());
                    } else {
                        log.info("不是电影");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            ApplicationContext acx = new ClassPathXmlApplicationContext("applicationContext.xml");
            UniversalBo universalBo = (UniversalBo) acx.getBean("universalBo");
            DoubanFinalPageCrawlJob finalPageCrawljob = new DoubanFinalPageCrawlJob();
            BaikeDbWriter dbWriter = new BaikeDbWriter();
            dbWriter.setUniversalBo(universalBo);
            InputStream in = ClassLoader.getSystemResourceAsStream("regex/www.douban.com/overview.properties");
            Properties processPageRegex = new Properties();
            processPageRegex.load(in);
            BaikePageProcessor overviewPageProcessor = new BaikePageProcessor(processPageRegex);
            InputStream in2 = ClassLoader.getSystemResourceAsStream("regex/www.douban.com/searchResultPage.properties");
            Properties processPageRegex2 = new Properties();
            processPageRegex2.load(in2);
            DoubanSearchResultPageProcessor searchResultPageProcessor = new DoubanSearchResultPageProcessor();
            searchResultPageProcessor.setPageProcessRegex(processPageRegex2);
            DoubanSearchResultPageCrawlJob job = new DoubanSearchResultPageCrawlJob();
            job.searchResultPageProcessor = searchResultPageProcessor;
            job.doubanFinalPageCrawlJob = finalPageCrawljob;
            job.universalBo = universalBo;
            job.doExecute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
