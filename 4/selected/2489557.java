package ch.epfl.lbd.io.writers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.data.shapefile.shp.ShapefileException;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

public class ShapeFileWriter extends Writer {

    protected org.geotools.data.shapefile.shp.ShapefileWriter shpWriter;

    protected File shp;

    protected File shx;

    protected FileOutputStream shpStream;

    protected FileOutputStream shxStream;

    protected String fileName;

    /**
	 * filename: file name without extension
	 * **/
    public ShapeFileWriter(String filename) {
        shp = new File(filename + ".shp");
        shx = new File(filename + ".shx");
        this.fileName = shp.getName();
        try {
            if (!shp.canWrite() || !shx.canWrite()) throw new IOException();
            shp.createNewFile();
            shx.createNewFile();
            this.shpStream = new FileOutputStream(shp);
            this.shxStream = new FileOutputStream(shx);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void open() {
        try {
            FileChannel shpChannel = shpStream.getChannel();
            FileChannel shxChannel = shxStream.getChannel();
            this.shpWriter = new org.geotools.data.shapefile.shp.ShapefileWriter(shpChannel, shxChannel);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.debug("Writable Shapefile " + this.fileName + " Opened.");
    }

    @Override
    public void write(Object[] input) {
        logger.debug("writing " + this.fileName + " ...");
        Geometry[] geometries = (Geometry[]) input;
        GeometryCollection coll = new GeometryCollection(geometries, new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4030));
        try {
            shpWriter.skipHeaders();
            shpWriter.write(coll, ShapeType.POLYGON);
        } catch (ShapefileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            shpWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.debug("Writable Shapefile " + this.fileName + " Closed.");
    }
}
