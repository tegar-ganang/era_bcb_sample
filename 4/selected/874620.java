package gov.nasa.worldwind.data;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.cache.*;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.*;
import org.w3c.dom.Document;
import java.io.File;
import java.lang.Thread;

/**
 * @author dcollins
 * @version $Id: TiledRasterProducer.java 1 2011-07-16 23:22:47Z dcollins $
 */
public abstract class TiledRasterProducer extends AbstractDataStoreProducer {

    private static final long DEFAULT_TILED_RASTER_PRODUCER_CACHE_SIZE = 300000000L;

    private static final int DEFAULT_TILED_RASTER_PRODUCER_LARGE_DATASET_THRESHOLD = 3000;

    private static final int DEFAULT_WRITE_THREAD_POOL_SIZE = 2;

    private static final int DEFAULT_TILE_WIDTH_AND_HEIGHT = 512;

    private static final int DEFAULT_SINGLE_LEVEL_TILE_WIDTH_AND_HEIGHT = 512;

    private static final double DEFAULT_LEVEL_ZERO_TILE_DELTA = 36d;

    private java.util.List<DataRaster> dataRasterList = new java.util.ArrayList<DataRaster>();

    private MemoryCache rasterCache;

    private final java.util.concurrent.ExecutorService tileWriteService;

    private final java.util.concurrent.Semaphore tileWriteSemaphore;

    private final Object fileLock = new Object();

    private int tile;

    private int tileCount;

    private DataRasterReaderFactory readerFactory;

