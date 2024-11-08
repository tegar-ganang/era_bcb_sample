package com.cidero.util;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import com.cidero.http.*;
import com.cidero.upnp.*;

/**
 * Describe class <code>ShoutcastOutputStream</code> here.
 *
 * This class allows a user to snoop a 'shoutcast' stream to see what
 * the HTTP headers and metadata look like
 *
 * Shoutcast is a slightly modified HTTP-GET protocol that is used by a 
 * number of Internet radio stations. The basics are:
 *
 *  1. When a client is interested in receiving shoutcast data in addition
 *     to the basic MP3 (or other format) data, the client includes a
 *     line in the HTTP request header like:
 *
 *       Icy-Metadata: 1
 *
 *  2. A shoutcast capable server sees the header, and enables the periodic
 *     insertion of shoutcast data. The HTTP response contains a header line
 *     like:
 *
 *       icy-metaint: 8192
 *
 *     which tells the client how often to expect a shoutcast data block
 *     (every 8192 bytes in this case)
 *
 *    Other optional headers (from ample.sourceforge.net) are:
 *
 *     icy-notice1   Informational message 
 *     icy-notice2   2nd informational message 
 *     icy-name      Name of the stream that server is sending.
 *                   (Station Name). 
 *
 *                   Note: Looks like Audiotron puts this momentarily on 
 *                   top line of it's screen at start of playback (then
 *                   switches to song/title
 *                   
 *     icy-genre     Genre of stream
 *     icy-url       Url associated for stream
 *                   (e.g. http://www.radioparadise.com)
 *     icy-pub       Not sure, believe it indicates if stream is public
 *                   or private. Use '1' for true 
 *     icy-br        Bit rate. This is for informational purposes, 
 *                   since most client decoders support VBR. Value
 *                   for 128K station is 128
 *     icy-irc       Some kind of format indication. RadioParadise sets
 *                   this to '#shoutcast'
 *     icy-icq       Unknown. RadioParadise sets this to 0
 *     icy-aim       Unknown. RadioParadise sets this to 'N/A'
 * 
 *
 *     Also found references to (in sourceforge javashout code):
 * 
 *     icy-desc      Stream description
 *
 *
 *  3. The server then inserts shoutcast metadata every 8192 bytes. The
 *     first data byte is a byte count of the data to follow, divided by
 *     16. In most cases, no new data is pending, so only a single 0 byte
 *     is added.  When there is new data (typically a song title change), 
 *     a string like:
 *       
 *      StreamTitle='The White Stripes - Button To Button"
 *
 *     is added, and the byte count reflects the string length (rounded
 *     up to the next multiple of 16 bytes)
 *
 *  That's it...simple!
 *
 */
public class ShoutcastSnooper {

    String url;

    String userAgent = "CideroRadio/1.0";

    int metadataInterval;

    byte[] metadataByteArray;

    long maxDataBytes = -1;

