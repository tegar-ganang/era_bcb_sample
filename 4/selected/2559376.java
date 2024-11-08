package com.faunos.util.net.http.file;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import com.faunos.util.Measure;
import com.faunos.util.io.file.ExtensionMimeMap;
import com.faunos.util.io.file.MimeMap;
import com.faunos.util.net.Stagelet;
import com.faunos.util.net.StageletStack;
import com.faunos.util.net.Validators;
import com.faunos.util.net.http.AbstractResponseFactory;
import com.faunos.util.net.http.Caroon;
import com.faunos.util.net.http.FileChannelStagelet;
import com.faunos.util.net.http.RequestHeader;
import com.faunos.util.net.http.ResponseFactory;
import com.faunos.util.net.http.ResponseFactoryConfig;
import com.faunos.util.net.http.Verb;

/**
 * A <tt>ResponseFactory</tt> for serving ordinary files.
 *
 * @author Babak Farhang
 */
public class FileResponseFactory extends AbstractResponseFactory implements Closeable {

    /**
     * Default mime map used to set Content-Type HTTP header. This is
     * file-extension-based.
     * 
     * @see ExtensionMimeMap
     */
    protected static final MimeMap DEFAULT_MIME_MAP = ExtensionMimeMap.newDefaultInstance();

    /**
     * Returns the instance configured at the specified <tt>uriMountPoint</tt>,
     * if any.  If some other type of {@linkplain ResponseFactory
     * ResponseFactory} is configured at that <tt>uriMountPoint</tt>, then an
     * exception is thrown.
     * 
     * @param config
     *        the configuration object, e.g. one exposed by the container
     *        ({@link Caroon Caroon}).
     * @param uriMountPoint
     *        the URI mount point. This is the root of HTTP URIs that the
     *        factory responds to.
     * 
     * @see #close()
     * @see #newInstance(ResponseFactoryConfig, String, File)
     * @see #newInstance(ResponseFactoryConfig, String, File, MimeMap, FileFilter)
     */
    public static FileResponseFactory getInstance(ResponseFactoryConfig config, String uriMountPoint) {
        ResponseFactory factory = config.getMapping(uriMountPoint);
        FileResponseFactory fileResponse = null;
        if (factory != null) {
            if (factory instanceof FileResponseFactory) fileResponse = (FileResponseFactory) factory; else Validators.ARG.fail("wrong factory type configured at " + uriMountPoint + " : " + factory.getClass().getName());
        }
        return fileResponse;
    }

    /**
     * Creates a new instance configured at the specified URI mount point, with
     * the given <tt>root</tt> directory. The Content-Type HTTP header is
     * determined using file-extension heuristics.
     * 
     * @param config
     *        the configuration object, e.g. one exposed by the container
     *        ({@link Caroon Caroon}).
     * @param uriMountPoint
     *        the URI mount point. This is the root of HTTP URIs that this
     *        factory will respond to. It must begin and end in a forward
     *        slash ('/')/
     * @param root
     *        the root directory that <tt>uriMountPoint</tt> will resolve to
     * 
     * @see #getInstance(ResponseFactoryConfig, String)
     * @see #close()
     */
    public static FileResponseFactory newInstance(ResponseFactoryConfig config, String uriMountPoint, File root) {
        return newInstance(config, uriMountPoint, root, DEFAULT_MIME_MAP, null);
    }

    /**
     * Creates a new instance configured at the specified URI mount point, with
     * the given <tt>root</tt> directory.
     * 
     * @param config
     *        the configuration object, e.g. one exposed by the container
     *        ({@link Caroon Caroon}).
     * @param uriMountPoint
     *        the URI mount point. This is the root of HTTP URIs that this
     *        factory will respond to. It must begin and end in a forward
     *        slash ('/')/
     * @param root
     *        the root directory that <tt>uriMountPoint</tt> will resolve to
     * @param mimeMap
     *        the mapping instance used to determine what the HTTP Content-Type
     *        header will be set to
     * @param filter
     *        an option file filter. May be <tt>null</tt>
     * 
     * @see #getInstance(ResponseFactoryConfig, String)
     * @see #close()
     */
    public static FileResponseFactory newInstance(ResponseFactoryConfig config, String uriMountPoint, File root, MimeMap mimeMap, FileFilter filter) {
        FileResponseFactory response = new FileResponseFactory(uriMountPoint, root, mimeMap, filter, config);
        config.addMapping(uriMountPoint, response);
        return response;
    }

