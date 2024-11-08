package be.roam.drest.bloglines;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import java.util.List;
import org.xml.sax.SAXException;
import be.roam.drest.service.bloglines.BloglinesEntry;
import be.roam.drest.service.bloglines.BloglinesEntryList;
import be.roam.drest.service.bloglines.BloglinesService;
import be.roam.drest.service.bloglines.BloglinesSite;
import be.roam.drest.service.bloglines.BloglinesSubscription;

public class BloglinesReader {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private BloglinesService service;

    private String userName;

    public BloglinesReader(String userName, String password) {
        this.userName = userName;
        service = new BloglinesService(userName, password);
    }

    public void process(Writer writer) throws IOException, SAXException {
        Date now = new Date();
        writer.write("Bloglines status for " + userName + " at " + now + LINE_SEPARATOR);
        writer.write("------------------------------------------------------------------------------" + LINE_SEPARATOR);
        writer.write("Subscriptions:" + LINE_SEPARATOR);
        BloglinesSubscription subscription = service.getSubscriptions();
        printSubscription(writer, subscription, 0);
        writer.write("------------------------------------------------------------------------------" + LINE_SEPARATOR);
        writer.write("Number of unread items: " + service.getNrUnreadMessages() + LINE_SEPARATOR);
        writer.write("------------------------------------------------------------------------------" + LINE_SEPARATOR);
        writer.write("Unread items: " + LINE_SEPARATOR);
        printEntries(writer, subscription);
    }

    private void printEntries(Writer writer, BloglinesSubscription subscription) throws IOException, SAXException {
        writer.write(subscription.getTitle() + (subscription.isFolder() ? "" : " (" + subscription.getUrlHtml() + " | " + subscription.getUrlXml() + ")") + LINE_SEPARATOR);
        List<BloglinesSubscription> topSubscriptionList = subscription.getSubscriptionList();
        if (topSubscriptionList != null && !topSubscriptionList.isEmpty()) {
            for (BloglinesSubscription topSubscription : topSubscriptionList) {
                if (topSubscription.isFolder()) {
                    writer.write("Fetching entries for " + topSubscription.getTitle() + LINE_SEPARATOR);
                    BloglinesEntryList entryList = service.getItems(topSubscription.getId(), false, null);
                    if (entryList != null) {
                        List<BloglinesSite> siteList = entryList.getSiteList();
                        if (siteList != null && !siteList.isEmpty()) {
                            for (BloglinesSite site : siteList) {
                                List<BloglinesEntry> itemList = site.getItemList();
                                if (itemList != null && !itemList.isEmpty()) {
                                    writer.write("Entries for " + topSubscription.getTitle() + LINE_SEPARATOR);
                                    for (BloglinesEntry item : itemList) {
                                        writer.write(getTabs(1) + item.getTitle() + " (" + item.getLink() + ")" + LINE_SEPARATOR);
                                    }
                                }
                            }
                        }
                    } else {
                        writer.write("No entries for " + topSubscription.getTitle() + LINE_SEPARATOR);
                    }
                }
            }
        }
    }

    private void printSubscription(Writer writer, BloglinesSubscription subscription, int nrTabs) throws IOException {
        writer.write(getTabs(nrTabs) + subscription.getTitle() + (subscription.isFolder() ? "" : " (" + subscription.getUrlHtml() + " | " + subscription.getUrlXml() + ")") + " " + subscription.getId() + LINE_SEPARATOR);
        List<BloglinesSubscription> entryList = subscription.getSubscriptionList();
        if (entryList != null && !entryList.isEmpty()) {
            for (BloglinesSubscription subSubscription : entryList) {
                printSubscription(writer, subSubscription, nrTabs + 1);
            }
        }
    }

    private String getTabs(int nrTabs) {
        String tabs = "";
        for (int loop = 0; loop < nrTabs; ++loop) {
            tabs += "   ";
        }
        return tabs;
    }

    public static void main(String[] args) {
        Writer writer = null;
        try {
            if (args.length < 2) {
                showUsage();
                return;
            }
            String userName = args[0];
            String password = args[1];
            writer = new OutputStreamWriter(System.out);
            if (args.length > 2) {
                writer = new FileWriter(args[2]);
            }
            new BloglinesReader(userName, password).process(writer);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void showUsage() {
        System.out.println("Arguments: <username> <password> [<target file name>]");
    }
}