    /**
   * Creates a new <code>ShoutcastOutputStream</code> instance.
   *
   */
    public ShoutcastSnooper() {
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setMaxDataBytes(long maxDataBytes) {
        this.maxDataBytes = maxDataBytes;
    }

    /**
   *  Read radio station data from a playlist resource. Leverages 
   *  playlist loader in CDSResource class...
   *
   */
    public void readPlaylist(CDSResource playlistResource, PrintWriter writer) throws MalformedURLException {
        System.out.println("  Reading playlist " + playlistResource.getName());
        CDSObjectList objList = playlistResource.getPlaylistItems();
        if (objList == null) {
            writer.println("  Status: ERROR (couldn't access playlist)");
            writer.flush();
            return;
        }
        for (int n = 0; n < objList.size(); n++) {
            CDSItem item = (CDSItem) objList.getObject(n);
            CDSResource streamResource = item.getResource(0);
            writer.println("  Trying playlist stream " + n + ": " + item.getTitle());
            try {
                if (readURL(streamResource.getName(), writer)) {
                    writer.println("  Status: OK");
                    writer.flush();
                    return;
                }
            } catch (Exception e) {
                System.out.println("Exception processing URL " + streamResource.getName() + " " + e);
            }
        }
        writer.println("  Status: ERROR");
        writer.flush();
    }

    /** 
   * Read data from  URL
   *
   */
    public boolean readURL(String urlString, PrintWriter writer) throws MalformedURLException {
        System.out.println("!!!!!!!!!!Session: readAndQueueURL " + urlString + "\n\n");
        URL url = new URL(urlString);
        try {
            HTTPConnection connection = new HTTPConnection();
            HTTPRequest request = new HTTPRequest(HTTP.GET, url);
            request.addHeader("User-Agent", "Cidero/1.0");
            request.addHeader("Accept", "*/*");
            request.addHeader("Icy-Metadata", "1");
            System.out.println("Added header");
            System.out.println("Request is:\n" + request.toString());
            HTTPResponse response = connection.sendRequest(request, false);
            writer.println("  HTTP Response:");
            writer.println("   " + response.getFirstLine());
            System.out.println("Response first line: " + response.getFirstLine());
            System.out.println("Response code: " + response.getStatusCode());
            for (int n = 0; n < response.getNumHeaders(); n++) {
                System.out.println(response.getHeader(n).toString());
                writer.println("   " + response.getHeader(n).toString());
            }
            int icy_metaint = -1;
            int readChunkSize = 8192;
            String icy_metaint_value = response.getHeaderValue("icy-metaint");
            if (icy_metaint_value != null) {
                icy_metaint = Integer.parseInt(icy_metaint_value);
                readChunkSize = icy_metaint;
            }
            int icy_br = -1;
            String icy_br_value = response.getHeaderValue("icy-br");
            if (icy_br_value != null) icy_br = Integer.parseInt(icy_br_value);
            long shoutcastBufSize = -1;
            long totalBytes = 0;
            long last = 0;
            if (response.getStatusCode() != HTTPStatus.OK) {
                response.releaseConnection();
                return false;
            }
            int bytes;
            byte[] buf = new byte[readChunkSize];
            byte[] metadataBuf = new byte[16384];
            BufferedInputStream inStream = (BufferedInputStream) response.getInputStream();
            long startTimeMillis = System.currentTimeMillis();
            long lastTimeMillis = startTimeMillis;
            long lastChunkTime = startTimeMillis;
            String metadataString = null;
            int consecNonZeroMetadata = 0;
            long loopCount = 0;
            while ((totalBytes < maxDataBytes) || (maxDataBytes < 0)) {
                int bytesRemaining = readChunkSize;
                while ((bytes = inStream.read(buf, readChunkSize - bytesRemaining, bytesRemaining)) > 0) {
                    bytesRemaining -= bytes;
                    if (bytesRemaining == 0) break;
                }
                long currTimeMillis = System.currentTimeMillis();
                long elapsedTime = currTimeMillis - startTimeMillis;
                long deltaT = currTimeMillis - lastTimeMillis;
                if ((loopCount % 8) == 0) {
                    System.out.println("read " + readChunkSize + " bytes - time:" + elapsedTime + "  deltaT: " + deltaT + " TotalBytes: " + totalBytes);
                    System.out.flush();
                }
                loopCount++;
                lastTimeMillis = currTimeMillis;
                if (bytes < 0) {
                    System.out.println("read returned -1 - done");
                    break;
                }
                if (icy_metaint > 0) {
                    int metadataBytes = inStream.read() * 16;
                    System.out.println("--- Metadata bytes = " + metadataBytes);
                    System.out.flush();
                    if (metadataBytes > 0) {
                        consecNonZeroMetadata++;
                        if (consecNonZeroMetadata > 3) {
                            System.out.println("More than 3 consec non-zero metadata blocks!");
                            System.out.println("TotalBytes = " + totalBytes);
                            System.out.flush();
                            System.exit(-1);
                        }
                        bytesRemaining = metadataBytes;
                        while (bytesRemaining > 0) {
                            bytes = inStream.read(metadataBuf, metadataBytes - bytesRemaining, bytesRemaining);
                            if (bytes < 0) {
                                System.out.println("--- Error reading metadata ");
                                return false;
                            }
                            bytesRemaining -= bytes;
                        }
                        String tmpMetadataString = new String(metadataBuf, 0, metadataBytes);
                        System.out.println("Metadata: [" + tmpMetadataString + "]");
                        System.out.flush();
                        if (metadataString == null) {
                            metadataString = tmpMetadataString;
                            writer.println("  Metadata: [" + metadataString + "]");
                        }
                    } else {
                        consecNonZeroMetadata = 0;
                    }
                }
                totalBytes += readChunkSize;
                if ((totalBytes - last) > 32768) {
                    long rate = (totalBytes - last) * 1000 / (currTimeMillis - lastChunkTime);
                    last = totalBytes;
                    lastChunkTime = currTimeMillis;
                }
                if ((totalBytes > 64 * 1024) && (deltaT > 300)) {
                    if (shoutcastBufSize < 0) shoutcastBufSize = totalBytes;
                }
            }
            writer.println("  TotalBytes snooped: " + totalBytes);
            writer.print("  Apparent shoutcast bufSize: " + shoutcastBufSize);
            if (icy_br > 0) {
                writer.print(" ( " + (shoutcastBufSize * 8) / icy_br + " msec @" + icy_br + "k )");
            }
            writer.println("");
            response.releaseConnection();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Session: readURL: Exception" + e);
            return false;
        }
        return true;
    }

    public static void main(String args[]) {
        if (args.length < 2) {
            System.out.println("Usage: snooper <playlistURL> <infoFile>");
            System.exit(-1);
        }
        try {
            String playlist = args[0];
            ShoutcastSnooper snooper = new ShoutcastSnooper();
            CDSResource resource = new CDSResource();
            String protocolInfo = null;
            if (playlist.toLowerCase().endsWith("m3u")) {
                protocolInfo = "http-get:*:audio/mpegurl:*";
            } else if (playlist.toLowerCase().endsWith("pls")) {
                protocolInfo = "http-get:*:audio/x-scpls:*";
            } else {
                System.out.println("Unknown playlist type, assuming .pls");
                protocolInfo = "http-get:*:audio/x-scpls:*";
            }
            resource.setProtocolInfo(protocolInfo);
            resource.setName(playlist);
            PrintWriter writer = new PrintWriter(new FileOutputStream(args[1]));
            snooper.readPlaylist(resource, writer);
        } catch (Exception e) {
            System.out.println("Session: readURL: Exception" + e);
        }
    }
}
