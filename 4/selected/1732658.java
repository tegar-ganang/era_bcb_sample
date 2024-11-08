package andresenspatialtest;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.operation.overlay.snap.GeometrySnapper;
import java.io.FilenameFilter;

/**
 * Class to perform Martin's spatial point-pattern comparison test.<br/>
 *
 * Version 2 of the algorithm runs in a separate thread so the GUI remains
 * responsive and is able to indicate it's progress.
 *
 * @author Nick Malleson
 */
public class SpatialTestAlgv2 {

    private File baseShapefile;

    private File testShapefile;

    private File areaShapefile;

    private File outputShapefile;

    private List<Geometry> baseGeometries;

    private List<Geometry> testGeometries;

    private List<Area> areas;

    private int monteCarlo = 100;

    private int samplePercentage = 85;

    private double globalS;

    private String basePointsField = null;

    private String testPointsField = null;

    private ConsoleWriter console = null;

    /** THe column name for the S-index (once calucalted) */
    private static final String SIndexColumnName = "SIndex";

    /**
     * Create a SpatialTestAlg object.
     */
    public SpatialTestAlgv2() {
        this.areas = new ArrayList<Area>();
    }

    /** Run the algorithm. This function is called once all the required variables
     have been set and actually performs the test */
    public boolean runAlgorithm() {
        output("Will run algorithm with following parameters: \n" + "\t: base data: " + this.baseShapefile.getName() + "\n" + "\t: test data: " + this.testShapefile.getName() + "\n" + "\t: area data: " + this.areaShapefile.getName() + "\n" + "\t: monte-carlo runs: " + this.monteCarlo + "\n" + "\t: sample percentage: " + this.samplePercentage + "\n");
        this.baseGeometries = readPointsShapefile(baseShapefile, null, false);
        this.testGeometries = readPointsShapefile(testShapefile, null, false);
        readPointsShapefile(areaShapefile, this.areas, true);
        output("Have read in " + this.baseGeometries.size() + " base points, " + this.testGeometries.size() + " test points" + " and " + this.areas.size() + " areas.");
        int totalBasePoints = this.baseGeometries.size();
        int absTotalTestPoints = this.testGeometries.size();
        output("Counting number of features in each area");
        for (Area a : this.areas) {
            a.numBasePoints = findPointsWithin(a, this.baseGeometries);
            a.absNumTestPoints = findPointsWithin(a, this.testGeometries);
        }
        output("Running Monte-Carlo simulation (sampling points and counting number in each area)");
        for (int i = 0; i < this.monteCarlo; i++) {
            List<Geometry> testSample = sample(testGeometries, this.samplePercentage);
            for (Area area : this.areas) {
                int pointsWithin = findPointsWithin(area, testSample);
                area.numTestPoints.add(pointsWithin);
            }
            output("\tCompleted run " + (i + 1));
        }
        int totalSampledTestPoints = 0;
        for (Area a : this.areas) {
            totalSampledTestPoints += a.numTestPoints.get(0);
        }
        output("Calculating percentage test points in each area for each run");
        for (Area a : this.areas) {
            for (Integer num : a.numTestPoints) {
                if (totalSampledTestPoints > 0) {
                    a.percentageTestPoints.add(100.0 * ((double) num / (double) totalSampledTestPoints));
                } else {
                    a.percentageTestPoints.add(0.0);
                }
            }
            a.percentageBasePoints = 100 * ((double) a.numBasePoints / totalBasePoints);
            a.absPercentageTestPoints = 100 * ((double) a.absNumTestPoints / absTotalTestPoints);
        }
        int numToRemove = (int) Math.round((this.monteCarlo * 0.05) / 2.0);
        output("Ranking percentages in ascending order and removing " + numToRemove + " outliers from top and bottom");
        for (Area a : this.areas) {
            Double[] percentages = a.percentageTestPoints.toArray(new Double[a.percentageTestPoints.size()]);
            Arrays.sort(percentages);
            a.pTestPoitsNoOutliers = new Vector<Double>(Arrays.asList(percentages));
            for (int i = 0; i < numToRemove; i++) {
                a.pTestPoitsNoOutliers.remove(0);
            }
            for (int i = 0; i < numToRemove; i++) {
                a.pTestPoitsNoOutliers.remove(a.pTestPoitsNoOutliers.size() - 1);
            }
        }
        output("Calculating S-index for each area");
        int globalSTotal = 0;
        for (Area a : this.areas) {
            int localS = 0;
            int samples = a.pTestPoitsNoOutliers.size();
            if (a.percentageBasePoints >= a.pTestPoitsNoOutliers.get(0) && a.percentageBasePoints <= a.pTestPoitsNoOutliers.get(samples - 1)) {
                localS = 0;
            } else if (a.percentageBasePoints < a.pTestPoitsNoOutliers.get(0)) {
                localS = -1;
            } else if (a.percentageBasePoints > a.pTestPoitsNoOutliers.get(samples - 1)) {
                localS = 1;
            } else {
                System.err.println("Error calculating local S value.\n\t" + "Percentage base points: " + a.percentageBasePoints + "\n\t" + "Percentage test points: " + a.pTestPoitsNoOutliers.get(0) + " - " + a.pTestPoitsNoOutliers.get(samples - 1));
            }
            a.sVal = localS;
            globalSTotal += Math.abs(localS);
        }
        this.globalS = 1 - ((double) globalSTotal / (double) areas.size());
        output("Found global S value: " + this.globalS);
        output("Outputting shapefile of areas: " + this.outputShapefile.getName());
        outputNewAreas(this.areas, this.outputShapefile);
        output("ALGORITHM HAS FINISHED");
        return true;
    }

