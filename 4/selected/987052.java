package com.cotrino.feed2ereader;

import java.io.File;
import yarfraw.core.datamodel.ChannelFeed;

/**
 * HttpFeed is a class to download the RSS/Atom XML file of a feed.
 * 
 * @author      Jose Cotrino
 * @version     %I%, %G%
 * @since       1.0
 */
public class HttpFeed {

    ChannelFeed channel = null;

    /** 
     * The constructor is doing all the action. It will:
     * <ul>
     * <li>Download the XML file using HttpGet</li>
     * <li>Parse the feed using FileFeed</li>
     * </ul>
     *
     * @param name		Feed name, which will be used as file name
     * @param url		HTTP link to the RSS/Atom XML file
     * @return          <code>HttpFeed</code>
     * @see				HttpGet
     * @see				FileFeed
     * @since           1.0
     */
    public HttpFeed(String name, String url) {
        String inputFile = escapeFilename(name);
        try {
            File folder = new File(Feed2ereader.tempFolder);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String cacheFile = "./news/" + inputFile + ".xml";
            System.out.println("Downloading \"" + name + "\" at " + url + "...");
            if (HttpGet.cache(cacheFile, url)) {
                FileFeed fileFeed = new FileFeed(cacheFile);
                this.channel = fileFeed.getChannel();
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
	 * Substitute any non-ASCII characters into '_'
	 * @param s		String to be escaped
	 * @return		Escaped string
	 */
    public static final String escapeFilename(String s) {
        StringBuffer sb = new StringBuffer();
        int n = s.length();
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            if (c > 127 || c == ' ' || c == '\'' || c == ':' || c == '\\' || c == '/' || c == '?') {
                sb.append("_");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public String getTitle() {
        return escapeFilename(this.channel.getTitleText());
    }

    public ChannelFeed getChannel() {
        return channel;
    }
}
