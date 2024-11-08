package jlibs.xml.sax.async;

import jlibs.core.nio.InputStreamChannel;
import jlibs.nbp.Feeder;
import jlibs.nbp.NBChannel;
import jlibs.nbp.NBParser;
import jlibs.nbp.NBReaderChannel;
import org.apache.xerces.impl.XMLEntityManager;
import org.xml.sax.InputSource;
import java.io.*;
import java.net.*;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * @author Santhosh Kumar T
 */
public class XMLFeeder extends Feeder {

    AsyncXMLReader xmlReader;

    String publicID;

    String systemID;

    Runnable postAction;

    public XMLFeeder(AsyncXMLReader xmlReader, NBParser parser, InputSource source, XMLScanner declParser) throws IOException {
        super(parser);
        this.xmlReader = xmlReader;
        init(source, declParser);
    }

    public static String toURL(String systemID) throws IOException {
        if (systemID == null) return null;
        int ix = systemID.indexOf(':', 0);
        if (ix >= 3 && ix <= 8) return systemID; else {
            String absPath = new File(systemID).getAbsolutePath();
            char sep = File.separatorChar;
            if (sep != '/') absPath = absPath.replace(sep, '/');
            if (absPath.length() > 0 && absPath.charAt(0) != '/') absPath = "/" + absPath;
            return new URL("file", "", absPath).toString();
        }
    }

    final void init(InputSource is, XMLScanner prologParser) throws IOException {
        postAction = null;
        iProlog = 0;
        this.prologParser = prologParser;
        elemDepth = 0;
        publicID = is.getPublicId();
        systemID = toURL(is.getSystemId());
        Reader charStream = is.getCharacterStream();
        if (charStream != null) setChannel(new NBReaderChannel(charStream)); else {
            ReadableByteChannel byteChannel = null;
            String encoding = is.getEncoding();
            if (is instanceof ChannelInputSource) {
                ChannelInputSource channelInputSource = (ChannelInputSource) is;
                byteChannel = channelInputSource.getChannel();
            }
            if (byteChannel == null) {
                InputStream inputStream = is.getByteStream();
                if (inputStream == null) {
                    assert systemID != null;
                    if (systemID.startsWith("file:/")) {
                        try {
                            inputStream = new FileInputStream(new File(new URI(systemID)));
                        } catch (URISyntaxException ex) {
                            throw new IOException(ex);
                        }
                    } else {
                        URLConnection con = new URL(systemID).openConnection();
                        if (con instanceof HttpURLConnection) {
                            final HttpURLConnection httpCon = (HttpURLConnection) con;
                            XMLEntityManager.setInstanceFollowRedirects(httpCon, true);
                        }
                        inputStream = con.getInputStream();
                        String contentType;
                        String charset = null;
                        String rawContentType = con.getContentType();
                        int index = (rawContentType != null) ? rawContentType.indexOf(';') : -1;
                        if (index != -1) {
                            contentType = rawContentType.substring(0, index).trim();
                            charset = rawContentType.substring(index + 1).trim();
                            if (charset.startsWith("charset=")) {
                                charset = charset.substring(8).trim();
                                if ((charset.charAt(0) == '"' && charset.charAt(charset.length() - 1) == '"') || (charset.charAt(0) == '\'' && charset.charAt(charset.length() - 1) == '\'')) {
                                    charset = charset.substring(1, charset.length() - 1);
                                }
                            }
                        } else contentType = rawContentType.trim();
                        String detectedEncoding = null;
                        if (contentType.equals("text/xml")) {
                            if (charset != null) detectedEncoding = charset; else detectedEncoding = "US-ASCII";
                        } else if (contentType.equals("application/xml")) {
                            if (charset != null) detectedEncoding = charset;
                        }
                        if (detectedEncoding != null) encoding = detectedEncoding;
                    }
                }
                byteChannel = new InputStreamChannel(inputStream);
            }
            nbChannel.setChannel(byteChannel);
            if (encoding == null) nbChannel.setEncoding("UTF-8", true); else nbChannel.setEncoding(encoding, false);
            setChannel(nbChannel);
        }
    }

    private NBChannel nbChannel = new NBChannel(null);

