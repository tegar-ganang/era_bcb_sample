package org.jaitools.tiledimage;

import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests of DiskMemTilesImage: writing and retrieving data
 * at the tile level
 *
 * @author Michael Bedward
 * @since 1.0
 * @version $Id: TileWritingTest.java 1711 2011-06-16 07:41:42Z michael.bedward $
 */
public class TileWritingTest extends TiledImageTestBase {

    private static final int TILE_WIDTH = 128;

    private static final int XTILES = 5;

    private static final int YTILES = 3;

    private DiskMemImage image;

    @Before
    public void setUp() {
        image = makeImage(TILE_WIDTH, XTILES, YTILES);
    }

    @Test
    public void testTileWriting() {
        System.out.println("   read/write with individual tiles");
        int numBands = image.getNumBands();
        int[] data = new int[numBands];
        for (int i = 0; i < numBands; i++) {
            data[i] = i + 1;
        }
        for (int y = image.getMinTileY(); y < image.getMaxTileY(); y++) {
            int py = TILE_WIDTH * y;
            for (int x = image.getMinTileX(); x < image.getMaxTileX(); x++) {
                int px = TILE_WIDTH * x;
                WritableRaster tile = image.getWritableTile(x, y);
                tile.setPixel(px, py, data);
                image.releaseWritableTile(x, y);
            }
        }
        int[] tileData = new int[numBands];
        for (int y = image.getMinTileY(); y < image.getMaxTileY(); y++) {
            int py = TILE_WIDTH * y;
            for (int x = image.getMinTileX(); x < image.getMaxTileX(); x++) {
                int px = TILE_WIDTH * x;
                Raster tile = image.getTile(x, y);
                tile.getPixel(px, py, tileData);
                for (int i = 0; i < numBands; i++) {
                    assertTrue(tileData[i] == data[i]);
                }
            }
        }
    }
}
