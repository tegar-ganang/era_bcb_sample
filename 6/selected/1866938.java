package edu.upf.da.p2p.sm;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import edu.upf.da.p2p.objects.DescriptionContent;
import edu.upf.da.p2p.sm.client.P2PClient;
import edu.upf.da.p2p.sm.client.message.SearchRequest;

public class TestClient {

    private static final Log log = LogFactory.getLog(TestClient.class);

    public static void main(String[] args) throws XMPPException, InterruptedException {
        SetLogForTest.set();
        P2PClient p2p = new P2PClient();
        p2p.connect();
        p2p.getAuthManager().login("Murray", "Muahahahahahahahahahahah");
        Thread.sleep(5000);
        List<DescriptionContent> results = p2p.getSearchResultManager(new SearchRequest()).getResults();
        Thread.sleep(5000);
        System.out.println(results.size() + " resultados.");
        for (DescriptionContent dc : results) {
            System.out.println(dc.getUserContent().getURL());
        }
        p2p.getAuthManager().logout();
        p2p.disconnect();
    }
}