    /**
     * Read a shapefile and return a list of geometries of all objects. Code from
     * <url>http://docs.codehaus.org/display/GEOTDOC/04+How+to+Read+a+Shapefile</url>
     * @param file The shapefile to read
     * @param areas Optional list of areas, if not null will be populated as geometries are read
     * @param features Optional feature colleciton, if not null will be populated from
     * the shapefile.
     * @return A list of geometries read in from the shapefile.
     */
    private static List<Geometry> readPointsShapefile(File file, List<Area> theAreas, boolean writeFeatureSource) {
        List<Geometry> geometryList = new ArrayList<Geometry>();
        Map<String, Serializable> connectParameters = new HashMap<String, Serializable>();
        try {
            connectParameters.put("url", file.toURI().toURL());
            connectParameters.put("create spatial index", true);
            DataStore dataStore = DataStoreFinder.getDataStore(connectParameters);
            String[] typeNames = dataStore.getTypeNames();
            String typeName = typeNames[0];
            FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = dataStore.getFeatureSource(typeName);
            if (writeFeatureSource) {
                Area.featureSource = featureSource;
            }
            FeatureCollection<SimpleFeatureType, SimpleFeature> collection = featureSource.getFeatures();
            FeatureIterator<SimpleFeature> iterator = collection.features();
            try {
                while (iterator.hasNext()) {
                    SimpleFeature feature = iterator.next();
                    Geometry geometry = (Geometry) feature.getDefaultGeometry();
                    geometryList.add(geometry);
                    if (theAreas != null) {
                        Area a = new Area(geometry);
                        a.feature = feature;
                        theAreas.add(a);
                    }
                }
            } finally {
                if (iterator != null) {
                    iterator.close();
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return geometryList;
    }

    /** Write the areas to a shapefile. Code from:
     * <url>http://docs.codehaus.org/display/GEOTDOC/05+SHP2SHP+Lab</url>
     * @param areas2 The areas to write out.
     * @param areaShapefile2 The shapefile location.
     */
    private static void outputNewAreas(List<Area> areas2, File areaShapefile) {
        try {
            final String fileName = areaShapefile.getName();
            if (areaShapefile.exists()) {
                for (File file : areaShapefile.getParentFile().listFiles(new FilenameFilter() {

                    public boolean accept(File dir, String name) {
                        return name.startsWith(fileName.substring(0, fileName.length() - 3));
                    }
                })) {
                    file.delete();
                }
            }
            SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
            featureTypeBuilder.init(Area.featureSource.getFeatures().iterator().next().getFeatureType());
            featureTypeBuilder.add(SIndexColumnName, Integer.class);
            featureTypeBuilder.add("NumBsePts", Integer.class);
            featureTypeBuilder.add("NumTstPts", Integer.class);
            featureTypeBuilder.add("PctBsePts", Double.class);
            featureTypeBuilder.add("PctTstPts", Double.class);
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureTypeBuilder.buildFeatureType());
            FeatureCollection<SimpleFeatureType, SimpleFeature> outFeatures = FeatureCollections.newCollection();
            for (Area a : areas2) {
                SimpleFeature existingFeature = a.feature;
                SimpleFeature newFeature = featureBuilder.buildFeature(existingFeature.getIdentifier().getID());
                for (int i = 0; i < existingFeature.getAttributeCount(); i++) {
                    newFeature.setAttribute(i, existingFeature.getAttribute(i));
                }
                newFeature.setAttribute(SIndexColumnName, a.sVal);
                newFeature.setAttribute("NumBsePts", a.numBasePoints);
                newFeature.setAttribute("NumTstPts", a.absNumTestPoints);
                newFeature.setAttribute("PctBsePts", a.percentageBasePoints);
                newFeature.setAttribute("PctTstPts", a.absPercentageTestPoints);
                outFeatures.add(newFeature);
            }
            File newFile = areaShapefile;
            DataStoreFactorySpi factory = new ShapefileDataStoreFactory();
            Map<String, Serializable> create = new HashMap<String, Serializable>();
            create.put("url", newFile.toURI().toURL());
            create.put("create spatial index", Boolean.TRUE);
            ShapefileDataStore newDataStore = (ShapefileDataStore) factory.createNewDataStore(create);
            newDataStore.createSchema(outFeatures.getSchema());
            Transaction transaction = new DefaultTransaction("create");
            String typeName = newDataStore.getTypeNames()[0];
            FeatureStore<SimpleFeatureType, SimpleFeature> featureStore;
            featureStore = (FeatureStore<SimpleFeatureType, SimpleFeature>) newDataStore.getFeatureSource(typeName);
            featureStore.setTransaction(transaction);
            try {
                featureStore.addFeatures(outFeatures);
                transaction.commit();
            } catch (Exception problem) {
                problem.printStackTrace();
                transaction.rollback();
            } finally {
                transaction.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Finds this number of points within the given area */
    private static int findPointsWithin(Area area, List<Geometry> inputPoints) {
        int numPoints = 0;
        for (Geometry g : inputPoints) {
            if (g.within(area.geometry)) {
                numPoints++;
            }
        }
        return numPoints;
    }

    /**
     * Return an X% sample from the input list. */
    private static List<Geometry> sample(List<Geometry> inputList, int percentage) {
        List<Geometry> copy = new ArrayList<Geometry>(inputList.size());
        for (int i = 0; i < inputList.size(); i++) {
            copy.add(i, inputList.get(i));
        }
        Collections.shuffle(copy);
        int numPointsToRemove = (int) Math.round(copy.size() * ((100 - percentage) / 100.0));
        int i = 0;
        while (i < numPointsToRemove) {
            copy.remove(i);
            i++;
        }
        return copy;
    }

    public double getGlobalS() {
        return this.globalS;
    }

    public void setSamplePercentage(int p) {
        this.samplePercentage = p;
    }

    private void output(String output) {
        if (this.console != null) {
            this.console.writeToConsole(output, false);
        }
    }

    private void error(String error) {
        if (this.console != null) {
            this.console.writeToConsole(error, true);
        }
    }

    public File getAreaShapefile() {
        return areaShapefile;
    }

    public void setAreaShapefile(File areaShapefile) {
        this.areaShapefile = areaShapefile;
    }

    public String getBasePointsField() {
        return basePointsField;
    }

    public void setBasePointsField(String basePointsField) {
        this.basePointsField = basePointsField;
    }

    public File getBaseShapefile() {
        return baseShapefile;
    }

    public void setBaseShapefile(File baseShapefile) {
        this.baseShapefile = baseShapefile;
    }

    public int getMonteCarlo() {
        return monteCarlo;
    }

    public void setMonteCarlo(int monteCarlo) {
        this.monteCarlo = monteCarlo;
    }

    public File getOutputShapefile() {
        return outputShapefile;
    }

    public void setOutputShapefile(File outputShapefile) {
        this.outputShapefile = outputShapefile;
    }

    public String getTestPointsField() {
        return testPointsField;
    }

    public void setTestPointsField(String testPointsField) {
        this.testPointsField = testPointsField;
    }

    public File getTestShapefile() {
        return testShapefile;
    }

    public void setTestShapefile(File testShapefile) {
        this.testShapefile = testShapefile;
    }

    public void setConsole(ConsoleWriter console) {
        System.out.println("Algorithm will write all output to a specific" + "console, not to standard output stream.");
        this.console = console;
    }

    /**
    * Return the name of the column that the S-Index is written to in the
    * output shapefile
    */
    public static String getSIndexColumnName() {
        return SpatialTestAlgv2.SIndexColumnName;
    }
}
