package gov.lanl.adore.djatoka.openurl;

import gov.lanl.adore.djatoka.DjatokaDecodeParam;
import gov.lanl.adore.djatoka.DjatokaException;
import gov.lanl.adore.djatoka.DjatokaExtractProcessor;
import gov.lanl.adore.djatoka.io.FormatConstants;
import gov.lanl.adore.djatoka.kdu.KduExtractExe;
import gov.lanl.adore.djatoka.plugin.ITransformPlugIn;
import gov.lanl.adore.djatoka.util.IOUtils;
import gov.lanl.adore.djatoka.util.ImageRecord;
import gov.lanl.util.ConcurrentEvictionFileDelete;
import gov.lanl.util.ConcurrentLinkedHashMap;
import gov.lanl.util.HttpDate;
import info.openurl.oom.ContextObject;
import info.openurl.oom.OpenURLRequest;
import info.openurl.oom.OpenURLRequestProcessor;
import info.openurl.oom.OpenURLResponse;
import info.openurl.oom.Service;
import info.openurl.oom.config.ClassConfig;
import info.openurl.oom.config.OpenURLConfig;
import info.openurl.oom.entities.ServiceType;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Properties;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.oclc.oomRef.descriptors.ByValueMetadataImpl;

/**
 * The OpenURLJP2KService OpenURL Service
 * 
 * @author Ryan Chute
 */
public class OpenURLJP2KService implements Service, FormatConstants {

    static Logger logger = Logger.getLogger(OpenURLJP2KService.class);

    private static final String DEFAULT_IMPL_CLASS = SimpleListResolver.class.getCanonicalName();

    private static final String PROPS_REQUESTER = "requester";

    private static final String PROPS_REFERRING_ENTITY = "referringEntity";

    private static final String PROPS_KEY_IMPL_CLASS = "OpenURLJP2KService.referentResolverImpl";

    private static final String PROPS_KEY_CACHE_ENABLED = "OpenURLJP2KService.cacheEnabled";

    private static final String PROPS_KEY_CACHE_TMPDIR = "OpenURLJP2KService.cacheTmpDir";

    private static final String PROPS_KEY_TRANSFORM = "OpenURLJP2KService.transformPlugin";

    private static final String PROPS_KEY_CACHE_SIZE = "OpenURLJP2KService.cacheSize";

    private static final String PROP_KEY_CACHE_MAX_PIXELS = "OpenURLJP2KService.cacheImageMaxPixels";

    private static final String SVC_ID = "info:lanl-repo/svc/getRegion";

    private static final String DEFAULT_CACHE_SIZE = "1000";

    private static final int DEFAULT_CACHE_MAXPIXELS = 100000;

    private static String implClass = null;

    private static Properties props = new Properties();

    private static boolean init = false;

    private static boolean cacheTiles = true;

    private static boolean transformCheck = false;

    private static ITransformPlugIn transform;

    private static String cacheDir = null;

    private static ConcurrentLinkedHashMap<String, String> tileCache;

    private static DjatokaExtractProcessor extractor;

    private static int maxPixels = DEFAULT_CACHE_MAXPIXELS;

