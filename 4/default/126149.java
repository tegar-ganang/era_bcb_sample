public class CrawlThread extends Thread {

    private Queue nodeQueue, writeQueue, allHosts;

    int timeout;

    int name;

    public CrawlThread(Queue node, Queue write, Queue hosts, int t, int n) {
        nodeQueue = node;
        writeQueue = write;
        allHosts = hosts;
        timeout = t;
        name = n;
    }

    public void run() {
        String ultraPeer;
        while (true) {
            if (Thread.interrupted()) {
                return;
            }
            try {
                ultraPeer = (String) nodeQueue.dequeue();
            } catch (InterruptedException e) {
                return;
            }
            String uhost = ultraPeer.split(":")[0];
            int uport = Integer.parseInt(ultraPeer.split(":")[1]);
            System.out.println("Thread " + name + " is Crawling " + uhost + ":" + uport);
            Crawler crawler = new Crawler();
            CrawlResult crawlResult = crawler.crawl(uhost, uport, timeout);
            if (crawlResult == null) {
                continue;
            }
            writeQueue.enqueue(crawlResult);
            if (crawlResult.getUltrapeers() != null) {
                String[] ultrapeers = { crawlResult.getUltrapeers() };
                if (ultrapeers[0].contains(",")) {
                    ultrapeers = ultrapeers[0].split(",");
                }
                for (int i = 0; i < ultrapeers.length; i++) {
                    if (!ultrapeers[i].isEmpty() && !allHosts.contains(ultrapeers[i])) {
                        nodeQueue.enqueue(ultrapeers[i]);
                        allHosts.enqueue(ultrapeers[i]);
                    }
                }
            }
            if (crawlResult.getLeaves() != null) {
                String[] leaveNodes = { crawlResult.getLeaves() };
                if (leaveNodes[0].contains(",")) {
                    leaveNodes = leaveNodes[0].split(",");
                }
                for (int i = 0; i < leaveNodes.length; i++) {
                    if (!leaveNodes[i].isEmpty() && !allHosts.contains(leaveNodes[i])) {
                        nodeQueue.enqueue(leaveNodes[i]);
                        allHosts.enqueue(leaveNodes[i]);
                    }
                }
            }
        }
    }
}
