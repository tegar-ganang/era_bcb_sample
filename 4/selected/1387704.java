package com.gcsf.books.engine.util;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import com.gcsf.books.engine.EngineActivator;

/**
 * Helper class for various Core operations.
 * 
 * @author bpasero
 */
public class CoreUtils {

    /** Folder Index Value for Long Arrays */
    public static final int FOLDER = 0;

    /** Bookmark Index Value for Long Arrays */
    public static final int BOOKMARK = 1;

    /** Newsbin Index Value for Long Arrays */
    public static final int NEWSBIN = 2;

    private static final String[] FEED_MIME_TYPES = new String[] { "application/rss+xml", "application/atom+xml", "application/rdf+xml" };

    private static final String[] FAVICON_MARKERS = new String[] { "shortcut icon", ".ico" };

    private static final String[] HREF_VARIANTS = new String[] { "href = ", "href= ", "href=", "HREF=", "href =" };

    private static final String[] RESERVED_FILENAME_CHARACTERS_WINDOWS = new String[] { "<", ">", ":", "\"", "/", "\\", "|", "?", "*" };

    private CoreUtils() {
    }

    /**
   * Normalizes the given Title by removing various kinds of response codes
   * (e.g. Re).
   * 
   * @param title
   *          The title to normalize.
   * @return Returns the normalized Title (that is, response codes have been
   *         removed).
   */
    public static String normalizeTitle(String title) {
        if (!StringUtils.isSet(title)) return title;
        String normalizedTitle = null;
        int start = 0;
        int len = title.length();
        boolean done = false;
        while (!done) {
            done = true;
            while (start < len && title.charAt(start) == ' ') start++;
            if (start < (len - 2)) {
                char c1 = title.charAt(start);
                char c2 = title.charAt(start + 1);
                char c3 = title.charAt(start + 2);
                if ((c1 == 'r' || c1 == 'R') && (c2 == 'e' || c2 == 'E')) {
                    if (c3 == ':') {
                        start += 3;
                        done = false;
                    } else if (start < (len - 2) && (c3 == '[' || c3 == '(')) {
                        int i = start + 3;
                        while (i < len && title.charAt(i) >= '0' && title.charAt(i) <= '9') i++;
                        char ci1 = title.charAt(i);
                        char ci2 = title.charAt(i + 1);
                        if (i < (len - 1) && (ci1 == ']' || ci1 == ')') && ci2 == ':') {
                            start = i + 2;
                            done = false;
                        }
                    }
                }
            }
            int end = len;
            while (end > start && title.charAt(end - 1) < ' ') end--;
            if (start == 0 && end == len) normalizedTitle = title; else normalizedTitle = title.substring(start, end);
        }
        return normalizedTitle;
    }