    private int iProlog = 0;

    CharBuffer singleChar = CharBuffer.allocate(1);

    CharBuffer sixChars = CharBuffer.allocate(6);

    XMLScanner prologParser;

    private static final int MAX_PROLOG_LENGTH = 70;

    @Override
    protected Feeder read() throws IOException {
        xmlReader.setFeeder(this);
        if (prologParser != null) {
            while (iProlog < 6) {
                sixChars.clear();
                int read = channel.read(sixChars);
                if (read == 0) return this; else if (read == -1) {
                    charBuffer.append("<?xml ", 0, iProlog);
                    return onPrologEOF();
                } else {
                    char chars[] = sixChars.array();
                    for (int i = 0; i < read; i++) {
                        char ch = chars[i];
                        if (isPrologStart(ch)) {
                            iProlog++;
                            if (iProlog == 6) {
                                charBuffer.append("<?xml ");
                                for (i = 0; i < MAX_PROLOG_LENGTH; i++) {
                                    singleChar.clear();
                                    read = channel.read(singleChar);
                                    if (read == 1) {
                                        ch = singleChar.get(0);
                                        charBuffer.append(ch);
                                        if (ch == '>') break;
                                    } else break;
                                }
                                if (charBuffer.position() > 0) {
                                    charBuffer.flip();
                                    charBuffer.position(prologParser.consume(charBuffer.array(), charBuffer.position(), charBuffer.limit(), false));
                                    charBuffer.compact();
                                }
                                if (read == 0) return this; else if (read == -1) return onPrologEOF();
                                break;
                            }
                        } else {
                            charBuffer.append("<?xml ", 0, iProlog);
                            while (i < read) charBuffer.append(chars[i++]);
                            iProlog = 7;
                            prologParser = null;
                            break;
                        }
                    }
                }
            }
            while (iProlog != 7) {
                singleChar.clear();
                int read = channel.read(singleChar);
                if (read == 0) return this; else if (read == -1) return onPrologEOF(); else prologParser.consume(singleChar.array(), 0, 1, false);
            }
        }
        return super.read();
    }

    private Feeder onPrologEOF() throws IOException {
        charBuffer.flip();
        channel.close();
        channel = null;
        return super.read();
    }

    private boolean isPrologStart(char ch) {
        switch(iProlog) {
            case 0:
                return ch == '<';
            case 1:
                return ch == '?';
            case 2:
                return ch == 'x';
            case 3:
                return ch == 'm';
            case 4:
                return ch == 'l';
            case 5:
                return ch == 0x20 || ch == 0x9 || ch == 0xa || ch == 0xd;
            default:
                throw new Error("impossible");
        }
    }

    void setDeclaredEncoding(String encoding) {
        iProlog = 7;
        parser.setLocation(prologParser);
        if (encoding != null && channel instanceof NBChannel) {
            NBChannel nbChannel = (NBChannel) channel;
            String detectedEncoding = nbChannel.decoder().charset().name().toUpperCase(Locale.ENGLISH);
            String declaredEncoding = encoding.toUpperCase(Locale.ENGLISH);
            if (!detectedEncoding.equals(declaredEncoding)) {
                if (detectedEncoding.startsWith("UTF-16") && declaredEncoding.equals("UTF-16")) return;
                if (!detectedEncoding.equals(encoding)) nbChannel.decoder(Charset.forName(encoding).newDecoder());
            }
        }
    }

    public InputSource resolve(String publicID, String systemID) throws IOException {
        InputSource inputSource = new InputSource(resolve(systemID));
        inputSource.setPublicId(publicID);
        return inputSource;
    }

    public String resolve(String systemID) throws IOException {
        if (systemID == null) return null; else {
            if (this.systemID == null) return toURL(systemID); else {
                if (systemID.length() == 0) return systemID;
                int ix = systemID.indexOf(':', 0);
                if (ix >= 3 && ix <= 8) return systemID; else {
                    try {
                        return new URI(this.systemID).resolve(new URI(systemID)).toString();
                    } catch (URISyntaxException ex) {
                        return systemID;
                    }
                }
            }
        }
    }

    int elemDepth;
}
