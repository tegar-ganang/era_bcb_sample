package org.zkoss.web.util.resource;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import javax.servlet.ServletContext;
import org.zkoss.lang.D;
import org.zkoss.lang.Exceptions;
import org.zkoss.lang.SystemException;
import org.zkoss.util.resource.ResourceCache;
import org.zkoss.util.logging.Log;
import org.zkoss.io.Files;
import org.zkoss.web.servlet.Servlets;

/**
 * Utilities to load (and parse) the servlet resource.
 *
 * <p>Usage 1:
 * <ol>
 * <li>Use {@link #getContent} to load the resource into a String-type content.
 * </ol>
 *
 * <p>Usage 2:
 * <ol>
 * <li>Implements a loader by extending from {@link ResourceLoader}.</li>
 * <li>Creates a resource cache ({@link ResourceCache})
 * by use of the loader in the previous step.</li>
 * <li>Invoke {@link #get} to load the resource.</li>
 * </ol>
 *
 * <p>Usage 2 has better performance because you need to parse the content
 * only once. Usage 1 is simple if you don't pase it into any intermediate
 * format.
 *
 * @author tomyeh
 */
public class ResourceCaches {

    private static final Log log = Log.lookup(ResourceCaches.class);

    /** Loads, parses and returns the resource of the specified URI,
	 * or null if not found. The parser is defined by the loader defined
	 * in {@link ResourceCache}.
	 *
	 * <p>If you don't need to parse the content, you might use
	 * {@link #getContent}
	 *
	 * @param cache the resource cache.
	 * Note: its loader must extend from {@link ResourceLoader}.
	 * @param path the URI path
	 * @param extra the extra parameter that will be passed to
	 * {@link ResourceLoader#parse(String,File,Object)} and
	 * {@link ResourceLoader#parse(String,URL,Object)}
	 */
    public static final Object get(ResourceCache cache, ServletContext ctx, String path, Object extra) {
        URL url = null;
        if (path == null || path.length() == 0) path = "/"; else if (path.charAt(0) != '/') {
            if (path.indexOf("://") > 0) {
                try {
                    url = new URL(path);
                } catch (java.net.MalformedURLException ex) {
                    throw new SystemException(ex);
                }
            } else path = '/' + path;
        }
        if (url == null) {
            if (path.startsWith("/~")) {
                final ServletContext ctx0 = ctx;
                final String path0 = path;
                final int j = path.indexOf('/', 2);
                final String ctxpath;
                if (j >= 0) {
                    ctxpath = "/" + path.substring(2, j);
                    path = path.substring(j);
                } else {
                    ctxpath = "/" + path.substring(2);
                    path = "/";
                }
                final ExtendletContext extctx = Servlets.getExtendletContext(ctx, ctxpath.substring(1));
                if (extctx != null) {
                    url = extctx.getResource(path);
                    if (url == null) return null;
                    return cache.get(new ResourceInfo(path, url, extra));
                }
                ctx = ctx.getContext(ctxpath);
                if (ctx == null) {
                    ctx = ctx0;
                    path = path0;
                }
            }
            final String flnm = ctx.getRealPath(path);
            if (flnm != null) {
                final File file = new File(flnm);
                if (file.exists()) return cache.get(new ResourceInfo(path, file, extra));
            }
        }
        try {
            if (url == null) url = ctx.getResource(path);
            if (url != null) return cache.get(new ResourceInfo(path, url, extra));
        } catch (Throwable ex) {
            log.warning("Unable to load " + path + "\n" + Exceptions.getMessage(ex));
        }
        return null;
    }

    private static final String ATTR_PAGE_CACHE = "org.zkoss.web.util.resource.PageCache";

    /** Returns the content of the specified path, or null if not found.
	 *
	 * <p> The content is returned directly as a string without any parsing.
	 *
	 * <p>Note: the encoding is assumed to be "UTF-8".
	 *
	 * @param path the URI path
	 */
    public static final String getContent(ServletContext ctx, String path) {
        return (String) get(getCache(ctx), ctx, path, null);
    }

    private static final ResourceCache getCache(ServletContext ctx) {
        ResourceCache cache = (ResourceCache) ctx.getAttribute(ATTR_PAGE_CACHE);
        if (cache == null) {
            synchronized (ResourceCaches.class) {
                cache = (ResourceCache) ctx.getAttribute(ATTR_PAGE_CACHE);
                if (cache == null) {
                    cache = new ResourceCache(new ContentLoader(ctx), 29);
                    cache.setMaxSize(1024);
                    cache.setLifetime(60 * 60 * 1000);
                    ctx.setAttribute(ATTR_PAGE_CACHE, cache);
                }
            }
        }
        return cache;
    }

    private static class ContentLoader extends ResourceLoader {

        private final ServletContext _ctx;

        private ContentLoader(ServletContext ctx) {
            _ctx = ctx;
        }

        protected Object parse(String path, File file, Object extra) throws Exception {
            final InputStream is = new BufferedInputStream(new FileInputStream(file));
            try {
                return readAll(is);
            } finally {
                Files.close(is);
            }
        }

        protected Object parse(String path, URL url, Object extra) throws Exception {
            InputStream is = url.openStream();
            if (is != null) is = new BufferedInputStream(is);
            try {
                return readAll(is);
            } finally {
                Files.close(is);
            }
        }

        private String readAll(InputStream is) throws Exception {
            if (is == null) return null;
            return Files.readAll(new InputStreamReader(is, "UTF-8")).toString();
        }
    }
}
