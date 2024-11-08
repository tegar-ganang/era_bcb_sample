package com.webobjects.appserver;

import static org.jboss.netty.channel.Channels.pipeline;
import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.COOKIE;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpContentCompressor;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.logging.CommonsLoggerFactory;
import org.jboss.netty.logging.InternalLogger;
import com.webobjects.appserver._private.WOInputStreamData;
import com.webobjects.appserver._private.WOProperties;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSDelayedCallbackCenter;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;

/**
 * How to use the WONettyAdaptor:
 *
 * 1. Build/Install ERWOAdaptor framework
 * 2. Include ERWOAdaptor framework in your app/project
 * 3. Run your app with the property:
 *	
 *	 -WOAdaptor er.woadaptor.ERWOAdaptor 
 *
 *  OR:
 *  
 *	 -WOAdaptor WONettyAdaptor
 *
 * 4. (Optional) If developing with the WONettyAdaptor set the following properties as well:
 * 
 *   -WODirectConnectEnabled false
 *   
 *  AND (maybe) 
 *   
 *   -WOAllowRapidTurnaround false
 * 
 * @author ravim
 * 
 * @version 1.0
 */
public class WONettyAdaptor extends WOAdaptor {

    private static final Logger log = Logger.getLogger(WONettyAdaptor.class);

    private int _port;

    private String _hostname;

    private ChannelFactory channelFactory;

    private Channel channel;

    private String hostname() {
        if (_hostname == null) {
            try {
                InetAddress _host = InetAddress.getLocalHost();
                _hostname = _host.getHostName();
            } catch (UnknownHostException exception) {
                log.error("Failed to get localhost address");
            }
        }
        return _hostname;
    }

    public WONettyAdaptor(String name, NSDictionary<String, Object> config) {
        super(name, config);
        Number number = (Number) config.objectForKey(WOProperties._PortKey);
        if (number != null) _port = number.intValue();
        if (_port < 0) _port = 0;
        _hostname = (String) config.objectForKey(WOProperties._HostKey);
        WOApplication.application()._setHost(hostname());
    }

    @Override
    public void registerForEvents() {
        channelFactory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
        ServerBootstrap bootstrap = new ServerBootstrap(channelFactory);
        bootstrap.setPipelineFactory(new PipelineFactory());
        channel = bootstrap.bind(new InetSocketAddress(hostname(), _port));
        log.debug("Binding adaptor to address: " + channel.getLocalAddress());
        _port = ((InetSocketAddress) channel.getLocalAddress()).getPort();
        System.setProperty(WOProperties._PortKey, Integer.toString(_port));
    }

    @Override
    public void unregisterForEvents() {
        ChannelFuture future = channel.close();
        future.awaitUninterruptibly();
        channelFactory.releaseExternalResources();
    }

    @Override
    public int port() {
        return _port;
    }

    @Override
    public boolean dispatchesRequestsConcurrently() {
        return true;
    }

    /**
	  * Originally inspired by: 
	  * 
	  * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
	  * @author Andy Taylor (andy.taylor@jboss.org)
	  * @author <a href="http://gleamynode.net/">Trustin Lee</a>
	  * 
	  * @see <a href="http://docs.jboss.org/netty/3.2/xref/org/jboss/netty/example/http/snoop/HttpServerPipelineFactory.html">HttpServerPipelineFactory</a>
	  * 
	  * @author ravim 	ERWOAdaptor/WONettyAdaptor
	  * 
	  * @property WOMaxIOBufferSize 		Max http chunking size. Defaults to WO default 8196 
	  * 									@see <a href="http://docs.jboss.org/netty/3.2/xref/org/jboss/netty/handler/codec/http/HttpMessageDecoder.html">HttpMessageDecoder</a>
	  * 
	  * @property WOFileUpload.sizeLimit	Max file upload size permitted
	  */
    protected class PipelineFactory implements ChannelPipelineFactory {

        public final Integer maxChunkSize = Integer.getInteger("WOMaxIOBufferSize", 8196);

        public final Integer maxFileSize = Integer.getInteger("WOFileUpload.sizeLimit", 1024 * 1024 * 100);

        public ChannelPipeline getPipeline() throws Exception {
            ChannelPipeline pipeline = pipeline();
            pipeline.addLast("decoder", new HttpRequestDecoder(4096, 8192, maxChunkSize));
            pipeline.addLast("aggregator", new HttpChunkAggregator(maxFileSize));
            pipeline.addLast("encoder", new HttpResponseEncoder());
            pipeline.addLast("deflater", new HttpContentCompressor());
            pipeline.addLast("handler", new RequestHandler());
            return pipeline;
        }
    }

