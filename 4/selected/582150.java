package org.gudy.azureus2.pluginsimpl.local.utils.xml.rss;

import java.io.InputStream;
import java.util.*;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.plugins.utils.Utilities;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloader;
import org.gudy.azureus2.plugins.utils.resourcedownloader.ResourceDownloaderException;
import org.gudy.azureus2.plugins.utils.xml.rss.RSSChannel;
import org.gudy.azureus2.plugins.utils.xml.rss.RSSFeed;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocument;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentException;
import org.gudy.azureus2.plugins.utils.xml.simpleparser.SimpleXMLParserDocumentNode;

/**
 * @author parg
 *
 */
public class RSSFeedImpl implements RSSFeed {

    private boolean is_atom;

    private RSSChannel[] channels;

    public RSSFeedImpl(Utilities utilities, ResourceDownloader downloader) throws ResourceDownloaderException, SimpleXMLParserDocumentException {
        this(utilities, downloader.download());
    }

    public RSSFeedImpl(Utilities utilities, InputStream is) throws SimpleXMLParserDocumentException {
        try {
            SimpleXMLParserDocument doc = utilities.getSimpleXMLParserDocumentFactory().create(is);
            String doc_name = doc.getName();
            is_atom = doc_name != null && doc_name.equalsIgnoreCase("feed");
            List chans = new ArrayList();
            if (is_atom) {
                chans.add(new RSSChannelImpl(doc, true));
            } else {
                SimpleXMLParserDocumentNode[] xml_channels = doc.getChildren();
                for (int i = 0; i < xml_channels.length; i++) {
                    SimpleXMLParserDocumentNode xml_channel = xml_channels[i];
                    String name = xml_channel.getName().toLowerCase();
                    if (name.equals("channel")) {
                        chans.add(new RSSChannelImpl(xml_channel, false));
                    }
                }
            }
            channels = new RSSChannel[chans.size()];
            chans.toArray(channels);
        } finally {
            try {
                is.close();
            } catch (Throwable e) {
                Debug.printStackTrace(e);
            }
        }
    }

    public boolean isAtomFeed() {
        return (is_atom);
    }

    public RSSChannel[] getChannels() {
        return (channels);
    }
}
