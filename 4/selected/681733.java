package us.jonesrychtar.gispatialnet.Writer;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import us.jonesrychtar.gispatialnet.DataSet;

/**
 * @author sctyler
 *
 */
public class SimpleShapefileWriter {

    private File nodeFile = new File("nodes.shp");

    private File edgeFile = new File("edges.shp");

    private DataSet ds;

    private String nodeSchemaString = "location:Point:srid=4326,Node_ID:String";

    private String nodeSchemaName = "PersonWithID";

    private String edgeSchemaString = "*l:LineString,From:String,To:String";

    private String edgeSchemaName = "EdgesWithConnections";

    private List<Integer> attList;

    private int attCount = 1;

    private int idCol = 0;

    /**
	 * @param nodeFile
	 * @param edgeFile
	 * @param ds
	 */
    public SimpleShapefileWriter(File simpleFile, DataSet ds) {
        this.nodeFile = simpleFile;
        this.edgeFile = simpleFile;
        this.ds = ds;
        this.attCount = (int) (ds.getAttb().getColumnCount() + 1);
    }

    public SimpleShapefileWriter(File nodeFile, File edgeFile, DataSet ds) {
        this.nodeFile = nodeFile;
        this.edgeFile = edgeFile;
        this.ds = ds;
        this.attCount = (int) (ds.getAttb().getColumnCount() + 1);
    }

    public SimpleShapefileWriter(String filenames, DataSet ds) {
        this.nodeFile = new File(filenames + ".shp");
        this.edgeFile = new File(filenames + "_edges.shp");
        this.ds = ds;
        this.attCount = (int) (ds.getAttb().getColumnCount() + 1);
    }

    public SimpleShapefileWriter(String nodeFile, String edgeFile, DataSet ds) {
        this.nodeFile = new File(nodeFile + ".shp");
        this.edgeFile = new File(edgeFile + ".shp");
        this.ds = ds;
        this.attCount = (int) (ds.getAttb().getColumnCount() + 1);
    }

    /**
	 * @return the attList
	 */
    public List<Integer> getAttList() {
        return attList;
    }

    /**
	 * @param attList the attList to set
	 */
    public void setAttList(List<Integer> attList) {
        this.attList = attList;
    }

    /**
	 * @return the attCount
	 */
    public int getAttCount() {
        return attCount;
    }

    /**
	 * @param attCount the attCount to set
	 */
    public void setAttCount(int attCount) {
        this.attCount = attCount;
    }

    /**
	 * @return the nodeFile
	 */
    public File getNodeFile() {
        return nodeFile;
    }

    /**
	 * @param nodeFile the nodeFile to set
	 */
    public void setNodeFile(File nodeFile) {
        this.nodeFile = nodeFile;
    }

    /**
	 * @return the edgeFile
	 */
    public File getEdgeFile() {
        return edgeFile;
    }

    /**
	 * @param edgeFile the edgeFile to set
	 */
    public void setEdgeFile(File edgeFile) {
        this.edgeFile = edgeFile;
    }

    /**
	 * @return the ds
	 */
    public DataSet getData() {
        return ds;
    }

    /**
	 * @param ds the ds to set
	 */
    public void setData(DataSet ds) {
        this.ds = ds;
    }

    public void writeEdges() {
        try {
            final SimpleFeatureType TYPE = DataUtilities.createType(this.edgeSchemaName, this.edgeSchemaString);
            FeatureCollection collection = FeatureCollections.newCollection();
            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
            for (int row = 0; row < this.ds.getX().getRowCount(); row++) {
                double fromX = this.ds.getX().getAsDouble(row, 0);
                double fromY = this.ds.getY().getAsDouble(row, 0);
                for (int col = 0; col < ds.getAdj().getColumnCount(); col++) {
                    if (ds.getAdj().getAsDouble(row, col) >= 0) {
                        double toX = this.ds.getX().getAsDouble(col, 0);
                        double toY = this.ds.getY().getAsDouble(col, 0);
                        System.out.println(fromX + "," + fromY + " -> " + toX + "," + toY);
                        LineString line = geometryFactory.createLineString(new Coordinate[] { new Coordinate(fromX, fromY), new Coordinate(toX, toY) });
                        featureBuilder.add(line);
                        featureBuilder.add(ds.getAttb().getRowLabel(row));
                        featureBuilder.add(ds.getAttb().getRowLabel(col));
                        SimpleFeature feature = featureBuilder.buildFeature(null);
                        collection.add(feature);
                    }
                }
            }
            DataStoreFactorySpi dataStoreFactory = new ShapefileDataStoreFactory();
            Map<String, Serializable> params = new HashMap<String, Serializable>();
            params.put("url", edgeFile.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);
            ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
            newDataStore.createSchema(TYPE);
            newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
            Transaction transaction = new DefaultTransaction("create");
            String typeName = newDataStore.getTypeNames()[0];
            FeatureSource featureSource = newDataStore.getFeatureSource(typeName);
            if (featureSource instanceof FeatureStore) {
                FeatureStore featureStore = (FeatureStore) featureSource;
                featureStore.setTransaction(transaction);
                try {
                    featureStore.addFeatures(collection);
                    transaction.commit();
                } catch (Exception problem) {
                    problem.printStackTrace();
                    transaction.rollback();
                } finally {
                    transaction.close();
                }
            } else {
                System.out.println(typeName + " does not support read/write access");
            }
        } catch (Exception e) {
            System.out.println("Cannot create edge features:\n" + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    public void writeNodes() {
        try {
            final SimpleFeatureType TYPE = DataUtilities.createType(this.nodeSchemaName, this.nodeSchemaString);
            FeatureCollection collection = FeatureCollections.newCollection();
            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
            for (int row = 0; row < this.ds.getX().getRowCount(); row++) {
                double xcoord = this.ds.getX().getAsDouble(row, 0);
                double ycoord = this.ds.getY().getAsDouble(row, 0);
                String name = this.ds.getAttb().getAsString(row, 0);
                Point point = geometryFactory.createPoint(new Coordinate(xcoord, ycoord));
                featureBuilder.add(point);
                featureBuilder.add(name);
                SimpleFeature feature = featureBuilder.buildFeature(null);
                collection.add(feature);
            }
            DataStoreFactorySpi dataStoreFactory = new ShapefileDataStoreFactory();
            Map<String, Serializable> params = new HashMap<String, Serializable>();
            params.put("url", nodeFile.toURI().toURL());
            params.put("create spatial index", Boolean.TRUE);
            ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
            newDataStore.createSchema(TYPE);
            newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
            Transaction transaction = new DefaultTransaction("create");
            String typeName = newDataStore.getTypeNames()[0];
            FeatureSource featureSource = newDataStore.getFeatureSource(typeName);
            if (featureSource instanceof FeatureStore) {
                FeatureStore featureStore = (FeatureStore) featureSource;
                featureStore.setTransaction(transaction);
                try {
                    featureStore.addFeatures(collection);
                    transaction.commit();
                } catch (Exception problem) {
                    problem.printStackTrace();
                    transaction.rollback();
                } finally {
                    transaction.close();
                }
            } else {
                System.out.println(typeName + " does not support read/write access");
            }
        } catch (Exception e) {
            System.out.println("Cannot create features!\n" + e.getMessage());
        }
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        DataSet d = new DataSet();
    }
}
