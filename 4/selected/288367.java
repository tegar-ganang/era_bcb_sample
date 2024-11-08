package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.cache.MemoryCache;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.BasicTiledImageLayer;
import gov.nasa.worldwind.util.*;
import org.w3c.dom.Document;
import java.io.IOException;

/**
 * @author dcollins
 * @version $Id: TiledImageProducer.java 1 2011-07-16 23:22:47Z dcollins $
 */
public class TiledImageProducer extends TiledRasterProducer {

    protected static final String DEFAULT_IMAGE_FORMAT = "image/png";

    protected static final String DEFAULT_TEXTURE_FORMAT = "image/dds";

    protected static DataRasterReader[] readers = new DataRasterReader[] { new RPFRasterReader(), new GDALDataRasterReader(), new ImageIORasterReader(), new GeotiffRasterReader() };

    public TiledImageProducer(MemoryCache cache, int writeThreadPoolSize) {
        super(cache, writeThreadPoolSize);
    }

    public TiledImageProducer() {
        super();
    }

    public String getDataSourceDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(Logging.getMessage("TiledImageProducer.Description"));
        sb.append(" (").append(super.getDataSourceDescription()).append(")");
        return sb.toString();
    }

    protected DataRaster createDataRaster(int width, int height, Sector sector, AVList params) {
        int transparency = java.awt.image.BufferedImage.TRANSLUCENT;
        BufferedImageRaster raster = new BufferedImageRaster(width, height, transparency, sector);
        return raster;
    }

    protected DataRasterReader[] getDataRasterReaders() {
        return readers;
    }

    protected DataRasterWriter[] getDataRasterWriters() {
        return new DataRasterWriter[] { new ImageIORasterWriter(false), new DDSRasterWriter() };
    }

    protected String validateDataSource(Object source, AVList params) {
        if (source == null) return Logging.getMessage("nullValue.SourceIsNull");
        if (source instanceof DataRaster) {
            DataRaster raster = (DataRaster) source;
            if (!(raster instanceof BufferedImageRaster)) return Logging.getMessage("TiledRasterProducer.UnrecognizedDataSource", raster);
            String s = this.validateDataSourceParams(raster, String.valueOf(raster));
            if (s != null) return s;
        } else {
            params = (params == null) ? new AVListImpl() : params;
            DataRasterReader reader = this.getReaderFactory().findReaderFor(source, params, this.getDataRasterReaders());
            if (reader == null) {
                return Logging.getMessage("TiledRasterProducer.UnrecognizedDataSource", source);
            } else if (reader instanceof RPFRasterReader) {
                return null;
            }
            String errMsg = this.validateDataSourceParams(params, String.valueOf(source));
            if (!WWUtil.isEmpty(errMsg)) {
                try {
                    reader.readMetadata(source, params);
                    errMsg = this.validateDataSourceParams(params, String.valueOf(source));
                } catch (IOException e) {
                    return Logging.getMessage("TiledRasterProducer.ExceptionWhileReading", source, e.getMessage());
                }
            }
            if (!WWUtil.isEmpty(errMsg)) return errMsg;
        }
        return null;
    }

    protected String validateDataSourceParams(AVList params, String name) {
        if (params.hasKey(AVKey.PIXEL_FORMAT) && params.getValue(AVKey.PIXEL_FORMAT) != AVKey.IMAGE) {
            return Logging.getMessage("TiledRasterProducer.UnrecognizedRasterType", params.getValue(AVKey.PIXEL_FORMAT), name);
        }
        if (params.hasKey(AVKey.COORDINATE_SYSTEM) && params.getValue(AVKey.COORDINATE_SYSTEM) != AVKey.COORDINATE_SYSTEM_GEOGRAPHIC && params.getValue(AVKey.COORDINATE_SYSTEM) != AVKey.COORDINATE_SYSTEM_PROJECTED) {
            return Logging.getMessage("TiledRasterProducer.UnrecognizedCoordinateSystem", params.getValue(AVKey.COORDINATE_SYSTEM), name);
        }
        if (params.getValue(AVKey.SECTOR) == null) return Logging.getMessage("TiledRasterProducer.NoSector", name);
        return null;
    }

    protected void initProductionParameters(AVList params) {
        if (params.getValue(AVKey.FORMAT_SUFFIX) != null) {
            String s = WWIO.makeMimeTypeForSuffix(params.getValue(AVKey.FORMAT_SUFFIX).toString());
            if (s != null) {
                params.setValue(AVKey.IMAGE_FORMAT, s);
                params.setValue(AVKey.AVAILABLE_IMAGE_FORMATS, new String[] { s });
            }
        }
        if (params.getValue(AVKey.PIXEL_FORMAT) == null) {
            params.setValue(AVKey.PIXEL_FORMAT, AVKey.IMAGE);
        }
        if (params.getValue(AVKey.IMAGE_FORMAT) == null) {
            params.setValue(AVKey.IMAGE_FORMAT, DEFAULT_IMAGE_FORMAT);
        }
        if (params.getValue(AVKey.AVAILABLE_IMAGE_FORMATS) == null) {
            params.setValue(AVKey.AVAILABLE_IMAGE_FORMATS, new String[] { params.getValue(AVKey.IMAGE_FORMAT).toString() });
        }
        if (params.getValue(AVKey.FORMAT_SUFFIX) == null) {
            params.setValue(AVKey.FORMAT_SUFFIX, WWIO.makeSuffixForMimeType(params.getValue(AVKey.IMAGE_FORMAT).toString()));
        }
    }

    /**
     * Returns a Layer configuration document which describes the tiled imagery produced by this TiledImageProducer. The
     * document's contents are based on the configuration document for a TiledImageLayer, except this document describes
     * an offline dataset. This returns null if the parameter list is null, or if the configuration document cannot be
     * created for any reason.
     *
     * @param params the parameters which describe a Layer configuration document's contents.
     *
     * @return the configuration document, or null if the parameter list is null or does not contain the required
     *         parameters.
     */
    protected Document createConfigDoc(AVList params) {
        AVList configParams = params.copy();
        if (configParams.getValue(AVKey.DISPLAY_NAME) == null) configParams.setValue(AVKey.DISPLAY_NAME, params.getValue(AVKey.DATASET_NAME));
        if (configParams.getValue(AVKey.SERVICE_NAME) == null) configParams.setValue(AVKey.SERVICE_NAME, AVKey.SERVICE_NAME_OFFLINE);
        configParams.setValue(AVKey.NETWORK_RETRIEVAL_ENABLED, Boolean.FALSE);
        configParams.setValue(AVKey.TEXTURE_FORMAT, DEFAULT_TEXTURE_FORMAT);
        configParams.setValue(AVKey.USE_MIP_MAPS, Boolean.TRUE);
        configParams.setValue(AVKey.USE_TRANSPARENT_TEXTURES, Boolean.TRUE);
        return BasicTiledImageLayer.createTiledImageLayerConfigDocument(configParams);
    }
}
