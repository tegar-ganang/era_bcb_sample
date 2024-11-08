package com.mapbased.sfw.http;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import com.mapbased.sfw.common.BaseNioServer;
import com.mapbased.sfw.model.HttpHandler;
import com.mapbased.sfw.site.SiteManager;

/**
 * <p>
 * Title: MA LUCENE
 * </p>
 * 
 * <p>
 * Description:
 * </p>
 * 
 * <p>
 * Copyright: Copyright (c) 2010
 * </p>
 * 
 * <p>
 * Company: woyo.com
 * </p>
 * 
 * @author changhuanyou
 * @version 1.0
 */
public class HttpServer extends BaseNioServer {

    SiteManager siteManager = new SiteManager();

    HttpHandler handler = new HttpHandler(this.siteManager);

    protected int defaultPort() {
        return 80;
    }

    /**
	 * getChannelPipelineFactory
	 * 
	 * @return ChannelPipelineFactory
	 * @todo Implement this com.woyo.search.common.BaseNioServer method
	 */
    protected ChannelPipelineFactory getChannelPipelineFactory() {
        return new ChannelPipelineFactory() {

            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = Channels.pipeline();
                pipeline.addLast("decoder", new HttpRequestDecoder());
                pipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
                pipeline.addLast("encoder", new HttpResponseEncoder());
                pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
                pipeline.addLast("handler", handler);
                return pipeline;
            }
        };
    }

    /**
	 * serverName
	 * 
	 * @return String
	 * @todo Implement this com.woyo.search.common.Server method
	 */
    public String serverName() {
        return "http-server";
    }

    public static void main(String[] args) {
        HttpServer server = new HttpServer();
        server.init();
        server.start();
    }
}
