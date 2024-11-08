package com.jradar.manager;

import com.jradar.reader.RssReader;
import com.jradar.service.NewsAggregator;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import de.nava.informa.core.ChannelIF;
import de.nava.informa.exporters.RSS_2_0_Exporter;

/**
 * Rss reader manager facilitates working with rss sources
 * @author Alexander Kirin
 */
public class RssReaderManager {

    List<RssReader> readers = new ArrayList();

    String exportFilePath = "c:/rss2.xml";

    /**
     * registers rss sources being fed
     * @param reader
     */
    public void registerReader(RssReader reader) {
        readers.add(reader);
    }

    /**
     * gets all rss channels
     * @return
     */
    public List<RssReader> getReaders() {
        return readers;
    }

    public ChannelIF getAggregatedChannel() {
        NewsAggregator aggregator = new NewsAggregator();
        for (int i = 0; i < readers.size(); i++) {
            RssReader rssReader = readers.get(i);
            aggregator.addChannel(rssReader.getChannel());
        }
        return aggregator.getAggregatedChannel();
    }

    /**
     * aggregates and exports news from all rss sources, presented by rss readers,
     * to rss ver. 2.0
     */
    public void exportToRss20() {
        try {
            RSS_2_0_Exporter exporter = new RSS_2_0_Exporter(exportFilePath);
            exporter.write(getAggregatedChannel());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