    /**
     * A no-op directory-file defaulter.
     */
    public static final DirectoryFileDefaulter NOOP_DEFAULTER = new DirectoryFileDefaulter() {

        public File getDefaultChild(File directory) {
            return null;
        }
    };

    protected final String uriMountPoint;

    protected final File root;

    protected final FileFilter filter;

    /**
     * Used to set the "Content-Type" HTTP header.
     */
    protected final MimeMap mimeMap;

    private ResponseFactoryConfig config;

    private volatile DirectoryFileDefaulter dirFileDefaulter = NOOP_DEFAULTER;

    protected FileResponseFactory(String uriMountPoint, File root, MimeMap mimeMap, FileFilter filter, ResponseFactoryConfig config) {
        if (!Measure.isFilepath(uriMountPoint)) Validators.ARG.fail("malformed uriMountPoint: " + uriMountPoint);
        if (uriMountPoint.charAt(0) != '/') Validators.ARG.fail("uriMountPoint must be an absolute path: " + uriMountPoint);
        if (uriMountPoint.charAt(uriMountPoint.length() - 1) != '/') Validators.ARG.fail("uriMountPoint must end in slash ('/'): " + uriMountPoint);
        if (!root.isDirectory()) Validators.ARG.fail("root is not a directory: " + root);
        Validators.ARG.notNull(mimeMap, "null MimeMap");
        this.uriMountPoint = uriMountPoint;
        this.root = root.getAbsoluteFile();
        this.mimeMap = mimeMap;
        this.filter = filter;
        this.config = config;
    }

    /**
     * Sets the file defaulter, for directory response resolution.
     */
    public void setDirectroyFileDefaulter(DirectoryFileDefaulter def) {
        Validators.ARG.notNull(def);
        this.dirFileDefaulter = def;
    }

    /**
     * Removes this instance from its associated configuration.
     * 
     * @see ResponseFactoryConfig#removeMapping(String)
     */
    public synchronized void close() {
        if (config == null) return;
        ResponseFactoryConfig c = config;
        config = null;
        Object factory = c.getMapping(uriMountPoint);
        if (factory == this) {
            factory = c.removeMapping(uriMountPoint);
            if (factory != this) Validators.STATE.fail("concurrent modification caused config corruption" + ": expected to remove " + this + "; actual was " + factory);
        }
    }

    @Override
    protected Stagelet prepareResponseImpl(RequestHeader request, StageletStack stack) {
        assert request.getUri().startsWith(uriMountPoint);
        String uri = request.getUri();
        uri = uri.substring(uriMountPoint.length());
        File file;
        switch(uri.length()) {
            case 0:
                file = root;
                break;
            case 1:
                if (uri.charAt(0) == '/') {
                    file = root;
                    break;
                }
            default:
                if (uri.charAt(0) == '/') uri = uri.substring(1);
                if (File.separatorChar != '/') uri = uri.replace('/', File.separatorChar);
                file = new File(root, uri);
        }
        if (!file.getPath().startsWith(root.getPath()) || !file.exists()) return notFound(request, stack);
        if (filter != null && !filter.accept(file)) return notFound(request, stack);
        FileChannelStagelet stage = null;
        try {
            if (file.isDirectory()) return prepareDirectoryResponse(request, stack, file);
            return createFileStagelet(file, request, stack);
        } catch (IOException iox) {
            Validators.GENERIC.fail(iox);
        }
        return stage;
    }

    /**
     * Returns the response when the request resolves to a file system
     * directory.
     */
    protected Stagelet prepareDirectoryResponse(RequestHeader request, StageletStack stack, File dir) throws IOException {
        File file = dirFileDefaulter.getDefaultChild(dir);
        if (file == null || !file.exists()) return notFound(request, stack);
        return createFileStagelet(file, request, stack);
    }

    private FileChannelStagelet createFileStagelet(File file, RequestHeader request, StageletStack stack) throws IOException {
        String contentType = mimeMap.getMimeType(file);
        FileChannelStagelet stage = new FileChannelStagelet(new FileInputStream(file).getChannel(), stack, request.isPersistentConnection(), request.getVerb() == Verb.HEAD);
        if (contentType == null) stage.getHeaders().setContentTypeAppOctet(); else stage.getHeaders().setContentType(contentType);
        return stage;
    }
}
