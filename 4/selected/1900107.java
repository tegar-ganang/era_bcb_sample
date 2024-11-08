package gg.arkheion.http;

import gg.arkehion.http.HttpFormattedHandler;
import gg.arkehion.http.HttpFormattedHandler.ArkArgs;
import gg.arkehion.http.HttpFormattedHandler.ArkRequest;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http2.CookieEncoder;
import org.jboss.netty.handler.codec.http2.DefaultHttpDataFactory;
import org.jboss.netty.handler.codec.http2.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http2.HttpDataFactory;
import org.jboss.netty.handler.codec.http2.HttpHeaders;
import org.jboss.netty.handler.codec.http2.HttpMethod;
import org.jboss.netty.handler.codec.http2.HttpPostRequestEncoder;
import org.jboss.netty.handler.codec.http2.HttpRequest;
import org.jboss.netty.handler.codec.http2.HttpVersion;
import org.jboss.netty.handler.codec.http2.QueryStringEncoder;
import org.jboss.netty.handler.codec.http2.HttpPostRequestEncoder.ErrorDataEncoderException;

/**
 * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
 * @author Andy Taylor (andy.taylor@jboss.org)
 * @author <a href="http://gleamynode.net/">Trustin Lee</a>
 * @author <a href="http://openr66.free.fr/">Frederic Bregier</a>
 *
 * @version $Rev: 612 $, $Date: 2010-11-11 19:35:43 +0100 (jeu., 11 nov. 2010) $
 */
public class HttpClient implements Runnable {

    public static int NB = 20;

    public static int NBPERTHREAD = 20;

    private static ClientBootstrap bootstrap = null;

    private static String host = null;

    private static int port = 0;

    private static URI uriSimple;

    private static String baseURI;

    private static List<Entry<String, String>> headers = null;

    private static HttpDataFactory factory = null;

    private static File file = null;

    public static AtomicInteger ok = new AtomicInteger(0);

