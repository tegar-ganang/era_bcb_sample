package com.moesol.maps.server.tms;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * Simple disk tile cache implementation.
 */
public class DiskTileCache implements ITileImageCache {

    /**
	 * Tile caching strategy if read and write to baseDirectory is not allowed.
	 * Allows the avoidance of continual conditional statements to determine
	 * if the access is allowed.
	 */
    private class NoOpTileCacheImpl implements ITileImageCache {

        @Override
        public BufferedImage getImage(String layerId, int level, int x, int y) {
            return null;
        }

        @Override
        public void addImage(String layerId, int level, int x, int y, BufferedImage image) {
        }
    }

    /**
	 * Tile caching strategy if read and write to baseDirectory is allowed.
	 */
    private class DiskTileCacheImpl implements ITileImageCache {

        @Override
        public BufferedImage getImage(String layerId, int level, int x, int y) {
            File imageFile = getImageFile(layerId, level, x, y);
            if (imageFile.exists()) {
                try {
                    return ImageIO.read(imageFile);
                } catch (Throwable e) {
                    LOGGER.log(Level.WARNING, "Failed reading image file from disk", e);
                }
            }
            return null;
        }

        private File getImageFile(String layerId, int level, int x, int y) {
            String filepath = m_baseDirectory + "/" + layerId + "/" + level + "/" + x + "/" + y + ".png";
            return new File(filepath);
        }

        @Override
        public void addImage(String layerId, int level, int x, int y, BufferedImage image) {
            File imageFile = getImageFile(layerId, level, x, y);
            if (!imageFile.getParentFile().exists()) {
                imageFile.mkdirs();
            }
            try {
                ImageIO.write(image, m_imageFormat, imageFile);
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "Failed writing image to disk", t);
            }
        }
    }

    private static final Logger LOGGER = Logger.getLogger(DiskTileCache.class.getName());

    private String m_baseDirectory;

    private String m_imageFormat;

    private ITileImageCache m_impl;

    public DiskTileCache(String baseDirectory, String imageFormat) {
        m_baseDirectory = baseDirectory;
        m_imageFormat = imageFormat;
        if (hasAccessToBaseDirectory(baseDirectory)) {
            m_impl = new DiskTileCacheImpl();
            LOGGER.log(Level.INFO, "Caching tiles at '" + baseDirectory + "'");
        } else {
            m_impl = new NoOpTileCacheImpl();
            LOGGER.log(Level.SEVERE, "Unable to read and write directory '" + baseDirectory + "'");
        }
    }

    private boolean hasAccessToBaseDirectory(String directory) {
        try {
            File dir = new File(directory);
            return dir.canRead() && dir.canWrite();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public BufferedImage getImage(String layerId, int level, int x, int y) {
        return m_impl.getImage(layerId, level, x, y);
    }

    @Override
    public void addImage(String layerId, int level, int x, int y, BufferedImage image) {
        m_impl.addImage(layerId, level, x, y, image);
    }
}
