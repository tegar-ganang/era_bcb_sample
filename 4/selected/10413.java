package org.apache.myfaces.trinidadinternal.image.cache;

import java.awt.Color;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.color.CMMException;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.myfaces.trinidad.logging.TrinidadLogger;
import org.apache.myfaces.trinidad.util.ArrayMap;
import org.apache.myfaces.trinidadinternal.agent.TrinidadAgent;
import org.apache.myfaces.trinidadinternal.image.ImageConstants;
import org.apache.myfaces.trinidadinternal.image.ImageContext;
import org.apache.myfaces.trinidadinternal.image.ImageProvider;
import org.apache.myfaces.trinidadinternal.image.ImageProviderRequest;
import org.apache.myfaces.trinidadinternal.image.ImageProviderResponse;
import org.apache.myfaces.trinidadinternal.image.ImageRenderer;
import org.apache.myfaces.trinidadinternal.image.ImageType;
import org.apache.myfaces.trinidadinternal.image.ImageTypeManager;
import org.apache.myfaces.trinidadinternal.image.encode.ImageEncoder;
import org.apache.myfaces.trinidadinternal.image.encode.ImageEncoderManager;
import org.apache.myfaces.trinidadinternal.image.util.FileUtils;
import org.apache.myfaces.trinidadinternal.image.util.MapArea;
import org.apache.myfaces.trinidadinternal.image.xml.ImageProviderRequestUtils;
import org.apache.myfaces.trinidadinternal.share.config.Configuration;
import org.apache.myfaces.trinidadinternal.share.io.InputStreamProvider;
import org.apache.myfaces.trinidadinternal.share.xml.XMLProvider;
import org.apache.myfaces.trinidadinternal.share.xml.XMLUtils;
import org.apache.myfaces.trinidadinternal.style.util.GraphicsUtils;
import org.apache.myfaces.trinidadinternal.util.nls.LocaleUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * FileSystemImageCache is an ImageProvider implementation which caches
 * images on the file system.  Since the FileSystemImageCache
 * are fairly expensive objects to create, FileSystemImageCache instances
 * are shared across applications in the same VM.  Clients can access
 * the shared FileSystemImageCache instance for a particular file system
 * cache location via the getSharedCache method.
 *
 * @see org.apache.myfaces.trinidadinternal.image.ImageProvider
 *
 * @version $Name:  $ ($Revision: adfrt/faces/adf-faces-impl/src/main/java/oracle/adfinternal/view/faces/image/cache/FileSystemImageCache.java#0 $) $Date: 10-nov-2005.19:06:06 $
 */
public class FileSystemImageCache implements ImageProvider, ImageConstants {

    public static final String BLOCK_IMAGE_GENERATION = "org.apache.myfaces.trinidad.image.BlockImageGeneration";

    /**
   * Returns a shared cache instance.
   * @param realPath The real path of the root directory of the cache.  If the
   *  specified path does not exist and can not be created, and
   *  IllegalArgumentException is thrown.
   * @return Returns an ImageProvider instance which can be used to
   *   obtain cached images.
   */
    public static ImageProvider getSharedCache(String realPath) {
        realPath = _getCanonicalPath(realPath);
        ImageProvider cache = _sSharedCaches.get(realPath);
        if (cache == null) {
            cache = new FileSystemImageCache(realPath);
            synchronized (_sSharedCaches) {
                ImageProvider tmp = _sSharedCaches.get(realPath);
                if (tmp != null) {
                    cache = tmp;
                } else {
                    _sSharedCaches.put(realPath, cache);
                }
            }
        }
        return cache;
    }

    /**
   * Creates a FileSystemImageCache.  Clients should use getSharedCache()
   * to obtain FileSystemImageCache instances.
   *
   * @param realPath The real path of the root directory of the cache.  If the
   *  specified path does not exist and can not be created, and
   *  IllegalArgumentException is thrown.
   */
    protected FileSystemImageCache(String realPath) {
        _realPath = realPath;
        File f = new File(_realPath);
        if (!f.exists() && !f.mkdirs()) {
            throw new IllegalArgumentException(_CACHE_DIRECTORY_ERROR + realPath);
        }
        _caches = new ConcurrentHashMap<String, Cache>(19);
    }

    /**
   * Implementation of ImageCache.getImage().
   *
   * @see org.apache.myfaces.trinidadinternal.image.ImageProvider#getImage
   */
    public ImageProviderResponse getImage(ImageContext context, ImageProviderRequest request) {
        try {
            ImageType type = _getImageType(context, request);
            assert (type != null);
            Cache cache = _getCache(context, type);
            assert (cache != null);
            Object key = null;
            if (request instanceof CacheKey) key = request; else key = _getCacheKey(context, type, request);
            assert (key != null);
            CacheEntry entry = _getImageFromCache(context, cache, key, request);
            if (entry != null) {
                if (entry == _MISS_ENTRY) return null;
                if (entry == _RETRY_ENTRY) {
                    if (!_shouldRetry(context)) return null;
                } else {
                    return entry;
                }
            }
            Map<Object, Object> properties = _getFilteredProperties(context, type, request);
            if (properties == null) {
                _putMissEntry(context, type, request, cache, key);
                return null;
            }
            entry = _getImageFromFileSystem(context, type, cache, key, properties);
            if (entry != null) {
                if (entry == _MISS_ENTRY) return null;
                if (entry != _RETRY_ENTRY) return entry;
            }
            return _generateImage(context, type, request, cache, key, properties);
        } catch (CacheException e) {
            _LOG.warning(e);
        }
        return null;
    }

    private void _loadCache(ImageContext context, Cache cache, boolean localized) throws CacheException {
        String localeString = _getLocaleString(context);
        String realPath = _realPath + File.separatorChar;
        if (localized) realPath += (localeString + File.separatorChar);
        File directory = new File(realPath);
        if (!directory.exists() && (!directory.mkdir())) _error(_CACHE_DIRECTORY_ERROR + realPath);
        if (_LOG.isFine()) _LOG.fine("Initializing image cache: " + realPath + " ...");
        String files[] = directory.list(IMXFilter.getInstance());
        if (files == null) return;
        long lastLog = 0;
        for (int i = 0; i < files.length; i++) {
            long current = System.currentTimeMillis();
            if ((current - lastLog) > 5000) {
                if (_LOG.isFine()) _LOG.fine("Loading image " + i + " of " + files.length + " from image cache: " + realPath);
                lastLog = current;
            }
            String imxName = files[i];
            String imxPath = realPath + imxName;
            XMLProvider parser = _getXMLProvider(context);
            assert (parser != null);
            try {
                _loadImage(context, cache, new File(imxPath), parser);
            } catch (CacheException e) {
                _LOG.warning(e);
            }
        }
        if (_LOG.isFine()) _LOG.fine("Finished initializing image cache: " + realPath);
    }

    private Cache _getCache(ImageContext context, ImageType type) throws CacheException {
        boolean localized = _isTypeLocalized(type);
        String language = _getLocaleString(context);
        Cache cache = null;
        if (localized) cache = _caches.get(language); else cache = _globalCache;
        if (cache != null) return cache;
        cache = new Cache();
        if (localized) {
            if (!_caches.containsKey(language)) {
                _caches.put(language, cache);
                _loadCache(context, cache, localized);
            } else {
                cache = _caches.get(language);
            }
        } else {
            if (_globalCache == null) {
                _globalCache = cache;
                _loadCache(context, cache, localized);
            } else {
                cache = _globalCache;
            }
        }
        if (cache == null) {
            String message = _CREATE_CACHE_ERROR + _realPath;
            if (localized) message += "/" + language;
            _error(message);
        }
        return cache;
    }

    private CacheEntry _getImageFromCache(ImageContext context, Cache cache, Object key, ImageProviderRequest request) throws CacheException {
        CacheEntry entry = cache.get(context, key);
        if ((entry == null) || (entry == _MISS_ENTRY) || (entry == _RETRY_ENTRY)) return entry;
        if (_checkModified(context)) {
            long time = System.currentTimeMillis();
            if (time > entry.getLastChecked() + _LAST_CHECK_INTERVAL) {
                if (!_imageExists(entry.getImageURI())) {
                    cache.remove(context, key, entry);
                    return null;
                }
                if (!entry.isValid(context, request)) {
                    cache.remove(context, key, entry);
                    _removeImageFromFileSystem(entry);
                    return null;
                }
                entry.setLastChecked(time);
            }
        }
        return entry;
    }

    private CacheEntry _getImageFromFileSystem(ImageContext context, ImageType type, Cache cache, Object key, Map<Object, Object> properties) throws CacheException {
        String name = _getFileName(context, type, properties);
        assert (name != null);
        XMLProvider parser = null;
        while (true) {
            String uniqueName = cache.getUniqueName(name);
            String path = _getRealPath(context, type, uniqueName, __IMX_EXTENSION);
            File file = new File(path);
            if (!file.exists()) {
                cache.releaseUniqueName(uniqueName);
                return null;
            }
            if (parser == null) {
                parser = _getXMLProvider(context);
                assert (parser != null);
            }
            Object newKey = null;
            try {
                newKey = _loadImage(context, cache, file, parser);
            } catch (CacheException e) {
                _LOG.warning(e);
            }
            if (key.equals(newKey)) return cache.get(context, key);
        }
    }

    private CacheEntry _generateImage(ImageContext context, ImageType type, ImageProviderRequest request, Cache cache, Object key, Map<Object, Object> properties) throws CacheException {
        ArrayMap<Object, Object> responseProperties = new ArrayMap<Object, Object>(3);
        byte[] imageData = null;
        try {
            imageData = _renderImageLocal(context, type, properties, responseProperties);
        } catch (CMMException e) {
            _LOG.warning(e);
        }
        if (imageData == null) {
            _putMissEntry(context, type, request, cache, key);
            return null;
        }
        cache = _checkCacheExists(context, type);
        assert (cache != null);
        CacheEntry entry = _getImageFromCache(context, cache, key, request);
        if (entry == null) entry = _getImageFromFileSystem(context, type, cache, key, properties);
        if (entry != null) {
            if ((entry != _MISS_ENTRY) && (entry != _RETRY_ENTRY)) return entry;
        }
        String name = _getFileName(context, type, properties);
        assert (name != null);
        String uniqueName = cache.getUniqueName(name);
        assert (uniqueName != null);
        File imxFile = _writeImageMetadataFile(context, type, uniqueName, properties, responseProperties);
        if (imxFile == null) return null;
        File imageFile = _writeImageFile(context, type, uniqueName, imageData, properties);
        if (imageFile == null) {
            imxFile.delete();
            return null;
        }
        return _putCachedImage(context, type, request, cache, key, uniqueName, properties, responseProperties);
    }

    private Object _loadImage(ImageContext context, Cache cache, File imxFile, XMLProvider parser) throws CacheException {
        String imxPath = imxFile.getPath();
        String imxName = imxFile.getName();
        InputStream in = null;
        try {
            in = new FileInputStream(imxPath);
        } catch (FileNotFoundException fnfe) {
            _error(_METADATA_FILE_ERROR, fnfe);
        }
        assert (in != null);
        Reader reader = _getUTF8Reader(in);
        InputSource source = new InputSource(reader);
        source.setSystemId(imxPath);
        ImageProviderRequest request = null;
        try {
            request = ImageProviderRequestUtils.createImageProviderRequest(context, parser, source);
        } catch (SAXException e) {
            _error(_XML_DECODING_ERROR + imxPath, e);
        } catch (IOException e) {
            _error(_XML_DECODING_ERROR + imxPath, e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                _error(e);
            }
        }
        if (request == null) return null;
        ImageType type = _getImageType(context, request);
        assert (type != null);
        Map<Object, Object> properties = request.getRenderProperties(context);
        assert (properties != null);
        int dotIndex = imxName.lastIndexOf('.');
        String baseName = (dotIndex == -1) ? imxName : imxName.substring(0, dotIndex);
        StringBuffer uriBuffer = new StringBuffer(imxName.length() + 3);
        if (_isTypeLocalized(type)) {
            uriBuffer.append(_getLocaleString(context));
            uriBuffer.append(_URI_DELIMITER);
        }
        uriBuffer.append(baseName);
        String extension = _getImageEncodingExtension(context, properties);
        assert (extension != null);
        uriBuffer.append(extension);
        String uri = uriBuffer.toString();
        if (!_imageExists(uri)) return null;
        CacheKeyFactory keyFactory = _getCacheKeyFactory(type);
        Object key = keyFactory.getCacheKey(context, properties);
        CacheEntry entry = _createCacheEntry(context, type, uri, properties);
        cache.put(context, key, entry);
        return key;
    }

    private CacheEntry _putCachedImage(ImageContext context, ImageType type, ImageProviderRequest request, Cache cache, Object key, String name, Map<Object, Object> properties, Map<Object, Object> responseProperties) throws CacheException {
        if (request == key) {
            key = _getCacheKey(context, type, request);
            assert (key != null);
        }
        boolean localized = _isTypeLocalized(type);
        String language = _getLocaleString(context);
        assert (language != null);
        String extension = _getImageEncodingExtension(context, properties);
        assert (extension != null);
        int length = name.length() + extension.length();
        if (localized) length += language.length();
        StringBuffer buffer = new StringBuffer(length);
        if (localized) {
            buffer.append(language);
            buffer.append("/");
        }
        buffer.append(name);
        buffer.append(extension);
        String uri = buffer.toString();
        CacheEntry entry = _createCacheEntry(context, type, uri, responseProperties);
        cache.put(context, key, entry);
        return entry;
    }

    private void _putMissEntry(ImageContext context, ImageType type, ImageProviderRequest request, Cache cache, Object key) throws CacheException {
        if (request == key) {
            key = _getCacheKey(context, type, request);
            assert (key != null);
        }
        cache.put(context, key, _MISS_ENTRY);
    }

    private void _removeImageFromFileSystem(CacheEntry entry) {
        String uri = entry.getImageURI();
        String imagePath = _getRealPath(uri);
        File imageFile = new File(imagePath);
        imageFile.delete();
        String encoding = entry.getEncoding();
        ImageEncoderManager manager = ImageEncoderManager.getDefaultImageEncoderManager();
        String extension = manager.getImageExtension(encoding);
        String imxPath = imagePath.substring(0, imagePath.length() - extension.length());
        File imxFile = new File(imxPath + __IMX_EXTENSION);
        imxFile.delete();
    }

    private byte[] _renderImageLocal(ImageContext context, ImageType type, Map<Object, Object> properties, Map<Object, Object> responseProperties) throws CacheException {
        Configuration config = context.getConfiguration();
        if (Boolean.TRUE.equals(config.getProperty(BLOCK_IMAGE_GENERATION))) {
            responseProperties.put(WIDTH_RESPONSE_KEY, _TEST_WIDTH);
            responseProperties.put(HEIGHT_RESPONSE_KEY, _TEST_HEIGHT);
            Object o = properties.get(TABS_KEY);
            if (o != null) {
                int length = ((Object[]) o).length;
                MapArea[] areas = new MapArea[length];
                for (int i = 0; i < length; i++) {
                    areas[i] = new MapArea(new Rectangle(i, 0, 1, 1));
                }
                responseProperties.put(IMAGE_MAP_AREAS_RESPONSE_KEY, areas);
            }
            return new byte[0];
        }
        if (Boolean.TRUE.equals(config.getProperty(Configuration.HEADLESS)) || !GraphicsUtils.isGraphicalEnvironment()) {
            if (TECATE_NAMESPACE.equals(type.getNamespaceURI()) && COLORIZED_ICON_NAME.equals(type.getLocalName())) {
                return _readColorizedIconData(context, properties, responseProperties);
            }
            return null;
        }
        ImageRenderer renderer = _getImageRenderer(type);
        assert (renderer != null);
        Image image = renderer.renderImage(context, properties, responseProperties);
        if (image == null) return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageEncoder encoder = _getImageEncoder(context, properties);
            assert (encoder != null);
            encoder.encodeImage(image, out);
        } catch (IOException e) {
            _error(e);
        } finally {
            image.flush();
        }
        return out.toByteArray();
    }

    private byte[] _readImageData(InputStream in) throws IOException, CacheException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = (in.read(buffer))) >= 0) out.write(buffer, 0, length);
        return out.toByteArray();
    }

    private byte[] _readSourceIconData(InputStreamProvider provider) throws IOException, CacheException {
        InputStream in = null;
        try {
            in = provider.openInputStream();
        } catch (IOException e) {
            return null;
        }
        byte[] iconData = null;
        try {
            iconData = _readImageData(in);
        } finally {
            in.close();
        }
        return iconData;
    }

    private boolean _shouldRetry(ImageContext context) {
        return true;
    }

    private File _writeImageFile(ImageContext context, ImageType type, String name, byte[] data, Map<Object, Object> properties) throws CacheException {
        String extension = _getImageEncodingExtension(context, properties);
        String path = _getRealPath(context, type, name, extension);
        File file = new File(path);
        try {
            OutputStream out = new FileOutputStream(file);
            out.write(data);
            out.close();
        } catch (IOException e) {
            _error(e);
        }
        return file;
    }

    private File _writeImageMetadataFile(ImageContext context, ImageType type, String name, Map<Object, Object> properties, Map<Object, Object> responseProperties) throws CacheException {
        String path = _getRealPath(context, type, name, __IMX_EXTENSION);
        File file = new File(path);
        PrintWriter writer = null;
        try {
            if (file.exists() || !file.createNewFile()) {
                return null;
            }
            writer = new PrintWriter(FileUtils.getUTF8Writer(path));
        } catch (IOException e) {
            _error(e);
        }
        try {
            _writeImageProviderRequest(context, type, properties, responseProperties, writer);
        } finally {
            writer.flush();
            writer.close();
            if (writer.checkError()) {
                if (file.exists()) file.delete();
                _error(_XML_ENCODING_ERROR + path);
            }
        }
        return file;
    }

    private void _writeImageProviderRequest(ImageContext context, ImageType type, Map<Object, Object> properties, Map<Object, Object> responseProperties, PrintWriter writer) throws CacheException {
        writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        try {
            ImageProviderRequestUtils.encodeImageProviderRequest(context, type.getNamespaceURI(), type.getLocalName(), type, properties, responseProperties, writer);
        } catch (IllegalArgumentException e) {
            throw new CacheException(e);
        }
    }

    private Cache _checkCacheExists(ImageContext context, ImageType type) throws CacheException {
        boolean localized = _isTypeLocalized(type);
        String language = _getLocaleString(context);
        String directoryPath = _getRealPath(localized ? language : "");
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            if (localized) {
                synchronized (_caches) {
                    if (_caches.containsKey(language)) {
                        _caches.remove(language);
                    }
                    File parent = directory.getParentFile();
                    if ((parent != null) && (!parent.exists())) {
                        synchronized (this) {
                            _globalCache = null;
                        }
                    }
                }
            } else {
                synchronized (this) {
                    _globalCache = null;
                }
            }
            directory.mkdirs();
        }
        return _getCache(context, type);
    }

    private boolean _checkModified(ImageContext context) {
        if (_configNotChecked) {
            _checkModified = true;
            _configNotChecked = false;
        }
        return _checkModified;
    }

    private CacheEntry _createCacheEntry(ImageContext context, ImageType type, String uri, Map<Object, Object> properties) {
        int width = _getIntSize(properties, WIDTH_RESPONSE_KEY);
        int height = _getIntSize(properties, HEIGHT_RESPONSE_KEY);
        MapArea[] areas = (MapArea[]) properties.get(IMAGE_MAP_AREAS_RESPONSE_KEY);
        String encoding = _getImageEncoding(context, properties);
        if (areas == null) {
            Object checkSource = type.getProperty(ImageType.CHECK_SOURCE_PROPERTY);
            if (Boolean.TRUE.equals(checkSource)) return new SourceCheckingCacheEntry(uri, width, height, encoding);
            return new CacheEntry(uri, width, height, encoding);
        }
        return new MapCacheEntry(uri, width, height, areas, encoding);
    }

    private void _error(String message) throws CacheException {
        throw new CacheException(message);
    }

    private void _error(Throwable t) throws CacheException {
        throw new CacheException(t);
    }

    private void _error(String message, Throwable t) throws CacheException {
        throw new CacheException(message, t);
    }

    private Object _getCacheKey(ImageContext context, ImageType type, ImageProviderRequest request) throws CacheException {
        CacheKeyFactory keyFactory = _getCacheKeyFactory(type);
        assert (keyFactory != null);
        Object key = keyFactory.getCacheKey(context, request.getRenderProperties(context));
        if (key == null) _error(_CACHE_KEY_ERROR + type);
        return key;
    }

    private CacheKeyFactory _getCacheKeyFactory(ImageType type) throws CacheException {
        CacheKeyFactory keyFactory = (CacheKeyFactory) type.getProperty(CacheKeyFactory.CACHE_KEY_FACTORY_PROPERTY);
        if (keyFactory == null) _error(_CACHE_KEY_FACTORY_ERROR + type);
        return keyFactory;
    }

    private static String _getCanonicalPath(String path) {
        String canonicalPath = _sCanonicalPaths.get(path);
        if (canonicalPath != null) return canonicalPath;
        File file = new File(path);
        try {
            canonicalPath = file.getCanonicalPath();
        } catch (IOException e) {
            throw new IllegalArgumentException(_CACHE_PATH_ERROR + path);
        }
        if (canonicalPath != null) _sCanonicalPaths.put(path, canonicalPath);
        return canonicalPath;
    }

    private String _getFileName(ImageContext context, ImageType type, Map<Object, Object> properties) throws CacheException {
        NameProvider nameProvider = (NameProvider) type.getProperty(NameProvider.NAME_PROVIDER_PROPERTY);
        if (nameProvider == null) _error(_NAME_PROVIDER_ERROR + type);
        String name = nameProvider.getName(context, properties);
        if (name == null) _error(_NAME_PROVIDING_ERROR + type);
        return name;
    }

    private Map<Object, Object> _getFilteredProperties(ImageContext context, ImageType type, ImageProviderRequest request) {
        Map<Object, Object> properties = request.getRenderProperties(context);
        if (properties == null) return null;
        if (properties.get(ENCODING_TYPE_KEY) == null) {
            String encoding = _getImageEncoding(context, properties);
            properties.put(ENCODING_TYPE_KEY, encoding);
        }
        PropertiesFilter filter = (PropertiesFilter) type.getProperty(PropertiesFilter.PROPERTIES_FILTER_PROPERTY);
        if (filter != null) return filter.filterProperties(context, properties);
        return properties;
    }

    private ImageEncoder _getImageEncoder(ImageContext context, Map<Object, Object> properties) throws CacheException {
        String encoding = _getImageEncoding(context, properties);
        assert (encoding != null);
        ImageEncoderManager manager = ImageEncoderManager.getDefaultImageEncoderManager();
        ImageEncoder encoder = manager.getImageEncoder(encoding);
        if (encoder == null) _error(_IMAGE_ENCODER_ERROR + encoding);
        return encoder;
    }

    private String _getImageEncoding(ImageContext context, Map<Object, Object> properties) {
        String encoding = (String) properties.get(ENCODING_TYPE_KEY);
        if (encoding != null) return encoding;
        Configuration config = context.getConfiguration();
        TrinidadAgent agent = context.getAgent();
        if ((agent.getCapability(TrinidadAgent.CAP_GIF_TYPE_IMAGE) == Boolean.TRUE) && !Boolean.FALSE.equals(config.getProperty(_GIF_ENABLED))) {
            return ImageEncoderManager.GIF_TYPE;
        }
        if (agent.getCapability(TrinidadAgent.CAP_PNG_TYPE_IMAGE) == Boolean.TRUE) {
            return ImageEncoderManager.PNG_TYPE;
        }
        assert false;
        return ImageEncoderManager.PNG_TYPE;
    }

    private String _getImageEncodingExtension(ImageContext context, Map<Object, Object> properties) throws CacheException {
        String encoding = _getImageEncoding(context, properties);
        assert (encoding != null);
        ImageEncoderManager manager = ImageEncoderManager.getDefaultImageEncoderManager();
        String extension = manager.getImageExtension(encoding);
        if (extension == null) _error(_IMAGE_ENCODING_EXTENSION_ERROR + encoding);
        return extension;
    }

    private ImageRenderer _getImageRenderer(ImageType type) throws CacheException {
        ImageRenderer renderer = (ImageRenderer) type.getProperty(ImageType.IMAGE_RENDERER_PROPERTY);
        if (renderer == null) _error(_IMAGE_RENDERER_ERROR + type);
        return renderer;
    }

    private ImageType _getImageType(ImageContext context, ImageProviderRequest request) throws CacheException {
        String namespace = request.getNamespaceURI();
        String name = request.getLocalName();
        ImageTypeManager manager = CacheUtils.getImageTypeManager(context);
        assert (manager != null);
        ImageType type = manager.getImageType(namespace, name);
        if (type == null) _error(_IMAGE_TYPE_ERROR + namespace + ", " + name);
        return type;
    }

    private int _getIntSize(Map<Object, Object> properties, Object key) {
        Integer value = (Integer) properties.get(key);
        if (value == null) return ImageProviderResponse.UNKNOWN_SIZE;
        return value.intValue();
    }

    private String _getLocaleString(ImageContext context) {
        Locale locale = context.getLocaleContext().getTranslationLocale();
        String language = locale.getLanguage();
        if (!_CHINESE_LANGUAGE.equals(language)) return language;
        return _isSimplifiedChinese(locale.getCountry()) ? _SIMPLIFIED_CHINESE_DIRECTORY : _TRADITIONAL_CHINESE_DIRECTORY;
    }

    private String _getRealPath(String uri) {
        int length = _realPath.length() + uri.length() + 1;
        StringBuffer buffer = new StringBuffer(length);
        buffer.append(_realPath);
        buffer.append(File.separatorChar);
        buffer.append(uri);
        return buffer.toString();
    }

    private String _getRealPath(ImageContext context, ImageType type, String name, String extension) {
        boolean localized = _isTypeLocalized(type);
        String language = _getLocaleString(context);
        assert (language != null);
        int length = _realPath.length() + name.length() + 1;
        if (localized) length += (language.length() + 1);
        if (extension != null) length += extension.length();
        StringBuffer buffer = new StringBuffer(length + 1);
        buffer.append(_realPath);
        buffer.append(File.separatorChar);
        if (localized) {
            buffer.append(language);
            buffer.append(File.separatorChar);
        }
        buffer.append(name);
        if (extension != null) buffer.append(extension);
        return buffer.toString();
    }

    private Reader _getUTF8Reader(InputStream in) throws CacheException {
        Reader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(in, _UTF8));
        } catch (UnsupportedEncodingException e) {
            _error(e);
        }
        assert (reader != null);
        return reader;
    }

    private XMLProvider _getXMLProvider(ImageContext context) throws CacheException {
        Configuration config = context.getConfiguration();
        XMLProvider parser = XMLUtils.getXMLProvider(config);
        if (parser == null) _error(_XML_PROVIDER_ERROR);
        return parser;
    }

    private boolean _imageExists(String uri) {
        String path = _getRealPath(uri);
        File file = new File(path);
        return file.exists();
    }

    private static boolean _isSimplifiedChinese(String region) {
        return ("CN".equals(region));
    }

    private boolean _isTypeLocalized(ImageType type) {
        Object localized = type.getProperty(ImageType.LOCALIZED_PROPERTY);
        return !Boolean.FALSE.equals(localized);
    }

    private byte[] _readColorizedIconData(ImageContext context, Map<Object, Object> properties, Map<Object, Object> responseProperties) throws CacheException {
        Object encoding = _getImageEncoding(context, properties);
        if (!ImageEncoderManager.GIF_TYPE.equals(encoding)) return null;
        if (CacheUtils.getReadingDirection(context, properties) != LocaleUtils.DIRECTION_LEFTTORIGHT) {
            return null;
        }
        Color darkColor = (Color) properties.get(DARK_COLOR_KEY);
        if ((darkColor != null) && ((darkColor.getRGB() & 0x00ffffff) != 0x00336699)) {
            return null;
        }
        Color darkAccentColor = (Color) properties.get(DARK_ACCENT_COLOR_KEY);
        if ((darkAccentColor != null) && ((darkAccentColor.getRGB() & 0x00ffffff) != 0x00d2d8b0)) {
            return null;
        }
        InputStreamProvider provider = (InputStreamProvider) properties.get(SOURCE_INPUT_STREAM_PROVIDER_KEY);
        if (provider != null) {
            byte[] data = null;
            ;
            try {
                data = _readSourceIconData(provider);
            } catch (IOException e) {
                _error(e);
            }
            if (data != null) {
                if (data[0] == 'G' && (data[1] == 'I') && (data[2] == 'F')) {
                    int width = (data[6] | (data[7] << 8));
                    int height = (data[8] | (data[9] << 8));
                    responseProperties.put(WIDTH_RESPONSE_KEY, width);
                    responseProperties.put(HEIGHT_RESPONSE_KEY, height);
                    return data;
                }
            }
        }
        return null;
    }

    private static class IMXFilter implements FilenameFilter {

        private IMXFilter() {
        }

        public static FilenameFilter getInstance() {
            if (_sInstance == null) _sInstance = new IMXFilter();
            return _sInstance;
        }

        public boolean accept(File dir, String name) {
            return name.endsWith(__IMX_EXTENSION);
        }

        private static IMXFilter _sInstance;
    }

    private String _realPath;

    private ConcurrentHashMap<String, Cache> _caches;

    private Cache _globalCache;

    private static final Hashtable<String, ImageProvider> _sSharedCaches = new Hashtable<String, ImageProvider>(19);

    static final String __IMX_EXTENSION = ".imx";

    private static final String _URI_DELIMITER = "/";

    private static final long _LAST_CHECK_INTERVAL = 10000;

    private boolean _configNotChecked = true;

    private boolean _checkModified = true;

    private static final CacheEntry _MISS_ENTRY = new CacheEntry(null, -1, -1);

    private static final CacheEntry _RETRY_ENTRY = new CacheEntry(null, -1, -1);

    private static final String _CACHE_PATH_ERROR = "Could not get canonical path for image cache directory ";

    private static final String _CACHE_DIRECTORY_ERROR = "Could not create image cache directory ";

    private static final String _XML_ENCODING_ERROR = "Error while generating image metadata file ";

    private static final String _CREATE_CACHE_ERROR = "Could not create image cache for ";

    private static final String _XML_DECODING_ERROR = "Could not decode image metadata from ";

    private static final String _CACHE_KEY_ERROR = "Could not create cache key for image type ";

    private static final String _CACHE_KEY_FACTORY_ERROR = "No CacheKeyFactory registered for image type ";

    private static final String _METADATA_FILE_ERROR = "Could not locate metadata file ";

    private static final String _IMAGE_TYPE_ERROR = "No image type registered for ";

    private static final String _IMAGE_RENDERER_ERROR = "No ImageRenderer registered for image type ";

    private static final String _NAME_PROVIDER_ERROR = "No NameProvider registered for image type ";

    private static final String _NAME_PROVIDING_ERROR = "Could not get image file name name for image type ";

    private static final String _IMAGE_ENCODER_ERROR = "No ImageEncoder registered for image encoding type ";

    private static final String _IMAGE_ENCODING_EXTENSION_ERROR = "No file extension registered for image encoding type ";

    private static final String _XML_PROVIDER_ERROR = "Could not load XMLProvider";

    private static String _UTF8 = "UTF8";

    private static final String _CHINESE_LANGUAGE = "zh";

    private static final String _SIMPLIFIED_CHINESE_DIRECTORY = "zhs";

    private static final String _TRADITIONAL_CHINESE_DIRECTORY = "zht";

    private static final Integer _TEST_WIDTH = Integer.valueOf(23);

    private static final Integer _TEST_HEIGHT = Integer.valueOf(32);

    private static final String _GIF_ENABLED = "gifEnabled";

    private static final Hashtable<String, String> _sCanonicalPaths = new Hashtable<String, String>(19);

    private static final TrinidadLogger _LOG = TrinidadLogger.createTrinidadLogger(FileSystemImageCache.class);
}