    /**
	 * Converts a Netty HttpRequest to a WORequest
	 * 
	 * @param request	Netty HttpRequest
	 * @return	a WORequest
	 * @throws IOException
	 */
    private static WORequest asWORequest(HttpRequest request) throws IOException {
        NSMutableDictionary<String, NSArray<String>> headers = new NSMutableDictionary<String, NSArray<String>>();
        for (Map.Entry<String, String> header : request.getHeaders()) {
            headers.setObjectForKey(new NSArray<String>(header.getValue().split(",")), header.getKey());
        }
        ChannelBuffer _content = request.getContent();
        NSData contentData = (_content.readable()) ? new WOInputStreamData(new NSData(new ChannelBufferInputStream(_content), 4096)) : NSData.EmptyData;
        WORequest _worequest = WOApplication.application().createRequest(request.getMethod().getName(), request.getUri(), request.getProtocolVersion().getText(), headers, contentData, null);
        String cookieString = request.getHeader(COOKIE);
        if (cookieString != null) {
            CookieDecoder cookieDecoder = new CookieDecoder();
            Set<Cookie> cookies = cookieDecoder.decode(cookieString);
            if (!cookies.isEmpty()) {
                for (Cookie cookie : cookies) {
                    WOCookie wocookie = asWOCookie(cookie);
                    _worequest.addCookie(wocookie);
                }
            }
        }
        return _worequest;
    }

    /**
	 * Converts a Netty Cookie to a WOCookie
	 * 
	 * @param cookie	Netty Cookie
	 * @return	A WOCookie
	 */
    private static WOCookie asWOCookie(Cookie cookie) {
        WOCookie wocookie = new WOCookie(cookie.getName(), cookie.getValue(), cookie.getPath(), cookie.getDomain(), cookie.getMaxAge(), cookie.isSecure());
        return wocookie;
    }

    /**
	 * Converts a WOResponse to a Netty HttpResponse
	 * 
	 * @param woresponse	A WOResponse
	 * @return	HttpResponse
	 */
    private static HttpResponse asHttpResponse(WOResponse woresponse) {
        return new WOResponseWrapper(woresponse);
    }

    /**
	 * Originally inspired by:
	 * 
	 * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
	 * @author Andy Taylor (andy.taylor@jboss.org)
	 * @author <a href="http://gleamynode.net/">Trustin Lee</a>
	 * 
	 * @see <a href="http://docs.jboss.org/netty/3.2/xref/org/jboss/netty/example/http/snoop/HttpRequestHandler.html">HttpRequestHandler</a>
	 * 
	 * @author ravim 	ERWOAdaptor/WONettyAdaptor version
	 */
    protected class RequestHandler extends SimpleChannelUpstreamHandler {

        private InternalLogger log = CommonsLoggerFactory.getDefaultFactory().newInstance(this.getClass().getName());

        /**
		 * @see <a href="http://docs.jboss.org/netty/3.2/api/org/jboss/netty/channel/SimpleChannelUpstreamHandler.html#messageReceived(org.jboss.netty.channel.ChannelHandlerContext,%20org.jboss.netty.channel.MessageEvent)">SimpleChannelUpstreamHandler</a>
		 */
        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            HttpRequest _request = (HttpRequest) e.getMessage();
            WORequest worequest = asWORequest(_request);
            worequest._setOriginatingAddress(((InetSocketAddress) ctx.getChannel().getRemoteAddress()).getAddress());
            WOResponse woresponse = WOApplication.application().dispatchRequest(worequest);
            NSDelayedCallbackCenter.defaultCenter().eventEnded();
            boolean keepAlive = isKeepAlive(_request);
            ChannelFuture future = e.getChannel().write(asHttpResponse(woresponse));
            if (!keepAlive) {
                future.addListener(ChannelFutureListener.CLOSE);
            }
        }

        /**
		 * @see <a href="http://docs.jboss.org/netty/3.2/api/org/jboss/netty/channel/SimpleChannelUpstreamHandler.html#exceptionCaught(org.jboss.netty.channel.ChannelHandlerContext,%20org.jboss.netty.channel.ExceptionEvent)">SimpleChannelUpstreamHandler</a>
		 */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            Throwable cause = e.getCause();
            log.warn(cause.getMessage());
            e.getChannel().close();
        }
    }
}
