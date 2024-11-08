package com.uusee.crawler.job.baike;

import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import com.uusee.crawler.dbwriter.BaikeDataSourceDbWriter;
import com.uusee.crawler.fetcher.FetchHTTP;
import com.uusee.crawler.model.CrawlStatusCodes;
import com.uusee.crawler.model.CrawlURI;
import com.uusee.crawler.pageprocessor.baike.BaikeDataSourcePageProcessor;
import com.uusee.framework.bo.UniversalBo;
import com.uusee.shipshape.bk.Constants;
import com.uusee.shipshape.bk.model.BaikeDataSource;
import com.uusee.util.StringUtils;

public class DoubanFinalPageCrawlJob {

    private static Log log = LogFactory.getLog(DoubanFinalPageCrawlJob.class);

    FetchHTTP fetch = new FetchHTTP();

    BaikeDataSourcePageProcessor overviewPageProcessor;

    BaikeDataSourceDbWriter dbWriter = new BaikeDataSourceDbWriter();

    private String tmpDir = System.getProperty("java.io.tmpdir");

    private int start;

    private int end;

    public void doExecute() {
        try {
            for (int i = start; i >= end; i--) {
                try {
                    crawl(createCrawlURI(i + ""));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

    public CrawlURI createCrawlURI(String id) {
        CrawlURI crawlURI = new CrawlURI();
        String crawlUrl = "http://www.douban.com/subject/" + id + "/";
        crawlURI.setSourceSite("www.douban.com");
        crawlURI.setCrawlUrl(crawlUrl);
        crawlURI.setCookieFlag(true);
        String cookiesFile = "report=; f=in_tableh; viewed=\"3752893\"; __gads=ID=f3998cbec8a76e79:T=1248962509:S=ALNI_Mb6-HVIWS4V32a572ZHBsY4aiRkTg; ll=\"118112\"; __utmv=164037162.3083756; __utmz=164037162.1249104253.5.4.utmcsr=tongji.alimama.com|utmccn=(referral)|utmcmd=referral|utmcct=/hostfrom.html; __utma=164037162.906402469579825000.1247146609.1250732807.1250822253.20; bid=\"ngR/5VE77QE\"; ue=\"xiaochawan@gmail.com\"; __utmb=164037162.11.10.1250822253; __utmc=164037162";
        crawlURI.setCookiesFile(cookiesFile);
        return crawlURI;
    }

    public void crawl(CrawlURI crawlURI) {
        try {
            fetch.process(crawlURI);
            overviewPageProcessor.process(crawlURI);
            if (crawlURI.getCrawlStatus() == CrawlStatusCodes.PAGE_PROCESS_SUCCESS) {
                BaikeDataSource baike = (BaikeDataSource) crawlURI.getModel();
                baike.setOriHtmlUrl(crawlURI.getCrawlUrl());
                String area = baike.getArea();
                if (StringUtils.isNotEmpty(area)) {
                    area = area.replaceAll("\\s*/\\s*", ",");
                }
                String language = baike.getLanguage();
                if (StringUtils.isNotEmpty(language)) {
                    language = language.replaceAll("\\s*/\\s*", ",");
                }
                String channelName = baike.getChannelName();
                String tags = baike.getTags();
                if ("电影".equalsIgnoreCase(channelName)) {
                    baike.setArea(area);
                    baike.setLanguage(language);
                    if (tags != null && tags.indexOf("动漫") >= 0) {
                        baike.setChannelCode(Constants.CHANNEL_CODE_ANIME);
                    } else if (tags != null && tags.indexOf("电视剧") >= 0) {
                        baike.setChannelCode(Constants.CHANNEL_CODE_TELEPLAY);
                    }
                    dbWriter.process(crawlURI);
                } else if ("专辑".equalsIgnoreCase(channelName)) {
                    baike.setChannelName("音乐");
                    baike.setChannelCode(Constants.CHANNEL_CODE_MUSIC);
                    dbWriter.process(crawlURI);
                }
                Thread.sleep(5000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setFetch(FetchHTTP fetch) {
        this.fetch = fetch;
    }

    public void setOverviewPageProcessor(BaikeDataSourcePageProcessor overviewPageProcessor) {
        this.overviewPageProcessor = overviewPageProcessor;
    }

    public void setDbWriter(BaikeDataSourceDbWriter dbWriter) {
        this.dbWriter = dbWriter;
    }

    public static void main(String[] args) {
        try {
            ApplicationContext acx = new ClassPathXmlApplicationContext("applicationContext.xml");
            UniversalBo universalBo = (UniversalBo) acx.getBean("universalBo");
            BaikeDataSourceDbWriter dbWriter = new BaikeDataSourceDbWriter();
            dbWriter.setUniversalBo(universalBo);
            InputStream in = ClassLoader.getSystemResourceAsStream("regex/www.douban.com/overview.properties");
            Properties processPageRegex = new Properties();
            processPageRegex.load(in);
            BaikeDataSourcePageProcessor pageProcessor = new BaikeDataSourcePageProcessor(processPageRegex);
            DoubanFinalPageCrawlJob job = new DoubanFinalPageCrawlJob();
            job.dbWriter = dbWriter;
            job.overviewPageProcessor = pageProcessor;
            job.start = 3747726;
            job.end = 2000000;
            job.doExecute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
