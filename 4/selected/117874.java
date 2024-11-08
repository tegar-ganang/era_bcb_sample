package com.googlecode.voctopus.request.handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import com.googlecode.voctopus.RequestResponseMediator.ReasonPhase;
import com.googlecode.voctopus.config.VOctopusConfigurationManager;

/**
 * Handler for ASCII-based files.
 * 
 * @author marcello Feb 11, 2008 4:16:43 PM
 */
public class AsciiContentRequestHandlerStrategy extends AbstractRequestHandler {

    private static final Logger logger = Logger.getLogger(AsciiContentRequestHandlerStrategy.class);

    /**
     * Creates a new handler for ASCII based content
     * 
     * @param uri
     * @param requestedFile
     * @param handlerFound
     * @param reasonPhrase
     */
    public AsciiContentRequestHandlerStrategy(URI uri, File requestedFile, String handlerFound, ReasonPhase reasonPhrase) {
        super(uri, requestedFile, RequestType.ASCII, handlerFound);
        this.status = reasonPhrase;
    }

    public String[] getResourceLines() throws IOException {
        if (this.requestedResourceExists()) {
            logger.debug("Serving the file " + this.getRequestedFile());
            FileChannel channel = new FileInputStream(this.getRequestedFile()).getChannel();
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, this.getRequestedFile().length());
            Charset charset = Charset.forName("ISO-8859-1");
            CharsetDecoder decoder = charset.newDecoder();
            CharBuffer charBuffer = decoder.decode(buffer);
            List<String> lines = new ArrayList<String>();
            StringBuilder builder = new StringBuilder();
            if (charBuffer.length() == 0) {
                this.setStatus(ReasonPhase.STATUS_204);
                return new String[] { "" };
            }
            for (int i = 0, n = charBuffer.length(); i < n; i++) {
                char charValue = charBuffer.get();
                if (charValue != '\n') {
                    builder.append(charValue);
                } else {
                    lines.add(builder.toString());
                    builder.delete(0, builder.capacity());
                }
            }
            return lines.toArray(new String[lines.size()]);
        } else {
            return new String[] { "" };
        }
    }

    public String[] getParticularResponseHeaders() {
        return null;
    }

    public static File getFilePathForAlias(URI uri) {
        if (uri.getPath().equals("/")) return null;
        String alias = "/" + uri.getPath().split("/")[1] + "/";
        String aliasValue = VOctopusConfigurationManager.WebServerProperties.ALIAS.getPropertyValue(alias);
        if (aliasValue != null) {
            String paths[] = uri.getPath().split("/");
            String filePath = aliasValue;
            for (int i = 2; i < paths.length; i++) {
                filePath = filePath + "/" + paths[i];
            }
            File fileAlias = new File(filePath);
            if (fileAlias.exists()) {
                return fileAlias;
            } else return null;
        } else {
            return null;
        }
    }
}
