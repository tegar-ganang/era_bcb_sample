package org.ala.rest;

import com.vividsolutions.jts.geom.Point;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeatureType;
import org.vfny.geoserver.util.DataStoreUtils;

/**
 *
 * @author angus
 */
public class Intersector {

    GazetteerConfig gc = new GazetteerConfig();

    GeoServer gs = GeoServerExtensions.bean(GeoServer.class);

    ServletContext sc = GeoServerExtensions.bean(ServletContext.class);

    Catalog catalog = gs.getCatalog();

    private static final Logger logger = Logging.getLogger("org.ala.rest.GazetteerFeature");

    private List<FeatureSource> featureSources = new ArrayList();

    private List<String> layers;

    private List<String> shapeFiles = new ArrayList();

    public Intersector(List<String> layers) {
        this.layers = layers;
    }

    public List<String[]> intersect(List<Point> points) {
        List<String[]> resultSet = new ArrayList();
        System.out.println("Intersecting points ...");
        int threadCount = 1;
        int step = points.size() / threadCount;
        if (step == 0) {
            step = 1;
            threadCount = points.size();
        }
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<IntersectThread> threads = new ArrayList();
        for (int i = 0; i < threadCount; i++) {
            String[] results = new String[step];
            IntersectThread it = new IntersectThread(points.subList(i * step, ((i + 1) * step)), layers, results, latch);
            threads.add(it);
            it.start();
        }
        try {
            latch.await();
        } catch (InterruptedException E) {
        }
        for (IntersectThread it : threads) {
            resultSet.add(it.results);
        }
        return resultSet;
    }

    private String createShapeFile(String fileName, FeatureSource layer) throws IOException, Exception {
        FeatureCollection featureCollection = layer.getFeatures();
        File shapeFile = new File(fileName);
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("url", shapeFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);
        ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
        newDataStore.createSchema((SimpleFeatureType) layer.getSchema());
        newDataStore.forceSchemaCRS(DefaultGeographicCRS.WGS84);
        Transaction transaction = new DefaultTransaction("create");
        String typeName = newDataStore.getTypeNames()[0];
        FeatureSource shapeSource = newDataStore.getFeatureSource(typeName);
        if (shapeSource instanceof FeatureStore) {
            FeatureStore shapeStore = (FeatureStore) shapeSource;
            shapeStore.setTransaction(transaction);
            try {
                shapeStore.addFeatures(featureCollection);
                transaction.commit();
            } catch (Exception e) {
                logger.severe("Problem writing shapefile: " + e.getMessage());
                transaction.rollback();
            } finally {
                transaction.close();
            }
        } else {
            throw new Exception(typeName + " does not support read/write access");
        }
        return shapeFile.getAbsolutePath().replace(".shp", "");
    }

    private static SimpleFeatureType createFeatureType() {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("Location");
        builder.setCRS(DefaultGeographicCRS.WGS84);
        builder.add("Location", Point.class);
        builder.length(15).add("Name", String.class);
        final SimpleFeatureType LOCATION = builder.buildFeatureType();
        return LOCATION;
    }
}