    /**
	 * Construct an info:lanl-repo/svc/getRegion web service class. Initializes 
	 * Referent Resolver instance using OpenURLJP2KService.referentResolverImpl property.
	 * 
	 * @param openURLConfig OOM Properties forwarded from OpenURLServlet
	 * @param classConfig Implementation Properties forwarded from OpenURLServlet
	 * @throws ResolverException 
	 */
    public OpenURLJP2KService(OpenURLConfig openURLConfig, ClassConfig classConfig) throws ResolverException {
        try {
            if (!init) {
                props = IOUtils.loadConfigByCP(classConfig.getArg("props"));
                if (!ReferentManager.isInit()) {
                    implClass = props.getProperty(PROPS_KEY_IMPL_CLASS, DEFAULT_IMPL_CLASS);
                    ReferentManager.init((IReferentResolver) Class.forName(implClass).newInstance(), props);
                }
                cacheDir = props.getProperty(PROPS_KEY_CACHE_TMPDIR);
                if (props.getProperty(PROPS_KEY_CACHE_ENABLED) != null) cacheTiles = Boolean.parseBoolean(props.getProperty(PROPS_KEY_CACHE_ENABLED));
                if (cacheTiles) {
                    int cacheSize = Integer.parseInt(props.getProperty(PROPS_KEY_CACHE_SIZE, DEFAULT_CACHE_SIZE));
                    tileCache = ConcurrentLinkedHashMap.create(ConcurrentLinkedHashMap.EvictionPolicy.SECOND_CHANCE, cacheSize, new ConcurrentEvictionFileDelete());
                }
                if (props.getProperty(PROPS_KEY_TRANSFORM) != null) {
                    transformCheck = true;
                    String transClass = props.getProperty(PROPS_KEY_TRANSFORM);
                    transform = (ITransformPlugIn) Class.forName(transClass).newInstance();
                    transform.setup(props);
                }
                if (props.getProperty(PROP_KEY_CACHE_MAX_PIXELS) != null) maxPixels = Integer.parseInt(props.getProperty(PROP_KEY_CACHE_MAX_PIXELS));
                extractor = new DjatokaExtractProcessor(new KduExtractExe());
                init = true;
            }
        } catch (IOException e) {
            logger.error(e, e);
            throw new ResolverException("Error attempting to open props file from classpath, disabling " + SVC_ID + " : " + e.getMessage());
        } catch (Exception e) {
            logger.error(e, e);
            throw new ResolverException("Unable to inititalize implementation: " + props.getProperty(implClass) + " - " + e.getMessage());
        }
    }

    /**
	 * Returns the OpenURL service identifier for this implementation of
	 * info.openurl.oom.Service
	 */
    public URI getServiceID() throws URISyntaxException {
        return new URI(SVC_ID);
    }

