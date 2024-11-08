package com.iver.cit.gvsig.fmap.spatialindex;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import javax.imageio.stream.FileImageOutputStream;
import com.infomatiq.jsi.IntProcedure;
import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.rtree.RTree;

/**
 * Persistent spatial index which can resolve nearest neighbour queries.
 * <br>
 * 
 * To use:
 * 
 * PersistentRTreeJsi sptidx = new PersistentRtreeJsi("/home/kk");
 * if(sptidx.exists())
 *  sptidx.load();
 *  
 *  
 *  sptidx.add(rect, int);
 *  ...
 *  sptidx.add(rect2,int2);
 *  sptidx.flush();
 * 
 * @author azabala
 *
 */
public class PersistentRTreeJsi implements IPersistentSpatialIndex, INearestNeighbourFinder, ISpatialIndex {

    /**
	 * Spatial index in memory
	 */
    private RTree rtree;

    /**
	 * Spatial index file
	 */
    private File rtreeFile;

    private boolean hasChange = false;

    /**
	 * Spatial index file extension
	 */
    final String rExt = ".rix";

    private LinkedHashMap rectangles;

    /**
	 * Constructor
	 * @param file path of the spatial index file
	 * @throws SpatialIndexException 
	 */
    public PersistentRTreeJsi(String file, boolean overwrite) throws SpatialIndexException {
        rtree = new RTree();
        Properties props = new Properties();
        rtree.init(props);
        rtreeFile = new File(file + rExt);
        rectangles = new LinkedHashMap();
        if (!overwrite) load();
    }

    public void flush() {
        try {
            if (!hasChange) return;
            RandomAccessFile file = new RandomAccessFile(rtreeFile, "rw");
            FileImageOutputStream output = new FileImageOutputStream(file);
            output.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            int numShapes = rtree.size();
            output.writeInt(numShapes);
            Iterator iterator = rtree.iterator();
            int count = 0;
            while (iterator.hasNext()) {
                Integer idx = (Integer) iterator.next();
                Rectangle nr = (Rectangle) rectangles.get(idx);
                float xmin = nr.min[0];
                float ymin = nr.min[1];
                float xmax = nr.max[0];
                float ymax = nr.max[1];
                output.writeFloat(xmin);
                output.writeFloat(ymin);
                output.writeFloat(xmax);
                output.writeFloat(ymax);
                output.writeInt(idx.intValue());
                count++;
            }
            output.flush();
            output.close();
            file.close();
            hasChange = false;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean exists() {
        return rtreeFile.exists();
    }

    public void load() throws SpatialIndexException {
        try {
            if (!rtreeFile.exists()) {
                return;
            }
            RandomAccessFile file = new RandomAccessFile(rtreeFile, "r");
            FileChannel channel = file.getChannel();
            MappedByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            buf.order(ByteOrder.LITTLE_ENDIAN);
            int numShapes = buf.getInt();
            for (int i = 0; i < numShapes; i++) {
                float xmin, ymin, xmax, ymax;
                int shapeIndex;
                xmin = buf.getFloat();
                ymin = buf.getFloat();
                xmax = buf.getFloat();
                ymax = buf.getFloat();
                shapeIndex = buf.getInt();
                Rectangle jsiRect = new Rectangle(xmin, ymin, xmax, ymax);
                rtree.add(jsiRect, shapeIndex);
            }
        } catch (Exception e) {
            throw new SpatialIndexException(e);
        }
    }

    public void close() {
        rectangles.clear();
        rectangles = null;
    }

    class ListIntProcedure implements IntProcedure {

        ArrayList solution = new ArrayList();

        public boolean execute(int arg0) {
            solution.add(new Integer(arg0));
            return true;
        }

        public List getSolution() {
            return solution;
        }
    }

    public List query(Rectangle2D rect) {
        ListIntProcedure solution = new ListIntProcedure();
        rtree.intersects(toJsiRect(rect), solution);
        return solution.getSolution();
    }

    public void insert(Rectangle2D rect, int index) {
        Rectangle jsiRect = toJsiRect(rect);
        rtree.add(jsiRect, index);
        rectangles.put(new Integer(index), jsiRect);
        hasChange = true;
    }

    public void delete(Rectangle2D rect, int index) {
        rtree.delete(toJsiRect(rect), index);
        rectangles.remove(new Integer(index));
        hasChange = true;
    }

    public List findNNearest(int numberOfNearest, Rectangle2D rect) {
        return (List) rtree.nearest(toJsiRect(rect), numberOfNearest);
    }

    public List findNNearest(int numberOfNearest, Point2D point) {
        com.infomatiq.jsi.Point jsiPoint = new com.infomatiq.jsi.Point((float) point.getX(), (float) point.getY());
        return (List) rtree.nearest(jsiPoint, numberOfNearest);
    }

    private Rectangle toJsiRect(Rectangle2D rect) {
        Rectangle jsiRect = new Rectangle((float) rect.getMinX(), (float) rect.getMinY(), (float) rect.getMaxX(), (float) rect.getMaxY());
        return jsiRect;
    }

    public int size() {
        return rtree.size();
    }
}
