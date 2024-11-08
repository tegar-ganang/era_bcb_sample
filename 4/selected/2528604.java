package slojj.dotsbox.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UTFDataFormatException;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.JDOMParseException;
import org.jdom.input.SAXBuilder;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import slojj.dotsbox.spider.HttpSpider;
import slojj.dotsbox.spider.SpiderException;

public class ChannelBuilder {

    private static final int STATUS_NOT_MODIFIED = 304;

    private static final int STATUS_AUTH_REQUIRED = 401;

    private final String link;

    private Channel channel = null;

    private Document document = null;

    public ChannelBuilder(String link) throws ChannelBuilderException {
        this.link = link;
        openDocument(false);
        if (document == null) return;
        FeedParser parser = new FeedParser(document, link);
        parser.parse();
        channel = parser.getChannel();
    }

    private Document openFromLocal(SAXBuilder builder, boolean forceDefaultEncoding) throws ChannelBuilderException {
        Document doc = null;
        try {
            if (!forceDefaultEncoding) {
                doc = builder.build(new File(link));
            } else {
                doc = builder.build(new InputStreamReader(new FileInputStream(link)));
            }
        } catch (JDOMException e) {
            throw new ChannelBuilderException(link, ChannelBuilderException.ERROR_INVALID_XML, e.getMessage());
        } catch (UTFDataFormatException e) {
            throw new ChannelBuilderException(link, ChannelBuilderException.ERROR_INVALID_XML, e.getMessage());
        } catch (IOException e) {
            throw new ChannelBuilderException(link, ChannelBuilderException.ERROR_FILE_NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ChannelBuilderException(link, ChannelBuilderException.ERROR_INVALID_XML, e.getMessage());
        }
        return doc;
    }

    private void openDocument(boolean forceDefaultEncoding) throws ChannelBuilderException {
        SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        document = null;
        setDefaultEntityResolver(builder);
        if (new File(link).exists()) {
            document = openFromLocal(builder, forceDefaultEncoding);
        } else {
            HttpSpider spider = new HttpSpider();
            try {
                InputStream is = spider.crawl(link);
                int rc = spider.getReturnCode();
                if (rc == STATUS_NOT_MODIFIED) {
                    throw new ChannelBuilderException(link, ChannelBuilderException.INFO_FEED_NOT_MODIFIED, null);
                }
                assert (is != null);
                if (!forceDefaultEncoding) {
                    document = builder.build(is);
                } else {
                    document = builder.build(new InputStreamReader(is));
                }
            } catch (SpiderException e) {
                int rc = spider.getReturnCode();
                if (rc == STATUS_AUTH_REQUIRED) {
                    throw new ChannelBuilderException(link, ChannelBuilderException.ERROR_AUTH_REQUIRED, e.getMessage());
                }
                throw new ChannelBuilderException(link, ChannelBuilderException.ERROR_FILE_NOT_FOUND, e.getMessage());
            } catch (JDOMParseException e) {
                throw new ChannelBuilderException(link, ChannelBuilderException.ERROR_INVALID_XML, e.getMessage());
            } catch (JDOMException e) {
                throw new ChannelBuilderException(link, ChannelBuilderException.ERROR_INVALID_XML, e.getMessage());
            } catch (UTFDataFormatException e) {
                if (!forceDefaultEncoding) {
                    openDocument(true);
                } else {
                    throw new ChannelBuilderException(link, ChannelBuilderException.ERROR_INVALID_XML, e.getMessage());
                }
            } catch (IOException e) {
                throw new ChannelBuilderException(link, ChannelBuilderException.ERROR_FILE_NOT_FOUND, e.getMessage());
            } catch (IllegalArgumentException e) {
                throw new ChannelBuilderException(link, ChannelBuilderException.ERROR_INVALID_XML, e.getMessage());
            } finally {
                spider.close();
            }
        }
    }

    public Channel getChannel() {
        return channel;
    }

    private static void setDefaultEntityResolver(SAXBuilder builder) {
        builder.setEntityResolver(new EntityResolver() {

            public InputSource resolveEntity(String publicId, String systemId) {
                return new InputSource(ChannelBuilder.class.getResourceAsStream("/metadata/dtd/entities.dtd"));
            }
        });
    }
}