    /**
	 * Returns the OpenURLResponse consisting of an image bitstream to be
	 * rendered on the client. Having obtained a result, this method is then
	 * responsible for transforming it into an OpenURLResponse that acts as a
	 * proxy for HttpServletResponse.
	 */
    public OpenURLResponse resolve(ServiceType serviceType, ContextObject contextObject, OpenURLRequest openURLRequest, OpenURLRequestProcessor processor) {
        String responseFormat = null;
        String format = "image/jpeg";
        int status = HttpServletResponse.SC_OK;
        HashMap<String, String> kev = setServiceValues(contextObject);
        DjatokaDecodeParam params = new DjatokaDecodeParam();
        if (kev.containsKey("region")) params.setRegion(kev.get("region"));
        if (kev.containsKey("format")) {
            format = kev.get("format");
            if (!format.startsWith("image")) {
                format = "image/jpeg";
            }
        }
        if (kev.containsKey("level")) params.setLevel(Integer.parseInt(kev.get("level")));
        if (kev.containsKey("rotate")) params.setRotationDegree(Integer.parseInt(kev.get("rotate")));
        if (kev.containsKey("scale")) {
            String[] v = kev.get("scale").split(",");
            if (v.length == 1) {
                if (v[0].contains(".")) params.setScalingFactor(Double.parseDouble(v[0])); else {
                    int[] dims = new int[] { -1, Integer.parseInt(v[0]) };
                    params.setScalingDimensions(dims);
                }
            } else if (v.length == 2) {
                int[] dims = new int[] { Integer.parseInt(v[0]), Integer.parseInt(v[1]) };
                params.setScalingDimensions(dims);
            }
        }
        if (kev.containsKey("clayer") && kev.get("clayer") != null) {
            int clayer = Integer.parseInt(kev.get("clayer"));
            if (clayer > 0) params.setCompositingLayer(clayer);
        }
        responseFormat = format;
        byte[] bytes = null;
        if (responseFormat == null) {
            try {
                bytes = ("Output Format Not Supported").getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            responseFormat = "text/plain";
            status = HttpServletResponse.SC_NOT_FOUND;
        } else if (params.getRegion() != null && params.getRegion().contains("-")) {
            try {
                bytes = ("Negative Region Arguments are not supported.").getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            responseFormat = "text/plain";
            status = HttpServletResponse.SC_NOT_FOUND;
        } else {
            try {
                ImageRecord r = ReferentManager.getImageRecord(contextObject.getReferent());
                if (r != null) {
                    if (transformCheck && transform != null) {
                        HashMap<String, String> instProps = new HashMap<String, String>();
                        if (r.getInstProps() != null) instProps.putAll(r.getInstProps());
                        if (contextObject.getRequesters().length > 0 && contextObject.getRequesters()[0].getDescriptors().length > 0) {
                            String requester = contextObject.getRequesters()[0].getDescriptors()[0].toString();
                            instProps.put(PROPS_REQUESTER, requester);
                        }
                        if (contextObject.getReferringEntities().length > 0 && contextObject.getReferringEntities()[0].getDescriptors().length > 0) instProps.put(PROPS_REFERRING_ENTITY, contextObject.getReferringEntities()[0].getDescriptors()[0].toString());
                        if (instProps.size() > 0) transform.setInstanceProps(instProps);
                        params.setTransform(transform);
                    }
                    if (!cacheTiles || !isCacheable(params)) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        extractor.extractImage(r.getImageFile(), baos, params, format);
                        bytes = baos.toByteArray();
                        baos.close();
                    } else {
                        String ext = getExtension(format);
                        String hash = getTileHash(r, params);
                        String file = tileCache.get(hash + ext);
                        File f;
                        if (file == null || (file != null && !(f = new File(file)).exists() && f.length() > 0)) {
                            if (cacheDir != null) f = File.createTempFile("cache" + hash.hashCode() + "-", "." + ext, new File(cacheDir)); else f = File.createTempFile("cache" + hash.hashCode() + "-", "." + ext);
                            f.deleteOnExit();
                            file = f.getAbsolutePath();
                            extractor.extractImage(r.getImageFile(), file, params, format);
                            boolean cacheTile = false;
                            synchronized (tileCache) {
                                String tilePath = tileCache.get(hash + ext);
                                if (tilePath == null) {
                                    tileCache.put(hash + ext, file);
                                    cacheTile = true;
                                } else {
                                    cacheTile = false;
                                }
                            }
                            bytes = IOUtils.getBytesFromFile(f);
                            if (cacheTile) logger.debug("makingTile: " + file + " " + bytes.length + " params: " + params); else {
                                f.delete();
                                logger.debug("tempTile: " + file + " " + bytes.length + " params: " + params);
                            }
                        } else {
                            bytes = IOUtils.getBytesFromFile(new File(file));
                            logger.debug("tileCache: " + file + " " + bytes.length);
                        }
                    }
                }
            } catch (ResolverException e) {
                logger.error(e, e);
                bytes = e.getMessage().getBytes();
                responseFormat = "text/plain";
                status = HttpServletResponse.SC_NOT_FOUND;
            } catch (DjatokaException e) {
                logger.error(e, e);
                bytes = e.getMessage().getBytes();
                responseFormat = "text/plain";
                status = HttpServletResponse.SC_NOT_FOUND;
            } catch (Exception e) {
                logger.error(e, e);
                bytes = e.getMessage().getBytes();
                responseFormat = "text/plain";
                status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            } catch (Error e) {
                logger.error(e, e);
                bytes = e.getMessage().getBytes();
                responseFormat = "text/plain";
                status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }
        }
        if (bytes == null || bytes.length == 0) {
            bytes = "".getBytes();
            responseFormat = "text/plain";
            status = HttpServletResponse.SC_NOT_FOUND;
        }
        HashMap<String, String> header_map = new HashMap<String, String>();
        header_map.put("Content-Length", bytes.length + "");
        header_map.put("Date", HttpDate.getHttpDate());
        return new OpenURLResponse(status, responseFormat, bytes, header_map);
    }

    private boolean isCacheable(DjatokaDecodeParam params) {
        if (transformCheck && params.getTransform().isTransformable()) return false;
        if (params.getRegion() != null) {
            String[] r = params.getRegion().split(",");
            if (r.length == 4) {
                int h = Integer.parseInt(r[2]);
                int w = Integer.parseInt(r[3]);
                if ((h * w) >= maxPixels) return false;
            }
        }
        return true;
    }

    private static final String getTileHash(ImageRecord r, DjatokaDecodeParam params) throws Exception {
        String id = r.getIdentifier();
        int level = params.getLevel();
        String region = params.getRegion();
        int rotateDegree = params.getRotationDegree();
        double scalingFactor = params.getScalingFactor();
        int[] scalingDims = params.getScalingDimensions();
        String scale = "";
        if (scalingDims != null && scalingDims.length == 1) scale = scalingDims[0] + "";
        if (scalingDims != null && scalingDims.length == 2) scale = scalingDims[0] + "," + scalingDims[1];
        int clayer = params.getCompositingLayer();
        String rft_id = id + "|" + level + "|" + region + "|" + rotateDegree + "|" + scalingFactor + "|" + scale + "|" + clayer;
        MessageDigest complete = MessageDigest.getInstance("SHA1");
        return new String(complete.digest(rft_id.getBytes()));
    }

    private static final String getExtension(String mimetype) {
        if (mimetype.equals(FORMAT_MIMEYPE_JPEG)) return FORMAT_ID_JPG;
        if (mimetype.equals(FORMAT_MIMEYPE_PNG)) return FORMAT_ID_PNG;
        if (mimetype.equals(FORMAT_MIMEYPE_BMP)) return FORMAT_ID_BMP;
        if (mimetype.equals(FORMAT_MIMEYPE_GIF)) return FORMAT_ID_GIF;
        if (mimetype.equals(FORMAT_MIMEYPE_PNM)) return FORMAT_ID_PNM;
        if (mimetype.equals(FORMAT_MIMEYPE_JP2)) return FORMAT_ID_JP2;
        if (mimetype.equals(FORMAT_MIMEYPE_JPX)) return FORMAT_ID_JPX;
        if (mimetype.equals(FORMAT_MIMEYPE_JPM)) return FORMAT_ID_JP2;
        return null;
    }

    private static HashMap<String, String> setServiceValues(ContextObject co) {
        HashMap<String, String> map = new HashMap<String, String>();
        Object[] svcData = (Object[]) co.getServiceTypes()[0].getDescriptors();
        if (svcData != null && svcData.length > 0) {
            for (int i = 0; i < svcData.length; i++) {
                Object tmp = svcData[i];
                if (tmp.getClass().getSimpleName().equals("ByValueMetadataImpl")) {
                    ByValueMetadataImpl kev = ((ByValueMetadataImpl) tmp);
                    if (kev.getFieldMap().size() > 0) {
                        if (kev.getFieldMap().containsKey("svc.region") && ((String[]) kev.getFieldMap().get("svc.region"))[0] != "") map.put("region", ((String[]) kev.getFieldMap().get("svc.region"))[0]);
                        if (kev.getFieldMap().containsKey("svc.format") && ((String[]) kev.getFieldMap().get("svc.format"))[0] != "") map.put("format", ((String[]) kev.getFieldMap().get("svc.format"))[0]);
                        if (kev.getFieldMap().containsKey("svc.level") && ((String[]) kev.getFieldMap().get("svc.level"))[0] != "") map.put("level", ((String[]) kev.getFieldMap().get("svc.level"))[0]);
                        if (kev.getFieldMap().containsKey("svc.rotate") && ((String[]) kev.getFieldMap().get("svc.rotate"))[0] != "") map.put("rotate", ((String[]) kev.getFieldMap().get("svc.rotate"))[0]);
                        if (kev.getFieldMap().containsKey("svc.scale") && ((String[]) kev.getFieldMap().get("svc.scale"))[0] != "") map.put("scale", ((String[]) kev.getFieldMap().get("svc.scale"))[0]);
                        if (kev.getFieldMap().containsKey("svc.clayer") && ((String[]) kev.getFieldMap().get("svc.clayer"))[0] != "") map.put("clayer", ((String[]) kev.getFieldMap().get("svc.clayer"))[0]);
                    }
                }
            }
        }
        return map;
    }
}
