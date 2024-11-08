package extract;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.IOUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AbstractParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class ImageSavingParser extends AbstractParser {

    private static final long serialVersionUID = 1L;

    private Map<String, File> wanted = new HashMap<String, File>();

    private Parser downstreamParser;

    public ImageSavingParser(Parser downstreamParser) {
        this.downstreamParser = downstreamParser;
        try {
            File t = File.createTempFile("tika", ".test");
            t.getParentFile();
        } catch (IOException e) {
        }
    }

    public File requestSave(String embeddedName) throws IOException {
        String suffix = embeddedName.substring(embeddedName.lastIndexOf('.'));
        File tmp = File.createTempFile("tika-embedded-", suffix);
        wanted.put(embeddedName, tmp);
        return tmp;
    }

    public Set<MediaType> getSupportedTypes(ParseContext context) {
        return null;
    }

    public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context) throws IOException, SAXException, TikaException {
        String name = metadata.get(Metadata.RESOURCE_NAME_KEY);
        if (name != null && wanted.containsKey(name)) {
            FileOutputStream out = new FileOutputStream(wanted.get(name));
            IOUtils.copy(stream, out);
            out.close();
        } else {
            if (downstreamParser != null) {
                downstreamParser.parse(stream, handler, metadata, context);
            }
        }
    }
}
