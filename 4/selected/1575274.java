package net.pms.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.external.StartStopListenerDelegate;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestHandlerV2 extends SimpleChannelUpstreamHandler {

    private static final Logger logger = LoggerFactory.getLogger(RequestHandlerV2.class);

    private static final Pattern TIMERANGE_PATTERN = Pattern.compile("timeseekrange\\.dlna\\.org\\W*npt\\W*=\\W*([\\d\\.:]+)?\\-?([\\d\\.:]+)?", Pattern.CASE_INSENSITIVE);

    private volatile HttpRequest nettyRequest;

    private final ChannelGroup group;

    private static final String[] KNOWN_HEADERS = { "Accept", "Accept-Language", "Accept-Encoding", "Callback", "Connection", "Content-Length", "Content-Type", "Date", "Host", "Nt", "Sid", "Timeout", "User-Agent" };

    public RequestHandlerV2(ChannelGroup group) {
        this.group = group;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        RequestV2 request = null;
        RendererConfiguration renderer = null;
        String userAgentString = null;
        StringBuilder unknownHeaders = new StringBuilder();
        String separator = "";
        HttpRequest nettyRequest = this.nettyRequest = (HttpRequest) e.getMessage();
        InetSocketAddress remoteAddress = (InetSocketAddress) e.getChannel().getRemoteAddress();
        InetAddress ia = remoteAddress.getAddress();
        if (filterIp(ia)) {
            e.getChannel().close();
            logger.trace("Access denied for address " + ia + " based on IP filter");
            return;
        }
        logger.trace("Opened request handler on socket " + remoteAddress);
        PMS.get().getRegistry().disableGoToSleep();
        if (HttpMethod.GET.equals(nettyRequest.getMethod())) {
            request = new RequestV2("GET", nettyRequest.getUri().substring(1));
        } else if (HttpMethod.POST.equals(nettyRequest.getMethod())) {
            request = new RequestV2("POST", nettyRequest.getUri().substring(1));
        } else if (HttpMethod.HEAD.equals(nettyRequest.getMethod())) {
            request = new RequestV2("HEAD", nettyRequest.getUri().substring(1));
        } else {
            request = new RequestV2(nettyRequest.getMethod().getName(), nettyRequest.getUri().substring(1));
        }
        logger.trace("Request: " + nettyRequest.getProtocolVersion().getText() + " : " + request.getMethod() + " : " + request.getArgument());
        if (nettyRequest.getProtocolVersion().getMinorVersion() == 0) {
            request.setHttp10(true);
        }
        renderer = RendererConfiguration.getRendererConfigurationBySocketAddress(ia);
        if (renderer != null) {
            PMS.get().setRendererfound(renderer);
            request.setMediaRenderer(renderer);
            logger.trace("Matched media renderer \"" + renderer.getRendererName() + "\" based on address " + ia);
        }
        for (String name : nettyRequest.getHeaderNames()) {
            String headerLine = name + ": " + nettyRequest.getHeader(name);
            logger.trace("Received on socket: " + headerLine);
            if (renderer == null && headerLine != null && headerLine.toUpperCase().startsWith("USER-AGENT") && request != null) {
                userAgentString = headerLine.substring(headerLine.indexOf(":") + 1).trim();
                renderer = RendererConfiguration.getRendererConfigurationByUA(userAgentString);
                if (renderer != null) {
                    request.setMediaRenderer(renderer);
                    renderer.associateIP(ia);
                    PMS.get().setRendererfound(renderer);
                    logger.trace("Matched media renderer \"" + renderer.getRendererName() + "\" based on header \"" + headerLine + "\"");
                }
            }
            if (renderer == null && headerLine != null && request != null) {
                renderer = RendererConfiguration.getRendererConfigurationByUAAHH(headerLine);
                if (renderer != null) {
                    request.setMediaRenderer(renderer);
                    renderer.associateIP(ia);
                    PMS.get().setRendererfound(renderer);
                    logger.trace("Matched media renderer \"" + renderer.getRendererName() + "\" based on header \"" + headerLine + "\"");
                }
            }
            try {
                StringTokenizer s = new StringTokenizer(headerLine);
                String temp = s.nextToken();
                if (request != null && temp.toUpperCase().equals("SOAPACTION:")) {
                    request.setSoapaction(s.nextToken());
                } else if (headerLine.toUpperCase().indexOf("RANGE: BYTES=") > -1) {
                    String nums = headerLine.substring(headerLine.toUpperCase().indexOf("RANGE: BYTES=") + 13).trim();
                    StringTokenizer st = new StringTokenizer(nums, "-");
                    if (!nums.startsWith("-")) {
                        request.setLowRange(Long.parseLong(st.nextToken()));
                    }
                    if (!nums.startsWith("-") && !nums.endsWith("-")) {
                        request.setHighRange(Long.parseLong(st.nextToken()));
                    } else {
                        request.setHighRange(-1);
                    }
                } else if (headerLine.toLowerCase().indexOf("transfermode.dlna.org:") > -1) {
                    request.setTransferMode(headerLine.substring(headerLine.toLowerCase().indexOf("transfermode.dlna.org:") + 22).trim());
                } else if (headerLine.toLowerCase().indexOf("getcontentfeatures.dlna.org:") > -1) {
                    request.setContentFeatures(headerLine.substring(headerLine.toLowerCase().indexOf("getcontentfeatures.dlna.org:") + 28).trim());
                } else {
                    Matcher matcher = TIMERANGE_PATTERN.matcher(headerLine);
                    if (matcher.find()) {
                        String first = matcher.group(1);
                        if (first != null) {
                            request.setTimeRangeStartString(first);
                        }
                        String end = matcher.group(2);
                        if (end != null) {
                            request.setTimeRangeEndString(end);
                        }
                    } else {
                        boolean isKnown = false;
                        for (String knownHeaderString : KNOWN_HEADERS) {
                            if (headerLine.toLowerCase().startsWith(knownHeaderString.toLowerCase())) {
                                isKnown = true;
                                break;
                            }
                        }
                        if (!isKnown) {
                            unknownHeaders.append(separator + headerLine);
                            separator = ", ";
                        }
                    }
                }
            } catch (Exception ee) {
                logger.error("Error parsing HTTP headers", ee);
            }
        }
        if (request != null) {
            if (request.getMediaRenderer() == null) {
                request.setMediaRenderer(RendererConfiguration.getDefaultConf());
                logger.trace("Using default media renderer " + request.getMediaRenderer().getRendererName());
                if (userAgentString != null && !userAgentString.equals("FDSSDP")) {
                    logger.info("Media renderer was not recognized. Possible identifying HTTP headers: User-Agent: " + userAgentString + ("".equals(unknownHeaders.toString()) ? "" : ", " + unknownHeaders.toString()));
                    PMS.get().setRendererfound(request.getMediaRenderer());
                }
            } else {
                if (userAgentString != null) {
                    logger.trace("HTTP User-Agent: " + userAgentString);
                }
                logger.trace("Recognized media renderer " + request.getMediaRenderer().getRendererName());
            }
        }
        if (HttpHeaders.getContentLength(nettyRequest) > 0) {
            byte data[] = new byte[(int) HttpHeaders.getContentLength(nettyRequest)];
            ChannelBuffer content = nettyRequest.getContent();
            content.readBytes(data);
            request.setTextContent(new String(data, "UTF-8"));
        }
        if (request != null) {
            logger.trace("HTTP: " + request.getArgument() + " / " + request.getLowRange() + "-" + request.getHighRange());
        }
        writeResponse(e, request, ia);
    }

    /**
	 * Applies the IP filter to the specified internet address. Returns true
	 * if the address is not allowed and therefore should be filtered out,
	 * false otherwise.
	 * @param inetAddress The internet address to verify.
	 * @return True when not allowed, false otherwise.
	 */
    private boolean filterIp(InetAddress inetAddress) {
        return !PMS.getConfiguration().getIpFiltering().allowed(inetAddress);
    }

    private void writeResponse(MessageEvent e, RequestV2 request, InetAddress ia) {
        boolean close = HttpHeaders.Values.CLOSE.equalsIgnoreCase(nettyRequest.getHeader(HttpHeaders.Names.CONNECTION)) || nettyRequest.getProtocolVersion().equals(HttpVersion.HTTP_1_0) && !HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase(nettyRequest.getHeader(HttpHeaders.Names.CONNECTION));
        HttpResponse response = null;
        if (request.getLowRange() != 0 || request.getHighRange() != 0) {
            response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.PARTIAL_CONTENT);
        } else {
            response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        }
        StartStopListenerDelegate startStopListenerDelegate = new StartStopListenerDelegate(ia.getHostAddress());
        try {
            request.answer(response, e, close, startStopListenerDelegate);
        } catch (IOException e1) {
            logger.trace("HTTP request V2 IO error: " + e1.getMessage());
            startStopListenerDelegate.stop();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        Channel ch = e.getChannel();
        Throwable cause = e.getCause();
        if (cause instanceof TooLongFrameException) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }
        if (cause != null && !cause.getClass().equals(ClosedChannelException.class) && !cause.getClass().equals(IOException.class)) {
            cause.printStackTrace();
        }
        if (ch.isConnected()) {
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
        e.getChannel().close();
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.setContent(ChannelBuffers.copiedBuffer("Failure: " + status.toString() + "\r\n", Charset.forName("UTF-8")));
        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelOpen(ctx, e);
        if (group != null) {
            group.add(ctx.getChannel());
        }
    }
}
