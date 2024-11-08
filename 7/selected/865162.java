package com.javagl4kids.learning.movingplan;

import java.awt.Color;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;

/**
 * Band of points to draw with GL_QUAD_STRIP mode.
 * 
 * | 5----6 | 
 * | 3----4 | 
 * | 1----2 |
 * 
 * @author alaloop
 * 
 */
public class Band3D {

    /**
	 * The Z to use for the first tile of a band
	 */
    public static final float Z0 = 12;

    /**
	 * The size along Z of the tiles
	 */
    public static final float Z_SIZE = 1.0f;

    /**
	 * The size along X of the tiles
	 */
    public static final float X_SIZE = 1.0f;

    /**
	 * The number of displayed tiles
	 */
    private int size;

    /**
	 * The band at the left
	 */
    private Band3D leftBand = null;

    /**
	 * The band at the right
	 */
    private Band3D rightBand = null;

    /**
	 * Tiles array to be displayed 
	 */
    private Tile[] tiles = null;

    /**
	 * Build a new band3D with parameters
	 * @param size
	 */
    public Band3D(int size) {
        this.size = size;
        this.tiles = new Tile[size];
    }

    /**
	 * Paint the band
	 * @param gLDrawable
	 */
    public void paint(GLAutoDrawable gLDrawable) {
        boolean previousWasEmpty;
        if (tiles != null) {
            final GL gl = gLDrawable.getGL();
            gl.glBegin(GL.GL_QUAD_STRIP);
            previousWasEmpty = false;
            for (int i = 0; i < (size - 1); i++) {
                if (tiles[i].getType().equals(TileType.EMPTY)) {
                    if (!previousWasEmpty) gl.glEnd();
                    previousWasEmpty = true;
                    continue;
                }
                if (previousWasEmpty) gl.glBegin(GL.GL_QUAD_STRIP);
                gl.glColor3f(tiles[i].getRedf(), tiles[i].getGreenf(), tiles[i].getBluef());
                gl.glVertex3d(tiles[i].getPt1().x, tiles[i].getPt1().y, tiles[i].getPt1().z);
                gl.glVertex3d(tiles[i].getPt2().x, tiles[i].getPt2().y, tiles[i].getPt2().z);
                previousWasEmpty = false;
            }
            gl.glVertex3d(tiles[size - 1].getPt3().x, tiles[size - 1].getPt3().y, tiles[size - 1].getPt3().z);
            gl.glVertex3d(tiles[size - 1].getPt4().x, tiles[size - 1].getPt4().y, tiles[size - 1].getPt4().z);
            gl.glEnd();
        }
    }

    /**
	 * Shift all lines erasing the first first one
	 */
    public void shiftLines() {
        int trueNb = tiles.length;
        for (int i = 0; i < (trueNb - 1); i++) {
            tiles[i] = tiles[i + 1];
        }
    }

    /**
	 * Given a x,z coordinate, return the y of the tile at this point
	 * @param z
	 * @return
	 */
    public float getYTileForZ(float x, float z) {
        DPoint3D pt = null;
        DPoint3D ptNext = null;
        for (int i = 0; i < (size - 1); i++) {
            pt = tiles[i].getPt1();
            ptNext = tiles[i].getPt3();
            if (z <= pt.z && z >= ptNext.z) {
                if (tiles[i].getType().equals(TileType.EMPTY)) return -1000;
                return (float) pt.y;
            }
        }
        return -1;
    }

    /**
	 * String representation of a band
	 * @see java.lang.Object#toString()
	 */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        return buf.toString();
    }

    public Band3D getLeftBand() {
        return leftBand;
    }

    public void setLeftBand(Band3D leftBand) {
        this.leftBand = leftBand;
    }

    public Band3D getRightBand() {
        return rightBand;
    }

    public void setRightBand(Band3D rightBand) {
        this.rightBand = rightBand;
    }

    public Tile[] getTiles() {
        return tiles;
    }

    public void setTiles(Tile[] tiles) {
        this.tiles = tiles;
    }
}
