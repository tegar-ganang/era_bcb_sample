package gov.nasa.worldwind.data;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.cache.MemoryCache;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.terrain.BasicElevationModel;
import gov.nasa.worldwind.util.*;
import org.w3c.dom.Document;
import java.io.IOException;

/**
 * @author dcollins
 * @version $Id: TiledElevationProducer.java 1 2011-07-16 23:22:47Z dcollins $
 */
public class TiledElevationProducer extends TiledRasterProducer {

    protected double[] extremes = null;

    protected static final String DEFAULT_IMAGE_FORMAT = "application/bil32";

    protected static final double DEFAULT_MISSING_DATA_SIGNAL = (double) Short.MIN_VALUE;

    protected static DataRasterReader[] readers = new DataRasterReader[] { new GDALDataRasterReader(), new BILRasterReader(), new DTEDRasterReader(), new GeotiffRasterReader() };

    public TiledElevationProducer(MemoryCache cache, int writeThreadPoolSize) {
        super(cache, writeThreadPoolSize);
    }

    public TiledElevationProducer() {
        super();
    }

    /**
     * Overridden to initialize this producer's extreme elevations prior to creating and installing elevation tiles.
     *
     * @param parameters the installation parameters.
     *
     * @throws Exception if production fails for any reason.
     */
    @Override
    protected void doStartProduction(AVList parameters) throws Exception {
        this.extremes = null;
        super.doStartProduction(parameters);
    }