    /**
   * Copies the contents of one stream to another.
   * 
   * @param fis
   *          the input stream to read from.
   * @param fos
   *          the output stream to write to.
   */
    public static void copy(InputStream fis, OutputStream fos) {
        try {
            byte buffer[] = new byte[0xffff];
            int nbytes;
            while ((nbytes = fis.read(buffer)) != -1) fos.write(buffer, 0, nbytes);
        } catch (IOException e) {
            EngineActivator.safeLogError(e.getMessage(), e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    EngineActivator.safeLogError(e.getMessage(), e);
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    EngineActivator.safeLogError(e.getMessage(), e);
                }
            }
        }
    }

    /**
   * @param fileName
   *          the name of the file to write the content into.
   * @param content
   *          the content to write into the file as {@link StringBuilder}.
   */
    public static void write(String fileName, StringBuilder content) {
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8");
            writer.write(content.toString());
            writer.close();
        } catch (IOException e) {
            EngineActivator.safeLogError(e.getMessage(), e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    EngineActivator.safeLogError(e.getMessage(), e);
                }
            }
        }
    }

    /**
   * @param <T>
   *          the type of elements of the list.
   * @param list
   *          a list to remove duplicates from using object identity equal
   *          checks.
   * @return returns a list where all duplicates are removed using object
   *         identiy equalness.
   */
    public static <T> List<T> removeIdentityDuplicates(List<T> list) {
        List<T> newList = new ArrayList<T>(list.size());
        Map<T, T> identityMap = new IdentityHashMap<T, T>();
        for (T t : list) {
            if (!identityMap.containsKey(t)) {
                newList.add(t);
                identityMap.put(t, t);
            }
        }
        return newList;
    }

    /**
   * @param reader
   *          a {@link BufferedReader} to read from. The caller is responsible
   *          to close any streams associated with it.
   * @param base
   *          the base {@link URI} to resolve any relative {@link URI} against.
   * @return a {@link URI} of a feed found in the content of the reader or
   *         <code>null</code> if none.
   */
    public static URI findFeed(BufferedReader reader, URI base) {
        return findUri(reader, base, FEED_MIME_TYPES);
    }

    /**
   * @param reader
   *          a {@link BufferedReader} to read from. The caller is responsible
   *          to close any streams associated with it.
   * @param base
   *          the base {@link URI} to resolve any relative {@link URI} against.
   * @return a {@link URI} of a favicon found in the content of the reader or
   *         <code>null</code> if none.
   */
    public static URI findFavicon(BufferedReader reader, URI base) {
        return findUri(reader, base, FAVICON_MARKERS);
    }

    private static URI findUri(BufferedReader reader, URI base, String[] markers) {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                for (String marker : markers) {
                    int index = line.indexOf(marker);
                    if (index > -1) {
                        for (int i = index; i >= 0; i--) {
                            if (line.charAt(i) == '<') {
                                index = i;
                                break;
                            }
                        }
                        String usedHref = null;
                        int hrefIndex = -1;
                        for (String href : HREF_VARIANTS) {
                            hrefIndex = line.indexOf(href, index);
                            if (hrefIndex != -1) {
                                usedHref = href;
                                break;
                            }
                        }
                        if (hrefIndex > -1 && usedHref != null) {
                            boolean inQuotes = false;
                            StringBuilder str = new StringBuilder();
                            for (int i = hrefIndex + usedHref.length(); i < line.length(); i++) {
                                char c = line.charAt(i);
                                if (c == '\"' || c == '\'') {
                                    if (inQuotes) break;
                                    inQuotes = true;
                                    continue;
                                }
                                if (Character.isWhitespace(c) || c == '>') break;
                                str.append(c);
                            }
                            String linkVal = str.toString();
                            linkVal = StringUtils.replaceAll(linkVal, "&amp;", "&");
                            try {
                                URI uri = new URI(linkVal);
                                linkVal = URIUtils.resolve(base, uri).toString();
                            } catch (URISyntaxException e) {
                                if (!linkVal.contains("://")) {
                                    try {
                                        if (!linkVal.startsWith("/")) linkVal = "/" + linkVal;
                                        linkVal = base.resolve(linkVal).toString();
                                    } catch (IllegalArgumentException e1) {
                                        linkVal = linkVal.startsWith("/") ? base.toString() + linkVal : base.toString() + "/" + linkVal;
                                    }
                                }
                            }
                            return new URI(URIUtils.fastEncode(linkVal));
                        }
                    }
                }
            }
        } catch (IOException e) {
            EngineActivator.safeLogError(e.getMessage(), e);
        } catch (URISyntaxException e) {
            EngineActivator.safeLogError(e.getMessage(), e);
        }
        return null;
    }

    /**
   * @param fileName
   *          the proposed filename.
   * @return a filename that is safe to be used on Windows.
   */
    public static String getSafeFileNameForWindows(String fileName) {
        String candidate = fileName;
        for (String reservedChar : RESERVED_FILENAME_CHARACTERS_WINDOWS) {
            candidate = StringUtils.replaceAll(candidate, reservedChar, "");
        }
        return candidate;
    }
}
