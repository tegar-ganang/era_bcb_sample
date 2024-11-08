package net.sf.jbaobab.tutorial;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.TreeMap;
import net.sf.jbaobab.io.impl.RoughHttpPacket;

/**
 * <p>
 * A helper class for {@link RoughHttpHandler}.
 * </p>
 *
 * @see RoughHttpHandler
 * @author Oakyoon Cha
 */
public class RoughHttpHelper {

    private static String ERROR_TEMPLATE;

    static {
        ERROR_TEMPLATE = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n";
        ERROR_TEMPLATE += "<html>\n";
        ERROR_TEMPLATE += "\t<head>\n";
        ERROR_TEMPLATE += "\t\t<title>{TITLE}</title>\n";
        ERROR_TEMPLATE += "\t</head>\n";
        ERROR_TEMPLATE += "\t<body>\n";
        ERROR_TEMPLATE += "\t\t<h1>{HEADER}</h1>\n";
        ERROR_TEMPLATE += "\t\t<hr />\n";
        ERROR_TEMPLATE += "\t\t<p>Powered by " + RoughHttpHandler.class.getCanonicalName() + "</p>\n";
        ERROR_TEMPLATE += "\t</body>\n";
        ERROR_TEMPLATE += "</html>\n";
    }

    private static final Map<String, String> MIME_MAP;

    static {
        MIME_MAP = new TreeMap<String, String>();
        MIME_MAP.put("bmp", "image/bmp");
        MIME_MAP.put("gif", "image/gif");
        MIME_MAP.put("jpeg", "image/jpeg");
        MIME_MAP.put("jpg", "image/jpeg");
        MIME_MAP.put("png", "image/png");
        MIME_MAP.put("ico", "image/x-icon");
        MIME_MAP.put("css", "text/css");
        MIME_MAP.put("htm", "text/html");
        MIME_MAP.put("html", "text/html");
        MIME_MAP.put("js", "text/javascript");
        MIME_MAP.put("txt", "text/plain");
        MIME_MAP.put("xml", "text/xml");
    }

    private static final String PARAM_SEPERATOR = "?";

    private static final String PATH_SEPERATOR = "/";

    private static final String WEB_ROOT = "doc";

    private static final String DIRECTORY_INDEX = "index.html";

    private RoughHttpHelper() {
        ;
    }

    public static void validateCommand(RoughHttpPacket packet) {
        if (packet.countCommands() != 3) throw RoughHttpError.HTTP_400;
        String version = packet.getCommand(2);
        if (!version.equals("HTTP/1.0") && !version.equals("HTTP/1.1")) throw RoughHttpError.HTTP_505;
        String head = packet.getCommand(0);
        if (!head.equals("GET") && !head.equals("POST")) throw RoughHttpError.HTTP_501;
    }

    public static String urlDecode(String str) {
        try {
            return URLDecoder.decode(str, "iso8859-1");
        } catch (UnsupportedEncodingException e) {
            throw RoughHttpError.HTTP_500;
        }
    }

    public static String trimParams(String uri) {
        if (uri == null) return null;
        int paramStart = uri.indexOf(PARAM_SEPERATOR);
        if (paramStart >= 0) uri = uri.substring(0, paramStart);
        return uri;
    }

    public static ByteBuffer fileContent(String path) {
        File file = new File(WEB_ROOT + path);
        if (!file.exists()) throw RoughHttpError.HTTP_404;
        if (file.isDirectory()) {
            if (path.endsWith(PATH_SEPERATOR)) {
                file = new File(WEB_ROOT + path + DIRECTORY_INDEX);
                if (!file.exists()) throw RoughHttpError.HTTP_403;
            } else {
                RoughHttpError e = new RoughHttpError(302);
                e.extras().put("Location", path + PATH_SEPERATOR);
                throw e;
            }
        }
        if (!file.canRead()) throw RoughHttpError.HTTP_403;
        FileInputStream stream;
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw RoughHttpError.HTTP_404;
        }
        FileChannel channel = stream.getChannel();
        MappedByteBuffer buffer;
        try {
            buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
        } catch (IOException e) {
            throw RoughHttpError.HTTP_500;
        }
        return buffer.load();
    }

    public static String fileExtension(String path) {
        if (path == null) return null;
        int ext = path.lastIndexOf('.');
        if (ext < 0) return null;
        return path.substring(ext).toLowerCase();
    }

    public static String mimeType(String ext) {
        if (ext == null || !MIME_MAP.containsKey(ext)) return null;
        return MIME_MAP.get(ext);
    }

    public static String errorPage(Integer code, String description) {
        String text = String.valueOf(code) + ' ' + description;
        return ERROR_TEMPLATE.replaceAll("(\\{TITLE\\}|\\{HEADER\\})", text);
    }
}
