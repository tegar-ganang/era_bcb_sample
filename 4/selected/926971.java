package net.siuying.any2rss.core;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.configuration.*;
import net.siuying.any2rss.handler.ContentHandlerException;
import net.siuying.any2rss.handler.ContentHandlerIF;
import net.siuying.any2rss.loader.ContentLoaderIF;
import net.siuying.any2rss.loader.LoaderException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import de.nava.informa.core.ChannelIF;
import de.nava.informa.exporters.RSS_1_0_Exporter;
import de.nava.informa.impl.basic.ChannelBuilder;

/**
 * <h2>Class to process request</h2>
 * <p>
 * A processor have exactly one ContentLoader and one ContentHandler,
 * ContentLoader retrieve the content, ContentHandler convert them into a RSS
 * feed
 * </p>
 * 
 * @author Francis Chong
 * @version $Revision: 1.3 $
 */
public class ContentProcessor {

    private static Log log = LogFactory.getLog(ContentProcessor.class);

    private CompositeConfiguration config;

    private ContentHandlerIF handler;

    private ContentLoaderIF loader;

    private ChannelIF channel;

    private URL contentUrl;

    /**
     * Creates a new ContentProcessor object.
     * 
     * @throws ConfigurationException
     */
    public ContentProcessor() throws ConfigurationException {
        config = new CompositeConfiguration();
        config.addConfiguration(new SystemConfiguration());
    }

    /**
     * Create a content processor that search config in specified path
     * 
     * @param propertyFile
     * @throws ConfigurationException
     */
    public ContentProcessor(String propertyFile) throws ConfigurationException {
        config = new CompositeConfiguration();
        config.addConfiguration(new SystemConfiguration());
        config.addConfiguration(new PropertiesConfiguration(propertyFile));
    }

    /**
     * @param config
     * @throws ConfigurationException
     */
    public ContentProcessor(CompositeConfiguration config) throws ConfigurationException {
        this.config = config;
    }

    /**
     * Set the Loader of this processor, a loader load the content to processor
     * 
     * @param loader the loader
     * 
     * @throws ConfigurationException some config parameter is missing or
     *             invalid
     */
    public void setLoader(ContentLoaderIF loader) throws ConfigurationException {
        this.loader = loader;
        loader.configure(config);
    }

    /**
     * Set the Handler of this processor, a handler process the content and
     * create RSS feed
     * 
     * @param handler the Handler
     * 
     * @throws ConfigurationException DOCUMENT ME!
     */
    public void setHandler(ContentHandlerIF handler) throws ConfigurationException {
        this.handler = handler;
        handler.configure(config);
    }

    public void setContentUrl(URL url) {
        contentUrl = url;
    }

    public URL getContentUrl() {
        return contentUrl;
    }

    /**
     * load the content and process them, loader and handler must have been set
     * before calling this, or a ContentProcessorException is thrown
     * 
     * @throws LoaderException
     * @throws ContentHandlerException
     * @throws ContentProcessorException
     */
    public void process() throws LoaderException, ContentHandlerException, ContentProcessorException {
        log.debug("start process content");
        if ((loader == null) || (handler == null) || (contentUrl == null)) {
            log.fatal("Connot process because URL, Loader or Handler is undefined.");
            throw new ContentProcessorException("Connot process because Loader and Handler is undefined.", new IllegalArgumentException("Connot process because Loader and Handler is undefined."));
        }
        String content = null;
        try {
            log.trace("loading content: " + getContentUrl().toString());
            content = loader.load(getContentUrl().toString());
            log.debug(content);
        } catch (LoaderException le) {
            log.fatal("Error loading content: " + le.getMessage());
            throw le;
        }
        if (content == null) {
            log.fatal("No content retrieved!");
            throw new ContentHandlerException("No content retrieved!");
        }
        try {
            log.trace("handling content");
            channel = handler.handle(content);
        } catch (ContentHandlerException e) {
            log.fatal("Error handling content: " + e.getMessage());
            throw new ContentHandlerException("Error handling content!", e);
        }
        channel.setGenerator("http2rss 1.0");
        log.debug("completed process content, channel generated");
    }

    /**
     * Get the processed channel, the channel is null until process() is
     * completed
     * 
     * @return The processed channel
     */
    public ChannelIF getChannel() {
        return channel;
    }

    /**
     * Get the configuration
     * 
     * @return the configuration of this processer
     */
    public Configuration getConfig() {
        return config;
    }

    /**
     * Set the configuration
     * 
     * @param ConfiguratorIF config to set
     */
    public void setConfig(CompositeConfiguration config) {
        this.config = config;
    }

    /**
     * Main method
     * 
     * @param args
     */
    public static void main(String[] args) {
        ContentProcessor proc = null;
        try {
            proc = new ContentProcessor();
        } catch (ConfigurationException ce) {
            System.err.println("Error loading configuration: " + ce.getMessage());
            System.exit(4);
        }
        ChannelIF channel = null;
        ContentLoaderIF loader = null;
        ContentHandlerIF handler = null;
        try {
            loader = (ContentLoaderIF) Class.forName(proc.getConfig().getString("loader.class")).newInstance();
        } catch (Exception e) {
            System.err.println("Cannot finding ContentLoader: " + proc.getConfig().getString("loader.class"));
            System.exit(6);
        }
        try {
            handler = (ContentHandlerIF) Class.forName(proc.getConfig().getString("handler.class")).newInstance();
            handler.setChannelBuilder(new ChannelBuilder());
        } catch (Exception e) {
            System.err.println("Cannot finding ContentHandler: " + proc.getConfig().getString("handler.class"));
            System.exit(6);
        }
        try {
            proc.setContentUrl(new URL(proc.getConfig().getString("target.input.url")));
            proc.setLoader(loader);
            proc.setHandler(handler);
        } catch (ConfigurationException ce) {
            System.err.println("Error loading configuration: " + ce.getMessage());
            System.exit(4);
        } catch (MalformedURLException me) {
            System.err.println("URL not specified in target.input.url, or the URL is invalid: " + proc.getConfig().getString("target.input.url") + ", " + me.getMessage());
            System.exit(4);
        }
        try {
            proc.process();
            channel = proc.getChannel();
        } catch (LoaderException le) {
            System.err.println("Error loading resources: " + le.getCause().getMessage());
            System.exit(1);
        } catch (ContentHandlerException che) {
            System.err.println("Error handling content: " + che.getMessage());
            System.exit(2);
        } catch (ContentProcessorException cpe) {
            System.err.println("Error occured while converting content: " + cpe.getCause().getMessage());
            System.exit(3);
        }
        String filename = proc.getConfig().getString("target.output.file", "http2rss.xml");
        RSS_1_0_Exporter exporter = null;
        if (filename == null) {
            System.err.println("No output file specified, please specify it with target.output.file ");
            System.exit(5);
        } else {
            File rssFile = new File(filename);
            String encoding = proc.getConfig().getString("encoding");
            try {
                exporter = new RSS_1_0_Exporter(rssFile, encoding);
            } catch (IOException ioe) {
                System.err.println("Error writing output file: " + filename + ", " + ioe.getMessage());
                System.exit(3);
            }
        }
        try {
            exporter.write(channel);
        } catch (IOException ie) {
            System.err.println("Error writing output: " + ie.getMessage());
            System.exit(5);
        }
    }
}
