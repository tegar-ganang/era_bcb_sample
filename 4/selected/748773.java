package net.pms.network;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.Range;
import net.pms.external.StartStopListenerDelegate;
import org.apache.commons.lang.StringUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.stream.ChunkedStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles all forms of incoming HTTP requests by constructing a proper HTTP response. 
 */
public class RequestV2 extends HTTPResource {

    private static final Logger logger = LoggerFactory.getLogger(RequestV2.class);

    private static final String CRLF = "\r\n";

    private static SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);

    private static int BUFFER_SIZE = 8 * 1024;

    private static final int[] MULTIPLIER = new int[] { 1, 60, 3600, 24 * 3600 };

    private final String method;

    /**
	 * A {@link String} that contains the argument with which this {@link RequestV2} was
	 * created. It contains a command, a unique resource id and a resource name, all
	 * separated by slashes. For example: "get/0$0$2$17/big_buck_bunny_1080p_h264.mov" or
	 * "get/0$0$2$13/thumbnail0000Sintel.2010.1080p.mkv"
	 */
    private final String argument;

    private String soapaction;

    private String content;

    private String objectID;

    private int startingIndex;

    private int requestCount;

    private String browseFlag;

    /**
	 * When sending an input stream, the lowRange indicates which byte to start from.  
	 */
    private long lowRange;

    private InputStream inputStream;

    private RendererConfiguration mediaRenderer;

    private String transferMode;

    private String contentFeatures;

    private final Range.Time range = new Range.Time();

    /**
	 * When sending an input stream, the highRange indicates which byte to stop at.  
	 */
    private long highRange;

    private boolean http10;

    public RendererConfiguration getMediaRenderer() {
        return mediaRenderer;
    }

    public void setMediaRenderer(RendererConfiguration mediaRenderer) {
        this.mediaRenderer = mediaRenderer;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    /**
	 * When sending an input stream, the lowRange indicates which byte to start from.  
	 * @return The byte to start from
	 */
    public long getLowRange() {
        return lowRange;
    }

    /**
	 * Set the byte from which to start when sending an input stream. This value will
	 * be used to send a CONTENT_RANGE header with the response.
	 * @param lowRange The byte to start from.
	 */
    public void setLowRange(long lowRange) {
        this.lowRange = lowRange;
    }

    public String getTransferMode() {
        return transferMode;
    }

    public void setTransferMode(String transferMode) {
        this.transferMode = transferMode;
    }

    public String getContentFeatures() {
        return contentFeatures;
    }

    public void setContentFeatures(String contentFeatures) {
        this.contentFeatures = contentFeatures;
    }

    public void setTimeRangeStart(Double timeseek) {
        this.range.setStart(timeseek);
    }

    public void setTimeRangeStartString(String str) {
        setTimeRangeStart(convertTime(str));
    }

    public void setTimeRangeEnd(Double rangeEnd) {
        this.range.setEnd(rangeEnd);
    }

    public void setTimeRangeEndString(String str) {
        setTimeRangeEnd(convertTime(str));
    }

    /**
	 * When sending an input stream, the highRange indicates which byte to stop at.
	 * @return The byte to stop at.  
	 */
    public long getHighRange() {
        return highRange;
    }

    /**
	 * Set the byte at which to stop when sending an input stream. This value will
	 * be used to send a CONTENT_RANGE header with the response.
	 * @param highRange The byte to stop at.
	 */
    public void setHighRange(long highRange) {
        this.highRange = highRange;
    }

    public boolean isHttp10() {
        return http10;
    }

    public void setHttp10(boolean http10) {
        this.http10 = http10;
    }

    /**
	 * This class will construct and transmit a proper HTTP response to a given HTTP request.
	 * Rewritten version of the {@link Request} class.  
	 * @param method The {@link String} that defines the HTTP method to be used.
	 * @param argument The {@link String} containing instructions for PMS. It contains a command,
	 * 		a unique resource id and a resource name, all separated by slashes.
	 */
    public RequestV2(String method, String argument) {
        this.method = method;
        this.argument = argument;
    }

    public String getSoapaction() {
        return soapaction;
    }

    public void setSoapaction(String soapaction) {
        this.soapaction = soapaction;
    }

    public String getTextContent() {
        return content;
    }

    public void setTextContent(String content) {
        this.content = content;
    }

    /**
	 * Retrieves the HTTP method with which this {@link RequestV2} was created.
	 * @return The (@link String} containing the HTTP method.
	 */
    public String getMethod() {
        return method;
    }

    /**
	 * Retrieves the argument with which this {@link RequestV2} was created. It contains
	 * a command, a unique resource id and a resource name, all separated by slashes. For
	 * example: "get/0$0$2$17/big_buck_bunny_1080p_h264.mov" or "get/0$0$2$13/thumbnail0000Sintel.2010.1080p.mkv"
	 * @return The {@link String} containing the argument.
	 */
    public String getArgument() {
        return argument;
    }

    /**
	 * Construct a proper HTTP response to a received request. After the response has been
	 * created, it is sent and the resulting {@link ChannelFuture} object is returned.
	 * See <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html">RFC-2616</a>
	 * for HTTP header field definitions. 
	 * @param output The {@link HttpResponse} object that will be used to construct the response.
	 * @param e The {@link MessageEvent} object used to communicate with the client that sent
	 * 			the request.
	 * @param close Set to true to close the channel after sending the response. By default the
	 * 			channel is not closed after sending.
	 * @param startStopListenerDelegate The {@link StartStopListenerDelegate} object that is used
	 * 			to notify plugins that the {@link DLNAResource} is about to start playing.
	 * @return The {@link ChannelFuture} object via which the response was sent.
	 * @throws IOException
	 */
    public ChannelFuture answer(HttpResponse output, MessageEvent e, final boolean close, final StartStopListenerDelegate startStopListenerDelegate) throws IOException {
        ChannelFuture future = null;
        long CLoverride = -2;
        StringBuilder response = new StringBuilder();
        DLNAResource dlna = null;
        boolean xbox = mediaRenderer.isXBOX();
        if ((method.equals("GET") || method.equals("HEAD")) && argument.startsWith("console/")) {
            output.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/html");
            response.append(HTMLConsole.servePage(argument.substring(8)));
        } else if ((method.equals("GET") || method.equals("HEAD")) && argument.startsWith("get/")) {
            String id = argument.substring(argument.indexOf("get/") + 4, argument.lastIndexOf("/"));
            id = id.replace("%24", "$");
            List<DLNAResource> files = PMS.get().getRootFolder(mediaRenderer).getDLNAResources(id, false, 0, 0, mediaRenderer);
            if (transferMode != null) {
                output.setHeader("TransferMode.DLNA.ORG", transferMode);
            }
            if (files.size() == 1) {
                dlna = files.get(0);
                String fileName = argument.substring(argument.lastIndexOf("/") + 1);
                if (fileName.startsWith("thumbnail0000")) {
                    output.setHeader(HttpHeaders.Names.CONTENT_TYPE, files.get(0).getThumbnailContentType());
                    output.setHeader(HttpHeaders.Names.ACCEPT_RANGES, "bytes");
                    output.setHeader(HttpHeaders.Names.EXPIRES, getFUTUREDATE() + " GMT");
                    output.setHeader(HttpHeaders.Names.CONNECTION, "keep-alive");
                    if (mediaRenderer.isMediaParserV2()) {
                        dlna.checkThumbnail();
                    }
                    inputStream = dlna.getThumbnailInputStream();
                } else if (fileName.indexOf("subtitle0000") > -1) {
                    output.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain");
                    output.setHeader(HttpHeaders.Names.EXPIRES, getFUTUREDATE() + " GMT");
                    List<DLNAMediaSubtitle> subs = dlna.getMedia().getSubtitlesCodes();
                    if (subs != null && !subs.isEmpty()) {
                        DLNAMediaSubtitle sub = subs.get(0);
                        inputStream = new java.io.FileInputStream(sub.getFile());
                    }
                } else {
                    Range.Time splitRange = dlna.getSplitRange();
                    if (range.getStart() == null && splitRange.getStart() != null) {
                        range.setStart(splitRange.getStart());
                    }
                    if (range.getEnd() == null && splitRange.getEnd() != null) {
                        range.setEnd(splitRange.getEnd());
                    }
                    inputStream = dlna.getInputStream(Range.create(lowRange, highRange, range.getStart(), range.getEnd()), mediaRenderer);
                    String subtitleHttpHeader = mediaRenderer.getSubtitleHttpHeader();
                    if (subtitleHttpHeader != null && !"".equals(subtitleHttpHeader)) {
                        List<DLNAMediaSubtitle> subs = dlna.getMedia().getSubtitlesCodes();
                        if (subs != null && !subs.isEmpty()) {
                            DLNAMediaSubtitle sub = subs.get(0);
                            int type = sub.getType();
                            if (type < DLNAMediaSubtitle.subExtensions.length) {
                                String strType = DLNAMediaSubtitle.subExtensions[type - 1];
                                String subtitleUrl = "http://" + PMS.get().getServer().getHost() + ':' + PMS.get().getServer().getPort() + "/get/" + id + "/subtitle0000." + strType;
                                output.setHeader(subtitleHttpHeader, subtitleUrl);
                            }
                        }
                    }
                    String name = dlna.getDisplayName(mediaRenderer);
                    if (inputStream == null) {
                        logger.error("There is no inputstream to return for " + name);
                    } else {
                        startStopListenerDelegate.start(dlna);
                        String rendererMimeType = getRendererMimeType(files.get(0).mimeType(), mediaRenderer);
                        if (rendererMimeType != null && !"".equals(rendererMimeType)) {
                            output.setHeader(HttpHeaders.Names.CONTENT_TYPE, rendererMimeType);
                        }
                        final DLNAMediaInfo media = dlna.getMedia();
                        if (media != null) {
                            if (StringUtils.isNotBlank(media.getContainer())) {
                                name += " [container: " + media.getContainer() + "]";
                            }
                            if (StringUtils.isNotBlank(media.getCodecV())) {
                                name += " [video: " + media.getCodecV() + "]";
                            }
                        }
                        PMS.get().getFrame().setStatusLine("Serving " + name);
                        boolean chunked = mediaRenderer.isChunkedTransfer();
                        long totalsize = dlna.length(mediaRenderer);
                        if (chunked && totalsize == DLNAMediaInfo.TRANS_SIZE) {
                            totalsize = -1;
                        }
                        long remaining = totalsize - lowRange;
                        long requested = highRange - lowRange;
                        if (requested != 0) {
                            long bytes = remaining > -1 ? remaining : inputStream.available();
                            if (requested > 0 && bytes > requested) {
                                bytes = requested + 1;
                            }
                            highRange = lowRange + bytes - (bytes > 0 ? 1 : 0);
                            logger.trace((chunked ? "Using chunked response. " : "") + "Sending " + bytes + " bytes.");
                            output.setHeader(HttpHeaders.Names.CONTENT_RANGE, "bytes " + lowRange + "-" + (highRange > -1 ? highRange : "*") + "/" + (totalsize > -1 ? totalsize : "*"));
                            if (chunked && requested < 0 && totalsize < 0) {
                                CLoverride = -1;
                            } else {
                                CLoverride = bytes;
                            }
                        } else {
                            CLoverride = remaining;
                        }
                        highRange = lowRange + CLoverride - (CLoverride > 0 ? 1 : 0);
                        if (contentFeatures != null) {
                            output.setHeader("ContentFeatures.DLNA.ORG", files.get(0).getDlnaContentFeatures());
                        }
                        output.setHeader(HttpHeaders.Names.ACCEPT_RANGES, "bytes");
                        output.setHeader(HttpHeaders.Names.CONNECTION, "keep-alive");
                    }
                }
            }
        } else if ((method.equals("GET") || method.equals("HEAD")) && (argument.toLowerCase().endsWith(".png") || argument.toLowerCase().endsWith(".jpg") || argument.toLowerCase().endsWith(".jpeg"))) {
            if (argument.toLowerCase().endsWith(".png")) {
                output.setHeader(HttpHeaders.Names.CONTENT_TYPE, "image/png");
            } else {
                output.setHeader(HttpHeaders.Names.CONTENT_TYPE, "image/jpeg");
            }
            output.setHeader(HttpHeaders.Names.ACCEPT_RANGES, "bytes");
            output.setHeader(HttpHeaders.Names.CONNECTION, "keep-alive");
            output.setHeader(HttpHeaders.Names.EXPIRES, getFUTUREDATE() + " GMT");
            inputStream = getResourceInputStream(argument);
        } else if ((method.equals("GET") || method.equals("HEAD")) && (argument.equals("description/fetch") || argument.endsWith("1.0.xml"))) {
            output.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/xml; charset=\"utf-8\"");
            output.setHeader(HttpHeaders.Names.CACHE_CONTROL, "no-cache");
            output.setHeader(HttpHeaders.Names.EXPIRES, "0");
            output.setHeader(HttpHeaders.Names.ACCEPT_RANGES, "bytes");
            output.setHeader(HttpHeaders.Names.CONNECTION, "keep-alive");
            inputStream = getResourceInputStream((argument.equals("description/fetch") ? "PMS.xml" : argument));
            if (argument.equals("description/fetch")) {
                byte b[] = new byte[inputStream.available()];
                inputStream.read(b);
                String s = new String(b);
                s = s.replace("uuid:1234567890TOTO", PMS.get().usn());
                String profileName = PMS.getConfiguration().getProfileName();
                if (PMS.get().getServer().getHost() != null) {
                    s = s.replace("<host>", PMS.get().getServer().getHost());
                    s = s.replace("<port>", "" + PMS.get().getServer().getPort());
                }
                if (xbox) {
                    logger.debug("DLNA changes for Xbox360");
                    s = s.replace("PS3 Media Server", "PS3 Media Server [" + profileName + "] : Windows Media Connect");
                    s = s.replace("<modelName>PMS</modelName>", "<modelName>Windows Media Connect</modelName>");
                    s = s.replace("<serviceList>", "<serviceList>" + CRLF + "<service>" + CRLF + "<serviceType>urn:microsoft.com:service:X_MS_MediaReceiverRegistrar:1</serviceType>" + CRLF + "<serviceId>urn:microsoft.com:serviceId:X_MS_MediaReceiverRegistrar</serviceId>" + CRLF + "<SCPDURL>/upnp/mrr/scpd</SCPDURL>" + CRLF + "<controlURL>/upnp/mrr/control</controlURL>" + CRLF + "</service>" + CRLF);
                } else {
                    s = s.replace("PS3 Media Server", "PS3 Media Server [" + profileName + "]");
                }
                if (!mediaRenderer.isPS3()) {
                    s = s.replace("<mimetype>image/png</mimetype>", "<mimetype>image/jpeg</mimetype>");
                    s = s.replace("/images/thumbnail-256.png", "/images/thumbnail-120.jpg");
                    s = s.replace(">256<", ">120<");
                }
                response.append(s);
                inputStream = null;
            }
        } else if (method.equals("POST") && (argument.contains("MS_MediaReceiverRegistrar_control") || argument.contains("mrr/control"))) {
            output.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/xml; charset=\"utf-8\"");
            response.append(HTTPXMLHelper.XML_HEADER);
            response.append(CRLF);
            response.append(HTTPXMLHelper.SOAP_ENCODING_HEADER);
            response.append(CRLF);
            if (soapaction != null && soapaction.contains("IsAuthorized")) {
                response.append(HTTPXMLHelper.XBOX_2);
                response.append(CRLF);
            } else if (soapaction != null && soapaction.contains("IsValidated")) {
                response.append(HTTPXMLHelper.XBOX_1);
                response.append(CRLF);
            }
            response.append(HTTPXMLHelper.BROWSERESPONSE_FOOTER);
            response.append(CRLF);
            response.append(HTTPXMLHelper.SOAP_ENCODING_FOOTER);
            response.append(CRLF);
        } else if (method.equals("POST") && argument.equals("upnp/control/connection_manager")) {
            output.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/xml; charset=\"utf-8\"");
            if (soapaction.indexOf("ConnectionManager:1#GetProtocolInfo") > -1) {
                response.append(HTTPXMLHelper.XML_HEADER);
                response.append(CRLF);
                response.append(HTTPXMLHelper.SOAP_ENCODING_HEADER);
                response.append(CRLF);
                response.append(HTTPXMLHelper.PROTOCOLINFO_RESPONSE);
                response.append(CRLF);
                response.append(HTTPXMLHelper.SOAP_ENCODING_FOOTER);
                response.append(CRLF);
            }
        } else if (method.equals("POST") && argument.equals("upnp/control/content_directory")) {
            output.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/xml; charset=\"utf-8\"");
            if (soapaction.indexOf("ContentDirectory:1#GetSystemUpdateID") > -1) {
                response.append(HTTPXMLHelper.XML_HEADER);
                response.append(CRLF);
                response.append(HTTPXMLHelper.SOAP_ENCODING_HEADER);
                response.append(CRLF);
                response.append(HTTPXMLHelper.GETSYSTEMUPDATEID_HEADER);
                response.append(CRLF);
                response.append("<Id>" + DLNAResource.getSystemUpdateId() + "</Id>");
                response.append(CRLF);
                response.append(HTTPXMLHelper.GETSYSTEMUPDATEID_FOOTER);
                response.append(CRLF);
                response.append(HTTPXMLHelper.SOAP_ENCODING_FOOTER);
                response.append(CRLF);
            } else if (soapaction.indexOf("ContentDirectory:1#GetSortCapabilities") > -1) {
                response.append(HTTPXMLHelper.XML_HEADER);
                response.append(CRLF);
                response.append(HTTPXMLHelper.SOAP_ENCODING_HEADER);
                response.append(CRLF);
                response.append(HTTPXMLHelper.SORTCAPS_RESPONSE);
                response.append(CRLF);
                response.append(HTTPXMLHelper.SOAP_ENCODING_FOOTER);
                response.append(CRLF);
            } else if (soapaction.indexOf("ContentDirectory:1#GetSearchCapabilities") > -1) {
                response.append(HTTPXMLHelper.XML_HEADER);
                response.append(CRLF);
                response.append(HTTPXMLHelper.SOAP_ENCODING_HEADER);
                response.append(CRLF);
                response.append(HTTPXMLHelper.SEARCHCAPS_RESPONSE);
                response.append(CRLF);
                response.append(HTTPXMLHelper.SOAP_ENCODING_FOOTER);
                response.append(CRLF);
            } else if (soapaction.contains("ContentDirectory:1#Browse") || soapaction.contains("ContentDirectory:1#Search")) {
                objectID = getEnclosingValue(content, "<ObjectID>", "</ObjectID>");
                String containerID = null;
                if ((objectID == null || objectID.length() == 0) && xbox) {
                    containerID = getEnclosingValue(content, "<ContainerID>", "</ContainerID>");
                    if (!containerID.contains("$")) {
                        objectID = "0";
                    } else {
                        objectID = containerID;
                        containerID = null;
                    }
                }
                Object sI = getEnclosingValue(content, "<StartingIndex>", "</StartingIndex>");
                Object rC = getEnclosingValue(content, "<RequestedCount>", "</RequestedCount>");
                browseFlag = getEnclosingValue(content, "<BrowseFlag>", "</BrowseFlag>");
                if (sI != null) {
                    startingIndex = Integer.parseInt(sI.toString());
                }
                if (rC != null) {
                    requestCount = Integer.parseInt(rC.toString());
                }
                response.append(HTTPXMLHelper.XML_HEADER);
                response.append(CRLF);
                response.append(HTTPXMLHelper.SOAP_ENCODING_HEADER);
                response.append(CRLF);
                if (soapaction.contains("ContentDirectory:1#Search")) {
                    response.append(HTTPXMLHelper.SEARCHRESPONSE_HEADER);
                } else {
                    response.append(HTTPXMLHelper.BROWSERESPONSE_HEADER);
                }
                response.append(CRLF);
                response.append(HTTPXMLHelper.RESULT_HEADER);
                response.append(HTTPXMLHelper.DIDL_HEADER);
                if (soapaction.contains("ContentDirectory:1#Search")) {
                    browseFlag = "BrowseDirectChildren";
                }
                String searchCriteria = null;
                if (xbox && PMS.getConfiguration().getUseCache() && PMS.get().getLibrary() != null && containerID != null) {
                    if (containerID.equals("7") && PMS.get().getLibrary().getAlbumFolder() != null) {
                        objectID = PMS.get().getLibrary().getAlbumFolder().getResourceId();
                    } else if (containerID.equals("6") && PMS.get().getLibrary().getArtistFolder() != null) {
                        objectID = PMS.get().getLibrary().getArtistFolder().getResourceId();
                    } else if (containerID.equals("5") && PMS.get().getLibrary().getGenreFolder() != null) {
                        objectID = PMS.get().getLibrary().getGenreFolder().getResourceId();
                    } else if (containerID.equals("F") && PMS.get().getLibrary().getPlaylistFolder() != null) {
                        objectID = PMS.get().getLibrary().getPlaylistFolder().getResourceId();
                    } else if (containerID.equals("4") && PMS.get().getLibrary().getAllFolder() != null) {
                        objectID = PMS.get().getLibrary().getAllFolder().getResourceId();
                    } else if (containerID.equals("1")) {
                        String artist = getEnclosingValue(content, "upnp:artist = &quot;", "&quot;)");
                        if (artist != null) {
                            objectID = PMS.get().getLibrary().getArtistFolder().getResourceId();
                            searchCriteria = artist;
                        }
                    }
                }
                List<DLNAResource> files = PMS.get().getRootFolder(mediaRenderer).getDLNAResources(objectID, browseFlag != null && browseFlag.equals("BrowseDirectChildren"), startingIndex, requestCount, mediaRenderer);
                if (searchCriteria != null && files != null) {
                    for (int i = files.size() - 1; i >= 0; i--) {
                        if (!files.get(i).getName().equals(searchCriteria)) {
                            files.remove(i);
                        }
                    }
                    if (files.size() > 0) {
                        files = files.get(0).getChildren();
                    }
                }
                int minus = 0;
                if (files != null) {
                    for (DLNAResource uf : files) {
                        if (xbox && containerID != null) {
                            uf.setFakeParentId(containerID);
                        }
                        if (uf.isCompatible(mediaRenderer) && (uf.getPlayer() == null || uf.getPlayer().isPlayerCompatible(mediaRenderer))) {
                            response.append(uf.toString(mediaRenderer));
                        } else {
                            minus++;
                        }
                    }
                }
                response.append(HTTPXMLHelper.DIDL_FOOTER);
                response.append(HTTPXMLHelper.RESULT_FOOTER);
                response.append(CRLF);
                int filessize = 0;
                if (files != null) {
                    filessize = files.size();
                }
                response.append("<NumberReturned>").append(filessize - minus).append("</NumberReturned>");
                response.append(CRLF);
                DLNAResource parentFolder = null;
                if (files != null && filessize > 0) {
                    parentFolder = files.get(0).getParent();
                }
                if (browseFlag != null && browseFlag.equals("BrowseDirectChildren") && mediaRenderer.isMediaParserV2() && mediaRenderer.isDLNATreeHack()) {
                    int totalCount = startingIndex + requestCount + 1;
                    if (filessize - minus <= 0) {
                        totalCount = startingIndex;
                    }
                    response.append("<TotalMatches>").append(totalCount).append("</TotalMatches>");
                } else if (browseFlag != null && browseFlag.equals("BrowseDirectChildren")) {
                    response.append("<TotalMatches>").append(((parentFolder != null) ? parentFolder.childrenNumber() : filessize) - minus).append("</TotalMatches>");
                } else {
                    response.append("<TotalMatches>1</TotalMatches>");
                }
                response.append(CRLF);
                response.append("<UpdateID>");
                if (parentFolder != null) {
                    response.append(parentFolder.getUpdateId());
                } else {
                    response.append("1");
                }
                response.append("</UpdateID>");
                response.append(CRLF);
                if (soapaction.contains("ContentDirectory:1#Search")) {
                    response.append(HTTPXMLHelper.SEARCHRESPONSE_FOOTER);
                } else {
                    response.append(HTTPXMLHelper.BROWSERESPONSE_FOOTER);
                }
                response.append(CRLF);
                response.append(HTTPXMLHelper.SOAP_ENCODING_FOOTER);
                response.append(CRLF);
            }
        } else if (method.equals("SUBSCRIBE")) {
            output.setHeader("SID", PMS.get().usn());
            output.setHeader("TIMEOUT", "Second-1800");
        } else if (method.equals("NOTIFY")) {
            output.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/xml");
            output.setHeader("NT", "upnp:event");
            output.setHeader("NTS", "upnp:propchange");
            output.setHeader("SID", PMS.get().usn());
            output.setHeader("SEQ", "0");
            response.append("<e:propertyset xmlns:e=\"urn:schemas-upnp-org:event-1-0\">");
            response.append("<e:property>");
            response.append("<TransferIDs></TransferIDs>");
            response.append("</e:property>");
            response.append("<e:property>");
            response.append("<ContainerUpdateIDs></ContainerUpdateIDs>");
            response.append("</e:property>");
            response.append("<e:property>");
            response.append("<SystemUpdateID>").append(DLNAResource.getSystemUpdateId()).append("</SystemUpdateID>");
            response.append("</e:property>");
            response.append("</e:propertyset>");
        }
        output.setHeader("Server", PMS.get().getServerName());
        if (response.length() > 0) {
            byte responseData[] = response.toString().getBytes("UTF-8");
            output.setHeader(HttpHeaders.Names.CONTENT_LENGTH, "" + responseData.length);
            if (!method.equals("HEAD")) {
                ChannelBuffer buf = ChannelBuffers.copiedBuffer(responseData);
                output.setContent(buf);
            }
            future = e.getChannel().write(output);
            if (close) {
                future.addListener(ChannelFutureListener.CLOSE);
            }
        } else if (inputStream != null) {
            if (CLoverride > -2) {
                if (CLoverride > -1 && CLoverride != DLNAMediaInfo.TRANS_SIZE) {
                    output.setHeader(HttpHeaders.Names.CONTENT_LENGTH, "" + CLoverride);
                }
            } else {
                int cl = inputStream.available();
                logger.trace("Available Content-Length: " + cl);
                output.setHeader(HttpHeaders.Names.CONTENT_LENGTH, "" + cl);
            }
            if (range.isStartOffsetAvailable() && dlna != null) {
                String timeseekValue = DLNAMediaInfo.getDurationString(range.getStartOrZero());
                String timetotalValue = dlna.getMedia().getDurationString();
                String timeEndValue = range.isEndLimitAvailable() ? DLNAMediaInfo.getDurationString(range.getEnd()) : timetotalValue;
                output.setHeader("TimeSeekRange.dlna.org", "npt=" + timeseekValue + "-" + timeEndValue + "/" + timetotalValue);
                output.setHeader("X-Seek-Range", "npt=" + timeseekValue + "-" + timeEndValue + "/" + timetotalValue);
            }
            future = e.getChannel().write(output);
            if (lowRange != DLNAMediaInfo.ENDFILE_POS && !method.equals("HEAD")) {
                ChannelFuture chunkWriteFuture = e.getChannel().write(new ChunkedStream(inputStream, BUFFER_SIZE));
                chunkWriteFuture.addListener(new ChannelFutureListener() {

                    @Override
                    public void operationComplete(ChannelFuture future) {
                        try {
                            PMS.get().getRegistry().reenableGoToSleep();
                            inputStream.close();
                        } catch (IOException e) {
                        }
                        future.getChannel().close();
                        startStopListenerDelegate.stop();
                    }
                });
            } else {
                try {
                    PMS.get().getRegistry().reenableGoToSleep();
                    inputStream.close();
                } catch (IOException ioe) {
                }
                if (close) {
                    future.addListener(ChannelFutureListener.CLOSE);
                }
                startStopListenerDelegate.stop();
            }
        } else {
            if (lowRange > 0 && highRange > 0) {
                output.setHeader(HttpHeaders.Names.CONTENT_LENGTH, "" + (highRange - lowRange + 1));
            } else {
                output.setHeader(HttpHeaders.Names.CONTENT_LENGTH, "0");
            }
            future = e.getChannel().write(output);
            if (close) {
                future.addListener(ChannelFutureListener.CLOSE);
            }
        }
        Iterator<String> it = output.getHeaderNames().iterator();
        while (it.hasNext()) {
            String headerName = it.next();
            logger.trace("Sent to socket: " + headerName + ": " + output.getHeader(headerName));
        }
        return future;
    }

    /**
	 * Returns a date somewhere in the far future.
	 * @return The {@link String} containing the date
	 */
    private String getFUTUREDATE() {
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(new Date(10000000000L + System.currentTimeMillis()));
    }

    /**
	 * Returns the string value that is enclosed by the left and right tag in a content string.
	 * Only the first match of each tag is used to determine positions. If either of the tags
	 * cannot be found, null is returned.
	 * @param content The entire {@link String} that needs to be searched for the left and right tag. 
	 * @param leftTag The {@link String} determining the match for the left tag. 
	 * @param rightTag The {@link String} determining the match for the right tag.
	 * @return The {@link String} that was enclosed by the left and right tag.
	 */
    private String getEnclosingValue(String content, String leftTag, String rightTag) {
        String result = null;
        int leftTagPos = content.indexOf(leftTag);
        int rightTagPos = content.indexOf(rightTag, leftTagPos + 1);
        if (leftTagPos > -1 && rightTagPos > leftTagPos) {
            result = content.substring(leftTagPos + leftTag.length(), rightTagPos);
        }
        return result;
    }

    /**
	 * Parse as double, or if it's not just one number, handles {hour}:{minute}:{seconds}
	 * @param time
	 * @return
	 */
    private double convertTime(String time) {
        try {
            return Double.parseDouble(time);
        } catch (NumberFormatException e) {
            String[] arrs = time.split(":");
            double value, sum = 0;
            for (int i = 0; i < arrs.length; i++) {
                value = Double.parseDouble(arrs[arrs.length - i - 1]);
                sum += value * MULTIPLIER[i];
            }
            return sum;
        }
    }
}
