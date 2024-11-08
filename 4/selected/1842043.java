package geovista.readers.shapefile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;
import org.geotools.data.shapefile.Lock;
import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileWriter;
import org.geotools.data.shapefile.shp.JTSUtilities;
import org.geotools.data.shapefile.shp.ShapeType;
import org.geotools.data.shapefile.shp.ShapefileWriter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import geovista.common.data.DataSetForApps;
import geovista.readers.example.GeoDataGeneralizedStates;

/**
 * Takes a file name and a set of shapes, and writes out a shapefile.
 * 
 * also see DBaseFile, ShapeFile
 * 
 */
public class ShapeFileDataWriter {

    protected static final Logger logger = Logger.getLogger(ShapeFileDataWriter.class.getName());

    public static void writeShapefile(Geometry[] paths, String fileNameRoot) {
        if (paths == null) {
            logger.severe("can't write null paths!");
            return;
        }
        try {
            GeometryFactory geomFact = new GeometryFactory();
            GeometryCollection geoms = null;
            geoms = new GeometryCollection(paths, geomFact);
            File shp = new File(fileNameRoot + ".shp");
            File shx = new File(fileNameRoot + ".shx");
            FileOutputStream shpStream = new FileOutputStream(shp);
            FileOutputStream shxStream = new FileOutputStream(shx);
            FileChannel shpChan = shpStream.getChannel();
            FileChannel shxChan = shxStream.getChannel();
            ShapefileWriter writer = new ShapefileWriter(shpChan, shxChan, new Lock());
            ShapeType bestType = JTSUtilities.getShapeType(paths[0], 2);
            writer.write(geoms, bestType);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int findMaxLeftSide(double[] array) {
        int len = 0;
        for (double num : array) {
            double leftNum = Math.floor(num);
            String numString = String.valueOf(leftNum);
            if (numString.length() > len) {
                len = numString.length();
            }
        }
        return len;
    }

    public static int findMaxStringLen(String[] array) {
        int len = 0;
        for (String str : array) {
            if (str.length() > len) {
                len = str.length();
            }
        }
        return len;
    }

    public static void writeDBFile(String[] columnNames, Object[] data, int nRecords, String fileNameRoot) {
        try {
            DbaseFileHeader header = new DbaseFileHeader();
            header.setNumRecords(nRecords);
            for (int i = 0; i < columnNames.length; i++) {
                Object array = data[i];
                String name = columnNames[i];
                if (array instanceof double[]) {
                    header.addColumn(name, 'N', 20, 4);
                } else if (array instanceof String[]) {
                    header.addColumn(name, 'C', findMaxStringLen((String[]) array), 0);
                } else if (array instanceof int[]) {
                    header.addColumn(name, 'N', 20, 0);
                } else {
                    logger.severe("hit unknown array type, " + array.getClass().getName());
                }
            }
            File dbf = new File(fileNameRoot + ".dbf");
            FileOutputStream dbfStream = new FileOutputStream(dbf);
            FileChannel dbfChan = dbfStream.getChannel();
            DbaseFileWriter writer = new DbaseFileWriter(header, dbfChan);
            Object[] record = new Object[columnNames.length];
            for (int i = 0; i < nRecords; i++) {
                for (int j = 0; j < record.length; j++) {
                    Object array = data[j];
                    if (array instanceof double[]) {
                        record[j] = new Double(((double[]) array)[i]);
                    } else if (array instanceof String[]) {
                        record[j] = ((String[]) array)[i];
                    } else if (array instanceof int[]) {
                        record[j] = new Integer(((int[]) array)[i]);
                    } else {
                        logger.severe("hit unknown array type, " + array.getClass().getName());
                    }
                }
                writer.write(record);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeDataSetForApps(DataSetForApps dsa, String fileName) {
        String[] varNames = dsa.getAttributeNamesOriginal();
        Object[] data = dsa.getNamedArrays();
        writeDBFile(varNames, data, dsa.getNumObservations(), fileName);
        ShapeFileDataWriter.writeShapefile(dsa.getGeomData(), fileName);
    }

    public static void main(String[] args) {
        GeoDataGeneralizedStates stateData = new GeoDataGeneralizedStates();
        String fileName = "C:\\temp\\test2";
        writeDataSetForApps(stateData.getDataForApps(), fileName);
    }
}