    public static AtomicInteger ko = new AtomicInteger(0);

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: " + HttpClient.class.getSimpleName() + " baseURI Filepath");
            return;
        }
        baseURI = args[0];
        String postSimple, postFile, get, getmeta;
        if (baseURI.endsWith("/")) {
            postSimple = baseURI + HttpFormattedHandler.ArkRequest.Post.name();
            postFile = baseURI + HttpFormattedHandler.ArkRequest.PostUpload.name();
            get = baseURI + HttpFormattedHandler.ArkRequest.Get.name();
            getmeta = baseURI + HttpFormattedHandler.ArkRequest.GetMeta.name();
        } else {
            postSimple = baseURI + "/" + HttpFormattedHandler.ArkRequest.Post.name();
            postFile = baseURI + "/" + HttpFormattedHandler.ArkRequest.PostUpload.name();
            get = baseURI + "/" + HttpFormattedHandler.ArkRequest.Get.name();
            getmeta = baseURI + "/" + HttpFormattedHandler.ArkRequest.GetMeta.name();
        }
        try {
            uriSimple = new URI(postSimple);
        } catch (URISyntaxException e) {
            System.err.println("Error: " + e.getMessage());
            return;
        }
        String scheme = uriSimple.getScheme() == null ? "http" : uriSimple.getScheme();
        host = uriSimple.getHost() == null ? "localhost" : uriSimple.getHost();
        port = uriSimple.getPort();
        if (port == -1) {
            if (scheme.equalsIgnoreCase("http")) {
                port = 80;
            } else if (scheme.equalsIgnoreCase("https")) {
                port = 443;
            }
        }
        if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
            System.err.println("Only HTTP(S) is supported.");
            return;
        }
        boolean ssl = scheme.equalsIgnoreCase("https");
        file = new File(args[1]);
        if (!file.canRead()) {
            System.err.println("A correct path is needed");
            return;
        }
        bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
        bootstrap.setPipelineFactory(new HttpClientPipelineFactory(ssl));
        factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);
        initHeader();
        if (headers == null) {
            factory.cleanAllHttpDatas();
            return;
        }
        Date date1 = new Date();
        launchThreads(get, ArkRequest.Get, (-NB / 2));
        Date date1b = new Date();
        launchThreads(getmeta, ArkRequest.GetMeta, (-NB / 2));
        Date date2 = new Date();
        long base = 500000;
        launchThreads(postSimple, ArkRequest.Post, base);
        Date date3 = new Date();
        base = -500000;
        launchThreads(postFile, ArkRequest.PostUpload, base);
        Date date4 = new Date();
        bootstrap.releaseExternalResources();
        factory.cleanAllHttpDatas();
        System.err.println("Get: " + (NB * NBPERTHREAD * 1000) / ((double) (date1b.getTime() - date1.getTime())));
        System.err.println("GetMeta: " + (NB * NBPERTHREAD * 1000) / ((double) (date2.getTime() - date1b.getTime())));
        System.err.println("Post: " + (NB * NBPERTHREAD * 1000) / ((double) (date3.getTime() - date2.getTime())));
        System.err.println("PostUpload: " + (NB * NBPERTHREAD * 1000) / ((double) (date4.getTime() - date3.getTime())));
        System.err.println("OK: " + ok.get() + " KO:" + ko.get());
    }

    public static void launchThreads(String url, ArkRequest request, long base) {
        ExecutorService pool = Executors.newFixedThreadPool(NB);
        HttpClient[] clients = new HttpClient[NB];
        switch(request) {
            case Get:
            case GetMeta:
                for (int i = 0; i < NB; i++) {
                    clients[i] = new HttpClient(url, request, base + i);
                }
                break;
            case Post:
            case PostUpload:
                for (int i = 0; i < NB; i++) {
                    clients[i] = new HttpClient(url, request, base + i * NBPERTHREAD * 2);
                }
                break;
        }
        for (int i = 0; i < NB; i++) {
            pool.execute(clients[i]);
        }
        pool.shutdown();
        try {
            pool.awaitTermination(100000, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return the list of headers that will be used in every example after
     * 
     */
    private static void initHeader() {
        HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, baseURI);
        request.setHeader(HttpHeaders.Names.HOST, host);
        request.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
        request.setHeader(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP + "," + HttpHeaders.Values.DEFLATE);
        request.setHeader(HttpHeaders.Names.ACCEPT_CHARSET, "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        request.setHeader(HttpHeaders.Names.ACCEPT_LANGUAGE, "fr");
        request.setHeader(HttpHeaders.Names.REFERER, uriSimple.toString());
        request.setHeader(HttpHeaders.Names.USER_AGENT, "Netty Simple Http Client side");
        request.setHeader(HttpHeaders.Names.ACCEPT, "text/html,text/plain,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        CookieEncoder httpCookieEncoder = new CookieEncoder(false);
        httpCookieEncoder.addCookie("my-cookie", "foo");
        httpCookieEncoder.addCookie("another-cookie", "bar");
        request.setHeader(HttpHeaders.Names.COOKIE, httpCookieEncoder.encode());
        headers = request.getHeaders();
    }

    /**
     * Standard usage of HTTP API in Netty without file Upload (get is not able to achieve File upload
     * due to limitation on request size).
    **/
    private static void formget(String get, ArkRequest req, long rank) {
        ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port));
        Channel channel = future.awaitUninterruptibly().getChannel();
        if (!future.isSuccess()) {
            future.getCause().printStackTrace();
            bootstrap.releaseExternalResources();
            return;
        }
        QueryStringEncoder encoder = new QueryStringEncoder(get);
        encoder.addParam(HttpFormattedHandler.ArkArgs.LEG.name(), "111");
        encoder.addParam(HttpFormattedHandler.ArkArgs.STO.name(), "-9151313343288442623");
        encoder.addParam(HttpFormattedHandler.ArkArgs.DID.name(), Long.toString(rank));
        encoder.addParam(HttpFormattedHandler.ArkArgs.CTYPE.name(), "text/plain");
        encoder.addParam(HttpFormattedHandler.ArkArgs.REQTYPE.name(), req.name());
        encoder.addParam("Send", "Send");
        URI uriGet;
        try {
            uriGet = new URI(encoder.toString());
        } catch (URISyntaxException e) {
            System.err.println("Error: " + e.getMessage());
            bootstrap.releaseExternalResources();
            return;
        }
        HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uriGet.toASCIIString());
        for (Entry<String, String> entry : headers) {
            request.setHeader(entry.getKey(), entry.getValue());
        }
        channel.write(request);
        channel.getCloseFuture().awaitUninterruptibly();
    }

    /**
     * Standard post without multipart but already support on Factory (memory management)
     * @param formurl
     * @param arkreq
     * @param rank
     */
    private static void formpost(String formurl, ArkRequest arkreq, long rank) {
        ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port));
        Channel channel = future.awaitUninterruptibly().getChannel();
        if (!future.isSuccess()) {
            future.getCause().printStackTrace();
            bootstrap.releaseExternalResources();
            return;
        }
        HttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, formurl);
        for (Entry<String, String> entry : headers) {
            request.setHeader(entry.getKey(), entry.getValue());
        }
        HttpPostRequestEncoder bodyRequestEncoder = null;
        try {
            bodyRequestEncoder = new HttpPostRequestEncoder(factory, request, (arkreq == ArkRequest.PostUpload));
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (ErrorDataEncoderException e) {
            e.printStackTrace();
        }
        try {
            bodyRequestEncoder.addBodyAttribute(ArkArgs.LEG.name(), "111");
            bodyRequestEncoder.addBodyAttribute(ArkArgs.STO.name(), "-9151313343288442623");
            bodyRequestEncoder.addBodyAttribute(ArkArgs.DID.name(), Long.toString(rank));
            bodyRequestEncoder.addBodyAttribute(ArkArgs.CTYPE.name(), "text/plain");
            bodyRequestEncoder.addBodyAttribute(ArkArgs.META.name(), textArea);
            if (arkreq == ArkRequest.PostUpload) {
                bodyRequestEncoder.addBodyAttribute(ArkArgs.REQTYPE.name(), ArkRequest.PostUpload.name());
                bodyRequestEncoder.addBodyFileUpload(ArkArgs.FILEDOC.name(), file, "application/x-zip-compressed", false);
            } else {
                bodyRequestEncoder.addBodyAttribute(ArkArgs.REQTYPE.name(), ArkRequest.Post.name());
                bodyRequestEncoder.addBodyAttribute(ArkArgs.FILENAME.name(), file.getAbsolutePath());
            }
            bodyRequestEncoder.addBodyAttribute("Send", "Send");
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (ErrorDataEncoderException e) {
            e.printStackTrace();
        }
        try {
            request = bodyRequestEncoder.finalizeRequest();
        } catch (ErrorDataEncoderException e) {
            e.printStackTrace();
        }
        channel.write(request);
        if (bodyRequestEncoder.isChunked()) {
            channel.write(bodyRequestEncoder).awaitUninterruptibly();
        }
        channel.getCloseFuture().awaitUninterruptibly();
    }

    public String url;

    public ArkRequest request;

    public long rank;

    /**
	 * @param url
	 * @param request
	 * @param rank
	 */
    private HttpClient(String url, ArkRequest request, long rank) {
        this.url = url;
        this.request = request;
        this.rank = rank;
    }

    @Override
    public void run() {
        switch(request) {
            case Get:
            case GetMeta:
                for (int i = 0; i < NBPERTHREAD; i++) formget(url, request, rank);
                break;
            case Post:
            case PostUpload:
                for (int i = 0; i < NBPERTHREAD; i++) formpost(url, request, rank + i);
                break;
        }
    }

    private static final String textArea = "lkjlkjlKJLKJLKJLKJLJlkj lklkj\r\n\r\nLKJJJJJJJJKKKKKKKKKKKKKKK ����&\r\n\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n" + "MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM\r\n";
}
