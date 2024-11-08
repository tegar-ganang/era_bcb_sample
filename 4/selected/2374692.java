package com.hardcode.gdbms.driver.shapefile;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import com.hardcode.gdbms.driver.exceptions.CloseDriverException;
import com.hardcode.gdbms.driver.exceptions.FileNotFoundDriverException;
import com.hardcode.gdbms.driver.exceptions.OpenDriverException;
import com.hardcode.gdbms.driver.exceptions.ReadDriverException;
import com.hardcode.gdbms.engine.spatial.GeneralPath;

/**
 *
 */
public class SHP {

    private static final int NULL = 0;

    private static final int POINT = 1;

    private static final int MULTIPOINT = 8;

    private static final int POLYLINE = 3;

    private static final int POLYGON = 5;

    private FileChannel channel;

    private FileChannel indexChannel;

    private FileInputStream fis;

    private FileInputStream indexFis;

    private Rectangle2D extent;

    /**
     * DOCUMENT ME!
     *
     * @param file DOCUMENT ME!
     * @param prefix DOCUMENT ME!
     * @throws ReadDriverException TODO
     * @throws OpenDriverException
     */
    public void open(File file, String prefix) throws OpenDriverException {
        File indexFile = new File(prefix + ".shx");
        try {
            indexFis = new FileInputStream(indexFile);
            indexChannel = indexFis.getChannel();
            fis = new FileInputStream(file);
            channel = fis.getChannel();
            ByteBuffer headBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, 100);
            headBuffer.order(ByteOrder.LITTLE_ENDIAN);
            extent = new Rectangle2D.Double(headBuffer.getDouble(36), headBuffer.getDouble(44), headBuffer.getDouble(52) - headBuffer.getDouble(36), headBuffer.getDouble(60) - headBuffer.getDouble(44));
            headBuffer = null;
        } catch (FileNotFoundException e) {
            throw new FileNotFoundDriverException(file.getName(), e, indexFile.getAbsolutePath());
        } catch (IOException e) {
            throw new OpenDriverException(file.getName(), e);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    public void close() throws CloseDriverException {
        try {
            channel.close();
            fis.close();
        } catch (IOException e) {
            throw new CloseDriverException("", e);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param recordPosition DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    private int getRecordLength(int recordPosition) throws IOException {
        ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, recordPosition, 8);
        buffer.order(ByteOrder.BIG_ENDIAN);
        return buffer.getInt(4) * 2;
    }

    /**
     * DOCUMENT ME!
     *
     * @param recordIndex DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    private int getRecordPosition(int recordIndex) throws IOException {
        int indexPos = 100 + (recordIndex * 8);
        ByteBuffer indexBuffer = indexChannel.map(FileChannel.MapMode.READ_ONLY, indexPos, 8);
        indexBuffer.order(ByteOrder.BIG_ENDIAN);
        return indexBuffer.getInt(0) * 2;
    }

    /**
     * DOCUMENT ME!
     *
     * @param buffer DOCUMENT ME!
     * @param index DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public double[] getPoint(ByteBuffer buffer, int index) {
        return new double[] { buffer.getDouble(index), buffer.getDouble(index + 8) };
    }

    /**
     * DOCUMENT ME!
     *
     * @param index DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    public GeneralPath getGeometry(int index) throws IOException {
        int recordPosition = getRecordPosition(index);
        int recordLenght = getRecordLength(recordPosition);
        ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, recordPosition + 8, recordLenght);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int shapeType = buffer.getInt(0);
        GeneralPath ret = new GeneralPath();
        double[] point;
        switch(shapeType) {
            case NULL:
                return null;
            case POINT:
                point = getPoint(buffer, 4);
                ret.moveTo(point[0], point[1]);
                return ret;
            case MULTIPOINT:
                int nPoints = buffer.getInt(32);
                for (int i = 0; i < nPoints; i++) {
                    point = getPoint(buffer, 40 + (16 * i));
                    ret.moveTo(point[0], point[1]);
                }
                return ret;
            case POLYLINE:
                int nParts = buffer.getInt(36);
                int[] parts = new int[nParts + 1];
                for (int i = 0; i < nParts; i++) {
                    parts[i] = buffer.getInt(44 + (i * 4));
                }
                parts[nParts] = recordLenght;
                int offset = 44 + nParts * 4;
                for (int i = 0; i < nParts; i++) {
                    int pointIndex = offset + parts[i];
                    point = getPoint(buffer, pointIndex);
                    ret.moveTo(point[0], point[1]);
                    pointIndex += 16;
                    while (pointIndex < parts[i + 1]) {
                        point = getPoint(buffer, pointIndex);
                        ret.lineTo(point[0], point[1]);
                        pointIndex += 16;
                    }
                }
                return ret;
            case POLYGON:
                nParts = buffer.getInt(36);
                parts = new int[nParts + 1];
                for (int i = 0; i < nParts; i++) {
                    parts[i] = buffer.getInt(44 + (i * 4));
                }
                parts[nParts] = recordLenght;
                offset = 44 + nParts * 4;
                for (int i = 0; i < nParts; i++) {
                    int pointIndex = offset + parts[i];
                    point = getPoint(buffer, pointIndex);
                    ret.moveTo(point[0], point[1]);
                    pointIndex += 16;
                    while (pointIndex < parts[i + 1]) {
                        point = getPoint(buffer, pointIndex);
                        ret.lineTo(point[0], point[1]);
                        pointIndex += 16;
                    }
                }
                ret.closePath();
                return ret;
            default:
                throw new RuntimeException("Unknown geometry type");
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public Rectangle2D getExtent() {
        return extent;
    }
}