    public TiledRasterProducer(MemoryCache cache, int writeThreadPoolSize) {
        if (cache == null) {
            String message = Logging.getMessage("nullValue.CacheIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (writeThreadPoolSize < 1) {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", "writeThreadPoolSize < 1");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        this.rasterCache = cache;
        this.tileWriteService = this.createDefaultTileWriteService(writeThreadPoolSize);
        this.tileWriteSemaphore = new java.util.concurrent.Semaphore(writeThreadPoolSize, true);
        try {
            readerFactory = (DataRasterReaderFactory) WorldWind.createConfigurationComponent(AVKey.DATA_RASTER_READER_FACTORY_CLASS_NAME);
        } catch (Exception e) {
            readerFactory = new BasicDataRasterReaderFactory();
        }
    }

    public TiledRasterProducer() {
        this(createDefaultCache(), DEFAULT_WRITE_THREAD_POOL_SIZE);
    }

    public Iterable<DataRaster> getDataRasters() {
        return this.dataRasterList;
    }

    protected DataRasterReaderFactory getReaderFactory() {
        return this.readerFactory;
    }

    public String getDataSourceDescription() {
        DataRasterReader[] readers = this.getDataRasterReaders();
        if (readers == null || readers.length < 1) return "";
        java.util.Set<String> suffixSet = new java.util.TreeSet<String>();
        java.util.Set<String> descriptionSet = new java.util.TreeSet<String>();
        for (DataRasterReader reader : readers) {
            String description = reader.getDescription();
            String[] names = reader.getSuffixes();
            if (names != null && names.length > 0) suffixSet.addAll(java.util.Arrays.asList(names)); else descriptionSet.add(description);
        }
        StringBuilder sb = new StringBuilder();
        for (String suffix : suffixSet) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("*.").append(suffix);
        }
        for (String description : descriptionSet) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(description);
        }
        return sb.toString();
    }

    public void removeProductionState() {
        java.io.File installLocation = this.installLocationFor(this.getStoreParameters());
        if (installLocation == null || !installLocation.exists()) {
            String message = Logging.getMessage("TiledRasterProducer.NoInstallLocation", this.getStoreParameters().getValue(AVKey.DATASET_NAME));
            Logging.logger().warning(message);
            return;
        }
        try {
            WWIO.deleteDirectory(installLocation);
        } catch (Exception e) {
            String message = Logging.getMessage("TiledRasterProducer.ExceptionRemovingProductionState", this.getStoreParameters().getValue(AVKey.DATASET_NAME));
            Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
        }
    }

    protected abstract DataRaster createDataRaster(int width, int height, Sector sector, AVList params);

    protected abstract DataRasterReader[] getDataRasterReaders();

    protected abstract DataRasterWriter[] getDataRasterWriters();

    protected MemoryCache getCache() {
        return this.rasterCache;
    }

    protected java.util.concurrent.ExecutorService getTileWriteService() {
        return this.tileWriteService;
    }

    protected java.util.concurrent.Semaphore getTileWriteSemaphore() {
        return this.tileWriteSemaphore;
    }

    protected void doStartProduction(AVList parameters) throws Exception {
        this.productionParams = parameters.copy();
        this.initProductionParameters(this.productionParams);
        this.assembleDataRasters();
        this.initLevelSetParameters(this.productionParams);
        LevelSet levelSet = new LevelSet(this.productionParams);
        this.installLevelSet(levelSet, this.productionParams);
        this.waitForInstallTileTasks();
        this.getCache().clear();
        this.installConfigFile(this.productionParams);
    }

    protected String validateProductionParameters(AVList parameters) {
        StringBuilder sb = new StringBuilder();
        Object o = parameters.getValue(AVKey.FILE_STORE_LOCATION);
        if (o == null || !(o instanceof String) || ((String) o).length() < 1) sb.append((sb.length() > 0 ? ", " : "")).append(Logging.getMessage("term.fileStoreLocation"));
        o = parameters.getValue(AVKey.DATA_CACHE_NAME);
        if (o == null || !(o instanceof String) || ((String) o).length() == 0) sb.append((sb.length() > 0 ? ", " : "")).append(Logging.getMessage("term.fileStoreFolder"));
        o = parameters.getValue(AVKey.DATASET_NAME);
        if (o == null || !(o instanceof String) || ((String) o).length() < 1) sb.append((sb.length() > 0 ? ", " : "")).append(Logging.getMessage("term.datasetName"));
        if (sb.length() == 0) return null;
        return Logging.getMessage("DataStoreProducer.InvalidDataStoreParamters", sb.toString());
    }

    protected java.io.File installLocationFor(AVList params) {
        String fileStoreLocation = params.getStringValue(AVKey.FILE_STORE_LOCATION);
        String dataCacheName = params.getStringValue(AVKey.DATA_CACHE_NAME);
        if (fileStoreLocation == null || dataCacheName == null) return null;
        String path = WWIO.appendPathPart(fileStoreLocation, dataCacheName);
        if (path == null || path.length() == 0) return null;
        return new java.io.File(path);
    }

    protected abstract void initProductionParameters(AVList params);

    protected void initLevelSetParameters(AVList params) {
        int largeThreshold = Configuration.getIntegerValue(AVKey.TILED_RASTER_PRODUCER_LARGE_DATASET_THRESHOLD, DEFAULT_TILED_RASTER_PRODUCER_LARGE_DATASET_THRESHOLD);
        boolean isDataSetLarge = this.isDataSetLarge(this.dataRasterList, largeThreshold);
        Sector sector = (Sector) params.getValue(AVKey.SECTOR);
        if (sector == null) {
            sector = this.computeBoundingSector(this.dataRasterList);
            if (sector != null) sector = sector.intersection(Sector.FULL_SPHERE);
            params.setValue(AVKey.SECTOR, sector);
        }
        Integer tileWidth = (Integer) params.getValue(AVKey.TILE_WIDTH);
        if (tileWidth == null) {
            tileWidth = isDataSetLarge ? DEFAULT_TILE_WIDTH_AND_HEIGHT : DEFAULT_SINGLE_LEVEL_TILE_WIDTH_AND_HEIGHT;
            params.setValue(AVKey.TILE_WIDTH, tileWidth);
        }
        Integer tileHeight = (Integer) params.getValue(AVKey.TILE_HEIGHT);
        if (tileHeight == null) {
            tileHeight = isDataSetLarge ? DEFAULT_TILE_WIDTH_AND_HEIGHT : DEFAULT_SINGLE_LEVEL_TILE_WIDTH_AND_HEIGHT;
            params.setValue(AVKey.TILE_HEIGHT, tileHeight);
        }
        LatLon rasterTileDelta = this.computeRasterTileDelta(tileWidth, tileHeight, this.dataRasterList);
        LatLon desiredLevelZeroDelta = this.computeDesiredTileDelta(sector);
        Integer numLevels = (Integer) params.getValue(AVKey.NUM_LEVELS);
        if (numLevels == null) {
            numLevels = isDataSetLarge ? this.computeNumLevels(desiredLevelZeroDelta, rasterTileDelta) : 1;
            params.setValue(AVKey.NUM_LEVELS, numLevels);
        }
        Integer numEmptyLevels = (Integer) params.getValue(AVKey.NUM_EMPTY_LEVELS);
        if (numEmptyLevels == null) {
            numEmptyLevels = 0;
            params.setValue(AVKey.NUM_EMPTY_LEVELS, numEmptyLevels);
        }
        LatLon levelZeroTileDelta = (LatLon) params.getValue(AVKey.LEVEL_ZERO_TILE_DELTA);
        if (levelZeroTileDelta == null) {
            double scale = Math.pow(2d, numLevels - 1);
            levelZeroTileDelta = LatLon.fromDegrees(scale * rasterTileDelta.getLatitude().degrees, scale * rasterTileDelta.getLongitude().degrees);
            params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, levelZeroTileDelta);
        }
        LatLon tileOrigin = (LatLon) params.getValue(AVKey.TILE_ORIGIN);
        if (tileOrigin == null) {
            tileOrigin = new LatLon(sector.getMinLatitude(), sector.getMinLongitude());
            params.setValue(AVKey.TILE_ORIGIN, tileOrigin);
        }
        if (!this.isWithinLatLonLimits(sector, levelZeroTileDelta, tileOrigin)) {
            String message = "TiledRasterProducer: native tiling is outside lat/lon limits. Falling back to default tiling.";
            Logging.logger().warning(message);
            levelZeroTileDelta = LatLon.fromDegrees(DEFAULT_LEVEL_ZERO_TILE_DELTA, DEFAULT_LEVEL_ZERO_TILE_DELTA);
            params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, levelZeroTileDelta);
            tileOrigin = new LatLon(Angle.NEG90, Angle.NEG180);
            params.setValue(AVKey.TILE_ORIGIN, tileOrigin);
            numLevels = this.computeNumLevels(levelZeroTileDelta, rasterTileDelta);
            params.setValue(AVKey.NUM_LEVELS, numLevels);
            int numLevelsNeeded = isDataSetLarge ? this.computeNumLevels(desiredLevelZeroDelta, rasterTileDelta) : 1;
            numEmptyLevels = (numLevels > numLevelsNeeded) ? (numLevels - numLevelsNeeded) : 0;
            params.setValue(AVKey.NUM_EMPTY_LEVELS, numEmptyLevels);
        }
    }

    protected boolean isDataSetLarge(Iterable<? extends DataRaster> rasters, int largeThreshold) {
        Sector sector = this.computeBoundingSector(rasters);
        LatLon pixelSize = this.computeSmallestPixelSize(rasters);
        int sectorWidth = (int) Math.ceil(sector.getDeltaLonDegrees() / pixelSize.getLongitude().degrees);
        int sectorHeight = (int) Math.ceil(sector.getDeltaLatDegrees() / pixelSize.getLatitude().degrees);
        return (sectorWidth >= largeThreshold) || (sectorHeight >= largeThreshold);
    }

    protected boolean isWithinLatLonLimits(Sector sector, LatLon tileDelta, LatLon tileOrigin) {
        double minLat = Math.floor((sector.getMinLatitude().degrees - tileOrigin.getLatitude().degrees) / tileDelta.getLatitude().degrees);
        minLat = tileOrigin.getLatitude().degrees + minLat * tileDelta.getLatitude().degrees;
        double maxLat = Math.ceil((sector.getMaxLatitude().degrees - tileOrigin.getLatitude().degrees) / tileDelta.getLatitude().degrees);
        maxLat = tileOrigin.getLatitude().degrees + maxLat * tileDelta.getLatitude().degrees;
        double minLon = Math.floor((sector.getMinLongitude().degrees - tileOrigin.getLongitude().degrees) / tileDelta.getLongitude().degrees);
        minLon = tileOrigin.getLongitude().degrees + minLon * tileDelta.getLongitude().degrees;
        double maxLon = Math.ceil((sector.getMaxLongitude().degrees - tileOrigin.getLongitude().degrees) / tileDelta.getLongitude().degrees);
        maxLon = tileOrigin.getLongitude().degrees + maxLon * tileDelta.getLongitude().degrees;
        return Sector.fromDegrees(minLat, maxLat, minLon, maxLon).isWithinLatLonLimits();
    }

    protected Sector computeBoundingSector(Iterable<? extends DataRaster> rasters) {
        Sector sector = null;
        for (DataRaster raster : rasters) {
            sector = (sector != null) ? raster.getSector().union(sector) : raster.getSector();
        }
        return sector;
    }

    protected LatLon computeRasterTileDelta(int tileWidth, int tileHeight, Iterable<? extends DataRaster> rasters) {
        LatLon pixelSize = this.computeSmallestPixelSize(rasters);
        double latDelta = tileHeight * pixelSize.getLatitude().degrees;
        double lonDelta = tileWidth * pixelSize.getLongitude().degrees;
        return LatLon.fromDegrees(latDelta, lonDelta);
    }

    protected LatLon computeDesiredTileDelta(Sector sector) {
        double levelZeroLat = Math.min(sector.getDeltaLatDegrees(), DEFAULT_LEVEL_ZERO_TILE_DELTA);
        double levelZeroLon = Math.min(sector.getDeltaLonDegrees(), DEFAULT_LEVEL_ZERO_TILE_DELTA);
        return LatLon.fromDegrees(levelZeroLat, levelZeroLon);
    }

    protected LatLon computeRasterPixelSize(DataRaster raster) {
        return LatLon.fromDegrees(raster.getSector().getDeltaLatDegrees() / raster.getHeight(), raster.getSector().getDeltaLonDegrees() / raster.getWidth());
    }

    protected LatLon computeSmallestPixelSize(Iterable<? extends DataRaster> rasters) {
        double smallestLat = Double.MAX_VALUE;
        double smallestLon = Double.MAX_VALUE;
        for (DataRaster raster : rasters) {
            LatLon curSize = this.computeRasterPixelSize(raster);
            if (smallestLat > curSize.getLatitude().degrees) smallestLat = curSize.getLatitude().degrees;
            if (smallestLon > curSize.getLongitude().degrees) smallestLon = curSize.getLongitude().degrees;
        }
        return LatLon.fromDegrees(smallestLat, smallestLon);
    }

    protected int computeNumLevels(LatLon levelZeroDelta, LatLon lastLevelDelta) {
        double numLatLevels = WWMath.logBase2(levelZeroDelta.getLatitude().getDegrees()) - WWMath.logBase2(lastLevelDelta.getLatitude().getDegrees());
        double numLonLevels = WWMath.logBase2(levelZeroDelta.getLongitude().getDegrees()) - WWMath.logBase2(lastLevelDelta.getLongitude().getDegrees());
        int numLevels = (int) Math.ceil(Math.max(numLatLevels, numLonLevels));
        if (numLevels < 1) numLevels = 1;
        return numLevels;
    }

    protected void assembleDataRasters() throws Exception {
        if (this.isStopped()) return;
        for (SourceInfo info : this.getDataSourceList()) {
            if (this.isStopped()) break;
            Thread.sleep(0);
            this.assembleDataSource(info.source, info);
        }
    }

    protected void assembleDataSource(Object source, AVList params) throws Exception {
        if (source instanceof DataRaster) {
            this.dataRasterList.add((DataRaster) source);
        } else {
            DataRasterReader reader = this.readerFactory.findReaderFor(source, params, this.getDataRasterReaders());
            this.dataRasterList.add(new CachedDataRaster(source, params, reader, this.getCache()));
        }
    }

    protected static MemoryCache createDefaultCache() {
        long cacheSize = Configuration.getLongValue(AVKey.TILED_RASTER_PRODUCER_CACHE_SIZE, DEFAULT_TILED_RASTER_PRODUCER_CACHE_SIZE);
        return new BasicMemoryCache((long) (0.8 * cacheSize), cacheSize);
    }

    protected void installLevelSet(LevelSet levelSet, AVList params) throws java.io.IOException {
        if (this.isStopped()) return;
        this.calculateTileCount(levelSet, params);
        this.startProgress();
        Sector sector = levelSet.getSector();
        Level level = levelSet.getFirstLevel();
        Angle dLat = level.getTileDelta().getLatitude();
        Angle dLon = level.getTileDelta().getLongitude();
        Angle latOrigin = levelSet.getTileOrigin().getLatitude();
        Angle lonOrigin = levelSet.getTileOrigin().getLongitude();
        int firstRow = Tile.computeRow(dLat, sector.getMinLatitude(), latOrigin);
        int firstCol = Tile.computeColumn(dLon, sector.getMinLongitude(), lonOrigin);
        int lastRow = Tile.computeRow(dLat, sector.getMaxLatitude(), latOrigin);
        int lastCol = Tile.computeColumn(dLon, sector.getMaxLongitude(), lonOrigin);
        buildLoop: {
            Angle p1 = Tile.computeRowLatitude(firstRow, dLat, latOrigin);
            for (int row = firstRow; row <= lastRow; row++) {
                Angle p2 = p1.add(dLat);
                Angle t1 = Tile.computeColumnLongitude(firstCol, dLon, lonOrigin);
                for (int col = firstCol; col <= lastCol; col++) {
                    Thread.yield();
                    if (this.isStopped()) break buildLoop;
                    Angle t2 = t1.add(dLon);
                    Tile tile = new Tile(new Sector(p1, p2, t1, t2), level, row, col);
                    DataRaster tileRaster = this.createTileRaster(levelSet, tile, params);
                    if (tileRaster != null) this.installTileRasterLater(levelSet, tile, tileRaster, params);
                    t1 = t2;
                }
                p1 = p2;
            }
        }
    }

    protected DataRaster createTileRaster(LevelSet levelSet, Tile tile, AVList params) throws java.io.IOException {
        if (this.isStopped()) return null;
        DataRaster tileRaster;
        if (this.isFinalLevel(levelSet, tile.getLevelNumber(), params)) {
            tileRaster = this.drawDataSources(levelSet, tile, this.dataRasterList, params);
        } else {
            tileRaster = this.drawDescendants(levelSet, tile, params);
        }
        this.updateProgress();
        return tileRaster;
    }

    protected DataRaster drawDataSources(LevelSet levelSet, Tile tile, Iterable<DataRaster> dataRasters, AVList params) throws java.io.IOException {
        DataRaster tileRaster = null;
        java.util.ArrayList<DataRaster> intersectingRasters = new java.util.ArrayList<DataRaster>();
        for (DataRaster raster : dataRasters) {
            if (raster.getSector().intersects(tile.getSector()) && raster.getSector().intersects(levelSet.getSector())) intersectingRasters.add(raster);
        }
        if (!intersectingRasters.isEmpty() && !tile.getLevel().isEmpty()) {
            tileRaster = this.createDataRaster(tile.getLevel().getTileWidth(), tile.getLevel().getTileHeight(), tile.getSector(), params);
            for (DataRaster raster : intersectingRasters) {
                raster.drawOnTo(tileRaster);
            }
        }
        intersectingRasters.clear();
        intersectingRasters = null;
        return tileRaster;
    }

    protected DataRaster drawDescendants(LevelSet levelSet, Tile tile, AVList params) throws java.io.IOException {
        DataRaster tileRaster = null;
        boolean hasDescendants = false;
        Tile[] subTiles = this.createSubTiles(tile, levelSet.getLevel(tile.getLevelNumber() + 1));
        DataRaster[] subRasters = new DataRaster[subTiles.length];
        for (int index = 0; index < subTiles.length; index++) {
            if (subTiles[index].getSector().intersects(levelSet.getSector())) {
                DataRaster subRaster = this.createTileRaster(levelSet, subTiles[index], params);
                if (subRaster != null) {
                    subRasters[index] = subRaster;
                    hasDescendants = true;
                }
            }
        }
        if (this.isStopped()) return null;
        if (hasDescendants) {
            if (!tile.getLevel().isEmpty()) {
                tileRaster = this.createDataRaster(tile.getLevel().getTileWidth(), tile.getLevel().getTileHeight(), tile.getSector(), params);
                for (int index = 0; index < subTiles.length; index++) {
                    if (subRasters[index] != null) {
                        subRasters[index].drawOnTo(tileRaster);
                        this.installTileRasterLater(levelSet, subTiles[index], subRasters[index], params);
                    }
                }
            }
        }
        for (int index = 0; index < subTiles.length; index++) {
            subTiles[index] = null;
            subRasters[index] = null;
        }
        subTiles = null;
        subRasters = null;
        return tileRaster;
    }

    protected Tile[] createSubTiles(Tile tile, Level nextLevel) {
        Angle p0 = tile.getSector().getMinLatitude();
        Angle p2 = tile.getSector().getMaxLatitude();
        Angle p1 = Angle.midAngle(p0, p2);
        Angle t0 = tile.getSector().getMinLongitude();
        Angle t2 = tile.getSector().getMaxLongitude();
        Angle t1 = Angle.midAngle(t0, t2);
        int row = tile.getRow();
        int col = tile.getColumn();
        Tile[] subTiles = new Tile[4];
        subTiles[0] = new Tile(new Sector(p0, p1, t0, t1), nextLevel, 2 * row, 2 * col);
        subTiles[1] = new Tile(new Sector(p0, p1, t1, t2), nextLevel, 2 * row, 2 * col + 1);
        subTiles[2] = new Tile(new Sector(p1, p2, t1, t2), nextLevel, 2 * row + 1, 2 * col + 1);
        subTiles[3] = new Tile(new Sector(p1, p2, t0, t1), nextLevel, 2 * row + 1, 2 * col);
        return subTiles;
    }

    protected boolean isFinalLevel(LevelSet levelSet, int levelNumber, AVList params) {
        if (levelSet.isFinalLevel(levelNumber)) return true;
        int maxNumOfLevels = levelSet.getLastLevel().getLevelNumber();
        int limit = this.extractMaxLevelLimit(params, maxNumOfLevels);
        return (levelNumber >= limit);
    }

    /**
     * Extracts a maximum level limit from the AVList if the AVList contains AVKey.TILED_RASTER_PRODUCER_LIMIT_MAX_LEVEL.
     * This method requires <code>maxNumOfLevels</code> - the actual maximum numbers of levels.
     *
     * The AVKey.TILED_RASTER_PRODUCER_LIMIT_MAX_LEVEL could specify multiple things:
     *
     * If the value of the AVKey.TILED_RASTER_PRODUCER_LIMIT_MAX_LEVEL is "Auto" (as String),
     * the calculated limit of levels will be 70% of the actual maximum numbers of levels <code>maxNumOfLevels</code>.
     *
     * If the type of the value of the AVKey.TILED_RASTER_PRODUCER_LIMIT_MAX_LEVEL is Integer,
     * it should contain an integer number between 0 (for level 0 only) and the actual maximum
     * numbers of levels <code>maxNumOfLevels</code>.
     *
     * It is also possible to specify the limit as percents, in this case the type of the
     * AVKey.TILED_RASTER_PRODUCER_LIMIT_MAX_LEVEL value must be "String", have a numeric value as text and
     * the "%" percent sign in the end. Examples: "100%", "25%", "50%", etc.
     *
     * Value of AVKey.TILED_RASTER_PRODUCER_LIMIT_MAX_LEVEL could be a numeric string (for example, "3"),
     * or Integer. The value will be correctly extracted and compared with the <code>maxNumOfLevels</code>.
     * Valid values must be smaller or equal to <code>maxNumOfLevels</code>.
     *
     * @param params AVList that may contain AVKey.TILED_RASTER_PRODUCER_LIMIT_MAX_LEVEL property
     * @param maxNumOfLevels The actual maximum numbers of levels
     *
     * @return A limit of numbers of levels that should producer generate.
     *
     */
    protected int extractMaxLevelLimit(AVList params, int maxNumOfLevels) {
        if (null != params && params.hasKey(AVKey.TILED_RASTER_PRODUCER_LIMIT_MAX_LEVEL)) {
            Object o = params.getValue(AVKey.TILED_RASTER_PRODUCER_LIMIT_MAX_LEVEL);
            if (o instanceof Integer) {
                int limit = (Integer) o;
                return (limit <= maxNumOfLevels) ? limit : maxNumOfLevels;
            } else if (o instanceof String) {
                String strLimit = (String) o;
                if ("Auto".equalsIgnoreCase(strLimit)) {
                    return (int) Math.ceil(0.75d * (double) maxNumOfLevels);
                } else if (strLimit.endsWith("%")) {
                    try {
                        float percent = Float.parseFloat(strLimit.substring(0, strLimit.length() - 1));
                        int limit = (int) Math.ceil(percent * (double) maxNumOfLevels);
                        return (limit <= maxNumOfLevels) ? limit : maxNumOfLevels;
                    } catch (Throwable t) {
                        Logging.logger().finest(WWUtil.extractExceptionReason(t));
                    }
                } else {
                    try {
                        int limit = Integer.parseInt(strLimit);
                        return (limit <= maxNumOfLevels) ? limit : maxNumOfLevels;
                    } catch (Throwable t) {
                        Logging.logger().finest(WWUtil.extractExceptionReason(t));
                    }
                }
            }
        }
        return maxNumOfLevels;
    }

    protected java.util.concurrent.ExecutorService createDefaultTileWriteService(int threadPoolSize) {
        return new java.util.concurrent.ThreadPoolExecutor(threadPoolSize, threadPoolSize, 0L, java.util.concurrent.TimeUnit.MILLISECONDS, new java.util.concurrent.LinkedBlockingQueue<Runnable>()) {

            protected void afterExecute(Runnable runnable, Throwable t) {
                super.afterExecute(runnable, t);
                TiledRasterProducer.this.installTileRasterComplete();
            }
        };
    }

    protected void installTileRasterLater(final LevelSet levelSet, final Tile tile, final DataRaster tileRaster, final AVList params) {
        this.getTileWriteSemaphore().acquireUninterruptibly();
        this.getTileWriteService().execute(new Runnable() {

            public void run() {
                try {
                    installTileRaster(tile, tileRaster, params);
                    if (tileRaster instanceof Disposable) ((Disposable) tileRaster).dispose();
                } catch (Throwable t) {
                    String message = Logging.getMessage("generic.ExceptionWhileWriting", tile);
                    Logging.logger().log(java.util.logging.Level.SEVERE, message, t);
                }
            }
        });
    }

    protected void installTileRasterComplete() {
        this.getTileWriteSemaphore().release();
    }

    protected void waitForInstallTileTasks() {
        try {
            java.util.concurrent.ExecutorService service = this.getTileWriteService();
            service.shutdown();
            while (!service.awaitTermination(1000L, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                Thread.sleep(5L);
            }
        } catch (InterruptedException e) {
            String msg = Logging.getMessage("generic.interrupted", this.getClass().getName(), "waitForInstallTileTasks()");
            Logging.logger().finest(msg);
            Thread.currentThread().interrupt();
        }
    }

    protected void installTileRaster(Tile tile, DataRaster tileRaster, AVList params) throws java.io.IOException {
        java.io.File installLocation;
        Object result = this.installLocationForTile(params, tile);
        if (result instanceof java.io.File) {
            installLocation = (java.io.File) result;
        } else {
            String message = result.toString();
            Logging.logger().severe(message);
            throw new java.io.IOException(message);
        }
        synchronized (this.fileLock) {
            java.io.File dir = installLocation.getParentFile();
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    String message = Logging.getMessage("generic.CannotCreateFile", dir);
                    Logging.logger().warning(message);
                }
            }
        }
        String formatSuffix = params.getStringValue(AVKey.FORMAT_SUFFIX);
        DataRasterWriter[] writers = this.getDataRasterWriters();
        Object writer = this.findWriterFor(tileRaster, formatSuffix, installLocation, writers);
        if (writer instanceof DataRasterWriter) {
            try {
                ((DataRasterWriter) writer).write(tileRaster, formatSuffix, installLocation);
            } catch (java.io.IOException e) {
                String message = Logging.getMessage("generic.ExceptionWhileWriting", installLocation);
                Logging.logger().log(java.util.logging.Level.SEVERE, message, e);
            }
        }
    }

    protected Object installLocationForTile(AVList installParams, Tile tile) {
        String path = null;
        String s = installParams.getStringValue(AVKey.FILE_STORE_LOCATION);
        if (s != null) path = WWIO.appendPathPart(path, s);
        s = tile.getPath();
        if (s != null) path = WWIO.appendPathPart(path, s);
        if (path == null || path.length() < 1) return Logging.getMessage("TiledRasterProducer.InvalidTile", tile);
        return new java.io.File(path);
    }

    protected Object findWriterFor(DataRaster raster, String formatSuffix, java.io.File destination, DataRasterWriter[] writers) {
        for (DataRasterWriter writer : writers) {
            if (writer.canWrite(raster, formatSuffix, destination)) return writer;
        }
        return Logging.getMessage("DataRaster.CannotWrite", raster, formatSuffix, destination);
    }

    /**
     * Returns a configuration document which describes the tiled data produced by this TiledRasterProducer. The
     * document's contents are derived from the specified parameter list, and depend on the concrete subclass'
     * implementation. This returns null if the parameter list is null, or if the configuration document cannot be
     * created for any reason.
     *
     * @param params the parameters which describe the configuration document's contents.
     *
     * @return the configuration document, or null if the parameter list is null or does not contain the required
     *         parameters.
     */
    protected abstract Document createConfigDoc(AVList params);

    /**
     * Installs the configuration file which describes the tiled data produced by this TiledRasterProducer. The install
     * location, configuration filename, and configuration file contents are derived from the specified parameter list.
     * This throws an exception if the configuration file cannot be installed for any reason.
     * <p/>
     * The parameter list must contain <strong>at least</strong> the following keys: <table> <tr><th>Key</th></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#FILE_STORE_LOCATION}</td><td></td></tr> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#DATA_CACHE_NAME}</td><td></td></tr> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#DATASET_NAME}</td><td></td></tr> </table>
     *
     * @param params the parameters which describe the install location, the configuration filename, and the
     *               configuration file contents.
     *
     * @throws Exception                if the configuration file cannot be installed for any reason.
     * @throws IllegalArgumentException if the parameter list is null.
     */
    protected void installConfigFile(AVList params) throws Exception {
        if (params == null) {
            String message = Logging.getMessage("nullValue.ParametersIsNull");
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }
        if (this.isStopped()) return;
        File configFile = this.getConfigFileInstallLocation(params);
        if (configFile == null) {
            String message = Logging.getMessage("TiledRasterProducer.NoConfigFileInstallLocation", params.getValue(AVKey.DATASET_NAME));
            Logging.logger().severe(message);
            throw new WWRuntimeException(message);
        }
        synchronized (this.fileLock) {
            java.io.File dir = configFile.getParentFile();
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    String message = Logging.getMessage("generic.CannotCreateFile", dir);
                    Logging.logger().warning(message);
                }
            }
        }
        Document configDoc = this.createConfigDoc(params);
        if (configDoc == null) {
            String message = Logging.getMessage("TiledRasterProducer.CannotCreateConfigDoc", params.getValue(AVKey.DATASET_NAME));
            Logging.logger().severe(message);
            throw new WWRuntimeException(message);
        }
        try {
            WWXML.saveDocumentToFile(configDoc, configFile.getAbsolutePath());
        } catch (Exception e) {
            String message = Logging.getMessage("TiledRasterProducer.CannotWriteConfigFile", configFile);
            Logging.logger().severe(message);
            throw new WWRuntimeException(message);
        }
        this.getProductionResultsList().add(configDoc);
    }

    /**
     * Returns the location of the configuration file which describes the tiled data produced by this
     * TiledRasterProducer. The install location is derived from the specified parameter list. This returns null if the
     * parameter list is null, or if it does not contain any of the following keys: <table> <tr><th>Key</th></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#FILE_STORE_LOCATION}</td><td></td></tr> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#DATA_CACHE_NAME}</td><td></td></tr> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#DATASET_NAME}</td><td></td></tr> </table>
     *
     * @param params the parameters which describe the install location.
     *
     * @return the configuration file install location, or null if the parameter list is null or does not contain the
     *         required parameters.
     */
    protected File getConfigFileInstallLocation(AVList params) {
        if (params == null) return null;
        String fileStoreLocation = params.getStringValue(AVKey.FILE_STORE_LOCATION);
        if (fileStoreLocation != null) fileStoreLocation = WWIO.stripTrailingSeparator(fileStoreLocation);
        if (WWUtil.isEmpty(fileStoreLocation)) return null;
        String cacheName = DataConfigurationUtils.getDataConfigFilename(params, ".xml");
        if (cacheName != null) cacheName = WWIO.stripLeadingSeparator(cacheName);
        if (WWUtil.isEmpty(cacheName)) return null;
        return new File(fileStoreLocation + File.separator + cacheName);
    }

    protected void calculateTileCount(LevelSet levelSet, AVList params) {
        Sector sector = levelSet.getSector();
        this.tileCount = 0;
        for (Level level : levelSet.getLevels()) {
            Angle dLat = level.getTileDelta().getLatitude();
            Angle dLon = level.getTileDelta().getLongitude();
            Angle latOrigin = levelSet.getTileOrigin().getLatitude();
            Angle lonOrigin = levelSet.getTileOrigin().getLongitude();
            int firstRow = Tile.computeRow(dLat, sector.getMinLatitude(), latOrigin);
            int firstCol = Tile.computeColumn(dLon, sector.getMinLongitude(), lonOrigin);
            int lastRow = Tile.computeRow(dLat, sector.getMaxLatitude(), latOrigin);
            int lastCol = Tile.computeColumn(dLon, sector.getMaxLongitude(), lonOrigin);
            this.tileCount += (lastRow - firstRow + 1) * (lastCol - firstCol + 1);
            if (this.isFinalLevel(levelSet, level.getLevelNumber(), params)) break;
        }
    }

    protected void startProgress() {
        this.tile = 0;
        this.firePropertyChange(AVKey.PROGRESS, null, 0d);
    }

    protected void updateProgress() {
        double oldProgress = this.tile / (double) this.tileCount;
        double newProgress = ++this.tile / (double) this.tileCount;
        this.firePropertyChange(AVKey.PROGRESS, oldProgress, newProgress);
    }
}
