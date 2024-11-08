package com.cotrino.feed2ereader;

import java.io.*;
import java.util.Date;
import java.text.DateFormat;

/**
 * Feed2reader is a command line tool to convert RSS/Atom feeds
 * into MobiPocket books, compatible with many popular eReaders
 * in three stages:
 * <ul>
 * <li>Read the OPML file to find the feeds</li>
 * <li>Download each feed file and parse with Yarfraw</li>
 * <li>Convert each feed to HTML, downloading all its images</li>
 * <li>Convert the HTML into MobiPocket using the Mobiperl suite</li>
 * </ul>
 * 
 * @author      Jose Cotrino
 * @version     %I%, %G%
 * @since       1.0
 */
public class Feed2ereader {

    public static String tempFolder = "./news";

    private static String html2mobi = "html2mobi";

    /** 
     * Main function.
     *
     * @param opmlFile		An OPML file containing the feeds to be synchronized
     * @param eReaderPath	Path to eReader device
     * 						(e.g. F:/eBooks, /media/Cybook/eBooks)
     * @param html2mobi		Executable of the html2mobi tool of the Mobiperl suite
     * @param proxy			Address of the proxy server, if any (optional)
     * @param proxyPort		Port of the proxy server, if any (optional) 
     * @return          <code>void</code>
     * @since           1.0
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Syntax: java -jar Feed2ereader.jar " + "opmlFile eReaderPath html2mobiExecutable [proxy] [proxyPort]");
            System.err.println("Examples:\n" + "java -jar Feed2ereader.jar " + "opml.xml F:/eBooks .\\mobiperl\\html2mobi.exe\n" + "java -jar Feed2ereader.jar " + "google-reader-subscriptions.xml /media/Cybook/eBooks /usr/local/bin/html2mobi myproxy.com 81");
            System.exit(0);
        }
        String opmlFile = args[0];
        String path = args[1];
        html2mobi = args[2];
        if (args.length >= 5) {
            String proxy = args[3];
            String proxyPort = args[4];
            System.getProperties().put("proxySet", "true");
            System.getProperties().put("proxyHost", proxy);
            System.getProperties().put("proxyPort", proxyPort);
        }
        Date now = new Date();
        DateFormat df = DateFormat.getDateInstance();
        String today = df.format(now);
        OpmlReader opml = new OpmlReader(opmlFile);
        for (String[] feed : opml.getFeeds()) {
            HttpFeed feedChannel = new HttpFeed(feed[0], feed[1]);
            String file = feedChannel.getTitle();
            HtmlWriter htmlWriter = new HtmlWriter(feedChannel.getChannel(), file, Feed2ereader.tempFolder, feed[2], today);
            if (toMobi(htmlWriter.getFile(), path + "/" + file + ".mobi")) {
                try {
                    File thumbnail = new File(path + "/" + file + "_6090.t2b");
                    if (thumbnail.exists()) {
                        thumbnail.delete();
                    }
                } catch (SecurityException e) {
                    System.out.println("Thumbnail could not be deleted");
                }
                System.out.println("=> " + file + " conversion succeeded!");
            } else {
                System.out.println("=> " + file + " conversion failed!");
            }
        }
        System.out.println("Synchronization finished!");
    }

    /** 
     * System call to the Mobiperl tools to convert from HTML to Mobi.
     * For this call, the given command line argument is used.
     *
     * @param html		Path to the HTML file
     * @param mobi		Path to the Mobi file
     * @return          <code>void</code>
     * @since           1.0
     */
    public static boolean toMobi(String html, String mobi) {
        boolean converted = false;
        try {
            if (html2mobi.indexOf(" ") != -1) {
                html2mobi = "\"" + html2mobi + "\"";
            }
            System.out.println("Converting to MobiPocket...");
            String[] command = new String[] { html2mobi, "--mobifile", mobi, "--imagerescale", "1", html };
            for (int i = 0; i < command.length; i++) {
                System.out.print(command[i] + " ");
            }
            System.out.println();
            Process p = Runtime.getRuntime().exec(command);
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String line = null;
            while ((line = input.readLine()) != null) {
                System.out.println(line);
            }
            input.close();
            int exitValue = p.waitFor();
            if (exitValue == 0) {
                converted = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return converted;
    }
}