    public String getDataSourceDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(Logging.getMessage("TiledElevationProducer.Description"));
        sb.append(" (").append(super.getDataSourceDescription()).append(")");
        return sb.toString();
    }

    protected DataRaster createDataRaster(int width, int height, Sector sector, AVList params) {
        AVList bufferParams = new AVListImpl();
        bufferParams.setValue(AVKey.DATA_TYPE, params.getValue(AVKey.DATA_TYPE));
        bufferParams.setValue(AVKey.BYTE_ORDER, params.getValue(AVKey.BYTE_ORDER));
        ByteBufferRaster bufferRaster = new ByteBufferRaster(width, height, sector, bufferParams);
        Object o = params.getValue(AVKey.MISSING_DATA_REPLACEMENT);
        if (o != null && o instanceof Double) {
            Double missingDataValue = (Double) o;
            bufferRaster.fill(missingDataValue);
            bufferRaster.setTransparentValue(missingDataValue);
        }
        return bufferRaster;
    }

    protected DataRasterReader[] getDataRasterReaders() {
        return readers;
    }

    protected DataRasterWriter[] getDataRasterWriters() {
        return new DataRasterWriter[] { new BILRasterWriter(true) };
    }

    protected String validateDataSource(Object source, AVList params) {
        if (source == null) {
            return Logging.getMessage("nullValue.SourceIsNull");
        }
        if (source instanceof DataRaster) {
            DataRaster raster = (DataRaster) source;
            if (!(raster instanceof BufferWrapperRaster)) {
                return Logging.getMessage("TiledRasterProducer.UnrecognizedDataSource", raster);
            }
            String s = this.validateDataSourceParams(raster, String.valueOf(raster));
            if (s != null) {
                return s;
            }
        } else {
            DataRasterReader reader = this.getReaderFactory().findReaderFor(source, params, this.getDataRasterReaders());
            if (reader == null) {
                return Logging.getMessage("TiledRasterProducer.UnrecognizedDataSource", source);
            }
            params = (params != null) ? params.copy() : new AVListImpl();
            try {
                reader.readMetadata(source, params);
            } catch (IOException e) {
                return Logging.getMessage("TiledRasterProducer.ExceptionWhileReading", source, e.getMessage());
            }
            String s = this.validateDataSourceParams(params, String.valueOf(source));
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    protected String validateDataSourceParams(AVList params, String name) {
        if (params.hasKey(AVKey.PIXEL_FORMAT) && params.getValue(AVKey.PIXEL_FORMAT) != AVKey.ELEVATION) {
            return Logging.getMessage("TiledRasterProducer.UnrecognizedRasterType", params.getValue(AVKey.PIXEL_FORMAT), name);
        }
        if (params.hasKey(AVKey.COORDINATE_SYSTEM) && params.getValue(AVKey.COORDINATE_SYSTEM) != AVKey.COORDINATE_SYSTEM_GEOGRAPHIC && params.getValue(AVKey.COORDINATE_SYSTEM) != AVKey.COORDINATE_SYSTEM_PROJECTED) {
            return Logging.getMessage("TiledRasterProducer.UnrecognizedCoordinateSystem", params.getValue(AVKey.COORDINATE_SYSTEM), name);
        }
        if (params.hasKey(AVKey.ELEVATION_UNIT) && params.getValue(AVKey.ELEVATION_UNIT) != AVKey.UNIT_METER && params.getValue(AVKey.ELEVATION_UNIT) != AVKey.UNIT_FOOT) {
            return Logging.getMessage("TiledElevationProducer.UnrecognizedElevationUnit", params.getValue(AVKey.ELEVATION_UNIT), name);
        }
        if (params.getValue(AVKey.SECTOR) == null) {
            return Logging.getMessage("TiledRasterProducer.NoSector", name);
        }
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
            params.setValue(AVKey.PIXEL_FORMAT, AVKey.ELEVATION);
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
        if (params.getValue(AVKey.DATA_TYPE) == null && params.getValue(AVKey.IMAGE_FORMAT) != null) {
            String s = WWIO.makeDataTypeForMimeType(params.getValue(AVKey.IMAGE_FORMAT).toString());
            if (s != null) {
                params.setValue(AVKey.DATA_TYPE, s);
            }
        }
        if (params.getValue(AVKey.DATA_TYPE) == null) {
            params.setValue(AVKey.DATA_TYPE, AVKey.INT16);
        }
        if (params.getValue(AVKey.BYTE_ORDER) == null) {
            params.setValue(AVKey.BYTE_ORDER, AVKey.LITTLE_ENDIAN);
        }
        if (params.getValue(AVKey.MISSING_DATA_REPLACEMENT) == null) {
            params.setValue(AVKey.MISSING_DATA_REPLACEMENT, DEFAULT_MISSING_DATA_SIGNAL);
        }
    }

    protected LatLon computeRasterTileDelta(int tileWidth, int tileHeight, Iterable<? extends DataRaster> rasters) {
        LatLon pixelSize = this.computeSmallestPixelSize(rasters);
        double latDelta = (tileHeight - 1) * pixelSize.getLatitude().degrees;
        double lonDelta = (tileWidth - 1) * pixelSize.getLongitude().degrees;
        return LatLon.fromDegrees(latDelta, lonDelta);
    }

    /**
     * Overridden to compute the extreme elevations prior to installing a tile in the filesystem.
     *
     * @param tile       the tile to install to the filesystem.
     * @param tileRaster the raster containing the raster's data content.
     * @param params     the installation parameters.
     */
    @Override
    protected void installTileRasterLater(LevelSet levelSet, Tile tile, DataRaster tileRaster, AVList params) {
        if (levelSet.isFinalLevel(tile.getLevelNumber())) {
            this.updateExtremeElevations(tileRaster);
        }
        super.installTileRasterLater(levelSet, tile, tileRaster, params);
    }

    protected void updateExtremeElevations(DataRaster raster) {
        if (!(raster instanceof BufferWrapperRaster)) {
            String message = Logging.getMessage("DataRaster.IncompatibleRaster", raster);
            Logging.logger().severe(message);
            return;
        }
        if (this.extremes == null) {
            this.extremes = WWUtil.defaultMinMix();
        }
        double[] tileExtremes = new double[2];
        if (raster.hasKey(AVKey.ELEVATION_MIN) && raster.hasKey(AVKey.ELEVATION_MAX)) {
            tileExtremes[0] = (Double) raster.getValue(AVKey.ELEVATION_MAX);
            tileExtremes[1] = (Double) raster.getValue(AVKey.ELEVATION_MIN);
        } else {
            tileExtremes = ((BufferWrapperRaster) raster).getExtremes();
            if (tileExtremes == null || tileExtremes.length < 2) {
                return;
            }
        }
        if (this.extremes[0] > tileExtremes[0]) {
            this.extremes[0] = tileExtremes[0];
        }
        if (this.extremes[1] < tileExtremes[1]) {
            this.extremes[1] = tileExtremes[1];
        }
    }

    /**
     * Returns an ElevationModel configuration document which describes the tiled elevation data produced by this
     * TiledElevationProducer. The document's contents are based on the configuration document for a basic
     * ElevationModel, except this document describes an offline dataset. This returns null if the parameter list is
     * null, or if the configuration document cannot be created for any reason.
     *
     * @param params the parameters which describe an ElevationModel configuration document's contents.
     *
     * @return the configuration document, or null if the parameter list is null or does not contain the required
     *         parameters.
     */
    protected Document createConfigDoc(AVList params) {
        AVList configParams = params.copy();
        if (configParams.getValue(AVKey.DISPLAY_NAME) == null) {
            configParams.setValue(AVKey.DISPLAY_NAME, params.getValue(AVKey.DATASET_NAME));
        }
        if (configParams.getValue(AVKey.SERVICE_NAME) == null) {
            configParams.setValue(AVKey.SERVICE_NAME, AVKey.SERVICE_NAME_OFFLINE);
        }
        configParams.setValue(AVKey.NETWORK_RETRIEVAL_ENABLED, Boolean.FALSE);
        configParams.removeKey(AVKey.MISSING_DATA_REPLACEMENT);
        configParams.setValue(AVKey.MISSING_DATA_SIGNAL, params.getValue(AVKey.MISSING_DATA_REPLACEMENT));
        if (this.extremes != null && this.extremes.length >= 2) {
            configParams.setValue(AVKey.ELEVATION_MIN, this.extremes[0]);
            configParams.setValue(AVKey.ELEVATION_MAX, this.extremes[1]);
        }
        return BasicElevationModel.createBasicElevationModelConfigDocument(configParams);
    }
}
