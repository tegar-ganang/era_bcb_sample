package org.jdesktop.swingx.mapviewer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.jdesktop.swingx.mapviewer.util.GeoUtil;
import org.jdesktop.swingx.util.PaintUtils;

/**
 * The <code>AbstractTileFactory</code> provides a basic implementation for the TileFactory.
 */
public abstract class AbstractTileFactory extends TileFactory {

    private static final Logger LOG = Logger.getLogger(AbstractTileFactory.class.getName());

    /**
     * Creates a new instance of DefaultTileFactory using the spcified TileFactoryInfo
     * @param info a TileFactoryInfo to configure this TileFactory
     */
    public AbstractTileFactory(TileFactoryInfo info) {
        super(info);
    }

    private int threadPoolSize = 4;

    private ExecutorService service;

    private Map<String, Tile> tileMap = new HashMap<String, Tile>();

    private TileCache cache = new TileCache();

    /**
     * Returns the tile that is located at the given tilePoint for this zoom. For example,
     * if getMapSize() returns 10x20 for this zoom, and the tilePoint is (3,5), then the
     * appropriate tile will be located and returned.
     * @param tilePoint
     * @param zoom
     * @return
     */
    public void clearTileCache() {
        cache = new TileCache();
        tileMap = new HashMap<String, Tile>();
    }

    public Tile getTile(int x, int y, int zoom) {
        return getTile(x, y, zoom, true);
    }

    private Tile getTile(int tpx, int tpy, int zoom, boolean eagerLoad) {
        int tileX = tpx;
        int numTilesWide = (int) getMapSize(zoom).getWidth();
        if (tileX < 0) {
            tileX = numTilesWide - (Math.abs(tileX) % numTilesWide);
        }
        tileX = tileX % numTilesWide;
        int tileY = tpy;
        String url = getInfo().getTileUrl(tileX, tileY, zoom);
        Tile.Priority pri = Tile.Priority.High;
        if (!eagerLoad) {
            pri = Tile.Priority.Low;
        }
        Tile tile = null;
        if (!tileMap.containsKey(url)) {
            if (!GeoUtil.isValidTile(tileX, tileY, zoom, getInfo())) {
                tile = new Tile(tileX, tileY, zoom);
            } else {
                tile = new Tile(tileX, tileY, zoom, url, pri, this);
                startLoading(tile);
            }
            tileMap.put(url, tile);
        } else {
            tile = tileMap.get(url);
            if (tile.getPriority() == Tile.Priority.Low && eagerLoad && !tile.isLoaded()) {
                promote(tile);
            }
        }
        return tile;
    }

    public TileCache getTileCache() {
        return cache;
    }

    public void setTileCache(TileCache cache) {
        this.cache = cache;
    }

    /**
     * Thread pool for loading the tiles
     */
    private static BlockingQueue<Tile> tileQueue = new PriorityBlockingQueue<Tile>(5, new Comparator<Tile>() {

        public int compare(Tile o1, Tile o2) {
            if (o1.getPriority() == Tile.Priority.Low && o2.getPriority() == Tile.Priority.High) {
                return 1;
            }
            if (o1.getPriority() == Tile.Priority.High && o2.getPriority() == Tile.Priority.Low) {
                return -1;
            }
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this;
        }
    });

    /**
     * Subclasses may override this method to provide their own executor services. This 
     * method will be called each time a tile needs to be loaded. Implementations should 
     * cache the ExecutorService when possible.
     * @return ExecutorService to load tiles with
     */
    protected synchronized ExecutorService getService() {
        if (service == null) {
            service = Executors.newFixedThreadPool(threadPoolSize, new ThreadFactory() {

                private int count = 0;

                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "tile-pool-" + count++);
                    t.setPriority(Thread.MIN_PRIORITY);
                    t.setDaemon(true);
                    return t;
                }
            });
        }
        return service;
    }

    /**
     * Set the number of threads to use for loading the tiles. This controls the number of threads
     * used by the ExecutorService returned from getService(). Note, this method should
     * be called before loading the first tile. Calls after the first tile are loaded will
     * have no effect by default.
     * @param size 
     */
    public void setThreadPoolSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size invalid: " + size + ". The size of the threadpool must be greater than 0.");
        }
        threadPoolSize = size;
    }

    @SuppressWarnings("unchecked")
    protected synchronized void startLoading(Tile tile) {
        if (tile.isLoading()) {
            System.out.println("already loading. bailing");
            return;
        }
        tile.setLoading(true);
        try {
            tileQueue.put(tile);
            getService().submit(createTileRunner(tile));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Subclasses can override this if they need custom TileRunners for some reason
     * @return
     */
    protected Runnable createTileRunner(Tile tile) {
        return new TileRunner();
    }

    /**
     * Increase the priority of this tile so it will be loaded sooner.
     */
    public synchronized void promote(Tile tile) {
        if (tileQueue.contains(tile)) {
            try {
                tileQueue.remove(tile);
                tile.setPriority(Tile.Priority.High);
                tileQueue.put(tile);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * An inner class which actually loads the tiles. Used by the thread queue. Subclasses
     * can override this if necessary.
     */
    private class TileRunner implements Runnable {

        /**
         * Gets the full URI of a tile.
         * @param tile
         * @throws java.net.URISyntaxException
         * @return
         */
        protected URI getURI(Tile tile) throws URISyntaxException {
            if (tile.getURL() == null) {
                return null;
            }
            return new URI(tile.getURL());
        }

        /**
         * implementation of the Runnable interface.
         */
        public void run() {
            final Tile tile = tileQueue.remove();
            int trys = 3;
            while (!tile.isLoaded() && trys > 0) {
                try {
                    BufferedImage img = null;
                    URI uri = getURI(tile);
                    img = cache.get(uri);
                    if (img == null) {
                        byte[] bimg = cacheInputStream(uri.toURL());
                        img = PaintUtils.loadCompatibleImage(new ByteArrayInputStream(bimg));
                        cache.put(uri, bimg, img);
                        img = cache.get(uri);
                    }
                    if (img == null) {
                        System.out.println("error loading: " + uri);
                        LOG.log(Level.INFO, "Failed to load: " + uri);
                        trys--;
                    } else {
                        final BufferedImage i = img;
                        SwingUtilities.invokeAndWait(new Runnable() {

                            public void run() {
                                tile.image = new SoftReference<BufferedImage>(i);
                                tile.setLoaded(true);
                            }
                        });
                    }
                } catch (OutOfMemoryError memErr) {
                    cache.needMoreMemory();
                } catch (Throwable e) {
                    LOG.log(Level.SEVERE, "Failed to load a tile at url: " + tile.getURL() + ", retrying", e);
                    System.err.println("Failed to load a tile at url: " + tile.getURL());
                    e.printStackTrace();
                    Object oldError = tile.getError();
                    tile.setError(e);
                    tile.firePropertyChangeOnEDT("loadingError", oldError, e);
                    if (trys == 0) {
                        tile.firePropertyChangeOnEDT("unrecoverableError", null, e);
                    } else {
                        trys--;
                    }
                }
            }
            tile.setLoading(false);
        }

        private byte[] cacheInputStream(URL url) throws IOException {
            InputStream ins = url.openStream();
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            byte[] buf = new byte[256];
            while (true) {
                int n = ins.read(buf);
                if (n == -1) break;
                bout.write(buf, 0, n);
            }
            return bout.toByteArray();
        }
    }
}
