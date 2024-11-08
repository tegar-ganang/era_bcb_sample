import java.util.ArrayList;
import java.util.List;

public class CrawlMaster extends Thread {

    int timeout = 30;

    int numThreads = 30;

    ObjectWriter crawlWriter;

    Queue allHosts = new Queue();

    Queue nodeQueue = new Queue();

    Queue writeQueue = new Queue();

    List<CrawlThread> threads = new ArrayList<CrawlThread>();

    public CrawlMaster(String addr, int port, int t, String path) {
        timeout = t;
        crawlWriter = new ObjectWriter(path);
        nodeQueue.enqueue(addr + ":" + port);
    }

    public void run() {
        System.out.println("Starting " + numThreads + " Threads...");
        for (int i = 0; i < numThreads; i++) {
            threads.add(new CrawlThread(nodeQueue, writeQueue, allHosts, timeout, i));
        }
        for (int i = 0; i < numThreads; i++) {
            threads.get(i).start();
        }
        CrawlResult crawlResult;
        while (true) {
            try {
                crawlResult = (CrawlResult) writeQueue.dequeue();
                crawlWriter.write(crawlResult);
            } catch (InterruptedException e) {
                for (int i = 0; i < numThreads; i++) {
                    threads.get(i).interrupt();
                }
                crawlWriter.close();
                return;
            }
        }
    }
}
