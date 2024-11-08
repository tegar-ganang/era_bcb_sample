package com.uusee.crawler.fastwriter;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import com.fastsearch.esp.content.DocumentFactory;
import com.fastsearch.esp.content.DocumentFeederFactory;
import com.fastsearch.esp.content.DuplicateElementException;
import com.fastsearch.esp.content.FactoryException;
import com.fastsearch.esp.content.IDocument;
import com.fastsearch.esp.content.IDocumentFeeder;
import com.fastsearch.esp.content.IDocumentFeederFactory;
import com.fastsearch.esp.content.IDocumentFeederStatus;
import com.fastsearch.esp.content.errors.DocumentError;
import com.fastsearch.esp.content.errors.DocumentWarning;
import com.fastsearch.esp.content.util.Pair;
import com.uusee.crawler.framework.Processor;
import com.uusee.crawler.model.CrawlStatusCodes;
import com.uusee.crawler.model.CrawlURI;
import com.uusee.framework.util.query.CriteriaInfo;
import com.uusee.shipshape.sp.model.Ugc;

public class UgcFastWriter extends Processor {

    IDocumentFeederFactory factory = null;

    IDocumentFeeder feeder = null;

    String cdLocation = "search6.uusee.com:16100";

    boolean liveCallbackEnabled = true;

    String collection;

    public UgcFastWriter(String collection) {
        try {
            Properties p = new Properties();
            p.put("com.fastsearch.esp.content.http.contentdistributors", cdLocation);
            factory = DocumentFeederFactory.newInstance(p);
            this.collection = collection;
            feeder = factory.createDocumentFeeder(collection, null);
            feeder.setMaxDocsInBatch(100);
            feeder.setMaxRetries(1);
            feeder.setDocumentTimeoutMin(1);
            feeder.setBatchSubmissionTimeoutSec(30);
        } catch (FactoryException e) {
            System.out.println("An error occured during creation of Document Feeder " + e);
        }
    }

    protected void innerProcess(CrawlURI crawlURI) {
        Ugc ugc = (Ugc) crawlURI.getModel();
        String playUrl = ugc.getPlayUrl();
        String videoSite = ugc.getVideoSite();
        String vid = ugc.getVid();
        String evid = ugc.getEvid();
        int length = ugc.getLength();
        if (playUrl.endsWith("/")) {
            playUrl = playUrl.substring(0, playUrl.length() - 1);
        }
        CriteriaInfo ci = new CriteriaInfo();
        ci.eq("playUrl", playUrl);
        List<Ugc> ugcList = null;
        if (!canProcess(crawlURI)) {
            if (isInvalidPage(crawlURI)) {
            }
            return;
        }
        try {
            IDocument doc = DocumentFactory.newDocument(ugc.getPlayUrl() + "");
            try {
                doc.addElement(DocumentFactory.newString("vid", ugc.getId() + ""));
                doc.addElement(DocumentFactory.newString("url", ugc.getPlayUrl() + ""));
                doc.addElement(DocumentFactory.newString("vvid", ugc.getVid()));
                doc.addElement(DocumentFactory.newString("vevid", ugc.getEvid()));
                doc.addElement(DocumentFactory.newString("vtitle", ugc.getTitle()));
                doc.addElement(DocumentFactory.newString("vlogo", ugc.getLogo()));
                doc.addElement(DocumentFactory.newString("vplayurl", ugc.getPlayUrl()));
                doc.addElement(DocumentFactory.newInteger("vlength", ugc.getLength()));
                doc.addElement(DocumentFactory.newString("vchannel", ugc.getChannel()));
                doc.addElement(DocumentFactory.newString("vcategorys", ugc.getCategory()));
                doc.addElement(DocumentFactory.newString("vtags", ugc.getTags()));
                doc.addElement(DocumentFactory.newString("vsourcesite", ugc.getSourceSite()));
                doc.addElement(DocumentFactory.newString("vuploaduserid", ugc.getUploadUserid()));
                doc.addElement(DocumentFactory.newString("vuploadusername", ugc.getUploadUsername()));
                doc.addElement(DocumentFactory.newDate("uuseeupdatedate", ugc.getUpdateDate()));
            } catch (DuplicateElementException e) {
            }
            long operationId = feeder.addDocument(doc);
            feeder.waitForCompletion();
            IDocumentFeederStatus status = feeder.getStatusReport();
            printStatus(status);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Print content in status report object returned from
	 * {@link IDocumentFeeder#getStatusReport()}.
	 * 
	 * @param statusReport
	 *            status report containing errors and warnings given for
	 *            documents feeded to FAST ESP
	 */
    public static void printStatus(IDocumentFeederStatus statusReport) {
        System.out.println("Number of errors in documents submitted to FAST ESP: " + statusReport.numDocumentErrors());
        Iterator it = statusReport.getAllDocumentErrors().iterator();
        while (it.hasNext()) {
            Pair elem = (Pair) it.next();
            DocumentError docError = (DocumentError) elem.getSecond();
            Long operationId = (Long) elem.getFirst();
            System.out.println("Operation id:" + operationId + " Error:" + docError.toString());
        }
        System.out.println("Number of warnings in documents submitted to FAST ESP: " + statusReport.numDocumentErrors());
        it = statusReport.getDocumentWarnings().iterator();
        while (it.hasNext()) {
            Pair elem = (Pair) it.next();
            DocumentWarning docWarning = (DocumentWarning) elem.getSecond();
            Long operationId = (Long) elem.getFirst();
            System.out.println("Operation id:" + operationId + " Warning:" + docWarning.toString());
        }
    }

    private boolean canProcess(CrawlURI crawlURI) {
        int crawlStatus = crawlURI.getCrawlStatus();
        if (crawlStatus == CrawlStatusCodes.PAGE_PROCESS_SUCCESS) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isInvalidPage(CrawlURI crawlURI) {
        int crawlStatus = crawlURI.getCrawlStatus();
        if (crawlStatus == CrawlStatusCodes.PAGE_PROCESS_INVALID) {
            return true;
        } else {
            return false;
        }
    }
}
